package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Taustakuvien asettelu tyhjään tilaan (`DgWallpaper.kt`, `docs/UI.md` › *Taustakuvat
 * tyhjään tilaan*). Väitteet joita nämä vartioivat, kaikki Tommin päätöksistä 15.9.2026:
 * kuva ei koskaan leikkaudu eikä ylitä aluetta; kuvasuhde säilyy; pieneen tilaan tulee yksi
 * ja leveään tilaan kolme pystykuvaa rinnakkain; korkeaan tilaan vaakakuvat allekkain;
 * useampi kuva vain jos se peittää enemmän; ja kansion nimi luetaan urista luettavaan muotoon.
 */
class DgWallpaperLayoutTest {

    private val gap = 24f
    private val landscape = 1920f / 1200f
    private val wide = 3440f / 1440f
    private val portrait = 0.6f

    private fun assertInside(rects: List<androidx.compose.ui.geometry.Rect>, w: Float, h: Float) {
        for (r in rects) {
            assertTrue("vasen $r", r.left >= -0.5f)
            assertTrue("ylä $r", r.top >= -0.5f)
            assertTrue("oikea $r", r.right <= w + 0.5f)
            assertTrue("ala $r", r.bottom <= h + 0.5f)
        }
    }

    @Test
    fun kuvasuhdeSailyyJaKuvaPysyyAlueella() {
        val rects = dgWallpaperLayout(1080f, 700f, listOf(landscape, wide, portrait), gap)
        assertInside(rects, 1080f, 700f)
        val ratios = listOf(landscape, wide, portrait)
        rects.forEachIndexed { i, r -> assertEquals(ratios[i], r.width / r.height, 0.01f) }
    }

    @Test
    fun pieneenTilaanYksi() {
        // Kapea pystyruutu, matala vapaa tila: yksi vaakakuva koko korkeudelta, koska
        // korkeus rajaa ennen leveyttä (500 * 1,6 = 800 < 1080).
        val rects = dgWallpaperLayout(1080f, 500f, listOf(landscape, landscape, landscape), gap)
        assertEquals(1, rects.size)
        assertEquals(500f, rects[0].height, 0.5f)
        assertEquals(800f, rects[0].width, 0.5f)
    }

    @Test
    fun leveaanTilaanKolmePystykuvaaRinnakkain() {
        val rects = dgWallpaperLayout(1752f, 900f, listOf(portrait, portrait, portrait), gap)
        assertEquals(3, rects.size)
        assertInside(rects, 1752f, 900f)
        // Rinnakkain: sama yläreuna, kasvava vasen reuna.
        assertEquals(rects[0].top, rects[1].top, 0.01f)
        assertTrue(rects[0].right + gap - 0.5f <= rects[1].left)
        assertTrue(rects[1].right + gap - 0.5f <= rects[2].left)
    }

    @Test
    fun korkeaanTilaanVaakakuvatAllekkain() {
        val rects = dgWallpaperLayout(1080f, 1600f, listOf(landscape, landscape, landscape), gap)
        assertTrue("odotettiin useampaa, saatiin ${rects.size}", rects.size >= 2)
        assertInside(rects, 1080f, 1600f)
        assertEquals(rects[0].left, rects[1].left, 0.01f)
        assertTrue(rects[0].bottom + gap - 0.5f <= rects[1].top)
    }

    @Test
    fun useampiKuvaVainKunPeittoKasvaa() {
        // Yksi vaakakuva täyttää alueen tarkalleen: toista ei mahdu lisäämättä tyhjää.
        val rects = dgWallpaperLayout(1920f, 1200f, listOf(landscape, landscape), gap)
        assertEquals(1, rects.size)
    }

    @Test
    fun vahemmanKuviaKuinKolmeKelpaa() {
        assertEquals(1, dgWallpaperLayout(1752f, 900f, listOf(portrait), gap).size)
        assertEquals(2, dgWallpaperLayout(1752f, 900f, listOf(portrait, portrait), gap).size)
        assertEquals(0, dgWallpaperLayout(1752f, 900f, emptyList(), gap).size)
    }

    @Test
    fun ryhmaKeskitetaan() {
        val rects = dgWallpaperLayout(1752f, 900f, listOf(portrait, portrait, portrait), gap)
        val left = rects.first().left
        val right = 1752f - rects.last().right
        assertEquals(left, right, 0.5f)
    }

    @Test
    fun kansionNimiUrista() {
        assertEquals(
            "Pictures/dg-taustakuvat",
            dgWallpaperFolderName("content://com.android.externalstorage.documents/tree/primary%3APictures%2Fdg-taustakuvat"),
        )
        assertEquals("1234-5678:kuvat", dgWallpaperFolderName("content://x/tree/1234-5678%3Akuvat"))
        assertEquals("content://x/y", dgWallpaperFolderName("content://x/y"))
    }
}

/** Napautus ja nimi (`DgWallpaper.tap`, `dgWallpaperLabel`), Tommin tilaus 15.9.2026 illalla. */
class DgWallpaperTapTest {

    private fun wallpaper(): DgWallpaper = DgWallpaper(emptyList(), listOf("a", "b")).apply {
        rects = listOf(
            androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 100f),
            androidx.compose.ui.geometry.Rect(120f, 0f, 220f, 100f),
        )
    }

    @Test
    fun napautusKuvaanMerkitseeJaToinenPoistaa() {
        val w = wallpaper()
        w.tap(androidx.compose.ui.geometry.Offset(50f, 50f))
        assertEquals(0, w.labelled)
        w.tap(androidx.compose.ui.geometry.Offset(150f, 50f))
        assertEquals(1, w.labelled)
        w.tap(androidx.compose.ui.geometry.Offset(150f, 50f))
        assertEquals(null, w.labelled)
    }

    @Test
    fun napautusMuualleTyhjentaa() {
        val w = wallpaper()
        w.tap(androidx.compose.ui.geometry.Offset(50f, 50f))
        w.tap(androidx.compose.ui.geometry.Offset(300f, 300f))
        assertEquals(null, w.labelled)
    }

    @Test
    fun nimiIlmanPaatetta() {
        assertEquals("Bluesky__Anders Zorn Mora 1886_1920x1200", dgWallpaperLabel("Bluesky__Anders Zorn Mora 1886_1920x1200.jpg"))
        assertEquals("kuva", dgWallpaperLabel("kuva"))
        assertEquals(".piilo", dgWallpaperLabel(".piilo"))
    }
}
