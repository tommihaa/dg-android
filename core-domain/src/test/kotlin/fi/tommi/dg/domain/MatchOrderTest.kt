package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Otteluluettelon oma lajittelu (dg-mobile-auditointi G1).
 *
 * Kaksi asiaa ovat tässä sisältöä eivätkä toteutusta, ja molemmat ovat `SUBSTANSSI.md`
 * kohdan 21 seurauksia: sivuston järjestys on kiireellisyysjärjestys, joten siihen on
 * päästävä takaisin, eikä sen tietoa saa hävittää enempää kuin pyydettiin.
 */
class MatchOrderTest {

    private fun match(
        id: String,
        opponent: String = "opponent",
        grace: String? = null,
        pool: String? = null,
        round: String? = null,
        length: Int? = null,
    ) = Match(
        id = MatchId(id),
        eventName = "Event $id",
        eventId = null,
        opponent = PlayerRef(name = opponent),
        myTurn = true,
        round = round,
        matchLength = length,
        graceText = grace,
        timePoolText = pool,
        playPath = "/bg/move/$id/1",
        reviewPath = null,
    )

    private fun List<Match>.ids() = map { it.id.value }

    @Test
    fun `sivuston jarjestys jattaa listan koskematta`() {
        val list = listOf(match("3", grace = "9:00"), match("1", grace = "1:00"))
        assertEquals(listOf("3", "1"), list.ordered(MatchOrder.SITE).ids())
    }

    @Test
    fun `grace nousevasti ja laskevasti`() {
        val list = listOf(
            match("a", grace = "13:26"),
            match("b", grace = "0:00"),
            match("c", grace = "2:31"),
        )
        assertEquals(listOf("b", "c", "a"), list.ordered(MatchOrder(MatchSortKey.GRACE)).ids())
        assertEquals(
            listOf("a", "c", "b"),
            list.ordered(MatchOrder(MatchSortKey.GRACE, descending = true)).ids(),
        )
    }

    @Test
    fun `aika luetaan lukuna eika tekstina`() {
        // Tekstina `9:00` olisi suurempi kuin `13:26`, ja juuri se on se hiljainen vika
        // jota vastaan tama testi on kirjoitettu.
        val list = listOf(match("iso", pool = "300:00"), match("pieni", pool = "59:59"))
        assertEquals(
            listOf("pieni", "iso"),
            list.ordered(MatchOrder(MatchSortKey.TIME_POOL)).ids(),
        )
    }

    @Test
    fun `puuttuva arvo on viimeisena molempiin suuntiin`() {
        val list = listOf(
            match("tyhja", pool = null),
            match("viiva", pool = "-"),
            match("arvo", pool = "100:00"),
        )
        assertEquals(
            listOf("arvo", "tyhja", "viiva"),
            list.ordered(MatchOrder(MatchSortKey.TIME_POOL)).ids(),
        )
        assertEquals(
            listOf("arvo", "tyhja", "viiva"),
            list.ordered(MatchOrder(MatchSortKey.TIME_POOL, descending = true)).ids(),
        )
    }

    @Test
    fun `sama arvo sailyttaa sivuston keskinaisen jarjestyksen`() {
        val list = listOf(
            match("ensin", length = 15),
            match("sitten", length = 15),
            match("lyhyt", length = 9),
        )
        assertEquals(
            listOf("lyhyt", "ensin", "sitten"),
            list.ordered(MatchOrder(MatchSortKey.LENGTH)).ids(),
        )
    }

    @Test
    fun `vastustaja aakkosittain isoista kirjaimista riippumatta`() {
        val list = listOf(
            match("1", opponent = "Willie Wonka"),
            match("2", opponent = "alydar"),
            match("3", opponent = "Quastel"),
        )
        assertEquals(
            listOf("2", "3", "1"),
            list.ordered(MatchOrder(MatchSortKey.OPPONENT)).ids(),
        )
    }

    @Test
    fun `kierros vertaillaan ensimmaisesta luvusta`() {
        val list = listOf(match("a", round = "5/6"), match("b", round = "1/9"), match("c", round = "-"))
        assertEquals(listOf("b", "a", "c"), list.ordered(MatchOrder(MatchSortKey.ROUND)).ids())
    }

    @Test
    fun `kolmas napautus palaa sivuston jarjestykseen`() {
        val first = MatchOrder.SITE.tapped(MatchSortKey.GRACE)
        assertEquals(MatchOrder(MatchSortKey.GRACE, descending = false), first)
        val second = first.tapped(MatchSortKey.GRACE)
        assertEquals(MatchOrder(MatchSortKey.GRACE, descending = true), second)
        assertEquals(MatchOrder.SITE, second.tapped(MatchSortKey.GRACE))
    }

    @Test
    fun `toinen sarake aloittaa nousevasta`() {
        val descending = MatchOrder(MatchSortKey.GRACE, descending = true)
        assertEquals(
            MatchOrder(MatchSortKey.OPPONENT, descending = false),
            descending.tapped(MatchSortKey.OPPONENT),
        )
    }
}
