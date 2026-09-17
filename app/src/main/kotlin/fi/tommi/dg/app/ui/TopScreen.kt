package fi.tommi.dg.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.MatchClock
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.SeenMatch
import fi.tommi.dg.domain.MatchOrder
import fi.tommi.dg.domain.MatchSortKey
import fi.tommi.dg.domain.ordered
import fi.tommi.dg.domain.PendingAction
import fi.tommi.dg.domain.TopPage

/**
 * Otteluluettelon kaksi listaa (Tommin tilaus 4.9.2026).
 *
 * **Ne eivät ole sama sivu kahdella suodattimella vaan kaksi sivua**, ja siksi ne ovat
 * segmenttejä eivätkä yksi lista. [YourTurn] on Top Page, joka näyttää vain ne otteluissa
 * joissa on vuoro. [Tournaments] on `/bg/userevent/<id>`, pelaajan omat käynnissä olevat
 * turnaukset, ja se asui 3.9.–4.9.2026 Loungessa. Siirron peruste on kysymys johon ne
 * vastaavat: omat turnaukset kertovat missä jo olen, ja lounge on se paikka johon mennään
 * mukaan.
 */
enum class MatchesSegment { YourTurn, Tournaments }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopScreen(
    state: TopUiState,
    onSignIn: (String, String) -> Unit,
    onRefresh: () -> Unit,
    /**
     * Ratas yläpalkissa: asetukset omana ruutunaan (Tommin päätös 4.9.2026, `docs/UI.md`).
     *
     * `Sign out` oli tässä palkissa `Refresh`in vieressä, ja niiden tarve on eri luokkaa:
     * Refreshiä painetaan päivittäin, uloskirjautumista tuskin koskaan. Teko siirtyi
     * Info-välilehden listariviksi, ja ratas otti sen paikan. Nappien määrä ei siis kasva,
     * eikä otsikolta vie leveyttä mikään uusi.
     */
    onOpenSettings: () -> Unit,
    onOpenMatch: (Match) -> Unit,
    // Vain WaitingNoticen tarve: välilehtipalkki hoitaa siirtymiset 26.8.2026 alkaen,
    // mutta ilmoituksen napin on yhä vietävä arkistoon.
    onOpenMessages: () -> Unit,
    /**
     * Porautuminen sivun omalla polulla. Tässä ruudussa se on oma profiili, ja se on
     * ainoa reitti niihin otteluihin joita Top Page ei näytä: sivu listaa vain ne joissa
     * on vuoro, ja odottavat sekä päättyneet ovat profiililla.
     */
    onOpenPage: (String) -> Unit,
    /**
     * Merkittyjen asemien määrä ja linkki listaan (Tommin toive 15.9.2026 illalla). Määrä
     * on kannasta kuten reunapäivä, ja linkki on otsakerivillä nimen alla, koska Tommi
     * valitsi lukupaikaksi otteluluettelon eikä `.mat`-tiedoston (`MarkedPosition`).
     */
    markCount: Int = 0,
    onOpenMarks: () -> Unit = {},
    /** Auki oleva segmentti ja sen vaihto, ks. [MatchesSegment]. */
    segment: MatchesSegment = MatchesSegment.YourTurn,
    onSelectSegment: (MatchesSegment) -> Unit = {},
    /** Omien turnausten tila. Oletus on tyhjä, koska ruutu ei tiedä siitä ennen kuin se annetaan. */
    tournaments: OwnTournamentsUiState = OwnTournamentsUiState.NoPath,
    /**
     * Ottelumuisti kannasta, ks. [fi.tommi.dg.data.MatchMemory]. Turnauslista lukee siitä
     * vastustajan ja kierroksen otteluille joita luettelo ei näytä. Oletus tyhjä, koska
     * esikatselu ja testi eivät ole lukeneet kantaa.
     */
    remembered: Map<MatchId, SeenMatch> = emptyMap(),
    /** Refreshin profiilihaun tila, ks. [OpponentsRefreshState]. */
    opponentsRefresh: OpponentsRefreshState = OpponentsRefreshState.Idle,
    modifier: Modifier = Modifier,
    /**
     * Luettelon järjestys. Oletus on sivuston oma, ja se on oletus myös tässä
     * allekirjoituksessa: ruutu ei tiedä järjestyksestä mitään ennen kuin sellainen
     * annetaan.
     */
    order: MatchOrder = MatchOrder.SITE,
    onSort: (MatchSortKey) -> Unit = {},
    /**
     * Arkiston reunapäivä kolmena tilana, ks. [ArchiveEdgeState].
     *
     * Tulee näkymämallin ohi suoraan arkistosta, koska se ei ole otteluluettelon tilaa
     * eikä kulje verkon kautta: [TopViewModel] pysyy sinä luokkana joka vain hakee sivun.
     *
     * Oletus on [ArchiveEdgeState.Unknown] eikä tyhjä, koska esikatselu ja testi eivät ole
     * lukeneet kantaa. Sama peruste kuin kerääjän alkuarvolla.
     */
    archiveEdge: ArchiveEdgeState = ArchiveEdgeState.Unknown,
    /**
     * Keskeytyneet lähetykset kannasta, vanhin ensin.
     *
     * Tulee näkymämallin ohi suoraan jonosta samasta syystä kuin [archiveEdge]:
     * tämä on paikallista tietoa eikä otteluluettelon tilaa, ja [TopViewModel] pysyy sinä
     * luokkana joka vain hakee sivun.
     */
    pending: List<PendingAction> = emptyList(),
    onOpenPending: (PendingAction) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.top_title)) },
                actions = {
                    if (state !is TopUiState.SignedOut) {
                        // Refresh koskee sitä listaa joka on auki, sama sääntö kuin
                        // loungella: kaksi eri sivua, eikä toista haeta toisen mukana.
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.top_refresh))
                        }
                        // Teksti eikä ikoni (Tommin päätös 4.9.2026 nähtyään molemmat
                        // laitteella). Ratas oli tässä hetken, ja se on nyt sana samalla
                        // tavalla kuin `Refresh` vieressään: palkissa ei ole yhtään kuvaa,
                        // joten mikään ei jää arvattavaksi.
                        TextButton(onClick = onOpenSettings) {
                            Text(stringResource(R.string.top_settings))
                        }
                    }
                },
            )
        },
    ) { insets ->
        // Keskeytyneet lähetykset ovat tilan **yläpuolella** eivätkä listan sisällä, ja se on
        // ehto eikä asettelu. Sama muoto kuin lautaruudulla ja samasta syystä: teko jäi
        // lähettämättä myös silloin kun luetteloa ei saatu haettua, ja juuri silloin sen
        // näkeminen on tärkeintä. Listan sisällä rivi olisi olemassa vain onnistuneen haun
        // jälkeen, eli poissa täsmälleen siinä tilassa jota varten se lisättiin.
        //
        // Mitattu 10.8.2026: yhteydettömänä käynnistetty sovellus jää tähän ruutuun eikä
        // lautaruutuun pääse lainkaan, koska ainoa reitti sinne kulki ottelurivin kautta.
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            // Uloskirjautuneena ei näytetä, koska rivi veisi ruutuun joka vaatii istunnon.
            // Jono ei katoa uloskirjautumisessa, joten rivi palaa kun kirjautuu takaisin.
            if (state !is TopUiState.SignedOut) {
                pending.forEach { action ->
                    PendingActionNotice(action = action, onOpen = { onOpenPending(action) })
                }
            }

            // Segmenttirivi vain kirjautuneena: uloskirjautuneena ruudulla on
            // kirjautumislomake, eikä kummallakaan listalla ole silloin mitään sanottavaa.
            if (state !is TopUiState.SignedOut) {
                SegmentRow(selected = segment, onSelect = onSelectSegment)
            }

            if (segment == MatchesSegment.Tournaments && state !is TopUiState.SignedOut) {
                // Vastustaja ja kierros luetaan tämän ruudun omasta luettelosta, koska
                // turnaussivu ei nimeä niitä. Null kun luetteloa ei ole, eikä sitä haeta
                // tätä varten; luettelolta puuttuva ottelu haetaan ottelumuistista.
                val listed = remember(state) {
                    (state as? TopUiState.Loaded)?.page?.matches?.associateBy { it.id }
                }
                OwnTournamentsSection(
                    state = tournaments,
                    onRefresh = onRefresh,
                    onOpenPage = onOpenPage,
                    listed = listed,
                    remembered = remembered,
                    opponentsRefresh = opponentsRefresh,
                )
                return@Column
            }

            when (state) {
                is TopUiState.SignedOut -> LoginScreen(
                    state = state,
                    onSignIn = onSignIn,
                )

                TopUiState.Loading -> WaitingForPage()

                is TopUiState.Failed -> RetryNote(
                    text = state.reason.text(),
                    onRetry = onRefresh,
                )

                // Sama ruutu kuin verkkovirheellä, koska käyttäjän teko on sama: yritä
                // uudelleen. Teksti on eri, koska tilanne on eri: yhteys toimi, ja
                // vastaus oli sivu jota tämä ruutu ei osaa lukea.
                TopUiState.NotATopPage -> RetryNote(
                    text = stringResource(R.string.top_not_a_match_list),
                    onRetry = onRefresh,
                )

                // Vanhentunut sisältö ei ole oma ruutunsa vaan sama odotusruutu kuin kylmässä
                // käynnistyksessä (Tommin päätös 4.9.2026 illalla, ensimmäisen version nähtyään).
                // Ruudulla ei ole mitään mitä lukea, joten sillä ei ole mitään mitä näyttää:
                // ottelurivit, odottavien luku ja viestihuomautus tulevat juuri haettavalta
                // sivulta, ja nimi ja arkiston reunapäivä katosivat niiden mukana vaikka ne
                // tulevat laitteelta. Ks. [TopUiState.Loaded.matchesStale].
                is TopUiState.Loaded -> if (state.matchesStale) {
                    WaitingForPage()
                } else {
                    MatchList(
                        page = state.page,
                        order = order,
                        onSort = onSort,
                        refreshing = state.refreshing,
                        contentPadding = PaddingValues(0.dp),
                        onOpenMatch = onOpenMatch,
                        onOpenMessages = onOpenMessages,
                        onOpenPage = onOpenPage,
                        archiveEdge = archiveEdge,
                        markCount = markCount,
                        onOpenMarks = onOpenMarks,
                    )
                }
            }
        }
    }
}

