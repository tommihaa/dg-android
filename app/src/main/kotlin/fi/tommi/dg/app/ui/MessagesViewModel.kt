package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.MessageFilterStore
import fi.tommi.dg.app.session.PhraseBook
import fi.tommi.dg.app.session.PhraseStore
import fi.tommi.dg.data.MessageArchive
import fi.tommi.dg.data.ReminderBook
import fi.tommi.dg.domain.ArchiveImport
import fi.tommi.dg.domain.readArchiveJson
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.domain.InviteChoice
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.domain.ReceivedInvitation
import fi.tommi.dg.domain.write
import fi.tommi.dg.domain.toArchiveJson
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.InboxParser
import fi.tommi.dg.scrape.InvitationParser
import fi.tommi.dg.scrape.SendResultParser
import fi.tommi.dg.scrape.toMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mitä jonon hakemisesta seurasi.
 *
 * Tiloja on enemmän kuin muissa ruuduissa, ja syy on että tässä yksi teko on peruuttamaton.
 * Käyttäjän on saatava tietää mikä lopputulos oli, koska hän ei voi tarkistaa sitä
 * hakemalla uudelleen: kohde on jo kulunut.
 */
sealed interface QueueUiState {

    /** Mitään ei ole haettu tässä ruudussa. Ainoa tila johon ruutu käynnistyy. */
    data object Idle : QueueUiState

    data object Fetching : QueueUiState

    /**
     * Viesti on kannassa. Tila asetetaan vasta kirjoituksen jälkeen, ei ennen.
     *
     * ~~Lomake kulki tässä tilassa eikä arkiston viestissä~~ **30.8.2026 alkaen lomake on
     * viestissä itsessään** ([Message.replyForm]), koska Tommi pyysi että arkiston
     * viestiin voi vastata klikkaamalla. Portin muoto säilyi vaikka paikka vaihtui:
     * lomakkeen voi yhä tuottaa vain jäsennin sivun omista tavuista, ja viesti jolla ei
     * ole lomaketta ei voi saada vastauskenttää milloinkaan.
     */
    data class Saved(val message: Message) : QueueUiState

    /**
     * Jonosta tuli sivuston oma ilmoitus. **Näytetään, mutta ei tallenneta**
     * (Tommin päätös 16.8.2026, ks. `docs/AVOIMET.md`).
     *
     * Oma tilansa eikä [Saved], koska ero on käyttäjälle merkitsevä: kohde kului jonosta
     * eikä siitä jäänyt kopiota mihinkään. Tämä on ainoa tila jossa niin käy, ja siksi
     * ilmoituksen teksti kulkee tilan mukana: se on hetken näkyvissä ruudulla eikä missään
     * muualla.
     */
    data class Announcement(val message: Message) : QueueUiState

    /**
     * Jono tarjosi jotain muuta kuin viestin, eikä mitään tallennettu.
     *
     * Oma tilansa eikä [Failed]: verkkovirheen voi yrittää uudelleen, mutta tämä tarkoittaa
     * että jonon kärjessä oli toisenlainen kohde.
     */
    data class NotAMessage(val kind: NotAMessageKind) : QueueUiState

    /**
     * Jono oli tyhjä: sivusto vastasi `/bg/nextgame`en otteluluettelolla eikä kohteella
     * (Tommin havainto 18.9.2026, *"Take an item on harhaanjohtava, jos dailygammonin
     * viestijono on tyhjä"*). Mitään ei kulunut eikä tallennettu.
     *
     * Oma tilansa eikä [NotAMessage]: siellä jonon kärjessä oli jotain, tässä kärkeä ei
     * ollut. Ilman tätä Top Page putosi [NotAMessageKind.Unreadable]en, joka käskee
     * tarkistamaan sivuston vaikka syy on tyhjä jono. Polku pudotetaan samalla nulliksi,
     * joten nappi katoaa siihen asti että luettelo antaa linkin uudelleen. Tämä sulkee
     * `docs/UI.md`:n 13.8.2026 kirjaaman aukon (*sitä ei ole mitattu mitä sivusto palauttaa
     * tyhjästä jonosta*) tunnistuksen puolelta; itse vastaus on yhä mittaamatta.
     */
    data object Empty : QueueUiState

    /**
     * Jonon kärjessä on toisen pelaajan suora ottelukutsu (Tommin päätös 14.9.2026:
     * *"ilmoitus napin alle, kaikkiin lomakkeisiin"* ei koske tätä, vaan *"vaihtoehto 1"*:
     * kutsu näytetään jonokortissa sivun sanoin ja siihen vastataan sovelluksesta).
     *
     * **Ei tallenneta, ja se on eri syy kuin [Announcement]illa.** Ilmoitus pudotetaan
     * määrän vuoksi vaikka se kuluu; kutsu ei kulu lainkaan (`docs/KOHDE.md`, mitattu
     * 14.9.2026 viidesti tavulleen samana), joten sen arkistointi tuottaisi kopion jokaisella
     * painalluksella eikä säilyttäisi mitään mitä sivusto ei jo säilytä. Kutsu on jonon
     * kärki kunnes siihen vastataan, ja vastaaminen on [MessagesViewModel.acceptInvitation],
     * [MessagesViewModel.declineInvitation] tai [MessagesViewModel.counterOffer].
     */
    data class Invitation(val invitation: ReceivedInvitation) : QueueUiState

    /** Verkko tai palvelin. Mitään ei kulunut, joten uudelleenyritys on turvallinen. */
    data class Failed(val reason: Failure) : QueueUiState

