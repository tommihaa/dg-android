# Käyttöliittymä

Lautanäkymä, mitat, asetusnäkymä, otsikkorivi, sivupaneeli, viestiruutu ja kännykän
näytön työlista.

Siirretty `CLAUDE.md`:stä 10.8.2026. Sisältö on ennallaan, vain sijainti muuttui.
Hakemisto ja pyytämättä laukeavat portit ovat yhä `CLAUDE.md`:ssä.

### Termien valinta (Tommin kirjaus 27.9.2026)

**Sovelluksen oma käyttöliittymätermi valitaan lyhyeksi, yksimerkityksiseksi ja
käännettäväksi. Sivuston sanat pidetään sivuston muodossa.**

Otteluluettelon segmentti oli tämän periaatteen esimerkki 28.9.–3.10.2026 nimellä `Your move`
(27.9. se oli `Playable`). **3.10.2026 Tommi yhtenäisti segmentin ja rivien merkinnän sivuston
sanaan `Your turn`** (*"Välilehden Your move-nimi ja Your turn-toiminto molemmat -> Your
turn"*): sama asia sanotaan ruudulla yhdellä sanalla, ja se on sivuston oma. Segmentti on siis
nyt sivuston sana eikä oma termi. Sivuston sanoja ovat esimerkiksi `Grace`, `Roll Dice`,
`Submit Move` ja `Your turn`. Niitä ei käännetä käännöksessäkään, koska käyttäjä vertaa sovellusta sivuun
rinnakkain (`docs/AVOIMET.md` › kielivalikoima 5.8.2026).

Peruste on käännöspolku. Käyttöliittymä on nyt pelkkää englantia, mutta kaikki teksti on
`strings.xml`:ssä, joten kieli lisätään kääntämällä eikä koodia muuttamalla. Lyhyt
yksimerkityksinen termi on halpa kääntää, idiomi ja pitkä lause eivät ole.

**Väitettä vartioi `I18nGuardTest` 28.9.2026 alkaen** (Tommin tilaus). Se kaatuu, jos
tekstiparametrissa on kirjaimia sisältävä literaali tai jos `stringResource`a liimataan `+`:lla,
koska liimaus kiinnittää sanajärjestyksen koodiin. Merkkiliitteet (lajittelun nuoli,
Crawford-tähti ja manuaalin numerointi) ovat testin poikkeuslistassa perusteluineen. Samalla
laudan `Mark`-nappi siirtyi muotoon `board_mark_add_move` (`Mark %1$d`). Lint ei käynyt
vartijaksi, koska `HardcodedText` ei tunne Composea. Vartija todennettiin mutaatiolla, ja se
kaatui molempiin sääntörikkeisiin.


### Lautanäkymä (`app/ui/Board*`, lisätty 5.8.2026, portti purettiin 10.8.2026)

**Osion kolme ensimmäistä kohtaa kuvaavat lukutilaa, joka päättyi 10.8.2026.** Ne ovat tässä
sellaisinaan, koska ne kertovat mitä purkamisella maksettiin ja mitä tilalle oli pakko
rakentaa. Nykytila on omassa alaosiossaan alempana; jos nämä kaksi ovat ristiriidassa, alaosio
voittaa.

- **Lukutila on portti eikä aikomus.** `BoardViewModel` tarkistaa polun ennen ensimmäistä
  pyyntöä (`/bg/move/`-alku, ei kyselyparametria), ja kelpaamattomalla polulla **pyyntöä ei
  tehdä lainkaan**. Ehto on rakenteellinen eikä luetteleva, joten se sulkee myös ne muuttavat
  muodot joita emme vielä tunne. `BoardViewModelTest` väittää tämän muodossa "pyyntö ei
  lähtenyt" eikä muodossa "nappia ei painettu".
- **Oma näkymämalli eikä `TopViewModel`in laajennus**, ja syy on vahvempi kuin testihygienia:
  `TopViewModel` väittää hakevansa oma-aloitteisesti vain Top Pagen, ja `TopViewModelTest`
  todistaa väitteen kirjaamalla jokaisen polun. Toinen haku samassa luokassa muuttaisi
  todistetun väitteen kommentiksi. Erillään kummankin polkulista on täydellinen lausuma.
- **Ruudulla lukutila on käännösaikainen tae:** `BoardScreen`in signatuurissa on vain
  `onRefresh` ja `onBack`. Composable joka ei saa toimintoa parametrina ei voi tehdä
  toimintoa. Mallin `moves`, `commands`, `form`, `undoHref` ja `Skip Game` jätetään
  käyttämättä, eikä `moves` piirry edes korostuksena: korostus houkuttelisi seuraavan
  lukijan tekemään siitä klikattavan.
- **Neljä tapaa joilla 200 OK ei ole pyydetty lauta**, kukin mitattuun tapaukseen sidottuna:
  ei lautaa (`isBoardPage` tai `parse` sanoo ei), eri ottelu, numeroimaton asema
  (Mini-skeema), kelvoton polku. Nämä ovat eri tila kuin verkkovirhe, koska sama haku ei
  korjaa niitä. `PositionUnreadable` on **käyttöliittymän luokittelu eikä jäsentimen
  sopimus**: `parse`n null-kysymys pysyy avoimena, ja `BoardParserSchemeTest` on
  koskematon. Luokittelu on silti yksiselitteinen näyttötarkoituksessa, koska
  backgammonissa 30 nappulaa on aina jossain.
- **Suunta on X-22-tilassa kiinteä, 13-24 ylhäällä; sivustouskollinen tila peilaa
  asetuksesta (25.8.2026).** Numerot tulevat mallista ja jäsennin on yhä normalisoinut
  `Home boards on left siden` pois; peilaus tehdään `BoardLook`issa säilötyn asetuksen
  perusteella eikä sivun asettelusta päättelemällä, joten vanha kielto (*peilaus olisi sen
  uudelleen päättelemistä minkä jäsennin hävittää*) koskee edelleen laudalta päättelyä.
  Aiempi seuraus (asetuksen kanssa sovellus ja selain eivät ole vertailtavissa) poistui
  sivustouskollisesta tilasta, ja juuri sitä varten tila on olemassa.
- **Pip-vahti näkyy ruudulla aina, ja ristiriita punaisena.** Se on sovelluksen ainoa
  automaattinen tunnistin hiljaa väärään menneelle jäsennykselle, ja debug-lipun taakse
  piilotettuna se hukkaisi juuri sen mitä varten se on. `PlayerPanel.backgroundColor` sen
  sijaan **ei** ole vuoromerkki ruudulla, koska se on kanonissa hypoteesi ja ruudulle
  maalattu hypoteesi muuttuu väitteeksi.
- **Jäsennys ajetaan `io`ssa.** Lautasivu on `Jsoup.parse` plus tusina valitsinta, ja
  numerointi kävelee vanhemmat jokaista kuvaa kohti. Se on liikaa työtä Mainille, ja Main
  saa vain valmiin muuttumattoman tilan.

  **`TopViewModel` teki saman 20.8.2026, ja se oli tämän kohdan viimeinen avoin puolisko.**
  Haku oli siellä jo `io`ssa, mutta `TopPageParser.parse` ajettiin Mainissa; nyt se on
  omassa `withContext(io)`-lohkossaan. Muutos on kolme riviä eikä se koske tilalogiikkaan:
  `onFailure` lukee `_state.value`n yhä Mainissa, ja `credentials.clear()` jäi sinne missä
  se oli. Tämä on tietoinen rajaus eikä puolitiehen jäänyt siirto, koska kumpikaan ei ole
  jäsennystyötä ja `_state`in lukeminen kahdesta säikeestä olisi uusi asia perusteltavaksi.

  **Todennus: `gradlew test` läpi ja `installDebug` Galaxy Tab S7+:lle 20.8.2026**, eli
  laiteajo tehtiin samassa sessiossa eikä jätetty roikkumaan. Yksikkötestiä joka vahtisi
  jäsennyksen säiettä ei kirjoitettu, ja syy on sama kuin lautanäkymässä: `TopPageParser`
  on objekti eikä injektoitu riippuvuus, joten testi joutuisi väittämään säikeen nimestä.
  Sijoittelu on siis luettava koodista, ei tarkistettavissa koneellisesti.
- **Piirto on tavallisia composableja eikä Canvasia**, ensisijaisesti koska Canvas ei jätä
  semantiikkapuuta: sen sisällöstä ei sano mitään testi, ruudunlukija eikä kuvavertailu.
  Kuvakirjastoa ei tarvittu lainkaan, koska nappula on ympyrä ja luku. Sivuston omia
  GIF-kuvia **ei haeta**: ne kulkisivat `DgClient`in ohi, jolloin sekä keksit että
  pyyntötahdin lattia jäisivät pois ja 2011-Apachelle lähtisi kymmeniä rinnakkaisia
  pyyntöjä yhtä lautaa kohti.
- **Reittikoodaus on tämän osuuden ainoa oikea riski, ja siksi omassa tiedostossaan.**
  Pelipolku sisältää kauttaviivoja ja reitti erottaa osansa kauttaviivalla, joten polku
  koodataan matkan ajaksi. Se on juuri sitä osoitteen käsittelyä jota projekti muuten
  välttää. Riski ei ollut poistettavissa `navigation-compose`a käyttäessä, joten se
  muutettiin väitteeksi: `DestinationsTest` vaatii polulta merkilleen saman merkkijonon
  takaisin. Kirjasto purkaa argumentin itse, joten `MainActivity` **ei pura sitä uudelleen**;
  kaksi purkua rikkoisi prosenttimerkin sisältävän arvon.
- Ottelurivi on klikattava **vain kun `playPath` on olemassa.** Profiilisivun otteluilla se
  on null, ja klikattavalta näyttävä rivi joka ei ole olisi huonompi kuin rivi joka ei näytä
  klikattavalta.
- Otteluluettelon näkymämalli elää **activityn tasolla eikä reitin**, jotta paluu laudalta ei
  laukaise uutta Top Page -hakua. Lauta sen sijaan on reittikohtainen, eli saman ottelun
  uudelleenavaus hakee uudestaan. Yksi pyyntö on kohtelias hinta tuoreudesta, eikä tätä pidä
  "korjata" välimuistiksi ilman syytä.
  **Poikkeus 16.9.2026 (Tommin päätös, mitattu tapaus `raakasivut/LUEMINUT.md` › *Otteluluettelo
  ei päivity paluussa*): jos laudalla lähetettiin teko, käsin tehty paluu hakee luettelon
  uudestaan** (`refresh(matchesKnownStale = true)`, sama odotusruutu kuin jonon tyhjentyessä).
  Sääntö yllä on kirjoitettu paluulle jossa laudalla ei tehty mitään, ja silloin haku olisi
  turha. Teon jälkeen rivi on varmasti vanhentunut, ja vanhentunut `Your turn` -rivi johti
  16.9. lukutilan lautaan ja turhaan `Skip Game`en. Haku on `/bg/top`, joka ei kuluta jonoa.
  Katsomiskäynti ilman tekoa ei hae, eli säännön hinta ei muutu siinä tapauksessa jota
  varten se kirjoitettiin.
  **Toinen poikkeus 18.9.2026 (Tommin tilaus monen pelisession jälkeen): paluu toisesta
  sovelluksesta luettelon ollessa auki hakee sen uudestaan**, kuten DG Mobile tekee. *"Kun
  vaihtaa takaisin sovellukseen ja Matches-näkymä on auki, niin ottelulistan tulisi
  päivittyä."* Hetki luetaan aktiviteetin `ON_RESUME`sta (`RefreshOnForeground`,
  `MainActivity.kt`), ja ensimmäinen tapahtuma ohitetaan, koska tarkkailija saa sen heti
  koostuessaan: se osuisi käynnistykseen ja jokaiseen paluuseen laudalta, jotka on säädetty
  yllä. Päätös haetaanko on `TopViewModel.onForeground`in, ja se hakee vain kun lista on jo
  ruudulla eikä haku ole kesken; virhetila jää virheelleen (palvelinvirhe ja nukkuminen eivät
  korjaannu vaihdosta, katkon hoitaa `onNetworkAvailable`). Rivit pysyvät ruudulla haun ajan,
  koska lista voi olla ajan tasalla ja tyhjennys olisi välähdys. Haku on `/bg/top`.

#### Portti purettiin, ja tae siirtyi ruudulta tyyppiin (10.8.2026)

Tommin päätös, ja se tehtiin kahdessa vaiheessa koska ensimmäinen muotoilu kaatui
mittauksessa. Ks. avoimien listan kohta pelaamisesta, jossa mittaus on kirjattu.

**Hyväksytty ehtopari oli osoitteen alkuperä ja HTTP-metodi. Jälkimmäinen mitattiin vääräksi
ennen ensimmäistä koodiriviä**, koska lautasivun jokainen toimintolomake on `method=get`.
Jäljelle jäi alkuperä yksin, ja se joutui kantamaan myös sen osan jonka toinen olisi kantanut.
Se ei riitä sellaisenaan lomakkeelle: lomakkeesta osoite on pakko koota kolmesta palasta,
joten "älä koota" ei ole siinä noudatettavissa. Ratkaisu on että **lomake ei ole osoite vaan
jakamaton yksikkö**, ja ainoa vapaa valinta on mikä sivun omista napeista painetaan.

Portti on nyt kolmessa kohdassa, ja kukin niistä on eri lajin tae:

| Kohta | Mikä estyy | Millä |
|---|---|---|
| `FormSubmission` (`core-domain`) | lähetyksen kokoaminen | `internal` konstruktori: ainoa tie on `BoardForm.press` |
| `BoardViewModel.follow` | keksityn osoitteen hakeminen | jäsenyys `BoardState.ownLinks()`issa, tarkistettuna lähetyshetkellä |
| `BoardViewModel.press` | keksityn napin painaminen | `submit !in submits` palauttaa nullin, eikä nullia lähetetä |

**Käännösaikainen tae ei kadonnut vaan vaihtoi paikkaa.** `BoardScreen`in signatuurissa oli
lukutilan ajan vain `onRefresh`, eli ruutu ei voinut tehdä tekoa koska sillä ei ollut mitään
millä. Sellaista taetta ei voi olla ruudulla jolla siirto tehdään. Nyt tae asuu lähetettävässä
arvossa: ruutu saa takaisinkutsut, mutta se ei voi rakentaa sitä mitä lähetetään. Tae on siis
siirtynyt kohteesta joka sen sattui kantamaan siihen kohteeseen jota se koskee, ja se on
vahvempi paikka, koska se pätee jokaiselle tulevallekin kutsupaikalle eikä vain tälle ruudulle.

**Ajoaikainen todistus vaihtoi muotoa samoin.** `BoardViewModelTest` väitti "pyyntö ei
lähtenyt" kyselyllisestä polusta. Väite on yhä olemassa mutta koskee vain **sisäänkäyntiä**,
eli sitä osoitetta jolla ruutu avataan; se on portin ainoa purkautumaton puolikas, koska
sisäänkäynnin osoite tulee navigaation läpi merkkijonona eikä laudan omasta linkistä.
Kokoamisen ja lähettämisen todistukset ovat uusia ja samaa muotoa: keksitty osoite ei tuota
pyyntöä, keksitty nappi ei tuota lähetystä, vanhentunut linkki ei kelpaa uudella laudalla.
16 testiä `BoardViewModelTest`issä, 7 `BoardFormPressTest`issä.

**Kolme seurausta jotka eivät ole ilmeisiä.**

`DgClient`in uusintaraja siirtyi HTTP-metodista pyynnön tarkoitukseen (`Intent.FETCH` ja
`Intent.ACT`). Ilman tätä `Submit Move` olisi lähtenyt uusivalla asiakkaalla, eli teko olisi
voinut mennä kahdesti. Vika olisi ilmennyt vasta katkenneella yhteydellä, eli harvoin ja
pahimmalla hetkellä.

Vahvistusruutua ei rastita puolesta. Sivulla lukee "Verify Double" tai "Verify Accept", ruutu
on rastittamaton, ja sen tarkoitus on olla toinen ele ennen peruuttamatonta tekoa. Sovellus
piirtää saman ruudun ja jättää sen käyttäjälle. Rastin tila nollautuu kun lomakkeen `action`
vaihtuu, koska seuraava lomake on eri kysymys.

**Rasti on pakollinen ja dialogi tulee sen jälkeen (Tommin päätös 3.9.2026).** Kuutioteko
(`Double`, `Accept`) on kolme elettä: sivun oma rasti, nappi, dialogi. Rastittamaton
painallus ei lähetä eikä kysy vaan näyttää kortin *"Tick Verify Accept first. Nothing was
sent."*, ja rasti kuittaa kortin pois. Kuution klikkaus laudalla lukee saman rastin kuin
nappirivi ja paneelipino, joten rastin tila asuu ruudun tasolla (`VerifyBox`) eikä rivillä.
Peruste on Tommin: *"tuplaus on hidas tapahtuma, näin välttyy vahingoilta."* Tämä korvaa
31.8.2026 version, jossa rastittamaton `Double` avasi dialogin ja lähetti rastitettuna eli
ohitti ruudun. **Kun sivulla ei ole rastia (pelaajan asetus pois), nappi lähettää suoraan**
(Tommin päätös 26.9.2026, `SUBSTANSSI.md` kohta 102): dialogi vahvisti siihen asti vastoin
pelaajan valintaa, ja Tommi huomasi sen `sessio-26-9-ilta2`:ssa. **Laudan kuutio kysyy aina**,
myös ilman ruutua, koska napautus laudalla on sovelluksen oma oikotie ja vahinko on siinä
helpompi (Tommin valinta samana päivänä). `Decline` lähtee ilman kumpaakaan, koska sivu ei vaadi sille rastia. Sääntö on
`cubeActionFor`, ja se on testattavissa ilman Composea (`CubeConfirmTest`).

Epäonnistunut **teko** näkyy, epäonnistunut **päivitys** ei. Päivityksellä vanha lauta jää
ruudulle, koska minuutin takainen asema on vanhentunutta mutta oikeaa tietoa. Teolla sama
käytös olisi vaarallinen: ruudulle jäänyt lauta on juuri se mitä käyttäjä lukisi todisteeksi
siitä että siirto meni perille.

~~**Laiteajoa ei ole tehty.**~~ **Vanhentunut 10.8.2026.** Kappale sanoi ettei `adb devices`
löydä laitetta, ettei ruudun asettelua ole nähty ja ettei yksikään siirto ole mennyt tätä
koodia pitkin oikeaan otteluun. Kaksi ensimmäistä ovat vanhentuneet: laiteajo on tehty sekä
Pixel 8a:lla että tabletilla, ja se on tuottanut neljä korjausta (`18eb6dc`, `2788c79`,
`860ba2f`, `5d26a7d`) joista yhtäkään ei olisi löytynyt testeistä eikä logcatista. Kolmas on yhä totta, ja se on kirjattu kokoamisen ja lähetyksen omaan
kohtaan avoimien listalla.

Yksi ohje kappaleesta jää voimaan sellaisenaan: **ensimmäinen oikea lähetys tehdään ottelussa
jossa siirto on merkityksetön**, koska sen jälkeen sitä ei saa takaisin.

### Lautanäkymän osien nimet (sovittu 5.8.2026)

Palautekierrokset laudan ulkoasusta törmäsivät toistuvasti samaan ongelmaan: sana kuten
"muuri" tai "kiila" viittasi eri kertoina eri koodikohtaan. Nimet kiinnitetään tähän, koska
kolmas kerta on sovittu käytäntö ([TYÖTAVAT.md](../Kaanon/TYÖTAVAT.md) "Jaettu sanasto").
Composable-nimi on sama kuin termi, joten hakea voi joko sanalla tai `grep`illä.

| Termi | Composable | Mikä se on |
|---|---|---|
| kiila | `PointWedge` | Yhden pisteen kolmio + nappulapino |
| numerorivi | `NumberRow` / `NumberGroup` | Pisteiden numerot pelialueen ulkopuolella |
| pelialue | `Board`in sisempi `BoxWithConstraints` (`Palette.Felt`) | Kiilojen ja muurin tumma tausta |
| lokero | `OffTray` | Ulos kannettujen oma laatikko, pelaajakohtainen |
| muuri, muurin segmentti | `BarSegment` | Pystysuora tili pisteiden 18/19 ja 7/6 välissä, ylä-/alapuolikas erikseen |
| keskitila | `MiddleStrip` | Vaakarivi puoliskojen välissä: nopat ja pip-vahti |
| tuplauskuutio | `CubeFace` | Omistettu omistajan pelaajakortissa kun paneeli on, muuten muurin päässä omistajan puoliskolla (`BarSegment`, 14.9.2026); omistamaton aina lokerosarakkeen keskikaistalla (`CubeBadge`, 29.9.2026, ennen sitä kaistan muurilla); tarjottu oman puoliskon keskellä (3.9.2026), lokerosarakkeessa kun napit ovat kaistalla |
| sivupaneeli | `SidePanel` | Pelaaja1/ottelutiedot/pelaaja2 laudan vasemmalla (2.9.2026 alkaen, aiemmin oikealla) |
| pelaajapaneeli | `PlayerPanelView` | Yhden pelaajan nimi, tulos, pipit |

Jos osa ei ole listalla, tarkin tapa on ruutukaappauksen ympyröinti tai rajaus ennen
liittämistä: se osoittaa kohteen yksiselitteisesti ilman että sanaa tarvitsee arvata.

### Ulkoasu: Monte Carlo X-22 ja johdetut mitat (8.8.2026)

Lauta on **Monte Carlo X-22**, Tommin valinta oikeasta laudasta. Ulkoasu suunniteltiin
Claude Designissa ja tuotiin tänne `app/ui/DgBoard.kt`:na.

**Asema muuttui 24.8.2026: X-22 on jatkossa sovelluksen oma valinta eikä ainoa lauta.**
Tommin päätöksellä oletukseksi tulee sivustouskollinen lauta pelaajan omilla
DailyGammon-asetuksilla, ja tämän osion päätökset (roolivärit, kiinteä orientaatio,
vasenkätisyyden normalisointi) siirtyvät X-22-tilan ominaisuuksiksi. **Toteutettu
25.8.2026:** tilat ratkaisee `BoardLook` (`app/ui/BoardLook.kt`), sivuston skeemavärit on
mitattu kuvatiedostoista `DgBoard`iin, ja paikallinen valinta asuu asetusruudun
laiteosiossa. Päätöshistoria on `docs/AVOIMET.md`:n kärjessä. Neljä asiaa kannattaa tietää,
koska ne eivät ole pääteltävissä tiedostosta itsestään.

**Designista siirtyi vain se osajoukko jonka Compose osaa kuluttaa.** Design tuotti kuvan
ja web-koodia; tänne tuli suhdeluvut ja värit. Kaikki muu Designin puolella on kuvitusta
eikä totuuden lähde. Kansiokiinnitys Designiin toimii (private-repo ei näy sen
GitHub-listassa, ks. muistin `reference_claude_design`), mutta paluusuunta kulkee tämän
tiedoston kautta eikä koodina.

**Mitään mittaa ei kirjoitettu korkeudeksi.** `Board` mittaa kahdesti ja kummallakin on oma
syynsä: ulompi `BoxWithConstraints` antaa leveyden, josta sarake on murto-osa, ja sisempi
antaa korkeuden vasta kun numerorivit ovat vieneet omansa. Chromea ei siis vähennetä
vakiona missään kohtaa. Designin oma budjetti olisi vähentänyt 76 dp, mikä on vähemmän
kuin pelkkä Material3-yläpalkki, ks. avoimien päätösten mittakohta.

**Nappulan väri on X-22-tilassa rooli eikä sivuston väri.** Kerma on kirjautunut pelaaja,
punainen vastustaja. Seuraus jonka pitää olla tiedossa on sama kuin kiinteällä
orientaatiolla: sovellus ja selain eivät ole tässä tilassa värillisesti rinnastettavissa.
**Sivustouskollisessa tilassa (25.8.2026) väri on sivuston väri**, mitattuna skeeman
omista kuvista, ja roolivärin menetys siellä on tarkoitus: juuri se palauttaa
rinnastettavuuden.

Rooli tunnetaan vain kun pip-vahti täsmää. **Kun `selfCheckerColor` palauttaa nullin, väri
annetaan sivuston värin mukaan** (keltainen saa kerman, sininen punaisen) eikä sijainnin
mukaan. Molempien piirtäminen neutraalilla olisi poistanut pelaajien erottelun kokonaan,
mikä on huonompi kuin se ettei sovellus tässä tilassa väitä kumpi pelaaja on käyttäjä itse.
Syy näkyy samalla ruudulla, koska pip-vahdin rivi kertoo miksi tarkistus ei onnistunut.

**Ulos kannetut ovat nyt pelaajakohtaisia lokeroita laudan sisällä**, vastustaja ylhäällä ja
itse alhaalla, eivät värilegendarivi laudan alla. Vanha muoto iteroi `CheckerColor.entries`
eikä siksi olisi voinut esittää roolia lainkaan.

**Toteutus poikkeaa Designin kuvasta yhdessä kohdassa tarkoituksella.** Mockissa on
`Submit move`, `Undo` ja siirtonuolet, eli **pelitila**, jota tämä sovellus ei tuota:
`BoardScreen`in signatuuri on käännösaikainen tae siitä ettei niitä voi lisätä vahingossa.

*Korjaus 8.8.2026:* tässä luki toisena poikkeamana että pip-vahti jätettiin laudan alle
omaksi rivikseen eikä pakattu keskitilaan. Se ei ole enää totta: rivi siirrettiin
keskitilaan samana päivänä (`6ce84ca`), koska oma rivi maksoi korkeutta jonka kiilat
tarvitsevat. Ristiriitateksti kantaa yhä paneelin luvut, eli se peruste jonka vuoksi
poikkeama aikanaan kirjattiin, on tallella pakatussakin muodossa.

**Pinon luku kirjoitetaan heti limityksen alkaessa, ei vasta kun pino ei mahdu**
(Tommin havainto 8.8.2026, muutettu samana päivänä). Löytö tuli sivuston ja puhelimen
rinnakkaisvertailusta: sivusto näytti kahdeksan nappulan pinossa luvun `8`, appi ei.

Syy oli laskettavissa eikä arvattavissa. Nappulan koko on kiilan korkeus jaettuna viidellä
ja tihein askel on 0,56 nappulaa, joten kiilaan mahtuu `1 + 4 / 0,56` eli **tasan
kahdeksan**. Kahdeksan oli siis täsmälleen se raja jossa vanha ehto (`luku vasta kun ei
mahdu`) vielä vaikeni. Sivusto tekee saman asian pienemmällä kapasiteetilla, koska sen
kiila on kiinteä GIF eikä skaalaudu.

Ehto on nyt `count > minOf(visible, CHECKERS_APART)`, eli kaksi eri syytä yhdessä
lausekkeessa: limittynyttä pinoa ei lue silmäyksellä (viiden raja) ja mahtumaton pino
kertoisi ilman lukua väärän määrän (kiilan raja). Kumpi tahansa yksinään jättäisi toisen
tapauksen numerottomaksi.

#### Laudan mittasuhteet ovat mitattuja, ja lähde on DailyGammon itse (8.8.2026)

Kysymys nousi Tommilta: pitäisikö tutkia miten oikea lauta ja verkkolaudat on mitoitettu.
Tutkimusta ei tarvittu, koska **sivusto kertoo mittansa itse talteen otetuissa tavuissa**.
Nämä neljä lukua ovat `move_board.html`in `IMG`-attribuuteista:

| Elementti | Sivustolla | Suhde nappulaan |
|---|---|---|
| Kiila | `WIDTH=23 HEIGHT=115` | kiila on 1:5, ja nappula on sen levyinen |
| Nappula | 23 leveä | 1 |
| Noppa | `25 x 25` | 1,09 |
| Kuutio | `29 x 29` | 1,26 |

Ensimmäinen rivi on tärkein, ja siitä seuraa kaksi asiaa. `115 / 23 = 5` tasan, eli
**`CHECKERS_APART` on laudan geometriaa eikä makuamme**: se oli päätelty siitä mitä silmä
lukee laskematta, ja mittaus sanoo saman luvun toisesta suunnasta.

Ja kiila on 1:5, kun meidän kiilamme oli tällä laitteella 47 x 134 eli **1:2,85**. Siinä on
se mataluus jonka Tommi nimesi pahimmaksi puutteeksi, nyt mitattuna eikä arvioituna.

**Kaksi viimeistä riviä otettiin käyttöön vasta 10.8.2026.** Siihen asti yksi vakio
`CUBE_PER_CHECKER = 1.12f` (Designista) kantoi sekä noppaa että kuutiota, eli osui nopan
kohdalle lähelle ja kuution kohdalle selvästi alle. Nyt vakioita on kaksi,
`DICE_PER_CHECKER = 25f / 23f` ja `CUBE_PER_CHECKER = 29f / 23f`, ja `BoardMetrics` kantaa
kummankin erikseen. Jakolaskuna eikä desimaalina, jotta taulukon luvut jäävät koodiin
näkyviin.

Korkeus ei muuttunut kummassakaan kuution paikassa: keskimmäinen on `CubeBadge`issa ja päädyt
`CubeSlot`issa lokerosarakkeen sisällä.

**Sama ilta osoitti että väite piti paikkansa mutta kertoi vain puolet.** Keskitila oli
kiinteä 59 dp, joten se ei kasvanut, mutta se **rajasi**: `Modifier.size` rajautuu tulevaan
rajoitteeseen, ja rajoite oli kiinteä vain korkeussuunnassa. Tabletilla kuutio oli siksi
79,5 x 57,9 dp eli suorakaide. Ks. seuraava osio.

#### Keskitila johdetaan palan koosta (10.8.2026, mitattu Galaxy Tab S7+:lla)

Ensimmäinen ajo tabletilla paljasti kaksi vikaa, eikä kumpaakaan olisi löytynyt testeistä tai
logcatista: molemmat ovat ulkoasua, eli täsmälleen se **koettimen työkalumaasto** jonka
`docs/TESTAUS.md` nimeää.

**Kuutio oli suorakaide.** Kiinteä `MID_BAND = 59 dp` rajasi korkeuden muttei leveyttä, ja
tabletilla johdettu koko oli suurempi kuin raja. Mitattu 79,5 x 57,9 dp.

Keskitila on nyt **kuution mittainen**. Mitta on kuution eikä nopan, koska kuutio on kahdesta
palasta suurempi: nopan mukaan mitoitettu tila olisi rajannut kuution täsmälleen kuten vakio.
Noppa jää siis hieman keskitilaa matalammaksi, kuten sivustollakin.

**Kehä ratkeaa kerralla eikä iteroimalla**, ja tämä tiedosto arvioi sen aiemmin toisin.
Alempi työlista sanoi keskitilan johtamisesta: *"ratkeaa laskemalla kierros kahdesti"*. Arvio
kehästä oli oikea ja arvio sen purkamisesta väärä. Kun `k` on nappula ja `H` kiilojen ja
keskitilan yhteinen korkeus, niin `2 * 5k + k * 1,26 = H`, eli

```
k = H / (2 * CHECKERS_APART + CUBE_PER_CHECKER)
```

`DgBoardWidthTest` väittää tämän kahdessa muodossa: kuutio mahtuu keskitilaan neliönä neljällä
eri korkeudella, ja kiilat plus keskitila täyttävät korkeuden tasan.

**Mitattu tulos laitteelta korjauksen jälkeen:** kuutio 170 x 170 px ja molemmat nopat
139 x 139 px, eli neliöitä. Sivutuote jota ei haettu: Pixel 8a:n nappula kasvaa 29,2:sta
31,2 dp:hen, koska johdettu keskitila on siellä 39,3 dp eikä 59.

#### Kuution ja noppien kulmat sekä ykköspinnan robotti (26.8.2026)

Kaksi Tommin pyytämää muutosta samaan palaan. **Kulman pyöristys on suhde sivuun**
(`DgBoard.CORNER_PER_SIDE`, kuudesosa) eikä entinen kiinteä 4 dp, joka näytti tabletin
isolla kuutiolla lähes terävältä ja puhelimen nopalla pyöreämmältä. Arvo on silmävarainen,
ei mitattu.

**Ykköspinta näyttää Android-robotin pään eikä numeroa** (`CubeRobotFace`). Fyysisessä
kuutiossa ei ole ykköspintaa, eli sivuston `ALT="1"` on jo keinotekoinen merkintä tilalle
"kukaan ei ole tuplannut", eikä ykkönen kanna päätöstietoa niin kuin 2 tai `DR`; siksi tämä
ei riko periaatetta "ALT sellaisenaan". Pää viittaa sovelluksen nimeen, väri on sama
`OnCube`-kerma kuin numeroilla, eikä pinnassa ole pilkkuja jotka sekoittuisivat noppaan.
Luonnoksissa hylättiin koko robotin hahmo (ei pysy luettavana pienenä), sivuston oma
noppa-favicon (omisi lähteen tunnuksen), `DR`-vitsi (oikea kuutiotila, valehtelisi peliä)
ja noppakuvat kuutiopinnassa (sekoittaisi palat jotka lauta pitää erillään). Robotti on
CC BY 3.0, ja nimeäminen on manuaalissa (`help_robot_body`). Launcher-kuvake on yhä
päättämättä; jos se tehdään, sama pää on luonteva ehdokas.

#### Toinen lähde kumosi ensimmäisen: nappula on 0,70 sarakkeesta (DG Mobile, 8.8.2026)

Sivuston GIF ei ole ainoa mitattavissa oleva lähde. **DG Mobile on Tommin puhelimessa**
(`com.dailygammon.DGMobile`), ja siitä otettiin `adb`:llä kuvakaappaus lautanäkymästä. Se on
`raakasivut/LUEMINUT.md`:n kuvakaappauslistan kohta 2, jota ei ollut nähty.

Mittaus kuvan pikseleistä (2400 x 1080, tiheys 2,625, eli 914 x 411 dp):

| Mitta | DG Mobile | DailyGammonin GIF |
|---|---|---|
| Sarake | 42,5 dp | 23 px |
| Nappula | 29,7 dp | 23 px |
| **Nappula / sarake** | **0,70** | **1,0** |
| Kiilan korkeus | 155 dp | 115 px |
| Kiilan suhde | 1:3,66 | 1:5 |

**Lähteet ovat eri mieltä, ja `CHECKER_PER_COLUMN` on 0,70** (Tommin valinta). DG Mobilen
0,70 on sama luku kuin Claude Designin `25 / 35`, eli kaksi toisistaan riippumatonta
suunnittelijaa päätyi samaan väljyyteen. Sivusto on niistä poikkeava, ja syy on nyt näkyvissä:
**sen kiila on yksi GIF-kuva jossa nappulat on piirretty valmiiksi sisään**, joten sillä ei
ole erillistä sarakkeen ja nappulan suhdetta lainkaan. Siitä luettu 1,0 oli suhde jota
lähteessä ei ole, ei mittausvirhe vaan väärään kysymykseen vastannut mittaus.

`CHECKERS_APART = 5` **säilyy molempien sanomana**, ja se on tässä olennaista: kaksi lähdettä
riitelee väljyydestä muttei siitä montako nappulaa kiilaan kuuluu. Kiilan suhde on siis
seuraus eikä asetettava luku, ja se on 1:3,55 kun nappula on 0,70 sarakkeesta.

Kolme muuta havaintoa samasta kaappauksesta, koska ne tukevat saman illan muita päätöksiä.
**DG Mobile on koko ruudun kokoinen**, vain sivunavigointi näkyy, eli palkkien piilotus ei ole
meillä erikoisuus. **Sen napit ovat laudan vieressä oikeassa reunassa** (noin 117 x 40 dp),
mikä on sama sijoittelu johon meillä päädyttiin kaventuneen laudan takia, ja se selittää
miksi sen kiila on 155 dp kun meillä on 134: sillä ei ole otsikkoriviä lainkaan. Ja **se
piirtää pinosta tasan viisi nappulaa eikä limitä koskaan**, luku päällimmäiseen; meillä
limitys jatkuu kiilan täyteen asti. Luku näkyy molemmissa, piirtotapa on eri.

Pikselilukemat ovat silmämääräisiä kuvasta, eli parin pikselin tarkkuudella.

**Väite jonka mittaus kumosi, ja se oli minun.** Sanoin ettei lautaa voi kaventaa, koska
kaventaminen pienentäisi saraketta ja siten nappulaa. Se pitää paikkansa vain Designin
suhteella `nappula = 0,714 x sarake`. Sivustolla nappula on sarakkeen levyinen, ja silloin
kaventaminen on ilmaista: nappulan koko tulee kiilan korkeudesta, joka ei muutu. Sarake on
nyt nappulan levyinen, lauta kapeni noin 670 dp:stä noin 384 dp:hen, ja nappula pysyi
26,8 dp:ssä. Ylimääräinen leveys ei ollut koskaan hyödyksi: se jaettiin sarakkeille joilla
ei ollut sille käyttöä, eli väljyydeksi nappuloiden ympärille.

**Leveys johdetaan siis korkeudesta eikä päinvastoin.** Se vaatii tietämään numerorivien
korkeuden ennen kuin ne on piirretty, ja `numberRowHeight()` lukee sen tyyliskaalasta ja
tiheydestä eikä vakiosta, jotta käyttäjän oma tekstikoko kasvattaa myös sitä. **Tämä ei ole
paluu korkeusbudjettiin:** rivi piirtyy yhä omalla mitallaan ja pelialue joustaa
`weight`illä, joten arvion virhe muuttaa laudan leveyttä muttei voi leikata mitään pois.
Ero vanhaan `WEDGE_HEIGHT`iin on juuri tämä: siellä arvaus oli vähennys jonka virhe
piirtyi pois, tässä se on kerroin jonka virhe näkyy leveytenä.

Fyysisen laudan mittasuhteet olisivat eri kysymys, eikä niitä ole selvitetty. Ne kannattaa
selvittää vain jos sivustosta halutaan poiketa tietoisesti.

**Järjestelmäpalkit piilotetaan lautaruudulta ja vain siltä** (Tommin valinta 8.8.2026).
Sovellus piirsi jo palkkien alle, mutta sisältö väisti ne `safeDrawingPadding`illa, joten ne
veivät korkeutta oikeasti. Lautaruudulla se korkeus **on** kiilan korkeus, eli sama budjetti
josta puolikas lauta johtui.

Rajaus yhteen ruutuun on se mikä tekee tästä halvan. Otteluluettelo ja asetusruutu ovat
vieritettäviä listoja, joille korkeus ei ole pula, ja niillä kellonaika, akku ja
takaisin-ele ovat hyödyllisempiä kuin muutama rivi lisää. Palkit saa hetkeksi näkyviin
pyyhkäisemällä (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`), eivätkä ne silloin muuta
asettelua. Sovelluksen oma paluu ei ole tämän varassa, koska otsikkorivillä on `Back`.

`safeDrawingPadding` jää voimaan, koska se kattaa myös näyttölovan, joka ei ole palkki eikä
katoa piilotuksella.

**Vieritettävä lauta hylättiin**, ja se on nyt kirjattu jottei sitä ehdoteta uudelleen. Se
olisi ratkaissut saman pulan säilyttämällä nappulan koon, mutta hinta olisi ollut se että
asemaa ei näe kerralla. Backgammonissa koko lauta on yksi luettava kuva: pip-luvut, muuri ja
molempien kotialueet ovat kysymyksiä joihin vastataan yhdellä silmäyksellä. Vieritys tekisi
niistä kaikista muistitehtäviä.

**Sivuston oma kynnys on mittaamaton eikä sitä ole kopioitu.** Numero on leivottu GIF-kuvan
sisään, joten sivun tavut kertovat vain määrän (`ALT="y6"`) eivätkä sitä näkyykö kuvassa
numero. Talteen otetuissa sivuissa suurin pino on kuusi, ja ainoa näyttö numerosta on
kuvakaappaus kahdeksasta. Tämän ruudun kynnys on siksi johdettu omasta perustelusta, ja
se sanotaan ääneen tässä: arvattu kynnys näyttäisi kopiolta.

**Kirjautunut pelaaja on aina sivun viimeinen eli sivupaneelin alapuolella, vastustaja
aina ensimmäinen eli yläpuolella (Tommin mitattu havainto 5.8.2026).** Tämä on nyt myös
koodissa: `selfCheckerColor` päättelee kirjautuneen pelaajan `CheckerColor`in pip-vahdista
(`board.players.last().pips` verrattuna `PipCheck.Agrees`in keltainen/sininen-summiin) sen
sijaan että väri arvattaisiin tai kiinnitettäisiin vakioksi. Muuri ja keskitilan nopat
jaetaan tällä: vasen noppa = vastustaja, oikea noppa = itse. **Muuri on 6.9.2026 alkaen
toisin päin kuin nopat**, eli yläsegmentti = itse ja alasegmentti = vastustaja; peruste on
alempana kohdassa *Muurin nappula on sillä puolella jonne se on menossa*.
Jos pip-vahti ei täsmää tai luvut ovat piilotettu, päättely palauttaa `null` ja käyttöön
jää kiinteä oletus (keltainen ylös, sininen alas) sen sijaan että arvattaisiin väärin.

**Pistemäärä piirretään sivun omassa muodossa, tähti mukaan lukien** (Tommin valinta
14.8.2026). Crawford-pelissä sivusto merkitsee johtajan pistemäärän tähdellä, ja paneelissa
lukee siksi `score 6*` eikä `score 6`. Vaihtoehtona oli oma rivi joka sanoo saman sanoin, ja
se hylättiin: sivustokaan ei selitä merkkiä, eikä tämä ruutu lisää omaa sanaansa siihen mitä
sivu sanoo. Sama sääntö kuin `backgroundColor`illa rivin verran ylempänä, mutta toisin päin,
koska tässä sivun merkintä on mitattu eikä hypoteesi.

Lukema itsessään on `BoardState.crawford`, ja se lukee kahta merkkiä yhtä aikaa (tähti ja
kuution puuttuminen). Ruutu käyttää siitä vain toista puolta, koska se piirtää sivun merkinnän
eikä tulkintaa; ristiriitainen sivu näyttäisi siis tähden vaikka lukema on tuntematon. Se on
tietoinen seuraus valinnasta piirtää lähdettä eikä päätelmää, ja tulkinta odottaa sitä hetkeä
jolloin kuution napit ovat käytössä.

#### Toinen kaappaus samasta sovelluksesta: nappisarake mitattuna (22.8.2026)

Edellinen osio luki 8.8.2026 otetusta kaappauksesta että **DG Mobilen napit ovat laudan
vieressä oikeassa reunassa**, noin 117 x 40 dp ja silmämääräisesti. Sama sovellus kaapattiin
uudelleen 22.8.2026 tabletilta juuri siinä tilanteessa jossa se tarjoaa sekä heittoa että
tuplausta, ja luvut on nyt luettu pikseleistä eikä silmällä.

| Mitta | 22.8.2026 (Galaxy Tab S7+) | 8.8.2026 (puhelin, silmämääräisesti) |
|---|---|---|
| `ROLL` | 116 x 50 dp | 117 x 40 dp |
| `DOUBLE` | 109 x 49 dp | |
| Nappisarake laudan ulkopuolella | 130 dp | |

Leveys täsmää molemmissa, korkeus ei. Ero on kymmenen dp:tä, ja lähteet eivät ole
vertailukelpoisia: eri laite, eri tiheys ja eri menetelmä. Uudempi luku on mitattu etsimällä
lähes valkeat alueet kuvasta ohjelmallisesti, eli se ei ole silmämääräinen.

**Sijoittelu on tämän osion tärkein havainto, ja se on nyt sanottavissa tarkemmin kuin 8.8.**
DG Mobile ei mahduta nappeja lautaan lainkaan. Sarake on laudan **ulkopuolella**, ja lauta
kaventuu sen verran: kehyksen oikea reuna on x=2523, kun näyttö on 2800 leveä. Sarakkeessa on
pystyjärjestyksessä vastustajan tiedot, `ROLL`, ottelun pituus, `DOUBLE` ja omat tiedot.
Kuutio on samalla tavalla ulkona, vasemmassa reunassa.

**Edellinen osio sanoo tästä vanhaa, ja rivi jätetään näkyviin.** Se sanoo sijoittelun olevan
*sama johon meillä päädyttiin*, ja se oli totta 8.8.2026, jolloin napit asuivat
lokerosarakkeessa. Napit siirtyivät `MiddleStrip`iin 14.8.2026, joten väite vanheni kuusi
päivää myöhemmin eikä sitä käyty korjaamassa. Tämä on sama vikamuoto joka mitattiin 21.8.2026
kolmesta muusta tekstistä: **teksti pysyi paikallaan kun ruutu muuttui alta.**

**Vertailu meidän ratkaisuun 22.8.2026 jälkeen.** Meillä napit ovat laudan sisällä
keskikaistassa, ja tila tehtiin kaventamalla noppien omaa aluetta kuudesta sarakkeesta
neljään. Kaksi eri tietä samaan päämäärään: DG Mobile ottaa tilan laudan leveydestä, me
noppien varauksesta. Napin korkeus päätyi käytännössä samaan, 48 dp meillä ja 50 siellä,
ja se on kirjaamisen arvoista koska luvut valittiin toisistaan tietämättä.

**Kaksi eroa jotka koskevat periaatetta eivätkä mittaa.** DG Mobilessa **ei ole
`Verify`-ruutua lainkaan**, vaikka sivulla se on. Ja napit lukevat `ROLL` ja `DOUBLE`, eivät
`Roll Dice` ja `Double`, eli se lyhentää sivun sanat. Meidän kirjattu periaate on käyttää
sivun sanaa sellaisenaan (`BoardScreen.MiddleActionsRow`), ja juuri se tekee rivistämme
pidemmän. Periaate ei kaadu tähän, mutta sen hinta on nyt mitattu eikä arvattu: sivun sanat
vievät noin kolmanneksen enemmän leveyttä kuin lyhennetyt.

**Mikä jäi yhä näkemättä.** `raakasivut/LUEMINUT.md`:n kuvakaappauslistan kohta 2 on
*DG Mobile, erityisesti istunnon katkeamisen ruutu*. Lautaruutu on nyt nähty kahdesti, mutta
katkeamisen ruutua ei kertaakaan, ja juuri se on kanonissa yksi kahdesta projektin syystä.

### Asetusnäkymä lukutilassa (`app/ui/Settings*`, lisätty 8.8.2026)

Omat asetukset (`/bg/profile`) ruudulla. **Ensimmäinen näkymä muulle kuin lautajäsentimelle**,
ja se otettiin ensin kolmesta ehdolla olleesta samasta syystä kuin lautanäkymä aikanaan
siirtämisen edeltä: se on halvin ja rakenteellisesti turvallisin. Kohde on kanonissa jo
nimetty kuluttamattomaksi, jäsennin ja sen testit olivat valmiina, eikä mikään muutu
sivustolla.

- **Lukutila on tässä vahvempi väite kuin lautanäkymässä, ja ero on rakenteessa.**
  `BoardViewModel` saa polun ulkoa ja joutuu tarkistamaan sen, koska polku tulee sivun
  linkistä. `SettingsViewModel` **ei ota polkua lainkaan**: kohde on vakio
  `DgPages.SETTINGS_PATH`. Muuta osoitetta ei voi pyytää kutsumalla luokkaa väärin, koska
  sellaista parametria ei ole. `SettingsViewModelTest` kirjaa silti pyydetyt polut, jotta
  kummankin näkymämallin polkulista on täydellinen lausuma omasta luokastaan.
- **Tunnistus ennen jäsennystä, ja tässä ruudussa se on pakko eikä varotoimi.**
  `SettingsParser.parse` ei kieltäydy väärästä sivusta vaan palauttaa siitä tyhjän tuloksen,
  ja tyhjä tulos ruudulla väittäisi että **kaikki asetukset ovat pois päältä**. Vika olisi
  hiljainen ja uskottavan näköinen, eli sitä lajia jota vastaan tässä projektissa
  suojaudutaan. `DgPages.isSettingsPage` erottaa ne, ja väärä sivu on oma tilansa
  (`NotSettingsPage`) eikä verkkovirhe.
- **Ruutu ei kirjoita, ja se näkyy ruudulla eikä vain koodissa.** Jokainen valintaruutu ja
  radiopainike on pysyvästi ei-vuorovaikutteinen (`onCheckedChange = null`, `onClick = null`),
  ei `enabled = false`. Ero on lupauksessa: jälkimmäinen sanoo että toiminto on olemassa
  mutta estetty juuri nyt. Sama muoto kuin viestiruudussa, josta hakunappi puuttuu kokonaan
  silloin kun jonon linkkiä ei ole: toiminto jota ei voi tehdä turvallisesti jätetään pois
  eikä näytetä toimimattomana. (Tässä luki 9.8.2026 asti otteluluettelon odotusilmoitus, joka
  oli oikea esimerkki siihen asti kunnes ilmoitus sai napin samana päivänä.)

  Tämä on se sivu jolla kirjoituksen hinta on korkein. `SettingsPage`n oma dokumentaatio
  sanoo miksi: lomake lähettää kaikki kentät kerralla ja rastittamaton ruutu ei lähde mukana,
  joten yksi väärin luettu kenttä tuhoaisi asetuksen eikä alkuperäistä tilaa olisi enää
  missään.
- **Käyttökohde oli sovelluksessa jo olemassa vastauksettomana.** Lautanäkymä sanoo kahdessa
  tilanteessa että vika on käyttäjän omassa asetuksessa: Mini-skeema pudottaa pistenumerot
  (`board_position_unreadable`) ja `Hide pip counts` piilottaa pip-luvut
  (`board_pips_hidden`). Kumpikin neuvoi mitä asetusta katsoa muttei kertonut mikä se nyt on,
  joten neuvon tarkistaminen vaati selaimen.
- **Sisäänkäynti on rivi otteluluettelossa eikä nappi yläpalkissa.** Yläpalkissa on
  ennestään `Refresh` ja `Sign out`, ja kolmas nappi veisi leveyden otsikolta ilman että
  mikään kertoisi siitä. Se on sama vikamuoto kuin laudan kiinteä kiilankorkeus samana
  päivänä, vain pienempänä. Rivi on kirjautuneen omien lukemien joukossa (kuka on
  kirjautuneena, mihin asti arkisto ulottuu), koska se on niiden kanssa samaa lajia: tietoa
  tästä tilistä eikä otteluista.
- **Reitti on parametriton**, joten se ei tarvitse `BoardRoute`n koodausta eikä sen testiä.
  Sama asia kuin näkymämallin puuttuva polkuparametri, toisessa kerroksessa.

#### Jäsentimeen lisättiin selitteet, ei uutta luettavaa

`SettingsPage.choices` (`PreferenceChoice`, `ChoiceOption`) kantaa radioryhmien otsikot ja
vaihtoehtojen selitteet. Syy on mitattu: ryhmien arvot ovat paljaita numeroita, joten ilman
selitettä ruutu sanoisi käyttäjälle `board 1`, kun sivu itse sanoo `Blue/White`. Numero ei
esiinny sivustolla missään näkyvissä, eli se on tieto jota ei voi verrata mihinkään.

Kaksi rajausta jotka pitävät lisäyksen erossa lähetyksestä. Kenttä on **vain näyttöä
varten** eikä osa lähetyssopimusta, joka on yhä `checked` + `radios` + `radioGroupNames`;
`SettingsParserTest` väittää tämän vertaamalla vanhat ja uudet kentät toisiinsa. Ja selite
on sivuston englantia joka voi muuttua sanamuodon mukana, eli sama varaus kuin
`SettingsPage.nameFor`issa: kelpaa näytölle, ei avaimeksi.

Selitteet luetaan eri kohdasta kuin valintaruutujen selitteet, ja ero on sivun omaa muotoa.
Valintaruudun selite on samassa solussa (`<TD><INPUT ...> Hide pip counts`), radion selite
viereisessä (`<TD><INPUT ...><TD>Blue/White`), ja ryhmän otsikko on taulukon ulkopuolella
`<H4>`:nä. Otsikko haetaan siksi lomakkeen elementtien dokumenttijärjestyksestä, joka on
ainoa suhde jonka sivu tässä antaa. Puuttuva otsikko on tavallinen tulos eikä virhe, ja
silloin ruutu turvautuu kentän nimeen: keksitty otsikko olisi käännös jota käyttäjä ei löydä
sivustolta silloin kun hän menee sinne muuttamaan asetusta.

**Todennettu laitteella 21.8.2026**, kaksitoista päivää kirjoittamisen jälkeen. Ruutu piirtyy
Pixel 8a:lla oikein: kahdeksan valintaruutua tiloineen, kolme radioryhmää otsikoineen
(`"Next" Match Ordering`, `Background Color`, `Board Scheme`) ja selite siitä että näkymä on
vain luettava. Tämän osion kuvaama otsikkotapaus näkyy siinä sellaisena kuin se on kirjattu:
ensimmäisellä valintaruutulohkolla ei ole otsikkoa lainkaan, koska sivu ei anna sille `<H4>`:ää,
eikä ruutu keksi sellaista.

*Yksi asia jäi todentamatta samalla ajolla, ja se on menetelmähuomio.* Luin ruudun ensin
`uiautomator`-puuna, ja siitä puuttui rivi `Skip Opponent's "Roll Dice" pages`. Rivi on
ruudulla, ja puuttuminen oli oman lukutapani vika: teksti sisältää lainausmerkit, jotka
katkaisivat XML-attribuutin poiminnan. **Puurakenne ei siis riitä ruudun katsomiseen**, ja
tämä on juuri se ero jonka vuoksi laiteajo on olemassa: puu vastasi kysymykseen väärin ja
näytti silti täydeltä listalta.

### Otsikkorivi poistui, ja leveydestä tuli rajoittava mitta (9.8.2026)

Alla oleva työlista on **tehty siltä osin kuin se oli päätetty**, ja toteutus muutti kahta sen
lukua. Tämä osio kertoo mikä muuttui ja miksi; työlista jää alle sellaisena kuin se oli, koska
sen mitatut ehdotukset ovat yhä auki.

**Otsikko ei ole rivi vaan hetki (Tommin ratkaisu).** Kysyin kumpaan kohtaan sivupaneelia
otsikko siirtyy, ja kumpikin vaihtoehtoni oli väärä kysymys: teksti näkyy nyt **hetken kun
palkit pyyhkäistään esiin**. Näkyvyys luetaan `WindowInsets.areStatusBarsVisible`istä, eli se
on täsmälleen palkkien oma tila eikä oma ajastin, ja käyttäjä oppii yhden eleen kahden sijaan.
Otsikko on `Box`in päällepiirto eikä asettelun jäsen, joten se ei vie korkeutta silloinkaan kun
se on näkyvissä. Yläreunan pehmuste luetaan `statusBarsIgnoringVisibility`stä, koska piilotetun
palkin oma inset on nolla eikä kertoisi mihin kohtaan teksti mahtuu.

Sivupaneeli säilyi siis koskemattomana, ja sen kanssa se mitä kysymys koski: ylempi
pelaajapaneeli on yhä laudan yläreunan tasalla.

**Aritmetiikka kaatoi työlistan nappisarakkeen leveyden, ja se tapahtui testissä eikä
laitteella.** Lista sanoi että vapautuva noin 130 dp:n sarake ottaa napit 116 dp leveinä.
Luku oli mitattu tilanteessa jossa korkeus oli rajoittava mitta. Kun otsikkorivin 32 dp palasi
kiilalle, **leveydestä tuli rajoittava mitta**, ja silloin jokainen nappisarakkeeseen varattu
dp on suoraan pois nappulan koosta: 116 dp olisi kutistanut nappulan noin 26 dp:hen eli
pienemmäksi kuin ennen koko muutosta. Sarake tehtiin siksi 72 dp:n varauksella, jolloin
nappula oli 28,2 dp.

**Sarake purettiin samana päivänä, ja se on koko ratkaisun paras kohta.** Tommi katsoi sitä
puhelimelta ja nimesi napit epäilyttäviksi: eleet vapauttavat pikselit muuhun käyttöön ja ovat
muista sovelluksista tuttuja, joten nappi maksaa tilaa siitä mitä pelaaja osaa jo ilman sitä.
Ruudulla ei ole enää yhtään omaa nappia.

| | 8.8. | Nappisarakkeella | Nyt |
|---|---|---|---|
| Nappula | 26,8 dp (mitattu) | 27,4 dp | **29,2 dp** (laskettu) |
| Ruudun omat napit | 2, 32 dp korkeita | 2, 48 dp korkeita | 0 |
| Rajoittava mitta | korkeus | leveys | korkeus |

29,2 dp on käytännössä DG Mobilen mitattu 29,7 dp, eikä se ole sattuma vaan sama syy
kummallakin: kummallakaan ei ole otsikkoriviä eikä nappeja laudan vieressä.

*Korjaus samana päivänä:* tässä luki ensin 30,0 dp, ja luku oli laskettu antamalla laskijalle
ikkunan korkeus sen korkeuden sijaan joka riville jää. Erotus on `Column`in oma 8 dp:n pehmuste.
`DgBoardWidthTest` sanoi saman väärän luvun, koska se syötti saman korkeuden; testi ei siis
ollut väärässä laskennastaan vaan siitä mitä sille annettiin. Ero on pieni eikä muuta yhtäkään
päätöstä, mutta se muuttaa vertailun suunnan: DG Mobile on yhä hieman edellä eikä jäljessä.

#### Ele on tilaa, ei vain vaihtoehto napille (9.8.2026)

Kirjattu periaatteeksi eikä tämän ruudun ratkaisuksi, koska se koskee jokaista tulevaa nappia.
Tommin muotoilu: *"eleet mahdollistaa pikselien käytön toisiin tarkoituksiin ja ovat muista
appeista tuttuja"*. Kosketusnäytöllä nappi ei ole ilmainen: se ottaa tilaa siltä sisällöltä joka
on ruudun tarkoitus, ja tällä ruudulla se tila on suoraan nappulan koko. Ele ei ota mitään.

**Ehto joka pitää tämän voimassa: näkymätön ele on kirjoitettava johonkin.** Arvattava ele ei ole
ele, ja siksi eleiden kanssa syntyi samalla manuaali (`HelpScreen`). Se maksaa saman minkä napit
maksoivat, mutta kerran eikä joka kierros.

Kolme elettä lautaruudulla, eikä yhtäkään niistä ole keksitty tässä: veto alaspäin hakee sivun
uudelleen, reunapyyhkäisy vie takaisin (järjestelmän oma), ja palkkien pyyhkäisy näyttää otsikon.

**Suunta on osa elettä, ja se on mitattu laitteella (Tommi 9.8.2026).** Kaikki kolme toimivat
tasan yhteen suuntaan: veto **ylhäältä alas**, paluu **oikealta vasemmalle**. Vasen reuna ei siis
vie takaisin, vaikka Androidin takaisin-ele on yleensä molemmilta reunoilta. Manuaali sanoo
suunnat, koska juuri suunta on se mitä eleestä ei voi arvata: ele jota kokeillaan väärään suuntaan
näyttää rikkinäiseltä eikä opettamattomalta.

Sama mittaus **sulki edellisen kappaleen riskin**: lautaruudulta pääsee pois, eli piilotetut palkit
eivät estä järjestelmän takaisin-elettä. Se oli tämän ratkaisun ainoa kohta jossa väärä oletus
olisi jättänyt pelaajan jumiin.

*Miksi vetopyyhkäisy on tehty käsin eikä `PullToRefreshBox`illa.* Material3:n toteutus kuuntelee
sisäkkäistä vieritystä, ja tämä ruutu on tarkoituksella vierittämätön (vieritettävä lauta
hylättiin 8.8.2026). Vieritystapahtumia ei synny, joten valmis komponentti ei laukeaisi
kertaakaan. Kynnys on 72 dp matkaa eikä nopeutta, ja haku lähtee **kerran per ele**: kohde on
vapaaehtoisvoimin pyöritetty sivusto.

*Riski joka oli auki muutaman tunnin ja on nyt suljettu:* ruudulla ei ole omaa `Back`ia missään
tilassa, joten jos järjestelmän reunapyyhkäisy ei toimisi piilotettujen palkkien kanssa, ruudulta
ei pääsisi pois. Todennettu laitteella samana päivänä: se toimii, oikeasta reunasta.

**Korjaus 10.8.2026: takaisin-elettä ei ole tällä laitteella, ja poistuminen on kaksi tekoa.**
`adb shell settings get secure navigation_mode` vastaa `0`, eli Pixel 8a on kolmen napin tilassa.
Reunasta tehtävää takaisin-elettä ei silloin ole olemassa kummassakaan reunassa, joten yllä oleva
lause *"paluu oikealta vasemmalle"* kuvaa elettä jota laitteessa ei ole. Poistumistie on mitattu
ja se toimii: reunapyyhkäisy paljastaa palkit, ja `◀` vie takaisin. Ne ovat kaksi tekoa, ja
jälkimmäinen jäi 9.8. kirjaamatta.

Kaksi rajausta jotta korjausta ei lueta laajempana kuin se on. **Riski on yhä suljettu**, eli
lautaruudulta pääsee pois, ja se on nyt todennettu tapahtumaketjuna eikä eleenä. Ja ele on
olemassa eletilassa oleville laitteille, joten manuaalin virhe ei ole väärä väite vaan vajaa: se
lupaa kaikille sen mikä pätee osalle.

*Miksi tämä meni väärin.* 9.8. mitattiin lopputulos (ruutu vaihtui) eikä sitä montako tekoa siihen
tarvittiin, ja kaksi peräkkäistä tekoa samalla kädellä samasta reunasta tuntuu yhdeltä. Sama
muoto kuin `koetin`-termin työkalumaastossa (`Kaanon/KÄSITTEISTÖ.md`): oikea lopputulos ei
todista sitä mekanismia jonka nojalla se selitettiin.

#### Piilotus muuttui ehdolliseksi, eikä uutta elettä tarvittu (20.8.2026)

Tommi tabletilla: *"peli tarvitsee lisää eleitä, poistuminen on vaikeaa"*, ja tarkennettuna
*"ainakin tabletille pitää saada ne kolme nappia tai lisää eleitä"*. Havainto on siis
edellisen kohdan toistuma toisella laitteella: Galaxy Tab S7+ on sekin kolmen napin tilassa
(`navigation_mode` on `0`, mitattu 20.8.2026), joten poistuminen on siellä sama kaksitekoinen
ketju, ja isolla ruudulla `◀` on vielä kauempana kädestä.

**Ratkaisu ei ole uusi ele vaan piilotuksen ehdollistaminen, ja perustelu on mitattu.**
Piilotuksen koko peruste on että jokainen palkeille menevä dp on pois nappulan koosta. Se
peruste lakkaa olemasta silloin kun `CHECKER_MAX` rajoittaa jo ennen korkeutta, ja tabletilla
niin on:

| Laite | Ikkunan korkeus | Palkit pystysuunnassa | Nappula palkkien kanssa | Nappula ilman |
|---|---|---|---|---|
| Galaxy Tab S7+ vaaka | 824 dp | 48 dp | 54 dp, katossa | 54 dp, katossa |
| Pixel 8a vaaka | 411 dp | 28 dp | 27,9 dp | 30,3 dp |

Tabletilla piilotus ostaa siis tasan nolla dp:tä ja maksaa ainoan tien pois ruudulta.
Puhelimella se ostaa vähemmän kuin miltä ensin näytti, mutta ostaa silti, ks. korjaus alempana.
`DgBoard.barsAreFree` tekee tämän valinnan, ja **ehto on mitta eikä laite**, sama muoto kuin
sivupaneelilla ja napeilla. Kääntöpiste on 648 dp sisäkorkeutta, eli kumpikaan laite ei ole
rajatapaus: molemmilla on yli 50 dp varaa omalle puolelleen. `DgBoardBarsTest` lukitsee sekä
molemmat puolet että sen ettei kumpikaan ole rajalla.

**Mittaus on ennen `safeDrawingPadding`ia, ja se on ehto eikä järjestys.** Padding kutistuu
juuri silloin kun palkit piilotetaan, joten sen sisältä luettu korkeus kumoaisi oman
päätöksensä joka kierroksella. Se on sama ansa joka 10.8.2026 tuotti 294 värähtelevää ruutua
viidessä sekunnissa, ja se väistetään lukemalla luku paikasta jota päätös ei liikuta.
`WindowInsets` luetaan samasta syystä `systemBarsIgnoringVisibility`stä: tavallinen
`systemBars` on nolla heti kun piilotus on kerran tehty, eli se vastaisi eri kysymykseen.

**Manuaalin eleosio muuttui samalla**, koska se lupasi kaikille sen mikä pätee osalle jo
ennestään ja olisi nyt luvannut väärin toiseen suuntaan. Teksti sanoo nyt kaksi tapausta:
isolla ruudulla `Back` on näkyvissä, pienellä palkki haetaan reunapyyhkäisyllä.

*Sivulöytö, ja se on kahden totuuden tapaus.* `Immersive.kt`:n oma docstring perusteli
transient-palkkien valintaa sillä että sovelluksen paluu ei ole niiden varassa, koska
*"lautaruudulla on oma `Back`-nappinsa nappisarakkeessa"*. Nappisarake purettiin 9.8.2026,
eikä `BoardScreen`issä ole `Back`ia missään tilassa. Tämä tiedosto sanoi asian oikein koko
ajan, eli sama asia eli kahtena totuutena kymmenen päivää, ja koodin puoleinen kopio oli se
väärä. Korjattu 20.8.2026 samassa muutoksessa.

**Korjaus 20.8.2026 illalla: puhelimen palkkiluku oli väärä, ja päätös ei muuttunut.**
Yllä olevassa taulukossa luki `72 dp` ja `noin 24 dp`, ja molemmat olivat laskettu arvatusta
summasta: tilapalkki plus navigointipalkki. Mittaus Pixel 8a:lta (`dumpsys window displays`,
lautaruutu auki) kaatoi sen. **Vaakatilassa puhelimen navigointipalkki on oikeassa reunassa**
(`sideHint=RIGHT`, 126 px eli 48 dp), joten se ei vie korkeutta lainkaan, ja pystysuunnassa
jää pelkkä tilapalkki, 28 dp. Oikeat luvut ovat siis 27,9 dp palkkien kanssa ja 30,3 dp ilman.

**Piilotuksen hyöty puhelimella on siis noin 2 dp eikä neljännes**, ja se on iso ero
perustelun kokoon. Päätös pysyy silti kahdesta syystä. Piilotus ostaa yhä hieman, ja
pieni etu ilman haittaa on yhä etu, koska poistumistie ei ole piilotuksen varassa samalla
tavalla kuin tabletilla: reunapyyhkäisy toimii. **Ja se mitä puhelimella oikeasti ostetaan,
on leveys eikä korkeus:** sivussa oleva palkki vie 48 dp leveyttä, ja tämä tiedosto sanoo
muualla että pienellä puhelimella juuri leveys on rajoittava mitta. Piilotus tekee siellä
siis sen minkä sen piti tehdä, mutta toisella akselilla kuin miksi se kirjattiin.

*Miksi virhe ei näkynyt.* Väärä luku ei muuttanut yhtään päätöstä: puhelimella varaa
kynnykseen on yli 300 dp, joten 72 ja 28 antavat saman vastauksen. Testi meni läpi, ehto
toimi laitteella, ja ainoa asia joka oli väärin, oli se mitä luvusta sanottiin. Sama muoto
kuin `koetin`-termin työkalumaastossa: oikea lopputulos ei todista sitä mekanismia jonka
nojalla se selitettiin. `DgBoardBarsTest` väittää nyt myös varan koosta, jotta luvun
vaihtaminen ei enää ole huomaamaton.

*Mitä tämä ei kumoa.* Tabletin luvut mitattiin eikä arvattu, ja niiden nojalla tehty päätös
on todennettu ruudulta. Puhelimen puoli on nyt todennettu samalla tarkkuudella:
`statusBars visible=false` ja `navigationBars visible=false` lautaruudun ollessa auki, eli
piilotus on siellä yhä voimassa.

*Muita eleitä ei lisätty, ja se on päätös eikä lykkäys* (Tommi 20.8.2026: *"tämä riitti eli
ei muita eleitä"*). Ehdolla olivat ottelusta toiseen siirtyminen, kootun siirron peruminen ja
sivupaneelin esiin ja piiloon. Ne kaikki ratkaisisivat asian jota kukaan ei ole nimennyt
ongelmaksi, ja alkuperäinen pyyntö *lisää eleitä* osoittautui yhdeksi ongelmaksi jonka
korjasi piilotuksen ehdollistaminen.

Kirjataan tänne kahdesta syystä. Kolme elettä on koodin puolelta halpoja lisätä nyt kun
`pullDownToRefresh` on jo olemassa, joten ilman kirjausta mikään ei myöhemmin muistuttaisi
että ne jätettiin tekemättä tarkoituksella. Ja jokainen uusi ele maksaa rivin manuaalissa,
eli sen saman hinnan jonka napit maksoivat: *näkymätön ele on kirjoitettava johonkin*.

*Todennettu ruudulta samana päivänä (Tommi 20.8.2026):* **palkki näkyy alhaalla ja lauta on
entisen kokoinen.** Molemmat puolet siis pitävät, ja jälkimmäinen on se joka ei ollut
itsestään selvä: laskelma sanoi että nappula pysyy 54 dp:ssä, ja silmä sanoo saman. Ilman
tätä katsomista väite olisi jäänyt laskennaksi, ja juuri tämän ruudun historiassa on kaksi
tapausta joissa oikea laskenta sai väärän syötteen (`CHECKER_MAX` 56 dp ja numerorivin
korkeus) eikä virhe näkynyt testissä, koska sama väärä luku oli myös testin syötteenä.

#### Palkkien tila ei voi ohjata päällepiirtoa, ja mekanismi vaihdettiin (10.8.2026)

**Päällepiirto ei ilmestynyt kertaakaan laitteella**, ja syy on rakenteellinen eikä säädettävä.
`TransientInfo` luki näkyvyytensä `WindowInsets.areStatusBarsVisible`istä, joka on johdettu
inseteistä, ja `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` on valittu **täsmälleen siksi ettei se
muuta insettejä**. Sama ominaisuus joka teki palkeista ilmaisia korkeuden kannalta tekee lipusta
pysyvästi epätoden. Kaksi 9.8. tehtyä päätöstä nojasivat siis saman lipun vastakkaisiin puoliin,
eikä kumpikaan yksinään ollut väärä.

Näyttö on kolmesta suunnasta, koska yksikään niistä ei yksin riitä. Kolmessa kaappauksessa palkit
ovat näkyvissä eikä päällepiirtoa ole. Lauta ei kutistunut palkkien tullessa esiin, eli
`safeDrawing` ei myöskään muuttunut. Ja logcatissa `WindowInsets changed` esiintyy vain ruudulle
tullessa, ei kertaakaan pyyhkäisyn hetkellä: sovellukselle ei siis lähetetä mitään mistä se voisi
päätellä palkkien näkyvän.

**Injektoitu ele ei ole selitys, ja se suljettiin pois mittaamalla.** Sama `adb shell input swipe`
laukaisi vetohaun samassa ruudussa samalla laitteella, eli sovellus vastaanottaa kosketuksen. Ilman
tätä kontrollia löydös olisi ollut erottamaton työkalun rajoitteesta, ja juuri se rajoite osui
samassa ajossa takaisin-eleeseen.

**Uusi mekanismi (Tommin valinta 10.8.2026): sama veto alas, lyhyempänä.** Tiedot ovat näkyvissä
sormen ollessa alhaalla ja katoavat kun ote irtoaa; kynnyksen ylittävä veto hakee sivun kuten
ennenkin. Yksi ele kantaa siis kaksi tekoa matkan mukaan, ja pidempi niistä on se joka maksaa
sivustolle.

Kolme syytä juuri tähän muotoon. Tunnistin on **jo mitattu toimivaksi tällä laitteella**, toisin
kuin oma reunapyyhkäisy, jonka kohdalla on avoinna päästääkö järjestelmä eleen sovellukselle asti
immersive-tilassa. **Ajastinta ei tarvita**, koska katoaminen on otteen irrottaminen eikä kulunut
aika, ja se säilyttää 9.8. kirjatun ehdon ettei näkyvyys ole oma arvio. Ja **lauta ei kutistu**,
mikä oli ainoa hinta siinä vaihtoehdossa jossa transient-tila olisi purettu.

Mitä päätöksestä säilyi ja mitä ei. Säilyi se mikä oli sen sisältö: tiedot maksavat nolla dp:tä,
ilmestyvät eleellä ja katoavat itsestään. Ei säilynyt se mikä oli sen mekanismi, eli *sama ele
kuin palkeilla*. Palkit ja tiedot ovat nyt kaksi eri elettä, ja se on tämän ratkaisun tunnettu
hinta: manuaali kantaa sen, koska ele jota ei näy ruudulla on kirjoitettava johonkin.

**Todennettu laitteella samana päivänä, ja ele pysäytettiin kesken todennusta varten.**
`adb shell input motionevent` erottaa painalluksen ja irrotuksen toisistaan, joten kaappaus
otettiin sormen ollessa alhaalla. Se on tämän ruudun kohdalla ainoa tapa nähdä tila jota ei ole
kuin eleen ajan, eikä `input swipe` kelpaa siihen lainkaan: se päättää eleen ennen kuin
kaappaus ehtii.

Neljä kohtaa mitattiin yhdestä kuvasta: otsikkorivi (`May 26 Deja Vu, Round 3`), molemmat
pelaajapaneelit, ottelun pituus ja sivuston oma kehote. Roolivärit ovat oikein päin, ja se on
niistä ainoa täysin hiljainen: `tommih` on alempana kermanvärisellä pallolla ja `pirkko` ylempänä
punaisella, eli sama järjestys kuin nappuloilla. Pip-vahdin riviä ei ollut ruudulla, eli lukema
oli `Agrees`, joten roolipäättely nojasi mitattuun eikä oletukseen. Irrotuksen jälkeinen kaappaus
on tavulleen sama tiedosto kuin tyhjä lauta, eli tiedot katosivat kokonaan.

Alareunan pehmuste ei osunut ongelmaksi, ja syy on kirjattava jottei se yllätä toisella
laitteella: alempi ryhmä käyttää kiinteää 4 dp:tä eikä lue insettiä, mutta tässä asennossa
navigaatiopalkki on oikeassa reunassa ja alareunan inset on nolla. Ehto on siis laitteen eikä
koodin, ja se pettää sillä laitteella jolla palkki on alhaalla.

#### Skaalautuvuus mitattiin, päätös tehtiin, ja työ pysäytettiin siihen (9.8.2026)

Tommi kysyi skaalautuuko lauta pienemmälle puhelimelle kuin Pixel 8a. Luvut ovat laskettuja
samasta johtamisesta jonka koodi ajaa, eivät mitattuja laitteilta, eikä yhtäkään pientä laitetta
ole ajettu.

| Ikkuna | Nyt | Ilman sivupaneelia | Ilman paneelia + pystysäästöt |
|---|---|---|---|
| Pixel 8a, 914 x 411 | 29,2 (korkeus rajoittaa) | 29,2 | 32,5 |
| 5 tuumaa, 640 x 360 | 18,8 (leveys rajoittaa) | 24,1 | 27,4 |
| hyvin pieni, 592 x 320 | 16,4 (leveys) | 20,1 | 23,4 |

**Kaksi asiaa jotka eivät ole ilmeisiä ja jotka kannattaa lukea ennen kuin tähän koskee.**
Sivupaneeli on Pixelillä ilmainen ja pienellä puhelimella kallis, koska rajoittava mitta on eri:
sama 200 dp on 22 % isosta ikkunasta ja 31 % pienestä. Ja pystysäästöt (numerorivit, keskitila,
kehyksen pehmuste) maksavat itsensä takaisin **vain siellä missä korkeus rajoittaa**, eli pienellä
puhelimella vasta paneelin poistuttua. Ne kaksi kysymystä ovat siis yksi kysymys.

**Kaksi vaihtoehtoa mitattiin ja hylättiin.** Sivupaneeli laudan ylle tai alle hävitään joka
koossa (Pixelillä 29,2 → 22,8, pienellä 18,8 → 17,7 kun rivi on 64 dp), koska vaakatilassa
korkeus on niukka mitta ja kiila on pystysuora. Sama peruste kaataa pystyasennon: lauta on 14
sarakkeen levyinen, joten pystyssä leveys puristaa nappulan 17,4 dp:hen. Vaakalukko on siis
mitoituksen kannalta oikea eikä vain tottumus.

**Päätös (Tommi): pelkkä lauta ruudulla, ja pelaajien tiedot samaan pyyhkäisyyn kuin otsikko.**
Nimet, pisteet, pip-luvut ja sivuston kehote tulevat näkyviin samalla eleellä ja katoavat samalla
tavalla itsestään. Se maksaa nolla dp joka näytöllä, se on yksi muoto eikä kaksi, ja se on sama
ratkaisu jonka otsikko sai jo. Vieritystä ei harkita: se veisi pelaamisen vähäisenkin immersion,
mikä on sama peruste jolla vieritettävä lauta hylättiin 8.8.

**Sivupaneeli purettiin 9.8.2026 illalla, ja pysäytys jäi voimaan muilta osin.** Tommin
kuittaus oli *"eleet kunniaan"*, eli päätös purettiin siihen samaan muotoon jonka otsikko sai
aiemmin samana päivänä: nimet, pisteet, pip-luvut, ottelun pituus ja sivuston kehote ovat nyt
päällepiirtoa, joka tulee näkyviin palkkien pyyhkäisyllä ja katoaa samalla tavalla. Ruudulla on
pelkkä lauta. Sijainti kantaa saman merkityksen kuin sarakkeessa: vastustaja ylhäällä ja
kirjautunut pelaaja alhaalla, eli samalla puolella kuin hänen nappulansa.

Mitä se maksoi ja mitä se osti, laskettuna eikä mitattuna: Pixel 8a:lla **nappula ei muuttunut
lainkaan** ja on yhä 29,2 dp, koska korkeus rajoitti jo ennen leveyttä. Pienellä puhelimella
sama muutos on koko ero. Tämä epäsymmetria on nyt testissä eikä vain tässä taulukossa
(`DgBoardWidthTest`), eli väite on tarkistettavissa ilman pientä laitetta.

**Kolme pystysäästöä jäivät tekemättä, ja ne ovat yhä tämän osion suunnitelmaa.** Ne maksavat
itsensä takaisin vain siellä missä korkeus rajoittaa, eli Pixelillä heti ja pienellä puhelimella
vasta nyt kun paneeli on poissa.

**Alkuperäinen pysäytys ja sen perustelu ovat yhä voimassa** kaikelle muulle ulkoasutyölle.
Tommin sanoin tämä voi viedä kauas
ensisijaisesta tavoitteesta, ja monella on jo oma vakiintunut tapansa pelata DailyGammonia.
Ulkoasu on nyt kelvollinen sekä puhelimella että tabletilla, ja se mikä erottaa tämän sovelluksen
sivustosta on arkisto ja pelattavuus, ei nappulan koko. **Tämä osio on siis mitattu suunnitelma
eikä tekemätön työ:** se odottaa hetkeä jolloin joku pieni laite tai jokin muu syy tekee siitä
ajankohtaisen, eikä sitä oteta työhön vain siksi että luvut ovat olemassa.

#### Manuaali on ruutu, ei tiedosto (9.8.2026)

`HelpScreen`, reitti `help`, sisäänkäynti otteluluettelossa asetusrivin alla. Viisi osiota:
lukutila lupauksena, kolme elettä, värit ja pip-varaus, viestien haku yksi kerrallaan, yhden
laitteen rajaus. Osioita oli neljä 9.8.2026 iltaan asti: viestiosio kirjoitettiin samana
päivänä kun ruutu jota se kuvaa alkoi olla olemassa, ja se on manuaalin ainoa kohta joka
koskee peruuttamatonta tekoa.

Kolme valintaa jotka eivät ole pääteltävissä tiedostosta. Teksti puhuu **pelaajalle**, koska
kanoni rajaa kohderyhmän niin (`Kenelle`-osio). Se on englantia kuten muukin käyttöliittymä.
Ja eleet kerrotaan tekoina eikä niminä (`pull down`, ei `pull-to-refresh`), koska nimi auttaa
vasta löytämisen jälkeen.

Sisäänkäynti on otteluluettelossa eikä lautaruudulla, ja se on ehto eikä sijoittelu: lautaruudulla
ei ole nappeja, joten manuaali on löydettävä **ennen** sinne menoa eikä sieltä käsin.

**Laudan leveys lasketaan nyt kehyksen ulkopuolella** (`DgBoard.frameWidth`), jotta kehys
kutistuu sisältönsä levyiseksi. Sama funktio ajetaan kehyksen sisällä (`DgBoard.contentWidth`),
eikä se ole toiste vaan ehto: kaksi kirjoitettua laskentaa ajautuisi erilleen, ja silloin kehys
olisi eri levyinen kuin lauta jonka se piirtää. `DgBoardWidthTest` vahtii juuri tätä yhtäsuuruutta.

**Pip-vahdin rivi näkyy vain kun sillä on sanottavaa** (Tommin pyyntö 9.8.2026: "on toiminut
moitteettomasti"). `Agrees` ei piirrä mitään, `Disagrees` ja `Unavailable` piirtävät kuten
ennen. Ehto on se mikä pitää aiemman perustelun voimassa: rivi on vahti eikä tulos, ja sen
tehtävä on kertoa milloin nappuloiden väreihin ei voi luottaa. Luvut itse ovat yhä
sivupaneelissa pelaajittain, joten ruudulta katosi summa eikä lähde. **Tämä ei vapauta
korkeutta:** rivi on keskitilan sisällä noppien välissä, joten muutos on luettavuutta eikä
nappulan kokoa. (Keskitila oli tätä kirjoitettaessa kiinteä 59 dp; se johdetaan 10.8.2026
alkaen palan koosta, mikä ei muuta tämän kohdan päättelyä.)

Asennettu Pixel 8a:lle samassa sessiossa (`installDebug`), ja Tommi katsoi sekä laudan että
eleet: koko ja värit kelpasivat sellaisenaan, ja eleet toimivat yhteen suuntaan kukin (ks. yllä).
**Manuaaliruutu katsottiin laitteelta 20.8.2026**, yksitoista päivää sen kirjoittamisen jälkeen.
Ruutu piirtyy kokonaan Galaxy Tab S7+:lla: viisi osiota, ei vieritystä, `Back` yläkulmassa.
Rakenteessa ei ollut vikaa, mutta **kaksi tekstiä oli väärässä ja molemmat viittasivat asiaan
jota ruudulla ei ole.**

Ensimmäinen oli saman päivän oma jälki. Palkkiehdon myötä manuaalin poistumiskappale
korjattiin, mutta samassa osiossa oleva viimeinen rivi jäi sanomaan *"swipe downwards from the
top edge to bring back the status bar"*, ja tabletilla tilapalkki on nyt pysyvästi näkyvissä.
Korjaus oli siis puolittainen ja tuotti uuden väärän lauseen tunnin ikäiseen tekstiin.

Toinen oli vanhempi ja itsenäinen: manuaali neuvoi *"open Messages from your match list"*,
kun rivin nimi luettelossa on `Message archive` (`top_open_messages`). **Ohje osoitti
kontrolliin nimellä jota ruudulla ei lue**, eli täsmälleen se vika jota `selkokieli`-skill
etsii. Se on ollut tekstissä 9.8.2026 alkaen.

*Mitä tämä sanoo katsomisesta.* Ruutu oli kirjoitettu, käännetty ja testattu, eikä kumpikaan
virhe ollut sellainen jonka kääntäjä tai testi voi nähdä: molemmat ovat tosi lauseita
väärästä ruudusta. Yksitoista päivää katsomatta jättämistä maksoi siis kaksi virhettä, joista
toinen ehti elää manuaalissa koko sen ajan.

*Yksi kohta jätettiin ennalleen harkiten.* Otsikko `The board screen has no buttons` on
tabletilla lähellä ristiriitaa oman leipätekstinsä kanssa, joka sanoo että `Back` on aina
näkyvissä. Se on silti totta siinä merkityksessä jossa se on kirjoitettu: sovellus ei piirrä
lautaruudulle yhtään nappia, ja `Back` on järjestelmän eikä sovelluksen. Leipäteksti selittää
eron heti seuraavalla rivillä, joten muutos olisi makuasia eikä korjaus.

### Sivupaneeli palasi, ja ehto vaihtui varauksesta ylijäämään (9.8.2026 illalla)

Paneeli purettiin aamupäivällä ja palautettiin samana iltana, joten kyse näyttää
mielenmuutokselta. Se ei ole sama paneeli, ja ero on juuri se joka purkuun johti.

**Vanha varasi 200 dp ennen laudan mitoitusta.** Se oli laitekohtainen kauppa, ja purkuhetkellä
kirjattu perustelu on yhä oikea: Pixel 8a:lla korkeus rajoittaa, joten varaus oli ilmainen,
mutta pienellä puhelimella leveys rajoittaa ja sama varaus kutisti nappulan 24,1:stä
18,8 dp:hen. **Uusi ei varaa mitään**: lauta mitoitetaan ensin, ja paneeli saa vain sen mikä
jää yli. Kapealla laitteella ylijäämää ei ole, jolloin paneelia ei ole eikä lauta kutistu.
Sama koodi tekee siis molemmilla oikein ilman että se tuntee laitteen.

**Syy paluuseen oli ruudulla näkyvä musta, ja sen määrä on laskettu.** Pixel 8a:n vaakaikkuna
on 914 x 411 dp, ja nappulan koko tulee korkeudesta: 30,0 dp korkeudesta vastaan 34,9 dp jos
leveys ratkaisisi. Lauta on siis noin 615 dp leveä ja sivuille jää noin **299 dp leveyttä jota
lauta ei voi käyttää millään**. Se oli tyhjää taustaa, ja Tommi pyysi tiedot siihen.

**Vedon aikainen päällepiirto poistui samalla** (Tommin päätös). Se ei ollut huono ele vaan
tarpeeton: tiedot jotka ovat koko ajan näkyvissä eivät tarvitse paljastajaa. Veto alaspäin
hakee yhä sivun uudelleen, eli ele kantaa taas yhden teon eikä kahta, ja se on elinkaarena
lyhyt: kaksitekoinen muoto eli yhden päivän.

**Nopat siirtyivät kahdesti, ja loppusijainti on kiilan mitta eikä välimatka.** Ensin ne
olivat laitoihin asti, jolloin näytön lova osui vasemmanpuoleisen nopan päälle. Sitten ne
keskitettiin nippuun, jolloin lova ei enää osunut mutta ne eivät osoittaneet kummankaan
puoliskoon. Lopullinen paikka on **kuuden kiilan ryhmän keskellä**, eli kolmannen ja
neljännen kiilan kohdalla (Tommi 10.8.2026). Leveys johdetaan nappulasta samalla suhteella
kuin lauta, joten ryhmä pysyy kohdakkain kiilojen kanssa myös toisella laitteella.

Jako vasen/oikea säilyi kaikissa kolmessa muodossa, koska se kantaa merkityksen: vastustaja
vasemmalla, oma oikealla, sama kuin puoliskoilla. Samasta lovasyystä paneeli on laudan
**vasemmalla** eikä oikealla kuten vanha: teksti sietää lovan naapuruuden paremmin kuin
nopan paikka, joka on luettava yhdellä silmäyksellä.

**Kumpi antaa periksi, on kysytty ja päätetty: paneeli** (Tommi 10.8.2026). Kun teksti ei
mahdu, turnauksen nimi katkeaa kolmeen pisteeseen; lauta ja nappula eivät kutistu koskaan sen
mukaan mitä paneelissa lukee. Päätös on kirjattu tänne siksi että vastakkainen sääntö on
houkutteleva juuri sillä hetkellä kun katkennut nimi ärsyttää, ja se palauttaisi täsmälleen
sen kaupan jonka vuoksi vanha paneeli purettiin.

### Lokerosarake kantaa pelaajatiedot silloin kun paneelia ei ole (14.8.2026)

**Tabletilla ruudulla ei ollut pisteitä, pippejä eikä pelaajien nimiä lainkaan**, ja se
huomattiin vasta kun Crawford-tähteä yritettiin todentaa laitteella. Sivupaneelin ehto on
mitattu leveys, ja Galaxy Tab S7+:lla lauta vie 1244 dp 1285:stä eli paneelille jää 41 dp.
Ehto toimi kuten on kirjattu, mutta sen seurausta ei ollut sanottu ääneen: **paneelin
puuttuminen ei ole vain tilan säästö vaan tiedon katoaminen**, eikä otteluluettelo kanna
pisteitä lainkaan.

Nimi, pisteet ja pipit piirtyvät nyt lokerosarakkeen päihin, vastustaja ylhäälle ja
kirjautunut alas, eli samaan järjestykseen kuin puoliskot, lokerot ja paneeli. Ehto on sama
kuin nappien kotipaikalla (`TRAY_ACTIONS_MIN`), koska se on jo se mitattu leveys jolla
sarakkeen teksti on luettavaa; toinen kynnys olisi toinen ylläpidettävä luku ilman omaa
mittausta.

**Turnauksen nimi ja ottelun pituus jäävät pois**, eivätkä katkaistuina mukaan. 10.8.2026
päätettiin että katkeava nimi on hyväksyttävä hinta paneelissa, ja se päätös ei siirry tänne:
paneelissa katkaisun vaihtoehto on laudan kutistaminen, sarakkeessa sen vaihtoehto on tyhjä
tila. Sarake on noin 110 dp.

**Puhelimella tämä ei näy**, koska siellä paneeli mahtuu (914 x 411 dp, laudan viereen jää noin
299 dp). Muutos on siis tabletin näköinen, mutta sen ehto on leveys eikä laite, sama muoto kuin
paneelilla ja napeilla.

**Korjaus samana iltana: tabletilla nämä eivät enää näy, koska paneeli palasi.** Nappulalle
tuli yläraja (ks. seuraava osio), joten Galaxy Tab S7+:lla sivupaneeli mahtuu taas ja kantaa
pelaajatiedot kuten puhelimella. Sarakkeen tiedot jäävät siis koodiin sitä välialuetta varten
jossa lauta täyttää leveyden mutta sarake on vähintään 96 dp: Tommin peruste on että sovellus
päätyy erilaisille näytöille kuin nämä kaksi. Rivi on tässä eikä poistettuna siksi, että
edellinen kappale sanoisi muuten yhä tabletista jotain joka ei pidä paikkaansa.

### Nappulan yläraja: lauta lakkaa kasvamasta leveyden mukana (14.8.2026)

Sivupaneeli palasi tabletille sillä että **lauta ei enää ota kaikkea leveyttä**: nappulan katto
on 54 dp, jolloin kehys on noin 1094 dp ja paneelille jää noin 184 dp. Keskitila irtosi samalla
kuution mitasta ja on nyt kaksi nappulaa (mitattuna 112 dp, aiemmin 82 dp), koska kuution mitta
oli alaraja eikä korkeus.

**Vaihtoehto oli venyttää keskitilaa 200 dp:hen, ja se hylättiin mitatusta syystä:** ylijäämä
olisi ollut tasan paneelin kynnys eli 150 dp, jolloin asettelu vaihtaisi muotoaan pienimmästäkin
muutoksesta ikkunan mitoissa. Yläraja antaa saman tilan ilman että keskitila vie neljänneksen
laudan korkeudesta.

**Ensimmäinen arvo 56 dp oli väärä, ja vain laiteajo kertoi sen.** Se jätti ylijäämäksi 144 dp
eli kuusi dp kynnyksen alle, koska ylijäämä laskettiin ikkunan leveydestä (1317,6 dp) eikä
laudalle tarjotusta tilasta (1285,6 dp); ruudun oma pehmuste vie 32 dp ennen laudan mitoitusta.
Testi oli vihreä, koska sama väärä luku oli sen omana syötteenä. Molemmat korjattiin, ja testi
vaatii nyt marginaalin eikä kynnystä.

### Pip-luku siirtyi numerorivin muurin kohdalle (15.8.2026)

**Tommin pyyntö: pipit samalle tasolle kuin kiilojen numerointi, muurin kohdalle.** `NumberRow`
piirtää kaksi kuusikkoa ja niiden välissä oli tyhjä `Spacer(weight(1f))`, tasan sen levyinen
väli joka muurilla on `HalfBoard`in riveillä. `BarPips` (uusi) täyttää nyt saman välin
pelaajan omalla pip-luvulla: ylärivillä (13-24) vastustajan, alarivillä (12-1) oman
pelaajan, sama vastustaja/oma-jako kuin `SidePanel`illa ja lokerosarakkeella.

**Korvaa eikä lisää** (Tommin valinta kahdesta): pip-luku poistui `PlayerPanelView`in
(sivupaneeli) ja `TrayPlayerFacts`in (lokerosarake) pistekentästä kokonaan, koska sama luku
kahdessa paikassa ei ollut tarkoitus. Kumpikin näyttää nyt vain pistekentän. `board_pips` ja
`board_pips_hidden` -merkkijonot poistuivat samalla, koska niillä ei ollut enää kutsujaa.

**Ei sivun sanaa "pips" mukana.** Kapea muurin väli sietää vain paljaan luvun, sama korkuisena
kuin pistenumerot (`labelSmall`, `numberRowHeight()` ei muuttunut). Väri on `Palette.PipOk`,
joka oli määriteltynä muttei käytössä ennen tätä.

**Piilotettu pip-luku on tyhjä väli eikä selittävä teksti.** Aiempi `board_pips_hidden`
("Pip counts are hidden...") selitti puuttumisen suoraan pelaajapaneelissa; se selitys ei
siirtynyt mukana, koska kapea väli ei mahduta lausetta. Manuaalin "värit ja pip-varaus"
-osio kattaa saman asetuksen, mutta rivi tässä ei enää selitä itseään laudalla samaan tapaan
kuin ennen. Kirjattu tänne siksi ettei tämä näytä unohdukselta seuraavalla lukukerralla.

**Ei todennettu laitteella.** Kirjoitettu näkemättä ruutua, kuten moni muukin kohta tässä
tiedostossa; `:app:compileDebugKotlin` meni läpi.

#### Todennettu laitteella 16.8.2026, ja väri sekä koko vaihtuivat sen jälkeen

Yllä oleva rivi *"ei todennettu laitteella"* on nyt vanhentunut ja jätetään näkyviin, koska
laiteajo muutti kahta valintaa. Tabletilla (Galaxy Tab S7+) avattiin oikea ottelu ja luvut
piirtyivät oikeille puolille, vastustaja ylärivillä ja oma alarivillä.

**Väri on `Palette.Accent` eikä `PipOk`.** Turkoosi `PipOk` on sama väri kuin kuutiolla, ja
ne olivat laudan ainoat kaksi turkoosia asiaa. Yhteys on väärä kahdesta syystä: kuutio on
**numero samalla laudalla**, eli samaa lajia kuin pip-luku, ja `PipOk` tarkoittaa
`PipCheckLine`ssä täsmäävää tarkistusta eikä pelaajan lukua. Sama väri kahdessa merkityksessä
on pahempi kuin sama väri kahdessa paikassa.

*Mitä oranssi maksaa, koska se ei ole vapaa väri sekään.* Se on sama kuin parittomien kiilojen
väri ja sama kuin vasemman paneelin `15 point match` -teksti. Ero on lajissa: kiilat ovat
pintoja eivätkä numeroita, ja ottelupituus on ottelutason tieto siinä missä pip on
pelaajatason. Vaihtokauppa hyväksyttiin tietoisesti (Tommi 16.8.2026), eikä uutta väriä
lisätty palettiin.

**Koko on `PIP_FONT_SCALE` eli 1,3-kertainen pistenumeroihin nähden**, ja kasvatettu on vain
`fontSize` eikä `lineHeight`. Ehto on rakenteellinen eikä makuasia: `numberRowHeight()` johtaa
laudan leveyden `labelSmall`in rivikorkeudesta, joten kasvanut rivi kaventaisi lautaa. 11sp
fontti 16sp:n riviboksissa kestää tämän kertoimen. **Isompi kerroin vaatii että
`numberRowHeight()` laskee maksimin kahdesta tyylistä**, ja se on kirjattu vakion viereen.

*Laiteajosta jäi myös ympäristöhavainto joka on jo `docs/TESTAUS.md`:ssä:* tabletti lukkiutui
kesken ajon, ja `screencap` palautti nollan tavun kuvan ilman virheilmoitusta. `mCurrentFocus`
näytti `BiometricPrompt`in, eli tarkistus toimi juuri niin kuin se on kuvattu.

Hinta osuu puhelimeen keskitilan kautta: nappula 31,2 → 29,3 dp. Yläraja itsessään ei tee siellä
mitään, koska nappula on 30 dp:n luokkaa ja korkeus rajoittaa jo ennen kattoa.

### Pistetilanne on away-notaatiota, pelaajakohtaisesti (21.8.2026, Tommin valinta)

Sovellus näytti saadut pisteet ja ottelupituuden erikseen. Nyt kummankin pelaajan nimen alla
on se **paljonko häneltä puuttuu voittoon** (`12a`, `11a`), ja Crawford-tähti liittyy siihen
(`4a*`).

**Peruste on domain-tasoinen** (`SUBSTANSSI.md` kohdat 82 ja 90): analyysin kolmijaon painot
tulevat kokonaan ottelutilanteesta, ja päätöksen kannalta merkitsevä suure on puuttuvat
pisteet. Gammon-go ja gammon-save ovat asemia jotka luetaan juuri niistä. Notaatio ei myöskään
tarvitse selitystä ruudulla, koska se on analyysin vakiintunut merkintä eikä tämän projektin
oma lyhenne.

**Johdos on `BoardState.awayOf` eikä piirtokoodissa**, koska puuttuvat pisteet ovat lajin
sääntö. Teksti syntyy `BoardScreen.kt`:n `scoreText`issä jota molemmat piirtoreitit kutsuvat
(`PlayerPanelView` ja `TrayPlayerFacts`), jottei ruutu sano eri asiaa eri laitteella.

**Järjestyskysymys ratkesi sijoituksella eikä valinnalla.** Kumpi luku on ensin, ei ole enää
kysymys: kumpikin asuu oman pelaajansa nimen alla, ja pari luetaan ylhäältä alas kuten muukin
ruudulla.

**Ottelupituusrivi jäi paikalleen** (Tommi 21.8.2026: *"ei piiloteta ainakaan vielä"*).
Alkuperäinen suunnitelma oli poistaa se, ja peruste oli että away-luvut kertovat pituuden.
Peruste ei kanna pelaajakohtaiseen muotoon: `12a` ja `11a` eivät kerro ottelun pituutta, joten
poisto olisi hävittänyt tiedon. Jos rivi joskus poistuu, perusteen on oltava tilankäyttö eikä
tämä.

### Viestiruutu ja jonon haku (`app/ui/Messages*`, lisätty 9.8.2026)

**Sovelluksen ensimmäinen peruuttamaton teko, ja koko osio on sen ympärillä.** Lukutilan ruudut
hakevat kuluttamattomia sivuja, joten niissä väärä haku maksaa yhden turhan pyynnön. Täällä
väärä haku syö kohteen jonosta, eikä sitä saa takaisin tarkistamaan mikä se oli.

Väli joka puuttui oli muunnos eikä jäsennin. Tässä tiedostossa luki seuraavana askeleena
`MessageParser` + lajittelu, mutta lajittelu oli jo `InboxParser`issa (`classify`) ja jäsennin
oli koko ajan olemassa. Puuttui `InboxItem.toMessage`, eli yksi funktio jossa on enemmän
perustelua kuin koodia. Kirjaus
oli siis oikeassa siitä että jokin puuttui ja väärässä siitä mikä, ja ero näkyi vasta kun
kohtaa yritti tehdä.

Neljä ehtoa `MessagesViewModel`issä, kukin oma testinsä:

| Ehto | Miten se on rakenteena | Mitä testi väittää |
|---|---|---|
| Haku vain pyynnöstä | `init` ei hae, luokassa ei ole ajastusta eikä uudelleenyritystä | ruudun avaaminen ei lähetä pyyntöä |
| Osoite luetaan | polku Top Pagen linkistä, seuraava sivun `Next >>`:stä | pyydetty polku on merkilleen sivun antama |
| Yksi kerrallaan | käynnissä oleva haku hylkää toisen kutsun | kaksoisnapautus kuluttaa yhden kohteen |
| Kirjoitus ennen näyttöä | `archive` palauttaa vasta tallennuksen jälkeen | viesti on kannassa siinä hetkessä kun se on tilassa |

**`TopPage.hasMessageNotice` on nyt polku eikä lippu** (`messageQueuePath`). Lippu olisi
pakottanut kirjoittamaan `/bg/nextgame` koodiin, eli kokoamaan osoitteen juuri siinä yhdessä
paikassa jossa väärä osoite ei tuota virhettä vaan peruuttamattoman teon. Boolean on yhä
olemassa johdettuna, jottei sama tieto ole kahtena kenttänä.

Kaksi hylättyä vaihtoehtoa, koska kumpikin näytti luontevammalta. Polku **reitin yli**
`MESSAGES_ROUTE`en olisi vaatinut koodauksen ja purun kuten `BoardRoute`ssa, ja väärin purettu
polku olisi täällä teko eikä virheilmoitus; ruutu ottaa sen siksi suoraan näkymämallilta.
Ja **hakunappi suoraan otteluluettelon odotusilmoitukseen** olisi asettanut kuluttavan napin
paikkaan josta arkistoa ei näe, eli luvannut tuloksen näyttämättä minne se päätyy. Ilmoituksen
nappi vie ruutuun, ja kuluttava teko on siellä.

#### Vastauskenttä, ja sopimusmuutos jonka se maksoi (22.8.2026)

**Kanoni sanoi ettei tämä näkymämalli voi tehdä sivustolla mitään, ja se muuttui.**
`AppContainer` erottaa `FormSender`in `PageFetcher`istä siksi, että mallin kyky toimia
luetaan konstruktorista, ja siinä luki nimeltä että `MessagesViewModel` ei sitä saa.
Vastauksen lähettäminen on peruuttamaton teko, joten lause ei voinut jäädä voimaan.
Sopimusmuutos-protokolla ajettiin: **luettelo muuttui, periaate ei.** Konstruktori kertoo
kyvystä jatkossakin totuuden, nyt toiseen suuntaan, ja takeen kantava puoli eli
`FormSubmission`in sisäinen konstruktori pysyi koskemattomana. Tommi vahvisti 22.8.2026.

~~**Vastauskenttä näkyy vain juuri saapuneen viestin kohdalla** (Tommin päätös).~~
**Purettu 30.8.2026 Tommin pyynnöstä, ks. oma alaosio alempana.** Alkuperäinen rajaus oli
tyypissä eikä ruudun ehdossa: `ReplyForm` kulki `QueueUiState.Saved`in mukana, ja arkiston
`Message` ei kantanut osoitetta, kentän nimeä eikä pituusrajaa.

Rajaus ei johtunut esteestä, ja se sanottiin ääneen jo kirjattaessa: mittaus 22.8.2026
osoitti että `POST /bg/sendmsg/<id>` on osoitettu **käyttäjälle eikä viestille**, joten
arkistosta vastaaminen onnistuisi teknisesti. Se olisi vaatinut kenttien lisäämisen malliin
ja kantamigraation, eikä sitä tehty koska sitä ei haluttu. **Juuri tuo hinta maksettiin
30.8.2026**, kun haluaminen muuttui: kirjaus piti purun halvempana kuin se olisi ollut
ilman sitä, koska mikään ei ollut mitattavaa uudelleen.

`ReplyForm.write` on `BoardForm.press`in pari, ja se on portti samalla tavalla: `press` ei
voi painaa nappia jota sivu ei tarjoa, `write` ei voi kirjoittaa lomakkeeseen jota sivulla
ei ollut. Tyhjä teksti tuottaa nullin, **eikä tekstiä koskaan leikata**: puolikkaana perille
mennyt viesti jättäisi käyttäjän tietämättä mitä hänestä jäi sanomatta. Pituusraja hylkäsi
lähetyksen 24.8.2026 asti, ks. seuraava kohta.

Kolme päätöstä joita ei voi lukea koodista:

| Kysymys | Päätös | Peruste |
|---|---|---|
| Meneekö lähetetty vastaus arkistoon | kyllä, lähettäjänä oma tunnus | sivusto ei säilytä sitäkään, ja keskustelu jossa näkyy vain toinen puoli on vajaa |
| Mitä lähettämättömälle tekstille tapahtuu | säilyy kunnes jonosta otetaan seuraava kohde | kenttään sidottu tila toistaisi sen vian jonka takia sovellus osin on olemassa, ks. `SUBSTANSSI.md` kohta 50 |
| Mitä katkos tekee | virhe ja teksti jää kenttään | kuljetus ei toista lomaketta itse, koska lähetys ei ole idempotentti |

**Neljäs tila on omani enkä kysynyt sitä, ja se on tärkein.** `ReplyUiState.Unconfirmed`
tarkoittaa että palvelin vastasi 200:lla jotain muuta kuin kuittausta. Sitä **ei lueta
onnistumiseksi eikä epäonnistumiseksi**, koska kumpaakaan ei tiedetä, ja ruudun teksti sanoo
sen suoraan: tarkista sivustolta ennen kuin lähetät uudelleen, koska kahdesti lähetetty menee
kahdesti perille. Ilman tätä tilaa `DgResponse.Ok` olisi tarkoittanut vain että palvelin
vastasi jotain.

**Lomakkeen metodi luetaan sivulta 22.8.2026 alkaen.** `FormSubmission` kantaa nyt
`FormMethod`in, ja `DgClient.send` valitsee sen mukaan. Aiemmin se lähetti aina GETin, mikä
oli oikein niin kauan kuin ainoa käyttökohde oli lauta (`method=get`), mutta pikaviestin
lomake on POST. Ero ei ole muodollinen: uusinnan perustelu nojaa siihen ettei GET-teolla ole
runkoa, ja POSTilla se nojaus katoaa, joten POST menee kerran ja epäonnistuu näkyvästi.

#### Vastauskenttä on monirivinen, eikä pituusraja enää estä mitään (24.8.2026)

Sivuston oma kenttä on `INPUT SIZE=80 MAXLENGTH=80`, ja sovellus toisti sen sellaisenaan.
Kaksi erillistä asiaa ratkaistiin samana päivänä eri perusteella, ja niiden erottaminen on
tämän kohdan pysyvin osa: **moniriviseksi näyttäminen** on ulkoasua, **rajan poistaminen**
olisi ollut oletus palvelimen käytöksestä.

**Monirivisyys on `minLines = 3`, `maxLines = 8`** (`MessagesScreen.kt`). Esikuva on
selainlaajennus DGText2Area, joka vaihtaa saman kentän muotoon `textarea rows=10 cols=64`,
mutta kymmenen riviä on työpöydän mitta: puhelimella näppäimistö vie ison osan ruudusta,
joten kiinteä korkeus söisi tilaa myös lyhyeltä viestiltä. Kolme riviä näkyy aina, kenttä
venyy kahdeksaan ja vierii sen jälkeen, jolloin lähetysnappi pysyy ruudulla. Painettu
rivinvaihto tuli mukana samalla, ja se on hyväksytty samasta lähteestä: laajennuksen
`textarea` ottaa rivinvaihtoja vastaan ja `format_reply.js` esitäyttää kentän monirivisellä
siteerauksella, eli rivinvaihtoja on kulkenut saman `sendmsg`-lomakkeen läpi rutiinilla.

**Raja poistui vasta kun palvelimen käytös oli mitattu** (`docs/KOHDE.md`: palvelin ei
leikkaa vaan rivittää 80 merkin kohdalta). Kolme kohtaa muuttui yhtä aikaa, koska yhden
muuttaminen olisi tuottanut hiljaisen ristiriidan: kenttä ei enää rajoita kirjoittamista,
`ReplyForm.write` ei enää hylkää yli rajan menevää, ja laskuri sanoo pelkän merkkimäärän
ilman kattoa. `ReplyForm.maxLength` luetaan yhä sivulta, koska se on sivun oma tosiasia,
mutta se ei enää estä mitään.

**Rajan tilalle ei kirjoitettu suurempaa keksittyä lukua.** Kanoni sanoo että raja luetaan
sivulta eikä keksitä koodissa, joten valinta oli kahden välillä: sivun luku sellaisenaan tai
ei rajaa lainkaan. Arvattu tuhat olisi ollut juuri se arvaus jonka sääntö kieltää.

#### Arkistosta vastataan klikkaamalla, ja lomake kulkee viestin mukana (30.8.2026)

Tommin pyyntö samana päivänä: *"vastaus pitäisi olla viestin yhteyteen sen alle ja pitäisi
pystyä vastaamaan vastaanotettua viestiä klikkaamalla"*. Ensimmäinen puoli oli tehty jo
24.8.2026 (kenttä juuri saapuneen alla); toinen purkaa 22.8.2026 rajauksen, jonka purkuhinta
oli kirjattu valmiiksi ylle.

**Mekanismi: `ReplyForm` siirtyi tilasta viestiin** (`Message.replyForm`, kannassa neljä
nullable-saraketta, migraatio 6→7). Portin muoto säilyi vaikka paikka vaihtui: lomakkeen voi
yhä tuottaa vain jäsennin sivun omista tavuista, `sendReply` lukee sen
`MessagesViewModel.replyTarget`ista eikä ota parametria, ja `openReply` ei ota kohteeksi
viestiä jolla lomaketta ei ole. *Osoite luetaan, ei koota* pätee sanatarkasti: talletus vain
erottaa lukuhetken ja käyttöhetken toisistaan, ja talletettu osoite ei vanhene koska se
viittaa henkilöön eikä kuluneeseen viestisivuun.

**Vanha rivi ei saa kenttää, ja se on totta eikä puute.** Ennen tätä versiota tallennetuilla
riveillä ei ole lomaketta, eikä sitä voi täydentää: viestisivu kului lukuhetkellä.
Lähettäjän numerosta kokoaminen olisi juuri se keksitty osoite jota tässä projektissa ei
lähetetä. Sama koskee omia lähetettyjä viestejä ja ilmoituksia: klikkaus ei tee mitään, eikä
rivi ole klikattavan näköinen (sama muoto kuin lautanäkymän `playPath`-ehdossa).

**Klikkaus togglaa, eikä luonnos tyhjene kentän siirtyessä.** Kirjoitettu teksti on ainoa
kopio (`SUBSTANSSI.md` kohta 50), joten kentän avaaminen toisen viestin alle kuljettaa
tekstin mukanaan; tyhjennys on yhä sidottu vain jonon seuraavaan kohteeseen ja onnistuneeseen
lähetykseen. Juuri saapuneen viestin alle kenttä aukeaa itsestään kuten ennenkin.

Samassa muutoksessa kolme muuta Tommin pyyntöä:

- **Pelaajasuodatin alasvetovalikossa, valinta muistetaan** (`MessageFilterStore`,
  SharedPreferences samaan tapaan kuin `BoardStyleStore`). Suodatus on esitystä: koko
  arkisto pysyy mallissa ja `Export` vie aina kaiken, koska rajattu vienti olisi hiljaa
  vajaa varmuuskopio. Säilötty nimi joka ei enää osu yhteenkään riviin näyttää tyhjän
  rajauksen ja oman tekstinsä (`messages_filter_empty`), joka neuvoo takaisin.
- **Ruudun levyinen viiva vain viestiketjujen väliin.** Ketju on viesti vastauksineen
  (ryhmäavain `replyTo ?: id`), ketjun sisällä viivaa ei ole, ja vastaus asettuu kohteensa
  alle vaikka väliin olisi saapunut muuta. `In reply to:` -rivi piirtyy enää vain kun kohde
  ei ole ruudulla, koska ketjussa lainaus suoraan kohteen alla olisi sama sisältö kahdesti.
- **Tunnuksille omat värit, sisältö pysyy vaaleana tummalla.** Väri on lähettäjän nimessä
  eikä rungossa, ja sävyt ovat `DgTheme.accentFor`in viisi mitattua paria (AA-kontrasti
  molemmissa teemoissa on mitattu siellä). Uusia sävyjä ei keksitty; väri on vahvistus eikä
  tunniste, kuten välilehdillä, koska kuudes pelaaja jakaa väistämättä sävyn. Johdos on
  nimen `hashCode`, joten sama nimi saa saman värin ilman säilöä.

Manuaali sai klikkauksen ja suodattimen (`help_messages_body`), koska näkymätön ele on
kirjoitettava johonkin. Vientimuotoon lomaketta **ei** lisätty: vienti on sisällön
varmuuskopio, ja lomake on kyky eikä sisältöä.

**Kaksi jatkopyyntöä samana iltana, heti todennuksen jälkeen.**

*Lainaus valintaruudulla* (*"jotta vastaanottaja näkee mihin vastattiin"*). Rasti liittää
`> `-etuliitetyt rivit lähtevän viestin alkuun DGText2Arean mitatussa muodossa
(`docs/TOINEN-ASIAKAS.md`), ja perillemeno on mitattu jo 24.8.2026 (`&gt;` entiteettinä,
LF raakana `<PRE>`:ssä). Kolme muotopäätöstä: **esikatselu eikä esitäyttö** (kenttä pysyy
pelkkänä omana tekstinä, eikä rastin irrotus joudu arvailemaan käyttäjän muokkausten
seasta mitä poistaa), **oletus on pois päältä** (24.8. päätös *viesti ennallaan* jää
lähtötasoksi, eikä rasti nollaudu viestin vaihtuessa), ja **pelkkä lainaus ei lähde**
(portti on kirjoitetussa tekstissä, tyhjä luonnos on tyhjä lähetys). Laskuri laskee
lainauksen mukaan, koska luku koskee lähtevää viestiä eikä kenttää. Arkistoon menee sama
teksti joka lähti. `Message.replyTo` ei nojaa lainaukseen kumpaankaan suuntaan.

*Laji näkyviin rivillä*: nimen perässä lukee nyt `Quick message` / `From a game` -selite
(`labelSmall`, `onSurfaceVariant`), koska saapumisreitti ei muuten näy listalta.
Lähettäjättömällä rivillä laji on entuudestaan nimen paikalla, eikä sitä toisteta.

*Lajimerkintä on linkki otteluun* (*"pelin tieto voisi olla linkkinä, niin kontekstia"*,
laajennettuna samana iltana: *"myös päättyneeseen otteluun pitäisi saada linkki"*).
Aktiivinen ottelu avautuu lautana **otteluluettelon omasta linkistä** (`BoardRoute.of`),
muu ottelu siirtohistoriana porautumisruudussa. Ensimmäinen muoto ehti elää tunteja:
linkki vain listalla oleville otteluille, mikä olisi jättänyt juuri päättyneet ottelut
linkittömiksi.

Siirtohistoria vaati kaksi asiaa, ja molemmat on kirjattu omiin paikkoihinsa:

- **26.8. rajaus purettiin** (`docs/AVOIMET.md`): Review-sivu `/bg/game/<id>/<n>/list` sai
  lukijan (`MatchLogParser`, raakasivu odotti 1.8. alkaen) ja porautumisruutu
  `MatchLog`-sisällön. Ruutu näyttää sivun omat sanat (`61: 13/7 8/7`, `Doubles => 2`)
  kahtena sarakkeena eikä tulkitse siirtoja; asemalinkkejä ei avata.
- **Listalta pudonneen ottelun Review-polku kootaan tunnisteesta**, ja se on nimetty
  poikkeus sääntöön *osoite luetaan, ei koota* (perustelu `MainActivity`n kytkennässä):
  muoto on mitattu neljästä lähteestä, haku on kuluttamaton GET, ja väärä osoite tuottaa
  porautumisruudun oman "ei tunnettu sivu" -tilan eikä tekoa. Listalla olevan ottelun
  polut luetaan yhä luettelosta.

Terminologia samalla korjattuna: `From a game` → **`From a match`** (Tommin huomio:
backgammonissa ottelu koostuu peleistä, ja viesti kuuluu otteluun). Resurssiavain seuraa
yhä enumia `GAME_MESSAGE`, joka on kannassa eikä liiku.

**Siirtohistorialinkki todennettu laitteella samana iltana aidolla sivulla:** päättyneen
ystävyysottelun `From a match` avasi Review-sivun porautumisruudussa kootulla polulla, ja
kaikki kolme jäsentimen ansaa näkyivät oikein oikeassa datassa (tanssi paljaana heittona,
pistetilanteet pelien alussa, kaksi saraketta kohdakkain). Otsikoksi jäi varateksti, koska
ystävyysottelun sivulla ei ole turnaus-`h3`:a; se on sivun oma puute eikä vika.

**Todennettu Galaxy Tab S7+:lla samana iltana, molemmista päistä.** Suodatin rajasi
keskustelun ja valinta säilyi force-stopin yli; ketjuviivat ja tunnusvärit (ackammon
sininen, tommih violetti) piirtyivät kaappauksiin; vanhan rivin napautus ei tehnyt mitään.
Lähetys ajettiin tilatulla viestillä kahden tunnuksen välillä: ackammonilta lähetetty
viesti otettiin jonosta sovelluksella, kenttä aukesi sen alle itsestään, napautus sulki ja
avasi kentän, ja fraasinapeilla koottu `hi gg ty gg u2` lähti talletetulla lomakkeella,
kuittautui (`Sent.`), ketjuuntui arkistossa kohteensa alle ja **luettiin vastaanottajan
selaimesta perille tulleena**. Lähetysnapin painoi Tommi, koska peruuttamaton teko on
käyttäjän ele.

#### Jonon polku on virta, ei kertakuva (korjattu 9.8.2026 samana iltana)

Ruutu luki polun otteluluettelon tilasta **kerran, avautuessaan**. Jos ruudun avasi ennen kuin
lista oli latautunut, polkua ei ollut vielä olemassa, ja ruutu sanoi ettei mitään odota vaikka
odottaisi. Vika on **hiljainen väärä nolla**: tulos on kelvollisen näköinen vastaus kysymykseen
"onko jotain odottamassa", ja se johtaa päättelemään ettei ole. Sama suunta kuin juuresta ajetun
haun nolla osumaa (`Kaanon/TYÖTAVAT.md`), eli virhe osuu aina suuntaan "tätä ei ole".

Sääntö korjauksen jälkeen: **kohde on aina tuorein sivulta luettu linkki.** Lähteitä on kaksi,
otteluluettelon ilmoituslinkki ja juuri haetun sivun `Next >>`, eikä kumpikaan ole koottu, joten
sääntö voi olla näin yksinkertainen: viimeksi saapunut voittaa. Haun jälkeen luettelon virta ei
emittoi uudelleen ellei arvo muutu, joten sivulta luettu jatkopolku säilyy.

Korjaus toi mukanaan toisen suunnan, jota kertakuvassa ei ollut: kun luettelo päivitetään eikä
ilmoitusta enää ole, polku muuttuu nulliksi ja nappi katoaa. Otteluluettelo on se joka tietää
onko jotain odottamassa, joten ruutu seuraa sitä eikä muista omaa käsitystään.

**Löytyi kirjoittamalla, ei ajamalla.** Laitteella tätä ei voinut nähdä, koska jonossa ei ollut
mitään: ilman ilmoitusta molemmat versiot sanovat saman. Kaksi testiä väittää kumpaakin suuntaa.

**Ehto täyttyi samana iltana**, ja testit eivät ole enää ainoa näyttö: jonossa oli kohde, ja
korjauksen toinen suunta nähtiin laitteella eli nappi katosi kun ilmoitus katosi luettelosta.
Ks. seuraava alaosio.

**Kortin teksti lyheni ensimmäisessä katselmuksessa** (Tommi 9.8.2026, kortti oli liian pitkä).
Jäljelle jäi kaksi lausetta, ja kummallakaan ei ollut vaihtoehtoa: varoitus siitä että haku
kuluttaa kohteen, ja lause siitä että otettu kohde päätyy tämän laitteen arkistoon. Jälkimmäinen
on kanonin vaatimus napin omalle tekstille (`Kenelle`-osio), ei kirjoittajan valinta.

Poistetut kaksi lausetta olivat lupauksia eivätkä varoituksia: tallennus ennen näyttöä ja yksi
kohde per painallus. **Manuaalia ei tarvinnut muokata, koska molemmat olivat siellä jo**, ja
ensimmäinen myös otteluluettelon odotusilmoituksessa. Se on ero joka kannattaa panna merkille:
siirto tarkoitti tässä pelkkää poistoa, koska kohde oli kirjoitettu kahteen paikkaan alun perin.

Jäljelle jäänyt jako on lause kerrallaan luettavissa. Kortti sanoo sen mitä ei voi perua,
manuaali sen mitä sovellus tekee puolestasi.

**Napin ehto, joka oli kirjattuna jo ennen kuin nappi oli mahdollinen.** `WaitingNotice`
sanoi 8.8.2026 alkaen ettei siinä ole avausnappia koska jäsennintä ei ole ja kohde katoaisi
tallentumatta. Ehto täyttyi tässä sessiossa, ja se on ainoa syy miksi nappi lisättiin. Tämä
on vastakohta sille vikamuodolle jonka `Odottaa Tommia` -osio nimeää: siellä kielto jäi voimaan
kun sen perustelu ratkesi muualla, tässä kielto purkautui samassa muutoksessa joka sen perusteen
poisti.

#### Putki ajettiin aidolla jonolla, ja nappi osoittautui tiedottomaksi (9.8.2026 illalla)

**Ensimmäinen kerta kun sovellus teki peruuttamattoman teon oikeaa sivustoa vasten.** Tommilla
oli jonossa yksi kohde, se otettiin sovelluksella, ja se oli turnausvoittoilmoitus toisesta
pelaajasta. Ehto joka tähän tarvittiin oli kirjattu edellisessä alaosiossa: laitteella tätä ei
voinut nähdä niin kauan kuin jono oli tyhjä, koska ilman ilmoitusta korjattu ja korjaamaton
versio sanovat saman.

Neljä asiaa todentui samalla painalluksella, ja ne olivat aiemmin eri näytön varassa:

| Mitä | Näyttö ennen | Näyttö nyt |
|---|---|---|
| `<PRE>`-lohko luetaan loppuun asti | fixture `inbox_telegram.html` | aito sivu, jossa linkit ovat keskellä virkettä |
| Lajittelu `ANNOUNCEMENT`iksi | yksikkötesti | arkistorivi sanoo `DailyGammon announcement` |
| Kirjoitus ennen näyttöä | `MessagesViewModelTest` | reunapäivä korvasi tilan `No messages yet` |
| Polun katoaminen nulliksi | yksi yksikkötesti | ilmoitus katosi luettelosta ja nappi sen mukana |

**Kaksi riippumatonta lähdettä sanoi lopputuloksesta samaa:** sovelluksen luettelosta ilmoitus
katosi, ja sivusto lakkasi näyttämästä `You have Messages!`. Jälkimmäistä ei haettu sovelluksella
vaan Tommi katsoi sen selaimesta, eli se on kohteen oma kanta eikä meidän jäsennyksemme.

**Löytö: `Take next item` ei kerro odottaako mitään.** Haun jälkeen kohde asetetaan sivun omasta
`Next >>` -linkistä, ja telegram-sivun `Next >>` osoittaa `/bg/nextgame`, eli **samaan
osoitteeseen kuin ennen hakua**. `item.nextPath ?: requestedPath` ei siis ole varakaatuminen
vaan sama arvo kahta reittiä. Sivu ei koskaan kerro olleensa viimeinen, joten nappi häviää vain
toisesta suunnasta: kun otteluluettelo päivitetään eikä ilmoitusta enää ole.

Muoto on tuttu ja käännetty: 9.8. iltapäivällä korjattu vika oli **hiljainen väärä nolla**, tämä
on **hiljainen väärä kyllä**. Nappi paikallaan tarkoittaa "polku on tiedossa" eikä "jotain
odottaa". Kumpaakaan ei voi lukea ruudulta, ja ainoa lähde joka tietää jonon tilan on
otteluluettelo.

Tätä ei korjattu tässä sessiossa, eikä syy ole kiire. **Korjaus vaatii tiedon jota sivu ei anna**,
ja ainoa tuntemamme tapa saada se on hakea seuraava kohde, eli kuluttaa jono sen selvittämiseksi
onko jonossa mitään. Se on juuri se teko jota vastaan koko ruutu on rakennettu. Kohta on siksi
avoimissa päätöksissä eikä työlistalla.

**Ratkaistu 13.8.2026: teksti lakkaa lupaamasta, eikä ruutu ala kysyä** (Tommin päätös).
Vaihtoehto oli pyytää otteluluettelo uudelleen haun jälkeen, jolloin nappi saisi ehtonsa
sieltä; se hylättiin, koska se tekisi yhdestä painalluksesta kaksi pyyntöä juuri siinä
ruudussa jossa jokainen pyyntö on tähän asti ollut suoraan käyttäjän ele.

**Ratkaisu on kapeampi kuin kysymys, ja se on tarkoitus.** Käyttäjä ei saa tietää mitä
jonossa on, koska sitä ei voi tietää kuluttamatta jonoa. Hän saa tietää mitä nappi tekee ja
mistä sen olemassaolo kertoo: ilmoitus on peräisin otteluluettelosta siltä hetkeltä jolloin
luettelo viimeksi haettiin, eikä haku päivitä sitä. Painallus on siis kysymys eikä vastaus.

**Kaksi tekstiä eikä yksi**, koska ne luetaan eri hetkellä. Kortin selitys luetaan ennen
ensimmäistä painallusta, ja tuloslause on se ainoa rivi jonka käyttäjä varmasti lukee haun
jälkeen. Jälkimmäinen on juuri se hetki jossa aukko oli, joten pelkkä selityksen korjaus
olisi jättänyt korjauksen väärään kohtaan sivua.

Mitä tämä **ei** ratkaise, jottei kohtaa lueta laajempana: sitä ei ole mitattu mitä sivusto
palauttaa tyhjästä jonosta, koska mittaus vaatisi jonon tyhjentämisen. Jos vastaus on jotain
jota jäsennin ei tunnista, käyttäjä näkee `messages_queue_unreadable`n eikä sanaa tyhjästä.
Se on tiedossa oleva aukko eikä yllätys, ja se sulkeutuu vasta jos joskus on syytä hakea
tyhjästä jonosta muutenkin.

**Aukko suljettu tunnistuksen puolelta 18.9.2026** (Tommin havainto: *"Take an item on
harhaanjohtava, jos dailygammonin viestijono on tyhjä"*, ja valinta koodin ja kaanonin
välillä oli koodi). Päätös 13.8. pysyy: luetteloa ei haeta uudelleen haun jälkeen. Sen
sijaan `readItem` tunnistaa otteluluettelon vastauksena (`DgPages.isTopPage`, sama
predikaatti kuin laudan `Next Game` -paluulla) omaksi tilakseen `QueueUiState.Empty`.
Napin alle tulee *The queue was empty: DailyGammon answered with your match list instead
of an item, so nothing was used up and nothing was saved. The button is gone until the
match list shows the notice again.*, ja polku pudotetaan nulliksi, jolloin nappi katoaa
ja *Nothing is waiting* tulee sen tilalle. Luettelon `Refresh` palauttaa napin jos linkki
on taas siellä, koska `queuePathUpstream` on `map` tilavirrasta ja emittoi jokaisesta
uudesta `Loaded`-tilasta myös saman polun. Ilman tätä Top Page putosi
`messages_queue_unreadable`en, joka käski tarkistamaan sivuston. Yksikkötesti `tyhja jono
vie napin eika tallenna mitaan`. Oletus jota koodi ei todista: että tyhjä `/bg/nextgame`
palauttaa Top Pagen, kuten laudan `Next Game` tekee (`raakasivut/LUEMINUT.md`, 8.9. ja
15.9.). Se todentuu seuraavassa pelisessiossa yhdellä painalluksella kun luettelo sanoo
ettei mitään odota, proxy päällä; haku tyhjästä jonosta ei kuluta.

**Ilmoituksen otsikkoriviä ei näytetä arkistossa** (Tommin päätös 9.8.2026, kysyttynä samana
iltana). `Message.rawHeader` on tallessa kannassa, joten päätös koskee vain esitystä ja on
peruttavissa ilman että mitään on menetetty. Laji ja teksti riittävät, koska otsikko on
sivuston oma rakenne eikä viestin sisältöä, ja lajitellun viestin kohdalla se toistaa saman
tiedon jonka lajirivi jo sanoo.

#### Lautasivulla on `<h3>`, ja päinvastainen oletus eli testin kommentissa (mitattu 9.8.2026)

`InboxParser` kieltäytyy nyt lautasivusta ennen kuin lukee mitään (`DgPages.isBoardPage`).
Ilman porttia jonosta noudettu lauta olisi jäsentynyt viestiksi, koska otsikko löytyi:
`move_board.html`in `<h3>` on `Nine Lives #2222, Round 4`. Tulos olisi ollut viesti jonka laji
on UNKNOWN ja runko tyhjä, ja `chat_thread.html` olisi antanut sille rungonkin, koska sillä on
sekä otsikko että `<pre>`.

Vika ei olisi kaatunut vaan kirjoittanut valeviestin arkistoon, eli osunut suoraan siihen
yhteen asiaan jota varten sovellus on olemassa.

Testi `lautasivua ei luulla viestiksi` oli ollut olemassa koko ajan, mutta se ajoi sivulla
`<p>Ei otsikkoa</p>` ja sen kommentti sanoi ettei lautasivulla ole `h3`-otsikkoa. Se ei siis
koskaan koskettanut väittämäänsä tapausta: **koettimen työkalumaasto** (`KÄSITTEISTÖ.md` §0.2),
eli komento joka ei koskenut kohdettaan antoi saman vihreän kuin komento joka koski ja löysi
kaiken kunnossa. Testi ajaa nyt aidoilla fixtureilla, ja chat-sivu on omana tapauksenaan.

#### Vientinappi (14.8.2026)

`MessagesScreen`in `TopAppBar`issa on `Export`-nappi kun arkisto ei ole tyhjä, muuten se
puuttuu kokonaan (sama muoto kuin jonon hakunapilla: toimintoa jota ei voi tehdä mielekkäästi
ei näytetä toimimattomana). Painallus avaa käyttöjärjestelmän oman tallennusvalitsimen
(`ActivityResultContracts.CreateDocument`), ehdotettu tiedostonimi
`dg-archive-YYYY-MM-DD.json` (`ArchiveExportFilename`).

**Kirjoitus on `MainActivity`ssä eikä `MessagesViewModel`issä.** Malli antaa vain
JSON-merkkijonon (`exportJson`, kutsuu `core-domain`in `List<Message>.toArchiveJson`ia); vasta
composable-taso saa `Uri`n käyttäjän valinnasta ja kirjoittaa siihen `ContentResolver`illa.
Sama raja kuin muualla sovelluksessa: näkymämallit eivät tunne Androidia laajemmin kuin
niiden oma rajapinta vaatii.

**Muoto on oma JSON eikä kannan raakakopio** (Tommin valinta kahdesta, 14.8.2026). Raakakopio
(`.db` + `-wal` + `-shm`) olisi ollut halvempi mutta lukukelvoton ilman toista DG Android
-asennusta; oma muoto maksaa skeeman mutta on ihmisluettava ja tuotavissa takaisin rivi
kerrallaan, jos tuonti joskus tehdään.

**Vientiä ei ole sidottu `Message.id`in tiivisteeseen.** Vienti on kopio arkistosta eikä osa
sitä, joten sen oma versio (`ARCHIVE_EXPORT_VERSION`,
`core-domain/.../MessageArchiveExport.kt`) saa muuttua ilman että kannassa jo olevien viestien
tunnisteet liikkuvat. Sama riski joka koskee tunnisteen kenttiä ei siis koske tätä. Rakenne on
käsin kirjoitettu eikä kirjastolla, koska `core-domain` ei saa riippua mistään.

**Tyhjä-tila on todennettu laitteella (Galaxy Tab S7+ 14.8.2026), nappia kantava haara ei:**
sen todentaminen kuluttaisi jonosta oikean kohteen. Haara on rakenteellisesti sama koodipolku
ja katettu yksikkötestein (`MessageArchiveExportTest`, `ArchiveExportFilenameTest`).

#### Tuontinappi (16.9.2026)

Viennin peilikuva, Tommin tilaus 16.9.2026 yöllä (`docs/AVOIMET.md` › Arkiston tuonti):
*"laajenna muistutuksiin ja fraaseihin, tuonti vain lisää"*. `Import` on `TopAppBar`issa
`Export`in vieressä ja **aina näkyvissä**, toisin kuin `Export`: tyhjä arkisto on juuri se
tilanne jossa tuontia tarvitaan, eli uusi laite. Painallus avaa `OpenDocument`-valitsimen
tyypillä `*/*`, koska tiedostonhallinta nimeää `.json`in vaihtelevasti ja tiukka suodatin
piilottaisi oman tiedoston ilman virhettä; muodon tunnistaa malli (`readArchiveJson`, `format`-
kenttä).

**Lukeminen on `MainActivity`ssä ja malli saa tekstin** (`importJson`), sama raja kuin
viennissä. Tulos näytetään rivinä varmuuskopiorivin alla kuittaukseen asti (`ImportLine`,
`ImportUiState`): rivi eikä dialogi, koska tulos on tieto eikä kysymys. Rivi sanoo luvut
sanan jäljessä (*messages 0, reminders 0, phrases 1*), koska *1 messages* näkyi ensimmäisessä
laiteajossa eikä kolmen luvun englannin monikkoa kannata ratkaista kolmella plurals-
resurssilla.

**Tiedosto laajeni versioon 4 samalla**: `reminders`, `phrases` ja viestin `replyTo`. Muoto
laajenee kerran eikä kahdesti, koska laitteen vaihto vie mukanaan kaiken mitä käyttäjä itse
kirjoitti. Vastauslomake jäi yhä pois 30.8.2026 päätöksellä (yllä: kyky eikä sisältöä), joten
tuotu viesti ei saa vastauskenttää uudella laitteella; merkityt asemat eivät ole tiedostossa,
ja manuaali sanoo sen. Lukija lukee versiot 1–4, koska jokainen on edellisen ylijoukko.

**Tuonti vain lisää eikä koskaan korvaa** (Tommin päätös). Viestin samuuden ratkaisee
sisältötiiviste, joka lasketaan kentistä uudestaan eikä lueta tiedoston `id`-kentästä;
muistutuksella samuus on kaikki viisi kenttää (`ReminderDao.countSame`), fraasilla
`PhraseBook.normalize`. Muistutuksen tuonti **ei siivoa** saman ottelun muita pelejä toisin
kuin käyttäjän oma kirjoitus, koska tuotu rivi ei ole todiste meneillään olevasta pelistä;
vanhentunut rivi jää näkymättömäksi kuten se olisi jäänyt ilman tuontiakin. Viesti kirjataan
tiedoston tilille jos se on nimetty, muuten kirjautuneelle, ja toisen tilin viesteistä rivi
sanoo etteivät ne näy ennen kuin se tili kirjautuu.

**Todennettu laitteella 16.9.2026 yöllä (SM-T970):** oma 15.9. klo 22.19 varmuuskopio (versio 2,
42 viestiä) takaisin antoi *"Nothing new in that file"*, eli jokainen kannan rivi tuotti saman
tiivisteen kuin tiedostosta luettu viesti; pelkän fraasin sisältävä testitiedosto antoi
*messages 0, reminders 0, phrases 1* ja fraasi ilmestyi listaan. Testit:
`MessageArchiveImportTest` (kääntäminen, versio 1, rikkinäinen rivi, ei-arkisto),
`ReminderBookTest` ja `MessageArchiveTest` (vain lisää oikeaa SQLiteä vasten),
`MessagesViewModelTest` (koko ketju, toinen tili, vanha tiedosto).

### Keskeytynyt lähetys näkyy otteluluettelossa (10.8.2026)

Jono on kannassa eikä verkossa, joten `TopScreen` lukee sen ja näyttää yhden kortin per
odottava teko. Rivi tulee `AppContainer.actions`ista suoraan eikä `TopViewModel`in kautta,
sama muoto ja sama perustelu kuin arkiston reunapäivällä: kyseessä on paikallinen tieto eikä
otteluluettelon tila, ja `TopViewModel` pysyy sinä luokkana joka vain hakee sivun.

**Kortti on tilan yläpuolella eikä listan sisällä**, eli se piirtyy myös `Loading`- ja
`Failed`-tilassa. Ehto on mitattu eikä esteettinen: yhteydettömänä käynnistetty sovellus jää
tähän ruutuun, koska otteluluettelo haetaan verkosta, joten listan sisällä kortti olisi poissa
täsmälleen siinä tilassa jota varten se lisättiin.

Kolme rajausta:

- **Ei uusinta- eikä hylkäysnappia.** Rivin ratkaisu nojaa lautasivuun, joten se kuuluu
  ruutuun jossa sivu on. Kaksi paikkaa joissa saman rivin voi poistaa olisi kaksi totuutta
  samasta teosta. (Uusintanappi poistui kokonaan 8.9.2026, ks. alempaa.)
- **Ei näy uloskirjautuneena.** Kortti veisi ruutuun joka vaatii istunnon. Jono ei katoa
  uloskirjautumisessa, joten kortti palaa kirjautumisen jälkeen.
- **Teksti kertoo mitä ei tiedetä**, sama sanamuoto kuin lautaruudun palkissa
  (`%1$s may not have gone through`). Kaksi eri sanaa samasta rivistä lukisi kuin kaksi eri
  tapahtumaa.

Reitti syntyy `BoardRoute.of(PendingAction)`illa tallennetusta polusta sellaisenaan.
`DestinationsTest` vaatii siltä saman reitin kuin ottelusta rakennetulta, koska kaksi eri
reittiä samaan lautaan tarkoittaisi että toinen niistä on koottu.

### Epäonnistunut teko säilyttää laudan, ja ilmoitus kantaa äänen (24.8.2026)

Teon epäonnistuminen ei enää pyyhi lautaa ruudulta. 24.8.2026 asti pyyhkiminen oli
tarkoituksellista, koska hiljaa säilynyt lauta olisi luettu todisteeksi teon onnistumisesta;
nyt säilyminen ei ole hiljaista. Lauta jää näkyviin tekoa **edeltävässä** tilassa, ja
`BoardUiState.Loaded.unconfirmed` merkitsee sen vahvistamattomaksi. Kolme seurausta:

- **Keskikaistan tilarivi kertoo pelaajalle tilanteen** (Tommin valinta 26.9.2026,
  `sessio-26-9-ilta2`, mockupin vaihtoehto A ilman viimeistä virkettä). Rivi on
  *`<syy>`. Nothing was confirmed.*, esimerkiksi *Site error 502. Nothing was confirmed.*, ja se
  on tilarivien ensimmäinen samassa lokerossa kuin *Please make a checker move* (`stripNotes`).
  Ahtaalla laudalla se on nappien alla kuten muutkin tilarivit. Rivi on näkyvissä niin kauan
  kuin lauta on vahvistamaton. 26.9.2026 asti tilanteen kertoi `UnconfirmedDialog`, joka
  piti sulkea ennen `Refresh`iä, ja Tommi pyysi viestin sinne minne muutkin viestit menevät.
- **Vahvistamaton lauta on näyttö eikä pelipinta.** `follow` ja `press` eivät ota vastaan
  mitään ennen uutta hakua, koska vanhentuneen laudan linkeistä lähetetty teko olisi sama
  sokea toisto jonka takia odottava rivi ratkaistaan haetusta sivusta. Veto alaspäin (Refresh) toimii
  ja tuore lauta avaa ruudun taas.
- **Katkotilassa keskikaistan keskellä on `Refresh`-nappi (Tommin tilaus 7.9.2026).** *"Paluun
  ja alaspäin swaippauksen sijaan kannatan Refresh-nappia keskellä ruutua."* Luenta kuitattiin
  monivalinnasta: nappi katkotilaan ja veto jää sinnekin. Peruste on että katko on ainoa
  hetki jolloin pelaaja ei tiedä mitä tehdä, ja ele ei näy ruudulla. Muualla 9.8.2026 päätös
  (veto napin tilalle) pysyy, joten pelattava lauta on yhä vapaa päällepiirroista. Nappi ja
  veto hakevat saman sivun kerran, ja nappi katoaa kun tuore lauta on saatu. **Paikka tarkentui kuvan jälkeen samana iltana:** *"Refresh-nappi
  keskelle keskipaneeli tarvittaessa omistamattoman tuplauskuution ylle."* Ensimmäinen muoto
  oli koko laudan keskipiste, joka osui kaistalle mutta noppien viereen. Nyt nappi piirtyy
  `MiddleStrip`in viimeisenä sen keskelle eli muurin kohtaan, omistamattoman kuution päälle
  kun se on siinä. Peittäminen on hinnatonta, koska lauta on siinä tilassa näyttö eikä
  pelipinta eikä kuutiota voi klikata. Toteutus `UnconfirmedRefresh` (`BoardScreen.kt`),
  parametri kulkee `LoadedBoard` → `Board` → `MiddleStrip`.
- **Nappi suureni ja sai viereensä syyn (Tommin tilaus 8.9.2026).** *"Keskellä näyttöä oleva
  Refresh-nappi voisi olla isompi"* ja *"keskipaneeliin sopisi viereen tieto huonosta
  yhteydestä"*. Napin mitat ovat nyt omat eivätkä Material3:n oletus, koska oletus mitoitetaan
  lomakkeeseen ja tämä on ainoa reitti eteenpäin umpikujassa. Vieressä luki `No connection`
  tai `Site error <koodi>` 26.9.2026 asti; nyt syy on tilarivillä (ylempi kohta), ja nappi on
  keskellä yksin.
- **Verkon paluu hakee lautasivun kerran**, samalla ehdolla kuin haun epäonnistumisessa.
  Ajastettua pollausta ei ole eikä tule ilman eri päätöstä (Tommi 24.8.2026): pelaajalle
  kerrotaan tilanne, palvelinta ei kuormiteta, eikä tekoa koskaan toisteta itsestään.

Päätöksen kulku ja perustelut: `docs/AVOIMET.md`, kohta "Epäonnistunut lähetys hävittää
ladatun laudan".

### `Try again` poistui, haku peri sen työn (Tommin päätös 8.9.2026)

Odottavan teon palkissa oli kaksi nappia, `Try again` ja `Discard`. Ensimmäinen teki kaksi
asiaa yhdessä eleessä: haki lautasivun tuoreena ja lähetti sitten uudestaan, jos sivu yhä
tarjosi napin. Tommin sanat: *"Try again -nappi tarjoava näyttö on tarpeeton"*, ja luenta
kuitattiin monivalinnasta *"Refresh korvaa sen"*.

Työn teki se ensimmäinen puolisko, ja se tapahtuu joka tapauksessa jokaisella haulla.
**Onnistunut laudan haku tyhjentää nyt rivin lähettämättä**, kun sivu ei enää tarjoa tekoa
(`BoardViewModel.resolvePending`). Jälkimmäistä puoliskoa ei tarvittu: jos sivu yhä tarjoaa
napin, nappi on laudalla ja pelaaja painaa sitä itse. Kaksi reittiä samaan paikkaan, ja
katkotilassa uusintanappi oli niistä huomiota vievempi.

**Mitään ei lähetetä**, joten 24.8.2026 rajaus pysyy sellaisenaan voimassa: haku hakee sivun,
ei tee sivustolla mitään. Kolme ehtoa sulkevat väärän tyhjennyksen. Vain oikeasti saatu sivu
kelpaa todisteeksi, koska epäonnistunut haku jättää vanhan laudan ruudulle. Vain sama lauta
kelpaa, koska saman ottelun toinen peli ei tarjoa nappia eikä ole todiste mistään. Ja
kokoamisesta riippuvaa tekoa ei tyhjennetä lainkaan, koska haku itse nollaa kokoamisen
palvelimella (mitattu 10.8.2026) eikä tyhjentynyt kokoamistila kerro teon kohtalosta mitään.
Sellainen rivi odotti `Discard`ia 16.9.2026 asti.

Palkki jäi, eli lauta kertoo yhä mitä ei tiedetä. Poisto näkyy edelleen
(`board_pending_gone`), koska rivi joka katoaa äänettömästi on erottamaton rivistä jota ei
koskaan ollut.

### `Discard` poistui, tuore lauta ratkaisee kootunkin siirron (Tommin tilaus 16.9.2026)

Palkki laukesi ensimmäisen kerran oikeassa pelissä `sessio-16-9-ilta3`ssa: `Submit Move`
lähti, vastaus ei tullut 30 sekunnissa, ja sivusto oli silti ottanut siirron vastaan. Tommi
painoi Refreshiä (lauta näytti siirron menneen), sitten `Discard`ia, ja sanoi *"punainen
yläpalkki heräsi henkiin, Refresh riittää"*; tarkennus kysyttäessä: *"Discard-nappi on turha,
palkkiin jää vain Refresh-neuvo"*.

Nappi lähti, ja sen mukana lähti yllä olevan jakson poikkeus: **kootun siirron rivi poistuu
nyt samalla tuoreella haulla kuin muutkin**, ilman `board_pending_gone`-ilmoitusta, koska
lauta itse on ilmoitus (siirto joko näkyy siinä tai nopat odottavat yhä). Kaksi muuta ehtoa
pysyvät: vain oikeasti saatu sivu ja vain sama lauta. Edellisestä ajosta jäänyt rivi näkyy
yhä otteluluettelossa (*"Open the match"*) siihen asti että ottelu avataan, ja avaus on se
haku joka sen ratkaisee. `BoardViewModel.discardPending` poistui; sovellus ei tee sivustolla
mitään tässäkään. Todentaminen: `BoardViewModelTest`, *kootun siirron rivin paivitys
tyhjentaa*.

### Kännykän näytön työlista (8.8.2026)

Lautaruudun ulkoasu tehtiin ensin tabletille ja korjattiin puhelimelle samana päivänä. Alla
on se mitä puhelimen näytöltä on mitattu muttei tehty. Lista on tässä eikä avoimissa
päätöksissä, koska nämä eivät ole päätöksiä vaan tekemätöntä työtä: suunta on selvä ja
mitat mitattu, vain toteutus puuttuu.

**Päätetty ja lykätty (Tommi 8.8.2026): otsikkorivin poisto.** **Tehty 9.8.2026, ks. edellinen
osio**, joka kertoo myös mitkä kaksi tämän kohdan lukua osoittautuivat vääriksi. Kolme
alakohtaa alla ovat siis suunnitelma sellaisena kuin se kirjoitettiin, eivät nykytila.

Lautaruudun oma otsikkorivi on 32 dp ja kantaa `Back`in, turnauksen nimen, kierroksen ja
siirtonumeron. Suunnitelma on kolmiosainen ja se ratkaisee kolme asiaa yhdellä muutoksella:

- Laudan kehys kutistuu sisältönsä levyiseksi. Nyt `Board` saa `weight(1f)` eli koko
  jäljelle jäävän leveyden, ja kun lauta kapeni 1:3,55-suhteeseen, molemmin puolin jäi
  **noin 65 dp pelkkää kehysväriä** jolle ei ole käyttöä. Lauta ei voi käyttää sitä, koska
  nappulan koko tulee korkeudesta.
- Vapautuva noin 130 dp:n sarake tulee laudan ja sivupaneelin väliin, ja `Back` ja `Refresh`
  siirtyvät sinne **48 dp korkeina**, eli Materialin minimikosketusalueeseen. Nyt ne ovat
  32 dp, eli alle mitan. Sama sijoittelu on mitattu DG Mobilesta: sen napit ovat laudan
  vieressä oikeassa reunassa, noin 117 x 40 dp.
- Otsikkorivi poistuu kokonaan ja sen teksti menee sivupaneelin yläreunaan, jolloin
  **lauta saa 32 dp lisää korkeutta** ja nappula kasvaa noin 26,8:sta noin 30 dp:hen. Se on
  käytännössä DG Mobilen 29,7 dp, ja sama syy: sillä ei ole otsikkoriviä lainkaan.

Tiedossa oleva hinta, jotta se ei tule yllätyksenä: otsikko siirtyy 200 dp leveään
sivupaneeliin, joten pitkä turnauksen nimi katkeaa aiemmin kuin nyt.

**Päätetty 10.8.2026 (Tommi): korkeus on hyvä, eikä loppua budjetista oteta työhön.**
Alla oleva taulukko jää tähän mittauksena, ei ehdotuksena. Ero on olennainen seuraavalle
lukijalle: luvut ovat yhä oikeat ja käyttökelpoiset jos joku joskus palaa asiaan, mutta
niitä ei saa esittää tekemättömänä työnä. Ehdotus joka on kerran hylätty ja jää listalle
ehdotukseksi, nousee uudelleen joka lukukerralla ilman että kukaan päätti sen nousevan.

Kaksi seurausta. Tämä oli näistä avoimista kohdista se ainoa jonka arvo oli kosmeettinen,
joten sen sulkeminen ei jätä mitään riippumaan siitä. Ja alla oleva ehtokappale
nappulan katosta menettää käyttökohteensa: se kertoo nyt miksi 8.8. tehty ratkaisu oli
oikea, ei mitä sen varassa vielä kannattaisi tehdä.

**Mitattu muttei tehty: loput korkeusbudjetista.** Nämä esitettiin 8.8.2026 illalla eikä
niitä otettu työhön. Luvut ovat aritmetiikkaa Pixel 8a:n 411 dp:n
ikkunasta, ja numerorivin osuus on arvio `labelSmall`in rivikorkeudesta.

| Kohde | Nyt | Voisi olla | Säästö |
|---|---|---|---|
| ~~Keskitila (kuutio, nopat, pip-vahti)~~ | ~~59 dp~~ | **tehty 10.8.2026: 39,3 dp** | **19,7 dp** |
| Numerorivit ylhäällä ja alhaalla | ~40 dp | ~28 dp, ilman pystypehmustetta | 12 dp |
| Kehyksen pystypehmuste | 12 dp | 6 dp | 6 dp |

Keskitilan johtaminen on näistä hankalin: nopan koko riippuu nappulasta, joka riippuu
kiilasta, joka riippuu keskitilasta. Se ratkeaa laskemalla kierros kahdesti, mutta se ei ole
yhden rivin muutos.

**Ensimmäinen rivi tehtiin 10.8.2026, ja kappale sen yllä oli väärässä kahdesta kohdasta.**
Kehä ratkeaa yhdellä kierroksella eikä kahdella, ja muutos oli käytännössä yhden rivin
kaava; ks. `checkerFromHeight` ja sen oma osio ylempänä. Ja säästö oli suurempi kuin
arvioitu 15 dp, koska keskitila mitoitettiin kuution eikä nopan mukaan: 59 → 39,3 dp.

Tärkeämpi korjaus koskee kuitenkin syytä. Rivi oli tällä listalla **korkeuden säästönä**,
ja se on väärä peruste: sillä perusteella se olisi yhä tekemättä, koska korkeudesta
päätettiin yllä ettei sitä oteta työhön. Se tehtiin koska kiinteä keskitila **litisti
kuution suorakaiteeksi**, mikä on oikeellisuusvika eikä optimointi. Korkeus tuli kaupan
päälle. Kahdella jäljellä olevalla rivillä ei ole vastaavaa toista syytä tiedossa, joten ne
jäävät sinne minne edellinen kappale ne jätti.

**Ehto joka tekee näistä kannattavia.** Jokainen tässä säästetty pystypikseli muuttuu
nappulan kooksi vain siksi että nappulan absoluuttinen katto poistettiin 8.8.2026. Sitä
ennen katto oli 25 dp ja kaikki säästö olisi mennyt hukkaan. Jos katto joskus palautetaan,
tämä lista lakkaa maksamasta itseään takaisin.


### Muistutus itselle laudan alla (23.8.2026)

Pelaajan oma muistiinpano yhden pelin ajaksi, `ReminderStrip` ja `ReminderRow`
(`app/ui/BoardScreen.kt`). Tommin pyyntö 23.8.2026 kuvakaappauksesta, ja päätökset samalta
päivältä: muistutus elää pelin mitalta ja pikanappi tulee mukaan.

**Tämä ei ole sivuston ominaisuus, ja se on koko osion lähtökohta.** Selaimessa näkyvä
`Reminder`-lohko tulee laajennuksesta `DGText2Area` eikä DailyGammonilta (`docs/KOHDE.md`).
Sivustolla ei siis ole päätä johon tämä kytkettäisiin: rivi kirjoitetaan Roomiin, mitään ei
lähetetä, jono ei kulu ja koko ominaisuus toimii katkossa. Se on tämän sovelluksen ainoa
kohta jossa käyttäjän painallus ei voi epäonnistua verkkoon.

**Pelin tunniste on ottelutunnus ja pistepari** (`fi.tommi.dg.domain.GameKey`). Sivustolla ei
ole pelin tunnistetta, vain ottelun, ja polun tilatunniste muuttuu joka siirrolla: toinen on
liian karkea ja toinen liian hieno. Pisteet muuttuvat vain pelin päättyessä ja kasvavat
monotonisesti, joten pari on ottelun sisällä ainutkertainen. Puuttuva piste tarkoittaa ettei
tunnistetta ole, jolloin kenttää ei tarjota lainkaan; rahapeli on tämän tunnettu tapaus.

**Elinkaari on kyselyssä eikä ajastuksessa.** Muistutukset haetaan aina yhdellä
pisteparilla, joten pelin vaihtuessa edelliset lakkaavat näkymästä ilman että mitään
poistetaan. Rivit poistuvat kannasta vasta kun käyttäjä kirjoittaa saman ottelun uuteen
peliin, eikä koskaan sivun latauksesta: automaattinen poisto jäsennetyn pisteluvun nojalla
hävittäisi rivit hiljaa jos yksi luku joskus luetaan väärin, ja se on tämän projektin pahin
vikamuoto. Perustelu asuu `ReminderDao.deleteOtherGames`issa.

**Yksi koti kaikilla näytöillä**, sama päätös kuin toimintorivillä 14.8.2026. Sisältö asuu
laudan alla vierivässä sarakkeessa eikä sivupaneelissa, koska paneeli näkyy vain kun leveyttä
jää yli. Tommin pyyntö koski nimenomaan tablettia, mutta kaksi kotia olisi kaksi
ylläpidettävää leveysehtoa, ja yksi koti kattaa myös sen mitä pyydettiin. Ehto on leveys eikä
laite silloinkin kun se ei ole ehto lainkaan.

**Kirjoituskenttä on suljettuna oletuksena, ja syy on korkeus.** Lauta kutistuu korkeudesta,
joten jokainen pysyvästi näkyvä rivi laudan alla maksaa nappulan kokoa. Suljettuna tämä on
yksi nappirivi. Pikanappi näkyy silti suljettunakin, koska se on tavallisin tapaus
(`SUBSTANSSI.md` kohta 25) eikä sitä kannata piilottaa kentän taakse.

**Nappien sanat ovat omiamme, toisin kuin toimintonapeissa.** Siellä sanaa ei käännetä koska
se on sivun oma sana; tässä sivu ei tiedä koko asiasta mitään. Laajennuksen `Remind Cube
Doubling` korvautui parilla `Cube reminder` (nappi) ja `Think about doubling` (rivi joka
syntyy), eli napissa lukee mitä se lisää ja lisättävä rivi on lause itselle. Tommin valinta
23.8.2026.

**Rajaus jota ei saa ohittaa: muistutus ei kutsu pelaamaan** (`SUBSTANSSI.md` kohta 94).
Se näkyy kun pelaaja avaa laudan itse, eikä sovellus ilmoita siitä missään muualla. Hetken
valinta on ainoa laadun suoja, ja ilmoitus joka vetää ruudulle purkaisi sen. Tämä on kirjattu
sekä koodiin että tänne, koska "muistutus" on Androidilla juuri se sana joka johtaa
ilmoitukseen ilman että kukaan päättää sitä.

Todentaminen: `GameKeyTest` (tunnisteen ehto), `ReminderBookTest` (elinkaari ja siivous
oikeaa SQLiteä vasten), `BoardViewModelTest` (mille pelille rivi kirjattiin, ja ettei mitään
lähetetty).

### Vastauskenttä siirtyi viestin alle, ja se on lukusuunnan korjaus (24.8.2026)

Kenttä oli 22.8.2026 alkaen jonon kortissa ruudun ylälaidassa ja saatu viesti arkistolistassa
sen alapuolella. Lukujärjestys oli siis väärä: viesti luettiin alhaalta ja vastaamaan piti
vierittää takaisin ylös. Tommin havainto ensimmäisestä oikeasta käyttökerrasta.

Kenttä piirtyy nyt sen arkistorivin alle johon se vastaa. Vaihtoehto olisi ollut näyttää
viestin teksti kortissa ja kenttä sen alla, kuten tiedotteella tehdään, mutta silloin sama
teksti olisi samalla ruudulla kahdesti.

**Oikeus vastata ei muuttunut, vain paikka.** Ehto on yhä tyypissä eikä ruudulla: `ReplyForm`
kulkee `QueueUiState.Saved`in mukana eikä arkiston `Message`ssa, joten vanhaan viestiin ei voi
vastata silloinkaan kun ruutu kirjoitettaisiin väärin. Rivin tunnisteen vertailu valitsee vain
paikan.

### Fraasinapit vastauskentässä (24.8.2026)

Kolme nappia kentän alla, `hi`, `gg` ja `ty gg u2`, eli `SUBSTANSSI.md` kohdan 58
rituaalifraasit. Nappi **täyttää kentän eikä lähetä**: lähetys jää Send-napille, joten
peruuttamattomaan tekoon on yhä täsmälleen yksi reitti. Painallus liittää fraasin
kirjoitetun perään eikä korvaa, koska korvaava nappi hävittäisi tekstin yhdellä
kosketuksella, ja juuri se on DG Mobilen kirjattu vika (kohta 50).

Fraasit eivät ole `strings.xml`:ssä, koska ne ovat lähetettävän viestin sisältöä eivätkä
käännettävää UI-tekstiä: kohta 58 sanoo että rituaalifraasit ovat englantia riippumatta
siitä kuka pelaa. ~~Lista on kiinteä; tallentuvat suosikkifraasit ovat kirjattu idea
(`docs/AVOIMET.md`), ei tilaus.~~ **Lista on muokattava 3.9.2026 alkaen**, ks. alla.

#### Fraasin lisäys ja poisto (3.9.2026)

Tommin tilaus: *"toteuta fraasin lisääminen/poisto"*. Idea oli kirjattu 24.8.2026 avoimiin
(*tallentuvat suosikit*), ja tämä on se tilattuna. Kolme rakenteellista valintaa:

- **Uusi fraasi kirjoitetaan viestikenttään ja tallennetaan napilla** (`Save text as
  phrase`). Omaa lomaketta ei ole: fraasi on teksti jonka käyttäjä olisi valmis lähettämään
  sellaisenaan, joten sama kenttä on oikea paikka. Nappi on pois päältä kun kenttä on tyhjä tai
  sama teksti on jo napiksi tallennettu. Tallennus ei tyhjennä kenttää, koska teksti on yhä
  luonnos ja lähtee vain Send-napista. Rivinvaihdot litistyvät välilyönniksi ja reunavälit
  putoavat (`MessagesViewModel.normalizePhrase`), koska nappi on yhden rivin teksti.
  **Tallennettava fraasi pienennetään** (Tommin päätös 3.9.2026 laiteajon havaintoon, ks.
  alla); luonnos jää kenttään sellaisena kuin se kirjoitettiin.
- **Poisto on muokkaustila** (`Edit phrases` / `Done`). Tilassa jokainen nappi näyttää
  rastin ja poistaa itsensä eikä täytä kenttää. Tila on ruudun oma eikä säily, ja tyhjä lista
  sulkee sen itsestään. Kaksi tilaa siksi, että poistonappi jokaisen fraasin vieressä
  tekisi rivistä kaksi kertaa leveämmän ja toisi hävittävän teon samaan riviin täyttävän
  kanssa; kohdan 50 vika on juuri se että väärä kosketus vie tekstiä.
- **Tyhjä lista on tyhjä eikä paluu oletukseen.** `PhraseStore` erottaa *ei koskaan
  talletettu* (oletus `hi`, `gg`, `ty gg u2`) ja *talletettu tyhjänä*. Ilman eroa
  viimeisen fraasin poisto kumoutuisi seuraavassa käynnistyksessä, ja se olisi hiljainen
  vika samaa lajia kuin varmuuskopion unohtunut osoite (1.9.2026).

Lista säilyy laitteella (`SharedPrefsPhrases`, `docs/ASETUKSET.md` luku 3) eikä lähde
verkkoon. Rivi on `FlowRow`, koska lista voi nyt olla ruutua leveämpi. Profiilin
viestikentässä (`PageScreen`) fraasinappeja ei ole edelleenkään, perustelu ennallaan.

**Sama rivi laudan chat-korttiin (Tommin tilaus 3.9.2026, samana iltana).** Tilaus syntyi
kysymyksestä näkyvätkö fraasit muillekin kuin yhdelle pelaajalle, ja vastaus paljasti aukon:
viestiruudun vastauskenttä aukeaa vain pikaviestin alle, koska ottelun chatsivulla ei ole
lomaketta (`nextgame_chat.html`, nolla `sendmsg`-osumaa), ja ottelun chat kirjoitetaan
laudan chat-kortilla, jossa nappeja ei ollut. `hi` ja `gg` ovat ottelun alku- ja
loppufraaseja, eli juuri sen kortin tekstiä. Rivi on nyt yksi komponentti (`PhraseRow`)
kahdessa paikassa, ja lista on yksi (`PhraseBook`, sovelluksen omistama eikä kummankaan
näkymämallin): laudalla tallennettu fraasi on heti viestiruudussa ja päinvastoin. Kortilla
nappi täyttää kentän ja lähetys on yhä sivun oma nappi (`Next Game`, `To Top`), joten
24.8.2026 sääntö pätee sellaisenaan.

*Todennettu kuoriproxylla 3.9.2026* (airlock 503, top `sessio-2-9/0001`, lauta `0010` eli
siirron jälkeinen sivu chat-lomakkeineen): `Message` avasi kortin `Chat with Kaino`, kentän
alla oli rivi `hi gg ty gg u2 Save text as phrase Edit phrases`, ja `hi` täytti kentän.
Mitään ei lähetetty, kuoren loki näyttää vain kaksi Top Pagea ja yhden lautasivun.
Ensimmäinen yritys sivulla `0004` (vastustajan siirron `Next`-sivu) ei näyttänyt korttia
lainkaan, koska sillä sivulla ei ole chat-lomaketta; se on sivuston muoto eikä vika.

*Laiteajo 3.9.2026 (Galaxy Tab, adb-ohjattuna, mitään lähettämättä):* ackammonin
pikaviestin vastauskenttään kirjoitettiin `gl`, `Save text as phrase` lisäsi sen neljänneksi
napiksi, `Edit phrases` näytti rivin `hi × gg × ty gg u2 × Gl × Done`, napautus poisti sen,
`Done` palautti täyttävät napit ja luonnos tyhjennettiin. Laite jäi lähtötilaan. **Yksi
havainto:** näppäimistö kirjoitti `Gl` isolla alkukirjaimella, vaikka kentän
`KeyboardCapitalization` on oletus (ei mitään). Se on Samsungin näppäimistön oma
automaattinen iso alkukirjain eikä sovelluksen asetus, mutta rituaalifraasit ovat
pienellä, joten tallennettu fraasi voi tulla eri muodossa kuin oli tarkoitus. **Ratkaistu
samana päivänä Tommin päätöksellä:** tallennettava fraasi pienennetään sovelluksessa,
luonnokseen ei kosketa.

### Suunta lukittiin: sovellus pystyyn, lauta vaakaan (24.8.2026)

Tommin päätös, ja molemmat puolet nojaavat mittaukseen eivätkä makuun.

**Lukuruudut pystyyn.** Sivusto rivittää viestin 80 merkin kohdalta (`docs/AVOIMET.md`, sama
mittaus jolla vastauksen pituusraja poistettiin). Se rivinmitta täyttää kapean ruudun ja loppuu
leveällä noin puoliväliin, eli vaakasuunta hukkaa puolet leveydestä. Sama koskee
otteluluetteloa: pystyssä ruudulle mahtui 17 ottelua, vaakana kuusi (Galaxy Tab S7+).

**Lauta vaakaan.** Lauta on leveä kuvio jonka korkeus on sen pienin mitta, ja nappulan koko
johdetaan korkeudesta. Pystysuunnassa se kutistuisi.

**Yksi peruste lisää, Tommi 5.9.2026, ja se on kokemus eikä mittaus:** puhelimen
pystyasennossa laudan kohteet ovat liian pieniä osuttaviksi. Se ei korvaa ylläolevaa
geometriaperustetta vaan täydentää sitä; geometria sanoo mitä lauta tekee, tämä sanoo mitä
siitä seuraa sormelle.

Toteutus on `android:screenOrientation="portrait"` manifestissa kylmää käynnistystä varten ja
**yksi ohjaus `MainActivity`ssä**: navigoinnin nykyinen kohde ratkaisee suunnan, lauta vaakaan ja
kaikki muu pystyyn. Asetus tehdään `LaunchedEffect`issä kohteen vaihtuessa.

**Tämä kumoaa 10.8.2026 tehdyn päätöksen muodon muttei sen mittausta.** Silloin manifestista
poistettiin `screenOrientation="landscape"`, koska se koski koko sovellusta: Pixel 8a:n
vaakaikkunassa otteluluettelon yläosa täytti ruudun ja listalle jäi nolla korkeutta. Se mittaus
on yhä voimassa ja puhuu nyt pystylukon puolesta, koska juuri listaruudut kärsivät vaakatilasta.
*(Mitattu uudelleen 16.9.2026 nykyisellä luettelolla: listalle jää 90 pikseliä 1080:stä,
`docs/AVOIMET.md` › Pitäisikö koko sovellus olla vaakasuuntainen.)*
Kumoutuva osa on tiedostossa `Orientation.kt` ollut lause "muut ruudut seuraavat aina laitteen
omaa asentoa": ne seuraavat nyt sovelluksen pystylukkoa. `Orientation.kt` poistettiin, koska sen
`LockLandscapeWhileVisible` oli toinen omistaja samalle asialle.

**Kaksi omistajaa oli myös se vika joka tässä maksoi eniten, ja se kirjataan menetelmänä.**
Ensimmäinen versio oli oma `LandscapeWhileVisible`, joka kirjoitettiin `Orientation.kt`:ta
huomaamatta. Laudalta palattua pystylukko oli poissa (`mOrientation=UNSPECIFIED`,
`dumpsys activity activities`), ja syy diagnosoitiin ensin väärin: arveltiin että laite lukee
`requestedOrientation`in väärin ja että paluu pitäisi kirjoittaa kiinteänä pystynä. Se ei
auttanut, koska vanha `LockLandscapeWhileVisible` asetti poistuessaan `UNSPECIFIED` saman
kierroksen aikana. Vika ei siis ollut kummassakaan palautuksessa vaan siinä että niitä oli kaksi.

**Kokeen muoto on tässä yhtä tärkeä kuin tulos.** Suuntaa ei voi todentaa kuvasta: sama pysty
kuva syntyy lukosta ja tabletin omasta asennosta. Todennus tehtiin pitämällä tablettia
vaaka-asennossa ja kääntelemällä sitä, jolloin luettelo pysyi pystyssä, ja lukemalla
`mOrientation` sekä `cur=`-mitta `dumpsys`ista. Kolme aiempaa "todennusta" tässä samassa
sessiossa olivat kuvia, ja kaikki kolme olivat yhtä hyvin selitettävissä laitteen asennolla.

**Rajaus, joka kuuluu päätöksen viereen.** Lukitus koskee kaikkia käyttäjiä, ja laite joka on
telineessä tai näppäimistössä kiinni jää lukuruuduilla väärään asentoon. Siitä ei ole mittausta.
Jos se osoittautuu haitaksi, tästä tulee asetus; nyt se on yksi päätös eikä kaksi ylläpidettävää
haaraa.

**Poikkeus 19.9.2026: kirjoitushetki kääntää laudan pystyyn.** Tommin havainto pelisession
jälkeen, kun hän kirjoitti muistutusta tabletilla: *"muistutusrivin kirjoittamista auttaisi
jos lauta kutistuisi ja näyttö menisi pystyasentoon"*. Kaappaus samasta hetkestä näytti syyn.
Vaakanäppäimistö vie Galaxy Tab S7+:lla puolet ruudun korkeudesta, ja `imePadding` työntää
laudan ylös niin että näkyviin jää vain sen alareuna ja kenttä. Tommin valinta neljästä
(pystyyn kentän fokuksella, vaaka ja kutistus, kenttä laudan päälle, ei muutosta): **pystyyn**,
perustelulla *"lautahan ei ole pelattavissa, joten kuva siitä riittää"*. Kirjoitushetkellä
lauta on siis kuva eikä pelipinta, ja se saa kutistua leveyden mittaan.

Laukaisin on kirjoitustila eikä kentän fokus: `composing` (Reminders) tai `marking` (Mark
position), koska kenttä on olemassa vain siinä tilassa ja molemmilla on sama ongelma. Add
tai kirjoitustilan sulkeminen palauttaa vaakaan. **Chat-kenttä liittyi joukkoon 21.9.2026**
(Tommin tilaus: *"Message tulisi toimia samoin kuin Mark position, eli näyttö pystyyn ja
lauta on kuva ja mahtuu ruudulle myös näppäimistön kanssa"*): `chatComposer` on kolmas
ehto samassa `writing`-arvossa. Kenttä on kortissa laudan yläpuolella eikä sen alla, ja
tila sulkeutuu sivun vaihtuessa, koska viesti lähtee kortin oman napin (`Next Game`,
`To Top`) mukana. Kortti sai `Close`-tekstinapin samana päivänä (Tommin tilaus, hän oli
sulkenut vahingossa avatun kortin laitteen back-eleellä, joka vei laudalta pois):
sulkee kentän lähettämättä ja palauttaa vaakaan, luonnos katoaa kortin mukana.
Päättymisruudun kortilla nappia ei ole, koska siellä kenttä on aina auki. Todennettu tabletilla kuoriproxylla päivän sivulla 0007:
`raakasivut/sessio-21-9-paiva/kehykset/tabletti-message-pystyyn.png` (kortti, lauta
kuvana, `Skip Game` ja linkit keskikaistalla) ja `-nappaimisto.png` (kenttä, kortti ja
koko lauta näkyvissä näppäimistön kanssa). Pystyssä lauta piirtyy leveyden mittaan ja
saa korkeudekseen sen mikä leveydestä johtuu ([`DgBoard.frameHeight`]), ei koko ruutua, jotta
kiilat eivät veny; sivupaneelia ei ole, koska leveyttä ei jää, ja napit putoavat keskikaistalle
kuten puhelimella. Geometriaperuste (lauta kutistuu pystyssä) on yhä tosi, ja siksi tämä on
poikkeus hetkeen eikä uusi oletus.

**Kaksi mittausta samalta illalta, ja molemmat muuttivat toteutusta.** Ensimmäinen asennus
(klo 18.32): ruutu kääntyi, mutta näppäimistö vei laudasta puolet yläreunan yli ja kentän
alle jäi tyhjä kaista. Syy oli manifestista puuttunut `windowSoftInputMode`: järjestelmä
panoroi ikkunaa ylös näppäimistön verran, ja `imePadding` lisäsi saman verran alle. Tämä
oli sama vika joka vaakanäytön kaappauksessa näkyi laudan alareunana; `adjustResize` poisti
panoroinnin. Toinen asennus (klo 18.34): lauta täytti näppäimistön yläpuolisen tilan ja kiilat
venyivät, koska korkeuskatto luettiin mittasuhteesta (`maxWidth < maxHeight`) ja laatikko oli
näppäimistön kanssa lähes neliö. Katto luetaan nyt kirjoitustilasta, kuten kaanoni sanoo.
Kolmas asennus (klo 18.36) todennettu kaappauksesta, Tommi: *"nyt on hyvä"*.

**Haitta ilmeni 2.9.2026, ja siitä tuli asetus** (Tommin päätös: *"laiteasetus, mutta lauta
pidetään pakotettuna vaaka"*). Tommi yön session jälkeen: *"tabletilla pelatessa en enää pidä
siitä että näyttö pakottautuu pysty-näkymään kun ottelulista on tyhjä."* Jonon tyhjentyessä
lauta palaa luetteloon, ja kädessä vaakana oleva tabletti kääntyy pystyyn kesken session.
`portrait_lock` (`PortraitLockStore`, oletus päällä, `docs/ASETUKSET.md`) ohjaa vain
lukuruutuja: pois kytkettynä ne seuraavat laitteen asentoa (`UNSPECIFIED`), lauta on vaakaan
aina. Omistaja on yhä yksi: sama `LaunchedEffect` `MainActivity`ssä lukee kohteen ja
asetuksen, ja kylmää käynnistystä varten `onCreate` vapauttaa suunnan ennen ensimmäistä
ruutua, jotta manifestin `portrait` ei ehdi kääntää tablettia. Todennettu 2.9.2026 samalla
menetelmällä kuin lukko itse: `mOrientation=UNSPECIFIED` luettelossa lukon ollessa pois, ja
laitteen pakotettu vaakakierto (`user_rotation=1`) antoi luettelolle mitan `cur=2800x1752`.
Kuva ei kelpaa todisteeksi, koska sama kuva syntyy laitteen asennosta.

**Rajaus: Android 16 -tabletilla suuntaa ei pakoteta (Tommin päätös 4.10.2026).** Android 16
ohittaa isolla näytöllä (pienempi mitta vähintään 600 dp, eli tabletit ja auki taitetut
taittopuhelimet) manifestin `screenOrientation`in ja koodin `requestedOrientation`in, kun
kohde-SDK on 36. Pystylukko, aina vaakana pysyvä lauta ja kirjoittamisen pystyasento eivät
silloin vaikuta, ja jokainen ruutu kääntyy laitteen mukana. Play Console nosti tämän
suosituksena julkaisusta `1 (1.0)`. Valinta kahdesta: ennalleen ja rajaus kirjataan (valittu)
tai pakotus rajataan itse puhelimiin, mikä olisi muuttanut Tommin Android 13 -tabletin
käytöksen. Puhelimella kaikki toimii ennallaan, ja Tommin tabletti on Android 13:ssa, joten
siihen rajaus ei vielä osu. Käyttäjälle se sanotaan pystylukon selitteessä ja App Helpin
asetusvastauksessa. Ei mitattu laitteella, koska Android 16 -tablettia ei ole.

**Kirjautumisruutu ja puhelimella kirjoittaminen pystyyn (Tommin päätökset 1.10.2026).**
Mittaus samana iltana Pixel 8a:lla vaakana: kirjautumisruudun näppäimistö peitti molemmat
kentät ja Log In -napin, ja foorumin uuden ketjun lomakkeesta näkyi noin 130 dp eli vain
kentän yläreuna. Pystyssä näppäimistö peittää kirjautumisruudusta vain tilittömän rivin, ja
Tommin sanoin se on *"mihin pyrin"*. Kirjoittamisesta Tommin valinta neljästä (pystyyn
kirjoittaessa, kenttä kutistuu vaakana, vain kenttä näkyy, otsikot piiloon): **pystyyn
kirjoittaessa**, sama ratkaisu kuin laudalla 19.9.2026.

Molemmat ovat `orientationFor`in `signingIn`-ehto, laudan jälkeen ja ennen pystylukkoa.
Kirjoittamisen laukaisin on toisin kuin laudalla kenttäfokus yhdessä näkyvän näppäimistön
kanssa, koska ruutuja on monta eikä niillä ole yhteistä kirjoitustilaa. Fokus luetaan
NavHostin `onFocusChanged`ista (kosketuksella vain tekstikenttä saa fokuksen), ja dialogin
kenttä ilmoittaa itse `LocalReportTyping`in kautta, koska dialogi on oma ikkunansa. Ehto
koskee vain puhelinta (`smallestScreenWidthDp < 600`): tabletilla vaakana kenttä ja nappi
mahtuvat näppäimistön yläpuolelle, kun NavHostin `imePadding` kutistaa ruudun ja
`keepInViewWhileTyping` vierittää kentän ja sen napin yhdessä näkyviin. Paluu vaakaan odottaa
500 ms, koska näppäimistö voi välähtää piiloon kierron aikana.

Todennettu 1.10.2026 molemmilla laitteilla: Pixelillä foorumi, Lounge, Info, pelaajasivun
Send ja Invite sekä asetusten siirtodialogi kääntyivät pystyyn kenttä ja nappi näkyvissä, ja
näppäimistön sulkeminen palautti pyynnön `UNSPECIFIED`iin. Tabletilla foorumin Create New
Thread nousi vaakana näppäimistön yläpuolelle. Inboxin vastauskenttää ei mitattu, koska se
näkyy vain sivustolta otetun viestin kohdalla ja haku kuluttaa jonon; sillä on sama ryhmä
ja sama ehto kuin muilla. Pixel nollaa pakotetun kierron (`user_rotation`) kun sovellus
kääntää ruudun, joten paluu vaakaan luettiin `requestedOrientation`ista eikä kuvasta.

**Ottelun päättymisruutu jäi päätöksen ulkopuolelle** (Tommin havainto pelisessiossa
3.10.2026, Pixel 8a, `sessio-3-10-ilta`). Ehto rajasi laudan reitin pois (`!onBoard`), koska
lauta kääntyy omasta kirjoitustilastaan, mutta päättymisruutu on samalla reitillä. Vaakana
näppäimistö auki fraasirivi näkyi, ja lähetysnappiin pääsi vain vierittämällä. Tommi olisi
halunnut kirjoittaa pystyssä toiminnot nähden. Korjaus: ehto koskee laudan reittiä myös
(`writing = boardWriting || phoneTyping`), päättymisruutu sai oman `imePadding`in koska
NavHostin väistö ei koske laudan reittiä, ja chat-kortin kenttä, fraasit ja napit ovat yksi
`keepInViewWhileTyping`-ryhmä.

Todennettu samana iltana kuorella Pixel 8a:lla (vastustaja A:n päättymissivu `0111`, *Reply to
<vastustaja A>*): kenttään napautus käänsi ruudun pystyyn (`requestedOrientation=PORTRAIT`,
`cur=1080x2400`), ja kenttä, fraasit, Save text as phrase ja Send olivat näppäimistön
yläpuolella. Näppäimistön sulkeminen palautti vaakaan (`cur=2400x1080`). Kuvat:
`raakasivut/sessio-3-10-ilta/kuori/`. Chat-kortin ryhmää ei ajettu, koska sivulla ei ollut
chat-lomaketta.

**Päättymisruudun viestikortti on pisterivien yläpuolella** (Tommin valinta 3.10.2026). Tommi
tarkensi kuoritestin jälkeen, että Sendin alla ollut *Chat with <vastustaja A>* -kortti häiritsi:
se näytti toiselta tekstikentältä. Neljä vaihtoehtoa kaapattiin kuorella Pixeliltä vaakana ja
pystyssä (nykyinen, ylös, piiloon kentän ajaksi, lainauksena ilman laatikkoa; kuvat
`viestikortti-*`), ja valinta oli **ylös**. Ruutu luetaan silloin järjestyksessä: mitä
vastustaja sanoi, oma vastaus ja Send, sivun napit. Siirto koskee vain lomakkeetonta korttia.
Lomakkeellinen kortti on kirjoituspinta, ja siinä ovat sivun napit, joten se jäi nappien
paikalle. Sitä ei kokeiltu. Toinen kysymys samassa yhteydessä: kääntyykö päättymisruutu
puhelimella pystyyn heti avautuessaan? Tommin vastaus: **vain kirjoittaessa**, kuten muutkin
ruudut. Todennettu kuorella (`viestikortti-valittu-*`).

### Lauta pystyssä: kytkin, oletus vaaka (Tommin päätös 23.9.2026)

Tommin tilaus: *"tehdään pelitilaan muutokset, jotka mahdollistavat pelaamisen myös
pystytilassa"*. Kaksi vaihtoehtoa punnittiin mittoineen. **A** piirtää vaakalaudan leveyden
mittaan. **B** kääntää laudan 90 astetta, jolloin kiilat tulevat sivuilta. Nappulan koko
laskettuna eikä mitattuna, sama johtaminen kuin `DgBoard.frameHeight`: A antaa Pixel 8a:lla
19,8 dp ja Galaxy Tab S7+:lla 40,5 dp, B puhelimella noin 31 dp ja tabletilla katon 54 dp.
Vaakalaudan vastaavat luvut ovat 29,2 ja 54 dp. Tommin valinnat: **A kaikilla laitteilla**, ja
**asetus jonka oletus on vaaka**.

Tämä muuttaa ylemmän päätöksen muotoa muttei sen mittausta. Geometriaperuste on yhä tosi:
pystyssä lauta kutistuu, puhelimella eniten. Siksi pystytila on valittava eikä oletus (kuudes
arvo, `docs/ARVOT.md`). Selite sanoo hinnan: puhelimella nappulat ovat pienempiä.

**Toteutus.** Kytkin on `BoardRotationStore` (`board_follows_device`, oletus pois).
`orientationFor` antaa laudalle `UNSPECIFIED`:n kun kytkin on päällä, eli lauta seuraa
laitteen asentoa ja järjestelmän kiertolukkoa samoin kuin lukuruudut ilman pystylukkoa.
Kirjoitushetki kääntää yhä pystyyn kytkimestä riippumatta. Omistaja on edelleen yksi
`LaunchedEffect`. Laudan mitta on sama jonka kirjoitushetki sai 19.9.2026: lauta leveyden mittaan ja korkeus
leveydestä. **Sivupaneeli on laudan alla** (Tommin ohje ensimmäisen kaappauksen jälkeen: *"älä
ahda toimintoja laudalle vaan käytä pystynäytön korkeutta"*). Ensimmäisessä versiossa napit
olivat keskikaistalla kuten puhelimen vaakalaudalla, ja tabletilla `Mark position` ja
`Reminders` jäivät vierityksen taakse (`raakasivut/sessio-23-9-kuori/pysty-toiminnot-laudalla.png`).
Nyt paneeli on sama kuin vaakatabletilla, laudan levyinen ja sen alta jäävän korkeuden
mittainen, ja keskikaistalle jäävät nopat. Kuutio on silloin muurilla samalla säännöllä kuin
vaakatabletilla (`BoardScreen` `panelBelow`, `fits`). Ahtauden raja luetaan paneelin omasta
korkeudesta. Kirjoitushetkellä paneelia ei ole, koska näppäimistö vie sen korkeuden. Todennettu
tabletilla kuoriproxylla 23.9.2026 (`pysty-paneeli-alla.png`: `cur=1752x2800`,
`mOrientation=UNSPECIFIED`). Puhelimella ajamatta. Ehto on ikkunan suunta
(`LocalConfiguration`) eikä laatikon mittasuhde, koska chat-kortin kanssa laatikko on
puhelimella lähes neliö. Järjestelmäpalkkeja ei pystyssä piiloteta, koska ne vievät korkeutta ja lauta
mitoittuu pystyssä leveydestä. `configChanges` sisältää `orientation`in, joten kääntö kesken
kokoamisen ei luo aktiviteettia uudelleen eikä pudota poimittuja askelia.

**Kortit laudan ympärille samana iltana** (Tommin ehdotus kaappauksen jälkeen: *"mitä jos
vastustajan kortti olisi pelilaudan yläpuolella ja oma juuri alapuolella, nyt omistettu
tuplauskuutio olisi oudossa paikassa"*). Kortti on omistajuutta (14.9.2026, B), joten
laudan alla vastustajan omistama kuutio piirtyi väärälle puolelle lautaa. Järjestys ylhäältä
alas, Tommin valinta: ottelukortti ruudun ylälaidassa (hän kallistui ylimpään), vastustajan
kortti, lauta, oma kortti, pelinapit ja lopuksi `Skip Game` ja muistutukset rivissä
(`MatchCard` irrotettiin `SidePanel`ista yhteiseksi). Pixel 8a:lla `Cube reminder` jäi ensin
ruudun alle, joten pystytila sai oman ahtausrajansa (`DgBoard.PORTRAIT_PANEL_COMPACT_BELOW`,
mittaukset sen kuvauksessa). Todennettu kuoriproxylla molemmilla laitteilla:
`pysty-kortit-puhelin.png` ja `pysty-kortit-tabletti.png`.

**Vasenkätisyys ei vaikuta pystyssä** (Tommin päätös samana yönä): puolta ei ole, koska
paneeli on laudan ylä- ja alapuolella. **Vahvistusruutu on pystyssä aina napin oikealla
puolella** (Tommin tilaus: *"siirrä Verify Double (ja mahdollisesti muut verify) myös
tabletin näyttökoossa sitä vastaavan napin oikealle puolelle"*), myös kahden napin kehyksessä
(`Accept`, `Decline`), joka vaakapaneelissa jää pinoksi (`actionItems.verifyBeside`). Vaaka
on ennallaan. Todennettu tabletilla kuorella: `pysty-verify-double-tabletti.png` ja
`pysty-verify-accept-tabletti.png` (20.9. illan tarjouslauta, ottelutunnus vaihdettu) sekä puhelimella `pysty-verify-accept-puhelin.png`.

**Puhelimen pystylaudalla sivun tilarivit ovat laudan alla** (Tommin kuittaus 24.9.2026).
Sama puhelinkaappaus näytti, että keskikaista on siellä kahden pienen nappulan korkuinen:
*Rolled back* katkesi, ja *eino has doubled.* jäi kokonaan piiloon. Ehto on pystytilan
ahtausraja (`stripCrowded`), joten uutta kynnystä ei syntynyt. Tabletilla rivit jäävät
kaistalle 14.9.2026 päätöksen mukaisesti. Rivien yllä on viiva, joka erottaa ne toiminnoista (Tommin tilaus samana yönä). Todennettu kuorella: `pysty-tilarivit-puhelin.png`
ja `pysty-tilarivit-tabletti.png`.

**Pystylaudalla odotuksen ilmaisin saa pinon korkeuden** (Tommin havainto pelisessiossa
24.9.2026: *"pystyasennossa animaatio tarvitsee lisää pystytilaa"*). Ilmaisimet piirtyvät
kiinteällä koollaan (korkein deco-tiimalasi 70 dp), ja 44 dp:n nappipaikka jätti ne valumaan
korttien päälle. Paikka on nyt sen pinon korkuinen, jonka tilalle ilmaisin tulee, ja vähintään
80 dp (`PORTRAIT_BUSY_MIN`), joten `Skip Game` -rivi ei hyppää painalluksesta. Vaakapaneeli on
ennallaan. Todennettu tabletilla kuorella 5 s:n viiveellä: `raakasivut/sessio-24-9-yo/
pysty-odotus-tabletti.png` (Submit Move ja Undo Move, deco-aurinko niiden paikalla) ja Pixelillä
`pysty-odotus-puhelin.png`. Puhelimella kahden napin pino on 80 dp:tä matalampi, joten `Skip Game`
-rivi siirtyy odotuksen ajaksi noin 8 dp alaspäin.

**Peruuttamattoman napin lisäväli poistui kaikkialta, ja C3 kumottiin** (Tommin päätös
24.9.2026). Havainto samasta pelisessiosta: *"roll dice ja double nappien pystyväli on isompi
kuin muilla napeilla"*. Väli oli foorumiauditoinnin C3-vaatimus (vähintään 24 dp, korjattu
24.8.2026 `036c569`), ja se kysyttiin Tommilta vaatimuksen kanssa. Valinta oli kolmesta
vaihtoehdosta poisto: 24 dp:n raja ja kaikki välit 24 dp:hen jäivät valitsematta. Suoja on nyt kuutiokehys
ja vahvistusruutu. Nappien väli on kaikkialla pinon tai rivin oma väli, pystypinossa, vaakapaneelissa ja
keskikaistan rivillä. Yllä olevat 16.9.2026 kappaleet kuvaavat rakoa sellaisena kuin se oli, ja
ne jätettiin historiaksi. Todennettu tabletilla kuorella: `raakasivut/sessio-24-9-yo/
pysty-nappivali-tabletti.png` (Roll Dice ja Double samalla välillä kuin muut napit).

### Puhelimen pystylauta väljäksi korkeuden salliessa (Tommin valinta B 24.9.2026)

Tommi pelasi session Pixelillä pystylaudalla: *"hyvin onnistui, mutta on se aika
tihrustamista, napit ja toiminnot voisi olla korkeuden sen salliessa väljemmin"*. Mockupissa oli
kolme vaihtoehtoa, ja Tommi valitsi B:n. Napit ja kortti ovat täysikokoisia. Vapaa korkeus jaetaan
niin, että pelinapit ovat keskellä ja `Skip Game` sekä muut lisät alareunassa.

**Toteutus: väljä mitataan ensin ja ahdas on varamuoto** (`RoomyOrCompact`). Paneeli
koostetaan väljänä, ja sen luontainen korkeus verrataan laudalta jäävään. Jos se ei mahdu,
piirretään ahdas kuten ennen. Päätös syntyy samassa mittauksessa eikä kirjoita tilaa, joten
10.8.2026 värähtelyn luokka ei voi toistua. Tabletilla paneeli on jo väljä
(`PORTRAIT_PANEL_COMPACT_BELOW`), eikä väljää muotoa kokeilla. Lisien napit lukevat muodon
`LocalRoomyPortrait`ista. Pystypinon väli on väljässä 10 dp.

Todennettu Pixelillä kuorella: `raakasivut/sessio-24-9-yo/pysty-valja-puhelin.png` (Submit Move
ja Undo Move), `pysty-valja-odotus-puhelin.png` (ilmaisin pinon paikalla, ja `Skip Game` pysyy
paikallaan, koska jousto ottaa eron) sekä `pysty-valja-tuplaus-puhelin.png` (Roll Dice, Double
ja Verify sekä neljä lisää, mahtuu väljänä). Ahdasta varamuotoa ei ajettu, koska yksikään
kokeiltu tilanne ei tarvinnut sitä.

### Väljä muoto kiristettiin, ja kortit ja ottelukortti muuttuivat (Tommin valinnat 24.9.2026 aamuyöllä)

Tommi katsoi kuoritodennusta Pixeliltä ja teki neljä havaintoa. Kustakin kysyttiin tulkinta
monivalintana ennen koodia.

- **Tiivis ylös** (*"nyt oli jo liian väljää"*, valinta "kaikki väljät" ja sitten "tiivis
  ylös" kolmesta). Joustot poistuvat. Napit ja lisät ovat tasavälein heti oman kortin alla,
  ja vapaa korkeus jää ruudun alareunaan. Napit ja kortti pysyvät täysikokoisina, ja
  varamuoto on ennallaan.
- **Rastihuomautus tilarivien jatkoksi** (*"Verify tick viesti meni turnauskortin päälle"*,
  valinta kahdesta). Varapaikka ruudun yläreunassa on tehty vaakalautaa varten. Pystyssä
  siinä on ottelukortti, joten huomautus piirtyi sen otsikon päälle. Kun paneeli on laudan
  alla, huomautus on viivan alla viimeisenä tilarivinä.
- **Pelaajakortit kolmirivisiksi** (*"miksi pelaajakortit ovat 2-riviä ja 4-riviä?"*,
  vastaus *"molemmat 3 riviä"*). Vastustajan kortti oli laudan yllä ahdas (nimi ja away
  samalla rivillä, pipit alla). Oma kortti oli väljässä paneelissa täysi (nimi, away, tyhjä
  rivi ja pipit). Pystylaudalla kumpikin on nyt nimi, away ja pipit ilman tyhjää riviä.
- **Ottelun pituus palaa ottelukorttiin** (sama vastaus: *"turnaukseen pitää saada ottelun
  pituus näkyville"*, valinta "samalle riville" neljästä). **Tämä kumoaa 14.9.2026 päätöksen**
  (alla › *Kortit omistajuutena*), jonka peruste oli SUBSTANSSI 90 ja 92: away-luku kantaa
  pituuden. Pituus on nyt kierroksen ja siirron rivillä samalla sanalla kuin
  otteluluettelossa: `Round 2 · Length 11 · Move 427`. Tabletin kortissa kierros on omalla
  rivillään, joten sen jälkimmäinen rivi on `Length 11 · Move 427`. SUBSTANSSI ei muutu,
  koska sen kohdat koskevat merkitystä. Tilansäästö oli koodijalanjälkeä koskeva peruste, ja
  Tommi punnitsi sen nyt toisin.

Todennettu kuorella 20.9. illan tarjouslaudalla (`raakasivut/sessio-24-9-kuori/`, gitignoressa):
`pysty-tarjous-tiivis-puhelin.png`, `pysty-verify-rivi-puhelin.png` ja
`pysty-kortit-3rivia-puhelin.png` Pixelillä sekä `pysty-kortit-tabletti.png` ja
`pysty-verify-rivi-tabletti.png` tabletilla. Tabletilla huomautus jää keskikaistalle, koska
siellä on tilaa. Samassa ajossa todentui tuplauksen vastaanotto pystyssä: `Accept?`-dialogi,
rastivartija ja `Cancel` ilman pyyntöä kuoren lokissa. Ahdas varamuoto jäi yhä ajamatta (ajettu 26.9.2026, ks. alla). Pixelillä
väljä mahtui kaikissa kokeilluissa tiloissa, ja avattu chat on kirjoitustila, jossa paneeli ei
ole laudan alla lainkaan.

**Ahdas varamuoto ajettu 26.9.2026 fonttikoolla** (`raakasivut/sessio-26-9-pysty`, kuori).
Pixelillä väljä mahtuu vielä koolla 1.6. Koolla 2.0 ahdas laukeaa ja leikkaa tekstiä: 32 dp:n
napit eivät kanna suurennettua tekstiä, ja laudan pistenumerot rivittyvät. Oletuskoolla
kumpaakaan ei synny. **Korjattu samana iltana (Tommin valinta A):** ahtaan muodon napeilla
ja linkeillä on kiinteän korkeuden sijaan alaraja (`heightIn(min)`), joten ne kasvavat tekstin
mukana ja oletuskoolla ovat ennallaan. Pistenumerot eivät seuraa järjestelmän tekstikokoa
(`numberStyle`, perustelu `docs/AVOIMET.md`). Todennettu kuorella: `q04-korjattu-rinnakkain.png`
(koot 1.0, 1.3 ja 2.0), `q05-ahdas-korjattu-zoom.png` ja vaakalauta oletuskoolla
`q07-vaaka-1.0.png`. Koolla 2.0 paneeli oli lisäksi ruutua korkeampi eikä vierittynyt, joten
viimeinen tilarivi leikkautui alareunasta (`q06-fontti-2.0-vieritys.png`). **Korjattu samana
iltana (Tommin valinta A kolmesta): pystypaneeli vierittyy, kun se ei mahdu.** Mahtuessa
vieritys on inertti. Todennettu kuorella katkon aikana: `r05-vieritys-rinnakkain.png` (koko 1.0,
koko 2.0 ennen vieritystä ja sen jälkeen, jolloin *Site asleep. Nothing was confirmed.* näkyy).
Samassa kuvassa näkyy havainto jota ei korjattu: koolla 2.0 keskikaistan `Refresh` peittää
kuutosnopan, koska napin teksti kasvaa fontin mukana. Lauta on katkon ajan lukittu.
**Saa peittää (Tommin päätös 26.9.2026):** *"refresh saa peittää, sehän on vain hetken
ruudulla"*. Ei korjata.

**Samasta ajosta vielä kaksi korjausta.**

- **Virhetilan Refresh matalalla kaistalla** (Tommin valinta "kaistalla pienempi" kahdesta).
  Kuori esti yhden napautuksen (503), ja virhetila näytti puhelimen pystylaudalla kaksi vikaa.
  Napin teksti leikkautui pois, koska 60 dp korkea nappi ei mahtunut kaistalle. *Site error 503*
  taas piirtyi kuution päälle, koska pystylaudalla kuutio on napin oikealla puolella. Kun
  kaista on matalampi kuin täysi nappi, pystypehmuste putoaa, teksti on `titleMedium` ja
  virheteksti on napin vasemmalla. Tabletin ja vaakalaudan nappi on ennallaan. Todennettu:
  `virhe-kaista-korjattu-zoom.png` (ennen: `virhe-kaista-zoom.png`).
- **Pystylaudalla virhetilan syy on tilarivi toimintojen alla, ja kaistalla on pelkkä
  `Refresh`** (Tommin valinnat 24.9.2026: C kolmesta, *"teksteille olisi tilaa toimintojen
  alla kuten puhelimelle tehtiin"* ja *"kaikki pystylaudat"*). Käyttökatkon kuoriajo näytti,
  että tabletin täysikorkealla kaistalla syy jäi napin oikealle puolelle ja piirtyi kuutosnopan
  päälle (`sessio-24-9-kayttokatko/k06-siru-rajaus.png`). Vasen puoli ei ollut vapaa, koska
  siellä on sivun vihje *Please make a checker move.* Ensin valittu A (syy napin alle) ei
  mahtunut: kaista on kaksi nappulaa (76 dp), kuution peittävä nappi 60 dp, ja kaistan alle
  valunut teksti jäi laudan alapuoliskon alle (`a02-rajaus.png`). Syy on nyt pystypaneelin
  ensimmäinen tilarivi viivan alla, samassa asussa kuin muut tilarivit mutta lihavoituna.
  Tämä kumoaa samana yönä puhelimelle tehdyn ratkaisun, jossa syy oli napin vasemmalla.
  Matalan kaistan napin mitoitus jää voimaan. Vaakalaudalla syy on yhä napin oikealla.
- **Pelaajakortin väripisteellä on rengas** (Tommin havainto: vastustajan piste oli mustalla
  kortilla näkymätön; valinnat "rengas kuten laudalla" ja sitten "kortin kehyksen väri").
  Kaikki tyylit tarkistettiin kontrastina korttitaustaa `#17120F` vasten. Vastustajan piste
  oli Decossa 1,0, Monte Carlossa 1,1, sivuston Classicissa 2,7, Blue/Whitessa 3,3 ja X22:ssa
  3,5. Laudan oma rengas auttaa vain Decossa (kulta 8,8), koska muiden tyylien rengas on lähes
  musta. Siksi niissä rengas on kortin kehyksen väri (4,1). Todennettu Decolla
  (`piste-deco-puhelin.png`) ja Plain-teeman Monte Carlo -variantilla
  (`piste-montecarlo-plain-puhelin.png`), jonka jälkeen asetukset palautettiin.

**Turnausrivi sanoo vastustajan vuoron** (Tommin kysymys ja valinta samana yönä: *"mitä
tarkoittaa has an active game"*). Top Page luettelee vain ottelut joissa on oma vuoro, joten
luettelosta puuttuva käynnissä oleva ottelu odottaa vastustajaa. Rivi on nyt *active game,
opponent's turn*, ja kierrosta ja vastustajaa ei haeta. Toisen pelaajan profiilin
turnauslistalla sana on ennallaan, koska vuoroa ei tiedetä. Todennettu Pixelillä oikealla
datalla: `turnaus-vastustajan-vuoro-puhelin.png`.

### Kätisyys korteiksi, Deco-napit ja Off-pino (Tommin valinnat 24.9.2026)

Kolme toivetta saman yön pelisession jälkeen. Kustakin näytettiin mockup chatissa ennen
toteutusta.

**Handedness on kaksi korttia esikatseluineen** (*"Settings/Handedness kaipaisi
visualisoinnit"*, valinta kahdesta tulkinnasta). Kortit ovat `Right hand` ja `Left hand`. Kummassakin
on sama pelitilanne kuin lautakorteissa, ja sen vieressä on pelin oma `SidePanel` sillä puolella
jolle se menee (`PreviewBoardWithPanel`). Ensimmäinen versio oli kaavio, ja Tommi hylkäsi sen
samana yönä: *"samanlaisia kuvia laudoista kun ylempänä on, ei tuollaisia yksinkertaistuksia"*.
Kuva piirretään kolminkertaisena, koska kaksinkertaisena paneelin pino meni ottelukortin
päälle. Arvo ja tallennus (`HandednessStore`) ovat ennallaan, ja selite jäi korttien alle.
Todennettu tabletilla: `raakasivut/sessio-24-9-yo/handedness-kortit-tabletti.png`. Valinta
kirjoitti `left_handed`-arvoiksi true ja false, ja asetus palautettiin oikeakätiseksi.
Suurennuslasi lisättiin samana yönä Tommin tilauksesta (*"handedness kuvat tarvitsevat
suurennuslasi-toiminnon"*): sama `BoardZoomDialog` kuin lautakorteissa, kuva yksinkertaisena
piirtona. Todennettu `handedness-suurennus-tabletti.png`. Ensimmäisen kokeilun jälkeen
`left_handed` oli true. Syy on Tommin kertoma: hän käänsi tablettia kaappauksen ja napautuksen
välissä, joten napautus osui käännettyyn ruutuun. Uusintakoe tuoreen kaappauksen koordinaatilla: Close jätti arvon falseksi. Asetus on
oikeakätinen.

**Kortit ovat toistensa peilikuvat 27.9.2026 alkaen** (Tommin kysymys: *"eikö vasenkätinen
preferoisi Dailygammon asetusta Home boards on left side?"*, valinta neljästä). Ennen tätä
molemmat kortit piirsivät laudan tilin suunnassa, joten oletustilassa `Left hand` näytti
kotikentän oikealla ja paneelin samalla puolella, eli siirtokäden kaukana kotikentästä. Nyt
`Right hand` näyttää kotikentän oikealla ja `Left hand` vasemmalla, tilin asetuksesta
riippumatta, ja selite sekä Help kertovat että laudan saa siihen suuntaan DailyGammon-välilehden
`Home boards on left side` -kentästä. Kätisyys ja peilaus pysyvät erillisinä arvoina
(`SUBSTANSSI.md` kohta 103): kuva ehdottaa, mikään ei vaihdu itsestään. Peruste on symmetria
Tommin omasta asetelmasta (oikea käsi, kotikenttä oikealla, paneeli vasemmalla). Vasenkätisiltä
sitä ei ole kysytty.
Todennettu tabletilla klo 0.55 pystyasennossa (`raakasivut/sessio-27-9-yo/kehykset/handedness-peilikuvat.png`):
`Right hand` 1–6 oikealla alhaalla ja Off oikealla, `Left hand` 1–6 vasemmalla alhaalla, Off
vasemmalla ja paneeli oikealla. Tilin asetus on Tommilla pois, joten `Left hand` on nyt eri kuin
ennen. Laitteella oli valittuna `Left hand` (`left_handed` true, kirjoitettu klo 0.50 ennen
asennusta), eikä sitä muutettu.

**Deco-teemassa laudan napit ovat viistettyjä ja niissä on sisäviiva** (*"art deco animaatiot
ovat tyylikkäitä, voisiko nappeja koristella lisää?"*, valinta A ja B yhdessä neljästä).
Muoto on `CutCornerShape(7.dp)` kuten kuution kahdeksankulmio, ja 4 dp:n päässä reunasta
kulkee samanmuotoinen messinkiviiva (`DecoButtons.kt`). Mockupissa viiva oli napin
ulkopuolella, mutta toteutuksessa se on sisällä, koska pinon 6 dp:n välissä ulkoviivat
olisivat koskettaneet toisiaan. Muissa teemoissa napit ovat ennallaan (kuudes arvo).
Todennettu tabletilla kuorella: `deco-napit-off-pino-tabletti.png`. Puhelin on Plain-teemassa, joten siellä deco-nappeja ei ajettu.

**Off-lokerossa on syrjäpino ja numero** (*"Off lukumäärä ei erotu kunnolla"*, valinta D
neljästä). Aiemmin luku oli 10 sp:n tekstinä nappulan sisällä. Nyt jokainen poistettu on
viiva syrjästä katsottuna, ja luku on pinon alla 16 sp:n kokoisena. Viivan paksuus
johdetaan lokeron korkeudesta (2–6 dp, 15 nappulaa), jotta täysi pino mahtuu myös puhelimen
pystylaudan lokeroon, joka on ahtain. Todennettu kuorella (7 ja 5 poistettua) tabletilla,
`off-pino-zoom.png`, ja Pixelin pystylaudalla, `off-pino-puhelin.png`. Pixelillä viiva oli
3 dp:n alarajalla, ja siitä laskettuna 15 nappulan pino olisi mennyt Off-sanan päälle. Siksi
alaraja laskettiin 2 dp:hen. Täyttä pinoa ei ajettu.

**Off-sana pois, luku kuutiota kohti ja viiden ryhmät 2.10.2026** (Tommin havainto Sage
Pron lokerosta, kaikki kolme osaa valittu monivalinnasta). Pino ja luku kertovat jo mikä
lokero on, joten sana poistui (`board_off` samalla). Luku on lokeron siinä päässä joka on
kuutiota kohti. Ylälokerossa se on siis alhaalla ja alalokerossa ylhäällä, ja pino kasvaa
laudan ulkoreunasta. Viiden ryhmien väliin jää yhden viivan rako, jos 15 viivaa ja kaksi
rakoa mahtuvat lokeroon vähintään 3 dp:n viivalla (`OFF_SLAB_GROUPED_MIN`). Muuten pino on
tasainen kuten ennen. Todennettu kuorella (12 ja 15 poistettua, sivu sed-muokattu) molemmilla
laitteilla: `raakasivut/sessio-2-10-kuori/off-ryhmat-tabletti.png` ja `-puhelin.png`. Pixelin
pystylaudallakin ryhmät erottuvat. Ensimmäinen ajo antoi valkoiselle 17 (muokattu 15 ja
odottavan siirron kaksi), ja silloin pino meni puhelimessa luvun päälle
(`off-17-yli-puhelin.png`). Pelissä 15 on katto, joten se on muokkauksen eikä koodin vika.

**Luku pelaajan värisessä kiekossa tabletilla, sama ilta** (*"tabletilla määrät voisi pompata
enemmän taustasta, koska tilaa on"*, valinta neljästä). Kiekko on nappulan kokoinen mutta
korkeintaan lokeron levyinen, ja väri, reuna ja luvun väri ovat nappulan omat. Luvun korkeus
on 0,42 halkaisijasta (`OFF_COUNT_TEXT`). Ensimmäinen ajo piirsi kiekon myös Pixelille, jossa
siitä tuli noin 18 dp:n kokoinen ja luvusta lukukelvoton (`off-kiekko-liian-pieni-puhelin.png`).
Siksi alle 28 dp:n kiekkoa (`OFF_DISC_MIN`) ei piirretä, vaan luku on paljas 16 sp kuten ennen.
Todennettu kuorella: `off-kiekko-tabletti.png`, `off-kiekko-zoom.png` ja
`off-kiekko-puhelin-paljas.png`.

### Lovi ja sivupalkki: lauta ei väistä kameraa, ja sivulla oleva palkki piilotetaan (16.9.2026)

Tommin havainto Pixel 8a:n pelisessiosta 16.9.2026: *"sivupaneelissa on todella ahdasta ja
näyttöä kaventaa kamera, onko sille pakko antaa tilaa? sehän ei peittäisi juuri mitään
turnauskortissa"*. Mitattu laitteesta: loven inset vaakatilassa 121 px paneelin 600:sta,
itse reikä 67 px:n ympyrä 69 px reunasta pystysuunnassa keskellä, ja kolmen napin
navigointipalkki oikealla 126 px. Yhteensä kymmenesosa ikkunan leveydestä, ja kaikki
paneelista.

**Päätös on Tommin, ja se on kakkonen kolmesta:** lovi pois ja palkit piiloon myös leveyden
takia. Kolme muutosta ja yksi löydös.

1. `MainActivity` antaa luvan piirtää loven alle (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`,
   API 28+). Androidin oletus kieltää sen vaakatilassa, joten pelkkä paddingin poisto ei
   olisi riittänyt. Lukuruudut väistävät loven yhä `safeDrawingPadding`illa, joten lupa
   ei näy niissä.
   **Korjattu 4.10.2026:** oma asetus poistettiin, koska Play varoitti siitä (`SHORT_EDGES` on
   vanhentunut Android 15:ssä, lähde `MainActivity.onCreate`). Lupa ei kadonnut, koska
   `enableEdgeToEdge` antaa sen jo: API 30:stä alkaen `ALWAYS` ja API 28–29:ssä `SHORT_EDGES`
   (androidx.activity 1.9.3:n tavukoodi). Oma rivi ajettiin kirjaston jälkeen ja vaihtoi
   `ALWAYS`n takaisin. Kirjattu perustelu *oletus kieltää sen vaakatilassa* piti paikkansa
   ilman `enableEdgeToEdge`ä, mutta sen kanssa rivi oli tarpeeton. Laitteella 4.10.2026
   (Pixel 8a, Android 17): `dumpsys window` näyttää sovelluksen ikkunalle
   `layoutInDisplayCutoutMode=always` sekä ennen asennusta että sen jälkeen, koska Android 15+
   pakottaa `always`n kohde-SDK:lle 35+. Ikkuna on siis sama, eikä lautaa avattu erikseen,
   koska avaaminen kuluttaa.
2. `BoardScreen` väistää vain palkit (`WindowInsets.systemBars`), ei lovea. Tämä kumoaa
   24.8. kirjatun kommentin *safeDrawing kattaa myös näyttölovan* tarkoituksella. Hinta:
   reikä osuu ottelukortin vasempaan reunaviivaan, ja vasenkätisellä laudan vasempaan
   kehykseen pisteen 13 kohdalla. Kumpaakaan ei ole katsottu laitteella, koska kuoriajo
   ei näytä reikää; kaappaus piirtää sen alle.
3. `DgBoard.barsAreFree` saa leveyden: sivulla oleva palkki ei ole koskaan ilmainen.
   20.8. taulukon *Pixel 8a 72 dp* oli eleohjaus; kolmen napin palkki on puhelimen
   vaakatilassa sivulla, jolloin korkeusehto sanoi *ilmainen* ja palkki vei paneelista
   126 px. Tabletilla palkki on alhaalla myös vaakana, joten Tommin 20.8. ehto (*ainakin
   tabletille pitää saada ne kolme nappia*) pitää.

**Löydös: palkit eivät olleet piiloutuneet Pixelillä kertaakaan**, vaikka 20.8. testi
sanoo ettei niiden pidä näkyä. Logcat 16.9.2026: lautaruutu koostuu luettelosta tullessa
ensimmäisen kerran **vielä pystyssä** (914 dp, palkit ilmaisia), suuntalukko kääntää
ikkunan vasta sen jälkeen, ja avaimeton `remember` lukitsi *ilmainen* koko ruudun
eliniäksi. Nyt avain on suunta: pystyssä ei piiloteta, vaakatilassa mitataan kerran.
4.9. pallottelu ei palaa, koska palkkien piilotus ei vaihda suuntaa.

**Ahdas paneeli** samasta nauhasta: `Skip Game` piirtyi vastustajan pippien päälle, koska
keskilohko sai 180 dp ja tarvitsi 216. Syy on Material-tekstinapin 40 dp:n
vähimmäiskorkeus kertaa enimmillään neljä lisää. Alle 480 dp:n paneelissa
(`DgBoard.PANEL_COMPACT_BELOW`) linkit ovat 28 dp, `Skip Game` 32 dp ja kortit luopuvat
14.9. tyhjästä rivistään; tabletin 824 dp ei laukaise ehtoa. Todennettu kuoriproxylla
Pixelillä neljällä lisällä (`raakasivut/sessio-16-9-pixel/kuori_lauta4.png`): kaikki
mahtuvat, ei päällekkäisyyttä, lauta täyttää ikkunan reunasta reunaan. Tablettia ei
katsottu tässä, koska sen proxy-asetus on Tommin pelin alla.

**Tuplaustarjous oli vielä 60 dp yli** (Tommin kaappaus oikealta laudalta samana iltana ja
kuoriajo `kuori_tuplaus.png`): pino on silloin Roll Dice, peruuttamattoman rako ja
Double-kehys Verify-rivillä, yhteensä 170 dp. Tommin kolme päätöstä samana iltana, kaikki
vain ahtaaseen paneeliin: pelaajakortin nimi ja away samalle riville (pipit jäävät, koska
kuutio tarvitsee kaksirivisen kortin), lisät `FlowRow`hun sarakkeen sijaan ja pinon napit
36 dp:n korkuisina 44:n sijaan. Peruuttamattoman rako 24 dp ei kavennu, koska se on
auditoinnin vaatimus (C3). Todennettu kuoriajolla neljällä lisällä ja tuplaustarjouksella
(`kuori_tuplaus2.png`): lisät kahdella rivillä, kaikki mahtuu, ei päällekkäisyyttä.

**Tiukin tapaus löytyi saman illan pelisessiosta**: tuplaustarjous, kierrosrivi ja kuutio
omassa kortissa (`ahdas2.png` oikealta laudalta, kuoriajo `kuori_kierros.png`). Lisien
toinen rivi jäi ottelukortin alle. Tommin kaksi päätöstä: **Round ja Move samalle riville**
(`board_round_and_move`, vain ahtaassa paneelissa) ja **Double-nappi, ruutu ja Verify
samalle riville** kehyksen sisään (`compactFrame`, vain kun kehyksessä on yksi nappi;
Accept ja Decline pysyvät pinona). Samalla linkit 26 dp (24 leikkasi alleviivauksen,
`kuori_kierros2_z.png`), `Skip Game` 28 dp ja peruuttamattoman rako pinossa 6 dp, koska
pinon välit ja kehyksen pehmuste antavat sen kanssa täsmälleen C3:n 24 dp. Todennettu
`kuori_kierros4.png`: kaikki mahtuu ilmavasti, alleviivaukset näkyvät. Kosketusalat ovat
ahtaassa paneelissa Materialin suositusta pienemmät, ja se on tietoinen hinta.

**Pino irti ottelukortista** (Tommin kaappaus samana iltana: *"ottelukortti ja roll dice
nappi on liian kiinni toisissaan"*): ahtaassa lohkossa keskitys palautuu kiinni korttiin,
joten pino sai 8 dp:n yläpehmusteen, saman kuin korttien väli, ja se maksettiin pinon
napin korkeudesta (36 → 32 dp). Todennettu `kuori_kierros5.png`. Samalla `Message`
sai ahtaassa paneelissa saman korkeuden kuin `Skip Game` ja lisärivin alkiot keskitetään
pystysuunnassa (Tommin kaappaus päättymissivulta `ahdas4.png`: 40 ja 28 dp rinnakkain,
yläreunasta tasattuina). Kuoriajon sivuilla ei ollut chat-kenttää, joten `Message`n korkeus
on todennettu vain koodista ja odottaa pelisessiota.

Kameran reikä Tommin silmin samana iltana: *"ei ole vielä peittänyt mitään tärkeää, vain
vähän nappia tai ottelukortin reunoja"*. Paneelin vasenta pehmustetta ei tehdä.

### Muistutusnapit siirtyivät laudan omalle toimintoriville (24.8.2026)

Tommin havainto pelinäytöltä: muistutus vei laudan alta oman rivinsä. Se piti paikkansa myös
silloin kun muistutuksia ei ollut yhtäkään, koska rivi oli nappirivi eikä sisältörivi, ja lauta
kutistuu korkeudesta.

Napit ovat nyt `MiddleActionsRow`issa sivun omien nappien perässä. Kirjoituskenttä ja kirjoitetut
muistutukset ovat yhä laudan alla, mutta `ReminderStrip` ei piirrä mitään kun muistutuksia ei ole
eikä kenttä ole auki. Kirjoitustila asuu siksi `LoadedBoard`issa: napin ja kentän on jaettava se,
ja ne ovat nyt eri paikoissa.

**Varaus, joka kirjataan vaikka päätös on tehty.** Toimintorivin sanat ovat tähän asti olleet
sivun omia sanoja, ja se on ollut sääntö eikä sattuma: sovellus joka sanoo eri sanan kuin sivu,
eroaa kohteestaan juuri siinä kohdassa jossa käyttäjä vertaa niitä rinnakkain. Nyt samalla
rivillä on kahden lähteen sanoja. Ero pidetään näkyvissä kahdella tavalla, järjestyksellä (sivun
omat ensin) ja värillä (omamme vaimeampana). Claude esitti varauksen ennen toteutusta, Tommi
päätti toisin, ja tämä rivi on tässä siksi ettei muutos näytä myöhemmin huomaamatta
tapahtuneelta.

### Vakiokehote pois laudalta, napit ohjaavat (24.8.2026)

Tommin päätös laitteelta: *"Please select your action teksti on turha, antaa nappien ohjata."*
Rivi on sivulla joka kerta samana, eli se on sivuston kalusteita eikä tämän ottelun tietoa, ja
ruudulla se vie tilaa kertomatta mitään mitä toimintorivi ei jo sano.

**Tilannetekstit eivät ole tätä lajia eivätkä katoa.** Sivulla on kaksi eri asiaa samassa
kohdassa: `h4` kertoo mitä juuri tapahtui (esim. "Kayttaja A declines the cube action.") ja
`body > b` on tuo vakiorivi, joka on kehotteena vain silloin kun `h4`:ää ei ole. Ensimmäinen
piirretään ennallaan.

**Rajaus on `BoardState.situation`issa eikä jäsentimessä**, samasta syystä kuin
peruutusilmoituksella (`plainNotices`): malli kantaa sen mitä sivu sanoi, ja näkymä päättää mitä
siitä näytetään. Jäsennin joka pudottaisi rivin hävittäisi tiedon siitä että sivu ylipäätään
vastasi, ja se on eri väite kuin "tässä ei ollut mitään kerrottavaa".

Tunnistus nojaa rivin alkuun (`Please select your action`) eikä koko lauseeseen: loppuosassa on
kauttaviiva ja kaksoispiste, eli juuri ne merkit joiden pysyvyydestä ei ole näyttöä.

### Arkisto kertoo mihin on vastattu (24.8.2026)

Tommin pyyntö syntyi selainlaajennuksen `format_reply.js`:stä, joka esitäyttää vastauskentän
saadulla viestillä lainattuna (`> ` joka rivin edessä). Perustelu oli kuitenkin eri kuin
kirjoittamisen helpotus: *"haluan tietää mihin viesteihin on vastattu."*

**Valinta oli kahden tavan välillä, ja lainaus jäi pois.** Laajennuksen mallissa lainaus lähtee
viestin mukana, jolloin yhteys näkyy myös vastaanottajalle, mutta viesti kasvaa ja sivusto
rivittää sen 80 merkin kohdalta. Tommin valinta oli pitää viesti ennallaan ja kantaa yhteys
arkistossa: vastaanottaja saa vain kirjoitetun tekstin, ja yhteys näkyy vain tässä
sovelluksessa.

**Yhteys kirjataan lähetyshetkellä tai ei koskaan.** DailyGammonissa ei ole viestiketjuja eikä
viesti kanna mitään viittausta, joten tieto on olemassa vain jos se syntyi silloin kun vastaus
lähetettiin täältä. Ennen tätä päivää tallennetut vastaukset näyttävät siksi tavallisilta
viesteiltä, eikä niitä täydennetä jälkikäteen: mikä tahansa täydennetty arvo olisi keksitty.

**Ruudulla se on kaksi eri lausetta, koska kysymys esitetään kahteen suuntaan.** Vastauksen
rivillä lukee `In reply to:` ja vastatun viestin ensimmäinen rivi, ja se on tekstin yläpuolella
koska se on tekstin konteksti. Vastatun viestin rivillä lukee `You replied to this`. Lainaus on
yksi rivi eikä koko viesti: koko teksti on listalla omana rivinään, joten sen toistaminen olisi
sama sisältö kahdesti.

Kannassa tämä on `messages.replyToId` (migraatio 5 → 6) ja indeksi samalla sarakkeella, koska
jälkimmäinen kysymys on haku sillä. Kenttä ei ole osa `Message.id`-tiivistettä, samasta syystä
kuin `opponent` ei ole: tiiviste tunnistaa viestitapahtuman, ja tapahtuma on sama riippumatta
siitä mihin se vastasi.

### Play!-nappi otteluluettelon yläreunassa (25.8.2026)

Tommin havainto käytöstä: *"kun pelaaja tulee pitkään ottelulistaan, niin listaus on
toissijaista ja hänelle pitäisi olla Play! nappi."* Luettelossa oli mittaushetkellä 17
ottelua joissa on vuoro, ja ylin niistä on se johon pelaaja lähes aina menee.

**Nappi avaa luettelon ensimmäisen ottelun jossa on vuoro, eikä kutsu `/bg/nextgamea`.**
Tämä oli sessiossa esitetty valinta ja Tommin vastaus siihen, joten se on kirjattu
päätöksenä eikä toteutuksen sivuseikkana. Nappi kutsuu samaa `onOpenMatch`ia samalla
`Match`illa kuin ottelurivin napautus, eli uutta kykyä ei syntynyt ja jonon kuluttaminen
pysyy poissa. Ero riviin on pelkkä etsimisen poisjäänti.

**Valinta asuu domainissa** (`TopPage.playable`), koska kyse on valinnasta eikä asettelusta:
väärä valinta veisi pelaajan otteluun jossa ei ole vuoroa. Ehtoja on kaksi. `myTurn == true`
eikä `!= false`, koska `null` tarkoittaa ettei sivu kerro vuorosta; profiilisivun ottelut
ovat juuri sellaisia. `playPath != null`, koska osoitetta ei koota, ja ilman sivun antamaa
linkkiä nappi lupaisi avata ottelun jolle ei ole reittiä. Järjestys on sivun oma: mikä
tahansa oma lajittelu olisi väite siitä mikä ottelu on kiireellisin, eikä sivulla ole
sellaista tietoa.

**Nappi puuttuu kokonaan kun siirrettävää ei ole**, ei ole harmaana. Sama sääntö kuin
asetusruudun valintaruuduilla: harmaa sanoo että toiminto on olemassa mutta estetty juuri
nyt, ja tässä sitä ei ole olemassa. Tyhjä vuoro on lisäksi se tila jossa luettelo on
pelaajalle hyödyllinen.

**Rivi vierittyy sisällön mukana.** Kiinteä yläosa mitattiin rikkinäiseksi 10.8.2026:
vaakatilassa se täytti koko ikkunan eikä listalle jäänyt korkeutta. Sama veto pienempänä
olisi sama veto, ja nappi on joka tapauksessa näkyvissä ruutua avattaessa, koska lista alkaa
alusta.

Napin alla on kaksi riviä. Vastustajan nimi kertoo minne nappi vie, koska nappi vie
jonnekin eikä kysy minne. Luku kertoo monessako ottelussa on vuoro, eli kuinka monta kertaa
napin voi painaa peräkkäin; sanamuoto `matches where you can move` on sivuston oma taulukon
caption eikä käännös.

Todennettu laitteella 25.8.2026 (SM-T970): nappi, `Next: bertta` ja `17 matches where you
can move`, ja luettelon ylin rivi oli sama bertta. Nappia ei painettu, koska ottelusivun
avaaminen lukee myös keskustelun, ja lukeminen on se hetki jolloin viesti katoaa sivustolta.

### Sopimusmuutos: `SettingsViewModel` saa `FormSender`in (kirjattu ja vahvistettu 25.8.2026)

**Tämä on sopimusmuutos eikä toteutus, ja se kirjataan ennen koodia.** `AppContainer` sanoo
`FormSender`in vieressä nimeltä, että `TopViewModel` ja `SettingsViewModel` eivät saa sitä,
ja tämän dokumentin asetusnäkymäosio kirjaa lukutilan valintana. Sama järjestys kuin
`MessagesViewModel`illa 22.8.2026: luettelo muuttuu, periaate ei.

**Periaate joka säilyy.** Konstruktori kertoo kyvystä totuuden. Näkymämalli joka ei saa
`FormSender`iä ei voi tehdä sivustolla mitään, ja se on luettavissa signatuurista ilman että
runkoa tarvitsee lukea. Asetusruutu kirjoittaa jatkossa, joten sen on saatava se, ja väite
pysyy tarkkana nimenomaan siksi että luetteloa muutetaan eikä sääntöä.

**Mikä ei muutu.** Kohde on yhä vakio eikä parametri: ruutu ei ota polkua ulkoa, vaan lukee
`DgPages.SETTINGS_PATH`in ja lähettää `SETTINGS_FORM_ACTION`iin. `FormSubmission`in voi tuottaa
vain `SettingsPage.update`, joten kutsupaikka ei voi koota omaa osoitettaan. Kirjoittava ruutu
ei siis laajenna sitä mihin sovellus voi kirjoittaa, vain sitä kuka saa painaa lähetystä.

**Miksi tämä on painavampi kuin viestiruudun sama muutos.** Vastauksen lähettäminen on
peruuttamaton teko mutta rajattu: väärin mennyt vastaus on yksi väärä viesti. Asetuslomake
lähtee kokonaisena, joten yksi väärin luettu kenttä on menetetty asetus jonka alkuperäistä
tilaa ei ole enää missään. Siksi `docs/ASETUKSET.md` luku 5 vaatii lähtötilan talteenoton
ennen ensimmäistä kirjoitusta, ja siksi todennussuunnitelman järjestys on riskin järjestys:
kiertokoe verkossa ennen kuin lähetys on ruudulla saatavilla.

**Tila 25.8.2026: vahvistettu ja koodissa, mutta ei vielä ruudulla.** `SettingsViewModel` saa
`FormSender`in ja `SettingsBaseline`n, ja siinä on `save`, joka lukee sivun ennen lähetystä,
kokoaa `SettingsPage.update`illa, lähettää ja lukee vielä kerran. Kolme lukua yhtä tallennusta
kohti on tämän ruudun sopimus: ilman edeltävää lukua vartio vertaisi vanhentuneeseen
lomakkeeseen, ja ilman jälkimmäistä epäonnistunut lähetys näyttäisi onnistuneelta.

Lopputuloksia on viisi ja jokaisella on eri seuraus. `Saved` on ainoa joka muuttaa ruudun
näyttämää tilaa. `Mismatch` on vaarallisin, koska lähetys lähti mutta paluuluku on eri, ja se
on ainoa jossa käyttäjää pyydetään tarkistamaan selaimella. `NotSent` kantaa syyn eikä
mitään lähtenyt. `SessionExpired` on oma haaransa kuten muuallakin. `Blocked` on vartion
pysäytys ennen verkkoa, ja sen tavallisin syy `FormChanged` tarkoittaa että oikea teko on
päivittää ruutu eikä yrittää uudelleen.

Lähtötila otetaan talteen ensimmäisestä onnistuneesta luvusta eikä myöhemmistä
(`SettingsBaseline`, oma `SharedPreferences`-tiedosto). Myöhempi lukema on jo sen jälkeinen
tila että sovellus on voinut kirjoittaa, joten sen tallentaminen tekisi varmuuskopiosta
kopion mahdollisesta virheestä. Säilöä ei tyhjennetä uloskirjautumisessa, toisin kuin
tunnuksia ja keksejä.

~~**Ruutu on yhä lukutilassa, ja se on järjestys eikä keskeneräisyys.**~~ **Kumottu
25.8.2026 illalla: ruutu kirjoittaa.** Ehto jonka tämä kappale asetti täyttyi, eikä sitä
ohitettu: `docs/ASETUKSET.md`:n todennussuunnitelma vaati kiertokokeen oikeaa tiliä vasten
ennen kuin kirjoitus on ruudulla saatavilla, ja se ajettiin 25.8.2026 laitteelta (Galaxy Tab
S7+, Tommin kuittauksella *voit kokeilla toimivuutta*). Koe oli täysi kierros yhdellä
vaarattomalla kentällä: *Confirmation on declining doubles* päälle, tallennus, paluuluku
täsmäsi, sama kenttä pois, tallennus, paluuluku täsmäsi. Tili jäi siihen tilaan jossa se oli.
Kenttä valittiin siksi ettei se koske `/bg/nextgamea` (kentät `4` ja `5`) eikä lautaa
(`3`, `6`, `7`, `board`).

**Mitä ruudulla nyt on, ja mikä niistä on tae eikä koriste.** Muokattavat kentät kaikki
yksitoista, lähetysnappi sivuston omalla tekstillä `Update Preferences`, napin yläpuolella
muuttuvien asetusten määrä, ja palautus joka **täyttää lomakkeen muttei lähetä sitä**.
Viimeinen on tietoinen valinta: lähettävä palautus olisi kirjoitus jota käyttäjä ei nähnyt
ennen kuin se tapahtui, ja tämä ruutu on olemassa juuri siksi ettei sellaista tehdä. Nappi on
estetty kun muutoksia ei ole, koska muuttumattoman lomakkeen lähettäminen olisi kirjoitus
ilman syytä.

**Kaksi valintaruutua sai selitteen jota muilla ei ole.** *Skip Opponent's "Roll Dice" pages*
ja *Skip all automatic pages* muuttavat `/bg/nextgamen` käyttäytymistä, eli sitä kuluttavaa
reittiä jonka varassa viestiarkisto on. Selite on niiden vieressä samalla perusteella kuin
`messages_queue_explain` on avausnapin vieressä: manuaali ei ole näkyvissä sillä hetkellä kun
ruutua raksitetaan. **Tunnistus tehdään selitteestä eikä kentän numerosta**, koska numerot
ovat paljaita ja renumerointi osuisi muuten hiljaa väärään ruutuun; jos sivuston teksti
muuttuu, selite jää pois eikä siirry väärään paikkaan.

**Lukutilan perustelu ei kumoutunut vaan maksettiin, ja siksi vanha teksti jää näkyviin.**
Rajoitus oli olemassa siksi, että lomake lähtee kokonaisena ja yksi väärin luettu kenttä on
menetetty asetus jonka alkuperäistä tilaa ei ole enää missään. Hinta maksettiin neljällä
vartiolla (`SettingsPage.update`), kolmella luvulla yhtä tallennusta kohti ja lähtötilan
säilöllä. Sama koskee merkkijonoa `settings_read_only`, joka jätettiin `strings.xml`:ään
käytöstä poistettuna eikä poistettu.


### Muurin nappula on sillä puolella jonne se on menossa (6.9.2026)

**Tommin havainto pelatessa:** *"muurilla olevat napit pitää olla vastustajan puolella,
koska vastustajan kotikentälle muurilta yritetään päästä."* `BarSegment` antoi yläsegmentin
vastustajalle ja alasegmentin kirjautuneelle pelaajalle, eli se seurasi paneelien
järjestystä (vastustaja ylhäällä, itse alhaalla). Se on oikea sääntö paneeleille ja väärä
muurille.

Muuri ei ole pelaajan paikka vaan nappulan matkan alku. Oma nappula tulee muurilta sisään
vastustajan kotikenttään, joka on pisteet 19–24 eli ylärivi, joten se kuuluu ylös.
Vastustajan nappula tulee vastaavasti alas oman kodin puolelle. Vaihto on ristikkäinen ja
koskee vain väriä: pino kasvaa yhä keskiviivaa kohti, ja kosketus seuraa nappulaa, koska
`href` tulee sen mukana eikä segmentiltä.

**Nopat eivät kääntyneet**, eli vasen on yhä vastustajan ja oikea oma. Muuri ja nopat
lakkasivat siis noudattamasta samaa jakoa, ja se on tarkoituksellista: noppa kertoo kenen
heitto se on, muurin nappula kertoo minne se on menossa. Sama peruste kuin lokerolla, joka
on oikein jo nyt: omat nappulat kannetaan ulos omasta kodista alhaalta.

**Todentaminen odottaa nappulaa muurilla.** Sama ehto joka piti 25.8.2026 kolme vikaa
piilossa: ilman syötyä nappulaa segmentti on tyhjä eikä väri näy.

### Muuri ja laudan oikea laita (25.8.2026, laiteajossa)

Kolme muutosta samalta laudalta, jolla peruminen todennettiin. Ne kuuluvat yhteen siksi, että
kaikki kolme jäivät huomaamatta niin kauan kuin kellään ei ollut nappulaa muurilla.

**Muurin nappula piirtyy keskitilan viereen, ei laudan ulkoreunaan.** `BarSegment` kohdisti
pinon samalla säännöllä kuin kiila, eli kannasta kärkeä kohti, ja kiilalla kanta on
numeron puolella. Muurilla nappulan paikka on laudan keskellä, joten laina oli väärä.
Ulkoreunassa nappula asettui kiilojen numerorivin tasalle ja luki siksi väärin: se näytti
kuuluvan pisteelle.

~~**Muurin segmentti saa saman korostuksen kuin siirrettävä kiila.**~~ **Kumottu samana
päivänä yhdessä kiilan korostuksen kanssa, ks. seuraava kohta.** Muuri sai korostuksen ensin,
koska kiila oli korostanut siirrettävyyttä 10.8.2026 alkaen ja muuri ei: sallituista
kohteista ainoa oli myös ainoa korostamaton.

**Siirtokorostus poistettiin kokonaan (Tommin päätös 25.8.2026), ja se kumoaa 10.8.2026
tehdyn päätöksen.** Korostus tuli committissa `0b3b6cd` samaan aikaan kun kiilasta tuli
klikattava, ja perustelu oli että korostamaton kosketuskohde on arvattava.

Purkava tieto on tämän päivän mittaus: **sivusto tarjoaa siirtolinkin jokaiselle omalle
nappulalle**, ei vain laillisille lähdöille (`moves=[6, 8, 9, 13, 24]` laudalla jolla vain
muurilta tulo oli laillinen). Korostus ei siis kertonut mikä on laillista vaan missä on omia
nappuloita, ja sen pelaaja lukee jo nappulan väristä. Alkuperäinen perustelu nojasi
oletukseen jota linkkien luonne ei kanna.

Samalla poistui vika jota ei tarvitse enää korjata: `Palette.Accent` sulautui oranssiin
kiilaan, joten korostus näkyi vain vihreillä kiiloilla eli puolella laudasta.

Tästä ei seuraa että kohteet olisivat arvattavia. Kiila on kosketettava koko sarakkeen
levyydeltä ja muurin segmentti koko korkeudeltaan, eli kohteet ovat suuria; se mikä poistui,
oli merkintä eikä osumapinta.

**Kehyksen värinen kaista erottaa pelialueen lokerosarakkeesta** (Tommin valinta).
Oikea laita oli ainoa suunta jossa lauta ei päättynyt mihinkään, koska ylhäällä ja alhaalla
on kehyksen kaista ja keskellä muuri. Kaista on kehyksen jatke eikä uusi sarake.

Leveys on `FRAME_PAD_H` eli sama 8 dp joka erottaa pelialueen sivupaneelista vasemmassa
laidassa, ja se on Tommin tarkennus samana päivänä: ensimmäinen versio oli `OUTLINE`in
levyinen 1,5 dp:n viiva, jolloin laudalla oli kaksi eri levyistä reunaa. Mitta luetaan siis
kehyksestä eikä kirjoiteta omaksi vakiokseen.

**Mitä ei päätetty.** Vasemman reunan muoto esiteltiin kolmena vaihtoehtona (kehyskaista
molemmin puolin, symmetriset lokerot, lokeron sisällön siirto sivupaneeliin), eikä yhtäkään
valittu, joten ne eivät ole tässä kaanonina. Symmetriset lokerot ovat yhä auki
`docs/AVOIMET.md`:n oletuslautakohdassa.

**Havainto joka jäi korjaamatta.** Korostuksen väri on `Palette.Accent`, ja kiilat ovat
vuorotellen oransseja ja vihreitä. Vihreällä kiilalla reunus erottuu, oranssilla se sulautuu
taustaansa, eli korostus toimii puolella laudasta. Väriin ei koskettu, koska väripäätökset
odottavat design-puolta (`docs/AVOIMET.md`).

### Nappirivin värit: neljä tasoa ja kuutioteal (22.–24.8.2026)

Rivi kantaa neljää eri lajia tekoa (sivun tarjoama teko, peruuttamaton teko, peruminen, sivun
komento) ja niiden keskinäinen paino oli valittu yksi nappi kerrallaan eikä kertaakaan
yhdessä. Kaksi katsausta korjasi sen, ja **kumpikin tehtiin rivinä eikä nappina**; juuri se
kaatoi ehdotuksia joita yhden napin läpi katsottuna ei olisi huomattu.

**Juurisyy löytyi ensin, eikä kohta osannut kysyä sitä.** `MainActivity` kutsui
`MaterialTheme { }` ilman `colorScheme`-argumenttia, joten koko sovellus ajoi Material 3:n
oletusvaaleaa palettia. Violetti `#6750A4` tuli sieltä, eikä sitä ole laudan omassa paletissa
lainkaan: painotasot oli siis valittu väriskaalaa vasten jota kukaan ei ollut valinnut.

**Mitattu, ei arvioitu.** Luvut ovat kontrastisuhteita laudan kaistaa `#6E4C33` vasten,
laskettuna laitekaappauksesta ja paletin arvoista:

| Kohde | Ennen | Jälkeen |
|---|---|---|
| `Swap Dice` -teksti | 1,19:1 | 5,34:1 |
| Täytetyn napin muoto | 1,19:1 | 6,32:1 |
| Napin teksti täytettä vasten | 6,44:1 | 15,36:1 |
| Vahvistusruutu | 1,22:1 | 6,32:1 |

Kolme uskottavaa ehdotusta kaatui mittaukseen, ja ne kirjataan siksi. Peruuttamattomien
tekojen merkitseminen **punaisella** olisi ollut sama vika uudelleen (`#C7333D` kaistalla on
1,45:1, käytännössä näkymätön). Oletus että vain `Swap Dice` on rikki oli väärä: täytetty
nappi oli samat 1,19:1, ja ainoa syy miksi `Submit Move` erottui oli sen valkoinen teksti —
napin muoto oli kadonnut jo aiemmin eikä sitä huomannut kukaan, koska tekstin saattoi yhä
lukea. Ja vahvistusruutu puuttui ehdotuksesta kokonaan; se oli teeman oletusvärissä
`#49454F`, eli käytännössä sama lukema kuin `Swap Dice`n.

**Neljä tasoa, kaikki laudan omista väreistä.** Peruuttamaton teko ei ole *kevyempi* kuin
teko vaan **erivärinen**, koska se ei ole vähemmän tärkeä vaan eri laji.

| Taso | Teot | Muoto |
|---|---|---|
| Teko | `Submit Move`, `Roll Dice` | kermatäyte, tumma teksti |
| Peruuttamaton teko | `Double`, `Accept`, `Decline` | kermatäyte, kuutiotealin teksti ja reunus, vahvistusruutu ennallaan |
| Peruminen | `Undo Move` | pelkkä kermareunus, ei täytettä |
| Sivun komento | `Swap Dice` | pelkkä teksti, vaimea kerma |
| Vahvistusruutu | `Verify Double`, `Verify Accept` | kermareunus, tumma rasti |

**Peruuttamattoman teon väri on kuutioteal, ja se perustelee itsensä semanttisesti**
(Tommin valinta 24.8.2026 kankaalta, jossa neljä vaihtoehtoa oli rinnakkain
kontrastilukuineen). `Double`, `Accept` ja `Decline` ovat kuutiotekoja, ja kuutio on laudalla
juuri tuota väriä (`Palette.Cube`, `#2E6B62`). Teksti on kuutiotealia kermalla (5,11:1,
ylittää AA:n toisin kuin punaisen 4,37:1) ja reunus vaalea teal `#8FD4C6` kaistalla (4,52:1).
**Punaisen kaksoismerkitys poistui samalla:** laudalla punainen tarkoittaa taas vain
vastustajaa (`Palette.CheckerOpp`). Vahvistusruutu saa peruttavan teon värin eikä
peruuttamattoman, koska ruutu on portti eikä itse teko.

**Että peruuttamaton teko erottuu ylipäätään, on vahvistettu käytöstä eikä katsomisesta.**
Tommi epäili eroa ensimmäisen käyttökerran jälkeen (*"en tiedä onko hyvä idea että ne on eri
värisiä"*) ja hyväksyi sen seuraavan pelisession jälkeen (*"tykkään myös että Roll Dice ja
Double ovat eri väreillä"*). Sama pelaaja, sama nappipari, muutaman tunnin välein; ero on
siinä että jälkimmäinen lausuttiin pelattuaan eikä nähtyään.

**Peruminen säilyttää löydettävyytensä mutta menettää painon:** sama 6,32:1 kuin täytteellä,
mutta reunuksella eikä täytteellä. Fontin paino oli liian hiljainen signaali kantamaan eroa
yksin.

**Sivutulos joka koski muitakin ruutuja:** juurisyy korjattiin antamalla `MainActivity`lle
oma `colorScheme`, jonka primary on savupuu `#6B5844` eli laudan kehysväri hillittynä
(6,43:1 silloista oletustaustaa `#FEF7FF` vasten). Tekstinäkymien tausta jäi silloin auki.
**Se sulkeutui 6.9.2026**, ja luku on nyt 6,39:1 omaa taustaa `#FBF8F4` vasten; ks. alla
*Vaalean teeman pintapaletti*.

### Sivun komennot ovat nappeja, sovelluksen omat lisät tekstiä (25.8.2026)

**Tommin päätös: `Swap Dice` on perustoiminto ja siitä tulee nappi.** Toimintorivillä oli
siihen asti kolme samannäköistä tekstikomentoa, `Swap Dice`, `Reminders` ja `Cube reminder`,
joista vain ensimmäinen on sivuston oma teko ja kaksi jälkimmäistä sovelluksen omia lisiä.
Samanlainen ulkoasu väitti niistä samaa painoarvoa.

**Ehto luetaan lähteestä eikä sanasta.** Nappeja ovat `board.commands`, eli ne teot jotka
sivu itse tarjoaa; tekstiksi jäävät sovelluksen omat. `Swap Dice`iin nimenä sidottu poikkeus
olisi kaatunut ensimmäiseen tuntemattomaan komentoon, ja se olisi ollut hiljainen kaatuminen:
uusi komento olisi näyttänyt muistutusnapilta.

**Ero perumiseen säilyy, mutta se siirtyi.** `Undo Move` erottui 22.8.2026 alkaen siitä että
se oli ainoa nappi muiden ollessa tekstiä. Nyt ero on värissä: peruminen käyttää nappulan
väriä (`CheckerSelf`), sivun muut komennot toissijaista tekstiväriä.

### `Skip Game` laudan komentoriville (26.8.2026)

**Foorumiauditoinnin kohta G2** (`docs/AUDITOINTI-FOORUMI.md`): puuttuva skip-nappi on
DG Mobilen ainoa nimetty valitus lokakuulta 2025, ja sivustolla teko on ollut olemassa koko
ajan. Nappi ei siis tuo uutta kykyä vaan lopettaa sen että sovellus jättää sivuston oman
komennon näyttämättä.

**Miksi se ei tullut mukaan aiemmin.** Komennoiksi luetaan linkit joissa on `move=`, ja
`Skip Game` on `/bg/nextgame?skip=<ottelu>`. Ehdon löysääminen olisi tuonut mukanaan koko
navigointipalkin (`Top Page`, `Game Lounge`, `Log Out`) ja `Review Gamen`, joten skip
luetaan omalla ehdollaan omaan kenttäänsä (`BoardState.skipHref`).

**Tunnistus on osoitteessa eikä sanassa.** Sanaan `Skip Game` sidottu ehto kaatuisi hiljaa
jos sivu vaihtaisi sanan, ja silloin nappi tekisi yhä saman kuluttavan teon väärällä
nimellä. Ruudun teksti tulee siksi omasta resurssistaan (`board_skip_game`,
`translatable="false"`), ja se on sivun sana koska pelaaja vertaa sitä sivustoon.

**Se on rivin viimeinen sivun komento, ei `Swap Dicen` vieressä.** Skip on ainoa rivin
napeista jonka jälkeen ruudulla on eri peli, ja C3:n oppi on että vahingossa painettava
nappi on lähellä `Roll Dicea`. Järjestys on siis: peruminen, sivun muut komennot, skip,
ja vasta sitten sovelluksen omat lisät.

**Ottelunvaihdon poikkeus on tarkka.** Portti joka torjuu väärän ottelun on olemassa siksi
ettei sivuston oma siirtymä seuraavaan peliin piirtyisi väärän otsikon alle. Skip saa saman
poikkeuksen kuin lomakkeen `Next Game`, ja ehto luetaan painetusta osoitteesta eikä
vastauksesta: muu ottelu jossakin muussa vastauksessa on yhä hiljainen virhe.

**Mitä tämä kuluttaa.** `/bg/nextgame` on jonoa kuluttava osoite, eli skip ohittaa tämän
ottelun siinä jonossa. Sovellus ei hae sitä koskaan itse: osoite lähtee verkkoon vain
painalluksesta, kuten muutkin laudan linkit.

### Sivun teot sivupaneeliin, vasempaan reunaan peukalon alle (2.9.2026)

**Tommin havainto illan pelisession jälkeen:** *"häiritsee kun pitää koko ajan klikkailla
keskelle ruutua missä napeilla on tilaa"*, ja tarkennus kysyttäessä: matka keskelle ja käsi
peittää laudan, napit reunaan peukalon alle. Ensin oikea reuna, sitten heti korjattuna
**vasen, koska se helpottaa kaksikätistä pelaamista.** Havainto on pelattavuutta (`CLAUDE.md`,
kolmas arvo): kolme viikkoa keskikaistalla riitti kertomaan että keskellä oleva nappi
maksaa käden liikkeen ja laudan näkyvyyden joka vuorolla.

**Koti on sivupaneeli, koska se on jo vasen reuna.** `PanelActionStack` piirtää saman
sisällön samassa järjestyksessä kuin `MiddleActionsRow` (yhteinen `actionItems`-lista:
lomakkeen napit, vahvistusruutu, peruminen, sivun muut komennot), vain pystyyn ja paneelin
levyisinä, jotta osumakohta on koko reuna eikä sana. Peruuttamattoman teon väli on
pystysuuntainen (`Roll Dice`, väli, `Double`). Keskikaista on silloin pelkät nopat.

**Pino on paneelin pohjalla lähes nimen kiinni, ja se on Tommin tarkennus laiteajon
jälkeen** (2.9.2026): *"pino saisi olla alempana lähes nimessä kiinni: skip game,
reminders, cube reminder osio ylempänä kuin roll dice, double ja verify double."*
Ensimmäinen versio oli ottelutietojen alla paneelin keskivaiheilla. Nyt ottelutiedot ja
omat lisät (`Skip Game`, muistutukset) ovat keskilohkossa ja sivun teot painuvat joustolla
pohjalle oman pelaajakortin päälle. `Skip Game` ei siis ole `Roll Dicen` kyljessä (C3,
`docs/AUDITOINTI-FOORUMI.md`): väli on koko jousto eikä vakio. Tommin kuittaus samalta
illalta: *"käytettävyys nousee kertaheitolla."*

**Kaksi tarkennusta samana iltana.** Pinon ja nimikortin väli on vahvistusruudun mitta
(`VERIFY_BOX`, 20 dp), Tommin sanoin *"checkboxin koon verran"*: mitta on jo ruudulla oleva
mitta eikä uusi luku. Ja **nopat ovat puoliskon keskellä** silloin kun kaistalla ei ole
nappeja: neljän kiilan varaus (26.8.2026) oli olemassa nappien takia, ja ilman nappeja
lokero on koko puolisko eli kuusi kiilaa (`HALF_POINTS`). Nopat ovat siis vuorossa olevan
pelaajan puoliskon keskellä, pisteiden 15 ja 16 tai 9 ja 10 välissä. Kun napit ovat
kaistalla (ei paneelia), varaus pysyy neljässä.

**Omat nopat paneelia vastapäiseen laitaan 13.9.2026, kumottu 14.9.2026.** Tommi pelasi
sillä yhden 20 ottelun session ja päätti: *"omat nopat ovat keskikaistan oikeassa laidassa,
pitäisi olla keskellä"*, tarkennettuna *"nopat puoliskon keskelle"*. Sääntö on taas sama
molemmille pelaajille, eli puoliskon keskikohta (2.9.2026), ja `panelOnRight` ei enää kulje
kaistalle. Alkuperäinen perustelu jää tähän tiedoksi (Tommin päätös: *"nopat ja Submit
vastakkaisiin laitoihin, kaksikätinen pelaaminen on meidän etu"*). Puoliskon keskikohta
kumoutuu omien noppien osalta silloin kun paneeli on: toinen käsi siirtää nappulat ja
napauttaa nopat laudan laidassa, toinen painaa paneelin napit, eivätkä kädet risteä. Laita
voittaa puoliskon, eli peilatulla laudalla omat nopat ovat silti paneelista kauimpana, koska
sääntö koskee käsiä eikä kotikenttää. Vastustajan nopat pysyvät puoliskonsa keskellä, koska ne
ovat tietoa eivätkä kosketuskohde. Ilman paneelia (kapea ruutu) mikään ei muutu. Keskikaistan
korkeus pysyi kahdessa nappulassa: viisi muunnelmaa (1,5, 2,5 ja 3 kehyksen värillä, 2 ja 2,5
huovan värillä) ajettiin kuorella samana iltana, ja nappula ei tällä laitteella muutu ennen
kolmea, joten korkeus oli lähes ilmainen valinta eikä syytä muuttaa. **Kaista on variantissa
huovan värinen** (Tommin päätös samana iltana: *"keskikaista oli parempi huovan värisenä,
off-lokero erotettuna"*), ja muuri piirretään kaistan läpi, koska katkos näytti virheeltä eikä
valinnalta. X-22 ja sivustouskollinen pitivät kehyksen värisen kaistan (`BoardLook.band`).
**X-22 seurasi perässä 14.9.2026** (Tommin havainto pelisessiossa: *"x22-lauta keskikaista
pitäisi olla laudan huovan värinen kuten eilen monte carlo variantille tehtiin"*), ja muuri
jatkuu siinäkin kaistan läpi samalla piirrolla. Lokerosarake pysyi X-22:ssa huovan värisenä,
koska havainto koski kaistaa eikä saraketta. ~~Sivustouskollinen pitää kehyksen värisen
kaistan.~~ **Sivustouskollinen seurasi 16.9.2026** (Tommin havainto Pixelin pelisessiossa:
*"huopa ja keskikaista pitäisi olla sama väri"*), joten `BoardLook.band` on nyt huopa
oletuksena eikä yksikään tyyli poikkea.
Samalla tarkistettiin Tommin pyynnöstä että lautasuhteet ovat tyylien välillä yhtenäiset:
`BoardMetrics` ei haaraudu tyylin mukaan lainkaan, joten ainoat erot variantin ja X-22:n
välillä ovat värit ja lokerosarakkeen tausta.

**Yhden kodin päätös 14.8.2026 kumoutuu osittain, ja ero on tarkoin rajattu.** Silloin
purettiin kolme leveysehtoista kotia. Nyt koteja on kaksi, mutta uutta leveysehtoa ei synny:
napit ovat paneelissa täsmälleen silloin kun paneeli on (`fits`), ja muuten keskikaistalla
kuten ennenkin. Se on sama ehto jolla muistutusnapit ja `Skip Game` jo vaihtavat kotia
(27.8.2026), joten haaroja ei tullut lisää, vain yksi haara kantaa nyt enemmän.

**Tuplauksen vahvistus siirtyi laudasta kutsujalle.** Dialogi ja sen ehto asuivat
`Board`issa, mutta sama `Double` voi nyt tulla paneelista joka on laudan ulkopuolella.
`pressChecked` on nyt ruudun tasolla ja kulkee sekä paneeliin, keskikaistalle että
kuutiolle; lauta ei enää tiedä dialogista mitään. Todennettu kuoriproxylla: rastittamaton
`Double` paneelista avasi dialogin eikä lähettänyt mitään. **Ehto vaihtui 3.9.2026** (rasti
pakollinen, dialogi sen jälkeen, ks. vahvistusruudun kohta ylempänä), reitti pysyi: sama
`pressChecked` kaikilla kolmella reitillä, ja rastin tila kulkee samaa tietä `VerifyBox`ina.
Todennettu kuoriproxylla 3.9.2026 tarjouslaudalla: rastittamaton `Accept` näytti kortin
eikä lähettänyt, rastitettu avasi `Accept?`-dialogin, ja kuittaus lähetti
`?submit=Accept&verify=Accept` yhtenä pyyntönä.

**Kuutio: omistettu lokerosarakkeessa, omistamaton muurilla, tarjottu noppien paikalla
(Tommin päätös 3.9.2026, kahdessa erässä).** Tommi: *"kun minut tuplataan tai minä tuplaan,
niin tuplauskuutio pitäisi liikkua sinne minne nopat tulevat"*. Ensimmäinen tulkinta vei
omistetun kuution omistajan puoliskolle muurin laitaan, ja se todennettiin kuoriproxylla;
Tommi korjasi saman illan: *"tuplattu ja vastaanotettu kuutio pitäisi olla samassa paikassa
kuten ennen, omistamaton kuutio on muurilla, tarjottu kuutio pitäisi olla noppien
paikalla."* Lopullinen sääntö on siis kolme paikkaa:

- **Omistettu kuutio** lokerosarakkeen päässä kuten 9.8.2026 alkaen, vastustajan ylhäällä ja
  oma alhaalla. Omistaja luetaan sivulta (`CubePosition`, solun `VALIGN`).
- **Omistamaton ja tuntematon kuutio** muurin kohdalla keskikaistan keskellä.
- **Tarjottu kuutio** oman puoliskon keskellä, siihen mihin omat nopat tulisivat, koska
  vastaus on oma. Sivu ei merkitse tarjousta kuutioon (kuva on yhä `cube1` keskellä,
  mitattu 17 tarjouslaudasta), joten tarjous luetaan lomakkeen `Accept`-napista. Kuutio
  piirtyy sillä arvolla jonka sivu näyttää, eli tuplaamattomana. **Kuution klikkaus on
  silloin Accept** (Tommi: *"jos Verify Accept ruksattuna, niin painamalla Accept-nappia tai
  klikkaamalla tuplauskuutiota saa sen omistukseensa"*), samaa rasti- ja dialogireittiä kuin
  nappi (`cubeActionFor`).
- **Tarjous voittaa omistuksen (14.9.2026).** Uudelleentuplauksessa sivu näyttää kuution yhä
  omistajan solussa ja lomake tarjoaa `Accept`ia, eli omistus ja tarjous ovat molemmat tosia.
  Siihen asti omistus luettiin ensin ja kuutio jäi vastustajan lokeron päähän (Tommin
  havainto pelisessiossa: *"kun vastustaja teki redouble -> 4 niin tarjotun kuulan pitäisi olla
  keskikaistalla eikä hänen omistuksessaan"*). Sääntö on `cubeOnStrip`, testattu ilman
  Composea. Todentamatta laitteella: seuraava uudelleentuplaus ratkaisee.

Kun sinä tuplaat, sivusto arvaa hyväksynnän heti ja vastaus on jo seuraava lauta, jossa
kuutio on vastustajan omistama (mitattu 16 `?submit=Double&verify=Double`-vastauksesta);
omaa tarjoustilaa ei siis ole piirrettävänä. Kaista saa kuution samalla ehdolla kuin nopat
keskitettiin puoliskolle 2.9.2026 (paneeli on); kun napit ovat kaistalla (puhelin), kaikki
kolme paikkaa ovat lokerosarakkeessa kuten ennen. Sama kuutio on aina tasan yhdessä
paikassa. **Todennettu kuoriproxylla 3.9.2026** illan laudoilla: oma DR-kuutio
lokerosarakkeen alapäässä, vastustajan kakkoskuutio yläpäässä, omistamaton muurilla ja
tarjottu oman puoliskon keskellä.

Sama ilta paljasti DR-kuution ALT-ansan: `cubedrs.gif` kantoi ALTia `cubedrs`, ja kuutiossa
luki `CUBEDRS`. Laji luetaan nyt tiedostonimestä (`Cube.doubleRepeat`), ks. `docs/KOHDE.md`.

**Todennettu kuoriproxylla 2.9.2026** kahdella laudalla: siirtovuoro (`Submit Move`,
`Undo Move`, väli, `Skip Game`, `Reminders`) ja heittovuoro (`Roll Dice`, väli, `Double`
kuutiotealina, `Verify Double`, väli, `Skip Game`, muistutukset). Puhelimella ei todennettu,
mutta siellä paneeli mahtuu (914 x 411 dp), joten pino on sielläkin.

### Tyhjä noppalokero ei varaa leveyttä (26.8.2026)

**Tommin havainto ja ehdotus laiteajossa:** `Skip Game` ei mahtunut ruudulle, ja hän kysyi
voisiko rivi käyttää noppien tilan silloin kun nopat ovat vastustajan puolella.

Keskikaista on kolme osaa: vastustajan noppalokero, nappirivi ja oma noppalokero. Molemmat
lokerot varasivat kahden kiilan levyisen kaistan **aina**, myös tyhjinä, vaikka nopat ovat
aina vain toisella puolella. Nappirivi puristui niiden väliin ja vieri vaakasuunnassa, eli
rivin viimeinen nappi jäi laidan taakse. `Skip Game` teki puutteen näkyväksi, mutta vika oli
olemassa jo ennen sitä: `Cube reminder` oli katkennut samalla tavalla.

**Näkyvät nopat eivät liiku.** Ne ovat kiinni omassa lokerossaan rivin laidassa, ja
vapautuva tila on vastakkaisella puolella. Myös kohdistus kolmannen ja neljännen kiilan
kohdalle säilyy, koska se lasketaan lokeron omasta leveydestä eikä rivin keskikohdasta.

**Ehto on nopan olemassaolo eikä vuoro.** Vuoro on pääteltyä, nopat ovat sivulla. Sama
periaate kuin muualla: sivun kertoma tieto voittaa sovelluksen päättelyn.

**Kohdistus korjattiin samana päivänä, koska ensimmäinen versio siirsi napit sivuun.**
Kolmiosaisessa rivissä napit saivat vain sen tilan joka jäi lokeroiden väliin, joten
tyhjän lokeron vapauttama kaista veti ryhmän omalle puolelleen: napit asettuivat jäljelle
jääneen tilan keskelle eivätkä laudan keskelle. Kaista on siksi `Box` eikä `Row`, nopat
ovat kiinni omissa laidoissaan (`CenterStart`, `CenterEnd`) ja napit keskellä. Napit
piirretään viimeisenä, eli nopat jäisivät niiden alle jos rivi joskus kasvaa lokeroon
asti; se on tietoinen järjestys, koska leikkautunut nappi on korjattavissa mutta nopan
peittävä nappi väittäisi laudasta väärin.

### Lähellä nollaa oleva aika erottuu otteluluettelossa (26.8.2026)

Foorumiauditoinnin kohta H4 (`docs/AUDITOINTI-FOORUMI.md`): `Grace` ja `Time Pool` olivat
sarakkeina ja yksikkö mitattu, mutta lopussa oleva aika näytti samalta kuin satojen tuntien
pooli.

**Kaksi lukemaa merkitään eri tavalla, koska ne tarkoittavat eri asiaa.** Mitattu kello on
kaksivaiheinen (`docs/KOHDE.md`, 25.8.2026): Grace kuluu ensin ja vasta sen loputtua Time
Pool alkaa kulua. Nollaan tullut Grace kertoo siis että kello käy nyt, ja se saa
tertiary-värin. Vähiin käynyt Time Pool kertoo että ottelu on lähellä aikatappiota, ja se
saa virhevärin. **Ottelun hävittää vain jälkimmäinen**, joten sama merkintä molemmille
olisi väittänyt kahdesta eri tilanteesta yhtä.

**Raja on 48 tuntia, ja se on Tommin valinta eikä mittaustulos.** Mitatut poolilukemat ovat
satoja tunteja (`234:39`, `561:18`), joten alle kahden vuorokauden pooli on eri kokoluokkaa
kuin tavallinen rivi. Luku asuu yhdessä paikassa (`MatchClock.POOL_WARNING_MINUTES`).

**Muoto on väri ja lihavointi eikä ikoni tai taustasävy.** Sarakkeet ovat kapeita, eikä
leveyttä saa viedä juuri niiltä luvuilta joissa tieto on; taustasävy taas olisi kilpaillut
vuorotunnuksen kanssa. Lihavointi on toinen kanava värin rinnalla, joten merkintä ei nojaa
pelkkään väriin.

**Tulkinta on erillinen lukija eikä mallin muutos.** `Match.graceText` ja `timePoolText`
pysyvät tekstinä, koska ne ovat sitä mitä sivu sanoo. `MatchClock` lukee niistä minuutit
vain tätä merkintää varten, ja se palauttaa nullin jokaisesta arvosta joka ei ole muotoa
`tunnit:minuutit`: päättyneeltä ottelulta kentät puuttuvat, ajattomassa ottelussa ne ovat
`-`, ja `Finished matches` -taulukossa on nähty `n/a`. Yksikään näistä ei ole nolla, ja
nollaksi tulkittuna jokainen olisi antanut varoituksen juuri niille otteluille joilla ei ole
kelloa lainkaan.

**Laitteella nähtiin toinen puoli.** `Grace 0:00` osui useaan riviin heti, koska
kiireellisyysjärjestyksen alkupää on juuri niitä. Virhevärin laukaisevaa poolia ei nähty,
koska Tommin neljässäkymmenessä ottelussa pienin pooli on `159:02`; se puoli nojaa
`MatchClockTest`iin siihen asti että sellainen ottelu tulee vastaan.

### Otteluluettelon sarakkeet mitattiin kapealla ruudulla (26.8.2026)

Auditoinnin kohdat D2 ja D3 vaativat mittauksen jota ei voi tehdä koodia lukemalla, ja
Tommi antoi luvan muuttaa laitteen asetuksia ja palauttaa ne perään. Mittaus ajettiin
hyväksyntäehdon oloissa: `wm size 720x1280` ja `wm density 360`, eli 320 dp, sekä
`font_scale 1.3`. Kolme puutetta löytyi, ja kaikki kolme ovat samaa lajia: rivi luotti
siihen että tilaa on.

**Tapahtumanimi vei koko leveyden ja puristi vuorotunnuksen pylvääksi.** `Your turn` taittui
neljälle riville kirjain kerrallaan (`Yo/ur/tur/n`). Nimi saa nyt jäljelle jäävän tilan ja
katkeaa itse kahden rivin jälkeen. Katkaisu kuuluu nimelle, koska nimi on toistuvaa tekstiä
(`DG 8x8 2026 - C vs F - Div 1`) ja tunnus ei ole.

**Sarakkeiden arvot koskettivat toisiaan** (`296:125/6`), joten jokainen solu sai välin.

**Leikattu vastustajanimi näytti kokonaiselta mutta toiselta nimeltä.** `Willie` on eri
pelaaja kuin `Ville Vaara`, eikä leikkaus näkynyt mistään. Katkaisumerkki lisättiin, ja se
on sama periaate kuin muualla: sovellus ei saa näyttää vajaata tietoa täydeltä.

**Väli ja katkaisumerkki söivät Time Poolin**, joka katkesi muotoon `184:…` juuri niillä
riveillä joilla aikaa on eniten kulunut. Sarake sai lisää painoa vastustajan kustannuksella.
Katkennut nimi on haitaton, koska rivin avaaminen kertoo sen; katkennut aika ei ole, koska
se on tässä taulukossa se luettava luku. Sarakeotsikot katkeavat kapealla ruudulla
(`Time P…`, `Ro…`), ja se jää: otsikko sanotaan kerran ja arvot ovat sen alla.

**`font_scale 2.0` tabletin omalla koolla ei rikkonut mitään** otteluluettelossa,
viestiarkistossa eikä manuaalissa. Laudan keskikaistan nappirivi sen sijaan ei mahdu
noppalokeroiden väliin isolla fontilla: `Reminders` piirtyi noppien päälle 1.3:lla ja `Cube
reminder` 2.0:lla. Piirtojärjestys on tietoinen (ks. *Tyhjä noppalokero ei varaa
leveyttä*), mutta iso fontti tekee siitä tavallisen tilanteen eikä reunatapauksen, ja
päätös siitä on auki.

### Nappirivi väistää nopat (26.8.2026)

Edellisen kohdan mittaus jätti yhden tapauksen auki, ja Tommi ratkaisi sen samana päivänä:
*korjaa rivi siirtämällä tekstejä ja nappeja tarvittaessa, että nopat näkyvät täysin*.

**Vika oli siinä mihin piirtojärjestys nojasi.** Nappirivi sai koko kaistan leveyden ja
piirrettiin viimeisenä, eli nopat jäivät sen alle jos rivi ylettyi lokeroon asti. Se oli
tietoinen valinta, mutta se nojasi siihen että peittyminen on reunatapaus. Iso
järjestelmäfontti teki siitä tavallisen tilanteen: `Reminders` peitti nopat 320 dp:llä ja
`font_scale 1.3`:lla, `Cube reminder` tabletin omalla koolla ja `2.0`:lla.

**Varaus koskee vain sitä laitaa jossa nopat ovat.** Tyhjä lokero ei varaa leveyttä, joten
rivi saa käyttää sen tilan; nopat ovat aina vain toisella puolella.

**Napit pysyvät laudan keskellä niin kauan kuin ne mahtuvat sinne, ja väistävät vasta
sitten.** Järjestys on tässä koko sääntö. Pelkkä kohdistus jäljelle jäävään tilaan olisi
vienyt rivin sivuun myös silloin kun se olisi mahtunut keskelle, eli palauttanut sen vian
joka korjattiin samana päivänä aiemmin (*Tyhjä noppalokero ei varaa leveyttä*). Sijoittelu
on siksi oma `Layout` eikä kohdistus, ja laskenta on erotettu testattavaksi
(`middleActionsX`, `MiddleActionsPlacementTest`): yksi testi vahtii keskellä pysymistä ja
toinen väistämistä, joten kumpikaan korjaus ei voi hiljaa kumota toista.

**Mitä ei muutettu.** Nappien tekstejä ei lyhennetä eikä käännetä, joten kapealla ruudulla
rivin loppupää on yhä vierityksen takana. Ero edelliseen on se että vieritys pysyy nyt
varauksen sisällä: pahimmillaan nappi on vierityksen päässä, mikä on korjattavissa
vierittämällä, kun taas peitetty noppa ei ollut korjattavissa mitenkään.

Todennettu laitteella (SM-T970) kolmella kokoonpanolla: 320 dp ja `font_scale 1.3`, tabletin
oma koko ja `2.0`, sekä tabletin oma koko ja `1.0`. Nopat näkyvät kaikissa kokonaan, myös
rivi vieritettynä, ja oletuskoolla napit ovat laudan keskellä kuten ennen muutosta.

### Otteluluettelon oma lajittelu (26.8.2026)

Auditoinnin kohta G1, listan ainoa kokonaan puuttunut P2. Se on koko listassa se kohta joka
osuu suorimmin kaanoniin, koska `SUBSTANSSI.md` kohta 21 sanoo että **sivuston järjestys on
kiireellisyysjärjestys** ja että sitä ei saa muuttaa ilman että pelaaja pyytää. Ominaisuus on
siis mahdollinen vain siinä muodossa jossa oletus säilyy ja pyyntö on näkyvä.

**Otsikkorivi on ohjain** (Tommin valinta). Napautus lajittelee sarakkeen mukaan, toinen
kääntää suunnan ja kolmas palaa sivuston järjestykseen. Kolmas napautus on tässä se osa joka
ratkaisee kaanonikysymyksen: oletukseen pääsee samalla eleellä jolla siitä lähdettiin, eikä
paluu vaadi erillistä nollausnappia jonka olemassaolo pitäisi arvata.

Valittu sarake näkyy nuolena ja korostuvana otsikkona. Nuoli on otsikon perässä eikä omana
ikoninaan, koska sarakkeet ovat kapeita ja ikoni veisi leveyttä myös niiltä otsikoilta joita
ei ole valittu (ks. edellinen sarakemittaus).

**Lajiteltavia ovat viisi olemassa olevaa saraketta.** Tapahtumanimi ei ole sarake vaan rivin
otsikko, eikä sille tehty omaa ohjainta: kaksi ohjainta samaan asiaan olisi ollut kalliimpi
kuin puuttuva lajitteluperuste, ja tapahtuman mukaan ryhmittely on eri kysymys kuin
sarakkeen mukaan järjestäminen.

**Kaksi sääntöä ovat sisältöä eivätkä toteutusta**, ja molemmat on testattu (`MatchOrderTest`).
Lajittelu on vakaa, joten saman arvon jakavat ottelut pysyvät sivuston keskinäisessä
järjestyksessä; kaksi yhtä pitkää ottelua ovat siis yhä kiireellisyysjärjestyksessä
keskenään. Ja puuttuva arvo on viimeisenä molempiin suuntiin, koska puuttuva ei ole suurin
eikä pienin: ilman sitä käänteinen järjestys nostaisi tyhjät rivit listan kärkeen.

**Aika luetaan lukuna eikä tekstinä.** Tekstinä `9:00` olisi suurempi kuin `13:26`. Lukija on
sama `MatchClock` jolla aikamerkintä tehdään, eli tulkinta on yhdessä paikassa.

**Valinta muistetaan käynnistysten yli** (Tommin valinta). Hinta on kirjattu, koska se on
tiedossa: unohtunut lajittelu voi jättää kiireellisimmän ottelun listan häntään. Siksi
valittu sarake on merkitty, ja manuaalissa sanotaan suoraan että nuoli sarakkeessa tarkoittaa
ettei lista ole enää määräaikajärjestyksessä.

**Play!-nappi seuraa näkyvää järjestystä.** `TopPage.playable` valitsee yhä sen mitä voi
avata, mutta järjestys tulee nyt kutsujalta (`List<Match>.playable()`), ja ruutu poimii napin
ottelun samasta listasta jonka se piirtää. Lupaus *avaa listan ensimmäisen* pysyy siis
kirjaimellisesti totena myös lajitellussa listassa. Vaihtoehto olisi ollut pitää nappi
sivuston järjestyksessä, mutta silloin se avaisi eri ottelun kuin ylin rivi, ja se ero olisi
pitänyt selittää ruudulla.

**Manuaaliin tuli oma kohtansa**, koska otsikkorivi ei näytä napilta ennen ensimmäistä
painallusta. Kappale kertoo myös mitä sivuston oma järjestys tarkoittaa: ilman sitä pelaaja
ei tiedä mitä hän vaihtaa pois.

Todennettu laitteella (SM-T970): oletus on sivuston järjestys ilman nuolta, `Opponent`
lajittelee isoista kirjaimista riippumatta (`19pel10`, `aapo`, `aarni`, `aatum`, `bertta`,
`brunok`, `CarlaLaine`), toinen napautus kääntää, kolmas palauttaa oletuksen, ja
`Time Pool ↓` säilyi sovelluksen uudelleenkäynnistyksen yli. Samassa kuvassa näkyi myös
vakaus: viisi ottelua joilla on `750:00` olivat keskenään grace-järjestyksessä. Play!-rivi
seurasi kärkeä joka kerta (`Next: aarni`, `Next: 19pel10`, `Next: CarlaLaine`).

### Play!-nappi piilotettiin (26.8.2026)

~~Nappi luettelon yläreunassa avasi ensimmäisen ottelun jossa on vuoro.~~ **Tommin päätös
käytön perusteella:** *"en enää muista miksi Play! napin halusin, taitaa olla turhake."*

Nappi lisättiin 25.8.2026 hänen omasta havainnostaan (*"listaus on toissijaista ja hänelle
pitäisi olla Play! nappi"*), eikä perustelu ollut väärä silloin. Ero on siinä että nappi ei
koskaan tuonut uutta kykyä: se teki saman kuin ottelurivin napautus, ja luettelon ylin rivi
on nappia vasten yhden napautuksen päässä joka tapauksessa. Kun etsimisen poisjäänti ei
tuntunut käytössä miltään, jäljelle jäi rivi joka vie pystytilaa listan yläpäästä, ja se
yläpää on `SUBSTANSSI.md` kohdan 21 mukaan juuri se osa jota ei saa pudottaa.

Poistettuja ovat nappi, sen kaksi tukiriviä (`Next: <nimi>` ja ottelumäärä), niiden neljä
merkkijonoa sekä domainin `TopPage.playable`, sen laajennus ja `TopPagePlayableTest`.
Valinta ei jäänyt kutsujattomaksi koodiksi, koska kutsujaton valinta olisi väite että jokin
ruutu tekee valinnan. **Kaikki löytyy gitistä** (`502bd3e` on viimeinen commit jossa nappi
on mukana), joten palautus on revert eikä uudelleenkirjoitus.

Sivuvaikutus kahteen kirjeluonnokseen on kirjattu niihin itseensä: `docs/YHTEYDENOTTO.md`
menetti taulukostaan rivin, jonka takeeksi nimetty testi poistettiin samalla, ja
`docs/YHTEYDENOTTO_HAPE42.md`:n liitekuva `otteluluettelo-rajattu.png` on nyt vanhentunut.

### Tumma tila seuraa laitteen asetusta (26.8.2026)

Auditoinnin kohta D5, joka oli 24.8.2026 linjattu odottamaan tarvetta. Tarve tuli Tommin
pyyntönä.

**Asetusta noudatetaan, sitä ei tarjota uudestaan.** Sovelluksessa ei ole omaa
kolmiasentoista valintaa (vaalea, tumma, järjestelmä): se olisi toinen paikka jossa sama
asia päätetään, ja vaatimus on nimenomaan järjestelmän asetuksen noudattaminen.

**Tummassa paletissa on nimetty kaksi väriä, ja loput ovat Material 3:n omia.** Primary on
sama puu vaaleampana, koska vaalean teeman `#6B5844` on tummaa taustaa vasten 1,84:1 eli
lukukelvoton; `#CBB093` on 8,2:1. Tertiary on nimetty siksi, ettei se saa törmätä erroriin:
otteluluettelo merkitsee kaksi aikatilaa näillä kahdella roolilla, ja Material 3:n tummat
oletukset (`#EFB8C8` ja `#F2B8B5`) ovat lähes sama vaaleanpunainen, jolloin H4:n koko ero
katoaisi juuri siltä riviltä jota varten merkintä tehtiin. `#E6C36A` on kellertävä ja erottuu
punaisesta myös harmaasävyisenä. Vaaleaan palettiin ei kosketa, koska siellä oletukset
erottuvat toisistaan (`#7D5260` ja `#B3261E`).

**Palkkien ikonit seuraavat nyt samaa asetusta.** Ne oli 21.8.2026 lyöty kiinni vaaleiksi,
ja perustelu oli silloin oikea: sovelluksella ei ollut tummaa teemaa, joten yötilan
valitsemat vaaleat ikonit jäivät vaalealle taustalle näkymättömiin. Perustelu kaatui tämän
muutoksen myötä, joten vakio korvattiin saman asetuksen lukemisella. Lautaruutu on tämän
poikkeus kuten ennenkin ja hoitaa oman tilansa itse.

**`values-night/themes.xml` on osa korjausta eikä koriste.** Ohut Android-teema on se mitä
käyttöjärjestelmä näyttää ennen kuin Compose ehtii piirtää; ilman yöversiota yötilassa
käynnistyvä sovellus välähtäisi valkoisena.

**Lautaruutu ei muutu, ja se on rajaus eikä puute.** Laudan värit ovat mitattu kokonaisuus
(X-22) jonka tausta on jo lähes musta, eikä sitä voi kääntää paloittain rikkomatta juuri
sitä mittaa. Sivustouskollisessa tilassa värit tulevat pelaajan omista DailyGammon
-asetuksista, eikä yöasetus saa ohittaa niitä.

Todennettu laitteella (SM-T970, `cmd uimode night yes` ja `no`, laite palautettiin
lähtötilaansa `yes`): otteluluettelo, viestiarkisto ja manuaali piirtyvät tummina,
linkkirivit vaalean puun värisinä, `Grace 0:00` kellertävänä, tilapalkin ikonit vaaleina
tummaa taustaa vasten, ja lauta samanlaisena kuin vaaleassa tilassa. Vaalea tila oli
kaappauksissa muuttumaton.

### Välilehtinavigointi ja kaksi uutta ruutua (kirjattu 26.8.2026)

Tommin pyyntö: Matches, Game Lounge, Discussion, Message Archive, Settings ja App help
ovat välilehtiä, joihin ei pääse lautaruudusta vaan listaruuduilta. Rajaukset sovittiin
monivalinnalla samana päivänä: vieritettävä välilehtipalkki ruudun yläreunassa, palkki
näkyy kaikilla kuudella välilehdellä muttei laudalla, Lounge saa luvun lisäksi Joinin,
Discussion on ketjulista ja ketjun luku ilman kirjoittamista.

**Palkki korvaa linkkirivit ja Back-napit.** `TopScreen`in `MessagesLine`, `SettingsLine`
ja `HelpLine` poistuvat, samoin viesti-, asetus- ja manuaaliruudun Back-napit. Perustelu
jonka `HelpLine`n KDoc kantoi (yläpalkissa ei ole tilaa kolmannelle toiminnolle) ei
kumoudu vaan siirtyy: toiminnot eivät mene yläpalkkiin vaan omaan palkkiinsa, jonka
leveys ei kilpaile otsikon kanssa. Järjestelmä-back miltä tahansa välilehdeltä palaa
otteluluetteloon, koska pino on aina enintään kaksi: `top` ja päällä oleva välilehti tai
lauta. `WaitingNotice`n napautus valitsee arkistovälilehden eikä avaa erillistä ruutua.

**Loungen ja palstan polut luetaan Top Pagen navigointipalkista** (`TopPage.loungePath`,
`TopPage.forumPath`), ei koota koodissa. Sama muoto kuin `messageQueuePath`illa. Ennen
kuin otteluluettelo on latautunut, kummallakin ruudulla on NoPath-tila joka sanoo syyn;
välilehteä ei harmaannuteta, koska tila on selitettävissä ja harmaa ei ole.

### Sopimusmuutos: `LoungeViewModel` saa `FormSender`in (kirjattu 26.8.2026)

Sama järjestys kuin `MessagesViewModel`illa 22.8.2026 ja `SettingsViewModel`illa
25.8.2026: luettelo muuttuu, periaate ei. Kirjattu ennen koodia, Tommin kuittaus samasta
suunnittelukeskustelusta jossa Join valittiin ("Luku + Join").

**`FormSubmission`in tuottajia on jatkossa kolme:** `BoardForm.press`, `ReplyForm.write`
ja uusi `LoungeInvitation.join`. Takuu ei muutu: `join` käärii sivun oman Join-linkin
hrefin sellaisenaan (`/bg/lounge?action=accept&id=...&userid=...`) GET-lähetykseksi
tyhjin kentin, eikä kokoa mitään. Kutsu on mitattu 26.8.2026 kaappauksesta
`raakasivut/sessio/0574_bg_lounge.html`: Join ei ole lomake vaan tavallinen linkki, joten
`FormMethod.GET` ja `DgClient.sendAsQuery` ovat täsmälleen oikea reitti, ja linkin oma
kysely kulkee koskemattomana.

**Miksi tämä on eri painoinen kuin asetuslomake.** Join on peruuttamaton (ottelu alkaa
heti) mutta rajattu: väärä Join on yksi väärä ottelu, ei menetetty tila. Vartioksi
riittää vahvistusdialogi ennen lähetystä. Lähetystä ei laiteta `ActionQueue`en eikä
yritetä uudelleen `AuthFailed`in jälkeen, koska tuplalähetys hyväksyisi kaksi kutsua.

**Join-vastauksen muotoa ei ole mitattu.** Yhtään accept-vastausta ei ole kaapattu, joten
näkymämalli lukee vastauksen varovasti: tunnistettu lounge-sivu jäsennetään, tuntematon
sivu näytetään huomautuksena ja lounge haetaan kerran uudelleen. Ensimmäinen oikea Join
ajetaan Tommin kuittauksella ja vastaus kaapataan `raakasivut/`-kansioon.

~~**Tournament Sign-Up jäsennetään muttei toteuteta.** Se on samaa GET-linkkilajia kuin
Join, mutta rajattu tästä kierroksesta pois tietoisesti, ks. `docs/AVOIMET.md`.~~ Toteutettu
3.9.2026 kaanonimuutoksella, ks. *Turnaukseen ilmoittautuminen ja sen peruminen*. Tuottajia on
siitä alkaen viisi: `LoungeTournament.signUp` ja `cancelSignup` samalla takuulla.

~~**`DiscussionViewModel` ei saa `FormSender`iä**, ja se on luettavissa konstruktorista:
palstaruutu on lukutilassa, `Add a Comment` ja uuden ketjun aloitus jäävät selaimeen.~~
Purettu 3.9.2026 Tommin päätöksellä, ks. *Forum-välilehti: uusi ketju ja kommentti*.

### Välilehdet kategorioiksi: kuudesta viiteen (päätetty 26.8.2026)

Edellinen osio kirjasi kuusi välilehteä vieritettävään palkkiin. Tämä muuttaa niiden
lukumäärän ja perusteen, ei palkin paikkaa eikä sitä ettei lauta ole välilehti.

**Muutoksen syy on jatkosuunta eikä palkki itsessään.** Kun koko sivuston tieto tuodaan
välilehdiksi (`docs/AVOIMET.md`, Tommi 26.8.2026), puuttuvia kohteita on yhdeksän. Kuusi
plus yhdeksän on yksitoista välilehteä, ja silloin palkki ei ole enää navigointi vaan
lista. Tommin tavoite: **viisi tai vähemmän, eikä yhtään alasvetovalikkoa.**

| Välilehti | Mitä sisältää | Sivuston osoitteet |
| --- | --- | --- |
| Matches | omat ottelut, oma profiili, päättyneet ottelut | `/bg/top`, `/bg/user/<id>` |
| Lounge | pelitarjoukset, pelaajalista, turnaukset | `/bg/lounge`, `/bg/plist`, `/bg/thall` |
| Discussion | General ja Politics | `/bg/forum2`, `/bg/forum2/politics` |
| Messages | viestijono ja arkisto | `/bg/nextgame`, `/bg/sendmsg` |
| Info | sovelluksen asetukset, peliasetukset, App Help, Connection drops, DG Help, Links | `/bg/profile`, `/help`, `/links.html` |

**Ryhmittelyperuste on mitä pelaaja on tekemässä, ei mistä tieto haetaan.** Siksi
pelaajalista ja turnaukset ovat Loungessa: ne vastaavat samaan kysymykseen kenen kanssa ja
mihin, vaikka ne ovat eri sivuja eri jäsentimillä.

**Viides on Info eikä Settings, ja päätös on Tommin.** Sana tuli Claude Code -sovelluksen
tabulaattoritäydennyksestä, ei Claudelta eikä Tommilta; Tommi tunnisti sen oikeaksi ja
päätti näin (`docs/AVOIMET.md`). Ero ei ole kosmeettinen: välilehti
kantaa myös App Helpin, DailyGammonin oman Helpin ja Links-sivun, eikä Settings kuvaisi
niitä. **DG Help ja App Help pysyvät erillisinä teksteinä**: edellinen on sivuston oma,
jälkimmäinen sovelluksen apu, eikä toista sulauteta toiseen.

**Lauta ei muutu.** Se on yhä porautuminen ottelurivistä eikä välilehti, eikä palkkia
piirretä laudalla. Samaan lajiin tulevat mukaan pelaajaprofiili (`/bg/user/<id>`),
turnaussivu (`/bg/event/<id>`) sekä profiilin `/bg/userevent/<id>` ja `/bg/userwins/<id>`.

**Tournament Hall on välilehden sisällä eikä oma välilehtensä, ja se on mitattu päätös.**
Sivu kaapattiin ennen päätöstä (`raakasivut/tournament_hall.html`): se on koko sivuston
turnauslista, 2848 aktiivista ja 340 päättynyttä riviä, ja Tommin oma käyttäjänimi esiintyy
sillä kerran. Pelaajan oma turnaustieto on `/bg/userevent/<id>`.

**Sisäisen jaon muoto ratkaistaan välilehti kerrallaan**, ja molemmat sallitut vaihtoehdot
ovat näkyviä: segmenttipainikerivi kahdelle tai kolmelle rinnakkaiselle (Discussionin
General ja Politics), listarivi josta porautuu eteenpäin silloin kun kohteet ovat erilaisia
(Infon viisi kohtaa). Alasvetovalikkoa ei käytetä.

**Mitä `DgTabs.kt` tästä joutuu muuttamaan.** `DgTab`in kuudesta arvosta jäljelle jää
viisi: `Lounge` ja `Discussion` säilyvät nimeltään mutta laajenevat sisällöltään,
`Settings` ja `Help` sulautuvat `Info`ksi. `PrimaryScrollableTabRow`in vierittävyys jää
paikalleen varmuudeksi isolla järjestelmäfontilla, vaikka viisi mahtuu puhelimen leveyteen
ilman vieritystä.

### Loungen segmenttirivi ja toteutusjärjestys (päätetty 26.8.2026)

Edellinen osio jätti kaksi asiaa auki ja sanoi että ne ratkaistaan välilehti kerrallaan.
Tämä sulkee molemmat. Muoto ja järjestys ovat Tommin päätöksiä, perustelu on kirjattu
tähän ennen kuin koodiin kosketaan.

**Loungen sisäinen jako on segmenttipainikerivi, ja pelitarjoukset on oletusvalinta.**
Kohteita on kolme (Offers, Players, Tournaments) ja ne ovat eri kokoiset: tarjouslistassa
on lomakkeita, pelaajalista on haku, turnaushallissa on 2848 aktiivista riviä. Erikokoisuus
olisi peruste listariville, mutta se ei kumoa sitä perustetta jolla nämä kolme ylipäänsä
päätyivät samaan välilehteen: ne vastaavat samaan kysymykseen kenen kanssa ja mihin. Se on
juuri se rinnakkaisuus jolla segmenttirivi valitaan. Pelitarjoukset on niistä se jota
katsotaan usein, joten se on valittuna kun välilehti avataan ja pysyy yhden napautuksen
päässä; listarivi olisi vienyt senkin porautumisen taakse.

**Discussionin ja Infon muodot eivät olleet enää auki.** Edellinen osio nimesi ne suluissa
esimerkkeinä: segmenttirivi Discussionin General/Politics-parille, listarivi Infon viidelle
kohteelle. Tämä toteaa sen luettuna eikä päätä uudelleen.

**Toteutusjärjestys on Info, Discussion, Lounge.** Info on ensin koska kuudesta viiteen
-muutos ei voi ohittaa sitä: `Settings` ja `Help` ovat ne kaksi jotka sulautuvat, joten
enumia ei voi kutistaa koskematta niihin. Discussion on toisena koska se on halvin uusi
sisältö: `ForumParser` on olemassa ja Politics on toinen osoite samalle jäsentimelle.
Lounge on viimeisenä koska se on kallein: `/bg/plist` ja `/bg/thall` tarvitsevat kumpikin
oman jäsentimensä eikä kumpaakaan sivua ole vielä kaapattu.

**Info porautuu paikallaan eikä omalle reitilleen.** Sub-ruutu avautuu saman välilehden
sisällä samaan tapaan kuin Discussionin ketju, ja `BackHandler` sulkee ensin sub-ruudun ja
vasta sitten poistuu välilehdeltä. Syy on palkki: omalla reitillään `tabFor` palauttaisi
nullin, palkki katoaisi ja asetusruutu näyttäisi laudalta. `SETTINGS_ROUTE` ja `HELP_ROUTE`
poistuvat siis reitteinä kokonaan.

**Info näyttää vain ne rivit jotka ovat olemassa.** Taulukko lupaa viisi kohdetta, mutta
DG Help (`/help`), Links (`/links.html`) ja peliasetusten erottaminen sovelluksen omista
ovat vielä tekemättä. Rivi jota ei voi avata ei ole rivi, joten listaan tulee nyt kaksi:
Settings ja App Help. Loput lisätään kun niillä on sisältö.

**DG Help ja Links tulivat 29.8.2026.** Sivut kaapattiin (`PageCaptureLiveTest`,
kuluttamattomat GETit Top Pagen navigointiriviltä luettuina) ja jäsentimet ovat
`SiteHelpParser` (h2-osastot, kysymykset `<a name><h3>`-muodosta, vastaukset luettavana
tekstinä) ja `SiteLinksParser` (h2-osastojen taulukot, romurivit ja kommenttirivit
pudotettuina). Rivit avautuvat välilehden sisällä porautumisruudulla (`PageViewModel` +
`PageScreen`), eli laji luetaan vastauksesta samoin kuin profiililla; Links-rivin
napautus avaa laitteen selaimen, koska kohteet ovat ulkoisia sivustoja, ja osoite näkyy
rivillä ennen avausta. Jäljellä viidestä on peliasetusten erottaminen sovelluksen omista.

**Donations kahteen paikkaan 27.9.2026 (Tommin päätös).** Links-ruudun ylin osasto on
sovelluksen oma *DailyGammon*, jossa on yksi rivi: `Donations` ja osoite `/bg/donate`.
Sivuston linkkisivulla sitä ei ole, vaan siihen linkittää vain Helpin FAQ. Sama rivi on
sivuston Helpissä kysymyksen *How can I contribute to the cost of running DailyGammon?*
alla, koska jäsennin litistää vastauksen linkin tekstiksi. Kysymys tunnistetaan sivun
ankkurista (`SiteHelpEntry.anchor == "donate"`) eikä sanamuodosta. Molemmat rivit ovat
`DonationsLinkRow`, sama muoto kuin muilla Links-riveillä (`ExternalLinkRow`).

**Molemmat Donations-rivit poistettu 29.9.2026 (Tommin päätös).** Play-kaupan
maksupolitiikka (*Payments*, luettu 29.9.2026) kieltää ohjaamasta käyttäjää muuhun
maksutapaan kuin Playn laskutukseen, ja se nimeää erikseen sovelluksen napit ja linkit.
Poikkeus koskee verovähennyskelpoisia lahjoituksia. DailyGammonin PayPal-lahjoitus ei ole
sellainen, vaikka raha menee palvelulle eikä kehittäjälle. Politiikka ei sano tästä
tapauksesta suoraan, ja tulkinnan punnitsisi tarkastaja uuden yksityistilin suljetun testin
aikana. Links näyttää nyt vain sivuston omat osastot, ja Helpin lahjoituskysymys näkyy
ilman riviä kuten muutkin. Todennettu tabletilla samana aamuna
(`raakasivut/sessio-29-9-donations/links-ilman-donations-tabletti.png` ja
`help-lahjoituskysymys-tabletti.png`). Ankkurin jäsennys (`SiteHelpEntry.anchor`) jäi paikalleen.
Saman punninnan tulos Ko-fi-linkille: julkisen repon README kyllä, sovellus ja kaupan
listaus ei (`docs/JULKAISU.md`).

### Palkki mitattiin puhelimella, ja mittaus kumosi perustelun (26.8.2026, Pixel 8a)

Kaksi edellistä osiota nojaavat väitteeseen että **viisi välilehteä mahtuu puhelimen
leveyteen kerralla**, ja että vierittävyys jää paikalleen vain varmuudeksi isolla
järjestelmäfontilla. Väitettä ei ollut mitattu puhelimella kun se kirjattiin. Laiteajo
Pixel 8a:lla (1080x2400, tiheys 420, eli leveys 411 dp) kumosi sen normaalillakin fontilla.
Rakenne pysyy, perustelu vaihtuu.

**Mitä mitattiin, kolmena mittauksena.** Ensin pitkillä nimillä (Game Lounge, Message
Archive): näkyvissä oli neljä välilehteä ja **Info jäi kokonaan vierityksen taakse**, eli
juuri se voitto jonka takia kuudesta mentiin viiteen jäi saamatta. Sitten lyhyillä nimillä
`PrimaryScrollableTabRow`illa: sama tulos. Syy ei ole nimien pituus vaan komponentti:
vierittävä palkki varaa 52 dp reunapehmustetta ja **vähintään 90 dp jokaiselle
välilehdelle**, joten viisi vaatii 450 dp eikä mahdu 411 dp:hen millään nimillä.

**Kolme muutosta seurasi, ja kaikki ovat mittauksen sanelemia.**

Palkki on nyt kiinteä `PrimaryTabRow`, joka jakaa leveyden tasan. Se on ainoa muoto jolla
viisi on aina näkyvissä, ja se on samalla se muoto jota edellinen osio ei osannut vaatia.
**Vierittävyyttä ei siis ole enää, eikä sitä voi olla**, joten aiempi lause vierittävyyden
säilymisestä ei ole voimassa.

Nimet lyhenivät taulukon nimistäkin: **Forum eikä Discussion, Inbox eikä Messages.**
Viidellä välilehdellä tilaa on 82 dp kutakin kohden, ja "Discussion" (n. 75 dp) katkesi
kahdelle riville kesken sanan. Ruutujen omat otsikot eivät lyhentyneet: palkin nimi on
navigointia ahtaassa tilassa, ruudun otsikko on sivun nimi. Tommin päätös, kolmesta
vaihtoehdosta (neljä välilehteä viestit Forumin alla, viisi lyhyillä nimillä, palkki
alalaitaan `NavigationBar`iksi).

Välilehden teksti on omassa sisältölohkossaan eikä `Tab`in `text`-parametrissa. Parametri
lisää 16 dp pehmustetta kummallekin puolelle, jolloin tekstille jää 66 dp ja "Matches"
(n. 68 dp) katkesi. Sisältölohko ei lisää pehmustetta. Teksti on yhdellä rivillä ja
katkaisu on kolme pistettä: isolla järjestelmäfontilla nimi ei mahdu, ja lyhennetty nimi on
luettavampi kuin kesken sanan revitty.

**Samalla korjattiin vika jota ei etsitty.** Palkin ja ruudun otsikon välissä oli tyhjä
noin 32 dp:n kaistale jokaisella välilehdellä. `DgTabRow` kuluttaa tilapalkin insetin
omassa modifier-ketjussaan, mutta kulutus ei näy sisarelle, joten jokainen välilehtiruutu
varasi saman insetin toistamiseen. `NavHost` kuluttaa sen nyt silloin kun palkki on
piirretty; laudalla ehto ei päde, koska siellä palkkia ei ole. Otsikko nousi 38 dp ja
otteluluetteloon mahtui yksi rivi lisää.

**Mikä tästä jää opiksi.** Leveysväite on laitekohtainen mitta, ja se kirjattiin kanoniin
ilman mittausta. Palkin muoto oli kirjattu kahdesti ennen kuin kumpikaan kirjaus oli nähty
puhelimella. Vastaava väite kirjataan jatkossa vasta laiteajon jälkeen tai merkitään
mittaamattomaksi.

### Muistutukset sivupaneeliin, mitattuna kahdella laitteella (27.8.2026)

Tommin havainto laiteajossa: laudan toimintorivillä tila loppui kesken `Skip Game`n, ja
`Reminders` sekä `Cube reminder` eivät mahtuneet lainkaan. Hän pyysi harkitsemaan niille
parempaa paikkaa, jossa ne eivät myöskään sekoittuisi DailyGammonin omiin nappeihin.

**Mitattiin ennen kuin päätettiin, ja mittaus muutti kysymyksen.** Ensin luultiin että
mitattu ottelu oli harvinaisen rikas tapaus. Kaapatut lautasivut ajettiin projektin omalla
jäsentimellä läpi ja riville tulevat kohteet laskettiin: 24 sivusta **kolme mahtuu varmasti
puhelimen leveyteen, seitsemän mahtuu vain kun nopat eivät ole pelaajan puolella, ja
neljätoista ei mahdu kummassakaan tapauksessa.** Rivi on siis ahdas oletusarvoisesti, ja
mitattu ottelu oli yksi neljästätoista eikä poikkeus.

**Tabletti mitattiin ennen kuin mitään siirrettiin, ja se oli Tommin vaatimus.** Pixel 8a on
vaakana 914 x 411 dp, Galaxy Tab S7+ 1318 x 824 dp. Tabletilla koko rivi mahtuu ja tilaa jää
noin 70 dp yli, eli **vika on puhelinkohtainen**. Ilman tätä mittausta puhelimen takia olisi
rikottu se mikä toimii isommalla ruudulla.

**Tabletti mahtuu silti vain oletusfontilla.** Aiempi osio (*Nappirivi väistää nopat*)
mittasi samalla tabletilla että `Reminders` peitti nopat `font_scale 1.3`:lla ja
`Cube reminder` `2.0`:lla. Rivi on siis tabletillakin niin täynnä että fonttikoon nosto vie
sen yli. Puhelin on ääritapaus eikä poikkeus, ja siksi ratkaisu on sama molemmilla.

**Päätös: muistutukset siirtyvät vasempaan sivupaneeliin.** Paneeli on sovelluksen oma alue
eikä siinä ole yhtään sivuston komentoa, joten lähteiden ero muuttuu fyysiseksi sen sijaan
että se on värissä ja järjestyksessä. Se vahvistaa 25.8. kirjattua periaatetta (*Sivun
komennot ovat nappeja, sovelluksen omat lisät tekstiä*) eikä heikennä sitä, ja vastaa siihen
mitä Tommi kysyi erikseen: sekoittumista ei voi tapahtua kun napit eivät ole samalla rivillä.

**Vastaväite joka oli olemassa ja johon vastataan.** Toiminnot asuivat sivupaneelissa
10.8.-14.8.2026 ja siirtyivät pois, koska napit ovat *hänen* tekojaan ja hänen vuoronsa on
merkitty sillä puolella keskikaistaa. Se peruste koskee **sivuston vuorosidonnaisia
komentoja**. Muistutus ei ole kummankaan pelaajan teko eikä sidottu vuoroon, joten perustetta
ei voi kääntää tätä vastaan.

**Sivupaneeli ei ole aina näkyvissä, ja se ratkaistaan ilman uutta leveysehtoa.** Paneeli
piirretään vain kun leveyttä jää yli (`SIDE_PANEL_MIN`), ja juuri se oli 14.8. peruste sille
että yksi koti on parempi kuin kolme. Muistutukset ovat siksi paneelissa kun paneeli on, ja
putoavat takaisin nappiriville kun ei ole. **Uutta mitattavaa lukua ei synny**: ehto on sama
`fits`-arvo joka jo päättää paneelin olemassaolon, eikä toista kynnystä ylläpidetä.

**Mikä tämä ei korjaa, ja se sanotaan ääneen.** Muistutukset vievät rivistä noin 170 dp,
mutta pelkkien sivun omien kohteiden pahin tapaus (`Accept | Decline | Verify Accept |
Skip Game`) on yhä puhelimen ahtaimman tilan yli. Sivun omien nappien vieritys jää siis
voimaan puhelimella, eikä tämä muutos lupaa sitä pois.

**Arviointitapa jäi kalibroiduksi.** Fixturekartoituksen leveysarvio (7,7 dp per merkki)
yliarvioi noin 14 prosenttia: kuuden kohteen rivi oli arviolta 688 dp ja tabletilla mitattuna
noin 595. Arvion tehtävä on järjestää tapaukset eikä antaa tarkkaa lukua, ja jos sitä
käytetään uudestaan, kerroin on tässä.

### `Skip Game` muistutusten joukkoon, ja muistutukset alleviivataan (27.8.2026)

Edellinen osio siirsi muistutukset sivupaneeliin ja sanoi ääneen mitä se ei korjaa: sivun
omien nappien vieritys jäi voimaan puhelimella. Tämä sulkee senkin, ja kaksi muutosta ovat
Tommin päätöksiä laiteajossa.

**`Skip Game` siirtyy paneeliin muistutusten joukkoon, ja peruste on hetki eikä lähde.**
Tommi kertoi käyttävänsä sitä vain silloin kun hän haluaa aikaa miettiä mitä kirjoittaa
vastustajalle. Se on siis saman hetken teko kuin muistutus: ottelu siirretään syrjään ja
siihen palataan. Ryhmittelyperuste on sama jolla välilehdetkin ryhmiteltiin, eli mitä
pelaaja on tekemässä.

**Mitattu vaikutus: viimeinenkin ylivuoto katosi.** `Skip Game` vie napin pehmusteineen noin
93 dp, ja kun se lähtee riviltä, jokainen 24:stä kaapatusta lautasivusta mahtuu puhelimen
ahtaimpaan tilaan. Pahin jäljelle jäävä on `Accept | Decline | Verify Accept` noin 290 dp
kun tilaa on 370. Todennettu laitteella: keskikaista päättyy `Verify Doubleen` eikä leikkaa.

**Turvallisuusperuste vahvistuu samalla.** Foorumivalitus jota vastaan rivi on suunniteltu
koskee osumakohtaa: hyväksymisnappi on siinä missä `Roll Dice`, ja sitä tulee painettua
vahingossa. `Skip Game` on kuluttava teko (`/bg/nextgame?skip=`), joten sen siirtyminen kauas
`Roll Dicen` osumakohdasta on sama periaate eikä poikkeus siitä.

**Se pysyy reunustettuna nappina, ja muistutukset ovat tekstiä.** Tommi harkitsi ääneen myös
pelkkää tekstiä ilman erotinviivaa. Väliviiva jäi pois, nappi jäi: `Skip Game` kuluttaa jonon
eikä sitä voi perua, kun taas muistutuksen voi kirjoittaa uudestaan, ja se on sama peruste
jolla `Undo Move` muutettiin napiksi 22.8. **25.8. kirjattu sääntö pysyy siis voimassa
sellaisenaan**: ehto luetaan lähteestä eikä sijainnista, joten sivun oma teko näyttää
napilta myös paneelissa.

**Muistutukset alleviivataan, koska ne ovat linkkejä.** Paneelissa ne olivat samanvärisiä
kuin ottelun staattiset tiedot (`Move 541`, sama `TextSecondary`), eli painettava näytti
samalta kuin luettava. Rivillä ne erottuivat asemansa perusteella, paneelissa ei mikään.

**Väri ei ollut vapaana, ja se on syy alleviivaukseen.** Kontrastit paneelin taustaa
(`PanelBg`) vasten: `TextSecondary` 12,45:1, `TextPrimary` 14,52, `CubeSoft` 10,53, `Accent`
7,61, `CheckerSelf` 14,73. Luku ei kuitenkaan ratkaissut vaan merkitys: `Accent` on samassa
paneelissa staattisella rivillä `21 point match`, `CheckerSelf` on käytännössä sama sävy kuin
`TextPrimary` eli erottumaton `Round`-rivistä, ja `CubeSoft` on **peruuttamattoman teon**
reunusväri, joten se väittäisi muistutuksesta päinvastaista kuin se on. Alleviivaus ei varaa
yhtään väriä eikä kilpaile olemassa olevien merkitysten kanssa, ja väri pysyy ennallaan.

**Uusi merkki tässä sovelluksessa, ja se sanotaan ääneen.** Alleviivausta ei ole käytetty
muualla. Tommin peruste on eksplisiittinen: se kunnioittaa aikaa jolloin linkit olivat
alleviivattuja. Sinistä sävyä harkittiin ja se jäi pois.

**Viiva piirretään itse, ja se on korjaus samana päivänä.** Ensimmäinen toteutus käytti
`TextDecoration.Underline`ia, ja Tommi näki sen laitteella heti: *"Alleviivaus on liian
kapea, sitä on vaikea nähdä mustalla taustalla."* `TextDecoration`in paksuus tulee fontin
metriikasta eikä ole säädettävissä lainkaan, joten ainoa tapa saada se omaksi mitaksi on
piirtää viiva. Paksuus on nyt `LINK_UNDERLINE` eli 1,5 dp.

**Todennettu molemmilla laitteilla.** Tabletilla toteutushetkellä ja Pixel 8a:lla heti perään
(tiheys 340 vastaan 420), ja Tommin vastaus jälkimmäisestä oli että 1,5 dp riittää. Luku on
siis mitattu sillä laitteella jolla vika alun perin nähtiin.

**Väri ei muuttunut, ja se on tässä olennaista.** Ongelma oli paksuus eikä kontrasti:
`TextSecondary` on paneelin taustaa vasten 12,45:1, eli kontrasti oli koko ajan riittävä.
Hiusviiva on vaikea nähdä silloinkin kun sen väri on kirkas, ja se on eri vika kuin
liian vaimea väri. Jos nämä olisi sekoitettu keskenään, korjaus olisi ollut värin
vaihtaminen, ja se olisi vienyt yhden varatun merkityksen turhaan.

### Forum-välilehti: Politics segmenttirivillä (toteutettu 27.8.2026)

Toteutusjärjestyksen toinen kohta (Info, Discussion, Lounge). Muoto oli päätetty
26.8.2026, ja tämä osio kirjaa sen mitä toteutus mittasi.

**Kaappaus tehtiin ennen koodia, ja se kumosi arvauksen.** `/bg/forum2/politics` ei ollut
kertaakaan nähty, ja edellinen osio nojasi siihen että Politics on "toinen osoite samalle
jäsentimelle". Se piti paikkansa vasta yleistyksen jälkeen: **palstan tunnus on osa polkua
eikä vakio `main`**, joten Politicsin ketjut ovat `/bg/forum2/politics/read/<id>` ja
`main`-alkuun sidottu tunnistus olisi hylännyt sivun kokonaan. Vika ei olisi näkynyt
jäsennysvirheenä vaan `NotAForumPage`-huomautuksena, eli sivuna jota ei tunneta. Mitattu
muoto on `docs/KOHDE.md`:ssä, fixture `forum_index_politics.html`.

**Palstat luetaan sivun omalta palstariviltä, ei nimetä koodissa.** `ForumIndex.boards`
on sivun rivi järjestyksessä, ja **näkyvillä oleva palsta on se jolla ei ole polkua**:
sivulla se on lihavoitua tekstiä eikä linkki. Valintaa ei siis pidetä kirjaa erikseen vaan
se luetaan ladatulta sivulta. Sama syy kuin `loungePath`illa ja `forumPath`illa, mutta
tässä se on vahvempi kuin tyyliseikka: **General on kahdella eri osoitteella** riippuen
siitä miltä sivulta katsotaan (`/bg/forum2` General-sivulla, `/bg/forum2/main`
Politics-sivulla), joten koottu polku olisi ollut oikein vain toisesta suunnasta.

Sovellus ei siis tiedä palstoja olevan kaksi. Jos sivustolle tulee kolmas, se ilmestyy
riville itsestään; jos riviä ei tunnisteta, segmenttiriviä ei piirretä ja ruutu on sama
kuin ennen tätä muutosta.

**Segmenttirivi on listan yllä ja `IndexView`n ulkopuolella.** Paikka on toteutuksen oma
päätös eikä muodon muutos: latauksen aikana indeksiä ei ole, joten rivin sisällä oleva
valinta katoaisi juuri sillä hetkellä kun käyttäjä odottaa vaihtamansa palstan
latautuvan. Valinta näkyy siksi heti napautuksesta ja ladattu sivu vahvistaa sen.

**Käyttäjän valinta voittaa yläjuoksun polun.** `forumPathUpstream` tuo aina Top Pagen
oman palstalinkin eli Generalin, ja ilman etusijaa Top Pagen uusi lataus olisi vaihtanut
palstan hiljaa takaisin kesken lukemisen: segmenttirivi olisi sanonut Politics ja Refresh
olisi hakenut Generalin. Etusija on `chosenPath` ja se on testattu.

**Ruudun otsikko ei muuttunut.** Palkin nimi on `Forum`, ruudun otsikko on yhä
`Discussion`, ja palstan nimi näkyy segmenttirivillä sivuston omalla sanalla. Uusia
käännösavaimia ei tullut yhtään, koska General ja Politics ovat sivuston sanoja.

**Mikä jäi mittaamatta.** Politicsin ketjusivua ei ole kaapattu: sen lukeminen nollaisi
New-merkin Tommin tilillä oikealla palstalla, eikä se ole kaappauksen arvoinen hinta kun
rakenne on Generalin kanssa sama kaikelta muulta osin. Jos se osoittautuu eri muotoiseksi,
ruutu sanoo sen `discussion_not_a_thread`illa eikä näytä tyhjää.

**Todennettu laitteella samana päivänä (Pixel 8a, tumma tila).** Segmenttirivi mahtuu
ruudun leveyteen kahdella palstalla ilman katkaisua, valittu puoli on rasti ja täytetty
tausta. Politicsin ketjulista jäsentyi oikein sivustolta: New-merkit, viestimäärät ja
aloittajat ovat rivillä, ja lukutilan lause pysyy alareunassa. Kolme asiaa mitattiin
napautuksina eikä pääteltynä: Politics säilyi valittuna sekä `Refresh`in yli että
välilehtikäynnin (Matches ja takaisin) yli, ja General palautui napautuksella. Logcatissa
ei ollut sovelluksen omia virheitä. Yhtään ketjua ei avattu, koska se olisi nollannut
New-merkin oikealla palstalla.

### Forum-välilehti: uusi ketju ja kommentti (toteutettu 3.9.2026)

**Kaanonimuutos.** Palsta oli lukutilassa Tommin rajauksella 26.8.2026, ja
`DiscussionViewModel`in konstruktori ilman `FormSender`iä oli sen väite. Tommi purki
rajauksen 3.9.2026 (*"Foorumin postaus ja vastaus"*), ja peruste on `SUBSTANSSI.md`:n kohta
34: palsta on hänelle osallistumista, ei ilmoitustaulu. Malli saa nyt `FormSender`in, ja
ruudun alareunan lukutilan lause poistui, koska se olisi ollut väärä.

**Kaksi tekoa, molemmat sivun omilla lomakkeilla.** Uuden ketjun sivu avataan indeksin omasta
`Add a New Thread` -linkistä ja kommenttisivu ketjun omasta `Add a Comment` -linkistä; kumpaakaan
polkua ei koota. Lomake luetaan sivulta (`ForumParser.parseCompose`), ja lähetys on sivun oma
nappi sivun omalla nimellä ja arvolla (`submit=Create New Thread`, `submit=Submit Comment`),
koska sivulla on toinenkin samanniminen nappi (`Preview`) ja palvelin erottaa ne arvosta.
Mitattu muoto on `docs/KOHDE.md`:ssä.

**Hetki ennen lähettämistä on vahvistusdialogi, ja se on kohdan 59 vaatimus.** Palstan viestiä
ei voi muokata eikä poistaa itse, ja se on Tommin ainoa nimetty kaduttava teko. Sovellus ei voi
tarjota perumista, joten se tarjoaa sen ainoan mitä voi: dialogin joka sanoo että viesti menee
julkiseksi omalla nimellä eikä sitä voi jälkeenpäin muuttaa. Nappi on `Post` eikä `Send`,
koska teko on julkaisu.

**Luonnos ei katoa virheeseen.** Otsikko ja teksti asuvat näkymämallissa eivätkä ruudussa,
joten ne selviävät kierrosta; ja epäonnistunut tai vahvistamaton lähetys jättää lomakkeen
auki tekstiineen. Vain onnistunut lähetys tyhjentää sen. Sivuston `Preview`-nappia ei tuoda
sovellukseen: sen vastaussivua ei ole mitattu, ja vahvistusdialogi tekee saman työn.

**Onnistuminen luetaan sisällöstä eikä vastaussivusta**, koska vastaussivua ei ole mitattu.
Kommentin jälkeen ketju haetaan uudelleen (vastaus kelpaa jos se on ketjusivu) ja lähetetyn
tekstin on löydyttävä sen viesteistä; uuden ketjun jälkeen indeksi haetaan uudelleen ja
otsikon on löydyttävä riveiltä. Muuten tila on `Unconfirmed`, ja teksti sanoo ettei mitään
lähetetty kahdesti. POST menee kerran eikä sitä uusita, samasta syystä kuin muuallakin.

**Otsikon raja on sivun oma `MAXLENGTH=80`**, ja se luetaan lomakkeelta eikä kirjoiteta
koodiin. Kentän laskuri näyttää sen. Kommentilla ei ole mitattua rajaa.

**Luonnos on lomakekohtainen, avaimena lomakkeen polku.** Ensimmäinen laiteajo näytti että
yksi yhteinen luonnos vuoti uuden ketjun tekstin kommenttikenttään, joten luonnokset
pidetään polun mukaan: uuden ketjun teksti odottaa uuden ketjun lomakkeella, ja kunkin
ketjun kommentti omallaan. Onnistunut lähetys poistaa vain sen polun luonnoksen.

**Todennettu SM-T970:llä 3.9.2026 lähettämättä mitään.** Listan perässä ovat sivun omat
rivit `Add a New Thread` ja `Old Threads` tässä järjestyksessä, ja lukutilan lause on poissa.
Uuden ketjun lomake avautuu otsikkokentällä ja laskurilla `0 / 80`, `Post` on harmaa kunnes
sekä otsikko että teksti on kirjoitettu, ja `Post` avaa dialogin jonka vahvistusnappi on sivun
oma `Create New Thread`. `Not yet` palautti lomakkeelle tekstiineen. Luetun ketjun (ei
New-merkkiä) lopussa on `Add a Comment`, ja sen lomakkeella otsikko on ketjun otsikko eikä
otsikkokenttää ole. Logcatissa ei sovelluksen virheitä.

**Mikä odottaa Tommia: ensimmäinen oikea lähetys.** Vastaussivua ei ole mitattu kummallakaan
lomakkeella, ja se kaapataan proxyn läpi ensimmäisellä oikealla postauksella
(`raakasivut/LUEMINUT.md` kohta 26). Jos vastaus on suoraan ketju tai indeksi, sovellus lukee
sen ilman toista hakua; muuten se hakee sivun kerran uudelleen.

### Forum-välilehti: Hide ja Show Hidden Threads (toteutettu 22.9.2026)

Tommin tilaus 22.9.2026: *"Forum-molemmille palstoille toteuta Hide-nappi joka riville ja
Show Hidden Threads-nappi alas"*. Molemmat ovat sivun omia linkkejä omilla sanoillaan.

**Hide on rivin oikeassa reunassa tekstinappina.** Se hakee rivin oman `hide`-polun, ja
onnistuminen luetaan uudelleen haetusta indeksistä (`HideUiState`). Onnistunut piilotus ei
kirjoita ilmoitusta, koska rivin katoaminen näkyy jo; odotus näkyy napissa (`Hiding…`), ja
vain virhe tai vahvistamaton tulos saa rivin listan yläpuolelle. Vahvistusdialogia ei ole,
koska sivullakaan ei ole ja piilotettu ketju on yhä luettavissa piilotettujen näkymästä.

**Show Hidden Threads on alalaidan kolmas linkki** Add a New Threadin ja Old Threadsin
rinnalla. Rivi on `FlowRow`, koska kolme nappia ei mahdu puhelimen leveydelle. Näkymä ei
ole oma kerroksensa vaan sivun oma `listhidden`-indeksi: piilotetut rivit ovat mukana ilman
Hide-nappia, ja nappi on nyt `Hide Hidden Threads`. Valinta pysyy kuten palstan valinta,
eli Top Pagen uusi lataus ei palauta tavallista näkymää kesken lukemisen.

**Unhidea ei ole**, koska sivun listoilla sitä ei ole (mitattu 22.9.2026, `docs/KOHDE.md`).
Kuukausiarkiston riveillä ei ole Hide-nappia samasta syystä.

### Lounge-välilehti: kolme kohdetta segmenttirivillä (toteutettu 27.8.2026)

Toteutusjärjestyksen kolmas ja viimeinen kohta (Info, Discussion, Lounge). Muoto oli
päätetty 26.8.2026: segmenttirivi, ja tarjoukset oletusvalintana koska se on se jota
katsotaan usein.

**Molemmat uudet sivut mitattiin ennen koodia**, ja pelaajalista maksoi yhden kaappauksen
(`raakasivut/player_list.html`); Tournament Hall oli kaapattu jo silloin kun siitä
päätettiin. Mitatut muodot ovat `docs/KOHDE.md`:ssä.

**Polut luetaan loungen omista linkeistä**, eivät vakioista: `LoungePage.playerListPath` ja
`tournamentHallPath` tulevat sivun alalaidan linkeistä *List of Players* ja *Tournament
Hall*. Siitä seuraa tilamalli: kumpikin segmentti on `NoPath` niin kauan kuin lounge itse ei
ole latautunut, ja ruutu sanoo sen syyn sen sijaan että näyttäisi tyhjän listan.

**Haku on laiska, ja se on tietoinen ero tarjouksiin.** Tarjoukset haetaan kun välilehti
avautuu, mutta pelaajalistaa ja hallia ei haeta ennen kuin niiden segmentti avataan. Syy on
koko: pelaajalista on sata riviä sivuston kärkeä ja halli oli mittaushetkellä 3188 riviä.
Kummankin sivu pysyy sen jälkeen haettuna kunnes Refresh pyytää uuden, samoin kuin
asetusruudulla. **Refresh koskee sitä segmenttiä joka on auki**, eikä aina loungea.

**Pelaajalistan linkit näytetään sivun omalla sanallaan** (*Sort By Rating*, *Next 100*)
rivinä listan yllä, eikä niitä luokitella lajittelijoiksi ja sivuttajiksi. Ero olisi
luettava linkin tekstistä eli arvattava, ja sivun oma sana kertoo sen paremmin. Sivutus
säilyy myös Refreshin yli: se hakee sen sivun jolla ollaan eikä palaa ensimmäiselle sadalle.

**Hallissa aktiiviset ovat ensin, vaikka sivulla ne ovat toisena.** Se on ainoa kohta jossa
ruutu poikkeaa sivun järjestyksestä, ja syy on lukeminen: aktiiviset ovat se osa jota
katsotaan, eikä kolmensadan päättyneen yli vierittäminen olisi ollut mitään muuta kuin
sivun rakenteen toistoa. Kumpikin otsake kertoo rivimäärän, jotta listan koko näkyy ennen
vierittämistä.

**Molemmat sanovat ääneen mitä lista on.** Pelaajalista ja halli ovat **koko sivuston**
listoja eivätkä pelaajan omia, eikä sitä voi päätellä listasta itsestään: kärkisata näyttää
ranking-listalta ja halli näyttää turnauslistalta. Pelaajan oma turnaustieto on profiilin
`/bg/userevent/<id>`:ssä, joka on eri sivu eikä vielä toteutettu.

~~**Mitä tähän ei tehty.** Pelaajahaku (`POST /bg/plist`, kenttä `like`) on loungen oma
lomake, ja se jäi pois: se lähettäisi jotain, ja lähettäminen on oma päätöksensä samoin
kuin Tournament Sign-Up. Ilman hakua pelaajan löytäminen listalta on sadan rivin sivujen
selaamista, eli sivun oma rajoite eikä sovelluksen.~~ **Tehty 3.9.2026, ks. alla.**

#### Pelaajahaku ja pelaajakortti (3.9.2026)

Tommin tilaus: *"toteuta pelaajahaku, pelaajakortti ja sieltä ensimmäisen quick messagen
lähetys"*. Kolmesta osasta kaksi oli jo olemassa: pelaajakortti on profiilisivu
(`PageScreen`, `PageContent.Profile`) ja pikaviestin lähetys sieltä on 29.8.2026 reitti.
Puuttuva osa oli haku, ja se tehtiin loungen omalla lomakkeella.

- **Hakukenttä Players-segmentin yläreunassa**, `Name starts with` ja `Search`. Lomake
  luetaan loungesta (`LoungePage.searchForm`, `PlayerSearchForm`): osoite, metodi,
  kentän nimi ja piilokenttä `type=name` ovat sivun omia. Kenttä on ruudulla vain kun
  lounge on luettu ja sillä on lomake. Nappi on pois päältä tyhjällä kentällä, koska
  tyhjää hakua ei lähetetä. Haku on lähetys muttei peruuttamaton teko, joten vahvistusta
  ei kysytä ja Refresh saa toistaa sen.
- **Tulos on pelaajalista** (mitattu, `docs/KOHDE.md`), joten se kulkee samalla
  jäsentimellä ja samassa tilassa kuin sivun oma lista. Alarivi sanoo mitä haettiin ja
  että rivin napautus avaa profiilin, jolta pikaviesti lähetetään. `Show all` palaa sivun
  omaan listaan; sivun omat *Sort By* -linkit vievät listaan myös hakutuloksen sivulta,
  joten linkin napautus päättää haun.
- **Pelaajakortti on profiili**: nimi, rating ja kokemus, `Message`-nappi, turnauslinkit,
  aktiiviset ja päättyneet ottelut. `Message` avaa kentän, ja lähetys on siellä sama
  `POST /bg/sendmsg/<id>` kuin ennenkin.

*Laiteajo 3.9.2026 proxyn läpi, mitään lähettämättä paitsi haku:* `ackammon` löytyi yhdellä
rivillä, rivi avasi profiilin (`Rating 1500, Experience 0`, `Message`, ottelut tommihia
vastaan). ~~**Pikaviestiä ei lähetetty**, koska se on peruuttamaton teko ja Tommin ele; kentän
lopputilat (`Sending`, `Sent`, `Unconfirmed`) ovat yhä yksikkötestien varassa kuten
29.8.2026 kirjattiin.~~

**Ensimmäinen pikaviesti lähetettiin profiililta 3.9.2026, ja Tommi painoi napin.** Tämä oli
se lopputila jota 29.8.2026 kirjaus jätti yksikkötestien varaan. Ruudulla luki lähetyksen
jälkeen `Sent, and saved to your message archive: DailyGammon does not keep it.`, kenttä
tyhjeni (`0 characters`) ja `Send message` jäi näkyviin uutta viestiä varten. Reitti
Lounge → Players → haku → nimi → `Message` → `Send message` on siis kuljettu päästä päähän
oikealla sivustolla, ja `Sent`-tila on nähty eikä vain testattu. `Unconfirmed` on yhä
näkemättä, ja se on hyvä asia: se syntyy vain kun sivusto vastaa oudosti. Arkisto todennettiin
heti perään Inboxista: ylimpänä rivi `tommih`, `Quick message`, `3 Sep 2026`, runko `Abc`,
eli lähetetty viesti kirjautui omana viestinä kuten `PageViewModel.confirm` lupaa.
**Perillemeno todennettu ackammonin puolelta** (Tommi 3.9.2026: *"viesti meni perille"*).
Samasta lähetyksestä tuli yksi havainto: fraasirivi puuttui profiilin viestikentästä, ja se
lisättiin samana yönä (ks. *Viestin lähetys pelaajan profiililta*).

**Todennettu laitteella samana päivänä, ja mittaus löysi yhden vian (Pixel 8a).**
Segmenttirivi mahtuu kolmella kohteella ilman katkaisua, pelaajalista ja halli latautuvat
vasta avattaessa, ja hallin otsake näytti 2845 aktiivista turnausta. Sivutus todennettiin
napautuksena: *Next 100* vaihtoi listan sijoituksiin 101 alkaen, ja **sivun omiin
linkkeihin ilmestyi silloin *Previous 100*** jota ensimmäisellä sivulla ei ollut. Se on
samalla todiste siitä että linkit tulevat sivulta eivätkä koodista.

**Vika oli linkkirivi, ja se näkyi vain laitteella.** Kolme *Sort By* -linkkiä täyttävät
411 dp:n leveyden, joten `Row`issa **`Next 100` jäi kokonaan ruudun ulkopuolelle**: juuri se
linkki jota ilman sadan rivin listaa ei voi selata lainkaan. Rivi on nyt `FlowRow`, joka
rivittää linkit sen sijaan että leikkaisi ne. Sama vikaperhe kuin välilehtipalkin 90 dp:n
minimileveys: teksti mahtuu testissä ja katoaa ruudulla, eikä logcat sano siitä mitään.

### Porautumiset: profiili, turnaus ja pelaajan turnauslistat (toteutettu 27.8.2026)

Viimeinen erä 26.8.2026 kartoituksesta: `/bg/user/<id>`, `/bg/event/<id>`,
`/bg/userevent/<id>` ja `/bg/userwins/<id>`. Kartoitus nimesi nämä **porautumisiksi
rivistä eikä välilehdiksi**, ja tämä osio toteuttaa sen.

**Yksi reitti ja yksi näkymämalli neljälle sivulle, ja laji luetaan vastauksesta.** Se ei
ole säästö vaan tämän projektin mitattu perusfakta sovellettuna navigointiin: pyydetty
osoite ei kerro mitä saatiin (`/bg/move/`-pyyntö palautti kerran 200 OK:lla sivun jolla ei
ollut lautaa). `PageViewModel` hakee polun ja kysyy `DgPages`ilta mikä sivu se on, joten
sivuston yllätys näkyy huomautuksena eikä tyhjänä listana. Reitti kuljettaa vain polun,
jonka kutsuja on lukenut sivun linkistä.

**Profiililta porautuu 27.8.2026 illasta alkaen myös pelilistaan ja versus-suodattimeen.**
Toisen pelaajan parametriton profiili ei kanna ottelutaulukoita, ja sivun omat
lajittelulinkit ovat ainoa reitti niihin (mitattu, fixture `profile_page_games.html`).
Ruutu näyttää *Show games* -linkin kun taulukot puuttuvat, ja *Matches versus you* -linkin
aina kun sivu tarjoaa sen; molemmat ovat sivun omia linkkejä sellaisinaan ja avaavat saman
porautumisreitin, koska tulossivu on sama profiilimuoto. Tyhjän listan varateksti jää
näkyviin vain jos sivulla ei ole lajittelulinkkiäkään. Tausta ja purettu rajaus:
`docs/AVOIMET.md` välilehtikohta.

**Sisäänkäyntejä on viisi, ja jokainen on sivun oma linkki.** Otteluluettelon *Signed in
as* -rivi avaa oman profiilin (ja on siten ainoa reitti niihin otteluihin joita Top Page ei
näytä), Loungen tarjousrivin nimi avaa tarjoajan profiilin, pelaajalistan rivi avaa
profiilin, ja sekä Loungen Sign-Up-rivi että hallin rivi avaavat turnaussivun. Profiililta
porautuu eteenpäin turnauslistoihin ja ottelurivin tapahtumasta turnaukseen.

~~**Turnauskaavio luetaan kierroksittain, ja se on ainoa kohta jossa muoto vaihtuu.** Sivu on
`ROWSPAN`-ruudukko eli työpöydän cup-kaavio, joka ei käänny puhelimen leveyteen. Ruutu
näyttää kunkin sarakkeen omana listanaan: kierros kerrallaan, ylhäältä alas. **Pariutus ei
näy, eteneminen näkyy**, ja se sanotaan ruudulla ääneen sen sijaan että annettaisiin
ymmärtää kaavion olevan kokonaan mukana.~~

**Turnauskaavio piirretään kaaviona 3.9.2026 alkaen (Tommi: *"turnausten kaaviot nyt mukaan
sovellukseen"*).** Kierroslistat olivat rajaus puhelimen leveyden takia, ja rajaus purettiin
vierityksellä eikä tiivistämällä: kaavio vierii vaakaan sarakkeittain ja pystyyn sivun mukana.
Ruudukko on sivun oma. `EventParser` lukee jokaiselle solulle ylimmän rivin (`EventEntry.top`)
ja sarakkeelle solukorkeuden (`EventRound.span`, sivun `ROWSPAN`), ja `BracketGrid` asettaa
solut niiden mukaan. **Pariutus näkyy viivoista, jotka piirretään ruudukosta eikä
merkinnöistä**: sarakkeen paikka liittyy niihin edellisen sarakkeen paikkoihin joiden rivit
se kattaa, olipa paikassa pelaaja tai ei. Tyhjä paikka ensimmäisellä kierroksella on
vapaakierros ja myöhemmällä tuleva ottelu, ja kumpikin näkyy tyhjänä kuten sivulla. Solun
napautus avaa pelaajasolusta profiilin ja ottelusolusta siirtolistan; ratkennut ottelu on
lihavoitu ja kesken oleva kursiivilla, sivun omalla merkintätavalla. Mitat ovat rivi 36 dp,
sarake 172 dp ja viivakaista 14 dp, joten 16 pelaajan kaavio on 576 dp korkea ja viisi
saraketta 860 dp leveä, eli tabletilla vaakaan kokonaan näkyvissä ja puhelimella vieritettävä.

**Nimen laatikko on korkeintaan kahden rivin korkuinen ja solun keskellä.** Ensimmäinen
laiteajo näytti miksi: `December 25 Championship` on 9 kierrosta ja 512 paikkaa, joten
viidennen kierroksen solu on 16 riviä eli 576 dp, ja täyteen venytetty laatikko näytti
taulukolta eikä kaaviolta. Liitosviivat osuvat solun keskelle, joten keskitetty laatikko pysyy
niissä kiinni. Ruudukon korkeus on silti sivun oma, 512 riviä, ja tulevat kierrokset ovat
tyhjää viivastoa oikealla; se on sama näkymä kuin sivustolla eikä sitä tiivistetä.

**Oma tunnus korostuu tekstin värillä (Tommi 3.9.2026: *"pelaaja itsensä tulisi korostaa
kaaviosta tekstin värillä"*).** Solun teksti on primary-värinen kun sen nimi on kirjautuneen
tunnus, kirjainkoosta piittaamatta. Tunnus tulee `PageViewModel.selfName`stä, joka on sama
lähde kuin arkistoinnin `self` (kirjautumislomakkeen tunnus). Väri on tekstissä eikä
taustassa, koska tausta on solun laji, ja huomautusrivi kaavion alla sanoo sen.

**Todennettu SM-T970:llä 3.9.2026:** loungen Tournaments-rivi avasi turnauksen, kaavio
piirtyi viidellä näkyvällä sarakkeella ja vieri vaakaan kierroksiin 6–9 ja Winneriin,
ratkennut solu oli lihavoitu ja kesken oleva kursiivilla, ja ratkenneen solun napautus avasi
saman ottelun siirtolistan (`December 25 Championship, Round 3`). Korostus todennettiin
`Weekday Warriors #4559`:ssä (64 paikkaa): `tommih` oli primary-värinen ensimmäiseltä
kierrokselta viidennelle asti ja muut nimet valkoisia. Logcatissa ei kaatumisia.

**Profiilin kentät ovat tekstinä eivätkä linkkeinä.** Ne ovat pelaajan itsensä
kirjoittamia, ja mitattu esimerkki kertoo miksi tämä on kirjattu: yhden pelaajan `Real
Name` oli lista YouTube-linkkejä. Ruutu on lukutilassa eikä avaa ulkoisia osoitteita.
Samasta syystä toisen pelaajan profiilin kutsulomaketta ei ole: se lähettäisi jotain.

**Tyhjä ottelulista selitetään.** Toisen pelaajan profiililla ei ole ottelutaulukoita
lainkaan, koska osoitteessa ei ole `active`- eikä `finished`-parametria. Ruutu sanoo sen
lauseena; ilman sitä sivu näyttäisi siltä kuin pelaajalla ei olisi otteluita.

**Kaksi vikaa jotka näkyivät vasta laitteella (Pixel 8a 27.8.2026).** Ensin
**välilehtipalkki väitti väärää**: porautumisruutu ei ole välilehti, joten `tabFor`
palautti nullin ja `DgTabRow` korosti varakohdettaan Matchesia, myös silloin kun profiili
avattiin Loungesta. Palkki on nyt pois samoin kuin laudalla. Sen korjaus paljasti heti
toisen: **otsikkorivi piirtyi kellon ja akun päälle**, koska tilapalkin insetin kulutti
juuri se palkki jota ei enää piirretä. Insetti annetaan nyt ruudulle itselleen samoin kuin
laudalla. Jälkimmäinen on sama vikaperhe kuin 26.8.2026 löytynyt tyhjä kaistale, eli
insetin omistajuus vaihtuu aina kun palkin näkyvyysehto muuttuu.

**Todennettu laitteella:** oma profiili (rating, kentät, 44 käynnissä olevaa ottelua),
tournament wins -lista, turnaussivu jonka kaavio eteni 16, 8, 4, 2, 1 ja voittajana
`tommih`, sekä pelaajalistalta avattu toisen pelaajan profiili jolla ottelutaulukoita ei
ole. Sovelluksen omia virheitä ei logcatissa ollut.

### Ottelun chat laudan yläpuolella (toteutettu 27.8.2026)

Siirron jälkeinen sivu kantaa vastustajan viestin, ja **sivusto ei säilytä sitä**. Kortti on
siksi laudan yläpuolella eikä alla: sillä hetkellä kun se on olemassa, se on ruudun tärkein
sisältö. Lauta antaa tilaa, koska se on painollinen laatikko ja kortti ei.

Kortissa on neljä osaa: vastustajan viesti sellaisenaan, kirjoituskenttä, lainausvalinta ja
nappirivi. Kolme ensimmäistä ovat tavanomaisia, neljäs ei.

**Napit ovat sivun omia (`Next Game`, `To Top`) eikä sovelluksen `Send`.** Syy on että
lähetys ja vuoron päättäminen ovat sivustolla sama teko: sama lomake vie viestin ja siirtää
eteenpäin. `Send`-nappi olisi siis valhe kahdesti, ensin siitä että viesti lähtisi yksin ja
sitten siitä että ruutu jäisi paikalleen. Kortin selite sanoo saman sanoina, koska nappien
tekstit yksin eivät kerro että viesti lähtee mukana.

Kumpaa nappia painetaan, on käyttäjän valinta eikä sovelluksen. `Next Game` siirtää jonossa
eteenpäin ja `To Top` poistuu laudalta, eikä kumpikaan ole turvallisempi oletus kuin toinen.

**Lainausvalinta on päällä silloin kun sivu piti sitä päällä**, ja se luetaan lomakkeelta
eikä oleteta koodissa. Juuri se valinta tekee edellisestä viestistä näkyvän vastaanottajalle
(`docs/AVOIMET.md`), joten sovellus ei saa hiljaa pudottaa sitä; purkaminen on käyttäjän oma
ele. Ruudutonta lomaketta ei voi lainata, ja silloin valintaa ei myöskään näytetä.

~~**Lähetysnappi on pois käytöstä kun kenttä on tyhjä.** Sivun omat napit ilman viestiä ovat
laudan tavallisella toimintorivillä, eikä tämä kortti ole toinen tie samaan tekoon.~~
**Kumottu 27.8.2026 laiteajossa, ks. alla.**

Luonnos ja lainausvalinta nollautuvat kun sivu vaihtuu, koska niiden avain on sivun oma
lomakeosoite: seuraavan ottelun kenttään ei jää edellisen ottelun tekstiä.

~~**Todentamatta:** kortin sijoitusta ei ole nähty laitteella.~~ **Todennettu 27.8.2026
tabletilla oikealla viestillä.** Kortti näkyi, vastustajan viesti luettiin siitä, arkistoon
tuli ensimmäinen `GAME_MESSAGE`-rivi ennen kuin mitään painettiin, ja sovelluksesta lähetetty
vastaus saapui vastustajalle lainauksineen. Kapealla ruudulla sijoitusta ei ole yhä nähty.

### Kortti omistaa teon: laudan nappirivi piiloon chatin ajaksi (Tommin päätös 27.8.2026)

Laiteajo näytti saman nappiparin kahdesti: `Next Game` ja `To Top` kortissa harmaina ja laudan
keskellä aktiivisina. Se on mitattu tilanne eikä hypoteesi, ja Tommin päätös on **tuplanapit
pois, ja kortti omistaa teon**.

**Laudan oma toimintorivi ei piirrä sivun lomakkeen nappeja silloin kun chat-kortti on
näkyvissä.** Rivi ei menetä mitään: vuoron päättävällä sivulla lomakkeen ainoat napit ovat
juuri nuo kaksi. `Skip Game` ja sovelluksen omat lisät jäävät riville, koska ne eivät ole
lomakkeen tekoja.

**Ja kortin napit ovat aktiivisia myös tyhjällä kentällä.** Tämä kumoaa yllä yliviivatun
kohdan, ja peruste on sivustolla: selaimessa tyhjä kenttä ja `To Top` tarkoittaa poistumista
ilman viestiä. Vanha perustelu nojasi siihen että laudan rivi tarjoaa saman teon ilman
viestiä, ja kun rivi piilotetaan, perustelu katoaa mukana. Ilman tätä muutosta kortti estäisi
poistumisen silloin kun ei ole mitään sanottavaa, eli olisi tiukempi kuin kohde.

**Sivutuote joka on syytä kirjata, koska se on toisen asiakkaan mitattu virhe.** Tommi
27.8.2026 DG Mobilesta: *"viestien kirjoitus oli vaikeaa, koska chat-ikkunan ulkopuolinen
klikkaus hävitti chat-ikkunan."* Tämä kortti ei ole dialogi vaan ruudun oma sisältö, joten
ulkopuolinen napautus ei voi hävittää sitä eikä kirjoitettu luonnos katoa vahingossa. Se on
tällä hetkellä rakenteen sivutuote; tästä eteenpäin se on vaatimus, eli korttia ei saa
myöhemmin muuttaa dialogiksi tai alalehdeksi joka sulkeutuu ulkopuolisesta eleestä.

### Paluuele on turvallinen uloskäynti vuoron päättävältä ruudulta (mitattu 27.8.2026)

Lautaruudulla ei ole omaa paluunappia (9.8.2026, Tommin valinta), ja chat-kortin myötä
heräsi epäily että järjestelmän paluuele jättäisi siirron viimeistelemättä: sivun lomakkeessa
on `commit=1`, ja siitä oli helppo päätellä että `Next Game` tai `To Top` on pakollinen.

**Mitattu tulos on päinvastainen.** Tommi poistui paluueleellä painamatta nappia, ja siirto
oli sivuston omassa kirjanpidossa. Siirto committoituu `Submit Move`illa, ks. `docs/KOHDE.md`.
Kortin napit ovat siis viestin lähetys ja navigointi, eivät vuoron viimeistely, eikä ruudulle
tarvita varoitusta poistumisesta.

**Mitä tästä ei seuraa.** Kortin nappeja ei saa tämän perusteella piilottaa tai tehdä
vaikeammin löytyviksi: ne ovat yhä ainoa tapa lähettää viesti, ja viesti katoaa sivustolta
lukuhetkellä.

### Palstan kuukausiarkisto on kolmas kerros (29.8.2026)

**Tommin havainto laitteelta:** *"Molemmat foorumit discussion ja politics näyttävät vain
uusimmat, pitäisi olla siirtymä vanhempiin viesteihinkin."* Havainto oli oikea eikä se ollut
jäsennysvika: sivustolla indeksi **on** kuluvan hetken lista, ja vanhempaan pääsee vain sivun
omalla `Old Threads` -linkillä, jota sovellus ei tarjonnut missään.

**Kerroksia on nyt kolme, ja ne sulkeutuvat sisältä ulos.** Indeksin päälle avautuu arkisto ja
sen päälle ketju; järjestelmä-back sulkee ketjun, sitten arkiston, ja vasta sitten poistuu
välilehdeltä. Arkisto avautuu **indeksin päälle eikä sen tilalle**, joten paluu ei hae
indeksiä uudelleen — sama valinta kuin ketjulla, ja samasta syystä.

**Sisäänkäynti on listan perässä eikä otsikkorivillä.** `Old Threads` on viimeisenä rivinä
ketjulistassa, koska se on listan jatke ja koska sivustollakin se on siinä kohtaa sivua.
Otsikkorivi olisi nostanut arkiston samanarvoiseksi `Refresh`in kanssa, mitä se ei ole.

**Kuukaudet ovat vaakarivinä uusin ensin.** Niitä on 272 (`docs/KOHDE.md`), joten pystylista
olisi vienyt koko ruudun ja pudottanut ketjut näkyvistä. Rivi on pitkä tarkoituksella: lähin
menneisyys on ilman vieritystä, ja kauas pääsee vierittämällä. Valittu kuukausi luetaan
**sivun omasta otsikosta** eikä pidetä kirjaa itse, koska `/month` ilman vuotta ja
`/month/2026/8` ovat sama kuukausi eikä polkujen vertailu tunnistaisi sitä.

**Palstan vaihto sulkee arkiston.** Arkisto kuuluu palstaan, ja toisen palstan kuukausisivu
jäisi muuten ruudulle väärän segmentin alle näyttäen eri palstan ketjuja.

**Ketjurivi on sama kummassakin listassa**, ja aika tulee mukaan vain kun se on olemassa.
Indeksissä aikasaraketta ei ole lainkaan, joten rivi näyttää siellä täsmälleen entiseltä.

Lukurajaus ei muutu: arkisto on lukua siinä missä indeksikin, ja `Reading only` -rivi on sen
alalaidassa samoin kuin listalla. Todennettu laitteella 29.8.2026 koko ketju läpi: indeksi →
`Old Threads` → maaliskuu 2026 → ketju → back palasi maaliskuuhun eikä indeksiin.


### Alleviivaus on lupaus, väri ei (29.8.2026)

Tommin havainto: *"mustalla taustalla ainakin pelkkä valkoinen teksti on tosi monotonista
luettavaa - värit ja ehkä alleviivaukset linkkeihin piristäisi ilmettä."* Havainto on oikea,
mutta se jakautuu kahteen eri sääntöön eikä yhteen.

**Väri on rakennetta.** Osastootsikot (`SectionHeading`, `PageHeading`) ovat nyt
primary-värillä, eli sama lämmin puusävy jota `Refresh` ja `Back` jo käyttävät. Se erottaa
osaston sen omista riveistä ilman että lupaa mitään: otsikkoon ei voi napauttaa.

**Alleviivaus on toimintaa, ja siksi sitä ei saa panna kaikkeen mikä näyttää linkiltä.**
Alleviivattu teksti lupaa että napautus vie jonnekin. Linkkisivun rivin otsikko on
alleviivattu, koska rivi todella avaa selaimen.

**Pelaajien kirjoittamia osoitteita ei alleviivata eikä avata**, vaikka ne ovat tekstinä
näkyvissä palstan viesteissä ja profiilin kentissä. Tämä ei ole tyylivalinta vaan vanha
päätös samassa dokumentissa: profiilin kentät ovat pelaajan itsensä kirjoittamia, ja mitattu
esimerkki on pelaaja jonka `Real Name` oli lista YouTube-linkkejä. Ero linkkisivuun on se
**kuka osoitteen takaa vastaa**: linkkisivu on sivuston oma kuratoitu lista, viestin osoite
on kenen tahansa kirjoittama. Napautettavaksi tekeminen olisi sovelluksen suositus.

Rajaus joka pätee kumpaankin: **lautanäkymä jää ennalleen**, koska sen värit ovat mitattu
kokonaisuus (X-22).

### Välilehdellä on oma sävy, ottelulistalla ei (29.8.2026)

Tommin valinta kolmesta vaihtoehdosta: *"kolmonen ilman ottelulistaa."* Jokainen välilehti
saa oman aksenttivärinsä, paitsi otteluluettelo joka pitää teeman perusprimaryn eli saman
kehyspuun kuin ennen.

**Ottelulistan poikkeus on peruste eikä laiskuus.** Sen rivit ovat jo värillisiä siellä missä
väri merkitsee jotain: `MatchClock` maalaa nollaan tulleen gracen tertiaryllä ja vähiin
käyneen poolin errorilla. Kolmas sävy samalla rivillä kilpailisi niiden kanssa, ja juuri ne
kaksi ovat listan ainoat värit joilla on merkitys.

**Toteutus korvaa `primary`n eikä lisää uutta väriä.** Ruudut käyttivät jo primarya kaikkeen
mikä on aksenttia (`Refresh`, `Back`, osastootsikot, `Old Threads`, linkkisivun alleviivaus,
palkin korostus), joten yksi arvo riittää eikä yhtäkään ruutua tarvinnut muuttaa. `onPrimary`
korvataan mukana, ja se on mitattu vika eikä varmuuden vuoksi tehty: ilman sitä Loungen
`Join` oli vihreä nappi jossa luki oletuspaletin tummanvioletti teksti.

**Sävyt on mitattu.** Kontrastit tummaa `#1C1B1F` ja vaaleaa taustaa vasten ovat
`DgTheme.accentFor`in taulukossa; matalin on 5,45:1 ja AA-raja 4,5:1. **Vaalean sarakkeen
luvut mitattiin uudestaan 6.9.2026**, kun tausta vaihtui oletuksesta `#FEF7FF` omaan
`#FBF8F4`:ään; sävyt eivät muuttuneet, ja jokainen luku laski neljä sadasosaa. Napin teksti tummaa
aksenttia vasten on 8,19–9,14:1.

**Väri ei ole ainoa merkki.** Vihreä ja turkoosi (Lounge, Info) sekä sininen ja violetti
(Discussion, Messages) voivat lähestyä toisiaan värisokealla lukijalla. Välilehdellä on aina
myös nimi, joten sävy on vahvistus eikä tunniste.

**Palkki on viisivärinen aina** (Tommin päätös samana päivänä: *"viisivärinen palkki oli
juuri sitä mitä halusin"*). Jokainen nimi on omalla sävyllään myös valitsemattomana, joten
väri kertoo **mikä välilehti on kyseessä** eikä sitä missä ollaan. Ensimmäinen toteutus teki
päinvastoin — koko palkki nykyisen välilehden sävyssä — ja silloin sävyn näki vasta käymällä
välilehdellä.

Kaksi seurausta joita tämä vaati. **Valintaa ei voi jättää värin varaan**, koska värejä on
viisi eikä yksikään niistä ole "valittu": valitun erottaa lihavointi ja korostusviiva.
Ja **perusprimary on luettava skeemasta eikä `MaterialTheme`sta** (`tabLabelColor`), koska
palkki piirtyy `DgTabAccent`in sisällä; ilman sitä otteluluettelon nimi olisi ottanut
naapurinsa värin sen mukaan millä välilehdellä ollaan.

**Ruudun otsikko on samaa sävyä** (Tommi 29.8.2026). Sääntö asuu yhdessä paikassa
(`dgTopAppBarColors`) eikä kahdeksassa ruudussa, jotta otsikot eivät pääse eriytymään
toisistaan.

**Väliotsikot eriytyivät silti kerran, ja se näkyi vasta laitteella.** Ensimmäinen väripass
kattoi ne kaksi ruutua joilla on jaettu otsikkokomponentti (`SectionHeading`, `PageHeading`),
mutta Settings, manuaali ja viestiarkisto kirjoittavat väliotsikkonsa omina `Text`-kutsuinaan.
Ne jäivät valkeiksi, ja ero näkyi heti kun asetusruutu avattiin. Nyt kaikki väliotsikot ovat
sävyä.

**Erottelu jonka tämä pakotti sanomaan ääneen:** sävyn saa **osaston** otsikko, ei sisältörivin
otsikko. Ottelun tapahtumanimi, viestin lähettäjä, asetusvaihtoehdon nimi ja Infon rivien nimet
ovat samaa typografista tasoa mutta ne ovat sisältöä, eivät rakennetta. Jos nekin värjättäisiin,
väri lakkaisi erottamasta mitään. Vain otsikko värjätään: nappien väri tulee jo primarysta, ja tausta sekä ikonit
jäävät Material 3:n oletuksiin.

Lauta ja porautumisruutu jäävät perusteemaan, koska ne eivät ole välilehtiä.

**Vaalea tila on nähty ruudulla 29.8.2026**, ei vain laskettu. Tabletti käännettiin
valoisaan tilaan (`cmd uimode night no`) ja palautettiin heti perään yötilaan. Viisi sävyä
erottuvat toisistaan myös vaalealla taustalla, ja otsikko sekä `Refresh` seuraavat sävyä
samoin kuin tummassa. Tämä on se puoli jota mittaus ei olisi kertonut: kontrastiluku sanoo
että teksti erottuu taustasta, ei sitä että viisi sävyä erottuvat toisistaan.

### Viestin lähetys pelaajan profiililta (toteutettu 29.8.2026)

Tämä on **keskustelun aloittaminen ilman ottelua**, ja se on eri asia kuin ottelun chat.
Ottelussa aloittaminen on ollut olemassa 27.8.2026 alkaen (`Message`-linkki laudan
sivupaneelissa), ja se kattaa vain ne pelaajat joiden kanssa on vuoro kesken. Profiilin oma
`Quick message` -lomake on ainoa reitti sanoa jotain silloin kun ottelua ei ole tai kun
vuoro on vastustajalla.

**Ruutu ei ollut enää lukutilassa kauttaaltaan, ja se kirjattiin kumoamisena.**
`PageViewModel`in oma kuvaus väitti nimeltä ettei konstruktorissa ole `FormSender`iä ja
ettei yksikään sen kuudesta sivusta voi tehdä sivustolla mitään. Väite oli tosi ja se
poistettiin tietoisesti; sama muutos tehtiin `LoungeViewModel`ille 26.8.2026, ja
molemmissa kumoaminen on kirjoitettu auki eikä pyyhitty pois.

**Kyky on yksi teko eikä lähetyskyky**, ja ero on rakenteessa: `send` ottaa lomakkeen
ladatusta profiilista eikä parametrina, ja `ReplyForm` syntyy vain jäsentimessä. Ruutu ei
siis voi koota osoitetta eikä lähettää sivun muita lomakkeita. Profiilisivulla on kolme
lomaketta, ja kaksi muuta (`/bg/invite/new`, `Ignore`) ~~ovat yhä ulkona rajauksena~~
tulivat mukaan 3.9.2026 omina nimettyinä tekoinaan (`invite`, `ignore`), ks. *Ottelukutsu ja
sivuutus profiililta*. Väite "yksi teko" ei siis enää päde, mutta rakenne pysyy: jokainen
lomake syntyy vain jäsentimessä ja elää vain ladatussa profiilissa.

**Lomake haetaan osoitteesta eikä järjestyksestä**, ja se on tässä oikea vika joka ei
sattunut: kutsulomake on sivulla **ennen** viestilomaketta, joten "ensimmäinen lomake"
olisi lähettänyt tekstin ottelukutsuna. Kutsulomakkeen oma tekstikenttä on `comment`, eli
sekaannus ei olisi kaatunut mihinkään vaan tuottanut väärän teon. Fixture
`profile_page_other.html` kantaa kaikki kolme lomaketta juuri siksi, ja kaksi testiä
väittää valinnan osoitteesta.

**Kenttä ei ole näkyvissä itsestään** vaan avataan `Message`-napista, sama ratkaisu kuin
lautaruudun chat-kortilla. Kaksi perustetta: profiili on ensisijaisesti luettava sivu
(rating, rivistö, ottelut), eikä peruuttamattoman teon kenttä kuulu ruudulle jolle tullaan
katsomaan lukuja. Avaaja on silti lähellä yläreunaa eikä listojen hännillä, koska toisen
pelaajan sivulla se on se teko jota varten sinne tullaan.

**Avaaja oli ensin linkki listojen keskellä, ja Tommi löysi sen vasta etsimällä
(29.8.2026).** Havainto oli kaksiosainen: *"vei hetken löytää Messages eikä se ollut nappi"*.
Molemmat puolet olivat rikki jo kirjattua sääntöä vastaan.

*Ulkoasu.* `Skip Game` -kohta sanoo että **sivun oma teko näyttää napilta ja ehto luetaan
lähteestä eikä sijainnista**. `Message` on POST sivustolle, mutta se piirtyi `LinkRow`illa eli
täsmälleen samannäköisenä kuin `Active tournaments`, `Tournament wins`, `Show games` ja
`Matches versus you`, jotka ovat pelkkää porautumista. Peruuttamaton teko oli siis
neljän navigointilinkin näköinen. Nyt se on reunustettu nappi, samasta syystä kuin `Skip Game`
on nappi muistutusten seassa.

*Sijainti.* Kuvaus lupasi "lähellä yläreunaa", mutta koodissa avaaja oli **rivistön alapuolella**
ja rivistön pituus on pelaajan oma valinta. Lupaus piti siis vain lyhyillä profiileilla, ja
juuri se kaatoi sen laitteella: `nappula65`:n rivistössä on neljä riviä, joista `Real Name`
vie kaksi. Avaaja on nyt rating-rivin alla, ennen rivistöä.

*Yleisempi opetus.* Kuvaus ei valehdellut vaan kuvasi lopputuloksen jonka rakenne tuottaa
**vain osalla aineistoa**. Sama laji virhe kuin `docs/TESTAUS.md`:n järjestetty lista: väite oli
tosi sillä sivulla jolla se kirjoitettiin.

**Sama vika on yhä lautaruudussa, ja se on tunnustettava tässä.** Tommin havainto koski
alunperin **ottelun sivupaneelia** eikä profiilia, ja korjaus meni profiiliin. Paneelissa
`Message` on `LinkText` muistutusten seassa (`BoardScreen.kt`), eli alleviivattua tekstiä siinä
missä `Reminders` ja `Cube reminder`, jotka jäävät laitteelle. Rikko on siellä **selvempi kuin
profiilissa**, koska `Skip Game` seisoo samassa pinossa nappina juuri sillä perusteella että se
tekee jotain sivustolla. Korjaus on sama muoto, ja se on tekemättä 29.8.2026.

**Lähetetty viesti menee arkistoon vasta sivuston kuittauksesta** (`isMessageSentPage`),
kuten vastaus viestiruudussa. Laji on `QUICK_MESSAGE` eikä arvaus: Tommin koe 22.8.2026
lähetti juuri tällä lomakkeella, ja vastaanottajan jono luokitteli tuloksen nimellä
`DailyGammon Quick Message`. `replyTo` on null, koska tämä on aloitus eikä vastaus.

**Kolme tunnustettua rajaa.** Kuittaussivu nimeää vastaanottajan linkkinä, mutta arkistoon
kirjataan sen profiilin nimi jolta lomake luettiin; sivun oma lause säilyy silti sanatarkasti
`rawHeader`issa, joten totuus on tallessa vaikka kentät pettäisivät. Sivun 80 merkin raja
kerrotaan sanoina muttei noudateta, koska palvelin rivittää eikä leikkaa (mitattu
24.8.2026). ~~Ja fraasinappeja ei ole: `hi`, `gg` ja `ty gg u2` ovat ottelun aloitus- ja
lopetusfraaseja, eikä profiililta lähetettävä viesti ole kumpikaan niistä hetkistä.~~
**Fraasinapit tulivat 3.9.2026** Tommin havainnosta ensimmäisen lähetyksen jälkeen
(*"fraasit puuttui lähetys-näytöltä"*): sama `PhraseRow` ja sama lista kuin viestiruudussa ja
laudalla. Aiempi rajaus nojasi kiinteään `hi`/`gg`-listaan, ja lista on nyt muokattava.

~~**Laiteajo 29.8.2026 jäi puolitiehen, ja se on sanottava tarkkana.**~~ **Täydennetty samana
iltana**, ks. alla. Ensimmäisessä ajossa nähtiin vain `Message`-linkki rivistön alla, eikä
kirjoituskenttää avattu.

**Lähetys tehtiin 3.9.2026, ja `Sent`-tila on nähty**, ks. *Pelaajahaku ja pelaajakortti*.
Alla oleva kirjaus jää kertomaan mitä oli nähty sitä ennen.

**Kenttä nähtiin ruudulla 29.8.2026 illalla, lähettämättä mitään.** `nappula65`:n profiililta
avattuna ruudulla oli otsikoitu kenttä `Message to nappula65`, laskuri `0 characters`,
rivitysteksti *"DailyGammon wraps the message every 80 characters. Longer text still arrives in
full, on several lines."* ja **`Send message` estettynä**, koska luonnos on tyhjä. Neljä asiaa
jotka olivat siihen asti pelkkien yksikkötestien varassa ovat siis nähtyjä. **Kenttään ei
kirjoitettu eikä mitään lähetetty**, joten lähetyksen lopputilat (`Sending`, `Sent`,
`Unconfirmed`) ovat yhä vain testeissä. Ensimmäinen oikea lähetys on Tommin teko.

**Sivutuote: yksi fixturen väite kumoutui samalla katsomisella.** Fixturen kommentti sanoi
että toisen pelaajan rivistössä on Home Page eikä E-mailia. Laitteella näkyi toisen pelaajan
profiili jolla oli sekä Real Name, Location, E-mail että Comment. Rivistö riippuu siis siitä
mitä pelaaja on itse täyttänyt eikä siitä kenen profiili on kyseessä, ja juuri niin
`ProfileParser` lukee sen jo (rakenteesta eikä nimilistasta). Yhden sivun rivistö oli luettu
koko sivulajin ominaisuudeksi.

### `Message` on nappi ja lukukelvottomalla sivulla on `Back` (1.9.2026)

Kaksi pientä muutosta samassa erässä, ja molemmat ovat **jo kirjatun säännön tekemättä jäänyt
puolisko** eivätkä uusia päätöksiä. Molemmat ovat `docs/AVOIMET.md`:n avoimia kohtia, ja
molemmat todennettiin kuoriproxylla samana iltana.

**Lautapaneelin `Message` on reunustettu nappi eikä alleviivattu teksti.** Sääntö on
25.8.2026 tehty: sivun oma teko näyttää napilta, sovelluksen oma lisä tekstiltä, ja ehto
luetaan lähteestä eikä sijainnista. `Message` avaa sivuston oman chat-lomakkeen, mutta se
näytti samalta kuin `Reminders` ja `Cube reminder`, jotka jäävät laitteelle. Tommin mitattu
vaiva 29.8.2026: *"vei hetken löytää Messages eikä se ollut nappi."*

Väri on sivun muiden komentojen `TextSecondary` eikä perumisen `CheckerSelf`: avaaminen ei
lähetä mitään eikä ansaitse perumisen painoa. **Se siirtyi samalla `Skip Gamen` edelle**,
koska toimintorivillä skip on sivun komennoista viimeinen ja syy pätee paneelissa
sellaisenaan: skip on ainoa jonka jälkeen ruudulla on eri peli.

*Tunnustettu hinta:* paneelissa on nyt kaksi samannäköistä nappia, joten skip ei ole enää
ainoa reunustettu. Ero luetaan järjestyksestä ja sanasta kuten toimintorivilläkin. Oma väri
`Message`lle olisi ollut vaihtoehto, mutta värit ovat tässä ruudussa varattuja merkityksiä.

**`BoardUiState.NotABoard` sai näkyvän `Back`-napin.** Lautaruudulla ei ole omaa paluunappia
(9.8.2026, Tommin valinta): nappi veisi laudalta tilaa ja paluu on järjestelmän ele. Molemmat
perusteet kaatuvat tässä yhdessä tilassa. **Lautaa ei ole**, joten tilaa ei viedä keneltäkään,
ja **ele ei ole näkyvissä**, koska ruutu piilottaa järjestelmäpalkit saadakseen nappulalle
korkeutta. Tabletilla lukukelvoton sivu oli siis umpikuja, vaikka sen oma teksti kehottaa
palaamaan (*"Go back and refresh the list"*).

Nappi on `RetryNote`n oma, eikä sille kirjoitettu uutta komponenttia: `retryLabel` on olemassa
täsmälleen tätä varten, eli sitä tapausta jossa uudelleenyritys on paluu eikä päivitys. **Ehto
on tilassa eikä ruudussa**, joten ladattu lauta on ennallaan ja 9.8.2026 tehty päätös pätee
siellä missä se koskee lautaa.

### Näkyvä varmuuskopio viestiruudun yläreunassa (1.9.2026)

**Tommin toive:** *"haluaisin synkronoinnin google tilille."* Punninnassa (`docs/AVOIMET.md`)
kävi ilmi että puolet siitä on ollut olemassa 13.8.2026 alkaen — kanta menee Auto Backupissa
Google-tilille — ja että puuttuva osa ei ole kopio vaan **tieto kopiosta**: Androidin oma
varmuuskopio palautuu vain asennettaessa, sitä ei voi pyytää, eikä sovellus näe sen tilaa
millään rajapinnalla. Tommi valitsi näkyvän varmuuskopion ennen monilaitesynkronointia.

**Rivi eikä kortti, ja se on arkiston yläpuolella.** Kortti väittäisi joka avauksella että
jotain on sattunut; tämä on tila, ja se luetaan silloin kun arkistoa katsotaan. Kolme tilaa:
ei käytössä, käytössä (aika mukana), ja epäonnistunut. Kolmas on koko ominaisuuden syy
näkyvänä: **hiljaa epäonnistuva varmuuskopio on väärä varmuus.**

**Aika on minuutin tarkkuudella**, toisin kuin arkiston reunapäivä (`ArchiveEdge`), ja ero on
kysymyksessä. Reunapäivä vastaa siihen mihin asti arkisto ulottuu, ja siihen päivä riittää.
Tämä rivi vastaa siihen onko kopio tuore, ja aamu ja ilta ovat siihen eri vastauksia.

**Yksi tiedosto, ylikirjoitettuna.** Kohde kysytään kerran (`CreateDocument`) ja oikeus
otetaan pysyväksi (`takePersistableUriPermission`); ilman sitä osoite toimisi sen kerran ja
epäonnistuisi seuraavalla käynnistyksellä, eli juuri se hiljainen vika jota vastaan tämä on.
Kirjoitus on `wt` eikä `w`: ilman katkaisua lyhyempi arkisto jättäisi edellisen kopion hännän
tiedoston loppuun, eli rikkinäisen JSONin joka näyttää onnistuneelta. Nimiehdotus on päivätön
(`dg-archive.json`), koska tämä on *se* ajantasainen kopio; `Export` on yhä erikseen ja sen
nimi on päivätty, koska se on kopio talteen eikä varmuuskopio.

**Automaattinen kopio kirjoitti tyhjän arkiston 4.9.2026 asti (mitattu ja korjattu
5.9.2026).** Vika oli siinä mistä vienti luki. Ruudun `messages`-virta on `stateIn`, jonka
alkuarvo on tyhjä lista, ja vuorokautinen kopio laukeaa juuri ruudun avautuessa. Tiedostoon
kirjoittui `"messages": []`, ja aika leimattiin silti tehdyksi, joten kopio esti itsensä
vuorokaudeksi. **Rivi sanoi arkiston olevan tallessa, ja se oli väärä varmuus** eli
täsmälleen se mitä tämä ominaisuus on olemassa estämään. `Save now` on aina toiminut, koska
sitä painetaan ladatun ruudun päällä. Vienti lukee nyt arkistosta, ja mittaus on
`docs/TESTAUS.md`:ssä.

**Ei taustatyötä, ja se on ehto eikä puute.** Automaattinen kopio syntyy vuorokauden välein
**viestiruudun avautuessa** (`BackupStatus.isDue`). `WorkManager` olisi tuonut ajastuksen ja
uuden riippuvuuden; kierrospohjaisessa pelissä ruutu avataan päivittäin, joten väli toteutuu
ilman sitä. Sovellus pysyy siinä mitä se on ollut: mitään ei tapahdu ilman että ruutu on auki.

**Käytössä olevalla rivillä on kaksi tekoa, ja toinen niistä puuttui ensin (Tommin havainto
1.9.2026).** Ensimmäinen versio näytti käyttöönoton jälkeen vain `Save now`n, jolloin
**kohdetta ei päässyt vaihtamaan lainkaan**: valitsin avautui vain käyttöönotossa. Tommi
valitsi ensimmäisellä kerralla Lataukset-kansion Driven sijaan, eikä ruudulla ollut mitään
millä korjata se. Rivillä on nyt `Change file` ja `Save now`.

Vika on samaa perhettä kuin illan kaksi muuta: ruutu näytti valmiilta tilanteessa jossa teko
puuttui. Ero on siinä, että tämän löysi ensimmäinen oikea käyttö eikä koodin luku — ja se
löytyi vasta kun ominaisuus oli **onnistunut**, koska vasta silloin ruutu näytti sen tilan
jossa aukko on.

**Mitä rivi ei sano.** Se ei väitä mitään Auto Backupista, koska sovellus ei voi lukea sen
tilaa. Teksti puhuu vain siitä tiedostosta jonka käyttäjä itse valitsi, ja `Archive is not
backed up to a file of your own` on sanamuotona tarkka juuri siitä syystä.

### Sivusto nimeää teot, sovellus nimeää paikat (Tommin päätös 1.9.2026)

**Purettu 22.9.2026**, ks. *Ensimmäinen välilehti on Top Page*. Kun välilehti on sivuston
oma `Top Page`, napin sivuston oma `To Top` osoittaa jo samannimiseen paikkaan, joten
käännökselle ei ole enää syytä. Osio jää historiaksi.

**Sääntö tarkentui, ei kumoutunut.** 25.8. kirjattu muoto sanoo että sivun oma teko näyttää
napilta ja sovelluksen oma lisä tekstiltä, ja että nappien sanoja ei käännetä koska ne ovat
sivun sanoja. Tähän tuli yksi rajattu poikkeus, ja sen laukaisi Tommin havainto laiteajossa:
*"To Top voisi lukea To Matches, joka on ensimmäinen välilehti."*

**Havainnon ydin on että sovellus oli jo nimennyt kohteen uudelleen.** Sivuston `Top Page` on
sovelluksessa `Matches`-välilehti 26.8.2026 alkaen, joten sana `Top` ei esiintynyt
sovelluksessa **missään muualla kuin tässä yhdessä napissa**. Nappi osoitti siis paikkaan
jota sovellus itse kutsuu toisella nimellä, ja juuri se on se hetki jolloin lukija joutuu
kääntämään sanan päässään.

**Tarkennus on yksi lause.** Sivusto nimeää teot, sovellus nimeää paikat. Verbi ja teko
pysyvät sivuston omina, mutta kohde jolle sovelluksella on oma nimi kirjoitetaan sillä
nimellä. `Next Game` ei siis käänny, koska jonon seuraavalle ottelulle sovelluksella ei ole
omaa nimeä; sivuston sana on ainoa olemassa oleva.

**Toinen suunta punnittiin ja hylättiin.** Vaihtoehto oli nimetä välilehti takaisin
`Top`iksi, jolloin sääntöön ei olisi tarvinnut koskea. Se kaatui siihen että
välilehtipalkki ei voi seurata sivustoa kokonaan: viidestä välilehdestä kahdella
(`Inbox`, `Info`) ei ole sivustolla vastinetta lainkaan, ja `Forum` on tietoinen uudelleen
nimeäminen sivuston `Discussion`ista. Yhden nimen siirtäminen sivuston sanaan olisi tehnyt
palkista sekoituksen jossa käyttäjä ei näe kummasta on kyse.

**Toteutus on uloin kerros ja vain se.** `SubmitLabels.isToTopPage` tunnistaa napin tarkalla
vertailulla, ja näyttöteksti kootaan `board_submit_to_place`sta ja `tab_matches`ista, jolloin
välilehden nimi ja napin kohde eivät voi erkaantua. **Lähetettävä arvo on yhä sivun oma
merkkijono**, ja kaikki ehdot lukevat sitä: peruuttamattomien tekojen joukko, kuutiotarkistus
ja lomakkeen POST. Käännös on siis yksisuuntainen, ja molemmat piirtokohdat (nappirivi ja
chat-kortti) käyttävät samaa muunnosta.

### Ensimmäinen välilehti on Top Page (Tommin päätös 22.9.2026)

Tommin sanat: *"sivuston oma sana käyttöön. Top Page, otsikko saa olla Matches. Laudan nappi
-> To Top"*. Päätös kumoaa kaksi aiempaa ratkaisua ja pitää kolmannen.

- **Välilehden nimi on `Top Page`** eikä `Matches`. 26.8.2026 peruste oli että välilehden
  nimi kertoo sisällön eikä sivuston osoitetta. Nyt voittaa sivuston oma sana, samaan tapaan
  kuin `Lounge` on sivuston sana.
- **Ruudun otsikko pysyy `Matches`** (`top_title`). Sama periaate kuin 26.8.2026 kirjattiin
  toiseen suuntaan: välilehden nimi ja ruudun otsikko saavat erota.
- **Laudan nappi on sivuston oma `To Top`** eikä enää `To Matches`. 1.9.2026 käännös oli
  olemassa vain siksi että paikalla oli sovelluksessa eri nimi, ja nyt sillä ei ole.
  `SubmitLabels` ja `board_submit_to_place` poistuivat käyttämättöminä.

App Helpin ja ruutujen tekstit jotka nimeävät välilehden (`Open the Matches tab first`,
`Listed under Matches`, välilehtikappale) sanovat nyt `Top Page`. Tekstit jotka puhuvat
ottelulistasta sisältönä (*your match list*) jäivät ennalleen, koska ne eivät nimeä
välilehteä.

### Tutun toiminnon nappi on sivuston sana kauttaaltaan (Tommin päätös 22.9.2026)

Tommin sanat: *"dailygammonista tuttujen toimintojen nappien pitäisi olla identtisiä
kauttaaltaan"*, ja tarkennus samana iltana: *"myös avausnapit sivuston sanoiksi ja Post
myös"*.

**Sääntö.** Kun sovelluksen nappi tekee saman teon kuin DailyGammonin nappi tai linkki, sen
teksti on sivuston sana merkki merkiltä, isot kirjaimet ja `<<` mukaan lukien. Sääntö koskee
myös lomakkeen avausnappia ja vahvistusdialogin hyväksyntää. Sovelluksen omat lisät
(Refresh, Export, Mark, Search ja muut) pysyvät omina, koska sivustolla ei ole vastinetta;
pelaajahaussa sivustolla ei ole nappia lainkaan, lomake lähtee Enterillä.

Tämä laajentaa 25.8.2026 säännön (sivun oma teko näyttää sivun omalta) koskemaan myös
nappeja jotka sovellus piirtää itse eikä lue sivulta.

| Kohta | Ennen | Nyt, sivuston sana |
| --- | --- | --- |
| Kirjautuminen | Sign in | Log In |
| Uloskirjautuminen | Sign out | Log Out |
| Viestiin vastaaminen | Send reply | Send Reply |
| Profiilin viesti, avaus ja lähetys | Message, Send message | Send, Send |
| Profiilin kutsu, avaus ja lähetys | Invite to a match, Send invitation | Invite, Invite |
| Ilmoittautuminen ja vahvistus | Sign up | Sign Up |
| Ilmoittautumisen peruminen | Cancel sign-up | Cancel Signup |
| Loungen liittymisen vahvistus | Join match | Join |
| Palstan lomakkeen nappi | Post | lomakkeen oma `Create New Thread` tai `Submit Comment` |
| Ottelun päättymisruutu | Review Game | <<Review Game |
| Luovutussivun rivi | Moves | Review |

Tekstit jotka nimeävät näitä nappeja (App Help, lounge-ilmoitukset) seuraavat samaa sanaa.
Otsikot ja proosa (*Sign in to DailyGammon*, *Signed out*) eivät ole nappeja eivätkä
muuttuneet.

### Asetusruutu kahdeksi segmentiksi (Tommin päätös 22.9.2026)

Tommin havainto: *"sekavuutta, kannattaako asetus-näytöt tehdä välilehdiksi?"* ja päätös:
*"kyllä, DailyGammon ja This device auki oletuksena"*.

**Sekavuuden syy oli rakenne eikä teksti.** Laiteosio tallentuu heti muutoksesta, ja
sivuston lomake vain Update Preferences -napista. Yhdessä vierityksessä sääntö vaihtui kesken
ruudun, eikä ruudusta nähnyt missä. Nyt ruudun yläreunassa on segmenttirivi `This device |
DailyGammon`, sama kuvio kuin `Your turn | Tournaments` ja `General | Politics`, ja kummankin
segmentin ensimmäinen lause sanoo sen yhden säännön.

- **This device on auki oletuksena**, ja sen avaaminen ei hae mitään. Sivuston asetussivu
  haetaan vasta kun DailyGammon-segmentti avataan ensimmäisen kerran
  (`SettingsViewModel.open`), ja Refresh näkyy vain siinä segmentissä.
- **Lähettämättömät raksit säilyvät segmentin vaihdon yli.** Segmentin sisältö poistuu
  koostumuksesta, joten kummankin tila säilötään `rememberSaveableStateHolder`illa
  avaimittain; ilman sitä vaihto hävittäisi muutokset hiljaa.
- **Kumpikin segmentti vierii omana sarakkeenaan.** 3.9.2026 vaakatilan ongelma
  (*Asetusruutu vierii kokonaisuutena*) ei voi palata, koska laiteosio ei enää jaa korkeutta
  lomakkeen kanssa.
- Laiteosion oma otsikko poistui, koska segmentti nimeää sen.
- **Update Preferences sykkii kun lomake poikkeaa luetusta sivusta** (Tommin tilaus samana
  iltana). Vertailukohta on sama kuin laskurilla, joten takaisin raksittu kenttä ei ole
  muutos. Mitattu nauhalta: napin kirkkaus 158 ja 82 välillä noin 1,4 sekunnin jaksolla.
  **Muoto vaihtui 24.9.2026** (Tommin kirjaus 22.9., valinta 24.9. *"purppura kehys jää"*):
  jatkuvan alfasykkeen tilalla on Cube reminderin muoto, eli purppura kehys joka sykkii kuusi
  kertaa ja jää ohuena lepotilaan. Yhteinen koodi on `ReminderPulse.kt` (`reminderAttention`),
  ja laitteen animaatioasetus nollassa antaa suoraan lepokehyksen. Muita odottavasta teosta
  muistuttavia animaatioita sovelluksessa ei ole. **Todennettu SM-T970:llä 24.9.2026 klo
  23.32** (`raakasivut/sessio-24-9-ilta2/kaappaukset`, `c10`, `c11` ja `c14`): *Hide pip
  counts* rastiin, jolloin *1 setting(s) will change.* ja paksu purppura kehys, kuusi
  sekuntia myöhemmin ohut lepokehys. Rasti pois, jolloin *Nothing changed yet.*, nappi
  harmaa ja kehys poissa. Mitään ei lähetetty.
- **Laitteella löytyi kaksi vikaa ennen committia.** Vieritetty sisältö piirtyi
  segmenttirivin päälle, ja yksi provider vaihtuvalla avaimella jätti ruudulle väärän
  segmentin sisällön. Korjaus: kumpikin segmentti on oma haaransa kiinteällä avaimella
  painollisen, rajaavan laatikon sisällä.
- Vaakatilaa ei kaapattu, koska pystylukko pitää asetusruudun pystyssä; 3.9.2026 ongelma on
  poistunut rakenteesta, ei vierityksellä korjattuna.

### Laiteosio neljään taittuvaan ryhmään (Tommin valinta 27.9.2026)

Tommin tilaus: *"laitteen omat asetukset voisi järjestää uudelleen, ehdota ryhmittelyjä"*.
Laiteosiossa oli viisitoista otsikkoa yhtenä luettelona syntyjärjestyksessä. Kätisyys oli
kaukana laudasta, ja sovelluslukko tuli listojen värien jälkeen. Kolmesta ehdotuksesta Tommi
valitsi neljä ryhmää ja taiton kaikki auki:

- **Theme** ylimpänä ryhmien ulkopuolella, koska se kirjoittaa alempia valintoja (22.9.2026).
- **Board**: Board look ja arvontakytkin, Dice, Score ja Handedness.
- **Playing**: Forced moves, Tapping the dice ja Money game.
- **Lists and screens**: Background, Pictures below the lists, Rarity colours ja Waiting
  for DailyGammon.
- **Screen and lock**: Screen orientation ja App lock.

Otsikot taittuvat samalla `FoldHeading`illa kuin Loungen jaksot, ja kiinni olevat ryhmät
muistetaan laitteella (`SettingsFoldStore`, oletus kaikki auki). Raidoitus alkaa kunkin ryhmän
sisällä alusta. **Neljännen ryhmän nimi vaihtui toteutuksessa:** valittu *This device* on jo
segmenttirivin nimi, joten ryhmästä tuli *Screen and lock* (Tommi vahvisti 27.9.2026). Todennettu Pixel 8a:lla samana
aamuna (`raakasivut/sessio-27-9-donate/asetukset-ryhmat-1.png`–`3.png`): Board taittui,
Playing nousi sen alle, ja taitto säilyi sovelluksen uudelleenkäynnistyksen yli.

### Asetusten valinnat kortteina esikatseluineen (Tommin tilaus 23.9.2026)

Tommin tilaus: *"Settings-näyttö kaipaa preview per vaihtoehto"*. Tarkennukset tulivat samassa
sessiossa. Asetteluksi valittiin C chatin neljästä mockupista eli kortit vierekkäin. Kuviksi
tarkennettiin *"mielellään pelitilanteista"*. Lisäksi *"klikkaamalla isot lautakuvat olisi
hyvä lisäys"*, ja suurennuksen ele valittiin kolmesta: erillinen ikoni.

- **Viisi radioryhmää on kortteja**: Theme, Board look, Dice, Score ja Waiting for
  DailyGammon. Kytkimet jäävät riveiksi, koska ne kuvaavat tekoa eivätkä ulkoasua. Kortit
  rivittyvät `FlowRow`lla, joten puhelimella ja pystytabletilla rivejä on useampi.
- **Kuva on sama pelitilanne kaikissa korteissa**, ja se on piirretty sovelluksen omilla
  komponenteilla eikä kuvina (`PreviewBoard`, `PreviewDice`, `PreviewScore`, `PreviewBusy`).
  Kuva ei siis voi vanhentua laudan muuttuessa. Tilanne on anonymisoitu fixture
  `move_greedy_bearoff.html` assetina (`assets/preview/situation.html`): 21 pisteen ottelu,
  kuutio 2:ssa ja heitto 3-3, josta yksi askel on pelattu. Tupla on valittu siksi, että
  noppavalinnan ero näkyy vain tuplassa.
- **Pienoislauta piirretään kaksinkertaisena ja skaalataan puoleen.** Kortin kokoisena
  numerorivit rivittyivät kahdelle riville ja veivät laudalta korkeuden (laiteajo
  23.9.2026). Suurennus piirtää luonnollisessa koossa, jolloin numerot ovat pelin kokoisia.
- **Suurennuslasi Theme- ja Board look -korteissa.** Se avaa laudan lähes koko ruudun
  levyisenä, ja siinä on napit `Use this` ja `Close`. Kortin muu pinta valitsee heti kuten
  ennen. Nopat, pisteet ja ilmaisimet eivät saaneet suurennusta, koska ne näkyvät jo
  luettavan kokoisina.
- **Suurennuslasi poistettiin 6.10.2026** (Tommin tilaus: *"poista suurennuslasi, sen sijaan
  klikkaamalla kuvaa saa sen ison kuvan"*, ja monivalinnasta *kaikki kuvat isoksi*). Kuvan
  napautus avaa ison kuvan kaikissa korteissa, myös Dice-, Score- ja Waiting-korteissa, ja
  kuvan alla oleva rivi radioineen valitsee. Iso kuva on kortin oma (`OptionCard`, `large`),
  joten ruudulla ei ole enää erillisiä suurennusmuuttujia. Nopat, pisteet ja ilmaisin
  suurenevat kokonaisina (`ZoomedPicture`), koska tekstin koko tulee fontista. Samana päivänä
  pelkän laudan kuvat saivat omistamattoman robottikuution lokerosarakkeen keskelle
  (*"tuplattu tuplauskuutio ei pitäisi olla näkyvissä vaan sivupaneelissa ja tuplaamaton
  androidin pää keskikaistan sivulokerossa"*, `previewUnownedCube`), Handedness-kuvat tuplatun
  kuution omistajan pelaajakorttiin ja tilanteen pelaajat Brayn nimet *Fanning Finn* ja
  *Dowager Duchess* kuten kauppakuvissa (`docs/KAUPPA.md`). Score-kuvat näyttävät nimen ja pelaajakortin
  parin oma ensin (`19-away 20-away`, `1-2/21`), koska kortti näyttää 4.10.2026 alkaen parin
  eikä vain omaa lukua (Tommi: *"pelaajakortissa on kaksi away-tietoa samalla rivillä"*,
  `previewCardScoreText`); turnaus *The Marathon #1234* entisen *Testitapahtuma #1234*:n tilalle,
  Tommin valinta neljästä.
- Ilmaisimet pyörivät keskikaistan huovalla ja noudattavat Art deco -kytkintä. Satunnaisen
  kortti kiertää viittä muotoa, koska sillä ei ole omaa muotoa.
- **Sivuston puolella samat kortit Background Color- ja Board Scheme -ryhmille** (Tommin
  tilaus samana iltana). Kuva on sivuston lauta sivuston kahdella nopalla, sivun
  taustavärillä. Kumpikin ryhmä näyttää toisen nykyisen valinnan, ja kuva luetaan
  lomakkeen muokkauspuskurista, joten tallentamaton valinta näkyy ennen Update Preferencesia.
  Värit luetaan sivulta (`ChoiceOption.swatch`, rivin `BGCOLOR`), eikä niitä ole
  sovelluksessa taulukkona. Taustaväri on yhä sovellukselle pelkkä esikatselu: lauta ei
  käytä sitä (`docs/ASETUKSET.md` luku 1, kohta A).
  **Background Color -kortit poistettiin 25.9.2026 (Tommin päätös).** Ne lupasivat
  sovellukseen jotain mitä se ei tee. Ryhmä on nyt valintalista, jonka alla on rivi
  `settings_background_browser_only`. Board Scheme -kortit piirretään sovelluksen omalle
  pinnalle, ja Minin viesti on pinnan tekstivärillä.
  **Mini-korttia ei voi valita 25.9.2026 alkaen (Tommin päätös).** Kortti näkyy himmeänä
  (`OptionCard(enabled = false)`), ja siinä on oma lyhyt viesti `settings_mini_card`, koska
  laudan kehotus vaihtaa skeema asetuksista on asetusruudussa kehäpäätelmä. Jos Mini on jo
  valittuna sivustolla, valinta näkyy ja lähtee lomakkeen mukana muuttumattomana. Laudan
  ja kortin viesti `board_mini_scheme` lyheni samalla kahteen virkkeeseen: sovellus ei
  näytä Miniä, valitse toinen skeema. Syy (pistenumerot puuttuvat) on `docs/KOHDE.md`:ssä
  eikä käyttäjän ruudulla.
- **Mini näyttää laudan viestin eikä lautaa.** Ensimmäinen versio piirsi Mini-kortille
  X-22-värisen laudan, mutta sovellus ei piirrä Mini-skeemalla lautaa lainkaan
  (`board_mini_scheme`). Kortti lupasi siis jotain mitä sovellus ei tee. Laitteelta
  huomattu ja korjattu ennen committia; Mini-kortilla ei ole suurennusta.
- Todennettu Galaxy Tab S7+:lla 23.9.2026 sekä pysty- että vaakatilassa. Kortit, pyörivät
  ilmaisimet ja suurennus (Monte Carlo variant ja Deco-teema) on kaapattu. Suurennus
  suljettiin Closella, eikä yhtään asetusta muutettu.

### Chat-kortin katto on korkeusraja eikä painotus (Tommin havainto 1.9.2026)

**Vika näkyi laudan koossa mutta asui kortissa.** Kortilla oli `weight(1f, fill = false)`,
ja sarakkeen painotus jakaa tilan **ennen mittausta**. Kortti sai siis puolet korkeudesta
riippumatta siitä tarvitsiko se sen, eikä käyttämätön osa palaudu laudalle. Yhden rivin
viesti maksoi laudalle saman kuin kymmenen rivin, ja loppu jäi tyhjäksi kaistaksi
alareunaan.

Mitattuna Galaxy Tab S7+:n vaakaruudulta 1.9.2026, kun saapunut viesti oli yksi rivi
(`ti chat 1`): kortti käytti 320 pikseliä, sen osuus oli 855, ja laudan alle jäi noin 500
pikseliä tyhjää. Tommin sanoin *"lauta kutistuu häiritsevästi"*.

**Korjaus antaa saman katon mutta vasta mittauksessa.** Kortti mitoitetaan
`BoxWithConstraints`in sisällä ja katto on `maxHeight / 2`, jolloin lyhyt kortti vie oman
korkeutensa ja lauta saa lopun. Painottamaton lapsi saa sarakkeessa jäljellä olevan
korkeuden ylärajakseen, joten luku on juuri se tila jota kortti ja lauta jakavat, eikä ruudun
korkeus.

**Katto ei piilota mitään lopullisesti**, koska kortin viestiosa vierii sisällään (28.8.2026
tehty rakenne). Avatun kirjoituskentän käytös ei muuttunut: se voittaa laudan kuten ennenkin,
koska kenttä johon ei pääse kirjoittamaan on pahempi kuin lauta jota ei hetkeen näe.

**Todennettu laitteella samaa sivua vasten (1.9.2026, kuoriproxy).** Sama kaappaus jossa
`ti chat 1` on, sama tabletti ja sama vaakasuunta. Laudan kehys oli ennen korjausta
1150 × 780 pikseliä ja sen jälkeen 1992 × 1288, eli korkeutta tuli 65 prosenttia lisää.
Kehyksen alareuna siirtyi 1132:sta 1640:een, joka on navigointipalkin yläreuna, eli laudan
alle jäänyt tyhjä kaista katosi kokonaan. Yläreuna pysyi paikallaan (352), koska kortti vie
yhä oman korkeutensa; se ei kutistunut, vaan lakkasi varaamasta sitä mitä ei käytä.

### Arvattu asema on merkintä, peruutus on kortti (Tommin havainto 1.9.2026)

*"This is what-if toistuu tarpeettoman usein, oikeastaan turhan usein. DailyGammon-pelaaja
tietää ilmiön."*

**Kaksi huomautusta olivat siihen asti sama kortti, ja se kohteli eri lajeja samoin.**
Peruutus (`board_rolled_back`) on tapahtuma: jokin meni toisin kuin oletit, se on harvinaista
ja se ansaitsee kortin jonka kuittaat. Arvattu asema (`board_speculative`) on tila, ja sivusto
tuottaa sen lähes joka siirrolla, koska se arvaa vastustajan siirtoja nopeuttaakseen otteluita
(`SUBSTANSSI.md` kohta 77).

**Peruste on jo kirjattu toisaalla samassa muodossa.** Varmuuskopiorivistä sanotaan että se on
rivi eikä kortti, koska *"tämä ei ole tapahtuma vaan tila, ja kortti väittäisi joka avauksella
että jotain on sattunut"*. Arvattu asema rikkoi täsmälleen sitä sääntöä: kolmen virkkeen
selitys ja kuittausnappi joka siirrolla, ilmiöstä jonka pelaaja tuntee ennestään.

~~**Arvattu asema on nyt kahden sanan merkintä** (`What-if position`) laudan yläreunassa, ilman
kuittausta. Napautus avaa saman pitkän tekstin joka oli ennen oletus, ja kuittaus sulkee sen
takaisin merkinnäksi.~~ **Merkintäkin poistettiin samana iltana, ks. alla.**

**Arvatusta asemasta ei sanota ruudulla mitään (Tommin päätös 1.9.2026).** Merkintä asennettiin
laitteelle ja Tommi katsoi sen: *"merkintäkin on liikaa, en usko yhteisön tarvitsevan sitä."*
Muoto ehti siis olla kolmen virkkeen kortti kuittausnappeineen ja kahden sanan merkintä, ja
molemmat kaatuivat samaan havaintoon. Sivusto arvaa vastustajan siirtoja lähes joka siirrolla,
joten kyse on kohteen tavallisimmasta tilasta, ja sovellus selitti sitä yleisölle jolle se on
ennestään tuttu.

**Peruutuskin on rivi 3.9.2026 alkaen, ja otsikon jako kaatui mittaukseen.** Kortin peruste
oli harvinaisuus, ja illan pelisessio antoi luvun: peruutus tuli seitsemällä laudalla 44:stä,
ja koko korpuksessa se on 44 sivulla. Tommi: *"kyllä se häiritsee, hiljaisempi ratkaisu."*
`board_rolled_back` on nyt yksi virke (*"Rolled back: your opponent did not play the site's
guess."*) sivupaneelissa tilannetekstin yläpuolella samassa asussa, ilman kuittausta; kortti,
sen `noticeDismissed`-tila ja kolmen virkkeen teksti poistuivat. Lippu `rolledBack` jäsennetään
ennallaan, ja `plainNotices` suodattaa lauseen yhä, joten sama tapahtuma ei piirry kahdesti.
Jäljelle jäävä kortti laudan päällä on rastittoman kuutioteon huomautus, ja se on eri laji:
vastaus painallukseen, ei sivun tila.

**Sivuston oma kysymys ei myöskään palaa ruudulle.** `BoardState.situation` on suodattanut sen
pois jo aiemmin, ja rajaus jää voimaan; vain sen peruste vaihtui. Aiemmin siksi että lippu sai
oman palkkinsa, nyt siksi ettei asiasta sanota mitään lainkaan.

**Lippu jäsennetään yhä** (`BoardState.speculative`). Malli kantaa sen mitä sivu sanoi ja näkymä
päättää mitä siitä näytetään; jäsentimen pudottamana tieto katoaisi kokonaan, ja se on eri väite
kuin *"tätä ei näytetä"*. Jos tarve joskus palaa, palautettava on yksi ehto eikä uusi jäsennys.

**Poistettu teksti on git-historiassa**, eikä sitä siirretty manuaaliin. Manuaaliin siirretty
selitys olisi sama väite hitaammassa muodossa: että ilmiö tarvitsee sovelluksen selityksen.

**Molemmat ovat yhä päällepiirtoa eivätkä palkkeja**, eli 31.8.2026 tehty ratkaisu pysyy:
palkki laudan yläpuolella vie korkeutta, ja lauta johtaa leveytensä korkeudesta, mistä syntyi
10.8.2026 mitattu värähtely. Päällepiirto ei kosketa asettelua lainkaan.

### Odottavien määrä otteluluettelon yläreunassa (2.9.2026)

Tommin toive 31.8.2026 (`docs/AVOIMET.md`): *"Ottelulistan tyhjettyä looginen siirtymä
mielestäni olisi Matches-näyttö ja 0 odottavaa."* Siirtymä oli jo olemassa 1.9.2026 alkaen:
Top Page -vastaus laudalle sulkee ruudun ja hakee luettelon tuoreena syystä riippumatta, eli
`To Top` ja tyhjentynyt jono päätyvät samaan (`MainActivity`, commit `baf0228`). Tämä on
lauseen jälkimmäinen puoli.

**Sanamuoto vaihtui 3.9.2026 Tommin pyynnöstä:** `It is your turn in N matches`, yksikössä
`It is your turn in 1 match` ja nollana `It is not your turn in any match`. Valinta tehdään
koodissa eikä plurals-resurssilla, samasta syystä kuin `settings_changes_count`. Samalla
`Signed in as` -rivi sai linkin ulkoasun (aksenttiväri ja alleviivaus), koska se on linkki
omaan profiiliin eikä näyttänyt siltä (Tommi: *"ei näytä linkiltä"*). Alla oleva teksti
kuvaa rivin aiemmalla sanamuodolla.

**Rivi poistui 27.9.2026 illalla Tommin tilauksesta** (*"Signed in as pelaaja on turhake ja sen
linkin pitäisi olla Refresh ja Settings-toimintojen yhteydessä"*, tulkinta kuitattu: nappi on
oma nimi). Otsikkorivillä on nyt `tommih | Refresh | Settings`, ja nimi avaa oman profiilin.
Nimi on sivuston sana, joten sitä ei käännetä (ks. *Termien valinta*). Pitkä nimi katkeaa
kolmeen pisteeseen. App Helpin `help_a_tabs` kertoo nimen, ja `help_a_d_marks` sanoo
merkintälistan olevan `Playable`-segmentin alla. **Todennettu Pixel 8a:lla ja SM-T970:llä
klo 22.41**, ja tabletilla napautus avasi profiilin. Alla oleva teksti kuvaa poistunutta riviä.

**Nimikyltti kolmelle välilehdelle 27.9.2026 myöhään illalla** (Tommin tilaus: kolmikko
*"kaikille näytöille, muttei kaikille välilehdille vaan lähinnä ensimmäiselle kolmelle"*).
Välivaihe oli sana `Profile` (`8b992de6`), ja lopullinen muoto on mockupin vaihtoehto B:
oma nimi pyöreässä kehyksessä, teksti ja kehys oman ratingin rarity-värissä (`ProfileAction`
tiedostossa `TopScreen.kt`). Tommin perustelu valinnalle: *"erottuu, mahtuu ja ilmaisee"*.
Muoto erottaa kyltin `Refresh`- ja `Settings`-napeista, ja väri vaihtuu ratingin mukana.
Kyltti on Top Pagella, Loungessa ja Forumin päänäkymässä, ei Inboxissa, Infossa eikä
foorumin ketjussa. **Todennettu Pixel 8a:lla kaikilla kolmella ja SM-T970:llä klo 22.58**:
kyltti on pinkki, koska oma rating 2219.6 on top 32:ssa.

**Rivi poistui 27.9.2026 Tommin pyynnöstä:** `Your turn` sanoo segmenttirivillä jo saman
asian, joten määrä siirtyi segmentin nimeen sulkeisiin (`Your turn (3)`) ja näkyy vain kun se
on yli nollan. Alla oleva teksti kuvaa poistunutta riviä.

**Segmentin nimi on `Playable` 27.9.2026 illasta** (Tommin valinta viidestä ehdotuksesta:
`Your turn`, `Matches`, `To play`, `Your move` ja `My turn`, sekä Tommin oma *Matches
playable*, josta lyhin muoto). Nimi nimeää listan eikä tilaa, ja se kertoo mitä sulkeiden
määrä laskee: `Playable (3)`. Rivien oma `Your turn` -merkintä on sivuston sana ja säilyy.

**Nimi vaihtui 28.9.2026 muotoon `Your move`** (Tommi: *"Playable ei enää tunnu parhaalta
ilmaisulta kontekstiin"*). Tommi ehdotti sivuston omaa *matches where you can move*, mutta se
on liian pitkä segmentiksi `Tournaments`in rinnalle puhelimessa. Neljästä lyhyestä muodosta
(`Can move`, `To move`, `Your move`, pitkä muoto) Tommi valitsi `Your move`. Todennettu
SM-T970:llä klo 18.45: `Your move (11)`.

**Nimi palasi 3.10.2026 muotoon `Your turn`** (Tommin tilaus: segmentti ja rivien merkintä
samalla sanalla). Sivuston oma lause on *It is your turn in N matches*, joten segmentti ja rivi
sanovat nyt saman asian sivuston sanalla: `Your turn (18)`.

**Rivi `N waiting for your move` on `Signed in as` -rivin alla, ja se näytetään myös nollana.**
Luku on `TopPage.yourTurnCount`, eli luettelosta johdettu eikä oma kenttä, joten se ei voi olla
eri mieltä rivien kanssa. Lasketaan vain ottelut joista sivu sanoo vuoron (`myTurn == true`);
`null` on tietämättömyys eikä nolla, eikä sitä lasketa kumpaankaan suuntaan. Top Pagella
`null` ei esiinny, mutta ehto on kirjoitettu mallin eikä sivun mukaan.

Rivi on samaa lajia kuin arkiston reunapäivä sen alla: mittalukema eikä ilmoitus, ja siksi
aina samassa paikassa. Nollana se on hyödyllisin, koska `No matches.` sanoo vain ettei
otteluita näy, ja tämä sanoo ettei yksikään odota siirtoa. Väri on sama kuin rivin `Your turn`
-tunnuksella kun luku on yli nollan, muuten toissijainen.

Testit: `TopPageTest` (core-domain, kolme arvoa erillään ja tyhjä luettelo nollana) ja
`TopViewModelTest` (yksi ottelu antaa yhden, oteluton luettelo nollan).

**Laiteajo 2.9.2026 (SM-T970):** rivi luki `18 waiting for your move` ensisijaisella värillä
`Signed in as` -rivin alla, ja luettelossa oli 18 `Your turn` -riviä. Nollatila on näkemättä
laitteella, koska se vaatii jonon pelaamisen tyhjäksi, ja se on Tommin teko; yksikkötesti
lukitsee nollan.

### Ottelun `.mat` jakovalikkoon (2.9.2026)

Tommin valinta illan ehdotuksista, ja kaanonimuutos: `SUBSTANSSI.md` kohta 24 sulki
Export-vaiheen 15.8.2026, ja Tommi avasi sen 2.9.2026 (*"a, vahvistan kaanonimuutoksen"*)
sen jälkeen kun portti oli näytetty. Peruste on kirjattu kohtaan 24: Android-laitteilla on
analyysiohjelmia, ja jakovalikko vie tiedoston myös Driveen eli työpöydälle. Rajaus säilyy
siinä että **tiedosto ei jää sovellukseen**.

**Mitä ruudulla on.** Profiilin `Finished matches` -rivillä on `Share .mat file` vastustajan
rivin oikeassa laidassa alleviivattuna, ja vain siellä, koska vain päättyneellä ottelulla on sivun oma `Export`-linkki (`docs/KOHDE.md`).
Painallus hakee `/bg/export/<id>` rivin omasta linkistä, kirjoittaa tiedoston välimuistiin
(`cacheDir/exports/dg-<id>.mat`) ja avaa Androidin jakovalikon `FileProvider`in kautta.
Laite näyttää vastaanottajat itse, sovellus ei valitse eikä tiedä mitä on asennettu.

**Kolme rajausta jotka ovat rakenteessa.** Ottelu annetaan näkymämallille arvona eikä polkuna,
ja se kelpaa vain jos se on ruudulla olevan profiilin päättyneiden listalla: osoitetta ei voi
antaa mistään muualta (`PageViewModel.export`, sama portti kuin `send`illä). Vastaus
tarkistetaan ennen jakoa: HTML-sivu `.mat`-nimellä olisi hiljainen väärä tiedosto, joten
runko kelpaa vain jos siinä ei ole `<html`-tunnistetta ja siinä on muodon oma otsikko
`point match` (`MatchExport.read`, mitattu yhdestä tallennetusta viennistä). Ja `Ready`
kuitataan heti kun valikko on avattu (`exportConsumed`), ettei ruudun uudelleenpiirto avaa
sitä toista kertaa.

**Tila yhtenä rivinä otsikon alla**, vain kun jotain on tekeillä tai meni pieleen: haku
käynnissä, sivu eikä tiedosto, verkko- tai palvelinvirhe, istunto katkesi. Onnistuminen ei
näy rivinä vaan jakovalikkona.

MIME on `text/plain`, koska `.mat` on tekstiä ja `application/octet-stream` pudottaisi
osan vastaanottajista. Haku ei kuluta jonoa. `FileProvider` jakaa vain `exports/`-kansion
(`res/xml/file_paths.xml`), ei mitään muuta sovelluksen tiedostoa.

Testit: `MatchExportTest` (core-domain, sivu ei ole vienti) ja `PageViewModelTest` (haku rivin
omasta linkistä ja nimi tunnuksesta, sivuvastaus ei jaeta, vieras ottelu ei tuota pyyntöä).

**Laiteajo 2.9.2026 (SM-T970), omalta profiililta.** `Share .mat file` piirtyi jokaiselle
`Finished matches` -riville eikä yhdellekään aktiiviselle. Ensimmäisen rivin painallus avasi
jakovalikon otsikolla `dg-5309133.mat`, ja välimuistissa oli samanniminen tiedosto jonka
ensimmäinen rivi on ` 9 point match` (198 riviä). Valikon ensimmäisellä rivillä oli
yhteystietoja ja toisella muun muassa `Quick Share`, `Bluesky`, selaimia ja **`Linkki
Windowsiin`**, eli reitti työpöydälle on valikossa sellaisenaan. Driveä ei näkynyt näkyvissä
olleessa osassa; valikko jatkuu vierittämällä eikä sitä vieritetty. **Mitään ei valittu**,
koska valinta on lähetys ulos ja se on Tommin teko. Analyysiohjelmaa laitteella ei ole,
joten se puoli on todentamatta.

**Tommin kuittaus 3.9.2026 heti puolenyön jälkeen:** *"desktop-tietokoneelle tallennus
onnistui."* Reitti työpöydälle on siis todennettu päästä päähän, ja se oli koko avaamisen
peruste. Kohde oli `Linkki Windowsiin` ja sen takana työpöytäkone `Tommikki`, eli sama
kohde joka näkyi valikon ensimmäisenä. Tiedosto meni siis suoraan siihen koneeseen jossa
analyysiohjelma on, ilman pilveä välissä.

~~**Tunnustettu hinta:** rivi kasvoi napin minimikorkeuden verran (noin 40 dp) myös silloin
kun nappi on rivin ainoa lisä.~~ **Korjattu 3.9.2026 Tommin pyynnöstä** (*"nappi vastustajan
riville, rivi takaisin entiseen korkeuteen"*). Linkki on nyt vastustajan rivin oikeassa
laidassa alleviivattuna tekstinä eikä `TextButton`ina, joten rivillä ei ole napin
minimikorkeutta ja se on kahden tekstirivin korkuinen kuten ennen. Alleviivaus on sama
lupaus kuin linkkisivulla (*Alleviivaus on lupaus, väri ei*): tämä tekee jotain, naapurit
porautuvat. Nähty laitteella: 21 päättynyttä riviä yhdellä ruudullisella, linkki jokaisella.

### Turnaukseen ilmoittautuminen ja sen peruminen (Tommin tilaus 3.9.2026)

Kaanonimuutos: `SUBSTANSSI.md` kohta 52 sanoi 16.8.2026 että turnauksiin ilmoittaudutaan
selaimella eikä sitä tarvita tähän sovellukseen. Tommi tilasi toteutuksen 3.9.2026
(*"turnauksiin liittymisen voisi toteuttaa nyt"*), portti näytettiin ja hän vahvisti
(*"Vahvistan, Sign Up ja Cancel"*). Kohta 52 kantaa käännöksen päivättynä.

**Mitä ruudulla on.** Loungen Offers-segmentin `Tournament sign-up` -listan rivillä on
sivun oma linkki nappina: `Sign up` kun ilmoittautuminen on auki, `Cancel sign-up` kun
siihen on jo ilmoittauduttu, eikä mitään kun rivillä ei ole linkkiä. Kolmas muoto mitattiin
samana päivänä (`docs/KOHDE.md`, `Cancel Signup`), ja se on syy siihen että peruutus tuli
samalla: ilman sitä sovellus olisi voinut sitoa muttei irrottaa. Rivin nimen perässä lukee
*(has note)* kun sivulla on `Has Note` -solu, koska järjestäjän teksti on tarkoitettu
luettavaksi ennen ilmoittautumista (`SUBSTANSSI.md`) ja se on turnaussivulla johon rivi vie.

**Sign up kysyy, Cancel ei.** Sign Up on peruttavissa rivin omalla linkillä, mutta se sitoo
useaan otteluun kuukausien ajaksi ja tuntuu halvalta juuri painettaessa (`SUBSTANSSI.md`
kohta 53). Dialogi nimeää turnauksen, pelimuodon, pituuden ja kierrokset, sanoo että
peruutus on tässä listassa turnauksen alkuun asti, ja mainitsee Has Noten erikseen.
Cancelin voi perua ilmoittautumalla uudestaan, joten se lähtee suoraan kuten pelaajahaku.

**Onnistuminen luetaan loungen riviltä eikä vastaussivulta.** Kumpaakaan vastausta ei ole
kaapattu, joten sivun sanamuotoon ei nojata kuten Joinissa (`isJoinConfirmationPage`). Sen
sijaan lounge luetaan lähetyksen jälkeen (vastauksesta jos se on lounge, muuten yhdellä
uudelleenhaulla) ja rivin linkki kertoo itse kumpi tila on voimassa: Sign Upin jälkeen
rivillä on `Cancel Signup`, peruutuksen jälkeen `Sign Up`. Se on sama todiste jonka
käyttäjä näkisi selaimessa. Kadonnut rivi ei todista peruutusta, koska turnaus on voinut
myös alkaa, ja silloin tila on `Unconfirmed` varoitusvärillä. Samat ehdot kuin Joinilla
muuten: lähetys on sivun oma href sellaisenaan, yksi lähetys kerrallaan (Join ja Sign Up
sulkevat toisensa), ei uudelleenlähetystä katkolla eikä katkenneella istunnolla.
Onnistunut teko hakee omien turnausten segmentin uudelleen, jos se on jo haettu.

**Ensimmäinen oikea Sign Up ajettiin proxyn kanssa samana iltapäivänä.** Vastaus on lounge
itse, jonka rivillä on jo `Cancel Signup` (`docs/KOHDE.md`), eli ensimmäinen haara riitti
eikä uudelleenhakua tarvittu. Rivitodiste jää silti: se on totta riippumatta siitä mitä
vastaussivu sanoo, ja Cancelin vastaus on yhä mittaamatta.

### Loungen Tournaments on pelaajan omat turnaukset (Tommin havainto 3.9.2026)

*"Lounge-sivun Tournaments pitäisi olla pelaajan omat turnaukset eikä kaikki turnauksia."*
Segmentti näytti 27.8.–3.9.2026 Tournament Hallin, joka on koko sivuston lista (2848
aktiivista riviä mittaushetkellä, Tommin nimi kerran). Segmenttirivin peruste oli että
kolme kohdetta vastaavat samaan kysymykseen kenen kanssa ja mihin, ja hallin kohdalla vastaus
oli kaikkien muiden turnaukset.

**Mitä muuttui.** Tournaments hakee nyt `/bg/userevent/<id>`:n, ja polku tulee Top Pagen
alalaidan *active tournaments* -linkistä (`TopPage.activeTournamentsPath`, luettu 3.9.2026
alkaen) eikä loungesta. Se on sama sivu jonka profiilin *active tournaments* avaa
porautumisruudulla, ja rivit ovat samat (`PageScreen.PlayerTournamentRowView` on jaettu).
Otsake kertoo rivimäärän, tyhjä lista ohjaa Offersiin. Haku on yhä laiska: sivu haetaan
vasta kun segmentti avataan, ja Refresh koskee auki olevaa segmenttiä.

**Halli ei kadonnut vaan siirtyi.** Segmentin alalaidassa on *Tournament Hall* sivun omalla
sanalla ja selitteellä, ja se avaa hallin porautumissivuna (`PageContent.Hall`, sama
aktiiviset ensin -järjestys ja sama alalaidan huomautus kuin ennen). Se on linkki eikä
neljäs segmentti, koska halli ei ole rinnakkainen omien kanssa vaan koko sivuston lista.
`hall_no_path`-teksti poistui, koska hallilla ei ole enää omaa tilaa.

**Sign-Up-lista pysyi Offersissa.** Se on loungen sivulla eikä omien turnausten sivulla, ja
Refresh hakee yhden sivun per segmentti. Jos ilmoittautumislista tuntuu kuuluvan
Tournaments-segmenttiin, se on erillinen päätös.

### Asetusruutu vierii kokonaisuutena (Tommin havainto 3.9.2026)

*"Info-näkymässä Dailygammon-asetukset ei ole vaaka-tilassa näkyvillä eikä vieritettävissä."*
Laiteosio oli kiinteä ruudun yläosassa ja sivuston lomake vieri omassa painollisessa
laatikossaan sen alla. Tabletin vaakatilassa (pystylukko pois, 2.9.2026) laiteosion kuusi
tyyliriviä ja kolme kytkintä täyttivät koko korkeuden, joten laatikolle ei jäänyt riviäkään
eikä mikään vierinyt, koska laiteosio ei ollut vierityksen sisällä.

**Korjaus on yksi vieritys koko sarakkeelle.** Laiteosio, jakoviiva ja sivuston tila
vierivät yhdessä, ja lomake on pelkkä sarake ilman omaa vieritystä. Lataus- ja virhetilat
piirtyvät sisältönsä korkuisina laiteosion perään. Laiteosion asema tilahaarojen
ulkopuolella ei muuttunut: se on käytettävissä silloinkin kun sivuston lomake ei latautunut.

### Latauspalkin paikka on varattu aina (Tommin havainto 3.9.2026)

*"Kun sivu lataa, niin näyttö nytkähtää ylhäälle ilmestyvän latauspalkin toimesta."* Palkki
oli yhdeksässä ruudussa ehdollinen (`if (refreshing) LinearProgressIndicator()`), joten se
ilmestyi sarakkeen alkuun ja työnsi kaiken alapuolisen neljä dp:tä alas, ja latauksen
päättyessä sama toiseen suuntaan. Nytkähdys osui juuri siihen hetkeen kun käyttäjä luki
tai napautti, koska Refresh ja lähetys ovat käyttäjän omia tekoja.

**Korjaus on Tommin ehdottama: tila varataan aina.** `ProgressSlot` (`Notes.kt`) on
neljän dp:n korkuinen paikka joka on aina sarakkeessa ja jossa palkki syttyy ja sammuu
paikallaan. Sama koti kaikille yhdeksälle ruudulle, myös laudalle: laudan korkeudesta
lähtee neljä dp pysyvästi sen sijaan että se lähtisi latauksen ajaksi. Korkeus on
kirjoitettu paikkaan eikä luettu komponentilta, koska Material ei anna sitä vakiona.

### Napit pois heti ja kaari odotuksen ajaksi (Tommin tilaus 16.9.2026, kokeilu)

Vertailumittaus 6 (`raakasivut/LUEMINUT.md` › *Pelisessio 16.9.2026 klo 18.02*) mittasi
vasteen eron DG Mobileen 0,1 s:ksi tekoa kohti, ja mittaus 3 samana yönä oli mitannut eron
palautteessa: DG Mobile tyhjentää napit painalluskehyksessä ja pyörittää kaarta koko
odotuksen, tämä sovellus näytti ripplen 0,3 s ja seisoi sitten liikkumatta. Yläreunan
neljän dp:n palkki (yllä) ei lue odotukseksi laudan äärestä. Tommi valitsi kokeiltavaksi
palautteen, koska se on UI-muutos eikä jäsentimen uudelleenkirjoitus.

**Toteutus on `actionItems`in yksi haara.** Kun `Loaded.refreshing` on tosi, toimintorivi
(keskikaista) ja paneelipino saavat nappien sijaan yhden alkion, `BusyArc`: nappien kokoinen
laatikko jonka keskellä pyörii 26 dp:n kaari nappulavärillä. Sivun napit, vahvistusruutu,
peruminen, komennot ja rivin omat lisät (`trailing`) ovat kaikki pois, ja ne palaavat kun
uusi tila on piirretty. Laatikko on nappien kokoinen, jotta rivi tai pino ei vaihda
korkeuttaan. Chat-kortin napit eivät ole tämän piirissä. Todennettu kuoriproxylla 1,5 s:n
viiveellä (`--viive`, lisätty samalla): napit katosivat painalluskehyksessä, kaari pyöri
odotuksen ja napit palasivat vastauksen kanssa.

**Tulos samana iltana, kuuden ottelun sessio** (`sessio-16-9-ilta3`): *"vaste tuntui
vähäisemmältä kuin aiemmin"*, vaikka mitattu vaste oli sama luokka (mediaani 0,85 s
`piirretty`, palvelin 0,56 s). Tuntuma oli siis palautteessa eikä vasteessa, ja kokeilu jää
voimaan. Toinen päätös samalla: *"latauskaari tekee ylimpänä olevan latausviivan
tarpeettomaksi"*, joten laudalta poistettiin `ProgressSlot`. Yllä oleva jakso *Latauspalkin
paikka on varattu aina* koskee 16.9.2026 alkaen muita kahdeksaa ruutua; lauta sai neljä dp
korkeutta takaisin.

**Kaari sai liukuvärin ja seuraa lautaa** (Tommin tarkennukset samana iltana: *"latauskaari
kaipaa väriä"*, *"ajattelin gradienttia väriä"*, neljästä pyörivästä vaihtoehdosta A eli
laudan värit, ja kysymykseen *"1, kaari seuraa lautaa"*). `BusyArc` piirtää pyörivän
pyyhkäisyliukuvärin läpinäkyvästä kiilan oranssin ja oman nappulan värin kautta kuution
vaaleaan tealiin. Kiila ja nappula luetaan `ArcLook`iin tämän laudan `BoardLook`ista ja
`BoardRoles`ista, joten Monte Carlo variantilla ja sivustouskollisella laudalla kaari on sen
laudan sävyissä; teal on kaikilla sama, koska kuutio ei kulje tyylissä. Todennettu Monte
Carlo variantilla kuoriproxylla 16.9.2026 klo 20.16 (`raakasivut/sessio-16-9-ilta3/kuori/
kaari-monte-carlo-variant.png`): kaari kulki variantin oranssista kerman kautta tealiin,
eli oma nappula luettiin valkoisena eikä X-22:n kermana, ja napit olivat pois odotuksen
ajan samoin kuin X-22:lla. Tabletin tyyli palautettiin X-22:een ajon jälkeen.

**Kaksi näyttävämpää muotoa kokeiltavana, valinta auki (18.9.2026).** Tommin kysymys
*"onko latauskaarelle näyttävämpiä vaihtoehtoja"* sai ensin neljä yleistä vaihtoehtoa ja
sitten kolme Tommin omaa toivetta (tuplauskuutio Android-tahkolla, kaksi noppaa ja kuutio,
ouroboros), kaikki liikkuvina esikatseluina chatissa. Tilaus *"rakenna ouroboros ja kuutio
pelin väreillä, android 64:n tilalle"*. `BusyIndicator` valitsee `BUSY_STYLE`-vakiolla
kolmesta: `BusyArc` (nykyinen), `BusyOuroboros` (käärme kiertää kehää, ruumis paksunee
hännästä päähän, väri kiilan parillisesta parittomaan ja takaisin `ArcLook.wedgeOdd`in
kautta, silmät kuution tealia) ja `BusyCube` (kuutio kääntyy litistyksellä tahkolta
tahkolle 2, 4, 8, 16, 32 ja robotti, samat piirrot kuin laudan kuutiolla). Ensimmäinen
laiteajo 26 ja 22 dp:ssä oli Tommin sanoin liian pieni (*"animaatiot saisi olla isompia"*),
joten molemmat vuotavat nappilokeron yli `requiredSize`-mitalla, 60 ja 52 dp; odotuksen
aikana kaikki napit ovat poissa, joten tila on olemassa. Todennettu kuoriproxylla tabletilla
kummastakin koosta (`raakasivut/sessio-18-9-kuori`, nauhat ja lähikuvaruudukot, LUEMINUT).
Kaanonihuomio kuutiosta: laudan kuutiossa robotti on ykköspinta (*kukaan ei ole tuplannut*),
lataajassa se on 64:n paikalla Tommin sanoin; ero on tarkoituksellinen, koska lataaja ei ole
kuution arvo. Samana iltana vakiosta tuli laiteasetus `busy_style` (Tommin tilaus
*"haluaisin busy-style asetuksen sovellukseen"*, `docs/ASETUKSET.md` luku 3), asetusruudun
ryhmä *Waiting for DailyGammon* kolmella rivillä, tuotuna lautaan `LocalBusyStyle`lla.
Kaari nostettiin samalla 60 dp:hen ja viiva 5 dp:hen, jotta kolme muotoa vertautuvat
samassa mitassa (Tommin kysymys *"onko latauskaari samaa kokoluokkaa kuin uudet?"*), ja
kuutio hidastettiin 700 → 840 ms tahkoa kohti (*"hidasta kuutiota 20%"*), ja saman illan
pelisession jälkeen takaisin 700:aan (*"hidastus oli virhe, nopeuta 25% eli alkuperäiseen
tahtiin"*, 18.9.2026 klo 20.35). Laudan
ensilatauksen keskellä oleva kaari oli Materialin oma 21.9.2026 asti, ks. alla.

**Asetus yltää tyhjän ruudun keskikaareen (Tommin tilaus 21.9.2026, *"haluaisin että asetus
yltää niihin asti"*).** Sovelluksessa oli kaksi muuta odotuksen ilmaisinta, kumpikin
Materialin oma: tyhjän ruudun keskellä pyörivä kaari (14 paikkaa: laudan ensilataus ja
Top Page -paluu, luettelo, Lounge, pelaajalista, foorumin neljä tilaa, sivunlukija,
turnaukset, asetusten lataus ja kirjautumisen odotus) ja Refreshin 4 dp:n vaakapalkki
otsakkeen alla (`ProgressSlot`, 9 ruutua). Neljästä vaihtoehdosta Tommi valitsi
keskikaaren: se vaihtuu valittuun muotoon (`BusyCentered`, X-22:n väreissä koska
luettelolla ei ole lautatyyliä), ja vaakapalkki jää, koska sen paikka on mitoitettu
nytkähdyksen takia (3.9.2026) eikä 48–60 dp:n hahmo mahdu siihen. Tila asuu
`MainActivity`n juuressa ja tarjotaan `LocalBusyStyle`lla koko sovellukselle, joten
asetusruudun muutos näkyy heti palattaessa; `BoardScreen`in oma `busyStyle`-parametri
poistui samalla. Ääretön-käärmeen rata on laudan ulkopuolella kehyksen puuta eikä
reunaviivaa (Tommin päätös samana yönä kaappauksesta `07-zoom.png`: reunaviiva ei
erottunut luettelon taustasta), lokerossa reunaviiva kuten ennen.

**Asetus jää kolmella arvolla** (Tommin päätös 18.9.2026 klo 19.18: *"asetus kolmella
arvolla"*). Kaaren 60 dp nähtiin pelisessiossa samana iltana ja kuitattiin (*"latauskaaren
koko oli hyvä"*). Kuution robottia Tommi ei nähnyt kertaakaan, ja syy on mitattu
(`raakasivut/LUEMINUT.md`, sessio-18-9-ilta2): kierros on 6 × 840 ms = 5,0 s, robotti on
viimeinen tahko, ja odotukset olivat 0,3–3,3 s. Ratkaisu samana iltana (Tommin päätös
*"kierros alkaa robotista"*): tahkojärjestys on robotti, 2, 4, 8, 16, 32, joten robotti näkyy
jokaisen odotuksen ensimmäisen tahkon ajan (700 ms 18.9. illasta). Asetusruudun rivi sanoo *robot first*. Todennettu kuorella samana iltana
(`raakasivut/LUEMINUT.md`, kuoritesti 19.34). Samat kehykset paljastivat käännösvian: tahko
vaihtui kolmanneksessa ja palasi kahdessa kolmanneksessa, joten seuraava tahko vilahti kesken
litistyksen. Korjattu vaihtumaan viivan kohdalla (`u < 0.5f`), todennettu toisella nauhalla.

**Käärme pelissä 18.9.2026 klo 17.33–17.37** (`raakasivut/LUEMINUT.md`): Tommi
*"pelasin käärmeellä, oli hyvä"*. Valinta kolmen välillä on yhä auki, ja asetus jää.

**Neljäs muoto: käärme ääretön-merkillä (Tommin tilaus 21.9.2026).** Pyyntö oli *"toinen
ouroboros odotus-animaatioksi, tässä käärme olisi ääretön symboli"*, ja kolmesta liikkuvasta
vaihtoehdosta chatissa (A yksi käärme koko radalla liukuvärillä, B lyhyt käärme ja rata
näkyvissä, C kaksi käärmettä kumpikin toisen häntä suussa) Tommi valitsi B:n A:n värityksellä
ja päätti että se on asetuksen neljäs arvo eikä kehäkäärmeen korvaaja (kaanonimuutos
kolmesta arvosta neljään, `docs/ASETUKSET.md`). `BusyInfinity` piirtää Bernoullin
lemniskaatan tasaväliseksi pistejonoksi (kaarenpituuden mukaan, ei parametrin, jotta käärme
ei hidastu ristikohdassa) ja liu'uttaa sillä käärmettä joka peittää radasta puolet: rata
reunaviivan värillä, ruumis paksunee hännästä päähän ja väri kulkee kiilan parillisesta
parittomaan ja takaisin, suomut ja pää samat kuin kehäkäärmeellä. Ero kehään on että
käärme ei pyöri vaan liukuu, joten pää käy vuorotellen kummassakin lenkissä ja ristikohdassa
ruumis kulkee itsensä yli. Kierros 1,8 s. Mitta 84 × 48 dp, koska kahdeksikko on leveä ja
matala; korkeus on sama luokka kuin kuution 52.

### Ottelukutsu ja sivuutus profiililta (Tommin tilaus 3.9.2026)

Tilaus oli *"Kutsulomake, Ignore, Resign matches toiminnot"*. Kaksi ensimmäistä tehtiin,
kolmas odottaa mittausta (`docs/AVOIMET.md`, `SUBSTANSSI.md` kohta 36). Molemmat olivat
27.8.2026 rajattu ulos peruuttamattomina tekoina, ja rajaus purettiin tilauksella; kaanonia
ei rikottu, koska `SUBSTANSSI.md` ei sano kutsusta eikä sivuutuksesta mitään kieltävää.

**Kolme nappia samalla rivillä profiilin yläosassa**: `Message`, `Invite to a match` ja
sivuutuksen nappi sivun omalla tekstillä (`Ignore <nimi>`). Teksti on sivun oma, koska vain se
kertoo suunnan: `changeto`-piilokenttä sekä asettaa että poistaa (`docs/KOHDE.md`), eikä
sovellus tiedä kumpi on kyseessä muuten kuin napista. Napit ovat vain toisen pelaajan
profiililla, ja ehto on lomakkeen olemassaolo sivulla eikä päättely siitä kenen sivu on,
samoin kuin viestillä.

**Kutsulomake on sivun oma kokonaisuudessaan** (`InviteForm`, `ProfileForms`): kolme valintaa
(`variant`, `length`, `time_control`) sivun vaihtoehdoilla ja esivalinnoilla (`backgammon`,
5, `Never`), kaksi tekstikenttää (`comment` 80, `name` 40) ja `Private Match` -ruutu. Valinnat
ovat alasvetovalikoita napin muodossa, ja tekstikentän pituus rajataan sivun `maxlength`iin
kentässä, koska palvelimen vastausta pidempään kutsutekstiin ei ole mitattu (viestikentästä
on, ja siksi se ei rajaa). `InviteForm.write` palauttaa nullin arvolle jota sivu ei tarjonnut,
eli sovellus ei voi lähettää keksittyä arvoa. Lähetys kysyy vahvistuksen samalla tavalla kuin
loungen Join: kutsu on peruuttamaton teko oikealle ihmiselle.

**Kutsun vastaussivua ei ole mitattu, ja se sanotaan ruudulla.** Tila on `Answered`, ei
`Done`: DailyGammon vastasi, ja jos vastaussivun alussa on lauseen mittainen teksti (enintään
160 merkkiä, `SendResultParser.notice`), se näytetään sivun sanoin. Profiili haetaan
uudelleen jotta ruutu ei jää vanhaan. Ensimmäinen oikea kutsu ajetaan proxyn kanssa, ja
tunnistus tarkentuu siitä, sama tie kuin turnauksen Sign Upilla 3.9.2026.

**Sivuutus on GET sivun piilokentillä, ja onnistuminen luetaan napin vaihtumisesta.**
Vastaus kelpaa profiiliksi jos se on profiili, muuten yksi uudelleenluku (sama kaava kuin
loungen Sign Upilla), ja `Done` vaatii että `changeto` on eri kuin lähetetty. Ilmoitus
toistaa napin uuden tekstin sivun sanoin. Vahvistusdialogin otsikko ja nappi ovat sivun oma
teksti, ja runko sanoo ettei sovellus ole mitannut mitä sivuutus tekee. Sävy on kohdan 36
mukainen: sivuston tavallinen nappi, ei varoitusta.

**Mitä ei tehty.** Sivuutuksen vaikutusta (viestit, kutsut, näkyvyys) ei mitattu, eikä
kutsun peruutusreittiä tiedetä; molemmat ovat `docs/KOHDE.md`:n kysymyksiä. Yksi teko
kerrallaan koskee myös viestiä: kutsu, sivuutus ja viesti eivät kulje rinnakkain.

**Todennettu SM-T970:llä 3.9.2026 lähettämättä mitään:** toisen pelaajan profiililla on
kolme nappia, kutsulomake avautuu esivalinnoin ja sivun vaihtoehdoin, vahvistusdialogi
kertoo pelaajan, lajin, pituuden ja aikakontrollin sivun sanoin, ja sivuutuksen dialogi
kantaa sivun napin tekstin. Kumpikin dialogi peruttiin. Ensimmäinen oikea lähetys on Tommin.

### Luovutussivu Info-välilehdellä (Tommin tilaus 3.9.2026)

Tilauksen kolmas osa (*"Resign matches toiminnot"*), tehty samana iltapäivänä kun Tommi oli
avannut `/bg/resign`-sivun proxy-Chromella lähettämättä mitään. `SUBSTANSSI.md` kohta 36 vaati
mittauksen ennen kosketusta, ja mittaus on `docs/KOHDE.md`:ssä.

**Paikka on Info-välilehden rivi *Resign matches*, ei otteluluettelon nappi.** Luovutus on
sivustolla tavallinen valikkotoiminto navigointipalkin linkin takana, ja kohta 36 sanoo että
käyttöliittymä joka kohtelisi sitä dramaattisena olisi eri maailmasta kuin kohde. Rivi avaa
saman porautumisruudun kuin sivuston Help ja Links, vakiopolulla `/bg/resign`, ja laji luetaan
vastauksesta (`ResignParser`, `PageContent.Resign`).

**Ruutu on sivun oma lomake sellaisenaan.** Rivit ovat keskeneräiset ottelut valintaruutuineen
(tapahtuma, vastustaja, kierros, pituus, ja `Moves`-linkki sivun Review-osoitteeseen), niiden
alla sivun oma varmistusruutu sivun omalla tekstillä (*Yes! I really want to resign.*) ja nappi
sivun omalla tekstillä (*Resign Selected Matches*). Nappi on käytössä vasta kun jokin rivi on
valittu ja varmistusruutu ruksattu, eli samat ehdot jotka sivu itse asettaa. **Omaa
vahvistusdialogia ei ole**, toisin kuin Joinilla, kutsulla ja Sign Upilla: sivun oma ruutu on
jo se varmistus, ja toinen päälle olisi juuri sitä draamaa jota kohta 36 kieltää. Rivit luetaan
samalla lukijalla kuin profiilin ottelutaulukot (`ProfileParser.parseRow`), koska sarakkeet
ovat samat.

~~**Vastaussivua ei ole mitattu, ja onnistuminen luetaan listasta.**~~ **Vastaus mitattiin
4.9.2026 illalla** (`docs/KOHDE.md`): se on oma `Post Resignation` -sivunsa, jossa lukee
`1 match resigned.`, eikä siinä ole lomaketta.

**Sivuston oma lukema on nyt ensisijainen** (Tommin päätös samana iltana). Ruutu sanoo
`Matches resigned. DailyGammon says: 1 match resigned.`, eli sama muoto kuin kutsulla:
sovellus kertoo mikä onnistui ja sivusto kertoo mitä se teki. Luku luetaan lauseen
ensimmäisestä kokonaisluvusta eikä sen sanoista, koska monikkomuotoa ei ole nähty ja yksi
näyte ei kanna arvausta siitä.

**Tuore lista haetaan silti**, ja se on tämän muutoksen ainoa hinta: kuittaussivulla ei ole
rivejä, joten ruutu jäisi ilman sitä näyttämään juuri luovutettuja otteluita. Pyyntö on siis
sama kuin ennen, ja muuttunut osa on se mistä lopputulos luetaan.

**Kadonnut rivi jäi varareitiksi.** Jos vastaus ei ole kuittaussivu, onnistuminen luetaan yhä
listasta ja ruutu käyttää vanhaa tekstiä. Kaksi todistetta eivät ole yhtä vahvoja, ja ruutu
kertoo kummasta on kyse. Nollaa ei ole nähty: jos sivusto sanoisi luovuttaneensa nolla
ottelua, tila on `Unconfirmed` eikä sille keksitä omaa tekstiä.

Alkuperäinen kirjaus jatkuu.
**Vastaussivua ei ole mitattu, ja onnistuminen luetaan listasta.** `POST /bg/resign/doit`
lähettää valitut ruudut arvolla `on` ja `reellysure=on`, kuten selain. Vastaus kelpaa
luovutussivuksi jos se on luovutussivu, muuten sivu haetaan uudelleen, ja `Done` vaatii että
**yksikään valittu ottelu ei ole enää listalla**. Todiste on kadonnut rivi, ja se on pätevä
koska sivu listaa kaikki keskeneräiset ottelut eikä vain vuorossa olevia (38 riviä
kaappauksessa, otteluluettelossa 5). Ensimmäinen oikea luovutus on Tommin, ja se kannattaa
ajaa proxyn kanssa, jolloin vastaussivu tulee mitatuksi.

**Todennettu SM-T970:llä 3.9.2026 lähettämättä mitään:** Info-rivi avasi sivun, lista näytti
keskeneräiset ottelut, nappi pysyi harmaana kunnes rivi ja varmistusruutu oli ruksattu, ja
valinta purettiin ennen kuin ruudulta poistuttiin.

### Noppien painallus tekona (Tommin tilaus 4.9.2026)

Omat nopat ovat kytkettävissä napiksi, kahdella laitteen omalla asetuksella. Tilaus oli
kaksiosainen: *"kun kaikki nopat on harmaita, niin noppia painamalla toteutuu Submit move"*
ja *"kun yksikään noppa ei ole harmaa, niin noppia painamalla toteutuu Swap Dice"*.
Molemmat ovat oletuksena pois, ja säilöt ovat `docs/ASETUKSET.md` luvussa 3.

**Painallus on ryhmällä eikä yksittäisellä nopalla.** Kumpikin sääntö puhuu kaikista
nopista, joten teko ei riipu siitä mihin noppaan osui. Vastustajan lokero ei ota
painallusta vastaan lainkaan.

**Ehto luetaan kokoamisesta eikä väristä, ja kaksi rajaa kysyttiin ennen toteutusta.**
Lähetys laukeaa kun vuoro on täysi, myös silloin kun toinen noppa jää pelaamatta; se on
pakotetun vuoron tavallisin muoto, ja siinä noppa jää harmaantumatta vaikka enempää ei voi
tehdä. Ja koska harmaus on paikallisen kokoamisen ominaisuus, sivun omalla laudalla
painallus ei tee mitään. Kesken kokoamisen kumpikaan ehto ei täyty, eli painallus on siinä
välissä äänetön.

Sääntö on yhtenä funktiona (`BoardScreen.diceTapFor`), ja lokero saa valmiin lambdan.
Piirtokohta ei siis tunne kokoamisen tiloja, samasta syystä kuin `cubeActionFor`illa:
piirtokohtia on kaksi, ja kahteen kirjoitettu ehto on kaksi ehtoa.

**Lähetys ei kysy vahvistusta.** Sama teko `Submit Move` -napista ei kysy sitäkään
(`IRREVERSIBLE_SUBMITS` ei sisällä sitä), ja kysyminen vain toisessa reitissä tekisi
kahdesta reitistä eri mieliset. Suoja on kytkin, joka on oletuksena pois. Vaihto ei mene
verkkoon lainkaan: se on kokoamisen oma `local:`-osoite, ja saman painalluksen toisto
kääntää järjestyksen takaisin.

### Vanhentuneet ottelurivit piiloon haun ajaksi (Tommin havainto ja päätös 4.9.2026)

Havainto pelisession päätteeksi: *"ottelulistan tyhjennyttyä Match näyttö vaikuttaa
levottomalta lataillessaan ottelutietoja, joita ei pitäisi olla."*

**Mekanismi luettiin lokista ja koodista, ei ruudulta.** Illan kaappauksessa (216 sivua,
`raakasivut/sessio-4-9`) oli session lopussa tasan yksi `/bg/top`-haku, joten sovellus ei
hae mitään toistuvasti. Levottomuus tuli siitä mitä ruudulla oli sen yhden haun ajan. Kun
viimeinen vuoro päättyy, sivusto vastaa Top Pagella, lautanäkymä tunnistaa sen ja sulkeutuu,
ja luettelo haetaan tuoreena. `refresh` säilytti edellisen sivun ja nosti latauspalkin, eli
ruudulla oli koko haun ajan juuri ne ottelut jotka oli äsken pelattu.

**Ero verkkovirheeseen on tieto eikä maku.** Sääntö *"epäonnistunut päivitys ei saa hävittää
jo näkyvää listaa"* on voimassa eikä muuttunut: vanhentunut lista on enemmän kuin virheruutu
silloin kun haku kaatuu. Tässä sovellus tietää haun **alkaessa** että lista on vanhentunut,
koska se hakee juuri siksi että lauta kertoi ottelun päättyneen. Näkyvä lista on silloin
väite jota mikään ei tue.

**Ratkaisu on rivien piilotus haun ajaksi, ja yläosa jää.** Piilossa ovat ottelurivit,
sarakeotsikko, odottavien rivi ja viestien odotusilmoitus. Paikalle ei tule mitään, ei myöskään tyhjän listan tekstiä,
koska sekin olisi väite siitä mitä ei vielä tiedetä; latauspalkki yläreunassa kertoo mitä on
kesken. Nimi ja arkiston reunapäivä pysyvät paikallaan, eikä niiden katoaminen tekisi
ruudusta rauhallisempaa vaan tyhjemmän.

**Odottavien rivi lisättiin piilotukseen samana päivänä** (Tommin päätös, sama vuoro).
Ensimmäinen toteutus jätti sen paikalleen, koska päätös oli *rivit piiloon ja yläosa jää*, ja
rivi on yläosaa. Se on silti ottelutietoa ja luetaan samalta vanhentuneelta sivulta, joten se
olisi sanonut vanhan luvun listan yläpuolella jota ei ole. **Raja kulkee lähteessä eikä
sijainnissa:** piilossa on se mikä tulee juuri haettavana olevalta sivulta, ja paikallaan se
mikä ei. Arkiston reunapäivä tulee laitteen omasta kannasta, joten se jää.

**Ehto on kutsujan tieto eikä luettelon oma** (`TopViewModel.refresh(matchesKnownStale)`).
Käsin painettu `Refresh` ei piilota mitään, koska silloin lista voi hyvinkin olla ajan
tasalla ja pyyhkiminen olisi vain toinen välähdys. Kaatunut haku palauttaa rivit näkyviin,
jottei ruudulle jää pelkkää yläosaa ilman tietä eteenpäin.

**Viestien odotusilmoitus tuli mukaan kolmantena** (Tommin päätös, sama vuoro). Se ei ole
ottelutietoa, ja juuri siksi se on hyvä koetin säännölle: ratkaiseva ei ole tiedon laji vaan
se mistä tieto tulee. Ilmoitus luetaan samalta vanhentuneelta sivulta kuin rivit, joten se
kuuluu piiloon.

~~**Ruudulla on haun ajan siis kolme asiaa:**~~ **Vanhentunut samana iltana**, ks. *Vanhentunut
Matches on odotusruutu eikä vaillinainen lista* alempana: yläosa katosi lopunkin osalta.
Alkuperäinen teksti seuraa muuttamattomana. **Ruudulla on haun ajan siis kolme asiaa:**
nimirivi, latauspalkki ja arkiston reunapäivä.
Kaikki muu tulee juuri haettavana olevalta sivulta ja odottaa vastausta. Sääntö on yhdellä
ehdolla (`matchesStale`) neljässä kohdassa, ja se on tietoinen toisto: kohdat ovat eri
komponentteja saman sarakkeen sisällä, eikä niillä ole yhteistä kääretta johon ehdon voisi
nostaa piirtämättä ruutua uusiksi.

### Vahvistusdialogeilla on yksi runko (Tommin päätös 4.9.2026)

Peruuttamattoman teon vahvistus oli kirjoitettu auki kuudesti: laudan kuutioteko, palstan
lähetys, loungen Join ja Sign Up sekä profiilin Invite ja Ignore. Kaavahaku K8 nimesi viisi
(`docs/AUDITOINTI-KOMPOSITIO.md`) ja jätti kysymyksen auki, koska nosto koskettaa viittä
ruutua kerralla ja tämä dokumentti kuvaa dialogit ruutukohtaisesti. Tommin päätös oli
korjata koodi.

**Nosto koskee runkoa eikä sanoja, ja juuri se pitää tämän dokumentin voimassa.**
`ConfirmDialog` omistaa `AlertDialog`in, otsikon paikan, tekstin paikan ja kaksi nappia.
Se ei tiedä mitä dialogi sanoo eikä milloin se avataan: kutsuja antaa otsikon, tekstin ja
vahvistusnapin sanan. Jokainen dialogi on yhä oma nimetty `@Composable` omalla
perustelullaan siinä ruudussa johon se kuuluu, joten ylempänä olevat ruutukohtaiset
kuvaukset kertovat yhä sen mitä käyttäjä näkee.

**Vahvistusnapin sana pysyy sivun omana sanana** siellä missä teko on sivuston teko:
`Double`, `Accept`, palstan `submitLabel` ja sivuutuksen napin teksti tulevat sivulta.
Sääntö on *Sivusto nimeää teot, sovellus nimeää paikat*, ja se on kutsujan tiedossa eikä
rungon, joten nappi on tavallinen merkkijono.

**Peruutusnappi on oletuksena `Cancel`**, ja yksi poikkeus jäi ennalleen: palstan lähetys
sanoo *Not yet*, koska siellä peruutus tarkoittaa keskeneräistä tekstiä eikä hylättyä
aikomusta. Poikkeus näkyy kutsupaikassa yhtenä nimettynä parametrina, eli sitä ei tarvitse
etsiä. Laudan oma `board_double_cancel` poistettiin: se oli sama sana kuin `dialog_cancel`
eri nimellä, ja se on sama siivous kuin `dialog_ok`illa ja `dialog_cancel`illa 3.9.2026.

**Kaksi dialogia jätettiin ulos, ja raja on kysymisen ja kertomisen välissä.** Laudan
`UnconfirmedDialog` ja asetusten `SaveResultDialog` kertovat lopputuloksen jälkikäteen:
niissä on yksi nappi eikä kahta, eikä niillä ole peruttavaa tekoa. Sama runko olisi
mahtunut niihin muodoltaan mutta peittänyt sen eron.

### Vanhentunut Matches on odotusruutu eikä vaillinainen lista (Tommin päätös 4.9.2026 illalla)

Saman päivän ensimmäinen versio piilotti ottelurivit, sarakeotsikon, odottavien luvun ja
viestihuomautuksen mutta jätti nimirivin, latauspalkin ja arkiston reunapäivän. Laitteella
nähtynä se ei riittänyt: *"en ole tyytyväinen Matches-näytölle siirtymisen suhteen, ehkä
tyhjä näyttö olisi parempi kunnes ajantasaiset tiedot on haettu."*

**Ruutu on nyt haun ajan sama kuin kylmässä käynnistyksessä**, eli keskitetty odotusmerkki
tyhjällä alueella. Välilehtipalkki ja otsikkorivi (`Matches`, `Refresh`, `Sign out`) jäävät,
koska ne ovat navigaatiota eivätkä ottelutietoa, ja niiden katoaminen veisi tien eteenpäin
kesken odotuksen.

**Arkiston reunapäivä katoaa, vaikka se tulee laitteen omasta kannasta.** Se on tietoinen
poikkeus edellisen version sääntöön, jonka mukaan raja kulkee lähteessä eikä sijainnissa.
Sääntö vastasi kysymykseen mitä saa väittää sivustosta, ja tähän kysymys oli toinen: miltä
ruutu näyttää siirtymän aikana. Kolmen rivin yläosa tyhjän päällä on levottomampi kuin ei
mitään, ja odottaminen on selvempää kertoa yhdellä merkillä kuin jäännöksellä.

**Toteutus on yksi tila eikä neljän summa.** Ehdollisia kohtia oli neljä eri puolilla
luetteloa, ja nyt `matchesStale` valitsee ruudun ylimmällä tasolla: `WaitingForPage` on sama
`@Composable` kuin kylmällä käynnistyksellä, eli kaksi tilannetta jakavat ruudun eivätkä
muistuta toisiaan. Keskeytyneet lähetykset ovat yhä sen yläpuolella, koska ne ovat laitteen
tietoa siitä mitä käyttäjä yritti eivätkä sivun sisältöä.

**Raja on ennallaan:** vain automaattipaluu laudalta tyhjentää ruudun. Käsin painettu
`Refresh` jättää listan paikalleen, koska silloin vanha lista on paras arvaus eikä tiedossa
väärä, ja kaatunut haku palauttaa rivit näkyviin.

### Suunta käännetään ennen paluuta, ei sen jälkeen (Tommin havainto 4.9.2026 illalla)

Kolmas versio siirtymästä poisti tyhjän ruudun mutta ei liikettä: *"siirtymässä on edelleen
nytkähdys Matches-näkymässä, ihan kuin se ladattaisiin uudelleen."*

**Se ei ollut lataus vaan kääntyminen.** Lauta on vaakaan lukittu ja muut ruudut pystyyn, ja
suunnan asettava efekti oli avainnettu voimassa olevaan kohteeseen. Järjestys oli siis tämä:
paluu vaihtaa kohteen, otteluluettelo piirtyy **laudan vaakasuuntaan**, efekti huomaa
muutoksen ja pyytää pystyä, ja luettelo asettuu uudelleen. Haku oli siinä vaiheessa jo valmis,
joten mikään ei latatunut; liike oli asettelua.

**Käännös tehdään nyt ennen ruudun vaihtoa**, samassa kohdassa joka odottaa listaa. Silloin se
osuu odotusmerkin päälle, joka on keskitetty eikä muutu kääntyessä, eli kääntymistä ei näe.
Yleinen ohjaus asettaa saman arvon paluun jälkeen uudelleen, ja se on tarkoituksellinen toisto:
ohjaus on yhä yksi omistaja, ja tämä on sen ennakointi eikä kilpailija.

Sääntö itse on `orientationFor`, ja se nostettiin omakseen samalla. Sama arvo lasketaan nyt
kahdessa paikassa, ja kahtena kirjoituksena ne olisivat voineet erota juuri siinä tilanteessa
jota varten jälkimmäinen tehtiin.

### Odotusruutu kuuluu kohteelle, ei lähteelle (Tommin havainto 4.9.2026 illalla)

Kun suunta oli korjattu, jäljelle jäi kolmas havainto: *"ihan kuin välilehtipalkki olisi
poistunut alle sekunniksi."* Se oli tarkka kuvaus siitä mitä tapahtui.

**Odotus tapahtuu laudan reitillä, ja palkki on piilossa juuri siellä.** Portti on
reittipredikaatti, sama kuin suuntalukolla, ja se sanoi laudan reittiä laudaksi vielä
silloin kun lauta oli jo lakannut olemasta lauta. Ruudulla oli siis odotusmerkki ilman
kehystä, ja sovellus näytti hetken menettäneen välilehtensä.

**Paluun ajan palkki ja sävy ovat nyt kohteen mukaiset.** Lippu `returningToTop` on
asetettuna vain sen ajan kun tuoretta luetteloa haetaan, ja se tekee kaksi asiaa: palkki
piirtyy vaikka reitti on yhä lauta, ja aksenttiväri on otteluluettelon eikä perusteeman.
Ilman jälkimmäistä väri olisi vaihtunut ruudun vaihtuessa, eli sama nytkähdys toisessa
muodossa.

Nollaus tehdään reitin mukaan eikä paluun jälkeen. Eleellä keskeytetty odotus ei siis jätä
lippua päälle, ja muut ruudut saavat oman palkkinsa ja värinsä normaalisti.

**Kolme havaintoa, kolme eri syytä, ja se on tämän merkinnän opetus.** Vanhat rivit, kääntyvä
ruutu ja katoava palkki näyttivät ruudulla samalta levottomuudelta, mutta yksikään korjaus ei
osunut seuraavaan. Siirtymä on monta samanaikaista muutosta, ja ne on nimettävä yksitellen.

### Suunnan ennakointi purettiin: kaappaus kumosi perustelun (4.9.2026 illalla)

Edellinen merkintä sanoi että nytkähdys oli kääntymistä, ja korjaus oli asettaa suunta ennen
ruudun vaihtoa. **Perustelu oli väärä, ja sen kumosi yksi kaappaus laitteelta.**

Tabletin ruutu oli vaakana ja Matches piirtyi vaakana (ikkuna 2800x1752). Se on mahdollista
vain jos pystylukko on pois. Silloin paluun arvo on `UNSPECIFIED` eikä pysty, ja tabletti on
jo vaakana, joten **käännöstä ei ollut piilotettavana**. Asetus oli siis lukon vapautus kesken
paluun, eli täsmälleen se kilpailu josta `MainActivity`in oma ohjaus varoittaa 24.8.2026
mitattuna: poistuminen osuu keskelle konfiguraatiomuutosta.

Havainto joka seurasi: *"palkki tanssi eestaas kolmisen sekuntia."* Se on yhdenmukainen
vapautuksen kanssa, koska järjestelmä ratkaisee suunnan uudelleen sillä hetkellä kun ruutu on
vaihtumassa.

**Suunnan omistaja on siis taas yksi**, ja se asettaa arvon vasta kun kohde on voimassa.
Välilehtipalkin ja sävyn ennakointi jää voimaan, koska ne ovat ruudun omia eivätkä
järjestelmän ratkaisemia.

**Menetelmähavainto, ja se on tämän merkinnän syy.** Kolme korjausta tehtiin peräkkäin
koodista päättelemällä, ja kaksi niistä osui väärään syyhyn. Ensimmäinen kaappaus laitteelta
kertoi enemmän kuin kolme lukukierrosta, ja se oli saatavilla koko ajan. Siirtymän seuraava
epäilys mitataan ennen kuin siihen kirjoitetaan korjaus.

### Nauhoitus ratkaisi siirtymän: odotus kuuluu kohteeseen (mitattu 4.9.2026 illalla)

Neljäs havainto samasta siirtymästä oli *"tilanne edelleen karmea"*, ja siinä vaiheessa
päättely koodista oli osunut väärään syyhyn kahdesti kolmesta. Paluu nauhoitettiin tabletilta
(`screenrecord`, 76 s, 2800x1752) ja purettiin kuvasarjaksi kuusi kuvaa sekunnissa.

**Mitä nauhalla näkyy.** Lauta päättymisruutuna (`Next Game`, `To Matches`), sitten **noin 1,4
sekuntia tummaa ruutua jossa on vain välilehtipalkki ja odotusmerkki**, sitten ristihäivytys ja
otteluluettelo. Kaksi asiaa erottui vasta kuvista.

Odotusruudulla oli välilehtipalkki muttei **otsikkoriviä** (`Matches`, `Refresh`, `Sign out`),
joten kehys kasvoi rivin verran juuri silloin kun sisältökin vaihtui. Ja vaihdon päällä oli
navigoinnin ristihäivytys kahden lähes samannäköisen ruudun välillä, eli sama asia liikkui
kahdesti.

**Syy oli rakenne eikä yksikään näistä eroista.** Odotus oli laudan reitillä ja joutui
teeskentelemään otteluluetteloa, joten jokainen ruutujen välinen ero oli korjattava erikseen.
Kolme korjattiin peräkkäin (sisältö, suunta, välilehtipalkki), ja neljäs löytyi vasta
nauhalta. Rakenne tuotti juuri tämän jonon.

**Odotus siirtyi kohteeseen.** Paluu tehdään heti, ja otteluluettelo näyttää oman
odotustilansa omassa kehyksessään: välilehtipalkki, otsikkorivi ja tausta ovat paikallaan koko
ajan, ja vain sisältöalue vaihtuu odotusmerkistä listaksi. Teeskentelyä ei ole, joten
löytymättömiä eroja ei voi olla.

Se on lähellä saman illan toista versiota, mutta kaksi sen levottomuuden syytä on löydetty ja
korjattu sen jälkeen: laudan `Go back to see the list` -teksti välähti ohi ennen vaihtoa, ja
suunnan vapautus osui keskelle vaihtoa. Kumpikaan ei enää tapahdu.

**Menetelmä on tässä se tulos joka kannattaa muistaa.** Yksi nauhoitus kertoi enemmän kuin
kolme lukukierrosta, ja se maksoi yhden siirron.

### Paluun luettelo on jo laitteella: toinen haku purettiin (Tommin päätös 4.9.2026 illalla)

Nauhoitus mittasi paluun odotukseksi noin 1,4 sekuntia, ja se oli kokonaan yhden pyynnön
mittainen: **sivusto oli jo lähettänyt otteluluettelon vastauksena laudan pyyntöön**, ja
sovellus heitti sen pois ja haki saman sivun uudelleen.

Aiempi perustelu oli kirjattu ja se oli hyvä siihen mitä silloin tiedettiin: vastauksen
välittäminen näkymämallista toiseen tarkoittaisi toista reittiä samaan tilaan, ja yksi
kuluttamaton pyyntö on halvempi kuin kaksi totuutta luettelosta. **Mitattu hinta muutti
punninnan**, ja toinen reitti osoittautui vältettäväksi.

**Lauta kantaa sivun, luettelo lukee sen.** `NotABoardKind.TopPage` kuljettaa vastauksen
tavut, ja `TopViewModel.adopt` lukee ne **samalla lukijalla** kuin oma haku ja päätyy samaan
tilaan. Lauta ei jäsennä otteluluetteloa eikä tiedä siitä mitään; se välittää sivun jota se ei
itse osaa lukea. Kahta totuutta ei siis synny, koska lukijoita on yhä yksi.

Haku jää varareitiksi sille tapaukselle jossa tavut eivät kelpaa luetteloksi. Sitä ei odoteta,
koska `DgPages.isTopPage` on jo sanonut kyllä, mutta tunnistus ja jäsennys ovat eri asioita.

**Yksi pyyntö vähemmän jokaisesta paluusta**, ja se on sivustolle ystävällisempi siinä missä
nopeampi käyttäjälle. Odotustila ([TopScreen], `matchesStale`) jää paikalleen varareittiä ja
käsin painettua hakua varten.

### Jumppa oli järjestelmäpalkkien vilkkuminen, ja logcat nimesi rivin (mitattu 4.9.2026 illalla)

Neljä kierrosta korjattiin siirtymän eri osia, ja Tommin kysymys osoitti oikean kohdan:
*"miksi siirtymä ei voi olla yhtä levollinen kuin toiselta välilehdeltä Matches siirtyminen
on?"* Kysymys on hyvä siksi, että välilehtivaihto on mitattu vertailukohta.

**Välilehtivaihto on yksi kehys.** Nauhoitettuna (20 kuvaa sekunnissa) Matches vaihtuu
Infoksi kahden peräkkäisen kehyksen välillä, ilman häivytystä ja ilman välitilaa. Molemmissa
päissä kehys on sama, molemmat ruudut ovat jo koostettuja, eikä ikkunassa muutu mikään.

**Paluu laudalta nauhoitettiin logcatin kanssa, ja loki ratkaisi.** Suunta ei ollut syy:
`rotationForOrientation(orient=UNSPECIFIED, last=ROTATION_90)` laski tulokseksi saman
`ROTATION_90`, eli kääntymistä ei tapahtunut. Sen sijaan lokissa oli kuusi
`InsetsController.onStateChanged` -tapahtumaa 0,67 sekunnin sisällä, ja `ITYPE_STATUS_BAR`
vaihtoi näkyvyyttään joka kerta.

Kutsupinot nimesivät tekijän: `show` kohdasta `HideSystemBarsWhileVisible ... onDispose`,
134 ms myöhemmin `hide` saman efektin rungosta, 153 ms myöhemmin `show` purusta uudelleen.
**Efekti ja sen purku pallottelivat keskenään**, ja ruudulla se oli palkkien vilkkumista
kolmesti. Jokainen vilkku muuttaa insettejä, eli koko sisältö siirtyy palkkien korkeuden
verran ylös ja alas.

**Syy oli takaisinkytkentä ehdossa.** `enabled` laskettiin `maxHeight`istä, ja ikkunan korkeus
kasvaa juuri silloin kun palkit piilotetaan, joten päätös kumosi oman päätöksensä. Tämä ansa
oli tunnistettu ja kirjattu `BoardScreen`iin jo aiemmin, ja se oli korjattu **puoliksi**:
`barsHeight` luetaan `systemBarsIgnoringVisibility`stä, mutta `windowHeight` luettiin yhä
laatikon sisältä.

**Päätös on nyt lukittu** (`remember` ilman avainta): ensimmäinen mittaus tehdään palkkien
ollessa näkyvissä, eli juuri siinä tilassa jota ehto kysyy, eikä sitä lasketa uudelleen.
Lauta on suuntalukossa, joten kokoa ei tarvitse mitata uudelleen.

**Todennettu laitteella samana iltana, ja mitta oli asetettu etukäteen.** Ennen ajoa
kirjattiin mitä lokissa saa olla: korkeintaan yksi `show` purusta. Tulos oli vähemmän kuin
sekään. **Nolla `HideSystemBarsWhileVisible`-kutsua** koko sessiossa, ja **nolla insettien
tilamuutosta paluun hetkellä**: paluu tapahtui 21.04.03 ja viimeiset insettitapahtumat olivat
21.01.43, eli kaksi ja puoli minuuttia aiemmin. Suunta ei kääntynyt tälläkään kerralla
(`ROTATION_90` molemmin puolin). Tommin tuomio: *"nyt siirtymää en edes huomannut."*

Nolla kutsua eikä yksi tarkoittaa, että ehto on tällä laitteella **epätosi jo ensimmäisellä
mittauksella**: palkit ovat vapaat isolla ruudulla, joten piilotusta ei tehdä eikä siten ole
mitään purettavaa. Aiempi vilkkuminen syntyi siitä että ehto ehti olla myös tosi.

**Menetelmähavainto, ja tämä on sen toinen puoli.** Nauhoitus näytti että jotain vilkkuu,
mutta vasta logcat kertoi kuka kutsui ja mistä rivistä. Kuva kertoo oireen, loki kertoo
tekijän, ja liikkuvassa UI-ongelmassa tarvitaan molemmat. Todennuksessa loki riitti yksin,
koska mitta oli lukumäärä eikä vaikutelma.

### Ratas otteluluettelossa, Sign out Info-listaan (Tommin päätös 4.9.2026)

Havainto oli naapuruus. `Sign out` oli otteluluettelon yläpalkissa `Refresh`in vieressä,
ja niiden tarve on eri luokkaa. Aktiivinen pelaaja painaa Refreshiä päivittäin,
uloskirjautumista tuskin koskaan. Väärä osuma ei myöskään ole halpa, koska istunto katkeaa
ja tunnukset on syötettävä uudelleen.

**Kaksi päätöstä, ja ne ovat eri lajia.** Uloskirjautuminen siirtyy pois palkista
Info-välilehden listariviksi. Asetukset saavat rattaan otteluluettelon palkkiin, ja ratas
avaa asetukset omana ruutunaan.

**Palkin kokoonpano on nyt `Refresh` ja ratas.** Nappien määrä ei kasva, joten 8.8.2026
kirjattu peruste ei kaadu vaan pysyy voimassa: kolmas nappi olisi vienyt leveyden otsikolta,
ja tässä sisään tuleva ratas ottaa lähtevän `Sign out`in paikan.

**Ratas on ikoni eikä teksti**, ja se on toistaiseksi ainoa ikoni sovelluksen yläpalkeissa.
Peruste on leveys. Sana `Settings` on leveämpi kuin `Sign out` jonka tilalle se tulisi, ja
ratas on niitä harvoja kuvia jotka luetaan samalla tavalla joka alustalla. Sisällönkuvaus on
silti asetettu, koska ruudunlukija ei lue kuvaa.

**Asetukset saavat oman reitin (`SETTINGS_ROUTE`), ja se kumoaa osan 26.8.2026 päätöksestä.**
Silloin `SETTINGS_ROUTE` ja `HELP_ROUTE` poistettiin ja porautuminen siirrettiin Info-välilehden
sisään, jottei välilehtipalkki katoa asetusruudussa. Perustelu oli oikea siihen tilanteeseen,
jossa ainoa sisäänkäynti oli Infon listarivi. Nyt sisäänkäyntejä on kaksi, ja toinen niistä on
toisella välilehdellä. Paikallaan porautuva ruutu ei voi olla otteluluettelon rattaan kohde
ilman että ratas ensin vaihtaa välilehteä, jolloin paluu veisi Info-listaan eikä sinne mistä
lähdettiin.

Asetusruutu on siis samaa lajia kuin lauta ja porautumisruutu. Välilehtipalkkia ei piirretä,
ruutu kuluttaa tilapalkin insetin itse, ja paluu on järjestelmän ele tai otsikkorivin `Back`.
`Help` ei palaa omaksi reitikseen, koska sillä on yhä yksi sisäänkäynti.

**Infon `Settings`-rivi vie samaan ruutuun.** Rivi ei katoa, mutta se navigoi reitille sen
sijaan että piirtäisi ruudun paikalleen. Kaksi kopiota samasta asetusruudusta olisi kaksi
totuutta samasta lomakkeesta, ja tämä on se sivu jolla kirjoituksen hinta on korkein.

**Sign out on Infossa rivi muttei `InfoSection`.** Osiot ovat kohteita joihin porautuu, ja
uloskirjautuminen on teko. Se piirtyy samalla rivimuodolla listan viimeisenä, ja ruutu saa sen
omana takaisinkutsunaan. Käsittely on sama kolme riviä kuin katkenneella istunnolla
(`SignOutOnExpiry`), eli tunnukset pois, keksi pois ja takaisin edelliseen ruutuun.

**Vahvistusdialogia ei tullut.** Teko on nyt kahden napautuksen päässä eri välilehdellä, eikä
sen vieressä ole mitään mitä painetaan päivittäin. Alkuperäinen vika oli naapuruus, ja se
poistui.

**Ratas ehti olla laitteella, ja se vaihtui sanaksi samana iltana.** Ensimmäinen versio oli
Material-ikoni, ja se piirtyi oletuksellaan harmaana viereisen `Refresh`in puunruskean
rinnalla. Väri korjattiin `dgTopAppBarColors`iin, mutta oikea johtopäätös oli toinen: Tommin
tuomio nähtyään molemmat oli *"teksti on ok"* ja *"rattaan tilalle nimi Settings"*.

**Yläpalkissa on siis kaksi sanaa eikä yhtään kuvaa**, `Refresh` ja `Settings`. Aiempi
peruste ikonille oli leveys, ja se osoittautui vääräksi mitaksi: `Sign out` lähti samasta
palkista, joten leveyttä vapautui enemmän kuin `Settings` vie. 29.8.2026 kirjattu rajaus
(ikonit jäävät Material 3:n oletuksiin) jää siis voimaan koskemattomana, koska yläpalkeissa
ei ole ikoneita.

**Vertailukohta oli mitattu eikä muistettu.** Tommi nimesi mallin, JAKO-korttipelikokoelman
avausnäytön, ja se katsottiin tabletilta (4.9.2026 klo 22.37). Sen otsikkorivillä on neljä
reunustettua neliönappia: kieli, jaa, `i` ja ratas. Kaksi asiaa siirtyi ja kaksi jäi. Ajatus
siitä että harvinaiset toiminnot ovat omassa paikassaan siirtyi. Ikonimuoto ja `Jaa` jäivät,
edellinen tuomion perusteella ja jälkimmäinen siksi ettei otteluluettelossa ole mitään
jaettavaa; ottelun `.mat` jakautuu jo lautanäkymästä, jossa on.

**`Settings`-rivi lähti Info-välilehdeltä kokonaan** (Tommin päätös samana iltana: *"Info ja
asetusnäkymä ja valinta ehdottomasti erikseen"*). Info on lukemista ja asetukset ovat
säätämistä, joten niillä on eri ruutu ja eri nappi. Sisäänkäyntejä on siis yksi eikä kaksi,
ja edellä kirjattu kahden sisäänkäynnin perustelu koskee vain sitä muutaman tunnin väliaikaa
jonka rivi ja nappi olivat molemmat olemassa. Oma reitti ei silti purkaudu: se on nyt sitä
suuremmalla syyllä oikea, koska ruutu ei ole minkään välilehden sisältöä.

**Info-välilehti jää** (*"Info on hyvä"*), ja sen rivit ovat manuaali, DG Help, Links,
`Resign matches` ja `Sign out`. Kaksi viimeistä ovat tekoja eivätkä luettavaa, ja ne ovat
täällä koska teko jota tarvitaan pari kertaa vuodessa ei ansaitse paikkaa siitä palkista
jota katsotaan joka päivä. Erillistä kolmen pisteen valikkoa ei tullut: rivi kantaa
selitteen, valikon kohta ei.

### Omat turnaukset otteluluetteloon, lounge yhdeksi ruuduksi (Tommin tilaus 4.9.2026)

Tilaus oli *"siirtää aktiiviset ottelut Matches-välilehdelle"*, ja tarkentavassa
kysymyksessä toinen lista osoittautui turnauksiksi eikä otteluiksi. Nimipariksi valittiin
`Your turn` ja `Tournaments`.

**Matches on nyt kaksi segmenttiä.** `Your turn` on Top Page, joka näyttää vain ne ottelut
joissa on vuoro. `Tournaments` on `/bg/userevent/<id>`, pelaajan omat käynnissä olevat
turnaukset. Ne eivät ole sama sivu kahdella suodattimella vaan kaksi eri sivua, ja siksi
segmenttirivi on oikea muoto: kaksi rinnakkaista listaa, molemmat yhden napautuksen päässä.

**`Your turn` on sivuston omat sanat.** Sivu sanoo *It is your turn in N matches* ja
ottelurivillä lukee jo `Your turn`, joten sama asia sanotaan yhdellä tavalla. Vaihtoehto
`Playable` olisi ollut sovelluksen oma sana samasta asiasta.

**Haku on laiska ja Refresh koskee auki olevaa segmenttiä**, sama sääntö kuin loungella
27.8.2026 alkaen. Turnaussivua ei haeta otteluluettelon mukana: polku on tiedossa heti
(`TopPage.activeTournamentsPath`), mutta pyyntö lähtee vasta kun segmentti avataan, ja sen
jälkeen sivu pysyy kunnes Refresh pyytää uuden.

**Näkymämalli on oma eikä osa `TopViewModel`ia** (`OwnTournamentsViewModel`). `TopViewModel`
on se luokka joka hakee Top Pagen, ja yhteen malliin sulautettuna kahden eri sivun tuoreus
olisi ollut yksi tila. Sisältö siirtyi `LoungeViewModel`ista sellaisenaan, ja sen kaksi
testiväitettä siirtyivät mukana omaksi testiluokakseen.

**Lounge kutistui segmenteistä yhdeksi ruuduksi** (Tommin päätös samassa vastauksessa).
Segmenttejä oli kolme 26.8.2026 alkaen, ja `Tournaments` lähti otteluluetteloon. Jäljelle
jäivät tarjoukset ja pelaajalista, eivätkä kaksi kohdetta ansaitse omaa riviään ruudun
yläreunassa. Ruutu on nyt tarjoukset, ja listan perässä on kaksi linkkiä sivuston omiin
kokonaislistoihin: `Players` ja `Tournament Hall`. Pelaajalista aukeaa porautumisena samassa
ruudussa (hakukenttä on sen omassa näkymässä, otsikkorivillä `Back`), halli
porautumissivuna kuten ennenkin.

**Jako on nyt yhdellä lauseella sanottavissa.** Matches kertoo missä jo olen, lounge kertoo
mihin voin mennä mukaan. Se on sama peruste jolla `Tournaments` erotettiin Tournament
Hallista 3.9.2026, vain yhtä kerrosta ylempänä.

**Yksi seuraus kirjattiin ääneen.** Ilmoittautuminen loungessa muutti ennen omien
turnausten listaa saman näkymämallin sisällä, ja se haki listan uudelleen. Nyt lista on
toisen välilehden oma, joten sitä ei haeta ilmoittautumisen jälkeen: se on vanha kunnes se
avataan tai Refresh pyytää sen. Sama sääntö kuin muillakin laiskoilla listoilla.

**Tournament Hall jäi loungeen eikä seurannut omia turnauksia.** Sen polku luetaan loungen
omalta sivulta, jota otteluluettelo ei hae, ja se on koko sivuston lista eli samaa lajia kuin
muu lounge. `OwnTournamentsSection` kantaa yhä `hallPath`-parametrin, ja otteluluettelossa se
on `null`.

### Ottelulista lakkasi olemasta taulukko (Tommin havainto 4.9.2026)

*"DG Android näyttää liian vähän sovellukselta ja liian paljon nostalgialta."* Havainto
osui rakenteeseen eikä väreihin: ruudut oli rakennettu sivuston omista taulukoista
sellaisinaan. Viidestä nimetystä web-perinnöstä Tommi valitsi kaksi korjattavaksi, ja
laajuudeksi ottelulistan ensin.

**Sarakeotsikkorivi poistui, ja lajittelu jäi.** Rivit eivät enää ole kohdakkain, joten
otsikolla ei ole mitään otsikoitavaa. Samat viisi perustetta ovat nyt valintoja
(`FilterChip`) rivillä jonka edessä lukee `Sort by`, ja valittu kantaa yhä suuntanuolen
nimensä perässä. Nuoli on tekstissä eikä omana ikoninaan, samasta syystä kuin
otsikkorivillä.

**Ottelurivi kantaa selitteet itse.** Tapahtumanimi on lihavoituna ensimmäisellä rivillä,
vastustaja omallaan (`vs RaimoKoski`), ja lukemat kolmantena rivinä muodossa
`Grace 14:15  Pool 264:22  Length 15`. Selite toistuu jokaisella rivillä, ja se on
tietoinen hinta: 26.8.2026 kirjattu peruste kerran sanotulle otsikolle oli juuri se ettei
selite toistu. Peruste piti niin kauan kuin rivit olivat kohdakkain.

**Sarakepainot poistuivat mitattuina arvoina.** Ne olivat oikein mitattuja (Time Pool sai
lisää painoa vastustajan kustannuksella 26.8.2026, 320 dp ja fontScale 1.3), mutta ne
mitattiin taulukolle jota ei enää ole. Lukemat eivät enää kilpaile leveydestä keskenään,
joten pisin aika ei katkea kapealla ruudulla. Vastustaja ei ole enää lukema muiden joukossa
vaan oma rivinsä, koska hän on ainoa kenttä jota ei lueta numerona.

**Puuttuva arvo jätetään pois eikä merkitä viivalla.** Sivusto kirjoittaa liigaottelun
kierrokseksi viivan, ja selitteen kanssa se olisi lukenut `Round -`. Taulukossa tyhjä solu
oli oikea muoto, koska sarake oli olemassa; rivillä jossa ei ole sarakkeita puuttuva lukema
ei ansaitse merkintää lainkaan.

**Vuoro on pilleri.** `Your turn` oli rivin oikeassa reunassa samaa tekstipainoa kuin data,
joten sen näki vasta lukemalla. Nyt se on primary-pinnalla pyöristetyssä muodossa, eli
pinta ja muoto värin lisäksi. Vastustajan vuoro saa hillityn `surfaceVariant`-pinnan, koska
se ei ole toimintakehotus vaan tieto.

**Rajaus.** Muutos koskee ottelulistaa. Muut listat seuraavat perässä jos tämä toimii, ja
kolme muuta nimettyä web-perintöä (alleviivatut linkit, litteä pinta ilman ryhmittelyä,
sivuston sanasto otsikoina) jäivät koskematta tässä erässä.

### Reunapäivärivillä on kolmas tila, koska kaksi valehteli (Tommin päätös 5.9.2026)

Rivi `Message archive reaches …` luki kannan reunapäivän virrasta jonka alkuarvo oli
`null`, ja `null` piirtyi tekstinä `Message archive is empty`. Ruutu siis **väitti arkiston
olevan tyhjä** siihen asti että Room ehti vastata. Löytö on 5.9.2026 tilatusta kaavahausta,
ja se on saman vikamuodon neljäs esiintymä: vastaamaton virta luettuna vastaukseksi
(`docs/AUDITOINTI-KOMPOSITIO.md`).

**Miksi tämä ei ollut pikkuasia vaikka vahinko oli pieni.** Rivi korjaa itsensä heti eikä
kirjoita mitään mihinkään, joten mitään ei menetetty. Se on silti se rivi jonka koko
tehtävä on olla mittalukema jonka näkee ilman arvailua, ja välähtävä väärä nolla on
arvailtava. Sama rivi vastaa arjen kysymykseen *"ehtikö tuo tallentua"*, ja juuri siihen
kysymykseen väärä vastaus on pahempi kuin ei vastausta.

**Vaihtoehtoja oli kaksi, ja Tommi valitsi erottelun.** Rivi olisi voinut jäädä tyhjäksi
ensimmäiseen arvoon asti, mutta silloin se olisi ollut hetken sama kuin puuttuva rivi, ja
puuttuvan rivin ja rikkinäisen rivin erottamattomuus on juuri se syy jonka takia tyhjä
arkisto sanotaan ääneen. Kolmas tila sanoo mitä tapahtuu.

Teksti on `Reading message archive…` ja se on Tommin valinta kolmesta. Se kertoo mitä
sovellus tekee eikä väitä arkistosta mitään, ja kolme pistettä kertovat ettei se ole
lopputila.

Toteutus on tyyppi eikä lipputieto (`ArchiveEdgeState`: `Unknown`, `Empty`, `Reaches`).
Erottelun koko tarkoitus on ettei kolmatta tilaa voi vahingossa piirtää toisen näköisenä,
ja `when` ilman haaraa ei käänny. `ArchiveEdgeState.from` ei tuota `Unknown`ia lainkaan:
se on kerääjän alkuarvo eikä kannan vastaus.

**Mitä laitteelta todennettiin ja mitä ei.** Asettunut tila luettiin laitteelta 5.9.2026
(`Message archive reaches 4 Sep 2026`). Lataustekstiä **ei saatu kiinni ruudulta**:
kylmäkäynnistyksen jälkeen kahdeksan peräkkäistä näkymähierarkian vedosta näytti jo
reunapäivän, eli Room vastasi ennen ensimmäistä mitattavaa kehystä. Sama koskee sitä
vanhaa väärää tyhjä-väitettä: sitäkään ei nähty ruudulla kertaakaan. Korjaus poistaa
mahdollisen väärän väitteen, eikä havaittua oiretta.

### Peruutuspyynnön `Accept` puhui kuutiosta (mitattu ja korjattu 5.9.2026)

Sivusto käyttää sanoja `Accept` ja `Decline` kahteen eri kysymykseen. Toinen on kuution
ottaminen, toinen on **vastustajan peruutuspyyntö**: *"Your opponent made an unexpected
move, and the game has been rolled back to that point."* Molemmissa lomake näyttää
samalta, ja molemmissa on rasti `verify=Accept`.

**Sovellus valitsi dialogin tekstin napin sanasta**, joten peruutussivun `Accept` olisi
avannut kuutiodialogin. Sen teksti lupaa että kuutio siirtyy omalle puolelle
kaksinkertaisena, ja peruutussivulla se on väärin joka sanaltaan: kuutio oli mittauksessa
ykkösessä eikä noppia ollut lainkaan.

**Vika ei laukennut, ja se on sattuma eikä suoja.** Tommi painoi `Decline`ä, joka menee
suoraan ilman dialogia (3.9.2026 tehty rajaus: sivu ei vaadi sille rastia). `Accept`
olisi näyttänyt väärän lupauksen.

**Korjaus lukee sivun lajin eikä napin sanaa.** `cubeDialogBody(label, rolledBack)` valitsee
rungon, ja peruutussivulla se on `board_rollback_accept_text`: *This accepts your opponent's
rollback right away. The game returns to that point, and it cannot be taken back.* Otsikko
pysyy sanana `Accept?`, koska se on sivun oma sana. Sivun laji tulee
`BoardState.rolledBack`ista, joka on ollut olemassa 1.8.2026 alkaen, joten korjaus ei
tarvinnut uutta jäsennystä.

Valinta on oma funktionsa eikä ehto piirtokohdassa, jotta se on testattavissa ilman Composea
(`CubeConfirmTest`). Sama peruste kuin `diceTapFor`illä ja `irreversibleGapBefore`llä.

**Mitä tästä jäi todentamatta.** Dialogia ei ole nähty laitteella peruutussivulla, koska
tilannetta ei voi tilata: se vaatii että vastustaja pyytää peruutusta ja että vastaus on
`Accept` eikä `Decline`. Teksti on siis oikea koodissa ja testissä, ei ruudulla.

### Vaalean teeman pintapaletti (6.9.2026)

**Tommin havainto oli että vaalea teema on väritön ja tylsä, ja koodi kertoi miksi.**
`DgTheme.kt`:ssa oli asetettu vain `primary`, kun tummaan teemaan oli asetettu kaksi arvoa.
Kaikki muu tuli Material 3:n vaaleista oletuksista, jotka taittavat violettiin. Vaalea teema
oli siis kirjaimellisesti vähemmän valittu kuin tumma, eikä havainto ollut makuasia vaan
kuvaus siitä mitä oli tekemättä.

**Mitkä värit korvattiin, ja miksi juuri ne.** Käyttö laskettiin lähteestä eikä arvattu:
`onSurfaceVariant` on sovelluksen käytetyin väri (59 kohtaa), `primary` toiseksi käytetyin
(28) ja `surfaceVariant` piirtää viisi pintaa. Näiden oletukset ovat `#49454F`, `#E7E0EC` ja
tausta `#FEF7FF`, eli juuri se violetti joka näkyy eniten. Korvattu on koko pintaperhe
(tausta, pinnat, containerit, ääriviivat), koska yhden jättäminen oletukseksi olisi tuonut
violetin takaisin yhteen kohtaan ja tehnyt siitä tahran.

**Sävy on sama kehyspuu kuin primaryssa eikä uusi väri.** Neutraalit taittavat lämpimään
siellä missä oletukset taittoivat violettiin, ja kylläisyys on matala samasta syystä kuin
primaryssa. Tausta on `#FBF8F4`, pinta `#E8E0D6` ja tekstin toissijainen sävy `#4C443B`.

**Kontrastit mitattiin uutta taustaa vasten, ja AA-raja on 4,5:1.** `onSurface` `#1E1A16` on
16,33:1, `onSurfaceVariant` on 9,03:1 taustalla ja 7,31:1 `surfaceVariant`illa, ja `primary`
on 6,39:1. `outline` `#7E756A` on 4,28:1, mikä ylittää käyttöliittymäelementin 3:1-rajan
muttei tekstin rajaa; sitä ei käytetä tekstiin. Error-värit jätettiin oletuksiksi, koska ne
ovat punaisia eivätkä violetteja eikä niitä ole syytä siirtää lämpimään.

**Kaksi asiaa jäi tämän ulkopuolelle.** Lautanäkymä ei käytä teemaa lainkaan (X-22 on mitattu
kokonaisuus), ja taustakuva on oma kysymyksensä `docs/AVOIMET.md`:ssä. Paletti ei ratkaise
sitä kumpaankaan suuntaan, koska kuva tarvitsee alleen värin joka tapauksessa.

### Taustakuvio tekstiruuduissa (6.9.2026)

**Poistettu 6.10.2026**, ks. *Taustakuvio poistettiin* tiedoston lopussa. Tämä luku ja sitä
seuraavat kuviota koskevat luvut ovat historiaa.

**Tämä kumoaa osan 29.8.2026 tehdystä päätöksestä, ja kumoaminen on Tommin.** Silloin
kuviollinen tausta hylättiin selkeyden nimissä, ja peruste oli se että tumma tila oli juuri
toteutettu, joten tausta ei ole enää yksi puhdas valkea. Tommin uusi rajaus erottelee sen mitä
tuo päätös niputti: kuvio kuuluu vaaleaan eikä tummaan. Peruste ei siis kaatunut vaan kaventui,
ja se on syy miksi vanha rivi jätettiin `docs/AVOIMET.md`:hen näkyviin eikä poistettu.

**Tilaus tuli samana päivänä kuin paletti, ja se on itsessään havainto.** Vaalean teeman
pintapaletti tehtiin muutamaa tuntia aiemmin, ja Tommin arvio sen jälkeen oli *"värit ei riitä
- näyttää monotoniselta 98-sovellukselta"*. Väriongelma oli siis oikein diagnosoitu muttei
riittävä korjaus, ja se kirjataan tänne koska paletin oma kirjaus lupasi enemmän kuin se
tuotti.

**Ensimmäinen motiivi oli laudan kiila, ja Tommi kaatoi sen laitteelta.** Perustelu oli, että
kiila on sama muoto jonka pelaaja katsoo laudalla. Ruudulla se ei toiminut, ja Tommin sanat
olivat *"kiilat ei toimi, mutta todella arvostan Escher"*. Kaksi versiota ehti laitteelle
ennen kaatoa, ja molemmat ovat alempana kohdassa *Kaksi hylättyä versiota*.

**Nykyinen motiivi on kuutiotessellaatio.** Kolmen vinoneliön kuusikulmiot täyttävät tason
ilman rakoja, jolloin sama kuvio lukeutuu kahdella tavalla: kuutioina ylhäältä ja kuutioina
alhaalta katsottuna. Kuution valinta tulee kohteesta eikä tyylistä, koska backgammonissa
kuutio on tuplauskuutio ja noppa.

**Tekijänoikeus on tässä kirjattava, koska motiivi tuli Escher-viittauksesta.** Kuvio on oma,
eikä siinä ole mitään Escherin teoksista, jotka ovat yhä suojattuja. Tessellaatio on menetelmä
ja vinoneliötäyttö on vuosisatoja vanha kuvio (kansanomainen nimi *tumbling blocks*), eli
vapaasti käytettävissä.

**Kuvio piirretään koodista eikä ladata kuvatiedostona** (`DgPattern.kt`). Sovelluksessa ei ole
yhtäkään bittikarttaa, eikä sellaista kannata tuoda tämän takia: piirretty tiili seuraa teeman
väriä, skaalautuu laitteen tiheydellä eikä kasvata APK:ta. Kuution särmä on 22 dp, jolloin
tiili on 38 × 66 dp. Saumattomuus syntyy siitä että kuusikulmioita piirretään tiilen reunojen
yli ja `Canvas` rajaa ylimenevän pois; naapuritiilen sama laskenta osuu samoihin kohtiin.

**Luettavuus on mitattu.** Muste on teeman `primary` `#6B5844`, ja kolmen tahkon peittävyydet
ovat 0,015, 0,045 ja 0,075. Ero tahkojen välillä on se mikä tekee tasokuviosta kuution, eli se
on muoto eikä sävytys. Tummin tahko antaa taustaksi `#F0ECE7`, jota vasten `onSurfaceVariant`
on 8,13:1, `primary` 5,75:1 ja välilehtien matalin aksentti 4,91:1. AA-raja on 4,5:1, eli
kuvion pahin kohta jättää yhä varaa. Tummimman tahkon oma ero taustaan on 1,11:1, ja se on
tarkoitus: kuvion pitää näkyä pintana muttei riviä katkaisevana viivana.

**Toteutus vaati yhden rakennemuutoksen, ja se on syytä tietää ennen kuin ruutuihin koskee.**
Tausta maalataan nyt kerran `MainActivity`ssä (`Modifier.dgScreenBackground`), ja kahdeksan
tekstiruudun `Scaffold`ille annettiin `containerColor = Color.Transparent`. Ilman sitä
`Scaffold` maalaisi oman taustansa kuvion päälle. **Uusi ruutu tarvitsee siis saman rivin**,
tai se jää yksin ilman kuviota.

**Kaksi hylättyä versiota, ja molemmat opettivat saman asian.** Ensimmäinen kiilaversio oli
tiililtään 52 × 44 dp ja etäisemmän kiilan peittävyys 0,035, jolloin vuorottelu ei näkynyt
lainkaan: ruudulla oli tasainen kolmioruudukko yhteen suuntaan, eli juuri se monotonisuus jota
vastaan kuvio tehtiin. Suurempi tiili ja nostettu peittävyys korjasivat sen, ja **toinen versio
näytti oikealta laskennallisesti mutta Tommi kaatoi sen ruudulta**. Opetus on molemmissa sama:
luku voi olla laskettu oikein ja näyttää silti väärältä, eli mittaus ei korvaa katsomista.
Kolmas versio (kuutiot) on ensimmäinen joka läpäisi katsomisen.

**Rajaukset.** Kuvio on vain vaaleassa teemassa; tummassa jää pelkkä taustaväri, ja se on
todennettu laitteella kääntämällä yötila päälle ja takaisin. Lautanäkymä on ulkona samalla
rajauksella kuin 29.8.2026, ja rajaus luetaan `MainActivity`n `enabled`-lipusta. **Lautaa ei
todennettu ruudulta**, koska ottelun avaaminen on kuluttava teko, joten sen osalta väite
nojaa koodiin eikä kuvaan.

### Metamorfoosi: kuutioista linnuiksi (6.9.2026)

**Tämä korvaa edellisen kohdan toteutustavan mutta säilyttää sen motiivin.** Kuutiot ovat
yhä ruudun yläosassa samoilla peittävyyksillä, ja tiili on poissa: tausta piirretään koko
ruudun kokoisena kuvana, jossa muodon parametri on ruudun pystysuunnan funktio. Syy on se,
että tiili on määritelmällisesti sama kaikkialla eikä voi muuttua matkalla.

**Päätös on Tommin ja peruste koskee käyttöä.** Samana iltana kuin kuutiot hyväksyttiin hän
sanoi *"Escher yhdistäminen varpusten parveiluun olisi upeaa"* ja *"muuttuva tausta saisi
minut palaamaan pelaamaan."* Idea on Escherin Metamorphosis-teosten menetelmä, jossa
säännöllinen ruudukko irtoaa vähitellen hahmoiksi. Menetelmä on vapaa ja teokset eivät;
kuutiotessellaatio on vuosisatoja vanha ja lintusiluetti on oma yhden viivan piirros.

**Kolme vyöhykettä alhaalta ylös, ja rajat arvotaan.** Alhaalla kuutiot ehjinä maana,
välissä muodonmuutos, ylhäällä linnut irrallaan ja harvenevina. Suunta on Tommin: ensimmäinen
laiteversio kulki ylhäältä alas, ja hän sanoi sen nähtyään *"alhaalta ylöspäin toteutuvat
tessellaatiot vaikuttaisi minua lisää."* Peilaus on yksi rivi, ja se sattuu sopimaan myös
luettavuuteen: ruudun yläosassa ovat otsikot ja välilehdet, ja siellä kuvio on harvinta.
Kuutioiden loppuraja on 15–30 % ruudun korkeudesta alhaalta lukien ja lintujen alkuraja
40–50 % sen yläpuolella, jotta kahden ruudun taivaat eroavat muutenkin kuin lintujen
paikoista.

**Välimuoto käyttää kuution omaa muotoa eikä vaihda sitä toiseen.** Ylätahkon alareunat
ovat valmiiksi V, jonka kärki on solun keskellä ja siivet ylöspäin. Muodonmuutos etenee
neljässä limittäisessä vaiheessa: sivutahkot haipyvät ja kuutio litistyy pinnaksi, ylätahko
muuttuu täytöstä ääriviivaksi ja kutistuu irti ruudukosta, yläkärki laskee keskipisteeseen
ja jäljelle jää V, ja V taipuu lintuviivaksi. Kutistuminen on siinä siksi että ilman sitä
naapurien siivenkärjet yhtyivät yhdeksi sahalinjaksi; se näkyi luonnoksessa ennen laitetta.

**Lintu on yksi viiva, siivet ylös ja kärjet notkahtaen.** Se oli 6.9.2026 luonnoksista
ainoa joka lukeutui linnuksi. Kaksi kaarta ylöspäin lukeutui kukkuloiksi, ja suora
lintutessellaatio siirtosymmetrialla kaatui kolmesti lehdiksi, tähdiksi ja houndstoothiksi.
Vika oli symmetriaryhmässä eikä parametreissa; metamorfoosi kiertää sen, koska linnut ovat
irrallaan eivätkä tessellaatio.

**Muutos riippuu arvasta, ja arpa heitetään joka ruudun avauksessa.** Saate tarjosi kolme
ajuria (vieritys, vuorot, vuorokaudenaika), ja Tommin oma lause samana iltana antoi
neljännen: *"unelma on, että algoritmi arpoo aina taustalle hauskan ... escher-mäisen
tesselaation."* Kolmesta ajankohdasta (käynnistys, ruudun avaus, päivä) Tommi valitsi
ruudun avauksen. Siemen sidotaan reittiin `MainActivity`ssä, joten välilehden vaihto arpoo
uuden taivaan ja saman ruudun uudelleenpiirto pitää entisen. Tilaa ei tallenneta. Asettelu
on siemenen suhteen deterministinen, ja se on testattu (`DgSkyLayoutTest`).

**Luettavuus on mitattu uudestaan, koska viiva on uusi tummin kohta.** Tommin rajaus samana
iltana oli *"taustan ei pidä vaikeuttaa tekstien ja toimintojen lukemista."* Lintuviivan
leveys on 1,5 dp ja peittävyys 0,10; taustaa `#FBF8F4` vasten se antaa `#EDE8E2`, jonka
päällä `primary` on 5,55:1 ja välilehtien matalin aksentti (Lounge `#2A7430`) 4,74:1.
AA-raja on 4,5:1. Vaihtoehdot laskettiin samalla: 0,075 olisi antanut 4,91:1 ja 0,15 olisi
pudonnut rajan alle (4,40:1). Tahkojen arvot ovat ennallaan.

**Menetelmä, joka tuli tästä työstä ja pätee jatkossa.** Tommi: *"sinulla on taide-makuni,
käytä sitä vaihtoehtojen visualisoimisessa ennen päätöksiä."* Kuutioita edeltävät versiot
menivät laitteelle kolme kertaa ennen kuin vaihtoehtoja piirrettiin kuviksi. Metamorfoosi
piirrettiin ensin PIL:llä täysvahvuudella, koska seitsemän prosentin peittävyydellä muotoa ei
voi arvioida, ja kaksi virhettä (jyrkkä siirtymä, sahalinja) löytyi kuvasta eikä ruudulta.
Vasta korjattu versio käännettiin. Luonnoskuvat eivät säily; tämä kappale on se mitä niistä
jäi.

**Rajaukset ovat ennallaan.** Kuvio on vain vaaleassa teemassa, lautanäkymä on ulkona
`enabled`-lipulla, ja perusväri maalataan `dgScreenBackground`issa koska ruutujen `Scaffold`it
ovat läpinäkyviä.

### Tyhjä tila täyteen: kuvio näkyviin, lukupinta tekstin alle ja sumi-e asetukseksi (6.9.2026)

**Tilaus on Tommin, laitekuvan jälkeen samana iltana:** *"täytetään valkoinen tila upeilla
kuvioilla (makuni mukaan), värilliset essellaatiot app-asetus."* Edellisen kohdan kuvio oli
mitoitettu näkymättömyyden rajalle (peittävyydet 0,015…0,10), koska se piti kestää tekstin
alla. Tämä kohta erottaa kaksi asiaa jotka siihen asti olivat yksi: tyhjän tilan kuvio ja
tekstin alla oleva kuvio.

**Viisi kandidaattia piirrettiin kuviksi ennen laitetta**, ja se on menetelmä eikä sattuma
(edellisen kohdan *Menetelmä*). Työkalu on `tyokalut/taustaluonnos.py`: se toistaa
`DgPattern.kt`:n asettelun PIL:llä ja piirtää paneelit rinnakkain otteluluettelon
jäljitelmällä ja ilman. Kandidaatit olivat nykyinen vertailukohtana, voimakas yksivärinen
(*Kehyspuu*) sekä kolme makuprofiilista johdettua värillistä: *Syysmaa* (okra, ruoste,
sammal), *Revontulet* (vihreä, sininen, violetti) ja *Sumi-e* (muste, vermilionin väriset
linnut ja iso kuu taivaalla; LK Creationsin resepti Escherin päällä). Kuva ei säily, työkalu
säilyy.

**Tommin päätös 6.9.2026: *"1 oletukseksi, 4 asetukseksi, lukupinta kelpaa."*** Kolme
päätöstä yhdessä lauseessa, ja jokaisella on oma kotinsa koodissa.

1. **Voimakkuus.** Tahkojen peittävyydet ovat nyt 0,14, 0,34 ja 0,60 (olivat 0,015, 0,045
   ja 0,075) ja lintuviivan 0,65 (oli 0,10). Arvo haettiin kolmella laiteversiolla samana
   yönä ja seuraavana päivänä. Nelinkertaisista (0,10, 0,24 ja 0,38) Tommi sanoi
   *"tessellaatiot ovat vaisuja, toisaalta tekstit ovat luettavia"*, ja lähes täydestä
   musteesta (0,18, 0,45 ja 0,80) seuraavana päivänä *"kuviot muuttuvat alhaalla liian
   intensiivisiksi ja tekstin lukeminen vaikeutuu"*. Nykyinen on näiden väli, ja lukupinta
   nousi samalla niin että tekstin alle jäävä kuvio on ohuempi kuin kummassakaan. Kaksi
   vastakkaista lukemaa on kirjattu `docs/AVOIMET.md`:hen kysymyksenä siitä riittääkö yksi
   kiinteä voimakkuus. **Kolmas lukema 27.9.2026:** asetussivun alaosa oli yhä liian
   intensiivinen. Tommi valitsi neljästä tavasta himmennyksen kaikkialla, joten tahkot ovat
   nyt 0,11, 0,26 ja 0,45. Samassa korjauksessa kuvio lakkasi piirtymästä järjestelmän
   alapalkin (tabletin tehtäväpalkki) taakse. **Samana iltana maa rikottiin:** Tommi, *"kuutioita
   on liikaa ja ne muodostavat kaistan säännöllisyyttä."* Neljästä luonnoksesta (tasainen,
   kapea, harva ja kukkulat, tabletin mitoissa) hän valitsi harvan ja kukkulat ja niiden
   väliin arvan. Vaalean maa on nyt 6–20 % alhaalta, muodonmuutos 33 %, ja joka ruutu arpoo
   harvennuksen 35–50 % ja yläreunan aaltoilun 0–8 % (`DgPattern.kt` › `rugged`). Tumman kaista
   oli jo kapea eikä muuttunut.
2. **Sumi-e on kytkin** (`SkyThemeStore`, asetusruudun laiteosio, **oletus pois**). Kuutiot
   ovat samat, linnut vaihtuvat täyteen vermilioniin (`#C8452A`) ja lintujen taakse tulee
   kuu: mustetta peittävyydellä 0,16, säde 22–30 % leveydestä, arvottu samasta siemenestä
   kuin taivas. Oletus on pois koska Tommin sana oli *asetus*: väri on valinta eikä
   lähtötila.
3. **Lukupinta** (`DgReadingSurface.kt`) on se mekanismi jolla sama kuvio täyttää sekä
   *upea* että *vain tekstin alta*. Sisältö piirtyy taustavärisen pinnan päälle, jonka
   peittävyys on 0,93, ja pinta loppuu siihen mihin sisältö loppuu. Tekstin alle jää kuviosta
   tehollisesti enintään 0,045, eli ohuempi kuin alkuperäisen kuvion tummin tahko (0,075). Laiskalla listalla raja
   luetaan asettelutiedosta piirtovaiheessa (`DgLazyColumn`), koska lista mittautuu aina
   täyteen kokoon; vierivällä sarakkeella pinta on tavallinen tausta `verticalScroll`in
   jälkeen, ja Info- ja Help-ruudun sarake vaihdettiin `fillMaxWidth`iin jotta se mittautuu
   sisältönsä korkuiseksi. Pinta on myös yläpalkin, välilehtipalkin, segmenttirivin ja
   listojen yläpuolisten linkkirivien alla, ja huomautuksissa (`RetryNote`) vain tekstin ja
   napin alla. Tilapalkin kaistale jää ilman, koska se ei ole sovelluksen tekstiä.

**Luettavuus mitattiin lukupinnan alta**, ja mittatapa on edellisen kohdan: kuvion tummin
kohta taustana, matalin välilehtiaksentti (Lounge `#2A7430`) tekstinä, AA-raja 4,5:1.

| Tila | Tummin kohta pinnan alla | Lounge sen päällä |
|---|---|---|
| Muste (oletus) | lintuviiva, `#F4F1EC` | 5,12:1 |
| Sumi-e | lintuviiva, `#F7EBE6` | 4,94:1 |

Ilman pintaa kumpikaan ei ylittäisi rajaa, eli pinta ei ole ulkoasu vaan luettavuuden ehto.
Kandidaattikuvassa pinta oli 0,85 ja kuvio vaimeampi; hylätyistä kandidaateista Revontulet
oli silloin pinnan alla 4,51:1, rajan tuntumassa.

**Todennettu Tommin tabletilla 6.9.2026** (kahdeksan ottelua listalla): pinta loppuu
viimeiseen riviin ja kuutiot täyttävät alaosan; sumi-e-tilassa linnut ja kuu näkyvät pinnan
läpi vaimeina ja tyhjässä tilassa täysinä. Asetuksen vaihto näkyy heti asetusruudun omassa
taustassa, koska tila luetaan `MainActivity`ssä samalla tavalla kuin pystylukko.

**Mikä ei muuttunut.** Asettelu, arpa per ruutu, suunta alhaalta ylös ja lintuviivan muoto
ovat ennallaan, ja tumma teema on yhä ilman kuviota. Testit (`DgSkyLayoutTest`) saivat yhden
lisäyksen: kuu on ruudun yläosassa ja sallitun kokoinen jokaisella siemenellä.

**Auki jäi generaattorin loppu** (`docs/AVOIMET.md` › *Taustageneraattori ja teemakirjasto*):
sumi-e on toinen tila muttei toinen motiivi, ja kala, sieni ja nisäkäs odottavat yhä
päätöstä siitä onko teema motiivi vai motiivipari.

### Tumma teema sai kuvion: viisi palettia, viisi hahmoa ja kapea kuutiokaista (6.9.2026 ilta)

**Lähtökohta oli Tommin linjaus edelliseltä yöltä:** *"light mode: tessellaatiot, dark mode:
värit"*, ja ensimmäisestä tummasta erästä *"värit ovat vaimeita ja motiivilla ei ole väliä,
kunhan se vaihtuu."* Toinen erä (`tyokalut/motiiviluonnos.py`) oli jäänyt arvioimatta, ja
tämä sessio alkoi siitä. Erä on viisi paneelia, jokaisessa oma hahmo ja oma paletti täytenä
kylläisyytenä mustalla: Revontulet + linnut, Meri + kalat, Hehku + sienet, Kuutamo + lehdet
(kuu) ja Sumi-e + tähdet (punainen kuu).

**Päätökset tulivat kolmessa vaiheessa, ja jokainen on luonnoskuvasta eikä laitteesta.**

1. *"Kuutiot himmeämmiksi."* Ensimmäinen vastaus oli kerroin 0,4 tahkoille, hahmoviiva
   täytenä. Samalla piirrettiin otteluluettelo lukupinnalla 0,93, koska Tommi kysyi miten
   teksti pärjää ja totesi ettei lauta tarvitse kuvioita taustalleen.
2. *"Vielä himmeämpi, arpa vaihtaa palettia, lukupinta sama kuin vaalealla."* Ja rajaus
   jota luonnos ei ollut kunnioittanut: *"en pidä että vähintään kolmennes alaosasta on
   käytännössä monotonista kuutiota, vähempikin riittää."* Vaalean kaista (täysi kuutio
   15–30 % ja muodonmuutos 40 % sen päällä) tekee alaosasta yli puolet kuutiota.
3. Kahdesta kapeammasta vaihtoehdosta Tommi valitsi **A:n**: kuutiot kertoimella 0,25,
   täysi kuutiokaista 5–10 % alhaalta ja muodonmuutos 30 % sen päällä. Hahmot täyttävät
   silloin yli puolet ruudusta. B (8–14 % ja 22 %) hävisi.

| | Vaalea (6.9. päivä) | Tumma (6.9. ilta) |
|---|---|---|
| tahkot | muste 0,14 / 0,34 / 0,60 | paletin kolme väriä, kerroin 0,25 kertaa syvyys |
| hahmoviiva | muste 0,65 tai vermilion 1,0 | paletin viivaväri 1,0, leveys 2 dp |
| täysi kuutio alhaalta | 15–30 % | 5–10 % |
| muodonmuutos sen päällä | 40 % | 30 % |
| lukupinta | 0,93 | 0,93 |

Syvyys on sama valo ylhäältä kuin luonnoksessa: alempana oleva kuutio on 35 % tummempi kuin
ylin, jotta maa saa syvyyttä eikä kaista ole yksi tasainen pinta.

**Toteutus.** `DgPattern.kt` piirtää molemmat teemat samasta asettelusta ja samasta
muodonmuutoksesta. Muutos on kolme parametria: kaistan mitat `dgSkyLayout`issa (oletukset
ovat vaalean arvot), hahmo `dgSkyOps`issa (`DgMotif`, oletus lintu) ja tahkon tunniste
piirto-operaatiossa (`DgFace`), jotta tahkon väri ja peittävyys ratkeavat vasta piirrettäessä
tilan mukaan. Hahmot ovat samat kaavat kuin luonnostyökalussa, siirrettyinä Kotliniin.
Paletti valitaan samasta siemenestä kuin taivas (`dgNightPalette`), joten ruudun avaus arpoo
molemmat kerralla ja saman ruudun uudelleenpiirto pitää entisen. Luonnostyökalun oletukset
vaihdettiin valittuihin arvoihin, joten paljas ajo piirtää sen mikä on koodissa.

**Kuvio on asetus molemmissa teemoissa, oletus pois** (`pattern`, `docs/ASETUKSET.md` ›
*Kymmenes paikallinen kytkin*). Ensimmäinen versio oli tumman oma kytkin oletuksena päällä,
ja se oletus oli Clauden valinta eikä Tommin; Tommi korjasi sen samassa vuorossa jossa
kysyin: *"oletuksena taustakuva vaalealla ja tummalla taustalla pois päältä."* Vaalea teema
menetti samalla aina-päällä-tilansa, jonka se oli saanut saman päivän aamuna. Sumi-e jäi
vaalean alakytkimeksi kuvion alle.

**Luettavuus mitattiin lukupinnan alta samalla tavalla kuin vaaleassa**, nyt tumman
välilehtiaksenttia (Lounge `#A8C69A`) vasten. Pahin kohta on kirkkain viivaväri täytenä.

| Paletti | Viiva pinnan alla | Lounge sen päällä |
|---|---|---|
| Revontulet | `#2A2A2F` | 7,62:1 |
| Meri | `#272A2F` | 7,69:1 |
| Hehku | `#2C292A` | 7,69:1 |
| Kuutamo | `#2C2A29` | 7,62:1 |
| Sumi-e | `#2C1F21` | 8,46:1 |

Kaikki ylittävät AA-rajan 4,5:1 selvästi, mikä on odotettua: tumma pinta vaimentaa vaalean
viivan tehokkaammin kuin vaalea pinta tumman. Tummassa raja ei ole luettavuus vaan se, että
pinnan alla kuvio näkyy vain häivähdyksenä; Tommin kysymys tekstistä sai vastauksen jo
luonnoskuvasta.

**Todennettu Tommin tabletilla 6.9.2026 illalla** (yhdeksän ottelua listalla, laite oli jo
tummassa teemassa): otteluluettelo sai Revontulet-paletin ja linnut, Lounge lehdet ja kuun,
asetusruutu sienet. Kuutiot alkavat vasta listan alta, ja hahmot näkyvät lukupinnan läpi
vaimeina. Yhdeksän ottelun lista peittää lähes koko ruudun, joten kuutiokaista näkyy
harvoin; se puolsi A:ta. Oletuksen vaihto pois todennettiin toisella asennuksella samana
iltana: tuoreen asennuksen asetusruutu on ilman kuviota ja Background-otsikon alla on kaksi
kytkintä molemmat pois.

**Mikä ei muuttunut.** Vaalea teema piirtyy samoin kuin ennen, koska uudet parametrit ovat
oletusarvoillaan vaalean vanhat luvut, ja `DgSkyLayoutTest`in vyöhykerajatesti vartioi sitä.
Lautanäkymä on yhä ilman kuviota molemmissa teemoissa. Testit saivat kolme lisäystä: tumman
kaista on kapeampi, paletti seuraa siementä ja kaikki viisi tulevat käyttöön, ja jokainen
hahmo antaa äärellisen pisteen koko matkalta.

**Auki jää** se mitä `docs/AVOIMET.md` › *Taustageneraattori ja teemakirjasto* sanoo:
riittääkö yksi kiinteä voimakkuus, nyt kummassakin teemassa erikseen, ja vaalean toinen hahmo.

### Vaalean puolen välilehtisävyt erottuivat taustasta muttei toisistaan (6.9.2026)

**Tommin havainto:** *"perus-moodin välilehtien värejä en nykyisellään erota toisistaan
helposti."* Havainto osui kohtaan jota aiempi mittaus ei kattanut. 29.8.2026 sävyille
laskettiin kontrasti **taustaa** vasten, ja kaikki viisi ylittivät AA-rajan reilusti. Sitä ei
laskettu, miten kaukana sävyt ovat **toisistaan**, ja juuri se on se mitä välilehtipalkissa
katsotaan: viisi nimeä vierekkäin samalla rivillä.

**Mitattu ero oli pieni, ja se selittää havainnon.** CIELAB-etäisyys pienimmällä parilla
(`Discussion` ja `Messages`) oli 20, ja kaikki neljä aksenttia olivat matalan kylläisyyden
tummia värejä lähes samalla vaaleudella. Ne luettiin siis ensin tummiksi ja vasta sitten
värillisiksi.

**Korjaus nostaa kylläisyyttä ja levittää sävyt kauemmas toisistaan**, ei vaaleutta. Uudet
arvot ovat `#2A7430` (Lounge), `#1565A8` (Discussion), `#8034A0` (Messages) ja `#00706B`
(Info), ja pienin pari on nyt 37 eli lähes kaksinkertainen. Kontrastivaatimus ei löystynyt, ja
6.9. lisätty taustakuvio laskettiin mukaan: matalin sävy on 5,00:1 kuvion tummimman kohdan
päällä, kun AA-raja on 4,5:1.

**Mikä ei muuttunut.** Tumma teema on ennallaan, koska havainto koski vaaleaa eikä samaa
ongelmaa ole siellä havaittu. Otteluluettelo on yhä ilman omaa sävyä, ja aiempi perustelu
siitä pätee sellaisenaan. Ja väri on yhä vahvistus eikä tunniste: jokaisella välilehdellä on
nimi, joten värisokea lukija ei menetä mitään.

**Aiempi perustelu kaventui muttei kaatunut.** 29.8. kirjattu *kylläisyys on alhaalla, jotta
väri erottaa välilehdet toisistaan mutta ei huuda* asetti kaksi ehtoa, ja mittaus osoitti
ettei ensimmäinen täyttynyt. Vaimeus oli siis liian vaimeaa omaan tarkoitukseensa nähden.

### Käsijälki: viisi kuviota tummaan ja negatiivijälki vaaleaan (6.9.2026 myöhään illalla)

**Tilaus oli 6.9.2026 pelisession jälkeen** (`docs/AVOIMET.md` › *Taustageneraattori ja
teemakirjasto*): maaliin kastettujen käsien joukko, eri värejä, käsistä muodostuu kuvioita.
Lähde on Far Cry Primal ja sen takana luolamaalausten käsijälki. Tommi: *"väräjän halusta nähdä
käsiä dg android-näytöllä seuraavassa sessiossa."* Sessio alkoi luonnoksesta kuten tilaus sanoi.

**Päätökset tulivat luonnoskuvista kolmessa erässä** (`tyokalut/kasiluonnos.py`), jokainen
Tommin sanoin, ja laite vasta niiden jälkeen.

1. Neljä luentaa rinnakkain: luolan seinä (positiivijälkiä ryhmissä), negatiivijälki
   (pigmentti puhallettu käden ympärille), kädet kuviona (rengas auringon ympärillä) ja
   käsiparvi (kuutiokaistasta ylös). Valinta: *"kädet kuviona, mutta auringon lisäksi
   fibonaccin kierre, <3 ja muita minulle tärkeitä symboleja"*, paletti Primal, ja
   *"negativijälki päiväteemassa kiinnostaa myös"*. Kesken erän: *"kädet ei pidä olla
   samankokoisia"*, josta kaksihuippuinen koko (joka neljäs lapsen käsi).
2. Symbolit makuprofiilista tarjottuina: puu, sieni ja kuutio valittiin, kuu ei. Sommittelu:
   *"aletaan piirtäminen kulmasta mikä oletettavasti on tekstistä vapaa"*. Kulmaan ankkuroitu
   kuvio leikkautui reunaan, ja korjaus oli *"puu, sieni ja kuutio ei toimi ja muitakin pitää
   keskittää, että ne näkyisivät jos pystytilaa on"*. Kuvio on siis alaosassa keskellä ja
   kokonaan ruudussa. Arpa: kädet kuudes hahmo ja kuvio oma arpa. Vaalea: arpaan
   mustetessellaation rinnalle.
3. *"Sienet pois, muut menee, lisää pyöristystä noppiin"*, ja puusta kahdesti: *"puu on liian
   symmetrinen"* ja *"runko suoraan ylöspäin ja oksistoa mukaan"*.

**Mikä luonnoksissa kaatui, jotta samaa ei yritetä uudestaan.** Sydän ilman täytettyä ydintä ei
lukeutunut sydämeksi; täytetty ydin auringon tapaan korjasi sen. Puun runko pinottuina käsinä
lukeutui pylvääksi, yksi iso käsi runkona hallitsi ruudun, ja tasakehäinen latvus oli
symmetrinen; lopullinen on täytetty runko, 4…6 kapenevaa oksaa eri kulmissa ja kädet oksien
päissä. Kuutio käsinä kuusikulmion reunoilla oli möykky; nopat pyöristetyin kulmin ja
silmäluvut käsinä lukeutuvat ilman selitystä, ja arpa heittää silmäluvut.

| Kuvio | Ydin täytettynä | Kädet |
|---|---|---|
| Aurinko | kehä 0,13 leveydestä | kaksi kehää, 11 ja 17, sormet ulos |
| Fibonaccin kierre | ei | kultainen spiraali, koko kasvaa ulospäin, sormet kulkusuuntaan |
| Sydän | sydänkäyrä | 24 ääriviivalla, sormet ulos |
| Puu | runko ja oksat | oksien päissä ja välissä |
| Nopat | kaksi noppaa, kulmat pyöristetty | silmäluvut, 1…6 per noppa |

**Toteutus** on `DgHands.kt`. Käsi on kämmen (soikio), ranne ja viisi pyöreäpäistä sormea
yhdistettynä polkujen unionilla, jotta puoliksi läpinäkyvä täyttö ei tummene päällekkäisyyksissä.
Asettelu on puhdasta laskentaa ja polut rakennetaan erikseen, jotta `DgHandsLayoutTest` voi
todeta sijainnit ilman `android.graphics.Path`ia. Mitat ovat ruudun leveyden osuuksia, joten
tabletilla kädet ovat isompia kuin luonnoksen puhelimella. Tummassa `dgNightPick` arpoo kuudesta
lipusta ja käsien osuessa kuvion viidestä; vaaleassa `dgLightHands` arpoo kahdesta. Kuutiokaistaa
ei ole käsien kanssa, koska luonnos hyväksyttiin ilman sitä. Vaalean negatiivijäljessä
pigmenttipilvi on säteittäisiä liukuja kämmenen ja sormenpäiden ympärillä, ei sumennus, koska
Compose piirtää liukuja ilman bittikarttaa; käsi on taustaväriä pilven päällä.

**Luettavuus mitattiin lukupinnan alta kuten muut** (Lounge-aksentti tekstinä, AA 4,5:1):
Primal-paletin kirkkain väri pinnan alla antaa tummassa 7,80:1, ja vaalean pigmenttien tummin
(yö `#2F3E6B`) 4,90:1. Vaalea on lähimpänä rajaa, koska tumma pigmentti vaaleaa vasten vaimenee
huonommin kuin vaalea tummaa vasten.

**Todennettu Tommin tabletilla 6.9.2026 myöhään illalla.** Tummassa 36 välilehden avausta antoi
kädet seitsemästi: aurinko Forumissa, nopat Infossa (5 ja 1) ja Forumissa (5 ja 6), puu Infossa
kahdesti. Kädet näkyvät lukupinnan läpi vaimeina ja täytenä listan alla. Vaaleassa 12 avausta
antoi negatiivijäljen kuudesti, Infossa täytenä ja Loungessa lukupinnan alla. Tila vaihdettiin
`cmd uimode night` -komennolla ja palautettiin tummaan. Otteluluettelossa kymmenen ottelua
peittää kuvion kokonaan, mikä on juuri se tilanne jonka *"jos pystytilaa on"* rajasi ulos.

**Laitekuva kumosi kuviot samana iltana.** Tommi: *"kädet ei vakuuta, ne ovat liian suuria, kun
taas satunnainen luolakuva toimii paremmin, sillä ei ole rajoituksia."* Kaksi seurausta:

- Tumman teeman kädet ovat nyt luonnoksen ensimmäinen luenta, **luolan seinä**: positiivijälkiä
  3…4 ryhmässä, 5…9 kättä kussakin samasta kulmasta poiketen, maaväreillä (okra, punamulta,
  hiili, kaoliini) kuten siinä kuvassa jonka hän arvioi. Viisi kuviota ovat git-historiassa
  (commit 30e5deb) ja luonnostyökalun `--kuviot`-lipun takana.
- **Käden koko on dp eikä ruudun leveyden osuus.** Kuvioversio skaalasi kädet leveydellä, ja
  tabletilla (1752 px) ne olivat 1,6-kertaisia luonnoksen puhelimeen (1080 px) nähden. Nyt
  aikuisen käsi on 40 dp kertaa tiheys, lapsen puolet siitä, joka laitteella sama fyysinen
  koko. `DgHandsLayoutTest` vartioi että leveämpi ruutu ei kasvata kättä.

Todennettu tabletilla samana iltana: 24 tumman avausta antoi seinän seitsemästi, 8 vaalean
avausta negatiivin kolmesti. Kädet ovat pieniä ja hajallaan, ja ylhäällä olevat jäävät
lukupinnan alle häivähdyksinä.

**Auki jää** sama kuin ennen: riittääkö yksi kiinteä voimakkuus. Ja yksi uusi: Primal-paletti
valittiin kuvioille eikä seinälle, joten seinä on maaväreillä; jos kylläisempi paletti
halutaan, se on yksi vakio.

### Luolan seinä: kivi, metsästys ja riitti alkuperäisellä kontrastilla (7.9.2026 yöllä)

**Tommin arviot käsijäljestä johtivat tähän kolmessa lauseessa**, kaikki samalta yöltä:
*"luolien seinät eivät ole mustia eikä valkeita, ei oikein toimi, chauvetin eläimet voisi
toimia"*, sitten *"ja metsästys, riitit"*, ja kun luonnos piirsi tummaan vaalean okran mustalle:
*"alkuperäinen värikontrasti olisi mielenkiintoisin."* Kolme seurausta, ja ne ovat `DgCave.kt`.

1. **Tausta on kivi, ei teeman väri.** Kivipinta on 40…60 pehmeää laikkua puolella
   voimakkuudella (Tommi: *"vaimeampi"*). Tumman kivi on soihdun valossa tummanruskea, vaalean
   kalkinvaalea. Compose piirtää laikut säteittäisinä liukuina, koska luonnoksen sumennus
   vaatisi bittikartan.
2. **Hiili on mustaa ja okra punaista molemmissa teemoissa.** Ero on kivessä, ei viivassa.
   Ensimmäinen arkki piirsi tummaan vaalean okran mustalle, ja se ei ollut luolan kontrasti.
3. **Lukupinta on kiven sävy, ei musta** (Tommin valinta kolmesta). Teeman tausta vaihdetaan
   kiveksi kun seinä on ruudulla (`DgCaveTheme` `MainActivity`ssä), jolloin lukupinta ja
   kortit seuraavat sitä ilman omaa ehtoa. Sävyt mitattiin heikointa välilehtiaksenttia
   vasten (`tyokalut/luolaluonnos.py`, `report_scrim`), ja ensimmäiset jäivät alle rajan.

| Kivi | Ensin | Heikoin aksentti | Nyt | Heikoin aksentti |
|---|---|---|---|---|
| tumma | `#5A4838` | 4,15:1 | `#4C3D30` | 4,98:1 |
| vaalea | `#EAE0CC` | 4,41:1 | `#EFE8D9` | 4,73:1 |

AA-raja on 4,5:1. Lukupinta on 0,93 kiveä laikkujen päällä, joten tehollinen tausta on lähes
pohjasävy ja mittaus pätee siihen.

**Kohtaukset arvotaan siemenestä, kaksi.** Metsästys: saalis arvotaan neljästä (biisoni,
hevonen, hirvi, mammutti; kaksi viimeistä Tommin lisäys *"hirvi ja mammutti mukaan"*), viisi
keihäänheittäjää perässä, keihäitä ilmassa, alempana toinen saalis ja kaksi seisovaa
metsästäjää. Riitti: piiritanssi tulen ympärillä, sarvipäinen hahmo hiilellä yläpuolella ja
seitsemän kättä sen ympärillä maaväreillä. Eläimet ovat omia piirroksia Chauvet'n tavasta,
pään ja selän linja kolmena hiukan poikkeavana vetona, varjo hierrettyinä pisteinä;
ihmishahmot ovat Levantin kalliomaalausten täytettyjä tikkuhahmoja liikkeessä.

**Arkista Tommi valitsi vain E:n ja F:n.** Kivi pelkkien käsien alle (A), hevospaneeli (B) ja
eläinpaneelit (C, D) jäivät luonnokseen. Sarvikuono ja leijona eivät lukeutuneet neljänkään
yrityksen jälkeen (sika, kala, hylje, tapiiri), ja ne ovat työkalussa jos niihin palataan.
Kädet saavat yhä kerrostua (Cueva de las Manos), ja luonnoksessa hylättiin myös negatiivijälki:
seinän kädet ovat positiivisia molemmissa teemoissa.

**Todennettu Tommin tabletilla 7.9.2026 yöllä.** Ensimmäinen kierros antoi 32 mustaa kuvaa,
koska näyttö oli nukahtanut kaappausten välissä; herätys ja uusi kierros. Tummassa 24 avausta
antoi seinän neljästi (metsästys Infossa hevosella ja biisonilla, riitti Inboxin lukupinnan
alla), vaaleassa 8 avausta neljästi. Lukupinta on kiven sävyä eikä musta laatikko, ja
Inboxin ohjelaatikko on teeman `surfaceVariant`, joka jäi harmaaksi; se ei ole lukupinta.
Tabletilla eläimet ovat leveyden osuuksina isompia kuin luonnoksen puhelimella, kuten seinän
maalaus, ja hiilen pisteet harvempia koska niiden määrä ei skaalaudu.

**Mikä ei muuttunut.** Viisi yöpalettia, mustetessellaatio ja sumi-e ovat ennallaan.
Kuudennen lipun ja vaalean puolikkaan arvat ovat samat kuin käsijäljellä; vain se mitä lippu
piirtää vaihtui, kolmannen kerran samana yönä.

**Auki jää.** Sama voimakkuuskysymys kuin ennen, nyt kivelle. Sarvikuono ja leijona. Ja
Inboxin harmaa ohjelaatikko kivellä: `surfaceVariant` voisi seurata kiveä samoin kuin
`surface`, mutta sitä ei ole päätetty.

### Tausta ja täytekuva: tila ratkaisee lajin (7.9.2026)

**Jako on Tommin, ja se tuli edellisen session päätössanasta:** *"parhaiten taustaksi sopii
mielestäni esimerkiksi lintu-tessellaatio, joka etenee alhaalta ylös. On myös kuvia, jotka
sopivat täyttämään vapaata tilaa."* Yön työ oli kohdellut kaikkia kuvia samana lajina: kaikki
piirrettiin koko ruudulle yhdessä paikassa, ja arpa valitsi tessellaation ja seinän välillä.
Siksi metsästys osui usein lukupinnan alle ja näkyi häivähdyksenä, eli seinä teki juuri sitä
mitä täytekuvan ei pitäisi tehdä.

**Yksi ristiriita jota jako ei itse ratkaise, ja se sanottiin ennen kysymyksiä.** Tessellaatio
etenee alhaalta ylös, joten sen tihein osa on ruudun alaosa. Tyhjä tila on myös ruudun
alaosa. Täytekuva peittää siis juuri kuutiot, ja "tausta aina, täytekuva päälle" ei ole
ilmainen yhdistelmä.

**Neljä päätöstä (Tommi: *"1 a 2 a 3 a 4 b"*):**

1. **Tila ratkaisee lajin, ei arpa.** Kun sisältö täyttää ruudun, ruudulla on tausta ja
   linnut näkyvät tekstin alla lukupinnan läpi. Kun sisällön alle jää vähintään **180 dp**
   (Tommin luku luonnoksesta, jossa riitti 156 dp:ssä oli pieni ja tungettu), tyhjään tilaan
   tulee täytekuva. Arpa säilyy kuvan sisällä. Tummassa seinä lakkaa olemasta kuudes lippu ja
   viisi yöpalettia ovat taustan arpa; vaaleassa tessellaatio on aina tausta.
2. **Kivi peittää vain vapaan alueen, ja sen yläreuna on pehmeä.** Kohtaus sommitellaan
   vapaaseen suorakulmioon eikä koko ruutuun. Kuutiot jäävät kiven alle sillä ruudulla jolla
   täytekuva näkyy; se on tämän päätöksen hinta ja hyväksytty. Teeman värjäys kiveksi
   (`DgCaveTheme`) poistui, lukupinta on aina teeman väri, ja Inboxin harmaa ohjelaatikko
   lakkasi olemasta kysymys. Reuna on 28 dp liuku teeman taustaan, alkukohta vaihtelee
   leveydellä yhdeksän solmun mukaan; Tommi: *"kelpaa"*.
3. **Yksi kytkin kuten nyt.** `pattern` kattaa molemmat lajit, oletus pois.
4. **Täytekuvia on kaksi kohtausperhettä: luolan seinä ja Cueva de las Manosin kädet.**
   Suositus oli pelkkä seinä; Tommi valitsi kädet mukaan omana kohtauksenaan. Ensimmäinen
   luonnos piirsi kädet tiheänä kenttänä kuudella pigmentillä, ja Tommi: *"liikaa, far cry
   primal oli hyvä tiheys."* Kohtaus on siis kasiluonnoksen luolan seinä: 3…4 ryhmää, 5…9
   kättä kussakin, maavärit, kerrostuen, dp-mitassa.

**Luonnos ensin** (`tyokalut/tayteluonnos.py`): sama ruutu 4…12 ottelurivillä, jotta näkyi miten
kohtaus elää vapaan tilan korkeuden mukaan. Työkalu piirtää taustan koko ruudulle, kiven ja
kohtauksen vapaan alueen kokoiseen kuvaan, ja liittää sen pehmeällä maskilla listan alle.
Siitä tuli mittayksikkö: kohtauksen mitat seuraavat `u = min(leveys, 1,5 × korkeus)`, jotta
matala alue ei litistä eläintä eikä korkea kasvata sitä yli leveyden. Toinen saalis ja
šamaani tulevat mukaan vain kun korkeutta on yli yksikön verran. Riitissä oli ensin kädet
šamaanin ympärillä kuten yön versiossa, ja laitekuvan jälkeen Tommi: *"älä sekoita käsiä
riittiin tai metsästykseen."* Kädet ovat siis vain Manosin kohtauksessa.

**Toteutus** on kolmessa tiedostossa. `DgFill.kt` on uusi: siemen kulkee `MainActivity`stä
`CompositionLocal`ina samasta arvasta kuin taivas (null kun kuvio on pois tai ruutu on lauta),
kynnys ja reuna ovat vakioita, ja sommittelu lasketaan kerran per koko välimuistiin. Vapaan
alueen raja tunnetaan vain piirtovaiheessa samasta syystä kuin lukupinnan raja, joten
täytekuva piirretään samassa `drawBehind`-kutsussa kuin lukupinta (`DgReadingSurface.kt`),
alueelle joka alkaa viimeisen rivin alareunasta; tyhjä lista on kokonaan vapaata. Info- ja
Help-ruudun vierivä sarake saa kehyksen (`DgFillFrame`), joka mittaa sarakkeen korkeuden.
`DgCave.kt` sommittelee kohtaukset suorakulmioon ja piirtää kiven pohjan, laikut ja reunan
itse. Kivellä on oma arpa, koska laikkujen määrä seuraa mittoja ja muuten leveämpi ruutu
olisi siirtänyt kohtauksen arvontaa (testi paljasti sen: käden koko riippui leveydestä
kiertotietä). Manosin kädet rajataan alueen sisään, koska matala alue päästi ne yli reunan.

**Testit** (`DgHandsLayoutTest`) vartioivat neljää suorakulmiota (matala, keskikorkea, korkea ja
tabletti): kohtaus pysyy alueessa, kaikki kolme kohtausta ja viisi palettia tulevat arvasta,
matala alue saa vähemmän, käden koko seuraa tiheyttä eikä leveyttä, kynnys on 180 dp ja
reunan käyrä on jatkuva.

**Todennettu Tommin tabletilla 7.9.2026.** Otteluluettelo 12 ottelulla: pelkkä tausta molemmissa
teemoissa, kuten tila sanoo. Info: tummassa kolme avausta antoi riitin kahdesti ja kädet
kerran, vaaleassa riitin kerran ja kädet kahdesti; metsästys ei osunut näihin kuuteen, mutta
käsien poiston jälkeen kuusi tumman avausta antoi sen neljästi (biisoni, mammutti ja hevonen),
riitin kerran ilman käsiä ja kädet kerran. Kivi alkaa viimeisestä rivistä pehmeällä reunalla, kohtaus
on kokonaan näkyvissä, ja lukupinta on teeman väri. Täysi Inbox-lista ja Loungen pitkä lista
saivat pelkän taustan. Tabletilla vapaa alue on leveämpi kuin korkea (1752 × noin 1200 px),
joten yksikkö on korkeuden puolella ja riitti piirtyy ilman šamaania.

**Mikä ei muuttunut.** Tessellaation asettelu, viisi yöpalettia, sumi-e, lukupinnan alpha ja
kiven sävyt. Kiven kontrastimittaus ei ole enää luettavuuden ehto, koska tekstiä ei ole kiven
päällä, mutta se pätee jos siihen palataan.

**Tabletin mittakaava ratkesi samana iltana.** Kohtaus seurasi leveyttä kuten seinän maalaus,
joten hahmot olivat tabletilla 1,8-kertaisia puhelimeen nähden. Tommi: *"hahmot pienemmiksi
tabletilla."* Yksiköllä on nyt yläraja 400 dp (`DgCaveUnitMaxDp`), puhelimen leveyden
luokkaa, joten puhelimella se ei vaikuta ja tabletilla hahmot ovat samaa fyysistä kokoa kuin
puhelimella. Sivuvaikutus on että tabletin vapaa alue on nyt yksikköön nähden korkea, joten
toinen saalis ja šamaani tulevat sielläkin mukaan. Todennettu kuudella Info-avauksella:
metsästys kolmesti kahdella saaliilla, riitti kahdesti šamaanin kanssa, kädet kerran.

**Auki jää.** Sama voimakkuuskysymys kuin ennen. Sarvikuono ja leijona. Metsästyksen juoksijat
levittyvät yhä koko leveydelle, joten tabletilla rivi on harva.

### Hakkaus: neljäs kohtaus ja toinen tekniikka (7.9.2026 ilta)

**Idea on Tommin:** *"muut tekniikat, jotka jättää vuosituhansia kestäviä kuvioita kallioon."*
Maalaus oli siihen asti ainoa tekniikka, ja se on kallion jäljistä hauraimmin säilyvä. Pohdinta
listasi hakkauksen, kuppikivet, uurtamisen, aavikkolakan raaputuksen, kiveen kaiverretun
tabula-laudan ja luonnolliset jäljet, ja tilaus oli *"hakkaus aavikkolakan raaputus ja
tabula-lauta, luonnos ensin."*

**Luonnos** (`tyokalut/hakkausluonnos.py`) piirsi kolme tekniikkaa täytekuvan kehykseen kolmella
piirtotavalla: hakkaus kuoppajonona, raaputus yhtenä katkeamattomana urana lakan läpi ja tabula
uurroksena jolla on varjo- ja valoreuna. Ensimmäinen arkki korjattiin ennen lähetystä: vaalean
kiven kuoppa oli lähes näkymätön (varjo 0,35, nyt 0,62; Tanumin museo maalaa kuviot punaisiksi
samasta syystä), kolibri ja hämähäkki eivät lukeutuneet. Tommi arkista: *"mikä tuo 6-jalkainen
eläin on?"* Se oli Nazcan hämähäkki, jonka jalkaparit sulautuivat arkin koossa. Valinta:
*"A ja D mukaan, ei nazcaa"*, eli hakkaus koodiin ja raaputus pois. Tabulasta ei sanaa, joten
se odottaa luonnoksessa.

**Hakkaus koodissa** (`DgCave.kt`, kohtaus `PECK`). Viiva on jono kuoppia: pisteet tasavälein
murtoviivaa pitkin, väli 2,2 ja säde 1,6 kertaa tiheys, sijainti ja koko arvottuina. Tummalla
kivellä kuoppa on tuoreen kiven vaalea (`#C9BBA5`), eli maalauksen käänteinen kontrasti.
Vaalealla kivellä kuoppa on varjonsa varassa: tumma puolikuu yläreunassa ja vaalea pohja sen
alla, kaksi piirtoa per kuoppa. Aiheet ovat Altan ja Tanumin kuvakielestä omina piirroksina:
kaksi hirveä, vene kuuden hengen miehistöllä ja eläinpääkeulalla, aurinkopyörä ja jousimies.
Jousimies ja kolmas hirvi tulevat mukaan vain kun korkeutta on yli yksikön verran. Hakkaus on
pelkkiä pisteitä, ei viivoja eikä käsiä, ja testi vartioi että kuoppia on satoja ja ne pysyvät
alueessa. Arpa jakautuu nyt neljään kohtaukseen.

**Todennettu Tommin tabletilla 7.9.2026 illalla.** Kuusi Info-avausta kummassakin teemassa antoi
hakkauksen kahdesti molemmissa; loput jakautuivat käsille, metsästykselle ja riitille. Tummalla
kivellä kuopat ovat vaaleita ja kuvat lukeutuvat etäältä; vaalealla kivellä varjo tekee ne
näkyviksi mutta hiljaisemmiksi kuin maalaus, mikä on tekniikan oma ero eikä vika. Ensimmäinen
kaappauskierros antoi kaksitoista mustaa kuvaa, koska näyttö oli lukittunut; lukitus aukesi
pyyhkäisyllä ilman Tommia, toisin kuin `docs/TESTAUS.md` olettaa, eli tällä tabletilla ei ole
PIN-lukkoa tai se ei ollut päällä.

**Tommin päätös samana iltana: *"ei tabulaa ja kädet voi tiputtaa myös pois."*** Täytekuvan
kohtauksia on siis kolme: metsästys, riitti ja hakkaus. Cueva de las Manosin kädet olivat
kohtaus yhden illan (commit 1d4d5d3 asti), ja käsien koko kaari kirjattakoon tähän: viisi
kuviota (30e5deb), seinä mustalla (79f0879), riitin kädet šamaanin ympärillä (92ac098) ja
ryhmät täytekuvana. Jokainen kaatui laitekuvaan, ja se on enemmän havainto motiivista kuin
toteutuksista: käsi on ihmisen jälki ja hallitsee ruudun, kun eläin ja hahmo ovat kuvia.
`DgHands.kt` jää, koska käden geometria ja arvonta-apurit ovat siellä valmiina. Tabula ja
raaputus ovat `hakkausluonnos.py`:ssä funktioina muttei arkilla eikä koodissa. Testi vaihtoi
nimeä (`DgCaveLayoutTest`), koska se ei enää vartioi käsiä. Todennettu tabletilla kahdeksalla
Info-avauksella: vain kolme kohtausta, ei käsiä.

### Ottelutiedot kortissa kuten pelaajat (Tommin tilaus 8.9.2026, kesken pelisession)

Tommi pelasi 35 ottelun sessiota ja sanoi kuutiodialogin kaappauksen jälkeen: *"ottelutietoja
voisi ympäröidä laatikko, jotta ne erottaisi paremmin toiminnoista kuten pelaajatiedoillakin
on"*. Kaappauksessa (`raakasivut/sessio-8-9-ilta/nauhat/kuutiodialogi-184806.png`) sivupaneelin
keskellä oli `15 point match`, `Move 535`, `Skip Game`, `Reminders` ja `Cube reminder` samana
pinona ilman rajaa, kun pelaajakortit ylhäällä ja alhaalla olivat kehyksissä.

**Ratkaisu on sama kortti eikä uusi.** `PlayerPanelView`:n kehys irrotettiin omaksi
`Modifier.panelCard()`-funktioksi, ja ottelutiedot (turnaus, kierros, ottelun pituus,
siirtonumero) piirretään sen sisään keskitettynä. Yksi paikka mitoille, jotta kortit eivät
ajaudu erilleen. Kortti jää piirtämättä kun mitään neljästä riviä ei ole.

**Raja kulkee tiedon ja teon välissä, ei sivuston ja sovelluksen.** Kortissa on sivuston
tietoa. `Skip Game` on sivuston teko ja muistutukset sovelluksen, mutta molemmat ovat tekoja,
joten ne jäävät kortin alle kahdeksan dp:n päähän. Tämä on sama jako jonka *Sivun komennot
ovat nappeja, sovelluksen omat lisät tekstiä* (25.8.2026) teki asulla; nyt se tehdään myös
paikalla.

Käännetty 8.9.2026 session aikana, **asennettu ja todennettu vasta session jälkeen**, koska
asennus tappaa käynnissä olevan sovelluksen kesken pelin.

### Kierrosrivi `Round 3/5` eikä `Round 3` (Tommin tilaus 8.9.2026, kesken toisen pelisession)

Saman illan toinen sessio (`sessio-8-9-yo3`, 13 ottelua): *"ottelutietoihin pitäisi saada
Round 3 sijaan Round 3/5 ilmaisija"*. Lautasivun `<h3>` sanoo vain `Round 4` (mitattu
`0003_bg_move_5310938_2181.html`), ja kierrosten kokonaismäärä on ainoastaan Top Pagen
`Round`-sarakkeessa muodossa `3/5`.

**Arvoa ei haeta, luettelo muistaa sen.** Erillinen haku turnaussivulta olisi rikkonut sen
väitteen että lauta ei hae mitään mitä käyttäjä ei pyytänyt. `TopViewModel` kirjoittaa
jokaisen jäsennetyn Top Pagen kierrokset ottelutunnisteella prosessin muistiin
(`ListedRounds`, `AppContainer`), ja `BoardViewModel` kysyy sivun oman tunnisteen kierroksen
ja yhdistää sen sivun numeroon (`Loaded.roundLabel`). Sivuston viiva (liigaottelu) jää
muistamatta, koska se ei ole kierros. Kun muisti ei tiedä ottelua (profiilista avattu,
prosessi käynnistynyt suoraan laudalle), rivi on sivun oma `Round 3`.

**Ensimmäinen versio kulki reitillä ja oli riittämätön, mitattu samana iltana.** Se liitti
kierroksen `BoardRoute`n kyselyparametriksi ja päti vain napautettuun otteluun, jotta väärän
ottelun kokonaismäärä ei päätyisi `Next Game` -ketjussa oikean numeron perään. Pelisessiossa
`sessio-8-9-yo5` Tommi napautti yhden ottelun (liigaottelu ilman kierrosta) ja pelasi neljä
muuta ketjussa: Marathon #4320 näytti laudalla `Round 3` vaikka luettelossa oli `3/5`
(`nauhat/marathon-150.png`). Napautus on Tommin pelitavassa poikkeus ja ketju sääntö, joten
reittiparametri kattoi juuri sen tapauksen jota ei tarvittu. Tunnisteella haettu muisti
kattaa molemmat ilman erillistä ehtoa, ja väärän ottelun arvo ei voi vuotaa koska avain on
sivun oma tunniste. Neljä näkymämallitestiä, yksi luettelotesti ja yksi reittitesti.

Käännetty ja testattu session aikana, **asennettu ja todennettu vasta session jälkeen**,
samasta syystä kuin edellinen kohta. Ketjutapaus on todennettava seuraavassa pelisessiossa
turnausottelulla, koska laitteella ei ole nyt vuorossa olevaa ottelua.

### Sivupaneeli kotikentän vastakkaiselle puolelle (Tommin päätös 8.9.2026, kesken kolmannen pelisession)

Tommi pyysi 8.9.2026 illalla *"asetuksiin mahdollisuuden siirtää sivupaneeli oikealle"* ja
arveli että sivuston `Home boards on left side` ehkä tekee sen jo. Ei tehnyt: asetus vaikutti
vain pistenumerointiin (`BoardLook.mirrored`), ja paneeli oli aina vasemmalla (2.9.2026,
peukalon alle). Kolmesta tulkinnasta (oma asetus, sivuston asetus, molemmat) Tommi valitsi
sivuston asetuksen laudan sanoin: *"sivupaneeli sille puolella missä kotikenttä ei ole."*

~~**Puoli seuraa peilausta eikä uutta asetusta.**~~ **Kumottu 9.9.2026 illalla, ks. alla
oleva kappale.** Vanha muoto: oletuksena kotikenttä on oikealla (pisteet 1–6 alhaalla
oikealla) ja paneeli vasemmalla kuten ennen; kun `Home boards on left side` on päällä, lauta
peilautuu ja paneeli siirtyy oikealle; lähde on sama `roles.look.mirrored` kuin
pistenumeroilla, joten paneeli ja numerot eivät voi ajautua eri mieltä laudan suunnasta;
uutta lomakekenttää ei tullut, koska päätös on johdettavissa laudan suunnasta eikä ole siitä
riippumaton makuasia.

**Puoli seuraa kätisyyttä, ei peilausta** (Tommin päätös 9.9.2026 illalla: *"korjataan
kaanoni, nimeä se kätisyydeksi"*). Sivupaneeli on sillä puolella jolla pelaaja **ei** siirrä
nappuloita: oikeakätisellä vasemmalla, vasenkätisellä oikealla. Peilaus ei siirrä paneelia
lainkaan, vaan koskee laudan suuntaa, pistenumeroita ja lokerosaraketta.

**Miksi vanha sääntö kumottiin.** Sen perustelu oli että puoli on johdettavissa laudan
suunnasta. `SUBSTANSSI.md` kohta 103 osoittaa ettei ole: peilatessa siirtokäsi ei vaihtunut,
vain paneeli siirtyi, eli puoli riippuu kätisyydestä eikä kotikentästä. Vanha sääntö osuu
oikeaan kahdessa yhdistelmässä neljästä, ja molemmat virheelliset ovat aitoja tapauksia:

| Kätisyys | Peilaus | Vanha sääntö | Oikea puoli |
|---|---|---|---|
| oikea | pois | vasen | vasen |
| oikea | päällä | oikea | **vasen** |
| vasen | pois | vasen | **oikea** |
| vasen | päällä | oikea | oikea |

Rivi 2 on se jonka Tommi löysi käytössä 9.9.2026 ja joka sai hänet kytkemään peilauksen pois.
Rivi 3 on vasenkätisen oletustila, eli sama vika toisesta suunnasta. Vasenkätisyys ei siis
ole erillinen kysymys vaan sama korjaus.

**Korjauksen ensimmäinen hyötyjä on Tommi itse.** Hän luopui peilauksesta jota oli kokeillut
ja halunnut, ja hinta oli paneeli. Irrotettuna peilauksen saa takaisin ilman sitä hintaa:
lauta ja lokerosarake kääntyvät, paneeli jää vasemmalle.

**Lokerosarake jää peilauksen varaan** eikä seuraa kätisyyttä, koska se on laudan osa eikä
käden alla; sen perustelu on seuraavassa kappaleessa eikä muutu.

Asetus on **sovelluksen oma** (`docs/ASETUKSET.md` luku 3, sama laji kuin pystylukko), ei
sivuston kenttä, koska kätisyys on laitteen ja pelaajan ominaisuus eikä tiliasetus. Oletus on
oikeakätinen, jolloin käytös on sama kuin ennen tätä muutosta.

**Todennettu 8.9.2026 klo 22.59 asennuksen jälkeen** (`sessio-8-9-yo4/nauhat/lauta-paneeli.png`):
Tommilla asetus on pois, kotikenttä on oikealla (1–6 alhaalla oikealla) ja paneeli
vasemmalla, eli sama kuin ennen. Peilattu tapaus on todentamatta laitteelta, koska se vaatii
Tommin sivustoasetuksen kytkemisen. Kirjoitin ensin väärin että asetus olisi hänellä päällä;
`ASETUKSET.md`:n taulukon sarake *Vaikutus lautaan* ei kerro hänen arvoaan.

Toteutus on `BoardScreen`in rivissä: sama `SidePanel` piirretään joko ennen lautaa tai sen
jälkeen, välissä sama `SIDE_PANEL_GAP`. Ehto `fits` on ennallaan.

Käännetty session aikana, **asennettu ja todennettu vasta session jälkeen**, kuten saman
illan aiemmat tilaukset.

**Peilattu tapaus todennettiin 9.9.2026 kuorella, ja se paljasti että mitään ei ollut
liikkunut.** Todennus tehtiin ilman että Tommin sivustoasetusta kytkettiin: kuoriproxy
(`tyokalut/kuoriproxy.py`) sai neljannen sivun, eli `/bg/profile`n jossa kenttä `7` on
rastittu. Airlock takaa ettei sivustolle mene mitään, joten profiili pysyi koskemattomana.

Sovellus luki asetuksen oikein (`home_boards_left = true` säilössä), mutta **lauta ei
peilautunut eikä paneeli siirtynyt**. Syy oli `BoardLook.x22()`, joka asetti `mirrored`in
kiinteästi epätodeksi ennen kuin asetusta katsottiin. Vika osui juuri siihen tyyliin joka
Tommilla on päällä, ja `BoardLookTest` vartioi sitä nimenomaisella testillä, eli kyse oli
kaanonista eikä lipsahduksesta. Tommin päätös: peilaus myös X-22:ssa, *"sivupaneelin
liikkuvuus oli paha puutos"*. Ks. `docs/ASETUKSET.md` luku 4.

**Peilaus kytkettiin pois 9.9.2026 iltapäivällä, ja syy on pelaamisessa eikä toteutuksessa.**
Tommin sanat illalla: *"kytkin pois muutama tunti sitten, se hidastaa kaksikätistä
pelaamista."* Tämä kumoaa saman päivän yöllä kirjatun *peilattu näkymä jää pysyväksi*, joka
oli kirjoitettu yhden session kokeilun perusteella. Kokeilu kesti siis alle vuorokauden, ja
pidempi käyttö kaatoi sen. **Toteutus jää paikalleen**, koska vika jota 9.9. yöllä korjattiin
oli aito (`BoardLook.x22()` jätti asetuksen lukematta), ja peilaus toimii nyt sitä haluavalle.
Muuttunut on vain Tommin oma tila, eikä sitä pidä lukea ominaisuuden kumoamiseksi.

*Mitä tästä ei tiedetä.* Mekanismia ei ole avattu, eli sitä miksi peilaus hidastaa juuri
kaksikätistä pelaamista. Kirjattu on Tommin lause sellaisenaan; jos syy tarkentuu, se kuuluu
`SUBSTANSSI.md`:hen pelaajatietona eikä tänne.

**Lokerosarake seuraa kotikenttää** (Tommin tilaus samana yönä korjauksen jälkeen: *"kun
sivupaneeli oikealla niin off-rack vasemalla, koska siellä pienimmät luvut"*). Ensimmäinen
korjaus siirsi paneelin muttei saraketta, joka jäi oikeaan laitaan kotikentästä erilleen.
Sarake on se paikka johon nappulat kannetaan ulos, ja ulos kannetaan kotikentästä, joten se
kuuluu pisteiden 1–6 viereen. `BoardScreen`in rivi järjestää nyt kolme palaa (pelialue,
kehyskaista, sarake) peilauksen mukaan, ja numerorivin varaus siirtyy samalle puolelle.

Molemmat todennettu kuvista: `sessio-9-9-yo/kuori/paneeli-vasemmalla-x22.png` (vika),
`paneeli-oikealla-offrack-oikealla.png` (ensimmäinen korjaus) ja
`paneeli-oikealla-offrack-vasemmalla.png` (valmis).

### Tarjottu kuutio näyttää tarjotun arvon, ja peruutetun sivun `Accept` on kuution hyväksyntä (8.9.2026)

Kaksi asiaa samasta pelisessiosta, ja jälkimmäinen on korjaus 5.9.2026 tehtyyn luentaan.

**Tarjottu kuutio näyttää tarjotun arvon** (Tommin tilaus YrjoSuomelain tarjouksen
kohdalla, kaappaus `raakasivut/sessio-8-9-ilta/nauhat/tuplattu-190132.png`: kuutiossa oli
robotin pää ja sivupaneelin alla *YrjoSuomela has doubled.*). Sivu ei käännä kuutiota ennen
hyväksyntää: tarjouksen aikana kuva on yhä `cube1.gif` ja ALT `1` (`sessio-8-9-ilta/0100`).
Fyysisessä pelissä tarjoaja kääntää kuution ja työntää sen vastustajalle, eli tarjouksen aikana
kuutiossa lukee tarjottu arvo. Sovellus näyttää siksi arvon kaksinkertaisena kun lomakkeessa on
`Accept` (`shownCube`, `ShownCubeTest`): keskellä oleva 1 näkyy kakkosena ja omistettu 2
uudelleentuplauksessa nelosena. DR pysyy DR:nä ja tuntematon ALT sellaisenaan. Sivun `1` on
tässä sama keinotekoinen merkintä joka perusteli robotin pään, joten "ALT sellaisenaan" ei
rikkoudu enempää kuin se jo rikkoutui.

**Peruutetun sivun `Accept` on kuution hyväksyntä, ei peruutuksen.** 5.9.2026 kirjattiin
vahvistusruudulle oma runko *This accepts your opponent's rollback* sivusta jossa oli
peruutusrivi, `Accept`, `Decline` ja rasti, kuutio ykkösessä eikä noppia. Teppo 77:n sivu
8.9.2026 (`sessio-8-9-ilta/0104`, kaappaus `nauhat/tuplattu-tony77-190442.png`) oli samaa
muotoa, ja siinä luki myös *Teppo 77 has doubled.* Kun 5.9. sivu luettiin kaappauksesta
uudelleen (`sessio-5-9-ilta2/0015`), siinäkin luki *eemelihk has doubled.* Molemmat olivat
siis tavallisia tarjouksia joiden edellä sivu kertoi peruutuksesta. Tommi hyväksyi Teppo 77:n
tarjouksen rastin kanssa, ja vastaus oli omistettu `cube2.gif`. Peruutusrunko ja sen merkkijono
poistuivat, `cubeDialogBody` ei enää lue `rolledBack`ia, ja testi vaihtui vastakkaiseksi.
Väärä teksti ehti olla ruudulla 5.9.–8.9.2026; Tommi ei lukenut sitä hyväksyessään, ja
nauha `nauhat/dg-8-9-ilta-accept.mp4` näyttää sen viimeisen kerran.

**Mitä tästä opittiin luennasta.** 5.9. sivu tulkittiin ruudulta ja muistista, ei kaappauksesta.
Kaappauksessa tilanneteksti oli koko ajan luettavissa, ja se ratkaisi asian yhdellä rivillä.
Käännetty 8.9.2026 session aikana, asennus session jälkeen.

### Tilanneteksti keskikaistan vapaalle puoliskolle (Tommin päätös 8.9.2026)

Tommi sanoi YrjoSuomelain tarjouksen kohdalla että *YrjoSuomela has doubled.* sopisi
keskipaneeliin, ja tarkensi kysyttäessä: laudan keskikaistalle, sinne missä kuutio ja nopat
näkyvät. Teksti oli siihen asti sivupaneelin pohjalla nimikortin alla (24.8.2026 alkaen
`situation`, ei `prompt`).

**Puolisko valitaan säännöllä eikä kiinteästi** (`situationSlot`, `SituationSlotTest`).
Vastustajan puoli kun siinä ei ole noppia, koska teksti kertoo yleensä hänen teostaan
(*has doubled*, *declines the cube action*). Muuten oma puoli, jos siinä ei ole noppia eikä
tarjottua kuutiota. Kun kumpikin on varattu, teksti jää paneeliin kuten ennen. Kun napit ovat
kaistalla eli paneelia ei ole, kaista on täynnä ja sääntö palauttaa null: puhelimen ruutu ei
muutu tästä lainkaan. Lokero on sama ja samanlevyinen kuin nopilla, joten teksti on kohdakkain
sen kanssa mitä se kommentoi. Asu on sama kuin paneelissa oli (pieni, vaimea, huopatausta).

Peruutusrivi (*Rolled back*) ei siirtynyt, koska pyyntö koski tilannetekstiä ja peruutus on
sivun tila eikä vastustajan teko. Käännetty 8.9.2026 session aikana, asennus session jälkeen.

**Kuittaus 8.9.2026 klo 23.1x kesken pelisession:** *"Please make a checker move. on oikealla
paikalla!"* Todennettu siis Tommin omin silmin oikeassa pelissä, ei vain kuorella.

**Varamuoto kuitattu 20.9.2026** (*"Kuittaus, kelpaa sellaisenaan"*). Avausheitossa kumpikin
lokero on nopan varaama, ja teksti jäi paneelin pohjalle nimikortin alle; ensimmäinen kerta
kun tapaus osui nauhalle oikeassa pelissä (`raakasivut/sessio-20-9-ilta/kehykset/
avausheitto-2206.png`, bertta, `DG 8x8 2026 - H vs C - Div 1`). Tommi näki kehyksen ja
hyväksyi sijainnin, joten avausheitto ei tarvitse omaa lokeroa.

### Kuutiomuistutus on pelkkä korostus, rivi laudan alta pois (Tommin päätös 14.9.2026)

Kysymys tuli päiväsession kehyksistä: Tommi kysyi sopisiko *Think about doubling*
keskikaistalle vastustajan puolelle, samaan lokeroon kuin tilanneteksti. Lokero on jaettu
(`situationSlot`): siinä ovat vastustajan nopat tai sivun tilanneteksti, ja molemmissa
session ruuduissa joissa muistutus näkyi, lokero oli jo varattu (`sessio-14-9-paiva`,
kehykset `b003` ja `b004`). Valintakysymyksen neljästä vaihtoehdosta Tommi valitsi
*"Vain kuution korostus, ei tekstiä lainkaan"*.

**Rivi ei enää piirry laudan alle.** `ReminderStrip` suodattaa kuutiomuistutuksen pois
samalla merkkijonolla jolla se kirjoitetaan, ja muut muistutukset näkyvät kuten ennen.
Kuution korostus (31.8.2026, robotin silmät ja kehys) on ainoa merkki, ja se riittää koska
se on sekä katseen alueella että jo valmiiksi se mitä painetaan. Samalla lauta ei enää
kapene rivin verran kun muistutus on asetettu (mitattu päiväsessiossa: paneeli 150 px ja
lauta alkoi kohdasta 187, muissa kehyksissä 137 ja 173).

**Poisto on sama linkki kuin lisäys.** Rivin poistoruksi katosi rivin mukana, ja
`Reminders`-linkki avaa kirjoituskentän eikä luetteloa, joten `Cube reminder` toimii nyt
kytkimenä: painallus lisää muistutuksen kun sitä ei ole ja poistaa sen kun on. Linkin sana
ei vaihdu, koska tila näkyy kuutiosta linkin vieressä. Se on ensimmäisen käsikokeilun
muoto, ja sana vaihtuu jos kokeilu näyttää ettei kuutio riitä kertomaan tilaa.

Muistutuksen elinkaari, tunniste ja rajaus (ei kutsu pelaamaan) ovat ennallaan, ks.
*Muistutus itselle laudan alla (23.8.2026)*.

**Todennettu kuoriproxylla 14.9.2026 heti käännöksen jälkeen**, päiväsession sivulla `0034`
(brunok, peruutustila, `Roll Dice` ja `Double`), ottelutunnus sovitettu aamun Top Pagen
ensimmäiseen riviin. Ensimmäinen painallus: kuution kehys ja robotin silmät syttyivät, laudan
alle ei tullut riviä, paneelin leveys ei muuttunut. Toinen painallus: korostus sammui.
Airlock 503 ennen ajoa, purun jälkeen `http_proxy` `:0`, tunneli poistettu, kuori tapettu
portin PID:stä, `dg_site_settings.xml` ennallaan (profiili oli aamun oma sivu). Tommin
käsikokeilu on vielä tekemättä.

### Katkotilan Refresh keskittyy kuutioon ja peittää sen (Tommin havainto 14.9.2026)

*"Kun lauta ei vastaa, niin Refresh-napin pitäisi peittää tuplauskuutio kokonaan (jos
tuplauskuutio on keskellä)."* Päätös 7.9.2026 sanoi jo saman (nappi omistamattoman kuution
päälle), mutta 8.9.2026 lisätty vierusteksti rikkoi sen hiljaa: nappi ja teksti keskitettiin
yhtenä rivinä, joten napin keskipiste siirtyi tekstin leveyden puolikkaan verran vasemmalle
ja kuutio pilkotti napin alta.

**Nappi on nyt kaistan keskipisteessä ja teksti sijoitetaan sen viereen siirtämättä nappia**
(`UnconfirmedRefresh`, oma `Layout`). Teksti saa vain napin oikealle puolelle jäävän tilan.
Napin vähimmäiskoko on kuutio ja 6 dp reunus joka suuntaan (`UNCONFIRMED_COVER_MARGIN`),
koska pelkän tekstin mitoittama nappi ei riittänyt peittämään kuutiota korkeussuunnassa
tabletin nappulakoolla.

Todennettu kuorella samana päivänä: kuori sammutettu, `Roll Dice` painettu (yhteys
kieltäytyi, ei mitään sivustolle), dialogi suljettu. Nappi kuution päällä ilman että kuution
kulma näkyy, *No connection* oikealla. Testin odottava teko hylätty `Discard`illa.

### Rastittoman kuutioteon huomautus on rivi eikä kortti (Tommin tilaus 14.9.2026)

*"Puuttuvasta checkboxista huomauttava teksti laudan yläosassa pitäisi yhtenäistää muiden
viestien kanssa."* Se oli viimeinen `Card`-muotoinen huomautus laudalla sen jälkeen kun
peruutus (3.9.2026) ja arvattu asema (1.9.2026) luopuivat kortista, ja se erottui kaikista
muista riveistä: eri kirjasin, eri tausta ja oma `Dismiss`-nappi.

**`VerifyHintRow`** piirtää saman virkkeen (*Tick Verify Double first. Nothing was sent.*)
laudan yläreunaan samassa asussa kuin tilanneteksti ja peruutusrivi: `labelSmall`,
vaimea väri, huopatausta, pyöristys 6 dp. Kuittausnappia ei ole. Rivi katoaa kun rasti
laitetaan, kun lomake vaihtuu tai kun riviä napautetaan. `BoardNotices` ja
`board_notice_dismiss` poistuivat, koska tämä oli niiden ainoa käyttö.

Todennettu kuorella samana päivänä brunok-sivulla `0034`: `Double` ilman rastia, rivi
laudan ylälaidassa muurin kohdalla, lauta ei liikahtanut. Tulkinta "yhtenäistää" luettiin
asuna eikä paikkana: rivi jäi laudan yläreunaan, koska Tommi nimesi paikan kysymyksessä.

### Rastihuomautus keskikaistalle tilannetekstin alle (Tommin päätös 14.9.2026, sama ilta)

Jatko edelliseen: *"yhtenäistä rastihuomautus myös paikan suhteen, sopisiko se keskikaistan
doubles you -tekstin alle"*. Accept-tapauksessa se sopii suoraan, mutta Double-tapauksessa
vastustajan puoliskolla ovat hänen noppansa eikä tekstiä, ja se on tavallisempi tapaus.
Tommi pyysi vaihtoehdot kuvina, ja neljä paikkaa piirrettiin päivän kaappaukseen (oma
puolisko, vastustajan noppien alle, laudan yläreuna, sivupaneeli rastin viereen) sekä
Accept-tapaus nauhan kehykseen `b004`. Valinta: **A, omalle puoliskolle**.

**Sääntö on `verifyHintSlot`** (`SituationSlotTest`, kolme tapausta): tilannetekstin alle
samaan lokeroon kun teksti on kaistalla, muuten sama sääntö kuin tilannetekstillä eli
vastustajan puoli kun siinä ei ole noppia ja oma puoli kun siinä ei ole noppia eikä
tarjottua kuutiota. Kun kaistalla on napit (puhelin), rivi jää laudan yläreunaan
(`VerifyHintRow`, nyt varapaikka). Lokero on pystypino (`StripNote` kummallekin riville,
4 dp väli) kaistan keskikohdassa, joten kaksi riviä kasvaa ylös ja alas yhtä paljon eikä
valu kiilojen päälle toiselta reunalta (Tommin tarkennus kesken toteutuksen: *"jos kaksi
riviä tekstiä niin keskitä ne korkeuden suhteen"*).

Todennettu kuorella samana iltana. Double-tapaus sivulla `0034`: rivi oman puoliskon
keskellä, vastustajan nopat paikallaan vasemmalla. Accept-tapaus sivulla `0035`: *brunok
has doubled.* ja huomautus allekkain vastustajan puoliskolla, pino kaistan keskellä, kiilojen
kärjet vapaana. Huomautuksen napautus kuittaa rivin kuten ennen.

### Ottelukortti kiinni vastustajan kortin alla (Tommin havainto 14.9.2026, iltasession jälkeen)

*"Häiritsee vähän kun turnauskortti liikkuu ylös alas."* Paneelin keskilohko (kortti,
muistutuslinkit, nappipino) oli kahden joustovälin välissä, joten kortin paikka riippui
pinon korkeudesta (nappien määrä, rasti) ja paneelin pohjan riveistä (peruutus,
tilanneteksti). Iltasession kehyksissä kortin yläreuna vaihteli 125:n ja 195:n välillä
900 px:n kehyksessä. Ylempi jousto poistettiin: kortti on nyt vastustajan kortin alla
vakiovälillä, ja ainoa jousto on lisien ja pinon välissä, joten pino pysyy pohjalla
(2.9.2026 päätös). Todennettu kuorella kahdella laudalla, kortin yläreuna 202 px
molemmissa.

### Liigaottelun nimi luetaan otsikosta ilman linkkiä (mitattu 14.9.2026)

Liigasivun `<h3>` on pelkkää tekstiä (`DG 8x8 2026 - H vs C - Div 1`) ilman
`/bg/event/`-linkkiä, ja `BoardParser` luki nimen vain linkistä, joten kortti näytti vain
pituuden ja siirron (kehykset `n1_001`, `n2_009`). Nyt nimi luetaan h3:sta kun linkkiä ei
ole, `, Round n` -loppu pois; turnauksessa linkki voittaa. `eventId` ja `round` jäävät
liigassa null. Testi `BoardParserTest`, todennus kuorella sivulla `0003`.

### Lomakkeen lopputulos napin alle, kaikissa lomakkeissa (Tommin päätös 14.9.2026)

**Sääntö.** Lähetyksen lopputulos (*Sending to DailyGammon.*, sivuston oma lause, virhe)
piirretään sen napin alle jolla lähetys tehtiin, ei otsikon alle eikä lomakkeen alkuun.
Ohjeteksti joka lupaa lopputuloksen etukäteen väistyy kun lause on olemassa.

**Mistä sääntö tuli.** Todennuslistan kohta 5 ajettiin oikealla luovutuksella 14.9.2026
(`raakasivut/sessio-14-9-luovutus`). Loki ja kuittaussivu menivät läpi, mutta *Matches
resigned. DailyGammon says: 1 match resigned.* ei ollut ruudulla kertaakaan: se piirrettiin
otsikon `Unfinished matches (29)` alle, ja rasti ja nappi ovat 29 rivin päässä luettelon
pohjalla. Lopputuloksen kertoi vain kadonnut rivi. Tommi: *"ilmoitus napin alle, kaikkiin
lomakkeisiin"*.

**Mihin se osui.** Kuusi paikkaa, kaikki samana iltana:

| Lomake | Ennen | Nyt |
|---|---|---|
| Luovutus (`ResignContent`) | otsikon alla, luettelon alussa | napin alla, ohjeteksti väistyy |
| Profiilin teot (kutsu, sivuutus, viesti) | nappirivin alla, lomakkeiden yllä | lomakkeiden alla, eli kutsulomakkeen `Send`in alla |
| Profiilin viestikenttä (`MessageComposer`) | `Send`in yllä | `Send`in alla |
| Inboxin vastauskenttä (`ReplyComposer`) | `Send`in yllä | `Send`in alla |
| Jonokortti (`QueueCard`) | napin yllä | napin alla, ilmoitusteksti sen perässä |
| Palstan lomake (`ComposeForm`) | lomakkeen alussa kenttien yllä | `Post`-napin alla |

**Kaksi paikkaa jäi ennalleen, ja syy on nimettävä.** Loungen liittymis- ja
ilmoittautumisilmoitukset ovat luettelon yllä, koska nappi on rivissä joka katoaa
onnistuessa: napin alle ei ole mitään jäljellä. Asetusten tallennus vastaa dialogilla,
joka on katseen kohdalla väistämättä.

**Todennus.** Luovutus kuorella (`tyokalut/kuoriproxy.py --resign`, lisätty samaan
tarpeeseen): kaappaus `sessio-14-9-luovutus/nauhat/kuori-ilmoitus-napin-alla.png` näyttää
lauseen napin alla, `OK` oikealla. Viisi muuta paikkaa on käännetty ja yksikkötestattu
mutta ei ajettu laitteella; ne todentuvat seuraavassa oikeassa lähetyksessä kussakin.

### Vastaanotettu kutsu jonokortissa, kolme nappia sivun sanoin (Tommin päätös 14.9.2026)

**Mistä.** Ackammonin suora kutsu tuli `/bg/nextgame`stä ja sovellus arkistoi sen viidesti
`Unrecognised message` -rivinä (`raakasivut/LUEMINUT.md` › sessio-14-9-kutsu). Kolmesta
vaihtoehdosta Tommi valitsi ensimmäisen: *"osallistun DG 8x8 tapaisiin turnauksiin useamman
kerran vuodessa ja kiva jos ei tarvitse selainta käyttää ollenkaan"*. Kohta 67 (selain tekee
sen paremmin) väistyy, koska kutsu tukkii jonon kunnes siihen on vastattu.

**Mitä ruudulla on.** `Take an item` tuottaa kutsulle oman tilan eikä viestiä: napin alla
*That is an invitation, not a message. It stays in the queue until you answer it, so nothing
was saved and nothing was used up.*, sen alla lause sivun sanoin (*ackammon invites you to
play a private 5 point match of backgammon with no time control.*) ja kolme nappia sivun
omilla nimillä: `Accept Invitation` (täytetty), `Decline Invitation` ja `Counter Offer`
(reunustetut). Hylkäys avaa 80 merkin kommenttikentän ja oman napin, vastatarjous avaa
profiilin kutsulomakkeen (`InviteComposer`) kutsun esivalinnoilla, `Private Match` mukaan
lukien. Hyväksyntä ja hylkäys kysyvät vahvistuksen (*Accept the invitation?* / *Decline the
invitation?*, vahvistusnappi sivun sana), vastatarjous profiilin kutsudialogin. Lopputulos
tulee nappien alle sivuston lauseena (*Answered. DailyGammon says: You have successfully
joined that game.*), ja se pysyy näkyvissä `OK`-kuittaukseen asti myös sen jälkeen kun
kutsu on poistunut kortista.

**Lautapolulla** kutsu on `NotABoardKind.Invitation`: ruutu sanoo että kutsu odottaa
Inboxissa, eikä väitä ettei sivu ole enää lauta.

**Todennus.** Kuorella (`kuoriproxy.py --nextgame`, `nauhat/kuori-kutsukortti.png` ja
`kuori-hyvaksynnan-dialogi.png`) ja oikealla kutsulla samana iltana: vastatarjous
(21.18.18) ja hyväksyntä (21.18.52) menivät läpi ja vastaussivut mitattiin. Nauha paljasti
kaksi vikaa jotka korjattiin heti: lopputulosrivi piirtyi vain kutsuosion sisällä, joten
*You have successfully joined that game.* jäi näyttämättä kun jono palasi Idleen; ja
`Private Match` alkoi tyhjänä vaikka sivu oli rastittanut sen. Korjattu versio on
asennettu, mutta sen kaksi riviä ovat laitteella todentamatta seuraavaan kutsuun asti.

### Avausheitto: vastustajan noppa hänen puolelleen pelin ensimmäisellä sivulla (Tommin toive 14.9.2026)

**Mistä.** Livepelissä kumpikin heittää yhden nopan omalle puolelleen, ja isomman heittänyt
aloittaa pelaamalla nuo kaksi silmälukua. DailyGammon tekee arvan palvelimella eikä näytä
vaihetta: pelin ensimmäinen lautasivu on jo aloittajan vuoro ja molemmat nopat ovat hänen
puolellaan (`SUBSTANSSI.md` kohta 104). Tommi 14.9.2026: *"dailygammon vaan ohittaa tämän
vaiheen, joka olisi minusta kiva tuoda peliin mukaan."* Ja tarkennus samana päivänä:
*"kunkin pelin aloittaja selviää sillä kumpi heittää isomman nopan omalle pelipuolelleen"*,
eli kyse on jokaisesta pelistä eikä vain ottelun ensimmäisestä.

**Mitä ruudulla on.** Pelin ensimmäisellä sivulla isompi noppa piirtyy aloittajan puolelle
ja pienempi vastustajan puolelle, kummankin omalla värillä, kuten livenä heitettynä. Muu ei
muutu: aloittaja pelaa yhä molemmat luvut, pelattu noppa harmaantuu siellä missä se on, ja
noppien painallus (`Noppien painallus tekona`) on ennallaan omalla nopalla. Seuraavasta
sivusta alkaen nopat ovat taas yhdellä puolella.

**Mistä pelin alku tunnistetaan.** Asemasta eikä siirtonumerosta: otsikon `Move n` juoksee
ottelun läpi (`Move 579`), joten se tunnistaisi vain ottelun ensimmäisen pelin. Ehto on
`BoardState.openingRoll`: lähtöasema (2, 5, 3 ja 5 kummallakin lähtöpisteillään, ei mitään
muurilla eikä ulkona) ja tasan kaksi eri silmälukua yhdellä omistajalla. Lähtöasema ei toistu
pelin aikana, joten esitys ei voi laueta keskellä peliä. Tasatilanteita ei näytetä eikä
keksitä, koska sivu näyttää vain lopullisen heiton (kohta 104).

**Missä se tehdään.** Malli johtaa (`openingRoll`, `withOpeningDice`), kokoaminen lukee sen
sivun laudasta eikä kootusta (`LocalComposition.boardNow`, koska ensimmäinen askel vie
aseman pois lähtöasemasta ja esityksen on pysyttävä koko vuoron), ja näkymä soveltaa sen
sivun lautaan kun kokoamista ei ole (`BoardScreen`). Lauta itse ei tiedä avausheitosta
mitään: se jakaa nopat omistajan mukaan kuten ennenkin.

**Todennus.** Domain-testi rakennetulla lähtöasemalla (`OpeningRollTest`, kahdeksan
rajatapausta) ja jäsennintesti oikealla ensimmäisellä sivulla (`BoardParserOpeningTest`,
fixture `move_opening_roll.html` 23.8.2026 sessiosta). Laitteella todentamatta: seuraava
uusi peli proxyn takana on ensimmäinen tilaisuus, eikä sitä voi tilata kuluttamatta.

### Sivun tilarivit keskikaistan pinoon (Tommin päätös 14.9.2026, myöhään illalla)

*"Mitä kaikkea viestiä tulee vasempaan alakulmaan? haluaisin yhtenäistää niitä keskelle
lautaa jos tilaa on."* Inventaario koodista: sivupaneelin pohjalla oman nimikortin alla
peruutusrivi, tilanneteksti (kun kaista oli varattu), pip-vahti ja nappulavahti; laudan alla
vasempaan reunaan tasattuna muistutukset, sivuston tuntemattomat ilmoitukset ja `Pending
Replay`. Ehdotus oli siirtää ne jotka kertovat sivun tilasta (peruutus, tilanneteksti,
vahdit, toistojono) ja jättää muistutukset (pelaajan omia, poistoruksilla) ja tuntemattomat
ilmoitukset (pituus tuntematon) laudan alle. Tommi: *"ehdotuksen mukaan, rivitä fiksusti,
pino saa kasvaa kiilojen päälle."*

**Mitä ruudulla on.** Sama lokero kuin tilannetekstillä 8.9. alkaen ja rastihuomautuksella
saman päivän aiemmin: vastustajan puolisko kun siinä ei ole noppia, muuten oma kun siinä ei
ole noppia eikä tarjottua kuutiota, ja puhelimella (napit kaistalla) ei mitään. Pino on
järjestyksessä peruutus, tilanneteksti, pip-vahti, nappulavahti, toistojono ja viimeisenä
rastihuomautus. Vahtirivit pitävät värinsä ja lihavointinsa, koska ne ovat varoituksia.
Rivi rivittyy lokeron leveydellä ilman rivirajaa (ennen kaksi riviä ja kolme pistettä), ja
pino kasvaa kiilojen päälle kun rivejä on monta. Kun lokeroa ei ole, kaikki on paikallaan
kuten ennen: paneelin pohjalla ja laudan alla.

**Missä se tehdään.** `stripNotes` kokoaa listan (`StripNoteSpec`: teksti, väri, lihavointi)
samoilla ehdoilla kuin paneelin rivit olivat; `situationSlot` saa `hasNotes`in tilannetekstin
sijaan ja sääntö on ennallaan (`SituationSlotTest`). `SidePanel.showNotes` piilottaa neljä
riviä kerralla, ja `Pending Replay` laudan alla piirtyy vain kun lokeroa ei ole.

**Todennettu kuorella samana iltana** peruutussivulla `sessio-14-9-paiva/0034` (brunok,
tunnus sovitettu illan Top Pagen ensimmäiseen riviin): *Rolled back: your opponent did not
play the site's guess.* omalla puoliskolla vastustajan noppien vieressä, paneelin pohja
tyhjä ja nappipino laskeutui vapautuneeseen tilaan. Kuva `sessio-14-9-paiva/kuori/
kaista-peruutusrivi.png`. Ensimmäinen ajo näytti vanhan version, koska kaistamuutosta ei
ollut asennettu; toinen ajo asennuksen jälkeen. Vahtirivit ja usean rivin pino ovat
laitteella näkemättä, koska kuorisivuilla vahdit täsmäävät.

### Ottelukortti pelaajakorttien puoliväliin yksin, lisät sen yllä ja pino pohjalla (Tommin valinnat 14.9.2026 illalla)

*"Sopiiko ottelutiedot aina vasemmalle korkeus-keskitettynä?"* Neljästä tulkinnasta Tommi
valitsi tämän: kortti geometrisesti vastustajan ja oman kortin väliin, nappipino pohjalla
omana kerroksenaan. Saman päivän aiempi korjaus (yllä) vei kortin kiinni vastustajan kortin
alle, koska kahden joustovälin keskitys hyppi pinon mukana. Keskitys palasi eri
mekanismilla: `CenteredOverStack` mittaa kortin, sen yläpuoliset lisät ja pinon erikseen ja
laskee kortin paikan lohkon koko korkeudesta (`DgBoard.centeredTop`), joten pino ei työnnä
korttia. Kortti väistää ylös vain jos se muuten osuisi pinoon, alas vain jos lisät eivät
muuten mahtuisi, ja pinon väistö voittaa. Testi `DgBoardPanelTest`.

Ensimmäinen versio keskitti kortin yhdessä `Skip Gamen` ja muistutuslinkkien kanssa, ja
kaappaus klo 23.03 näytti kortin keskikohdan yläpuolella. Tommin tarkennus: *"siirrä Skip
Game ja reminders ottelukortin yläpuolelle"*. Nyt kortti itse on keskellä ja lisät roikkuvat
sen yläreunasta. Todennettu kaappauksesta klo 23.13 (bertta, `Next`-lauta): vastustajan
kortin alareuna 178 px ja oman yläreuna 1043 px näyttökuvassa, kortti 542–680 px eli
keskikohta 611 px kun välin keskikohta on 610 px.

### Pipit pelaajakortin kolmantena rivinä, muurin väli taas tyhjä (Tommin valinta 14.9.2026 illalla)

*"Pips paikka on hyvä, mutta Pips ja +-tilanne pitäisi olla mukana, että sen hahmottaa."*
Ensimmäinen toteutus yritti `Pips 131 (+5)` muurin väliin numerorivillä kun leveys riittää,
ja kaappaus klo 23.03 näytti että tabletillakaan se ei riitä: väli on yksi sarake
kolmestatoista ja teksti kolmen mittainen. Sitten Tommi ehdotti keskikaistaa, pyysi
visualisoimaan vaihtoehdot (neljä kaistalle, yksi pelaajakorttiin) ja valitsi kortin:
*"entä jos pips laittaisi henkilökorttiin?"*, *"kolmas rivi"*.

Pipit ovat nyt `PlayerPanelView`in kolmas rivi (`Pips 106 (-7)`), korostusvärillä kuten
ottelun pituus, ja lokerosarakkeessa sama ilman sanaa (`board_pips_short`), koska sarake on
kapea ja ilman sitä puhelin jäisi ilman pippejä. Muurin väli numerorivillä on tyhjä kuten
ennen 15.8.2026; `BarPips`, `pipFontSp`, kutistuslattia ja niiden testi poistuivat, koska
niillä ei ole enää kutsujaa. Etumerkki oli 14.9. kilpajuoksun etumatka (`DgBoard.pipLead`):
plus kun tämä pelaaja on edellä eli toisella on enemmän pippejä, `0` tasan. Suunta oli Clauden
valinta eikä Tommin, koska yksikään valittu vaihtoehto ei sanonut sitä. **Se tuntui väärältä,
ja kääntyi 15.9.2026 yöllä** pelisession kesken (Tommi: *"pips +- on väärinpäin, pienempi on
parempi ja -"*): luku on nyt oma miinus toisen, eli miinus kun tällä pelaajalla on vähemmän
pippejä. Saatteen 14.9. rivi *"pip etumerkit on hyvin"* koski kaappausta ennen pelaamista;
pelatessa suunta luettiin erotuksena kuten pip-laskurit yleensä. Todennettu kaappauksesta
14.9. klo 23.13 vanhalla suunnalla: bertta `Pips 106 (-7)`, tommih `Pips 99 (+7)`, muurin
kohta numerorivillä tyhjä; uudella suunnalla samat luvut ovat `(+7)` ja `(-7)`. Tommin tarkennus kaappauksen jälkeen:
*"jos tilaa on niin Pips riviä voisi edeltää tyhjä rivi"*. Paneelikortissa pipejä edeltää
nyt yhden tekstirivin mittainen väli, lokerosarakkeessa ei. Todennettu kaappauksesta klo
23.16.

### Pelaajakortti avaa profiilin (Tommin toive 14.9.2026 illalla)

*"Nimikortti voisi olla linkki pelaaja-näkymään."* `BoardParser` lukee nyt pelaajan
linkistä myös `profilePath`in, ja kortti on napautettava kun polku on. Ilman polkua (nimet
linkittöminä sivuston asetuksella, `move_board_no_name_links.html`) kortti ei näytä
napautettavalta. Avaus menee `onOpenPage`-koukulla samaan lukunäkymään kuin päättyneen
ottelun profiililinkit. **Todennettu napauttamalla 21.9.2026** Pixel 8a:lla kuoriproxyn
uudella `--user`-reitillä (idea tuli Tommilta uudelleen, mikä kertoi ettei todennusta
ollut): vastustajan kortin napautus haki `/bg/user/24994` (kuoren loki `KAYTTAJA`) ja
pelaajanäkymä avautui pystyyn, `raakasivut/sessio-21-9-paiva/kehykset/
kannykka-pelaajakortti-napautus.png`. Tommin päätös samana päivänä: pelkkä todennus, ei
napautettavuuden merkkiä korttiin.

### Vahvistusruutu vasempaan laitaan ja irti napista (Tommin pyynnöt 14.9.2026 illalla)

*"Tasaa Verify-checkboxit vasempaan laitaan"*, ja kaappauksen jälkeen *"hieman rako
korkeussuunnassa"*. Pino keskitti rivin, ja ruutu oli nappien välissä irrallaan reunasta.
Rivi saa nyt paneelipinossa koko leveyden ja pinon oman välin toiseen kertaan yläpuolelleen
(`verifyRowModifier`), jolloin ruutu on nappien vasemman reunan kohdalla eikä kiinni
`Doublessa`. Keskikaistalla rivi on ennallaan oman sisältönsä levyinen. Laita todennettu
kaappauksesta klo 23.03 ja rako klo 23.15 (CarlaLaine, `Double` tarjolla).

### Vahvistusruutu irti tekstistä ja kuutioteko kehykseen (Tommin havainnot 15.9.2026 yöllä)

Pelisession kesken kaksi havaintoa. Ensin *"checkbox ja verify ovat kiinni toisissaan"*:
20 dp:hen rajatulla ruudulla ei ole Materialin kosketusmarginaalia, joka muuten pitää tekstin
irti, joten `VerifyRow` sai 6 dp:n vaakavälin, saman kuin pinon oma väli. Sitten *"verify
kokonaisuus ehkä parempi leveys-keskitettynä tai onko parempaa tapaa osoittaa sen kuuluvuutta
yllä olevaan"*. Neljä vaihtoehtoa (yhteinen kehys, sisennys, ruutu napin viereen samalle
riville, leveyskeskitys), ja Tommi valitsi kehyksen. Keskitys olisi ollut heikoin, koska kaikki
muukin pinossa on keskellä tai koko leveydellä, jolloin keskitetty rivi lukee pinon omana
rivinä eikä napin alarivinä.

`VerifyFrame` piirtää paneelipinossa peruuttamattomat napit ja vahvistusruudun saman
reunuksen sisään, reunus kuution pehmeällä värillä kuten peruuttamattoman napin oma reunus
ja kulma sama kuin korteilla. Sisus on 6 dp, joten kehystetyt napit ovat 12 dp kapeampia kuin
`Roll Dice`, ja se on osa merkkiä eikä virhe. `Accept | Decline | Verify Accept` menee samaan
kehykseen, koska kaikki kolme ovat samaa kuutiotekoa. Keskikaistalla kehystä ei ole, koska
siellä kohteet ovat vierekkäin ja ruutu on oma kohteensa kuten ennen. Rako napin ja rivin
välissä (`verifyRowModifier`, 14.9.) säilyi kehyksen sisällä. **Todennettu kuoriproxylla 15.9.2026
klo 00.20** session tarjoussivulla (`sessio-14-9-yo2/0020`, RaimoKoski on tuplannut): kehys
`Accept`in, `Decline`n ja `Verify Accept`in ympärillä kuution värillä, rasti irti tekstistä,
ja pipit `RaimoKoski 109 (-48)`, `tommih 157 (+48)`. Kuori tarjosi vain Top Pagen ja laudan,
`/bg/profile` sai 503, joten laitteelle ei jäänyt asetusjälkeä.

**`Roll Dice`-muoto todennettu oikeassa pelissä 15.9.2026 klo 01.10–01.12** (`sessio-15-9-yo`,
kehykset `k006`, `k016`, `k023`): `Roll Dice` koko leveydellä ja sen alla kehys, jossa
kapeampi `Double` ja `Verify Double`. Pino pysyi ottelukortin ja oman kortin välin keskellä
myös nelirivisen ottelukortin kanssa. Kehys ja `Undo Move` eivät kohtaa koskaan, koska
sivusto tarjoaa `Double`n vain ennen heittoa: heiton vastaussivulla on `Submit Move` yksin.
Se on DailyGammonin sääntö eikä sovelluksen valinta, joten tilaa "kehys ja `Undo` samassa
pinossa" ei tarvitse suunnitella.

### Vahvistusruutu kehyksen keskelle (Tommin tilaus 15.9.2026 illalla kesken pelisession)

*"kaappaa jotta näet mitä tarkoitan, keskitä verify korttiinsa"*. Kaappaus klo 23.58
(CarlaLaine, `Double` tarjolla, `sessio-15-9-ilta3`): `Double` on kehyksen keskellä, mutta
`Verify Double` on kehyksen vasemmassa laidassa. Vasen laita oli 14.9. valinta pinoon jossa
kehystä ei vielä ollut, ja ruutu haki silloin nappien reunaa. Kehyksen sisällä ruutu on
kortin oma rivi, ja kortti keskittää sisältönsä kuten nimikortit. `VerifyFrame` sai
`horizontalAlignment = CenterHorizontally`, ja kehyksen sisällä `VerifyRow` on sisältönsä
levyinen (vain 6 dp:n yläväli), jotta kehys voi keskittää sen. Kehyksettömässä pinossa
`verifyRowModifier` on ennallaan koko leveydellä, keskikaistalla ennallaan. Yllä oleva 15.9.
yön perustelu keskitystä vastaan koski kehyksetöntä pinoa, ja kehys muutti sen: rivi ei enää
lue pinon omana rivinä vaan kehyksen rivinä. Käännetty klo 23.59, asennettu 16.9. klo 00.06
session jälkeen. **Ei vielä todennettu laitteella**, koska `Double` ei ollut enää tarjolla.

### Lisät ja pino omien väliensä keskelle (Tommin tarkennus 14.9.2026 illalla)

*"Jos tilaa on niin osiot nimikorttien ja ottelukortin välissä voisi korkeus-keskittää"*,
ja peruste: *"kokonaisuuksia tällöin erottaisi tunnistettava tila."* `CenteredOverStack`
sijoittaa nyt lisät (`Skip Game`, muistutuslinkit) vastustajan kortin ja ottelukortin välin
keskelle ja pinon (sivun napit, vahvistusruutu) ottelukortin ja oman kortin välin
keskelle. Pino oli 2.9.2026 alkaen pohjalla nimessä kiinni vahvistusruudun mittaisella
välillä; se väli poistui, koska keskitys tekee saman. Ahtaassa lohkossa välit ovat nollaa
ja kaikki on kiinni kortissa kuten ennen. Todennettu kaappauksesta klo 23.19 (Rami,
`Next`-lauta): lisät 155–215 px välissä 105–270 ja `Next` 405–437 px välissä 340–505
puolikokoisessa kuvassa, eli kumpikin välinsä keskellä kahden pikselin tarkkuudella.

### Toistojonorivi näkyi kahdesti (mitattu 14.9.2026 klo 23.14, korjattu samana iltana)

Ramiin laudalla `Pending Replay: 1` oli sekä keskikaistalla että laudan alla. Syy oli
`LoadedBoard`in paikallinen muuttuja `notesOnStrip`, joka asetettiin `BoxWithConstraints`in
sisällä: se koostaa sisältönsä vasta mittauksessa, eli sen jälkeen kun sarakkeen loput oli
jo koostettu, joten laudan alla oleva lukija näki aina epätoden. Muuttuja on nyt
`remember { mutableStateOf }`, jolloin kirjoitus koostaa lukijan uudelleen. Vika oli
commitista 778cf24 (14.9.2026) asti, eli saman päivän. Todennettu kaappauksesta klo 23.19:
rivi vain kaistalla.

### Omistettu kuutio muurin päähän, lokero poistetuille (Tommin tilaus 14.9.2026 illalla)

*"Nyt entiselle pips-paikalle mahtuisi omistettu tuplauskuutio, tällöin nappulalokero olisi
poistetuille pyhitetty"*, ja tarkennus mittojen jälkeen: *"tuplauskuutio ei numeroiden
sekaan tilaa viemään vaan muurin ylä- tai alaosaan."* Numerorivi on noin 20 dp ja kuutio
noin 36 dp, joten riviin se ei mahdu ilman että lauta menettää korkeutta. Muurin nappulat
pinoutuvat keskeltä ulospäin (6.9.2026), joten muurin päät ovat vapaita: vastustajan
omistama kuutio on nyt ylemmän muurisegmentin yläpäässä ja oma alemman alapäässä
(`BarSegment`), sama jako kuin lokerossa 9.8.–14.9.2026. Kapealla laudalla kuutio kutistuu
muurin levyiseksi. `CubeSlot` poistui, ja lokerosarake näyttää enää poistetut nappulat sekä
puhelimella omistamattoman tai tarjotun kuution (`CubeBadge`) kun napit ovat kaistalla.
Omistamattoman ja tarjotun kuution säännöt (3.9. ja 14.9.2026) eivät muuttuneet.
Todennettu kaappauksesta klo 23.27 (Rami): DR-kuutio ylemmän muurisegmentin yläpäässä,
lokerosarake tyhjä. Tommin jatkoajatus samalla: *"parasta olisi laittaa omistettu
tuplauskuutio pelaajakortin sisään, mutta se on tuunattu de luxe-versio"*; visualisoitu
kaksi korttivaihtoehtoa, ja Tommi valitsi B:n samana iltana: *"kortti on omistajuutta"*.

**Muurin pino väistää muurilla makaavaa kuutiota (Tommin tilaus 23.9.2026):** *"siirrä
muurilla oleva nappula nappulan verran kauemmas tuplauskuutiosta"*. Omistamaton kuutio on
kaistan keskellä muurin kohdalla, ja muurin pino alkaa juuri siitä, joten ensimmäinen nappula
kosketti kuutiota. Kun kuutio on muurilla (`cubeOnStrip`, ei tarjottu), pino alkaa nyt
nappulan verran kauempaa ja tiivistyy lyhyempään tilaan (`BarSegment.cubeGap`). Omistettu
kuutio on segmentin ulkopäässä, joten siirto veisi pinon sitä kohti; siksi ehto on kuution
paikka eikä sen olemassaolo. Todennettu tabletilla kuoriproxylla 23.9.2026
(`raakasivut/sessio-23-9-kuori/muuri-rako-kuutiosta.png`, kaksi nappulaa muurilla).

### Omistettu kuutio pelaajakortin nimen rivillä kun paneeli on (Tommin valinta 14.9.2026 illalla, B)

Tabletilla omistettu ja tarjoamaton kuutio on nyt omistajan kortissa nimen rivin oikeassa
reunassa (`PlayerPanelView`, `CARD_CUBE` 28 dp), ja muurin pää jää tyhjäksi
(`Board.ownedCubeInPanel`). Puhelimella, jossa paneelia ei ole, kuutio on muurin päässä
kuten yllä. Tarjottu kuutio menee kaistalle ja omistamaton muurille kuten ennen; kortti
saa vain omistetun tarjoamattoman, samalla ehdolla jolla `cubeOnStrip` sen kaistalta
kieltää. Oman kuution napautus tuplaa kun sivu tarjoaa `Doublea`, samaa rastia lukien
kuin nappi. Todennettu kaappauksesta klo 23.34 (Rami): DR-kuutio Ramiin kortissa nimen
tasolla, muuri tyhjä.

### Kortit omistajuutena: kehys, muistutuslippu, pituusrivi pois ja turnauslinkki (Tommin valinnat 14.9.2026 illalla)

*"Kortti on omistajuutta, voisiko siihen yhdistää edullisesti vielä jotain muuta tilaa
säästämään tai pelaajakokemusta parantamaan?"* Dokumenteista (AVOIMET, foorumiauditointi,
SUBSTANSSI) löytyi kolme, ja Tommi valitsi kaikki sekä lisäsi neljännen:

- **Kuution omistajan kortilla on oma kehys** (*"kuution omistusta korostaisi omanlaisensa
  kehys"*): `panelCard(outline = Palette.Cube)` kun kortissa on kuutio. Todennettu
  kaappauksesta klo 23.39 (Rami).
- **Pituusrivi poistui ottelukortista**, peruste SUBSTANSSI 90 ja 92: away-luku kantaa saman.
  Merkkijono `board_match_length` poistui. Todennettu samasta kaappauksesta. **Kumottu
  24.9.2026:** pituus palasi kierroksen ja siirron riville (ylempänä › *Väljä muoto
  kiristettiin*).
- **Ottelukortti avaa turnaussivun** (G4 foorumiauditoinnissa). `BoardState.eventPath`
  luetaan sivun linkistä; liigaottelulla null eikä kortti ole napautettava. Testi
  `BoardParserTest`. Todennettu napauttamalla klo 23.40: `May 26 Deja Vu` -kaavio avautui.
- **Kuutiomuistutuksen lippu** `Pick me` kuution viereen kortissa kun muistutus on voimassa,
  AVOIMET-kohdan vaihtoehto *erillinen pieni lippu*. Ei todennettu laitteella, koska yhdelläkään
  avatulla laudalla ei ollut muistutusta päällä.

Kaanoni kieltää kaksi muuta: rating ja Experience ovat vain profiilissa (SUBSTANSSI 14), ja
Grace ja Time Pool kuuluvat otteluluetteloon eikä aikapankkia esitetä hälytyksenä
(SUBSTANSSI 91 ja 93).

### Vastustajan omistama kuutio ei kanna muistutusta (Tommin havainto ja päätös 15.9.2026 päivällä)

Pelisession kesken: *"eino-ottelussa kiinnitin huomiota että cube reminder oli päällä vielä kun
tuplauskuutio oli hänen omistamansa."* Muistutus on ottelukohtainen merkki kannassa, ja
mikään ei poistanut sitä omistajanvaihdoksessa, joten korostus piirtyi vastustajan korttiin
laserkehyksenä ja `Pick me` -tekstinä vaikka tuplata ei voi. Tommi: *"kyse ei ole bugista"*,
ja hän valitsi kolmesta vaihtoehdosta kaksi yhdessä. **Korostus näkyy vain kun kuutio on
oma tai keskellä**, eli `cubeReminderShown` on epätosi kun kuution paikka on `TOP`, ja
**muistutus poistuu kun vastustaja ottaa kuution**: `LaunchedEffect` `BoardScreen`issä kutsuu
`onRemoveReminder`ia heti kun lauta näyttää kuution vastustajalla ja muistutus on olemassa.
Poisto on ruudulla eikä näkymämallissa, koska muistutus tunnistetaan resurssitekstistä
*Think about doubling*, jota näkymämalli ei tunne. `UNKNOWN`-paikka ei sammuta eikä poista,
koska se on lukuvirhe. Kolmas vaihtoehto, ennallaan jättäminen, hylättiin. Tehty kesken
session, asennus session jälkeen.

### Kortin kuutio kahdelle riville ja muistutuksen korostus kortissa (Tommin tarkennukset 14.9.2026 illalla)

Tommin kuittaukset kaappausten jälkeen: *"pip etumerkit on hyvin"* (plus on etumatka, päätös
nyt Tommin eikä Clauden), *"pelaajakortilta pääsi profiiliin"* (napautus todennettu Tommin
laitteella) ja *"olen todella tyytyväinen sivupaneelin siistiytymisestä"*.

*"Henkilökortin tuplauskuutiolle anna toinen rivi tilaa."* Nimi ja away ovat nyt omassa
sarakkeessaan vasemmalla ja kuutio oikealla niiden yhteisellä korkeudella (`CARD_CUBE`
40 dp, ennen 28 dp nimen rivillä). Pipit jäävät koko leveydelle kuution alle. Todennettu
kaappauksesta klo 23.48 (Rami).

*"Kun omistaa kuution ja kuutiomuistutus, niin siihen pitää vielä keksiä korostus
pelaajakorttiin."* Keksitty: kortin kehys vaihtuu kuution väristä laserin väriin
(`Palette.CubeLaser`) ja paksunee kahteen viivaan, eli sama väri ja sama keino kuin kuution
omalla pysyvällä korostuksella pulssien jälkeen. Lippu `Pick me` on kuution päällä samassa
sarakkeessa. Staattinen, jotta lepotila piirtää nolla ruutua. **Ei todennettu laitteella**,
koska yhdelläkään avatulla laudalla ei ollut muistutusta päällä, enkä asettanut muistutusta
Tommin peliin hänen puolestaan.

### Taustakuvat tyhjään tilaan (15.9.2026)

Tommin päätökset samalta päivältä ovat `docs/AVOIMET.md`:ssä (*Katutaide tyhjään tilaan*), ja
tässä on se mitä niistä tuli koodiin ja miksi juuri niin.

**Paikka on sama kuin luolan seinän.** Tyhjä tila on *"vain luettelojen alaosa"*, ja se on
täsmälleen se alue jonka `DgFill.kt` jo tuntee: vierivän sisällön alle jäävä vapaa tila, jonka
raja luetaan piirtovaiheessa. Taustakuvat menevät siihen luolan seinän sijasta samalla 180 dp:n
kynnyksellä. Kuvio ei ole ehto: kansio on oma kytkimensä (`WallpaperStore`), ja kuvat näkyvät
vaikka tessellaatio olisi pois. Kun kuvio on päällä ja kuvat vielä purkamatta, tila saa hetkeksi
luolan seinän. Lauta on ulkona samalla lipulla kuin kuvio.

**Lähde on laitteen kansio, ei asset.** Kuvat ovat muiden taiteilijoiden töitä, joten ne eivät
mene APK:hon. Kansio valitaan kerran asetusten laiteosiosta järjestelmän kansiovalitsimella
(`OpenDocumentTree`, pysyvä lukuoikeus, sama mekanismi kuin varmuuskopion tiedostolla), eikä
manifestiin tullut uutta oikeutta. Valitsin näyttää kaikki tarjoajat jotka antavat kansion,
mutta suositus on laitteen oma kansio: pilvikansio hakisi kuvat verkosta joka avauksella, ja
Tommin huoli 15.9.2026 illalla oli juuri se. Kuvat viedään tabletille PC:ltä
`tyokalut/taustakuvat.py`:llä, kansio on `/sdcard/Pictures/dg-taustakuvat`.

**Asettelu on peitto, ei sääntö kuvamäärästä.** Tommin sana oli *"1-3 kuvaa"* ja *"en halua että
kuvia leikkautuu"*. `dgWallpaperLayout` laskee jokaiselle määrälle yhdestä kolmeen sekä
rinnakkain että allekkain sen yhteisen mitan jolla kaikki mahtuvat kokonaisina, ja valitsee sen
jonka kuvat peittävät vapaasta alueesta suurimman osuuden. Useampi kuva voittaa vain jos peitto
kasvaa aidosti. Siitä seuraa itsestään se mitä Tommi kuvasi: matalaan tilaan pystyruudulla tuli
kolme vaakakuvaa rinnakkain (otteluluettelo, seitsemän riviä), korkeaan tilaan kaksi allekkain
(Info-välilehti). Ryhmä keskitetään ja kuvien väli on 12 dp, ja sama väli on listan viimeisen
rivin ja ensimmäisen kuvan välissä (Tommin pyyntö laitekuvasta samana iltana: kuva alkoi
suoraan rivin alareunasta). Kuva ei koskaan leikkaudu, ja ympärille jäävä tausta on
hyväksytty hinta.

**Arpa on ruudun avauksen siemen**, sama kuin taivaalla ja luolalla, joten välilehden vaihto
arpoo uudet kuvat ja saman ruudun uudelleenpiirto pitää entiset. Tämä on Tommin ensimmäinen
vaihe (*"tässä vaiheessa näkymän tahtiin"*); päivän tahti on toinen vaihe ja odottaa toteamusta
että ensimmäinen toimii.

**Lataus on kaksivaiheinen ja kaikki paikallista.** Kansion luettelo luetaan kerran per kansio
`DocumentsContract`-kyselynä (1756 riviä, ei kuvien avaamista), ja ruudun avaus purkaa vain
arvotut enintään kolme kuvaa pienennettyinä niin että pisin sivu on enintään 1600 pikseliä.
Kadonnut oikeus (kansio poistettu tai lupa peruttu) unohtaa kansion, jotta asetusruutu pyytää
valitsemaan uudestaan eikä vaikene; sama peruste kuin varmuuskopiossa.

**Ansa ensimmäisessä laiteajossa.** `BitmapFactory.decodeStream` palauttaa aina nullin kun
`inJustDecodeBounds` on päällä, ja ensimmäinen versio luki sen puuttuvaksi kuvaksi: kaikki
kuvat "puuttuivat" ja tila sai luolan seinän ilman virhettä. Kansio oli tallessa ja asetusrivi
näytti sen, joten vika näkyi vasta laitekuvasta. Korjaus tarkistaa virran erikseen.

**Havainto laitekuvista, ei vika:** osa lähdekuvista kantaa omaa letterbox-reunaansa (mustat tai
harmaat palkit tiedostossa), ja ne piirtyvät osana kuvaa koska sovellus ei leikkaa. Jos se
häiritsee, korjaus on lähteessä eikä sovelluksessa.

**Napautus näyttää nimen (Tommin tilaus 15.9.2026 illalla).** Kuvan päällä napautus piirtää
tiedostonimen ilman päätettä laatikkoon kuvan yläpuolelle, lukupinnan värillä ja
`labelMedium`-tyylillä. Toinen napautus samaan kuvaan tai napautus muualle tyhjään tilaan
poistaa laatikon, ja ruudun vaihto arpoo uudet kuvat ilman merkintää. Osuma luetaan
viimeksi piirretyistä paikoista, jotka piirto kirjoittaa (`DgWallpaper.rects`), joten
napautus ja kuva eivät voi erota. Vieritys ei ole napautus, joten lista vierii kuten ennen.
Ylimmän kuvan yläpuolella ei ole tilaa (väli listaan on 12 dp, laatikko korkeampi), ja tausta
piirtyy sisällön alle, joten sinne piirretty laatikko jäi listan rivien alle (Tommin
havainto laitteelta samana iltana). Silloin laatikko menee kuvan sisään sen yläreunaan.
Todennettu laitteella Info-välilehdellä ääkkösnimellä, molemmat paikat.

**Info-välilehden *Links* on *DailyGammon Links*** (Tommi 15.9.2026 illalla: *"siellä ei ole
muita linkkejä"*), samaa muotoa kuin *DailyGammon Help* sen yllä. Porautumisruudun otsikko
on yhä sivuston oma *Links*.

### Merkitty asema analyysiin, `Mark position` muistutusten seassa (Tommin toive 15.9.2026 illalla)

Tommi kesken pelisession: *"mark position nappi olisi kiva reminders-sekaan: siis ottelun
jälkeen näkisi mark-positiot, joita analyysin tullessa muistaisi uudelleen pohtia"*. Kolme
valintaa samana iltana: sisältö on peli, siirtonumero ja vapaaehtoinen sana; merkki säilyy
kunnes Tommi poistaa sen; ja lukupaikka on linkki otteluluettelossa, koska `.mat`-tiedosto
ei kelpaa (alla).

**Tämä ei ole muistutus vaikka linkki asuu niiden seassa.** Muistutus elää yhden pelin ja
lakkaa näkymästä pisteiden muuttuessa (`ReminderDao.observeByGame`). Merkki luetaan vasta
ottelun jälkeen, useita pelejä myöhemmin, ja analyysi voi tulla viikkoja sen jälkeen
(`SUBSTANSSI.md` kohta 20). Siksi se on oma taulunsa `marked_positions` ja oma kirjansa
`MarkBook`, jonka elinkaari on käyttäjän poisto eikä kysely eikä ajastus. Kaanonia vasten
luettu: kohta 22 (analyysi ottelun jälkeen, läpikäynti siirto siirrolta) ja kohta 27
(analyysin tuote on pelaajan muisti) kantavat tätä, ja kohta 9 ei laukea koska merkki on
viesti itselle eikä neuvo.

**Mitä ruudulla on.** Laudan toimintorivillä `Reminders`- ja `Cube reminder` -linkkien perässä
on `Mark position`, samalla alleviivauksella koska se on sovelluksen oma teko. Linkki näkyy
vain kun sivu kertoi siirtonumeron, samoin kuin muistutuskenttä vain kun pisteet on luettu:
merkki ilman numeroa ei löytyisi analyysiohjelmasta. Painallus avaa laudan alle kentän
~~`Why? (optional)`~~ `Comment (optional)` ja napin `Mark <numero>`, jossa numero on nähtävissä ennen painallusta.
Kentän nimi vaihtui 17.9.2026 (Tommin kysymys *"comments voisi olla muuten parempi nimi
napille kuin mark?"*, päätös välimuoto): nappi pysyy Mark, koska se on teko laudalla ja
Message-napin vieressä Comments lukisi viestinä vastustajalle, mutta kenttä ottaa sen sanan
jota GNU Backgammon, BGBlitz ja eXtreme Gammon käyttävät samasta asiasta, koska teksti menee
`.sgf`:n `C[]`-kommentiksi sellaisenaan.
Nappi on aina painettavissa, toisin kuin muistutuksen `Add`, koska merkin arvo on kohdassa
eikä selityksessä. Muistutuskenttä ja merkkikenttä eivät ole auki yhtä aikaa, koska laudan
alla on tilaa yhdelle riville. ~~Tehty merkki näkyy heti laudan alla rivinä *Marked for
analysis: move 377* ja sanalla, jos sana on, ja rivillä on sama × kuin muistutuksella.~~

**Rivi poistettiin 30.9.2026** (Tommi pelisessiossa `sessio-30-9-ilta2`, Pixel 8a vaakana:
*"Laudan alle tulee rivi Marked for analysis-se rikkoo sivupaneelin"* ja *"mielestäni riittää
että se jää sgf-tiedostoon"*). Nauhassa rivi vei korkeutta laudan alta, jolloin lauta ja
sivupaneeli kutistuivat. Nyt kuittaus on kentän sulkeutuminen. Merkit luetaan ja poistetaan
`Marked positions` -ruudulta, ja ne kulkevat `.sgf`:ään kuten ennenkin.

**Siirtonumero luetaan ladatulta laudalta eikä anneta ruudulta** (`BoardViewModel.markPosition`),
samoin kuin muistutuksen pistepari: pyydetty ei määrää saatua. Ilman pelin tunnistetta tai
numeroa mitään ei kirjata, ja se on rajaus eikä virhe.

**Asema tallennetaan merkin mukana 16.9.2026 alkaen** (Tommin tilaus samana iltana, *"tehdään
se Mark tieto sgf-tiedostoon mukaan"*, tulkinta kohdistus). Siirtonumero ei kohdista merkkiä
`.mat`-tiedoston siirtoon, koska sivun luku on tilalaskuri, mutta asema kohdistaa: `SgfExport`
toistaa pelin teko teolta (`MatGame.positions`) ja kirjoittaa kommentin siihen solmuun jossa
asema täsmää, tarkemmin täsmäävää tekoa seuraavaan, koska GNU näyttää solmun kohdalla aseman
ennen sen siirtoa. Asema luetaan laudalta istuimen suunnasta (`CheckerPosition.of`,
`resolveSeat`; kun pipit ovat piilossa, väri nimisolusta ja suunta katsojan) kummankin
pelaajan omaan numerointiin, samaan jota `.mat` käyttää. Kanta sai sarakkeen
`marked_positions.position` (migraatio 10→11, tekstirivi `1:` + 26 + `/` + 26 lukua), ja
vanhat merkit jäävät ilman asemaa: ne ja osumattomat (esimerkiksi kesken sivustolla
askelletun siirron tehty merkki) kirjoitetaan pelin alkuun kuten ennen. Ruutu ei näytä
asemaa; se on viennin tieto.

**Laiteajo 16.9.2026 klo 22.30–22.39 (SM-T970), kuoriproxylla vanhoilla sivuilla ja
päättyneen ottelun oikealla `.mat`:lla.** Migraatio 10→11 ajettiin laitteella: `user_version`
11, sarake `position`, kaksi vanhaa merkkiä nulliksi, 43 viestiä ennallaan, logcat puhdas.
Kuori tarjosi Top Pagen `sessio/0890` ja laudan `sessio/0881` (ottelu 5311448 Jaakkohia
vastaan, pelin 5 alku, Jaakkoh heittänyt 63: 24/18 18/15, tommih heittänyt 11). `Mark
position` ja *Mark 433* sanalla *Kuori 11* kirjoittivat rivin `(5311448, 1, 4, 433)` ja
aseman `1:…2,0/…5,0,1,…,1,0`: oma aloitusasema, vastustajalla 24:1 ja 15:1, eli täsmälleen
`.mat`:n ensimmäinen teko. Saman ottelun `.mat` (haettu 16.9. yöllä) vietiin `SgfExport`illä
tällä merkillä: ainoa `C[]` on pelin 5 solmussa `;B[11qrqrstst]`, eli Tommin 11: 8/7 8/7
6/5 6/5, ja `gnubg-cli`:n `load match` + `save match` antoi kaikki 557 solmua kommentteineen
samoina. Kuori tapettiin ja proxy purettiin; `Refresh` toi oikean listan (3 vuoroa).
**Testimerkki *Kuori 11* jäi laitteelle** todisteeksi, ja sen poisto on listan ×.

**Lista on otteluluettelossa nimen alla**, rivinä *N positions marked for analysis*, joka on
linkki myös nollana, jotta lista löytyy ennen ensimmäistä merkkiä. Ruutu `Marked positions`
(`MarksScreen`, reitti `MARKS_ROUTE`) ryhmittelee merkit ottelun mukaan, uusin merkki ensin,
ja nimeää ottelun ottelumuistista (*vs opponent (match 5316472)*, turnaus, kierros, pituus).
Numero on aina mukana, koska se on `.mat`-tiedoston nimi (`dg-5316472.mat`). Rivi sanoo
*Game at 1–5, move 377*, sitten sanan ja päivän. Lauta ei avaudu tästä, koska ottelu on
tyypillisesti päättynyt eikä sivustolla ole aseman osoitetta. Poisto on kuittaus, ei
vahvistusta.

**Miksi merkki ei mene `.mat`-tiedostoon, vaikka Tommi kysyi sitä ensin.** Mitattu 15.9.2026
tällä koneella: XG2 avasi `;`-alkuisilla kommenttiriveillä täydennetyn tiedoston (otsikon
perässä ja pelin sisällä) moitteetta, kaikki pelit tulivat mukaan, mutta rivit eivät näy
ohjelmassa missään. GNU Backgammonin tuonti lukee `;`-rivit vain otsikosta
(`ImportMatVariation`). Kommentti tiedostossa olisi siis näkymätön juuri siinä ohjelmassa
jossa se pitäisi lukea, ja siksi lukupaikka on sovellus. Testitiedosto oli kopio Tommin
omasta viennistä eikä sitä tallennettu XG2:ssa.

**Laiteajo 15.9.2026 klo 23.09–23.15 (SM-T970), kuoriproxylla illan sivuilla** (Top Page
`0002` ja opponentin rolled back -lauta `0033`), koska oikeaa vuoroa ei ollut auki. Luettelossa
nimen alla *No positions marked for analysis*. Laudalla `Mark position` kolmantena linkkinä
`Cube reminder`in alla, painallus avasi laudan alle kentän *Why? (optional)* ja napin *Mark
377*, ja painallus tuotti rivin *Marked for analysis: move 377, Recube after rollback* (iso
R on näppäimistön automaattinen). Luettelossa *1 position marked for analysis*, ja sen takana
*vs opponent (match 5316472)*, *March 26 Championship, Round 2/9, Length 11*, *Game at 1–5,
move 377*, *Recube after rollback · 15 Sep 2026*. Ensimmäinen ajo näytti merkkiruudussa
välilehtipalkin Matches korostettuna, koska reitti puuttui palkin ehdosta; korjattu ja
todennettu toisella asennuksella ilman palkkia. Kuori ei jättänyt laitteelle mitään
(`/bg/profile` estetty, `dg_site_settings.xml` ennallaan). **Testimerkki jäi laitteelle**
todisteeksi, ja sen poisto on listan ×.

Todentaminen: `MarkBookTest` (elinkaari oikeaa SQLiteä vasten, erityisesti ettei pelin
vaihtuminen siivoa), `DgDatabaseMigrationTest` (kahdeksas migraatio), `BoardViewModelTest`
(peli ja siirtonumero laudalta, ei lähetystä, laudan alta pois muttei kannasta),
`HelpConsistencyTest` (`d_marks` nimeää linkin ja välilehden). App Help: ryhmä B, *You can
mark a position to look at after the match.*


### Merkkilistan rivien väliin viiva (Tommin tilaus 17.9.2026)

*"Marked positions tarvitsee markit toisistaan erottavat viivat."* `MarkRow` saa alleen
`HorizontalDivider`in samaan tapaan kuin Inbox-välilehden rivit, ja rivin ympärille 4 dp
pystyväliä. Ottelun otsikko ei saa viivaa, koska sen yläpuolella on edellisen ottelun
viimeisen merkin viiva ja 12 dp väli. Todennettu laitteella neljän merkin listalla samana
yönä: viiva jokaisen merkin alla, otsikot erottuvat.

### Luolan seinä pois (Tommin päätös 17.9.2026)

*"Luolamaalauskuvia ei enää tarvita taustaksi."* `DgCave.kt` ja `DgCaveLayoutTest` poistettu,
ja `DgFill` piirtää enää taustakuvat: kun kansiota ei ole valittu tai kuvat ovat vielä
purkamatta, vapaa tila saa pelkän taustan. Tessellaatio (`DgPattern.kt`) ja sen kytkin ovat
ennallaan. Seinä eli 7.9.–17.9.2026, ensin tumman kuudentena lippuna ja sitten täytekuvana;
päätöshistoria on yllä jaksossa *Tausta ja täytekuva*, ja luonnos jäi
`tyokalut/luolaluonnos.py`:hyn. Päätös syntyi laitekuvasta jossa merkkilistan alle tuli
seinä. Kansio oli tyhjä Tommin omasta valinnasta eikä kadonnut: *"kokeilen millaista pelata
ilman taustakuvia, siksi katkaisin linkin dg-taustakuvat-alkuperaiset."* Commit ce1c7b9
väittää katoamista, ja tämä rivi korjaa sen. Kokeilu on kesken, ja sen tulos on Tommin.

### Vuororaidoitus riviluetteloihin (Tommin tilaus 17.9.2026)

*"Luetteloihin pitäisi saada eri rivit hieman eri väreillä, en tiedä termiä mutta helpottaa
luettavuutta."* Termi on zebra striping. Kaksi valintaa ennen toteutusta: kaikkiin
riviluetteloihin (ei vain otteluihin), ja raita viivojen lisäksi eikä niiden sijaan.

**Mekanismi on lukupinnassa, ei riveissä** (`DgReadingSurface.kt`). Jokainen `items`-kutsu
merkitsee rivinsä `contentType = { DgStripedRow }`, 22 kutsua seitsemässä ruudussa, ja
`dgReadingSurface(state)` piirtää `visibleItemsInfo`sta joka toiselle näin merkitylle
riville tekstivärin ohuen kerroksen pinnan päälle. Otsikot, selitteet ja muut yksittäiset
`item`-kohdat eivät saa raitaa. Parillisuus on listan indeksistä, joten peräkkäiset rivit
vuorottelevat aina ja otsikon jälkeinen ensimmäinen rivi saa kumman tahansa.

**Peittävyys 9 %** (`DgStripeAlpha`). Neljä vaihtoehtoa kuvana molemmissa teemoissa
sovelluksen omilla väreillä: tekstiväri 4 %, 6 %, 9 % ja primary-sävy 10 %. Tommi: *"C,
näkyy parhaiten."* Primary-sävy hylättiin myös siksi että se kilpailisi otteluluettelon
Grace- ja Pool-värien kanssa. Tekstiväri tummentaa vaaleassa ja vaalentaa tummassa, eikä
lukupinnan alpha muutu.

Todennettu tabletilla tummassa teemassa turnausluettelolla (60 riviä): joka toinen rivi
vaaleampi, viivat säilyvät. Vaalean teeman Tommi katsoi laitteella samana yönä: *"hyvältä
näyttää"*.

### Vuororaidoitus asetusruutuun (Tommin tilaus 17.9.2026: *"Settings-näkymälle sama raidoitus"*)

Asetusruutu on tavallinen vierivä sarake eikä laiska lista, joten `contentType`-mekanismi
ei ulotu sinne. Rinnalle tuli `Modifier.dgStripe(striped)` (`DgReadingSurface.kt`), sama
väri ja sama `DgStripeAlpha`, mutta parillisuuden laskee kutsupaikka.

**Yksikkö on ryhmä, ei rivi** (Tommin tarkennus samana yönä ensimmäisen laiteajon jälkeen:
*"haluan että ryhmien raidoitus vuorottelee, mutta ryhmän sisällä ei raidoitusta"*). Raita
kattaa otsikon ja kaikki sen rivit selitteineen, ja joka toinen ryhmä on raidalla.
Laiteosiossa ryhmä on `DeviceGroup` (yhdeksän otsikkoa Board lookista Pictures below the
listsiin), ja sen kahdeksan sisäkirjoitettua kytkintä tiivistyivät samalla
`DeviceToggle`-komposiitiksi. Sivuston lomakkeessa kytkinlista on ryhmä nolla ilman raitaa,
ja radioryhmät (`ChoiceGroup`) vuorottelevat sen perään raidalla alkaen.

Ensimmäinen versio raidoitti rivit otsikon alla (commit f216ee4) ja eli yhden laiteajon:
asetusrivit ovat eri korkuisia selitteineen, joten rivitason raita teki ruudusta levottoman
eikä auttanut löytämään ryhmää. Todennettu tabletilla vaaleassa teemassa molemmissa
osioissa.

### Katkohistoria Info-välilehdellä (Tommin tilaus 18.9.2026)

Info-välilehden rivi *Connection drops* heti *App Help*in perässä, ja sen takana lista
uusin ensin: hetki laitteen kellosta minuutin tarkkuudella, painettu nappi, ottelu
ottelumuistista (`vs <vastustaja> (match <nro>)` tai pelkkä numero) ja poikkeuksen luokan
nimi sellaisenaan (`SocketTimeoutException`). Rivi kirjoitetaan `Board not confirmed`
-kortin syntyessä eikä koskaan muulloin (`BoardViewModel.afterPress`, vain
`DgResponse.Offline`), ja se jää kun kortti ja jonon rivi poistuvat.

**Paikka on Info eikä Settings**, koska Info on lukemista ja asetukset säätämistä (Tommin
päätös 4.9.2026), ja tämä on lukulista kuten merkityt asemat: ei poistoa, ei avautuvaa
lautaa, ei verkkoa. Historia rajaa itsensä sataan (`DropLog.KEEP`), mikä riittää
kysymykseen "mikä se äskeinen oli".

**Syy näytetään luokan nimenä eikä käännetä sanaksi**, koska lukija on se joka mittaa:
`UnknownHostException` (DNS), `SocketTimeoutException` (hidas verkko) ja `ConnectException`
(yhteys hylätty) ovat juuri se ero jota tallentamattomasta sessiosta ei voinut lukea
(`raakasivut/LUEMINUT.md` › `sessio-18-9-paiva`). App Help sanoo saman `unconfirmed`- ja
`tabs`-kohdissa (`docs/OHJE.md`).

Laiteajo odottaa: lista on tyhjä kunnes ensimmäinen katko osuu, joten ruutu todennetaan
tyhjänä ja täyttyminen seuraavasta oikeasta katkosta tai kuoritestistä proxy alhaalla.

### Drive-reitti `.sgf`:lle todennettu päästä päähän (18.9.2026)

Tommi kysyi voiko sovellus tallentaa Driveen ja BGBlitz lukea sieltä. Vastaus on kyllä
ilman yhtään koodiriviä, ja se todennettiin samana päivänä. Tabletilla `Share .sgf for GNU
Backgammon` › jakovalikon *Tallenna Driveen* (Tommin teko, klo 15.07), ja tällä koneella
Drive for desktop näytti tiedoston `G:\Oma Drive\dg-5301910.sgf` (2 237 tavua,
`AP[DG Android:0.1]`, 5 pisteen ottelu, 4 peliä) ennen kuin sitä ehdittiin kysyä. BGBlitz
avaa sen `File › Analyze › GnuBG (SGF)` tavallisena tiedostona, koska `G:` on asema.

`.mat`-reitti työpöydälle todennettiin 3.9.2026 *Linkki Windowsiin* -rivillä, ja 2.9.
laiteajossa Driveä ei nähty valikon näkyvässä osassa; nyt se on nähty ja käytetty. Ero
reittien välillä on vain jakovalikon rivi, koska molemmat tiedostot kulkevat
`MatchExportShare`n kautta samalla MIME-tyypillä. App Helpin `d_marks` sanoo saman yhdellä
lauseella (`docs/OHJE.md`).

Todennettu tiedosto oli ilman `C[]`-kommenttia, eli tässä ottelussa ei ollut merkkiä. Se ei
heikennä todennusta, koska reitti ei katso sisältöä, mutta Frankin korjatun buildin mittaus
tehdään yhä `raakasivut/dg-5311448.sgf`:llä jossa kommentti on.

### Sivupaneelin napit: yksi muotokieli, hiljaiset komennot ja kehys pois (Tommin valinta 20.9.2026, D)

Tommi pyysi ideoita sivupaneelin nappien tyylikkyyteen illan pelisession jälkeen. Kuusi
ideaa, joista neljä vietiin kankaalle rinnakkain kontrastilukuineen samaan tapaan kuin
24.8.2026 (artefakti *Sivupaneelin napit*, lähtöruutu `sessio-20-9-ilta` klo 22.06,
`The Marathon #4326`, `Roll Dice` + `Double`). Kontrastit laskettiin paneelin pohjaa
`#1B1712` vasten eikä kaistaa vasten, koska napit ovat 2.9.2026 alkaen paneelissa. Tommi
valitsi neljännen sarakkeen, joka sisältää kolme edellistä: *"D yksin riittää, kirjaa uusi
toteutus kaanonimuutos"*. Neljän tason värijärjestelmä (teko, peruuttamaton teko, peruminen,
sivun komento) ja kaikki värit ovat ennallaan; muutos koskee muotoa, painoa ja signaalia.

**Yksi muotokieli.** Kortit olivat pyöristettyjä suorakaiteita ja napit Materialin
stadionpillereitä, eli paneelissa oli kaksi kieltä. Nappien pyöristys on nyt kortin tasolla
(`DgBoard.BUTTON_SHAPE`, 8 dp), ja se koskee kaikkia paneelin ja keskikaistan nappeja
(`Submit Move`, `Roll Dice`, `Double`, `Undo Move`, `Swap Dice`, `Skip Game`, `Message`,
muistutuksen `Add`). Lukuina A ja B olivat identtiset: pyöristys ei koske väriä.

**Alleviivauksen tilalle nuolenpää ja vaimea väri.** Alleviivaus oli Tommin päätös
27.8.2026: painettava ei saanut näyttää samalta kuin luettava, eikä väriä ollut vapaana.
Päätöksen tarve on ennallaan, toteutus vaihtuu: `Reminders`, `Cube reminder` ja `Mark
position` ovat nuolenpäällä (`›`) alkavia rivejä `TextMuted`-värillä (5,49:1 paneelia
vasten, AA-tasolla), vasemmalle tasattuina, ja ryhmän alla on ohut viiva (`PanelOutline`,
2,97:1, rakenne eikä luettava asia, sama kuin kortin reunus) joka erottaa sovelluksen omat
lisät ottelukortista ja sivun teoista. Signaali on kaksi asiaa alleviivauksen yhden sijaan:
nuolenpää sanoo että rivi vie johonkin, ja väri että se on toissijainen. `TextMuted` ei ole
paneelissa varattu muuhun kuin toissijaisiin riveihin, joten se ei kilpaile merkitysten
kanssa. Ahtaassa paneelissa (`FlowRow`) rivit ovat sisältönsä levyisiä eikä viivaa ole.

**Fontin paino kantaa hierarkiaa.** Kaikki napit olivat mediumia tai normaalia, ja
näytöllä molemmat lukivat lihavina. Nyt vain ensisijainen teko (`Submit Move`, `Roll Dice`,
`Next`, `Next Game`) on lihava, peruuttamaton teko medium, ja peruminen sekä sivun komennot
medium (aiemmin normaali).

**Kuutioteon tealkehys pois.** Kehys (15.9.2026) sanoi vahvistusruudun kuuluvuuden nappiin
alueena, mutta se oli laatikko laatikossa: reunus napissa ja toinen ryhmän ympärillä.
Kuutioteal on napissa itsessään (`CubeSoft`-reunus 10,53:1), ja vahvistusruutu on napin
alla samassa vasemmassa reunassa kuten 14.9.2026 ennen kehystä (*"Tasaa Verify-checkboxit
vasempaan laitaan"*). Kehyksen keskitys (15.9., *"keskitä verify korttiinsa"*) poistui
kehyksen mukana, koska se oli kortin ominaisuus. Ahtaan paneelin rivi (16.9.2026, nappi ja
ruutu rinnakkain) säilyy, kehyksettä.

**`Skip Game` ja `Message` pysyvät reunustettuina nappeina**, vaikka kankaan D-sarake
näytti `Skip Game`n samana hiljaisena rivinä kuin muistutukset. Syy on kirjattu sääntö
jota kangas ei ottanut huomioon: sivun oma teko näyttää napilta ja sovelluksen oma lisä
tekstiltä, ja ehto luetaan lähteestä eikä sijainnista (25.8.2026); `Skip Game` kuluttaa
jonon eikä sitä voi perua. Toteutus poikkeaa siis kankaasta tässä yhdessä kohdassa.
**Kuitattu 20.9.2026 kaappauksen jälkeen** (*"nappi jää, skip game on erikoisuus"*):
sääntö 25.8. pysyy, ja `Skip Game` on muistutusten joukossa hetken takia mutta napin
näköinen lähteen takia. Todennettu kuoriproxylla samana iltana (`raakasivut/
sessio-20-9-ilta/kehykset/paneelin-napit-D-kuori.png`, illan sivu 0018 ottelutunnus
sovitettuna vanhaan Top Pageen); Tommin käsikokeilu on seuraavassa pelisessiossa, ja
avoin kysymys siihen on väljän paneelin rivikorkeus (Materialin 40 dp).
**Käsikokeilu tehty kahdessa pelisessiossa** (`sessio-20-9-yo` 10 ottelua ja
`sessio-21-9-paiva` 19 ottelua), Tommin palaute 21.9.2026: *"D-paneeli on hyvä, mietin
vain Skip Game-napin sijaintia ja miltä näyttää kännykän näytössä."* Rivikorkeudesta ei
tullut huomautusta. Kaksi avointa mietintää ovat Tommin, eivät päätöksiä: `Skip Game`n
paikka (nyt paneelin yläreunassa muistutusten yläpuolella) ja paneelin ulkoasu
kännykällä, jota ei ole kaapattu D-paneelin kanssa lainkaan. (Kirjoitin tähän ensin
että kännykän napit ovat kaistalla; Tommi huomautti 21.9. että se on vanhaa tietoa, ja
oli oikeassa. Kaista on paikka silloin kun paneeli ei mahdu leveyteen (`fits`,
`SIDE_PANEL_MIN` 150 dp), ja vaakatilassa Pixel 8a:n paneeli mahtuu ja on 16.9.2026
alkaen *ahdas paneeli*. Vanhemmat *kun napit ovat kaistalla (puhelin)* -maininnat
2.–3.9.2026 kuvaavat sen ajan tilaa, jolloin puhelimen lauta oli kaistalla.)

**Kännykkä kaapattu samana päivänä ja kaksi päätöstä** (Pixel 8a kuoriproxylla,
`raakasivut/sessio-21-9-paiva/kehykset/kannykka-D-paneeli.png`). Ahdas paneeli latoi
`FlowRow`lla `Skip Game`n ja `› Reminders`in ensimmäiselle riville ja jätti `› Mark
position`in yksin toiselle. Tommin sääntö: *"jos sivupaneelin yläosassa on pariton määrä
toimintoja, niin anna Skip Game olla omalla rivillään"*, ja järjestys *"Skip game, mark
position, reminders, cube reminder"* (kysymys oli mahtuuko `Mark position` `Skip Game`n
riville; mahtuu, mitattu 4 toiminnon tilassa). Toteutus `BoardScreen`in
`buttonsOwnRow`: sivun napit (`Message`, `Skip Game`) saavat oman rivin kun niitä on
kaksi tai kun toimintojen kokonaismäärä on pariton, muuten `FlowRow` latoo napin ja
`Mark position`in yhteen. Järjestys vaihtui myös väljässä paneelissa, jotta luettelo on
yksi. Kolme tilaa kännykältä `kehykset/kannykka-skip-kolme-tilaa.png`: 3 toimintoa
(nappi yksin, kaksi linkkiä parina), 4 (nappi ja `Mark position`, `Reminders` ja `Cube
reminder`) ja 5 `Next Game` -sivulla (`Message` ja `Skip Game` parina, kaksi linkkiä
parina, `Cube reminder` yksin). Viides tila on tavallinen: korpuksen 1 080 `Next Game`
-sivusta 1 047 kantaa chat-lomakkeen, ja poikkeukset ovat arvatut asemat (18) ja sivut
joilla vastustaja oli kirjoittanut (4, silloin chat-kortti on auki eikä `Message`-nappia
ole). Tommin tarkennus samana päivänä: *"viiden toiminnon tapauksessa Reminders ja Cube
Reminder samalle riville"*, joten `Mark position` saa oman rivin nappirivin alle
(`markOwnRow`), ja kuutiomuistutus jää koska `Next Game` -sivulla vuoro on ohi mutta peli
ei (Tommin huomautus). Todennettu `kehykset/kannykka-skip-5-viisi-b.png`.

**`Message` on `Chat` 21.9.2026 alkaen, ja pari on aina samalla rivillä** (Tommin tilaus
`Kaanon/TYOJONO.md` kohta 29 ja saman päivän tarkennus *"jos se esiintyy ruudulla yhdessä
Skip Game -napin kanssa, niin haluaisin ne aina samalle riville"*). Nimi: sivustolla on
kaksi kanavaa, laudan chat joka kulkee siirron mukana (sivun oma sana *You may chat with X
here*) ja profiilin yksityisviesti joka menee Inboxiin, ja sovellus nimesi molempien napit
`Message`. Laudan nappi kantaa nyt sivun omaa sanaa (`board_chat_open`), profiilin
`Message` (`page_message_open`), kortin `Reply` ja luettelon `Message archive` jäävät. App
Helpin *Beside the board* -jakso sanoo eron yhdellä lauseella. Rivi: ahdas paneeli latoi
parin jo omalle rivilleen (`buttonsOwnRow`), mutta väljä paneeli latoo `actions()`-lohkon
`Column`iin, joten tabletilla `Message` ja `Skip Game` olivat allekkain. `buttonsPaired`
antaa parille sisältönsä levyisen `Row`n myös väljässä paneelissa ja kaistalla. Yksin
esiintyvä nappi ei muutu.

Kankaalla ei ollut ideoita 5 (`Undo Move` eri muotoon kuin kortti) ja 6 (kaksivaiheinen
vahvistus ruudun sijaan). Kortin reunus `PanelOutline` mitattiin samalla ensi kertaa
paneelia vasten: 2,97:1, alle ei-tekstisen AA-rajan; ei kuulu tähän erään.

## Käynnistyskuvake (21.9.2026)

Sovelluksella ei ollut kuvaketta lainkaan ennen 21.9.2026 (`res/`ssä ei `mipmap`-kansiota,
manifestissa ei `android:icon`), ja laite näytti järjestelmän oletusrobotin. Julkistuksen
työjono (`docs/JULKAISU.md`) nosti puutteen, ja kuvake valittiin luonnoksista: ensin kolme
lautamotiivia (`tyokalut/kuvakeluonnos.py`), Tommin suunta *"kolmiulotteinen, läpinäkyvä tai
värikäs, tuplauskuutio"*, sitten kuusi kuutiota (`tyokalut/kuutioluonnos.py`), joista
Tommi valitsi F:n tarkennuksin: *"64-tahko olkoon se androidin pää, eri tahkojen numeroilla
eri värit"* ja *"muista kulmien pyöristys"*.

**Toteutus** (`tyokalut/kuvake.py`, käsin ei muokata): isometrinen kuutio laudan kolmella
värillä (ylätahko oranssi, vasen vihreä, oikea punainen), ylätahkolla laudan kuution
robotinpää (sama 60-yksikön piirros kuin `CubeRobotFace`, projisoitu tahkolle, silmät
tahkon väriä), vasemmalla `2` kermalla ja oikealla `4` laudan mustalla. Siluetin kulmat
pyöristetty maskilla (säde 0,11 särmästä), särmät tahkojen välissä terävät luettavuuden
takia. Adaptiivinen kuvake: tausta laudan kenttä `#1A1614`
(`values/ic_launcher_background.xml`), etuala PNG viidessä tiheydessä
(`mipmap-*/ic_launcher_foreground.png`, 108 dp:n kangas, kuutio 60 dp korkea eli
turva-alueen 66 dp:n ympyrän sisällä), `mipmap-anydpi-v26/ic_launcher.xml` ja
`_round`. PNG eikä vektori, koska numerot ovat tekstiä. Kaupan 512×512 kuvake
`docs/kuvat/kauppa/ic_launcher_512.png` samasta piirrosta täydellä taustalla.

**Todennettu Pixel 8a:n sovellusvalikossa** (`raakasivut/sessio-21-9-kuori2/
pixel-sovellusvalikko.png`, lähikuva `pixel-kuvake-zoom.png`): kuutio ympyrämaskissa DG
Mobilen vieressä, numerot luettavissa 48 dp:ssä. App Helpin *Board look* -jakso sanoo että
kuvake on sama kuutio, joten Googlen robotin CC BY -nimeäminen kattaa senkin.

**Yhtenäistys laudan kuutioon samana iltana** (Tommi: *"haluaisin yhtenäistää tästä myös
pelin tuplauskuutioksi, tahkot samanvärisiä, tekstit erivärisiä"*, sitten *"pidän pelin
värien käytöstä, androidin pää väritä kuten se on cube reminder-tilassa ja 32-tahkolle
ehdota toista väriä"*, ja neljästä ehdokkaasta *"pinkki puuttui pelistä, se käy"*). Kuvakkeen
tahkot ovat nyt pelin kuution väri `Palette.Cube` kolmella valolla (1,25, 1,0 ja 0,72), pää
kermana ja silmät `CubeLaser`-purppuralla kuten muistutuksen aikana, ja väriero on
numeroissa. Asteikko `Palette.onCube(value)`: 2 kerma (`OnCube`), 4 laudan oranssi
(`WedgeEven`), 8 koralli `#FF786E`, 16 `CubeSoft`, 32 pinkki `#FF96C8`, 64 valkoinen; `DR`
ja tuntematon ALT kerma. Purppura jätettiin asteikosta pois, koska se on muistutuksen väri.
Sama asteikko piirtää laudan kuution (`CubeFace`, lauta ja pelaajakortti) ja vierivän
odotuskuution (`BusyCube`) numeron; pinta pysyy yhtenä värinä. App Helpin *Board look* sanoo
sen yhdellä lauseella. Todennettu tabletilla kuorella arvolla 32
(`raakasivut/sessio-21-9-kuori2/k2-03-kuutio32-zoom.png`).

**Muistutustilan tarkennus** (Tommi 21.9.2026: *"cube reminder-tilassa tahkon rajat jäävät
purppuraiseksi"* ja *"laserin sammuttua jäljelle jää vain purppurainen kehys ja itse android
on pelkkää kermaa, siksi 2"*). Kolmesta tulkinnasta valittiin toinen: kuvakkeen pää on kerma
ja silmät reikiä eli tahkon väriä, ja muistutustilan väri on purppura kehys. ~~Kehys kulki ensin siluetin ympäri~~
(`pixel-kuvake-zoom-2.png`), ja Tommi hylkäsi sen: *"ei hyvä, purppurat rajat vain androidin
päätä ympäröivälle tahkolle"*. Kehys (`CubeLaser`, leveys 0,075 särmästä) kulkee nyt vain
ylätahkon reunoja, koska laudalla se on ykköspinnan reunus eikä koko esineen. Kuvake on siis
kuutio siinä tilassa johon laudan muistutus sen jättää laserin sammuttua. Todennettu Pixelin
sovellusvalikossa (`raakasivut/sessio-21-9-kuori2/pixel-kuvake-zoom-3.png`).

### Loungen Players ja Tournament Hall listan päähän (Tommin päätös 21.9.2026)

Tommin havainto: *"Lounge-näkymässä Players ja Tournament Hall ovat pysty-näytölläkin aina
aluksi näkymättömissä olevia linkkejä kun taas ylemmät listat latautuu."* Linkit olivat
4.9.2026 alkaen listan perässä, ja tarjoukset ja ilmoittautumiset ovat pitkiä listoja, joten
kumpikaan linkki ei näy ennen vieritystä eikä niitä löydä tietämättä että ne ovat siellä.

**Linkit ovat nyt yhtenä rivinä listojen yläpuolella**, kaksi tekstipainiketta vierekkäin
(`Players`, `Tournament Hall`). Neljästä vaihtoehdosta (rivi ylhäällä, otsikkorivin
toiminnot, selitteelliset rivit listan alkuun, kiinteä alarivi) Tommi valitsi ensimmäisen
perusteella *"löydettävyys ja ne ovat pitkiä listoja kun taas automaattisesti näkyvät ei"*:
tarjoukset ja ilmoittautumiset näkyvät joka tapauksessa, koska ne ovat ruudun sisältö, ja
linkit ovat se osa joka tarvitsee paikan jonka löytää.

**Tämä ei palauta segmenttiriviä**, jonka 4.9.2026 päätös poisti. Segmenttirivissä yksi
kohde on aina valittuna ja rivi kertoo missä ollaan; tämä rivi on navigointia kahteen
sivuston kokonaislistaan, eikä mikään ole valittuna. Muoto on sama kuin pelaajalistan oma
*Sort By Rating · Next 100* -rivi (`FlowRow` ja `TextButton`it), joka on ollut listan
yläpuolella 27.8.2026 alkaen. 4.9. peruste (kaksi kohdetta ei ansaitse riviä yläreunassa)
väistyy löydettävyyden tieltä, ja hinta on yksi rivi tarjouksilta.

**Selitteet jäivät pois.** `LinkRow`-muoto (nimi ja selite alla) olisi vienyt kaksi riviä,
ja se on täsmälleen se mitä 4.9. päätös vältti. Kohdesivun otsikko ja pelaajalistan oma
huomautus (*Players on DailyGammon…*, 25.9.2026 alkaen) kertovat saman kun sinne on menty. Merkkijono
`lounge_players_summary` poistui käyttämättömänä; `own_tournaments_hall_summary` säilyy,
koska otteluluettelon `OwnTournamentsSection` käyttää sitä yhä.

**Tournament Hall on rivillä vain kun lounge antaa polun**, sama ehto kuin ennen: polku
luetaan loungen omasta linkistä (`LoungePage.tournamentHallPath`), ja ilman sitä nappia
ei piirretä harmaana vaan se puuttuu. `Players` on aina, koska sen polku tulee samasta
sivusta ja puuttuvan polun syy sanotaan porautumisen omassa ruudussa (`NoPath`).

**Todennettu Pixel 8a:lla 21.9.2026** pystyssä ja vaakana (`raakasivut/sessio-21-9-lounge/
l-02-lounge-pysty.png`, `l-01-lounge-vaaka.png`): rivi *Players · Tournament Hall* on
otsikkorivin alla ennen *Matches waiting for opponents* -otsaketta, ja tarjoukset alkavat
sen jälkeen ilman vieritystä.

**Tournament Hall porautuu paikallaan samana iltana** (Tommin havainto ensimmäisen
laiteajon jälkeen: *"Kun klikkaan Players, niin välilehdistä huomaan korostetun
Lounge-välilehteä mille tämä näkymä kuuluu. Kun klikkaan Tournament Hall, niin välilehdet
häviää. Ylin parempi."*). Halli avautui porautumisreitille (`PageRoute`), jolla palkkia ei
piirretä samoin kuin laudalla ja profiililla. Nyt `MainActivity`n lounge-reitti pitää
`hallPath`-valintaa `rememberSaveable`issa ja piirtää `PageScreen`in loungen tilalle samaan
tapaan kuin Info-välilehti DG Helpin ja Linksin kohdalla; `BackHandler` sulkee ensin hallin.
Hallin rivit (turnaussivut) menevät yhä porautumisreitille, kuten Infon sivujen linkit.
Todennettu Pixel 8a:lla (`raakasivut/sessio-21-9-lounge/l-04-halli.png`): palkki näkyy ja
Lounge on korostettuna hallin yllä.

### Inboxin selityskappale kiinni ensimmäisen kerran jälkeen (Tommin tilaus 21.9.2026)

*"Take an item from Dailygammon-selityskappale on raskas, pienennä se oletuksena
ensimmäisen näytön jälkeen."* Kappale (`messages_queue_explain`, kolme kappaletta) on
auki kokonaisena ensimmäisellä Inboxin avauksella ja sen jälkeen oletuksena kiinni otsikon
takana. Otsikkorivi on kytkin, ja sen perässä lukee *Why?* kiinni ollessa ja *Hide* auki
ollessa, koska pelkkä otsikko ei kertoisi että sen takana on jotain.

**Kerran nähty on laitteen oma tieto** (`QueueExplainStore`, `SharedPrefsQueueExplain`,
samaa lajia kuin noppatyyli): luetaan kerran ruudun avauksessa ja merkitään nähdyksi
samalla, jotta merkintä ei sulje kappaletta kesken saman katselun. Tila on `remember`issä
eikä `rememberSaveable`issa: ensimmäinen laiteajo palautti avatun tilan välilehden vaihdon
yli, ja toinen käynti on juuri se jolloin kappaleen pitää olla kiinni. Kortin muut tekstit
(jonon lopputulos, *Nothing is waiting*) eivät ole kappaleen takana, koska ne ovat sen
hetken tietoa eikä selitystä. App Helpin *Why do I have to press Take an item?* sanoo
taittumisen yhdellä lauseella. Todennettu Pixel 8a:lla (`raakasivut/sessio-21-9-lounge/
l-09-vertailu.png`: kiinni toisella avauksella, *Why?* avaa).


### Palstan rungon linkit avautuvat (Tommin päätös 21.9.2026)

Tommin havainto puhelimella: *"Kännykässä auki discussion-postaus, missä linkki ei toimi,
desktop selain-versiossa toimii."* Ketju oli *DailyGammon 8x8 Tournament 2026 - Kickoff*,
ja linkki `http://tiny.cc/dg_8x8` oli rungossa pelkkänä tekstinä. Se ei ollut vika vaan
kirjattu rajaus: `ForumPost.body` litisti linkit näkyväksi tekstikseen, koska palsta oli
lukutilassa (26.8.2026), ja sama sääntö kirjattiin profiilin kentille (*Real Name* täynnä
YouTube-linkkejä). Tommi kumosi rajauksen palstan osalta: *"tämä taisi olla relikti
vaatimattomammalta ajalta jolloin en ymmärtänyt mihin pystymme."*

**Kaanoni.** Postauksen rungon linkit ovat napautettavia. Ulkoinen osoite (`http`, `https`)
avaa laitteen selaimen samalla mekanismilla kuin Infon Links-rivit (`LocalUriHandler`), ja
sivuston oma polku (`/bg/...`) avautuu sovelluksessa porautumisena kuten muutkin sivun
linkit. Muunlainen href (suhteellinen, `mailto`) jää tekstiksi, koska sitä ei ole mitattu.
Linkin teksti näkyy sivun omana tekstinä primary-värillä ja alleviivattuna, jotta
napautettavuus näkyy ilman että sitä tarvitsee arvata. Profiilin kentät pysyvät tekstinä:
ne ovat pelaajan kirjoittamia vapaita kenttiä, eikä tämä päätös koske niitä.

**Toteutus.** `ForumPost` saa rungon rinnalle `links`-listan (`ForumBodyLink`: `href` ja
merkkiväli rungossa), jonka `ForumParser.paragraphText` kerää kävellessään; runko pysyy
samana merkkijonona kuin ennen, joten arkisto ja testit eivät muutu. `<p>`-elementin
lapset kävellään nyt samalla tavalla kuin muutkin solmut, jotta kappaleen sisäinen linkki
tulee mukaan. `DiscussionScreen.PostView` piirtää rungon `AnnotatedString`inä
`LinkAnnotation`eilla. Fixture on `forum_read.html`, jonka viimeinen kappale on linkki.

**Todennettu Pixel 8a:lla 21.9.2026** (`raakasivut/sessio-21-9-lounge/d-03-ketju-zoom.png`,
`d-04-selain-zoom.png`): *Kickoff*-ketjun `http://tiny.cc/dg_8x8` on alleviivattu
primary-värillä, ja napautus avasi Braven (osoite ohjautui `dg8x8.gergelyelias.com`iin).
App Helpin *Can I edit a forum post afterwards?* sanoo linkkien avautumisen yhdellä
lauseella; `PageScreen.SiteLinksContent`in kommentti sanoi vielä että palstan viestien
osoitteita ei avata, ja se korjattiin samassa commitissa.

### Add a New Thread on nappi (Tommin tilaus 21.9.2026)

*"Discussion-sivuilla Add a New Thread ansaitsee mielestäni napin ulkoasun."* Rivi oli
`LinkRow` samassa muodossa kuin *Old Threads*. Nyt se on `Button` samalla paikalla listan
perässä, samaa muotoa kuin Join, Sign up ja Take an item: ne kaikki ovat tekoja jotka
kirjoittavat sivustolle, ja Old Threads pysyy linkkirivinä koska se vain avaa listan.
Todennettu Pixel 8a:lla (`raakasivut/sessio-21-9-lounge/d-05-nappi-zoom.png`).

**Add a Comment samoin, ja Old Threads napin viereen** (Tommin kuittaus samana iltana:
*"tee Add a Commentista myös nappi ja Old Threads-linkki samalle riville napin kanssa"*).
Ketjun lopun *Add a Comment* on nyt `Button` samasta syystä kuin uusi ketju, ja indeksin
perässä on yksi rivi: *Add a New Thread* nappina ja *Old Threads* `TextButton`ina sen
vieressä. Kumpikin on rivillä vain jos sivu tarjoaa linkin. `DiscussionScreen`in oma
`LinkRow` poistui käyttämättömänä. Todennettu Pixel 8a:lla (`d-08-napit.png` indeksin rivi,
`d-07-ketju-loppu-zoom.png` ketjun loppu).

### Ottelun päätösruutu väistää loven (Tommin havainto pelisessiossa 21.9.2026)

*"Kameran lovi vie N kirjaimen Next Game linkistä, eka kerta kun lovi vie tärkeää."*
Lautareitti väistää 16.9.2026 alkaen vain palkit eikä lovea, ja se päätös koski lautaa,
jossa reikä osuu ottelukortin reunaviivaan. Ottelun päätösruutu (`MatchOverContent`) on
samalla reitillä mutta se on tekstiä, ja Pixel 8a:n vaakatilassa reikä oli täsmälleen
`Next Game`n kohdalla (`raakasivut/sessio-21-9-ilta/lovi-next-game.png`, sisältö alkoi
60 px:stä). Ruutu väistää nyt `WindowInsets.displayCutout`in; lauta ei muutu. Todennettu
kuorella (`raakasivut/sessio-21-9-lovi/k-02-paatos-zoom.png`): sisältö alkaa 164 px:stä.
Kuoren pystylukko ja `Loading`-tila eivät kuulu tähän, koska niissä ei ole loven kohdalla
mitään luettavaa.

### Päätösruudun pisterivi avaa profiilin (Tommin havainto 21.9.2026)

*"Olisin halunnut laittaa Ainotille viestiä ottelun päätyttyä, miksi piti toimia
kiertoteitse?"* Sivun päätösruudussa nimet ovat linkkejä (`<a href=/bg/user/225>`), mutta
`MatchOverParser.scores` luki linkistä vain numeron ja ruutu piirsi nimet tekstinä, joten
ainoa reitti profiilille oli Lounge → Players → haku. Nyt `PlayerRef.profilePath` luetaan
myös tästä sivusta, ja pisterivi on napautettava kun polku on: aksenttiväri ja alleviivaus
vain silloin, sama lupaus kuin muualla. Profiililta `Message` on se pikaviesti jonka
ottelun chat ei enää tarjoa, koska ottelu on ohi. Todennettu kuorella
`sessio-21-9-lovi` (`--user` illan sivu `0006`): `k-03-paatos-linkit-zoom.png` rivit
alleviivattuina, napautus haki `/bg/user/225` (kuoren loki `KAYTTAJA`) ja
`k-04-profiili-zoom.png` näyttää ainotin profiilin `Message`-nappeineen.


### Loungen otsikot taittavat listansa ja kantavat rivimäärän (Tommin tilaus 21.9.2026)

*"Matches waiting for opponents ja Tournaments sign-up eivät ole linkkejä, mutta niiden
pitäisi olla sellaisia, joita klikkaamalla saa minimoitua tai maksimoitua niiden listan ja
dg android muistaa sen ja avoimien määrä pitäisi olla luettavissa otsikko-linkissä."*
Kumpikin otsikko on nyt koko riviltään napautettava (`FoldHeading`), ja otsikkoteksti
kantaa listan rivimäärän sulkeissa (*Matches waiting for opponents (3)*), jotta määrä on
luettavissa silloinkin kun lista on kiinni. Luku on listan rivimäärä sellaisenaan: Sign-up-
listassa se sisältää myös ne turnaukset joihin on jo ilmoittauduttu, koska nekin ovat
sivun listalla.

**Merkki otsikon edessä on tila** (Tommin tarkennus samana iltana: *"sen rivin kaikille
linkeille vois laittaa sen +- riippuen tilasta"*): `+` kun lista on kiinni ja `−` kun se on
auki. Miinus on U+2212 eikä yhdysmerkki, jotta se on plussan levyinen eikä rivi hyppää
vaihdossa. Kiinni ollessa myös tyhjän listan huomautus (*No matches are waiting*) on
otsikon takana, koska luku `(0)` sanoo saman.

**Taitto on laitteen oma tieto** (`LoungeFoldStore`, `SharedPrefsLoungeFold`, samaa lajia
kuin `QueueExplainStore`): tila asuu `LoungeViewModel.folds`issa ja kirjoitetaan
jokaisesta vaihdosta, joten se säilyy välilehtien vaihdon ja uudelleenkäynnistyksen yli.
Oletus on auki, jolloin tuore asennus näyttää ruudun entisellään. App Helpin *What
happens when I press Join or Sign up?* sanoo taittumisen ja merkit.
Todennettu Pixel 8a:lla (`raakasivut/sessio-21-9-taitto/`): `t-01-lounge-auki.png` otsikot
luvuin (2 ja 16) ja miinuksin, `t-03-paluu-zoom.png` Matches kiinni plussalla välilehden
vaihdon jälkeen, `t-04-uudelleenkaynnistys-zoom.png` sama tila kylmäkäynnistyksen jälkeen.


### Players ja Tournament Hall jaksoiksi, luvut muistiin (Tommin tarkennus 21.9.2026 illalla)

*"+- otsikon edessä ja lisää sama linkeille Players ja Tournament Hall, jotka voisi
rivittää omille riveilleen. Ylläpidä Players tai Tournament Hall lukumäärää ettei sitä
aina haeta, koska se tieto ei ole ensi[sijaista] mutta silti kiinnostava."* Lounge on nyt
neljä taittuvaa jaksoa yhdessä listassa: Players, Tournament Hall, Matches waiting for
opponents ja Tournament sign-up. Porautuminen omaan näkymään poistui: pelaajalista
(hakukenttä, Sort By -linkit, sata riviä) ja halli (aktiiviset, päättyneet, huomautus)
avautuvat otsikon alle `LazyListScope`-laajennuksina (`playersItems`, `hallItems`).
`MainActivity`n `hallPath`-`PageScreen`-lohko poistui; `PageScreen`in oma hallisisältö
jäi, koska `/bg/thall` voi yhä tulla porautumispolkuna muualta.

**Avaus hakee kerran, sulku ei pyyhi.** Players ja Tournament Hall ovat oletuksena kiinni,
koska avaus hakee sivun; kiinni muistettu jakso ei hae mitään. Auki muistettu jakso
haetaan kylmäkäynnistyksessä vasta kun lounge on antanut polun (`fetch` jatkaa siihen),
muuten otsikon alla olisi "ei polkua" vaikka polku juuri saapui. Refresh hakee loungen ja
sen lisäksi auki olevat haetut jaksot; kiinni olevaa ei haeta.

**Luvut otsikoissa.** Tournament Hall kantaa viime haun aktiivisten määrän
(`LoungeFoldStore.hallCount`), joten se on luettavissa ilman kolmen tuhannen rivin sivua.
**Players ei kanna lukua 22.9.2026 alkaen (Tommin päätös).** Edellisen illan muoto oli
suurin nähty sijoitus ja sen perässä *Why?*-dialogi, koska sivusto ei kerro pelaajien
kokonaismäärää missään: pelaajalista on sadan rivin sivuja ilman summaa, eikä loungella tai
profiililla ole lukua (tarkistettu kaappauksista). Luku ja dialogi poistettiin kokonaan, ja
otsikko on pelkkä `Players`. Mukana lähti koko kertymä (`playersSeen`, `LoungeFoldStore`in
`players_seen`, `notePlayersSeen`), koska luku oli sen ainoa lukija; halli kantaa lukunsa
ennallaan. Edeltävä keskustelu on git-historiassa (`5d7785e`-sukupolvi): *"fog of unknown"*
ja *"ehkä Players lukumäärä voisi olla se mitä orgaanisesti on löytynyt, ei todellinen"*.

Todennettu Pixel 8a:lla (`raakasivut/sessio-21-9-taitto/`): `t-05-nelja-jaksoa.png` neljä
otsikkoa, Players ja Hall `(?)`; `t-06-07-vertailu.png` halli auki `(2841)` ja Players
auki hakukenttineen; `t-08-kylma-zoom.png` auki muistettu Players haettuna
kylmäkäynnistyksen jälkeen; `t-09-kylma-kiinni-zoom.png` hallin `(2841)` ilman hakua;
`t-12-players-luku.png` `Players (100)` ja *Why?*; `t-13-why-zoom.png` dialogi;
`t-14-players-200-kiinni-zoom.png` `(200)` Next 100:n jälkeen ja kiinni. Kolme viimeistä
kuvaa näyttävät 21.9. muodon, joka ei ole enää voimassa (yllä).


### Profiilin *Matches versus you* kantaa keskinäisen saldon (Tommin tilaus 21.9.2026 illalla)

*"Matches versus you voisi näyttää tilastotiedon voitoista ja linkin pelattuihin
otteluihin."* Toisen pelaajan profiilin linkkirivi on nyt *Matches versus you: 2 won,
1 lost* ja pysyy linkkinä samoihin otteluihin. Saldo luetaan sivuston omasta versus-sivusta
(`?sort_win_loss=1&finished=1&active=1&versus=<oma id>`), jossa päättyneet ovat yhdessä
taulukossa `won`- ja `lost`-jaksoina **profiilin omistajan silmin**; `ProfilePage.finishedRecord`
(`WinLoss`) lukee jaon ja `PageViewModel.versus` kääntää sen katsojan näkökulmaan
(`flipped`). Fixture `profile_page_versus.html` kaappauksesta `sessio-21-9-ilta/0007`.

**Kaanoni tarkentui ensin.** Ehdotus osui `SUBSTANSSI.md` kohtaan 75 (*vastustajan
tilastoja tai historiaa vastaan pelattaessa ei rakenneta*) ja kysyin korjataanko koodi vai
kaanoni. Tommi valitsi tarkennuksen: sivuston oma keskinäinen saldo profiililla on
sallittu, laudalle ei tule mitään. Kirjattu kohtaan 75 ennen koodia. Muoto ja haku olivat
samassa kysymyksessä: katsojan näkökulma, ja haku heti profiilin mukana (yksi lisähaku
joka ei kuluta jonoa). Hakuvirhe jättää rivin pelkäksi linkiksi, ei näytä *0 won*.

Todennettu Pixel 8a:lla (`raakasivut/sessio-21-9-taitto/`): `v-02-profiili-zoom.png`
ainotin profiililla *Matches versus you: 2 won, 1 lost* (sivu sanoo ainot won 1, lost 2),
`v-03-versus-lista-zoom.png` rivin napautus avaa samat kolme päättynyttä ottelua.

### Inbox vastustajittain: pelaajalista etusivuna (Tommin valinta B 24.9.2026)

Kolmesta luonnoksesta valittiin B (`docs/AVOIMET.md`), ja toteutuksen avoimet kohdat
ratkesivat samana iltana kolmella valinnalla:

- **Etusivu on pelaajalista, uusin ensin** (*"uusin ensin, mutta group by pelaaja"*). Rivi
  kertoo nimen tunnusvärillä, viestimäärän, uusimman päivän ja sen ensimmäisen rivin; oma
  viesti alkaa `You:`. Koko arkiston yhteistä aikajanaa (*All players*) ei ole enää, ja
  alasvetovalikko poistui. Ryhmän avain on `Message.conversationKey` (kumppani tai
  lähettäjä), sama arvo jonka suodatin säilöi, joten muistettu valinta pysyi voimassa.
- **Juuri haettu viesti vastataan listalla** (*"2 jos siihen voi vastata helposti"*, tarkennus
  *"vastaus listalla"*). Keskustelu nousee kärkeen `New`-merkillä, viesti näkyy kokonaan ja
  rivillä on `Reply`. Haku avaa vastauskentän itsestään kuten ennenkin
  (`MessagesViewModel.replyTarget`), joten kenttä on valmiiksi auki rivin alla. Merkki on
  muistissa (`MessagesViewModel.fresh`) ja poistuu keskustelun avauksesta tai lähetetystä
  vastauksesta. Toisen pelaajan viesti sulkee auki olevan keskustelun, jotta rivi näkyy;
  saman pelaajan viesti tulee auki olevaan keskusteluun ilman merkkiä.
- **Avattu keskustelu muistetaan** (*"muistetaan"*): `MessageFilterStore` ennallaan. Keskustelun
  yläreunassa on `‹ All players`, ja Back palaa listaan eikä pois Inboxista.

Keskustelun sisällä kaikki on ennallaan: päiväotsikot, kuplat ja ketjut. Testit
`MessagesViewModelTest` › *Pelaajalista ja New-merkki*.

**Ensimmäisen laiteajon jälkeen 24.9.2026 (Tommi):** *"päivämäärälle aina sama x-koordinaatti
mistä alkaa, lihavoi vain lukematon tai uusin viesti"*. Määrä ja päiväys ovat nyt kiinteän
levyisissä sarakkeissa (`LIST_COUNT_WIDTH`, `LIST_DATE_WIDTH`). Ensimmäisessä versiossa
nimen ja välitilan kaksi painoa jakoivat vapaan tilan puoliksi, ja päiväys kellui nimen
pituuden mukaan. Lihavoitu on New-rivi, ja kun sellaista ei ole, ylin rivi (Tommin valinta).
Muiden rivien nimet ovat tavallisella painolla. Todennettu Pixel 8a:lla kuvakaappauksesta.

Inboxien yhdistäminen ajettiin samana iltana ensimmäisen kerran (`tyokalut/inboxit.py`):
Pixel sai 52 viestiä ja tabletti 13, kuten skripti ennusti, ja kummassakin on nyt 65. Laiteajo odotti proxytonta
pelisessiota (Tommin järjestys 24.9.2026: *"tehdään niitä ensin ja todennetaan myöhemmin"*).

**Laiteajo SM-T970:llä 24.9.2026 klo 23.30, nauha ja kaappaukset `raakasivut/sessio-24-9-ilta2`.**
Pelaajalista näkyy tabletilla, ja päiväyssarake on samassa x-kohdassa joka rivillä. Back
keskustelusta palaa listaan eikä pois Inboxista, samoin `‹ All players`. Avattu keskustelu
(Zeppo) säilyi Top Pagelle ja takaisin vaihdettaessa. **Todentamatta on New-rivi ja vastaus
listalla**, koska merkki syntyy vain haetusta viestistä, ja haku kuluttaa oikean viestin
lukuhetkellä. Se todennetaan kun viesti tulee jonoon itsestään.

**Lihavointisääntö ei näy tabletilla, ja syy on laitteen asetus.** Tabletissa on
järjestelmän lihavointi päällä (`settings get secure font_weight_adjustment` = 300, Pixelissä
`null`). Se lisää 300 jokaiseen painoon, joten `FontWeight.Normal` (400) piirtyy 700:na eli
lihavana, eikä `Bold` (700 → 1000) erotu siitä. Kaappauksessa jokaisen rivin viestiteksti on
lihava. Nimet ovat ohuita myös ylimmällä rivillä, vaikka Pixelillä ylin nimi oli lihava.
Syytä ei ole mitattu (todennäköinen ehdokas on nimikirjasimen painovalikoima, kun paino
nousee 1000:een). Koodi tekee säännön oikein, mutta sääntö nojaa painoon, ja paino on juuri
se jonka käyttäjän asetus voi siirtää.

**Korostus värillä (Tommin valinta 24.9.2026, *"2, korostus värillä"*, sävyksi kulta).**
Korostetun rivin viestiteksti on nyt teeman aksenttivärillä (`colorScheme.primary`), ja
lihavointi jää rinnalle. Muiden rivien viestit pysyvät `onSurfaceVariant`illa. New-rivin koko
viesti saa saman värin. Nimi pysyy pelaajan tunnusvärillä. Todennettu kaappauksin samana
iltana: SM-T970:llä (Deco, järjestelmälihavointi päällä) ylimmän rivin *testi viesti 2* on
kultainen ja muut khakeja. Pixel 8a:lla (Plain) sama rivi on teeman liilalla ja lihavoituna, ja
muut ovat harmaita normaalipainolla. App Helpin `help_a_archive` sanoo nyt *highlighted* eikä
*in bold*.

**Raita väliviivan sijaan ja viestimäärä harvinaisuusväreinä (Tommi 24.9.2026 myöhään
illalla).** Kaksi tilausta peräkkäin: *"kokeile taustaväriä väliviivan sijaan"* (valinta:
vuorottelu kuten otteluluettelossa) ja *"viestien määrä paremmalle paikalle tai dnd-väreillä
ilmaistuna"* (valinta: luku paikallaan, väri roolipelien esineharvinaisuuden mukaan).

- **Raita on kokeilu, ja se poikkeaa 17.9. linjasta** (raita viivojen *lisäksi*, `DgReadingSurface.kt`)
  vain tässä luettelossa. Raita on `dgStripe` palstan levyisenä eikä lukupinnan
  `DgStripedRow`, koska Inboxin rivit ovat keskitetyssä palstassa (`inboxColumn`).
- **Portaat** (`inboxCountTier`, rajat Clauden ehdotus): 1 harmaa, 2–4 vihreä, 5–9 sininen,
  10–19 violetti ja 20+ oranssi. Tumma teema käyttää tavallisia sävyjä ja vaalea
  tummennettuja. Testi `InboxCountTierTest`, App Help `help_a_archive` mainitsee värit.
- Todennettu kaappauksin samana iltana: tabletilla (Deco) *33* on oranssi, *5* sininen ja
  *2*–*3* vihreitä, ja raita on palstan levyinen. Pixelillä (Plain) sama.
- **Sarakeotsikot `From whom` ja `When last`** (Tommi 25.9.2026 yöllä, *"lyhennetty ok"*,
  mutta täysi muoto mahtuu). Otsikkorivi (`ConversationListHeader`) käyttää samoja
  sarakeleveyksiä kuin rivi, ja määräsarakkeen kohta on tyhjä, koska sille ei tilattu
  otsikkoa. Todennettu kaappauksin tabletilla ja Pixelillä: *When last* alkaa päiväysten
  kohdalta.
- **`With whom` ja `Count`** (Tommi 27.9.2026). Keskustelu kulkee kumpaankin suuntaan, joten
  *From whom* vaihtui. Määräsarake sai otsikon `Count` neljästä vaihtoehdosta (`#`, `Msgs`,
  `Messages`, `Count`), ja sarake levisi 28 dp:stä 40 dp:hen jotta otsikko mahtuu.

### Pelaajakortin nimi ratingin mukaan (Tommin tilaus 24.9.2026, rajat 25.9.2026)

Tilaus: *"pelaajakortin nimen väri harvinaisuusväriksi rating-perusteisesti"*. Laudan sivulla
ei ole ratingia, joten kaksi valintaa tehtiin ennen koodia.

- **Lähde on vain jo nähty rating** (Tommin valinta). `PlayerRatingStore` kirjaa käyttäjänumeron
  ja ratingin sivuilta jotka haetaan joka tapauksessa: toisen pelaajan profiili
  (`PageViewModel`), oma profiili otteluluettelon Refreshistä (`OwnTournamentsViewModel`) ja
  loungen pelaajalista (`LoungeViewModel`). Yhtään pyyntöä ei tehdä ratingin takia, joten
  ylläpidolle annettu *"only when the person using it asks for something"* pitää. Tuntematon
  pelaaja saa kortin tavallisen tekstivärin.
- **Rajat mitattiin ensin talletetuista sivuista** (Tommin valinta *"mitataan ensin"*). Otos oli
  kärkipainotteinen: 108 rankinglistan pelaajaa (top-100 alkoi noin 2143:sta, kärki 2328) ja
  seitsemän vastustajaprofiilia (1820–2328). Viidenneksiä ei voinut laskea, joten portaat
  sidottiin ankkureihin, ja Tommi hyväksyi ne: alle 1600 harmaa, 1600 vihreä, 1800 sininen,
  1950 violetti ja 2140 oranssi eli top-100:n taso (`ratingTier`, testi `RatingTierTest`).
- Värit ovat samat kuin Inboxin viestimäärässä (`Rarity.kt`), tumman pinnan sävyt, koska
  kortin tekstiväri `Palette.TextPrimary` on vaalea kaikissa lautatyyleissä. Sama väri on sekä
  kortissa että kapeassa nauhassa.

**Todennettu SM-T970:llä 25.9.2026 klo 0.09 ilman laudan avausta.** Oman profiilin avaus kirjasi
`20311 = 2216.43`, ja loungen Players-jakson avaus nosti varaston sataan riviin
(`run-as fi.tommi.dg cat shared_prefs/dg_player_ratings.xml`). **Kortin väriä ei ole vielä nähty
laitteella**, koska laudan avaus kuluttaisi vastustajan mahdollisen viestin. Se luetaan
seuraavan pelisession nauhalta. App Help `help_a_beside` kertoo portaat ja sen ettei ratingia
haeta erikseen.

**Poikkeus 25.9.2026:** omien turnausten Refresh hakee tuntemattoman vastustajan profiilin,
ks. *Turnausrivin odotus ja vastustajan väri*.

### Turnausrivin odotus ja vastustajan väri (Tommin tilaus 25.9.2026)

Tilaus: *"hape42 mainitsi ettei jordania kiinnosta vastata, siksi haluaisin että turnaukset
haetaan ja väritetään"*, tarkennus *"pelaajanimi suhteuta rating"*. Peruste on se, että
ylläpidon hiljaisuus on fakta eikä odotus (`docs/YHTEYDENOTTO.md`, 19.9.2026), joten kuormasta
päätetään itse. Neljä valintaa tehtiin ennen koodia:

- **Haku lähtee Tournaments-segmentin Refreshistä**, ei avauksesta eikä riviltä. Refresh hakee
  entisten kahden sivun (lista ja oma profiili) jälkeen jokaisen linkittömän rivin turnaussivun
  ja sitten niiden vastustajien profiilit, joiden ratingia ei ole nähty. Kaikki peräkkäin
  `DgClient`in minimivälillä, eikä mikään kuluta jonoa. Painallus on käyttäjän pyyntö, joten
  ylläpidolle annettu *"only when the person using it asks for something"* pitää edelleen.
- **Tuntemattoman vastustajan profiili haetaan** (Tommin valinta). Tämä kumoaa 24.9. säännön
  *"yhtään pyyntöä ei tehdä ratingin takia"* tältä osin: Refresh hakee profiilin vain
  linkillisen rivin vastustajalle, ja vain kerran, koska nähty rating jää varastoon. Laudan
  kortti ja muut ruudut noudattavat yhä vanhaa sääntöä.
- **Linkitön rivi kertoo odotuksen pituuden eikä nimiä:** `2 wins · waiting for 1 match`.
  Luku on vastahaaran ratkeamattomat ottelut, eli kesken olevat ja aloittamattomat, jotka on
  pelattava ennen kuin vastustaja tiedetään (mittaus `docs/KOHDE.md` › *Turnaussivu*).
  Luku säilyy laitteella turnausnumeron mukaan seuraavaan Refreshiin, ja rivi ilman lukua on
  ennallaan.
- **Linkillisen rivin vastustajan nimi väritetään** samoilla harvinaisuusportailla kuin
  pelaajakortti (`ratingTier`), kun rating tunnetaan.

Laskennan ensimmäinen versio vaati etenemiseen lihavoinnin ja antoi kahdelle ison turnauksen
riville "ei odota", koska vapaakierros siirtää pelaajan eteenpäin lihavoimattomana. Korjattu
ennen asennusta, ja tulos tarkistettiin kaikkia 28 talletettua sivua vasten
(`EventWaitTest`, `OwnTournamentsViewModelTest`).

**Todennettu SM-T970:llä 25.9.2026 klo 0.44.** Refresh kesti noin puolitoista minuuttia, ja
sen jälkeen linkittömät rivit sanoivat odotuksen (#4285 `waiting for 1 match`, December 25
Championship `waiting for 2 matches`, samat luvut kuin mittauksessa) ja linkillisten rivien
vastustajat olivat sinisiä ja violetteja. Asennettu myös Pixel 8a:han. Heti asennuksen jälkeen
Tommi havaitsi pahan lagin, ks. `docs/AVOIMET.md`.

### Turnauskaavion nimet ratingin mukaan (Tommin tilaus 26.9.2026)

Tilaus: *"olisi upeaa jos värit olisivat myös turnauskaavioissa"*. Kaavion pelaajasolu saa
saman harvinaisuusvärin kuin pelaajakortti (`ratingTier`), kun rating on nähty.
Käyttäjänumero luetaan solun profiilipolusta (`EventEntry.userId`). Myöhempien kierrosten
voittajasolu on ottelulinkki ilman numeroa, joten se saa numeron saman kaavion samannimisestä
pelaajasolusta, ja väri seuraa pelaajaa koko reitin (laitteella nähty puute 26.9.2026). Kaaviosivulla ei ole
ratingeja, eikä niitä haeta sen takia, joten tuntematon pelaaja jää oletusväriin.

**Oma nimi: rating ja reunus** (Tommin valinta kolmesta). Ennen omaa nimeä erotti tekstin
korostusväri (3.9.2026), ja ratingväri olisi vienyt saman keinon. Nyt oma solu saa 2 dp:n
korostusvärisen reunuksen, ja nimi värittyy ratingin mukaan kuten muutkin. Jos omaa ratingia
ei ole nähty, nimi pysyy korostusvärissä. Hylätyt vaihtoehdot olivat oma tekstiväri
ennallaan ja ratingväri ilman erillistä korostusta.

Värit tulevat teeman mukaan (`dgDark()`), koska kaavio on sivuruutu eikä lauta. App Help
`help_a_beside` ja kaavion alainen huomautus `page_bracket_note` kertovat sen.

### Rarity-värit kaikkiin pelaajanimiin ja kytkin (Tommin tilaus 26.9.2026)

Tilaus: *"rarity-värit mukaan myös Your turn-näkymään, oikeastaan kaikkiin pelaajanimi-juttuihin
ja uusi asetus haluaako rarity-värit ollenkaan näkymään"*. Kolme valintaa ennen koodia:
oletus **päällä** (Tommin valinta, suositus oli kuudennen arvon mukainen pois; poikkeus
kirjattu `docs/ARVOT.md`:hen), kytkin kattaa **nimet ja Inboxin viestimäärän**, ja Inboxin
nimissä **rarity korvaa tunnistusvärin ja tuntematon jää tavalliseen tekstiväriin**, koska
tunnistusvärien vihreä, sininen ja violetti olisivat sekoittuneet portaisiin.

Väritetyt kohdat: Your turn -lista (`vs nimi`), ottelun päättymissivun pisterivit (väri voittaa
linkin korostuksen, alleviivaus jää), Loungen pelaajalista ja kutsun tarjoaja, Forumin ketjun
aloittaja ja viestin kirjoittaja, profiilin ja pelaajan turnausten otsikko, profiilin
ottelurivit ja luovutussivun rivit, Inboxin lista, keskustelun otsikko ja kuplien nimet.
Laudan kortti, nauha, turnausrivi ja kaavio olivat jo väritettyjä. Kaikki kulkevat saman
funktion kautta (`playerRarityColor`, `textWithPlayerColor` tiedostossa `Rarity.kt`).

**Nimellä haku** on uusi, koska foorumin aloittaja ja Inboxin keskustelu antavat pelkän nimen.
`PlayerRatingStore.recordNames` kirjaa nimen ja käyttäjänumeron parin sivuilta, joilla molemmat
näkyvät: Loungen pelaajalista, profiili, omien turnausten Refresh ja otteluluettelo. Avain on
`n:` ja nimi pienillä kirjaimilla samassa tiedostossa kuin ratingit. Vanhoilla ratingeilla ei
ole nimeä, joten hakemisto täyttyy vasta käytön mukana.

**Todennettu SM-T970:llä 26.9.2026 klo 19.14–19.18.** Your turn -lista: Sanni sininen,
Bertiln vihreä ja frans violetti. Loungen pelaajalista oranssi. Sen avaus kirjasi 103 nimeä.
Inboxissa siiri, Teppo 77, nellipeltonen, Pertti ja Ville Vaara oransseja ja tuntemattomat
tavallisia. Kytkin pois: lista tavallinen, Inboxin määrät harmaita ja nimet tunnistusväreissä;
takaisin päälle palautti värit. Forumin näkyvistä aloittajista kukaan ei ollut hakemistossa,
joten foorumin ja profiilin väriä ei ole nähty laitteella.

### Pinkki kuudenneksi portaaksi (Tommin tilaus 27.9.2026)

Tilaus: *"miettivän vaihtoehtoa rarity-väreihin jossa pink on korkein taso"*, tarkennus
*"giants of backgammon oli viittaus 32-pelaajaan pinkillä värillä, sen alle rarity-värit"*.
Giants of Backgammon -listalta lainataan siis vain luku 32, ei nimiä eikä lähdettä. Pinkki
on roolipelien kuudes taso *artifact* nykyisten viiden yläpuolella, ja ne jäävät ennalleen.

Valinnat mockupista samana iltana: sävy A (tumma `#FF5FAF`, vaalea `#C2186B`), kiinteä raja
2200 ja Inboxin viestimäärässä 50+. **Raja on mitattu:** tabletin varaston 223 nähdyn ratingin
32. paras oli 2207 ja 100. paras 2136, mikä vahvisti myös oranssin 2140. Kiinteä raja vanhenee
rankingin liikkuessa samoin kuin oranssi, mutta se ei tee yhtään pyyntöä.

**Todennettu laitteilla 27.9.2026 klo 22.33.** Loungen Players-listassa pinkki kattoi sijat
1–33 (33. oli 2206.75, 34. oli 2193.33 oranssina) tabletilla tummassa Decossa, ja Pixel 8a:lla
vaaleassa tilassa pinkki erottui violetista ja oranssista. Inboxin 50+ -keskustelua ei nähty,
koska arkistossa ei ole niin pitkää.

**Raja vaihtui samana iltana sijaan 32** (Tommi: *"tee pinkistä sittenkin top 32 pelaajia
määrittävä, kunnioittaa giants-perinnettä"*, lähteeksi valittu sivuston ranking). Kun Loungen
pelaajalista luetaan ja se on ratingjärjestys (sijat 1–32 mukana, ratingit eivät nouse),
sijan 32 rating tallennetaan rajaksi (`top32Cutoff`, `PlayerRatingStore.recordTop32`, avain
`top32`). Nimi- ja kokemusjärjestys, haku ja *Next 100* eivät muuta rajaa. Ennen ensimmäistä
lukua raja on 2200, eikä pinkki laskeudu oranssin 2140 alle. Tasapelissä sijan 32 kanssa
pinkkejä voi olla 33. **Todennettu SM-T970:llä klo 22.38:** varastoon kirjautui `2207.17`, ja
listassa pinkki päättyi sijaan 32; sija 33 (2206.75) on oranssi.

### Rarity-portaat sijoista (Tommin valinta 28.9.2026)

Tilaus: *"rarity-väri pinkin lisäämisen top 32-pelaajille jälkeen olisi pitänyt rekalibroida
muut lokerot"*. Mittaus ennen muutosta: tabletin 689 tallennetusta ratingista 32 oli pinkkejä,
60 oranssia ja 463 violetteja, eli violetti kattoi valtaosan turnauskaaviosta. Tommi valitsi
kolmesta vaihtoehdosta *kaikki sijoista*.

**Portaat:** pinkki top 32, oranssi top 100, violetti top 300 ja sininen top 1000. Vihreä
pysyy kiinteänä 1600:ssa ja harmaa sen alla (`TIER_RANKS`, `GREEN_FLOOR`, `ratingTier`).
Raja luetaan Loungen pelaajalistan sivulta kun sivu on pala ratingjärjestystä: sijat
juoksevat yhtenäisinä, ratingit eivät nouse ja rivejä on vähintään 32 (`rankCutoffs`).
Ensimmäinen sivu antaa sijat 32 ja 100, *Next 100* -sivut 201–300 ja 901–1000 muut kaksi.
Tallennus `PlayerRatingStore.recordRankCutoffs`, avaimet `top32` (27.9. avain sellaisenaan),
`top100`, `top300` ja `top1000`. Sivua ei haeta erikseen, joten raja päivittyy vain kun
käyttäjä selaa listaa. Siihen asti pätee varalukema `RANK_CUTOFF_FALLBACK`. Raja ei
laskeudu alemman portaan alle, jos luvut vanhenevat eri tahtiin.

**Mitattu SM-T970:llä klo 18.46–18.47** selaamalla sivut 1–1000: sija 32 2207.68, sija 100
2133.9, sija 300 2027.2 ja sija 1000 1836.11. Luvut ovat varalukemina. Sivu 901–1000 näkyi
sinisenä ja sivu 101–200 violettina. Blaablaa oli listan sijalla 3.

### Asetusten siirto merkkijonona (Tommin tilaus 28.9.2026)

Tilaus: *"haluaisin sovelluksen asetuksille export ja import ominaisuuden: copy-pastettava
merkkimössö"*. Kaksi valintaa ennen koodia: mukaan This device -osion valinnat sekä
ottelujärjestys ja Inboxin pelaajasuodatin (ei pikaviestejä eikä kuvakansiota, jonka lupa on
laitekohtainen), ja tuonti esikatselee ennen kirjoitusta.

Laiteosion alimpana on ryhmä *Move settings to another device*, jossa on kaksi nappia.
*Copy settings* vie leikepöydälle yhden rivin `DGA1.…` (deflate ja URL-turvallinen base64,
JSON jossa jokainen arvo kantaa tyyppinsä). *Paste settings* avaa ruudun, joka täyttyy
leikepöydältä kun siellä on tämän sovelluksen teksti. *Preview* luettelee muuttuvat valinnat
asetusruudun omin otsikoin, ja vasta *Use these settings* kirjoittaa. Mukana oleva tiedosto
korvataan kokonaan, joten puuttuva avain palaa oletukseensa. Kirjoituksen jälkeen aktiviteetti
luodaan uudelleen, koska MainActivityn tilat ovat `remember`-tiloja säilöjen arvoista.
Siirrettävät tiedostot ovat `SettingsTransfer.FILES`; tunnukset, sovelluslukko ja arkistot
eivät ole mukana. Testi `SettingsTransferTest` (Robolectric, kaksi tiedostojoukkoa kahtena
laitteena).

**Todennettu SM-T970:llä klo 20.32–20.33:** Copy settings, sitten *Pattern behind the lists*
päälle, sitten Paste settings. Ruutu täyttyi leikepöydältä, esikatselu sanoi *These change:
· Background*, ja *Use these settings* palautti levylle `pattern=false`, jolloin tausta hävisi
uudelleen luodusta ruudusta. **Jäi todentamatta:** siirto laitteelta toiselle, koska
leikepöytä ei kulje tabletin ja Pixelin välillä ilman viestisovellusta.

### Rarity-portaat: kaksi sijoista, kolme kiinteinä (Tommin valinta 29.9.2026)

Tilaus ensin muodossa *"ekat 32 pinkki, seuraavat 100 oranssi, seuraavat 300 purple,
seuraavat 900 sininen, seuraavat 2000 vihreä"*. Claude luki rajat kertyvinä sijoina ja kysyi,
ja Tommi tarkensi: pinkki ja oranssi sijoista, muut kiinteinä ratingrajoina.

**Portaat:** pinkki top 32, oranssi sijat 33–132, violetti 2000 ja yli, sininen 1800–2000,
vihreä 1600–1800 ja harmaa sen alla (`TIER_RANKS` = 32 ja 132, `RATING_FLOORS`). Raja ei
laskeudu alemman alle, joten jos sija 132 putoaisi alle 2000:n, oranssi alkaisi 2000:sta.
Sija 32 luetaan pelaajalistan ensimmäiseltä sivulta ja sija 132 *Next 100* -sivulta 101–200,
avaimet `top32` ja `top132`. 28.9. tallennetut `top100`, `top300` ja `top1000` jäävät
tiedostoon lukemattomina.

**Mitattu SM-T970:llä 29.9.2026 klo 2.32–2.33** selaamalla sivut 1–100 ja 101–200: sija 132
2113.69, tallentui avaimeksi `top132` ja on nyt varalukema (ennen mittausta arvio 2116.83
sijojen 100 ja 300 välistä). Kaappauksessa sija 132 (2113.69) on oranssi ja sija 133
(2111.46) violetti, ja sivun 1 rivit 1–16 ovat pinkkejä.

### Rarity-portaat viidellä värillä ja kiinteinä (Tommin valinta 29.9.2026 illalla)

Tommi: *"pinkki on mustaa meemihuumoria ja jää sittenkin pois"*. Claude tarjosi neljä
porrastusta, joissa oranssi on top 32 ja muut rajat asettuvat eri tavoin, ja lisäksi kuudetta
väriä pinkin tilalle. Tommi hylkäsi kuudennen värin (*"tunnetut rarity-värit riittää"*) ja
antoi rajat itse: **oranssi 2200+, violetti 2000–2200, sininen 1800–2000, vihreä 1600–1800 ja
harmaa alle 1600** (`RATING_FLOORS`, `ratingTier`).

**Sijoista luetut rajat poistettiin kokonaan.** Kaikki viisi porrasta ovat kiinteitä, joten
Loungen pelaajalistan selaaminen ei enää muuta värejä (`rankCutoffs`, `LocalRankCutoffs`,
`PlayerRatingStore.recordRankCutoffs` ja varalukemat poistettu). Raja 2200 on lähellä 27.9.
mitattua sijaa 32 (2207), mutta se ei seuraa rankingia. Tiedostoon kirjoitetut avaimet
`top32` … `top1000` jäävät lukemattomina, koska niiden siivous ei maksa itseään.

**Inboxin viestimäärä menetti portaan 50+**, koska se oli pinkki. Määrä 20 ja yli on nyt
oranssi (`inboxCountTier`). Asetusten selite ja App Helpin *beside*- ja *archive*-jaksot
päivitettiin samassa commitissa.

**Todennettu SM-T970:llä klo 19.08** Loungen pelaajalistasta tummassa Decossa: sija 33
(2203.06) on oranssi ja sija 34 (2196.73) violetti, eikä pinkkiä näy sijoilla 1–49
(`raakasivut/sessio-29-9-rarity/players-2200-raja.png`, versioimatta, kuvassa pelaajanimiä).
Top Pagen vastustaja 1800–2000:n välissä näkyi sinisenä.

### Turnausten rarity-värit (Tommin kuittaus 2.10.2026)

Tilaus: *"rarity-värit pitäisi saada myös turnauksiin, voitko mitata turnausten harvinaisuutta
tai vaikeutta voittaa"*. Claude mittasi 33 tallennettua turnaussivua ja loungen
ilmoittautumislistan ja tarjosi kahta mittaria: montako ottelua putkeen voittaminen vaatii, tai
kuinka usein sarja järjestetään. Tommi kuittasi ensimmäisen ja taulukon sellaisenaan, *"kaikkiin
paikkoihin joissa turnauksen nimi näkyy"*.

**Porras on kierrosmäärä** (`tournamentTier` tiedostossa `Rarity.kt`): enintään 4 harmaa, 5
vihreä, 6 sininen, 7–8 violetti ja vähintään 9 oranssi. Kierrosmäärä on voittojen määrä, ja
tasaväkisellä pelaajalla voiton todennäköisyys on noin 1/2^kierrokset. **Champs on aina
oranssi**, koska turnaussivun NOTE sanoo että sinne pääsee vain voittamalla saman sarjan
tavallisen turnauksen. Esimerkiksi Tortoise Threers Champsin voittaja on voittanut 5 + 7 = 12
ottelua putkeen. Kierrosmäärä luetaan turnauskohtaisesti, koska sama sarja vaihtelee:
Marathon on ollut 5- ja 6-kierroksinen, Stratified Sevens 4- ja 6-kierroksinen.

**Mistä luku tulee.** Otteluluettelon `Round 3/5`, loungen Rounds-sarake ja turnaussivun
`Size`-ehto (`EventPage.roundCount`) kertovat sen suoraan. Tournament Hall, pelaajan
turnauslistat, laudan oma otsikko (`Round 4`) ja ottelun tulossivu eivät kerro, joten ne
lukevat sen `TournamentRoundsStore`sta turnausnumerolla. Luku kirjataan sinne piirron yhteydessä
(`tournamentRarityColor`, `SideEffect`) aina kun se on ruudulla, joten mitään ei haeta sen
vuoksi. Omien turnausten Refresh kirjaa luvut lisäksi niiltä sivuilta jotka se hakee
joka tapauksessa: oman profiilin otteluista (`3/5`) ja linkittömien rivien turnaussivuilta.
Muisti ilmoittaa kirjauksesta Composelle (`mutableIntStateOf`), joten rivi värittyy heti
eikä vasta seuraavalla avauksella. Tuntematon turnaus jää tavalliseen väriin. Marksin
otsikko lukee ottelumuistin kierroksen, koska muisti ei tallenna turnausnumeroa.

**Todennettu Pixel 8a:lla 2.10.2026 klo 19.55–20.05** tummassa Decossa
(`raakasivut/sessio-2-10-rarity/`, versioimatta): otteluluettelon *The Marathon #4299*
(Round 4/5) vihreä. Omien turnausten Refreshin jälkeen kaikki 55 riviä värittyivät:
Marathon vihreä, *Marathon #4289* (3/6), Nine Lives ja Slower Sevens sininen, Deja Vut,
kuukauden Nackit ja *2026 Open* violetti, Championshipit sekä *Weekday Warriors Champs #26*
ja *Tortoise Threers Champs #25* oranssi ja Stratified-turnaukset harmaa. Turnaussivun avaus
kirjasi *December 25 Championship*in yhdeksän kierrosta, ja rivi oli paluun jälkeen oranssi.
Loungen ilmoittautumislista: *October 26 Championship* oranssi, *October 26 Nack* ja *Deja
Vu* violetti, *One Pointer Champs #27* oranssi ja sen *(has note)* tavallinen. **Todentamatta**:
laudan ottelukortti ja ottelun tulossivu, koska ottelun avaaminen kuluttaa jonoa. Tournament
Hallin rivit olivat värittömiä, koska niitä turnauksia ei ollut nähty muualla.

**Paikat:** otteluluettelo, profiilin ottelut, luovutuslista, Tournament Hall (Lounge ja
porautuminen), loungen ilmoittautumisrivi ja sen vahvistusikkuna, omat turnaukset ja pelaajan
turnauslistat, turnaussivun otsikko, laudan ottelukortti, ottelun tulossivun otsikko ja Marks.
Värin saa vain nimi: kierros, *Has Note* ja muu lauseen teksti pitävät oman värinsä. Laudan
paneelissa sävy seuraa paneelin vaaleutta kuten pelaajakortin nimessä.

**Kytkin on sama** (`rarity`), ja sen nimi laajeni muotoon *Colour names by rating, tournaments
by rounds and the Inbox count by size*. Pois kytkettynä `MainActivity` antaa tyhjän varaston,
ja apufunktio palauttaa nullin.

**Vuosittainen turnaus on aina oranssi** (Tommi samana iltana: *"2026 open on kerran
vuodessa järjestettävä ja siksi legendary"*, ja kysymys *"onko mahdollista hakea turnauksia
joissa vain vuosiluku päivittyy?"*). Tallennetusta Tournament Hallista (`raakasivut/
tournament_hall.html`, 26.8.2026, 3185 riviä vuosilta 2021–2026) poimittiin nimet, joissa
on vuosiluku, ja ryhmiteltiin ne vuosiluku poistettuna. Vain kolme sarjaa toistuu pelkän
vuosiluvun vaihtuessa: `<vuosi> Open` (2024, 2025 ja 2026, tammikuu), `<vuosi> Invitational`
(2025 ja 2026, tammikuu) ja `<vuosi> Double Repeat` (2025 ja 2026, helmikuu). Kaikissa
vuosiluku on nimen alussa, eikä mikään muu nimi ala vuosiluvulla, joten sääntö on *nimi
alkaa vuosiluvulla* (`YEARLY`). Strawberry Stratifiedsin `#2005` on sarjanumero eikä vuosi,
ja *DG 8x8 2026*:ssa vuosiluku on lopussa. Kuukausiturnaukset (*May 26 Championship*:
kuukausi ja vuoden kaksi numeroa) seuraavat yhä kierrosmäärää. Ennen tätä *2026 Open* oli
kahdeksalla kierroksellaan violetti.

**Tommin todennus 2.10.2026 illalla:** otteluluettelo ja laudan ottelukortti *"värittyi
kivasti"*. Ottelun tulossivu on yhä näkemättä.

**Pelaajien omat turnaukset jäävät värittömiksi** (Tommin valinta 2.10.2026 kolmesta:
värittömät, yksi kiinteä väri tai oma väri nimen alun mukaan). Väri kertoo silloin aina
mitatusta asiasta. Esimerkiksi *DG 8x8 2026* on sivustolla
nimettyjä kutsuotteluita ilman turnaussivua: otteluluettelon Round-sarake on `-`, eikä
nimessä ole linkkiä (Top Page 2.10.2026). Kierrosmäärää ei siis ole mistä lukea.

### Asetusten siirto vain tiedostona (Tommin tilaus 29.9.2026)

Tilaus: *"poista copy settings ja paste settings, alemmat napit hoitaa homman ilman
confusion"*. Ryhmässä *Move settings to another device* ovat enää *Save to file* ja *Open
file*. Avattu tiedosto menee samaan esikatseluruutuun kuin ennen, nyt otsikolla *Settings from
a file*, ja sen tekstikenttä on yhä muokattava. Leikepöytää sovellus ei enää lue eikä kirjoita
tässä ryhmässä. Ruudun selite ja App Helpin Settings-jakso kertovat saman.

### Inboxin määräsarake vasemmasta reunasta (Tommin valinta 29.9.2026)

Tilaus: *"numero alla samalla x-koordinaatilla, nyt taitaa olla oikeassa laidassa"*.
Otsikko `Count` ja rivin luku olivat kumpikin oikeaan tasattuja 40 dp:n sarakkeessa, jolloin
yksinumeroinen luku jäi sanan oikeaan laitaan. Kolmesta vaihtoehdosta Tommi valitsi vasemman
reunan: otsikko ja luku alkavat nyt kumpikin sarakkeen vasemmasta reunasta.

### Pelaajalistan sivuttajat myös listan alla (Tommin tilaus 29.9.2026)

Tilaus: *"Lisää Players-listauksen alle samanlainen Previous 100- ja Next 100-toiminnot mitä
sen yllä on"*. Listan viimeisen rivin alla on nyt sama linkkirivi (`PlayerListLinks`), mutta
vain sivuttajat: sivun linkeistä ne joiden teksti alkaa sanalla *Previous* tai *Next*
(`isPagingLink`). Lajittelijat jäävät vain ylös. Tämä on ainoa kohta jossa linkki luokitellaan
tekstistä, ja tunnistamaton linkki jää vain ylös, joten väärä arvaus ei piilota mitään.

**Alalinkki vierittää uuden sivun alkuun.** Ensimmäinen laiteajo (SM-T970 klo 2.37) näytti,
että vieritys jäi sivun loppuun: sivun 201–300 ensimmäinen näkyvä rivi oli 285. Nyt Lounge
omistaa listan `LazyListState`n (`DgLazyColumn` sai `state`-parametrin), ja alalinkin painallus
vierittää kohtaan 0, kun uusi sivu on ladattu. Players on Loungen ensimmäinen jakso, joten
kohta 0 on sen otsikko. Ylälinkki ei vieritä.

**Todennettu SM-T970:llä klo 2.39:** sivun 1 alla on pelkkä *Next 100* (sivulla ei ole
Previousia), ja sen painallus avasi sivun 101–200 alusta, rivi 101 linkkirivin alla. Sivun
101–200 alla ovat *Previous 100* ja *Next 100*. Alarivin selite sanoo nyt, että sivua
käännetään ylä- tai alalinkeistä.

### Pelaajahaun kenttään Clear (Tommin tilaus 29.9.2026)

Tilaus: *"Players Search kaipaa sitä tyhjentäjä ruksia tekstikenttään, muuten aiempi haku jää
kenttään vaikka sen pyyhkisi"*. Käsin pyyhitty kenttä jätti edellisen haun tulokset listaksi,
koska tyhjää hakua ei lähetetä eikä pyyhkiminen ole teko. Kentän sisällä on nyt `Clear`, sama
tekstinappi kuin Infon hakukentässä (`info_search_clear`). Se näkyy kun kentässä on tekstiä tai
haku on voimassa, ja tyhjentää kummankin kerralla. Kentän alla oleva *Show all* säilyi.

**Todennettu SM-T970:llä klo 2.56:** haku `mark` näytti Mark-alkuiset pelaajat ja Clearin
kentässä. Clear tyhjensi kentän ja palautti ratingjärjestyksen sivun 1, ja Search palasi
harmaaksi.

**Search kentän kehyksen keskelle** (Tommi 29.9.2026: *"hakukentän Clear ei ole aivan samalla
y-koordinaatilla kun Search"*). Rivi keskitti napin koko reunustettuun kenttään, johon kuuluu
otsikon 8 dp:n varaus kehyksen yllä, joten Search oli 7 px Clearia ylempänä (keskikohdat y 323
ja 330 kaappauksessa). Napin 8 dp:n yläpehmuste siirsi sen kehyksen keskelle, ja klo 3.00
kaappauksessa kummankin keskikohta on y 330.

### Pystylaudan lisät pareina (Tommin tilaus 26.9.2026)

Tilaus sellaisenaan: *"Puhelimen Pystytilassa Skip Game ja Mark position samalle riville"* ja
*"Reminders ja Cube reminder samalle riville"*. Laudan alla lisät olivat yksi `FlowRow`, joka
rivitti niitä leveyden mukaan eikä parien mukaan.

Nyt pystylaudalla (`panelBelow`) `ReminderActions` saa lipun `pairRows`. Sivun napit (`Chat`,
`Skip Game`) ja `Mark position` ovat yksi pari, `Reminders` ja `Cube reminder` toinen. Kumpikin
pari on oma `FlowRow`nsa ulomman rivin sisällä, joten pari rivittyy kokonaisena. Puhelimella
parit ovat silloin omilla riveillään. Tabletin pystylaudalla kaikki mahtuvat yhdelle riville,
joten siellä järjestys ja rivi pysyvät ennallaan. Isolla fonttikoolla pari voi yhä rivittyä
sisäisesti eikä leikkaudu reunaan. Ahtaan paneelin rivityssäännöt (`buttonsOwnRow`,
`markOwnRow`) koskevat enää vaakalaudan sivupaneelia.

**Todennettu Pixel 8a:lla 26.9.2026 klo 22.41 kuorella** (`sessio-26-9-ilta3`, `0038` ja
`0052`, airlock 503): rivillä yksi `Skip Game` ja `Mark position`, rivillä kaksi `Reminders` ja
`Cube reminder` (`raakasivut/sessio-26-9-pysty/p09-paririvit.png`). Tablettia ja
`Roll Dice`/`Double`-laudan tilaa, jossa `Skip Game` on `Roll Dicen` alla ja pari on pelkkä
`Mark position`, ei ajettu.

**`Skip Game` pois `Roll Dicen` alta (Tommin valinta A 26.9.2026, kumoaa 24.9.2026).** Tommin
kaappaus samana iltana oikealta laudalta (`The Marathon #4299`, `Roll Dice` ja `Double`):
`Skip Game` oli `Roll Dicen` alla, ja yksinäinen `Mark position` mahtui muistutusparin viereen,
joten kolme linkkiä olivat yhdellä rivillä. Tommi: *"Skip game ja Mark position ovat eri
riveillä"*. Kolmesta mockupista (A pois alta, B yksin Mark, C Mark Skipin viereen) valinta oli
A. Pystylaudalla `Skip Game` on nyt aina `Mark position`in parina, ja `Verify Double` on
`Doublen` alla vasemman sarakkeen jäädessä tyhjäksi. 24.9. päätös (*"Skip game sopisi hyvin
heti Roll dice alle"*) oli kirjattu vain koodikommentteihin (`skipInStack`,
`underReversible`), ja ne poistuivat tämän mukana.

### Asetusten tallennusalue kiinni alareunassa (Tommin tilaus 26.9.2026)

Tilaus: *"Settings/Dailygammon haluaisin sivun alaosan Mini-kortin alla olevan koko ajan
näkyvissä, jotta pelaaja muistaa tallentaa muutokset"*. Muutoslaskuri, `Update Preferences` ja
`Fill in the first reading` olivat lomakkeen lopussa. Sykkivä nappi jäi siksi vierityksen
taakse juuri silloin kun muutos odotti.

Nyt `SettingsForm` vierittää itse vain kentät, ja tallennusalue on sen alla kiinteänä viivan
erottamana. Pystypehmuste puolittui, koska alue vie tilaa joka käynnillä. Palautuksen
selitysteksti poistui kokonaan (Tommin päätös samana iltana: *"selitystekstiä ei tarvita"*).
Muut tilat (lataus, virhe, väärä sivu) vierittävät kuten ennen.

**Todennettu Pixel 8a:lla 26.9.2026 klo 22.52 kuorella** (profiili `sessio-25-9-ilta2/0016`,
airlock 503): yksi raksi pois, laskuri *1 setting(s) will change.* ja purppura kehys näkyivät
lomakkeen alussa ja Mini-kortin kohdalla (`raakasivut/sessio-26-9-pysty/p10-` ja
`p11-tallennus-*.png`). Lomaketta ei lähetetty. Tablettia ei ajettu.

**Valinta A todennettu Pixel 8a:lla 26.9.2026 klo 23.00–23.02 kuorella** (top `sessio-26-9-ilta3/0038`,
laudat `sessio-26-9-ilta3/0053` ja `sessio-26-9-yo/0039`, airlock 503). Ilman vahvistusruutua
`Roll Dice` ja `Double` ovat allekkain. Vahvistusruudun kanssa ne ovat rinnakkain, `Verify Double`
on `Doublen` alla ja vasen sarake on tyhjä. Molemmissa tiloissa `Skip Game` ja `Mark position`
ovat yhdellä rivillä, ja `Reminders` ja `Cube reminder` toisella
(`raakasivut/sessio-26-9-pysty/p12-` ja `p13-valinta-a*.png`).

### Puhelimen pystylaudan pelaajakortit kaksirivisiksi (Tommin tilaus 26.9.2026)

Tilaus: *"muuta pixelin pystytilan pelaajakortit 2-rivisiksi kuten vaakatilassa"*. Tämä kumoaa
puhelimen osalta 24.9.2026 valinnan *"molemmat 3 riviä"*. Sen peruste oli se, että laudan ylä-
ja alapuolen kortit näyttivät kahdelta eri kortilta, ja se pysyy voimassa: molemmat kortit
muuttuvat yhdessä. Nimi ja away ovat nyt ensimmäisellä rivillä ja pipit toisella, kuten
vaakatilan ahtaassa paneelissa. Ehto on sama `compactPanel` joka päättää ahtaan paneelin, eli
mitta eikä laite. Tabletin pystylaudalla ehto ei täyty, joten siellä kortti pysyy kolmirivisenä.

**Todennettu 26.9.2026 klo 23.04–23.05 kuorella** (top `sessio-26-9-ilta3/0038`, lauta
`sessio-26-9-yo/0039`, airlock 503). Pixel 8a: molemmat kortit kaksirivisiä
(`frans 3-away` ja `Pips 101 (-24)`), `raakasivut/sessio-26-9-pysty/p14-kortit-2-rivia.png`.
SM-T970 pystyssä: kortit kolmirivisiä kuten ennen, ja lisät mahtuvat yhdelle riville
(`t01-tabletti-kortit-3-rivia.png`).

### Oikea tahko 16 ja käynnistysruudun kierto (1.10.2026)

**Oikea sivutahko on 16** (Tommin valinta 1.10.2026, aiemmin 64 ja 23.9. alkaen 4). Kuvake on
nyt pää, 2 ja 16. Pään ja 2:n vierekkäisyys ei ole ristiriita: Tommin kuudessa kuutiossa
64 ja 32 olivat useimmiten vastakkain, ja se mikä oli 2:n vieressä vaihteli.

**Käynnistysruutu kääntää kuution kuten ensimmäinen tuplaus** (Tommin tilaus 1.10.2026):
pää kiertyy ylhäältä sivulle ja 2 nousee päälle. Kotinäytön ja kaupan kuvake ei voi liikkua,
mutta Android 12+ näyttää sovellusta avattaessa animoidun kuvakkeen. `kuvake.py` piirtää
kuution nyt kuuden tahkon kappaleena kierrolla `t` etu-taka-akselin ympäri, ja kuvake on sen
nollakohta (pikselivertailu vanhaan: ero vain 16:n kohdalla). Animaatio on 15 kuvan sarja
(`drawable-nodpi/splash_cube_*.webp`, 1 Mt, `drawable/splash_cube.xml`), kesto 150 + 13 × 50
+ 200 = 1000 ms, Googlen suosituksen yläraja. Teema `values-v31` ja `values-night-v31`,
kuvakkeen tausta kotinäytön musta emali. `MainActivity.keepSplashUntilCubeTurned` pitää
ruudun auki kierron loppuun, koska järjestelmä poistaisi sen heti ensimmäisestä ruudusta.
3.10.2026 alkaen se tehdään pidättämällä sovelluksen ensimmäistä ruutua, ja järjestelmä
poistaa ruutunsa itse (ks. alla Luovutushetken välähdys).

**Dokumentaatio ei ollut yksiselitteinen**: muotovaatimus sanoo vektorianimaatio, mutta
toinen kohta mainitsee kuvasarjan (`AnimationDrawable`). Kuvasarja todennettiin laitteella:
nauhat `splash-pixel2.mp4` (Pixel 8a, Android 16) ja `splash-tabletti.mp4` (SM-T970,
Android 13) session scratchpadissa, kehykset 8–10/s. Molemmissa kuutio kääntyy päästä 2:een.
Tabletilla ruutu pysyy 2 päällä noin 1,4 s pidempään, koska sovelluksen ensimmäinen ruutu
valmistuu hitaammin; se on järjestelmän käytös eikä vika.

**Liike vaihtui 3.10.2026 heitoksi** (Tommin tilaus: *"tuplauskuutio -> 2 sijaan tuplauskuutio
pyörivän ja asemaan androidin pää"*). Neljästä luonnoksesta (takaisinkierto 2:sta päähän, täysi
kierros etu-taka-akselin ympäri, hyrrä pystyakselin ympäri, heitto) Tommi valitsi heiton: kuutio
pyörii x- ja z-akselin ympäri, jäljellä alussa 1,3 ja 1,5 puolikierrosta, ja asettuu pienen
yliheiton kautta kuvakkeen lepoasentoon, pää ylhäällä. `kuvake.py` kiertää nyt mielivaltaisen
akselisarjan (`splash_turn`), kuvake on yhä nollakohta, ja kesto on sama 1000 ms. App Helpin
*Board look* sanoo sen yhdellä lauseella. Todennettu SM-T970:llä kolmella nauhalla: kuutio lähtee
asennosta, jossa 4 ja 32 näkyvät, ja asettuu pää ylhäällä.

**Luovutushetken välähdys on vanha vika** (havaittu 3.10.2026). Juuri ennen häivytystä kuution
paikalla näkyy toisinaan 1–4 kehystä pelkkä kuvakkeen tausta. Uusi heitto välähti kolmesta
ajosta kahdesti, ja 1.10.2026 kierto, joka asennettiin vertailuun samalle laitteelle, välähti
neljästä ajosta kerran. Se osuu järjestelmän luovutukseen (`keepSplashUntilCubeTurned`) eikä
kuvasarjaan. **Korjattu 3.10.2026**, mittaus alla.

**Syy on luovutuksen pinta.** Kun sovellus asettaa `setOnExitAnimationListener`in, järjestelmä
siirtää käynnistysruudun sovelluksen ikkunaan. Animoitu kuvake on siirrossa `SurfaceView`
(luettu lokista: `iconView` on `android.view.SurfaceView`, 340 × 340 px), jonka sisällön
piirtää järjestelmän prosessi ja jonka taustana on kuvakkeen musta pohja bittikarttana.
Siirron ensimmäisessä kehyksessä pinta on toisinaan vielä tyhjä, jolloin näkyy pelkkä pohja.
Erillinen pinta ei myöskään tottele näkymän alphaa, joten 150 ms:n häivytys haalisti vain
taustan ja kuutio katosi lopussa kerralla. Pixel 8a:lla (Android 17) näin kävi 6/6 ajossa.

**Kuuntelijassa tehty korjaus ei riitä.** Ensimmäinen kokeilu piilotti pinnan kuuntelijassa
ja piirsi tilalle oman `ImageView`n samasta kuvasarjasta. Häivytys korjaantui, mutta tyhjä
kehys jäi (2/5 ajoa), koska järjestelmä piirtää siirretyn ruudun kerran ja kutsuu
kuuntelijaa vasta sen jälkeen. Kuuntelija ei siis ehdi väliin.

**Korjaus on pidätys ilman luovutusta.** Kuuntelijaa ei aseteta, vaan `OnPreDrawListener`
estää sovelluksen ensimmäisen ruudun kunnes kierto on ohi (Androidin dokumentoima tapa pitää
käynnistysruutua pidempään). Kierron alkua ei ilman kuuntelijaa voi lukea, joten raja
lasketaan prosessin alusta: kolmessa kylmäkäynnistyksessä kierto alkoi −40…+66 ms
prosessin alusta (SM-T970, loki), ja raja on prosessin alku + 100 ms marginaali + 1000 ms
kierto. Prosessin myöhempi aktiviteetti laskee omasta luonnistaan, ja uudelleenluonti
(`savedInstanceState`) ei pidätä, koska käynnistysruutua ei silloin näytetä. Juuriaktiviteetin
Back ei Android 12+:ssa tuhoa aktiviteettia, joten tavallinen paluu on kuuma käynnistys ilman
käynnistysruutua.

**Mittaus 3.10.2026**, kylmäkäynnistykset nauhalta, keskeltä rajattu 120 × 120 px kirkkaus
(nauhat ja `nauha.py` session scratchpadissa):

| Laite | Versio | Ajoja | Tyhjä kehys ennen häivytystä | Häivytys |
|---|---|---|---|---|
| SM-T970, Android 13 | ennen | 3 | 1 (yksi kehys) | tausta haalistuu, kuutio katkeaa |
| SM-T970 | kuuntelija + oma kuva | 5 | 2 | kuutio haalistuu mukana |
| SM-T970 | pidätys (lopullinen) | 6 | 0 | järjestelmän oma, noin 50 ms |
| Pixel 8a, Android 17 | ennen | 6 | 0 | tausta haalistuu, kuutio katkeaa 6/6 |
| Pixel 8a | pidätys (lopullinen) | 6 | 0 | järjestelmän oma, noin 150 ms |

Lopullisessa versiossa kuutio oli kaikissa 12 ajossa lepoasennossa pää ylhäällä ennen
poistumista, myös Pixelillä, jolla sovellus valmistuu ennen kierron loppua ja pidätys siis
ratkaisee. **Jäljelle jää järjestelmän oma poisto:** SM-T970:llä 5/6 ajossa yksi kehys
häivytyksen keskellä näyttää kuvakepohjan noin kolmanneksen peittävyydellä, ja Pixelillä
häivytyksen viimeiset kaksi kehystä ovat lähes mustaa tyhjää ympyrää. Ne ovat järjestelmän
poistoanimaatiota, johon sovellus ei pääse käsiksi ilman luovutusta.

**Pixelin jäätynyt kuutio on erillinen järjestelmän ilmiö.** Pixel 8a:lla kuutio jäi
kerran kuudesta alkukuvaansa (4 ja 32) eikä pyörinyt, sekä vanhalla että uudella versiolla
(`splash_pixelennen_4`, `splash_pixel_1`). Tabletin 19 nauhoitetussa ajossa sitä ei näkynyt. Syytä ei ole
selvitetty.

**Kallistus pysyy isometrisenä** (Tommin päätös 1.10.2026). Kuusi kallistusta luonnosteltiin
(kierto ja kallistus 45/35, 30/35, 60/35, 45/20, 45/50 ja 25/22 astetta), ja nykyinen 45/35
valittiin.

## Päättymissivun Reply to (27.9.2026)

**Ottelun päättymissivulla vastustajan pisterivin alla on nappi *Reply to <nimi>*,** joka
avaa viestikentän ottelun nimi lainausrivinä. Tommin tilaus 27.9.2026 (*"kyllä, tee
Reply-nappi päättymissivulle"*) saman illan selvityksen jälkeen.

**Malli on selainlaajennus DGText2Area, ei sivusto.** Brave näytti turnausottelun 5315033
päättymissivulla napin *Reply to <vastustaja>*, ja sovellus ei näyttänyt samasta osoitteesta
(`raakasivut/sessio-27-9-ilta`, kaappaukset vastustajan nimellä, `-loppu.png` ja
`-loppu-selain.png`). Braven tallentamassa lähdekoodissa (`selain-5315033-4840.html`)
nappia ei ole. Sen lisää laajennuksen `win_reply.js`: kun sivun `H4` vastaa lauseketta
`wins \d+ points? and the match.`, skripti lisää pistetaulukkoon napin jokaiselle pelaajalle
paitsi evästeen `USERID` omistajalle. Painallus avaa lomakkeen `/bg/sendmsg/<id>`, jonka
kenttä on esitäytetty ottelun otsikolla ja vastustajan viestillä lainausriveinä. Selvityksen
kulku ja kaksi kumottua arvausta (tekstikenttä, User-Agent) ovat `raakasivut/LUEMINUT.md`:n
session luvussa.

**Kolme valintaa, jotka poikkeavat laajennuksesta tai täydentävät sitä:**

- **Lomake luetaan vastustajan profiilista eikä koota.** Laajennus rakentaa osoitteen
  pelaajanumerosta. Sovellus hakee profiilin pistetaulukon omasta linkistä ja lähettää sen
  `Quick message` -lomakkeella (`BoardViewModel.sendMatchReply`), samoin portein kuin
  profiilin viestikenttä: lomake sivulta, yksi lähetys kerrallaan ja onnistuminen
  kuittaussivulta ennen arkistointia. Profiilin haku ei kuluta jonoa. Lomakkeeton profiili
  antaa tilan `ReplyUiState.NoForm`, eikä mitään lähetetä.
- **Nappi on pisterivin alla eikä vieressä,** koska puhelimen kapealla ruudulla rivi on nimi
  ja luku. Kenttä on sama komponentti kuin profiilissa (`MessageComposer`) fraasirivin kanssa.
- **Esitäyttö on laajennuksen muoto** (`matchReplyPrefill`): otsikko lainausrivinä,
  vastustajan viesti tyhjän lainausrivin jälkeen jos sellainen oli, ja kaksi rivinvaihtoa.
  Fraasi jatkaa rivinvaihdon jälkeen eikä syö sitä (`withPhrase`). Kuoriajossa ennen
  korjausta `gg` joutui lainausriville (`kuori4.png`).

**Lähtevä viesti on pikaviesti.** Vastaanottaja saa sen jonoonsa otsikolla *DailyGammon Quick
Message* (`docs/KOHDE.md`), eikä se liity ottelun chattiin. Arkistossa sen `matchId` on null
kuten profiilista lähetetyllä, ja ottelu näkyy viestissä lainausrivinä.

**Todennettu kuorella tabletilla 27.9.2026 klo 21.18–21.22.** Päättymissivuna tommihin
turnausvoitto (`sessio-12-9-ilta2/0007`, Zakarias). Nappi näkyi vain Zakariasin rivillä
(`kuori2.png`), kenttä avautui esitäytettynä (`kuori3.png`), fraasi meni omalle rivilleen
(`kuori7.png`), ja Send haki profiilin sivun linkistä ja lähetti sen lomakkeen osoitteeseen.
Kuori esti POSTin, ruutu kertoi 503:n ja teksti jäi kenttään (`kuori8.png`). **Oikeaa
lähetystä sivustolle ei ole tehty**, joten kuittaussivun luku ja arkistointi ovat todennettu
vain yksikkötesteillä. Ne todentuvat ensimmäisellä oikealla vastauksella ottelun päätyttyä.

**Oikealla päättymissivulla SM-T970:llä 29.9.2026 klo 4.00–4.01** (Pentamerous Nack #4246,
Round 4, tommih 5 matti.v 0, ennalta ratkaistu). Ruutu näytti selitteen *DailyGammon settled
this in advance*, pisterivit rarity-väreissä (tommih pinkki, matti.v violetti), napin *Reply
to matti.v* vain vastustajan rivin alla ja kortin *Chat with matti.v* fraaseineen,
varoitusrivin sekä napit `Next Game` ja `To Top`. Vastustajan viestiä ei ollut. *Reply to*
haki profiilin ja avasi kentän *Message to matti.v* esitäytöllä `> Pentamerous Nack #4246,
Round 4` ja kahdella rivinvaihdolla (laskuri 35 merkkiä), fraasirivi ja `Send` alla ja chat-
kortti ennallaan sen alapuolella. Kaappaukset `raakasivut/sessio-29-9-yo/`.

**Ensimmäinen oikea lähetys, Tommi klo 4.05:** fraasi *ty gm, till we meet again*
esitäytön perään, Send. Ruutu kertoi *Sent, and saved to your message archive*, kenttä
tyhjeni ja Send harmaantui. Arkistossa (`messages`, laitteelta kopioitu kanta) rivi
`QUICK_MESSAGE`, `matchId` null, `opponent` matti.v, runko esitäyttö ja fraasi kahden
rivinvaihdon jälkeen, `rawHeader` *Your message has been sent to matti.v*. Kuittaussivun
luku ja arkistointi ovat siis todennettu laitteella.

**Kenttä sulkeutuu onnistuneen lähetyksen jälkeen** (Tommin tilaus 29.9.2026, kysymyksestä
*"miksi näkymässä on auki kaksi text areaa?"*). Lähetyksen jälkeen tyhjä Reply to -kenttä jäi
auki chat-kortin yläpuolelle, ja ruudulla oli kaksi päällekkäistä viestikenttää. Nyt tila
`ReplyUiState.Sent` sulkee kentän, nappi *Reply to* palaa, ja sen alla lukee kuittaus
(`page_message_sent`). Napin painallus avaa uuden viestin esitäytettynä, ja kuittaus poistuu.
Kaksi kenttää ovat eri kanavia: Reply to lähettää pikaviestin heti, ottelun chat lähtee vasta
Next Gamen tai To Topin mukana.

**Todennettu kuorella SM-T970:llä klo 4.10–4.11** (`kuoriproxy.py` uudella `--sendmsg`-
reitillä: päättymissivu `sessio-12-9-ilta2/0007`, profiili `sessio-16-9-aamu/0003`, kuittaus
`sessio-27-9-yo/0006`). Reply to Zakarias, fraasi gg, Send: kuori sai `POST /bg/sendmsg/16674`,
kenttä sulkeutui ja kuittaus jäi napin alle; uusi painallus avasi kentän ja kuittaus poistui.
Kuoriajo jätti arkistoon lähettämättömän rivin, ks. `kuoriproxy.py` › `--sendmsg`. Rivi
poistettiin Tommin luvalla klo 4.15 (sovellus pysäytettynä, kanta kopiona koneelle, yksi rivi
pois, `integrity_check` ok, takaisin): arkistossa 76 → 75 viestiä, ja Inbox avautui ilman
Zakarias-riviä. Päivittäinen varmuuskopio (klo 2.30) oli otettu ennen kuoriajoa.

### Siirtonuolet kootusta siirrosta (Tommin idea ja kuittaus 29.9.2026)

Kytkin `Show arrows for my move` (`MoveArrowsStore`, oletus pois, `docs/ASETUKSET.md`) piirtää
paikallisen kokoamisen jokaisen askeleen nuolena ennen `Submit Move`a. Viitekuvat ovat XG:stä
ja BGBlitzistä (`raakasivut/nuoli-xg-ennen-lahetysta-1.webp`, `-2.webp`,
`raakasivut/kommentti-bgblitz-3.4.5.png`), ja kuittaus annettiin mockupin jälkeen
monivalintana: kaikki askeleet, kytkin oletuksena pois, vastustajan siirto erikseen.

**Nuolen päät tulevat kokoamisesta eikä ruudulta.** `CompositionSession.arrows` simuloi askeleet
lähtöasemasta, joten häntä on lähtöpisteen päällimmäinen nappula sillä hetkellä ja kärki
kohteen seuraava vapaa paikka. Tuplan ketju jatkuu siksi siitä mihin edellinen nuoli päättyi, ja
saman pisteen kaksi nappulaa saa rinnakkaiset nuolet (`LocalCompositionTest`). Syöty blotti ei
vie paikkaa, ja uloskanto päättyy lokerosarakkeeseen sen puoliskon kohdalle jolta kannettiin.

**Piirto on Canvas-kerros koko laudan päällä, ja se on tietoinen poikkeus** tämän osion
periaatteesta *tavalliset composablet eikä Canvasia*. Nuoli ylittää muurin ja keskikaistan,
eikä yksikään laudan composable omista sitä tilaa. Poikkeus on rajattu: nuoli on koriste joka
toistaa jo piirretyn laudan, sillä ei ole tekoa eikä kosketusta, joten kiilat ottavat
napautukset kuten ennen. Geometria (`arrowLines`, `MoveArrows.kt`) on puhdas funktio samoista
mitoista kuin lauta (13 saraketta, `CheckerStack`in pinon askel, peilaus), ja `MoveArrowsTest`
väittää sen koordinaatit. Väri on oman nappulan täyttö tumman reunan päällä, jotta nuoli
erottuu sekä huovasta että saman värisistä nappuloista kaikissa lautatyyleissä.

**Vastustajan edellinen siirto (Tommin kuittaus 29.9.2026 illalla).** Oma kytkin `Show arrows
for the opponent's move` samassa ryhmässä, oletus pois (`MoveArrowsStore`, `docs/ASETUKSET.md`).
Siirto luetaan laudan omasta `<<Review Game` -linkistä (`BoardState.reviewPath`) siirtolistalta,
puretaan askeliksi ja lasketaan taaksepäin nykyisestä laudasta (`OpponentMove.kt`). Lista joka ei
sovi lautaan ei piirrä mitään. Nuolet ovat vastustajan nappulan värisiä ja näkyvät omalla
vuorolla ennen ja jälkeen `Roll Dice`n, kunnes ensimmäinen oma askel on koottu. **Kumottu
30.9.2026 illalla:** nuolet näkyvät vain vastustajan noppien kanssa ja häviävät omassa `Roll
Dice`ssa (Tommin kuittaus, *"kun vastustajan nopat häviää, niin siirtonuolet häviää"*). Heiton
jälkeen nuolilta puuttui heitto josta ne syntyivät. Ehto on `BoardScreen`issä, koska oma väri
luetaan siellä; koottu askel pysyy ehtona tilassa (`showsOpponentMove`). Muurilta tuleva
nuoli lähtee alasegmentistä, koska vastustajan nappula on siellä (`BarSegment`).

**Lista tulee noin sekunnin laudan jälkeen** (mitattu `sessio-29-9-ilta3`: 0,7–1,2 s). Nopea
`Roll Dice` ehti ennen sitä. Odotuksen ajan keskikaistan tilariveissä lukee *Reading opponent's
move…* (Tommin valinta A kolmesta mockupista, `stripNotes`). Painallusta ei estetä. Heiton
jälkeen samaa listaa ei haeta uudelleen, koska asema on sama.

**Rivi noudattaa samaa ehtoa kuin nuolet** (Tommin tilaus 29.9.2026 myöhään illalla,
`showsOpponentMove`): se näkyy vain kun omaa askelta ei ole koottu. Sessiossa
`sessio-29-9-ackammon` rivi näkyi hetken sovelluksen esipoimiman pakkosiirron päällä, vaikka
nuolia ei silloin piirretä. Lista haetaan edelleen, jotta `Undo Move` palauttaa nuolet.

**Tumma nuoli saa vaalean reunan** (Tommin valinta samana iltana, `arrowHalo`): musta nuoli
tummalla laudalla ja mustien nappuloiden päällä erottui tumman reunan kanssa huonosti
(Verneriin ottelu, `sessio-29-9-ilta3`). Sääntö koskee molempia nuolia ja luetaan täytön
kirkkaudesta.

### Omistamaton kuutio lokerosarakkeen keskikaistalle (Tommin tilaus 29.9.2026 illalla)

*"tuplauskuutiolle pysyvä paikka keskikaistan nappulalokerossa"* ja tarkennus
monivalinnasta: **vain omistamaton kuutio** ja **lokero kulkee peilauksen mukana**. Kuutio on
nyt aina lokerosarakkeen keskellä, ylä- ja alalokeron välissä (`CubeBadge`), eikä enää
kaistan muurilla. Omistettu pysyy omistajan kortissa tai muurin päässä. Tarjottu on oman
puoliskon keskellä kuten ennen. Muurin pinon väistö (23.9.2026, `cubeGap`) poistui, koska
muurilla ei enää ole kuutiota jota väistää.


### Koko näyttö, lovi ja ahdas vaakapaneeli (Tommin havainnot 30.9.2026)

Koko näytön kytkin (`docs/ASETUKSET.md` › Koko näyttö) teki Pixel 8a:lla näkyväksi kaksi asiaa,
jotka tilapalkki oli peittänyt, ja kolmannen, joka ei johtunut siitä.

**Välilehtirivi väistää tilapalkin ja loven yhdessä** (`tabRowInsets`, `DgTabs.kt`). Pystyssä
reikä rikkoi sanan `Forum`, koska rivi väisti vain tilapalkkia ja piilotettu palkki on nolla.
`NavHost` kuluttaa saman arvon, jottei välilehtiruutu varaa kaistaletta toistamiseen.

**Lautaruutu väistää yläreunan loven** (`BoardScreen`). Pystyssä reikä oli turnausnimen
päällä. Vain yläreuna väistetään, joten 16.9.2026 päätös (vaakatilan sivulovi ei vie
paneelin leveyttä) on ennallaan. Laitteella, jonka reikä on kulmassa, vaakatilan lovi voi
osua pelaajakortin tekstiin. Sitä ei ole mitattu.

**Ahtaassa vaakapaneelissa sivun teot kahdessa sarakkeessa** (Tommin valinta A mockupista,
`PanelActionStack`). Tämä ei johtunut koko näytöstä, koska puhelimen lauta piilotti palkit
jo ennen kytkintä. JB:n laudalla (2026 Open, ei omaa vuoroa) lisät `Chat` + `Skip Game`,
`Mark position` ja muistutukset, ottelukortti sekä `Next Game` ja `To Top` olivat noin 25 dp
korkeampia kuin korttien väli. `CenteredOverStack` antaa pinon voittaa, joten lisät valuivat
vastustajan kortin päälle. Rinnakkain pino säästää napin korkeuden. Ehto: ahdas paneeli,
vaakalauta, ei odotusta eikä vahvistusruutuja. Vahvistusruudun ryhmä on yksi lohko, joka ei
mahdu puoleen leveyteen, joten silloin pino pysyy pystyssä. Todennettu laitteella vain
pystypinon puoli (`Roll Dice`, `Double` + `Verify Double`, mahtui). Rinnakkainen muoto odottaa
seuraavaa lautaa, jolla on `Next Game` ja `To Top`.

### Pelaajakortin koko tilanne ja ahtaan paneelin rastirivit (Tommin ehdotus ja valinnat 4.10.2026)

Ehdotus pelisession aikana: *"ottelutilanteen lukeminen vaatii pelaajakorttien tietojen
yhdistelyä"*. Kumpikin kortti näyttää nyt koko tilanteen oman pelaajansa näkökulmasta, oma
luku ensin (`cardScoreText`). Muoto seuraa `Score`-asetusta eikä tuonut uutta valintaa.
Away-asetuksella kortti sanoo `5-away 3-away`, sivun pisteillä `2-4/7`, koska silloin pituutta
ei voi lukea luvuista (*"pituus on tärkeä tieto, jos ei ole away-luvut käytössä"*). Mockup
chatissa ensin, kolme versiota (nyt, A away-pari, B pisteet ja pituus), ja Tommi kuittasi
tulkinnan. Lokerosarake näyttää yhä vain oman luvun (`scoreText`), koska kapeaan sarakkeeseen
pari ei mahdu. Rahapelissä ja puuttuvalla paneelilla kortti putoaa vanhaan muotoon.

Samalla: *"nyt pixelillä ahdas tilanne, ehdotan että checkboxit siirtyvät Accept ja
Decline-nappulariveille"*, tarkennus *"rastit vain ahtaassa tilassa toiminnon kanssa samalla
rivillä"*. Kaappaus kesken session (klo 19.27, Slower Sevens #4585, vastustaja B tuplasi)
näytti `Skip Game`n ja `Mark position`in vastustajan kortin alareunan päällä, koska `Accept`,
`Decline` ja kaksi ruuturiviä veivät neljä riviä. Ahtaassa paneelissa kukin ruutu on nyt oman
nappinsa oikealla puolella selitteellä `Verify` (`board_verify_short`), koska napin nimi on
jo rivillä. Väljä paneeli ja pystylauta ovat ennallaan. TalkBackin puu lukee ruudun yhä
koko nimellä (`Verify Accept`, `Verify Decline`).

**Todennettu 4.10.2026 klo 19.46–19.48 kuorella Pixel 8a:lla** (top
`sessio-30-9-aamu/0002`, lauta `sessio-4-10-ilta/0125`, airlock 503 PC:ltä, koska Pixelissä ei
ole curlia). Away-asetuksella kortit `<vastustaja B> 4-away 5-away` ja `tommih 5-away 4-away`,
rastit nappiriveillä, `Skip Game` irti kortista (`raakasivut/sessio-4-10-ilta/kuori/k2-vaaka-tarjous.png`).
Sivun pisteillä `3-2/7` ja `2-3/7` (`k3-vaaka-site.png`). Pystylaudalla kortti mahtui
nimen riville (`tb-lauta-tarjous.png`). Asetus palautettiin away-muotoon kaappauksen jälkeen.
Tabletilla ei kaapattu.

### Pipit muurille kolmelle riville, tilanne kortin omalle riville (Tommin päätös 5.10.2026)

Tommi katsoi vanhoja kauppakuvia: *"pelaajakortin ottelutieto ei sovi nimen kanssa aina, joten
siirrä se omalle riville ja pips-tieto keskipaneeliin kolmelle riville seuraavaan malliin
(pilkut rivien erottomina) Pips, 126, (+10)"*. Kuvissa pitkä nimi katkesi pisteisiin
(*Tempestuous ...*), koska ahtaassa paneelissa tilanne oli 16.9.2026 alkaen nimen rivillä.
Keskipaneeli tulkittiin monivalinnalla laudan muuriksi: vastustajan pipit ylhäällä, omat
alhaalla. Tommi kuittasi tulkinnan ennen toteutusta.

**Kortti on nyt aina nimi ja tilanne kahdella rivillä** kaikissa asetteluissa. `compact`- ja
`portrait`-parametrit poistuivat [PlayerPanelView]ista, koska niiden ainoa tehtävä oli tilanteen
paikka ja pippien yläpuolinen väli.

**Pipit ovat muurin ulkopäissä (`BarPips`)** kun sivupaneeli on ruudulla (`fits`). Ilman
paneelia ne pysyvät lokerosarakkeessa, koska muurin pää kuuluu silloin omistetulle kuutiolle.
Paneelin kanssa kuutio on kortissa tai kaistalla, joten pää on vapaa. Muuri kantoi pipit kerran
ennenkin numerorivin muurin välissä 15.8.–14.9.2026, ja silloin `Pips 131 (+5)` ei mahtunut
yhdelle riville tabletillakaan. Kolme riviä ratkaisee juuri sen, koska leveintä riviä on viisi
merkkiä. Kapealla muurilla koko pienenee leveimmän rivin mukaan, joten luku ei leikkaudu.
Muurille lyöty nappula piirtyy pippien päälle, ja pino ulottuu pippeihin vasta neljästä
nappulasta.

**Ensimmäisen kaappauksen jälkeen kaksi korjausta Tommin havainnoista:** *"tabletin
keskipalkin pipseihin voi käyttää isompaa fonttia"*, joten lähtökoko on `bodyLarge` eikä
pistenumeroiden `labelSmall`. Toinen havainto oli *"nappilokeron napit ovat liian kiinni ylä- ja
alareunoissa (osa kuvasta jää reunan alle)"*, ja kaappauksessa ulos kannettujen uloin viiva
piirtyi lokeron reunaviivan alle ja kiekko oli kiinni sisäreunassa. Lokero sai 4 dp:n
pystysuuntaisen sisävaran (`OFF_TRAY_INSET`). Väri vaihtui Pixelin kaappauksen jälkeen
oranssista pistenumeroiden väriin, koska oranssi katosi vaalean puulaudan kehykseen.

**Todennettu 5.10.2026 klo 18.35–18.37 kuorella** (`sessio-21-9-kuori2`, airlock 503, ottelu
The Marathon #4303): tabletti vaakana Deco-laudalla, Pixel 8a pystyssä ja vaakana puulaudalla.
Kortit `<vastustaja C>` / `16-away 7-away` ja `tommih` / `7-away 16-away`, muurilla `Pips 36 (+1)`
ylhäällä ja `Pips 35 (-1)` alhaalla. Pixelin pystylaudalla koko pienenee ja mahtuu. Kaappaukset
ovat kansiossa `raakasivut/sessio-5-10-wood-talkback/` (`p6`, `p7` ja `p8`). Repoon niitä ei
viety, koska vastustajan nimi on oikea. Muurille lyötyä nappulaa ei ollut kuoren laudalla,
eikä lokerosarakkeen asettelua ilman paneelia ajettu.

### Painetun pisteen numero laudan reunassa (Tommin tavoite ja valinnat 4.10.2026)

**Tavoite:** *"pixelillä nappuloihin osumista halusin helpottaa"*. Ohilyönti on osumaton napautus,
koska *"sormi peittää sarakkeen"* (`docs/TOINEN-ASIAKAS.md` › JustGammonin nappulan valinta).
Kosketusalue oli jo koko sarake, joten puuttui vastaus sormen ulkopuolella.

**Valinta:** neljästä mockupista (ei muutosta, numero syttyy reunassa, kupla sormen yllä,
kaksivaiheinen valinta ilman haamua) Tommi valitsi numeron. Haamunappula JustGammonin tapaan
olisi vaatinut kohteen tietämistä, ja sen ratkaisee sivusto eikä lauta (`PointWedge`).

**Kytkin:** `Show which point I'm pressing` asetusten Playing-ryhmässä, siirtonuolten jälkeen.
Oletus on pois kaikilla laitteilla eikä ruudun koon mukaan. Tommi valitsi ensin oletuksen puhelimen
mukaan, ja tarkensi heti: *"pelaaja itse päättää, pieni näyttö on lähes sokealle todella
tulkinnanvarainen"*. Kuudes arvo pätee siis ilman poikkeusta.

**Toiminta:** painettu numero kasvaa 1,6-kertaiseksi ja lihavoituu. Se on oranssi (`Palette.Accent`)
kun pisteeltä voi siirtää, ja yliviivattu kun ei voi. Numero viipyy 700 ms irrotuksen jälkeen,
koska nopea napautus on juuri se joka jää epävarmaksi. Sormen liukuminen sarakkeen ulkopuolelle
peruu merkin heti.

**Kaksi vikaa jotka vain laite näytti** (Pixel 8a pystyssä, kuori 4.10.2026: 30.9. top ja
4.10. siirtovuoron sivu ottelutunnus vaihdettuna):

1. Fonttikoon kasvu kasvatti numerorivin korkeutta, ja koko lauta hyppäsi painalluksen ajaksi
   noin 12 px. Kasvu tehdään nyt piirrossa (`graphicsLayer`), joten asettelu ei muutu.
2. Siirrettävän pisteen numero katosi heti irrotuksessa. `clickable` kuluttaa irrotuksen, ja
   `waitForUpOrCancellation` lukee kulutetun irrotuksen peruutukseksi. Linkittömällä pisteellä
   viipymä toimi, koska kuluttajaa ei ollut. Tarkkailija odottaa nyt itse, että sormet nousevat.

Todennettu korjausten jälkeen: pohjassa siirrettävä 8 ja ylärivin 20 oransseina, linkitön 9
yliviivattuna, 8 viipyy siirron jälkeen ja on poissa 1,5 s kuluttua, ja numerorivi pysyy
paikallaan. Tabletin kaappaus puuttuu.

**Ohi-ääni (Tommin tilaus 6.10.2026):** *"ohi painamisesta sopiva äänimerkki"*. Tulkinta
kuitattiin monivalinnassa: ääni soi kun sormi nousee sarakkeen sisällä pisteeltä jolla ei ole
siirtolinkkiä, eli juuri siltä jolle numero piirtyy yliviivattuna. Raahaus pois sarakkeesta ei
soi, eikä vastustajan vuoro, koska silloin jokainen napautus olisi ohi. Ehtoina ovat tämä kytkin
ja `Game sounds`, ei teema (kaanonin laajennus, `docs/AVOIMET.md` › Äänet). Kutsu kulkee
`BoardTouch.onMiss`illa ja `observePress`in irrotuksella; ääni `WoodSound.MISS`,
`docs/ASETUKSET.md` › Wood-teema. Ääni valittiin kolmesta tabletin kaiuttimesta kuunneltuna:
kaksi laskevaa naputusta. Asetusruutu kaapattu tabletilla 6.10.2026 klo 19.46
(`docs/kuvat/game-sounds-asetukset.png`): kytkin käytössä Deco-teemassa ja pois, Wood-kortti
lyhyellä nimellä.

**Laudalla todennettu tabletilla 6.10.2026 klo 19.49 kuoriproxyn kautta** (Top Page
`sessio-5-10-ilta/0144`, lauta `sessio-6-10-iltapaiva/0058` ottelutunnus sovitettuna, airlock
503, `Game sounds` ja painalluskytkin päällä, Deco-teema). Ääni luettiin `dumpsys audio`n
soitintapahtumista: napautus tyhjään pisteeseen 10 klo 19:49:28,44 käynnisti sovelluksen
soittimen 28,58 ja piirsi yliviivatun 10:n tummalle laatalle (`docs/kuvat/ohi-napautus-tabletti.png`).
Raahaus pisteeltä 10 sivuun (52,16) ei käynnistänyt mitään. Siirrettävän pisteen 9 napautus
(54,53) kokosi askeleen 9→7 ja soitti 54,72, eli nappulan äänen; ohi-ääni ei voi soida siinä,
koska linkillinen piste antaa `miss`-kutsulle nullin. Siirto peruttiin `Undo Move`lla. Loki näytti
lisäksi kaksi käynnistystä 42,19 ja 46,82, jolloin Claude ei napauttanut mitään. Tommi kertoi
koskettaneensa tablettia kerran, joten toisen käynnistyksen syy on todentamatta. Tommin korvat eivät olleet todennuksessa mukana.

### Taustakuvio poistettiin (Tommin karsinta 6.10.2026)

**Tommin sana:** *"on aika kitkeä, Settings/Lists and screens ensimmäiset kaksi kytkintä ovat
liikaa, jätetään vain pictures below the lists."* Kytkimet olivat *Pattern behind the lists*
ja *Vermilion birds and a moon (light theme)*, molemmat oletuksena pois.

**Ominaisuus poistui kokonaan eikä vain kytkin** (Tommin valinta kahdesta). Pelkän kytkimen
poisto olisi jättänyt noin 800 riviä koodia, jota kukaan ei näe. Lähtivät `DgPattern.kt`,
`SkyThemeStore`, `DgSkyLayoutTest`, viisi merkkijonoa ja `dg_sky_theme` asetustiedoston
siirrosta. Ruutujen perusväri maalataan yhä yhdessä paikassa (`DgScreenBackground.kt`),
koska ruutujen `Scaffold`it ovat läpinäkyviä. Taustakuvien satunnaissiemen oli lainattu
taivaalta, ja se jäi nimellä `pictureSeed`: kuvat arvotaan yhä joka ruudun avauksessa.

**Kuvien paikka** (Tommin valinta kahdesta, 6.10.2026): *Pictures below the lists* pysyy
*Lists and screens* -ryhmässä ensimmäisenä, ennen rarity-värejä ja odotusilmaisinta. Toinen
vaihtoehto oli oma ryhmä Themen alla aina näkyvissä. Paikka syntyi poistosta, koska
*Background*-ryhmä oli kuvien edellä.

Lukupinta (`DgReadingSurface.kt`) jäi, vaikka sen alkuperäinen syy oli kuvio tekstin alla.
Sen poisto on eri päätös, jota ei kysytty.


### Chromebookin ikkuna ja testaajien toiveet (testaajapalaute 9.10.2026)

**Palaute tuli suljetun testin Google-ryhmään** kahdelta testaajalta. Molemmat olivat
Chromebookilla. Toinen ei saanut ikkunaa suuremmaksi kuin puhelimen kokoiseksi, ja toisen
kaappauksessa (1920×864, sovellus noin 880×395 keskellä) `Skip Game` ja `Mark position`
piirtyivät vastustajan kortin päälle. Tommi kuittasi korjaukset ja kaikki toiveet samana
päivänä.

**Chromebookin ikkuna.** Manifestin `screenOrientation="portrait"` lukitsee sovelluksen
ChromeOS:ssä puhelimen kokoiseen ikkunaan. Kiinteä suunta poistui manifestista, ja
pystylukko asetetaan nyt `MainActivity.onCreate`ssa ennen ensimmäistä ruutua. ChromeOS:llä
(`isChromeOs`, ARC:n ominaisuusnimi) sovellus ei pyydä suuntaa koskaan (`orientationFor`in
`freeWindow`), koska pyyntö muuttaisi ikkunan muotoa eikä käännä laitetta. Todentamatta:
Chromebookia ei ole, joten korjaus on päätelty eikä mitattu. Vahvistus tulee testaajalta.

**Päällekkäisyys.** `CenteredOverStack` sijoitti lisät negatiiviseen y:hyn, kun lisät,
ottelukortti ja sivun napit eivät mahtuneet lohkoon. Nyt ottelukortti tiivistyy ensin
(nimi yhdelle riville, kierros hännälle) ja vasta viimeisenä jää pois, koska se on lohkoista
ainoa joka ei ole teko. Todennettu tabletilla kuoren alla testaajan ikkunan kokoisena
(`wm size 1756x790`, `wm density 320`, eli 878×395 dp): lisät pysyvät kortin alla.

**Kolme uutta laitekytkintä**, kaikki testaajan toiveita:

- *Score* sai kolmannen arvon *Points won, next to the name* (`ScoreStyle.NAME`): kortti on
  `Hirsute Harry (3)` yhdellä rivillä ilman paria ja pituutta, koska pituus on jo ottelukortissa.
- *Show Mark position and the reminders* (`BoardExtrasStore`, oletus päällä) piilottaa
  `Mark position`, `Reminders` ja `Cube reminder` -linkit. Kirjoitetut muistutukset näkyvät
  yhä, ja `Skip Game` ja `Message` jäävät, koska ne ovat sivun tekoja.
- *Roll by tapping the opponent's dice* (`DiceRollStore`, oletus pois): vastustajan noppien
  napautus on `Roll Dice`, vain kun sivu tarjoaa sitä (`diceRollTapFor`).

Todennettu kuoriproxyn alla: kortit, piilotetut linkit ja vapautunut tila testaajan ikkunassa,
sekä napautus joka lähetti `?submit=Roll+Dice` (kuori vastasi tallennetulla sivulla). Kuvat
`raakasivut/testaajapalaute-9-10/`. Tabletin omat asetukset palautettiin ajon jälkeen.

**Rikottu sääntö samassa ajossa.** `docs/TESTAUS.md`:n sääntö 2 rikkoutui ennen kuorta:
`wm size` asetettiin elävällä tilillä, ja sen jälkeen otteluluettelossa tehtiin pyyhkäisyjä ja
yksi napautus. Lautaa ei avattu eikä mitään lähtenyt, ja koko palautettiin ennen kuorta.
