package fi.tommi.dg.scrape

import fi.tommi.dg.domain.GameViewerPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Katselulaudan (`/bg/game/<id>/...`) jäsennin, raakasivuista
 * `raakasivut/katselulauta-8-11` (haettu 8.11.2026).
 *
 * Lauta luetaan [BoardParser]illa, koska aineisto on sama: otsikko `Match <id>, Move <n>`,
 * pistenumerorivit, `ALT`-koodatut pisteet, paneelit pippeineen ja pisteineen ja ottelun
 * pituus. Uutta on vain navigointi, ja se luetaan linkin tekstistä eikä paikasta.
 *
 * **Tunnistus vaatii navigoinnin ja lomakkeen puutteen.** Pelattavalla sivulla ei ole
 * `First`/`Prev`/`Next`/`Last`-riviä, ja katselusivulla ei ole `/bg/move/`-lomaketta eikä
 * siirtolinkkejä. Navigointirivin `Move`-linkki on poikkeus, ks. [isViewerPage]. Kumpikin
 * ehto erikseen kelpuuttaisi väärän sivun: navigointisanat ovat tavallisia englannin sanoja,
 * ja lomakkeen puute pätee myös siirtolistaan.
 */
object GameViewerParser {

    private val NAV_WORDS = listOf("First", "Prev", "Next", "Last")

    fun parse(html: String): GameViewerPage? {
        val document = Jsoup.parse(html)
        if (!isViewerPage(document)) return null
        val board = BoardParser.parse(html) ?: return null

        fun nav(word: String): String? =
            document.select("a[href*=/bg/game/]")
                .firstOrNull { it.text().trim() == word }
                ?.attr("href")
                ?.takeIf { it.isNotBlank() }

        return GameViewerPage(
            board = board,
            firstPath = nav("First"),
            prevPath = nav("Prev"),
            nextPath = nav("Next"),
            lastPath = nav("Last"),
            listPath = document.select("a[href*=/list]")
                .firstOrNull { it.text().contains("List of Moves", ignoreCase = true) }
                ?.attr("href"),
        )
    }

    fun isViewerPage(html: String): Boolean = isViewerPage(Jsoup.parse(html))

    fun isViewerPage(document: Document): Boolean {
        if (document.selectFirst("form[action*=/bg/move/]") != null) return false
        // Ainoa sallittu `/bg/move/`-linkki on navigointirivin `Move` (pelattava sivu). Se on
        // sivulla oman vuoron ottelun välitiloissa (laiteajo 8.11.2026: sivu ei jäsentynyt,
        // kun ehto hylkäsi kaikki siirtolinkit). Laudan siirtolinkit hylkäävät yhä.
        if (document.select("a[href*=/bg/move/]").any { it.text().trim() != "Move" }) return false
        if (!document.title().contains("Move")) return false
        // Kaikki neljä sanaa näkyvät tekstinä sivulla, linkkeinä tai ilman. Teksti luetaan
        // navigointikappaleesta, joka on lautataulukon jälkeinen irtoteksti bodyssa.
        val text = document.body().text()
        return NAV_WORDS.all { Regex("""\b$it\b""").containsMatchIn(text) } &&
            document.selectFirst("a[href*=/bg/game/]") != null
    }
}
