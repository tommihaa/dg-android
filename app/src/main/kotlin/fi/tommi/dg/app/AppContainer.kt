package fi.tommi.dg.app

import android.content.Context
import fi.tommi.dg.app.session.BackupStore
import fi.tommi.dg.app.session.BoardStyleStore
import fi.tommi.dg.app.session.BusyStyleStore
import fi.tommi.dg.app.session.DiceStyleStore
import fi.tommi.dg.app.session.SharedPrefsBusyStyle
import fi.tommi.dg.app.session.DiceSubmitStore
import fi.tommi.dg.app.session.DiceSwapStore
import fi.tommi.dg.app.session.SharedPrefsDiceSubmit
import fi.tommi.dg.app.ui.OwnTournamentCount
import fi.tommi.dg.app.session.SharedPrefsDiceSwap
import fi.tommi.dg.app.session.ForcedStepsStore
import fi.tommi.dg.app.session.GreedyBearoffStore
import fi.tommi.dg.app.session.ListedRounds
import fi.tommi.dg.app.session.HandednessStore
import fi.tommi.dg.app.session.PortraitLockStore
import fi.tommi.dg.app.session.SharedPrefsForcedSteps
import fi.tommi.dg.app.session.SharedPrefsGreedyBearoff
import fi.tommi.dg.app.session.SharedPrefsHandedness
import fi.tommi.dg.app.session.SharedPrefsPortraitLock
import fi.tommi.dg.app.session.SharedPrefsSkyTheme
import fi.tommi.dg.app.session.SkyThemeStore
import fi.tommi.dg.app.session.WallpaperStore
import fi.tommi.dg.app.session.SharedPrefsWallpaper
import fi.tommi.dg.app.session.MatchOrderStore
import fi.tommi.dg.app.session.MessageFilterStore
import fi.tommi.dg.app.session.PhraseBook
import fi.tommi.dg.app.session.PhraseStore
import fi.tommi.dg.app.session.SharedPrefsPhrases
import fi.tommi.dg.app.session.SharedPrefsMessageFilter
import fi.tommi.dg.app.session.ScoreStyleStore
import fi.tommi.dg.app.session.SharedPrefsDiceStyle
import fi.tommi.dg.app.session.SharedPrefsScoreStyle
import fi.tommi.dg.app.session.CredentialsStore
import fi.tommi.dg.app.session.SettingsBaseline
import fi.tommi.dg.app.session.SharedPrefsBoardStyle
import fi.tommi.dg.app.session.SharedPrefsMatchOrder
import fi.tommi.dg.app.session.SharedPrefsBackup
import fi.tommi.dg.app.session.SharedPrefsCredentialsStore
import fi.tommi.dg.app.session.SharedPrefsSettingsBaseline
import fi.tommi.dg.app.session.SharedPrefsSiteSettings
import fi.tommi.dg.app.session.SiteSettingsRefresher
import fi.tommi.dg.app.session.SiteSettingsStore
import fi.tommi.dg.data.ActionQueue
import fi.tommi.dg.data.DgData
import fi.tommi.dg.data.DropLog
import fi.tommi.dg.data.MatchMemory
import fi.tommi.dg.data.MessageArchive
import fi.tommi.dg.data.MarkBook
import fi.tommi.dg.data.ReminderBook
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.net.DgClient
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.net.FileCookieStore
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Sivun haku yhtenä funktiona.
 *
 * Tämä sauma on olemassa testausta varten: [DgClient] on luokka eikä rajapinta, ja
 * näkymämalli halutaan ajaa ilman verkkoa. Samalla se rajaa mitä UI:lla on lupa tehdä,
 * eli hakea sivu polulla, ei rakentaa pyyntöjä itse.
 */
fun interface PageFetcher {
    fun fetch(path: String): DgResponse
}

