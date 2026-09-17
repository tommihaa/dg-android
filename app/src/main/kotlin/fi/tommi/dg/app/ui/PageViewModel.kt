package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.data.MarkBook
import fi.tommi.dg.data.MessageArchive
import fi.tommi.dg.domain.EventPage
import fi.tommi.dg.domain.InviteChoice
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.ResignPage
import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.MatMatch
import fi.tommi.dg.domain.MatchExport
import fi.tommi.dg.domain.MatchLogPage
import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.domain.PlayerTournaments
import fi.tommi.dg.domain.ProfilePage
import fi.tommi.dg.domain.SgfExport
import fi.tommi.dg.domain.SiteHelpPage
import fi.tommi.dg.domain.SiteLinksPage
import fi.tommi.dg.domain.TournamentHall
import fi.tommi.dg.domain.sgfFileName
import fi.tommi.dg.domain.write
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.EventParser
import fi.tommi.dg.scrape.MatchLogParser
import fi.tommi.dg.scrape.PlayerTournamentsParser
import fi.tommi.dg.scrape.ProfileParser
import fi.tommi.dg.scrape.ResignParser
import fi.tommi.dg.scrape.SiteHelpParser
import fi.tommi.dg.scrape.SiteLinksParser
import fi.tommi.dg.scrape.TournamentHallParser
import fi.tommi.dg.scrape.SendResultParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mikä sivu vastauksesta tunnistettiin. Laji tulee sisällöstä eikä pyydetystä osoitteesta,
 * ks. [PageViewModel].
 */
sealed interface PageContent {

    data class Profile(val page: ProfilePage) : PageContent

    data class Event(val page: EventPage) : PageContent

    /** `/bg/userevent/<id>` tai `/bg/userwins/<id>`; kumpi, näkyy riveistä. */
    data class Tournaments(val page: PlayerTournaments) : PageContent

    /** Sivuston oma Help `/help`, Info-välilehden rivi (29.8.2026). */
    data class SiteHelp(val page: SiteHelpPage) : PageContent

    /** Sivuston linkkisivu `/links.html`, Info-välilehden rivi (29.8.2026). */
    data class SiteLinks(val page: SiteLinksPage) : PageContent

    /**
     * Ottelun siirtohistoria `/bg/game/<id>/<n>/list` (30.8.2026, Tommin pyyntö:
     * päättyneen ottelun linkki viestiruudusta tarvitsi kohteen). Ks. [MatchLogPage].
     */
    data class MatchLog(val page: MatchLogPage) : PageContent

    /**
     * Tournament Hall `/bg/thall`, koko sivuston turnauslista (3.9.2026). Oli Loungen
     * segmentti 27.8.–3.9.2026; nyt porautumissivu Loungen omien turnausten alta.
     */
    data class Hall(val page: TournamentHall) : PageContent

    /**
     * Luovutussivu `/bg/resign`, Info-välilehden rivi (3.9.2026, Tommin tilaus). Sivun oma
     * lomake sellaisenaan: rivit, varmistusruutu ja nappi sivun sanoin. Ks. [ResignPage].
     */
    data class Resign(val page: ResignPage) : PageContent
}

/** Mittaamattoman vastaussivun lause otetaan mukaan vain jos se on lauseen mittainen. */
private const val NOTICE_MAX = 160

/** Profiilin kaksi tekoa viestin lisäksi, 3.9.2026 alkaen. */
enum class ProfileAction { INVITE, IGNORE }

/**
 * Luovutuksen tila. [Done] kantaa kadonneiden rivien määrän, koska se on se todiste jolla
 * onnistuminen luettiin.
 *
 * **Vastaussivu mitattiin 4.9.2026 illalla, eikä se muuttanut tätä** (`docs/KOHDE.md`).
 * Sivu on oma sivunsa (`DailyGammon: Post Resignation`) ja sanoo luvun omin sanoin
 * (`1 match resigned.`), mutta siinä ei ole lomaketta, joten ruudulle tarvittava tuore
 * luettelo on haettava joka tapauksessa. Kadonnut rivi on siis yhä se todiste jolla
 * ruutu päivittyy, ja sivuston oma lukema on avoin parannus eikä puute.
 */
sealed interface ResignUiState {

    data object Idle : ResignUiState

    data object Sending : ResignUiState

    /**
     * Luovutus onnistui. [count] on luovutettujen määrä ja [notice] sivuston oma lause.
     *
     * **Kaksi todistetta samasta teosta, ja ne ovat eri lähteistä.** [notice] on olemassa
     * kun vastaus oli sivuston oma kuittaussivu, joka sanoo luvun omin sanoin (mitattu
     * 4.9.2026 illalla). Ilman sitä luku on kadonneiden rivien määrä, eli sovelluksen oma
     * päättely tuoreesta listasta. Ruutu kertoo kumpi on kyseessä, koska ne eivät ole
     * yhtä vahvoja.
     */
    data class Done(val count: Int, val notice: String? = null) : ResignUiState

