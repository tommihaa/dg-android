package fi.tommi.dg.scrape

import fi.tommi.dg.domain.BarCheckers
import fi.tommi.dg.domain.BoardForm
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.BorneOffCheckers
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.CommandLink
import fi.tommi.dg.domain.Cube
import fi.tommi.dg.domain.CubePosition
import fi.tommi.dg.domain.Die
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.MoveLink
import fi.tommi.dg.domain.PlayerPanel
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.Point
import fi.tommi.dg.domain.isRolledBackNotice
import fi.tommi.dg.domain.isSpeculativePrompt
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Lautasivun jäsennin.
 *
 * Onnekas löydös: lautaa ei tarvitse päätellä kuvatiedostojen nimistä, koska jokaisella
 * pisteen kuvalla on `ALT`-attribuutti joka koodaa tilan suoraan. `y3` on kolme keltaista,
 * `b1` yksi sininen, `_` tyhjä piste.
 *
 * Pisteen numero luetaan **sivun omasta numerorivistä**, ei kuvan suunnasta. Aiempi versio
 * päätteli sen suunnasta (`pt_..._down` = 13-24 vasemmalta oikealle), ja se osoittautui
 * vääräksi 1.8.2026: käyttäjäasetus `Home boards on left side` peilaa koko laudan, jolloin
 * ylärivi juoksee 24-13 ja nappulat ovat käänteisessä järjestyksessä. Suunnasta päätelty
 * numero olisi silloin osoittanut peilipisteeseen, eikä se olisi tuottanut virhettä vaan
 * väärän siirron.
 *
 * Lauta on siksi luettava sellaisena kuin sivu sen itse nimeää: numerot ovat omalla
 * rivillään kuvarivin ylä- tai alapuolella, ja sarakeindeksi yhdistää ne toisiinsa.
 * Fallbackia ei ole. Jos numeroa ei löydy, piste jätetään pois, koska arvattu numero on
 * tässä pahempi kuin puuttuva.
 */
object BoardParser {

    private val TITLE = Regex("""Match\s+(\d+),\s*Move\s+(\d+)""")
    private val STATE_TOKEN = Regex("""/bg/move/\d+/(\d+)""")
    private val MOVE_CODE = Regex("""[?&]move=([^&]+)""")
    private val MATCH_LENGTH = Regex("""(\d+)\s+Point Match""")
    private val PIPS = Regex("""(\d+)\s*pips""")
    /**
     * Pistekenttä sellaisenaan, tähti mukaan lukien.
     *
     * Tähti on sivuston Crawford-merkintä (`score: <B>6*</B>`), ja aiempi muoto `(\d+)` luki
     * siitä pelkän luvun ja pudotti merkin sanomatta mitään. Merkki on rajattu tähteen eikä
     * mihin tahansa jälkiliitteeseen, koska muita ei ole nähty: mitattu 14.8.2026 koko
     * korpuksesta, jossa pistekentän muodot ovat pelkkiä lukuja ja `2*`, `6*` ja `8*`.
     */
    private val SCORE = Regex("""score:\s*(\d+\*?)""")
    private val ALT_CHECKERS = Regex("""^([a-z])(\d+)$""")
    /**
     * Ulos kannettujen pino tiedostonimestä.
     *
     * **Loppuosa on `_` tai `.`, ja alaviivan vaatiminen oli vika 22.8.2026 asti.** Sivusto
     * piirtää täyden viiden pinon nimellä `off_y5.gif` ja osittaisen nimellä
     * `off_y4_top.gif`, eli täydessä pinossa ei ole paikkaosaa lainkaan. Vanha lauseke
     * vaati alaviivan luvun jälkeen, joten **jokainen täysi pino putosi hiljaa**: yhdeksän
     * ulos kannettua näkyi neljänä, ja pip-luku oli silti oikein, koska se luetaan sivulta.
     */
    private val OFF_CHECKERS = Regex("""off_([a-z])(\d+)[_.]""")
    private val BAR_CHECKERS = Regex("""bar_([a-z])(\d+)""")

