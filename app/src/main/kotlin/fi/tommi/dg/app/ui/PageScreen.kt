package fi.tommi.dg.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.domain.EventEntry
import fi.tommi.dg.domain.EventPage
import fi.tommi.dg.domain.HallTournament
import fi.tommi.dg.domain.IgnoreForm
import fi.tommi.dg.domain.InviteChoice
import fi.tommi.dg.domain.InviteForm
import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.MatchLogPage
import fi.tommi.dg.domain.PlayerTournamentRow
import fi.tommi.dg.domain.PlayerTournaments
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.ProfilePage
import fi.tommi.dg.domain.ResignPage
import fi.tommi.dg.domain.SelectField
import fi.tommi.dg.domain.SiteHelpPage
import fi.tommi.dg.domain.SiteLinksPage
import fi.tommi.dg.domain.TournamentHall

/**
 * Porautumisruutu: pelaajaprofiili, turnaussivu ja pelaajan turnauslistat samassa ruudussa.
 *
 * ~~**Ruutu on lukutilassa kauttaaltaan.**~~ **Yhtä lukuun ottamatta 29.8.2026 alkaen:**
 * toisen pelaajan profiililta voi lähettää viestin sivun omalla `Quick message` -lomakkeella.
 * Kutsulomake ja `Ignore` ovat yhä ulkona rajauksena, eli ruutu ei ole muuttunut
 * lomakeruuduksi vaan sai yhden nimetyn teon (`PageViewModel.send`).
 *
 * **Kenttä ei ole näkyvissä itsestään** vaan avataan `Message`-napista, ja se on sama
 * ratkaisu kuin lautaruudun chat-kortilla (`docs/UI.md`). Peruste on kaksiosainen: profiili
 * on ensisijaisesti luettava sivu, ja peruuttamattoman teon kenttä ei kuulu ruudulle jolle
 * tullaan katsomaan ratingia. Avaaja on **reunustettu nappi rivistön yläpuolella**, ei linkki
 * sen alla: se on sivun oma teko toisin kuin naapurinsa, ja rivistön pituus on pelaajan oma
 * valinta, joten "lähellä yläreunaa" ei pitänyt paikkaansa täytetyllä profiililla.
 *
 * Muu toiminto on porautua eteenpäin: profiilista turnauslistoihin, listasta turnaukseen,
 * turnauksesta pelaajaan. Kaikki kohteet ovat sivun omia linkkejä, ja ne avataan samalla
 * reitillä ([PageRoute]) koska laji luetaan vasta vastauksesta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScreen(
    state: PageUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPath: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Kirjautuneen tunnus, jonka turnauskaavio korostaa. Null kun ei tiedossa. */
    selfName: String? = null,
    /** Omien turnausten lukema muistista, ks. [OwnTournamentCount]. Null kun ei ladattu. */
    ownTournamentCount: OwnTournamentCount? = null,
    // Lähetyksen osat ovat oletusarvollisia, koska viisi kuudesta sivusta ei lähetä mitään
    // eikä niiden kutsupaikkojen pidä joutua nimeämään kykyä jota niillä ei ole.
    draft: String = "",
    send: ReplyUiState = ReplyUiState.Idle,
    onDraftChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    // Kutsu ja sivuutus (3.9.2026): samat oletusarvot samasta syystä, vain profiili kantaa ne.
    action: ProfileActionUiState = ProfileActionUiState.Idle,
    onInvite: (InviteChoice) -> Unit = {},
    onIgnore: () -> Unit = {},
    onDismissAction: () -> Unit = {},
    // Luovutus (3.9.2026): vain luovutussivu kantaa lomakkeen, joten oletusarvot samoin.
    resign: ResignUiState = ResignUiState.Idle,
    onResign: (Set<MatchId>) -> Unit = {},
    onDismissResign: () -> Unit = {},
    // Vienti on samalla perusteella oletusarvollinen: vain profiili kantaa päättyneitä.
    export: ExportUiState = ExportUiState.Idle,
    onExport: (Match, ExportFormat) -> Unit = { _, _ -> },
    /**
     * Fraasinapit viestikentän alle, sama lista kuin muualla (Tommin havainto ensimmäisen
     * profiililta lähetetyn viestin jälkeen 3.9.2026: *"fraasit puuttui lähetys-näytöltä"*).
     * Ks. [PhraseRow] ja `PhraseBook`.
     */
    phrases: List<String> = emptyList(),
    onAddPhrase: (String) -> Unit = {},
    onRemovePhrase: (String) -> Unit = {},
) {
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
                        text = title(state),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.board_back))
                    }
                },
                actions = {
                    if (state is PageUiState.Loaded) {
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.top_refresh))
                        }
                    }
                },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            when (state) {
                PageUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                is PageUiState.Failed -> RetryNote(
                    text = state.reason.text(),
                    onRetry = onRefresh,
                )

                PageUiState.NotAKnownPage -> RetryNote(
                    text = stringResource(R.string.page_not_known),
                    onRetry = onRefresh,
                )

                // Uloskirjautuminen navigoi pois MainActivityssa.
                PageUiState.SessionExpired -> Unit

                is PageUiState.Loaded -> {
                    ProgressSlot(active = state.refreshing)
                    when (val content = state.content) {
                        is PageContent.Profile -> ProfileContent(
                            page = content.page,
                            onOpenPath = onOpenPath,
                            ownTournamentCount = ownTournamentCount,
                            draft = draft,
                            send = send,
                            onDraftChange = onDraftChange,
                            onSend = onSend,
                            action = action,
                            onInvite = onInvite,
                            onIgnore = onIgnore,
                            onDismissAction = onDismissAction,
                            export = export,
                            onExport = onExport,
                            phrases = phrases,
                            onAddPhrase = onAddPhrase,
                            onRemovePhrase = onRemovePhrase,
                        )
                        is PageContent.Event -> EventContent(content.page, selfName, onOpenPath)
                        is PageContent.Tournaments -> TournamentsContent(content.page, onOpenPath)
                        is PageContent.SiteHelp -> SiteHelpContent(content.page)
                        is PageContent.SiteLinks -> SiteLinksContent(content.page)
                        is PageContent.MatchLog -> MatchLogContent(content.page)
                        is PageContent.Hall -> HallContent(content.page, onOpenPath)
                        is PageContent.Resign -> ResignContent(
                            page = content.page,
                            resign = resign,
                            onResign = onResign,
                            onDismiss = onDismissResign,
                            onOpenPath = onOpenPath,
                        )
                    }
                }
            }
        }
    }
}

