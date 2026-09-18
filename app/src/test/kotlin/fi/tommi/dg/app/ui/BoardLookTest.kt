package fi.tommi.dg.app.ui

import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.SiteBoardSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Piirtotavan ratkaisu tyylistä, säilöstä ja sivun skeemasta.
 *
 * Väitteet joita nämä vartioivat (`docs/ASETUKSET.md` luku 4): X-22 ei lue säilön värejä
 * mutta **peilaa asetuksen mukaan 9.9.2026 alkaen**; sivustouskollinen väri tulee skeemasta
 * ja ensisijainen lähde on sivu itse; tuntematon skeema ei väitä sivuston värejä; ja
 * peilaus seuraa asetusta väreistä riippumatta, koska se tulee eri kentästä.
 */
class BoardLookTest {

    private val site = SiteBoardSettings(
        hidePips = false,
        homeBoardsLeft = true,
        scheme = BoardScheme.CLASSIC,
    )

    @Test
    fun `x22 peilaa asetuksen mukaan mutta ei lue sivuston vareja`() {
        // Käännetty 9.9.2026 (Tommin päätös). Tämä testi vartioi aiemmin sitä että X-22 ei
        // peilaa lainkaan, ja juuri se väitti mitatun vian olevan kaanonia: `Home boards on
        // left side` ei tehnyt Tommin omalla tyylillä mitään. X-22:n kiinteys koskee kiiloja
        // ja värejä, ei sitä kummalla puolella pelaajan kotikenttä on.
        val look = BoardLook.of(BoardStyle.X22, site, pageScheme = BoardScheme.BLUE_WHITE)

        assertTrue(look.mirrored)
        assertNull(look.sitePaint(CheckerColor.YELLOW))
        assertEquals(DgBoard.Palette.WedgeEven, look.wedgeEven)
    }

    @Test
    fun `monte carlo variant vaihtaa huovan ja roolivarit mutta ei lue sivuston vareja`() {
        // Ainoa tyyli joka vaihtaa huovan (kaanonin tarkennus 13.9.2026). Roolivärit ovat
        // valkoinen ja musta, ja peilaus seuraa asetusta kuten X-22:ssa.
        val look = BoardLook.of(
            BoardStyle.MONTE_CARLO_VARIANT,
            site,
            pageScheme = BoardScheme.BLUE_WHITE,
        )

        assertTrue(look.mirrored)
        assertNull(look.sitePaint(CheckerColor.YELLOW))
        assertEquals(DgBoard.MonteCarloVariant.Felt, look.felt)
        assertEquals(DgBoard.MonteCarloVariant.WedgeOdd, look.wedgeOdd)
        assertEquals(DgBoard.MonteCarloVariant.CheckerSelf, look.roleSelf.fill)
        assertEquals(DgBoard.MonteCarloVariant.CheckerOpp, look.roleOpponent.fill)
        // Lokerosarake on kotelon puuta eikä huopaa (Tommin päätös 13.9.2026), mutta
        // kehystä tummempaa, jotta kehyskaista ja ulkoreuna näkyvät (17.9.2026).
        assertEquals(DgBoard.MonteCarloVariant.TrayColumn, look.trayColumn)
        assertNotEquals(look.frame, look.trayColumn)
        assertNotEquals(look.trayColumn, look.tray)
        assertEquals(DgBoard.MonteCarloVariant.Felt, look.band)
    }

    @Test
    fun `x22 lokerosarake on huovan varinen kuten ennen ja kaista huovan varinen 14 9 2026`() {
        val look = BoardLook.x22()

        assertEquals(look.felt, look.trayColumn)
        // Keskikaista seurasi varianttia (Tommin havainto pelisessiossa 14.9.2026).
        assertEquals(look.felt, look.band)
    }

    @Test
    fun `sivustouskollinen pitaa x22 huovan ja kehyksen`() {
        // Sivusto ei sano huovasta mitään, joten SITE ei saa vaihtaa sitä.
        val look = BoardLook.of(BoardStyle.SITE, site, pageScheme = BoardScheme.CLASSIC)

        assertEquals(DgBoard.Palette.Felt, look.felt)
        assertEquals(DgBoard.Palette.Frame, look.frame)
        // Keskikaista on huopaa myös täällä 16.9.2026 alkaen (Tommin havainto Pixelillä).
        assertEquals(look.felt, look.band)
    }