    /**
     * Kuution arvo kuvatiedoston nimestä, esim. `cube2.gif`. `cubedr.gif` ja `cubedrs.gif`
     * eivät osu, ja se on oikein: niissä ei ole numeroarvoa.
     */
    private val CUBE_VALUE = Regex("""cube(\d+)\.gif""")

    /** Double repeat -kuutio tiedostonimestä: `cubedr.gif` (keskellä) ja `cubedrs.gif` (omistettu). */
    private val CUBE_DR = Regex("""cubedrs?\.gif""")
    private val ROUND = Regex("""Round\s+(\d+)""")

    /** Montako numeroitua solua rivillä on oltava jotta se kelpaa numeroriviksi. */
    private const val MIN_LABELS_PER_ROW = 6

    fun parse(html: String): BoardState? {
        val document = Jsoup.parse(html)

        val titleMatch = TITLE.find(document.title())

        // Lautaosoite voi olla joko linkissä tai lomakkeen action-attribuutissa, ja
        // heittoa edeltävällä sivulla se on **vain** lomakkeessa: siellä ei ole yhtään
        // /bg/move/-ankkuria, koska siirtolinkit syntyvät vasta heiton jälkeen. Kumpikin
        // luetaan sivulta samalla säännöllä: seuraa linkkiä, älä laske sitä.
        val movePaths: List<String> =
            document.select("a[href*=/bg/move/]").map { it.attr("href") } +
                document.select("form[action*=/bg/move/]").map { it.attr("action") }

        val matchId = titleMatch?.groupValues?.get(1)
            ?: movePaths.firstNotNullOfOrNull {
                Regex("""/bg/move/(\d+)/""").find(it)?.groupValues?.get(1)
            }
            ?: return null

        // Numerointi tehdään kerran ja jaetaan: pisteiden ja siirtolinkkien on pakko olla
        // samasta luvusta, tai linkki tarjoaisi siirtoa väärältä pisteeltä.
        val notices = parseNotices(document)
        val numbered = numberPointImages(document)
        val points = numbered.map { (number, image) -> toPoint(number, image) }
            .sortedBy { it.number }

        return BoardState(
            matchId = MatchId(matchId),
            moveNumber = titleMatch?.groupValues?.get(2)?.toIntOrNull(),
            stateToken = movePaths.firstNotNullOfOrNull {
                STATE_TOKEN.find(it)?.groupValues?.get(1)
            },
            // Liigaottelun otsikko on pelkkaa tekstia ilman tapahtumalinkkia (mitattu
            // 14.9.2026, `DG 8x8 2026 - H vs C - Div 1`), joten nimi luetaan silloin
            // h3:sta itsestaan. Turnauksessa h3 on linkki ja `, Round n`, ja linkki voittaa.
            eventName = document.selectFirst("a[href*=/bg/event/]")?.text()
                ?: document.selectFirst("h3")?.text()
                    ?.let { ROUND.replace(it, "").trimEnd(',', ' ') }
                    ?.takeIf { it.isNotBlank() },
            eventId = document.selectFirst("a[href*=/bg/event/]")
                ?.let { SiteIds.eventId(it.attr("href")) },
            eventPath = document.selectFirst("a[href*=/bg/event/]")
                ?.attr("href")?.takeIf { it.isNotBlank() },
            round = document.selectFirst("h3")?.text()
                ?.let { ROUND.find(it)?.groupValues?.get(1) }
                ?.let { "Round $it" },
            matchLength = MATCH_LENGTH.find(document.text())?.groupValues?.get(1)?.toIntOrNull(),
            cube = parseCube(document),
            dice = parseDice(document),
            scheme = parseScheme(document),
            points = points,
            players = parsePlayers(document),
            // h4 ensin ja vasta sitten b: jälkimmäinen on joka sivulla sama vakiorivi
            // ("Please select your action/make your move:"), edellinen kertoo tilanteen.
            // Molemmat ovat sivulla yhtä aikaa silloin kun tilanneteksti on olemassa.
            prompt = document.selectFirst("h4")?.text()
                ?: document.selectFirst("body > b")?.text(),
            notices = notices,
            // Tuntomerkki tulee core-domainista, koska sama sääntö on käytössä myös
            // näkymässä: se jättää lipuksi luetun lauseen piirtämättä toistamiseen.
            rolledBack = notices.any(::isRolledBackNotice),
            // Koko sivun tekstistä eikä ilmoituksista: kysymys on lautataulukon yläpuolella
            // omana `b`-rivinään eikä ilmoitusten joukossa. Tuntomerkki tulee
            // core-domainista samasta syystä kuin yllä, ks. `isSpeculativePrompt`.
            speculative = isSpeculativePrompt(document.text()),
            moves = parseMoves(numbered, points),
            commands = parseCommands(document),
            form = parseForm(document),
            undoHref = parseUndoHref(document),
            skipHref = parseSkipHref(document),
            borneOff = parseBorneOff(document),
            bar = parseBar(document),
            pendingReplays = parsePendingReplays(document),
        )
    }

