package fi.tommi.dg.domain

/**
 * Teko joka painettiin muttei mennyt perille.
 *
 * **Tämä ei ole lähetys vaan painallus**, ja ero on koko tyypin syy. [FormSubmission] on
 * valmis lähtevä arvo, jonka konstruktori on tämän moduulin sisäinen juuri siksi ettei
 * lähetystä voi koota missään muualla. Jos jono tallentaisi valmiin lähetyksen, se olisi
 * pakko koota takaisin kannasta, ja tae purkautuisi hiljaa siihen kohtaan.
 *
 * Siksi tässä on vain se mitä käyttäjä teki: **millä sivulla hän oli ja mitä nappia hän
 * painoi.** Uudelleenyritys hakee lautasivun tuoreena ja painaa saman napin uudestaan, eli
 * lähetys syntyy yhä sivun omasta lomakkeesta eikä kannasta.
 *
 * Sivutuote joka on tärkeämpi kuin muoto: **sivu itse kertoo onko itsenäinen teko jo mennyt
 * läpi.** Jos nappi ei ole enää sivun nappien joukossa, tekoa ei toisteta. Ks.
 * [BoardState.stillOffers].
 *
 * **Kokoamisesta riippuvaan tekoon sama päättely ei päde**, ja se mitattiin laiteajossa
 * 10.8.2026: koottu siirto ei kestä sivun uudelleenhakua lainkaan, vaan nappulat palaavat
 * lähtöruutuihin myös silloin kun mitään ei lähetetty. Kontrolli ajettiin ilman katkoa ja
 * antoi saman tuloksen. Uusinnan turvallisuus perustuu siihen että sivu haetaan ensin, joten
 * juuri se haku tuhoaa sen mitä oltiin lähettämässä. Ks. [PendingAction.dependsOnAssembly].
 *
 * @property boardPath lautasivun sisäänkäyntipolku, jolta lomake haetaan uudelleen. Sama
 *   polku jolla ruutu avattiin, eli sivun oma linkki eikä koottu osoite.
 * @property submit painetun napin nimi sivun omalla sanalla, esim. `Submit Move`.
 * @property verified rastittiko käyttäjä sivun vahvistusruudun. Se on hänen elensä eikä
 *   sovelluksen politiikka, joten se säilytetään sellaisenaan eikä oleteta uudelleen.
 * @property lastErrorText viimeisimmän katkoksen syy raakana. Kirjataan jo siihen katkokseen
 *   jonka käyttäjä koki painaessaan, koska rivi on olemassa juuri sen kertomista varten.
 *
 * **Yrityslaskuria ei ole** (8.9.2026). Se laski uusinnat, ja uusintanappi poistui: rivi
 * ratkeaa nyt haetusta sivusta tai käyttäjän hylkäyksestä, eikä kumpikaan ole yritys.
 * Kannan sarake jäi paikalleen, ks. [fi.tommi.dg.data.db.PendingActionEntity].
 */
data class PendingAction(
    val id: Long = 0,
    val matchId: MatchId?,
    val boardPath: String,
    val submit: String,
    val pendingMove: String?,
    val verified: Boolean,
    val createdAtEpochMillis: Long,
    val lastErrorText: String? = null,
)

/**
 * Nojaako tämä painallus siihen kokoamistilaan jota palvelin piti painallushetkellä.
 *
 * **Laji luetaan sivun omasta piilokentästä eikä napin nimestä.** `move`-kenttä on olemassa
 * täsmälleen silloin kun lähetettävänä on kerättyä tilaa, joten ehto seuraa sivustoa myös jos
 * napin teksti joskus muuttuu. Nimeen sidottu ehto (`submit == "Submit Move"`) olisi arvaus
 * sanasta, ja se on tässä projektissa sama virhe kuin osoitteen kokoaminen.
 *
 * Tällaista tekoa haku ei myöskään ratkaise, koska haku itse nollaa kokoamisen: sivu ei
 * tarjoa tekoa haun jälkeen, muttei siksi että teko olisi mennyt perille. Rivi jää jonoon ja
 * odottaa hylkäystä: se on muistutus siitä että teko jäi epävarmaksi, ja se on arvokas
 * silloinkin kun sitä ei voi toistaa.
 */
val PendingAction.dependsOnAssembly: Boolean
    get() = pendingMove != null

/**
 * Tarjoaako tämä lauta yhä täsmälleen sen painalluksen jonka [action] kuvaa.
 *
 * Kolme ehtoa, ja jokainen niistä sulkee eri tavan jolla toisto olisi väärä teko:
 *
 * 1. **Lomake on olemassa.** Ilman lomaketta sivu ei tarjoa yhtään nappia, eli tilanne on
 *    toinen kuin painallushetkellä.
 * 2. **Nappi on yhä sivun nappien joukossa.** Sama ehto kuin [BoardForm.press]issä, ja se on
 *    tässä uudestaan siksi että kysymys esitetään ennen kuin mitään lähetetään.
 * 3. **Kokoamistila on sama.** Ehto on tarkoituksella tiukin mahdollinen: null vastaa vain
 *    nulliin. Lomake jolla ei ole piilokenttää on eri kysymys kuin lomake jolla on, eikä eroa
 *    saa lukea puuttuvaksi tiedoksi.
 *
 * **Tässä luki 10.8.2026 asti että ehto 3 vastaa kysymykseen menikö teko perille**, koska
 * palvelin tyhjentää kokoamisen lähetyksen yhteydessä. Mittaus kumosi puolet väitteestä: se
 * tyhjenee myös pelkästä uudelleenhausta, joten eroava `move`-arvo ei erota lähetettyä
 * nollautuneesta. Ehto jää silti sellaisenaan, koska se erehtyy turvalliseen suuntaan eli
 * jättää lähettämättä. Kysymys ratkaistaan nyt ennen tätä funktiota:
 * [PendingAction.dependsOnAssembly] on tosi täsmälleen siinä tapauksessa jota ei voi ratkaista,
 * ja sellaista tekoa ei yritetä uudelleen lainkaan.
 */
fun BoardState.stillOffers(action: PendingAction): Boolean {
    val form = this.form ?: return false
    return action.submit in form.submits && form.pendingMove == action.pendingMove
}
