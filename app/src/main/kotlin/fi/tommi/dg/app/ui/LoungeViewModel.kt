package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.PlayerRatingStore
import fi.tommi.dg.app.session.LoungeFoldStore
import fi.tommi.dg.app.session.LoungeFolds
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.domain.LoungeInvitation
import fi.tommi.dg.domain.LoungePage
import fi.tommi.dg.domain.LoungeTournament
import fi.tommi.dg.domain.PlayerList
import fi.tommi.dg.domain.PlayerListLink
import fi.tommi.dg.domain.PlayerSearchForm
import fi.tommi.dg.domain.TournamentHall
import fi.tommi.dg.domain.cancelSignup
import fi.tommi.dg.domain.join
import fi.tommi.dg.domain.signUp
import fi.tommi.dg.domain.write
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.LoungeParser
import fi.tommi.dg.scrape.PlayerListParser
import fi.tommi.dg.scrape.TournamentHallParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Game Loungen tila. Sama kaava kuin [TopUiState]: `Loaded` kantaa [LoungeUiState.Loaded.refreshing]-lipun,
 * jottei päivitys pyyhi listaa ruudulta.
 */
sealed interface LoungeUiState {

    /**
     * Loungen polkua ei ole vielä luettu. Polku tulee Top Pagen navigointipalkista,
     * joten tämä tila tarkoittaa ettei otteluluettelo ole latautunut. Oma tilansa eikä
     * tyhjä lista, jotta ruutu voi sanoa syyn eikä väittää loungea tyhjäksi.
     */
    data object NoPath : LoungeUiState

    data object Loading : LoungeUiState

    data class Loaded(val page: LoungePage, val refreshing: Boolean = false) : LoungeUiState

    /**
     * Palvelin vastasi 200 OK:lla, mutta sivu ei ole lounge. Sama kaava ja sama syy kuin
     * [TopUiState.NotATopPage]lla: tyhjäksi jäsentynyt väärä sivu olisi väärä väite.
     */
    data object NotALoungePage : LoungeUiState

    data class Failed(val reason: Failure) : LoungeUiState

    /** Istunto ei kelvannut. Tunnusten poisto kuuluu [TopViewModel]ille. */
    data object SessionExpired : LoungeUiState, SessionExpiredState
}

/**
 * Yhden Join-lähetyksen lopputulos, erillään [LoungeUiState]ista samasta syystä kuin
 * [ReplyUiState] on erillään jonon tilasta: epäonnistunut lähetys ei saa pyyhkiä listaa.
 */
sealed interface JoinUiState {

    data object Idle : JoinUiState

    data object Joining : JoinUiState

    /** Vastaus tunnistettiin loungeksi ja lista on jo päivitetty siitä. */
    data object Joined : JoinUiState

    /**
     * Palvelin vastasi jotain muuta kuin loungea tai kuittaussivua. **Ei tulkita
     * kummaksikaan suunnaksi:** tässä ei tiedetä menikö hyväksyntä perille. Lounge
     * haetaan kerran uudelleen, ja käyttäjä näkee listasta katosiko kutsu.
     *
     * Tämä oli 31.8.2026 asti myös onnistuneen liittymisen tila, koska sivuston oma
     * kuittaussivu oli mittaamatta; nyt se on kaventunut aidosti tuntemattomaan.
     */
    data object Unconfirmed : JoinUiState

    /**
     * Lounge luettiin juuri ennen lähetystä, eikä sama tarjous ollut enää samalla
     * linkillä. **Mitään ei lähetetty** ja lista on päivitetty siitä luvusta.
     *
     * Syy on mitattu korpuksesta 27.9.2026: Joinin `id` on uudelleenkäytettävä paikka eikä
     * tarjouksen tunniste (`id=2` oli 3.9.2026 kanadaisukin 5 pisteen tarjous ja 14.9.2026
     * KAAPO41:n nack-tarjous), joten vanhentunut tai toistettu linkki voisi hyväksyä eri
     * ottelun. Tämä kattaa myös tuplajoinin: jo hyväksytty tarjous on poissa listalta.
     */
    data object Changed : JoinUiState

