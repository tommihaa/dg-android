package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.SettingsBaseline
import fi.tommi.dg.app.session.SettingsSnapshot
import fi.tommi.dg.app.session.SiteSettingsStore
import fi.tommi.dg.domain.SettingsPage
import fi.tommi.dg.domain.SettingsUpdate
import fi.tommi.dg.domain.siteBoardSettings
import fi.tommi.dg.domain.update
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.SettingsParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Asetusnäkymän tila.
 *
 * `Loaded` kantaa [SettingsUiState.Loaded.refreshing]-lipun samasta syystä kuin kaksi muuta
 * näkymämallia: päivitys ei saa pyyhkiä jo luettuja asetuksia ruudulta.
 */
sealed interface SettingsUiState {

    data object Loading : SettingsUiState

    data class Loaded(
        val page: SettingsPage,
        val refreshing: Boolean = false,
    ) : SettingsUiState

    /**
     * Sivu saapui 200 OK:lla muttei ollut asetussivu.
     *
     * Oma tilansa eikä [Failed], samasta syystä kuin `BoardUiState.NotABoard`: verkkovirheen
     * voi yrittää uudelleen, mutta väärä sivu tarkoittaa että tilanne on toinen kuin luultiin.
     *
     * Tämä tila on tässä ruudussa pakollinen eikä varmuuden vuoksi lisätty. Syy on se mitä
     * tyhjä tulos tarkoittaisi: se on erottamaton asetussivusta jolla ei ole yhtään
     * asetusta, ja ruutu väittäisi silloin että kaikki asetukset ovat pois päältä.
     *
     * **Korjattu kuvaus 4.9.2026.** Tässä luki että `SettingsParser.parse` palauttaa aina
     * sivun myös roskasta, ja se lakkasi pitämästä paikkansa 1.9.2026 (H3): jäsennin
     * tunnistaa sivun itse ja palauttaa nullin kun sivu ei ole asetussivu. Tila ei siis ole
     * kutsupaikan lisäportti vaan jäsentimen oma vastaus luettuna.
     */
    data object NotSettingsPage : SettingsUiState

    data class Failed(val reason: Failure) : SettingsUiState

    /** Istunto ei kelvannut. Tunnusten poisto kuuluu [TopViewModel]ille. */
    data object SessionExpired : SettingsUiState, SessionExpiredState
}

/**
 * Yhden tallennusyrityksen lopputulos.
 *
 * Neljä haaraa eikä kaksi, koska niillä on eri teksti ja eri seuraus käyttäjälle
 * (`docs/ASETUKSET.md` luku 5). Vaarallisin on [Mismatch]: lähetys lähti, mutta paluuluku ei
 * täsmää siihen mitä lähetettiin, eli sivustolla voi nyt olla tila jota kukaan ei pyytänyt.
 * Se on ainoa haara jossa käyttäjää pyydetään tarkistamaan selaimella.
 */
sealed interface SettingsSaveResult {

    /** Paluuluku täsmäsi lähetettyyn kenttä kentältä. Vasta tämä muuttaa ruudun tilan. */
    data object Saved : SettingsSaveResult

    /** Lähetys ei lähtenyt. Sivustolla ei muuttunut mitään. */
    data class NotSent(val reason: Failure) : SettingsSaveResult

    /** Lähti, mutta paluuluku on eri kuin lähetetty. */
    data object Mismatch : SettingsSaveResult

    /**
     * Vastaus saapui muttei ollut asetussivu, joten lähetystä ei koottu.
     *
     * Oma haaransa eikä palvelinvirhe: [Failure.Server] kantaa koodin, ja tässä sellaista
     * ei ole. Keksitty luku olisi mittaamaton väite.
     */
    data object WrongPage : SettingsSaveResult

    /**
     * Istunto ei kelvannut kesken tallennuksen.
     *
     * **Ei [SessionExpiredState]ia**, samasta syystä kuin `ReplyUiState.SessionExpired`illa:
     * merkintä tarkoittaisi uloskirjausta ja ruudun sulkemista, ja se veisi lomakkeen
     * muutokset joita ei ole vielä tallennettu. Ruudun oma [SettingsUiState.SessionExpired]
     * kantaa merkinnän, ja se laukeaa kun ruutu itse haetaan uudelleen. Sääntö on
     * [SessionExpiredState]in dokumentaatiossa.
     */
    data object SessionExpired : SettingsSaveResult

