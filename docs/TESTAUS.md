# Laiteajo ja todentaminen

Milloin laiteajo tehdään, mitä siinä voi todentaa itse ja mikä vaatii Tommin silmät.

Siirretty `CLAUDE.md`:stä 10.8.2026. Sisältö on ennallaan, vain sijainti muuttui.
Hakemisto ja pyytämättä laukeavat portit ovat yhä `CLAUDE.md`:ssä.

**Todentaminen on osoittautunut helpommaksi kuin oletettiin, ja syy on kaksi työkalua
(Tommi 16.8.2026).** Proxy lukee tavut ennen sivun omaa koodia (`TALTEENOTTO.md`), ja laite on
hallittavissa USB:n kautta, joten sekä lähde että kohde ovat nähtävissä ilman arvailua. Tämä on
kirjattu tänne siksi, että oletus meni toiseen suuntaan: verkkoa vasten tehtävää todentamista
pidettiin projektin kalleimpana osana, ja se on käytännössä sen halvimpia. Seuraus on
suunnitteluun asti ulottuva, koska todentamisen halpuus on syy tehdä laiteajo samassa sessiossa
eikä seuraavassa, ja juuri sitä alla oleva osio vaatii.


**Testilaitteet, luettuna laitteilta `getprop`illa eikä muistista.** Pixel 8a: **Android 17,
API 37**, 891 x 411 dp. Galaxy Tab S7+ (SM-T970): **Android 13, API 33**, 1752 x 2800 px,
tiheys 340, vaaka-asennossa noin 1318 x 825 dp. `minSdk 26` on siis oikea reilulla
marginaalilla. Kokoero on se joka on laukaissut ulkoasuvikoja molempiin suuntiin: lauta
johtaa leveytensä korkeudesta, joten iso näyttö tuottaa laudan joka vie koko leveyden.

### Odottaa seuraavaa pelisessiota (kirjattu 1.9.2026)

Tämä osio on lyhyt tarkoituksella, ja se tyhjennetään sitä mukaa kun kohdat todentuvat. Se
on olemassa siksi, että 1.9. sessiossa korjattiin kolme asiaa joista **vain yksi ehti
todentua oikeassa käytössä**; muut ovat yksikkötestien varassa, ja tämä tiedosto sanoo
missä ero menee.

**Ohje itselle: proxy päälle heti alussa** (`raakasivut/LUEMINUT.md`), koska jokainen alla
oleva kohta on luettavissa lokista jälkikäteen eikä vaadi oikeaa hetkeä. Ja tarkista
prosessilistalla ettei vanha proxy ole yhä käynnissä; portin kaappaus on hiljainen.

1. ~~**`To Top` päättymissivulla.**~~ **Todennettu 1.9.2026 kuoriproxylla, ilman yhtään
   pyyntöä sivustolle** (alempi osio). Ruudulla olivat kaikki neljä tekoa sivun omassa
   järjestyksessä — `Next Game`, `To Top`, `Review game`, `Skip game` — ja `To Top` tuotti
   täsmälleen odotetun jäljen: `POST /bg/move/5304226/1491` kentillä `commit,submit` ja heti
   perässä `GET /bg/top`. Ruutu sulkeutui itsestään otteluluetteloon eikä väliin jäänyt
   viestiruutua. Painallus itse oikeaa sivustoa vasten on yhä tekemättä, mutta se on nyt
   `Next Game`in kanssa sama mekanismi, joka meni läpi kello 0.38.

2. **Päättymissivun chat-kortti. Piirtyminen todennettu, lähettäminen ei.** Kortti,
   `Your message` -kenttä, selitysteksti ja sivun omat napit `Next Game` ja `To Top`
   piirtyvät oikein chat-muunnelmassa, eivätkä lomakkeen napit tule silloin kahdesti
   (todennettu kuoriproxylla 1.9.2026). **Lähettämistä ei todennettu tarkoituksella:**
   onnistunut lähetys kirjaa viestin arkistoon, ja kuoriproxyn sivut ovat tallennettuja,
   joten arkistoon menisi keksitty rivi. Se on väärä hinta todennuksesta.

   ~~Yhä avoinna oikeassa käytössä: saapunut viesti on arkistossa **ennen** kuin poistut
   ruudulta, ja lähetetty viesti menee perille eikä katoa.~~ **Mitattu 7.9.2026 yöllä, ja
   vastaus oli ei** (`raakasivut/sessio-7-9-yo/0062`, nauha `nauhat/dg-7-9-yo-2.mp4` loppu):
   erichktn kirjoitti ottelun viimeisellä vuorolla, päättymissivu kantoi viestin ilman
   kenttää, ruutu näytti pelkän tuloksen ja napit, eikä arkistoon tullut riviä. Syy oli
   lukijassa, joka vaati kentän ennen arkistointia (`docs/KOHDE.md`, neljäs muunnelma).
   Korjattu ja lukittu kolmella testillä samana yönä. **Tommin päätös samana yönä: aja
   kuori ja kirjaa viesti arkistoon.** Ajettu 23.56 (`0062` kuoresta, ottelu avattu
   luettelosta): arkistossa on nyt rivi `erichktn`, ottelu 5319620, *"Another good win! Keep
   it going!"*, otsikko `erichktn says:`, aika 23.56 eikä 23.40. **Sama ajo paljasti toisen
   vian:** kortti piirtyi tyhjänä palkkina ilman viestiä. Viestisarakkeen painotus
   (`weight(1f, fill = false)`) antaa nollan päättymisruudun rajattomassa sarakkeessa;
   laudalla kortilla on katto, ja kenttämuunnelmassa kenttä antoi kortille korkeuden.
   Korjattu (`ChatCard.messageScrolls = false` päättymisruudulla) ja asennettu, **ruutu
   todentamatta**: toisto kirjaisi viestin toiseen kertaan (tunniste sisältää
   saapumishetken), eikä laitteella ole `sqlite3`:a rivin poistoon. Lähetetty viesti
   todennettiin 7.9.2026 illalla (`sessio-7-9`, Willie Wonka).

3. **Pistelukujen tasapeli ja paikallinen kokoaminen.** Korjattu 1.9., todennettu korpuksesta
   muttei laitteelta. Tasapelilauta (molemmilla sama pip-luku) pitäisi nyt koota siirron
   paikallisesti: lokiin tulee **yksi** rivi vuoroa kohti (`?move=<kirjaimet>&submit=Submit+Move`)
   eikä rivi per nappula. Ennen korjausta samat laudat tuottivat kolme riviä.

4. **`Reply to <pelaaja>` -napin ehto.** Yhä tuntematon. Jos nappi ilmestyy päättymissivulle,
   kirjaa mitä siinä hetkessä oli totta, erityisesti oliko kyseiseltä pelaajalta lukematon
   pikaviesti.

5. **`cubedrs` eli omistettu DR-kuutio.** Nähty laitteella 1.9. (tuplasit Prado-ottelussa),
   joten tämä on **tehty**; rivi on tässä vain siksi ettei sitä etsitä uudestaan.

6. **Pakollisten askelten esipoiminta (kirjattu 2.9.2026).** Asetus `Place forced checkers
   for me` päällä: kun nappula on muurilla ja vain toinen noppa pääsee sisään, laudan pitäisi
   avautua tulo jo poimittuna (`Undo Move` näkyvissä heti, yksi kuutio vähemmän tai sivuston
   noppaesityksessä kirjain jo kertyneenä). Lokiin tulee yhä **yksi** rivi vuoroa kohti, ja
   sen kirjaimissa on `y` ensimmäisenä. **Todennettu kuoriproxylla 2.9.2026** tallennetusta
   laudasta (`sessio/0774`, ottelu 5310938, nopat 4 ja 3, pakollinen askel uloskanto
   kuutospisteeltä): lauta avautui `Undo Move` näkyvissä, yksi kuutio (3) jäljellä ja
   kuutosen nappula jo kannettu ulos, ja kuoren loki näytti yhden `GET /bg/move/` -rivin eikä
   yhtään lähetystä. Ei oikeaa sivustoa vasten; sivuston vastaus esipoimitulla kirjaimella
   alkavaan lähetykseen on se mikä puuttuu. Jos sivusto hylkää lähetyksen, kirjaa mitä lomake
   sanoi. Asetus jätettiin laitteella päälle, jotta seuraava pelisessio todentaa sen.
   **Todennettu oikeaa sivustoa vasten 2.9.2026 illan pelisessiossa** (proxy
   `raakasivut/sessio-2-9`): 12 asemassa 76:sta pakollinen askel, lähetetty siirto sisälsi
   sen 12/12, eikä sivusto hylännyt yhtään lähetystä.

