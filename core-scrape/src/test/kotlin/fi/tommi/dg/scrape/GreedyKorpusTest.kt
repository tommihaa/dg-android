package fi.tommi.dg.scrape

import fi.tommi.dg.domain.BAR_POINT
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.Seat
import fi.tommi.dg.domain.Turn
import fi.tommi.dg.domain.legalTurns
import fi.tommi.dg.domain.moveLetter
import fi.tommi.dg.domain.resolveSeat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Mittaa korpuksesta (`raakasivut/`, ei versionhallinnassa) kaksi asiaa greedy bearoffista,
 * Tommin tilausta varten (2.9.2026, `docs/AVOIMET.md`): sovellus poimii greedy-siirron
 * laudalle valmiiksi kun kontaktia ei ole, ja lähetys jää pelaajalle.
 *
 * 1. **Milloin sivusto tarjoaa `Submit Greedy Bearoff` -nappia**, suhteessa kontaktiin ja
 *    kotiin. Tommin ehto on *ei kontaktia*, ja sulkulause *"voisi toimia jo aiemminkin"*
 *    väittää että sivuston ehto on tiukempi. Tämä mittaa kumpi on.
 * 2. **Mikä sääntö tuottaa sivuston esitäytön.** Sivun `move`-piilokenttä on sivuston oma
 *    greedy-siirto, ja sitä verrataan ehdokassääntöihin: eniten ulos, ja tasatilanteessa
 *    jokin toissijainen peruste. Tulos kertoo mitä sovelluksen on laskettava, jotta sen
 *    poiminta on sama kuin se johon pelaaja on tottunut.
 *
 * Ajo: `./gradlew :core-scrape:korpusTest`, raportti `build/korpus/greedy.txt`.
 */
@Tag("korpus")
class GreedyKorpusTest {

    private val corpusRoot = File("../raakasivut")

    private class Pos(val own: IntArray, val opp: IntArray, val oppBar: Int, val ownOff: Int) {
        val ownPips: Int get() = (1..BAR_POINT).sumOf { own[it] * it }
        val highestOwn: Int get() = (BAR_POINT downTo 1).firstOrNull { own[it] > 0 } ?: 0
        /** Pienin vastustajan sijainti omalla akselilla; muuri on 0. */
        val lowestOpp: Int get() = if (oppBar > 0) 0 else (1..24).firstOrNull { opp[it] > 0 } ?: 99
        val contact: Boolean get() = highestOwn > lowestOpp
        val allHome: Boolean get() = highestOwn <= 6
    }

    private fun position(board: BoardState, seat: Seat, turn: Turn?): Pos {
        val own = IntArray(BAR_POINT + 1)
        val opp = IntArray(BAR_POINT + 1)
        board.points.forEach { p ->
            val distance = if (seat.homeIsLow) p.number else 25 - p.number
            when (p.owner) {
                seat.color -> own[distance] = p.count
                null -> Unit
                else -> opp[distance] = p.count
            }
        }
        own[BAR_POINT] = board.bar.firstOrNull { it.color == seat.color }?.count ?: 0
        var oppBar = board.bar.firstOrNull { it.color != seat.color }?.count ?: 0
        var ownOff = board.borneOff.firstOrNull { it.color == seat.color }?.count ?: 0
        turn?.steps?.forEach { step ->
            val from = if (step.fromPoint == BAR_POINT) BAR_POINT else if (seat.homeIsLow) step.fromPoint else 25 - step.fromPoint
            own[from] -= 1
            val target = from - step.die
            if (target >= 1) {
                if (opp[target] == 1) {
                    opp[target] = 0
                    oppBar += 1
                }
                own[target] += 1
            } else {
                ownOff += 1
            }
        }
        return Pos(own, opp, oppBar, ownOff)
    }

    private fun letters(turn: Turn): Map<Char, Int> =
        turn.steps.map { moveLetter(it.fromPoint) }.groupingBy { it }.eachCount()

