package fi.tommi.dg.scrape

import org.jsoup.nodes.Element

/*
 * Taulukkorivin lukemisen yhteiset apurit. Nostettu 31.8.2026 samalla päätöksellä kuin
 * [SiteIds]; perustelu on siellä.
 */

/**
 * Rivin sarakesolut. `normalName` eikä `tagName`: [MatchLogParser] käytti ainoana
 * `tagName`ia, ja ero ei näy vain siksi että Jsoup normalisoi tagit pieniksi. Kaksi
 * kirjoitusasua oli kaksi tilaisuutta ajautua erilleen, joten muoto on nyt yksi.
 */
internal fun Element.tableCells(): List<Element> =
    children().filter { it.normalName() == "td" }

/**
 * Solun teksti siivottuna, tai null kun solua ei ole tai se on tyhjä. Indeksi saa olla
 * null ja vastaa silloin nulliin: sarakkeen nimellä hakeva kutsuja ([ProfileParser])
 * antaa löytymättömän sarakkeen sellaisenaan.
 */
internal fun List<Element>.textAt(index: Int?): String? =
    index?.let { getOrNull(it) }?.text()?.trim()?.takeIf { it.isNotEmpty() }
