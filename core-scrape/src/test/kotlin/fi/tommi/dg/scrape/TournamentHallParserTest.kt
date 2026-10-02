package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TournamentHallParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val hall = TournamentHallParser.parse(fixture("tournament_hall.html"))!!

    @Test
    fun `taulukot erotetaan captionista eika jarjestyksesta`() {
        // Sivulla päättyneet ovat ensin ja aktiiviset toisena, mikä on päinvastoin kuin
        // mitä nimestä odottaisi. Caption ratkaisee, joten järjestys saa vaihtua.
        assertEquals(2, hall.finished.size)
        assertEquals(3, hall.active.size)
    }

    @Test
    fun `paattynyt rivi kantaa voittajan ja paattymisajan`() {
        val first = hall.finished.first()
        assertEquals("Sample Sevens #3587 Group 1", first.name)
        assertEquals("109825", first.eventId)
        assertEquals("/bg/event/109825", first.eventPath)
        assertEquals("ekapelaaja", first.winner?.name)
        assertEquals("14866", first.winner?.userId)
        assertEquals("2026-08-26 14:59:38", first.dateText)
    }

    @Test
    fun `aktiivisella rivilla ei ole voittajaa ja aika on alkamisaika`() {
        val first = hall.active.first()
        assertEquals("Sample Sevens #3758 Group 4", first.name)
        assertNull(first.winner?.name)
        assertNull(first.winner?.userId)
        assertEquals("2026-08-26 14:12:04", first.dateText)
    }

    @Test
    fun `otsikkorivit eivat paady turnauksiksi`() {
        assertTrue(hall.active.all { it.eventPath != null })
        assertTrue(hall.finished.all { it.eventPath != null })
    }

    @Test
    fun `loungen turnaustaulukkoa ei luulla halliksi`() {
        // Loungen caption on "Tournament Sign-Up", eli eri sana eikä osajono. Ennen
        // 1.9.2026 tulos oli tyhjä halli, nyt jäsennin kieltäytyy sivusta (H3).
        assertNull(TournamentHallParser.parse(fixture("lounge_page.html")))
    }
}
