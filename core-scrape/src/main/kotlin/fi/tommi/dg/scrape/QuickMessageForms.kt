package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.ReplyForm
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * `POST /bg/sendmsg/<id>` -lomakkeen lukija, jaettuna kahdelle sivulle.
 *
 * **Lomake on sama, sivuja on kaksi**, ja se on mitattu 22.8.2026 raaoista tavuista: sama
 * lomakelaji on sekä jonon pikaviestisivulla (`InboxParser`) että jokaisen toisen pelaajan
 * profiilisivulla (`ProfileParser`). Osoite on käyttäjälle eikä viestille, mikä on koko syy
 * siihen että kaksi eri ruutua voivat käyttää samaa lukijaa.
 *
 * **Miksi tämä on oma tiedostonsa 29.8.2026 alkaen.** Lukija asui siihen asti
 * `InboxParser`in yksityisenä metodina, ja profiililta lähettäminen olisi tarvinnut siitä
 * kopion. Kopio olisi ollut juuri se vikamuoto joka tässä projektissa on nimetty: kaksi
 * lukijaa samalle sivun tosiasialle ajautuvat erilleen, ja alla olevat kaksi mitattua
 * yksityiskohtaa (kentän laji, `maxlength`in merkitys) olisivat päätyneet vain toiseen.
 * Siirto on siirto eikä uudelleenkirjoitus: sisältö on `InboxParser`in oma, ja sen
 * perustelut tulivat mukana.
 *
 * Ero sivujen välillä on siinä **mitä lomake tarkoittaa**, ei siinä miten se luetaan.
 * Jonosivulla se on vastaus juuri saapuneeseen viestiin ja elää vain lukuhetken;
 * profiilisivulla se on keskustelun aloitus, ja se on siellä aina. Tuo ero on
 * kutsupaikkojen asia, ei tämän.
 */
internal object QuickMessageForms {

    /**
     * Vastaus- tai aloituslomake sivulta, tai null jos jokin lähetykseen tarvittava osa
     * puuttuu.
     *
     * **Kaikki tai ei mitään**, koska vajaalla lomakkeella ei voi lähettää: ilman kentän
     * nimeä palvelin ei löydä tekstiä, ja ilman osoitetta ei ole minne lähettää. Puolikas
     * lomake näyttäisi kutsupaikassa kyvyltä jota ei ole, ja juuri sen estäminen on
     * `ReplyForm`in tehtävä.
     *
     * `maxlength` sen sijaan saa puuttua, ja silloin se on null eikä rajattomuus.
     *
     * **Lomake haetaan osoitteesta eikä järjestyksestä**, ja profiilisivu on syy sanoa se
     * ääneen: siellä on kolme lomaketta, ja viestilomake on niistä toinen. Kutsulomake
     * (`/bg/invite/new`) tulee sivulla ensin, joten "ensimmäinen lomake" lähettäisi tekstin
     * väärään paikkaan. Fixture `profile_page_other.html` kantaa kaikki kolme juuri siksi.
     */
    fun read(document: Document): ReplyForm? {
        val form = document.selectFirst("form[action*=/bg/sendmsg/]") ?: return null
        val action = form.attr("action").takeIf { it.isNotEmpty() } ?: return null
        val field = readField(form) ?: return null
        return ReplyForm(
            action = action,
            // Sivulta luettuna. Pikaviestilomake on POST, toisin kuin lautasivun lomake,
            // ja väärä metodi ei tuottaisi virhettä vaan viestin joka ei mene perille.
            method = FormMethod.from(form.attr("method")),
            field = field,
            maxLength = form.selectFirst("input[type=text][maxlength]")
                ?.attr("maxlength")
                ?.toIntOrNull(),
        )
    }

    /**
     * **Kentän laji luetaan myös eikä oleteta**, ja se on mitattu virhe: tämä haki aiemmin
     * vain `textarea`n, koska ainoa nähty sivu oli tallennettu selaimella jossa on
     * laajennus joka vaihtaa yksirivisen kentän moniriviseksi. Aidolla sivulla kenttä on
     * `<INPUT TYPE=text>`, joten `replyField` olisi ollut null juuri tuotannossa. Molemmat
     * luetaan nyt, koska laajennuksen muoto on yhä se joka Tommin omalla koneella näkyy.
     */
    private fun readField(form: Element): String? =
        form.selectFirst("input[type=text], textarea")
            ?.attr("name")
            ?.takeIf { it.isNotBlank() }
}
