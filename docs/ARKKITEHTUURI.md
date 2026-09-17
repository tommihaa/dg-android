# Kanta ja sovellus

Moduulien `data` ja `app` sisäinen rakenne ja sen perustelut.

Siirretty `CLAUDE.md`:stä 10.8.2026. Sisältö on ennallaan, vain sijainti muuttui.
Hakemisto ja pyytämättä laukeavat portit ovat yhä `CLAUDE.md`:ssä.


### Paikallinen kokoaminen (design kirjattu 24.8.2026, Tommin päätös)

Siirto kootaan laitteessa ja lähetetään yhtenä pyyntönä, kuten DG Mobile mitatusti tekee
(`docs/TOINEN-ASIAKAS.md`, mitattu 24.8.2026). Verkkokäynnit putoavat murto-osaan ja ruutu pysyy
rauhallisena kokoamisen ajan. `Roll Dice`, `Double`, kuutiovastaukset ja `Submit Move` ovat
edelleen verkkokäyntejä; paikallinen on vain kirjainten kertymä niiden välissä.

**Mekanismi johon tämä nojaa on mitattu, ei päätelty** (`docs/KOHDE.md`, lautaosio):
kirjain on lähtöpiste (`a`=1 ... `x`=24, `y`=muuri), kirjaimet kuluttavat nopat näytetyssä
järjestyksessä yksi per nappula, `S`-etuliite vaihtaa noppajärjestyksen ja on mahdollinen
vain ennen ensimmäistä kirjainta (Swap Dice katoaa kun noppa on käytetty), ja sivusto ottaa
kokonaisen kirjainjonon vastaan yhtenä pyyntönä (DG Mobilen loki, sivuston oma greedy
bearoff -piilokenttä `move=jgcc` ja `move=Sgd`).

**Laillisuus lasketaan täydellä haulla eikä sääntölistalla.** `core-domain` saa
siirtogeneraattorin joka tuottaa kaikki lailliset kokonaiset siirtosarjat asemasta ja
nopista. Napautus kelpaa jos kertynyt sarja on jonkin laillisen sarjan etuliite, ja
`Submit Move` on tarjolla vain kun sarja on kokonainen. Haku kattaa erikoissäännöt
(pakollinen muurilta tulo, molempien noppien pakko, isomman nopan pakko kun vain toinen
mahtuu, uloskannon ylitys vain ylimmältä pisteeltä, syönti) ilman että ne ovat erillisiä
ehtoja joita voi unohtaa.

**Sivun omat linkit ovat generaattorin oraakkeli.** Jokainen lautafixture kantaa aseman,
nopat ja sivuston itsensä laskemat ensimmäisen askeleen linkit. Testi vaatii että
generaattorin ensimmäisen askeleen joukko on täsmälleen sivun linkkijoukko jokaisessa
fixturessa. Sama vertailu tehdään ajossa jokaiselle haetulle laudalle ilmaiseksi, ja
**erimielisyys pudottaa laudan vanhaan reittiin**: napautukset seuraavat sivun linkkejä
kuten ennenkin eikä paikallista kokoamista käytetä sillä laudalla. Vika näkyy silloin
hitautena eikä vääränä siirtona.

**Koko vuoron mitassa generaattori on todennettu pelattuja siirtoja vasten** (mitattu
2.9.2026, `PakkosiirtoKorpusTest`, korpus `raakasivut/`). Oraakkeli sitoo vain ensimmäisen
askeleen, mutta kaapatuissa istunnoissa on myös se mitä sivulle lopulta lähetettiin:
`Submit Move` -vastauksen tiedostonimi kantaa kirjaimet, ja ne liitetään laudan omaan
tilatunnisteeseen. **441 pelattua siirtoa 441:stä on generaattorin vuorojen joukossa**, ja
samoin sivuston 47 `Submit Greedy Bearoff` -esitäyttöä. Luku laskee erillisiä asemia joille
lähetys löytyi, ei sivuja. Se ei todista ettei generaattori tuottaisi myös laittomia vuoroja,
vain että se ei hylkää laillisia; laiton vuoro kaatuisi oraakkeliin ensimmäisellä askeleella
tai sivuston omaan tarkistukseen lähetettäessä. Rajaus on osa mittausta: korpus on yhden
pelaajan omaa peliä, joten harvat asemat (esim. kaksi nappulaa muurilla tuplilla) voivat
puuttua siitä kokonaan.