7. **Ahne uloskanto (kirjattu 2.9.2026).** Asetus `Bear off greedily for me` päällä: kun
   kontaktia ei ole ja uloskanto on mahdollinen, lauta avautuu ahne vuoro poimittuna. Jos
   ahne loppuasema on yksikäsitteinen, `Submit Move` näkyy heti; muuten osa askelista on
   poimittu ja loput jäävät valinnaksi. **Todennettu kuoriproxylla 2.9.2026** kahdella illan
   laudalla (`sessio-2-9/0005` Golden, nopat 5 ja 4: koko vuoro ja `Submit Move`;
   `sessio-2-9/0058` mimi711, tupla 2: kolme askelta neljästä). Ei oikeaa sivustoa vasten;
   puuttuu sivuston vastaus sovelluksen kokoamaan ahneeseen lähetykseen, joka on kirjaimiltaan
   sama laji kuin käsin koottu. Asetus jätettiin laitteella päälle seuraavaa pelisessiota
   varten. **Illan toinen pelisessio 2.9.2026** (`raakasivut/sessio-2-9-ilta`, 18 asemaa)
   ei sisältänyt yhtään lautaa jolla uloskanto olisi ollut mahdollinen, joten ahne jäi
   harjoittamatta; pakollinen askel oli 5 asemassa ja lähetys sisälsi sen 5/5, hylättyjä
   lähetyksiä ei ollut. Odottaa yhä sitä pelisessiota jossa kilpajuoksu on uloskannossa.
   **Todennettu oikeaa sivustoa vasten 2.9.2026 yön pelisessiossa** (`raakasivut/sessio-2-9-yo`,
   11 asemaa): neljä kontaktitonta uloskantolautaa. Kolmella sivusto tarjosi greedy-nappia ja
   sovellus poimi saman koko vuoron (`fe`, `db`, `da`), ja Tommi lähetti täsmälleen sen.
   Neljännellä (tupla 3, ei nappia, kolme ahnetta loppuasemaa) sovellus poimi yhteisen osan
   `ccc` ja Tommi lisäsi `f`:n. Sivusto hyväksyi kaikki neljä: jokaista seurasi vastustajan
   vuoro ja seuraava `Roll Dice`. Pakollinen askel 3 asemassa, lähetys sisälsi sen 3/3.
   **Toistui 3.9.2026 illan sessiossa** (`raakasivut/sessio-3-9-ilta`, 44 asemaa): kaksi
   kontaktitonta uloskantolautaa samassa ottelussa (`0038` ja `0039`), sivusto tarjosi
   greedy-nappia molemmilla, sovellus poimi koko vuoron (`dc`, `ea`) ja Tommi lähetti
   täsmälleen sen; jälkimmäinen päätti ottelun. Pakollinen askel 5 asemassa, lähetys sisälsi
   sen 5/5, hylättyjä lähetyksiä ei ollut.

8. **Napit sivupaneelissa (kirjattu 2.9.2026).** Tabletilla sivun napit ovat vasemmassa
   paneelissa ottelutietojen alla pystypinona, ja keskikaistalla on vain nopat. Todennettu
   kuoriproxylla 2.9.2026 (siirtovuoro ja heittovuoro, tuplauksen dialogi paneelista).
   Tommin silmät samana iltana: pino alemmas nimen kiinni ja muistutusosio ylös, tehty ja
   todennettu kuorella heittovuorolla. Puhelimella katsomatta.

### Ruutu jota ei voi tilata: kuoriproxy (1.9.2026)

**Ongelma on kirjattu monta kertaa eri muodoissa: osaa ruuduista ei voi järjestää.**
Päättymissivulle pääsee vain kun ottelu päättyy, chat-muunnelmalle vain kun vastustaja on
kirjoittanut, ja tasapelilaudalle vain kun luvut sattuvat olemaan samat. Ne ovat jääneet
yksikkötestien varaan, ja 1.9.2026 kävi ilmi mitä se maksaa: juuri niissä ruuduissa oli
kaksi vikaa joita testit eivät nähneet, koska molemmat olivat ruudun ja tilan välissä
(napit puuttuivat, teot olivat kuolleita).

`tyokalut/kuoriproxy.py` tekee sellaisesta ruudusta todennettavan. Se ei ole `proxy.py`:n
muunnelma vaan sen vastakohta: **proxy välittää kaiken ja tallentaa, kuori ei välitä
mitään.** Tunnetut polut saavat tallennetun sivun, kaikki muu saa 503.

```bash
python tyokalut/kuoriproxy.py top.html paattymissivu.html
adb reverse tcp:8899 tcp:8899
adb shell settings put global http_proxy 127.0.0.1:8899
```

**Airlock on koko turvallisuusperuste, ja se kytkeytyy suoraan yllä olevaan sokeiden
koordinaattien sääntöön.** Rajaus siellä on lähetettävyys eikä syötteen laji: napautusta ei
saa tehdä siellä missä se voi lähettää jotain sivustolle. Kuoriproxyn aikana **mikään pyyntö
ei mene ulos**, joten ehto ei täyty edes silloin kun napautus osuu väärin. Airlock
todennetaan ennen jokaista ajoa pyynnöllä joka ei koske kohdetta, ja sen on palautettava
503:

```bash
adb shell 'curl -s -o /dev/null -w "%{http_code}
" -x http://127.0.0.1:8899 http://example.com'
```

Loput neljästä säännöstä pätevät ennallaan: fokus luetaan ennen napautusta, kohde luetaan
samassa tilassa otetusta kaappauksesta, näytön kokoa ei muuteta.

**Kolmas todennus samalla työkalulla 1.9.2026:** päättymissivun `Review game` avaa
siirtolistan sivunlukijassa (otsikko, `11 point match`, pelit erikseen, kuutiopäätökset
`Doubles => 2` / `Takes` / `Drops`). Ennen korjausta sama nappi haki sivun lautanäkymään,
joka vastasi *"This is no longer a board"*. Kuoren kolmas sivu (`/bg/game/...`) annetaan
komentoriviltä kolmantena tiedostona.

**Neljäs ja viides todennus samana päivänä, ja ne kertovat mihin työkalu vielä venyy.**
Kuori ei tarvitse päättymissivua ollakseen hyödyllinen: `move`-polulle annettu **tavallinen
lautasivu** todensi sivupaneelin `Message`-napin (nappi eikä tekstilinkki, `Skip Gamen`
edellä, painallus avasi chat-kortin lähettämättä mitään), ja **tunnisteeltaan eri ottelu**
todensi `NotABoard`in uuden `Back`-napin. Jälkimmäinen on esimerkki siitä mitä tällä saa
ilmaiseksi: `DifferentMatch` syntyi siitä että kuoren lautasivun ottelutunnus ei ollut sama
kuin luettelosta avatun, eli **ruutu jota ei muuten voi tilata syntyi yhden `sed`-korvauksen
kautta**. Ottelutunnuksen sovittaminen on siis kuoren oma säädin, ei este.

**Kolme rajausta, ja ne rajaavat mihin tämä kelpaa.** Sivut ovat tallennettuja, joten
sovellus näyttää vanhaa tietoa eikä tämä todenna mitään tuoretta. Lähetys ei mene perille,
joten nähtävissä on ruudun ja tilan käytös eikä sivuston vastaus. Ja **tekoja jotka
kirjoittavat kantaan ei saa ajaa kuoren päällä**: onnistunut chat-lähetys kirjaisi arkistoon
rivin tallennetusta sivusta, eli keksityn viestin oikeaan arkistoon.

**Monte Carlo variant todennettu kuorella 13.9.2026.** Kolmas lautatyyli (`docs/ASETUKSET.md`
luku 3) ajettiin tabletilla `sessio-12-9-ilta`n sivuilla (`0002` top, `0006` lauta, `0003`
profiili). Airlock palautti 503, asetusrivi valittiin ruudulta ja lauta piirtyi vihreällä
huovalla, oranssi ja kerma kiiloilla, valkoisella omalla ja mustalla vastustajalla. Kuvat
`raakasivut/sessio-12-9-ilta/kuori/monte-carlo-variant.png` ja `-asetus.png`. Väriarvot ovat
silmämääräiset lähtöarvot, ja säätö odottaa Tommin käsikokeilua. Tabletin asetus jäi tähän
tyyliin, jotta kokeilu alkaa suoraan.

**Samana iltana kuorella myös keskikaistan viisi muunnelmaa** (`kaista-k15/k25/k30/h20/h25.png`,
ks. `docs/UI.md`) ja kaksi päätöstä niiden jälkeen: omat nopat paneelia vastapäiseen laitaan ja
lokerosarake kehyksen väriseksi (`nopat-laitaan-lokerosarake-puuta.png`). Kokeilumuunnelmat
ajettiin väliaikaisella koodilla joka luki suhteen tiedostosta `files/kokeilu.txt` `run-as`-
kirjoituksella, ja koodi peruttiin ennen committia; se ei ole repossa. Kolmas päätös kuvien
jälkeen: kaista huovan väriseksi ja muuri sen läpi (`kaista-huopa-muuri-lapi.png`).

**Kuudes todennus 7.9.2026: katkotila syntyy tappamalla kuori.** Katkotilan `Refresh`-nappi
(`UnconfirmedRefresh`) ei ole tilattavissa oikeaa sivustoa vasten, mutta kuoren kanssa se
syntyy kolmella askeleella: lauta kuoresta (`sessio-7-9-ilta/0010`, Quastelin heittovuoro),
kuori alas, `Roll Dice`. Yhteys kieltäytyy, `PendingBar` nousee, dialogi sanoo uuden tekstin
(*press Refresh on the board or pull down*), ja `OK`:n jälkeen laudalla on `Refresh`. Kuori
takaisin ylös ja nappi: kuoren loki näytti yhden `GET /bg/move/5312554/2067` -rivin, nappi
katosi ja lauta oli taas pelipinta. Lopuksi `Discard` kuoren suojassa, koska odottava teko
on kannassa (`pending_actions`) ja jäisi muuten oikean ottelun päälle; taulu luettiin
tyhjäksi `run-as`:lla ennen asetuksen purkua.

**Seitsemäs todennus 8.9.2026: katkotila syntyi itsestään oikeaa sivustoa vasten**
(`raakasivut/sessio-8-9-yo2`, ottelu 5320443, rivit 5–7). `Submit Move` kello 0.53.46 ei
koskaan tullut proxylle, sovellus avasi katkotilan ilmoituksen 21 sekunnin kuluttua ilman
kosketusta. `OK`, `Refresh` ja uusi siirto menivät läpi 0.54.12–0.54.27. Tunnistus on
logcatista eikä Tommin muistista, ketju on `LUEMINUT.md`:ssä. Kuoren kanssa mitattu polku
ja oikea polku olivat samat. Erona kuoritestiin `Discard`ia ei tarvittu, koska onnistunut
siirto pyyhkii odottavan teon itse.

