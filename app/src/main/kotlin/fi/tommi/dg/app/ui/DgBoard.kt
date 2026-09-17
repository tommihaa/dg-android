package fi.tommi.dg.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Laudan ulkoasun arvot, tuotuna Claude Designista 8.8.2026.
 *
 * Lauta on Monte Carlo X-22, Tommin valinta. Design tuotti kuvan ja web-koodia, ja tämä
 * tiedosto on se osajoukko jonka Compose osaa kuluttaa: suhdeluvut ja värit. Kaikki muu
 * Designin puolella on kuvitusta eikä totuuden lähde.
 *
 * **Mikään näistä ei ole korkeus.** Designin viiteikkuna oli 891 x 411 dp vaaka, mutta
 * [BoardScreen] johtaa kiilan korkeuden mitatusta tilasta `BoxWithConstraints`illa eikä
 * vähennä chromea vakiona. Syy on mitattu: 8.8.2026 Pixel 8a:lla kiinteä 200 dp kiila
 * jätti laudan alapuoliskon piirtymättä **ilman virheilmoitusta**, koska `Column` ei kutista
 * kiinteäkorkuisia lapsia. Vakioksi kirjoitettu korkeusbudjetti toistaisi saman vian
 * pienempänä aina kun arvaus chromen koosta on liian pieni.
 *
 * Sama koskee leveyttä: sarakeleveys johdetaan mitatusta leveydestä eikä ole 35 dp.
 * Kiinteä sarake ylittäisi hiljaa kapeamman ruudun.
 *
 * Suunta on kiinteä: 13-24 ylhäällä, kirjautunut pelaaja alhaalla, ei peilausta.
 */
object DgBoard {

    // --- Suhteet, kaikki nappulan halkaisijaan sidottuna ---------------------

    /*
     * Tässä oli `CHECKER_SIZE = 25.dp`, Designin nappulakoko sen viiteikkunassa, ja se toimi
     * nappulan absoluuttisena kattona. **Poistettu 8.8.2026, Tommin päätöksellä.**
     *
     * Syy on mitattu. Kun järjestelmäpalkit piilotettiin lautaruudulta samana iltana, kiilaan
     * mahtui 26,8 dp:n nappula, mutta vakio piti sen 25:ssä. Se teki kaikesta muusta
     * pystytilan säästöstä hyödytöntä: säästetty pikseli ei olisi voinut muuttua nappulan
     * kooksi. Katto on nyt pelkkä suhde sarakkeen leveyteen ([CHECKER_PER_COLUMN]), joten
     * lauta käyttää sen tilan joka sillä on, myös tabletilla johon vakio oli sidottu.
     *
     * Vakiota ei korvattu isommalla vakiolla samasta syystä kuin kiilan korkeutta ei
     * korvattu: parempi arvaus on yhä arvaus, ja se osuu väärin jollain toisella laitteella.
     */

    /**
     * Montako nappulaa mahtuu kiilaan **limittymättä**. Tämä on laudan oma sääntö eikä
     * mielipide: viisi on se määrä jonka pelaaja lukee yhdellä silmäyksellä ilman laskemista.
     *
     * **Vahvistui mittauksella 8.8.2026.** DailyGammonin kiilakuva on 23 x 115 pikseliä ja
     * nappula sen levyinen, eli `115 / 23 = 5` tasan. Luku ei siis ole peräisin Designista
     * eikä maustamme vaan laudan geometriasta, ja se on nyt kahden riippumattoman lähteen
     * sanoma sama asia.
     *
     * Siksi nappulan koko johdetaan myös kiilan korkeudesta eikä pelkästä leveydestä. 8.8.2026
     * puhelimella nappulat limittyivät jo neljästä, koska koko tuli leveydestä ja korkeus jäi
     * pienemmäksi kuin viisi nappulaa vaatii. Korkeus on se rajoittavampi mitta, joten se on
     * nyt mukana laskussa.
     */
    const val CHECKERS_APART = 5

    /**
     * Nopan sivu suhteessa nappulaan, sivuston omista kuvamitoista: `25 / 23`.
     *
     * **Tässä oli 10.8.2026 asti yksi vakio 1.12 (Designista), joka kantoi sekä noppaa että
     * kuutiota.** Sivustolla ne ovat eri kokoiset (mitattu 8.8.2026): noppa `25 x 25` ja
     * kuutio `29 x 29`, kun nappula on 23. Yhteinen arvo osui siis nopan kohdalle lähelle
     * (1,12 vastaan 1,09) ja kuution kohdalle selvästi alle (1,12 vastaan 1,26), eli kuutio
     * piirtyi liian pienenä nimenomaan suhteessa noppaan, jonka vieressä se on.
     *
     * Suhdeluku kirjoitetaan jakolaskuna eikä desimaalina samasta syystä kuin
     * [CHECKER_PER_COLUMN]: mitatut pikselit jäävät näkyviin, joten luvun voi tarkistaa
     * lähdettä vasten laskematta takaisinpäin.
     */
    const val DICE_PER_CHECKER = 25f / 23f