    /**
     * `Pending Replay: <a href=/bg/game/...>_1_</a>` bodyn suorana lapsena, vain double
     * repeat -otteluissa. Luku on linkin tekstissä alaviivojen välissä, ja nimiö on oma
     * tekstisolmunsa sen edellä. Nimiö jätetään pois [parseNotices]ista, koska luku
     * piirretään omana rivinään: pelkkä nimiö näkyi laitteella 9.9.2026 ilman sisältöä.
     * Linkkiä ei kanneta, ks. `BoardState.pendingReplays`.
     */
    private fun parsePendingReplays(document: Document): Int? =
        document.body()?.childNodes()
            ?.zipWithNext()
            ?.firstNotNullOfOrNull { (node, next) ->
                if (node is TextNode && isPendingReplayLabel(node.text()) &&
                    next is Element && next.tagName().equals("a", ignoreCase = true)
                ) {
                    PENDING_REPLAY_COUNT.find(next.text())?.value?.toIntOrNull()
                } else {
                    null
                }
            }

    private fun isPendingReplayLabel(text: String): Boolean =
        text.trim().equals(PENDING_REPLAY_LABEL, ignoreCase = true)

    /** Toistojonon nimiö ja linkin luku, ks. [parsePendingReplays]. */
    private const val PENDING_REPLAY_LABEL = "Pending Replay:"
    private val PENDING_REPLAY_COUNT = Regex("[0-9]+")

    /**
     * Lomake luetaan sellaisenaan, koska se on sivun ainoa tie muuttaviin toimintoihin:
     * Submit Move, Submit Greedy Bearoff, Roll Dice, Double, Accept ja Decline ovat kaikki
     * nappeja eivätkä linkkejä. Siirtojen kokoaminen on linkkejä, ja juuri se ero piti
     * lomakkeen jäsentimen katveessa 4.8.2026 asti.
     *
     * Valitaan lautaosoitteeseen osoittava lomake nimenomaisesti: rajaamaton `form` osuisi
     * ensimmäiseen, ja asetussivulla se olisi salasananvaihto. Sama varovaisuus kuin
     * [SettingsParser]issa, nyt toisella sivulla.
     */
    private fun parseForm(document: Document): BoardForm? {
        val form = document.selectFirst("form[action*=/bg/move/]") ?: return null
        return BoardForm(
            action = form.attr("action"),
            // Sivulta luettuna, ei oletettuna. Lautasivulla tämä on GET, mutta sama tyyppi
            // kantaa nyt myös pikaviestin POSTin, eikä metodi saa olla kutsupaikan tietoa.
            method = FormMethod.from(form.attr("method")),
            pendingMove = form.selectFirst("input[type=hidden][name=move]")
                ?.attr("value")
                ?.takeIf { it.isNotEmpty() },
            submits = form.select("input[type=submit]").map { it.attr("value") },
            verify = form.selectFirst("input[type=checkbox][name=verify]")
                ?.attr("value")
                ?.takeIf { it.isNotEmpty() },
            // Sama valinta kuin [ChatParser]issa, koska kyse on samasta asiasta: sivun oma
            // `commit=1` on lomakkeen osa, ja ilman sitä POST ei tee mitään.
            hiddenFields = form.select("input[type=hidden]")
                .filter { it.attr("name").isNotBlank() }
                .associate { it.attr("name") to it.attr("value") },
        )
    }

