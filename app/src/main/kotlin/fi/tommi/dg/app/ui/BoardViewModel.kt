package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.data.ActionQueue
import fi.tommi.dg.data.MessageArchive
import fi.tommi.dg.data.MarkBook
import fi.tommi.dg.data.ReminderBook
import fi.tommi.dg.data.MatchMemory
import fi.tommi.dg.domain.SeenMatch
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.ChatForm
import fi.tommi.dg.domain.CheckerCheck
import fi.tommi.dg.domain.CheckerPosition
import fi.tommi.dg.domain.CompositionSession
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.MarkedPosition
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.MatchOverPage
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.domain.PendingAction
import fi.tommi.dg.domain.PipCheck
import fi.tommi.dg.domain.Reminder
import fi.tommi.dg.domain.Seat
import fi.tommi.dg.domain.dependsOnAssembly
import fi.tommi.dg.domain.gameKey
import fi.tommi.dg.domain.ownLinks
import fi.tommi.dg.domain.press
import fi.tommi.dg.domain.reconcileCheckers
import fi.tommi.dg.domain.reconcilePips
import fi.tommi.dg.domain.resolveSeat
import fi.tommi.dg.domain.stillOffers
import fi.tommi.dg.domain.write
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.BoardParser
import fi.tommi.dg.scrape.ChatParser
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.InboxParser
import fi.tommi.dg.scrape.InvitationParser
import fi.tommi.dg.scrape.MatchOverParser
import fi.tommi.dg.scrape.toMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lautanäkymän tila.
 *
 * `Loaded` kantaa [BoardUiState.Loaded.refreshing]-lipun samasta syystä kuin
 * [TopUiState.Loaded]: päivitys ei saa pyyhkiä lautaa ruudulta.
 */
sealed interface BoardUiState {

    data object Loading : BoardUiState

    data class Loaded(
        val board: BoardState,
        val pips: PipCheck,
        /**
         * Nappulavahdin tulos. Erillään [pips]istä, koska se kattaa eri alueen: ulos
         * kannetut eivät tuota pipsejä, joten pip-vahti on niille sokea (mitattu 22.8.2026).
         */
        val checkers: CheckerCheck = CheckerCheck.Unavailable,
        val refreshing: Boolean = false,
        /**
         * Paikallinen kokoaminen, tai null kun lauta kulkee vanhaa reittiä. Null ei ole
         * virhe: `LocalComposition.begin` kieltäytyy aina kun jokin sen ehdoista ei
         * täyty, ja silloin napautukset seuraavat sivun linkkejä kuten ennenkin.
         * Ks. `docs/ARKKITEHTUURI.md`, paikallinen kokoaminen.
         */
        val composition: CompositionSession? = null,
        /**
         * Käyttäjän teko epäonnistui, ja ruudulla oleva lauta on tekoa **edeltävä** tila
         * jota sivusto ei ole sen jälkeen vahvistanut. Null tarkoittaa tavallista lautaa.
         *
         * Ennen 24.8.2026 teon epäonnistuminen pyyhki laudan, koska hiljaa säilynyt lauta
         * olisi luettu todisteeksi teon onnistumisesta. Nyt lauta säilyy ja tämä kenttä
         * tekee säilymisestä äänekkään: ruutu näyttää siitä ilmoituksen, eikä
         * vahvistamaton lauta ota vastaan uusia tekoja (ks. [BoardViewModel.follow] ja
         * [BoardViewModel.press]). Laitteessa jo ollut tieto ei siis enää katoa
         * lähetysyrityksen mukana (`docs/AVOIMET.md`, avattu 22.8.2026).
         */
        val unconfirmed: Failure? = null,
        /**
         * Tämän sivun keskusteluosa, tai null kun sivulla ei ole kirjoituskenttää.
         *
         * **Vastustajan viesti on jo arkistoitu siinä vaiheessa kun tämä on olemassa**, ks.
         * [BoardViewModel.readBoard]. Kenttä on siis näyttöä varten eikä säilytystä: sivu
         * katoaa lukuhetkellä, joten näytetty muttei tallennettu viesti olisi mennyt.
         */
        override val chat: ChatOnBoard? = null,
        /**
         * Kierrosrivi ottelutietoihin. Sivun oma `Round 4` täydennettynä luettelon
         * kokonaismäärällä (`Round 4/5`) kun se tunnetaan tälle ottelulle, muuten sivun
         * sana sellaisenaan. Ks. [BoardViewModel.roundLabel].
         */
        val roundLabel: String? = board.round,
    ) : BoardUiState, ActingSurface {
        override val ownLinks: Set<String> get() = board.ownLinks()
        override val matchId: MatchId get() = board.matchId
        override val blocked: Boolean get() = unconfirmed != null
        override fun pageSubmission(submit: String, verify: Boolean): FormSubmission? =
            board.form?.press(submit, verify)

        /** Skip on laudan ainoa linkki jonka koko tarkoitus on vaihtaa ottelu. */
        override fun linkChangesMatch(href: String): Boolean = href == board.skipHref
        override fun pressChangesMatch(submit: String): Boolean = submit in MATCH_CHANGING_SUBMITS
    }

    /**
     * Ottelu on päättynyt, ja sivusto vastasi päättymissivulla.
     *
     * **Oma tila eikä [NotABoard]in laji**, koska sivulla on luettavaa sisältöä ja yksi teko:
     * tulos, pisteet ja **chat-kenttä**. Ennen 31.8.2026 tämä sivu päätyi
     * [NotABoardKind.WrongPage]en, jolloin ruudulla luki *"This is no longer a board"* eikä
     * sivulta luettu mitään — ja juuri sillä sivulla kirjoitetaan kiitokset, joita sivusto ei
     * säilytä.
     */
    data class MatchOver(
        val page: MatchOverPage,
        /** Sivun keskusteluosa samalla lukijalla kuin laudalla, tai null. */
        override val chat: ChatOnBoard? = null,
    ) : BoardUiState, ActingSurface {
        override val ownLinks: Set<String> get() = page.ownLinks()
        override val matchId: MatchId? get() = page.matchId

        /** Päättyneessä ottelussa ei ole vahvistamatonta tilaa: tekoja ei ole ollut. */
        override val blocked: Boolean get() = false
        override fun pageSubmission(submit: String, verify: Boolean): FormSubmission? =
            page.form?.press(submit)

        /**
         * Päättymissivun **jokainen** teko sallii ottelun vaihtumisen, koska ratkenneessa
         * ottelussa ei ole muuta tekemistä. Laudalla vastaava sääntö on kapeampi, ja ero on
         * pinnan ominaisuus eikä poikkeus.
         *
         * Sama salliva sääntö kattaa 6.9.2026 alkaen pelin päättymissivun
         * (`MatchOverPage.matchContinues`), jonka `Next` jatkaa **samassa** ottelussa. Ehto
         * on siis sille löysempi kuin olisi pakko, ja se on tarkoituksellista: sivu tulee
         * myös vastauksena toisen ottelun `commit`iin, jolloin ruudulla oleva ottelu on eri
         * kuin se johon `Next` vie. Tiukempi ehto hylkäisi juuri sen tapauksen.
         */
        override fun linkChangesMatch(href: String): Boolean = true
        override fun pressChangesMatch(submit: String): Boolean = true
    }

    /**
     * Sivu saapui 200 OK:lla muttei ollut se lauta jota pyydettiin.
     *
     * Tämä on tietoisesti eri tila kuin [Failed]: verkkovirheen voi yrittää uudelleen, mutta
     * väärä sivu tarkoittaa että tilanne sivustolla on toinen kuin luultiin, eikä sama haku
     * korjaa sitä.
     */
    data class NotABoard(val kind: NotABoardKind) : BoardUiState

    data class Failed(val reason: Failure) : BoardUiState

    /**
     * Istunto ei kelvannut. Tunnusten poisto kuuluu [TopViewModel]ille, joka omistaa
     * `CredentialsStore`n; tämä vain kertoo tilanteesta.
     */
    data object SessionExpired : BoardUiState, SessionExpiredState
}

/**
 * Pinta jolta sivun omia tekoja voi tehdä: seurata linkkiä, painaa nappia, lähettää viesti.
 *
 * **Tämä on sääntö rakenteena eikä kommenttina, ja se kirjoitettiin mitatun virheen takia
 * (1.9.2026).** Sääntö *"tekoja tehdään vain pinnalta jonka sivu on juuri antanut"* asui
 * siihen asti kuutena erillisenä `as? BoardUiState.Loaded ?: return` -rivinä. Kun
 * päättymissivu sai oman tilansa, jokainen niistä riveistä pudotti sen hiljaa: ruutu piirsi
 * napit ja mikään napautus ei tehnyt mitään, eikä yksikään testi eikä kääntäjä huomauttanut,
 * koska mitään ei ollut rikki — teot vain eivät koskeneet uutta tilaa.
 *
 * Nyt tilaa lisäävä joutuu vastaamaan kysymykseen: **voiko tältä pinnalta tehdä tekoja?**
 * Kyllä tarkoittaa tämän rajapinnan toteuttamista, ei tarkoittaa sen jättämistä. Kumpikin on
 * päätös, eikä kumpaakaan voi tehdä vahingossa lisäämättä yhtään riviä. `ActingSurfaceTest`
 * vaatii lisäksi että jokainen uusi tila on toisessa näistä joukoista nimeltä.
 *
 * Rajapinta kantaa vain sen mikä on **yhteistä kaikille pinnoille**. Kokoaminen on laudan
 * oma piirre ja jonoon kirjaaminen laudan oma sääntö, joten ne jäävät kutsupaikkaan; ks.
 * [BoardViewModel.press].
 */
    /**
     * Napit joiden koko tarkoitus on vaihtaa ottelua, sivun omalla tekstillä.
     *
     * **Luettelo eikä ehto, ja se on tässä tietoinen valinta.** Muualla tässä projektissa
     * luetteloita vältetään, koska ne pudottavat sen mitä ei vielä tunneta, ja juuri se
     * pudotti `commit`-kentän samana päivänä. Tässä luettelo on silti oikea muoto:
     * ottelunvaihto ei ole sivun rakenteessa näkyvä ominaisuus vaan sen napin merkitys,
     * eikä merkitystä voi lukea merkkijonosta millään ehdolla. Vaihtoehto olisi päätellä
     * se vastauksesta, ja silloin tarkistus hyväksyisi ottelunvaihdon myös silloin kun
     * käyttäjä ei pyytänyt sitä. Se on tasan se tapaus jota portti on varten.
     *
     * Väärin arvattu jäsen ei ole vaarallinen kumpaankaan suuntaan: puuttuva nimi näkyy
     * turhana virheruutuna, ja ylimääräinen nimi ei ole nappi jota sivu tarjoaisi, koska
     * `BoardForm.press` kieltäytyy painamasta nappia jota sivulla ei ole.
     *
     * `To Top` ei ole listalla: se vie Top Pagelle, jolla ei ole lautaa lainkaan, joten
     * se päätyy `WrongPage`en eikä tähän tarkistukseen.
     */