    /** Vastaus tuli, mutta sivu ei näytä muutosta tai sitä ei saatu luettua. */
    data object Unconfirmed : ResignUiState

    data class Failed(val reason: Failure) : ResignUiState

    data object SessionExpired : ResignUiState
}

/**
 * Kutsun ja sivuutuksen tila. Lopputuloksia on useampi kuin kaksi, koska teot ovat eri
 * tavalla mitattuja: sivuutuksen onnistuminen näkyy profiilin napista ([Done]), kutsun
 * kuittaussivu tunnistetaan otsikostaan ([Sent], mitattu 4.9.2026), tuntematon vastaus on
 * [Answered], ja [Unconfirmed] on se tilanne jossa sivu ei näytä muutosta. Mitään ei
 * lähetetä uudelleen missään näistä.
 *
 * Sivuutuksen molemmat kuittaussivut on nyt mitattu (`ignore_done.html` 4.9.2026 aamulla,
 * `ignore_undone.html` samana iltana), eivätkä ne muuta tätä. Otsikko ei kerro lopputulosta
 * kummassakaan suunnassa, eikä vastaus kanna profiilin nappia, joten sivuutus lukee
 * onnistumisen yhä profiilin napista. Mittaus vahvisti reitin sen sijaan että olisi
 * korvannut sen: ero kutsuun on sivun rakenteessa eikä valinnassa.
 */
sealed interface ProfileActionUiState {

    data object Idle : ProfileActionUiState

    data class Sending(val action: ProfileAction) : ProfileActionUiState

    /** Profiilin nappi vaihtui. [label] on napin uusi teksti sivun sanoin. */
    data class Done(val action: ProfileAction, val player: String, val label: String?) : ProfileActionUiState

    /** DailyGammon vastasi, mutta vastaussivua ei lueta. [notice] on sivun oma lause jos lyhyt. */
    data class Answered(val action: ProfileAction, val player: String, val notice: String?) : ProfileActionUiState

    /**
     * Sivusto kuittasi teon omalla kuittaussivullaan (4.9.2026 alkaen kutsulla,
     * `DgPages.isInviteSentPage`). Ero [Answered]iin on todisteen laji: siellä palvelin
     * vastasi jotain, tässä se sanoi mitä tapahtui. [notice] on sivun oma lause.
     */
    data class Sent(val action: ProfileAction, val player: String, val notice: String?) : ProfileActionUiState

    /** Vastaus tuli, mutta profiili ei näytä muutosta. Ei tulkita kumpaankaan suuntaan. */
    data class Unconfirmed(val action: ProfileAction, val player: String) : ProfileActionUiState

    /** Verkko tai palvelin. Mitään ei lähtenyt perille. */
    data class Failed(val action: ProfileAction, val reason: Failure) : ProfileActionUiState

    data object SessionExpired : ProfileActionUiState
}

/**
 * Päättyneen ottelun `.mat`-tiedoston haku jakoa varten (Tommin valinta 2.9.2026, kaanonin
 * kohta 24 avattu samana iltana). Tila on erillään sivun tilasta samasta syystä kuin
 * [ReplyUiState]: sivu pysyy ruudulla riippumatta siitä miten haku päättyi.
 */
/**
 * Kumpi tiedosto jaetaan. `.mat` on sivuston oma ja kelpaa kaikille ohjelmille; `.sgf` on
 * GNU Backgammonin muoto ja ainoa jossa merkitty asema kulkee mukana (Tommin päätös
 * 15.9.2026, `SgfExport`).
 */
enum class ExportFormat { MAT, SGF }

sealed interface ExportUiState {

    data object Idle : ExportUiState

    data object Fetching : ExportUiState

    /**
     * Tiedosto on kädessä ja jakovalikko avataan. Ruutu kuittaa [PageViewModel.exportConsumed]illa.
     * [fileName] ja [text] ovat se mikä jaetaan: `.mat` sellaisenaan tai siitä kirjoitettu
     * `.sgf` merkkeineen ([ExportFormat]).
     */
    data class Ready(val export: MatchExport, val fileName: String, val text: String) : ExportUiState

    /** 200 OK, mutta runko on sivu eikä tiedosto. Mitään ei jaettu. */
    data object NotAnExport : ExportUiState

    /** `.mat` saatiin, mutta sen rivit eivät jäsentyneet siirroiksi, joten `.sgf`:ää ei kirjoitettu. */
    data object NotConvertible : ExportUiState

