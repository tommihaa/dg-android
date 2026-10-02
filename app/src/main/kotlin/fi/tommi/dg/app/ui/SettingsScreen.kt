package fi.tommi.dg.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.AppTheme
import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.app.session.BusyStyle
import fi.tommi.dg.app.session.DeviceSettingsGroup
import fi.tommi.dg.app.session.DiceStyle
import fi.tommi.dg.app.session.ScoreStyle
import fi.tommi.dg.app.session.SettingsSnapshot
import fi.tommi.dg.app.session.SettingsTransfer
import fi.tommi.dg.domain.PreferenceChoice
import fi.tommi.dg.domain.PreferenceToggle
import fi.tommi.dg.domain.SettingsPage
import fi.tommi.dg.domain.SettingsUpdate
import fi.tommi.dg.domain.SiteBoardSettings
import fi.tommi.dg.domain.BOARD_SCHEME_GROUP
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.ChoiceOption
import fi.tommi.dg.domain.siteBoardSettings
import androidx.compose.ui.unit.Dp
import fi.tommi.dg.net.DgClient

/**
 * Omat DailyGammon-asetukset, **luettuna ja muokattavana**.
 *
 * **Sopimusmuutos 25.8.2026, ja se on kirjattu kahdesti ennen tätä tiedostoa**
 * (`docs/UI.md`, `docs/ASETUKSET.md` luku 5). Ruutu oli tähän asti tarkoituksella lukutila,
 * ja peruste oli tosi: lomake lähtee kokonaisena, joten yksi väärin luettu kenttä on
 * menetetty asetus jonka alkuperäistä tilaa ei ole enää missään. Peruste ei kumoutunut vaan
 * **maksettiin**: lähetyksen kokoaa `SettingsPage.update` neljän vartion takana, tallennus
 * lukee sivun kolmesti, ja ensimmäinen luku ennen ensimmäistä kirjoitusta on säilössä.
 *
 * **Mikä tässä ruudussa on eri kuin muissa.** Muualla sovelluksessa ruudun tila on se mitä
 * sivustolla lukee. Täällä on kaksi tilaa: [SettingsPage] on se mitä sivustolla lukee, ja
 * paikallinen muokkaustila on se mitä käyttäjä on raksittanut. Ne eroavat siitä hetkestä
 * kun ensimmäistä ruutua kosketaan siihen että lähetys on todennettu, eikä eroa piiloteta:
 * napin vieressä lukee montako asetusta on muuttumassa, koska yhden ruudun raksitus
 * lähettää silti kaikki yksitoista kenttää.
 *
 * **Muokkaustila nollautuu sivun mukana.** `rememberSaveable(page)` sitoo sen luettuun
 * sivuun, joten onnistunut tallennus ja päivitys molemmat aloittavat puhtaalta. Se on
 * tarkoitus: kesken jäänyt raksi vanhan sivun päällä olisi muutos jota käyttäjä ei enää näe
 * suhteessa mihinkään.
 *
 * **Muokkaustila kestää taustasammutuksen 31.8.2026 alkaen** (Tommin päätös,
 * kompositioauditoinnin kysymys 2). Aiempi `remember` hävitti keskeneräiset raksit kun
 * Android sammutti sovelluksen taustalla, ja juuri tämä lomake on työläin näpytellä
 * uudestaan. Kääntöä tämä ei koske eikä sen tarvitse: manifesti lukitsee pystyasennon ja
 * `configChanges` estää uudelleenluonnin, joten kääntyvää tilannetta ei ole. Todennettu
 * laitteella 31.8.2026: raksi säilyi `am kill` -tapon ja uudelleenkäynnistyksen yli, ja
 * navigointi palasi asetusruutuun itsestään. Mikään ei lähde
 * sivustolle ennen Savea, joten säilyvyys on kokonaan laitteen sisäinen asia; levylle asti
 * tallennettua säilöä ei tehty, koska päiviä vanha puskuri palaisi sivulle joka on voinut
 * sillä välin muuttua sivustolla. Ks. raja [SettingsForm]in puskurin kommentissa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    saveResult: SettingsSaveResult?,
    storedBaseline: SettingsSnapshot?,
    /** Laudan tyyli, laitteen oma valinta. Ks. [DeviceSection]. */
    boardStyle: BoardStyle,
    onBoardStyleChange: (BoardStyle) -> Unit,
    /** Ottelukohtainen lauta ja sen arvontajoukko (Tommi 27.9.2026). Ks. `BoardShuffle.kt`. */
    boardShuffle: Boolean = false,
    onBoardShuffleChange: (Boolean) -> Unit = {},
    shufflePool: Set<BoardStyle> = emptySet(),
    onShufflePoolChange: (Set<BoardStyle>) -> Unit = {},
    /** Noppien ja pisteiden esitys, omat kytkimensä (Tommi 27.8.2026). Ks. [DeviceSection]. */
    diceStyle: DiceStyle,
    onDiceStyleChange: (DiceStyle) -> Unit,
    scoreStyle: ScoreStyle,
    onScoreStyleChange: (ScoreStyle) -> Unit,
    /** Sovelluksen teema (Tommi 22.9.2026), kirjoittaa laudan ja ilmaisimen kerralla. */
    appTheme: AppTheme,
    onAppThemeChange: (AppTheme) -> Unit,
    /** Odotuksen ilmaisin nappien paikalla, oma kytkin (Tommi 18.9.2026). Ks. [DeviceSection]. */
    busyStyle: BusyStyle,
    onBusyStyleChange: (BusyStyle) -> Unit,
    /** Deco-viimeistely odotuksen ilmaisimille (Tommi 22.9.2026). Ks. `BusyDeco.kt`. */
    busyDeco: Boolean,
    onBusyDecoChange: (Boolean) -> Unit,
    /** Pakollisten askelten esipoiminta, oma kytkin (Tommi 2.9.2026). Ks. [DeviceSection]. */
    playForcedSteps: Boolean,
    onPlayForcedStepsChange: (Boolean) -> Unit,
    /** Ahne uloskanto ilman kontaktia, toinen kytkin samassa osiossa (Tommi 2.9.2026). */
    playGreedyBearoff: Boolean,
    onPlayGreedyBearoffChange: (Boolean) -> Unit,
    /** Siirtonuolet kootusta siirrosta (Tommi 29.9.2026), oletus pois. Ks. `MoveArrowsStore`. */
    moveArrows: Boolean,
    onMoveArrowsChange: (Boolean) -> Unit,
    /** Vastustajan edellisen siirron nuolet (Tommi 29.9.2026 illalla), oletus pois. */
    opponentArrows: Boolean,
    onOpponentArrowsChange: (Boolean) -> Unit,
    /** `Confirm Beaver` voimassa olevana arvona (Tommi 27.9.2026). Ks. `BeaverConfirmStore`. */
    confirmBeaver: Boolean,
    onConfirmBeaverChange: (Boolean) -> Unit,
    /** Noppien painallus tekona, kaksi kytkintä (Tommin tilaus 4.9.2026). Ks. `diceTapFor`. */
    diceSubmitTap: Boolean,
    onDiceSubmitTapChange: (Boolean) -> Unit,
    diceSwapTap: Boolean,
    onDiceSwapTapChange: (Boolean) -> Unit,
    /** Lukuruutujen pystylukko (Tommi 2.9.2026), lauta on vaakaan aina. Ks. [DeviceSection]. */
    portraitLock: Boolean,
    onPortraitLockChange: (Boolean) -> Unit,
    fullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    /** Lauta seuraa laitteen asentoa (Tommin päätös 23.9.2026), oletus pois. */
    boardRotates: Boolean,
    onBoardRotatesChange: (Boolean) -> Unit,
    /** Kätisyys (Tommin päätös 9.9.2026), oletus oikea. Ks. [DeviceSection]. */
    leftHanded: Boolean,
    onLeftHandedChange: (Boolean) -> Unit,
    /** Taustakuvion sumi-e-tila (Tommin päätös 6.9.2026), oletus pois. Ks. [DeviceSection]. */
    sumiE: Boolean,
    onSumiEChange: (Boolean) -> Unit,
    /** Taustakuvio päällä vai pois, molemmat teemat (Tommin päätös 6.9.2026), oletus pois. Ks. [DeviceSection]. */
    pattern: Boolean,
    onPatternChange: (Boolean) -> Unit,
    /**
     * Taustakuvien kansio (Tommin päätökset 15.9.2026), null kun kuvia ei näytetä. Kansio on
     * itse kytkin, ks. `WallpaperStore`. Ks. [DeviceSection].
     */
    wallpaperFolder: String?,
    onChooseWallpaperFolder: () -> Unit,
    onClearWallpaperFolder: () -> Unit,
    /** Rarity-värit nimissä ja Inboxin määrässä (Tommin tilaus 26.9.2026), oletus päällä. */
    rarity: Boolean,
    onRarityChange: (Boolean) -> Unit,
    /**
     * Sovelluslukko käynnistyksessä (Tommin tilaus 27.9.2026), oletus pois. [appLockAvailable]
     * on väärin kun laitteella ei ole mitään millä lukon avaisi, ja kytkin on silloin harmaa.
     */
    appLock: Boolean,
    appLockAvailable: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    /** Kiinni olevat laiteasetusten ryhmät. Ks. [DeviceSection] ja `SettingsFoldStore`. */
    folds: Set<DeviceSettingsGroup>,
    onToggleFold: (DeviceSettingsGroup) -> Unit,
    onRefresh: () -> Unit,
    onSave: (Set<String>, Map<String, String>) -> Unit,
    onDismissResult: () -> Unit,
    /** Paluu Info-välilehden listaan. Ks. `InfoScreen`. */
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** DailyGammon-segmentti avattiin; sivu haetaan ensimmäisellä kerralla. */
    onOpenSite: () -> Unit = {},
    /** Sivuston lauta-asetukset säilöstä, jotta `Follow my DailyGammon settings` esikatselee oikein. */
    siteBoard: SiteBoardSettings = SiteBoardSettings.UNKNOWN,
    /** Asetusten vienti ja tuonti laiteosion alla (Tommi 28.9.2026); null piilottaa sen. */
    settingsTransfer: SettingsTransfer? = null,
    onSettingsImported: () -> Unit = {},
) {
    // Kaksi segmenttiä, laiteosio auki oletuksena (Tommin päätös 22.9.2026, `docs/UI.md`).
    // Kummallakin on yksi tallennussääntö: laiteosio tallentuu heti, sivuston lomake vain
    // napista. Yhdessä vierityksessä sääntö vaihtui kesken ruudun näkymättä.
    var siteShown by rememberSaveable { mutableStateOf(false) }
    if (siteShown) LaunchedEffect(Unit) { onOpenSite() }
    // Segmentin sisältö poistuu koostumuksesta vaihdossa, joten lomakkeen raksit ja
    // kummankin vierityskohta säilötään avaimittain. Ilman tätä segmentin vaihto
    // hävittäisi lähettämättömät muutokset hiljaa.
    val saveable = rememberSaveableStateHolder()

    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.settings_title)) },
                // Back-nappi tuli 26.8.2026 kun asetukset siirtyivät Info-välilehden
                // sisään: ruutu on nyt listarivin takana eikä oma välilehtensä.
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.board_back))
                    }
                },
                actions = {
                    // Laiteosiossa ei ole mitään haettavaa.
                    if (siteShown) {
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.top_refresh))
                        }
                    }
                },
            )
        },
    ) { insets ->
        // Laiteosio on tilahaarojen ulkopuolella tarkoituksella: se ei tarvitse verkkoa
        // eikä istuntoa, joten sen on oltava käytettävissä myös silloin kun sivuston
        // lomake ei latautunut.
        //
        // Koko ruutu vierii yhtenä sarakkeena (Tommin havainto 3.9.2026). Laiteosio oli
        // tähän asti kiinteä ja sivuston lomake vieri omassa painollisessa laatikossaan
        // sen alla; tabletin vaakatilassa laiteosio täytti koko korkeuden, joten
        // laatikolle ei jäänyt riviäkään eikä mikään vierinyt. Yksi vieritys kattaa
        // molemmat, ja tilahaarat piirtyvät laiteosion perään sisältönsä korkuisina.
        //
        // 22.9.2026 alkaen osiot ovat eri segmenteissä, ja kumpikin vierii omana
        // sarakkeenaan segmenttirivin alla. Vaakatilan ongelma ei palaa, koska laiteosio ei
        // enää jaa korkeutta lomakkeen kanssa.
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .dgReadingSurface()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                listOf(false, true).forEachIndexed { index, site ->
                    SegmentedButton(
                        selected = siteShown == site,
                        onClick = { siteShown = site },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    ) {
                        Text(
                            stringResource(
                                if (site) R.string.settings_segment_site else R.string.settings_device_section
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
            // Segmentin sisältö saa sarakkeen jäljelle jäävän korkeuden painolla, ja laatikko
            // rajaa piirron omiin rajoihinsa: ilman tätä vieritetty sisältö piirtyi
            // segmenttirivin ja läpinäkyvän yläpalkin päälle (laitteella 22.9.2026).
            Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                // Kumpikin segmentti on oma haaransa kiinteällä avaimella. Yksi provider
                // vaihtuvalla avaimella ja sisällä luettu valinta jätti ruudulle väärän
                // segmentin sisällön (laitteella 22.9.2026).
                if (siteShown) {
                    saveable.SaveableStateProvider("site") {
                        // Ladattu lomake vierittää itse, jotta tallennusalue voi pysyä ruudun
                        // alareunassa vierivän alueen ulkopuolella, ks. [SettingsForm].
                        if (state is SettingsUiState.Loaded) {
                            SettingsForm(
                                page = state.page,
                                refreshing = state.refreshing,
                                storedBaseline = storedBaseline,
                                onSave = onSave,
                                header = { SiteOnlyNote() },
                            )
                        } else Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .dgReadingSurface(),
                        ) {
                            SiteOnlyNote()
                            Box(modifier = Modifier.fillMaxWidth()) {
                                when (state) {
                                    SettingsUiState.Loading -> Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        BusyIndicator(Modifier, ArcLook.X22, track = DgBoard.Palette.Frame)
                                    }

                                    is SettingsUiState.Failed -> RetryNote(
                                        text = state.reason.text(),
                                        onRetry = onRefresh,
                                    )

                                    SettingsUiState.NotSettingsPage -> RetryNote(
                                        text = stringResource(R.string.settings_wrong_page),
                                        onRetry = onRefresh,
                                    )

                                    SettingsUiState.SessionExpired -> RetryNote(
                                        text = stringResource(R.string.board_session_expired),
                                        onRetry = null,
                                    )

                                    is SettingsUiState.Loaded -> Unit // piirretty yllä
                                }
                            }
                        }
                    }
                } else {
                    saveable.SaveableStateProvider("device") {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .dgReadingSurface(),
                        ) {
                            DeviceSection(
                                siteBoard = siteBoard,
                                appTheme = appTheme,
                                onAppThemeChange = onAppThemeChange,
                                boardStyle = boardStyle,
                                onBoardStyleChange = onBoardStyleChange,
                                boardShuffle = boardShuffle,
                                onBoardShuffleChange = onBoardShuffleChange,
                                shufflePool = shufflePool,
                                onShufflePoolChange = onShufflePoolChange,
                                diceStyle = diceStyle,
                                onDiceStyleChange = onDiceStyleChange,
                                scoreStyle = scoreStyle,
                                onScoreStyleChange = onScoreStyleChange,
                                busyStyle = busyStyle,
                                onBusyStyleChange = onBusyStyleChange,
                                busyDeco = busyDeco,
                                onBusyDecoChange = onBusyDecoChange,
                                playForcedSteps = playForcedSteps,
                                onPlayForcedStepsChange = onPlayForcedStepsChange,
                                playGreedyBearoff = playGreedyBearoff,
                                onPlayGreedyBearoffChange = onPlayGreedyBearoffChange,
                                moveArrows = moveArrows,
                                onMoveArrowsChange = onMoveArrowsChange,
                                opponentArrows = opponentArrows,
                                onOpponentArrowsChange = onOpponentArrowsChange,
                                confirmBeaver = confirmBeaver,
                                onConfirmBeaverChange = onConfirmBeaverChange,
                                diceSubmitTap = diceSubmitTap,
                                onDiceSubmitTapChange = onDiceSubmitTapChange,
                                diceSwapTap = diceSwapTap,
                                onDiceSwapTapChange = onDiceSwapTapChange,
                                portraitLock = portraitLock,
                                onPortraitLockChange = onPortraitLockChange,
                                fullScreen = fullScreen,
                                onFullScreenChange = onFullScreenChange,
                                boardRotates = boardRotates,
                                onBoardRotatesChange = onBoardRotatesChange,
                                leftHanded = leftHanded,
                                onLeftHandedChange = onLeftHandedChange,
                                sumiE = sumiE,
                                onSumiEChange = onSumiEChange,
                                pattern = pattern,
                                onPatternChange = onPatternChange,
                                wallpaperFolder = wallpaperFolder,
                                onChooseWallpaperFolder = onChooseWallpaperFolder,
                                onClearWallpaperFolder = onClearWallpaperFolder,
                                rarity = rarity,
                                onRarityChange = onRarityChange,
                                appLock = appLock,
                                appLockAvailable = appLockAvailable,
                                onAppLockChange = onAppLockChange,
                                folds = folds,
                                onToggleFold = onToggleFold,
                            )
                            settingsTransfer?.let { SettingsTransferGroup(it, onSettingsImported) }
                        }
                    }
                }
            }
        }
    }

    if (saveResult != null) {
        SaveResultDialog(result = saveResult, onDismiss = onDismissResult)
    }
}