    /**
     * Kuution sivu suhteessa nappulaan, sivuston omista kuvamitoista: `29 / 23`.
     *
     * Kuutio on kahdesta palasta suurempi, joten se määrää keskitilan korkeuden
     * ([bandHeight]). Ks. sieltä miksi kiinteä korkeus jouduttiin purkamaan.
     */
    const val CUBE_PER_CHECKER = 29f / 23f

    /**
     * Nappula suhteessa sarakkeeseen. Sarake on kiilan leveys.
     *
     * **Kaksi lähdettä mitattiin 8.8.2026 ja ne ovat eri mieltä.** DG Mobilen kaappauksessa
     * nappula on 29,7 dp ja sarake 42,5 dp, eli 0,70. Sivuston kiilakuvassa nappula on kiilan
     * levyinen, eli 1,0. Tommi valitsi DG Mobilen, ja sama luku tuli Claude Designista
     * itsenäisesti (`25 / 35 = 0,714`).
     *
     * Sivusto on poikkeava, ja syy on näkyvissä: **sen kiila on yksi GIF-kuva jossa nappulat
     * on piirretty valmiiksi sisään**, joten sillä ei ole erillistä sarakkeen ja nappulan
     * suhdetta lainkaan. Siitä luettu 1,0 vastasi väärään kysymykseen.
     *
     * Kiilan suhde on tämän **seuraus eikä asetettava luku**: viisi nappulaa korkeussuunnassa
     * ja 0,70 leveyssuunnassa antavat 1:3,55. [CHECKERS_APART] sen sijaan on molempien
     * lähteiden sanoma sama luku, joten se ei ollut riidan kohteena.
     */
    const val CHECKER_PER_COLUMN = 25f / 35f

    /** Ulos kannettujen lokero suhteessa sarakkeeseen: Designin 44 / 35. */
    const val TRAY_PER_COLUMN = 44f / 35f

    /** Pelialue on 12 kiilaa + muuri = 13 saraketta, ja lokerosarake niiden oikealla. */
    const val PLAY_COLUMNS = 13

    /**
     * Keskitilan korkeus, johdettuna palasta jonka se kantaa.
     *
     * **Tässä oli 10.8.2026 asti kiinteä 59 dp**, ja sen perustelu oli että sisältö ei veny.
     * Perustelu piti paikkansa väärinpäin: pala ei veny, joten kiinteä korkeus **litisti**
     * sen. `Modifier.size` rajautuu tulevaan rajoitteeseen, ja rajoite oli kiinteä vain
     * korkeussuunnassa, joten leveys jäi vapaaksi.
     *
     * Mitattu Galaxy Tab S7+:lla 10.8.2026: nappula oli 59,8 dp, joten kuutio oli 79,5 dp
     * leveä mutta 57,9 dp korkea. Ruudulla se on suorakaide, ja mitatut suhteet eivät päässeet
     * ruudulle lainkaan. Vika oli vanha, mutta saman päivän suhdemuutos (1,12 → 1,26) leveni
     * ilman että korkeus saattoi seurata, eli se teki siitä näkyvän.
     *
     * **Mitta on kuution eikä nopan**, koska kuutio on kahdesta palasta suurempi: nopan
     * mukaan mitoitettu tila olisi rajannut kuution täsmälleen samalla tavalla kuin vakio.
     * Noppa jää siis hieman keskitilaa matalammaksi, kuten sivustollakin.
     *
     * **Suhde irtosi kuutiosta 14.8.2026 (Tommin valinta), ja mitta on nyt kaksi nappulaa.**
     * Kuution mitta oli alaraja eikä korkeus: se kertoi mihin pala mahtuu, muttei mitä
     * keskitilan pitäisi olla. Tabletilla se näytti matalalta suhteessa laudan leveyteen, ja
     * se on sama havainto joka toi kuution mitan tähän alun perin, nyt toisin päin.
     *
     * Kuutio ei silti pienene eikä litisty: se on yhä `checkerSize * CUBE_PER_CHECKER`, ja
     * uusi suhde on sitä suurempi, joten pala mahtuu tilaansa väljemmin kuin ennen.
     */
    const val BAND_PER_CHECKER = 2f

