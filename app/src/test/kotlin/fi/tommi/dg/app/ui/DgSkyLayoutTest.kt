package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Taustan metamorfoosin asettelu (`DgPattern.kt`, `docs/UI.md` kohta *Taustakuvio
 * tekstiruuduissa*).
 *
 * Väitteet joita nämä vartioivat: sama siemen antaa saman taivaan ja eri siemen eri taivaan,
 * koska arpa heitetään kutsupaikassa ja asettelun pitää olla sen suhteen deterministinen;
 * alhaalla on ehjiä kuutioita ja ylhäällä valmiita lintuja (Tommin suunta 6.9.2026), eli
 * kolme vyöhykettä ovat olemassa eivätkä vain nimiä; linnut harvenevat ylöspäin; ja vyöhykerajat pysyvät
 * välillä jolla siirtymä on nähtävissä yhdellä ruudulla.
 */
class DgSkyLayoutTest {

    private val width = 1080f
    private val height = 2340f
    private val side = 55f

    @Test
    fun samaSiemenAntaaSamanTaivaan() {
        val a = dgSkyLayout(width, height, side, seed = 42)
        val b = dgSkyLayout(width, height, side, seed = 42)
        assertEquals(a, b)
    }

    @Test
    fun eriSiemenAntaaEriTaivaan() {
        val a = dgSkyLayout(width, height, side, seed = 1)
        val b = dgSkyLayout(width, height, side, seed = 2)
        assertNotEquals(a.cells, b.cells)
    }

    @Test
    fun alhaallaKuutioitaJaYlhaallaLintuja() {
        val layout = dgSkyLayout(width, height, side, seed = 7)
        val top = layout.cells.filter { it.cy in 0f..(height * 0.1f) }
        val bottom = layout.cells.filter { it.cy >= height * 0.85f }
        assertTrue(top.isNotEmpty())
        assertTrue(bottom.isNotEmpty())
        assertTrue(bottom.all { it.p == 0f })
        assertTrue(top.all { it.p == 1f })
    }

    @Test
    fun linnutHarvenevatYlospain() {
        val layout = dgSkyLayout(width, height, side, seed = 7)
        val band = height * 0.1f
        val top = layout.cells.count { it.cy in 0f..band }
        val bottom = layout.cells.count { it.cy in (height - band)..height }
        assertTrue("ylhäällä $top, alhaalla $bottom", top < bottom)
    }

    @Test
    fun kuuOnLintualueellaRuudunSisalla() {
        for (seed in 0 until 200) {
            val layout = dgSkyLayout(width, height, side, seed)
            val moon = layout.moon
            // Keskipiste ruudun yläosassa: alle 30 % korkeudesta ylhäältä, eli lintujen
            // puolella (lintujen alkuraja on vähintään 55 % alhaalta).
            assertTrue(moon.cy in 0f..(height * 0.30f))
            assertTrue(moon.cx in 0f..width)
            assertTrue(moon.r in (width * 0.22f)..(width * 0.30f))
        }
    }

    @Test
    fun tummanKuutiokaistaOnKapeampi() {
        // Tommin rajaus 6.9.2026: alaosasta ei saa olla kolmannesta monotonista kuutiota.
        // Tumman arvot ovat samat kuin DgPattern.kt:n NightCube*-vakiot.
        for (seed in 0 until 200) {
            val layout = dgSkyLayout(width, height, side, seed, cubeFloor = 0.05f, cubeSpan = 0.05f, band = 0.30f)
            assertTrue(layout.cubeEnd in 0.05f..0.10f)
            assertTrue(layout.birdStart in 0.35f..0.50f)
        }
    }

    @Test
    fun yopalettiSeuraaSiementaJaKaikkiViisiTulevatKayttoon() {
        assertEquals(dgNightPalette(42), dgNightPalette(42))
        val seen = (0 until 50).map { dgNightPalette(it) }.toSet()
        assertEquals(DgNightPalettes.size, seen.size)
        // Negatiivinen siemen on tavallinen (Random.nextInt), eikä se saa kaataa valintaa.
        dgNightPalette(-7)
        dgNightPalette(Int.MIN_VALUE)
    }

    @Test
    fun jokainenMotiiviPiirtyyKokoMatkalta() {
        // Vain pisteet: polut ovat android.graphics.Path, jota JVM-testi ei voi luoda.
        for (palette in DgNightPalettes) {
            for (i in 0..40) {
                val p = palette.motif(-1f + 2f * i / 40f, 40f, 55f)
                assertTrue(palette.name, p.x.isFinite() && p.y.isFinite())
            }
        }
    }

    @Test
    fun vyohykerajatPysyvatValilla() {
        for (seed in 0 until 200) {
            val layout = dgSkyLayout(width, height, side, seed)
            assertTrue(layout.cubeEnd in 0.15f..0.30f)
            assertTrue(layout.birdStart in 0.55f..0.80f)
            assertTrue(layout.sparseStart < 0.90f)
        }
    }
}
