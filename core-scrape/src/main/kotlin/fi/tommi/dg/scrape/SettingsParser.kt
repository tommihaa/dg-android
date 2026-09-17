package fi.tommi.dg.scrape

import fi.tommi.dg.domain.ChoiceOption
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.PreferenceChoice
import fi.tommi.dg.domain.PreferenceToggle
import fi.tommi.dg.domain.SETTINGS_FORM_ACTION
import fi.tommi.dg.domain.SettingsPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Asetussivun (`/bg/profile`) jäsennin.
 *
 * Tämä jäsennin on olemassa siksi että lomake **lähetetään takaisin**, ei siksi että
 * asetuksia näytettäisiin. Ks. `SettingsPage`: rastittamaton valintaruutu ei lähde
 * lomakkeessa mukana, joten kentän puuttuminen luetusta tilasta nollaa asetuksen.
 *
 * Kolme ansaa jotka ovat tällä sivulla ja jotka jäsennin ottaa huomioon:
 *
 * 1. **Lomakkeita on kolme** (`/bg/profile/pw`, `/bg/profile/pub`, `/bg/profile/pref`),
 *    eikä sivulla ole id- tai class-attribuutteja joilla ne erottaisi. Jäsennin rajaa
 *    `action`iin. Ilman rajausta ensimmäinen lomake on salasananvaihto, eli kentät joita
 *    tästä sivusta ei pidä lukea lainkaan.
 * 2. **Kentän nimi ei kerro asetusta.** Valintaruudut ovat `NAME=0` ... `NAME=7`, ja ainoa
 *    merkitystä kantava tieto on ruudun vieressä oleva teksti. Se luetaan solun tekstinä
 *    eikä tekstisolmuina, koska yksi seliteteksti sisältää `<em>`-elementin
 *    (`Skip <em>all</em> automatic pages`).
 * 3. **Valinnaton radioryhmä on eri asia kuin puuttuva ryhmä.** Ryhmien nimet luetaan
 *    erikseen valinnoista, jotta lähetystä koottaessa voi vaatia että jokaisella ryhmällä
 *    on arvo. Muuten valinnaton ryhmä katoaisi lähetyksestä hiljaa, mikä on täsmälleen sama
 *    vika kuin rastittamaton valintaruutu.
 */
object SettingsParser {

    /**
     * Asetuslomakkeen action. Sama vakio jäsentimellä ja lähettäjällä, jottei se eriydy.
     *
     * Arvo asuu `core-domain`issa (`SETTINGS_FORM_ACTION`), koska lähetyksen kokoaminen on
     * siellä eikä domain saa tietää jäsentimestä. Tämä on sen nimi tässä moduulissa.
     */
    const val PREF_ACTION = SETTINGS_FORM_ACTION

    private val TITLE = Regex("""Settings for\s+(.+?)\s*$""")

    fun parse(html: String): SettingsPage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isSettingsPage(document)) return null
        val form = document.selectFirst("form[action=$PREF_ACTION]")

        val radios = form?.select("input[type=radio]").orEmpty()
        val groups = radios.groupBy { it.attr("name") }.filterKeys { it.isNotEmpty() }

        return SettingsPage(
            player = PlayerRef(
                name = TITLE.find(document.title())?.groupValues?.get(1)
                    ?: document.selectFirst("h2 em")?.text()?.takeIf { it.isNotBlank() },
            ),
            // Metodi luetaan lomakkeelta samalla perusteella kuin kenttien nimet. Puuttuva
            // lomake antaa GETin, koska se on HTML:n oma sääntö tyhjälle attribuutille;
            // silloin sivulla ei ole lähetettävää eikä valinnalla ole kohdetta.
            method = FormMethod.from(form?.attr("method").orEmpty()),
            toggles = form?.select("input[type=checkbox]").orEmpty()
                .filter { it.attr("name").isNotEmpty() }
                .map { box ->
                    PreferenceToggle(
                        name = box.attr("name"),
                        label = box.label(),
                        checked = box.hasAttr("checked"),
                    )
                },
            radios = groups.mapNotNull { (name, inputs) ->
                inputs.firstOrNull { it.hasAttr("checked") }?.let { name to it.attr("value") }
            }.toMap(),
            radioGroupNames = groups.keys.toList(),
            choices = groups.map { (name, inputs) ->
                PreferenceChoice(
                    name = name,
                    label = form?.headingBefore(inputs.first()),
                    options = inputs.map { input ->
                        ChoiceOption(
                            value = input.attr("value"),
                            label = input.optionLabel(),
                            selected = input.hasAttr("checked"),
                        )
                    },
                )
            },
        )
    }

    /**
     * Selite on ruudun sisältävän solun teksti. Input itse ei tuota tekstiä, joten solun
     * teksti on juuri ja vain selite.
     */
    private fun Element.label(): String = (parent() ?: this).text().trim()

    /**
     * Radiovaihtoehdon selite on **viereisen** solun teksti, ei oman.
     *
     * Ero valintaruutuihin on sivun omaa muotoa eikä tulkintaa: valintaruudun selite on
     * samassa solussa (`<TD><INPUT ...> Hide pip counts`), radion selite omassaan
     * (`<TD><INPUT ...><TD>Blue/White`). Sama sääntö molemmille lukisi radion selitteeksi
     * tyhjän merkkijonon, ja tyhjä selite näyttäisi ruudulla puuttuvalta asetukselta.
     *
     * Varasuunta on arvo eikä tyhjä. Paljas numero on huono selite mutta se on sivun oma
     * tieto, kun taas tyhjä rivi olisi väite ettei vaihtoehdolla ole nimeä.
     */
    private fun Element.optionLabel(): String {
        val cell = parent()?.nextElementSibling()?.text()?.trim()
        return cell?.takeIf { it.isNotEmpty() } ?: attr("value")
    }

    /**
     * Ryhmän otsikko: lähin `<H4>` joka on sivulla ennen tätä elementtiä.
     *
     * Otsikko on taulukon ulkopuolella (`<H4>Board Scheme</H4><TABLE>`), joten sitä ei
     * löydä ryhmästä itsestään eikä sen vanhemmista. Vertailu tehdään siksi lomakkeen
     * elementtien **dokumenttijärjestyksessä**, joka on ainoa suhde jonka sivu tässä antaa.
     *
     * Null on tavallinen tulos eikä virhe: ryhmä ilman otsikkoa on kelvollinen sivu, ja
     * silloin näyttö turvautuu kentän nimeen.
     */
    private fun Element.headingBefore(input: Element): String? {
        val order = allElements.toList()
        val at = order.indexOf(input).takeIf { it >= 0 } ?: return null
        return order.take(at)
            .lastOrNull { it.tagName() == "h4" }
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
