package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Viestimäärän harvinaisuusporras, rajat 1, 2–4, 5–9, 10–19, ja 20+ (24.9.2026; pinkki 50+ poistettu 29.9.2026). */
class InboxCountTierTest {

    @Test
    fun `jokainen raja osuu omaan portaaseensa`() {
        val odotettu = mapOf(
            1 to 0,
            2 to 1, 4 to 1,
            5 to 2, 9 to 2,
            10 to 3, 19 to 3,
            20 to 4, 49 to 4, 50 to 4, 999 to 4,
        )
        odotettu.forEach { (maara, porras) ->
            assertEquals("määrä $maara", porras, inboxCountTier(maara))
        }
    }

    /**
     * Mittaus 6.10.2026 ilman ackammonia (`docs/AVOIMET.md`): 21 keskustelua, ja ehdotuksen
     * jakauma oli 1 oranssi, 3 violettia, 5 sinistä, 7 vihreää ja 5 harmaata.
     */
    @Test
    fun `sijaporrastus toistaa mitatun jakauman`() {
        val maarat = listOf(12, 5, 4, 4) + List(5) { 3 } + List(7) { 2 } + List(5) { 1 }
        val porras = inboxRankTiers(maarat)
        val jakauma = maarat.groupingBy(porras).eachCount()
        assertEquals(mapOf(4 to 1, 3 to 3, 2 to 5, 1 to 7, 0 to 5), jakauma)
    }

    @Test
    fun `tasaluvut saavat saman ylemman portaan`() {
        val maarat = List(20) { 7 }
        val porras = inboxRankTiers(maarat)
        assertEquals(4, porras(7))
    }

    @Test
    fun `yksi viesti on aina harmaa`() {
        val maarat = List(25) { 1 }
        assertEquals(0, inboxRankTiers(maarat)(1))
    }

    @Test
    fun `alle kahdenkymmenen keskustelun arkisto kayttaa kiinteita rajoja`() {
        val maarat = listOf(3) + List(18) { 1 }
        val porras = inboxRankTiers(maarat)
        assertEquals(19, maarat.size)
        assertEquals(inboxCountTier(3), porras(3))
        assertEquals(1, porras(3))
    }
}
