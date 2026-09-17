package fi.tommi.dg.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Maksavatko järjestelmän palkit nappulan kokoa, eli piilotetaanko ne lautaruudulla.
 *
 * Testi on olemassa siksi että päätöksen molemmat puolet ovat oikein juuri niin kauan kuin
 * mitat pysyvät samoina. `CHECKER_MAX`in tai `SCREEN_CHROME`n muutos siirtää kääntöpistettä,
 * ja siirtymä olisi hiljainen: väärä puoli ei näy virheenä vaan pienempänä nappulana tai
 * kadonneena poistumistienä. Kumpaakaan ei huomaa kääntäjä.
 *
 * Mitat on luettu laitteilta 20.8.2026 (`wm size`, `wm density`, `dumpsys window displays`).
 */
class DgBoardBarsTest {

    /** Galaxy Tab S7+ vaaka: 2800 x 1752 px tiheydellä 2,125. */
    private val TABLET_HEIGHT = 824.dp

    /**
     * Saman laitteen palkkien pystysuora osuus: tilapalkki 51 px ja navigointipalkki 51 px.
     *
     * Toisin kuin puhelimella, tabletin navigointipalkki on **vaakatilassa alhaalla**, joten
     * se kuuluu tähän summaan. Se on mitattu lautaruudulta 20.8.2026: sisältö päättyi
     * y=1623:een 1752:sta, eli alalaitaan jäi palkin verran tilaa.
     */
    private val TABLET_BARS = 48.dp

    /** Pixel 8a vaaka: 2400 x 1080 px tiheydellä 2,625. */
    private val PIXEL_HEIGHT = 411.dp

    /**
     * Saman laitteen palkkien **pystysuora** osuus, ja se on tilapalkki yksin.
     *
     * **Tässä luki 72 dp 20.8.2026 asti, ja luku oli väärä.** Se oli arvattu summa
     * tilapalkista ja navigointipalkista, ja mittaus samana päivänä kaatoi sen: Pixel 8a:lla
     * navigointipalkki on **vaakatilassa oikeassa reunassa** (`sideHint=RIGHT`, 126 px eli
     * 48 dp), joten se ei vie korkeutta lainkaan. Pystysuunnassa jää tilapalkki, 74 px eli
     * 28 dp.
     *
     * Luku on tässä sen muotoisena kuin [DgBoard.barsAreFree] sen saa, eli `getTop`in ja
     * `getBottom`in summana. Sivulla oleva palkki ei kuulu siihen, ja juuri se oli virheen
     * koko sisältö: summattiin kaksi palkkia joista toinen on eri akselilla.
     */
    private val PIXEL_BARS = 28.dp

    @Test
    fun `tabletilla palkit ovat ilmaisia`() {
        // Nappula on siellä katossa jo ilman piilotusta, joten piilotus ostaisi tyhjää ja
        // maksaisi ainoan tien pois ruudulta.
        assertTrue(DgBoard.barsAreFree(TABLET_HEIGHT, TABLET_BARS))
    }

    @Test
    fun `puhelimella palkit maksavat nappulan kokoa`() {
        assertFalse(DgBoard.barsAreFree(PIXEL_HEIGHT, PIXEL_BARS))
    }

    @Test
    fun `kumpikaan laite ei ole rajatapaus`() {
        // Kääntöpiste on se sisäkorkeus jolla nappula osuu tasan kattoon. Molempien
        // laitteiden pitää olla siitä selvästi omalla puolellaan, jottei pieni muutos
        // fonttikoossa tai palkin korkeudessa heiluta päätöstä kesken pelin.
        val turningPoint = DgBoard.CHECKER_MAX *
            (DgBoard.CHECKERS_APART * 2 + DgBoard.BAND_PER_CHECKER)
        assertEquals(648.dp.value, turningPoint.value, 0.01f)

        val tabletInner = TABLET_HEIGHT - TABLET_BARS - DgBoard.SCREEN_CHROME
        val pixelInner = PIXEL_HEIGHT - PIXEL_BARS - DgBoard.SCREEN_CHROME
        assertTrue("tabletilla varaa ${tabletInner - turningPoint}", tabletInner - turningPoint > 50.dp)
        assertTrue("puhelimella varaa ${turningPoint - pixelInner}", turningPoint - pixelInner > 50.dp)
        // Puhelimen varaa on yli 300 dp, eli oikean palkkiluvun jälkeenkin se on kaukana
        // kynnyksestä. Väärä 72 dp:n arvaus ei siis muuttanut yhtään päätöstä, ja juuri
        // siksi se olisi jäänyt huomaamatta ilman mittausta.
        assertTrue(turningPoint - pixelInner > 300.dp)
    }

    @Test
    fun `sivulla oleva palkki ei ole ilmainen vaikka korkeus riittaisi`() {
        // Puhelimen vaakatilassa kolmen napin palkki on oikeassa reunassa (48 dp) eikä vie
        // korkeutta lainkaan, mutta se vie sivupaneelin leveyttä. Korkeusehto yksin sanoisi
        // tabletin mitoilla *ilmainen*; leveys kääntää sen (Tommin päätös 16.9.2026).
        assertFalse(DgBoard.barsAreFree(TABLET_HEIGHT, TABLET_BARS, barsWidth = 48.dp))
        assertTrue(DgBoard.barsAreFree(TABLET_HEIGHT, TABLET_BARS, barsWidth = 0.dp))
    }

    @Test
    fun `piilotus ei ole laitekysymys vaan korkeuskysymys`() {
        // Sama tabletti pystyasennossa ei ole tässä testissä, koska ruutu on vaakalukossa.
        // Sen sijaan tämä: kun ikkuna kutistuu tabletillakin riittävästi, ehto kääntyy.
        // Ilman tätä väite "ehto on mitta eikä laite" olisi pelkkä kommentti.
        assertFalse(DgBoard.barsAreFree(600.dp, TABLET_BARS))
    }
}
