package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Viennin ainoa tarkistus on ettei se ole sivu. Sisältöä ei tulkita, ks. [MatchExport].
 */
class MatchExportTest {

    private val id = MatchId("7000034")

    @Test
    fun `mat-teksti kelpaa ja nimi tulee tunnuksesta`() {
        val export = MatchExport.read(id, " 21 point match\n\n Game 1\n a : 0  b : 0\n")
        assertEquals("dg-7000034.mat", export?.fileName)
        assertEquals(" 21 point match\n\n Game 1\n a : 0  b : 0\n", export?.text)
    }

    @Test
    fun `html-sivu ei ole vienti`() {
        assertNull(MatchExport.read(id, "<html><body>21 point match</body></html>"))
        assertNull(MatchExport.read(id, "<HTML><head><title>DailyGammon Login</title></head></HTML>"))
    }

    @Test
    fun `tyhja tai otsikoton runko ei ole vienti`() {
        assertNull(MatchExport.read(id, ""))
        assertNull(MatchExport.read(id, "   \n"))
        assertNull(MatchExport.read(id, "Not Found"))
    }
}
