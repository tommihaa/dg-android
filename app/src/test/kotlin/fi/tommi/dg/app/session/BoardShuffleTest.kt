package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ottelukohtainen lauta (`docs/ASETUKSET.md` › Ottelukohtainen lauta).
 *
 * Kantava väite on pysyvyys: sama ottelu saa saman laudan joka kerta. Toinen on se, että
 * kytkin on oletuksena pois, jottei kenenkään lauta vaihdu itsestään päivityksen jälkeen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoardShuffleTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun alusta() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("testi_arvonta", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `kytkin on oletuksena pois ja joukko on sovelluksen omat laudat`() {
        val sailo = SharedPrefsBoardShuffle(prefs)
        assertFalse(sailo.on())
        assertEquals(BoardShuffleStore.DEFAULT_POOL, sailo.pool())
    }

    @Test
    fun `kytkin ja joukko sailyvat yli uuden lukijan`() {
        SharedPrefsBoardShuffle(prefs).apply {
            saveOn(true)
            savePool(setOf(BoardStyle.SITE, BoardStyle.DECO))
        }
        val luettu = SharedPrefsBoardShuffle(prefs)
        assertTrue(luettu.on())
        assertEquals(setOf(BoardStyle.SITE, BoardStyle.DECO), luettu.pool())
    }

    @Test
    fun `tyhja joukko antaa oletuksen`() {
        SharedPrefsBoardShuffle(prefs).savePool(emptySet())
        assertEquals(BoardShuffleStore.DEFAULT_POOL, SharedPrefsBoardShuffle(prefs).pool())
    }

    @Test
    fun `sama ottelu saa aina saman laudan`() {
        val joukko = BoardShuffleStore.DEFAULT_POOL
        repeat(50) { i ->
            val id = (5311000 + i).toString()
            assertEquals(boardForMatch(id, joukko), boardForMatch(id, joukko))
        }
    }

    @Test
    fun `lauta tulee aina joukosta ja jokainen joukon lauta esiintyy`() {
        val joukko = setOf(BoardStyle.X22, BoardStyle.MONTE_CARLO_VARIANT, BoardStyle.DECO)
        val nahdyt = (5311000 until 5311060).map { boardForMatch(it.toString(), joukko) }
        assertTrue(nahdyt.all { it in joukko })
        assertEquals(joukko, nahdyt.toSet())
    }

    @Test
    fun `yhden laudan joukko antaa sen laudan`() {
        assertEquals(BoardStyle.DECO, boardForMatch("5311448", setOf(BoardStyle.DECO)))
    }
}
