package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckboxField
import fi.tommi.dg.domain.ChoiceOption
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.IgnoreForm
import fi.tommi.dg.domain.InviteForm
import fi.tommi.dg.domain.SelectField
import fi.tommi.dg.domain.TextField
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Profiilisivun kutsu- ja sivuutuslomakkeiden lukija. Viestilomake on `QuickMessageForms`,
 * koska se on jaettu jonosivun kanssa; nämä kaksi ovat vain profiililla.
 *
 * **Lomake haetaan osoitteesta eikä järjestyksestä**, samasta syystä kuin viestilomake:
 * sivulla on kolme lomaketta, ja järjestys on kutsu, viesti, sivuutus (mitattu
 * `user_profile_other.html`). Kaikki tai ei mitään: vajaa lomake on null, jotta ruutu ei
 * näytä kykyä jota ei ole.
 */
internal object ProfileForms {

    fun invite(document: Document): InviteForm? =
        document.selectFirst("form[action*=/bg/invite/new]")?.let { inviteForm(it) }

    /**
     * Kutsulomake annetusta `<form>`-elementistä. Erotettu 14.9.2026, koska vastaanotetun
     * kutsun `Counter Offer` on sama lomake samoin kentin eri osoitteessa
     * (`/bg/invite/<id>`, piilokenttä `action=counter`), ks. `InvitationParser`.
     */
    fun inviteForm(form: Element): InviteForm? {
        val action = form.attr("action").takeIf { it.isNotBlank() } ?: return null

        return InviteForm(
            action = action,
            method = FormMethod.from(form.attr("method")),
            hidden = form.hiddenFields(),
            variant = form.selectField("variant") ?: return null,
            length = form.selectField("length") ?: return null,
            timeControl = form.selectField("time_control") ?: return null,
            comment = form.textField("comment"),
            name = form.textField("name"),
            privateMatch = form.selectFirst("input[type=checkbox][name]")?.let {
                CheckboxField(
                    name = it.attr("name"),
                    value = it.attr("value").ifEmpty { "on" },
                    checked = it.hasAttr("checked"),
                )
            },
        )
    }

    fun ignore(document: Document): IgnoreForm? {
        val form = document.selectFirst("form[action*=/bg/ignore]") ?: return null
        val action = form.attr("action").takeIf { it.isNotBlank() } ?: return null
        val fields = form.hiddenFields()
        if (fields.isEmpty()) return null
        val label = form.selectFirst("input[type=submit]")?.attr("value")?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return null
        return IgnoreForm(
            action = action,
            method = FormMethod.from(form.attr("method")),
            fields = fields,
            label = label,
        )
    }

    fun Element.hiddenFields(): Map<String, String> =
        select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") }

    /**
     * `<select>` nimeltä, vaihtoehdot sivun järjestyksessä. Arvoton `<option>` kantaa
     * tekstinsä arvona, kuten selain tekisi. Tyhjä lista on null: valinta ilman
     * vaihtoehtoja ei ole kenttä.
     */
    private fun Element.selectField(name: String): SelectField? {
        val select = selectFirst("select[name=$name]") ?: return null
        val options = select.select("option").map { option ->
            val text = option.text().trim()
            ChoiceOption(
                value = if (option.hasAttr("value")) option.attr("value") else text,
                label = text,
                selected = option.hasAttr("selected"),
            )
        }
        if (options.isEmpty()) return null
        return SelectField(name = select.attr("name"), options = options)
    }

    fun Element.textField(name: String): TextField? =
        selectFirst("input[type=text][name=$name]")?.let {
            TextField(name = it.attr("name"), maxLength = it.attr("maxlength").toIntOrNull())
        }
}