    /**
     * "Undo Move" on linkki eikä nappi, ja sen osoitteessa ei ole `move`-parametria
     * lainkaan. Siksi sitä ei löydy [parseCommands]in valinnalla (`a[href*=move=]`), ja
     * siksi se oli näkymätön: se on ainoa lautasivun linkki jonka tunnistaa vain tekstistä.
     */
    private fun parseUndoHref(document: Document): String? =
        document.select("a[href*=/bg/move/]")
            .firstOrNull { it.text().trim().equals("Undo Move", ignoreCase = true) }
            ?.attr("href")

    /**
     * Ulos kannetut nappulat. Väri ja lukumäärä luetaan **tiedostonimestä**
     * (`off_y3_bot.gif`), koska ALT ei kanna väriä kaikissa tapauksissa: mitattu 4.8.2026,
     * `_top`-kuvien ALT on paljas luku ja `_bot`-kuvien ALT on värillinen.
     *
     * ~~Sama väri voi esiintyä sivulla vain kerran~~, mutta summataan silti: yhdenkin
     * odottamattoman toiston hiljainen pudottaminen olisi pahempi kuin liian suuri luku,
     * joka näkyy heti pip-vahdissa.
     *
     * **Oletus kumoutui 22.8.2026 ja varovaisuus palkittiin.** Sama väri esiintyy sivulla
     * niin monena kuvana kuin pinoja tarvitaan: yhdeksän ulos kannettua on `off_y5.gif` ja
     * `off_y4_top.gif`, eli kaksi kuvaa samalle värille. Summaus oli siis oikea ratkaisu
     * väärästä syystä, ja se toimi heti kun lauseke lakkasi pudottamasta täysiä pinoja.
     */
    private fun parseBorneOff(document: Document): List<BorneOffCheckers> =
        document.select("img[src*=off_]")
            .mapNotNull { image -> OFF_CHECKERS.find(image.attr("src")) }
            .mapNotNull { match ->
                val color = CheckerColor.fromCode(match.groupValues[1].first())
                    ?: return@mapNotNull null
                val count = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                color to count
            }
            .groupBy({ it.first }, { it.second })
            .map { (color, counts) -> BorneOffCheckers(color, counts.sum()) }
            .sortedBy { it.color.name }

    /**
     * Muurilla olevat nappulat. Väri ja lukumäärä luetaan **tiedostonimestä**
     * (`bar_b1.gif`), samasta syystä kuin [parseBorneOff]issa: ALT riippuu paikasta eikä
     * sisällöstä. Mitattu 5.8.2026, ja sama tiedosto kantaa kahta eri ALTia:
     * `move_board_oikea.html` sanoo `b1`, `move_rollback.html` sanoo `1`. ALTiin nojaava
     * luku menettäisi siis värin hiljaa niissä tapauksissa joissa sitä ei ole.
     *
     * Regex nimeen eikä pelkkä `src*=bar_`: jälkimmäinen osuisi myös hypoteettiseen
     * `navbar_*.gif`:iin, ja nimeen sidottu haku jättää sen huomiotta sen sijaan että
     * tuottaisi roskaa.
     *
     * Summataan värin mukaan samasta syystä kuin ulos kannetut: odottamattoman toiston
     * hiljainen pudottaminen olisi pahempi kuin liian suuri luku, joka näkyy heti
     * pip-vahdissa.
     */
    private fun parseBar(document: Document): List<BarCheckers> =
        document.select("img[src*=bar_]")
            .mapNotNull { image ->
                val match = BAR_CHECKERS.find(image.attr("src")) ?: return@mapNotNull null
                val color = CheckerColor.fromCode(match.groupValues[1].first())
                    ?: return@mapNotNull null
                val count = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                // Linkki luetaan kuvan ymparilta, koska muurilla ei ole omaa solunumeroa
                // johon sen voisi sitoa. `parent()` riittaa: sivu kietoo linkin suoraan
                // kuvan ymparille kuten pisteillakin.
                val href = image.parent()
                    ?.takeIf { it.normalName() == "a" }
                    ?.attr("href")
                    ?.takeIf { MOVE_CODE.containsMatchIn(it) }
                Triple(color, count, href)
            }
            .groupBy { it.first }
            .map { (color, osat) ->
                BarCheckers(
                    color = color,
                    count = osat.sumOf { it.second },
                    // Sama vari voi esiintya kahtena pinona kuten ulos kannetuilla, ja
                    // linkki on niista silla joka sen sai. Ensimmainen ei-tyhja kelpaa,
                    // koska sivu tarjoaa saman osoitteen jokaiselle saman varin nappulalle.
                    href = osat.firstNotNullOfOrNull { it.third },
                )
            }
            .sortedBy { it.color.name }