/**
 * Rajausrivi lomakkeen yllä (Tommin päätös 22.9.2026): salasana ja julkinen profiili
 * vaihdetaan sivustolla, ja rivi avaa sivuston asetussivun laitteen selaimessa. Sivulla on
 * kolme lomaketta, ja sovellus lähettää niistä vain `/bg/profile/pref`in
 * (`docs/ASETUKSET.md` luku 5). Rivi näkyy myös silloin kun lomake ei latautunut, koska
 * selain ei tarvitse sovelluksen istuntoa. Osoite näkyy ennen napautusta kuten Links-sivulla.
 *
 * Paikka on segmentin ylälaita eikä alalaita, koska sivuston tunteva pelaaja etsii
 * salasanaa sieltä mistä sivuston oma sivu sen alkaa (Tommin tarkennus samana iltana).
 */
@Composable
private fun SiteOnlyNote() {
    val uriHandler = LocalUriHandler.current
    val url = DgClient.DEFAULT_BASE_URL + "bg/profile"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_site_only),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.settings_site_only_link),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

/**
 * Laitteen oma osio: laudan tyyli (`docs/ASETUKSET.md` luku 3).
 *
 * Valinta esitetään sivuston lomakkeen radiorivien näköisenä mutta omassa osiossaan oman
 * otsikkonsa alla, ja selite sanoo eron ääneen: nämä eivät lähde koskaan verkkoon. Sama
 * ruutu näyttää muuten kahdenlaisia asetuksia joista toiset lähtevät ja toiset eivät, eikä
 * käyttäjä voi nähdä erotusta ruudusta ilman että se sanotaan.
 *
 * Muutos tallentuu heti valinnasta ilman lähetysnappia, ja sekin on osa eroa: sivuston
 * lomake vaatii napin koska lähetys on riski, paikallinen valinta ei ole.
 */
