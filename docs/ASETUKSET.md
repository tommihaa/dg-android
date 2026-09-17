# Asetukset: mistä luetaan, mihin vaikuttavat, miten kirjoitetaan

Tämä dokumentti on **designehdotus, ei kaanoni**. Se vastaa `docs/AVOIMET.md`:n kahteen
kärkikohtaan (sivustouskollinen oletuslauta 24.8.2026 ja kirjoittava asetusnäkymä
24.8.2026). Kohdat kirjattiin erikseen mutta ne koskevat **samaa yhtätoista lomakekenttää**,
joten yksi dokumentti on vähemmän ristiriitaista kuin kaksi. Suunta on Tommin päättämä,
tämän sisältö ei ole: valinnat ovat luvussa 6, ja ne on tarkoitus kuitata ennen kuin koodia
kirjoitetaan. Kaikkiin on vastattu 24.8.2026.

## 1. Kenttäkartta: mitä lomakkeella oikeasti on

Mitattu tiedostosta `raakasivut/profile_settings.html` (kaapattu 1.8.2026, fixture
`core-scrape/src/test/resources/fixtures/settings_page.html`). Kenttiä on **11**: kahdeksan
valintaruutua nimillä `0`..`7` ja kolme radioryhmää nimillä `order`, `color`, `board`.
Sarake *vaikutus lautaan* kertoo, muuttaako asetus sitä mitä lautanäkymä piirtää.

| Kenttä | Selite sivustolla | Vaikutus lautaan |
|---|---|---|
| `0` | Confirmation on offering doubles | ei |
| `1` | Confirmation on accepting doubles | ei |
| `2` | Confirmation on declining doubles | ei |
| `3` | Player Name links on game page | **kyllä**, pudottaa paneelit |
| `4` | Skip Opponent's "Roll Dice" pages | ei lautaan, muuttaa `/bg/nextgamea` |
| `5` | Skip *all* automatic pages | ei lautaan, muuttaa `/bg/nextgamea` |
| `6` | Hide pip counts | **kyllä** |
| `7` | Home boards on left side | **kyllä**, peilaa numerot, laudan ja lokerosarakkeen (8.9.2026). **Ei enää siirrä sivupaneelia**, joka seuraa kätisyyttä 9.9.2026 alkaen (luku 3). Tommilla **pois** 9.9.2026 iltapäivästä alkaen |
| `order` | "Next" Match Ordering, 4 vaihtoehtoa | ei |
| `color` | Background Color, 6 vaihtoehtoa | sivun tausta, **jää ulos** (kohta A) |
| `board` | Board Scheme, 3 vaihtoehtoa | **kyllä** |

**Sivustouskollinen lauta on siis kolme kenttää eikä yksitoista** (`6`, `7`, `board`), ja
`color` olisi ollut neljäs mutta jää ulos (kohta A). Tämä on kartan tärkein tulos: kysymys
*"mitä asetukset toteutuu kattaa"* on kapeampi kuin miltä se `AVOIMET.md`:ssä näytti.

**Kenttä `3` mitattiin 24.8.2026, ja se on neljäs riski jota ei ollut listalla.** Tulos ja
sen luvut ovat `docs/KOHDE.md`:ssä; tähän kuuluu se osa joka muuttaa tätä designia. Ilman
pelaajanimen linkkiä `parsePlayers` palauttaa **tyhjän listan**, koska linkki on sen ainoa
valitsin. Lauta jäsentyy silti, joten mikään ei kerro puutteesta: ruudulta katoavat nimet,
pisteet ja pip-luvut, pip-vahdilla ei ole mitään mihin verrata, ja koska rooli tunnetaan vain
vahdin kautta, roolivärit menevät varasuunnalle.

**Tämä on korjattava ennen lautatiloja eikä niiden mukana**, ja syy on ajoitus. Vika koskee
sovellusta jo nyt, mutta se ei ole näkynyt kenellekään: Tommilla asetus on päällä. Uusi
käyttäjä on juuri se jota varten sivustouskollinen tila rakennetaan, ja hänellä asetus voi
olla pois. Tila jonka arvo on rinnastettavuus selaimeen näyttäisi silloin laudan ilman
nimiä, pisteitä ja pip-lukuja, eli epäsuhta olisi suurin juuri siinä tilassa.

**Korjattu 24.8.2026, Tommin päätös tehdä se ennen yhdistelmämittausta.** Sivu säilyttää
solun ja sen sisällön (`<B>vastapelaaja</B>`, `155 pips`, `score: 7`); vain `<a>` katoaa.
`parsePlayers` tunnistaa paneelin nyt kahdella tuntomerkillä: entinen käyttäjälinkki ja sen
rinnalla pistekenttä solun **omasta** tekstistä (`td:matchesOwn(score:)`). Nimi luetaan
linkistä kun se on, muuten solun ensimmäisestä `<b>`:stä; pistekenttä on samassa solussa ja
sekin lihavoitu, mutta se tulee nimen jälkeen, joten järjestys erottaa ne tulkitsematta
tekstiä. `PlayerPanel.userId` on tässä tilassa null, koska käyttäjänumero on vain linkissä.
Fixture `move_board_no_name_links.html`, testi `BoardParserPlayerLinksTest`.

Linkkiehto jäi paikalleen vaikka uusi kattaa mitatut sivut yksinään: pistekenttää ei ole
todistettu olevan joka pelimuodossa, joten jälkimmäinen on varasuunta eikä korvaaja.

**Yksi väite jouduttiin korjaamaan kirjoittaessa.** Ensimmäinen versio perusteli tiukempaa
valitsinta sillä että väljempi `td:contains(score:)` poimisi laudan ympäriltä ulompia
soluja. Testi kaatui, koska sivun taulukot ovat litteät: kaksi `<table>`ä eikä kumpikaan
solun sisällä, joten ulompaa solua ei ole olemassakaan ja molemmat muodot antavat tänään
saman tuloksen. Tiukempi jäi silti valinnaksi tulevaisuuden varalta, ja testi vertaa lukuja
keskenään, jolloin rakenteen muuttuminen näkyy punaisena.

