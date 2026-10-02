package fi.tommi.dg.data

import fi.tommi.dg.data.db.MessageDao
import fi.tommi.dg.data.db.toDomain
import fi.tommi.dg.data.db.toEntity
import fi.tommi.dg.domain.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Arkisto on ainoa tie jäsennetystä viestistä ruudulle.
 *
 * Sääntö "viesti kirjoitetaan kantaan ennen kuin se näytetään" on tässä rakenteena eikä
 * kommenttina: [archive] on ainoa funktio joka palauttaa juuri haetun viestin, ja se
 * palauttaa sen vasta tallennuksen jälkeen. Kutsuja ei siis voi vahingossa näyttää ensin
 * ja tallentaa sitten, koska sille ei ole rajapintaa.
 *
 * Syy on että haku on tuhoava: `/bg/nextgame` kuluttaa jonoa, joten näytetty mutta
 * tallentamatta jäänyt viesti on mennyt lopullisesti, jos sovellus kaatuu ennen kirjoitusta.
 *
 * **Tämä on rajapinta 1.9.2026 alkaen (auditoinnin H6), ja syy on sauma eikä vaihtoehto.**
 * [DgData] on sanonut alusta asti että ulos näkyvä lupaus on tallennustavasta riippumaton,
 * mutta konkreettinen luokka teki siitä väitteen: `:app`in testit eivät voineet korvata
 * arkistoa, joten ne toteuttivat Roomin DAO-rajapinnan käsin ja tunsivat siten
 * `MessageEntity`n. Sauma oli siis kerrosta liian syvällä juuri siellä missä sitä eniten
 * käytetään. Toteutuksia on yhä yksi ([RoomMessageArchive]); kyse ei ole vaihdettavuudesta
 * vaan siitä, mitä kerrosta testin on pakko tuntea.
 *
 * **Arkisto on tilikohtainen 16.9.2026 alkaen** (Tommin päätös 15.9.2026, `docs/AVOIMET.md`
 * › Useampi tili samalla laitteella). Jokainen kirjoitus nimeää tilin ja jokainen luku
 * rajaa sillä, paitsi [observeByMatch]: ottelunumero on sivustolla tilikohtainen jo
 * itsessään, joten rajaus olisi toisto. Tili on parametri eikä rakentajan tila, koska
 * `Sign out` ja toisella tilillä kirjautuminen tapahtuvat saman prosessin sisällä, ja
 * rakentajaan luettu nimi olisi vanhentunut hiljaa.
 */
interface MessageArchive {

    /**
     * Tallentaa juuri haetut viestit ja palauttaa ne näytettäväksi.
     *
     * @param account Kirjautuneen tili, tai `null` jos tunnuksia ei juuri nyt ole. Nullilla
     *   tallennettu rivi on omistajaton ja tulee näkyviin seuraavalle kirjautujalle
     *   ([claimUnowned]); se on parempi kuin jättää viesti tallentamatta, koska hakua ei
     *   voi toistaa.
     * @return sama lista, mutta vasta kun jokainen viesti on kannassa. Uudelleen haettu
     *   viesti palautetaan silti: se on kutsujalle yhtä näytettävä, ja kanta pysyy
     *   muuttumattomana koska tunniste on sisältötiiviste.
     */
    suspend fun archive(messages: List<Message>, account: String?): List<Message>

    /**
     * Yksi viesti on yhden mittainen lista, eikä toteutus saa päättää siitä toisin. Siksi
     * tämä on tässä eikä toteutuksessa: kirjoitus ja paluuarvo pysyvät samana sääntönä
     * riippumatta siitä mikä kanta alla on.
     */
    suspend fun archive(message: Message, account: String?): Message =
        archive(listOf(message), account).single()

    /**
     * Tuonti tiedostosta: kirjoittaa ne viestit joita kannassa ei vielä ole.
     *
     * Sama kirjoitus kuin [archive]lla ja sama sääntö (tunniste on sisältötiiviste, törmäys
     * ohitetaan), mutta paluuarvo on määrä eikä lista, koska tuonnin ruutu kertoo kuinka
     * moni oli uusi. Jo kannassa oleva rivi ei muutu: ei tili, ei aika, ei mikään (Tommin
     * päätös 16.9.2026: tuonti vain lisää eikä koskaan korvaa).
     *
     * @return kuinka monta viestiä kirjoitettiin.
     */
    suspend fun addMissing(messages: List<Message>, account: String?): Int