**Pakolliset askeleet poimitaan valmiiksi asetuksella (2.9.2026, Tommin vaatimus).**
Pakollinen askel on askel joka on jokaisessa laillisessa vuorossa, monijoukon leikkauksena
(`CompositionSession.forcedSteps`). Kun laitteen kytkin `forced_steps` on päällä,
`LocalComposition.begin` poimii ne oraakkelin jälkeen (`prefillForced`), ja pelaaja poimii
loput ja painaa `Submit Move`n itse. Kolme rajausta jotka pitävät tämän samassa
riskiluokassa kuin muun kokoamisen. *Poiminta kulkee saman `step`-reitin kautta kuin
napautus*, ei suoralla asetuksella, jotta `submission`in toisto päätyy varmasti samaan
monijoukkoon. *Tulos on täsmälleen pakollisten askelten monijoukko tai tyhjä*: jos ahne
noppavalinta ei tavoita sitä kummallakaan noppajärjestyksellä, mitään ei poimita. *Lähetys
ei siirry sovellukselle*, koska vuoro ei mitatusti ole koskaan kokonaan pakollinen sivuston
valintasivulla (sivusto hoitaa tiukan pakkosiirron itse, `docs/KOHDE.md`). Korpuksesta
mitattu 2.9.2026: 88 asemassa 478:sta on pakollinen askel, esipoiminta tavoittaa sen
täsmälleen 88 kertaa 88:sta, ja pelattu siirto sisälsi sen 79 kertaa 79:stä.

**Ahne uloskanto poimitaan valmiiksi toisella asetuksella (2.9.2026, Tommin tilaus).**
`CompositionSession.greedySteps` on saman leikkauksen toinen käyttö: kun kontaktia ei ole,
otetaan vuorot jotka kantavat ulos eniten, ja jos niiden loppuasema on yksikäsitteinen,
poimitaan koko vuoro, muuten vuorojen yhteinen osa. `prefillForced` ja `prefillGreedy` ovat
saman yksityisen `prefill(target)`-reitin kaksi kutsua, joten takeet ovat samat: napautuksen
reitti, täsmälleen tai tyhjä, lähetys pelaajalle. `LocalComposition.begin` kokeilee ahneen
ensin ja pakollisen vain jos ahne ei poiminut, koska ahneen tulos sisältää pakolliset aina
kun se ei ole tyhjä. Kontakti lasketaan aseman etäisyyksistä: jokin oma nappula on
vastustajan nappulan takana, ja muurilla oleva vastustaja on takimmainen. Sivuston oma ehto
ja sääntö on mitattu `GreedyKorpusTest`issä, tulokset `docs/ASETUKSET.md`:ssä.

**Kirjainten rakentaminen on tietoinen poikkeus sääntöön "älä rakenna osoitetta itse".**
Pohjapolku ja `submit`-arvot luetaan yhä sivun lomakkeesta. Mutta toisen ja myöhempien
askelten kirjaimia ei ole sivulla mistä lukea, koska sivu ei enää näe välitiloja, joten ne
tuotetaan mitatusta kuvauksesta piste→kirjain. Kuvaus on testivahdittu, ja ensimmäisen
askeleen oraakkeli sitoo sen sivun totuuteen jokaisella laudalla: jos kuvaus pettäisi,
erimielisyys pudottaisi laudan vanhaan reittiin ennen kuin yhtään kirjainta rakennetaan.

**Peruminen ja Swap Dice muuttuvat paikallisiksi.** Palvelimella ei ole kokoamistilaa
ennen lähetystä, joten `Undo Move` on kirjaimen poisto ja asemavedoksen palautus ilman
verkkoa, ja noppajärjestyksen vaihto on `S`-etuliitteen ehto samoin rajoituksin kuin
sivulla. Sivun omaa `Undo`-linkkiä ei kesken paikallisen kokoamisen ole olemassakaan.

**Mitä ruutu väittää ja millä katteella.** Kesken kokoamisen lauta näyttää tilaa jota
sivusto ei ole vahvistanut. Se on rajattu täsmälleen samaan ikkunaan jossa DG Mobile tekee
samoin, kate on että jono rakennetaan vain laillisista sarjoista, ja lähetyksen vastaus on
edelleen ainoa totuus: `Submit Move`n palauttama lauta korvaa paikallisen tilan aina.


### Kanta (`data`, lisätty 30.7.2026)

- **Jono on vain lähtevälle liikenteelle.** `pending_actions` sisältää muuttavia toimintoja
  jotka odottavat yhteyttä. Hakujonoa ei ole eikä saa olla, samasta syystä kuin taustahakua
  ei ole: `/bg/nextgame` kuluttaa itseään.
- **Jonon rivi kuvaa painallusta eikä valmista lähetystä** (muutettu 10.8.2026). Tässä luki
  siihen asti että rivin `path` on sivun linkistä luettu osoite sellaisenaan. Se kelpaa
  linkille muttei lomakkeelle: lomakkeesta lähtevä osoite on `action` ja kentät, ja niistä
  koottu merkkijono olisi kannassa juuri se koottu osoite jota portti kieltää. Rivillä on
  siis `boardPath`, `submit` ja `pendingMove`, eli millä sivulla oltiin ja mitä painettiin.
  `PendingActionDaoTest` vahtii tätä erikseen.
