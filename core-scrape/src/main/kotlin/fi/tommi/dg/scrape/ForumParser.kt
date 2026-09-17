package fi.tommi.dg.scrape

import fi.tommi.dg.domain.ForumArchivePage
import fi.tommi.dg.domain.ForumBoard
import fi.tommi.dg.domain.ForumComposePage
import fi.tommi.dg.domain.ForumIndex
import fi.tommi.dg.domain.ForumMonthLink
import fi.tommi.dg.domain.ForumPost
import fi.tommi.dg.domain.ForumThread
import fi.tommi.dg.domain.ForumThreadPage
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.PlayerRef
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Keskustelupalstan jäsennin: indeksi (`/bg/forum2`) ja ketjun lukusivu
 * (`/bg/forum2/<palsta>/read/<id>`). Sivujen mitattu rakenne on `docs/KOHDE.md`:ssä
 * (26.8.2026), lähteinä `raakasivut/sessio/0416` ja `0417`.
 *
 * **Palstan tunnus on osa polkua eikä vakio `main`** (mitattu 27.8.2026
 * `raakasivut/forum2_politics.html`:stä): Politicsin ketjut ovat
 * `/bg/forum2/politics/read/<id>`. Siksi ketjurivin tuntomerkki on polun muoto eikä
 * `main`-alkuinen merkkijono, ja sama muoto on `DgPages.isForumIndex`issä.
 *
 * Syöte saapuu jo purettuna merkkijonona: palstan `windows-1252` on [DgClient]in
 * vastuulla eikä tämä jäsennin koske tavuihin.
 *
 * **Ketjusivu jaetaan viesteihin raakatekstistä eikä puusta**, ja se on poikkeus talon
 * tapaan. Syy on sivun muoto: viestit ovat rungon suoraa sisältöä ilman säiliöelementtiä,
 * ja sulkematon `<A NAME=n>` monistuu HTML5-jäsennyksessä jokaisen kappaleen sisään
 * (adoption agency), jolloin puusta ei näe missä viesti vaihtuu. Ankkuri `<A NAME=n>` on
 * silti sivun oma rakenne siinä missä captionit muualla, joten jako tehdään sillä ja
 * vasta palat jäsennetään Jsoupilla.
 */
object ForumParser {

    /**
     * Ketjun lukusivu millä tahansa palstalla. Sama valitsin on `DgPages.isForumIndex`issä.
     */
    const val THREAD_LINK = "a[href~=^/bg/forum2/[^/]+/read/]"
    /**
     * Palstalinkki palstarivillä: `/bg/forum2/` ja täsmälleen yksi osa perässä. Rajaus
     * sulkee ulos sekä yläpalkin `/bg/forum2`:n että sivun omat `/bg/forum2/main/new`- ja
     * `/bg/forum2/main/month`-linkit.
     */
    private const val BOARD_LINK = "a[href~=^/bg/forum2/[^/]+\$]"
    /**
     * `Old Threads` -linkki indeksin alalaidassa: `/bg/forum2/<palsta>/month` ilman
     * vuotta ja kuukautta. Mitattu 29.8.2026 `raakasivut/forum2_politics.html`:stä.
     */
    const val ARCHIVE_LINK = "a[href~=^/bg/forum2/[^/]+/month\$]"
    /**
     * Yhden kuukauden linkki arkistosivun `Previous Months` -luettelossa:
     * `/bg/forum2/<palsta>/month/<vuosi>/<kk>`.
     */
    const val MONTH_LINK = """a[href~=^/bg/forum2/[^/]+/month/\d+/\d+$]"""
    /** `Add a New Thread` -linkki indeksin alalaidassa: `/bg/forum2/<palsta>/new`. */
    const val NEW_THREAD_LINK = "a[href~=^/bg/forum2/[^/]+/new\$]"
    /** `Add a Comment` -linkki ketjusivun lopussa: `/bg/forum2/<palsta>/add/<id>`. */
    const val ADD_COMMENT_LINK = """a[href~=^/bg/forum2/[^/]+/add/\d+$]"""
    /**
     * Kirjoituslomake: `action` on `/bg/forum2/<palsta>/submitnew` tai
     * `.../submitadd/<id>`. Mitattu 3.9.2026 (`docs/KOHDE.md`).
     */
    const val COMPOSE_FORM = """form[action~=^/bg/forum2/[^/]+/submit(new|add/\d+)$]"""
    /** Arkistosivun paluulinkin teksti. */
    private const val BACK_TO_INDEX = "Back to Index"
    /** Esikatselunapin arvo lomakkeella; sitä ei lähetetä. */
    private const val PREVIEW = "Preview"
    /** Sivun `&nbsp;` purkautuu Jsoupissa tähän merkkiin, ja se on kuukausilinkeissä. */
    private const val NBSP = ' '
    /** Palstan nimi otsikosta "DailyGammon Forum: General". */
    private val BOARD_NAME = Regex("""DailyGammon Forum:\s*(.+?)\s*$""")
    /** Viestin alku, sivun oma ankkuri. */
    private val POST_ANCHOR = Regex("""<A\s+NAME=(\d+)>""", RegexOption.IGNORE_CASE)
    /** Allekirjoitusrivin alku, jonka jälkeen runko ei jatku. */
    private val SIGNATURE = Regex("""<P>\s*<B>\s*Posted\s+by\b""", RegexOption.IGNORE_CASE)
    /** Aikaleima allekirjoituksesta: kaikki nimen jälkeisen " at ":n perässä. */
    private val POSTED_AT = Regex("""\bat\s+(.+)$""")

