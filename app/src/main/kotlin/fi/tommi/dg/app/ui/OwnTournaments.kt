package fi.tommi.dg.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.PlayerRatingStore
import fi.tommi.dg.app.session.TournamentRoundsStore
import fi.tommi.dg.app.session.TournamentWaitStore
import fi.tommi.dg.app.R
import fi.tommi.dg.data.MatchMemory
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.PlayerTournamentRow
import fi.tommi.dg.domain.SeenMatch
import fi.tommi.dg.domain.PlayerTournaments
import fi.tommi.dg.domain.ProfilePage
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.EventParser
import fi.tommi.dg.scrape.PlayerTournamentsParser
import fi.tommi.dg.scrape.ProfileParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pelaajan omien turnausten tila (`/bg/userevent/<id>`).
 *
 * [NoPath] tarkoittaa tässä eri asiaa kuin muissa ruuduissa: polku tulee **Top Pagen**
 * alalaidan linkistä (`TopPage.activeTournamentsPath`), joten se puuttuu niin kauan kuin
 * otteluluettelo ei ole latautunut. Se ei siis ole virhe vaan järjestys.
 */
sealed interface OwnTournamentsUiState {

    data object NoPath : OwnTournamentsUiState

    data object Loading : OwnTournamentsUiState

    data class Loaded(
        val page: PlayerTournaments,
        val refreshing: Boolean = false,
    ) : OwnTournamentsUiState

    /** 200 OK, mutta sivu ei ole turnauslista. */
    data object NotTournaments : OwnTournamentsUiState

    data class Failed(val reason: Failure) : OwnTournamentsUiState

    /**
     * Tunnukset eivät kelpaa: sivusto vastasi kirjautumissivulla (`DgResponse.AuthFailed`).
     *
     * **Vie kirjautumiseen eikä näytä virhettä (Tommin päätös 27.9.2026).** Tähän asti
     * katkeaminen näkyi palvelinvirheenä 401, joka kertoi väärän syyn ja jätti Refreshin
     * yrittämään kelpaamattomilla tunnuksilla. Tila ei kanna [SessionExpiredState]-merkintää,
     * koska se lupaa myös ruudun sulkemisen ja lista asuu juuriruudussa, jota ei suljeta.
     * `MainActivity` tyhjentää tunnukset ja otteluluettelo näyttää kirjautumislomakkeen.
     */
    data object SessionExpired : OwnTournamentsUiState
}

/**
 * Omien turnausten lukumäärä viimeksi ladatulta listalta, pelaajasivun riviä varten.
 *
 * Tommin tilaus 10.9.2026: pelaajasivu laskee `Active games (32)` samalta sivulta, mutta
 * `active tournaments` on siellä pelkkä linkki `/bg/userevent/<id>` ilman lukemaa, joten
 * lukema on vain tällä listalla. Päätös samana päivänä: ei uutta hakua vaan muistista, ja
 * kun muistissa ei ole mitään, rivi on ennallaan ilman selitystä.
 *
 * [path] on se polku jolta lista haettiin. Pelaajasivu vertaa sitä omaan linkkiinsä, joten
 * lukema osuu vain omalle profiilille eikä koskaan vieraalle; nimivertailua ei tarvita.
 */
data class OwnTournamentCount(val path: String, val count: Int)

/**
 * Refreshin toinen puoli: oman profiilin `Active games` ottelumuistiin (Tommin päätös
 * 15.9.2026, *"Refresh-nappi niille jotka haluaa ajankohtaista tietoa"*).
 *
 * Oma tila eikä osa [OwnTournamentsUiState]a, koska turnauslista ja vastustajat ovat eri
 * sivuja eri kohtaloineen: lista voi latautua kun profiili kaatuu, ja rivit piirtyvät silti.
 * Epäonnistuminen sanotaan ääneen eikä jätetä muistin vanhaksi hiljaa, koska painaja
 * odotti tuoretta.
 */
sealed interface OpponentsRefreshState {

    data object Idle : OpponentsRefreshState

    data object Running : OpponentsRefreshState

    /** 200 OK, mutta sivu ei ole profiili. */
    data object NotAProfile : OpponentsRefreshState

    data class Failed(val reason: Failure) : OpponentsRefreshState
}