private val MATCH_CHANGING_SUBMITS = setOf("Next Game")

sealed interface ActingSurface {
    /** Sivun omat seurattavat osoitteet. Muu osoite ei ole sivun oma, eikä sitä seurata. */
    val ownLinks: Set<String>

    /** Tämän sivun keskusteluosa, tai null kun sivulla ei ole kirjoituskenttää. */
    val chat: ChatOnBoard?

    /** Ottelu jota tämä pinta koskee, arkistointia varten. */
    val matchId: MatchId?

    /**
     * Teot on estetty, koska ruudulla oleva tila on vahvistamaton.
     *
     * Laudalla tämä on epäonnistunut lähetys: sivu jonka linkit luettiin on voinut muuttua,
     * joten niiden seuraaminen olisi sokea toisto (ks. [BoardUiState.Loaded.unconfirmed]).
     */
    val blocked: Boolean

    /** Lähetys sivun omasta lomakkeesta, tai null kun sivu ei tarjoa tätä nappia. */
    fun pageSubmission(submit: String, verify: Boolean): FormSubmission?

    /** Vaihtaako tämän linkin seuraaminen ottelua. */
    fun linkChangesMatch(href: String): Boolean

    /** Vaihtaako tämän napin painaminen ottelua. */
    fun pressChangesMatch(submit: String): Boolean
}

/**
 * Siirron jälkeisen sivun keskustelu sellaisena kuin ruutu sen tarvitsee.
 *
 * Kolme asiaa yhdessä, koska ne elävät saman sivun: mitä vastustaja kirjoitti, kenen kanssa
 * puhutaan, ja millä lomakkeella vastataan. Mikään näistä ei ole tallennettavaa tietoa
 * paitsi viesti, ja se on tallennettu jo ennen kuin tämä olio syntyy.
 */
data class ChatOnBoard(
    /**
     * Vastustajan viesti ilman lainattua osaa, tai null kun keskustelua ei vielä ole.
     *
     * Null ei tarkoita ettei chattia ole: sivu tarjoaa kirjoituskentän myös silloin kun
     * ketju on tyhjä, ja juuri se on keskustelun aloittaminen.
     */
    val incoming: String?,
    /**
     * Arkistoidun saapuneen viestin `Message.id`, tai null kun viestiä ei ollut.
     *
     * Kannetaan tässä eikä lasketa uudelleen, koska tunniste sisältää saapumishetken: sama
     * viesti uudelleen muodostettuna saisi eri tunnisteen, ja vastauksen `replyTo` osoittaisi
     * riviin jota ei ole.
     */
    val incomingId: String?,
    /** Kenen kanssa, laudan pelaajapaneeleista koottuna. Null kun nimeä ei saatu. */
    val opponent: String?,
    /**
     * Vastauslomake, tai null kun sivu kantaa viestin muttei kenttää.
     *
     * Mitattu 7.9.2026 (`raakasivut/sessio-7-9-yo/0062`, fixture `match_over_says.html`):
     * ottelun päättymissivu jolla vastustaja oli kirjoittanut, mutta lomakkeessa oli vain
     * `Next Game` ja `To Top`. Siihen asti tämä kenttä ei ollut null-sallittu, ja lukija
     * palautti nullin ennen arkistointia, joten **viesti ei mennyt arkistoon eikä ruudulle**
     * ja katosi sivustolta seuraavaan lukuun mennessä. Se oli ykkösominaisuuden vika.
     */
    val form: ChatForm?,
)

sealed interface NotABoardKind {

    /**
     * Vastaus oli otteluluettelo, eli tavallisin tapa poistua laudalta.
     *
     * Erotettu [WrongPage]sta 29.8.2026, ja ehto sille oli mitattu joukko eikä arvaus:
     * kolme havaittua tapausta (3.8.2026 tyhjä jono viimeisen siirron jälkeen, 28.8.2026
     * kahdesti `To Top`) olivat kaikki Top Page, eikä muita tapauksia ole havaittu
     * (`docs/AVOIMET.md`). Tunnistus on `DgPages.isTopPage`, eli sama predikaatti jota
     * `TopViewModel` käyttää: tieto siitä mikä on Top Page asuu yhä yhdessä paikassa,
     * eikä kahta totuutta synny.
     *
     * **[html] on sivu sellaisenaan, ja se kannetaan mukana 4.9.2026 alkaen** (Tommin päätös
     * illalla, mitatun 1,4 sekunnin odotuksen jälkeen). Sivusto on jo lähettänyt
     * otteluluettelon vastauksena tähän pyyntöön, ja aiemmin se heitettiin pois ja sama sivu
     * haettiin uudelleen. Odotus oli juuri se toinen haku.
     *
     * **Tämä ei ole toinen reitti samaan tilaan**, ja se oli aiemman ratkaisun peruste.
     * Tavut annetaan `TopViewModel.adopt`ille, joka lukee ne samalla lukijalla ja päätyy
     * samaan tilaan kuin oma hakunsa; vain tavujen lähde eroaa. Lauta ei jäsennä
     * otteluluetteloa eikä tiedä siitä mitään, vaan välittää sivun jota se ei itse osaa lukea.
     */
    data class TopPage(val html: String) : NotABoardKind

    /**
     * Sivulla ei ollut lautaa, eikä sivua tunnistettu miksikään tunnetuksi.
     *
     * 29.8.2026 asti tähän päätyi myös Top Page; nyt se on oma lajinsa [TopPage].
     *
     * **Korjattu 1.9.2026: tähän ei enää päädy jäsentimen null.** Tässä luki että
     * *"rikkinäinen lauta"* ja *"ei lautaa lainkaan"* eivät ole erotettavissa, mutta ne ovat:
     * `DgPages.isBoardPage` on tarkistettu **ennen** jäsentimen kutsua, joten null sen
     * jälkeen tarkoittaa lautasivua jota ei saatu luettua. Erottelu oli siis olemassa,
     * ja tämä tila väitti silti *"This is no longer a board"* sivusta jolla oli lauta.
     * Se on [BoardUnreadable], ja tämä jäi tarkoittamaan sitä mitä nimi sanoo.
     */
    data object WrongPage : NotABoardKind

    /**
     * Vastaus oli jonon viestisivu (pikaviesti tai ilmoitus), ei lauta.
     *
     * **Mitattu 13.9.2026 klo 01.00 (`sessio-13-9-yo`, rivi 8), ja se oli ykkösominaisuuden
     * vika.** `Submit Move` palautti 435 tavun `DailyGammon Quick Message` -sivun: vastustaja
     * oli lähettänyt pikaviestin, ja sivusto tarjoaa jonon kohteen minkä tahansa siirron
     * vastauksena eikä vain `/bg/nextgame`sta. Sivu päätyi [WrongPage]en, ruudulla luki
     * *"This is no longer a board"*, eikä viestiä kirjoitettu kantaan. Sivusto ei säilytä
     * sitä, joten ilman proxyn raakasivua se olisi ollut poissa. Sama tapaus on korpuksessa
     * jo 2.9.2026 (`sessio-2-9`, rivi 11), silloin huomaamatta.
     *
     * Viesti luetaan samalla lukijalla kuin jonossa (`InboxParser`) ja kirjoitetaan kantaan
     * **ennen** kuin tila asetetaan, samasta syystä kuin `MessagesViewModel.readItem`issä:
     * haettua kohdetta ei saa takaisin. Ilmoitusta ei kirjoiteta (Tommin päätös 16.8.2026),
     * ja [saved] kertoo kumpi tapahtui. Vastaaminen tapahtuu Inboxin arkistosta, jossa
     * lomake elää viestin mukana; lauta ei tarjoa toista vastausreittiä.
     */
    data class InboxItem(val message: Message, val saved: Boolean) : NotABoardKind

    /**
     * Jonon kärjessä on toisen pelaajan ottelukutsu, ja sivusto tarjosi sen siirron
     * vastauksena kuten viestisivunkin. Ei tallenneta eikä vastata täältä: kutsu ei kulu
     * (`docs/KOHDE.md`, 14.9.2026) ja siihen vastataan Inboxin jonokortista. Ilman tätä
     * lajia sivu putoaisi [WrongPage]en, joka sanoisi ettei sivu ole enää lauta ja jättäisi
     * kertomatta miksi.
     */
    data class Invitation(val from: String) : NotABoardKind

    /**
     * Sivulla **on** lauta, mutta jäsennin ei saanut siitä otetta.
     *
     * Ero [PositionUnreadable]iin on se mikä puuttui: siellä asema jäi lukematta vaikka
     * ottelu tunnistettiin, tässä ei saatu edes ottelun tunnistetta (`BoardParser` palauttaa
     * nullin vain silloin kun otsikossa eikä yhdessäkään `/bg/move/`-osoitteessa ole
     * ottelunumeroa).
     *
     * **Nimetty 1.9.2026 kompositioauditoinnin H3:n takia.** Tämä tila oli saavutettavissa
     * ilman nimeä: se näytti [WrongPage]n tekstin, joka sanoo ettei sivu ole enää lauta.
     * Väite oli väärä juuri silloin kun se näytettiin, ja väärä väite sivustosta on tässä
     * projektissa se yksi asia jota vastaan koko tunnistusketju on rakennettu.
     */
    data object BoardUnreadable : NotABoardKind