    fun bandHeight(checkerSize: Dp): Dp = checkerSize * BAND_PER_CHECKER

    /**
     * Nappulan suurin sallittu koko.
     *
     * **Tämä on olemassa jotta lauta lakkaa kasvamasta leveyden mukana** (Tommin valinta
     * 14.8.2026). Ilman ylärajaa lauta ottaa koko leveyden, ja Galaxy Tab S7+:lla se otti
     * 1285,6 dp ikkunan 1317,6 dp:stä eli jätti 32 dp. Sivupaneeli tarvitsee 150 dp, joten
     * ruudulta katosivat pisteet, pipit ja pelaajien nimet kokonaan.
     *
     * **Yläraja ratkaisee sen halvemmalla kuin keskitilan venyttäminen.** Sama tila olisi
     * saatu kasvattamalla keskitila 200 dp:hen, mutta se olisi jättänyt ylijäämäksi tasan
     * kynnyksen verran, eli asettelu olisi vaihtanut muotoaan pienimmästäkin muutoksesta.
     * Tällä arvolla ylijäämä on 184 dp eli selvästi rajan päällä.
     *
     * **Arvo oli ensin 56 dp, ja laiteajo kumosi sen samana iltana.** Se antoi ylijäämäksi
     * 144 dp eli kuusi dp kynnyksen alle, koska laskin sen ikkunan leveydestä (1317,6 dp)
     * enkä laudalle tarjotusta tilasta: ruudun oma pehmuste vie 32 dp ennen tätä laskentaa.
     * Virhe ei näkynyt testissä, koska sama väärä luku oli myös testin syötteenä, ja se
     * korjattiin molempiin.
     *
     * Puhelimella tämä ei tee mitään: siellä nappula on 30 dp:n luokkaa ja korkeus rajoittaa,
     * eli raja ei ole lähelläkään. Se on siis tabletin näköinen muutos, mutta ehto on mitta
     * eikä laite, sama muoto kuin sivupaneelilla ja napeilla.
     */
    val CHECKER_MAX: Dp = 54.dp

    /**
     * Se korkeus jonka lautaruutu kuluttaa muuhun kuin kiiloihin ja keskitilaan.
     *
     * Kaksi numeroriviä (`numberRowHeight`, `2 dp` pehmustetta molemmin puolin `labelSmall`in
     * riviä) ja `BoxWithConstraints`in oma `4 dp` pystypehmuste kahdesti. Luku on likiarvo,
     * koska numerorivi seuraa käyttäjän tekstikokoa, ja se saa olla likiarvo: se ei mitoita
     * mitään vaan valitsee kahden vaihtoehdon välillä [barsAreFree]ssä, ja siellä lähin
     * kääntöpiste on yli sadan dp:n päässä molemmista mitatuista laitteista.
     */
    val SCREEN_CHROME: Dp = 48.dp

    /**
     * Sivupaneelin korkeus jonka alapuolella pelaajakortit luopuvat tyhjästä rivistään.
     *
     * Mitattu Pixel 8a:lla 16.9.2026 (paneeli 411 dp palkit piilotettuina): keskilohko sai
     * 180 dp ja tarvitsi 216, kun lisissä oli `Skip Game`, otsikko ja yksi muistutus, joten
     * `Skip Game` piirtyi vastustajan pippien päälle. Lisät ovat Material-tekstinappeja
     * joiden vähimmäiskorkeus on 40 dp, ja niitä on enimmillään neljä; ahtaassa paneelissa
     * ne madalletaan ([COMPACT_LINK_HEIGHT], [COMPACT_BUTTON_HEIGHT]) ja kortit luopuvat
     * tyhjästä rivistään. Tabletilla paneeli on 824 dp. Raja on näiden välissä ja lähempänä
     * puhelinta, koska tyhjä rivi on Tommin valinta (14.9.2026) ja siitä luovutaan vain
     * kun se maksaa päällekkäisyyden.
     */
    val PANEL_COMPACT_BELOW: Dp = 480.dp

    /**
     * Ahtaan paneelin tekstilinkin korkeus Materialin 40 dp:n sijaan, ks. [PANEL_COMPACT_BELOW].
     * Tekstirivi on 20 dp ja alleviivaus sen alla; 24 leikkasi alleviivauksen (`kuori_kierros2_z.png`), 26 ei. Tiukin mitattu tapaus
     * (tuplaustarjous, kierrosrivi ja kuutio omassa kortissa, `kuori_kierros2.png`) tarvitsi
     * tämän; 28 jätti toisen rivin ottelukortin alle.
     */
    val COMPACT_LINK_HEIGHT: Dp = 26.dp