/** Kaksi rinnakkaista listaa, molemmat näkyvissä yhden napautuksen päässä. */
@Composable
private fun SegmentRow(selected: MatchesSegment, onSelect: (MatchesSegment) -> Unit) {
    val labels = mapOf(
        MatchesSegment.YourTurn to R.string.matches_segment_your_turn,
        MatchesSegment.Tournaments to R.string.matches_segment_tournaments,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .dgReadingSurface()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        MatchesSegment.entries.forEachIndexed { index, segment ->
            SegmentedButton(
                selected = segment == selected,
                onClick = { onSelect(segment) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = MatchesSegment.entries.size,
                ),
            ) {
                Text(text = stringResource(labels.getValue(segment)), maxLines = 1)
            }
        }
    }
}

/**
 * Odotusruutu: sivua ei ole vielä luettu, eikä ruudulla siksi ole mitään sanottavaa.
 *
 * **Yksi ruutu kahteen tilanteeseen** (Tommin päätös 4.9.2026 illalla). Kylmä käynnistys ei
 * ole vielä hakenut mitään, ja lautanäkymästä palaava haku tietää alkaessaan että edellinen
 * sivu on vanhentunut. Kumpaakin yhdistää se ettei ruudulla ole tietoa jota näyttää, joten
 * sen näyttäminen on sama teko molemmissa.
 *
 * Keskeytyneet lähetykset ovat tämän **yläpuolella** eivätkä tämän piirissä: ne ovat laitteen
 * omaa tietoa siitä mitä käyttäjä yritti, eivätkä ne kerro mitään siitä sivusta jota odotetaan.
 */
