package fi.tommi.dg.app

import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fi.tommi.dg.app.session.canRequestAppLock
import fi.tommi.dg.app.session.requestAppLockAuthentication
import fi.tommi.dg.app.ui.AppLockScreen
import fi.tommi.dg.app.ui.ArchiveEdgeState
import fi.tommi.dg.app.ui.ArchiveExportFilename
import fi.tommi.dg.app.ui.BackupUiState
import fi.tommi.dg.app.ui.BackupStatus
import fi.tommi.dg.app.ui.BoardRoute
import fi.tommi.dg.app.ui.PageRoute
import fi.tommi.dg.app.ui.PageScreen
import fi.tommi.dg.app.ui.PageUiState
import fi.tommi.dg.app.ui.PageViewModel
import fi.tommi.dg.app.ui.SessionExpiredState
import fi.tommi.dg.app.ui.BoardScreen
import fi.tommi.dg.app.ui.BoardUiState
import fi.tommi.dg.app.ui.NotABoardKind
import fi.tommi.dg.app.ui.BoardViewModel
import fi.tommi.dg.app.ui.DISCUSSION_ROUTE
import fi.tommi.dg.app.ui.DgTab
import fi.tommi.dg.app.ui.ExportUiState
import fi.tommi.dg.app.ui.DgTabAccent
import fi.tommi.dg.app.ui.DgTabRow
import fi.tommi.dg.app.ui.DgTheme
import fi.tommi.dg.app.ui.DiscussionScreen
import fi.tommi.dg.app.ui.DropsScreen
import fi.tommi.dg.app.ui.DiscussionUiState
import fi.tommi.dg.app.ui.DiscussionViewModel
import fi.tommi.dg.app.ui.HelpScreen
import fi.tommi.dg.app.ui.INFO_ROUTE
import fi.tommi.dg.app.ui.InfoScreen
import fi.tommi.dg.app.ui.InfoSection
import fi.tommi.dg.app.ui.LOUNGE_ROUTE
import fi.tommi.dg.app.ui.LoungeScreen
import fi.tommi.dg.app.ui.MatchesSegment
import fi.tommi.dg.app.ui.OwnTournamentsViewModel
import fi.tommi.dg.app.ui.LoungeUiState
import fi.tommi.dg.app.ui.LoungeViewModel
import fi.tommi.dg.app.ui.LocalDgWallpaper
import fi.tommi.dg.app.ui.rememberDgWallpaper
import fi.tommi.dg.app.ui.dgScreenBackground
import kotlin.random.Random
import fi.tommi.dg.app.ui.tabFor
import fi.tommi.dg.app.ui.MESSAGES_ROUTE
import fi.tommi.dg.app.ui.MARKS_ROUTE
import fi.tommi.dg.app.ui.MarksScreen
import fi.tommi.dg.app.ui.MessagesScreen
import fi.tommi.dg.app.ui.MessagesViewModel
import fi.tommi.dg.app.ui.QueueUiState
import fi.tommi.dg.app.ui.SettingsScreen
import fi.tommi.dg.app.ui.SettingsUiState
import fi.tommi.dg.app.ui.SettingsViewModel
import fi.tommi.dg.app.ui.SETTINGS_ROUTE
import fi.tommi.dg.app.ui.TOP_ROUTE
import fi.tommi.dg.app.ui.TopScreen
import fi.tommi.dg.app.ui.TopUiState
import fi.tommi.dg.app.ui.TopViewModel
import fi.tommi.dg.domain.MatchId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * `FragmentActivity` eikä `ComponentActivity`: `BiometricPrompt` vaatii sen.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // **Palkkien ikonit kerrotaan tässä, ja 26.8.2026 alkaen ne kerrotaan yöasetuksen
        // mukaan.** Argumentiton `enableEdgeToEdge()` lukisi saman asetuksen itse, mutta
        // ero on siinä mitä tapahtuu kun asetus ja sovelluksen tausta ovat eri mieltä.
        // Ennen tummaa teemaa sovellus piirsi aina vaalean taustan, jolloin yötilan
        // valitsemat vaaleat ikonit jäivät vaalealle taustalle eli näkymättömiin (mitattu
        // Pixel 8a, 21.8.2026), ja siksi tyyli lyötiin kiinni vaaleaksi. Nyt tausta seuraa
        // yöasetusta, joten ikonien on seurattava samaa asetusta eikä vakiota.
        //
        // `light` tarkoittaa vaaleaa **taustaa** ja siis tummia ikoneita, ei vaaleita.
        // Lautaruutu on tämän poikkeus ja hoitaa oman tilansa itse, ks.
        // `DarkSystemBarIconsWhileVisible`.
        //
        // Yöasetuksen vaihto käynnistää activityn uudelleen (`uiMode` ei ole
        // `configChanges`-listalla), joten tämä luetaan uudelleen silloin kun se muuttuu.
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val barStyle = if (night) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        super.onCreate(savedInstanceState)

        // **Näyttöloven alle saa piirtää (Tommin päätös 16.9.2026).** Androidin oletus
        // (`DEFAULT`) kieltää sen vaakatilassa, ja Pixel 8a:lla kielto vei laudan
        // sivupaneelista 121 px eli viidenneksen sen leveydestä, vaikka itse reikä on
        // 67 px:n ympyrä keskellä paneelin reunaviivaa. Tämä on lupa eikä asettelu:
        // lukuruudut väistävät loven yhä `safeDrawingPadding`illa, ja vain lautaruutu
        // väistää pelkät palkit (`BoardScreen`, `docs/UI.md` › Lovi ja sivupalkki).
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val container = (application as DgApplication).container

        // Manifesti sanoo kylmää käynnistystä varten `portrait`. Jos pystylukko on kytketty
        // pois (Tommin päätös 2.9.2026, `docs/UI.md`), suunta vapautetaan tässä ennen
        // ensimmäistä ruutua eikä vasta navigoinnin efektissä, jotta kädessä vaakana oleva
        // tabletti ei ehdi kääntyä pystyyn ja takaisin.
        if (!container.portraitLock.get()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        setContent {
            DgTheme {
                Surface {
                    // Lukko on prosessin käynnistyksen kohdalla, ei jokaisen taustalta
                    // palaamisen. Laite jolla ei ole omaa suojausta (ei sormenjälkeä,
                    // kasvoja eikä PIN/kuvio/salasanaa) ei voi koskaan avata tätä lukkoa,
                    // joten sellaisella laitteella sitä ei näytetä lainkaan.
                    var locked by remember { mutableStateOf(canRequestAppLock(this@MainActivity)) }

                    fun retry() {
                        requestAppLockAuthentication(
                            activity = this@MainActivity,
                            onSuccess = { locked = false },
                            onFailure = { /* Jää lukkoon; käyttäjä yrittää uudelleen napista. */ },
                        )
                    }

                    LaunchedEffect(Unit) {
                        if (locked) retry()
                    }

                    if (locked) {
                        AppLockScreen(onRetry = ::retry)
                        return@Surface
                    }

                    // Asetusten avaushaku (`docs/ASETUKSET.md` luku 2): kerran per
                    // käynnistys, lukon jälkeen, taustalla. Lauta piirtyy sillä mitä
                    // säilössä on, ja uusi lukema vaikuttaa siitä eteenpäin; epäonnistunut
                    // haku ei kirjoita mitään. Ilman tunnuksia ei haeta, koska vastaus
                    // olisi joka tapauksessa kirjautumissivu eikä asetussivu.
                    LaunchedEffect(Unit) {
                        if (container.credentials.get() != null) {
                            withContext(Dispatchers.IO) {
                                container.siteSettingsRefresher.refresh()
                            }
                        }
                    }

                    val navController = rememberNavController()

                    // **Suunta luetaan navigoinnista eikä ruudun sivuvaikutuksesta**
                    // (24.8.2026, toinen yritys). Ensimmäinen versio oli lautaruudussa
                    // `DisposableEffect`inä, joka palautti suunnan poistuessaan. Laitteella se
                    // ei purrut: laudalta palattua aktiviteetin pyydetty suunta oli
                    // `UNSPECIFIED` eikä `PORTRAIT`, eli lukko oli poissa (mitattu
                    // `dumpsys activity activities`illa, Galaxy Tab S7+ 24.8.2026). Poistuminen
                    // osuu keskelle konfiguraatiomuutosta, koska ruutu on juuri kääntymässä.
                    //
                    // Tässä asetus tehdään sen jälkeen kun kohde on jo voimassa, ja jokainen
                    // navigointi asettaa sen uudelleen. Yksi omistaja, yksi paikka, eikä
                    // riippuvuutta siitä milloin edellinen ruutu ehtii purkautua.
                    //
                    // **Pystylukko on laiteasetus 2.9.2026 alkaen** (Tommin päätös, `docs/UI.md`),
                    // lauta on vaakaan aina. Pois kytkettynä lukuruudut seuraavat laitteen
                    // asentoa (`UNSPECIFIED`). Asetus luetaan tilana, jotta asetusruudussa
                    // tehty vaihto vaikuttaa heti eikä vasta seuraavassa navigoinnissa.
                    val entry by navController.currentBackStackEntryAsState()
                    val onBoard = entry?.destination?.route == BoardRoute.PATTERN
                    var portraitLock by remember { mutableStateOf(container.portraitLock.get()) }
                    var leftHanded by remember { mutableStateOf(container.handedness.get()) }
                    // Taustakuvion väritila tilana samasta syystä kuin pystylukko: vaihto
                    // asetusruudussa näkyy heti sen omassa taustassa.
                    var sumiE by remember { mutableStateOf(container.skyTheme.get()) }
                    var pattern by remember { mutableStateOf(container.skyTheme.pattern()) }
                    // Taustakuvien kansio tilana samasta syystä: valinta asetusruudussa
                    // näkyy heti sen omassa tyhjässä tilassa. Ks. `DgWallpaper.kt`.
                    var wallpaperFolder by remember { mutableStateOf(container.wallpaper.folder()) }

                    LaunchedEffect(onBoard, portraitLock) {
                        this@MainActivity.requestedOrientation =
                            orientationFor(onBoard = onBoard, portraitLock = portraitLock)
                    }

                    // Otteluluettelon malli on activityn tasolla eikä reitin, jotta paluu
                    // laudalta ei laukaise uutta Top Page -hakua. Lista on yhä oikeaa tietoa
                    // siitä hetkestä jolloin se haettiin, ja Refresh on käyttäjän oma valinta.
                    // Poikkeus 18.9.2026: paluu toisesta sovelluksesta luettelon ollessa auki
                    // hakee sen uudestaan (`RefreshOnForeground`), koska silloin on kulunut
                    // aikaa jonka pituutta sovellus ei tiedä.
                    val topModel: TopViewModel = viewModel(
                        factory = TopViewModel.Factory(
                            container.pages,
                            container.credentials,
                            container.matchOrder,
                            container.listedRounds,
                            // Luettelon rivit ottelumuistiin, ks. `MatchMemory`.
                            container.matchMemory,
                        )
                    )

                    // Välilehtipalkki on NavHostin yläpuolella ja piirtyy kaikilla
                    // välilehdillä muttei laudalla: portti on sama reittipredikaatti kuin
                    // suuntalukolla yllä, joten palkki ja vaakatila eivät voi erota.
                    //
                    // popUpTo(TOP_ROUTE) pitää pinon aina korkeintaan kahtena (top ja
                    // päällä oleva välilehti tai lauta), joten järjestelmä-back miltä
                    // tahansa välilehdeltä palaa otteluluetteloon ja siitä ulos.
                    // saveState/restoreState säilyttää välilehden tilan ja näkymämallit
                    // vaihtojen yli, eli paluu välilehdelle ei hae sivua uudestaan;
                    // Refresh on käyttäjän oma valinta kuten otteluluettelossa.
                    fun openTab(tab: DgTab) {
                        navController.navigate(tab.route) {
                            popUpTo(TOP_ROUTE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    // Porautumisruutu ei ole välilehti, ja ilman tätä palkki väitti
                    // vastakkaista: `tabFor` palauttaa nullin, jolloin `DgTabRow` korosti
                    // varakohdettaan Matchesia. Mitattu laitteella 27.8.2026 avaamalla
                    // profiili, ja korostus oli väärä myös silloin kun profiili avattiin
                    // Loungesta. Palkki on siis pois samoin kuin laudalla.
                    val onPage = entry?.destination?.route == PageRoute.PATTERN

                    // Asetusruutu on 4.9.2026 alkaen samaa lajia kuin lauta ja
                    // porautumisruutu: oma reitti välilehtien ulkopuolella, joten palkkia ei
                    // piirretä ja ruutu kuluttaa tilapalkin insetin itse.
                    val onSettings = entry?.destination?.route == SETTINGS_ROUTE
                    // Merkityt asemat samaa lajia kuin asetukset (15.9.2026). Ensimmäinen
                    // laiteajo näytti palkin ja korosti Matchesia, koska ehto puuttui.
                    val onMarks = entry?.destination?.route == MARKS_ROUTE

                    // Välilehden sävy kattaa palkin ja ruudun, jotta korostettu välilehti
                    // ja sen ruudun aksentit ovat samaa väriä. Lauta ja porautumisruutu
                    // saavat nullin `tabFor`ista, eli ne jäävät perusteemaan.
                    DgTabAccent(tab = tabFor(entry?.destination?.route)) {
                    // Ruudun tausta maalataan tässä yhdessä paikassa, ja ruutujen
                    // `Scaffold`it ovat läpinäkyviä. Kuvio on vaalean teeman oma
                    // (`DgPattern.kt`), ja lautanäkymä on sen ulkopuolella samalla
                    // rajauksella kuin muutkin lautaa koskevat poikkeukset tässä
                    // tiedostossa.
                    //
                    // Taivas arvotaan joka ruudun avauksessa (Tommin valinta 6.9.2026
                    // kolmesta: käynnistys, ruudun avaus, päivä). Siemen sidotaan
                    // reittiin, joten välilehden vaihto arpoo uuden ja saman ruudun
                    // uudelleenpiirto pitää entisen. Tilaa ei tallenneta.
                    val route = entry?.destination?.route
                    val skySeed = remember(route) { Random.nextInt() }
                    // Täytekuva (taustakuvat sisällön alle jäävässä tilassa, `DgFill.kt`)
                    // saa saman siemenen kuin taivas, ja lauta on ulkona kuten kuviostakin.
                    // Tila ratkaisee näkyykö se, ei arpa (Tommin päätös 7.9.2026). Kansio on
                    // oma kytkimensä eikä riipu kuviosta (Tommin päätökset 15.9.2026).
                    // Kadonnut oikeus unohtaa kansion, jotta asetusruutu pyytää valitsemaan
                    // uudestaan eikä vaikene.
                    val wallpaper = rememberDgWallpaper(
                        folder = if (onBoard) null else wallpaperFolder,
                        seed = skySeed,
                        onLost = {
                            container.wallpaper.forget()
                            wallpaperFolder = null
                        },
                    )
                    CompositionLocalProvider(LocalDgWallpaper provides wallpaper) {
                    Column(
                        modifier = Modifier.fillMaxSize().dgScreenBackground(!onBoard, skySeed, sumiE, pattern),
                    ) {
                        if (!onBoard && !onPage && !onSettings && !onMarks) {
                            DgTabRow(
                                selected = tabFor(entry?.destination?.route),
                                onSelect = ::openTab,
                            )
                        }

                    NavHost(
                        navController = navController,
                        startDestination = TOP_ROUTE,
                        // Tilapalkin inset kulutetaan tässä kun palkki on piirretty, ja
                        // syy on mitattu (Pixel 8a 26.8.2026): `DgTabRow` kuluttaa insetin
                        // omassa modifier-ketjussaan, mutta kulutus ei näy sisarelle, joten
                        // jokainen välilehtiruutu varasi saman 32 dp:n toistamiseen ja
                        // palkin alle jäi tyhjä kaistale. Laudalla ja porautumisruudulla
                        // ehto ei päde: niissä palkkia ei piirretä, joten ruutu tarvitsee
                        // insetin itse. Mitattu 27.8.2026: ilman tätä porautumisruudun
                        // otsikkorivi piirtyi kellon ja akun päälle.
                        modifier = if (onBoard || onPage || onSettings || onMarks) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)
                        },
                    ) {
                        composable(TOP_ROUTE) {
                            val state by topModel.state.collectAsStateWithLifecycle()
                            // Auki oleva segmentti on reitin tilaa eikä näkymämallin:
                            // se on valinta ruudulla, ei mitään mitä sivustolta luetaan.
                            var segment by rememberSaveable {
                                mutableStateOf(MatchesSegment.YourTurn)
                            }
                            // Omien turnausten polku Top Pagelta virtana, samasta syystä
                            // kuin loungella: kertakuva olisi väärä null aina kun ruutu
                            // avataan ennen luettelon latautumista.
                            val tournamentsPathUpstream = remember(topModel) {
                                topModel.state.map {
                                    (it as? TopUiState.Loaded)?.page?.activeTournamentsPath
                                }
                            }
                            // Oman profiilin polku samasta sivusta: Refresh hakee sen
                            // `Active games` -listan ottelumuistiin (15.9.2026).
                            val profilePathUpstream = remember(topModel) {
                                topModel.state.map {
                                    (it as? TopUiState.Loaded)?.page?.user?.profilePath
                                }
                            }
                            val tournamentsModel: OwnTournamentsViewModel = viewModel(
                                factory = OwnTournamentsViewModel.Factory(
                                    pages = container.pages,
                                    pathUpstream = tournamentsPathUpstream,
                                    countSink = container.ownTournamentCount,
                                    profilePathUpstream = profilePathUpstream,
                                    matchMemory = container.matchMemory,
                                )
                            )
                            val tournaments by tournamentsModel.state
                                .collectAsStateWithLifecycle()
                            val opponentsRefresh by tournamentsModel.opponents
                                .collectAsStateWithLifecycle()
                            // Järjestys on oma virtansa samasta syystä kuin näkymämallissa:
                            // se on laitteen valinta eikä sivulta saatua tilaa.
                            val order by topModel.order.collectAsStateWithLifecycle()
                            // Reunapäivä tulee arkistosta suoraan eikä näkymämallin kautta:
                            // se on paikallista tietoa eikä otteluluettelon tilaa, ja
                            // TopViewModel pysyy luokkana joka vain hakee sivun.
                            // Alkuarvo on oma tilansa eikä sama null jolla tyhjä arkisto
                            // kerrotaan. Ennen 5.9.2026 se oli sama, ja rivi väitti arkiston
                            // olevan tyhjä siihen asti että Room vastasi (kaavahaku 5.9.2026).
                            // Kirjautunut tili, luettuna uudestaan aina kun luettelon tila
                            // vaihtuu: kirjautuminen ja Sign out kulkevat molemmat sen kautta,
                            // joten tämä seuraa tiliä ilman omaa virtaa. Arkisto on
                            // tilikohtainen 16.9.2026 alkaen (`MessageArchive`).
                            val account = remember(state) { container.credentials.get()?.login }
                            // Ennen 16.9.2026 tallennetut rivit ovat omistajattomia, ja
                            // ensimmäinen kirjautunut tili ottaa ne. Idempotentti, joten
                            // toistuva ajo on halpa eikä tee mitään toisella kerralla.
                            LaunchedEffect(account) {
                                if (account != null) {
                                    withContext(Dispatchers.IO) { container.messages.claimUnowned(account) }
                                }
                            }
                            val archiveEdge by remember(account) {
                                container.messages.observeNewestStoredAt(account)
                                    .map(ArchiveEdgeState::from)
                            }.collectAsStateWithLifecycle(initialValue = ArchiveEdgeState.Unknown)
                            // Jono luetaan samalla tavalla ja samasta syystä: se on kannassa
                            // eikä verkossa, joten se on luettavissa myös silloin kun
                            // otteluluettelon haku epäonnistui.
                            val pending by remember { container.actions.observeAll() }
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                            // Ottelumuisti samalla tavalla: kannassa eikä verkossa, ja
                            // turnauslista lukee sitä otteluille joita luettelo ei näytä.
                            val remembered by remember { container.matchMemory.observeAll() }
                                .collectAsStateWithLifecycle(initialValue = emptyMap())
                            // Merkittyjen asemien määrä otsakeriville, kannasta eikä
                            // verkosta kuten reunapäivä. Ks. `MarkBook`.
                            val markCount by remember { container.marks.observeAll().map { it.size } }
                                .collectAsStateWithLifecycle(initialValue = 0)
                            WakeOnReconnect(container.network, topModel::onNetworkAvailable)
                            RefreshOnForeground(topModel::onForeground)
                            TopScreen(
                                state = state,
                                order = order,
                                onSort = topModel::sortBy,
                                archiveEdge = archiveEdge,
                                onSignIn = topModel::signIn,
                                // Refresh koskee auki olevaa listaa, ei aina Top Pagea.
                                onRefresh = {
                                    when (segment) {
                                        MatchesSegment.YourTurn -> topModel.refresh()
                                        MatchesSegment.Tournaments -> tournamentsModel.refresh()
                                    }
                                },
                                segment = segment,
                                onSelectSegment = { chosen ->
                                    segment = chosen
                                    // Laiska haku: turnaussivu haetaan vasta avattaessa ja
                                    // vain kerran, kuten se haettiin loungessakin.
                                    if (chosen == MatchesSegment.Tournaments) {
                                        tournamentsModel.onOpened()
                                    }
                                },
                                tournaments = tournaments,
                                remembered = remembered,
                                opponentsRefresh = opponentsRefresh,
                                // Ratas vie asetuksiin omalle reitilleen, ei välilehdelle:
                                // paluu palaa tähän ruutuun eikä Info-listaan (4.9.2026).
                                onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                                // Reitti rakennetaan sivun omasta linkistä, ja ilman linkkiä
                                // sitä ei rakenneta lainkaan: rivi ei ole klikattava.
                                onOpenMatch = { match ->
                                    BoardRoute.of(match)?.let(navController::navigate)
                                },
                                pending = pending,
                                // Reitti syntyy tallennetusta polusta sellaisenaan, eli
                                // samasta linkistä jolla ruutu alun perin avattiin. Mitään ei
                                // koota kannasta.
                                onOpenPending = { navController.navigate(BoardRoute.of(it)) },
                                // Ilmoituksen nappi valitsee arkistovälilehden samalla
                                // navigoinnilla kuin palkki, jottei pinoon synny toista
                                // muotoa samasta siirtymästä.
                                onOpenPage = { navController.navigate(PageRoute.of(it)) },
                                onOpenMessages = { openTab(DgTab.Messages) },
                                markCount = markCount,
                                onOpenMarks = { navController.navigate(MARKS_ROUTE) },
                            )
                        }

                        composable(MARKS_ROUTE) {
                            // Lista luetaan kannasta suoraan ilman näkymämallia, kuten
                            // jono ja ottelumuisti otteluluettelossa: ruudulla ei ole
                            // tilaa jota pitäisi hakea tai säilyttää, vain kaksi virtaa
                            // ja yksi poisto.
                            val marks by remember { container.marks.observeAll() }
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                            val remembered by remember { container.matchMemory.observeAll() }
                                .collectAsStateWithLifecycle(initialValue = emptyMap())
                            val scope = rememberCoroutineScope()
                            MarksScreen(
                                marks = marks,
                                remembered = remembered,
                                onRemove = { id -> scope.launch { container.marks.remove(id) } },
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(MESSAGES_ROUTE) {
                            // Jonon polku luetaan Top Pagen omasta linkistä ja annetaan
                            // suoraan, ei reitin yli. Reitti vaatisi koodauksen ja purun, ja
                            // tämä on kuluttava osoite: väärin purettu polku ei tuottaisi
                            // virhettä vaan peruuttamattoman teon.
                            //
                            // Virtana eikä arvona: kertakuva olisi ollut null aina kun ruutu
                            // avataan ennen kuin luettelo on latautunut, ja ruutu olisi
                            // sanonut ettei mitään odota. Ks. MessagesViewModel.
                            val queuePathUpstream = remember(topModel) {
                                topModel.state.map {
                                    (it as? TopUiState.Loaded)?.page?.messageQueuePath
                                }
                            }

                            val messagesModel: MessagesViewModel = viewModel(
                                factory = MessagesViewModel.Factory(
                                    pages = container.pages,
                                    forms = container.forms,
                                    archive = container.messages,
                                    queuePathUpstream = queuePathUpstream,
                                    // Lauta jonosta pyyhkii luettelon ilmoituksen (18.9.2026).
                                    onMessagesDrained = topModel::clearMessageNotice,
                                    // Oma nimi lähetetyn viestin lähettäjäksi. Luetaan
                                    // kutsuhetkellä eikä oteta talteen, jotta uloskirjautuminen
                                    // näkyy tässäkin.
                                    self = { container.credentials.get()?.login },
                                    filterStore = container.messageFilter,
                                    phraseBook = container.phraseBook,
                                    reminders = container.reminders,
                                )
                            )
                            val queue by messagesModel.queue.collectAsStateWithLifecycle()
                            val importState by messagesModel.import.collectAsStateWithLifecycle()
                            val phrases by messagesModel.phrases.collectAsStateWithLifecycle()
                            val messages by messagesModel.messages.collectAsStateWithLifecycle()
                            val path by messagesModel.queuePath.collectAsStateWithLifecycle()
                            val draft by messagesModel.draft.collectAsStateWithLifecycle()
                            val reply by messagesModel.reply.collectAsStateWithLifecycle()
                            val invitationAction by messagesModel.invitationAction.collectAsStateWithLifecycle()
                            val opponents by messagesModel.opponents.collectAsStateWithLifecycle()
                            val filter by messagesModel.filter.collectAsStateWithLifecycle()
                            val replyTarget by messagesModel.replyTarget.collectAsStateWithLifecycle()
                            val quote by messagesModel.quote.collectAsStateWithLifecycle()

                            // Otteluluettelon nykyiset ottelut lajimerkinnän linkkiä varten.
                            // Sama tila jota luettelo itse näyttää, ei erillistä hakua.
                            val topStateForLinks by topModel.state.collectAsStateWithLifecycle()
                            val currentMatches =
                                (topStateForLinks as? TopUiState.Loaded)?.page?.matches
                                    ?: emptyList()

                            // Sama käsittely kuin kahdessa muussa ruudussa ja samasta syystä:
                            // tunnusvaraston omistaa TopViewModel.
                            SignOutOnExpiry(queue, container, topModel, navController)

                            // Kirjoitus on tässä eikä näkymämallissa, koska se vaatii
                            // `ContentResolver`in eikä `MessagesViewModel` tunne Androidia
                            // enempää kuin muutkaan mallit. Malli antaa vain sen mitä
                            // kirjoitetaan (`exportJson`), tämä lohko päättää minne.
                            val exportContext = LocalContext.current
                            val exportScope = rememberCoroutineScope()
                            val exportLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.CreateDocument("application/json"),
                            ) { uri ->
                                if (uri != null) {
                                    exportScope.launch {
                                        val json = messagesModel.exportJson()
                                        withContext(Dispatchers.IO) {
                                            exportContext.contentResolver.openOutputStream(uri)
                                                ?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                                        }
                                    }
                                }
                            }

                            // Tuonti (16.9.2026): peilikuva viennistä. Lukeminen on tässä
                            // samasta syystä kuin kirjoitus, ja malli saa vain tekstin
                            // (`importJson`). Tyyppisuodatin on väljä, koska tiedostonhallinta
                            // nimeää `.json`in vaihtelevasti (`application/json`, `text/plain`
                            // tai ei mitään), ja tiukka suodatin piilottaisi oman tiedoston
                            // valitsimesta ilman virhettä. Malli tunnistaa muodon itse.
                            val importLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.OpenDocument(),
                            ) { uri ->
                                if (uri != null) {
                                    exportScope.launch {
                                        val text = withContext(Dispatchers.IO) {
                                            runCatching {
                                                exportContext.contentResolver.openInputStream(uri)
                                                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                                            }.getOrNull()
                                        }
                                        messagesModel.importJson(text ?: "")
                                    }
                                }
                            }

                            // **Näkyvä varmuuskopio** (Tommin valinta 1.9.2026,
                            // `docs/AVOIMET.md`). Sama kirjoitus kuin `Export`illa ja sama
                            // syy sille että se on täällä eikä näkymämallissa: tarvitaan
                            // `ContentResolver`, jota mikään malli ei tunne.
                            //
                            // Ero `Export`iin on kohde ja toisto: vienti kysyy paikan joka
                            // kerta, tämä kysyy sen kerran ja kirjoittaa samaan tiedostoon
                            // uudestaan. Siksi osoite otetaan pysyväksi
                            // (`takePersistableUriPermission`), ja siksi kirjoitus on `wt`
                            // eikä `w`: ilman katkaisua lyhyempi arkisto jättäisi edellisen
                            // kopion hännän tiedoston loppuun, eli rikkinäisen JSONin joka
                            // näyttää onnistuneelta.
                            var backupTarget by remember { mutableStateOf(container.backup.target()) }
                            var backupSaved by remember { mutableStateOf(container.backup.lastSaved()) }
                            var backupFailed by remember { mutableStateOf(false) }

                            val writeBackup: (String) -> Unit = { uri ->
                                exportScope.launch {
                                    val json = messagesModel.exportJson()
                                    val ok = withContext(Dispatchers.IO) {
                                        runCatching {
                                            exportContext.contentResolver
                                                .openOutputStream(Uri.parse(uri), "wt")
                                                ?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                                                ?: error("no stream")
                                        }.isSuccess
                                    }
                                    if (ok) {
                                        val now = System.currentTimeMillis()
                                        container.backup.saveTime(now)
                                        backupSaved = now
                                        backupFailed = false
                                    } else {
                                        // Osoite unohdetaan, koska sama epäonnistuminen
                                        // toistuisi joka avauksella eikä ruutu voi korjata
                                        // sitä itse. Aika jää näkyviin: se kertoo mistä
                                        // hetkestä kopio on, ja se on nyt tärkeämpi tieto
                                        // kuin ennen vikaa. Rivin teksti on
                                        // `backup_failed_saved`, ja `BackupStore.forget`
                                        // säilyttää ajan myös käynnistyksen yli.
                                        container.backup.forget()
                                        backupTarget = null
                                        backupFailed = true
                                    }
                                }
                            }

                            val backupLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.CreateDocument("application/json"),
                            ) { uri ->
                                if (uri != null) {
                                    // Pysyvä oikeus on koko toiston ehto. Ilman tätä osoite
                                    // toimisi tämän kerran ja epäonnistuisi seuraavalla
                                    // käynnistyksellä — hiljainen vika juuri siinä
                                    // ominaisuudessa joka on olemassa hiljaisuutta vastaan.
                                    runCatching {
                                        contentResolver.takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                        )
                                    }
                                    container.backup.saveTarget(uri.toString())
                                    backupTarget = uri.toString()
                                    backupSaved = null
                                    backupFailed = false
                                    writeBackup(uri.toString())
                                }
                            }

                            // Automaattinen kopio kerran vuorokaudessa, ruudun avautuessa.
                            // Sovellus ei aja mitään taustalla eikä tämä muuta sitä: ehto on
                            // `BackupStatus.isDue`, ja se laukeaa vain kun viestiruutu on
                            // auki. Rajaus on kirjattu sinne, ei tähän.
                            //
                            // Avain on `messages`, joten kopio syntyy myös silloin kun
                            // arkisto kasvaa ruudun ollessa auki — mutta vain jos vuorokausi
                            // on täynnä, joten jokainen uusi viesti ei kirjoita tiedostoa.
                            LaunchedEffect(backupTarget, messages) {
                                val target = backupTarget ?: return@LaunchedEffect
                                if (BackupStatus.isDue(backupSaved, System.currentTimeMillis())) {
                                    writeBackup(target)
                                }
                            }

                            MessagesScreen(
                                messages = messages,
                                queue = queue,
                                queuePath = path,
                                onFetchNext = messagesModel::fetchNext,
                                invitationAction = invitationAction,
                                onAcceptInvitation = messagesModel::acceptInvitation,
                                onDeclineInvitation = messagesModel::declineInvitation,
                                onCounterOffer = messagesModel::counterOffer,
                                onDismissInvitationAction = messagesModel::dismissInvitationAction,
                                draft = draft,
                                reply = reply,
                                onDraftChange = messagesModel::draftChanged,
                                onSendReply = messagesModel::sendReply,
                                phrases = phrases,
                                onAddPhrase = messagesModel::addPhrase,
                                onRemovePhrase = messagesModel::removePhrase,
                                quote = quote,
                                onQuoteChange = messagesModel::quoteChanged,
                                opponents = opponents,
                                filter = filter,
                                onFilterChange = messagesModel::filterChanged,
                                replyTargetId = replyTarget?.id,
                                onMessageClick = messagesModel::openReply,
                                // Lajimerkinnän ottelulinkki: aktiivinen ottelu avautuu
                                // lautana otteluluettelon omasta linkistä (sama
                                // `BoardRoute.of(match)` kuin luettelon rivillä), muu
                                // ottelu siirtohistoriana. Review-polku luetaan luettelosta
                                // kun ottelu on siellä; **listalta pudonneelle ottelulle
                                // polku kootaan tunnisteesta**, ja se on nimetty poikkeus
                                // sääntöön "osoite luetaan": muoto `/bg/game/<id>/0/list`
                                // on mitattu neljästä eri lähteestä (Top Page, profiili,
                                // turnaus, pelaajan turnauslista), haku on kuluttamaton
                                // GET, ja väärä osoite tuottaa porautumisruudun
                                // "ei tunnettu sivu" -tilan eikä tekoa. Ilman kokoamista
                                // päättyneellä ottelulla ei olisi linkkiä lainkaan, ja juuri
                                // sen Tommi pyysi 30.8.2026.
                                onOpenMatch = { id ->
                                    val match = currentMatches.firstOrNull { it.id.value == id }
                                    val route = match?.let { BoardRoute.of(it) }
                                        ?: PageRoute.of(
                                            match?.reviewPath ?: "/bg/game/$id/0/list"
                                        )
                                    navController.navigate(route)
                                },
                                onExport = if (messages.isEmpty()) {
                                    null
                                } else {
                                    {
                                        exportLauncher.launch(
                                            ArchiveExportFilename.of(System.currentTimeMillis()),
                                        )
                                    }
                                },
                                onImport = { importLauncher.launch(arrayOf("*/*")) },
                                importState = importState,
                                onDismissImport = messagesModel::importDismissed,
                                backup = BackupUiState.of(
                                    target = backupTarget,
                                    lastSaved = backupSaved,
                                    failedNow = backupFailed,
                                ),
                                // Nimi on päivätön, toisin kuin viennissä: tämä on **se**
                                // ajantasainen kopio eikä yksi monista, ja päivätty nimi
                                // vanhenisi heti ensimmäisessä ylikirjoituksessa.
                                onSetUpBackup = { backupLauncher.launch("dg-archive.json") },
                                onBackupNow = { backupTarget?.let(writeBackup) },
                            )
                        }

                        composable(LOUNGE_ROUTE) {
                            // Loungen polku Top Pagen navigointipalkista, virtana samasta
                            // syystä kuin jonon polku yllä: kertakuva olisi väärä nolla
                            // ennen luettelon latautumista.
                            val loungePathUpstream = remember(topModel) {
                                topModel.state.map {
                                    (it as? TopUiState.Loaded)?.page?.loungePath
                                }
                            }
                            val loungeModel: LoungeViewModel = viewModel(
                                factory = LoungeViewModel.Factory(
                                    pages = container.pages,
                                    // Kolmas ruutu joka saa tämän: kutsun hyväksyminen on
                                    // teko sivustolla. Ks. FormSender ja docs/UI.md.
                                    forms = container.forms,
                                    loungePathUpstream = loungePathUpstream,
                                )
                            )
                            val state by loungeModel.state.collectAsStateWithLifecycle()
                            val join by loungeModel.join.collectAsStateWithLifecycle()
                            val signup by loungeModel.signup.collectAsStateWithLifecycle()
                            val players by loungeModel.players.collectAsStateWithLifecycle()
                            val search by loungeModel.search.collectAsStateWithLifecycle()

                            // Sama käsittely kuin muissa ruuduissa ja samasta syystä:
                            // tunnusvaraston omistaa TopViewModel.
                            SignOutOnExpiry(state, container, topModel, navController)

                            LoungeScreen(
                                state = state,
                                join = join,
                                signup = signup,
                                players = players,
                                // Refresh koskee sitä listaa joka on auki.
                                onRefresh = loungeModel::refresh,
                                onRefreshPlayers = loungeModel::refreshPlayers,
                                onOpenPlayers = loungeModel::openPlayers,
                                onOpenPlayerListLink = loungeModel::openPlayerListLink,
                                // Porautuminen sivun omalla polulla: tarjoajan profiili,
                                // pelaajalistan rivi tai turnaussivu.
                                onOpenPage = { navController.navigate(PageRoute.of(it)) },
                                onJoin = loungeModel::join,
                                onDismissJoinResult = loungeModel::clearJoinResult,
                                // Ilmoittautuminen ja sen peruutus rivin omilla linkeillä
                                // (3.9.2026, kaanonimuutos SUBSTANSSI kohtaan 52).
                                onSignUp = loungeModel::signUp,
                                onCancelSignup = loungeModel::cancelSignup,
                                onDismissSignupResult = loungeModel::clearSignupResult,
                                // Pelaajahaku loungen omalla lomakkeella (3.9.2026). Lomake
                                // luetaan tilasta, joten kenttä on ruudulla vain kun sivu antoi sen.
                                searchAvailable = (state as? LoungeUiState.Loaded)?.page?.searchForm != null,
                                search = search,
                                onSearch = loungeModel::searchPlayers,
                                onClearSearch = loungeModel::clearSearch,
                            )
                        }

                        composable(DISCUSSION_ROUTE) {
                            val forumPathUpstream = remember(topModel) {
                                topModel.state.map {
                                    (it as? TopUiState.Loaded)?.page?.forumPath
                                }
                            }

                            val discussionModel: DiscussionViewModel = viewModel(
                                factory = DiscussionViewModel.Factory(
                                    pages = container.pages,
                                    // Palstan kirjoituslomakkeet, Tommin päätös 3.9.2026.
                                    // Ks. FormSender ja docs/UI.md.
                                    forms = container.forms,
                                    forumPathUpstream = forumPathUpstream,
                                )
                            )
                            val state by discussionModel.state.collectAsStateWithLifecycle()
                            val thread by discussionModel.thread.collectAsStateWithLifecycle()
                            val archive by discussionModel.archive.collectAsStateWithLifecycle()
                            val compose by discussionModel.compose.collectAsStateWithLifecycle()
                            val post by discussionModel.post.collectAsStateWithLifecycle()
                            val draftTitle by discussionModel.draftTitle.collectAsStateWithLifecycle()
                            val draftComment by discussionModel.draftComment.collectAsStateWithLifecycle()
                            val boards by discussionModel.boards.collectAsStateWithLifecycle()
                            val selectedBoard by discussionModel.selectedBoard
                                .collectAsStateWithLifecycle()

                            SignOutOnExpiry(state, container, topModel, navController)

                            DiscussionScreen(
                                state = state,
                                thread = thread,
                                archive = archive,
                                boards = boards,
                                selectedBoard = selectedBoard,
                                onRefresh = discussionModel::refresh,
                                onSelectBoard = discussionModel::selectBoard,
                                onOpenThread = discussionModel::openThread,
                                onCloseThread = discussionModel::closeThread,
                                onOpenArchive = discussionModel::openArchive,
                                onOpenMonth = discussionModel::openMonth,
                                onCloseArchive = discussionModel::closeArchive,
                                compose = compose,
                                post = post,
                                draftTitle = draftTitle,
                                draftComment = draftComment,
                                onDraftTitleChange = discussionModel::setDraftTitle,
                                onDraftCommentChange = discussionModel::setDraftComment,
                                onOpenNewThread = discussionModel::openNewThread,
                                onOpenComment = discussionModel::openComment,
                                onCloseCompose = discussionModel::closeCompose,
                                onPost = discussionModel::post,
                                onDismissPost = discussionModel::dismissPost,
                            )
                        }

                        /*
                         * Info-välilehti: listarivit ja niiden takana olevat ruudut samassa
                         * reitissä.
                         *
                         * **Porautuminen tapahtuu paikallaan eikä omalle reitilleen**
                         * (`docs/UI.md` 26.8.2026). Syy on välilehtipalkki: omalla
                         * reitillään `tabFor` palauttaisi nullin, palkki katoaisi ja
                         * asetusruutu näyttäisi laudalta. Sama ratkaisu kuin Discussionin
                         * ketjulla, ja `BackHandler` on siellä samasta syystä: se sulkee
                         * ensin sub-ruudun ja vasta sen jälkeen poistutaan välilehdeltä.
                         *
                         * Valinta on `rememberSaveable`issa, jotta se säilyy sekä
                         * välilehtien vaihdon (`restoreState`) että prosessin tapon yli.
                         */
                        composable(INFO_ROUTE) {
                            var openSection by rememberSaveable {
                                mutableStateOf<InfoSection?>(null)
                            }
                            BackHandler(enabled = openSection != null) { openSection = null }

                            when (openSection) {
                                null -> InfoScreen(
                                    onOpen = { openSection = it },
                                    // Sama kolmen rivin käsittely kuin katkenneella
                                    // istunnolla (`SignOutOnExpiry`): tunnukset pois, keksi
                                    // pois ja takaisin edelliseen ruutuun. Pelkkä tunnusten
                                    // poisto jättäisi istunnon voimaan, jolloin sovellus
                                    // näyttäisi yhä dataa uloskirjautuneelle käyttäjälle.
                                    onSignOut = {
                                        container.signOut()
                                        topModel.signOut()
                                        navController.popBackStack()
                                    },
                                )

                                // Manuaali ensin, koska se ei tarvitse mitään: ei
                                // näkymämallia, ei verkkoa eikä istuntoa. Sisältö on
                                // sovelluksen omaa tekstiä.
                                InfoSection.Help -> HelpScreen(
                                    onBack = { openSection = null },
                                )

                                // Katkohistoria: kaksi virtaa kannasta, ei näkymämallia
                                // eikä verkkoa. Ottelumuisti nimeää ottelun kuten
                                // merkityillä asemilla.
                                InfoSection.Drops -> {
                                    val drops by remember { container.drops.observeNewestFirst() }
                                        .collectAsStateWithLifecycle(initialValue = emptyList())
                                    val remembered by remember { container.matchMemory.observeAll() }
                                        .collectAsStateWithLifecycle(initialValue = emptyMap())
                                    DropsScreen(
                                        drops = drops,
                                        remembered = remembered,
                                        onBack = { openSection = null },
                                    )
                                }

                                /*
                                 * Sivuston omat sivut samalla porautumisruudulla kuin
                                 * profiilit ja turnaukset: laji luetaan vastauksesta
                                 * (`PageViewModel`), ja polku on vakio koska nämä ovat
                                 * sivuston juuren staattisia sivuja eikä sivulta
                                 * luettuja linkkejä. Ruutu pysyy välilehden sisällä;
                                 * vastauksen omat linkit (joita näillä sivuilla ei
                                 * porautumismielessä ole) menisivät porautumisreitille.
                                 */
                                InfoSection.SiteHelp, InfoSection.SiteLinks, InfoSection.Resign -> {
                                    val path = when (openSection) {
                                        InfoSection.SiteHelp -> "/help"
                                        // Top Pagen navigointipalkin oma linkki, vakiopolku.
                                        InfoSection.Resign -> "/bg/resign"
                                        else -> "/links.html"
                                    }
                                    val pageModel: PageViewModel = viewModel(
                                        // Avain on polku samasta syystä kuin
                                        // porautumisreitillä: rivin vaihto on eri malli.
                                        key = path,
                                        factory = PageViewModel.Factory(
                                            pages = container.pages,
                                            forms = container.forms,
                                            archive = container.messages,
                                            path = path,
                                            self = { container.credentials.get()?.login },
                                            marks = container.marks,
                                        ),
                                    )
                                    val state by pageModel.state.collectAsStateWithLifecycle()
                                    val resignState by pageModel.resign.collectAsStateWithLifecycle()

                                    SignOutOnExpiry(state, container, topModel, navController)

                                    PageScreen(
                                        state = state,
                                        onBack = { openSection = null },
                                        onRefresh = pageModel::refresh,
                                        selfName = pageModel.selfName,
                                        onOpenPath = {
                                            navController.navigate(PageRoute.of(it))
                                        },
                                        resign = resignState,
                                        onResign = pageModel::resign,
                                        onDismissResign = pageModel::dismissResign,
                                    )
                                }

                            }
                        }

                        /*
                         * Asetuslomake omana reittinään (Tommin päätös 4.9.2026, `docs/UI.md`).
                         *
                         * Sisäänkäyntejä on kaksi, otteluluettelon ratas ja Infon
                         * `Settings`-rivi, ja se pakotti reitin: paikallaan porautuva ruutu
                         * olisi vienyt paluun Info-listaan myös silloin kun lähdettiin
                         * otteluluettelosta. Palkkia ei piirretä samoin kuin laudalla, ja
                         * `onBack` on siksi `popBackStack` eikä valinnan nollaus.
                         */
                        composable(SETTINGS_ROUTE) {
                            // Näkymämalli on reitin tasolla eikä activityn: asetusruutu
                            // haetaan silloin kun se avataan, eikä se ole tilaa jota
                            // otteluluettelo tarvitsisi. Poistuminen vapauttaa sen.
                            val settingsModel: SettingsViewModel = viewModel(
                                factory = SettingsViewModel.Factory(
                                    pages = container.pages,
                                    forms = container.forms,
                                    baseline = container.settingsBaseline,
                                    siteSettings = container.siteSettings,
                                )
                            )
                            val state by settingsModel.state.collectAsStateWithLifecycle()
                            val saveResult by settingsModel.saveResult
                                .collectAsStateWithLifecycle()

                            // Sama käsittely kuin lautanäkymässä ja samasta syystä:
                            // tunnusvaraston omistaa TopViewModel, joten katkennut istunto
                            // hoidetaan yhdessä paikassa eikä kahdessa.
                            SignOutOnExpiry(state, container, topModel, navController)

                            // Tyylivalinta luetaan ja kirjoitetaan tässä eikä näkymämallissa:
                            // se on laitteen oma asetus eikä sivuston tilaa, joten se ei
                            // kuulu malliin joka kuvaa mitä sivustolla lukee.
                            var boardStyle by remember { mutableStateOf(container.boardStyle.get()) }
                            var diceStyle by remember { mutableStateOf(container.diceStyle.get()) }
                            var scoreStyle by remember { mutableStateOf(container.scoreStyle.get()) }
                            var busyStyle by remember { mutableStateOf(container.busyStyle.get()) }
                            var playForcedSteps by remember { mutableStateOf(container.forcedSteps.get()) }
                            var playGreedyBearoff by remember { mutableStateOf(container.greedyBearoff.get()) }
                            var diceSubmitTap by remember { mutableStateOf(container.diceSubmit.get()) }
                            var diceSwapTap by remember { mutableStateOf(container.diceSwap.get()) }

                            // Kansiovalitsin taustakuville: sama mekanismi kuin varmuuskopion
                            // tiedostolla (pysyvä uri-oikeus), mutta kansio ja lukuoikeus.
                            // Valitsin näyttää kaikki kansion antavat tarjoajat, myös pilven
                            // jos sellainen on; suositus on laitteen oma kansio, koska pilvi
                            // hakisi kuvat verkosta joka avauksella (Tommin huoli 15.9.2026).
                            val wallpaperLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.OpenDocumentTree(),
                            ) { uri ->
                                if (uri != null) {
                                    runCatching {
                                        contentResolver.takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                        )
                                    }
                                    container.wallpaper.saveFolder(uri.toString())
                                    wallpaperFolder = uri.toString()
                                }
                            }

                            SettingsScreen(
                                state = state,
                                saveResult = saveResult,
                                boardStyle = boardStyle,
                                onBoardStyleChange = { style ->
                                    container.boardStyle.save(style)
                                    boardStyle = style
                                },
                                diceStyle = diceStyle,
                                onDiceStyleChange = { style ->
                                    container.diceStyle.save(style)
                                    diceStyle = style
                                },
                                scoreStyle = scoreStyle,
                                onScoreStyleChange = { style ->
                                    container.scoreStyle.save(style)
                                    scoreStyle = style
                                },
                                busyStyle = busyStyle,
                                onBusyStyleChange = { style ->
                                    container.busyStyle.save(style)
                                    busyStyle = style
                                },
                                playForcedSteps = playForcedSteps,
                                onPlayForcedStepsChange = { enabled ->
                                    container.forcedSteps.save(enabled)
                                    playForcedSteps = enabled
                                },
                                playGreedyBearoff = playGreedyBearoff,
                                onPlayGreedyBearoffChange = { enabled ->
                                    container.greedyBearoff.save(enabled)
                                    playGreedyBearoff = enabled
                                },
                                diceSubmitTap = diceSubmitTap,
                                onDiceSubmitTapChange = { enabled ->
                                    container.diceSubmit.save(enabled)
                                    diceSubmitTap = enabled
                                },
                                diceSwapTap = diceSwapTap,
                                onDiceSwapTapChange = { enabled ->
                                    container.diceSwap.save(enabled)
                                    diceSwapTap = enabled
                                },
                                portraitLock = portraitLock,
                                onPortraitLockChange = { enabled ->
                                    container.portraitLock.save(enabled)
                                    portraitLock = enabled
                                },
                                leftHanded = leftHanded,
                                onLeftHandedChange = { enabled ->
                                    container.handedness.save(enabled)
                                    leftHanded = enabled
                                },
                                sumiE = sumiE,
                                onSumiEChange = { enabled ->
                                    container.skyTheme.save(enabled)
                                    sumiE = enabled
                                },
                                pattern = pattern,
                                onPatternChange = { enabled ->
                                    container.skyTheme.savePattern(enabled)
                                    pattern = enabled
                                },
                                wallpaperFolder = wallpaperFolder,
                                onChooseWallpaperFolder = { wallpaperLauncher.launch(null) },
                                onClearWallpaperFolder = {
                                    container.wallpaper.forget()
                                    wallpaperFolder = null
                                },
                                // Luetaan piirtohetkellä eikä virtana: lähtötila
                                // kirjoitetaan kerran eikä se muutu ruudun eliniän aikana.
                                storedBaseline = settingsModel.storedBaseline,
                                onRefresh = settingsModel::refresh,
                                onSave = settingsModel::save,
                                onDismissResult = settingsModel::clearSaveResult,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        /*
                         * Porautumiset: pelaajaprofiili, turnaussivu ja pelaajan
                         * turnauslistat. Yksi reitti, koska sivun laji luetaan vasta
                         * vastauksesta (ks. `PageViewModel`).
                         *
                         * Reitti on välilehtien ulkopuolella samoin kuin lauta, joten
                         * palkkia ei piirretä ja paluu on järjestelmän ele tai otsikon
                         * oma nappi.
                         */
                        composable(
                            route = PageRoute.PATTERN,
                            arguments = listOf(
                                navArgument(PageRoute.ARG_PATH) { type = NavType.StringType },
                            ),
                        ) { entry ->
                            // Argumentti tulee purettuna, samoin kuin lautareitillä.
                            val path = entry.arguments?.getString(PageRoute.ARG_PATH).orEmpty()

                            val pageModel: PageViewModel = viewModel(
                                // Avain on polku: sama ruutu eri polulla on eri malli,
                                // muuten profiilista profiiliin porautuminen näyttäisi
                                // edellisen sivun.
                                key = path,
                                factory = PageViewModel.Factory(
                                    pages = container.pages,
                                    forms = container.forms,
                                    archive = container.messages,
                                    path = path,
                                    // Oma nimi lähetetyn viestin lähettäjäksi, luettuna
                                    // kutsuhetkellä kuten viestiruudulla.
                                    self = { container.credentials.get()?.login },
                                    marks = container.marks,
                                ),
                            )
                            val state by pageModel.state.collectAsStateWithLifecycle()

                            SignOutOnExpiry(state, container, topModel, navController)

                            val draft by pageModel.draft.collectAsStateWithLifecycle()
                            val sendState by pageModel.send.collectAsStateWithLifecycle()
                            val actionState by pageModel.action.collectAsStateWithLifecycle()

                            // Haettu .mat-tiedosto menee jakovalikkoon kerran: Ready
                            // kuitataan heti kun valikko on avattu, ettei ruudun
                            // uudelleenpiirto avaa sitä toiseen kertaan.
                            val exportState by pageModel.export.collectAsStateWithLifecycle()
                            val context = LocalContext.current
                            val chooserTitle = stringResource(R.string.page_export_chooser)
                            LaunchedEffect(exportState) {
                                val ready = exportState as? ExportUiState.Ready
                                    ?: return@LaunchedEffect
                                MatchExportShare.share(context, ready.fileName, ready.text, chooserTitle)
                                pageModel.exportConsumed()
                            }

                            // Sama fraasilista kuin viestiruudussa ja laudalla (3.9.2026).
                            val phrases by container.phraseBook.phrases.collectAsStateWithLifecycle()
                            // Omien turnausten lukema muistista pelaajasivun riville
                            // (10.9.2026), ei uutta hakua.
                            val ownTournamentCount by container.ownTournamentCount
                                .collectAsStateWithLifecycle()
                            PageScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onRefresh = pageModel::refresh,
                                selfName = pageModel.selfName,
                                ownTournamentCount = ownTournamentCount,
                                // Sivun oma linkki sellaisenaan, samalle reitille.
                                onOpenPath = { navController.navigate(PageRoute.of(it)) },
                                draft = draft,
                                send = sendState,
                                phrases = phrases,
                                onAddPhrase = container.phraseBook::add,
                                onRemovePhrase = container.phraseBook::remove,
                                onDraftChange = pageModel::onDraftChange,
                                onSend = pageModel::send,
                                export = exportState,
                                onExport = pageModel::export,
                                action = actionState,
                                onInvite = pageModel::invite,
                                onIgnore = pageModel::ignore,
                                onDismissAction = pageModel::dismissAction,
                            )
                        }

                        composable(
                            route = BoardRoute.PATTERN,
                            arguments = listOf(
                                navArgument(BoardRoute.ARG_PATH) { type = NavType.StringType },
                                navArgument(BoardRoute.ARG_MATCH) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                            ),
                        ) { entry ->
                            // Argumentit tulevat purettuina, koska kirjasto purkaa reitin
                            // itse. Koodauksen häviöttömyyttä väittää DestinationsTest;
                            // tässä ei siis pureta uudelleen, koska kaksi purkua rikkoisi
                            // prosenttimerkin sisältävän arvon.
                            val playPath = entry.arguments?.getString(BoardRoute.ARG_PATH).orEmpty()
                            val matchId = entry.arguments?.getString(BoardRoute.ARG_MATCH)

                            val boardModel: BoardViewModel = viewModel(
                                factory = BoardViewModel.Factory(
                                    pages = container.pages,
                                    // Ainoa ruutu joka saa tämän. Ks. `FormSender`.
                                    forms = container.forms,
                                    // Ainoa ruutu joka kirjoittaa jonoon, samasta syystä
                                    // kuin `forms`: vain täällä tehdään tekoja.
                                    queue = container.actions,
                                    // Ainoa ruutu jolla peli on tiedossa, eli ainoa
                                    // paikka jossa muistutuksella on koti. Ei kyky
                                    // sivustolla, ks. `ReminderBook`.
                                    reminderBook = container.reminders,
                                    // Ainoa ruutu joka kirjoittaa merkkejä, samasta syystä
                                    // kuin muistutukset: vain täällä peli ja siirtonumero
                                    // ovat tiedossa. Ks. `MarkBook`.
                                    markBook = container.marks,
                                    // Toinen ruutu joka kirjoittaa arkistoon, ja syy on
                                    // sama kuin viestiruudulla: ottelun chat näkyy vain
                                    // siirron jälkeisellä sivulla eikä sivusto säilytä sitä.
                                    archive = container.messages,
                                    // Laitteen asetus luetaan laudan avautuessa, ei nyt:
                                    // funktio eikä arvo, ks. `BoardViewModel`.
                                    playForcedSteps = { container.forcedSteps.get() },
                                    playGreedyBearoff = { container.greedyBearoff.get() },
                                    self = { container.credentials.get()?.login },
                                    playPath = playPath,
                                    expectedMatchId = matchId?.let(::MatchId),
                                    // Luettelon kierrokset tunnisteella, ks. `ListedRounds`.
                                    listedRound = container.listedRounds::of,
                                    // Luettu lauta ottelumuistiin, ks. `MatchMemory`.
                                    matchMemory = container.matchMemory,
                                    // Katko kesken teon historiaan, ks. `DropLog`.
                                    dropLog = container.drops,
                                )
                            )
                            val state by boardModel.state.collectAsStateWithLifecycle()
                            val pending by boardModel.pending.collectAsStateWithLifecycle()
                            val note by boardModel.note.collectAsStateWithLifecycle()
                            val reminders by boardModel.reminders.collectAsStateWithLifecycle()
                            val marks by boardModel.marks.collectAsStateWithLifecycle()

                            // Katkennut istunto vie takaisin listalle ja tyhjentää
                            // istunnon kokonaan. Tunnusten poisto on TopViewModelin työ,
                            // koska se omistaa tunnusvaraston.
                            SignOutOnExpiry(state, container, topModel, navController)
                            LaunchedEffect(state) {
                                val current = state
                                // Sivuston oma `To Top` vie Top Pagelle, ja lautanäkymä
                                // tunnistaa sen jo. Väliruutu ei kuitenkaan ole poistuminen:
                                // nappi lupaa viedä ylös, joten ruutu suljetaan ja luettelo
                                // haetaan tuoreena. Juuri päättynyt ottelu on silloin poissa
                                // listalta, mikä on koko napin tarkoitus. Haku on `/bg/top`,
                                // joka ei kuluta jonoa (`docs/KOHDE.md`), toisin kuin
                                // `/bg/nextgame`.
                                //
                                // ~~Tuore haku eikä vastauksen kierrätys: sivu on jo haettu
                                // kerran, mutta sen välittäminen näkymämallista toiseen
                                // tarkoittaisi toista reittiä samaan tilaan. Yksi kuluttamaton
                                // pyyntö on halvempi kuin toinen totuus luettelosta.~~
                                // **Purettu 4.9.2026 illalla mitatun hinnan takia**, ks. alla:
                                // toinen reitti ei syntynyt, koska luettelo lukee tavut itse.
                                if (current is BoardUiState.NotABoard &&
                                    current.kind is NotABoardKind.TopPage
                                ) {
                                    // **Odotus tapahtuu kohteessa eikä lähteessä**, ja se on
                                    // mitattu päätös (4.9.2026 illalla, nauhoitus tabletilta).
                                    // Väliversiossa paluu odotti listaa laudan reitillä, ja
                                    // odotusruutu joutui teeskentelemään otteluluetteloa:
                                    // erot löytyivät yksi kerrallaan (sisältö, suunta,
                                    // välilehtipalkki, otsikkorivi) ja päälle tuli vielä
                                    // navigoinnin ristihäivytys. Nyt paluu tehdään heti ja
                                    // luettelo näyttää oman odotustilansa omassa kehyksessään.
                                    //
                                    // **Ja odotusta ei yleensä ole lainkaan**, koska sivu on
                                    // jo laitteella: sivusto vastasi tähän pyyntöön
                                    // otteluluettelolla, ja `adopt` lukee juuri ne tavut.
                                    // Aiemmin ne heitettiin pois ja sama sivu haettiin
                                    // uudelleen, mikä oli nauhalta mitattu 1,4 sekuntia.
                                    // Toista reittiä samaan tilaan ei silti synny: tavut
                                    // kulkevat luettelon omalla lukijalla samaan tilaan kuin
                                    // sen oma haku, ja lauta vain välittää sivun jota se ei
                                    // itse osaa lukea.
                                    //
                                    // Haku jää varareitiksi sille tapaukselle jossa tavut
                                    // eivät kelpaakaan luetteloksi. Se ei ole odotettu
                                    // tilanne, koska `DgPages.isTopPage` on jo sanonut kyllä,
                                    // mutta tunnistus ja jäsennys ovat eri asioita eikä
                                    // ruudulle jätetä vanhaa listaa sen varaan.
                                    //
                                    // Suuntaan ei kosketa täällä. Sen omistaja on yllä oleva
                                    // ohjaus, joka asettaa arvon vasta kun kohde on voimassa;
                                    // vapautus kesken paluun kokeiltiin samana iltana ja
                                    // purettiin, koska se on se kilpailu josta ohjaus
                                    // varoittaa 24.8.2026 mitattuna.
                                    val adopted = topModel.adopt(current.kind.html)
                                    if (!adopted) topModel.refresh(matchesKnownStale = true)
                                    navController.popBackStack()
                                }
                            }

                            // **Teon jälkeen paluu hakee luettelon uudestaan** (Tommin päätös
                            // 16.9.2026, `docs/UI.md`). Luettelo elää activityn tasolla eikä
                            // päivity itsestään, ja 16.9. nauhalta mitattu vanhentunut
                            // `Your turn` -rivi johti lukutilan lautaan ja turhaan Skip
                            // Gameen. Katsomiskäynti ei hae, joten säännön hinta ei muutu.
                            // Sama polku molemmille paluille: järjestelmän eleelle
                            // (`BackHandler`) ja ruudun omalle napille (`onBack`), jotta
                            // tieto ei riipu siitä kummalla poistuttiin. Sivuston omalla
                            // Top Page -vastauksella poistuva `adopt`-reitti yllä ei kulje
                            // tästä, eikä se hae, koska sivu on jo laitteella.
                            val leaveBoard = {
                                if (boardModel.actedOnSite) topModel.refresh(matchesKnownStale = true)
                                navController.popBackStack()
                                Unit
                            }
                            BackHandler(onBack = leaveBoard)

                            // Paluu on 9.8.2026 alkaen järjestelmän oma ele eikä ruudun nappi
                            // (Tommin valinta), joten `navController` hoitaa sen itse.
                            // **Poikkeus 1.9.2026: lukukelvoton sivu saa näkyvän paluun**,
                            // koska ruutu piilottaa palkit eikä elettä silloin näy — ja
                            // koska laudan tilan säästäminen ei ole peruste ruudulla jolla
                            // ei ole lautaa. Ehto on `BoardScreen`in puolella, ks. `onBack`.
                            WakeOnReconnect(container.network, boardModel::onNetworkAvailable)
                            // Sama fraasilista kuin viestiruudussa (Tommin tilaus 3.9.2026).
                            // Omistaja on sovellus eikä kumpikaan näkymämalli, ks. `PhraseBook`.
                            val phrases by container.phraseBook.phrases.collectAsStateWithLifecycle()
                            BoardScreen(
                                state = state,
                                pending = pending,
                                note = note,
                                onRefresh = boardModel::refresh,
                                onFollow = boardModel::follow,
                                onPress = boardModel::press,
                                phrases = phrases,
                                onAddPhrase = container.phraseBook::add,
                                onRemovePhrase = container.phraseBook::remove,
                                // Luettava sivu avataan sivunlukijalla, samalla reitillä kuin
                                // muualtakin. Lautanäkymä ei osaa siirtolistaa eikä turnausta,
                                // ja lukijat niille ovat olleet olemassa jo ennen tätä.
                                onOpenPage = { navController.navigate(PageRoute.of(it)) },
                                onSendChat = boardModel::sendChat,
                                onBack = leaveBoard,
                                reminders = reminders,
                                onAddReminder = boardModel::addReminder,
                                onRemoveReminder = boardModel::removeReminder,
                                marks = marks,
                                onMarkPosition = boardModel::markPosition,
                                onRemoveMark = boardModel::removeMark,
                                // Luetaan piirtohetkellä eikä oteta talteen: tyyli vaihdetaan
                                // asetusruudussa, ja lautaruutu koostuu aina uudestaan siihen
                                // tullessa, joten tuore luku riittää ilman virtaa.
                                style = container.boardStyle.get(),
                                siteSettings = container.siteSettings.get(),
                                diceStyle = container.diceStyle.get(),
                                scoreStyle = container.scoreStyle.get(),
                                busyStyle = container.busyStyle.get(),
                                // Luetaan piirtohetkellä kuten kaksi ylläolevaa: laudalle
                                // tullaan asetusruudun jälkeen, joten muutos on voimassa
                                // seuraavassa ottelussa ilman erillistä virtaa.
                                diceSubmitTap = container.diceSubmit.get(),
                                diceSwapTap = container.diceSwap.get(),
                                leftHanded = leftHanded,
                            )
                        }
                    }
                    }
                    }
                    }
                }
            }
        }
    }
}