    /**
     * `.mat` jäsentyi, mutta jokin peli alkaa muusta kuin tavallisesta aloitusasemasta
     * (nackgammon tai double repeatin toistettu peli, `MatGame.setup`). `.sgf` kirjoitettaisiin
     * tavallisesta aloitusasemasta ja GNU näyttäisi laittomia siirtoja, joten sitä ei
     * kirjoiteta (mitattu 16.9.2026, `docs/AVOIMET.md` › `.sgf`-vienti).
     */
    data object SetupPosition : ExportUiState

    data class Failed(val reason: Failure) : ExportUiState

    data object SessionExpired : ExportUiState
}

sealed interface PageUiState {

    data object Loading : PageUiState

    data class Loaded(val content: PageContent, val refreshing: Boolean = false) : PageUiState

    /** 200 OK, mutta sivu ei ole mikään niistä joita tämä ruutu osaa lukea. */
    data object NotAKnownPage : PageUiState

    data class Failed(val reason: Failure) : PageUiState

    /** Istunto ei kelvannut. Tunnusten poisto kuuluu [TopViewModel]ille. */
    data object SessionExpired : PageUiState, SessionExpiredState
}

/**
 * Porautumisruudun näkymämalli: pelaajaprofiili, turnaussivu, pelaajan turnauslistat sekä
 * sivuston oma Help ja linkkisivu (kaksi jälkimmäistä 29.8.2026, Info-välilehden rivit).
 *
 * **Yksi näkymämalli kuudelle sivulle, ja laji luetaan vastauksesta.** Se ei ole
 * laiskuutta vaan tämän projektin mitattu perusfakta: `/bg/move/`-pyyntö palautti kerran
 * 200 OK:lla sivun jolla ei ollut lautaa (`docs/KOHDE.md`), joten pyydetty osoite ei kerro
 * mitä saatiin. Kun laji luetaan `DgPages`illa, sivuston yllätys näkyy ruudulla
 * huomautuksena eikä tyhjänä listana.
 *
 * ~~**Konstruktorissa ei ole `FormSender`iä**, ja se on väite samaan tapaan kuin
 * [DiscussionViewModel]illa: yksikään näistä sivuista ei voi tehdä sivustolla mitään.~~
 * **Kumottu 29.8.2026, ja kumoaminen on tässä auki kirjoitettuna eikä poistettuna.**
 * Ruutu lähettää nyt yhden asian: pelaajan profiilin *Quick message* -lomakkeen. Väite oli
 * voimassa niin kauan kuin ruutu oli kauttaaltaan lukutilassa, ja sen katoaminen on juuri
 * se asia joka ei saa tapahtua hiljaa.
 *
 * **Yksi lähetys eikä lähetyskyky.** Ero on rakenteessa eikä kurinalaisuudessa: [send]
 * ottaa lomakkeen [PageUiState.Loaded]in profiilisisällöstä eikä parametrina, ja
 * `ReplyForm` syntyy vain jäsentimessä. Ruutu ei siis voi koota osoitetta eikä lähettää
 * sivun muita lomakkeita. Kutsulomake (`/bg/invite/new`) ja `Ignore` ovat yhä ulkona, ja
 * ne ovat ulkona rajauksena (`docs/AVOIMET.md`), eivät siksi ettei niitä osattaisi lukea.
 *
 * **Miksi juuri tämä lomake sai poikkeuksen.** Sivusto ei säilytä keskustelua, ja
 * profiilin lomake on ainoa reitti aloittaa se ilman että vastapuoli kirjoittaa ensin.
 * Lähetetty viesti menee samaan arkistoon kuin vastaus, koska kumpaakaan ei säilytetä
 * sivustolla; ilman arkistointia sovellus lähettäisi tekstiä jota se ei itsekään muista.
 *
 * Polku tulee kertakuvana eikä virtana, toisin kuin välilehdillä: se on reitin oma
 * parametri ja kutsuja on lukenut sen sivun linkistä, joten se ei voi muuttua ruudun
 * elinaikana.
 */