**Sama ajo toistettiin samana iltana napin siirron jälkeen** (`sessio-7-9-ilta/0022`,
ReggiePerrin, omistamaton kuutio kaistan keskellä): nappi piirtyi kaistan keskelle kuution
päälle, haku kuoresta (`GET /bg/move/5315340/814`) toi kuution takaisin ja napin pois, ja
`Discard` tyhjensi taulun. **Yksi lipsahdus samassa ajossa, ja se on jo kirjattu sääntönä
(fokustarkistus on portti vasta omana kutsunaan, 29.8.2026):** asennus oli sulkenut
sovelluksen, fokus luettiin samassa kutsussa napautusten kanssa eikä ehtona, ja kaksi
napautusta meni käynnistimeen. Kaappaus näytti ettei mitään avautunut. Toisto teki
fokuksen omana kutsunaan ennen jokaista napautusta.


```bash
adb shell settings put global http_proxy :0
adb reverse --remove tcp:8899
```

### Laiteajo kuuluu sessioon, ei seuraavaan sessioon (8.8.2026)

**Älä kirjoita loppumerkintää joka lykkää laiteajon, jos laite on kiinni.** Tarkistus on
`adb devices`, ja jos laite vastaa, asenna ja katso ensin:

```
adb devices
gradlew :app:installDebug
```

*Miksi tämä on tässä eikä `paata-sessio`-skillissä.* Skilli on kokoelman yhteinen ja laiteajo
koskee yhtä projektia kahdeksasta, joten tarkistus kuuluu sinne missä laite on. Skilli teki
oman työnsä sinä iltana jona tämä kirjattiin: neljä tarkistusta menivät läpi ja merkintä
syntyi. Vika oli tässä projektissa.

*Mitä mitattiin.* 8.8.2026 loppumerkintä kirjasi kolmen muutoksen laiteajon Tommin varaan.
Laite oli pöydällä, ajo tehtiin viisi minuuttia myöhemmin, ja se **kumosi niistä yhden**
(kiilan suhde 1:5 muuttui 0,70:ksi DG Mobilen kaappauksen perusteella) sekä korjasi kanonista
väärän Android-version. Merkintä oli siis vanhentunut minuutteja kirjoittamisensa jälkeen.

*Miksi juuri tämä muoto.* Yleinen versio olisi "esitä kysymykset ennen päättämistä", ja se on
sama vika kuin kalenteriauditoinnissa (`Kaanon/TYÖTAVAT.md`): avoin kehotus laukeaa hetkellä
jolloin mitään ei olla päättämässä ja tuottaa listan kysymyksiä eli lisää työtä sen sijaan
että sulkisi sen. Sinä iltana se olisi tuottanut arvauksia siitä mitä puhelimessa voisi
näkyä, kun oikea teko oli asentaa ja katsoa. Tämä muoto sen sijaan luetteloi eikä hae, joten
se ei voi antaa väärää nollaa: laite joko vastaa tai ei.

### Mitä laiteajossa voi todentaa itse ja mikä vaatii Tommin silmät (9.8.2026)

Edellinen osio sanoo **milloin** laiteajo tehdään. Tämä sanoo **millä**, ja se on kirjattu
koska väärä työkalu antaa tässä väärän nollan.

**`adb` ei ole polussa.** Se on `C:/Users/nojop/AppData/Local/Android/Sdk/platform-tools/adb.exe`,
ja pelkkä `adb devices` vastaa `command not found`. Se on äänekäs virhe eikä ansa, mutta se
maksaa yhden kierroksen joka kerta ellei polkua muista.

Kolme tallennetta, ja ne vastaavat eri kysymyksiin:

| Työkalu | Näkee | Ei näe |
|---|---|---|
| `adb logcat --pid=<pid>` | kaatuminen, poikkeus, verkkovirhe, Composen mittausvirhe | mitään ulkoasusta |
| `adb exec-out screencap -p` | koko ruudun sellaisenaan, eli leikkautuminen ja värit | mitään eleistä tai ajallisesta |
| Tommin katse | eleet, ajoitus, katoaako päällepiirto itsestään | |

**Manifestin pystylukko ei pidä tabletilla (mitattu 31.8.2026).** `AndroidManifest.xml`
pyytää `screenOrientation="portrait"`, ja Pixelillä se toteutuu, mutta SM-T970 ajaa
sovellusta myös vaaka-asennossa (`dumpsys window`: `land`, `w1317dp h752dp`,
`mDisplayRotation=ROTATION_90`). Iso näyttö ohittaa pyynnön. Seuraus todentamiselle: **laudan
ulkoasu on katsottava molemmissa asennoissa**, eikä manifestista voi päätellä kumpi on
ruudulla. Ruutukaappauksen mitat kertovat sen suoraan, kaappaus kaappauksen jälkeen.

**Logcat on sidottu prosessiin eikä sovellukseen.** `pidof fi.tommi.dg` antaa pid:n, ja jos
sovellus kaatuu tai käynnistetään uudelleen, pid vaihtuu ja nauhoitus loppuu siihen. Se on
hyödyllinen signaali eikä puute: tallenteen päättyminen kertoo kaatumisesta.

**Ulkoasuvika on hiljainen logcatissa.** Leikkautunut lokero, ristiin menneet pelaajavärit ja
navigaatiopalkin alle jäänyt päällepiirto tuottavat täsmälleen saman lokin kuin kaikki
kunnossa. Tämä on `koetin`-termin työkalumaasto (`Kaanon/KÄSITTEISTÖ.md`) ja samaa perhettä
kuin `git -C`-ansa: komento onnistuu ja raportoi oikean näköisesti. Puhdas logcat ei siis ole
näyttö ulkoasusta, ja sitä ei saa esittää sellaisena.

**Lukittu ruutu tuottaa nollan tavun kuvan ilman virheilmoitusta** (mitattu 10.8.2026).
Tabletin näyttö lukkiutuu minuuteissa, ja lukitusnäytön biometrinen kehote on suojattu ikkuna:
`screencap` onnistuu, `adb` palauttaa nolla, eikä mikään kerro miksi. Sama tulos syntyy myös
laitteelle kirjoitettaessa, eli `screencap -p /sdcard/x.png` jättää 0-tavuisen tiedoston.

Ansa on hiljainen ja samaa perhettä kuin `git -C`:n väärä repo: tulos on kelvollisen näköinen
ja se johtaa etsimään vikaa sovelluksesta. Tarkistus ennen kuin epäilee koodia:

```bash
adb shell dumpsys window | grep mCurrentFocus     # BiometricPrompt = ruutu on lukossa
```

Sovellus näkyy tällöin yhä `mFocusedApp`issa ja `pidof` vastaa, eli kaksi tavallista
elossaolotarkistusta antavat molemmat vihreän. Korjaus on pyytää Tommia avaamaan lukitus;
`KEYCODE_WAKEUP` ei riitä, koska näyttö on jo hereillä.

**Bashissa laitepolut on suojattava.** Git Bash muuntaa `/sdcard/...`-argumentin
Windows-poluksi, jolloin `screencap` tulostaa käyttöohjeensa eikä tee mitään. `MSYS_NO_PATHCONV=1`
poistaa muunnoksen. Sama koskee `adb pull`in lähdepolkua ja `run-as`-komentojen polkuja.
Ansa osui uudelleen 3.9.2026, joten kaappaus on nyt yksi komento: `tyokalut/kaappaus.sh
<nimi> [hakemisto]` tekee screencapin laitteelle, pullin ja luettavan pienennöksen
(`<nimi>_s.png`, enintään 800x1280) suojatuin poluin.

~~**Ruutukaappaus on Tommin ruutu, joten sitä ei oteta kysymättä.**~~ **Kumottu 22.8.2026
Tommin päätöksellä, ks. seuraava osio.** Alkuperäinen perustelu jätetään näkyviin, koska sen
toinen puoli on yhä voimassa ja koska juuri se puoli ratkesi eri tavalla kuin oletettiin.

Kaappaus näyttää käynnissä olevan ottelun ja vastustajan nimen. Ehto ei koskenut vain
yksityisyyttä vaan myös osumista: kaappaus kertoo mitä ruudulla oli sillä hetkellä, joten se
oli pyydettävä siihen hetkeen jolloin oikea näkymä on auki. Ennakkotapaus 9.8.2026: kaappaus
pyydettiin laudasta ja osui otteluluetteloon, eikä yhtäkään tarkistettavaa kohtaa voinut lukea
siitä.

*Sivutuote joka kannattaa lukea silloinkin kun kaappaus osui väärään ruutuun:* otteluluettelon
arkistorivi (`Message archive is empty`) on riippumaton todiste kannan tilasta, eli sama väite
jonka koodista lukeminen antaa. Kaksi lähdettä samasta asiasta on tässä halpaa.

### Lukutilassa saa liikkua itse, myös kaapata (22.8.2026)

**Laitteella navigointi ja ruutukaappaus eivät enää odota Tommia.** Otteluiden avaaminen,
luetteloon palaaminen, `uiautomator dump` ja `screencap` ovat kaikki tehtävissä ilman että
kysytään erikseen. Kysyminen jää niihin tekoihin jotka **lähettävät jotain**: siirto, tuplaus,
kutsun hyväksyntä ja viestin lähetys.

**Raja on peruttavuus eikä laite.** Lautasivun avaaminen on kyselyparametriton GET, joka ei
muuta sivustolla mitään ja on peruttavissa yhdellä paluulla. Se on siis samalla puolella rajaa
kuin tiedoston lukeminen työpuusta, ja kokoelman kanoni sanoo saman yleisemmin: peruttavasta
teosta ei kysytä lupaa.

**Mikä tämän laukaisi.** Sama muutos todennettiin 22.8.2026 kolmessa erässä, ja jokainen erä
päättyi pyyntöön avata ottelu uudelleen, koska asennus sulkee sovelluksen. Tommi kysyi miksi
häntä tarvitaan välikädeksi, eikä kysymykseen ollut kestävää vastausta: peruuttamattomuuden
varovaisuus oli lainattu väärään paikkaan, koska avaaminen ei ole peruuttamaton teko.

