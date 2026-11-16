package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Profiilin Active games- ja Finished matches -otsikoiden taitto (Tommin tilaus 12.11.2026,
 * `docs/UI.md` › Profiilin ottelulistat muistavat taittonsa).
 *
 * Oletus on auki, koska ruudun pitää näyttää samalta kuin ennen muutosta niille jotka eivät
 * ole koskeneet otsikoihin. Toinen väite on se jota tilaus pyysi: valinta säilyy uuden
 * lukijan yli, eli profiilin sulkemisen ja sovelluksen uudelleenkäynnistyksen yli.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPrefsProfileFoldTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun alusta() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("testi_profiilin_taitto", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `oletus on molemmat auki`() {
        assertEquals(ProfileFolds(activeFolded = false, finishedFolded = false), SharedPrefsProfileFold(prefs).get())
    }

    @Test
    fun `taitto sailyy yli uuden lukijan`() {
        SharedPrefsProfileFold(prefs).save(ProfileFolds(activeFolded = true, finishedFolded = false))
        assertEquals(ProfileFolds(activeFolded = true, finishedFolded = false), SharedPrefsProfileFold(prefs).get())
        SharedPrefsProfileFold(prefs).save(ProfileFolds(activeFolded = false, finishedFolded = true))
        assertEquals(ProfileFolds(activeFolded = false, finishedFolded = true), SharedPrefsProfileFold(prefs).get())
    }
}
