package fi.tommi.dg.scrape

import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.moveLetter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Esipoiminta oikeilla lautasivuilla: jokaisessa fixturessa jossa kokoaminen käynnistyy,
 * asetuksella käynnistetty sessio sisältää täsmälleen pakollisten askelten monijoukon,
 * ei enempää eikä vähempää. Se on `prefillForced`in lupaus (täsmälleen tai tyhjä), ja
 * tässä se todennetaan sivuston omista sivuista eikä käsin rakennetuista asemista.
 *
 * Raportti tulostuu myös onnistuessa, jotta näkyy missä fixtureissa pakollisia ylipäätään
 * on: sama havainto kuin korpuksessa, useimmilla laudoilla ei ole yhtään.
 */
class ForcedStepsFixtureTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val boardFixtures = listOf(
        "move_after_crawford.html",
        "move_assembling.html",
        "move_bar_entry.html",
        "move_board.html",
        "move_board_arrows.html",
        "move_board_no_pips.html",
        "move_board_oikea.html",
        "move_board_vasen.html",
        "move_borne_off.html",
        "move_borne_off_full_stack.html",
        "move_crawford.html",
        "move_cube_alt_cube2.html",
        "move_cube_dr.html",
        "move_cube_offered.html",
        "move_cube_owned.html",
        "move_greedy_bearoff.html",
        "move_pending_next_game.html",
        "move_pip_tie.html",
        "move_roll_double.html",
        "move_roll_double_no_verify.html",
        "move_rollback.html",
        "move_submit_ready.html",
    )

    @Test
    fun `esipoiminta on tasmalleen pakollisten askelten monijoukko jokaisessa fixturessa`() {
        val report = StringBuilder()
        var started = 0
        var withForced = 0

        boardFixtures.forEach { name ->
            val board = BoardParser.parse(fixture(name)) ?: return@forEach
            val plain = LocalComposition.begin(board) ?: return@forEach
            started += 1
            val forced = plain.forcedSteps()
            val prefilled = checkNotNull(LocalComposition.begin(board, playForcedSteps = true))

            assertEquals(
                forced,
                prefilled.steps.groupingBy { it }.eachCount(),
                "$name: esipoiminta ei ole pakollisten askelten monijoukko",
            )
            if (forced.isNotEmpty()) {
                withForced += 1
                report.appendLine(
                    "$name: pakolliset=${forced.entries.map { (s, n) -> "${moveLetter(s.fromPoint)}${s.die}x$n" }} " +
                        "kirjaimet=${prefilled.letters} jaljella=${prefilled.remainingDice()}",
                )
            } else {
                report.appendLine("$name: ei pakollisia")
            }
        }

        assertTrue(started > 0, "Yhdessäkään fixturessa kokoaminen ei käynnistynyt")
        println("Fixtureita joissa kokoaminen kaynnistyi: $started, pakollisia askelia: $withForced\n$report")
    }
}