    /** Ahtaan paneelin reunustetun napin (`Skip Game`) korkeus 40 dp:n sijaan, sama peruste. */
    val COMPACT_BUTTON_HEIGHT: Dp = 28.dp

    /**
     * Maksavatko järjestelmän palkit mitään tällä ruudulla.
     *
     * **Tämä on syy siihen ettei lautaruutu piilota palkkeja kaikkialla** (Tommi 20.8.2026:
     * *"ainakin tabletille pitää saada ne kolme nappia tai lisää eleitä"*). Piilotuksen koko
     * peruste on että jokainen palkeille menevä dp on pois nappulan koosta. Kun [CHECKER_MAX]
     * rajoittaa jo ennen korkeutta, se peruste lakkaa olemasta: palkit eivät silloin vie
     * nappulalta mitään, ja niiden piilottaminen ostaa tyhjää mutta maksaa poistumistien.
     *
     * Ehto on mitta eikä laite, samoin kuin sivupaneelilla ja napeilla. Mitattuna 20.8.2026:
     *
     * | Laite | Ikkunan korkeus | Palkit | Nappula palkkien kanssa |
     * |---|---|---|---|
     * | Galaxy Tab S7+ vaaka | 824 dp | 48 dp | 54 dp, eli katossa |
     * | Pixel 8a vaaka | 411 dp | 72 dp | 24 dp, kun ilman on 30 dp |
     *
     * Tabletilla piilotus ei siis kasvattaisi nappulaa lainkaan, ja puhelimella se kasvattaa
     * sitä neljänneksellä. Kääntöpiste on `648 dp` sisäkorkeutta, eli molemmat laitteet ovat
     * kaukana rajasta eikä kumpikaan ole rajatapaus.
     *
     * **Sivulla oleva palkki ei ole koskaan ilmainen** ([barsWidth], 16.9.2026). Taulukon
     * 72 dp oli Pixel 8a:n eleohjaus; kolmen napin palkki on puhelimen vaakatilassa
     * sivulla, jolloin se ei maksa korkeutta lainkaan ja korkeusehto sanoi *ilmainen*, mutta
     * se vei sivupaneelista 126 px (nauha `sessio-16-9-pixel`). Tabletilla palkki on
     * alhaalla myös vaakana, joten leveys on siellä nolla ja Tommin 20.8. ehto pitää.
     */
    fun barsAreFree(windowHeight: Dp, barsHeight: Dp, barsWidth: Dp = 0.dp): Boolean =
        barsWidth <= 0.dp &&
            checkerFromHeight((windowHeight - barsHeight - SCREEN_CHROME).coerceAtLeast(1.dp)) >=
            CHECKER_MAX

    /**
     * Nappulan koko korkeudesta, kun keskitila lasketaan mukaan.
     *
     * **Kehä aukeaa suljetussa muodossa eikä vaadi kahta kierrosta.** Nappula riippuu kiilasta,
     * kiila keskitilasta ja keskitila nappulasta, mikä näyttää iteroitavalta. Sitä se ei ole:
     * kun `k` on nappula ja `H` kiilojen ja keskitilan yhteinen korkeus, niin
     * `2 * (5k) + k * KUUTIO = H`, eli `k = H / (2 * CHECKERS_APART + CUBE_PER_CHECKER)`.
     *
     * Tämä on se työ jonka `docs/UI.md` nimesi näistä hankalimmaksi ja jätti tekemättä. Se oli
     * oikea arvio siitä että kehä on olemassa, ja väärä siitä ettei sitä voi ratkaista kerralla.
     *
     * Kutsuja ottaa tästä ja leveydestä pienemmän, joten arvion virhe kutistaa nappulaa muttei
     * koskaan leikkaa mitään pois.
     */
    fun checkerFromHeight(innerHeight: Dp): Dp =
        innerHeight / (CHECKERS_APART * 2 + BAND_PER_CHECKER)