/**
 * Omien turnausten näkymämalli.
 *
 * **Oma malli eikä osa [TopViewModel]ia** (4.9.2026, kun lista siirtyi loungesta
 * otteluluetteloon). `TopViewModel` on se luokka joka hakee Top Pagen, ja tämä hakee eri
 * sivun eri hetkellä; yhteen malliin sulautettuna kahden sivun tuoreus olisi yksi tila.
 * Sama sisältö oli 3.9.–4.9.2026 `LoungeViewModel`in sisällä, ja se siirtyi tänne
 * sellaisenaan.
 *
 * Sivun haku on pelkkä luku eikä kuluta jonoa, joten se saa tapahtua kun segmentti
 * avataan. Polku tulee virtana samasta syystä kuin loungella: kertakuva olisi väärä null
 * aina kun ruutu avataan ennen otteluluettelon latautumista.
 *
 * **Refresh hakee kaksi sivua, avaus yhden** (15.9.2026). Avaus on laiska haku jota kukaan
 * ei painanut, ja se pysyy yhtenä pyyntönä. Refresh on painallus, ja painaja haluaa
 * tuoretta: turnaussivun lisäksi haetaan oma profiili, jonka `Active games` listaa kaikki
 * käynnissä olevat ottelut vastustajineen ja kierroksineen, ja ne kirjoitetaan
 * [MatchMemory]yn. Kumpikaan sivu ei kuluta jonoa. Ilman painallusta muisti täyttyy vain
 * otteluluettelosta ja avatuista laudoista.
 *
 * **Yksi poikkeus: tyhjä muisti täytetään profiilista kerran** (Tommin päätös 15.9.2026,
 * *"kakkonen"*). Uuden asennuksen ensimmäinen Tournaments-ruutu olisi muuten pelkkiä
 * rivejä ilman vastustajaa ja kierrosta siihen asti että joku painaa Refresh. Hinta on yksi sivu
 * per asennus, eikä se toistu: ehto on tyhjä muisti eikä avaus. Koko turnaustiedon haku
 * asennushetkellä olisi ollut 60 sivua, ja se on se julkistuksen kuormapiikki jota vältetään.
 *
 * **Refresh hakee 25.9.2026 alkaen myös turnaussivut ja vastustajien profiilit** (Tommin
 * tilaus, `docs/UI.md` › *Turnausrivin odotus ja vastustajan väri*). Ketju on peräkkäinen,
 * koska jatko tarvitsee kahden ensimmäisen sivun sisällön: linkittömien rivien turnaussivut
 * listalta ja linkillisten rivien vastustajat profiilista. Vastustajan profiili haetaan vain
 * kun ratingia ei ole nähty. Avaus ja tyhjän muistin kertatäyttö pysyvät ennallaan.
 */
