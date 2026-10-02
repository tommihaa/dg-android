# App Help: jaksot ja mistä kunkin väitteen totuus luetaan

Manuaali (`HelpScreen.kt`, merkkijonot `help_*` tiedostossa `strings.xml`) on osa toimintoa
eikä sen jälkikirjoitus. Tommin tilaus 15.9.2026: *"tämä pitää olla luotettavaa silloinkin
kun sovellus etenee jaettavaksi tai play-kauppaan"*. Saman päivän tarkistus löysi neljä
vanhentunutta jaksoa (värit, lauta, viestit, lajittelu) ja kokonaisia puuttuvia aiheita,
koska päivitys oli muistin varassa. Siksi kaksi mekanismia:

1. **Portti `CLAUDE.md`:ssä**: käyttäjälle näkyvän toiminnon muutos päivittää manuaalin
   samassa commitissa. Tämä taulukko kertoo mikä jakso ja mistä totuus luetaan.
2. **`HelpConsistencyTest`** vartioi nimettävän osan: välilehdet, lajittelunapit, lautatyylit,
   laiteasetusten kytkimet ja keskeiset napit on mainittava manuaalissa samalla nimellä kuin
   ruudulla. Uusi kytkin ilman mainintaa kaataa testin.

Manuaali ei koskaan lupaa sellaista mitä koodi ei tee, eikä se mainitse sitä mikä on lipun
takana pois päältä. Sovelluslukko oli sellainen (`APP_LOCK_ENABLED = false`) 27.9.2026 asti,
jolloin siitä tuli laitekytkin (`AppLockStore`, oletus pois) ja se mainitaan `settings`-jaksossa.

## Jaksot