    /**
     * Vartio pysäytti ennen verkkoa. Mitään ei lähetetty.
     *
     * Yleisin syy on [SettingsUpdate.FormChanged], eli lomake ei ole enää se josta ruutu
     * piirrettiin. Silloin oikea teko on päivittää ruutu eikä yrittää uudelleen.
     */
    data class Blocked(val reason: SettingsUpdate.Blocked) : SettingsSaveResult
}

/**
 * Asetusnäkymän näkymämalli.
 *
 * **Luki ja vain luki 25.8.2026 asti, ja sopimusmuutos on kirjattu ja vahvistettu**
 * (`docs/UI.md`). Malli saa nyt [FormSender]in, eli se voi lähettää asetuslomakkeen. Sama
 * järjestys kuin `MessagesViewModel`illa 22.8.2026: luettelo muuttui, periaate ei.
 *
 * **Mikä ei muuttunut, ja se on tämän luokan tärkein osa.** Tämä luokka **ei ota polkua
 * lainkaan**: luettava kohde on vakio [DgPages.SETTINGS_PATH] ja kirjoitettava kohde tulee
 * `SettingsPage.update`ista, joka on ainoa tapa tuottaa `FormSubmission` tälle lomakkeelle.
 * Muuta osoitetta ei voi pyytää kutsumalla tätä väärin, koska sellaista parametria ei ole.
 * Kirjoitusoikeus laajensi siis sitä kuka saa painaa lähetystä, ei sitä minne voi kirjoittaa.
 *
 * **Kolme lukua yhtä tallennusta kohti, eikä se ole varovaisuutta.** Lomake lähtee
 * kokonaisena, joten yksi väärin luettu kenttä on menetetty asetus jonka alkuperäistä tilaa
 * ei ole enää missään. Siksi [save] lukee sivun uudestaan ennen lähetystä (vartio vertaa
 * kenttäjoukkoa siihen josta ruutu piirrettiin) ja vielä kerran sen jälkeen (paluuluku
 * verrataan lähetettyyn). Ilman jälkimmäistä epäonnistunut lähetys näyttäisi onnistuneelta.
 */