class OwnTournamentsViewModel(
    private val pages: PageFetcher,
    pathUpstream: Flow<String?>,
    /** Oman profiilin polku Top Pagen `Signed in as` -linkistä, samaa virtaa kuin [pathUpstream]. */
    profilePathUpstream: Flow<String?> = flowOf(null),
    /** Ottelumuisti johon Refresh kirjoittaa profiilin ottelut. Null testeissä joita se ei koske. */
    private val matchMemory: MatchMemory? = null,
    private val now: () -> Long = System::currentTimeMillis,
    /**
     * Mihin ladatun listan lukumäärä kirjoitetaan ([OwnTournamentCount]). Malli elää
     * otteluluettelon reitin sisällä eikä pelaajasivu näe sitä, joten lukema kulkee
     * containerin virran kautta. Null testeissä joita lukema ei koske.
     */
    private val countSink: MutableStateFlow<OwnTournamentCount?>? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /** Oman profiilin rating kirjataan tänne laudan korttia varten, ks. [PlayerRatingStore]. */
    private val ratings: PlayerRatingStore = PlayerRatingStore.NONE,
    /** Linkittömien rivien odotukset Refreshistä, ks. [TournamentWaitStore]. */
    private val waitStore: TournamentWaitStore = TournamentWaitStore.NONE,
    /**
     * Kierrosmäärät nimen rarity-väriin (2.10.2026), ks. [TournamentRoundsStore]. Kirjataan
     * vain sivuilta jotka haetaan joka tapauksessa: oman profiilin otteluista ja Refreshin
     * hakemilta turnaussivuilta.
     */
    private val roundsStore: TournamentRoundsStore = TournamentRoundsStore.NONE,
) : ViewModel() {

    private val _waits = MutableStateFlow(waitStore.all())

    /** Odotukset turnausnumeron mukaan, ks. `EventPage.matchesBeforeOpponent`. */
    val waits: StateFlow<Map<String, Int>> = _waits.asStateFlow()

    private val _ratingsRevision = MutableStateFlow(0)

    /**
     * Kasvaa kun Refresh on kirjannut vastustajien ratingeja. Varasto ei ole virta, joten
     * rivit lukevat sen uudelleen vasta kun tämä muuttuu.
     */
    val ratingsRevision: StateFlow<Int> = _ratingsRevision.asStateFlow()

    private val _extrasRunning = MutableStateFlow(false)

    /** Turnaussivujen ja vastustajaprofiilien haku käynnissä. */
    val extrasRunning: StateFlow<Boolean> = _extrasRunning.asStateFlow()

    private val _path = MutableStateFlow<String?>(null)
    private val _profilePath = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow<OwnTournamentsUiState>(OwnTournamentsUiState.NoPath)
    val state: StateFlow<OwnTournamentsUiState> = _state.asStateFlow()

    private val _opponents = MutableStateFlow<OpponentsRefreshState>(OpponentsRefreshState.Idle)
    val opponents: StateFlow<OpponentsRefreshState> = _opponents.asStateFlow()

    /**
     * Onko segmentti avattu kertaakaan. Ilman tätä polun saapuminen hakisi sivun myös
     * silloin kun käyttäjä ei ole avannut segmenttiä, eli otteluluettelon lataus toisi
     * mukanaan toisen pyynnön jota kukaan ei pyytänyt.
     */
    private var opened = false

    init {
        viewModelScope.launch {
            pathUpstream.collect { path ->
                _path.value = path
                if (path != null && opened && needsFetch()) {
                    fetch()
                }
            }
        }
        viewModelScope.launch {
            profilePathUpstream.collect {
                _profilePath.value = it
                seedOpponentsIfEmpty()
            }
        }
    }

    /**
     * Haetaanko avauksessa: ei vielä haettu, tai edellinen haku päättyi uloskirjautumiseen,
     * jolloin uudelleen kirjautunut käyttäjä näkisi muuten tyhjän segmentin.
     */
    private fun needsFetch(): Boolean = _state.value.let {
        it is OwnTournamentsUiState.NoPath || it is OwnTournamentsUiState.SessionExpired
    }

    /** Onko tyhjän muistin kertatäyttö jo tehty tai todettu tarpeettomaksi. */
    private var seeded = false

    /** Segmentti avattiin: haetaan kerran, ei joka avauksella. */
    fun onOpened() {
        opened = true
        if (needsFetch()) fetch()
        seedOpponentsIfEmpty()
    }

    /**
     * Tyhjä muisti profiilista kerran, ks. luokan kuvaus. Kutsutaan avauksesta ja polun
     * saapumisesta, koska kumpi tahansa voi tulla ensin; [seeded] estää toiston.
     */
    private fun seedOpponentsIfEmpty() {
        val memory = matchMemory ?: return
        if (seeded || !opened || _profilePath.value == null) return
        seeded = true
        viewModelScope.launch {
            if (withContext(io) { memory.count() } == 0) refreshOpponents()
        }
    }

    /** Painallus: lista, oma profiili, turnaussivut ja vastustajat, ks. luokan kuvaus. */
    fun refresh() {
        viewModelScope.launch {
            val list = loadList() as? OwnTournamentsUiState.Loaded
            val profile = loadOpponents()
            if (list != null) refreshExtras(list.page, profile)
        }
    }

    /**
     * Linkittömien rivien turnaussivut odotuksiksi ja linkillisten rivien tuntemattomat
     * vastustajat ratingeiksi. Epäonnistunut haku katkaisee ketjun eikä korvaa odotuksia,
     * koska edelliset ovat silloin tuoreimmat nähdyt.
     */
    private suspend fun refreshExtras(page: PlayerTournaments, profile: ProfilePage?) {
        val own = _profilePath.value
        _extrasRunning.value = true
        try {
            if (own != null) {
                val waits = mutableMapOf<String, Int>()
                var complete = true
                for (row in page.rows) {
                    if (row.activeMatchPath != null) continue
                    val eventId = row.eventId ?: continue
                    val path = row.eventPath ?: continue
                    val response = withContext(io) { pages.fetch(path) }
                    if (response !is DgResponse.Ok) {
                        complete = false
                        break
                    }
                    val event = withContext(io) { EventParser.parse(response.html) } ?: continue
                    event.matchesBeforeOpponent(own)?.let { waits[eventId] = it }
                    event.roundCount?.let { roundsStore.record(eventId, it) }
                }
                if (complete) {
                    withContext(io) { waitStore.replace(waits) }
                    _waits.value = waits.toMap()
                }
            }

            val active = page.rows.mapNotNull { it.activeMatchId }.toSet()
            val unknown = profile?.activeMatches.orEmpty()
                .filter { it.id in active }
                .map { it.opponent }
                .filter { opponent ->
                    val id = opponent.userId
                    id != null && opponent.profilePath != null && ratings.rating(id) == null
                }
                .distinctBy { it.userId }
            var recorded = false
            for (opponent in unknown) {
                val path = opponent.profilePath ?: continue
                val response = withContext(io) { pages.fetch(path) }
                if (response !is DgResponse.Ok) break
                val seen = withContext(io) { ProfileParser.parse(response.html) } ?: continue
                seen.player?.userId?.let {
                    ratings.record(mapOf(it to seen.ratingText))
                    seen.player?.name?.let { name -> ratings.recordNames(mapOf(name to it)) }
                    recorded = true
                }
            }
            if (recorded) _ratingsRevision.value++
        } finally {
            _extrasRunning.value = false
        }
    }

    /**
     * Oman profiilin `Active games` ottelumuistiin. Ilman polkua tai muistia ei tehdä mitään,
     * eikä osoitetta kokoilla: polku on Top Pagen oma linkki tai ei mitään.
     */
    private fun refreshOpponents() {
        viewModelScope.launch { loadOpponents() }
    }

    /** [refreshOpponents] odotettavana; palauttaa luetun profiilin tai null. */
    private suspend fun loadOpponents(): ProfilePage? {
        val memory = matchMemory ?: return null
        val path = _profilePath.value ?: return null
        _opponents.value = OpponentsRefreshState.Running
        var read: ProfilePage? = null
        run {
            val result = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok -> {
                        val page = ProfileParser.parse(response.html)
                        read = page
                        page?.player?.userId?.let {
                            ratings.record(mapOf(it to page.ratingText))
                            page.player?.name?.let { name -> ratings.recordNames(mapOf(name to it)) }
                        }
                        if (page == null) {
                            OpponentsRefreshState.NotAProfile
                        } else {
                            page.activeMatches.forEach { match ->
                                val eventId = match.eventId ?: return@forEach
                                roundsFromText(match.round)?.let { roundsStore.record(eventId, it) }
                            }
                            val seenAt = now()
                            memory.remember(
                                page.activeMatches.map { match ->
                                    SeenMatch(
                                        matchId = match.id,
                                        opponent = match.opponent,
                                        round = match.round?.takeIf { it.isNotBlank() && it != "-" },
                                        matchLength = match.matchLength,
                                        eventName = match.eventName.takeIf { it.isNotBlank() },
                                        seenAtEpochMillis = seenAt,
                                    )
                                },
                            )
                            OpponentsRefreshState.Idle
                        }
                    }
                    is DgResponse.Offline -> OpponentsRefreshState.Failed(Failure.Offline)
                    is DgResponse.ServerError ->
                        OpponentsRefreshState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> OpponentsRefreshState.Failed(Failure.Sleeping)
                    // Kelpaamaton tunnus koskee koko otteluluetteloa eikä vain vastustajia,
                    // joten se kirjoitetaan listan tilaan, josta `MainActivity` sen lukee.
                    DgResponse.AuthFailed -> OpponentsRefreshState.Idle.also {
                        _state.value = OwnTournamentsUiState.SessionExpired
                    }
                }
            }
            _opponents.value = result
        }
        return read
    }

    private fun fetch() {
        viewModelScope.launch { loadList() }
    }

    /** [fetch] odotettavana; palauttaa uuden tilan. */
    private suspend fun loadList(): OwnTournamentsUiState {
        val path = _path.value ?: run {
            _state.value = OwnTournamentsUiState.NoPath
            return OwnTournamentsUiState.NoPath
        }
        _state.value = when (val current = _state.value) {
            is OwnTournamentsUiState.Loaded -> current.copy(refreshing = true)
            else -> OwnTournamentsUiState.Loading
        }
        run {
            val loaded = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok ->
                        PlayerTournamentsParser.parse(response.html)
                            ?.let { OwnTournamentsUiState.Loaded(it) }
                            ?: OwnTournamentsUiState.NotTournaments
                    is DgResponse.Offline -> OwnTournamentsUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError ->
                        OwnTournamentsUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping ->
                        OwnTournamentsUiState.Failed(Failure.Sleeping)
                    // Tunnusvaraston omistaa TopViewModel, joten tästä ei kirjoiteta
                    // uloskirjautumista itse vaan tila, jonka `MainActivity` käsittelee.
                    DgResponse.AuthFailed -> OwnTournamentsUiState.SessionExpired
                }
            }
            _state.value = loaded
            // Lukema päivittyy vain onnistuneesta latauksesta. Epäonnistunut haku ei
            // tyhjennä sitä, koska edellinen lista on yhä se tuorein mikä on nähty.
            if (loaded is OwnTournamentsUiState.Loaded) {
                countSink?.value = OwnTournamentCount(path, loaded.page.rows.size)
            }
            return loaded
        }
    }

    class Factory(
        private val pages: PageFetcher,
        private val pathUpstream: Flow<String?>,
        private val countSink: MutableStateFlow<OwnTournamentCount?>,
        private val profilePathUpstream: Flow<String?> = flowOf(null),
        private val matchMemory: MatchMemory? = null,
        private val ratings: PlayerRatingStore = PlayerRatingStore.NONE,
        private val waitStore: TournamentWaitStore = TournamentWaitStore.NONE,
        private val roundsStore: TournamentRoundsStore = TournamentRoundsStore.NONE,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OwnTournamentsViewModel(
                pages = pages,
                pathUpstream = pathUpstream,
                profilePathUpstream = profilePathUpstream,
                matchMemory = matchMemory,
                countSink = countSink,
                ratings = ratings,
                waitStore = waitStore,
                roundsStore = roundsStore,
            ) as T
    }
}

