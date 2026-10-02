package fi.tommi.dg.app.ui

import fi.tommi.dg.app.session.PlayerRatingStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Ratingin harvinaisuusporras: 1600, 1800, 2000 ja 2200 kiinteinä 29.9.2026 illasta. */
class RatingTierTest {

    @Test
    fun `rajat osuvat portaisiinsa`() {
        val odotettu = mapOf(
            1403.49 to 0, 1500.0 to 0, 1599.99 to 0,
            1600.0 to 1, 1799.99 to 1,
            1800.0 to 2, 1999.99 to 2,
            2000.0 to 3, 2199.99 to 3,
            2200.0 to 4, 2207.68 to 4, 2328.65 to 4,
        )
        odotettu.forEach { (rating, porras) ->
            assertEquals("rating $rating", porras, ratingTier(rating))
        }
    }

    @Test
    fun `sivun teksti luvuksi ja lukukelvoton nulliksi`() {
        assertEquals(2219.9, PlayerRatingStore.parse(" 2219.90 ")!!, 0.0001)
        assertNull(PlayerRatingStore.parse(null))
        assertNull(PlayerRatingStore.parse(""))
        assertNull(PlayerRatingStore.parse("n/a"))
    }
}
