package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.app.session.DiceStyle
import fi.tommi.dg.app.session.ScoreStyle
import fi.tommi.dg.app.session.SettingsSnapshot
import fi.tommi.dg.domain.PreferenceChoice
import fi.tommi.dg.domain.PreferenceToggle
import fi.tommi.dg.domain.SettingsPage
import fi.tommi.dg.domain.SettingsUpdate

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
    /** Noppien ja pisteiden esitys, omat kytkimensä (Tommi 27.8.2026). Ks. [DeviceSection]. */
    diceStyle: DiceStyle,
    onDiceStyleChange: (DiceStyle) -> Unit,
    scoreStyle: ScoreStyle,
    onScoreStyleChange: (ScoreStyle) -> Unit,
    /** Pakollisten askelten esipoiminta, oma kytkin (Tommi 2.9.2026). Ks. [DeviceSection]. */
    playForcedSteps: Boolean,
    onPlayForcedStepsChange: (Boolean) -> Unit,
    /** Ahne uloskanto ilman kontaktia, toinen kytkin samassa osiossa (Tommi 2.9.2026). */
    playGreedyBearoff: Boolean,
    onPlayGreedyBearoffChange: (Boolean) -> Unit,
    /** Noppien painallus tekona, kaksi kytkintä (Tommin tilaus 4.9.2026). Ks. `diceTapFor`. */
    diceSubmitTap: Boolean,
    onDiceSubmitTapChange: (Boolean) -> Unit,
    diceSwapTap: Boolean,
    onDiceSwapTapChange: (Boolean) -> Unit,
    /** Lukuruutujen pystylukko (Tommi 2.9.2026), lauta on vaakaan aina. Ks. [DeviceSection]. */
    portraitLock: Boolean,
    onPortraitLockChange: (Boolean) -> Unit,
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
    onRefresh: () -> Unit,
    onSave: (Set<String>, Map<String, String>) -> Unit,
    onDismissResult: () -> Unit,
    /** Paluu Info-välilehden listaan. Ks. `InfoScreen`. */
    onBack: () -> Unit,
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
                title = { Text(stringResource(R.string.settings_title)) },
                // Back-nappi tuli 26.8.2026 kun asetukset siirtyivät Info-välilehden
                // sisään: ruutu on nyt listarivin takana eikä oma välilehtensä.
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.board_back))
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text(stringResource(R.string.top_refresh))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .dgReadingSurface(),
        ) {
            DeviceSection(
                boardStyle = boardStyle,
                onBoardStyleChange = onBoardStyleChange,
                diceStyle = diceStyle,
                onDiceStyleChange = onDiceStyleChange,
                scoreStyle = scoreStyle,
                onScoreStyleChange = onScoreStyleChange,
                playForcedSteps = playForcedSteps,
                onPlayForcedStepsChange = onPlayForcedStepsChange,
                playGreedyBearoff = playGreedyBearoff,
                onPlayGreedyBearoffChange = onPlayGreedyBearoffChange,
                diceSubmitTap = diceSubmitTap,
                onDiceSubmitTapChange = onDiceSubmitTapChange,
                diceSwapTap = diceSwapTap,
                onDiceSwapTapChange = onDiceSwapTapChange,
                portraitLock = portraitLock,
                onPortraitLockChange = onPortraitLockChange,
                leftHanded = leftHanded,
                onLeftHandedChange = onLeftHandedChange,
                sumiE = sumiE,
                onSumiEChange = onSumiEChange,
                pattern = pattern,
                onPatternChange = onPatternChange,
                wallpaperFolder = wallpaperFolder,
                onChooseWallpaperFolder = onChooseWallpaperFolder,
                onClearWallpaperFolder = onClearWallpaperFolder,
            )
            HorizontalDivider()
            Box(modifier = Modifier.fillMaxWidth()) {
                when (state) {
                    SettingsUiState.Loading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
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

                    is SettingsUiState.Loaded -> SettingsForm(
                        page = state.page,
                        refreshing = state.refreshing,
                        storedBaseline = storedBaseline,
                        onSave = onSave,
                    )
                }
            }
        }
    }

    if (saveResult != null) {
        SaveResultDialog(result = saveResult, onDismiss = onDismissResult)
    }
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
    boardStyle: BoardStyle,
    onBoardStyleChange: (BoardStyle) -> Unit,
    diceStyle: DiceStyle,
    onDiceStyleChange: (DiceStyle) -> Unit,
    scoreStyle: ScoreStyle,
    onScoreStyleChange: (ScoreStyle) -> Unit,
    playForcedSteps: Boolean,
    onPlayForcedStepsChange: (Boolean) -> Unit,
    playGreedyBearoff: Boolean,
    onPlayGreedyBearoffChange: (Boolean) -> Unit,
    diceSubmitTap: Boolean,
    onDiceSubmitTapChange: (Boolean) -> Unit,
    diceSwapTap: Boolean,
    onDiceSwapTapChange: (Boolean) -> Unit,
    portraitLock: Boolean,
    onPortraitLockChange: (Boolean) -> Unit,
    leftHanded: Boolean,
    onLeftHandedChange: (Boolean) -> Unit,
    sumiE: Boolean,
    onSumiEChange: (Boolean) -> Unit,
    pattern: Boolean,
    onPatternChange: (Boolean) -> Unit,
    wallpaperFolder: String?,
    onChooseWallpaperFolder: () -> Unit,
    onClearWallpaperFolder: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.settings_device_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.settings_device_explain),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
        )
        DeviceGroup(title = stringResource(R.string.settings_board_style), striped = false) {
            StyleRow(
                label = stringResource(R.string.settings_board_style_site),
                selected = boardStyle == BoardStyle.SITE,
                onSelect = { onBoardStyleChange(BoardStyle.SITE) },
            )
            StyleRow(
                label = stringResource(R.string.settings_board_style_x22),
                selected = boardStyle == BoardStyle.X22,
                onSelect = { onBoardStyleChange(BoardStyle.X22) },
            )
            StyleRow(
                label = stringResource(R.string.settings_board_style_monte_carlo_variant),
                selected = boardStyle == BoardStyle.MONTE_CARLO_VARIANT,
                onSelect = { onBoardStyleChange(BoardStyle.MONTE_CARLO_VARIANT) },
            )

            // Nopat ja pisteet ovat omia kytkimiään eivätkä lautatilan mukana (Tommin päätös
            // 27.8.2026): X-22-laudalla on voitava pitää sivuston noppaesitys. Vaihtoehtojen
            // sanat kuvaavat mitä ruudulla näkyy, eivät kytkimen teknistä nimeä.
        }
        DeviceGroup(title = stringResource(R.string.settings_dice_style), striped = true) {
            StyleRow(
                label = stringResource(R.string.settings_dice_style_counter),
                selected = diceStyle == DiceStyle.COUNTER,
                onSelect = { onDiceStyleChange(DiceStyle.COUNTER) },
            )
            StyleRow(
                label = stringResource(R.string.settings_dice_style_site),
                selected = diceStyle == DiceStyle.SITE,
                onSelect = { onDiceStyleChange(DiceStyle.SITE) },
            )

        }
        DeviceGroup(title = stringResource(R.string.settings_score_style), striped = false) {
            StyleRow(
                label = stringResource(R.string.settings_score_style_away),
                selected = scoreStyle == ScoreStyle.AWAY,
                onSelect = { onScoreStyleChange(ScoreStyle.AWAY) },
            )
            StyleRow(
                label = stringResource(R.string.settings_score_style_site),
                selected = scoreStyle == ScoreStyle.SITE,
                onSelect = { onScoreStyleChange(ScoreStyle.SITE) },
            )

            // Pakolliset askeleet ovat kytkin eikä valinta vaihtoehtojen välillä, joten rivi on
            // valintaruutu eikä radiopari. Selite sanoo mitä lauta tekee ja mitä se ei tee
            // (lähetys jää pelaajalle), koska tämä on ensimmäinen laiteasetus joka koskee
            // tekoa eikä esitystä (docs/AVOIMET.md, pakkosiirrot).
        }
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

            // **Noppien painallus tekona** (Tommin tilaus 4.9.2026). Oma otsikkonsa eikä
            // pakkosiirtojen jatkoa, koska nämä eivät kokoa siirtoa pelaajan puolesta vaan
            // muuttavat sen mitä laudalla oleva painallus tarkoittaa. Kaksi riviä eikä yksi,
            // koska teot ovat eri painoisia: vaihdon voi perua painamalla uudestaan, lähetys
            // päättää vuoron. Selitteet sanovat ehdon noppien värinä, koska se on se mitä
            // pelaaja näkee, ja lähetyksen selite nimeää sen tapauksen jossa väri ja ehto
            // eroavat (vain toinen noppa pelattavissa).
        }
        DeviceGroup(title = stringResource(R.string.settings_dice_tap), striped = false) {
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

            // Pystylukko on esitys eikä teko, mutta se on kytkin eikä valinta vaihtoehtojen
            // välillä, joten rivi on valintaruutu. Selite sanoo sen mikä ei muutu: lauta on
            // vaakaan aina (Tommin päätös 2.9.2026, docs/UI.md).
        }
        DeviceGroup(title = stringResource(R.string.settings_orientation), striped = true) {
            DeviceToggle(
                label = stringResource(R.string.settings_portrait_lock_toggle),
                explain = stringResource(R.string.settings_portrait_lock_explain),
                checked = portraitLock,
                onChange = onPortraitLockChange,
            )

            // Kätisyys on ensimmäinen laitekytkin joka kuvaa pelaajaa eikä sovellusta (Tommin
            // päätös 9.9.2026). Rivi on valintaruutu eikä kahden vaihtoehdon valinta, koska
            // oletus on oikeakätinen ja kytkin poikkeaa siitä; sama muoto kuin pystylukolla.
            // Selite sanoo syyn eikä vain seurausta: paneeli menee toisen käden ulottuville.
        }
        DeviceGroup(title = stringResource(R.string.settings_handedness), striped = false) {
            DeviceToggle(
                label = stringResource(R.string.settings_left_handed_toggle),
                explain = stringResource(R.string.settings_left_handed_explain),
                checked = leftHanded,
                onChange = onLeftHandedChange,
            )

            // Taustakuvio on kytkin oletuksena pois molemmissa teemoissa (Tommin päätös 6.9.2026
            // illalla), ja sumi-e on sen alakytkin vaalealle teemalle (saman päivän aiempi päätös:
            // yksi värillinen kandidaatti asetukseksi, oletus pois). Vaikutus näkyy heti tämän
            // ruudun taustassa, joten selitteet kertovat vain sen mitä ei näe tästä: jokainen
            // ruutu arpoo oman taivaansa, ja tummassa myös paletin ja hahmon.
        }
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

            // Taustakuvat tyhjään tilaan (Tommin päätökset 15.9.2026, `DgWallpaper.kt`). Kansio
            // on itse kytkin: valittu kansio näyttää kuvat, `Stop showing` unohtaa sen. Samaa
            // muotoa kuin varmuuskopion rivi viestiruudussa: tilarivi ja napit, ei checkboxia,
            // koska valinta vaatii järjestelmän kansiovalitsimen eikä ole päällä/pois.
        }
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
    }
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
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
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