/** Otsikko tulee sisällöstä, koska reitti ei tiedä mitä sivua se avaa. */
@Composable
private fun title(state: PageUiState): String {
    val loaded = state as? PageUiState.Loaded ?: return stringResource(R.string.page_loading_title)
    return when (val content = loaded.content) {
        is PageContent.Profile -> content.page.player.name
        is PageContent.Event -> content.page.name
        is PageContent.Tournaments -> content.page.player.name
        // Sivuston omien sivujen nimet ovat sivuston omat sanat, ei sovelluksen.
        is PageContent.SiteHelp -> stringResource(R.string.page_site_help_title)
        is PageContent.SiteLinks -> stringResource(R.string.page_site_links_title)
        // Sivun oma h3 on turnaus ja kierros, eli juuri se konteksti jota linkillä haettiin.
        is PageContent.MatchLog -> content.page.heading.ifBlank { null }
        is PageContent.Hall -> stringResource(R.string.page_hall_title)
        is PageContent.Resign -> stringResource(R.string.page_resign_title)
    } ?: stringResource(R.string.page_loading_title)
}

@Composable
private fun ProfileContent(
    page: ProfilePage,
    onOpenPath: (String) -> Unit,
    ownTournamentCount: OwnTournamentCount?,
    draft: String,
    send: ReplyUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    action: ProfileActionUiState,
    onInvite: (InviteChoice) -> Unit,
    onIgnore: () -> Unit,
    onDismissAction: () -> Unit,
    export: ExportUiState,
    onExport: (Match, ExportFormat) -> Unit,
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
) {
    // Avain on lomakeosoite eikä pelaajan nimi: osoite on se joka kertoo kenelle viesti
    // menee, ja se on myös ainoa osa joka ei voi olla kahdella pelaajalla sama.
    var composerOpen by rememberSaveable(page.messageForm?.action) { mutableStateOf(false) }
    // Kutsulomake samalla avaimella: piilokenttä `player` on se joka kertoo kenelle.
    var inviteOpen by rememberSaveable(page.inviteForm?.hidden?.get("player")) { mutableStateOf(false) }
    var confirmingIgnore by remember { mutableStateOf(false) }
    var confirmingInvite by remember { mutableStateOf<InviteChoice?>(null) }
    val actionBusy = action is ProfileActionUiState.Sending
    // Resurssit luetaan LazyColumnin ulkopuolella, koska sen rakennuslohko ei ole composable.
    val exportNote = exportText(export)
    val actionNote = actionText(action)

    page.ignoreForm?.let { form ->
        if (confirmingIgnore) {
            IgnoreConfirmDialog(
                form = form,
                onConfirm = {
                    confirmingIgnore = false
                    onIgnore()
                },
                onDismiss = { confirmingIgnore = false },
            )
        }
    }
    page.inviteForm?.let { form ->
        confirmingInvite?.let { choice ->
            InviteConfirmDialog(
                form = form,
                player = page.player.displayName,
                choice = choice,
                onConfirm = {
                    confirmingInvite = null
                    onInvite(choice)
                },
                onDismiss = { confirmingInvite = null },
            )
        }
    }

    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = listOfNotNull(
                        page.ratingText?.let { stringResource(R.string.players_rating, it) },
                        page.experienceText?.let {
                            stringResource(R.string.players_experience, it)
                        },
                    ).joinToString(", "),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        // Viestilomake on vain toisen pelaajan profiililla; omalta se puuttuu sivulta
        // itseltään, joten ehto on lomakkeen olemassaolo eikä päättely siitä kenen sivu on.
        //
        // Paikka on rivistön yläpuolella eikä sen alla. Kuvaus lupasi alusta asti "lähellä
        // yläreunaa", mutta rivistön pituus on pelaajan oma valinta (Real Name, Location,
        // E-mail, Comment), joten lupaus piti vain lyhyillä profiileilla.
        // Sivun omat teot näyttävät napeilta, ja ehto luetaan lähteestä eikä sijainnista
        // (`docs/UI.md`, `Skip Game` muistutusten joukossa). Ne ovat lähetyksiä sivustolle,
        // kun taas naapuririvit ovat pelkkää porautumista, joten `LinkRow` antoi
        // peruuttamattomalle teolle saman ulkoasun kuin navigoinnille. Tommin havainto
        // laitteelta 29.8.2026: sen löytäminen vei hetken, eikä se ollut nappi.
        //
        // Kolme nappia samalla rivillä 3.9.2026 alkaen: viesti, kutsu ja sivuutus. Kaksi
        // jälkimmäistä olivat ulkona rajauksena 27.8.2026 alkaen, ja rajaus purettiin Tommin
        // tilauksesta. Sivuutuksen teksti on sivun oma, koska vain se kertoo suunnan.
        if (page.messageForm != null || page.inviteForm != null || page.ignoreForm != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (page.messageForm != null && !composerOpen) {
                        OutlinedButton(onClick = { composerOpen = true }, enabled = !actionBusy) {
                            Text(stringResource(R.string.page_message_open))
                        }
                    }
                    if (page.inviteForm != null && !inviteOpen) {
                        OutlinedButton(onClick = { inviteOpen = true }, enabled = !actionBusy) {
                            Text(stringResource(R.string.page_invite_open))
                        }
                    }
                    page.ignoreForm?.let { form ->
                        OutlinedButton(onClick = { confirmingIgnore = true }, enabled = !actionBusy) {
                            Text(form.label)
                        }
                    }
                }
            }
        }
        page.messageForm?.let { form ->
            if (composerOpen) {
                item {
                    MessageComposer(
                        recipient = page.player.name,
                        maxLength = form.maxLength,
                        draft = draft,
                        send = send,
                        onDraftChange = onDraftChange,
                        onSend = onSend,
                        phrases = phrases,
                        onAddPhrase = onAddPhrase,
                        onRemovePhrase = onRemovePhrase,
                    )
                }
            }
        }
        page.inviteForm?.let { form ->
            if (inviteOpen) {
                item {
                    InviteComposer(
                        form = form,
                        player = page.player.displayName,
                        enabled = !actionBusy,
                        onSend = { confirmingInvite = it },
                    )
                }
            }
        }
        // Teon lopputulos lomakkeiden alla eikä nappirivin alla (Tommin päätös 14.9.2026,
        // kaikki lomakkeet: ilmoitus napin alle). Kutsulomakkeen `Send` on lomakkeen
        // pohjalla, ja sen yllä piirretty lause olisi jäänyt lomakkeen taakse. Sivuutuksella
        // lomaketta ei ole, jolloin tämä on suoraan nappirivin alla kuten ennenkin.
        actionNote?.let { text ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (!actionBusy) {
                        TextButton(onClick = onDismissAction) {
                            Text(stringResource(R.string.page_action_dismiss))
                        }
                    }
                }
            }
        }
        items(page.fields, contentType = { DgStripedRow }) { field ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = field.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Kenttien sisältö on pelaajan itsensä kirjoittamaa ja voi olla mitä
                // tahansa, myös linkkejä. Ruutu näyttää sen tekstinä eikä avaa mitään.
                Text(text = field.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { HorizontalDivider() }
        page.activeTournamentsPath?.let { path ->
            // Lukema vain kun muistissa on juuri tämän polun lista, eli omalla profiililla
            // Tournaments-välilehden avaamisen jälkeen. Muuten rivi on ennallaan ilman
            // selitystä: avaaminen antaa lukeman, ja selitys kertoisi sovelluksen tilasta
            // eikä pelaajasta (Tommin päätös 10.9.2026).
            val count = ownTournamentCount?.takeIf { it.path == path }?.count
            item {
                LinkRow(
                    if (count == null) stringResource(R.string.page_active_tournaments)
                    else stringResource(R.string.page_active_tournaments_counted, count),
                ) { onOpenPath(path) }
            }
        }
        page.tournamentWinsPath?.let { path ->
            item { LinkRow(stringResource(R.string.page_tournament_wins)) { onOpenPath(path) } }
        }
        // Pelilista näytetään linkkinä vain kun taulukot puuttuvat, eli toisen pelaajan
        // parametrittomalla profiililla. Omalla profiililla ottelut ovat jo alla, ja
        // linkki tarjoaisi saman sisällön toiseen kertaan eri järjestyksessä.
        if (page.activeMatches.isEmpty() && page.finishedMatches.isEmpty()) {
            page.gamesPath?.let { path ->
                item { LinkRow(stringResource(R.string.page_show_games)) { onOpenPath(path) } }
            }
        }
        page.versusPath?.let { path ->
            item { LinkRow(stringResource(R.string.page_versus_you)) { onOpenPath(path) } }
        }
        if (page.activeMatches.isNotEmpty()) {
            item {
                PageHeading(
                    stringResource(R.string.page_active_matches, page.activeMatches.size)
                )
            }
            items(page.activeMatches, contentType = { DgStripedRow }) { match -> MatchRow(match, onOpenPath) }
        }
        if (page.finishedMatches.isNotEmpty()) {
            item {
                PageHeading(
                    stringResource(R.string.page_finished_matches, page.finishedMatches.size)
                )
            }
            // Haun tila yhtenä rivinä otsikon alla, vain kun jotain on tekeillä tai meni
            // pieleen. Onnistuminen ei näy tässä vaan jakovalikkona.
            exportNote?.let { text ->
                item {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            items(page.finishedMatches, contentType = { DgStripedRow }) { match -> MatchRow(match, onOpenPath, onExport) }
        }
        if (page.activeMatches.isEmpty() && page.finishedMatches.isEmpty() &&
            page.gamesPath == null
        ) {
            // Toisen pelaajan parametriton profiili ei kanna ottelutaulukoita (mitattu
            // 27.8.2026), mutta tavallisesti sillä on lajittelulinkki joka kantaa ne;
            // tämä varateksti näkyy vain kun sitäkään ei ole. Tyhjä sanotaan ääneen.
            item {
                Text(
                    text = stringResource(R.string.page_no_matches),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/**
 * Ottelurivi profiililla. Tapahtumasta porautuu turnaussivulle ja vastustajasta profiiliin,
 * eli molemmat ovat sivun omia linkkejä; rivi itse ei avaa lautaa, koska tämä ruutu voi
 * olla myös toisen pelaajan profiili.
 */
@Composable
private fun MatchRow(
    match: Match,
    onOpenPath: (String) -> Unit,
    onExport: ((Match, ExportFormat) -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val profilePath = match.opponent.profilePath
        val eventPath = match.eventPath
        Text(
            text = match.eventName.ifEmpty { stringResource(R.string.page_no_event) },
            style = MaterialTheme.typography.bodyLarge,
            modifier = if (eventPath != null) {
                Modifier.clickable { onOpenPath(eventPath) }
            } else {
                Modifier
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = listOfNotNull(
                    match.opponent.name?.takeIf { it.isNotEmpty() },
                    match.round?.let { stringResource(R.string.page_round, it) },
                    match.matchLength?.let { stringResource(R.string.lounge_length_label, it) },
                ).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .then(
                        profilePath?.let { path -> Modifier.clickable { onOpenPath(path) } }
                            ?: Modifier
                    ),
            )
            // Vain päättyneillä, koska vain niillä on sivun oma `Export`-linkki
            // (`docs/KOHDE.md`). Ehto luetaan rivistä eikä listasta, kuten muutkin rivin
            // linkit. Vastustajan rivillä eikä omalla (Tommin pyyntö 3.9.2026), koska
            // nappi omalla rivillään kasvatti rivin minimikorkeutensa verran. Alleviivaus
            // on lupaus toiminnasta (`docs/UI.md`, Alleviivaus on lupaus), ja tämä tekee
            // jotain toisin kuin naapurinsa jotka vain porautuvat.
            if (onExport != null && match.exportPath != null) {
                Text(
                    text = stringResource(R.string.page_export_share),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable { onExport(match, ExportFormat.MAT) },
                )
                // Toinen tiedosto samasta hausta: GNU Backgammonin `.sgf`, jossa merkityt
                // asemat kulkevat mukana (Tommin päätös 15.9.2026, `SgfExport`). Kaksi
                // linkkiä eikä valintaa, koska kumpikin on yksi napautus ja vastaanottaja
                // valitaan vasta jakovalikossa.
                Text(
                    text = stringResource(R.string.page_export_share_sgf),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable { onExport(match, ExportFormat.SGF) },
                )
            }
        }
    }
}

/** Viennin tila sanoina, tai null kun mitään ei ole sanottavana. */
@Composable
private fun exportText(export: ExportUiState): String? = when (export) {
    ExportUiState.Idle -> null
    ExportUiState.Fetching -> stringResource(R.string.page_export_fetching)
    is ExportUiState.Ready -> null
    ExportUiState.NotAnExport -> stringResource(R.string.page_export_not_a_file)
    ExportUiState.NotConvertible -> stringResource(R.string.page_export_not_convertible)
    ExportUiState.SetupPosition -> stringResource(R.string.page_export_setup_position)
    is ExportUiState.Failed -> export.reason.text()
    ExportUiState.SessionExpired -> stringResource(R.string.error_auth)
}

@Composable
private fun EventContent(page: EventPage, selfName: String?, onOpenPath: (String) -> Unit) {
    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        items(page.conditions, contentType = { DgStripedRow }) { condition ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = condition.heading,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = condition.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (page.rows == 0 || page.rounds.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.page_no_brackets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            item { BracketGrid(page, selfName, onOpenPath) }
        }
        item {
            Text(
                text = stringResource(R.string.page_bracket_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Kaavion ruudukon yksi rivi: ensimmäisen kierroksen solun korkeus. */
private val BRACKET_ROW = 36.dp

/** Yhden kierrossarakkeen leveys liitosviivoineen. */
private val BRACKET_COLUMN = 172.dp

/** Sarakkeen reunassa oleva kaista jossa liitosviivat kulkevat solusta seuraavaan. */
private val BRACKET_GAP = 14.dp

/**
 * Cup-kaavio sivun omassa muodossa: sarakkeet ovat kierroksia ja solun paikka ja korkeus
 * tulevat sivun `ROWSPAN`eista ([EventEntry.top], [EventRound.span]). Pariutus näkyy
 * viivoista, jotka piirretään ruudukosta eikä merkinnöistä: sarakkeen solu liittyy niihin
 * edellisen sarakkeen paikkoihin joiden rivit se kattaa, olipa paikassa pelaaja tai ei.
 * Tyhjä paikka ensimmäisellä kierroksella on vapaakierros, myöhemmällä tuleva ottelu, ja
 * molemmat näkyvät tyhjänä kuten sivulla.
 *
 * Kaavio ei käänny puhelimen leveyteen, joten se vierii vaakaan; korkeus on rivien
 * määrä kertaa [BRACKET_ROW], ja 64 pelaajan kaavio vierii pystyyn luettelon mukana.
 */
@Composable
private fun BracketGrid(page: EventPage, selfName: String?, onOpenPath: (String) -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        page.rounds.forEachIndexed { index, round ->
            val previousSpan = page.rounds.getOrNull(index - 1)?.span
            val isLast = index == page.rounds.lastIndex
            Column(modifier = Modifier.width(BRACKET_COLUMN)) {
                Text(
                    text = round.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = if (index > 0) BRACKET_GAP else 0.dp,
                        bottom = 4.dp,
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BRACKET_ROW * page.rows)
                        .drawBehind {
                            drawBracketLines(
                                rows = page.rows,
                                span = round.span,
                                previousSpan = previousSpan,
                                isLast = isLast,
                                color = lineColor,
                            )
                        },
                ) {
                    round.entries.forEach { entry ->
                        BracketCell(
                            entry = entry,
                            // Tunnus verrataan kirjainkoosta piittaamatta: sivu näyttää
                            // nimen omassa asussaan, kirjautumislomake hyväksyy minkä vain.
                            isSelf = selfName != null && entry.label.equals(selfName, ignoreCase = true),
                            onOpenPath = onOpenPath,
                            modifier = Modifier
                                .offset(y = BRACKET_ROW * entry.top)
                                .height(BRACKET_ROW * round.span)
                                .fillMaxWidth()
                                .padding(
                                    start = if (index > 0) BRACKET_GAP else 0.dp,
                                    end = if (isLast) 0.dp else BRACKET_GAP,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Liitosviivat yhdelle sarakkeelle. Vasemmassa kaistassa pystyviiva yhdistää edellisen
 * sarakkeen paikkojen keskipisteet ja vaakaviiva vie sen tämän sarakkeen soluun; oikeassa
 * kaistassa tynkä jatkaa solun keskeltä seuraavaan sarakkeeseen. Viivat piirretään
 * jokaiselle paikalle, myös tyhjälle, koska ruudukko on sivun oma eikä sovelluksen.
 */
private fun DrawScope.drawBracketLines(
    rows: Int,
    span: Int,
    previousSpan: Int?,
    isLast: Boolean,
    color: Color,
) {
    val rowPx = BRACKET_ROW.toPx()
    val gapPx = BRACKET_GAP.toPx()
    val stroke = 1.dp.toPx()
    val cellPx = rowPx * span
    val slots = rows / span
    for (slot in 0 until slots) {
        val top = slot * cellPx
        val centre = top + cellPx / 2
        if (previousSpan != null && previousSpan in 1 until span) {
            val childPx = rowPx * previousSpan
            val firstCentre = top + childPx / 2
            val lastCentre = top + cellPx - childPx / 2
            drawLine(color, Offset(0f, firstCentre), Offset(0f, lastCentre), stroke)
            drawLine(color, Offset(0f, centre), Offset(gapPx, centre), stroke)
        }
        if (!isLast) {
            drawLine(color, Offset(size.width - gapPx, centre), Offset(size.width, centre), stroke)
        }
    }
}

/**
 * Yksi kaavion solu. Pelaajasolu avaa profiilin, ottelusolu siirtolistan; sivun oma
 * muotoilu säilyy: ratkennut ottelu lihavoituna, kesken oleva kursiivilla. Oma tunnus
 * erottuu tekstin värillä (primary), jotta oma reitti kaaviossa löytyy yhdellä silmäyksellä
 * (Tommi 3.9.2026). Väri on tekstissä eikä taustassa, koska tausta on solun laji.
 */
@Composable
private fun BracketCell(
    entry: EventEntry,
    isSelf: Boolean,
    onOpenPath: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = entry.profilePath ?: entry.matchPath
    // Laatikko on korkeintaan kahden rivin korkuinen ja solun keskellä: 512 pelaajan
    // kaaviossa viidennen kierroksen solu on 16 riviä, eikä nimen laatikko saa venyä sen
    // mukana. Liitosviivat osuvat keskelle, joten keskitys pitää ne kiinni laatikossa.
    Box(modifier = modifier.padding(vertical = 2.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = BRACKET_ROW * 2 - 4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
                .then(if (target != null) Modifier.clickable { onOpenPath(target) } else Modifier)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.decided) FontWeight.Bold else null,
                fontStyle = if (entry.inProgress) FontStyle.Italic else null,
                color = when {
                    isSelf -> MaterialTheme.colorScheme.primary
                    entry.inProgress -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Luovutussivu sivun omassa muodossa: rivit valintaruutuineen, sivun oma varmistusruutu
 * sivun omalla tekstillä ja nappi sivun omalla tekstillä. Nappi on käytössä vasta kun
 * jokin rivi on valittu ja varmistusruutu ruksattu, eli samat ehdot jotka sivu itse
 * asettaa selaimessa. Omaa dialogia ei ole: luovutus on sivustolla tavallinen
 * valikkotoiminto (`SUBSTANSSI.md` kohta 36), ja sivun ruutu on se varmistus.
 *
 * Valinta ei säily ruudun uudelleenluonnin yli, ja se on tarkoitus: luovutus on kerran
 * tehtävä teko, ja vanha valinta uudella sivulla olisi väärä lähtökohta.
 */
@Composable
private fun ResignContent(
    page: ResignPage,
    resign: ResignUiState,
    onResign: (Set<MatchId>) -> Unit,
    onDismiss: () -> Unit,
    onOpenPath: (String) -> Unit,
) {
    var selected by remember(page) { mutableStateOf(emptySet<MatchId>()) }
    var confirmed by remember(page) { mutableStateOf(false) }
    val sending = resign is ResignUiState.Sending
    val note = resignText(resign)

    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { PageHeading(stringResource(R.string.page_resign_heading, page.rows.size)) }
        if (page.rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.page_resign_empty),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(page.rows, contentType = { DgStripedRow }) { row ->
            val match = row.match
            val checked = match.id in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !sending) {
                        selected = if (checked) selected - match.id else selected + match.id
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { on -> selected = if (on) selected + match.id else selected - match.id },
                    enabled = !sending,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.eventName.ifEmpty { stringResource(R.string.page_no_event) },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = listOfNotNull(
                            match.opponent.name?.takeIf { it.isNotEmpty() },
                            match.round?.let { stringResource(R.string.page_round, it) },
                            match.matchLength?.let { stringResource(R.string.lounge_length_label, it) },
                        ).joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                match.reviewPath?.let { path ->
                    TextButton(onClick = { onOpenPath(path) }, enabled = !sending) {
                        Text(stringResource(R.string.page_resign_review))
                    }
                }
            }
            HorizontalDivider()
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                page.confirmField?.let {
                    Row(
                        modifier = Modifier.clickable(enabled = !sending) { confirmed = !confirmed },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = confirmed, onCheckedChange = { confirmed = it }, enabled = !sending)
                        Text(page.confirmLabel ?: stringResource(R.string.page_resign_confirm_fallback))
                    }
                }
                Button(
                    onClick = { onResign(selected) },
                    enabled = !sending && confirmed && selected.isNotEmpty() && page.confirmField != null,
                ) {
                    Text(page.submitLabel)
                }
                // Lopputulos napin alla eikä otsikon alla (Tommin päätös 14.9.2026, kaikki
                // lomakkeet). Mitattu luovutuksella `sessio-14-9-luovutus`: rasti ja nappi
                // ovat luettelon pohjalla, ja otsikon alle piirretty `Matches resigned.
                // DailyGammon says: 1 match resigned.` ei ollut ruudulla kertaakaan, koska
                // luettelo oli 29 rivin päässä siitä. Lopputuloksen kertoi vain kadonnut
                // rivi. Ohjeteksti väistyy kun lause on olemassa, koska se lupaa saman
                // asian etukäteen.
                if (note != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (!sending) {
                            TextButton(onClick = onDismiss) { Text(stringResource(R.string.page_action_dismiss)) }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.page_resign_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun resignText(resign: ResignUiState): String? = when (resign) {
    ResignUiState.Idle -> null
    ResignUiState.Sending -> stringResource(R.string.page_action_sending)
    // Sivuston oma lause kun se on olemassa, muuten sovelluksen oma paattely kadonneista
    // riveista. Ks. [ResignUiState.Done]: todisteet ovat eri lahteista eivatka yhta vahvoja.
    is ResignUiState.Done -> resign.notice
        ?.let { stringResource(R.string.page_resign_done_notice, it) }
        ?: stringResource(R.string.page_resign_done, resign.count)
    ResignUiState.Unconfirmed -> stringResource(R.string.page_resign_unconfirmed)
    is ResignUiState.Failed -> resign.reason.text()
    ResignUiState.SessionExpired -> stringResource(R.string.error_auth)
}

@Composable
private fun TournamentsContent(page: PlayerTournaments, onOpenPath: (String) -> Unit) {
    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        if (page.rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.page_no_tournaments),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(page.rows, contentType = { DgStripedRow }) { row ->
            PlayerTournamentRowView(row, onOpenPath)
            HorizontalDivider()
        }
    }
}

/**
 * Tournament Hall porautumissivuna (3.9.2026 alkaen; oli Loungen segmentti 27.8.–3.9.).
 * Aktiiviset ensin ja päättyneet perässä, molemmat omalla otsakkeellaan.
 *
 * **Järjestys on päinvastainen kuin sivulla**, jolla päättyneet ovat ensin. Aktiiviset
 * ovat se osa jota katsotaan, ja lista on pitkä (2848 riviä mittaushetkellä), joten
 * päättyneiden yli vierittäminen olisi ollut sivun rakenteen toistoa eikä lukemista.
 */
@Composable
private fun HallContent(hall: TournamentHall, onOpenPath: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        DgLazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item { PageHeading(stringResource(R.string.hall_active_heading, hall.active.size)) }
            if (hall.active.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.hall_empty),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(hall.active, contentType = { DgStripedRow }) { tournament ->
                HallRow(tournament, onOpenPath)
                HorizontalDivider()
            }
            item { PageHeading(stringResource(R.string.hall_finished_heading, hall.finished.size)) }
            items(hall.finished, contentType = { DgStripedRow }) { tournament ->
                HallRow(tournament, onOpenPath)
                HorizontalDivider()
            }
        }
        Text(
            text = stringResource(R.string.hall_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

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
        Text(text = tournament.name.orEmpty(), style = MaterialTheme.typography.bodyLarge)
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
 * Pelaajan oman turnauslistan rivi. Jaettu Loungen Tournaments-segmentin kanssa
 * (3.9.2026), joka näyttää saman sivun samoina riveinä.
 *
 * [activeGameText] on käynnissä olevan ottelun kuvaus kun kutsuja tietää siitä enemmän kuin
 * sivu (Matches-välilehden Tournaments 15.9.2026: kierros, vastustaja ja vuoro luettelosta
 * ja ottelumuistista). Sivu itse sanoo vain että peli on, ja profiilin porautumisruudulla
 * se on koko tieto. Kuvaus on ensin ja voitot sen perään, koska kierros on Tommille
 * voittoja merkittävämpi.
 *
 * [showWins] on false kun kuvauksessa on jo kierros (Tommin tilaus 15.9.2026: *"jos näkyy
 * round-tieto, niin wins määrää ei enää tarvita"*). Kutsuja päättää, koska vain se tietää
 * mitä kuvaukseen meni.
 */
@Composable
internal fun PlayerTournamentRowView(
    row: PlayerTournamentRow,
    onOpenPath: (String) -> Unit,
    activeGameText: String? = null,
    showWins: Boolean = true,
) {
    val path = row.eventPath
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (path != null) Modifier.clickable { onOpenPath(path) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = row.name.orEmpty(), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = listOfNotNull(
                row.activeMatchPath?.let {
                    activeGameText ?: stringResource(R.string.page_your_turn_waiting)
                },
                row.winsText?.takeIf { showWins }?.let { stringResource(R.string.page_wins, it) },
                row.whenText,
            ).joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Sivuston oma Help luettuna: osastot väliotsikkoina, kysymykset ja vastaukset allekkain.
 *
 * Pelkkää lukua, eikä sivun sisäisiä ristiviittauksia voi seurata: jäsennin litistää
 * linkit tekstiksi (`SiteHelpEntry.answer`). Se on manuaalille riittävä muoto, ja raja
 * on sanottu ääneen domain-mallissa.
 */
@Composable
private fun SiteHelpContent(page: SiteHelpPage) {
    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        page.sections.forEach { section ->
            item { PageHeading(section.title) }
            items(section.entries, contentType = { DgStripedRow }) { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = entry.question,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(text = entry.answer, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { HorizontalDivider() }
        }
    }
}

/**
 * Ottelun siirtohistoria sivun omin sanoin (30.8.2026): pituusrivi, pelit väliotsikoineen
 * ja pistetilanteineen, vuorot kahtena sarakkeena kuten sivullakin. Solut ovat sivun omaa
 * tekstiä (`61: 13/7 8/7`, `Doubles => 2`), eikä ruutu lisää tulkintaa; tyhjä sarake on
 * sivun oma tyhjä (avausvuoro, tanssi). Siirtokohtaisia asemalinkkejä ei avata, koska
 * yksittäisen aseman sivu on lautasivu jota tämä ruutu ei piirrä.
 */
@Composable
private fun MatchLogContent(page: MatchLogPage) {
    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        page.matchLengthText?.let { length ->
            item {
                Text(
                    text = length,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        page.games.forEach { game ->
            item { PageHeading(game.title) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Spacer(modifier = Modifier.width(TURN_NUMBER_WIDTH))
                    Text(
                        text = game.scoreLeft,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = game.scoreRight,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            items(game.turns, contentType = { DgStripedRow }) { turn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = turn.number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(TURN_NUMBER_WIDTH),
                    )
                    Text(
                        text = turn.left,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = turn.right,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item { HorizontalDivider() }
        }
    }
}

/** Vuoronumeron sarake, sama leveys pistetilannerivillä jotta sarakkeet ovat kohdakkain. */
private val TURN_NUMBER_WIDTH = 40.dp

/**
 * Sivuston linkkisivu: ulkoisia osoitteita, joten rivin napautus avaa laitteen selaimen
 * eikä mitään tämän sovelluksen ruutua. Osoite näkyy rivillä ennen kuin sitä avataan,
 * jotta napautus ei ole hyppy tuntemattomaan.
 */
@Composable
private fun SiteLinksContent(page: SiteLinksPage) {
    val uriHandler = LocalUriHandler.current
    DgLazyColumn(modifier = Modifier.fillMaxSize()) {
        page.sections.forEach { section ->
            item { PageHeading(section.title) }
            items(section.links, contentType = { DgStripedRow }) { link ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(link.url) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Alleviivaus on lupaus, ja tässä se pitää: rivin napautus avaa
                    // selaimen. Muualla sovelluksessa näkyvät osoitteet ovat pelaajien
                    // itsensä kirjoittamia (profiilin kentät, palstan viestit), eikä
                    // niitä alleviivata eikä avata, ks. `docs/UI.md`.
                    Text(
                        text = link.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                    Text(
                        text = link.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    link.note?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

/**
 * Kutsulomake sivun omilla vaihtoehdoilla: kolme valintaa, kaksi tekstikenttää ja
 * valintaruutu, kaikki `InviteForm`ista eikä koodista. Esivalinnat ovat sivun omat
 * (`backgammon`, 5, `Never`). Lähetys menee vahvistusdialogin kautta, koska kutsu on
 * peruuttamaton teko oikealle ihmiselle, samoin kuin loungen Join.
 *
 * Tekstikenttien pituus rajataan sivun `maxlength`iin täällä eikä lähetyksessä, koska
 * palvelimen vastausta pidempään kenttään ei ole mitattu (viestikentästä se on).
 */
@Composable
internal fun InviteComposer(
    form: InviteForm,
    player: String,
    enabled: Boolean,
    onSend: (InviteChoice) -> Unit,
) {
    var variant by rememberSaveable { mutableStateOf(form.variant.default?.value.orEmpty()) }
    var length by rememberSaveable { mutableStateOf(form.length.default?.value.orEmpty()) }
    var timeControl by rememberSaveable { mutableStateOf(form.timeControl.default?.value.orEmpty()) }
    var comment by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // Esivalinta sivulta: profiilin kutsussa tyhjä, vastatarjouksessa kutsun oma arvo.
    var privateMatch by rememberSaveable { mutableStateOf(form.privateMatch?.checked == true) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.page_invite_confirm_title, player).trimEnd('?'),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        ChoiceMenu(stringResource(R.string.page_invite_variant), form.variant, variant, enabled) { variant = it }
        ChoiceMenu(stringResource(R.string.page_invite_length), form.length, length, enabled) { length = it }
        ChoiceMenu(stringResource(R.string.page_invite_time), form.timeControl, timeControl, enabled) { timeControl = it }
        form.comment?.let { field ->
            LimitedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = stringResource(R.string.page_invite_comment),
                maxLength = field.maxLength,
                enabled = enabled,
            )
        }
        form.name?.let { field ->
            LimitedTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.page_invite_name),
                maxLength = field.maxLength,
                enabled = enabled,
            )
        }
        form.privateMatch?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = privateMatch, onCheckedChange = { privateMatch = it }, enabled = enabled)
                Text(stringResource(R.string.page_invite_private))
            }
        }
        Button(
            onClick = {
                onSend(
                    InviteChoice(
                        variant = variant,
                        length = length,
                        timeControl = timeControl,
                        comment = comment,
                        name = name,
                        privateMatch = privateMatch,
                    )
                )
            },
            enabled = enabled && form.variant.offers(variant) && form.length.offers(length) &&
                form.timeControl.offers(timeControl),
        ) {
            Text(stringResource(R.string.page_invite_send))
        }
    }
}

/** Yksi sivun `<select>` alasvetovalikkona. Selite ja arvo ovat sivun omat. */
@Composable
private fun ChoiceMenu(
    label: String,
    field: SelectField,
    value: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, enabled = enabled) {
            Text("$label: ${field.label(value) ?: value}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            field.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        open = false
                        onChange(option.value)
                    },
                )
            }
        }
    }
}

/** Yksirivinen kenttä jonka pituus on rajattu sivun `maxlength`iin, laskuri näkyvissä. */
@Composable
private fun LimitedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int?,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> if (maxLength == null || text.length <= maxLength) onValueChange(text) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        supportingText = maxLength?.let { { Text(stringResource(R.string.page_invite_count, value.length, it)) } },
    )
}

@Composable
internal fun InviteConfirmDialog(
    form: InviteForm,
    player: String,
    choice: InviteChoice,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(R.string.page_invite_confirm_title, player),
        body = stringResource(
            R.string.page_invite_confirm_body,
            player,
            form.variant.label(choice.variant) ?: choice.variant,
            form.length.label(choice.length) ?: choice.length,
            form.timeControl.label(choice.timeControl) ?: choice.timeControl,
        ),
        confirmLabel = stringResource(R.string.page_invite_confirm_ok),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Sivuutuksen vahvistus. Otsikko on napin oma teksti, koska vain se kertoo suunnan
 * (`Ignore nimi` tai sen vastakohta), eikä sovellus tiedä mitä sivuutus tekee: vaikutusta
 * ei ole mitattu. Sävy on sivuston oma, tavallinen nappi (`SUBSTANSSI.md` kohta 36).
 */
@Composable
private fun IgnoreConfirmDialog(
    form: IgnoreForm,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = form.label,
        body = stringResource(R.string.page_ignore_confirm_body),
        confirmLabel = form.label,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** Kutsun tai sivuutuksen lopputulos yhtenä lauseena, tai null kun mitään ei ole tehty. */
@Composable
private fun actionText(action: ProfileActionUiState): String? = when (action) {
    ProfileActionUiState.Idle -> null
    is ProfileActionUiState.Sending -> stringResource(R.string.page_action_sending)
    is ProfileActionUiState.Done -> stringResource(R.string.page_ignore_done, action.player, action.label.orEmpty())
    is ProfileActionUiState.Sent -> action.notice
        ?.let { stringResource(R.string.page_invite_sent_notice, action.player, it) }
        ?: stringResource(R.string.page_invite_sent, action.player)
    is ProfileActionUiState.Answered -> action.notice
        ?.let { stringResource(R.string.page_invite_answered_notice, action.player, it) }
        ?: stringResource(R.string.page_invite_answered, action.player)
    is ProfileActionUiState.Unconfirmed -> stringResource(R.string.page_action_unconfirmed)
    is ProfileActionUiState.Failed -> action.reason.text()
    ProfileActionUiState.SessionExpired -> stringResource(R.string.error_auth)
}

/**
 * Viestin kirjoitus ja lähetys pelaajan profiililla.
 *
 * **Tämä on keskustelun aloitus**, eli se puoli jota sivusto ei säilytä ja jota ei voi
 * hakea jälkikäteen mistään. Lähetetty viesti menee arkistoon vasta kun sivusto on
 * kuitannut sen (`PageViewModel.confirm`), joten ruutu ei väitä lähettäneensä mitään mitä
 * se ei nähnyt menevän perille.
 *
 * Kolme valintaa on peritty viestiruudulta eikä keksitty tässä, koska teko on sama teko:
 * kenttä on monirivinen vaikka sivun oma on `INPUT` (80 merkkiä ei mahdu puhelimen yhdelle
 * riville), merkkilaskuri kertoo pituuden muttei lupaa rajaa, ja lähetysnappi on pois
 * käytöstä tyhjällä kentällä.
 *
 * Fraasinapit tulivat 3.9.2026 Tommin havainnosta ensimmäisen lähetyksen jälkeen
 * (*"fraasit puuttui lähetys-näytöltä"*). Aiempi rajaus sanoi ettei profiililta lähetettävä
 * viesti ole ottelun aloitus eikä lopetus, mutta lista on 3.9.2026 alkaen muokattava eikä
 * pelkkä `hi`/`gg`, ja sama kenttä on tarkoitettu samaan tekoon: rivi on sama komponentti
 * ja sama lista kuin viestiruudussa ja laudalla ([PhraseRow], `PhraseBook`).
 */
@Composable
private fun MessageComposer(
    recipient: String?,
    maxLength: Int?,
    draft: String,
    send: ReplyUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
) {
    val sending = send is ReplyUiState.Sending

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !sending,
            minLines = 3,
            maxLines = 8,
            label = {
                Text(
                    // Vastaanottaja on otsikossa eikä pelkkänä sanana `Message`: ruutu on
                    // sama kaikille pelaajille, ja lähetys menee sille jonka sivulla ollaan.
                    text = recipient?.let { stringResource(R.string.page_message_to, it) }
                        ?: stringResource(R.string.page_message_label),
                )
            },
            supportingText = {
                Text(stringResource(R.string.messages_reply_count, draft.length))
            },
        )

        PhraseRow(
            phrases = phrases,
            draft = draft,
            enabled = !sending,
            onDraftChange = onDraftChange,
            onAddPhrase = onAddPhrase,
            onRemovePhrase = onRemovePhrase,
        )

        // Sivun oma raja kerrotaan sanoina, koska sitä ei enää noudateta: palvelin rivittää
        // eikä leikkaa (mitattu 24.8.2026, `docs/KOHDE.md`). Luku on siis tieto siitä miten
        // viesti näkyy perillä, ei este täällä. Puuttuva raja jätetään sanomatta eikä
        // esitetä rajattomuutena.
        maxLength?.let {
            Text(
                text = stringResource(R.string.page_message_wrap, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(onClick = onSend, enabled = !sending && draft.isNotBlank()) {
            Text(stringResource(R.string.page_message_send))
        }

        // Lopputulos napin alla (Tommin päätös 14.9.2026, kaikki lomakkeet).
        sendOutcome(send)?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Lähetyksen lopputulos yhtenä lauseena, tai null kun mitään ei ole lähetetty.
 *
 * Tekstit ovat viestiruudun omia kolmea lukuun ottamatta: `Sent` sanoo täällä eri asian,
 * koska arkisto ei ole tämän ruudun alla vaan omalla välilehdellään.
 */
@Composable
private fun sendOutcome(send: ReplyUiState): String? = when (send) {
    ReplyUiState.Idle -> null
    ReplyUiState.Sending -> stringResource(R.string.messages_reply_sending)
    ReplyUiState.Sent -> stringResource(R.string.page_message_sent)
    ReplyUiState.Unconfirmed -> stringResource(R.string.messages_reply_unconfirmed)
    ReplyUiState.SessionExpired -> stringResource(R.string.error_auth)
    is ReplyUiState.Failed -> send.reason.text()
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(text = text)
    }
}

@Composable
private fun PageHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        // Aksenttiväri erottaa osaston sen omista riveistä. Väri on rakennetta eikä
        // toimintaa: tähän ei voi napauttaa, eikä otsikkoa siksi alleviivata.
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

