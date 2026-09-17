package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.PendingAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reittikoodauksen tarkkuus.
 *
 * Robolectricillä, koska `Uri.encode` on Androidin luokka. Sama syy kuin
 * `SharedPrefsCredentialsStoreTest`illä, eli tässä moduulissa jo tunnettu ratkaisu.
 *
 * **Rajaus jonka pitää olla luettavissa:** tämä väittää koodauksen ja purun olevan
 * häviöttömiä ja reitin rakenteen kestäviä. Se ei väitä mitään siitä miten
 * navigaatiokirjasto sisäisesti purkaa argumentin; kirjasto käyttää samaa `Uri.decode`a,
 * ja jos se joskus muuttuu, tämä testi ei huomaa sitä. Sen huomaa laitteella.
 */
@RunWith(RobolectricTestRunner::class)
// Sama kiinnitys kuin SharedPrefsCredentialsStoreTestillä: Robolectric 4.14.1 kattaa
// SDK 35:een asti, ja projektin targetSdk on 36. Uri.encode ei ole versiosta riippuvainen.
@Config(sdk = [34])
class DestinationsTest {

    @Test
    fun `pelipolku palautuu koodauksesta merkilleen samana`() {
        // Tämä on koko tiedoston olemassaolon syy. Väärin purkautunut polku ei tuottaisi
        // virhettä vaan haun väärään osoitteeseen oikeassa ottelussa.
        val polku = "/bg/move/5302842/541"
        assertEquals(polku, BoardRoute.decode(BoardRoute.encode(polku)))
    }

    @Test
    fun `koodattu polku ei sisalla reitin omia erotinmerkkeja`() {
        // Jos kauttaviiva jäisi koodaamatta, reitti "board/{path}" hajoaisi useaksi
        // segmentiksi ja argumentti katkeaisi ensimmäiseen kauttaviivaan.
        val koodattu = BoardRoute.encode("/bg/move/5302842/541")
        assertFalse("Kauttaviiva jäi koodaamatta: $koodattu", koodattu.contains('/'))
        assertFalse("Kysymysmerkki jäi koodaamatta: $koodattu", koodattu.contains('?'))
    }

    @Test
    fun `reitti rakennetaan ottelusta ja kantaa ottelutunnisteen`() {
        val reitti = checkNotNull(BoardRoute.of(ottelu(playPath = "/bg/move/5302842/541")))

        assertEquals(
            "board/${BoardRoute.encode("/bg/move/5302842/541")}?match=5302842",
            reitti,
        )
    }

    @Test
    fun `luettelon kierros ei kulje reitilla`() {
        // Kulki 8.9.2026 illan (`&round=`), poistettu samana iltana: reitti kattoi vain
        // napautetun ottelun, ja kierrokset kulkevat nyt `ListedRounds`in kautta.
        val reitti = checkNotNull(BoardRoute.of(ottelu(playPath = "/bg/move/5302842/541", round = "3/5")))

        assertEquals("board/${BoardRoute.encode("/bg/move/5302842/541")}?match=5302842", reitti)
    }

    @Test
    fun `ottelu ilman pelipolkua ei tuota reittia`() {
        // Profiilisivun otteluilla playPath on null, koska sivulla ei ole /bg/move/-linkkejä
        // lainkaan. Reitin rakentaminen tunnisteesta olisi juuri se osoitteen keksiminen
        // jota sääntö kieltää.
        assertNull(BoardRoute.of(ottelu(playPath = null)))
    }

    @Test
    fun `reitti odottavasta teosta on sama kuin ottelusta`() {
        // Kannan läpi kulkenut polku ei saa erota siitä jolla ruutu alun perin avattiin.
        // Kaksi eri reittiä samaan lautaan tarkoittaisi että toinen niistä on koottu.
        val polku = "/bg/move/5302842/541"

        assertEquals(
            checkNotNull(BoardRoute.of(ottelu(playPath = polku))),
            BoardRoute.of(odottavaTeko(boardPath = polku, matchId = MatchId("5302842"))),
        )
    }

    @Test
    fun `odottava teko ilman ottelutunnistetta jattaa kyselyosan pois`() {
        // Tyhjä arvo olisi eri asia kuin puuttuva: se olisi vertailukohta jota ei ole, ja
        // lautanäkymä vertaa sitä sivun omaan tunnisteeseen.
        val reitti = BoardRoute.of(odottavaTeko(boardPath = "/bg/move/5302842/541", matchId = null))

        assertEquals("board/${BoardRoute.encode("/bg/move/5302842/541")}", reitti)
    }

    private fun odottavaTeko(boardPath: String, matchId: MatchId?) = PendingAction(
        matchId = matchId,
        boardPath = boardPath,
        submit = "Roll Dice",
        pendingMove = null,
        verified = false,
        createdAtEpochMillis = 0,
    )

    private fun ottelu(playPath: String?, round: String? = null) = Match(
        id = MatchId("5302842"),
        eventName = "Some Event",
        eventId = null,
        opponent = PlayerRef(name = "Someone"),
        myTurn = true,
        round = round,
        matchLength = null,
        graceText = null,
        timePoolText = null,
        playPath = playPath,
        reviewPath = null,
    )

    @Test
    fun `porautumisreitti kantaa kyselyn ja fragmentin koskemattomana`() {
        // Profiililinkki kantaa kyselyn (`?days_to_view=...`) ja ketjun tai ottelun linkki
        // voi kantaa fragmentin. Kumpikin katoaisi tai rikkoisi reitin ilman koodausta.
        val path = "/bg/user/20311?days_to_view=100&active=1#end"
        val route = PageRoute.of(path)

        assertFalse(route.removePrefix("page/").contains("/"))
        assertFalse(route.removePrefix("page/").contains("?"))
        assertEquals(path, BoardRoute.decode(route.removePrefix("page/")))
    }
}
