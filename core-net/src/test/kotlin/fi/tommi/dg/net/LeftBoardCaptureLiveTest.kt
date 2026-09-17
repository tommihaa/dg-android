package fi.tommi.dg.net

import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.TopPageParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Kaappauslistan 10: lauta kun asetus `Home boards on left side` on päällä.
 *
 * Tämä on listan ainoa kohta joka **ei ole haku vaan asetusmuutos**: se kääntää Tommin
 * oman tilin asetusta sivustolla. Tommi pyysi tämän erikseen 1.8.2026, eikä sitä saa ajaa
 * ilman uutta pyyntöä.
 *
 * Kolme varotoimea, koska kyseessä on toisen ihmisen tili:
 *
 * 1. **Nykytila luetaan lomakkeelta, ei oleteta.** DailyGammonin asetuslomake lähettää
 *    kaikki kentät kerralla, ja rastittamaton valintaruutu ei lähde mukana lainkaan.
 *    Käsin kirjoitettu kenttäjoukko siis nollaisi jokaisen asetuksen jota ei muistanut
 *    mainita.
 * 2. **Palautus tehdään `finally`ssä**, eli myös silloin kun kaappaus kaatuu kesken.
 * 3. **Palautus todennetaan** hakemalla lomake uudestaan ja vertaamalla rastit
 *    alkutilaan. Testi on punainen jos tili jäi väärään tilaan, koska hiljainen
 *    epäonnistuminen olisi tässä pahin mahdollinen lopputulos.
 */
@Tag("live")
class LeftBoardCaptureLiveTest {

    @Test
    fun `lauta kaapataan kotialue vasemmalla`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        // Lautapolku haetaan ennen asetuksen kääntämistä: jos siirrettävää ottelua ei ole,
        // asetukseen ei kosketa lainkaan.
        val playPath = TopPageParser.parse(client.okOrFail(DgPages.TOP_PATH))!!
            .matches
            .firstNotNullOfOrNull { it.playPath }
        assumeTrue(playPath != null, "Yhdessäkään ottelussa ei ollut Play-linkkiä")
        check(!playPath!!.contains("submit", ignoreCase = true)) { "Kuluttava polku: $playPath" }

        val before = client.liveSettings()
        println("Asetukset ennen: rastit=${before.checked} radiot=${before.radios}")

        // Ruutu haetaan selitteestä, koska nimet ovat paljaita numeroita joilla ei ole
        // merkitystä itsessään. Kovakoodattu numero osuisi renumeroinnin jälkeen hiljaa
        // väärään asetukseen, ja koska lähetys koskee koko lomaketta, väärä ruutu
        // tarkoittaisi kahta väärää asetusta kerralla.
        val leftBoard = before.nameFor(LEFT_BOARD_LABEL)
        assumeTrue(leftBoard != null) {
            "Selitettä \"$LEFT_BOARD_LABEL\" ei ole lomakkeella, älä oleta numerointia"
        }
        // Kumpikaan avain ei ole yksin vakaa: numero muuttuu renumeroinnissa ja teksti
        // sanamuodon muuttuessa. Molemmat yhdessä pysäyttävät kumman tahansa muutoksen
        // ennen kuin asetuksiin kosketaan.
        assertEquals(LEFT_BOARD, leftBoard, "Selite ja numero eivät täsmää, tili jää koskematta")

        var restored = false
        try {
            client.writeSettings(before, checked = before.checked + leftBoard!!)

            val board = client.okOrFail(playPath)
            File("../raakasivut/move_vasen.html").writeText(board)
            println("Tallennettu: move_vasen.html (${board.length} merkkiä)")
        } finally {
            client.writeSettings(before)
            val after = client.liveSettings()
            restored = after.checked == before.checked && after.radios == before.radios
            println("Asetukset jälkeen: rastit=${after.checked} radiot=${after.radios}")
            assertEquals(before.checked, after.checked, "Valintaruudut jäivät väärään tilaan")
            assertEquals(before.radios, after.radios, "Radiovalinnat jäivät väärään tilaan")
        }
        assertTrue(restored)
    }

    private companion object {
        /** `Home boards on left side` kaapatulla sivulla. Ristiintarkistetaan selitteeseen. */
        const val LEFT_BOARD = "7"

        const val LEFT_BOARD_LABEL = "Home boards on left side"
    }
}
