# Odottaa Tommia: raakasivut ja talteenotto

Mitä Claude ei voi tehdä itse, ja miten talteen otettu raakasivu käsitellään.

Siirretty `CLAUDE.md`:stä 10.8.2026. Sisältö on ennallaan, vain sijainti muuttui.
Hakemisto ja pyytämättä laukeavat portit ovat yhä `CLAUDE.md`:ssä.


## Odottaa Tommia, ei Claudea

Osion pyyntö on aina sama: sivua ei saa hakea puolesta, koska haku kuluttaa jonon. Ohje
sivujen tallentamiseen on `raakasivut/LUEMINUT.md`.

**Korjaus 8.8.2026:** tämä kappale sanoi että viestien jäsennintä ei ole eikä sitä saa
aloittaa, koska viestisivuja ei ole nähty HTML:nä ja kaksi sivua puuttuu. Kappaleen oma
osio kumoaa sen kolme riviä alempana (`Osio on tyhjä 4.8.2026 alkaen`), ja molemmat sivut
ovat fixtureina. Kielto oli siis voimassa neljä päivää ilman perustettaan, ja se koski
täsmälleen sitä työtä jonka sama osio nimeää seuraavaksi askeleeksi.

Kolmas osuma samasta vikamuodosta samana päivänä, ks. avoimien listan siirtomekanismikohta.
Yhteinen piirre kaikissa: **kielto tai este säilyi, kun sen perustelu ratkesi muualla.**
Ratkeaminen kirjataan sinne missä se mitataan, eikä mikään työvaihe pakota palaamaan siihen
kohtaan joka sen nojalla kieltää jotain.

1. ~~`raakasivut/nextgame_chat.html`~~ **saatu 3.8.2026 ja jäsennetty** (`ChatParser`,
   fixture `chat_thread.html`). Yksi rajaus jäi: kaappaus tehtiin `Ctrl+S`:llä, joten
   `<pre>`-lohkon sulkeutuminen ei ole todennettu. Sama sivu `Ctrl+U`:lla purkaisi sen,
   mutta se ei estä mitään, koska jäsennin kestää molemmat muodot.
2. `raakasivut/nextgame_telegram.html`, `/bg/nextgame` ilmoituksella.

3. ~~`raakasivut/nextgame_quickmessage.html`~~ **saatu ja jäsennetty 1.8.2026**
   (`InboxParser`, fixture `inbox_quick_message.html`). Kysymys otsikoiden erosta ratkesi
   samalla, ks. viestiosion taulukko. **Fixture uusittiin 4.8.2026 raaoista tavuista**,
   koska vanha oli laajennuksen saastuttama, ks. alempi kohta.

**Osio on tyhjä 4.8.2026 alkaen.** Kohta 2 saatiin toisen pelisession sivutuotteena
(`/bg/nextgame` tarjoili kaksi ilmoitusta ennen ensimmäistä lautaa), eikä sitä haettu
tarkoituksella. Aito sivu on 346 tavua, ja fixture `inbox_telegram.html` on siitä. Se korvaa
**kuvakaappauksesta käsin kirjoitetun synteettisen sivun** (`6dd8e38`), joka oli tässä
tiedostossa muualla nimetty kelpaamattomaksi jäsentimen lähteeksi.

Se nimeäminen osoittautui oikeaksi mitattuna: aidossa sivussa on `<PRE>`-lohkon sisällä
linkkejä keskellä virkettä, ja niiden takia jäsennin oli lukenut ilmoituksesta vain kaksi
ensimmäistä sanaa. Kuvakaappauksesta kirjoitettu sivu ei olisi koskaan tuonut sitä
piirrettä esiin, koska kirjoittaja ei keksi rakennetta jota ei näe.

