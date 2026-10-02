package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.ResignOutcome
import fi.tommi.dg.domain.ResignPage
import fi.tommi.dg.domain.ResignRow
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Luovutussivun `/bg/resign` jäsennin. Rakenne on `docs/KOHDE.md`:ssä (3.9.2026).
 *
 * Ottelurivit luetaan samalla lukijalla kuin profiilin taulukot ([ProfileParser]), koska
 * sarakkeet ovat samat ja ystävyysottelun rivi on samaa muotoa (tapahtuma tekstinä ilman
 * linkkiä, viivat ajoissa). Tämän sivun oma osa on rivin valintaruutu ja lomakkeen loppu.
 */
object ResignParser {

    fun parse(html: String): ResignPage? {
        val document = Jsoup.parse(html)
        val form = document.selectFirst("form[action*=/bg/resign]") ?: return null
        val action = form.attr("action").takeIf { it.isNotBlank() } ?: return null
        val submit = form.selectFirst("input[type=submit]")?.attr("value")?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return null

        val rows = form.select("table").flatMap { table ->
            val columns = ProfileParser.headerColumns(table)
            table.select("tr").mapNotNull { row ->
                val checkbox = row.selectFirst("input[type=checkbox][name]") ?: return@mapNotNull null
                val match = ProfileParser.parseRow(row, columns) ?: return@mapNotNull null
                ResignRow(match = match, field = checkbox.attr("name"))
            }
        }

        // Varmistusruutu on taulukon ulkopuolella, eikä sen nimi ole ottelun tunnus.
        val rowFields = rows.map { it.field }.toSet()
        val confirm = form.select("input[type=checkbox][name]")
            .firstOrNull { it.attr("name") !in rowFields && it.parents().none { p -> p.normalName() == "table" } }

        return ResignPage(
            action = action,
            method = FormMethod.from(form.attr("method")),
            rows = rows,
            confirmField = confirm?.attr("name"),
            confirmLabel = confirm?.let(::labelAfter),
            submitLabel = submit,
        )
    }

    /**
     * Luovutuksen kuittaussivun oma lukema, tai null kun sivu ei ole se sivu.
     *
     * **Mitattu 4.9.2026 illalla** (`resign_done.html`, Tommin oma luovutus). Vastaus ei ole
     * luovutussivu vaan oma sivunsa, jossa ei ole lomaketta lainkaan, joten [parse] palauttaa
     * siitä nullin. Tunnistus on otsikossa (`DgPages.isResignDonePage`) samasta syystä kuin
     * kutsun kuittauksella: se on ainoa kohta joka ei riipu luovutettujen otteluista.
     *
     * **Luku luetaan lauseen alusta eikä sen sanoista.** Mitattu lause on `1 match resigned.`,
     * ja monikkomuoto on näkemättä. Sääntö on siksi se mitä yksi näyte kantaa: rivin
     * ensimmäinen kokonaisluku, kun lause puhuu luovuttamisesta. Se kestää sekä `2 matches
     * resigned.` että muun taivutuksen, eikä se keksi kumpaakaan.
     */
    fun resigned(html: String): ResignOutcome? = resigned(Jsoup.parse(html))

    fun resigned(document: Document): ResignOutcome? {
        if (!DgPages.isResignDonePage(document)) return null
        val notice = SendResultParser.notice(document) ?: return null
        if (!notice.contains("resign", ignoreCase = true)) return null
        val count = Regex("""\d+""").find(notice)?.value?.toIntOrNull() ?: return null
        return ResignOutcome(count = count, notice = notice)
    }

    /** Valintaruudun jälkeinen teksti seuraavaan elementtiin asti, sivun sanoin. */
    private fun labelAfter(input: Element): String? {
        val parts = mutableListOf<String>()
        var node = input.nextSibling()
        while (node != null) {
            if (node is Element) break
            if (node is TextNode) parts += node.text()
            node = node.nextSibling()
        }
        return parts.joinToString(" ").replace(Regex("""\s+"""), " ").trim().takeIf { it.isNotEmpty() }
    }
}
