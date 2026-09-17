package fi.tommi.dg.scrape

import fi.tommi.dg.domain.MatchLogGame
import fi.tommi.dg.domain.MatchLogPage
import fi.tommi.dg.domain.MatchLogTurn
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Ottelun siirtohistorian (`/bg/game/<id>/<n>/list`) jäsennin, raakasivusta
 * `raakasivut/game_list.html` (haettu 1.8.2026, jäsennetty 30.8.2026 kun lajimerkinnän
 * linkki tarvitsi kohteen päättyneelle ottelulle).
 *
 * Sivun muoto ja sen kolme ansaa, kaikki aidosta sivusta:
 *
 * - **Rivin sarakkeet elävät `colspan`illa.** Tavallisella vuorolla soluja on viisi
 *   (numero, heitto, siirto, heitto, siirto), mutta avausvuoro tyhjentää toisen puolen
 *   `colspan=2`:lla ja kuutiotoimi (`Doubles => 2`, `Takes`, `Drops`, `Wins 1 point`)
 *   vie puolensa yhtenä `colspan=2`-soluna. Sarake ratkeaa siis vain laskemalla
 *   `colspan`-leveyksiä, ei solun järjestysnumerolla.
 * - **Tanssi on tyhjä linkki.** Heitolla `66:` voi olla siirtosolu jonka `<a>` on tyhjä.
 *   Tyhjä on sivun oma vastaus eikä sitä täytetä.
 * - **Irtorivit.** Pelin viimeisen rivin perässä on tyhjä `<tr>`, ja sivun molemmissa
 *   päissä on navigointitaulukko jonka rivit eivät ole vuoroja. Edellinen ohitetaan
 *   tyhjyydellä, jälkimmäiset sillä ettei niitä edellä `Game`-väliotsikko.
 *
 * Solutekstit kulkevat sivun omina sanoina, ks. [MatchLogPage].
 */
object MatchLogParser {

    fun parse(html: String): MatchLogPage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isMatchLogPage(document)) return null

        val heading = document.selectFirst("h3")?.text()?.trim().orEmpty()
        // Pituusrivi on irtotekstiä otsikon ja taulukon välissä (` 11 point match`), eli
        // se ei ole minkään elementin oma. Tunnistus sanoista eikä paikasta, ja puuttuva
        // rivi on null eikä keksitty.
        val matchLengthText = document.body().textNodes()
            .map { it.text().trim() }
            .firstOrNull { it.contains("point match") }

        val games = mutableListOf<MatchLogGame>()
        var title: String? = null
        var scoreLeft = ""
        var scoreRight = ""
        var turns = mutableListOf<MatchLogTurn>()

        fun closeGame() {
            title?.let { games += MatchLogGame(it, scoreLeft, scoreRight, turns) }
            scoreLeft = ""
            scoreRight = ""
            turns = mutableListOf()
        }

        for (row in document.select("tr")) {
            val header = row.selectFirst("th")?.text()?.trim()
            if (header != null && header.startsWith("Game")) {
                closeGame()
                title = header
                continue
            }
            // Navigointitaulukoiden rivit ja kaikki muu ennen ensimmäistä väliotsikkoa.
            if (title == null) continue

            val cells = row.tableCells()
            if (cells.isEmpty()) continue

            val number = cells.first().text().trim()
            if (number.isEmpty()) {
                // Pistetilannerivi: ensimmäinen solu on tyhjä ja puolet ovat kaksi
                // `colspan=2`-solua. Ehto on colspanissa, jottei navigointirivin tyhjä
                // alku lue navigointia pistetilanteeksi.
                if (cells.size >= 3 && cells[1].attr("colspan") == "2") {
                    scoreLeft = cells[1].text().trim()
                    scoreRight = cells[2].text().trim()
                }
                continue
            }

            val (left, right) = readSides(cells.drop(1))
            if (left.isEmpty() && right.isEmpty()) continue
            turns += MatchLogTurn(number, left, right)
        }
        closeGame()

        return MatchLogPage(heading = heading, matchLengthText = matchLengthText, games = games)
    }

    /**
     * Jakaa vuororivin solut vasempaan ja oikeaan pelaajaan `colspan`-leveyksiä laskien:
     * sarakkeet 0–1 ovat vasemman, 2–3 oikean. Saman puolen solut (heitto ja siirto)
     * liitetään välilyönnillä, jolloin tulos on sivun oma rivi luettavana:
     * `61: 13/7 8/7`.
     */
    private fun readSides(cells: List<Element>): Pair<String, String> {
        var column = 0
        val left = StringBuilder()
        val right = StringBuilder()
        for (cell in cells) {
            val span = cell.attr("colspan").toIntOrNull() ?: 1
            val text = cell.text().trim()
            if (text.isNotEmpty()) {
                val side = if (column < 2) left else right
                if (side.isNotEmpty()) side.append(' ')
                side.append(text)
            }
            column += span
        }
        return left.toString() to right.toString()
    }
}
