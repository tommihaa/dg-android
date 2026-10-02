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

/**
 * Asetusten vienti ja tuonti (Tommin tilaus 28.9.2026). Kaksi laitetta on kaksi
 * tiedostojoukkoa eri etuliitteellä, joten sama testi vie yhdeltä ja tuo toiselle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsTransferTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun laite(tunnus: String): (String) -> SharedPreferences =
        { name -> context.getSharedPreferences("$tunnus-$name", Context.MODE_PRIVATE) }

    private val a = laite("a")
    private val b = laite("b")

    @Before
    fun tyhjenna() {
        (SettingsTransfer.FILES + listOf("dg_credentials", "dg_phrases")).forEach { name ->
            a(name).edit().clear().commit()
            b(name).edit().clear().commit()
        }
    }

    @Test
    fun `vienti ja tuonti siirtävät jokaisen tyypin sellaisenaan`() {
        a("dg_app_theme").edit().putString("app_theme", "DECO").putBoolean("busy_deco_before", false).commit()
        a("dg_board_shuffle").edit().putBoolean("on", true).putStringSet("pool", setOf("DECO", "X22")).commit()
        a("dg_match_order").edit().putString("sort_key", "OPPONENT").putInt("n", 3).putLong("t", 5L).putFloat("f", 1.5f).commit()
        val teksti = SettingsTransfer(a).export()
        assertTrue(teksti.startsWith(SettingsTransfer.PREFIX))
        assertFalse(teksti.any { it.isWhitespace() })

        val tuoja = SettingsTransfer(b)
        val sisalto = tuoja.parse(teksti)!!
        assertEquals(listOf("dg_app_theme", "dg_board_shuffle", "dg_match_order"), tuoja.changes(sisalto))
        tuoja.apply(sisalto)

        assertEquals("DECO", b("dg_app_theme").getString("app_theme", null))
        assertEquals(setOf("DECO", "X22"), b("dg_board_shuffle").getStringSet("pool", null))
        assertEquals(3, b("dg_match_order").getInt("n", 0))
        assertEquals(5L, b("dg_match_order").getLong("t", 0L))
        assertEquals(1.5f, b("dg_match_order").getFloat("f", 0f), 0f)
        assertEquals(emptyList<String>(), tuoja.changes(sisalto))
    }

    @Test
    fun `tuonti palauttaa puuttuvan avaimen oletukseensa`() {
        b("dg_handedness").edit().putBoolean("left_handed", true).commit()
        val tuoja = SettingsTransfer(b)
        val sisalto = tuoja.parse(SettingsTransfer(a).export())!!
        assertEquals(listOf("dg_handedness"), tuoja.changes(sisalto))
        tuoja.apply(sisalto)
        assertFalse(b("dg_handedness").contains("left_handed"))
    }

    @Test
    fun `tunnukset ja pikaviestit eivät kulje`() {
        a("dg_credentials").edit().putString("password", "salainen").commit()
        a("dg_phrases").edit().putString("p1", "gl").commit()
        val teksti = SettingsTransfer(a).export()
        SettingsTransfer(b).apply(SettingsTransfer(b).parse(teksti)!!)
        assertFalse(b("dg_credentials").contains("password"))
        assertFalse(b("dg_phrases").contains("p1"))
    }

    @Test
    fun `vieras tai katkennut teksti ei kelpaa`() {
        val tuoja = SettingsTransfer(b)
        val teksti = SettingsTransfer(a).export()
        assertNull(tuoja.parse("hello"))
        assertNull(tuoja.parse(teksti.dropLast(6)))
        assertNull(tuoja.parse(SettingsTransfer.PREFIX + "!!!"))
        // Rivitys ja välilyönnit liitettäessä eivät haittaa.
        assertTrue(tuoja.parse("  " + teksti.chunked(20).joinToString("\n") + " ") != null)
    }
}
