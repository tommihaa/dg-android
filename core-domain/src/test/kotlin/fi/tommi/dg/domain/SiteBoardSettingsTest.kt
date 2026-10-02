package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Lautaan vaikuttavien asetusten johdos asetussivusta.
 *
 * Kaksi väitettä joita nämä vartioivat: tunnistus tehdään selitteestä eikä numerosta
 * (renumeroitu sivu ei osu väärään asetukseen), ja puuttuva tieto on null eikä false
 * (lauta ei saa väittää asetusta kumpaankaan suuntaan sivusta joka ei sanonut mitään).
 */
class SiteBoardSettingsTest {

    private fun page(
        toggles: List<PreferenceToggle>,
        radios: Map<String, String> = mapOf("board" to "1"),
    ) = SettingsPage(
        player = PlayerRef(name = "pelaaja"),
        method = FormMethod.POST,
        toggles = toggles,
        radios = radios,
        radioGroupNames = radios.keys.toList(),
    )

    @Test
    fun `kolme kenttaa luetaan selitteista ja ryhman nimesta`() {
        val page = page(
            toggles = listOf(
                PreferenceToggle("6", "Hide pip counts", checked = true),
                PreferenceToggle("7", "Home boards on left side", checked = false),
            ),
        )

        assertEquals(
            SiteBoardSettings(
                hidePips = true,
                homeBoardsLeft = false,
                scheme = BoardScheme.BLUE_WHITE,
            ),
            page.siteBoardSettings(),
        )
    }

    @Test
    fun `selite ratkaisee eika numero, joten renumeroitu sivu luetaan oikein`() {
        // Sama selite eri numerolla. Numeroon sidottu tulkinta lukisi tässä kentän "6"
        // (jonka selite on nyt jotain muuta) ja osuisi väärään asetukseen hiljaa.
        val page = page(
            toggles = listOf(
                PreferenceToggle("6", "Something new entirely", checked = true),
                PreferenceToggle("9", "Hide pip counts", checked = false),
            ),
        )

        assertEquals(false, page.siteBoardSettings().hidePips)
    }

    @Test
    fun `puuttuva selite on null eika false`() {
        val page = page(toggles = emptyList(), radios = emptyMap())

        val settings = page.siteBoardSettings()

        assertNull(settings.hidePips)
        assertNull(settings.homeBoardsLeft)
        assertNull(settings.scheme)
    }

    @Test
    fun `skeeman arvot vastaavat lomakkeen numeroita ja tuntematon on null`() {
        val toggles = emptyList<PreferenceToggle>()

        assertEquals(
            BoardScheme.CLASSIC,
            page(toggles, mapOf("board" to "0")).siteBoardSettings().scheme,
        )
        assertEquals(
            BoardScheme.MINI,
            page(toggles, mapOf("board" to "2")).siteBoardSettings().scheme,
        )
        assertNull(page(toggles, mapOf("board" to "3")).siteBoardSettings().scheme)
    }
}
