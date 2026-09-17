package fi.tommi.dg.scrape

import fi.tommi.dg.domain.EventCondition
import fi.tommi.dg.domain.EventEntry
import fi.tommi.dg.domain.EventPage
import fi.tommi.dg.domain.EventRound
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Turnaussivun jäsennin `/bg/event/<id>`. Sivun mitattu rakenne on `docs/KOHDE.md`:ssä
 * (27.8.2026), lähteenä `raakasivut/event_page.html`.
 *
 * **Kaavio luetaan sarakkeittain, ja se vaatii `ROWSPAN`ien laskennan.** Sivu on cup-kaavio
 * jossa etenevä pelaaja venytetään usean rivin yli, joten solun sarake **ei** ole sama kuin
 * sen järjestysnumero rivillä: myöhemmällä rivillä ensimmäinen `<TD>` voi olla kolmas
 * sarake, koska kaksi ensimmäistä ovat yhä edellisen rivin solujen peitossa. Ilman
 * laskentaa kierrokset menisivät sekaisin nimenomaan siellä missä kaavio etenee, eli
 * juuri siinä osassa jota katsotaan.
 *
 * Tyhjät solut (`&nbsp;`) ovat kaavion tulevia kierroksia tai vapaakierroksia eivätkä
 * merkintöjä, joten ne jätetään pois listoista. Niiden `ROWSPAN` luetaan silti sarakkeen
 * korkeudeksi, koska tulevalla kierroksella se on ainoa solu joka sen kertoo.
 */
object EventParser {

    /** Voittajasolun kulmasulkeet, sivun oma merkintätapa: `<nimi>`. */
    private val ANGLE_BRACKETS = Regex("""^<\s*(.*?)\s*>$""")

    fun parse(html: String): EventPage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isEventPage(document)) return null

        val table = document.select("table:has(caption)").firstOrNull {
            it.selectFirst("caption")?.text()?.contains("Brackets", ignoreCase = true) == true
        }

        val brackets = table?.let(::parseBrackets)

        return EventPage(
            name = document.selectFirst("h2")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            conditions = document.select("h4").mapNotNull { heading ->
                val text = conditionText(heading)
                if (text.isEmpty()) null else EventCondition(heading.text().trim(), text)
            },
            rounds = brackets?.rounds.orEmpty(),
            rows = brackets?.rows ?: 0,
        )
    }

    /**
     * Sääntökohdan teksti: `<h4>`-otsikon jälkeen tuleva sisältö seuraavaan otsikkoon asti.
     * Teksti on rungon suoraa sisältöä ilman säiliöelementtiä, samoin kuin palstan viestit.
     */
    private fun conditionText(heading: Element): String {
        val parts = mutableListOf<String>()
        var node = heading.nextSibling()
        while (node != null) {
            if (node is Element && node.normalName() in HEADINGS) break
            if (node is Element && node.normalName() == "hr") break
            val text = when (node) {
                is Element -> node.text()
                else -> node.toString()
            }
            parts += text
            node = node.nextSibling()
        }
        return parts.joinToString(" ").replace(WHITESPACE, " ").trim()
    }

    private class Brackets(val rounds: List<EventRound>, val rows: Int)

    private fun parseBrackets(table: Element): Brackets? {
        val titles = table.select("tr")
            .firstOrNull { row -> row.children().any { it.normalName() == "th" } }
            ?.children()
            ?.filter { it.normalName() == "th" }
            ?.map { it.text().trim() }
            ?: return null

        val columns = List(titles.size) { mutableListOf<EventEntry>() }
        // Sarakkeen solukorkeus sivun omasta ROWSPANista, ensimmäisestä solusta luettuna.
        val spans = IntArray(titles.size)
        // Montako riviä kukin sarake on vielä edellisen solunsa peitossa.
        val covered = IntArray(titles.size)
        var rows = 0

        table.select("tr").forEach { row ->
            val cells = row.tableCells()
            if (cells.isEmpty()) return@forEach
            val top = rows
            rows++
            var column = 0
            cells.forEach { cell ->
                while (column < titles.size && covered[column] > 0) column++
                if (column >= titles.size) return@forEach

                val span = cell.attr("rowspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                if (spans[column] == 0) spans[column] = span
                parseEntry(cell)?.let { columns[column] += it.copy(top = top) }
                covered[column] = span
                column++
            }
            for (index in covered.indices) if (covered[index] > 0) covered[index]--
        }

        return Brackets(
            rounds = titles.mapIndexed { index, title ->
                EventRound(title, columns[index], spans[index].coerceAtLeast(1))
            },
            rows = rows,
        )
    }

    private fun parseEntry(cell: Element): EventEntry? {
        val raw = cell.text().replace(' ', ' ').trim()
        if (raw.isEmpty()) return null

        val link = cell.selectFirst("a[href]")
        val href = link?.attr("href")?.takeIf { it.isNotBlank() }
        val label = ANGLE_BRACKETS.find(raw)?.groupValues?.get(1) ?: raw

        return EventEntry(
            label = label,
            profilePath = href?.takeIf { it.contains("/bg/user/") },
            matchPath = href?.takeIf { it.contains("/bg/game/") },
            inProgress = raw.equals("In Progress", ignoreCase = true),
            // Lihavointi ja kulmasulkeet ovat sivun tapa merkitä ratkennut ottelu.
            decided = cell.selectFirst("b") != null,
        )
    }

    private val HEADINGS = setOf("h1", "h2", "h3", "h4")
    private val WHITESPACE = Regex("""\s+""")
}