@Composable
private fun WaitingForPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Yksi keskeytynyt lähetys otteluluettelossa.
 *
 * **Teksti kertoo mitä ei tiedetä, ei mitä tapahtui**, sama sääntö kuin lautaruudun palkissa:
 * verkkokerros ei voi tietää ehtikö lähetys perille, koska yhteys voi katketa kummalla puolen
 * tahansa sitä hetkeä jolloin palvelin luki pyynnön.
 *
 * **Tässä ei ole uusinta- eikä hylkäysnappia**, ja se on tietoinen rajaus eikä puute. Uusinta
 * hakee lautasivun ensin ja päättää sen perusteella lähetetäänkö mitään, joten se kuuluu
 * ruutuun jossa sivu on. Kaksi paikkaa joissa saman rivin voi poistaa olisi kaksi totuutta
 * samasta teosta.
 */
@Composable
private fun PendingActionNotice(action: PendingAction, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.top_pending_title, action.submit),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.top_pending_explain),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onOpen, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.top_pending_open))
            }
        }
    }
}

@Composable
private fun MatchList(
    page: TopPage,
    order: MatchOrder,
    onSort: (MatchSortKey) -> Unit,
    refreshing: Boolean,
    contentPadding: PaddingValues,
    onOpenMatch: (Match) -> Unit,
    onOpenMessages: () -> Unit,
    onOpenPage: (String) -> Unit,
    archiveEdge: ArchiveEdgeState,
    markCount: Int,
    onOpenMarks: () -> Unit,
) {
    // Järjestys lasketaan kerran eikä rivi kerrallaan. Sivun oma lista säilyy koskemattomana
    // (`page.matches`), joten sivuston järjestykseen palataan jättämällä lajittelu pois eikä
    // hakemalla sivu uudelleen.
    val matches = remember(page, order) { page.matches.ordered(order) }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        // Latauspalkki jää vierityksen ulkopuolelle, koska se kertoo koko ruudun tilasta eikä
        // ole sisältöä. Se on muutaman pikselin korkuinen, joten se ei ole korkeudesta pois.
        // Paikka on varattu aina (`ProgressSlot`), jotta palkki ei siirrä listaa (3.9.2026).
        ProgressSlot(active = refreshing)

        // **Kaikki muu on saman vierityksen sisällä, myös yläosa** (Tommin pyyntö 10.8.2026).
        // Aiemmin yläosa oli kiinteä ja vain ottelurivit vierittyivät, ja se rikkoutui
        // laitteella: vaakatilassa yläosa täytti koko ikkunan, jolloin listalle jäi nolla
        // korkeutta eikä yhteenkään otteluun päässyt käsiksi. Kiinteä yläosa on aina veto siitä
        // että ikkuna on sitä korkeampi, eikä sitä vetoa voi voittaa kaikilla laitteilla.
        DgLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column {
                    page.user.name?.let { name ->
                        val profilePath = page.user.profilePath
                        // Rivi on linkki omaan profiiliin ja näyttää siltä (Tommin havainto
                        // 3.9.2026: *"ei näytä linkiltä"*): aksenttiväri ja alleviivaus, sama
                        // muoto kuin profiilin `Share .mat file` -linkillä. Alleviivaus on
                        // lupaus toiminnasta (`docs/UI.md`), joten se on vain kun polku on.
                        Text(
                            text = stringResource(R.string.top_signed_in_as, name),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (profilePath != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (profilePath != null) TextDecoration.Underline else null,
                            modifier = Modifier
                                .then(
                                    profilePath?.let { path ->
                                        Modifier.clickable { onOpenPage(path) }
                                    } ?: Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    // Odottavien määrä yhtenä rivinä, myös nollana (Tommin toive 31.8.2026:
                    // *"Matches-näyttö ja 0 odottavaa"*). Samaa lajia kuin reunapäivä alla:
                    // mittalukema eikä ilmoitus, joten se on paikallaan myös nollana, ja
                    // juuri silloin se on hyödyllisin. Tyhjä lista sanoo vain ettei otteluita
                    // näy, tämä sanoo ettei yksikään odota siirtoa.
                    YourTurnLine(count = page.yourTurnCount)

                    // Ennen otteluita ja ennen tyhjän listan tekstiä, koska arkisto on olemassa
                    // riippumatta siitä onko otteluita. Tyhjä lista ei saa viedä reunapäivää
                    // mukanaan.
                    ArchiveEdgeLine(edge = archiveEdge)

                    // Merkityt asemat samaa lajia kuin reunapäivä: laitteen oma tieto
                    // joka on olemassa riippumatta otteluista. Linkki eikä lukema, koska
                    // lista on sen takana (15.9.2026).
                    MarksLine(count = markCount, onOpen = onOpenMarks)

                    // Arkiston, asetusten ja manuaalin sisäänkäynnit olivat tässä riveinä
                    // 26.8.2026 asti, koska yläpalkissa ei ollut niille tilaa. Välilehtipalkki
                    // korvasi ne: sama peruste (ei nappeja yläpalkkiin) pätee yhä, mutta
                    // toiminnot asuvat nyt omassa palkissaan jonka leveys ei kilpaile
                    // otsikon kanssa.
                    if (page.hasMessageNotice) {
                        WaitingNotice(onOpenMessages = onOpenMessages)
                    }
                }
            }

            if (matches.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.top_empty),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                return@DgLazyColumn
            }

            // Lajitteluvalinta vierittyy sisällön mukana eikä jää kiinni yläreunaan. Kiinni
            // jäävä rivi olisi sama veto korkeudesta kuin kiinteä yläosa oli, pienempänä.
            item {
                Column {
                    HorizontalDivider()
                    MatchSortRow(order = order, onSort = onSort)
                    HorizontalDivider()
                }
            }

            items(matches, key = { it.id.value }, contentType = { DgStripedRow }) { match ->
                // Klikattava vain kun sivu antoi pelilinkin. Profiilisivun otteluilla
                // playPath on null, ja klikattavalta näyttävä rivi joka ei ole on huonompi
                // kuin rivi joka ei näytä klikattavalta.
                MatchRow(
                    match = match,
                    onOpen = match.playPath?.let { { onOpenMatch(match) } },
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * Odottavien määrä yhtenä rivinä (`It is your turn in N matches`, 3.9.2026 asti
 * `N waiting for your move`).
 *
 * Luku on [TopPage.yourTurnCount], eli luettelosta johdettu eikä erikseen laskettu, joten
 * rivi ei voi olla eri mieltä rivien kanssa. Väri on sama kuin rivin `Your turn`
 * -tunnuksella kun luku on yli nollan, ja nollana toissijainen: nolla ei ole varoitus vaan
 * lukema, ja lukema saa näyttää levolliselta.
 */
@Composable
private fun YourTurnLine(count: Int) {
    // Sanamuoto on Tommin (3.9.2026): *"It is your turn in 9 matches"*. Yksikkö ja nolla
    // valitaan koodissa eikä plurals-resurssilla, samasta syystä kuin
    // `settings_changes_count`: sovellus on yksikielinen.
    val text = when (count) {
        0 -> stringResource(R.string.top_your_turn_none)
        1 -> stringResource(R.string.top_your_turn_one)
        else -> stringResource(R.string.top_your_turn_count, count)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (count > 0) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
    )
}

/**
 * Arkiston reunapäivä yhtenä rivinä.
 *
 * Rivi näytetään myös tyhjänä, eli tyhjyys sanotaan ääneen sen sijaan että rivi jätettäisiin
 * pois. Puuttuva rivi ja rikkinäinen rivi näyttäisivät samalta, ja tämän rivin koko tehtävä
 * on olla se paikka josta arkiston ulottuvuuden näkee ilman arvailua.
 *
 * Vaatimaton tyyli on tarkoituksellinen. Tämä ei ole varoitus vaan mittalukema: se on
 * hyödyllinen juuri siksi että se on aina samassa paikassa myös silloin kun kaikki on hyvin.
 */
@Composable
private fun ArchiveEdgeLine(edge: ArchiveEdgeState) {
    Text(
        // Kolme tilaa eikä kaksi, 5.9.2026 alkaen. Lukematon kanta sanoo lukevansa eikä
        // väitä arkistosta mitään, koska ennen tätä se sanoi arkiston olevan tyhjä.
        text = when (edge) {
            ArchiveEdgeState.Unknown -> stringResource(R.string.archive_edge_loading)
            ArchiveEdgeState.Empty -> stringResource(R.string.archive_edge_empty)
            is ArchiveEdgeState.Reaches -> stringResource(
                R.string.archive_edge,
                ArchiveEdge.formatDate(edge.storedAtEpochMillis),
            )
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
    )
}

/**
 * Merkittyjen asemien linkki. Yksikkö ja nolla koodissa kuten [YourTurnLine]issa, ja
 * alleviivaus koska rivi tekee jotain (`docs/UI.md`, *Alleviivaus on lupaus*). Myös
 * nollana linkki, jotta lista löytyy ennen ensimmäistä merkkiä eikä vasta sen jälkeen.
 */
@Composable
private fun MarksLine(count: Int, onOpen: () -> Unit) {
    val text = when (count) {
        0 -> stringResource(R.string.top_marks_none)
        1 -> stringResource(R.string.top_marks_one)
        else -> stringResource(R.string.top_marks_count, count)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
    )
}

/**
 * Ilmoitus siitä että jotain odottaa.
 *
 * Tässä ei ollut avausnappia ennen 9.8.2026, koska avaaminen kuluttaa kohteen jonosta eikä
 * viestien jäsennintä ollut olemassa: nappi ennen jäsennintä olisi hävittänyt kohteen
 * tallentumatta, eli tehnyt juuri sen mitä sovellus on tehty estämään. Väli on nyt olemassa
 * (`InboxItem.toMessage`), joten ehto on täyttynyt.
 *
 * **Nappi vie viestiruutuun eikä hae mitään.** Kuluttava teko tapahtuu vasta siellä, ja se on
 * eri asia kuin siirtyminen: nappi jonka nimi on `Open` ja joka kuluttaa kohteen olisi tässä
 * kohtaa lupaus jota ei voi perua, ja arkisto johon tulos päätyy ei olisi näkyvissä.
 */
@Composable
private fun WaitingNotice(onOpenMessages: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.top_something_waiting),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.top_something_waiting_explain),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onOpenMessages,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.top_something_waiting_open))
            }
        }
    }
}