- **Uudelleenyritys hakee sivun ennen kuin lähettää**, ja se on ehto eikä varovaisuutta.
  `DgResponse.Offline` syntyy poikkeuksesta joka voi osua joko ennen sitä kun palvelin luki
  pyynnön tai sen jälkeen, joten verkkokerros ei voi tietää menikö teko perille. Sivu voi
  vastata siihen mihin verkkokerros ei pysty, mutta **vain itsenäisen painalluksen osalta**
  (`BoardState.stillOffers`). Ehto on tiukin mahdollinen, eli null vastaa vain nulliin.
- **Kokoamisesta riippuvaa tekoa ei yritetä uudelleen** (mitattu ja päätetty 10.8.2026,
  Tommin valinta kolmesta). Tässä luki siihen asti että palvelin tyhjentää kokoamistilan kun
  siirto on lähetetty, joten sama `move`-arvo tuoreella sivulla tarkoittaisi että siirto on
  yhä lähettämättä. **Väite mitattiin vääräksi laiteajossa:** koottu siirto ei kestä sivun
  uudelleenhakua lainkaan, vaan nappulat palaavat lähtöruutuihin myös silloin kun mitään ei
  lähetetty. Kontrolli ajettiin ilman katkoa ja tulos oli sama, joten kyse ei ole
  yhteyskatkosta vaan hausta.

  Seuraus osuu juuri siihen tekoon jota varten jono kirjoitettiin. Rivin ratkaisu nojaa
  siihen että sivu haetaan tuoreena, ja juuri se haku tuhoaa sen mitä oltiin lähettämässä,
  joten `Submit Move` ei ole uudelleenlähetettävissä millään ehdolla. Muut
  napit ovat toista lajia: `Roll Dice`, `Accept`, `Decline` ja `Double` ovat itsenäisiä
  painalluksia jotka eivät nojaa kerättyyn tilaan.

  Laji luetaan `pendingMove`sta eikä napin nimestä. Kenttä on sivun oma piilokenttä, joten
  ehto on mitattu sivulta eikä arvattu sanasta, ja se seuraa sivustoa jos napin teksti joskus
  muuttuu. Kokoamisesta riippuva rivi näkyy palkkina, ja 16.9.2026 alkaen saman laudan tuore
  haku poistaa senkin: tyhjentynyt kokoamistila ei kerro teon kohtalosta, mutta tuore lauta
  kertoo. `Discard`-nappi poistui samalla (Tommin tilaus, ks. `docs/UI.md`).

  **`Try again` -nappi poistui kokonaan 8.9.2026** (Tommin päätös, *"Refresh korvaa sen"*).
  Onnistunut laudan haku tyhjentää rivin lähettämättä kun sivu ei enää tarjoa tekoa
  (`BoardViewModel.resolvePending`), ja jos sivu tarjoaa sen yhä, pelaaja painaa nappia
  laudalta itse. Ks. `docs/UI.md`.

  **Yrityslaskuri poistui samalla, mutta vain puoliksi.** `attempts` laski uusinnat, ja ainoa
  uusinta oli se nappi. Kenttä lähti domainista (`PendingAction`) ja sitä kasvattanut polku
  (`ActionQueue.noteFailure`, `PendingActionDao.recordFailure`) poistettiin, mutta **kannan
  sarake jäi**: sen poisto olisi skeemamuutos ja vaatisi migraation kaikkien laitteiden
  kannoille pelkän siisteyden vuoksi. Sarake on nyt aina 0 eikä sitä lueta mistään, joten se
  ei voi valehdella kenellekään. `lastErrorText` sen sijaan jäi käyttöön kokonaan: se
  kirjataan jo siihen katkokseen jonka käyttäjä koki painaessaan.

  Vaihtoehto "ei kirjata jonoon lainkaan" hylättiin, koska se palauttaisi täsmälleen sen
  tilan joka 10.8. näytti pahimmalta: epäonnistunut siirto katosi jäljettömiin. Muistutus on
  arvokas silloinkin kun sitä ei voi toistaa, koska pelaaja voi tarkistaa asian itse.
- **Jono ei lähetä mitään itsestään.** Se on kirjanpitoa, ja teko lähtee vain käyttäjän
  painalluksesta. Perustelu on `DgClient`in uusintarajassa (`Intent.ACT`): kahdesti lähetetty
  siirto on pahempi kuin lähettämättä jäänyt, ja se päätös tehtiin 10.8.2026 ennen jonoa.
- **Ottelulla on kerrallaan enintään yksi rivi**, ja uusi korvaa vanhan. Jos ensimmäinen
  painallus ei mennyt perille, palvelimen tila ei liikkunut, joten toinen painallus koskee
  samaa hetkeä. Kaksi riviä kuvaisivat siis samaa tekemätöntä tekoa kahdesti, ja listan
  pituus kertoisi yritysten määrästä eikä siitä mitä on tekemättä.
