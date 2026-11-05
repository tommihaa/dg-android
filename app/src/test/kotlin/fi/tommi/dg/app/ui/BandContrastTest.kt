package fi.tommi.dg.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keskikaistan tekstit kaistan omaa taustaa vasten, [PanelLook.forBand] (20.10.2026).
 *
 * Kapealla ruudulla napit ja linkit ovat kaistalla eli laudan huovalla, mutta värit tulivat
 * paneelista. Tummassa tilassa vaahterahuovalla `Skip Game` ja linkit olivat vaaleaa
 * vaalealla. Testi käy läpi jokaisen paneelin jokaisella huovalla, myös yhdistelmät joita
 * nykyinen teemalogiikka ei tuota, koska teemojen sääntö voi muuttua ja kaista ei saa
 * hiljaa jäädä lukukelvottomaksi.
 */
class BandContrastTest {

    private val bands = mapOf(
        "X-22" to DgBoard.Palette.Felt,
        "Deco" to DgBoard.Deco.Felt,
        "vaalea Deco" to DgBoard.DecoLight.Felt,
        "Monte Carlo variant" to DgBoard.MonteCarloVariant.Felt,
        "Maple" to DgBoard.Wood.Maple.felt,
        "Walnut" to DgBoard.Wood.Walnut.felt,
        "Oak leather" to DgBoard.Wood.OakLeather.felt,
        "Olive" to DgBoard.Wood.Olive.felt,
    )

    private val panels = mapOf(
        "DEFAULT" to PanelLook.DEFAULT,
        "MONTE_CARLO_VARIANT" to PanelLook.MONTE_CARLO_VARIANT,
        "DECO" to PanelLook.DECO,
        "DECO_LIGHT" to PanelLook.DECO_LIGHT,
        "PLAIN_LIGHT" to PanelLook.PLAIN_LIGHT,
        "WOOD_LIGHT" to PanelLook.WOOD_LIGHT,
        "wood(Maple)" to PanelLook.wood(DgBoard.Wood.Maple),
        "wood(Walnut)" to PanelLook.wood(DgBoard.Wood.Walnut),
    )

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    @Test
    fun kaistanTekstitErottuvatJokaisellaHuovalla() {
        val failures = mutableListOf<String>()
        for ((bandName, band) in bands) for ((panelName, panel) in panels) {
            val look = PanelLook.forBand(panel, band)
            listOf(
                "text" to look.text,
                "secondary" to look.secondary,
                "faint" to look.faint,
                "outlineButton" to look.outlineButton,
            ).forEach { (field, color) ->
                val ratio = contrast(color, band)
                if (ratio < 4.5f) failures += "$panelName / $bandName / $field: %.2f".format(ratio)
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun kelpaavaPaneeliPysyyEnnallaan() {
        // Tumma X-22 omalla mustalla huovallaan ja vaalea Deco omalla norsunluullaan ovat
        // suunniteltuja pareja, joten niihin ei kosketa.
        assertEquals(PanelLook.DEFAULT, PanelLook.forBand(PanelLook.DEFAULT, DgBoard.Palette.Felt))
        assertEquals(PanelLook.DECO_LIGHT, PanelLook.forBand(PanelLook.DECO_LIGHT, DgBoard.DecoLight.Felt))
    }
}
