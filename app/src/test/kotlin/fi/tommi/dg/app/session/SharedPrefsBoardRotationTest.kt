package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.app.orientationFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Laudan pystytilan kytkin levyllä ja suuntasäännössä (`docs/ASETUKSET.md` luku 3,
 * `docs/UI.md` › Lauta pystyssä).
 *
 * Ensimmäinen väite on se jonka takia tämä testi on olemassa: **oletus on vaaka**. Jos oletus
 * vaihtuisi vahingossa, lauta kääntyisi pystyyn kaikilta joilla kytkintä ei ole koskettu, ja
 * puhelimella nappula putoaisi 29 dp:stä noin 20 dp:hen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPrefsBoardRotationTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun alusta() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("testi_laudan_suunta", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `oletus on vaaka`() {
        assertFalse(SharedPrefsBoardRotation(prefs).get())
    }

    @Test
    fun `kytkin sailyy yli uuden lukijan`() {
        SharedPrefsBoardRotation(prefs).save(true)
        assertTrue(SharedPrefsBoardRotation(prefs).get())
    }

    @Test
    fun `kytkin pois palauttaa vaakalukon laudalle`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            orientationFor(onBoard = true, portraitLock = true, boardRotates = false),
        )
    }

    @Test
    fun `kytkin paalla lauta seuraa laitetta pystylukosta riippumatta`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            orientationFor(onBoard = true, portraitLock = true, boardRotates = true),
        )
    }

    @Test
    fun `kirjoitushetki kaantaa pystyyn kytkimesta riippumatta`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            orientationFor(onBoard = true, portraitLock = false, writing = true, boardRotates = true),
        )
    }

    @Test
    fun `kytkin ei koske lukuruutuja`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            orientationFor(onBoard = false, portraitLock = true, boardRotates = true),
        )
    }

    // Kirjautumisruutu ja puhelimella kirjoittaminen (Tommin päätökset 1.10.2026).
    @Test
    fun `kirjautuminen ja kirjoittaminen ovat pystyssa vaikka pystylukko on pois`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            orientationFor(onBoard = false, portraitLock = false, signingIn = true),
        )
    }

    @Test
    fun `lauta ratkaisee ennen kirjautumisehtoa`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            orientationFor(onBoard = true, portraitLock = false, signingIn = true),
        )
    }
}
