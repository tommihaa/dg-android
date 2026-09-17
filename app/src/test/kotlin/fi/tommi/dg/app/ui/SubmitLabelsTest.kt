package fi.tommi.dg.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kohteen tunnistus napin tekstistä.
 *
 * Testin painavin väite on viimeinen: vertailu on tarkka, joten mikään muu sivun nappi ei
 * saa sovelluksen omaa sanaa vahingossa.
 */
class SubmitLabelsTest {

    @Test
    fun `sivuston To Top on Top Pagen nappi`() {
        assertTrue(SubmitLabels.isToTopPage(SubmitLabels.TO_TOP))
    }

    @Test
    fun `jonon seuraava ottelu ei ole paikka jolla on oma nimi`() {
        assertFalse(SubmitLabels.isToTopPage("Next Game"))
    }

    @Test
    fun `kuutiotekoja ei kosketa`() {
        assertFalse(SubmitLabels.isToTopPage("Accept"))
        assertFalse(SubmitLabels.isToTopPage("Decline"))
        assertFalse(SubmitLabels.isToTopPage("Double"))
    }

    @Test
    fun `osittainen osuma ei riita`() {
        assertFalse(SubmitLabels.isToTopPage("To Top Page"))
        assertFalse(SubmitLabels.isToTopPage("Top"))
        assertFalse(SubmitLabels.isToTopPage("to top"))
    }
}