@Composable
private fun StyleRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

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
) {
    // Prosessikuoleman yli palautettu puskuri kohtaa uudelleen haetun sivun ilman
    // avainvertailua: jos sivu ehti muuttua sivustolla juuri siinä ikkunassa, vanhat raksit
    // näkyvät uuden sivun päällä. Raja on hyväksytty ja kirjattu eikä piilotettu, ja
    // session sisällä avain (page) nollaa puskurin kuten ennenkin.
    var checked by rememberSaveable(page, stateSaver = checkedSaver) { mutableStateOf(page.checked) }
    var radios by rememberSaveable(page, stateSaver = radioSaver) { mutableStateOf(page.radios) }

    val changed = countChanges(page, checked, radios)

    // Vieritys on ulompana (koko ruutu), joten tämä on pelkkä sarake.
    Column(modifier = Modifier.fillMaxWidth()) {
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
            ChoiceGroup(
                choice = choice,
                striped = index % 2 == 0,
                selected = radios[choice.name],
                onSelect = { value -> radios = radios + (choice.name to value) },
            )
        }

        if (page.toggles.isEmpty() && page.choices.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_empty),
                modifier = Modifier.padding(16.dp),
            )
        } else {
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
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = if (changed == 0) {
                stringResource(R.string.settings_changes_none)
            } else {
                stringResource(R.string.settings_changes_count, changed)
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Napin teksti on sivuston oma (`Update Preferences`) eikä käännös, samasta
            // syystä kuin selitteet: käyttäjä menee sivustolle etsimään samaa asiaa samalla
            // nimellä. Nappi on estetty kun muutoksia ei ole, koska muuttumattoman lomakkeen
            // lähettäminen olisi kirjoitus ilman syytä, ja jokainen kirjoitus tälle
            // lomakkeelle on riski jota ei tarvitse ottaa.
            Button(onClick = onSave, enabled = changed > 0) {
                Text(stringResource(R.string.settings_submit))
            }

            if (hasBaseline) {
                TextButton(onClick = onRestore) {
                    Text(stringResource(R.string.settings_restore))
                }
            }
        }

        if (hasBaseline) {
            Text(
                text = stringResource(R.string.settings_restore_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
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
) {
    Column(modifier = Modifier.fillMaxWidth().dgStripe(striped).padding(vertical = 8.dp)) {
        Text(
            text = choice.label ?: choice.name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
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