**Tehty 9.8.2026, ja askel oli eri kuin tässä luki.** Tässä sanottiin seuraavaksi askeleeksi
`MessageParser` + `MessageSource`-lajittelu, mutta lajittelu oli jo `InboxParser.classify`ssä
eikä erillistä jäsennintä tarvittu. Puuttuva osa oli muunnos `InboxItem` → `Message`, ja este
sen edestä poistui samana päivänä: `Message` osaa nyt kantaa tuntemattoman otsikon
(`rawHeader` + `MessageSource.UNKNOWN`), joten muunnos ei enää joudu valitsemaan otsikon
pudottamisen ja tallentamatta jättämisen väliltä. Putki on nyt kokonainen jonosta ruudulle,
ks. viestiruudun oma osio `:app`-osuudessa.

Avoin kysymys jota ei saa arvata on yhä kirjattu `MessageSource.ANNOUNCEMENT`in kommenttiin,
eikä muunnos koske sitä: se siirtää lajin sellaisenaan eikä tulkitse otsikkoa uudelleen.

### Selaimen laajennus voi päätyä tallenteeseen sivuston rakenteena (4.8.2026)

Tämä on `Ctrl+S`- ja HAR-ansojen kolmas jäsen, ja hiljaisin niistä. Ne kaksi muuttavat
**muotoa** (tagit, rivinvaihdot, puuttuvat rungot), ja tämä muuttaa **sisältöä**: sivulle
ilmestyy elementtejä joita palvelin ei lähettänyt, eikä tallenteessa ole mitään mikä
kertoisi eron.

Mitattu tapaus: Tommilla on Bravessa laajennus joka vaihtaa DailyGammonin yksirivisen
viestikentän moniriviseksi. 1.8.2026 tallennetussa pikaviestisivussa oli siksi
`<textarea rows=10 cols=64>` esitäytettynä lainauksella, kun palvelin lähettää
`<INPUT TYPE=text MAXLENGTH=80>` tyhjänä. Sekä kanoni että `InboxParser` kirjoitettiin
laajennuksen DOMia vasten, ja jäsennin haki vastauskenttää valitsimella `textarea`: se olisi
palauttanut nullin juuri tuotannossa, eli siellä missä laajennusta ei ole.

**Proxy on tälle rakenteellisesti immuuni**, koska se lukee tavut ennen kuin sivulla ajetaan
riviäkään JS:ää. Se on kolmas erillinen syy suosia sitä, ja ainoa joka ei koske muotoa.

Fixturet auditoitiin samana päivänä: 21:stä **kaksi** oli selaimen kautta otettuja
(`chat_thread.html`, `inbox_quick_message.html`), loput ovat raakaa palvelin-HTML:ää
(isot tagit). Molemmat käsiteltiin. Pikaviesti uusittiin raaoista tavuista.
`chat_thread.html`in chat-rakenne sen sijaan **todennettiin aidoksi** proxyn tavuja vasten
(`<textarea name=chat rows=5 cols=80 wrap=physical>` on sivuston oma), ja siitä poistettiin
vain laajennuksen lisäämä `Reminder`-lohko.

Sääntö tästä eteenpäin: **selaimen kautta otettu tallenne ei kelpaa fixtureksi.** Jos
sellainen on pakko käyttää, sen rakenne todennetaan proxyn tavuja vasten ennen kuin
jäsennin nojaa siihen.

### Talteenotto ei saa nojata ihmisen ajoitukseen (3.8.2026)

`tyokalut/proxy.py` on lokittava välityspalvelin, ja se korvaa käsin tallentamisen. Syy on
mitattu eikä mukavuus: jokainen aiempi tapa vaati että tilanne tunnistetaan **sillä hetkellä
kun sivu on ruudulla**, ja juuri se on kaatunut. DevToolsin muistibudjetti pudottaa rungot,
ja `Save as...` epäonnistuu silloin hiljaa ilman virheilmoitusta.

Kolme asiaa jotka tämä ratkaisee ja joita mikään selaimen tallenne ei ratkaise:

