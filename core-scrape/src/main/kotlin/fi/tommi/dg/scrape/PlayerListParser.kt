package fi.tommi.dg.scrape

import fi.tommi.dg.domain.PlayerList
import fi.tommi.dg.domain.PlayerListLink
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.PlayerRow
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Pelaajalistan jäsennin `/bg/plist`. Sivun mitattu rakenne on `docs/KOHDE.md`:ssä
 * (27.8.2026), lähteenä `raakasivut/player_list.html`.
 *
 * Sama doktriini kuin [LoungeParser]illa, ja tällä sivulla se on tavallistakin
 * tarpeellisempi: **rivillä on seitsemän solua joista kolme on pelkkiä välistyksiä**
 * (`<TD width=30>`, kaksi `<TD width=10>`). Solut luetaan siksi pelaajalinkin suhteen,
 * ei rivin alusta.
 *
 * Sivulla ei ole `<H1>`-otsikkoa lainkaan, joten sivun laji tunnistetaan riveistä
 * (`DgPages.isPlayerList`).
 */
object PlayerListParser {

    /** Sijoitussolu, esim. "1.". Piste on sivun oma eikä kuulu lukuun. */
    private val RANK = Regex("""^(\d+)\.?$""")

    /** Pelaajarivi: rivi jolla on profiililinkki. Otsikkorivillä ei ole. */
    const val PLAYER_ROW = "tr:has(a[href~=^/bg/user/\\d+])"

    fun parse(html: String): PlayerList? {
        val document = Jsoup.parse(html)
        if (!DgPages.isPlayerList(document)) return null

        return PlayerList(
            players = document.select(PLAYER_ROW).mapNotNull(::parseRow),
            // Sivun omat linkit sivun omassa järjestyksessä. Navigointipalkissa ei ole
            // yhtään /bg/plist-linkkiä, joten rajaus riittää sellaisenaan.
            links = document.select("a[href^=/bg/plist]").mapNotNull { link ->
                val label = link.text().trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val path = link.attr("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                PlayerListLink(label = label, path = path)
            },
        )
    }

    private fun parseRow(row: Element): PlayerRow? {
        val cells = row.tableCells()
        val playerIndex = cells.indexOfFirst { it.selectFirst("a[href*=/bg/user/]") != null }
        if (playerIndex < 0) return null

        val playerLink = cells[playerIndex].selectFirst("a[href*=/bg/user/]") ?: return null
        val name = playerLink.text().trim().takeIf { it.isNotEmpty() } ?: return null

        return PlayerRow(
            // Sijoitus on pelaajalinkkiä edeltävä solu jonka teksti on luku ja piste.
            // Välistyssolut ovat tyhjiä, joten ne eivät voi osua tähän.
            rank = cells.take(playerIndex)
                .asReversed()
                .firstNotNullOfOrNull { RANK.find(it.text().trim())?.groupValues?.get(1) }
                ?.toIntOrNull(),
            player = PlayerRef(
                name = name,
                userId = SiteIds.userId(playerLink.attr("href")),
                profilePath = playerLink.attr("href").takeIf { it.isNotBlank() },
            ),
            // Rating on heti pelaajasolun jälkeen, kokemus rivin viimeinen ei-tyhjä solu.
            ratingText = cells.getOrNull(playerIndex + 1)?.text()?.trim()?.takeIf { it.isNotEmpty() },
            experienceText = cells.asReversed()
                .firstOrNull { it.text().trim().isNotEmpty() }
                ?.takeIf { cells.indexOf(it) > playerIndex + 1 }
                ?.text()
                ?.trim(),
        )
    }
}