- **Kanta on yksi olio prosessia kohti** (`DgDatabase.open`, korjattu 10.8.2026). Roomin
  muutostenseuranta elää olion sisällä, joten kaksi `RoomDatabase`-oliota samaan tiedostoon
  eivät kerro toisilleen kirjoituksista: toisen kautta tehty lisäys ei laukaisisi toisen
  `Flow`ta, ja ruutu jäisi näyttämään vanhaa ilman virheilmoitusta. Vika ei voinut ilmetä
  niin kauan kuin ulos näkyviä osia oli yksi, mutta se ei ollut rakenteen ansiota.
- Jono puretaan vanhin ensin, ja samalla millisekunnilla luodut erottaa juokseva id.
  Järjestys on oikeellisuutta: siirtosarjan tilatunniste kasvaa askeleittain.
- Viestin pääavain on `Message.id`, eli sisältötiiviste. Tallennus on siksi idempotentti,
  ja se on suora seuraus siitä ettei viestiä voi hakea uudestaan. Törmäys käsitellään
  `IGNORE`lla eikä `REPLACE`lla: jälkimmäinen siirtäisi vanhan viestin listan kärkeen.
- `MessageEntity.storedAtEpochMillis` on **saapumisaika tälle laitteelle**, ei lähetysaika.
  Sivuston oma aika on tulkitsematonta tekstiä, joten listan järjestys on saapumisjärjestys.
- `MessageArchive` on ainoa tie jäsennetystä viestistä ruudulle, ja se palauttaa viestin
  vasta tallennuksen jälkeen. Sääntö "kantaan ennen näyttämistä" on siis rakenne eikä
  kommentti: kutsujalle ei ole rajapintaa jolla sen voisi ohittaa.
- **Ei `fallbackToDestructiveMigration`ia.** Tavallinen kehitysoletus, mutta täällä se
  pudottaisi ainoan olemassa olevan kopion viestihistoriasta. Skeema viedään versioon
  (`data/schemas/`), ja muutos vaatii kirjoitetun migraation myös ennen julkaisua.

  **Migraatio 2 → 3 pudottaa taulun, ja se on toistaiseksi ainoa sellainen.** Ehto on
  kirjoitettu auki `DgDatabase`en, ja se on kaksiosainen: `pending_actions`in sarakkeiden
  merkitys muuttui kokonaan, joten siirrettävää ei ole edes periaatteessa, ja taulu on ollut
  **kirjoittajaton koko olemassaolonsa ajan** (tarkistettavissa hakemalla `enqueue`n
  kutsujat). `messages` ei kuulu tämän piiriin eikä koskaan kuulu. Juuri tämä ero on syy
  siihen miksi tuhoava varakaatuminen on poissa: se ei osaa erottaa taulua jonka saa
  pudottaa siitä jota ei saa.
- **Vastustajan nimi on viestin oma sarake** (`messages.opponent`, migraatio 3 → 4,
  16.8.2026). Viestit säilytetään pelaajakohtaisesti (Tommin päätös 15.8.2026,
  `SUBSTANSSI.md` kohta 29), ja ilman tätä saraketta yhteys henkilöön katkeaisi: `matchId`
  on orpo numero sen jälkeen kun ottelu putoaa sivustolta, eli juuri siinä aineistossa jonka
  vuoksi sovellus on olemassa. Toinen vaihtoehto oli otteluiden oma taulu, ja se hylättiin
  koska taulun ainoa tehtävä olisi säilyä sivuston unohduksen yli ja se vaatisi liitoksen
  jokaiseen pelaajakohtaiseen hakuun; nimi viestin omalla rivillä ei tarvitse mitään muuta
  säilyäkseen. Haku on `MessageDao.observeByOpponent` ja `observeOpponents`.

  Kaksi seurausta jotka eivät ole ilmeisiä. Sarake on tarkoituksella `Message.id`-tiivisteen
  **ulkopuolella**, koska tiiviste tunnistaa viestitapahtuman eikä sitä tiesikö sovellus
  kumppanin nimen; muuten jo tallennettujen viestien pääavaimet olisivat liikkuneet sarakkeen
  lisäämisestä. Ja vanhat rivit jäävät `null`iksi eivätkä peri `sender`iä: arvaus näyttäisi
  jälkikäteen mitatulta tiedolta.
- `MessageSource` talletetaan **nimenä**, ei järjestysnumerona: numero sitoisi kannan
  enumin kirjoitusjärjestykseen. Tuntematon nimi luetaan `ANNOUNCEMENT`iksi eikä kaadeta
  lukua, koska väärä laji hukkaa lajittelun mutta poikkeus hukkaisi koko listan.
