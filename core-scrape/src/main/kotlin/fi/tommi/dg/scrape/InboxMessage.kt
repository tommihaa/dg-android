package fi.tommi.dg.scrape

import fi.tommi.dg.domain.Message

/**
 * Jonon kohteesta arkistoitava viesti.
 *
 * Tämä on se puuttunut väli, jonka takia viestiputki ei ollut olemassa vaikka molemmat päät
 * olivat: `InboxParser` osasi lukea sivun ja `MessageArchive` osasi tallentaa viestin, mutta
 * mikään ei muuntanut edellistä jälkimmäiseksi. Este poistui 9.8.2026, kun [Message] sai
 * kentän otsikolle ja lajin tuntemattomalle.
 *
 * **Leima annetaan tässä eikä tallennettaessa**, ja se on koko funktion tärkein ehto.
 * Saapumishetki on osa viestin tunnistetta (päätös 1.8.2026), joten sama sivu tuottaa saman
 * viestin vain jos leima annetaan kerran. Jos `MessageArchive` leimaisi, epäonnistuneen
 * kirjoituksen uusiminen tuottaisi kaksoiskappaleen sen sijaan että olisi turvallista.
 * Siksi hetki tulee parametrina eikä kellosta: kutsuja ottaa sen kerran, ja testi voi
 * antaa saman luvun kahdesti ja väittää tunnisteesta.
 */
fun InboxItem.toMessage(receivedAtEpochMillis: Long): Message = Message(
    // Jonon sivu ei nimeä ottelua, eikä sitä päätellä. Pikaviesti ei kuulu mihinkään
    // otteluun lainkaan, ja ilmoitus mainitsee turnauksen tekstissä muttei tunnisteena.
    // Vastustajan siirron mukana tullut viesti on eri sivu ja eri jäsennin (`ChatParser`),
    // ja ottelu tiedetään siellä.
    matchId = null,
    // Jonon sivulla keskustelukumppani on sama kuin lähettäjä, koska saapunut viesti on
    // aina toiselta. Kentät eivät silti sulaudu yhdeksi: chat-ketjussa oma viesti saa
    // lähettäjäksi käyttäjän itsensä, ja vastustaja on silti sama henkilö.
    //
    // `null` eikä tyhjä silloin kun sivu ei nimennyt ketään, ja ero on merkitsevä:
    // sivuston ilmoituksella ei ole keskustelukumppania lainkaan, eikä nimetön keskustelu
    // ole olemassa oleva asia.
    opponent = sender?.name,
    // Tyhjä kun sivu ei nimennyt lähettäjää, eikä sijaisnimeä keksitä. Ilmoituksella ei ole
    // lähettäjää, ja `DailyGammon` tähän kirjoitettuna olisi sovelluksen väite eikä sivun.
    sender = sender?.displayName.orEmpty(),
    // Jonon sivulla ei ole aikaleimaa lainkaan, ei kummassakaan lajissa. Tyhjä kenttä
    // sanoo sen; [Message.receivedAtEpochMillis] kertoo milloin laite näki viestin, mikä
    // on eri tieto eikä korvaa tätä.
    timestampText = "",
    body = body,
    rawHeader = rawHeader,
    source = source,
    receivedAtEpochMillis = receivedAtEpochMillis,
    // Sivun oma vastauslomake kulkee viestin mukana arkistoon asti (30.8.2026), jotta
    // arkiston viestiin voi vastata jälkikäteen. Osoite on yhä sivulta luettu eikä koottu;
    // talletus vain siirtää lukuhetken ja käyttöhetken erilleen. Ks. `Message.replyForm`.
    replyForm = replyForm,
)
