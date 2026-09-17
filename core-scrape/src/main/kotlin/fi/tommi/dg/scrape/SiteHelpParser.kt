package fi.tommi.dg.scrape

import fi.tommi.dg.domain.SiteHelpEntry
import fi.tommi.dg.domain.SiteHelpPage
import fi.tommi.dg.domain.SiteHelpSection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Sivuston oman Helpin (`/help`) jäsennin. Lähde `raakasivut/site_help.html` (29.8.2026).
 *
 * Sivun mitattu rakenne: `h2`-osastot, ja jokainen kysymys muodossa
 * `<a name="X"><h3>Kysymys</h3></a>` jonka perässä vastaus on rungon suoraa sisältöä
 * seuraavaan kysymykseen, osastoon tai `Top`-paluulinkkiin asti. Vastauksissa on
 * kappaleita (`<br><br>`), luetteloita ja kaksi taulukkoa.
 *
 * **Kulku on rungon lapsia järjestyksessä eikä valitsimia**, koska vastauksella ei ole
 * säiliöelementtiä: se on irtotekstiä ja -elementtejä kysymysten välissä, sama muoto
 * jonka takia [ForumParser] jakaa ketjusivun raakatekstistä. Täällä puu kelpaa, mutta
 * kysymysankkuri ei ole aina suljettu: sivulla on molempia muotoja, ja auki jäänyt
 * ankkuri saa vastauksen alun lapsekseen. Se luetaan kysymyksen kohdalla erikseen.
 *
 * Kysymys tunnistetaan **kummassakin muodossa**: `h3` ankkurin sisällä tai suoraan
 * rungossa. HTML4-sivu panee lohkoelementin `<a>`:n sisään, ja se on jäsentimen version
 * asia kumpaan kohtaan `h3` normalisoituu; tämä ei saa riippua siitä.
 */
object SiteHelpParser {

    fun parse(html: String): SiteHelpPage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isSiteHelpPage(document)) return null
        val body = document.body()

        val sections = mutableListOf<SiteHelpSection>()
        var sectionTitle: String? = null
        var entries = mutableListOf<SiteHelpEntry>()
        var question: String? = null
        val answer = StringBuilder()

        fun closeEntry() {
            val q = question
            if (q != null) {
                val text = readable(answer.toString())
                if (text.isNotEmpty()) entries.add(SiteHelpEntry(question = q, answer = text))
            }
            question = null
            answer.setLength(0)
        }

        fun closeSection() {
            closeEntry()
            val title = sectionTitle
            // Osasto ilman yhtään kysymystä (sisällysluettelon oma otsikko) ei ole osasto.
            if (title != null && entries.isNotEmpty()) {
                sections.add(SiteHelpSection(title = title, entries = entries))
                entries = mutableListOf()
            }
            sectionTitle = null
        }

        body.childNodes().forEach { node ->
            val element = node as? Element
            val questionHeading = element?.questionHeading()
            when {
                element?.normalName() == "h2" -> {
                    closeSection()
                    sectionTitle = element.text().trim()
                }
                questionHeading != null -> {
                    closeEntry()
                    question = questionHeading.text().trim()
                    // Sulkematon ankkuri nielaisee vastauksen alun. Sivulla on molempia
                    // muotoja: `<a name="pw"> <h3>..</h3> </a>` on suljettu, mutta
                    // `<a name="run"><h3>..</h3>` ei ole, ja Jsoup sulkee sen vasta
                    // vastauksen ensimmäiseen `<a>`-alkuun. Väliin jäävä teksti on siis
                    // ankkurin lapsi eikä rungon, ja ilman tätä se katoaa hiljaa.
                    element?.childNodes()
                        ?.dropWhile { it !== questionHeading }
                        ?.drop(1)
                        ?.forEach { appendReadable(it, answer) }
                }
                // Navigointipalkit ja pääotsikko asuvat diveissä, eikä niissä ole sisältöä.
                element?.normalName() == "div" -> Unit
                // Sivun oma paluulinkki seuraa jokaista vastausta, joten se on myös
                // vastauksen pääte: sen jälkeinen irtoteksti (osaston häntä, sivun
                // päivitysrivi) ei kuulu kysymykseen.
                element?.normalName() == "a" && element.attr("href") == "#top" -> closeEntry()
                question != null -> appendReadable(node, answer)
                else -> Unit
            }
        }
        closeSection()

        return SiteHelpPage(sections)
    }

    /** Kysymysotsikko tästä elementistä: `h3` itse tai ankkurin sisällä. */
    private fun Element.questionHeading(): Element? = when {
        normalName() == "h3" -> this
        normalName() == "a" -> children().firstOrNull { it.normalName() == "h3" }
        else -> null
    }

    /**
     * Solmun sisältö luettavana tekstinä: rivinvaihdot vain sivun omista rakenteista
     * (`br`, luettelorivi, taulukkorivi), muu tyhjä tila yhdeksi välilyönniksi.
     */
    private fun appendReadable(node: Node, out: StringBuilder) {
        when {
            node is TextNode -> out.append(node.text().replace(WHITESPACE, " "))
            node is Element -> when (node.normalName()) {
                "br" -> out.append('\n')
                "li" -> {
                    out.append("\n• ")
                    node.childNodes().forEach { appendReadable(it, out) }
                }
                // Taulukko litistyy riveiksi: solujen tekstit peräkkäin. Kahden mitatun
                // taulukon sisältö on esimerkinomainen, joten rivimuoto riittää.
                "table" -> node.select("tr").forEach { row ->
                    out.append('\n').append(row.text().trim())
                }
                "ul", "ol" -> {
                    node.childNodes().forEach { appendReadable(it, out) }
                    out.append('\n')
                }
                "p" -> {
                    out.append("\n\n")
                    node.childNodes().forEach { appendReadable(it, out) }
                }
                else -> node.childNodes().forEach { appendReadable(it, out) }
            }
        }
    }

    /** Siivous: rivit trimmattuina, tyhjien rivien jonot yhdeksi kappaleenvaihdoksi. */
    private fun readable(raw: String): String =
        raw.lineSequence()
            .map { it.trim().replace(WHITESPACE, " ") }
            .joinToString("\n")
            .replace(BLANK_RUN, "\n\n")
            .trim()

    private val WHITESPACE = Regex("""\s+""")
    private val BLANK_RUN = Regex("""\n{3,}""")
}
