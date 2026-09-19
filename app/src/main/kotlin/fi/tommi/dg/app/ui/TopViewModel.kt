package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.CredentialsStore
import fi.tommi.dg.app.session.MatchOrderStore
import fi.tommi.dg.domain.MatchOrder
import fi.tommi.dg.domain.MatchSortKey
import fi.tommi.dg.app.session.ListedRounds
import fi.tommi.dg.data.MatchMemory
import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.SeenMatch
import fi.tommi.dg.domain.TopPage
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.TopPageParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Otteluluettelon tila.
 *
 * `Loaded` kantaa myös [refreshing]-lipun sen sijaan että lataus olisi oma tilansa. Syy on
 * käytännöllinen: päivitys ei saa pyyhkiä listaa ruudulta, koska tyhjä ruutu näyttäisi
 * siltä ettei otteluita ole.
 */
sealed interface TopUiState {

    /** Tunnuksia ei ole tallennettu, tai ne eivät kelvanneet. */
    data class SignedOut(
        val lastAttemptFailed: Boolean = false,
        val busy: Boolean = false,
        /**
         * Salasanaa ei saatu suojattua laitteella. Eri asia kuin hylätty tunnus, ja siksi
         * eri kenttä: käyttäjä ei voi korjata tätä kirjoittamalla salasanan uudestaan.
         */
        val storageFailed: Boolean = false,
    ) : TopUiState

    data object Loading : TopUiState

    data class Loaded(
        val page: TopPage,
        val refreshing: Boolean = false,
        /**
         * Ottelurivit ovat tiedossa vanhentuneita, joten niitä ei piirretä tämän haun ajaksi
         * (Tommin päätös 4.9.2026).
         *
         * **Havainto:** *"ottelulistan tyhjennyttyä Match näyttö vaikuttaa levottomalta
         * lataillessaan ottelutietoja, joita ei pitäisi olla."* Kun viimeinen vuoro päättyy,
         * sivusto vastaa Top Pagella, lauta sulkeutuu ja luettelo haetaan tuoreena. Haun ajan
         * ruudulla säilyi edellinen lista, eli juuri ne ottelut jotka oli äsken pelattu.
         *
         * **Ero verkkovirheeseen on tieto eikä maku.** Vanha lista säilytetään kun päivitys
         * kaatuu, koska vanhentunut tieto on enemmän kuin ei mitään ([onFailure]). Tässä
         * sovellus tietää haun **alkaessa** että lista on vanhentunut, koska se hakee juuri
         * siksi että lauta kertoi ottelun päättyneen. Näkyvä lista olisi silloin väite jota
         * mikään ei tue.
         *
         * ~~Yläosa jää: nimi, odottavien rivi ja reunapäivä eivät ole ottelurivejä, eikä niiden
         * katoaminen tekisi ruudusta rauhallisempaa vaan tyhjemmän.~~ **Yläosa ei jää** (Tommin
         * päätös samana iltana, kun ensimmäinen versio oli nähty laitteella): *"en ole
         * tyytyväinen Matches-näytölle siirtymisen suhteen, ehkä tyhjä näyttö olisi parempi
         * kunnes ajantasaiset tiedot on haettu."* Ruutu on haun ajan sama kuin kylmässä
         * käynnistyksessä, eli keskitetty odotusmerkki tyhjällä alueella, ja arkiston reunapäivä
         * katoaa vaikka se tulee laitteen omasta kannasta. Ensimmäinen versio piirsi neljä
         * kohtaa ehdollisena ja se jätti ruudulle vaihtelevan yläosan; nyt tila on yksi eikä
         * neljän summa. Ks. [TopScreen] ja `docs/UI.md`.
         *
         * **Kolmas versio siirsi odotuksen pois tästä ruudusta, ja neljäs toi sen takaisin.**
         * Väliversiossa paluu laudalta odotti hakua, jotta luettelo ilmestyisi kerralla
         * täytettynä. Nauhoitus tabletilta näytti mitä se maksoi: odotusruutu oli laudan
         * reitillä ja joutui teeskentelemään tätä ruutua, ja erot löytyivät yksi kerrallaan
         * (sisältö, suunta, välilehtipalkki, otsikkorivi), päälle vielä navigoinnin
         * ristihäivytys kahden lähes samannäköisen ruudun välillä.
         *
         * **Odotus kuuluu siis tänne, koska kehys on täällä valmiina.** Ruutu piirtää
         * välilehtipalkin, otsikkorivin ja oman taustansa, ja vain sisältöalue vaihtuu
         * odotusmerkistä listaksi. Mitään ei tarvitse teeskennellä, eikä kehys liiku.
         */
        val matchesStale: Boolean = false,
    ) : TopUiState