    /**
     * Sivulla oli lauta, mutta eri ottelusta kuin pyydettiin: sivusto siirtyi seuraavaan
     * peliin kun edellinen siirto oli tehty (mitattu 4.8.2026, kaksi osumaa).
     *
     * Jäsennin kestää tämän, koska se lukee ottelutunnisteen sivulta. Näkymä ei saa kestää
     * sitä, koska se piirtäisi väärän pelin oikean otsikon alle.
     */
    data class DifferentMatch(val actual: MatchId) : NotABoardKind

    /**
     * Lauta oli sivulla mutta pisteitä ei voitu numeroida, ja sivu itse kertoi syyksi
     * Mini-skeeman, joka pudottaa pistenumerorivin kokonaan.
     *
     * Tämä on **käyttöliittymän luokittelu eikä jäsentimen sopimus**: `BoardParser.parse`
     * palauttaa yhä ei-nullin laudan jonka `points` on tyhjä. Sopimusta ei muutettu, koska
     * kysymys oli väärin aseteltu: syytä ei tarvitse päätellä tyhjästä listasta, kun se on
     * luettavissa sivun kuvapoluista (`docs/AVOIMET.md`, kiinni 13.8.2026).
     *
     * Luokittelu on yksiselitteinen näyttötarkoituksessa, koska backgammonissa 30 nappulaa
     * on aina jossain: lauta jolla ei ole yhtään pistettä ei ole asema.
     */
    data object MiniScheme : NotABoardKind

    /**
     * Pisteitä ei voitu numeroida, **eikä sivu kertonut miksi**.
     *
     * Erillään [MiniScheme]stä, koska neuvo eroaa: Minin kohdalla korjaus on käyttäjän oma
     * asetus sivustolla, tässä ei tiedetä mitään korjattavaa. Ennen 13.8.2026 näitä ei
     * erotettu, ja ruutu neuvoi vaihtamaan skeeman myös silloin kun skeema ei ollut Mini.
     * Se oli oikea arvaus niin kauan kuin Mini oli ainoa tunnettu syy, ja väärä neuvo siitä
     * hetkestä kun jokin muu sivumuoto tuottaa saman tyhjän.
     *
     * Toistaiseksi tunnettuja tapauksia ei ole yhtään. Tämä on siis varaus mitatulle
     * tuntemattomalle eikä havaittu tila, ja jos se joskus näkyy laitteella, sivu kuuluu
     * ottaa talteen `raakasivut/`-kansioon.
     */
    data object PositionUnreadable : NotABoardKind

    /**
     * Ruudun avaava polku ei ollut sisäänkäynnin polku, eikä sitä haettu lainkaan.
     *
     * Koskee **vain sisäänkäyntiä**, eli sitä osoitetta jolla ruutu avataan. Sen jälkeen
     * portin kantaa [BoardViewModel.follow]in jäsenyysehto, koska sisäänkäynti on ainoa
     * kohta jossa osoite tulee navigaation läpi merkkijonona eikä laudan omasta linkistä.
     */
    data object PathNotReadOnly : NotABoardKind
}

/**
 * Mitä odottavalle teolle viimeksi tapahtui, silloin kun se ei näy laudasta itsestään.
 *
 * Yksi arvo, koska tapauksia on yksi: uudelleenyritys haki sivun ja sivu ei enää tarjonnut
 * sitä nappia. Silloin rivi poistetaan jonosta, ja ilman tätä poisto olisi äänetön. Sama
 * peruste kuin epäonnistuneen teon näkymisellä: teko jonka jälki katoaa ruudulta on
 * erottamaton teosta jota ei koskaan tehty.
 */
enum class PendingNote {
    /** Sivu ei enää tarjoa tätä nappia, joten tekoa ei toistettu ja rivi poistettiin. */
    NoLongerOffered,
}

