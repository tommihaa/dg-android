package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Turnauksen harvinaisuusporras voittoja putkeen, Tommin kuittaus 2.10.2026. */
class TournamentTierTest {

    @Test
    fun `kierrosmaara osuu portaaseensa`() {
        val odotettu = mapOf(3 to 0, 4 to 0, 5 to 1, 6 to 2, 7 to 3, 8 to 3, 9 to 4, 10 to 4)
        odotettu.forEach { (kierroksia, porras) ->
            assertEquals("$kierroksia kierrosta", porras, tournamentTier(kierroksia, "The Marathon #4434"))
        }
    }

    @Test
    fun `Champs on aina oranssi, kierrosmaarasta riippumatta`() {
        assertEquals(4, tournamentTier(7, "Tortoise Threers Champs #25"))
        assertEquals(4, tournamentTier(null, "Weekday Warriors Champs #26"))
        // Sana nimen osana ei riitä.
        assertNull(tournamentTier(null, "Champsville Open"))
    }

    @Test
    fun `vuosittainen turnaus on aina oranssi`() {
        assertEquals(4, tournamentTier(8, "2026 Open"))
        assertEquals(4, tournamentTier(null, "2025 Invitational"))
        assertEquals(4, tournamentTier(null, "2026 Double Repeat"))
        // Vuosiluku muualla kuin alussa ei tee vuosittaista: pelaajien oma turnaus ja
        // sarjanumero pysyvät ennallaan.
        assertNull(tournamentTier(null, "DG 8x8 2026 - H vs C - Div 1"))
        assertEquals(0, tournamentTier(4, "Strawberry Stratifieds #2005 Group 4"))
        // Kuukausiturnaus on kuukausi ja kaksi numeroa, ja se seuraa kierroksia.
        assertEquals(3, tournamentTier(8, "May 26 Nack"))
    }

    @Test
    fun `tuntematon kierrosmaara jattaa nimen varittomaksi`() {
        assertNull(tournamentTier(null, "DG 8x8 2026 - H vs C - Div 1"))
        assertNull(tournamentTier(0, "The Marathon #4434"))
    }

    @Test
    fun `kierrosteksti antaa kokonaismaaran`() {
        assertEquals(5, roundsFromText("3/5"))
        assertEquals(5, roundsFromText("Round 4/5"))
        assertNull(roundsFromText("Round 4"))
        assertNull(roundsFromText("-"))
        assertNull(roundsFromText(null))
    }

    @Test
    fun `turnausnumero polusta`() {
        assertEquals("112087", eventIdFromPath("/bg/event/112087"))
        assertNull(eventIdFromPath("/bg/user/20311"))
        assertNull(eventIdFromPath(null))
    }
}
