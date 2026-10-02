package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.InviteChoice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Vastaanotettu ottelukutsu, mitattu 14.9.2026 (`invitation_received.html`, ackammonin
 * kutsu jonon kärjestä). Kolme lomaketta samaan osoitteeseen, ja kuvaus `<h3>`:n jälkeen.
 */
class InvitationParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use {
            it.readBytes().toString(Charsets.UTF_8)
        }

    @Test
    fun `kutsuja ja kuvaus luetaan sivun sanoin`() {
        val invitation = checkNotNull(InvitationParser.parse(fixture("invitation_received.html")))

        assertEquals("anonplayer", invitation.from.name)
        assertEquals("10001", invitation.from.userId)
        // Kuvaus ei ole otsikossa vaan sen jälkeen irrallisena tekstinä. InboxParser luki
        // 14.9.2026 vain otsikon, ja arkistoon jäi "invites you to play" ilman pituutta.
        assertEquals(
            "a private 5 point match of backgammon with no time control.",
            invitation.description,
        )
    }

    @Test
    fun `hyvaksynta on GET ja hylkays POST syykentalla, molemmat sivun osoitteeseen`() {
        val invitation = checkNotNull(InvitationParser.parse(fixture("invitation_received.html")))

        val accept = invitation.accept.write()
        assertEquals("/bg/invite/900001", accept.action)
        assertEquals(FormMethod.GET, accept.method)
        assertEquals(mapOf("action" to "accept"), accept.fields)
        assertEquals("Accept Invitation", invitation.acceptLabel)

        val decline = invitation.decline.write("no thanks")
        assertEquals("/bg/invite/900001", decline.action)
        assertEquals(FormMethod.POST, decline.method)
        assertEquals(mapOf("action" to "decline", "reason" to "no thanks"), decline.fields)
        assertEquals(80, invitation.decline.reason?.maxLength)
        assertEquals("Decline Invitation", invitation.declineLabel)
    }

    @Test
    fun `vastatarjous on sama lomake kuin kutsu, ja private perii kutsun arvon`() {
        val invitation = checkNotNull(InvitationParser.parse(fixture("invitation_received.html")))
        val counter = checkNotNull(invitation.counter)

        assertEquals("Counter Offer", invitation.counterLabel)
        assertEquals(mapOf("action" to "counter"), counter.hidden)
        assertTrue(counter.length.offers("7"))
        val submission = checkNotNull(
            counter.write(InviteChoice(variant = "1", length = "7", timeControl = "0", privateMatch = true))
        )
        assertEquals(FormMethod.POST, submission.method)
        assertEquals("counter", submission.fields["action"])
        assertEquals("private", submission.fields["private"])
    }

    @Test
    fun `lahetetyn kutsun kuittaus ei ole vastaanotettu kutsu vaikka otsikko on sama`() {
        // Sama <title>, mutta ei yhtään lomaketta. Tunnistus on lomakkeessa.
        assertNull(InvitationParser.parse(fixture("invite_sent.html")))
    }

    @Test
    fun `viestisivu ja lauta eivat ole kutsuja`() {
        assertNull(InvitationParser.parse(fixture("inbox_quick_message.html")))
        assertNotNull(InvitationParser.parse(fixture("invitation_received.html")))
    }

    @Test
    fun `vastaussivujen lause luetaan sivuston sanoin`() {
        // Mitattu 14.9.2026: hyvaksynta ja vastatarjous vastaavat omalla sivullaan, jonka
        // otsikko on pelkka DailyGammon. Lause on se joka menee ruudulle napin alle.
        assertEquals(
            "You have successfully joined that game.",
            SendResultParser.notice(fixture("invitation_accepted.html")),
        )
        assertEquals(
            "Your counter-offer has been sent.",
            SendResultParser.notice(fixture("invitation_countered.html")),
        )
        // Kumpikaan ei ole vastaanotettu kutsu, vaikka osoite on sama.
        assertNull(InvitationParser.parse(fixture("invitation_accepted.html")))
        assertNull(InvitationParser.parse(fixture("invitation_countered.html")))
    }
}