    /**
     * Kapein lokerosarake johon pelaajien nimet, pisteet ja pipit mahtuvat luettavasti.
     *
     * **Uudelleennimetty 14.8.2026, arvo säilytetty.** Nappien koti siirtyi kokonaan
     * [BoardScreen]in `MiddleStrip`iin, joten alkuperäinen mittaus (sivuston pisimmän
     * napin, `Submit Greedy Bearoff`, vaatima leveys kahdelle riville katkaistuna) ei
     * enää perustele tätä rajaa: tämä koskee nyt vain sarakkeen omaa teksti-sisältöä
     * (`TrayPlayerFacts`). Arvo on jätetty ennalleen, koska se oli jo lähellä käytännön
     * kynnystä (Galaxy Tab S7+ landscape 108 dp, pieni puhelin noin 30 dp) eikä uutta
     * mittausta pelaajatiedon luettavuudelle ole tehty. Vanha nimi oli `TRAY_ACTIONS_MIN`.
     */
    val TRAY_TEXT_MIN: Dp = 96.dp

    /**
     * Pelaajapaneelin leveys päällepiirtona.
     *
     * **Tämä ei ole enää laudalta pois 9.8.2026 alkaen.** Sama 200 dp oli siihen asti
     * laudan oikea sivusarake, eli suoraan pois siitä leveydestä jonka lauta olisi voinut
     * käyttää. Nyt paneeli on `TransientInfo`n päällepiirto ja tulee näkyviin samalla
     * eleellä kuin järjestelmän palkit, joten luku koskee vain sitä kuinka leveä paneeli
     * on laudan **päällä**.
     *
     * Sama arvo säilytettiin tarkoituksella: paneelin sisältö ei muuttunut, ja 200 dp on
     * mitattu riittäväksi pelaajan nimelle ja luvuille. Kapeampi olisi ollut muutos jota
     * mikään mittaus ei pyydä.
     */
    val INFO_PANEL_WIDTH: Dp = 200.dp

    /**
     * Kehyksen pehmuste, eli se osa kehysväriä joka jää laudan ympärille.
     *
     * Luku asuu täällä eikä pelkkänä `padding`-argumenttina, koska **laudan leveys johdetaan
     * kehyksen ulkopuolella** ([frameWidth]) ja kehys itse mittaa sen sisäpuolella. Kaksi
     * paikkaa lukee saman pehmusteen, joten kirjoitettuna kahdesti ne voisivat ajautua eri
     * lukuun ilman että kumpikaan näyttää väärältä.
     */
    val FRAME_PAD_H: Dp = 8.dp
    val FRAME_PAD_V: Dp = 6.dp

    /*
     * Tässä oli 9.8.2026 muutaman tunnin ajan nappisarakkeen mitat (`CONTROL_COLUMN_MIN` 72 dp
     * ja napin katto 116 dp). **Sarake purettiin samana päivänä** kun Tommi katsoi sitä
     * puhelimelta: paluu on järjestelmän oma reunapyyhkäisy ja uudelleenhaku on veto alaspäin,
     * joten ruudulla ei ole omia nappeja lainkaan.
     *
     * Mitattu seuraus on se miksi tämä on kirjattu eikä vain poistettu: sarake maksoi
     * leveyttä, ja leveys oli otsikkorivin poiston jälkeen se rajoittava mitta. Ilman
     * saraketta rajoittavaksi palaa korkeus, ja nappula on **29,2 dp** eli käytännössä DG
     * Mobilen mitattu 29,7 dp. Sarakkeen kanssa se oli 28,2 dp.
     *
     * *Korjaus 9.8.2026:* tässä luki 30 dp. Luku oli laskettu antamalla laskijalle ikkunan
     * korkeus sen korkeuden sijaan joka riville jää, eli `Column`in oma 8 dp:n pehmuste
     * puuttui. Sama virhe korjattiin samana päivänä `docs/UI.md`:hen ja
     * `DgBoardWidthTest`iin, ja tämä kommentti jäi sanomaan vanhaa.
     */

    /**
     * Laudan **kehyksen** leveys mitatusta tilasta, pehmuste mukaan luettuna.
     *
     * Tämä on 9.8.2026 siirretty pois kehyksen sisältä, ja syy on mitattu: kehys sai koko
     * jäljelle jäävän leveyden (`weight(1f)`) ja piirsi laudan sen keskelle, jolloin molemmin
     * puolin jäi noin 65 dp pelkkää kehysväriä jolle ei ollut käyttöä. Lauta ei voinut käyttää
     * sitä, koska nappulan koko tulee korkeudesta. Nyt leveys lasketaan ennen riviä, kehys
     * kutistuu sisältönsä levyiseksi ja vapautuva sarake menee napeille.
     *
     * Laskenta ei ole korkeusbudjetti eikä vakio: [availableHeight] on mitattu ja
     * [numberRowHeight] luettu tyyliskaalasta, ja virhe kummassa tahansa muuttaa laudan
     * leveyttä muttei voi leikata mitään pois.
     */
    fun frameWidth(availableWidth: Dp, availableHeight: Dp, numberRowHeight: Dp): Dp =
        contentWidth(
            width = availableWidth - FRAME_PAD_H * 2,
            height = availableHeight - FRAME_PAD_V * 2,
            numberRowHeight = numberRowHeight,
        ) + FRAME_PAD_H * 2