**Osumaehto ei kaadu vaan ratkeaa.** Yllä oleva vanha perustelu sanoo että kaappaus on
pyydettävä juuri siihen hetkeen jolloin oikea näkymä on auki, ja se oli oikea huoli: 9.8.2026
kaappaus osui otteluluetteloon eikä lautaan. Ongelma seurasi kuitenkin nimenomaan siitä että
kaappaaja ja navigoija olivat eri henkilöt. Kun sama taho avaa näkymän ja kaappaa sen, hetki
osuu määritelmän mukaan.

**Mitä jää voimaan yksityisyydestä.** Kaappaukset syntyvät käynnissä olevista otteluista ja
sisältävät vastustajien nimiä. Ne ovat mittausvälineitä eivätkä aineistoa: ne jäävät
scratchpadiin eivätkä päädy versioon, ja fixtureksi menevä sivu anonymisoidaan kuten ennenkin
(`docs/TALTEENOTTO.md`).

### Yhteyskatko testataan tabletilla, ei puhelimella (10.8.2026)

Katkos oli pitkään todentamatta, ja este oli väline: puhelin on kehityskoneen hotspot, joten
sen lentotila katkaisee myös sen koneen yhteyden jolta `adb` ajetaan. Tabletti erottaa nämä
kaksi. Se on saman hotspotin varassa mutta ei ole se hotspot, ja `adb` kulkee USB:tä pitkin
eikä sen wifin läpi jonka lentotila sammuttaa. Serialista näkee kummasta on kyse:
`R52R403ZMZF` on USB, `host:5555` olisi verkko.

```bash
adb shell cmd connectivity airplane-mode enable    # ei vaadi roottia Android 13:ssa
adb shell settings get global airplane_mode_on     # 1 = poikki
adb shell cmd connectivity airplane-mode disable
```

Kaksi asiaa jotka eivät ole ilmeisiä, ja molemmat maksavat kierroksen jos ne unohtaa.

**Offlinena ei pääse lautaan.** Sovellus avautuu otteluluetteloon ja luettelo haetaan verkosta,
joten katkon jälkeen käynnistetty sovellus ei näytä lautaruutua lainkaan. Kaikki mikä koskee
laudan tilaa katkon aikana on siis ladattava ruudulle **ennen** katkaisua, ja uudelleenkäynnistys
on testattava kahtena: offlinena ja yhteys palautettuna.

**Värähtely ei näy logcatissa vaan `gfxinfo`ssa.** Tyhjäkäynnillä piirrettyjen ruutujen määrä on
suora mitta, koska paikallaan olevan näkymän kuuluu piirtää nolla. Sama komento erottaa myös
korjauksen onnistumisen arvauksesta, ja se kelpaa numeroksi committiviestiin.

```bash
adb shell dumpsys gfxinfo fi.tommi.dg reset && sleep 5 && adb shell dumpsys gfxinfo fi.tommi.dg
```

Kanta luetaan koneelle, koska laitteella ei ole `sqlite3`:a:

```bash
adb exec-out run-as fi.tommi.dg cat databases/dg.db > dg.db   # myös -wal ja -shm
```

### Katko mitattiin molemmista sovelluksista (22.8.2026)

Edellinen osio sanoo **miten** katko tehdään. Tämä sanoo **mitä siitä nähtiin**, kun sama katko
ajettiin sekä DG Mobilelle että omalle sovellukselle saman istunnon aikana. Kuvat elävät
scratchpadissa eivätkä versiossa, koska ne ovat käynnissä olevista otteluista
(`Lukutilassa saa liikkua itse` -osion yksityisyysrajaus).

**Oma sovellus kestää katkon, ja `CLAUDE.md`:n kohta 2 sai näytön.** Kylmäkäynnistys
lentotilassa avautui suoraan `Signed in as` -tilassa ilman kirjautumiskehotetta, ja tarjosi
tekstin `No connection. Refresh to see what actually happened.` sekä Refresh-napin.
Offline-Refresh toisti saman viestin kaatumatta. Ladattu lauta säilyi katkon yli bittiä myöten:
kaappaukset ennen katkoa ja katkon jälkeen eroavat vain tilapalkin lentokonesymbolista.

**DG Mobile ei säilytä mitään paikallisesti.** Käynnissä oleva sovellus kesti hiljaisuuden niin
kauan kuin sivua ei haettu uudelleen, mutta paluu otteluluetteloon paljasti tilan heti:
otsikkorivi jäi näkyviin, rivit hupenivat nollaan ja päälle nousi ilmoitus `Unable to resolve
host. An internet connection is required.`

**Kanonin kohta 2 osui oikeaan asiaan mutta väärästä syystä, ja Tommin oma käytäntö nimesi
mekanismin.** Kohta sanoo että DG Mobilessa palvelimen hiljaisuus pakottaa kirjautumaan
uudestaan. Pelkkä hiljaisuus ei pakota mitään. Rikki menee **lentotilassa tehty
uudelleenkäynnistys**: sovellus avautui tyhjään luetteloon, eikä sen oma Refresh palauttanut
listaa edes yhteyden palattua (kolme painallusta, noin 40 sekuntia, ei muutosta). Tommi palautti
tilan sulkemalla ja avaamalla sovelluksen uudelleen, jolloin luettelo latautui täytenä eikä
kirjautumista kysytty. **Istunto siis säilyy, ja se mikä ei toivu on epäonnistunut ensimmäinen
haku.** Tämä on Tommin vakiintunut kiertotapa, ja mittaus selittää miksi se toimii: korjaus on
toinen käynnistys eikä Refresh.

**Katko on hiljainen logcatissa molemmissa sovelluksissa.** DG Mobilen 114 rivistä ja oman
sovelluksen kolmesta rivistä nolla koski verkkovirhettä. Katkoa mitataan siis kaappauksella eikä
lokilla, ja se on sama työkalumaasto kuin ulkoasuvioilla yllä.

Kolme löytöä oman sovelluksen puolelta, ja ne ovat tilattavissa työksi erikseen.

~~**Yhteyden palaaminen ei herätä näkymää.**~~ **Korjattu samana päivänä**, ks. `docs/ARKKITEHTUURI.md`.
Havainto oli se, että sekä otteluluettelo että lauta jäivät offline-viestiin kunnes Refresh
painettiin käsin. Sovellus kuuntelee nyt yhteyden palaamista ja hakee uudelleen, jos edellinen
haku kaatui verkkoon.

**Lähetysnappi jää tarjolle offlinena, ja se on hyvä niin.** `Roll Dice` näkyy katkon aikana
samanlaisena kuin verkossa, ja painallus tuottaa ilmoituksen `Roll Dice may not have gone
through` sekä vaihtoehdot `Try again` ja `Discard`. Selitysteksti sanoo että uusi yritys lataa
sivun ensin DailyGammonilta eikä lähetä mitään, jos siirto näkyy jo perillä. Mitään ei siis
jonoteta sokeasti. `Discard` todennettiin: yhteyden palattua Refresh palautti laudan ennalleen
(`Move 340`, nopat 3 ja 1, `Roll Dice` painamatta), eli sivustolle ei mennyt mitään.

**Epäonnistunut lähetys hävittää ladatun laudan.** Ennen painallusta lauta oli ruudulla ehjänä
myös katkon aikana. Painalluksen jälkeen tilalla oli tyhjä ruutu ja `No connection`, eli
laitteessa jo ollut tieto katosi lähetysyrityksen mukana.

**Työkaluhuomio, joka maksoi yhden kierroksen.** `uiautomator dump` palauttaa `bounds`-arvot eri
koordinaatistossa kuin `screencap`: laudalla näytön leveys on 2800, mutta dumpin suurin x-arvo
oli 2081. `input tap` noudattaa kaappauksen koordinaatteja, joten napin paikka luetaan kuvasta
eikä dumpista. Dumpin arvo on siinä että se kertoo mitä nappeja on olemassa, ei siinä missä ne
ovat.

### Elävää tiliä ei ajeta sokeilla koordinaateilla (24.8.2026)

Edellinen osio päättyy työkaluhuomioon siitä, että `uiautomator dump` ja `screencap` puhuvat
eri koordinaatistoa. Tämä osio on saman virheen kalliimpi muoto, ja se kirjataan siksi että
edellinen huomio oli olemassa eikä estänyt sitä.

**Mitä tapahtui.** 23.8.2026 klo 23.23 sovellus näytti seitsemän ottelua, kaikki `Your turn`.
Klo 23.48 sivuston oma Top Page näytti kaksi. Väliin mahtui laiteajo, jossa Claude avasi kaksi
lautaa ja napautti ruutua koordinaateilla. Otteluista katosi kuusi, eli niihin lähti siirtoja
joita kukaan ei tarkoittanut lähettää.

**Mekanismi, ja se on rekonstruktio eikä mitattu.** Kapean leveyden testiä varten näyttö
asetettiin komennolla `wm size 720x1280`, mutta sovellus piirsi yhä vaakasuuntaan 1280x720.
`input tap` tulkitsee koordinaatit näytön luonnollisessa suunnassa, joten napautus `(272, 1090)`
ei ollut siellä missä sen luultiin olevan: y-arvo ei mahdu 720 pikselin korkeuteen lainkaan.
Napautukset osuivat laudalle sattumanvaraisesti, ja laudalla on nappuloita sekä keskellä
`Next`. Kun `Next Game` menee läpi, sovellus lataa seuraavan ottelun jossa on siirto, ja
seuraava sokea napautus tekee saman uudelleen. Yksi väärä koordinaatisto kertautuu siis
otteluketjuksi.

**Sovellus toimi oikein, ja se on tämän kohdan tärkein rivi.** `SUBSTANSSI.md` kohta 99 sanoo
ettei lähetettyä siirtoa peruta ja kohta 98 että vahvistus kuuluu kuutiolle eikä siirrolle.
Sovellus teki täsmälleen sen mitä kanoni siltä pyytää. Tästä ei siis seuraa vahvistusruutua
siirrolle, eikä tätä kohtaa saa myöhemmin lukea perusteluksi sellaiselle: peruste olisi
Clauden virhe eikä pelaajan tarve.