    /** Tilin koko arkisto uusin ensin. `null`-tili näkee tyhjän listan. */
    fun observeAll(account: String?): Flow<List<Message>>

    fun observeByMatch(matchId: String): Flow<List<Message>>

    /**
     * Yhden pelaajan historia. **Arkiston pääsypolku**, ks. `MessageDao.observeByOpponent`.
     */
    fun observeByOpponent(opponent: String, account: String?): Flow<List<Message>>

    /** Ketkä tilin arkistossa ovat, uusin keskustelu ensin. */
    fun observeOpponents(account: String?): Flow<List<String>>

    /** Kaikkien tilien rivit yhteensä. Ei ruudun tieto vaan testin ja migraation. */
    suspend fun count(): Int

    /**
     * Antaa omistajattomat rivit tälle tilille. Kutsutaan kirjautuneena käynnistyksessä ja
     * kirjautumisen jälkeen; idempotentti. Ks. `MessageDao.claimUnowned`.
     *
     * @return kuinka monta riviä sai omistajan.
     */
    suspend fun claimUnowned(account: String): Int

    /**
     * Arkiston reunapäivä, eli tämän laitteen viimeisin kirjoitushetki, tai `null` jos
     * arkisto on tyhjä.
     *
     * Tämä on ulos näkyvä eikä sisäinen tieto, ja syy on `docs/ARKKITEHTUURI.md`:n varmuuskopiokohta:
     * arkisto on ainoa kopio historiasta, joten sen on kerrottava mihin asti se ulottuu.
     * Vanhentunut arkisto joka ei näytä reunaansa on vanhentunut hiljaa, ja juuri se tekee
     * varmuuskopiosta vaarallisen. Reuna näkyvissä sama palautus on pelkkä vajaa arkisto.
     */
    fun observeNewestStoredAt(account: String?): Flow<Long?>
}

/*
 * Huom: arkisto ei leimaa aikaa. Saapumishetki on osa viestin tunnistetta, joten se on
 * annettava kerran viestiä muodostettaessa. Jos leima annettaisiin täällä, sama viesti
 * saisi eri tunnisteen joka tallennusyrityksellä ja epäonnistuneen kirjoituksen uusiminen
 * tuottaisi duplikaatin. Päätös 1.8.2026, perustelu `docs/KOHDE.md`.
 */
class RoomMessageArchive(
    private val dao: MessageDao,
) : MessageArchive {

    override suspend fun archive(messages: List<Message>, account: String?): List<Message> {
        if (messages.isEmpty()) return emptyList()
        dao.insertIfNew(messages.map { it.toEntity(account) })
        return messages
    }

    override suspend fun addMissing(messages: List<Message>, account: String?): Int {
        if (messages.isEmpty()) return 0
        // Room palauttaa ohitetulle riville -1, joten kirjoitetut ovat ne joilla on rivinumero.
        return dao.insertIfNew(messages.map { it.toEntity(account) }).count { it != -1L }
    }

    override fun observeAll(account: String?): Flow<List<Message>> =
        dao.observeAll(account).map { rows -> rows.map { it.toDomain() } }

    override fun observeByMatch(matchId: String): Flow<List<Message>> =
        dao.observeByMatch(matchId).map { rows -> rows.map { it.toDomain() } }

    override fun observeByOpponent(opponent: String, account: String?): Flow<List<Message>> =
        dao.observeByOpponent(opponent, account).map { rows -> rows.map { it.toDomain() } }

    override fun observeOpponents(account: String?): Flow<List<String>> = dao.observeOpponents(account)

    override suspend fun count(): Int = dao.count()

    override suspend fun claimUnowned(account: String): Int = dao.claimUnowned(account)

    override fun observeNewestStoredAt(account: String?): Flow<Long?> = dao.observeNewestStoredAt(account)
}
