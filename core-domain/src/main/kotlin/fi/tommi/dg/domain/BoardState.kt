package fi.tommi.dg.domain

/**
 * Lauta yhdellä hetkellä.
 *
 * Tila on luettu sivulta, ei laskettu. Erityisesti [MoveLink.code] ja [stateToken] otetaan
 * sivun omista linkeistä eikä muodosteta itse: DailyGammon pitää siirron välitilaa
 * palvelimella, ja väärin arvattu tunniste ei tuottaisi virhettä vaan väärän siirron.
 *
 * Huom myös: sivustolla on koneen arvaus, ja lauta voi palautua taaksepäin jos vastustaja
 * teki muun kuin arvatun siirron. Tallennettu lautatila ei siis ole lopullinen totuus.
 *
 * **Osoitteen kolme sanaa ovat kolme asiaa, ei yksi ajautunut** (mitattu 1.9.2026,
 * kompositioauditoinnin H7). Havainto väitti että `path`, `href` ja `action` ovat sama
 * käsite eri sanoin; ne eivät ole, ja siksi niitä ei yhtenäistetty.
 *
 * - `action` on lomakkeen oma kohde ([BoardForm.action], [ChatForm], [FormSubmission]).
 *   Lomakkeella on lisäksi metodi ja kentät, eikä sitä seurata linkkinä.
 * - `href` on tällä laudalla se osoite jonka näkymä seuraa `follow`illa, **ja se voi olla
 *   synteettinen**: `CompositionSession` antaa paikalliselle kokoamiselle `local:`-alkuiset
 *   arvot ([undoHref], [MoveLink.href], [CommandLink.href], [BarEntry.href]), joilla ei ole
 *   vastinetta sivustolla. Juuri siksi sanaa ei vaihdettu poluksi: `path` väittäisi
 *   osoitteesta jotain mikä ei pidä paikkaansa, ja `LocalComposition` nojaa siihen että
 *   arvo ei ole polku.
 * - `path` on sivun oma luettu polku (`playPath`, `profilePath`, `messageQueuePath`), jota
 *   ei koota vaan luetaan, ja joka kelpaa pyynnöksi sellaisenaan.
 *
 * [skipHref] on `href` vaikka se on aina sivuston oma osoite: se kuuluu samaan
 * seurattavien joukkoon ([links]), jonka jäsenyys on portti väärää ottelua vastaan.
 */
