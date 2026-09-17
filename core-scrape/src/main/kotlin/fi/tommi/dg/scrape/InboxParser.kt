package fi.tommi.dg.scrape

import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.ReplyForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * `/bg/nextgame`-jonon yksittäisen kohteen jäsennin.
 *
 * Jono tarjoilee samasta osoitteesta kolmea eri asiaa: pelilautoja, sivuston ilmoituksia
 * ja henkilöiden pikaviestejä. Tämä jäsennin lukee kaksi jälkimmäistä.
 *
 * Lajittelu tehdään otsikosta, ja se osoittautui mahdolliseksi vasta kun molemmat muodot
 * nähtiin. Ne eroavat toisistaan enemmän kuin oletettiin:
 *
 * - ilmoitus: `You have received the following telegram message:`, ei lähettäjää
 * - pikaviesti: `You have received the following quick message from <linkki>`, lähettäjä
 *   linkkinä käyttäjäsivulle
 *
 * Jäsennin kieltäytyy lautasivusta ennen kuin lukee mitään, ks. [parse]. Sama osoite
 * tarjoilee kolmatta lajia, ja sen erottaminen on portti eikä tulkinta.
 *
 * Tuntematonta otsikkoa **ei arvata**: laji on silloin [MessageSource.UNKNOWN] ja
 * [InboxItem.rawHeader] säilyy raakana, jotta kutsuja voi tallentaa viestin menettämättä
 * sitä ja jotta tuntematon muoto huomataan.
 *
 * **Lajin null poistui 9.8.2026, ja se on sopimusmuutos eikä siistiminen.** Aiemmin
 * tuntematon otsikko oli null, ja lupaus "kutsuja voi tallentaa viestin menettämättä
 * sitä" ei ollut pidettävissä: `Message`ssä ei ollut kenttää johon otsikko olisi mahtunut
 * eikä lajia jollaisena tuntemattoman olisi voinut tallentaa. Molemmat lisättiin, ja
 * samalla kaksi esitystapaa samasta asiasta (null täällä, jokin laji siellä) sulautui
 * yhdeksi. Tuntematon säilyy nyt tuntemattomana koko matkan sivulta listalle.
 *
 * **Viestiputken neljä sanaa ja kenen ne ovat** (H7, mitattu 1.9.2026). Havainto luki
 * neljä sanastoa ajautumisena; kolme niistä on rajapinta ja yksi oli oikeasti ajautunut.
 *
 * - `telegram` on **sivuston oma sana**, ja se esiintyy vain siinä ehdossa joka lukee
 *   sivun otsikkoa ([classify]). Sitä ei viedä pidemmälle, koska se on sivun tekstiä.
 * - `announcement` on **sovelluksen sana** samalle lajille, ja sillä laji kulkee
 *   `MessageSource.ANNOUNCEMENT`ina arkistoon ja ruudulle asti.
 * - `inbox` nimeää **tämän sivun ja sen lukeman kohteen**, ei arkistoitua viestiä.
 * - `message` on **domainin sana** tallennetusta viestistä ([Message]).
 *
 * Ajautunut oli [InboxItem]in kaksi kenttää: sama arvo kulki putken päästä päähän eri
 * nimisenä (`kind` → `source`, `headerText` → `rawHeader`). Ne on yhtenäistetty domainin
 * sanoihin, koska ne ovat myös kannan sarakkeita.
 */
object InboxParser {

    fun parse(html: String): InboxItem? {
        val document = Jsoup.parse(html)
        // Lautasivu ei ole viesti, ja tämä rivi on tässä koska päinvastainen oletus oli
        // kirjattuna testin kommenttiin asti. Mitattu 9.8.2026: `move_board.html`in `<h3>`
        // on `Nine Lives #2222, Round 4`, eli lautasivulla ON otsikko. Ilman tätä porttia
        // jonosta noudettu lauta olisi jäsentynyt viestiksi jonka laji on UNKNOWN ja runko
        // tyhjä, ja `chat_thread.html` olisi antanut sille vielä rungonkin.
        //
        // Vika olisi ollut hiljainen ja tuhoava kerralla: arkistoon olisi kirjoittunut
        // valeviesti, eikä jonosta kulunutta kohdetta saa takaisin tarkistamaan mikä se oli.
        if (DgPages.isBoardPage(document)) return null

        val header = document.selectFirst("h3") ?: return null
        val headerText = header.text()

        val senderLink = header.selectFirst("a[href*=/bg/user/]")

        return InboxItem(
            source = classify(headerText),
            rawHeader = headerText,
            sender = senderLink?.let {
                PlayerRef(name = it.text(), userId = SiteIds.userId(it.attr("href")))
            },
            body = readBody(document),
            replyForm = readReplyForm(document),
            nextPath = document.selectFirst("a[href*=/bg/nextgame]")?.attr("href"),
        )
    }

