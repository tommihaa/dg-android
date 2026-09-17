package fi.tommi.dg.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.R
import fi.tommi.dg.data.MatchMemory
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.PlayerTournamentRow
import fi.tommi.dg.domain.SeenMatch
import fi.tommi.dg.domain.PlayerTournaments
import fi.tommi.dg.net.DgResponse
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
 * *waiting for your turn* -rivejä siihen asti että joku painaa Refresh. Hinta on yksi sivu
 * per asennus, eikä se toistu: ehto on tyhjä muisti eikä avaus. Koko turnaustiedon haku
 * asennushetkellä olisi ollut 60 sivua, ja se on se julkistuksen kuormapiikki jota vältetään.
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
) : ViewModel() {

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
                if (path != null && opened && _state.value is OwnTournamentsUiState.NoPath) {
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

    /** Onko tyhjän muistin kertatäyttö jo tehty tai todettu tarpeettomaksi. */
    private var seeded = false

    /** Segmentti avattiin: haetaan kerran, ei joka avauksella. */
    fun onOpened() {
        opened = true
        if (_state.value is OwnTournamentsUiState.NoPath) fetch()
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

    /** Painallus: turnaussivu ja oma profiili, ks. luokan kuvaus. */
    fun refresh() {
        fetch()
        refreshOpponents()
    }

    /**
     * Oman profiilin `Active games` ottelumuistiin. Ilman polkua tai muistia ei tehdä mitään,
     * eikä osoitetta kokoilla: polku on Top Pagen oma linkki tai ei mitään.
     */
    private fun refreshOpponents() {
        val memory = matchMemory ?: return
        val path = _profilePath.value ?: return
        _opponents.value = OpponentsRefreshState.Running
        viewModelScope.launch {
            val result = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok -> {
                        val page = ProfileParser.parse(response.html)
                        if (page == null) {
                            OpponentsRefreshState.NotAProfile
                        } else {
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
                    DgResponse.AuthFailed -> OpponentsRefreshState.Failed(Failure.Server(401))
                }
            }
            _opponents.value = result
        }
    }

    private fun fetch() {
        val path = _path.value ?: run {
            _state.value = OwnTournamentsUiState.NoPath
            return
        }
        _state.value = when (val current = _state.value) {
            is OwnTournamentsUiState.Loaded -> current.copy(refreshing = true)
            else -> OwnTournamentsUiState.Loading
        }
        viewModelScope.launch {
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
                    // Katkennut istunto käsitellään otteluluettelon puolella, koska
                    // tunnusvaraston omistaa TopViewModel. Tästä ei siis kirjoiteta
                    // uloskirjautumista, vaan jäädään tilaan josta Refresh yrittää uudestaan.
                    DgResponse.AuthFailed -> OwnTournamentsUiState.Failed(Failure.Server(401))
                }
            }
            _state.value = loaded
            // Lukema päivittyy vain onnistuneesta latauksesta. Epäonnistunut haku ei
            // tyhjennä sitä, koska edellinen lista on yhä se tuorein mikä on nähty.
            if (loaded is OwnTournamentsUiState.Loaded) {
                countSink?.value = OwnTournamentCount(path, loaded.page.rows.size)
            }
        }
    }

    class Factory(
        private val pages: PageFetcher,
        private val pathUpstream: Flow<String?>,
        private val countSink: MutableStateFlow<OwnTournamentCount?>,
        private val profilePathUpstream: Flow<String?> = flowOf(null),
        private val matchMemory: MatchMemory? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OwnTournamentsViewModel(
                pages = pages,
                pathUpstream = pathUpstream,
                profilePathUpstream = profilePathUpstream,
                matchMemory = matchMemory,
                countSink = countSink,
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
) {
    when (state) {
        OwnTournamentsUiState.NoPath ->
            CenteredNote(stringResource(R.string.own_tournaments_no_path))

        OwnTournamentsUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is OwnTournamentsUiState.Failed -> RetryNote(
            text = state.reason.text(),
            onRetry = onRefresh,
        )

        OwnTournamentsUiState.NotTournaments -> RetryNote(
            text = stringResource(R.string.own_tournaments_not_a_list),
            onRetry = onRefresh,
        )

        is OwnTournamentsUiState.Loaded -> Column(modifier = Modifier.fillMaxSize()) {
            ProgressSlot(active = state.refreshing || opponentsRefresh == OpponentsRefreshState.Running)
            OwnTournamentsList(
                page = state.page,
                hallPath = hallPath,
                onOpenPage = onOpenPage,
                listed = listed,
                remembered = remembered,
                opponentsRefresh = opponentsRefresh,
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
 * (`Active Game` on rivillä myös silloin, laiteajo 15.9.2026), ja rivi sanoo sen muistetun
 * tiedon perään, koska muistettu kierros voi olla yhden jäljessä. Kierros ennen nimeä on
 * Tommin valinta (15.9.2026, *"round x/y on voittoja merkittävämpi"*).
 *
 * [ActiveGameLine.hasRound] kertoo rivinäkymälle että voitot jätetään pois (Tommin tilaus
 * pelisession jälkeen 15.9.2026: *"jos näkyy round-tieto, niin wins määrää ei enää
 * tarvita"*). Voitot ovat kierroksen karkeampi muoto, ja kierros 4/4 sanoo saman kuin
 * 3 wins lyhyemmin. Ilman kierrosta voitot jäävät, koska ne ovat silloin ainoa etenemistieto.
 */
internal data class ActiveGameLine(val text: String, val hasRound: Boolean)

@Composable
private fun activeGameLine(
    row: PlayerTournamentRow,
    listed: Map<MatchId, Match>?,
    remembered: Map<MatchId, SeenMatch>,
): ActiveGameLine? {
    val id = row.activeMatchId ?: return null
    val fresh = listed?.get(id)
    val seen = remembered[id]
    val opponent = fresh?.opponent?.name?.takeIf { it.isNotBlank() } ?: seen?.opponent?.name
    val round = fresh?.round?.takeIf { it.isNotBlank() && it != "-" } ?: seen?.round
    val parts = buildList {
        when {
            opponent != null && round != null ->
                add(stringResource(R.string.page_active_game_round_against, round, opponent))
            opponent != null -> add(stringResource(R.string.page_active_game_against, opponent))
            round != null -> add(stringResource(R.string.page_active_game_round, round))
        }
        if (fresh == null && listed != null) {
            add(stringResource(R.string.page_active_game_opponents_turn))
        }
    }
    return parts.takeIf { it.isNotEmpty() }
        ?.let { ActiveGameLine(text = it.joinToString(", "), hasRound = round != null) }
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
            val line = activeGameLine(row, listed, remembered)
            PlayerTournamentRowView(
                row = row,
                onOpenPath = onOpenPage,
                activeGameText = line?.text,
                showWins = line?.hasRound != true,
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