data class BoardState(
    val matchId: MatchId,
    /** Otsikon juokseva siirtonumero, esim. "Move 579". Eri asia kuin [stateToken]. */
    val moveNumber: Int?,
    /**
     * Polun tilatunniste sivun omista linkeistä, esim. 1507 osoitteessa
     * /bg/move/5296577/1507. Ei laskettavissa: haettu sivu voi olla eri tunnisteella
     * kuin sen omat linkit.
     */
    val stateToken: String?,
    val eventName: String?,
    val eventId: String?,
    /**
     * Turnaussivun polku sivun omasta linkistä, tai null kun linkkiä ei ole (liigaottelu).
     * Luetaan eikä koota numerosta, samasta syystä kuin [PlayerRef.profilePath].
     */
    val eventPath: String? = null,
    /** Kierros otsikosta, esim. "Round 4". */
    val round: String?,
    /** Ottelun pituus pisteinä, luettu tekstistä "9 Point Match". */
    val matchLength: Int?,
    val cube: Cube?,
    val dice: List<Die>,
    /**
     * Käyttäjän `Board Scheme` sivun omista kuvapoluista, tai `null` jos hakemisto ei ole
     * mikään tunnetuista.
     *
     * Tämä on olemassa yhtä kysymystä varten: **miksi lauta ei ole luettavissa.** [points]
     * jää Minillä tyhjäksi, ja tyhjä lista yksin ei kerro syytä, koska se on erottamaton
     * laudasta jolla ei ole yhtään nappulaa. Ruutu neuvoi silti vaihtamaan skeeman, eli se
     * päätteli syyn seurauksesta. Nyt syy luetaan sivulta.
     *
     * `null` on siis eri asia kuin [BoardScheme.MINI] eikä sen varovaisempi muoto: se
     * tarkoittaa ettei sivu sanonut mitään tunnistettavaa, ja silloin skeemaa ei saa
     * syyttää.
     */
    val scheme: BoardScheme?,
    /** 24 pistettä, indeksi 0 = piste 1. */
    val points: List<Point>,
    val players: List<PlayerPanel>,
    /**
     * Tilanneteksti laudan alta, esim. "Please make a checker move." tai
     * "<nimi> declines the cube action.". Sivulla on lisäksi vakiorivi
     * "Please select your action/make your move:", joka on joka sivulla samana eikä siksi
     * kerro tilanteesta mitään; se on tämän varalla vain jos tilannetekstiä ei ole.
     */
    val prompt: String?,
    /**
     * Palvelimen ilmoitukset laudan yläpuolelta, sellaisenaan ja järjestyksessä.
     *
     * Nämä ovat sivulla paljaana tekstinä ilman omaa elementtiä, joten ne katosivat
     * jäsennyksestä kokonaan 1.8.2026 asti. Tunnettu tapaus on peruutus, ks. [rolledBack],
     * mutta säiliö on tarkoituksella yleinen: muita ilmoituksia ei ole vielä nähty, eikä
     * niiden sanamuotoa voi siksi arvata.
     */
    val notices: List<String>,
    /**
     * Sivu kertoi että peli on palautettu taaksepäin, koska vastustaja teki muun kuin
     * koneen arvaaman siirron.
     *
     * Tämä ei ole kosmeettinen tieto vaan syy epäillä tallennettua tilaa: aiemmin tallennettu
     * lauta samasta ottelusta voi olla peräisin haarasta jota ei enää ole. Siksi lippu on
     * omanaan eikä pelkkänä tekstinä [notices]issa, vaikka teksti on sielläkin.
     *
     * Kahdentumisen hinta maksetaan esitettäessä eikä täällä, ks. [plainNotices].
     */
    val rolledBack: Boolean,
    /**
     * Lauta on sivuston arvaus tulevasta asemasta, ei ottelun nykytila: sivu näyttää mihin
     * peli etenisi ja kysyy siirtoa etukäteen ([isSpeculativePrompt]).
     *
     * **Ei virhe vaan sivuston oma toiminto** (`SUBSTANSSI.md` kohta 77): arvaus kattaa
     * neljä tilannetta ja enintään neljä siirtoa, ja siihen vastaaminen on käyttöä. Sivu
     * torjuttiin hetken 27.8.2026, mutta kaapatuista siirtovastauksista 110 kappaletta
     * 312:sta on tällaisia, eli torjunta olisi vienyt joka kolmannen siirron virheruutuun.
     *
     * Lippu on omanaan eikä pelkkänä [prompt]ina samasta syystä kuin [rolledBack]: se on syy
     * epäillä sitä mitä ruudulla näkyy, ja [pipCount]in kaltaiset johdokset koskevat asemaa
     * jota ei välttämättä koskaan tule. Kahdentumisen hinta maksetaan esitettäessä, ks.
     * [situation].
     *
     * **Ruutu ei piirrä lipusta mitään 1.9.2026 alkaen** (Tommin päätös, `docs/UI.md`). Lippu
     * jää silti tänne, koska malli kantaa sen mitä sivu sanoi ja näkymä päättää mitä siitä
     * näytetään. Jäsentimen pudottamana tieto katoaisi kokonaan, ja se on eri väite kuin
     * *"tätä ei näytetä"*.
     */
    val speculative: Boolean,
    /** Pisteet joilta voi juuri nyt siirtää, sivun omine linkkeineen. */
    val moves: List<MoveLink>,
    /** Muut komennot kuin nappulan siirto, esim. "Swap Dice". */
    val commands: List<CommandLink>,
    /**
     * Sivun lomake, eli ne toiminnot jotka eivät ole linkkejä. Null kun sivulla ei ole
     * lomaketta lainkaan.
     *
     * Tämä oli jäsentimen katve 4.8.2026 asti: siirtojen kokoaminen on linkkejä, mutta
     * **jokainen vuoron päättävä ja kuutiota koskeva toiminto on lomakkeen nappi**, eikä
     * yhtäkään niistä siis nähty.
     */
    val form: BoardForm?,
    /**
     * "Undo Move" -linkin osoite, tai null kun mitään ei ole koottuna.
     *
     * Huom mitä se tekee: osoite on tilatunniste ilman `move`-parametria, eli se **tyhjentää
     * koko kokoamisen** eikä peruuta viimeistä nappulaa. Mitattu 4.8.2026 sekä kolmen että
     * neljän kirjaimen kohdalta: kummassakin osoite oli sama tyhjä.
     */
    val undoHref: String?,
    /**
     * `Skip Game` -linkin osoite, tai null kun sivu ei tarjoa sitä.
     *
     * **Tämä on erillään [commands]ista, koska se on ainoa laudan linkki joka vaihtaa
     * ottelun.** Muut komennot (`Swap Dice`) pysyvät samassa ottelussa, ja portti joka
     * torjuu väärän ottelun on olemassa juuri siksi ettei sivuston oma siirtymä
     * seuraavaan peliin piirtyisi hiljaa väärän otsikon alle. Skip on sama laji kuin
     * lomakkeen `Next Game`, ja se saa saman poikkeuksen samalla ehdolla: painallus on
     * käyttäjän, ja vain painettu nappi siirtää odotuksen.
     *
     * **Osoite kantaa merkityksen, ei teksti.** Linkki on `/bg/nextgame?skip=<ottelu>`,
     * eli se on jonoa kuluttava osoite, ja juuri siksi se tunnistetaan osoitteesta eikä
     * sanasta `Skip Game`: sanaan sidottu ehto kaatuisi hiljaa jos sivu vaihtaisi sanan,
     * ja silloin nappi tekisi yhä saman kuluttavan teon väärällä nimellä.
     */
    val skipHref: String? = null,
    /**
     * Ulos kannetut nappulat väreittäin.
     *
     * Väri luetaan **kuvatiedoston nimestä eikä ALTista**, ja se on poikkeus samaan sääntöön
     * kuin nopat. Syy on mitattu 4.8.2026: ALT riippuu paikasta eikä sisällöstä, joten
     * `off_y3_bot` on `ALT="y3"` mutta `off_b3_top` on `ALT="3"` ilman värikirjainta.
     * ALTiin nojaava luku menettäisi siis puolet tapauksista hiljaa.
     */
    val borneOff: List<BorneOffCheckers>,
    /**
     * Muurilla (bar) olevat nappulat väreittäin.
     *
     * Väri luetaan **kuvatiedoston nimestä eikä ALTista**, ja syy on täsmälleen sama kuin
     * [borneOff]illa: ALT riippuu paikasta eikä sisällöstä. Mitattu 5.8.2026 fixtureista,
     * ja sama tiedosto `bar_b1.gif` kantaa kahta eri ALTia: `move_board_oikea.html` ja
     * `move_board_vasen.html` sanovat `b1`, mutta `move_rollback.html` sanoo `1` ilman
     * värikirjainta. Kanoni väitti 1.8.-5.8.2026 että muuri käyttää samaa ALT-koodia kuin
     * pisteet; se piti paikkansa siitä yhdestä otoksesta jonka perusteella se kirjoitettiin.
     *
     * Muuri ei ole piste eikä siis mukana [points]issa. Ilman tätä kenttää asema on hiljaa
     * vajaa aina kun jollakin on nappula muurilla, ja [pipCount] alittaa paneelin luvun
     * tasan 25:llä nappulaa kohti.
     */
    val bar: List<BarCheckers>,
    /**
     * Toistoa odottavien haarojen määrä double repeat -ottelussa, tai null kun sivulla ei
     * ole riviä `Pending Replay:`.
     *
     * Sivulla rivi on nimiö ja linkki (`_1_`), ja linkki vie haarautumishetkeen eli asemaan
     * jossa vastustaja hyväksyi DR:n (`docs/KOHDE.md`, mitattu 9.9.2026). Sovellus näyttää
     * vain luvun eikä linkkiä (Tommin päätös 9.9.2026: *"desktop selain välilehtineen on
     * tähän paljon parempi, mutta tärkeä näyttää kuinka monta pending replayta on"*).
     * Nimiö ilman lukua näkyi laitteella 9.9.2026, koska linkki on elementti ja putosi
     * [notices]ista samalla tavalla kuin punainen rivi 3.9.2026 asti. Merkitys:
     * `SUBSTANSSI.md` kohta 17.
     */
    val pendingReplays: Int? = null,
) {
    /**
     * Ilmoitukset joilla ei ole omaa esitystä, eli [notices] ilman sitä joka on jo luettu
     * [rolledBack]-lipuksi.
     *
     * **Tämä on esitysvalinta eikä jäsennystä**, ja siksi se on johdettu kenttä eikä
     * suodatus [notices]iin: säiliö on tarkoituksella yleinen, koska tuntemattoman
     * ilmoituksen sanamuotoa ei voi arvata, ja tunnetun poisto säiliöstä olisi juuri se
     * lauseeseen sidottu jäsennin jota kanoni kieltää.
     *
     * *Miksi kenttä on olemassa.* Peruutus näkyi laitteella 10.8.2026 kahdesti samalla
     * ruudulla: kerran sovelluksen omana varoituspalkkina ja kerran sivuston omana lauseena
     * laudan alla. Kumpikin oli oikein yksinään, ja yhdessä ne saivat lukijan etsimään kahta
     * eri tapahtumaa. Havainto on Tommin, ja se tuli laitteelta eikä testeistä.
     */
    val plainNotices: List<String> get() = notices.filterNot(::isRolledBackNotice)

    /**
     * Kehote silloin kun se kertoo tilanteesta, tai null kun sivulla oli vain vakiorivi.
     *
     * **Vakiorivi jätetään piirtämättä 24.8.2026 alkaen** (Tommin päätös laitteelta:
     * *"antaa nappien ohjata"*). "Please select your action/make your move:" on joka sivulla
     * sama, eli se on sivun kalusteita eikä tämän ottelun tietoa, ja ruudulla se vie tilaa
     * kertomatta mitään mitä napit eivät jo sano. Tilannetekstit kuten
     * "<nimi> declines the cube action." eivät ole tätä lajia, ja ne piirretään ennallaan.
     *
     * Rajaus on tässä eikä jäsentimessä, samasta syystä kuin [plainNotices]: malli kantaa sen
     * mitä sivu sanoi, ja näkymä päättää mitä siitä näytetään. Jäsennin joka pudottaisi rivin
     * hävittäisi tiedon siitä että sivu ylipäätään vastasi, ja se on eri väite.
     */
    val situation: String? get() = prompt
        ?.takeUnless(::isStandardPrompt)
        // Arvauskysymys on lautasivulla `body > b`, eli täsmälleen se rivi jonka [prompt]
        // lukee silloin kun `h4` puuttuu. Sitä ei näytetä, ja peruste vaihtui 1.9.2026:
        // aiemmin siksi että lippu sai oman palkkinsa, nyt siksi ettei arvatusta asemasta
        // sanota ruudulla mitään lainkaan (Tommin päätös, ks. `docs/UI.md`). Rajaus jää siis
        // tänne vaikka palkki poistui, ja se on nyt tämän lauseen ainoa suodatin.
        // Sama valinta ja sama syy kuin [plainNotices]issa.
        ?.takeUnless(::isSpeculativePrompt)


    /**
     * Paljonko pelaajalta puuttuu ottelun voittoon, eli away-notaation luku.
     *
     * **Merkitsevä suure on puuttuva eikä saatu piste** (Tommi 16.8.2026, `SUBSTANSSI.md`
     * kohdat 82, 90 ja 92). Saadut pisteet kertovat mitä on tapahtunut, ja päätöksen kannalta
     * merkitsevä on se paljonko kummaltakin puuttuu: gammon-go ja gammon-save eivät ole
     * pelityylejä vaan asemia jotka luetaan juuri näistä luvuista. Merkintä `4a-2a` on
     * analyysin vakiintunutta kieltä eikä sivuston omaa, joten se ei tarvitse selitystä
     * pelaajalle.
     *
     * Tämä on johdos eikä luettu kenttä, ja se asuu tässä eikä piirtokoodissa, koska se on
     * lajin sääntö. Sivusto ei kirjoita away-lukua mihinkään.
     *
     * `null` kolmessa tapauksessa, eikä yhtäkään niistä arvata: ottelulla ei ole pituutta
     * (rahapeli), pistettä ei saatu luettua, tai luku menisi nollaan tai sen alle. Viimeinen
     * ei ole mahdollinen ehjällä sivulla, koska ottelu päättyy kun luku saavuttaa nollan, joten
     * epäkelpo tulos on merkki väärin luetusta eikä tilanteesta jota kannattaisi näyttää.
     */
    fun awayOf(panel: PlayerPanel): Int? {
        val length = matchLength ?: return null
        val score = panel.score ?: return null
        return (length - score).takeIf { it > 0 }
    }

    /**
     * Avausheitto, eli se vaihe jonka sivusto ohittaa (`SUBSTANSSI.md` kohta 104, Tommin
     * toive 14.9.2026: *"dailygammon vaan ohittaa tämän vaiheen, joka olisi minusta kiva
     * tuoda peliin mukaan"*).
     *
     * **Mistä se luetaan.** Jokaisen pelin aloittaja arvotaan yhdellä nopalla per pelaaja,
     * ja isomman heittänyt aloittaa pelaamalla nuo kaksi silmälukua. DailyGammon tekee
     * heiton palvelimella ja antaa ensimmäisen lautasivun jo aloittajan vuorona nopat
     * heitettyinä, molemmat hänen puolellaan (mitattu 14.9.2026, 19 sivua korpuksesta).
     * Tieto on siis sivulla kokonaan: isompi noppa on aloittajan, koska juuri se antoi
     * vuoron, ja pienempi on vastustajan.
     *
     * **Pelin alku tunnistetaan asemasta eikä siirtonumerosta.** Otsikon `Move n` juoksee
     * ottelun läpi (`Move 579` on mahdollinen vain ottelussa), joten se tunnistaisi vain
     * ottelun ensimmäisen pelin, ja Tommin tarkennus samana päivänä koski jokaista peliä:
     * *"kunkin pelin aloittaja selviää sillä kumpi heittää isomman nopan omalle
     * pelipuolelleen"*. Aloitusasema ei toistu pelin aikana, joten ehto ei voi laueta
     * keskellä peliä, ja se on sama riippumatta siitä kumpi väri on kummankin.
     *
     * `null` kun sivu ei ole pelin alku, nopat eivät ole kaksi eri silmälukua yhdellä
     * omistajalla, tai asema on luettu vajaana. Tasatilanteita ei keksitä: sivu näyttää
     * vain lopullisen heiton (kohta 104), ja tämä näyttää saman.
     */
    val openingRoll: OpeningRoll?
        get() {
            if (dice.size != 2) return null
            val (first, second) = dice
            val starter = first.owner ?: return null
            if (second.owner != starter || first.value == second.value) return null
            if (!isStartingPosition()) return null
            return OpeningRoll(
                starter = starter,
                starterValue = maxOf(first.value, second.value),
                otherValue = minOf(first.value, second.value),
            )
        }

    /**
     * Sama lauta nopat avausheiton esityksessä, eli pienempi noppa vastustajan omistuksessa
     * ja siten hänen puolellaan; muulloin lauta sellaisenaan. Ks. [openingRoll].
     */
    fun withOpeningDice(): BoardState = openingRoll?.let { copy(dice = it.present(dice)) } ?: this

    /**
     * Onko asema pelin lähtöasema: kummallakin 2, 5, 3 ja 5 nappulaa omilla lähtöpisteillään,
     * ei mitään muurilla eikä ulkona. Väristä riippumaton, koska sivun numerointi on kiinteä
     * ja värit vaihtuvat otteluittain: toinen väri on pisteillä 1, 12, 17 ja 19, toinen
     * pisteillä 24, 13, 8 ja 6, ja kumpi on kumpi ei vaikuta.
     */
    fun isStartingPosition(): Boolean {
        if (bar.any { it.count > 0 } || borneOff.any { it.count > 0 }) return false
        val occupied = points.filter { it.count > 0 }
        if (occupied.size != START_NEAR.size + START_FAR.size) return false
        val near = occupied.filter { it.number in START_NEAR }
        val far = occupied.filter { it.number in START_FAR }
        if (near.size != START_NEAR.size || far.size != START_FAR.size) return false
        if (near.any { it.count != START_NEAR[it.number] }) return false
        if (far.any { it.count != START_FAR[it.number] }) return false
        val nearColor = near.map { it.owner }.toSet().singleOrNull() ?: return false
        val farColor = far.map { it.owner }.toSet().singleOrNull() ?: return false
        return nearColor != farColor
    }

    private companion object {
        /** Lähtöpisteet ja määrät sille värille joka kulkee kohti isoja numeroita. */
        val START_NEAR = mapOf(1 to 2, 12 to 5, 17 to 3, 19 to 5)

        /** Sama sille värille joka kulkee kohti pieniä numeroita. */
        val START_FAR = mapOf(24 to 2, 13 to 5, 8 to 3, 6 to 5)
    }

    /**
     * Onko käynnissä Crawford-peli, eli se yksi peli jossa kuutio ei ole kummankaan käytössä.
     *
     * **Sivu merkitsee sen kahdella tavalla yhtä aikaa, ja molemmat luetaan** (Tommin valinta
     * 14.8.2026). Johtajan pistemäärän perässä on tähti ([PlayerPanel.scoreLabel]) ja
     * kuutiokuva puuttuu sivulta kokonaan ([cube] on null). Mitattu koko kaapatusta
     * korpuksesta 14.8.2026: 648 lautasivua, joista 39 kantoi molemmat merkit ja 609
     * kumpaakaan, eikä yhtään sivua ollut jolla merkit olisivat eri mieltä.
     *
     * *Miksi kaksi merkkiä eikä toinen niistä.* Kumpikin yksin riittäisi mitattuun aineistoon,
     * eikä kumpikaan yksin kertoisi mitään siinä hetkessä jolloin sivusto muuttuu. Kaksi
     * merkkiä maksaa tässä lähes nolla, koska kuutio luettiin ennestään ja uutta lukukohtaa
     * tuli yksi.
     *
     * *Miksi tähti ei riitä väitteeksi pistetilanteesta.* Tähti ei tarkoita pistettä vaille
     * voittoa vaan sitä yhtä peliä: korpuksessa on 19 sivua joilla joku on `n-1`:ssä mutta
     * kuutio on tallella eikä tähteä ole, eli Crawford on jo pelattu. Ottelun pituuden ja
     * pisteiden vertailu antaisi siis eri vastauksen kuin tämä, ja se olisi eri kysymys.
     *
     * *Double repeat luopuu Crawford-säännöstä.* DR-ottelussa kuutio on sivulla
     * ([Cube.label] on `dr` tai vastaava) eikä tähteä tule, joten tämä sanoo [CUBE_IN_PLAY]
     * eikä [UNKNOWN]. Se on oikea vastaus eikä puuttuva: säännön poissaolo on juuri se mitä
     * DR:stä on kirjattu, ks. sivuston oma `dr.html`.
     */
    val crawford: CrawfordReading
        get() {
            val star = players.any { it.scoreLabel?.contains('*') == true }
            val cubeMissing = cube == null
            return when {
                star && cubeMissing -> CrawfordReading.CRAWFORD
                !star && !cubeMissing -> CrawfordReading.CUBE_IN_PLAY
                else -> CrawfordReading.UNKNOWN
            }
        }
}

