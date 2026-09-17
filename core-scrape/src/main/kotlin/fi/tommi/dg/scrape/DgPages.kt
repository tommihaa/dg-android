package fi.tommi.dg.scrape

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Sivun tunnistus sisällön perusteella.
 *
 * Tämä on koko istunnonhallinnan kulmakivi, ja syy on mitattu: uloskirjautunut pyyntö
 * osoitteeseen /bg/top palauttaa **200 OK ja login-lomakkeen**, ei 302:ta eikä 401:tä.
 * Statuskoodi ei siis kerro istunnon tilasta mitään. Asiakas joka luottaa
 * `response.isSuccessful`-tarkistukseen luulee login-sivua otteludataksi.
 *
 * **Tunnistus ajetaan jäsentimen sisällä, ei sen edellä** (Tommin päätös 1.9.2026,
 * kompositioauditoinnin H3). Kutsupaikka kysyi ennen ensin `isXPage` ja vasta sitten
 * `XParser.parse`, ja sääntö oli muistin varassa jokaisessa uudessa kutsupaikassa
 * erikseen. Mitattuna jokainen tuotannon kutsupaikka noudatti sitä, mutta unohdus olisi
 * ollut hiljainen: väärä sivu olisi jäsentynyt tyhjäksi arvoksi eikä virheeksi.
 *
 * Nyt jokainen jäsennin aloittaa oman `is`-kysymyksensä kutsulla ja palauttaa `null` kun
 * vastaus on ei. Sopimus on siis tyypissä eikä kommentissa: **`null` tarkoittaa aina
 * *tämä ei ole se sivu*, ja tyhjä sisältö tulee arvona**, koska sivustolla on aidosti
 * tyhjiä listoja. Kutsupaikka ei voi enää unohtaa porttia, koska porttia ei ole erikseen.
 *
 * Document-ylikuormat ovat siksi tässä pareittain: jäsennin on jo jäsentänyt sivun
 * Jsoupilla, eikä sitä tehdä toista kertaa pelkän tunnistuksen vuoksi.
 */
object DgPages {

    /** Kirjautumislomakkeen action-polku, sama sillä sivulla jolle lomake palautetaan. */
    const val LOGIN_ACTION = "/bg/login"

    /**
     * Otteluluettelo. Ainoa osoite jonka sovellus saa hakea oma-aloitteisesti: se on
     * pelkkä luku eikä kuluta mitään.
     *
     * Vastakohta on `/bg/nextgame`, joka kuluttaa jonoa ja jota ei siksi ole täällä
     * vakiona lainkaan. Kuluttavat osoitteet luetaan sivun omista linkeistä silloin kun
     * käyttäjä on pyytänyt niitä.
     */
    const val TOP_PATH = "/bg/top"

    /**
     * Omat asetukset. Pelkkä luku eikä kuluta mitään, joten haku on turvallinen.
     *
     * Huomaa ero: tämä on **kirjautuneen omat** asetukset eikä sisällä käyttäjänumeroa,
     * kun taas `/bg/user/<id>` on jonkun pelaajan julkinen profiili. Suomeksi molempia on
     * kutsuttu profiiliksi, mutta ne ovat eri sivuja eri jäsentimillä.
     */
    const val SETTINGS_PATH = "/bg/profile"

    fun isLoginPage(html: String): Boolean = isLoginPage(Jsoup.parse(html))

    /**
     * Onko sivu omien asetusten sivu.
     *
     * Tunnistus on lomakkeessa eikä otsikossa, toisin kuin `isProfilePage`illa. Syy on eri
     * suuntaan kuin siellä: otsikko sisältää käyttäjänimen, ja asetuslomake on se osa jonka
     * olemassaolosta kysymys oikeasti on. Sivu jolla otsikko lupaa asetuksia mutta lomaketta
     * ei ole, ei kelpaa lähetyksen pohjaksi, ja juuri sitä käyttökohde tarvitsee.
     */
    fun isSettingsPage(html: String): Boolean = isSettingsPage(Jsoup.parse(html))

