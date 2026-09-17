package fi.tommi.dg.scrape

import fi.tommi.dg.domain.BAR_POINT
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.legalTurns
import fi.tommi.dg.domain.resolveSeat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Oraakkeli: sivun omat linkit vastaan siirtogeneraattorin ensimmainen askel.
 *
 * Jokainen lautafixture kantaa aseman, nopat ja sivuston itsensa laskemat ensimmaisen
 * askeleen linkit, eli sivuston oman saantomoottorin tuloksen. Testi vaatii etta
 * generaattori paatyy samaan joukkoon jokaisessa fixturessa jossa paikallinen kokoaminen
 * ylipaataan kaynnistyy. Ks. docs/ARKKITEHTUURI.md, paikallinen kokoaminen.
 *
 * Fixture jossa kokoaminen ei kaynnisty (ei omia noppia, ei linkkeja, istuin ei ratkea)
 * ei ole virhe: se on vanhan reitin lauta. Testi raportoi silti mitka putosivat, jotta
 * pudonneiden joukko on tietoinen eika hiljainen.
 */
class LocalCompositionOracleTest {

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
    fun `generaattorin ensimmainen askel tasmaa sivun linkkeihin jokaisessa fixturessa`() {
        val report = StringBuilder()
        var candidates = 0
        var started = 0

        boardFixtures.forEach { name ->
            val board = BoardParser.parse(fixture(name)) ?: run {
                report.appendLine("$name: ei jasenny laudaksi")
                return@forEach
            }
            val seat = board.resolveSeat()
            val ownDice = seat?.let { s -> board.dice.filter { it.owner == s.color }.map { it.value } }
            val hasSteps = board.moves.isNotEmpty() ||
                board.bar.any { b -> seat != null && b.color == seat.color && b.href != null }

            if (seat == null || ownDice == null || ownDice.size != 2 || !hasSteps || board.undoHref != null) {
                report.appendLine(
                    "$name: vanha reitti (seat=$seat, dice=$ownDice, linkkeja=${board.moves.size}, " +
                        "undo=${board.undoHref != null})",
                )
                return@forEach
            }

            candidates += 1
            val session = LocalComposition.begin(board)
            if (session == null) {
                val pageFroms = buildSet {
                    board.moves.forEach { add(it.fromPoint) }
                    if (board.bar.any { it.color == seat.color && it.href != null }) add(BAR_POINT)
                }
                val engineFroms = legalTurns(board, seat, ownDice)
                    .flatMap { turn -> turn.steps.map { it.fromPoint } }
                    .toSet()
                report.appendLine(
                    "$name: ORAAKKELI ERI MIELTA sivu=$pageFroms moottori=$engineFroms " +
                        "seat=$seat dice=$ownDice",
                )
            } else {
                started += 1
                report.appendLine("$name: ok, lahdot=${session.legalFromPoints()}")
            }
        }

        assertTrue(
            candidates == started,
            "Oraakkeli oli eri mielta $candidates - $started fixturessa:\n$report",
        )
        // Raportti nakyy myos onnistuessa, jotta pudonneiden joukko on tietoinen.
        println(report)
    }
}
