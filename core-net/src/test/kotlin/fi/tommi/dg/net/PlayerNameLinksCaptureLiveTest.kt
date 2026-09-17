package fi.tommi.dg.net

import fi.tommi.dg.scrape.BoardParser
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.TopPageParser
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Mittaa asetuksen `Player Name links on game page` vaikutuksen lautasivuun.
 *
 * **Neljäs riski, jota ei nimetty kolmen joukossa.** `SettingsRiskCaptureLiveTest` mittasi
 * `Hide pip countsin` ja `Board Scheman`, ja `docs/KOHDE.md` nimesi kolmantena `Skip all
 * automatic pagesin` jota ei voi mitata kuluttamatta jonoa. Tämä kenttä jäi listan
 * ulkopuolelle, vaikka se muuttaa juuri sitä sivun rakennetta jota `BoardParser` lukee:
 * pelaajanimen ympärillä oleva `<a href=/bg/user/...>` on `parsePlayers`in **ainoa**
 * valitsin, eikä yksikään fixture ole sivulta jolla asetus olisi ollut pois.
 *
 * Ennuste jonka tämä testi joko vahvistaa tai kumoaa: ilman linkkiä `parsePlayers` palauttaa
 * tyhjän listan, jolloin sivu jäsentyy yhä laudaksi mutta ilman nimiä, pisteitä ja
 * pip-lukuja. Pip-vahti on silloin hiljaa, ja se on koko mittauksen syy: vahti on
 * sovelluksen ainoa automaattinen tunnistin hiljaa väärin menneelle jäsennykselle.
 *
 * **Kolmas otos lisättiin 24.8.2026 korjauksen jälkeen** (`docs/ASETUKSET.md` kohta E).
 * Korjattu `parsePlayers` tunnistaa paneelin myös pistekentästä, ja kun myös `Hide pip
 * counts` on päällä, solusta katoaa pip-rivi eli tunnistus nojaa yksin `score:`-tekstiin.
 * Sitä ei ollut mitattu, ja korjaus oli siihen asti sen oletuksen varassa että rivi on
 * paikalla. Otos on samassa ajossa eikä omassa testissään tarkoituksella: kolme kaappausta
 * yhdellä palautuksella koskee tiliä vähemmän kuin kaksi ajoa omine palautuksineen.
 *
 * **Tommin pyyntö 24.8.2026** (`docs/ASETUKSET.md` kohdat D ja E), eikä tätä saa ajaa ilman uutta
 * pyyntöä. Samat kolme varotoimea kuin sisartesteillä, koska kyseessä on toisen ihmisen
 * tili: nykytila luetaan lomakkeelta, palautus on `finally`ssä ja palautus todennetaan.
 * Lisäksi muutos varmennetaan lukemalla asetussivu uudestaan ennen laudan hakua, koska
 * ilman sitä epäonnistunut lähetys näyttäisi tulokselta "asetuksella ei ole vaikutusta".
 */
@Tag("live")
class PlayerNameLinksCaptureLiveTest {

    @Test
    fun `pelaajanimen linkin poisto mitataan laudalta`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        // Lautapolku haetaan ennen asetuksen kääntämistä: jos siirrettävää ottelua ei ole,
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

        val nameLinks = before.nameFor(NAME_LINKS_LABEL)
        assumeTrue(nameLinks != null) { "Selitettä \"$NAME_LINKS_LABEL\" ei ole lomakkeella" }
        // Selite ja numero ristiin, jotta kumpi tahansa muutos pysäyttää ennen kuin
        // asetuksiin kosketaan. Ks. SettingsPage.nameFor.
        assertEquals(NAME_LINKS, nameLinks, "Selite ja numero eivät täsmää, tili jää koskematta")
        assumeTrue(nameLinks!! in before.checked) {
            "Asetus on jo pois päältä, jolloin lähtötasoa ei saa samasta ottelusta"
        }

