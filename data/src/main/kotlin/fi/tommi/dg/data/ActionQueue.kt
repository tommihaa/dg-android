package fi.tommi.dg.data

import fi.tommi.dg.data.db.PendingActionDao
import fi.tommi.dg.data.db.toDomain
import fi.tommi.dg.data.db.toEntity
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PendingAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Lähtevien tekojen jono, eli se mitä painettiin muttei mennyt perille.
 *
 * Rinnakkainen [MessageArchive]lle ja samasta syystä olemassa: `:app` ei saa nähdä Roomia,
 * jotta tallennustavan vaihto ei vuoda käyttöliittymään. Ulos näkyy [PendingAction], joka
 * on tallennustavasta riippumaton. Rajapinta samasta syystä kuin [MessageArchive]lla
 * (1.9.2026, auditoinnin H6): sauma on siinä kerroksessa jota `:app` käyttää.
 *
 * **Jono ei lähetä mitään.** Se on tarkoituksella pelkkä kirjanpito: teko lähtee vain
 * käyttäjän painalluksesta, ja lähetyksen rakentaa sivun oma lomake. Perustelu on
 * `DgClient`in uusintarajassa (`Intent.ACT`) ja `PendingAction`in omassa tekstissä: teko
 * joka lähtee itsestään voi lähteä kahdesti, ja kahdesti lähetetty siirto on pahempi kuin
 * lähettämättä jäänyt.
 */
interface ActionQueue {

    /**
     * Kirjaa epäonnistuneen painalluksen.
     *
     * **Ottelulla on kerrallaan enintään yksi rivi**, ja uusi korvaa vanhan. Syy on että
     * rivit eivät ole toistensa jatkoa: jos ensimmäinen painallus ei mennyt perille,
     * palvelimen tila ei liikkunut, joten toinen painallus koskee samaa hetkeä. Kaksi riviä
     * kuvaisi siis samaa tekemätöntä tekoa kahdesti, ja käyttäjä näkisi listan jonka
     * pituus kertoisi yritysten määrästä eikä siitä mitä on tekemättä.
     *
     * Ottelutta luodut rivit eivät korvaa mitään, koska niillä ei ole sitä avainta jolla
     * korvattava tunnistettaisiin. Sellaisia ei vielä synny; kenttä on nullable siksi että
     * kaikki muuttavat toiminnot eivät liity otteluun.
     */
    suspend fun record(action: PendingAction): Long

    /** Ottelun odottava teko, tai `null` kun mitään ei odota. */
    fun observeByMatch(matchId: MatchId): Flow<PendingAction?>

    fun observeAll(): Flow<List<PendingAction>>

    /**
     * Kutsutaan kun teko on mennyt perille tai kun sivu ei enää tarjoa sitä.
     *
     * **Ainoa tapa jolla rivi poistuu itsestään**, ja sitä vastaava yritysten kirjaus poistui
     * 8.9.2026 uusintanapin mukana. Riviä ei päivitetä enää millään: se joko on jonossa
     * sellaisenaan tai ei ole.
     */
    suspend fun clear(id: Long)

    suspend fun count(): Int
}

class RoomActionQueue(
    private val dao: PendingActionDao,
) : ActionQueue {

    override suspend fun record(action: PendingAction): Long {
        action.matchId?.let { dao.removeByMatch(it.value) }
        return dao.enqueue(action.toEntity())
    }

    override fun observeByMatch(matchId: MatchId): Flow<PendingAction?> =
        dao.observeByMatch(matchId.value).map { rows -> rows.firstOrNull()?.toDomain() }

    override fun observeAll(): Flow<List<PendingAction>> =
        dao.observeOldestFirst().map { rows -> rows.map { it.toDomain() } }

    override suspend fun clear(id: Long) = dao.remove(id)

    override suspend fun count(): Int = dao.count()
}
