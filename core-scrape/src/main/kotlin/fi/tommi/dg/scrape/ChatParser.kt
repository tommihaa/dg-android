package fi.tommi.dg.scrape

import fi.tommi.dg.domain.ChatForm
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.isSpeculativePrompt
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Siirron jälkeisen ruudun chat-osa.
 *
 * Tämä on se sivu jota varten koko sovellus on olemassa: vastustajan viesti näkyy vain
 * täällä, eikä palvelin säilytä sitä. Lauta on samalla sivulla, mutta sen lukee
 * [BoardParser]; tämä jäsennin koskee vain keskustelua ja lähetyslomaketta.
 *
 * Otsikkoja on **kolme ja ne tarkoittavat eri tilannetta** (kaksi ensimmäistä Tommin
 * havainto 3.8.2026, kolmas mitattu 21.8.2026):
 *
 * - `You may chat with <nimi> here:` kun keskustelua ei vielä ole
 * - `Chat:` kun ketju on jo alkanut, jolloin `<pre>` sisältää edellisen vaihdon
 * - `<nimi> says:` kun vastustaja on juuri kirjoittanut, jolloin nimi on otsikossa
 *
 * Yhtäkään ei siis voi käyttää yksin sivun tunnistamiseen.
 *
 * Kolmas löytyi DG Mobilen liikenteestä eikä omasta selailusta, ja se oli **vuotanut
 * jäsentimen läpi hiljaa**: sivu jäsentyi muuten oikein, mutta `opponentName` jäi nulliksi
 * juuri sillä sivulla jolla vastustajan nimi on näkyvissä.
 */
object ChatParser {

    private val MATCH_ID = Regex("""/bg/move/(\d+)/""")
    private val CHAT_WITH = Regex("""You may chat with\s+(.+?)\s+here:""", RegexOption.IGNORE_CASE)

    /**
     * Kolmas otsikkomuoto. Ankkuroitu molemmista päistä, koska pelkkä `says:`-osajono
     * osuisi myös viestin omaan tekstiin.
     */
    private val SAYS = Regex("""^(.+?)\s+says:$""", RegexOption.IGNORE_CASE)

    fun parse(html: String): ChatScreen? {
        val document = Jsoup.parse(html)
        val form = document.selectFirst("form[action*=/bg/move/]") ?: return null

        val bodyText = document.body()?.wholeText().orEmpty()
        val header = headerText(document, bodyText)
        val thread = readThread(document)

        return ChatScreen(
            matchId = MATCH_ID.find(form.attr("action"))
                ?.groupValues?.get(1)
                ?.let { MatchId(it) },
            speculative = isSpeculativePrompt(bodyText),
            headerText = header,
            opponentName = header?.let {
                CHAT_WITH.find(it)?.groupValues?.get(1)?.trim()
                    ?: SAYS.find(it)?.groupValues?.get(1)?.trim()
            },
            incoming = thread.incoming,
            quoted = thread.quoted,
            form = parseForm(form),
        )
    }

    /**
     * Lomake kootaan domainin tyypiksi eikä irrallisiksi kentiksi, koska lähetys tehdään
     * `ChatForm.write`llä ja sen portit ovat siellä. Null kun tekstikenttää ei ole: ilman
     * sitä sivulla ei ole mitään mihin kirjoittaa, eikä lomakkeen muista osista ole silloin
     * hyötyä. Sivu voi silti kantaa vastustajan viestin, ja se luetaan erikseen.
     */
    private fun parseForm(form: Element): ChatForm? {
        val field = form.selectFirst("textarea")?.attr("name")?.takeIf { it.isNotBlank() }
            ?: return null
        val quote = form.selectFirst("input[type=checkbox]")
        return ChatForm(
            action = form.attr("action"),
            // Sivulta luettuna eikä oletettuna, vaikka tämä on mitattu POSTiksi.
            method = FormMethod.from(form.attr("method")),
            field = field,
            quoteField = quote?.attr("name")?.takeIf { it.isNotBlank() },
            // Attribuutin arvo voittaa, ja sen puuttuessa `on` on HTML:n oma sääntö eikä
            // tämän koodin arvaus. Ks. `ChatForm.quoteValue`.
            quoteValue = quote?.attr("value")?.takeIf { it.isNotBlank() } ?: "on",
            quoteCheckedByDefault = quote?.hasAttr("checked") ?: false,
            hiddenFields = form.select("input[type=hidden]")
                .filter { it.attr("name").isNotBlank() }
                .associate { it.attr("name") to it.attr("value") },
            submits = form.select("input[type=submit]")
                .map { it.attr("value") }
                .filter { it.isNotBlank() },
        )
    }

