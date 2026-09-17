package fi.tommi.dg.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.domain.InviteChoice
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.domain.ReceivedInvitation
import fi.tommi.dg.domain.ReplyForm

/**
 * Viestiarkisto, ja ainoa paikka jossa sovellus tekee peruuttamattoman teon.
 *
 * Ruutu on kahtia jakautunut tarkoituksella. Ylhäällä on jonon haku, joka kuluttaa kohteen
 * DailyGammonilla, ja alhaalla arkisto, joka on pelkkää paikallista lukua. Ne ovat samalla
 * ruudulla juuri siksi että ne on nähtävä yhdessä: nappi on ymmärrettävä vain sen vieressä
 * mihin sen tulos päätyy, ja arkisto on ainoa paikka johon se päätyy.
 *
 * Nappi puuttuu kokonaan kun jonon linkkiä ei ole. Se on sama muoto kuin lautanäkymän
 * klikkaamattomalla rivillä ja asetusruudun lukituilla valintaruuduilla: toiminto jota ei
 * voi tehdä turvallisesti jätetään pois eikä näytetä toimimattomana.
 *
 * **Arkiston viestiin vastataan klikkaamalla sitä** (Tommin pyyntö 30.8.2026), ja
 * vastauskenttä aukeaa viestin alle. Klikattavia ovat vain viestit joilla on sivulta
 * luettu ja talletettu lomake ([Message.replyForm]); muut rivit eivät reagoi, sama muoto
 * kuin yllä. Lähetys itse on yhä [MessagesViewModel.sendReply]in porttien takana.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    messages: List<Message>,
    queue: QueueUiState,
    /** Jonon polku sivun omasta linkistä. Null tarkoittaa ettei hakua ole tarjolla. */
    queuePath: String?,
    onFetchNext: () -> Unit,
    /** Kutsuun vastaamisen tila ja teot, ks. [InvitationActionUiState]. */
    invitationAction: InvitationActionUiState = InvitationActionUiState.Idle,
    onAcceptInvitation: () -> Unit = {},
    onDeclineInvitation: (String) -> Unit = {},
    onCounterOffer: (InviteChoice) -> Unit = {},
    onDismissInvitationAction: () -> Unit = {},
    /** Kirjoitettu mutta lähettämätön vastaus. Säilyy kunnes jonosta otetaan seuraava. */
    draft: String,
    reply: ReplyUiState,
    onDraftChange: (String) -> Unit,
    onSendReply: () -> Unit,
    /** Fraasinapit tallennusjärjestyksessä. Muokattava lista, ks. [MessagesViewModel.phrases]. */
    phrases: List<String>,
    /** Tallentaa annetun tekstin (käytännössä luonnoksen) uudeksi fraasiksi. */
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
    /** Lähteekö lainaus vastauksen mukana. Ks. [MessagesViewModel.quote]. */
    quote: Boolean,
    onQuoteChange: (Boolean) -> Unit,
    /** Keskustelukumppanit alasvetovalikkoon, uusin ensin. */
    opponents: List<String>,
    /** Valittu pelaaja, tai null kun kaikki näkyvät. Valinta muistetaan laitteella. */
    filter: String?,
    onFilterChange: (String?) -> Unit,
    /** Viesti jonka alla vastauskenttä on auki, tai null. Ks. [MessagesViewModel.replyTarget]. */
    replyTargetId: String?,
    onMessageClick: (Message) -> Unit,
    /**
     * Avaa ottelun tunnisteella (Tommin pyynnöt 30.8.2026: *"pelin tieto voisi olla
     * linkkinä"* ja *"myös päättyneeseen otteluun pitäisi saada linkki"*).
     *
     * Lajimerkintä on linkki aina kun viestillä on ottelutunniste. Minne linkki vie,
     * päättää kutsuja: aktiivinen ottelu avautuu lautana otteluluettelon omasta linkistä,
     * muu ottelu siirtohistoriana (Review-sivu). Ks. `MainActivity`n kytkentä.
     */
    onOpenMatch: (String) -> Unit,
    /**
     * Null kun arkisto on tyhjä. Sama muoto kuin jonon napilla yllä: toiminto jota ei voi
     * tehdä mielekkäästi jätetään pois eikä näytetä toimimattomana nappina.
     */
    onExport: (() -> Unit)?,
    /**
     * Tuonti tiedostosta (16.9.2026). Aina näkyvissä, toisin kuin [onExport]: tyhjä arkisto
     * on se tilanne jossa tuontia tarvitaan. Tulos näytetään rivinä varmuuskopiorivin alla
     * kunnes käyttäjä kuittaa sen, ks. [ImportUiState].
     */
    onImport: () -> Unit = {},
    importState: ImportUiState = ImportUiState.Idle,
    onDismissImport: () -> Unit = {},
    /**
     * Näkyvä varmuuskopio: onko se käytössä ja milloin se viimeksi onnistui.
     *
     * Rivi on tässä ruudussa eikä asetuksissa, koska se koskee arkistoa ja sitä luetaan
     * silloin kun arkistoa katsotaan. Ks. [BackupUiState] ja `docs/AVOIMET.md`.
     */
    backup: BackupUiState = BackupUiState.Off,
    onSetUpBackup: () -> Unit = {},
    onBackupNow: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.messages_title)) },
                actions = {
                    TextButton(onClick = onImport) {
                        Text(stringResource(R.string.messages_import_action))
                    }
                    if (onExport != null) {
                        TextButton(onClick = onExport) {
                            Text(stringResource(R.string.messages_export_action))
                        }
                    }
                },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            ProgressSlot(active = queue is QueueUiState.Fetching)

            BackupLine(backup, onSetUpBackup, onBackupNow)
            ImportLine(importState, onDismissImport)

            // Kortti on vierityksen sisällä eikä sen yläpuolella (10.8.2026). Se kasvaa haetun
            // viestin mittaiseksi, joten kiinteänä se voisi viedä koko ikkunan korkeuden ja
            // jättää arkiston nollan korkuiseksi. Sama vika mitattiin samana päivänä
            // otteluluettelosta, ks. `TopScreen`.
            // Mihin viesteihin on vastattu, laskettuna kerran listaa kohti eikä riviä kohti.
            // Yhteys on vastauksen rivillä (`replyTo`), joten vastatun viestin puoli on tämä
            // käänteinen joukko: ilman sitä jokainen rivi joutuisi selaamaan koko listan.
            //
            // Joukko lasketaan koko arkistosta eikä suodatetusta listasta, jotta merkintä
            // ei katoa suodattimen mukana.
            val answeredIds = remember(messages) { messages.mapNotNull { it.replyTo }.toSet() }

            // Suodatus on esitystä: koko arkisto pysyy mallissa ja viennissä ennallaan.
            val visible = remember(messages, filter) {
                if (filter == null) messages else messages.filter { it.opponent == filter }
            }

            // **Ketju on viesti vastauksineen, ja viiva piirtyy vain ketjujen väliin**
            // (Tommin pyyntö 30.8.2026). Ryhmän avain on vastatun viestin tunniste, joten
            // vastaus asettuu kohteensa alle riippumatta siitä mitä välissä on saapunut.
            // Lista on uusin ensin, ja ketju asettuu uusimman jäsenensä kohdalle; ketjun
            // sisällä järjestys on vanhin ensin, koska keskustelu luetaan alusta loppuun.
            val threads = remember(visible) {
                val groups = LinkedHashMap<String, MutableList<Message>>()
                visible.forEach { m -> groups.getOrPut(m.replyTo ?: m.id) { mutableListOf() }.add(m) }
                groups.values.map { it.sortedBy(Message::receivedAtEpochMillis) }
            }
            val visibleIds = remember(visible) { visible.mapTo(mutableSetOf()) { it.id } }

            DgLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Column {
                        QueueCard(
                            queue = queue,
                            queuePath = queuePath,
                            onFetchNext = onFetchNext,
                            invitationAction = invitationAction,
                            onAcceptInvitation = onAcceptInvitation,
                            onDeclineInvitation = onDeclineInvitation,
                            onCounterOffer = onCounterOffer,
                            onDismissInvitationAction = onDismissInvitationAction,
                        )

                        HorizontalDivider()
                    }
                }

                // Valikko piirtyy myös silloin kun suodatettu lista on tyhjä: juuri silloin
                // sitä tarvitaan, koska ainoa tie takaisin on vaihtaa valintaa.
                if (opponents.isNotEmpty() || filter != null) {
                    item {
                        Column {
                            PlayerFilterRow(
                                opponents = opponents,
                                filter = filter,
                                onFilterChange = onFilterChange,
                            )
                            HorizontalDivider()
                        }
                    }
                }

                if (visible.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(
                                if (messages.isEmpty()) R.string.messages_archive_empty
                                else R.string.messages_filter_empty
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    return@DgLazyColumn
                }

                items(threads, key = { it.first().id }, contentType = { DgStripedRow }) { thread ->
                    Column {
                        thread.forEach { message ->
                            MessageRow(
                                message = message,
                                // Yhteysrivi vain kun kohde ei ole ruudulla: ketjussa kohde on
                                // suoraan yllä, ja lainaus toistaisi saman sisällön kahdesti.
                                repliedTo = message.replyTo
                                    ?.takeIf { it !in visibleIds }
                                    ?.let { id -> messages.firstOrNull { it.id == id } },
                                answered = message.id in answeredIds,
                                onClick = { onMessageClick(message) },
                                onOpenMatchLink = message.matchId?.value
                                    ?.let { id -> { onOpenMatch(id) } },
                            )

                            // **Vastauskenttä sen viestin alla johon se vastaa** (Tommin päätös
                            // 24.8.2026, laajennettu 30.8.2026 koskemaan klikattua arkiston
                            // viestiä). Ehto ei ole tämän `if`in varassa vaan rakenteessa:
                            // lähetys lukee lomakkeen `MessagesViewModel.replyTarget`ista, ja
                            // sinne pääsee vain viesti jolla lomake on. Vertailu `id`:llä
                            // valitsee vain paikan, ei oikeutta.
                            val form = message.replyForm
                            if (message.id == replyTargetId && form != null) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    ReplyField(
                                        form = form,
                                        original = message,
                                        draft = draft,
                                        reply = reply,
                                        quote = quote,
                                        onQuoteChange = onQuoteChange,
                                        onDraftChange = onDraftChange,
                                        onSendReply = onSendReply,
                                        phrases = phrases,
                                        onAddPhrase = onAddPhrase,
                                        onRemovePhrase = onRemovePhrase,
                                    )
                                }
                            }
                        }

                        // Ruudun levyinen viiva ketjujen väliin (Tommin pyyntö 30.8.2026).
                        // Ketjun sisällä viivaa ei ole, jotta viesti ja vastaus lukevat
                        // yhtenä keskusteluna.
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Pelaajasuodatin alasvetovalikkona (Tommin pyyntö 30.8.2026, *"muista valinta"*).
 *
 * Valinta säilötään laitteelle (`MessageFilterStore`), joten ruutu avautuu samaan rajaukseen
 * johon se jäi. `DropdownMenu` eikä `ExposedDropdownMenuBox`, koska kenttään ei kirjoiteta:
 * vaihtoehdot ovat arkiston omat nimet, eikä hakua ole (ks. `MessageDao.observeByOpponent`,
 * pääsypolku on nimi jonka käyttäjä jo tietää).
 *
 * Valittu nimi piirtyy pelaajan omalla värillä, samalla jonka viestirivit antavat sille.
 */
/**
 * Varmuuskopion tila ruudulle.
 *
 * Kolme tilaa eikä lippu, koska *"ei käytössä"* ja *"käytössä mutta ei onnistunut"* ovat eri
 * asioita käyttäjälle: edellinen odottaa päätöstä, jälkimmäinen odottaa korjausta.
 */
sealed interface BackupUiState {

    /** Ei otettu käyttöön. */
    data object Off : BackupUiState

    /** Käytössä. [lastSaved] on null vain käyttöönoton ja ensimmäisen kirjoituksen välissä. */
    data class On(val lastSaved: Long?) : BackupUiState

    /**
     * Kirjoitus epäonnistui: tiedosto poistettiin tai oikeus meni. Osoite on unohdettu, joten
     * korjaus on käyttöönotto uudestaan.
     *
     * **Tämä on koko ominaisuuden syy näkyvänä**: hiljaa epäonnistuva varmuuskopio on
     * pahempi kuin ei varmuuskopiota, koska se on väärä varmuus.
     *
     * [lastSaved] on se hetki jolloin kopio viimeksi onnistui, ja **se näytetään**. Kenttä oli
     * olemassa 1.9.2026 asti käyttämättömänä, eli rivi lupasi kommentissaan ajan jota se ei
     * piirtänyt. Laiteajo löysi eron, ja Tommi valitsi korjata koodin eikä lupausta.
     */
    data class Failed(val lastSaved: Long?) : BackupUiState

    companion object {

        /**
         * Tila kolmesta arvosta, jotta se on testattavissa ilman Composea ja `Activity`ä.
         * Sama peruste kuin [BackupStatus]illa ja [ArchiveEdge]llä: tässä ovat ne ehdot joissa
         * voi mennä vikaan, ja ne on parempi lukea testistä kuin ruudulta.
         *
         * **Epäonnistuminen kestää käynnistyksen yli, eikä siihen tarvita omaa lippua.**
         * Käyttöönotto nollaa ajan (`BackupStore.saveTarget`) ja vika unohtaa osoitteen
         * (`forget`), joten ajan olemassaolo ilman osoitetta voi syntyä vain viasta. Ehto on
         * siis johdettu eikä muistettu. Ilman tätä sovellus sanoisi uudella käynnistyksellä
         * *"ei varmuuskopiota"* tilanteessa jossa kopio on olemassa ja kirjoitus epäonnistui,
         * eli vaikenisi juuri siitä mitä varten koko rivi on.
         */
        fun of(target: String?, lastSaved: Long?, failedNow: Boolean): BackupUiState = when {
            failedNow -> Failed(lastSaved)
            target != null -> On(lastSaved)
            lastSaved != null -> Failed(lastSaved)
            else -> Off
        }
    }
}

/**
 * Yksi rivi arkiston yläpuolella: onko kopio olemassa, miltä hetkeltä, ja mitä siitä voi tehdä.
 *
 * Rivi eikä kortti, ja se on tarkoituksellista: tämä ei ole tapahtuma vaan tila, ja se on
 * ruudulla joka kerta. Kortti väittäisi joka avauksella että jotain on sattunut.
 */
@Composable
private fun BackupLine(
    backup: BackupUiState,
    onSetUp: () -> Unit,
    onBackupNow: () -> Unit,
) {
    val text = when (backup) {
        BackupUiState.Off -> stringResource(R.string.backup_off)
        is BackupUiState.On -> backup.lastSaved
            ?.let { stringResource(R.string.backup_saved, BackupStatus.format(it)) }
            ?: stringResource(R.string.backup_on_not_yet)

        is BackupUiState.Failed -> backup.lastSaved
            ?.let { stringResource(R.string.backup_failed_saved, BackupStatus.format(it)) }
            ?: stringResource(R.string.backup_failed)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
        )
        when (backup) {
            // **Kaksi tekoa eikä yksi (1.9.2026, Tommin havainto).** Ensimmäinen versio
            // näytti käytössä ollessaan vain `Save now`n, ja silloin kohdetta ei päässyt
            // vaihtamaan lainkaan: valitsin oli olemassa vain käyttöönotossa. Tommi valitsi
            // vahingossa Lataukset-kansion Driven sijaan, eikä ruudulla ollut mitään millä
            // korjata se. Sama vikamuoto kuin tekstilinkillä ja kuolleilla teoilla: ruutu
            // näytti valmiilta tilanteessa jossa teko puuttui.
            is BackupUiState.On -> Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSetUp) {
                    Text(stringResource(R.string.backup_change))
                }
                TextButton(onClick = onBackupNow) {
                    Text(stringResource(R.string.backup_now))
                }
            }

            else -> TextButton(onClick = onSetUp) {
                Text(stringResource(R.string.backup_set_up))
            }
        }
    }
    HorizontalDivider()
}

