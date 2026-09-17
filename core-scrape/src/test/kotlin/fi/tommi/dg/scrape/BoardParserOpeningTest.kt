package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.OpeningRoll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pelin ensimmäinen lautasivu, `move_opening_roll.html`: pelisession 23.8.2026 ensimmäinen
 * sivu (`0005_bg_move_..._1`), 0-0, molemmat nopat kirjautuneen pelaajan puolella.
 *
 * Todistaa että jäsentimen lukema asema täyttää [fi.tommi.dg.domain.BoardState.isStartingPosition]in
 * oikealla sivulla eikä vain rakennetulla: jos pistenumeron luku tai muurin tyhjyys
 * lipsahtaisi, avausheitto ei koskaan laukeaisi eikä mikään muu testi huomaisi sitä.
 */
class BoardParserOpeningTest {

    private fun board(name: String) = checkNotNull(
        BoardParser.parse(
            checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
                "Fixture puuttuu: $name"
            }.reader(Charsets.UTF_8).readText(),
        ),
    ) { "Lautaa ei luettu: $name" }

    @Test
    fun `ensimmainen lautasivu on lahtoasema ja avausheitto luetaan siita`() {
        val lauta = board("move_opening_roll.html")
        assertTrue(lauta.isStartingPosition())
        assertEquals(OpeningRoll(CheckerColor.YELLOW, starterValue = 5, otherValue = 1), lauta.openingRoll)
        val esitys = lauta.withOpeningDice().dice
        assertEquals(listOf(CheckerColor.YELLOW, CheckerColor.BLUE), esitys.map { it.owner })
        assertEquals(listOf(5, 1), esitys.map { it.value })
    }

    @Test
    fun `keskella pelia avausheittoa ei ole`() {
        assertNull(board("move_roll_double_no_verify.html").openingRoll)
    }
}