/**
 * Pelaajan omat käynnissä olevat turnaukset (`/bg/userevent/<id>`).
 *
 * Rivit ovat samat kuin profiilin porautumisruudulla (`PlayerTournamentRowView` jaettu).
 * [hallPath] on `null` otteluluettelossa ja se on tarkoitus: Tournament Hall on koko
 * sivuston lista, se asuu loungessa ja sen polku luetaan loungen sivulta.
 *
 * [listed] on otteluluettelo ottelun numerolla ja [remembered] ottelumuisti kannasta.
 * Turnaussivu ei nimeä vastustajaa eikä kierrosta (Tommin tilaus 15.9.2026: *"kun on active
 * game, niin pitäisi kertoa ketä vastaan"*, ja *"round x/y on voittoja merkittävämpi"*),
 * mutta `Active Game` osoittaa samaan otteluun jonka Top Page listaa ja jonka lauta on
 * joskus näytetty. Kumpaakaan ei haeta tätä varten, ks. [activeGameText].
 */
@Composable
fun OwnTournamentsSection(
    state: OwnTournamentsUiState,
    onRefresh: () -> Unit,
    onOpenPage: (String) -> Unit,
    hallPath: String? = null,
    listed: Map<MatchId, Match>? = null,
    remembered: Map<MatchId, SeenMatch> = emptyMap(),
    opponentsRefresh: OpponentsRefreshState = OpponentsRefreshState.Idle,
    waits: Map<String, Int> = emptyMap(),
    ratingsRevision: Int = 0,
    extrasRunning: Boolean = false,
) {
    when (state) {
        OwnTournamentsUiState.NoPath ->
            CenteredNote(stringResource(R.string.own_tournaments_no_path))

        OwnTournamentsUiState.Loading -> BusyCentered()

        // Uloskirjautuminen on jo matkalla, ja luettelo vaihtuu kirjautumislomakkeeksi.
        OwnTournamentsUiState.SessionExpired -> BusyCentered()

        is OwnTournamentsUiState.Failed -> RetryNote(
            text = state.reason.text(),
            onRetry = onRefresh,
        )

        OwnTournamentsUiState.NotTournaments -> RetryNote(
            text = stringResource(R.string.own_tournaments_not_a_list),
            onRetry = onRefresh,
        )

        is OwnTournamentsUiState.Loaded -> Column(modifier = Modifier.fillMaxSize()) {
            ProgressSlot(
                active = state.refreshing || opponentsRefresh == OpponentsRefreshState.Running ||
                    extrasRunning,
            )
            OwnTournamentsList(
                page = state.page,
                hallPath = hallPath,
                onOpenPage = onOpenPage,
                listed = listed,
                remembered = remembered,
                opponentsRefresh = opponentsRefresh,
                waits = waits,
                ratingsRevision = ratingsRevision,
            )
        }
    }
}

