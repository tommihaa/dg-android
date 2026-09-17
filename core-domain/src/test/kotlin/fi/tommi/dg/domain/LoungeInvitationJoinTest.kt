package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoungeInvitationJoinTest {

    private fun invitation(joinPath: String?) = LoungeInvitation(
        variant = "backgammon",
        length = 1,
        player = PlayerRef(name = "vastustaja", userId = "10002"),
        timeout = "Once a Day (200/+4/24)",
        comment = null,
        joinPath = joinPath,
    )

    @Test
    fun `join on sivun oma href sellaisenaan tyhjin kentin`() {
        // Takuu on kokoamattomuus: linkin oma kysely kulkee actionissa koskemattomana,
        // eikä kenttiä ole, jottei sendAsQuery kirjoita kyselyä uudelleen.
        val submission = invitation("/bg/lounge?action=accept&id=11&userid=10001").join()

        checkNotNull(submission)
        assertEquals("/bg/lounge?action=accept&id=11&userid=10001", submission.action)
        assertEquals(FormMethod.GET, submission.method)
        assertTrue(submission.fields.isEmpty())
    }

    @Test
    fun `ilman join-linkkia ei synny lahetysta`() {
        // Oma tarjous: rivi on olemassa mutta sivu ei tarjoa Joinia, joten painettavaa
        // ei ole eikä sitä voi keksiä.
        assertNull(invitation(joinPath = null).join())
    }
}