class SettingsViewModel(
    private val pages: PageFetcher,
    private val forms: FormSender,
    private val baseline: SettingsBaseline,
    /**
     * Laitteen kopio lautaan vaikuttavista asetuksista. Jokainen onnistunut asetussivun
     * luku päivittää säilön, jotta tässä ruudussa tehty muutos näkyy laudalla heti eikä
     * vasta seuraavassa avauksessa.
     *
     * **Pakollinen 1.9.2026 alkaen (auditoinnin H6).** Tämä oli `SiteSettingsStore? = null`,
     * eli kytkennän unohtaminen olisi kääntynyt ja oireillut vain siten että lauta piirtyisi
     * vanhoilla asetuksilla. Oletus oli testejä varten, ja testit saavat nyt oman säilön:
     * hiljaisen vanhentumisen mahdollisuutta ei kannata ostaa sillä että testissä on yksi
     * argumentti vähemmän.
     */
    private val siteSettings: SiteSettingsStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /** Erotettu testattavuuden vuoksi, samoin kuin tunnusten säilön avaimen poisto. */
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /**
     * Viimeisimmän tallennusyrityksen tulos, `null` kun yritystä ei ole tai se on kuitattu.
     *
     * Erillään [state]sta tarkoituksella: tämä on kertaluontoinen ilmoitus siitä mitä
     * tapahtui, kun taas [state] on se mitä sivustolla nyt lukee. Yhdistettyinä
     * epäonnistunut lähetys pyyhkisi ruudulta asetukset joita se ei muuttanut.
     */
    private val _saveResult = MutableStateFlow<SettingsSaveResult?>(null)
    val saveResult: StateFlow<SettingsSaveResult?> = _saveResult.asStateFlow()

    /** Lähtötila jos se on otettu talteen. Ruutu tarjoaa siitä palautuksen. */
    val storedBaseline: SettingsSnapshot? get() = baseline.get()

    init {
        load(isRefresh = false)
    }

    /** Päivitys vain käyttäjän pyynnöstä. Ei kyselyä, ei ajastusta, ei ennakointia. */
    fun refresh() {
        val current = _state.value
        _state.value = when (current) {
            is SettingsUiState.Loaded -> current.copy(refreshing = true)
            else -> SettingsUiState.Loading
        }
        load(isRefresh = true)
    }

    private fun load(isRefresh: Boolean) {
        viewModelScope.launch {
            // Jäsennys samassa lohkossa kuin haku: sivulla on kolme lomaketta ja
            // kolmisenkymmentä kenttää, eli enemmän työtä kuin Mainille kuuluu.
            _state.value = withContext(io) {
                when (val response = pages.fetch(DgPages.SETTINGS_PATH)) {
                    is DgResponse.Ok -> readSettings(response.html)
                    is DgResponse.Offline -> onFailure(isRefresh, Failure.Offline)
                    is DgResponse.ServerError -> onFailure(isRefresh, Failure.Server(response.code))
                    DgResponse.Sleeping -> onFailure(isRefresh, Failure.Sleeping)
                    // Tunnuksia ei poisteta täällä: CredentialsStoren omistaa TopViewModel,
                    // ja kahdesta poistajasta seuraisi kaksi totuutta samasta asiasta.
                    DgResponse.AuthFailed -> SettingsUiState.SessionExpired
                }
            }
        }
    }

    /**
     * Väärä sivu on null eikä tyhjä tulos: jäsennin tunnistaa sivun itse (`DgPages`).
     */
    private fun readSettings(html: String): SettingsUiState {
        val page = SettingsParser.parse(html) ?: return SettingsUiState.NotSettingsPage
        // Lähtötila talteen ensimmäisestä onnistuneesta luvusta, ei myöhemmistä. Myöhempi
        // lukema on jo sen jälkeinen tila että sovellus on voinut kirjoittaa, joten se
        // tekisi varmuuskopiosta kopion mahdollisesta virheestä.
        baseline.captureOnce(page.checked, page.radios, clock())
        // Laitteen kopio sen sijaan päivittyy joka luvusta: se ei ole varmuuskopio vaan
        // lautanäkymän lähde, ja tuorein onnistunut luku on sen totuus.
        siteSettings.save(page.siteBoardSettings())
        return SettingsUiState.Loaded(page)
    }

    /**
     * Tallentaa koko lomakkeen.
     *
     * Parametrit ovat **koko tila eivätkä muutos**, koska osittaista lähetystä ei ole
     * olemassa: rastittamaton ruutu ei lähde mukana lainkaan, joten palvelin ei erota
     * kohtia "tätä ei muutettu" ja "tämä otettiin pois".
     *
     * Ruudun näyttämä tila muuttuu vasta täsmäävästä paluuluvusta. Epäonnistuneessa
     * tapauksessa ruutu jää ennalleen ja lopputulos kerrotaan dialogina, sama muoto kuin
     * laudalla 24.8.2026.
     */
    fun save(checked: Set<String>, radios: Map<String, String>) {
        val drawnFrom = (_state.value as? SettingsUiState.Loaded)?.page ?: return
        _saveResult.value = null
        viewModelScope.launch {
            val result = withContext(io) { send(drawnFrom, checked, radios) }
            _saveResult.value = result
        }
    }

    /** Kuittaa dialogin. Tulos on kertaluontoinen ilmoitus eikä ruudun pysyvä tila. */
    fun clearSaveResult() {
        _saveResult.value = null
    }

    private fun send(
        drawnFrom: SettingsPage,
        checked: Set<String>,
        radios: Map<String, String>,
    ): SettingsSaveResult {
        // Luku ennen lähetystä. Ruudun piirtämisen ja napin painamisen välissä voi olla mitä
        // tahansa, myös selaimessa tehty muutos.
        val latest = when (val fresh = readPage()) {
            is Read.Ok -> fresh.page
            is Read.Failed -> return fresh.result
        }

        val update = drawnFrom.update(latest, checked = checked, radios = radios)
        if (update !is SettingsUpdate.Ready) {
            return SettingsSaveResult.Blocked(update as SettingsUpdate.Blocked)
        }

        when (val response = forms.send(update.submission)) {
            is DgResponse.Ok -> Unit
            is DgResponse.Offline -> return SettingsSaveResult.NotSent(Failure.Offline)
            is DgResponse.ServerError ->
                return SettingsSaveResult.NotSent(Failure.Server(response.code))
            DgResponse.Sleeping ->
                return SettingsSaveResult.NotSent(Failure.Sleeping)
            DgResponse.AuthFailed -> return SettingsSaveResult.SessionExpired
        }

        // Luku lähetyksen jälkeen. Ilman tätä epäonnistunut lähetys näyttäisi onnistuneelta,
        // ja se on tällä sivulla väärä johtopäätös eikä virhe: asetus vain ei ole se mikä
        // ruudulla lukee.
        val after = when (val verify = readPage()) {
            is Read.Ok -> verify.page
            // Lähetys lähti mutta paluulukua ei saatu. Sitä ei saa lukea onnistumiseksi
            // eikä epäonnistumiseksi, ja Mismatch on näistä se joka pyytää tarkistamaan.
            is Read.Failed -> return SettingsSaveResult.Mismatch
        }

        if (after.checked != update.checked || after.radios != update.radios) {
            return SettingsSaveResult.Mismatch
        }

        _state.value = SettingsUiState.Loaded(after)
        return SettingsSaveResult.Saved
    }

    private sealed interface Read {
        data class Ok(val page: SettingsPage) : Read
        data class Failed(val result: SettingsSaveResult) : Read
    }

    private fun readPage(): Read = when (val response = pages.fetch(DgPages.SETTINGS_PATH)) {
        is DgResponse.Ok ->
            // Väärä sivu on oma haaransa eikä palvelinvirhe: keksitty koodi olisi luku jota
            // kukaan ei mitannut, ja tilanne on eri kuin verkkovirhe. Väärästä sivusta luettu
            // tyhjä tulos lähetettynä nollaisi kaikki asetukset, ja juuri siksi lukija
            // kieltäytyy sen sijaan että palauttaisi tyhjän (`DgPages`).
            SettingsParser.parse(response.html)?.let { page ->
                // Sama sääntö kuin readSettingsissä: jokainen onnistunut luku päivittää
                // laitteen kopion, myös lähetystä ympäröivät kaksi.
                siteSettings.save(page.siteBoardSettings())
                Read.Ok(page)
            } ?: Read.Failed(SettingsSaveResult.WrongPage)

        is DgResponse.Offline -> Read.Failed(SettingsSaveResult.NotSent(Failure.Offline))
        is DgResponse.ServerError ->
            Read.Failed(SettingsSaveResult.NotSent(Failure.Server(response.code)))
        DgResponse.Sleeping ->
            Read.Failed(SettingsSaveResult.NotSent(Failure.Sleeping))

        DgResponse.AuthFailed -> Read.Failed(SettingsSaveResult.SessionExpired)
    }

    /**
     * Epäonnistunut päivitys ei saa hävittää jo näkyviä asetuksia. Vanha lukema on
     * vanhentunutta mutta oikeaa tietoa, virheruutu ei ole mitään.
     */
    private fun onFailure(isRefresh: Boolean, reason: Failure): SettingsUiState {
        val current = _state.value
        return if (isRefresh && current is SettingsUiState.Loaded) {
            current.copy(refreshing = false)
        } else {
            SettingsUiState.Failed(reason)
        }
    }

    class Factory(
        private val pages: PageFetcher,
        private val forms: FormSender,
        private val baseline: SettingsBaseline,
        private val siteSettings: SiteSettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(pages, forms, baseline, siteSettings) as T
    }
}
