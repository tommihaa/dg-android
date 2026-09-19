# Kohteesta mitattua

Mittauksia DailyGammon-sivustosta. Nämä on todettu kohteesta eikä niitä voi päätellä
koodista. Älä arvaa näitä uudelleen.

Siirretty `CLAUDE.md`:stä 10.8.2026. Sisältö on ennallaan, vain sijainti muuttui.
Hakemisto ja pyytämättä laukeavat portit ovat yhä `CLAUDE.md`:ssä.


## Kohteesta mitattua (älä arvaa näitä uudelleen)

- Sivusto on **pelkkää HTTP:tä**. Portti 443 hylkää yhteyden. Android tarvitsee siis
  `network_security_config`in, jossa selkokielinen liikenne sallitaan vain tälle domainille.
- **Uloskirjautunut pyyntö palauttaa 200 OK ja login-sivun**, ei 302:ta eikä 401:tä.
  Istunnon tilaa ei voi päätellä statuskoodista. Tämä on `DgPages.isLoginPage`in olemassaolon
  syy, eikä sitä saa optimoida pois.
- Kirjautuminen: `POST /bg/login`, kentät `path`, `login`, `password`, `save`.
  `path` on palvelimen kertoma paluukohde, ja se luetaan login-lomakkeen piilokentästä.
  Polkumuunnos `/bg/top` -> `top/` olisi arvaus, älä tee sitä.
- **Rinnakkaiset istunnot samalla tunnuksella ovat sallittuja.** Tommin havainto omasta
  käytöstään 7.8.2026: kirjautumisia on useita yhtä aikaa eikä uusi pudota vanhoja. Lähde
  on siis käyttö eikä sovelluksella tehty mittaus, ja se riittää tähän, koska väite on
  olemassaolo eikä poissaolo.

  Tämä kumoaa yhden hypoteesin: kaksi laitetta **ei** joudu kierteeseen, jossa hiljainen
  uudelleenkirjautuminen potkii toista ulos näkymättömästi. Se ei kumoa mitään muuta.
  `/bg/nextgame` on yhä kuluttava, joten kohde päätyy sille laitteelle joka ehti ensin, ja
  arkistot eroavat pysyvästi. Istuntoraja ja jonon kuluminen ovat eri mekanismeja, ja vain
  edellinen on tässä mitattu.
- **Siirto on sarja pyyntöjä**, ei yksi POST: nappuloita klikataan yksi kerrallaan ja
  välitila elää palvelimella. Siksi `submitForm` ei toista lähetystä automaattisesti.
- **Siirtopyynnön metodi ei ole sivuston vaatimus vaan asiakkaan valinta** (mitattu
  21.8.2026 DG Mobilen liikenteestä, 311 vastausta). DG Mobile lähettää siirrot
  **POSTina siten että parametrit ovat kyselymerkkijonossa ja runko on tyhjä**
  (`POST /bg/move/5312666/1171?move=gg&submit=Submit+Move`), kun taas oma sovelluksemme
  lähettää samat parametrit GETillä. Sivusto hyväksyy molemmat. Jakauma oli 246 POSTia ja
  41 GETiä, ja GETit olivat aina paljaita sivunhakuja ilman kyselyä. Aitoja lomakerunkoja
  oli 35: 34 kappaletta `commit,submit` pelin päättyessä ja yksi `commit,submit,chat`.
  **Seuraus: metodia ei tarvitse muuttaa**, mutta jos siirto joskus epäonnistuu tavalla jota
  ei osaa selittää, tässä on tiedossa toinen muoto joka toimii oikeasti.
- **`/bg/nextgame` ei ole ainoa tapa liikkua otteluiden välillä, eikä toinen asiakas käytä
  sitä lainkaan** (mitattu 21.8.2026). Kokonaisessa DG Mobile -sessiossa oli 27 eri
  ottelutunnusta ja **nolla osumaa `nextgameen`**. Se navigoi suoraan osoitteella
  `/bg/move/<ottelu>/<tila>` ja käyttää Top Pagea listana.

  Tällä on suora vaikutus `CLAUDE.md`:n yhden laitteen rajaukseen, ja vaikutus on kaventava
  eikä kumoava. Rajaus perustellaan sillä että `/bg/nextgame` näyttää kunkin kohteen kerran,
  joten kohde päätyy sen laitteen arkistoon joka ehti avata sen. Perustelu pätee yhä
  selaimeen ja tähän sovellukseen, mutta **DG Mobile ei kilpaile samasta jonosta**, koska se
  ei kuluta sitä. Rinnalla pelattu DG Mobile -sessio ei siis vie kohteita tämän sovelluksen
  jonosta. Se ei tee kahden laitteen käytöstä vaaratonta, koska viestit katoavat lukuhetkellä
  riippumatta siitä millä osoitteella sivu haettiin.
- Sivusto on HTML 4 ilman id- ja class-attribuutteja. Jäsennys on positionaalista ja siksi
  haurasta. Vastalääke on fixture-testit, ei huolellisempi selektori.
- Käyttäjäasetus "Home boards on left side" peilaa koko lautasivun, ei vain suuntaa.
  Tarkemmin lautasivun omassa osiossa alempana.
- Valmiille otteluille on `.mat`-vientilinkki, joka on vakaampi jäsentää kuin lauta-HTML.
  Vienti on **oma reittinsä** `/bg/export/<id>`, ei linkki ottelun sisällä (mitattu
  1.8.2026). Keskeneräisellä ottelulla sitä ei ole lainkaan, joten linkin olemassaolo on
  samalla merkki siitä että ottelu on päättynyt. Runko on pelkkää tekstiä ilman HTML:ää,
  ensimmäinen rivi on ottelun pituus muodossa ` 21 point match` ja sen jälkeen pelit
  siirtoineen (`raakasivut/match_export.mat`, 537 riviä). Sovellus tarkistaa vain tämän
  otsikon ennen jakoa (`MatchExport.read`).

  **Muunnelma ei näy otsikossa, mutta kaksi muunnelmaa näkyy peleissä** (mitattu 16.9.2026
  kahdesta päättyneestä ottelusta Chromella, `core-domain/src/test/resources/`). Nackgammonin
  jokainen peli alkaa valeheitolla `1) 12: Illegal play (7;0;1;0;0;<nimi>;<nimi>;0;0;0;1;0;0;
  -2;-2;0;0;0;4;0;3;0;0;0;-4;4;0;0;0;-3;0;-4;0;0;0;2;2;0;5;1;)`, jonka sulkeissa on sivuston
  oma asemamerkkijono. Luettavissa siitä on 24 pisteen nappulamäärät etumerkillä omistajan
  mukaan (luvut ovat nackgammonin aloitusasema) ja lopussa seuraavan heiton nopat; kumman
  pelaajan suunnasta pisteet lasketaan ja mitä alun kentät ovat, on mittaamatta. Double repeatissa
  (`DRive To Five`) hyväksytty tuplaus toistetaan omana pelinään samasta asemasta, ja
  tiedosto ei merkitse sitä mitenkään: 9 pelin ottelussa pelit 3, 4, 6 ja 8 alkoivat
  keskeltä (`1) 32: 3/0 3/1`). Kummankin ottelun `.mat` alkaa tavallisella ` N point match`
  -otsikolla. Sovellus lukee kummankin `MatGame.setup`iksi eikä kirjoita niistä `.sgf`:ää.