**Kohta E mitattiin samana päivänä, ja korjauksen viimeinen oletus piti.** Kolmas otos
samasta ottelusta, linkit pois ja `Hide pip counts` päällä: 0 käyttäjälinkkiä, 2
pistekenttäsolua, 2 paneelia, nimet ja pisteet tallessa, pipit null. `score:` on siis
paikallaan myös silloin kun pip-rivi ei ole, eikä tunnistus jää yhden oletuksen varaan.
Fixture `move_board_no_links_no_pips.html`.

Sama ajo todensi korjauksen livenä: `paneeleja=2` siinä tapauksessa jossa se oli ennen
korjausta 0. `PlayerPanel.backgroundColor` ei silti kelpaa kolmanneksi tuntomerkiksi, koska
se on kanonissa hypoteesi.

## 2. Mistä asetukset luetaan

**Asetussivulta, ei laudalta.** `/bg/profile` on kanonissa kuluttamaton kohde,
`SettingsParser` on olemassa ja `DgPages.isSettingsPage` tunnistaa vastauksen. Laudalta
päättely olisi houkuttelevaa, koska lauta haetaan joka tapauksessa, mutta se kaatuu kolmeen
kohtaan:

1. **`Hide pip counts` ei ole pääteltävissä ilman että vahti hiljenee.** Puuttuva pip-luku on
   laudalla juuri se merkki jonka pip-vahti tulkitsee epäonnistuneeksi jäsennykseksi. Jos
   sovellus alkaisi lukea puuttumisen asetukseksi, se selittäisi oman vikansa pois. Vahti on
   sovelluksen ainoa automaattinen tunnistin hiljaa väärin menneelle jäsennykselle, eikä sitä
   saa maksaa tästä.
2. **`Home boards on left side` on tarkoituksella hävitetty.** `BoardParser` lukee pisteen
   numeron sivun omasta numerorivistä, joten peilaus normalisoituu pois ennen kuin malli
   syntyy. Sivun asettelusta takaisin päättely olisi juuri sen palauttamista mitä jäsennin
   heittää pois, ja se päättely meni kerran jo väärin (korjattu 1.8.2026).
3. **`color` ei näy laudalta mitatusti.** Neljä riskiotosta (`risk_baseline.html`,
   `risk_hide_pips.html`, `risk_board_classic.html`, `risk_board_mini.html`) ovat kaikki
   `<BODY BGCOLOR=#FFFFFF>`, ja kaikki neljä otettiin taustavärillä Classic White. Otos ei siis
   todista sitä että tausta seuraisi asetusta eikä sitäkään ettei se seuraisi.

Vain `board` on laudalta luotettavasti luettavissa (`BoardState.scheme`), ja se on yksi
kolmesta. Yhden kentän vuoksi ei kannata pitää kahta lähdettä.

**Seuraus jota ei saa ohittaa: lautanäkymä tarvitsee tiedon jota se ei itse hae.** Asetukset
luetaan siis kerran ja säilötään laitteelle, ja lauta lukee säilöä. Muoto on sama kuin
`SharedPrefsCredentialsStore`illa: oma `SharedPreferences`-tiedosto ja pieni rajapinta, ei
uutta kantataulua, koska kyse on yhdestätoista kentästä eikä historiasta.

**Haku tehdään sovelluksen avauksessa, Tommin päätös 24.8.2026.** Ankkuri on
tunnistautuminen; koska `APP_LOCK_ENABLED` on `false` (päätös 21.8.2026), haku tehdään
toistaiseksi samassa kohdassa käynnistystä jossa lukko laukeaisi. Kun lukko joskus palaa,
haku siirtyy sen jälkeen eikä muuta tarvita. Näin selaimessa tehty asetusmuutos näkyy
laudalla seuraavalla avauksella eikä vasta seuraavassa asetusruudun käynnissä.

Kolme ehtoa jotka tämä valinta asettaa, koska muuten se kaventaisi väitteitä hiljaa:

1. **Haku ei kuulu `TopViewModel`ille.** Se väittää hakevansa oma-aloitteisesti vain Top
   Pagen, ja `TopViewModelTest` todistaa väitteen kirjaamalla jokaisen polun. Asetushaku saa
   siis oman omistajansa, jolloin kummankin polkuluettelo pysyy täydellisenä lausumana.
   Erillään pidettynä väite ei muutu kommentiksi vaan säilyy testattavana.
2. **Epäonnistunut haku ei tyhjennä säilöä.** Käynnistys lentotilassa on mitattu tavallinen
   tilanne tässä sovelluksessa, ja tyhjä lukema piirtäisi laudan väärillä asetuksilla.
   Vanha lukema on vanhentunutta mutta oikeaa tietoa; sama muoto kuin näkymämallien
   `refreshing`-lipulla.
3. **Haku ei estä eikä hidasta käynnistystä.** Lauta piirtyy sillä mitä säilössä on, ja
   uusi lukema vaikuttaa siitä eteenpäin. Asetusten odottaminen käynnistyksessä maksaisi
   verkkoviiveen joka kerta siitä että kolme kenttää voi olla muuttunut.

## 3. Missä sovelluksen oma valinta asuu

Paikallinen asetus `board_style`, kolme arvoa: `SITE` (sivustouskollinen), `X22` ja
`MONTE_CARLO_VARIANT`. Oletus on `SITE`, koska se on Tommin päätöksen sisältö; Tommilla
itsellään arvo on `X22`.

**Kolmas arvo tuli 13.9.2026 Tommin tilauksesta**, malli Gammon Geaux New Orleans 2026
-finaalin lähetyslaudasta (Ace Point Backgammon). Laji eikä merkki: turnauskokoinen
puukotelolauta, vihreä huopa, oranssi ja kermanvalkoinen kiila vuorotellen, mustat ja valkoiset
nappulat. Valmistajaa ei todennettu kuvasta, joten nimi on Tommin antama *Monte Carlo variant*
eikä valmistajan nimi. Kolme päätöstä samalla kertaa:

- **Oma nappula on valkoinen, vastustaja musta.** Sama logiikka kuin X-22:ssa, jossa oma on
  vaalea. Väri on rooli kuten X-22:ssa, ei sivuston väri.
- **Tasaväri ensin, marmorointi myöhemmin.** Lähetyslaudan nappulat ovat pyörrekuvioiset,
  mutta kuvio on oma piirtotyö ja voi haitata luettavuutta. Käsikokeilu laitteella ennen
  hienosäätöä.
- **Tyyli saa vaihtaa huovan ja kehyksen värin.** Tämä on kaanonimuutos, kirjattu luvussa 4.

Valinta asuu **sovelluksen omassa osiossa eikä DailyGammon-asetusten seassa**, ja ero on
kirjoitettava ruudulle asti. Sama ruutu näyttäisi muuten kahdenlaisia asetuksia joista toiset
lähtevät verkkoon ja toiset eivät, eikä käyttäjä voi nähdä erotusta ruudusta. Ehdotus:
asetusnäkymän yläosassa oma osio otsikolla joka sanoo että nämä koskevat vain tätä laitetta,
ja sivuston lomake sen alla omana kokonaisuutenaan sivuston omassa järjestyksessä.

**Kaksi paikallista valintaa lisää 27.8.2026, Tommin päätös: omia kytkimiä eikä
lautatilaan sidottuja.** Vaihtoehto jossa `board_style` olisi määrännyt myös nämä
punnittiin ja Tommi hylkäsi sen; syy on tässä luvussa, ja
käytännön seuraus on että X-22-laudalla voi pitää sivuston noppaesityksen.