        val findings = mutableListOf<String>()
        try {
            // Lähtötaso samasta ottelusta ja samasta osoitteesta, jotta ero johtuu
            // asetuksesta eikä eri laudasta.
            findings += client.capture("linkit paalla", playPath, "risk_name_links_on.html")

            client.writeSettings(before, checked = before.checked - nameLinks)
            val now = client.liveSettings()
            check(nameLinks !in now.checked) { "Asetus ei mennyt perille: rastit=${now.checked}" }
            check(now.radios == before.radios) { "Radiot muuttuivat: ${now.radios}" }

            findings += client.capture("linkit pois", playPath, "risk_name_links_off.html")

            // Kolmas otos: molemmat pois, jolloin solusta katoaa myös pip-rivi ja
            // tunnistuksella on jäljellä vain pistekenttä.
            val hidePips = before.nameFor(HIDE_PIPS_LABEL)
            check(hidePips == HIDE_PIPS) { "Selite ja numero eivät täsmää: $hidePips" }
            client.writeSettings(before, checked = before.checked - nameLinks + hidePips!!)
            val both = client.liveSettings()
            check(nameLinks !in both.checked && hidePips in both.checked) {
                "Yhdistelmä ei mennyt perille: rastit=${both.checked}"
            }

            findings += client.capture(
                "linkit pois ja pipit piilossa",
                playPath,
                "risk_name_links_off_no_pips.html",
            )
        } finally {
            client.writeSettings(before)
            val after = client.liveSettings()
            println("Asetukset jälkeen: rastit=${after.checked} radiot=${after.radios}")
            println(findings.joinToString("\n"))
            assertEquals(before.checked, after.checked, "Valintaruudut jäivät väärään tilaan")
            assertEquals(before.radios, after.radios, "Radiovalinnat jäivät väärään tilaan")
        }

        assertEquals(3, findings.size, "Kaikkia kolmea otosta ei saatu")
    }

    /**
     * Hakee laudan, tallentaa raakasivun ja kertoo mitä jäsennin siitä sai.
     *
     * Raportoidut luvut ovat juuri ne joista tämä riski ratkeaa: paneelien lukumäärä kertoo
     * löysikö `parsePlayers` mitään, ja nimet, pipit ja pisteet kertovat mitä paneeleista
     * jäi jäljelle. Pisteiden ja noppien luvut ovat mukana verrokkina: jos ne muuttuvat,
     * lauta ei ole sama eikä vertailu kelpaa.
     */
    private fun DgClient.capture(label: String, playPath: String, fileName: String): String {
        val html = okOrFail(playPath)
        File("../raakasivut/$fileName").writeText(html)

        val document = Jsoup.parse(html)
        val userLinks = document.select("a[href*=/bg/user/]").size
        // Molemmat tuntomerkit erikseen: korjatun jäsentimen kaksi ehtoa ovat juuri nämä,
        // ja niiden osumat kertovat kumpi kantoi missäkin otoksessa.
        val scoreCells = document.select("td:matchesOwn(score:)").size

        val board = BoardParser.parse(html)
        val panels = board?.players

        return buildString {
            append("$label ($fileName, ${html.length} merkkiä): ")
            append("jäsentyi=${board != null} ")
            append("kayttajalinkkeja=$userLinks ")
            append("pistekenttasoluja=$scoreCells ")
            append("paneeleja=${panels?.size} ")
            append("nimet=${panels?.map { it.player.name }} ")
            append("pipit=${panels?.map { it.pips }} ")
            append("pisteet=${panels?.map { it.score }} ")
            append("miehitettyja pisteita=${board?.points?.count { it.count > 0 }} ")
            append("noppia=${board?.dice?.size}")
        }
    }

    private companion object {
        /** `Player Name links on game page` kaapatulla sivulla. Ristiintarkistetaan selitteeseen. */
        const val NAME_LINKS = "3"

        const val NAME_LINKS_LABEL = "Player Name links on game page"

        /** `Hide pip counts` kaapatulla sivulla. Ristiintarkistetaan selitteeseen. */
        const val HIDE_PIPS = "6"

        const val HIDE_PIPS_LABEL = "Hide pip counts"
    }
}
