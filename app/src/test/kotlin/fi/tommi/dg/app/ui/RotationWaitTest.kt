package fi.tommi.dg.app.ui

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Käännön odotusmerkin anturilukema (`RotationWait.kt`, Tommin valinta 15.10.2026). */
class RotationWaitTest {

    @Test
    fun pystyJaSivuttainTunnistetaan() {
        assertEquals(false, sidewaysFor(0, null))
        assertEquals(false, sidewaysFor(350, null))
        assertEquals(false, sidewaysFor(180, null))
        assertEquals(true, sidewaysFor(90, null))
        assertEquals(true, sidewaysFor(270, null))
    }

    @Test
    fun rajojenValissaAsentoPysyyEdellisena() {
        // 45 asteen kohdalla pidetty laite ei saa vilkuttaa merkkiä kumpaankaan suuntaan.
        assertEquals(true, sidewaysFor(45, true))
        assertEquals(false, sidewaysFor(45, false))
        assertEquals(null, sidewaysFor(135, null))
    }

    @Test
    fun vaakatasossaAsentoaEiTiedeta() {
        assertEquals(null, sidewaysFor(-1, true))
    }

    @Test
    fun omaKaantoLukeeSuunnanKulmasta() {
        // Pixel 8a 19.10.2026: kulma 288 käänsi Androidin kiertoon 1 eli LANDSCAPE.
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, targetFor(288, null))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, targetFor(90, null))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, targetFor(10, null))
    }

    @Test
    fun omaKaantoPitaaSuunnanVaakatasossaJaYlosalaisin() {
        val vaaka = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        assertEquals(vaaka, targetFor(-1, vaaka))
        assertEquals(vaaka, targetFor(180, vaaka))
        assertEquals(vaaka, targetFor(45, vaaka))
    }
}