- **`dice_style`**: `COUNTER` (yksi kuutio jokaista siirtoa kohti, tupla laajenee
  neljäksi) tai `SITE` (tasan kaksi kuutiota kuten sivustolla). **Pelattu kuutio ei katoa
  vaan harmaantuu** kummassakin (Tommin tilaus 2.9.2026: *"älä poista sitä näytöltä vaan
  harmauta se"*, oletuksen korvaus eikä kolmas arvo). `Die.spent` kantaa osuuden 0..1:
  laskurissa ja sivuston ei-tuplassa 0 tai 1, sivuston tuplassa puolikas askelta kohti,
  koska kaksi kuutiota kantaa neljä askelta. Piirto himmentää kuution 65 prosenttia
  (`SPENT_DIE_DIM`), jolloin silmät jäävät luettaviksi. Todennettu kuoriproxylla 2.9.2026
  tallennetusta tuplalaudasta (`sessio-2-9/0179`): esipoimittu muurilta tulo harmaana heti,
  napautus harmensi toisen, rivi pysyi paikallaan. Ennen 2.9.2026 laskuri poisti
  kuution siirron myötä. Sivuston oma käytös on mitattu 27.8.2026
  kaappauksista: 2698 lautasivua ja jokaisella tasan kaksi `die_`-kuvaa, myös tuplan
  kokoamisen keskellä (`sessio/0154`–`0157`, tuplakakkonen `move_v`→`vvmm`). Ero on
  pelkkää esitystä: kokoamisen laillisuus ja kirjainjono lasketaan molemmissa samoin.
- **`score_style`**: `AWAY` (`12-away`, nykyinen) tai `SITE` (sivun oma pistekenttä
  sellaisenaan). Rahapelissä away ei ole olemassa, jolloin molemmat tilat näyttävät sivun
  kentän; se oli tähänkin asti varareitti eikä muutu.

**Kolmas paikallinen kytkin 2.9.2026, ja se on eri lajia: teko eikä esitys.**
`forced_steps` (`ForcedStepsStore`, oletus pois) poimii pakolliset askeleet valmiiksi kun
paikallinen kokoaminen alkaa. Pakollinen askel on askel joka on mukana jokaisessa
laillisessa vuorossa (`CompositionSession.forcedSteps`), ja lukutapa on Tommin kuittaama
2.9.2026: *"jos joku siirto on pakollinen, niin se pelataan"*, siirto on askel eikä vuoro.
Lähetys jää aina pelaajalle, ja `Undo Move` tyhjentää myös esipoimitut. Mittaus ja
perusteet ovat `docs/AVOIMET.md`:ssä (pakkosiirrot), mekanismi `docs/ARKKITEHTUURI.md`:ssä.
Oletus on pois riskiluokan takia eikä maun: tämä on ensimmäinen laiteasetus joka tekee
laudalle jotain pelaajan puolesta, ja sellainen otetaan käyttöön nähtynä eikä oletuksena.

*Laiteajo oikealla sivustolla 2.9.2026 (kytkin päällä, proxy `raakasivut/sessio-2-9`):*
illan 76 asemasta 12:ssa oli pakollinen askel, ja lähetetty siirto sisälsi sen 12/12
(korpusmittauksen istuntokohtainen rivi, `:core-scrape:korpusTest`). Kaappaus ei erota
esipoimittua askelta napautetusta, joten se todistaa vain ettei esipoiminta ollut koskaan
ristiriidassa pelatun kanssa; sen että askel oli valmiina laudalla näkee vain pelaaja.

**Neljäs paikallinen kytkin 2.9.2026, samaa lajia: ahne uloskanto.** `greedy_bearoff`
(`GreedyBearoffStore`, oletus pois) poimii ahneen uloskannon valmiiksi kun kontaktia ei ole
(`CompositionSession.greedySteps`, `prefillGreedy`). Tommin tilaus 2.9.2026 pelisession
keskeltä, lukutapa kuitattu samana päivänä: sovellus poimii, `Submit Move` jää pelaajalle,
ja ehto lasketaan itse asemasta eikä sivuston napista. Sääntö on mitattu sivuston omasta
`Submit Greedy Bearoff` -esitäytöstä (`GreedyKorpusTest`, 56 lautaa): esitäyttö on aina
vuoro joka kantaa ulos eniten, ja sivusto tarjoaa nappinsa täsmälleen silloin kun sellaisen
vuoron loppuasema on yksikäsitteinen eikä kontaktia ole (56 lautaa nappi, 18 kontaktitonta
uloskantolautaa ilman nappia, kaikilla 18:lla useampi ahne loppuasema; 24 kontaktillista
ilman nappia). Sovellus tekee saman ja jatkaa siitä mihin sivusto jättää: kun ahneita
loppuasemia on useampi, se poimii niiden yhteisen osan ja valinta jää pelaajalle. Korpuksessa
sovelluksen poiminta on koko vuoro 56/56 sivuston greedy-laudalla, tyhjä 24/24 kontaktissa
ja yhteinen osa 18/18 muulla (nollasta kolmeen askelta). Kun ahne poimii jotain, se sisältää
pakolliset askeleet, joten `forced_steps` kokeillaan vain jos ahne ei poiminut mitään.
Todennettu kuoriproxylla 2.9.2026 kahdella illan laudalla: Golden (5 ja 4) avautui koko vuoro
poimittuna ja `Submit Move` näkyvissä, mimi711 (tupla 2) kolme askelta neljästä poimittuna ja
viimeinen valintana. *Oikeaa sivustoa vasten 2.9.2026 yöllä* (`raakasivut/sessio-2-9-yo`):
neljä uloskantolautaa, kolmella koko vuoro sama kuin sivuston oma greedy ja neljännellä
yhteinen osa jota Tommi täydensi, sivusto hyväksyi kaikki neljä (`docs/TESTAUS.md` kohta 7).

**Viides paikallinen kytkin 2.9.2026: lukuruutujen pystylukko.** `portrait_lock`
(`PortraitLockStore`, **oletus päällä**) pitää luettelon, viestit, foorumin ja asetukset
pystyssä kuten 24.8.2026 päätös sanoo (`docs/UI.md`, Suunta lukittiin). Pois kytkettynä ne
seuraavat laitteen asentoa. Lauta on vaakaan aina, asetuksesta riippumatta; se on Tommin
rajaus samassa päätöksessä. Oletus on päällä koska lukon mittaus on yhä voimassa puhelimella
(vaakaikkunassa listalle jäi nolla korkeutta), ja kytkin syntyi tabletin tarpeesta: kädessä
vaakana oleva tabletti kääntyi pystyyn kun jono tyhjeni. Tommin laitteella kytkin jätettiin
pois päältä laiteajon jälkeen.

**Kuudes paikallinen valinta 3.9.2026, ja se on lista eikä kytkin: fraasinapit.** `phrases`
(`PhraseStore`, `SharedPrefsPhrases`) on viestiruudun fraasinappien lista tallennusjärjestyksessä.
Oletus on kohdan 58 rituaalifraasit `hi`, `gg`, `ty gg u2`, ja säilö erottaa oletuksen
tyhjästä: käyttäjän tyhjentämä lista pysyy tyhjänä. Fraasit ovat lähetettävän viestin
sisältöä eivätkä UI-tekstiä, joten ne eivät ole `strings.xml`:ssä. Lisäys ja poisto:
`docs/UI.md` › *Fraasin lisäys ja poisto*. Ei asetuslomakkeella, koska muokkaus tapahtuu
siellä missä nappeja käytetään: viestiruudun vastauskentässä ja laudan chat-kortissa, ja
lista on yksi (`PhraseBook`).

**Seitsemäs ja kahdeksas paikallinen kytkin 4.9.2026: noppien painallus tekona.**
`dice_submit` ja `dice_swap` (`DiceSubmitStore`, `DiceSwapStore`, **molemmat oletuksena
pois**) tekevät omista nopista napin. Täydellä vuorolla painallus lähettää siirron, ja
koskemattomilla nopilla se vaihtaa noppien järjestyksen. Tommin tilaus: *"kun kaikki nopat
on harmaita, niin noppia painamalla toteutuu Submit move"* ja *"kun yksikään noppa ei ole
harmaa, niin noppia painamalla toteutuu Swap Dice"*.

Kaksi kytkintä eikä yksi, koska teot ovat eri painoisia. Vaihdon voi perua painamalla
uudestaan, lähetys päättää vuoron. Sama laji kuin pakkosiirroilla ja ahneella uloskannolla:
nämä eivät muuta esitystä vaan sitä mitä laudalla oleva painallus tekee, ja siksi oletus on
pois.

**Kaksi rajaa kysyttiin ja Tommi kuittasi ne samana päivänä.** Lähetyksen ehto on vuoron
täysinäisyys eikä noppien väri: pakotetussa vuorossa toinen noppa jää harmaantumatta, ja
juuri silloin sivusto ei salli enempää, joten kirjaimellinen luenta olisi jättänyt
tavallisimman lyhyen vuoron ulos. Ja ehto luetaan **paikallisesta kokoamisesta**: harmaus on
kokoamisen ominaisuus, joten sivun omalla laudalla painallus ei tee mitään.

Sääntö asuu yhtenä funktiona (`BoardScreen.diceTapFor`, testi `DiceTapTest`) eikä kahtena
ehtona piirtokohdassa, koska noppalokeroita on kaksi. Lähetys ei kysy vahvistusta, ja se on
valinta: sama teko `Submit Move` -napista ei kysy sitäkään, ja kysyminen vain toisessa
reitissä tekisi niistä eri mieliset. Suoja on kytkin joka on oletuksena pois.

**Oletukset ovat nykyinen käytös** (`COUNTER` ja `AWAY`), eikä se seuraa `board_style`n
sivustouskollisesta oletuksesta: kumpikin kytkin syntyi havainnosta eikä viasta, ja
oletuksen vaihtaminen olisi eri päätös jota ei ole kysytty. Jos se joskus kysytään,
uuden käyttäjän tuttuusperuste puoltaisi `SITE`-arvoja.

**Yhdeksäs paikallinen kytkin 6.9.2026: taustakuvion sumi-e-tila.** `sumi_e`
(`SkyThemeStore`, **oletus pois**) vaihtaa tekstiruutujen taustakuvion linnut vermilioniin ja
lisää taivaalle kuun; kuutiot ja asettelu ovat samat molemmissa tiloissa. Tommin päätös
viidestä kuvaksi piirretystä kandidaatista: *"1 oletukseksi, 4 asetukseksi, lukupinta
kelpaa"*, eli voimakas yksivärinen on oletus ja yksi värillinen on kytkin. Oletus on pois
koska Tommin oma sana oli *"värilliset tessellaatiot app-asetus"*: väri on valinta eikä
lähtötila. Rivi on asetusruudun laiteosion viimeinen, ja vaihto näkyy heti ruudun omassa
taustassa. Mittaukset ja lukupinta: `docs/UI.md` › *Tyhjä tila täyteen*.

**Kymmenes paikallinen kytkin 6.9.2026 illalla: taustakuvio päällä vai pois.** `pattern`
(`SkyThemeStore`, **oletus pois**) kattaa molemmat teemat. Päällä vaalea piirtää
mustetessellaation kuten 6.9. päivästä alkaen, ja tumma värillisen taivaan jossa jokainen
ruutu arpoo oman palettinsa ja hahmonsa viidestä (linnut, kalat, sienet, lehdet, tähdet).
Pois kytkettynä kumpikin teema on pelkkä taustaväri. **Luolan seinä tuli 7.9.2026 molempiin teemoihin
saman kytkimen alle**, ensin yöllä tumman kuudentena lippuna ja vaalean arvan toisena
puolikkaana, ja saman päivän jatkosessiossa **täytekuvana**: tessellaatio on aina tausta kun
kytkin on päällä, ja seinä piirretään vain sisällön alle jäävään vapaaseen tilaan kun sitä on
vähintään 180 dp. Teeman taustaa ei enää vaihdeta kiveksi. Sumi-e-alakytkin koskee vain
mustetessellaatiota, eikä kytkimiä tullut lisää (Tommin valinta kolmesta: *"yksi kytkin kuten
nyt"*). Saman yön kaksi aiempaa versiota (käsijälki mustalla, viisi käsikuviota) hylättiin
laitekuvista. Ks. `docs/UI.md` › *Tausta ja täytekuva*. Tommin tilaus: *"vie koodiin ja
sovelluksen omana asetuksena haluaako yksivärisen taustan vai näitä"*, ja oletus heti perään:
*"oletuksena taustakuva vaalealla ja tummalla taustalla pois päältä."* Ensimmäinen versio
samana iltana oli tumman oma kytkin oletuksena päällä ja vaalea ilman kytkintä; se eli yhden
commitin. Rivi on laiteosion Background-otsikon ensimmäinen, ja sumi-e on sen alla
alakytkimenä, joka vaikuttaa vain kuvion ollessa päällä vaaleassa teemassa. Päätökset ja
mittaukset: `docs/UI.md` › *Tumma teema sai kuvion*.

**Kätisyys, 9.9.2026, ja se on ensimmäinen paikallinen kytkin joka kuvaa pelaajaa eikä
sovellusta.** `handedness` (`HandednessStore`, oletus oikeakätinen) kertoo kummalla kädellä
pelaaja siirtää nappuloita, ja **sivupaneeli menee vastakkaiselle puolelle**: oikeakätisellä
vasemmalle, vasenkätisellä oikealle. Tommin päätös illalla: *"korjataan kaanoni, nimeä se
kätisyydeksi"*.

Kytkin korvaa 8.9.2026 tehdyn ratkaisun jossa paneelin puoli johdettiin sivuston `Home boards
on left side` -kentästä (kenttä `7`). Peruste kumoutui käytössä: peilatessa siirtokäsi ei
vaihdu, vain paneeli siirtyy, joten puoli riippuu kätisyydestä eikä laudan suunnasta.
Todistus, kumoutumisen taulukko ja se miksi vasenkätisyys on sama korjaus eikä eri kysymys:
`docs/UI.md` › *Puoli seuraa kätisyyttä*. Pelaajan oma kuvaus työnjaosta on `SUBSTANSSI.md`
kohdassa 103.

**Miksi sovelluksen oma eikä tilikenttä.** Kätisyys on pelaajan ja laitteen ominaisuus, ja
sama tili voi olla auki selaimessa jossa paneelia ei ole lainkaan. Sivustolle ei siis ole
mitään lähetettävää, ja tämä on samaa lajia kuin pystylukko: ei lähde koskaan verkkoon.

**Oletus on oikeakätinen**, jolloin käytös on sama kuin ennen muutosta. Se ei ole kannanotto
yleisyyteen vaan valinta joka pitää nykyisen käytöksen ennallaan niille jotka eivät kytkintä
löydä; vasenkätisen on se löydettävä, ja se on tämän ratkaisun tunnustettu hinta.

**Peilaus vapautui samalla.** `Home boards on left side` kääntää nyt laudan, pistenumerot ja
lokerosarakkeen muttei paneelia, joten sen voi ottaa käyttöön ilman että sivupaneeli siirtyy
siirtokäden alle. Juuri se hinta sai Tommin kytkemään peilauksen pois 9.9.2026 iltapäivällä.

## 4. Mikä vaihtuu tilan mukana ja mikä ei

Yksi sääntö kattaa oletuslautakohdan avoimet kysymykset 1, 4 ja 5 (mitä *asetukset
toteutuu* kattaa, koskevatko värisidonnaiset osat molempia tiloja, symmetriset lokerot):

> **Sivustouskollisuus koskee vain sitä mitä sivuston asetus sanoo. Kaikki muu on sovelluksen
> omaa ja pysyy molemmissa tiloissa samana.**

**Tarkennus 13.9.2026 (Tommin päätös), kun kolmas tyyli tuli.** Sääntö koskee `SITE`n ja
sovelluksen omien tyylien eroa, ei sovelluksen omien tyylien keskinäistä eroa. Sovelluksen omat
tyylit (`X22`, `MONTE_CARLO_VARIANT`) saavat erota toisistaan myös huovan, kehyksen ja lokeron
värissä, koska vihreä huopa on juuri se piirre joka tekee lähetyslaudasta tunnistettavan.
`SITE` piirtää nämä yhä X-22:n väreillä, koska sivusto ei sano niistä mitään. Geometria pysyy
kaikissa tyyleissä samana, ja sivupaneelin kortit, nappirivi ja muut sovelluksen omat pinnat
pysyvät ominaan tyylistä riippumatta; vain lauta vaihtuu. `BoardLook` kantaa siis 13.9.2026
alkaen myös huovan, kehyksen ja lokeron värit sekä roolivärit. Ulos kannettujen sarake
(`trayColumn`) on samaa lajia: X-22:ssa ja `SITE`ssä huovan värinen kuten ennen, variantissa
kehyksen väri, koska Tommin sana samana iltana oli *"bear-off-paneeli ei"* saa olla huovan
värinen. Keskikaista on variantissa huovan värinen (`band`, Tommin päätös samana iltana:
*"keskikaista oli parempi huovan värisenä"*), muissa tyyleissä kehyksen värinen kuten ennen.

Sivusto sanoo kolme asiaa (`6`, `7`, `board`), joten tiloilla oli aluksi **kolme eroa**.
**Yksi niistä poistui 9.9.2026**, ks. suunta alla.

- **Nappuloiden ja kiilojen värit.** `X22`: kerma on kirjautunut pelaaja ja punainen
  vastustaja, eli väri on rooli. `SITE`: värit tulevat skeemasta, eli väri on sivuston väri ja
  rooli luetaan lokeroista ja paneeleista. Roolivärin menetys on tässä tilassa tarkoitus eikä
  regressio, koska juuri se tekee sovelluksesta ja selaimesta rinnastettavia.
- ~~**Suunta.**~~ **Ei enää ero, Tommin päätös 9.9.2026.** Kenttä `7` (`Home boards on left
  side`) peilaa laudan ja siirtää sivupaneelin **molemmissa tiloissa**. Peilaus tehdään
  **asetuksen perusteella**, ei sivun asettelusta päättelemällä, joten jäsennin saa pitää
  normalisointinsa ja piirto saa oman tietonsa omasta lähteestään. `docs/UI.md`:n perustelu
  *"peilaus olisi sen uudelleen päättelemistä minkä jäsennin hävittää"* koskee laudalta
  päättelyä eikä tätä.

  **Miksi kaanoni muuttui.** Aiempi muoto sanoi että `X22`-suunta on kiinteä, ja
  `BoardLookTest` vartioi sitä nimenomaisella testillä. Kuoriproxylla mitattiin 9.9.2026 että
  seuraus oli tämä: asetus luettiin oikein (säilössä luki `home_boards_left = true`) mutta se
  ei tehnyt mitään, koska Tommin oma tyyli on `X22`. Sivupaneelin puoli oli kytketty samaan
  lippuun 8.9.2026, joten myöskään paneeli ei liikkunut. Tommin sanat: *"sivupaneelin
  liikkuvuus oli paha puutos"*. **Kiinteys koskee siis kiiloja ja värejä eli sitä miltä lauta
  näyttää, ei sitä kummalla puolella pelaajan kotikenttä on**, koska jälkimmäinen on pelaajan
  oma asetus sivustolla eikä lautatyylin ominaisuus.
- **Pip-luvun näkyvyys.** Kenttä `6` päällä: sivustolla pip-lukua ei ole. Sovellus ei silti saa
  piilottaa omaa vahtiaan, joten ehdotus on että **vahdin rivi jää ja vain muurin väliin
  piirretty luku noudattaa asetusta**. Muuten sivustouskollisuus ostaisi itselleen sen
  hiljaisuuden joka luvussa 2 kiellettiin.

**Mikä ei vaihdu, ja miksi se on sama vastaus kolmeen kysymykseen.** Geometria pysyy: lokerot,
paneelit, nappirivi, mitat ja kiilojen muoto. Symmetriset lokerot (DG Mobile -havainto
24.8.2026) kuuluvat siis molempiin tiloihin, koska sivusto ei sano lokeroista mitään; kysymys
niistä ei ole tilakysymys vaan asettelukysymys, ja se ratkeaa erikseen. Samasta syystä
nappirivin kaista pysyy sovelluksen omana pintana molemmissa tiloissa, jolloin sitä vasten
lasketut kontrastiluvut kelpaavat sellaisinaan eikä niitä tarvitse laskea uudelleen per tila.

**Taustaväri (`color`) jää ulos, Tommin päätös 24.8.2026.** Sivustouskollisuus koskee lautaa,
ja sovelluksen omat pinnat pysyvät ominaan. Vaihtoehto oli mitattu eikä makuasia: mukaan
otettuna tila saisi kuusi mahdollista taustaa, ja kolme skeemaa kertaa kuusi taustaa on **18
yhdistelmää** joiden kontrastia ei voi tarkistaa etukäteen yksi kerrallaan, vaan se olisi
laskettava ajossa taustan kirkkaudesta. Se oli ainoa avoin kohta joka olisi kasvattanut työn
kokoluokkaa, joten tämä päätös pitää työn väriarvojen vaihtona.

Seuraus jonka pitää olla tiedossa: sovellus ja selain eivät ole taustaväriltään samat, jos
pelaaja on valinnut jonkin muun kuin Classic Whiten. Rinnastettavuus koskee siis lautaa eikä
ruutua kokonaisuutena.

## 5. Kirjoittava asetusnäkymä

Kirjoittavalta ruudulta vaadittiin vastaus neljään kysymykseen ennen kuin nappia lisätään
(uskollisuuden todistus, lähetetäänkö koko lomake, miten epäonnistuminen näytetään, otetaanko
lähtötila talteen). Vastaukset:

**Uskollisuus todistetaan kiertokokeella, ja kiertokoe on jo ajettu oikeaa tiliä vasten.**
`core-net/src/test/kotlin/fi/tommi/dg/net/LiveSettings.kt` sisältää `writeSettings`in ja
`SettingsRiskCaptureLiveTest` sen ympärille rakennetun `applyAndVerify`n, joka lähettää
muutoksen ja **lukee asetussivun uudestaan** ennen kuin jatkaa. Sitä ajettiin 3.8.2026
neljästi peräkkäin, ja `finally`-lohko palautti alkutilan ja vertasi sen kenttä kentältä
lähtötilaan. Kirjoittava toteutus ei siis ole uusi kyky vaan olemassa olevan siirto
testilähteestä tuotantoon.

Siirron ehto: **yksi kopio pysyy yhtenä kopiona.** `LiveSettings` sanoo itse olevansa
tarkoituksella yksi toteutus, ja siirron jälkeen live-testin on kutsuttava tuotantokoodia eikä
säilytettävä omaansa. Kaksi rinnakkaista lähetintä samalle lomakkeelle olisi juuri se riski
jonka `SettingsParser` poisti.

**Lähetetään aina koko lomake.** Osittaista lähetystä ei ole olemassa: rastittamaton ruutu ei
lähde mukana lainkaan, joten palvelin ei erota kohtia *"tätä ei muutettu"* ja *"tämä otettiin
pois"*. `writeSettings` vartioi tämän jo kahdella tarkistuksella, jotka molemmat pysäyttävät
ennen verkkoa: valinnaton radioryhmä ja tuntematon valintaruutu.

**Kolmas vartio on lisättävä, koska sitä ei ole.** Lähetys saa käyttää vain sitä kenttäjoukkoa
joka luettiin, ja lukemisen ja lähettämisen välissä sivu on voinut muuttua. Ehdotus:
lähetykseen liitetään sen sivun kenttänimet joilta ruutu piirrettiin, ja jos lähetystä
edeltävä luku antaa eri joukon, lähetys ei lähde vaan ruutu pyytää päivitystä. Sivuston kentät
ovat paljaita numeroita, joten renumerointi osuisi muuten hiljaa väärään ruutuun, ja koska
lähetys koskee koko lomaketta, yksi väärä ruutu on kaksi väärää asetusta.

**Siirto tehty 25.8.2026, ja vartioita on neljä eikä kolme.** Kokoaminen asuu nyt
`core-domain`issa (`SettingsPage.update`), ja `LiveSettings.writeSettings` on sen kuori.
Sijainti on osa taetta: `FormSubmission`in konstruktori on `core-domain`in sisäinen, joten
asetuslähetyksen voi tuottaa vain tuo funktio, samalla tavalla kuin lautasivun napin voi
tuottaa vain `BoardForm.press`. Vartiot pysäyttävät ennen verkkoa tässä järjestyksessä:
kenttäjoukko muuttunut, radioryhmä ilman valintaa, tuntematon radioryhmä, tuntematon
valintaruutu.

**Neljäs vartio on lisäys tähän designiin eikä sen toteutus.** `UnknownRadioGroups` on
`UnknownToggles`in pari toisessa kenttätyypissä: ilman sitä ryhmänimi jota lomakkeella ei ole
lähtisi mukana ylimääräisenä kenttänä, eikä kutsupaikka saisi tietää osoittaneensa asetukseen
jota ei ole. Vika on samaa lajia kuin ne kolme jotka design nimesi, mutta se on kirjattu
erikseen koska sitä ei pyydetty.

**Järjestys on osa turvaa.** Kenttäjoukon vertailu on ensimmäisenä siksi, että kaikki muut
vartiot mittaavat annettua tilaa lomaketta vasten. Väärää lomaketta vasten mitattu tulos on
oikean näköinen mutta väärä, eli tasan se vikamuoto jota nämä vartiot ovat vastassa.
Vertailu koskee joukkoa eikä järjestystä, koska ryhmien järjestys on sivun järjestys eikä
sopimus.

**Metodi luetaan lomakkeelta samalla kertaa.** `SettingsPage` kantaa nyt lomakkeen oman
`method`in, ja se on POST toisin kuin lautasivun toimintolomakkeet. Itse valittu metodi
lähettäisi kentät paikkaan josta palvelin ei niitä lue, eikä se olisi virhe vaan asetus joka
näyttää tallentuvan muttei tallennu. Sama sääntö kuin lautasivulla 22.8.2026.

**Epäonnistunut lähetys näytetään dialogina, ja ruudun tila jää ennalleen.** Sama muoto kuin
laudalla 24.8.2026 (*epäonnistunut teko säilyttää laudan: dialogi kertoo tilanteen, ei
pollausta*). Kolme lopputulosta joilla on eri teksti: lähetys ei lähtenyt (verkko), lähetys
lähti mutta paluuluku ei täsmää (vaarallisin, ja ainoa jossa käyttäjää pyydetään tarkistamaan
selaimella), istunto vanheni. Vasta täsmäävä paluuluku muuttaa ruudun näyttämän tilan.

**Lähtötila otetaan talteen, ja se on tämän ruudun halvin vakuutus.** Ensimmäinen onnistunut
luku ennen ensimmäistä kirjoitusta säilötään laitteelle sellaisenaan, sen päivän kanssa jona
se luettiin, eikä sitä ylikirjoiteta myöhemmillä luvuilla. Ruudulle tulee toiminto joka
palauttaa sen. Tämä vastaa suoraan siihen lauseeseen jonka vuoksi ruutu oli lukutilassa:
alkuperäistä tilaa ei ollut enää missään. Nyt se on yhdessä paikassa.

**Mimikointi tarkoittaa sivuston omaa rakennetta ja sanastoa.** Ryhmät sivun järjestyksessä,
otsikot sivun `<H4>`-teksteistä, selitteet sivun englannista, lähetysnappi sivun omalla
tekstillä `Update Preferences`. Käännöksiä ei tehdä, koska käyttäjä menee sivustolle etsimään
samaa asetusta samalla nimellä. Muutokset pidetään paikallisina siihen asti että nappia
painetaan, ja napin vieressä sanotaan monta asetusta on muuttumassa, koska yhden ruudun
raksitus lähettää silti kaikki yksitoista kenttää.

**Salasana- ja profiililomakkeisiin ei kosketa.** Sivulla on kolme lomaketta
(`/bg/profile/pw`, `/bg/profile/pub`, `/bg/profile/pref`) eikä niitä erota id tai class.
Jäsennin rajaa jo `action`iin, ja sama rajaus on lähetyksen ainoa sallittu kohde.

## 6. Ratkenneet ja avoimet kohdat

Kaikki viisi kohtaa ratkesivat 24.8.2026. Neljä Tommin kuittauksella ja viides mittauksella,
jonka kohdan D mittaus synnytti.

**A. Taustaväri jää ulos, ratkennut 24.8.2026.** Ks. luku 4.

**B. Haku joka avauksella, ratkennut 24.8.2026.** Ankkuri on tunnistautuminen ja lukon
ollessa pois sama kohta käynnistystä. Tarkennus oli tarpeen, koska tunnistautuminen toistuu
joka käynnistyksellä mutta kirjautuminen tapahtuu kerran tunnusten syöttöä kohti, eivätkä ne
ole saman tahtisia. Ks. luku 2 ja sen kolme ehtoa.

**C. Kaikki yksitoista kenttää ovat muokattavissa, ratkennut 24.8.2026.** Ruutu on sivuston
lomake eikä sen puolikas. Koko lomake lähtee joka tapauksessa, joten rajaus olisi koskenut
vain sitä mitä käyttäjä voi muuttaa, ei sitä mitä lähtee. Kentät `4` ja `5` muuttavat siis
`/bg/nextgamen` käyttäytymistä käyttäjän omalla valinnalla. Tästä seuraa vaatimus ruudulle:
ne kaksi kenttää koskevat sitä kuluttavaa reittiä joka on sovelluksen ykkösominaisuuden ehto,
ja se on sanottava niiden vieressä samalla tavalla kuin `messages_queue_explain` sanoo sen
avausnapin vieressä. Manuaali ei ole näkyvissä sillä hetkellä kun ruutua raksitetaan.

**D. Kenttä `3` mitattiin ennen koodausta, Tommin päätös 24.8.2026.** Mittaus tehtiin samana
päivänä ja se muutti työjärjestystä: `parsePlayers` on korjattava ennen lautatiloja, koska
ilman linkkiä se palauttaa tyhjän listan. Ks. luku 1 ja `docs/KOHDE.md`.

**E. Yhdistelmä mitattu, ratkennut 24.8.2026.** Korjaus tehtiin ensin Tommin päätöksellä ja
mittaus heti perään. Oletus piti: `score:` on paikallaan myös ilman pip-riviä. Ks. luku 1.

## 7. Todennussuunnitelma

Järjestys on sama kuin riskin järjestys, eli kirjoitus todennetaan ennen kuin se on ruudulla
saatavilla.

0. ~~`parsePlayers` korjattuna niin että paneelit löytyvät ilman pelaajanimen linkkiä, ja
   yhdistelmä `3` pois ja `6` pois mitattuna.~~ **Tehty 24.8.2026**,
   `BoardParserPlayerLinksTest` ja `PlayerNameLinksCaptureLiveTest`.
1. ~~`SettingsParserTest` ja uusi lähetyksen kokoamisen testi fixturea vasten, ilman
   verkkoa.~~ **Tehty 25.8.2026**, kaksi testiä eikä yksi: `SettingsUpdateTest`
   (`core-domain`, vartiot käsin rakennetulla sivulla) ja `SettingsUpdateFixtureTest`
   (`core-scrape`, koko ketju kaapatulta sivulta). Jako on tarkoituksellinen: käsin
   rakennettu sivu ei voi paljastaa jäsentimen ja kokoajan välistä eroa, koska se on
   kirjoitettu molempia varten.
2. ~~Kiertokoe live-ajona: luettu tila lähetettynä takaisin tuottaa saman sivun kenttä
   kentältä. Tämä on `applyAndVerify` nykyisessä muodossaan, nyt tuotantokoodia kutsuen.~~
   **Ajettu 25.8.2026**, oma testi `SettingsRoundTripLiveTest`. Ennen ja jälkeen: rastit
   `[0, 1, 3, 4, 5]`, radiot `{order=0, color=0, board=1}`, metodi POST, kenttäjoukko sama.

   **Oma testi eikä riskitestin osa, ja ero koskee riskiä.** `SettingsRiskCaptureLiveTest`
   hakee lisäksi lautasivun neljästi, ja lautasivun lukeminen lukee myös keskustelun.
   Live-testi ei arkistoi mitään, joten se veisi mahdollisen viestin sivustolta eikä
   mihinkään. Kiertokoe ei tarvitse lautaa lainkaan.

   Tämä on ainoa live-testi repossa ilman palautuslohkoa, ja syy on että lähetettävä tila on
   täsmälleen luettu tila: palautettavaa ei synny edes keskeytyksessä.
3. ~~Kenttä `3` mitattuna samalla koejärjestelyllä.~~ **Tehty 24.8.2026**, ja se siirsi
   `parsePlayers`in korjauksen tämän listan ensimmäiseksi kohdaksi.
4. ~~Laiteajo: molemmat lautatilat samasta ottelusta, ja sivustouskollinen tila rinnakkain
   selaimen kanssa. Tämä on se vertailu jota varten koko tila on olemassa, eikä sitä voi
   todentaa ilman Tommin silmiä.~~ **Ajettu 25.8.2026** (SM-T970, ottelu Nine Lives #4293):
   sivustouskollinen tila piirtyi Blue/White-väreillä ja X-22 omillaan samasta asemasta,
   ja Tommi kuittasi näkymät. Peilaus todennettiin yksikkötestein ja fixturesta
   (`move_board_vasen.html`), ei laitteella, koska laitetodennus vaatisi asetusmuutoksen
   Tommin tilillä.

Toteutuksen kotipaikat: `BoardLook` (`app/ui/BoardLook.kt`, tilan ratkaisu ja
skeemavärit), `SiteSettingsStore` ja `SiteSettingsRefresher` (`app/session/`, säilö ja
avaushaku), `BoardStyleStore` (paikallinen valinta), `SettingsPage.siteBoardSettings`
(`core-domain`, kenttien tulkinta selitteistä). Tämä dokumentti jää designin
perustelumuistioksi; koodin ja tämän erotessa koodi ja sen testit voittavat.
