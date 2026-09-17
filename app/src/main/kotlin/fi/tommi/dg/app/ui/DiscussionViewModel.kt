package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.ForumArchivePage
import fi.tommi.dg.domain.ForumBoard
import fi.tommi.dg.domain.ForumComposePage
import fi.tommi.dg.domain.ForumIndex
import fi.tommi.dg.domain.ForumMonthLink
import fi.tommi.dg.domain.ForumThread
import fi.tommi.dg.domain.ForumThreadPage
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.ForumParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ketjulistan tila. Sama kaava kuin [LoungeUiState]lla, syyt siellä.
 */
sealed interface DiscussionUiState {

    /** Palstan polkua ei ole vielä luettu Top Pagen navigointipalkista. */
    data object NoPath : DiscussionUiState

    data object Loading : DiscussionUiState

    data class Loaded(val index: ForumIndex, val refreshing: Boolean = false) : DiscussionUiState

    /** 200 OK, mutta sivu ei ole palstan indeksi. */
    data object NotAForumPage : DiscussionUiState

    data class Failed(val reason: Failure) : DiscussionUiState

    /** Istunto ei kelvannut. Tunnusten poisto kuuluu [TopViewModel]ille. */
    data object SessionExpired : DiscussionUiState, SessionExpiredState
}

/**
 * Avatun ketjun tila. Null [DiscussionViewModel.thread]issa tarkoittaa ettei ketjua ole
 * auki ja ruudulla on lista.
 */
sealed interface ThreadUiState {

    /** [origin] on listan rivi, jotta otsikko on ruudulla jo latauksen aikana. */
    data class Loading(val origin: ForumThread) : ThreadUiState

    data class Loaded(val page: ForumThreadPage) : ThreadUiState

    /** 200 OK, mutta sivu ei ole ketju. */
    data object NotAThread : ThreadUiState

    data class Failed(val reason: Failure) : ThreadUiState
}

/**
 * Kuukausiarkiston tila. Null [DiscussionViewModel.archive]ssa tarkoittaa että ruudulla on
 * indeksi. Arkisto on oma kerroksensa eikä indeksin tila, koska indeksi jää sen alle
 * odottamaan paluuta eikä sitä haeta uudelleen.
 */
sealed interface ArchiveUiState {

    /** [monthLabel] on tunnettu vain kuukautta vaihdettaessa, ei arkistoa avattaessa. */
    data class Loading(val monthLabel: String?) : ArchiveUiState

    data class Loaded(val page: ForumArchivePage) : ArchiveUiState

    /** 200 OK, mutta sivu ei ole kuukausiarkisto. */
    data object NotAnArchive : ArchiveUiState

    data class Failed(val reason: Failure) : ArchiveUiState
}

/** Kumpi lomake on auki: uusi ketju indeksiltä vai kommentti avattuun ketjuun. */
enum class ComposeKind { NEW_THREAD, COMMENT }

/**
 * Kirjoituslomakkeen tila. Null [DiscussionViewModel.compose]ssa tarkoittaa ettei lomake
 * ole auki. Lomake on oma kerroksensa ketjun tai indeksin päällä, joten paluu ei hae
 * mitään uudelleen.
 */
sealed interface ComposeUiState {
    val kind: ComposeKind

    data class Loading(override val kind: ComposeKind) : ComposeUiState

    data class Loaded(override val kind: ComposeKind, val page: ForumComposePage) : ComposeUiState

    /** 200 OK, mutta sivu ei ole kirjoituslomake. */
    data class NotAForm(override val kind: ComposeKind) : ComposeUiState

    data class Failed(override val kind: ComposeKind, val reason: Failure) : ComposeUiState
}

/**
 * Lähetyksen tila. Sama kaava kuin luovutuksella (`ResignUiState`): vastaussivua ei ole
 * mitattu, joten onnistuminen luetaan sisällöstä eikä vastauksesta. Kommentilla todiste on
 * lähetetty teksti uudelleen haetussa ketjussa, uudella ketjulla otsikko indeksin rivillä.
 */
sealed interface PostUiState {
    data object Idle : PostUiState

    data object Sending : PostUiState

    data class Done(val kind: ComposeKind) : PostUiState

    /** Sivusto vastasi, mutta sisältöä ei löytynyt tai sivua ei voitu lukea. */
    data class Unconfirmed(val kind: ComposeKind) : PostUiState