    /**
     * Yhdistää jokaisen pistekuvan siihen numeroon jonka sivu itse on sen kohdalle
     * kirjoittanut.
     *
     * Numerot ovat omalla rivillään: ylärivin numerot kuvarivin yläpuolella ja alarivin
     * numerot sen alapuolella. Suuntaa ei tarvitse tietää, koska naapureista vain toinen
     * on numerorivi. Yhdistävä tekijä on **sarakeindeksi**, ja se kestää laudan peilauksen:
     * peilatulla sivulla numerorivikin on peilattu, joten pari pysyy oikeana.
     *
     * Muurin kuva ei ole `pt_`-alkuinen, joten se rajautuu pois jo valinnassa. Sen sarake
     * on numerorivillä tyhjä, eli se putoaisi pois myös numeroa lukiessa.
     */
    private fun numberPointImages(document: Document): List<Pair<Int, Element>> =
        document.select("img[src*=pt_]").mapNotNull { image ->
            val cell = image.parents().firstOrNull { it.normalName() == "td" }
                ?: return@mapNotNull null
            val row = cell.parent()?.takeIf { it.normalName() == "tr" } ?: return@mapNotNull null
            val index = row.tableCells().indexOf(cell).takeIf { it >= 0 } ?: return@mapNotNull null
            val number = labelRowFor(row)?.getOrNull(index) ?: return@mapNotNull null
            number to image
        }

    /**
     * Rivin numerot sarakeindeksin mukaisessa järjestyksessä, tai null jos naapureista
     * kumpikaan ei ole numerorivi.
     */
    private fun labelRowFor(row: Element): List<Int?>? {
        val neighbours = listOfNotNull(row.previousElementSibling(), row.nextElementSibling())
        val labels = neighbours.map { neighbour -> neighbour.tableCells().map { pointNumber(it) } }
        return labels.firstOrNull { it.count { number -> number != null } >= MIN_LABELS_PER_ROW }
    }

    private fun pointNumber(cell: Element): Int? =
        cell.ownText().trim().toIntOrNull()?.takeIf { it in 1..24 }

    /**
     * Palvelimen ilmoitukset bodyn alla, kahdessa muodossa ja sivun järjestyksessä.
     *
     * 1. **Paljas teksti ilman omaa elementtiä.** Peruutusilmoitus on tätä lajia, ja juuri
     *    elementittömyys teki siitä näkymättömän jäsentimelle 1.8.2026 asti.
     * 2. **Punainen `FONT`-rivi** (`COLOR=#cc0000`), bodyn suorana lapsena. Sivusto vastaa
     *    näin vahvistamattomaan kuutiotekoon: `?submit=Accept` ilman `verify`ä palauttaa
     *    saman laudan ja rivin `Previous move not verified!` sen alle. Mitattu 3.9.2026
     *    (fixture `move_accept_not_verified.html`), ja sama rivi löytyi jälkikäteen kolmesta
     *    aiemmasta `Double`-painalluksesta (28.8. ja 31.8.2026). Ne oli luettu *"sivusto
     *    palautti saman laudan"*, koska rivi oli elementti eikä tekstisolmu, ja siksi se
     *    putosi tästä listasta. Koko korpuksessa väriä käyttää vain kaksi lausetta, tämä ja
     *    asetussivun `Profile Successfully Updated!`, eli väri on sivuston ilmoitusväri.
     *
     * Luetaan solmut eikä etsitä tiettyä lausetta: tunnettua lausetta hakeva jäsennin ei
     * koskaan löydä sitä toista, jota ei vielä tunneta.
     */
    private fun parseNotices(document: Document): List<String> =
        document.body()?.childNodes()
            ?.mapNotNull { node ->
                when {
                    node is TextNode && isPendingReplayLabel(node.text()) -> null
                    node is TextNode -> node.text()
                    node is Element && node.tagName().equals("font", ignoreCase = true) &&
                        node.attr("color").equals(NOTICE_COLOR, ignoreCase = true) -> node.text()
                    else -> null
                }
            }
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    /** Sivuston ilmoitusväri, ks. [parseNotices]. */
    private const val NOTICE_COLOR = "#cc0000"