- **`reminders` on kolmas taulu 23.8.2026 alkaen, ja se on eri lajia kuin kaksi muuta.**
  Rivi on pelaajan itsensä kirjoittama muistiinpano yhden pelin ajaksi, eikä sillä ole
  mitään tekemistä sivuston kanssa: selaimen `Reminder`-lohko tulee laajennuksesta
  (`docs/KOHDE.md`), joten mitään ei haeta eikä lähetetä. `ReminderBook` saa vain DAO:n, ja
  se on sama tapa kertoa kyvystä kuin `FormSender`in puuttuminen näkymämallissa.

  **Tällä taululla on poisto ja `messages`illa ei**, eikä ero ole epäjohdonmukaisuus. Viesti
  on ainoa kopio siitä mitä sivusto ei säilytä. Muistutuksen kirjoitti käyttäjä ja hän voi
  kirjoittaa sen uudestaan, ja rivin poisto on nimenomaan se teko jota häneltä odotetaan kun
  muistutus on hoidettu.

  **Elinkaari on kyselyssä eikä ajastuksessa.** Rivit haetaan aina yhdellä pisteparilla
  (`GameKey`), joten pelin vaihtuessa edelliset lakkaavat näkymästä ilman että mitään
  poistetaan. Siivous ajetaan vain kun käyttäjä kirjoittaa saman ottelun uuteen peliin, ei
  koskaan sivun latauksesta: jäsennetyn pisteluvun nojalla tehty automaattinen poisto veisi
  rivit hiljaa yhdestä väärin luetusta luvusta. Näkyvyys ei siis missään kohdassa riipu
  siivouksesta. Perustelut: `ReminderDao` ja `docs/UI.md`.
- **`marked_positions` on viides taulu 15.9.2026 alkaen, ja sen elinkaari on päinvastainen
  kuin muistutusten.** Rivi on pelaajan kirjanmerkki ottelun jälkeistä analyysia varten:
  peli pistepareina, otsikon siirtonumero ja vapaaehtoinen sana. Se luetaan vasta ottelun
  jälkeen ja pohditaan analyysin tullessa, joten sitä ei siivota pelin eikä ottelun
  vaihtuessa; vain käyttäjän poisto vie rivin. `MarkBook` saa vain DAO:n kuten
  `ReminderBook`, eikä mitään haeta eikä lähetetä. Otteluluettelo lukee koko taulun
  virtana (määrä ja lista), lauta yhden pelin. Perustelut: `MarkedPosition` ja
  `docs/UI.md` › Merkitty asema analyysiin.
- **`messages` on tilikohtainen 16.9.2026 alkaen** (Tommin päätös 15.9.2026, `docs/AVOIMET.md`
  › Useampi tili samalla laitteella). Sarake `account` on tili jonka kirjautuneena laite otti
  viestin talteen, ja jokainen ruudun kysely rajaa sillä; `observeByMatch` ei, koska
  ottelunumero on sivustolla tilikohtainen jo itsessään. Tili on `MessageArchive`n
  parametri eikä rakentajan tila, koska `Sign out` ja toisen tilin kirjautuminen tapahtuvat
  saman prosessin sisällä. Migraatio 9→10 jättää vanhat rivit omistajattomiksi, koska
  tunnukset eivät ole kannassa; ensimmäinen kirjautunut tili ottaa ne (`claimUnowned`,
  `MainActivity`). Muut taulut jäävät yhteisiksi: muistutukset ja ottelumuisti ovat
  ottelunumerolla ja numero on tilikohtainen, merkit samoin, jono tyhjenee lähetyksessä.
  Vientitiedosto (versio 3) nimeää tilin otsakkeessa, ja tuonti (16.9.2026) kirjaa viestit
  sille tilille; muistutukset ja fraasit tuodaan yhteisinä.
- **Room ei näy ulos.** `:app` käyttää `DgData.messageArchive(context)`ia eikä `DgDatabase`a,
  jottei `RoomDatabase` vuoda käyttöliittymäkerrokseen. Kääntäjä valvoo tätä: `:app`illa ei
  ole Roomia luokkapolussa lainkaan.
- **Ottelumuisti `seen_matches` on tiivistelmä sivuista jotka haettiin muusta syystä**
  (Tommin päätös 15.9.2026, *"oma täydentyvä kanta paras"*). Rivi on ottelun numero,
  vastustaja, kierros, pituus ja tapahtuma, ja sen kirjoittavat otteluluettelo (kaikki
  vuorossa olevat) ja lauta (avattu ottelu). Lukija on Matches-välilehden Tournaments, jonka
  sivu nimeää käynnissä olevan ottelun vain numerolla. Vaihtoehto oli oman profiilin haku joka
  listaa kaikki käynnissä olevat, ja se hylättiin lisähakuna. Seuraus: muistettu kierros voi
  olla yhden jäljessä kun vastustaja on pelannut pelin loppuun, ja rivi sanoo silloin
  *waiting for your turn* muistetun tiedon perään. Yhdistäminen on `SeenMatch.mergedWith`,
  jossa null ei pyyhi aiempaa arvoa, koska lauta ei aina tiedä vastustajaa eikä
  kokonaismäärää. Yksi rivi per ottelu eikä poistoa: kasvu on hidasta ja päättyneen ottelun
  numero voi tulla vastaan kaaviossa.

### Sovellus (`app`, lisätty 30.7.2026)

- `network_security_config.xml`: selkokielinen liikenne sallitaan **vain**
  `dailygammon.com`ille, `base-config` pysyy kieltona. Manifestin
  `usesCleartextTraffic="false"` on mukana, mutta API 28:sta alkaen konfiguraatio voittaa sen.
