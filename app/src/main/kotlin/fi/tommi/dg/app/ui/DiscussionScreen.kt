package fi.tommi.dg.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.domain.ForumArchivePage
import fi.tommi.dg.domain.ForumBoard
import fi.tommi.dg.domain.ForumComposePage
import fi.tommi.dg.domain.ForumIndex
import fi.tommi.dg.domain.ForumMonthLink
import fi.tommi.dg.domain.ForumPost
import fi.tommi.dg.domain.ForumThread

/**
 * Keskustelupalsta: ketjulista, ketjun luku ja 3.9.2026 alkaen kirjoittaminen sivun omilla
 * lomakkeilla (uusi ketju indeksin `Add a New Thread` -linkistä, kommentti ketjun
 * `Add a Comment` -linkistä). ~~Kirjoittaminen jää selaimeen, ja se sanotaan ruudulla
 * ääneen (`discussion_read_only`).~~ Rajaus purettu, ks. `docs/UI.md`.
 *
 * **Lähetystä edeltää vahvistusdialogi**, ja se on `SUBSTANSSI.md`:n kohdan 59 vaatimus:
 * palstan viestiä ei voi muokata eikä poistaa itse, joten sovellus tarjoaa sen ainoan mitä
 * voi, hetken ennen lähettämistä. Lomake on neljäs kerros ketjun ja arkiston rinnalla, ja
 * järjestelmä-back sulkee sen ensimmäisenä.
 *
 * Ketju avautuu paikalleen samaan välilehteen eikä omaksi reitikseen: ketjun polku on
 * sivulta luettu eikä kulje reitin yli (ks. `DISCUSSION_ROUTE`). Järjestelmä-back sulkee
 * ensin ketjun ja vasta sitten poistuu välilehdeltä, ja sen hoitaa [BackHandler] joka on
 * käytössä vain ketjun ollessa auki.
 *
 * **Palstat ovat segmenttirivinä listan yllä** (Tommin päätös 26.8.2026, `docs/UI.md`):
 * General ja Politics ovat rinnakkaisia ja molemmat näkyvissä yhden napautuksen päässä.
 * Rivi tulee sivun omalta palstariviltä nimineen, joten sovellus ei nimeä palstoja itse
 * eikä tiedä niitä olevan kaksi.
 *
 * **Kuukausiarkisto on kolmas kerros indeksin ja ketjun rinnalla** (Tommin havainto
 * 29.8.2026: molemmat palstat näyttivät vain uusimmat). Indeksi on sivustolla kuluvan
 * hetken lista, ja vanhempiin pääsee vain sivun omalla `Old Threads` -linkillä. Arkisto
 * avautuu indeksin päälle eikä sen tilalle, joten paluu ei hae indeksiä uudelleen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(
    state: DiscussionUiState,
    thread: ThreadUiState?,
    archive: ArchiveUiState?,
    boards: List<ForumBoard>,
    selectedBoard: String?,
    onRefresh: () -> Unit,
    onSelectBoard: (ForumBoard) -> Unit,
    onOpenThread: (ForumThread) -> Unit,
    onCloseThread: () -> Unit,
    onOpenArchive: (String) -> Unit,
    onOpenMonth: (ForumMonthLink) -> Unit,
    onCloseArchive: () -> Unit,
    modifier: Modifier = Modifier,
    compose: ComposeUiState? = null,
    post: PostUiState = PostUiState.Idle,
    draftTitle: String = "",
    draftComment: String = "",
    onDraftTitleChange: (String) -> Unit = {},
    onDraftCommentChange: (String) -> Unit = {},
    onOpenNewThread: () -> Unit = {},
    onOpenComment: () -> Unit = {},
    onCloseCompose: () -> Unit = {},
    onPost: () -> Unit = {},
    onDismissPost: () -> Unit = {},
) {
    // Järjestys on kerrosten järjestys: ketju sulkeutuu ensin, arkisto sitten, ja vasta
    // sen jälkeen back poistuu välilehdeltä.
    BackHandler(enabled = compose != null || thread != null || archive != null) {
        when {
            compose != null -> onCloseCompose()
            thread != null -> onCloseThread()
            else -> onCloseArchive()
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
                title = {
                    Text(
                        when {
                            compose is ComposeUiState.Loaded ->
                                compose.page.heading ?: composeTitle(compose.kind)
                            compose != null -> composeTitle(compose.kind)
                            thread is ThreadUiState.Loading -> thread.origin.title
                            thread is ThreadUiState.Loaded ->
                                thread.page.title ?: stringResource(R.string.discussion_title)
                            thread != null -> stringResource(R.string.discussion_title)
                            archive is ArchiveUiState.Loaded ->
                                archive.page.monthLabel
                                    ?: stringResource(R.string.discussion_archive)
                            archive is ArchiveUiState.Loading ->
                                archive.monthLabel ?: stringResource(R.string.discussion_archive)
                            archive != null -> stringResource(R.string.discussion_archive)
                            else -> stringResource(R.string.discussion_title)
                        }
                    )
                },
                navigationIcon = {
                    if (compose != null || thread != null || archive != null) {
                        TextButton(
                            onClick = when {
                                compose != null -> onCloseCompose
                                thread != null -> onCloseThread
                                else -> onCloseArchive
                            },
                            enabled = post !is PostUiState.Sending,
                        ) {
                            Text(stringResource(R.string.board_back))
                        }
                    }
                },
                actions = {
                    if (compose == null && thread == null && archive == null &&
                        state !is DiscussionUiState.NoPath
                    ) {
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.top_refresh))
                        }
                    }
                },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            // Lähetyksen tulos näkyy siellä missä käyttäjä on: lomakkeella kun se on auki
            // (virhe jättää lomakkeen auki tekstiineen), muuten listan tai ketjun yllä.
            if (compose == null) {
                PostNote(post = post, onDismiss = onDismissPost)
            }
            if (compose != null) {
                ComposeView(
                    compose = compose,
                    post = post,
                    draftTitle = draftTitle,
                    draftComment = draftComment,
                    onDraftTitleChange = onDraftTitleChange,
                    onDraftCommentChange = onDraftCommentChange,
                    onPost = onPost,
                    onDismissPost = onDismissPost,
                    onClose = onCloseCompose,
                )
            } else if (thread != null) {
                ThreadView(thread = thread, onClose = onCloseThread, onOpenComment = onOpenComment)
            } else if (archive != null) {
                ArchiveView(
                    archive = archive,
                    onOpenThread = onOpenThread,
                    onOpenMonth = onOpenMonth,
                    onClose = onCloseArchive,
                )
            } else {
                // Rivi on [IndexView]n ulkopuolella, jotta se pysyy paikallaan myös
                // palstan latautuessa: silloin indeksiä ei ole, mutta valinta on.
                if (boards.size > 1) {
                    BoardRow(
                        boards = boards,
                        selected = selectedBoard,
                        onSelect = onSelectBoard,
                    )
                }
                IndexView(
                    state = state,
                    onRefresh = onRefresh,
                    onOpenThread = onOpenThread,
                    onOpenArchive = onOpenArchive,
                    onOpenNewThread = onOpenNewThread,
                )
            }
        }
    }
}

@Composable
private fun composeTitle(kind: ComposeKind): String = when (kind) {
    ComposeKind.NEW_THREAD -> stringResource(R.string.discussion_new_thread)
    ComposeKind.COMMENT -> stringResource(R.string.discussion_add_comment)
}

/**
 * Palstavalinta sivun omalla sanalla. Valittu palsta on se jolla ei ole polkua, ja sen
 * napautus ei tee mitään; [selected] on nimenä eikä indeksinä, koska rivin pituus ja
 * järjestys tulevat sivulta.
 */
