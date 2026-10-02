package fi.tommi.dg.data.db

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.domain.ReplyForm

/**
 * Domainin ja kannan välinen muunnos.
 *
 * [MessageSource] talletetaan nimenä eikä järjestysnumerona: numero sitoisi kannan
 * enumin kirjoitusjärjestykseen, jolloin uuden lajin lisääminen väliin nimeäisi vanhat
 * rivit uudelleen hiljaa.
 *
 * Huom sarakkeen nimestä: `storedAtEpochMillis` säilytti nimensä kun saapumishetki tuli
 * osaksi tunnistetta (1.8.2026), jottei skeema muutu tarpeettomasti. Sen merkitys on
 * `Message.receivedAtEpochMillis`, eli milloin laite näki viestin.
 */
/**
 * @param account Tili jonka kirjautuneena viesti otettiin talteen. Parametrina eikä
 *   [Message]in kenttänä, koska omistaja on tämän laitteen tieto eikä viestin sisältöä:
 *   sivu ei kerro sitä, eikä sama viesti muutu toiseksi jos toinen tili lukisi sen.
 */
fun Message.toEntity(account: String?): MessageEntity = MessageEntity(
    id = id,
    matchId = matchId?.value,
    opponent = opponent,
    sender = sender,
    timestampText = timestampText,
    body = body,
    rawHeader = rawHeader,
    source = source.name,
    storedAtEpochMillis = receivedAtEpochMillis,
    replyToId = replyTo,
    replyAction = replyForm?.action,
    replyMethod = replyForm?.method?.name,
    replyField = replyForm?.field,
    replyMaxLength = replyForm?.maxLength,
    account = account,
)

fun MessageEntity.toDomain(): Message = Message(
    matchId = matchId?.let(::MatchId),
    opponent = opponent,
    sender = sender,
    timestampText = timestampText,
    body = body,
    rawHeader = rawHeader,
    source = source.toMessageSource(),
    receivedAtEpochMillis = storedAtEpochMillis,
    replyTo = replyToId,
    replyForm = readReplyForm(),
)

/**
 * Talletettu vastauslomake, tai `null` jos yksikin kolmesta pakollisesta osasta puuttuu.
 *
 * Kokonaisuusehto on tässä eikä kutsupaikassa: puolikas lomake ei ole lähetettävissä, ja
 * `null` sanoo sen samalla tavalla kuin sivu joka ei tarjonnut lomaketta lainkaan.
 * Tuntematon metodinimi kaataa lomakkeen eikä koko rivin lukua, samasta syystä kuin
 * tuntematon laji alla: viestin säilyminen voittaa, ja vastauskyky on lisä jonka voi
 * menettää menettämättä viestiä.
 */
private fun MessageEntity.readReplyForm(): ReplyForm? {
    val action = replyAction ?: return null
    val field = replyField ?: return null
    val method = FormMethod.entries.firstOrNull { it.name == replyMethod } ?: return null
    return ReplyForm(action = action, method = method, field = field, maxLength = replyMaxLength)
}

/**
 * Tuntematon nimi ei kaada lukua vaan luetaan tuntemattomaksi lajiksi.
 *
 * Perustelu: tämä voi toteutua vain jos kantaan on kirjoittanut uudempi sovellusversio,
 * eli käyttäjä on siirtynyt taaksepäin. Poikkeus hukkaisi silloin koko listan, kun taas
 * väärä laji hukkaa vain lajittelun, ja itse viesti säilyy luettavana. Viestin
 * säilyminen on tämän sovelluksen koko syy, joten se voittaa.
 *
 * **Kaatuminen [MessageSource.UNKNOWN]iin eikä [MessageSource.ANNOUNCEMENT]iin (9.8.2026).**
 * Ennen tuntemattoman lajin olemassaoloa tässä oli valittava jokin tunnetuista, ja
 * ilmoitus oli niistä vähiten väärä. Nyt valintaa ei tarvitse tehdä: rivi joka tuli
 * tuntemattomasta lajista luetaan tuntemattomaksi, eikä se väitä olevansa sivuston
 * ilmoitus. Tämä on sama muoto kuin jäsentimen puolella, eli tuntematon säilyy
 * tuntemattomana koko matkan sivulta listalle.
 *
 * Huom seuraus: tällaisen rivin [Message.id] laskeutuu eri arvoon kuin sen pääavain, koska
 * laji on osa tiivistettä. Rivi säilyy luettavana, mutta sitä ei pidä kirjoittaa takaisin.
 */
private fun String.toMessageSource(): MessageSource =
    MessageSource.entries.firstOrNull { it.name == this } ?: MessageSource.UNKNOWN
