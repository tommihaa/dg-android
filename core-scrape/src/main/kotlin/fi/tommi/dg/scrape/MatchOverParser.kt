package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.MatchOverForm
import fi.tommi.dg.domain.MatchOverPage
import fi.tommi.dg.domain.MatchOverScore
import fi.tommi.dg.domain.PlayerRef
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Ottelun päättymissivun jäsennin.
 *
 * Rakenne mitattu 31.8.2026 kahdesta oikeasta ottelusta, ja ne erosivat toisistaan juuri
 * siinä osassa joka on sovellukselle tärkein: toisella oli chat-lomake (`Next Game` ja
 * `To Top` sen nappeina), toisella pelkkä `Next Game>>` -linkki. Molemmat luetaan, eikä
 * puuttuvaa keksitä.
 *
 * Chat luetaan `ChatParser`illa eikä täällä: lomake on sama `form[action*=/bg/move/]` kuin
 * lautasivulla, joten toinen lukija olisi toinen totuus samasta asiasta.
 *
 * **Neljäs muunnelma mitattiin 6.9.2026, ja se on eri sivu kuin ottelun loppu.** Kun
 * tuplaus hylätään, peli päättyy heti eikä sivulla ole lautaa lainkaan: jäljellä on
 * tulosrivi `andersm wins 1 point.`, pistetaulukko ja lomake jonka ainoa nappi on `Next`.
 * Se ei sano `and the match`, joten tämä jäsennin hylkäsi sen ja sivu päätyi
 * `NotABoardKind.WrongPage`en, eli *"This is no longer a board"* ilman yhtään nappia.
 * Ottelu jäi siihen tilaan, ja koska sivusto tarjoaa jonon kärkeä uudelleen jokaisen
 * `commit`in jälkeen, sama sivu tuli vastaukseksi myös muiden otteluiden siirtoihin
 * (`raakasivut/sessio-6-9-ilta3`, rivit 69, 78, 80, 82, 94). Nyt sivu luetaan, ja
 * [MatchOverPage.matchContinues] erottaa sen ottelun lopusta.
 *
 * **Kolmas muunnelma mitattiin 1.9.2026, ja se kaatoi yllä olevan kahtiajaon.** Ottelun
 * 5304226 päättymissivulla oli lomake nappeineen mutta **ei chat-kenttää lainkaan**, eli
 * `Next Game` ja `To Top` olivat olemassa eikä kumpikaan lukija nähnyt niitä: `ChatParser`
 * vaatii tekstikentän ja tämä etsi vain linkkiä. Ruudulle jäi `Review game` ja `Skip game`,
 * joten päättyneestä ottelusta ei päässyt eteenpäin. Napit luetaan nyt [readForm]illa, ja
 * **vain kun sivulla ei ole tekstikenttää**: ehto on tarkalleen `ChatParser`in ehdon
 * käänteispuoli, joten sama nappi ei voi tulla kahdesta lähteestä.
 */
object MatchOverParser {

    /**
     * Tulosrivi silloin kun **ottelu** päättyi. Ankkuroitu sanoihin `and the match`, koska
     * pelin päättyminen kesken ottelun on eri asia; [DgPages.isMatchOverPage] nojaa tähän
     * eikä saa laueta siitä.
     */
    internal val RESULT = Regex("""(.+?\s+wins\s+\d+\s+points?\s+and\s+the\s+match\.)""", RegexOption.IGNORE_CASE)

    /**
     * Tulosrivi silloin kun **peli** päättyi ja ottelu jatkuu, esim. `andersm wins 1 point.`
     *
     * Piste heti pisteluvun jälkeen erottaa tämän [RESULT]ista, joka jatkuu sanoilla
     * `and the match`.
     *
     * Tätä luetaan vain silloin kun sivulla ei ole lautaa, ja se on **varotoimi eikä mitattu
     * tarve**: 6.9.2026 session 103 sivusta tulosrivi oli vain lauduttomilla, eikä
     * lautasivua jolla tulos lukee ole nähty. Vartija on silti paikallaan, koska väärä osuma
     * tekisi laudasta päättymissivun eli veisi pelaajalta laudan.
     */
    internal val GAME_RESULT = Regex("""(.+?\s+wins\s+\d+\s+points?\.)""", RegexOption.IGNORE_CASE)

    private val MATCH_ID = Regex("""Match\s+(\d+)""", RegexOption.IGNORE_CASE)
    private val ROUND = Regex("""(Round\s+\d+)""", RegexOption.IGNORE_CASE)

    fun parse(html: String): MatchOverPage? = parse(Jsoup.parse(html))

