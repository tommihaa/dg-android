package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource

/**
 * Oma lähetetty viesti arkistoon, **yhtenä muodostajana kolmen sijaan** (1.9.2026,
 * auditoinnin H4).
 *
 * Kolme reittiä lähettää: lautachat ([BoardViewModel]), vastaus pikaviestiin
 * ([MessagesViewModel]) ja profiilin lomake ([PageViewModel]). Ne kirjoittivat kukin oman
 * `Message`-konstruktorikutsunsa, ja kolme neljästä yhteisestä kentästä oli kolme kertaa
 * perusteltu erikseen. Ero reittien välillä on kutsupaikan omissa parametreissa, ja se
 * perustellaan yhä siellä: kenen kanssa, minkä lajinen, mihin vastataan.
 *
 * Kolme kenttää ovat lähtevän viestin sääntö eivätkä reitin valinta, ja ne ovat tässä:
 *
 * - **Lähettäjä on käyttäjä itse.** Tyhjä tarkoittaa ettei omaa nimeä tiedetä, jolloin
 *   viestiruutu näyttää lajin nimen (`MessagesScreen`). Se ei ole virhe: nimi luetaan
 *   sivulta, eikä lähettäminen edellytä että se on ehditty lukea.
 * - **Aikaleimaa ei ole.** Sivusto ei anna lähtevälle viestille omaa aikaleimaa millään
 *   kolmesta lomakkeesta, ja sovelluksen kirjoittama leima olisi väite eikä sivun kertoma
 *   (`Message.timestampText`).
 * - **Vastauslomaketta ei ole.** Lomake on se osoite jolla viestiin vastataan, eikä omaan
 *   lähetykseen vastata täältä. Kenttä jätetään pois eikä sitä koota mistään.
 *
 * **Kirjataan tässä tai ei koskaan.** Sivusto ei säilytä lähetettyä tekstiä, joten sitä ei
 * ole luettavissa mistään jälkikäteen (`CLAUDE.md`, ykkösominaisuus).
 */
fun outgoingMessage(
    matchId: MatchId?,
    opponent: String?,
    self: String?,
    body: String,
    rawHeader: String,
    source: MessageSource,
    sentAtEpochMillis: Long,
    replyTo: String? = null,
): Message = Message(
    matchId = matchId,
    opponent = opponent,
    sender = self.orEmpty(),
    timestampText = "",
    body = body,
    rawHeader = rawHeader,
    source = source,
    receivedAtEpochMillis = sentAtEpochMillis,
    replyTo = replyTo,
)