    @Test
    fun `greedy bearoff, sivuston ehto ja esitaytto`() {
        assumeTrue(corpusRoot.isDirectory, "Korpusta ei ole: ${corpusRoot.absolutePath}")
        val files = corpusRoot.walkTopDown().filter { it.isFile && it.name.endsWith(".html") }.toList()

        val out = StringBuilder()
        out.appendLine("GREEDY BEAROFF: SIVUSTON EHTO JA ESITÄYTTÖ")

        // 1. Ehto: oman vuoron laudat ennen ensimmäistä poimintaa, luokiteltuna.
        val condition = mutableMapOf<String, Int>()
        val conditionExamples = mutableMapOf<String, MutableList<String>>()
        // 2. Esitäyttö: greedy-napin laudat joilla move-kenttä on.
        data class Greedy(val file: File, val dice: List<Int>, val pending: String, val turns: List<Turn>, val before: Pos, val seat: Seat, val board: BoardState)
        val greedy = mutableListOf<Greedy>()
        val seen = mutableSetOf<String>()
        val raceRows = StringBuilder()
        val appKinds = mutableMapOf<String, Int>()

        files.forEach { file ->
            val html = file.readText(Charsets.ISO_8859_1)
            val board = BoardParser.parse(html) ?: return@forEach
            val seat = board.resolveSeat() ?: return@forEach
            val dice = board.dice.filter { it.owner == seat.color }.map { it.value }
            if (dice.size != 2) return@forEach
            if (board.undoHref != null) return@forEach
            val hasSteps = board.moves.isNotEmpty() || board.bar.any { it.color == seat.color && it.href != null }
            if (!hasSteps) return@forEach
            val key = listOf(board.matchId.value, board.stateToken, dice.sorted(), board.points).toString()
            if (!seen.add(key)) return@forEach

            val before = position(board, seat, null)
            val button = board.form?.submits?.any { it.contains("Greedy") } == true
            val turns = legalTurns(board, seat, dice)
            val canBearOff = turns.any { t -> position(board, seat, t).ownOff > before.ownOff }
            val kind = "kontakti=${before.contact} koti=${before.allHome} uloskanto_mahdollinen=$canBearOff nappi=$button"
            condition[kind] = (condition[kind] ?: 0) + 1
            conditionExamples.getOrPut(kind) { mutableListOf() }.let {
                if (it.size < 3 || (!before.contact && canBearOff && !button)) {
                    it += "${file.parentFile.name}/${file.name} napit=${board.form?.submits} kehote=\"${board.prompt}\" nopat=$dice"
                }
            }

            if (canBearOff) {
                val scoredAll = turns.map { t -> t to position(board, seat, t) }
                val best = scoredAll.maxOf { it.second.ownOff }
                val bestPositions = scoredAll.filter { it.second.ownOff == best }.map { it.second.own.joinToString(",") }.toSet()
                // Sovelluksen oma poiminta samalle laudalle: täysi vuoro kun ahne loppuasema
                // on yksikäsitteinen, muuten ahneiden yhteinen osa, tyhjä kontaktissa.
                val app = LocalComposition.begin(board, playGreedyBearoff = true)
                val appSteps = app?.steps ?: emptyList()
                val appOff = app?.let { s -> s.steps.count { st -> (if (seat.homeIsLow) st.fromPoint else 25 - st.fromPoint) - st.die < 1 } } ?: 0
                val appFinal = app?.let { s -> position(board, seat, Turn(s.steps)).own.joinToString(",") }
                val appKind = when {
                    before.contact -> if (appSteps.isEmpty()) "ok_tyhja_kontakti" else "VIRHE_poimi_kontaktissa"
                    bestPositions.size == 1 -> if (appFinal != null && appFinal in bestPositions && app.isComplete) "ok_koko_vuoro" else "VIRHE_ei_koko_vuoroa"
                    else -> if (appOff + before.ownOff <= best) "ok_yhteinen_osa(${appSteps.size} askelta)" else "VIRHE_liikaa"
                }
                appKinds[appKind.substringBefore("(")] = (appKinds[appKind.substringBefore("(")] ?: 0) + 1
                val oppHome = (1..18).all { before.opp[it] == 0 } && before.oppBar == 0
                val oppOff = board.borneOff.firstOrNull { it.color != seat.color }?.count ?: 0
                val ownOnPoints = (1..24).sumOf { before.own[it] }
                val allPositions = scoredAll.map { it.second.own.joinToString(",") }.toSet()
                raceRows.appendLine(
                    "  nappi=$button kontakti=${before.contact} ${file.parentFile.name}/${file.name} nopat=$dice koti=${before.allHome} korkein=${before.highestOwn} omia_laudalla=$ownOnPoints " +
                        "eniten_ulos=$best (${best - before.ownOff} tällä vuorolla) parhaita_asemia=${bestPositions.size} kaikkia_asemia=${allPositions.size} " +
                        "vast_kotona=$oppHome vast_ulkona=$oppOff sovellus=$appKind poimii=${appSteps.joinToString("") { s -> "${moveLetter(s.fromPoint)}${s.die}" }}",
                )
            }

            val pending = board.form?.pendingMove?.removePrefix("S")
            if (button && !pending.isNullOrEmpty()) {
                greedy += Greedy(file, dice, pending, turns, before, seat, board)
            }
        }

        out.appendLine()
        out.appendLine("1. MILLOIN NAPPI ON TARJOLLA (oma vuoro, askel tarjolla, ennen poimintaa; erillisiä asemia)")
        condition.entries.sortedByDescending { it.value }.forEach { (k, v) ->
            out.appendLine("  ${"%4d".format(v)}  $k")
            conditionExamples[k]?.forEach { out.appendLine("          esim. $it") }
        }

        out.appendLine()
        out.appendLine("1b. LAUDAT JOILLA ULOSKANTO ON MAHDOLLINEN, napin kanssa ja ilman, ja mitä sovellus poimii")
        appKinds.entries.sortedByDescending { it.value }.forEach { (k, v) -> out.appendLine("  sovellus $k: $v") }
        out.append(raceRows)

        out.appendLine()
        out.appendLine("2. ESITÄYTTÖ VASTAAN EHDOKASSÄÄNNÖT (greedy-napin laudat joilla move-kenttä)")
        out.appendLine("lautoja: ${greedy.size}")
        var inTurns = 0
        var maxOff = 0
        var uniqueMaxOff = 0
        val tiebreakHits = mutableMapOf<String, Int>()
        val tiebreakTotal = mutableMapOf<String, Int>()
        val rows = StringBuilder()
        greedy.forEach { g ->
            val pendingCounts = g.pending.groupingBy { it }.eachCount()
            val match = g.turns.filter { letters(it) == pendingCounts }
            if (match.isEmpty()) {
                rows.appendLine("  EI GENERAATTORISSA ${g.file.parentFile.name}/${g.file.name} nopat=${g.dice} greedy=${g.pending}")
                return@forEach
            }
            inTurns += 1
            val scored = g.turns.map { t -> t to position(g.board, g.seat, t) }
            val best = scored.maxOf { it.second.ownOff }
            // Kirjaimet eivät kerro noppaa, joten sama kirjainjono voi olla useampi vuoro;
            // sivuston valinnaksi luetaan niistä se joka kantaa eniten ulos.
            val siteOff = scored.filter { letters(it.first) == pendingCounts }.maxOf { it.second.ownOff }
            if (siteOff == best) {
                maxOff += 1
            } else {
                rows.appendLine(
                    "  EI ENITEN ULOS ${g.file.parentFile.name}/${g.file.name} nopat=${g.dice} greedy=${g.pending} sivusto_ulos=$siteOff paras=$best " +
                        "omat=${(1..24).filter { g.before.own[it] > 0 }.map { "$it:${g.before.own[it]}" }} " +
                        "vuorot=${scored.map { (t, p) -> t.steps.joinToString("") { s -> "${moveLetter(s.fromPoint)}${s.die}" } + "->" + p.ownOff }}",
                )
            }
            val top = scored.filter { it.second.ownOff == best }
            val topPositions = top.map { it.second.own.joinToString(",") }.toSet()
            if (topPositions.size == 1) {
                uniqueMaxOff += 1
                return@forEach
            }
            // Tasatilanne: mikä toissijainen peruste osuu sivuston valintaan?
            val sitePos = scored.filter { letters(it.first) == pendingCounts }.maxBy { it.second.ownOff }.second
            val rules = mapOf(
                "vahiten pippejä jäljellä" to top.minOf { it.second.ownPips },
                "eniten pippejä jäljellä" to top.maxOf { it.second.ownPips },
                "korkein nappula matalimmalle (min korkein)" to top.minOf { it.second.highestOwn },
                "korkein nappula pysyy (max korkein)" to top.maxOf { it.second.highestOwn },
            )
            val siteValues = mapOf(
                "vahiten pippejä jäljellä" to sitePos.ownPips,
                "eniten pippejä jäljellä" to sitePos.ownPips,
                "korkein nappula matalimmalle (min korkein)" to sitePos.highestOwn,
                "korkein nappula pysyy (max korkein)" to sitePos.highestOwn,
            )
            rules.forEach { (name, value) ->
                tiebreakTotal[name] = (tiebreakTotal[name] ?: 0) + 1
                if (siteValues.getValue(name) == value) tiebreakHits[name] = (tiebreakHits[name] ?: 0) + 1
            }
            // Isompi noppa ensin -ehdokas: sivuston kirjainjonon ensimmäinen askel
            val bigFirst = "isompi noppa ensin korkeimmalta"
            tiebreakTotal[bigFirst] = (tiebreakTotal[bigFirst] ?: 0) + 1
            val big = g.dice.max()
            val expectedFirst = top.map { it.first }.filter { it.steps.first().die == big }
                .maxByOrNull { g.seat.let { s -> if (s.homeIsLow) it.steps.first().fromPoint else 25 - it.steps.first().fromPoint } }
            if (expectedFirst != null && letters(expectedFirst) == pendingCounts) tiebreakHits[bigFirst] = (tiebreakHits[bigFirst] ?: 0) + 1

            rows.appendLine(
                "  TASATILANNE ${g.file.parentFile.name}/${g.file.name} nopat=${g.dice} greedy=${g.pending} ulos=$best " +
                    "vaihtoehdot=${top.map { (t, p) -> t.steps.map { s -> "${moveLetter(s.fromPoint)}${s.die}" }.joinToString("") + "(pip ${p.ownPips}, korkein ${p.highestOwn})" }}",
            )
        }
        out.appendLine("  esitäyttö on generaattorin vuoro: $inTurns / ${greedy.size}")
        out.appendLine("  esitäyttö kantaa ulos eniten: $maxOff / $inTurns")
        out.appendLine("  joista loppuasema yksikäsitteinen (sääntö 'eniten ulos' riittää): $uniqueMaxOff")
        out.appendLine("  tasatilanteet: ${inTurns - uniqueMaxOff}, toissijainen peruste -> osumia:")
        tiebreakTotal.forEach { (name, total) -> out.appendLine("    $name: ${tiebreakHits[name] ?: 0} / $total") }
        out.append(rows)

        val report = out.toString()
        println(report)
        File("build/korpus").mkdirs()
        File("build/korpus/greedy.txt").writeText(report)
    }
}