/**
 * Kertooko lautasivu olevansa Crawford-peli.
 *
 * Kolmas arvo on olemassa siksi, että merkkejä on kaksi ja ne voivat olla eri mieltä. Silloin
 * ei arvata kumpaankaan suuntaan, sama sääntö kuin [CubePosition.UNKNOWN]illa: puuttuva tieto
 * on turvallisempi kuin väärä, koska kuution käytettävyys on pelin sääntö eikä koriste.
 */
enum class CrawfordReading {
    /** Molemmat merkit sanovat Crawford: tähti pistekentässä ja kuutio poissa sivulta. */
    CRAWFORD,

    /** Molemmat merkit sanovat ettei ole: kuutio on sivulla eikä tähteä ole. */
    CUBE_IN_PLAY,

    /** Merkit ovat eri mieltä, tai sivulta ei saatu kumpaakaan. */
    UNKNOWN,
}

/**
 * Onko ilmoitus se peruutusilmoitus jonka [BoardState.rolledBack] jo kantaa.
 *
 * Koko lause on "Your opponent made an unexpected move, and the game has been rolled back to
 * that point.", mutta tunnistus nojaa sen vakaimpaan osaan: alkuosassa on vastustajan teko ja
 * loppuosassa välimerkkejä, keskellä on itse tapahtuma.
 *
 * Tuntomerkki asuu täällä eikä jäsentimessä, koska sitä lukee nyt kaksi eri kohtaa: jäsennin
 * asettaa lipun ja näkymä jättää saman lauseen piirtämättä. Kaksi kopiota samasta säännöstä
 * ajautuisi erilleen, ja seuraus olisi juuri se kahdennus joka tässä korjataan.
 */