    /**
     * Laudan piirtoalueen leveys, eli sarakkeet ja lokerosarake ilman kehyksen pehmustetta.
     *
     * Nappula lasketaan ensin kummastakin suunnasta ja sarake vasta siitä. Järjestys on
     * merkitsevä sen jälkeen kun [CHECKER_PER_COLUMN] ei enää ole 1: sarake on nappulaa
     * leveämpi, ja toisin päin laskettuna väljyys kertautuisi laudan leveyteen kahdesti.
     */
    fun contentWidth(width: Dp, height: Dp, numberRowHeight: Dp): Dp {
        // Keskitilaa ei vähennetä täältä 10.8.2026 alkaen, koska sitä ei enää tiedetä ennen
        // nappulaa: [checkerFromHeight] kantaa sen itse. Vakion vähentäminen tässä olisi juuri
        // se korkeusbudjetti jota tämä tiedosto muualla kieltää.
        val inner = (height - numberRowHeight * 2).coerceAtLeast(1.dp)
        val widestColumn = (width / (PLAY_COLUMNS + TRAY_PER_COLUMN)).coerceAtLeast(1.dp)
        val checker = minOf(
            widestColumn * CHECKER_PER_COLUMN,
            checkerFromHeight(inner),
            CHECKER_MAX,
        )
        val column = checker / CHECKER_PER_COLUMN
        return column * (PLAY_COLUMNS + TRAY_PER_COLUMN)
    }

    /**
     * Lokerosarakkeen leveys kehyksen leveydestä.
     *
     * Oma funktionsa eikä laskutoimitus kutsupaikassa, ja syy on sama kuin [frameWidth]illä:
     * `BoardScreen` päättää tämän luvun perusteella mihin toiminnot piirtyvät, ja `Board`
     * piirtää sarakkeen. Kaksi kaavaa samasta luvusta voisi ajautua erilleen ilman että
     * kumpikaan näyttää väärältä, ja ero näkyisi vasta napittomana ruutuna.
     */
    fun trayWidth(frameWidth: Dp): Dp {
        val content = frameWidth - FRAME_PAD_H * 2
        return content / (PLAY_COLUMNS + TRAY_PER_COLUMN) * TRAY_PER_COLUMN
    }

    /** Litteä piirto: ei varjoja eikä gradientteja, erottelu on pelkkä reunaviiva. */
    val OUTLINE: Dp = 1.5.dp

    /**
     * Nopan ja kuution kulman pyöristys suhteessa palan sivuun.
     *
     * **Tässä oli 26.8.2026 asti kiinteä 4 dp**, ja se toisti pienoiskoossa saman virheen
     * jonka kiinteät mitat ovat tehneet tässä tiedostossa ennenkin: pala skaalautuu nappulan
     * mukana mutta pyöristys ei, joten tabletin isolla kuutiolla kulma näytti käytännössä
     * terävältä (4 / 68 = 6 %) ja puhelimen nopalla pyöreämmältä (4 / 33 = 12 %). Tommi
     * pyysi hieman lisää pyöreyttä, ja suhde antaa sen samannäköisenä joka koossa.
     *
     * Arvo on silmävarainen eikä mitattu: kuudesosa sivusta on tavallisen pelinopan
     * kulman luokkaa. Todennetaan laiteajossa, ja jos se näyttää väärältä, muutetaan
     * tätä yhtä lukua.
     */
    const val CORNER_PER_SIDE = 1f / 6f

    /**
     * Pinon tihein sallittu askel, suhteessa nappulaan (Designin 14 / 25). Viisi mahtuu
     * kiilaan erillään, ja siitä eteenpäin ne limittyvät tähän asti.
     *
     * Luku kirjoitetaan päällimmäisen nappulan sisään heti limityksen alkaessa, ei vasta kun
     * pino ei mahdu. Ero on mitattu 8.8.2026: puhelimella kiilaan mahtuu tasan kahdeksan
     * (`1 + 4 / 0,56`), joten kahdeksan nappulan pino piirtyi kokonaan ja jäi ilman lukua,
     * kun sivusto näytti samasta asemasta luvun. Ks. `CheckerStack`.
     *
     * Suhde eikä kiinteä dp, koska nappula itse skaalautuu: kiinteä 14 dp olisi pienellä
     * nappulalla harvempi kuin nappula on leveä, eli "tihein" ei olisi tiheä.
     */
    const val MIN_STACK_STEP_RATIO = 14f / 25f

