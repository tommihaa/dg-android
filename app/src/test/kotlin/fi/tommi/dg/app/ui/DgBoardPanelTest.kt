package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pip-etumatkan etumerkki ja ottelukortin pystysijoitus, molemmat 14.9.2026 illan pyyntöjä.
 *
 * Etumatka: plus on edellä eli pienempi luku, koska lukija hahmottaa etumerkin asemana.
 * Sijoitus: kortti on lohkon keskellä eikä pinon jälkeen jäävän tilan keskellä, ja pino
 * vaikuttaa vain kun tila ei riitä. Ks. [DgBoard.pipLead] ja [DgBoard.centeredTop].
 */
class DgBoardPanelTest {

    @Test
    fun `edella oleva saa miinuksen`() {
        assertEquals("-5", DgBoard.pipLead(own = 131, other = 136))
    }

    @Test
    fun `jaljessa oleva saa plussan`() {
        assertEquals("+5", DgBoard.pipLead(own = 136, other = 131))
    }

    @Test
    fun `tasan on nolla ilman merkkia`() {
        assertEquals("0", DgBoard.pipLead(own = 131, other = 131))
    }

    @Test
    fun `kortti on lohkon keskella kun pino mahtuu alle`() {
        // 900 korkea lohko, 200 korkea kortti: keskikohta 350 riippumatta pinosta.
        assertEquals(350, DgBoard.centeredTop(height = 900, contentHeight = 200, stackHeight = 100))
        assertEquals(350, DgBoard.centeredTop(height = 900, contentHeight = 200, stackHeight = 300))
    }

    @Test
    fun `kortti vaistaa ylos vasta kun pino osuisi siihen`() {
        // Keskitettynä kortti päättyisi 550:een ja pino alkaisi 500:sta: kortti nousee 300:aan.
        assertEquals(300, DgBoard.centeredTop(height = 900, contentHeight = 200, stackHeight = 400))
    }

    @Test
    fun `ahtaassa lohkossa kortti on ylalaidassa eika negatiivinen`() {
        assertEquals(0, DgBoard.centeredTop(height = 500, contentHeight = 200, stackHeight = 400))
    }

    @Test
    fun `ylapuolinen lohko ei siirra korttia kun se mahtuu`() {
        assertEquals(350, DgBoard.centeredTop(900, contentHeight = 200, stackHeight = 100, aboveHeight = 300))
    }

    @Test
    fun `ylapuolinen lohko tyontaa korttia alas vain kun se ei muuten mahdu`() {
        assertEquals(400, DgBoard.centeredTop(900, contentHeight = 200, stackHeight = 100, aboveHeight = 400))
    }

    @Test
    fun `pinon vaisto voittaa ylapuolisen lohkon`() {
        // Pino vaatii kortin ylareunan 300:aan, ylalohko 400:aan: pino voittaa.
        assertEquals(300, DgBoard.centeredTop(900, contentHeight = 200, stackHeight = 400, aboveHeight = 400))
    }
}