    /**
     * Palvelin vastasi 200 OK:lla, mutta sivu ei ole otteluluettelo.
     *
     * **Oma tilansa eikä tyhjä lista**, ja se on tämän tilan koko olemassaolon syy. Ilman
     * sitä `TopPageParser` jäsentää minkä tahansa sivun, tulos on tyhjä `TopPage`, ja ruutu
     * sanoo `No matches.` Se on kelvollinen tila ja väärä väite sivustosta, eikä sitä voi
     * erottaa oikeasta vastauksesta katsomalla.
     *
     * Sama kaava kuin `BoardUiState.NotABoard`illa, joka syntyi 3.8.2026 samasta syystä.
     * Tälle ei ole `kind`-erittelyä, koska syitä ei tunneta yhtäkään: ainoa havaittu tapaus
     * (21.8.2026) jäi tunnistamatta, ja arvattu erittely olisi juuri sitä keksimistä jota
     * tämä tila estää.
     */
    data object NotATopPage : TopUiState

    data class Failed(val reason: Failure) : TopUiState
}

sealed interface Failure {
    data object Offline : Failure
    data class Server(val code: Int) : Failure

    /**
     * Sivusto nukkuu varmuuskopion ajan ([DgResponse.Sleeping]). Oma lajinsa, koska
     * teksti on eri: ei virhettä eikä koodia vaan sivuston oma kehotus palata puolen
     * tunnin päästä, ja koska katko on rutiini eikä häiriö (`SUBSTANSSI.md` kohta 49).
     */
    data object Sleeping : Failure
}

/**
 * Otteluluettelon näkymämalli.
 *
 * Yksi sääntö on tässä rakenteena: **sovellus hakee oma-aloitteisesti vain
 * [DgPages.TOP_PATH]in.** Se on pelkkä luku eikä kuluta mitään. `/bg/nextgame` on
 * kuluttava, ja siksi se ei esiinny tässä luokassa lainkaan, ei myöskään
 * käynnistyshaussa. `TopViewModelTest` vahtii tätä erikseen.
 */
