package fi.tommi.dg.app.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.app.session.DiceStyle
import fi.tommi.dg.app.session.ScoreStyle
import fi.tommi.dg.app.ui.DgBoard.Palette
import fi.tommi.dg.domain.BoardForm
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.SiteBoardSettings
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.CompositionSession
import fi.tommi.dg.domain.CrawfordReading
import fi.tommi.dg.domain.Cube
import fi.tommi.dg.domain.CubePosition
import fi.tommi.dg.domain.Die
import fi.tommi.dg.domain.PendingAction
import fi.tommi.dg.domain.dependsOnAssembly
import fi.tommi.dg.domain.CheckerCheck
import fi.tommi.dg.domain.PipCheck
import fi.tommi.dg.domain.PlayerPanel
import fi.tommi.dg.domain.MarkedPosition
import fi.tommi.dg.domain.Reminder
import fi.tommi.dg.domain.gameKey
import fi.tommi.dg.domain.Point
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
     * Tämän pelin merkityt asemat ja niiden kirjoitus, ks. [MarkStrip]. Eri lista kuin
     * [reminders], koska elinkaari on eri: merkki elää ottelun yli.
     */
    marks: List<MarkedPosition> = emptyList(),
    onMarkPosition: (String) -> Unit = {},
    onRemoveMark: (Long) -> Unit = {},
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
    /**
     * Tosi kun pelaaja siirtää vasemmalla kädellä, jolloin sivupaneeli on oikealla
     * (Tommin päätös 9.9.2026). Oletus on oikeakätinen, eli sama paneelin puoli kuin ennen.
     */
    leftHanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
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
        // ei palaa. Pystyssä ei piiloteta, koska pysty on tällä ruudulla ohimenevä.
        val landscape = maxWidth > maxHeight
        val hideBars = remember(landscape) {
            landscape && !DgBoard.barsAreFree(
                windowHeight = maxHeight,
                barsHeight = barsHeight,
                barsWidth = barsWidth,
            )
        }
        HideSystemBarsWhileVisible(enabled = hideBars)

        // Ikonit vaaleiksi ehdoitta, koska laudan tausta on musta kaikilla laitteilla. Tämä
        // ei jaa piilotuksen ehtoa: näkyvä palkki tarvitsee tämän, ja piilotettu palkki
        // tarvitsee sen silloin kun se pyyhkäistään hetkeksi esiin.
        DarkSystemBarIconsWhileVisible()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.PanelBg)
                // Vain palkit, ei näyttölovea (Tommin päätös 16.9.2026, `docs/UI.md` › Lovi
                // ja sivupalkki). Lovi on puhelimen vaakatilassa paneelin reunalla ja sen
                // inset vei viidenneksen paneelista; reikä itse osuu ottelukortin
                // reunaviivaan. Kun palkit on piilotettu, tämä on nolla ja lauta saa koko
                // ikkunan. Lupa piirtää loven alle on `MainActivity`ssä.
                .windowInsetsPadding(WindowInsets.systemBars),
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
                        is BoardUiState.Loaded -> {
                            LoadedBoard(
                                state,
                                style = style,
                                siteSettings = siteSettings,
                                diceStyle = diceStyle,
                                scoreStyle = scoreStyle,
                                diceSubmitTap = diceSubmitTap,
                                diceSwapTap = diceSwapTap,
                                leftHanded = leftHanded,
                                onRefresh = onRefresh,
                                onFollow = onFollow,
                                onPress = onPress,
                                onOpenPage = onOpenPage,
                                onSendChat = onSendChat,
                                phrases = phrases,
                                onAddPhrase = onAddPhrase,
                                onRemovePhrase = onRemovePhrase,
                                reminders = reminders,
                                onAddReminder = onAddReminder,
                                onRemoveReminder = onRemoveReminder,
                                marks = marks,
                                onMarkPosition = onMarkPosition,
                                onRemoveMark = onRemoveMark,
                            )
                            UnconfirmedDialog(state.unconfirmed)
                        }

                        BoardUiState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }

                        is BoardUiState.MatchOver -> MatchOverContent(
                            state = state,
                            onFollow = onFollow,
                            onPress = onPress,
                            onOpenPage = onOpenPage,
                            onSendChat = onSendChat,
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
                        // negheonegun pikaviestillä pelisessiossa 13.9. ilta2.
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
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
 * Ilmoitus siitä että teko epäonnistui ja alla oleva lauta on vahvistamaton (24.8.2026,
 * Tommin valinta: pelaajalle kerrotaan tilanne, palvelinta ei kuormiteta pollauksella).
 *
 * **Dialogi eikä pysyvä merkintä**, koska pysyvä signaali on jo olemassa: katkennut lähetys
 * näkyy [PendingBar]issa siihen asti että käyttäjä ratkaisee sen, ja lauta ei ota vastaan
 * tekoja ennen uutta hakua. Tämä kertoo kerran mitä tapahtui ja mitä lauta nyt on.
 *
 * Sulkeminen on paikallinen eikä näkymämallin tila: [BoardUiState.Loaded.unconfirmed] jää
 * voimaan (se ohjaa laudan lukitusta ja verkon paluun hakua), mutta samaa ilmoitusta ei
 * näytetä uudelleen ennen kuin lauta on välillä saatu sivustolta ([remember]in avain).
 */
@Composable
private fun UnconfirmedDialog(reason: Failure?) {
    if (reason == null) return
    var dismissed by remember(reason) { mutableStateOf(false) }
    if (dismissed) return

    AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text(stringResource(R.string.board_unconfirmed_title)) },
        text = {
            Text(
                when (reason) {
                    Failure.Offline -> stringResource(R.string.board_unconfirmed_offline)
                    is Failure.Server ->
                        stringResource(R.string.board_unconfirmed_server, reason.code)
                    Failure.Sleeping -> stringResource(R.string.board_unconfirmed_sleeping)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { dismissed = true }) {
                Text(stringResource(R.string.board_unconfirmed_ok))
            }
        },
    )
}

/**
 * Katkotilan `Refresh` valmiina tekona ja sen syy, tai null kun lauta on vahvistettu.
 *
 * Yksi arvo eikä kaksi rinnakkaista parametria, koska nappi ja sen viereinen teksti ovat
 * yksi asia: kumpikaan ei ole mielekäs ilman toista, ja kahdella parametrilla kutsuja voisi
 * antaa syyn ilman nappia. Kulkee laudalle asti, joten lauta ei lue tilamallia itse.
 */
