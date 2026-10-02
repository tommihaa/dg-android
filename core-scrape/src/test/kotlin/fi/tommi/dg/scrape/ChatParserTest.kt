package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val screen = checkNotNull(ChatParser.parse(fixture("chat_thread.html")))

    @Test
    fun `vastapuolen viesti luetaan ilman lainattua osaa`() {
        // Ainoa osa joka kuuluu arkistoon. Jos sitaatti tulisi mukaan, sama viesti saisi
        // eri sisältötiivisteen joka kierroksella ja arkisto täyttyisi kopioilla.
        assertEquals("thanks for the heads up, i had no time to look at it yet", screen.incoming)
    }

    @Test
    fun `lainattu osa erottuu omaksi kentakseen ilman etuliitetta`() {
        val quoted = checkNotNull(screen.quoted)
        assertTrue(quoted.startsWith("did you see the tournament bracket?")) { quoted }
        assertFalse(quoted.contains(">")) { "Etuliite jäi mukaan: $quoted" }
    }

    @Test
    fun `hypoteettinen lauta tunnistetaan otsikosta`() {
        // Sivu näyttää tulevan aseman ja kysyy siirtoa etukäteen. Ilman tätä lippua
        // lautajäsennin tallentaisi hypoteesin pelin tilaksi, eikä mikään näyttäisi rikki.
        assertTrue(screen.speculative)
    }

    @Test
    fun `lomakkeen kohde luetaan sivulta eika koota polusta`() {
        // Haettu sivu oli tilatunnisteella 1119, mutta sen oma lomake osoittaa 1121.
        assertEquals("/bg/move/7000003/1121", screen.form?.action)
        assertEquals("7000003", screen.matchId?.value)
    }

    @Test
    fun `lahetys on POST-lomake piilokenttineen eika GET-osoite`() {
        // Kanoni tunsi aiemmin vain yhden POST-lomakkeen kirjautumisen jälkeen
        // (/bg/sendmsg). Tämä on toinen, ja se elää lautaosoitteessa.
        assertEquals(mapOf("commit" to "1"), screen.form?.hiddenFields)
        assertEquals("chat", screen.form?.field)
        assertEquals(FormMethod.POST, screen.form?.method)
    }

    @Test
    fun `lainausvalinta on oletuksena paalla`() {
        // Selittää miksi vastapuolen viesti saapuu sitaatti mukanaan: lähettäjän lomakkeessa
        // ruutu oli valmiiksi valittuna.
        assertEquals("quote", screen.form?.quoteField)
        assertTrue(screen.form?.quoteCheckedByDefault == true)
        // Ruudussa ei ole value-attribuuttia, jolloin arvo on HTML:n sääntönä `on`.
        assertEquals("on", screen.form?.quoteValue)
    }

    @Test
    fun `painikkeet luetaan sivulta`() {
        assertEquals(listOf("Next Game", "To Top"), screen.form?.submits)
    }

    @Test
    fun `alkaneessa ketjussa otsikko on Chat eika chat-kutsu`() {
        assertEquals("Chat:", screen.headerText)
        // Nimi on saatavilla vain tyhjän keskustelun otsikosta, ei tästä muodosta.
        assertNull(screen.opponentName)
    }

    @Test
    fun `tyhjan keskustelun otsikosta luetaan vastustajan nimi`() {
        val empty = ChatParser.parse(
            """
            <html><body>
            <b>Please select your action/make your move:</b>
            <b><i>You may chat with kaverina here:</i></b>
            <form method=POST action="/bg/move/7000003/1121">
            <input type=hidden name=commit value=1>
            <textarea name=chat rows=5 cols=80></textarea>
            <input type=submit name=submit value="Next Game">
            </form>
            </body></html>
            """.trimIndent(),
        )

        assertNotNull(empty)
        assertEquals("kaverina", empty!!.opponentName)
        assertNull(empty.incoming) { "Tyhjässä keskustelussa ei ole viestiä" }
        assertFalse(empty.speculative)
    }

    @Test
    fun `sivu ilman lomaketta ei ole chat-ruutu`() {
        assertNull(ChatParser.parse(fixture("top_page.html")))
    }

    @Test
    fun `paattymissivun viesti luetaan vaikka kenttaa ei ole`() {
        // Mitattu 7.9.2026 (`sessio-7-9-yo/0062`): vastustaja kirjoitti ottelun viimeisellä
        // vuorolla, ja päättymissivun lomakkeessa on vain Next Game ja To Top. Viesti on
        // `<b><i>nimi says:</i></b>` ja sulkematon `<PRE>`. Lomake on null, viesti ei.
        val screen = checkNotNull(ChatParser.parse(fixture("match_over_says.html")))
        assertNull(screen.form)
        assertEquals("Another good win! Keep it going!", screen.incoming)
        assertEquals("vastapelaaja says:", screen.headerText)
        assertEquals("vastapelaaja", screen.opponentName)
    }

    @Test
    fun `sulkematon pre ei tuota viestia kahteen kertaan`() {
        // Pikaviestisivulla <pre> jää lähteessä sulkematta, jolloin lomake on sen sisällä.
        // Onko sama totta tällä sivulla, ei ole todennettu: kaappaus tehtiin Ctrl+S:llä,
        // joka sulkee elementin itse. Testi varmistaa että kumpikin muoto kelpaa.
        val unclosed = ChatParser.parse(
            """
            <html><body>
            <b><i>Chat:</i></b><pre>&gt;vanha rivi

            uusi viesti
            <form method=POST action="/bg/move/7000003/1121">
            <input type=hidden name=commit value=1>
            <textarea name=chat></textarea>
            <input type=submit name=submit value="Next Game">
            </form>
            </body></html>
            """.trimIndent(),
        )

        assertEquals("uusi viesti", unclosed?.incoming)
        assertEquals("vanha rivi", unclosed?.quoted)
    }

    // --- Kolmas otsikkomuoto ja suljettu <pre>, kaapattu DG Mobilen liikenteestä
    // 21.8.2026 proxyn läpi. Tämä on ensimmäinen tämän sivun kaappaus raakoina tavuina:
    // aiempi oli selaimen `Ctrl+S`-tallenne, joka sulkee elementit itse eikä siksi kelpaa
    // todistamaan sulkeutumisesta kumpaankaan suuntaan.

    private val says = checkNotNull(ChatParser.parse(fixture("chat_thread_says.html")))

    @Test
    fun `suljetusta pre-lohkosta luetaan viesti`() {
        assertEquals("just too good for me for sure ...", says.incoming)
        assertNull(says.quoted)
    }

    @Test
    fun `nimi says on kolmas otsikkomuoto ja siita luetaan vastustaja`() {
        // Dokumentoituja muotoja oli kaksi (`You may chat with X here:` ja `Chat:`).
        // Tämä on kolmas, ja se on ainoa jossa nimi on otsikossa ilman `chat with`
        // -rakennetta. Ilman tätä vastustajan nimi katoaa juuri siltä sivulta jolla
        // viesti on.
        assertEquals("vastapelaaja", says.opponentName)
    }

    @Test
    fun `lomakkeen kentat luetaan says-muodosta`() {
        assertEquals("chat", says.form?.field)
        assertEquals("quote", says.form?.quoteField)
        assertTrue(says.form?.quoteCheckedByDefault == true)
    }

    @Test
    fun `saapuva aakkonen sailyy tavuna eika muutu entiteetiksi`() {
        // Mitattu 23.8.2026 kahdella omalla tunnuksella: toiselta lähetetty chat-viesti
        // saapui ensimmäiselle, ja proxyn tallentamassa sivussa tavut olivat
        // 0xE5 0xE4 0xF6 eikä muotoa `&#N;`. Saapumissuunta käyttäytyy siis kuten
        // aiemmin mitattu kirjoitussuunta, eikä siirrettävyys kentästä toiseen ole
        // enää oletus. Fixture on levyllä UTF-8:na, koska `DgClient` purkaa tavut
        // `windows-1252`:sta merkkijonoksi ennen kuin jäsennin näkee ne.
        val aakkoset = checkNotNull(ChatParser.parse(fixture("chat_thread_aakkoset.html")))
        assertEquals("abc åäö", aakkoset.incoming)
    }

    @Test
    fun `pitkassa ketjussa lainaus on yhta tasoa eika kasva`() {
        // Mitattu 27.8.2026 kahden oman tunnuksen vaihdolla. Lähetetty viesti oli
        // lainaus päällä, eli vastaanottaja näki sen sitaatin kera, ja silti tässä
        // takaisin saapuvassa lohkossa on vain edellisen viestin oma teksti. Sivusto
        // siis pudottaa lainauksen lainauksesta, ja juuri siksi `>>`-tasoa ei synny.
        // Jäsentimen yhden tason oletus on tämän jälkeen mitattu eikä onnekas.
        val pitka = checkNotNull(ChatParser.parse(fixture("chat_thread_long_quote.html")))
        assertEquals("koe 2", pitka.incoming)
        val quoted = checkNotNull(pitka.quoted)
        assertFalse(quoted.contains(">")) { "Toinen lainaustaso jäi mukaan: $quoted" }
    }

    @Test
    fun `palvelimen rivitys sailyy lainatussa osassa sellaisenaan`() {
        // Sama vaihto: viesti kirjoitettiin yhtenä pötkönä ilman omia rivinvaihtoja ja
        // saapui 29 rivinä, joista pisin on tasan 80 merkkiä. Rivittäjä on palvelin eikä
        // selain, mikä nähdään pikaviestin 24.8.2026 mittauksesta: siellä lähetys tehtiin
        // yksirivisestä kentästä joka ei voi tuottaa rivinvaihtoja, ja perillä oli 25 riviä.
        // Arkistoon menee siis rivitetty muoto, eikä alkuperäistä voi palauttaa.
        val pitka = checkNotNull(ChatParser.parse(fixture("chat_thread_long_quote.html")))
        val rivit = checkNotNull(pitka.quoted).lines()
        assertEquals(29, rivit.size)
        assertEquals(80, rivit.maxOf { it.trimEnd().length })
        assertTrue(rivit.count { it.isBlank() } == 8) { "Tyhjät rivit katosivat: $rivit" }
    }
}