    data class Failed(val reason: Failure) : PostUiState
}

/**
 * Keskustelupalstan näkymämalli.
 *
 * ~~**Konstruktorissa ei ole `FormSender`iä, ja se on väite:** tämä ruutu ei voi tehdä
 * sivustolla mitään.~~ Rajaus purettiin 3.9.2026 Tommin päätöksellä (`docs/UI.md`, *Forum-
 * välilehti: uusi ketju ja kommentti*): malli saa [FormSender]in, ja sen ainoat käyttökohteet
 * ovat sivun omat kirjoituslomakkeet ([post]). Haut ovat yhä pelkkää lukua. Ketjun lukeminen
 * ei kuluta mitään: New-merkit seuraavat sivuston omaa lukutilaa, joka päivittyy luvun
 * myötä, mutta itse viestit säilyvät palstalla toisin kuin jonon viestit.
 *
 * **Luonnos asuu täällä eikä ruudussa**, jotta se selviää kierrosta ja epäonnistuneesta
 * lähetyksestä: palstan viestiä ei voi korjata jälkikäteen (`SUBSTANSSI.md` kohta 59), joten
 * sen kirjoittamiseen käytetty aika on arvokasta eikä sitä hukata.
 *
 * Polku tulee virtana samasta syystä kuin [MessagesViewModel]illa ja [LoungeViewModel]illa.
 */
