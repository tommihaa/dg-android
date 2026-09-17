package fi.tommi.dg.domain

/**
 * Omat asetukset (`/bg/profile`).
 *
 * **Tämä on ainoa tähän mennessä jäsennetty sivu jota myös kirjoitetaan**, ja se muuttaa
 * virheen luonteen. Muualla väärin luettu sivu näyttää väärää tietoa, ja korjaus on uusi
 * haku. Täällä väärin luettu sivu **tuhoaa käyttäjän asetukset seuraavassa lähetyksessä**,
 * eikä alkuperäistä tilaa ole enää missään.
 *
 * Syy on lomakkeen muodossa: `/bg/profile/pref` lähettää kaikki kentät kerralla, ja
 * **rastittamaton valintaruutu ei lähde mukana lainkaan**. Palvelin ei siis erota "tätä ei
 * muutettu" ja "tämä otettiin pois" toisistaan. Kentän puuttuminen mallista on siksi sama
 * asia kuin asetuksen nollaaminen, ja se on syy siihen että malli kantaa **koko lomakkeen**
 * eikä vain sitä kenttää jota kulloinkin muutetaan.
 *
 * Sivu on aina kirjautuneen omat asetukset, toisin kuin [ProfilePage] joka voi olla kenen
 * tahansa. Osoitteessa ei ole käyttäjänumeroa lainkaan.
 */
/**
 * Asetuslomakkeen `action`.
 *
 * Vakio asuu domainissa eikä jäsentimessä siksi, että sitä käytetään kahdelta puolelta:
 * jäsennin rajaa sillä oikean lomakkeen sivun kolmesta, ja lähetys rajaa sillä ainoan
 * sallitun kohteen. Kaksi kopiota eriytyisi toisen muuttuessa, ja eriytyminen tarkoittaisi
 * tällä sivulla salasanalomakkeeseen osumista.
 */
const val SETTINGS_FORM_ACTION = "/bg/profile/pref"