    fun parseIndex(html: String): ForumIndex? {
        val document = Jsoup.parse(html)
        if (!DgPages.isForumIndex(document)) return null

        val threads = document.select("tr:has($THREAD_LINK)")
            .mapNotNull(::parseThreadRow)

        return ForumIndex(
            boardName = document.select("h1").firstNotNullOfOrNull { heading ->
                BOARD_NAME.find(heading.text())?.groupValues?.get(1)
            },
            boards = parseBoards(document),
            threads = threads,
            archivePath = document.selectFirst(ARCHIVE_LINK)?.attr("href")
                ?.takeIf { it.isNotBlank() },
            newThreadPath = document.selectFirst(NEW_THREAD_LINK)?.attr("href")
                ?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Kirjoituslomake, uuden ketjun tai kommentin. Kentät luetaan lomakkeelta nimineen,
     * ja lähetysnapiksi otetaan se `submit`-nappi joka **ei** ole `Preview`: sivulla on
     * kaksi samannimistä nappia, ja esikatselun vastaussivua ei ole mitattu.
     */
    fun parseCompose(html: String): ForumComposePage? {
        val document = Jsoup.parse(html)
        val form = document.selectFirst(COMPOSE_FORM) ?: return null
        val comment = form.selectFirst("textarea[name]") ?: return null
        val submit = form.select("input[type=submit][name]")
            .firstOrNull { !it.attr("value").equals(PREVIEW, ignoreCase = true) }
            ?: return null
        val title = form.selectFirst("input[type=text][name]")

        return ForumComposePage(
            action = form.attr("action"),
            method = FormMethod.from(form.attr("method")),
            boardName = document.select("h1").firstNotNullOfOrNull { heading ->
                BOARD_NAME.find(heading.text())?.groupValues?.get(1)
            },
            heading = document.selectFirst("h2")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            titleField = title?.attr("name")?.takeIf { it.isNotBlank() },
            titleMaxLength = title?.attr("maxlength")?.toIntOrNull(),
            commentField = comment.attr("name"),
            submitField = submit.attr("name"),
            submitLabel = submit.attr("value"),
        )
    }

    /**
     * Kuukausiarkisto. Ketjurivit ovat samat kuin indeksissä, joten ne luetaan samalla
     * funktiolla; sivun omat erot (aikasarake, ei New-merkkejä) ovat [parseThreadRow]issa.
     */
    fun parseArchive(html: String): ForumArchivePage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isForumArchive(document)) return null

        return ForumArchivePage(
            boardName = document.select("h1").firstNotNullOfOrNull { heading ->
                BOARD_NAME.find(heading.text())?.groupValues?.get(1)
            },
            // Kuukausi luetaan sivun omasta otsikosta eikä polusta: `/month` ilman
            // vuotta on kuluva kuukausi, eikä polku silloin kerro mikä se on.
            monthLabel = document.selectFirst("h2")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            threads = document.select("tr:has($THREAD_LINK)").mapNotNull(::parseThreadRow),
            months = document.select(MONTH_LINK).mapNotNull { link ->
                val label = link.text().replace(NBSP, ' ').trim()
                val path = link.attr("href")
                if (label.isEmpty() || path.isBlank()) null else ForumMonthLink(label, path)
            },
            // Paluulinkki tunnistetaan sen omasta tekstistä eikä polun muodosta: sivulla
            // on myös toisen palstan linkki, ja se on samaa muotoa.
            indexPath = document.select(BOARD_LINK).firstOrNull { it.text().trim() == BACK_TO_INDEX }
                ?.attr("href")?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Palstarivi otsikon alta: `<P><b>General</b> | <A HREF=/bg/forum2/politics>Politics</A>`.
     *
     * Rivi tunnistetaan sen ainoasta palstalinkistä ja luetaan sen jälkeen kappaleen
     * lapsista järjestyksessä, jotta palstat pysyvät sivun omassa järjestyksessä eikä
     * lihavoitu nykyinen putoa pois. Kappaleessa ei ole muuta kuin palstat ja erottimet.
     */
    private fun parseBoards(document: Document): List<ForumBoard> {
        val row = document.selectFirst(BOARD_LINK)?.parent() ?: return emptyList()

        return row.children().mapNotNull { child ->
            when (child.normalName()) {
                "b", "strong" -> child.text().trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { ForumBoard(name = it, path = null) }
                "a" -> child.attr("href").takeIf { it.startsWith("/bg/forum2/") }
                    ?.let { href ->
                        child.text().trim().takeIf { it.isNotEmpty() }
                            ?.let { ForumBoard(name = it, path = href) }
                    }
                else -> null
            }
        }
    }

    private fun parseThreadRow(row: Element): ForumThread? {
        val readLink = row.selectFirst(THREAD_LINK) ?: return null
        val readPath = readLink.attr("href").takeIf { it.isNotBlank() } ?: return null

        return ForumThread(
            title = readLink.text().trim(),
            // Polku fragmentteineen sellaisenaan: #n on palvelimen oma osoitin
            // ensimmäiseen lukemattomaan viestiin.
            readPath = readPath,
            postCount = row.select("td[align=center]").firstNotNullOfOrNull {
                it.text().trim().toIntOrNull()
            },
            poster = row.selectFirst("i")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            // New-merkki on oma <FONT><B>New</B></FONT> -elementtinsä linkin edellä.
            // Vertailu on koko elementin teksti eikä contains, koska otsikko voi
            // sisältää sanan New.
            isNew = row.select("font b").any { it.text().trim() == "New" },
            // Aikasolu on kuukausisivun neljäs sarake. Tunnistus on linkittömyys eikä
            // järjestysnumero: indeksin neljäs solu on `Hide`-linkki, ja pelkkä indeksi
            // poimisi sen tekstin ajaksi.
            postedAtText = row.select("td").lastOrNull()
                ?.takeIf { it.selectFirst("a") == null }
                ?.text()?.trim()?.takeIf { it.isNotEmpty() && it.toIntOrNull() == null },
        )
    }

    fun parseThread(html: String): ForumThreadPage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isForumThread(document)) return null
        val title = document.selectFirst("h2")?.text()?.trim()?.takeIf { it.isNotEmpty() }

        val anchors = POST_ANCHOR.findAll(html).toList()
        val posts = anchors.mapIndexed { index, anchor ->
            val start = anchor.range.last + 1
            val end = anchors.getOrNull(index + 1)?.range?.first ?: html.length
            parsePost(
                ordinal = anchor.groupValues[1].toIntOrNull(),
                segment = html.substring(start, end),
            )
        }

        return ForumThreadPage(
            title = title,
            posts = posts,
            addCommentPath = document.selectFirst(ADD_COMMENT_LINK)?.attr("href")
                ?.takeIf { it.isNotBlank() },
        )
    }

