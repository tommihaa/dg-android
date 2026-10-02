package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.PlayerRef
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.LoungeFolds
import fi.tommi.dg.domain.HallTournament
import fi.tommi.dg.domain.LoungeInvitation
import fi.tommi.dg.domain.LoungePage
import fi.tommi.dg.domain.LoungeTournament
import fi.tommi.dg.domain.PlayerTournamentRow
import fi.tommi.dg.domain.PlayerTournaments
import fi.tommi.dg.domain.PlayerListLink
import fi.tommi.dg.domain.PlayerRow

/**
 * Game Lounge: kutsut ja turnausten ilmoittautumislista.
 *
 * Ruudulla on kolme tekoa sivustolla: Join, Sign Up ja Cancel Signup. Join ja Sign Up
 * kulkevat vahvistusdialogin läpi, Cancel ei. Join aloittaa ottelun heti eikä sitä voi
 * perua. Sign Up on peruttavissa rivin omalla Cancel-linkillä, mutta se sitoo useaan
 * otteluun ja tuntuu halvalta juuri painettaessa (`SUBSTANSSI.md` kohta 53), joten kysymys
 * kysytään. Cancel on peruttavissa ilmoittautumalla uudestaan, joten sitä ei kysytä.
 * Sivun oma verify-doktriini ei päde näihin: sivulla ei ole valintaruutua, joten dialogi
 * on ruudun oma vartio eikä sivun asetuksen toisto. Ilmoittautuminen tuli 3.9.2026
 * kaanonimuutoksella (`SUBSTANSSI.md` kohta 52).
 *
 * **Yksi ruutu eikä segmenttiriviä 4.9.2026 alkaen** (Tommin päätös, `docs/UI.md`).
 * Segmenttejä oli kolme 26.8.2026 alkaen, ja niistä `Tournaments` siirtyi otteluluetteloon
 * omaksi välilehdekseen: omat turnaukset kertovat missä jo olen, ja lounge on se paikka
 * johon mennään mukaan. Jäljelle jäivät tarjoukset ja pelaajalista.
 *
 * **Neljä taittuvaa jaksoa yhdessä listassa 21.9.2026 illasta** (Tommin tilaus ja
 * tarkennus, `docs/UI.md`): Players, Tournament Hall, Matches waiting for opponents ja
 * Tournament sign-up, kukin otsikkonaan jonka edessä on `+` kiinni ja `−` auki. Taitto
 * muistetaan laitteella (`LoungeFoldStore`). Players ja Tournament Hall olivat 21.9.
 * päivällä linkkirivi listojen yllä ja sitä ennen listan perässä; porautuminen omaan
 * näkymään (pelaajalista tässä ruudussa, halli `PageScreen`inä) poistui, ja kumpikin
 * avautuu otsikon alle. Avaus hakee sivun kerran, sulku ei pyyhi haettua. Hallin otsikko
 * kantaa viime haun aktiivisten määrän ilman hakua, ja `?` ennen ensimmäistä hakua
 * (*"fog of unknown"*). Players on 22.9.2026 alkaen pelkkä otsikko ilman lukua (Tommin
 * päätös, `docs/UI.md` › *Luvut otsikoissa*); edellisen illan suurin nähty sijoitus
 * poistui kertymineen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoungeScreen(
    state: LoungeUiState,
    join: JoinUiState,
    /** Ilmoittautumisen tai peruutuksen lopputulos (3.9.2026), ks. [SignupUiState]. */
    signup: SignupUiState,
    players: PlayerListUiState,
    /** Tournament Hall jaksona (21.9.2026), ks. [HallUiState]. */
    hall: HallUiState,
    /** Hallin aktiivisten määrä viime hausta, tai null ennen ensimmäistä hakua. */
    hallCount: Int?,
    /** Refresh loungelle ja auki oleville haetuille jaksoille. */
    onRefresh: () -> Unit,
    /** Asetusreitti, sama kuin otteluluettelon `Settings` (24.9.2026). */
    onOpenSettings: () -> Unit,
    /** Oma pelaaja Top Pagelta nimikylttiä varten, tai null ennen latausta. Ks. [ProfileAction]. */
    self: PlayerRef? = null,
    /** Retry pelaajalistalle: aktiivinen haku toistetaan, muuten lista haetaan. */
    onRefreshPlayers: () -> Unit,
    /** Retry hallille. */
    onRefreshHall: () -> Unit,
    onOpenPlayerListLink: (PlayerListLink) -> Unit,
    /** Porautuminen sivun omalla polulla: pelaajaprofiili tai turnaussivu. */
    onOpenPage: (String) -> Unit,
    onJoin: (LoungeInvitation) -> Unit,
    onDismissJoinResult: () -> Unit,
    onSignUp: (LoungeTournament) -> Unit,
    onCancelSignup: (LoungeTournament) -> Unit,
    onDismissSignupResult: () -> Unit,
    /**
     * Jaksojen taitto (21.9.2026): jokainen otsikko on kytkin, ja tila muistetaan
     * laitteella, ks. [LoungeViewModel.folds].
     */
    folds: LoungeFolds = LoungeFolds(),
    onTogglePlayers: () -> Unit = {},
    onToggleHall: () -> Unit = {},
    onToggleInvitations: () -> Unit = {},
    onToggleTournaments: () -> Unit = {},
    /**
     * Pelaajahaku (3.9.2026): onko loungella hakulomake, mikä haku on aktiivinen, ja
     * teot. Kenttä on ruudulla vain kun lomake on sivulla, ks. [LoungeViewModel.search].
     */
    searchAvailable: Boolean = false,
    search: String? = null,
    onSearch: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Vahvistusta odottava kutsu. Ruudun omaa tilaa eikä näkymämallin: kysymys on
    // olemassa vain niin kauan kuin dialogi on ruudulla.
    var confirming by remember { mutableStateOf<LoungeInvitation?>(null) }
    var confirmingSignup by remember { mutableStateOf<LoungeTournament?>(null) }
    // Listan alle lisätty sivuttaja (29.9.2026) vie uuden sivun alkuun: ilman tätä vieritys
    // jäi sivun loppuun, ja sivun 201–300 ensimmäinen näkyvä rivi oli 285. Ylälinkki ei
    // vieritä, koska lista alkaa jo sen alta. Players on Loungen ensimmäinen jakso, joten
    // alku on kohta 0 (laiteajo SM-T970 29.9.2026).
    val listState = rememberLazyListState()
    var pageTurnFromBottom by remember { mutableStateOf(false) }
    LaunchedEffect(players) {
        if (!pageTurnFromBottom) return@LaunchedEffect
        when (players) {
            is PlayerListUiState.Loaded -> if (!players.refreshing) {
                listState.scrollToItem(0)
                pageTurnFromBottom = false
            }
            PlayerListUiState.Loading -> Unit
            else -> pageTurnFromBottom = false
        }
    }

    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.lounge_title)) },
                actions = {
                    // NoPath-tilassa ei ole mitään haettavaa, joten nappi on poissa eikä
                    // harmaana.
                    ProfileAction(self, onOpenPage)
                    if (state !is LoungeUiState.NoPath) {
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.top_refresh))
                        }
                    }
                    // `Refresh` ja `Settings` parina jokaisella päänäkymällä (Tommin tilaus
                    // 24.9.2026): asetukset eivät riipu siitä saatiinko lounge haettua.
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.top_settings))
                    }
                },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            when (state) {
                LoungeUiState.NoPath -> CenteredNote(stringResource(R.string.lounge_no_path))

                LoungeUiState.Loading -> BusyCentered()

                is LoungeUiState.Failed -> RetryNote(
                    text = state.reason.text(),
                    onRetry = onRefresh,
                )

                LoungeUiState.NotALoungePage -> RetryNote(
                    text = stringResource(R.string.lounge_not_a_lounge),
                    onRetry = onRefresh,
                )

                // Uloskirjautuminen navigoi pois MainActivityssa; tässä ei näytetä mitään
                // sen sijaan että vilautettaisiin väärää tilaa.
                LoungeUiState.SessionExpired -> Unit

                is LoungeUiState.Loaded -> LoungeContent(
                    page = state.page,
                    join = join,
                    signup = signup,
                    refreshing = state.refreshing,
                    onAskJoin = { confirming = it },
                    onDismissJoinResult = onDismissJoinResult,
                    onAskSignUp = { confirmingSignup = it },
                    onCancelSignup = onCancelSignup,
                    onDismissSignupResult = onDismissSignupResult,
                    onOpenPage = onOpenPage,
                    folds = folds,
                    onToggleInvitations = onToggleInvitations,
                    onToggleTournaments = onToggleTournaments,
                    listState = listState,
                    playersSection = {
                        item {
                            FoldHeading(
                                text = stringResource(R.string.lounge_players_heading),
                                folded = folds.playersFolded,
                                onToggle = onTogglePlayers,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        if (!folds.playersFolded) {
                            playersItems(
                                state = players,
                                onRefresh = onRefreshPlayers,
                                onOpenLink = onOpenPlayerListLink,
                                onOpenLinkBelow = { link ->
                                    pageTurnFromBottom = true
                                    onOpenPlayerListLink(link)
                                },
                                onOpenPage = onOpenPage,
                                searchAvailable = searchAvailable,
                                search = search,
                                onSearch = onSearch,
                                onClearSearch = onClearSearch,
                            )
                        }
                    },
                    hallSection = {
                        item {
                            FoldHeading(
                                text = stringResource(
                                    R.string.lounge_hall_heading,
                                    hallCount?.toString() ?: stringResource(R.string.lounge_count_unknown),
                                ),
                                folded = folds.hallFolded,
                                onToggle = onToggleHall,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                            )
                        }
                        if (!folds.hallFolded) {
                            hallItems(state = hall, onRefresh = onRefreshHall, onOpenPage = onOpenPage)
                        }
                    },
                )
            }
        }
    }

    confirming?.let { invitation ->
        JoinConfirmDialog(
            invitation = invitation,
            onConfirm = {
                confirming = null
                onJoin(invitation)
            },
            onDismiss = { confirming = null },
        )
    }

    confirmingSignup?.let { tournament ->
        SignupConfirmDialog(
            tournament = tournament,
            onConfirm = {
                confirmingSignup = null
                onSignUp(tournament)
            },
            onDismiss = { confirmingSignup = null },
        )
    }
}

