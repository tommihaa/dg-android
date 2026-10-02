package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Fraasin liittäminen luonnokseen, ks. [withPhrase]. */
class PhraseRowTest {

    @Test
    fun `tyhja luonnos korvautuu fraasilla`() {
        assertEquals("gg", withPhrase("  ", "gg"))
    }

    @Test
    fun `teksti saa valilyonnin`() {
        assertEquals("hi there gg", withPhrase("hi there  ", "gg"))
    }

    @Test
    fun `rivinvaihtoon paattyva esitaytto sailyy ja fraasi menee omalle rivilleen`() {
        // Päättymissivun Reply to -esitäyttö: lainausrivi ja kaksi rivinvaihtoa (27.9.2026).
        val esitaytto = "> Nine Lives #4293, Round 5\n\n"
        assertEquals("> Nine Lives #4293, Round 5\n\ngg", withPhrase(esitaytto, "gg"))
    }
}
