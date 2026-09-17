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
takana pois päältä (sovelluslukko, `APP_LOCK_ENABLED = false`). Jos lippu kääntyy, jakso
lisätään samassa commitissa.

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
| `about` | `about` | Kirjautuu omalla tilillä, lukee sivuston sivut, sama pyyntö kuin selaimen | `core-net`, `CLAUDE.md` › Mitä tämä on |
| `differences` | `d_messages` | Viesti talteen ennen näyttöä; arkisto vain laitteella, kehittäjä ei näe sitä | `MessagesViewModel.readItem`, `MessageArchive` (ei verkkoa) |
| `differences` | `d_session` | Katko ei pudota istuntoa | `core-net` istunnon uusiminen |
| `differences` | `d_sort` | Sort by -napit, kolmas napautus palauttaa sivuston järjestyksen, valinta laitteella | `MatchOrder.next`, `MatchOrderStore` |
| `differences` | `d_local` | Napautukset laitteella Submit Moveen asti, Undo | `LocalComposition.kt`, `BoardViewModel.kt` |
| `differences` | `d_place` | Pakolliset ja ahne asettavat mutta eivät lähetä | `ForcedStepsStore`, `GreedyBearoffStore` |
| `differences` | `d_tournament` | Turnausrivi nimeää vastustajan ja kierroksen luettelosta ja muistista, ei lisähakuja | `OwnTournaments.activeGameLine`, `MatchMemory` |
| `differences` | `d_archive` | Chat molemmin puolin talteen, suodatin, Export, Back up, Import; kehittäjä ei pääse arkistoon | `board_chat_*`, `MessageFilterStore`, `MessagesViewModel.exportJson` ja `importJson`, `BackupStore` |
| `differences` | `d_accounts` | Tili näkee vain omat viestinsä; Export ja Back up vain oman tilin; Sign out säilyttää kaikki; fraasit, muistutukset ja laiteasetukset yhteiset | `MessageDao` (`account`-rajaus), `MessagesViewModel.exportJson`, `MainActivity` (`claimUnowned`), `docs/AVOIMET.md` › Useampi tili samalla laitteella |
| `differences` | `d_reminders` | Muistutukset laitteella pelin loppuun, kuutiomuistutus sytyttää silmät | `ReminderBook`, `DgBoard.kt` robotti |
| `differences` | `d_marks` | Mark position muistutusten vieressä; peli, siirtonumero ja kommentti (kenttä `Comment (optional)`, sana on analyysiohjelmien, Tommin päätös 17.9.2026); säilyy poistoon asti; lista Matches-välilehdellä nimen alla otteluittain; ei lähetystä; profiilin päättyneellä ottelulla Share .mat file ja Share .sgf for GNU Backgammon, merkki tallentaa aseman ja `.sgf`:n kommentti on merkityn siirron solmussa, asematon tai osumaton merkki pelin alussa, vain GNU näyttää kommentin, sivuston siirtonumero ei ole pelin siirtonumero; nackgammon ja double repeatin toistettu peli eivät kirjoitu `.sgf`:ksi, DR-ottelu ilman toistoa ilman Crawfordia turnaussivun `Game`-ehdosta ja ystävyysottelu Crawfordilla | `MarkBook`, `BoardScreen.kt` `MarkStrip`, `MarksScreen.kt`, `TopScreen.kt` `MarksLine`, `PageScreen.kt` `MatchRow`, `SgfExport`, `CheckerPosition`, `MatGame.setup`, `EventPage.doubleRepeat` (mittaus 15.9.2026 ja 16.9.2026 `docs/AVOIMET.md`) |
| `differences` | `d_look` | Monte Carlo -tyylit värittävät roolin mukaan, robotti kuutiossa | `BoardLook.kt`, `DgBoard.kt` |
| `differences` | `d_confirm` | Join, Sign up, Double, Accept ja postaus vahvistetaan | `lounge_*_confirm_*`, `discussion_confirm_*`, `CubeAction` |
| `differences` | `d_unconfirmed` | Board not confirmed, ei automaattista toistoa | `board_unconfirmed_*`, `BoardViewModel.kt` |
| `tabs` | `tabs` | Viisi välilehteä; profiili ja turnaus porautumisina; viesti profiililta tallentuu | `DgTabs.kt`, `InfoScreen.kt`, `PageScreen.kt`, `page_message_sent` |
| `matches` | `order` | Määräaikajärjestys; Sort by -napit ja kolmen napautuksen kierto; nuoli; valinta säilyy | `MatchOrder.next`, `TopScreen.kt` rivit 537–578, `MatchOrderStore` |
| `matches` | `open` | Avaus ei kuluta vuoroa; paluu teon jälkeen hakee luettelon, katsomiskäynti ei; Refresh on käyttäjän | `BoardViewModel.actedOnSite`, `MainActivity.kt` › leaveBoard, `docs/UI.md` poikkeus 16.9.2026 |
| `matches` | `tournaments` | Kierros ja vastustaja luettelosta tai muistista; voitot vain ilman kierrosta; ikärivi; Refresh lukee profiilin; ensiavaus lukee profiilin kerran | `OwnTournaments.kt`, `PlayerTournamentRowView.showWins`, `MatchMemory` |
| `board` | `move` | Sivuston nimet; kokoaminen laitteella ja Submit Move; Undo; noppanapautus lähettää kun asetus päällä; ei-todennettu asema askel kerrallaan | `BoardViewModel.kt`, `LocalComposition.kt`, `DiceSubmitStore` |
| `board` | `forced` | Pakolliset ja ahne asettavat, eivät lähetä | `ForcedStepsStore`, `GreedyBearoffStore` |
| `board` | `double` | Kuution kolme vaihetta, kuution napautus sama painallus | `CubeAction` (`BoardScreen.kt`) |
| `board` | `unconfirmed` | Board not confirmed, asema on viimeinen ladattu | `board_unconfirmed_*` |
| `board` | `gestures` | Veto alas hakee; Refresh sama; Back; palkit piiloon vain kun ne veisivät laudan kokoa tai sivulla paneelin leveyttä; reunapyyhkäisy | `BoardScreen.pullDownToRefresh`, `DgBoard.barsAreFree`, `Immersive.kt` |
| `board` | `beside` | Sivupaneeli: linkit, Message ja Reply, chat siirron mukana ja talteen, muistutukset, kuutiomuistutus | `BoardScreen.kt` sivupaneeli, `board_chat_*`, `board_reminder_*`, `Notes.kt` |
| `board` | `skip` | Skip Game ohittaa jonossa, ottelu koskematon | `SkipGameAction` |
| `board` | `look` | Kolme lautatyyliä ja värit; noppa- ja pistetyyli; robotti ja CC BY | `BoardLook.kt`, `BoardStyleStore`, `DiceStyleStore`, `ScoreStyleStore`, `DgBoard.kt` |
| `messages` | `take` | Ei taustahakua, ei ajastinta, ei ilmoituksia; Take an item; tallennus ennen näyttöä | `MessagesViewModel.readItem` |
| `messages` | `queue` | Ilmoitus ei tallennu, lauta ei tallennu, kutsu jää jonoon ja siihen vastataan | `InvitationParser`, `MessagesScreen.kt` |
| `messages` | `reply` | Vastauslaatikko, vastaus pelaajalle, fraasit, lainaus; oma viesti näyttää kohteen | `MessagesScreen.kt` (`MessageRow`, `messages_to`), `PhraseStore` |
| `messages` | `archive` | Suodatin; Export; Back up kerran vuorokaudessa Inboxin ollessa auki ja Save now; tiedostossa viestit, muistutukset ja fraasit; Import lisää puuttuvat eikä muuta olemassa olevaa | `MessageFilterStore`, `BackupStatus.isDue`, `MainActivity` backup-lohko, `MessagesViewModel.importJson`, `ArchiveFile` |
| `messages` | `one_device` | Arkisto vain tällä laitteella; muualla luettu viesti ei tule tänne; siirrot muualla eivät vie mitään | `CLAUDE.md` › Kenelle |
| `messages` | `move_device` | Export ja Back up kirjoittavat tiedoston; Import uudella laitteella tuo viestit, muistutukset ja fraasit ja jättää laitteen omat ennalleen; merkityt asemat eivät ole tiedostossa | `MessagesViewModel.exportJson` ja `importJson`, `MarkBook` (ei viennissä); testi `tuontiLuvataanJaSenSaantoSanotaan` |
| `lounge` | `join` | Join ja Sign up vahvistetaan; Cancel sign-up | `LoungeScreen.kt`, `lounge_*_confirm_*` |
| `lounge` | `forum` | New ja Add a Comment vahvistetaan, postausta ei voi muokata | `DiscussionScreen.kt`, `discussion_confirm_*` |
| `lounge` | `settings` | Laiteosion kytkimet nimeltä, mitään ei lähde sivustolle; sivuston lomake kokonaisena ja luetaan takaisin | `SettingsScreen.DeviceSection`, `docs/ASETUKSET.md` luvut 3 ja 5 |
| `never` | `never` | Ei lähetystä ilman painallusta; ei taustatyötä; lukee vain avatut sivut; säilyttää vain laitteella | `MessagesViewModel` (ei ajastusta), `SiteSettingsRefresher`, `HelpScreen.kt` otsakekommentti |

## Miten tämä pidetään ajan tasalla

- Uusi käyttäjälle näkyvä toiminto: lisää väite oikeaan vastaukseen tai uusi kysymys
  (`help_q_*` ja `help_a_*` sekä `HELP_GROUPS`-listaan `HelpScreen.kt`:ssä), ja rivi tähän
  taulukkoon. Jos toiminto on ero sivustoon, myös väite `differences`-ryhmään. Jos toiminto on nimetty nappi tai kytkin, lisää sen nimi myös
  `HelpConsistencyTest`iin, ellei se osu jo olemassa olevaan sääntöön (kaikki
  `settings_*_toggle` ja `settings_board_style_*` tarkistetaan automaattisesti).
- Poistettu toiminto: poista väite samassa commitissa. Testi ei huomaa poistoa, vain
  puuttuvan maininnan.
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