/**
 * Kutsuu [onAvailable]a aina kun yhteys palaa, niin kauan kuin ruutu on näkyvissä.
 *
 * **Sidottu koostumukseen eikä sovellukseen**, ja se on tämän ratkaisun kantava valinta.
 * Reittinsä jättänyt ruutu ei koostu, joten se ei myöskään herää: taustalle jäänyt näkymä
 * ei hae mitään, ja pyyntöjä syntyy vain sinne mihin käyttäjä katsoo. Se vastaa
 * `docs/AVOIMET.md`:n avoimeen puoleen, ja vastaus on kapein mahdollinen.
 *
 * Haku tulee heti eikä viiveellä, koska ehto on jo tiukka: näkymämalli hakee vain jos
 * edellinen haku kaatui verkkoon. Viive suojaisi tahdilta jota tässä ei synny.
 */
/**
 * Ruudun suunta yhtenä sääntönä, jotta se on sama molemmissa kutsupaikoissa.
 *
 * Lauta on vaakaan aina, muut ruudut seuraavat pystylukkoa (`docs/UI.md`, Tommin päätös
 * 2.9.2026).
 *
 * Sääntö nostettiin omakseen 4.9.2026 illalla, kun paluu laudalta asetti suunnan ennen ruudun
 * vaihtoa. Se asetus purettiin samana iltana mittauksen perusteella, joten kutsupaikkoja on
 * taas yksi. Funktio jäi silti, koska se on nyt kirjoitettuna se mitä ennen luettiin
 * `when`-lohkosta kutsupaikan sisältä: **kaksi ehtoa, kolme arvoa, ja lauta ensin.**
 */