    /** Istunto ei kelvannut. Tunnusten poisto kuuluu [TopViewModel]ille. */
    data object SessionExpired : QueueUiState, SessionExpiredState
}

/**
 * Vastauksen lähetyksen tila.
 *
 * Erillään [QueueUiState]istä, koska ne mittaavat eri tekoa: jono kuluttaa kohteen
 * sivustolta, vastaus lähettää tekstin ihmiselle. Yhdistettynä lähetyksen epäonnistuminen
 * pyyhkisi ruudulta sen viestin johon ollaan vastaamassa.
 */
sealed interface ReplyUiState {

    data object Idle : ReplyUiState

    data object Sending : ReplyUiState

    /** Kuittaussivu saatiin ja oma viesti on arkistossa. */
    data object Sent : ReplyUiState

    /**
     * Lähetys ei mennyt perille. **Teksti jää kenttään** (Tommin päätös 22.8.2026), koska
     * kuljetus ei toista lomaketta itse eikä käyttäjä saa menettää kirjoittamaansa.
     */
    data class Failed(val reason: Failure) : ReplyUiState

    /**
     * Istunto oli katkennut lähetyshetkellä. Teksti jää kenttään tässäkin.
     *
     * **Ei [SessionExpiredState]ia, ja se on koko sisältö.** Merkintä tarkoittaa että
     * `SignOutOnExpiry` kirjaa ulos ja sulkee ruudun, ja se veisi juuri sen tekstin jonka
     * yllä oleva lause lupaa jättää kenttään (Tommin päätös 22.8.2026). Tämä tila kantoi
     * merkintää 1.9.–4.9.2026 ilman vaikutusta, koska ruutu ei anna tätä virtaa
     * käsittelijälle; vaikutus olisi tullut sinä päivänä jona joku antaa. Sääntö on
     * [SessionExpiredState]in dokumentaatiossa.
     */
    data object SessionExpired : ReplyUiState

    /**
     * Palvelin vastasi jotain muuta kuin kuittausta. **Ei tulkita onnistumiseksi.**
     *
     * Oma tilansa, koska tässä ei tiedetä menikö viesti perille: sivu ei ollut
     * kuittaus eikä login-lomake. Uudelleenlähetys voisi siis tuottaa kaksi viestiä, ja
     * siksi käyttäjän on saatava tietää nimenomaan tämä eikä pelkkä "ei onnistunut".
     */
    data object Unconfirmed : ReplyUiState
}

/**
 * Kutsuun vastaamisen tila. Erillään [QueueUiState]istä samasta syystä kuin [ReplyUiState]:
 * jono kuluttaa kohteen, vastaus lähettää teon, ja yhdistettynä epäonnistuminen pyyhkisi
 * kutsun ruudulta ennen kuin siihen on vastattu.
 */
sealed interface InvitationActionUiState {

    data object Idle : InvitationActionUiState

    data class Sending(val action: InvitationAction) : InvitationActionUiState

    /**
     * Palvelin vastasi 200:lla. [notice] on vastaussivun ensimmäinen lause
     * (`SendResultParser.notice`), tai null kun sivu oli otteluluettelo tai lause ei
     * kelvannut. **Vastaussivun muotoa ei ole mitattu** ennen 14.9.2026 iltaa, joten tila
     * sanoo vain että vastaus tuli ja mitä sivu sanoi; se ei väitä että kutsu on käsitelty.
     * Kutsun tila selviää Top Pagesta: jos jonon merkki ja kutsu ovat poissa, se meni läpi.
     */
    data class Answered(val action: InvitationAction, val notice: String?) : InvitationActionUiState

    data class Failed(val action: InvitationAction, val reason: Failure) : InvitationActionUiState

    data object SessionExpired : InvitationActionUiState, SessionExpiredState
}

enum class InvitationAction { ACCEPT, DECLINE, COUNTER }

enum class NotAMessageKind {
    /** Jonon kärjessä oli pelilauta. Tavallinen tilanne eikä vika. */
    Board,

    /** Sivu ei ollut lauta muttei myöskään jäsentynyt viestiksi. Tuntematon muoto. */
    Unreadable,
}

/**
 * Viestiarkisto ja jonon haku.
 *
 * **Tämä on sovelluksen ensimmäinen näkymämalli joka tekee peruuttamattoman teon**, ja koko
 * luokan rakenne on sen ympärillä. Muut mallit hakevat kuluttamattomia sivuja ja voivat
 * siksi hakea käynnistyessään; tämä ei voi.
 *
 * Neljä ehtoa, jotka ovat tässä rakenteena eivätkä kommenttina:
 *
 * 1. **Haku vain [fetchNext]istä.** `init` ei hae mitään, eikä luokassa ole ajastusta,
 *    ennakointia eikä uudelleenyritystä. Arkiston luku on eri asia eikä koske verkkoa.
 * 2. **Osoite luetaan, ei koota.** Polku tulee Top Pagen omasta linkistä ja seuraava polku
 *    juuri haetun sivun omasta `Next >>` -linkistä. Ilman linkkiä ei ole hakua: silloin
 *    [queuePath] on null eikä ruudulla ole nappia.
 * 3. **Yksi kerrallaan.** Käynnissä olevan haun aikana toinen kutsu ei tee mitään, joten
 *    kaksoisnapautus ei kuluta kahta kohdetta.
 * 4. **Kirjoitus ennen näyttöä.** [QueueUiState.Saved] asetetaan vasta kun `archive` on
 *    palannut, ja `MessageArchive` palauttaa viestin vasta tallennuksen jälkeen. Ehdolla on
 *    yksi poikkeus ja se on nimetty: sivuston ilmoitusta ei kirjoiteta lainkaan, jolloin
 *    tila on [QueueUiState.Announcement] eikä `Saved`. Ero on tilan nimessä eikä pelkässä
 *    tekstissä, jottei tallentamaton kohde voi näyttää tallennetulta.
 */
