package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.Die
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nappirivin väli keskikaistalla (Tommin valinta 7.11.2026): muurin puolisko jolla noppia ei
 * ole, ei koskaan muurin päällä.
 *
 * Aiempi sääntö (26.8.2026) keskitti rivin laudan keskelle ja väisti vain noppalokeroa, joten
 * rivi oli aina muurin päällä, ja Pixel 8a:lla ilman paneelia `Skip Game` katkesi laitaan.
 * Noppien väistö säilyy, koska nopat ovat aina vastakkaisella puoliskolla.
 *
 * Luvut ovat pikseleitä, kuten `Layout`issa. Kaista 1300 px ilman pehmustetta: muuri on
 * 100 px leveä ja kohdissa 600–700.
 */
class MiddleActionsPlacementTest {

    @Test
    fun `ilman noppia tai vastustajan nopilla vali on oikea puolisko muurin jalkeen`() {
        assertEquals(704 to 1300, middleActionsSpan(width = 1300, pad = 0, gap = 4, leftHalf = false))
    }

    @Test
    fun `omilla nopilla oikealla vali on vasen puolisko ennen muuria`() {
        assertEquals(0 to 596, middleActionsSpan(width = 1300, pad = 0, gap = 4, leftHalf = true))
    }

    @Test
    fun `vali ei koskaan ulotu muurin kohdalle`() {
        listOf(300, 640, 1284, 2000).forEach { width ->
            val pad = 21
            val barHalf = (width + 2 * pad) / 13 / 2
            val (_, leftEnd) = middleActionsSpan(width, pad, gap = 0, leftHalf = true)
            val (rightStart, _) = middleActionsSpan(width, pad, gap = 0, leftHalf = false)
            assertTrue("leveys $width", leftEnd <= width / 2 - barHalf)
            assertTrue("leveys $width", rightStart >= width / 2 + barHalf)
        }
    }

    // --- Ahdas avausheitto (Tommin valinta 12.11.2026, pelaajan XA2) ---------------------
    //
    // Avausheiton esitys antaa kummallekin puoliskolle yhden nopan, eikä nappiriville jäänyt
    // vapaata puoliskoa: rivi piirtyi oman nopan päälle (kuorikaappaus 640x360 dp).

    private val self = CheckerColor.YELLOW
    private val opponent = CheckerColor.BLUE
    private val opening = listOf(Die(5, self), Die(1, opponent))

    @Test
    fun `ilman aloittajaa nopat jakautuvat omistajan mukaan`() {
        assertEquals(listOf(Die(1, opponent)) to listOf(Die(5, self)), stripDiceSides(opening, self, opponent, null))
    }

    @Test
    fun `oma avausheitto kokoaa nopat omalle puoliskolle varit sailyttaen`() {
        val (opp, own) = stripDiceSides(opening, self, opponent, self)
        assertTrue(opp.isEmpty())
        assertEquals(opening, own)
    }

    @Test
    fun `vastustajan avausheitto kokoaa nopat hanen puoliskolleen`() {
        val (opp, own) = stripDiceSides(opening, self, opponent, opponent)
        assertEquals(opening, opp)
        assertTrue(own.isEmpty())
    }
}
