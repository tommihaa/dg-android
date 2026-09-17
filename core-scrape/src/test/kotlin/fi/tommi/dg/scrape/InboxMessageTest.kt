package fi.tommi.dg.scrape

import fi.tommi.dg.domain.MessageSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Jonon kohteen muunnos arkistoitavaksi viestiksi.
 *
 * Lähteenä aidot fixturet eikä käsin kirjoitettu HTML, koska väitteet koskevat sitä mitä
 * sivuilla oikeasti on: pikaviestillä on lähettäjä ja ilmoituksella ei, kummallakaan ei ole
 * aikaleimaa.
 */
class InboxMessageTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private fun item(name: String) = checkNotNull(InboxParser.parse(fixture(name)))

    private val quick = item("inbox_quick_message.html").toMessage(HETKI)
    private val telegram = item("inbox_telegram.html").toMessage(HETKI)

    @Test
    fun `pikaviestista tulee viesti jonka laji ja lahettaja sailyvat`() {
        assertEquals(MessageSource.QUICK_MESSAGE, quick.source)
        assertEquals("vastapelaaja", quick.sender)
        assertEquals("Great match!", quick.body)
    }

    @Test
    fun `sivun otsikko sailyy raakana`() {
        // Otsikko talletetaan aina eikä vain tuntemattomasta (päätös 9.8.2026): jos
        // lajittelusääntö osoittautuu vääräksi, laji on johdettavissa uudelleen vain siitä
        // tekstistä josta se alun perin johdettiin.
        assertTrue(quick.rawHeader.contains("quick message from")) { quick.rawHeader }
        assertTrue(telegram.rawHeader.contains("telegram message")) { telegram.rawHeader }
    }

    @Test
    fun `ilmoituksella ei ole lahettajaa eika sijaisnimea keksita`() {
        assertEquals(MessageSource.ANNOUNCEMENT, telegram.source)
        assertEquals("", telegram.sender, "Sijaisnimi olisi sovelluksen väite eikä sivun")
    }

    @Test
    fun `aikaleima jaa tyhjaksi koska sivu ei kerro sita`() {
        // Kummallakaan jonon sivulla ei ole lähetysaikaa. Sovellus ei saa väittää siitä
        // enempää kuin sivu sanoo, joten kenttä on tyhjä eikä saapumishetkellä täytetty.
        assertEquals("", quick.timestampText)
        assertEquals("", telegram.timestampText)
    }

    @Test
    fun `ottelua ei paatella jonon sivulta`() {
        assertNull(quick.matchId)
        assertNull(telegram.matchId)
    }

    @Test
    fun `keskustelukumppani on lahettaja, ja ilmoituksella ei ole kumpaakaan`() {
        // Jonon sivulla nämä ovat sama nimi, koska saapunut viesti on aina toiselta.
        // Kentät ovat silti kaksi, koska chat-ketjussa oma viesti eroaa lähettäjältään.
        assertEquals(quick.sender, quick.opponent)
        // Null eikä tyhjä: sivuston ilmoituksella ei ole keskustelukumppania lainkaan, ja
        // se on eri asia kuin nimetön kumppani. Lähettäjä on tyhjä samasta syystä kuin
        // ennenkin, eli sijaisnimeä ei keksitä.
        assertNull(telegram.opponent)
    }

    @Test
    fun `keskustelukumppani ei ole osa tunnistetta`() {
        // Tiiviste tunnistaa viestitapahtuman, ja tapahtuma on sama riippumatta siitä
        // tiesikö sovellus kumppanin nimen. Ilman tätä ehtoa sarakkeen lisääminen olisi
        // muuttanut jo tallennettujen viestien pääavaimet.
        assertEquals(quick.id, quick.copy(opponent = "joku aivan muu").id)
    }

    @Test
    fun `sama kohde samalla leimalla tuottaa saman tunnisteen`() {
        // Idempotenssi: epäonnistuneen kirjoituksen uusiminen on turvallista vain jos leima
        // annetaan kerran viestiä muodostettaessa. Tämä testi on se väite.
        val uudestaan = item("inbox_quick_message.html").toMessage(HETKI)
        assertEquals(quick.id, uudestaan.id)
    }

    @Test
    fun `sama kohde eri leimalla on eri viesti`() {
        // Toinen puoli samasta päätöksestä: pikaviestisivulla ei ole aikaleimaa, joten kaksi
        // erikseen lähetettyä `Great match!` erottuvat vain saapumishetkestä. Ilman tätä
        // jälkimmäinen katoaisi hiljaa kannan IGNORE-sääntöön.
        val myohemmin = item("inbox_quick_message.html").toMessage(HETKI + 1)
        assertNotEquals(quick.id, myohemmin.id)
    }

    private companion object {
        /** Mikä tahansa kiinteä hetki. Testi väittää leiman käsittelystä, ei arvosta. */
        const val HETKI = 1_754_700_000_000L
    }
}