class TopViewModel(
    private val pages: PageFetcher,
    private val credentials: CredentialsStore,
    private val matchOrder: MatchOrderStore,
    /**
     * Luettelon kierrokset laudalle, ks. [ListedRounds]. Kirjoitetaan jokaisesta onnistuneesti
     * jäsennetystä Top Pagesta; ei muuta tämän luokan pyyntöjä eikä tilaa.
     */
    private val listedRounds: ListedRounds = ListedRounds(),
    /**
     * Ottelumuisti kantaan, ks. [MatchMemory]. Kirjoitetaan samasta sivusta kuin
     * [listedRounds]: luettelo on ainoa sivu joka kertoo vastustajan ja kierroksen `n/m`
     * kaikista vuorossa olevista, joten se otetaan talteen kun se on käsillä. Null testeissä
     * joita muisti ei koske.
     */
    private val matchMemory: MatchMemory? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow<TopUiState>(
        if (credentials.hasCredentials()) TopUiState.Loading else TopUiState.SignedOut()
    )
    val state: StateFlow<TopUiState> = _state.asStateFlow()

    /**
     * Luettelon järjestys, oma virtansa eikä osa [TopUiState]a.
     *
     * Ero on tarkoituksellinen: järjestys on laitteen valinta, joka on olemassa myös ennen
     * ensimmäistä hakua ja epäonnistuneen haun jälkeen, kun taas [TopUiState] on sitä mitä
     * sivulta saatiin. Yhdistettynä järjestys olisi hävinnyt jokaisessa tilanvaihdossa.
     */
    private val _order = MutableStateFlow(matchOrder.get())
    val order: StateFlow<MatchOrder> = _order.asStateFlow()

    /**
     * Sarakeotsikon napautus: nouseva, laskeva, ja takaisin sivuston järjestykseen.
     *
     * Valinta kirjoitetaan levylle heti eikä ruudulta poistuttaessa, koska se on yhden
     * rivin kirjoitus ja koska poistumista ei ole: otteluluettelo on sovelluksen kotiruutu.
     */
    fun sortBy(key: MatchSortKey) {
        val next = _order.value.tapped(key)
        _order.value = next
        matchOrder.save(next)
    }

    init {
        // Haku käynnistyksessä on turvallinen vain siksi että kohde on Top Page.
        // Jos tähän joskus lisätään toinen osoite, tarkista ensin ettei se kuluta mitään.
        if (credentials.hasCredentials()) refresh()
    }

    fun signIn(login: String, password: String) {
        // Tunnus siivotaan reunavalilyonneista, salasana ei. Autofill on lisannyt tunnukseen
        // valilyonteja (DG-foorumi 4/2021) eika valilyonti voi olla tunnuksen aito osa,
        // mutta salasanassa se voi olla: siihen ei kosketa, koska hiljaa muutettu salasana
        // olisi pahempi vika kuin hylatty.
        val trimmedLogin = login.trim()
        if (trimmedLogin.isEmpty() || password.isEmpty()) {
            _state.value = TopUiState.SignedOut(lastAttemptFailed = true)
            return
        }
        if (!credentials.save(trimmedLogin, password)) {
            _state.value = TopUiState.SignedOut(storageFailed = true)
            return
        }
        _state.value = TopUiState.SignedOut(busy = true)
        // Erillistä login-kutsua ei tarvita: DgClient huomaa login-sivun ja kirjautuu itse.
        // Yksi tie sisään on vähemmän tiloja joissa voi mennä pieleen.
        load(isRefresh = false)
    }

    /**
     * Luettelon haku uudelleen.
     *
     * [matchesKnownStale] on tosi silloin kun kutsuja tietää että ruudulla olevat rivit ovat
     * vanhentuneita, eli lautaruudun palatessa Top Pagella. Ks. [TopUiState.Loaded.matchesStale].
     * Käsin painettu Refresh ei ole sellainen: silloin lista voi hyvinkin olla ajan tasalla,
     * ja sen pyyhkiminen olisi juuri se välähdys jota tämä poistaa.
     *
     * Palauttaa haun [Job]in. Kutsupaikat eivät tällä hetkellä odota sitä, mutta se on
     * palautettu 4.9.2026 alkaen, koska haun valmistuminen on ainoa hetki jonka kutsuja voi
     * haluta tietää eikä sitä saa tilasta ilman kyselysilmukkaa.
     */
    fun refresh(matchesKnownStale: Boolean = false): Job {
        val current = _state.value
        _state.value = when (current) {
            is TopUiState.Loaded -> current.copy(refreshing = true, matchesStale = matchesKnownStale)
            else -> TopUiState.Loading
        }
        return load(isRefresh = true)
    }

    /**
     * Ottaa käyttöön otteluluettelon jonka **joku muu on jo hakenut**.
     *
     * Käyttöpaikka on yksi: lautaruudun paluu, jossa sivusto vastasi laudan pyyntöön Top
     * Pagella ([NotABoardKind.TopPage]). Sivu on siis jo laitteella, ja ennen 4.9.2026 se
     * heitettiin pois ja sama sivu haettiin uudelleen. Nauhoitus mittasi odotukseksi noin 1,4
     * sekuntia, ja tämä poistaa sen kokonaan: luettelo on valmis sillä hetkellä kun lauta
     * sulkeutuu.
     *
     * **Yksi lukija ja yksi tila.** Tavut kulkevat saman [readTopPage]n läpi kuin oma haku,
     * joten tulos on sama `Loaded` tai `NotATopPage` eikä uutta tilalajia synny. Vain lähde
     * eroaa, eikä se näy tilassa.
     *
     * **Jäsennys on io:ssa** samasta syystä kuin [load]issa, joten tämä on suspend eikä
     * tavallinen funktio. Kutsuja on jo korutiinissa.
     *
     * Palauttaa tiedon siitä kelpasiko sivu. `false` tarkoittaa että tavut eivät olleet
     * otteluluettelo, ja silloin kutsujan kuuluu hakea sivu tavalliseen tapaan: tämä ei
     * kirjoita `NotATopPage`a ruudulle sivusta jota käyttäjä ei pyytänyt.
     */
    suspend fun adopt(html: String): Boolean {
        val next = withContext(io) { readTopPage(html) }
        if (next !is TopUiState.Loaded) return false
        _state.value = next
        return true
    }

    /**
     * Yhteys palasi. Haku uusitaan **vain jos edellinen kaatui verkkoon**.
     *
     * Kolme rajausta, ja kaikki kolme ovat tarkoituksellisia. Ehto on tila eikä ilmoitus:
     * ilman sitä jokainen wifin heilahdus hakisi sivun uudelleen myös silloin kun ruudulla
     * on ajantasainen lista, ja kohteen pyyntötahti on säästettävää tavaraa
     * (`docs/KOHDE.md`). `Failure.Server` jätetään ulkopuolelle, koska palvelimen virhe ei
     * korjaannu siitä että laitteella on taas yhteys. Ja `NotATopPage` jätetään ulkopuolelle
     * samasta syystä: sivu tuli perille, joten vika ei ole verkossa.
     *
     * **`Failure.Sleeping` jätetään ulkopuolelle Tommin päätöksellä 11.9.2026** (*"Refresh
     * riittää, ei automaattista päivitystä"*). Nukkuminen ei ole verkon tila, eikä sen
     * päättymiselle ole signaalia jota kuunnella; ajastettu uusinta olisi arvaus katkon
     * kestosta, joka on mitattu kerran (alle 10 min) ja sivun mukaan puoli tuntia.
     *
     * Käsin painettu Refresh ei muutu miksikään, ja se on tässä se vara joka jää: jos ehto
     * on liian tiukka, käyttäjä pääsee silti eteenpäin yhdellä painalluksella.
     */
    fun onNetworkAvailable() {
        val current = _state.value
        if (current is TopUiState.Failed && current.reason is Failure.Offline) refresh()
    }

    /**
     * Sovellus palasi etualalle ja otteluluettelo on auki. Haku uusitaan **vain jos lista on
     * jo ruudulla eikä haku ole kesken** (Tommin tilaus 18.9.2026, monen pelisession
     * havainto: *"kun vaihtaa takaisin sovellukseen ja Matches-näkymä on auki, niin
     * ottelulistan tulisi päivittyä"*, kuten DG Mobilessa).
     *
     * Rajaus on tarkoituksellinen ja sama kuin [onNetworkAvailable]issa toisin päin.
     * `Loaded` on ainoa tila jossa ruudulla on vanhentuvaa tietoa; `Loading` on jo hakemassa,
     * `SignedOut` ei voi hakea ja `Failed` jätetään virheelleen, koska palvelinvirhe tai
     * nukkuminen ei korjaannu siitä että sovellus vaihdettiin näkyviin (Sleeping on Tommin
     * päätös 11.9.2026, verkkokatkon hoitaa [onNetworkAvailable]). Rivit pidetään ruudulla
     * haun ajan (`matchesStale = false`), koska lista voi hyvinkin olla ajan tasalla ja
     * tyhjennys olisi välähdys.
     *
     * Kohde on Top Page, joka ei kuluta jonoa (`docs/KOHDE.md`), joten haku on turvallinen
     * ilman painallusta. Käynnistys ei kulje tästä: [init] hakee jo, ja ensimmäinen
     * etualalletulo suodatetaan kutsupaikassa.
     */
    fun onForeground() {
        val current = _state.value
        if (current is TopUiState.Loaded && !current.refreshing) refresh()
    }

    /**
     * Viestit on luettu: jonon kärjestä tuli lauta (Tommin tilaus 18.9.2026, *"en halua
     * dialogia jos ei ole viestejä"*). Luettelon ilmoitus *Something is waiting* on
     * viimeisen haun tila, ja se jäi näkyviin senkin jälkeen kun `Take an item` oli lukenut
     * ainoan viestin (sessio-18-9-ilta3). Lauta jonossa tarkoittaa että viestejä ei enää
     * ole sen edellä, koska sivusto tarjoilee kohteet järjestyksessä, joten ilmoitus
     * pyyhitään ilman uutta hakua. Polku katoaa samalla, ja siitä seuraa että viestiruudun
     * nappi katoaa (`MessagesViewModel` seuraa tätä virtaa). Seuraava haku lukee merkin
     * sivulta uudelleen, joten tämä ei peitä uutta viestiä.
     */
    fun clearMessageNotice() {
        val current = _state.value
        if (current is TopUiState.Loaded && current.page.messageQueuePath != null) {
            _state.value = current.copy(page = current.page.copy(messageQueuePath = null))
        }
    }

    fun signOut() {
        credentials.clear()
        _state.value = TopUiState.SignedOut()
    }

    private fun load(isRefresh: Boolean): Job =
        viewModelScope.launch {
            val response = withContext(io) { pages.fetch(DgPages.TOP_PATH) }
            _state.value = when (response) {
                // Jäsennys on io:ssa samasta syystä kuin BoardViewModelissa: TopPageParser
                // on Jsoup.parse plus taulukkokohtainen rivien läpikäynti, eikä se ole
                // Mainin työtä. Main saa vain valmiin muuttumattoman TopPagen.
                is DgResponse.Ok -> withContext(io) { readTopPage(response.html) }
                is DgResponse.Offline -> onFailure(isRefresh, Failure.Offline)
                is DgResponse.ServerError -> onFailure(isRefresh, Failure.Server(response.code))
                DgResponse.Sleeping -> onFailure(isRefresh, Failure.Sleeping)
                // Tunnukset jäävät tallessa vain jos ne kelpasivat. Kelpaamaton tunnus
                // poistetaan, jottei istunnon hiljainen uusiminen jää yrittämään sitä.
                DgResponse.AuthFailed -> {
                    credentials.clear()
                    TopUiState.SignedOut(lastAttemptFailed = true)
                }
            }
        }

    /**
     * **Sivu tarkistetaan ennen jäsentämistä.** Järjestys on merkitsevä: jäsennin ei voi
     * kertoa epäonnistuneensa, koska tyhjä tulos on sille kelvollinen tulos.
     *
     * Tuntematon sivu **korvaa näkyvän listan** eikä säilytä sitä, toisin kuin verkkovirhe
     * ([onFailure]). Ero on tarkoituksellinen ja se on sama valinta kuin lautaruudulla:
     * verkkovirheessä vanha lista on vanhentunutta mutta oikeaa tietoa, tässä sen
     * säilyttäminen piilottaisi juuri sen tapauksen jota varten koko tarkistus on olemassa.
     */
    private fun readTopPage(html: String): TopUiState =
        TopPageParser.parse(html)
            ?.also { listedRounds.remember(it.matches) }
            ?.also { rememberMatches(it.matches) }
            ?.let { TopUiState.Loaded(it) }
            ?: TopUiState.NotATopPage

    /**
     * Luettelon rivit ottelumuistiin. Oma korutiini eikä osa hakua: kirjoitus kantaan ei saa
     * viivyttää listan piirtymistä eikä sen epäonnistuminen kaataa sivua.
     */
    private fun rememberMatches(matches: List<Match>) {
        val memory = matchMemory ?: return
        val seenAt = now()
        val seen = matches.map { match ->
            SeenMatch(
                matchId = match.id,
                opponent = match.opponent,
                round = match.round?.takeIf { it.isNotBlank() && it != "-" },
                matchLength = match.matchLength,
                eventName = match.eventName.takeIf { it.isNotBlank() },
                seenAtEpochMillis = seenAt,
            )
        }
        viewModelScope.launch(io) { memory.remember(seen) }
    }

    /**
     * Epäonnistunut päivitys ei saa hävittää jo näkyvää listaa. Vanha lista on
     * vanhentunutta mutta oikeaa tietoa, ja virheruutu sen tilalla olisi vähemmän.
     */
    private fun onFailure(isRefresh: Boolean, reason: Failure): TopUiState {
        val current = _state.value
        return if (isRefresh && current is TopUiState.Loaded) {
            // **Vanhentuneet rivit palaavat näkyviin kun haku kaatuu**, eli lippu nollataan
            // tässä. Piilotus kestää haun ajan eikä pidempään: epäonnistuneen haun jälkeen
            // vanha lista on taas enemmän kuin tyhjä ruutu, ja se on myös ainoa tie
            // eteenpäin ilman uutta hakua. Ilman nollausta rivit jäisivät piiloon siihen
            // asti että jokin haku onnistuu. Ks. [TopUiState.Loaded.matchesStale].
            current.copy(refreshing = false, matchesStale = false)
        } else {
            TopUiState.Failed(reason)
        }
    }

    class Factory(
        private val pages: PageFetcher,
        private val credentials: CredentialsStore,
        private val matchOrder: MatchOrderStore,
        private val listedRounds: ListedRounds,
        private val matchMemory: MatchMemory? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TopViewModel(pages, credentials, matchOrder, listedRounds, matchMemory) as T
    }
}