    private fun toPoint(number: Int, image: Element): Point {
        val alt = image.attr("alt")
        val checkers = ALT_CHECKERS.find(alt)
        return Point(
            number = number,
            owner = checkers?.groupValues?.get(1)?.firstOrNull()?.let { CheckerColor.fromCode(it) },
            count = checkers?.groupValues?.get(2)?.toIntOrNull() ?: 0,
        )
    }

    /**
     * Siirtolinkit luetaan sivulta, ei muodosteta. Havainnon mukaan kirjain on
     * `a` pisteelle 1 ... `x` pisteelle 24, mutta oletusta ei käytetä osoitteen
     * rakentamiseen: väärä arvaus tuottaisi väärän siirron eikä virhettä.
     */
    private fun parseMoves(
        numbered: List<Pair<Int, Element>>,
        points: List<Point>,
    ): List<MoveLink> =
        numbered.mapNotNull { (number, image) ->
            val anchor = image.parent()?.takeIf { it.normalName() == "a" } ?: return@mapNotNull null
            val href = anchor.attr("href")
            val code = MOVE_CODE.find(href)?.groupValues?.get(1) ?: return@mapNotNull null
            MoveLink(fromPoint = number, code = code, href = href)
        }.sortedBy { it.fromPoint }
            // Piste jolla ei ole nappuloita ei voi olla lähtöpiste; jos sellainen tulisi,
            // jäsennys on mennyt pieleen ja on parempi jättää se pois kuin tarjota se.
            .filter { link -> points.firstOrNull { it.number == link.fromPoint }?.count?.let { it > 0 } == true }

    /**
     * `Skip Game`, eli sivun oma tapa siirtyä seuraavaan peliin pelaamatta tätä.
     *
     * **Tämä ei kelpaa komennoksi [parseCommands]in mielessä eikä sitä kelpuuteta sinne.**
     * Komentojen ehto on `move=`, ja se ehto on se mikä pitää navigointipalkin (`Top Page`,
     * `Game Lounge`, `Log Out`) ja `Review Gamen` poissa napeista. Ehdon löysääminen olisi
     * tuonut ne kaikki mukanaan, joten skip luetaan omalla ehdollaan: osoitteessa on sekä
     * `/bg/nextgame` että `skip=`.
     *
     * **Osoite on jonoa kuluttava, ja se on tarkoitus eikä sivuvaikutus.** Sivustolla juuri
     * `/bg/nextgame` on se reitti joka näyttää kunkin kohteen kerran, ja skip on pyyntö
     * ohittaa tämä ottelu siinä jonossa. Sovellus ei hae sitä koskaan itse: osoite päätyy
     * verkkoon vain käyttäjän painalluksesta, kuten muutkin laudan linkit.
     */
    private fun parseSkipHref(document: Document): String? =
        document.selectFirst("a[href*=/bg/nextgame][href*=skip=]")?.attr("href")

    /**
     * Komennot ovat samoja `?move=`-linkkejä mutta tekstinä, esim. "Swap Dice"
     * (`?type=9&move=S`). Nappulalinkeissä teksti on tyhjä, koska ne kietovat kuvan.
     */
    private fun parseCommands(document: Document): List<CommandLink> =
        document.select("a[href*=move=]")
            .filter { it.selectFirst("img") == null }
            .mapNotNull { anchor ->
                val label = anchor.text().trim()
                if (label.isEmpty()) null else CommandLink(label, anchor.attr("href"))
            }