    /** Verkko tai palvelin. Ei automaattista uudelleenlähetystä: kaksi acceptia olisi kaksi ottelua. */
    data class Failed(val reason: Failure) : JoinUiState
}

/** Kumpi turnausrivin linkki painettiin. Kulkee tilan mukana, jotta ilmoitus osaa sanoa sen. */
enum class SignupAction { SignUp, Cancel }

/**
 * Yhden ilmoittautumis- tai peruutuslähetyksen lopputulos, erillään loungen tilasta samasta
 * syystä kuin [JoinUiState]. Tommin tilaus 3.9.2026, kaanonimuutos `SUBSTANSSI.md` kohtaan 52.
 *
 * **Onnistuminen luetaan loungen riviltä eikä vastaussivulta.** Sign Upin vastaus mitattiin
 * 3.9.2026 loungeksi jonka rivillä on jo Cancel Signup (`docs/KOHDE.md`); Cancelin vastausta
 * ei ole kaapattu. Sivun sanamuotoon ei nojata. Lounge luetaan lähetyksen jälkeen
 * (vastauksesta jos se on lounge, muuten yhdellä uudelleenhaulla) ja rivin linkki kertoo
 * itse kumpi tila on voimassa: Sign Upin jälkeen rivillä on `Cancel Signup`, peruutuksen
 * jälkeen `Sign Up`. Se on sama todiste jonka käyttäjä näkisi selaimessa, ja se on totta
 * riippumatta siitä mitä välisivu sanoi.
 */
sealed interface SignupUiState {

    data object Idle : SignupUiState

    data class Sending(val action: SignupAction) : SignupUiState

    /** Loungen rivi näyttää teon menneen läpi. [name] on turnauksen nimi ilmoitusta varten. */
    data class Done(val action: SignupAction, val name: String?) : SignupUiState

    /**
     * Loungea ei saatu luettua tai rivi ei näytä muutosta. Ei tulkita kumpaankaan
     * suuntaan, ja mitään ei lähetetä uudelleen.
     */
    data class Unconfirmed(val action: SignupAction) : SignupUiState

    /** Verkko tai palvelin. Mitään ei lähtenyt perille, eikä sitä yritetä uudelleen. */
    data class Failed(val action: SignupAction, val reason: Failure) : SignupUiState
}

/**
 * Pelaajalistan tila. Sama kaava kuin [LoungeUiState]lla.
 *
 * [NoPath] tarkoittaa tässä eri asiaa kuin loungessa: polku tulee **loungen omasta
 * linkistä**, joten se puuttuu niin kauan kuin lounge itse ei ole latautunut.
 */
/**
 * Tournament Hall loungen jaksona (21.9.2026 illalla; oli porautumissivu 3.9.–21.9.).
 * Sama tilamalli kuin pelaajalistalla: [NoPath] niin kauan kuin lounge ei ole antanut
 * hallin linkkiä tai jaksoa ei ole avattu.
 */
sealed interface HallUiState {

    data object NoPath : HallUiState

    data object Loading : HallUiState

    data class Loaded(val hall: TournamentHall, val refreshing: Boolean = false) : HallUiState

    /** 200 OK, mutta sivu ei ole Tournament Hall. */
    data object NotAHall : HallUiState

    data class Failed(val reason: Failure) : HallUiState
}

sealed interface PlayerListUiState {

    data object NoPath : PlayerListUiState

    data object Loading : PlayerListUiState

    data class Loaded(val list: PlayerList, val refreshing: Boolean = false) : PlayerListUiState

    /** 200 OK, mutta sivu ei ole pelaajalista. */
    data object NotAPlayerList : PlayerListUiState