**Neljä sääntöä, ja ne ovat kieltoja koska niiden rikkominen ei näy mistään.**

1. `monkey` ei käynnistä sovellusta. Se lähettää käynnistyksen jälkeen satunnaisen syötteen,
   ja satunnainen syöte elävällä tilillä on lähetettävä teko. Käynnistys on `am start -n`.
2. Näytön kokoa tai tiheyttä ei muuteta ajossa joka jatkuu napautuksilla. Jos `wm size` tai
   `wm density` on asetettu, ajo on siitä eteenpäin pelkkää kaappaamista, ja koko palautetaan
   (`wm size reset`, `wm density reset`) ennen kuin ruutua kosketaan uudelleen.
3. Napautuksen kohde luetaan **samasta** kaappauksesta joka on otettu kyseisessä tilassa ja
   kyseisessä koordinaatistossa. Edellisen ruudun kaappaus ei kelpaa kohteen lähteeksi, koska
   paneelin avautuminen tai näppäimistön nousu siirtää kaiken.
4. Ennen napautusta luetaan **mikä sovellus on edessä**, ja sen on oltava odotettu paketti:

   ```bash
   adb shell dumpsys window displays | grep -m1 mCurrentFocus
   ```

   **Tämä on mitattu aukko eikä varmuuden vuoksi lisätty rivi.** Kohta 3 kirjattiin
   23.8.2026 ja rikottiin samana iltana yhtä committia myöhemmin: napautus meni sovellukseen
   jota ei ollut avattu, koska tabletilla oli Tommin oma peli edessä ja kohde luettiin
   edellisen tilan kaappauksesta. Kohta 3 ei siis riitä yksinään, koska "tuore kaappaus" on
   totta myös väärästä sovelluksesta. Fokus on tarkistettavissa yhdellä rivillä ja se on ainoa
   näistä neljästä joka kertoo kenen ruutuun ollaan koskemassa.

**Mitä yhä saa tehdä itse.** Asennus, käynnistys, kaappaus, logcat, `gfxinfo`, kannan lukeminen
ja lukutilassa liikkuminen ovat ennallaan (`Lukutilassa saa liikkua itse`). Rajaus koskee
napautuksia ja kirjoittamista, eli kaikkea joka voi lähettää jotain sivustolle. Epäselvässä
tapauksessa kohde on Tommin käsissä eikä Clauden, ja se on halvempi kuin yksi väärä siirto.

**Tarkennus 24.8.2026, Tommin kuittauksella: ratkaiseva on lähetettävyys eikä syötteen laji.**
Ilmoituspaneeli oli auki laitteella ja esti kannan tarkistuksen, ja Claude sulki sen
`KEYCODE_BACK`illa kysymättä. Se on sallittua, ja peruste on sama kuin koko rajauksella: paneeli
ei ole sivusto eikä sen sulkeminen voi lähettää mitään. Ehtoja on silti kaksi, ja molemmat ovat
tästä samasta neljän säännön listasta. Fokus luetaan ensin (kohta 4), eli tiedetään kenen ruutuun
ollaan koskemassa, ja kohde on järjestelmän oma ikkuna eikä sovellus jossa on lomake. Sama
painallus sovelluksen päällä olisi kielletty, koska takaisin-ele siellä on navigointia ja
navigointi voi hakea sivun.

**Rajaus.** Tämä ei sano ettei laiteajoa tehdä, ja se olisi väärä johtopäätös. Sama ilta tuotti
kaksi todennettua tulosta (muistutus säilyy prosessin yli, kolminumeroinen pip leikkautuu
kapealla leveydellä), eikä kumpikaan olisi syntynyt ilman laitetta. Kalliiksi tuli menetelmä,
ei laiteajo.

### Fokustarkistus on portti vasta omana kutsunaan (29.8.2026)

24.8.2026 kirjattu sääntö 4 sanoo että ennen napautusta luetaan mikä sovellus on edessä. Se
rikkoutui viisi päivää myöhemmin tavalla jota sen sanamuoto ei estä: **tarkistus ja napautukset
olivat samassa komennossa.**

**Mitä tapahtui.** Komento luki fokuksen, sai vastaukseksi `mCurrentFocus=null`, ja teki perään
kolme napautusta. Kaappaus napautusten jälkeen näytti match-3-pelin joukkuenäkymän, eli
napautukset menivät sovellukseen jota ei ollut avattu. Kohteet eivät olleet ilmeisiä nappeja,
mutta **sitä ei voi todeta jälkikäteen**: käytettävissä on vain jälkitila, ei sitä mitä
kohdissa oli napautushetkellä. Tabletti oli ilmeisesti Tommin kädessä, koska seuraava
tarkistus näytti launcherin.

**Sääntö tarkentuu, ei muutu.** Fokuksen luku on **oma kutsunsa**, ja seuraava komento lähtee
vasta kun sen tulos on nähty ja se on odotettu paketti. Samaan komentoon niputettu tarkistus on
tuloste eikä portti: se ehtii kertoa mikä oli edessä, muttei estää mitään. Sama koskee
`null`-vastausta, joka ei ole odotettu paketti vaan siirtymä, eli syy odottaa eikä jatkaa.

**Hinta on yksi kierros napautusryhmää kohti**, ja se on halvempi kuin yksikin väärä napautus.
Kaappaus riittää yhä portiksi silloin kun se on otettu samasta tilasta: käytännössä komento saa
päättyä kaappaukseen, ja seuraava komento lähtee sen perusteella.

### Näkymäpuun dumppi ei ole todiste puuttumisesta (29.8.2026)

Samana iltana väitettiin että asetusruudulta oli kadonnut valintaruutuosio, ja perusteluna oli
kaksi lähdettä: kaappaus ja `uiautomator dump`. **Molemmat olivat oikeassa ja johtopäätös oli
väärä.** Dumppi luetteloi sen mikä on ruudulla, ei sitä mikä on asettelussa, joten se ei voi
todistaa puuttumista. Kahden lähteen samanmielisyys ei siis kasvattanut katetta lainkaan, koska
niillä on sama katve.

**Mikä oikeasti oli kyseessä.** Asetusruudun laitekohtainen osio ei vieri lomakkeen mukana.
Vieritysele joka alkaa laitekohtaisen osion päältä ei tee mitään, joten kaksi peräkkäistä
kaappausta olivat identtiset ja se luettiin todisteeksi siitä ettei sisältöä ole. **Identtinen
kaappaus vierityksen jälkeen on ensisijaisesti merkki siitä että ele osui väärään paikkaan**,
eikä siitä että ruutu olisi muuttunut. Ele on aloitettava siltä alueelta jonka halutaan vierivän.

**Sivutuote samasta ajosta:** näppäimistö työntää vaaka-asennossa koko laudan pois ruudulta ja
jättää vain kirjoituskentän ylös. Nähtiin muistutuskentässä, ja sama koskee oletettavasti
ottelun viestikenttää.

### Kuvakaappaus ei näytä kaikkea mitä ruudulla on (27.8.2026)

`adb exec-out screencap` on ollut tässä projektissa oletusarvoinen tapa katsoa mitä laitteella
näkyy, ja se riittää sovelluksen omaan ulkoasuun. **Se ei kuitenkaan tallenna kaikkea mitä
ruudulla on**, eikä puute näy virheenä vaan puhtaana kulmana.

**Mitattu tapaus.** Tommi näki Pixel 8a:lla kelluvan peliohjainnapin (Google Play Gamesin
Pelipaneeli) Info-välilehden päällä. Se ei näkynyt yhdessäkään `screencap`-kuvassa eikä
`dumpsys window windows` -listassa, jossa ainoa `APPLICATION_OVERLAY` oli järjestelmän oma
`ShellDropTarget`. **Puhelimen omalla eleellä otettu kuvakaappaus näytti sen heti.**

**Sääntö joka tästä seuraa.** Jos Tommi sanoo näkevänsä ruudulla jotain jota kaappaus ei
näytä, **hän on oikeassa ja mittausväline on väärä**. Silloin pyydetään laitteen oma kaappaus
ja haetaan se `adb pull`illa. **Hakemisto etsitään eikä muisteta**, koska se eroaa
laitteiden välillä: `adb -s <sarja> shell 'ls -t /sdcard/*/Screenshots/ | head -3'` nimeää
uusimmat. Kohdepolku annetaan Windows-muodossa (`C:\...`), koska Git Bash muuntaa
`/sdcard/`-alkuisen lähdepolun levypoluksi ja pull epäonnistuu oudolla virheellä.

**Miksi tämä on tässä eikä pelkkänä anekdoottina.** Kolme peräkkäistä mittausta antoi oikean
tuloksen väärään kysymykseen, ja ne johtivat epäilemään jo oikein tehtyä tunnistusta. Väline
jonka katve on tuntematon on huonompi kuin väline jonka katve on kirjattu.

### Yksi ruudullinen ei ole otos listasta, kun lista on järjestetty (29.8.2026)

Edellinen osio sanoo että kaappaus voi jättää näyttämättä jotain mitä ruudulla on. Tämä on
saman virheen toinen muoto ja hankalampi huomata: **kaappaus näytti kaiken mitä ruudulla oli,
ja johtopäätös oli silti väärä**, koska ruutu oli listan alku eikä listan otos.

**Mitattu tapaus.** Laiteajossa otettiin otteluluettelosta yksi kaappaus, jossa kaikilla
näkyvillä riveillä oli `Grace 0:00` korostettuna. Siitä pääteltiin että korostus on joka
rivillä eikä siis erota mitään, ja se kirjattiin havaintona Tommille. Tommi sanoi ettei alas
asti ollut vieritetty, ja vieritys kaatoi päätelmän heti: alempana ovat `1:00`, `1:10`,
`3:26`, `7:39`, `22:55` ja `30:13`, kaikki ilman korostusta. Erottelu toimi täsmälleen niin
kuin `docs/AUDITOINTI-FOORUMI.md` kohta H4 sen kuvaa.