@Composable
private fun BoardRow(
    boards: List<ForumBoard>,
    selected: String?,
    onSelect: (ForumBoard) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        boards.forEachIndexed { index, board ->
            SegmentedButton(
                selected = board.name == selected,
                onClick = { onSelect(board) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = boards.size),
            ) {
                Text(text = board.name, maxLines = 1)
            }
        }
    }
}

@Composable
private fun IndexView(
    state: DiscussionUiState,
    onRefresh: () -> Unit,
    onOpenThread: (ForumThread) -> Unit,
    onOpenArchive: (String) -> Unit,
    onOpenNewThread: () -> Unit,
) {
    when (state) {
        DiscussionUiState.NoPath -> CenteredNote(stringResource(R.string.discussion_no_path))

        DiscussionUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is DiscussionUiState.Failed -> RetryNote(
            text = state.reason.text(),
            onRetry = onRefresh,
        )

        DiscussionUiState.NotAForumPage -> RetryNote(
            text = stringResource(R.string.discussion_not_a_forum),
            onRetry = onRefresh,
        )

        // Uloskirjautuminen navigoi pois MainActivityssa.
        DiscussionUiState.SessionExpired -> Unit

        is DiscussionUiState.Loaded -> ThreadList(
            index = state.index,
            refreshing = state.refreshing,
            onOpenThread = onOpenThread,
            onOpenArchive = onOpenArchive,
            onOpenNewThread = onOpenNewThread,
        )
    }
}