    data class Failed(val reason: Failure) : PlayerListUiState
}

/**
 * Game Loungen näkymämalli.
 *
 * **Konstruktorissa on [FormSender], ja se on sopimusmuutos** (kirjattu `docs/UI.md`
 * 26.8.2026): tämä on kolmas näkymämalli joka toimii sivustolla. Ainoa teko on kutsun
 * hyväksyminen [join]illa, ja sen lähetys syntyy `LoungeInvitation.join`illa eli sivun
 * oman linkin painamisella; mitään ei koota.
 *
 * Sivun haku on pelkkä luku eikä kuluta mitään, joten se saa tapahtua itsestään kun polku
 * saapuu. Polku tulee virtana samasta syystä kuin [MessagesViewModel]illa: kertakuva
 * olisi väärä nolla aina kun ruutu avataan ennen otteluluettelon latautumista.
 */
class LoungeViewModel(
    private val pages: PageFetcher,
    private val forms: FormSender,
    loungePathUpstream: Flow<String?>,
    /**
     * Listojen taiton säilö (Tommin tilaus 21.9.2026). Valinta muistetaan laitteella,
     * joten se luetaan täältä käynnistyksessä ja kirjoitetaan jokaisesta vaihdosta.
     * Säilötön malli (testit) pitää tilan muistissa ja alkaa auki, kuten uusi asennus.
     */
    private val foldStore: LoungeFoldStore = MemoryFolds(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /** Pelaajalistan ratingit kirjataan tänne laudan korttia varten, ks. [PlayerRatingStore]. */
    private val ratings: PlayerRatingStore = PlayerRatingStore.NONE,
) : ViewModel() {

    private class MemoryFolds : LoungeFoldStore {
        private var stored = LoungeFolds()
        private var count: Int? = null
        override fun get(): LoungeFolds = stored
        override fun save(folds: LoungeFolds) {
            stored = folds
        }
        override fun hallCount(): Int? = count
        override fun saveHallCount(count: Int) {
            this.count = count
        }
    }

    private val _path = MutableStateFlow<String?>(null)

    private val _folds = MutableStateFlow(foldStore.get())

    /** Neljän jakson taitto: otsikko on aina näkyvissä, rivit vain auki ollessa. */
    val folds: StateFlow<LoungeFolds> = _folds.asStateFlow()

    private val _hallCount = MutableStateFlow(foldStore.hallCount())

    /**
     * Hallin aktiivisten turnausten määrä viime hausta, tai null ennen ensimmäistä hakua.
     * Otsikossa ilman hakua (Tommin tarkennus 21.9.2026): tieto ei ole ensisijaista
     * mutta kiinnostavaa, eikä sen takia haeta kolmen tuhannen rivin sivua.
     */
    val hallCount: StateFlow<Int?> = _hallCount.asStateFlow()

    fun toggleInvitations() = setFolds(_folds.value.copy(invitationsFolded = !_folds.value.invitationsFolded))

    fun toggleTournaments() = setFolds(_folds.value.copy(tournamentsFolded = !_folds.value.tournamentsFolded))

    /** Avaus hakee pelaajalistan kerran, ei joka avauksella; sulku ei pyyhi haettua. */
    fun togglePlayers() {
        val folded = !_folds.value.playersFolded
        setFolds(_folds.value.copy(playersFolded = folded))
        if (!folded) openPlayers()
    }

    /** Sama sääntö hallille. */
    fun toggleHall() {
        val folded = !_folds.value.hallFolded
        setFolds(_folds.value.copy(hallFolded = folded))
        if (!folded) openHall()
    }

    private fun setFolds(folds: LoungeFolds) {
        _folds.value = folds
        foldStore.save(folds)
    }

    private val _state = MutableStateFlow<LoungeUiState>(LoungeUiState.NoPath)
    val state: StateFlow<LoungeUiState> = _state.asStateFlow()

    private val _join = MutableStateFlow<JoinUiState>(JoinUiState.Idle)
    val join: StateFlow<JoinUiState> = _join.asStateFlow()

    private val _players = MutableStateFlow<PlayerListUiState>(PlayerListUiState.NoPath)
    val players: StateFlow<PlayerListUiState> = _players.asStateFlow()

    private val _signup = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    val signup: StateFlow<SignupUiState> = _signup.asStateFlow()

    /**
     * Pelaajalistan polku joka voi olla myös sivutus- tai lajittelulinkki. Ensimmäinen
     * arvo tulee loungelta, ja sivun omat linkit korvaavat sen napautuksesta.
     */
    private var playerListPath: String? = null

    init {
        viewModelScope.launch {
            loungePathUpstream.collect { path ->
                _path.value = path
                // Ensimmäinen saapunut polku laukaisee haun. Myöhemmät arvot eivät
                // pyyhi jo haettua sisältöä: polku on käytännössä vakio /bg/lounge,
                // ja Refresh on käyttäjän oma valinta kuten otteluluettelossa.
                if (path != null && _state.value is LoungeUiState.NoPath) fetch()
            }
        }
    }

    /**
     * Refresh hakee loungen ja sen lisäksi ne haetut jaksot jotka ovat auki (21.9.2026
     * illasta, kun Players ja Tournament Hall tulivat jaksoiksi). Kiinni olevaa ei
     * haeta: sen sisältö ei ole ruudulla, eikä hallin sivu ole halpa luettava.
     */
    fun refresh() {
        fetch()
        if (!_folds.value.playersFolded && _players.value !is PlayerListUiState.NoPath) refreshPlayers()
        if (!_folds.value.hallFolded && _hall.value !is HallUiState.NoPath) fetchHall()
    }

    private fun fetch() {
        val path = _path.value ?: run {
            _state.value = LoungeUiState.NoPath
            return
        }
        _state.value = when (val current = _state.value) {
            is LoungeUiState.Loaded -> current.copy(refreshing = true)
            else -> LoungeUiState.Loading
        }
        viewModelScope.launch {
            _state.value = withContext(io) { read(path) }
            // Auki muistettu jakso ei voinut hakea ennen kuin lounge antoi polun, joten
            // se haetaan nyt (21.9.2026): muuten kylmäkäynnistys näyttäisi avatun otsikon
            // alla "ei polkua" vaikka polku juuri saapui.
            if (_state.value is LoungeUiState.Loaded) {
                if (!_folds.value.playersFolded && _players.value is PlayerListUiState.NoPath) fetchPlayers()
                if (!_folds.value.hallFolded && _hall.value is HallUiState.NoPath) fetchHall()
            }
        }
    }

    /**
     * Epäonnistunut päivitys korvaa näkyvän sisällön virheruudulla, ja se on valinta eikä
     * vuoto (Tommi 31.8.2026, kompositioauditoinnin kysymys 1). Otteluluettelon sääntö
     * "epäonnistunut päivitys ei saa hävittää jo näkyvää listaa" (`TopViewModel.onFailure`)
     * koskee Topia, asetuksia ja lautaa; Lounge, palsta ja profiilisivu ovat toissijaisia
     * näkymiä ja saavat näyttää virheruudun. `DiscussionViewModel` ja `PageViewModel`
     * osoittavat tähän kommenttiin.
     */
    private fun read(path: String): LoungeUiState =
        when (val response = pages.fetch(path)) {
            is DgResponse.Ok ->
                // Null tarkoittaa aina "ei tämä sivu", eikä kutsupaikan tarvitse muistaa
                // kysyä erikseen: jäsennin tunnistaa itse, ks. `DgPages`.
                LoungeParser.parse(response.html)
                    ?.let { LoungeUiState.Loaded(it) }
                    ?: LoungeUiState.NotALoungePage
            is DgResponse.Offline -> LoungeUiState.Failed(Failure.Offline)
            is DgResponse.ServerError -> LoungeUiState.Failed(Failure.Server(response.code))
            DgResponse.Sleeping -> LoungeUiState.Failed(Failure.Sleeping)
            // Tunnuksia ei poisteta täällä: CredentialsStoren omistaa TopViewModel.
            DgResponse.AuthFailed -> LoungeUiState.SessionExpired
        }

    /**
     * Avaa pelaajalistan ja hakee sen ensimmäisellä avauksella.
     *
     * **Haku on laiska tarkoituksella.** Pelaajalista on sivuston oma kokonaislista (sata
     * riviä kerrallaan), eikä sitä haeta sen varalta että joku avaisi sen. Sivu haetaan
     * kerran ja pysyy sen jälkeen, kuten asetusruudulla; uuden haun pyytää Refresh.
     *
     * Segmenttirivi poistui 4.9.2026 kun omat turnaukset siirtyivät otteluluetteloon
     * (`docs/UI.md`), ja lounge on nyt yksi ruutu jonka porautuminen tämä on.
     */
    fun openPlayers() {
        if (_players.value is PlayerListUiState.NoPath) fetchPlayers()
    }

    /** Refresh pelaajalistalle. Aktiivinen haku toistetaan, muuten lista haetaan. */
    fun refreshPlayers() {
        _search.value?.let { searchPlayers(it) } ?: fetchPlayers()
    }

    private val _hall = MutableStateFlow<HallUiState>(HallUiState.NoPath)
    val hall: StateFlow<HallUiState> = _hall.asStateFlow()

    /** Hallin jakso avattiin: haetaan kerran, ei joka avauksella. */
    fun openHall() {
        if (_hall.value is HallUiState.NoPath) fetchHall()
    }

    /** Refresh hallille; myös virhetilan Retry. */
    fun refreshHall() = fetchHall()

    private fun fetchHall() {
        val path = (_state.value as? LoungeUiState.Loaded)?.page?.tournamentHallPath
            ?: run {
                _hall.value = HallUiState.NoPath
                return
            }
        _hall.value = when (val current = _hall.value) {
            is HallUiState.Loaded -> current.copy(refreshing = true)
            else -> HallUiState.Loading
        }
        viewModelScope.launch {
            val result = withContext(io) { readHall(pages.fetch(path)) }
            _hall.value = result
            if (result is HallUiState.Loaded) {
                _hallCount.value = result.hall.active.size
                foldStore.saveHallCount(result.hall.active.size)
            }
        }
    }

    private fun readHall(response: DgResponse): HallUiState = when (response) {
        is DgResponse.Ok ->
            TournamentHallParser.parse(response.html)
                ?.let { HallUiState.Loaded(it) }
                ?: HallUiState.NotAHall
        is DgResponse.Offline -> HallUiState.Failed(Failure.Offline)
        is DgResponse.ServerError -> HallUiState.Failed(Failure.Server(response.code))
        DgResponse.Sleeping -> HallUiState.Failed(Failure.Sleeping)
        DgResponse.AuthFailed -> {
            _state.value = LoungeUiState.SessionExpired
            HallUiState.NoPath
        }
    }

    private val _search = MutableStateFlow<String?>(null)

    /**
     * Aktiivinen hakusana, tai null kun pelaajalista näyttää sivun omaa listaa.
     *
     * Pelaajahaku on loungen oma lomake (`POST /bg/plist`, Tommin tilaus 3.9.2026; rajattu
     * ulos 27.8.2026 lähetyksenä, ja päätös on nyt tehty). Se on lähetys muttei
     * peruuttamaton teko: haku ei muuta sivustolla mitään, joten vahvistusta ei kysytä ja
     * sama haku saa lähteä uudelleen Refreshistä. Tulos on pelaajalista, joten se luetaan
     * samalla jäsentimellä ja näytetään samassa tilassa kuin sivun oma lista.
     */
    val search: StateFlow<String?> = _search.asStateFlow()

    /** Loungen hakulomake, tai null kun loungea ei ole luettu tai sillä ei ole lomaketta. */
    val searchForm: PlayerSearchForm?
        get() = (_state.value as? LoungeUiState.Loaded)?.page?.searchForm

    /**
     * Hakee pelaajat joiden nimi alkaa annetulla tekstillä. Lomake ja sen kentät ovat
     * sivulta luettuja ([PlayerSearchForm]); tyhjä hakusana ei lähetä mitään.
     */
    fun searchPlayers(query: String) {
        val submission = searchForm?.write(query) ?: return
        _search.value = query.trim()
        _players.value = when (val current = _players.value) {
            is PlayerListUiState.Loaded -> current.copy(refreshing = true)
            else -> PlayerListUiState.Loading
        }
        viewModelScope.launch {
            _players.value = withContext(io) { readPlayerList(forms.send(submission)) }
        }
    }

    /** Takaisin sivun omaan listaan: hakusana pois ja lista haetaan loungen linkillä. */
    fun clearSearch() {
        if (_search.value == null) return
        _search.value = null
        playerListPath = null
        fetchPlayers()
    }

    /** Pelaajalistan vastaus tilaksi, sama luku haulle ja sivun omalle listalle. */
    private fun readPlayerList(response: DgResponse): PlayerListUiState = when (response) {
        is DgResponse.Ok ->
            // Loungen ja pelaajalistan erottaa tunnistin, ja jäsennin ajaa sen itse
            // (`DgPages`).
            PlayerListParser.parse(response.html)
                ?.also { list ->
                    ratings.record(
                        list.players.mapNotNull { row -> row.player.userId?.let { it to row.ratingText } }.toMap(),
                    )
                    ratings.recordNames(
                        list.players.mapNotNull { row ->
                            val name = row.player.name ?: return@mapNotNull null
                            row.player.userId?.let { name to it }
                        }.toMap(),
                    )
                }
                ?.let { PlayerListUiState.Loaded(it) }
                ?: PlayerListUiState.NotAPlayerList
        is DgResponse.Offline -> PlayerListUiState.Failed(Failure.Offline)
        is DgResponse.ServerError -> PlayerListUiState.Failed(Failure.Server(response.code))
        DgResponse.Sleeping -> PlayerListUiState.Failed(Failure.Sleeping)
        DgResponse.AuthFailed -> {
            _state.value = LoungeUiState.SessionExpired
            PlayerListUiState.NoPath
        }
    }

    /**
     * Avaa pelaajalistan oman linkin (lajittelu tai seuraavat sata). Polku on sivun oma
     * href kyselyineen, eikä sitä koota täällä.
     */
    fun openPlayerListLink(link: PlayerListLink) {
        // Sivun oma linkki on aina listan linkki, myös hakutuloksen sivulla, joten
        // haku päättyy tähän: Refresh hakee tästä eteenpäin linkin sivun.
        _search.value = null
        playerListPath = link.path
        fetchPlayers()
    }

    private fun fetchPlayers() {
        val path = playerListPath
            ?: (_state.value as? LoungeUiState.Loaded)?.page?.playerListPath
            ?: run {
                _players.value = PlayerListUiState.NoPath
                return
            }
        playerListPath = path
        _players.value = when (val current = _players.value) {
            is PlayerListUiState.Loaded -> current.copy(refreshing = true)
            else -> PlayerListUiState.Loading
        }
        viewModelScope.launch {
            _players.value = withContext(io) { readPlayerList(pages.fetch(path)) }
        }
    }

    /**
     * Ilmoittautuu turnaukseen rivin omalla Sign Up -linkillä. Samat ehdot kuin [join]illa
     * (sivun linkki, yksi kerrallaan, ei uudelleenlähetystä), yhdellä erolla kohdassa 3:
     * onnistuminen luetaan loungen riviltä eikä vastaussivulta, ks. [SignupUiState].
     *
     * Peruuttamattomuus on lievempi kuin Joinissa: rivi saa perumislinkin, joten teon voi
     * perua niin kauan kuin turnaus on listalla. Vahvistus kysytään silti ruudulla, koska
     * ilmoittautuminen sitoo useaan otteluun ja tuntuu halvalta juuri sillä hetkellä
     * (`SUBSTANSSI.md` kohta 53).
     */
    fun signUp(tournament: LoungeTournament) =
        sendSignup(SignupAction.SignUp, tournament, tournament.signUp())

    /** Peruu ilmoittautumisen rivin omalla Cancel Signup -linkillä. */
    fun cancelSignup(tournament: LoungeTournament) =
        sendSignup(SignupAction.Cancel, tournament, tournament.cancelSignup())

    private fun sendSignup(
        action: SignupAction,
        tournament: LoungeTournament,
        submission: FormSubmission?,
    ) {
        if (_signup.value is SignupUiState.Sending || _join.value is JoinUiState.Joining) return
        submission ?: return

        _signup.value = SignupUiState.Sending(action)
        viewModelScope.launch {
            _signup.value = withContext(io) {
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> {
                        // Vastaus kelpaa loungeksi jos se on lounge; muuten yksi
                        // uudelleenluku. Kummassakin tapauksessa todiste on rivi.
                        val lounge = LoungeParser.parse(response.html)
                            ?: _path.value?.let { path ->
                                (read(path) as? LoungeUiState.Loaded)?.page
                            }
                        if (lounge == null) {
                            SignupUiState.Unconfirmed(action)
                        } else {
                            _state.value = LoungeUiState.Loaded(lounge)
                            val row = lounge.tournaments.firstOrNull {
                                it.eventId != null && it.eventId == tournament.eventId
                            }
                            val confirmed = when (action) {
                                SignupAction.SignUp -> row?.cancelPath != null
                                // Peruutuksen jälkeen rivillä on taas Sign Up. Kadonnut
                                // rivi ei todista mitään: turnaus on voinut myös alkaa.
                                SignupAction.Cancel -> row != null && row.cancelPath == null
                            }
                            if (confirmed) {
                                // Omien turnausten lista on nyt otteluluettelon puolella
                                // omana näkymämallinaan (4.9.2026), joten sitä ei haeta
                                // täältä. Lista on siis vanha ilmoittautumisen jälkeen
                                // kunnes se avataan tai Refresh pyytää sen; sama sääntö
                                // kuin muillakin laiskoilla listoilla.
                                SignupUiState.Done(action, tournament.name)
                            } else {
                                SignupUiState.Unconfirmed(action)
                            }
                        }
                    }
                    is DgResponse.Offline -> SignupUiState.Failed(action, Failure.Offline)
                    is DgResponse.ServerError ->
                        SignupUiState.Failed(action, Failure.Server(response.code))
                    DgResponse.Sleeping ->
                        SignupUiState.Failed(action, Failure.Sleeping)
                    DgResponse.AuthFailed -> {
                        _state.value = LoungeUiState.SessionExpired
                        SignupUiState.Idle
                    }
                }
            }
        }
    }

    fun clearSignupResult() {
        if (_signup.value !is SignupUiState.Sending) _signup.value = SignupUiState.Idle
    }

    /**
     * Hyväksyy kutsun. **Peruuttamaton teko: ottelu alkaa heti.** Ehdot rakenteena:
     *
     * 1. **Lähetys tulee sivun linkistä.** `invitation.join()` palauttaa nullin kun
     *    Join-linkkiä ei ole, eikä sitä voi keksiä.
     * 2. **Yksi kerrallaan.** Käynnissä olevan lähetyksen aikana toinen kutsu ei tee
     *    mitään, joten kaksoisnapautus ei hyväksy kahta.
     * 3. **Onnistuminen luetaan sivulta.** Kaksi vastausta luetaan onnistumiseksi:
     *    tunnistettu lounge ja sivuston oma kuittaussivu (mitattu 31.8.2026, ks.
     *    `DgPages.isJoinConfirmationPage`). Muu on [JoinUiState.Unconfirmed] ja lounge
     *    haetaan kerran uudelleen.
     * 4. **Ei automaattista uudelleenlähetystä**, ei katkolla eikä katkenneella
     *    istunnolla: toinen accept voisi olla toinen hyväksytty kutsu.
     * 5. **Tarjous todennetaan ennen lähetystä.** Lounge luetaan juuri ennen Joinia, ja
     *    teko lähtee vain jos samalla linkillä on yhä sama tarjous (muoto, pituus, pelaaja,
     *    aikaraja ja kommentti). Muuten [JoinUiState.Changed] eikä mitään lähetetä. Luku ei
     *    kuluta jonoa, koska lounge ei ole `/bg/nextgame` (27.9.2026).
     */
    fun join(invitation: LoungeInvitation) {
        if (_join.value is JoinUiState.Joining || _signup.value is SignupUiState.Sending) return
        val submission = invitation.join() ?: return

        _join.value = JoinUiState.Joining
        viewModelScope.launch {
            _join.value = withContext(io) {
                val path = _path.value
                if (path != null) {
                    when (val fresh = read(path)) {
                        is LoungeUiState.Loaded -> {
                            val same = fresh.page.invitations
                                .firstOrNull { it.joinPath == invitation.joinPath }
                            if (same != invitation) {
                                _state.value = fresh
                                return@withContext JoinUiState.Changed
                            }
                        }
                        LoungeUiState.SessionExpired -> {
                            _state.value = fresh
                            return@withContext JoinUiState.Idle
                        }
                        is LoungeUiState.Failed -> return@withContext JoinUiState.Failed(fresh.reason)
                        // Tuntematon sivu luettaessa: ei lähetetä arvauksen varassa.
                        else -> return@withContext JoinUiState.Changed
                    }
                }
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> {
                        val lounge = LoungeParser.parse(response.html)
                        if (lounge != null) {
                            // Vastaus on jo tuore lounge, joten lista päivittyy siitä
                            // ilman uutta hakua.
                            _state.value = LoungeUiState.Loaded(lounge)
                            JoinUiState.Joined
                        } else if (DgPages.isJoinConfirmationPage(response.html)) {
                            // Sivuston oma kuittaussivu (mitattu 31.8.2026): se sanoo
                            // liittymisen onnistuneen muttei kanna loungea, joten lista
                            // haetaan kerran. Haku on luku eikä uusi yritys.
                            _path.value?.let { _state.value = read(it) }
                            JoinUiState.Joined
                        } else {
                            // Tuntematon vastaus: yksi uudelleenluku, joka on pelkkä
                            // luku eikä uusi yritys.
                            _path.value?.let { _state.value = read(it) }
                            JoinUiState.Unconfirmed
                        }
                    }
                    is DgResponse.Offline -> JoinUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError -> JoinUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> JoinUiState.Failed(Failure.Sleeping)
                    DgResponse.AuthFailed -> {
                        _state.value = LoungeUiState.SessionExpired
                        JoinUiState.Idle
                    }
                }
            }
        }
    }

    fun clearJoinResult() {
        if (_join.value !is JoinUiState.Joining) _join.value = JoinUiState.Idle
    }

    class Factory(
        private val pages: PageFetcher,
        private val forms: FormSender,
        private val loungePathUpstream: Flow<String?>,
        private val foldStore: LoungeFoldStore,
        private val ratings: PlayerRatingStore = PlayerRatingStore.NONE,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoungeViewModel(pages, forms, loungePathUpstream, foldStore, ratings = ratings) as T
    }
}