class DiscussionViewModel(
    private val pages: PageFetcher,
    private val forms: FormSender,
    forumPathUpstream: Flow<String?>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _path = MutableStateFlow<String?>(null)

    /**
     * Käyttäjän valitsema palsta, joka voittaa yläjuoksun polun. Ilman tätä Top Pagen
     * uusi lataus vaihtaisi palstan hiljaa takaisin Generaliksi kesken lukemisen, koska
     * yläjuoksu tuntee vain sen.
     */
    private var chosenPath: String? = null

    private val _state = MutableStateFlow<DiscussionUiState>(DiscussionUiState.NoPath)
    val state: StateFlow<DiscussionUiState> = _state.asStateFlow()

    private val _thread = MutableStateFlow<ThreadUiState?>(null)
    val thread: StateFlow<ThreadUiState?> = _thread.asStateFlow()

    private val _archive = MutableStateFlow<ArchiveUiState?>(null)
    val archive: StateFlow<ArchiveUiState?> = _archive.asStateFlow()

    private val _compose = MutableStateFlow<ComposeUiState?>(null)
    val compose: StateFlow<ComposeUiState?> = _compose.asStateFlow()

    private val _post = MutableStateFlow<PostUiState>(PostUiState.Idle)
    val post: StateFlow<PostUiState> = _post.asStateFlow()

    /** Avoinna olevan lomakkeen otsikko luonnoksena. Tyhjä kommentilla. */
    private val _draftTitle = MutableStateFlow("")
    val draftTitle: StateFlow<String> = _draftTitle.asStateFlow()

    /** Avoinna olevan lomakkeen runko luonnoksena. */
    private val _draftComment = MutableStateFlow("")
    val draftComment: StateFlow<String> = _draftComment.asStateFlow()

    /**
     * Luonnokset lomakkeen polun mukaan. Polku on avain siksi että uuden ketjun teksti ei
     * saa ilmestyä kommenttikenttään eikä yhden ketjun kommentti toiseen ketjuun; mitattu
     * laitteella 3.9.2026, kun yksi yhteinen luonnos teki juuri niin.
     */
    private val drafts = mutableMapOf<String, Pair<String, String>>()

    /** Avoinna olevan lomakkeen polku, luonnosten avain. */
    private var composePath: String? = null

    /** Avatun ketjun polku, jotta ketju voidaan hakea uudelleen kommentin jälkeen. */
    private var openThreadPath: String? = null

    /**
     * Sivun oma palstarivi. Erillään [state]sta, jotta segmenttirivi pysyy ruudulla myös
     * palstan latautuessa: latauksen ajan ei ole indeksiä josta lukea.
     */
    private val _boards = MutableStateFlow<List<ForumBoard>>(emptyList())
    val boards: StateFlow<List<ForumBoard>> = _boards.asStateFlow()

    /** Valitun palstan nimi. Ladatulla sivulla se on se palsta jolla ei ole polkua. */
    private val _selectedBoard = MutableStateFlow<String?>(null)
    val selectedBoard: StateFlow<String?> = _selectedBoard.asStateFlow()

    init {
        viewModelScope.launch {
            forumPathUpstream.collect { path ->
                if (chosenPath == null) _path.value = path
                if (path != null && _state.value is DiscussionUiState.NoPath) fetch()
            }
        }
    }

    fun refresh() = fetch()

    /**
     * Vaihtaa palstaa. Polku on sivun oma linkki, ja näkyvillä olevalla palstalla sitä ei
     * ole lainkaan, joten napautus siihen ei tee mitään.
     */
    fun selectBoard(board: ForumBoard) {
        val path = board.path ?: return
        chosenPath = path
        _path.value = path
        // Arkisto kuuluu palstaan: toisen palstan kuukausisivu jäisi ruudulle väärän
        // segmentin alle, ja sen ketjut olisivat eri palstan ketjuja.
        _archive.value = null
        // Valinta näkyy heti eikä vasta latauksen jälkeen; ladattu sivu vahvistaa sen.
        _selectedBoard.value = board.name
        fetch()
    }

    // Epäonnistunut päivitys saa korvata näkyvän sisällön virheruudulla: valinta eikä
    // vuoto, ks. perustelu `LoungeViewModel.read`.
    private fun fetch() {
        val path = _path.value ?: run {
            _state.value = DiscussionUiState.NoPath
            return
        }
        _state.value = when (val current = _state.value) {
            is DiscussionUiState.Loaded -> current.copy(refreshing = true)
            else -> DiscussionUiState.Loading
        }
        viewModelScope.launch {
            _state.value = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok ->
                        ForumParser.parseIndex(response.html)?.let { index ->
                            if (index.boards.isNotEmpty()) {
                                _boards.value = index.boards
                                _selectedBoard.value = index.boards.firstOrNull { it.path == null }
                                    ?.name
                                    ?: _selectedBoard.value
                            }
                            DiscussionUiState.Loaded(index)
                        } ?: DiscussionUiState.NotAForumPage
                    is DgResponse.Offline -> DiscussionUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError ->
                        DiscussionUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping ->
                        DiscussionUiState.Failed(Failure.Sleeping)
                    // Tunnuksia ei poisteta täällä: CredentialsStoren omistaa TopViewModel.
                    DgResponse.AuthFailed -> DiscussionUiState.SessionExpired
                }
            }
        }
    }

    /**
     * Avaa ketjun. Polku on listan rivin oma `readPath` fragmentteineen; fragmentti on
     * selaimen oma osoitin eikä kulje verkon yli, ja OkHttp pudottaa sen itse.
     */
    fun openThread(origin: ForumThread) {
        if (_thread.value is ThreadUiState.Loading) return

        openThreadPath = origin.readPath
        _thread.value = ThreadUiState.Loading(origin)
        viewModelScope.launch {
            _thread.value = withContext(io) {
                when (val response = pages.fetch(origin.readPath)) {
                    is DgResponse.Ok ->
                        ForumParser.parseThread(response.html)
                            ?.let { ThreadUiState.Loaded(it) }
                            ?: ThreadUiState.NotAThread
                    is DgResponse.Offline -> ThreadUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError -> ThreadUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> ThreadUiState.Failed(Failure.Sleeping)
                    DgResponse.AuthFailed -> {
                        _state.value = DiscussionUiState.SessionExpired
                        null
                    }
                }
            }
        }
    }

    fun closeThread() {
        _thread.value = null
        openThreadPath = null
    }

    /**
     * Avaa uuden ketjun lomakkeen. Polku on indeksin oma `Add a New Thread` -linkki, ja
     * ilman sitä napautus ei tee mitään: polkua ei koota palstan tunnuksesta.
     */
    fun openNewThread() {
        val path = (_state.value as? DiscussionUiState.Loaded)?.index?.newThreadPath ?: return
        openCompose(ComposeKind.NEW_THREAD, path)
    }

    /** Avaa kommentin lomakkeen avattuun ketjuun sen omasta `Add a Comment` -linkistä. */
    fun openComment() {
        val path = (_thread.value as? ThreadUiState.Loaded)?.page?.addCommentPath ?: return
        openCompose(ComposeKind.COMMENT, path)
    }

    private fun openCompose(kind: ComposeKind, path: String) {
        if (_compose.value is ComposeUiState.Loading || _post.value is PostUiState.Sending) return

        _post.value = PostUiState.Idle
        composePath = path
        val draft = drafts[path]
        _draftTitle.value = draft?.first.orEmpty()
        _draftComment.value = draft?.second.orEmpty()
        _compose.value = ComposeUiState.Loading(kind)
        viewModelScope.launch {
            _compose.value = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok ->
                        ForumParser.parseCompose(response.html)
                            ?.let { ComposeUiState.Loaded(kind, it) }
                            ?: ComposeUiState.NotAForm(kind)
                    is DgResponse.Offline -> ComposeUiState.Failed(kind, Failure.Offline)
                    is DgResponse.ServerError ->
                        ComposeUiState.Failed(kind, Failure.Server(response.code))
                    DgResponse.Sleeping ->
                        ComposeUiState.Failed(kind, Failure.Sleeping)
                    DgResponse.AuthFailed -> {
                        _state.value = DiscussionUiState.SessionExpired
                        null
                    }
                }
            }
        }
    }

    /**
     * Sulkee lomakkeen. Luonnos jää talteen: sulkeminen ei ole hylkäys, ja sama teksti on
     * tarjolla kun lomake avataan seuraavan kerran. Lähetystä ei suljeta kesken.
     */
    fun closeCompose() {
        if (_post.value is PostUiState.Sending) return
        _compose.value = null
    }

    fun setDraftTitle(value: String) {
        _draftTitle.value = value
        rememberDraft()
    }

    fun setDraftComment(value: String) {
        _draftComment.value = value
        rememberDraft()
    }

    private fun rememberDraft() {
        val path = composePath ?: return
        drafts[path] = _draftTitle.value to _draftComment.value
    }

    /**
     * Lähettää lomakkeen sivun omalla napilla. Vahvistus on ruudun asia ja edeltää tätä:
     * palstan viestiä ei voi muokata eikä poistaa (`SUBSTANSSI.md` kohta 59), ja dialogi on
     * se hetki ennen lähettämistä jonka sovellus voi tarjota.
     *
     * **Vastaussivua ei ole mitattu** (`docs/KOHDE.md`). Kommentin jälkeen ketju haetaan
     * uudelleen (vastaus kelpaa jos se on ketjusivu) ja lähetetyn tekstin ensimmäisen rivin
     * on löydyttävä sen viesteistä. Uuden ketjun jälkeen indeksi haetaan uudelleen ja
     * otsikon on löydyttävä riveiltä. Muuten tila on [PostUiState.Unconfirmed]. POST menee
     * kerran eikä sitä uusita, koska uusittu runko olisi kaksi viestiä palstalla.
     *
     * Onnistunut lähetys tyhjentää luonnoksen ja sulkee lomakkeen; muu jättää molemmat.
     */
    fun post() {
        if (_post.value is PostUiState.Sending) return
        val compose = _compose.value as? ComposeUiState.Loaded ?: return
        val title = _draftTitle.value
        val comment = _draftComment.value
        val submission = compose.page.write(title = title, comment = comment) ?: return

        _post.value = PostUiState.Sending
        viewModelScope.launch {
            _post.value = withContext(io) {
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> when (compose.kind) {
                        ComposeKind.COMMENT -> confirmComment(response.html, comment)
                        ComposeKind.NEW_THREAD -> confirmNewThread(response.html, title.trim())
                    }
                    is DgResponse.Offline -> PostUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError -> PostUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> PostUiState.Failed(Failure.Sleeping)
                    DgResponse.AuthFailed -> {
                        _state.value = DiscussionUiState.SessionExpired
                        PostUiState.Idle
                    }
                }
            }
            if (_post.value is PostUiState.Done) {
                composePath?.let(drafts::remove)
                _draftTitle.value = ""
                _draftComment.value = ""
                _compose.value = null
            }
        }
    }

    /**
     * **Kuittaussivu mitattiin 4.9.2026** (fixture `forum_posted.html`), ja se muutti
     * todisteen lajin. Tähän asti onnistuminen luettiin siitä että lähetetty teksti löytyi
     * uudelleen haetusta ketjusta, eli sovelluksen omasta vertailusta. Nyt sivusto sanoo
     * sen itse: vastauksella on linkki `Review your message.`, joka osoittaa juuri syntyneen
     * viestin ankkuriin (`DgPages.isForumPostedPage`).
     *
     * Ketju haetaan silti uudelleen, koska ruudun on näytettävä oma viesti paikallaan.
     * Ero on siinä mitä tapahtuu kun haku epäonnistuu: kuitattu lähetys on kuitattu, vaikka
     * ruutu jäisi vanhaan. Tekstivertailu jää varareitiksi sille tapaukselle jossa sivusto
     * vastaa jotain muuta kuin mitattua kuittausta.
     */
    private fun confirmComment(responseHtml: String, comment: String): PostUiState {
        val kuitattu = DgPages.isForumPostedPage(responseHtml)
        val fresh = ForumParser.parseThread(responseHtml)
            ?: openThreadPath?.let { path ->
                (pages.fetch(path) as? DgResponse.Ok)?.let { ForumParser.parseThread(it.html) }
            }
        if (fresh != null) _thread.value = ThreadUiState.Loaded(fresh)
        if (kuitattu) return PostUiState.Done(ComposeKind.COMMENT)
        if (fresh == null) return PostUiState.Unconfirmed(ComposeKind.COMMENT)
        // Ensimmäinen ei-tyhjä rivi riittää todisteeksi: sivu rivittää ja kappaleistaa
        // rungon omalla tavallaan, joten koko tekstin vertailu voisi kaatua muotoiluun.
        val probe = comment.lines().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
        val found = probe.isNotEmpty() && fresh.posts.any { it.body.contains(probe) }
        return if (found) PostUiState.Done(ComposeKind.COMMENT) else PostUiState.Unconfirmed(ComposeKind.COMMENT)
    }

    private fun confirmNewThread(responseHtml: String, title: String): PostUiState {
        val fresh = ForumParser.parseIndex(responseHtml)
            ?: _path.value?.let { path ->
                (pages.fetch(path) as? DgResponse.Ok)?.let { ForumParser.parseIndex(it.html) }
            }
            ?: return PostUiState.Unconfirmed(ComposeKind.NEW_THREAD)
        _state.value = DiscussionUiState.Loaded(fresh)
        val found = fresh.threads.any { it.title.trim() == title }
        return if (found) PostUiState.Done(ComposeKind.NEW_THREAD) else PostUiState.Unconfirmed(ComposeKind.NEW_THREAD)
    }

    /** Lähetysilmoituksen kuittaus. Lähetystä ei kuitata pois kesken. */
    fun dismissPost() {
        if (_post.value !is PostUiState.Sending) _post.value = PostUiState.Idle
    }

    /**
     * Avaa kuukausiarkiston. Polku on sivun oma `Old Threads` -linkki, eikä sitä koota:
     * `/bg/forum2/<palsta>/month` ilman vuotta on **kuluva** kuukausi, ja sivu kertoo
     * otsikossaan mikä se on.
     */
    fun openArchive(path: String) = loadArchive(path, monthLabel = null)

    /** Vaihtaa kuukautta arkiston omasta luettelosta. */
    fun openMonth(month: ForumMonthLink) = loadArchive(month.path, monthLabel = month.label)

    private fun loadArchive(path: String, monthLabel: String?) {
        if (_archive.value is ArchiveUiState.Loading) return

        _archive.value = ArchiveUiState.Loading(monthLabel)
        viewModelScope.launch {
            _archive.value = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok ->
                        ForumParser.parseArchive(response.html)
                            ?.let { ArchiveUiState.Loaded(it) }
                            ?: ArchiveUiState.NotAnArchive
                    is DgResponse.Offline -> ArchiveUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError ->
                        ArchiveUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping ->
                        ArchiveUiState.Failed(Failure.Sleeping)
                    DgResponse.AuthFailed -> {
                        _state.value = DiscussionUiState.SessionExpired
                        null
                    }
                }
            }
        }
    }

    fun closeArchive() {
        _archive.value = null
    }

    class Factory(
        private val pages: PageFetcher,
        private val forms: FormSender,
        private val forumPathUpstream: Flow<String?>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DiscussionViewModel(pages, forms, forumPathUpstream) as T
    }
}
