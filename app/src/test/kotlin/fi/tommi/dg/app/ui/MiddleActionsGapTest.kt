package fi.tommi.dg.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Peruuttamattoman napin erottava lisävali nappirivillä (dg-mobile-auditointi C3).
 *
 * Foorumivalitus jota tämä vahtii: "the 'accept doubles' button is just where the 'roll'
 * button is, and I keep pressing it accidently" (3/2022). Sääntö on rajassa eikä napissa:
 * vali tulee jokaiseen kohtaan jossa peruttavuus vaihtuu, ja rivin ensimmäinen
 * peruuttamaton saa valin aina, jotta se ei piirry `Roll Dice`n totuttuun osumakohtaan.
 *
 * Nappien nimet ovat sivun omia sanoja, samat joilla `MiddleActionsRow` ne piirtää.
 */
class MiddleActionsGapTest {

    @Test
    fun `rivin alussa oleva peruuttamaton saa valin`() {
        // Kuutiotarjous: Accept ja Decline. Ilman valia Accept osuisi samaan kohtaan
        // johon Roll Dice piirtyy muissa tiloissa.
        val submits = listOf("Accept", "Decline")
        assertTrue(irreversibleGapBefore(submits, 0))
    }

    @Test
    fun `kahden peruuttamattoman valiin ei tule valia`() {
        // Accept ja Decline ovat samaa lajia, eikä valitus koske niiden sekoittumista.
        val submits = listOf("Accept", "Decline")
        assertFalse(irreversibleGapBefore(submits, 1))
    }

    @Test
    fun `peruttavan ja peruuttamattoman rajaan tulee vali molempiin suuntiin`() {
        assertTrue(irreversibleGapBefore(listOf("Roll Dice", "Double"), 1))
        assertTrue(irreversibleGapBefore(listOf("Double", "Roll Dice"), 1))
    }

    @Test
    fun `pelkat peruttavat eivat saa valia`() {
        val submits = listOf("Submit Move", "Submit Greedy Bearoff")
        assertFalse(irreversibleGapBefore(submits, 0))
        assertFalse(irreversibleGapBefore(submits, 1))
    }

    @Test
    fun `rivin alussa oleva peruttava ei saa valia`() {
        assertFalse(irreversibleGapBefore(listOf("Roll Dice", "Double"), 0))
    }
}