/**
 * Tuonnin tulos yhtenä rivinä, kuittaukseen asti. Rivi eikä dialogi, koska tulos on tieto
 * eikä kysymys: mitään ei jää odottamaan vastausta, ja luvut saa lukea rauhassa.
 */
@Composable
private fun ImportLine(state: ImportUiState, onDismiss: () -> Unit) {
    val text = when (state) {
        ImportUiState.Idle -> return
        ImportUiState.NotAnArchive -> stringResource(R.string.import_not_archive)
        is ImportUiState.Done -> buildList {
            if (state.messages == 0 && state.reminders == 0 && state.phrases == 0) {
                add(stringResource(R.string.import_nothing_new))
            } else {
                add(stringResource(R.string.import_done, state.messages, state.reminders, state.phrases))
                if (state.alreadyHere > 0) add(stringResource(R.string.import_already_here, state.alreadyHere))
            }
            if (state.unreadable > 0) add(stringResource(R.string.import_unreadable, state.unreadable))
            if (state.otherAccount != null && state.messages > 0) {
                add(stringResource(R.string.import_other_account, state.otherAccount))
            }
        }.joinToString(" ")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
        )
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.import_dismiss))
        }
    }
    HorizontalDivider()
}

@Composable
private fun PlayerFilterRow(
    opponents: List<String>,
    filter: String?,
    onFilterChange: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            TextButton(onClick = { open = true }) {
                Text(
                    text = filter ?: stringResource(R.string.messages_filter_all),
                    color = filter?.let { playerColor(it, dark) } ?: Color.Unspecified,
                )
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.messages_filter_all)) },
                    onClick = {
                        open = false
                        onFilterChange(null)
                    },
                )
                // Säilötty nimi joka ei ole enää listalla pysyy valittavana: se on käyttäjän
                // oma valinta eikä katoa siksi että lista laskettiin uudelleen.
                val names = if (filter != null && filter !in opponents) {
                    listOf(filter) + opponents
                } else {
                    opponents
                }
                names.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name, color = playerColor(name, dark) ?: Color.Unspecified) },
                        onClick = {
                            open = false
                            onFilterChange(name)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Jonon haku ja sen viimeisin tulos yhdessä kortissa.
 *
 * Tulos jää näkyviin eikä katoa itsestään, ja syy on sama kuin koko ruudulla: haettua
 * kohdetta ei saa takaisin, joten käyttäjän on nähtävä mitä siitä tuli silloinkin kun hän
 * katsoi muualle sillä hetkellä kun se tuli.
 */
@Composable
private fun QueueCard(
    queue: QueueUiState,
    queuePath: String?,
    onFetchNext: () -> Unit,
    invitationAction: InvitationActionUiState,
    onAcceptInvitation: () -> Unit,
    onDeclineInvitation: (String) -> Unit,
    onCounterOffer: (InviteChoice) -> Unit,
    onDismissInvitationAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.messages_queue_heading),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.messages_queue_explain),
                style = MaterialTheme.typography.bodySmall,
            )

            if (queuePath == null) {
                Text(
                    text = stringResource(R.string.messages_queue_no_link),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(
                    onClick = onFetchNext,
                    enabled = queue !is QueueUiState.Fetching,
                ) {
                    Text(stringResource(R.string.messages_queue_fetch))
                }
            }

            // Lopputulos napin alla (Tommin päätös 14.9.2026, kaikki lomakkeet), ja
            // ilmoituksen teksti sen perässä samasta syystä.
            queueOutcome(queue)?.let { text ->
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }

            // Ilmoituksen teksti kortissa, koska sitä ei ole kannassa eikä listalla. Tämä on
            // ainoa paikka jossa se on olemassa haun jälkeen, ja se katoaa seuraavasta
            // hausta. Tallennetun viestin kohdalla vastaavaa ei tarvita: se on listalla
            // alempana.
            (queue as? QueueUiState.Announcement)?.let { state ->
                Text(
                    text = state.message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Kutsu kortissa samasta syystä kuin ilmoitus: se ei ole kannassa eikä listalla.
            // Toisin kuin ilmoitus se ei katoa seuraavasta hausta vaan vasta vastauksesta.
            (queue as? QueueUiState.Invitation)?.let { state ->
                InvitationSection(
                    invitation = state.invitation,
                    action = invitationAction,
                    onAccept = onAcceptInvitation,
                    onDecline = onDeclineInvitation,
                    onCounter = onCounterOffer,
                    onDismiss = onDismissInvitationAction,
                )
            }
            // Vastauksen lopputulos myös silloin kun kutsu on jo poissa kortista, eli aina
            // onnistuessa: jono palaa Idleen vastauksen tullessa, ja 14.9.2026 nauha näytti
            // että *You have successfully joined that game.* jäi silloin näyttämättä, koska
            // rivi piirtyi vain kutsuosion sisällä.
            if (queue !is QueueUiState.Invitation) {
                InvitationOutcomeRow(action = invitationAction, onDismiss = onDismissInvitationAction)
            }
        }
    }
}