fun isRolledBackNotice(notice: String): Boolean = ROLLED_BACK_NOTICE.containsMatchIn(notice)

private val ROLLED_BACK_NOTICE = Regex("""rolled back""", RegexOption.IGNORE_CASE)

/**
 * Onko teksti se kysymys jolla sivusto kertoo näyttävänsä arvatun tulevan aseman.
 *
 * Koko lause on "What will you do if the game proceeds this way?", ja tunnistus nojaa sen
 * keskiosaan: alussa on puhuttelu ja lopussa kysymysmerkki, keskellä on itse väite siitä
 * että peli **etenisi** näin. Sama valinta ja sama syy kuin [isRolledBackNotice]issa.
 *
 * Tuntomerkki asuu täällä eikä jäsentimessä, koska sitä lukee kolme kohtaa: `BoardParser`
 * asettaa lipun, `ChatParser` lukee saman sivun chat-osan, ja [BoardState.situation] jättää
 * lauseen piirtämättä toistamiseen. Kolme kopiota samasta säännöstä ajautuisi erilleen.
 */
fun isSpeculativePrompt(text: String): Boolean = SPECULATIVE_PROMPT.containsMatchIn(text)

private val SPECULATIVE_PROMPT =
    Regex("""the game proceeds this way""", RegexOption.IGNORE_CASE)

/**
 * Onko kehote se joka sivulla on aina, riippumatta tilanteesta.
 *
 * Koko rivi on "Please select your action/make your move:", ja tunnistus nojaa sen alkuun:
 * loppuosassa on kauttaviiva ja kaksoispiste, eli juuri ne merkit joiden pysyvyydestä ei ole
 * näyttöä. Alkuosa on nähty kaikissa kaapatuissa lautasivuissa samana.
 *
 * Tuntomerkki asuu täällä eikä näkymässä samasta syystä kuin [isRolledBackNotice]: sääntö on
 * yksi, ja kaksi kopiota ajautuisi erilleen.
 */
fun isStandardPrompt(text: String): Boolean =
    text.trim().startsWith("Please select your action", ignoreCase = true)

/**
 * Lautasivun lomake sellaisena kuin sivu sen kirjoittaa.
 *
 * Kenttiä ei tulkita eikä osoitetta koota valmiiksi. Sama syy kuin [MoveLink.code]illa:
 * lomake on muuttava toiminto oikeassa ottelussa, ja itse rakennettu osoite ei tuottaisi
 * virhettä vaan väärän teon.
 */
