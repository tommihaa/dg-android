package fi.tommi.dg.data

import fi.tommi.dg.data.db.MarkedPositionDao
import fi.tommi.dg.data.db.toDomain
import fi.tommi.dg.data.db.toEntity
import fi.tommi.dg.domain.CheckerPosition
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.MarkedPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Merkityt asemat, eli pelaajan kirjanmerkit ottelun jälkeistä analyysia varten.
 *
 * **Tämä ei kosketa sivustoon eikä voi koskea**, samoin kuin [ReminderBook]: toteutus saa
 * vain DAO:n. Merkki on sovelluksen omaa tietoa alusta loppuun, ks.
 * [fi.tommi.dg.domain.MarkedPosition].
 *
 * **Elinkaari on käyttäjän poisto eikä kysely**, ja siinä tämä eroaa [ReminderBook]ista.
 * Muistutus lakkaa näkymästä pelin vaihtuessa, koska se koskee yhtä peliä. Merkki luetaan
 * ottelun jälkeen ja pohditaan analyysin tullessa, joka voi olla viikkoja myöhemmin, joten
 * mikään ajastus eikä pisteluku saa viedä sitä. Poisto on se teko jolla pelaaja kuittaa
 * aseman pohdituksi.
 */
interface MarkBook {

    /** Yhden pelin merkit siirtonumeron järjestyksessä, laudan alle. */
    fun observe(game: GameKey): Flow<List<MarkedPosition>>

    /** Kaikki merkit uusin ensin, otteluluettelon linkin taakse. */
    fun observeAll(): Flow<List<MarkedPosition>>

    /**
     * Kirjoittaa merkin. Sana saa olla tyhjä: merkin arvo on kohdassa eikä selityksessä,
     * toisin kuin muistutuksessa jonka koko sisältö on teksti. [position] on asema
     * merkintähetkellä `.sgf`-viennin kohdistusta varten, tai null kun lautaa ei voitu lukea.
     */
    suspend fun add(game: GameKey, moveNumber: Int, note: String, atEpochMillis: Long, position: CheckerPosition? = null): Long

    suspend fun remove(id: Long)

    suspend fun count(): Int
}

class RoomMarkBook(
    private val dao: MarkedPositionDao,
) : MarkBook {

    override fun observe(game: GameKey): Flow<List<MarkedPosition>> =
        dao.observeByGame(game.matchId.value, game.opponentScore, game.selfScore)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeAll(): Flow<List<MarkedPosition>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(game: GameKey, moveNumber: Int, note: String, atEpochMillis: Long, position: CheckerPosition?): Long =
        dao.insert(
            MarkedPosition(
                id = 0,
                game = game,
                moveNumber = moveNumber,
                note = note.trim(),
                createdAtEpochMillis = atEpochMillis,
                position = position,
            ).toEntity(),
        )

    override suspend fun remove(id: Long) = dao.delete(id)

    override suspend fun count(): Int = dao.count()
}