- **Osoitteen ottelunumero ja sivun ottelunumero voivat olla eri.** Mitattu 9.9.2026
  pelisessiossa `sessio-9-9-yo4`: pyyntö `/bg/move/5305894/2493` palautti sivun jonka
  otsikko on `DailyGammon Match 5322721` ja pituusrivi `1 Point Match`, kun saman ottelun
  edelliset kuusi sivua sanoivat `5305894` ja `21 Point Match`. Ottelu oli päättynyt, ja
  sivusto tarjoili seuraavan ottelun samalla osoitteella.

  Tästä seuraa että **ottelun tunniste luetaan sivun otsikosta eikä osoitteesta**, ja niin
  `BoardParser` jo tekee: otsikko ensin, osoite vasta varalta. Sovellus menee tätä pidemmälle
  eikä vain lue oikein: se **kieltäytyy näyttämästä** laudan jos otsikon numero on eri kuin
  avatun ottelun, ja sanoo sen ruudulla (*"DailyGammon returned a different match (5322721)
  than the one you opened, so nothing is shown."*). Todennettu kuoriproxylla 9.9.2026, ja se
  on samalla syy siihen että kuoritestaus vaatii sivun jonka otsikko vastaa avattua ottelua. Tallennetun sivun nimi tulee
  pyynnön osoitteesta, joten `raakasivut/`-tiedostonimi voi luvata eri ottelun kuin sivu
  sisältää. Se ei ole tallennusvirhe vaan tämän saman ilmiön jälki.

Top Pagesta erikseen (kaapattu ja jäsennetty 29.7.2026, 25 ottelua):

- **Vuoro ei ole rivillä vaan taulukon captionissa:** `Matches where you can move:`.
  Ryhmä ratkaisee, ei rivi. Huom: `contains("can move")` osuu myös sanaan `cannot move`,
  ja `TopPageParser` varoo sitä erikseen.
- **Otsikkorivillä on 7 saraketta, datariveillä 9.** Review- ja Play-linkeillä ei ole
  otsikkoa, joten sarakkeita ei voi laskea otsikoista.
- Rivin kiintopisteet ovat linkkien muodot: `/bg/event/<id>`, `/bg/user/<id>`,
  `/bg/game/<id>/<n>/list`, `/bg/move/<id>/<n>`. Pelitunniste on sama molemmissa
  jälkimmäisissä. Aika- ja kierrossarakkeissa ei ole linkkiä, joten ne luetaan indeksillä
  **suhteessa tapahtumasarakkeeseen**, ei rivin alkuun.
- **Oma käyttäjänumero on samassa muodossa kuin vastustajien** (`/bg/user/<id>`) ja erottuu
  vain kyselyparametrista `days_to_view`. Ilman erottelua ensimmäinen vastustaja luultaisiin
  käyttäjäksi itsekseen.
- `You have Messages!` (`/bg/nextgame`) **ei ole todiste henkilöviestistä eikä sitä vastaan.**
  Ilmoitus tulee myös esimerkiksi turnauksen voitosta (Tommin havainto 29.7.2026), ja se voi
  yhtä lailla koskea henkilöviestiä (Tommin tarkennus 15.8.2026). Lippu kertoo vain että
  osoitteessa on jotain, ja laji selviää vasta sisällön haettua. Aiempi muotoilu *"ei tarkoita
  henkilöviestiä"* sanoi enemmän kuin tarkoitti, ja se korjattiin sanamuotona: jatkolause on
  ollut alusta asti oikein, mutta lihavoitu alku luettiin poissulkevana. Siksi `MessageSource`ssa on
  oma `ANNOUNCEMENT`: jos tiedotteet menisivät samaan säiliöön henkilöviestien kanssa,
  arkisto täyttyisi niistä ja se historia jota varten sovellus tehdään hukkuisi joukkoon.
- Aikakentät (`0:00`, `278:29`) ovat mallissa **tekstiä**. Tunnit ja minuutit on uskottava
  tulkinta muttei luettavissa sivulta, eikä arvausta piiloteta tyyppiin.
- **Top Page näyttää vain siirrettävät ottelut** (mitattu 1.8.2026). Kun niitä ei ole
  yhtään, sivulla ei ole taulukkoa lainkaan vaan lause `There are no matches where you can
  move.` ja linkki omalle profiilille. Odottavat ottelut eivät siis ole toisessa
  taulukossa vaan **eri sivulla**, ja aiempi oletus toisesta captionista oli väärä.
  Seuraus: tyhjä `TopPage.matches` on kelvollinen tulos eikä jäsennysvirhe, ja
  käyttöliittymän on erotettava "ei siirrettävää" tyhjästä hausta.
- **Tyhjä sivu saapuu myös lautaosoitteesta** (kaapattu 3.8.2026, fixture
  `top_page_no_matches.html`). Sama sisältö palautui osoitteesta `/bg/move/<id>/<tila>`
  eikä `/bg/top`:sta, kun viimeinenkin siirto oli tehty. **Sivun laji ei siis ole
  pääteltävissä pyydetystä osoitteesta**, mikä on sama havainto kuin `isLoginPage`in
  taustalla ja koskee myös lautaa: `/bg/move/`-pyyntö voi palauttaa jotain muuta kuin
  laudan, 200 OK:lla. Sivu jäsentyy oikein, ja `userName` sekä `userId` ovat siltä yhä
  luettavissa, joten sitä ei voi eikä pidä ohittaa kokonaan.
- Tyhjällä sivulla on linkki `/bg/userevent/<id>` (active tournaments), jota Top Pagen
  taulukoissa ei ole. Sama linkki on jokaisen Top Pagen alalaidassa, ja 3.9.2026 alkaen
  `TopPage.activeTournamentsPath` lukee sen Loungen Tournaments-segmenttiä varten.
- **Oma profiili on se sivu jolla ottelut oikeasti ovat.** Polku luetaan Top Pagen
  linkistä (`/bg/user/<id>?days_to_view=30&active=1&finished=1`), ja sillä on kaksi
  taulukkoa: `Active games:` ja `Finished matches:`. Molemmilla on Review-linkki
  `/bg/game/<id>/<n>/list`, jälkimmäisellä lisäksi Export. Polku on `TopPage.profilePath`,
  ja se otetaan linkistä sellaisenaan: kyselyparametrit ovat palvelimen omat, joten
  osoitteen kokoaminen käyttäjänumerosta olisi sama arvaus kuin `/bg/top` -> `top/`.

Profiilisivusta erikseen (jäsennetty 3.8.2026, `ProfileParser`, fixture `profile_page.html`):

- **Taulukoilla ei ole samat sarakkeet.** `Active games:` on
  `# · Event · Grace · Time Pool · Round · Length · Opponent`, ja `Finished matches:`
  jättää **Gracen ja Time Poolin kokonaan pois**. `TopPageParser` ei siis kelpaa tälle
  sivulle: sen tapa laskea indeksi tapahtumasarakkeesta eteenpäin lukisi päättyneestä
  ottelusta kierroksen graceksi ja pituuden time pooliksi, hiljaa ja oikean näköisesti.
  Sen `caption.contains("match")` osuu lisäksi vain `Finished matches:`iin, eli juuri
  siihen taulukkoon joka menisi väärin, ja jättäisi kesken olevat lukematta.
- **Sarakkeet luetaan otsikkorivin nimistä, ei paikoista.** Se on mahdollista täällä muttei
  Top Pagella: siellä otsikoita on 7 ja sarakkeita 9, kun taas profiilissa otsikottomat
  Review ja Export ovat rivin lopussa, joten alkupää on kohdakkain. Sivutuotteena tämä
  ratkaisee linkittömän tapahtuman (kaapatussa sivussa yksi ottelu, jonka tapahtumasarake
  on pelkkä teksti `double-repeat`), jota Top Pagen puolella arvataan indeksillä.
- **Sivu ei kerro vuoroa kummassakaan taulukossa**, eikä sillä ole `/bg/move/`-linkkejä
  lainkaan. Siksi `Match.myTurn` on nyt `Boolean?` ja profiilirivit saavat sen nulliksi.
  `false` olisi arvaus, ja se olisi väärä juuri niissä otteluissa joita käyttäjä eniten
  etsii. Käyttöliittymä jättää tunnuksen näyttämättä kun arvo on tuntematon.
- **Profiilin kohteen numero luetaan `/bg/userevent/`- ja `/bg/userwins/`-linkeistä**, ei
  muodosta `/bg/user/<id>`: vastustajilla on sivulla samat linkit, ja sivun omat
  lajittelulinkit (`?sort_name=1`) ovat samassa muodossa kuin ne.
- Kentät ovat `playerName` ja `playerId` eivätkä `userName`/`userId`, koska **sivu on jonkun
  pelaajan profiili eikä välttämättä kirjautuneen.** Osoitteen voi rakentaa kenelle tahansa,
  eikä sivu itse kerro kumpi tapaus on kyseessä.
- `Match.exportPath` täyttyy vain päättyneillä otteluilla, mikä on sama havainto kuin
  vientilinkin olemassaolo lopetusmerkkinä, nyt mallissa.
- `DgPages.isProfilePage` tunnistaa sivun **otsikosta** (`Info on player`), koska captionit
  katoaisivat profiililta jolla ei ole yhtään ottelua. Tarve on sama kuin `isLoginPage`illa:
  laji ei ole pääteltävissä pyydetystä osoitteesta.
- **Rating ja Experience ovat profiilisivulla omassa kahden rivin taulukossaan**, oikeassa
  ylälaidassa ennen otteluluetteloita ja ennen linkkejä `active tournaments` ja
  `tournament wins`. Rakenne on `<TD align=right>Rating</TD><TD align=right><FONT
  size=+1>arvo</FONT></TD>`, eli selite ja arvo ovat eri soluissa kuten radiovalinnoilla
  asetussivulla. Rating on desimaaliluku, Experience kokonaisluku. `ProfileParser` ei lue
  kumpaakaan, ja se on kirjattu tänne siksi ettei sijaintia tarvitse etsiä uudestaan siinä
  vaiheessa jos ne joskus luetaan.

  **Mikä luku tämä on, koska luvun viereen kuuluu se mitä se laskee.** Rating mittaa vain
  sivuston omista turnausotteluista kertyvää tulosta eikä kata yksityisotteluita
  (`SUBSTANSSI.md` kohdat 14 ja 97), ja Experience on siitä riippumaton: pelaajalistalla
  (`/bg/plist`) on korkean ratingin pelaajia joilla on vähän kokemusta ja päinvastoin.
  Vahvuusarvio ei siis ole tämä luku yksin, vaan kahden vihjeen yhdistelmä jonka toinen puoli
  on turnausvoittojen lista (kohta 54). Sillä on suora seuraus jos luku joskus näytetään:
  yksin esitettynä se väittäisi enemmän kuin sivusto väittää.

  **Luku elää, ja se on mitattu kahdesta hetkestä.** Tunnuksella `tommih` rating oli
  `2223.26` ja Experience `12818` 1.8.2026 (`user_profile.html`), ja `2219.9` sekä `12944`
  24.8.2026 (Tommin kuvakaappaus omalta profiililta). Kolmessa viikossa rating siis laski
  3,4 pistettä ja kokemus kasvoi 126:lla. **Desimaalien määrä ei ole vakio:** `2223.26` on
  kaksidesimaalinen ja `2219.9` yksi, joten jäsennin ei saa olettaa muotoa vaan lukea luvun.

  **1500.00 on DailyGammonin aloitusrating, ei paikanpitäjä**, ja se tekee fixturesta
  ansan eikä vain epätarkan. `profile_page.html` sanoo `1500.00` ja `1000`, ja anonymisointi
  valitsi juuri ne. Aloitusarvo on mitattu: Tommin toisella tunnuksella `ackammon` (luotu
  22.8.2026) rating on `1500` (Tommin ilmoitus 24.8.2026, ei nähty tästä sivulta). Fixturen
  luku on siis **kelvollinen todellinen arvo** eikä mikään sivulla erota sitä mittaustuloksesta,
  mikä on huonompi tilanne kuin ilmeisen keksitty luku.

  **Se ehti jo vuotaa kanoniin.** `docs/PELAAJA.md` sanoi 24.8.2026 asti Tommin ratingiksi
  noin 1500, eli aloitusarvo luettiin hänen luvukseen. Korjattu samana päivänä
  kuvakaappauksen perusteella. Anonymisoitu fixture on johdettu kopio, ja tässä se puuttui
  takaisin lähteeseen.

  **Ansa poistettiin eikä vain dokumentoitu.** Fixturen arvot ovat 24.8.2026 alkaen
  `1234.56` ja `1234`, jotka eivät ole aloitusarvo eivätkä pyöreitä. Muutos on riskitön,
  koska yksikään testi ei nojaa lukuihin eikä `ProfileParser` lue niitä; jos se joskus lukee,
  arvo joka ei voi olla kenenkään oikea rating on parempi odote kuin arvo joka voi.
  **Sääntö tästä eteenpäin: anonymisoitu arvo ei saa olla kelvollinen todellinen arvo
  silloin kun kelvollisuus on tunnistettavissa.** Pelaajanimet (`pelaaja`, `vastapelaaja`)
  ja käyttäjänumerot (`90001`, `91002`) noudattavat tätä jo, rating ei noudattanut.

- **Profiilisivulla on kolme lomaketta, ja kaksi niistä on toimintoja joita sovellus ei
  tunne.** Mitattu 22.8.2026 raaoista tavuista sivulta `/bg/user/<id>`, jäsennin lukee
  näistä nolla. Lueteltuna siksi, ettei niitä tarvitse mitata uudelleen jos ne joskus
  tulevat ajankohtaisiksi:

  | Lomake | Osoite ja metodi | Kentät |
  |---|---|---|
  | Ottelukutsu | `POST /bg/invite/new` | `player` (piilotettu), `variant`, `length`, `comment` (max 80), `name` (max 40), `time_control`, `private` (valintaruutu, arvo `private`) |
  | Pikaviesti | `POST /bg/sendmsg/<id>` | `text` (max 80), ks. viestiosio |
  | Sivuutus | `GET /bg/ignore` | `changeto`, `user` |

  Kaksi huomiota. Sivuutus on **GET eikä POST**, eli se on muuttava toiminto tavallisen
  linkin muodossa, ja `changeto` viittaa siihen että sama osoite sekä asettaa että
  poistaa.

- **Luovutussivu `/bg/resign`, mitattu 3.9.2026 kaappauksesta
  `raakasivut/sessio-3-9-resign/0006_bg_resign.html`** (Tommi avasi sen proxy-Chromella
  lähettämättä mitään). `<TITLE>` on `DailyGammon: Resignation Page`, ei `<h1>`-otsikkoa.
  Sivulla on **yksi lomake `POST /bg/resign/doit`**, jonka sisällä on taulukko
  `<CAPTION>Unfinished matches:</CAPTION>` samoin sarakkein kuin profiilin ottelutaulukossa
  (`#`, `Event`, `Grace`, `Time Pool`, `Round`, `Length`, `Opponent`, sitten `Review`-linkki
  `/bg/game/<id>/<n>/list`) ja rivin viimeisenä soluna **`<INPUT TYPE=CHECKBOX NAME=<ottelun
  tunnus>>` ilman arvoa**. Ystävyysottelun rivi on samaa muotoa kuin muuallakin (tapahtuma
  tekstinä, ajat viivoina). Taulukon alla `<INPUT TYPE=CHECKBOX NAME=reellysure> Yes! I
  really want to resign.` ja `<INPUT TYPE=SUBMIT VALUE="Resign Selected Matches">`.
  Kaappauksessa oli 38 riviä, eli **kaikki keskeneräiset ottelut eikä vain vuorossa
  olevat**; siksi lista kelpaa luovutuksen todisteeksi (kadonnut rivi). Kirjautumattomana
  sama osoite antoi kirjautumissivun (`0003_bg_resign.html`).

  ~~Mittaamatta: `doit`-vastauksen muoto~~, ja onko sivun haku kuluttava jonon kannalta.
  **`doit`-vastaus mitattiin 4.9.2026 illalla** (`sessio-4-9-ilta2/0012`, Tommin oma
  luovutus), ja se on **oma sivunsa eikä luovutussivu**: `<TITLE>DailyGammon: Post
  Resignation</TITLE>`, runko `<BR>1 match resigned.` ja `<BR>Back to <A HREF=/bg/resign>`.
  Lomaketta ei ole. Kaksi seurausta. Sivusto **kertoo luvun omin sanoin**, kun sovellus
  laskee sen kadonneista riveistä; **sivuston lukema otettiin käyttöön samana iltana**, ja
  luku luetaan lauseen ensimmäisestä kokonaisluvusta, koska monikkomuoto on näkemättä. Ja
  `ResignParser.parse` ei lue tätä sivua, joten koodin
  varareitti (hae `/bg/resign` uudelleen) on se joka aina ajetaan; lokissa POST ja heti
  perässä GET. Fixture `resign_done.html`.

  Toinen puoli jäi auki, ja alkuperäinen kirjaus jatkuu tästä. Sivu on
  luettelo ilman `nextgame`-ohjausta, ja Tommin kaksi hakua eivät muuttaneet mitään näkyvää,
  mutta se on havainto kahdesta hausta eikä mittaus. Kirjattu `SUBSTANSSI.md` kohdan 36
  vaatimuksen täyttämiseksi ennen kuin sovellus koskee sivuun.

- **`Private Match` -valintaruudulla ei ole löydettyä vaikutusta kutsun sanamuodon
  ulkopuolella** (mitattu 22.8.2026 neljältä pinnalta, kolme hypoteesia kuoli). Ainoa mitattu
  vaikutus on että sana `private` ilmestyy kutsun lauseeseen: ruudullinen kutsu lukee
  vastaanottajalla `a private 5 point match of backgammon with no time control .` ja ruuduton
  `a 3 point match ...`. Muuten sivut ovat samanlaiset. Mitä ruutu **ei** tee:
  - *Ei vaikuta ratingiin.* Sivuston Help sanoo suoraan `Nackgammon and Backgammon tournament
    matches are included in the same ratings. Non-tournament games are not included in
    ratings.`, eli ratingin koskemattomuus seuraa turnauksettomuudesta eikä ruudusta
    (`SUBSTANSSI.md` kohta 97). Help ei tunne ruutua lainkaan; sen oma päiväys on
    `Updated November 2010`, joten vaikeneminen voi olla myös ikää.
  - *Ei ohjaa lounge-näkyvyyttä.* Kohdennettu kutsu puuttuu `Game Lounge` -sivulta
    **molemmilla arvoilla**. Lounge on avoimen ilmoituksen paikka, eikä se listaa käynnissä
    olevia otteluita lainkaan.
  - *Ei ohjaa julkisen profiilin näkyvyyttä.* Mitattu kahdella riippumattomalla tavalla:
    saman ottelun ennen–jälkeen -vertailulla (vastatarjous käänsi ottelun yksityiseksi,
    rivi jäi paikalleen `Review`-linkkeineen) ja kolmen rinnakkaisen ottelun sisäisellä
    vertailulla samojen tunnusten välillä (kaksi yksityistä, yksi ei; kaikki kolme näkyvät).

  Vastatarjouslomake **perii kutsun arvon** eikä käytä omaa oletusta, mitattu samalla.
  Ainoa koskematon selitys on loungen **avoin** ilmoitus, jossa vastaanottajaa ei nimetä: sen
  testaaminen vaatisi oikean ottelun tuntemattoman kanssa, eli isomman hinnan kuin neljä
  edellistä koetta yhteensä. Kiirettä ei ole, koska sovellus ei tunne ratingia (`rating` ei
  esiinny lähdekoodissa) eikä lähetä kutsuja.

  **Viivat eivät ole aloittamattomuuden merkki:** Grace, Time Pool ja Round ovat `-` myös
  hyväksymisen jälkeen, kun ottelussa ei ole aikarajaa eikä turnausta. Ne kertovat ottelun
  lajista eivätkä sen vaiheesta.

- **Ei tehty:** `:app` ei vielä hae profiilia. Jäsennin on olemassa ja Top Page tarjoaa
  polun, mutta se että tyhjä Top Page johtaisi automaattisesti profiilihakuun on
  käyttöliittymäpäätös eikä jäsennystä.
- **Ei nähty vielä:** sekamuoto, jossa osa otteluista on siirrettävissä ja osa ei. Vasta se
  kertoo onko Top Pagella silloin yksi taulukko vai kaksi.

Asetussivusta erikseen (`/bg/profile`, kaapattu 1.8.2026, jäsennetty 3.8.2026,
`SettingsParser`, fixture `settings_page.html`):

- **Tämä on ensimmäinen jäsennetty sivu jota myös kirjoitetaan**, ja se muuttaa virheen
  luonteen. Muualla väärin luettu sivu näyttää väärää tietoa ja korjaus on uusi haku. Täällä
  väärin luettu sivu **tuhoaa asetukset seuraavassa lähetyksessä**, eikä alkuperäistä tilaa
  ole enää missään. Siksi malli kantaa koko lomakkeen eikä sitä kenttää jota muutetaan.
- **Rastittamaton valintaruutu ei lähde lomakkeessa mukana lainkaan.** Palvelin ei siis
  erota "tätä ei muutettu" ja "tämä otettiin pois" toisistaan, ja kentän puuttuminen luetusta
  tilasta on sama asia kuin asetuksen nollaaminen. Tämä oli tiedossa jo 1.8.
  (`LeftBoardCaptureLiveTest`), mutta luku asui live-testissä, eli ainoana jäsennyksenä koko
  projektissa jota mikään offline-testi ei kattanut.
- **Sama vika koskee radioryhmiä, ja se oli nimeämättä.** Ryhmä jolla ei ole valintaa on
  erottamaton ryhmästä jota ei ole, jos luetaan vain `input[type=radio][checked]`. Silloin
  ryhmä katoaa lähetyksestä hiljaa. `SettingsPage.radioGroupNames` on olemassa vain tämän
  vertailukohdaksi, ja lähettäjä pysähtyy jos ryhmä puuttuu valinnoista.
- **Lomakkeita on kolme** (`/bg/profile/pw`, `/bg/profile/pub`, `/bg/profile/pref`) eikä
  sivulla ole id- eikä class-attribuutteja. Rajaamaton luku osuisi ensimmäiseen, joka on
  salasananvaihto.
- **Kentän nimi ei kerro asetusta.** Valintaruudut ovat `NAME=0` ... `NAME=7`, ja ainoa
  merkitystä kantava tieto on ruudun vieressä oleva teksti. Selite luetaan solun tekstinä
  eikä tekstisolmuina, koska yksi niistä sisältää elementin
  (`Skip <em>all</em> automatic pages`). Kaapattu numerointi oli 0 tuplauksen tarjoaminen,
  1 hyväksyminen, 2 hylkääminen, 3 nimilinkit, 4 vastustajan Roll Dice -sivujen ohitus,
  5 kaikkien automaattisivujen ohitus, 6 pip-lukujen piilotus, 7 kotialue vasemmalla.
  **Se on kaapatun sivun tila, ei taattu järjestys**, joten koodi hakee ruudun selitteestä ja
  ristiintarkistaa numeroon: kumpi tahansa muutos tekee testistä punaisen sen sijaan että
  osuttaisiin hiljaa väärään ruutuun.
- `DgPages.isSettingsPage` tunnistaa sivun **lomakkeesta** eikä otsikosta, toisin kuin
  `isProfilePage`. Syy on eri suuntaan: otsikossa on käyttäjänimi, ja kysymys koskee
  oikeasti lomakkeen olemassaoloa, koska sitä käyttökohde tarvitsee.
- **Nimeämisero jota suomi hämärtää:** `/bg/profile` on kirjautuneen **omat asetukset** ilman
  käyttäjänumeroa, kun taas `/bg/user/<id>` on **jonkun pelaajan julkinen profiili**. Molempia
  on kutsuttu profiiliksi, mutta ne ovat eri sivuja eri jäsentimillä.

Asetuksia jotka koskevat suoraan jo kirjoitettua lautajäsennintä on **neljä eikä kolme**.
Kolme ensimmäistä kirjattiin hypoteeseina 3.8.2026, ja **kaksi niistä mitattiin samana
päivänä** Tommin pyynnöstä (`SettingsRiskCaptureLiveTest`, neljä otosta samasta ottelusta ja
samasta osoitteesta, asetukset palautettu ja palautus todennettu). Fixturet
`move_board_mini.html` ja `move_board_no_pips.html`, testit `BoardParserSchemeTest`.

**Neljäs löytyi 24.8.2026 asetusdesignia kirjoitettaessa** (`docs/ASETUKSET.md`), ja se on
alla omana kohtanaan. Sitä ei ollut listalla lainkaan, eli listan pituus oli itsessään
väärä tieto.

- **`Hide pip counts` vie vahdin muttei jäsennystä. Mitattu, hypoteesi piti paikkansa.**
  Sivu jäsentyy täysin normaalisti ja lauta on tallessa, mutta molempien paneelien `pips`
  on null. `BoardParserTest` vertaa laudasta laskettua summaa juuri siihen lukuun, joten
  vahti ei kaadu vaan **muuttuu tyhjäksi lupaukseksi**. Tämä on koko projektin ainoa kohta
  jossa sivun kaksi riippumatonta osaa todistavat toisensa, ja asetus poistaa toisen niistä.
- **`Board Scheme` vaihtaa hakemiston, ei tiedostonimiä. Mitattu, ja hypoteesi oli väärä.**
  Skeema on polun numero: Classic `/images/1/`, Blue/White `/images/2/` (Tommin nykyinen),
  Mini `/images/3/`. Tiedostonimet ovat kaikissa kolmessa identtiset, joten `img[src*=pt_]`
  ja noppien värin luku nimestä (`die_b5.gif`) ovat **turvassa**, toisin kuin arveltiin.
- **Mini rikkoo laudan silti, ja eri syystä kuin arvattiin.** Se pudottaa **pistenumerorivin
  kokonaan**, ja koska jäsennin lukee numeron sivulta eikä päättele sitä suunnasta (sääntö
  1.8.2026), yhtäkään pistettä ei voi numeroida. Nappulat ovat sivulla tallessa: 24
  pistekuvaa ALT-koodeineen, nopat, paneelit ja tilatunniste. **Vika on hiljainen:**
  `BoardParser.parse` ei palauta nullia vaan laudan jonka `points` on tyhjä, mikä on
  erottamaton laudasta jolla ei ole yhtään nappulaa. Mini pudottaa myös nuolet, eli tieto
  edellisestä siirrosta katoaa.

  **Hiljaisuus poistui 13.8.2026, ja korjaus tuli tästä samasta mittauksesta.** Skeema on
  hakemisto, eli se on luettavissa sivulta, joten tyhjän listan ei tarvitse todistaa syytä.
  `BoardState.scheme` kantaa sen, ja ruutu erottaa nyt Minin tuntemattomasta syystä.
  Sopimusta ei muutettu, ja `parse` palauttaa yhä laudan.
- **`Player Name links on game page` pudottaa molemmat pelaajapaneelit. Mitattu 24.8.2026
  Tommin pyynnöstä, ja ennuste piti paikkansa.** `PlayerNameLinksCaptureLiveTest`, kaksi
  otosta samasta ottelusta ja samasta osoitteesta, asetus palautettu ja palautus todennettu.
  Linkit päällä: 2 käyttäjälinkkiä, 2 paneelia, nimet `[bazari, tommih]`, pipit `[155, 164]`,
  pisteet `[7, 1]`. Linkit pois: 0 käyttäjälinkkiä, **0 paneelia**, kaikki kolme tyhjinä.
  Lauta itse on molemmissa sama (11 miehitettyä pistettä, 2 noppaa), ja koko ero sivujen
  välillä on neljä riviä: kaksi `<A HREF=/bg/user/...>`-avausta ja niiden sulut.

  Syy on `parsePlayers`in valitsimessa `td:has(a[href*=/bg/user/])`: linkki on paneelin
  **ainoa** tuntomerkki, eikä yksikään fixture ole sivulta jolla asetus olisi ollut pois.
  Solu itse on tallessa ja sisältää yhä nimen (`<B>bazari</B>`), pip-luvun ja pisteet, eli
  tieto ei katoa sivulta vaan jäsentimeltä.

  **Vika on hiljainen ja se vie vahdin, eli sama laji kuin `Hide pip counts`.**
  `BoardParser.parse` palauttaa laudan (`jäsentyi=true`), joten mikään ei kerro puutteesta;
  ruudulta katoavat nimet, pisteet ja pip-luvut, pip-vahdilla ei ole mitään mihin verrata, ja
  koska rooli tunnetaan vain vahdin kautta, myös nappuloiden roolivärit menevät varasuunnalle.
  Yhdellä asetuksella siis kaatuu enemmän kuin yhdelläkään aiemmin mitatulla.

  **Käyttäjänumero katoaa oikeasti, ja se on ainoa osa jota ei voi palauttaa.** Nimi, pipit
  ja pisteet ovat solun tekstissä, mutta `/bg/user/<id>` on vain linkissä. Korjattu jäsennin
  voi siis lukea kaiken muun mutta `PlayerPanel.userId` jää tässä tilassa nulliksi.

  **Korjattu samana päivänä.** `parsePlayers` tunnistaa paneelin nyt myös pistekentästä
  (`td:matchesOwn(score:)`) ja lukee nimen solun ensimmäisestä `<b>`:stä kun linkkiä ei ole.
  Fixture `move_board_no_name_links.html`, testi `BoardParserPlayerLinksTest`.

  **Yhdistelmä mitattiin samana päivänä kolmantena otoksena.** Linkit pois ja `Hide pip
  counts` päällä: 0 käyttäjälinkkiä, 2 pistekenttäsolua, 2 paneelia, nimet `[bazari, tommih]`
  ja pisteet `[7, 1]` tallessa, pipit `[null, null]`. Pistekenttä on siis paikallaan myös
  silloin kun pip-rivi ei ole, eli korjattu tunnistus ei jää yhden oletuksen varaan. Fixture
  `move_board_no_links_no_pips.html`. Sama ajo todensi korjauksen livenä: paneeleja oli 2
  siinä tapauksessa jossa niitä oli ennen korjausta 0.

- **Mini on sivuston oma kapean näytön lauta.** Se on ainoa skeema jolla on
  `<meta name=viewport content="width=320">`. Tämä ei ole jäsennysasia vaan löytö: sivustolla
  on jo puhelimelle tarkoitettu lautanäkymä, ja se on juuri se joka on koneelle luettavissa
  huonoiten.
**Asetusten nykyarvot luettiin kuvakaappauksesta 22.8.2026** (Tommin oma asetussivu). Ne ovat
tilannekuva eivätkä sivuston oletuksia, ja ne päivätään siksi tähän. Sivun henkilötiedot
(nimi, sähköposti) jätetään kirjaamatta, koska ne eivät ole jäsentimen eivätkä sovelluksen
asia.

| Valintaruutu | Tommilla |
|---|---|
| Confirmation on offering doubles | päällä |
| Confirmation on accepting doubles | päällä |
| Confirmation on declining doubles | **pois** |
| Player Name links on game page | päällä |
| Skip Opponent's `Roll Dice` pages | päällä |
| Skip *all* automatic pages | päällä |
| Hide pip counts | pois |
| Home boards on left side | pois |

Kolme ensimmäistä ovat tämän taulukon tärkein rivi, ja ne selittävät `verify`-kentän:
**vahvistus on pelaajakohtainen asetus eikä sivuston kiinteä ominaisuus.** Ne ovat myös
keskenään eri arvoisia, eli sama pelaaja haluaa vahvistuksen kahteen tekoon kolmesta.
Rajaus: **ei ole mitattu**, katoaako `verify`-valintaruutu sivulta kun asetus on pois. Se
olisi mitattavissa vain muuttamalla Tommin asetusta, joten se jää auki.

**`Next` Match Ordering on valittavissa, ja se sulkee `SUBSTANSSI.md`:n kohdan 21 nimeämän
mittauksen osittain.** Vaihtoehtoja on neljä (`Grace then Pool`, `Pool`,
`Grace+Pool Total`, `Recent Opponent Move`), ja Tommilla on ensimmäinen. Järjestys ei siis
ole sivuston kiinteä ominaisuus vaan pelaajan valinta, ja tarkka sääntö on
**grace ensin, sitten pool** eikä pelkkä grace.

*Mikä jää auki, ja tämä on syytä lukea tarkkaan:* asetuksen otsikko on `"Next" Match
Ordering`, eli se puhuu `Next`-toiminnosta. Kuvakaappaus **ei todista** että sama valinta
ohjaisi myös `/bg/top`-sivun listausta, vaikka Tommin kuvaus listan järjestyksestä on sen
kanssa yhteensopiva. Kohdan 21 kielto olla koskematta sivuston antamaan järjestykseen ei
riipu tästä kummallakaan tavalla.

**Taustaväri on `Classic White` ja skeema `Blue/White`**, eli sama skeema joka on yllä
mitattu hakemistoksi `/images/2/`.

- **`Skip all automatic pages` jäi mittaamatta, eikä se ole unohdus.** Asetus vaikuttaa siihen
  mitä `/bg/nextgame` tarjoilee, ja se osoite kuluttaa jonoa. Riskiä ei siis voi mitata
  rikkomatta sääntöä joka on koko sovelluksen olemassaolon syy. Se on Tommilla päällä, ja
  arvaus siitä että se selittäisi välitilojen vaikean kiinnisaamisen on yhä arvaus.

Viesteistä ja siirroista (Tommin havainnot ja kuvakaappaukset 29.7.2026):

- **Haku on tuhoava operaatio.** Viesti katoaa kun sen on kerran nähnyt, ja `/bg/nextgame`
  on jono joka kuluttaa itseään: se näyttää yhden kohteen ja `Next >>` siirtää seuraavaan.
  Siitä seuraa suoraan **kova sääntö: sovellus ei saa hakea viestejä taustalla, ei
  ajastetusti eikä ennakoivasti.** Taustahaku söisi jonosta kohteen, ja jos sovellus on
  sillä hetkellä offline tai kaatuu, viesti on mennyt lopullisesti eikä kukaan näe sitä
  koskaan. Kuluttava pyyntö tehdään vain käyttäjän pyynnöstä, yksi kerrallaan, ja
  **viesti kirjoitetaan kantaan ennen kuin se näytetään ruudulla.**

  Sääntö on 9.8.2026 alkaen koodattuna eikä pelkkä kirjaus: `MessagesViewModel` kantaa sen
  neljänä ehtona, joilla kullakin on oma testinsä. Ks. viestiruudun osio `:app`-osuudessa.

  Sama havainto hautaa palvelinpuolisen arkiston ajastetulla kyselyllä: se olisi tehokas
  tapa hävittää juuri se historia jota se yrittää säilyttää.
- `/bg/nextgame` tarjoilee sekä ilmoituksia että pelilautoja samasta osoitteesta.
- **Lajeja on kolme eikä kaksi: myös ottelukutsu saapuu `/bg/nextgame`stä (22.8.2026).**
  Mitattu Tommin kahdesta kuvakaappauksesta, joissa toiselta omalta tunnukselta lähetetty
  kutsu näkyy vastaanottajalla. Sivulla ei ole lautaa vaan lause muotoa `<nimi> invites you
  to play a 3 point match of backgammon with no time control .`, nappi `Accept Invitation`,
  `Comment`-kenttä ja `Decline Invitation`, sekä vastatarjouslomake (`variant`, `length`,
  `comment`, `name`, `time_control`, `private`) nappinaan `Counter Offer`. Vastatarjouksen
  `private` perii kutsun oman arvon, eli lomake näyttää mitä vastataan eikä oletusta.

  **Kutsulla on oma osoite `/bg/invite/<id>`** (mitattu `490350`), ja **kutsu ei häviä jos
  siihen jättää vastaamatta** (Tommin havainto 22.8.2026). Se on eri lajia kuin viesti, joka
  katoaa kun sen on kerran nähnyt, ja se tarkoittaa että kutsusivu on toisin kuin viesti
  haettavissa uudelleen. ~~Mittaamatta on se, tarjoillaanko sama kutsu `/bg/nextgame`stä joka
  kierroksella vai vain kerran~~, ja säilyykö osoite vastaamisen jälkeen.

  **Mitattu 14.9.2026 (`raakasivut/sessio-14-9-kutsu`, ackammonin kutsu tommih:lle):
  `/bg/nextgame` tarjoaa vastaamattoman kutsun joka kierroksella**, viisi hakua 28 sekunnin
  sisällä ja kaikki tavulleen samat (2877 tavua, `0004`–`0009`). Kutsu on siis jonon kärki
  joka ei kulu, ja niin kauan kuin siihen ei vastata, jonosta ei pääse sen ohi; sen takana
  mahdollisesti oleva viesti jää näkemättä. Samalla **Top Pagen `You have Messages!` katosi
  ensimmäisen haun jälkeen** (`0003` ennen, `0008` jälkeen), vaikka kutsu oli yhä jonossa:
  merkki kertoo uudesta kohteesta, ei jonon tilasta. Sivun rakenne (fixture
  `invitation_received.html`): `<TITLE>DailyGammon Invitation</TITLE>`, **sama otsikko kuin
  lähetetyn kutsun kuittauksella**, joten `DgPages.isInviteSentPage` ei erota niitä ilman
  lomaketta; `<H3><a href=/bg/user/<id>>nimi</a> invites you to play</h3>` ja ottelun kuvaus
  (*a private 5 point match of backgammon with no time control*) irrallisena tekstinä H3:n
  jälkeen; kolme lomaketta samaan osoitteeseen `/bg/invite/<id>`, joista `Accept Invitation`
  on **GET** `action=accept`, `Decline Invitation` POST `action=decline` kenttineen `reason`
  (80) ja `Counter Offer` POST `action=counter` samoin kentin kuin `/bg/invite/new`.

  **Mitä sovellus teki sille.** `InboxParser` luki sivun tuntemattomana lajina, tallensi
  otsikon `ackammon invites you to play` ilman ottelun kuvausta ja kirjoitti sen arkistoon
  jokaisella painalluksella uudestaan: viisi riviä `Unrecognised message`. Sääntö
  *tuntematonta ei arvata vaan säilytetään raakana* on kirjoitettu kuluvalle viestille, ja
  kutsu ei kulu. ~~Päätös `docs/AVOIMET.md`.~~ **Ratkennut samana iltana:** kutsu on oma
  lajinsa jonossa (`InvitationParser`, `QueueUiState.Invitation`), ei arkistoon, ja siihen
  vastataan jonokortista sivun omilla napeilla (`docs/UI.md`).

  **Vastaussivut mitattu 14.9.2026 illalla, samasta kutsusta** (`sessio-14-9-kutsu` rivit
  12–15, fixturet `invitation_countered.html` ja `invitation_accepted.html`). Kumpikin
  vastaa omalla sivullaan jonka `<TITLE>` on pelkkä `DailyGammon` ja jossa on logo, lause
  ja jonolinkki `/bg/nextgame`:

  - `Counter Offer` (POST `action=counter`, kentät `variant,length,comment,name,
    time_control`; `private` puuttui koska ruutu alkoi tyhjänä, korjattu): `<h3>Your
    counter-offer has been sent.</h3>` ja linkki `Continue >` **samalla lähderivillä**, mikä
    liimasi ne yhteen `SendResultParser`issa (korjattu poistamalla jonolinkki ennen lukua).
  - `Accept Invitation` (GET `action=accept`): `You have successfully joined that game.`
    ja linkki `Next`.

  **Vastatarjous kulkee kutsun tunnuksella edestakaisin, ja kumpikin puoli näkee sen
  omassa jonossaan.** Tommin vastatarjous (nack, 3 pistettä, ilman `private`ä) meni
  ackammonille, joka vastasi selaimesta omalla vastatarjouksellaan ja **rastitti
  `private`n** (Tommin tarkennus samana iltana; selaimen puoli ei ole lokissa, väli
  21.18.18–21.18.43). Sen jälkeen Top Page näytti taas `You have Messages!` ja
  `/bg/nextgame` antoi **saman kutsun `490747`** kuvauksella *a private 3 point match of
  nack with no time control*, otsikko yhä *ackammon invites you to play*. Tommi hyväksyi
  sen ja sai *You have successfully joined that game.* Kutsun tunnus ei siis vaihdu
  vastatarjouksissa, kuvaus vaihtuu viimeisimmän tarjouksen mukaan ja `private` on
  tarjouskohtainen kenttä. (Tässä luki ensin että sivusto näyttäisi oman vastatarjouksen
  tarjoajalle itselleen; se oli väärä tulkinta lokista jossa selaimen puoli ei näy.)
  Uutta ottelua ei ollut `Your turn` -listalla heti (`0013` ennen hyväksyntää, lista ei
  haettu sen jälkeen). Ackammon aloitti hyväksytyn ottelun (Tommin havainto selaimesta
  samana iltana), ja **se oli avausheiton tulos eikä roolin**: aloittaja arvotaan aina
  isommalla nopalla, sivusto vain tekee heiton näkymättä (`SUBSTANSSI.md` kohta 104).
  Tässä luki ensin että hyväksyjä ei aloita; se oli väärä yleistys yhdestä ottelusta.
  Pelin ensimmäinen lautasivu (mitattu 14.9.2026 korpuksesta, 19 sivua kymmenestä
  ottelusta): lähtöasema, kaksi eri silmälukua ja molemmat nopat aloittajan värillä, eli
  sivusto siirtää molemmat nopat aloittajan puolelle. Otsikon `Move n` juoksee ottelun
  läpi eikä ala pelistä alusta, joten pelin alku luetaan asemasta (`docs/UI.md` › Avausheitto).

- **Rating ja kokemus karttuvat vain turnausotteluista (22.8.2026, Help-sivu).**
  `Nackgammon and Backgammon tournament matches are included in the same ratings.
  Non-tournament games are not included in ratings.` **Todennettu myös mittaamalla:** kolme
  ystävyysottelua pelattiin ja luovutettiin, ja `ackammon` on yhä `Rating 1500` ja
  `Experience 0`. Mittaus vaati nimenomaan **päättyneet** ottelut, koska sama sivu sanoo
  ratingin päivittyvän vasta kun molempien siirrot ovat valmiit; käynnissä olevista otteluista
  otettu lukema ei olisi todistanut mitään. **Kokemuspisteistä Help ei sano mitään**, joten
  niiden osalta tämä on mittaus eikä sivun oma väite: kokemus ei kartu turnauksen
  ulkopuolisista otteluista. Kolmesta ottelusta kaksi oli yksityisiä ja yksi ei, joten
  erottava tekijä on turnauksettomuus eikä yksityisyys. Sama sivu kertoo, että laskenta on
  FIBS-järjestelmä, päivitys tapahtuu heti kun molempien siirrot ovat valmiit, ja muutos on
  verrannollinen ottelupituuden neliöjuureen, joten yhdeksän pisteen ottelu liikuttaa
  ratingia kolme kertaa niin paljon kuin yhden pisteen. Sovellus ei lue ratingia, joten tämä
  on tietoa kohteesta eikä vaatimus.

- **Profiilisivun tyhjä tila ja tuoreen tunnuksen arvot (22.8.2026).** Kun otteluita ei ole
  päättynyt, sivulla lukee `There are no finished matches.` Tuore tunnus näyttää arvot
  `Rating 1500` ja `Experience 0`, ja molemmat pysyivät ennallaan sen jälkeen kun kolme
  ystävyysottelua oli pelattu ja luovutettu.
- **`View active matches` Top Pagella osoittaa omaan profiiliin** eli osoitteeseen
  `/bg/user/<id>?days_to_view=30&active=1&finished=1`. Se ei ole oma sivutyyppinsä, ja se on
  sama polku jonka `TopPage.profilePath` jo kantaa.
- **`Game Lounge` ei listaa käynnissä olevia otteluita (22.8.2026).** Sivulla on turnauksia ja
  kutsuja turnauksen ulkopuolisiin otteluihin. Listausta käynnissä olevista otteluista ei ole,
  eli otteluita ei voi seurata loungen kautta. Ensimmäinen lukema oli pelkkä tyhjä sivu, joka
  ei olisi erottanut puuttuvaa listausta tyhjästä; eron sulki Tommi eikä sivu.

  **Loungen rakenne, mitattu 26.8.2026 kaappauksesta `sessio/0574_bg_lounge.html`.** Sivulla
  on kaksi caption-taulukkoa (`Matches waiting for opponents:` ja `Tournament Sign-Up`),
  niiden välissä tarjouslomake `FORM ACTION=/bg/lounge METHOD=POST` (kentät `action=submit`,
  `variant`, `length`, `comment`, `time_control`) ja lopussa pelaajahaun lomake
  `ACTION=/bg/plist`. Tarjouslomake on läsnä vaikka taulukot olisivat tyhjiä, joten
  `DgPages.isLoungePage` tunnistaa sivun siitä. **Join ja Sign Up ovat tavallisia
  GET-linkkejä eivätkä lomakkeita**: `/bg/lounge?action=accept&id=<tarjous>&userid=<oma>` ja
  `/bg/lounge?action=signup&event=<id>&userid=<oma>`.

  **Hyväksynnän vastaus on oma sivunsa, mitattu 31.8.2026 oikeasta Joinista.** Se ei ole
  lounge vaan 1237-tavuinen kuittaus: logo, lause `You have successfully joined that game.`,
  linkki `<A HREF=/bg/nextgame>Next</a>` ja navigointipalkki. Ei lomaketta, ei taulukkoa, ja
  otsikko on sivuston yleisin `DailyGammon`, joten tunnistettavaa on vain lause. Liittyminen
  näkyy loungessa siten että hyväksytty `action=accept`-rivi katoaa; uusi ottelu ilmestyy Top
  Pagelle vasta ensimmäisen siirron jälkeen. Fixture `lounge_join_confirm.html`. Kutsurivin sarakkeet ovat Join, Type,
  Length, Player, Timeout ja Comment; turnausrivin Tournament Name, Variant, Length, Rounds,
  Time (kaksi solua), Grace ja Sign Up. Turnausrivillä voi Sign Upin tilalla olla `&nbsp;`
  (ilmoittautuminen ei auki) ja rivin perässä ylimääräinen `Has Note` -solu, joka siirtää
  solumäärää, joten sarakkeet on luettava tapahtumalinkin suhteen eikä rivin lopusta.

  **Sign Upin kolmas muoto on `Cancel Signup`, mitattu 3.9.2026 kaappauksesta
  `sessio-3-9/0004_bg_lounge.html`.** Turnauksella johon on jo ilmoittauduttu Sign Upin
  tilalla on linkki `/bg/lounge?action=cancel&event=<id>&userid=<oma>`, samaa GET-lajia
  kuin signup. Ilmoittautuminen on siis peruttavissa loungesta niin kauan kuin turnaus on
  ilmoittautumislistalla, ja rivin linkki kertoo itse kumpi tila on voimassa. Kaapatulla
  sivulla oli 20 turnausriviä: 18 `Sign Up`, yksi `Cancel Signup` (Has Note -solun kanssa)
  ja yksi ilman linkkiä.

  **Sign Upin vastaus on lounge itse, mitattu 3.9.2026 oikeasta ilmoittautumisesta**
  (`sessio-3-9-signup/0006_bg_lounge_action_signup_…`). Toisin kuin Join, jolle sivusto
  antaa oman kuittaussivun, `?action=signup` palauttaa 200 OK:lla tavallisen loungen
  (8390 tavua, sama lomake ja samat taulukot), jossa ilmoittaudutun turnauksen rivillä on
  jo `Cancel Signup`. Sovelluksen rivitodiste toimi siis ilman uudelleenhakua. Cancelin
  vastausta ei ole kaapattu.

- **Palstan rakenne, mitattu 26.8.2026 kaappauksista `sessio/0416_bg_forum2.html` ja
  `sessio/0417_bg_forum2_main_read_64101.html`.** Indeksin (`/bg/forum2`) ketjurivit
  tunnistaa linkeistä `/bg/forum2/main/read/<id>`, joissa on yleensä perässä fragmentti
  (`#3`), palvelimen oma osoitin ensimmäiseen lukemattomaan viestiin; ilman fragmenttia
  linkki vie ketjun alkuun. Lukemattoman ketjun rivillä on punainen `New`-merkki linkin
  edellä samassa solussa. Sarakkeet: otsikko, viestimäärä (`ALIGN=CENTER`), aloittaja
  (`<I>`), Hide-linkki. Palstoja on kaksi (`General` oletuksena, `/bg/forum2/politics`).
  Ketjusivulla otsikko on `<H2>`, ja jokainen viesti alkaa ankkurista `<A NAME=n>` ja
  päättyy riviin `<P><B>Posted by <A HREF=/bg/user/<id>>nimi</A> at <päiväys>` ja `<HR>`;
  runko on tagitonta tekstiä jossa `<P>` erottaa kappaleet, lainaus kulkee entiteettinä
  `&gt;` ja ulkoiset linkit ovat tavallisia `<A HREF=http...>`-elementtejä. Viestien väliin
  voi tulla `<H3>`-otsake `New From Here`, joka ei ole viesti. Ketjusivun `<TITLE>` on sama
  kuin indeksin, joten laji erotetaan sisällöstä (`A NAME` + `Posted by`). Sivutusta ei
  havaittu: 21 viestin ketju tuli yhtenä sivuna. Merkistö on palstalla `windows-1252`
  (mitattu 4.8.2026, ks. merkistökohta).

  **Politics-palsta mitattiin 27.8.2026 kaappauksesta `raakasivut/forum2_politics.html`, ja
  se kumosi kaksi oletusta.** Ensinnäkin **palstan tunnus on osa polkua eikä vakio `main`**:
  Politicsin ketjut ovat `/bg/forum2/politics/read/<id>` ja Hide-linkit
  `/bg/forum2/politics/hide/<id>`. `main`-alkuun sidottu tunnistus olisi siis hylännyt koko
  sivun, eikä ero olisi näkynyt jäsennysvirheenä vaan sivuna jota ei tunneta. Toiseksi
  **palstarivin polut ovat sivukohtaisia**: General-sivulla rivi on `<b>General</b>` ja linkki
  `/bg/forum2/politics`, Politics-sivulla linkki `/bg/forum2/main` ja `<b>Politics</b>`.
  General on siis kahdella osoitteella riippuen siitä miltä sivulta katsotaan, joten polkua ei
  voi koota vaan se luetaan riviltä. Näkyvillä oleva palsta on aina lihavoitu teksti eikä
  linkki, eli valinta on luettavissa sivulta. Muu rakenne on sama kuin Generalilla: `<H1>` on
  `DailyGammon Forum: Politics`, mutta `<H2>` on molemmilla `Main Index`, joten palstan nimi
  luetaan `<H1>`:stä eikä `<H2>`:sta. Ketjusivua ei kaapattu tältä palstalta: sen lukeminen
  nollaisi New-merkin Tommin tilillä, ja rakenne on Generalin kanssa sama tuntemattomaan asti.

  **Politicsin ketjusivu mitattiin 27.8.2026 Tommin luvalla** (`raakasivut/forum2_politics_read.html`).
  Rakenne on sama kuin Generalilla kaikilta osin: `<H2>`-otsikko, `<A NAME=n>`-ankkuri,
  tagiton runko `<P>`-kappaleineen ja `Posted by`-allekirjoitus. Ainoa ero on polkujen
  palstatunnus (`Add a Comment` on `/bg/forum2/politics/add/<id>`), eli sama ero kuin
  indeksillä. Kaappaus maksoi yhden New-merkin, ja siksi ketju valittiin viestimäärältään
  pienimmäksi: yhden viestin ketju.

  **Palstan kaksi lomakesivua mitattiin 3.9.2026** (`PageCaptureLiveTest`, Tommin pyyntö:
  postaus ja vastaus sovelluksesta; raakasivut `forum2_politics_new.html` ja `forum2_add.html`).
  Kumpikin on kyselyparametriton GET, ja kummallakin on täsmälleen yksi lomake.

  *Uusi ketju* `/bg/forum2/<palsta>/new`: `<H2>New Thread</H2>`, `<FORM
  ACTION=/bg/forum2/<palsta>/submitnew METHOD=POST>`, kentät `<INPUT TYPE=TEXT NAME=title
  SIZE=80 MAXLENGTH=80>` ja `<TEXTAREA NAME=comment ROWS=10 COLS=80 WRAP=physical>`, sekä
  **kaksi samannimistä nappia** `NAME=submit`: `Preview` ja `Create New Thread`. Palvelin
  erottaa ne siis napin arvosta, joten nappi lähetetään kenttänä eikä jätetä pois.

  *Kommentti* `/bg/forum2/<palsta>/add/<id>`: `<H2>` on ketjun otsikko, sen alla teksti `Add a
  comment here:`, `<FORM ACTION=/bg/forum2/<palsta>/submitadd/<id> METHOD=POST>`, sama
  `comment`-tekstialue ilman otsikkokenttää ja napit `Preview` ja `Submit Comment`.

  `WRAP=physical` tarkoittaa että selain lähettää tekstialueen rivinvaihdot sellaisinaan
  80 sarakkeen kohdalta; sovellus lähettää käyttäjän omat rivinvaihdot eikä rivitä itse,
  koska rivitys on selaimen vanha oletus eikä palvelimen vaatimus (mittaamatta, oletus
  kirjattu). **Mittaamatta on myös lähetyksen vastaussivu** kummallakin lomakkeella sekä
  `Preview`-sivun muoto. Onnistuminen luetaan siitä että ketju tai indeksi haettuna uudelleen
  sisältää lähetetyn tekstin tai otsikon, samaan tapaan kuin luovutuksessa kadonnut rivi.
  Kommenttisivu haettiin General-palstan ketjuun ilman ketjun avaamista: `add`-polku
  johdettiin indeksin `read`-polusta, ja ketjuksi valittiin sellainen jolla ei ollut
  New-merkkiä.

- **Pelaajalista `/bg/plist`, mitattu 27.8.2026 kaappauksesta `raakasivut/player_list.html`.**
  Sivulla ei ole `<H1>`-otsikkoa lainkaan, ja `<TITLE>` on `DailyGammon - Player List`.
  Yksi taulukko, jonka otsikkorivi on `#`, `Player`, `Rating`, `Experience` kahden tyhjän
  `<TH>`:n kanssa. **Rivillä on seitsemän solua joista kolme on pelkkiä välistyksiä**
  (`<TD width=30>` ja kaksi `<TD width=10>`), joten sarakkeet on luettava profiililinkin
  suhteen eikä rivin alusta. Sijoitus on muotoa `1.`, rating desimaalilukuna vaihtelevalla
  tarkkuudella (`2292.2`) ja kokemus kokonaislukuna `ALIGN=right`-solussa. Sivu on sadan
  rivin mittainen ja **oletuslajittelu on rating**, joten ensimmäinen sivu on sivuston
  kärki eikä liity katsojaan mitenkään. Alalaidassa ovat sivun omat linkit
  `/bg/plist?type=name|rate|exp&length=100` ja `?type=rate&start=101&length=100`; ensimmäisellä
  sivulla ei ole edellinen-linkkiä. Loungella on lisäksi `POST`-hakulomake (kenttä `like`,
  piilokenttä `type=name`), ~~jota ei ole haettu~~ **mitattu 3.9.2026 oikealla haulla
  sovelluksesta proxyn läpi** (`sessio-3-9/0006`, fixture `player_search.html`). Lomake on
  sivulla sanoin *"Find a player whose name starts with"*, `MAXLENGTH=40`. **Vastaus on
  tavallinen pelaajalista**: sama `<TITLE>`, sama taulukko ja samat seitsemän solua, joten
  `PlayerListParser` lukee sen sellaisenaan. Erot listasivuun: rivejä on vain osumat
  (yhden nimen haulla yksi rivi, sijoitus `1.` on tuloksen eikä sivuston), sivun koko oli
  2534 tavua, ja alalaidassa ovat vain kolme *Sort By* -linkkiä (`?type=…&length=100`)
  ilman `Next 100`:aa. Linkit vievät siis sivun omaan listaan eivätkä lajittele hakutulosta,
  ja siksi sovellus päättää haun kun linkkiä napautetaan.

  **Sivun laji ei ole luettavissa rakenteesta, ja se on tämän sivun oma ansa.** Loungella on
  sekä profiililinkkejä riveillä että oma `/bg/plist`-linkki, eli molemmat rakenteelliset
  tuntomerkit erikseen, joten kumpikaan ei kelpaa tunnistukseen. `DgPages.isPlayerList`
  kysyy siksi `<TITLE>`ä, toisin kuin palstalla jossa otsikko on sama indeksillä ja
  ketjulla. Sama mitattiin toisesta suunnasta testinä: loungen sivu menee jäsentimen läpi
  ja tuottaa rivejä joiden "rating" on aikaraja.

- **Tournament Hallin taulukot mitattiin 27.8.2026** samasta kaappauksesta jolla sivun laji
  ratkesi 26.8.2026 (`raakasivut/tournament_hall.html`). Taulukot tunnistaa captionista, ja
  **ne ovat eri levyiset**: `Finished Tournaments` on nimi, voittaja ja päättymisaika,
  `Active Tournaments` on nimi ja alkamisaika. Aika on molemmilla rivin viimeinen solu mutta
  ei tarkoita samaa asiaa. Päättyneet ovat sivulla **ensin**, mikä on päinvastoin kuin
  nimestä odottaisi, joten järjestykseen ei saa nojata. Linkkejä on vain kahta lajia,
  `/bg/event/<id>` ja `/bg/user/<id>`.

  **Toisen pelaajan profiili on eri sivu kuin oma, mitattu 27.8.2026**
  (`raakasivut/user_profile_other.html`). `<TITLE>` on sama (`Info on player <nimi>`) ja
  rakenne muuten sama, mutta kolme eroa ovat olennaisia. **Ottelutaulukoita ei ole
  lainkaan**, koska pelaajalistan linkissä ei ole `active`- eikä `finished`-parametria;
  tyhjä ottelulista ei siis ole vika vaan sivun muoto. **Esittelyrivit vaihtelevat**: omalla
  oli E-mail, tällä Home Page, joten rivistöä ei voi kiinnittää nimilistaan. Ja sivulla on
  kaksi asiaa joita omalla ei ole: `matches versus you` -linkki ja **kutsulomake**
  (`POST /bg/invite/new`, kentät `player`, `variant`, `length`, `comment`, `name`,
  aikakontrolli). Profiilikenttiin voi kirjoittaa mitä tahansa: kaapatulla sivulla `Real
  Name` oli lista YouTube-linkkejä.

- **Turnaussivu `/bg/event/<id>`, mitattu 27.8.2026 kaappauksesta `raakasivut/event_page.html`.**
  Sivulla on `<h2>`-nimi, `<h3>Conditions of Contest</h3>` ja sen alla `<h4>`-otsikot
  `Game`, `Size` ja `Time control`, joiden teksti on rungon suoraa sisältöä ilman
  säiliöelementtiä. Sitten `<TABLE BORDER=1><CAPTION>Brackets</CAPTION>`, eli cup-kaavio.

  **Kaavio on `ROWSPAN`-ruudukko, ja se on tämän sivun koko vaikeus.** Etenevä pelaaja
  venytetään usean rivin yli (`ROWSPAN=2`, `4`, `8`, ...), joten **solun sarake ei ole sama
  kuin sen järjestysnumero rivillä**: myöhemmällä rivillä ensimmäinen `<TD>` voi olla
  kolmas sarake. Sarake on siis laskettava peittävistä soluista, ja vasta silloin sarake
  vastaa kierrosta. Soluja on kolmea lajia: pelaajalinkki (`/bg/user/`), ratkennut ottelu
  (`<B>&lt;<A HREF=/bg/game/...>voittaja</A>&gt;</B>`, eli lihavoitu ja kulmasulkeissa) ja
  kesken oleva (`<I><A ...>In Progress</A></I>`). Tulevat kierrokset ovat `&nbsp;`-soluja
  isolla `ROWSPAN`illa. Todennettu laitteella päättyneellä turnauksella: 16, 8, 4, 2, 1.

- **`/bg/userevent/<id>` ja `/bg/userwins/<id>`, mitattu 27.8.2026** (`raakasivut/user_events.html`,
  `user_wins.html`). Molemmilla `<h2>`-otsikko jossa on pelaajan profiililinkki
  (`Active Tournaments for <nimi>`, `Tournament Wins for <nimi>`) ja yksi taulukko.
  Käynnissä olevilla sarakkeet ovat sijoitus, tapahtuma, voitot ja **mahdollinen**
  `Active Game` -linkki, joka **puuttuu riviltä kokonaan** kun ottelua ei ole käynnissä; rivissä
  on lisäksi samat kolme tyhjää välistyssolua kuin pelaajalistassa. Voitetuilla sarakkeet
  ovat tapahtuma ja ajankohta, ja rivin `<A>` jää sulkematta niin että seuraava `<TD>` alkaa
  sen sisältä (sama muoto kuin viestisivun `<pre>`). Otsikot ovat `DailyGammon Events For
  <nimi>` ja `DailyGammon Wins For <nimi>`.

  **Nämä ovat pelaajan omia, toisin kuin Tournament Hall.** Ero on kirjattu tähän siksi,
  että sivut näyttävät samalta: kaikki kolme ovat turnauslistoja, mutta halli on koko
  sivuston lista.

  **`Active Game` ei kerro vuorosta eikä vastustajaa (laiteajo 15.9.2026).** Tässä luki
  27.8.–15.9. *"kun vuoroa ei ole kesken"*, ja se oli tulkinta yhdestä kaappauksesta.
  Kun rivin ottelunumero yhdistettiin Top Pagen luetteloon, osa linkillisistä riveistä jäi
  ilman osumaa: niissä vuoro oli vastustajalla. Vastustajan nimi on siis luettavissa vain
  otteluluettelosta tai oman profiilin `Active games` -listalta, ei tältä sivulta.

  **`Active Game` osoittaa katselulaudalle eikä siirtolistalle (mitattu 1.9.2026).** Osoite on
  `/bg/game/<id>/`, siis ilman siirtonumeroa ja ilman `/list`iä, ja se on **kolmas asia jonka
  sivusto piirtää laudaksi** `/bg/move/<id>/<n>`:n ja päättymissivun rinnalle. Sivulla on
  ottelun otsikko (`The Marathon #4303, Round 3`), siirron numero, lauta pistenumeroineen,
  molempien paneelit pippeineen ja pisteineen sekä ottelun pituus — eli sama aineisto jota
  `BoardParser` lukee. Erot pelattavaan sivuun ovat teoissa: nappuloissa ei ole siirtolinkkejä
  eikä sivulla ole lomaketta, vaan navigointi `First`, `Prev`, `Next`, `Last` (viimeisessä
  asemassa `Next` ja `Last` ovat tekstiä eivätkä linkkejä), linkki `Move` pelattavalle sivulle,
  linkki `List of Moves` siirtolistalle sekä muistutuslomake.

  **Domain-kommentti väitti tästä väärin 27.8.2026 alkaen** (`PlayerTournamentRow.activeMatchPath`,
  *"siirtolistan polku"*), ja väite oli kirjoitettu olettaen: turnauskaavion solut osoittavat
  `/bg/game/<id>/<n>/list`iin, ja sama muoto luettiin tähän ilman että osoitetta katsottiin.
  Fixture `player_events.html` sanoi toista koko ajan.

  **Ystävyysottelun rivi on eri muotoinen kuin turnausottelun (22.8.2026).** Sekä Top Pagella
  että profiilin `Active games` -taulukossa se on `backgammon`, `-`, `-`, `-`, `3`,
  `<vastustaja>`, `Review` ja vuorollaan `Play`. Tapahtumasarake on **pelkkää tekstiä ilman
  `/bg/event/`-linkkiä**, ja Grace, Time Pool ja Round ovat viivoja, koska aikarajaa ja
  turnausta ei ole. Viiva ei siis ole aloittamattomuuden merkki: se säilyy hyväksymisen
  jälkeen. `TopPageParser` osuu tähän tapaukseen jo nyt (`anchorIndex` putoaa arvoon 1 kun
  tapahtumalinkkiä ei ole), ja rivi jäsentyy `graceText`, `timePoolText` ja `round`
  arvoinaan `-`. Todennettu sivulta eikä koodista pääteltynä.

  **Aloittamaton ottelu on profiilin luettelossa**, eli neuvottelun alettua rivi on olemassa
  ennen kuin kumpikaan on siirtänyt. Se on odotettavaa mutta ei ennen mitattua, ja se koskee
  sovellusta suoraan, koska profiilisivu on sen ainoa reitti odottaviin otteluihin.

  **Miksi tämä on kirjattava eikä pelkkä yksityiskohta.** Sovellus tunnistaa sivun lajin
  kahdessa paikassa (`DgPages.isBoardPage`, `DgPages.isTopPage`), ja molemmat kirjoitettiin
  siksi että 200 OK voi kantaa väärää sivua. Kutsusivu on kolmas laji joka voi tulla lautaa
  pyydettäessä. Fixturea ei ole, mutta oman osoitteensa ja pysyvyytensä ansiosta se on
  otettavissa talteen ilman että jonoa kulutetaan, jos kutsu joskus on auki.
- **Ratkennut 1.8.2026:** Quick Message ja ilmoitus eroavat otsikosta, joten lajittelu ei
  vaadi sisällön tulkintaa. Aiempi huoli päinvastaisesta oli aiheeton.

  | | `<h3>` | `<title>` | Lähettäjä |
  |---|---|---|---|
  | Ilmoitus | `You have received the following telegram message:` | `DailyGammon Telegram` | ei ole |
  | Pikaviesti | `You have received the following quick message from <linkki>` | `DailyGammon Quick Message` | linkki `/bg/user/<id>` |

  **Todennettu kokeella 22.8.2026, ja se on eri laji kuin yllä oleva havainto.** 1.8. ja 4.8.
  näyttö oli kaksi eri sivua jotka sattuivat olemaan eri lajia. Nyt lähettäjä oli tunnettu ja
  aikomus oli tuottaa telegram: Tommi lähetti toiselta omalta tunnukseltaan viestin profiilin
  `Quick message` -kentällä, ja sivu palautti `DailyGammon Quick Message`in lähettäjälinkkeineen
  ja vastauslomakkeineen. **Tunnuksen lähettämä viesti ei siis voi saapua ilmoituksena**, eikä
  lajittelusääntö nojaa enää siihen että kaksi eri sivua osui kohdalle. Sivun rakenne oli
  tavulleen sama kuin fixturen `inbox_quick_message.html`, joten jäsentimeen ei tullut työtä.

  `InboxParser` lukee tämän. Tuntematonta otsikkoa **ei arvata**: laji on `UNKNOWN` ja otsikko
  säilyy raakana, koska haku on tuhoava eikä tuntematon muoto saa hukata viestiä. (Tässä luki
  9.8.2026 asti että `kind` jää nulliksi. Null poistui samana päivänä, ks. avoimien listan
  otsikkokohta.)
- **Jonon viestisivu voi tulla minkä tahansa lautapyynnön vastauksena, ei vain
  `/bg/nextgame`sta.** Mitattu 13.9.2026 klo 01.00.16 (`sessio-13-9-yo`, rivi 8): `Submit
  Move` osoitteeseen `/bg/move/<ottelu>/<siirto>` palautti 435 tavun `DailyGammon Quick
  Message` -sivun, jonka `Next >>` osoittaa `/bg/nextgame`en. Top Page oli 26 sekuntia
  aiemmin näyttänyt `Messages!`-merkin. Sama tapaus on korpuksessa jo 2.9.2026 (`sessio-2-9`,
  rivi 11, `/bg/move/5314359/673`). Sivusto siis tarjoaa jonon kohteen siirron jälkeen
  samaan tapaan kuin se tarjoaa seuraavan laudan; sovelluksen on tunnistettava viestisivu
  lautapolulla eikä olettaa että viesti tulee vain Inboxin haussa.
- Pikaviestiin vastataan lomakkeella `POST /bg/sendmsg/<käyttäjänumero>`, kenttä `text`.
  Kentän nimi luetaan lomakkeelta eikä oleteta. (Tätä luonnehdittiin 1.8.2026 ainoaksi
  POST-lomakkeeksi kirjautumisen jälkeen. Se ei pidä paikkaansa: siirron jälkeisen
  chat-ruudun lähetys on myös POST, ks. alempaa.)
- **`/bg/sendmsg/<käyttäjänumero>` on yksi osoite johon on kaksi sisäänkäyntiä, ja se on
  osoitettu käyttäjälle eikä viestille.** Mitattu 22.8.2026 raaoista tavuista, kahdella
  omalla tunnuksella. Pelaajan profiilisivulla on lomake `Quick message to <nimi>:`, ja sen
  `action` on sama `/bg/sendmsg/<numero>` ja sama kenttä `text` kuin saapuneen viestin
  vastauslomakkeessa. Kentän `maxlength` on molemmissa 80, vain `size` eroaa (40 profiililla,
  80 viestisivulla), eli raja on sama ja vain ruudun leveys eri.

  **Tästä seuraa se mitä arkisto tarvitsee: vastaaminen onnistuu senkin jälkeen kun
  viestisivu on kulutettu.** Koe 22.8.2026: viesti luettiin klo 10:29, jolloin sivu
  vastauslomakkeineen katosi, ja lähetys samaan osoitteeseen onnistui klo 10:48. Osoite ei
  siis viittaa viestiin vaan vastaanottajaan, ja `InboxItem.replyPath` on varmennus eikä
  ainoa reitti. Ilman tätä arkistosta vastaaminen olisi pitänyt rakentaa sen varaan että
  vastauslomake on tallessa lukuhetkeltä.

- **Onnistuneella lähetyksellä on oma sivunsa, ja se on ainoa tapa erottaa onnistuminen.**
  `POST` palauttaa 200 OK:lla sivun jonka otsikko on `Message Sent` ja jonka runko sanoo
  `Your message has been sent to <linkki vastaanottajaan>`. Statuskoodi ei kelpaa
  erottimeksi, koska katkennut istunto palauttaa login-sivun myös 200 OK:lla.
  `DgPages.isMessageSentPage` tunnistaa tämän otsikosta, fixture `message_sent.html`.

  Runko nimeää vastaanottajan, eli lähetyksen kohteen voisi **tarkistaa** eikä vain
  luottaa siihen. Sitä ei toistaiseksi lueta, koska lähettävää ruutua ei ole.

- **Sovelluksen oma lähetys todennettiin sivustoa vasten 22.8.2026, merkistö mukaan lukien.**
  Tämä on eri asia kuin aiemmat merkistömittaukset, ja ero on kirjattava: 4.8. ja 5.8. koetin
  luki **selaimen** lähettämiä tavuja profiilin `Location`-kentästä, ja `DgClient.formBody`
  kirjoitettiin sitä vasten. Pariteetti oli siis päätelty eikä mitattu, koska sovellus ei
  lähettänyt mitään.

  Nyt se on mitattu kahdella tunnuksella: sovelluksesta lähetetty vastaus luettiin
  vastaanottajan tunnuksella selaimessa, ja `å` näkyi oikein. Sama sivu näytti oikein myös
  selaimesta lähetetyn `äöå`:n, eli molemmat reitit tuottavat saman lopputuloksen.

  **Rajaus, jotta näyttöä ei lueta laajemmaksi kuin se on.** Sovelluksen lähettämässä
  tekstissä oli vain `å` (0xE5), koska tabletin näppäimistön ennakointi muutti kirjoitetun
  merkin. `ä` (0xE4) ja `ö` (0xF6) on todennettu selaimen reitiltä. Kaikki kolme ovat
  `windows-1252`:ssa yksitavuisia samalla alueella, joten yhden todennus on vahva mutta ei
  tyhjentävä näyttö kahdesta muusta.

- **Vastauskenttä on yksirivinen ja rajattu 80 merkkiin** (`<INPUT TYPE=text NAME=text
  SIZE=80 MAXLENGTH=80>`), eikä sitä ole esitäytetty millään. Mitattu 4.8.2026 raaoista
  tavuista. Raja on ruudun rajoite eikä kosmetiikkaa, ja `InboxItem.replyMaxLength` lukee
  sen sivulta.

  **Kanoni sanoi tästä kaksi kertaa väärin, ja syy on kirjattava**, koska se on menetelmä
  eikä yksityiskohta. Ainoa pikaviestisivu oli 1.8.2026 tallennettu Bravella, jossa Tommilla
  on laajennus joka vaihtaa sivuston yksirivisen kentän moniriviseksi ja esitäyttää sen
  lainauksella. **Tallenteeseen päätyi laajennuksen DOM sivuston omana rakenteena**, ja
  sekä kanoni että jäsennin kirjoitettiin sitä vasten. `InboxParser` haki vastauskenttää
  valitsimella `textarea`, eli `replyField` olisi ollut null juuri tuotannossa. Ks.
  laajennusansa omana kohtanaan alempana.
- Viestisivun `<pre>`-lohkon **sisällä** ovat vastauslomake ja erotinviivat, joten naiivi
  `pre.text()` lukisi liikaa. `InboxParser` lukee siksi lohkoa ensimmäiseen lomakkeen
  aloittavaan elementtiin asti (`<hr>`, `<form>`, `<input>`, `<textarea>`).

  **Ei siis ensimmäiseen elementtiin, ja ero on mitattu.** Aito ilmoitussivu sisältää
  `<a>`-linkkejä keskellä virkettä, joten vanha "pysähdy ensimmäiseen elementtiin" jätti
  viestiksi pelkän `Congratulations to` ja pudotti loput hiljaa. Vika ei näkynyt aiemmin,
  koska ainoa telegram-testi käytti kuvakaappauksesta käsin kirjoitettua sivua jossa ei
  ollut linkkejä. Aiempi perustelu tässä kohdassa (lomake sisältää saman viestin
  lainattuna, joten viesti kaksinkertaistuisi) oli myös väärä: lainaus tuli laajennuksesta.
- **Pikaviestisivulla ei ole aikaleimaa lainkaan.** Sivu kertoo lähettäjän ja tekstin,
  ei hetkeä. Tästä seurasi sopimusmuutos, ks. seuraava kohta.
- **Saapumishetki on osa viestin tunnistetta** (päätetty 1.8.2026, Tommi).

  *Mikä muuttui:* aiempi kanoni sanoi että tunniste lasketaan **pelkästä sisällöstä**, ja
  perusteli sen idempotenssilla: sama sivu voidaan jäsentää useasti. Koska pikaviestisivulla
  ei ole aikaleimaa, se tarkoitti että sama pelaaja voi lähettää `Great match!` kahdesti ja
  jälkimmäinen katoaa hiljaa `OnConflictStrategy.IGNORE`en. Tommin perustelu muutokselle:
  **kohteliaisuus on sisään kirjoitettu suureen osaan pelaajista**, joten lyhyt toistuva
  viesti ei ole reunatapaus vaan tavallisin tapaus.

  *Miksi tämä ei riko idempotenssia:* saapumishetki leimataan **kerran, kun viesti
  muodostetaan sivulta**, ei tallennushetkellä. Sama `Message`-olio tuottaa siis aina saman
  tunnisteen, joten epäonnistuneen kirjoituksen uusiminen on yhä turvallista. Vain **eri
  noudot** eroavat toisistaan, ja koska haku on tuhoava, sama viesti voidaan noutaa vain
  kerran. Suojaus jonka menetämme koskee siis tilannetta jota ei ole.

  *Mitä tästä seuraa:* `Message.receivedAtEpochMillis` on osa tiivistettä, eikä
  `MessageArchive` enää leimaa mitään. Leiman antaa se joka muuntaa sivun viestiksi.
  Kannan sarake `storedAtEpochMillis` säilytti nimensä, jottei skeema muutu tarpeettomasti;
  sen merkitys on nyt "milloin laite näki viestin", ja se on osa tunnistetta.
- **Merkistö on `windows-1252` lukusuunnassa. Mitattu 4.8.2026, ja kaksi aiempaa oletusta
  kaatui kerralla.** Toinen pelisessio proxyn läpi kävi ensi kertaa keskustelupalstalla
  (`/bg/forum2`), ja siellä olivat korpuksen ensimmäiset ei-ASCII-tavut: kolme sivua 421:stä,
  seitsemän eri tavuarvoa. Palvelin lähettää yhä `Content-Type: text/html` ilman charsetia,
  eli otsake ei ratkaissut mitään; tavut ratkaisivat.

  | Tavu | `windows-1252` | `ISO-8859-1` |
  |---|---|---|
  | `0x92` | `’` U+2019 | U+0092, C1-kontrollimerkki |
  | `0x93` | `“` U+201C | U+0093, C1-kontrollimerkki |
  | `0x94` | `”` U+201D | U+0094, C1-kontrollimerkki |
  | `0x97` | `—` U+2014 | U+0097, C1-kontrollimerkki |
  | `0xE7` `0xE9` `0xF6` | `ç` `é` `ö` | sama |

  Neljä ylintä ovat ratkaisevia, koska ne **eivät ole Latin-1:ssä olemassa** näkyvinä
  merkkeinä. Merkistö on siis `windows-1252` eikä Latin-1, ja ero osuu juuri englannin
  tavallisimpiin merkkeihin: kaarevaan heittomerkkiin sanassa `It’s` ja lainausmerkkeihin.
  Molemmat toistuvat jokaisessa palstaviestissä, eli ero ei ole reunatapaus.

  **Nykyinen koodi on mitattavasti väärässä.** `DgClient` lukee rungon
  `response.body?.string()`illa, ja koska charsetia ei ole, OkHttp olettaa UTF-8:aa. Nämä
  tavut eivät ole kelvollista UTF-8:aa lainkaan (todennettu: `0x92` kaataa dekoodauksen),
  joten ne muuttuvat korvausmerkiksi U+FFFD. Se on **peruuttamaton**: alkuperäinen tavu on
  mennyt ennen kuin jäsennin näkee mitään. Latin-1 taas ei kaadu vaan tuottaa hiljaa
  kontrollimerkin, mikä on sama vika toisessa muodossa.

- **Numeerisia entiteettejä ei ollut yhtäkään.** Koko 421 sivun korpuksesta löytyi nolla
  osumaa muotoon `&#123;` tai `&#x1F600;`. Toisen asiakkaan havainto emojista (ks. oma
  osionsa) ei siis kuvaa lukusuuntaa, ja **kysymyksen uudelleenmuotoilu 4.8.2026 aamulla oli
  väärä**: työ ei ole entiteettien purkamista.

  Yhteensopiva selitys molemmille havainnoille on olemassa, ja se on hypoteesi: **selain**
  koodaa lomaketta lähettäessään ne merkit joita sivun merkistö ei tunne, muotoon `&#N;`.
  Emoji ei mahdu `windows-1252`:een, ääkkönen mahtuu. Jos selitys pitää paikkansa,
  entiteetti ei ole sivuston tapa vaan selaimen, eikä meillä ole selainta. Silloin
  **kirjoitussuunta on eri työ kuin lukusuunta**, ja se on yhä mittaamatta.

- **Kirjoitussuunta mitattiin samana iltana, ja se on tavutarkka.** Koetin oli profiilin
  `Location`-kenttä (`/bg/profile/pub`, `NAME=where`), koska se on ainoa tuntemamme paikka
  jossa oma syöte palautuu heti: `internet` → `internetissä öå` → takaisin. Tulos oli sama
  kolmella pyynnöllä, joista yksi oli erillinen GET, eli **palvelin säilytti tavut** eikä
  vain kaiuttanut niitä: `ä` = `0xE4`, `ö` = `0xF6`, `å` = `0xE5`, entiteettejä nolla.

  Kaksi rajausta jotka estävät lukemasta tätä liian pitkälle. Nämä kolme merkkiä ovat
  Latin-1-alueella eli identtisiä `windows-1252`:ssa ja `ISO-8859-1`:ssä, joten **erottelu
  näiden kahden välillä lepää yhä yksinomaan palstasivujen neljällä tavulla**, ei tällä.
  Ja emoji jäi koettimesta pois, joten se on yhä mittaamatta.

  Seuraus koodiin: lomakerunko koodataan `windows-1252`:na eikä OkHttpin oletuksena, koska
  `FormBody` prosenttikoodaa aina UTF-8:na ja `ä` lähtisi kahtena tavuna. ~~Chat-kentän
  ääkköskoetin on siis yhä ajamatta, mutta se ei enää ole se koetin josta asia riippuu.~~
  **Ajettu 23.8.2026, ks. seuraava kohta.**

- **Chat-alueen saapumissuunta mitattu 23.8.2026, ja se on tavutarkka.** Koetin oli kahden
  oman tunnuksen keskinäinen ottelu: toiselta tunnukselta kirjoitettu chat-viesti `abc åäö`
  luettiin ensimmäisellä tunnuksella tabletilta proxyn läpi. Sivu palautti `<PRE>`-lohkossa
  tavut `0xE5 0xE4 0xF6`, eli `å ä ö` yksitavuisina, eikä yhtään `&#N;`-entiteettiä.
  Saapumissuunta käyttäytyy siis kuten profiilikentästä mitattu kirjoitussuunta, eikä
  koettimen siirrettävyys kentästä toiseen ole enää oletus.

  Kolme rajausta, jotta näyttöä ei lueta laajemmaksi kuin se on. Viesti on **lavastettu eikä
  havaittu**: se todistaa mitä palvelin tekee ääkköselle, ei sitä mitä oikeat vastustajat
  kirjoittavat. Merkit ovat samalla Latin-1-alueella kuin aiemmissa mittauksissa, joten
  erottelu `windows-1252`:n ja `ISO-8859-1`:n välillä lepää yhä palstasivujen neljällä
  tavulla. Ja emoji jäi tästäkin koettimesta pois.

  **Sivutuote, joka selittää tyhjältä näyttävän ruudun:** chat-lohko ei ole laudan
  yleisnäkymässä vaan siirron jälkeisellä sivulla. Kaappauksen 22 sivusta `says:`-otsikko on
  tasan yhdellä, ja se on `Submit Move`n vastaus. Vastaanotettu viesti on siis näkymätön
  siihen asti kunnes oma siirto on tehty.

  Jäsennin lukee tämän ilman muutosta, ja se on lukittu fixtureen `chat_thread_aakkoset.html`
  sekä testiin `saapuva aakkonen sailyy tavuna eika muutu entiteetiksi`. Fixture on levyllä
  UTF-8:na, koska `DgClient` purkaa tavut `windows-1252`:sta merkkijonoksi ennen jäsennintä.

- **Emoji ratkesi 5.8.2026, ja vastaus on ettei sivusto kanna sitä lainkaan.** Koetin oli
  sama profiilin `Location`-kenttä kuin ääkkösellä, ja ratkaisevaa oli **julkisen profiilin
  (`/bg/user/<id>`) lataaminen samalla kun arvo oli asetettuna.** Ilman sitä sivua olisi
  jäänyt auki, escapaako sivusto vain lomakekentässä vai kaikkialla.

  Ketju kokonaisuudessaan, kolme mitattua askelta:

  1. **Selain muuntaa vain sen mitä merkistö ei kanna.** Samassa lähetyksessä `ä` meni
     tavuna `0xE4` ja emoji muotona `&#128536;`. Kaksi eri kohtelua samassa kentässä
     samalla POSTilla on se mikä tekee tästä havainnon eikä arvauksen. 4.8. kirjattu
     hypoteesi on siis nyt mitattu.
  2. **Sivusto tallentaa entiteetin tekstinä eikä tulkitse sitä.**
  3. **Ulostulossa `&` escapataan muotoon `&amp;`, ja tämä pätee myös näyttösivulla.**
     Julkinen profiili palautti `<TD>internetissä &amp;#128536;`, eli selain piirtää
     ruudulle kirjaimellisen merkkijonon `&#128536;`. Emoji ei siis renderöidy
     DailyGammonissa kenellekään, ei edes selaimessa.

  **Seuraus lukusuuntaan: älä pura toista kerrosta.** Jsoup purkaa `&amp;#128536;` muotoon
  `&#128536;`, ja se on täsmälleen se mitä sivustokin näyttää. Toinen purkukierros
  näyttäisi hymiön siellä missä sivu näyttää tekstiä, eli väittäisi enemmän kuin sivu
  sanoo. Tämä kirjataan tänne siksi että se on **houkutteleva ja väärä** korjaus, jonka
  joku (minä 5.8.) ehtii ehdottaa ennen kuin näyttösivu on mitattu.

  **Seuraus kirjoitussuuntaan: `DgClient` tekee saman kuin selain** (Tommin päätös
  5.8.2026, *"selaimen tapa ja pariteetti sivuston kanssa"*). Merkki jota `windows-1252`
  ei tunne muuntuu muotoon `&#N;` ennen URL-koodausta. Aiempi `?`-korvaus poistui: se oli
  näkyvä menetys aikana jolloin oikeaa muotoa ei tiedetty, ja nyt tiedetään.

  Muunnos käy **koodipisteinä eikä merkkeinä**, koska emoji on Javassa sijaispari: merkki
  kerrallaan luettuna siitä syntyisi kaksi entiteettiä joista kumpikaan ei ole se merkki.

  Kaksi rajausta jotka pariteetti tuo mukanaan tarkoituksella. Vastaanottaja näkee sotkua,
  ja **yksi emoji syö kymmenen merkkiä** pikaviestin 80:n rajasta. Kumpikaan ei ole meidän
  aiheuttamaamme: se on se mitä selainkäyttäjä jo tuottaa, ja pariteetti tarkoittaa myös
  kohteen vikojen perimistä.
- **Siirto committoituu `Submit Move`illa, ei vuoron päättävän sivun napilla** (mitattu
  27.8.2026 laiteajossa). Vuoron päättävän sivun lomakkeessa on piilokenttä `commit=1`, ja
  siitä pääteltiin että `Next Game` tai `To Top` viimeistelee siirron. **Päätelmä oli väärä.**
  Tommi poistui laudalta järjestelmän paluueleellä painamatta kumpaakaan nappia, ja siirto
  löytyi silti sivuston omasta kirjanpidosta (`<<Review Game`, rivi `53: 18/13 16/13`).

  **Seuraus sovellukselle:** paluuele on täysiverinen uloskäynti eikä hiljainen ansa, ja
  sivun napit ovat navigointia sekä viestin lähetys, eivät vuoron viimeistely.

  **Miksi tämä meni ensin väärin, ja se on kirjaamisen arvoista.** Ottelu jäi Top Pagen
  listalle grace-kello juoksemassa, mikä näytti keskeneräiseltä vuorolta. Selitys on alla
  oleva kohta, ja hämäystä lisäsi se että sama heitto 5-3 sattui tulemaan kahdesti.
- **Top Pagen `Your turn` ja grace kattavat myös ennakkokysymykset** (mitattu 27.8.2026).
  Ottelu näkyy siirrettävänä myös silloin kun vastustajan oma siirto on tekemättä ja sivusto
  vain kysyy varautumista arvaussivulla (`What will you do if the game proceeds this way?`).
  Kirjanpidossa se näkyy niin että vastustajan rivi on tyhjä (`54:` ilman siirtoja) vaikka
  oma siirto on jo kirjattu.

  **Seuraus sovellukselle:** otteluluettelon `Your turn` ei tarkoita että vastustaja odottaa
  sinua. Se on syytä muistaa jos luettelon merkitystä joskus kiristetään tai siitä johdetaan
  lukuja.
- Vastustajan viesti näkyy **vasta oman siirron jälkeen**, ei lautaa avattaessa. Siirron
  jälkeisellä ruudulla on tekstikenttä sekä painikkeet `Next Game` ja `To Top`. Chat voi
  muodostaa ketjun kun molemmat kirjoittavat.

Siirron jälkeisestä chat-ruudusta (kaapattu ja jäsennetty 3.8.2026, `ChatParser`):

- **Otsikkoja on kolme ja ne tarkoittavat eri tilannetta.** Kaksi ensimmäistä ovat Tommin
  vahvistus 3.8.2026: `You may chat with <nimi> here:` kun keskustelua ei vielä ole, ja
  pelkkä `Chat:` kun ketju on alkanut. **Kolmas mitattiin 21.8.2026: `<nimi> says:`**, ja se
  esiintyy kun vastustaja on juuri kirjoittanut. Yksikään ei siis yksin tunnista sivua.

  *Luku muuttui kahdesta kolmeen, ja lähde on syytä nimetä.* Kolmas muoto löytyi DG Mobilen
  liikenteestä eikä omasta selailusta (`docs/TOINEN-ASIAKAS.md`), ja se oli **vuotanut
  jäsentimen läpi hiljaa**: sivu jäsentyi muuten oikein, mutta `ChatParser.opponentName`
  palautti nullin juuri sillä sivulla jolla vastustajan nimi on näkyvissä. Vika ei näkynyt
  virheenä vaan puuttuvana tietona, eli sama sokean koettimen muoto joka on nimetty
  `Kaanon/TYÖTAVAT.md`:ssä. Korjattu 21.8.2026, fixture `chat_thread_says.html`.

  Vanha rivi sanoi että vastustajan nimi on luettavissa vain ensimmäisestä muodosta. Se ei
  pidä paikkaansa: nimi on myös kolmannessa, ja nimenomaan siinä tilanteessa jossa viesti on.
- **Siirron jälkeisen chat-sivun `<pre>` sulkeutuu** (todennettu 21.8.2026 raa'oista tavuista).
  Tämä oli auki 3.8.2026 alkaen, koska silloinen kaappaus oli selaimen `Ctrl+S`-tallenne, joka
  sulkee auki jääneet elementit itse eikä siksi kelpaa todisteeksi kumpaankaan suuntaan.
  **Pikaviestisivun `<pre>` jää yhä sulkematta**, eli sivut eroavat toisistaan tässä eikä
  havaintoa saa yleistää sivulta toiselle. Jäsennin ei nojaa kumpaankaan, ja se pysyy niin.

  **Vahvistettu 27.8.2026 kolmesta riippumattomasta kaappauksesta**, jotka ovat kolmesta eri
  ottelusta ja kolmesta eri proxysessiosta (`sessio/0074`, `sessio-23-8-chat/0021`,
  `sessio-dgmobile/0202`). Jokaisessa on tasan yksi `<PRE>` ja yksi `</PRE>`, ja lomake alkaa
  vasta sulkevan tagin jälkeen. Yksi näistä on DG Mobilen liikennettä, eli sulkeutuminen ei
  ole selaimen tekoa.
- **Ketju on `<pre>`-lohkossa, jossa lainattu osa alkaa merkillä `>`.** Lainaamaton osa on
  vastapuolen uusin viesti, ja **vain se kuuluu arkistoon**. Jos sitaatti otettaisiin
  mukaan, sama viesti saisi eri sisältötiivisteen joka kierroksella ja `Message.id`:n
  idempotenssi pettäisi juuri siinä kohdassa jota varten se rakennettiin.
- Lomakkeessa on `Quote previous message` **oletuksena valittuna** (`name=quote`), mikä
  selittää sitaatin syntymisen. Syvempää lainausta (`>>`) ei ole nähty, eikä sen käsittelyä
  ole kirjoitettu arvauksen varaan.
- **Chat-kentässä ei ole `maxlength`-attribuuttia lainkaan** (mitattu 27.8.2026 samoista
  kolmesta kaappauksesta): kenttä on `<textarea name=chat rows=5 cols=80 wrap=physical>`.
  Vertailukohta on pikaviestin `<input type=text size=80 maxlength=80>`, eli 80 merkin
  kirjoitusrajoitin kuuluu sinne eikä tänne. Sivulta luettavaa rajaa ei siis ole olemassa,
  ja se on eri asia kuin että raja olisi mitattu suureksi.
- **Lainaus ei ole tekstikentässä vaan erillisessä valinnassa**, joten sitaatti ei rakenteen
  puolesta voi syödä vastauksen tilaa. Tämä vastaa Tommin ehtoon *jollei se haittaa
  vastaamista* (`docs/AVOIMET.md`) siltä osin kuin kysymys koskee kenttää. Palvelimen puoli
  on eri kysymys, ja se on yhä mittaamatta.
- ~~**`wrap=physical` on selaimen teko, jota sovellus ei tee.**~~ **Kumottu 27.8.2026, ja
  korjaus on tässä koska väite oli minun eikä sivuston.** Kirjasin aamulla että selaimesta ja
  sovelluksesta lähetetty pitkä viesti saapuu eri muodossa, koska attribuutti panee selaimen
  rivittämään ja `ChatForm.write` ei rivitä. Mittaus sanoo muuta: **rivittäjä on palvelin.**

  Todiste ei ole tämän päivän kaappaus yksin vaan sen ja 24.8.2026 pikaviestimittauksen
  yhdistelmä. Siellä lähetys tehtiin yksirivisestä `<input type=text>` -kentästä, joka **ei
  voi tuottaa rivinvaihtoja lainkaan**, ja perillä oli silti 25 riviä joiden pisin oli tasan
  80. Sivusto siis rivittää itse, eikä selaimen osuutta tarvita selittämään mitään.

  Chat-kentästä lähetetty viesti käyttäytyi samoin: 29 riviä, pisin tasan 80 merkkiä,
  rivinvaihdot LF eikä CRLF, eikä lopusta katkennut mitään. Fixture `chat_thread_long_quote.html`
  lukitsee muodon. **Mitä jää auki:** sovelluksesta lähetettyä pitkää viestiä ei ole nähty
  perillä, joten viimeinen askel on laiteajossa. Ennuste on nyt kuitenkin päinvastainen kuin
  aamulla: eron ei pitäisi syntyä.

  Rivitys on silti tiedon menetys, ja se koskee myös sovellusta: perillä olevasta
  rivinvaihdosta ei näe kumpi sen teki, lähettäjä vai rivitys.

  **Auki jäänyt askel mitattiin 28.8.2026, ja ennuste piti paitsi yhdessä kohdassa joka on
  vika.** Sovelluksesta lähetettiin 8627 merkin viesti (111 LF-rivinvaihtoa, ei yhtään
  CR:ää), ja se luettiin perillä vastaanottajan selaimesta (view-source-tallenne
  `long-text-at-ckammon.html`, ei versioon). Kolme tulosta:

  - **Palvelin rivittää, kuten ennustettu**: perillä 190 riviä, pisin tasan 80, ei yhtään
    yli. Koko sisältö tuli perille eikä loppu katkennut, eli rajaa ei löytynyt vielä
    8627 merkilläkään.
  - **Paljas LF maksaa yhden merkin LF-parin jäljestä.** Diffaus lähetetyn rungon
    (arkistosta) ja perillä olleen välillä: jokainen kadonnut merkki on heti kahden
    peräkkäisen LF:n jälkeen. `"\n\nGemini"` saapui muodossa `"\n\nemini"`, ja
    `"\n\n\n"`-kolmikosta katosi kolmas LF. Yhdeksän kappaleenalkua menetti ensimmäisen
    kirjaimensa. Selain ei koskaan laukaise tätä, koska HTML:n lomakenormalisointi
    lähettää rivinvaihdot CRLF:nä; palvelin siis olettaa CRLF:n ja syö paljaan LF-parin
    perästä yhden tavun.
  - **Korjaus samana päivänä**: `ChatForm.write` normalisoi rivinvaihdot CRLF:ksi ennen
    lähetystä (testi `ChatFormWriteTest`). Arkistoon viesti kirjataan yhä niin kuin se
    kirjoitettiin, koska tämä on lankamuoto eikä sisältö. Korjattu muoto on todentamatta
    perillä; se todennetaan seuraavasta sovelluksesta lähetetystä monikappaleisesta
    viestistä.
- **Chat-lohko asuu vuoron päättävällä ruudulla, ei tietyllä teolla** (mitattu 27.8.2026
  illalla, ja tämä korjaa saman päivän aiemman kirjauksen).

  **Väärä välimuoto ensin, koska se kertoo mistä virhe syntyi.** Aamupäivällä kirjattiin että
  lohko *"tarttuu ensimmäiseen palveltuun sivuun sen jälkeen kun viesti on lähetetty, tekoon
  katsomatta"*. Päätelmä nojasi yhteen näytteeseen: viesti saapui tiedostossa
  `0093_..._submit_Roll_Dice.html`, siis muulla kuin siirtovastauksella. Illan ajossa sama
  viesti oli odottamassa viiden peräkkäisen sivun ajan **ilman että lohko oli yhdelläkään**,
  ja `0138`:ssa teko oli jopa `Submit Move`. Yhden näytteen sääntö kumoutui siis viidellä.

  **Oikea erotin on vastaussivun laji eikä pyynnön osoite.** Kuudesta sivusta kaikissa
  kolmessa joissa on painike `Next Game`, on myös chat-lohko, eikä yhdessäkään kolmesta muusta
  ole kumpaakaan. Lohko on siis sillä ruudulla jolla vuoro päättyy ja sivu tarjoaa poistumisen
  (`Next Game`, `To Top`), riippumatta siitä pyydettiinkö sivu `Submit Move`illa, `Roll
  Dice`illä vai `Submit Forced Move`lla. Ylempi rivi *"vasta oman siirron jälkeen"* oli siis
  oikein, ja aamun korjaus oli väärä.

  **Yleinen muoto, koska tämä virhe on toistunut:** pyynnön osoite kertoo mitä pyydettiin,
  ei mitä vastaukseksi tuli. Tässä projektissa tiedostonimet on johdettu osoitteesta, joten
  osoitteeseen nojaava luokittelu näyttää mittaukselta olematta sitä.

  **Yksi seuraus on suoraan lukuun.** 27.8.2026 laskettu 110/312 laskettiin vain
  `*_submit_Submit_Move.html`-tiedostoista, joten se on alaraja eikä osuus: vuoron päättävä
  ruutu voi syntyä muunkin pyynnön vastauksena. Sovelluksen käytös on oikein riippumatta
  tästä, koska `BoardViewModel` ajaa `ChatParser`in jokaiselle hyväksytylle lautavastaukselle.
- **Jono ei tuo chat-lohkoa mukanaan** (mitattu 27.8.2026, ja tämä sulkee avoimen kohdan).
  Toinen tunnus lähetti viestin, minkä jälkeen vastaanottaja meni `/bg/nextgameen` avaamatta
  ottelua Top Pagelta. Jono tarjosi oikean ottelun (`0103_bg_nextgame.html`, ottelu 5316601,
  otsikko `Please select your action/make your move:`), eikä sivulla ollut `name=chat`-kenttää,
  `<pre>`-lohkoa eikä yhtään kolmesta otsikkomuodosta.

  **Ajoitus todistettiin jälkikäteen eikä oletettu.** Sama viesti ilmestyi 5 minuuttia
  myöhemmin samalle tunnukselle vuoron päättävällä ruudulla (`0144`, sisältönä `koe 4` ja
  lainattuna `koe 3`), eli se oli olemassa jo silloin kun jono antoi laudan ilman sitä.
  Ilman tätä askelta mittaus olisi nojannut siihen ettei viesti vain ollut vielä perillä.

  **Seuraus sovellukselle:** samaa viestiä ei voi saada kahdesta lähteestä, joten
  `readChat`iin ei tarvita kaksinkertaisen kirjauksen vahtia jonon takia.
- **Lainaus on yhtä tasoa rakenteen pakosta, ei sattumalta** (mitattu 27.8.2026). Viesti
  lähetettiin lainaus päällä, ja vastaanottaja näki sitaatin. Kun hän vastasi lainaus päällä,
  takaisin saapuvassa lohkossa oli **vain hänen lainaamansa oma tekstimme**, ei enää sitä
  viestiä jonka me olimme lainanneet. Sivusto lainaa siis edellisen viestin oman sisällön ja
  pudottaa sen lainauksen, joten `>>` ei voi syntyä eikä ketju kasva. Aiempi kirjaus sanoi
  vain ettei syvempää lainausta ole nähty; nyt tiedetään myös miksi.
- **Valintaruutu puuttuu kokonaan kun lainattavaa ei ole** (mitattu 27.8.2026). 136:ssa
  kaapatussa sivussa on otsikko `You may chat with <nimi> here:`, ja niistä **nollassa** on
  `name=quote`. Ruutu ja `<pre>`-lohko ilmestyvät yhdessä vasta kun ketju on alkanut.
  `ChatForm.quoteField` on siksi oikein nullable.
- **Chatin lähetys on POST**, kohteena lautaosoite (`/bg/move/<id>/<tila>`), kentät `chat`,
  `quote` ja piilokenttä `commit=1`. Aiempi kirjaus "ainoa nähty POST-lomake" koski
  `/bg/sendmsg`iä ja on nyt vanhentunut: **POST-lomakkeita on kaksi**, ja siirtojen
  GET-luonne koskee vain nappuloiden klikkailua.
- Lomakkeen `action` osoitti tilatunnisteeseen 1121, vaikka haettu sivu oli 1119. Sama
  sääntö kuin muualla: lue linkki, älä laske sitä.
- **Otsikko `What will you do if the game proceeds this way?` tarkoittaa että lauta on
  hypoteettinen**, ei nykytila. Kyse on alempana kuvatusta koneen arvauksesta: sivusto
  näyttää tulevan aseman ja kysyy siirtoa etukäteen. Chat samalla sivulla on silti aitoa.
  Ansa on hiljainen, koska lauta näyttää täysin tavalliselta; `ChatScreen.speculative` on
  ainoa erotin ja siksi tästä sivusta luettua lautaa ei saa kirjata pelin tilaksi.
- Chat-lohko **ei kerro lähettäjää** lainkaan. Sivulla on kaksi käyttäjälinkkiä joista
  kumpaakaan ei ole merkitty kirjoittajaksi, joten lähettäjä kootaan laudan
  pelaajapaneeleista eikä arvata chatista.
- ~~**Rajaus jota ei ole todennettu:** onko tämän sivun `<pre>` sulkematta kuten
  pikaviestisivulla.~~ **Vanhentunut kirjaus, poistettu käytöstä 27.8.2026.** Rajaus purkautui
  jo 21.8.2026 yllä olevan kohdan mukaan, mutta tämä rivi jäi tiedostoon sanomaan päinvastaista
  ja vaati vielä `Ctrl+U`-kaappausta. Sama väite oli myös `ChatParser`in dokumentaatiossa, ja
  molemmat korjattiin samalla kertaa.

  **Miksi tämä kirjataan eikä pyyhitä pois.** Tiedosto oli ristiriidassa itsensä kanssa
  kuuden päivän ajan, ja ristiriita maksoi työtä: sen varaan kirjoitettiin 27.8.2026
  mittaussuunnitelma jonka ensimmäinen kohta oli jo mitattu. Vanhentunut avoin kohta on siis
  kalliimpi kuin puuttuva kirjaus, koska se näyttää tekemättömältä työltä.

  `ChatParser` lukee tekstisolmut ensimmäiseen elementtiin asti, jolloin kumpikin muoto
  kelpaa, ja testi väittää molemmat. **Lukutapaa ei muutettu**, koska se on oikein myös
  suljetulle lohkolle eikä muutos toisi mitään; pikaviestisivu tarvitsee sen yhä.
- **Toiminnot ovat GET-pyyntöjä kyselyparametrilla**, esim.
  `/bg/move/5302842/561?submit=Roll+Dice`. Polun tilatunniste muuttuu askeleittain
  (541 -> 561), eli siirtosarja on osoitteita eikä lomakepostauksia. Sivu latautuu joka
  klikkauksella uudelleen, joten lautalogiikkaa ei ole selaimen puolella lainkaan.
- Lautasivulla näkyvät: pistenumerot 13-24 ylhäällä ja 12-1 alhaalla, molempien pelaajien
  pip-luku ja pistetilanne, ottelun pituus, kuutio, nopat sekä painikkeet `Roll Dice` ja
  `Double` ja valinta `Verify Double`. Lisäksi linkit `<<Review Game` ja `Skip Game`.
- DailyGammonissa on **koneen arvaus**: sivusto arvaa pelaajien siirtoja nopeuttaakseen
  otteluita. Lauta voi siis palautua taaksepäin (`Your opponent made an unexpected move,
  and the game has been rolled back to that point`). Tallennettu lautatila ei ole
  lopullinen totuus.
- **Tuplauksen vastaus on heti oma heittovuoro, ja otto on arvaus** (mitattu 15.9.2026
  `sessio-15-9-ilta2/0035` ja 16.9.2026 `sessio-16-9-yo/0052`, Tommin sääntö samana yönä:
  sivusto arvaa aina oton, ei koskaan passia). Vastaussivu on tavallinen lautasivu kuutio
  vastustajan solussa ja nopat heitettynä, ilman arvaussivun otsikkoa ja ilman
  peruutusilmoitusta, joten sivulta ei voi lukea että lauta on ehdollinen. Pelillinen
  seuraus `SUBSTANSSI.md` kohta 105.
- **`Reminder`-elementit eivät ole sivuston omia** vaan Tommin selainlaajennuksesta. Ne
  näkyvät kuvakaappauksissa mutta eivät OkHttpilla haetussa HTML:ssä. Älä jäsennä niitä.

Lautasivusta (`/bg/move/<id>/<tila>`, kaapattu ja jäsennetty 29.7.2026):

- **Lauta on koneluettava `ALT`-attribuuteista.** Jokaisen pisteen kuvalla on ALT joka
  koodaa tilan: `y3` kolme keltaista, `b1` yksi sininen, `_` tyhjä. Kuvatiedostojen nimiä
  ei tarvitse tulkita.
- Pisteen numero luetaan **sivun omasta numerorivistä**, ei kuvan suunnasta eikä taulukon
  geometriasta. Numerot ovat omalla `<TR>`:llään kuvarivin ylä- tai alapuolella, ja
  sarakeindeksi yhdistää numeron kuvaan. Aiempi sääntö (`pt_..._down` = 13-24 vasemmalta)
  oli voimassa 29.7.-1.8.2026 ja **osoittautui vääräksi**: se ei kestä käyttäjäasetusta
  `Home boards on left side`, ks. alempaa. Fallbackia vanhaan ei jätetty, koska arvattu
  pistenumero on tässä pahempi kuin puuttuva.
- **Siirto on linkki:** `?move=<kirjain>`, jossa kirjain on lähtöpiste, `a`=1 ... `x`=24.
  Havainto on varmistettu neljällä pisteellä. **Silti: älä rakenna osoitetta itse.** Koodi
  lukee kirjaimen sivun linkistä, ja `BoardParserTest` vahtii oletusta erikseen. Väärä
  arvaus ei tuottaisi virhettä vaan väärän siirron oikeassa ottelussa.
- **Siirto kootaan kasvavaan merkkijonoon saman tilatunnisteen alla** (mitattu 3.8.2026,
  21 oikeaa siirtoa viidestä ottelusta, `raakasivut/dailygammon.com.har`):

  ```
  ?move=w -> ?move=wh -> ?move=whf -> ?move=whff -> ?move=whff&submit=Submit+Move
  ```

  Polku pysyy samana koko kokoamisen ajan (yllä `/2360`), ja kasvava osa on `move`.
  Kirjaimia on **yksi per siirretty nappula**: kaksi tavallisella heitolla, **neljä
  tuplaheitolla** (nähdyt `whff`, `rpnl`, `yffw`, `yffi`). Toistuva kirjain tarkoittaa kahta
  nappulaa samalta pisteeltä. `Submit Move` liitetään samaan kertyneeseen osoitteeseen.
- **Kuutio menee yhtenä pyyntönä, ei kahtena.** `?submit=Double&verify=Double` ja
  `?submit=Accept&verify=Accept`. Lautasivun `Verify Double` ei siis ole erillinen
  vahvistusaskel vaan sama pyyntö. Lähde nähtiin 3.8.2026: se on tavallinen lomake, jossa
  `Roll Dice` ja `Double` ovat saman `name=submit`-kentän arvoja ja `Verify Double` on
  `name=verify` -valintaruutu.
  **Tarkennus 3.9.2026: vahvistamaton pyyntö ei ole äänetön.** `?submit=Accept` ilman
  `verify`ä palauttaa saman laudan, ja laudan alle tulee punainen rivi
  `<FONT COLOR=#cc0000>Previous move not verified!</FONT>` (`sessio-3-9-ilta/0043`, fixture
  `move_accept_not_verified.html`). Sama rivi on kolmessa aiemmassa `Double`-vastauksessa
  (`sessio-28-8/0029`, `sessio-31-8/0034` ja `0035`), jotka oli luettu *"sivusto palautti
  saman laudan"*: rivi on elementti eikä paljas tekstisolmu, joten se putosi
  `BoardParser.notices`ista. Koko korpuksessa väriä `#cc0000` käyttää vain kaksi lausetta,
  tämä ja asetussivun `Profile Successfully Updated!`, eli se on sivuston ilmoitusväri.
  `Decline` menee ilman `verify`ä läpi (6 kertaa 3.9.2026), koska ruudun arvo on `Accept`.
  **Yhden pisteen ottelussa `Double`a ei tarjota** (mitattu 9.9.2026 koko korpuksesta).
  Sivuja joilla lukee täsmälleen `1 Point Match` on 49, ja niistä yhdellä Tommi on
  heittovuorossa (`sessio-3-9-paiva/0172`, `Roll Dice` tarjolla). Sillä sivulla lomakkeessa on
  vain `Roll Dice`, ei `Double`a eikä `verify`-ruutua, kun 5 pisteen ottelun vastaava sivu
  (`sessio-1-9-ilta/0023`) tarjoaa kaikki kolme. Kuutio piirretään silti: kaikilla 49 sivulla
  on `cube1.gif` keskisolussa, eikä `cube2`, `cube4` tai `cubedr` esiinny kertaakaan. Kuutio
  on siis laudan koriste eikä pelimekaniikka, ja tämä sulkee `LUEMINUT.md`:n `yo4`-luvun
  todentamattoman kohdan. Aiempi haku `1 Point Match` osui myös `21 Point Match`iin, joten
  luku on laskettu numeroa edeltävällä ei-numerolla.
- **DR-ottelun `Pending Replay` on linkkirivi, ei tilateksti** (mitattu 9.9.2026, 78 sivua).
  Muoto on `<br>Pending Replay: <a href="/bg/game/<ottelu>/0/<siirto>">_1_</a>`, ja linkin
  teksti on 72 sivulla `_1_` ja kuudella `_5_`. Kohde on `/bg/game/`-osoite eikä `/bg/move/`,
  eli katselusivu. Mitä rivi tarkoittaa pelillisesti, on `SUBSTANSSI.md`:n kohdassa 17.
  **Linkin kohde on haarautumishetki** (Tommi 9.9.2026 selaimesta): `/bg/game/5311332/0/1012`
  avaa ottelun 5311332 aseman siirrossa 1012, siinä kohdassa jossa vastustaja hyväksyi DR:n.
  Siirtonumero on siis haaran alku eikä nykyinen siirto (sivu itse oli `/bg/move/5311332/2649`),
  ja `0` on osoitteen keskiosa samassa muodossa kuin `/bg/game/<id>/<n>/list`-osoitteissa.

  **Rivi on ehdollinen eikä DR-ottelun vakio-osa** (mitattu 9.9.2026 illalla koko korpuksesta,
  395 DR-sivua). Rivi on **78 sivulla ja puuttuu 317:ltä**, ja ratkaiseva on se että sama
  ottelu esiintyy molemmissa tiloissa: 5311332 on 71 kertaa rivin kanssa ja 156 kertaa ilman.
  Rivi on siis **jonon näkymä eikä ottelun ominaisuus**, ja se tulee ja menee ottelun
  edetessä. Siirtonumeron mukaan järjestettynä jono vaihtelee näin (yksi odottava haara tai
  ei yhtään): 902–922 yksi, 926–1342 ei, 1344 yksi, 1395–1705 ei, 1706–1759 yksi, 1918–2375
  ei, 2377–2622 yksi, 2723 ei.

  **Miksi tämä mitattiin.** Tommi katsoi 9.9.2026 illalla ottelun 5311332 sivua 2723
  selaimella eikä nähnyt riviä lainkaan, eikä syy ollut tiedossa. Mittaus vastaa: jono oli
  tyhjä, koska 2377–2622 välillä odottanut haara oli pelattu. Selain ja sovellus näyttivät
  siis saman oikean asian. **Seuraus jäsentimelle: rivin puuttumista ei saa lukea
  jäsennysvirheeksi eikä puuttuvaksi tiedoksi, vaan tyhjäksi jonoksi.** Sama koskee kirjettä
  toiselle toteuttajalle, jossa luku 78 yksin antaisi väärän kuvan.
- **Nopat kuuluvat sille jonka vuoro on, ja se on luotettavampi tunniste kuin pip-luku**
  (mitattu 1.9.2026 koko kaapatusta korpuksesta). Niillä 745 lautasivulla joilla on oma
  askel tarjolla ja pisteluvut eroavat, noppien omistajaväri on **poikkeuksetta** sama kuin
  pisteluvuista päätelty oma väri. Nolla ristiriitaa.

  **Miksi tämä kirjattiin.** Pip-luvuista päättely kaatuu tasapeliin, jota korpuksessa on 53
  sivulla, ja kaatuminen on hiljainen: se ei tuota virhettä vaan uskottavan väärän istuimen.
  Nopat eivät kanna skeeman väripalettia mukanaan toisin kuin nimisolun `BGCOLOR`, joten ne
  kelpaavat ratkaisijaksi ilman skeemakohtaista taulukkoa (`docs/AVOIMET.md`).

  **Rajaus on osa havaintoa:** tämä koskee sivuja joilla oma askel on tarjolla. Vastustajan
  vuoron sivuista mittaus ei sano mitään, eikä sääntöä saa käyttää niihin.
- **Alempi pelaajapaneeli on aina katsoja itse** (mitattu 1.9.2026, 1658 lautasivua, nolla
  poikkeusta omassa liikenteessä). Kymmenen poikkeavaa sivua ovat toisen tunnuksen
  (`ackammon`) istunnosta, jossa katsoja on eri ihminen; ne eivät siis kumoa sääntöä vaan
  nimeävät sen ehdon. Sääntö kantaa sekä nappuloiden värin että istuimen päättelyn.
- **Päättynyt ottelu jää jonon kärkeen kunnes se kuitataan, eikä `Skip Game` kuittaa sitä**
  (mitattu 1.9.2026). Ottelun 5304226 päättymissivu palautui saman session aikana viisi
  kertaa tavulleen samana, ja jokainen `/bg/nextgame?skip=<jokin muu ottelu>` toi sen
  takaisin. Kuittaus on sivun oman lomakkeen POST (`commit=1` ja `submit=Next Game` tai
  `To Top`); vasta se poisti ottelun sekä jonosta että Top Pagen luettelosta. `Skip Game`
  ohittaa sen ottelun jossa ollaan, ei päättynyttä, joten se pidentää silmukkaa eikä katkaise
  sitä.
- **Päättymissivu voi kantaa vastustajan viestin ilman vastauskenttää** (mitattu 7.9.2026,
  `raakasivut/sessio-7-9-yo/0062`, fixture `match_over_says.html`). Kun vastustaja kirjoittaa
  ottelun viimeisellä vuorollaan, oman viimeisen siirron vastaus on päättymissivu jolla on
  `<b><i>nimi says:</i></b>`, sulkematon `<PRE>` ja lomake jossa on vain `commit`, `Next
  Game` ja `To Top`. Kenttää ei ole, koska ottelu on ohi. Se on neljäs muunnelma kolmen
  aiemman rinnalle (pelkät linkit, kenttä ilman viestiä, napit ilman kenttää), ja se oli
  ainoa jota sovellus ei lukenut: chat-lukija vaati kentän ja palautui ennen arkistointia.
  Korjattu samana yönä (`ChatOnBoard.form` null-sallittu).

  **Viesti katosi suorasta osoitteesta muttei jonosta.** Sama sivu haettiin kolmesti: siirron
  vastauksena (`0062`, viesti mukana), otteluluettelosta suoralla osoitteella 28 sekuntia
  myöhemmin (`0063`, 1590 tavua, ei viestiä, `Next Game>>` linkkinä) ja `/bg/nextgame`sta
  (`0064`, tavulleen sama kuin `0062`, viesti mukana). Suora osoite kulutti siis viestin,
  mutta jonon kärki kantoi sen yhä siihen asti että `commit` lähti. Jono ja suora osoite
  ovat eri lähde samalle sivulle, ja vain jono on se jota lukuhetki ei tyhjennä ennen
  kuittausta.
- **Kuution kuva kertoo sekä arvon että omistajan, ja arvo ei aina ole luku** (mitattu
  9.8.2026, kuusi ottelua, `CubeCaptureLiveTest`). Tiedostot ovat `cube1.gif` (29x29),
  `cube2.gif` (29x39) ja `cubedr.gif` (29x29), ja ALT on tiedoston oma merkintä: `1`, `2`,
  `dr`. Kuvassa `cubedr.gif` lukee kirjaimet `DR`, eikä sivu selitä sitä missään.

  Omistajuus on **solun `VALIGN`**: omistamaton kuutio istuu keskimmäisessä solussa
  (`middle`, samalla rivillä noppien kanssa) ja omistettu ylimmässä (`top`), joka on
  omistamattomalla sivulla tyhjä. Kolmas paikka (`bottom`) on rakenteessa olemassa muttei
  vielä nähtynä.

  **Korjaus 31.8.2026: `bottom` on nähty, ja kuvatiedostoja on neljä eikä kolme.** Molemmat
  täydennykset tulevat samasta mittauksesta, joka tehtiin koko kaapatusta korpuksesta eikä
  kuudesta ottelusta. Neljäs tiedosto on `cubedrs.gif` (29x39), jonka ALT on `dr` niin kuin
  `cubedr.gif`inkin. **Korjaus 3.9.2026: ei aina.** Koko korpuksesta mitattuna `cubedrs.gif`
  kantaa ALTia `dr` 34 sivulla ja `cubedrs` 13:lla (`sessio-3-9-ilta/0045`, fixture
  `move_cube_drs_alt.html`), eli sama kahtiajako kuin `cube2.gif`illä (`2` 395, `cube2` 174).
  Laji luetaan siksi tiedostonimestä (`Cube.doubleRepeat`), kuten arvo jo 10.8.2026 alkaen;
  ALTista luettuna kuutiossa luki laitteella `CUBEDRS`.

  **Kuvan korkeus koodaa omistajuuden, ja se on toisinto solun `VALIGN`ista.** Keskellä oleva
  kuutio on aina 29x29 ja omistettu aina 29x39, sekä numeroituna että DR:nä:

  | kuva | koko | solun `VALIGN` | osumia korpuksessa |
  |---|---|---|---|
  | `cube1.gif` | 29x29 | `middle` | 576 |
  | `cube2.gif` / `cube4.gif` | 29x39 | omistettu | 252 / 15 |
  | `cubedr.gif` | 29x29 | `middle` | 120 |
  | `cubedrs.gif` | 29x39 | `top` 32, `bottom` 6 | 38 |

  Poikkeuksia ei ole yhtään. `s`-pääte on siis **omistetun kuution korkea muoto eikä oma
  kuutiotila**, ja numerokuutioilla sama ero kulkee arvossa: keskellä oleva kuutio on aina 1,
  joten `cube1.gif` on koko sarjan ainoa matala kuva. Sivu kertoo **lajin ALTissa ja
  omistajuuden solussa**, eikä kuvan nimeä tarvitse lukea kumpaankaan.

  Tämä ei muuta jäsennintä: `Cube.label` on ALT sellaisenaan ja omistajuus luetaan jo
  `VALIGN`ista. Fixturet kattavat molemmat DR-muodot, `move_cube_dr.html` keskellä ja
  `move_borne_off.html` omistettuna.
- **Crawford-peli on luettavissa sivulta, ja merkkejä on kaksi yhtä aikaa** (mitattu
  14.8.2026 koko kaapatusta korpuksesta, 648 lautasivua, joista 39 on Crawford-pelistä).
  Johtajan pistemäärän perässä on tähti (`score: <B>6*</B>`), ja **kuutiokuva puuttuu
  sivulta kokonaan**: solu jää paikalleen tyhjänä
  (`<TD BGCOLOR=#999999 ALIGN=center VALIGN=middle>&nbsp;</TD>`), joten laudan rakenne ei
  muutu. Merkit olivat samaa mieltä kaikilla 39 sivulla, ja loput 609 kantavat kuutiokuvan
  eikä yhdessäkään ole tähteä. Erimielistä sivua ei ole yhtään.

  **Tähti ei tarkoita pistettä vaille voittoa vaan sitä yhtä peliä.** Korpuksessa on 19
  sivua joilla joku on `n-1`:ssä mutta kuutio on tallella eikä tähteä ole, eli Crawford on jo
  pelattu. Sama ottelu näyttää molemmat vaiheet itsessään: 6-3 ilman kuutiota tähden kanssa,
  ja seuraavassa pelissä 6-4 kuution kanssa ilman tähteä, jolloin kuutio ehti arvoon 2.
  Fixturet `move_crawford.html` ja `move_after_crawford.html` ovat tuosta parista.

  **Double repeat luopuu Crawford-säännöstä, ja se näkyy tässäkin mittauksessa.** Korpuksen
  DR-ottelu on `n-1`:ssä (4-10 yhdestätoista) ja kantaa silti `cubedrs`-kuution eikä siinä
  ole tähteä. Sivuston oma `dr.html` sanoo saman, joten DR-ottelussa ei pidä odottaa
  kumpaakaan merkkiä eikä sitä saa käyttää tämän mittaamiseen.

  **Kanoni etsi tätä väärästä paikasta.** Avoin kohta nimesi erottimeksi lomakkeen nappirivin
  eli sen ettei `Double` ole tarjolla Crawford-pelissä, ja se jäi mittaamatta: yhdelläkään
  39 sivusta ei ole heittopäätöstä, eli `Roll Dice` ei tullut Crawford-pelissä vastaan
  kertaakaan. Hypoteesi on siis yhä todistamatta, eikä sitä enää tarvita.
- **Korjattu 4.8.2026: `verify`-ruudun arvo on sivukohtainen vakio eikä seuraa nappia.**
  Tässä luki 3.8.-4.8.2026 että `?submit=Decline` tulee ilman `verify`iä. Sessiossa mitattu
  osoite oli `?submit=Decline&verify=Accept`, ja lomake kertoo miksi: tuplausta tarjoavalla
  sivulla on kaksi nappia (`Accept`, `Decline`) ja **yksi** valintaruutu jonka arvo on
  `Accept`. Heittosivulla sama ruutu on `Double`. Arvo on siis sivun myönteinen toiminto,
  ei painettu nappi. Vanha sääntö oli päätelty otoksesta jossa ruutu sattui olemaan
  rastittamatta, eikä sitä ollut luettu lomakkeesta. Fixture `move_cube_offered.html`.
- **Heittosivu jää väliin kun sillä ei ole valittavaa** (mitattu 6.9.2026 neljästä
  sessiosta, `raakasivut/LUEMINUT.md` › *sessio-6-9-ilta4*). Sivu jolla on vain
  `Roll Dice` -nappi ilman `Double`a ei tarjoa mitään päätettävää, ja `Skip all automatic
  pages` ohittaa sen: valinnattomia heittosivuja oli 14/30 ja 8/19 kun rasti oli pois, ja
  2/70 ja 0/10 kun se oli päällä. Sama ottelu (5318269, Crawford) luettiin molemmilla
  asetuksilla, ja ero näkyi siinä.

  **Ohitus ei ole ehdoton**, ja se sanotaan tässä koska luku kaksi on mitattu eikä nolla:
  kaksi läpi tullutta valinnatonta sivua eivät olleet Crawford-pelejä. Sääntö kattaa siis
  ainakin tilanteen jossa tuplaus on sääntöjen mukaan mahdoton, muttei kaikkea.
  Toinen tunnistettu tapaus 11.9.2026: kuutio vastustajan hallussa (5312677, `cube2.gif`
  vastustajan puolella), viisi `Submit Move`a peräkkäin ilman `Roll Dice`a
  (`raakasivut/LUEMINUT.md` › *sessio-11-9-yo*).

  **`Skip Opponent's "Roll Dice" pages` -rastin vaikutusta ei ole nähty lainkaan.** Neljä
  sessiota kattaa kaikki neljä rastiyhdistelmää, eikä vastustajan heittosivua ole tullut
  kertaakaan. Rastin nimi lupaa sivun jota tämä tunnus ei näytä saavan.

- **Pelin päättyminen kesken ottelun on oma sivunsa ilman lautaa** (mitattu 6.9.2026,
  `raakasivut/sessio-6-9-ilta3/0080`, fixture `game_over_no_board.html`). Kun tuplaus
  hylätään (`?submit=Decline`), vastaus on 1622 tavun sivu jolla on tulosrivi
  `andersm wins 1 point.`, pistetaulukko, lomake jonka ainoa nappi on `Next` sekä linkit
  `<<Review Game` ja `Skip Game`. Lautaa ei ole lainkaan.

  **Kaksi eroa ottelun päättymissivuun**, ja molemmat merkitsevät lukijalle: tulosrivistä
  puuttuvat sanat `and the match`, ja napin teksti on `Next` eikä `Next Game`. Muoto on
  muuten sama, joten sama jäsennin lukee molemmat (`MatchOverParser`), ja laji luetaan
  tulosrivin sanoista.

  **Sivu palautuu jonon kärkenä muillekin siirroille.** Samassa sessiossa kaksi eri otteluun
  tehtyä `commit`ia sai vastaukseksi juuri tämän sivun, eli sivusto tarjoaa jonon kärkeä
  uudelleen kunnes se kuitataan. Yksi lukematon sivu tukkii siis jonon eikä vain yhtä
  ottelua.

  **Tarkennus 11.9.2026: tämä sivu on automaattisivu, ja oletusasetuksilla sitä ei näy.**
  Korpuksen 22 `Decline`-vastauksesta 21 on suoraan seuraavan pelin tai ottelun lautasivu,
  ja ainoa laudaton päättymissivu on `sessio-6-9-ilta3/0069`, jossa `Skip all automatic
  pages` (rasti 5) oli pois. Rasti 5 päällä hylätty tuplaus palauttaa uuden pelin laudan
  noppa valmiiksi heitettynä (`sessio-11-9-paiva/0093`). Jäsennin pysyy, koska sivu on
  olemassa rasti pois, mutta oletuspolku on lautasivu.

- **Pakkosiirto on sivustolla oma sivunsa, ei valintasivu** (mitattu 2.9.2026 koko
  kaapatusta korpuksesta, `PakkosiirtoKorpusTest`). 478 erillisestä asemasta joilla oma
  askel on tarjolla **yhdessäkään** eivät kaikki lailliset vuorot pääty samaan asemaan;
  sivusto ei siis pyydä pelaajaa kokoamaan siirtoa jossa ei ole valintaa. Sen sijaan on sivu
  jolla kehote on vakiorivi *Please select your action/make your move:*, siirtolinkkejä ja
  `Undo Move`a ei ole, ja lomakkeen ainoa nappi on **`Submit Forced Move`**; lomakkeen
  tunniste on jo seuraava (`/bg/move/5316601/97` sivulla joka haettiin tunnisteella 96).
  Nähty kerran, toisen tunnuksen istunnossa (`raakasivut/sessio-27-8-chat/0142`, sininen
  muurilla, nopat 6 ja 2, asema ja pip-luvut samat ennen ja jälkeen eli siirtoa ei ollut).
  ~~Tommin omissa istunnoissa sivua ei ole kertaakaan~~, ja hänen asetuksissaan `Skip all
  automatic pages` on rastittu; ~~**että rasti ohittaa juuri tämän sivun on päätelmä eikä
  mitattu.**~~ **Mitattu 6.9.2026 illalla:** kun molemmat skip-rastit käännettiin pois yhden
  session ajaksi, sivu tuli Tommin omaan istuntoon (`raakasivut/sessio-6-9-ilta2/0048`) ja
  sovellus lähetti `Submit Forced Move`n. **Rasti tunnistettiin seuraavana iltana**
  (`raakasivut/sessio-6-9-ilta3`): kun pois käännettiin vain `Skip all automatic pages` ja
  `Skip Opponent's "Roll Dice" pages` jätettiin päälle, sivu tuli neljä kertaa. Tekijä on
  siis `Skip all automatic pages`, ja tämä on mitattu eikä pääteltyä. Help-sivu vahvistaa vain vastustajan puolen: *if your opponent's play is forced,
  then we go right ahead to your next move.* Sama mittaus on generaattorin
  koko vuoron todennus, ks. `docs/ARKKITEHTUURI.md`, ja vaatimuksen seuraus on
  `docs/AVOIMET.md`:ssä.
- **`Submit Greedy Bearoff` on olemassa** (nähty osoitteena 3.8.2026:
  `?move=<kirjaimia>&submit=Submit+Greedy+Bearoff`), se on **muuttava**, ja sovelluksen on
  tunnettava se viimeistään siinä vaiheessa kun siirtoja lähetetään.

  **Korjaus 8.8.2026:** tässä luki että sivua itseään ei saatu talteen. Se saatiin 4.8.2026
  ja on fixturena `move_greedy_bearoff.html` (`BoardParserFormTest`), eli lause oli neljä
  päivää vanhentunut.

  Sivu myös **kumoaa tässä kirjatun epäilyn** eikä vain täydennä sitä. Tässä epäiltiin että
  osoitteessa olisi numero `move`-merkkijonossa, mikä olisi ristiriidassa säännön
  `a`=1 ... `x`=24 kanssa, ja varaus oli oikea: lukema oli kuvakaappauksesta. Sivun omat
  tavut sanovat `<input type=hidden name=move value=jgcc>`, eli pelkkiä kirjaimia. Yksi sivu
  ei todista ettei numeroa voi koskaan esiintyä, mutta se poistaa tilan "ei tarkistettavissa":
  ainoa näyttö numerosta on kuvakaappaus, ja ainoa näyttö tavuista on sitä vastaan.
- **Vuoron päättävä POST osuu lautaosoitteeseen** (9 kertaa samassa sessiossa). Se on
  chat-lähetys, ks. chat-osio. Siirtoja ei postata koskaan.
- **Polun tilatunniste on luettava, ei laskettava.** Haettu sivu oli `/1497`, mutta sen omat
  linkit osoittivat `/1507`. Sama periaate kuin kirjautumisen `path`-kentässä: seuraa
  linkkiä, älä keksi sitä. Otsikon `Move 579` on eri luku kuin tilatunniste.
  Tunniste muuttuu **toimintojen välissä muttei siirtoa koottaessa**, ks. edellinen kohta,
  ja askel on epäsäännöllinen: yhden ottelun mitattu kulku oli `2336 2337 2341 2342 2345
  2349 2350 2353 2354 2357 2360 2364 2368 2369`.
- Komennot ovat samoja `?move=`-linkkejä mutta tekstinä, esim. Swap Dice
  (`?type=9&move=S`). **Swap Dice on kokoamisen etuliite eikä erillinen toiminto**
  (mitattu 3.8.2026): sen jälkeen kirjaimet kasautuvat `S`:n perään aivan normaalisti
  (`S` → `Sf` → `Sfe` → `Sfe&submit=Submit+Move`). `type`-parametrin merkitys on yhä
  tuntematon, mutta se esiintyy vain ensimmäisessä pyynnössä, ei jatkossa.
- **Pip-luku on jäsennyksen tarkistuskeino.** Laudasta laskettu summa täsmää
  pelaajapaneelin lukuun: kaapatussa sivussa keltainen 99 ja sininen 117, ja paneelit
  sanoivat samaa. Kumpaakin väriä on 15 nappulaa. Sivun kaksi riippumatonta osaa siis
  todistavat toisensa, ja `BoardParserTest` käyttää tätä vahtina.
- **`Skip Game` osoittaa `/bg/nextgame?skip=<id>`**, eli se on kuluttava linkki. Älä seuraa
  sitä automaattisesti.
- **Muurin ALT riippuu paikasta eikä sisällöstä, aivan kuten ulos kannettujen** (mitattu
  5.8.2026 fixtureista). Laudan keskisolussa on `<IMG SRC=/images/2/bar_b1.gif>`, ja **sama
  tiedosto kantaa kahta eri ALTia**: `move_board_oikea.html` ja `move_board_vasen.html`
  sanovat `ALT="b1"`, mutta `move_rollback.html` sanoo `ALT="1"` ilman värikirjainta. Väri on
  siis luettava tiedostonimestä, ja tämä on **kolmas** poikkeus sääntöön "kuvatiedostojen
  nimiä ei tarvitse tulkita", nopan värin ja `off_`:n rinnalla.

  Tässä luki 1.8.-5.8.2026 että muuri käyttää samaa ALT-koodia kuin pisteet. Se piti
  paikkansa siitä yhdestä otoksesta jonka perusteella se kirjoitettiin, ja on nyt mitattu
  vajaaksi: ALTiin nojaava luku olisi menettänyt värin hiljaa siellä missä sitä ei ole.
  Sama vikamuoto kuin ulos kannetuilla, ja se löytyi vasta kun kolmatta fixturea katsottiin.

  Pip-laskenta todistaa lukutavan oikeaksi: pelkistä pisteistä laskettu sininen jäi 137:ään
  kun paneeli sanoi 162, ja erotus on tasan 25 eli yksi nappula muurilla.

  **Termi on muuri**, ei patsas (Tommin korjaus 5.8.2026). Englanniksi `bar`, joka on
  sivuston oma sana ja jää siksi koodin tunnisteisiin.

  **Ei nähty:** miltä muurilta siirtyminen näyttää HTML:nä. Muurin vaikutusta `moves`iin ei
  siksi mallinneta, vaikka pelin säännöissä se on pakollinen ensimmäinen siirto.
- **Käyttäjäasetus `Home boards on left side` peilaa koko laudan, ei vain numeroita**
  (mitattu 1.8.2026, `raakasivut/move_vasen.html`). Sama lauta samalla hetkellä tuotti
  käänteisen pistenumeroinnin, käänteisen nappulajärjestyksen rivin sisällä ja käänteisen
  noppasolujen järjestyksen. Se kaatoi silloisen jäsentimen hiljaisesti: pip-vahti antoi
  peilatusta sivusta 174/201 ja 165/185 kun paneelit sanoivat 138 ja 162.
  **Korjattu samana päivänä** lukemalla numero sivun numerorivistä. Vahtina on fixture-pari
  `move_board_oikea.html` ja `move_board_vasen.html`, jotka ovat sama lauta samalta
  hetkeltä asetuksen molemmilla arvoilla, ja `BoardParserOrientationTest` vaatii niistä
  saman piste-erittelyn. Pari on olemassa vain tätä varten, joten älä korvaa toista
  tuoreemmalla kaappauksella: silloin väite muuttuu tyhjäksi.
- **Nopat näkyvät myös heittoa edeltävällä sivulla**, koska ne ovat edellisestä heitosta
  (mitattu 1.8.2026). Noppien olemassaolo ei siis kerro kenen vuoro on eikä sitä onko
  heitetty. Erottava merkki on **siirtolinkki**: `?move=`-linkkejä on vain silloin kun
  nopat ovat omat ja siirto on tekemättä. Kehote erottaa saman:
  `Please select your action/make your move:` on ennen heittoa,
  `Please make a checker move.` sen jälkeen.
- **Sivulla on kolme viestikanavaa kolmessa eri muodossa**, ja ne on erotettava toisistaan
  (mitattu 1.8.2026, fixture `move_rollback.html` kantaa kaikkia kolmea yhtä aikaa):
  1. `<b>` ennen lautaa: `Please select your action/make your move:`. **Vakiorivi joka on
     joka sivulla samana**, eli se ei kerro tilanteesta mitään.
  2. `<h4>` laudan alla: tilanneteksti, esim. `Please make a checker move.` tai
     `<nimi> declines the cube action.`. Tämä on se joka kertoo missä mennään, ja siksi
     `BoardParser.prompt` ottaa `h4`:n ensin ja `<b>`:n vain sen puuttuessa.
  3. **Paljas teksti bodyn alla ilman omaa elementtiä:** peruutusilmoitus `Your opponent
     made an unexpected move, and the game has been rolled back to that point.`
     Elementittömyys teki siitä näkymättömän jäsentimelle 1.8.2026 asti.
  Ilmoitukset luetaan nyt bodyn suorina tekstisolmuina kenttään `BoardState.notices`, eikä
  tunnettua lausetta etsimällä: lauseeseen sidottu jäsennin ei koskaan löytäisi sitä toista
  ilmoitusta jota ei vielä tunneta. Peruutus saa lisäksi oman lippunsa `rolledBack`, koska
  se on syy epäillä aiemmin tallennettua lautaa eikä pelkkä teksti.
- **Kaikki neljä aiemmin puuttunutta lautatilaa saatiin 4.8.2026** ensimmäisessä
  kokonaisessa pelisessiossa proxyn läpi (337 sivua). Tässä luki siihen asti ettei nappulaa
  ulkona, tuplaheittoa, Submit Move -välitilaa eikä kuution tarjoamista ole nähty HTML:nä.
  Ne ovat nyt fixtureina, ja seuraavat kohdat ovat niistä luettuja.
- **Lomake on jäsentimen toinen puoli, ja se oli katveessa 4.8.2026 asti.** Kokoaminen on
  linkkejä, mutta **jokainen vuoron päättävä ja kuutiota koskeva toiminto on lomakkeen
  nappi**: `Submit Move`, `Submit Greedy Bearoff`, `Roll Dice`, `Double`, `Accept`,
  `Decline`. `BoardParser` luki vain ankkureita, joten ne olivat kaikki näkymättömiä yhtä
  aikaa. Katve ei näkynyt mistään, koska siihenastiset fixturet olivat tiloista joissa
  lomakkeella ei ollut mitään kiinnostavaa. Nyt `BoardState.form`.
- **Sivu kertoo itse milloin siirron voi lähettää.** Kesken kootussa siirrossa (`?move=m`)
  on vain `Undo Move`, ei piilokenttää eikä nappia; täydessä (`?move=mf`, `?move=rrmm`)
  molemmat ilmestyvät. Kirjainten lukumäärää ei siis tarvitse laskea eikä nopista päätellä
  onko siirto valmis. Fixturet `move_assembling.html` ja `move_submit_ready.html`.
- **`Undo Move` tyhjentää koko kokoamisen, ei viimeistä nappulaa.** Osoite on tilatunniste
  ilman `move`-parametria (`/bg/move/<id>/117?`), ja sama sekä kolmen että neljän kirjaimen
  kohdalla samassa ottelussa. Se on lisäksi ainoa lautasivun linkki jonka tunnistaa vain
  tekstistä: osoitteessa ei ole `move=`-parametria, joten `a[href*=move=]` ei löydä sitä.
- **`Submit Greedy Bearoff` on sivuston esilaskema siirto**, ei kokoamisen tulos.
  Piilokentässä on valmis siirto (`move=jgcc`) ilman yhtään `?move=`-askelta, ja sessiossa
  näkyi kuusi peräkkäistä greedy-lähetystä joiden välissä ei ollut kokoamista lainkaan.
  Se yhdistyy myös `Swap Dice`en (`?move=Sgd&submit=Submit+Greedy+Bearoff`).

  **Napin ehto ja esitäytön sääntö mitattu 2.9.2026** (`GreedyKorpusTest`, 98 lautaa joilla
  uloskanto on mahdollinen): esitäyttö on aina vuoro joka kantaa ulos eniten (56/56), ja
  nappi on tarjolla täsmälleen silloin kun sellaisen vuoron loppuasema on yksikäsitteinen
  eikä kontaktia ole. Kontaktittomat uloskantolaudat joilla ahneita loppuasemia on useampi
  (18) ja kontaktilliset (24) tulevat ilman nappia, tavallisena valintasivuna. Sovelluksen
  vastine on `docs/ASETUKSET.md`:n `greedy_bearoff`.
- **Ulos kannettujen nappuloiden ALT riippuu paikasta eikä sisällöstä.** `off_y3_bot` on
  `ALT="y3"` mutta `off_b3_top` on `ALT="3"` **ilman värikirjainta**. Väri on siis luettava
  tiedostonimestä, ja tämä on toinen poikkeus sääntöön "kuvatiedostojen nimiä ei tarvitse
  tulkita", nopan värin rinnalla. ALTiin nojaava luku menettäisi puolet tapauksista
  hiljaa. Fixture `move_borne_off.html`.
- **Pyydetty ottelutunniste ei määrää palautettua ottelua** (mitattu 4.8.2026, kaksi
  osumaa). Haku osoitteesta `/bg/move/<A>/457` palautti sivun jonka lomake ja `Skip Game`
  osoittavat otteluun `<B>`: sivusto siirsi seuraavaan peliin kun edellinen siirto oli
  tehty. Sama periaate kuin `isLoginPage`in taustalla mutta terävämpi: pyydetystä
  osoitteesta ei voi päätellä sivun lajin lisäksi **myöskään ottelua**. Jäsennin kesti
  tämän valmiiksi, koska se lukee tunnisteen sivulta, mutta sääntö kannattaa tietää
  erikseen.
- **Ei nähty vielä HTML:nä:** kuution tarjoaminen **omalta** puolelta, eli sivu jolla
  `Double` on lähetetty ja vastustaja ei ole vielä vastannut.
- **Nuolet merkitsevät edellistä siirtoa** (kaapattu 3.8.2026, fixture
  `move_board_arrows.html`). `arrow_up.gif`, `arrow_left.gif` ja `arrow_down.gif` ovat
  samassa taulukossa kuin pisteet mutta omilla riveillään, ja niiden `ALT` on `A`.
  Jäsennin kesti tämän valmiiksi, koska pisteet valitaan kuvatiedoston nimestä
  (`img[src*=pt_]`) eikä ALTista. **Tämä on syy pitää valinta siinä missä se on:**
  ALT-pohjainen valinta olisi lukenut nuolen pisteeksi.
- **Heittoa edeltävällä sivulla ei ole yhtään `/bg/move/`-ankkuria.** Seuraava tilatunniste
  on **lomakkeen `action`-attribuutissa** (`<form action=/bg/move/<id>/2184 method=get>`),
  koska siirtolinkit syntyvät vasta heiton jälkeen. `BoardParser` luki 3.8.2026 asti vain
  ankkureita, joten `stateToken` jäi tällä sivulla nulliksi. Korjattu lukemalla molemmat.
  Sama sääntö kuin muualla, nyt vain toisessa attribuutissa: seuraa linkkiä, älä laske sitä.
- **Noppien väri on kuvatiedoston nimessä, ei ALTissa.** `die_b5.gif ALT="5"` on sinisen
  viitonen. Tämä on ainoa kohta jossa tiedostonimi kantaa tietoa jota ALT ei kanna, ja siksi
  se on poikkeus sääntöön "kuvatiedostojen nimiä ei tarvitse tulkita".
- **`#3399CC`-hypoteesi (3.8.2026):** korostus näyttää olevan sen pelaajan paneelissa jonka
  **nopat ovat näkyvissä**, eli edellisen siirtäjän. Kolmannessa otoksessa korostettu paneeli
  oli sinisen ja nopat olivat sinisiä, ja pip-laskenta todistaa kumpi pelaaja on sininen
  (156 vastaan 150, samat luvut paneeleissa). Se sopii yhteen aiemman havainnon kanssa, jonka
  mukaan väri oli kahdessa otoksessa eri pelaajalla. **Hypoteesi, ei sääntö:** arvo
  säilytetään yhä raakana, ja `BoardParserArrowTest` väittää vain havainnon eikä tulkintaa,
  jotta se hajoaa jos vastaesimerkki tulee vastaan.

- **Grace kuluu, Time Pool ei kulu sen aikana (mitattu 21.8.2026).** Kanoni on tuntenut
  sarakkeiden **nimet** 3.8.2026 alkaen muttei sitä mitä ne tekevät. Ero mitattiin
  sivutuotteena, kun oman sovelluksen ja DG Mobilen otteluluettelot kaapattiin peräkkäin
  samalta tabletilta. DG Mobilen näkymä oli haettu 14 minuuttia aiemmin eikä ollut
  päivittynyt, ja kahdeksan yhteisen ottelun Grace-lukemat erosivat **jokaisessa tasan 14
  minuuttia** (13:26 → 13:12, 13:31 → 13:17, 13:46 → 13:32, 14:09 → 13:55, 14:59 → 14:45,
  22:44 → 22:30, 22:49 → 22:35, 23:24 → 23:10). Samojen kahdeksan **Time Pool oli
  identtinen** (234:39, 186:38, 186:55, 153:11, 180:33, 450:00, 339:16, 579:21).

  Vakio ero kahdeksassa rivissä sulkee pois kahden sovelluksen eri laskutavan: eri laskutapa
  ei tuota samaa erotusta joka rivillä. Grace on siis jäljellä olevaa aikaa ja laskee
  reaaliajassa, eikä Time Pool liikkunut lainkaan samojen minuuttien aikana.

  **Mitä tämä ei todista, ja se on eri kappaleessa tarkoituksella.** Kaikissa kahdeksassa oli
  oma vuoro ja ikkuna oli neljätoista minuuttia. Yhteensopiva selitys on kaksivaiheinen kello
  jossa Time Pool alkaa kulua vasta gracen loputtua, mutta mittaus ei erota sitä selityksestä
  jossa Time Poolin kuluminen riippuu jostain muusta ehdosta. ~~Erottava koe on sama vertailu
  ottelulla jonka grace on lopussa, ja se on ajamatta.~~ **Ajettu 25.8.2026**, ks. seuraava
  kohta.
- **Grace on tunnit:minuutit, ja tuntikenttä liikkui (mitattu 25.8.2026).** Kanoni on tähän
  asti sanonut yksikön olevan uskottava tulkinta muttei mitattu (`Match.kt`), ja aikakentät
  ovat siksi tekstiä eivätkä kestoja. Nyt yksikkö on mitattu.

  Kaksi lukemaa samasta otteluluettelosta, **12 h 16 min välissä** (25.8. klo 1.16.15 ja
  13.31). Kuusi ottelua putosi täsmälleen kuluneen ajan verran: 12:52 → 0:36, 13:05 → 0:49,
  14:25 → 2:09, 21:02 → 8:46, 21:11 → 8:55, 21:23 → 9:07. Seitsemäs (12:06) oli pohjassa
  `0:00`, mikä on sekin ennusteen mukainen. Minuutit:sekunnit olisi tyhjentänyt jokaisen
  kentän moninkertaisesti, joten se on poissuljettu.

  **Ottelut tunnistettiin Time Poolista eikä nimestä**, koska luettelo oli muuttunut yön
  aikana: jälkimmäisessä lukemassa oli rivejä joiden Time Pool ei vastaa yhtäkään lähtörivin
  ottelua. Ne jätettiin pois, koska tuntematon pari olisi vertailu kahden eri ottelun välillä.

  **Ero 21.8. mittaukseen on ikkunan pituus eikä menetelmä.** Silloin väli oli 14 minuuttia,
  ja se osoitti että Grace laskee reaaliajassa muttei sitä mikä kenttä on mikä: vakio 14
  minuutin erotus näkyy samana riippumatta siitä onko jälkimmäinen kenttä minuutteja vai
  sekunteja. Vasta yli tunnin ikkuna pakottaa **tuntikentän** liikkumaan, ja tässä se liikkui
  kahdellatoista.

- **Time Pool alkaa kulua vasta gracen loputtua, ja se on samaa yksikköä (mitattu 25.8.2026).**
  Tämä on 21.8. nimetty erottava koe. Yhden ottelun Grace saavutti nollan noin klo 13.22
  laskien, ja klo 13.31 sen Time Pool oli pudonnut `136:52` → `136:42`, eli kymmenen kuluneen
  yhdeksän tai kymmenen minuutin aikana. Saman luettelon muiden otteluiden Time Pool oli
  koskematon, ja niillä Gracea oli jäljellä.

  Kaksivaiheinen kello on siis se selitys joka jää voimaan: Grace ensin, sitten Time Pool.

  **Tämä on yhden ottelun havainto eikä seitsemän, ja se on heikompi kuin yllä oleva
  Grace-tulos.** Se erottaa kaksivaiheisen kellon siitä selityksestä jossa kuluminen riippuu
  jostain muusta ehdosta vain sikäli kuin nollaan osunut grace on ainoa muuttunut asia.
  Vahvistus tulee itsestään seuraavalla kerralla kun jonkin ottelun grace loppuu.
- **DG Mobilen näkymä ei päivity itsestään (sama mittaus).** Neljäntoista minuutin aikana
  otteluluetteloon oli ilmestynyt yhdeksäs ottelu, joka näkyi omassa sovelluksessa muttei DG
  Mobilen auki olevassa näkymässä. Sama muoto kuin `docs/UI.md`:n *jonon polku on virta, ei
  kertakuva*, mutta toisesta asiakkaasta mitattuna.

Kohde on pieni vapaaehtoisvoimin pyöritetty sivusto vuoden 2011 Apachella. Pyyntötahti
pidetään kohteliaana: yksi pyyntö kerrallaan ja minimiväli (`DgClient.minRequestIntervalMillis`).

**Palvelin sulkee kierrätetyn yhteyden viidessä sekunnissa** (`Apache/2.2.22`,
`KeepAliveTimeout` oletuksena 5 s; mitattu 8.8.2026 ja uudelleen 14.8.2026 tilapäisellä
lokituksella, joka näytti epäonnistuneen lähetyksen kohdalla `gapMs=5112`). Tämä ei ole
satunnainen katko vaan rakenteellinen: ihmisen tauko kahden painalluksen välissä ylittää
viisi sekuntia lähes aina, joten ilman uusintaa **ensimmäinen pyyntö tauon jälkeen
epäonnistuu joka kerta** ja näkyy käyttäjälle katkenneena verkkona.

Seuraus koodissa: **raja kulkee siinä onko pyynnöllä runko**, ei HTTP-metodissa eikä
kutsupaikassa. Rungoton GET (haku ja lautasivun lomake, joka on `method=get`) uusitaan
OkHttpin `retryOnConnectionFailure`illa, joka uusii vain sen tapauksen jossa kierrätetty
yhteys todetaan kuolleeksi **ennen kuin mitään on lähetetty**. POST (runko) menee kerran ja
epäonnistuu näkyvästi, koska kahdesti lähetetty runko voisi viedä pelin tilaan jota kukaan ei
pyytänyt. Alkuperäinen sääntö *"lomakelähetystä ei toisteta"* (4.8.2026) koski nimenomaan
runkoa, ja tämä on sen tarkennus eikä kompromissi. Vahti on `DgClientRetryTest`.

**Sama ilmiö osui POSTeihin 18.9.2026 kolmesti, ja POST sai oman keinon: ei uusintaa vaan
tuore yhteys.** `Next Game` ja `To Top` ovat POSTeja (`commit`-kenttä), ja katkohistoria
(`connection_drops`) kirjasi ne klo 15.13, 17.34 ja 17.35 paljaana `IOException`ina.
`KitkaLoki` osoitti mekanismin ilman proxya: edellisestä vastauksesta oli 9,7 s (`Submit
Move` 17.34.38,7 → `Next Game` 17.34.48,5) ja 6,4 s (`refresh` 17.35.16,9 → `To Top`
17.35.23,3), ja kumpikin päättyi 20–23 ms:ssa, eli runko lähti yhteyteen jonka palvelin oli
jo sulkenut. Pidemmät tauot (11 ja 18 s) menivät läpi 0,9–1,8 s:ssa, eli silloin OkHttp
huomasi sulkeutuneen yhteyden ja avasi uuden; ikkuna on siis heti viiden sekunnin jälkeen,
ennen kuin sulkeminen näkyy asiakkaan päässä. Korjaus `DgClient.execute`ssa: rungollinen
teko hylkää altaan joutilaat yhteydet (`connectionPool.evictAll`) kun edellisestä
vastauksesta on yli 4 s (`stalePoolAfterMillis`), jolloin POST menee uudella yhteydellä
kerran. Runkoa ei yhä koskaan lähetetä kahdesti, joten 4.8. sääntö pysyy; hinta on
TCP-kättely tauon jälkeen, ja sivusto on http, joten TLS:ää ei ole. Vahti
`DgClientRetryTest` › *tauon jälkeinen rungollinen teko avaa uuden yhteyden*
(`sequenceNumber` 0 toisella pyynnöllä). Todentuu pelissä: `connection_drops` ei saa enää
`Next Game`- tai `To Top`-rivejä `IOException`illa.

**Toinen katkolaji samalla napilla: sivusto ei vastaa POSTiin 20 sekuntiin** (17.9.2026
klo 21.40 ja 18.9.2026 klo 18.41, molemmat `Next Game`, molemmat proxyn takana ja alle kahden
sekunnin tauon jälkeen, proxyn `TimeoutError 10060` 21 s:ssa). Keep-alive ei selitä sitä,
koska tauko oli lyhyt eikä proxy kierrätä yhteyksiä. Kummallakin kerralla Refresh näytti
saman siirtonumeron ja sama POST meni perään sekunnissa, eli pyyntö ei ollut mennyt perille.
Erottelematta on, jäikö yhdistäminen vai vastaus tulematta (`raakasivut/LUEMINUT.md`,
sessio-18-9-ilta). Ilman proxya sama näkyisi sovelluksessa 30 s `readTimeout`in jälkeen.
Tahti 18.9.2026 klo 19.23: 2 jumia 28 proxyllisesta `Next Game`sta kolmessa sessiossa, ja
18 proxytonta ilman jumia (sessio-18-9-ilta2, ei jumia viidestä).

**Nimi 18.9.2026 klo 19.45: pudonnut SYN, eli uusi TCP-yhteys sivustolle ei saa vastausta.**
Paikannus kolmesta todisteesta. Ensin kesto: 21 062 ms on Windowsin SYN-uusintojen summa
(3 + 6 + 12 s, `TcpMaxConnectRetransmissions` 2), kun taas Pythonin oma 30 s aikakatkaisu
sanoisi `timed out` ilman WinError-numeroa. Toiseksi toisto: proxy ohjattuna mustaan aukkoon
(`10.255.255.1`, SYN ilman vastausta) antoi 502:n 21 046 ms:ssa ja saman `WinError 10060`
-tekstin sanasta sanaan. Kolmanneksi `proxy.py` jakaa 18.9. alkaen välityksen vaiheisiin
`yhdistys` ja `vastaus` (`X-Proxy-Error`-otsakkeen alussa) ja kirjaa onnistuneen pyynnön
yhdistysajan otsakkeeseen `X-Proxy-Connect-Ms`; musta aukko kirjautui vaiheeseen `yhdistys`.
Sivuston HTTP-käsittely ei siis ole jumissa, vaan yhteys ei synny. Proxy on tälle alttiimpi
kuin sovellus, koska se avaa joka pyynnölle uuden yhteyden (`Connection: close`), ja
sovellus poolaa; keep-alive-korjauksen (`8f067a4`) jälkeen sovelluskin avaa uuden yhteyden
POSTille yli 4 s tauon jälkeen, joten sama pudotus näkyisi siellä `connectTimeout`in 15 s
jälkeen `SocketTimeoutException: connect timed out` -tekstinä KitkaLokissa. **Mistä SYN
putoaa, ei ole mitattu**: kotireititin, operaattori tai sivuston vastaanottojono ovat kaikki
mahdollisia, ja erottelu vaatisi pakettikaappauksen PC:llä. Kumpikin osuma oli POST heti
GETin perään, mikä voi olla sattuma kahdella tapauksella. Seuraava 502 luetaan vaiheen
nimestä eikä kestosta.

**Pakettikaappaus 18.9.2026 klo 20.25–20.28 vahvisti pudotuksen PC:n ulkopuolelle**
(`raakasivut/sessio-18-9-ilta3/paketit.txt`, pktmon, 138 kättelyä sivustolle). SYN-vahdin
kättely klo 20.28.24 kesti 15 264 ms: PC lähetti saman SYNin viidesti (0, 1, 3, 7 ja 15 s),
neljä ensimmäistä jäivät ilman SYN-ACKia ja viides sai sen 238 ms:ssa. Heti perään avattu
seuraava yhteys uudesta portista sai SYN-ACKin 182 ms:ssa. Toinen pienempi tapaus klo
20.26.14: yksi SYN pudonnut, uusinta sekunnin päästä läpi (1223 ms). Kaikki 138 saivat
lopulta SYN-ACKin, mediaani 212 ms. Uusintojen väli on siis 1, 2, 4 ja 8 s eikä aiemmin
päätelty 3, 6 ja 12; 502:n 21 s on Windowsin luovutus viidennen SYNin jälkeen. Sivusto oli
elossa (SYN-ACK tuli normaalilla viiveellä heti kun SYN pääsi perille), ja pudotus koski
yhtä virtaa kerrallaan: samaan aikaan muut yhteydet kulkivat. Se sopii tilalliseen
laitteeseen matkalla (NAT, palomuuri tai sivuston SYN-jono) paremmin kuin reitin katkoon,
mutta erottelua ei ole tehty. Seuraava koe: kun vahdin kättely venyy yli 1,5 s, avaa
rinnakkainen yhteys uudesta portista; jos se menee läpi uusintojen yhä pudotessa, pudotus on
virtakohtainen.

**Tyhjän jonon vastaus laudan `Next Game`sta ei ole Top Page vaan lause** (mitattu
18.9.2026, `sessio-18-9-ilta2` rivi 36): `There are no matches where you can move.` ja
viisi linkkiä, joista `active matches` kantaa `days_to_view`-parametrin. Ei taulukkoa.
`DgPages.isTopPage` tunnistaa sen samasta linkistä, ja sovellus siirtyy otteluluetteloon
hakematta `/bg/top`ia. Viestijonon `/bg/nextgame` tyhjänä on yhä mittaamatta.

- **Help-sivun kysymysankkuri on osin sulkematon (mitattu 29.8.2026).** Muoto on
  `<a name="X"><h3>Kysymys</h3></a>` useimmissa kohdissa, mutta ei kaikissa: `Who runs
  DailyGammon?` alkaa `<a name="run"><h3>...</h3>` ilman sulkevaa tagia. HTML sallii sen,
  ja Jsoup sulkee ankkurin vasta vastauksen ensimmäiseen `<a>`-alkuun, joten väliin jäävä
  teksti on **ankkurin lapsi eikä rungon**.

  Katve on mitattu eikä arvattu: koko sivun 67 kysymyksestä tämä on **ainoa** ankkuri jonka
  sisään jää tekstiä, ja tekstiä on yksi merkki, avaava lainausmerkki. Ruudulla luki siis
  `Jordan", aka` kun sivulla lukee `"Jordan", aka`. Vika oli näkymätön testeissä, koska
  fixture oli kirjoitettu pelkällä suljetulla muodolla; fixtureen lisättiin sulkematon
  tapaus samalla kun `SiteHelpParser` korjattiin lukemaan ankkurin loppuosa vastaukseksi.

  **Yleistys jota tästä ei saa tehdä:** muut jäsentimet eivät kulje rungon lapsia samalla
  tavalla, joten tämä ei ole koko koodikannan ansa vaan yhden sivun kirjoitusasu. Sen sijaan
  se on muistutus siitä että **typistetty fixture voi menettää juuri sen ansan jota varten
  se otettiin**, ja se on yleinen.

- **Palstan indeksi on kuluvan hetken lista, ja arkisto on oma sivunsa (mitattu 29.8.2026).**
  `/bg/forum2/<palsta>` näyttää vain nykyiset ketjut, ja sivun alalaidassa on kolme linkkiä:
  `Add a New Thread`, `Show Hidden Threads` ja **`Old Threads`** (`/bg/forum2/<palsta>/month`).
  Viimeinen on ainoa reitti vanhempaan, eikä indeksissä ole muuta sivutusta.

  Arkistosivun muoto eroaa indeksistä neljällä tavalla, ja jokainen niistä on mitattu
  molemmilta palstoilta (`raakasivut/forum2_main_month.html`, `forum2_politics_month.html`):
  taulukossa on **neljä saraketta** (Title, #, Poster, **Time**) kolmen sijaan, riveillä ei ole
  `New`-merkkiä eikä `Hide`-linkkiä, otsikko on `Threads created in <kuukausi> <vuosi>`, ja
  taulun perässä on `<H3>Previous Months</H3>` ja linkki jokaiseen kuukauteen muodossa
  `/bg/forum2/<palsta>/month/<vuosi>/<kk>`.

  **Kuukausia oli 272, tammikuusta 2004 elokuuhun 2026**, ja luku on kummallakin palstalla sama.
  Syvyyttä ei siis ole enempää kuin yksi napautus: kuukausisivulta ei tarvitse edetä
  seuraavaan, koska koko luettelo on jokaisella kuukausisivulla.

  Kaksi ansaa jotka näkyivät vasta jäsennintä kirjoitettaessa. **Aikasolua ei voi tunnistaa
  sarakenumerolla**, koska indeksin neljäs solu on `Hide`-linkki; tunnusmerkki on
  linkittömyys, muuten indeksin riveillä lukisi ajaksi `Hide`. Ja **paluulinkkiä ei voi
  tunnistaa polun muodosta**: `Back to Index` on `/bg/forum2/main`, mutta samalla sivulla on
  toisen palstan linkki `/bg/forum2/politics`, joka on täsmälleen samaa muotoa. Tunnistus on
  linkin oma teksti.

  Kuukausisivu täyttää myös indeksin tuntomerkin (siinä on ketjulinkkejä), joten
  `DgPages.isForumArchive` tarkistetaan **ennen** `isForumIndex`iä.

- **Kolme lähetyksen kuittaussivua mitattiin 4.9.2026 illan sessiossa** (Tommin omat teot,
  proxy `raakasivut/sessio-4-9-ilta`). Kaikki kolme olivat siihen asti tilassa *odottaa*,
  ja jokainen niistä on rakenteeltaan eri lajia. Fixturet `ignore_done.html`,
  `invite_sent.html` ja `forum_posted.html`.

  **Sivuutus** (`GET /bg/ignore?changeto=1&user=<id>`) vastaa 1127-tavuisella sivulla, jonka
  koko sisältö on `<P>You are now ignoring <nimi>.` ja navigointipalkki. **Otsikko ei kerro
  lopputulosta**, se on pelkkä `DailyGammon`, joten tunnistus ei voi nojata siihen niin kuin
  `Message Sent` -sivulla. ~~Purkamisen vastausta (`changeto=0`) ei ole mitattu, joten~~
  **Purkaminen mitattiin saman päivän toisessa iltasessiossa** (`sessio-4-9-ilta2/0006`), ja
  se on samanmuotoinen: `<P>You are no longer ignoring <nimi>.`, sama otsikko, ei profiilia
  eikä nappia. **Suunta luetaan siis vain lauseesta**, ja se on ainoa kohta joka sen kertoo.
  Sovellus lukee sivuutuksen onnistumisen yhä profiilin napin vaihtumisesta, ja mittaus
  vahvistaa että reitti on oikea: vastaus itse ei kanna nappia kummassakaan suunnassa.
  Fixture `ignore_undone.html`.

  **Ottelukutsu** (`POST /bg/invite/new`) vastaa 1146-tavuisella sivulla, jonka otsikko on
  `DailyGammon Invitation` ja runko `You have invited <a href=/bg/user/<id>>nimi</a> to a
  match.` Otsikko kertoo lopputuloksen, joten tunnistus on siinä
  (`DgPages.isInviteSentPage`). Sivulla **ei ole `<BODY>`-alkutagia eikä yhtään `<p>`:tä**.

  **Palstakommentti** (`POST /bg/forum2/<palsta>/submitadd/<id>`) vastaa 1400-tavuisella
  sivulla, joka on palstan tavallinen kuori: otsikko on sama `DailyGammon Forum: <palsta>`
  kuin ketjulla ja indeksillä, `H2` on ketjun otsikko ja runko on vakiolause `Thank you for
  your comments.` Tunnistus on siksi linkissä `Review your message.`, jonka osoite **nimeää
  juuri syntyneen viestin** muodossa `/bg/forum2/<palsta>/read/<ketju>/<numero>#<numero>`.
  Uuden ketjun vastausta (`submitnew`) ei ole mitattu.

  **Turnausilmoittautumisen peruutus** (`GET /bg/lounge?action=cancel&event=<id>&userid=<id>`)
  vastaa loungella itsellään, kuten 3.9.2026 oletettiin. Oletus siis piti, ja rivitodiste
  toimii ilman uudelleenhakua.

- **Kuittauslauseen luku ei voi nojata `<p>`:hen** (mitattu 4.9.2026, kolmen sivun jälkeen).
  `SendResultParser.notice` luki runkoa ensimmäiseen `<p>`:hen asti, ja sääntö oli johdettu
  yhdestä sivusta. Sivuutuksen lause on `<P>`:n **sisällä**, joten luettavaa ei ollut, ja
  kutsun sivulla ei ole `<p>`:tä lainkaan, joten luku otti navigointipalkin mukaan. Yhteistä
  kaikille kolmelle on se, että lause on **rungon ensimmäinen tekstirivi kun navigointipalkki
  on poistettu**, ja palkki on aina taulukko.

## Käyttökatkoilmoitus: oma sivu, vain kirjautuneelle, alle kymmenen minuuttia (mitattu 11.9.2026)

Päivittäisen varmuuskopion aikana `GET /bg/top` kirjautuneena antaa HTTP 200 ja seitsemän
rivin sivun: otsikko `DailyGammon Backups`, logo `dglogo.gif`, `DailyGammon is sleeping --
SHH!!` ja kehotus palata puolen tunnin päästä. Ei lomaketta, taulukkoa eikä linkkejä.
Raakasivu `raakasivut/kayttokatko.html`, fixture `site_sleeping.html`.

Kolme mittausta jotka eivät ole pääteltävissä sivusta:

- **Kirjautumaton haku ei saa ilmoitusta** vaan tavallisen login-lomakkeen (`path=top/`),
  curl samalla minuutilla. Ilmoitus tulee siis vasta istunnon tarkistuksen jälkeen, eikä se
  ole login-sivun muunnelma.
- **Kesto oli alle kymmenen minuuttia**, ei sivun lupaama puoli tuntia: 0.16 nukkui, 0.26
  sovellus haki otteluluettelon tavallisesti. Yksi mittaus, ei jakauma.
- **Otsakkeet ovat tavalliset**: `Content-Type: text/html` ilman charsetia, `Pragma:
  no-cache`, ei statuskoodia joka erottaisi katkon. Erottelu on rungossa.

Mitä siitä seuraa koodissa: `DgClient.readBody` lukee sivun ennen kutsupaikan omaa tulkintaa
ja palauttaa `DgResponse.Sleeping`. Järjestys on merkitsevä, koska katkon aikana myös
kirjautumisvastaus on tämä sivu, eikä se ole login-sivu; ilman omaa lajia istunnon uusiminen
näyttäisi onnistuneelta ja palauttaisi ilmoituksen pyydettynä sivuna.

Ei automaattista päivitystä katkon jälkeen (Tommin päätös 11.9.2026, *"Refresh riittää"*).
Verkon paluulle on signaali, nukkumisen päättymiselle ei, ja ajastin olisi arvaus kestosta.
Ruutu kehottaa painamaan Refresh, ja se on koko toipuminen.

Mittaamatta: mitä lomakkeen lähetys (siirto, viesti) saa katkon aikana. Oletus on sama sivu,
ja `Sleeping` kohtelee sitä toteutumattomana tekona kuten `ServerError`, ei epäselvänä kuten
`Offline`. Oletus on nimetty, ei mitattu.

## Palstan viesti: tyhjä rivi säilyy, rivinvaihto ei (mitattu 5.9.2026)

`Preview` uudessa ketjussa (`/bg/forum2/main/new`, vastaus `/bg/forum2/main/submitnew`)
osoitti että sivusto tekee kappaleen tyhjästä rivistä mutta **hylkää yksittäisen
rivinvaihdon**. Peräkkäisille riveille kirjoitettu luettelo valui yhdeksi kappaleeksi, jossa
väliviivat olivat keskellä riviä; sama teksti tyhjä rivi kunkin kohdan välissä tuotti
omat kappaleensa.

Käytännön sääntö: **luettelon jokaisen kohdan väliin tyhjä rivi**, ja rivitys tekstissä
saa olla mitä tahansa, koska sivusto rivittää itse.

`Preview` on turvallinen mittausväline, koska se ei luo ketjua eikä kirjoita mitään: vastaus
on `Preview Comment` -otsikoitu sivu ja sen alla sama lomake uudestaan.
