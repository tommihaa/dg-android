package fi.tommi.dg.app.session

import org.junit.Assert.assertEquals
import org.junit.Test

/** Eleiden kytkin: pääkytkin ohittaa ruudut, ja tuntematon nimi ohitetaan (`GestureStore`). */
class GestureStoreTest {

    @Test
    fun `paakytkin paalla sallii kaikki`() {
        assertEquals(Gesture.entries.toSet(), activeGestures(true, emptySet()))
    }

    @Test
    fun `paakytkin pois sammuttaa kaikki`() {
        assertEquals(emptySet<Gesture>(), activeGestures(false, emptySet()))
    }

    @Test
    fun `ruutu sammuttaa vain oman eleensa`() {
        assertEquals(
            Gesture.entries.toSet() - Gesture.BUZZ,
            activeGestures(true, setOf(Gesture.BUZZ)),
        )
    }

    @Test
    fun `tuntematon nimi jaa pois`() {
        assertEquals(setOf(Gesture.SWIPE_TABS), gesturesNamed(setOf("SWIPE_TABS", "POISTETTU")))
    }
}
