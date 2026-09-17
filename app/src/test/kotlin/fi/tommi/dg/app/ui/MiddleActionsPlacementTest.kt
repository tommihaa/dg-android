package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nappirivin sijoittelu keskikaistalla, kun noppalokerolle varataan tila.
 *
 * Tämä vahtii kahta jo kertaalleen osunutta vikaa yhtä aikaa. Iso järjestelmäfontti ajoi
 * rivin noppien päälle (mitattu 26.8.2026, `Reminders` fontScale 1.3:lla ja `Cube reminder`
 * 2.0:lla), ja sitä ennen pelkkä kohdistus jäljelle jäävään tilaan vei napit sivuun myös
 * silloin kun ne mahtuivat laudan keskelle. Korjaus toiseen ei saa palauttaa toista.
 *
 * Luvut ovat pikseleitä, kuten `Layout`issa.
 */
class MiddleActionsPlacementTest {

    @Test
    fun `mahtuva rivi on laudan keskella eika jaljelle jaavan tilan keskella`() {
        // Nopat vasemmalla, 200 px varattu. Keskikohta on 400, ei 500.
        assertEquals(400, middleActionsX(width = 1000, rowWidth = 200, start = 200, end = 0))
        // Sama oikealla päin.
        assertEquals(400, middleActionsX(width = 1000, rowWidth = 200, start = 0, end = 200))
    }

    @Test
    fun `liian leveä rivi vaistaa noppia eika piirry niiden paalle`() {
        // Vasen lokero varattu: rivi alkaa varauksen jälkeen.
        assertEquals(200, middleActionsX(width = 1000, rowWidth = 700, start = 200, end = 0))
        // Oikea lokero varattu: rivi loppuu varaukseen, eli 1000 - 200 - 700.
        assertEquals(100, middleActionsX(width = 1000, rowWidth = 700, start = 0, end = 200))
    }

    @Test
    fun `tayden levyinen rivi ei ylita kumpaakaan varausta`() {
        // Rivi mitataan varausten jälkeen jäävällä leveydellä, joten tämä on sen raja.
        assertEquals(200, middleActionsX(width = 1000, rowWidth = 800, start = 200, end = 0))
    }

    @Test
    fun `varaukseton kaista keskittaa kuten ennenkin`() {
        assertEquals(400, middleActionsX(width = 1000, rowWidth = 200, start = 0, end = 0))
    }
}
