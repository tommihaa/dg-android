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
}