@Composable
private fun InvitationOutcomeRow(action: InvitationActionUiState, onDismiss: () -> Unit) {
    val sending = action is InvitationActionUiState.Sending
    invitationOutcome(action)?.let { text ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (!sending) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.page_action_dismiss)) }
            }
        }
    }
}

/**
 * Vastaanotettu kutsu jonokortissa: lause sivun sanoin ja kolme nappia sivun omilla
 * nimillä (Tommin päätös 14.9.2026, *"vaihtoehto 1"*: DG 8x8:n kaltaiset turnaukset alkavat
 * suorilla kutsuilla eikä selainta tarvitse avata). Hyväksyntä ja hylkäys kulkevat
 * vahvistusdialogin kautta, koska molemmat ovat peruuttamattomia tekoja toiselle ihmiselle;
 * vastatarjous käyttää profiilin kutsulomaketta ja sen dialogia, koska lomake on sama.
 * Lopputulos on nappien alla (Tommin päätös samana iltana, kaikki lomakkeet).
 */
@Composable
private fun InvitationSection(
    invitation: ReceivedInvitation,
    action: InvitationActionUiState,
    onAccept: () -> Unit,
    onDecline: (String) -> Unit,
    onCounter: (InviteChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val sending = action is InvitationActionUiState.Sending
    var confirmingAccept by rememberSaveable { mutableStateOf(false) }
    var declining by rememberSaveable { mutableStateOf(false) }
    var confirmingDecline by rememberSaveable { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf("") }
    var countering by rememberSaveable { mutableStateOf(false) }
    var confirmingCounter by remember { mutableStateOf<InviteChoice?>(null) }
    val from = invitation.from.displayName

    if (confirmingAccept) {
        ConfirmDialog(
            title = stringResource(R.string.messages_invitation_accept_title),
            body = stringResource(R.string.messages_invitation_accept_body, from),
            confirmLabel = invitation.acceptLabel,
            onConfirm = {
                confirmingAccept = false
                onAccept()
            },
            onDismiss = { confirmingAccept = false },
        )
    }
    if (confirmingDecline) {
        ConfirmDialog(
            title = stringResource(R.string.messages_invitation_decline_title),
            body = stringResource(R.string.messages_invitation_decline_body, from),
            confirmLabel = invitation.declineLabel,
            onConfirm = {
                confirmingDecline = false
                onDecline(reason)
            },
            onDismiss = { confirmingDecline = false },
        )
    }
    invitation.counter?.let { form ->
        confirmingCounter?.let { choice ->
            InviteConfirmDialog(
                form = form,
                player = from,
                choice = choice,
                onConfirm = {
                    confirmingCounter = null
                    onCounter(choice)
                },
                onDismiss = { confirmingCounter = null },
            )
        }
    }

    HorizontalDivider()
    Text(
        text = stringResource(R.string.messages_invitation_sentence, from, invitation.description),
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { confirmingAccept = true }, enabled = !sending) {
            Text(invitation.acceptLabel)
        }
        OutlinedButton(onClick = { declining = !declining }, enabled = !sending) {
            Text(invitation.declineLabel)
        }
        invitation.counterLabel?.let { label ->
            if (invitation.counter != null) {
                OutlinedButton(onClick = { countering = !countering }, enabled = !sending) {
                    Text(label)
                }
            }
        }
    }
    if (declining) {
        val max = invitation.decline.reason?.maxLength
        OutlinedTextField(
            value = reason,
            onValueChange = { if (max == null || it.length <= max) reason = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !sending,
            singleLine = true,
            label = { Text(stringResource(R.string.messages_invitation_reason, from)) },
        )
        Button(onClick = { confirmingDecline = true }, enabled = !sending) {
            Text(invitation.declineLabel)
        }
    }
    if (countering) {
        invitation.counter?.let { form ->
            InviteComposer(
                form = form,
                player = from,
                enabled = !sending,
                onSend = { confirmingCounter = it },
            )
        }
    }
    InvitationOutcomeRow(action = action, onDismiss = onDismiss)
}

/** Kutsuun vastaamisen lopputulos yhtenä lauseena, tai null kun mitään ei ole lähetetty. */
@Composable
private fun invitationOutcome(action: InvitationActionUiState): String? = when (action) {
    InvitationActionUiState.Idle -> null
    is InvitationActionUiState.Sending -> stringResource(R.string.page_action_sending)
    is InvitationActionUiState.Answered -> action.notice
        ?.let { stringResource(R.string.messages_invitation_answered, it) }
        ?: stringResource(R.string.messages_invitation_answered_plain)
    is InvitationActionUiState.Failed -> action.reason.text()
    InvitationActionUiState.SessionExpired -> stringResource(R.string.error_auth)
}

/**
 * Vastauskenttä viestiin, jonka alla se piirtyy.
 *
 * Neljä asiaa on tässä rakenteena eikä ohjetekstissä:
 *
 * 1. **Pituutta ei rajoiteta, ja se on mitattu eikä oletettu (24.8.2026).** Sivun oma
 *    `maxlength` on 80, mutta palvelin tallensi 1329 merkin viestin kokonaisena: se ei
 *    leikkaa vaan rivittää 80 merkin kohdalta. Kenttä näyttää siis pelkän merkkimäärän
 *    ilman kattoa, koska katto jota ei ole ei kuulu ruudulle.
 * 2. **Nappi on pois päältä kun lähetettävää ei ole.** Tyhjä lähetys kuluttaisi teon.
 * 3. **Teksti jää kenttään epäonnistuessa.** Kuljetus ei lähetä uudelleen itse, joten
 *    kirjoitettu teksti on tässä ainoa kopio siitä mitä käyttäjä sanoi.
 * 4. **Kenttä on monirivinen vaikka sivun kenttä ei ole.** Sivulla on `INPUT SIZE=80
 *    MAXLENGTH=80`, eli yksi rivi, mutta 80 merkkiä ei mahdu puhelimen yhdelle riville.
 *    Tämä koskee vain näyttämistä: raja on yhä sivun oma, ja sitä noudatetaan yllä.
 *    Malli on selainlaajennus DGText2Area, joka korvaa saman lomakkeen kentän
 *    elementillä `textarea rows=10 cols=64`. Korkeus ei ole sen korkeus, koska 10 riviä
 *    on työpöytäselaimen mitta, ja laajennuksen pudottamaa `maxlength`ia ei seurattu.
 */
@Composable
private fun ReplyField(
    form: ReplyForm,
    /** Viesti johon vastataan; lainauksen lähde. */
    original: Message,
    draft: String,
    reply: ReplyUiState,
    quote: Boolean,
    onQuoteChange: (Boolean) -> Unit,
    onDraftChange: (String) -> Unit,
    onSendReply: () -> Unit,
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
) {
    val sending = reply is ReplyUiState.Sending

    // **Valittu lainaus näkyy ennen lähetystä täsmälleen lähtevässä muodossaan** (Tommin
    // pyyntö 30.8.2026: vastaanottajan pitää nähdä mihin vastattiin). Esikatselu kentän
    // yllä eikä esitäyttö kenttään: esitäyttö sotkisi luonnoksen ja sen poisto rastin
    // irrotessa olisi arvailua käyttäjän muokkausten seasta. Näin kenttä on aina pelkkä
    // oma teksti, ja rasti kertoo koko totuuden siitä mitä sen eteen liitetään.
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = quote,
            onCheckedChange = onQuoteChange,
            enabled = !sending,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(R.string.messages_reply_quote),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(enabled = !sending) { onQuoteChange(!quote) },
        )
    }
    if (quote) {
        Text(
            text = MessagesViewModel.quoteForReply(original.body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }

    OutlinedTextField(
        value = draft,
        onValueChange = onDraftChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !sending,
        // Sivun kenttä on yksirivinen INPUT, mutta 80 merkkiä ei mahdu puhelimen yhdelle
        // riville luettavasti. Kolme riviä näkyy aina ja kenttä venyy kahdeksaan, jonka
        // jälkeen sisältö vierii: lähetysnappi pysyy näin ruudulla myös pisimmällä viestillä.
        minLines = 3,
        maxLines = 8,
        label = { Text(stringResource(R.string.messages_reply_label)) },
        // Laskuri kertoo pituuden muttei lupaa rajaa, koska rajaa ei enää ole. Se on silti
        // hyödyllinen: sivusto rivittää 80 merkin kohdalta, joten luku kertoo suunnilleen
        // kuinka monelle riville viesti perillä hajoaa. Lainaus lasketaan mukaan, koska
        // luku koskee lähtevää viestiä eikä kenttää.
        //
        // Pituus mitataan siitä samasta tekstistä joka lähtee (`replyBody`) eikä lasketa
        // osista: ruutu ei tiedä millä lainaus ja oma teksti liitetään, eikä siis voi olla
        // siitä eri mieltä kuin lähettäjä.
        supportingText = {
            val sent = MessagesViewModel.replyBody(original.body, quote, draft)
            Text(stringResource(R.string.messages_reply_count, sent.length))
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

    Button(
        onClick = onSendReply,
        enabled = !sending && draft.isNotBlank(),
    ) {
        Text(stringResource(R.string.messages_reply_send))
    }

    // Lopputulos napin alla (Tommin päätös 14.9.2026, kaikki lomakkeet).
    replyOutcome(reply)?.let { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Lähetyksen lopputulos yhtenä lauseena, tai null kun mitään ei ole lähetetty. */
@Composable
private fun replyOutcome(reply: ReplyUiState): String? = when (reply) {
    ReplyUiState.Idle -> null
    ReplyUiState.Sending -> stringResource(R.string.messages_reply_sending)
    ReplyUiState.Sent -> stringResource(R.string.messages_reply_sent)
    ReplyUiState.Unconfirmed -> stringResource(R.string.messages_reply_unconfirmed)
    ReplyUiState.SessionExpired -> stringResource(R.string.error_auth)
    is ReplyUiState.Failed -> reply.reason.text()
}

/**
 * Haun lopputulos yhtenä lauseena, tai null kun mitään ei ole vielä haettu.
 *
 * Jokainen teksti sanoo myös **kuluiko jonosta jotain**. Se on tässä ruudussa se tieto jota
 * käyttäjä ei voi tarkistaa mistään muualta.
 */
@Composable
private fun queueOutcome(queue: QueueUiState): String? = when (queue) {
    QueueUiState.Idle -> null
    QueueUiState.Fetching -> stringResource(R.string.messages_queue_fetching)
    is QueueUiState.Saved -> stringResource(R.string.messages_queue_saved)
    is QueueUiState.Announcement -> stringResource(R.string.messages_queue_announcement)
    is QueueUiState.NotAMessage -> when (queue.kind) {
        NotAMessageKind.Board -> stringResource(R.string.messages_queue_was_board)
        NotAMessageKind.Unreadable -> stringResource(R.string.messages_queue_unreadable)
    }

    is QueueUiState.Invitation -> stringResource(R.string.messages_queue_invitation)

    is QueueUiState.Failed -> queue.reason.text()

    QueueUiState.SessionExpired -> stringResource(R.string.error_auth)
}

@Composable
private fun MessageRow(
    message: Message,
    repliedTo: Message? = null,
    answered: Boolean = false,
    onClick: () -> Unit = {},
    /** Avaa ottelun laudan, tai null kun luettelo ei tarjoa siihen linkkiä juuri nyt. */
    onOpenMatchLink: (() -> Unit)? = null,
) {
    // Klikattava vain kun klikkaus tekee jotain: viesti ilman talletettua lomaketta ei voi
    // saada vastauskenttää, ja klikattavalta tuntuva rivi joka ei reagoi olisi huonompi kuin
    // rivi joka ei ole klikattava. Sama periaate kuin lautanäkymän ottelurivillä.
    val rowModifier = if (message.replyForm != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(modifier = rowModifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // Lähettäjätön viesti sanotaan lajilla eikä keksityllä nimellä:
                    // ilmoituksella ei ole lähettäjää, ja sijaisnimi olisi sovelluksen
                    // väite eikä sivun.
                    text = message.sender.ifBlank { stringResource(sourceLabel(message.source)) },
                    style = MaterialTheme.typography.titleSmall,
                    // Tunnuksen oma väri (Tommin pyyntö 30.8.2026). Väri on nimessä eikä
                    // rungossa: sisältö pysyy teeman perusvärissä, eli tummalla taustalla
                    // vaaleana. Lähettäjätön rivi jää teeman väriin.
                    color = playerColor(message.sender, isSystemInDarkTheme()) ?: Color.Unspecified,
                )
                // Laji nimen perässä (Tommin pyyntö 30.8.2026): pikaviesti ja ottelun
                // mukana tullut ovat eri reittiä saapuneita, ja ero luetaan tästä eikä
                // pääteltäväksi jätettynä. Lähettäjättömällä rivillä laji on jo nimen
                // paikalla, joten toisto jää pois.
                //
                // Ottelusta tulleella viestillä merkintä on linkki otteluun (Tommin
                // pyynnöt 30.8.2026): aktiivinen ottelu avautuu lautana, päättynyt
                // siirtohistoriana. Väri kantaa eron: primary on linkki, harmaa pelkkä
                // merkintä (viesti jolla ei ole ottelutunnistetta).
                // Oma viesti sanoo kenelle se meni (Tommin tilaus 15.9.2026 illalla: "kun
                // olen lähettäjä niin pitäisi näkyä myös kohde"). Sääntö ei tarvitse omaa
                // nimeä: saapuneessa viestissä lähettäjä on sama kuin keskustelukumppani
                // (`Message.opponent`), omassa ei, ja tyhjä lähettäjä oman nimen puuttuessa
                // on myös oma. Kohde saa oman tunnusvärinsä kuten lähettäjä.
                val recipient = message.opponent?.takeIf { it.isNotBlank() && it != message.sender }
                if (recipient != null) {
                    Text(
                        text = stringResource(R.string.messages_to, recipient),
                        style = MaterialTheme.typography.titleSmall,
                        color = playerColor(recipient, isSystemInDarkTheme()) ?: Color.Unspecified,
                    )
                }
                if (message.sender.isNotBlank()) {
                    Text(
                        text = stringResource(sourceLabel(message.source)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (onOpenMatchLink != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = if (onOpenMatchLink != null) {
                            Modifier.clickable(onClick = onOpenMatchLink)
                        } else {
                            Modifier
                        },
                    )
                }
            }
            Text(
                text = ArchiveEdge.formatDate(message.receivedAtEpochMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // **Yhteys luetaan ennen tekstiä, koska se on tekstin konteksti** (Tommin päätös
        // 24.8.2026). Vastaus ilman kohdetta on arkistossa irrallinen lause, ja juuri sen
        // näkeminen oli koko syy tähän: *"haluan tietää mihin viesteihin on vastattu"*.
        //
        // Rivi piirtyy 30.8.2026 alkaen vain kun kohde ei ole ruudulla: ketjuryhmittely
        // asettaa vastauksen kohteensa alle, ja lainaus suoraan kohteen alla olisi sama
        // sisältö kahdesti.
        //
        // Lainaus on yksi rivi eikä koko viesti, tunnistamista eikä lukemista varten.
        repliedTo?.let { kohde ->
            Text(
                text = stringResource(R.string.messages_reply_to, kohde.body.oneLine()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(text = message.body, style = MaterialTheme.typography.bodyMedium)

        // Toinen puoli samasta tiedosta, ja se on tässä siksi että kysymys esitetään
        // useammin näin päin: onko tähän jo vastattu.
        if (answered) {
            Text(
                text = stringResource(R.string.messages_answered),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Tuntemattoman viestin otsikko näytetään, koska laji ei kerro siitä mitään. Juuri
        // se teksti on ainoa jäljellä oleva vihje siitä mitä sivusto lähetti, ja se on
        // tallessa vain siksi että se päätettiin tallettaa aina (9.8.2026).
        if (message.source == MessageSource.UNKNOWN && message.rawHeader.isNotBlank()) {
            Text(
                text = message.rawHeader,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun sourceLabel(source: MessageSource): Int = when (source) {
    MessageSource.GAME_MESSAGE -> R.string.messages_source_game
    MessageSource.QUICK_MESSAGE -> R.string.messages_source_quick
    MessageSource.ANNOUNCEMENT -> R.string.messages_source_announcement
    MessageSource.UNKNOWN -> R.string.messages_source_unknown
}

/**
 * Tunnuksen oma väri nimestä johdettuna (Tommin pyyntö 30.8.2026), tai null tyhjälle
 * nimelle.
 *
 * **Sävyparit ovat `DgTheme`n välilehtiaksenttien mitatut arvot**, eli jokainen ylittää
 * AA-rajan 4,5:1 sekä tummaa (`#1C1B1F`) että vaaleaa (`#FEF7FF`) taustaa vasten; mittaukset
 * ovat `accentFor`in taulukossa. Uusia sävyjä ei keksitty, koska mitattuja oli valmiina
 * viisi ja väri on tässä vahvistus eikä tunniste: nimi on aina näkyvissä, ja kuudes pelaaja
 * jakaa väistämättä sävyn jonkun kanssa. Sama varaus kuin välilehdillä.
 *
 * Johdos on nimen oma `hashCode`, jotta sama nimi saa saman värin joka ruudulla ja joka
 * käynnistyksellä ilman että mitään säilötään.
 */
internal fun playerColor(name: String, dark: Boolean): Color? {
    if (name.isBlank()) return null
    val pair = PLAYER_HUES[(name.hashCode().let { it xor (it shr 16) }.and(Int.MAX_VALUE)) % PLAYER_HUES.size]
    return if (dark) pair.first else pair.second
}

/** (tumma, vaalea) -parit, arvot `DgTheme.accentFor`ista mittauksineen. */
private val PLAYER_HUES = listOf(
    Color(0xFFCBB093) to Color(0xFF6B5844), // kehyspuu
    Color(0xFFA8C69A) to Color(0xFF4C6B41), // vihreä
    Color(0xFF9EBBD6) to Color(0xFF3F5F7A), // sininen
    Color(0xFFC4AAD3) to Color(0xFF5F4A6B), // violetti
    Color(0xFF93C6C0) to Color(0xFF3D6862), // turkoosi
)

/**
 * Viestin ensimmäinen rivi yhtenä rivinä, lainausta varten.
 *
 * Sivusto rivittää viestin 80 merkin kohdalta, joten pitkä viesti on kannassa monirivinen
 * eikä sen ensimmäinen rivi ole sama asia kuin sen ensimmäinen lause. Tämä ei yritä olla
 * tiivistelmä: se on tunnistamista varten, ja loppu katkaistaan ruudulla kolmeen pisteeseen.
 */
private fun String.oneLine(): String = trim().substringBefore('\n').trim()
