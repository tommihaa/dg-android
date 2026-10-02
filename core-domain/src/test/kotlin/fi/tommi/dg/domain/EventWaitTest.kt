package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * [EventPage.matchesBeforeOpponent]: kaavio kahdeksalle, sarakkeet kuten sivulla
 * (ilmoittautuneet, Round 2, Round 3, Winner). Oma pelaaja on `/bg/user/5` riviltä 4.
 * Tyhjät solut puuttuvat listoista kuten jäsentimen tuloksessa.
 */
class EventWaitTest {

    private fun player(n: Int) = EventEntry(
        label = "p$n",
        profilePath = "/bg/user/$n",
        matchPath = null,
        inProgress = false,
        decided = false,
        top = n - 1,
    )

    private fun won(label: String, top: Int) = EventEntry(
        label = label,
        profilePath = null,
        matchPath = "/bg/game/1/0/list",
        inProgress = false,
        decided = true,
        top = top,
    )

    private fun playing(top: Int) = EventEntry(
        label = "In Progress",
        profilePath = null,
        matchPath = "/bg/game/2/0/list#end",
        inProgress = true,
        decided = false,
        top = top,
    )

    private fun page(round2: List<EventEntry>, round3: List<EventEntry>, winner: List<EventEntry> = emptyList()) =
        EventPage(
            name = "Testi",
            conditions = emptyList(),
            rounds = listOf(
                EventRound("Round 1", (1..8).map(::player), span = 1),
                EventRound("Round 2", round2, span = 2),
                EventRound("Round 3", round3, span = 4),
                EventRound("Winner", winner, span = 8),
            ),
            rows = 8,
        )

    private val me = "/bg/user/5"

    @Test
    fun `yksi kesken oleva ottelu vastahaarassa on yhden ottelun odotus`() {
        // Voitettu kierros 1, vastahaarassa (rivit 6–7) ottelu kesken.
        val event = page(
            round2 = listOf(won("p1", 0), won("p3", 2), won("p5", 4), playing(6)),
            round3 = emptyList(),
        )
        assertEquals(1, event.matchesBeforeOpponent(me))
    }

    @Test
    fun `aloittamaton ottelu lasketaan kesken olevan lisaksi`() {
        // Kaksi voittoa. Vastahaara on rivit 0–3: toinen kierroksen 1 ottelu kesken, joten
        // kierroksen 2 ottelu ei ole alkanut. Vastustaja ratkeaa kahden ottelun jälkeen.
        val event = page(
            round2 = listOf(won("p1", 0), playing(2), won("p5", 4), won("p7", 6)),
            round3 = listOf(won("p5", 4)),
        )
        assertEquals(2, event.matchesBeforeOpponent(me))
    }

    @Test
    fun `oma ottelu kesken ei ole odotus`() {
        val event = page(
            round2 = listOf(won("p1", 0), won("p3", 2), won("p5", 4), won("p7", 6)),
            round3 = listOf(playing(0), playing(4)),
        )
        assertNull(event.matchesBeforeOpponent(me))
    }

    @Test
    fun `karsiutunut ei odota`() {
        val event = page(
            round2 = listOf(won("p1", 0), won("p3", 2), won("p5", 4), won("p7", 6)),
            round3 = listOf(playing(0), won("p7", 4)),
        )
        assertNull(event.matchesBeforeOpponent(me))
    }

    @Test
    fun `voittaja ei odota ketaan`() {
        val event = page(
            round2 = listOf(won("p1", 0), won("p3", 2), won("p5", 4), won("p7", 6)),
            round3 = listOf(won("p1", 0), won("p5", 4)),
            winner = listOf(won("p5", 0)),
        )
        assertNull(event.matchesBeforeOpponent(me))
    }

    @Test
    fun `vapaakierros etenee ilman lihavointia eika ole ottelu`() {
        // Rivit 5 ja 7 tyhjiä: p5 ja p7 etenevät ottelutta pelkkinä linkkeinä. Vastahaara
        // (rivit 0–3) pelaa yhä kierrosta 1, joten odotus on kaksi ottelua.
        val bye = { label: String, top: Int -> won(label, top).copy(decided = false) }
        val event = EventPage(
            name = "Testi",
            conditions = emptyList(),
            rounds = listOf(
                EventRound("Round 1", (1..8).filter { it != 6 && it != 8 }.map(::player), span = 1),
                EventRound("Round 2", listOf(playing(0), won("p3", 2), bye("p5", 4), bye("p7", 6)), span = 2),
                EventRound("Round 3", listOf(won("p5", 4)), span = 4),
                EventRound("Winner", emptyList(), span = 8),
            ),
            rows = 8,
        )
        assertEquals(2, event.matchesBeforeOpponent(me))
    }

    @Test
    fun `pelaaja tunnistetaan numerosta vaikka linkissa on kysely`() {
        val event = page(
            round2 = listOf(won("p1", 0), won("p3", 2), won("p5", 4), playing(6)),
            round3 = emptyList(),
        )
        assertEquals(1, event.matchesBeforeOpponent("/bg/user/5?days_to_view=30"))
        assertNull(event.matchesBeforeOpponent("/bg/user/99"))
    }
}