**Miksi virhe ei ollut sattuma.** Otteluluettelon oletusjärjestys on sivuston oma
kiireellisyysjärjestys (`SUBSTANSSI.md` kohta 21), ja `Grace 0:00` on kiireellisin.
Korostetut rivit kasautuvat siis **aina** listan alkuun, ja ensimmäinen ruudullinen on
määritelmän mukaan pelkkää korostusta. Mitä kiireellisempi tilanne, sitä varmemmin yksi
kaappaus tuottaa vaikutelman *"korostus on kaikkialla"*. Järjestys tuottaa siis juuri sen
väärän yleistyksen, eikä väline anna mitään vihjettä siitä.

**Sääntö.** Väitettä *"kaikilla riveillä on X"* ei tehdä yhdestä kaappauksesta silloin kun
lista on järjestetty sen mukaan mitä X on. Joko vieritetään listan loppuun asti, tai väite
rajataan siihen mitä nähtiin (*"ensimmäisillä n rivillä"*). Sama koskee mitä tahansa
korostusta, merkkiä tai tyhjää arvoa joka on samalla lajitteluperuste.

**Yleistys jota tästä ei saa tehdä.** Tämä ei sano että vieritys kuuluu jokaiseen
tarkistukseen. Ulkoasukysymykset (leikkautuminen, välit, kontrasti) ratkeavat yhdestä
ruudullisesta, koska ne koskevat riviä eivätkä listaa. Ehto laukeaa vain kun väite koskee
**kaikkia** rivejä.

### Laitetason vaikutus mitataan jokaisella laiteperheellä (22.–27.8.2026, `appCategory`)

Manifestin `android:appCategory="game"` lisättiin 22.8.2026 ja **poistettiin 27.8.2026**.
Kohta on tässä siksi, että mekanismi ja sen mittaustapa jäävät voimaan vaikka päätös vaihtui,
ja koska kaksi laiteperhettä antoi samasta manifestin rivistä eri seurauksen.

**Mekanismi on mitattu eikä pääteltu** (`adb shell dumpsys package <paketti>`):
`com.dailygammon.DGMobile` antaa `category=0 (manifest=0)`, eli se ilmoittaa itse olevansa
`CATEGORY_GAME`; `fi.tommi.dg`:llä ei ollut `category`-riviä lainkaan. Luokittelu tulee siis
sovelluksen omasta ilmoituksesta eikä valmistajan arvauksesta. `appCategory` on tuettu API
26:sta alkaen ja `minSdk` on 26.

**Samsung (SM-T970): Game Launcher ja Game Booster.** Peliksi ilmoittautuva sovellus päätyy
Game Launcherin listalle, ja siitä seuraa kaksi asiaa: näyttöä pidetään hereillä sovelluksen
ajan (`SCREEN_BRIGHT_WAKE_LOCK`), jolloin järjestelmän oma näytön aikakatkaisu ei laukea, ja
automaattinen kosketussuojaus voi tuoda ruudulle `GAME_TOOL_OVERLAY`-lukon. **Kumpikaan ei
ole Androidin lukitusnäyttö**, joten `isKeyguardShowing` on `false` ja `deviceLocked` on `0`
silloinkin kun lukko on näkyvissä; juuri se tekee ilmiöstä vaikean tunnistaa. Jos jokin
näyttää Samsung-laiteajossa oudolta, Game Booster tarkistetaan ennen kuin syytä etsitään
koodista. Sama koskee avainta `game_edgescreen_touch_lock`, joka palautui laitteella
itsestään arvoon `1` käyttöliittymästä nollattuna: reunapyyhkäisy on Androidissa paluuele.

**Pixel 8a: Googlen oma Pelikojelauta.** Pixelissä ei ole Game Boosteria lainkaan; sama
ilmoitus tuottaa siellä kelluvan paneelin sovelluksen päälle (ruudunkaappaus, FPS-mittari,
häiriöttömyys, pelivideon tallennus). Yhdestäkään ei ole hyötyä kierrospohjaisessa
backgammonissa, ja se oli syy poistoon: menetetään Samsungin hereilläpito, joka oli aito
hyöty, mutta mikään laite ei enää saa päälleen paneelia jota sovellus ei piirtänyt.

**Manifestin puhdistus ei yksin riitä laitteella jolla sovellus on joskus asennettu
peli-ilmoituksen kanssa.** Poiston jälkeen paneeli palasi ruudulle, ja kolme mittausta antoi
oikean tuloksen väärään kysymykseen: `cmd game list-configs fi.tommi.dg` vastasi *"Package
fi.tommi.dg is not of game type"*, eli **Android-kehys oli jo samaa mieltä**. Paneelin
omistaja ei ole kehys vaan **Google Play -palvelut**: SystemUI:n dumpista löytyi sääntö
`Pelipaneeli`, omistajana `com.google.android.gms` ja ehtona `game_platform__gaming`.
Play-palveluilla on oma listansa, jonka se muodosti silloin kun sovellus vielä ilmoitti
olevansa peli, eikä manifestin muutos kerro sille mitään. **Ratkaisu oli sovelluksen poisto
ja uudelleenasennus.** Hinta on paikallinen arkisto ja kirjautuminen, joten sitä ei saa
ehdottaa kevyesti.

**Sääntö joka tästä jää voimaan:** laitetason vaikutus on mitattava jokaisella laiteperheellä
jota käytetään, tai kirjattava sen laitteen nimellä jolla se mitattiin. 22.8. kirjattu
mekanismikuvaus sanoi "Samsung-laitteilla" ja oli siltä osin oikein; virhe oli päätöksessä,
joka tehtiin yhden laiteperheen mittauksella. Yksi asia jää mittaamatta: estikö paneeli
napautuksen vai oliko se vain päällä. Jos kategoria joskus palautetaan, se on ensimmäinen
mitattava.

### Kirjautumisruudun näkee ilman puhdasta asennusta (27.8.2026)

Google Salasanat toimii `LoginScreen`illä: Tommi kirjautui ulos ja sisään, ja täyttö aukesi
itsestään, eli kentillä on automaattitäytön tarvitsema semantiikka.

Mittaustapa on se osa joka kannattaa muistaa. Tässä luki ensin että mittaus vaatii puhtaan
asennuksen, koska havainto tehtiin uudelleenasennuksen yhteydessä. Se ei pitänyt paikkaansa:
**uloskirjautuminen paljastaa saman ruudun**, eli mittaus oli koko ajan halpa ja
toistettavissa. Tilanne jossa jotain nähtiin, ei ole sama asia kuin ehto jolla se voidaan
nähdä.

### Tiedostonvalitsin ei ollut koskaan auennut (1.9.2026)

**Vika löytyi ensimmäisestä oikeasta napautuksesta, ei testeistä eikä katselmuksesta.** Tommi
painoi uutta `Back up` -nappia, ja sovellus katosi kotinäytölle: *"en siis päässyt valitsemaan
mitään."* Kaatuminen oli logcatin kaatumispuskurissa, ja se osui napin omalle riville:

```
java.lang.IllegalArgumentException: Can only use lower 16 bits for requestCode
    at fi.tommi.dg.app.MainActivity$...(MainActivity.kt:505)
```

**Syy on riippuvuuspuussa eikä koodissa.** `MainActivity` on `FragmentActivity`, koska
`BiometricPrompt` vaatii sen. Ainoa `androidx.biometric`in versio joka on saatavilla vakaana
(1.1.0) vetää mukanaan `androidx.fragment:1.2.5`:n, jonka
`FragmentActivity.startActivityForResult` vaatii että pyyntökoodi mahtuu 16 bittiin.
`ActivityResultRegistry` — se jonka päällä `rememberLauncherForActivityResult` on — arpoo
koodit **tarkoituksella sen yläpuolelta**, jotta ne eivät osuisi vanhoihin koodeihin. Kaksi
kirjastoa ovat siis molemmat oikeassa ja yhdessä väärässä.

**Merkitys on isompi kuin uusi nappi: sama vika koski arkiston `Export`ia siitä asti kun se
lisättiin.** Vienti on ollut koodissa, dokumentissa ja yksikkötesteissä, mutta yksikkötestit
kattoivat JSONin eivätkä valitsimen avaamista, eikä nappia ollut painettu laitteella
kertaakaan. Ominaisuus oli siis olemassa ja rikki yhtä aikaa, eikä mikään työvaihe olisi
kertonut sitä ilman napautusta.

**Korjaus on `androidx.fragment` nostettuna käsin 1.8.5:een** (`app/build.gradle.kts`, perustelu
siellä). Biometriikkaa ei tarvinnut vaihtaa eikä aktiviteettia. Todennettu laitteella heti
perään: `Back up` avaa tiedostonvalitsimen nimiehdotuksella `dg-archive.json`, kaatumispuskuri
pysyy tyhjänä, ja `Export` kulkee samaa korjattua reittiä.

**Opetus on menetelmällinen ja se on tämän luvun syy.** Sovelluksen ja käyttöjärjestelmän
rajapinta ei ole yksikkötestattavissa, ja juuri siellä oli vika joka teki napista koristeen.
Sama kaava kuin 1.9.2026 aiemmin löytyneet kuolleet teot päättymissivulla: **testit väittivät
tilasta, eivät siitä että nappi tekee jotain.**

### Varmuuskopion vikatilaa ei saa aikaan poistamalla tiedostoa (1.9.2026)

**Kysymys oli miten vikarivi nähdään ilman että mitään menetetään.** Varmuuskopiorivin kolmesta
tilasta kaksi oli näkemättä, ja epäonnistunut on niistä se joka on koko ominaisuuden syy.
Ilmeisin koe on poistaa kohdetiedosto, ja se ei toimi.

