package fi.tommi.dg.domain

/**
 * Vastaanotettu ottelukutsu, eli toisen pelaajan suora kutsu joka odottaa vastausta.
 *
 * Mitattu 14.9.2026 (`raakasivut/sessio-14-9-kutsu`, fixture `invitation_received.html`),
 * ja Tommin päätös samana iltana: kutsuun vastataan sovelluksesta, koska DG 8x8:n kaltaiset
 * turnaukset alkavat suorilla kutsuilla useamman kerran vuodessa eikä selainta tarvitse
 * silloin avata lainkaan.
 *
 * **Kutsu ei ole viesti, ja ero on kolmessa kohdassa** (`docs/KOHDE.md`). Se ei kulu:
 * `/bg/nextgame` tarjoaa sen joka kierroksella tavulleen samana kunnes siihen vastataan.
 * Sillä on oma osoite `/bg/invite/<id>`. Ja se vaatii teon: kolme lomaketta samaan
 * osoitteeseen, joista `Accept Invitation` on GET (`action=accept`), `Decline Invitation`
 * POST syyn kanssa ja `Counter Offer` POST samoin kentin kuin `/bg/invite/new`. Siksi sitä
 * ei kirjoiteta arkistoon: arkiston sääntö *tuntematon säilytetään raakana* on kirjoitettu
 * kuluvalle viestille, ja 14.9.2026 se tuotti viisi kaksoisriviä ilman ottelun kuvausta.
 *
 * Sama sääntö kuin muissa lomakkeissa: **mitään ei koota.** Osoite, metodi, kentät ja
 * napit ovat sivun omia, ja puuttuva lomake tekee koko kutsusta nullin jäsentimessä.
 */
data class ReceivedInvitation(
    /** Kutsuja, sivun `<h3>`-linkistä. */
    val from: PlayerRef,
    /**
     * Ottelun kuvaus sivun sanoin, esim. *a private 5 point match of backgammon with no
     * time control*. Sivulla se on `<h3>`:n jälkeen irrallisena tekstinä, ei otsikossa.
     */
    val description: String,
    /** `Accept Invitation`, sivun oma lomake sellaisenaan. */
    val accept: AcceptForm,
    /** `Decline Invitation` syykenttineen. */
    val decline: DeclineForm,
    /** `Counter Offer`, tai null jos sivulla ei ole vastatarjouslomaketta. */
    val counter: InviteForm?,
    /** Napin tekstit sivun sanoin, samassa järjestyksessä: accept, decline, counter. */
    val acceptLabel: String,
    val declineLabel: String,
    val counterLabel: String?,
)

/**
 * `Accept Invitation`: GET `action=accept`, ei muita kenttiä. Metodi luetaan sivulta
 * vaikka se on mitattu GETiksi, samasta syystä kuin muuallakin.
 */
data class AcceptForm(
    val action: String,
    val method: FormMethod,
    val hidden: Map<String, String>,
) {
    fun write(): FormSubmission = FormSubmission(action = action, method = method, fields = hidden)
}

/**
 * `Decline Invitation`: POST `action=decline` ja vapaaehtoinen `reason` (max 80). Tyhjä syy
 * lähtee tyhjänä kenttänä kuten selaimessa; kenttää ei jätetä pois.
 */
data class DeclineForm(
    val action: String,
    val method: FormMethod,
    val hidden: Map<String, String>,
    val reason: TextField?,
) {
    fun write(reason: String): FormSubmission {
        val fields = linkedMapOf<String, String>()
        fields += hidden
        this.reason?.let { fields[it.name] = reason }
        return FormSubmission(action = action, method = method, fields = fields)
    }
}
