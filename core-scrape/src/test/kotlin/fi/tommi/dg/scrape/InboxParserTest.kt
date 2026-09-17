package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.MessageSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InboxParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val quick = checkNotNull(InboxParser.parse(fixture("inbox_quick_message.html")))

    @Test
    fun `pikaviesti tunnistetaan otsikosta`() {
        assertEquals(MessageSource.QUICK_MESSAGE, quick.source)
        assertTrue(quick.rawHeader.contains("quick message from"))
    }

    @Test
    fun `lähettäjä luetaan otsikon linkistä`() {
        assertEquals("vastapelaaja", quick.sender?.name)
        assertEquals("91004", quick.sender?.userId)
    }

    @Test
    fun `viestin teksti ei ota mukaan lomaketta joka on pre-lohkon sisällä`() {
        // Vastauslomake ja erotinviivat ovat <pre>-lohkon sisällä, joten naiivi pre.text()
        // lukisi myös painikkeen tekstin.
        assertEquals("Great match!", quick.body)
        assertFalse(quick.body.contains("Send Reply"), "Painikkeen teksti vuoti viestiin")
    }

    @Test
    fun `vastauslomake luetaan sivulta`() {
        val form = checkNotNull(quick.replyForm)
        assertTrue(form.action.contains("/bg/sendmsg/91004"))
        // Kentän nimi luetaan lomakkeelta eikä oleteta, samasta syystä kuin siirtokirjain.
        assertEquals("text", form.field)
        // Metodi myös. Pikaviestilomake on POST, toisin kuin lautasivun lomake, ja väärä
        // metodi ei tuottaisi virhettä vaan viestin joka ei mene perille.
        assertEquals(FormMethod.POST, form.method)
    }

    @Test
    fun `vastauskenttä on yksirivinen input eikä textarea`() {
        // Mitattu 4.8.2026 raaoista tavuista. Aiempi fixture oli tallennettu selaimella
        // jossa on laajennus joka vaihtaa kentän moniriviseksi, ja jäsennin haki siksi
        // pelkkää textareaa: aidolla sivulla replyField olisi ollut null.
        assertEquals(80, quick.replyForm?.maxLength, "Vastauksen 80 merkin raja on sivuston oma")
    }

    @Test
    fun `laajennuksen korvaama monirivinen kenttä luetaan yhä`() {
        // Tommin omalla koneella sivu näyttää tältä, joten tämän on toimittava vaikka se
        // ei ole sivuston oma muoto. maxlength puuttuu, koska laajennus ei sitä aseta.
        val html = """
            <html><head><title>DailyGammon Quick Message</title></head><body>
            <h3>You have received the following quick message from <a href="/bg/user/12345">joku</a></h3><pre>Moi
            <hr><form action="/bg/sendmsg/12345" method="post"><textarea name="text" rows="10">&gt; Moi
            </textarea><input type="submit" value="Send Reply"></form><hr>
            </pre><p><a href="/bg/nextgame">Next &gt;&gt;</a>
            </body></html>
        """.trimIndent()

        val item = checkNotNull(InboxParser.parse(html))
        assertEquals("text", item.replyForm?.field)
        assertNull(item.replyForm?.maxLength)
        assertEquals("Moi", item.body, "Laajennuksen lainaus ei saa vuotaa viestiin")
    }

    @Test
    fun `seuraavan kohteen linkki luetaan`() {
        assertTrue(quick.nextPath!!.contains("/bg/nextgame"))
    }

    @Test
    fun `absoluuttiset osoitteet luetaan yhtä lailla`() {
        // Fixture on nyt palvelimen omaa muotoa eli suhteellinen. Absoluuttinen muoto tulee
        // silloin kun sivu on tallennettu selaimella, ja sekin on yhä luettava.
        val html = """
            <html><head><title>DailyGammon Quick Message</title></head><body>
            <h3>You have received the following quick message from <a href="http://www.dailygammon.com/bg/user/12345">joku</a></h3><pre>Moi
            <hr><form action="http://www.dailygammon.com/bg/sendmsg/12345" method="post"><input type="text" name="text" maxlength="80"><input type="submit" value="Send Reply"></form><hr>
            </pre><p><a href="http://www.dailygammon.com/bg/nextgame">Next &gt;&gt;</a>
            </body></html>
        """.trimIndent()

        val item = checkNotNull(InboxParser.parse(html))
        assertEquals(MessageSource.QUICK_MESSAGE, item.source)
        assertEquals("12345", item.sender?.userId)
        assertEquals("Moi", item.body)
        assertTrue(item.replyForm!!.action.contains("/bg/sendmsg/12345"))
    }

    @Test
    fun `turnausilmoitus tunnistetaan ilmoitukseksi eikä henkilöviestiksi`() {
        // Fixture on AITO sivu (proxyn tavut 4.8.2026), ei enää kuvakaappauksesta käsin
        // kirjoitettu. Nimet ja tunnisteet on vaihdettu, kuten kaikissa fixtureissa.
        val item = checkNotNull(InboxParser.parse(fixture("inbox_telegram.html")))

        assertEquals(MessageSource.ANNOUNCEMENT, item.source)
        assertNull(item.sender, "Ilmoituksella ei ole lähettäjää")
        assertNull(item.replyForm, "Ilmoitukseen ei voi vastata")
        // Koko lause, ei vain alkua: <pre>-lohkossa on linkkejä keskellä virkettä, ja
        // aiempi luku pysähtyi ensimmäiseen elementtiin eli jätti tähän "Congratulations to".
        assertTrue(item.body.contains("vastapelaaja")) { item.body }
        assertTrue(item.body.contains("Weekday Warriors #4444")) { item.body }
        assertTrue(item.body.trimEnd().endsWith("tournament!")) { item.body }
    }

    @Test
    fun `ilmoituksen linkki saa katketa rivinvaihtoon kesken tagin`() {
        // Aidolla sivulla <a\nhref=...> on jaettu kahdelle riville PRE-lohkon sisällä.
        // Juuri tämä on se piirre jota kuvakaappauksesta kirjoitettu sivu ei olisi tuonut,
        // ja se vaikuttaa suoraan siihen mihin asti body luetaan.
        val item = checkNotNull(InboxParser.parse(fixture("inbox_telegram.html")))
        assertFalse(item.body.contains("href"), "Tagin sisus vuoti viestiin: ${item.body}")
    }

    @Test
    fun `tuntematonta otsikkoa ei arvata mutta viesti säilyy`() {
        // Tuntematon muoto ei saa hukata viestiä: haku on tuhoava, joten toista
        // tilaisuutta ei tule.
        val html = """
            <html><body><h3>You have received something entirely new</h3>
            <pre>Sisältö tallessa</pre></body></html>
        """.trimIndent()

        val item = checkNotNull(InboxParser.parse(html))
        // UNKNOWN eikä null (9.8.2026): tuntematon on oma lajinsa, ei lajin puuttuminen.
        // Ero näkyy vasta arkistossa, jossa null olisi pakottanut valitsemaan jonkin
        // tunnetun lajin ja hautaamaan viestin sen sekaan.
        assertEquals(MessageSource.UNKNOWN, item.source)
        assertEquals("You have received something entirely new", item.rawHeader)
        assertEquals("Sisältö tallessa", item.body)
    }

    @Test
    fun `otsikoton sivu ei ole viesti`() {
        assertNull(InboxParser.parse("<html><body><p>Ei otsikkoa</p></body></html>"))
    }

    @Test
    fun `lautasivua ei luulla viestiksi`() {
        // Tämän testin kommentti sanoi 9.8.2026 asti ettei lautasivulla ole h3-otsikkoa, ja
        // se oli väärin: `move_board.html`in otsikko on `Nine Lives #2222, Round 4`. Testi
        // ajoi siis otsikottomalla sivulla eikä koskaan koskettanut väittämäänsä tapausta,
        // eli se oli sokea koetin.
        assertNull(InboxParser.parse(fixture("move_board.html")))
    }

    @Test
    fun `siirron jalkeinen chat-sivu ei ole viesti`() {
        // Vaarallisempi tapaus kuin pelkkä lauta: sivulla on sekä h3 että <pre>, joten
        // ilman lautaporttia siitä olisi jäsentynyt viesti jolla on runkoakin. Chat on oma
        // jäsentimensä, ja tämän tehtävä on vain olla lukematta sitä viestiksi.
        assertNull(InboxParser.parse(fixture("chat_thread.html")))
    }
}
