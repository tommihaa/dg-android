package fi.tommi.dg.net

import fi.tommi.dg.domain.SettingsPage
import fi.tommi.dg.scrape.BoardParser
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.TopPageParser
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Mittaa ne asetukset jotka voivat kaataa lautajäsentimen.
 *
 * `docs/KOHDE.md` nimesi 3.8.2026 kolme riskiä jotka putosivat asetussivun jäsennyksestä, ja
 * kirjasi ne **todentamattomina**. Tämä testi todentaa niistä kaksi:
 *
 * 1. `Hide pip counts` veisi `BoardParserTest`in tarkistuskeinon. Pip-luku on ainoa kohta
 *    jossa sivun kaksi riippumatonta osaa todistavat toisensa, joten sen katoaminen ei
 *    riko jäsennintä vaan **vahdin**, mikä on hiljaisempi vika.
 * 2. `Board Scheme` voi vaihtaa kuvatiedostojen nimet. Jäsennin valitsee pisteet nimestä
 *    (`img[src*=pt_]`) ja lukee noppien värin nimestä (`die_b5.gif`), eli juuri niistä
 *    kahdesta kohdasta jotka eivät nojaa ALTiin.
 *
 * **Kolmas riski ei ole tässä, eikä se ole unohdus.** `Skip all automatic pages` vaikuttaa
 * siihen mitä `/bg/nextgame` tarjoilee, ja se osoite kuluttaa jonoa. Riskiä ei siis voi
 * mitata rikkomatta sääntöä joka on koko sovelluksen olemassaolon syy, joten se jää auki.
 *
 * Sama kolme varotoimea kuin `LeftBoardCaptureLiveTest`illä, koska kyseessä on toisen
 * ihmisen tili: nykytila luetaan lomakkeelta, palautus on `finally`ssä ja palautus
 * todennetaan. Lisäksi **jokainen muutos varmennetaan lukemalla asetussivu uudestaan**
 * ennen laudan hakua: ilman sitä epäonnistunut lähetys näyttäisi tulokselta
 * "asetuksella ei ole vaikutusta", mikä on väärä johtopäätös eikä virhe.
 */
@Tag("live")
class SettingsRiskCaptureLiveTest {

    @Test
    fun `pip-luvun piilotus ja lautaskeema mitataan laudalta`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        // Lautapolku haetaan ennen asetusten kääntämistä: jos siirrettävää ottelua ei ole,
        // asetuksiin ei kosketa lainkaan.
        val playPath = TopPageParser.parse(client.okOrFail(DgPages.TOP_PATH))!!
            .matches
            .firstNotNullOfOrNull { it.playPath }
        assumeTrue(playPath != null, "Yhdessäkään ottelussa ei ollut Play-linkkiä")
        // Kaksi kuluttavaa muotoa, ei yksi: `submit` lähettää toiminnon ja `move` kokoaa
        // siirtoa. Kumpikin oikeassa ottelussa.
        check(!playPath!!.contains("submit", ignoreCase = true)) { "Kuluttava polku: $playPath" }
        check(!playPath.contains("move=", ignoreCase = true)) { "Siirtoa kokoava polku: $playPath" }

        val before = client.liveSettings()
        println("Asetukset ennen: rastit=${before.checked} radiot=${before.radios}")

        val hidePips = before.nameFor(HIDE_PIPS_LABEL)
        assumeTrue(hidePips != null) { "Selitettä \"$HIDE_PIPS_LABEL\" ei ole lomakkeella" }
        // Selite ja numero ristiin, jotta kumpi tahansa muutos pysäyttää ennen kuin
        // asetuksiin kosketaan. Ks. SettingsPage.nameFor.
        assertEquals(HIDE_PIPS, hidePips, "Selite ja numero eivät täsmää, tili jää koskematta")
        assumeTrue(BOARD_SCHEME in before.radios) { "Radioryhmää $BOARD_SCHEME ei ole lomakkeella" }

        val findings = mutableListOf<String>()
        try {
            // Lähtötaso samasta ottelusta ja samasta osoitteesta, jotta erot johtuvat
            // asetuksesta eivätkä eri laudasta.
            findings += client.capture("lahtotaso", playPath, "risk_baseline.html")

            client.applyAndVerify(before, checked = before.checked + hidePips!!)
            findings += client.capture("pip-luvut piilotettu", playPath, "risk_hide_pips.html")

            client.applyAndVerify(before, radios = before.radios + (BOARD_SCHEME to CLASSIC))
            findings += client.capture("skeema Classic", playPath, "risk_board_classic.html")

            client.applyAndVerify(before, radios = before.radios + (BOARD_SCHEME to MINI))
            findings += client.capture("skeema Mini", playPath, "risk_board_mini.html")
        } finally {
            client.writeSettings(before)
            val after = client.liveSettings()
            println("Asetukset jälkeen: rastit=${after.checked} radiot=${after.radios}")
            println(findings.joinToString("\n"))
            assertEquals(before.checked, after.checked, "Valintaruudut jäivät väärään tilaan")
            assertEquals(before.radios, after.radios, "Radiovalinnat jäivät väärään tilaan")
        }

        assertTrue(findings.size == 4, "Kaikkia neljää otosta ei saatu")
    }

    /** Lähettää muutoksen ja lukee sivun uudestaan varmistaakseen että se meni perille. */
    private fun DgClient.applyAndVerify(
        original: SettingsPage,
        checked: Set<String> = original.checked,
        radios: Map<String, String> = original.radios,
    ) {
        writeSettings(original, checked = checked, radios = radios)
        val now = liveSettings()
        check(now.checked == checked) { "Rastit eivät menneet perille: ${now.checked} != $checked" }
        check(now.radios == radios) { "Radiot eivät menneet perille: ${now.radios} != $radios" }
    }

    /**
     * Hakee laudan, tallentaa raakasivun ja kertoo mitä jäsennin siitä sai.
     *
     * Raportoidut luvut ovat juuri ne joista riskit ratkeavat: paneelin pip-luku on vahdin
     * lähde, ja kuvapolkujen alkuosat kertovat pysyvätkö `pt_` ja `die_` paikoillaan.
     */
    private fun DgClient.capture(label: String, playPath: String, fileName: String): String {
        val html = okOrFail(playPath)
        File("../raakasivut/$fileName").writeText(html)

        val document = Jsoup.parse(html)
        val prefixes = document.select("img[src]")
            .map { it.attr("src").substringAfterLast('/').substringBefore('_') }
            .filter { it.isNotBlank() }
            .toSortedSet()

        val board = BoardParser.parse(html)
        val pips = board?.players?.map { it.pips }
        val occupied = board?.points?.count { it.count > 0 }

        return buildString {
            append("$label ($fileName, ${html.length} merkkiä): ")
            append("jäsentyi=${board != null} ")
            append("paneelien pipit=$pips ")
            append("miehitettyjä pisteitä=$occupied ")
            append("noppia=${board?.dice?.size} ")
            append("kuvien etuliitteet=$prefixes")
        }
    }

    private companion object {
        /** `Hide pip counts` kaapatulla sivulla. Ristiintarkistetaan selitteeseen. */
        const val HIDE_PIPS = "6"

        const val HIDE_PIPS_LABEL = "Hide pip counts"

        const val BOARD_SCHEME = "board"
        const val CLASSIC = "0"
        const val MINI = "2"
    }
}