- **Varmuuskopiointi on päällä arkistolle ja pois tunnuksilta** (`allowBackup="true"` +
  `data_extraction_rules.xml` + `backup_rules.xml`). Kaksi eri syytä samaan lippuun oli
  aiemmin sama vastaus, ja 13.8.2026 ne erkanivat: vain toinen syy oli pysyvä, ja se koski
  vain tunnuksia.

  **Sääntötiedostot luettelevat mitä otetaan mukaan, eivät mitä jätetään pois.** Ero ei ole
  tyylillinen. Poissulkeva lista vanhenee hiljaa: seuraava kannan tai asetustiedoston lisäys
  päätyisi pilveen ilman että kukaan päättää siitä, koska oletus olisi mukaan ottaminen.
  Mukaan ottava lista epäonnistuu toiseen suuntaan, eli uusi tallennuspaikka jää kopion
  ulkopuolelle kunnes se lisätään käsin. Se on oikea suunta, koska puuttuva varmuuskopio
  huomataan palautettaessa ja liikaa kopioitu yksityisviesti ei huomata koskaan.

  Kaksi tiedostoa eikä yksi, koska Android vaihtoi muodon API 31:ssä ja `minSdk` on 26:
  `dataExtractionRules` pätee uusilla, `fullBackupContent` vanhoilla. Sama sisältö
  kirjoitetaan siis kahdesti, ja jos ne eroavat, laite valitsee eron mukaan hiljaa.

  Kannasta otetaan mukaan `dg.db` **ja sen `-wal`- ja `-shm`-toverit**, koska Room kirjoittaa
  WAL-tilassa: pelkkä `dg.db` olisi kopio joka ei sisällä viimeisimpiä kirjoituksia, eli
  hiljaa vajaa arkisto juuri niiltä päiviltä jotka kiinnostavat eniten.

  Kaksi eri syytä alkuperäiseen kieltoon, ja vain toinen oli pysyvä.
  - *Tunnukset: tekninen pakko.* `KeystoreKey` pitää avaimen järjestelmän puolella, joten
    varmuuskopioitu salattu salasana palautuisi toiselle laitteelle tavuina joita mikään ei
    avaa. Tässä ei ole harkittavaa.
  - *Arkisto: ehto eikä kielto (kirjattu 7.8.2026).* Aiempi perustelu sanoi, että
    vanhentuneena palautuva arkisto olisi pahempi kuin tyhjä, koska vanha tilanne näyttäisi
    nykyiseltä. Se lepää kokonaan sanan "näyttäisi" varassa, ja juuri se on korjattavissa:
    **arkisto päivää itsensä**, koska uusimman rivin aikaleima on täsmälleen se reuna johon
    palautus katkeaa. Kun näkymä kertoo tuoreimman viestin päivän, vanhentuminen ei ole enää
    hiljaista eikä vastalausetta jää. Ehto siis kuuluu: **arkiston varmuuskopio pysyy pois
    kunnes arkisto näyttää oman reunapäivänsä**, ja sen jälkeen lipun kääntäminen on vapaa
    valinta. Ratkaisu on `johdettu kopio` -kuvion vakiomuoto (`Kaanon/KÄSITTEISTÖ.md` §0.2):
    mitätöijä, ei kielto. **Ehto täyttyi 7.8.2026**, ks. reunapäivä alempana.

    **Päätös tehtiin 13.8.2026: arkisto tulee kopion piiriin** (Tommi). Ehdon täyttyminen
    avasi päätöksen eikä tehnyt sitä, ja se oli auki kuusi päivää. Ratkaiseva puoli oli se
    kumpi menetys on peruuttamaton: laitteen menetys hävittää ainoan olemassa olevan
    historian eikä sitä voi hakea takaisin mistään, koska sivustolla ei ole arkistoa.
    Vastapuoli on todellinen mutta ei peruuttamaton: muiden pelaajien yksityisviestit
    siirtyvät laitteelta Googlen palvelimelle päästä päähän salattuina, eivätkä he ole
    siihen vaikuttaneet. Se on sama altistus jonka arkiston olemassaolo jo tuottaa
    pienemmässä mitassa, ja se oli tiedossa jo silloin kun arkisto päätettiin pitää.

  Kolme rajausta, jotta ehtoa ei lueta väärempänä kuin se on. **Auto Backup ei ole synkkaa:**
  palautus tapahtuu vain asennushetkellä, joten se korjaa laitteen menetyksen muttei tuo
  historiaa kahdelle laitteelle yhtä aikaa. **Se ei myöskään ole kirjautuminen:** Android on
  käyttöjärjestelmä eikä selain, ja Google-tili on laitteella valmiiksi, joten kytkentä on
  pelkkä manifestimäärittely ilman riviäkään koodia ja ilman että käyttäjältä kysytään mitään.
  Ja **yksityisyys ei ole se argumentti joka tämän ratkaisee:** arkistossa on muiden pelaajien
  yksityisviestejä, mutta Android 9:stä alkaen Auto Backup on päästä päähän salattu
  lukitusnäytön tunnusluvusta johdetulla avaimella, joten altistus on todellinen mutta pieni.
  Vienti tiedostoon ei korvaa varmuuskopiota vaan täydentää sitä: kopio kattaa sen menetyksen
  jota ei osattu odottaa, vienti antaa päivätyn kopion jonka käyttäjä siirtää itse. Tuonti
  (16.9.2026) lukee saman tiedoston takaisin ja vain lisää: `MessageArchive.addMissing`,
  `ReminderBook.addMissing`, `PhraseBook.addMissing`. Ks. `docs/UI.md` › Tuontinappi.
