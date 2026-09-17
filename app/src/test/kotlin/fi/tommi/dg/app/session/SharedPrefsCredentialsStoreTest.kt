package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.GeneralSecurityException
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPrefsCredentialsStoreTest {

    private lateinit var prefs: SharedPreferences
    private var avainPoistettu = false

    @Before
    fun alusta() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("testi_tunnukset", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        avainPoistettu = false
    }

    private fun store(cipher: SecretCipher = toimivaSalaus()) = SharedPrefsCredentialsStore(
        prefs = prefs,
        cipher = cipher,
        deleteKey = { avainPoistettu = true },
    )

    /** Oikea AES-GCM mutta tavallisella avaimella: Keystore ei toimi Robolectricillä. */
    private fun toimivaSalaus(): SecretCipher {
        val avain = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return AesGcmCipher { avain }
    }

    @Test
    fun `tallennettu tunnus palautuu samana`() {
        val store = store()

        assertTrue(store.save("tommi", "salasana"))

        val tunnus = store.get()!!
        assertEquals("tommi", tunnus.login)
        assertEquals("salasana", tunnus.password)
        assertTrue(store.hasCredentials())
    }

    @Test
    fun `salasana ei ole levylla selkokielisena`() {
        // Koko muutoksen tarkoitus yhtenä väitteenä.
        store().save("tommi", "salasana")

        val levylla = prefs.all.values.joinToString(" ")
        assertFalse(levylla.contains("salasana"))
    }

    @Test
    fun `kayttajanimi on levylla selkokielisena`() {
        // Tietoinen valinta: nimi ei ole salaisuus, ja selkokielisenä se kertoo onko
        // tunnus tallessa myös silloin kun avain on mitätöitynyt.
        store().save("tommi", "salasana")

        assertTrue(prefs.all.values.joinToString(" ").contains("tommi"))
    }

    @Test
    fun `kayttajanimen valilyonnit siistitaan`() {
        // Näppäimistön automaattinen välilyönti nimen perässä tuottaisi muuten
        // hylätyn kirjautumisen josta ei näe syytä.
        val store = store()
        store.save("  tommi  ", "salasana")

        assertEquals("tommi", store.get()!!.login)
    }

    @Test
    fun `avautumaton salasana luetaan puuttuvaksi tunnukseksi`() {
        // Näin käy jos Keystoren avain on mitätöitynyt. Sovellus ei saa kaatua vaan
        // palata kirjautumisruutuun.
        val store = store()
        store.save("tommi", "salasana")

        val eiAukea = store(cipher = object : SecretCipher {
            override fun encrypt(plaintext: String) = plaintext
            override fun decrypt(stored: String): String? = null
        })

        assertNull(eiAukea.get())
        assertFalse(eiAukea.hasCredentials())
    }

    @Test
    fun `epaonnistunut salaus palauttaa false eika jata mitaan levylle`() {
        // Selkokielinen tallennus varmuuden vuoksi olisi väärä armo: se pettäisi juuri
        // sen lupauksen jonka takia salaus on olemassa.
        val rikki = object : SecretCipher {
            override fun encrypt(plaintext: String): String =
                throw GeneralSecurityException("laitteen salaus ei toimi")

            override fun decrypt(stored: String): String? = null
        }

        val store = store(cipher = rikki)

        assertFalse(store.save("tommi", "salasana"))
        assertEquals(emptyMap<String, Any>(), prefs.all)
        assertNull(store.get())
    }

    @Test
    fun `uloskirjautuminen tyhjentaa arvot ja poistaa avaimen`() {
        val store = store()
        store.save("tommi", "salasana")

        store.clear()

        assertEquals(emptyMap<String, Any>(), prefs.all)
        assertNull(store.get())
        assertTrue(avainPoistettu)
    }

    @Test
    fun `tyhja salasana ei kelpaa tunnukseksi`() {
        val store = store()
        store.save("tommi", "")

        assertNull(store.get())
    }

    @Test
    fun `pelkka kayttajanimi ilman salasanaa ei kelpaa`() {
        prefs.edit().putString("login", "tommi").commit()

        assertNull(store().get())
    }
}