**FAQ-muoto 15.9.2026 illalla** (Tommi: *"aikamoinen wall of text, saisiko siitä FAQ version"*
ja *"erot DailyGammonin saittiin myös näkyviksi"*). Avaimet ovat `help_q_<avain>` (kysymys tai
väite) ja `help_a_<avain>` (vastaus), ryhmät `help_group_*`. Vastaukset ovat entiset kappaleet,
joten totuuslähteet ovat samat; taulukko on nyt avaimittain. Ruudulla kysymys on aina näkyvissä
ja vastaus aukeaa napautuksella, yksi kerrallaan, lukupinta 640 dp. Kysymykset on numeroitu
muotoon ryhmä.kohta, ryhmä kirjaimena, esimerkiksi B.7 (Tommin tarkennukset samana iltana:
*"numeroida, jotta niihin olisi helpompi viitata"*, *"siten, että numeron poistuessa kaikki ei
mene uusiksi"* ja *"ryhmät ehkä numeroinnin sijaan aakkosilla"*). Poisto siirtää vain saman
ryhmän loppupään, ja kirjain erottaa ryhmän kohdasta.

**Ryhmät lukitaan ennen julkistusta, kohdat eivät** (Tommin päätös 15.9.2026 illalla: *"ennen
julkistusta ryhmistä tehdään pysyviä, mutta kysymykset voi elää ja yhdentyä"*). Kirjain A–H
on siis pysyvä viittaus kaupan version jälkeen, ja uusi ryhmä saa seuraavan vapaan kirjaimen
listan loppuun eikä väliin. Kohdan numero voi muuttua kun kysymyksiä lisätään, poistetaan
tai yhdistetään, joten kohtaan viitataan ryhmän ja kysymystekstin kautta, ei pelkällä
numerolla. Lukitus kirjataan `docs/JULKAISU.md`:hen sillä hetkellä kun se tehdään. Taulukon avain on pysyvä,
numero ei, joten pysyvä viittaus on avain ja keskustelun viittaus numero.

| Ryhmä | Avain | Väitteet | Totuus luetaan |
|---|---|---|---|
| `about` | `about` | Kirjautuu omalla tilillä, lukee sivuston sivut, sama pyyntö kuin selaimen; tili luodaan sivustolla, salasana sähköpostilla (27.9.2026); kirjautumisruudun linkki sivuston Create Account -sivulle poistettu 1.10.2026 (Play Consolen Data safety, `docs/KAUPPA.md`) | `core-net`, `CLAUDE.md` › Mitä tämä on, `LoginScreen.kt` `NoAccountNote`, `docs/KOHDE.md` › Sivuston toiminnot joita sovellus ei tee; testi `tilinLuontiSanotaan` |
| `about` | `encryption` | Sivusto ei tarjoa HTTPS:ää: kirjautuminen, siirrot ja viestit kulkevat selkokielisinä, sama riski kuin selaimella; salasana laitteella salattuna | `network_security_config.xml`, `docs/KOHDE.md`, `AesGcmCipher`; testi `salaamatonYhteysSanotaan` (Tommin tilaus 21.9.2026) |
| `about` | `site_only` | Sivustolle jäävät: Create Account (mainittu lihavoimatta, sovellus ei avaa sivua 1.10.2026 alkaen), Change Password ja Public Profile selaimeen, Mini-korttia ei voi valita ja Mini-laudalla ruutu neuvoo vaihtamaan, Background Color vain selaimeen, Active Game ei sovelluksessa (vaihtoehtona `.mat` tai `.sgf`); Donations ei tarkoituksella (29.9.2026) | `docs/KOHDE.md` › Sivuston toiminnot joita sovellus ei tee, `board_mini_scheme`, `settings_mini_card`; testi `sivustolleJaavatNimelta` |
| `differences` | `d_messages` | Viesti talteen ennen näyttöä; arkisto vain laitteella, kehittäjä ei näe sitä | `MessagesViewModel.readItem`, `MessageArchive` (ei verkkoa) |
| `differences` | `d_session` | Katko ei pudota istuntoa | `core-net` istunnon uusiminen |
| `differences` | `d_sort` | Sort by -napit, kolmas napautus palauttaa sivuston järjestyksen, valinta laitteella | `MatchOrder.next`, `MatchOrderStore` |
| `differences` | `d_local` | Napautukset laitteella Submit Moveen asti, Undo | `LocalComposition.kt`, `BoardViewModel.kt` |
| `differences` | `d_place` | Pakolliset ja ahne asettavat mutta eivät lähetä | `ForcedStepsStore`, `GreedyBearoffStore` |
| `differences` | `d_tournament` | Turnausrivi nimeää vastustajan ja kierroksen luettelosta ja muistista; Refresh hakee turnaussivut ja tuntemattomien vastustajien profiilit (25.9.2026) | `OwnTournaments.activeGameLine`, `OwnTournamentsViewModel.refreshExtras`, `MatchMemory` |
| `differences` | `d_archive` | Chat molemmin puolin talteen, suodatin, Export, Back up, Import; kehittäjä ei pääse arkistoon | `board_chat_*`, `MessageFilterStore`, `MessagesViewModel.exportJson` ja `importJson`, `BackupStore` |
| `differences` | `d_accounts` | Tili näkee vain omat viestinsä; Export ja Back up vain oman tilin; Sign out säilyttää kaikki; fraasit, muistutukset ja laiteasetukset yhteiset | `MessageDao` (`account`-rajaus), `MessagesViewModel.exportJson`, `MainActivity` (`claimUnowned`), `docs/AVOIMET.md` › Useampi tili samalla laitteella |
| `differences` | `d_reminders` | Muistutukset laitteella pelin loppuun, kuutiomuistutus sytyttää silmät ja kasvattaa kuution, joka jää isoksi purppurakehyksin (27.9.2026, Pick me -lippu pois) | `ReminderBook`, `DgBoard.kt` robotti, `ReminderPulse.kt` `CUBE_GROWTH` |
| `differences` | `d_marks` | Mark position muistutusten vieressä; peli, siirtonumero ja kommentti (kenttä `Comment (optional)`, sana on analyysiohjelmien, Tommin päätös 17.9.2026); säilyy poistoon asti; lista Matches-välilehdellä nimen alla otteluittain; ei lähetystä; profiilin päättyneellä ottelulla napit Share .mat ja Share .sgf (napit ja nimet 27.9.2026), merkki tallentaa aseman ja `.sgf`:n kommentti on merkityn siirron solmussa, asematon tai osumaton merkki pelin alussa, vain GNU näyttää kommentin, kohdistettu merkki saa `DO[]`:n ja GNU:n *Go › Next marked move* hyppää merkistä toiseen (Tommin päätös 30.9.2026, `docs/VERTAILU-SAGE-XG.md`), sivuston siirtonumero ei ole pelin siirtonumero; Share on laitteen jakovalikko ja Save to Drive vie tiedoston Driveen, josta Drive for desktop tekee tavallisen tiedoston koneelle; nackgammon ja double repeat eivät kirjoitu `.sgf`:ksi, DR tunnistetaan turnaussivun `Game`-ehdosta myös ilman tuplausta (Tommin päätös 27.9.2026), ystävyysottelu kirjoitetaan Crawfordilla | `MarkBook`, `BoardScreen.kt` `MarkStrip`, `MarksScreen.kt`, `TopScreen.kt` `MarksLine`, `PageScreen.kt` `MatchRow`, `SgfExport`, `CheckerPosition`, `MatGame.setup`, `EventPage.doubleRepeat` (mittaus 15.9.2026 ja 16.9.2026 `docs/AVOIMET.md`), `MatchExportShare.kt` ja Drive-reitti `docs/UI.md` (todennettu 18.9.2026) |
| `differences` | `d_look` | Monte Carlo -tyylit värittävät roolin mukaan, robotti kuutiossa | `BoardLook.kt`, `DgBoard.kt` |
| `differences` | `d_confirm` | Join, Sign up ja postaus vahvistetaan, Double ja Accept seuraavat sivun asetusta, kuutio kysyy aina (26.9.2026); Confirm Beaver kysyy ennen Beaver!ia ja Accept Beaveria (27.9.2026) | `lounge_*_confirm_*`, `discussion_confirm_*`, `CubeAction` |
| `differences` | `d_unconfirmed` | Nothing was confirmed keskikaistalla (dialogi pois 26.9.2026), ei automaattista toistoa | `board_unconfirmed_*`, `BoardViewModel.kt` |
| `tabs` | `tabs` | Viisi välilehteä; Top Pagella, Loungella ja Forumilla `Refresh` ja `Settings` parina (24.9.2026); Infon hakukenttä hakee App Helpistä ja sivuston Helpistä, jälkimmäinen noudetaan ensimmäisellä merkillä; Lounge on neljä taittuvaa jaksoa (pelaajalista, halli, tarjoukset, ilmoittautumiset); Info sisältää katkohistorian; Links näyttää vain sivuston omat osastot (Donations-rivit poistettu 29.9.2026); profiili ja turnaus porautumisina; viesti profiililta tallentuu; toisen pelaajan profiilin versus-rivi kantaa saldon katsojan silmin | `DgTabs.kt`, yläpalkkien `actions` (`TopScreen.kt`, `LoungeScreen.kt`, `DiscussionScreen.kt`), `InfoScreen.kt` (`InfoSection.Drops`), `InfoSearch.kt`, `PageScreen.kt`, `page_message_sent`, `PageViewModel.versus`, `page_versus_you_record` |
| `matches` | `order` | Määräaikajärjestys; Sort by -napit ja kolmen napautuksen kierto; nuoli; valinta säilyy | `MatchOrder.next`, `TopScreen.kt` rivit 537–578, `MatchOrderStore` |
| `matches` | `open` | Avaus ei kuluta vuoroa; paluu teon jälkeen hakee luettelon, katsomiskäynti ei; kutsuun vastaaminen hakee luettelon (27.9.2026); paluu toisesta sovelluksesta luettelon ollessa auki hakee; Refresh on käyttäjän | `BoardViewModel.actedOnSite`, `MainActivity.kt` › leaveBoard, `InvitationActionUiState.Answered` ja `RefreshOnForeground`, `TopViewModel.onForeground`, `docs/UI.md` poikkeukset 16.9.2026 ja 18.9.2026 |
| `matches` | `tournaments` | Kierros ja vastustaja luettelosta tai muistista; voitot vain ilman kierrosta; ikärivi; Refresh lukee profiilin; ensiavaus lukee profiilin kerran; linkittömän rivin odotus ja vastustajan väri Refreshistä | `OwnTournaments.kt`, `PlayerTournamentRowView.showWins`, `MatchMemory`, `EventPage.matchesBeforeOpponent`, `TournamentWaitStore` |
| `board` | `move` | Sivuston nimet; kokoaminen laitteella ja Submit Move; Undo; siirtonuolet koottavasta siirrosta kun asetus päällä, tuplat ketjuna ja sovelluksen asettamat mukana, poistuvat lähetyksessä ja Undossa (29.9.2026); vastustajan edellinen siirto omalla kytkimellä vastustajan värillä vastustajan noppien rinnalla ja poistuu omassa heitossa (30.9.2026 illalla), lisähaku Review Game -sivulta (29.9.2026 illalla), eikä nuolia piirretä jos sivu on laudasta jäljessä (`MatchLogPage.reaches`, 30.9.2026); noppanapautus lähettää kun asetus päällä; ei-todennettu asema askel kerrallaan | `BoardViewModel.kt`, `LocalComposition.kt` (`arrows`), `MoveArrows.kt`, `MoveArrowsStore`, `OpponentMove.kt`, `DiceSubmitStore` |
| `board` | `forced` | Pakolliset ja ahne asettavat, eivät lähetä | `ForcedStepsStore`, `GreedyBearoffStore` |
| `board` | `double` | Kuution kolme vaihetta asetus päällä, ilman ruutua nappi lähettää suoraan ja kuution napautus kysyy (26.9.2026); money gamen Beaver! ja Accept Beaver, Beaver!lla ei sivun ruutua eikä asetusta, laitteen Confirm Beaver ja sen oletus sivun tuplausvahvistuksesta (27.9.2026, `BeaverConfirmStore`, `docs/KOHDE.md` › Money game); `Verify Decline` sivun omana ruutuna, kaksi vaihetta (korjattu 26.9.2026) | `CubeAction`, `verifyBoxesFor` (`BoardScreen.kt`), `BoardForm.verifyDecline` |
| `board` | `unconfirmed` | Syy ja *Nothing was confirmed* keskikaistan ensimmäisenä tilarivinä, ahtaalla nappien alla (`stripNotes`, 26.9.2026); asema on viimeinen ladattu; `Refresh` keskellä; jokainen katko Connection drops -listaan Info-välilehdelle (hetki, ottelu, nappi, virheen nimi), sata viimeistä; avaamaton yhteys avataan hiljaa uudelleen enintään kolmesti ennen katkoa (26.9.2026) | `board_unconfirmed_*`, `BoardViewModel.afterPress`, `DropLog`, `DropsScreen.kt`, `DgClient.kt` `connectAttempts` |
| `board` | `gestures` | Veto alas hakee; Refresh sama; Back; palkit piiloon vain kun ne veisivät laudan kokoa tai sivulla paneelin leveyttä; reunapyyhkäisy; koko näytön kytkin piilottaa ne kaikilta ruuduilta (30.9.2026) | `BoardScreen.pullDownToRefresh`, `DgBoard.barsAreFree`, `Immersive.kt`, `FullScreenStore` |
| `board` | `beside` | Sivupaneeli: linkit, Message ja Reply, chat siirron mukana ja talteen, muistutukset, kuutiomuistutus; pystylaudalla sisältö laudan ympärillä (ottelu ja vastustaja yllä, oma kortti, napit ja muistutukset alla), vahvistusruutu napin oikealla, paitsi Roll Dice ja Double samalla rivillä ja Verify Double Doublen alla vasemmassa reunassa ja Skip Game Roll Dicen alla (24.9.2026), vasenkätisyys ei vaikuta; nimen harvinaisuusväri ratingin mukaan kortissa ja turnauskaaviossa (26.9.2026); portaat 1600, 1800, 2000 ja 2200 kiinteinä, pinkki ja sijarajat poistettu (29.9.2026 illalla) | `BoardScreen.kt` sivupaneeli ja `panelBelow`, `board_chat_*`, `board_reminder_*`, `Notes.kt`, `Rarity.kt`, `PageScreen.kt` `BracketCell` |
| `board` | `skip` | Skip Game ohittaa jonossa, ottelu koskematon | `SkipGameAction` |
| `board` | `look` | Teema (Plain, Deco: sovelluksen värit, Deco-lauta ja deco-viimeistely kerralla, Plain palauttaa edelliset; Deco seuraa laitteen vaaleaa ja tummaa tilaa, vaaleassa lauta vaalenee; vaaleassa tilassa lautaruudun ympäristö on vaalea molemmissa teemoissa, `panelLookFor`, 27.9.2026); kahdeksan lautatyyliä ja värit (puulaudat Maple, Walnut, Oak and leather ja Olive syyllä ja lokeron puisella paneelilla 30.9.2026, `DgBoard.Wood`, `WoodGrain.kt`), `A different board each match` -kytkin (ottelulla oma lauta rastitetuista, kortit rasteiksi, `BoardShuffle.kt`, 27.9.2026), variantin paneeli lokeron puuta tummassa tilassa; noppa- ja pistetyyli; odotuksen ilmaisin (kaari, ouroboros, kuutio, ääretön-käärme, tiimalasi tai satunnainen joka odotuksella; Art deco and clockwork -viimeistely omilla väreillään; nappilokero ja tyhjän ruudun keskus); robotti ja CC BY | `BoardLook.kt` (`PanelLook`), `BoardStyleStore`, `WoodGrain.kt`, `DiceStyleStore`, `ScoreStyleStore`, `BusyStyleStore`, `BusyIndicator`, `DgBoard.kt` |
| `messages` | `take` | Ei taustahakua, ei ajastinta, ei ilmoituksia; Take an item; tallennus ennen näyttöä | `MessagesViewModel.readItem` |
| `messages` | `queue` | Ilmoitus ei tallennu, lauta ei tallennu ja pyyhkii luettelon ilmoituksen (viestit luettu), kutsu jää jonoon ja siihen vastataan; tyhjä jono vastaa otteluluettelolla eikä kuluta, nappi katoaa | `InvitationParser`, `MessagesScreen.kt`, `QueueUiState.Empty`, `TopViewModel.clearMessageNotice` |
| `messages` | `reply` | Vastauslaatikko, vastaus pelaajalle, fraasit, lainaus; oma viesti näyttää kohteen; päättymissivun `Reply to` pikaviestinä ottelun nimi lainattuna, malli DGText2Area (27.9.2026) | `MessagesScreen.kt` (`MessageRow`, `messages_to`), `PhraseStore`, `BoardScreen.kt` (`MatchOverContent`), `BoardViewModel.sendMatchReply` |
| `messages` | `archive` | Pelaajalista uusin ensin, haettu viesti kärkeen New-merkillä kokonaisena ja Reply listalla, kenttä auki haun jälkeen; rivi avaa keskustelun, Back ja All players palaavat, avattu keskustelu muistetaan; keskustelussa päiväotsikot, viestit kuplina ja omat oikealla (`isOwn`), vastaus kohteensa alla omalla päivällään, Match ja Quick nimen perässä; Export; Back up kerran vuorokaudessa Inboxin ollessa auki ja Save now; tiedostossa viestit, muistutukset ja fraasit; Import lisää puuttuvat eikä muuta olemassa olevaa; rarity päällä nimi ratingin väri nimellä ja tuntematon tavallinen, pois päältä määrä tavallinen ja nimi tunnistusväri (26.9.2026) | `MessagesScreen.kt` (`inboxNameColor`, `conversationList`, `ConversationRow`, `InboxDayHeading`, `shortSourceLabel`), `MessagesViewModel.fresh`, `MessageFilterStore`, `BackupStatus.isDue`, `MainActivity` backup-lohko, `MessagesViewModel.importJson`, `ArchiveFile` |
| `messages` | `one_device` | Arkisto sillä laitteella jolla viesti otetaan; selaimessa tai muussa sovelluksessa luettu viesti ei tule arkistoon; kaksi laitetta yhdistetään Importilla (1.10.2026); siirrot muualla eivät vie mitään | `docs/ARVOT.md` › Kenelle, `move_device` |
| `messages` | `move_device` | Export ja Back up kirjoittavat tiedoston; Import uudella laitteella tuo viestit, muistutukset ja fraasit ja jättää laitteen omat ennalleen; kaksi laitetta yhdistetään samoin molempiin suuntiin (21.9.2026, kirje Peterille); merkityt asemat eivät ole tiedostossa; asetukset siirtyvät erikseen Settingsin Save to file -tiedostona (28.9.2026; Copy settings ja Paste settings poistuivat vastauksesta 29.9.2026, koska napit poistettiin samana päivänä) | `MessagesViewModel.exportJson` ja `importJson`, `MarkBook` (ei viennissä); testi `tuontiLuvataanJaSenSaantoSanotaan` |
| `lounge` | `join` | Neljä jaksoa otsikon takana (+ kiinni, − auki), taitto muistetaan; Hall kantaa viime haun luvun (? ennen hakua), Players ei kanna lukua; avaus hakee kerran, Refresh hakee loungen ja auki olevat; Join ja Sign up vahvistetaan; Join lukee loungen uudelleen ja jättää lähettämättä jos tarjous ei ole enää samalla linkillä (27.9.2026, `JoinUiState.Changed`); Cancel sign-up | `LoungeScreen.kt` (`FoldHeading`, `playersItems`, `hallItems`), `LoungeFoldStore`, `LoungeViewModel.refresh`, `lounge_*_confirm_*` |
| `lounge` | `forum` | New ja Add a Comment vahvistetaan, postausta ei voi muokata; rungon linkit avautuvat (web-osoite selaimeen, sivuston polku sovellukseen); Hide joka rivillä molemmilla palstoilla, piilotus on sivuston puolella; Show Hidden Threads näyttää piilotetut muiden joukossa ilman Hidea ja Hide Hidden Threads palaa; Unhidea ei ole | `DiscussionScreen.kt` (`PostView`, `ThreadRow`), `discussion_confirm_*`, `ForumPost.links`, `ForumThread.hidePath`, `ForumIndex.showsHidden`, `docs/KOHDE.md` › piilotettujen näkymä |
| `lounge` | `settings` | Kaksi segmenttiä, This device auki oletuksena; laiteosio Themen alla neljässä taittuvassa ryhmässä Board, Playing, Lists and screens ja Screen and lock, kaikki auki kunnes suljetaan, taitto muistetaan (`SettingsFoldStore`, 27.9.2026); laiteosion kytkimet nimeltä (myös `Turn the board with the device`, `BoardRotationStore`, ja `Colour names by rating and the Inbox count by size`, `RarityStore`, oletus päällä 26.9.2026; `Ask for fingerprint or PIN when the app starts`, `AppLockStore`, oletus pois ja harmaa ilman laitteen omaa suojausta 27.9.2026; `Hide the status and navigation bars`, `FullScreenStore`, oletus pois 30.9.2026; puhelimella kirjoittaminen kääntää jokaisen ruudun pystyyn kytkimistä riippumatta ja kirjautumisruutu on aina pystyssä, 1.10.2026), tallentuvat heti eikä mitään lähde sivustolle; viisi valintaryhmää kortteina saman pelitilanteen esikatselulla, Theme- ja Board look -korteissa suurennuslasi ja Use this; Handedness-kortit toistensa peilikuvina tilin laudan suunnasta riippumatta, Left hand kotikenttä vasemmalla, ja selite nimeää `Home boards on left side` -kentän (27.9.2026); DailyGammon-lomake haetaan avattaessa, Board Scheme kortteina lomakkeen tallentamattomasta tilasta sovelluksen omalla pinnalla, Background Color listana jonka alla rivi että se vaikuttaa vain selaimeen (25.9.2026) (Mini-korttia ei voi valita ja siinä on oma lyhyt viesti, 25.9.2026; suurennuksen Use this vain täyttää lomakkeen), lähtee vain Update Preferences -napista kokonaisena ja luetaan takaisin, nappi saa kun lomake poikkeaa luetusta Cube reminderin purppurakehyksen (kuusi pulssia, sitten pysyvä, `reminderAttention`); salasana ja Public Profile jäävät sivustolle, lomakkeen yllä rivi joka avaa `/bg/profile`-sivun selaimessa; laiteosion alla Save to file ja Open file (28.9.2026): merkkijono tiedostona Androidin tiedostoikkunan kautta (Drive mukaan lukien), leikepöydän Copy settings ja Paste settings poistettu 29.9.2026, tuonti esikatselee muuttuvat valinnat ja kirjoittaa vasta vahvistuksesta, mukana ottelujärjestys ja Inboxin pelaajasuodatin, ei tunnuksia, lukkoa, viestejä, pikaviestejä eikä kuvakansiota (`SettingsTransfer`) | `MainActivity` (`orientationFor`, `phoneTyping`), `SettingsScreen` (segmenttirivi, `DeviceSection`, `SubmitArea`, `SiteOnlyNote`), `SettingsPreviews.kt`, `SettingsViewModel.open`, `docs/ASETUKSET.md` luvut 3 ja 5, `docs/UI.md` › Asetusruutu kahdeksi segmentiksi |
| `never` | `never` | Ei lähetystä ilman painallusta; ei taustatyötä; lukee vain avatut sivut; säilyttää vain laitteella | `MessagesViewModel` (ei ajastusta), `SiteSettingsRefresher`, `HelpScreen.kt` otsakekommentti |

## Miten tämä pidetään ajan tasalla

- Uusi käyttäjälle näkyvä toiminto: lisää väite oikeaan vastaukseen tai uusi kysymys
  (`help_q_*` ja `help_a_*` sekä `HELP_GROUPS`-listaan `HelpScreen.kt`:ssä), ja rivi tähän
  taulukkoon. Jos toiminto on ero sivustoon, myös väite `differences`-ryhmään. Jos toiminto on nimetty nappi tai kytkin, lisää sen nimi myös
  `HelpConsistencyTest`iin, ellei se osu jo olemassa olevaan sääntöön (kaikki
  `settings_*_toggle` ja `settings_board_style_*` tarkistetaan automaattisesti).
- Poistettu toiminto: poista väite samassa commitissa. **Testi huomaa poistetun nimen
  29.9.2026 alkaen** (`lihavoidutNimetOvatRuudulla`): vastauksissa ruudun nimet lihavoidaan
  `<b>`-merkinnällä, ja jokaisen lihavoidun nimen on löydyttävä jostain muusta kuin
  `help_`-merkkijonosta. Poistettu nappi vie merkkijononsa mukanaan, joten testi kaatuu.
  Sivuston ja Androidin nimet ovat testin poikkeuslistassa, ja listan vanhentunut rivi kaataa
  testin sekin. Mitä testi ei näe: lihavoimaton maininta ja luvut (rarity-portaat 29.9.2026).
  Uusi nimi vastaukseen siis aina lihavoituna.
- Ennen jakelua tai kauppaa: lue manuaali laitteelta kerran läpi ruutu ruudulta, samaan
  tapaan kuin 15.9.2026, ja kirjaa löydökset tähän päivämäärällä.

## Tarkistushistoria

- 15.9.2026: koko manuaali luettu koodia vasten ja kirjoitettu uusiksi. Vanhentuneet: *Cream
  is you* (lautatyylejä kolme), *Playing on the board* (noppanapautus ja esipoiminta), *Messages*
  (Inbox-välilehti, kutsut, fraasit, vienti, varmuuskopio), *Sorting* (napit eivät sarakeotsikot).
  Harhaanjohtava: *One device* (selaimessa luettu viesti katoaa). Puuttui: välilehdet,
  asetukset, chat, muistutukset, Lounge, Forum, varmuuskopio.
- 15.9.2026 illalla: FAQ-muoto (8 ryhmää, 34 kysymystä), erot sivustoon omana ryhmänä 11
  väitteenä, lukupinta 640 dp. Sisältötarkistuksessa löytyi yksi liikaa luvattu asia: *"take
  the archive with you"* lupasi siirron, mutta tuontia ei ole. Korjattu `move_device`-vastaukseen
  ja kirjattu `docs/AVOIMET.md`:hen.
- 17.9.2026: kaikki muut kuin Help-merkkijonot luettu koodia vasten ennen julkista repoa
  (Tommi: *"näkymien tekstit pitää vielä käydä läpi ettei katteettomia lupauksia"*). 137
  pitkää merkkijonoa, 30 lupausta todennettu. Yksi katteeton: `lounge_signup_confirm_body`
  lupasi perumisen turnauksen alkuun asti, mutta mitattu on vain `Cancel Signup` rivillä
  ilmoittautumisen jälkeen (`docs/KOHDE.md` 3.9.2026). Lause sanoo nyt sen. Help-vastausten
  `never`- ja `always`-väitteet (7) osuvat yllä olevan taulukon riveihin.
- 29.9.2026: vastaukset jaettu lyhyiksi kappaleiksi ja ruudun nimet lihavoitu (Tommi: pitkiä
  kappaleita eikä korostuksia, valinta b). Sanavertailu vanhaa tekstiä vasten näytti vain
  välimerkit ja kolme korjausta. Vanhentuneita löytyi kaksi: `move_device` lupasi tänään
  poistetut Copy settings ja Paste settings -napit, ja `beside` antoi rarity-portaiksi 1950 ja
  2140, vaikka `Rarity.kt` käyttää arvoja 2000, top 132 ja top 32. Lisäksi napin nimi *Quote
  the message above in your reply* oli lyhentynyt vastauksessa. `lihavointiOnParillinenJaKokonainen`
  vartioi merkinnät.