/**
 * Lajitteluperusteet siinä järjestyksessä kuin DailyGammon sarakkeensa antaa.
 *
 * **Painot poistuivat 4.9.2026 taulukon mukana** (Tommin havainto: *"liian vähän
 * sovellukselta ja liian paljon nostalgialta"*). Sarakeleveydet olivat mitattuja arvoja
 * (Time Pool sai lisää painoa vastustajan kustannuksella 26.8.2026, 320 dp ja fontScale
 * 1.3), mutta ne mitattiin kohdakkain seisovalle taulukolle jota ei enää ole. Nimet
 * jäivät, koska ne ovat nyt lajittelun nimiä.
 */
private data class MatchColumn(
    val labelRes: Int,
    /** Lajitteluperuste jonka tämän valinnan napautus asettaa. */
    val key: MatchSortKey,
)

private val MATCH_COLUMNS = listOf(
    MatchColumn(R.string.col_grace, MatchSortKey.GRACE),
    MatchColumn(R.string.col_time_pool, MatchSortKey.TIME_POOL),
    MatchColumn(R.string.col_round, MatchSortKey.ROUND),
    MatchColumn(R.string.col_length, MatchSortKey.LENGTH),
    MatchColumn(R.string.col_opponent, MatchSortKey.OPPONENT),
)