class PageViewModel(
    private val pages: PageFetcher,
    private val forms: FormSender,
    private val archive: MessageArchive,
    private val path: String,
    private val self: () -> String?,
    /** Merkityt asemat `.sgf`-vientiin. Pelkkää lukua. */
    private val marks: MarkBook,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow<PageUiState>(PageUiState.Loading)
    val state: StateFlow<PageUiState> = _state.asStateFlow()

    /**
     * Kirjautuneen tunnus ruudulle, joka korostaa sen turnauskaaviosta (Tommi 3.9.2026).
     * Sama lähde kuin arkistoinnin `self`, luettuna kerran: tunnus ei vaihdu ruudun eläessä,
     * koska uloskirjautuminen navigoi pois.
     */
    val selfName: String? by lazy { self() }

    /**
     * Lähetyksen lopputulos. Sama tyyppi kuin viestiruudulla, koska teko on sama teko:
     * `POST /bg/sendmsg/<id>` ja sen kuittaussivu.
     */
    private val _send = MutableStateFlow<ReplyUiState>(ReplyUiState.Idle)
    val send: StateFlow<ReplyUiState> = _send.asStateFlow()

    /**
     * Luonnos on mallissa eikä ruudussa, samasta syystä kuin viestiruudulla: epäonnistunut
     * lähetys ei saa hävittää kirjoitettua tekstiä, ja ruudun uudelleenpiirto ei ole
     * käyttäjän ele.
     */
    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _export = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val export: StateFlow<ExportUiState> = _export.asStateFlow()

    /** Kutsun ja sivuutuksen tila, yksi kerrallaan. Ks. [ProfileActionUiState]. */
    private val _action = MutableStateFlow<ProfileActionUiState>(ProfileActionUiState.Idle)
    val action: StateFlow<ProfileActionUiState> = _action.asStateFlow()

    /** Luovutuksen tila. Ks. [ResignUiState]. */
    private val _resign = MutableStateFlow<ResignUiState>(ResignUiState.Idle)
    val resign: StateFlow<ResignUiState> = _resign.asStateFlow()

    init {
        fetch()
    }

    fun onDraftChange(text: String) {
        _draft.value = text
    }

    fun refresh() = fetch()

    // Epäonnistunut päivitys saa korvata näkyvän sisällön virheruudulla: valinta eikä
    // vuoto, ks. perustelu `LoungeViewModel.read`.
    private fun fetch() {
        _state.value = when (val current = _state.value) {
            is PageUiState.Loaded -> current.copy(refreshing = true)
            else -> PageUiState.Loading
        }
        viewModelScope.launch {
            _state.value = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok -> read(response.html)
                    is DgResponse.Offline -> PageUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError -> PageUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> PageUiState.Failed(Failure.Sleeping)
                    // Tunnuksia ei poisteta täällä: CredentialsStoren omistaa TopViewModel.
                    DgResponse.AuthFailed -> PageUiState.SessionExpired
                }
            }
        }
    }

    /**
     * Tunnistus järjestyksessä. Predikaatit ovat toisensa poissulkevia mitatuilla sivuilla
     * (profiilin otsikko, turnauksen säännöt, listojen otsikot), joten järjestys ei ratkaise
     * mitään; se on kirjoitettu vain siksi että jokin järjestys on pakko olla.
     */
    private fun read(html: String): PageUiState =
        READERS.firstNotNullOfOrNull { lue -> lue(html) }
            ?.let { PageUiState.Loaded(it) }
            ?: PageUiState.NotAKnownPage

    /**
     * Lähettää viestin pelaajan profiilin omalla lomakkeella. **Peruuttamaton teko
     * oikealle ihmiselle**, ja ehdot ovat samat neljä kuin viestiruudun vastauksella:
     *
     * 1. **Lomake tulee tilasta, ei parametrina.** Lähettää voi vain sille pelaajalle jonka
     *    profiili on ruudulla, koska `ReplyForm` syntyy vain jäsentimessä ja elää vain
     *    ladatussa profiilissa. Muu sisältö (turnaus, Help, linkkisivu) ei kanna lomaketta,
     *    joten ruudun laji portittaa tämän itsestään.
     * 2. **Osoitetta ei koota.** `ReplyForm.write` palauttaa `FormSubmission`in, ja tyhjä
     *    teksti tuottaa nullin.
     * 3. **Yksi kerrallaan**, ja se koskee profiilin kaikkia lähetyksiä eikä vain tätä
     *    nappia: [busy] on sama vartija jota `invite` ja `ignore` kysyvät. Ehto oli tässä
     *    3.9.–4.9.2026 kirjoitettuna pelkkänä `_send`in tarkistuksena, jolloin kutsu lähti
     *    kesken kutsulomakkeen lähetyksen mutta ei toisin päin. Vartijoita oli siis kaksi
     *    ja ne olivat eri mieltä, eikä erolle ollut syytä.
     * 4. **Onnistuminen luetaan sivulta.** Pelkkä `Ok` ei riitä, koska palvelin vastaa
     *    200:lla myös katkenneella istunnolla. Vasta kuittaussivu kirjoittaa arkistoon.
     */
    fun send() {
        if (busy()) return
        val profile = currentProfile() ?: return
        val form = profile.messageForm ?: return
        val text = _draft.value
        val submission = form.write(text) ?: return

        _send.value = ReplyUiState.Sending
        viewModelScope.launch {
            _send.value = withContext(io) {
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> confirm(response.html, text, profile)
                    is DgResponse.Offline -> ReplyUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError -> ReplyUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> ReplyUiState.Failed(Failure.Sleeping)
                    // Kuljetus ei lähetä uudelleen automaattisesti, koska lähetys ei ole
                    // idempotentti. Teksti jää kenttään ja käyttäjä päättää itse.
                    DgResponse.AuthFailed -> ReplyUiState.SessionExpired
                }
            }
        }
    }

    /**
     * Kuittaussivu vastaanotettuna: oma viesti arkistoon, tai ei mitään.
     *
     * Luonnos tyhjennetään **vain onnistuessa**, koska epäonnistuneessa lähetyksessä
     * kentän teksti on ainoa kopio siitä mitä käyttäjä kirjoitti.
     */
    private suspend fun confirm(html: String, text: String, profile: ProfilePage): ReplyUiState {
        if (!DgPages.isMessageSentPage(html)) return ReplyUiState.Unconfirmed

        archive.archive(
            outgoingMessage(
                // Pikaviesti ei kuulu mihinkään otteluun.
                matchId = null,
                // Vastaanottaja on profiilin kohde, eli se sivu jolta lomake luettiin.
                // Sivu on kuitenkin se joka tietää totuuden: kuittaus nimeää saajan
                // linkkinä, ja se säilyy alla `rawHeader`issa sanatarkasti.
                opponent = profile.player.name,
                self = self(),
                body = text,
                // Sivun oma lause sellaisenaan, esim. `Your message has been sent to <nimi>`.
                rawHeader = SendResultParser.notice(html).orEmpty(),
                // Profiilin lomakkeella lähetetty viesti on pikaviesti, ja se on mitattu
                // eikä oletettu: Tommin koe 22.8.2026 lähetti tällä lomakkeella, ja
                // vastaanottajan jono luokitteli tuloksen `DailyGammon Quick Message`ksi
                // (ks. `MessageSource.QUICK_MESSAGE`).
                source = MessageSource.QUICK_MESSAGE,
                sentAtEpochMillis = now(),
                // Aloitus eikä vastaus: tämä on se ero jonka vuoksi koko kohta on
                // olemassa, eikä sitä saa liittää mihinkään aiempaan viestiin.
                replyTo = null,
            ),
            account = self(),
        )
        _draft.value = ""
        return ReplyUiState.Sent
    }

    /**
     * Lähettää ottelukutsun profiilin omalla lomakkeella (rajaus purettu 3.9.2026,
     * `docs/UI.md`). Samat portit kuin [send]illä: lomake tulee ladatusta profiilista, arvot
     * ovat sivun omia vaihtoehtoja (`InviteForm.write` palauttaa muuten nullin), yksi teko
     * kerrallaan, eikä kuljetus toista POSTia.
     *
     * **Vastaussivua ei ole mitattu**, joten onnistumista ei lueta siitä. Tila on
     * [ProfileActionUiState.Answered]: palvelin vastasi, ja sivun oma lause otetaan mukaan
     * jos se on lyhyt. Profiili luetaan vastauksesta tai haetaan uudelleen, jotta ruutu ei
     * jää vanhaan. Ensimmäinen oikea kutsu ajetaan proxyn kanssa, ja tunnistus tarkentuu
     * siitä, sama tie kuin turnauksen Sign Upilla.
     */
    fun invite(choice: InviteChoice) {
        if (busy()) return
        val page = currentProfile() ?: return
        val form = page.inviteForm ?: return
        val submission = form.write(choice) ?: return

        _action.value = ProfileActionUiState.Sending(ProfileAction.INVITE)
        viewModelScope.launch {
            _action.value = withContext(io) {
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> {
                        // Lause luetaan vain vastaussivulta joka ei ole profiili: profiililla
                        // ei ole lausetta, ja uudelleen haettu profiili ei ole vastaus.
                        val answered = ProfileParser.parse(response.html)
                        val notice = if (answered == null) {
                            SendResultParser.notice(response.html)?.takeIf { it.length <= NOTICE_MAX }
                        } else {
                            null
                        }
                        reload(answered)
                        // **Kuittaussivu mitattiin 4.9.2026**, ja siihen asti tässä oli vain
                        // `Answered`: palvelin vastasi, eikä sovellus tiennyt mitä. Nyt sivu
                        // on tunnistettavissa otsikostaan, joten ruutu voi sanoa että kutsu
                        // lähti. `Answered` jää voimaan tuntemattomalle vastaukselle.
                        val nimi = page.player.displayName
                        if (DgPages.isInviteSentPage(response.html)) {
                            ProfileActionUiState.Sent(ProfileAction.INVITE, nimi, notice)
                        } else {
                            ProfileActionUiState.Answered(ProfileAction.INVITE, nimi, notice)
                        }
                    }
                    is DgResponse.Offline -> ProfileActionUiState.Failed(ProfileAction.INVITE, Failure.Offline)
                    is DgResponse.ServerError ->
                        ProfileActionUiState.Failed(ProfileAction.INVITE, Failure.Server(response.code))
                    DgResponse.Sleeping ->
                        ProfileActionUiState.Failed(ProfileAction.INVITE, Failure.Sleeping)
                    DgResponse.AuthFailed -> ProfileActionUiState.SessionExpired
                }
            }
        }
    }

    /**
     * Sivuuttaa pelaajan tai purkaa sivuutuksen profiilin omalla `Ignore`-lomakkeella.
     * Suunta on sivun oma (`changeto`), eikä sovellus tiedä kumpi on kyseessä muuten kuin
     * napin tekstistä. Onnistuminen luetaan siitä että **profiilin nappi vaihtui**: vastaus
     * kelpaa profiiliksi jos se on profiili, muuten yksi uudelleenluku, sama kaava kuin
     * loungen Sign Upilla.
     */
    fun ignore() {
        if (busy()) return
        val page = currentProfile() ?: return
        val before = page.ignoreForm ?: return

        _action.value = ProfileActionUiState.Sending(ProfileAction.IGNORE)
        viewModelScope.launch {
            _action.value = withContext(io) {
                when (val response = forms.send(before.write())) {
                    is DgResponse.Ok -> {
                        val after = reload(ProfileParser.parse(response.html))?.ignoreForm
                        if (after != null && after.changeTo != before.changeTo) {
                            ProfileActionUiState.Done(ProfileAction.IGNORE, page.player.displayName, after.label)
                        } else {
                            ProfileActionUiState.Unconfirmed(ProfileAction.IGNORE, page.player.displayName)
                        }
                    }
                    is DgResponse.Offline -> ProfileActionUiState.Failed(ProfileAction.IGNORE, Failure.Offline)
                    is DgResponse.ServerError ->
                        ProfileActionUiState.Failed(ProfileAction.IGNORE, Failure.Server(response.code))
                    DgResponse.Sleeping ->
                        ProfileActionUiState.Failed(ProfileAction.IGNORE, Failure.Sleeping)
                    DgResponse.AuthFailed -> ProfileActionUiState.SessionExpired
                }
            }
        }
    }

    /**
     * Luovuttaa valitut ottelut sivun omalla lomakkeella (`POST /bg/resign/doit`). Lomake
     * tulee ladatusta luovutussivusta, valinnat ovat sen rivejä ja varmistus on sivun oma
     * `reellysure`-ruutu, jonka käyttäjä on ruksannut ruudulla. Sovellus ei lisää omaa
     * varmistusdialogia: luovutus on sivustolla tavallinen valikkotoiminto (`SUBSTANSSI.md`
     * kohta 36), ja sivun oma ruutu on jo se varmistus.
     *
     * **Vastaus ei ole luovutussivu** (mitattu 4.9.2026 illalla): se on oma `Post
     * Resignation` -sivunsa ilman lomaketta, mutta se **sanoo luvun omin sanoin**, ja se on
     * ensisijainen lopputulos (Tommin päätös samana iltana). Tuore lista haetaan silti,
     * koska kuittaussivulla ei ole rivejä eikä ruutu voi jäädä näyttämään juuri
     * luovutettuja otteluita.
     *
     * Kadonnut rivi jää varareitiksi: jos vastaus ei ole kuittaussivu, onnistuminen luetaan
     * yhä siitä että luovutussivu haettuna uudelleen **ei enää listaa** valittuja otteluita.
     * Kaksi todistetta eivät ole yhtä vahvoja, ja ruutu kertoo kummasta on kyse. Kadonnut rivi on tässä todiste, koska sivu listaa kaikki
     * keskeneräiset ottelut eikä vain vuorossa olevia.
     */
    fun resign(selected: Set<MatchId>) {
        if (_resign.value is ResignUiState.Sending) return
        val page = ((_state.value as? PageUiState.Loaded)?.content as? PageContent.Resign)?.page ?: return
        val submission = page.write(selected) ?: return

        _resign.value = ResignUiState.Sending
        viewModelScope.launch {
            _resign.value = withContext(io) {
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> {
                        // Sivuston oma lukema luetaan ensin, koska se on vastauksessa itsessään
                        // (Tommin päätös 4.9.2026 illalla). Lista haetaan silti tuoreena, koska
                        // kuittaussivulla ei ole rivejä eikä ruutu voi jäädä näyttämään
                        // luovutettuja otteluita.
                        val reported = ResignParser.resigned(response.html)
                        val fresh = ResignParser.parse(response.html)
                            ?: (pages.fetch(path) as? DgResponse.Ok)?.let { ResignParser.parse(it.html) }
                        if (fresh != null) _state.value = PageUiState.Loaded(PageContent.Resign(fresh))

                        val gone = fresh?.let { page ->
                            val remaining = page.rows.map { row -> row.match.id }.toSet()
                            selected.count { it !in remaining }
                        }
                        when {
                            // Sivusto sanoi luvun. Nolla ei ole onnistuminen vaan tilanne jossa
                            // se kertoo ettei mitään luovutettu; sitä ei ole nähty, eikä sille
                            // keksitä omaa tekstiä.
                            reported != null && reported.count > 0 ->
                                ResignUiState.Done(reported.count, reported.notice)
                            reported != null -> ResignUiState.Unconfirmed
                            // Vanha todiste: rivi katosi listalta.
                            gone != null && gone == selected.size -> ResignUiState.Done(gone)
                            else -> ResignUiState.Unconfirmed
                        }
                    }
                    is DgResponse.Offline -> ResignUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError -> ResignUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> ResignUiState.Failed(Failure.Sleeping)
                    DgResponse.AuthFailed -> ResignUiState.SessionExpired
                }
            }
        }
    }

    /** Luovutusilmoituksen kuittaus. Lähetystä ei kuitata pois kesken. */
    fun dismissResign() {
        if (_resign.value !is ResignUiState.Sending) _resign.value = ResignUiState.Idle
    }

    /** Ilmoituksen kuittaus. Lähetystä ei kuitata pois kesken. */
    fun dismissAction() {
        if (_action.value !is ProfileActionUiState.Sending) _action.value = ProfileActionUiState.Idle
    }

    private fun busy(): Boolean =
        _action.value is ProfileActionUiState.Sending || _send.value is ReplyUiState.Sending

    /**
     * Ruudulla juuri nyt oleva profiili, tai null kun sisältö on jotain muuta.
     *
     * **Yksi kirjoitusasu neljälle teolle (4.9.2026).** Sama kaksoiscast oli auki
     * kirjoitettuna `send`issä ja `export`issa ja tämän takana `invite`ssä ja `ignore`ssa,
     * eli sama portti kolmena eri rivinä. Ne olivat samaa mieltä, mutta juuri se on H1:n
     * muoto: seuraava teko kirjoittaa neljännen rivin, ja poikkeama jää huomaamatta koska
     * yksinään jokainen rivi näyttää oikealta.
     *
     * Luovutussivu ei kuulu tähän eikä ole poikkeama. Sen sisältö on
     * [PageContent.Resign] eikä profiili, joten `resign` lukee oman lajinsa.
     */
    private fun currentProfile(): ProfilePage? =
        ((_state.value as? PageUiState.Loaded)?.content as? PageContent.Profile)?.page

    /**
     * Profiili teon jälkeen: vastauksesta jos se on profiili, muuten samalta polulta
     * uudelleen. Tuore profiili korvaa ruudun sisällön; null tarkoittaa ettei tuoretta
     * saatu, ja silloin ruutu jää entiseen eikä kutsuja tulkitse mitään.
     */
    private fun reload(answered: ProfilePage?): ProfilePage? {
        val profile = answered
            ?: (pages.fetch(path) as? DgResponse.Ok)?.let { ProfileParser.parse(it.html) }
            ?: return null
        _state.value = PageUiState.Loaded(PageContent.Profile(profile))
        return profile
    }

    /**
     * Hakee päättyneen ottelun `.mat`-tiedoston jakoa varten. Sama portti kuin [send]illä:
     * ottelu on kelvollinen vain jos se on **ruudulla olevan profiilin** päättyneiden
     * listalla, ja osoite on sen rivin oma `Export`-linkki eikä koottu. Kutsuja antaa
     * ottelun eikä polkua, jotta polkua ei voi antaa mistään muualta.
     *
     * Haku ei kuluta jonoa (`/bg/export/<id>` ei ole `/bg/nextgame`), eikä tulosta
     * tallenneta sovellukseen: se kulkee välimuistin kautta jakovalikkoon ja siitä eteenpäin.
     */
    fun export(match: Match, format: ExportFormat = ExportFormat.MAT) {
        if (_export.value is ExportUiState.Fetching) return
        val profile = currentProfile() ?: return
        val onPage = profile.finishedMatches.firstOrNull { it.id == match.id } ?: return
        val path = onPage.exportPath ?: return

        _export.value = ExportUiState.Fetching
        viewModelScope.launch {
            _export.value = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok ->
                        MatchExport.read(onPage.id, response.html)
                            ?.let { ready(it, format, onPage.eventPath) }
                            ?: ExportUiState.NotAnExport
                    else -> exportFailed(response)
                }
            }
        }
    }

    private fun exportFailed(response: DgResponse): ExportUiState = when (response) {
        is DgResponse.Ok -> error("Onnistunut vastaus ei ole vika")
        is DgResponse.Offline -> ExportUiState.Failed(Failure.Offline)
        is DgResponse.ServerError -> ExportUiState.Failed(Failure.Server(response.code))
        DgResponse.Sleeping -> ExportUiState.Failed(Failure.Sleeping)
        DgResponse.AuthFailed -> ExportUiState.SessionExpired
    }

    /**
     * `.mat` sellaisenaan, tai `.sgf` kirjoitettuna siitä merkkeineen. Merkit luetaan
     * kannasta tässä eikä ruudulta, koska ruutu ei tunne niitä eikä sen tarvitse: vienti on
     * ainoa kohta jossa ottelun merkit ja ottelun tiedosto kohtaavat. Päivä on viennin
     * päivä eikä ottelun, koska tiedosto ei kerro ottelun päivää.
     *
     * `RU[Crawford]` luetaan turnaussivulta (`/bg/event/<id>`, rivin oma linkki), koska
     * `.mat` ei kerro muunnelmaa ja double repeat luopuu Crawfordista. Haku ei kuluta
     * jonoa. Ilman turnauslinkkiä (ystävyysottelu) tai ilman `Game`-ehtoa ottelu on
     * Crawford, kuten tavallinen ottelu on; turnaussivun hakuvirhe on viennin virhe eikä
     * arvaus, koska tiedosto kirjoitettaisiin silloin väärällä säännöllä.
     */
    private suspend fun ready(export: MatchExport, format: ExportFormat, eventPath: String?): ExportUiState {
        if (format == ExportFormat.MAT) return ExportUiState.Ready(export, export.fileName, export.text)
        val match = MatMatch.parse(export.text) ?: return ExportUiState.NotConvertible
        if (match.startsFromSetup) return ExportUiState.SetupPosition

        var doubleRepeat = false
        if (eventPath != null) {
            when (val response = pages.fetch(eventPath)) {
                is DgResponse.Ok -> doubleRepeat = EventParser.parse(response.html)?.doubleRepeat ?: false
                else -> return exportFailed(response)
            }
        }
        val ownMarks = marks.observeAll().first().filter { it.game.matchId == export.id }
        val date = java.time.Instant.ofEpochMilli(now()).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        return ExportUiState.Ready(
            export,
            sgfFileName(export.id),
            SgfExport.write(match, ownMarks, self = self(), date = date, crawford = !doubleRepeat),
        )
    }

    /** Jakovalikko on avattu, joten tiedostoa ei tarjota toista kertaa ruudun piirtyessä. */
    fun exportConsumed() {
        if (_export.value is ExportUiState.Ready) _export.value = ExportUiState.Idle
    }

    class Factory(
        private val pages: PageFetcher,
        private val forms: FormSender,
        private val archive: MessageArchive,
        private val path: String,
        private val self: () -> String?,
        private val marks: MarkBook,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PageViewModel(pages, forms, archive, path, self, marks) as T
    }
}