/**
 * Ottelumuistin hetki luettavana. Sama englanti ja sama vyöhykeperuste kuin [ArchiveEdge],
 * mutta kellonajan kanssa: Refresh voi olla samalta päivältä kuin edellinen kirjoitus, ja
 * rivi luetaan juuri silloin kun kysytään ehtikö painallus vaikuttaa.
 */
internal object SeenAt {
    private val FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)

    fun format(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime().format(FORMAT)
}

/**
 * Käynnissä olevan ottelun rivi sanoina, tai null kun mitään ei tiedetä.
 *
 * Kaksi lähdettä samassa järjestyksessä: luettelo ensin, koska se on tuore, ja muisti sen
 * jälkeen, koska se on viimeksi nähty. Luettelolta puuttuva ottelu on vastustajan vuorolla
 * (`Active Game` on rivillä myös silloin, laiteajo 15.9.2026). Rivi sanoi sen 15.–21.9.2026
 * muistetun tiedon perään (*waiting for your turn*), koska muistettu kierros voi olla yhden
 * jäljessä; Tommi poisti sen 21.9.2026 (*"turhake"*): vuoron kertoo jo se ettei ottelu ole
 * Your turn -luettelossa, ja ikärivi listan yllä kertoo tuoreuden. Kierros ennen nimeä on
 * Tommin valinta (15.9.2026, *"round x/y on voittoja merkittävämpi"*).
 *
 * [ActiveGameLine.hasRound] kertoo rivinäkymälle että voitot jätetään pois (Tommin tilaus
 * pelisession jälkeen 15.9.2026: *"jos näkyy round-tieto, niin wins määrää ei enää
 * tarvita"*). Voitot ovat kierroksen karkeampi muoto, ja kierros 4/4 sanoo saman kuin
 * 3 wins lyhyemmin. Ilman kierrosta voitot jäävät, koska ne ovat silloin ainoa etenemistieto.
 */