data class BoardForm(
    /** `action`-attribuutti sellaisenaan, esim. "/bg/move/7000012/336". */
    val action: String,
    /** Lomakkeen oma `method`. Lautasivulla se on GET, mutta sitä ei oleteta täällä. */
    val method: FormMethod,
    /**
     * Piilokentän `move`-arvo, esim. "rrmm", tai null kun kenttää ei ole.
     *
     * Kentän **olemassaolo on merkitsevä**: se ilmestyy vasta kun siirto on täysi. Kesken
     * kootussa siirrossa (`?move=m`) sivulla on vain "Undo Move", ei piilokenttää eikä
     * lähetysnappia. Sivu siis kertoo itse milloin siirron voi lähettää, eikä sitä tarvitse
     * päätellä nopista.
     */
    val pendingMove: String?,
    /**
     * Lähetysnappien `value`-arvot sivun järjestyksessä, esim. ["Accept", "Decline"] tai
     * ["Submit Greedy Bearoff"]. Nappien nimi on aina `submit`.
     */
    val submits: List<String>,
    /**
     * Valintaruudun (`name=verify`) arvo, tai null kun ruutua ei ole.
     *
     * **Arvo on sivukohtainen vakio eikä seuraa painettua nappia.** Tuplausta tarjottaessa
     * se on "Accept" ja heittosivulla "Double", vaikka samalla sivulla on myös Decline ja
     * Roll Dice. Tästä seurasi kanonin korjaus 4.8.2026: aiempi sääntö väitti että
     * `?submit=Decline` tulee ilman `verify`iä, mutta mitattu osoite oli
     * `?submit=Decline&verify=Accept`. Vanha sääntö oli päätelty otoksesta jossa ruutu
     * sattui olemaan rastittamatta, ei sivun rakenteesta.
     */
    val verify: String?,
    /**
     * Lomakkeen **kaikki** piilokentät sellaisinaan, myös [pendingMove]n oma `move`.
     *
     * **Lisätty 22.8.2026 mitatun vian takia, ja vika oli hiljainen.** `Next Game`
     * -painallus lähti kahdesti oikeaan osoitteeseen ja sivusto vastasi 200, mutta palautti
     * tavulleen saman sivun (sha `7d0c2437caf8` kolmella peräkkäisellä vastauksella).
     * Syy näkyi vasta proxyn kenttälokista: POSTissa oli vain `submit`, kun sivun lomake on
     * `<input type=hidden name=commit value=1>` ja nappi sen sisällä. Kenttä oli siis
     * pudotettu, eikä mikään kertonut siitä, koska teko epäonnistui onnistumisen näköisenä.
     *
     * **Tieto oli projektissa jo, väärässä paikassa.** `ChatPage.hiddenFields` on olemassa
     * nimenomaan tämän saman `commit=1`:n takia, mutta lautalomake ei perinyt sitä. Tämä ei
     * siis ole uusi oletus vaan sama ratkaisu toisella sivulla.
     *
     * Kenttiä ei luetella eikä suodateta: selain lähettää lomakkeen omat piilokentät
     * sellaisinaan, ja luettelo olisi tässä sama arvaus joka pudotti `commit`in.
     *
     * Oletusarvo on tyhjä, jotta olemassa olevat kutsupaikat ja testit kuvaavat lomaketta
     * jossa piilokenttiä ei ole. Kenttä on viimeisenä samasta syystä.
     */
    val hiddenFields: Map<String, String> = emptyMap(),
)

/**
 * Kaikki osoitteet jotka **tämä lauta itse tarjoaa**, sellaisinaan.
 *
 * Tämä on portin lukupuoli: hyväksytty seuraava osoite on jäsen tässä joukossa, eikä mikään
 * muu kelpaa. Joukko syntyy jäsennetystä sivusta, joten se vanhenee samalla hetkellä kuin
 * lauta: edellisen laudan linkki ei kelpaa uudella laudalla, mikä on oikein, koska sivuston
 * tilatunniste on osa osoitetta.
 *
 * Mukana on myös [BoardState.undoHref], vaikka se tyhjentää kokoamisen: se on peruminen eikä
 * lähetys, ja sivu tarjoaa sen samalla tavalla linkkinä kuin siirrotkin.
 *
 * Mukana on myös [BarCheckers.href], eli muurilta laudalle tuominen. **Se puuttui 22.8.2026
 * asti, ja puuttuminen oli portin toinen puolisko samasta viasta:** jäsennin ei lukenut
 * muurin linkkiä lainkaan, joten se ei voinut päätyä tähänkään joukkoon. Kun jäsennin
 * korjattiin, ruutu piirsi kosketettavan alueen mutta portti hylkäsi napautuksen hiljaa,
 * koska osoite ei ollut jäsen. Vika oli siis nähtävissä vasta laitteella: käännös meni läpi,
 * napautus rekisteröityi logcatiin, eikä mitään tapahtunut.
 *
 * Lomake ei ole tässä. Se ei ole osoite vaan lomake, ja se kulkee [BoardForm.press]in kautta.
 */
fun BoardState.ownLinks(): Set<String> =
    buildSet {
        moves.forEach { add(it.href) }
        commands.forEach { add(it.href) }
        bar.forEach { checkers -> checkers.href?.let { add(it) } }
        undoHref?.let { add(it) }
        skipHref?.let { add(it) }
    }

/**
 * Valmis lähetys: sivun oma osoite ja sivun omat kentät, valmiina menemään ulos.
 *
 * **Konstruktori on `internal`, ja se on tämän tyypin koko tarkoitus.** Ainoa tie tähän
 * arvoon on [BoardForm.press], eli sivun itsensä tarjoaman napin painaminen. Mikään muu
 * moduuli ei voi koota lähetystä, ei `:app`, ei `core-net`, ei testi. Se on käännösaikainen
 * tae, ja se on kirjoitettu tähän korvaamaan se tae joka purkautui portin mukana:
 * `BoardScreen`in lukutila oli aiemmin tae siitä ettei toimintoa ollut olemassakaan.
 *
 * Tae on nimenomaan **kokoamattomuus** eikä turvallisuus yleensä. Väärän napin painaminen on
 * yhä mahdollista, koska se on käyttäjän oikeus; keksityn napin painaminen ei ole.
 */
enum class FormMethod {
    GET,
    POST,
    ;

    companion object {
        /**
         * Lomakkeen `method`-attribuutista, kirjainkoosta riippumatta.
         *
         * **Tyhjä attribuutti on GET, eikä se ole oletus vaan HTML:n oma sääntö.** Sivusto
         * käyttää molempia: siirtolomake on `method=get`, kun taas chat, kirjautuminen ja
         * pikaviestin lähetys ovat POSTeja. Metodi luetaan siis sivulta samalla perusteella
         * kuin kenttien nimet, koska väärä metodi ei tuota virhettä vaan teon joka ei mene
         * perille.
         */
        fun from(attribute: String): FormMethod =
            if (attribute.trim().equals("post", ignoreCase = true)) POST else GET
    }
}

class FormSubmission internal constructor(
    /** Lomakkeen `action` sellaisenaan, esim. "/bg/move/7000011/117". */
    val action: String,
    /** Lomakkeen oma metodi. Luetaan sivulta, ei valita täällä. */
    val method: FormMethod,
    /**
     * Kentät nimineen ja arvoineen, kaikki sivulta luettuja. Järjestys on sivun järjestys,
     * eli piilokenttä ennen nappia, koska lähtevä osoite on silloin sama kuin selaimen.
     */
    val fields: Map<String, String>,
) {
    override fun toString(): String = "FormSubmission($method $action, $fields)"
}

/**
 * Painaa yhtä sivun tarjoamista napeista.
 *
 * Palauttaa null kun [submit] ei ole [BoardForm.submits]issa, ja se on portin toinen puoli:
 * nappia jota sivu ei tarjoa ei voi painaa, eikä kutsupaikka voi keksiä sellaista. Käytännön
 * seuraus: siirtoa ei voi lähettää ennen kuin sivu itse kertoo sen olevan täysi, koska
 * `Submit Move` ilmestyy nappilistaan vasta silloin.
 *
 * [verify] vastaa sivun valintaruutua ("Verify Double", "Verify Accept"), ja se on
 * **käyttäjän valinta eikä sovelluksen politiikka**. Ruutu on sivulla rastittamaton, joten
 * automaattinen rastitus olisi peruuttamattoman teon vahvistamista käyttäjän puolesta.
 * Ruudutonta lomaketta ei voi vahvistaa: silloin arvoa ei ole mistä lukea, ja keksitty arvo
 * olisi täsmälleen se mitä tämä tyyppi estää.
 */
