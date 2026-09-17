package fi.tommi.dg.scrape

import fi.tommi.dg.domain.MatchId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlayerTournamentsParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val events = PlayerTournamentsParser.parse(fixture("player_events.html"))!!
    private val wins = PlayerTournamentsParser.parse(fixture("player_wins.html"))!!

    @Test
    fun `pelaaja luetaan otsikon linkista`() {
        assertEquals("pelaaja", events.player.name)
        assertEquals("/bg/user/90001", events.player.profilePath)
        assertEquals("pelaaja", wins.player.name)
    }

    @Test
    fun `kaynnissa oleva turnaus kantaa voitot ja ottelulinkin`() {
        assertEquals(3, events.rows.size)
        val first = events.rows.first()
        assertEquals("Sample Stratifieds #1920 Group 1", first.name)
        assertEquals("111128", first.eventId)
        assertEquals("/bg/event/111128", first.eventPath)
        assertEquals("2", first.winsText)
        assertEquals("/bg/game/5305249/", first.activeMatchPath)
        assertEquals(MatchId("5305249"), first.activeMatchId)
        assertNull(first.whenText)
    }

    @Test
    fun `active game voi puuttua rivilta`() {
        // Mitattu eikä oletettu: linkki on rivillä vain kun ottelu on käynnissä, joten
        // sarakkeita ei voi laskea rivin lopusta.
        val third = events.rows[2]
        assertEquals("Sample Marathon #4285", third.name)
        assertEquals("2", third.winsText)
        assertNull(third.activeMatchPath)
        assertNull(third.activeMatchId)
    }

    @Test
    fun `voitettu turnaus kantaa ajankohdan sivun sanoin`() {
        assertEquals(2, wins.rows.size)
        val first = wins.rows.first()
        assertEquals("Sample Sevens #3673 Group 1", first.name)
        assertEquals("112140", first.eventId)
        assertEquals("Wed Aug 12 16:08:57 2026", first.whenText)
        assertNull(first.winsText)
        assertNull(first.activeMatchPath)
    }
}