- **Arkiston reunapäivä näkyy otteluluettelossa** (7.8.2026). Yksi rivi kertoo mihin asti
  arkisto ulottuu, ja tyhjä arkisto sanoo olevansa tyhjä sen sijaan että rivi jätettäisiin
  pois: puuttuva rivi ja rikkinäinen rivi näyttäisivät samalta. Kolme valintaa on kirjattu
  koodiin perusteluineen, ja ne ovat samaa juurta.
  - Lukema on `storedAtEpochMillis` eli **tämän laitteen kirjoitushetki**, ei viestin oma
    aika. `timestampText` on tulkitsematonta sivun tekstiä, eikä kysymys "mihin asti arkisto
    ulottuu" muutenkaan koske lähetyshetkeä.
  - Tyhjä kanta antaa `null`in eikä nollaa (`MAX` tyhjästä on SQL:ssä NULL). Nolla olisi
    vuoden 1970 päivämäärä, eli tyhjä arkisto väittäisi ulottuvansa johonkin.
  - Päivä ilman kellonaikaa, kuukausi kirjaimin, kiinteä englanti ja laitteen aikavyöhyke.
    Vyöhyke on `ArchiveEdge.formatDate`in parametri, jotta `ArchiveEdgeTest` voi kiinnittää
    sen: sama hetki on eri päivä eri vyöhykkeellä, ja käyttäjälle oikea on hänen omansa.

  Rivi kulkee arkistosta suoraan `TopScreen`ille eikä `TopViewModel`in kautta. Se ei ole
  otteluluettelon tilaa eikä kulje verkon yli, ja näkymämalli pysyy sinä luokkana jonka
  koko sopimus on "hakee vain Top Pagen".
- **`TopViewModel` hakee oma-aloitteisesti vain `DgPages.TOP_PATH`in.** `/bg/nextgame` ei
  esiinny sen luokan koodissa lainkaan, ei myöskään käynnistyshaussa. `TopViewModelTest`
  väittää tämän kirjaamalla jokaisen pyydetyn polun.

  Osoite ei esiinny **haun kohteena** missään, ei myöskään 9.8.2026 lisätyssä
  viestiruudussa: jonon polku luetaan sivun linkistä. Kirjoitettuna merkkijonona se esiintyy
  kahdessa paikassa, ja molemmat ovat valitsimia joilla sivun oma linkki löydetään
  (`a[href*=/bg/nextgame]` kahdessa jäsentimessä), eivät osoitteita joihin mennään. Väite oli
  aiemmin muodossa "ei esiinny koko moduulissa", mikä oli totta vain niin kauan kuin jonoa ei
  haettu mistään.
- Odottavasta kohteesta näytetään ilmoitus, ja **9.8.2026 alkaen siinä on nappi joka vie
  viestiruutuun**. Nappi ei hae mitään: kuluttava teko on viestiruudussa, arkiston vieressä.
  Ehto oli kirjattuna ennen kuin nappi oli mahdollinen, ks. viestiruudun osio.
- Käyttäjälle näkyvä teksti ei lupaa henkilöviestiä vaan sanoo "jotain odottaa", koska
  sivuston lippu ei erottele. Sama varaus näkyy `TopPage.hasMessageNotice`in kommentissa.
- Epäonnistunut päivitys **säilyttää jo näkyvän listan**. Vanhentunut lista on yhä oikeaa
  tietoa siitä hetkestä jolloin se haettiin, virheruutu ei ole.
- `DgResponse.AuthFailed` **poistaa tunnukset**. Muuten istunnon hiljainen uusiminen jäisi
  yrittämään kelpaamatonta tunnusta joka pyynnöllä.
- Uloskirjautuminen poistaa myös keksitiedoston. Pelkkä tunnusten poisto jättäisi istunnon
  voimaan.
- Kirjautumisruutu **kertoo HTTPS:n puutteesta ennen kuin salasana kirjoitetaan**, ei
  jälkikäteen.
