package fi.tommi.dg.domain

/**
 * Asetuslomakkeen lähetyksen kokoaminen ja sen vartiot.
 *
 * **Tämä on olemassa siksi, että lähetys on koko lomake eikä muutos.** Rastittamaton
 * valintaruutu ei lähde mukana lainkaan, joten palvelin ei erota kohtia "tätä ei muutettu"
 * ja "tämä otettiin pois". Yksi puuttuva kenttä on siis nollattu asetus, eikä alkuperäistä
 * tilaa ole sen jälkeen enää missään (ks. [SettingsPage]).
 *
 * **Sijainti on osa taetta eikä makuasia.** [FormSubmission]in konstruktori on tämän
 * moduulin sisäinen, joten arvon voi tuottaa vain täällä oleva funktio. Lähettäjä
 * (`FormSender`) ottaa vain sen tyypin, eikä kutsupaikka voi koota asetuslähetystä itse sen
 * enempää kuin lautasivun nappia. Tämä on kolmas tuottaja `BoardForm.press`in ja
 * `ReplyForm.write`n rinnalle.
 *
 * Logiikka asui 3.8.2026 alkaen live-testin lähteessä (`core-net`in `LiveSettings`), koska
 * kiertokoe tarvitsi sen ennen kuin ruutua oli. Siirto tänne on `docs/ASETUKSET.md` luvun 5
 * ehto: yksi kopio pysyy yhtenä kopiona, ja live-testi kutsuu tuotantokoodia eikä omaansa.
 */
sealed interface SettingsUpdate {

    /**
     * Lähetys on koottu ja vartiot läpäisty.
     *
     * [checked] ja [radios] ovat mukana siksi, että paluuluku on verrattava täsmälleen
     * siihen mitä lähetettiin. Kutsupaikan oma muistikuva lähetetystä tilasta olisi toinen
     * kopio samasta tiedosta, ja juuri sen eriytyminen on tällä sivulla se vika jota
     * mikään ei näyttäisi.
     */
    data class Ready(
        val submission: FormSubmission,
        val checked: Set<String>,
        val radios: Map<String, String>,
    ) : SettingsUpdate

    /** Yhteinen yläkäsite niille tuloksille joissa mitään ei lähetetä. */
    sealed interface Blocked : SettingsUpdate

    /**
     * Radioryhmällä ei ole valintaa, ja lähetys nollaisi sen.
     *
     * Sama vika kuin rastittamattomassa valintaruudussa, nyt toisessa kenttätyypissä.
     * Pysäytys tapahtuu ennen verkkoa eikä paluuluvun vertailussa, koska siinä vaiheessa
     * asetus olisi jo mennyt.
     */
    data class MissingRadioGroups(val groups: Set<String>) : Blocked

    /** Rastitettavaksi pyydettyä valintaruutua ei ole lomakkeella. */
    data class UnknownToggles(val names: Set<String>) : Blocked

    /**
     * Radioryhmälle on annettu arvo vaikka ryhmää ei ole lomakkeella.
     *
     * Tämä on [UnknownToggles]in pari toisessa kenttätyypissä. Ilman sitä tuntematon ryhmä
     * lähtisi mukana ylimääräisenä kenttänä, eikä kutsupaikka saisi tietää että se osoitti
     * asetukseen jota ei ole.
     */
    data class UnknownRadioGroups(val groups: Set<String>) : Blocked

    /**
     * Lomake ei ole enää se josta ruutu piirrettiin.
     *
     * Kenttien nimet ovat paljaita numeroita, joten renumeroinnin jälkeen sama numero
     * tarkoittaisi eri asetusta. Lähetys koskee koko lomaketta, joten yksi siirtynyt ruutu
     * on kaksi väärää asetusta. Ainoa turvallinen tulos on olla lähettämättä ja pyytää
     * ruudun päivitystä.
     */
    data class FormChanged(val drawnFrom: Set<String>, val latest: Set<String>) : Blocked
}

/**
 * Kokoaa asetuslomakkeen lähetyksen.
 *
 * Vastaanottaja on se sivu **josta ruutu piirrettiin**, ja [latest] se sivu joka luettiin
 * juuri ennen lähetystä. Kaksi lukemaa eikä yksi on `docs/ASETUKSET.md` luvun 5 kolmas
 * vartio: ruudun piirtämisen ja napin painamisen välissä voi olla mitä tahansa, myös
 * selaimessa tehty muutos tai sivuston oma muutos.
 *
 * Metodi otetaan [latest]ista, koska se on se sivu jolle lähetetään.
 *
 * @param checked rastitettavat valintaruudut kokonaisuudessaan, ei muutos
 * @param radios radioryhmien valinnat kokonaisuudessaan, ei muutos
 */
fun SettingsPage.update(
    latest: SettingsPage,
    checked: Set<String> = latest.checked,
    radios: Map<String, String> = latest.radios,
): SettingsUpdate {
    // Kenttäjoukko ensin: kaikki muut tarkistukset mittaavat annettua tilaa lomaketta
    // vasten, ja väärää lomaketta vasten mitattu tulos olisi oikean näköinen mutta väärä.
    if (fieldNames != latest.fieldNames) {
        return SettingsUpdate.FormChanged(drawnFrom = fieldNames, latest = latest.fieldNames)
    }

    val missing = latest.radioGroupNames.toSet() - radios.keys
    if (missing.isNotEmpty()) return SettingsUpdate.MissingRadioGroups(missing)

    val unknownGroups = radios.keys - latest.radioGroupNames.toSet()
    if (unknownGroups.isNotEmpty()) return SettingsUpdate.UnknownRadioGroups(unknownGroups)

    val unknownToggles = checked - latest.toggles.map { it.name }.toSet()
    if (unknownToggles.isNotEmpty()) return SettingsUpdate.UnknownToggles(unknownToggles)

    // Rasti lähtee arvolla `on`, joka on selaimen oma muoto arvottomalle valintaruudulle.
    // Rastittamaton ei lähde lainkaan, ja se on lomakkeen sopimus eikä tämän valinta.
    val fields = radios + checked.associateWith { "on" }
    return SettingsUpdate.Ready(
        submission = FormSubmission(
            action = SETTINGS_FORM_ACTION,
            method = latest.method,
            fields = fields,
        ),
        checked = checked,
        radios = radios,
    )
}