@Composable
private fun ThreadList(
    index: ForumIndex,
    refreshing: Boolean,
    onOpenThread: (ForumThread) -> Unit,
    onOpenArchive: (String) -> Unit,
    onOpenNewThread: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ProgressSlot(active = refreshing)
        DgLazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (index.threads.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.discussion_empty),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(index.threads, contentType = { DgStripedRow }) { thread ->
                ThreadRow(thread = thread, onOpen = { onOpenThread(thread) })
                HorizontalDivider()
            }
            // Sivun omat linkit listan perässä samassa järjestyksessä kuin sivulla:
            // uusi ketju ja arkisto. Kumpikin vain jos sivu tarjoaa linkin.
            index.newThreadPath?.let {
                item {
                    LinkRow(text = stringResource(R.string.discussion_new_thread), onClick = onOpenNewThread)
                }
            }
            index.archivePath?.let { path ->
                item {
                    LinkRow(text = stringResource(R.string.discussion_archive), onClick = { onOpenArchive(path) })
                }
            }
        }
    }
}

/** Sivun oma linkki listan rivinä, esim. `Add a New Thread` tai `Old Threads`. */
@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
    HorizontalDivider()
}

@Composable
private fun ThreadRow(thread: ForumThread, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (thread.isNew) {
                // Sivuston oma lukemattoman merkki, sivuston omalla sanalla.
                Text(
                    text = stringResource(R.string.discussion_new),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(text = thread.title, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = listOfNotNull(
                thread.postCount?.let { stringResource(R.string.discussion_posts_count, it) },
                thread.poster?.let { stringResource(R.string.discussion_started_by, it) },
                // Vain kuukausisivulla; indeksissä aikasaraketta ei ole lainkaan.
                thread.postedAtText,
            ).joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Kuukausiarkisto: kuukausivalinta ja sen kuukauden ketjut.
 *
 * Kuukaudet ovat vaakarivinä uusin ensin, koska sivun oma luettelo on siinä järjestyksessä
 * ja koska niitä on paljon: mittaus 29.8.2026 antoi 272 kuukautta, tammikuusta 2004 alkaen.
 * Rivi on siis pitkä tarkoituksella, ja lähin menneisyys on ilman vieritystä.
 */
@Composable
private fun ArchiveView(
    archive: ArchiveUiState,
    onOpenThread: (ForumThread) -> Unit,
    onOpenMonth: (ForumMonthLink) -> Unit,
    onClose: () -> Unit,
) {
    when (archive) {
        is ArchiveUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is ArchiveUiState.Failed -> RetryNote(
            text = archive.reason.text(),
            onRetry = onClose,
            retryLabel = stringResource(R.string.discussion_archive_back),
        )

        ArchiveUiState.NotAnArchive -> RetryNote(
            text = stringResource(R.string.discussion_not_an_archive),
            onRetry = onClose,
            retryLabel = stringResource(R.string.discussion_archive_back),
        )

        is ArchiveUiState.Loaded -> ArchiveContent(
            page = archive.page,
            onOpenThread = onOpenThread,
            onOpenMonth = onOpenMonth,
        )
    }
}

@Composable
private fun ArchiveContent(
    page: ForumArchivePage,
    onOpenThread: (ForumThread) -> Unit,
    onOpenMonth: (ForumMonthLink) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Kuukausirivi on listan yläpuolella eikä sen sisällä, joten lukupinta on annettava
        // sille erikseen (6.9.2026); lista saa omansa `DgLazyColumn`ista.
        if (page.months.isNotEmpty()) Column(modifier = Modifier.fillMaxWidth().dgReadingSurface()) {
            Text(
                text = stringResource(R.string.discussion_archive_months),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(page.months, contentType = { DgStripedRow }) { month ->
                    FilterChip(
                        // Valinta luetaan sivun omasta otsikosta eikä pidetä kirjaa:
                        // `/month` ilman vuotta ja `/month/2026/8` ovat sama kuukausi,
                        // eikä polkujen vertailu tunnistaisi sitä.
                        selected = page.monthLabel?.endsWith(month.label) == true,
                        onClick = { onOpenMonth(month) },
                        label = { Text(month.label, maxLines = 1) },
                    )
                }
            }
            HorizontalDivider()
        }
        DgLazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (page.threads.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.discussion_archive_empty),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(page.threads, contentType = { DgStripedRow }) { thread ->
                ThreadRow(thread = thread, onOpen = { onOpenThread(thread) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ThreadView(thread: ThreadUiState, onClose: () -> Unit, onOpenComment: () -> Unit) {
    when (thread) {
        is ThreadUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is ThreadUiState.Failed -> RetryNote(
            text = thread.reason.text(),
            // Uudelleenyritys on paluu listalle: avaus on listan rivin teko, ja rivi
            // tietää polun jonka tämä tila on jo hukannut.
            onRetry = onClose,
            retryLabel = stringResource(R.string.discussion_thread_back),
        )

        ThreadUiState.NotAThread -> RetryNote(
            text = stringResource(R.string.discussion_not_a_thread),
            onRetry = onClose,
            retryLabel = stringResource(R.string.discussion_thread_back),
        )

        is ThreadUiState.Loaded -> DgLazyColumn(modifier = Modifier.fillMaxSize()) {
            items(thread.page.posts, contentType = { DgStripedRow }) { post ->
                PostView(post)
                HorizontalDivider()
            }
            // Sivun oma `Add a Comment` viestien perässä, kuten sivullakin.
            thread.page.addCommentPath?.let {
                item {
                    LinkRow(text = stringResource(R.string.discussion_add_comment), onClick = onOpenComment)
                }
            }
        }
    }
}

/**
 * Kirjoituslomake: otsikko (vain uudella ketjulla) ja teksti, nappi sivun omalla sanalla.
 * Nappi avaa vahvistusdialogin eikä lähetä suoraan, ks. ruudun kommentti.
 */
@Composable
private fun ComposeView(
    compose: ComposeUiState,
    post: PostUiState,
    draftTitle: String,
    draftComment: String,
    onDraftTitleChange: (String) -> Unit,
    onDraftCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismissPost: () -> Unit,
    onClose: () -> Unit,
) {
    when (compose) {
        is ComposeUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is ComposeUiState.Failed -> RetryNote(
            text = compose.reason.text(),
            onRetry = onClose,
            retryLabel = stringResource(R.string.board_back),
        )

        is ComposeUiState.NotAForm -> RetryNote(
            text = stringResource(R.string.discussion_not_a_form),
            onRetry = onClose,
            retryLabel = stringResource(R.string.board_back),
        )

        is ComposeUiState.Loaded -> ComposeForm(
            page = compose.page,
            post = post,
            draftTitle = draftTitle,
            draftComment = draftComment,
            onDraftTitleChange = onDraftTitleChange,
            onDraftCommentChange = onDraftCommentChange,
            onPost = onPost,
            onDismissPost = onDismissPost,
        )
    }
}

@Composable
private fun ComposeForm(
    page: ForumComposePage,
    post: PostUiState,
    draftTitle: String,
    draftComment: String,
    onDraftTitleChange: (String) -> Unit,
    onDraftCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismissPost: () -> Unit,
) {
    val sending = post is PostUiState.Sending
    var confirming by rememberSaveable { mutableStateOf(false) }
    val titleTooLong = page.titleMaxLength?.let { draftTitle.trim().length > it } == true
    val titleMissing = page.isNewThread && draftTitle.isBlank()
    val canPost = !sending && draftComment.isNotBlank() && !titleMissing && !titleTooLong

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .dgReadingSurface()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (page.isNewThread) {
            OutlinedTextField(
                value = draftTitle,
                onValueChange = onDraftTitleChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending,
                singleLine = true,
                isError = titleTooLong,
                label = { Text(stringResource(R.string.discussion_title_label)) },
                // Raja on sivun oma MAXLENGTH, luettu lomakkeelta eikä kirjoitettu tähän.
                supportingText = page.titleMaxLength?.let { max ->
                    { Text(stringResource(R.string.discussion_title_count, draftTitle.trim().length, max)) }
                },
            )
        }
        OutlinedTextField(
            value = draftComment,
            onValueChange = onDraftCommentChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !sending,
            minLines = 6,
            maxLines = 14,
            label = { Text(stringResource(R.string.discussion_comment_label)) },
        )
        Button(onClick = { confirming = true }, enabled = canPost) {
            Text(stringResource(R.string.discussion_post))
        }
        // Lopputulos napin alla eikä lomakkeen alussa (Tommin päätös 14.9.2026, kaikki
        // lomakkeet): kenttä on kuusi riviä korkea ja katse on napissa.
        PostNote(post = post, onDismiss = onDismissPost)
        Text(
            text = stringResource(R.string.discussion_post_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirming) {
        // Hetki ennen lähettämistä (SUBSTANSSI kohta 59): viesti jää palstalle omalla
        // nimellä eikä sitä voi muokata tai poistaa itse. Nappi on sivun oma sana.
        ConfirmDialog(
            title = stringResource(R.string.discussion_confirm_title),
            body = stringResource(R.string.discussion_confirm_body),
            confirmLabel = page.submitLabel,
            onConfirm = { confirming = false; onPost() },
            onDismiss = { confirming = false },
            cancelLabel = stringResource(R.string.discussion_confirm_cancel),
        )
    }
}

/** Lähetyksen tila tekstinä kuittausnapilla; ei mitään kun tila on Idle. */
@Composable
private fun PostNote(post: PostUiState, onDismiss: () -> Unit) {
    val text = when (post) {
        PostUiState.Idle -> return
        PostUiState.Sending -> stringResource(R.string.page_action_sending)
        is PostUiState.Done -> when (post.kind) {
            ComposeKind.NEW_THREAD -> stringResource(R.string.discussion_posted_thread)
            ComposeKind.COMMENT -> stringResource(R.string.discussion_posted_comment)
        }
        is PostUiState.Unconfirmed -> when (post.kind) {
            ComposeKind.NEW_THREAD -> stringResource(R.string.discussion_unconfirmed_thread)
            ComposeKind.COMMENT -> stringResource(R.string.discussion_unconfirmed_comment)
        }
        is PostUiState.Failed -> post.reason.text()
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (post is PostUiState.Failed || post is PostUiState.Unconfirmed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (post !is PostUiState.Sending) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.page_action_dismiss)) }
        }
    }
    HorizontalDivider()
}

@Composable
private fun PostView(post: ForumPost) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = post.body, style = MaterialTheme.typography.bodyMedium)
        // Tekijä ja aika sivun omana allekirjoituksena rungon alla, kuten sivullakin.
        Text(
            text = stringResource(
                R.string.discussion_posted_by,
                post.author.displayName,
                post.postedAtText.orEmpty(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