internal data class ActiveGameLine(val text: AnnotatedString, val hasRound: Boolean)

/**
 * Vastustajan nimi saa harvinaisuusvärin ratingin mukaan kuten laudan kortti (Tommin tilaus
 * 25.9.2026, *"pelaajanimi suhteuta rating"*), kun rating tunnetaan. [ratingsRevision] ei
 * vaikuta tulokseen; se on parametrina jotta rivi luetaan uudelleen Refreshin kirjattua
 * ratingeja.
 */
@Composable
private fun activeGameLine(
    row: PlayerTournamentRow,
    listed: Map<MatchId, Match>?,
    remembered: Map<MatchId, SeenMatch>,
    @Suppress("UNUSED_PARAMETER") ratingsRevision: Int,
): ActiveGameLine? {
    val id = row.activeMatchId ?: return null
    val fresh = listed?.get(id)
    val seen = remembered[id]
    val freshName = fresh?.opponent?.name?.takeIf { it.isNotBlank() }
    val opponent = freshName ?: seen?.opponent?.name
    val userId = (if (freshName != null) fresh.opponent.userId else null) ?: seen?.opponent?.userId
    val round = fresh?.round?.takeIf { it.isNotBlank() && it != "-" } ?: seen?.round
    val text = when {
        opponent != null && round != null ->
            stringResource(R.string.page_active_game_round_against, round, opponent)
        opponent != null -> stringResource(R.string.page_active_game_against, opponent)
        round != null -> stringResource(R.string.page_active_game_round, round)
        else -> return null
    }
    val rating = userId?.let(LocalPlayerRating.current)
    val color = rating?.let { Rarity.color(ratingTier(it), dgDark()) }
    val styled = buildAnnotatedString {
        append(text)
        val at = opponent?.let { text.lastIndexOf(it) } ?: -1
        if (color != null && at >= 0) {
            addStyle(SpanStyle(color = color), at, at + opponent!!.length)
        }
    }
    return ActiveGameLine(text = styled, hasRound = round != null)
}