**Mitattu kahdella tavalla, molemmat vaikutuksettomia.** `adb shell rm` poisti tiedoston, ja
`Save now` loi sen takaisin ja kertoi onnistumisesta. Sen jälkeen poistin myös MediaStore-rivin
(`content delete --uri content://media/external/downloads`), ja tulos oli sama: uusi rivi, uusi
tiedosto, onnistunut kirjoitus. Syy on osoitteen lajissa. Valittu kohde on
Downloads-providerin dokumentti (`content://com.android.providers.downloads.documents/document/1625`),
ei MediaStore-rivi, ja provider luo tiedoston takaisin kun siihen kirjoitetaan. Providerin
kautta ei myöskään poisteta `adb`:llä: `content delete` sen osoitteella kaatuu
`SecurityException`iin, koska DocumentsProvider vaatii `ACTION_OPEN_DOCUMENT`in kautta saadun
oikeuden.

**Toimiva laukaisin on kuollut osoite.** Sovellus on debug-versio, joten `run-as` pääsee sen
omiin asetuksiin, ja kohde vaihdetaan olemattomaksi dokumentiksi ilman että oikeaan tiedostoon
kosketaan:

```
adb shell am force-stop fi.tommi.dg
adb shell run-as fi.tommi.dg sed -i 's@document/1625@document/999999@' /data/data/fi.tommi.dg/shared_prefs/dg_backup.xml
```

Sen jälkeen `Save now` epäonnistuu aidosti samaa polkua kuin oikeassa viassa, koska
`openOutputStream` heittää eikä mikään erottele syytä. Palautus on sama tiedosto takaisin
(`adb push` plus `run-as cp`), ja se on syytä ottaa talteen **ennen** koetta, koska vika
tyhjentää osoitteen.

**Kaksi varoitusta menetelmästä.** Asetustiedostoa muokataan vain sovellus pysäytettynä, muuten
muistissa oleva `SharedPreferences` kirjoittaa sen yli. Ja koe on synteettinen laukaisin
oikeaan vikapolkuun: se todistaa mitä ruutu tekee kun kirjoitus kaatuu, muttei sitä mikä
oikeassa käytössä saa kirjoituksen kaatumaan. Jälkimmäinen jää yhä mittaamatta, ja löytö 1
kertoo että se on harvinaisempi kuin luultiin.

**Löytö tuli sivutuotteena, ja se oli isompi kuin koe.** Vikarivi ei näyttänyt aikaa vaikka
`BackupUiState.Failed` kantoi sen ja kommentti lupasi sen, ja `forget` pyyhki ajan kokonaan.
Korjaus ja päätös ovat `docs/AVOIMET.md`:ssä; tämän luvun opetus on että **tila jota ei pääse
laukaisemaan on tila jota ei ole katsottu**, ja juuri siihen jäi lupaus jota koodi ei pitänyt.

### Kuoriproxyn toisto kirjoittaa oikeaan arkistoon (1.9.2026)

**Varoitus jota tässä ei ollut, ja siksi siihen käveltiin.** Kuoriproxy vastaa tallennetuilla
sivuilla eikä päästä mitään sivustolle, ja sen turvallisuusperuste on juuri se. Airlock koskee
kuitenkin vain **ulos menevää** liikennettä. Sovellus itse käsittelee vastauksen tavallisena
sivuna, joten toistettu chat-sivu **arkistoituu kuten oikea**.

Näin kävi laudan korkeuskorjauksen todennuksessa. Sivu `0033` toistettiin klo 21.10.37, ja
arkistoon syntyi toinen rivi viestistä jonka Tommi oli lukenut klo 20.44.58. Sama ottelu, sama
lähettäjä, sama teksti, eri tunniste.

**Eri tunniste ei ole vika vaan seuraus, ja se on oma löytönsä.** `Message.id` sisältää
`receivedAtEpochMillis`-kentän, eli tunniste tunnistaa lukutapahtuman eikä sisältöä. Kahdesti
luettu sama viesti on siis kaksi riviä eikä yksi, ja se kaataa `docs/AVOIMET.md`:ssä olleen
väitteen arkistojen yhdistämisen hinnasta. Löytö tuli sivutuotteena eikä sitä etsitty.

**Rivi jätettiin paikalleen** Tommin päätöksellä 1.9.2026: *"ilman todisteita virheitä jää
luokittelematta"*. Kanta ei siis ole tältä osin puhdas, ja se on tietoista.

**Käytännön sääntö tästä eteenpäin.** Jos toistettava sivu sisältää chatin tai pikaviestin,
tiedä että arkistoon tulee rivi. Se ei ole peruste jättää todentamatta, koska vaihtoehto on
todentamaton ruutu, mutta se on peruste valita toistettava sivu tietoisesti ja kertoa
seurauksesta samassa raportissa.

### Varmuuskopio oli tyhjä, ja rivi sanoi sen olevan tallessa (5.9.2026)

Mittaus tilattiin punnintaa varten (`docs/AVOIMET.md`, Google-synkronointi), ja se löysi vian
sen sijaan. Kysymys oli arkiston koko 25 megatavun kattoa vasten, ja vastaus tuli
ohimennen: **tiedosto oli 114 tavua ja sen `messages` oli tyhjä lista.**

**Mittaustapa, koska se on toistettava.** Kanta luetaan laitteelta `run-as`illa, koska
debug-paketti sallii sen ilman roottia. Laitteella ei ole `sqlite3`-binääriä, joten kanta
haetaan koneelle ja kysytään siellä.

```
adb shell run-as fi.tommi.dg ls -l databases/
adb exec-out run-as fi.tommi.dg cat databases/dg.db > dg.db
```

**Todiste on kahden luvun ero.** Kannassa oli 27 viestiä, tuorein `storedAtEpochMillis`
4.9.2026 klo 17.10. Varmuuskopio oli kirjoitettu klo 17.19, eli yhdeksän minuuttia myöhemmin,
ja siinä oli nolla viestiä. Ruutu luki `Archive backed up 4 Sep 2026 17:19`.

**Syy on virran alkuarvo, ja se on tässä projektissa tuttu vikamuoto.** `MessagesViewModel.
messages` on `stateIn`, jonka alkuarvo on tyhjä lista. Vuorokautinen kopio laukeaa ruudun
avautuessa, eli täsmälleen silloin kun Room ei ole vielä vastannut. Kopio kirjoitti tyhjän
arkiston, leimasi ajan tehdyksi ja esti siten itsensä vuorokaudeksi. Sama laji kuin
`queuePathUpstream`in kertakuva 9.8.2026, ja sama seuraus: virta joka ei ole vastannut
luetaan vastaukseksi.

**Miksi tätä ei nähty 1.9.2026 laiteajossa.** Silloin todennettiin rivin teksti ja tiedoston
aikaleima levyllä, eikä kertaakaan tiedoston sisältöä. `Save now` painetaan ruudun ollessa
auki ja ladattu, joten käsin tehty kopio on aina ollut oikea; vain automaattinen oli tyhjä.
**Aikaleima ei ole todiste sisällöstä.**

**Korjaus ja sen todennus laitteella.** `exportJson` lukee nyt arkistosta
(`archive.observeAll().first()`) eikä ruudun virrasta, ja testi `vienti sisaltaa arkiston
vaikka ruudun virta on viela tyhja` kaatuu vanhalla toteutuksella. Laitteella `Save now`
5.9.2026 klo 2.21 kirjoitti 11 635 tavua ja 27 viestiä, eli täsmälleen sen mitä kannassa on.

### Auto Backup todennettiin kerran, koska sovellus ei näe sitä (5.9.2026)

Sovellus ei voi kertoa Auto Backupin tilaa millään rajapinnalla, mutta adb voi. Tämä on
kertaluontoinen mittaus eikä ominaisuus:

```
adb shell bmgr enabled
adb shell bmgr list transports
adb shell bmgr backupnow fi.tommi.dg
```

Tulos 5.9.2026. Varmuuskopiointi on päällä, aktiivinen kuljetin on Googlen
`com.google.android.gms/.backup.BackupTransportService`, ja ajo päättyi tulokseen `Success`
siirrettyään 948 736 tavua. **Kopio on siis oikeasti olemassa Google-tilillä**, ei vain
asetuksena.

**Koko suhteessa kattoon.** Sovelluksen data on 608 kilotavua (`dg.db` 69 632, WAL 420 272,
shm 32 768), ja siirretty kokonaisuus 948 736 tavua. Auto Backupin katto on 25 megatavua,
eli käytössä on noin 3,6 prosenttia. Viennin JSON on 431 tavua viestiä kohti, joten katto
tulisi vastaan vasta kymmenissä tuhansissa viesteissä.

### Todentamatta 4.9.2026 illan jälkeen: kuusi kohtaa ehtoineen

Lista on tässä eikä chatissa, koska sen käyttöhetki on seuraavan session alku. Kolme
ensimmäistä ei vaadi yhtään peruuttamatonta tekoa, kolme viimeistä vaatii.

**Pelisessio 5.9.2026 ei siirtänyt tätä listaa** (`raakasivut/sessio-5-9`, kaksi ottelua).
Kuutiotekoa ei tullut tarjolle, noppien painalluksen kytkimet olivat pois päältä eikä
luovutusta tehty, joten kohdat 1, 4 ja 5 ovat yhä ehtonsa varassa. Sessio todisti sen mitä
se saattoi: jokaista noppaa kohti lähti täsmälleen yksi `Submit Move`, eikä yhtään tekoa
lähtenyt kahdesti.

**Tila 4.9.2026 yöllä: kaksi kuudesta ajettu** (kohdat 2 ja 3, Pixel 8a, proxy
`raakasivut/sessio-4-9-todennus`). Otsikon luku kertoo mistä lähdettiin eikä sitä mitä on
jäljellä. Neljä muuta odottaa omaa ehtoaan, ja kolme niistä vaatii oman vuoron laudalla.

