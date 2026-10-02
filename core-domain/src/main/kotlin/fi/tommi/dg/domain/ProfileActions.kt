package fi.tommi.dg.domain

/**
 * Toisen pelaajan profiilin kaksi tekolomaketta viestilomakkeen rinnalla: ottelukutsu ja
 * sivuutus. Molemmat mitattiin 22.8. ja 27.8.2026 (`docs/KOHDE.md`, `raakasivut/user_profile_other.html`)
 * ja **rajattiin ulos 27.8.2026**, koska ne ovat peruuttamattomia tekoja eivätkä sivun
 * sisältöä. Rajaus purettiin 3.9.2026 Tommin tilauksesta (*"Kutsulomake, Ignore, Resign
 * matches toiminnot"*), ks. `docs/UI.md` › Ottelukutsu ja sivuutus profiililta.
 *
 * Sama sääntö kuin viestilomakkeella: **mitään ei koota**. Osoite, metodi, kenttien nimet,
 * vaihtoehdot ja piilokentät ovat sivun omia, ja puuttuva osa tekee lomakkeesta nullin
 * jäsentimessä eikä puolikasta kykyä ruudulle.
 */

/**
 * Ottelukutsu `POST /bg/invite/new`. Mitatut kentät sivun järjestyksessä: `player`
 * (piilotettu), `variant`, `length`, `comment` (max 80), `name` (max 40), `time_control` ja
 * `private` (valintaruutu, arvo `private`).
 *
 * Kutsu on **kohdennettu** tälle pelaajalle: se saapuu vastaanottajalle `/bg/nextgame`stä
 * lauseena `<nimi> invites you ...` ja sillä on oma osoite `/bg/invite/<id>` (mitattu
 * 22.8.2026). Se on eri asia kuin loungen avoin ilmoitus (`SUBSTANSSI.md` kohta 11).
 */
data class InviteForm(
    val action: String,
    val method: FormMethod,
    /** Piilokentät sellaisinaan, sivulla `player`. */
    val hidden: Map<String, String>,
    val variant: SelectField,
    val length: SelectField,
    val timeControl: SelectField,
    /** Vapaa teksti kutsuun, sivulla `comment`. Null jos sivulla ei ole kenttää. */
    val comment: TextField?,
    /** Ottelun nimi, sivulla `name`. Null jos sivulla ei ole kenttää. */
    val name: TextField?,
    /** `Private Match` -ruutu. Mitattu vaikutus on vain sana `private` kutsun lauseessa. */
    val privateMatch: CheckboxField?,
) {
    /**
     * Lomake lähetettäväksi käyttäjän valinnoilla, tai null jos jokin valinta ei ole sivun
     * oma vaihtoehto. Null on portti eikä virhe: ruutu ei voi lähettää arvoa jota sivu ei
     * tarjonnut, koska palvelimen vastausta keksittyyn arvoon ei ole mitattu.
     *
     * Kentät ovat sivun järjestyksessä ja tekstikentät mukana tyhjinäkin, koska selain
     * lähettää ne niin; ruksaamaton valintaruutu jää pois samasta syystä.
     */
    fun write(choice: InviteChoice): FormSubmission? {
        if (!variant.offers(choice.variant)) return null
        if (!length.offers(choice.length)) return null
        if (!timeControl.offers(choice.timeControl)) return null

        val fields = linkedMapOf<String, String>()
        fields += hidden
        fields[variant.name] = choice.variant
        fields[length.name] = choice.length
        comment?.let { fields[it.name] = choice.comment }
        name?.let { fields[it.name] = choice.name }
        fields[timeControl.name] = choice.timeControl
        if (choice.privateMatch) privateMatch?.let { fields[it.name] = it.value }
        return FormSubmission(action = action, method = method, fields = fields)
    }
}

/** Käyttäjän valinnat kutsuun. Arvot ovat lomakkeen arvoja (`1`, `5`, `0`), eivät selitteitä. */
data class InviteChoice(
    val variant: String,
    val length: String,
    val timeControl: String,
    val comment: String = "",
    val name: String = "",
    val privateMatch: Boolean = false,
)

/** Yksi `<select>`: kentän nimi ja vaihtoehdot sivun järjestyksessä. */
data class SelectField(
    val name: String,
    val options: List<ChoiceOption>,
) {
    /** Sivun esivalinta, tai ensimmäinen jos sivu ei valinnut mitään. */
    val default: ChoiceOption? get() = options.firstOrNull { it.selected } ?: options.firstOrNull()

    fun offers(value: String): Boolean = options.any { it.value == value }

    fun label(value: String): String? = options.firstOrNull { it.value == value }?.label
}

/** Yksi tekstikenttä: nimi ja sivun `maxlength`, tai null jos sitä ei ole. */
data class TextField(
    val name: String,
    val maxLength: Int?,
)

/** Yksi valintaruutu: nimi ja arvo joka lähtee kun ruutu on ruksattu. */
data class CheckboxField(
    val name: String,
    val value: String,
    /**
     * Sivun esivalinta (`checked`). Lisätty 14.9.2026: vastatarjouksen `Private Match`
     * perii kutsun arvon ja on sivulla rastitettu, mutta ruutu alkoi tyhjänä ja lähetti
     * vastatarjouksen ilman `private`ä (mitattu `sessio-14-9-kutsu` rivi 12).
     */
    val checked: Boolean = false,
)

/**
 * Sivuutus `GET /bg/ignore` piilokentin `changeto` ja `user`. Muuttava teko tavallisen
 * linkin muodossa, ja `changeto` viittaa siihen että sama osoite sekä asettaa että poistaa
 * (`docs/KOHDE.md`). Vastausta ei ole mitattu; onnistuminen luetaan siitä että profiilin
 * nappi vaihtuu.
 */
data class IgnoreForm(
    val action: String,
    val method: FormMethod,
    /** Piilokentät sellaisinaan, sivulla `changeto` ja `user`. */
    val fields: Map<String, String>,
    /** Napin oma teksti, sivulla esim. `Ignore <nimi>`. Se on ainoa mikä kertoo suunnan. */
    val label: String,
) {
    /** Sivun `changeto`-arvo, eli mihin tilaan lähetys vie. */
    val changeTo: String? get() = fields["changeto"]

    fun write(): FormSubmission = FormSubmission(action = action, method = method, fields = fields)
}