/**
 * Lautanäkymän näkymämalli.
 *
 * **Portti purettiin 10.8.2026, ja tilalle tuli kaksi ehtoa** (Tommin päätös). Aiemmin tämä
 * luokka kieltäytyi hakemasta mitään kyselyllistä, mikä sulki myös pelaamisen: jokainen
 * kokoava askel on `?move=`. Kielto ei siirtynyt vaan korvautui:
 *
 * 1. **Osoitetta ei koota missään kohdassa.** [follow] ottaa vain osoitteen joka on juuri
 *    haetun laudan omassa linkissä sellaisenaan, ja jäsenyys tarkistetaan lähetyshetkellä
 *    eikä luoteta siihen että kutsupaikka poimi sen oikein. Ruutu voi siis pyytää vain sitä
 *    mitä sivu juuri tarjosi.
 * 2. **Lomaketta ei koota missään kohdassa.** [press] ei ota polkua vaan napin nimen, ja
 *    lähtevän lähetyksen rakentaa `BoardForm.press`, jonka paluutyypin konstruktori on
 *    `core-domain`in sisäinen. Keksittyä nappia ei voi painaa eikä keksittyä kenttää lisätä.
 *
 * Kolmas ehto ei ole portti vaan kohteliaisuus: **yksi pyyntö kerrallaan** ([inFlight]).
 * Kaksoisnapautus ei tuota kahta tekoa, ja se on tässä tärkeämpää kuin muualla, koska teot
 * eivät ole peruttavissa.
 *
 * Se mikä ei muuttunut: mitään ei tehdä itsestään. Ei ajastusta, ei ennakointia, ei
 * automaattista uusintaa. Jokainen pyyntö on käyttäjän ele.
 *
 * Oma luokkansa eikä [TopViewModel]in laajennus, ja syy on vahvempi kuin testihygienia:
 * `TopViewModel` **väittää** hakevansa oma-aloitteisesti vain Top Pagen, ja
 * `TopViewModelTest` todistaa sen kirjaamalla jokaisen pyydetyn polun. Toinen haku samassa
 * luokassa muuttaisi todistetun väitteen kommentiksi. Erillään kummankin polkulista on
 * täydellinen lausuma omasta luokastaan.
 *
 * [playPath] tulee sellaisenaan `Match.playPath`ista: sitä ei koota, normalisoida eikä
 * yhdistetä. Se on sääntö "lue linkki, älä laske sitä" konstruktoriparametrina.
 *
 * **Neljäs ehto tuli 10.8.2026: epäonnistunut teko ei katoa** ([queue]). Laiteajossa
 * `Submit Move` päätyi `DgResponse.Offline`en eikä siirtoa ollut laudalla, eikä siitä jäänyt
 * mihinkään jälkeä. Jono ei kuitenkaan lähetä mitään itsestään: se on kirjanpitoa, ja
 * rivi poistuu vasta kun sama lauta on haettu tuoreena ja sivu itse vastaa teon kohtaloon.
 * Ks. [resolvePending].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModel(
    private val pages: PageFetcher,
    /**
     * Käyttäjän tekojen lähetys. Erillään [pages]ista, jotta signatuurista näkee että tämä
     * luokka voi tehdä sivustolla jotain; muut näkymämallit eivät saa tätä lainkaan.
     */
    private val forms: FormSender,
    /**
     * Epäonnistuneiden tekojen kirjanpito. Sama luokka on käytettävissä muillekin ruuduille,
     * mutta vain tämä kirjoittaa siihen, koska vain tämä tekee sivustolla tekoja.
     */
    private val queue: ActionQueue,
    /**
     * Pelaajan omat muistiinpanot. **Ei kyky sivustolla**, toisin kuin [forms]: tämä ei
     * tuota yhtään pyyntöä, joten sen saaminen ei laajenna sitä mitä tämä luokka voi tehdä
     * kohteessa. Ks. [ReminderBook].
     */
    private val reminderBook: ReminderBook,
    /**
     * Merkityt asemat. **Ei kyky sivustolla** kuten [reminderBook], ja eri elinkaari:
     * merkki luetaan ottelun jälkeen eikä katoa pelin vaihtuessa. Ks. [MarkBook].
     */
    private val markBook: MarkBook,
    /**
     * Ottelumuisti. **Ei kyky sivustolla** kuten [reminderBook]: kirjoitetaan jokaisesta
     * luetusta laudasta vastustaja ja kierros, jotta turnauslista voi nimetä ottelun jota
     * luettelo ei enää näytä. Null testeissä joita muisti ei koske. Ks. [MatchMemory].
     */
    private val matchMemory: MatchMemory? = null,
    /**
     * Viestiarkisto. **Ei kyky sivustolla** kuten [reminderBook], mutta eri syystä tärkeä:
     * ottelun chat on se puoli jota sivusto ei säilytä lainkaan, ja tämä ruutu on ainoa
     * paikka jossa se on luettavissa. Ilman tätä sivu näyttäisi viestin ja heittäisi sen pois.
     */
    private val archive: MessageArchive,
    /**
     * Kirjautuneen käyttäjän nimi, tai null kun sitä ei tiedetä. Tarvitaan vain siihen että
     * laudan kahdesta pelaajapaneelista tunnistetaan kumpi on vastustaja. Sama muoto ja
     * sama syy kuin `MessagesViewModel`issa.
     */
    private val self: () -> String?,
    private val playPath: String,
    /**
     * Se ottelu jota käyttäjä luuli avaavansa. Käytetään **vain** vertailuun jäsennyksen
     * jälkeen, ei koskaan osoitteen rakentamiseen.
     */
    private val expectedMatchId: MatchId?,
    /**
     * Kierros otteluluettelon sarakkeesta muodossa `3/5` ottelutunnisteella, tai null kun
     * luettelo ei tiennyt sitä. Lautasivu itse sanoo vain `Round 4`, ja Tommi pyysi 8.9.2026
     * kesken pelisession ottelutietoihin muodon `Round 3/5`. Haku tehdään **sivun omalla
     * tunnisteella**, joten `Next Game` -ketjussa tullut ottelu saa oman kokonaismääränsä
     * eikä napautetun ottelun; ensimmäinen versio vei arvon reitillä ja kattoi vain
     * napautetun (`sessio-8-9-yo5`). Funktio eikä arvo, ks. `ListedRounds`.
     */
    private val listedRound: (MatchId) -> String? = { null },
    /**
     * Kello omana parametrinaan, jotta kirjatun teon hetki on testissä kiinnitettävissä.
     * Sama muoto kuin `MessagesViewModel`issa ja samasta syystä.
     */
    private val now: () -> Long = System::currentTimeMillis,
    /**
     * Pelataanko pakolliset askeleet valmiiksi (laitteen asetus, `docs/ASETUKSET.md` luku 3).
     * Luetaan laudan avautuessa eikä oteta talteen, samoin kuin muut laiteasetukset
     * lautanäkymässä: asetus vaihdetaan toisessa ruudussa ja lauta koostuu uudestaan.
     * Funktio eikä arvo, jotta näkymämalli ei kanna vanhentunutta kopiota. Oletus pois.
     */
    private val playForcedSteps: () -> Boolean = { false },
    /** Ahne uloskanto ilman kontaktia, sama laji ja lukutapa kuin [playForcedSteps]. */
    private val playGreedyBearoff: () -> Boolean = { false },
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow<BoardUiState>(BoardUiState.Loading)
    val state: StateFlow<BoardUiState> = _state.asStateFlow()

    private val _note = MutableStateFlow<PendingNote?>(null)
    val note: StateFlow<PendingNote?> = _note.asStateFlow()

    /**
     * Mitä ottelua seurataan jonossa.
     *
     * Alkuarvo on kutsujan antama odotus, mutta se **korvautuu sivun omalla tunnisteella**
     * heti kun lauta on jäsennetty. Syy on mitattu: pyydetty ottelutunniste ei määrää
     * palautettua ottelua, ja profiilin kautta avatulla ottelulla odotusta ei ole lainkaan.
     * Kirjaus tehdään joka tapauksessa sivun tunnisteella, joten myös seuranta tehdään sillä.
     */
    private val watchedMatch = MutableStateFlow(expectedMatchId)

    /**
     * Se peli jonka muistutukset ovat näkyvissä, tai `null` kun tunnistetta ei ole.
     *
     * Arvo tulee laudalta jokaisella onnistuneella jäsennyksellä, ja **pelin vaihtuminen on
     * pelkkä uusi arvo tässä**: pisteiden muuttuessa avain muuttuu, kysely osuu toisiin
     * riveihin, ja edellisen pelin muistutukset lakkaavat näkymästä ilman että mitään
     * poistetaan. Elinkaari on siis tässä sijoituksessa eikä missään ajastuksessa.
     *
     * `null` tarkoittaa ettei sivu kertonut molempia pisteitä, ks. [fi.tommi.dg.domain.GameKey].
     * Silloin muistutuksia ei näytetä eikä oteta vastaan, koska väärään peliin kirjattu
     * muistutus olisi pahempi kuin puuttuva muistutus.
     */
    private val currentGame = MutableStateFlow<GameKey?>(null)

    /**
     * Tämän pelin muistutukset. Virtana samasta syystä kuin [pending]: kertakuva olisi tyhjä
     * juuri sillä hetkellä kun ruutu avataan, ja tyhjä lista on tässä väite eikä odotus.
     */
    val reminders: StateFlow<List<Reminder>> = currentGame
        .flatMapLatest { game -> if (game == null) flowOf(emptyList()) else reminderBook.observe(game) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Tämän pelin merkityt asemat laudan alle, jotta painallus näkyy tehdyksi. Sama
     * avain kuin muistutuksilla, mutta lista on vain näkymä: kannassa rivi elää ottelun
     * yli ja luetaan otteluluettelon linkistä, ks. [MarkBook].
     */
    val marks: StateFlow<List<MarkedPosition>> = currentGame
        .flatMapLatest { game -> if (game == null) flowOf(emptyList()) else markBook.observe(game) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Se ottelu jota vastaan saatu sivu tarkistetaan. Alkuarvo on kutsujan odotus, ja se
     * muuttuu **vain** kun käyttäjä on itse painanut nappia joka vaihtaa ottelun.
     *
     * **Miksi tämä ei ole enää pelkkä [expectedMatchId] (22.8.2026).** Portti torjui `Next
     * Game`n, eli teon jonka koko tarkoitus on vaihtaa ottelua. Sivustolla teko meni läpi ja
     * ruutu sanoi silti että väärä ottelu palautui, joten sovellus teki oikein sivustolla ja
     * väärin ruudulla. Odotus ei siis ollut väärä käsite vaan liian pysyvä.
     *
     * **Rajaus on tarkka, koska portin alkuperäinen tapaus on yhä voimassa.** 4.8.2026
     * mitattiin että sivusto siirtyy seuraavaan peliin **itse** kun siirto on tehty, ja se
     * torjutaan jatkossakin: silloin käyttäjä pyysi siirtoa eikä ottelunvaihtoa, ja väärän
     * pelin piirtäminen oikean otsikon alle olisi hiljainen virhe. Ero näiden kahden välillä
     * ei ole vastauksessa vaan pyynnössä, ja siksi se luetaan painetusta napista.
     */
    private var expectedMatch: MatchId? = expectedMatchId

    /**
     * Se lauta jonka [refresh] hakee, eli **se lauta joka on ruudulla** eikä se jolla ruutu
     * avattiin.
     *
     * **Miksi tämä ei ole [playPath] (23.8.2026).** Mitattu tapaus: Quastelin ottelu avattiin
     * listasta, `Next Game` siirsi Navakingin otteluun, ja siellä painettu päivitys haki
     * `/bg/move/5312554/1089`, eli tavun tarkkuudella sen osoitteen jolla ruutu oli avattu
     * kaksi minuuttia aiemmin. Sivusto palautti täsmälleen mitä pyydettiin, ja ottelun
     * vahti torjui sen oikein. Vika ei siis ollut vahdissa vaan siinä että päivitys tarkoitti
     * "palaa sinne mistä tulin".
     *
     * **Arvo luetaan lomakkeen actionista eikä koota.** Se on sama sääntö kuin [follow]illa,
     * ja sama lähde jota `BoardParser` jo käyttää ottelutunnisteen varalähteenä. Kelpuutus on
     * [isEntryPath], joten kyselyllinen osoite ei voi päätyä tänne: kokoava askel on `?move=`,
     * ja sen uusiminen olisi kuluttava teko.
     *
     * **Rajaus, ja se on sivuston ominaisuus eikä tämän ratkaisun puute.** Lauta jolla ei ole
     * vuoroa ei sisällä yhtään `/bg/move/`-osoitetta, ei linkkinä eikä lomakkeena, joten sen
     * omaa polkua ei ole mistä lukea. Silloin tämä säilyttää edellisen arvonsa, ja
     * ottelun vahti on yhä se joka estää väärän laudan piirtymisen oikean otsikon alle.
     */
    private var currentPath: String = playPath

    /**
     * Tämän ottelun odottava teko, tai `null` kun mitään ei odota.
     *
     * Virtana eikä kertakuvana, ja syy on sama kuin viestiruudun jonopolulla: kertakuva olisi
     * ollut null aina kun ruutu avataan ennen kuin kanta on ehtinyt vastata, eli ruutu olisi
     * sanonut ettei mitään odota. Se on hiljainen väärä nolla, ja se osuu aina suuntaan
     * "tätä ei ole".
     */
    val pending: StateFlow<PendingAction?> = watchedMatch
        .flatMapLatest { id -> if (id == null) flowOf(null) else queue.observeByMatch(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Yksi pyyntö kerrallaan. Lipun asetus on Mainissa eikä [io]ssa, joten kaksi napautusta
     * peräkkäin näkee saman arvon: Compose kutsuu näitä Mainista.
     */
    private var inFlight = false

    /**
     * Onko tältä laudalta yritetty tekoa sivustolla.
     *
     * Paluu lukee tämän: teon jälkeen otteluluettelon rivi on varmasti vanhentunut, joten
     * käsin tehty paluu hakee luettelon uudestaan (`docs/UI.md`, poikkeus 16.9.2026).
     * Katsomiskäynti jättää tämän epätodeksi, eikä luetteloa silloin haeta.
     *
     * **Yritys riittää, vastausta ei odoteta.** Verkkoon kaatunut lähetys on voinut mennä
     * perille (siksi on `PendingAction`), joten rivi on epävarma jo yrityksestä, ja epävarma
     * rivi on sama asia kuin vanhentunut. Haku ja päivitys eivät ole tekoja, eivätkä ne
     * aseta tätä.
     */
    var actedOnSite: Boolean = false
        private set

    init {
        load(isRefresh = false)
    }

    /** Päivitys vain käyttäjän pyynnöstä. Ei kyselyä, ei ajastusta, ei ennakointia. */
    fun refresh() = load(isRefresh = true)

    /**
     * Yhteys palasi. Sama ehto ja sama perustelu kuin `TopViewModel.onNetworkAvailable`illa,
     * eikä sitä toisteta tässä: haku uusitaan vain jos edellinen kaatui verkkoon. Verkkoon
     * kaatunut voi olla joko haku ([BoardUiState.Failed]) tai teko, jonka jäljiltä lauta
     * säilyi vahvistamattomana ([BoardUiState.Loaded.unconfirmed]); kummassakin haetaan
     * lautasivu **kerran**, ei ajastetusti. Ajastettua pollausta ei ole Tommin päätöksellä
     * 24.8.2026: pelaajalle kerrotaan tilanne (ks. `BoardScreen`), palvelinta ei kuormiteta.
     *
     * **Odottavaa tekoa ei yritetä uudelleen tästä.** Se on tämän kohdan tärkein rajaus.
     * Jonossa oleva teko on lähettävä ja peruuttamaton, ja sen uusiminen on käyttäjän
     * päätös, ei verkon paluun seuraus. Yhteyden palaaminen saa siis
     * hakea sivun, ei tehdä sivustolla mitään.
     */
    fun onNetworkAvailable() {
        when (val current = _state.value) {
            is BoardUiState.Failed -> if (current.reason is Failure.Offline) refresh()
            is BoardUiState.Loaded -> if (current.unconfirmed is Failure.Offline) refresh()
            else -> Unit
        }
    }

    private fun load(isRefresh: Boolean) {
        // Sisäänkäynti hakee sen osoitteen jolla ruutu avattiin, päivitys sen laudan joka on
        // ruudulla. Ehto on molemmilla sama eikä sitä ohiteta kummallakaan.
        val path = if (isRefresh) currentPath else playPath
        if (!isEntryPath(path)) {
            // Pyyntöä ei tehdä lainkaan, eikä sitä siirretä myöhemmäksi.
            _state.value = BoardUiState.NotABoard(NotABoardKind.PathNotReadOnly)
            return
        }
        request(
            keepBoardOnFailure = isRefresh,
            resolvePendingFor = path,
            actsOnSite = false,
            mita = if (isRefresh) "refresh" else "load",
        ) { pages.fetch(path) }
    }

    /**
     * Seuraa laudan omaa linkkiä, eli kokoaa siirtoa tai antaa muun sivun tarjoaman komennon.
     *
     * **Jäsenyys tarkistetaan täällä eikä luoteta kutsupaikkaan.** Ruutu voisi periaatteessa
     * antaa minkä tahansa merkkijonon, ja juuri siksi ehto on tässä: ainoa hyväksytty arvo on
     * osoite joka on juuri jäsennetyn laudan omassa linkissä sellaisenaan. Kokoamista ei ole
     * missään kohdassa, ei täällä eikä ruudulla.
     *
     * Tunnistamaton osoite ei tuota virhettä vaan **ei tee mitään**, ja se on tietoinen
     * valinta: tilanne tarkoittaa että ruutu ja malli ovat eri mieltä laudasta, ja silloin
     * ainoa oikea teko on olla lähettämättä mitään.
     */
    fun follow(href: String) {
        // Yksi portti kaikille teoille, ks. [ActingSurface]. Tässä oli ennen kaksi erillistä
        // `as?`-riviä, ja jälkimmäinen oli lisättävä käsin kun päättymissivu sai oman tilansa.
        val surface = acting() ?: return

        // Paikallisen kokoamisen synteettiset osoitteet siepataan ennen jäsenyysehtoa,
        // eivätkä ne tuota pyyntöä: askel, peruminen ja noppajärjestyksen vaihto ovat
        // puhtaita tilamuutoksia. Etuliite jota sessio ei tunne putoaa läpi jäsenyysehtoon,
        // joka hylkää sen, joten "local:" ei voi ikinä lähteä verkkoon. Kokoaminen on laudan
        // oma piirre, joten se on tässä eikä rajapinnassa.
        if (surface is BoardUiState.Loaded) {
            val composition = surface.composition
            if (composition != null && href.startsWith("local:")) {
                val next = when {
                    href == CompositionSession.LOCAL_UNDO -> composition.undo()
                    href == CompositionSession.LOCAL_SWAP -> composition.swap()
                    href.startsWith(CompositionSession.LOCAL_STEP_PREFIX) ->
                        href.removePrefix(CompositionSession.LOCAL_STEP_PREFIX)
                            .toIntOrNull()
                            ?.let { composition.step(it) }
                    else -> null
                } ?: return
                _state.value = surface.copy(composition = next)
                return
            }
        }

        // Jäsenyysehto ja ottelunvaihdon ehto luetaan pinnalta. Ehto luetaan painetusta
        // osoitteesta eikä vastauksesta: muu ottelu jossakin muussa vastauksessa on yhä
        // hiljainen virhe ja torjutaan.
        if (href !in surface.ownLinks) return
        request(
            keepBoardOnFailure = false,
            allowMatchChange = surface.linkChangesMatch(href),
            mita = "follow",
        ) { pages.fetch(href) }
    }

    /**
     * Pinta jolta juuri nyt voi tehdä tekoja, tai null.
     *
     * **Tämän funktion olemassaolo on H1:n korjaus.** Ehto on yhdessä paikassa, joten uusi
     * tila ei voi jäädä hiljaa tekojen ulkopuolelle eikä vahvistamattoman tilan portti voi
     * jäädä kopioimatta yhteen kolmesta tekofunktiosta.
     */
    private fun acting(): ActingSurface? =
        (_state.value as? ActingSurface)?.takeIf { !it.blocked }

    /**
     * Painaa yhtä sivun tarjoamista napeista. **Peruuttamaton teko oikeassa ottelussa.**
     *
     * Parametri on napin nimi eikä osoite, ja lähtevän lähetyksen rakentaa `BoardForm.press`.
     * Sitä ei voi ohittaa: [FormSender] ottaa vastaan vain tyypin jonka konstruktori on
     * `core-domain`in sisäinen. Nappi jota sivu ei tarjoa ei siis mene läpi täältä eikä
     * mistään muualtakaan.
     *
     * [verify] on sivun oma valintaruutu ja tulee käyttäjältä. Sitä ei aseteta täällä
     * oletuksena, koska ruudun tarkoitus on nimenomaan olla se toinen ele ennen
     * peruuttamatonta tekoa.
     */
    fun press(submit: String, verify: Boolean = false) {
        val surface = acting() ?: return

        // Paikallisesti koottu siirto lähtee session omasta lähetyksestä, joka on olemassa
        // vain kun vuoro on täysi. Kokoaminen on laudan oma piirre, joten se on tässä eikä
        // rajapinnassa.
        if (surface is BoardUiState.Loaded) {
            val composition = surface.composition
            if (composition != null) {
                if (submit != CompositionSession.SUBMIT_MOVE) return
                val submission = composition.submission() ?: return
                val attempt = PendingAction(
                    matchId = surface.board.matchId,
                    boardPath = currentPath,
                    submit = submit,
                    pendingMove = composition.letters,
                    verified = false,
                    createdAtEpochMillis = now(),
                )
                request(
                    keepBoardOnFailure = false,
                    after = { response -> afterPress(attempt, response) },
                    mita = "press $submit (koottu)",
                ) {
                    forms.send(submission)
                }
                return
            }
        }

        val submission = surface.pageSubmission(submit, verify) ?: return

        // **Jonoon kirjataan vain pelattu teko, ja se on päätös eikä unohdus.** Jonon
        // tarkoitus on teko joka voi olla mennyt perille vaikka vastaus katosi
        // (`PendingAction`). Päättymissivun napissa ei ole sitä riskiä: ottelu on jo
        // ratkennut, eikä sivun viesti ole menetettävissä, koska chat arkistoitiin
        // lukuhetkellä. Epäonnistuminen näkyy virheruutuna ja korjautuu hakemalla sivu
        // uudestaan. Ero on siis pinnan ominaisuus, ja siksi se luetaan pinnasta.
        val attempt = (surface as? BoardUiState.Loaded)?.let { loaded ->
            // Kirjattava kuvaus muodostetaan **ennen lähetystä**, koska sen ainesosat luetaan
            // siltä laudalta jota käyttäjä katsoi. Lähetyksen jälkeen ruudulla on jo toinen
            // lauta, eikä kokoamistilaa saisi enää mistään.
            PendingAction(
                matchId = loaded.board.matchId,
                boardPath = currentPath,
                submit = submit,
                pendingMove = loaded.board.form?.pendingMove,
                verified = verify,
                createdAtEpochMillis = now(),
            )
        }

        request(
            keepBoardOnFailure = false,
            allowMatchChange = surface.pressChangesMatch(submit),
            after = { response -> attempt?.let { afterPress(it, response) } },
            mita = "press $submit",
        ) {
            forms.send(submission)
        }
    }

    /**
     * Lähettää viestin ottelun chat-lomakkeella ja painaa samalla sivun omaa nappia.
     *
     * **Peruuttamaton teko oikealle ihmiselle**, ja lisäksi se päättää vuoron: sivun
     * lomake on sama jolla siirrytään eteenpäin, joten `Next Game` vie jonossa seuraavaan
     * otteluun ja `To Top` poistuu laudalta. Siksi nappi tulee käyttäjältä eikä täältä, ja
     * sen jäsenyyden tarkistaa `ChatForm.write` samalla portilla kuin `BoardForm.press`.
     *
     * **Lainausvalinta tulee ruudulta eikä oletuksesta.** Sivun ruutu on valmiiksi
     * valittuna, ja juuri se tekee edellisestä viestistä näkyvän vastaanottajalle. Arvo
     * kulkee siis käyttäjän valintana läpi, eikä sitä aseteta täällä kumpaankaan suuntaan.
     *
     * **Mitä tämä ei tee, ja molemmat ovat päätöksiä.** Epäonnistunutta lähetystä ei kirjata
     * jonoon eikä yritetä uudelleen: haku tuo sivun tuoreena (ks. [resolvePending]),
     * ja tuore sivu ei enää kanna tätä viestiä, joten toisto lähettäisi tekstin sokeasti
     * uudelleen tai ei ollenkaan. Eikä onnistumista voi lukea sivulta: chatilla ei ole
     * `DgPages.isMessageSentPage`in kaltaista kuittaussivua, joten oma viesti kirjataan
     * arkistoon `Ok`-vastauksella. Se on heikompi tae kuin pikaviestillä, ja se on tässä
     * sanottu ääneen eikä piilotettu.
     */
    fun sendChat(text: String, quote: Boolean, submit: String) {
        val surface = acting() ?: return
        val chat = surface.chat ?: return
        // Lomakkeeton chat on pelkkä näyttö: siltä ei voi lähettää, ja ruutu ei tarjoa nappia.
        val form = chat.form ?: return
        val submission = form.write(text, quote, submit) ?: return

        request(
            keepBoardOnFailure = false,
            allowMatchChange = surface.pressChangesMatch(submit),
            after = { response ->
                // Tyhjä teksti on poistuminen ilman viestiä eikä viesti (`ChatForm.write`),
                // joten arkistoon ei kirjata riviä jolla ei ole runkoa. Portti on tässä eikä
                // lomakkeessa, koska lomakkeen on lähetettävä kenttä myös tyhjänä.
                if (response is DgResponse.Ok && text.isNotBlank()) {
                    archiveOwnChat(text, chat, surface.matchId)
                }
            },
            mita = "sendChat $submit",
        ) {
            forms.send(submission)
        }
    }

    /**
     * Oma lähetetty viesti arkistoon. Kirjataan tässä tai ei koskaan: sivusto ei säilytä
     * chattia, eikä lähetettyä tekstiä ole luettavissa mistään jälkikäteen.
     */
    private suspend fun archiveOwnChat(text: String, chat: ChatOnBoard, matchId: MatchId?) {
        archive.archive(
            outgoingMessage(
                matchId = matchId,
                // Keskustelukumppani on sama kumpaankin suuntaan, lähettäjä ei. Juuri tästä
                // syystä ne ovat eri kentät (`Message.opponent`).
                opponent = chat.opponent,
                self = self(),
                body = text,
                // Sivu ei anna lähetykselle omaa otsikkoa, toisin kuin pikaviestin
                // kuittaussivu. Tyhjä on siis sivun tila eikä puuttuva jäsennys.
                rawHeader = "",
                source = MessageSource.GAME_MESSAGE,
                sentAtEpochMillis = now(),
                // Yhteys kirjataan tässä tai ei koskaan, samoin kuin pikaviestissä. Null kun
                // tämä aloittaa keskustelun: sivustolla ei ole ketjuja, joten vastattavaa ei
                // silloin ole. Vastatun viestin tunniste on sen oma sisältötiiviste.
                replyTo = chat.incomingId,
            ),
            account = self(),
        )
    }

    /**
     * Ratkaiseeko juuri saatu sivu odottavan teon, eli poistuuko rivi jonosta lähettämättä.
     *
     * **Tämä korvasi `Try again` -napin** (Tommin päätös 8.9.2026, *"Refresh korvaa sen"*).
     * Nappi teki kaksi asiaa yhdessä eleessä: haki sivun tuoreena ja lähetti sitten uudestaan.
     * Ensimmäinen puolisko oli se joka teki työn, ja se tapahtuu joka tapauksessa jokaisella
     * haulla. Jälkimmäinen ei ollut tarpeen: jos sivu yhä tarjoaa napin, se on laudalla ja
     * pelaaja painaa sitä itse. Nappi joka tekee sen minkä `Refresh` jo teki on kaksi reittiä
     * samaan paikkaan, ja katkotilassa se oli niistä huomiota vievempi.
     *
     * **Mitään ei lähetetä**, ja se on koko kohdan ehto. Tämä vain toteaa: sivu itse vastaa
     * siihen kysymykseen jota verkkokerros ei voi vastata (`DgResponse.Offline` voi osua joko
     * ennen tai jälkeen sen kun palvelin luki pyynnön). Kun nappia ei enää tarjota, tilanne on
     * toinen kuin painallushetkellä, ja toistokelvoton rivi jäisi jonoon ikuisesti.
     * 24.8.2026 rajaus pysyy siis voimassa sellaisenaan: haku hakee sivun, ei tee sivustolla
     * mitään.
     *
     * Kolme ehtoa, ja jokainen sulkee yhden väärän tyhjennyksen:
     *
     * - **Vain oikeasti saatu sivu kelpaa todisteeksi.** Epäonnistunut haku säilyttää vanhan
     *   laudan ruudulla, eikä vanhaa lautaa saa lukea tähän: se vastaisi kysymykseen eilisen
     *   sivun tiedoilla.
     * - **Vain sama lauta.** Haettu polku on oltava se jolta teko lähti. Saman ottelun toinen
     *   peli ei tarjoa nappia, eikä se ole todiste mistään.
     * - **Kokoamisesta riippuva teko ratkeaa haulla ilman `stillOffers`-ehtoa.** Haku
     *   nollaa kokoamisen palvelimella (mitattu 10.8.2026), joten `stillOffers` olisi aina
     *   false eikä kertoisi teon kohtalosta mitään. Siksi rivi odotti 8.9.–16.9.2026
     *   `Discard`ia. **`Discard` poistui 16.9.2026** (Tommin tilaus ensimmäisen oikean
     *   laukeamisen jälkeen, `sessio-16-9-ilta3`: *"Discard-nappi on turha, palkkiin jää
     *   vain Refresh-neuvo"*). Palkin oma neuvo on *"Refresh the board: if it is still your
     *   turn, make the move once more"*, ja tuore lauta on se joka vastaa: siirto joko näkyy
     *   siinä tai nopat odottavat yhä. Rivi ei kerro enempää, joten se poistuu samalla
     *   haulla, ilman ilmoitusta koska lauta itse on ilmoitus. Sovellus ei tee sivustolla
     *   mitään tässäkään.
     */
    private suspend fun resolvePending(response: DgResponse, haettu: String) {
        if (response !is DgResponse.Ok) return
        val board = (_state.value as? BoardUiState.Loaded)?.board ?: return
        val action = pending.value ?: return
        if (action.boardPath != haettu) return
        if (action.dependsOnAssembly) {
            queue.clear(action.id)
            return
        }
        if (board.stillOffers(action)) return
        queue.clear(action.id)
        _note.value = PendingNote.NoLongerOffered
    }

    /**
     * Mitä painalluksen jälkeen kirjataan jonoon.
     *
     * **Vain [DgResponse.Offline] kirjataan**, ja rajaus on tarkoituksellinen. Se on ainoa
     * vastaus joka jättää epäselväksi tapahtuiko teko: palvelinvirhe ja katkennut istunto
     * ovat molemmat palvelimen omia vastauksia, eli yhteys toimi ja pyyntö tuli käsitellyksi.
     * Onnistunut teko puolestaan tekee aiemman rivin tarpeettomaksi, koska sivun tila liikkui.
     *
     * Rajaus vahvistettu 17.9.2026 (Tommin päätös *"palkki vain yhteyskatkosta"*) sen jälkeen
     * kun mittausproxyn oma 502 näytti ruudulla sirun mutta ei palkkia (`sessio-17-9-ilta`).
     * Ilman proxya sama katko olisi [DgResponse.Offline], ja palkki tulisi.
     */
    private suspend fun afterPress(attempt: PendingAction, response: DgResponse) {
        when (response) {
            // Syy kirjataan jo tähän ensimmäiseen riviin eikä vasta uusintaan. Se on se
            // katkos jonka käyttäjä koki, ja ilman sitä rivi olisi tyhjä juuri siltä osin
            // jota se on olemassa kertomaan. Yrityslaskuri ei kasva, koska sen merkitys on
            // muualla: se kertoo montako kertaa palvelinta on kuormitettu uusinnalla.
            is DgResponse.Offline ->
                queue.record(attempt.copy(lastErrorText = offlineReason(response)))
            is DgResponse.Ok -> pending.value?.let { queue.clear(it.id) }
            else -> Unit
        }
    }

    /**
     * Kirjoittaa muistutuksen tälle pelille.
     *
     * **Teksti tulee ruudulta myös pikanapin tapauksessa**, eikä sitä ole vakiona täällä.
     * Syy on että muistutus on käyttäjälle näkyvää tekstiä, ja tämän sovelluksen
     * käyttöliittymä on englanniksi resurssitiedostossa (`CLAUDE.md`). Vakio täällä olisi
     * ainoa käyttöliittymän teksti joka asuu koodissa.
     *
     * Ilman pelin tunnistetta ei tehdä mitään. Se ei ole virhetilanne vaan sama rajaus kuin
     * näkyvyydessä: ruutu ei tarjoa kenttää silloin kun tunnistetta ei ole.
     */
    fun addReminder(text: String) {
        val game = currentGame.value ?: return
        viewModelScope.launch { reminderBook.add(game, text, now()) }
    }

    /** Muistutus pois. Käyttäjän oma rivi ja hänen oma poistonsa, ks. [ReminderBook]. */
    fun removeReminder(id: Long) {
        viewModelScope.launch { reminderBook.remove(id) }
    }

    /**
     * Merkitsee ruudulla olevan aseman ottelun jälkeistä analyysia varten.
     *
     * **Siirtonumero luetaan ladatulta laudalta eikä anneta kutsujana**, samasta syystä
     * kuin muistutuksen pistepari: pyydetty ei määrää saatua. Ilman pelin tunnistetta tai
     * siirtonumeroa ei tehdä mitään, eikä se ole virhe vaan sama rajaus kuin
     * näkyvyydessä: ruutu ei tarjoa nappia silloin kun jompikumpi puuttuu. Väärään
     * kohtaan kirjattu merkki olisi analyysissa pahempi kuin puuttuva.
     *
     * **Asema tallennetaan mukaan** (16.9.2026), koska se on ainoa tieto joka kohdistaa
     * merkin `.mat`-tiedoston siirtoon (`SgfExport`). Istuin luetaan pip-vahdista
     * ([resolveSeat]); kun pipit ovat piilossa tai tasan ilman noppaa, väri luetaan
     * nimisolun taustasta ja suunta oletetaan katsojan (koti 1..6), joka on mitattu
     * fixtureista kummallakin värillä (`move_board.html`, `move_board_oikea.html`). Väärä
     * suunta ei kohdistaisi merkkiä väärin vaan jättäisi sen pelin alkuun, koska peilattu
     * asema ei täsmää mihinkään toiston asemaan. Null asema kelpaa: merkki menee silloin
     * pelin alkuun kuten ennen tätä.
     *
     * Merkki ei ole teko sivustolla: mitään ei lähetetä eikä haeta.
     */
    fun markPosition(note: String) {
        val game = currentGame.value ?: return
        val board = (_state.value as? BoardUiState.Loaded)?.board ?: return
        val move = board.moveNumber ?: return
        val position = seatForMark(board)?.let { CheckerPosition.of(board, it) }
        viewModelScope.launch { markBook.add(game, move, note, now(), position) }
    }

    private fun seatForMark(board: BoardState): Seat? =
        board.resolveSeat()
            ?: board.players.lastOrNull()?.checkerColor?.let { Seat(it, homeIsLow = true) }

    /** Merkki pois. Käyttäjän oma rivi ja hänen oma kuittauksensa, ks. [MarkBook]. */
    fun removeMark(id: Long) {
        viewModelScope.launch { markBook.remove(id) }
    }

    /**
     * Yhteinen kulku jokaiselle pyynnölle: yksi kerrallaan, taustalla, ja tulos samaan tilaan.
     *
     * [keepBoardOnFailure] on totta vain käyttäjän pyytämällä päivityksellä, ja se valitsee
     * epäonnistumisen äänen eikä säilymistä: lauta säilyy kummassakin, mutta vain teon
     * epäonnistuminen merkitsee sen vahvistamattomaksi. Ks. [onFailure].
     *
     * [after] ajetaan **ennen kuin uusi tila asetetaan**, jotta jonon kirjaus ja ruudun tila
     * eivät voi erota toisistaan yhdenkään kehyksen ajaksi.
     */
    private fun request(
        keepBoardOnFailure: Boolean,
        allowMatchChange: Boolean = false,
        /**
         * Haettu polku silloin kun tämä haku saa ratkaista odottavan teon, muuten null.
         * Vain [load] antaa sen: kokoava askel ja muu linkin seuraaminen eivät ole todiste
         * siitä että sivu olisi päässyt uuteen tilaan. Ks. [resolvePending].
         */
        resolvePendingFor: String? = null,
        /** Epätosi vain haulla ja päivityksellä, jotka lukevat eivätkä tee. Ks. [actedOnSite]. */
        actsOnSite: Boolean = true,
        after: suspend (DgResponse) -> Unit = {},
        /** Teon nimi kitkalokiin, ks. [KitkaLoki]. */
        mita: String = "haku",
        call: () -> DgResponse,
    ) {
        if (inFlight) return
        inFlight = true
        if (actsOnSite) actedOnSite = true
        _note.value = null
        KitkaLoki.alku(mita)

        val current = _state.value
        _state.value = when (current) {
            is BoardUiState.Loaded -> current.copy(refreshing = true)
            else -> BoardUiState.Loading
        }

        viewModelScope.launch {
            try {
                val response = withContext(io) { call() }
                KitkaLoki.merkki("vastaus")
                after(response)
                KitkaLoki.merkki("jalkityo")
                // Jäsennys on io:ssa: lautasivu on Jsoup.parse plus tusina valitsinta, ja
                // numerointi kävelee vanhemmat jokaista kuvaa kohti. Se on liikaa työtä
                // Mainille. Main saa vain valmiin muuttumattoman tilan.
                val tila =
                    withContext(io) { toState(response, keepBoardOnFailure, allowMatchChange) }
                KitkaLoki.merkki("tila")
                KitkaLoki.odota(tila)
                _state.value = tila
                if (resolvePendingFor != null) resolvePending(response, resolvePendingFor)
            } finally {
                inFlight = false
            }
        }
    }

    private suspend fun toState(
        response: DgResponse,
        keepBoardOnFailure: Boolean,
        allowMatchChange: Boolean = false,
    ): BoardUiState =
        when (response) {
            is DgResponse.Ok -> readBoard(response.html, allowMatchChange)
            is DgResponse.Offline -> onFailure(keepBoardOnFailure, Failure.Offline)
            is DgResponse.ServerError ->
                onFailure(keepBoardOnFailure, Failure.Server(response.code))
            DgResponse.Sleeping ->
                onFailure(keepBoardOnFailure, Failure.Sleeping)
            // Tunnuksia ei poisteta täällä: CredentialsStoren omistaa TopViewModel,
            // ja kahdesta poistajasta seuraisi kaksi totuutta samasta asiasta.
            DgResponse.AuthFailed -> BoardUiState.SessionExpired
        }

    /**
     * Sisäänkäynnin ehto, ja **vain sisäänkäynnin**.
     *
     * Tämä on portin se puolikas joka ei purkautunut. Ruudun avaava osoite tulee navigaation
     * läpi merkkijonona, joten sen alkuperää ei voi tarkistaa jäsenyydellä niin kuin [follow]in
     * osoitteen. Ehto on siksi yhä rakenteellinen: sisäänkäynti on kyselytön `/bg/move/`-polku,
     * mikä sulkee `/bg/nextgame`n eli jonoa kuluttavan osoitteen, ja sulkee sen että ruutu
     * avattaisiin suoraan tekoon.
     *
     * Kokoaminen ja lähettäminen tapahtuvat vasta laudan päällä, ja siellä ehto on eri: ks.
     * [follow] ja [press].
     */
    private fun isEntryPath(path: String): Boolean =
        path.startsWith("/bg/move/") && !path.contains('?')

    /**
     * Jonon viestisivu lautapolulla, tai null jos sivu ei ole sellainen.
     *
     * Ehto on laji eikä pelkkä jäsentyminen: `InboxParser.parse` hyväksyy minkä tahansa
     * ei-lautasivun jolla on `<h3>`, joten tuntematon laji tässä tarkoittaisi että mikä
     * tahansa vieras sivu kirjoittuisi kantaan valeviestinä. Jonossa sama laji säilytetään,
     * koska siellä haku on jo kuluttanut kohteen; täällä vieras sivu on todennäköisemmin
     * juuri vieras sivu. Ks. [NotABoardKind.InboxItem].
     */
    private suspend fun readInboxItem(html: String): BoardUiState? {
        InvitationParser.parse(html)?.let {
            return BoardUiState.NotABoard(NotABoardKind.Invitation(it.from.displayName))
        }
        val item = InboxParser.parse(html) ?: return null
        val message = item.toMessage(now())
        return when (message.source) {
            MessageSource.QUICK_MESSAGE ->
                BoardUiState.NotABoard(NotABoardKind.InboxItem(archive.archive(message, self()), saved = true))
            MessageSource.ANNOUNCEMENT ->
                BoardUiState.NotABoard(NotABoardKind.InboxItem(message, saved = false))
            else -> null
        }
    }

    /**
     * Järjestys on merkitsevä, ja jokainen askel vastaa mitattuun tapaukseen.
     */
    private suspend fun readBoard(html: String, allowMatchChange: Boolean = false): BoardUiState {
        if (!DgPages.isBoardPage(html)) {
            // Top Page erotellaan koska se on tavallisin tapa poistua laudalta ja kaikki
            // kolme havaittua ei-lauta-tapausta olivat sitä, ks. [NotABoardKind.TopPage].
            return when {
                DgPages.isTopPage(html) -> BoardUiState.NotABoard(NotABoardKind.TopPage(html))
                // Päättymissivu luetaan ennen tuntemattomaksi toteamista, ja chat luetaan
                // samalla lukijalla kuin laudalla: myös ottelun viimeinen viesti kuuluu
                // arkistoon, eikä se ehdi sinne mitään muuta reittiä.
                //
                // Tunnistinta ei kysytä erikseen. `MatchOverParser.parse` palauttaa nullin
                // täsmälleen silloin kun `isMatchOverPage` on epätosi (H3, 1.9.2026), joten
                // erillinen ehto teki `?:`-haarasta tavoittamattoman: sen sisällä null oli
                // mahdoton. Nyt jäsennin on sekä tunnistin että lukija, ja sivu jäsennetään
                // kerran eikä kahdesti.
                else -> MatchOverParser.parse(html)?.let { page ->
                    BoardUiState.MatchOver(page, readChat(html, page.matchId, null))
                }
                    // Jonon viestisivu luetaan päättymissivun jälkeen ja vasta tunnetulla
                    // lajilla, ks. [readInboxItem]. Tuntematon sivu jää [WrongPage]ksi.
                    ?: readInboxItem(html)
                    ?: BoardUiState.NotABoard(NotABoardKind.WrongPage)
            }
        }
        // Sivu on jo todettu lautasivuksi yllä, joten null tarkoittaa lukemisen
        // epäonnistumista eikä väärää sivua. Ne ovat eri viesti käyttäjälle.
        val board = BoardParser.parse(html)
            ?: return BoardUiState.NotABoard(NotABoardKind.BoardUnreadable)

        if (!allowMatchChange && expectedMatch != null && board.matchId != expectedMatch) {
            return BoardUiState.NotABoard(NotABoardKind.DifferentMatch(board.matchId))
        }
        if (board.points.isEmpty()) {
            // Syy luetaan sivulta eikä päätellä tyhjästä listasta: skeema on kuvapolun
            // hakemisto, ja vain Mini pudottaa pistenumerot. Muu tai tuntematon skeema saa
            // oman viestinsä, koska sen kohdalla ei tiedetä mitään korjattavaa.
            return if (board.scheme == BoardScheme.MINI) {
                BoardUiState.NotABoard(NotABoardKind.MiniScheme)
            } else {
                BoardUiState.NotABoard(NotABoardKind.PositionUnreadable)
            }
        }
        // Jonoa seurataan sivun omalla tunnisteella eikä kutsujan odotuksella, ks.
        // [watchedMatch]. Tähän asti päästään vain kun tunniste on jo todettu oikeaksi tai
        // kun teko oli ottelunvaihto, ja jälkimmäisessä myös odotus siirtyy uuteen otteluun:
        // seuraava painallus tarkistetaan sitä vastaan mitä ruudulla nyt on.
        watchedMatch.value = board.matchId
        expectedMatch = board.matchId
        // Päivityksen kohde luetaan samalta sivulta jonka juuri hyväksyimme, ks. [currentPath].
        board.form?.action?.let { if (isEntryPath(it)) currentPath = it }
        // Muistutusten peli luetaan samalta laudalta. Tämä on sijoitus eikä ehto: jos
        // pisteitä ei ollut luettavissa, arvo on null ja muistutukset ovat poissa käytöstä.
        currentGame.value = board.gameKey
        rememberBoard(board)
        return BoardUiState.Loaded(
            board,
            board.reconcilePips(),
            board.reconcileCheckers(),
            composition = LocalComposition.begin(
                board,
                playForcedSteps = playForcedSteps(),
                playGreedyBearoff = playGreedyBearoff(),
            ),
            chat = readChat(html, board),
            roundLabel = roundLabel(board),
        )
    }

    /**
     * Lauta ottelumuistiin. Vastustaja luetaan samalla ehdolla kuin viestin nimeäminen
     * ([opponentName]), joten epävarma nimi jää kirjaamatta eikä koskaan arvata. Kierros on
     * sama teksti jonka ruutu näyttää, ilman sanaa, eli `4/5` tai pelkkä `4` kun luettelo ei
     * tiennyt kokonaismäärää; yhdistäminen aiempaan riviin on muistin asia.
     */
    private suspend fun rememberBoard(board: BoardState) {
        val memory = matchMemory ?: return
        val name = opponentName(board)
        val panel = name?.let { n ->
            board.players.firstOrNull { it.player.displayName.trim().equals(n, ignoreCase = true) }
        }
        memory.remember(
            SeenMatch(
                matchId = board.matchId,
                opponent = panel?.player ?: PlayerRef(name = name),
                round = roundLabel(board)?.removePrefix("Round")?.trim()?.takeIf { it.isNotEmpty() },
                matchLength = board.matchLength,
                eventName = board.eventName?.takeIf { it.isNotBlank() },
                seenAtEpochMillis = now(),
            ),
        )
    }

    /**
     * `Round 4/5` kun luettelo kertoi kokonaismäärän tälle ottelulle, muuten sivun oma
     * `Round 4` tai null.
     *
     * Luettelon arvo haetaan sivun omalla tunnisteella, joten väärän ottelun kokonaismäärä
     * ei voi päätyä oikean numeron perään. Numero otetaan sivulta ja kokonaismäärä
     * luettelosta; jos ne eivät erotu muodosta `n/m`, luettelon arvo jää käyttämättä.
     */
    private fun roundLabel(board: BoardState): String? {
        val listed = listedRound(board.matchId) ?: return board.round
        val total = listed.substringAfter('/', "").trim().toIntOrNull() ?: return board.round
        val number = board.round?.substringAfter("Round ")?.trim()?.toIntOrNull()
            ?: listed.substringBefore('/').trim().toIntOrNull()
            ?: return board.round
        return "Round $number/$total"
    }

    /**
     * Sivun keskusteluosa, ja **vastustajan viesti arkistoon ennen kuin se on ruudulla**.
     *
     * Tämä on se kytkentä joka puuttui 27.8.2026 asti: `ChatParser` oli olemassa muttei
     * sillä ollut yhtään kutsujaa, joten siirron jälkeisen sivun chat-osa jäsentyi vain
     * testeissä ja katosi sovelluksessa. Sivusto ei säilytä sitä, joten kadonnutta ei saa
     * takaisin mistään.
     *
     * **Järjestys on merkitsevä.** Kirjoitus ensin, näyttö vasta sen jälkeen, samasta syystä
     * kuin `MessageArchive`n rajapinnassa: näytetty muttei tallennettu viesti on mennyt, jos
     * sovellus kaatuu ennen kirjoitusta.
     *
     * **Null on tavallinen tulos.** Suurin osa lautasivuista ei tarjoa kirjoituskenttää
     * lainkaan, ja silloin tässä ei ole mitään näytettävää.
     */
    private suspend fun readChat(html: String, board: BoardState): ChatOnBoard? =
        readChat(html, board.matchId, opponentName(board))

    /**
     * Sama lukija ilman lautaa: ottelun päättymissivulla ei ole lautaa, mutta chat-lomake on
     * sama. Vastustajan nimi tulee silloin sivun omasta otsikosta.
     */
    private suspend fun readChat(
        html: String,
        matchId: MatchId?,
        boardOpponent: String?,
    ): ChatOnBoard? {
        val screen = ChatParser.parse(html) ?: return null
        // Lomakkeen puute ei ole paluun syy: viesti arkistoidaan ennen kuin lomakkeesta
        // päätetään mitään, koska juuri lomakkeeton sivu vei viestin 7.9.2026
        // ([ChatOnBoard.form]). Ilman viestiä ja ilman kenttää sivulla ei ole chattia.
        val form = screen.form
        if (form == null && screen.incoming == null) return null
        // Nimi kootaan laudan pelaajapaneeleista eikä chat-lohkosta, koska lohkossa on kaksi
        // käyttäjälinkkiä joista kumpaakaan ei ole merkitty kirjoittajaksi (`docs/KOHDE.md`).
        // Sivun oma otsikko on varalla: `You may chat with <nimi> here:` ja `<nimi> says:`
        // nimeävät vastustajan suoraan, eli se on sivun tietoa eikä arvausta chat-lohkosta.
        val opponent = boardOpponent ?: screen.opponentName

        val incoming = screen.incoming
        val incomingId = incoming?.let { text ->
            // Avain on pari eikä yhdistetty merkkijono: erotinta ei tarvitse valita, joten
            // kaksi eri paria ei voi tuottaa samaa avainta millään sisällöllä.
            // Lomakkeettomalla sivulla avaimen ensimmäinen puoli on ottelu, koska lomaketta
            // ei ole; tarkoitus on sama, sama viesti samalta sivulta vain kerran.
            archivedChats.getOrPut((form?.action ?: "match:${matchId?.value}") to text) {
                archive.archive(
                    Message(
                        matchId = matchId,
                        // Arkiston avain on henkilö eikä ottelu: päättynyt ottelu putoaa
                        // sivustolta ja tunnisteesta jää orpo numero (`Message.opponent`).
                        opponent = opponent,
                        // Saapuva viesti on aina vastustajan: oma teksti ei tule takaisin
                        // sivulta vaan kirjataan lähetettäessä.
                        sender = opponent.orEmpty(),
                        // Chat-lohkossa ei ole aikaleimaa, samoin kuin pikaviestissä.
                        timestampText = "",
                        body = text,
                        // Sivun oma otsikkorivi sellaisenaan, kolmessa eri muodossa
                        // (`Chat:`, `<nimi> says:`, `You may chat with <nimi> here:`).
                        rawHeader = screen.headerText.orEmpty(),
                        source = MessageSource.GAME_MESSAGE,
                        receivedAtEpochMillis = now(),
                    ),
                    account = self(),
                ).id
            }
        }
        return ChatOnBoard(
            incoming = incoming,
            incomingId = incomingId,
            opponent = opponent,
            form = form,
        )
    }

    /**
     * Vastustaja laudan pelaajapaneeleista, eli se paneeli joka ei ole käyttäjä itse.
     *
     * Null kolmessa tapauksessa, eikä yhtäkään niistä arvata: paneeleja ei ollut, oma nimi ei
     * ole tiedossa, tai ehto jättää muun kuin tasan yhden nimen. Nimetön viesti on arkistossa
     * huonompi kuin nimetön, mutta väärälle henkilölle kirjattu on huonompi kuin kumpikaan.
     */
    private fun opponentName(board: BoardState): String? {
        val me = self()?.trim().orEmpty()
        if (me.isEmpty()) return null
        return board.players
            .map { it.player.displayName.trim() }
            .filter { it.isNotEmpty() && !it.equals(me, ignoreCase = true) }
            .distinct()
            .singleOrNull()
    }

    /**
     * Ne chat-viestit jotka tämä näkymämalli on jo kirjoittanut arkistoon.
     *
     * **Avain on sivun oma lomakeosoite ja viestin teksti, arvo arkistoidun rivin tunniste**,
     * eli sama sivu samalla sisällöllä ei kirjoitu kahdesti ja vastaus osaa yhä osoittaa
     * oikeaan riviin. Osoite on avaimessa mukana koska se kantaa sivun tilatunnisteen: sama
     * teksti ottelun eri vaiheessa on eri viesti. Ilman tätä päivitys kirjoittaisi saman viestin uudelleen, ja
     * `Message.id` sisältää saapumishetken, joten rivi olisi kannassa eri tunnisteella eikä
     * `insertIfNew` estäisi sitä.
     *
     * **Rajaus, ja se on nimettävä.** Vahti elää tämän olion ajan. Jos sama sivu saapuisi
     * uudelleen sovelluksen käynnistyksen jälkeen, viesti kirjoittuisi toiseen kertaan.
     * Sitä ei ole mitattu, koska sivuston oma sääntö on että viesti katoaa lukuhetkellä;
     * kysymys on `docs/AVOIMET.md`:ssä eikä ratkaistu arvaamalla.
     */
    private val archivedChats = mutableMapOf<Pair<String, String>, String>()

    /**
     * Epäonnistunut pyyntö ei hävitä jo näkyvää lautaa. Minuutin takainen asema on
     * vanhentunutta mutta oikeaa tietoa, virheruutu ei ole mitään.
     *
     * Ero päivityksen ja teon välillä on äänessä eikä säilymisessä (24.8.2026 asti teon
     * epäonnistuminen pyyhki laudan). Päivitys säilyttää laudan hiljaa, koska lukutila ei
     * väitä mitään uutta. Teon jälkeen sama hiljaisuus olisi valhe: säilynyt lauta
     * luettaisiin todisteeksi teon onnistumisesta. Siksi teko merkitsee laudan
     * vahvistamattomaksi ([BoardUiState.Loaded.unconfirmed]), ruutu näyttää siitä
     * ilmoituksen ja lauta lukittuu näytöksi kunnes sivu on haettu uudelleen.
     */
    private fun onFailure(isRefresh: Boolean, reason: Failure): BoardUiState {
        val current = _state.value
        return when {
            current !is BoardUiState.Loaded -> BoardUiState.Failed(reason)
            isRefresh -> current.copy(refreshing = false)
            else -> current.copy(refreshing = false, unconfirmed = reason)
        }
    }

    class Factory(
        private val pages: PageFetcher,
        private val forms: FormSender,
        private val queue: ActionQueue,
        private val reminderBook: ReminderBook,
        private val markBook: MarkBook,
        private val archive: MessageArchive,
        private val self: () -> String?,
        private val playPath: String,
        private val expectedMatchId: MatchId?,
        private val listedRound: (MatchId) -> String? = { null },
        private val playForcedSteps: () -> Boolean = { false },
        private val playGreedyBearoff: () -> Boolean = { false },
        private val matchMemory: MatchMemory? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BoardViewModel(
                pages,
                forms,
                queue,
                reminderBook,
                markBook = markBook,
                matchMemory = matchMemory,
                archive = archive,
                self,
                playPath,
                expectedMatchId,
                listedRound = listedRound,
                playForcedSteps = playForcedSteps,
                playGreedyBearoff = playGreedyBearoff,
            ) as T
    }

    private companion object {

        /**
         * Kirjattava syy verkkovirheestä: sana `Offline` ja poikkeuksen luokan nimi.
         *
         * **Tässä oli 10.8.2026 asti pelkkä vakio `"Offline"`**, koska verkkokerros ei
         * kertonut muuta. Nyt se kertoo, ja rivillä lukee kumpi katkoista oli kyseessä.
         * Kirjattava syy on yhä lokia eikä logiikkaa: mikään ei lue sitä takaisin, ja siksi
         * se saa olla teksti.
         *
         * Sana säilyy alussa tarkoituksella. Pelkkä luokan nimi olisi rivillä joka lukee
         * `SocketTimeoutException` ilman että mikään kertoo mihin luokkaan vika kuuluu, ja
         * rivin lukija on käyttäjä eikä kehittäjä.
         */
        fun offlineReason(response: DgResponse.Offline) = "Offline: ${response.cause}"
    }
}
