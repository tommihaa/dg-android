package fi.tommi.dg.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
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
 * johon mennään mukaan. Jäljelle jäivät tarjoukset ja pelaajalista, eivätkä kaksi kohdetta
 * ansaitse omaa riviään ruudun yläreunassa.
 *
 * Ruutu on siis tarjoukset, ja listan perässä on kaksi linkkiä sivuston omiin
 * kokonaislistoihin: pelaajalista ja Tournament Hall. Edellinen aukeaa tässä ruudussa
 * porautumisena (hakukenttä on sen omassa näkymässä), jälkimmäinen porautumissivuna.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoungeScreen(
    state: LoungeUiState,
    join: JoinUiState,
    /** Ilmoittautumisen tai peruutuksen lopputulos (3.9.2026), ks. [SignupUiState]. */
    signup: SignupUiState,
    players: PlayerListUiState,
    /** Refresh tarjouksille, eli loungen omalle sivulle. */
    onRefresh: () -> Unit,
    /** Refresh pelaajalistalle: aktiivinen haku toistetaan, muuten lista haetaan. */
    onRefreshPlayers: () -> Unit,
    /** Pelaajalista avattiin: haetaan kerran, ei joka avauksella. */
    onOpenPlayers: () -> Unit,
    onOpenPlayerListLink: (PlayerListLink) -> Unit,
    /** Porautuminen sivun omalla polulla: pelaajaprofiili tai turnaussivu. */
    onOpenPage: (String) -> Unit,
    onJoin: (LoungeInvitation) -> Unit,
    onDismissJoinResult: () -> Unit,
    onSignUp: (LoungeTournament) -> Unit,
    onCancelSignup: (LoungeTournament) -> Unit,
    onDismissSignupResult: () -> Unit,
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

    // Porautuminen paikallaan, sama muoto kuin Info-välilehdellä ja Discussionin ketjulla:
    // omalla reitillään välilehtipalkki katoaisi ja pelaajalista näyttäisi laudalta.
    // Valinta on `rememberSaveable`issa, jotta se säilyy välilehtien vaihdon yli.
    var showingPlayers by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showingPlayers) { showingPlayers = false }

    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = {
                    Text(
                        stringResource(
                            if (showingPlayers) R.string.lounge_players_link
                            else R.string.lounge_title
                        )
                    )
                },
                // Sovelluksen oma paluu porautumisesta, sama muoto kuin muilla ruuduilla.
                navigationIcon = {
                    if (showingPlayers) {
                        TextButton(onClick = { showingPlayers = false }) {
                            Text(stringResource(R.string.board_back))
                        }
                    }
                },
                actions = {
                    // Refresh koskee sitä listaa joka on auki; NoPath-tilassa ei ole mitään
                    // haettavaa, joten nappi on poissa eikä harmaana.
                    val canRefresh = if (showingPlayers) {
                        players !is PlayerListUiState.NoPath
                    } else {
                        state !is LoungeUiState.NoPath
                    }
                    if (canRefresh) {
                        TextButton(onClick = if (showingPlayers) onRefreshPlayers else onRefresh) {
                            Text(stringResource(R.string.top_refresh))
                        }
                    }
                },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            if (showingPlayers) {
                PlayersSection(
                    state = players,
                    onRefresh = onRefreshPlayers,
                    onOpenLink = onOpenPlayerListLink,
                    onOpenPage = onOpenPage,
                    searchAvailable = searchAvailable,
                    search = search,
                    onSearch = onSearch,
                    onClearSearch = onClearSearch,
                )
            } else {
                OffersSection(
                    state = state,
                    join = join,
                    signup = signup,
                    onRefresh = onRefresh,
                    onAskJoin = { confirming = it },
                    onDismissJoinResult = onDismissJoinResult,
                    onAskSignUp = { confirmingSignup = it },
                    onCancelSignup = onCancelSignup,
                    onDismissSignupResult = onDismissSignupResult,
                    onOpenPage = onOpenPage,
                    onOpenPlayers = {
                        showingPlayers = true
                        onOpenPlayers()
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

@Composable
private fun OffersSection(
    state: LoungeUiState,
    join: JoinUiState,
    signup: SignupUiState,
    onRefresh: () -> Unit,
    onAskJoin: (LoungeInvitation) -> Unit,
    onDismissJoinResult: () -> Unit,
    onAskSignUp: (LoungeTournament) -> Unit,
    onCancelSignup: (LoungeTournament) -> Unit,
    onDismissSignupResult: () -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenPlayers: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
            when (state) {
                LoungeUiState.NoPath -> CenteredNote(stringResource(R.string.lounge_no_path))

                LoungeUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

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
                    onOpenPlayers = onOpenPlayers,
                    join = join,
                    signup = signup,
                    refreshing = state.refreshing,
                    onAskJoin = onAskJoin,
                    onDismissJoinResult = onDismissJoinResult,
                    onAskSignUp = onAskSignUp,
                    onCancelSignup = onCancelSignup,
                    onDismissSignupResult = onDismissSignupResult,
                    onOpenPage = onOpenPage,
                )
            }
    }
}

/**
 * Pelaajalista: sata riviä kerrallaan ja sivun omat linkit rivinä listan yllä.
 *
 * **Linkit ovat sivun omalla sanallaan eikä luokiteltuina** (*Sort By Rating*,
 * *Next 100*). Lajittelijan ja sivuttajan ero olisi luettava linkin tekstistä, eli
 * arvattava, ja sivu kertoo sen itse paremmin kuin arvaus.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayersSection(
    state: PlayerListUiState,
    onRefresh: () -> Unit,
    onOpenLink: (PlayerListLink) -> Unit,
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
        PlayerSearchRow(search = search, onSearch = onSearch, onClearSearch = onClearSearch)
    }
    when (state) {
        PlayerListUiState.NoPath -> CenteredNote(stringResource(R.string.players_no_path))

        PlayerListUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is PlayerListUiState.Failed -> RetryNote(
            text = state.reason.text(),
            onRetry = onRefresh,
        )

        PlayerListUiState.NotAPlayerList -> RetryNote(
            text = stringResource(R.string.players_not_a_list),
            onRetry = onRefresh,
        )

        is PlayerListUiState.Loaded -> Column(modifier = Modifier.fillMaxSize()) {
            ProgressSlot(active = state.refreshing)
            if (state.list.links.isNotEmpty()) {
                // Rivittyvä eikä `Row`, ja se on mittauksen sanelema (Pixel 8a, 411 dp):
                // kolme Sort By -linkkiä täyttävät leveyden, jolloin `Next 100` jäi
                // kokonaan ruudun ulkopuolelle. Se on niistä neljästä juuri se jota
                // ilman lista ei ole selattavissa.
                FlowRow(
                    modifier = Modifier.fillMaxWidth().dgReadingSurface().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.list.links.forEach { link ->
                        TextButton(onClick = { onOpenLink(link) }) {
                            Text(text = link.label, maxLines = 1)
                        }
                    }
                }
            }
            DgLazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
            }
            // Rajaus ääneen: lista on koko sivuston eikä pelaajan oma. Haun jälkeen rivi
            // sanoo mitä haettiin ja mihin rivin napautus vie.
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

/**
 * Pelaajahaku: kenttä, `Search` ja aktiivisen haun `Show all`.
 *
 * Lomake on sivun oma (`POST /bg/plist`, kenttä `like`), ja sivun sanoin haku on
 * alkuosahaku; kentän nimi sanoo saman. Nappi on pois päältä tyhjällä kentällä, koska
 * tyhjää hakua ei lähetetä (`PlayerSearchForm.write`). Haku ei ole peruuttamaton teko,
 * joten vahvistusta ei kysytä.
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
        )
        Button(onClick = { onSearch(query) }, enabled = query.isNotBlank()) {
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
            Text(text = player.player.displayName, style = MaterialTheme.typography.bodyLarge)
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
    onOpenPlayers: () -> Unit,
    join: JoinUiState,
    signup: SignupUiState,
    refreshing: Boolean,
    onAskJoin: (LoungeInvitation) -> Unit,
    onDismissJoinResult: () -> Unit,
    onAskSignUp: (LoungeTournament) -> Unit,
    onCancelSignup: (LoungeTournament) -> Unit,
    onDismissSignupResult: () -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val sending = join is JoinUiState.Joining || signup is SignupUiState.Sending
    Column(modifier = Modifier.fillMaxSize()) {
        ProgressSlot(active = refreshing || sending)

        // Lopputulos listan yläpuolella eikä sisällä, kuten keskeytyneet lähetykset
        // otteluluettelossa: sen näkeminen on tärkeintä juuri kun lista muuttui.
        JoinResultNotice(join = join, onDismiss = onDismissJoinResult)
        SignupResultNotice(signup = signup, onDismiss = onDismissSignupResult)

        DgLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = stringResource(R.string.lounge_invitations_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (page.invitations.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lounge_invitations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(page.invitations, contentType = { DgStripedRow }) { invitation ->
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
                Text(
                    text = stringResource(R.string.lounge_tournaments_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp),
                )
            }
            if (page.tournaments.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lounge_tournaments_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(page.tournaments, contentType = { DgStripedRow }) { tournament ->
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

            // Kaksi sivuston omaa kokonaislistaa listan perässä, eivät segmentteinä
            // (4.9.2026). Kumpikaan ei ole rinnakkainen tarjousten kanssa: tarjoukset
            // ovat minulle osoitettuja, nämä ovat kaikkien.
            item {
                LinkRow(
                    label = stringResource(R.string.lounge_players_link),
                    summary = stringResource(R.string.lounge_players_summary),
                    onClick = onOpenPlayers,
                )
            }
            page.tournamentHallPath?.let { hallPath ->
                item {
                    LinkRow(
                        label = stringResource(R.string.own_tournaments_hall_link),
                        summary = stringResource(R.string.own_tournaments_hall_summary),
                        onClick = { onOpenPage(hallPath) },
                    )
                }
            }
        }
    }
}

/**
 * Listan perässä oleva linkki sivuston omaan kokonaislistaan. Sivun oma sana ja selite sen
 * alla, sama muoto kuin Info-välilehden riveillä.
 */
@Composable
private fun LinkRow(label: String, summary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val body = buildString {
        append(stringResource(R.string.lounge_signup_confirm_body, tournament.name.orEmpty(), what))
        if (tournament.hasNote) {
            append("\n\n")
            append(stringResource(R.string.lounge_signup_confirm_note))
        }
    }

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
        Text(
            text = listOfNotNull(
                tournament.name,
                // Has Note sivun omalla sanalla: järjestäjän teksti odottaa turnaussivulla.
                if (tournament.hasNote) stringResource(R.string.lounge_has_note) else null,
            ).joinToString(" "),
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

