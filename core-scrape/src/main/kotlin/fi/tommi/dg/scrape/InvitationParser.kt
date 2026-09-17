package fi.tommi.dg.scrape

import fi.tommi.dg.domain.AcceptForm
import fi.tommi.dg.domain.DeclineForm
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.ReceivedInvitation
import fi.tommi.dg.scrape.ProfileForms.hiddenFields
import fi.tommi.dg.scrape.ProfileForms.textField
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Vastaanotetun ottelukutsun lukija (`invitation_received.html`, mitattu 14.9.2026).
 *
 * **Tunnistus on lomakkeessa eikä otsikossa.** Sivun `<title>` on `DailyGammon Invitation`,
 * ja se on sama kuin lähetetyn kutsun kuittauksella (`DgPages.isInviteSentPage`). Ero on
 * siinä että tämä sivu kantaa lomakkeen jonka piilokenttä on `action=accept`; kuittaus ei
 * kanna yhtään lomaketta. Otsikko tarkistetaan silti, jotta vieras sivu jolla sattuu olemaan
 * samanniminen kenttä ei jäsenny kutsuksi.
 *
 * Kaikki tai ei mitään: puuttuva hyväksyntä- tai hylkäyslomake tekee tuloksesta nullin,
 * koska ruutu ei saa näyttää kutsua johon ei voi vastata. Vastatarjous on valinnainen,
 * koska sen puuttuminen ei estä vastaamasta.
 */
object InvitationParser {

    fun parse(html: String): ReceivedInvitation? = parse(Jsoup.parse(html))

    fun parse(document: Document): ReceivedInvitation? {
        if (document.title().trim() != "DailyGammon Invitation") return null

        val acceptForm = formWithAction(document, "accept") ?: return null
        val declineForm = formWithAction(document, "decline") ?: return null
        val counterForm = formWithAction(document, "counter")

        val header = document.selectFirst("h3") ?: return null
        val link = header.selectFirst("a[href*=/bg/user/]")
        val from = PlayerRef(
            name = link?.text()?.trim()?.takeIf { it.isNotEmpty() } ?: header.text().trim(),
            userId = link?.let { SiteIds.userId(it.attr("href")) },
            profilePath = link?.attr("href")?.takeIf { it.isNotBlank() },
        )

        return ReceivedInvitation(
            from = from,
            description = description(header),
            accept = AcceptForm(
                action = acceptForm.attr("action"),
                method = FormMethod.from(acceptForm.attr("method")),
                hidden = acceptForm.hiddenFields(),
            ),
            decline = DeclineForm(
                action = declineForm.attr("action"),
                method = FormMethod.from(declineForm.attr("method")),
                hidden = declineForm.hiddenFields(),
                reason = declineForm.textField("reason"),
            ),
            counter = counterForm?.let { ProfileForms.inviteForm(it) },
            acceptLabel = submitLabel(acceptForm) ?: return null,
            declineLabel = submitLabel(declineForm) ?: return null,
            counterLabel = counterForm?.let { submitLabel(it) },
        )
    }

    private fun formWithAction(document: Document, action: String): Element? =
        document.select("form").firstOrNull { form ->
            form.attr("action").isNotBlank() &&
                form.selectFirst("input[type=hidden][name=action]")?.attr("value") == action
        }

    private fun submitLabel(form: Element): String? =
        form.selectFirst("input[type=submit]")?.attr("value")?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Ottelun kuvaus on `<h3>`:n jälkeen irrallisena tekstinä ensimmäiseen lomakkeeseen
     * asti: *a private 5 point match of backgammon with no time control* ja erillinen piste.
     * Sisarsolmut luetaan tekstinä kunnes vastaan tulee lomake, ja tyhjät sekä irtopiste
     * siistitään.
     */
    private fun description(header: Element): String {
        val parts = mutableListOf<String>()
        var node = header.nextSibling()
        while (node != null) {
            if (node is Element && node.tagName().equals("form", ignoreCase = true)) break
            val text = when (node) {
                is Element -> node.text()
                is TextNode -> node.text()
                else -> ""
            }.trim()
            if (text.isNotEmpty()) parts += text
            node = node.nextSibling()
        }
        return parts.joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .replace(" .", ".")
            .trim()
    }
}