    private fun headerText(document: Document, bodyText: String): String? {
        document.selectFirst("b:matchesOwn(^Chat:$)")?.let { return "Chat:" }
        document.select("i, b").firstOrNull { it.text().contains("Chat:") }?.let { return "Chat:" }
        document.select("i, b")
            .map { it.text().trim() }
            .firstOrNull { SAYS.matches(it) }
            ?.let { return it }
        return CHAT_WITH.find(bodyText)?.value
    }

    /**
     * Ketju luetaan `<pre>`-lohkosta, ja lukutapa on sama kuin [InboxParser]issa samasta
     * syystä: pikaviestisivulla lohko jää sulkematta, jolloin lomake on lähteessä sen
     * sisällä ja `pre.text()` palauttaisi tekstin kahteen kertaan.
     *
     * **Tämän sivun `<pre>` sulkeutuu** (todennettu 21.8.2026 raa'oista tavuista, vahvistettu
     * 27.8.2026 kolmesta eri ottelusta ja proxysessiosta, joista yksi on DG Mobilen
     * liikennettä). Lukutapa on silti tämä eikä `pre.text()`, koska se on oikein myös
     * suljetulle lohkolle ja pikaviestisivu tarvitsee sen yhä.
     *
     * Tässä luki 27.8.2026 asti että sulkeutuminen on todentamatta ja purkautuu vasta
     * `Ctrl+U`-kaappauksella. Väite oli silloin jo kuusi päivää vanhentunut, ks.
     * `docs/KOHDE.md`.
     */
    private fun readThread(document: Document): Thread {
        val pre = document.selectFirst("pre") ?: return Thread(null, null)
        val raw = pre.childNodes()
            .takeWhile { it !is Element }
            .filterIsInstance<TextNode>()
            .joinToString("") { it.wholeText }

        val quoted = mutableListOf<String>()
        val incoming = mutableListOf<String>()
        raw.lines().forEach { line ->
            if (line.startsWith(">")) {
                // Vain yksi taso poistetaan kerrallaan. Syvempää lainausta (">>") ei ole
                // nähty, joten sen käsittelyä ei kirjoiteta arvauksen varaan.
                quoted += line.removePrefix(">")
            } else {
                incoming += line
            }
        }

        return Thread(
            incoming = incoming.joinToString("\n").trim().takeIf { it.isNotEmpty() },
            quoted = quoted.joinToString("\n").trim().takeIf { it.isNotEmpty() },
        )
    }

    private data class Thread(val incoming: String?, val quoted: String?)
}

/**
 * Siirron jälkeisen ruudun keskusteluosa.
 *
 * Huom mitä tässä EI ole: lähettäjän nimeä. Chat-lohko ei kerro kuka kirjoitti, ja sivulla
 * on kaksi käyttäjälinkkiä joista kumpikaan ei ole merkitty. Lähettäjä kootaan laudan
 * pelaajapaneeleista ([BoardParser]) eikä arvata täältä.
 */
data class ChatScreen(
    val matchId: MatchId?,
    /**
     * Lauta on hypoteettinen eikä nykytila. **Tallennuksen este:** tästä sivusta luettua
     * lautaa ei saa kirjata pelin tilaksi, vaikka chat on aitoa.
     */
    val speculative: Boolean,
    val headerText: String?,
    /** Vastustajan nimi, mutta vain `You may chat with <nimi> here:` -muodossa. */
    val opponentName: String?,
    /**
     * Lainaamaton osa, eli vastapuolen uusin viesti. Null kun keskustelua ei ole.
     *
     * **Tämä on ainoa osa joka kuuluu arkistoon.** Lainattu osa on jo tallennettu kerran,
     * ja koska `Message.id` on sisältötiiviste, sitaatin mukaan ottaminen tuottaisi samasta
     * viestistä eri tunnisteen joka kierroksella ja täyttäisi arkiston kopioilla.
     */
    val incoming: String?,
    /** Lainattu osa ilman `>`-etuliitettä, historian tunnistamista varten. */
    val quoted: String?,
    /**
     * Lähetyslomake, tai null kun sivulla ei ole kirjoituskenttää.
     *
     * **Kohde luetaan, ei koota:** haetun sivun tilatunniste oli 1119 ja sen oman lomakkeen
     * 1121, eli polusta laskettu osoite olisi ollut väärä. Kentät asuvat `ChatForm`issa
     * eivätkä täällä, koska lähetyksen portit ovat domainissa (`ChatForm.write`).
     */
    val form: ChatForm?,
)
