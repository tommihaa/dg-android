package fi.tommi.dg.scrape

import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.SiteBoardSettings
import fi.tommi.dg.domain.siteBoardSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Koko ketju kaapatulta sivulta: jäsennys ja lautakenttien johdos yhdessä.
 *
 * Sama jako kuin asetuslähetyksellä (`SettingsUpdateTest` / `SettingsUpdateFixtureTest`)
 * ja samasta syystä: käsin rakennettu sivu ei voi paljastaa jäsentimen ja johdoksen
 * välistä eroa, koska se on kirjoitettu molempia varten. Fixturen tila on mitattu sivun
 * omista attribuuteista: kentät `6` ja `7` rastittamatta, `board`-ryhmässä `1` valittuna.
 */
class SiteBoardSettingsFixtureTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    @Test
    fun `kaapattu asetussivu antaa lautakentat`() {
        val page = SettingsParser.parse(fixture("settings_page.html"))!!

        assertEquals(
            SiteBoardSettings(
                hidePips = false,
                homeBoardsLeft = false,
                scheme = BoardScheme.BLUE_WHITE,
            ),
            page.siteBoardSettings(),
        )
    }
}