/**
 * Käyttäjän teon lähetys yhtenä funktiona.
 *
 * Rinnakkainen [PageFetcher]ille ja erillään siitä tarkoituksella: näkymämalli joka ei saa
 * tätä konstruktoriinsa ei voi tehdä sivustolla mitään, ja se on luettavissa signatuurista.
 * `TopViewModel` ei saa tätä.
 *
 * **`SettingsViewModel` sai tämän 25.8.2026, ja luettelo muuttui toisen kerran.** Sopimusmuutos
 * kirjattiin `docs/UI.md`:hen ja vahvistettiin Tommilla ennen koodia, sama järjestys kuin
 * viestiruudulla. Asetusruutu kirjoittaa, joten sen on saatava tämä. Kirjoitettava kohde on
 * silti vakio: asetuslomakkeen `FormSubmission`in voi tuottaa vain `SettingsPage.update`, joten
 * laajennus koskee sitä kuka saa painaa lähetystä eikä sitä minne voi kirjoittaa.
 *
 * **`MessagesViewModel` sai tämän 22.8.2026, ja luettelo muuttui eikä periaate.** Aiemmin
 * tässä luki myös se, ja lause oli tosi niin kauan kuin viestiruutu vain haki. Vastauksen
 * lähettäminen on peruuttamaton teko, joten mallin on saatava tämä; periaate säilyy, koska
 * konstruktori kertoo kyvystä jatkossakin totuuden, nyt toiseen suuntaan. Sopimusmuutos
 * kirjattiin ennen koodia ja vahvistettiin Tommilla, ks. `docs/UI.md`.
 *
 * Parametri on [FormSubmission] eikä polku, joten tähänkään ei voi antaa itse koottua
 * osoitetta. Se on takeen kantava puoli ja se pysyy koskemattomana: arvon voi tuottaa vain
 * `BoardForm.press` tai `ReplyForm.write`, eli sivun oman napin painaminen tai sivun oman
 * lomakkeen täyttäminen. Ks. tyypin oma perustelu `core-domain`issa.
 */
fun interface FormSender {
    fun send(submission: FormSubmission): DgResponse
}