    private fun classify(headerText: String): MessageSource {
        val normalized = headerText.lowercase()
        return when {
            normalized.contains("quick message") -> MessageSource.QUICK_MESSAGE
            normalized.contains("telegram message") -> MessageSource.ANNOUNCEMENT
            else -> MessageSource.UNKNOWN
        }
    }

    /**
     * Vastauslomake sivulta, tai null jos sitä ei voi lähettää.
     *
     * **Lukija on jaettu 29.8.2026 alkaen** (`QuickMessageForms`), koska sama lomake on myös
     * pelaajan profiilisivulla. Perustelut ja kaksi mitattua yksityiskohtaa ovat siellä.
     *
     * Ero säilyy siinä mitä lomake tässä tarkoittaa: jonosivulla se on vastaus juuri
     * saapuneeseen viestiin, ja se elää vain lukuhetken. Ks. `QueueUiState.Saved`.
     */
    private fun readReplyForm(document: org.jsoup.nodes.Document): ReplyForm? =
        QuickMessageForms.read(document)

    /**
     * Viestin teksti on `<pre>`-lohkossa, jonka **sisällä** vastauslomake ja erotinviivat
     * ovat. Naiivi `pre.text()` palauttaisi siis myös painikkeen tekstin.
     *
     * Luku päättyy siksi ensimmäiseen lomakkeen aloittavaan elementtiin, **ei ensimmäiseen
     * elementtiin.** Ero on mitattu 4.8.2026 aidolla ilmoitussivulla: turnausilmoituksessa
     * on `<a>`-linkkejä keskellä lausetta, joten "pysähdy ensimmäiseen elementtiin" jätti
     * viestiksi pelkän `Congratulations to` ja pudotti loput. Vika ei näkynyt aiemmin,
     * koska ainoa telegram-testi käytti kuvakaappauksesta käsin kirjoitettua sivua jossa
     * ei ollut linkkejä.
     *
     * Huom: aiempi perustelu tässä sanoi että lomake sisältää saman viestin lainattuna ja
     * että viesti siksi kaksinkertaistuisi. Se ei pidä paikkaansa. Lainaus tuli
     * selainlaajennuksesta, ei sivustolta, ks. [readReplyField]. Lomake on silti lohkon
     * sisällä ihan itsestään, joten raja on yhä tarpeen.
     */
    private fun readBody(document: org.jsoup.nodes.Document): String {
        val pre = document.selectFirst("pre") ?: return ""
        return pre.childNodes()
            .takeWhile { it !is Element || it.tagName() !in BODY_ENDS_AT }
            .joinToString("") { node ->
                when (node) {
                    is TextNode -> node.wholeText
                    is Element -> node.wholeText()
                    else -> ""
                }
            }
            .trim()
    }

    /** Lohkon sisällä oleva vastauslomake alkaa jostakin näistä. */
    private val BODY_ENDS_AT = setOf("hr", "form", "input", "textarea")
}

/**
 * Yksi jonon kohde.
 *
 * Huom mitä tässä EI ole: aikaleimaa. Sivu ei kerro milloin viesti on lähetetty, mikä on
 * merkittävää arkiston kannalta, ks. `docs/KOHDE.md`.
 */
data class InboxItem(
    /**
     * [MessageSource.UNKNOWN] kun otsikkoa ei tunnisteta. Älä arvaa lajia, tallenna se
     * silti: [rawHeader] säilyttää sen mitä sivu sanoi.
     *
     * **Sana on sama kuin [Message.source] 1.9.2026 alkaen** (H7). Kenttä oli `kind` ja
     * viereinen `headerText`, ja molemmat siirtyivät `Message`en eri nimisinä. Sama arvo
     * kahdella nimellä putken kahdessa päässä on juuri se ajautuminen jonka havainto
     * nimeää, ja domainin sana voittaa koska se on myös kannan sarake.
     */
    val source: MessageSource,
    val rawHeader: String,
    /** Lähettäjä sivun omasta linkistä, tai null kun sivu ei nimeä ketään (ilmoitus). */
    val sender: PlayerRef?,
    val body: String,
    /**
     * Vastauslomake kokonaisena, tai null jos sivulla ei ole lähetyskelpoista sellaista.
     *
     * **Oli kolme erillistä kenttää 22.8.2026 asti** (`replyPath`, `replyField`,
     * `replyMaxLength`), ja ne yhdistettiin kun vastauksen lähettäminen tuli mahdolliseksi.
     * Syy on sama kuin `BoardForm`illa: kolme erillistä arvoa voi yhdistää kutsupaikassa
     * miten tahansa, ja silloin lähetettävän lomakkeen kokoaa se joka lähettää. Yhtenä
     * tyyppinä lomake on se mitä sivu tarjosi, eikä muuta voi lähettää.
     *
     * Ilmoituksella lomaketta ei ole lainkaan, ja se on lajien mitattu ero: sivuston oma
     * tiedote on lyhyt sivu ilman lähettäjää ja ilman vastauslomaketta.
     */
    val replyForm: ReplyForm?,
    val nextPath: String?,
)