@Composable
private fun DeviceSection(
    siteBoard: SiteBoardSettings,
    appTheme: AppTheme,
    onAppThemeChange: (AppTheme) -> Unit,
    boardStyle: BoardStyle,
    onBoardStyleChange: (BoardStyle) -> Unit,
    boardShuffle: Boolean,
    onBoardShuffleChange: (Boolean) -> Unit,
    shufflePool: Set<BoardStyle>,
    onShufflePoolChange: (Set<BoardStyle>) -> Unit,
    diceStyle: DiceStyle,
    onDiceStyleChange: (DiceStyle) -> Unit,
    scoreStyle: ScoreStyle,
    onScoreStyleChange: (ScoreStyle) -> Unit,
    busyStyle: BusyStyle,
    onBusyStyleChange: (BusyStyle) -> Unit,
    busyDeco: Boolean,
    onBusyDecoChange: (Boolean) -> Unit,
    playForcedSteps: Boolean,
    onPlayForcedStepsChange: (Boolean) -> Unit,
    playGreedyBearoff: Boolean,
    onPlayGreedyBearoffChange: (Boolean) -> Unit,
    moveArrows: Boolean,
    onMoveArrowsChange: (Boolean) -> Unit,
    opponentArrows: Boolean,
    onOpponentArrowsChange: (Boolean) -> Unit,
    confirmBeaver: Boolean,
    onConfirmBeaverChange: (Boolean) -> Unit,
    diceSubmitTap: Boolean,
    onDiceSubmitTapChange: (Boolean) -> Unit,
    diceSwapTap: Boolean,
    onDiceSwapTapChange: (Boolean) -> Unit,
    portraitLock: Boolean,
    onPortraitLockChange: (Boolean) -> Unit,
    fullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    boardRotates: Boolean,
    onBoardRotatesChange: (Boolean) -> Unit,
    leftHanded: Boolean,
    onLeftHandedChange: (Boolean) -> Unit,
    sumiE: Boolean,
    onSumiEChange: (Boolean) -> Unit,
    pattern: Boolean,
    onPatternChange: (Boolean) -> Unit,
    wallpaperFolder: String?,
    onChooseWallpaperFolder: () -> Unit,
    onClearWallpaperFolder: () -> Unit,
    /** Rarity-värit nimissä ja Inboxin määrässä (Tommin tilaus 26.9.2026), oletus päällä. */
    rarity: Boolean,
    onRarityChange: (Boolean) -> Unit,
    /**
     * Sovelluslukko käynnistyksessä (Tommin tilaus 27.9.2026), oletus pois. [appLockAvailable]
     * on väärin kun laitteella ei ole mitään millä lukon avaisi, ja kytkin on silloin harmaa.
     */
    appLock: Boolean,
    appLockAvailable: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    folds: Set<DeviceSettingsGroup>,
    onToggleFold: (DeviceSettingsGroup) -> Unit,
) {
    // Osion nimi on segmenttirivillä (22.9.2026), joten tässä on vain sen sääntö.
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.settings_device_explain),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
        )
        // Teema ylimpänä, koska se kirjoittaa alempia valintoja: lauta ja ilmaisimen
        // viimeistely vaihtuvat sen mukana ja jäävät sen jälkeen säädettäviksi (22.9.2026).
        val situation = rememberPreviewSituation()
        val board = situation?.board(siteDice = diceStyle == DiceStyle.SITE)
        val look = BoardLook.of(boardStyle, siteBoard, pageScheme = null, decoLight = decoLightBoard())
        // Suurennettu kortti, null kun mitään ei ole auki. Kaksi muuttujaa eikä yksi, koska
        // teemakortti piirtää laudan teeman pinnalla ja lautakortti ilman.
        var zoomTheme by remember { mutableStateOf<AppTheme?>(null) }
        var zoomBoard by remember { mutableStateOf<BoardStyle?>(null) }
        // Kytkin päällä kortti on arvontajoukon jäsen eikä valinta: napautus lisää tai poistaa,
        // eikä viimeistä voi poistaa, koska tyhjästä joukosta ei arvota mitään. Valittu lauta
        // jää talteen koskematta ja palaa kun kytkin sammuu (Tommin valinta 27.9.2026).
        val pickBoard: (BoardStyle) -> Unit = { style ->
            if (!boardShuffle) {
                onBoardStyleChange(style)
            } else if (style !in shufflePool) {
                onShufflePoolChange(shufflePool + style)
            } else if (shufflePool.size > 1) {
                onShufflePoolChange(shufflePool - style)
            }
        }
        // Kätisyyden kortti suurennettuna (Tommin tilaus 24.9.2026: *"handedness kuvat
        // tarvitsevat suurennuslasi-toiminnon"*). Arvo on vasenkätisyys, null kun kiinni.
        var zoomHand by remember { mutableStateOf<Boolean?>(null) }
        // Kätisyyden kortit ovat toistensa peilikuvat eivätkä seuraa tilin laudan suuntaa
        // (Tommin valinta 27.9.2026): Left hand näyttää kotikentän vasemmalla, eli sen
        // siirtokäden puolella, ja selite kertoo mistä kentästä suunnan saa.
        val decoLight = decoLightBoard()
        val handLook: (Boolean) -> BoardLook = { left ->
            BoardLook.of(boardStyle, siteBoard.copy(homeBoardsLeft = left), pageScheme = null, decoLight = decoLight)
        }
        if (board != null) {
            zoomTheme?.let { theme ->
                val themeStyle = themeBoardStyle(theme, boardStyle)
                BoardZoomDialog(
                    label = stringResource(if (theme == AppTheme.DECO) decoThemeLabel() else R.string.settings_theme_plain),
                    onUse = { onAppThemeChange(theme); zoomTheme = null },
                    onClose = { zoomTheme = null },
                ) { height -> ThemePreview(theme, themeStyle, board, siteBoard, height, scaleUp = 1f) }
            }
            zoomBoard?.let { style ->
                BoardZoomDialog(
                    label = stringResource(boardStyleLabel(style)),
                    onUse = { pickBoard(style); zoomBoard = null },
                    onClose = { zoomBoard = null },
                ) { height ->
                    PreviewBoard(board, BoardLook.of(style, siteBoard, pageScheme = null, decoLight = decoLightBoard()), CubeLook.of(style), height, scaleUp = 1f)
                }
            }
            zoomHand?.let { left ->
                BoardZoomDialog(
                    label = stringResource(if (left) R.string.settings_left_hand else R.string.settings_right_hand),
                    onUse = { onLeftHandedChange(left); zoomHand = null },
                    onClose = { zoomHand = null },
                ) { height ->
                    PreviewBoardWithPanel(
                        board,
                        handLook(left),
                        CubeLook.of(boardStyle),
                        panelLookFor(boardStyle),
                        height = height,
                        panelOnRight = left,
                        scaleUp = 1f,
                    )
                }
            }
        }
        DeviceGroup(title = stringResource(R.string.settings_theme), striped = true) {
            OptionCards {
                listOf(
                    AppTheme.PLAIN to R.string.settings_theme_plain,
                    AppTheme.DECO to decoThemeLabel(),
                ).forEach { (theme, label) ->
                    val themeStyle = themeBoardStyle(theme, boardStyle)
                    OptionCard(
                        label = stringResource(label),
                        selected = appTheme == theme,
                        onSelect = { onAppThemeChange(theme) },
                        width = WIDE_CARD,
                        onZoom = { zoomTheme = theme },
                    ) {
                        if (board != null) {
                            ThemePreview(theme, themeStyle, board, siteBoard, BOARD_PREVIEW_HEIGHT - 16.dp)
                        }
                    }
                }
            }
        }

        // Neljä taittuvaa ryhmää Themen alla (Tommin valinta 27.9.2026): laudan ulkoasu,
        // pelaaminen, listojen ulkoasu ja laite. Ennen tätä viisitoista ryhmää olivat yhtenä
        // luettelona, jossa kätisyys oli kaukana laudasta ja lukko listojen värien perässä.
        // Raidoitus alkaa kunkin ryhmän sisällä alusta.
        DeviceFold(DeviceSettingsGroup.BOARD, R.string.settings_fold_board, folds, onToggleFold) {
            DeviceGroup(title = stringResource(R.string.settings_board_style), striped = true) {
                OptionCards {
                    BoardStyle.entries.forEach { style ->
                        OptionCard(
                            label = stringResource(boardStyleLabel(style)),
                            selected = if (boardShuffle) style in shufflePool else boardStyle == style,
                            onSelect = { pickBoard(style) },
                            width = WIDE_CARD,
                            onZoom = { zoomBoard = style },
                            checkbox = boardShuffle,
                        ) {
                            if (board != null) {
                                PreviewBoard(
                                    board,
                                    BoardLook.of(style, siteBoard, pageScheme = null, decoLight = decoLightBoard()),
                                    CubeLook.of(style),
                                    height = BOARD_PREVIEW_HEIGHT,
                                )
                            }
                        }
                    }
                }
                // Kytkin korttien alla eikä viidentenä korttina (Tommin valinta 27.9.2026): arvonta ei
                // ole lauta, ja kytkin säilyttää valitun laudan kun se sammuu.
                DeviceToggle(
                    label = stringResource(R.string.settings_board_shuffle_toggle),
                    explain = stringResource(R.string.settings_board_shuffle_explain),
                    checked = boardShuffle,
                    onChange = onBoardShuffleChange,
                )
            }
            // Nopat ja pisteet ovat omia kytkimiään eivätkä lautatilan mukana (Tommin päätös
            // 27.8.2026): X-22-laudalla on voitava pitää sivuston noppaesitys. Vaihtoehtojen
            // sanat kuvaavat mitä ruudulla näkyy, eivät kytkimen teknistä nimeä. Esikatselu
            // piirtyy valitulla laudalla, koska se on se lauta jolla valinta näkyy.
            DeviceGroup(title = stringResource(R.string.settings_dice_style), striped = false) {
                OptionCards {
                    listOf(
                        DiceStyle.COUNTER to R.string.settings_dice_style_counter,
                        DiceStyle.SITE to R.string.settings_dice_style_site,
                    ).forEach { (style, label) ->
                        OptionCard(
                            label = stringResource(label),
                            selected = diceStyle == style,
                            onSelect = { onDiceStyleChange(style) },
                            width = NARROW_CARD,
                        ) {
                            situation?.board(siteDice = style == DiceStyle.SITE)?.let {
                                PreviewDice(it, look, size = 32.dp)
                            }
                        }
                    }
                }
            }
            DeviceGroup(title = stringResource(R.string.settings_score_style), striped = true) {
                OptionCards {
                    listOf(
                        ScoreStyle.AWAY to R.string.settings_score_style_away,
                        ScoreStyle.SITE to R.string.settings_score_style_site,
                    ).forEach { (style, label) ->
                        OptionCard(
                            label = stringResource(label),
                            selected = scoreStyle == style,
                            onSelect = { onScoreStyleChange(style) },
                            width = NARROW_CARD,
                        ) {
                            if (board != null) {
                                PreviewScore(board, look, panelLookFor(boardStyle), awayShown = style == ScoreStyle.AWAY)
                            }
                        }
                    }
                }
            }
            // Kätisyys on ensimmäinen laitekytkin joka kuvaa pelaajaa eikä sovellusta (Tommin
            // päätös 9.9.2026). Selite sanoo syyn eikä vain seurausta: paneeli menee toisen
            // käden ulottuville.
            DeviceGroup(title = stringResource(R.string.settings_handedness), striped = false) {
                // Kaksi korttia esikatseluineen 24.9.2026 alkaen (Tommin valinta): valintaruutu ei
                // näyttänyt mitä kytkin tekee, ja kortti näyttää paneelin puolen. Arvo ja
                // tallennus ovat samat kuin ruudulla, vain esitys vaihtui.
                OptionCards {
                    listOf(false to R.string.settings_right_hand, true to R.string.settings_left_hand).forEach { (left, label) ->
                        OptionCard(
                            label = stringResource(label),
                            selected = leftHanded == left,
                            onSelect = { onLeftHandedChange(left) },
                            width = WIDE_CARD,
                            onZoom = { zoomHand = left },
                        ) {
                            if (board != null) {
                                PreviewBoardWithPanel(
                                    board,
                                    handLook(left),
                                    CubeLook.of(boardStyle),
                                    panelLookFor(boardStyle),
                                    height = BOARD_PREVIEW_HEIGHT,
                                    panelOnRight = left,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.settings_left_handed_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
                )
            }
        }

        DeviceFold(DeviceSettingsGroup.PLAYING, R.string.settings_fold_playing, folds, onToggleFold) {
            // Pakolliset askeleet ovat kytkin eikä valinta vaihtoehtojen välillä, joten rivi on
            // valintaruutu eikä radiopari. Selite sanoo mitä lauta tekee ja mitä se ei tee
            // (lähetys jää pelaajalle), koska tämä on ensimmäinen laiteasetus joka koskee
            // tekoa eikä esitystä (docs/AVOIMET.md, pakkosiirrot).
            DeviceGroup(title = stringResource(R.string.settings_forced_steps), striped = true) {
                DeviceToggle(
                    label = stringResource(R.string.settings_forced_steps_toggle),
                    explain = stringResource(R.string.settings_forced_steps_explain),
                    checked = playForcedSteps,
                    onChange = onPlayForcedStepsChange,
                )

                // Ahne uloskanto on toinen teko-kytkin samassa osiossa (Tommin tilaus 2.9.2026).
                // Selite sanoo ehdon (ei kontaktia), säännön (eniten ulos) ja sen mikä jää pelaajalle
                // silloin kun ahneita vaihtoehtoja on useampi.
                DeviceToggle(
                    label = stringResource(R.string.settings_greedy_bearoff_toggle),
                    explain = stringResource(R.string.settings_greedy_bearoff_explain),
                    checked = playGreedyBearoff,
                    onChange = onPlayGreedyBearoffChange,
                )
            }
            // **Siirtonuolet** (Tommin idea ja kuittaus 29.9.2026, `MoveArrowsStore`). Oma otsikkonsa,
            // koska kytkin ei tee mitään pelaajan puolesta vaan näyttää sen mitä on koottu. Se
            // kuuluu silti tähän ryhmään: hyöty on suurin juuri yllä olevien kytkinten kanssa,
            // kun sovellus on asettanut nappulat itse.
            DeviceGroup(title = stringResource(R.string.settings_move_arrows), striped = false) {
                DeviceToggle(
                    label = stringResource(R.string.settings_move_arrows_toggle),
                    explain = stringResource(R.string.settings_move_arrows_explain),
                    checked = moveArrows,
                    onChange = onMoveArrowsChange,
                )
                // Vastustajan siirto omana rivinään (kuittaus 29.9.2026 illalla), koska se maksaa
                // lisähaun ja oma nuoli ei. Selite sanoo sen, jotta hinta on näkyvissä valinnan
                // kohdalla eikä vain ohjeessa.
                DeviceToggle(
                    label = stringResource(R.string.settings_opponent_arrows_toggle),
                    explain = stringResource(R.string.settings_opponent_arrows_explain),
                    checked = opponentArrows,
                    onChange = onOpponentArrowsChange,
                )
            }
            // **Noppien painallus tekona** (Tommin tilaus 4.9.2026). Oma otsikkonsa eikä
            // pakkosiirtojen jatkoa, koska nämä eivät kokoa siirtoa pelaajan puolesta vaan
            // muuttavat sen mitä laudalla oleva painallus tarkoittaa. Kaksi riviä eikä yksi,
            // koska teot ovat eri painoisia: vaihdon voi perua painamalla uudestaan, lähetys
            // päättää vuoron. Selitteet sanovat ehdon noppien värinä, koska se on se mitä
            // pelaaja näkee, ja lähetyksen selite nimeää sen tapauksen jossa väri ja ehto
            // eroavat (vain toinen noppa pelattavissa).
            DeviceGroup(title = stringResource(R.string.settings_dice_tap), striped = true) {
                DeviceToggle(
                    label = stringResource(R.string.settings_dice_submit_toggle),
                    explain = stringResource(R.string.settings_dice_submit_explain),
                    checked = diceSubmitTap,
                    onChange = onDiceSubmitTapChange,
                )
                DeviceToggle(
                    label = stringResource(R.string.settings_dice_swap_toggle),
                    explain = stringResource(R.string.settings_dice_swap_explain),
                    checked = diceSwapTap,
                    onChange = onDiceSwapTapChange,
                )
            }
            // **Beaverin vahvistus** (Tommin tilaus 27.9.2026, `BeaverConfirmStore`). Yksi kytkin:
            // Acceptin ja Declinen vahvistukset hoitaa sivun oma asetus (Tommin karsinta samana
            // aamuna), ja beaverille sivulla ei ole asetusta. Selite nimeää sivun asetuksen josta
            // oletus tulee, koska muuten kytkin näyttäisi päällä olevalta ilman valintaa.
            DeviceGroup(title = stringResource(R.string.settings_money_game), striped = false) {
                DeviceToggle(
                    label = stringResource(R.string.settings_confirm_beaver_toggle),
                    explain = stringResource(R.string.settings_confirm_beaver_explain),
                    checked = confirmBeaver,
                    onChange = onConfirmBeaverChange,
                )
            }
        }

        DeviceFold(DeviceSettingsGroup.LISTS, R.string.settings_fold_lists, folds, onToggleFold) {
            // Taustakuvio on kytkin oletuksena pois molemmissa teemoissa (Tommin päätös 6.9.2026
            // illalla), ja sumi-e on sen alakytkin vaalealle teemalle (saman päivän aiempi päätös:
            // yksi värillinen kandidaatti asetukseksi, oletus pois). Vaikutus näkyy heti tämän
            // ruudun taustassa, joten selitteet kertovat vain sen mitä ei näe tästä: jokainen
            // ruutu arpoo oman taivaansa, ja tummassa myös paletin ja hahmon.
            DeviceGroup(title = stringResource(R.string.settings_background), striped = true) {
                DeviceToggle(
                    label = stringResource(R.string.settings_pattern_toggle),
                    explain = stringResource(R.string.settings_pattern_explain),
                    checked = pattern,
                    onChange = onPatternChange,
                )
                DeviceToggle(
                    label = stringResource(R.string.settings_sumi_e_toggle),
                    explain = stringResource(R.string.settings_sumi_e_explain),
                    checked = sumiE,
                    onChange = onSumiEChange,
                )
            }
            // Taustakuvat tyhjään tilaan (Tommin päätökset 15.9.2026, `DgWallpaper.kt`). Kansio
            // on itse kytkin: valittu kansio näyttää kuvat, `Stop showing` unohtaa sen. Samaa
            // muotoa kuin varmuuskopion rivi viestiruudussa: tilarivi ja napit, ei checkboxia,
            // koska valinta vaatii järjestelmän kansiovalitsimen eikä ole päällä/pois.
            DeviceGroup(title = stringResource(R.string.settings_wallpaper), striped = false) {
                Text(
                    text = if (wallpaperFolder == null) {
                        stringResource(R.string.settings_wallpaper_off)
                    } else {
                        stringResource(R.string.settings_wallpaper_on, dgWallpaperFolderName(wallpaperFolder))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Text(
                    text = stringResource(R.string.settings_wallpaper_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onChooseWallpaperFolder) {
                        Text(
                            stringResource(
                                if (wallpaperFolder == null) R.string.settings_wallpaper_choose
                                else R.string.settings_wallpaper_change,
                            ),
                        )
                    }
                    if (wallpaperFolder != null) {
                        TextButton(onClick = onClearWallpaperFolder, modifier = Modifier.padding(start = 8.dp)) {
                            Text(stringResource(R.string.settings_wallpaper_clear))
                        }
                    }
                }
            }
            // Rarity-värit on kytkin eikä valinta vaihtoehtojen välillä, joten rivi on
            // valintaruutu (Tommin tilaus 26.9.2026, oletus päällä, ks. `RarityStore`).
            DeviceGroup(title = stringResource(R.string.settings_rarity), striped = true) {
                DeviceToggle(
                    label = stringResource(R.string.settings_rarity_toggle),
                    explain = stringResource(R.string.settings_rarity_explain),
                    checked = rarity,
                    onChange = onRarityChange,
                )
            }
            // Odotuksen ilmaisin (Tommin tilaus 18.9.2026). Viisi muotoa ja kaikki laudan väreissä;
            // vaihtoehtojen sanat kuvaavat kuvaa eivätkä koodin nimeä. Esikatselu pyörii
            // keskikaistan huovalla, koska ilmaisin korvaa siellä napit.
            DeviceGroup(title = stringResource(R.string.settings_busy_style), striped = false) {
                OptionCards {
                    listOf(
                        BusyStyle.ARC to R.string.settings_busy_style_arc,
                        BusyStyle.OUROBOROS to R.string.settings_busy_style_ouroboros,
                        BusyStyle.CUBE to R.string.settings_busy_style_cube,
                        BusyStyle.INFINITY to R.string.settings_busy_style_infinity,
                        BusyStyle.HOURGLASS to R.string.settings_busy_style_hourglass,
                        BusyStyle.RANDOM to R.string.settings_busy_style_random,
                    ).forEach { (style, label) ->
                        OptionCard(
                            label = stringResource(label),
                            selected = busyStyle == style,
                            onSelect = { onBusyStyleChange(style) },
                            width = NARROW_CARD,
                        ) {
                            if (board != null) PreviewBusyOnBand(style, board, look)
                        }
                    }
                }
                // Viimeistely on kytkin eikä kuudes muoto: se koskee kaikkia viittä (Tommin kuittaus
                // 22.9.2026). Kuudes rivi on satunnainen, joka arpoo muodon eikä ole itse muoto.
                DeviceToggle(
                    label = stringResource(R.string.settings_busy_deco_toggle),
                    explain = stringResource(R.string.settings_busy_deco_explain),
                    checked = busyDeco,
                    onChange = onBusyDecoChange,
                )
            }
        }

        DeviceFold(DeviceSettingsGroup.DEVICE, R.string.settings_fold_device, folds, onToggleFold) {
            // Pystylukko on esitys eikä teko, mutta se on kytkin eikä valinta vaihtoehtojen
            // välillä, joten rivi on valintaruutu. Laudan suunta on toinen rivi samassa
            // ryhmässä (Tommin päätös 23.9.2026), koska lukko ei koske lautaa.
            DeviceGroup(title = stringResource(R.string.settings_orientation), striped = true) {
                DeviceToggle(
                    label = stringResource(R.string.settings_portrait_lock_toggle),
                    explain = stringResource(R.string.settings_portrait_lock_explain),
                    checked = portraitLock,
                    onChange = onPortraitLockChange,
                )
                DeviceToggle(
                    label = stringResource(R.string.settings_board_rotates_toggle),
                    explain = stringResource(R.string.settings_board_rotates_explain),
                    checked = boardRotates,
                    onChange = onBoardRotatesChange,
                )
            }
            // Koko näyttö on oma ryhmänsä eikä suunnan kolmas rivi, koska se ei käännä mitään
            // (Tommin tilaus 30.9.2026, oletus pois, ks. `FullScreenStore`).
            DeviceGroup(title = stringResource(R.string.settings_full_screen), striped = false) {
                DeviceToggle(
                    label = stringResource(R.string.settings_full_screen_toggle),
                    explain = stringResource(R.string.settings_full_screen_explain),
                    checked = fullScreen,
                    onChange = onFullScreenChange,
                )
            }
            // Sovelluslukko on kytkin (Tommin tilaus 27.9.2026, oletus pois, ks. `AppLockStore`).
            // Ilman laitteen omaa suojausta lukkoa ei voisi avata, joten kytkin on harmaa ja
            // selite sanoo miksi eikä vain että.
            DeviceGroup(title = stringResource(R.string.settings_app_lock), striped = true) {
                DeviceToggle(
                    label = stringResource(R.string.settings_app_lock_toggle),
                    explain = stringResource(
                        if (appLockAvailable) R.string.settings_app_lock_explain
                        else R.string.settings_app_lock_unavailable,
                    ),
                    checked = appLock && appLockAvailable,
                    onChange = onAppLockChange,
                    enabled = appLockAvailable,
                )
            }
        }
    }
}

/**
 * Laiteasetusten taittuva ryhmä (Tommin valinta 27.9.2026): otsikko on sama kytkin kuin
 * Loungen jaksoissa ([FoldHeading]), ja sisältö piirtyy vain auki ollessa.
 */
@Composable
private fun DeviceFold(
    group: DeviceSettingsGroup,
    @StringRes title: Int,
    folds: Set<DeviceSettingsGroup>,
    onToggle: (DeviceSettingsGroup) -> Unit,
    content: @Composable () -> Unit,
) {
    val folded = group in folds
    FoldHeading(
        text = stringResource(title),
        folded = folded,
        onToggle = { onToggle(group) },
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    if (!folded) content()
}

/**
 * Otsikoitu ryhmä laiteosiossa. Vuororaidoitus on ryhmien välillä eikä rivien
 * (Tommin tarkennus 17.9.2026: *"ryhmien raidoitus vuorottelee, mutta ryhmän sisällä ei
 * raidoitusta"*): raita kattaa otsikon ja kaikki sen rivit, ja joka toinen ryhmä on raidalla.
 */
@Composable
private fun DeviceGroup(title: String, striped: Boolean, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().dgStripe(striped).padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        content()
    }
}

/** Laiteosion kytkin selitteineen yhtenä rivinä. Selite kuuluu kytkimeen eikä ole oma kohtansa. */
@Composable
private fun DeviceToggle(
    label: String,
    explain: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox, enabled = enabled)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Text(
            text = explain,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
        )
    }
}

/**
 * Teeman lauta: Plain palauttaa aiemman laudan, mutta Decolta palatessa sitä ei tunneta
 * täällä, joten kuva näyttää X-22:n, joka on sovelluksen oma oletuslauta.
 */
private fun themeBoardStyle(theme: AppTheme, boardStyle: BoardStyle): BoardStyle = when {
    theme == AppTheme.DECO -> BoardStyle.DECO
    boardStyle == BoardStyle.DECO -> BoardStyle.X22
    else -> boardStyle
}

/** Decon kortin nimi laitteen tilan mukaan, koska kortin kuva näyttää sen tilan Decon (26.9.2026). */
@Composable
private fun decoThemeLabel(): Int =
    if (dgDark()) R.string.settings_theme_deco else R.string.settings_theme_deco_light

private fun boardStyleLabel(style: BoardStyle): Int = when (style) {
    BoardStyle.SITE -> R.string.settings_board_style_site
    BoardStyle.X22 -> R.string.settings_board_style_x22
    BoardStyle.MONTE_CARLO_VARIANT -> R.string.settings_board_style_monte_carlo_variant
    BoardStyle.DECO -> R.string.settings_board_style_deco
    BoardStyle.MAPLE -> R.string.settings_board_style_maple
    BoardStyle.WALNUT -> R.string.settings_board_style_walnut
    BoardStyle.OAK_LEATHER -> R.string.settings_board_style_oak_leather
    BoardStyle.OLIVE -> R.string.settings_board_style_olive
}

/** Teeman esikatselu: lauta teeman omalla pinnalla, jotta taustan ero näkyy. */
@Composable
private fun ThemePreview(
    theme: AppTheme,
    themeStyle: BoardStyle,
    board: BoardState,
    siteBoard: SiteBoardSettings,
    height: Dp,
    scaleUp: Float = 2f,
) {
    DgTheme(theme) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
        ) {
            PreviewBoard(
                board,
                BoardLook.of(themeStyle, siteBoard, pageScheme = null, decoLight = decoLightBoard()),
                CubeLook.of(themeStyle),
                height = height,
                scaleUp = scaleUp,
            )
        }
    }
}