    /** Nappuloita on pelissä viisitoista, joten pinoon ei koskaan piirretä enempää. */
    const val MAX_CHECKERS = 15

    // --- Pip-luku muurin välissä -------------------------------------------

    /**
     * Kilpajuoksun ero etumerkkeineen, oma miinus toisen: `-5` kun [own] on viisi pippiä
     * edellä eli pienempi, `+5` kun jäljessä ja `0` tasan. Miinus on hyvä, koska pienempi
     * pip-luku on parempi ja pelaaja lukee luvun erotuksena kuten pip-laskurit yleensä
     * (Tommin korjaus 15.9.2026 yöllä: *"pienempi on parempi ja -"*; 14.9. suunta oli
     * Clauden valinta toisin päin).
     */
    fun pipLead(own: Int, other: Int): String {
        val diff = own - other
        return if (diff > 0) "+$diff" else diff.toString()
    }

    /**
     * Sisällön yläreuna lohkossa jonka pohjalla on pino ja jonka sisällön yllä roikkuu
     * [aboveHeight] korkuinen lohko: keskellä koko [height]in suhteen, mutta ylemmäs jos
     * keskitetty sisältö osuisi pinoon, ja alemmas jos yläpuolinen lohko ei muuten
     * mahtuisi. Pinon väistö voittaa, koska pino on se jota painetaan.
     */
    fun centeredTop(height: Int, contentHeight: Int, stackHeight: Int, aboveHeight: Int = 0): Int {
        val centered = (height - contentHeight) / 2
        return maxOf(aboveHeight, minOf(centered, height - stackHeight - contentHeight))
            .coerceAtLeast(0)
            .coerceAtMost(maxOf(0, height - stackHeight - contentHeight))
    }

    // --- Paletti (X-22 Earth) ----------------------------------------------

    object Palette {
        val Felt = Color(0xFF17120F)
        val Frame = Color(0xFF6E4C33)
        val Tray = Color(0xFF4E3524)
        val TrayOutline = Color(0xFF3A2718)
        val Outline = Color(0xFF17120F)

        val WedgeEven = Color(0xFFE9973F)
        val WedgeOdd = Color(0xFF4E7B33)

        val CheckerSelf = Color(0xFFF0EBC0)
        val CheckerOpp = Color(0xFFC7333D)
        val OnCheckerSelf = Color(0xFF17120F)
        val OnCheckerOpp = Color(0xFFF0E7D6)

        /** Tyhjä piste tai tuntematon omistaja. Ei kummankaan pelaajan väri. */
        val CheckerUnknown = Color(0xFF3A2718)

        val Cube = Color(0xFF2E6B62)
        val OnCube = Color(0xFFF0EBC0)

        /**
         * Kuution väri vaaleana. Peruuttamattoman teon reunus kaistaa vasten:
         * tumma kuutioteal olisi kaistalla 1,24:1 eli näkymätön, tämä on 4,52:1.
         */
        val CubeSoft = Color(0xFF8FD4C6)

        /**
         * Kuutiomuistutuksen lasersilmä robotin ykköspinnassa (Tommin valinta 3.9.2026).
         *
         * Purppura eikä punainen, koska punainen on jo vastustajan nappulan väri
         * ([CheckerOpp]) ja pip-ristiriidan väri ([PipConflict]). Kolmas punainen esine
         * laudalla väittäisi sukulaisuutta kahteen asiaan jotka eivät liity muistutukseen.
         * Purppuraa ei ole laudalla muualla, joten se ei voi sekoittua pelitietoon.
         */
        val CubeLaser = Color(0xFFC77DFF)

        /**
         * Lasersilmän lähtöväri, josta se jäähtyy purppuraan pulssien mitassa
         * (Tommin pyyntö 3.9.2026). Punainen kelpaa välähdykseksi mutta ei tilaksi,
         * ks. [CubeLaser].
         */
        val CubeLaserHot = Color(0xFFFF4136)

        val PanelBg = Color(0xFF1B1712)
        val PanelOutline = Color(0xFF6E6154)
        val Accent = Color(0xFFE9973F)