    @Test
    fun `x22 ei peilaa kun asetusta ei tunneta`() {
        // Kolmiarvoisuus pätee myös tässä tyylissä: puuttuva tieto ei ole sama asia kuin
        // pois päältä oleva asetus, mutta kumpikaan ei peilaa.
        val look = BoardLook.of(
            BoardStyle.X22,
            SiteBoardSettings.UNKNOWN,
            pageScheme = null,
        )

        assertFalse(look.mirrored)
    }

    @Test
    fun `sivun oma skeema voittaa sailotyn`() {
        // Säilössä Classic, sivulla Blue/White: sivu kertoo mitkä kuvat juuri tällä
        // sivulla olivat, joten värien on tultava siitä.
        val look = BoardLook.of(BoardStyle.SITE, site, pageScheme = BoardScheme.BLUE_WHITE)

        assertEquals(
            DgBoard.SiteBlueWhite.checkerBlue,
            look.sitePaint(CheckerColor.BLUE)?.fill,
        )
        assertEquals(DgBoard.SiteBlueWhite.wedgeDark, look.wedgeEven)
    }

    @Test
    fun `sailotty skeema on varasuunta sivulle joka ei sanonut mitaan`() {
        val look = BoardLook.of(BoardStyle.SITE, site, pageScheme = null)

        assertEquals(
            DgBoard.SiteClassic.checkerYellow,
            look.sitePaint(CheckerColor.YELLOW)?.fill,
        )
        assertTrue(look.mirrored)
    }

    @Test
    fun `tuntematon skeema ei vaita sivuston vareja mutta peilaa silti`() {
        val look = BoardLook.of(
            BoardStyle.SITE,
            SiteBoardSettings(hidePips = null, homeBoardsLeft = true, scheme = null),
            pageScheme = null,
        )

        assertNull(look.sitePaint(CheckerColor.YELLOW))
        assertEquals(DgBoard.Palette.WedgeOdd, look.wedgeOdd)
        assertTrue(look.mirrored)
    }

    @Test
    fun `mini piirtyy sovelluksen vareilla koska sen vareja ei ole mitattu`() {
        val look = BoardLook.of(
            BoardStyle.SITE,
            site.copy(scheme = BoardScheme.MINI),
            pageScheme = BoardScheme.MINI,
        )

        assertNull(look.sitePaint(CheckerColor.BLUE))
    }

    @Test
    fun `peilaus vaatii asetuksen tosi eika pelkkaa tuntematonta`() {
        // Null on "sivu ei sanonut", ja silloin ei peilata: väärinpäin piirretty lauta
        // olisi pahempi väite kuin oletussuunta.
        val look = BoardLook.of(
            BoardStyle.SITE,
            site.copy(homeBoardsLeft = null),
            pageScheme = BoardScheme.CLASSIC,
        )

        assertFalse(look.mirrored)
        assertNotNull(look.sitePaint(CheckerColor.YELLOW))
    }

    @Test
    fun `paneeli seuraa tyylia vain variantissa 17 9 2026`() {
        // X-22 ja SITE pitävät entiset värit; variantti saa lokerosarakkeen puun ja kolme
        // vaaleampaa sävyä (Tommin päätös 17.9.2026, ASETUKSET luku 4).
        assertEquals(PanelLook.DEFAULT, PanelLook.of(BoardStyle.X22))
        assertEquals(PanelLook.DEFAULT, PanelLook.of(BoardStyle.SITE))
        assertEquals(DgBoard.Palette.PanelBg, PanelLook.DEFAULT.background)

        val variant = PanelLook.of(BoardStyle.MONTE_CARLO_VARIANT)
        assertEquals(DgBoard.MonteCarloVariant.TrayColumn, variant.background)
        assertEquals(BoardLook.monteCarloVariant().trayColumn, variant.background)
        assertNotEquals(PanelLook.DEFAULT.muted, variant.muted)
        assertNotEquals(PanelLook.DEFAULT.outline, variant.outline)
        assertEquals(DgBoard.Palette.CubeSoft, variant.cubeOutline)
    }
}