    fun isSettingsPage(document: Document): Boolean =
        document.selectFirst("form[action=${SettingsParser.PREF_ACTION}]") != null

    /**
     * Onko sivu pelaajan profiili.
     *
     * Tarpeen samasta syystä kuin `isLoginPage`: sivun laji ei ole pääteltävissä pyydetystä
     * osoitteesta. Se mitattiin 3.8.2026, kun `/bg/move/`-pyyntö palautti 200 OK:lla sivun
     * jolla ei ollut lautaa lainkaan.
     *
     * Tunnistus on otsikossa, koska se on ainoa kohta joka ei riipu siitä onko profiililla
     * yhtään ottelua. Taulukoiden captionit katoaisivat tyhjällä profiililla.
     */
    fun isProfilePage(html: String): Boolean = isProfilePage(Jsoup.parse(html))

    fun isProfilePage(document: Document): Boolean =
        document.title().contains("Info on player")

    /**
     * Onko sivu viestin lähetyksen kuittaus, eli meniko lähetys perille.
     *
     * Tarpeen samasta syystä kuin [isLoginPage]: statuskoodi ei kerro lopputuloksesta
     * mitään, koska sivusto vastaa 200 OK:lla myös silloin kun istunto on katkennut ja
     * vastauksena tulee login-lomake. Ilman tätä `submitForm`in `Ok` tarkoittaisi vain
     * että palvelin vastasi jotain.
     *
     * Tunnistus on otsikossa, koska se on ainoa kohta joka ei riipu vastaanottajasta.
     * Runko nimeää vastaanottajan linkkinä, ja se olisi ainoa tapa tarkistaa että viesti
     * meni sille jolle se aiottiin; sitä ei toistaiseksi lueta, koska lähettävää ruutua
     * ei ole. Mitattu 22.8.2026 raaoista tavuista (`POST /bg/sendmsg/<id>`).
     */
    fun isMessageSentPage(html: String): Boolean = isMessageSentPage(Jsoup.parse(html))

    fun isMessageSentPage(document: Document): Boolean =
        document.title().trim() == "Message Sent"

    /**
     * Onko sivu ottelukutsun kuittaus (`POST /bg/invite/new`).
     *
     * Tunnistus on otsikossa samasta syystä kuin [isMessageSentPage]illä: se on ainoa
     * kohta joka ei riipu kutsutusta pelaajasta. Runko nimeää hänet linkkinä, ja se on
     * luettavissa [SendResultParser.notice]illa.
     *
     * Mitattu 4.9.2026 raaoista tavuista (`invite_sent.html`). Tähän asti sovellus sanoi
     * vain että vastaus tuli, koska kutsun vastausta ei ollut nähty; nyt se voi sanoa että
     * kutsu lähti.
     */
    fun isInviteSentPage(html: String): Boolean = isInviteSentPage(Jsoup.parse(html))

    fun isInviteSentPage(document: Document): Boolean =
        document.title().trim() == "DailyGammon Invitation"

    /**
     * Onko sivu luovutuksen kuittaus (`POST /bg/resign/doit`).
     *
     * Tunnistus on otsikossa samasta syystä kuin [isInviteSentPage]illä: runko nimeää luvun,
     * joka vaihtelee, kun otsikko on sama joka kerta. Sivulla ei ole lomaketta, joten
     * `ResignParser.parse` palauttaa siitä nullin eikä sitä voi erottaa luovutussivusta
     * lomakkeen puuttumisella yksin: sen tekisi mikä tahansa lukukelvoton sivu.
     *
     * Mitattu 4.9.2026 illalla (`resign_done.html`). Tähän asti sovellus luki onnistumisen
     * vain siitä että rivi katosi listalta; nyt se voi kertoa sivuston oman lauseen.
     */
    fun isResignDonePage(html: String): Boolean = isResignDonePage(Jsoup.parse(html))

    fun isResignDonePage(document: Document): Boolean =
        document.title().trim() == "DailyGammon: Post Resignation"