        val TextPrimary = Color(0xFFF0E7D6)
        val TextSecondary = Color(0xFFE4D6BE)
        val TextMuted = Color(0xFF9A8D7A)

        val PipOk = Color(0xFF7FA88C)
        val PipConflict = Color(0xFFE2555F)
    }

    // --- Sivuston skeemavärit (sivustouskollinen tila) -----------------------

    /**
     * DailyGammonin omien lautakuvien värit, mitattuna kuvatiedostoista 25.8.2026.
     *
     * Lähde on sivuston omat GIFit (`/images/1/` Classic, `/images/2/` Blue/White):
     * kiilakuvien `pt_lt_up0`/`pt_dk_up0` yleisimmät pikseliarvot ja nappulakuvien
     * `pt_lt_up_y3`/`pt_dk_up_b3` sekä noppakuvien `die_y3`/`die_b3` täyttövärit. Kuvien
     * haku ei kuluta jonoa, koska ne ovat staattisia tiedostoja eivätkä `/bg/`-polkuja.
     *
     * **Vain kiilat ja nappulat**, koska sivustouskollisuus koskee vain sitä mitä sivuston
     * asetus sanoo (`docs/ASETUKSET.md` luku 4): kehys, huopa, lokerot, kuutio ja tekstit
     * pysyvät sovelluksen omina molemmissa tiloissa. Nopat perivät nappulavärin samasta
     * syystä kuin X-22:ssa: sivustolla nopan väri on heittäjän nappulaväri.
     *
     * Pariteetti on mitattu fixturesta `move_board.html`: ylärivillä 13 on vaalea ja 14
     * tumma, eli **parillinen piste on tumma kiila**. Sama vuorottelu kuin X-22:ssa, vain
     * arvot vaihtuvat.
     */
    interface SiteScheme {
        val wedgeDark: Color
        val wedgeLight: Color
        val checkerYellow: Color
        val onCheckerYellow: Color
        val checkerBlue: Color
        val onCheckerBlue: Color
    }

    /**
     * Monte Carlo variant (Tommin tilaus 13.9.2026): turnauskokoisen puukotelolaudan värit
     * Gammon Geaux 2026 -finaalin lähetyskuvasta. Arvot on luettu kuvasta silmällä eikä
     * mitattu, ja ne ovat käsikokeilun lähtöarvot (`docs/ASETUKSET.md` luku 3). Nappulat
     * ovat tasavärit; lähetyslaudan marmorointi jäi myöhemmäksi.
     */
    object MonteCarloVariant {
        val Felt = Color(0xFF3D7A3F)
        val Frame = Color(0xFF5A3D27)
        val Tray = Color(0xFF44301E)
        val TrayOutline = Color(0xFF2E2014)
        val WedgeEven = Color(0xFFE07338)
        val WedgeOdd = Color(0xFFEDE5D0)
        val CheckerSelf = Color(0xFFF3F0E8)
        val OnCheckerSelf = Color(0xFF17120F)
        val CheckerOpp = Color(0xFF1C1B1B)
        val OnCheckerOpp = Color(0xFFF0E7D6)
    }

    /** Classic (`/images/1/`): hopeanharmaa tausta, vihreät kiilat, keltainen ja sininen. */
    object SiteClassic : SiteScheme {
        override val wedgeDark = Color(0xFF006600)
        override val wedgeLight = Color(0xFF00CC00)
        override val checkerYellow = Color(0xFFFFCC33)
        override val onCheckerYellow = Color(0xFF000000)
        override val checkerBlue = Color(0xFF3333FF)
        override val onCheckerBlue = Color(0xFFFFFFFF)
    }

    /**
     * Blue/White (`/images/2/`): keltaisen nappulan kuva on tässä skeemassa valkoinen ja
     * "tumma" kiila vaaleansininen. Nimet ovat sivun ALT-koodien värejä (`y`/`b`), eivät
     * väitteitä ruudulla näkyvästä sävystä; valkoinen nappula erottuu valkoisesta kiilasta
     * reunaviivalla, joka on sovelluksen oma [Palette.Outline] molemmissa tiloissa.
     */
    object SiteBlueWhite : SiteScheme {
        override val wedgeDark = Color(0xFF33CCFF)
        override val wedgeLight = Color(0xFFFFFFFF)
        override val checkerYellow = Color(0xFFFFFFFF)
        override val onCheckerYellow = Color(0xFF333333)
        override val checkerBlue = Color(0xFF0066CC)
        override val onCheckerBlue = Color(0xFFFFFFFF)
    }
}
