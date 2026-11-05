package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.domain.Phrase
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
        assertEquals(listOf("hi", "gg", "ty gg u2"), SharedPrefsPhrases(prefs).get().map { it.text })
    }

    @Test
    fun `tyhjaksi talletettu lista pysyy tyhjana eika palaudu oletukseen`() {
        val store = SharedPrefsPhrases(prefs)
        store.save(emptyList())

        assertEquals(emptyList<String>(), SharedPrefsPhrases(prefs).get())
    }

    @Test
    fun `jarjestys valilyonnit ja hetket sailyvat`() {
        val store = SharedPrefsPhrases(prefs)
        val fraasit = listOf(Phrase("ty gg u2", 1), Phrase("gg", 2), Phrase("hi", 3), Phrase("good luck", 1_792_000_000_000))
        store.save(fraasit)

        assertEquals(fraasit, SharedPrefsPhrases(prefs).get())
    }

    @Test
    fun `ennen hetkia tallennettu lista saa hetket jarjestyksesta`() {
        // Ennen 20.10.2026 säilössä oli vain tekstirivi. Se luetaan sellaisenaan, ja hetket
        // tulevat paikasta, jotta näkyvä järjestys ei muutu päivityksessä.
        prefs.edit().putString("phrases", "hi there, ty u2\nhi\ngg").commit()

        assertEquals(
            listOf(Phrase("hi there, ty u2", 1), Phrase("hi", 2), Phrase("gg", 3)),
            SharedPrefsPhrases(prefs).get(),
        )
    }
}
