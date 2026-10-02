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
 * Fraasinappien lista levyllä.
 *
 * Keskimmäinen väite on se jonka takia säilö ei ole pelkkä `getString`: kun käyttäjä on
 * poistanut viimeisenkin fraasin, seuraava käynnistys ei saa palauttaa oletusta. Oletus on
 * vain uuden asennuksen tila.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPrefsPhrasesTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun alusta() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("testi_fraasit", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `uusi asennus alkaa rituaalifraaseista`() {
        assertEquals(listOf("hi", "gg", "ty gg u2"), SharedPrefsPhrases(prefs).get())
    }

    @Test
    fun `tyhjaksi talletettu lista pysyy tyhjana eika palaudu oletukseen`() {
        val store = SharedPrefsPhrases(prefs)
        store.save(emptyList())

        assertEquals(emptyList<String>(), SharedPrefsPhrases(prefs).get())
    }

    @Test
    fun `jarjestys ja valilyonnit sailyvat`() {
        val store = SharedPrefsPhrases(prefs)
        store.save(listOf("ty gg u2", "gg", "hi", "good luck"))

        assertEquals(listOf("ty gg u2", "gg", "hi", "good luck"), SharedPrefsPhrases(prefs).get())
    }
}
