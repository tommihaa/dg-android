package fi.tommi.dg.scrape

import fi.tommi.dg.domain.SiteLink
import fi.tommi.dg.domain.SiteLinksPage
import fi.tommi.dg.domain.SiteLinksSection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Sivuston linkkisivun (`/links.html`) jäsennin. Lähde `raakasivut/site_links.html`
 * (29.8.2026).
 *
 * Sivun mitattu rakenne: `h2`-otsikko ja sen perässä taulukko jonka riveillä on linkki
 * ensimmäisessä solussa ja selite toisessa. Kolme ansaa samalta sivulta:
 *
 * - **Navigointipalkki on myös taulukko**, mutta sillä ei ole edeltävää `h2`:ta, joten
 *   osastokulku ohittaa sen rakenteellisesti eikä listalla.
 * - **Sivulla on rikkinäisiä rivejä** (sisäkkäinen `<tr><tr>`, tyhjiä sulkutageja) ja
 *   kommentteihin piilotettuja vanhoja rivejä. Rivi kelpaa vain jos sen ensimmäisessä
 *   solussa on linkki; Jsoup pudottaa kommentit itse.
 * - **Viimeisellä rivillä ei ole linkkiä lainkaan** (logokuvat), ja se putoaa samalla
 *   ehdolla. Se kertoo miten sivustolle linkitetään, eikä se ole linkki minnekään.
 */
object SiteLinksParser {

    fun parse(html: String): SiteLinksPage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isSiteLinksPage(document)) return null

        val sections = document.select("h2").mapNotNull { heading ->
            val table = tableAfter(heading) ?: return@mapNotNull null
            val links = table.select("tr").mapNotNull(::parseRow)
            if (links.isEmpty()) return@mapNotNull null
            SiteLinksSection(title = heading.text().trim(), links = links)
        }

        return SiteLinksPage(sections)
    }

    /** Otsikkoa seuraava taulukko, tai null jos seuraava osasto alkaa ensin. */
    private fun tableAfter(heading: Element): Element? {
        var element = heading.nextElementSibling()
        while (element != null) {
            when (element.normalName()) {
                "table" -> return element
                "h2" -> return null
                else -> element = element.nextElementSibling()
            }
        }
        return null
    }

    private fun parseRow(row: Element): SiteLink? {
        val cells = row.tableCells()
        val link = cells.firstOrNull()?.selectFirst("a[href]") ?: return null
        val url = link.attr("href").trim().takeIf { it.isNotEmpty() } ?: return null
        val title = link.text().trim().takeIf { it.isNotEmpty() } ?: return null

        return SiteLink(
            title = title,
            url = url,
            note = cells.getOrNull(1)?.text()?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}