    /**
     * Onko sivu palstalähetyksen kuittaus, eli syntyikö viesti.
     *
     * **Tunnistus on linkissä eikä otsikossa**, ja se on mittauksen sanelema: sivu on
     * palstan tavallinen kuori, jonka `TITLE` on sama `DailyGammon Forum: <palsta>` kuin
     * ketjulla ja indeksillä. Ainoa kohta joka esiintyy vain kuittauksessa on linkki
     * `Review your message.`, ja se on myös hyödyllisin: sen osoite nimeää **juuri sen
     * viestin joka syntyi** (`/bg/forum2/<palsta>/read/<ketju>/<numero>#<numero>`).
     *
     * Mitattu 4.9.2026 raaoista tavuista kommentista (`forum_posted.html`,
     * `POST /bg/forum2/politics/submitadd/<id>`). **Uuden ketjun vastausta ei ole
     * mitattu**, eikä tämä väitä siitä mitään; `submitnew` voi vastata toisin, ja siihen
     * asti sen onnistuminen luetaan indeksistä kuten ennenkin.
     */
    fun isForumPostedPage(html: String): Boolean = isForumPostedPage(Jsoup.parse(html))

    fun isForumPostedPage(document: Document): Boolean =
        postedMessagePath(document) != null

    /** Kuittauksen `Review your message.` -linkin osoite, tai null kun sivu ei ole kuittaus. */
    fun postedMessagePath(document: Document): String? =
        document.select("a[href]")
            .firstOrNull { it.text().trim() == "Review your message." }
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }

    fun isLoginPage(document: Document): Boolean =
        document.selectFirst("form[action=$LOGIN_ACTION]") != null

    /**
     * Onko sivu käyttökatkoilmoitus, eli sivuston vastaus päivittäisen varmuuskopion aikana.
     *
     * Mitattu 11.9.2026 klo 00.16 (`site_sleeping.html`, raakasivu `kayttokatko.html`):
     * `GET /bg/top` kirjautuneena antoi HTTP 200 ja seitsemän rivin sivun jonka otsikko on
     * `DailyGammon Backups` ja runko `DailyGammon is sleeping -- SHH!!` sekä kehotus palata
     * puolen tunnin päästä. Tunnistus on otsikko, koska se on sivun ainoa rakenteellinen
     * osa: ei lomaketta, ei taulukkoa, ei linkkejä. **Kirjautumaton haku ei saa tätä sivua**
     * vaan tavallisen login-lomakkeen (mitattu samalla minuutilla), joten ilmoitus tulee vain
     * istunnon kanssa eikä se ole login-sivun muunnelma.
     *
     * Sivu on onnistunut vastaus, ei `Offline` eikä `ServerError`, ja siksi tämä on olemassa:
     * ilman tunnistinta se olisi jäsentimelle tuntematon sivu ja ruudulla `NotATopPage` tai
     * `WrongPage`, eli vika sovelluksessa vaikka kyse on rutiinista (`SUBSTANSSI.md` kohta 49).
     */
    fun isSleepingPage(html: String): Boolean = isSleepingPage(Jsoup.parse(html))

    fun isSleepingPage(document: Document): Boolean =
        document.title().trim() == "DailyGammon Backups"

    /**
     * Onko sivu Game Lounge.
     *
     * Tunnistus on tarjouslomake eikä otsikko tai taulukot, samalla perusteella kuin
     * [isSettingsPage]: lomake on läsnä silloinkin kun molemmat taulukot ovat tyhjiä
     * (mitattu 22.8.2026, tyhjä lounge), joten se on sivun pysyvin osa. Loungen polkua
     * ei ole täällä vakiona: se luetaan Top Pagen navigointipalkista
     * (`TopPage.loungePath`), koska hyväksymislinkit ovat muuttavia tekoja ja koko sivu
     * haetaan vain käyttäjän pyynnöstä.
     */
    fun isLoungePage(html: String): Boolean = isLoungePage(Jsoup.parse(html))

    fun isLoungePage(document: Document): Boolean =
        document.selectFirst("form[action=/bg/lounge]") != null

    /**
     * Onko sivu kutsun hyväksymisen kuittaus, eli meniko Join perille.
     *
     * Mitattu 31.8.2026 oikeasta hyväksynnästä (`/bg/lounge?action=accept&id=<n>`), ja
     * mittaus kaatoi oletuksen jonka varassa tulos luettiin: vastaus **ei ole lounge**
     * vaan oma 1237-tavuinen kuittaussivunsa, jossa on yksi lause ja linkki
     * `/bg/nextgame`iin. Ilman tätä predikaattia onnistunut liittyminen näyttäytyi
     * tuntemattomana vastauksena, vaikka sivusto sanoi sen suoraan.
     *
     * Tunnistus on lauseessa eikä rakenteessa, toisin kuin muilla tämän tiedoston
     * predikaateilla. Se on rajaus eikä valinta: sivulla ei ole lomaketta, taulukkoa
     * eikä omaa otsikkoa (`<TITLE>DailyGammon</TITLE>` on sivuston yleisin), joten
     * mitään pysyvämpää ei ole tarjolla. Osuma on lauseen kestävä osa ilman
     * välimerkkiä, koska sanamuoto on sivuston omaa tekstiä eikä sopimus.
     */
    fun isJoinConfirmationPage(html: String): Boolean =
        isJoinConfirmationPage(Jsoup.parse(html))

    fun isJoinConfirmationPage(document: Document): Boolean =
        document.body().text().contains("successfully joined")

    /**
     * Onko sivu ottelun päättymissivu.
     *
     * Ehto on sama säännöllinen lauseke jolla `MatchOverParser` lukee tuloksen, eikä oma
     * kopionsa: predikaatti ja jäsennin eivät voi olla eri mieltä siitä mikä sivu tämä on.
     *
     * **Ankkuri on `and the match`**, koska yksittäisen pelin päättyminen kesken ottelun on
     * eri sivu jolla peli jatkuu. Mitattu 31.8.2026 kahdesta oikeasta ottelusta.
     */
    fun isMatchOverPage(html: String): Boolean = isMatchOverPage(Jsoup.parse(html))

    fun isMatchOverPage(document: Document): Boolean =
        MatchOverParser.RESULT.containsMatchIn(document.body().wholeText())

    /**
     * Onko sivu keskustelupalstan indeksi. Sama valitsin kuin `ForumParser.parseIndex`in
     * riveillä, jotta predikaatti ja jäsennin eivät voi olla eri mieltä.
     *
     * Huomaa raja: tyhjä palsta (ei yhtään ketjua) olisi tälle epätosi. Sellaista ei ole
     * mitattu, ja General-palstalla on ollut ketjuja koko mittaushistorian ajan.
     *
     * Valitsin kattaa molemmat palstat: palstan tunnus on osa polkua eikä vakio `main`
     * (mitattu 27.8.2026, `raakasivut/forum2_politics.html`).
     */
    fun isForumIndex(html: String): Boolean = isForumIndex(Jsoup.parse(html))

    fun isForumIndex(document: Document): Boolean =
        document.selectFirst(ForumParser.THREAD_LINK) != null

    /**
     * Onko sivu palstan kuukausiarkisto. Erottelu [isForumIndex]iin on kuukausiluettelo:
     * arkistosivulla on ketjulinkkien lisäksi linkki jokaiseen kuukauteen, indeksissä ei
     * ole yhtään. Tarkistus tehdään ennen indeksiä, koska arkistosivu on **myös** indeksin
     * tuntomerkin mukainen: siinä on ketjulinkkejä.
     */
    fun isForumArchive(html: String): Boolean = isForumArchive(Jsoup.parse(html))

    fun isForumArchive(document: Document): Boolean =
        document.selectFirst(ForumParser.MONTH_LINK) != null &&
            document.selectFirst(ForumParser.THREAD_LINK) != null

    /**
     * Onko sivu ketjun lukusivu. Otsikko ei kelpaa, koska `<title>` on sama kuin
     * indeksillä (mitattu 26.8.2026). Tunnistus on viestin oma rakenne: ankkuri
     * `<a name=n>` ja allekirjoitus `Posted by`, eli täsmälleen se mihin
     * `ForumParser.parseThread` jakaa sivun. Kumpikaan ei yksin riitä: pelkkä ankkuri
     * voisi olla millä tahansa sivulla, ja chat-sivun "says:" -muoto ei sisällä
     * kumpaakaan.
     */
    fun isForumThread(html: String): Boolean = isForumThread(Jsoup.parse(html))

    fun isForumThread(document: Document): Boolean =
        document.selectFirst("a[name]") != null &&
            document.selectFirst("b:contains(Posted by)") != null

    /**
     * Onko sivu pelaajalista `/bg/plist`.
     *
     * **Tunnus on otsikko eikä linkki**, ja se on poikkeus tämän tiedoston tapaan.
     * Rakenteellinen tuntomerkki olisi ollut sivun oma `/bg/plist`-linkki, mutta sellainen
     * on myös loungella (*List of Players*), ja loungella on lisäksi profiililinkkejä
     * riveillä, joten kumpikin tuntomerkki yksin tunnistaisi loungen pelaajalistaksi.
     * `<TITLE>` on tällä sivulla yksiselitteinen (`DailyGammon - Player List`), toisin kuin
     * palstalla jossa indeksi ja ketju jakavat saman otsikon.
     */
    fun isPlayerList(html: String): Boolean = isPlayerList(Jsoup.parse(html))

    fun isPlayerList(document: Document): Boolean =
        document.title().contains("Player List", ignoreCase = true) &&
            document.selectFirst(PlayerListParser.PLAYER_ROW) != null

    /**
     * Onko sivu Tournament Hall `/bg/thall`. Captionit ovat sivuston omat ja erottuvat
     * loungen `Tournament Sign-Up`ista; kumpi tahansa taulukko riittää, koska päättyneiden
     * lista voisi periaatteessa olla tyhjä.
     */
    fun isTournamentHall(html: String): Boolean = isTournamentHall(Jsoup.parse(html))

    fun isTournamentHall(document: Document): Boolean =
        document.select("table:has(caption) caption").any {
            val caption = it.text()
            caption.contains("Active Tournaments", ignoreCase = true) ||
                caption.contains("Finished Tournaments", ignoreCase = true)
        }

    /**
     * Onko sivu turnaussivu `/bg/event/<id>`.
     *
     * Tunnus on sivun oma väliotsikko `Conditions of Contest`, ei `<TITLE>` eikä kaavion
     * caption. Otsikko olisi ollut muotoa `DailyGammon -- <turnauksen nimi>`, eli
     * turnauksen nimen varassa, ja `Brackets`-caption puuttuisi turnaukselta jonka kaaviota
     * ei ole vielä arvottu. Säännöt ovat sivulla aina.
     *
     * **Rajaus sanotaan ääneen:** mitattuna on yksi turnaussivu (cup-kaavio). Jos
     * sivustolla on muun muotoisia turnauksia, tämä tunnistaa ne mutta kaavio voi jäädä
     * tyhjäksi. Tyhjä kaavio on silloin oikea vastaus eikä väärä väite.
     */
    fun isEventPage(html: String): Boolean = isEventPage(Jsoup.parse(html))

    fun isEventPage(document: Document): Boolean =
        document.select("h3").any { it.text().contains("Conditions of Contest", ignoreCase = true) }

    /**
     * Onko sivu pelaajan omien turnausten lista, eli `/bg/userevent/<id>` tai
     * `/bg/userwins/<id>`. Molemmat kelpaavat samalle jäsentimelle, joten tämä ei erottele
     * niitä; kumpi sivu on kyseessä, näkyy riveistä.
     *
     * Tunnistus on otsikossa samasta syystä kuin profiilisivulla: pelaajalla voi olla nolla
     * turnausta, jolloin taulukossa on vain otsikkorivi.
     */
    fun isPlayerTournaments(html: String): Boolean = isPlayerTournaments(Jsoup.parse(html))

    fun isPlayerTournaments(document: Document): Boolean =
        document.title().let {
            it.contains("Events For", ignoreCase = true) ||
                it.contains("Wins For", ignoreCase = true)
        }

    /**
     * Onko sivu ottelun siirtohistoria, eli `/bg/game/<id>/<n>/list` (Review-linkki).
     *
     * Tunnistus on rakenteessa eikä otsikossa, ja syy on mitattu: siirtohistorian otsikko
     * on `DailyGammon Match <id>, Move <n>`, eli samaa muotoa kuin lautasivun otsikko,
     * joten otsikko ei erota niitä. `Game <n>` -väliotsikot (`<th colspan=5>`) ovat sen
     * sijaan vain tällä sivulla: lautasivulla ei ole `th`-soluja lainkaan, ja muiden
     * tunnettujen sivujen `th`-tekstit ovat sarakeotsikoita (`Opponent`, `Grace`, ...).
     */
    fun isMatchLogPage(html: String): Boolean = isMatchLogPage(Jsoup.parse(html))

    fun isMatchLogPage(document: Document): Boolean =
        document.select("th").any { GAME_HEADER.matches(it.text().trim()) }

    private val GAME_HEADER = Regex("""Game \d+""")

    /**
     * Sivuston oma Help (`/help`). Staattinen sivu sivuston juuressa, ei `/bg/`-polkua.
     *
     * Tunnistus on otsikossa, koska se on yksiselitteinen (mitattu 29.8.2026,
     * `raakasivut/site_help.html`): `DailyGammon Help`, kun sovelluksen tunteman sisällön
     * otsikot ovat muotoa `DailyGammon - ...` tai `DailyGammon Forum: ...`. Tasan-vertailu
     * eikä contains, koska jokainen sivuston otsikko sisältää sanan DailyGammon.
     */
    fun isSiteHelpPage(html: String): Boolean = isSiteHelpPage(Jsoup.parse(html))

    fun isSiteHelpPage(document: Document): Boolean =
        document.title().trim().equals("DailyGammon Help", ignoreCase = true)

    /**
     * Sivuston linkkisivu (`/links.html`). Sama peruste ja sama muoto kuin
     * [isSiteHelpPage]lla; otsikon kirjoitusasu on sivulla `Dailygammon Links` pienellä
     * g:llä, ja vertailu on siksi kirjainkoosta piittaamaton täälläkin.
     */
    fun isSiteLinksPage(html: String): Boolean = isSiteLinksPage(Jsoup.parse(html))

    fun isSiteLinksPage(document: Document): Boolean =
        document.title().trim().equals("Dailygammon Links", ignoreCase = true)

    /**
     * Montako pistekuvaa lautasivulla on. Mitattu kaikista 22 fixturesta 5.8.2026:
     * jokaisella lautasivulla tasan 24, jokaisella muulla nolla.
     */
    private const val POINT_IMAGE_COUNT = 24

    /**
     * Onko sivu lautasivu, eli onko sillä asema jonka voi piirtää.
     *
     * Tarpeen samasta syystä kuin [isProfilePage]: sivun laji ei ole pääteltävissä
     * pyydetystä osoitteesta. `/bg/move/`-pyyntö voi palauttaa 200 OK:lla tyhjän Top Pagen
     * (mitattu 3.8.2026), toisen ottelun laudan (4.8.2026) tai istunnon katkettua sen
     * sivun jonne palvelin kirjautumisen jälkeen ohjaa.
     *
     * Tunnistus nojaa **pistekuviin**, ja perustelun muoto on sama kuin kahdella muulla
     * predikaatilla: valitaan se osa jonka olemassaolosta kysymys oikeasti on. Käyttökohde
     * tarvitsee aseman piirrettäväksi, ja pistekuvat ovat asema. Kolme hylättyä vaihtoehtoa:
     *
     * - Otsikkoregex `Match N, Move M` on [BoardParser]in oma varamekanismi ottelunumerolle,
     *   joten predikaatti olisi kehämäinen: tosi täsmälleen silloin kun `parse` on jo
     *   ei-null, eikä se lisäisi mitään.
     * - `/bg/move/`-linkki tai lomakkeen action: **mitattu väärä positiivi**, `top_page.html`
     *   sisältää viisi sellaista.
     * - Numerorivi kutsuisi Mini-skeemaa ei-laudaksi, eli päättäisi hiljaa sen avoimen
     *   kysymyksen milloin `parse` palauttaa nullin. Se on oma päätöksensä eikä predikaatin.
     *
     * Kynnys on `>=` eikä `==`: mahdollinen ylimääräinen `pt_`-kuva ei saa muuttaa lautaa
     * ei-laudaksi. Ja `>= 24` eikä `> 0`: yksi hajallinen pistekuva ei ole asema.
     *
     * **Tämä ei yksin ole turvatae.** Siirron jälkeinen chat-sivu on tälle tosi, koska
     * sillä on aito 24 pisteen lauta, mutta se lauta on spekulatiivinen: sivusto näyttää
     * tulevan aseman ja kysyy siirtoa etukäteen. Sen erottaminen on [ChatParser]in työ
     * (`ChatScreen.speculative`) eikä tämän.
     */
    fun isBoardPage(html: String): Boolean = isBoardPage(Jsoup.parse(html))

    fun isBoardPage(document: Document): Boolean =
        document.select("img[src*=pt_]").size >= POINT_IMAGE_COUNT

    /**
     * Onko sivu otteluluettelo.
     *
     * **Tarpeen samasta syystä kuin [isBoardPage], ja se syy mitattiin vasta 21.8.2026.**
     * Ilman tätä `TopPageParser` jäsentää minkä tahansa 200 OK:lla saadun sivun, ja tulos on
     * tyhjä `TopPage`. Ruudulla se näkyy tekstinä `No matches.`, joka on kelvollinen tila ja
     * väärä väite: se sanoo sivustosta jotain mitä ei tiedetä. Lautaruudulla sama tapaus
     * kohdattiin 3.8.2026, kun `/bg/move/`-pyyntö palautti 200 OK:lla sivun jolla ei ollut
     * lautaa, ja se ratkaistiin silloin tällä samalla kaavalla.
     *
     * **Tunnistus on oman profiilin linkki, ei otsikko eikä taulukot.** Taulukot katoavat
     * kun siirrettäviä otteluita ei ole, eli juuri siinä tapauksessa jota tämä erottaa
     * tyhjästä vastauksesta. Otsikko `<h2>` on olemassa myös keskustelupalstalla ja
     * asetussivulla. Kyselyparametrillinen `days_to_view`-linkki sen sijaan on mitattu
     * 21.8.2026 kaikista 27 fixturesta: se on tasan kahdella top-sivulla (otteluiden kanssa
     * ja ilman) eikä yhdelläkään muulla.
     *
     * Sama valitsin kuin `TopPageParser`illa, ja se on tarkoituksellista: predikaatti kysyy
     * täsmälleen sitä mitä jäsennin tarvitsee, joten ne eivät voi olla eri mieltä siitä
     * onko sivu luettavissa.
     */
    fun isTopPage(html: String): Boolean = isTopPage(Jsoup.parse(html))

    fun isTopPage(document: Document): Boolean =
        document.selectFirst("a[href*=days_to_view]") != null

    /**
     * Login-lomakkeen piilokenttä `path` kertoo minne palvelin ohjaa onnistuneen
     * kirjautumisen jälkeen. Uudelleenkirjautuessa se kannattaa säilyttää: silloin
     * alkuperäinen kohde saadaan samalla pyynnöllä eikä erillistä uudelleenhakua tarvita.
     *
     * Arvo on palvelimen itsensä kertoma, ei meidän päättelemämme. Se on tärkeää:
     * polkumuunnos /bg/top -> "top/" olisi arvaus, tämä ei ole.
     */
    fun loginReturnPath(html: String): String? =
        Jsoup.parse(html)
            .selectFirst("form[action=$LOGIN_ACTION] input[name=path]")
            ?.attr("value")
            ?.takeIf { it.isNotBlank() }
}