fun BoardForm.press(submit: String, verify: Boolean = false): FormSubmission? {
    if (submit !in submits) return null

    val fields = buildMap {
        // Sivun omat piilokentät ensin ja sellaisinaan. Ne ovat lomakkeen osa siinä missä
        // napitkin, ja niiden pudottaminen tuottaa pyynnön joka näyttää onnistuvan mutta ei
        // tee mitään (ks. [BoardForm.hiddenFields]).
        putAll(hiddenFields)
        // Piilokenttä sellaisenaan. Sen arvo on palvelimen pitämä kokoamistila, eikä sitä
        // lasketa täällä sen enempää kuin muutakaan. Tämä on `hiddenFields`in jäsen, ja se
        // asetetaan silti erikseen: kenttä on merkitsevä omana tietonaan, eikä sen arvo saa
        // riippua siitä osuiko jäsennin piilokenttien yleiseen valintaan.
        pendingMove?.let { put("move", it) }
        put("submit", submit)
        if (verify) this@press.verify?.let { put("verify", it) }
    }
    return FormSubmission(action = action, method = method, fields = fields)
}

/**
 * Tuplauskuutio sivulta luettuna.
 *
 * **Oli `Int?` 9.8.2026 asti, ja tyyppi hävitti kuution hiljaa.** Arvo luettiin
 * `alt.toIntOrNull()`illa, joten jokainen ei-numeerinen ALT muuttui nulliksi eikä null
 * eronnut mitenkään siitä tapauksesta jossa sivulla ei ole kuutiokuvaa lainkaan. Laiteajossa
 * kuutio ei piirtynyt oikeassa ottelussa, ja mittaus kuudesta ottelusta kertoi syyn:
 * `cubedr.gif` kantaa `ALT="dr"`. Kuvassa lukee kirjaimet `DR`.
 *
 * Siksi [label] on ALT sellaisenaan ja [value] vain se osa joka on luku. Sama muoto kuin
 * `InboxItem`in tuntemattomalla otsikolla: tuntematonta ei arvata eikä pudoteta, se
 * säilytetään.
 */
data class Cube(
    /** Kuvan ALT sellaisenaan, esim. "2" tai "dr". Aina tallessa. */
    val label: String,
    /** Numeroarvo kun ALT on luku, muuten null. */
    val value: Int?,
    /**
     * Kuutio on double repeat -tarjous (`cubedr.gif` tai `cubedrs.gif`), luettuna
     * tiedostonimestä eikä ALTista, samasta syystä kuin [value]: sama kuva kantaa eri
     * sivuilla eri ALTin (`dr` 192 sivulla, `cubedrs` 13:lla, mitattu 3.9.2026 koko
     * korpuksesta), ja ALTista luettuna kuutiossa luki laitteella `CUBEDRS`. Mitä DR
     * tarkoittaa: `SUBSTANSSI.md` kohta 17, se ei ole kuutio vaan haarautus.
     */
    val doubleRepeat: Boolean = false,
    /**
     * Kuution solun pystysuuntainen paikka laudan vasemmassa harmaassa sarakkeessa.
     *
     * **Tämä on omistajuus, ja se mitattiin 9.8.2026** kuudesta oikeasta ottelusta: arvon 1
     * ja `dr`:n solu oli keskimmäinen (`VALIGN=middle`), ja molempien kakkoskuutioiden solu
     * oli ylin (`VALIGN=top`), eli sama solu joka omistamattomalla sivulla on tyhjä. Kanoni
     * oli nimennyt juuri tämän kokeen ratkaisijaksi.
     *
     * Paikka luetaan solun omasta `VALIGN`ista eikä rivi-indeksistä laskien, samasta syystä
     * kuin pistenumero luetaan sivun numerorivistä: laskettu paikka on oletus taulukon
     * muodosta, luettu ei.
     */
    val position: CubePosition,
)

/**
 * Kuution solun paikka pystysuunnassa.
 *
 * **Nimet ovat sivun geometriaa eivätkä pelaajia**, ja ero on tarkoituksellinen. Se kumpi
 * pelaaja on sivun ylälaidassa ei ole tästä pääteltävissä, ja arvattu yhteys tekisi
 * omistajuudesta väitteen jota mikään mittaus ei kata. Ks. avoin kohta kuution omistajuudesta.
 */
enum class CubePosition {
    TOP,
    MIDDLE,
    BOTTOM,

    /** `VALIGN` puuttui tai oli jokin muu. Ei arvata kumpaan suuntaan. */
    UNKNOWN,
}

data class BorneOffCheckers(
    val color: CheckerColor,
    val count: Int,
)

/**
 * Muurilla olevat nappulat yhdeltä väriltä.
 *
 * Muoto on tarkoituksella sama kuin [BorneOffCheckers]illa: sama ongelma ja sama ratkaisu,
 * ja poikkeava muoto kysyisi lukijalta perustelun jota ei ole.
 */
data class BarCheckers(
    val color: CheckerColor,
    val count: Int,
    /**
     * Sivun oma linkki jolla nappula tuodaan muurilta laudalle, tai `null` jos sivu ei
     * tarjoa sitä (vastustajan nappula, ei vuoroa, tai kaikki nopat käytetty).
     *
     * **Tämä ei ole [MoveLink], eikä syy ole tyylillinen.** `MoveLink` vaatii
     * `fromPoint`-numeron, ja muurilla ei ole numeroa. Juuri siksi muurin linkki putosi
     * jäsentimestä hiljaa 22.8.2026 asti: se ei mahtunut siihen tyyppiin jossa siirrot
     * kulkevat, joten se suodattui pois numeroituihin pisteisiin rajaavassa ehdossa.
     * Vaikutus oli estävä eikä kosmeettinen, koska ilman tätä kenttää yhtäkään vuoroa
     * jossa on tullut syödyksi ei voi pelata sovelluksella lainkaan.
     *
     * Kirjain on sivun antama (`?move=y`) eikä laskettu, samasta syystä kuin
     * [MoveLink.code]issa: väärä arvaus tuottaisi väärän siirron eikä virhettä.
     */
    val href: String? = null,
)

/**
 * Pip-luku eli montako silmälukua värillä on jäljellä maaliin.
 *
 * Tämä ei ole vain tilastoa vaan **jäsennyksen tarkistuskeino**: sivu kertoo pip-luvun myös
 * pelaajapaneelissa, eli laudan ja paneelin on pakko täsmätä. Jos pisteiden numerointi,
 * omistajuus tai suunta menee pieleen, summa lakkaa täsmäämästä. Kaapatussa sivussa
 * keltainen sai 99 ja sininen 117, ja paneelit sanoivat samaa.
 *
 * [homeIsLowNumbers] kertoo kumpaan suuntaan väri kulkee. Sitä ei päätellä värista, koska
 * yhdestä otoksesta ei voi tietää onko värin ja suunnan yhteys vakio. Oikea suunta on
 * pääteltävissä juuri vertaamalla tulosta paneelin lukuun.
 */
fun BoardState.pipCount(color: CheckerColor, homeIsLowNumbers: Boolean): Int =
    points.filter { it.owner == color }
        .sumOf { point ->
            point.count * if (homeIsLowNumbers) point.number else 25 - point.number
        }

/**
 * Pisteiden ja muurin yhteispip, eli se luku jonka kanssa pelaajapaneelin on määrä täsmätä.
 *
 * [pipCount] laskee **vain pisteet** ja jättää muurin pois. Se on sen dokumentoitu sopimus
 * eikä puute, ja se on väitetty kolmessa testiluokassa, joten sitä ei muuteta vaan tämä
 * tulee sen rinnalle.
 *
 * Muurilla oleva nappula on 25 pipiä, ja luku on mitattu eikä oletettu: pelkistä pisteistä
 * laskettu sininen jäi 137:ään kun paneeli sanoi 162, ja erotus oli tasan yksi nappula.
 * Ulos kannettu nappula on 0 pipiä eikä siksi esiinny tässä lainkaan.
 */