- **Yhteyden palaaminen hakee näkymän uudelleen, ja ehto on tiukin mahdollinen**
  (22.8.2026). Kuuntelija on `NetworkAvailability` (`app/Connectivity.kt`) ja ehto asuu
  näkymämalleissa (`TopViewModel.onNetworkAvailable`, `BoardViewModel.onNetworkAvailable`).
  Uusi haku tehdään **vain** jos edellinen kaatui verkkoon; palvelinvirhe ja `NotATopPage`
  jäävät ulkopuolelle, koska kumpikaan ei korjaannu yhteyden palaamisesta.

  Kuuntelu on **sidottu koostumukseen eikä sovellukseen**, joten taustalle jäänyt näkymä ei
  hae mitään ja pyyntöjä syntyy vain sinne mihin käyttäjä katsoo. Haku tulee heti eikä
  viiveellä, koska ehto on jo tiukka.

  **Odottavaa tekoa ei yritetä uudelleen tästä.** Jonossa oleva teko on peruuttamaton, ja sen
  uusiminen on käyttäjän päätös (`retryPending`) eikä verkon paluun seuraus. Sitä vahtii oma
  testi. Tämä on myös se raja jota ajastettu pollaus rikkoisi: *"mitään ei tehdä itsestään,
  jokainen pyyntö on käyttäjän ele"* (`BoardViewModel`) saisi ensimmäisen ajastetun
  poikkeuksensa, joten pollaus olisi kaanonimuutos eikä pelkkä toteutus. Tommin peruste
  24.8.2026: *"haluan tehdä pelaajalle tiedoksi mistä on kyse enkä siis halua kuormittaa
  dailygammon-palvelinta."*
- **Sovelluslukko on pois päältä, ja se on vakio eikä asetus** (`AppLock.kt`,
  `APP_LOCK_ENABLED`, Tommin päätös 21.8.2026: *"laitetaan se toistaiseksi pois päältä, se on
  vaan tiellä"*). Asetus vaatisi oman ruutunsa, oman tallennuksensa ja oman päätöksensä
  oletuksesta, eli eri kokoisen työn kuin mitä pyydettiin; *toistaiseksi* on se sana joka
  tekee vakiosta oikean muodon. Takaisin päälle vaihtamalla lippu `true`ksi.

  **Hinta ei ole nolla.** Viestiarkisto on ainoa kopio siitä mitä sivusto ei säilytä, ja
  lukko oli se mikä esti sen lukemisen laitteen ollessa auki jonkun toisen kädessä. Laitteen
  oma lukitusnäyttö on yhä voimassa, joten suoja kutistuu yhteen kerrokseen eikä katoa.
  Päätöksellä oli myös mitattu peruste: lukko keskeytti saman yön laiteajot neljä kertaa,
  koska jokainen `installDebug` tappaa prosessin ja jokainen kylmä käynnistys pyysi avausta.
- **Salasana on levyllä AES-GCM-salattuna**, avain Androidin Keystoressa
  (`KeystoreKey`, alias `dg_credentials_key`). Salaus ja avaimen nouto ovat eri luokkia
  tarkoituksella: Keystorea ei voi ajaa JVM-testissä, mutta `AesGcmCipher` on tavallista
  JCE:tä ja testattavissa ilman laitetta. Ilman tätä jakoa koko salauslogiikka olisi
  todennettavissa vasta tabletilla.
- GCM eikä CBC: se todentaa sisällön muuttumattomuuden itse, joten muokattu tavu tuottaa
  poikkeuksen eikä roskaa. Roskaksi purkautunut salasana lähtisi verkkoon sellaisenaan.
- **Avaimen käyttö ei vaadi käyttäjän tunnistautumista.** Houkutteleva lisäys, mutta se
  rikkoisi istunnon hiljaisen uusimisen: uusiminen tapahtuu keskellä tavallista
  sivunhakua, eikä siinä voi pysähtyä kysymään sormenjälkeä.
- Purun epäonnistuminen palauttaa **null eikä poikkeusta**: mitätöitynyt avain tarkoittaa
  käyttäjälle vain uutta kirjautumista, ja poikkeus kaataisi sovelluksen käynnistyksessä.
- Jos salaus ei onnistu, tunnusta **ei tallenneta selkokielisenä varmuuden vuoksi**.
  `save` palauttaa false, ja käyttäjä saa oman virheilmoituksensa
  (`login_storage_failed`). Väärä salasana ja rikkoutunut suojaus ovat eri vika, eikä
  jälkimmäistä voi korjata kirjoittamalla salasanan uudestaan.
- Käyttäjänimeä ei salata. Se ei ole salaisuus, ja selkokielisenä se kertoo onko tunnus
  tallessa myös silloin kun avain on mitätöitynyt.
- Uloskirjautuminen poistaa arvot **ennen** avainta. Toisessa järjestyksessä keskeytys
  jättäisi levylle arvon jota ei voi avata eikä tunnistaa roskaksi.
- `core-net` julkaisee OkHttpin `api`na eikä `implementation`ina: OkHttpin tyypit näkyvät
  `DgClient`in ja `CookieStore`n julkisissa rajapinnoissa, joten kutsuja ei käänny ilman niitä.