**1. Kuutiodialogi, `Double` ja `Accept`.** Yksi kuudesta `ConfirmDialog`-kutsupaikasta
(4.9.2026). Ehto on että sivusto tarjoaa kuutiotekoa, eli sitä ei voi tilata. Kulku on
kolme elettä: sivun oma rasti, nappi, dialogi. Katso että otsikko ja teksti ovat
kuutiotekstit, vahvistusnappi kantaa **sivun oman sanan** (`Double` tai `Accept`) ja
peruutus on `Cancel`. Peruminen ei saa lähettää mitään, ja se on todennettavissa proxyn
lokista: rivin pitää puuttua, ei riitä että ruutu näyttää samalta.

**Lokipuoli ajettu 7.9.2026 (`raakasivut/sessio-7-9`): kolme `Double`a ja yksi `Decline`,
kaikki rastin kanssa, ei yhtään `not verified` -riviä.** Peruutusta ei tullut, joten lokin
puuttuva rivi on yhä mittaamatta. Sanoista Tommi ei muista mitään; hän katsoi kuution
sijainnin ja sen että napit erosivat heiton napeista. Sanat odottavat siis yhä seuraavaa
kuutiotekoa, ja kysymys on sanottava ennen sessiota eikä sen jälkeen.

**Yksi `Double` lisää 8.9.2026 (`raakasivut/sessio-8-9-yo2`, rivi 23), rastin kanssa.**
Logcat näytti dialogin auki kolme sekuntia (0.55.54–0.55.57) ennen lähetystä. Dialogi
ehti tulla ruudulle eikä vahvistus mennyt sen ohi. Sanat jäivät taas kysymättä ennen
sessiota. Peruutus on yhä mittaamatta.

**Sanat ja peruutus molemmat 8.9.2026 illalla (`raakasivut/sessio-8-9-ilta`), ja kysymys
sanottiin tällä kertaa ennen sessiota.** Tommi pyysi kaappaamaan dialogin egis-ottelussa
klo 18.48 (`nauhat/kuutiodialogi-184806.png`): otsikko `Double?`, teksti *This sends Double
to your opponent right away. It cannot be taken back.*, napit `Cancel` ja `Double`. Hän
painoi `Cancel`, ja loki todistaa sen: rivien 36 (18.46.08, `Submit Move`) ja 37 (18.49.00,
`Roll Dice`) välissä ei ole yhtään riviä, eli peruutus ei lähettänyt mitään. Rivin 37
`Roll Dice` kantoi `verify=Double`-parametrin, koska rasti jäi päälle; sivusto ei
välittänyt siitä. `Accept`-dialogi todennettiin kuorella session jälkeen
(`nauhat/kuori-accept-dialogi.png`): otsikko `Accept?`, kuutioteksti, napit `Cancel` ja
`Accept`. Sen sanat olivat sessiossa itsessään väärät (peruutusrunko, ks. `docs/UI.md`
8.9.2026), ja Tommi ei lukenut niitä hyväksyessään. Kohta on ratkennut lukuun ottamatta
`Cancel`ia `Accept`-dialogissa, jota ei ole vielä painettu oikealla sivulla.

**2. Palstan lähetyksen vahvistus.** Toinen kutsupaikka, ja **ainoa jonka peruutusnappi ei
ole `Cancel` vaan `Not yet`**, joten se on koko noston ainoa poikkeus. Tämä on ajettavissa
ilman postausta: avaa kommenttikenttä ketjuun jolla ei ole New-merkkiä, kirjoita tekstiä,
paina `Post`, ja paina `Not yet`. Lokissa ei saa näkyä `submitadd`-riviä.

**Ajettu 4.9.2026 klo 23.44, ja odotus piti.** Ketju oli `Firefox` (ei New-merkkiä),
teksti `Verification+draft`. Dialogi sanoi *Post to the forum?*, teksti nimesi julkisen
palstan ja peruuttamattomuuden, ja napit olivat `Not yet` ja `Submit Comment`. Peruutus
palautti lomakkeen teksti tallella. **Loki ei kasvanut lainkaan** painalluksen yli (13
riviä ennen ja jälkeen), eli `submitadd`-rivin puuttuminen on mitattu eikä pääteltu
ruudusta.

**3. Profiilin `Invite` ja `Ignore`.** Kolmas ja neljäs kutsupaikka, ja molemmat ajettavissa
peruuttamatta mitään. Reitti on Lounge → Players → haku nimellä → profiili. `Invite` avaa
lomakkeen, ja dialogin tekstin pitää nimetä pelaaja, laji, pituus ja aikakontrolli sivun
sanoin. `Ignore`-dialogin otsikko **ja** vahvistusnappi ovat molemmat sivun napin teksti.
Peru molemmat; lokissa ei saa olla `invite/new`- eikä `ignore?changeto`-riviä.

**Ajettu 4.9.2026 klo 23.47, ja molemmat dialogit sanoivat sen mitä piti.** Kohteena oli
listan ensimmäinen pelaaja, ja reitti oli Lounge → Players → nimi; hakukenttää ei tarvittu,
koska luettelo itse riittää profiiliin. `Invite`-dialogi sanoi *Invite <nimi>?* ja teksti
nimesi lajin, pituuden ja aikakontrollin sivun sanoin (*backgammon, length 5, time control
Never*) sekä sen ettei kutsua saa takaisin. `Ignore`-dialogin otsikko ja vahvistusnappi
olivat molemmat sivun napin teksti, ja peruutus oli kummassakin `Cancel`. Molemmat
peruttiin, ja loki pysyi 16 rivissä: **nolla osumaa** hakuun `submitadd|invite/new|ignore?changeto`.

**Yksi sivuhavainto samalta ajolta.** Kutsulomake avautui ilman uutta hakua, eli se
piirretään jo haetusta profiilisivusta. Se on sama laji kuin paluun luettelon lukeminen
saadusta vastauksesta, ja se näkyy lokissa suoraan: `/bg/user/39579` on ainoa rivi jonka
koko kutsun kulku tuotti.

**4. Noppien painallus tekona.** Toteutettu 4.9.2026, asennettu, ajamatta. **Molemmat
kytkimet ovat oletuksena pois**, joten tämä ei laukea itsestään: Settings → *Tapping the
dice*. `Submit the move by tapping the dice` lähettää täyden vuoron ilman vahvistusta, eli
lokissa pitää olla täsmälleen yksi `?move=…&submit=Submit+Move` painallusta kohti.
`Swap the dice by tapping them` toimii vain koskemattomilla nopilla eikä mene verkkoon
lainkaan, eli lokissa **ei saa olla riviä**, ja saman painalluksen toisto kääntää
järjestyksen takaisin. Kaksi rajaa jotka on erikseen katsottava: vastustajan lokero ei ota
painallusta vastaan, eikä sivun oma lauta (lukutila) reagoi.

**Tommin kuittaus 8.9.2026 illalla, kesken pelisession:** *"noppien painallus toimii kuten
pitää"*. Molemmat kytkimet olivat päällä, ja sessiossa oli 88 `Submit Move` -riviä ja 65
`Roll Dice` -riviä ilman yhtään kaksoislähetystä. Loki ei erota painallusta napista, joten
kuittaus on ainoa todiste ja se riittää: kysymys oli tekeekö painallus sen mitä lupaa.

**5. Luovutuksen lopputulos sivuston sanoin.** Vaatii oikean luovutuksen, joten se odottaa
tilannetta jossa luovutat joka tapauksessa. Ruudun pitää sanoa `Matches resigned.
DailyGammon says: <N> match(es) resigned.` eikä vanhaa `Resigned N match(es)` -tekstiä;
jälkimmäinen tarkoittaisi että kuittaussivua ei tunnistettu ja luku tuli kadonneista
riveistä.

**Ajettu 14.9.2026 klo 20.19 (`raakasivut/sessio-14-9-luovutus`), Tommin luovutus omaa
testitiliään vastaan. Lokipuoli meni läpi, ruutu ei.** POST `doit` kentillä `5316601,
reellysure`, kuittaussivu tavulleen fixturen muotoinen (`1 match resigned.`), GET heti
perässä ja rivi poissa (29 → 28). Mutta ilmoitusta ei nähty: se piirretään otsikon alle ja
käyttäjä on napin luona luettelon pohjalla, joten nauhalla näkyi vain `Sending to
DailyGammon.` (luettelo hyppäsi hetkeksi alkuun) ja sen jälkeen 28 riviä. Sanamuoto on
yksikkötestissä, laitteella se odottaa ilmoituksen siirtoa katseen kohdalle
(`docs/AVOIMET.md`).

**Ruutu todennettu kuorella samana iltana klo 20.29**, ilmoituksen siirron jälkeen
(`kuoriproxy.py --resign`, illan omat sivut `0002` ja `0003`, airlock 503 todennettu):
napin alla lukee *Matches resigned. DailyGammon says: 1 match resigned.* ja `OK`
(`sessio-14-9-luovutus/nauhat/kuori-ilmoitus-napin-alla.png`). Kohta on ratkennut: loki
oikealla teolla, lause laitteella kuorella. Sääntö `docs/UI.md` › Lomakkeen lopputulos
napin alle.

**6. Palstan uuden ketjun vastaus (`submitnew`).** Ainoa kolmesta kuittaussivusta joka on
yhä mittaamatta. **Ei testipostausta**: odottaa hetkeä jolloin sinulla on oikeasti ketju
aloitettavana, ja proxy on silloin päällä ennen lähetystä.

**Ja yksi joka koskee eri laitetta.** Palkkien piilotuspäätöksen lukitus (4.9.2026) on
todennettu tabletilla, jossa ehto on **epätosi**: palkit ovat vapaat, joten piilotusta ei
tehdä lainkaan. Puhelimella (Pixel 8a) ehto on tosi, eli lukituksen toinen haara on
ajamatta. Odotus on että palkit piiloutuvat laudalle kerran ja palaavat paluussa kerran,
eikä lokissa ole enempää kuin yksi `hide` ja yksi `show`.
