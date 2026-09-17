package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MatchClockTest {

    private fun match(grace: String?, pool: String?) = Match(
        id = MatchId("1"),
        eventName = "Event",
        eventId = null,
        opponent = PlayerRef(name = "opponent"),
        myTurn = true,
        round = null,
        matchLength = null,
        graceText = grace,
        timePoolText = pool,
        playPath = null,
        reviewPath = null,
    )

    @Test
    fun `lukee tunnit ja minuutit`() {
        assertEquals(0, MatchClock.minutes("0:00"))
        assertEquals(36, MatchClock.minutes("0:36"))
        // Mitattuja lukemia sivustolta (docs/KOHDE.md).
        assertEquals(278 * 60 + 29, MatchClock.minutes("278:29"))
        assertEquals(13 * 60 + 26, MatchClock.minutes(" 13:26 "))
    }

    @Test
    fun `ei-aika on null eikä nolla`() {
        // Jokainen näistä on nähty sivulla, ja nollaksi tulkittuna jokainen antaisi
        // väärän varoituksen juuri niille otteluille joilla ei ole kelloa lainkaan.
        assertNull(MatchClock.minutes(null))
        assertNull(MatchClock.minutes(""))
        assertNull(MatchClock.minutes("-"))
        assertNull(MatchClock.minutes("n/a"))
        assertNull(MatchClock.minutes("12"))
        assertNull(MatchClock.minutes("12:3"))
        assertNull(MatchClock.minutes("12:345"))
        assertNull(MatchClock.minutes("1:2:3"))
        assertNull(MatchClock.minutes("-1:00"))
        assertNull(MatchClock.minutes("12:60"))
    }

    @Test
    fun `grace on kulunut vasta nollassa`() {
        assertTrue(MatchClock.graceSpent(match("0:00", "136:42")))
        assertFalse(MatchClock.graceSpent(match("0:01", "136:42")))
        assertFalse(MatchClock.graceSpent(match(null, null)))
        assertFalse(MatchClock.graceSpent(match("n/a", "275:21")))
    }

    @Test
    fun `pooli on vähissä alle kahden vuorokauden`() {
        assertTrue(MatchClock.poolLow(match("0:00", "47:59")))
        assertFalse(MatchClock.poolLow(match("0:00", "48:00")))
        assertFalse(MatchClock.poolLow(match("13:26", "234:39")))
        // Päättyneellä ottelulla ei ole kelloa, eikä puuttuva kenttä ole vähissä oleva.
        assertFalse(MatchClock.poolLow(match(null, null)))
    }
}