/**
 * Lajitteluvalinta kerran listan yläpuolella.
 *
 * **Tämä oli sarakeotsikkorivi 4.9.2026 asti**, ja sen napautus lajitteli sen sarakkeen
 * mukaan. Kun rivit lakkasivat olemasta kohdakkain, otsikkorivillä ei ollut enää mitään
 * otsikoitavaa, mutta lajittelu jäi: samat viisi perustetta ovat nyt valintoja. Suunta on
 * yhä nuoli nimen perässä eikä oma ikoninsa, samasta syystä kuin ennen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchSortRow(order: MatchOrder, onSort: (MatchSortKey) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.top_sort_by),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MATCH_COLUMNS.forEach { column ->
            val active = order.key == column.key
            FilterChip(
                selected = active,
                onClick = { onSort(column.key) },
                label = {
                    Text(
                        text = stringResource(column.labelRes) + when {
                            !active -> ""
                            order.descending -> " ↓"
                            else -> " ↑"
                        },
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

/**
 * Yksi ottelu listalla.
 *
 * **Rivi ei ole enää taulukon rivi** (Tommin havainto 4.9.2026). Arvot olivat kohdakkain
 * sarakeotsikoiden alla, ja se on HTML-taulukon muoto eikä sovelluksen. Nyt jokainen luku
 * kantaa oman selitteensä, jolloin rivin leveys ei enää sido lukuja toisiinsa eikä pisin
 * aika katkea kapealla ruudulla.
 *
 * Selite toistuu jokaisella rivillä, ja se on tietoinen hinta. `docs/UI.md` perusteli
 * 26.8.2026 kerran sanotun otsikon juuri sillä ettei selite toistu, ja peruste piti niin
 * kauan kuin rivit olivat kohdakkain.
 *
 * **Vuoro on pilleri eikä tekstiä muun tekstin joukossa** (sama havainto). Se oli rivin
 * oikeassa reunassa samaa kokoa kuin data, joten sen näki vasta lukemalla. Pilleri on
 * pinta ja muoto värin lisäksi, ja vastustajan vuoro saa hillityn pinnan koska se ei ole
 * toimintakehotus.
 */