    /**
     * Kuutio kuvasta ja sen solusta.
     *
     * **ALTia ei pakoteta luvuksi.** `cubedr.gif` kantaa `ALT="dr"`, ja vanha
     * `alt.toIntOrNull()` teki siitä nullin, joka on erottamaton siitä ettei kuutiota ole
     * sivulla lainkaan. Mitattu 9.8.2026 kuudesta oikeasta ottelusta: kuutiokuva oli
     * jokaisella sivulla, eli null ei ollut kertaakaan oikea lukema.
     *
     * Paikka luetaan solun `VALIGN`ista. Tuntematon arvo ei muutu keskikohdaksi, koska
     * keskikohta on sivulla oma merkityksensä (omistamaton) eikä oletusarvo.
     */
    private fun parseCube(document: Document): Cube? {
        val image = document.selectFirst("img[src*=cube]") ?: return null
        val label = image.attr("alt")
        // **Arvo tulee tiedostonimestä eikä ALTista** (mitattu 10.8.2026). Sama kuva
        // `/images/2/cube2.gif` kantaa kahdella eri lautasivulla kahta eri ALTia, `2` ja
        // `cube2`, joten ALTista luettu numero katoaa sivun mukaan. Tiedostonimi on sama
        // molemmilla. Sääntö on tässä kolmatta kertaa samasta syystä: nappuloilla ja
        // muurilla väri luetaan jo tiedostonimestä, koska ALT ei kanna sisältöä luotettavasti.
        val value = CUBE_VALUE.find(image.attr("src"))?.groupValues?.get(1)?.toIntOrNull()
        // Sama sääntö neljättä kertaa (3.9.2026): `cubedrs.gif` kantaa ALTia `dr` 34 sivulla
        // ja `cubedrs` 13:lla, joten DR:kin luetaan tiedostonimestä. Fixture
        // `move_cube_drs_alt.html` on jälkimmäistä muotoa.
        val doubleRepeat = CUBE_DR.containsMatchIn(image.attr("src"))
        val position = when (image.closest("td")?.attr("valign")?.lowercase()) {
            "top" -> CubePosition.TOP
            "middle" -> CubePosition.MIDDLE
            "bottom" -> CubePosition.BOTTOM
            else -> CubePosition.UNKNOWN
        }
        return Cube(label = label, value = value, doubleRepeat = doubleRepeat, position = position)
    }

    private fun parseDice(document: Document): List<Die> =
        document.select("img[src*=die_]").mapNotNull { image ->
            val value = image.attr("alt").toIntOrNull() ?: return@mapNotNull null
            val owner = Regex("""die_([a-z])""").find(image.attr("src"))
                ?.groupValues?.get(1)?.firstOrNull()
                ?.let { CheckerColor.fromCode(it) }
            Die(value, owner)
        }

    /**
     * Käyttäjän `Board Scheme` lautakuvien hakemistosta.
     *
     * Skeema on polun numero eikä tiedostonimi (mitattu 3.8.2026), joten se luetaan juuri
     * niistä kahdesta kuvalajista jotka ovat joka skeemassa samannimisiä. Mini on ainoa
     * jolla on merkitystä jäsentimelle, mutta kaikki kolme tunnistetaan, jotta tuntematon
     * hakemisto pysyisi erossa tunnetuista.
     *
     * **Ristiriitainen sivu luetaan tuntemattomaksi.** Jos hakemistoja on useampi kuin yksi,
     * sivu ei ole mikään skeema vaan jotain jota ei ole nähty, ja arvaus olisi silloin
     * pahempi kuin tyhjä: ruutu neuvoisi vaihtamaan asetusta jota vika ei koske.
     */
    private fun parseScheme(document: Document): BoardScheme? {
        val directories = document.select("img[src*=pt_], img[src*=die_]")
            .map { it.attr("src").substringBeforeLast('/') }
            .toSet()
        return when (directories.singleOrNull()) {
            "/images/1" -> BoardScheme.CLASSIC
            "/images/2" -> BoardScheme.BLUE_WHITE
            "/images/3" -> BoardScheme.MINI
            else -> null
        }
    }