    fun parse(document: Document): MatchOverPage? {
        val text = document.body()?.wholeText().orEmpty()
        val matchResult = RESULT.find(text)?.groupValues?.get(1)?.trim()
        // Pelin päättymissivu luetaan vain kun lautaa ei ole. Lautasivulla sama lause on
        // laudan seurana, ja siellä sivu on lauta eikä tämä, ks. [GAME_RESULT].
        val gameResult = matchResult ?: if (DgPages.isBoardPage(document)) {
            null
        } else {
            GAME_RESULT.find(text)?.groupValues?.get(1)?.trim()
        }
        val result = matchResult ?: gameResult ?: return null

        val event = document.selectFirst("h3 a[href*=/bg/event/]")
        val heading = document.selectFirst("h3")?.text().orEmpty()

        return MatchOverPage(
            // Tunniste tulee otsikosta, koska se on molemmissa muunnelmissa; lomakkeen
            // osoite on vain toisessa.
            matchId = MATCH_ID.find(document.title())?.groupValues?.get(1)?.let { MatchId(it) },
            eventName = event?.text()?.takeIf { it.isNotBlank() },
            eventPath = event?.attr("href")?.takeIf { it.isNotBlank() },
            roundLabel = ROUND.find(heading)?.groupValues?.get(1),
            resultText = result,
            // Ottelu jatkuu silloin kun tulosrivi ei sano `and the match`, ks. [GAME_RESULT].
            matchContinues = matchResult == null,
            // Sivun oma merkintä on linkki ohjesivulle, ja teksti sen ympärillä. Linkki on
            // niistä kahdesta se joka ei riipu sanamuodosta.
            predicted = document.selectFirst("a[href*=#predict]") != null,
            matchLength = matchLength(document),
            scores = scores(document),
            nextGamePath = document.select("a[href]")
                .firstOrNull { it.attr("href").trimEnd('/') == "/bg/nextgame" }
                ?.attr("href"),
            reviewPath = document.select("a[href*=/list]")
                .firstOrNull { it.text().contains("Review", ignoreCase = true) }
                ?.attr("href"),
            skipPath = document.select("a[href*=skip=]").firstOrNull()?.attr("href"),
            form = readForm(document),
        )
    }

    /**
     * Sivun nappilomake silloin kun se ei ole chat-lomake.
     *
     * Ehto on tekstikentän puuttuminen, koska juuri se erottaa muunnelmat toisistaan:
     * chat-muunnelmassa napit kuuluvat `ChatForm`ille ja lähtevät `ChatForm.write`n mukana,
     * eikä niitä saa lukea myös täältä. Nappiton lomake jätetään lukematta, koska napiton
     * lomake ei ole teko jonka voisi tarjota.
     */
    private fun readForm(document: Document): MatchOverForm? {
        val form = document.select("form[action*=/bg/move/]")
            .firstOrNull { it.selectFirst("textarea") == null }
            ?: return null
        val submits = form.select("input[type=submit]")
            .map { it.attr("value") }
            .filter { it.isNotBlank() }
        if (submits.isEmpty()) return null
        return MatchOverForm(
            action = form.attr("action"),
            method = FormMethod.from(form.attr("method")),
            hiddenFields = form.select("input[type=hidden]")
                .filter { it.attr("name").isNotBlank() }
                .associate { it.attr("name") to it.attr("value") },
            submits = submits,
        )
    }

    /** `<tr><th>Length<th>7`, eli otsikkosolu ja sen pari samalla rivillä. */
    private fun matchLength(document: Document): Int? =
        document.select("tr:has(th)")
            .firstOrNull { it.selectFirst("th")?.text().equals("Length", ignoreCase = true) }
            ?.select("th")?.getOrNull(1)?.text()?.trim()?.toIntOrNull()

    /**
     * Pistetaulukon rivit. Nimi luetaan pelaajalinkistä, koska sama solu kantaa myös
     * lihavoinnin, ja pisteet ovat rivin seuraavassa solussa.
     */
    private fun scores(document: Document): List<MatchOverScore> =
        document.select("tr:has(a[href*=/bg/user/])").mapNotNull { row ->
            val link = row.selectFirst("a[href*=/bg/user/]") ?: return@mapNotNull null
            val cells = row.select("td")
            MatchOverScore(
                player = PlayerRef(
                    name = link.text().trim().ifBlank { return@mapNotNull null },
                    userId = SiteIds.userId(link.attr("href")),
                ),
                score = cells.getOrNull(1)?.text()?.trim()?.toIntOrNull(),
            )
        }
}