private fun orientationFor(onBoard: Boolean, portraitLock: Boolean): Int = when {
    onBoard -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    portraitLock -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

@Composable
private fun WakeOnReconnect(network: NetworkAvailability, onAvailable: () -> Unit) {
    // Avaimena vain verkko: kuuntelija rekisteröidään kerran eikä jokaisella
    // uudelleenkoostumuksella. Kutsuttava luetaan tuoreena, jottei vanha viittaus jää
    // elämään pitkäikäiseen kuuntelijaan.
    val latest by rememberUpdatedState(onAvailable)
    LaunchedEffect(network) {
        network.availability().collect { latest() }
    }
}

/**
 * Sovellus palasi etualalle otteluluettelon ollessa auki: luettelo haetaan uudestaan
 * (Tommin tilaus 18.9.2026, DG Mobile tekee saman). Päätös siitä haetaanko on
 * [TopViewModel.onForeground]in, tämä vain kertoo hetken.
 *
 * **Ensimmäinen `ON_RESUME` ohitetaan.** Tarkkailija saa sen heti rekisteröityessään, koska
 * aktiviteetti on jo `RESUMED` kun ruutu koostuu, ja se osuisi sekä käynnistykseen (haku on
 * jo käynnissä `init`istä) että jokaiseen paluuseen laudalta tai välilehdeltä, jossa ruutu
 * koostuu uudestaan. Katsomiskäynti laudalla ei saa hakea (`docs/UI.md`), ja teon jälkeinen
 * paluu hakee jo omaa reittiään. Vasta seuraava `ON_RESUME` on paluu toisesta sovelluksesta
 * tai lukitulta näytöltä, ja siitä haetaan.
 */
@Composable
private fun RefreshOnForeground(onForeground: () -> Unit) {
    val latest by rememberUpdatedState(onForeground)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        var first = true
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            if (first) first = false else latest()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

/**
 * Katkennut istunto: tunnukset pois, keksi pois ja takaisin edelliseen ruutuun.
 *
 * **Yksi määritelmä kuuden kopion tilalle (1.9.2026, H1).** Sama kolmen rivin käsittely oli
 * kirjoitettu erikseen jokaiseen ruutuun, kukin oman hierarkiansa `SessionExpired`ille.
 * Ehto luetaan nyt merkinnästä [SessionExpiredState], joten ruutu kertoo vain **mitä tilaa**
 * seurataan eikä sitä mitä katkeaminen tarkoittaa.
 *
 * Tunnusten poisto on `TopViewModel`in työ, koska se omistaa tunnusvaraston; keksin poisto
 * on containerin, koska pelkkä tunnusten poisto jättäisi istunnon voimaan.
 */
@Composable
private fun SignOutOnExpiry(
    state: Any?,
    container: AppContainer,
    topModel: TopViewModel,
    navController: NavHostController,
) {
    LaunchedEffect(state) {
        if (state is SessionExpiredState) {
            container.signOut()
            topModel.signOut()
            navController.popBackStack()
        }
    }
}