/** Sama muoto kuin loungen omassa vastineessa; kopio, koska tuo on tiedostonsa sisäinen. */
@Composable
private fun CenteredNote(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OwnTournamentsList(
    page: PlayerTournaments,
    hallPath: String?,
    onOpenPage: (String) -> Unit,
    listed: Map<MatchId, Match>?,
    remembered: Map<MatchId, SeenMatch>,
    opponentsRefresh: OpponentsRefreshState,
    waits: Map<String, Int>,
    ratingsRevision: Int,
) {
    // Lukupinta kuten otteluluettelossa (`DgLazyColumn`): ilman sitä rivit piirtyivät
    // suoraan yötaustan päälle, ja tumman teeman motiiviviiva (esim. `#FFF0B0`) antoi
    // leipätekstille 1,4–1,8:1 ja vaalean oikea tahko 3,4:1. Mitattu 13.9.2026 pelisession
    // jälkeen Tommin havainnosta "jotkin taustat ovat todella kirkkaita".
    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.own_tournaments_heading, page.rows.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        item {
            // Ikä sanotaan kerran listan yllä eikä joka rivillä (Tommin kuittaus 15.9.2026:
            // "tieto voi olla vanhaa, ja ruudun on sanottava se"). Hetki on muistin uusin
            // kirjoitus, eli otteluluettelon, laudan tai Refreshin profiilihaun aika.
            val newest = remembered.values.maxOfOrNull { it.seenAtEpochMillis }
            Text(
                text = when (opponentsRefresh) {
                    OpponentsRefreshState.NotAProfile ->
                        stringResource(R.string.own_tournaments_opponents_not_a_profile)
                    is OpponentsRefreshState.Failed ->
                        stringResource(R.string.own_tournaments_opponents_failed, opponentsRefresh.reason.text())
                    else -> if (newest == null) {
                        stringResource(R.string.own_tournaments_opponents_none)
                    } else {
                        stringResource(R.string.own_tournaments_opponents_as_of, SeenAt.format(newest))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (page.rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.own_tournaments_empty),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(page.rows, contentType = { DgStripedRow }) { row ->
            val line = activeGameLine(row, listed, remembered, ratingsRevision)
            // Linkittömän rivin odotus Refreshin hakemalta turnaussivulta (25.9.2026).
            val wait = row.eventId?.let(waits::get)?.takeIf { row.activeMatchPath == null }
            // **Vastustajan vuoro sanotaan** (Tommin kysymys ja valinta 24.9.2026: *"mitä
            // tarkoittaa has an active game"*). Top Page luettelee vain ottelut joissa on oma
            // vuoro (`docs/KOHDE.md`, mitattu 1.8.2026), joten luettelosta puuttuva käynnissä
            // oleva ottelu odottaa vastustajaa. Kierrosta ja vastustajaa ei haeta (sama
            // valinta: sanamuoto eikä lisähakua). Ilman luetteloa sanaa ei voi päätellä.
            val opponentsTurn = stringResource(R.string.page_active_game_opponents_turn)
            PlayerTournamentRowView(
                row = row,
                onOpenPath = onOpenPage,
                activeGameText = line?.text
                    ?: opponentsTurn.takeIf { listed != null && row.activeMatchId != null }
                        ?.let(::AnnotatedString),
                showWins = line?.hasRound != true,
                waitText = wait?.let {
                    pluralStringResource(R.plurals.own_tournaments_waiting_matches, it, it)
                },
                // Kierrosmäärä nimen väriin luettelosta tai ottelumuistista (2.10.2026).
                rounds = row.activeMatchId
                    ?.let { listed?.get(it)?.round ?: remembered[it]?.round }
                    .let(::roundsFromText),
            )
            HorizontalDivider()
        }
        if (hallPath != null) {
            item {
                // Sivun oma linkki sivun omalla sanalla, ei segmenttinä: halli on koko
                // sivuston lista eikä rinnakkainen omien kanssa.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPage(hallPath) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.own_tournaments_hall_link),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.own_tournaments_hall_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
