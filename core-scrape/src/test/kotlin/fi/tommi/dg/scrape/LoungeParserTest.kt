package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.write
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoungeParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val page = LoungeParser.parse(fixture("lounge_page.html"))!!

    @Test
    fun `kayttaja luetaan tervehdyksesta`() {
        assertEquals("pelaaja", page.userName)
    }

    @Test
    fun `kaikki kutsurivit loytyvat ja otsikkorivi ei tule mukaan`() {
        assertEquals(3, page.invitations.size)
    }

    @Test
    fun `kutsurivi jasentyy kokonaan`() {
        val invitation = page.invitations.first()
        assertEquals("backgammon", invitation.variant)
        assertEquals(1, invitation.length)
        assertEquals("vastustaja", invitation.player.name)
        assertEquals("10002", invitation.player.userId)
        assertEquals("Once a Day (200/+4/24)", invitation.timeout)
        assertNull(invitation.comment)
    }

    @Test
    fun `join-polku on sivun oma href kyselyineen`() {
        // Polku sellaisenaan: kysely on palvelimen oma eikä sitä pureta tai kasata.
        assertEquals(
            "/bg/lounge?action=accept&id=11&userid=10001",
            page.invitations.first().joinPath,
        )
    }

    @Test
    fun `kommentti luetaan kun solu ei ole tyhja`() {
        assertEquals("friendly game welcome", page.invitations[1].comment)
    }

    @Test
    fun `kaikki turnausrivit loytyvat`() {
        assertEquals(7, page.tournaments.size)
    }

    @Test
    fun `jo ilmoittaudutulla rivilla on cancel-polku eika sign uppia`() {
        // Sign Upin tilalla on Cancel Signup (mitattu 3.9.2026), ja rivillä on Has Note.
        // Polku sellaisenaan kyselyineen kuten signupPath.
        val signed = page.tournaments.first { it.eventId == "900007" }
        assertEquals("Signed Champs #5", signed.name)
        assertNull(signed.signupPath)
        assertEquals("/bg/lounge?action=cancel&event=900007&userid=10001", signed.cancelPath)
        assertEquals("24:00", signed.graceText)
        assertTrue(signed.hasNote)
    }

    @Test
    fun `has note luetaan vain riveilta joilla se on`() {
        assertTrue(page.tournaments.first { it.eventId == "900003" }.hasNote)
        assertTrue(page.tournaments.first { it.eventId == "900004" }.hasNote)
        assertFalse(page.tournaments.first { it.eventId == "900001" }.hasNote)
        assertNull(page.tournaments.first { it.eventId == "900001" }.cancelPath)
    }

    @Test
    fun `turnausrivi jasentyy kokonaan`() {
        val tournament = page.tournaments.first()
        assertEquals("Sample Championship", tournament.name)
        assertEquals("900001", tournament.eventId)
        assertEquals("/bg/event/900001", tournament.eventPath)
        assertEquals("backgammon", tournament.variant)
        assertEquals(11, tournament.length)
        assertEquals(9, tournament.rounds)
        assertEquals("150:00", tournament.timeText)
        assertEquals("+2:00", tournament.incrementText)
        assertEquals("24:00", tournament.graceText)
        assertEquals("/bg/lounge?action=signup&event=900001&userid=10001", tournament.signupPath)
    }

    @Test
    fun `sign up voi puuttua rivilta`() {
        // Ilmoittautuminen ei ole auki: solu on &nbsp; eikä linkki, ja polku jää nulliksi.
        val closed = page.tournaments.first { it.name == "Plain Closed #3" }
        assertNull(closed.signupPath)
    }

    @Test
    fun `has note -lisasolu ei siirra sarakkeita eika sotke sign uppia`() {
        // Has Note -solussa on /bg/event/-linkki rivin lopussa; ankkurin on oltava
        // ensimmäinen tapahtumalinkkisolu eikä viimeinen. Rivillä 900003 ei ole Sign Uppia
        // ja rivillä 900004 on, ja Has Note on molemmilla.
        val noted = page.tournaments.first { it.eventId == "900003" }
        assertEquals("Closed Champs #1", noted.name)
        assertEquals("24:00", noted.graceText)
        assertNull(noted.signupPath)

        val notedOpen = page.tournaments.first { it.eventId == "900004" }
        assertEquals("/bg/lounge?action=signup&event=900004&userid=10001", notedOpen.signupPath)
        assertEquals("24:00", notedOpen.graceText)
    }

    @Test
    fun `pelaajalistan ja hallin polut luetaan sivun linkeista`() {
        assertEquals("/bg/plist", page.playerListPath)
        assertEquals("/bg/thall", page.tournamentHallPath)
    }

    @Test
    fun `pelaajahaun lomake luetaan kenttineen`() {
        // Osoite, metodi, tekstikentän nimi ja piilokenttä ovat kaikki sivun omia
        // (mitattu 27.8.2026): ilman `type=name` palvelin ei tiedä mitä haetaan.
        val form = page.searchForm!!
        assertEquals("/bg/plist", form.action)
        assertEquals(FormMethod.POST, form.method)
        assertEquals("like", form.field)
        assertEquals(mapOf("type" to "name"), form.hidden)
        assertEquals(40, form.maxLength)

        val submission = form.write("  tom ")!!
        assertEquals(mapOf("like" to "tom", "type" to "name"), submission.fields)
        assertEquals(null, form.write("   "))
    }

    @Test
    fun `tyhja lounge jasentyy tyhjaksi eika kaadu`() {
        // Mitattu tila 22.8.2026: molemmat taulukot voivat puuttua kokonaan, ja sivusta
        // jää jäljelle tarjouslomake. Synteettinen runko samasta muodosta.
        val html = """
            <HTML><BODY>
            <h2>Welcome to the DailyGammon waiting lounge, pelaaja.</h2>
            <FORM ACTION=/bg/lounge METHOD=POST><INPUT TYPE=submit VALUE=Submit></FORM>
            </BODY></HTML>
        """.trimIndent()

        val parsed = LoungeParser.parse(html)!!

        assertEquals("pelaaja", parsed.userName)
        assertEquals(0, parsed.invitations.size)
        assertEquals(0, parsed.tournaments.size)
    }
}