/**
 * Käsin koottu riippuvuuspuu. Sovellus on pieni, ja yksi luettava tiedosto kertoo
 * kytkennät suoremmin kuin generoitu graafi.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val credentials: CredentialsStore = SharedPrefsCredentialsStore(appContext)

    /**
     * Asetusten lähtötila ennen ensimmäistä kirjoitusta. Ks. [SettingsBaseline].
     *
     * **Tätä ei tyhjennetä uloskirjautumisessa**, toisin kuin tunnukset ja keksit. Lähtötila
     * on varmuuskopio siitä mitä sivustolla luki, ja juuri uloskirjautuminen on hetki jolloin
     * sen menettäminen olisi peruuttamatonta.
     */
    val settingsBaseline: SettingsBaseline by lazy { SharedPrefsSettingsBaseline(appContext) }

    /**
     * Sivuston lautaan vaikuttavat asetukset laitteella. Kirjoittajia on kaksi ja molemmat
     * kirjoittavat vain onnistuneesta luvusta: [siteSettingsRefresher] avauksessa ja
     * `SettingsViewModel` asetusruudussa. Lautanäkymä lukee. Ks. `docs/ASETUKSET.md` luku 2.
     */
    val siteSettings: SiteSettingsStore by lazy { SharedPrefsSiteSettings(appContext) }

    /** Laudan tyyli, laitteen oma valinta. Ei lähde koskaan verkkoon. */
    val boardStyle: BoardStyleStore by lazy { SharedPrefsBoardStyle(appContext) }

    /** Noppien esitys, samaa lajia kuin [boardStyle] mutta oma kytkin (Tommi 27.8.2026). */
    val diceStyle: DiceStyleStore by lazy { SharedPrefsDiceStyle(appContext) }

    /** Pisteiden esitys, samaa lajia kuin [diceStyle]. */
    val scoreStyle: ScoreStyleStore by lazy { SharedPrefsScoreStyle(appContext) }

    /** Odotuksen ilmaisin nappien paikalla, samaa lajia kuin [diceStyle] (Tommi 18.9.2026). */
    val busyStyle: BusyStyleStore by lazy { SharedPrefsBusyStyle(appContext) }

    /**
     * Pakollisten askelten esipoiminta, laitteen oma kytkin kuten [diceStyle] mutta eri
     * lajia sisällöltään: tämä ei muuta esitystä vaan sitä mitä lauta tekee pelaajan
     * puolesta. Oletus pois, ks. [ForcedStepsStore].
     */
    val forcedSteps: ForcedStepsStore by lazy { SharedPrefsForcedSteps(appContext) }

    /** Ahne uloskanto ilman kontaktia, samaa lajia kuin [forcedSteps]. Oletus pois. */
    val greedyBearoff: GreedyBearoffStore by lazy { SharedPrefsGreedyBearoff(appContext) }

    /**
     * Otteluluettelon kierrokset laudan ottelutiedoille (`Round 3/5`). Prosessin muisti eikä
     * säilö: luettelo kirjoittaa, lauta lukee, ks. [ListedRounds].
     */
    val listedRounds: ListedRounds by lazy { ListedRounds() }

    /**
     * Noppien painallus lähettää täyden vuoron, ks. [DiceSubmitStore]. Samaa lajia kuin
     * [forcedSteps]: ei muuta esitystä vaan sitä mitä painallus laudalla tekee. Oletus pois.
     */
    val diceSubmit: DiceSubmitStore by lazy { SharedPrefsDiceSubmit(appContext) }

    /** Noppien painallus vaihtaa järjestyksen, samaa lajia kuin [diceSubmit]. Oletus pois. */
    val diceSwap: DiceSwapStore by lazy { SharedPrefsDiceSwap(appContext) }

    /** Lukuruutujen pystylukko, oletus päällä. Lauta on aina vaakaan, ks. [PortraitLockStore]. */
    val portraitLock: PortraitLockStore by lazy { SharedPrefsPortraitLock(appContext) }

    /** Kätisyys, oletus oikea. Sivupaneeli menee vastakkaiselle puolelle, ks. [HandednessStore]. */
    val handedness: HandednessStore by lazy { SharedPrefsHandedness(appContext) }

    /** Taustakuvion sumi-e-tila, oletus pois. Ks. [SkyThemeStore]. */
    val skyTheme: SkyThemeStore by lazy { SharedPrefsSkyTheme(appContext) }

    /** Taustakuvien kansio tyhjään tilaan, oletus ei mitään. Ks. [WallpaperStore]. */
    val wallpaper: WallpaperStore by lazy { SharedPrefsWallpaper(appContext) }

    /** Otteluluettelon järjestysvalinta, samaa lajia kuin [boardStyle]. */
    val matchOrder: MatchOrderStore by lazy { SharedPrefsMatchOrder(appContext) }

    /** Viestiruudun pelaajasuodatin, samaa lajia kuin [matchOrder]. */
    val messageFilter: MessageFilterStore by lazy { SharedPrefsMessageFilter(appContext) }

    /**
     * Fraasinapit, yksi lista koko sovellukselle: viestiruudun vastauskenttä ja laudan
     * chat-kortti lukevat samaa. Oletus on rituaalifraasit, ks. [PhraseStore].
     */
    val phraseBook: PhraseBook by lazy { PhraseBook(SharedPrefsPhrases(appContext)) }

    /**
     * Omien turnausten lukumäärä viimeksi ladatulta listalta, ks. [OwnTournamentCount].
     * Containerissa eikä näkymämallissa, koska kirjoittaja (otteluluettelon reitti) ja
     * lukija (pelaajasivun reitti) ovat eri reittejä eikä kummankaan malli näe toista.
     * Prosessin muisti riittää: lukema on korvattavissa avaamalla Tournaments-välilehti.
     */
    val ownTournamentCount = MutableStateFlow<OwnTournamentCount?>(null)

    /**
     * Arkiston näkyvän varmuuskopion kohde ja aika. Samaa lajia kuin [matchOrder]: tämän
     * laitteen oma valinta, joka ei lähde verkkoon. Ks. [BackupStore].
     */
    val backup: BackupStore by lazy { SharedPrefsBackup(appContext) }

    /** Asetusten avaushaun omistaja. `MainActivity` ajaa tämän taustalla käynnistyksessä. */
    val siteSettingsRefresher: SiteSettingsRefresher by lazy {
        SiteSettingsRefresher(pages, siteSettings)
    }

    /**
     * Viestiarkisto. Molemmat puolet käytössä 9.8.2026 alkaen: otteluluettelo näyttää
     * reunapäivän, viestiruutu listaa sisällön ja kirjoittaa jonosta haetun viestin.
     */
    val messages: MessageArchive by lazy { DgData.messageArchive(appContext) }

    /**
     * Epäonnistuneiden tekojen jono. Vain lautanäkymä **kirjoittaa** tähän, koska vain se
     * tekee sivustolla tekoja.
     *
     * **Otteluluettelo lukee sitä 10.8.2026 alkaen**, ja rajaus on siksi kirjoitusoikeudessa
     * eikä pääsyssä. Syy on mitattu: yhteydettömänä käynnistetty sovellus jää
     * otteluluetteloon eikä pääse lautaruutuun lainkaan, joten jono olisi saavuttamattomissa
     * juuri siinä tilassa jota varten se on olemassa. Lukeminen ei koske sivustoon.
     */
    val actions: ActionQueue by lazy { DgData.actionQueue(appContext) }

    /**
     * Muistutukset, eli pelaajan omat muistiinpanot pelin ajaksi. Vain lautanäkymä käyttää.
     *
     * **Tämä ei kosketa sivustoon**, joten se ei ole `pages`in eikä `forms`in kaltainen
     * kyky: muistutus on kokonaan sovelluksen omaa tietoa, ks. [ReminderBook].
     */
    val reminders: ReminderBook by lazy { DgData.reminderBook(appContext) }

    /**
     * Ottelumuisti: viimeksi nähty vastustaja ja kierros ottelun numerolla. Otteluluettelo
     * ja lauta kirjoittavat, turnauslista lukee. **Ei kosketa sivustoon**, ks. [MatchMemory].
     */
    val matchMemory: MatchMemory by lazy { DgData.matchMemory(appContext) }

    /**
     * Merkityt asemat ottelun jälkeistä analyysia varten (Tommin toive 15.9.2026). Lauta
     * kirjoittaa, otteluluettelon linkin takana oleva lista lukee. **Ei kosketa
     * sivustoon**, ks. [MarkBook].
     */
    val marks: MarkBook by lazy { DgData.markBook(appContext) }

    /**
     * Katkohistoria: milloin `Board not confirmed` tuli ja miksi (Tommin tilaus 18.9.2026).
     * Lauta kirjoittaa katkon hetkellä, Info-välilehden rivi lukee. **Ei kosketa
     * sivustoon**, ks. [DropLog].
     */
    val drops: DropLog by lazy { DgData.dropLog(appContext) }

    private val client: DgClient by lazy {
        DgClient(
            credentials = credentials,
            // Keksi säilyy prosessin kuoleman yli. Ilman tätä istunto katoaisi joka
            // käynnistyksessä, ja juuri se on se kipu jonka takia sovellus on olemassa.
            cookieStore = FileCookieStore(File(appContext.filesDir, "dg_cookies.txt")),
        )
    }

    val pages: PageFetcher get() = PageFetcher(client::fetch)

    /**
     * Ilmoitus yhteyden palaamisesta. Ks. [NetworkAvailability].
     *
     * Konteksti annetaan tässä eikä ruudulla, jotta kuuntelija ei ole sidottu activityyn:
     * `applicationContext` elää kierron yli, ja rekisteröinnin elinkaari on sen mukana
     * kutsuvan koroutiinin varassa eikä ruudun.
     */
    val network: NetworkAvailability by lazy { androidNetworkAvailability(appContext) }

    /** Lautanäkymä ja viestiruutu saavat tämän. Ks. [FormSender]. */
    val forms: FormSender get() = FormSender(client::send)

    fun signOut() {
        credentials.clear()
        File(appContext.filesDir, "dg_cookies.txt").delete()
    }
}