fun BoardState.totalPipCount(color: CheckerColor, homeIsLowNumbers: Boolean): Int =
    pipCount(color, homeIsLowNumbers) +
        bar.filter { it.color == color }.sumOf { it.count } * PIPS_FROM_BAR

private const val PIPS_FROM_BAR = 25

/**
 * Nappulamäärien vertailun tulos, eli [PipCheck]in sisarvahti sille alueelle jonka pip
 * jättää katveeseen.
 *
 * **Syy on mitattu 22.8.2026.** Jäsennin pudotti täydet ulos kannettujen pinot, ja ruudulla
 * luki neljä kun nappuloita oli yhdeksän. Pip-vahti ei voinut nähdä sitä lainkaan, koska
 * **ulos kannettu nappula ei tuota pipsejä**: laskettu summa on identtinen väärällä ja
 * oikealla luvulla. Vika oli siis rakenteellisesti näkymätön ainoalle tunnistimelle joka
 * sovelluksessa oli, ja sen huomasi ihminen laudalta.
 *
 * Sama muoto kuin pip-vahdissa ja samasta syystä: tulos on kolmiarvoinen, koska tekemätön
 * tarkistus ei ole sama asia kuin epäonnistunut.
 */
sealed interface CheckerCheck {
    /** Molemmilla väreillä on yhtä monta nappulaa. [count] on se yhteinen luku. */
    data class Agrees(val count: Int) : CheckerCheck

    /** Värit ovat eri mieltä, eli ainakin toisen luenta on vajaa. */
    data class Disagrees(val yellow: Int, val blue: Int) : CheckerCheck

    /** Laudassa ei ole pisteitä (Mini-skeema), joten laskettavaa ei ole. Ei virhe. */
    data object Unavailable : CheckerCheck
}

/**
 * Montako nappulaa värillä on kaikkiaan: pisteillä, muurilla ja ulos kannettuina.
 *
 * Kolme lähdettä, ja juuri se on vahdin arvo. Pip lukee vain ensimmäistä kahta, joten
 * kolmannen katoaminen ei näy siellä millään.
 */
fun BoardState.totalCheckers(color: CheckerColor): Int =
    points.filter { it.owner == color }.sumOf { it.count } +
        bar.filter { it.color == color }.sumOf { it.count } +
        borneOff.filter { it.color == color }.sumOf { it.count }

/**
 * Vertaa värien nappulamääriä **toisiinsa** eikä vakioon viisitoista.
 *
 * Vakio olisi ollut suoraviivaisempi ja väärä. Sivustolla pelataan muunnelmia joiden
 * säännöistä tämä projekti ei tiedä kaikkea (`SUBSTANSSI.md` kohta 2: `nack` ja
 * `double-repeat` ovat yhä auki), eikä nappuloiden lukumäärä ole niissä varmasti sama.
 * Vakioon nojaava vahti hälyttäisi silloin oikeasta laudasta, ja **väärää hälytystä antava
 * vahti lakkaa olemasta vahti**, koska sen ohittamisesta tulee tapa.
 *
 * Symmetria sen sijaan pätee jokaisessa tunnetussa muunnelmassa: pelaajilla on yhtä monta
 * nappulaa, oli niitä montako tahansa. Vertailu on siis riippumaton siitä mitä muunnelmasta
 * tiedetään, ja juuri se oli ehto jonka vakio ei täyttänyt.
 *
 * Hyväksytty katve: jos sama vika pudottaa saman verran molemmilta väreiltä, summat ovat
 * yhtä suuret eikä vahti hälytä. Se on tunnustettu aukko eikä huomaamaton, ja se on silti
 * enemmän kuin nolla: mitattu vika osui vain toiseen väriin, koska toisella ei ollut yhtään
 * ulos kannettua.
 */
fun BoardState.reconcileCheckers(): CheckerCheck {
    if (points.isEmpty()) return CheckerCheck.Unavailable

    val yellow = totalCheckers(CheckerColor.YELLOW)
    val blue = totalCheckers(CheckerColor.BLUE)
    return if (yellow == blue) {
        CheckerCheck.Agrees(yellow)
    } else {
        CheckerCheck.Disagrees(yellow = yellow, blue = blue)
    }
}

/**
 * Laudan ja pelaajapaneelin vertailun tulos.
 *
 * Tämä on sovelluksen ainoa automaattinen tunnistin hiljaa väärään menneelle jäsennykselle:
 * sivun kaksi riippumatonta osaa, kuvat ja paneelin teksti, todistavat toisensa. Siksi
 * tulos on oma tyyppinsä eikä totuusarvo, ja siksi "ei voitu tarkistaa" on eri asia kuin
 * "ei täsmää".
 */
sealed interface PipCheck {
    /** Suunta ratkesi ja summat täsmäsivät paneeliin. */
    data class Agrees(val yellow: Int, val blue: Int) : PipCheck

    /** Sivun kaksi puolta ovat eri mieltä, eli jäsennys on mennyt pieleen. */
    data class Disagrees(val yellow: Int, val blue: Int, val panel: List<Int>) : PipCheck

    /**
     * Vertailua ei voitu tehdä: paneelissa ei ole lukuja (käyttäjäasetus `Hide pip counts`)
     * tai laudassa ei ole pisteitä (Mini-skeema). Kumpikaan ei ole virhe.
     */
    data object Unavailable : PipCheck
}

/**
 * Vertaa laudasta laskettua pip-lukua siihen minkä pelaajapaneelit sanovat.
 *
 * Suuntaa **ei päätellä väristä**, koska yhdestä otoksesta ei voi tietää onko värin ja
 * suunnan yhteys vakio (ks. [pipCount]). Siksi molemmat sijoitukset kokeillaan ja tulosta
 * verrataan paneelin lukupariin. Sivutuotteena tämä on samalla se tapa jolla suunta
 * ylipäätään saadaan selville, koska mallissa ei ole orientaatiokenttää.
 *
 * Symmetrinen asema voi täsmätä molemmilla suunnilla. Se on yhä yhtäpitävyys eikä
 * moniselitteisyys jota pitäisi raportoida: kumpi tahansa luenta antaa samat luvut.
 */
fun BoardState.reconcilePips(): PipCheck {
    val panel = players.mapNotNull { it.pips }
    if (panel.size < 2 || points.isEmpty()) return PipCheck.Unavailable

    val panelSet = panel.toSet()
    // Kaksi mahdollista sijoitusta: joko keltainen kulkee kohti pieniä numeroita tai sininen.
    listOf(true, false).forEach { yellowHomeIsLow ->
        val yellow = totalPipCount(CheckerColor.YELLOW, yellowHomeIsLow)
        val blue = totalPipCount(CheckerColor.BLUE, !yellowHomeIsLow)
        if (setOf(yellow, blue) == panelSet) return PipCheck.Agrees(yellow, blue)
    }

    // Ristiriita raportoidaan ensimmäisellä sijoituksella. Kumpi tahansa kelpaa, koska
    // kumpikaan ei täsmää; olennaista on että luvut näkyvät paneelin rinnalla.
    return PipCheck.Disagrees(
        yellow = totalPipCount(CheckerColor.YELLOW, homeIsLowNumbers = true),
        blue = totalPipCount(CheckerColor.BLUE, homeIsLowNumbers = false),
        panel = panel,
    )
}