/** Laudan esikatselukortti: neljä mahtuu tabletin vaakariville, puhelimella rivittyy. */
private val WIDE_CARD = 290.dp
private val BOARD_PREVIEW_HEIGHT = 150.dp

/** Nopat, pisteet ja ilmaisin: pieni kuva, pidempi nimi. */
private val NARROW_CARD = 200.dp

/**
 * Muokkauspuskurin tallentimet. `Set` ja `Map` eivät kelpaa järjestelmän tilapakettiin
 * sellaisinaan, joten ne litistetään merkkijonolistaksi ja kootaan takaisin. Radiot
 * kulkevat pareina nimi, arvo; sivun omissa nimissä ja arvoissa ei ole rivinvaihtoja
 * eikä muuta erotinta tarvita, koska parillisuus kantaa rakenteen.
 */
private val checkedSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private val radioSaver = listSaver<Map<String, String>, String>(
    save = { it.flatMap { (name, value) -> listOf(name, value) } },
    restore = { flat -> flat.chunked(2).associate { (name, value) -> name to value } },
)

@Composable
private fun SettingsForm(
    page: SettingsPage,
    refreshing: Boolean,
    storedBaseline: SettingsSnapshot?,
    onSave: (Set<String>, Map<String, String>) -> Unit,
    /** Vierivän osan alkuun, ennen omistajaa ([SiteOnlyNote]). */
    header: @Composable () -> Unit = {},
) {
    // Prosessikuoleman yli palautettu puskuri kohtaa uudelleen haetun sivun ilman
    // avainvertailua: jos sivu ehti muuttua sivustolla juuri siinä ikkunassa, vanhat raksit
    // näkyvät uuden sivun päällä. Raja on hyväksytty ja kirjattu eikä piilotettu, ja
    // session sisällä avain (page) nollaa puskurin kuten ennenkin.
    var checked by rememberSaveable(page, stateSaver = checkedSaver) { mutableStateOf(page.checked) }
    var radios by rememberSaveable(page, stateSaver = radioSaver) { mutableStateOf(page.radios) }

    val changed = countChanges(page, checked, radios)

    // Esikatselu luetaan muokkauspuskurista eikä luetusta sivusta: tallentamaton skeema,
    // peilaus ja pip-luku näkyvät kuvassa ennen Update Preferencesia. Lauta on sivuston
    // kahdella nopalla, koska sivu näyttää heiton niin.
    val previewBoard = rememberPreviewSituation()?.board(siteDice = true)
    val colorChoice = page.choices.firstOrNull { choice -> choice.options.any { it.swatch != null } }

    val hasFields = page.toggles.isNotEmpty() || page.choices.isNotEmpty()

    // **Tallennusalue on aina näkyvissä** (Tommin tilaus 26.9.2026: *"koko ajan näkyvissä,
    // jotta pelaaja muistaa tallentaa muutokset"*). Se oli lomakkeen lopussa Mini-kortin alla,
    // ja muutos jäi lähettämättä kun sykkivä nappi oli vierityksen takana. Nyt vain kentät
    // vierivät, ja laskuri ja `Update Preferences` ovat ruudun alareunassa.
    Column(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .dgReadingSurface(),
    ) {
        header()
        ProgressSlot(active = refreshing)

        page.player.name?.let { name ->
            Text(
                text = stringResource(R.string.settings_owner, name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Ennen asetuksia eikä niiden jälkeen, sama paikka kuin lukutilan ilmoituksella oli:
        // lukija saa tietää miten lähetys käyttäytyy ennen kuin hän koskee ensimmäiseen
        // ruutuun, eikä vasta kun hän etsii nappia.
        Text(
            text = stringResource(R.string.settings_whole_form),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
        )

        HorizontalDivider()

        page.toggles.forEach { toggle ->
            ToggleRow(
                toggle = toggle,
                checked = toggle.name in checked,
                onToggle = { on ->
                    checked = if (on) checked + toggle.name else checked - toggle.name
                },
            )
        }

        // Kytkinlista on ryhmä nolla ilman raitaa, ja radioryhmät vuorottelevat sen perään
        // (Tommin tarkennus 17.9.2026, ks. `DeviceGroup`).
        page.choices.forEachIndexed { index, choice ->
            HorizontalDivider()
            val onSelect = { value: String -> radios = radios + (choice.name to value) }
            when {
                // Taustaväri on lista eikä kortteja (Tommin päätös 25.9.2026): sovellus ei käytä
                // sitä, joten kuva lupasi jotain mitä sovellus ei tee. Kenttä pysyy silti
                // lomakkeessa, koska puuttuva radioryhmä nollaisi sen sivustolla.
                choice == colorChoice -> ChoiceGroup(
                    choice = choice,
                    striped = index % 2 == 0,
                    selected = radios[choice.name],
                    onSelect = onSelect,
                    note = stringResource(R.string.settings_background_browser_only),
                )
                // Lautaskeema kortteina (Tommin tilaus 23.9.2026).
                previewBoard != null && choice.name == BOARD_SCHEME_GROUP -> {
                    val drawable = { option: ChoiceOption ->
                        page.withBuffer(checked, radios + (choice.name to option.value))
                            .siteBoardSettings().scheme != BoardScheme.MINI
                    }
                    SiteChoiceCards(
                        choice, striped = index % 2 == 0, selected = radios[choice.name], onSelect = onSelect,
                        zoomable = drawable,
                        enabled = drawable,
                    ) { option, height, scaleUp ->
                        // Skeeman kuvaus luetaan samasta funktiosta kuin tallennettu, jotta arvon
                        // ja skeeman vastaavuus on yhdessä paikassa.
                        val site = page.withBuffer(checked, radios + (choice.name to option.value))
                            .siteBoardSettings()
                        SitePreview(previewBoard, site, height, scaleUp)
                    }
                }
                else -> ChoiceGroup(
                    choice = choice,
                    striped = index % 2 == 0,
                    selected = radios[choice.name],
                    onSelect = onSelect,
                )
            }
        }

        if (!hasFields) {
            Text(
                text = stringResource(R.string.settings_empty),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
        if (hasFields) Column(modifier = Modifier.fillMaxWidth().dgReadingSurface()) {
            HorizontalDivider()
            SubmitArea(
                changed = changed,
                hasBaseline = storedBaseline != null,
                onRestore = {
                    // Palautus täyttää lomakkeen eikä lähetä sitä. Se on tietoinen ero:
                    // lähettävä palautus olisi kirjoitus jota käyttäjä ei nähnyt ennen kuin
                    // se tapahtui, ja tämä ruutu on olemassa juuri siksi ettei sellaista
                    // tehdä. Painallus siis näyttää lähtötilan, ja käyttäjä lähettää sen.
                    storedBaseline?.let { snapshot ->
                        checked = snapshot.checked
                        radios = snapshot.radios
                    }
                },
                onSave = { onSave(checked, radios) },
            )
        }
    }
}

/**
 * Montako asetusta on muuttumassa suhteessa siihen mitä sivustolla luettiin.
 *
 * Luku on käyttäjälle eikä lähetykselle: lähetys koskee joka tapauksessa koko lomaketta.
 * Juuri siksi se on tarpeen, ja se on eri luku kuin *montako kenttää lähtee*.
 */
private fun countChanges(
    page: SettingsPage,
    checked: Set<String>,
    radios: Map<String, String>,
): Int {
    val toggles = (checked - page.checked).size + (page.checked - checked).size
    val choices = radios.count { (name, value) -> page.radios[name] != value }
    return toggles + choices
}

@Composable
private fun SubmitArea(
    changed: Int,
    hasBaseline: Boolean,
    onRestore: () -> Unit,
    onSave: () -> Unit,
) {
    // Kiinteä alue ruudun alareunassa (26.9.2026), joten pystypehmuste on puolet entisestä.
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = if (changed == 0) {
                stringResource(R.string.settings_changes_none)
            } else {
                stringResource(R.string.settings_changes_count, changed)
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Napin teksti on sivuston oma (`Update Preferences`) eikä käännös, samasta
            // syystä kuin selitteet: käyttäjä menee sivustolle etsimään samaa asiaa samalla
            // nimellä. Nappi on estetty kun muutoksia ei ole, koska muuttumattoman lomakkeen
            // lähettäminen olisi kirjoitus ilman syytä, ja jokainen kirjoitus tälle
            // lomakkeelle on riski jota ei tarvitse ottaa.
            //
            // Nappi sykkii kun lomake poikkeaa luetusta sivusta (Tommin tilaus 22.9.2026):
            // muutos on olemassa vasta kun se lähetetään, ja syke muistuttaa siitä myös
            // segmentin vaihdon jälkeen. Vertailukohta on sama kuin laskurilla, eli sivu
            // sellaisena kuin se luettiin; takaisin raksittu kenttä ei ole muutos.
            //
            // Syke on Cube reminderin muoto 24.9.2026 alkaen (Tommin valinta *"purppura
            // kehys jää"*): kuusi pulssia purppurana kehyksenä, ja kehys jää ohuena kunnes
            // lomake lähetetään tai palautetaan. Jatkuva alfasyke piirsi ruutua niin kauan
            // kuin muutos odotti, ks. [reminderAttention].
            val ring = reminderAttention(changed > 0).ring
            Button(
                onClick = onSave,
                enabled = changed > 0,
                border = if (changed > 0) {
                    BorderStroke(1.dp * (1f + 2f * ring), DgBoard.Palette.CubeLaser)
                } else {
                    null
                },
            ) {
                Text(stringResource(R.string.settings_submit))
            }

            if (hasBaseline) {
                TextButton(onClick = onRestore) {
                    Text(stringResource(R.string.settings_restore))
                }
            }
        }

    }
}

/**
 * Yksi valintaruutu, ja kahdella niistä on selite jota muilla ei ole.
 *
 * Kentät `4` ja `5` (*Skip Opponent's "Roll Dice" pages*, *Skip all automatic pages*)
 * muuttavat `/bg/nextgamen` käyttäytymistä, eli sitä kuluttavaa reittiä joka on sovelluksen
 * ykkösominaisuuden ehto. Se sanotaan niiden vieressä samalla tavalla kuin
 * `messages_queue_explain` sanoo sen avausnapin vieressä, koska manuaali ei ole näkyvissä
 * sillä hetkellä kun ruutua raksitetaan (`docs/ASETUKSET.md` kohta C).
 *
 * **Tunnistus tehdään selitteestä eikä numerosta.** Kenttien nimet ovat paljaita numeroita
 * joilla ei ole merkitystä itsessään, ja numeroon kovakoodattu selite osuisi renumeroinnin
 * jälkeen hiljaa väärään ruutuun. Sama varaus kuin `SettingsPage.nameFor`issa: jos sivuston
 * teksti muuttuu, selite jää pois eikä siirry väärään paikkaan.
 */
@Composable
private fun ToggleRow(
    toggle: PreferenceToggle,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onToggle, role = Role.Checkbox)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Kytkin on rivin sisällä eikä oma kohteensa: koko rivi on kosketusalue, joten
            // pieni ruutu ei ole ainoa osuma. Tämä on saavutettavuutta eikä koristetta.
            Checkbox(checked = checked, onCheckedChange = null)
            Text(
                text = toggle.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        if (affectsQueue(toggle.label)) {
            Text(
                text = stringResource(R.string.settings_queue_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

/** Vertailu on sivuston oma englanti, ks. [ToggleRow]in perustelu tunnistustavasta. */
private fun affectsQueue(label: String): Boolean =
    label.equals("Skip Opponent's \"Roll Dice\" pages", ignoreCase = true) ||
        label.equals("Skip all automatic pages", ignoreCase = true)

/**
 * Sivuston radioryhmä kortteina esikatseluineen, sama korttimuoto kuin laiteosiossa. Otsikko
 * ja selitteet ovat sivun omia kuten [ChoiceGroup]issa. Suurennuksen `Use this` asettaa
 * vain muokkauspuskurin: mitään ei lähde ennen Update Preferencesia.
 */
@Composable
private fun SiteChoiceCards(
    choice: PreferenceChoice,
    striped: Boolean,
    selected: String?,
    onSelect: (String) -> Unit,
    /** Saako kortti suurennuslasin: Mini ei piirrä lautaa, ja tekstin suurennus on turha. */
    zoomable: (ChoiceOption) -> Boolean,
    /** Voiko vaihtoehdon valita: Miniä ei voi, koska sovellus ei näytä sitä (25.9.2026). */
    enabled: (ChoiceOption) -> Boolean = { true },
    preview: @Composable (option: ChoiceOption, height: Dp, scaleUp: Float) -> Unit,
) {
    var zoom by remember { mutableStateOf<ChoiceOption?>(null) }
    Column(modifier = Modifier.fillMaxWidth().dgStripe(striped).padding(vertical = 8.dp)) {
        Text(
            text = choice.label ?: choice.name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OptionCards {
            choice.options.forEach { option ->
                OptionCard(
                    label = option.label,
                    selected = option.value == selected,
                    onSelect = { onSelect(option.value) },
                    width = WIDE_CARD,
                    onZoom = if (zoomable(option)) ({ zoom = option }) else null,
                    enabled = enabled(option),
                ) {
                    preview(option, BOARD_PREVIEW_HEIGHT, 2f)
                }
            }
        }
    }
    zoom?.let { option ->
        BoardZoomDialog(
            label = option.label,
            onUse = { onSelect(option.value); zoom = null },
            onClose = { zoom = null },
        ) { height -> preview(option, height, 1f) }
    }
}

/**
 * Sivuston skeemalla piirretty lauta sovelluksen omalla pinnalla. Sivun taustaväriä ei
 * käytetä (Tommin päätös 25.9.2026), koska sovellus ei käytä sitä laudan ympärillä.
 */
@Composable
private fun SitePreview(board: BoardState, site: SiteBoardSettings, height: Dp, scaleUp: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        // **Mini ei piirrä lautaa lainkaan** (laiteajo 23.9.2026): sivu jättää skeemassa
        // pisteiden numerot pois, ja lautaruutu näyttää silloin tämän tekstin. Ensimmäinen
        // versio piirsi tähän X-22-laudan, joka lupasi jotain mitä sovellus ei tee.
        if (site.scheme == BoardScheme.MINI) {
            Text(
                // Oma lyhyt teksti eikä laudan viesti (Tommin päätös 25.9.2026): laudan kehotus
                // vaihtaa skeema asetuksista on tässä ruudussa jo tehty tai tekemättä.
                text = stringResource(R.string.settings_mini_card),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.height(height - 16.dp),
            )
            return@Box
        }
        PreviewBoard(
            board,
            BoardLook.of(BoardStyle.SITE, site, pageScheme = null),
            CubeLook.DEFAULT,
            height = height - 16.dp,
            scaleUp = scaleUp,
        )
    }
}

/**
 * Yksi radioryhmä otsikkoineen.
 *
 * Otsikkona on sivun oma `<H4>`, ja sen puuttuessa lomakkeen kentän nimi. Kenttänimi on
 * huono otsikko (`board`), mutta se on sivun omaa tietoa: keksitty otsikko olisi käännös
 * jota käyttäjä ei löydä sivustolta silloin kun hän menee sinne muuttamaan asetusta.
 */
@Composable
private fun ChoiceGroup(
    choice: PreferenceChoice,
    striped: Boolean,
    selected: String?,
    onSelect: (String) -> Unit,
    /** Sovelluksen oma huomautus otsikon alla; sivun tekstit pysyvät sivun omina. */
    note: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().dgStripe(striped).padding(vertical = 8.dp)) {
        Text(
            text = choice.label ?: choice.name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
            )
        }
        choice.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option.value == selected,
                        onClick = { onSelect(option.value) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option.value == selected, onClick = null)
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

/**
 * Tallennuksen lopputulos dialogina, sama muoto kuin laudalla 24.8.2026.
 *
 * Haaroilla on eri teksti eikä yhteistä *jokin meni pieleen* -lausetta, koska niillä on eri
 * seuraus käyttäjälle. [SettingsSaveResult.Mismatch] on ainoa joka pyytää tarkistamaan
 * selaimella: siinä lähetys lähti mutta paluuluku ei täsmää, eli sivustolla voi olla tila
 * jota kukaan ei pyytänyt.
 */
@Composable
private fun SaveResultDialog(result: SettingsSaveResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
        },
        title = { Text(stringResource(titleFor(result))) },
        text = { Text(textFor(result)) },
    )
}

private fun titleFor(result: SettingsSaveResult): Int = when (result) {
    SettingsSaveResult.Saved -> R.string.settings_saved_title
    else -> R.string.settings_not_saved_title
}

@Composable
private fun textFor(result: SettingsSaveResult): String = when (result) {
    SettingsSaveResult.Saved -> stringResource(R.string.settings_saved)

    is SettingsSaveResult.NotSent -> when (val reason = result.reason) {
        Failure.Offline -> stringResource(R.string.settings_not_sent_offline)
        is Failure.Server -> stringResource(R.string.settings_not_sent_server, reason.code)
        Failure.Sleeping -> stringResource(R.string.settings_not_sent_sleeping)
    }

    SettingsSaveResult.Mismatch -> stringResource(R.string.settings_mismatch)
    SettingsSaveResult.WrongPage -> stringResource(R.string.settings_wrong_page)
    SettingsSaveResult.SessionExpired -> stringResource(R.string.board_session_expired)

    // Vartio pysäytti ennen verkkoa, eli sivustolla ei muuttunut mitään. Yleisin syy on
    // FormChanged, ja sillä on oma tekstinsä koska sillä on oma tekonsa: silloin ruutu on
    // päivitettävä eikä lähetystä yritettävä uudelleen. Muut kolme ovat sovelluksen omia
    // vikoja eivätkä käyttäjän korjattavissa, joten ne saavat saman rehellisen tekstin.
    is SettingsSaveResult.Blocked -> when (result.reason) {
        is SettingsUpdate.FormChanged -> stringResource(R.string.settings_blocked_form_changed)
        else -> stringResource(R.string.settings_blocked_internal)
    }
}

/** Sivu muokkauspuskurin arvoilla, jotta sivulta johdetut lukemat näkevät tallentamattomat valinnat. */
private fun SettingsPage.withBuffer(checked: Set<String>, radios: Map<String, String>): SettingsPage =
    copy(toggles = toggles.map { it.copy(checked = it.name in checked) }, radios = radios)
