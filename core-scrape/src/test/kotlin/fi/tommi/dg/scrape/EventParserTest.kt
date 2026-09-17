package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val event = EventParser.parse(fixture("event_page.html"))!!

    @Test
    fun `nimi ja saannot luetaan sivun omilla otsikoilla`() {
        assertEquals("Sample Deja Vu", event.name)
        assertEquals(
            listOf("Game", "Size", "Time control"),
            event.conditions.map { it.heading },
        )
        assertEquals("double-repeat, 11 point matches.", event.conditions.first().text)
        assertTrue(event.conditions.last().text.startsWith("Initial pool of 150:00"))
    }

    @Test
    fun `kaavion sarakkeet ovat kierroksia eivatka rivin solujarjestys`() {
        // Tämän testin koko syy: ROWSPAN venyttää solun usean rivin yli, joten
        // myöhemmällä rivillä ensimmäinen <td> ei ole ensimmäinen sarake. Ilman
        // laskentaa kierrokset menisivät sekaisin juuri siellä missä kaavio etenee.
        assertEquals(
            listOf("Round 1", "Round 2", "Round 3", "Winner"),
            event.rounds.map { it.title },
        )
        assertEquals(
            listOf("ekapelaaja", "tokapelaaja", "kolmas", "neljas", "viides"),
            event.rounds.first().entries.map { it.label },
        )
        assertEquals(
            listOf("ekapelaaja", "tokapelaaja", "kolmas", "neljas"),
            event.rounds[1].entries.map { it.label },
        )
    }

    @Test
    fun `pelaajasolu on profiililinkki ja ottelusolu on siirtolista`() {
        val player = event.rounds.first().entries.first()
        assertEquals("/bg/user/17135", player.profilePath)
        assertNull(player.matchPath)
        assertFalse(player.decided)

        val decided = event.rounds[2].entries.first()
        assertEquals("tokapelaaja", decided.label)
        assertEquals("/bg/game/5254610/0/list", decided.matchPath)
        assertNull(decided.profilePath)
        assertTrue(decided.decided)
    }

    @Test
    fun `kesken oleva ottelu tunnistetaan sivun omasta sanasta`() {
        val inProgress = event.rounds[2].entries.last()
        assertTrue(inProgress.inProgress)
        assertEquals("/bg/game/5307621/0/list#end", inProgress.matchPath)
    }

    @Test
    fun `tyhjat solut eivat ole merkintoja`() {
        // Tulevien kierrosten &nbsp;-solut ovat kaavion muotoa eivätkä tietoa.
        assertTrue(event.rounds.last().entries.isEmpty())
    }

    @Test
    fun `solulla on rivipaikka ja sarakkeella korkeus sivun omista rowspaneista`() {
        // Pariutus luetaan geometriasta: ottelusolu kattaa ne edellisen sarakkeen solut
        // joiden rivit osuvat sen väliin. Siksi rivipaikka ja korkeus ovat tietoa.
        assertEquals(8, event.rows)
        assertEquals(listOf(1, 2, 4, 8), event.rounds.map { it.span })
        assertEquals(
            listOf(0, 2, 4, 6, 7),
            event.rounds.first().entries.map { it.top },
        )
        assertEquals(listOf(0, 2, 4, 6), event.rounds[1].entries.map { it.top })
        assertEquals(listOf(0, 4), event.rounds[2].entries.map { it.top })
    }

    @Test
    fun `ottelun osapuolet ovat ne edellisen kierroksen solut jotka solu kattaa`() {
        // Round 3:n ratkennut ottelu kattaa rivit 0..3, ja Round 2:ssa niillä riveillä
        // ovat ekapelaaja (0..1) ja tokapelaaja (2..3).
        val decided = event.rounds[2].entries.first()
        val span = event.rounds[2].span
        val sides = event.rounds[1].entries.filter { it.top in decided.top until decided.top + span }
        assertEquals(listOf("ekapelaaja", "tokapelaaja"), sides.map { it.label })

        // Round 1:ssä viides (rivi 7) kohtasi neljäs (rivi 6), ja rivin 1 tyhjä solu on
        // ekapelaajan vapaakierros: siltä riviltä ei ole merkintää.
        val first = event.rounds.first().entries
        assertNull(first.firstOrNull { it.top == 1 })
    }

    @Test
    fun `vaaraa sivua ei jasenneta kaavioksi`() {
        // Ennen 1.9.2026 tämä palautti tyhjän kaavion, eli väärä sivu ja tyhjä turnaus
        // olivat paluuarvossa sama asia. Nyt jäsennin kieltäytyy itse (H3).
        assertNull(EventParser.parse(fixture("tournament_hall.html")))
    }
}
