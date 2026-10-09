package fi.tommi.dg.app.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate as drawRotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.app.session.BusyStyle
import fi.tommi.dg.app.session.DiceStyle
import fi.tommi.dg.app.session.ScoreStyle
import fi.tommi.dg.app.ui.DgBoard.Palette
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.BoardForm
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.DECLINE_SUBMIT
import fi.tommi.dg.domain.SiteBoardSettings
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.CompositionSession
import fi.tommi.dg.domain.CrawfordReading
import fi.tommi.dg.domain.Cube
import fi.tommi.dg.domain.CubePosition
import fi.tommi.dg.domain.Die
import fi.tommi.dg.domain.MoveArrow
import fi.tommi.dg.domain.PendingAction
import fi.tommi.dg.domain.dependsOnAssembly
import fi.tommi.dg.domain.CheckerCheck
import fi.tommi.dg.domain.reconcileCheckers
import fi.tommi.dg.domain.PipCheck
import fi.tommi.dg.domain.PlayerPanel
import fi.tommi.dg.domain.Reminder
import fi.tommi.dg.domain.gameKey
import fi.tommi.dg.domain.Point
import fi.tommi.dg.domain.reconcilePips
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lauta, ja 10.8.2026 alkaen myös pelaaminen.
 *
 * **Tae vaihtoi paikkaa portin purkautuessa, eikä se hävinnyt.** Tässä oli siihen asti
 * käännösaikainen tae siitä ettei ruudulla ole toimintoja: signatuurissa oli vain
 * [onRefresh]. Sellaista ei voi enää olla, koska ruutu on se paikka jossa siirto tehdään.
 * Tae asuu nyt siellä minne se kuuluu, eli lähetettävässä arvossa: `FormSubmission`in
 * konstruktori on `core-domain`in sisäinen, joten tämä composable ei voi koota lähetystä
 * vaikka se saisi mitä tahansa takaisinkutsuja. [onPress] ottaa napin nimen eikä osoitetta,
 * ja [onFollow] osoitteen jonka tämä sai sivulta luettuna laudan mukana.
 *
 * Se mikä ei muuttunut: ruutu ei aloita mitään itsestään. Jokainen pyyntö on ele, eikä
 * ruudulla ole ajastusta.
 *
 * **Ruudulla ei ole navigointinappeja 9.8.2026 alkaen** (Tommin valinta). Paluu on
 * järjestelmän oma reunapyyhkäisy ja uudelleenhaku on veto alaspäin, ks. [pullDownToRefresh].
 * Toimintonapit ovat eri asia: ne ovat sivun omia nappeja eivätkä ruudun keksimiä, ja
 * arvattava ele ei kelpaa peruuttamattomaan tekoon.
 *
 * Piirtotapa on tavallisia composableja eikä Canvasia, ja ensisijainen syy on että ruudusta
 * voi väittää jotain: Canvas ei jätä semantiikkapuuta, joten sen sisällöstä ei sano mitään
 * testi, ruudunlukija eikä kuvavertailu. Kuvakirjastoa ei tarvita, koska nappula on ympyrä
 * ja luku.
 */
// `systemBarsIgnoringVisibility` on kokeellinen rajapinta. Sen tilalla ei ole vakaata
// vaihtoehtoa: `systemBars` on nolla heti kun piilotus on kerran tehty, eli se vastaisi
// eri kysymykseen kuin se jota tässä kysytään.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoardScreen(
    state: BoardUiState,
    /** Tämän ottelun teko joka painettiin muttei mennyt perille, tai null. */
    pending: PendingAction?,
    /** Mitä odottavalle teolle viimeksi tapahtui, silloin kun se ei näy laudasta. */
    note: PendingNote?,
    onRefresh: () -> Unit,
    /** Laudan oman linkin seuraaminen. Osoite tulee tälle ruudulle sivulta, ei kootusti. */
    onFollow: (String) -> Unit,
    /** Sivun oman napin painaminen: nimi ja vahvistusruudun tila. */
    onPress: (String, Boolean) -> Unit,
    /**
     * Sivun oma **luettava** linkki, joka avataan sivunlukijalla eikä tässä näkymässä.
     *
     * Ero [onFollow]iin on sivun laji eikä osoitteen muoto: [onFollow] hakee laudan tälle
     * samalle ruudulle, ja tämä avaa sivun jota lautanäkymä ei osaa lukea. Ilman tätä
     * päättymissivun `Review game` päätyi omaan varareittiinsä (*"This is no longer a
     * board"*), vaikka sovelluksessa on siirtolistalle valmis lukija (`MatchLogParser`).
     */
    onOpenPage: (String) -> Unit = {},
    /**
     * Viestin lähetys ottelun chat-lomakkeella: teksti, lainausvalinta ja **sivun oma nappi**.
     *
     * Nappi on parametri eikä oletus, koska sama lähetys myös päättää vuoron: `Next Game`
     * siirtää jonossa eteenpäin ja `To Top` poistuu laudalta. Ruutu tarjoaa vain ne napit
     * jotka sivu antoi, eikä `ChatForm.write` hyväksy muita.
     */
    onSendChat: (String, Boolean, String) -> Unit = { _, _, _ -> },
    /** Päättymissivun *Reply to <pelaaja>*, ks. [MatchReplyUi]. Null piilottaa napin. */
    matchReply: MatchReplyUi? = null,
    /**
     * Fraasinapit chat-kortin kentän alle, sama lista kuin viestiruudussa (Tommin tilaus
     * 3.9.2026: *"lisää sama fraasirivi laudan chat-korttiin"*). Ottelun alku- ja
     * loppufraasit (`hi`, `gg`) kuuluvat juuri tänne, koska ottelun chat kirjoitetaan tällä
     * kortilla eikä viestiruudussa. Ks. [PhraseRow] ja `PhraseBook`.
     */
    phrases: List<String> = emptyList(),
    onAddPhrase: (String) -> Unit = {},
    onRemovePhrase: (String) -> Unit = {},
    /**
     * Paluu luettelolle **vain silloin kun ruudulla ei ole lautaa**.
     *
     * Lautaruudulla ei ole omaa paluunappia (9.8.2026, Tommin valinta): paluu on
     * järjestelmän ele, ja nappi veisi laudalta tilaa. Se päätös koskee lautaa, ja tässä
     * lautaa ei ole: [BoardUiState.NotABoard] on juuri se tila jossa sivua ei voi lukea.
     *
     * **Ele ei myöskään ole näkyvissä tällä ruudulla**, ja se on syy eikä mielipide: ruutu
     * piilottaa järjestelmäpalkit ([HideSystemBarsWhileVisible]) saadakseen nappulalle
     * korkeutta, joten tabletilla lukukelvottomalla sivulla ei ollut yhtään näkyvää tietä
     * pois vaikka teksti itse kehottaa palaamaan (*"Go back and refresh the list"*).
     * Havainto on 31.8.2026 laiteajosta, ks. `docs/AVOIMET.md`.
     */
    onBack: () -> Unit = {},
    /** Tämän pelin muistutukset, vanhin ensin. Ks. [ReminderStrip]. */
    reminders: List<Reminder> = emptyList(),
    onAddReminder: (String) -> Unit = {},
    onRemoveReminder: (Long) -> Unit = {},
    /**
     * Kirjoitustila (muistutus tai merkki) on auki. Kutsuja kääntää ruudun pystyyn sen
     * ajaksi (Tommin päätös 19.9.2026, `docs/UI.md` › Suunta lukittiin › Poikkeus).
     */
    onWritingChange: (Boolean) -> Unit = {},
    /** Aseman merkitseminen analyysiin, ks. [MarkStrip]. */
    onMarkPosition: (String) -> Unit = {},
    /**
     * Laudan tyyli, laitteen oma valinta (`docs/ASETUKSET.md` luku 3). Oletukset ovat
     * tarkoituksella nykytilan näköiset: SITE ilman säilöttyä tietoa piirtyy X-22-väreillä
     * peilaamatta, joten kutsuja joka ei anna näitä saa saman laudan kuin ennen tiloja.
     */
    style: BoardStyle = BoardStyle.SITE,
    /** Sivuston lautaan vaikuttavat asetukset laitteen säilöstä. Ks. [BoardLook.of]. */
    siteSettings: SiteBoardSettings = SiteBoardSettings.UNKNOWN,
    /**
     * Noppien ja pisteiden esitys, omat kytkimensä eivätkä [style]en sidotut (Tommi
     * 27.8.2026, `docs/ASETUKSET.md` luku 3). Oletukset ovat sovelluksen aiempi käytös.
     */
    diceStyle: DiceStyle = DiceStyle.COUNTER,
    scoreStyle: ScoreStyle = ScoreStyle.AWAY,
    /**
     * Noppien painallus tekona, kaksi laitteen kytkintä (Tommin tilaus 4.9.2026).
     * Oletukset ovat pois, eli kutsuja joka ei anna näitä saa saman laudan kuin ennen:
     * noppa on kuva. Sääntö on [diceTapFor], ei näissä lipuissa.
     */
    diceSubmitTap: Boolean = false,
    diceSwapTap: Boolean = false,
    /** Vastustajan noppien painallus heittää (testaajan toive 9.10.2026). Oletus pois. */
    diceRollTap: Boolean = false,
    /** `Mark position`, `Reminders` ja `Cube reminder` näkyvissä (9.10.2026), ks. `BoardExtrasStore`. */
    boardExtras: Boolean = true,
    /** Siirtonuolet kootusta siirrosta (Tommi 29.9.2026, `MoveArrowsStore`). Oletus pois. */
    moveArrows: Boolean = false,
    /** Painetun pisteen numero laudan reunassa (Tommi 4.10.2026, `PointPressStore`). Oletus pois. */
    pointPress: Boolean = false,
    /** Wood-teeman kolahdus (Tommi 4.10.2026, `WoodSounds.kt`). Oletus pois, eli äänetön. */
    woodSound: Boolean = false,
    /**
     * Laitteen `Confirm Beaver` (Tommin tilaus 27.9.2026, `BeaverConfirmStore`): kysyykö dialogi
     * ennen `Beaver!`ia ja `Accept Beaver`ia. Oletus on pois, eli kutsuja joka ei anna tätä saa
     * saman laudan kuin ennen.
     */
    confirmBeaver: Boolean = false,
    /**
     * Tosi kun pelaaja siirtää vasemmalla kädellä, jolloin sivupaneeli on oikealla
     * (Tommin päätös 9.9.2026). Oletus on oikeakätinen, eli sama paneelin puoli kuin ennen.
     */
    leftHanded: Boolean = false,
    /**
     * Laitteen koko näytön kytkin (Tommin tilaus 30.9.2026, `FullScreenStore`). Päällä
     * `MainActivity` piilottaa palkit koko sovellukselta, ja tämä ruutu ei tee omaa piilotusta,
     * koska sen purku toisi palkit takaisin luetteloon palatessa. Oletus pois.
     */
    fullScreen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Kolahdus puuhun: uusi lauta verrataan edelliseen saman ruudun aikana, ja kuution napit
    // soivat painettaessa. Pois päältä soitinta ei ole, joten mitään ei ladata eikä soi.
    val woodPlayer = rememberWoodSounds(woodSound)
    val loadedBoard = (state as? BoardUiState.Loaded)?.board
    val loadedSteps = (state as? BoardUiState.Loaded)?.composition?.steps?.size ?: 0
    var heardBoard by remember { mutableStateOf<BoardState?>(null) }
    var heardSteps by remember { mutableStateOf(0) }
    LaunchedEffect(loadedBoard, loadedSteps) {
        if (loadedBoard != null) {
            woodSoundFor(heardBoard, loadedBoard, heardSteps, loadedSteps)?.let { woodPlayer?.play(it) }
            heardBoard = loadedBoard
            heardSteps = loadedSteps
        }
    }
    val pressWithSound: (String, Boolean) -> Unit = { submit, verify ->
        if (submit in WOOD_CUBE_SUBMITS) woodPlayer?.play(WoodSound.CUBE)
        onPress(submit, verify)
    }

    // Vaakatila koko ruudun ajan: se ostaa korkeutta, ja korkeus on kiilan korkeus. Lukko on
    // tässä eikä manifestissa, jotta se koskee vain pelaamista.

    // **Mittaus on ennen `safeDrawingPadding`ia, ja se on ehto eikä järjestys.** Padding
    // kutistuu juuri silloin kun palkit piilotetaan, joten sen sisältä luettu korkeus
    // kumoaisi oman päätöksensä joka kierroksella. Sama ansa kuin `SideEffect`illä ylöspäin
    // luetulla paneelipäätöksellä 10.8.2026, ja siksi luku otetaan paikasta jota päätös ei
    // liikuta.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val bars = WindowInsets.systemBarsIgnoringVisibility
        // `IgnoringVisibility`, koska tavallinen `systemBars` on nolla heti kun piilotus on
        // kerran tehty. Sillä luettuna ehto vastaisi kysymykseen "maksavatko piilotetut
        // palkit mitään", ja vastaus olisi aina ei.
        val barsHeight = with(density) { (bars.getTop(density) + bars.getBottom(density)).toDp() }
        // Puhelimen vaakatilassa kolmen napin palkki on sivulla eikä alhaalla, jolloin se
        // ei maksa korkeutta vaan sivupaneelin leveyttä (Pixel 8a 16.9.2026: 126 px paneelin
        // 600:sta). Leveys luetaan samasta lähteestä samasta syystä kuin korkeus.
        val layoutDirection = LocalLayoutDirection.current
        val barsWidth = with(density) {
            (bars.getLeft(density, layoutDirection) + bars.getRight(density, layoutDirection)).toDp()
        }

        // Palkit pois niin kauan kuin tämä ruutu on näkyvissä, mutta vain jos ne oikeasti
        // maksavat nappulan kokoa. Ruudun tasolla eikä tilan, koska lauta on tämän ruudun
        // tarkoitus myös silloin kun se ei juuri nyt ole latautunut: palkkien vilkkuminen
        // latauksen ja laudan välissä olisi levotonta eikä antaisi tilaa.
        // **Päätös luetaan kerran eikä joka mittauksella** (mitattu logcatista 4.9.2026
        // illalla). `barsHeight` oli jo suojattu `IgnoringVisibility`llä, mutta `maxHeight`
        // ei ollut: ikkunan korkeus **kasvaa juuri silloin kun palkit piilotetaan**, joten
        // ehto kumosi oman päätöksensä ja efekti pallotteli purkunsa kanssa.
        //
        // Loki paluun hetkeltä, kutsupinot mukaan lukien: `show` kohdasta
        // `HideSystemBarsWhileVisible ... onDispose`, 134 ms myöhemmin `hide` efektin
        // rungosta, 153 ms myöhemmin `show` purusta uudelleen. Ruudulla se oli palkkien
        // vilkkumista kolmesti noin 0,65 sekunnin ajan, ja jokainen vilkku siirsi koko
        // sisältöä insettien verran. Juuri se oli se *jumppa* jota etsittiin neljä kierrosta.
        //
        // `remember` lukitsee päätöksen, ja avain on suunta eikä korkeus. Ilman avainta
        // ~~päätös luettiin tämän ruudun eliniäksi~~, ja se oli puhelimella väärä ruutu:
        // luettelosta tullessa tämä ruutu koostuu ensimmäisen kerran **vielä pystyssä**
        // (914 dp korkeutta, palkit ilmaisia), suuntalukko kääntää ikkunan vasta sen
        // jälkeen, ja lukittu *ilmainen* jäi voimaan vaakatilassa. Mitattu logcatista
        // 16.9.2026: Pixel 8a:lla palkit eivät olleet piiloutuneet kertaakaan, vaikka
        // 20.8. testi sanoo ettei niiden pidä näkyä. Suunta avaimena mittaa vaakatilassa
        // täsmälleen kerran, ja palkkien piilotus ei vaihda suuntaa, joten 4.9. pallottelu
        // ei palaa. Pystyssä ei piiloteta, koska palkit vievät silloin korkeutta ja pystyssä
        // lauta mitoittuu leveydestä (myös 23.9.2026 alkaen, kun pystyssä voi pelata).
        val landscape = maxWidth > maxHeight
        val hideBars = remember(landscape) {
            landscape && !DgBoard.barsAreFree(
                windowHeight = maxHeight,
                barsHeight = barsHeight,
                barsWidth = barsWidth,
            )
        }
        HideSystemBarsWhileVisible(enabled = hideBars && !fullScreen)

        // Ikonit taustan mukaan: vaaleiksi tummalla paneelilla, tummiksi vaaleassa tilassa
        // (27.9.2026). Tämä ei jaa piilotuksen ehtoa: näkyvä palkki tarvitsee tämän, ja
        // piilotettu palkki tarvitsee sen silloin kun se pyyhkäistään hetkeksi esiin.
        // Paneelin värit tyylistä (Tommin päätös 17.9.2026, [PanelLook]): luetaan tässä
        // eikä [BoardLook]ista, koska tausta piirretään myös lataus- ja virhetiloissa,
        // ennen kuin sivun skeema on tiedossa. Lapset lukevat saman [LocalPanelLook]ista.
        // Vaaleassa tilassa väri tulee teemasta eikä tyylistä (27.9.2026, [panelLookFor]).
        val panelLook = panelLookFor(style)
        DarkSystemBarIconsWhileVisible(lightBackground = panelLook.light)
        CompositionLocalProvider(LocalPanelLook provides panelLook, LocalCubeLook provides CubeLook.of(style)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(panelLook.background)
                // Vain palkit, ei näyttölovea (Tommin päätös 16.9.2026, `docs/UI.md` › Lovi
                // ja sivupalkki). Lovi on puhelimen vaakatilassa paneelin reunalla ja sen
                // inset vei viidenneksen paneelista; reikä itse osuu ottelukortin
                // reunaviivaan. Kun palkit on piilotettu, tämä on nolla ja lauta saa koko
                // ikkunan. Lupa piirtää loven alle on `MainActivity`ssä.
                // **Poikkeus 30.9.2026: yläreunan lovi väistetään** (Tommin havainto Pixel
                // 8a:lla pystyssä koko näytön kanssa: reikä oli turnausnimen päällä). Ennen
                // koko näyttöä tilapalkki peitti reiän, piilotettuna palkki on nolla. Vain
                // yläreuna, joten vaakatilan sivulovi jää 16.9. päätöksen mukaisesti.
                .windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Top)),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Odottava teko on jokaisen tilan yläpuolella eikä laudan sisällä, ja se on ehto
                // eikä asettelu: teko jäi lähettämättä myös silloin kun sivu ei juuri nyt ole
                // lauta, ja juuri silloin sen näkeminen on tärkeintä.
                PendingBar(pending = pending, note = note)

                // Kitkamittauksen sulkeva merkki: ensimmäinen kehys joka piirtää uuden tilan
                // (`KitkaLoki`). drawWithContent ajetaan piirrossa eikä kompositiossa, joten
                // se on lähinnä sitä hetkeä jolloin lauta on ruudulla.
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).drawWithContent {
                        drawContent()
                        KitkaLoki.piirretty(state)
                    },
                ) {
                    when (state) {
                        is BoardUiState.Loaded -> CompositionLocalProvider(
                            LocalActionsBlocked provides state.blocked,
                            LocalScoreBesideName provides (scoreStyle == ScoreStyle.NAME),
                        ) {
                            LoadedBoard(
                                state,
                                style = style,
                                siteSettings = siteSettings,
                                diceStyle = diceStyle,
                                scoreStyle = scoreStyle,
                                diceSubmitTap = diceSubmitTap,
                                diceSwapTap = diceSwapTap,
                                diceRollTap = diceRollTap,
                                boardExtras = boardExtras,
                                moveArrows = moveArrows,
                                pointPress = pointPress,
                                onPointMiss = { woodPlayer?.play(WoodSound.MISS) },
                                confirmBeaver = confirmBeaver,
                                leftHanded = leftHanded,
                                onRefresh = onRefresh,
                                onFollow = onFollow,
                                onPress = pressWithSound,
                                onOpenPage = onOpenPage,
                                onSendChat = onSendChat,
                                phrases = phrases,
                                onAddPhrase = onAddPhrase,
                                onRemovePhrase = onRemovePhrase,
                                reminders = reminders,
                                onAddReminder = onAddReminder,
                                onRemoveReminder = onRemoveReminder,
                                onWritingChange = onWritingChange,
                                onMarkPosition = onMarkPosition,
                            )
                        }

                        BoardUiState.Loading -> BusyCentered()

                        is BoardUiState.MatchOver -> MatchOverContent(
                            state = state,
                            onFollow = onFollow,
                            onPress = onPress,
                            onOpenPage = onOpenPage,
                            onSendChat = onSendChat,
                            matchReply = matchReply,
                            phrases = phrases,
                            onAddPhrase = onAddPhrase,
                            onRemovePhrase = onRemovePhrase,
                        )

                        // Väriä ei enää anneta käsin (13.9.2026): `RetryNote` maalaa oman
                        // lukupintansa teeman taustavärillä ja valitsee tekstin siihen
                        // sopivaksi. Pakotettu `Palette.TextPrimary` oli 10.8.2026 korjaus
                        // ajalta jolloin teksti oli suoraan `PanelBg`in päällä, ja lukupinnan
                        // tulon (6.9.) jälkeen se teki vaalean teeman kortista lukukelvottoman:
                        // kerma `#F0E7D6` vaalean `#EBE8E4` päällä, mitattu tabletilta
                        // nellipeltonenn pikaviestillä pelisessiossa 13.9. ilta2.
                        // Paluu on tässä tilassa ruudun oma nappi eikä pelkkä ele, ks.
                        // [onBack]. `RetryNote`n nappi kantaa sen sellaisenaan: sen
                        // `retryLabel` on olemassa juuri siksi että uudelleenyritys on
                        // joskus paluu eikä päivitys.
                        // **Top Page on automaattinen paluu eikä lukukelvoton sivu**
                        // (Tommin päätös 4.9.2026 illalla). Ruutu sulkeutuu itsestään heti
                        // kun tuore luettelo on haettu, joten kehotus painaa `Back` olisi
                        // väärä neuvo ja ehtisi näkyä vain välähdyksenä. Odotus näytetään
                        // täällä eikä otteluluettelossa, jotta luettelo ilmestyy kerralla
                        // valmiina: yksi vaihdos eikä kolmea. Ks. [MainActivity]in paluu.
                        is BoardUiState.NotABoard -> if (state.kind is NotABoardKind.TopPage) {
                            BusyCentered()
                        } else {
                            RetryNote(
                                text = state.kind.text(),
                                onRetry = onBack,
                                retryLabel = stringResource(R.string.board_back),
                            )
                        }

                        is BoardUiState.Failed -> RetryNote(
                            text = state.reason.text(),
                            onRetry = onRefresh,
                        )

                        BoardUiState.SessionExpired -> RetryNote(
                            text = stringResource(R.string.board_session_expired),
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * Palkki odottavasta teosta, eli siitä mitä painettiin muttei mennyt perille.
 *
 * **Teksti kertoo mitä ei tiedetä, ei mitä tapahtui.** Verkkokerros ei voi tietää menikö
 * lähetys perille, joten palkki sanoo sen ääneen sen sijaan että väittäisi siirron
 * jääneen tekemättä. Väärä varmuus olisi tässä pahempi kuin epätietoisuus, koska pelaaja
 * voi tarkistaa asian selaimesta ja hylätä rivin itse.
 *
 * Napin nimi tulee sivulta sellaisenaan (`Submit Move`, `Roll Dice`), samasta syystä kuin
 * [MiddleActionsRow]issa: sovellus joka sanoo eri sanan kuin sivu eroaa kohteestaan juuri
 * siinä kohdassa jossa käyttäjä vertaa niitä rinnakkain.
 *
 * [note] näkyy vasta kun rivi on jo poistettu, eli se kertoo miksi palkki katosi. Ilman sitä
 * haku joka päätyi toteamukseen "sivu ei enää tarjoa tätä" olisi äänetön.
 *
 * **`Try again` -nappia ei enää ole** (Tommin päätös 8.9.2026, *"Refresh korvaa sen"*). Se
 * haki sivun ja lähetti sitten uudestaan, ja haku on nyt se joka ratkaisee rivin: onnistunut
 * laudan haku poistaa sen lähettämättä kun sivu ei enää tarjoa tekoa
 * ([BoardViewModel.resolvePending]). Jos sivu yhä tarjoaa sen, nappi on laudalla ja pelaaja
 * painaa sitä itse. Palkki siis kertoo, muttei lähetä mitään.
 *
 * **`Discard`-nappia ei enää ole** (Tommin tilaus 16.9.2026 ensimmäisen oikean laukeamisen
 * jälkeen: *"Discard-nappi on turha, palkkiin jää vain Refresh-neuvo"*). Palkki on nyt
 * pelkkä teksti, ja se katoaa kun sama lauta on haettu tuoreena, myös kootun siirron
 * tapauksessa (ks. [BoardViewModel.resolvePending]).
 */
@Composable
private fun PendingBar(
    pending: PendingAction?,
    note: PendingNote?,
) {
    if (pending == null) {
        if (note == PendingNote.NoLongerOffered) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.board_pending_gone),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        // Kokoamisesta riippuva teko saa oman tekstinsä, koska haku nollaa kokoamisen
        // (mitattu 10.8.2026): siirtoa ei voi lähettää uudestaan, ja tuore lauta kertoo
        // menikö se perille.
        val resolvesItself = !pending.dependsOnAssembly

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.board_pending_title, pending.submit),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    if (resolvesItself) R.string.board_pending_body
                    else R.string.board_pending_body_assembled,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}


/**
 * Katkotilan `Refresh` valmiina tekona ja sen syy, tai null kun lauta on vahvistettu.
 *
 * Yksi arvo eikä kaksi rinnakkaista parametria, koska nappi ja sen viereinen teksti ovat
 * yksi asia: kumpikaan ei ole mielekäs ilman toista, ja kahdella parametrilla kutsuja voisi
 * antaa syyn ilman nappia. Kulkee laudalle asti, joten lauta ei lue tilamallia itse.
 */
private data class UnconfirmedRefreshSpec(
    val reason: Failure,
    val onRefresh: () -> Unit,
)

/**
 * Vahvistamattoman laudan `Refresh`-nappi keskikaistan keskellä (Tommin tilaus 7.9.2026).
 *
 * Katko on ainoa hetki jolloin pelaaja ei tiedä mitä tehdä, ja silloin ohje *"pull down"*
 * dialogissa oli ainoa reitti eteenpäin: ele ei näy ruudulla, ja dialogi suljetaan ennen kuin
 * sitä voi tehdä. Tommin sanat: *"Paluun ja alaspäin swaippauksen sijaan kannatan
 * Refresh-nappia keskellä ruutua."* Luenta kuitattiin 7.9.2026: nappi katkotilaan ja veto
 * jää sinnekin, eli molemmat hakevat saman sivun kerran ([BoardViewModel.refresh]).
 *
 * **Paikka on keskikaistan keskikohta eli muurin kohta, ja tarvittaessa omistamattoman
 * kuution päällä** (Tommin tarkennus samana iltana, kuvan jälkeen: ensimmäinen muoto oli koko
 * laudan keskipiste, joka osui kaistalle mutta noppien viereen). Kuution peittäminen on
 * tässä tilassa hinnatonta, koska lauta on näyttö eikä pelipinta ([BoardViewModel.follow]
 * hylkää kaiken) eikä kuutiota voi klikata. Piirretään kaistan viimeisenä, jotta se on
 * päällimmäisenä myös silloin kun napit ovat kaistalla (puhelin).
 *
 * **Syy on tilarivi eikä napin vieressä eikä dialogissa** (Tommin valinta 26.9.2026,
 * `sessio-26-9-ilta2`). 8.9.–26.9.2026 napin vieressä luki lyhyt syy ja sen lisäksi aukesi
 * `Board not confirmed` -dialogi. Nyt syy on keskikaistan tilarivien ensimmäinen
 * ([stripNotes]), samassa lokerossa kuin *Please make a checker move*, ja se jää näkyviin
 * niin kauan kuin lauta on vahvistamaton.
 *
 * **Vain tässä tilassa**, koska 9.8.2026 päätös (veto napin tilalle) on yhä voimassa
 * muualla: lauta pysyy vapaana päällepiirroista silloin kun sillä pelataan. Nappi katoaa
 * itsestään kun tuore lauta on saatu, koska `unconfirmed` nollautuu vasta silloin.
 *
 * **Kokoa kasvatettiin Tommin tilauksesta 8.9.2026** (*"keskellä näyttöä oleva Refresh-nappi
 * voisi olla isompi"*). Mitat ovat omat eivätkä Material3:n oletus, koska oletusnappi mitoitetaan
 * lomakkeeseen ja tämä on ainoa reitti eteenpäin umpikujassa. Peittoala kasvaa myös siksi että
 * lauta on tässä tilassa lukittu, joten napin alla ei ole mitään jota se veisi.
 *
 * **Nappi keskittyy kuution kohdalle** (Tommin havainto 14.9.2026: *"Refresh-napin pitäisi
 * peittää tuplauskuutio kokonaan"*). Napin vähimmäiskoko on kuutio reunuksineen
 * ([UNCONFIRMED_COVER_MARGIN]), koska pelkkä tekstin mitta ei riittänyt peittämään kuutiota
 * korkeussuunnassa tabletin nappulakoolla.
 */
@Composable
private fun UnconfirmedRefresh(
    metrics: BoardMetrics,
    onRefresh: () -> Unit,
) = BoxWithConstraints {
    val cover = metrics.cubeSize + UNCONFIRMED_COVER_MARGIN * 2
    // **Matalalla kaistalla nappi mitoitetaan kaistaan** (Tommin havainto ja valinta 24.9.2026
    // puhelimen pystylaudalla: napin teksti leikkautui pois ja *Site error 503* piirtyi
    // kuution päälle). Täysikokoinen nappi on 60 dp korkea, ja pystylaudan kaista on kahden
    // pienen nappulan korkuinen. Silloin pystypehmuste putoaa ja teksti pienenee. Virheteksti
    // oli ensin napin vasemmalla, ja myöhemmin samana yönä pystylaudan syy siirtyi paneelin
    // tilariviksi. 26.9.2026 alkaen syy ei ole napin vieressä lainkaan ([stripNotes]).
    val cramped = maxHeight < UNCONFIRMED_FULL_HEIGHT
    Layout(
        content = {
            Button(
                onClick = onRefresh,
                contentPadding = if (cramped) {
                    PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                } else {
                    PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                },
                modifier = Modifier.sizeIn(
                    minWidth = cover,
                    minHeight = if (cramped) minOf(cover, maxHeight) else cover,
                ),
            ) {
                Text(
                    text = stringResource(R.string.top_refresh),
                    style = if (cramped) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val button = measurables[0].measure(loose)
        layout(constraints.maxWidth, button.height) {
            button.place((constraints.maxWidth - button.width) / 2, 0)
        }
    }
}

/** Katkotilan syy lyhyenä, tilarivin alku ([stripNotes]). */
@Composable
private fun Failure.chipText(): String = when (this) {
    Failure.Offline -> stringResource(R.string.board_unconfirmed_chip_offline)
    is Failure.Server -> stringResource(R.string.board_unconfirmed_chip_server, code)
    Failure.Sleeping -> stringResource(R.string.board_unconfirmed_chip_sleeping)
}

/** Kuinka paljon katkotilan nappi ulottuu kuution reunan yli joka suuntaan. */
private val UNCONFIRMED_COVER_MARGIN = 6.dp

/** Täysikokoisen katkotilan napin korkeus: 16 dp pehmuste kahdesti ja `titleLarge`n rivi. */
private val UNCONFIRMED_FULL_HEIGHT = 60.dp

/**
 * Veto alaspäin hakee sivun uudelleen (Tommin valinta 9.8.2026, `Refresh`-napin tilalle).
 * Katkotilassa laudan keskellä on lisäksi nappi (7.9.2026, [UnconfirmedRefresh]), ja veto
 * toimii silloinkin.
 *
 * **Ele on tehty käsin eikä `PullToRefreshBox`illa**, ja syy on rakenteellinen: Material3:n
 * oma toteutus kuuntelee sisäkkäistä vieritystä, ja tämä ruutu on tarkoituksella vierittämätön
 * (vieritettävä lauta hylättiin 8.8.2026, koska asemaa pitää voida lukea yhdellä silmäyksellä).
 * Vieritystapahtumia ei siis synny, joten valmis komponentti ei laukeaisi kertaakaan.
 *
 * Kynnys on matka eikä nopeus: hidas ja huolellinen veto laukaisee saman kuin nopea, koska
 * kohde on vapaaehtoisvoimin pyöritetty sivusto ja yksi haku vahingossa on eri asia kuin
 * useita. Sama syy pitää haun **yhtenä per ele**: [fired] estää toiston kesken vedon, joten
 * pitkä veto ei kerro pyyntöä kahdesti.
 *
 * Ele ei näy ruudulla, ja se on tämän ratkaisun tunnettu hinta: se kuuluu pelaajan manuaaliin.
 *
 * **Ele kantoi 10.8.2026 päivän ajan kaksi tekoa matkan mukaan:** vedon alku paljasti ruudun
 * tiedot päällepiirtona ja kynnyksen ylitys haki sivun. Paljastus poistui samana päivänä, kun
 * tiedot saivat pysyvän paikan laudan vierestä (`SidePanel`). Se ei ollut huono ele vaan
 * tarpeeton: tiedot jotka ovat koko ajan näkyvissä eivät tarvitse paljastajaa, ja kaksi tekoa
 * samassa eleessä maksoi selitystä jota yksi teko ei tarvitse.
 */
private fun Modifier.pullDownToRefresh(
    onRefresh: () -> Unit,
): Modifier =
    this.pointerInput(onRefresh) {
        val threshold = PULL_THRESHOLD.toPx()
        var travelled = 0f
        var fired = false
        detectVerticalDragGestures(
            onDragStart = {
                travelled = 0f
                fired = false
            },
        ) { change, delta ->
            travelled += delta
            if (!fired && travelled > threshold) {
                fired = true
                onRefresh()
            }
            change.consume()
        }
    }

/**
 * Ottelun keskustelu: vastustajan viesti, kirjoituskenttä ja sivun omat napit.
 *
 * **Näkyy vain siirron jälkeisellä sivulla**, koska vain siellä on chat-lomake. Se ei ole
 * rajoitus vaan sivuston muoto: viesti näkyy tuolla sivulla eikä palvelin säilytä sitä.
 * Saapunut viesti on jo arkistossa siinä vaiheessa kun tämä piirtyy, ks.
 * `BoardViewModel.readChat`.
 *
 * **Napit tulevat sivulta eikä täältä.** Lähetys ja vuoron päättäminen ovat sivustolla sama
 * teko, joten nappirivi on sivun oma (`Next Game`, `To Top`) eikä sovelluksen keksimä
 * `Send`. Se on myös ainoa rehellinen esitys siitä mitä painallus tekee: viesti lähtee ja
 * ruutu vaihtuu samalla.
 *
 * **Lainausvalinta on päällä silloin kun sivu piti sitä päällä.** Juuri se tekee edellisestä
 * viestistä näkyvän vastaanottajalle (`docs/AVOIMET.md`), joten sovellus ei saa hiljaa
 * pudottaa sitä; valinnan saa purkaa vain käyttäjä itse.
 */
/**
 * Ottelun päättymissivu.
 *
 * **Sivun omat sanat ensin.** Tulos ja pisteet piirtyvät sellaisina kuin sivusto ne kirjoitti,
 * eikä sovellus laske voittajaa tai pisteitä itse: sama sääntö kuin `Cube.label`illa ja
 * ottelulokilla. Tästä ruudusta puuttuu tarkoituksella kaikki tulkinta.
 *
 * **Chat on sama kortti kuin laudalla** ([ChatCard]) eikä oma toteutuksensa: lomake on sama,
 * ja kaksi korttia samasta asiasta olisi kaksi ylläpidettävää. Kortti on auki heti, koska
 * tällä sivulla ei ole muuta tekemistä ja kirjoittamisen hetki on juuri tämä.
 *
 * **Napit ovat sivun omia.** `Next Game` ja `To Top` ovat chat-lomakkeen submitteja silloin
 * kun kenttä on olemassa (jolloin ne kulkevat kortin kautta ja vievät kirjoitetun tekstin
 * mukanaan), ja muussa tapauksessa sivulla on `Next Game>>` tavallisena linkkinä. Siksi
 * tässä ei ole omaa nappilogiikkaa vaan kaksi haaraa, kumpikin sivun mukaan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchOverContent(
    state: BoardUiState.MatchOver,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    onOpenPage: (String) -> Unit,
    onSendChat: (String, Boolean, String) -> Unit,
    matchReply: MatchReplyUi?,
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
) {
    val page = state.page
    // Kenen riville nappi tulee: jokainen pelaaja jolla on profiili, paitsi kirjautunut
    // itse, kuten laajennuksessa. Tuntematon oma nimi näyttää napin molemmille.
    var replyTo by rememberSaveable(page.matchId) { mutableStateOf<String?>(null) }
    // Onnistunut lähetys sulkee kentän (Tommin tilaus 29.9.2026): auki jäänyt tyhjä kenttä
    // näytti chat-kortin kanssa kahdelta päällekkäiseltä viestikentältä. Kuittaus jää napin
    // alle, ja nappi avaa uuden viestin.
    val replySent = matchReply?.send == ReplyUiState.Sent
    LaunchedEffect(replySent) {
        if (replySent) replyTo = null
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Lovi väistetään tässä vaikka lautareitti ei sitä väistä (16.9.2026): se
            // päätös koski lautaa, jossa reikä osuu reunaviivaan. Tämä on tekstiruutu, ja
            // Pixel 8a:n vaakatilassa reikä söi `Next Game`n N-kirjaimen (Tommin havainto
            // pelisessiossa 21.9.2026, `raakasivut/sessio-21-9-ilta/lovi-next-game.png`).
            .windowInsetsPadding(WindowInsets.displayCutout)
            // Näppäimistön väistö: NavHostin `imePadding` ei koske laudan reittiä, jolla tämä
            // ruutu on (3.10.2026). Ilman sitä kenttä ja lähetysnappi jäivät näppäimistön alle.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        page.eventName?.let { name ->
            // Turnauksen nimi rarity-värissä (2.10.2026). Sivun kierros on `Round 3` ilman
            // kokonaismäärää, joten luku tulee nähdyistä turnausnumerolla. Väri koskee vain
            // nimeä, ja kierros pitää linkin tai toissijaisen tekstin värin.
            val path = page.eventPath
            val nameColor = tournamentRarityColor(eventIdFromPath(path), name, dark = !LocalPanelLook.current.light)
            val heading = textWithTournamentColor(
                listOfNotNull(name, page.roundLabel).joinToString(", "),
                name,
                nameColor,
            )
            // Osoite on ollut jäsennettynä alusta asti muttei käytössä. Turnaussivulle on
            // lukija, joten otsikko on linkki silloin kun sivu antaa osoitteen ja pelkkää
            // tekstiä silloin kun ei anna.
            if (path == null) {
                Text(text = heading, style = MaterialTheme.typography.titleSmall, color = LocalPanelLook.current.secondary)
            } else {
                TextButton(
                    onClick = { onOpenPage(path) },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(text = heading, style = MaterialTheme.typography.titleSmall, color = LocalPanelLook.current.accent)
                }
            }
        }
        Text(
            text = page.resultText,
            style = MaterialTheme.typography.titleLarge,
            color = LocalPanelLook.current.text,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        if (page.predicted) {
            Text(
                text = stringResource(R.string.match_over_predicted),
                style = MaterialTheme.typography.bodySmall,
                color = LocalPanelLook.current.secondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        page.matchLength?.let { length ->
            Text(
                text = stringResource(R.string.match_over_length, length),
                style = MaterialTheme.typography.bodySmall,
                color = LocalPanelLook.current.secondary,
            )
        }
        // Lomakkeeton viestikortti tuloksen ja pisterivien väliin (Tommin valinta 3.10.2026,
        // vertailukuvat `raakasivut/sessio-3-10-ilta/kuori/viestikortti-*`). Ruutu luetaan
        // silloin järjestyksessä: mitä vastustaja sanoi, oma vastaus ja Send, sivun napit.
        // Sendin alla kortti näytti toiselta tekstikentältä.
        state.chat?.takeIf { it.form == null }?.let { chat ->
            ChatCard(
                chat = chat,
                composerOpen = false,
                onOpenComposer = {},
                onSendChat = onSendChat,
                phrases = phrases,
                onAddPhrase = onAddPhrase,
                onRemovePhrase = onRemovePhrase,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                // Ruutu vierii itse, ks. `ChatCard.messageScrolls`.
                messageScrolls = false,
            )
        }
        page.scores.forEach { row ->
            // Pisterivi avaa pelaajan profiilin kun sivu antaa linkin (Tommin havainto
            // 21.9.2026: onnittelu vastustajalle heti ottelun päätyttyä, ja profiilin
            // `Message` on siihen ainoa reitti koska ottelun chat sulkeutui). Nimi on
            // alleviivattu vain kun polku on, samoin kuin muualla: lupaus pitää.
            val path = row.player.profilePath
            Text(
                text = listOfNotNull(row.player.name, row.score?.toString()).joinToString("  "),
                style = MaterialTheme.typography.bodyLarge,
                // Rarity-väri voittaa linkin korostuksen (Tommin tilaus 26.9.2026), ja
                // alleviivaus kertoo yhä linkistä. Sävy seuraa lautaruudun pintaa (27.9.2026).
                color = playerRarityColor(row.player.userId, row.player.name, dark = !LocalPanelLook.current.light)
                    ?: if (path != null) LocalPanelLook.current.accent else LocalPanelLook.current.text,
                textDecoration = if (path != null) TextDecoration.Underline else null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .then(if (path != null) Modifier.clickable { onOpenPage(path) } else Modifier),
            )
            val name = row.player.name
            val canReply = matchReply != null && path != null && name != null &&
                !name.equals(matchReply.selfName, ignoreCase = true)
            if (canReply && replyTo != name) {
                // Nappi pisterivin alla eikä vieressä: puhelimen kapealla ruudulla rivi on
                // nimi ja luku, ja nappi sen perässä rikkoisi rivin. Laajennus lisää sen
                // taulukon soluun, mikä on sama paikka työpöydän mitoissa.
                OutlinedButton(
                    onClick = {
                        replyTo = name
                        matchReply!!.onOpen()
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(stringResource(R.string.match_over_reply, name.orEmpty()))
                }
                if (replySent) {
                    Text(
                        text = stringResource(R.string.page_message_sent),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalPanelLook.current.secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (canReply && replyTo == name) {
                MessageComposer(
                    recipient = name,
                    maxLength = null,
                    draft = matchReply!!.draft,
                    send = matchReply.send,
                    onDraftChange = matchReply.onDraftChange,
                    onSend = { matchReply.onSend(row.player) },
                    phrases = phrases,
                    onAddPhrase = onAddPhrase,
                    onRemovePhrase = onRemovePhrase,
                )
            }
        }

        // Lomakkeellinen kortti on kirjoituspinta ja kantaa sivun napit, joten se jää tähän
        // nappien paikalle. Lomakkeeton on pisterivien yläpuolella, ks. alku.
        state.chat?.takeIf { it.form != null }?.let { chat ->
            ChatCard(
                chat = chat,
                // Kenttä auki vain kun sivulla on kenttä. Lomakkeeton kortti on viestin
                // näyttö, ja sivun omat napit tulevat alla `page.form`ista (7.9.2026).
                composerOpen = chat.form != null,
                onOpenComposer = {},
                onSendChat = onSendChat,
                phrases = phrases,
                onAddPhrase = onAddPhrase,
                onRemovePhrase = onRemovePhrase,
                modifier = Modifier.padding(top = 12.dp),
                // Ruutu vierii itse, ks. `ChatCard.messageScrolls`.
                messageScrolls = false,
            )
        }

        FlowRow(modifier = Modifier.padding(top = 12.dp)) {
            // Sivun oman lomakkeen napit ensin ja sivun järjestyksessä, koska ne ovat sen
            // muunnelman ainoa tie eteenpäin (`MatchOverForm`). Chat-muunnelmassa lomaketta
            // ei ole, ja samat napit tulevat chat-kortin mukana; lista on silloin tyhjä eikä
            // nappi näy kahdesti.
            page.form?.submits?.forEach { submit ->
                TextButton(onClick = { onPress(submit, false) }) {
                    // Näyttöteksti käännetään samalla säännöllä kuin laudalla ja chat-
                    // kortilla (`submitText`, Tommin päätös 1.9.2026). Tämä polku oli jäänyt
                    // raakatekstille, ja nauha 12.9.2026 klo 22.16 näytti `To Top` viestittömällä
                    // päättymissivulla samana iltana kun chat-muunnelma näytti `To Matches`.
                    Text(submitText(submit))
                }
            }
            page.nextGamePath?.let { path ->
                TextButton(onClick = { onFollow(path) }) {
                    Text(stringResource(R.string.match_over_next))
                }
            }
            page.reviewPath?.let { path ->
                // Siirtolista ei ole lauta, joten se avataan sivunlukijalla. Lautanäkymään
                // haettuna se päätyi `NotABoardKind.WrongPage`en, eli sovellus sanoi ettei
                // sivu ole lauta — mikä oli totta ja hyödytöntä, koska lukija oli olemassa.
                TextButton(onClick = { onOpenPage(path) }) {
                    Text(stringResource(R.string.match_over_review))
                }
            }
            page.skipPath?.let { path ->
                TextButton(onClick = { onFollow(path) }) {
                    Text(stringResource(R.string.match_over_skip))
                }
            }
        }
    }
}

@Composable
private fun ChatCard(
    chat: ChatOnBoard,
    /**
     * Onko kirjoituskenttä auki. Suljettuna kortti on viestin näyttö ja sivun napit,
     * eli pienin muoto joka ei hukkaa sisältöä (Tommin havainto ja kuittaus 27.8.2026:
     * kortti avautui isona jokaisella siirrolla vaikkei ollut mitään mihin vastata).
     */
    composerOpen: Boolean,
    onOpenComposer: () -> Unit,
    onSendChat: (String, Boolean, String) -> Unit,
    /**
     * Sulkee kentän lähettämättä. Null päättymisruudulla, jossa kenttä on aina auki eikä
     * sulkemista ole. Laudalla tarpeen 21.9.2026 alkaen, koska avattu kenttä kääntää
     * ruudun pystyyn (kirjoitustila) ja vahingossa avattu kortti jäi auki sivun vaihtoon
     * asti; Tommi sulki sen laitteen back-eleellä, joka vei koko laudalta pois.
     */
    onCloseComposer: (() -> Unit)? = null,
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Vieriikö viestiosa kortin sisällä. Tosi laudalla, jossa kortilla on katto
     * (`BoxWithConstraints`) ja painotus jakaa sen. Epätosi päättymisruudulla, joka vierii
     * itse: siellä sarakkeen korkeus on rajaton, ja painotettu lapsi saa rajattomassa
     * sarakkeessa nollan. Mitattu 7.9.2026 kuorella: kortti piirtyi tyhjänä palkkina ja
     * viesti oli näkymätön vaikka arkistossa. Sama vika oli ollut kenttämuunnelmassa
     * piilossa, koska kenttä ja napit antoivat kortille korkeuden.
     */
    messageScrolls: Boolean = true,
) {
    // Luonnos ja valinta nollautuvat kun sivu vaihtuu, koska avain on sivun oma
    // lomakeosoite: seuraavan ottelun kenttään ei jää edellisen ottelun tekstiä.
    //
    // Omistaja on ruutu eikä näkymämalli, ja taso on todettu riittäväksi (Tommi 31.8.2026,
    // kompositioauditoinnin kysymys 2): rememberSaveable kestää taustasammutuksen, ja jos
    // luonnos silti katoaa, viestiin voi vastata Inboxista.
    // Viestiluonnokset ovat näkymämallissa eri syystä, ks. `MessagesViewModel`.
    // Lomake voi puuttua ([ChatOnBoard.form]); silloin kortti on viestin näyttö eikä
    // kirjoituspinta, ja avaimena on tyhjä koska ei ole mitään mihin luonnos sitoutuisi.
    val form = chat.form
    var draft by rememberSaveable(form?.action.orEmpty()) { mutableStateOf("") }
    var quote by rememberSaveable(form?.action.orEmpty()) {
        mutableStateOf(form?.quoteCheckedByDefault ?: false)
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Vain viesti vierii; kenttä, lainausvalinta ja nappirivi ovat aina näkyvissä.
            // Ensimmäinen muoto pani koko sisällön samaan vieritykseen, ja Reply avasi
            // silloin kentän näkyvän alueen alapuolelle ilman että mikään vihjasi siitä
            // (mitattu Pixel 8a:lla 28.8.2026, uiautomator: kenttä oli puussa mutta
            // viewportin ulkopuolella). Kenttä ei kasva rajatta koska maxLines on 6,
            // joten vain saapuva viesti tarvitsee vierityksen.
            Column(
                modifier = if (messageScrolls) {
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = chat.opponent?.let { stringResource(R.string.board_chat_with, it) }
                        ?: stringResource(R.string.board_chat_title),
                    style = MaterialTheme.typography.titleSmall,
                )

                // Saapunut viesti sellaisenaan. Null on mahdollinen vain avatulla kentällä:
                // ilman viestiä ja ilman avausta korttia ei piirretä lainkaan (ks. kutsuja).
                chat.incoming?.let { incoming ->
                    Text(text = incoming, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Kenttä, fraasit ja napit yhtenä ryhmänä näppäimistön yläpuolelle
            // (`keepInViewWhileTyping`). Tarpeen päättymisruudulla, joka vierii itse: Pixel 8a:lla
            // fraasirivi näkyi mutta lähetysnappi jäi näppäimistön alle (pelisessio 3.10.2026).
            Column(
                modifier = Modifier.keepInViewWhileTyping(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (composerOpen && form != null) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6,
                        label = { Text(stringResource(R.string.board_chat_label)) },
                    )

                    // Sama rivi kuin viestiruudussa ja sama lista (3.9.2026). Nappi täyttää
                    // kentän eikä lähetä: lähetys on yhä alla sivun oma nappi.
                    PhraseRow(
                        phrases = phrases,
                        draft = draft,
                        enabled = true,
                        onDraftChange = { draft = it },
                        onAddPhrase = onAddPhrase,
                        onRemovePhrase = onRemovePhrase,
                    )

                    // `if` eikä `let`: Compose ei salli piirtoa `let`in lambdassa, ja ehto on
                    // tässä sivun tosiasia. Ruudutonta lomaketta ei voi lainata, ja silloin
                    // valintaa ei myöskään näytetä.
                    if (form.quoteField != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = quote, onCheckedChange = { quote = it })
                            Text(
                                text = stringResource(R.string.board_chat_quote),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.board_chat_send_explain),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Ilman lomaketta ei ole Replyta eikä nappeja: sivun omat napit piirtää silloin
                // päättymisruutu `page.form`ista (7.9.2026, [ChatOnBoard.form]).
                if (form != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reply avaa kentän vasta pyydettäessä. Sivun napit toimivat suljetullakin
                    // kentällä: tyhjä teksti on poistuminen ilman viestiä kuten selaimessa
                    // (`ChatForm.write`), eikä sitä kirjata arkistoon.
                    if (!composerOpen) {
                        OutlinedButton(onClick = onOpenComposer) {
                            Text(stringResource(R.string.board_chat_reply))
                        }
                    }
                    // Sulkeminen ennen sivun nappeja: se on sovelluksen oma lisä eikä sivun
                    // teko, joten se on tekstinappi (sääntö 25.8.2026) ja sivun napit tulevat
                    // sen jälkeen samassa järjestyksessä kuin ennen.
                    if (composerOpen && onCloseComposer != null) {
                        TextButton(onClick = onCloseComposer) {
                            Text(stringResource(R.string.board_chat_close))
                        }
                    }
                    form.submits.forEach { label ->
                        Button(
                            // Aktiivinen myös tyhjällä kentällä (Tommin päätös 27.8.2026):
                            // selaimessa tyhjä kenttä ja `To Top` tarkoittaa poistumista ilman
                            // viestiä, eikä sovellus saa olla tiukempi kuin kohde. Tyhjän
                            // viestin arkistoinnin estää `BoardViewModel.sendChat`.
                            onClick = { onSendChat(draft, quote, label) },
                        ) {
                            // Painallus vie sivun oman `label`in, ruutu näyttää [submitText]in.
                            Text(submitText(label))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sivun napin näyttöteksti: sivun oma sana sellaisenaan. ~~`To Top` käännettiin muotoon
 * `To Matches` (1.9.2026).~~ Purettu 22.9.2026, kun välilehti sai sivuston nimen `Top Page`;
 * ks. `docs/UI.md` › Ensimmäinen välilehti on Top Page. Funktio jäi, jotta molemmat
 * piirtokohdat (nappirivi ja chat-kortti) lukevat tekstin yhä samasta paikasta.
 */
private fun submitText(label: String): String = label

@Composable
private fun NotABoardKind.text(): String = when (this) {
    is NotABoardKind.TopPage -> stringResource(R.string.board_top_page)
    NotABoardKind.WrongPage -> stringResource(R.string.board_wrong_page)
    is NotABoardKind.InboxItem -> stringResource(
        if (saved) R.string.board_inbox_item_saved else R.string.board_inbox_item_unsaved,
        message.sender,
        message.body,
    )
    is NotABoardKind.Invitation -> stringResource(R.string.board_invitation, from)
    NotABoardKind.BoardUnreadable -> stringResource(R.string.board_unreadable)
    is NotABoardKind.DifferentMatch ->
        stringResource(R.string.board_different_match, actual.value)

    NotABoardKind.MiniScheme -> stringResource(R.string.board_mini_scheme)
    NotABoardKind.PositionUnreadable -> stringResource(R.string.board_position_unreadable)
    NotABoardKind.PathNotReadOnly -> stringResource(R.string.board_path_not_read_only)
}

/**
 * Laudan ja sivupaneelin rivi kantaa [Modifier.weight]iä, ja **lauta täyttää sen mitä
 * jää jäljelle**. Aiemmin rivi keskitti kiinteämittaisen laudan pystysuunnassa, mikä
 * toimi vain sillä tabletilla jolle mitat oli skaalattu; nyt suunta on päinvastainen,
 * eli lauta saa mitattavan tilan ja johtaa mittansa siitä. Yläosan tekstirivit (otsikko,
 * peruutusilmoitus) ja alaosan tekstirivit (viesti, ilmoitukset, pip-vahti) eivät kanna
 * painoa, koska niiden korkeus on sisällön mittainen eikä venytettävä, ja juuri siksi
 * niitä ei tarvitse arvata laudan puolella.
 */
@Composable
private fun LoadedBoard(
    state: BoardUiState.Loaded,
    style: BoardStyle,
    siteSettings: SiteBoardSettings,
    diceStyle: DiceStyle,
    scoreStyle: ScoreStyle,
    /** Ks. [diceTapFor]: kaksi kytkintä, sääntö on funktiossa. */
    diceSubmitTap: Boolean,
    diceSwapTap: Boolean,
    /** Ks. [diceRollTapFor]. */
    diceRollTap: Boolean,
    /** Ks. `BoardExtrasStore`: sovelluksen omat lisälinkit paneelissa. */
    boardExtras: Boolean,
    /** Piirretäänkö kootun siirron nuolet, ks. [MoveArrowLayer]. */
    moveArrows: Boolean,
    /** Syttyykö painetun pisteen numero, ks. [PointPress]. */
    pointPress: Boolean,
    /** Ohi-napautuksen ääni, ks. [BoardTouch.onMiss]. Soitin päättää soiko se. */
    onPointMiss: () -> Unit,
    /** Ks. [cubeActionFor]: laitteen kytkin joka lisää beaverille dialogin. */
    confirmBeaver: Boolean,
    /** Tosi kun siirtokäsi on vasen, jolloin sivupaneeli on oikealla. Ks. kutsukohta. */
    leftHanded: Boolean,
    onRefresh: () -> Unit,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    /** Sivun avaus lukunäkymään, ks. [PlayerPanelView]: pelaajakortti avaa profiilin. */
    onOpenPage: (String) -> Unit,
    onSendChat: (String, Boolean, String) -> Unit,
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
    reminders: List<Reminder>,
    onAddReminder: (String) -> Unit,
    onRemoveReminder: (Long) -> Unit,
    onWritingChange: (Boolean) -> Unit,
    onMarkPosition: (String) -> Unit,
) {
    // Katkotilan nappi keskikaistalle, ks. [UnconfirmedRefresh]. Ehto on tässä eikä
    // laudassa: lauta saa valmiin teon tai ei mitään, kuten noppien painalluksessa. Syy
    // kulkee mukana samasta syystä, eli lauta ei lue tilamallia vaan saa valmiin tiedon.
    val unconfirmedRefresh = state.unconfirmed?.let { UnconfirmedRefreshSpec(it, onRefresh) }

    // Paikallisen kokoamisen aikana piirretään session lauta eikä sivun: poimitut askeleet
    // näkyvät heti ja tarjolla ovat session synteettiset local:-osoitteet, jotka
    // näkymämalli sieppaa tilamuutoksiksi. Ilman sessiota tämä on täsmälleen sivun lauta.
    // Avausheitto sivun laudalle tässä ja kootulle laudalle `boardNow`issa: pelin
    // ensimmäisellä sivulla vastustajan noppa piirtyy hänen puolelleen, ks.
    // [BoardState.openingRoll] ja `docs/UI.md` › Avausheitto.
    val board = state.composition?.boardNow(siteDice = diceStyle == DiceStyle.SITE)
        ?: state.board.withOpeningDice()

    // Pisteiden esitys yhtenä johdoksena molemmille kodeille (paneeli ja lokerosarake):
    // null away tarkoittaa scoreTextissä sivun omaa pistekenttää, joten SITE-valinta on
    // sama asia kuin away jota ei ole. Rahapelin varareitti ei siis muutu mistään.
    val awayOf: (PlayerPanel) -> Int? = when (scoreStyle) {
        ScoreStyle.AWAY -> state.board::awayOf
        // Nimen perässä on sivun oma luku, joten away ei ole käytössä (9.10.2026).
        ScoreStyle.SITE, ScoreStyle.NAME -> { _ -> null }
    }

    // Kosketus syntyy laudan omista linkeistä, ja se on tyhjä silloin kun sivu ei tarjoa
    // yhtään siirtoa. Silloin lauta on täsmälleen sama kuin ennen porttia: katsottava kuva.
    // Painettu piste elää tässä eikä sarakkeessa, koska sen kaksi puolta ovat eri riveillä:
    // kosketus tulee kiilalta ([PointWedge]) ja näkyvä merkki numeroriviltä ([NumberGroup]).
    val press = remember { PointPress() }
    LaunchedEffect(press.number, press.down) {
        if (press.number != null && !press.down) {
            delay(POINT_PRESS_LINGER_MS)
            press.number = null
        }
    }
    val touch = BoardTouch(
        movable = board.moves.associate { it.fromPoint to it.href },
        onFollow = onFollow,
        press = if (pointPress) press else null,
        onMiss = if (pointPress && board.moves.isNotEmpty()) onPointMiss else null,
    )

    // **Muistutusten kirjoitustila asuu tässä, koska sen kaksi puolta ovat eri paikoissa
    // 24.8.2026 alkaen.** Avaava nappi on laudan toimintorivillä ([ReminderActions]) ja
    // kenttä laudan alla ([ReminderStrip]). Jos tila jäisi kentän puolelle, nappi ei voisi
    // avata sitä, ja jos se jäisi laudan puolelle, kenttä ei näkisi sitä.
    var composing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    // Merkin kirjoitustila samasta syystä samassa paikassa. Kaksi kenttää eivät ole auki
    // yhtä aikaa, koska laudan alla on tilaa yhdelle riville kerrallaan.
    var marking by remember { mutableStateOf(false) }
    var markDraft by remember { mutableStateOf("") }
    // Chat-kortin näkyvyys (Tommin havainto ja kuittaus 27.8.2026): sivu tarjoaa
    // chat-kentän jokaisella siirron jälkeisellä sivulla myös tyhjänä, ja korttina se vei
    // puhelimen vaakaruudulta ison siivun joka siirrolla. Kortti piirretään siksi vain kun
    // sillä on jotain näytettävää: saapunut viesti, tai käyttäjän itse avaama kenttä.
    // Avain on lomakeosoite kuten kortin luonnoksellakin, joten tila ei vuoda sivulta
    // toiselle.
    val chat = state.chat
    var chatComposer by remember(chat?.form?.action) { mutableStateOf(false) }
    val chatCardVisible = chat != null && (chat.incoming != null || chatComposer)
    // Kirjoitustila ylös kutsujalle, joka omistaa suunnan. Poistuessa epätosi, ettei
    // seuraava lauta avaudu pystyyn tilasta joka jäi tänne. Chat-kenttä kuuluu joukkoon
    // 21.9.2026 alkaen (Tommin tilaus: *"Message tulisi toimia samoin kuin Mark position,
    // eli näyttö pystyyn ja lauta on kuva"*): sama näppäimistö vie saman puolikkaan, ja
    // kenttä on kortissa laudan yläpuolella. Tila sulkeutuu sivun vaihtuessa, koska viesti
    // lähtee kortin oman napin (`Next Game`, `To Top`) mukana.
    val writing = composing || marking || chatComposer
    LaunchedEffect(writing) { onWritingChange(writing) }
    DisposableEffect(Unit) { onDispose { onWritingChange(false) } }

    // Peli luetaan laudalta eikä välitetä erikseen: sama johdos kuin näkymämallissa, ja
    // `null` tarkoittaa samaa molemmissa. Ruutu ei siis tarjoa kenttää siihen mitä
    // näkymämalli ei ottaisi vastaan.
    val gameKnown = board.gameKey != null
    val cubeText = stringResource(R.string.board_reminder_cube_text)

    // **Kuutiomuistutus tunnistetaan samasta merkkijonosta jolla se kirjoitetaan**
    // (`onToggleCube` alla). Muistutus on vapaata tekstiä eikä kanna lajia, joten yhteys on
    // resurssi: yksi omistaja, kaksi käyttöä. Jos muistutuksille joskus tulee laji, tämä on
    // se kohta joka vaihtuu, eikä ehto ole silloin enää tekstissä.
    //
    // Korostuksen syy on Tommin havainto 31.8.2026: alalaidan muistutusrivi jää huomaamatta
    // kesken pelin, koska katse on laudalla. Kuutio on sekä katseen alueella että jo valmiiksi
    // se mitä painetaan.
    //
    // **Korostus on ainoa merkki 14.9.2026 alkaen** (Tommin päätös, `docs/UI.md`): rivi
    // *Think about doubling* ei piirry laudan alle, joten lauta ei kapene sen verran, ja
    // `Cube reminder` -linkki on kytkin joka myös poistaa muistutuksen. Muut muistutukset
    // näkyvät laudan alla kuten ennen.
    val cubeReminder = reminders.firstOrNull { it.text == cubeText }
    val cubeReminderSet = cubeReminder != null
    val visibleReminders = reminders.filter { it.text != cubeText }

    // **Vastustajan omistama kuutio ei kanna muistutusta** (Tommin havainto ja päätös
    // 15.9.2026 päivällä, eino-ottelu: laserkehys ja `Pick me` vastustajan kortissa vaikka
    // tuplata ei voi). Kaksi osaa, ja molemmat ovat Tommin: korostus piirtyy vain kun
    // kuutio on oma tai keskellä, ja itse muistutus poistuu kun vastustaja ottaa kuution.
    // Poisto on täällä eikä näkymämallissa, koska muistutus tunnistetaan resurssin
    // tekstistä (yllä), jota näkymämalli ei tunne. `UNKNOWN` ei sammuta eikä poista, koska
    // se on lukuvirhe eikä tieto omistajasta.
    val cubeOpponents = board.cube?.position == CubePosition.TOP
    val cubeReminderShown = cubeReminderSet && !cubeOpponents
    LaunchedEffect(cubeOpponents, cubeReminder?.id) {
        if (cubeOpponents && cubeReminder != null) onRemoveReminder(cubeReminder.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalPanelLook.current.background)
            // Näppäimistö kaventaa saraketta eikä peitä sitä: ilman tätä chat-kentän
            // avaaminen jättäisi nappirivin näppäimistön alle laitteilla joilla ikkuna
            // ei itse kutistu. Kun ikkuna kutistuu, ime-inset on nolla ja tämä ei tee mitään.
            .imePadding()
            // Koko ruutu ottaa vedon vastaan eikä vain lauta: ele ei näy ruudulla, joten
            // sen kohtaa ei voi arvata, ja väärään kohtaan osunut veto tuntuisi rikkinäiseltä.
            .pullDownToRefresh(onRefresh),
    ) {
        // Latauspalkkia ei ole laudalla 16.9.2026 alkaen (Tommin päätös kuuden ottelun
        // session jälkeen: *"latauskaari tekee ylimpänä olevan latausviivan
        // tarpeettomaksi"*). Odotus näkyy nappien paikalla kaarena ([BusyArc]), joka on
        // katseen kohdassa; neljän dp:n viiva yläreunassa ei ollut. Muilla ruuduilla
        // `ProgressSlot` on ennallaan, koska niillä ei ole kaarta. Lauta sai samalla
        // neljä dp korkeutta takaisin.

        // Keskustelu on laudan yläpuolella eikä alla, ja se on tietoinen järjestys:
        // vastustajan viesti näkyy vain tällä sivulla eikä sivusto säilytä sitä, joten se on
        // ruudun tärkein sisältö sillä hetkellä kun se on olemassa. Lauta antaa tilaa, koska
        // se on painollinen laatikko ja tämä ei. Näkyvyysehto on ylempänä: tyhjä lomake ei
        // piirrä korttia.
        //
        // Tässä oli lisäksi `&& chat != null`, joka oli K1-kääntäjän pakko eikä logiikkaa:
        // K2 kuljettaa null-tiedon `chatCardVisible`n läpi, ja ehto muuttui varoitukseksi
        // "aina tosi". Poistettu 31.8.2026.
        if (chatCardVisible) {
            // **Katto luetaan mittauksesta eikä painotuksesta, ja se on 1.9.2026 tehty
            // korjaus.** Kortilla oli siihen asti `weight(1f, fill = false)`, joka varasi
            // sille puolet korkeudesta riippumatta siitä tarvitsiko se sen: sarakkeen
            // painotus jakaa tilan ennen mittausta, eikä käyttämätön osa palaudu muille.
            // Yhden rivin viesti maksoi laudalle siis saman kuin kymmenen rivin, ja loppu
            // jäi tyhjäksi kaistaksi alareunaan (Tommin havainto laiteajossa: *"lauta
            // kutistuu häiritsevästi"*). `BoxWithConstraints` antaa saman katon vasta
            // mittauksessa, jolloin lyhyt kortti vie oman korkeutensa ja lauta saa lopun.
            //
            // Painottamaton lapsi saa sarakkeessa jäljellä olevan korkeuden ylärajakseen,
            // joten `maxHeight` on tässä juuri se tila jota kortti ja lauta jakavat. Se on
            // tarkempi luku kuin ruudun korkeus, koska edeltävä edistymispalkki on jo
            // vähennetty.
            BoxWithConstraints {
                ChatCard(
                    chat = chat,
                    composerOpen = chatComposer,
                    onOpenComposer = { chatComposer = true },
                    onCloseComposer = { chatComposer = false },
                    onSendChat = onSendChat,
                    phrases = phrases,
                    onAddPhrase = onAddPhrase,
                    onRemovePhrase = onRemovePhrase,
                    // Avattu kirjoituskortti voittaa laudan, suljettu ottaa oman
                    // korkeutensa. Suljettuna katto on puolet jaettavasta tilasta, ettei
                    // pitkä saapunut viesti vie lautaa ruudulta; viestiosa vierii kortin
                    // sisällä, joten katto ei piilota mitään lopullisesti. Avattuna kattoa
                    // ei ole: kortti mitoitetaan ensin ja lauta saa jäännöksen, koska
                    // kenttä johon ei pääse kirjoittamaan on pahempi kuin lauta jota ei
                    // hetkeen näe (Tommin havainto 28.8.2026: kenttä avautui näkyvän
                    // alueen alapuolelle).
                    modifier = if (chatComposer) {
                        Modifier
                    } else {
                        Modifier.heightIn(max = maxHeight / 2)
                    },
                )
            }
        }

        // Onko sivun tilarivien lokero keskikaistalla; luetaan laudan alla, jotta
        // toistojono ei piirry kahdesti. Asetetaan lohkon sisällä, luetaan sen jälkeen.
        //
        // **Tila eikä paikallinen muuttuja** (mitattu 14.9.2026 klo 23.14, Rami: `Pending
        // Replay: 1` sekä kaistalla että laudan alla). `BoxWithConstraints` koostaa
        // sisältönsä vasta mittauksessa, eli sen jälkeen kun tämän sarakkeen loput on jo
        // koostettu, joten paikallinen muuttuja luettiin aina epätotena. Tilan kirjoitus
        // koostaa lukijan uudelleen, ja rivi katoaa laudan alta samassa kehyksessä.
        var notesOnStrip by remember { mutableStateOf(false) }
        val portraitWindow = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            // **Laudan leveys lasketaan ennen piirtoa**, jotta kehys kutistuu sisältönsä
            // levyiseksi eikä vie koko tarjottua leveyttä. Ylijäävä leveys jää
            // marginaaliksi laudan ympärille, koska lauta ei kasva leveydestä vaan
            // korkeudesta.
            //
            // **Koko leveys on laudan 9.8.2026 alkaen.** Tässä vähennettiin siihen asti
            // sivupaneelin 200 dp ja yksi väli, ja se oli Pixel 8a:lla ilmainen mutta
            // pienellä puhelimella kallis: siellä leveys on rajoittava mitta, ja sama
            // varaus kutisti nappulan 24,1:stä 18,8 dp:hen. Paneelin sisältö on nyt
            // päällepiirtona samassa pyyhkäisyssä kuin otsikko, ks. [TransientInfo].
            val frameWidth = DgBoard.frameWidth(
                availableWidth = maxWidth,
                availableHeight = maxHeight,
                numberRowHeight = numberRowHeight(),
            )
            // Roolit ja paneeli luetaan sivun laudasta eikä session laudasta: pip-vahti
            // vertaa paneelin lukuihin jotka kuvaavat lähtöasemaa, ja kesken kootun aseman
            // vertailu sanoisi "ei täsmää" jokaisesta poimitusta askeleesta.
            //
            // Skeema luetaan samasta syystä sivun laudasta: se kertoo mitkä kuvat juuri
            // tällä sivulla olivat. Säilötty asetus on BoardLookin varasuunta, ei tämä.
            val look = BoardLook.of(style, siteSettings, state.board.scheme, decoLight = decoLightBoard())
            val roles = BoardRoles(selfCheckerColor(state.board, state.pips), look)

            // **Paneeli saa vain ylijäävän leveyden, ei varattua.** Lauta mitoitetaan ensin,
            // ja paneeli ilmestyy vain jos sen jälkeen jää tarpeeksi. Kiinteä varaus oli syy
            // sen poistoon 9.8.2026: se oli Pixel 8a:lla ilmainen mutta kutisti pienellä
            // puhelimella nappulan 24,1:stä 18,8 dp:hen. Nyt sama koodi tekee kummallakin
            // oikein ilman laitetuntemusta, koska ehto on mitattu leveys eikä laite.
            val spare = maxWidth - frameWidth - SIDE_PANEL_GAP
            val panelWidth = spare.coerceAtMost(SIDE_PANEL_MAX)

            // Kirjoitustilassa (19.9.2026, ruutu pystyssä) lauta on kuva ja saa vain sen
            // korkeuden jonka leveys antaa; koko ruutu venyttäisi kiilat. Ehto on tila eikä
            // mittasuhde: näppäimistön kanssa laatikko on lähes neliö, ja mittasuhteesta
            // luettu ehto jäi epätodeksi (mitattu tabletilla klo 18.34). Luetaan tässä,
            // koska rivin sisällä `maxWidth` olisi jo toisen mittauksen.
            //
            // **Pystyssä pelaaminen käyttää samaa laudan mittaa** (Tommin päätös 23.9.2026,
            // kytkin `BoardRotationStore`, `docs/UI.md` › Lauta pystyssä). Ehto on ikkunan
            // suunta eikä tämän laatikon mittasuhde samasta syystä: chat-kortin kanssa
            // laatikko on puhelimella lähes neliö.
            val portraitBoardHeight: Dp? = if (writing || portraitWindow) {
                minOf(maxHeight, DgBoard.frameHeight(frameWidth, numberRowHeight()))
            } else {
                null
            }
            // **Pystyssä pelatessa paneeli on laudan alla** (Tommin ohje 23.9.2026: *"älä
            // ahda toimintoja laudalle vaan käytä pystynäytön korkeutta"*). Sivulle ei jää
            // leveyttä, mutta alle jää korkeutta, joten paneeli on sama kuin vaakatabletilla
            // ja keskikaista jää pelkille nopille. Kirjoitustilassa ei, koska näppäimistö
            // vie sen korkeuden.
            val panelBelow = portraitWindow && !writing && portraitBoardHeight != null
            val panelBelowHeight = if (panelBelow) maxHeight - portraitBoardHeight!! - SIDE_PANEL_GAP else 0.dp

            // **Päätös ei saa muuttaa sitä mistä se luetaan.** Tämä kirjoitettiin 10.8.2026
            // asti `SideEffect`illä ylös, ja laudan alle ilmestynyt toimintorivi vei laudalta
            // korkeutta. Lauta kutistuu korkeudesta, joten kapeampi lauta jätti taas tilaa
            // paneelille, ja päätös kumosi itsensä joka ruudulla: 294 värähtelevää ruutua
            // viidessä sekunnissa tyhjäkäynnillä, kaikki hitaita UI-säikeessä (Galaxy Tab S7+
            // 10.8.2026). Laukaisija oli odottavan teon palkki, joka siirsi asettelun tämän
            // rajan tuntumaan, mutta sama olisi tullut mistä tahansa laudan yläpuolisesta
            // palkista. Arvo pysyy nyt siinä mittauksessa jossa se syntyy.
            val fits = panelBelow || panelWidth >= SIDE_PANEL_MIN

            // **Napit asuvat 14.8.2026 alkaen aina [MiddleStrip]issä**, riippumatta
            // näytön leveydestä. Aiemmin niiden koti vaihtui kolmen ehdon mukaan
            // (sivupaneeli, lokerosarake, alareunan päällepiirto), ja jokainen ehto oli
            // oma leveysmittauksensa. Tommin päätös 14.8.2026: yksi koti kaikilla
            // näytöillä on parempi kuin kolme ylläpidettävää haaraa, ja MiddleStrip on
            // aina näkyvissä laudan täydellä leveydellä riippumatta paneelin tai
            // sarakkeen tilasta, joten se ei tarvitse omaa mahtumisehtoa.
            //
            // **Pelaajien nimet, pisteet ja pipit tarvitsevat silti kodin ilman
            // paneelia**, ja se on ainoa syy joka jäi jäljelle sarakkeen leveysehdolle.
            val showTrayFacts = !fits && DgBoard.trayWidth(frameWidth) >= DgBoard.TRAY_TEXT_MIN

            // Muistutusnapit koottuna kerran, koska niillä on kaksi mahdollista kotia ja
            // vain toinen on kerrallaan käytössä (27.8.2026, `docs/UI.md`). Ne ovat
            // sivupaneelissa kun paneeli on näkyvissä; ilman paneelia ne putoavat
            // toimintoriville kuten ennen tätä muutosta.
            //
            // **Uutta leveysehtoa ei synny**, ja se on koko ratkaisun ehto: haara lukee
            // saman `fits`-arvon joka jo päättää paneelin olemassaolon. Toinen kynnys olisi
            // toinen ylläpidettävä mitta, ja juuri se purettiin 14.8.2026.
            // **Ahdas paneeli** (16.9.2026): Pixel 8a:n nauhalla `Skip Game` piirtyi
            // vastustajan pippien päälle, koska keskilohko sai 180 dp ja tarvitsi 216.
            // Material-tekstinapin vähimmäiskorkeus on 40 dp, ja lisiä on paneelissa
            // enimmillään neljä; ahtaassa paneelissa ne madalletaan ja kortit luopuvat
            // tyhjästä rivistään. Ehto on mitta eikä laite, ks. [DgBoard.PANEL_COMPACT_BELOW].
            // Laudan alla paneelin korkeus on se mitä laudalta jää, ei koko laatikko.
            val compactPanel = if (panelBelow) {
                panelBelowHeight < DgBoard.PORTRAIT_PANEL_COMPACT_BELOW
            } else {
                maxHeight < DgBoard.PANEL_COMPACT_BELOW
            }
            // Sama ehto kuin lohkon sisällä: `Chat` (`Message` 21.9.2026 asti), `Skip Game`
            // tai muistutukset.
            // Vain viivaa varten, ks. [SidePanel].
            val panelActionsPresent =
                (chat != null && !chatCardVisible) || board.skipHref != null || (gameKnown && boardExtras)
            // **Ahtaan paneelin rivitys luetaan lukumäärästä** (Tommin sääntö 21.9.2026
            // kännykän kaappauksen jälkeen: *"jos sivupaneelin yläosassa on pariton määrä
            // toimintoja, niin anna Skip Game olla omalla rivillään"*). `FlowRow` rivitti
            // `Skip Game`n ja ensimmäisen linkin yhteen ja jätti viimeisen linkin yksin
            // toiselle riville. Nyt sivun napit (`Chat`, `Skip Game`) saavat oman rivin
            // kun niitä on kaksi tai kun toimintojen kokonaismäärä on pariton, jolloin
            // linkit jäävät pareiksi. Parillinen määrä yhdellä napilla latoo napin ja
            // ensimmäisen linkin (`Mark position`) samalle riville kuten ennen.
            val messageVisible = chat != null && !chatCardVisible
            // Kuutionappi piilotetaan kun tuplaus ei voi olla oma teko:
            // Crawford-peli tai kuutio vastustajan solussa (ylin, kartta
            // mitattu 9.8.2026). Kaikki kolme haaraa todennettu laitteella
            // 24.8.2026, ks. docs/AVOIMET.md.
            //
            // Neljäs haara 9.9.2026: yhden pisteen ottelussa kuutiota ei ole
            // lainkaan (`SUBSTANSSI.md` kohta 7), joten muistutus on siellä
            // väärässä eikä vain turha. Mitattu pelisessiossa `sessio-9-9-yo4`,
            // jossa linkki näkyi ottelussa `1 point match` vaikka sivuston omilla
            // sivuilla ei ollut `submit=Double`-linkkiä eikä `verify`-kenttää.
            // Tuntematon pituus (`null`) ei piilota, koska se on lukuvirhe eikä
            // tieto yhden pisteen ottelusta.
            val cubeVisible = board.crawford != CrawfordReading.CRAWFORD &&
                board.cube?.position != CubePosition.TOP &&
                board.matchLength != 1
            val showMark = board.moveNumber != null
            // Lisälinkit voi piilottaa (testaajan toive 9.10.2026, `BoardExtrasStore`). Ehto on
            // sama kuin pelin tunnistus, joten piilotettu ja tunnistamaton peli näyttävät samalta:
            // sivun napit (`Message`, `Skip Game`) jäävät, linkit lähtevät.
            val extrasShown = gameKnown && boardExtras
            // `Skip Game` oli pystylaudalla `Roll Dicen` alla 24.–26.9.2026; nyt se on aina
            // tässä joukossa (Tommin valinta A 26.9.2026, `docs/UI.md` › Pystylaudan lisät).
            val buttonCount = (if (messageVisible) 1 else 0) +
                (if (board.skipHref != null) 1 else 0)
            val linkCount = if (extrasShown) 1 + (if (cubeVisible) 1 else 0) + (if (showMark) 1 else 0) else 0
            val buttonsOwnRow = compactPanel && buttonCount > 0 &&
                (buttonCount == 2 || (buttonCount + linkCount) % 2 == 1)
            // Viiden toiminnon tila (`Next Game` -sivu: `Chat`, `Skip Game` ja kolme
            // linkkiä) latoi `Cube reminder`in yksin alimmalle riville. Tommin tarkennus
            // 21.9.2026: *"Reminders ja Cube Reminder samalle riville"*, joten `Mark
            // position` saa silloin oman rivin nappirivin alle. Kolme linkkiä eivät mahdu
            // yhdelle 238 dp:n riville, joten yksin jää joka tapauksessa yksi.
            val markOwnRow = buttonsOwnRow && linkCount == 3
            // **`Chat` ja `Skip Game` ovat aina samalla rivillä, myös väljässä paneelissa**
            // (Tommin tilaus 21.9.2026: *"jos se esiintyy ruudulla yhdessä Skip Game -napin
            // kanssa, niin haluaisin ne aina samalle riville"*). Väljä paneeli latoo
            // `actions()`-lohkon `Column`iin, joten ilman omaa riviä kaksi nappia olisivat
            // allekkain. Rivi on sisältönsä levyinen, jotta se keskittyy sarakkeessa ja
            // pysyy vaakasuorana myös kaistalla (`middleTrailing`), joka on itse rivi.
            val buttonsPaired = !compactPanel && buttonCount == 2
            val siteButtons: @Composable () -> Unit = {
                // Keskustelun aloitus pyydettäessä (Tommin kuittaus 27.8.2026): kun sivulla
                // on chat-kenttä muttei viestiä, kortti ei piirry itsestään vaan avataan
                // tästä. Samassa joukossa kuin muistutukset, koska hetki on sama: siirron
                // jälkeen mietitään mitä vastustajalle sanotaan.
                //
                // **Nappi eikä tekstilinkki 1.9.2026 alkaen**, ja peruste on jo kirjattu
                // sääntö eikä uusi päätös: sivun oma teko näyttää napilta, sovelluksen oma
                // lisä tekstiltä, ja ehto luetaan lähteestä eikä sijainnista (25.8.2026,
                // `docs/UI.md`). `LinkText`inä tämä oli samannäköinen kuin `Reminders` ja
                // `Cube reminder`, jotka jäävät laitteelle, vaikka sen takana on sivuston
                // oma chat-lomake ja sen POST. Tommi 29.8.2026: *"vei hetken löytää Messages
                // eikä se ollut nappi"*, eli mitattu vaiva oli juuri tämä.
                //
                // Väri on sivun muiden komentojen [TextSecondary] eikä perumisen
                // [CheckerSelf]: tämä ei ole painavin teko rivillä, ja avaaminen itsessään
                // ei lähetä mitään.
                if (messageVisible) {
                    OutlinedButton(
                        onClick = { chatComposer = true },
                        // Sama korkeus kuin `Skip Game`lla ahtaassa paneelissa (Tommin
                        // kaappaus 16.9.2026: *"message ja skip game napeille sama korkeus"*).
                        modifier = (if (compactPanel) Modifier.heightIn(min = DgBoard.COMPACT_BUTTON_HEIGHT) else Modifier).decoButtonLine(),
                        shape = boardButtonShape(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LocalPanelLook.current.secondary,
                        ),
                        border = BorderStroke(DgBoard.OUTLINE, LocalPanelLook.current.secondary),
                    ) {
                        Text(
                            text = stringResource(R.string.board_chat_open),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                // `Skip Game` on sivuston oma komento mutta se asuu tässä joukossa
                // (Tommin päätös 27.8.2026, `docs/UI.md`). Peruste on hetki eikä
                // lähde: hän painaa sitä silloin kun haluaa aikaa miettiä mitä
                // kirjoittaa vastustajalle, eli samassa hetkessä kuin muistutuksen.
                //
                // **Se pysyy reunustettuna nappina muistutusten ollessa tekstiä**, koska
                // se kuluttaa jonon eikä sitä voi perua, kun taas muistutuksen voi
                // kirjoittaa uudestaan. Ero luetaan siis yhä lähteestä eikä sijainnista
                // (25.8.2026), ja se on sama merkki kuin toimintorivillä.
                //
                // **Se on sivun komennoista viimeinen tässäkin joukossa**, eli `Chat`in
                // jälkeen: sama järjestys kuin toimintorivillä, ja samasta syystä. Skip on
                // ainoa näistä jonka jälkeen ruudulla on eri peli.
                board.skipHref?.let { href -> SkipGameAction(href, onFollow, compact = compactPanel && !LocalRoomyPortrait.current) }
            }
            // Pystylaudalla sivun napit kulkevat `Mark position`in parina, ks. [ReminderActions].
            val pairRows = panelBelow && extrasShown
            val reminderActions: @Composable () -> Unit = {
                if (pairRows) {
                    // Sivun napit piirtää [ReminderActions] parin alkuun.
                } else if (buttonsOwnRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        siteButtons()
                    }
                } else if (buttonsPaired) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        siteButtons()
                    }
                } else {
                    siteButtons()
                }

                if (extrasShown) {
                    ReminderActions(
                        onToggleComposing = {
                            composing = !composing
                            if (composing) marking = false
                        },
                        onToggleCube = {
                            if (cubeReminder != null) onRemoveReminder(cubeReminder.id)
                            else onAddReminder(cubeText)
                        },
                        showCube = cubeVisible,
                        // Merkki tarvitsee siirtonumeron, koska se on se sana jolla asema
                        // löytyy analyysiohjelmasta. Ilman numeroa linkkiä ei tarjota,
                        // samoin kuin kenttää ei tarjota ilman pelin tunnistetta.
                        onToggleMarking = {
                            marking = !marking
                            if (marking) composing = false
                        },
                        showMark = showMark,
                        // Laudan alla linkit ovat rivissä kuten ahtaassa paneelissa: leveyttä on, ja
                        // koko rivin levyiset linkit pinoutuivat Pixelillä ruudun alle
                        // (kaappaus 23.9.2026).
                        compact = compactPanel || panelBelow,
                        markOwnRow = markOwnRow && !pairRows,
                        pairRows = pairRows,
                        leading = siteButtons,
                    )
                }
            }

            // **Huomautukset ovat laudan päällä eivätkä sen yläpuolella** (Tommin pyyntö
            // 31.8.2026). Palkki vei korkeutta, ja lauta johtaa leveytensä korkeudesta:
            // sama palkki tuotti 10.8.2026 mitatun värähtelyn, koska kutistunut lauta
            // vapautti leveyttä sivupaneelille ja käänsi paneelin mahtumisehdon. Päällepiirto
            // ei kosketa asettelua lainkaan, joten koko vikaluokka jää pois.

            // **Arvatusta asemasta ei sanota ruudulla mitään (Tommin päätös 1.9.2026).**
            // Ilmoitus oli ensin kolmen virkkeen kortti kuittausnappeineen, sitten kahden
            // sanan merkintä, ja molemmat kaatuivat samaan havaintoon: *"toistuu tarpeettoman
            // usein, DailyGammon-pelaaja tietää ilmiön"*. Sivusto arvaa vastustajan siirtoja
            // lähes joka siirrolla, joten kyse on kohteen tavallisimmasta tilasta, ja
            // sovellus selitti sen kohdeyleisölle jolle se on ennestään tuttu.
            //
            // Lippu jäsennetään yhä (`BoardState.speculative`), koska malli kantaa sen mitä
            // sivu sanoi. Näkymä vain ei piirrä siitä mitään, ja se on juuri se työnjako jota
            // mallin ja näkymän välillä muutenkin noudatetaan.
            //
            // Peruutus oli kortti kuittausnappeineen 3.9.2026 asti sillä perusteella että se
            // on harvinainen tapahtuma. Mittaus kaatoi perusteen: 3.9. illan sessiossa se
            // tuli seitsemällä laudalla 44:stä ja koko korpuksessa 44 sivulla, ja Tommi
            // sanoi kortin häiritsevän. Se on nyt rivi sivupaneelissa tilannetekstin
            // yläpuolella, ilman kuittausta ([SidePanel]), samasta syystä kuin arvattu
            // asema yllä: ilmiö on pelaajalle tuttu, ja kortti väitti joka kerta että jotain
            // on sattunut.

            // Kuutiotekojen vahvistus asuu tässä eikä laudassa 2.9.2026 alkaen, koska sama
            // nappi voi olla paneelipinossa laudan ulkopuolella. Sääntö on [cubeActionFor]
            // (Tommin päätös 3.9.2026): sivun oma rasti on pakollinen, ja dialogi tulee
            // vasta sen jälkeen. Rasti on ruudun tasolla samasta syystä kuin dialogi: sama
            // teko lähtee paneelista, keskikaistalta ja kuutiosta, ja kaikkien on luettava
            // sama ruutu. Avaimena lomakkeen osoite: uusi lomake on uusi kysymys, eikä
            // edellisen rasti saa periytyä sille. Verify-kenttä katoaa sivulta kun pelaajan
            // vahvistusasetus on pois (mitattu 24.8.2026, fixture
            // move_roll_double_no_verify.html), joten lauta kantaa asetuksen ja sovellus
            // seuraa sitä eikä keksi omaansa (SUBSTANSSI.md kohta 102).
            // Rastit ruuduittain (25.9.2026): sivun oma ruutu ja asetuksesta piirretty
            // `Verify Decline` ovat eri kysymyksiä, eikä toisen rasti saa vahvistaa toista.
            var verified by remember(board.form?.action) { mutableStateOf(emptySet<String>()) }
            var asked by remember(board.form?.action) { mutableStateOf<String?>(null) }
            var verifyHint by remember(board.form?.action) { mutableStateOf<String?>(null) }
            val verifyBox = VerifyBox(
                boxes = verifyBoxesFor(board.form),
                checked = verified,
            ) { box, checked ->
                verified = if (checked) verified + box else verified - box
                // Rasti on vastaus huomautukseen, joten huomautus lähtee sen mukana.
                if (checked) verifyHint = null
            }
            val pressRouted: (String, Boolean, Boolean) -> Unit = { label, isVerified, fromCube ->
                when (
                    cubeActionFor(
                        label,
                        siteAsksVerify = verifyBox.boxFor(label) != null,
                        verified = isVerified,
                        fromCube = fromCube,
                        deviceConfirms = confirmBeaver && label in BEAVER_SUBMITS,
                    )
                ) {
                    CubeAction.SEND -> onPress(label, isVerified)
                    CubeAction.NEEDS_VERIFY -> verifyHint = label
                    CubeAction.ASK -> asked = label
                }
            }
            val pressChecked: (String, Boolean) -> Unit = { label, isVerified -> pressRouted(label, isVerified, false) }
            // Kuution napautus kysyy aina, myös ilman sivun ruutua (26.9.2026, [cubeActionFor]).
            val pressCube: (String, Boolean) -> Unit = { label, isVerified -> pressRouted(label, isVerified, true) }

            // **Noppien painallus tekona** (Tommin tilaus 4.9.2026). Teko päätetään tässä
            // yhdessä kohdassa ja lokero saa valmiin lambdan, joten piirtokohta ei tunne
            // sääntöä. Reitti on sama kuin napilla: lähetys kulkee [pressChecked]in läpi ja
            // vaihto on kokoamisen oma `local:`-osoite, jonka näkymämalli sieppaa.
            //
            // Lähetys ei kysy vahvistusta, ja se on tietoinen valinta eikä unohdus: sama
            // teko samalla laudalla `Submit Move` -napista ei kysy sitäkään
            // ([IRREVERSIBLE_SUBMITS] ei sisällä sitä), ja kysyminen tässä tekisi kahdesta
            // reitistä eri mieliset. Suoja on kytkin, joka on oletuksena pois.
            val onDiceTap: (() -> Unit)? =
                when (diceTapFor(state.composition, diceSubmitTap, diceSwapTap)) {
                    DiceTap.SUBMIT -> ({ pressChecked(CompositionSession.SUBMIT_MOVE, false) })
                    DiceTap.SWAP -> ({ onFollow(CompositionSession.LOCAL_SWAP) })
                    null -> null
                }
            // Vastustajan nopat heittävät samaa reittiä kuin `Roll Dice` -nappi (9.10.2026).
            // Ei vahvistusta samasta syystä kuin lähetyksellä yllä: nappikaan ei kysy.
            val onOpponentDiceTap: (() -> Unit)? =
                if (diceRollTapFor(board.form?.submits, diceRollTap)) {
                    { pressChecked(ROLL_DICE_SUBMIT, false) }
                } else {
                    null
                }

            // **Sivun tilarivit keskikaistalle kun siellä on vapaa puolisko** (tilanneteksti
            // 8.9.2026; peruutus, vahdit ja toistojono Tommin päätös 14.9.2026: *"haluaisin
            // yhtenäistää niitä keskelle lautaa jos tilaa on"*). Rivit ovat ne jotka kertovat
            // tämän sivun tilasta; muistutukset ovat pelaajan omia ja jäävät laudan alle,
            // tuntemattomat ilmoitukset samoin koska niiden pituutta ei tunneta. Kun
            // lokeroa ei ole, rivit ovat paneelin pohjalla ja laudan alla kuten ennen.
            // **Vastustajan siirto näkyy vain vastustajan noppien kanssa** (Tommin kuittaus
            // 30.9.2026 illalla: *"kun vastustajan nopat häviää, niin siirtonuolet häviää"*).
            // Sivu näyttää vastustajan heiton omaan `Roll Dice`en asti, ja nuolet ilman noppia
            // eivät kertoneet mistä heitosta ne syntyivät. Ehto koskee nuolia ja latausriviä.
            val showsOpponentMove = state.showsOpponentMove &&
                board.dice.any { it.owner == roles.opponentColor }
            val stripNotes = stripNotes(board, state.pips, state.checkers, state.unconfirmed, state.readingOpponent && showsOpponentMove)
            // **Puhelimen pystylaudalla rivit laudan alle** (Tommin kuittaus 24.9.2026): kaista
            // on siellä kahden pienen nappulan korkuinen, ja *Rolled back* katkesi ja
            // tuplausrivi jäi kokonaan piiloon (`pysty-verify-accept-puhelin.png`). Laudan
            // alla on tilaa. Ehto on sama ahtausraja joka jo erottaa puhelimen tabletista
            // pystyssä; tabletilla rivit jäävät kaistalle kuten 14.9.2026 päätös sanoo.
            val stripCrowded = !fits || (panelBelow && compactPanel)
            val situationSlot = situationSlot(
                hasNotes = stripNotes.isNotEmpty(),
                opponentHasDice = board.dice.any { it.owner == roles.opponentColor },
                selfHasDice = board.dice.any { it.owner == roles.selfColor },
                cubeOffered = board.form?.submits?.contains(ACCEPT_SUBMIT) == true,
                stripHasActions = stripCrowded,
            )
            notesOnStrip = situationSlot != null
            // Rastihuomautus samaan lokeroon (Tommin päätös 14.9.2026, vaihtoehto A
            // neljästä kuvasta): tilannetekstin alle kun se on kaistalla, muuten samalla
            // säännöllä vapaalle puoliskolle, ja laudan yläreunaan vain kun kaistalla ei
            // ole tilaa. Ks. [verifyHintSlot].
            val hintSlot = verifyHint?.let {
                verifyHintSlot(
                    situationSlot = if (stripNotes.isNotEmpty()) situationSlot else null,
                    opponentHasDice = board.dice.any { it.owner == roles.opponentColor },
                    selfHasDice = board.dice.any { it.owner == roles.selfColor },
                    cubeOffered = board.form?.submits?.contains(ACCEPT_SUBMIT) == true,
                    stripHasActions = stripCrowded,
                )
            }
            val verifyHintText = verifyHint?.let { label ->
                stringResource(R.string.board_verify_first, verifyBox.boxFor(label) ?: label)
            }
            Box(modifier = Modifier.fillMaxSize()) {
            // Pystyssä pelatessa lauta ylhäällä ja paneeli sen alla ([panelBelow]); muuten
            // rivi kuten ennen. Sama sisältö kummassakin, vain pääakseli vaihtuu.
            val arrange: @Composable (@Composable () -> Unit) -> Unit = { content ->
                if (panelBelow) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { content() }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                    ) { content() }
                }
            }
            arrange {
                // **Paneeli siirtokäden vastakkaiselle puolelle** (Tommin päätös 9.9.2026:
                // *"korjataan kaanoni, nimeä se kätisyydeksi"*). Oikeakätisellä paneeli on
                // vasemmalla ja vasenkätisellä oikealla; peilaus ei siirrä sitä lainkaan.
                //
                // Tämä korvaa 8.9.2026 säännön joka luki puolen [BoardLook.mirrored]ista.
                // Peruste kumoutui käytössä: peilatessa siirtokäsi ei vaihdu, vain paneeli
                // siirtyy, ja se vie paneelin sen käden alle jolla nappuloita liikutetaan
                // (`SUBSTANSSI.md` kohta 103). Lokerosarake seuraa yhä peilausta, koska se
                // on laudan osa eikä käden alla.
                val panelOnRight = leftHanded
                // Omistettu kuutio paneelin korttiin (Tommin valinta 14.9.2026 illalla, B).
                // Ehdot ovat samat kuin laudan omat: tarjottu kuutio menee kaistalle ja
                // omistamaton muurille, ja vain omistettu tarjoamaton on kortissa.
                val cubeOwnedHere = board.cube?.position == CubePosition.TOP ||
                    board.cube?.position == CubePosition.BOTTOM
                val cubeOfferedHere = board.form?.submits?.contains(ACCEPT_SUBMIT) == true
                val panelCube = shownCube(board).takeIf { fits && cubeOwnedHere && !cubeOfferedHere }
                val panelOnCube: (() -> Unit)? = when {
                    board.form?.submits?.contains(DOUBLE_SUBMIT) == true ->
                        ({ pressCube(DOUBLE_SUBMIT, verifyBox.isChecked(DOUBLE_SUBMIT)) })
                    else -> null
                }
                val panel: @Composable () -> Unit = {
                    SidePanel(
                        board = state.board,
                        compact = compactPanel,
                        roundLabel = state.roundLabel,
                        roles = roles,
                        pips = state.pips,
                        checkers = state.checkers,
                        awayOf = awayOf,
                        onOpenPage = onOpenPage,
                        // Sama sääntö kuin laudalla ([cubeOnStrip]): omistettu ja
                        // tarjoamaton kuutio ei ole kaistalla, ja paneelin ollessa se
                        // on omistajan kortissa eikä muurin päässä.
                        panelCube = panelCube,
                        onCube = panelOnCube,
                        cubeReminder = cubeReminderShown,
                        modifier = if (panelBelow) {
                            Modifier.width(frameWidth).height(panelBelowHeight)
                        } else {
                            Modifier.width(panelWidth).fillMaxHeight()
                        },
                        actions = reminderActions,
                        actionsPresent = panelActionsPresent,
                        // Napit vasempaan reunaan peukalon alle (Tommin päätös 2.9.2026).
                        // Sama chat-kortin ehto kuin keskikaistalla.
                        formActions = {
                            PanelActionStack(
                                board,
                                onFollow = onFollow,
                                onPress = pressChecked,
                                verify = verifyBox,
                                showSubmits = !chatCardVisible,
                                busy = state.refreshing,
                                arc = arcLookOf(roles),
                                compact = compactPanel,
                            )
                        },
                        showNotes = situationSlot == null,
                    )
                }
                if (fits && !panelOnRight && !panelBelow) {
                    panel()
                    Spacer(modifier = Modifier.width(SIDE_PANEL_GAP))
                }
                // **Pystyssä kortit laudan ympärille** (Tommin ehdotus 23.9.2026: *"mitä jos
                // vastustajan kortti olisi pelilaudan yläpuolella ja oma juuri alapuolella,
                // nyt omistettu tuplauskuutio olisi oudossa paikassa"*). Kortti on
                // omistajuutta (14.9.2026, B), joten omistettu kuutio on nyt sillä puolella
                // lautaa jolla omistajan nappulat ovat. Ottelukortti ruudun ylälaitaan
                // (Tommin kallistus samana iltana), pelinapit heti oman kortin alle ja
                // sovelluksen omat lisät niiden alle.
                if (panelBelow) {
                    MatchCard(
                        state.board,
                        state.roundLabel,
                        compactPanel,
                        onOpenPage,
                        modifier = Modifier.width(frameWidth),
                    )
                    Spacer(modifier = Modifier.height(SIDE_PANEL_GAP))
                    state.board.players.getOrNull(0)?.let {
                        Box(modifier = Modifier.width(frameWidth)) {
                            PlayerPanelView(
                                it,
                                awayOf(it),
                                roles.opponentPaint(),
                                other = state.board.players.getOrNull(1),
                                otherAway = state.board.players.getOrNull(1)?.let { p -> awayOf(p) },
                                matchLength = state.board.matchLength,
                                onOpen = it.player.profilePath?.let { path -> { onOpenPage(path) } },
                                cube = panelCube?.takeIf { c -> c.position == CubePosition.TOP },
                                onDouble = panelOnCube,
                                cubeAttention = cubeReminderShown,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(SIDE_PANEL_GAP))
                }
                Board(
                    board,
                    roles = roles,
                    pips = state.pips,
                    awayOf = awayOf,
                    touch = touch,
                    onFollow = onFollow,
                    onPress = pressChecked,
                    onCubePress = pressCube,
                    onDiceTap = onDiceTap,
                    onOpponentDiceTap = onOpponentDiceTap,
                    // Nuolet luetaan samasta kokoamisesta kuin piirretty lauta (`boardNow`), joten
                    // ne eivät voi näyttää muuta kuin sitä mikä lähtee. Sivun omalla laudalla
                    // kokoamista ei ole, eikä nuolia silloin myöskään.
                    arrows = if (moveArrows) state.composition?.arrows().orEmpty() else emptyList(),
                    // Vastustajan siirto näkyy vain kunnes ensimmäinen oma askel on koottu
                    // (Tommin kuittaus 29.9.2026 illalla), joten nuolet eivät ole koskaan
                    // laudalla yhtä aikaa. Ehto on koottu askel eikä kytkin, jotta vastustajan
                    // nuolet häviävät myös silloin kun omat nuolet ovat pois.
                    opponentArrows = if (showsOpponentMove) state.opponentArrows else emptyList(),
                    verify = verifyBox,
                    modifier = if (portraitBoardHeight != null) {
                        Modifier.width(frameWidth).height(portraitBoardHeight)
                    } else {
                        Modifier.width(frameWidth).fillMaxHeight()
                    },
                    // **Sama ehto ja sama mitta kuin ennen napeilla** (Tommin valinta
                    // 14.8.2026, `TRAY_TEXT_MIN` uudelleennimetty samalla). Toinen kynnys
                    // omalle sisällölleen olisi toinen ylläpidettävä luku ilman omaa
                    // mittausta, ja tämä on jo se leveys jolla sarakkeen teksti on
                    // luettavaa. Ilman tätä riviä pisteet, pipit ja pelaajien nimet eivät
                    // ole tabletilla ruudulla missään, koska paneelia ei ole ja
                    // otteluluettelokin näyttää vain nimen ja polun.
                    playerFactsInTray = showTrayFacts,
                    ownedCubeInPanel = panelCube != null,
                    pipsOnBar = fits,
                    cubeReminder = cubeReminderShown,
                    verifyHint = if (hintSlot != null) verifyHintText else null,
                    verifyHintSlot = hintSlot,
                    onDismissVerifyHint = { verifyHint = null },
                    // Tyhjä kun paneeli on: napit ovat siellä eivätkä molemmissa.
                    middleTrailing = if (fits) ({}) else reminderActions,
                    // Sama sääntö toisella akselilla: chat-kortin ollessa näkyvissä sivun
                    // lomakkeen napit ovat kortissa eivätkä molemmissa. Ehto on kortin
                    // näkyvyys eikä lomakkeen olemassaolo: piirtämätön kortti ei omista
                    // mitään, ja napit pysyvät silloin toimintorivillä.
                    middleShowSubmits = !chatCardVisible,
                    busy = state.refreshing,
                    // **Ei koskaan tässä haarassa 11.9.2026 alkaen.** `Skip Game` tulee riville
                    // `reminderActions`in mukana silloin kun paneelia ei ole (`middleTrailing`
                    // yllä), ja `!fits` piirsi sen toisen kerran rivin omana alkiona. Kaksois-
                    // nappi mitattiin 320 dp:n kaappauksesta; se oli ollut siellä 27.8.2026
                    // alkaen näkymättä, koska rivin loppu oli vierityksen takana.
                    middleShowSkip = false,
                    // Napit ovat paneelissa kun paneeli on (2.9.2026), muuten kaistalla.
                    middleShowActions = !fits,
                    // Syy on tilarivi eikä napin vieressä (26.9.2026, [stripNotes]).
                    unconfirmedRefresh = unconfirmedRefresh,
                    stripNotes = if (situationSlot != null) stripNotes else emptyList(),
                    stripNotesSlot = situationSlot,
                )
                if (fits && panelOnRight && !panelBelow) {
                    Spacer(modifier = Modifier.width(SIDE_PANEL_GAP))
                    panel()
                }
                if (panelBelow) {
                    Spacer(modifier = Modifier.height(SIDE_PANEL_GAP))
                    // **Väljästi kun korkeus sallii** (Tommin havainto ja valinta B 24.9.2026
                    // puhelimen pelisession jälkeen: *"aika tihrustamista, napit ja toiminnot
                    // voisi olla korkeuden sen salliessa väljemmin"*). Väljä versio mitataan
                    // ensin: täysikokoiset napit ja kortti tasavälein oman kortin alla, ja vapaa
                    // korkeus jää alareunaan (Tommin kiristys samana päivänä: *"nyt oli jo liian
                    // väljää"*, valinta "tiivis ylös"; joustot pois). Jos se ei mahdu, piirretään
                    // ahdas kuten ennen. Päätös tehdään samassa mittauksessa eikä tilasta, joten
                    // se ei voi värähdellä (ks. 10.8.2026 `fits`).
                    RoomyOrCompact(
                        modifier = Modifier.width(frameWidth).fillMaxHeight(),
                        forceCompact = !compactPanel,
                    ) { roomy ->
                    val compact = compactPanel && !roomy
                    CompositionLocalProvider(LocalRoomyPortrait provides roomy) {
                    // **Vierittyy kun ahdaskaan ei mahdu** (Tommin valinta A 26.9.2026). Pixelillä
                    // `font_scale 2.0` teki paneelista ruutua korkeamman, ja viimeinen tilarivi,
                    // jossa katkon syykin on, leikkautui alareunasta. Mahtuessa vieritys on
                    // inertti, joten tavallinen koko ei muutu.
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (roomy) 10.dp else 8.dp),
                    ) {
                        state.board.players.getOrNull(1)?.let {
                            PlayerPanelView(
                                it,
                                awayOf(it),
                                roles.selfPaint(),
                                other = state.board.players.getOrNull(0),
                                otherAway = state.board.players.getOrNull(0)?.let { p -> awayOf(p) },
                                matchLength = state.board.matchLength,
                                onOpen = it.player.profilePath?.let { path -> { onOpenPage(path) } },
                                cube = panelCube?.takeIf { c -> c.position == CubePosition.BOTTOM },
                                onDouble = panelOnCube,
                                cubeAttention = cubeReminderShown,
                            )
                        }
                        PanelActionStack(
                            board,
                            onFollow = onFollow,
                            onPress = pressChecked,
                            verify = verifyBox,
                            showSubmits = !chatCardVisible,
                            busy = state.refreshing,
                            arc = arcLookOf(roles),
                            compact = compact,
                            verifyBeside = true,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            reminderActions()
                        }
                        // Rastihuomautus on pystyssä tilarivien jatko, kun keskikaistalla ei ole
                        // sille tilaa (Tommin havainto ja valinta 24.9.2026: *"Verify tick viesti
                        // meni turnauskortin päälle"*). Varapaikka ruudun yläreunassa on tehty
                        // vaakalautaa varten, ja pystyssä siinä on ottelukortti.
                        val panelHint = verifyHintText?.takeIf { hintSlot == null }
                        val panelNotes = if (situationSlot == null) stripNotes else emptyList()
                        // Katkotilan syy on tilarivien ensimmäinen ([stripNotes], 26.9.2026).
                        if (panelNotes.isNotEmpty() || panelHint != null) {
                            // Viiva erottaa sivun tilarivit toiminnoista (Tommin tilaus
                            // 24.9.2026), samassa asussa kuin sivupaneelin lisien viiva.
                            HorizontalDivider(
                                thickness = DgBoard.OUTLINE,
                                color = LocalPanelLook.current.outline.copy(alpha = 0.6f),
                            )
                            panelNotes.forEach { note ->
                                Text(
                                    text = note.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = note.color,
                                    fontWeight = if (note.bold) FontWeight.Bold else null,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            panelHint?.let { VerifyHintRow(text = it, onDismiss = { verifyHint = null }) }
                        }
                    }
                    }
                    }
                }
            }

                asked?.let { label ->
                    CubeDialog(
                        label = label,
                        rolledBack = board.rolledBack,
                        onConfirm = {
                            asked = null
                            // Rasti on jo tehty kun tänne päästään (tai sivu ei pyydä sitä),
                            // joten lähetys kantaa ruudun tilan sellaisenaan.
                            onPress(label, verifyBox.isChecked(label))
                        },
                        onDismiss = { asked = null },
                    )
                }

                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                    // Rastittamaton kuutioteko ei lähde eikä katoa tyhjään: ruutu sanoo
                    // mitä puuttuu. Sivusto vastaisi samaan painallukseen punaisella
                    // rivillä `Previous move not verified!` (mitattu 3.9.2026), ja tämä
                    // on sama tieto ennen pyyntöä eikä sen jälkeen.
                    if (hintSlot == null && verifyHintText != null && !panelBelow) {
                        VerifyHintRow(text = verifyHintText, onDismiss = { verifyHint = null })
                    }
                }
            }
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ReminderStrip(
                reminders = visibleReminders,
                // Peli luetaan laudalta yllä eikä välitetä erikseen: sama johdos kuin
                // näkymämallissa, ja `null` tarkoittaa samaa molemmissa. Ruutu ei siis
                // tarjoa kenttää siihen mitä näkymämalli ei ottaisi vastaan.
                composing = composing && gameKnown,
                draft = draft,
                onDraftChange = { draft = it },
                onAdd = {
                    onAddReminder(it)
                    draft = ""
                    composing = false
                },
                onRemove = onRemoveReminder,
            )
            MarkStrip(
                marking = marking && gameKnown && board.moveNumber != null,
                moveNumber = board.moveNumber,
                draft = markDraft,
                onDraftChange = { markDraft = it },
                onMark = {
                    onMarkPosition(it)
                    markDraft = ""
                    marking = false
                },
            )

            // Ilmoitukset sellaisenaan ja järjestyksessä. Niiden sanamuotoa ei tunneta kuin
            // yhden osalta, joten niitä ei tulkita.
            //
            // Se yksi tunnettu jätetään tässä pois: peruutus on jo yllä omana palkkinaan, ja
            // molemmat piirtämällä sama tapahtuma näkyi laitteella kahdesti (Tommin havainto
            // 10.8.2026). Rajaus on `plainNotices`issa eikä tässä, ks. `BoardState`.
            board.plainNotices.forEach { notice ->
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalPanelLook.current.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            // Toistojonon pituus lukuna, ei linkkinä (Tommin päätös 9.9.2026): haaran
            // alkuasema katsotaan selaimella, mutta se että toisto odottaa kuuluu laudalle.
            // Kaistalla 14.9.2026 alkaen kun lokero on; tässä vain varapaikkana.
            if (!notesOnStrip) board.pendingReplays?.let { count ->
                Text(
                    text = stringResource(R.string.board_pending_replays, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalPanelLook.current.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Muistutukset, eli pelaajan omat muistiinpanot tämän pelin ajaksi.
 *
 * **Tämä ei ole sivuston ominaisuus vaan sovelluksen oma.** Selaimessa näkyvä
 * `Reminder`-lohko tulee laajennuksesta `DGText2Area`, ja se on mitattu: elementit ovat
 * kuvakaappauksessa muttei haetussa HTML:ssä (`docs/KOHDE.md`). Siitä seuraa kaksi asiaa
 * jotka näkyvät tässä composablessa. Painallus ei tuota yhtään pyyntöä, joten se ei voi
 * epäonnistua verkkoon eikä sitä tarvitse jonottaa. Ja nappien tekstit ovat omiamme, toisin
 * kuin [MiddleActionsRow]issa: siellä sanaa ei käännetä koska se on sivun oma sana, tässä
 * sivu ei tiedä koko asiasta mitään.
 *
 * **Yksi koti kaikilla näytöillä**, samalla päätöksellä kuin toimintorivillä 14.8.2026:
 * sisältö asuu laudan alla vierivässä sarakkeessa eikä sivupaneelissa. Paneeli näkyy vain
 * kun leveyttä jää yli, eli puhelimella ei useinkaan lainkaan, ja Tommin pyyntö oli
 * nimenomaan että tämä on käytettävissä. Kaksi kotia olisi kaksi ylläpidettävää
 * leveysehtoa, ja se on juuri se mikä nappien kanssa purettiin.
 *
 * **Kirjoituskenttä on suljettuna oletuksena, ja syy on korkeus.** Lauta kutistuu
 * korkeudesta, joten jokainen pysyvästi näkyvä rivi laudan alla maksaa nappulan kokoa.
 * Suljettuna tämä on yksi nappirivi, ja auki vain silloin kun käyttäjä kirjoittaa.
 * Pikanappi on silti näkyvissä suljettunakin: se on kohdan 25 tavallisin tapaus
 * (`SUBSTANSSI.md`), eikä sitä kannata piilottaa kirjoituskentän taakse.
 *
 * **Muistutus ei kutsu pelaamaan** (`SUBSTANSSI.md` kohta 94). Se näkyy kun pelaaja avaa
 * laudan itse, eikä sovellus ilmoita siitä missään muualla. Hetken valinta on ainoa laadun
 * suoja, ja ilmoitus joka vetää ruudulle purkaisi sen.
 *
 * [gameKnown] on epätosi silloin kun sivu ei kertonut molempia pisteitä, ks.
 * [fi.tommi.dg.domain.GameKey]. Silloin kenttää ei tarjota lainkaan: muistutus ilman pelin
 * tunnistetta joutuisi väärään peliin tai katoaisi oikeasta, ja kumpikin olisi hiljainen
 * virhe. Jo kirjoitetut rivit näkyvät silti, koska ne on luettu oikealla tunnisteella.
 */
@Composable
private fun ReminderStrip(
    reminders: List<Reminder>,
    composing: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    // **Tyhjänä tämä ei ole mitään, eikä vie riviä.** Ennen 24.8.2026 tässä oli aina
    // nappirivi, ja se maksoi laudalta korkeutta myös silloin kun muistutuksia ei ollut.
    if (reminders.isEmpty() && !composing) return

    val panelLook = LocalPanelLook.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        reminders.forEach { reminder ->
            ReminderRow(reminder = reminder, onRemove = onRemove)
        }

        if (!composing) return@Column

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                // Pituusrajaa ei ole, koska sitä ei ole kellään: rivi jää tähän
                // laitteeseen eikä mene sivustolle, joten sivun 80 merkin raja ei
                // koske tätä eikä sitä pidä jäljitellä.
                label = {
                    Text(
                        text = stringResource(R.string.board_reminder_field),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    // Värit annetaan käsin, koska tämä ruutu on laudan oma tumma
                    // paletti eikä teeman vaalea pinta. Ilman näitä kenttä piirtyisi
                    // vaalealla tekstillä vaalealle taustalle.
                    focusedTextColor = LocalPanelLook.current.text,
                    unfocusedTextColor = LocalPanelLook.current.text,
                    focusedBorderColor = LocalPanelLook.current.accent,
                    unfocusedBorderColor = panelLook.outline,
                    focusedLabelColor = LocalPanelLook.current.accent,
                    unfocusedLabelColor = panelLook.muted,
                    cursorColor = LocalPanelLook.current.accent,
                ),
            )
            OutlinedButton(
                onClick = { onAdd(draft) },
                // Tyhjä rivi ei ole muistutus. Sama sääntö kuin viestiruudun
                // lähetysnapilla, vaikka syy on lievempi: tässä ei kulu tekoa.
                enabled = draft.isNotBlank(),
                shape = boardButtonShape(),
                // Värit käsin samasta syystä kuin kentällä yllä: tämä on laudan oma
                // tumma paletti eikä teeman pinta. Oletusten disabloitu teksti oli
                // tummalla pohjalla niin heikko, ettei sovelluksen ainoa käyttäjä
                // huomannut koko nappia (25.8.2026). Disabloitu saa TextMutedin:
                // himmeä mutta luettava, sama sävy kuin kentän lepotilan label.
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = LocalPanelLook.current.accent,
                    disabledContentColor = panelLook.muted,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (draft.isNotBlank()) LocalPanelLook.current.accent else panelLook.outline,
                ),
            ) {
                Text(
                    text = stringResource(R.string.board_reminder_add),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(
            text = stringResource(R.string.board_reminder_scope),
            style = MaterialTheme.typography.labelSmall,
            color = panelLook.muted,
        )
    }
}

/**
 * Muistutusten napit laudan omalla toimintorivillä, sivun omien nappien perässä.
 *
 * **Sijainti on Tommin päätös 24.8.2026 ja se maksoi periaatteen puolikkaan.** Rivin sanat
 * ovat siihen asti olleet sivun omia sanoja, ja nyt samalla rivillä on kahden lähteen sanoja.
 * Vastine on korkeus: laudan alla oleva pysyvä nappirivi kutisti nappulaa jokaisella ruudulla,
 * myös silloin kun muistutuksia ei ollut yhtäkään. Ks. [MiddleActionsRow], jossa järjestys
 * pitää eron näkyvissä.
 *
 * Pikanappi kirjoittaa rivin suoraan eikä täytä kenttää: valmis muistutus yhdellä
 * painalluksella on koko sen olemassaolon syy, ja kentän kautta kiertäminen tekisi siitä
 * kaksi painallusta.
 *
 * Väri on sama kuin sivun omilla komennoilla (värikatsaus 24.8.2026: `TextMuted` oli
 * kaistaa vasten 2,36:1 eli mitatusti heikko, `TextSecondary` on 5,34:1). Ero sivun
 * tekoihin kantautuu järjestyksellä, ei himmeydellä.
 */
/**
 * `Skip Game`, sivuston oma komento, kahdessa mahdollisessa paikassa.
 *
 * **Ulkoasu on sama molemmissa, ja se on koko erottelun ehto** (27.8.2026, `docs/UI.md`).
 * Kaanoni sanoo että sivun omat komennot ovat nappeja ja sovelluksen omat lisät tekstiä, ja
 * ehto luetaan lähteestä eikä sijainnista. Kun tämä siirtyy sivupaneeliin muistutusten
 * seuraan, se pysyy reunustettuna nappina: paneelissa on silloin kahta lajia, mutta ne
 * erottuvat samalla merkillä kuin toimintorivillä.
 */
@Composable
private fun SkipGameAction(
    href: String,
    onFollow: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Ahdas paneeli: 32 dp:n korkuinen nappi 40:n sijaan, ks. [ReminderActions]. */
    compact: Boolean = false,
) {
    val blocked = LocalActionsBlocked.current
    OutlinedButton(
        onClick = { onFollow(href) },
        modifier = (if (compact) modifier.heightIn(min = DgBoard.COMPACT_BUTTON_HEIGHT) else modifier)
            .blockedAlpha(blocked)
            .decoButtonLine(),
        enabled = !blocked,
        shape = boardButtonShape(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LocalPanelLook.current.secondary,
            disabledContentColor = LocalPanelLook.current.secondary,
        ),
        border = BorderStroke(DgBoard.OUTLINE, LocalPanelLook.current.secondary),
    ) {
        Text(
            text = stringResource(R.string.board_skip_game),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ReminderActions(
    onToggleComposing: () -> Unit,
    /** Lisää kuutiomuistutuksen tai poistaa sen: sama linkki kumpaankin (14.9.2026). */
    onToggleCube: () -> Unit,
    showCube: Boolean,
    /** Avaa merkin kentän laudan alle, ks. [MarkStrip]. Muistutusten seassa Tommin toiveesta 15.9.2026. */
    onToggleMarking: () -> Unit = {},
    showMark: Boolean = false,
    /**
     * Ahdas paneeli (16.9.2026): tekstinapit 28 dp:n korkuisina Materialin 40 dp:n sijaan.
     * `Modifier.heightIn(min)` ohittaa napin oman `defaultMinSize`n, koska se antaa nollasta
     * poikkeavan alarajan. Kosketusala kapenee, ja se on hinta jonka ahdas paneeli maksaa
     * päällekkäisyyden sijaan. Alaraja eikä kiinteä korkeus (26.9.2026): fonttikoolla 2.0
     * kiinteä korkeus leikkasi tekstin alaosan, alaraja antaa napin kasvaa tekstin mukana.
     */
    compact: Boolean = false,
    /** Ahdas paneeli viidellä toiminnolla: `Mark position` omalle riville, ks. kutsuja. */
    markOwnRow: Boolean = false,
    /**
     * Pystylauta: [leading] (sivun napit) ja `Mark position` yhtenä parina, `Reminders` ja
     * `Cube reminder` toisena (Tommin tilaus 26.9.2026 puhelimen pystytilaan). Pari rivittyy
     * kokonaisena, joten puhelimella parit ovat omilla riveillään ja tabletilla, jossa kaikki
     * mahtuvat, rivi on ennallaan.
     */
    pairRows: Boolean = false,
    leading: @Composable () -> Unit = {},
) {
    // **Painettava ei saa näyttää samalta kuin luettava** (Tommin päätös 27.8.2026,
    // `docs/UI.md`): sivupaneelissa nämä olivat samanvärisiä kuin ottelun staattiset
    // tiedot. Väri ei ollut vapaana: `Accent` on samassa paneelissa staattisella rivillä,
    // `CheckerSelf` on erottumaton `TextPrimary`sta, ja `CubeSoft` on peruuttamattoman
    // teon reunusväri eli väittäisi muistutuksesta päinvastaista kuin se on. Toteutus oli
    // 27.8.–20.9.2026 alleviivaus; nyt nuolenpää ja `TextMuted`, ks. [LinkText].
    // Väljässä paneelissa rivit ovat koko leveyden ja vasemmalle tasattuja (20.9.2026),
    // ahtaassa sisältönsä levyisiä, koska `FlowRow` latoo ne vierekkäin.
    // Järjestys `Mark position`, `Reminders`, `Cube reminder` (Tommin tilaus 21.9.2026
    // kännykän kaappauksen jälkeen: *"järjestys Skip game, mark position, reminders,
    // cube reminder"*). Ahtaassa paneelissa `Mark position` on silloin `Skip Game`n
    // rivikaveri, ja sama järjestys pidetään väljässä paneelissa, jotta luettelo on
    // yksi eikä kaksi. Oli 15.–21.9.2026 `Reminders`, `Cube reminder`, `Mark position`.
    val link = if (compact) Modifier.heightIn(min = DgBoard.COMPACT_LINK_HEIGHT) else Modifier.fillMaxWidth()
    val mark: @Composable () -> Unit = {
        TextButton(
            onClick = onToggleMarking,
            modifier = link,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            LinkText(stringResource(R.string.board_mark_position), fill = !compact)
        }
    }
    val reminders: @Composable () -> Unit = {
        TextButton(
            onClick = onToggleComposing,
            modifier = link,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            LinkText(stringResource(R.string.board_reminders_title), fill = !compact)
        }
    }
    val cube: @Composable () -> Unit = {
        TextButton(
            onClick = onToggleCube,
            modifier = link,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            LinkText(stringResource(R.string.board_reminder_cube), fill = !compact)
        }
    }
    if (pairRows) {
        // Pari on itse `FlowRow`, jotta isolla fonttikoolla pari rivittyy sisäisesti eikä
        // leikkaudu ruudun reunaan.
        val pair: @Composable (@Composable () -> Unit) -> Unit = { content ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
        pair {
            leading()
            if (showMark) mark()
        }
        pair {
            reminders()
            if (showCube) cube()
        }
        return
    }
    if (showMark) {
        if (markOwnRow) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { mark() }
        } else {
            mark()
        }
    }
    reminders()
    if (showCube) cube()
}

/**
 * Merkin kirjoituskenttä laudan alla.
 *
 * **Samaa lajia kuin [ReminderStrip] mutta eri elinkaarta** (Tommin toive 15.9.2026 illalla).
 * Kannassa merkki elää ottelun yli ja luetaan otteluluettelon linkistä (`MarksScreen`).
 *
 * **Tehty merkki ei jää laudan alle riviksi 30.9.2026 alkaen** (Tommi Pixel 8a:n
 * vaakalaudalla: *"Laudan alle tulee rivi Marked for analysis-se rikkoo sivupaneelin"* ja
 * *"riittää että se jää sgf-tiedostoon"*). Rivi vei korkeutta laudalta ja paneelilta.
 * Kuittaus on kentän sulkeutuminen, ja lista ja poisto ovat `MarksScreen`issä.
 *
 * Sana on vapaaehtoinen ja nappi on aina painettavissa, toisin kuin muistutuksen `Add`:
 * merkin arvo on kohdassa eikä selityksessä. Kenttä on suljettuna oletuksena samasta
 * syystä kuin muistutuksilla, eli korkeus.
 */
@Composable
private fun MarkStrip(
    marking: Boolean,
    moveNumber: Int?,
    draft: String,
    onDraftChange: (String) -> Unit,
    onMark: (String) -> Unit,
) {
    if (!marking) return

    val panelLook = LocalPanelLook.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        text = stringResource(R.string.board_mark_field),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LocalPanelLook.current.text,
                    unfocusedTextColor = LocalPanelLook.current.text,
                    focusedBorderColor = LocalPanelLook.current.accent,
                    unfocusedBorderColor = panelLook.outline,
                    focusedLabelColor = LocalPanelLook.current.accent,
                    unfocusedLabelColor = panelLook.muted,
                    cursorColor = LocalPanelLook.current.accent,
                ),
            )
            OutlinedButton(
                onClick = { onMark(draft) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalPanelLook.current.accent),
                border = BorderStroke(width = 1.dp, color = LocalPanelLook.current.accent),
            ) {
                Text(
                    // Napissa lukee mitä se merkitsee, jotta numero on nähty ennen painallusta.
                    text = moveNumber?.let { stringResource(R.string.board_mark_add_move, it) }
                        ?: stringResource(R.string.board_mark_add),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(
            text = stringResource(R.string.board_mark_scope),
            style = MaterialTheme.typography.labelSmall,
            color = panelLook.muted,
        )
    }
}

/**
 * Sovelluksen oma painettava teksti, eli linkki.
 *
 * **Nuolenpää ja vaimea väri alleviivauksen sijaan** (Tommin valinta 20.9.2026 kankaalta,
 * `docs/UI.md` › Sivupaneelin napit). Tarve on 27.8.2026 päätöksen: painettava ei saa
 * näyttää samalta kuin luettava, eikä väri ollut vapaana. Alleviivaus oli yksi signaali
 * ja luki selaimen linkiltä; nyt signaaleja on kaksi. Nuolenpää sanoo että rivi vie
 * johonkin, ja [Palette.TextMuted] (5,49:1 paneelia vasten, AA) että se on toissijainen
 * ottelukortin [Palette.TextPrimary]-tekstiin nähden. `TextMuted` ei ole paneelissa
 * varattu muuhun kuin toissijaisiin riveihin, joten se ei kilpaile merkitysten kanssa.
 *
 * [fill] tasaa rivin paneelin vasempaan laitaan; ahtaan paneelin `FlowRow`ssa rivit ovat
 * sisältönsä levyisiä ja keskitettyjä kuten ennenkin.
 */
@Composable
private fun LinkText(text: String, fill: Boolean = false) {
    Text(
        text = "\u203A $text",
        style = MaterialTheme.typography.labelLarge,
        color = LocalPanelLook.current.faint,
        textAlign = TextAlign.Start,
        modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
    )
}

/**
 * Yksi muistutus ja sen poisto.
 *
 * Poisto on rivillä eikä valikossa, koska se on tämän tiedon tavallisin loppu: muistutus
 * kirjoitetaan hoidettavaksi, ja hoidettuaan sen pelaaja poistaa rivin. Vahvistusta ei
 * kysytä, koska rivin voi kirjoittaa uudestaan; se on eri asia kuin viesti, jota ei voi.
 */
@Composable
private fun ReminderRow(reminder: Reminder, onRemove: (Long) -> Unit) {
    val removeLabel = stringResource(R.string.board_reminder_remove)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = reminder.text,
            style = MaterialTheme.typography.bodySmall,
            color = LocalPanelLook.current.accent,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onRemove(reminder.id) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            // Merkki on lyhyt mutta ruudunlukijalle tyhjä, joten teko nimetään erikseen.
            modifier = Modifier.semantics { contentDescription = removeLabel },
        ) {
            Text(
                text = "\u00d7",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPanelLook.current.muted,
            )
        }
    }
}

/**
 * Laudan kosketus: mistä pisteestä voi siirtää ja mihin osoitteeseen.
 *
 * **Sivun linkki kulkee tässä muuttumattomana lautaruudulle asti.** Ruutu ei muodosta
 * osoitetta pistenumerosta, koska kirjaimen ja pisteen yhteys (`a`=1 ... `x`=24) on havainto
 * eikä sopimus. Väärä arvaus tuottaisi väärän siirron eikä virhettä.
 *
 * Tyhjä [movable] tarkoittaa lautaa jolla ei ole siirtoja: vastustajan vuoro, kesken oleva
 * kuutiokysymys tai päättynyt peli. Silloin lauta ei ota kosketusta vastaan lainkaan.
 */
private class BoardTouch(
    val movable: Map<Int, String>,
    val onFollow: (String) -> Unit,
    /** Painetun pisteen tila, tai null kun kytkin on pois. Ks. [PointPress]. */
    val press: PointPress? = null,
    /**
     * Kutsutaan kun sormi nousee pisteeltä jolta ei voi siirtää (Tommin tilaus 6.10.2026:
     * *"ohi painamisesta sopiva äänimerkki"*). Null kun painalluskytkin on pois tai laudalla
     * ei ole siirtoja: vastustajan vuorolla jokainen napautus olisi ohi, eikä se ole virhe.
     */
    val onMiss: (() -> Unit)? = null,
)

/**
 * Mihin pisteeseen sormi painaa juuri nyt (Tommin tavoite ja valinta 4.10.2026, `PointPressStore`).
 *
 * **Syy on se ettei sormen alta näe osuiko.** Ohilyönti Pixelillä on osumaton napautus, ja
 * *"sormi peittää sarakkeen"* (`docs/TOINEN-ASIAKAS.md`). Kosketusalue oli jo koko sarake, joten
 * puuttui vain näkyvä vastaus sormen ulkopuolella. Se on laudan reunan numero: aksenttiväri kun
 * pisteeltä voi siirtää, yliviivaus kun ei voi.
 *
 * [down] erottaa painalluksen sen jälkeisestä viipymästä. Numero jää näkyviin
 * [POINT_PRESS_LINGER_MS] irrotuksen jälkeen, koska nopea napautus ei muuten ehtisi näkyä
 * lainkaan, ja juuri nopea napautus on se joka jää epävarmaksi.
 */
@Stable
private class PointPress {
    var number by mutableStateOf<Int?>(null)
    var down by mutableStateOf(false)
}

/** Kuinka kauan painetun pisteen numero viipyy irrotuksen jälkeen. */
private const val POINT_PRESS_LINGER_MS = 700L

/**
 * Sivun omat toiminnot yhtenä vaakariviin vieritettävänä rivinä [MiddleStrip]in
 * keskellä, sen pelaajan puolella jonka vuoro on. Napit ovat aina kirjautuneen pelaajan
 * tekoja, koska sivu tarjoaa lomakkeen vain sille jonka vuoro on.
 *
 * **Napit ovat sivun nappeja, eivät ruudun keksimiä.** Teksti on [BoardForm.submits]ista
 * sellaisenaan, eli sama sana jonka selainkäyttäjä näkee (`Submit Move`, `Roll Dice`,
 * `Double`, `Accept`, `Decline`). Sanaa ei käännetä eikä yhtenäistetä: sovellus joka sanoo
 * eri asian kuin sivu jota se lukee, eroaa kohteestaan juuri siinä kohdassa jossa käyttäjä
 * vertaa niitä rinnakkain. Sama syy kuin kielivalinnalla, ks. kanonin kielikohta.
 *
 * **Vahvistusruutu on sivun ruutu ja se on rastittamaton.** Sivulla lukee "Verify Double" tai
 * "Verify Accept", ja se on toinen ele ennen peruuttamatonta tekoa. Sitä ei rastita
 * puolesta, ja sen tila nollautuu kun lomake vaihtuu, koska seuraava lomake on eri kysymys.
 *
 * **Järjestys on sivun järjestys 22.8.2026 alkaen: napit ensin, vahvistusruutu viimeisenä.**
 * Se oli toisin päin siihen asti, ja yhdessä liian kapean kaistan kanssa se tuotti pahimman
 * mahdollisen lopputuloksen: rivistä näkyy aina ensimmäinen alkio, ja se oli vahvistusruutu
 * eli juuri se jolla ei yksin tee mitään. Sama peruste kuin nappien tekstillä yllä, nyt
 * järjestykseen sovellettuna: sovellus joka esittää saman lomakkeen eri järjestyksessä kuin
 * sivu jota se lukee, eroaa kohteestaan siinä kohdassa jossa käyttäjä vertaa niitä rinnakkain.
 *
 * **Ainoa koti 14.8.2026 alkaen** (Tommin päätös). Sama sisältö asui 10.-14.8.2026 kolmessa
 * paikassa näytön leveyden mukaan: sivupaneelissa (`ActionRow`, koko leveydeltä), kapeassa
 * lokerosarakkeessa (`trayActionItems`/`TrayActionStack`, pystyyn pinottuna kahdelle riville
 * katkaistuna) ja kapean laitteen alareunan päällepiirtona. Kolme kotia oli kolme
 * ylläpidettävää leveysehtoa, ja lokerosarakkeen versio oli se joka typisti napin tekstin,
 * koska sen pino oli korkeudesta rajattu eikä kahden rivin teksti aina mahtunut. Yksi koti
 * kaikilla näytöillä poistaa sekä ylläpidon että sen vikamuodon.
 *
 * **Nappi on leipätekstiä suurempi, koska se on toiminto** (Tommin päätös 22.8.2026):
 * `labelLarge` ja korkeus kaistasta johdettuna. Se kumoaa 14.8.2026 tehdyn tiiviyden, joka oli
 * `labelSmall` ja kiinteä 32 dp. Tiiviys oli kirjoitettu ruutua näkemättä, ja sen perustelu oli
 * mahtuminen kaistaan; kun kaista mitattiin laitteelta, se osoittautui tabletilla noin 103 dp
 * korkeaksi eli kolminkertaiseksi tarpeeseen nähden.
 *
 * **Korkeus luetaan kaistasta eikä kirjoiteta vakioksi.** Kaista on kaksi nappulaa, joten se
 * kutistuu nappulan mukana kapealla puhelimella. Kiinteä 40 dp olisi mahtunut tabletilla ja
 * leikkautunut siellä, eli juuri se vikamuoto joka tässä on jo kerran osunut.
 *
 * Tekstiä ei lyhennetä eikä käännetä: leveys hoidetaan vierityksellä, ei katkaisulla.
 */
@Composable
private fun MiddleActionsRow(
    board: BoardState,
    metrics: BoardMetrics,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    trailing: @Composable () -> Unit = {},
    /**
     * Piirretäänkö `Skip Game` tällä rivillä (27.8.2026).
     *
     * Epätosi kun sivupaneeli on näkyvissä: silloin skip on siellä eikä molemmissa. Ehto on
     * sama `fits` joka jo päättää paneelin olemassaolon, joten uutta leveysmittaa ei synny.
     */
    showSkip: Boolean = true,
    /**
     * Piirretäänkö sivun lomakkeen napit tällä rivillä (Tommin päätös 27.8.2026).
     *
     * Epätosi kun chat-kortti on näkyvissä: silloin samat napit ovat kortissa, ja laiteajo
     * näytti ne kahdesti eri tilassa. Rivi ei menetä tästä mitään, koska vuoron päättävällä
     * sivulla lomakkeen ainoat napit ovat `Next Game` ja `To Top`. `Skip Game` ja
     * sovelluksen omat lisät eivät ole lomakkeen tekoja ja jäävät riville.
     */
    showSubmits: Boolean = true,
    /** Vahvistusruudun tila, ruudun tasolla 3.9.2026 alkaen, ks. [VerifyBox]. */
    verify: VerifyBox = VerifyBox.NONE,
    /** Pyyntö on kesken: napit pois ja kaari tilalle, ks. [actionItems]. */
    busy: Boolean = false,
    /** Kaaren värit tältä laudalta, ks. [BusyArc]. */
    arc: ArcLook = ArcLook.X22,
) {
    val form = if (showSubmits) board.form else null
    // Korkeus luetaan kaistasta eika kirjoiteta vakioksi: kaista on kaksi nappulaa
    // (`DgBoard.BAND_PER_CHECKER`), joten se on tabletilla noin 103 dp ja kapealla
    // puhelimella alle 40. Kiintea 40 dp mahtuisi tassa ja leikkautuisi siella.
    val actionHeight = (metrics.bandHeight - 12.dp).coerceIn(32.dp, 44.dp)

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actionItems(
            board = board,
            form = form,
            buttonModifier = Modifier.height(actionHeight),
            onFollow = onFollow,
            onPress = onPress,
            verify = verify,
            showSkip = showSkip,
            busy = busy,
            arc = arc,
        ).forEach { item ->
            when (item) {
                is ActionItem.Content -> item.body()
            }
        }
        // Odotuksen aikana rivillä on vain kaari, myös omat napit pois (16.9.2026).
        if (busy) return@Row

        // **Sovelluksen omat napit viimeisenä** (Tommin päätös 24.8.2026). Ne olivat siihen
        // asti omalla rivillään laudan alla, ja se rivi maksoi laudalta korkeutta myös
        // silloin kun muistutuksia ei ollut yhtäkään.
        //
        // **Varaus, joka kuuluu tähän vaikka päätös on tehty.** Rivin sanat ovat tähän asti
        // olleet sivun omia sanoja, ja se on ollut sääntö eikä sattuma: sovellus joka sanoo
        // eri sanan kuin sivu eroaa kohteestaan juuri siinä kohdassa jossa käyttäjä vertaa
        // niitä rinnakkain. Nyt samalla rivillä on kahden lähteen sanoja. Järjestys on se
        // mikä eron pitää näkyvissä: sivun omat ensin, omamme perässä.
        trailing()
    }
}

/**
 * Sivun teot pystypinona sivupaneelissa (Tommin päätös 2.9.2026, `docs/UI.md`).
 *
 * **Vasen reuna, peukalon alle.** Tommi illan pelisession jälkeen: *"häiritsee kun pitää
 * koko ajan klikkailla keskelle ruutua"*, ja tarkennus: käsi peittää laudan, ja vasen reuna
 * on parempi kuin oikea koska se helpottaa kaksikätistä pelaamista. Paneeli on jo laudan
 * vasemmalla, joten tämä ei lisää uutta leveysehtoa: napit ovat täällä täsmälleen silloin
 * kun paneeli on (`fits`), ja muuten keskikaistalla kuten 14.8.2026 alkaen.
 *
 * Sama sisältö samassa järjestyksessä kuin [MiddleActionsRow] ([actionItems]), vain suunta
 * on toinen: peruuttamattoman teon väli on pystysuuntainen, ja nappi täyttää paneelin
 * leveyden, jotta osumakohta on koko reuna eikä sana.
 */
@Composable
private fun PanelActionStack(
    board: BoardState,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    /** Ks. [MiddleActionsRow]: epätosi kun chat-kortti omistaa sivun napit. */
    showSubmits: Boolean = true,
    /** Vahvistusruudun tila, ruudun tasolla 3.9.2026 alkaen, ks. [VerifyBox]. */
    verify: VerifyBox = VerifyBox.NONE,
    /** Pyyntö on kesken: napit pois ja kaari tilalle, ks. [actionItems]. */
    busy: Boolean = false,
    /** Kaaren värit tältä laudalta, ks. [BusyArc]. */
    arc: ArcLook = ArcLook.X22,
    /**
     * Ahdas paneeli (Tommin päätös 16.9.2026): napit [PANEL_ACTION_HEIGHT_COMPACT]
     * -korkuisina.
     */
    compact: Boolean = false,
    /** Pystytilan lauta: vahvistusruutu napin oikealle, ks. [actionItems]. */
    verifyBeside: Boolean = false,
) {
    val form = if (showSubmits) board.form else null
    val height = if (compact) PANEL_ACTION_HEIGHT_COMPACT else PANEL_ACTION_HEIGHT
    // **Pystylaudalla odotuksen ilmaisin saa pinon korkeuden** (Tommin havainto
    // pelisessiossa 24.9.2026: *"pystyasennossa animaatio tarvitsee lisää pystytilaa"*).
    // Ilmaisimet piirtyvät kiinteällä koollaan (`requiredSize`, korkein 70 dp), ja napin
    // korkuinen paikka jätti ne valumaan korttien päälle. Paikka on sen pinon korkuinen jonka
    // tilalle ilmaisin tulee, vähintään [PORTRAIT_BUSY_MIN], joten alla oleva rivi ei hyppää.
    // Vaakapaneeli on ennallaan.
    val density = LocalDensity.current
    var pileHeight by remember { mutableStateOf(0.dp) }
    val busyModifier = if (verifyBeside && busy) {
        Modifier.fillMaxWidth().height(maxOf(pileHeight, PORTRAIT_BUSY_MIN))
    } else {
        Modifier.fillMaxWidth().heightIn(min = height)
    }
    // **Ahtaassa paneelissa pino ei saa koskea ottelukorttiin** (Tommin kaappaus 16.9.2026:
    // *"ottelukortti ja roll dice nappi on liian kiinni toisissaan"*). Väljässä paneelissa
    // keskitys jättää välin itsestään; ahtaassa lohkossa keskitys palautuu kiinni korttiin
    // (`DgBoard.centeredTop`), joten väli on annettava tässä. Sama 8 dp kuin korttien
    // välillä (`SidePanel`), ja se maksettiin napin korkeudesta (36 → 32).
    // **Ahtaassa vaakapaneelissa teot ovat kahdessa sarakkeessa** (Tommin valinta 30.9.2026
    // mockupista, vaihtoehto A). Pixel 8a:n vaakalaudalla `Chat`, `Mark position`, muistutukset,
    // ottelukortti ja `Next Game` + `To Top` olivat noin 25 dp korkeampia kuin korttien väli,
    // ja lisät valuivat vastustajan kortin päälle. Rinnakkain pino säästää napin korkeuden.
    // Vahvistusruudun ryhmä on yksi lohko, joka ei mahdu puoleen leveyteen, joten silloin
    // pino pysyy pystyssä. Pystylauta ja väljä paneeli ovat ennallaan (2.9.2026).
    val paired = compact && !verifyBeside && !busy && (form == null || verify.boxes.isEmpty())
    val items = actionItems(
        board = board,
        form = form,
        buttonModifier = if (busy) busyModifier else Modifier.fillMaxWidth().heightIn(min = height),
        compactFrame = compact,
        verifyBeside = verifyBeside,
        // Rako nappiin on pinon oma väli toiseen kertaan (Tommin tarkennus 14.9.2026:
        // *"hieman rako korkeussuunnassa"*): ruutu ei ole nappi eikä kuulu sen kylkeen.
        verifyRowModifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        frameVerify = true,
        onFollow = onFollow,
        onPress = onPress,
        verify = verify,
        // Skip on paneelissa muistutusten seurassa (27.8.2026), ei tässä pinossa.
        showSkip = false,
        busy = busy,
        arc = arc,
    )
    val pileModifier = Modifier.fillMaxWidth()
        .then(if (compact) Modifier.padding(top = 8.dp) else Modifier)
        // Pehmusteen jälkeen, jotta mitta on pinon sisältö ilman yläväliä.
        .onSizeChanged { if (!busy) pileHeight = with(density) { it.height.toDp() } }
    if (paired) {
        FlowRow(
            modifier = pileModifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = 2,
        ) {
            items.forEach { item ->
                when (item) {
                    is ActionItem.Content -> Box(modifier = Modifier.weight(1f)) { item.body() }
                }
            }
        }
    } else {
        Column(
            modifier = pileModifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item ->
                when (item) {
                    is ActionItem.Content -> item.body()
                }
            }
        }
    }
}

/** Pystylaudan paneeli väljässä muodossa, ks. [RoomyOrCompact]. Lukevat lisien napit. */
internal val LocalRoomyPortrait = compositionLocalOf { false }

/** Pisteet nimen perässä (`ScoreStyle.NAME`, 9.10.2026), ks. [PlayerPanelView]. */
internal val LocalScoreBesideName = compositionLocalOf { false }

/**
 * Piirtää [content]in väljänä (`true`) jos se mahtuu annettuun korkeuteen, muuten ahtaana.
 * Väljän korkeus luetaan sisällön luontaisesta korkeudesta, joten päätös syntyy samassa
 * mittauksessa eikä kirjoita mitään tilaan. [forceCompact] on tosi silloin kun paneeli on
 * jo väljä sellaisenaan (tabletti), jolloin väljää muotoa ei kokeilla lainkaan.
 */
@Composable
private fun RoomyOrCompact(
    modifier: Modifier,
    forceCompact: Boolean,
    content: @Composable (roomy: Boolean) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val roomyFits = !forceCompact && subcompose("roomy-probe") { content(true) }
            .sumOf { it.maxIntrinsicHeight(constraints.maxWidth) } <= constraints.maxHeight
        val chosen = subcompose(if (roomyFits) "roomy" else "compact") { content(roomyFits) }
            .map { it.measure(constraints) }
        val height = chosen.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            chosen.forEach { it.place(0, 0) }
        }
    }
}

/** Pystylaudan odotuspaikan vähimmäiskorkeus: korkein ilmaisin (deco-tiimalasi 70 dp) ja väljyys. */
private val PORTRAIT_BUSY_MIN = 80.dp

/** Napin korkeus paneelipinossa; keskikaistalla se johdetaan kaistasta, täällä sitä ei ole. */
private val PANEL_ACTION_HEIGHT = 44.dp

/** Ahtaan paneelin napin korkeus, ks. [PanelActionStack] ja [DgBoard.PANEL_COMPACT_BELOW]. */
private val PANEL_ACTION_HEIGHT_COMPACT = 32.dp

/** Vahvistusruudun sivu; sama mitta on pinon ja nimikortin väli paneelissa. */
private val VERIFY_BOX = 20.dp

/** Kuution sivu pelaajakortissa: nimen ja away-rivin yhteinen korkeus (14.9.2026), ei laudan mitta. */
private val CARD_CUBE = 40.dp

/**
 * Ottaako lauta juuri nyt vastaan tekoja. Tosi kun lauta on vahvistamaton
 * ([BoardUiState.Loaded.blocked]): [BoardViewModel.acting] hylkää silloin painalluksen
 * hiljaa, ja tämä on sen näkyvä puoli.
 *
 * **Napit himmennetään eikä piiloteta** (Tommin tilaus 17.9.2026 `sessio-17-9-ilta`n
 * jälkeen: `Next Game` ja `To Matches` näyttivät sirun aikana tavallisilta vaikka
 * painallus oli ei-mitään). Piilotus veisi tiedon siitä mitä sivu tarjosi, ja se tieto
 * on juuri se jonka pelaaja vertaa tuoreeseen lautaan Refreshin jälkeen. CompositionLocal
 * eikä parametri, koska ehto on koko laudan tila ja napit rakennetaan neljässä paikassa.
 */
private val LocalActionsBlocked = compositionLocalOf { false }

/** Estetyn napin himmennys; värit pysyvät omina, jotta peruuttamaton erottuu yhä. */
private fun Modifier.blockedAlpha(blocked: Boolean): Modifier =
    if (blocked) alpha(BLOCKED_ALPHA) else this

private const val BLOCKED_ALPHA = 0.38f

/** Sivun lomakkeen nappi; peruuttamaton saa kuutiotealin ja reunuksen (24.8.2026). */
@Composable
private fun SubmitButton(
    label: String,
    modifier: Modifier,
    verified: Boolean,
    onPress: (String, Boolean) -> Unit,
) {
    val irreversible = label in IRREVERSIBLE_SUBMITS
    val blocked = LocalActionsBlocked.current
    val panel = LocalPanelLook.current
    val contentColor = if (irreversible) panel.irreversible else panel.onButton
    Button(
        onClick = { onPress(label, verified) },
        modifier = modifier.blockedAlpha(blocked).decoButtonLine(),
        enabled = !blocked,
        shape = boardButtonShape(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        // Peruuttamattoman teon väri on kuutioteal eikä punainen (24.8.2026). Estetyn napin
        // värit ovat samat ja himmennys tulee alphasta, ks. [blockedAlpha].
        colors = ButtonDefaults.buttonColors(
            containerColor = panel.button,
            contentColor = contentColor,
            disabledContainerColor = panel.button,
            disabledContentColor = contentColor,
        ),
        border = if (irreversible) BorderStroke(DgBoard.OUTLINE, panel.irreversibleBorder) else null,
    ) {
        // **Vain ensisijainen teko on lihava** (Tommin valinta 20.9.2026, `docs/UI.md` ›
        // Sivupaneelin napit): paino kantaa hierarkiaa, kun kaikki muut napit ovat mediumia.
        // Peruuttamaton teko erottuu jo värillä ja reunuksella, joten se ei tarvitse painoa.
        Text(
            text = submitText(label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (irreversible) FontWeight.Medium else FontWeight.Bold,
        )
    }
}

/**
 * Kaari nappien paikalla pyynnön ajaksi, ks. [actionItems]. Nappien kokoinen laatikko,
 * jotta rivi tai pino ei vaihda korkeuttaan kun napit katoavat ja palaavat.
 *
 * **Liukuväri eikä yksi väri** (Tommin tarkennus 16.9.2026 illalla kuuden ottelun session
 * jälkeen: *"latauskaari kaipaa väriä"*, ja vaihtoehdoista *"ajattelin gradienttia väriä"*).
 * Materialin `CircularProgressIndicator` on yksivärinen, joten kaari piirretään itse:
 * pyörivä pyyhkäisyliukuväri läpinäkyvästä kiilan oranssin ja nappulan kerman kautta
 * kuution vaaleaan tealiin, eli laudan omat värit järjestyksessä. Häntä on läpinäkyvä,
 * jotta kaari näyttää liikkeen suunnan. Kierros sekunnissa, sama tahti kuin Materialilla.
 *
 * **Kaari seuraa lautaa** (Tommin valinta 16.9.2026 kahdesta: *"1, kaari seuraa lautaa"*).
 * Kiila ja nappula tulevat [ArcLook]ista eli tämän laudan [BoardLook]ista ja pelaajan omasta
 * nappulaväristä, joten Monte Carlo variantilla ja sivustouskollisella laudalla kaari on
 * sen laudan sävyissä. Teal on kuution väri, ja kuutio on sovelluksen omaa eikä kulje
 * tyylissä, joten se on sama kaikilla.
 */
@Composable
private fun BusyArc(modifier: Modifier, arc: ArcLook) {
    val kulma by rememberInfiniteTransition(label = "busyArc").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "busyArcAngle",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(BUSY_ARC).rotate(kulma)) {
            val viiva = 5.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    0.00f to Color.Transparent,
                    0.35f to arc.wedge,
                    0.70f to arc.checker,
                    1.00f to DgBoard.Palette.CubeSoft,
                ),
                startAngle = 20f,
                sweepAngle = 330f,
                useCenter = false,
                topLeft = Offset(viiva / 2, viiva / 2),
                size = Size(size.width - viiva, size.height - viiva),
                style = Stroke(width = viiva, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Kaaren halkaisija. Oli 26 dp (pienin nappikorkeus 32) 18.9.2026 asti; nyt sama koko kuin
 * [BUSY_OUROBOROS], jotta asetuksen kolme muotoa vertautuvat samassa mitassa (Tommin kysymys
 * *"onko latauskaari samaa kokoluokkaa kuin uudet?"*), ja viiva 3 → 5 dp samassa suhteessa.
 */
private val BUSY_ARC = 60.dp

/**
 * Odotuksen ilmaisin nappien paikalla, ks. [BusyArc]. Kokeilu 18.9.2026 (Tommin kysymys
 * *"onko latauskaarelle näyttävämpiä vaihtoehtoja"* ja tilaus *"rakenna ouroboros ja kuutio
 * pelin väreillä, android 64:n tilalle"*): kolme muotoa saman kytkimen takana. Kytkin oli
 * ensin vakio, ja samana iltana siitä tuli laiteasetus [BusyStyle] (Tommin tilaus
 * *"haluaisin busy-style asetuksen sovellukseen"*), joka tuodaan tänne [LocalBusyStyle]lla
 * jotta sitä ei tarvitse kuljettaa viiden funktion läpi kuten `arc`ia.
 */
val LocalBusyStyle = compositionLocalOf { BusyStyle.ARC }

/**
 * Sama ilmaisin tyhjän ruudun keskellä, laudan ulkopuolella X-22:n väreissä koska
 * luettelolla ei ole lautatyyliä (21.9.2026, Tommin tilaus *"asetus yltää niihin asti"*).
 * Korvaa Materialin `CircularProgressIndicator`in ensilatauksissa; Refreshin 4 dp:n
 * vaakapalkki ([ProgressSlot]) jää, koska sen paikka on mitoitettu nytkähdyksen takia.
 */
@Composable
fun BusyCentered(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BusyIndicator(Modifier, ArcLook.X22, track = Palette.Frame)
    }
}

/**
 * [track] on ääretön-käärmeen radan väri: laudan lokerossa reunaviiva, laudan ulkopuolella
 * kehyksen puu, koska reunaviiva ei erottunut luettelon taustasta (kuoritodennus
 * 21.9.2026 `07-zoom.png`, Tommin päätös *"vaihda rata kehyksen väriin laudan ulkopuolella"*).
 */
@Composable
fun BusyIndicator(modifier: Modifier, arc: ArcLook, track: Color = Palette.Outline) {
    val chosen = LocalBusyStyle.current
    // Satunnainen arpoo kerran odotusta kohden: `remember` elää niin kauan kuin ilmaisin on
    // ruudulla, ja seuraava odotus koostaa sen uudelleen.
    val style = remember(chosen) { if (chosen == BusyStyle.RANDOM) nextRandomBusy() else chosen }
    BusyShape(modifier, style, arc, track)
}

/** Edellinen arvottu muoto, jotta sama ei tule kahdesti peräkkäin. Prosessin muisti riittää. */
private var lastRandomBusy: BusyStyle? = null

private fun nextRandomBusy(): BusyStyle =
    BusyStyle.shapes.filter { it != lastRandomBusy }.random().also { lastRandomBusy = it }

@Composable
private fun BusyShape(modifier: Modifier, style: BusyStyle, arc: ArcLook, track: Color) = if (LocalBusyDeco.current) {
    // Deco-viimeistely omilla väreillään, sama muoto (Tommi 22.9.2026), ks. `BusyDeco.kt`.
    BusyDeco(modifier, style)
} else when (style) {
    BusyStyle.ARC -> BusyArc(modifier, arc)
    BusyStyle.OUROBOROS -> BusyOuroboros(modifier, arc)
    BusyStyle.CUBE -> BusyCube(modifier)
    BusyStyle.INFINITY -> BusyInfinity(modifier, arc, track)
    BusyStyle.HOURGLASS -> BusyHourglass(modifier, arc, track)
    BusyStyle.LAUREL_GROW -> BusyLaurel(modifier, grow = true)
    BusyStyle.LAUREL_SHIMMER -> BusyLaurel(modifier, grow = false)
    BusyStyle.RANDOM -> error("RANDOM ratkaistaan BusyIndicatorissa")
}

/**
 * Tiimalasi (Tommin valinta 22.9.2026 chatin neljästä ehdokkaasta: *"H, mutta pyöristä
 * tiimalasia ja käytä laudan värejä"*). Lasi on kaksi pyöreää kupua kapealla kaulalla
 * radan värillä kuten [BusyInfinity]llä, päätylaudat parittoman kiilan väriä ja hiekka oman
 * nappulan väriä.
 *
 * **Hiekka eikä vesi** (Tommin havainnot mockupista: *"valuminen näyttää vedeltä"* ja
 * *"hiekka kasautuu"*). Virta on erillisiä jyviä jotka kiihtyvät pudotessaan, eikä yhtenäinen
 * viiva. Pohjalle kasvaa keko jonka huippu on kaulan alla, ja yläkuvun pintaan painuu kuoppa
 * kaulan kohdalle. Kierroksen lopussa lasi kääntyy puoli kierrosta, ja käännetty lasi on
 * sama kuva kuin kierroksen alku, joten saumaa ei näy.
 */
@Composable
private fun BusyHourglass(modifier: Modifier, arc: ArcLook, track: Color) {
    val q by rememberInfiniteTransition(label = "hourglass").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(HOURGLASS_CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "hourglassPhase",
    )
    val valuu = 0.85f
    val f = kotlin.math.min(q / valuu, 1f)
    val kaanto = if (q > valuu) {
        val u = (q - valuu) / (1f - valuu)
        // Hidas alku ja loppu, jotta käännös näyttää kädellä tehdyltä.
        180f * (0.5f - 0.5f * kotlin.math.cos(u * Math.PI).toFloat())
    } else {
        0f
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(BUSY_HOURGLASS_W, BUSY_HOURGLASS_H).rotate(kaanto)) {
            val c = center
            val hh = size.height * 0.40f
            val w = size.width * 0.40f
            val n = size.width * 0.055f
            val lasi = Path().apply {
                moveTo(c.x - n, c.y)
                cubicTo(c.x - n, c.y - hh * 0.30f, c.x - w, c.y - hh * 0.35f, c.x - w, c.y - hh * 0.72f)
                cubicTo(c.x - w, c.y - hh, c.x - w * 0.55f, c.y - hh, c.x, c.y - hh)
                cubicTo(c.x + w * 0.55f, c.y - hh, c.x + w, c.y - hh, c.x + w, c.y - hh * 0.72f)
                cubicTo(c.x + w, c.y - hh * 0.35f, c.x + n, c.y - hh * 0.30f, c.x + n, c.y)
                cubicTo(c.x + n, c.y + hh * 0.30f, c.x + w, c.y + hh * 0.35f, c.x + w, c.y + hh * 0.72f)
                cubicTo(c.x + w, c.y + hh, c.x + w * 0.55f, c.y + hh, c.x, c.y + hh)
                cubicTo(c.x - w * 0.55f, c.y + hh, c.x - w, c.y + hh, c.x - w, c.y + hh * 0.72f)
                cubicTo(c.x - w, c.y + hh * 0.35f, c.x - n, c.y + hh * 0.30f, c.x - n, c.y)
                close()
            }
            val hiekka = arc.checker
            clipPath(lasi) {
                // Yläkupu: pinta laskee kaulaa kohti ja painuu keskeltä kuopalle.
                val jaljella = 1f - f
                if (jaljella > 0.01f) {
                    val pinta = c.y - hh * 0.80f * kotlin.math.sqrt(jaljella)
                    val kuoppa = hh * 0.14f * kotlin.math.min(f * 6f, 1f) * kotlin.math.sqrt(jaljella)
                    drawPath(
                        Path().apply {
                            moveTo(c.x - w, pinta)
                            quadraticTo(c.x - w * 0.25f, pinta, c.x, pinta + kuoppa)
                            quadraticTo(c.x + w * 0.25f, pinta, c.x + w, pinta)
                            lineTo(c.x + w, c.y)
                            lineTo(c.x - w, c.y)
                            close()
                        },
                        hiekka,
                    )
                }
                // Alakupu: keko, jonka rinne loivenee ja pohja nousee hiekan kertyessä.
                val pohja = c.y + hh - hh * 0.55f * f
                val huippu = pohja - hh * 0.28f * kotlin.math.min(f * 3f, 1f)
                val leveys = w * (0.35f + 0.65f * kotlin.math.min(f * 2.5f, 1f))
                drawPath(
                    Path().apply {
                        moveTo(c.x - w, c.y + hh)
                        lineTo(c.x - w, pohja)
                        lineTo(c.x - leveys, pohja)
                        quadraticTo(c.x - leveys * 0.2f, huippu, c.x, huippu)
                        quadraticTo(c.x + leveys * 0.2f, huippu, c.x + leveys, pohja)
                        lineTo(c.x + w, pohja)
                        lineTo(c.x + w, c.y + hh)
                        close()
                    },
                    hiekka,
                )
                // Virta: erilliset jyvät, jotka kiihtyvät kaulasta keon huippuun.
                if (q < valuu) {
                    val jyvia = 9
                    val matka = huippu - c.y
                    val r = size.width * 0.028f
                    for (i in 0 until jyvia) {
                        val t = ((q * HOURGLASS_CYCLE_MS / HOURGLASS_GRAIN_MS) + i / jyvia.toFloat()) % 1f
                        val sivu = ((i * 37) % 7 - 3) / 3f * n * 0.45f
                        drawCircle(hiekka, radius = r, center = Offset(c.x + sivu, c.y + matka * t * t))
                    }
                }
            }
            drawPath(lasi, track, style = Stroke(width = size.width * 0.06f, cap = StrokeCap.Round))
            val lauta = Size(size.width * 0.96f, size.height * 0.08f)
            val kulma = androidx.compose.ui.geometry.CornerRadius(lauta.height / 2f)
            drawRoundRect(arc.wedgeOdd, Offset(c.x - lauta.width / 2f, c.y - hh - lauta.height * 0.9f), lauta, kulma)
            drawRoundRect(arc.wedgeOdd, Offset(c.x - lauta.width / 2f, c.y + hh - lauta.height * 0.1f), lauta, kulma)
        }
    }
}

/** Kierros ja käännös yhteensä 2,4 s, jolla lyhyessäkin odotuksessa ehtii valua näkyvästi. */
private const val HOURGLASS_CYCLE_MS = 2400

/** Yhden jyvän putoamisaika kaulasta kekoon. */
private const val HOURGLASS_GRAIN_MS = 260f

private val BUSY_HOURGLASS_W = 42.dp

private val BUSY_HOURGLASS_H = 62.dp

/**
 * Häntäänsä syövä käärme kiertää kehää (Tommin toive 18.9.2026: *"ouroboros!"*). Ruumis
 * paksunee hännästä päähän ja väri kulkee kiilan parillisesta sävystä parittomaan ja
 * takaisin, eli laudan kahdella kiilavärillä; suomut ovat reunaviivan tummia pisteitä ja
 * silmät kuution vaaleaa tealia. Muoto on sama kehä kuin [BusyArc]illa, joten se kestää
 * 26 dp:n koon: pää on siinä noin 4 dp.
 */
@Composable
private fun BusyOuroboros(modifier: Modifier, arc: ArcLook) {
    val kulma by rememberInfiniteTransition(label = "ouroboros").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        // Kierros 960 ms. Oli 1200, ja 22.9.2026 Tommi pyysi *"käärmeitä 25%"* nopeammaksi:
        // nopeus × 1,25 eli kesto / 1,25.
        animationSpec = infiniteRepeatable(tween(960, easing = LinearEasing), RepeatMode.Restart),
        label = "ouroborosAngle",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(BUSY_OUROBOROS).rotate(kulma)) {
            val s = size.minDimension
            val r = s * 0.36f
            val c = center
            val n = 48
            val len = 315f
            for (i in 0 until n) {
                val t = i / n.toFloat()
                val a0 = len * t
                val w = s * (0.05f + 0.11f * t)
                val col = if (t < 0.5f) {
                    lerp(arc.wedge, arc.wedgeOdd, t * 2f)
                } else {
                    lerp(arc.wedgeOdd, arc.wedge, (t - 0.5f) * 2f)
                }
                drawArc(
                    color = col,
                    startAngle = a0,
                    sweepAngle = len / n + 1.5f,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(2f * r, 2f * r),
                    style = Stroke(width = w, cap = StrokeCap.Round),
                )
                if (i % 4 == 2 && t > 0.15f) {
                    val am = Math.toRadians((a0 + len / n / 2f).toDouble())
                    drawCircle(
                        color = Palette.Outline,
                        radius = w * 0.18f,
                        center = Offset(c.x + kotlin.math.cos(am).toFloat() * r, c.y + kotlin.math.sin(am).toFloat() * r),
                    )
                }
            }
            val ah = Math.toRadians(len.toDouble())
            val head = Offset(c.x + kotlin.math.cos(ah).toFloat() * r, c.y + kotlin.math.sin(ah).toFloat() * r)
            val hs = s * 0.17f
            drawRotate(degrees = len + 90f, pivot = head) {
                translate(left = head.x, top = head.y) {
                    val skull = Path().apply {
                        moveTo(-hs * 0.55f, -hs * 0.2f)
                        quadraticTo(0f, -hs * 1.05f, hs * 0.55f, -hs * 0.2f)
                        quadraticTo(hs * 0.3f, hs * 0.6f, 0f, hs * 0.75f)
                        quadraticTo(-hs * 0.3f, hs * 0.6f, -hs * 0.55f, -hs * 0.2f)
                        close()
                    }
                    drawPath(skull, arc.wedge)
                    val eyes = listOf(Offset(-hs * 0.25f, -hs * 0.25f), Offset(hs * 0.25f, -hs * 0.25f))
                    eyes.forEach { drawCircle(Palette.Outline, radius = hs * 0.11f, center = it) }
                    eyes.forEach { drawCircle(Palette.CubeSoft, radius = hs * 0.05f, center = it) }
                }
            }
        }
    }
}

/**
 * Vierivä tuplauskuutio (Tommin toive 18.9.2026: *"vierivä tuplauskuutio, missä yksi tahko
 * on androidin pää"*, ja tarkennus *"android 64:n tilalle"*). Kuusi tahkoa järjestyksessä
 * robotti, 2, 4, 8, 16 ja 32, samalla piirrolla kuin laudan kuutio ([CubeFace],
 * [CubeRobotFace]). Käännös on arvottu heitto kolmella akselilla ([rememberTumble],
 * 24.9.2026): tahko kääntyy reunalleen X- tai Y-akselin ympäri ja avautuu seuraavana.
 *
 * Huomio kaanonista: laudan kuutiossa robotti on ykköspinta eli *kukaan ei ole tuplannut*
 * (26.8.2026). Tässä se on 64:n paikalla Tommin sanoin, ja ero on tarkoituksellinen: tämä
 * ei ole kuution arvo vaan odotuksen kuva.
 *
 * **Kierros alkaa robotista** (Tommin päätös 18.9.2026 pelisession jälkeen, *"kierros alkaa
 * robotista"*). Ensin robotti oli viimeinen tahko, ja pelisessiossa 18.9. (sessio-18-9-ilta2)
 * Tommi ei nähnyt sitä kertaakaan: kierros on 6 × 840 ms = 5,0 s, ja odotukset olivat
 * 0,3–3,3 s, joten robotin vuoro ei koskaan tullut. Ensimmäisenä se näkyy jokaisessa
 * odotuksessa, myös puolen sekunnin. Tahkojen järjestys on siis robotin jälkeen nouseva,
 * ja 64:n paikka on 32:n jälkeen kierroksen alussa.
 */
@Composable
private fun BusyCube(modifier: Modifier) {
    // 467 ms tahkoa kohti. Välillä 840 (18.9.2026 iltapäivä, *"hidasta kuutiota 20%"*),
    // ja saman illan pelisession jälkeen takaisin: *"hidastus oli virhe, nopeuta 25% eli
    // alkuperäiseen tahtiin"*. Peruste on peli eikä kuoriajo: pelissä odotukset ovat
    // 0,3–3 s, ja hitaampi tahko ehtii näyttää vähemmän. 22.9.2026 *"nopeuta
    // tuplauskuutiota 50%"*: nopeus × 1,5 eli 700 → 467 ms tahkolta, kierros 2,8 s.
    //
    // **Käännös on heitto kolmella akselilla 24.9.2026 alkaen** ([rememberTumble]). Ennen sitä
    // se oli litistys `scaleX` ja 17 asteen `rotationZ`, sama joka tahkolla.
    val tumble by rememberTumble(faces = 6, stepMs = 467)
    val density = LocalDensity.current
    val face = tumble.face
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .requiredSize(BUSY_CUBE)
                .graphicsLayer { tumble(tumble, density) }
                .clip(RoundedCornerShape(BUSY_CUBE * DgBoard.CORNER_PER_SIDE))
                .background(Palette.Cube),
            contentAlignment = Alignment.Center,
        ) {
            if (face == 0) {
                CubeRobotFace(BUSY_CUBE)
            } else {
                // Tahko 1 on 2, tahko 5 on 32. Numeron väri arvon mukaan kuten laudalla
                // (Palette.onCube, 21.9.2026).
                val value = 1 shl face
                val text = value.toString()
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = cubeFontSize(text.length, BUSY_CUBE),
                    lineHeight = cubeFontSize(text.length, BUSY_CUBE),
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.Bold,
                    color = Palette.onCube(value),
                )
            }
        }
    }
}

/**
 * Kuution sivu ja käärmeen kehä nappilokerossa. Lokero on yhden napin korkuinen, mutta
 * odotuksen aikana kaikki napit ovat poissa, joten ilmaisin saa vuotaa lokeron yli
 * (`requiredSize`, Tommin tarkennus 18.9.2026 ensimmäisen laiteajon jälkeen: *"animaatiot
 * saisi olla isompia"*; 26 ja 22 dp olivat laitteella täpliä). Kuutio on kehää pienempi,
 * koska neliö näyttää kehää isommalta.
 */
private val BUSY_CUBE = 52.dp

private val BUSY_OUROBOROS = 60.dp

/**
 * Käärme ääretön-merkillä (Tommin tilaus 21.9.2026: *"toinen ouroboros, tässä käärme olisi
 * ääretön symboli"*, kolmesta chatin vaihtoehdosta B eli lyhyt käärme näkyvällä radalla,
 * A:n värityksellä). Rata on Bernoullin lemniskaatta reunaviivan värillä, ja käärme peittää
 * siitä puolet: ruumis paksunee hännästä päähän ja väri kulkee kiilan parillisesta
 * parittomaan ja takaisin kuten [BusyOuroboros]illa, suomut ja pää samat. Käärme ei pyöri
 * vaan liukuu, joten pää käy vuorotellen kummassakin lenkissä ja ristikohdassa ruumis kulkee
 * itsensä yli.
 *
 * Pisteet ovat tasavälein kaarenpituuden mukaan eikä parametrin, koska parametrin mukaan
 * käärme hidastuisi lenkkien päissä ja kiihtyisi ristikohdassa. Muoto lasketaan kerran
 * yksikkömitassa ([INFINITY_TRACK]) ja skaalataan piirrossa.
 */
@Composable
private fun BusyInfinity(modifier: Modifier, arc: ArcLook, track: Color) {
    val vaihe by rememberInfiniteTransition(label = "infinity").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        // Kierros 1440 ms. Oli 1800, nopeutettu 25 % samalla pyynnöllä kuin [BusyOuroboros].
        animationSpec = infiniteRepeatable(tween(1440, easing = LinearEasing), RepeatMode.Restart),
        label = "infinityPhase",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(BUSY_INFINITY_W, BUSY_INFINITY_H)) {
            val h = size.height
            val a = size.width * 0.46f
            val c = center
            val pts = INFINITY_TRACK
            val n = pts.size
            fun at(i: Int): Offset {
                val p = pts[((i % n) + n) % n]
                return Offset(c.x + p.x * a, c.y + p.y * a)
            }
            val rata = Path().apply {
                val p0 = at(0)
                moveTo(p0.x, p0.y)
                for (i in 1 until n) {
                    val p = at(i)
                    lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(rata, track, style = Stroke(width = h * 0.07f, cap = StrokeCap.Round))
            val start = (vaihe * n).toInt()
            val len = n / 2
            for (i in 0 until len) {
                val t = i / len.toFloat()
                val p = at(start + i)
                val w = h * (0.07f + 0.13f * t)
                val col = if (t < 0.5f) {
                    lerp(arc.wedge, arc.wedgeOdd, t * 2f)
                } else {
                    lerp(arc.wedgeOdd, arc.wedge, (t - 0.5f) * 2f)
                }
                drawCircle(color = col, radius = w / 2f, center = p)
                if (i % 6 == 3 && t > 0.15f) {
                    drawCircle(color = Palette.Outline, radius = w * 0.18f, center = p)
                }
            }
            val head = at(start + len)
            val back = at(start + len - 4)
            val suunta = Math.toDegrees(kotlin.math.atan2((head.y - back.y).toDouble(), (head.x - back.x).toDouble())).toFloat()
            // 0,21 → 0,25 ensimmäisen laiteajon jälkeen (Tommi 21.9.2026: *"suurenna päätä 20%"*).
            val hs = h * 0.25f
            drawRotate(degrees = suunta + 90f, pivot = head) {
                translate(left = head.x, top = head.y) {
                    val skull = Path().apply {
                        moveTo(-hs * 0.55f, -hs * 0.2f)
                        quadraticTo(0f, -hs * 1.05f, hs * 0.55f, -hs * 0.2f)
                        quadraticTo(hs * 0.3f, hs * 0.6f, 0f, hs * 0.75f)
                        quadraticTo(-hs * 0.3f, hs * 0.6f, -hs * 0.55f, -hs * 0.2f)
                        close()
                    }
                    drawPath(skull, arc.wedge)
                    val eyes = listOf(Offset(-hs * 0.25f, -hs * 0.25f), Offset(hs * 0.25f, -hs * 0.25f))
                    eyes.forEach { drawCircle(Palette.Outline, radius = hs * 0.11f, center = it) }
                    eyes.forEach { drawCircle(Palette.CubeSoft, radius = hs * 0.05f, center = it) }
                }
            }
        }
    }
}

/** Kahdeksikko on leveä ja matala, joten oma mitta: korkeus kuution luokkaa. */
private val BUSY_INFINITY_W = 84.dp
private val BUSY_INFINITY_H = 48.dp

/**
 * Lemniskaatan 240 pistettä tasavälein kaarenpituuden mukaan, yksikkömitassa (a = 1).
 * Parametrimuoto x = cos t / (1 + sin²t), y = sin t cos t / (1 + sin²t); tiheä näyte ja
 * uudelleenjako kumulatiivisen pituuden mukaan.
 */
private val INFINITY_TRACK: List<Offset> by lazy {
    val raw = (0..2000).map { i ->
        val t = i / 2000.0 * 2 * Math.PI
        val d = 1 + kotlin.math.sin(t) * kotlin.math.sin(t)
        Offset((kotlin.math.cos(t) / d).toFloat(), (kotlin.math.sin(t) * kotlin.math.cos(t) / d).toFloat())
    }
    val cum = FloatArray(raw.size)
    for (i in 1 until raw.size) cum[i] = cum[i - 1] + (raw[i] - raw[i - 1]).getDistance()
    val total = cum.last()
    val n = 240
    var j = 0
    List(n) { k ->
        val s = k / n.toFloat() * total
        while (cum[j + 1] < s) j++
        val f = (s - cum[j]) / (cum[j + 1] - cum[j])
        raw[j] + (raw[j + 1] - raw[j]) * f
    }
}

/** Kaaren kaksi laudasta luettavaa väriä: kiilan oranssi ja oma nappula, ks. [BusyArc]. */
data class ArcLook(val wedge: Color, val checker: Color, val wedgeOdd: Color) {
    companion object {
        val X22 = ArcLook(DgBoard.Palette.WedgeEven, DgBoard.Palette.CheckerSelf, DgBoard.Palette.WedgeOdd)
    }
}

/** Tämän laudan värit kaarelle: kiila, oma nappula ja pariton kiila, ks. [ArcLook]. */
private fun arcLookOf(roles: BoardRoles) = ArcLook(roles.look.wedgeEven, roles.selfPaint().fill, roles.look.wedgeOdd)

/**
 * Vahvistusruudut tekstineen (`Verify Double`, `Verify Accept`, `Verify Decline`), yksi rivi
 * ruutua kohti. Yhdellä ruudulla tämä on sama rivi kuin ennen 25.9.2026; kahdella rivit ovat
 * allekkain nappien järjestyksessä, ks. [verifyBoxesFor].
 */
@Composable
private fun VerifyRows(verify: VerifyBox, modifier: Modifier) {
    if (verify.boxes.size == 1) {
        VerifyRow(verify.boxes.single(), modifier, verify)
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            verify.boxes.forEach { box -> VerifyRow(box, Modifier, verify) }
        }
    }
}

/** Yksi vahvistusruutu tekstineen. */
@Composable
private fun VerifyRow(
    box: String,
    modifier: Modifier,
    verify: VerifyBox,
    /** Pelkkä `Verify`, kun ruutu on oman nappinsa rivillä ja nappi sanoo loput. */
    short: Boolean = false,
) {
    // Ruutu seuraa nappien värivalintaa: kerma on kaistaa vasten 6,32:1.
    // Rako tekstiin on pinon oma väli (Tommin havainto 15.9.2026 yöllä:
    // *"checkbox ja verify ovat kiinni toisissaan"*): 20 dp:n ruudulla ei ole
    // Materialin kosketusmarginaalia, joka muuten pitäisi tekstin irti.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Checkbox(
            checked = box in verify.checked,
            onCheckedChange = { verify.onChange(box, it) },
            modifier = Modifier.size(VERIFY_BOX),
            colors = CheckboxDefaults.colors(
                checkedColor = LocalPanelLook.current.outlineButton,
                uncheckedColor = LocalPanelLook.current.outlineButton,
                checkmarkColor = LocalPanelLook.current.background,
            ),
        )
        Text(
            text = if (short) stringResource(R.string.board_verify_short) else stringResource(R.string.board_verify, box),
            style = MaterialTheme.typography.labelSmall,
            color = LocalPanelLook.current.text,
        )
    }
}

/**
 * Kuutioteon ryhmä paneelipinossa: peruuttamattomat napit ja niiden vahvistusruutu yhtenä
 * sarakkeena.
 *
 * **Kehys poistui 20.9.2026** (Tommin valinta kankaalta, `docs/UI.md` › Sivupaneelin
 * napit). Kehys (15.9.2026) sanoi ruudun kuuluvuuden nappiin alueena, mutta se oli
 * laatikko laatikossa: tealreunus napissa ja toinen ryhmän ympärillä. Kuutioteal on napissa
 * itsessään, ja ruutu on napin alla samassa vasemmassa laidassa kuten 14.9.2026 (*"Tasaa
 * Verify-checkboxit vasempaan laitaan"*). Kehyksen keskitys poistui kehyksen mukana, koska
 * se oli kortin ominaisuus. Ryhmä on yhä yksi pinon alkio, jotta ahtaan paneelin rivi
 * (16.9.2026, nappi ja ruutu rinnakkain) säilyy.
 */
@Composable
private fun VerifyGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start,
        content = content,
    )
}

/**
 * Lomakkeessa tasan kaksi nappia, toinen peruttava ja toinen peruuttamaton vahvistusruudun
 * kanssa (`Roll Dice` ja `Double`). Pystylaudalla ne ovat samalla rivillä (valinta C
 * 24.9.2026). `Skip Game` oli silloin peruttavan alla 26.9.2026 asti, ks. `docs/UI.md`.
 */
internal fun BoardForm?.pairsRollAndDouble(): Boolean =
    this != null && verify != null && submits.size == 2 &&
        submits.count { it in IRREVERSIBLE_SUBMITS } == 1

/** Yksi toimintorivin tai -pinon alkio: nappi tai peruuttamattoman teon väli. */
private sealed interface ActionItem {
    class Content(val body: @Composable () -> Unit) : ActionItem
}

/**
 * Sivun teot yhtenä listana, jotta rivi ja pino piirtävät täsmälleen saman sisällön samassa
 * järjestyksessä. Järjestys on sivun järjestys (22.8.2026): lomakkeen napit, vahvistusruutu,
 * peruminen, sivun muut komennot, `Skip Game`. Kaikki alla olevat päätökset ovat vanhoja ja
 * niiden perustelut on kirjattu [MiddleActionsRow]n dokumenttiin ja `docs/UI.md`:hen.
 */
@Composable
private fun actionItems(
    board: BoardState,
    form: BoardForm?,
    buttonModifier: Modifier,
    /**
     * Vahvistusruudun rivin modifier kehyksettömänä. Paneelipino antaa koko leveyden, jolloin
     * ruutu tasautuu vasempaan laitaan nappien reunan kanssa (Tommin pyyntö 14.9.2026:
     * *"Tasaa Verify-checkboxit vasempaan laitaan"*); keskikaistalla rivi on oman
     * sisältönsä levyinen kuten ennenkin, koska siellä kohteet ovat vierekkäin. Kehyksen
     * sisällä tätä ei käytetä, ks. [VerifyGroup].
     */
    verifyRowModifier: Modifier = Modifier,
    /** Paneelipino kehystää peruuttamattomat napit ja vahvistusruudun yhdeksi, ks. [VerifyGroup]. */
    frameVerify: Boolean = false,
    /**
     * Ahdas paneeli (Tommin päätös 16.9.2026: *"checkbox, verify ja double-nappi samalla
     * rivillä"*): kun kehyksessä on yksi nappi, nappi ja vahvistusrivi ovat rinnakkain.
     * Kahden napin kehys (`Accept`, `Decline`) pysyy pinona, koska rivi ei mahdu paneeliin.
     */
    compactFrame: Boolean = false,
    /**
     * Vahvistusruutu nappien oikealle samalle riville napin määrästä ja paneelin
     * ahtaudesta riippumatta (Tommin tilaus 23.9.2026 pystytilaan: *"siirrä Verify Double
     * (ja mahdollisesti muut verify) myös tabletin näyttökoossa sitä vastaavan napin
     * oikealle puolelle"*). Laudan alla leveyttä on, joten kahden napin kehyskin mahtuu.
     */
    verifyBeside: Boolean = false,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    /** Ruudut ja niiden rastit, ks. [VerifyBox]. */
    verify: VerifyBox,
    showSkip: Boolean,
    /**
     * Pyyntö on kesken (`refreshing`). Silloin lista on pelkkä kaari nappien paikalla.
     *
     * **Napit pois heti ja kaari odotuksen ajaksi** (Tommin tilaus 16.9.2026, kokeilu
     * kitkakohtaan). Vertailumittaus 6 samana iltana osoitti vasteen eron DG Mobileen
     * 0,1 s:ksi tekoa kohti, mutta mittaus 3 yöllä eron palautteessa: DG Mobile tyhjentää
     * napit painalluskehyksessä ja pyörittää kaarta koko odotuksen, tämä sovellus näytti
     * ripplen 0,3 s ja seisoi sitten 0,4–0,7 s liikkumatta (yläreunan neljän dp:n palkki
     * ei lue odotukseksi laudan äärestä). Napin katoaminen on samalla toinen portti
     * kaksoisnapautukselle `inFlight`in rinnalla, mutta se ei ole syy: syy on että
     * liikkumaton odotus tuntuu pidemmältä kuin liikkuva. Ks. `docs/AVOIMET.md`, kitkakohta.
     */
    busy: Boolean = false,
    /** Kaaren värit tältä laudalta, ks. [BusyArc]. */
    arc: ArcLook = ArcLook.X22,
): List<ActionItem> = buildList {
    if (busy) {
        add(ActionItem.Content { BusyIndicator(buttonModifier, arc) })
        return@buildList
    }
    val submits = form?.submits.orEmpty()
    // **Kuutioteko on yksi kehystetty kokonaisuus pinossa** (Tommin valinta 15.9.2026 yöllä
    // neljästä vaihtoehdosta: *"onko parempaa tapaa osoittaa sen kuuluvuutta yllä olevaan"*).
    // Peruuttamattomat napit ja niiden vahvistusruutu piirretään saman kehyksen sisään,
    // jolloin ruudun kuuluvuus nappiin luetaan alueesta eikä rivin paikasta. Keskikaistalla
    // kohteet ovat vierekkäin eikä kehystä ole; siellä ruutu on oma kohteensa kuten ennen.
    val hasVerify = form != null && verify.boxes.isNotEmpty()
    val framed = frameVerify && hasVerify
    val firstIrreversible = submits.indexOfFirst { it in IRREVERSIBLE_SUBMITS }
    // **Pystytilassa peruttava ja peruuttamaton nappi samalla rivillä** (Tommin valinta C
    // 24.9.2026: *"Roll Dice ja Double eivät ala samasta x-koordinaatista ... älyttömän leveä
    // nappi"*). Kaksi yhtä leveää saraketta sivun järjestyksessä, ja vahvistusruutu on
    // peruuttamattoman napin alla sen vasemmassa reunassa sivun omalla tekstillään (Tommin
    // tarkennus samana iltana: *"asemoi Verify double Double-painikkeen mukaan"*, sama
    // vasen tasaus kuin 14.9.2026). Koskee vain tapausta
    // jossa nappeja on tasan kaksi ja toinen on peruttava (`Roll Dice` ja `Double`);
    // `Accept` ja `Decline` jäävät alla olevaan kehykseen.
    if (verifyBeside && framed && form.pairsRollAndDouble()) {
        add(
            ActionItem.Content {
                // Kaksi riviä eikä kaksi saraketta (Tommin havainto 24.9.2026: *"tekstit
                // eivät ole samalla korkeudella"*). Sarakkeina silloinen `Skip Game` ja vahvistusruutu
                // olivat kumpikin oman napin alla ylälaitaan tasattuina, ja eri korkuiset
                // kohteet jättivät tekstit eri korkeudelle. Toisella rivillä ne keskitetään
                // pystysuunnassa yhteen, ja sarakkeet pysyvät yhtä leveinä.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        submits.forEach { label ->
                            SubmitButton(label, Modifier.weight(1f).then(buttonModifier), verify.isChecked(label), onPress)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        submits.forEach { label ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                // Peruttavan napin alla ei ole mitään 26.9.2026 alkaen (Tommin
                                // valinta A): `Skip Game` oli siinä 24.9. alkaen, mutta se erotti
                                // sen `Mark position`ista, ks. `docs/UI.md` › Pystylaudan lisät.
                                if (label in IRREVERSIBLE_SUBMITS) {
                                    VerifyRows(verify, Modifier)
                                }
                            }
                        }
                    }
                }
            },
        )
    } else submits.forEachIndexed { index, label ->
        val irreversible = label in IRREVERSIBLE_SUBMITS
        // Peruuttamattoman napin lisäväli poistui 24.9.2026 (Tommin päätös, C3 kumottu):
        // suoja on kuutiokehys ja vahvistusruutu, ja väli luettiin pelissä epätasaisuudeksi.
        // Järjestys pysyy sivun järjestyksenä.
        when {
            framed && irreversible && index == firstIrreversible -> add(
                ActionItem.Content {
                    val framedLabels = submits.filter { it in IRREVERSIBLE_SUBMITS }
                    VerifyGroup {
                        if (verifyBeside || (compactFrame && framedLabels.size == 1)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                framedLabels.forEach { framedLabel ->
                                    SubmitButton(framedLabel, Modifier.weight(1f).then(buttonModifier), verify.isChecked(framedLabel), onPress)
                                }
                                VerifyRows(verify, Modifier)
                            }
                        } else if (compactFrame) {
                            // **Ahtaassa paneelissa kukin ruutu oman nappinsa rivillä** (Tommin
                            // valinta 4.10.2026: *"rastit vain ahtaassa tilassa toiminnon kanssa
                            // samalla rivillä"*). Pixelin vaakapaneelissa `Accept`, `Decline` ja
                            // kaksi ruuturiviä veivät neljä riviä, ja `Skip Game` painui
                            // vastustajan kortin päälle. Selite on lyhyt `Verify`, koska napin nimi
                            // on jo samalla rivillä.
                            framedLabels.forEach { framedLabel ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SubmitButton(framedLabel, Modifier.weight(1f).then(buttonModifier), verify.isChecked(framedLabel), onPress)
                                    verify.boxFor(framedLabel)?.let { box -> VerifyRow(box, Modifier, verify, short = true) }
                                }
                            }
                        } else {
                            framedLabels.forEach { framedLabel ->
                                SubmitButton(framedLabel, buttonModifier, verify.isChecked(framedLabel), onPress)
                            }
                            // Koko leveys, jotta ruutu tasautuu nappien vasempaan laitaan
                            // (20.9.2026); rako nappiin on sama kuin pinon ulkopuolella.
                            VerifyRows(verify, Modifier.fillMaxWidth().padding(top = 6.dp))
                        }
                    }
                },
            )
            framed && irreversible -> Unit // piirretty jo kehyksen sisään
            else -> add(ActionItem.Content { SubmitButton(label, buttonModifier, verify.isChecked(label), onPress) })
        }
    }
    if (hasVerify && !framed) {
        add(ActionItem.Content { VerifyRows(verify, verifyRowModifier) })
    }

    // Peruminen on linkki eikä nappi sivustolla, mutta ruudulla nappi 22.8.2026 alkaen
    // (Tommin valinta): löytyy yhtä helposti kuin lähetys, muttei näytä yhtä painavalta.
    board.undoHref?.let { href ->
        add(
            ActionItem.Content {
                val blocked = LocalActionsBlocked.current
                OutlinedButton(
                    onClick = { onFollow(href) },
                    modifier = buttonModifier.blockedAlpha(blocked).decoButtonLine(),
                    enabled = !blocked,
                    shape = boardButtonShape(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LocalPanelLook.current.outlineButton,
                        disabledContentColor = LocalPanelLook.current.outlineButton,
                    ),
                    border = BorderStroke(DgBoard.OUTLINE, LocalPanelLook.current.outlineButton),
                ) {
                    Text(
                        text = stringResource(R.string.board_undo),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )
    }

    // Sivun muut komennot, esim. `Swap Dice`: nappeja 25.8.2026 alkaen, ero perumiseen
    // värissä. Nimi ja osoite sivulta sellaisinaan, eikä niitä suodateta.
    board.commands.forEach { command ->
        add(
            ActionItem.Content {
                val blocked = LocalActionsBlocked.current
                OutlinedButton(
                    onClick = { onFollow(command.href) },
                    modifier = buttonModifier.blockedAlpha(blocked).decoButtonLine(),
                    enabled = !blocked,
                    shape = boardButtonShape(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LocalPanelLook.current.secondary,
                        disabledContentColor = LocalPanelLook.current.secondary,
                    ),
                    border = BorderStroke(DgBoard.OUTLINE, LocalPanelLook.current.secondary),
                ) {
                    Text(
                        text = command.label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )
    }

    // `Skip Game` viimeisenä sivun komentona, koska se vaihtaa ottelun ja kuluttaa jonoa.
    // Rivillä vain kun sivupaneelia ei ole (27.8.2026).
    if (showSkip) {
        board.skipHref?.let { href ->
            add(ActionItem.Content { SkipGameAction(href, onFollow, buttonModifier) })
        }
    }
}

/**
 * Pelaaja- ja ottelutiedot laudan vasemmalla, pysyvästi näkyvissä.
 *
 * **Paneeli purettiin 9.8.2026 ja palautettiin 10.8.2026, eikä se ole sama paneeli.** Vanha
 * varasi 200 dp ennen laudan mitoitusta, ja se oli laitekohtainen kauppa: Pixel 8a:lla
 * korkeus rajoittaa eli varaus oli ilmainen, pienellä puhelimella leveys rajoittaa ja sama
 * varaus kutisti nappulan 24,1:stä 18,8 dp:hen. Nyt lauta mitoitetaan ensin ja paneeli saa
 * vain sen mikä jää yli, joten kauppaa ei ole: leveys jota lauta ei voi käyttää, on ainoa
 * leveys jonka paneeli ottaa.
 *
 * Syy paluuseen on mitattu: Pixel 8a:n vaakaikkunassa laudalle jää noin 299 dp käyttämätöntä
 * leveyttä, koska nappulan koko tulee korkeudesta (30,0 dp vastaan 34,9 dp jos leveys
 * ratkaisisi). Se näkyi ruudulla mustana, ja Tommi pyysi tiedot siihen.
 *
 * **Vasemmalla eikä oikealla, toisin kuin vanha.** Näytön lova on tällä laitteella vaaka-
 * asennossa vasemmalla, ja paneeli sietää sen paremmin kuin lauta: teksti voi väistää, nopan
 * paikka ei. Sama syy kuin noppien siirrossa keskemmälle, ks. [MiddleStrip].
 *
 * **Sijainti kantaa merkityksen.** Vastustaja ylhäällä ja kirjautunut alhaalla, eli samalla
 * puolella kuin hänen nappulansa. `players` tulee sivun omassa järjestyksessä eikä sitä
 * lajitella: Tommin mittaus 5.8.2026 on että kirjautunut on sivulla aina viimeinen ja
 * vastustaja ensimmäinen, ja sama oletus kantaa [selfCheckerColor]ia.
 */

/**
 * Ottelukortti: turnaus, kierros ja siirtonumero, napautus avaa turnaussivun. Sivupaneelin
 * keskellä vaakatilassa ja ruudun ylälaidassa pystytilassa (Tommin valinta 23.9.2026).
 */
@Composable
private fun MatchCard(
    board: BoardState,
    roundLabel: String?,
    compact: Boolean,
    onOpenPage: (String) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Lohko ei mahdu muuten (9.10.2026, [CenteredOverStack]): kierros siirtyy hännälle kuten
     * ahtaassa paneelissa ja turnauksen nimi mahtuu yhdelle riville.
     */
    tight: Boolean = false,
) {
    // **Ottelutiedot kortissa kuten pelaajat** (Tommin tilaus 8.9.2026 kesken
    // pelisession: *"ottelutietoja voisi ymparoida laatikko, jotta ne erottaisi
    // paremmin toiminnoista kuten pelaajatiedoillakin on"*). Kortti on sama
    // [panelCard] kuin pelaajilla, koska raja kulkee samassa kohdassa: kortissa
    // on sivuston tietoa, kortin ulkopuolella tekoja. Muistutukset ja `Skip Game`
    // jäävät siis kortin alle eivätkä sen sisään.
    // **Ahtaassa paneelissa kierros ja siirto ovat samalla rivillä** (Tommin
    // päätös 16.9.2026: *"ottelukorttia voimme vielä pienentää Round ja Move
    // samalle riville"*), koska kolmirivinen kortti työnsi lisien toisen rivin
    // kortin alle Pixelillä. Tabletilla kortti on ennallaan.
    // **Pituus palasi 24.9.2026** (Tommin valinta: *"turnaukseen pitää saada ottelun pituus
    // näkyville"*), kierroksen ja siirron riville. Sana vaihtui samana iltana otteluluettelon
    // `Length 5`:stä sivuston omaan `5 point match` -muotoon (Tommin tilaus 24.9.2026).
    val moveText = board.moveNumber?.let { stringResource(R.string.board_move_number, it) }
    val lengthText = board.matchLength?.let { stringResource(R.string.board_match_length, it) }
    val roundOnTail = (compact || tight) && roundLabel != null && moveText != null
    val tailParts = listOfNotNull(roundLabel.takeIf { roundOnTail }, lengthText, moveText)
    val tail = tailParts.takeIf { it.isNotEmpty() }?.reduce { a, b ->
        stringResource(R.string.board_round_and_move, a, b)
    }
    val matchLines = listOfNotNull(board.eventName, roundLabel.takeIf { !roundOnTail })
    // **Ottelukortti avaa turnaussivun** (Tommin valinta 14.9.2026 illalla, G4
    // `docs/AUDITOINTI-FOORUMI.md`): sama ele kuin pelaajakortilla. Liigaottelulla
    // sivu ei anna linkkiä, ja silloin kortti ei ole napautettava.
    //
    // **Pituusrivi poistui 14.9.2026** (Tommin valinta, peruste `SUBSTANSSI.md`
    // kohdat 90 ja 92) ja palasi 24.9.2026 kierroksen ja siirron riville, ks. yllä.
    if (matchLines.isNotEmpty() || tail != null) {
        val eventPath = board.eventPath
        // Turnauksen nimi rarity-värissä kierrosmäärän mukaan (2.10.2026): `Round 4/5`
        // kertoo sen kun luettelo tunsi ottelun, muuten luku tulee nähdyistä. Kortti on
        // vaaleassa tilassa vaalea kuten pelaajakortin nimellä ([ratedNameColor]).
        val eventColor = tournamentRarityColor(
            board.eventId,
            board.eventName,
            roundsFromText(roundLabel),
            dark = !LocalPanelLook.current.light,
        )
        Column(
            modifier = modifier
                .then(if (eventPath != null) Modifier.clickable { onOpenPage(eventPath) } else Modifier)
                .panelCard(outline = LocalPanelLook.current.outline, fill = LocalPanelLook.current.card),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            matchLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.labelLarge,
                    color = eventColor?.takeIf { line == board.eventName } ?: LocalPanelLook.current.text,
                    textAlign = TextAlign.Center,
                    maxLines = if (tight) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            tail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalPanelLook.current.secondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SidePanel(
    board: BoardState,
    /** Ahdas paneeli: kortit ilman tyhjää riviä, ks. [PlayerPanelView]. */
    compact: Boolean,
    /** Kierrosrivi, `Round 4/5` kun kokonaismäärä tunnetaan; ks. `BoardUiState.Loaded.roundLabel`. */
    roundLabel: String?,
    roles: BoardRoles,
    pips: PipCheck,
    checkers: CheckerCheck,
    /** Pisteiden esitys: away-luku tai null joka pudottaa sivun omaan pistekenttään. */
    awayOf: (PlayerPanel) -> Int?,
    /** Pelaajakortin napautus avaa profiilin, ks. [PlayerPanelView]. */
    onOpenPage: (String) -> Unit,
    /** Omistettu kuutio omistajan korttiin, tai null kun kuutio on muualla; ks. [PlayerPanelView]. */
    panelCube: Cube? = null,
    onCube: (() -> Unit)? = null,
    cubeReminder: Boolean = false,
    modifier: Modifier = Modifier,
    /**
     * Sovelluksen omat lisät, eli muistutusnapit (27.8.2026, `docs/UI.md`).
     *
     * **Ne ovat täällä eivätkä toimintorivillä, ja se on mitattu päätös.** Rivi on
     * puhelimella ahdas oletusarvoisesti: 24:stä kaapatusta lautasivusta kolme mahtui
     * varmasti ja neljätoista ei mahtunut kummassakaan tapauksessa. Tabletilla rivi mahtuu
     * oletusfontilla muttei isolla, joten ratkaisu on sama molemmilla.
     *
     * Paneeli on samalla se alue jolla ei ole yhtään sivuston komentoa, joten lähteiden ero
     * on nyt fyysinen eikä vain värissä ja järjestyksessä.
     *
     * Tyhjä lohko kun paneelia ei ole tai peli on tunnistamaton; silloin napit piirtyvät
     * toimintoriville kuten ennen, ks. kutsuja.
     */
    actions: @Composable () -> Unit = {},
    /** Onko [actions]-lohkossa sisältöä; ohjaa vain lohkon alaviivaa (20.9.2026). */
    actionsPresent: Boolean = false,
    /**
     * Sivun teot pystypinona ([PanelActionStack]), ottelutietojen alla ja omien lisien
     * yllä (Tommin päätös 2.9.2026). Tyhjä kun paneelia ei ole; silloin napit ovat
     * keskikaistalla kuten 14.8.2026 alkaen.
     */
    formActions: @Composable () -> Unit = {},
    /**
     * Epätosi kun sivun tilarivit ovat keskikaistalla ([stripNotes], [situationSlot]);
     * silloin peruutus, tilanneteksti ja vahdit eivät ole täällä. Tosi on varapaikka.
     */
    showNotes: Boolean = true,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        board.players.getOrNull(0)?.let {
            PlayerPanelView(
                it,
                awayOf(it),
                roles.opponentPaint(),
                other = board.players.getOrNull(1),
                otherAway = board.players.getOrNull(1)?.let { p -> awayOf(p) },
                matchLength = board.matchLength,
                onOpen = it.player.profilePath?.let { path -> { onOpenPage(path) } },
                cube = panelCube?.takeIf { c -> c.position == CubePosition.TOP },
                onDouble = onCube,
                cubeAttention = cubeReminder,
            )
        }

        // **Ottelukortti pelaajakorttien puoliväliin, pino pohjalla siitä riippumatta**
        // (Tommin valinta 14.9.2026 illalla, *"ottelutiedot aina vasemmalle
        // korkeus-keskitettynä"*). Saman päivän aiempi korjaus vei kortin kiinni vastustajan
        // kortin alle, koska kahden joustovälin keskitys hyppi pinon ja pohjarivien mukana.
        // Keskitys palaa, mutta eri mekanismilla: [CenteredOverStack] laskee kortin paikan
        // koko lohkon korkeudesta eikä pinon jälkeen jäävästä tilasta, joten pino ei enää
        // työnnä korttia. Kortti väistää vain jos se muuten osuisi pinoon.
        CenteredOverStack(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            above = {
                // **Muistutukset ja `Skip Game` kortin yläpuolelle** (Tommin tarkennus
                // 14.9.2026 klo 23.03 kaappauksen jälkeen: kortti oli keskitetty yhdessä
                // niiden kanssa eikä yksin, joten se istui keskikohdan yläpuolella). Nyt
                // kortti itse on keskellä ja lisät roikkuvat sen yläreunasta ylöspäin.
                // Pelaajakortit ovat laidoissa ja ne ovat sivuston tietoa, joten sovelluksen
                // omat teot asuvat niiden välissä samassa lohkossa kuin ottelun omat tiedot.
                // **Ahtaassa paneelissa lisät ovat rivissä eivätkä sarakkeessa** (Tommin
                // päätös 16.9.2026 tuplaustarjouksen kaappauksen jälkeen: pino oli 170 dp ja
                // sarake 96, kun tilaa oli 260). `FlowRow` rivittää tarvittaessa kahdelle
                // riville, joten säästö on 28–60 dp lisien määrästä riippuen.
                if (compact) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions()
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        actions()
                        // **Viiva lisien ja ottelukortin väliin** (Tommin valinta 20.9.2026
                        // kankaalta, `docs/UI.md` › Sivupaneelin napit): sovelluksen omat
                        // lisät ja sivun teot erotetaan rakenteella eikä tyhjällä tilalla.
                        // Kortin reunuksen väri, koska viiva on rakenne eikä luettava asia.
                        // Vain kun lisiä on, muuten viiva olisi yksin kortin yllä.
                        if (actionsPresent) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp),
                                thickness = DgBoard.OUTLINE,
                                color = LocalPanelLook.current.outline.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            },
            stack = {
                // Sivun teot olivat paneelin pohjalla lähes nimessä kiinni 2.9.–14.9.2026
                // (Tommin tarkennus silloin), nyt ottelukortin ja oman kortin välin
                // keskellä ([CenteredOverStack]). Muistutukset ja `Skip Game` ovat
                // ottelukortin yläpuolella, joten `Skip Game` ei ole `Roll Dicen` kyljessä
                // (C3, `docs/AUDITOINTI-FOORUMI.md`).
                // Väli nimikorttiin oli vahvistusruudun mitta 2.9.–14.9.2026; keskitys
                // omaan väliinsä tekee saman ja enemmän, joten vakioväli poistui.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    formActions()
                }
            },
        ) { tight ->
            MatchCard(board, roundLabel, compact, onOpenPage, tight = tight)
        }

        // **Toiminnot asuivat tässä paneelin yläpuolella 10.8.-14.8.2026, eivät enää.**
        // Ne siirtyivät `MiddleStrip`iin, koska napit ovat *hänen* tekojaan ja hänen
        // vuoronsa on nyt merkitty suoraan sillä puolella keskikaistaa. Sijainti kantaa
        // yhä saman merkityksen kuin ennen (sama puoli kuin hänen nappulansa), vain
        // koti vaihtui.
        board.players.getOrNull(1)?.let {
            PlayerPanelView(
                it,
                awayOf(it),
                roles.selfPaint(),
                other = board.players.getOrNull(0),
                otherAway = board.players.getOrNull(0)?.let { p -> awayOf(p) },
                matchLength = board.matchLength,
                onOpen = it.player.profilePath?.let { path -> { onOpenPage(path) } },
                cube = panelCube?.takeIf { c -> c.position == CubePosition.BOTTOM },
                onDouble = onCube,
                cubeAttention = cubeReminder,
            )
        }

        // Kehote on sivun omaa tekstiä eikä tulkittua, joten sitä ei sanota toisin. Se on
        // alimpana kirjautuneen pelaajan paneelin kanssa, koska se koskee sitä mitä *hän*
        // voi tehdä.
        //
        // **`situation` eikä `prompt` 24.8.2026 alkaen**, eli joka sivulla sama vakiorivi
        // jätetään piirtämättä. Rajaus on `BoardState.situation`issa eikä tässä, ks. sen
        // perustelu. Sama muoto kuin peruutusilmoituksella yllä.
        //
        // **Peruutus on rivi eikä kortti 3.9.2026 alkaen** (Tommi: *"kyllä se häiritsee,
        // hiljaisempi ratkaisu"*). Se tuli illan sessiossa seitsemällä laudalla 44:stä, eli
        // se ei ole harvinainen tapahtuma vaan sivuston tavallinen tila, ja kortti vaati
        // kuittauksen joka kerta. Rivi on tilannetekstin yläpuolella samassa asussa, koska
        // se on samaa lajia: sivun oma sana siitä missä mennään.
        if (showNotes && board.rolledBack) {
            Text(
                text = stringResource(R.string.board_rolled_back),
                style = MaterialTheme.typography.labelSmall,
                color = LocalPanelLook.current.faint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(LocalPanelLook.current.card)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (showNotes) board.situation?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = LocalPanelLook.current.faint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(LocalPanelLook.current.card)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        // Pip-vahti oli 10.8.2026 asti keskitilassa muurin kohdalla. Se siirtyi tänne kun
        // nopat tulivat lähemmäs keskustaa: vahti on jäsennyksen tarkistus eikä laudan
        // tieto, joten sen paikka on siellä missä muutkin ruudun tiedot ovat.
        if (showNotes) {
            PipCheckLine(pips)
            CheckerCheckLine(checkers)
        }
    }
}

/**
 * Sivupaneelin keskilohko: [content] koko lohkon pystykeskellä, [above] kiinni sen
 * yläreunassa ja [stack] pohjalla.
 *
 * Kaksi joustoväliä (`Spacer(weight)`) keskittivät sisällön siihen tilaan joka pinon
 * jälkeen jäi, joten keskikohta liikkui pinon korkeuden mukana. Tämä mittaa lohkot
 * erikseen ja laskee sisällön paikan lohkon koko korkeudesta: pino ei vaikuta siihen,
 * eikä yläpuolinen lohko. Poikkeus on ahdas lohko: keskitetty sisältö väistää ylös jos
 * se osuisi pinoon, ja alas jos yläpuolinen lohko ei muuten mahtuisi, ja pinon väistö
 * voittaa. Sijoitus on [DgBoard.centeredTop]issa, jotta sen voi testata ilman Composea.
 */
@Composable
private fun CenteredOverStack(
    modifier: Modifier = Modifier,
    above: @Composable () -> Unit,
    stack: @Composable () -> Unit,
    /** `tight` on tosi kun lohko ei muuten mahdu, ks. [MatchCard]in tiivis muoto. */
    content: @Composable (tight: Boolean) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minHeight = 0)
        val abovePlaceables = subcompose("above", above).map { it.measure(loose) }
        val stackPlaceables = subcompose("stack", stack).map { it.measure(loose) }
        val aboveHeight = abovePlaceables.sumOf { it.height }
        val stackHeight = stackPlaceables.sumOf { it.height }
        // **Ahdas lohko tiivistää sisällön ja viimeisenä jättää sen pois** (testaajapalaute
        // 9.10.2026, Chromebookin puhelinikkuna). Lisät sijoitettiin ennen negatiiviseen
        // y:hyn, eli vastustajan kortin päälle, kun kolme lohkoa ei mahtunut. Ottelukortti on
        // lohkoista ainoa joka ei ole teko, joten se antaa tilaa ensin.
        fun fits(contentHeight: Int) = !constraints.hasBoundedHeight ||
            aboveHeight + contentHeight + stackHeight <= constraints.maxHeight
        var contentPlaceables = subcompose("content") { content(false) }.map { it.measure(loose) }
        if (!fits(contentPlaceables.sumOf { it.height })) {
            contentPlaceables = subcompose("tight") { content(true) }.map { it.measure(loose) }
            if (!fits(contentPlaceables.sumOf { it.height })) contentPlaceables = emptyList()
        }
        val contentHeight = contentPlaceables.sumOf { it.height }
        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            aboveHeight + contentHeight + stackHeight
        }
        val width = constraints.maxWidth
        val contentTop = DgBoard.centeredTop(height, contentHeight, stackHeight, aboveHeight)
        val contentBottom = contentTop + contentHeight
        layout(width, height) {
            // **Lisät ja pino omien väliensä keskelle** (Tommin tarkennus 14.9.2026 illalla:
            // *"osiot nimikorttien ja ottelukortin välissä voisi korkeus-keskittää"*).
            // Pino oli pohjalla nimikortin tuntumassa 2.9.2026 alkaen; nyt se on
            // ottelukortin ja oman kortin välin keskellä, ja lisät vastustajan kortin ja
            // ottelukortin välin keskellä. Ahtaassa lohkossa välit ovat nollaa ja
            // keskitys palautuu kiinni korttiin, ks. [DgBoard.centeredTop].
            // Lisät eivät nouse lohkon yläreunan yli (9.10.2026), vaikka lohko olisi niille
            // ja pinolle yhdessäkin liian matala.
            var aboveY = ((contentTop - aboveHeight) / 2).coerceAtLeast(0)
            abovePlaceables.forEach { placeable ->
                placeable.placeRelative((width - placeable.width) / 2, aboveY)
                aboveY += placeable.height
            }
            var y = contentTop
            contentPlaceables.forEach { placeable ->
                placeable.placeRelative((width - placeable.width) / 2, y)
                y += placeable.height
            }
            var stackY = contentBottom + (height - contentBottom - stackHeight) / 2
            stackPlaceables.forEach { placeable ->
                placeable.placeRelative((width - placeable.width) / 2, stackY)
                stackY += placeable.height
            }
        }
    }
}

/**
 * Pelaajan pistekentän teksti, eli away-notaatio silloin kun se on johdettavissa.
 *
 * **Näytetty suure vaihtui saaduista pisteistä puuttuviin 21.8.2026** (Tommin valinta, peruste
 * `SUBSTANSSI.md` kohdat 82, 90 ja 92): päätöksen kannalta merkitsevä on se paljonko voittoon
 * puuttuu, ja away on analyysin omaa kieltä joka ei tarvitse selitystä. Pari luetaan
 * kahdesta paneelista, vastustaja ensin ja kirjautunut toisena, eli samassa järjestyksessä kuin
 * kaikki muukin ruudulla.
 *
 * **Sana kirjoitetaan auki 24.8.2026 alkaen** (`4-away`, ei `4a`), Tommin pyyntö: tilaa on
 * koko sanalle, ja lyhenne säästi tilaa jota ei tarvinnut säästää.
 *
 * **Crawford-tähti seuraa mukana** eikä katoa notaation vaihtuessa. Se on sivuston oma merkintä
 * ja ruudun ainoa Crawford-vihje, joten sen pudottaminen olisi ollut tiedon poisto eikä
 * esitystavan vaihto. Tähti tulee eri kentästä ([PlayerPanel.scoreLabel]) kuin luku, joten se
 * liitetään tässä eikä muotoiluresurssissa.
 *
 * **Vanha muoto on yhä varareitti**, eikä se ole jäänne: rahapelissä ottelulla ei ole pituutta,
 * jolloin away ei ole olemassa ja sivun oma pistekenttä on ainoa mitä voi näyttää.
 *
 * **Sama varareitti on 27.8.2026 alkaen myös valinta**: `score_style`-kytkimen SITE-arvo
 * antaa `away = null` kaikille paneeleille (ks. `LoadedBoard.awayOf`), jolloin tämä näyttää
 * sivun kentän. Esitys ei siis haaraudu tässä vaan syötteessä.
 */
@Composable
private fun scoreText(panel: PlayerPanel, away: Int?): String? {
    val star = if (panel.scoreLabel?.contains('*') == true) "*" else ""
    if (away != null) return stringResource(R.string.board_score_away, away) + star
    val raw = panel.scoreLabel ?: panel.score?.toString() ?: return null
    return stringResource(R.string.board_score, raw)
}

/**
 * Pelaajakortin koko ottelutilanne kortin omistajan näkökulmasta, oma luku ensin.
 *
 * **Kortti kertoo parin eikä vain oman lukunsa** (Tommin ehdotus ja valinta 4.10.2026:
 * *"ottelutilanteen lukeminen vaatii pelaajakorttien tietojen yhdistelyä"*). Away-asetuksella
 * pari on `5-away 3-away`. Sivun pisteillä se on `2-4/7`, koska silloin pituutta ei voi lukea
 * luvuista (*"pituus on tärkeä tieto, jos ei ole away-luvut käytössä"*). Tähti seuraa kunkin
 * pelaajan omaa lukua kuten [scoreText]issä.
 *
 * Kun pari ei ole koottavissa (rahapeli, toinen paneeli puuttuu), näytetään oma luku kuten
 * ennen. Lokerosarake käyttää yhä [scoreText]iä, koska kapeaan sarakkeeseen pari ei mahdu.
 */
@Composable
private fun cardScoreText(panel: PlayerPanel, away: Int?, other: PlayerPanel?, otherAway: Int?, matchLength: Int?): String? {
    fun star(p: PlayerPanel) = if (p.scoreLabel?.contains('*') == true) "*" else ""
    if (other != null && away != null && otherAway != null) {
        return stringResource(R.string.board_score_away_pair, away, star(panel), otherAway, star(other))
    }
    val own = panel.scoreLabel ?: panel.score?.toString()
    val theirs = other?.let { it.scoreLabel ?: it.score?.toString() }
    if (away == null && own != null && theirs != null && matchLength != null) {
        return stringResource(R.string.board_score_of, own, theirs, matchLength)
    }
    return scoreText(panel, away)
}

/**
 * Pelaajakortin nimen väri: harvinaisuusporras ratingin mukaan, kun rating on nähty
 * profiililla tai pelaajalistassa, ja muuten kortin tavallinen tekstiväri (Tommin tilaus
 * 24.9.2026, rajat 25.9.2026, ks. [ratingTier]). Lokero on aina tumma, joten sävy on tumman
 * pinnan versio; paneelin kortti ([onPanel]) on vaaleassa tilassa vaalea (27.9.2026).
 */
@Composable
private fun ratedNameColor(panel: PlayerPanel, onPanel: Boolean = false): Color {
    val rating = panel.player.userId?.let(LocalPlayerRating.current)
    val look = LocalPanelLook.current
    val dark = !(onPanel && look.light)
    return rating?.let { Rarity.color(ratingTier(it), dark = dark) }
        ?: if (onPanel) look.text else Palette.TextPrimary
}

/**
 * Pelaajapaneeli.
 *
 * `backgroundColor` on tarkoituksella lukematta: sivusto korostaa toisen pelaajan, mutta
 * korostuksen merkitys on kanonissa merkitty hypoteesiksi. Ruudulle maalattu hypoteesi
 * muuttuu väitteeksi, eikä tämä sovellus väitä enempää kuin sivu sanoo.
 */
/**
 * Sivupaneelin kortti: sama kehys pelaajille ja ottelutiedoille (8.9.2026). Yksi paikka
 * mitoille, jotta kortit eivät voi ajautua erilleen toisistaan.
 */
private fun Modifier.panelCard(
    outline: Color,
    outlineWidth: Dp = DgBoard.OUTLINE,
    fill: Color = Palette.Felt,
): Modifier = this
    .fillMaxWidth()
    .clip(RoundedCornerShape(6.dp))
    .background(fill)
    .border(outlineWidth, outline, RoundedCornerShape(6.dp))
    .padding(horizontal = 12.dp, vertical = 10.dp)

@Composable
private fun PlayerPanelView(
    panel: PlayerPanel,
    away: Int?,
    paint: CheckerPaint,
    /** Profiilin avaus, tai null kun sivu ei antanut pelaajalle linkkiä. */
    onOpen: (() -> Unit)?,
    /**
     * Tämän pelaajan omistama kuutio nimen rivin oikeassa reunassa, tai null.
     *
     * **Kortti on omistajuutta** (Tommin valinta 14.9.2026 illalla kolmesta kuvasta, B):
     * omistettu kuutio kuuluu sen pelaajan korttiin jolla se on, samaan tapaan kuin
     * pipit ja away. Muurin pää ([BarSegment]) jää puhelimelle, jossa korttia ei ole.
     * Napautus on sama teko kuin muualla: oma kuutio tuplaa kun sivu tarjoaa sitä.
     */
    cube: Cube? = null,
    onDouble: (() -> Unit)? = null,
    cubeAttention: Boolean = false,
    /** Toinen pelaaja, hänen away-lukunsa ja ottelun pituus kortin pariin, ks. [cardScoreText]. */
    other: PlayerPanel? = null,
    otherAway: Int? = null,
    matchLength: Int? = null,
    modifier: Modifier = Modifier,
) {
    // **Nimen perässä vain oma luku** (`ScoreStyle.NAME`, testaajan toive 9.10.2026:
    // *Unknown DailyGammoner (0)*). Kortti on silloin yhden rivin korkuinen, ja se on valinnan syy.
    val besideName = LocalScoreBesideName.current
    val ownPoints = (panel.scoreLabel ?: panel.score?.toString()).takeIf { besideName }
    val score = if (besideName) null else cardScoreText(panel, away, other, otherAway, matchLength)
    // **Kortti on linkki pelaajanäkymään** (Tommin toive 14.9.2026 illalla). Napautus
    // avaa sivun oman profiilipolun lukunäkymään; ilman polkua (nimet linkittöminä
    // sivuston asetuksella) kortti on pelkkä kortti eikä näytä napautettavalta.
    // **Kuution omistajan kortilla on oma kehys** (Tommin tarkennus 14.9.2026 illalla:
    // *"kuution omistusta korostaisi omanlaisensa kehys"*). Kehys on kuution väri, jolloin
    // kortti ja kuutio lukevat yhdeksi asiaksi; muut kortit pitävät paneelin kehyksen.
    // **Muistutus korostaa koko kortin** (Tommin tilaus 14.9.2026 illalla: *"kun omistaa
    // kuution ja kuutiomuistutus, niin siihen pitää vielä keksiä korostus pelaajakorttiin"*).
    // Kehys vaihtuu kuution väristä laserin väriin ja paksunee kahteen viivaan, eli sama
    // väri ja sama keino kuin kuution omalla pysyvällä korostuksella pulssien jälkeen.
    // Staattinen, koska lepotilan kuuluu piirtää nolla ruutua (`docs/TESTAUS.md`).
    val reminded = cube != null && cubeAttention
    val panelLook = LocalPanelLook.current
    Column(
        modifier = modifier
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .panelCard(
                outline = when {
                    reminded -> Palette.CubeLaser
                    cube != null -> panelLook.cubeOutline
                    else -> panelLook.outline
                },
                outlineWidth = if (reminded) DgBoard.OUTLINE * 2 else DgBoard.OUTLINE,
                fill = panelLook.card,
            ),
    ) {
        // **Kuutio saa kaksi riviä** (Tommin tarkennus 14.9.2026 illalla: *"henkilökortin
        // tuplauskuutiolle anna toinen rivi tilaa"*): nimi ja away vasemmalla omassa
        // sarakkeessaan, kuutio oikealla niiden yhteisellä korkeudella. Pipit jäävät
        // koko leveydelle kuution alle.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Nappulan väri nimen vieressä: paneelin ja laudan yhdistää muuten vain
                    // sijainti, ja se on juuri se päättely jota tämä säästää.
                    // **Pisteellä on rengas** (Tommin havainto ja valinnat 24.9.2026: musta
                    // piste oli mustalla kortilla näkymätön). Decossa rengas on laudan oma
                    // kultarengas. Muissa tyyleissä laudan rengas on lähes musta, joten piste
                    // saa kortin kehyksen värin: Monte Carlon vastustaja nousi kontrastista
                    // 1,1 arvoon 4,1 ja sivuston Classicin sininen arvosta 2,7.
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(paint.fill)
                            .checkerRim(paint, panelLook.outline, DgBoard.OUTLINE),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ownPoints?.let { stringResource(R.string.board_score_beside_name, panel.player.displayName, it) }
                            ?: panel.player.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = ratedNameColor(panel, onPanel = true),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                // **Ottelutilanne aina omalla rivillään** (Tommin päätös 5.10.2026: *"pelaajakortin
                // ottelutieto ei sovi nimen kanssa aina, joten siirrä se omalle riville"*).
                // Ahtaassa paneelissa se oli 16.9.2026 alkaen nimen rivillä, ja kauppakuvissa
                // pitkä nimi katkesi pisteisiin. Pari on [cardScoreText]. Kortti on yhä
                // kaksirivinen, koska pipit siirtyivät samalla muurille ([BarPips]).
                score?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalPanelLook.current.secondary,
                    )
                }
            }
            if (cube != null) {
                Spacer(modifier = Modifier.width(8.dp))
                // `Pick me` -lippu kuution yllä poistui 27.9.2026 (Tommin valinta): muistutus
                // näkyy kuution kasvuna, ks. [reminderAttention].
                CubeFace(cube, CARD_CUBE, onDouble, cubeAttention)
            }
        }
        // Pipit olivat kortin kolmas rivi 14.9.–5.10.2026, ja ne ovat nyt muurilla, ks. [BarPips].
    }
}

/**
 * Lauta.
 *
 * **Suunta tulee [BoardLook]ista**: oletuksena 13-24 ylhäällä vasemmalta oikealle ja 12-1
 * alhaalla, peilattuna 24-13 ja 1-12 (mitattu fixturesta `move_board_vasen.html`). Peilaus
 * on turvallinen juuri siksi että se tehdään **asetuksen perusteella eikä sivun asettelusta
 * päättelemällä**: numerot tulevat mallista, jäsennin on normalisoinut `Home boards on left
 * side`:n pois, ja piirto saa suuntansa säilötystä asetuksesta. Aiempi kielto ("peilaus
 * olisi sen uudelleen päättelemistä minkä jäsennin hävittää") koski laudalta päättelyä eikä
 * tätä, ks. `docs/ASETUKSET.md` luku 4.
 *
 * Piste haetaan **numerolla eikä indeksillä**: lista ei ole taatusti 24 pitkä eikä tiheä,
 * koska numeroimaton piste jätetään jäsennyksessä pois.
 *
 * **Muuri on pystysuora tili keskellä**, pisteiden 18/19 ja 7/6 välissä, ei vaakarivi
 * puoliskojen välissä. Siksi jokainen puolisko on `Row` jossa on kolme jäsentä (kuusikko,
 * muurin oma segmentti, kuusikko): sama pystylinja jatkuu ylä- ja alarivin läpi, koska
 * segmentit ovat samalla kohdalla molemmissa riveissä.
 *
 * **Numerot ovat harmaan pelialueen ulkopuolella**, omina riveinään laatikon ylä- ja
 * alapuolella. Pelialue (kiilat, muuri, keskitila) on oma laatikkonsa jolla on tausta;
 * numerorivi ei ole sen sisällä, koska numerointi on laudan reunamerkintä eikä osa
 * itse pelialueen väripintaa.
 *
 * **Mitat johdetaan mitatusta tilasta, ei kirjoiteta vakioksi.** Kaksi `BoxWithConstraints`ia
 * mittaavat eri asian ja kummallekin on oma syynsä. Ulompi antaa leveyden, josta sarake on
 * murto-osa: kiinteä 35 dp sarake ylittäisi kapean ruudun hiljaa. Sisempi antaa korkeuden
 * vasta sen jälkeen kun numerorivit ovat vieneet omansa, joten yläpalkin, tilapalkin ja
 * tekstirivien korkeutta ei tarvitse arvata oikein kertaakaan. Aiempi kiinteä `WEDGE_HEIGHT`
 * jätti 8.8.2026 Pixel 8a:lla laudan alapuoliskon piirtymättä ilman virheilmoitusta, koska
 * `Column` ei kutista kiinteäkorkuisia lapsia; vähennettäväksi vakioksi kirjoitettu
 * korkeusbudjetti toistaisi saman vian aina kun arvaus on liian pieni.
 *
 * `roles` kantaa pelaajajaon: muuri, keskitila ja lokerot jakavat nappulat/nopat pelaajan
 * (ei vain värin) mukaan, ja se johdetaan pip-vahdista [selfCheckerColor]issa.
 */
@Composable
private fun Board(
    board: BoardState,
    roles: BoardRoles,
    pips: PipCheck,
    /** Pisteiden esitys lokerosarakkeen pelaajariveille, ks. [SidePanel]. */
    awayOf: (PlayerPanel) -> Int?,
    touch: BoardTouch,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    /** Kuution napautus. Eri reitti kuin [onPress], koska kuutio kysyy aina ([cubeActionFor]). */
    onCubePress: (String, Boolean) -> Unit = onPress,
    /**
     * Omien noppien painallus, tai null kun painallus ei tee mitään. Valmis teko eikä ehto:
     * sääntö on kutsujalla ([diceTapFor]), jotta lauta ei tunne kokoamisen tiloja.
     */
    onDiceTap: (() -> Unit)? = null,
    /** Vastustajan noppien painallus, ks. [diceRollTapFor]. Null kun painallus ei tee mitään. */
    onOpponentDiceTap: (() -> Unit)? = null,
    /** Kootun siirron nuolet, tyhjä kun niitä ei piirretä. Ks. [MoveArrowLayer]. */
    arrows: List<MoveArrow> = emptyList(),
    /** Vastustajan edellisen siirron nuolet vastustajan värillä, tyhjä kun niitä ei piirretä. */
    opponentArrows: List<MoveArrow> = emptyList(),
    /** Katkotilan `Refresh` kaistan keskelle, päällimmäisenä. Ks. [UnconfirmedRefresh]. */
    unconfirmedRefresh: UnconfirmedRefreshSpec? = null,
    /** Sivun tilarivit vapaalla puoliskolla, ks. [stripNotes] ja [situationSlot]. Tyhjä kun ne ovat paneelissa. */
    stripNotes: List<StripNoteSpec> = emptyList(),
    stripNotesSlot: StripSlot? = null,
    /** Rastihuomautus keskikaistalla, ks. [verifyHintSlot]. Null kun se on laudan yläreunassa. */
    verifyHint: String? = null,
    verifyHintSlot: StripSlot? = null,
    onDismissVerifyHint: () -> Unit = {},
    modifier: Modifier = Modifier,
    /**
     * Vahvistusruudun tila. Kutsujalla eikä laudassa, koska kuution klikkaus lähettää
     * ruudun tilan siinä missä nappirivikin (Tommin päätös 3.9.2026, ks. [cubeActionFor]).
     */
    verify: VerifyBox = VerifyBox.NONE,
    /**
     * Piirretäänkö pelaajan nimi, pisteet ja pipit lokerosarakkeen päihin.
     *
     * Sisältö on lautatilassa jo valmiina (`board.players`), joten kutsujalla ei ole
     * mitään annettavaa. Se päättää vain mahtuuko se, ja se on leveyskysymys jota lauta
     * ei näe.
     */
    playerFactsInTray: Boolean = false,
    /**
     * Tosi kun omistettu kuutio on sivupaneelin pelaajakortissa ([PlayerPanelView]), jolloin
     * muurin pää ([BarSegment]) jää tyhjäksi. Sama kuutio on tasan yhdessä paikassa
     * (3.9.2026), ja paikan valitsee kutsuja koska lauta ei tiedä onko paneelia.
     */
    ownedCubeInPanel: Boolean = false,
    /**
     * Piirretäänkö pelaajien pipit muurin päihin ([BarPips]). Tosi kun sivupaneeli on, eli
     * pelaajakortit ovat ruudulla; ilman paneelia pipit ovat lokerosarakkeessa
     * ([TrayPlayerFacts]) ja muurin pää kuuluu omistetulle kuutiolle ([BarSegment]).
     */
    pipsOnBar: Boolean = false,
    /**
     * Onko tälle pelille voimassa kuutiomuistutus. Lauta korostaa silloin kuutiota, ks.
     * [reminderAttention].
     *
     * **Ehto tulee kutsujalta eikä laudalta**, samasta syystä kuin [middleTrailing]: lauta
     * ei tunne muistutuksia eikä niiden säilytystä, ja se on tarkoitus.
     */
    cubeReminder: Boolean = false,
    /**
     * Sovelluksen omat napit sivun omien perään toimintorivillä, tai tyhjä kun niitä ei ole.
     *
     * Paikka eikä sisältö: lauta ei tiedä mitä napit tekevät, ja se on tarkoitus. Ilman
     * tätä muistutusnapit olisi pitänyt kuljettaa lautaan asti käsitteinä, ja lauta olisi
     * oppinut sanan jota sivustolla ei ole.
     */
    middleTrailing: @Composable () -> Unit = {},
    /**
     * Piirretäänkö `Skip Game` keskikaistan riville. Epätosi kun sivupaneeli on näkyvissä,
     * ks. [MiddleActionsRow].
     */
    middleShowSkip: Boolean = true,
    /**
     * Piirtääkö toimintorivi sivun lomakkeen napit (Tommin päätös 27.8.2026).
     *
     * Epätosi kun chat-kortti on ruudulla: kortti omistaa silloin saman nappiparin.
     * Ks. [MiddleActionsRow].
     */
    middleShowSubmits: Boolean = true,
    /** Pyyntö on kesken: toimintorivillä pelkkä kaari, ks. [actionItems]. */
    busy: Boolean = false,
    /**
     * Piirtääkö keskikaista toimintorivin lainkaan. Epätosi kun napit ovat sivupaneelissa
     * (Tommin päätös 2.9.2026, [PanelActionStack]); kaista on silloin pelkät nopat.
     */
    middleShowActions: Boolean = true,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(roles.look.frame)
            .woodGrain(roles.look.frameGrain, seed = 1)
            .padding(horizontal = DgBoard.FRAME_PAD_H, vertical = DgBoard.FRAME_PAD_V),
    ) {
        // **Leveys johdetaan korkeudesta, ei päinvastoin** (mitattu 8.8.2026).
        //
        // Kun sarake sai koko leveydestä murto-osansa, kiilasta tuli tällä laitteella
        // 47 x 134 eli 1:2,85, ja lauta näytti matalalta. Vika ei ollut korkeudessa vaan
        // siinä että ylimääräinen leveys jaettiin sarakkeille joilla ei ollut sille käyttöä:
        // nappula oli jo korkeudesta rajattu, joten leveämpi sarake tuotti vain väljyyttä
        // nappuloiden ympärille. Vertailukohta on mitattu: DG Mobilessa kiila on 1:3,66.
        //
        // Sarake on siksi nappulan levyinen ja lauta kapenee, sen sijaan että se venyisi.
        // Nappula ei pienene tässä pikseliäkään, koska sen koko tulee samasta kiilan
        // korkeudesta kuin ennenkin.
        // Sama johtaminen kuin kutsujalla, ja **tarkoituksella sama funktio**: kutsuja laski
        // tämän kehyksen leveyden ([DgBoard.frameWidth]) ennen riviä, ja jos luku johdettaisiin
        // täällä toisin, kehys olisi eri levyinen kuin se lauta jonka se piirtää. Kutsujan
        // antama leveys on tämän tuloksen mittainen, joten mikään ei muutu kahdesti.
        //
        // **Absoluuttista nappulakattoa ei ole**, ja se on päätös eikä unohdus (Tommi
        // 8.8.2026). Tässä oli `min(25.dp, ...)`, joka on Designin nappulakoko sen
        // viiteikkunassa. Kun järjestelmäpalkit piilotettiin samana iltana, kiilaan olisi
        // mahtunut 26,8 dp:n nappula, ja vakio söi erotuksen: jokainen muualta säästetty
        // pystypikseli olisi mennyt hukkaan, koska nappula ei olisi saanut kasvaa. Katto on
        // nyt pelkkä suhde eli sarakkeen leveys, ja se skaalautuu myös tabletille johon
        // vakio oli sidottu.
        val boardWidth = DgBoard.contentWidth(
            width = maxWidth,
            height = maxHeight,
            numberRowHeight = numberRowHeight(),
        )
        val column = boardWidth / (DgBoard.PLAY_COLUMNS + DgBoard.TRAY_PER_COLUMN)
        val trayWidth = column * DgBoard.TRAY_PER_COLUMN

        Column(
            modifier = Modifier.fillMaxHeight().width(boardWidth).align(Alignment.Center),
        ) {
            NumberRow(
                topNumbers(roles.look.mirrored, left = true),
                topNumbers(roles.look.mirrored, left = false),
                board.points,
                touch,
                trayWidth,
                trayOnLeft = roles.look.mirrored,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(roles.look.felt)
                    .woodGrain(roles.look.feltGrain, seed = 2),
            ) {
                // **Nappula mitoitetaan myös korkeudesta**, jotta viisi mahtuu kiilaan
                // limittymättä. Pelkkä leveyskatto tuotti puhelimella 25 dp nappulan 84 dp
                // kiilaan, jolloin limitys alkoi jo neljästä.
                //
                // Sarakkeen leveys on yllä johdettu arviosta ja tämä korkeus on mitattu, joten
                // tässä otetaan pienempi. Arvion virhe kutistaa siis nappulaa muttei koskaan
                // ylitä laudan omaa leveyttä, eikä leikkaudu mistään hiljaa.
                //
                // **Keskitila on mukana laskussa 10.8.2026 alkaen** eikä vähennettynä ennen
                // sitä: se on nappulan mittainen, joten se ei ole tiedossa ennen nappulaa.
                // Ks. [DgBoard.checkerFromHeight], jossa kehä ratkeaa kerralla.
                val checkerSize = minOf(
                    column * DgBoard.CHECKER_PER_COLUMN,
                    DgBoard.checkerFromHeight(maxHeight),
                    // Yläraja on jo mukana `column`issa, koska se tulee samasta laskusta.
                    // Se toistetaan tässä siksi, että tämä `minOf` on se joka piirtää: jos
                    // leveyden lähde joskus muuttuu, katon puuttuminen näkyisi vasta
                    // ruudulla eikä käännöksessä.
                    DgBoard.CHECKER_MAX,
                )
                val bandHeight = DgBoard.bandHeight(checkerSize)
                // Kiila saa sen mikä keskitilalta jää. Korkeusrajatussa tapauksessa se on
                // tasan viisi nappulaa; leveysrajatussa se on enemmän, ja ylimääräinen jää
                // kiilan pohjalle väljyydeksi kuten ennenkin.
                val wedgeHeight = ((maxHeight - bandHeight) / 2)
                    .coerceAtLeast(column * 2)
                val metrics = BoardMetrics(
                    trayWidth = trayWidth,
                    checkerSize = checkerSize,
                    diceSize = checkerSize * DgBoard.DICE_PER_CHECKER,
                    cubeSize = checkerSize * DgBoard.CUBE_PER_CHECKER,
                    bandHeight = bandHeight,
                    wedgeHeight = wedgeHeight,
                )
                // Kuutio on tuplausnappi vain kun sivu tarjoaa tuplausta, ja ehto luetaan
                // lomakkeesta eika paatella vuorosta tai nopista. Sama lahde kuin
                // nappirivilla: `Double` on submitsissa tasan silloin kun teko on mahdollinen.
                val canDouble = board.form?.submits?.contains(DOUBLE_SUBMIT) == true
                // Tarjottu kuutio samalla säännöllä (Tommin päätös 3.9.2026): kun sivu
                // tarjoaa `Accept`ia, kuution klikkaus on Accept, ja se kulkee saman
                // rastin ja dialogin kautta kuin nappi. Sivu ei merkitse tarjousta
                // kuutioon (kuva on yhä `cube1` keskellä, mitattu 17 sivusta), joten
                // nappi on tarjouksen ainoa tuntomerkki.
                val canAccept = board.form?.submits?.contains(ACCEPT_SUBMIT) == true
                // Tuplauksen vahvistus (dialogi) asuu kutsujalla 2.9.2026 alkaen, koska sama
                // painallus voi tulla paneelipinosta joka on laudan ulkopuolella; [onPress]
                // on jo sen lapi kulkenut. Ks. `pressChecked` kutsujalla.
                val pressChecked = onPress
                // Kuutio lukee saman rastin kuin nappirivi: ilman rastia klikkaus ei lähde
                // eikä avaa dialogia (3.9.2026). Ilman sivun ruutua se kysyy silti, joten
                // se kulkee omaa reittiään (26.9.2026, [cubeActionFor]).
                val onCube: (() -> Unit)? = when {
                    canDouble -> ({ onCubePress(DOUBLE_SUBMIT, verify.isChecked(DOUBLE_SUBMIT)) })
                    canAccept -> ({ onCubePress(ACCEPT_SUBMIT, verify.isChecked(ACCEPT_SUBMIT)) })
                    else -> null
                }
                // **Sama kuutio on tasan yhdessä paikassa** (Tommin päätös 3.9.2026, toinen
                // muoto): omistettu kuutio muurin päässä omistajan puoliskolla (14.9.2026
                // alkaen, sitä ennen lokerosarakkeen päässä 9.8.2026 alkaen), omistamaton
                // muurilla keskikaistalla ja tarjottu oman puoliskon keskellä keskikaistalla.
                // Kaista saa kuution vain kun se on vapaa napeista (paneeli on); muuten
                // omistamaton ja tarjottu ovat sarakkeessa kuten ennen. Omistettu on aina
                // muurilla, koska muuri on molemmissa asetteluissa. Tarjous voittaa
                // omistuksen (14.9.2026), ks. [cubeOnStrip].
                val cubeOwned = board.cube?.position == CubePosition.TOP ||
                    board.cube?.position == CubePosition.BOTTOM
                val cubeOnStrip = cubeOnStrip(
                    stripHasActions = middleShowActions,
                    cubeOwned = cubeOwned,
                    cubeOffered = canAccept,
                )
                // Tarjottu kuutio näyttää tarjotun arvon, ei sivun kuvaa (8.9.2026),
                // ks. [shownCube].
                val shownCube = shownCube(board)
                // **Omistamaton kuutio on aina lokerosarakkeen keskellä** (Tommin tilaus
                // 29.9.2026 illalla: *"pysyvä paikka keskikaistan nappulalokerossa"*, vain
                // omistamaton). Se makasi siihen asti kaistan muurilla. Muurin pino väisti
                // sitä (23.9.2026). Tarjottu ja omistettu noudattavat yhä [cubeOnStrip]iä.
                val cubeUnowned = !cubeOwned && !canAccept

                // **Lokerosarake on kotikentän puolella** (Tommin tilaus 9.9.2026:
                // *"kun sivupaneeli oikealla niin off-rack vasemalla, koska siellä
                // pienimmät luvut"*). Sarake on se paikkaan johon nappulat kannetaan
                // ulos, ja ulos kannetaan kotikentästä, joten se kuuluu pisteiden 1–6
                // viereen eikä kiinteään laitaan. Peilaamattomana koti on oikealla ja
                // sarake oikealla kuten ennen; peilattuna molemmat vaihtavat puolta.
                val playArea: @Composable RowScope.() -> Unit = {
                    Column(modifier = Modifier.weight(1f)) {
                        HalfBoard(
                            board, roles, metrics, touch, top = true,
                            cube = shownCube.takeIf { !cubeOnStrip && !ownedCubeInPanel && it?.position == CubePosition.TOP },
                            onDouble = onCube,
                            cubeAttention = cubeReminder,
                            pipsOnBar = pipsOnBar,
                        )
                        MiddleStrip(
                            board,
                            roles,
                            metrics,
                            onFollow,
                            pressChecked,
                            middleTrailing,
                            showSkip = middleShowSkip,
                            showSubmits = middleShowSubmits,
                            busy = busy,
                            showActions = middleShowActions,
                            verify = verify,
                            onDiceTap = onDiceTap,
                            onOpponentDiceTap = onOpponentDiceTap,
                            cube = if (cubeOnStrip && !cubeUnowned) shownCube else null,
                            cubeOffered = canAccept,
                            notes = stripNotes,
                            notesSlot = stripNotesSlot,
                            verifyHint = verifyHint,
                            verifyHintSlot = verifyHintSlot,
                            onDismissVerifyHint = onDismissVerifyHint,
                            onDouble = onCube,
                            cubeReminder = cubeReminder,
                            unconfirmedRefresh = unconfirmedRefresh,
                        )
                        HalfBoard(
                            board, roles, metrics, touch, top = false,
                            cube = shownCube.takeIf { !cubeOnStrip && !ownedCubeInPanel && it?.position == CubePosition.BOTTOM },
                            onDouble = onCube,
                            cubeAttention = cubeReminder,
                            pipsOnBar = pipsOnBar,
                        )
                    }
                }
                    // Kehyksen värinen kaista erottaa pelialueen lokerosarakkeesta
                    // (Tommi 25.8.2026). Ne törmäsivät siihen asti toisiinsa ilman rajaa,
                    // eli oikea laita oli ainoa suunta jossa lauta ei pääty mihinkään:
                    // ylhäällä ja alhaalla on kehyksen kaista ja keskellä muuri.
                    //
                    // **Leveys on [DgBoard.FRAME_PAD_H] eikä oma vakio**, koska sama mitta
                    // erottaa pelialueen sivupaneelista laudan vasemmassa laidassa. Kaista
                    // on siis kehyksen jatke molemmissa merkityksissä, värissä ja mitassa,
                    // eikä lauta saa kahta eri levyistä reunaa.
                val frameBand: @Composable () -> Unit = {
                    Box(
                        modifier = Modifier
                            .width(DgBoard.FRAME_PAD_H)
                            .fillMaxHeight()
                            .background(roles.look.frame)
                            .woodGrain(roles.look.frameGrain, seed = 3),
                    )
                }
                val tray: @Composable () -> Unit = {
                    OffTrayColumn(
                        board,
                        roles,
                        metrics,
                        awayOf,
                        playerFactsInTray,
                        // Omistamaton aina (29.9.2026), tarjottu kun kaistalla on napit;
                        // omistettu on kortissa tai muurilla ([BarSegment]).
                        cube = when {
                            cubeUnowned -> shownCube
                            cubeOnStrip || cubeOwned -> null
                            else -> shownCube
                        },
                        onDouble = onCube,
                        cubeReminder = cubeReminder,
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    if (roles.look.mirrored) {
                        tray()
                        frameBand()
                        playArea()
                    } else {
                        playArea()
                        frameBand()
                        tray()
                    }
                }
                // Nuolet kaiken päällä, koska ne ylittävät muurin ja kaistan (29.9.2026).
                if (opponentArrows.isNotEmpty()) {
                    MoveArrowLayer(
                        arrows = opponentArrows,
                        board = board,
                        color = roles.opponentColor,
                        trayWidth = trayWidth,
                        checkerSize = checkerSize,
                        wedgeHeight = wedgeHeight,
                        bandHeight = bandHeight,
                        mirrored = roles.look.mirrored,
                        fill = roles.opponentPaint().fill,
                        halo = arrowHalo(roles.opponentPaint().fill),
                        barOnTop = false,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                if (arrows.isNotEmpty()) {
                    MoveArrowLayer(
                        arrows = arrows,
                        board = board,
                        color = roles.selfColor,
                        trayWidth = trayWidth,
                        checkerSize = checkerSize,
                        wedgeHeight = wedgeHeight,
                        bandHeight = bandHeight,
                        mirrored = roles.look.mirrored,
                        fill = roles.selfPaint().fill,
                        halo = arrowHalo(roles.selfPaint().fill),
                        modifier = Modifier.matchParentSize(),
                    )
                }

            }
            NumberRow(
                bottomNumbers(roles.look.mirrored, left = true),
                bottomNumbers(roles.look.mirrored, left = false),
                board.points,
                touch,
                trayWidth,
                trayOnLeft = roles.look.mirrored,
            )
        }
    }
}

/** Laudan mitat yhtenä arvona, jotta johdettu ja vakio eivät sekoitu kutsuketjussa. */
private data class BoardMetrics(
    val trayWidth: Dp,
    val checkerSize: Dp,
    val diceSize: Dp,
    val cubeSize: Dp,
    /** Keskitilan korkeus, kuution mittainen. Ks. [DgBoard.bandHeight]. */
    val bandHeight: Dp,
    val wedgeHeight: Dp,
)

/**
 * Pelaajajako väreiksi, piirtotavan ([BoardLook]) mukaan.
 *
 * X-22-paletissa nappulan väri on **rooli** (kerma = kirjautunut pelaaja, punainen =
 * vastustaja) eikä sivuston oma keltainen/sininen. Rooli tunnetaan vain kun pip-vahti
 * täsmää, ja [selfCheckerColor] palauttaa muuten nullin.
 *
 * **Null-tapauksessa väri annetaan sivuston värin mukaan** (keltainen saa kerman, sininen
 * punaisen) eikä sijainnin mukaan. Vaihtoehto olisi ollut piirtää molemmat neutraalilla,
 * mutta se poistaisi pelaajien erottelun kokonaan, mikä on huonompi kuin se että sovellus
 * ei tässä tilassa väitä kumpi on käyttäjä itse. Syy näkyy samalla ruudulla: pip-vahdin
 * rivi kertoo miksi tarkistus ei onnistunut.
 *
 * **Sivustouskollisessa tilassa väri on sivuston väri eikä rooli** (`docs/ASETUKSET.md`
 * luku 4): keltainen piirtyy skeeman keltaisella ja sininen sinisellä riippumatta siitä
 * kumpi on käyttäjä. Roolijako ([selfColor], [opponentColor]) säilyy silti, koska muuri,
 * keskitila ja lokerot jakavat nappulat pelaajan mukaan molemmissa tiloissa; vain maali
 * vaihtuu. Roolivärin menetys on siinä tilassa tarkoitus eikä regressio.
 */
private class BoardRoles(self: CheckerColor?, val look: BoardLook) {
    val selfColor: CheckerColor = self ?: CheckerColor.YELLOW
    val opponentColor: CheckerColor = CheckerColor.entries.first { it != selfColor }

    fun selfPaint() = paint(selfColor)
    fun opponentPaint() = paint(opponentColor)

    fun paint(color: CheckerColor?): CheckerPaint = when {
        color == null -> CheckerPaint(Palette.CheckerUnknown, Palette.TextMuted)
        else -> look.sitePaint(color) ?: rolePaint(color)
    }

    // Roolivärit tulevat tyylistä 13.9.2026 alkaen, koska Monte Carlo variant on
    // valkoinen ja musta eikä kerma ja punainen.
    private fun rolePaint(color: CheckerColor): CheckerPaint = if (color == selfColor) {
        look.roleSelf
    } else {
        look.roleOpponent
    }
}

/**
 * Laudan toinen puolisko: kuusikko, muurin oma segmentti, kuusikko. Sama pystylinja jatkuu
 * ylä- ja alarivin läpi, koska segmentit ovat samalla kohdalla molemmissa riveissä.
 */
@Composable
private fun HalfBoard(
    board: BoardState,
    roles: BoardRoles,
    metrics: BoardMetrics,
    touch: BoardTouch,
    top: Boolean,
    /** Tämän puoliskon omistama kuutio muurin päähän, tai null; ks. [BarSegment]. */
    cube: Cube? = null,
    onDouble: (() -> Unit)? = null,
    cubeAttention: Boolean = false,
    /** Ks. [Board]in `pipsOnBar`. */
    pipsOnBar: Boolean = false,
) {
    val mirrored = roles.look.mirrored
    val left = if (top) topNumbers(mirrored, left = true) else bottomNumbers(mirrored, left = true)
    val right = if (top) topNumbers(mirrored, left = false) else bottomNumbers(mirrored, left = false)
    Row(modifier = Modifier.fillMaxWidth().height(metrics.wedgeHeight)) {
        WedgeRow(board, roles, metrics, touch, left, pointsDown = top, modifier = Modifier.weight(6f))
        BarSegment(
            board, roles, metrics, touch, top = top,
            modifier = Modifier.weight(1f),
            cube = cube,
            onDouble = onDouble,
            cubeAttention = cubeAttention,
            pipsOnBar = pipsOnBar,
        )
        WedgeRow(board, roles, metrics, touch, right, pointsDown = top, modifier = Modifier.weight(6f))
    }
}

/**
 * Ylärivin pistenumerot vasemmalta oikealle, puolikkaittain.
 *
 * Yksi lähde molemmille lukijoille ([NumberRow]in kutsut ja [HalfBoard]) samasta syystä
 * kuin [DgBoard.trayWidth]illä: kaksi kopiota samasta järjestyksestä voisi ajautua
 * erilleen, ja ero näkyisi numerona joka ei osu kiilansa kohdalle.
 */
private fun topNumbers(mirrored: Boolean, left: Boolean): List<Int> = when {
    !mirrored && left -> (13..18).toList()
    !mirrored -> (19..24).toList()
    left -> (24 downTo 19).toList()
    else -> (18 downTo 13).toList()
}

/** Alarivin pistenumerot vasemmalta oikealle, puolikkaittain. Ks. [topNumbers]. */
private fun bottomNumbers(mirrored: Boolean, left: Boolean): List<Int> = when {
    !mirrored && left -> (12 downTo 7).toList()
    !mirrored -> (6 downTo 1).toList()
    left -> (1..6).toList()
    else -> (7..12).toList()
}

/**
 * Pelaajan oma väri, pääteltynä pip-vahdista eikä arvattuna.
 *
 * Kirjautunut pelaaja on paneelin viimeinen (ks. [SidePanel]in mitattu havainto), ja
 * hänen pip-lukunsa on suoraan paneelista luettavissa ([PlayerPanel.pips]). Kun pip-vahti
 * täsmää ([PipCheck.Agrees]), sen laskemat keltainen/sininen summa ovat samat luvut kuin
 * paneelissa vain eri järjestyksessä, joten se kumpi niistä täsmää pelaajan omaan
 * pip-lukuun kertoo hänen värinsä. Muissa tapauksissa (täsmäys epäonnistui, pip-luvut
 * piilotettu, paneeli vajaa) väriä ei voi tietää, joten palautetaan null sen sijaan että
 * arvattaisiin; kutsuja päättää tällöin oletusarvon.
 */
/**
 * Kumpi väri on käyttäjän, eli alemman paneelin.
 *
 * **Sivun oma merkintä ensin, pisteluvut vasta varasuuntana (31.8.2026).** Nimisolun tausta
 * kertoo värin suoraan (`PlayerPanel.checkerColor`, mitattu 132 laudasta ilman ristiriitaa),
 * ja se on vakio koko ottelun ajan. Pisteluvuista päättely jää alle siltä varalta että sivu
 * joskus jättää taustan pois, mutta se ei saa olla ensisijainen: **se kääntää roolit kesken
 * ottelun.** Syy on [reconcilePips]in dokumentoitu ominaisuus eikä bugi — symmetrinen asema
 * täsmää molemmilla suunnilla — jolloin tasapeli osuu tämän `when`in ensimmäiseen haaraan ja
 * sinisenä pelaava näkee omat nappulansa vastustajan värillä siihen asti kunnes luvut taas
 * eroavat. Tommi näki sen laiteajossa 31.8.2026, ja illan sadassa laudassa tasapelejä oli
 * seitsemän.
 */
private fun selfCheckerColor(board: BoardState, pips: PipCheck): CheckerColor? {
    val self = board.players.lastOrNull() ?: return null
    self.checkerColor?.let { return it }

    val selfPips = self.pips ?: return null
    val agrees = pips as? PipCheck.Agrees ?: return null
    return when (selfPips) {
        agrees.yellow -> CheckerColor.YELLOW
        agrees.blue -> CheckerColor.BLUE
        else -> null
    }
}

/**
 * Numerorivi, kahtena kuusikkona ja niiden välissä muurin levyinen väli, jotta numerot
 * pysyvät kohdakkain kiilojen kanssa. Lopussa lokerosarakkeen levyinen väli samasta syystä:
 * ilman sitä numerot venyisivät laudan yli.
 *
 * Numerot ovat pelialueen **ulkopuolella**, kehyksen päällä. Numerointi on laudan
 * reunamerkintä eikä osa pelialueen väripintaa.
 *
 * **Muurin väli kantoi pip-luvun 15.8.–14.9.2026** (Tommin pyyntö ja Tommin peruutus).
 * Kapea väli ei mahduttanut sanaa eikä etumatkaa tabletillakaan, joten pipit menivät
 * pelaajakorttiin ja 5.10.2026 alkaen muurille kolmelle riville ([BarPips]). Väli on tyhjä.
 */
@Composable
private fun NumberRow(
    leftNumbers: List<Int>,
    rightNumbers: List<Int>,
    points: List<Point>,
    touch: BoardTouch,
    trayWidth: Dp,
    /** Onko lokerosarake vasemmalla. Sama ehto kuin laudalla, ks. `playArea`/`tray`. */
    trayOnLeft: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = NUMBER_ROW_PADDING)) {
        // Varaus on samalla puolella kuin sarake itse, muuten pistenumerot jäisivät
        // peilattuna kiilojensa vierestä sarakkeen leveyden verran sivuun.
        if (trayOnLeft) Spacer(modifier = Modifier.width(trayWidth))
        NumberGroup(leftNumbers, points, touch, modifier = Modifier.weight(6f))
        // Muurin väli. Kantoi pip-luvun 15.8.–14.9.2026, nyt tyhjä, ks. [BarPips].
        Spacer(modifier = Modifier.weight(1f))
        NumberGroup(rightNumbers, points, touch, modifier = Modifier.weight(6f))
        if (!trayOnLeft) Spacer(modifier = Modifier.width(trayWidth))
    }
}

/**
 * Pistenumeroiden tyyli: `labelSmall` fonttikoolla 1.0 järjestelmän asetuksesta riippumatta.
 *
 * **Pistenumerot eivät seuraa järjestelmän tekstikokoa** (Tommin valinta 26.9.2026,
 * `docs/AVOIMET.md` › *Pistenumerot eivät kutistu*). Pixelillä `font_scale 2.0` rivitti
 * ryhmän, ja `13` katkesi kahdelle riville. Koko ryhmä on aina samankokoinen, joten 24.8.
 * päätöksen ehto (ei eri kokoisia numeroita samalla rivillä) pysyy voimassa.
 */
@Composable
private fun numberStyle(): TextStyle {
    val style = MaterialTheme.typography.labelSmall
    val scale = LocalDensity.current.fontScale
    fun TextUnit.unscaled() = if (isSpecified && isSp) (value / scale).sp else this
    return style.copy(fontSize = style.fontSize.unscaled(), lineHeight = style.lineHeight.unscaled())
}

/**
 * Numerorivin korkeus **ennen** kuin se on piirretty.
 *
 * Tarpeen siksi että laudan leveys johdetaan korkeudesta: jäljelle jäävä korkeus on
 * tiedettävä jo silloin kun sarakkeen leveys lasketaan, eikä sitä voi kysyä riviltä joka ei
 * ole vielä olemassa. Arvo luetaan tyyliskaalasta ja tiheydestä eikä kirjoiteta vakioksi,
 * jotta käyttäjän oma tekstikoko kasvattaa myös tätä lukua eikä leikkaa numeroa.
 *
 * **Tämä ei ole korkeusbudjetti** eikä siis toista sitä vikaa jonka kiinteä `WEDGE_HEIGHT`
 * teki: rivi piirtyy yhä omalla mitallaan ja pelialue joustaa `weight`illä, joten arvion
 * virhe muuttaa laudan leveyttä muttei voi leikata mitään pois.
 */
@Composable
private fun numberRowHeight(): Dp {
    val style = numberStyle()
    val line = when {
        style.lineHeight.isSpecified && style.lineHeight.isSp -> style.lineHeight
        style.fontSize.isSpecified && style.fontSize.isSp -> style.fontSize
        else -> null
    }
    val text = line?.let { with(LocalDensity.current) { it.toDp() } } ?: 16.dp
    return text + NUMBER_ROW_PADDING * 2
}

/**
 * Pistenumerot toisella puolella lautaa.
 *
 * **Nämä eivät kutistu, ja se on päätös eikä puute (Tommi 24.8.2026).** Mitattu samana
 * päivänä: leveys 360 dp ja Androidin `font_scale 1.8` ahtaa ylärivin numerot kiinni
 * toisiinsa. Päätös on että **iso järjestelmäfontti on tiedostettu raja eikä tuettava tila**,
 * joten `BarPips` (poistettu 14.9.2026, pipit [PlayerPanelView]issä)in kutistuslogiikkaa ei siirretä tänne. Peruste on lajiero: leikkautunut pip
 * oli väärä luku joka näytti kelvolliselta, tiivis pistenumerorivi on oikea luku jota on
 * työläs lukea, eikä jälkimmäinen valehtele kenellekään.
 *
 * Jos tätä joskus muutetaan, kutistuksen on koskettava **koko ryhmää kerralla** ja mittauksen
 * on tehtävä levein numero (`24`) huomioiden. Eri kokoiset pistenumerot samalla rivillä olisi
 * pahempi jälki kuin tiivis rivi. Koko päätös ja sen rajaus ovat `docs/AVOIMET.md`:ssä.
 *
 * **26.9.2026 alkaen numerot eivät myöskään kasva järjestelmän tekstikoon mukana**, ks.
 * [numberStyle]. Pixelillä koko 2.0 rivitti ryhmän, mikä oli pahempi kuin 24.8. mitattu törmäys.
 */
@Composable
private fun NumberGroup(
    numbers: List<Int>,
    points: List<Point>,
    touch: BoardTouch,
    modifier: Modifier = Modifier,
) {
    val pressed = touch.press?.number
    val base = numberStyle()
    Row(modifier = modifier.fillMaxWidth()) {
        numbers.forEach { number ->
            val point = points.firstOrNull { it.number == number }
            val hit = pressed == number
            val movable = number in touch.movable
            // Painettu numero kasvaa eikä vain vaihda väriä (Tommi 4.10.2026: *"pieni näyttö on
            // lähes sokealle todella tulkinnanvarainen"*). **Kasvu on piirrossa eikä fontissa:**
            // isompi fonttikoko kasvatti rivin korkeutta, ja koko lauta hyppäsi painalluksen ajaksi
            // (Pixel 8a, kuori 4.10.2026). Skaalaus ei koske asetteluun.
            val style = if (!hit) base else base.copy(
                fontWeight = FontWeight.Bold,
                textDecoration = if (movable) null else TextDecoration.LineThrough,
            )
            // Siirroton painallus saa tumman laatan (Tommin valinta 6.10.2026, mockup B). Pelkkä
            // yliviiva samalla harmaalla kuin vierusnumerot ei erottunut Pixelin pelissä
            // (`sessio-6-10-iltapaiva`, kehys b017). Laatta on tekstin kokoinen eikä sarakkeen,
            // ja vain vaakapadding, joten rivin korkeus ei muutu.
            val tile = hit && !movable
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).then(
                    if (hit) Modifier.graphicsLayer(scaleX = POINT_PRESS_SCALE, scaleY = POINT_PRESS_SCALE)
                    else Modifier
                ),
            ) {
                Text(
                    text = number.toString(),
                    style = style,
                    color = when {
                        hit && movable -> Palette.Accent
                        hit -> Palette.TextPrimary
                        point == null -> Palette.TextMuted.copy(alpha = UNKNOWN_ALPHA)
                        else -> Palette.TextSecondary
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = if (tile) {
                        Modifier
                            .background(Palette.PanelBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 3.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                )
            }
        }
    }
}

/** Painetun numeron koko suhteessa tavalliseen, ks. [PointPress]. */
private const val POINT_PRESS_SCALE = 1.6f

@Composable
private fun WedgeRow(
    board: BoardState,
    roles: BoardRoles,
    metrics: BoardMetrics,
    touch: BoardTouch,
    numbers: List<Int>,
    pointsDown: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxHeight()) {
        numbers.forEach { number ->
            PointWedge(
                number = number,
                point = board.points.firstOrNull { it.number == number },
                roles = roles,
                metrics = metrics,
                moveHref = touch.movable[number],
                onFollow = touch.onFollow,
                press = touch.press,
                onMiss = touch.onMiss,
                pointsDown = pointsDown,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Yhden pisteen kiila ja nappulat. Kolme tapausta ja kolme eri piirtoa, koska ne
 * tarkoittavat eri asiaa:
 *
 * - `null`: sivu ei nimennyt tätä pistettä. **Tuntematon, ei tyhjä**, ja Mini-skeemassa
 *   tämä koskee jokaista saraketta.
 * - `count == 0`: aidosti tyhjä piste.
 * - `count > 0`: nappuloita.
 *
 * Kiila (kolmio) piirretään aina numerosta riippumatta siitä onko piste tunnettu, koska
 * ruudukko on laudan oma rakenne eikä jäsennyksen tulos. Väri vuorottelee pistenumeron
 * pariteetin mukaan, ei omistajan: se on lautagrafiikkaa, ei pelitilaa. Numero itse on
 * omassa [NumberRow]issaan, ei tässä.
 *
 * Värit tulevat [BoardLook]ista eikä teemasta: lauta esittää joko Monte Carlo X-22:ta
 * (oranssi/vihreä) tai sivuston omaa skeemaa, ei sovelluksen teemaa. Pariteetti on
 * lautagrafiikkaa eikä pelitilaa, ja se on sama molemmissa: parillinen piste on sivustolla
 * tumma kiila (mitattu fixturesta `move_board.html`).
 *
 * Kiila täyttää sarakkeen leveyden, ja erottelun tekee sarakkeen oma sivupadding.
 * **Nappulapino on kiilan sisarus eikä lapsi**: kolmion muotoon leikattuna pino leikkautuisi
 * kärkeä kohti kapenevaksi, eli nappuloista näkyisi sitä vähemmän mitä korkeammalle pino
 * yltää. Pino alkaa kiilan kannasta (numeron puolelta) ja kasvaa kärkeä (muuria) kohti,
 * samaan tapaan kuin oikealla laudalla.
 */
@Composable
private fun PointWedge(
    number: Int,
    point: Point?,
    roles: BoardRoles,
    metrics: BoardMetrics,
    /**
     * Sivun antama siirtolinkki tältä pisteeltä, tai null kun siirto ei ole tarjolla.
     *
     * **Null on tässä sivun sana eikä ruudun päätelmä.** Piste ei siis ole klikattava sillä
     * perusteella että siinä on omia nappuloita, vaan sillä että sivu antoi sille linkin.
     * Sääntöjä ei tunneta täällä eikä niitä tarvitse tuntea.
     */
    moveHref: String?,
    onFollow: (String) -> Unit,
    /** Painetun pisteen tila, tai null kun kytkin on pois. Ks. [PointPress]. */
    press: PointPress?,
    /** Ks. [BoardTouch.onMiss]; soi vain kun tältä pisteeltä ei ole linkkiä. */
    onMiss: (() -> Unit)?,
    pointsDown: Boolean,
    modifier: Modifier = Modifier,
) {
    val miss by rememberUpdatedState(if (moveHref == null) onMiss else null)
    val wedgeColor = if (number % 2 == 0) roles.look.wedgeEven else roles.look.wedgeOdd
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp)
            // Koko sarake ottaa kosketuksen vastaan eikä pelkkä nappula: kiila on kapea, ja
            // nappulan kokoinen kohde alittaisi Materialin 48 dp:n kosketusalueen reilusti.
            // `clickable` vain kun linkki on, joten piste jolta ei voi siirtää ei myöskään
            // reagoi eikä jätä semantiikkapuuhun toimintoa.
            .then(if (press != null) Modifier.observePress(press, number) { miss?.invoke() } else Modifier)
            .then(
                if (moveHref != null) Modifier.clickable { onFollow(moveHref) } else Modifier
            ),
        contentAlignment = if (pointsDown) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(pointWedge(pointsDown))
                .background(wedgeColor),
        )

        // Siirrettävä piste erottui reunuksella 10.8.-25.8.2026, ja korostus poistettiin
        // Tommin päätöksellä (`docs/UI.md`). Mittaus purki sen perustelun: sivusto antaa
        // linkin jokaiselle omalle nappulalle, joten reunus kertoi missä on omia nappuloita
        // eikä mikä on laillista. Kosketuskohde on yhä koko sarake, eli poistui merkintä
        // eikä osumapinta.
        CheckerStack(
            paint = roles.paint(point?.owner),
            count = point?.count ?: 0,
            metrics = metrics,
            growUp = !pointsDown,
        )
    }
}

/**
 * Kirjaa painalluksen [PointPress]iin kuluttamatta sitä, jotta `clickable` saa saman
 * kosketuksen ja siirto toimii kuten ilman kytkintä. Tarkkailu koskee myös pistettä jolla ei
 * ole linkkiä: juuri siinä osumaton napautus tarvitsee vastauksen. Semantiikkapuuhun ei jää
 * mitään, koska tämä ei ole toiminto. Sormen liukuminen sarakkeen ulkopuolelle peruu merkin heti,
 * samoin kuin se peruu `clickable`n.
 *
 * **Irrotusta ei odoteta `waitForUpOrCancellation`illa**, koska `clickable` kuluttaa irrotuksen
 * ennen tätä, ja kulutettu irrotus näyttää sille peruutukselta. Siirrettävän pisteen numero
 * katosi siksi heti eikä viipynyt (Pixel 8a, kuori 4.10.2026), ja linkittömällä pisteellä
 * viipymä toimi koska kuluttajaa ei ollut.
 */
private fun Modifier.observePress(
    press: PointPress,
    number: Int,
    /** Irrotus sarakkeen sisällä, eli napautus joka osui tähän pisteeseen. */
    onRelease: () -> Unit,
) = pointerInput(press, number) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        press.number = number
        press.down = true
        var cancelled = false
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.none { it.pressed }) break
            if (event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                cancelled = true
                break
            }
        }
        press.down = false
        if (cancelled && press.number == number) press.number = null
        if (!cancelled) onRelease()
    }
}

/** Kolmio jonka kärki osoittaa muuria kohti: alaspäin yläpuoliskolla, ylöspäin alapuoliskolla. */
private fun pointWedge(pointsDown: Boolean) = GenericShape { size, _ ->
    if (pointsDown) {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width / 2f, size.height)
    } else {
        moveTo(0f, size.height)
        lineTo(size.width, size.height)
        lineTo(size.width / 2f, 0f)
    }
    close()
}

/**
 * Muurin (bar) yksi segmentti: pystysuora tili pisteiden 18/19 ja 7/6 välissä, johon
 * syöty nappula sijoitetaan odottamaan paluuta. Segmentti on oma laatikkonsa ylä- ja
 * alapuoliskolla, jotta [MiddleStrip] mahtuu niiden väliin samalla korkeudella kuin
 * pisterivit; koko muuri ei siis ole yksi yhtenäinen sarake vaan kaksi, samaan tapaan
 * kuin oikealla laudalla nappulat pinoutuvat keskiviivaa kohti kummaltakin puolelta.
 *
 * Jako tulee [BoardRoles]ilta. `board.bar` ei itse kerro kumman puoliskon segmenttiin
 * nappula kuuluu, joten sivu ei todista jakoa, mutta pelaajan oma väri on silti pip-vahdin
 * kautta tiedossa eikä arvattu.
 *
 * **Suunta kääntyi 6.9.2026 (Tommin havainto pelatessa): yläsegmentti saa kirjautuneen
 * pelaajan nappulan, alasegmentti vastustajan.** Ensimmäinen jako seurasi paneelien
 * järjestystä, eli vastustaja ylhäällä ja itse alhaalla, ja se oli väärä laina: muuri ei
 * ole pelaajan paikka vaan nappulan matkan alku. Omat nappulat tulevat muurilta sisään
 * **vastustajan kotikenttään** (pisteet 19–24, ylärivi), joten oma nappula kuuluu sinne
 * mihin se on menossa. Vastustajan nappula tulee vastaavasti alas oman kodin puolelle.
 * Kosketus seuraa nappulaa eikä segmenttiä, koska `href` tulee nappulan mukana.
 *
 * Piirretään myös tyhjänä, koska se on laudan jakaja: katoava sarake saisi laudan
 * hyppimään sen mukaan onko jollakin nappula muurilla.
 *
 * **Segmentti on kosketettava 22.8.2026 alkaen, ja koko segmentti eikä nappula.** Sivulla
 * sama teko on 21×21 pikselin kuva, kun jokainen kiila on 23×115, eli yli viisinkertainen
 * kohde. Ero mitattiin sinä iltana jona muurilta siirtäminen ylipäätään tuli mahdolliseksi
 * (`docs/AVOIMET.md`), ja se oli syy siihen miksi teko ei löytynyt myöskään selaimesta.
 * Sovellus ei siis toista sivun kohdekokoa, koska mikään ei vaadi sitä: linkki on sama,
 * vain osumapinta on isompi.
 *
 * **Nappula on keskitilan vieressä 25.8.2026 alkaen, ei laudan ulkoreunassa** (Tommin havainto
 * laiteajossa). Segmentti kohdisti pinon samalla säännöllä kuin kiila, eli kannasta kärkeä
 * kohti, ja se oli väärä laina: kiilan kanta on numeron puolella, mutta muurilla nappulan
 * paikka on laudan keskellä. Yllä oleva kappale lupasi jo keskiviivaa kohti pinoutumista, eli
 * koodi ja sen oma kuvaus sanoivat eri asiaa. Ulkoreunassa nappula luki myös väärin sen takia
 * että se asettui kiilojen numerorivin tasalle, jolloin se näytti kuuluvan pisteelle.
 */
@Composable
private fun BarSegment(
    board: BoardState,
    roles: BoardRoles,
    metrics: BoardMetrics,
    touch: BoardTouch,
    top: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Omistettu kuutio segmentin ulkopäässä (Tommin tilaus 14.9.2026 illalla: *"tuplauskuutio
     * ei numeroiden sekaan tilaa viemään vaan muurin ylä- tai alaosaan"*, ja peruste:
     * *"nappulalokero olisi poistetuille pyhitetty"*). Muurin nappulat pinoutuvat keskeltä
     * ulospäin, joten pää on vapaa, ja kuutio ei maksa korkeutta eikä leveyttä. Ylhäällä
     * vastustajan kuutio, alhaalla oma, sama jako kuin lokerossa 9.8.–14.9.2026.
     */
    cube: Cube? = null,
    onDouble: (() -> Unit)? = null,
    cubeAttention: Boolean = false,
    /** Ks. [Board]in `pipsOnBar` ja [BarPips]. */
    pipsOnBar: Boolean = false,
    // Pino väisti 23.9.–29.9.2026 muurilla makaavaa kuutiota (`cubeGap`). Omistamaton kuutio
    // siirtyi lokerosarakkeeseen 29.9.2026, joten muurilla ei ole enää mitään väistettävää.
) {
    // Ylhäällä oma, alhaalla vastustajan: nappula piirtyy sille puolelle jonne se on
    // muurilta menossa, ks. tämän funktion kuvaus.
    val color = if (top) roles.selfColor else roles.opponentColor
    val onBar = board.bar.firstOrNull { it.color == color }
    val count = onBar?.count ?: 0
    // Linkki tulee sivulta ja on olemassa vain silloin kun teko on mahdollinen: oma
    // nappula, oma vuoro ja noppa jaljella. Tyhja segmentti ei siis ole kosketettava
    // vaikka se piirretaan.
    val href = onBar?.href?.takeIf { count > 0 }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(roles.look.frame)
            .woodGrain(roles.look.frameGrain, seed = 4)
            .then(
                if (href != null) {
                    // Muuri sai kiilan kanssa saman reunuksen 25.8.2026, ja molemmat
                    // poistettiin samana päivänä Tommin päätöksellä (`docs/UI.md`).
                    // Segmentti on yhä kosketettava koko korkeudeltaan.
                    Modifier.clickable { touch.onFollow(href) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 2.dp),
        contentAlignment = if (top) Alignment.BottomCenter else Alignment.TopCenter,
    ) {
        // Pipit ensin, jotta muurille lyöty nappula piirtyy niiden päälle eikä alle: pino
        // kasvaa keskeltä ulospäin ja ulottuu pippeihin vasta neljästä nappulasta.
        if (pipsOnBar) {
            // Ylhäällä vastustaja, alhaalla oma, sama jako kuin pelaajakorteilla.
            val self = board.players.getOrNull(1)
            val opponent = board.players.getOrNull(0)
            val (mine, theirs) = if (top) opponent to self else self to opponent
            val pips = mine?.pips
            val otherPips = theirs?.pips
            if (pips != null && otherPips != null) {
                BarPips(
                    pips = pips,
                    lead = DgBoard.pipLead(pips, otherPips),
                    modifier = Modifier
                        .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                        .padding(vertical = 6.dp),
                )
            }
        }
        CheckerStack(
            paint = roles.paint(color),
            count = count,
            metrics = metrics,
            growUp = top,
        )
        if (cube != null) {
            // Kuutio kutistuu muurin levyiseksi kapealla laudalla: muuri on yksi sarake
            // kolmestatoista ja kuutio on nappulaa leveämpi, joten ilman tätä se
            // valuisi kiilojen päälle. Leveällä laudalla koko on sama kuin kaistalla.
            BoxWithConstraints(
                modifier = Modifier
                    .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                val fitted = if (maxWidth < metrics.cubeSize) metrics.cubeSize.coerceAtMost(maxWidth) else metrics.cubeSize
                CubeFace(cube, metrics.copy(cubeSize = fitted), onDouble, cubeAttention)
            }
        }
    }
}

/**
 * Pelaajan pipit muurin päässä kolmella rivillä: `Pips`, luku ja etumatka sulkeissa (Tommin
 * päätös 5.10.2026, malli *"Pips, 126, (+10)"*, pilkut rivien erottimina). Ylhäällä
 * vastustajan, alhaalla oma.
 *
 * **Muuri kantoi pipit kerran ennenkin** (numerorivin muurin väli 15.8.–14.9.2026), ja silloin
 * `Pips 131 (+5)` ei mahtunut yhdelle riville tabletillakaan. Kolme riviä ratkaisee juuri sen:
 * leveintä riviä on viisi merkkiä. Kapeimmalla muurilla (puhelin pystyssä) koko pienenee
 * mahtuakseen, ja koko lasketaan leveimmästä rivistä, jotta kolme riviä ovat samankokoisia.
 * Luku ei siis koskaan leikkaudu, ja se oli 24.8.2026 kutistuksen peruste: leikattu pip on
 * väärä luku joka näyttää kelvolliselta.
 *
 * **Lähtökoko on `bodyLarge` eikä pistenumeroiden koko** (Tommin havainto ensimmäisestä
 * kaappauksesta 5.10.2026: *"tabletin keskipalkin pipseihin voi käyttää isompaa fonttia"*).
 * Tabletin muurilla pistenumeroiden `labelSmall` jätti puolet leveydestä tyhjäksi. Kapealla
 * muurilla sama sovitus pienentää kokoa kuten ennenkin.
 */
@Composable
private fun BarPips(pips: Int, lead: String, modifier: Modifier = Modifier) {
    val lines = listOf(
        stringResource(R.string.board_pips_word),
        pips.toString(),
        stringResource(R.string.board_pips_paren, lead),
    )
    val base = MaterialTheme.typography.bodyLarge
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val room = constraints.maxWidth
        val widest = lines.maxOf { measurer.measure(it, base, maxLines = 1, softWrap = false).size.width }
        val style = if (widest <= room || widest == 0) base else {
            val scale = room.toFloat() / widest
            base.copy(
                fontSize = base.fontSize * scale,
                lineHeight = if (base.lineHeight.isSpecified) base.lineHeight * scale else base.lineHeight,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            lines.forEach {
                Text(
                    text = it,
                    style = style,
                    // Pistenumeroiden väri eikä kortin korostusväri: muuri on kehyksen puuta,
                    // ja oranssi katosi Pixelillä vaalean puulaudan kehykseen (5.10.2026).
                    color = Palette.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Keskitila puoliskojen välissä: tuplauskuutio laudan vasemmassa laidassa, nopat
 * pelaajittain sen oikealla puolella (vastustaja vasemmalla, kirjautunut pelaaja oikealla),
 * ja sivun toimintonapit ([MiddleActionsRow]) kirjautuneen pelaajan puolella keskellä.
 *
 * **Kuutio ei ole enää tässä vaan lokerosarakkeessa**, ja tämä kappale on jäänyt sanomaan
 * vanhaa kahdesti: se siirtyi sarakkeeseen ulkoasumuutoksessa, ja 9.8.2026 alkaen sen paikka
 * riippuu omistajasta, joka luetaan sivulta. Omistettu on muurin päässä 14.9.2026 alkaen
 * ([BarSegment]), omistamaton ja tarjottu tässä tai [CubeBadge]ssa, ks. [cubeOnStrip].
 *
 * Jako pelaajittain tulee [BoardRoles]ilta samasta [selfCheckerColor]-päättelystä kuin
 * [BarSegment]. Ilman sitä jako olisi pitänyt tehdä väreittäin eikä pelaajittain, koska
 * [PlayerPanel] ei itse kanna `CheckerColor`ia.
 *
 * **Napit asuvat 14.8.2026 alkaen tässä, aina kirjautuneen pelaajan puolella** (Tommin
 * päätös): napit ovat aina hänen tekojaan, koska sivu tarjoaa lomakkeen vain sille jonka
 * vuoro on. Ne täyttävät saman tilan jossa aiemmin oli pelkkä väliä täyttävä [Spacer], eli
 * keskitila ei tarvinnut leveysmuutosta napeille kotia varten.
 *
 * Korkeus on kiinteä, toisin kuin kiiloilla: sisältö on kuutio, nopat ja nyt myös napit,
 * joiden koko ei veny, joten venyvä keskitila veisi tilaa vain kiiloilta.
 */
@Composable
private fun MiddleStrip(
    board: BoardState,
    roles: BoardRoles,
    metrics: BoardMetrics,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    trailing: @Composable () -> Unit = {},
    showSkip: Boolean = true,
    /** Ks. [MiddleActionsRow]: epätosi kun chat-kortti omistaa sivun napit. */
    showSubmits: Boolean = true,
    /** Pyyntö on kesken: toimintorivillä pelkkä kaari, ks. [actionItems]. */
    busy: Boolean = false,
    /** Epätosi kun napit ovat sivupaneelissa ([PanelActionStack]); kaista on silloin vain nopat. */
    showActions: Boolean = true,
    /** Ks. [VerifyBox]. */
    verify: VerifyBox = VerifyBox.NONE,
    /** Omien noppien painallus, ks. [diceTapFor]. Null kun painallus ei tee mitään. */
    onDiceTap: (() -> Unit)? = null,
    /** Vastustajan noppien painallus, ks. [diceRollTapFor]. */
    onOpponentDiceTap: (() -> Unit)? = null,
    /**
     * Kuutio kaistalla, tai null kun se on lokerosarakkeessa. Ks. kaistan kuutiokohta alla
     * ja [OffTrayColumn]: sama kuutio on aina tasan yhdessä paikassa.
     */
    cube: Cube? = null,
    /** Sivu tarjoaa `Accept`ia: kuutio on tarjottu ja piirtyy oman puoliskon keskelle. */
    cubeOffered: Boolean = false,
    onDouble: (() -> Unit)? = null,
    cubeReminder: Boolean = false,
    /** Katkotilan `Refresh` kaistan keskelle, päällimmäisenä. Ks. [UnconfirmedRefresh]. */
    unconfirmedRefresh: UnconfirmedRefreshSpec? = null,
    /** Sivun tilarivit vapaalla puoliskolla, ks. [stripNotes] ja [situationSlot]. Tyhjä kun ne ovat paneelissa. */
    notes: List<StripNoteSpec> = emptyList(),
    notesSlot: StripSlot? = null,
    /** Rastihuomautus samassa lokerossa tilarivien alla, ks. [verifyHintSlot]. */
    verifyHint: String? = null,
    verifyHintSlot: StripSlot? = null,
    onDismissVerifyHint: () -> Unit = {},
    // Omat nopat olivat 13.9.2026 yhden illan paneelia vastapäisessä laidassa
    // (kaksikätinen pelaaminen). Tommi kumosi sen pelisession jälkeen 14.9.2026:
    // "nopat puoliskon keskelle". Sääntö on taas sama molemmille pelaajille.
) {
    // **Kaista on Box eikä Row 26.8.2026 alkaen, ja syy on nappirivin kohdistus.**
    // Rivissä napit saivat vain sen tilan joka jäi noppalokeroiden väliin, joten ne
    // asettuivat jäljelle jääneen tilan keskelle eivätkä laudan keskelle: tyhjän lokeron
    // vapauttama kaista veti ryhmän omalle puolelleen. Päällekkäisenä nopat ovat kiinni
    // omissa laidoissaan ja napit laudan keskellä, eli molemmat kohdistuvat siihen mihin
    // ne kuuluvat eivätkä toisiinsa.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.bandHeight)
            // Tilarivien pino saa kasvaa kiilojen päälle (ks. alla), joten kaista piirtyy
            // puoliskojen jälkeen. Muuten alempi puolisko maalaisi pinon alaosan yli.
            .zIndex(1f)
            .background(roles.look.band)
            .woodGrain(roles.look.feltGrain, seed = 5)
            // Muuri jatkuu kaistan läpi (13.9.2026): huovan värinen kaista katkaisi sen, ja
            // katkos näytti virheeltä eikä valinnalta. Sama sarake kuin [HalfBoard]in
            // muurilla, eli yksi kolmestatoista. Kehyksen värisellä kaistalla tämä on
            // näkymätön, joten haaraa ei tarvita.
            .drawBehind {
                val w = size.width / DgBoard.PLAY_COLUMNS
                drawRect(
                    color = roles.look.frame,
                    topLeft = Offset((size.width - w) / 2f, 0f),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                )
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Vastustajan nopat vasemmalla, omat oikealla, sama jako kuin puoliskoilla ja
        // sivupaneelissa. Sijainti on siis sama tieto kahteen kertaan värin kanssa, ja
        // se on tarkoitus: väri kertoo kenen, paikka vahvistaa saman ilman lukemista.
        //
        // **Nopat keskitetään kuuden kiilan ryhmän keskelle** (Tommi 10.8.2026), eli
        // kolmannen ja neljännen kiilan kohdalle. Ne olivat ensin laitoihin asti, jolloin
        // näytön lova osui vasemmanpuoleisen nopan päälle, ja sen jälkeen keskellä nipussa,
        // jolloin ne eivät osoittaneet kummankaan puoliskoon.
        //
        // Kiilan leveys johdetaan nappulasta samalla suhteella kuin lauta itse, joten ryhmä
        // pysyy kohdakkain kiilojen kanssa myös silloin kun nappula skaalautuu.
        // **Nopat puoliskon keskelle kun kaistalla ei ole nappeja** (Tommin päätös
        // 2.9.2026: *"keskitä nopat vuorossa olevan pelaajan keskipaneelin"*). Napit
        // siirtyivät sivupaneeliin samana iltana, joten kaista on tabletilla pelkät
        // nopat eikä neljän kiilan varaus ole enää tarpeen: lokero on koko puolisko,
        // kuusi kiilaa, ja nopat ovat sen keskellä. Kun napit ovat kaistalla (ei
        // paneelia), varaus pysyy neljässä kuten 26.8.2026 alkaen.
        val groupPoints = if (showActions) DICE_GROUP_POINTS else HALF_POINTS
        val group = metrics.checkerSize / DgBoard.CHECKER_PER_COLUMN * groupPoints
        val opponentDice = board.dice.filter { it.owner == roles.opponentColor }
        val selfDice = board.dice.filter { it.owner == roles.selfColor }

        // **Tyhjä noppalokero ei varaa leveyttä** (Tommi 26.8.2026). Nopat ovat aina vain
        // toisella puolella, joten toinen lokero on koko ajan tyhjä, ja se varasi silti
        // kahden kiilan levyisen kaistan josta nappirivi puristui. Rivi vieri, ja pisin
        // nappi jäi laidan taakse: `Skip Game` teki puutteesta näkyvän, mutta se oli
        // olemassa jo ennen sitä.
        //
        // **Näkyvät nopat eivät liiku tästä.** Ne ovat kiinni omassa lokerossaan, joka on
        // rivin laidassa; vapautuva tila on vastakkaisella puolella, eli sitä ei ole
        // kohdistettu mihinkään. Kohdistus kiiloihin (kolmas ja neljäs) säilyy sekin,
        // koska se lasketaan lokeron omasta leveydestä eikä rivin keskikohdasta.
        //
        // Ehto on nopan olemassaolo eikä vuoro. Vuoro on pääteltyä, nopat ovat sivulla.
        if (opponentDice.isNotEmpty()) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart).width(group),
                contentAlignment = Alignment.Center,
            ) {
                DiceGroup(opponentDice, roles, metrics, onTap = onOpponentDiceTap)
            }
        }
        // Muurin kohta täyttyy napeista kun niitä on. Tässä oli aiemmin pip-vahti, ja se
        // siirtyi sivupaneeliin 10.8.2026 kun nopat tulivat lähemmäs keskustaa: se on
        // jäsennyksen tarkistus eikä laudan tieto, joten se ei kilpaile laudan omista
        // kohdista. Rivi vierii vaakasuunnassa jos napit eivät mahdu tähän tilaan, sama
        // periaate kuin sivupaneelissa oli ennen tätä siirtoa.
        if (selfDice.isNotEmpty()) {
            // Puoliskon keskelle kuten 2.9.2026 alkaen; laita 13.9.2026 kumottiin 14.9.2026.
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).width(group),
                contentAlignment = Alignment.Center,
            ) {
                DiceGroup(selfDice, roles, metrics, onTap = onDiceTap)
            }
        }
        // **Omistamaton kuutio muurilla, tarjottu kuutio noppien paikalla** (Tommin päätös
        // 3.9.2026, toinen muoto; ensimmäinen vei myös omistetun kuution puoliskolle, ja
        // se palautettiin lokerosarakkeeseen samana iltana). Backgammonin tapa: omistamaton
        // kuutio makaa muurilla kummankaan puolella, ja tarjottu kuutio työnnetään sille
        // jonka vastaus se on. Sivu ei merkitse tarjousta kuutioon (kuva on yhä `cube1`
        // keskellä), joten tarjous tulee kutsujalta lomakkeen `Accept`-napista.
        //
        // Tarjottu kuutio on **oman puoliskon keskellä**, siinä mihin omat nopat tulisivat:
        // vastaus on oma, ja tarjouslaudalla ei ole noppia. Kuution klikkaus on silloin
        // Accept samaa reittiä kuin nappi (rasti, dialogi). Tämä kaista on tabletilla
        // pelkät nopat (napit ovat paneelissa), joten kuutio mahtuu tänne samalla ehdolla
        // jolla nopat keskitettiin puoliskolle 2.9.2026; kun napit ovat kaistalla, kuutio
        // pysyy lokerosarakkeessa ([OffTrayColumn]) ja tänne tulee null. Tuntematon
        // omistaja (`UNKNOWN`) piirtyy muurille: väärä puoli on pahempi kuin puolettomuus.
        if (cube != null) {
            val alignment = if (cubeOffered) Alignment.CenterEnd else Alignment.Center
            Box(
                modifier = Modifier.align(alignment).width(group),
                contentAlignment = Alignment.Center,
            ) {
                CubeFace(cube, metrics, onDouble, cubeReminder)
            }
        }
        // **Tilanneteksti vapaalla puoliskolla** (Tommin päätös 8.9.2026 kesken pelisession:
        // *"YrjoSuomela has doubled. teksti sopisi keskipaneliin"*, ja tarkennus *"laudan
        // keskikaistalle, sinne missä kuutio ja nopat näkyvät"*). Teksti oli siihen asti
        // sivupaneelin pohjalla nimikortin alla. Puolisko valitaan [situationSlot]issa:
        // vastustajan puoli kun se on vapaa, koska teksti kertoo yleensä hänen teostaan
        // (*has doubled*, *declines*), muuten oma puoli, ja kun kumpikaan ei ole vapaa tai
        // kaistalla on napit, teksti jää paneeliin kuten ennen. Sama lokero ja sama leveys
        // kuin nopilla, joten teksti on kohdakkain sen kanssa mitä se kommentoi.
        //
        // **Rastihuomautus samaan lokeroon, tilannetekstin alle** (Tommin päätös 14.9.2026,
        // vaihtoehto A neljästä kuvasta). Accept-tapauksessa se on *X has doubled.* -rivin
        // alla, Double-tapauksessa oman puoliskon keskellä koska vastustajan puoliskolla ovat
        // hänen noppansa. Lokero on pystypino kaistan keskikohdassa: kaksi riviä kasvaa
        // ylös ja alas yhtä paljon eikä valu kiilojen päälle toiselta reunalta (Tommin
        // tarkennus samana päivänä).
        //
        // **Samaan pinoon myös peruutus, vahdit ja toistojono** (Tommin päätös 14.9.2026,
        // ks. [stripNotes]). Pino saa kasvaa kiilojen päälle ja rivi saa rivittyä lokeron
        // leveydellä (Tommi: *"rivitä fiksusti, pino saa kasvaa kiilojen päälle"*): tapaus
        // jossa rivejä on monta on harvinainen, ja katkaistu rivi kertoisi vähemmän kuin
        // kiilan kärjen peittävä.
        for (slot in StripSlot.entries) {
            val notesHere = notes.takeIf { notesSlot == slot }.orEmpty()
            val hintHere = verifyHint?.takeIf { verifyHintSlot == slot }
            if (notesHere.isEmpty() && hintHere == null) continue
            val alignment = if (slot == StripSlot.OPPONENT) Alignment.CenterStart else Alignment.CenterEnd
            // **Korkeus ei ole kaistan korkeus** (25.9.2026, puhelimen vaakalauta,
            // `puhelin-vaaka-verify-decline.png`). Rajattuna pino sai kaistan korkeuden, ja
            // kaksirivinen *Rolled back* vei siitä lähes kaiken: *eino has doubled.* sai
            // jäännöksen ja siitä näkyi vain pisterivi. Rajaamaton pino mitataan koko
            // korkeudelleen, ja Box keskittää sen, eli ylitys jakautuu tasan ylös ja alas.
            Column(
                modifier = Modifier
                    .align(alignment)
                    .width(group)
                    .wrapContentHeight(unbounded = true),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                notesHere.forEach { StripNote(it.text, color = it.color, bold = it.bold) }
                hintHere?.let { StripNote(it, onTap = onDismissVerifyHint) }
            }
        }
        // **Napit väistävät nopat, eivät piirry niiden päälle** (Tommin pyyntö 26.8.2026).
        // Aiemmin rivi sai koko kaistan leveyden ja piirtyi viimeisenä, joten isolla
        // järjestelmäfontilla teksti tuli noppien päälle: `Reminders` 320 dp:llä ja
        // fontScale 1.3:lla, `Cube reminder` fontScale 2.0:lla (mitattu laitteella).
        // Piirtojärjestys oli tietoinen valinta, mutta se nojasi siihen että peittyminen
        // on reunatapaus; iso fontti teki siitä tavallisen tilanteen, ja peitetty noppa on
        // pelitietoa jonka voi lukea väärin ilman että mikään kertoo siitä.
        //
        // Varaus on sen lokeron levyinen jossa nopat ovat, eikä molempien: tyhjä lokero ei
        // varaa leveyttä muutenkaan (ks. yllä), joten rivi saa käyttää sen tilan.
        //
        // **Napit pysyvät laudan keskellä niin kauan kuin ne mahtuvat sinne** peittämättä
        // noppia, ja siirtyvät vasta sitten sen verran kuin on pakko. Sijoittelu on siksi
        // oma `Layout` eikä kohdistus: kohdistus keskittäisi rivin jäljelle jääneen tilan
        // keskelle heti, eli veisi napit sivuun myös silloin kun ne mahtuisivat keskelle.
        // Se oli juuri se vika joka korjattiin aiemmin samana päivänä.
        //
        // Jos rivi ei mahdu varauksen jälkeenkään, se vierii vaakasuunnassa kuten ennenkin.
        // Nappi jää siis pahimmillaan vierityksen taakse, ja se on korjattavissa
        // vierittämällä; peitetty noppa ei ollut.
        val diceReserveStart = if (opponentDice.isNotEmpty()) group else 0.dp
        val diceReserveEnd = if (selfDice.isNotEmpty()) group else 0.dp
        if (showActions) Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {
                MiddleActionsRow(board, metrics, onFollow, onPress, trailing, showSkip, showSubmits, verify, busy, arcLookOf(roles))
            },
        ) { measurables, constraints ->
            val start = diceReserveStart.roundToPx()
            val end = diceReserveEnd.roundToPx()
            val width = constraints.maxWidth
            val available = (width - start - end).coerceAtLeast(0)
            val row = measurables.first().measure(
                constraints.copy(minWidth = 0, maxWidth = available),
            )
            layout(width, row.height) {
                row.place(middleActionsX(width, row.width, start, end), 0)
            }
        }
        // Viimeisenä, jotta se on kuution ja nappirivin päällä; kaistan Box keskittää sen.
        unconfirmedRefresh?.let { UnconfirmedRefresh(metrics, it.onRefresh) }
    }
}

/**
 * Nappirivin vasen reuna keskikaistalla, kun noppalokerolle on varattava tila.
 *
 * Sääntö on kaksiosainen ja järjestys on se joka merkitsee: rivi on **laudan keskellä**
 * niin kauan kuin se mahtuu sinne peittämättä noppia, ja väistää vasta sitten sen verran
 * kuin on pakko. Pelkkä kohdistus jäljelle jäävään tilaan olisi siirtänyt rivin sivuun myös
 * silloin kun se olisi mahtunut keskelle, ja se oli juuri se vika joka korjattiin
 * 26.8.2026 aiemmin.
 *
 * Varaus koskee vain sitä laitaa jossa nopat ovat: tyhjä lokero ei varaa leveyttä.
 *
 * Luvut ovat pikseleitä, koska mittaus tehdään `Layout`issa.
 */
internal fun middleActionsX(width: Int, rowWidth: Int, start: Int, end: Int): Int {
    // Jos varaukset syövät koko kaistan, viimeinen sallittu kohta on alkuvaraus: napit
    // väistävät niin pitkälle kuin voivat, eikä lokeron päälle mennä kummassakaan
    // tapauksessa.
    val last = (width - end - rowWidth).coerceAtLeast(start)
    return ((width - rowWidth) / 2).coerceIn(start, last)
}

/** Kuinka paljon kokonaan pelattu noppa himmenee; 0,65 jättää silmät luettaviksi. */
private const val SPENT_DIE_DIM = 0.65f

@Composable
private fun DiceGroup(
    dice: List<Die>,
    roles: BoardRoles,
    metrics: BoardMetrics,
    /**
     * Ryhmän painallus, tai null kun ryhmä on pelkkä kuva. Painallus on **ryhmällä eikä
     * yksittäisellä nopalla**, koska teko koskee heittoa kokonaisuutena eikä sitä noppaa
     * johon osui: kumpikin sääntö puhuu kaikista nopista.
     */
    onTap: (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = if (onTap == null) Modifier else Modifier.clickable(onClick = onTap),
    ) {
        dice.forEach { die ->
            val paint = roles.paint(die.owner)
            // Pyöristys on suhde sivuun eikä kiinteä dp, ks. [DgBoard.CORNER_PER_SIDE].
            val corner = RoundedCornerShape(metrics.diceSize * DgBoard.CORNER_PER_SIDE)
            // **Pelattu noppa harmaantuu eikä katoa** (Tommin tilaus 2.9.2026). Kuutio
            // pysyy paikallaan, joten rivi ei hypi kokoamisen aikana ja heitto on
            // luettavissa loppuun asti; himmennys kertoo mikä on jo käytetty. Sivuston
            // tuplassa kaksi kuutiota kantavat neljä askelta, jolloin himmennys tulee
            // puolikas kerrallaan, ks. [Die.spent].
            Box(
                modifier = Modifier
                    .size(metrics.diceSize)
                    .alpha(1f - die.spent * SPENT_DIE_DIM)
                    .clip(corner)
                    .background(paint.fill)
                    .border(DgBoard.OUTLINE, Palette.Outline, corner),
                contentAlignment = Alignment.Center,
            ) {
                // **Silmät numeron sijaan** (Tommin pyyntö 26.8.2026). Sama esitystapa kuin
                // sivuston omissa noppakuvissa (`die_y3.gif` ym. näyttävät silmät), eli tämä
                // on askel sivua kohti eikä siitä pois. Numero jää varapoluksi arvolle jota
                // silmiksi ei voi piirtää: tuntematonta ei arvata eikä pudoteta.
                if (die.value in 1..6) {
                    DiePips(die.value, metrics.diceSize, paint.onFill)
                } else {
                    Text(
                        text = die.value.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = paint.onFill,
                    )
                }
            }
        }
    }
}

/**
 * Nopan silmät 1-6, väri on nappuliväriä vastaava tekstiväri kummassakin lautatilassa.
 *
 * Paikat ovat vakioruudukko kolmanneksin (0,26 / 0,50 / 0,74 sivusta) ja säde suhde sivuun,
 * samasta syystä kuin kulman pyöristys [DgBoard.CORNER_PER_SIDE]: noppa skaalautuu nappulan
 * mukana, joten kiinteä mitta piirtyisi eri näköisenä eri laitteilla.
 */
@Composable
private fun DiePips(value: Int, size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val side = this.size.minDimension
        val lo = 0.26f * side
        val mid = 0.5f * side
        val hi = 0.74f * side
        val pips = when (value) {
            1 -> listOf(mid to mid)
            2 -> listOf(lo to lo, hi to hi)
            3 -> listOf(lo to lo, mid to mid, hi to hi)
            4 -> listOf(lo to lo, lo to hi, hi to lo, hi to hi)
            5 -> listOf(lo to lo, lo to hi, mid to mid, hi to lo, hi to hi)
            else -> listOf(lo to lo, lo to mid, lo to hi, hi to lo, hi to mid, hi to hi)
        }
        pips.forEach { (x, y) ->
            drawCircle(color, radius = 0.09f * side, center = Offset(x, y))
        }
    }
}

/**
 * Ulos kannetut, omana sarakkeenaan laudan oikeassa laidassa. Lokero on **pelaajakohtainen
 * eikä värikohtainen**: vastustaja ylhäällä, kirjautunut pelaaja alhaalla, samassa
 * järjestyksessä kuin puoliskot ja sivupaneeli.
 *
 * Molemmat piirretään myös nollana, jottei lokeron olemassaolo välky sen mukaan onko
 * siinä jotain.
 *
 * **Napit asuivat tässä sarakkeessa 10.8.-14.8.2026, eivät enää.** Ne siirtyivät kokonaan
 * `MiddleStrip`iin (Tommin päätös 14.8.2026), koska yksi koti kaikilla näytöillä korvasi
 * kolme leveysehtoista kotia. Kuution paikka ei muuttunut siirrossa: sarake on yhä
 * `paino, keskitila, paino`, joten kuutio on samalla korkeudella kuin laudan oma keskitila.
 */
@Composable
private fun OffTrayColumn(
    board: BoardState,
    roles: BoardRoles,
    metrics: BoardMetrics,
    /** Pisteiden esitys, sama johdos kuin sivupaneelilla. Ks. [SidePanel]. */
    awayOf: (PlayerPanel) -> Int?,
    playerFacts: Boolean = false,
    onDouble: (() -> Unit)? = null,
    /** Kuutiomuistutus on voimassa: kuutio korostuu, ks. [reminderAttention]. */
    cubeReminder: Boolean = false,
    /**
     * Kuutio tässä sarakkeessa, tai null kun se on keskikaistalla ([MiddleStrip],
     * 3.9.2026). Lokerot pitävät paikkansa kummassakin tapauksessa, joten sarake ei
     * muuta mittojaan sen mukaan missä kuutio on.
     */
    cube: Cube? = board.cube,
) {
    Column(
        modifier = Modifier
            .width(metrics.trayWidth)
            .fillMaxHeight()
            // Tausta tulee tyylistä eikä peri huopaa (13.9.2026), ks. [BoardLook.trayColumn].
            .background(roles.look.trayColumn),
    ) {
        if (playerFacts) {
            TrayPlayerFacts(
                board.players.getOrNull(0),
                awayOf,
                roles.opponentPaint(),
                otherPips = board.players.getOrNull(1)?.pips,
            )
        }
        // Lokero on poistetuille pyhitetty 14.9.2026 alkaen (Tommin tilaus): omistettu
        // kuutio siirtyi muurin päähän ([BarSegment]), ja `CubeSlot` poistui.
        OffTray(
            board,
            roles.opponentColor,
            roles.opponentPaint(),
            roles.look,
            metrics,
            Modifier.weight(1f).fillMaxWidth(),
            cubeBelow = true,
        )
        CubeBadge(
            cube.takeIf { it?.position != CubePosition.TOP && it?.position != CubePosition.BOTTOM },
            metrics,
            onDouble,
            cubeReminder,
        )
        run {
            OffTray(
                board,
                roles.selfColor,
                roles.selfPaint(),
                roles.look,
                metrics,
                Modifier.weight(1f).fillMaxWidth(),
                cubeBelow = false,
            )
        }
        if (playerFacts) {
            TrayPlayerFacts(
                board.players.getOrNull(1),
                awayOf,
                roles.selfPaint(),
                otherPips = board.players.getOrNull(0)?.pips,
            )
        }
    }
}

/**
 * Yhden pelaajan nimi ja pisteet lokerosarakkeen päässä.
 *
 * **Tämä on olemassa vain sitä laitetta varten jolla sivupaneelia ei ole** (Tommin valinta
 * 14.8.2026). Galaxy Tab S7+:lla lauta vie 1244 dp 1285:stä, joten paneelin 150 dp:n kynnys
 * ei ylity eikä paneelia piirretä; ennen tätä muutosta ruudulla ei siis ollut pisteitä,
 * pippejä eikä pelaajien nimiä lainkaan, ja otteluluettelokaan ei näytä pisteitä. Vika
 * huomattiin kun Crawford-tähteä yritettiin todentaa laitteella eikä sitä voinut nähdä.
 *
 * **Pipit eivät ole tässä 15.8.2026 alkaen.** Ne näkyvät numerorivin muurin kohdalla
 * (`BarPips` (poistettu 14.9.2026, pipit [PlayerPanelView]issä)) riippumatta siitä onko tämä sarake näkyvissä, joten sarakkeella ei ole enää
 * omaa pip-lukua kannettavanaan.
 *
 * **Sija on pelaajan oma pää saraketta**, eli vastustaja ylhäällä ja kirjautunut alhaalla.
 * Se on sama järjestys kuin puoliskoilla, lokeroilla ja sivupaneelilla, joten mikään ruudulla
 * ei väitä eri asiaa kuin toinen.
 *
 * **Turnauksen nimi ja ottelun pituus jäävät pois** eivätkä katkaistuina mukaan. Sarake on
 * noin 110 dp leveä, ja katkaistu nimi on juuri se hinta jonka 10.8.2026 tehty päätös hyväksyi
 * paneelissa mutta jota tämä ei osta: paneelissa katkaisu on vaihtoehto laudan kutistamiselle,
 * täällä se olisi vaihtoehto tyhjälle tilalle.
 */
@Composable
private fun TrayPlayerFacts(
    panel: PlayerPanel?,
    awayOf: (PlayerPanel) -> Int?,
    paint: CheckerPaint,
    /** Toisen pelaajan pip-luku etumatkaa varten, ks. [PlayerPanelView]. */
    otherPips: Int?,
) {
    if (panel == null) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sama nappulanvärin merkki kuin paneelissa, ja samasta syystä: ilman sitä
            // paneelin ja laudan yhdistää vain sijainti.
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(paint.fill),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = panel.player.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = ratedNameColor(panel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Pistekenttä samassa muodossa kuin paneelissa, ks. [scoreText].
        scoreText(panel, awayOf(panel))?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = Palette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Pipit kolmantena rivinä kuten paneelissa, mutta ilman sanaa: sarake on kapea, ja
        // luku etumatkoineen on se osa joka kantaa tiedon. Muurin väli on tyhjä 14.9.2026
        // alkaen, joten ilman tätä puhelin jäisi ilman pippejä kokonaan.
        val pips = panel.pips
        if (pips != null && otherPips != null) {
            Text(
                text = stringResource(R.string.board_pips_short, pips, DgBoard.pipLead(pips, otherPips)),
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Tuplauskuutio lokerosarakkeessa, pisteiden 1 ja 24 oikealla puolella.
 *
 * **Paikka on ilmainen:** se on juuri se keskitilan korkuinen väli joka jää lokeroiden
 * väliin, joten kuutio ei vie leveyttä laudalta eikä korkeutta kiiloilta. Aiempi oma sarake
 * laudan vasemmalla söi leveyttä, ja sitä ennen keskitilan vasen laita söi noppien tilan.
 *
 * **Keskikohta on nyt omistamattoman kuution paikka eikä ei-väite.** Niin kauan kuin
 * omistajuutta ei luettu, keskellä oleminen oli ainoa paikka joka ei väittänyt mitään;
 * 9.8.2026 alkaen se väittää saman kuin sivu, eli ettei kuutio ole kummankaan puolella.
 * Omistettu kuutio on muurin päässä ([BarSegment]) 14.9.2026 alkaen, sitä ennen sarakkeen
 * päädyssä (`CubeSlot`, poistettu).
 *
 * Tänne osuu myös [CubePosition.UNKNOWN], eli solu jonka `VALIGN`ia ei tunnisteta. Se on
 * tietoinen valinta eikä laiskuus: tuntematon paikka ei saa piirtyä kummallekaan puolelle,
 * koska väärä puoli on pahempi kuin puolettomuus.
 *
 * **Teksti on ALT sellaisenaan eikä luku**, koska kuutio ei aina kanna lukua: `cubedr.gif`
 * sanoo `dr` eli double-repeat, ja lukuun pakotettuna se katosi ruudulta kokonaan.
 */
/**
 * Yksi rivi keskikaistan lokerossa: tilanneteksti, rastihuomautus tai jokin [stripNotes]in
 * riveistä. Sama asu kaikilla (pieni, huopatausta), koska ne ovat samaa lajia: yksi virke
 * siitä missä mennään. Vahtirivit pitävät oman värinsä ja lihavointinsa, koska ne ovat
 * varoituksia eivätkä tilannetta. [onTap] on huomautuksen kuittaus; muilla sitä ei ole.
 *
 * **Rivimäärää ei rajata** (14.9.2026): teksti rivittyy lokeron leveydellä ja pino kasvaa
 * kiilojen päälle. Ennen tätä kaksi riviä ja kolme pistettä, mikä sopi yhdelle virkkeelle
 * muttei pip-vahdin luvuille.
 */
@Composable
private fun StripNote(
    text: String,
    onTap: (() -> Unit)? = null,
    color: Color = Palette.TextMuted,
    bold: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.Bold else null,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.Felt)
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * Rastittoman kuutioteon huomautus laudan yläreunassa, samassa asussa kuin muut rivit.
 *
 * **Varapaikka 14.9.2026 alkaen.** Ensisijainen paikka on keskikaista ([verifyHintSlot],
 * [StripNote]), ja tämä piirtyy vain kun kaistalla ei ole vapaata lokeroa.
 *
 * Rastittamaton kuutioteko ei lähde eikä katoa tyhjään: ruutu sanoo mitä puuttuu. Sivusto
 * vastaisi samaan painallukseen punaisella rivillä `Previous move not verified!` (mitattu
 * 3.9.2026), ja tämä on sama tieto ennen pyyntöä eikä sen jälkeen.
 *
 * **Rivi eikä kortti 14.9.2026 alkaen** (Tommin tilaus: *"puuttuvasta checkboxista
 * huomauttava teksti laudan yläosassa pitäisi yhtenäistää muiden viestien kanssa"*). Tämä oli
 * viimeinen `Card`-muotoinen huomautus laudalla sen jälkeen kun peruutus (3.9.2026) ja arvattu
 * asema (1.9.2026) luopuivat kortista, ja se erottui nyt kaikista muista riveistä: eri
 * kirjasin, eri tausta ja oma `Dismiss`-nappi. Asu on sama kuin tilannetekstillä ja
 * peruutusrivillä (pieni, vaimea, huopatausta), koska se on samaa lajia: yksi virke siitä
 * missä mennään.
 *
 * Kuittausnappia ei ole. Rivi katoaa kun rasti laitetaan (rasti on vastaus huomautukseen),
 * kun lomake vaihtuu, tai kun riviä napautetaan.
 */
@Composable
private fun VerifyHintRow(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Palette.TextMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.Felt)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun CubeBadge(
    cube: Cube?,
    metrics: BoardMetrics,
    onDouble: (() -> Unit)? = null,
    attention: Boolean = false,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(metrics.bandHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (cube != null) CubeFace(cube, metrics, onDouble, attention)
    }
}

/**
 * Kuution ulkoasu, sama kaikissa kolmessa paikassa.
 *
 * **Kuutio on tuplausnappi silloin kun sivu tarjoaa tuplausta** (Tommin pyyntö 22.8.2026).
 * Ehtoa ei päätellä vuorosta eikä nopista vaan luetaan lomakkeesta: `Double` on
 * [BoardForm.submits]issa tasan silloin kun teko on mahdollinen, ja se on sama periaate kuin
 * nappirivillä eli se mitä sivu tarjoaa, on se mitä pelaaja voi tehdä. Kutsuja antaa
 * [onDouble]n vain siinä tapauksessa, joten tämä ei tunne ehtoa lainkaan.
 *
 * Klikkaus ei lähetä mitään vaan avaa vahvistuksen: teko on peruuttamaton, ja sivun oma
 * `verify`-ruutu on juuri se toinen ele jonka dialogi täällä korvaa.
 */
@Composable
private fun CubeFace(
    cube: Cube,
    metrics: BoardMetrics,
    onDouble: (() -> Unit)? = null,
    /** Kuutiomuistutus on voimassa tälle pelille, ks. [reminderAttention]. */
    attention: Boolean = false,
) = CubeFace(cube, metrics.cubeSize, onDouble, attention)

/**
 * Sama kuutio pelkällä sivun mitalla. Pelaajakortti ([PlayerPanelView]) ei tunne laudan
 * mittoja, ja kuution piirto lukee mitoista vain sivun.
 */
@Composable
private fun CubeFace(
    cube: Cube,
    size: Dp,
    onDouble: (() -> Unit)? = null,
    attention: Boolean = false,
) {
    val pulse = reminderAttention(attention)
    val ring = pulse.ring
    val scale = 1f + (CUBE_GROWTH - 1f) * pulse.grow
    // Ulkoasu lautatyylistä (22.9.2026), ks. [CubeLook]. Decossa viistetyt kulmat.
    val look = LocalCubeLook.current
    val shape = if (look.cutCorners) {
        CutCornerShape(size * 0.22f)
    } else {
        RoundedCornerShape(size * DgBoard.CORNER_PER_SIDE)
    }
    Box(
        modifier = Modifier
            // Muistutuksen kasvu, ks. [CUBE_GROWTH]. Ennen kokoa, jotta koko kuutio kehyksineen
            // skaalautuu eikä vain sisältö.
            .then(if (scale != 1f) Modifier.graphicsLayer { scaleX = scale; scaleY = scale } else Modifier)
            .size(size)
            // Pyöristys on suhde sivuun eikä kiinteä dp, ks. [DgBoard.CORNER_PER_SIDE].
            .clip(shape)
            .background(look.fill)
            .then(
                if (ring > 0f) {
                    Modifier.border(DgBoard.OUTLINE * (1f + 2f * ring), Palette.CubeLaser, shape)
                } else if (look.border != null) {
                    Modifier.border(DgBoard.OUTLINE * 1.5f, look.border, shape)
                } else {
                    Modifier
                },
            )
            .then(if (onDouble != null) Modifier.clickable(onClick = onDouble) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (cube.value == 1) {
            // **Ykköspinta on robotin pää eikä numero** (Tommin valinta 26.8.2026).
            // Fyysisessä kuutiossa ei ole ykköspintaa lainkaan, eli sivuston `ALT="1"` on
            // jo itsessään keinotekoinen merkintä tilalle "kukaan ei ole tuplannut", eikä
            // ykkönen kanna päätöstietoa niin kuin 2 tai `DR` kantavat. Siksi tämä ei riko
            // alla olevaa periaatetta "ALT sellaisenaan". Pää viittaa sovelluksen nimeen,
            // ks. [CubeRobotFace].
            //
            // Kuutiomuistutus näkyy tässä pinnassa lasersilminä, ks. [CubeRobotFace].
            CubeRobotFace(size, ring, pulse.heat, pulse.eyes, face = look.robot, hole = look.fill)
        } else {
            // **Numero kun se on luettavissa, DR kun kuva on DR, muuten ALT sellaisenaan.**
            // Tiedostonimestä luettu arvo on sama molemmilla ALT-kirjoitusasuilla (`2` ja
            // `cube2`), joten ruudulla lukee `2` kummassakin tapauksessa. Sama pätee DR:ään
            // 3.9.2026 alkaen: `cubedrs.gif` kantoi ALTia `cubedrs`, ja kuutiossa luki
            // laitteella `CUBEDRS`. Tuntematon kuva näyttää yhä ALTin: sitä ei arvata eikä
            // pudoteta.
            val text = cube.value?.toString() ?: if (cube.doubleRepeat) "DR" else cube.label.uppercase()

            // **Merkki mahtuu neliöön yhdellä rivillä, myös monimerkkisenä.** Laiteajossa
            // 10.8.2026 ruudulla luki `CUB` ja sen alla puolikas `E2`: teksti rivittyi ja
            // leikkautui. Neliön koko tulee nappulasta eikä tekstistä, joten teksti antaa periksi.
            // Arvokorjaus poisti tämän tapauksen, mutta ei kaikkia: `DRS` on kolme merkkiä.
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontSize = cubeFontSize(text.length, size),
                lineHeight = cubeFontSize(text.length, size),
                maxLines = 1,
                softWrap = false,
                fontWeight = if (look.bold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = look.fontFamily,
                // Väri arvon mukaan (Tommin päätös 21.9.2026), pinta pysyy yhtenä.
                color = look.onCube(cube.value),
            )
        }
    }
}

/**
 * Android-robotin pää kuution ykköspinnassa (Tommin valinta 26.8.2026).
 *
 * Pää symboloi sovelluksen nimeä DG Android, ja se on samalla se kuva jonka laite näytti
 * oletuskuvakkeena ennen kuin sovelluksella oli omaa. Pelkkä pää eikä koko hahmo, koska
 * pää pysyy luettavana pienimmässäkin koossa; vartalomuunnelmat hylättiin luonnoksissa.
 * Väri on [Palette.OnCube] eli sama kerma jolla kuution numerot piirretään, joten pinta
 * ei väitä olevansa eri esine kuin muut kuutiopinnat. Ei pilkkuja, joten ei sekoitu noppaan.
 *
 * **Kuutiomuistutus näkyy tässä pinnassa lasersilminä** (Tommin idea 3.9.2026). Silmät ovat
 * muuten reikiä kuoressa eli kuution pohjaväriä, ja muistutuksen alkaessa reikä syttyy
 * punaisena ([Palette.CubeLaserHot]), jäähtyy purppuraan ja häipyy sitten takaisin reiäksi.
 * Korostus on siis kuvassa eikä sen vieressä, ja se säästää tilan jonka teksti veisi laudan
 * keskikaistalta.
 *
 * **Silmät ovat huomion herätys, kehys on muistin paikka** (Tommin valinta 3.9.2026). Laser
 * kestää saman ajan kuin vilkkuminen ja katoaa sen mukana, ja voimassa oleva muistutus näkyy
 * sen jälkeen purppurana kehyksenä. Sama kehys näkyy myös numeropinnalla, jossa silmiä ei ole
 * lainkaan, joten muistutuksella on yksi lepotila eikä kahta.
 *
 * Robotti on Googlen työtä ja käytössä CC BY 3.0 -lisenssillä; nimeäminen on manuaalissa
 * (`help_robot_body`), koska se on ainoa ruutu jossa tekstille on tilaa.
 *
 * Mitat ovat 60-yksikön ruudukossa samasta luonnoksesta jonka Tommi hyväksyi, ja ne
 * skaalataan kuution sivuun. Silmät ovat kuution pohjaväriä eli reikiä, kuten esikuvassa.
 */
@Composable
private fun CubeRobotFace(
    size: Dp,
    /** Sykkeen taso 0..1, ks. [reminderAttention]. Ohjaa hehkun peittoa. */
    glow: Float = 0f,
    /** Silmän lämpö 1..0 eli punaisesta purppuraan, ks. [reminderAttention]. */
    heat: Float = 0f,
    /** Silmien näkyvyys 1..0, ks. [reminderAttention]. Nolla on tavallinen reikä kuoressa. */
    laserEyes: Float = 0f,
    /** Pään väri; Decossa kultaa, ks. [CubeLook]. */
    face: Color = Palette.OnCube,
    /** Silmäreiän väri eli kuution pinta. */
    hole: Color = Palette.Cube,
) {
    Canvas(modifier = Modifier.size(size)) {
        val u = this.size.minDimension / 60f

        // **Pää keskitetään pystysuunnassa piirroksen omien reunojen mukaan.** Luonnoksen
        // ruudukossa kuva ei istu keskellä: ylin piste on antennin kärki `14.5 - 1.1`
        // (puolikas viivanleveys, koska kärki on pyöristetty) ja alin on tyven `36.5`, joten
        // keskikohta on `24.95` kun ruudun keskikohta on `30`. Ero näkyi laitteella
        // pelisessiossa 3.9.2026 päänä joka istui liian ylhäällä. Siirto tehdään tässä eikä
        // korjaamalla jokaista lukua, jotta luonnoksen mitat pysyvät luettavina sellaisina
        // kuin ne hyväksyttiin 26.8.2026.
        val top = 14.5f - 1.1f
        val bottom = 36.5f
        translate(top = (30f - (top + bottom) / 2f) * u) {
            // Kupoli: puolikaari ja matala tyvi jonka alakulmat on pyöristetty.
            val dome = Path().apply {
                arcTo(Rect(16f * u, 20f * u, 44f * u, 48f * u), 180f, 180f, forceMoveTo = true)
                lineTo(44f * u, 35f * u)
                quadraticTo(44f * u, 36.5f * u, 42.5f * u, 36.5f * u)
                lineTo(17.5f * u, 36.5f * u)
                quadraticTo(16f * u, 36.5f * u, 16f * u, 35f * u)
                close()
            }
            drawPath(dome, face)

            val eyes = listOf(Offset(23.5f * u, 29f * u), Offset(36.5f * u, 29f * u))
            val laser = lerp(Palette.CubeLaser, Palette.CubeLaserHot, heat)
            if (laserEyes > 0f) {
                // Hehku piirretään kuoren päälle eikä alle: se on valo joka tulee reiästä,
                // eikä varjo joka olisi pään takana.
                val halo = laser.copy(alpha = (0.15f + 0.4f * glow) * laserEyes)
                eyes.forEach { drawCircle(halo, radius = 3.4f * u, center = it) }
            }
            // Reikä on lähtökohta ja laser häivytetään sen päälle, joten silmä palautuu
            // tavalliseksi ilman erillistä ehtoa kun näkyvyys on nollassa.
            val eye = lerp(hole, laser, laserEyes)
            eyes.forEach { drawCircle(eye, radius = 2.1f * u, center = it) }

            drawLine(
                face,
                start = Offset(23f * u, 22f * u),
                end = Offset(18.5f * u, 14.5f * u),
                strokeWidth = 2.2f * u,
                cap = StrokeCap.Round,
            )
            drawLine(
                face,
                start = Offset(37f * u, 22f * u),
                end = Offset(41.5f * u, 14.5f * u),
                strokeWidth = 2.2f * u,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun OffTray(
    board: BoardState,
    color: CheckerColor,
    paint: CheckerPaint,
    look: BoardLook,
    metrics: BoardMetrics,
    modifier: Modifier = Modifier,
    /** Kuutio on lokeron alapuolella, eli tämä on ylempi lokero. Luku menee kuutiota kohti. */
    cubeBelow: Boolean,
) {
    val count = board.borneOff.filter { it.color == color }.sumOf { it.count }

    // Tyhja lokero ei kerro mitaan, joten sita ei piirreta (Tommin valinta 22.8.2026
    // pelisession aikana). Ulos kannettuja on nollasta lahtien vasta pelin lopussa, eli
    // valtaosan ajasta ruudulla oli `OFF` ja sen alla `0` molemmilla pelaajilla.
    //
    // **Paikka jaa varatuksi, ja se on tarkoitus.** Lokero saa `weight(1f)`:n riippumatta
    // tasta, joten sarakkeen keskikohta ei liiku kun ensimmainen nappula kannetaan ulos,
    // vaan lokero ilmestyy paikkaan joka oli jo tyhjana.
    //
    // **Varaus tarvitsee tyhjän tilan, ei pelkkää paluuta** (korjattu 29.9.2026). Paluu ennen
    // ensimmäistä composablea jätti painon soveltamatta, joten kahden tyhjän lokeron välissä
    // kuutio nousi sarakkeen yläpäähän (`sessio-29-9-ilta3`, kuoriproxy, fransin lauta).
    if (count == 0) {
        Spacer(modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(look.tray)
            .border(DgBoard.OUTLINE, look.trayOutline, RoundedCornerShape(4.dp))
            // Sisävara pystysuunnassa (Tommin havainto tabletilla 5.10.2026: *"napit ovat
            // liian kiinni ylä- ja alareunoissa (osa kuvasta jää reunan alle)"*). Pino ja
            // kiekko alkoivat lokeron reunasta, joten uloin viiva piirtyi reunaviivan alle.
            // Viivan paksuus johdetaan jäljelle jäävästä korkeudesta, joten 15 mahtuu yhä.
            .padding(vertical = OFF_TRAY_INSET),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // **Ei Off-sanaa, luku kuutiota kohti ja viiden ryhmät** (Tommin valinta 2.10.2026,
        // Sage Pron lokerosta). Pino ja luku kertovat jo mikä lokero on, joten sana vei vain
        // tilaa pinolta. Luku on lokeron sillä reunalla joka on kuutiota kohti, ja pino kasvaa
        // laudan ulkoreunasta, joten ylälokeron pino kasvaa ylhäältä alas.
        // **Luku on pelaajan värisessä kiekossa** (Tommin valinta 2.10.2026: *"tabletilla
        // määrät voisi pompata enemmän taustasta, koska tilaa on"*). Kiekko on nappulan
        // kokoinen mutta korkeintaan lokeron levyinen, joten tabletilla se kasvaa ja
        // ahtaalla lokerolla se täyttää leveyden. Väri ja reuna ovat nappulan omat.
        //
        // **Puhelimessa kiekkoa ei piirretä.** Pixelin pystylaudalla kiekosta tuli 18 dp ja
        // luvusta lukukelvoton (kuorella 2.10.2026), joten alle [OFF_DISC_MIN]:n luku jää
        // paljaaksi 16 sp:n luvuksi kuten ennen. Tilaus koski tablettia, jolla tilaa on.
        val number = @Composable {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val disc = metrics.checkerSize.coerceAtMost(maxWidth)
                if (disc < OFF_DISC_MIN) {
                    Text(
                        text = count.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.TextPrimary,
                    )
                    return@BoxWithConstraints
                }
                val text = with(LocalDensity.current) { (disc * OFF_COUNT_TEXT).toSp() }
                Box(
                    modifier = Modifier
                        .size(disc)
                        .clip(CircleShape)
                        .background(paint.fill)
                        .checkerRim(paint, Palette.Outline, DgBoard.OUTLINE),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = text,
                        fontWeight = FontWeight.Bold,
                        color = paint.onFill,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        if (!cubeBelow) number()
        // **Syrjäpino** (Tommin havainto ja valinta D 24.9.2026: *"Off lukumäärä ei erotu
        // kunnolla"*). Jokainen poistettu on viiva syrjästä katsottuna, kuten laudan vieressä
        // pöydällä. Viivan paksuus johdetaan lokeron korkeudesta, jotta 15 mahtuu myös
        // puhelimen pystylaudalla.
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = if (cubeBelow) Alignment.TopCenter else Alignment.BottomCenter,
        ) {
            // Viiden ryhmien väliin jää yhden viivan rako silloin kun 15 viivaa ja kaksi rakoa
            // mahtuvat lokeroon ja viiva on vielä erotettavan paksu. Muuten pino on tasainen.
            // Viisi on [DgBoard.CHECKERS_APART], sama määrä jonka pelaaja lukee laskematta.
            val groups = (DgBoard.MAX_CHECKERS - 1) / DgBoard.CHECKERS_APART
            val grouped = (maxHeight / (DgBoard.MAX_CHECKERS + groups)).coerceAtMost(OFF_SLAB_MAX)
            val apart = grouped >= OFF_SLAB_GROUPED_MIN
            val step = if (apart) grouped else (maxHeight / DgBoard.MAX_CHECKERS).coerceIn(OFF_SLAB_MIN, OFF_SLAB_MAX)
            val groupGap = if (apart) step else 0.dp
            val ring = paint.ring ?: Palette.Outline
            Canvas(
                modifier = Modifier
                    .width(metrics.checkerSize)
                    .height(step * count + groupGap * ((count - 1) / DgBoard.CHECKERS_APART)),
            ) {
                val gap = 1.dp.toPx()
                val h = step.toPx() - gap
                val corner = CornerRadius(minOf(h / 2f, 2.dp.toPx()))
                repeat(count) { i ->
                    val fromEdge = i * step.toPx() + (i / DgBoard.CHECKERS_APART) * groupGap.toPx()
                    val top = if (cubeBelow) fromEdge else size.height - fromEdge - step.toPx() + gap
                    drawRoundRect(paint.fill, Offset(0f, top), Size(size.width, h), corner)
                    // Vaalean nappulan syrjä saa inlayn tumman linjan, koska 1 px kultaa ei
                    // erotu kermasta (24.9.2026).
                    drawRoundRect(paint.inlay ?: ring, Offset(0f, top), Size(size.width, h), corner, style = Stroke(1f))
                }
            }
        }
        if (cubeBelow) number()
    }
}

/**
 * Nappulan reuna: pelkkä rengas, tai [CheckerPaint.inlay]n kanssa kulta kahden tumman
 * hiuslinjan välissä (valinta D 24.9.2026). Rengas pysyy [width]n levyisenä molemmissa, joten
 * nappulan koko ja luku eivät muutu; inlay vie vain hiuslinjojen verran sisältä.
 */
internal fun Modifier.checkerRim(paint: CheckerPaint, fallback: Color, width: Dp): Modifier {
    val inlay = paint.inlay ?: return border(width, paint.ring ?: fallback, CircleShape)
    val ring = paint.ring ?: fallback
    return drawWithContent {
        drawContent()
        val hair = CHECKER_INLAY_HAIR.toPx()
        val w = width.toPx()
        val r = size.minDimension / 2f
        drawCircle(inlay, r - hair / 2f, style = Stroke(hair))
        drawCircle(ring, r - hair - w / 2f, style = Stroke(w))
        drawCircle(inlay, r - hair - w - hair / 2f, style = Stroke(hair))
    }
}

/** Inlayn hiuslinja; puhelimen pienimmälläkin nappulalla erillinen viiva. */
internal val CHECKER_INLAY_HAIR = 0.8.dp

/** Off-pinon viiva ja väli yhteensä: ohuimmillaan vielä erillisiä, paksuimmillaan ei kasva pinoksi. */
private val OFF_SLAB_MIN = 2.dp
private val OFF_SLAB_MAX = 6.dp

/** Ohuin viiva jolla viiden ryhmät piirretään: ohuemmalla rako ei erotu viivojen välistä. */
private val OFF_SLAB_GROUPED_MIN = 3.dp

/** Off-kiekon luvun korkeus kiekon halkaisijasta: kaksinumeroinen mahtuu renkaan sisään. */
private const val OFF_COUNT_TEXT = 0.42f

/** Pienin Off-kiekko: tätä pienemmässä luku ei olisi 16 sp:n paljasta lukua isompi. */
private val OFF_DISC_MIN = 28.dp

/** Off-lokeron pystysuuntainen sisävara, jotta pino ja kiekko eivät ala reunaviivan alta. */
private val OFF_TRAY_INSET = 4.dp

/**
 * Nappulapino kiilassa tai muurin segmentissä.
 *
 * **Näkyvien määrä johdetaan kiilan korkeudesta eikä ole vakio.** Viisi mahtuu erilleen kun
 * tilaa on, ja siitä eteenpäin ne limittyvät tiheimmilleen (`MIN_STACK_STEP`), eli ruudulla
 * näkyy niin monta nappulaa kuin sinne oikeasti mahtuu. Vakio näkyvien määrä olisi kertonut
 * vähemmän kuin tila sallii korkealla kiilalla ja liikaa matalalla.
 *
 * **Luku kirjoitetaan päällimmäiseen kahdesta eri syystä, ja siksi ehto on `minOf`.**
 * Limittynyttä pinoa ei voi lukea silmäyksellä, ja mahtumaton pino kertoisi ilman lukua
 * väärän määrän. Edellinen raja on [DgBoard.CHECKERS_APART] eli viisi, joka on jo nimetty
 * se määrä jonka pelaaja lukee laskematta; jälkimmäinen on [visible], joka riippuu kiilan
 * korkeudesta. Kumpi tahansa yksinään jättäisi toisen tapauksen numerottomaksi.
 *
 * Sivusto kirjoittaa saman luvun ja käytännössä samasta syystä, mutta sen kiila on kiinteä
 * GIF-kuva ja tämä skaalautuu. **Sivuston oma kynnys on siksi mittaamaton:** numero on
 * leivottu kuvan sisään, joten sivun tavut kertovat vain määrän (`ALT="y6"`) eivätkä sitä
 * näkyykö kuvassa numero. Tämän ruudun kynnys on siis johdettu omasta perustelusta eikä
 * kopioitu sivustolta, ja se on tarkoituksellista: arvattu kynnys näyttäisi kopiolta.
 *
 * [growUp] kääntää suunnan: pino alkaa aina kiilan kannasta (numeron puolelta) ja kasvaa
 * kärkeä (muuria) kohti. Siirtymä lasketaan itse eikä `Arrangement.spacedBy`llä, koska
 * limitys on negatiivinen väli. Päällimmäinen piirretään viimeisenä, joten sen sisällä oleva
 * luku ei jää muiden alle.
 */
@Composable
private fun CheckerStack(
    paint: CheckerPaint,
    count: Int,
    metrics: BoardMetrics,
    growUp: Boolean,
) {
    if (count <= 0) return
    val size = metrics.checkerSize
    val available = metrics.wedgeHeight
    val minStep = size * DgBoard.MIN_STACK_STEP_RATIO
    val drawable = (1 + ((available - size) / minStep).toInt())
        .coerceIn(1, DgBoard.MAX_CHECKERS)
    val visible = minOf(count, drawable)
    val step = if (visible <= 1) size else minOf(size, (available - size) / (visible - 1))

    Box(modifier = Modifier.width(size).height(size + step * (visible - 1))) {
        (0 until visible).forEach { index ->
            val topmost = index == visible - 1
            Box(
                modifier = Modifier
                    .align(if (growUp) Alignment.BottomCenter else Alignment.TopCenter)
                    .offset(y = if (growUp) -(step * index) else step * index)
                    .size(size)
                    .clip(CircleShape)
                    .background(paint.fill)
                    .checkerRim(paint, Palette.Outline, DgBoard.OUTLINE),
                contentAlignment = Alignment.Center,
            ) {
                if (topmost && count > minOf(visible, DgBoard.CHECKERS_APART)) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = paint.onFill,
                    )
                }
            }
        }
    }
}

/**
 * Pip-vahdin tulos, näkyvissä aina, **keskitilassa eikä omalla rivillään**.
 *
 * Ristiriita on varoitus eikä lokirivi: se on sovelluksen ainoa automaattinen tunnistin
 * hiljaa väärään menneelle jäsennykselle, ja debug-lipun taakse piilotettuna se hukkaisi
 * juuri sen mitä varten se on olemassa. Paikka vaihtui 8.8.2026 laudan alta keskitilaan,
 * koska oma rivi maksoi korkeutta jonka kiilat tarvitsevat, ja keskitilassa on vaakatilaa
 * jota mikään muu ei käytä. Näkyvyys ei siis heikentynyt vaan parani: teksti on nyt
 * keskellä lautaa eikä sen alla.
 *
 * **Sanamuoto lyheni samalla, ja se maksoi jotain.** Aiempi ristiriitateksti sanoi ääneen
 * että näytetty asema voi olla väärä. Nyt sen kantaa `MISMATCH` ja punainen väri, ja
 * paneelin luvut ovat mukana, koska juuri ne kertovat kumpi lähde on eri mieltä.
 */
@Composable
private fun PipCheckLine(pips: PipCheck) {
    val (text, color) = when (pips) {
        // **Täsmäävä tarkistus ei sano mitään ruudulla** (Tommin pyyntö 9.8.2026: "on
        // toiminut moitteettomasti"). Rivi on vahti eikä tulos: sen tehtävä on kertoa
        // milloin nappuloiden väreihin ei voi luottaa, ja `Agrees` tarkoittaa ettei
        // kerrottavaa ole. Luvut itse ovat yhä numerorivillä pelaajittain, joten mitään
        // ei katoa ruudulta: tässä oli niiden summa, siellä on niiden lähde.
        //
        // Kaksi muuta tapausta jäävät näkyviin, ja se on koko ehdon syy. `Disagrees` on
        // ainoa merkki siitä että roolipäättely saattoi mennä väärin, ja `Unavailable`
        // erottaa tekemättömän tarkistuksen epäonnistuneesta.
        //
        // Tämä ei vapauta korkeutta: keskitila on `MID_BAND` eli kiinteä, ja rivi on sen
        // sisällä noppien välissä. Muutos on siis luettavuutta eikä nappulan kokoa.
        is PipCheck.Agrees -> return

        is PipCheck.Disagrees -> stringResource(
            R.string.board_pip_check_failed,
            pips.yellow,
            pips.blue,
            pips.panel.joinToString(" / "),
        ) to Palette.PipConflict

        // Loadediin päästään vain kun pisteitä on, joten tämä tarkoittaa käytännössä
        // asetusta Hide pip counts. Todetaan se, koska tarkistuksen puuttuminen ei ole
        // sama asia kuin tarkistuksen epäonnistuminen.
        PipCheck.Unavailable -> stringResource(R.string.board_pip_check_unavailable) to
            LocalPanelLook.current.muted
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
    )
}

/**
 * Nappulavahdin rivi, ja se noudattaa pip-vahdin ehtoa: **täsmäävä tarkistus ei sano mitään**.
 *
 * Vahti on siis hiljainen aina paitsi silloin kun värien nappulamäärät eroavat, eli kun
 * ainakin toisen luenta on vajaa. `Unavailable` ei myöskään sano mitään, koska se tarkoittaa
 * Mini-skeemaa, jonka pip-rivi kertoo jo omalla tavallaan; kaksi riviä samasta syystä olisi
 * kohinaa.
 *
 * Tämä on eri ehto kuin pip-rivillä, jossa `Unavailable` näkyy. Siellä se erottaa
 * käyttäjäasetuksen (`Hide pip counts`) epäonnistuneesta tarkistuksesta, ja se ero on
 * olemassa vain pipeillä.
 */
@Composable
private fun CheckerCheckLine(checkers: CheckerCheck) {
    val mismatch = checkers as? CheckerCheck.Disagrees ?: return
    Text(
        text = stringResource(
            R.string.board_checker_check_failed,
            mismatch.yellow,
            mismatch.blue,
        ),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Palette.PipConflict,
        maxLines = 1,
    )
}

/** Numeroimattoman pisteen numero piirretään vaimeana: tuntematon ei ole tyhjä. */
private const val UNKNOWN_ALPHA = 0.38f

/*
 * Tässä oli `SLOT_GAP = 16.dp`, väli laudan ja sen viereisen sarakkeen välissä. Poistettu
 * 9.8.2026 yhdessä sivupaneelin kanssa: laudan vieressä ei ole enää mitään, joten väliä ei
 * ole olemassa eikä sitä ole vähennettävä laudan leveydestä.
 */

/**
 * Kuinka pitkä veto alaspäin hakee sivun uudelleen. 72 dp on tarkoituksella pitkä: kohde on
 * vapaaehtoisvoimin pyöritetty sivusto, joten vahinkohaku maksaa enemmän kuin toinen veto.
 */
private val PULL_THRESHOLD = 72.dp

/**
 * Sivupaneelin mitat. **Alaraja on ehto eikä leveys:** paneeli ilmestyy vain jos laudan
 * jälkeen jää vähintään tämän verran, joten kapealla laitteella sitä ei ole eikä lauta
 * kutistu. Yläraja estää sen leviämisen tabletilla koko ylijäämän mittaiseksi.
 */
/**
 * Lomakkeen lähetysnapin nimi tuplaukselle, sivun omalla kirjoitusasulla.
 *
 * Sanaa ei käännetä eikä yhtenäistetä, koska sillä sekä tunnistetaan sivun tarjoama teko että
 * lähetetään se takaisin. Ks. [MiddleActionsRow].
 */
private const val DOUBLE_SUBMIT = "Double"

/** `Accept` sivun omana sanana, ks. [DOUBLE_SUBMIT]. */
private const val ACCEPT_SUBMIT = "Accept"

/**
 * Money gamen beaver-napit sivun omina sanoina (mitattu 27.9.2026, `sessio-27-9-money-peli`
 * `0079` ja `0092`). `Beaver!` on tuplauksen saajan kolmas nappi `Accept`in ja `Decline`n
 * rinnalla, `Accept Beaver` tuplaajan vastaus siihen. Kumpikin on kuutioteko jota ei voi perua.
 */
internal const val BEAVER_SUBMIT = "Beaver!"
internal const val ACCEPT_BEAVER_SUBMIT = "Accept Beaver"

/** Kuution käsittelyn napit, joiden painallus kolahtaa Wood-teemassa ([WoodSound.CUBE]). */
internal val WOOD_CUBE_SUBMITS = setOf(DOUBLE_SUBMIT, ACCEPT_SUBMIT, BEAVER_SUBMIT, ACCEPT_BEAVER_SUBMIT)

/** Napit joita laitteen `Confirm Beaver` koskee. */
internal val BEAVER_SUBMITS = setOf(BEAVER_SUBMIT, ACCEPT_BEAVER_SUBMIT)

/**
 * Sivun vahvistusruudun tila ruudun tasolla.
 *
 * Ruutu oli 3.9.2026 asti nappirivin ja paneelipinon oma tila, ja kuution klikkaus ei
 * nähnyt sitä lainkaan. Kun rasti on pakollinen ([cubeActionFor]), kaikkien kolmen reitin
 * on luettava sama ruutu, joten tila asuu kutsujalla ja kulkee tässä yhtenä arvona.
 */
internal class VerifyBox(
    /** Piirrettävät ruudut järjestyksessä, ks. [verifyBoxesFor]. */
    val boxes: List<String>,
    /** Rastitetut ruudut. */
    val checked: Set<String>,
    val onChange: (box: String, checked: Boolean) -> Unit,
) {
    /**
     * Ruutu joka vahvistaa napin [label], tai null kun sillä ei ole ruutua.
     *
     * `Decline` vahvistuu vain omalla ruudullaan (25.9.2026). Sivun ainoa ruutu
     * (`Verify Accept`) vahvisti sen aiemmin sivutuotteena, koska sivu lähettää ruudun
     * napista riippumatta; nyt kummankin rasti koskee vain omaa tekoaan.
     */
    fun boxFor(label: String): String? = when (label) {
        DECLINE_SUBMIT -> label.takeIf { it in boxes }
        // Sivu ei piirrä beaverille omaa ruutua (`0079`: vain `Verify Accept` ja `Verify
        // Decline`), eikä `Accept`in rasti saa vahvistaa eri tekoa (27.9.2026).
        BEAVER_SUBMIT -> null
        else -> boxes.firstOrNull { it != DECLINE_SUBMIT }
    }

    fun isChecked(label: String): Boolean = boxFor(label)?.let { it in checked } == true

    companion object {
        val NONE = VerifyBox(emptyList(), emptySet()) { _, _ -> }
    }
}

/**
 * Laudalle piirrettävät vahvistusruudut: täsmälleen ne jotka sivu piirtää.
 *
 * **Sivu on ainoa lähde** (`SUBSTANSSI.md` kohta 102, korjaus 26.9.2026). 25.9.2026
 * `Verify Decline` johdettiin asetussivulta, koska sivun omaa ruutua luultiin puuttuvaksi.
 * Se piirtyi tabletilla kahdesti, kun sivun ainoa ruutu oli `Decline`, ja se jäi piirtymättä
 * käyttäjältä jonka asetuksia ei ollut luettu.
 */
internal fun verifyBoxesFor(form: BoardForm?): List<String> =
    listOfNotNull(
        form?.verify,
        DECLINE_SUBMIT.takeIf { form?.verifyDecline == true && it in form.submits },
    )

/** Mitä kuutioteon painallukselle tehdään, ks. [cubeActionFor]. */
internal enum class CubeAction {
    /** Lähetetään sellaisenaan: teko ei ole kuutioteko tai sivu ei pyydä vahvistusta. */
    SEND,
    /** Ei lähetetä eikä kysytä: sivun oma rasti puuttuu, ja ruutu sanoo sen. */
    NEEDS_VERIFY,
    /** Rasti on tehty: dialogi kysyy vielä kerran ennen lähetystä. */
    ASK,
}

/**
 * Kuutioteon (`Double`, `Accept`) reitti painalluksesta lähetykseen.
 *
 * **Rasti on pakollinen ja dialogi tulee sen jälkeen** (Tommin päätös 3.9.2026: *"rasti
 * pitää aina olla ja dialogi ok sen jälkeen, tuplaus on hidas tapahtuma, näin välttyy
 * vahingoilta"*). Rastittamaton painallus ei lähde eikä avaa dialogia vaan näyttää
 * huomautuksen; rastitettu painallus avaa dialogin, ja vasta sen kuittaus lähettää.
 *
 * Sääntö on yhtenä funktiona, koska samaan tekoon on kolme reittiä: nappirivi, paneelipino
 * ja kuution klikkaus. Se korvaa 31.8.2026 tehdyn version, jossa rastittamaton `Double`
 * avasi dialogin ja lähetti rastitettuna eli ohitti ruudun. Se jätti `Accept`in ennalleen,
 * ja 3.9.2026 illan sessiossa `Accept` lähti ilman rastia: sivusto vastasi samalla laudalla
 * ja rivillä `Previous move not verified!` (fixture `move_accept_not_verified.html`).
 *
 * **Kun sivu ei pyydä vahvistusta, nappi lähettää suoraan** (Tommin päätös 26.9.2026,
 * `SUBSTANSSI.md` kohta 102). Pelaaja on ottanut asetuksen pois, ja dialogi vahvisti silloin
 * vastoin hänen valintaansa (`sessio-26-9-ilta2`: `Double?` yhdelle vastustajalle, `Accept?` toiselle ja
 * ackammonille). 3.9.–26.9.2026 teko meni tässä tilassa dialogin kautta.
 *
 * **Laitteen kytkin lisää dialogin** ([deviceConfirms], Tommin tilaus 27.9.2026,
 * `BeaverConfirmStore`). Se koskee vain `Beaver!`ia ja `Accept Beaver`ia, joille sivulla ei ole
 * omaa vahvistusasetusta, ja se kysyy myös ilman rastia. Rastia se ei keksi.
 *
 * **Laudan kuutio kysyy aina** ([fromCube], Tommin valinta samana päivänä). Kuution
 * napautus on sovelluksen oma oikotie, jota sivulla ei ole, ja se ohittaa nappirivin.
 * Vahinkonapautus laudalla on helpompi kuin napin painallus, joten ilman sivun ruutua
 * dialogi jää sen ainoaksi toiseksi eleeksi.
 *
 * **`Decline` vaatii rastin vain kun pelaajan asetus sitä pyytää** (25.9.2026). Asetus pois
 * se meni ilman rastia läpi kuudesti 3.9.2026. Asetus päällä sivu hylkäsi sen kolmesti, ja
 * ruutu on silloin sovelluksen piirtämä ([verifyBoxesFor]). Rastitettu `Decline` lähtee ilman
 * dialogia: dialogi on Tommin valinta tuplaukselle ja hyväksynnälle, eikä sitä laajenneta
 * kysymättä.
 */
/**
 * Mitä noppien painallus tekee, tai null kun se ei tee mitään.
 *
 * **Tommin tilaus 4.9.2026**, kaksi kytkintä: täydellä vuorolla painallus lähettää siirron
 * ja koskemattomilla nopilla se vaihtaa järjestyksen. Kytkimet ovat erikseen
 * (`DiceSubmitStore`, `DiceSwapStore`) ja molemmat oletuksena pois.
 *
 * **Ehto luetaan kokoamisesta eikä noppien väristä**, ja se on Tommin kuittaus samana
 * päivänä. Harmaus on kokoamisen ominaisuus, joten ilman kokoamista tämä on aina null.
 * Lähetyksen ehto on [CompositionSession.isComplete]: pakotetussa vuorossa toinen noppa jää
 * harmaantumatta, ja juuri silloin sivusto ei salli enempää, joten kirjaimellinen "kaikki
 * harmaita" olisi jättänyt tavallisimman lyhyen vuoron ulos.
 *
 * Vaihdon ehto on [CompositionSession.canSwap], joka on sivun oma sääntö: vaihto on
 * mahdollinen vain ennen ensimmäistä poimintaa, eikä tuplaa voi vaihtaa.
 *
 * Järjestys on merkitsevä vain teoriassa. Täysi vuoro ilman yhtään askelta on mahdollinen
 * vain jos laillinen vuoro on tyhjä, ja silloin lähetys on oikea teko eikä vaihto.
 *
 * Sääntö on yhtenä funktiona samasta syystä kuin [cubeActionFor]: piirtokohtia on kaksi
 * (omat nopat kummallakin puoliskolla), eikä ehtoa saa kirjoittaa kahdesti.
 */
internal fun diceTapFor(
    composition: CompositionSession?,
    submitEnabled: Boolean,
    swapEnabled: Boolean,
): DiceTap? = when {
    composition == null -> null
    submitEnabled && composition.isComplete -> DiceTap.SUBMIT
    swapEnabled && composition.canSwap -> DiceTap.SWAP
    else -> null
}

/**
 * Heittääkö vastustajan noppien painallus (testaajan toive 9.10.2026: *Option to "Roll
 * Dice" by tapping on opponents dice?*). Ehto on sivun oma `Roll Dice` -nappi eikä
 * pääteltyä vuoroa, joten painallus tekee täsmälleen sen minkä nappi tekisi ja vain kun
 * nappi on tarjolla. Kytkin on oletuksena pois kuten muilla noppakytkimillä.
 */
internal fun diceRollTapFor(submits: Collection<String>?, enabled: Boolean): Boolean =
    enabled && submits?.contains(ROLL_DICE_SUBMIT) == true

/** `Roll Dice` sivun omana sanana, ks. [DOUBLE_SUBMIT]. */
internal const val ROLL_DICE_SUBMIT = "Roll Dice"

/** Noppien painalluksen teot. Ks. [diceTapFor]. */
internal enum class DiceTap {
    /** Täysi vuoro lähtee sivustolle, sama teko kuin `Submit Move` -nappi. */
    SUBMIT,

    /** Noppien järjestys vaihtuu paikallisesti, sama teko kuin `Swap Dice` -komento. */
    SWAP,
}

/**
 * Vahvistusdialogin runko: mitä teko tekee.
 *
 * **`Accept` on laudalla aina kuution hyväksyntä, myös peruutetulla sivulla** (korjattu
 * 8.9.2026). Tämä funktio syntyi 5.9.2026 vastakkaiselle luennalle: sivulla oli
 * peruutusrivi, `Accept`, `Decline` ja rasti, kuutio ykkösessä eikä noppia, ja se luettiin
 * peruutuspyynnöksi jolle kuutioteksti olisi väärin. Sivu luettiin 8.9.2026 uudelleen
 * kaappauksesta (`sessio-5-9-ilta2/0015`), ja siinä lukee *eemelihk has doubled.* Sama
 * yhdistelmä toistui samana iltana (`sessio-8-9-ilta/0104`, *Teppo 77 has doubled.*), ja
 * Tommi hyväksyi sen: vastaus oli `cube2.gif` omistettuna, eli kuutio siirtyi. Tarjous
 * peruutetulla sivulla on siis tavallinen tarjous, jonka edellä sivu kertoo peruutuksesta.
 * Peruutus itsessään ei kysy mitään, se on jo tapahtunut kun sivu latautuu.
 *
 * Peruutusteksti ehti olla ruudulla 5.9.–8.9.2026, ja se lupasi väärän teon. Kaappaus
 * `nauhat/dg-8-9-ilta-accept.mp4` näyttää sen viimeisen kerran.
 *
 * Oma funktio eikä ehto piirtokohdassa, jotta valinta on testattavissa ilman Composea.
 */
@StringRes
internal fun cubeDialogBody(label: String): Int = when (label) {
    ACCEPT_SUBMIT -> R.string.board_accept_text
    BEAVER_SUBMIT -> R.string.board_beaver_text
    ACCEPT_BEAVER_SUBMIT -> R.string.board_accept_beaver_text
    else -> R.string.board_double_text
}

/** Dialogin otsikko, sama jako kuin [cubeDialogBody]. */
internal fun cubeDialogTitle(label: String): Int = when (label) {
    ACCEPT_SUBMIT -> R.string.board_accept_title
    BEAVER_SUBMIT -> R.string.board_beaver_title
    ACCEPT_BEAVER_SUBMIT -> R.string.board_accept_beaver_title
    else -> R.string.board_double_title
}

/**
 * Kuutio sellaisena kuin se piirretään: tarjottu kuutio näyttää **tarjotun arvon**.
 *
 * **Tommin tilaus 8.9.2026 kesken pelisession** (YrjoSuomelain tarjous, kuutiossa robotin
 * pää): *"kun tarjotaan tuplauskuutiota, niin sen arvon tulisi päivittyä seuraavaan eli nyt
 * pitäisi androidin pään sijaan olla 2"*. Sivu ei käännä kuutiota ennen hyväksyntää: kuva
 * on tarjouksen aikana yhä `cube1.gif` ja ALT `1` (mitattu 17 sivusta 3.9.2026 ja tänä
 * iltana `sessio-8-9-ilta/0100`). Fyysisessä pelissä tarjoaja kääntää kuution ja työntää
 * sen vastustajalle, eli tarjouksen aikana kuutiossa **lukee** tarjottu arvo. Sivun `1`
 * on tässä kohtaa sama keinotekoinen merkintä joka jo perusteli robotin pään
 * ([CubeRobotFace]), joten tämä ei riko periaatetta "ALT sellaisenaan" enempää kuin se.
 *
 * Tarjous luetaan lomakkeen `Accept`-napista, kuten sijaintikin ([MiddleStrip]), ja
 * peruutettu sivu ei muuta sitä: Teppo 77:n tarjous 8.9.2026 tuli peruutusrivin kanssa
 * (`sessio-8-9-ilta/0104`), ks. [cubeDialogBody]. Kaksi rajaa. DR-kuutio pysyy DR:nä,
 * koska se ei ole arvo vaan haarautus (`SUBSTANSSI.md` kohta 17). Tuntematon arvo (ALT ei
 * luku) jää sellaisenaan: sitä ei arvata kahdella kerrottavaksi.
 *
 * Oma funktio eikä ehto piirtokohdassa, jotta sääntö on testattavissa ilman Composea.
 */
internal fun shownCube(board: BoardState): Cube? {
    val cube = board.cube ?: return null
    val offered = board.form?.submits?.contains(ACCEPT_SUBMIT) == true
    val value = cube.value
    if (!offered || cube.doubleRepeat || value == null) return cube
    val doubled = value * 2
    return cube.copy(label = doubled.toString(), value = doubled)
}

/**
 * Piirtyykö kuutio keskikaistalle (tosi) vai lokerosarakkeeseen (epätosi).
 *
 * Kolme paikkaa (Tommin päätös 3.9.2026, ks. `docs/UI.md`): omistettu sarakkeen päässä,
 * omistamaton muurilla ja tarjottu oman puoliskon keskellä. Kaista on käytössä vain kun
 * napit ovat paneelissa; napit kaistalla vievät kaikki kolme sarakkeeseen.
 *
 * **Tarjous voittaa omistuksen** (Tommin havainto pelisessiossa 14.9.2026: *"kun vastustaja teki
 * redouble -> 4 niin tarjotun kuulan pitäisi olla keskikaistalla eikä hänen
 * omistuksessaan"*). Uudelleentuplauksessa sivu näyttää kuution yhä omistajan solussa
 * (`VALIGN=top`) ja lomake tarjoaa `Accept`ia, joten omistus ja tarjous ovat molemmat
 * tosia. Siihen asti omistus luettiin ensin ja kuutio jäi vastustajan lokeron päähän,
 * vaikka fyysisesti se on työnnetty vastaajalle. Sama sääntö kuin ensitarjouksessa:
 * tarjottu kuutio on siellä mihin vastaus annetaan.
 *
 * Oma funktio eikä ehto piirtokohdassa, jotta sääntö on testattavissa ilman Composea.
 */
internal fun cubeOnStrip(
    stripHasActions: Boolean,
    cubeOwned: Boolean,
    cubeOffered: Boolean,
): Boolean = !stripHasActions && (!cubeOwned || cubeOffered)

/** Keskikaistan puolisko, ks. [situationSlot]. */
internal enum class StripSlot { OPPONENT, SELF }

/** Yksi keskikaistan pinon rivi: teksti ja vahtirivin oma asu, ks. [stripNotes]. */
internal data class StripNoteSpec(
    val text: String,
    val color: Color = Palette.TextMuted,
    val bold: Boolean = false,
)

/**
 * Sivun tilarivit keskikaistan pinoon, järjestyksessä: peruutus, tilanneteksti, pip-vahti,
 * nappulavahti ja toistojono (Tommin päätös 14.9.2026: *"ehdotuksen mukaan"*, eli rivit 1,
 * 2, 3, 4 ja 7 inventaariosta; muistutukset ja tuntemattomat ilmoitukset jäivät laudan alle).
 *
 * Ehdot ovat samat kuin paneelin riveillä olivat: täsmäävä vahti ei sano mitään
 * ([PipCheckLine]), nappulavahti vain erimielisenä ([CheckerCheckLine]), toistojono vain
 * kun sivulla on rivi. Lista on tyhjä täsmälleen silloin kun paneelin pohjakin olisi tyhjä,
 * ja silloin lokeroa ei varata ([situationSlot]).
 */
@Composable
internal fun stripNotes(
    board: BoardState,
    pips: PipCheck,
    checkers: CheckerCheck,
    unconfirmed: Failure? = null,
    /** Vastustajan siirtolista on matkalla, ks. `BoardUiState.Loaded.readingOpponent`. */
    readingOpponent: Boolean = false,
): List<StripNoteSpec> =
    buildList {
        // Katkotilan syy ensin (Tommin valinta 26.9.2026, `sessio-26-9-ilta2`): dialogi
        // poistui, ja syy kulkee samaan lokeroon kuin sivun omat viestit.
        unconfirmed?.let {
            add(StripNoteSpec(stringResource(R.string.board_unconfirmed_note, it.chipText()), Palette.TextPrimary, bold = true))
        }
        if (board.rolledBack) add(StripNoteSpec(stringResource(R.string.board_rolled_back)))
        board.situation?.let { add(StripNoteSpec(it)) }
        when (pips) {
            is PipCheck.Agrees -> Unit
            is PipCheck.Disagrees -> add(
                StripNoteSpec(
                    stringResource(
                        R.string.board_pip_check_failed,
                        pips.yellow,
                        pips.blue,
                        pips.panel.joinToString(" / "),
                    ),
                    color = Palette.PipConflict,
                    bold = true,
                ),
            )
            PipCheck.Unavailable -> add(
                StripNoteSpec(stringResource(R.string.board_pip_check_unavailable), bold = true),
            )
        }
        (checkers as? CheckerCheck.Disagrees)?.let {
            add(
                StripNoteSpec(
                    stringResource(R.string.board_checker_check_failed, it.yellow, it.blue),
                    color = Palette.PipConflict,
                    bold = true,
                ),
            )
        }
        board.pendingReplays?.let { add(StripNoteSpec(stringResource(R.string.board_pending_replays, it))) }
        // Viimeisenä, koska se on sovelluksen oma odotus eikä sivun sana. Se katoaa noin
        // sekunnissa (Tommin valinta A 29.9.2026 illalla).
        if (readingOpponent) add(StripNoteSpec(stringResource(R.string.board_reading_opponent)))
    }

/**
 * Kummalle keskikaistan puoliskolle sivun tilanneteksti menee, tai null kun se jää
 * sivupaneeliin.
 *
 * **Tommin päätös 8.9.2026**: *YrjoSuomela has doubled.* kuuluu laudan keskikaistalle,
 * sinne missä kuutio ja nopat näkyvät, eikä sivupaneelin pohjalle. Puolisko on vapaa kun
 * siinä ei ole noppia eikä tarjottua kuutiota (tarjottu kuutio on omalla puoliskolla,
 * ks. [MiddleStrip]). Vastustajan puoli ensin, koska teksti kertoo yleensä hänen
 * teostaan. Kun napit ovat kaistalla (ei sivupaneelia), kaista on täynnä ja teksti jää
 * paneeliin, joka silloin tosin puuttuu: puhelimella teksti on siis ennallaan siellä
 * missä se oli, eli tämä ei muuta puhelimen ruutua.
 *
 * Oma funktio eikä ehto piirtokohdassa, jotta sääntö on testattavissa ilman Composea.
 */
internal fun situationSlot(
    hasNotes: Boolean,
    opponentHasDice: Boolean,
    selfHasDice: Boolean,
    cubeOffered: Boolean,
    stripHasActions: Boolean,
): StripSlot? = when {
    !hasNotes || stripHasActions -> null
    !opponentHasDice -> StripSlot.OPPONENT
    !selfHasDice && !cubeOffered -> StripSlot.SELF
    else -> null
}

/**
 * Kummalle keskikaistan puoliskolle rastihuomautus menee, tai null kun se jää laudan
 * yläreunaan ([VerifyHintRow]).
 *
 * **Tommin päätös 14.9.2026** (vaihtoehto A neljästä kuvasta): tilannetekstin alle samaan
 * lokeroon kun teksti on kaistalla, muuten samalla säännöllä kuin tilanneteksti eli
 * vastustajan puoli kun siinä ei ole noppia ja oma puoli kun siinä ei ole noppia eikä
 * tarjottua kuutiota. [situationSlot] on tilannetekstin lokero tai null kun tekstiä ei ole
 * kaistalla. Oma funktio, jotta sääntö on testattavissa ilman Composea.
 */
internal fun verifyHintSlot(
    situationSlot: StripSlot?,
    opponentHasDice: Boolean,
    selfHasDice: Boolean,
    cubeOffered: Boolean,
    stripHasActions: Boolean,
): StripSlot? = situationSlot ?: situationSlot(
    hasNotes = true,
    opponentHasDice = opponentHasDice,
    selfHasDice = selfHasDice,
    cubeOffered = cubeOffered,
    stripHasActions = stripHasActions,
)

internal fun cubeActionFor(
    label: String,
    siteAsksVerify: Boolean,
    verified: Boolean,
    fromCube: Boolean = false,
    deviceConfirms: Boolean = false,
): CubeAction =
    when {
        label !in CUBE_SUBMITS -> CubeAction.SEND
        siteAsksVerify && !verified -> CubeAction.NEEDS_VERIFY
        // Kieltäytyminen ei kysy dialogilla: se vaatii rastin vain kun ruutu on piirretty.
        label == DECLINE_SUBMIT -> CubeAction.SEND
        siteAsksVerify || fromCube || deviceConfirms -> CubeAction.ASK
        else -> CubeAction.SEND
    }

/** Kuutioteot joita [cubeActionFor] reitittää; muut napit lähtevät sellaisenaan. */
private val CUBE_SUBMITS = setOf(DOUBLE_SUBMIT, ACCEPT_SUBMIT, DECLINE_SUBMIT, BEAVER_SUBMIT, ACCEPT_BEAVER_SUBMIT)

/**
 * Teot joita ei voi perua: kuution tarjoaminen ja siihen vastaaminen.
 *
 * **Nimet ovat sivun omia sanoja**, kuten nappien tekstitkin ([MiddleActionsRow]). Lista on
 * siis sidottu sivustoon eika kaannettavissa, ja tuntematon sana putoaa turvalliselle
 * puolelle: se piirtyy tavallisena tekona. Se on oikea suunta vaarin mennessa vain siksi,
 * etta vahvistusruutu tulee sivulta erikseen eika tasta listasta.
 *
 * Peruttavat teot (`Roll Dice`, `Submit Move`) eivat ole taalla: heiton voi tehda ja siirron
 * koota uudelleen, ja `Undo Move` purkaa kokoamisen.
 */
// Beaver-napit lisätty 27.9.2026 Tommin sanalla (*"kehystä"*): money gamessa `Beaver!` piirtyi
// mustana omalle rivilleen, vaikka se on kuutioteko jota ei voi perua.
private val IRREVERSIBLE_SUBMITS = setOf(DOUBLE_SUBMIT, "Accept", "Decline", BEAVER_SUBMIT, ACCEPT_BEAVER_SUBMIT)

/**
 * Linkin alleviivauksen paksuus. Fontin oma alleviivaus oli hiusviiva ja mustaa taustaa
 * vasten vaikea nähdä (Tommin havainto 27.8.2026).
 */


/**
 * Kuutioteon vahvistus, eli kolmas ele rastin ja napin jälkeen ([cubeActionFor]).
 *
 * Dialogi oli 31.8.–3.9.2026 rastin korvike, nyt se on rastin lisä: tuplaus on hidas
 * tapahtuma, ja kolme elettä on Tommin valitsema hinta vahingosta. Teksti sanoo mitä
 * tapahtuu ja että sitä ei voi perua. Peruuttamattomuus on sivuston ominaisuus eikä
 * sovelluksen: lähetetty tuplaus on vastustajan vuorossa, hyväksytty kuutio on omalla
 * puolella kaksinkertaisena.
 */
@Composable
private fun CubeDialog(
    label: String,
    rolledBack: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(cubeDialogTitle(label)),
        body = stringResource(cubeDialogBody(label)),
        // Napin sana on sivun oma sana, sama jolla teko lähtee.
        confirmLabel = label,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Noppien oman alueen leveys sarakkeina keskitilassa, kummallakin puolella.
 *
 * **Tämä oli kuusi 14.8.-22.8.2026, ja se jätti napit kokonaan piiloon.** Kaksi
 * kuuden sarakkeen aluetta vie kaksitoista saraketta, ja koko pelialue on
 * [DgBoard.PLAY_COLUMNS] eli kolmetoista. Napeille jäi siis **yksi sarake**, tabletilla noin
 * 75 dp, kun rivi tarvitsee mitattuna noin 270 dp. Vika ei näkynyt aiemmin, koska rivin
 * ensimmäisestä alkiosta näkyy aina kaistale: kun se oli `Submit Move` -nappi, napin saattoi
 * painaa, ja kun se oli vahvistusruutu, näkyviin jäi juuri se alkio jolla ei yksin tee mitään.
 *
 * **Neljä eikä pienempi, koska nopan on osoitettava puoliskoonsa.** Keskitettynä neljän
 * sarakkeen alueeseen nopat ovat toisen sarakkeen kohdalla, eli yhä selvästi oman puoliskonsa
 * päällä. Se on 10.8.2026 tehdyn päätöksen ehto, ja molemmat sen hylkäämät ääripäät pysyvät
 * hylättyinä: laitaan asti (näytön lova osui nopan päälle) ja keskelle nippuun (nopat eivät
 * osoittaneet kummankaan puoliskoon).
 *
 * **Neljä eikä suurempi, koska napeille ei varata enempää kuin ne tarvitsevat** (Tommin
 * tarkennus 22.8.2026). Tämä jättää napeille viisi saraketta eli noin 375 dp, kun ensin
 * visualisoitu kolmen sarakkeen alue olisi antanut seitsemän saraketta eli noin
 * kaksinkertaisesti tarpeeseen nähden.
 */
private const val DICE_GROUP_POINTS = 4

/** Puoliskon kiilat; noppalokeron leveys kun kaistalla ei ole nappeja, ks. [MiddleStrip]. */
private const val HALF_POINTS = 6

/**
 * Kuution merkin kirjasinkoko, johdettuna merkkimäärästä ja neliön koosta.
 *
 * Jakaja on merkkimäärä plus puoli, koska leveys on kirjaimen leveys eikä korkeus: `labelSmall`
 * on lihavoituna noin 0,6 kertaa korkeutensa levyinen, ja puolikas merkki jää reunoiksi.
 * Yläraja pitää yhden merkin luettavana kasvattamatta sitä neliön mittaiseksi.
 */
private fun cubeFontSize(chars: Int, cubeSize: Dp) =
    (cubeSize.value / (chars.coerceAtLeast(1) + 0.5f) * 1.4f).coerceAtMost(cubeSize.value * 0.6f).sp


private val SIDE_PANEL_MIN = 150.dp
private val SIDE_PANEL_MAX = 240.dp
private val SIDE_PANEL_GAP = 8.dp

/**
 * Numerorivin pystypehmuste. Nimetty vakioksi koska se esiintyy kahdessa paikassa: rivillä
 * itsellään ja [numberRowHeight]issä joka ennakoi rivin korkeuden. Kaksi kirjoitettua lukua
 * eriytyisi ennen pitkää, ja silloin laudan leveys laskettaisiin väärästä korkeudesta.
 */
private val NUMBER_ROW_PADDING = 2.dp

/**
 * Pip-luvun koko suhteessa pistenumeroihin (`BarPips` (poistettu 14.9.2026, pipit [PlayerPanelView]issä), Tommin pyyntö 16.8.2026).
 *
 * **Yläraja tulee riviboksista eikä mausta.** `numberRowHeight` johtaa laudan leveyden
 * `labelSmall`in rivikorkeudesta, joten pip-luku saa kasvaa vain sen verran kuin samaan
 * boksiin mahtuu (11sp fontti 16sp rivillä). Suurempi kerroin vaatii että `numberRowHeight`
 * laskee maksimin kahdesta tyylistä, eikä sitä tehdä ennen kuin sille on syy.
 */

/**
 * Asetusruudun esikatselut (Tommin tilaus 23.9.2026: *"preview per vaihtoehto"*, tarkennus
 * *"mielellään pelitilanteista"*). Tässä tiedostossa eikä `SettingsPreviews.kt`:ssä, koska
 * lauta, nopat ja pistekenttä ovat tämän tiedoston yksityisiä, ja esikatselu piirtää **samat
 * komponentit eikä kuvia niistä**: kuva vanhenisi hiljaa seuraavassa laudan muutoksessa.
 * Kosketus on tyhjä ja toimintorivi poissa, joten lauta on pelkkä katsottava kuva.
 *
 * **Kuutio on omistamaton robotti lokerosarakkeen keskellä** (Tommin tilaus 6.10.2026:
 * *"tuplattu tuplauskuutio ei pitäisi olla näkyvissä vaan sivupaneelissa ja tuplaamaton
 * androidin pää keskikaistan sivulokerossa"*). Tilanteen kuutio on 2:ssa, ja ilman paneelia
 * peli piirtää sen muurin päähän; pelissä paneeli on yleensä ruudulla ja omistettu kuutio
 * siellä. Pelkän laudan kuva näyttää siksi kuution lepopaikassaan, ks. [previewUnownedCube].
 */
@Composable
internal fun PreviewBoard(
    board: BoardState,
    look: BoardLook,
    cube: CubeLook,
    height: Dp,
    /** Kortissa 2, suurennuksessa 1: isossa näkymässä numerot mahtuvat omassa koossaan. */
    scaleUp: Float = PREVIEW_SCALE_UP,
) {
    val board = previewUnownedCube(board)
    val roles = BoardRoles(selfCheckerColor(board, board.reconcilePips()), look)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        // **Piirretään kaksinkertaisena ja skaalataan puoleen** (laiteajo 23.9.2026): kortin
        // kokoisena numerorivit rivittyivät kahdelle riville ja veivät laudalta korkeuden,
        // koska numeron koko on fontista eikä laudasta. Skaalattuna kuva on sama lauta kuin
        // pelissä, vain pienempänä.
        val w = maxWidth * scaleUp
        val h = maxHeight * scaleUp
        val frameWidth = DgBoard.frameWidth(
            availableWidth = w,
            availableHeight = h,
            numberRowHeight = numberRowHeight(),
        )
        CompositionLocalProvider(LocalCubeLook provides cube) {
            Board(
                board,
                roles = roles,
                pips = board.reconcilePips(),
                awayOf = board::awayOf,
                touch = BoardTouch(movable = emptyMap(), onFollow = {}),
                onFollow = {},
                onPress = { _, _ -> },
                modifier = Modifier
                    .requiredSize(frameWidth, h)
                    .graphicsLayer {
                        scaleX = 1f / scaleUp
                        scaleY = 1f / scaleUp
                    },
                middleShowSkip = false,
                middleShowSubmits = false,
                middleShowActions = false,
            )
        }
    }
}

/** Sama tilanne kuution ollessa omistamaton: ykköspinta (robotti) keskisolussa, ks. [shownCube]. */
internal fun previewUnownedCube(board: BoardState): BoardState =
    board.copy(cube = Cube(label = "1", value = 1, position = CubePosition.MIDDLE))

private const val PREVIEW_SCALE_UP = 2f
private const val PANEL_PREVIEW_SCALE_UP = 3f

/**
 * Lauta ja sivupaneeli rinnakkain kätisyyden korttiin (Tommin tarkennus 24.9.2026: *"samanlaisia
 * kuvia laudoista kun ylempänä on, ei tuollaisia yksinkertaistuksia"*). Sama pelitilanne ja
 * sama kaksinkertainen piirto kuin [PreviewBoard]issa, ja paneeli on pelin oma [SidePanel]
 * pinoineen, joten kuva näyttää paneelin sillä puolella jolle se pelissä menee.
 *
 * Tuplattu kuutio on omistajan pelaajakortissa eikä muurin päässä (6.10.2026), sama sääntö
 * kuin pelissä paneelin ollessa ruudulla ([cubeOnStrip], `panelCube`).
 */
@Composable
internal fun PreviewBoardWithPanel(
    board: BoardState,
    look: BoardLook,
    cube: CubeLook,
    panel: PanelLook,
    height: Dp,
    panelOnRight: Boolean,
    // Kolminkertainen eikä kaksinkertainen (kaappaus 24.9.2026): kaksinkertaisena paneelin
    // kortit ja pino eivät mahtuneet laudan korkeuteen, koska tekstien koko on fontista.
    scaleUp: Float = PANEL_PREVIEW_SCALE_UP,
) {
    val pips = board.reconcilePips()
    val roles = BoardRoles(selfCheckerColor(board, pips), look)
    val panelCube = shownCube(board)?.takeIf { it.position == CubePosition.TOP || it.position == CubePosition.BOTTOM }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        val w = maxWidth * scaleUp
        val h = maxHeight * scaleUp
        val panelWidth = (w * 0.3f).coerceAtMost(SIDE_PANEL_MAX)
        val frameWidth = DgBoard.frameWidth(
            availableWidth = w - panelWidth - SIDE_PANEL_GAP,
            availableHeight = h,
            numberRowHeight = numberRowHeight(),
        )
        val sidePanel: @Composable () -> Unit = {
            SidePanel(
                board = board,
                compact = false,
                roundLabel = null,
                roles = roles,
                pips = pips,
                checkers = board.reconcileCheckers(),
                awayOf = board::awayOf,
                onOpenPage = {},
                panelCube = panelCube,
                modifier = Modifier.width(panelWidth).fillMaxHeight(),
                actionsPresent = true,
                formActions = { PanelActionStack(board, onFollow = {}, onPress = { _, _ -> }) },
                showNotes = false,
            )
        }
        CompositionLocalProvider(LocalCubeLook provides cube) {
            Row(
                modifier = Modifier
                    .requiredSize(frameWidth + SIDE_PANEL_GAP + panelWidth, h)
                    .graphicsLayer {
                        scaleX = 1f / scaleUp
                        scaleY = 1f / scaleUp
                    }
                    .background(panel.background),
            ) {
                if (!panelOnRight) {
                    sidePanel()
                    Spacer(modifier = Modifier.width(SIDE_PANEL_GAP))
                }
                Board(
                    board,
                    roles = roles,
                    pips = pips,
                    awayOf = board::awayOf,
                    touch = BoardTouch(movable = emptyMap(), onFollow = {}),
                    onFollow = {},
                    onPress = { _, _ -> },
                    modifier = Modifier.width(frameWidth).fillMaxHeight(),
                    ownedCubeInPanel = panelCube != null,
                    middleShowSkip = false,
                    middleShowSubmits = false,
                    middleShowActions = false,
                )
                if (panelOnRight) {
                    Spacer(modifier = Modifier.width(SIDE_PANEL_GAP))
                    sidePanel()
                }
            }
        }
    }
}

/** Esikatselun nopat laudan huovalla, samalla [DiceGroup]illa kuin laudalla. */
@Composable
internal fun PreviewDice(board: BoardState, look: BoardLook, size: Dp) {
    val roles = BoardRoles(selfCheckerColor(board, board.reconcilePips()), look)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(look.felt)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        DiceGroup(board.dice, roles, BoardMetrics(size, size, size, size, size, size))
    }
}

/** Esikatselun pelaajakortin pari, sama [cardScoreText] kuin pelin kortissa (6.10.2026). */
@Composable
internal fun previewCardScoreText(board: BoardState, panel: PlayerPanel, awayShown: Boolean): String? {
    val other = board.players.firstOrNull { it !== panel }
    fun away(p: PlayerPanel?) = if (awayShown && p != null) board.awayOf(p) else null
    return cardScoreText(panel, away(panel), other, away(other), board.matchLength)
}

/** Pelaajan nappulan maali esikatselulle, sama roolijako kuin laudalla. */
internal fun previewPaint(board: BoardState, look: BoardLook, panel: PlayerPanel): CheckerPaint =
    BoardRoles(selfCheckerColor(board, board.reconcilePips()), look).paint(panel.checkerColor)

/** Odotuksen ilmaisin tietyssä muodossa, ohi [LocalBusyStyle]n. Deco-viimeistely seuraa kytkintä. */
@Composable
internal fun PreviewBusy(style: BusyStyle, board: BoardState, look: BoardLook, modifier: Modifier = Modifier) {
    val roles = BoardRoles(selfCheckerColor(board, board.reconcilePips()), look)
    BusyShape(modifier, style, arcLookOf(roles), Palette.Outline)
}

/**
 * Päättymissivun *Reply to <pelaaja>* -kentän tila ja teot yhtenä nippuna (27.9.2026).
 *
 * Malli on DGText2Area-laajennus: sivusto ei tarjoa tätä nappia, vaan laajennus lisää sen
 * päättymissivulle (`docs/AVOIMET.md`). Lähetys on pikaviesti, ja lomake luetaan
 * vastaanottajan profiilista (`BoardViewModel.sendMatchReply`).
 */
data class MatchReplyUi(
    val send: ReplyUiState,
    val draft: String,
    /** Kirjautuneen tunnus, jonka riville nappia ei tule. Null kun sitä ei tiedetä. */
    val selfName: String?,
    val onOpen: () -> Unit,
    val onDraftChange: (String) -> Unit,
    val onSend: (PlayerRef) -> Unit,
)