    /**
     * Pelaajapaneelit, **kahdella tuntomerkillä eikä yhdellä**.
     *
     * Ennen tässä oli vain `td:has(a[href*=/bg/user/])`, eli pelaajanimen linkki. Se
     * osoittautui 24.8.2026 mitatusti riittämättömäksi: käyttäjäasetus `Player Name links on
     * game page` poistaa linkin, ja silloin tämä palautti **tyhjän listan**
     * (`PlayerNameLinksCaptureLiveTest`, kaksi otosta samasta ottelusta). Sivu jäsentyi yhä
     * laudaksi, joten vika oli hiljainen: ruudulta katosivat nimet, pisteet ja pip-luvut,
     * pip-vahdilla ei ollut mitään mihin verrata, ja koska rooli tunnetaan vain vahdin
     * kautta, myös roolivärit menivät varasuunnalle.
     *
     * Tieto ei katoa sivulta vaan katosi jäsentimeltä. Solu on molemmissa tiloissa sama, ja
     * koko ero on `<A HREF=/bg/user/...>`-elementin olemassaolo nimen ympärillä.
     *
     * **Toinen tuntomerkki on pistekenttä, ja se luetaan solun omasta tekstistä**
     * (`:matchesOwn`). Väljempi `:contains` antaisi mitatuilla sivuilla saman tuloksen, ja
     * se tarkistettiin: fixturessa on kaksi taulukkoa eikä kumpikaan ole solun sisällä,
     * joten ulompaa solua ei ole olemassakaan. Tiukempi muoto valittiin siltä varalta että
     * rakenne joskus muuttuu sisäkkäiseksi, koska silloin `:contains` poimisi myös
     * ympäröivät solut ja paneeleita olisi enemmän kuin pelaajia. Ero on tänään nolla, ja
     * `BoardParserPlayerLinksTest` kertoo jos se lakkaa olemasta nolla.
     *
     * **Linkkiehto jää, vaikka uusi kattaa mitatut sivut yksinään.** Pistekenttä on nähty
     * ottelusivuilla, mutta sivustolla on pelimuotoja joita ei ole mitattu, eikä ole
     * todistettu että rivi on aina paikalla. Kahdesta ehdosta jälkimmäinen on siis
     * varasuunta eikä korvaaja, ja niiden yhdiste pitää aiemmat fixturet ennallaan.
     *
     * **Käyttäjänumero katoaa oikeasti kun linkkiä ei ole.** Nimi, pipit ja pisteet ovat
     * solun tekstissä, mutta `/bg/user/<id>` on vain linkissä, joten [PlayerPanel.userId] on
     * silloin null. Se on kentän tyypin mukaista eikä puute jota voisi paikata lukemalla
     * tarkemmin.
     */
    private fun parsePlayers(document: Document): List<PlayerPanel> =
        document.select("td:has(a[href*=/bg/user/]), td:matchesOwn(score:)").mapNotNull { cell ->
            val link = cell.selectFirst("a[href*=/bg/user/]")
            val text = cell.text()
            val scoreLabel = SCORE.find(text)?.groupValues?.get(1)
            PlayerPanel(
                player = PlayerRef(
                    // Linkitön nimi luetaan solun ensimmäisestä `<b>`:stä. Pistekenttä on
                    // samassa solussa ja sekin lihavoitu (`score: <B>7</B>`), mutta se tulee
                    // nimen jälkeen, joten järjestys erottaa ne ilman että tekstiä tulkitaan.
                    // Nimetön solu jätetään pois: paneeli ilman nimeä ei ole pelaaja.
                    name = link?.text() ?: cell.selectFirst("b")?.text() ?: return@mapNotNull null,
                    userId = link?.attr("href")?.let { SiteIds.userId(it) },
                    // Polku sellaisenaan, ei koottuna numerosta: pelaajakortti avaa sen
                    // (Tommin toive 14.9.2026), ja se on sivun oma linkki.
                    profilePath = link?.attr("href")?.takeIf { it.isNotBlank() },
                ),
                pips = PIPS.find(text)?.groupValues?.get(1)?.toIntOrNull(),
                // Luku ja merkintä erotetaan tässä eikä mallissa: luku on sama kuin ennen,
                // ja merkki kulkee erikseen sille joka osaa lukea sen.
                score = scoreLabel?.trimEnd('*')?.toIntOrNull(),
                scoreLabel = scoreLabel,
                backgroundColor = cell.attr("bgcolor").takeIf { it.isNotBlank() },
            )
        }
}
