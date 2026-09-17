package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tilanneteksti keskikaistan vapaalla puoliskolla (Tommin päätös 8.9.2026), ks. [situationSlot].
 * Sama lokero kantaa 14.9.2026 alkaen koko [stripNotes]-pinon, ja sääntö on ennallaan.
 */
class SituationSlotTest {

    @Test
    fun `tarjous menee vastustajan puolelle`() {
        // YellowishSun has doubled: ei noppia, tarjottu kuutio omalla puolella.
        assertEquals(
            StripSlot.OPPONENT,
            situationSlot(hasNotes = true, opponentHasDice = false, selfHasDice = false, cubeOffered = true, stripHasActions = false),
        )
    }

    @Test
    fun `vastustajan nopat tyontavat tekstin omalle puolelle`() {
        assertEquals(
            StripSlot.SELF,
            situationSlot(hasNotes = true, opponentHasDice = true, selfHasDice = false, cubeOffered = false, stripHasActions = false),
        )
    }

    @Test
    fun `omat nopat ja vastustajan nopat yhta aikaa jattavat tekstin paneeliin`() {
        assertNull(situationSlot(hasNotes = true, opponentHasDice = true, selfHasDice = true, cubeOffered = false, stripHasActions = false))
    }

    @Test
    fun `napit kaistalla pitavat tekstin ennallaan`() {
        assertNull(situationSlot(hasNotes = true, opponentHasDice = false, selfHasDice = false, cubeOffered = true, stripHasActions = true))
    }

    @Test
    fun `ilman tilannetekstia ei lokeroa`() {
        assertNull(situationSlot(hasNotes = false, opponentHasDice = false, selfHasDice = false, cubeOffered = false, stripHasActions = false))
    }

    // Rastihuomautus samaan lokeroon (Tommin päätös 14.9.2026), ks. [verifyHintSlot].

    @Test
    fun `huomautus menee tilannetekstin alle kun teksti on kaistalla`() {
        assertEquals(
            StripSlot.OPPONENT,
            verifyHintSlot(situationSlot = StripSlot.OPPONENT, opponentHasDice = true, selfHasDice = true, cubeOffered = false, stripHasActions = false),
        )
    }

    @Test
    fun `double-tapauksessa vastustajan nopat vievat huomautuksen omalle puolelle`() {
        assertEquals(
            StripSlot.SELF,
            verifyHintSlot(situationSlot = null, opponentHasDice = true, selfHasDice = false, cubeOffered = false, stripHasActions = false),
        )
    }

    @Test
    fun `napit kaistalla jattavat huomautuksen laudan ylareunaan`() {
        assertNull(verifyHintSlot(situationSlot = null, opponentHasDice = false, selfHasDice = false, cubeOffered = false, stripHasActions = true))
    }
}