/**
 * Tämän ruudun tuntemat sivut yhtenä listana.
 *
 * **Lista eikä `when`-ketju, ja ero on siinä mitä lisääminen vaatii.** Ketjussa uusi sivulaji
 * oli kaksi riviä jotka piti muistaa pariksi: oikea tunnistin ja oikea jäsennin. Jäsennin
 * tunnistaa nyt itse (`DgPages`), ja lisäys on yksi rivi. Järjestys ei ratkaise mitään, koska tunnistimet ovat
 * mitatuilla sivuilla toisensa poissulkevia; jokin järjestys on silti pakko olla.
 */
private val READERS: List<(String) -> PageContent?> = listOf(
    { html -> ProfileParser.parse(html)?.let(PageContent::Profile) },
    { html -> EventParser.parse(html)?.let(PageContent::Event) },
    { html -> PlayerTournamentsParser.parse(html)?.let(PageContent::Tournaments) },
    { html -> SiteHelpParser.parse(html)?.let(PageContent::SiteHelp) },
    { html -> SiteLinksParser.parse(html)?.let(PageContent::SiteLinks) },
    { html -> MatchLogParser.parse(html)?.let(PageContent::MatchLog) },
    { html -> TournamentHallParser.parse(html)?.let(PageContent::Hall) },
    { html -> ResignParser.parse(html)?.let(PageContent::Resign) },
)
