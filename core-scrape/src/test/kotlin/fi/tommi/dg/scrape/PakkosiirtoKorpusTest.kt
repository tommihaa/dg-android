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
 * Pakkosiirtojen korpusmittaus (`docs/AVOIMET.md`, pakkosiirtojen automaattinen pelaaminen).
 *
 * Mittaa kaapatusta korpuksesta (`raakasivut/`, ei versionhallinnassa) sen kohdan joka
 * vaatimuksessa on nimetty mittaamattomaksi: pakkosiirto ei ole `legalTurns(...).size == 1`
 * vaan *kaikki lailliset vuorot päätyvät samaan asemaan*. Testi laskee molemmat ja kertoo
 * kuinka paljon ne eroavat, sekä missä tilanteissa (muurilta tulo, tupla, uloskanto).
 *
 * **Pelatut siirrot ovat oraakkeli.** Proxyn tiedostonimi kantaa lähetetyt kirjaimet
 * (`..._move_hf_submit_Submit_Move.html`), joten samasta istuntokansiosta luetaan mitä
 * pelaaja oikeasti pelasi. Pakkosiirtolaudalla pelatun on oltava jokin generaattorin
 * vuoroista; jos ei ole, tunnistus on väärä ja se on tämän mittauksen tärkein tulos.
 *
 * Ajetaan erikseen (`gradlew :core-scrape:korpusTest`), koska korpus on koneella eikä
 * repossa. Ilman korpusta testi ohitetaan eikä kaadu. Raportti kirjoitetaan
 * `build/korpus/pakkosiirrot.txt`.
 *
 * **Ensimmäinen ajo 2.9.2026, tulokset `docs/AVOIMET.md`:ssä.** Pakkosiirtoja oli nolla 478
 * asemasta, koska sivusto ei näytä pakkosiirtoa valintasivuna vaan omana `Submit Forced
 * Move` -sivunaan; 441 pelattua siirtoa 441:stä oli generaattorin vuorojen joukossa. Sama
 * ajo laski myös pakolliset askeleet (askel joka on jokaisessa laillisessa vuorossa):
 * 88 asemassa 478:sta on vähintään yksi, ja pelattu siirto sisälsi ne 79/79 kertaa. Testi
 * ei väitä tulosta assertiolla, koska korpus kasvaa jokaisesta pelisessiosta ja luvut
 * muuttuvat sen mukana; se on mittari eikä regressiotesti.
 */
@Tag("korpus")
class PakkosiirtoKorpusTest {

    private val corpusRoot = File("../raakasivut")

    /**
     * Lähetetyn siirron kirjaimet vastaussivun nimestä. Nimen tunniste on sen laudan jolta
     * siirto lähti, ja se liitetään laudan omaan `stateToken`iin alempana.
     */
    private val submitName = Regex("""_bg_move_(\d+)_(\d+)_move_([A-Za-z]+)_submit_Submit_Move\.html$""")

    private data class Candidate(
        val file: File,
        val board: BoardState,
        val seat: Seat,
        val dice: List<Int>,
        val turns: List<Turn>,
        val positions: Set<String>,
        val oracleAgrees: Boolean,
        val played: List<String>,
    ) {
        val forced: Boolean get() = turns.isNotEmpty() && positions.size == 1
        val singleTurn: Boolean get() = turns.size == 1
        val isDouble: Boolean get() = dice.distinct().size == 1
        val onBar: Boolean get() = board.bar.any { it.color == seat.color && it.count > 0 }
        val bearingOff: Boolean get() = turns.any { t -> t.steps.any { stepBearsOff(board, seat, t, it) } }

        private fun stepBearsOff(board: BoardState, seat: Seat, turn: Turn, step: fi.tommi.dg.domain.MoveStep): Boolean {
            val from = if (step.fromPoint == BAR_POINT) BAR_POINT else if (seat.homeIsLow) step.fromPoint else 25 - step.fromPoint
            return from - step.die < 1
        }

        /**
         * Askeleet jotka ovat mukana **jokaisessa** laillisessa vuorossa, monijoukon
         * leikkauksena (sama askel kahdesti vain jos se on kahdesti jokaisessa vuorossa).
         * Tämä on Tommin lauseen *"jos joku siirto on pakollinen, niin se pelataan"* toinen
         * lukutapa: siirto on askel eikä vuoro, ja pakollinen askel pelataan vaikka loppu
         * vuorosta jää valinnaksi.
         */
        val forcedSteps: Map<fi.tommi.dg.domain.MoveStep, Int>
            get() {
                if (turns.isEmpty()) return emptyMap()
                return turns.map { it.multiset }.reduce { acc, m ->
                    acc.filterKeys { it in m }.mapValues { (k, v) -> minOf(v, m.getValue(k)) }
                }
            }
        val forcedStepCount: Int get() = forcedSteps.values.sum()

        /** Pelattu kirjainjono täsmää johonkin vuoroon monijoukkona; null kun pelattua ei löydy. */
        val playedMatches: Boolean?
            get() {
                if (played.isEmpty()) return null
                return played.any { letters ->
                    val counts = letters.groupingBy { it }.eachCount()
                    turns.any { turn -> turn.steps.map { moveLetter(it.fromPoint) }.groupingBy { it }.eachCount() == counts }
                }
            }
    }