/**
 * Mitä tuonnista seurasi (16.9.2026). Luvut ovat ruudulle, koska tuonti joka ei kirjoita
 * mitään näyttäisi muuten samalta kuin tuonti joka kirjoitti kaiken.
 */
sealed interface ImportUiState {

    data object Idle : ImportUiState

    /**
     * @param alreadyHere Tiedoston viestit jotka olivat jo kannassa. Muistutuksista ja
     *   fraaseista ei lasketa vastaavaa, koska niiden lukumäärä on pieni ja lause pitenisi.
     * @param unreadable Rivit joita lukija ei saanut luettua (`ArchiveImport.Read.unreadable`).
     * @param otherAccount Tiedoston tili jos se ei ole kirjautunut tili, muuten null.
     */
    data class Done(
        val messages: Int,
        val reminders: Int,
        val phrases: Int,
        val alreadyHere: Int,
        val unreadable: Int,
        val otherAccount: String?,
    ) : ImportUiState

    /** Tiedosto ei ollut tämän sovelluksen arkisto. Mitään ei kirjoitettu. */
    data object NotAnArchive : ImportUiState
}

class MessagesViewModel(
    private val pages: PageFetcher,
    /**
     * Vastauksen lähetys. **Tämän olemassaolo konstruktorissa on se mikä muuttui
     * 22.8.2026**, ks. `FormSender`in oma perustelu: näkymämallin kyky toimia sivustolla
     * luetaan signatuurista, ja nyt se kertoo että tämä malli toimii.
     */
    private val forms: FormSender,
    private val archive: MessageArchive,
    /**
     * Otteluluettelon kertoma jonon polku, virtana eikä arvona.
     *
     * **Kertakuva olisi hiljainen väärä nolla** (mitattu vikamuoto, korjattu 9.8.2026). Ruutu
     * luki polun kerran avautuessaan, joten ruudun avaaminen ennen kuin lista oli latautunut
     * jätti polun nulliksi ja ruutu sanoi ettei mitään odota, vaikka odottaisi. Väärä nolla
     * luetaan vastaukseksi eikä virheeksi, ja se on tässä projektissa se suunta jota vastaan
     * suojaudutaan.
     *
     * Virtana myös toiseen suuntaan: kun luettelo päivitetään eikä ilmoitusta enää ole,
     * polku muuttuu nulliksi ja hakunappi katoaa. Otteluluettelo on se joka tietää onko
     * jotain odottamassa, joten ruutu seuraa sitä eikä muista omaa käsitystään.
     */
    queuePathUpstream: Flow<String?>,
    /**
     * Kutsutaan kun jonon kärjestä tuli lauta, eli viestit on luettu (18.9.2026). Kutsuja
     * pyyhkii luettelon ilmoituksen ([TopViewModel.clearMessageNotice]); tämä malli ei
     * tunne luetteloa, joten pyyhintä on annettu funktiona. Oletus ei tee mitään (testit).
     */
    private val onMessagesDrained: () -> Unit = {},
    /**
     * Saapumishetki. Parametrina eikä kellosta suoraan, koska se on osa viestin tunnistetta
     * ja testin on voitava antaa sama luku kahdesti.
     */
    private val now: () -> Long = System::currentTimeMillis,
    /**
     * Kirjautuneen oma käyttäjänimi, tai null jos tunnuksia ei ole.
     *
     * Tarpeen vain lähetettyä viestiä arkistoitaessa: sen lähettäjä on käyttäjä itse.
     * **Ei arvaus vaan sovelluksen oma tieto**, ja `Message.opponent`in perustelu on
     * kirjoitettu tätä tapausta varten: lähettäjä ja keskustelukumppani eroavat heti kun
     * oma viesti tallennetaan. Kuittaussivu ei nimeä lähettäjää, joten sitä ei voi lukea
     * sivulta, ja tyhjä nimi tekisi omasta viestistä nimettömän.
     */
    private val self: () -> String? = { null },
    /**
     * Pelaajasuodattimen säilö. Valinta muistetaan laitteella (Tommin pyyntö 30.8.2026),
     * joten se luetaan täältä käynnistyksessä ja kirjoitetaan jokaisesta vaihdosta.
     * Oletus ei suodata mitään, koska säilötön malli (testit) vastaa uutta asennusta.
     */
    private val filterStore: MessageFilterStore = NoFilter,
    /**
     * Fraasinappien lista (Tommin tilaus 3.9.2026), sovelluksen yhteinen: sama lista on
     * laudan chat-kortissa. Kirjaton malli (testit) pitää listan muistissa ja alkaa
     * oletuksesta, kuten uusi asennus.
     */
    private val phraseBook: PhraseBook = PhraseBook(MemoryPhrases()),
    /**
     * Muistutukset, viennin ja tuonnin osana 16.9.2026 alkaen. Pakollinen eikä oletus,
     * jotta kytkennän unohtaminen näkyy käännösvirheenä eikä vientinä josta muistutukset
     * puuttuvat hiljaa.
     */
    private val reminders: ReminderBook,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private object NoFilter : MessageFilterStore {
        override fun get(): String? = null
        override fun save(opponent: String?) = Unit
    }

    private class MemoryPhrases : PhraseStore {
        private var stored: List<String> = PhraseStore.DEFAULT
        override fun get(): List<String> = stored
        override fun save(phrases: List<String>) {
            stored = phrases
        }
    }

    private val _queuePath = MutableStateFlow<String?>(null)

    /** Seuraavan haun kohde, tai null jos linkkiä ei ole. Ruudun napin ehto. */
    val queuePath: StateFlow<String?> = _queuePath.asStateFlow()

    init {
        // Kohde on aina **tuorein sivulta luettu linkki**, ja lähteitä on kaksi: luettelon
        // ilmoituslinkki ja juuri haetun sivun `Next >>`. Kumpikaan ei ole koottu, joten
        // sääntö voi olla näin yksinkertainen: viimeksi saapunut voittaa.
        //
        // Haun jälkeen luettelon virta ei emittoi uudelleen ellei arvo muutu, joten sivulta
        // luettu jatkopolku säilyy. Jos ilmoitus katoaa luettelosta, virta emittoi nullin ja
        // nappi katoaa.
        viewModelScope.launch {
            queuePathUpstream.collect { _queuePath.value = it }
        }
    }

    private val _queue = MutableStateFlow<QueueUiState>(QueueUiState.Idle)
    val queue: StateFlow<QueueUiState> = _queue.asStateFlow()

    private val _invitationAction = MutableStateFlow<InvitationActionUiState>(InvitationActionUiState.Idle)

    /** Kutsuun vastaamisen tila, ks. [InvitationActionUiState]. */
    val invitationAction: StateFlow<InvitationActionUiState> = _invitationAction.asStateFlow()

    private val _draft = MutableStateFlow("")

    /**
     * Kirjoitettu mutta lähettämätön vastaus.
     *
     * **Säilyy kunnes jonosta otetaan seuraava kohde** (Tommin päätös 22.8.2026). Kenttään
     * sidottu tila katoaisi ruudulta poistuttaessa, ja se toistaisi juuri sen vian jonka
     * takia sovellus osin on olemassa: `SUBSTANSSI.md` kohta 50 sanoo että nykyisen
     * välineen pahin puute on se että väärä kosketus hävittää koko kirjoitetun viestin.
     *
     * Tyhjennys on siis sidottu viestin vaihtumiseen eikä näkyvyyteen, ja se tapahtuu
     * yhdessä paikassa: [readItem]issä, jossa uusi kohde korvaa vanhan.
     */
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _reply = MutableStateFlow<ReplyUiState>(ReplyUiState.Idle)
    val reply: StateFlow<ReplyUiState> = _reply.asStateFlow()

    fun draftChanged(text: String) {
        _draft.value = text
        // Vanha lopputulos ei saa jäädä ruudulle kun käyttäjä alkaa kirjoittaa uudestaan:
        // "lähetetty" kirjoittamisen päällä olisi väite väärästä tekstistä.
        if (_reply.value !is ReplyUiState.Sending) _reply.value = ReplyUiState.Idle
    }

    /**
     * Arkiston sisältö. Paikallinen luku, ei verkkoa, joten tämä saa alkaa itsestään.
     *
     * **Aina koko arkisto suodattimesta riippumatta.** Suodatus on esitystä ja tehdään
     * ruudulla, ja rajattu lista olisi hiljaa vajaa.
     *
     * ~~Tämä lista on myös viennin ([exportJson]) lähde.~~ **Ei ole 5.9.2026 alkaen**, ks.
     * [exportJson]. Tämä virta on ruutua varten, ja sen alkuarvo on tyhjä lista.
     */
    val messages: StateFlow<List<Message>> = byAccount { archive.observeAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Keskustelukumppanit uusin ensin, alasvetovalikon sisältö. */
    val opponents: StateFlow<List<String>> = byAccount { archive.observeOpponents(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Arkiston virta kirjautuneen tilin mukaan, tili luettuna **tilauksen alkaessa** eikä
     * näkymämallin syntyessä. Näkymämalli elää aktiviteetin mitan ja selviää `Sign out`ista,
     * joten rakentajassa luettu nimi näyttäisi edellisen tilin arkistoa seuraavalle
     * kirjautujalle. `WhileSubscribed` katkaisee tilauksen kun Inbox ei ole näkyvissä, ja
     * kirjautumisruutu on aina välissä, joten uusi tilaus lukee uuden nimen.
     */
    private fun <T> byAccount(source: (String?) -> Flow<T>): Flow<T> =
        flow { emitAll(source(self())) }

    private val _filter = MutableStateFlow(filterStore.get())

    /** Valittu pelaaja, tai null kun mitään ei rajata. Säilyy laitteella käynnistysten yli. */
    val filter: StateFlow<String?> = _filter.asStateFlow()

    fun filterChanged(opponent: String?) {
        _filter.value = opponent
        filterStore.save(opponent)
    }

    /**
     * Fraasinapit tallennusjärjestyksessä. Lista on sovelluksen yhteinen ([PhraseBook]),
     * joten laudalla tehty muutos näkyy täällä heti ja päinvastoin.
     */
    val phrases: StateFlow<List<String>> = phraseBook.phrases

    fun addPhrase(text: String) = phraseBook.add(text)

    fun removePhrase(phrase: String) = phraseBook.remove(phrase)

    private val _quote = MutableStateFlow(false)

    /**
     * Lähteekö vastauksen mukana lainaus viestistä johon vastataan (Tommin pyyntö
     * 30.8.2026: *"jotta vastaanottaja näkee mihin vastattiin"*).
     *
     * Oletus on pois päältä, koska 24.8.2026 päätös (viesti ennallaan, yhteys vain tässä
     * laitteessa `replyTo`-kenttänä) jää lähtötasoksi ja lainaus on siihen tietoinen lisä.
     * Valinta ei nollaudu viestin vaihtuessa: joka kerta lainaava käyttäjä rastittaa
     * kerran sessiossa eikä joka viestillä.
     *
     * Muoto on selainlaajennus DGText2Arean mitattu muoto (`> `-etuliite riveittäin,
     * `docs/TOINEN-ASIAKAS.md`), joten vastaanottajan silmissä lainaus näyttää samalta
     * kuin muidenkin pelaajien selaimista tuleva. Perillemeno on mitattu 24.8.2026:
     * `>` kulkee entiteettinä ja rivinvaihto säilyy `<PRE>`-lohkossa raakana.
     */
    val quote: StateFlow<Boolean> = _quote.asStateFlow()

    fun quoteChanged(enabled: Boolean) {
        _quote.value = enabled
    }

    private val _replyTarget = MutableStateFlow<Message?>(null)

    /**
     * Viesti jonka alla vastauskenttä on auki, tai null kun kenttää ei ole.
     *
     * Koko olio eikä pelkkä tunniste, jotta [sendReply] lukee lomakkeen suoraan tästä eikä
     * etsi listasta: viesti on muuttumaton, joten talteen otettu olio ei vanhene.
     */
    val replyTarget: StateFlow<Message?> = _replyTarget.asStateFlow()

    /**
     * Avaa tai sulkee vastauskentän klikatun viestin alta (Tommin pyyntö 30.8.2026).
     *
     * Viesti ilman lomaketta ei avaa mitään: sille ei ole yhtään sivulta luettua osoitetta,
     * eikä sellaista koota. Sama muoto kuin hakunapin puuttumisella, eli toiminto jota ei
     * voi tehdä turvallisesti jätetään pois.
     *
     * Luonnos **ei** tyhjene kentän siirtyessä viestiltä toiselle. Kirjoitettu teksti on
     * ainoa kopio siitä mitä käyttäjä sanoi (`SUBSTANSSI.md` kohta 50), ja väärään kohtaan
     * avattu kenttä ei saa hävittää sitä; tyhjennys on yhä sidottu vain jonon seuraavaan
     * kohteeseen ja onnistuneeseen lähetykseen.
     */
    fun openReply(message: Message) {
        if (message.replyForm == null) return
        _replyTarget.value = if (_replyTarget.value?.id == message.id) null else message
    }

    /**
     * Nykyinen arkisto vientimuodossa. Pelkkää lukua, ei verkkoa eikä kirjoitusta. Tiedoston
     * kirjoittaminen valittuun `Uri`iin on `MainActivity`n työtä, koska se vaatii
     * `ContentResolver`in eikä tämä luokka tunne Androidia laajemmin kuin muutkaan mallit.
     *
     * **Luetaan arkistosta eikä [messages]-virrasta, ja se on korjaus 5.9.2026 mitattuun
     * vikaan.** [messages] on `stateIn`, jonka alkuarvo on tyhjä lista siihen asti että Room
     * ehtii vastata. Vuorokautinen varmuuskopio laukeaa ruudun avautuessa, eli täsmälleen
     * silloin kun virta on vielä alkuarvossaan, joten se kirjoitti tiedoston jossa oli
     * `"messages": []`. Aika leimattiin silti tehdyksi, joten seuraava kopio esti itsensä
     * vuorokaudeksi ja ruutu sanoi arkiston olevan tallessa.
     *
     * Todiste on laitteelta: kannassa oli 27 viestiä, tuorein tallennettu 4.9.2026 klo 17.10,
     * ja klo 17.19 kirjoitettu tiedosto oli 114 tavua ilman yhtään viestiä.
     *
     * **Tämä on sama vikamuoto kuin `queuePathUpstream`in kertakuva** (korjattu 9.8.2026):
     * kerran luettu arvo virrasta joka ei ole vielä vastannut, luettuna vastaukseksi. Siellä
     * se sanoi ettei mitään odota, täällä että arkisto on tyhjä.
     */
    suspend fun exportJson(exportedAtEpochMillis: Long = now()): String =
        archive.observeAll(self()).first().toArchiveJson(
            exportedAtEpochMillis,
            account = self(),
            // Muistutukset ja fraasit ovat laitteen yhteiset eivätkä tilin (`docs/AVOIMET.md`
            // › Useampi tili samalla laitteella), joten ne menevät mukaan kokonaisina.
            reminders = reminders.all(),
            phrases = phraseBook.phrases.value,
        )

    private val _import = MutableStateFlow<ImportUiState>(ImportUiState.Idle)

    /** Viimeisimmän tuonnin tulos, kunnes ruutu kuittaa sen [importDismissed]illa. */
    val import: StateFlow<ImportUiState> = _import.asStateFlow()

    /**
     * Tuo tiedoston sisällön laitteelle. **Vain lisää, ei koskaan korvaa** (Tommin päätös
     * 16.9.2026): viestillä samuuden ratkaisee sisältötiiviste, muistutuksella kaikki kentät
     * ja fraasilla normalisoitu teksti. Laitteella jo oleva ei muutu miltään osin, joten
     * vanha tiedosto ei voi ohittaa uudempaa tietoa.
     *
     * Viestit kirjataan tiedoston tilille jos se on nimetty, muuten kirjautuneelle. Tiedoston
     * tili on se jonka arkisto viestit ovat, ja toisen tilin viestit eivät kuulu tämän tilin
     * listalle (arkisto on tilikohtainen); vanhassa tiedostossa (versio 1–2) tiliä ei ole,
     * ja silloin ainoa järkevä omistaja on se joka tuontia painoi.
     *
     * Lukeminen on kutsujan (tiedosto tulee `ContentResolver`ista), tämä saa tekstin.
     */
    suspend fun importJson(text: String) {
        _import.value = when (val read = withContext(io) { readArchiveJson(text) }) {
            ArchiveImport.NotAnArchive -> ImportUiState.NotAnArchive
            is ArchiveImport.Read -> {
                val file = read.file
                val account = file.account ?: self()
                val messages = withContext(io) { archive.addMissing(file.messages, account) }
                val reminderCount = withContext(io) { reminders.addMissing(file.reminders) }
                val phrases = phraseBook.addMissing(file.phrases)
                ImportUiState.Done(
                    messages = messages,
                    reminders = reminderCount,
                    phrases = phrases,
                    alreadyHere = file.messages.size - messages,
                    unreadable = read.unreadable,
                    // Toisen tilin arkisto: viestit menivät kantaan mutta eivät näy tälle
                    // tilille, ja ruudun on sanottava se ettei tuonti näytä epäonnistuneelta.
                    otherAccount = account.takeIf { it != self() },
                )
            }
        }
    }

    fun importDismissed() {
        _import.value = ImportUiState.Idle
    }

    /**
     * Hakee jonosta yhden kohteen. **Kuluttava teko, ja ainoa tie siihen.**
     */
    fun fetchNext() {
        if (_queue.value is QueueUiState.Fetching) return
        val path = _queuePath.value ?: return

        _queue.value = QueueUiState.Fetching
        viewModelScope.launch {
            _queue.value = withContext(io) {
                when (val response = pages.fetch(path)) {
                    is DgResponse.Ok -> readItem(response.html, requestedPath = path)
                    is DgResponse.Offline -> QueueUiState.Failed(Failure.Offline)
                    is DgResponse.ServerError -> QueueUiState.Failed(Failure.Server(response.code))
                    DgResponse.Sleeping -> QueueUiState.Failed(Failure.Sleeping)
                    // Tunnuksia ei poisteta täällä: CredentialsStoren omistaa TopViewModel.
                    DgResponse.AuthFailed -> QueueUiState.SessionExpired
                }
            }
        }
    }

    /**
     * Tunnistus ensin, jäsennys sitten, tallennus vasta sitten (tai ei lainkaan).
     *
     * Järjestys on merkitsevä kahdesti. Lautasivu on jonossa tavallinen, ja `InboxParser`
     * kieltäytyy siitä itse; tässä se erotetaan omaksi lopputuloksekseen, jotta käyttäjä
     * saa tietää mitä jonosta tuli. Ja viesti kirjoitetaan kantaan ennen kuin se päätyy
     * tilaan: haettua kohdetta ei saa takaisin, joten näyttäminen ennen tallennusta olisi
     * tilaisuus hävittää se lopullisesti.
     */
    private suspend fun readItem(html: String, requestedPath: String): QueueUiState {
        if (DgPages.isBoardPage(html)) {
            // Lauta jonon kärjessä tarkoittaa että viestit on luettu: sivusto tarjoilee
            // kohteet järjestyksessä ja lauta pysyy jonossa kunnes se on pelattu, joten
            // seuraava painallus toisi saman laudan (mitattu 18.9.2026, kolme painallusta,
            // sama tiiviste). Polku pois ja luettelon ilmoitus pois, jottei ruutu kutsu
            // painamaan uudelleen eikä luettelo väitä että jotain odottaa.
            _queuePath.value = null
            onMessagesDrained()
            return QueueUiState.NotAMessage(NotAMessageKind.Board)
        }
        // Otteluluettelo vastauksena tarkoittaa tyhjää jonoa, kuten `Next Game` laudalla
        // (`docs/KOHDE.md`). Ei kulunut mitään, joten polkuun ei jää mitään haettavaa:
        // nappi katoaa ja palaa vasta luettelon uudesta linkistä.
        if (DgPages.isTopPage(html)) {
            _queuePath.value = null
            return QueueUiState.Empty
        }
        // Kutsu ennen viestilukijaa, koska `InboxParser` hyväksyisi sen tuntemattomana
        // viestinä ja kirjoittaisi kantaan (mitattu 14.9.2026: viisi kaksoisriviä). Kutsu ei
        // kulu eikä vie jonoa eteenpäin, joten polku jää ennalleen ja edellinen vastaus
        // pyyhitään: uusi haku on uusi kysymys.
        InvitationParser.parse(html)?.let { invitation ->
            _invitationAction.value = InvitationActionUiState.Idle
            return QueueUiState.Invitation(invitation)
        }
        val item = InboxParser.parse(html)
            ?: return QueueUiState.NotAMessage(NotAMessageKind.Unreadable)

        val message = item.toMessage(now())

        // Uusi kohde korvaa vanhan, joten edellisen viestin luonnos ja lopputulos eivät saa
        // jäädä ruudulle. Tämä on se yksi paikka jossa luonnos tyhjennetään, ks. [draft].
        _draft.value = ""
        _reply.value = ReplyUiState.Idle

        // Seuraava kohde sivun omasta linkistä. Jos sivu ei anna sitä, entinen polku jää
        // voimaan: se on se linkki jonka Top Page antoi, eikä kumpikaan ole koottu.
        //
        // Tämä asetetaan ennen paluuta molemmissa haaroissa, koska jono eteni riippumatta
        // siitä tallennettiinko kohde.
        _queuePath.value = item.nextPath ?: requestedPath

        // **Sivuston ilmoitusta ei kirjoiteta kantaan** (Tommin päätös 16.8.2026). Peruste
        // on määrä: tiedotteita tulee muutama päivässä, ja kuukausia vanhassa arkistossa ne
        // hukuttaisivat sen yksityisen keskustelun jonka vuoksi sovellus on olemassa.
        //
        // Ehto luetaan lajista eikä otsikosta, eli samasta säännöstä jonka `InboxParser`
        // jo teki. Tuntematon laji **ei** kuulu tähän: se on määritelmällisesti se jonka
        // sääntö ei tunnistanut, joten sen pudottaminen olisi arvaus peruuttamattomasta
        // teosta.
        //
        // Hinta on nimettävä, koska se ei näy täältä: haku kulutti kohteen sivustolla, eikä
        // pudotettua saa takaisin mistään. Siksi viesti palautetaan tilassa näytettäväksi,
        // vaikka sitä ei tallenneta.
        if (message.source == MessageSource.ANNOUNCEMENT) {
            return QueueUiState.Announcement(message)
        }

        val saved = archive.archive(message, self())
        // Kenttä aukeaa juuri saapuneen viestin alle itsestään, kuten ennen klikattavuutta:
        // saapumishetki on todennäköisin vastaushetki, eikä sitä pidä joutua klikkaamaan
        // esiin. Lomakkeeton viesti sulkee kentän, koska vanha kohde ei ole enää se jota
        // ruudulla luetaan.
        _replyTarget.value = saved.takeIf { it.replyForm != null }
        return QueueUiState.Saved(saved)
    }

    /**
     * Lähettää vastauksen viestiin jonka alla kenttä on auki. **Peruuttamaton teko oikealle
     * ihmiselle**, ja siksi ehdot ovat rakenteena eivätkä ruudun varassa:
     *
     * 1. **Lomake tulee viestistä, ei parametrina.** Kohde on [replyTarget], ja sen lomake
     *    on sivulta luettu ja viestin mukana talletettu ([Message.replyForm]). Viestiin
     *    jolla ei ole lomaketta ei voi vastata, koska tästä ei silloin lähde mitään.
     *    30.8.2026 asti lomake eli vain [QueueUiState.Saved]issa; nyt se elää arkistossa,
     *    ja juuri se muutos teki arkistosta vastaamisen mahdolliseksi.
     * 2. **Osoitetta ei koota.** `ReplyForm.write` palauttaa `FormSubmission`in, jonka
     *    konstruktori on `core-domain`in sisäinen. Tyhjä teksti tuottaa nullin, eikä
     *    tekstiä leikata.
     * 3. **Yksi kerrallaan**, kuten haussa: kaksoisnapautus ei lähetä kahta viestiä.
     * 4. **Onnistuminen luetaan sivulta.** Pelkkä `Ok` ei riitä, koska palvelin vastaa
     *    200:lla myös silloin kun sivu on jotain muuta. Kuittaus tunnistetaan
     *    `DgPages.isMessageSentPage`illä, ja vasta se kirjoittaa oman viestin arkistoon.
     */
    fun sendReply() {
        if (_reply.value is ReplyUiState.Sending) return
        val target = _replyTarget.value ?: return
        val form = target.replyForm ?: return
        // Portti on kirjoitetussa tekstissä eikä kootussa: pelkkä lainaus ilman omaa
        // sanaa ei lähde, koska tyhjän luonnoksen lähetys on aina vahinko.
        if (_draft.value.isBlank()) return
        val text = replyBody(target.body, _quote.value, _draft.value)
        val submission = form.write(text) ?: return

        _reply.value = ReplyUiState.Sending
        viewModelScope.launch {
            _reply.value = withContext(io) {
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> confirm(response.html, text, target)
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

    /** `Accept Invitation`, sivun oma lomake. Peruuttamaton: ottelu alkaa. */
    fun acceptInvitation() = answerInvitation(InvitationAction.ACCEPT) { it.accept.write() }

    /** `Decline Invitation` syyn kanssa tai ilman. Peruuttamaton: kutsu poistuu. */
    fun declineInvitation(reason: String) =
        answerInvitation(InvitationAction.DECLINE) { it.decline.write(reason) }

    /** `Counter Offer` sivun omilla vaihtoehdoilla, tai ei mitään jos valinta ei ole sivun. */
    fun counterOffer(choice: InviteChoice) =
        answerInvitation(InvitationAction.COUNTER) { it.counter?.write(choice) }

    /**
     * Yksi teko kolmesta, samat ehdot kuin [sendReply]llä: lomake tulee kutsusta eikä
     * parametrina, osoitetta ei koota, yksi kerrallaan, ja vastaus luetaan sivulta.
     *
     * Onnistuessa jonon tila palaa [QueueUiState.Idle]en, koska kutsu ei ole enää se mitä
     * ruudulla luetaan; polku jää, ja seuraava haku kertoo mitä jonossa nyt on. Vastaussivu
     * on mittaamatta, joten [InvitationActionUiState.Answered] sanoo mitä sivu sanoi eikä
     * enempää.
     */
    private fun answerInvitation(
        action: InvitationAction,
        write: (ReceivedInvitation) -> FormSubmission?,
    ) {
        if (_invitationAction.value is InvitationActionUiState.Sending) return
        val invitation = (_queue.value as? QueueUiState.Invitation)?.invitation ?: return
        val submission = write(invitation) ?: return

        _invitationAction.value = InvitationActionUiState.Sending(action)
        viewModelScope.launch {
            _invitationAction.value = withContext(io) {
                when (val response = forms.send(submission)) {
                    is DgResponse.Ok -> {
                        val notice = if (DgPages.isTopPage(response.html)) {
                            null
                        } else {
                            SendResultParser.notice(response.html)?.takeIf { it.length <= INVITATION_NOTICE_MAX }
                        }
                        _queue.value = QueueUiState.Idle
                        InvitationActionUiState.Answered(action, notice)
                    }
                    is DgResponse.Offline -> InvitationActionUiState.Failed(action, Failure.Offline)
                    is DgResponse.ServerError ->
                        InvitationActionUiState.Failed(action, Failure.Server(response.code))
                    DgResponse.Sleeping -> InvitationActionUiState.Failed(action, Failure.Sleeping)
                    DgResponse.AuthFailed -> InvitationActionUiState.SessionExpired
                }
            }
        }
    }

    /** Kuittaa kutsun vastauksen ilmoituksen ruudulta. */
    fun dismissInvitationAction() {
        if (_invitationAction.value !is InvitationActionUiState.Sending) {
            _invitationAction.value = InvitationActionUiState.Idle
        }
    }

    /**
     * Kuittaussivu vastaanotettuna: oma viesti arkistoon, tai ei mitään.
     *
     * Luonnos tyhjennetään **vain onnistuessa**. Epäonnistuneessa lähetyksessä teksti on
     * ainoa kopio siitä mitä käyttäjä kirjoitti, eikä sitä hävitetä hänen puolestaan.
     */
    private suspend fun confirm(html: String, text: String, original: Message): ReplyUiState {
        if (!DgPages.isMessageSentPage(html)) return ReplyUiState.Unconfirmed

        archive.archive(
            outgoingMessage(
                // Pikaviesti ei kuulu mihinkään otteluun, ei saapuvana eikä lähtevänä.
                matchId = null,
                // Keskustelukumppani on sama henkilö kumpaankin suuntaan: se jolta viesti
                // tuli on se jolle vastaus menee. Juuri tästä syystä lähettäjä ja
                // keskustelukumppani ovat eri kentät.
                opponent = original.opponent ?: original.sender.takeIf { it.isNotBlank() },
                self = self(),
                body = text,
                // Sivun oma lause sellaisenaan (Tommin päätös 22.8.2026), esim.
                // `Your message has been sent to <nimi>`. Sovelluksen kirjoittama otsikko
                // olisi väite, tämä on sivun tekstiä.
                rawHeader = SendResultParser.notice(html).orEmpty(),
                // Sama laji kuin viestillä johon vastattiin: lähetetty pikaviesti on
                // pikaviesti. Suunta luetaan lähettäjästä, ei lajista.
                source = original.source,
                sentAtEpochMillis = now(),
                // Yhteys kirjataan tässä tai ei koskaan: sivustolla ei ole ketjuja, joten
                // tämä on ainoa hetki jona tiedetään mihin vastattiin (Tommin päätös
                // 24.8.2026, `Message.replyTo`).
                replyTo = original.id,
            ),
            account = self(),
        )
        _draft.value = ""
        return ReplyUiState.Sent
    }

    companion object {
        /**
         * Viesti lainausriveiksi: `> `-etuliite joka riville, kuten DGText2Area tekee
         * (`docs/TOINEN-ASIAKAS.md`, mitattu `> Jhgffgkk jkkjj`). Loppuun ei tule
         * rivinvaihtoa; kutsuja liittää oman tekstin perään.
         *
         * Jo lainatun lainaaminen tuottaa `> > `-rivin, ja se on tarkoitus: se on saman
         * muodon toinen kerros eikä virhe.
         */
        internal fun quoteForReply(body: String): String =
            body.trimEnd().lines().joinToString("\n") { "> $it" }

        /**
         * Se teksti joka lähtee: lainaus, rivinvaihto ja oma teksti — tai pelkkä oma teksti.
         *
         * **Yksi koti kahden sijaan (1.9.2026, auditoinnin H5).** Sääntö oli [sendReply]issä,
         * mutta ruutu laski laskuriinsa lähtevän pituuden itse muodossa `lainaus.length + 1 +
         * luonnos.length`, eli **liitosmerkin pituus oli koodattu numeroksi** toiseen
         * paikkaan. Kahdesta rivinvaihdosta tai erottimen vaihtumisesta laskuri olisi ollut
         * hiljaa väärässä, eikä mikään olisi kaatunut.
         *
         * Laskuri on tässä ruudussa oikeasti hyödyllinen, ja siksi sen on oltava oikea:
         * sivusto rivittää 80 merkin kohdalta, joten luku kertoo suunnilleen kuinka monelle
         * riville viesti perillä hajoaa.
         */
        internal fun replyBody(originalBody: String, quote: Boolean, draft: String): String =
            if (quote) quoteForReply(originalBody) + "\n" + draft else draft
    }

    class Factory(
        private val pages: PageFetcher,
        private val forms: FormSender,
        private val archive: MessageArchive,
        private val queuePathUpstream: Flow<String?>,
        private val onMessagesDrained: () -> Unit,
        private val self: () -> String?,
        private val filterStore: MessageFilterStore,
        private val phraseBook: PhraseBook,
        private val reminders: ReminderBook,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(
                pages,
                forms,
                archive,
                queuePathUpstream,
                onMessagesDrained = onMessagesDrained,
                self = self,
                filterStore = filterStore,
                phraseBook = phraseBook,
                reminders = reminders,
            ) as T
    }
}

/** Vastaussivun lauseen yläraja, sama kuin profiilin teoilla (`PageViewModel`). */
private const val INVITATION_NOTICE_MAX = 160