private data class UnconfirmedRefreshSpec(val reason: Failure, val onRefresh: () -> Unit)

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
 * **Napin vieressä lukee mistä on kyse** (Tommin tilaus 8.9.2026, *"keskipaneeliin sopisi
 * viereen tieto huonosta yhteydestä"*). Saman asian kertoo [UnconfirmedDialog], mutta se
 * suljetaan ja katoaa; tämä jää näkyviin niin kauan kuin lauta on vahvistamaton, eli
 * täsmälleen niin kauan kuin tieto pätee. Teksti on lyhyt ja napin vieressä eikä sen sijasta,
 * koska kaista on kapea ja reitti eteenpäin on yhä se nappi.
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
 * **Nappi keskittyy kuution kohdalle, teksti asettuu sen viereen** (Tommin havainto
 * 14.9.2026: *"Refresh-napin pitäisi peittää tuplauskuutio kokonaan"*). Rivinä keskitettynä
 * napin ja tekstin pari oli keskellä, jolloin nappi itse jäi keskikohdan vasemmalle puolelle
 * ja kuutio pilkotti sen alta. Nyt napin keskipiste on kaistan keskipiste ja teksti sijoitetaan
 * napin oikealle puolelle siirtämättä nappia. Napin vähimmäiskoko on kuutio reunuksineen
 * ([UNCONFIRMED_COVER_MARGIN]), koska pelkkä tekstin mitta ei riittänyt peittämään kuutiota
 * korkeussuunnassa tabletin nappulakoolla.
 */
@Composable
private fun UnconfirmedRefresh(reason: Failure, metrics: BoardMetrics, onRefresh: () -> Unit) {
    val cover = metrics.cubeSize + UNCONFIRMED_COVER_MARGIN * 2
    val gap = 12.dp
    Layout(
        content = {
            Button(
                onClick = onRefresh,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                modifier = Modifier.sizeIn(minWidth = cover, minHeight = cover),
            ) {
                Text(
                    text = stringResource(R.string.top_refresh),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                text = when (reason) {
                    Failure.Offline -> stringResource(R.string.board_unconfirmed_chip_offline)
                    is Failure.Server ->
                        stringResource(R.string.board_unconfirmed_chip_server, reason.code)
                    Failure.Sleeping -> stringResource(R.string.board_unconfirmed_chip_sleeping)
                },
                style = MaterialTheme.typography.titleSmall,
                color = Palette.TextPrimary,
            )
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val button = measurables[0].measure(loose)
        val gapPx = gap.roundToPx()
        // Teksti saa vain napin oikealle puolelle jäävän tilan, jotta se ei työnnä nappia
        // eikä valu kaistan ulkopuolelle.
        val textRoom = ((constraints.maxWidth - button.width) / 2 - gapPx).coerceAtLeast(0)
        val text = measurables[1].measure(loose.copy(maxWidth = textRoom))
        val height = maxOf(button.height, text.height)
        layout(constraints.maxWidth, height) {
            val buttonX = (constraints.maxWidth - button.width) / 2
            button.place(buttonX, (height - button.height) / 2)
            text.place(buttonX + button.width + gapPx, (height - text.height) / 2)
        }
    }
}

/** Kuinka paljon katkotilan nappi ulottuu kuution reunan yli joka suuntaan. */
private val UNCONFIRMED_COVER_MARGIN = 6.dp

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
    phrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
) {
    val page = state.page
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        page.eventName?.let { name ->
            val heading = listOfNotNull(name, page.roundLabel).joinToString(", ")
            // Osoite on ollut jäsennettynä alusta asti muttei käytössä. Turnaussivulle on
            // lukija, joten otsikko on linkki silloin kun sivu antaa osoitteen ja pelkkää
            // tekstiä silloin kun ei anna.
            val path = page.eventPath
            if (path == null) {
                Text(text = heading, style = MaterialTheme.typography.titleSmall, color = Palette.TextSecondary)
            } else {
                TextButton(
                    onClick = { onOpenPage(path) },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(text = heading, style = MaterialTheme.typography.titleSmall, color = Palette.Accent)
                }
            }
        }
        Text(
            text = page.resultText,
            style = MaterialTheme.typography.titleLarge,
            color = Palette.TextPrimary,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        if (page.predicted) {
            Text(
                text = stringResource(R.string.match_over_predicted),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        page.matchLength?.let { length ->
            Text(
                text = stringResource(R.string.match_over_length, length),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
        }
        page.scores.forEach { row ->
            Text(
                text = listOfNotNull(row.player.name, row.score?.toString()).joinToString("  "),
                style = MaterialTheme.typography.bodyLarge,
                color = Palette.TextPrimary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        state.chat?.let { chat ->
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

/**
 * Sivun napin näyttöteksti. Ehto ja sen perustelu ovat [SubmitLabels]issa; tässä on vain se
 * puoli joka tarvitsee resurssit, eli paikan nimi välilehden omasta tekstistä.
 */
@Composable
private fun submitText(label: String): String =
    if (SubmitLabels.isToTopPage(label)) {
        stringResource(R.string.board_submit_to_place, stringResource(R.string.tab_matches))
    } else {
        label
    }

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
    marks: List<MarkedPosition>,
    onMarkPosition: (String) -> Unit,
    onRemoveMark: (Long) -> Unit,
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
        ScoreStyle.SITE -> { _ -> null }
    }

    // Kosketus syntyy laudan omista linkeistä, ja se on tyhjä silloin kun sivu ei tarjoa
    // yhtään siirtoa. Silloin lauta on täsmälleen sama kuin ennen porttia: katsottava kuva.
    val touch = BoardTouch(
        movable = board.moves.associate { it.fromPoint to it.href },
        onFollow = onFollow,
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
    // 15.9.2026 päivällä, egis-ottelu: laserkehys ja `Pick me` vastustajan kortissa vaikka
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
            .background(Palette.PanelBg)
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
        // **Tila eikä paikallinen muuttuja** (mitattu 14.9.2026 klo 23.14, Rasp: `Pending
        // Replay: 1` sekä kaistalla että laudan alla). `BoxWithConstraints` koostaa
        // sisältönsä vasta mittauksessa, eli sen jälkeen kun tämän sarakkeen loput on jo
        // koostettu, joten paikallinen muuttuja luettiin aina epätotena. Tilan kirjoitus
        // koostaa lukijan uudelleen, ja rivi katoaa laudan alta samassa kehyksessä.
        var notesOnStrip by remember { mutableStateOf(false) }
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
            val look = BoardLook.of(style, siteSettings, state.board.scheme)
            val roles = BoardRoles(selfCheckerColor(state.board, state.pips), look)

            // **Paneeli saa vain ylijäävän leveyden, ei varattua.** Lauta mitoitetaan ensin,
            // ja paneeli ilmestyy vain jos sen jälkeen jää tarpeeksi. Kiinteä varaus oli syy
            // sen poistoon 9.8.2026: se oli Pixel 8a:lla ilmainen mutta kutisti pienellä
            // puhelimella nappulan 24,1:stä 18,8 dp:hen. Nyt sama koodi tekee kummallakin
            // oikein ilman laitetuntemusta, koska ehto on mitattu leveys eikä laite.
            val spare = maxWidth - frameWidth - SIDE_PANEL_GAP
            val panelWidth = spare.coerceAtMost(SIDE_PANEL_MAX)

            // **Päätös ei saa muuttaa sitä mistä se luetaan.** Tämä kirjoitettiin 10.8.2026
            // asti `SideEffect`illä ylös, ja laudan alle ilmestynyt toimintorivi vei laudalta
            // korkeutta. Lauta kutistuu korkeudesta, joten kapeampi lauta jätti taas tilaa
            // paneelille, ja päätös kumosi itsensä joka ruudulla: 294 värähtelevää ruutua
            // viidessä sekunnissa tyhjäkäynnillä, kaikki hitaita UI-säikeessä (Galaxy Tab S7+
            // 10.8.2026). Laukaisija oli odottavan teon palkki, joka siirsi asettelun tämän
            // rajan tuntumaan, mutta sama olisi tullut mistä tahansa laudan yläpuolisesta
            // palkista. Arvo pysyy nyt siinä mittauksessa jossa se syntyy.
            val fits = panelWidth >= SIDE_PANEL_MIN

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
            val compactPanel = maxHeight < DgBoard.PANEL_COMPACT_BELOW
            val reminderActions: @Composable () -> Unit = {
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
                if (chat != null && !chatCardVisible) {
                    OutlinedButton(
                        onClick = { chatComposer = true },
                        // Sama korkeus kuin `Skip Game`lla ahtaassa paneelissa (Tommin
                        // kaappaus 16.9.2026: *"message ja skip game napeille sama korkeus"*).
                        modifier = if (compactPanel) Modifier.height(DgBoard.COMPACT_BUTTON_HEIGHT) else Modifier,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Palette.TextSecondary,
                        ),
                        border = BorderStroke(DgBoard.OUTLINE, Palette.TextSecondary),
                    ) {
                        Text(
                            text = stringResource(R.string.board_chat_open),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Normal,
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
                // **Se on sivun komennoista viimeinen tässäkin joukossa**, eli `Message`n
                // jälkeen: sama järjestys kuin toimintorivillä, ja samasta syystä. Skip on
                // ainoa näistä jonka jälkeen ruudulla on eri peli.
                board.skipHref?.let { href -> SkipGameAction(href, onFollow, compact = compactPanel) }

                if (gameKnown) {
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
                        showMark = board.moveNumber != null,
                        compact = compactPanel,
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
            var verified by remember(board.form?.action) { mutableStateOf(false) }
            var asked by remember(board.form?.action) { mutableStateOf<String?>(null) }
            var verifyHint by remember(board.form?.action) { mutableStateOf<String?>(null) }
            val siteAsksVerify = board.form?.verify != null
            val verifyBox = VerifyBox(verified) { checked ->
                verified = checked
                // Rasti on vastaus huomautukseen, joten huomautus lähtee sen mukana.
                if (checked) verifyHint = null
            }
            val pressChecked: (String, Boolean) -> Unit = { label, isVerified ->
                when (cubeActionFor(label, siteAsksVerify, isVerified)) {
                    CubeAction.SEND -> onPress(label, isVerified)
                    CubeAction.NEEDS_VERIFY -> verifyHint = label
                    CubeAction.ASK -> asked = label
                }
            }

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

            // **Sivun tilarivit keskikaistalle kun siellä on vapaa puolisko** (tilanneteksti
            // 8.9.2026; peruutus, vahdit ja toistojono Tommin päätös 14.9.2026: *"haluaisin
            // yhtenäistää niitä keskelle lautaa jos tilaa on"*). Rivit ovat ne jotka kertovat
            // tämän sivun tilasta; muistutukset ovat pelaajan omia ja jäävät laudan alle,
            // tuntemattomat ilmoitukset samoin koska niiden pituutta ei tunneta. Kun
            // lokeroa ei ole, rivit ovat paneelin pohjalla ja laudan alla kuten ennen.
            val stripNotes = stripNotes(board, state.pips, state.checkers)
            val situationSlot = situationSlot(
                hasNotes = stripNotes.isNotEmpty(),
                opponentHasDice = board.dice.any { it.owner == roles.opponentColor },
                selfHasDice = board.dice.any { it.owner == roles.selfColor },
                cubeOffered = board.form?.submits?.contains(ACCEPT_SUBMIT) == true,
                stripHasActions = !fits,
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
                    stripHasActions = !fits,
                )
            }
            val verifyHintText = verifyHint?.let { label ->
                stringResource(R.string.board_verify_first, board.form?.verify ?: label)
            }
            Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
            ) {
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
                        ({ pressChecked(DOUBLE_SUBMIT, verifyBox.checked) })
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
                        modifier = Modifier.width(panelWidth).fillMaxHeight(),
                        actions = reminderActions,
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
                                arc = ArcLook.of(roles),
                                compact = compactPanel,
                            )
                        },
                        showNotes = situationSlot == null,
                    )
                }
                if (fits && !panelOnRight) {
                    panel()
                    Spacer(modifier = Modifier.width(SIDE_PANEL_GAP))
                }
                Board(
                    board,
                    roles = roles,
                    pips = state.pips,
                    awayOf = awayOf,
                    touch = touch,
                    onFollow = onFollow,
                    onPress = pressChecked,
                    onDiceTap = onDiceTap,
                    verify = verifyBox,
                    modifier = Modifier.width(frameWidth).fillMaxHeight(),
                    // **Sama ehto ja sama mitta kuin ennen napeilla** (Tommin valinta
                    // 14.8.2026, `TRAY_TEXT_MIN` uudelleennimetty samalla). Toinen kynnys
                    // omalle sisällölleen olisi toinen ylläpidettävä luku ilman omaa
                    // mittausta, ja tämä on jo se leveys jolla sarakkeen teksti on
                    // luettavaa. Ilman tätä riviä pisteet, pipit ja pelaajien nimet eivät
                    // ole tabletilla ruudulla missään, koska paneelia ei ole ja
                    // otteluluettelokin näyttää vain nimen ja polun.
                    playerFactsInTray = showTrayFacts,
                    ownedCubeInPanel = panelCube != null,
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
                    unconfirmedRefresh = unconfirmedRefresh,
                    stripNotes = if (situationSlot != null) stripNotes else emptyList(),
                    stripNotesSlot = situationSlot,
                )
                if (fits && panelOnRight) {
                    Spacer(modifier = Modifier.width(SIDE_PANEL_GAP))
                    panel()
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
                            onPress(label, verified)
                        },
                        onDismiss = { asked = null },
                    )
                }

                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                    // Rastittamaton kuutioteko ei lähde eikä katoa tyhjään: ruutu sanoo
                    // mitä puuttuu. Sivusto vastaisi samaan painallukseen punaisella
                    // rivillä `Previous move not verified!` (mitattu 3.9.2026), ja tämä
                    // on sama tieto ennen pyyntöä eikä sen jälkeen.
                    if (hintSlot == null && verifyHintText != null) {
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
                marks = marks,
                marking = marking && gameKnown && board.moveNumber != null,
                moveNumber = board.moveNumber,
                draft = markDraft,
                onDraftChange = { markDraft = it },
                onMark = {
                    onMarkPosition(it)
                    markDraft = ""
                    marking = false
                },
                onRemove = onRemoveMark,
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
                    color = Palette.TextSecondary,
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
                    color = Palette.TextSecondary,
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
                    focusedTextColor = Palette.TextPrimary,
                    unfocusedTextColor = Palette.TextPrimary,
                    focusedBorderColor = Palette.Accent,
                    unfocusedBorderColor = Palette.PanelOutline,
                    focusedLabelColor = Palette.Accent,
                    unfocusedLabelColor = Palette.TextMuted,
                    cursorColor = Palette.Accent,
                ),
            )
            OutlinedButton(
                onClick = { onAdd(draft) },
                // Tyhjä rivi ei ole muistutus. Sama sääntö kuin viestiruudun
                // lähetysnapilla, vaikka syy on lievempi: tässä ei kulu tekoa.
                enabled = draft.isNotBlank(),
                // Värit käsin samasta syystä kuin kentällä yllä: tämä on laudan oma
                // tumma paletti eikä teeman pinta. Oletusten disabloitu teksti oli
                // tummalla pohjalla niin heikko, ettei sovelluksen ainoa käyttäjä
                // huomannut koko nappia (25.8.2026). Disabloitu saa TextMutedin:
                // himmeä mutta luettava, sama sävy kuin kentän lepotilan label.
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Palette.Accent,
                    disabledContentColor = Palette.TextMuted,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (draft.isNotBlank()) Palette.Accent else Palette.PanelOutline,
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
            color = Palette.TextMuted,
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
    OutlinedButton(
        onClick = { onFollow(href) },
        modifier = if (compact) modifier.height(DgBoard.COMPACT_BUTTON_HEIGHT) else modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DgBoard.Palette.TextSecondary,
        ),
        border = BorderStroke(DgBoard.OUTLINE, DgBoard.Palette.TextSecondary),
    ) {
        Text(
            text = stringResource(R.string.board_skip_game),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Normal,
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
     * `Modifier.height` ohittaa napin oman `defaultMinSize`n, koska se antaa kiinteän
     * rajoitteen. Kosketusala kapenee, ja se on hinta jonka ahdas paneeli maksaa
     * päällekkäisyyden sijaan.
     */
    compact: Boolean = false,
) {
    // **Alleviivaus eikä oma väri** (Tommin päätös 27.8.2026, `docs/UI.md`). Nämä ovat
    // painettavia, ja sivupaneelissa ne olivat samanvärisiä kuin ottelun staattiset tiedot,
    // eli painettava näytti samalta kuin luettava. Väri ei ollut vapaana: `Accent` on
    // samassa paneelissa staattisella rivillä, `CheckerSelf` on erottumaton
    // `TextPrimary`sta, ja `CubeSoft` on peruuttamattoman teon reunusväri eli väittäisi
    // muistutuksesta päinvastaista kuin se on. Alleviivaus ei varaa yhtään väriä eikä
    // kilpaile olemassa olevien merkitysten kanssa.
    val link = if (compact) Modifier.height(DgBoard.COMPACT_LINK_HEIGHT) else Modifier
    TextButton(
        onClick = onToggleComposing,
        modifier = link,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        LinkText(stringResource(R.string.board_reminders_title))
    }
    if (showCube) {
        TextButton(
            onClick = onToggleCube,
            modifier = link,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            LinkText(stringResource(R.string.board_reminder_cube))
        }
    }
    if (showMark) {
        TextButton(
            onClick = onToggleMarking,
            modifier = link,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            LinkText(stringResource(R.string.board_mark_position))
        }
    }
}

/**
 * Merkityt asemat laudan alla ja merkin kirjoituskenttä.
 *
 * **Samaa lajia kuin [ReminderStrip] mutta eri elinkaarta** (Tommin toive 15.9.2026 illalla).
 * Rivi näyttää tämän pelin merkit, jotta painallus näkyy tehdyksi, mutta kannassa merkki
 * elää ottelun yli ja luetaan otteluluettelon linkistä (`MarksScreen`). Siksi rivi sanoo
 * siirtonumeron eikä pelkkää sanaa: numero on se jolla asema löytyy analyysiohjelmasta.
 *
 * Sana on vapaaehtoinen ja nappi on aina painettavissa, toisin kuin muistutuksen `Add`:
 * merkin arvo on kohdassa eikä selityksessä. Kenttä on suljettuna oletuksena samasta
 * syystä kuin muistutuksilla, eli korkeus.
 */
@Composable
private fun MarkStrip(
    marks: List<MarkedPosition>,
    marking: Boolean,
    moveNumber: Int?,
    draft: String,
    onDraftChange: (String) -> Unit,
    onMark: (String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    if (marks.isEmpty() && !marking) return

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        marks.forEach { mark -> MarkRow(mark = mark, onRemove = onRemove) }

        if (!marking) return@Column

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
                    focusedTextColor = Palette.TextPrimary,
                    unfocusedTextColor = Palette.TextPrimary,
                    focusedBorderColor = Palette.Accent,
                    unfocusedBorderColor = Palette.PanelOutline,
                    focusedLabelColor = Palette.Accent,
                    unfocusedLabelColor = Palette.TextMuted,
                    cursorColor = Palette.Accent,
                ),
            )
            OutlinedButton(
                onClick = { onMark(draft) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Accent),
                border = BorderStroke(width = 1.dp, color = Palette.Accent),
            ) {
                Text(
                    // Napissa lukee mitä se merkitsee, jotta numero on nähty ennen painallusta.
                    text = moveNumber?.let { stringResource(R.string.board_mark_add) + " $it" }
                        ?: stringResource(R.string.board_mark_add),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(
            text = stringResource(R.string.board_mark_scope),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextMuted,
        )
    }
}

@Composable
private fun MarkRow(mark: MarkedPosition, onRemove: (Long) -> Unit) {
    val removeLabel = stringResource(R.string.board_mark_remove)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (mark.note.isBlank()) {
                stringResource(R.string.board_mark_row, mark.moveNumber)
            } else {
                stringResource(R.string.board_mark_row_note, mark.moveNumber, mark.note)
            },
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Accent,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onRemove(mark.id) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.semantics { contentDescription = removeLabel },
        ) {
            Text(
                text = "\u00d7",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextMuted,
            )
        }
    }
}

/**
 * Sovelluksen oma painettava teksti, eli linkki.
 *
 * **Viiva piirretään itse eikä `TextDecoration.Underline`illa** (Tommin havainto 27.8.2026).
 * `TextDecoration`in paksuus tulee fontin metriikasta, ja tällä kirjasinkoolla se on
 * hiusviiva joka on mustaa taustaa vasten vaikea nähdä. Paksuus ei ole `TextDecoration`issa
 * säädettävissä lainkaan, joten ainoa tapa saada se omaksi mitaksi on piirtää viiva.
 *
 * Väri on sama kuin tekstillä (`TextSecondary`, 12,45:1 paneelin taustaa vasten), koska
 * ongelma oli paksuus eikä kontrasti; ks. `docs/UI.md` värivalinnan perustelu.
 */
@Composable
private fun LinkText(text: String) {
    val color = Palette.TextSecondary
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.drawBehind {
            val stroke = LINK_UNDERLINE.toPx()
            // Puoli paksuutta ylös, jotta viiva mahtuu kokonaan tekstin laatikkoon eikä
            // leikkaudu alareunasta.
            val y = size.height - stroke / 2f
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
            )
        },
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
            color = Palette.Accent,
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
                color = Palette.TextMuted,
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
)

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
    verify: VerifyBox = VerifyBox(false) {},
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
            verified = verify.checked,
            onVerified = verify.onChange,
            showSkip = showSkip,
            busy = busy,
            arc = arc,
        ).forEach { item ->
            when (item) {
                ActionItem.Gap -> Spacer(Modifier.width(IRREVERSIBLE_GAP))
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
    verify: VerifyBox = VerifyBox(false) {},
    /** Pyyntö on kesken: napit pois ja kaari tilalle, ks. [actionItems]. */
    busy: Boolean = false,
    /** Kaaren värit tältä laudalta, ks. [BusyArc]. */
    arc: ArcLook = ArcLook.X22,
    /**
     * Ahdas paneeli (Tommin päätös 16.9.2026): napit [PANEL_ACTION_HEIGHT_COMPACT]
     * -korkuisina. Peruuttamattoman rako [IRREVERSIBLE_GAP] ei kapene, koska se on
     * auditoinnin vaatimus (C3) eikä asettelua.
     */
    compact: Boolean = false,
) {
    val form = if (showSubmits) board.form else null
    val height = if (compact) PANEL_ACTION_HEIGHT_COMPACT else PANEL_ACTION_HEIGHT
    // **Ahtaassa paneelissa pino ei saa koskea ottelukorttiin** (Tommin kaappaus 16.9.2026:
    // *"ottelukortti ja roll dice nappi on liian kiinni toisissaan"*). Väljässä paneelissa
    // keskitys jättää välin itsestään; ahtaassa lohkossa keskitys palautuu kiinni korttiin
    // (`DgBoard.centeredTop`), joten väli on annettava tässä. Sama 8 dp kuin korttien
    // välillä (`SidePanel`), ja se maksettiin napin korkeudesta (36 → 32).
    Column(
        modifier = Modifier.fillMaxWidth().then(if (compact) Modifier.padding(top = 8.dp) else Modifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        actionItems(
            board = board,
            form = form,
            buttonModifier = Modifier.fillMaxWidth().height(height),
            compactFrame = compact,
            // Rako nappiin on pinon oma väli toiseen kertaan (Tommin tarkennus 14.9.2026:
            // *"hieman rako korkeussuunnassa"*): ruutu ei ole nappi eikä kuulu sen kylkeen.
            verifyRowModifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            frameVerify = true,
            onFollow = onFollow,
            onPress = onPress,
            verified = verify.checked,
            onVerified = verify.onChange,
            // Skip on paneelissa muistutusten seurassa (27.8.2026), ei tässä pinossa.
            showSkip = false,
            busy = busy,
            arc = arc,
        ).forEach { item ->
            when (item) {
                // Ahtaassa paneelissa rako on pinon oma väli kolmesti (6 + 6 + kehyksen
                // pehmuste 6) plus 6, eli 24 dp nappien väliin: täsmälleen C3:n alaraja
                // eikä sen alle. Väljässä paneelissa 18 kuten rivillä.
                ActionItem.Gap -> Spacer(
                    Modifier.height(if (compact) IRREVERSIBLE_GAP_COMPACT else IRREVERSIBLE_GAP),
                )
                is ActionItem.Content -> item.body()
            }
        }
    }
}

/** Napin korkeus paneelipinossa; keskikaistalla se johdetaan kaistasta, täällä sitä ei ole. */
private val PANEL_ACTION_HEIGHT = 44.dp

/** Ahtaan paneelin napin korkeus, ks. [PanelActionStack] ja [DgBoard.PANEL_COMPACT_BELOW]. */
private val PANEL_ACTION_HEIGHT_COMPACT = 32.dp

/** Vahvistusruudun sivu; sama mitta on pinon ja nimikortin väli paneelissa. */
private val VERIFY_BOX = 20.dp

/** Kuution sivu pelaajakortissa: nimen ja away-rivin yhteinen korkeus (14.9.2026), ei laudan mitta. */
private val CARD_CUBE = 40.dp

/** Sivun lomakkeen nappi; peruuttamaton saa kuutiotealin ja reunuksen (24.8.2026). */
@Composable
private fun SubmitButton(
    label: String,
    modifier: Modifier,
    verified: Boolean,
    onPress: (String, Boolean) -> Unit,
) {
    val irreversible = label in IRREVERSIBLE_SUBMITS
    Button(
        onClick = { onPress(label, verified) },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        // Peruuttamattoman teon väri on kuutioteal eikä punainen (24.8.2026).
        colors = ButtonDefaults.buttonColors(
            containerColor = DgBoard.Palette.CheckerSelf,
            contentColor = if (irreversible) DgBoard.Palette.Cube else DgBoard.Palette.OnCheckerSelf,
        ),
        border = if (irreversible) BorderStroke(DgBoard.OUTLINE, DgBoard.Palette.CubeSoft) else null,
    ) {
        Text(submitText(label), style = MaterialTheme.typography.labelLarge)
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
        Canvas(modifier = Modifier.size(BUSY_ARC).rotate(kulma)) {
            val viiva = 3.dp.toPx()
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

/** Kaaren halkaisija; pienempi kuin pienin nappikorkeus (32 dp), jotta se mahtuu aina. */
private val BUSY_ARC = 26.dp

/** Kaaren kaksi laudasta luettavaa väriä: kiilan oranssi ja oma nappula, ks. [BusyArc]. */
private data class ArcLook(val wedge: Color, val checker: Color) {
    companion object {
        val X22 = ArcLook(DgBoard.Palette.WedgeEven, DgBoard.Palette.CheckerSelf)
        fun of(roles: BoardRoles) = ArcLook(roles.look.wedgeEven, roles.selfPaint().fill)
    }
}

/** Sivun oma vahvistusruutu tekstineen (`Verify Double`, `Verify Accept`). */
@Composable
private fun VerifyRow(
    form: BoardForm,
    modifier: Modifier,
    verified: Boolean,
    onVerified: (Boolean) -> Unit,
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
            checked = verified,
            onCheckedChange = onVerified,
            modifier = Modifier.size(VERIFY_BOX),
            colors = CheckboxDefaults.colors(
                checkedColor = DgBoard.Palette.CheckerSelf,
                uncheckedColor = DgBoard.Palette.CheckerSelf,
                checkmarkColor = DgBoard.Palette.OnCheckerSelf,
            ),
        )
        Text(
            text = stringResource(R.string.board_verify, form.verify.orEmpty()),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextPrimary,
        )
    }
}

/**
 * Kuutioteon kehys paneelipinossa (Tommin valinta 15.9.2026 yöllä): peruuttamattomat napit
 * ja vahvistusruutu saman reunuksen sisällä, reunus kuution pehmeällä värillä kuten
 * peruuttamattoman napin oma reunus. Sama keino kuin kuution omistajan kortissa: alue
 * sanoo kuuluvuuden, eikä lukijan tarvitse tulkita rivin paikkaa.
 */
@Composable
private fun VerifyFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(DgBoard.OUTLINE, DgBoard.Palette.CubeSoft, RoundedCornerShape(6.dp))
            .padding(VERIFY_FRAME_PAD),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        // Vahvistusruutu kehyksen keskelle (Tommin tilaus 15.9.2026 illalla kesken pelisession:
        // *"keskitä verify korttiinsa"*). Vasen laita oli 14.9. valinta kehyksettömään pinoon;
        // kehyksen sisällä rivi on kortin oma rivi, ja kortti keskittää sisältönsä.
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Kehyksen sisus; napit ovat tämän verran kapeampia kuin pinon muut, ja se on osa kuuluvuuden merkkiä. */
private val VERIFY_FRAME_PAD = 6.dp

/** Yksi toimintorivin tai -pinon alkio: nappi tai peruuttamattoman teon väli. */
private sealed interface ActionItem {
    data object Gap : ActionItem
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
     * sisällä tätä ei käytetä, ks. [VerifyFrame].
     */
    verifyRowModifier: Modifier = Modifier,
    /** Paneelipino kehystää peruuttamattomat napit ja vahvistusruudun yhdeksi, ks. [VerifyFrame]. */
    frameVerify: Boolean = false,
    /**
     * Ahdas paneeli (Tommin päätös 16.9.2026: *"checkbox, verify ja double-nappi samalla
     * rivillä"*): kun kehyksessä on yksi nappi, nappi ja vahvistusrivi ovat rinnakkain.
     * Kahden napin kehys (`Accept`, `Decline`) pysyy pinona, koska rivi ei mahdu paneeliin.
     */
    compactFrame: Boolean = false,
    onFollow: (String) -> Unit,
    onPress: (String, Boolean) -> Unit,
    verified: Boolean,
    onVerified: (Boolean) -> Unit,
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
        add(ActionItem.Content { BusyArc(buttonModifier, arc) })
        return@buildList
    }
    val submits = form?.submits.orEmpty()
    // **Kuutioteko on yksi kehystetty kokonaisuus pinossa** (Tommin valinta 15.9.2026 yöllä
    // neljästä vaihtoehdosta: *"onko parempaa tapaa osoittaa sen kuuluvuutta yllä olevaan"*).
    // Peruuttamattomat napit ja niiden vahvistusruutu piirretään saman kehyksen sisään,
    // jolloin ruudun kuuluvuus nappiin luetaan alueesta eikä rivin paikasta. Keskikaistalla
    // kohteet ovat vierekkäin eikä kehystä ole; siellä ruutu on oma kohteensa kuten ennen.
    val framed = frameVerify && form?.verify != null
    val firstIrreversible = submits.indexOfFirst { it in IRREVERSIBLE_SUBMITS }
    submits.forEachIndexed { index, label ->
        val irreversible = label in IRREVERSIBLE_SUBMITS
        // Peruuttamaton nappi erotetaan peruttavista välillä eikä vain värillä (foorumin
        // valitus 3/2022 osumakohdasta). Järjestys pysyy sivun järjestyksenä.
        if (irreversibleGapBefore(submits, index)) add(ActionItem.Gap)
        when {
            framed && irreversible && index == firstIrreversible -> add(
                ActionItem.Content {
                    val framedLabels = submits.filter { it in IRREVERSIBLE_SUBMITS }
                    VerifyFrame {
                        if (compactFrame && framedLabels.size == 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SubmitButton(framedLabels.single(), Modifier.weight(1f).then(buttonModifier), verified, onPress)
                                VerifyRow(form, Modifier, verified, onVerified)
                            }
                        } else {
                            framedLabels.forEach { framedLabel ->
                                SubmitButton(framedLabel, buttonModifier, verified, onPress)
                            }
                        // Sisältönsä levyinen, jotta kehys voi keskittää sen; rako nappiin
                        // on sama kuin pinon ulkopuolella.
                            VerifyRow(form, Modifier.padding(top = 6.dp), verified, onVerified)
                        }
                    }
                },
            )
            framed && irreversible -> Unit // piirretty jo kehyksen sisään
            else -> add(ActionItem.Content { SubmitButton(label, buttonModifier, verified, onPress) })
        }
    }
    if (form?.verify != null && !framed) {
        add(ActionItem.Content { VerifyRow(form, verifyRowModifier, verified, onVerified) })
    }

    // Peruminen on linkki eikä nappi sivustolla, mutta ruudulla nappi 22.8.2026 alkaen
    // (Tommin valinta): löytyy yhtä helposti kuin lähetys, muttei näytä yhtä painavalta.
    board.undoHref?.let { href ->
        add(
            ActionItem.Content {
                OutlinedButton(
                    onClick = { onFollow(href) },
                    modifier = buttonModifier,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DgBoard.Palette.CheckerSelf,
                    ),
                    border = BorderStroke(DgBoard.OUTLINE, DgBoard.Palette.CheckerSelf),
                ) {
                    Text(
                        text = stringResource(R.string.board_undo),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Normal,
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
                OutlinedButton(
                    onClick = { onFollow(command.href) },
                    modifier = buttonModifier,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DgBoard.Palette.TextSecondary,
                    ),
                    border = BorderStroke(DgBoard.OUTLINE, DgBoard.Palette.TextSecondary),
                ) {
                    Text(
                        text = command.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Normal,
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
                otherPips = board.players.getOrNull(1)?.pips,
                onOpen = it.player.profilePath?.let { path -> { onOpenPage(path) } },
                cube = panelCube?.takeIf { c -> c.position == CubePosition.TOP },
                onDouble = onCube,
                cubeAttention = cubeReminder,
                compact = compact,
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
            val moveText = board.moveNumber?.let { stringResource(R.string.board_move_number, it) }
            val joinedTail = if (compact && roundLabel != null && moveText != null) {
                stringResource(R.string.board_round_and_move, roundLabel, moveText)
            } else {
                null
            }
            val matchLines = listOfNotNull(board.eventName, roundLabel.takeIf { joinedTail == null })
            // **Ottelukortti avaa turnaussivun** (Tommin valinta 14.9.2026 illalla, G4
            // `docs/AUDITOINTI-FOORUMI.md`): sama ele kuin pelaajakortilla. Liigaottelulla
            // sivu ei anna linkkiä, ja silloin kortti ei ole napautettava.
            //
            // **Pituusrivi poistui 14.9.2026** (Tommin valinta, peruste `SUBSTANSSI.md`
            // kohdat 90 ja 92): away-luku pelaajakorteissa kantaa jo sen mitä pituus
            // sanoi, ja rivi oli toistoa. Pituus on yhä `BoardState.matchLength`issa.
            if (matchLines.isNotEmpty() || board.moveNumber != null) {
                val eventPath = board.eventPath
                Column(
                    modifier = Modifier
                        .then(if (eventPath != null) Modifier.clickable { onOpenPage(eventPath) } else Modifier)
                        .panelCard(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    matchLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelLarge,
                            color = Palette.TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    (joinedTail ?: moveText)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
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
                otherPips = board.players.getOrNull(0)?.pips,
                onOpen = it.player.profilePath?.let { path -> { onOpenPage(path) } },
                cube = panelCube?.takeIf { c -> c.position == CubePosition.BOTTOM },
                onDouble = onCube,
                cubeAttention = cubeReminder,
                compact = compact,
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
                color = Palette.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Palette.Felt)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (showNotes) board.situation?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = Palette.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Palette.Felt)
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
    content: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(above, content, stack),
        modifier = modifier,
    ) { (aboveMeasurables, contentMeasurables, stackMeasurables), constraints ->
        val loose = constraints.copy(minHeight = 0)
        val abovePlaceables = aboveMeasurables.map { it.measure(loose) }
        val contentPlaceables = contentMeasurables.map { it.measure(loose) }
        val stackPlaceables = stackMeasurables.map { it.measure(loose) }
        val aboveHeight = abovePlaceables.sumOf { it.height }
        val contentHeight = contentPlaceables.sumOf { it.height }
        val stackHeight = stackPlaceables.sumOf { it.height }
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
            var aboveY = (contentTop - aboveHeight) / 2
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
    outline: Color = Palette.PanelOutline,
    outlineWidth: Dp = DgBoard.OUTLINE,
): Modifier = this
    .fillMaxWidth()
    .clip(RoundedCornerShape(6.dp))
    .background(Palette.Felt)
    .border(outlineWidth, outline, RoundedCornerShape(6.dp))
    .padding(horizontal = 12.dp, vertical = 10.dp)

@Composable
private fun PlayerPanelView(
    panel: PlayerPanel,
    away: Int?,
    paint: CheckerPaint,
    /** Toisen pelaajan pip-luku, josta etumatka lasketaan; null pudottaa rivin pois. */
    otherPips: Int?,
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
    /**
     * Tosi kun paneeli on ahdas ([DgBoard.PANEL_COMPACT_BELOW]): away nimen rivillä ja
     * pippien yläpuolinen tyhjä rivi pois, eli kortti on kaksirivinen. Kaksi riviä on
     * alaraja, koska kuutio tarvitsee ne (Tommi 16.9.2026), ks. [SidePanel].
     */
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
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
    Column(
        modifier = modifier
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .panelCard(
                outline = when {
                    reminded -> Palette.CubeLaser
                    cube != null -> Palette.Cube
                    else -> Palette.PanelOutline
                },
                outlineWidth = if (reminded) DgBoard.OUTLINE * 2 else DgBoard.OUTLINE,
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(paint.fill),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = panel.player.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // **Ahtaassa paneelissa away on nimen rivillä** (Tommin päätös
                    // 16.9.2026: *"pelaajakortin nimi ja away voi olla samalla rivillä"*,
                    // vain puhelimelle, tabletin kortti ennallaan). Pipit jäävät omalle
                    // rivilleen samasta päätöksestä: kuutio tarvitsee kaksirivisen kortin.
                    if (compact) scoreText(panel, away)?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
                // Pistekenttä away-notaationa, ks. [scoreText].
                if (!compact) scoreText(panel, away)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextSecondary,
                    )
                }
            }
            if (cube != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Muistutuslippu kuution päälle (Tommin valinta 14.9.2026 illalla, AVOIMET
                    // *Kuutiomuistutus jää huomaamatta*): `PICK ME` ei mahtunut kuution
                    // pintaan, ja vaihtoehto oli erillinen pieni lippu sen viereen. Sama väri
                    // kuin kortin ja kuution korostuskehyksellä.
                    if (cubeAttention) {
                        Text(
                            text = stringResource(R.string.board_cube_reminder_flag),
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.CubeLaser,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    CubeFace(cube, CARD_CUBE, onDouble, cubeAttention)
                }
            }
        }
        // **Pipit kolmantena rivinä** (Tommin valinta 14.9.2026 illalla viidestä
        // visualisoidusta paikasta: *"entä jos pips laittaisi henkilökorttiin?"*, *"kolmas
        // rivi"*). Pipit olivat täällä 15.8.2026 asti paljaana lukuna ja siirtyivät sitten
        // numerorivin muurin kohdalle, jossa kapea väli ei mahduttanut sanaa eikä etumatkaa
        // tabletillakaan (mitattu 14.9. klo 23.03). Kortissa tila on, ja luku on
        // pelaajan oma tieto samaan tapaan kuin away. Muurin väli on taas tyhjä.
        // Etumatka on kilpajuoksun etumatka, ks. [DgBoard.pipLead].
        val pips = panel.pips
        if (pips != null && otherPips != null) {
            // Tyhjä rivi ennen pippejä (Tommin tarkennus 14.9.2026 kaappauksen jälkeen:
            // *"jos tilaa on niin Pips riviä voisi edeltää tyhjä rivi"*). Tilaa on aina
            // kun paneeli on, koska paneeli mahtuu vain kun korkeutta on yli; lokerosarake
            // ei saa riviä, koska siellä tilaa ei ole. Rivin mitta on tekstirivin mitta,
            // ei erillinen vakio.
            val style = MaterialTheme.typography.bodyMedium
            val line = with(LocalDensity.current) {
                if (style.lineHeight.isSpecified && style.lineHeight.isSp) style.lineHeight.toDp() else 20.dp
            }
            if (!compact) Spacer(modifier = Modifier.height(line))
            Text(
                text = stringResource(R.string.board_pips_lead, pips, DgBoard.pipLead(pips, otherPips)),
                style = style,
                color = Palette.Accent,
            )
        }
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
    /**
     * Omien noppien painallus, tai null kun painallus ei tee mitään. Valmis teko eikä ehto:
     * sääntö on kutsujalla ([diceTapFor]), jotta lauta ei tunne kokoamisen tiloja.
     */
    onDiceTap: (() -> Unit)? = null,
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
    verify: VerifyBox = VerifyBox(false) {},
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
     * Onko tälle pelille voimassa kuutiomuistutus. Lauta korostaa silloin kuutiota, ks.
     * [cubeAttention].
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
                trayWidth,
                trayOnLeft = roles.look.mirrored,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(roles.look.felt),
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
                // eikä avaa dialogia (3.9.2026).
                val onCube: (() -> Unit)? = when {
                    canDouble -> ({ pressChecked(DOUBLE_SUBMIT, verify.checked) })
                    canAccept -> ({ pressChecked(ACCEPT_SUBMIT, verify.checked) })
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
                            cube = if (cubeOnStrip) shownCube else null,
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
                            .background(roles.look.frame),
                    )
                }
                val tray: @Composable () -> Unit = {
                    OffTrayColumn(
                        board,
                        roles,
                        metrics,
                        awayOf,
                        playerFactsInTray,
                        // Vain omistamaton ja tarjottu; omistettu on muurilla ([BarSegment]).
                        cube = if (cubeOnStrip || cubeOwned) null else shownCube,
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

            }
            NumberRow(
                bottomNumbers(roles.look.mirrored, left = true),
                bottomNumbers(roles.look.mirrored, left = false),
                board.points,
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
 * Kapea väli ei mahduttanut sanaa eikä etumatkaa tabletillakaan, joten pipit ovat taas
 * pelaajakortissa ([PlayerPanelView]) ja väli on tyhjä.
 */
@Composable
private fun NumberRow(
    leftNumbers: List<Int>,
    rightNumbers: List<Int>,
    points: List<Point>,
    trayWidth: Dp,
    /** Onko lokerosarake vasemmalla. Sama ehto kuin laudalla, ks. `playArea`/`tray`. */
    trayOnLeft: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = NUMBER_ROW_PADDING)) {
        // Varaus on samalla puolella kuin sarake itse, muuten pistenumerot jäisivät
        // peilattuna kiilojensa vierestä sarakkeen leveyden verran sivuun.
        if (trayOnLeft) Spacer(modifier = Modifier.width(trayWidth))
        NumberGroup(leftNumbers, points, modifier = Modifier.weight(6f))
        // Muurin väli. Kantoi pip-luvun 15.8.–14.9.2026, nyt taas tyhjä, ks. [PlayerPanelView].
        Spacer(modifier = Modifier.weight(1f))
        NumberGroup(rightNumbers, points, modifier = Modifier.weight(6f))
        if (!trayOnLeft) Spacer(modifier = Modifier.width(trayWidth))
    }
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
    val style = MaterialTheme.typography.labelSmall
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
 */
@Composable
private fun NumberGroup(numbers: List<Int>, points: List<Point>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        numbers.forEach { number ->
            val point = points.firstOrNull { it.number == number }
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (point == null) {
                    Palette.TextMuted.copy(alpha = UNKNOWN_ALPHA)
                } else {
                    Palette.TextSecondary
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

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
    pointsDown: Boolean,
    modifier: Modifier = Modifier,
) {
    val wedgeColor = if (number % 2 == 0) roles.look.wedgeEven else roles.look.wedgeOdd
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp)
            // Koko sarake ottaa kosketuksen vastaan eikä pelkkä nappula: kiila on kapea, ja
            // nappulan kokoinen kohde alittaisi Materialin 48 dp:n kosketusalueen reilusti.
            // `clickable` vain kun linkki on, joten piste jolta ei voi siirtää ei myöskään
            // reagoi eikä jätä semantiikkapuuhun toimintoa.
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
    verify: VerifyBox = VerifyBox(false) {},
    /** Omien noppien painallus, ks. [diceTapFor]. Null kun painallus ei tee mitään. */
    onDiceTap: (() -> Unit)? = null,
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
            .background(roles.look.band)
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
                DiceGroup(opponentDice, roles, metrics)
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
        // *"YellowishSun has doubled. teksti sopisi keskipaneliin"*, ja tarkennus *"laudan
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
            Column(
                modifier = Modifier.align(alignment).width(group),
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
                MiddleActionsRow(board, metrics, onFollow, onPress, trailing, showSkip, showSubmits, verify, busy, ArcLook.of(roles))
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
        unconfirmedRefresh?.let { UnconfirmedRefresh(it.reason, metrics, it.onRefresh) }
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
    /** Kuutiomuistutus on voimassa: kuutio korostuu, ks. [cubeAttention]. */
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
                color = Palette.TextPrimary,
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
 * Kuution korostus kun kuutiomuistutus on voimassa.
 *
 * @property ring kehyksen ja hehkun voimakkuus 0..1.
 * @property heat silmän väri: 1 on punainen laser, 0 on purppura. Ks. [CubeRobotFace].
 * @property eyes silmien näkyvyys 1..0: 1 on laser, 0 on tavallinen reikä kuoressa.
 */
@Immutable
private data class CubeAttention(val ring: Float, val heat: Float, val eyes: Float)

/**
 * Kuution korostuksen voimakkuus, kun kuutiomuistutus on voimassa.
 *
 * **Kuusi pulssia ja sen jälkeen pysyvä kehys, ei jatkuvaa vilkkumista** (määrä kaksinkertaistui
 * Tommin pyynnöstä 3.9.2026, kolme oli liian ohi ennen kuin katse ehti kuutioon). Rajaus itse on
 * mitattu: laudan asettelu on kerran tuottanut 294 värähtelevää ruutua viidessä sekunnissa
 * tyhjäkäynnillä (`docs/TESTAUS.md`), ja paikallaan olevan näkymän kuuluu piirtää nolla.
 * Pulssit ovat rajattu tapahtuma, kehys ei piirrä mitään sen jälkeen.
 *
 * **Silmä jäähtyy punaisesta purppuraan täsmälleen pulssien mitassa** (Tommin pyyntö 3.9.2026).
 * Punainen on huomioväri jota lauta käyttää jo kahteen muuhun asiaan, joten se ei kelpaa
 * pysyväksi tilaksi; hetkellisenä se kelpaa, koska katse on silloin juuri saapumassa eikä
 * lukemassa laudan värejä. Jäähtyminen loppuu samalla hetkellä kuin vilkkuminen, jotta ruutu
 * asettuu kerralla eikä kahdessa erässä.
 *
 * **Lepotila on purppura kehys, ei purppura silmä** (Tommin valinta 3.9.2026): pulssien jälkeen
 * silmät häivytetään takaisin tavallisiksi rei'iksi ja muistutus jää kehykseen. Silmät ovat siis
 * huomion herätys ja kehys on muistin paikka, eli sama jako kuin animoidun ja pysyvän välillä
 * muutenkin. Kehys on myös se osa joka näkyy numeropinnalla, jossa silmiä ei ole lainkaan.
 *
 * **Animaatioasetus luetaan laitteelta.** `ANIMATOR_DURATION_SCALE` on nolla kun käyttäjä on
 * poistanut animaatiot käytöstä, ja silloin korostus tulee suoraan pysyvänä purppurana
 * kehyksenä ilman lasersilmiä. Se on esteettömyysasetus eikä makuasia: vilkkuminen ja
 * välähtävä punainen ovat juuri se osa jota se koskee.
 */
@Composable
private fun cubeAttention(attention: Boolean): CubeAttention {
    val context = LocalContext.current
    val animate = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val level = remember { Animatable(0f) }
    val heat = remember { Animatable(0f) }
    val eyes = remember { Animatable(0f) }
    LaunchedEffect(attention, animate) {
        if (!attention) {
            level.snapTo(0f)
            heat.snapTo(0f)
            eyes.snapTo(0f)
            return@LaunchedEffect
        }
        if (animate) {
            heat.snapTo(1f)
            eyes.snapTo(1f)
            // Jäähtyminen kestää koko pulssisarjan, eli se lasketaan samoista luvuista kuin
            // sarja itse eikä omasta vakiostaan: muuten kahden luvun muuttaminen erikseen
            // päästäisi värin asettumaan ennen vilkkumista tai sen jälkeen.
            launch { heat.animateTo(0f, tween(durationMillis = CUBE_PULSES * 2 * CUBE_PULSE_MS)) }
            repeat(CUBE_PULSES) {
                level.animateTo(1f, tween(durationMillis = CUBE_PULSE_MS))
                level.animateTo(0.35f, tween(durationMillis = CUBE_PULSE_MS))
            }
            eyes.animateTo(0f, tween(durationMillis = CUBE_PULSE_MS))
        }
        level.snapTo(0.35f)
        heat.snapTo(0f)
        eyes.snapTo(0f)
    }
    return CubeAttention(level.value, heat.value, eyes.value)
}

/** Kuutiomuistutuksen pulssien määrä, ks. [cubeAttention]. */
private const val CUBE_PULSES = 6

/** Yhden pulssin puolikkaan kesto millisekunteina, ks. [cubeAttention]. */
private const val CUBE_PULSE_MS = 400

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
    /** Kuutiomuistutus on voimassa tälle pelille, ks. [cubeAttention]. */
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
    val pulse = cubeAttention(attention)
    val ring = pulse.ring
    val shape = RoundedCornerShape(size * DgBoard.CORNER_PER_SIDE)
    Box(
        modifier = Modifier
            .size(size)
            // Pyöristys on suhde sivuun eikä kiinteä dp, ks. [DgBoard.CORNER_PER_SIDE].
            .clip(shape)
            .background(Palette.Cube)
            .then(
                if (ring > 0f) {
                    Modifier.border(DgBoard.OUTLINE * (1f + 2f * ring), Palette.CubeLaser, shape)
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
            CubeRobotFace(size, ring, pulse.heat, pulse.eyes)
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
                fontWeight = FontWeight.Bold,
                color = Palette.OnCube,
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
    /** Sykkeen taso 0..1, ks. [cubeAttention]. Ohjaa hehkun peittoa. */
    glow: Float = 0f,
    /** Silmän lämpö 1..0 eli punaisesta purppuraan, ks. [cubeAttention]. */
    heat: Float = 0f,
    /** Silmien näkyvyys 1..0, ks. [cubeAttention]. Nolla on tavallinen reikä kuoressa. */
    laserEyes: Float = 0f,
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
            drawPath(dome, Palette.OnCube)

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
            val eye = lerp(Palette.Cube, laser, laserEyes)
            eyes.forEach { drawCircle(eye, radius = 2.1f * u, center = it) }

            drawLine(
                Palette.OnCube,
                start = Offset(23f * u, 22f * u),
                end = Offset(18.5f * u, 14.5f * u),
                strokeWidth = 2.2f * u,
                cap = StrokeCap.Round,
            )
            drawLine(
                Palette.OnCube,
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
) {
    val count = board.borneOff.filter { it.color == color }.sumOf { it.count }

    // Tyhja lokero ei kerro mitaan, joten sita ei piirreta (Tommin valinta 22.8.2026
    // pelisession aikana). Ulos kannettuja on nollasta lahtien vasta pelin lopussa, eli
    // valtaosan ajasta ruudulla oli `OFF` ja sen alla `0` molemmilla pelaajilla.
    //
    // **Paikka jaa varatuksi, ja se on tarkoitus.** Lokero saa `weight(1f)`:n riippumatta
    // tasta, joten sarakkeen keskikohta ei liiku kun ensimmainen nappula kannetaan ulos,
    // vaan lokero ilmestyy paikkaan joka oli jo tyhjana.
    if (count == 0) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(look.tray)
            .border(DgBoard.OUTLINE, look.trayOutline, RoundedCornerShape(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.board_off),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(metrics.checkerSize)
                .clip(CircleShape)
                .background(paint.fill)
                .border(DgBoard.OUTLINE, Palette.Outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = paint.onFill,
            )
        }
    }
}

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
                    .border(DgBoard.OUTLINE, Palette.Outline, CircleShape),
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
            Palette.TextMuted
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
 * Sivun vahvistusruudun tila ruudun tasolla.
 *
 * Ruutu oli 3.9.2026 asti nappirivin ja paneelipinon oma tila, ja kuution klikkaus ei
 * nähnyt sitä lainkaan. Kun rasti on pakollinen ([cubeActionFor]), kaikkien kolmen reitin
 * on luettava sama ruutu, joten tila asuu kutsujalla ja kulkee tässä yhtenä arvona.
 */
internal class VerifyBox(val checked: Boolean, val onChange: (Boolean) -> Unit)

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
 * Kun sivu ei pyydä vahvistusta (pelaajan asetus pois, ruutua ei ole sivulla), rastia ei
 * voi vaatia, ja teko menee dialogin kautta suoraan. `Decline` ei kuulu tähän: sivu ei
 * vaadi sille rastia, ja se meni ilman rastia läpi kuudesti samana iltana.
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
 * kaappauksesta (`sessio-5-9-ilta2/0015`), ja siinä lukee *erichktn has doubled.* Sama
 * yhdistelmä toistui samana iltana (`sessio-8-9-ilta/0104`, *Tony 77 has doubled.*), ja
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
internal fun cubeDialogBody(label: String): Int =
    if (label == ACCEPT_SUBMIT) R.string.board_accept_text else R.string.board_double_text

/**
 * Kuutio sellaisena kuin se piirretään: tarjottu kuutio näyttää **tarjotun arvon**.
 *
 * **Tommin tilaus 8.9.2026 kesken pelisession** (YellowishSunin tarjous, kuutiossa robotin
 * pää): *"kun tarjotaan tuplauskuutiota, niin sen arvon tulisi päivittyä seuraavaan eli nyt
 * pitäisi androidin pään sijaan olla 2"*. Sivu ei käännä kuutiota ennen hyväksyntää: kuva
 * on tarjouksen aikana yhä `cube1.gif` ja ALT `1` (mitattu 17 sivusta 3.9.2026 ja tänä
 * iltana `sessio-8-9-ilta/0100`). Fyysisessä pelissä tarjoaja kääntää kuution ja työntää
 * sen vastustajalle, eli tarjouksen aikana kuutiossa **lukee** tarjottu arvo. Sivun `1`
 * on tässä kohtaa sama keinotekoinen merkintä joka jo perusteli robotin pään
 * ([CubeRobotFace]), joten tämä ei riko periaatetta "ALT sellaisenaan" enempää kuin se.
 *
 * Tarjous luetaan lomakkeen `Accept`-napista, kuten sijaintikin ([MiddleStrip]), ja
 * peruutettu sivu ei muuta sitä: Tony 77:n tarjous 8.9.2026 tuli peruutusrivin kanssa
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
 * **Tarjous voittaa omistuksen** (Tommin havainto pelisessiossa 14.9.2026: *"kun eg teki
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
internal fun stripNotes(board: BoardState, pips: PipCheck, checkers: CheckerCheck): List<StripNoteSpec> =
    buildList {
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
    }

/**
 * Kummalle keskikaistan puoliskolle sivun tilanneteksti menee, tai null kun se jää
 * sivupaneeliin.
 *
 * **Tommin päätös 8.9.2026**: *YellowishSun has doubled.* kuuluu laudan keskikaistalle,
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

internal fun cubeActionFor(label: String, siteAsksVerify: Boolean, verified: Boolean): CubeAction =
    when {
        label != DOUBLE_SUBMIT && label != ACCEPT_SUBMIT -> CubeAction.SEND
        siteAsksVerify && !verified -> CubeAction.NEEDS_VERIFY
        else -> CubeAction.ASK
    }

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
private val IRREVERSIBLE_SUBMITS = setOf(DOUBLE_SUBMIT, "Accept", "Decline")

/**
 * Lisavali peruuttamattoman napin viereen nappirivilla.
 *
 * Rivin oma vali on 6 dp (`Arrangement.spacedBy`), ja se lisataan myos Spacerin molemmin
 * puolin, joten napin ja peruuttamattoman napin valiin jaa 6 + 18 + 6 = 30 dp ja rivin
 * alkuun 18 + 6 = 24 dp. Checklist-vaatimus on vahintaan 24 dp (dg-mobile-auditointi C3),
 * ja molemmat ylittavat sen.
 */
private val IRREVERSIBLE_GAP = 18.dp

/**
 * Ahtaan paneelipinon rako (16.9.2026). Pinossa napin ja peruuttamattoman napin väliin
 * kertyy pinon väli 6 + rako + väli 6 + kehyksen pehmuste 6, joten 6 dp:n rako antaa
 * täsmälleen C3:n 24 dp. Ei rivillä, jossa pehmustetta ei ole.
 */
private val IRREVERSIBLE_GAP_COMPACT = 6.dp

/**
 * Linkin alleviivauksen paksuus. Fontin oma alleviivaus oli hiusviiva ja mustaa taustaa
 * vasten vaikea nähdä (Tommin havainto 27.8.2026).
 */
private val LINK_UNDERLINE = 1.5.dp

/**
 * Kuuluuko nappirivin kohtaan [index] lisavali ennen nappia.
 *
 * Vali tulee jokaiseen rajaan jossa peruttavuus vaihtuu, molempiin suuntiin: seka ennen
 * peruuttamatonta nappia etta sen jalkeiseen peruttavaan. `Accept`in ja `Decline`n valiin
 * valia ei tule, koska valitus koskee peruttavan ja peruuttamattoman sekoittumista eika
 * kahta samanlajista. Rivin ensimmainen peruuttamaton saa valin aina, jotta se ei piirry
 * siihen kohtaan johon `Roll Dice` piirtyy muissa tiloissa.
 *
 * Oma funktio eika ehto piirtokohdassa, jotta saanto on testattavissa ilman Composea.
 */
internal fun irreversibleGapBefore(submits: List<String>, index: Int): Boolean {
    val irreversible = submits[index] in IRREVERSIBLE_SUBMITS
    val previous = submits.getOrNull(index - 1) ?: return irreversible
    return irreversible != (previous in IRREVERSIBLE_SUBMITS)
}

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
    val accept = label == ACCEPT_SUBMIT
    ConfirmDialog(
        title = stringResource(if (accept) R.string.board_accept_title else R.string.board_double_title),
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