    private fun parsePost(ordinal: Int?, segment: String): ForumPost {
        // Runko päättyy allekirjoitusriviin. Allekirjoituksen jälkeinen häntä (viimeisen
        // viestin footer, välissä oleva New From Here -otsake) jää käyttämättä.
        val signatureStart = SIGNATURE.find(segment)?.range?.first
        val bodyHtml = if (signatureStart != null) segment.substring(0, signatureStart) else segment
        val signatureHtml = if (signatureStart != null) segment.substring(signatureStart) else ""

        val authorLink = Jsoup.parse(signatureHtml)
            .selectFirst("b:contains(Posted by) a[href*=/bg/user/]")
        val signatureText = authorLink?.parent()?.text().orEmpty()

        return ForumPost(
            ordinal = ordinal,
            body = paragraphText(Jsoup.parse(bodyHtml).body()),
            author = PlayerRef(
                name = authorLink?.text()?.trim()?.takeIf { it.isNotEmpty() },
                userId = authorLink?.let { SiteIds.userId(it.attr("href")) },
            ),
            postedAtText = authorLink?.let {
                val afterName = signatureText.substringAfter(it.text(), missingDelimiterValue = "")
                POSTED_AT.find(afterName)?.groupValues?.get(1)?.trim()?.takeIf { at -> at.isNotEmpty() }
            },
        )
    }

    /**
     * Rungon teksti niin että `<p>` erottaa kappaleet tyhjällä rivillä. Jsoupin `text()`
     * ei kelpaa yksin, koska se litistää kappaleet yhdeksi välilyönnillä. Linkit ja muut
     * rivinsisäiset elementit litistyvät näkyväksi tekstikseen.
     */
    private fun paragraphText(body: Element): String {
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            val text = current.toString().trim()
            if (text.isNotEmpty()) paragraphs.add(text)
            current.setLength(0)
        }

        fun walk(node: Node) {
            when {
                node is TextNode -> current.append(node.text())
                node is Element && node.normalName() == "p" -> {
                    flush()
                    current.append(node.text())
                    flush()
                }
                node is Element -> node.childNodes().forEach(::walk)
            }
        }
        body.childNodes().forEach(::walk)
        flush()

        return paragraphs.joinToString("\n\n")
    }
}