/**
 * Pelaajalista jaksona: hakukenttä, sivun omat linkit rivinä ja sata riviä kerrallaan.
 * `LazyListScope`n laajennus eikä oma sarake, koska jakso on loungen listan sisällä
 * (21.9.2026) ja pelaajarivit vierivät samassa listassa muiden jaksojen kanssa.
 *
 * **Linkit ovat sivun omalla sanallaan eikä luokiteltuina** (*Sort By Rating*,
 * *Next 100*). Lajittelijan ja sivuttajan ero olisi luettava linkin tekstistä, eli
 * arvattava, ja sivu kertoo sen itse paremmin kuin arvaus.
 */
@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.playersItems(
    state: PlayerListUiState,
    onRefresh: () -> Unit,
    onOpenLink: (PlayerListLink) -> Unit,
    /** Listan alapuolinen sivuttaja; kutsuja vierittää uuden sivun alkuun. */
    onOpenLinkBelow: (PlayerListLink) -> Unit,
    onOpenPage: (String) -> Unit,
    searchAvailable: Boolean,
    search: String?,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    // Hakukenttä on listan yläpuolella jokaisessa tilassa jossa lounge on luettu, myös
    // virhetilassa: epäonnistunut haku ei saa viedä kenttää mukanaan. Kentän teksti on
    // ruudun oma, ja lähetetty hakusana tulee näkymämallilta (`search`), jotta tulosrivi
    // sanoo mitä haettiin eikä mitä kenttään on sen jälkeen kirjoitettu.
    if (searchAvailable) {
        item { PlayerSearchRow(search = search, onSearch = onSearch, onClearSearch = onClearSearch) }
    }
    when (state) {
        PlayerListUiState.NoPath -> item { SectionNote(stringResource(R.string.players_no_path)) }

        PlayerListUiState.Loading -> item { SectionBusy() }

        is PlayerListUiState.Failed -> item {
            RetryNote(text = state.reason.text(), onRetry = onRefresh)
        }

        PlayerListUiState.NotAPlayerList -> item {
            RetryNote(text = stringResource(R.string.players_not_a_list), onRetry = onRefresh)
        }

        is PlayerListUiState.Loaded -> {
            item { ProgressSlot(active = state.refreshing) }
            if (state.list.links.isNotEmpty()) {
                item { PlayerListLinks(state.list.links, onOpenLink) }
            }
            if (state.list.players.isEmpty()) {
                item {
                    Text(
                        text = if (search == null) {
                            stringResource(R.string.players_empty)
                        } else {
                            stringResource(R.string.players_search_empty, search)
                        },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.list.players, contentType = { DgStripedRow }) { player ->
                PlayerRowView(player, onOpenPage)
                HorizontalDivider()
            }
            // Sivuttajat myös listan alle (Tommin tilaus 29.9.2026): sadan rivin lopusta ei
            // tarvitse vierittää takaisin ylös. Lajittelijat jäävät vain ylös.
            val paging = state.list.links.filter(::isPagingLink)
            if (state.list.players.isNotEmpty() && paging.isNotEmpty()) {
                item { PlayerListLinks(paging, onOpenLinkBelow) }
            }
            // Rajaus ääneen: lista on koko sivuston eikä pelaajan oma. Haun jälkeen rivi
            // sanoo mitä haettiin ja mihin rivin napautus vie.
            item {
                Text(
                    text = if (search == null) {
                        stringResource(R.string.players_note)
                    } else {
                        stringResource(R.string.players_search_results, search)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Pelaajalistan linkkirivi sivun omin sanoin. Rivittyvä eikä `Row`, ja se on mittauksen
 * sanelema (Pixel 8a, 411 dp): kolme Sort By -linkkiä täyttävät leveyden, jolloin `Next 100`
 * jäi kokonaan ruudun ulkopuolelle. Se on niistä neljästä juuri se jota ilman lista ei ole
 * selattavissa.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerListLinks(links: List<PlayerListLink>, onOpenLink: (PlayerListLink) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().dgReadingSurface().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        links.forEach { link ->
            TextButton(onClick = { onOpenLink(link) }) {
                Text(text = link.label, maxLines = 1)
            }
        }
    }
}

/**
 * Sivuttaja listan alle: sivun oma sana *Previous* tai *Next* (`Previous 100`, `Next 100`).
 * Tämä on ainoa kohta jossa linkki luokitellaan tekstistä. Tunnistamaton linkki jää vain
 * ylös, joten väärä arvaus ei piilota mitään.
 */
internal fun isPagingLink(link: PlayerListLink): Boolean =
    link.label.trim().let { it.startsWith("Previous", ignoreCase = true) || it.startsWith("Next", ignoreCase = true) }

/**
 * Tournament Hall jaksona (21.9.2026 illasta; oli porautumissivu `PageScreen`issä
 * 3.9.–21.9.). Aktiiviset ensin ja päättyneet perässä, molemmat omalla otsakkeellaan.
 *
 * **Järjestys on päinvastainen kuin sivulla**, jolla päättyneet ovat ensin. Aktiiviset
 * ovat se osa jota katsotaan, ja lista on pitkä (2848 riviä mittaushetkellä), joten
 * päättyneiden yli vierittäminen olisi ollut sivun rakenteen toistoa eikä lukemista.
 */
private fun LazyListScope.hallItems(
    state: HallUiState,
    onRefresh: () -> Unit,
    onOpenPage: (String) -> Unit,
) {
    when (state) {
        HallUiState.NoPath -> item { SectionNote(stringResource(R.string.hall_no_path)) }

        HallUiState.Loading -> item { SectionBusy() }

        is HallUiState.Failed -> item {
            RetryNote(text = state.reason.text(), onRetry = onRefresh)
        }

        HallUiState.NotAHall -> item {
            RetryNote(text = stringResource(R.string.hall_not_a_hall), onRetry = onRefresh)
        }

        is HallUiState.Loaded -> {
            val hall = state.hall
            item { ProgressSlot(active = state.refreshing) }
            item { SubHeading(stringResource(R.string.hall_active_heading, hall.active.size)) }
            if (hall.active.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.hall_empty),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(hall.active, contentType = { DgStripedRow }) { tournament ->
                HallRow(tournament, onOpenPage)
                HorizontalDivider()
            }
            item { SubHeading(stringResource(R.string.hall_finished_heading, hall.finished.size)) }
            items(hall.finished, contentType = { DgStripedRow }) { tournament ->
                HallRow(tournament, onOpenPage)
                HorizontalDivider()
            }
            item {
                Text(
                    text = stringResource(R.string.hall_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Jakson sisäinen väliotsikko: rakennetta eikä toimintaa, joten ei napautettava. */
@Composable
private fun SubHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Jakson huomautus otsikon alla; ei keskitetä koko ruutuun, koska jakso on listan osa. */
@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Jakson latausmerkki otsikon alla, samasta syystä listan osana eikä keskellä ruutua. */
@Composable
private fun SectionBusy() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        BusyCentered()
    }
}

/**
 * Pelaajahaku: kenttä, `Search` ja aktiivisen haun `Show all`.
 *
 * Lomake on sivun oma (`POST /bg/plist`, kenttä `like`), ja sivun sanoin haku on
 * alkuosahaku; kentän nimi sanoo saman. Nappi on pois päältä tyhjällä kentällä, koska
 * tyhjää hakua ei lähetetä (`PlayerSearchForm.write`). Haku ei ole peruuttamaton teko,
 * joten vahvistusta ei kysytä.
 *
 * **Kentässä on oma `Clear`** (Tommin tilaus 29.9.2026), sama kuin Infon hakukentässä. Se
 * tyhjentää tekstin ja aktiivisen haun kerralla. Käsin pyyhitty kenttä jätti edellisen haun
 * tulokset listaksi, koska tyhjää hakua ei lähetetä eikä pyyhkiminen ole teko.
 */
@Composable
private fun PlayerSearchRow(
    search: String?,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf(search.orEmpty()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(stringResource(R.string.players_search_label)) },
            trailingIcon = if (query.isNotEmpty() || search != null) {
                {
                    TextButton(onClick = {
                        query = ""
                        onClearSearch()
                    }) {
                        Text(stringResource(R.string.info_search_clear))
                    }
                }
            } else {
                null
            },
        )
        // Yläpehmuste on reunustetun kentän otsikon varaus (8 dp kehyksen yllä): ilman sitä
        // rivi keskitti napin koko kenttään eikä sen kehykseen, ja `Search` jäi kentän
        // `Clear`ia ylemmäs (Tommi 29.9.2026, SM-T970).
        Button(
            onClick = { onSearch(query) },
            enabled = query.isNotBlank(),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.players_search_action))
        }
    }
    if (search != null) {
        TextButton(
            onClick = {
                query = ""
                onClearSearch()
            },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(stringResource(R.string.players_search_clear))
        }
    }
}

@Composable
private fun PlayerRowView(player: PlayerRow, onOpenPage: (String) -> Unit) {
    val path = player.player.profilePath
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (path != null) Modifier.clickable { onOpenPage(path) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        player.rank?.let { rank ->
            Text(
                text = stringResource(R.string.players_rank, rank),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = player.player.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = playerRarityColor(player.player.userId, player.player.name) ?: Color.Unspecified,
            )
            Text(
                text = listOfNotNull(
                    player.ratingText?.let { stringResource(R.string.players_rating, it) },
                    player.experienceText?.let {
                        stringResource(R.string.players_experience, it)
                    },
                ).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hallin rivi, sama muoto kuin `PageScreen`in entisellä `HallRow`illa. */
@Composable
private fun HallRow(tournament: HallTournament, onOpenPath: (String) -> Unit) {
    val path = tournament.eventPath
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (path != null) Modifier.clickable { onOpenPath(path) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = tournament.name.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            color = tournamentRarityColor(tournament.eventId, tournament.name) ?: Color.Unspecified,
        )
        Text(
            text = listOfNotNull(
                tournament.dateText,
                tournament.winner?.name?.let { stringResource(R.string.hall_winner, it) },
            ).joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Vahvistus ennen lähetystä. Teksti nimeää pelimuodon ja vastustajan, jotta kysymys on
 * juuri siitä rivistä jota painettiin eikä yleinen "oletko varma".
 */
@Composable
private fun JoinConfirmDialog(
    invitation: LoungeInvitation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val what = listOfNotNull(
        invitation.variant,
        invitation.length?.let { stringResource(R.string.lounge_length_label, it) },
    ).joinToString(", ").ifEmpty { stringResource(R.string.lounge_title) }

    ConfirmDialog(
        title = stringResource(R.string.lounge_join_confirm_title),
        body = stringResource(
            R.string.lounge_join_confirm_body,
            what,
            invitation.player.displayName,
        ),
        confirmLabel = stringResource(R.string.lounge_join_confirm_ok),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun LoungeContent(
    page: LoungePage,
    join: JoinUiState,
    signup: SignupUiState,
    refreshing: Boolean,
    onAskJoin: (LoungeInvitation) -> Unit,
    onDismissJoinResult: () -> Unit,
    onAskSignUp: (LoungeTournament) -> Unit,
    onCancelSignup: (LoungeTournament) -> Unit,
    onDismissSignupResult: () -> Unit,
    onOpenPage: (String) -> Unit,
    folds: LoungeFolds,
    onToggleInvitations: () -> Unit,
    onToggleTournaments: () -> Unit,
    listState: LazyListState,
    /** Players ja Tournament Hall jaksoina listan alkuun; kutsuja kokoaa ne omista tiloistaan. */
    playersSection: LazyListScope.() -> Unit,
    hallSection: LazyListScope.() -> Unit,
) {
    val sending = join is JoinUiState.Joining || signup is SignupUiState.Sending
    Column(modifier = Modifier.fillMaxSize()) {
        ProgressSlot(active = refreshing || sending)

        // Lopputulos listan yläpuolella eikä sisällä, kuten keskeytyneet lähetykset
        // otteluluettelossa: sen näkeminen on tärkeintä juuri kun lista muuttui.
        JoinResultNotice(join = join, onDismiss = onDismissJoinResult)
        SignupResultNotice(signup = signup, onDismiss = onDismissSignupResult)

        DgLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            // Sivuston kokonaislistat ensin, koska ne olivat linkkirivinä listojen yllä
            // ennen jaksoja (21.9.2026 päivällä, syy `docs/UI.md`) ja järjestys säilyi.
            playersSection()
            hallSection()
            // Otsikot ovat kytkimiä ja kantavat rivimäärän (Tommin tilaus 21.9.2026):
            // kiinni ollessa lista on otsikon takana, ja luku kertoo paljonko siellä on
            // ilman avaamista. Merkki otsikon edessä (+ tai −) sanoo kumpaan suuntaan
            // napautus vie.
            item {
                FoldHeading(
                    text = stringResource(R.string.lounge_invitations_heading, page.invitations.size),
                    folded = folds.invitationsFolded,
                    onToggle = onToggleInvitations,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            if (!folds.invitationsFolded && page.invitations.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lounge_invitations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            val invitations = if (folds.invitationsFolded) emptyList() else page.invitations
            items(invitations, contentType = { DgStripedRow }) { invitation ->
                InvitationRow(
                    onOpenPage = onOpenPage,
                    invitation = invitation,
                    // Nappi puuttuu kokonaan kun sivu ei tarjoa Joinia, ei ole harmaana:
                    // sama sääntö kuin otteluluettelon Play-linkillä.
                    onJoin = invitation.joinPath?.let { { onAskJoin(invitation) } },
                    joining = sending,
                )
                HorizontalDivider()
            }

            item {
                FoldHeading(
                    text = stringResource(R.string.lounge_tournaments_heading, page.tournaments.size),
                    folded = folds.tournamentsFolded,
                    onToggle = onToggleTournaments,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            if (!folds.tournamentsFolded && page.tournaments.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lounge_tournaments_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            val tournaments = if (folds.tournamentsFolded) emptyList() else page.tournaments
            items(tournaments, contentType = { DgStripedRow }) { tournament ->
                TournamentRow(
                    tournament = tournament,
                    // Nappi puuttuu kokonaan kun rivillä ei ole linkkiä, samoin kuin
                    // Join: harmaa nappi väittäisi että teko on olemassa muttei sallittu.
                    onSignUp = tournament.signupPath?.let { { onAskSignUp(tournament) } },
                    onCancel = tournament.cancelPath?.let { { onCancelSignup(tournament) } },
                    sending = sending,
                    onOpenPage = onOpenPage,
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * Listan otsikko kytkimenä (21.9.2026). Koko rivi on napautettava, ja edessä on merkki
 * tilan mukaan (Tommin tarkennus samana iltana): `+` kiinni ollessa ja `−` auki ollessa,
 * koska pelkkä otsikko ei kertoisi että sen takana on jotain. Otsikkoteksti kantaa
 * hallin rivimäärän; Playersilla lukua ei ole (Tommin päätös 22.9.2026).
 */
@Composable
internal fun FoldHeading(
    text: String,
    folded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(if (folded) R.string.lounge_fold_show else R.string.lounge_fold_hide),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Ilmoittautumisen tai peruutuksen lopputulos. Sama muoto kuin [JoinResultNotice], ja
 * sama värisääntö: vain tuntematon lopputulos saa varoitusvärin.
 */
@Composable
private fun SignupResultNotice(signup: SignupUiState, onDismiss: () -> Unit) {
    val text = when (signup) {
        SignupUiState.Idle, is SignupUiState.Sending -> return
        is SignupUiState.Done -> when (signup.action) {
            SignupAction.SignUp ->
                stringResource(R.string.lounge_signup_done, signup.name.orEmpty())
            SignupAction.Cancel ->
                stringResource(R.string.lounge_signup_cancel_done, signup.name.orEmpty())
        }
        is SignupUiState.Unconfirmed -> when (signup.action) {
            SignupAction.SignUp -> stringResource(R.string.lounge_signup_unconfirmed)
            SignupAction.Cancel -> stringResource(R.string.lounge_signup_cancel_unconfirmed)
        }
        is SignupUiState.Failed -> when (signup.reason) {
            Failure.Offline -> stringResource(R.string.lounge_join_failed_offline)
            is Failure.Server ->
                stringResource(R.string.lounge_signup_failed_server, signup.reason.code)
            Failure.Sleeping -> stringResource(R.string.lounge_signup_failed_sleeping)
        }
    }
    val container = when (signup) {
        is SignupUiState.Unconfirmed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.lounge_dismiss))
            }
        }
    }
}

/**
 * Vahvistus ennen ilmoittautumista. Nimeää turnauksen, pelimuodon, pituuden ja
 * kierrokset, koska kierrosmäärä on se luku joka kertoo mihin sitoudutaan. Has Note
 * sanotaan ääneen: järjestäjän teksti on tarkoitettu luettavaksi ennen ilmoittautumista
 * (`SUBSTANSSI.md`), ja se on turnaussivulla johon rivi itse vie.
 */
@Composable
private fun SignupConfirmDialog(
    tournament: LoungeTournament,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val what = listOfNotNull(
        tournament.variant,
        tournament.length?.let { stringResource(R.string.lounge_length_label, it) },
        tournament.rounds?.let { stringResource(R.string.lounge_rounds_label, it) },
    ).joinToString(", ")
    val sentence = stringResource(R.string.lounge_signup_confirm_body, tournament.name.orEmpty(), what)
    val note = if (tournament.hasNote) stringResource(R.string.lounge_signup_confirm_note) else null
    val body = textWithTournamentColor(
        text = sentence + note?.let { "\n\n$it" }.orEmpty(),
        name = tournament.name,
        color = tournamentRarityColor(tournament.eventId, tournament.name, tournament.rounds),
    )

    ConfirmDialog(
        title = stringResource(R.string.lounge_signup_confirm_title),
        body = body,
        confirmLabel = stringResource(R.string.lounge_signup_confirm_ok),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun JoinResultNotice(join: JoinUiState, onDismiss: () -> Unit) {
    val text = when (join) {
        JoinUiState.Idle, JoinUiState.Joining -> return
        JoinUiState.Joined -> stringResource(R.string.lounge_join_done)
        JoinUiState.Unconfirmed -> stringResource(R.string.lounge_join_unconfirmed)
        JoinUiState.Changed -> stringResource(R.string.lounge_join_changed)
        is JoinUiState.Failed -> when (join.reason) {
            Failure.Offline -> stringResource(R.string.lounge_join_failed_offline)
            is Failure.Server ->
                stringResource(R.string.lounge_join_failed_server, join.reason.code)
            Failure.Sleeping -> stringResource(R.string.lounge_join_failed_sleeping)
        }
    }
    // Unconfirmed on ainoa jossa lopputulosta ei tiedetä, ja se saa varoitusvärin.
    // Verkkovirhe on tavallinen eikä vaarallinen: mitään ei lähtenyt.
    val container = when (join) {
        JoinUiState.Unconfirmed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.lounge_dismiss))
            }
        }
    }
}

@Composable
private fun InvitationRow(
    invitation: LoungeInvitation,
    onJoin: (() -> Unit)?,
    joining: Boolean,
    onOpenPage: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = listOfNotNull(
                    invitation.variant,
                    invitation.length?.let { stringResource(R.string.lounge_length_label, it) },
                ).joinToString(", "),
                style = MaterialTheme.typography.bodyLarge,
            )
            invitation.player.name?.let { name ->
                // Tarjoajan nimestä porautuu profiiliin. Se on tässä ruudussa erityisen
                // hyödyllinen: Join on peruuttamaton, ja rating on se mitä siitä haluaa
                // tietää etukäteen.
                val path = invitation.player.profilePath
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = playerRarityColor(invitation.player.userId, name) ?: Color.Unspecified,
                    modifier = path?.let { p -> Modifier.clickable { onOpenPage(p) } } ?: Modifier,
                )
            }
            invitation.timeout?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            invitation.comment?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (onJoin != null) {
            Button(onClick = onJoin, enabled = !joining) {
                Text(stringResource(R.string.lounge_join))
            }
        }
    }
}

@Composable
private fun TournamentRow(
    tournament: LoungeTournament,
    onSignUp: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    sending: Boolean,
    onOpenPage: (String) -> Unit,
) {
    val path = tournament.eventPath
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
    Column(
        modifier = Modifier
            .weight(1f)
            .then(if (path != null) Modifier.clickable { onOpenPage(path) } else Modifier),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val nameColor = tournamentRarityColor(tournament.eventId, tournament.name, tournament.rounds)
        val hasNote = if (tournament.hasNote) stringResource(R.string.lounge_has_note) else null
        Text(
            // Has Note sivun omalla sanalla: järjestäjän teksti odottaa turnaussivulla.
            // Väri koskee vain nimeä, koska Has Note ei ole osa turnausta (2.10.2026).
            text = buildAnnotatedString {
                tournament.name?.let { name ->
                    if (nameColor != null) withStyle(SpanStyle(color = nameColor)) { append(name) } else append(name)
                }
                hasNote?.let { if (length > 0) append(" "); append(it) }
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = listOfNotNull(
                tournament.variant,
                tournament.length?.let { stringResource(R.string.lounge_length_label, it) },
                tournament.rounds?.let { stringResource(R.string.lounge_rounds_label, it) },
            ).joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
        )
        // Otsikointi seuraa sivun omaa jakoa, ja se on mitattu eikä valittu: turnaustaulun
        // otsikkorivillä `Time` on `colspan=2`, eli pooli ja lisäys ovat molemmat Timea, ja
        // `Grace` on oma sarakkeensa. Kolme lukua, kaksi nimeä.
        //
        // Luvut jäävät sivun omaan `/`-merkintään Timen sisällä: sama notaatio on sivulla
        // toisaallakin, ottelutarjouksen Time Control -valikossa (`Once a Day (200/+4/24)`),
        // joten se on kohteen kirjoitusasu eikä tämän ruudun keksintö.
        val time = listOfNotNull(tournament.timeText, tournament.incrementText)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" / ")
        Text(
            text = listOfNotNull(
                time?.let { stringResource(R.string.lounge_time_label, it) },
                tournament.graceText?.let { stringResource(R.string.lounge_grace_label, it) },
            ).joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
        // Sivun oma sana napissa: Sign Up tai Cancel Signup, kumpi rivillä on. Ei
        // kumpaakaan kun rivillä ei ole linkkiä (ilmoittautuminen ei auki).
        when {
            onSignUp != null -> Button(onClick = onSignUp, enabled = !sending) {
                Text(stringResource(R.string.lounge_signup))
            }
            onCancel != null -> TextButton(onClick = onCancel, enabled = !sending) {
                Text(stringResource(R.string.lounge_signup_cancel))
            }
        }
    }
}

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

