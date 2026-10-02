package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Varmuuskopion osoite ja aika levyllä.
 *
 * Kolme väitettä, ja keskimmäinen on 1.9.2026 tehty korjaus: vika unohtaa osoitteen muttei
 * aikaa. Aiemmin se pyyhki molemmat, jolloin seuraava käynnistys sanoi *"ei varmuuskopiota"*
 * vaikka kopio oli tallessa.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPrefsBackupTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun alusta() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("testi_varmuuskopio", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `osoite ja aika sailyvat`() {
        val store = SharedPrefsBackup(prefs)
        store.saveTarget("content://kohde")
        store.saveTime(1_788_281_892_040L)

        assertEquals("content://kohde", store.target())
        assertEquals(1_788_281_892_040L, store.lastSaved())
    }

    @Test
    fun `vika unohtaa osoitteen muttei aikaa`() {
        val store = SharedPrefsBackup(prefs)
        store.saveTarget("content://kohde")
        store.saveTime(1_788_281_892_040L)

        store.forget()

        assertNull(store.target())
        assertEquals(1_788_281_892_040L, store.lastSaved())
    }

    /** Uusi kohde on eri tiedosto, joten vanha aika ei kerro siitä mitään. */
    @Test
    fun `kohteen vaihto nollaa ajan`() {
        val store = SharedPrefsBackup(prefs)
        store.saveTarget("content://kohde")
        store.saveTime(1_788_281_892_040L)

        store.saveTarget("content://toinen")

        assertNull(store.lastSaved())
    }
}
