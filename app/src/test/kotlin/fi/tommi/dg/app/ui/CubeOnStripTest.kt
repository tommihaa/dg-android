package fi.tommi.dg.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kuution paikka: kaista vai lokerosarake. Ks. [cubeOnStrip]. */
class CubeOnStripTest {

    @Test
    fun `omistamaton kuutio on kaistalla kun napit ovat paneelissa`() {
        assertTrue(cubeOnStrip(stripHasActions = false, cubeOwned = false, cubeOffered = false))
    }

    @Test
    fun `omistettu kuutio on sarakkeessa`() {
        assertFalse(cubeOnStrip(stripHasActions = false, cubeOwned = true, cubeOffered = false))
    }

    @Test
    fun `uudelleentuplauksessa tarjottu kuutio on kaistalla vaikka sivu nayttaa sen omistajan solussa`() {
        // Tommin havainto 14.9.2026: redouble -> 4, kuutio jäi vastustajan lokeron päähän.
        assertTrue(cubeOnStrip(stripHasActions = false, cubeOwned = true, cubeOffered = true))
    }

    @Test
    fun `napit kaistalla vievat kaiken sarakkeeseen`() {
        assertFalse(cubeOnStrip(stripHasActions = true, cubeOwned = false, cubeOffered = true))
    }
}