    @Test
    fun `pakkosiirto on sama loppuasema eika yksi vuoro`() {
        assumeTrue(corpusRoot.isDirectory, "Korpusta ei ole: ${corpusRoot.absolutePath}")

        val files = corpusRoot.walkTopDown().filter { it.isFile && it.name.endsWith(".html") }.toList()
        val playedByBoard = mutableMapOf<String, MutableList<String>>()
        files.forEach { file ->
            submitName.find(file.name)?.let { m ->
                val letters = m.groupValues[3].removePrefix("S")
                playedByBoard.getOrPut("${file.parentFile.name}/${m.groupValues[1]}/${m.groupValues[2]}") { mutableListOf() }.add(letters)
            }
        }

        var boards = 0
        var ownTurnNoSteps = 0
        val candidates = mutableListOf<Candidate>()
        val seen = mutableSetOf<String>()
        var duplicates = 0
        /** Omien noppien sivut lajeittain: kehote, undo, askel tarjolla, lomakkeen napit. */
        val ownDiceKinds = mutableMapOf<String, Int>()
        val siteComposed = mutableListOf<String>()

        files.forEach { file ->
            val html = file.readText(Charsets.ISO_8859_1)
            val board = BoardParser.parse(html) ?: return@forEach
            boards += 1
            val seatForKinds = board.resolveSeat()
            if (seatForKinds != null && board.dice.count { it.owner == seatForKinds.color } == 2) {
                val steps = board.moves.isNotEmpty() || board.bar.any { it.color == seatForKinds.color && it.href != null }
                val kind = "kehote=\"${board.prompt}\" undo=${board.undoHref != null} askel=$steps napit=${board.form?.submits}"
                ownDiceKinds[kind] = (ownDiceKinds[kind] ?: 0) + 1
                val submits = board.form?.submits ?: emptyList()
                if (submits.any { it.contains("Forced") || it.contains("Greedy") }) {
                    siteComposed += "${rel(file)} napit=$submits undo=${board.undoHref != null} linkkeja=${board.moves.size} nopat=${board.dice.map { it.value }}"
                }
            }
            if (board.undoHref != null) return@forEach
            val seat = seatForKinds ?: return@forEach
            val dice = board.dice.filter { it.owner == seat.color }.map { it.value }
            if (dice.size != 2) return@forEach
            val hasSteps = board.moves.isNotEmpty() || board.bar.any { it.color == seat.color && it.href != null }
            if (!hasSteps) {
                ownTurnNoSteps += 1
                return@forEach
            }

            val key = listOf(board.matchId.value, board.stateToken, dice.sorted(), board.points, board.bar.map { it.color to it.count }).toString()
            if (!seen.add(key)) {
                duplicates += 1
                return@forEach
            }

            val turns = legalTurns(board, seat, dice)
            val positions = turns.map { finalPosition(board, seat, it) }.toSet()
            // Liitos laudan OMALLA tilatunnisteella (linkeistä luettu), ei tiedostonimen:
            // vastaussivun nimi kantaa sen laudan tunnisteen josta siirto tehtiin, mutta sivun
            // sisältö on jo seuraava lauta. Tiedostonimen tunniste osoittaisi siis edelliseen.
            val played = board.stateToken?.let { token ->
                playedByBoard["${file.parentFile.name}/${board.matchId.value}/$token"]
            } ?: emptyList()
            candidates += Candidate(
                file, board, seat, dice, turns, positions,
                oracleAgrees = LocalComposition.begin(board) != null,
                played = played.distinct(),
            )
        }

        val forced = candidates.filter { it.forced }
        val single = candidates.filter { it.singleTurn }
        val forcedMany = forced.filter { !it.singleTurn }
        val withPlayed = candidates.filter { it.played.isNotEmpty() }
        val playedOk = withPlayed.count { it.playedMatches == true }
        val forcedWithPlayed = forced.filter { it.played.isNotEmpty() }
        val forcedPlayedOk = forcedWithPlayed.count { it.playedMatches == true }

        val out = StringBuilder()
        out.appendLine("PAKKOSIIRTOJEN KORPUSMITTAUS")
        out.appendLine("html-tiedostoja: ${files.size}")
        out.appendLine("lautasivuja (BoardParser jäsensi): $boards")
        out.appendLine("oma vuoro, nopat heitetty, ei omaa askelta tarjolla (ei laillista siirtoa tai muu tila): $ownTurnNoSteps")
        out.appendLine("oma vuoro ja askel tarjolla, ennen ensimmäistä poimintaa: ${candidates.size + duplicates}, joista saman aseman toisintoja $duplicates")
        out.appendLine("erillisiä asemia (mittausjoukko): ${candidates.size}")
        out.appendLine()
        out.appendLine("PAKKOSIIRTO = kaikki lailliset vuorot päätyvät samaan asemaan")
        out.appendLine("pakkosiirtoja: ${forced.size} (${pct(forced.size, candidates.size)} asemista)")
        out.appendLine("  joista legalTurns.size == 1: ${forced.count { it.singleTurn }}")
        out.appendLine("  joista useampi vuoro samaan asemaan: ${forcedMany.size}")
        out.appendLine("legalTurns.size == 1 kaikkiaan: ${single.size} (kaikki näistä pakkosiirtoja: ${single.all { it.forced }})")
        out.appendLine()
        out.appendLine("PAKKOSIIRRON TILANTEET (asema voi kuulua useaan)")
        out.appendLine("  tupla: ${forced.count { it.isDouble }} / tuplia mittausjoukossa ${candidates.count { it.isDouble }}")
        out.appendLine("  oma nappula muurilla: ${forced.count { it.onBar }} / muurilla mittausjoukossa ${candidates.count { it.onBar }}")
        out.appendLine("  uloskanto jossakin vuorossa: ${forced.count { it.bearingOff }} / uloskantoa mittausjoukossa ${candidates.count { it.bearingOff }}")
        out.appendLine("  ei mikään kolmesta: ${forced.count { !it.isDouble && !it.onBar && !it.bearingOff }}")
        out.appendLine("  oraakkeli hyväksyy (LocalComposition.begin != null): ${forced.count { it.oracleAgrees }} / ${forced.size}")
        out.appendLine()
        out.appendLine("VUOROJEN MÄÄRÄ PAKKOSIIRROISSA (legalTurns.size -> asemia)")
        forced.groupingBy { it.turns.size }.eachCount().toSortedMap().forEach { (n, c) -> out.appendLine("  $n -> $c") }
        out.appendLine()
        out.appendLine("OMIEN NOPPIEN SIVUT LAJEITTAIN (kehote, undo, askel tarjolla, lomakkeen napit -> sivuja)")
        ownDiceKinds.entries.sortedByDescending { it.value }.forEach { (k, v) -> out.appendLine("  $v  $k") }
        out.appendLine()
        out.appendLine("SIVUSTON ITSE KOKOAMAT (Forced / Greedy -nappi)")
        out.appendLine("sivuja: ${siteComposed.size}")
        siteComposed.forEach { out.appendLine("  $it") }
        out.appendLine()
        out.appendLine("ERI LOPPUASEMIEN MÄÄRÄ (positions.size -> asemia), eli kuinka lähellä pakkoa valinta on")
        fun histogram(label: String, subset: List<Candidate>) {
            out.appendLine("  $label (${subset.size} asemaa):")
            subset.groupingBy { it.positions.size }.eachCount().toSortedMap().forEach { (n, c) -> out.appendLine("    $n loppuasemaa -> $c") }
        }
        histogram("kaikki", candidates)
        histogram("tupla", candidates.filter { it.isDouble })
        histogram("oma nappula muurilla", candidates.filter { it.onBar })
        histogram("uloskanto jossakin vuorossa", candidates.filter { it.bearingOff })
        histogram("Greedy-nappi tarjolla", candidates.filter { it.board.form?.submits?.any { s -> s.contains("Greedy") } == true })
        out.appendLine()
        out.appendLine("PAKOLLINEN ASKEL = askel joka on jokaisessa laillisessa vuorossa (siirto on askel, ei vuoro)")
        val withForcedStep = candidates.filter { it.forcedStepCount > 0 }
        out.appendLine("asemia joissa vähintään yksi pakollinen askel: ${withForcedStep.size} / ${candidates.size} (${pct(withForcedStep.size, candidates.size)})")
        out.appendLine("  pakollisten askelten määrä -> asemia:")
        candidates.groupingBy { it.forcedStepCount }.eachCount().toSortedMap().forEach { (n, c) -> out.appendLine("    $n -> $c") }
        fun forcedBy(label: String, subset: List<Candidate>) {
            val f = subset.filter { it.forcedStepCount > 0 }
            out.appendLine("  $label: ${f.size} / ${subset.size} asemassa pakollinen askel; askelia yhteensä ${f.sumOf { it.forcedStepCount }} / vuoron askelia ${subset.sumOf { c -> c.turns.firstOrNull()?.steps?.size ?: 0 }}")
        }
        forcedBy("tupla", candidates.filter { it.isDouble })
        forcedBy("oma nappula muurilla", candidates.filter { it.onBar })
        forcedBy("uloskanto jossakin vuorossa", candidates.filter { it.bearingOff })
        forcedBy("ei mikään kolmesta", candidates.filter { !it.isDouble && !it.onBar && !it.bearingOff })
        out.appendLine("  esipoiminta (LocalComposition.begin(board, playForcedSteps = true)) tavoittaa pakolliset täsmälleen: " +
            withForcedStep.count { c ->
                LocalComposition.begin(c.board, playForcedSteps = true)?.steps?.groupingBy { it }?.eachCount() == c.forcedSteps
            } + " / ${withForcedStep.size}")
        out.appendLine("  pakollinen askel on muurilta tulo: ${withForcedStep.count { c -> c.forcedSteps.keys.any { it.fromPoint == BAR_POINT } }}")
        out.appendLine("  pakollinen askel on uloskanto: ${withForcedStep.count { c -> c.forcedSteps.keys.any { s -> (if (c.seat.homeIsLow) s.fromPoint else 25 - s.fromPoint) - s.die < 1 } }}")
        out.appendLine("  pelattu siirto sisältää kaikki pakolliset askeleet (kirjaimina): " +
            withForcedStep.filter { it.played.isNotEmpty() }.let { ps ->
                val ok = ps.count { c ->
                    val need = c.forcedSteps.entries.groupingBy { moveLetter(it.key.fromPoint) }.fold(0) { acc, e -> acc + e.value }
                    c.played.any { letters -> need.all { (ch, n) -> letters.count { it == ch } >= n } }
                }
                "$ok / ${ps.size}"
            })
        out.appendLine("  istunnoittain (kansio: asemia joissa pakollinen / asemia; pelattu sisälsi pakolliset / pelattu löytyy):")
        candidates.groupBy { it.file.parentFile.name }.toSortedMap().forEach { (dir, cs) ->
            val f = cs.filter { it.forcedStepCount > 0 }
            val ps = f.filter { it.played.isNotEmpty() }
            val ok = ps.count { c ->
                val need = c.forcedSteps.entries.groupingBy { moveLetter(it.key.fromPoint) }.fold(0) { acc, e -> acc + e.value }
                c.played.any { letters -> need.all { (ch, n) -> letters.count { it == ch } >= n } }
            }
            out.appendLine("    $dir: ${f.size} / ${cs.size}; $ok / ${ps.size}")
        }
        out.appendLine("  kaikki asemat joissa pakollinen askel:")
        withForcedStep.forEach {
            out.appendLine("    ${rel(it.file)} nopat=${it.dice} pakolliset=${it.forcedSteps.entries.map { (s, n) -> "${moveLetter(s.fromPoint)}${s.die}x$n" }} vuoroja=${it.turns.size} loppuasemia=${it.positions.size} pelattu=${it.played}")
        }
        out.appendLine()
        out.appendLine("GREEDY-NAPIN ESITÄYTETTY SIIRTO VASTAAN GENERAATTORI")
        candidates.filter { it.board.form?.submits?.any { s -> s.contains("Greedy") } == true }.forEach {
            val pending = it.board.form?.pendingMove?.removePrefix("S")
            val counts = pending?.groupingBy { c -> c }?.eachCount()
            val known = it.turns.any { t -> t.steps.map { s -> moveLetter(s.fromPoint) }.groupingBy { c -> c }.eachCount() == counts }
            out.appendLine("  ${rel(it.file)} nopat=${it.dice} greedy=$pending generaattorissa=$known loppuasemia=${it.positions.size} pelattu=${it.played}")
        }
        out.appendLine()
        out.appendLine("PELATTU SIIRTO ORAAKKELINA (saman istuntokansion Submit Move -tiedostonimi)")
        out.appendLine("asemia joille pelattu siirto löytyy: ${withPlayed.size}, pelattu on jokin generaattorin vuoro: $playedOk")
        out.appendLine("pakkosiirtoja joille pelattu löytyy: ${forcedWithPlayed.size}, pelattu on jokin generaattorin vuoro: $forcedPlayedOk")
        withPlayed.filter { it.playedMatches == false }.forEach {
            out.appendLine("  RISTIRIITA ${rel(it.file)}: pelattu=${it.played} nopat=${it.dice} vuorot=${it.turns.map { t -> t.steps.map { s -> "${moveLetter(s.fromPoint)}${s.die}" } }}")
        }
        out.appendLine()
        out.appendLine("PAKKOSIIRROT JOISSA USEAMPI VUORO (tarkistettavaksi silmällä)")
        forcedMany.forEach {
            out.appendLine("  ${rel(it.file)} nopat=${it.dice} muuri=${it.onBar} uloskanto=${it.bearingOff} pelattu=${it.played} vuorot=${it.turns.map { t -> t.steps.map { s -> "${moveLetter(s.fromPoint)}${s.die}" } }}")
        }
        out.appendLine()
        out.appendLine("PAKKOSIIRROT JOISSA YKSI VUORO")
        forced.filter { it.singleTurn }.forEach {
            out.appendLine("  ${rel(it.file)} nopat=${it.dice} muuri=${it.onBar} uloskanto=${it.bearingOff} pelattu=${it.played} vuoro=${it.turns.single().steps.map { s -> "${moveLetter(s.fromPoint)}${s.die}" }}")
        }

        val report = out.toString()
        println(report)
        File("build/korpus").mkdirs()
        File("build/korpus/pakkosiirrot.txt").writeText(report)
    }

    private fun rel(file: File): String = "${file.parentFile.name}/${file.name}"

    private fun pct(a: Int, b: Int): String = if (b == 0) "-" else "%.1f %%".format(100.0 * a / b)

    /**
     * Loppuasema vuoron jälkeen omina etäisyyksinä, merkkijonona vertailua varten.
     *
     * Toistaa `EnginePosition.apply`n säännön (syöty blotti muurille, alle yhden jäävä
     * etäisyys on uloskanto), koska se on `internal` eikä näy tähän moduuliin. Tämä on
     * mittarin oma kopio eikä tuotantokoodia.
     */
    private fun finalPosition(board: BoardState, seat: Seat, turn: Turn): String {
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
        turn.steps.forEach { step ->
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
        return own.joinToString(",") + "|" + opp.joinToString(",") + "|" + oppBar + "|" + ownOff
    }
}