/**
 * Yksi piste. [count] on nolla kun piste on tyhjä, jolloin [owner] on null.
 */
data class Point(
    val number: Int,
    val owner: CheckerColor?,
    val count: Int,
)

/**
 * Nappuloiden värit sellaisina kuin sivusto ne merkitsee: kuvan ALT-attribuutti on
 * kirjain ja lukumäärä, esim. `y3` tai `b1`.
 *
 * Väri ei kerro kuka pelaaja on kyseessä. Se on pääteltävä siitä kumman värin noppia
 * heitetään ja millä pisteillä on siirtolinkkejä.
 */
enum class CheckerColor(val code: Char) {
    YELLOW('y'),
    BLUE('b');

    companion object {
        fun fromCode(code: Char): CheckerColor? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Noppa sellaisena kuin se piirretään. [spent] on kokoamisen kuluttama osuus 0..1: sivun
 * omilla nopilla aina 0, ja paikallisessa kokoamisessa pelattu noppa jää näkyviin
 * harmaana sen sijaan että katoaisi (Tommin tilaus 2.9.2026, `docs/ASETUKSET.md`).
 * Välillä 0 ja 1 on vain sivuston kahden kuution tuplaesitys, jossa yksi kuutio kantaa
 * kaksi askelta. Laillisuuteen tai kirjainjonoon arvo ei vaikuta.
 */
data class Die(val value: Int, val owner: CheckerColor?, val spent: Float = 0f)

/**
 * Avausheitto luettuna pelin ensimmäisestä lautasivusta, ks. [BoardState.openingRoll].
 *
 * [starter] heitti [starterValue]n ja aloittaa; vastustajan noppa on [otherValue]. Arvot ovat
 * aina eri, koska tasan heitetään uudestaan eikä sivu näytä sitä vaihetta.
 */
data class OpeningRoll(
    val starter: CheckerColor,
    val starterValue: Int,
    val otherValue: Int,
) {
    val other: CheckerColor get() = CheckerColor.entries.first { it != starter }

    /**
     * Nopat avausheiton esityksessä: vastustajan silmäluku hänen omistukseensa, aloittajan
     * ennallaan. Kulutus ([Die.spent]) kulkee mukana, koska aloittaja pelaa molemmat luvut
     * ja pelattu noppa harmaantuu siellä missä se on.
     */
    fun present(dice: List<Die>): List<Die> =
        dice.map { die -> if (die.value == otherValue) die.copy(owner = other) else die }
}

data class PlayerPanel(
    /** Paneelin pelaaja: nimi ja numero sivun omasta linkistä. */
    val player: PlayerRef,
    val pips: Int?,
    val score: Int?,
    /**
     * Pistekenttä sellaisena kuin sivu sen kirjoittaa, esim. `6` tai `6*`.
     *
     * **Tähti on sivuston oma Crawford-merkintä**, ja se katosi jäsennyksestä 14.8.2026 asti
     * hiljaa: pisteregex luki numerot ja pudotti loput sanomatta mitään. Kenttä on siksi
     * raakana eikä lippuna, samasta syystä kuin [Cube.label]: sivun merkintä säilyy myös
     * silloin kun sen merkitys on vasta osittain tiedossa.
     *
     * Tulkinta on [BoardState.crawford], eikä sitä tehdä tässä: sama merkki on osa kahden
     * merkin lukemaa, eikä toinen niistä ole pelaajan ominaisuus.
     */
    val scoreLabel: String?,
    /**
     * Solun taustaväri raakana, esim. "#3399CC". Arvo säilytetään raakana kuten ennenkin;
     * tulkinta on [checkerColor], ja se on mitattu eikä pääteltyä.
     */
    val backgroundColor: String?,
) {

    /**
     * Pelaajan nappuloiden väri, luettuna nimisolun taustasta.
     *
     * ~~Merkitys ei ole yhdestä otoksesta pääteltävissä.~~ **Mitattu 31.8.2026 illan
     * pelisession sadasta kaapatusta laudasta:** `#3399CC` on sininen ja `#FFFFFF`
     * keltainen. Vertailukohtana oli pisteluvuista päätelty väri niissä laudoissa joissa
     * luvut eroavat, eli riippumaton lähde: **132 yksiselitteistä tapausta, nolla
     * ristiriitaa.** Sama arvo pysyi vakiona ottelun sisällä kolmentoista tilan yli
     * molempien vuoroilla ja vaihteli otteluiden välillä, joten se on puolen merkki eikä
     * vuoron.
     *
     * **Miksi tämä on tärkeämpi kuin se miltä näyttää.** Ennen tätä sovellus päätteli
     * roolin pisteluvuista, ja päättely on epävakaa juuri siksi että [reconcilePips]
     * hyväksyy symmetrisen aseman molemmilla suunnilla: tasapelissä luku ei erota pelaajia,
     * ja rooli kääntyi kesken ottelun. Sivu kertoo saman asian suoraan.
     *
     * Tuntematon arvo on null eikä arvaus, samalla säännöllä kuin [Cube.label].
     */
    val checkerColor: CheckerColor?
        get() = when (backgroundColor?.uppercase()) {
            "#3399CC" -> CheckerColor.BLUE
            "#FFFFFF" -> CheckerColor.YELLOW
            else -> null
        }
}

/**
 * Nappulan siirto lähtöpisteeltä.
 *
 * [code] on sivun antama kirjain (`?move=g`). Havainnon mukaan `a`=1 ... `x`=24, mutta
 * arvoa ei lasketa vaan luetaan, jotta virheellinen oletus ei tuota väärää siirtoa.
 */
data class MoveLink(
    val fromPoint: Int,
    val code: String,
    val href: String,
)

data class CommandLink(
    val label: String,
    val href: String,
)

/**
 * Sivuston `Board Scheme`, eli se kolmen vaihtoehdon asetus joka vaihtaa lautakuvien
 * hakemiston.
 *
 * Mitattu 3.8.2026 oikeaa sivustoa vasten, ja hypoteesi oli väärä: skeema ei vaihda
 * tiedostonimiä vaan hakemiston, joten nimestä lukevat kohdat ovat turvassa. Vaihtoehtoja
 * on tasan kolme (`raakasivut/profile_settings.html`), ja lomakkeen arvo on yhtä pienempi
 * kuin hakemistonumero.
 *
 * [MINI] on ainoa jolla on merkitystä jäsentimelle: se pudottaa pistenumerorivin, jolloin
 * yhtäkään pistettä ei voi numeroida. Kaksi muuta ovat mukana siksi, että tuntematon
 * hakemisto pysyisi erossa tunnetuista: ilman niitä `null` tarkoittaisi sekä "jotain muuta
 * kuin Mini" että "en tunnistanut", eikä ruutu voisi erottaa niitä toisistaan.
 */
enum class BoardScheme {
    CLASSIC,
    BLUE_WHITE,

    /**
     * Sivuston oma kapean näytön lauta, ja **ainoa skeema jota tämä sovellus ei tue**.
     * Pistenumerot puuttuvat sivulta, eikä jäsennin päättele numeroa suunnasta, koska
     * `Home boards on left side` peilaa laudan ja päätelty numero osoittaisi silloin
     * peilipisteeseen. Ruutu kertoo tämän ja neuvoo vaihtamaan asetuksen sivustolla.
     */
    MINI,
}
