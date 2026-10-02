package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Kätisyys levyllä (`docs/ASETUKSET.md` luku 3).
 *
 * Ensimmäinen väite on se jonka takia tämä testi on olemassa: **oletus on oikeakätinen**, eli
 * asentamaton kytkin antaa saman paneelin puolen kuin ennen 9.9.2026 tehtyä muutosta. Jos
 * oletus vaihtuisi vahingossa, sivupaneeli siirtyisi kaikilta joilla kytkintä ei ole koskettu,
 * eikä mikään ruudulla kertoisi miksi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPrefsHandednessTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun alusta() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("testi_katisyys", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `oletus on oikeakatinen`() {
        assertFalse(SharedPrefsHandedness(prefs).get())
    }

    @Test
    fun `vasenkatisyys sailyy yli uuden lukijan`() {
        SharedPrefsHandedness(prefs).save(true)
        assertTrue(SharedPrefsHandedness(prefs).get())
    }

    @Test
    fun `takaisin oikeakatiseksi ei jaa vasenta paalle`() {
        val sailo = SharedPrefsHandedness(prefs)
        sailo.save(true)
        sailo.save(false)
        assertFalse(SharedPrefsHandedness(prefs).get())
    }
}