@Composable
private fun MatchRow(match: Match, onOpen: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tapahtumanimi saa jaljelle jaavan tilan ja katkeaa itse. Ilman painoa se vei
            // koko leveyden ja puristi vuorotunnuksen kirjaimen levyiseksi pylvaaksi
            // (mitattu 26.8.2026, 320 dp ja fontScale 1.3). Nimi on toistuvaa tekstia
            // (`DG 8x8 2026 - C vs F - Div 1`), tunnus ei, joten katkaisu kuuluu nimelle.
            Text(
                text = match.eventName,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Tunnusta jatetaan nayttamatta kokonaan kun sivu ei kerro vuoroa. Profiilisivun
            // otteluista sita ei tieda kumpikaan vaihtoehto, ja odottaa olisi niista vaara
            // juuri niissa joissa on vuoro.
            match.myTurn?.let { myTurn -> TurnPill(myTurn) }
        }

        // Vastustaja omalle rivilleen: han on se toinen osapuoli eika yksi luku muiden
        // joukossa, ja nimi on ainoa kentta jota ei lueta numerona.
        Text(
            text = stringResource(R.string.top_versus, match.opponent.displayName),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )

        // Aikakentat naytetaan raakana. Sovellus ei muunna eika yhdista lukemia, joten se ei
        // vaita niista enempaa kuin sivu itse sanoo.
        //
        // Puuttuva arvo jatetaan pois kokonaan eika merkita viivalla: profiilisivun
        // paattyneilta otteluilta Grace ja Time Pool puuttuvat, ja tyhja selite vaittaisi
        // etta lukema on olemassa mutta tyhja.
        //
        // Lahella nollaa oleva aika erottuu, ja kaksi lukemaa erottuvat eri tavalla, koska
        // ne tarkoittavat eri asiaa (`MatchClock`, mitattu kaksivaiheinen kello). Nollaan
        // tullut Grace kertoo etta Time Pool kuluu juuri nyt, ja vahiin kaynyt Time Pool
        // kertoo etta ottelu on lahella aikatappiota. Vain jalkimmainen havittaa ottelun,
        // ja se saa siksi virhevarin.
        val values = listOfNotNull(
            match.graceText?.let {
                Triple(R.string.top_value_grace, it, if (MatchClock.graceSpent(match)) 1 else 0)
            },
            match.timePoolText?.let {
                Triple(R.string.top_value_pool, it, if (MatchClock.poolLow(match)) 2 else 0)
            },
            // Viiva on sivuston oma merkki puuttuvalle kierrokselle (liigaottelut), ja
            // selitteen kanssa se lukisi `Round -`. Puuttuva jaetaan pois kokonaan.
            match.round?.takeIf { it.isNotBlank() && it != "-" }
                ?.let { Triple(R.string.top_value_round, it, 0) },
            match.matchLength?.let { Triple(R.string.top_value_length, it.toString(), 0) },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            values.forEach { (labelRes, value, alarm) ->
                Text(
                    text = stringResource(labelRes, value),
                    style = MaterialTheme.typography.bodySmall,
                    // Lihavointi on toinen kanava varin rinnalla, jottei merkinta nojaa
                    // pelkkaan variin.
                    fontWeight = if (alarm > 0) FontWeight.Bold else null,
                    color = when (alarm) {
                        2 -> MaterialTheme.colorScheme.error
                        1 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

/** Vuoro pillerina. Muoto ja pinta erottavat sen datasta, ei pelkka vari. */
@Composable
private fun TurnPill(myTurn: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (myTurn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(
            text = stringResource(
                if (myTurn) R.string.top_your_turn else R.string.top_waiting
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            color = if (myTurn) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