- **Tavut ovat sivun omia tavuja**, ei DOMista serialisoituja. Todennettu tiivistevertailulla
  suoraan hakuun 3.8.2026. `Ctrl+S`:n vika on siis poissa rakenteellisesti.
- **Merkistökysymys voi ratketa**, koska selaimen tallenne on aina jo purettu merkkijono.
- **Kuluttavuussääntö pysyy voimassa**, koska proxy ei tee yhtään omaa pyyntöä vaan välittää
  vain selaimen omat. `/bg/nextgame` kuluu silloin kun sinne itse mennään, ei muulloin.

Pyyntöjen rungoista kirjataan **vain kenttien nimet, ei koskaan arvoja**: kenttänimet ovat
toistuvasti se mitä jäsennin tarvitsee, ja arvoissa olisi salasana. Käyttöohje on
`raakasivut/LUEMINUT.md`.

### Raakasivun käsittely on session ensimmäinen työ, ei erikseen pyydettävä

Jos `raakasivut/`-kansiossa on HTML-tiedosto jota `LUEMINUT.md`:n taulukko ei merkitse
haetuksi, se käsitellään ennen muuta työtä. Tommin ei tarvitse pyytää sitä erikseen:
tiedoston olemassaolo **on** pyyntö, ja hän on nähnyt vaivaa saadakseen tilanteen talteen
juuri sillä hetkellä kun se oli olemassa.

Käsittely on aina sama neljä vaihetta: lue sivu, kirjoita tai täydennä jäsennin, tee
anonymisoitu fixture `core-scrape/src/test/resources/fixtures/`iin ja päivitä `LUEMINUT.md`:n
tilasarake. Vain fixture menee versioon, raakasivu ei.

Kaksi ansaa jotka ovat jo kerran osuneet 1.8.2026:

- **Anonymisointi jää vajaaksi jos korvaa vain tutut linkkimuodot.** Ensimmäinen versio
  vaihtoi `/bg/move/`- ja `/bg/game/`-tunnisteet mutta jätti oikean pelitunnisteen
  linkkiin `/bg/nextgame?skip=<id>`. Tarkista lopputuloksesta hakemalla alkuperäisiä
  arvoja, älä luota korvauslistaan.
- **Kuva ei kelpaa jäsentimen lähteeksi.** PNG kertoo mitä ihminen näkee, ei `ALT`-koodia
  eikä linkkien muotoa. Kohdat 5, 6, 7 ja 11 olivat kuvina, ja niistä vain 11 ratkesi, kun
  sama tilanne osui myöhemmin kohdalle HTML:nä.

### Fixture jonka teksti on vaihdettu, ei vain anonymisoitu (27.8.2026)

`chat_thread_long_quote.html` on ensimmäinen fixture jossa **viestin sisältö on korvattu**
eikä pelkästään käyttäjänimiä vaihdettu. Syy on kaksiosainen: alkuperäinen koeviesti oli
liuska kopioitua artikkelitekstiä, jota ei ole syytä monistaa repoon, eikä sen sanoilla ole
mitään tekemistä sen kanssa mitä fixture todistaa.

**Mitä korvaus säilytti, koska juuri se on todiste.** Rivien lukumäärä (29 lainattua), kunkin
rivin merkkipituus merkilleen, pisin rivi tasan 80, tyhjät lainausrivit (8), rivien
loppuvälilyönnit (12) ja lainaamaton osa omalla paikallaan. Rivitys on palvelimen tekoa, ja
sitä ei voi lukita testiin ilman että pituudet ovat oikein.

**Sääntö tästä eteenpäin:** jos fixturen tekstiä vaihdetaan, korvaus tehdään ohjelmallisesti
pituus pituudelta ja se kirjataan tänne. Käsin kirjoitettu korvike ei pidä pituuksia, ja
silloin fixture väittää rivityksestä jotain muuta kuin sivusto teki.