data class SettingsPage(
    /** Kenen asetukset, luettu otsikosta. */
    val player: PlayerRef,
    /**
     * Lomakkeen oma metodi, luettu sivulta eikä valittu täällä.
     *
     * Sama sääntö kuin lautasivulla (22.8.2026): sivusto käyttää molempia, ja itse valittu
     * metodi lähettäisi kentät paikkaan josta palvelin ei niitä lue. Se ei olisi virhe vaan
     * teko joka ei mene perille, eli tällä sivulla asetus joka näyttää tallentuvan muttei
     * tallennu.
     */
    val method: FormMethod,
    /** Valintaruudut lomakkeen järjestyksessä. */
    val toggles: List<PreferenceToggle>,
    /**
     * Radioryhmät: kentän nimi -> valittu arvo (`order`, `color`, `board`).
     *
     * Ryhmä jolla ei ole valintaa **puuttuu kokonaan**, ja se on tarkoituksellista: null
     * arvona vaatisi kutsujaa muistamaan tarkistaa sen, kun taas puuttuva avain kaataa
     * lähetyksen kokoamisen siinä kohdassa jossa vika on. Ks. [radioGroupNames].
     */
    val radios: Map<String, String>,
    /**
     * Kaikki sivulla olleet radioryhmien nimet, myös ne joilla ei ollut valintaa.
     *
     * Tämä on olemassa vain [radios]in vertailukohdaksi. Ilman sitä valinnaton ryhmä olisi
     * erottamaton ryhmästä jota ei ole, ja lähetys jättäisi sen hiljaa pois: sama vika kuin
     * rastittamattomassa valintaruudussa, nyt vain toisessa kenttätyypissä.
     */
    val radioGroupNames: List<String>,
    /**
     * Radioryhmät selitteineen, **vain näyttämistä varten**.
     *
     * Tämä ei ole osa lähetyssopimusta eikä saa tulla siksi. Lähetys kootaan [checked]istä,
     * [radios]ista ja [radioGroupNames]ista, eli kentän nimistä ja arvoista, jotka ovat
     * sivuston omaa konetta. Selite on sivuston englantia ja voi muuttua sanamuodon mukana,
     * ja juuri siksi se kelpaa näytölle muttei avaimeksi: sama varaus kuin [nameFor]issa,
     * nyt toisessa kenttätyypissä.
     *
     * Olemassaolon syy on mitattu eikä kosmeettinen. Ryhmien arvot ovat paljaita numeroita
     * (`board` = `0`, `1`, `2`), joten ilman selitettä näyttö sanoisi käyttäjälle `board 1`.
     * Se on tieto jota hän ei voi verrata mihinkään: sivu itse sanoo `Blue/White`, eikä
     * numero esiinny sivustolla missään näkyvissä.
     *
     * Tyhjä lista on kelvollinen. Se tarkoittaa ettei sivulla ollut ryhmiä, ei sitä että
     * ne olisi luettu tyhjiksi.
     */
    val choices: List<PreferenceChoice> = emptyList(),
) {
    /** Rastitettujen valintaruutujen nimet. Tämä joukko lähetetään sellaisenaan takaisin. */
    val checked: Set<String> get() = toggles.filter { it.checked }.map { it.name }.toSet()

    /**
     * Kaikki lomakkeen kenttänimet, valintaruudut ja radioryhmät yhdessä.
     *
     * Tämä on olemassa lähetyksen kolmatta vartiota varten (`docs/ASETUKSET.md` luku 5).
     * Ruutu piirretään yhdestä luvusta ja lähetetään toisen jälkeen, ja niiden välissä sivu
     * on voinut muuttua. Kentät ovat paljaita numeroita, joten renumerointi osuisi muuten
     * hiljaa väärään ruutuun, ja koska lähetys koskee koko lomaketta, yksi väärä ruutu on
     * kaksi väärää asetusta.
     */
    val fieldNames: Set<String> get() = toggles.map { it.name }.toSet() + radioGroupNames

    /**
     * Valintaruudun nimi selitetekstin perusteella, tai null jos tekstiä ei ole sivulla.
     *
     * Nimet ovat paljaita numeroita (`0`..`7`) joilla ei ole merkitystä itsessään, joten
     * ainoa asetuksen tunnistava tieto sivulla on sen viereinen teksti. Numeroon
     * kovakoodattu asetus osuisi renumeroinnin jälkeen hiljaa väärään ruutuun, ja koska
     * lähetys koskee koko lomaketta, väärä ruutu tarkoittaa kahta väärää asetusta.
     *
     * Tämä ei silti ole vakaa avain: teksti on sivuston omaa englantia ja voi muuttua.
     * Kestävä tapa on **kysyä molemmat ja vaatia niiden täsmäävän**, jolloin kumpi tahansa
     * muutos tekee testistä punaisen sen sijaan että toinen niistä osuisi väärään ruutuun.
     */
    fun nameFor(label: String): String? =
        toggles.firstOrNull { it.label.equals(label, ignoreCase = true) }?.name
}

/**
 * Yksi radioryhmä selitteineen.
 *
 * Ryhmällä on kaksi eri nimeä eikä yksi, ja ne tulevat sivun eri kohdista: [name] on
 * lomakkeen kenttä (`board`) ja [label] sitä edeltävä otsikko (`Board Scheme`). Otsikko
 * on `<H4>` taulukon ulkopuolella, joten sen puuttuminen on tavallinen mahdollisuus eikä
 * virhe, ja silloin näyttö turvautuu kentän nimeen.
 *
 * @param name lomakkeen kentän nimi, sama avain kuin `SettingsPage.radios`issa
 * @param label ryhmän otsikko sivulla, tai null jos otsikkoa ei löytynyt
 * @param options ryhmän vaihtoehdot sivun järjestyksessä
 */
data class PreferenceChoice(
    val name: String,
    val label: String?,
    val options: List<ChoiceOption>,
) {
    /** Valitun vaihtoehdon selite, tai null jos ryhmässä ei ollut valintaa. */
    val selectedLabel: String? get() = options.firstOrNull { it.selected }?.label
}

/**
 * Yksi radiovaihtoehto.
 *
 * @param value lomakkeen arvo, sivustolla paljas numero
 * @param label vaihtoehdon selite viereisestä solusta, ainoa ihmiselle merkitystä kantava tieto
 * @param selected oliko tämä valittuna sivua haettaessa
 */
data class ChoiceOption(
    val value: String,
    val label: String,
    val selected: Boolean,
)

/**
 * Yksi asetusvalintaruutu.
 *
 * @param name lomakkeen kentän nimi, sivuston muodossa paljas numero
 * @param label ruudun vieressä oleva selite, ainoa merkitystä kantava tieto
 * @param checked oliko ruutu rastitettu sivua haettaessa
 */
data class PreferenceToggle(
    val name: String,
    val label: String,
    val checked: Boolean,
)
