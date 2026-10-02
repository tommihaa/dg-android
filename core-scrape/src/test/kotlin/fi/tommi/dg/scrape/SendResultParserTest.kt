package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendResultParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use {
            it.readBytes().toString(Charsets.UTF_8)
        }

    @Test
    fun `kuittauksen lause luetaan kokonaan`() {
        val notice = checkNotNull(SendResultParser.notice(fixture("message_sent.html")))

        // Vastaanottajan nimi on lauseen sisällä linkkinä, joten pysähtyminen ensimmäiseen
        // elementtiin jättäisi jäljelle "Your message has been sent to". Juuri se vika
        // mitattiin ilmoitussivulla 4.8.2026, eikä sitä toisteta tässä.
        assertTrue(notice.contains("vastapelaaja"), notice)
        assertEquals("Your message has been sent to vastapelaaja", notice)
    }

    @Test
    fun `navigointipalkki ei paady lauseeseen`() {
        val notice = checkNotNull(SendResultParser.notice(fixture("message_sent.html")))

        // Palkki on taulukkona, ja se poistetaan ennen lukua. Ilman rajaa arkistoon menisi
        // otsikkorivina koko sivuston valikko.
        assertTrue(!notice.contains("Game Lounge"), notice)
        assertTrue(!notice.contains("Next"), notice)
    }

    /**
     * Sivuutuksen kuittaus, mitattu 4.9.2026. Lause on `<P>`:n sisalla, ja juuri se pudotti
     * vanhan luvun tyhjaksi: luku paattyi ensimmaiseen `<p>`:hen, joten luettavaa ei ollut.
     */
    @Test
    fun `sivuutuksen lause luetaan vaikka se on p-elementissa`() {
        val notice = checkNotNull(SendResultParser.notice(fixture("ignore_done.html")))

        assertEquals("You are now ignoring vastapelaaja.", notice)
    }

    /**
     * Sivuutuksen purkaminen, mitattu 4.9.2026 illalla. Pari edelliselle, ja se on tassa
     * siksi etta suunta luetaan **vain** lauseesta: otsikko on molemmissa pelkka
     * `DailyGammon`, eika kumpikaan sivu sisalla profiilin nappia.
     */
    @Test
    fun `purkamisen lause kertoo suunnan ja se erottaa sen asettamisesta`() {
        val asetus = checkNotNull(SendResultParser.notice(fixture("ignore_done.html")))
        val purku = checkNotNull(SendResultParser.notice(fixture("ignore_undone.html")))

        assertEquals("You are no longer ignoring vastapelaaja.", purku)
        assertTrue(purku != asetus, "Suunnan on erotuttava lauseesta")
    }

    /**
     * Luovutuksen kuittaus, mitattu 4.9.2026 illalla. Sivulla ei ole `<p>`:ta vaan `<BR>`,
     * ja lause kantaa sivuston oman lukeman luovutetuista otteluista.
     */
    @Test
    fun `luovutuksen lause luetaan BR-rivilta`() {
        val notice = checkNotNull(SendResultParser.notice(fixture("resign_done.html")))

        assertEquals("1 match resigned.", notice)
        assertTrue(!notice.contains("Game Lounge"), notice)
    }

    /**
     * Kutsun kuittaus, mitattu 4.9.2026. Sivulla ei ole yhtaan `<p>`:ta, joten vanha luku ei
     * pysahtynyt mihinkaan ja otti navigointipalkin mukaan.
     */
    @Test
    fun `kutsun lause luetaan vaikka sivulla ei ole yhtaan p-elementtia`() {
        val notice = checkNotNull(SendResultParser.notice(fixture("invite_sent.html")))

        assertEquals("You have invited vastapelaaja to a match.", notice)
        assertTrue(!notice.contains("Game Lounge"), notice)
    }

    @Test
    fun `tyhja runko ei tuota tyhjaa lausetta`() {
        // Null eika tyhja merkkijono: kutsupaikan on erotettava "sivu ei sanonut mitaan"
        // siita etta lause olisi luettu.
        assertNull(SendResultParser.notice("<html><body></body></html>"))
        assertNull(SendResultParser.notice("<html><body>   </body></html>"))
    }
}
