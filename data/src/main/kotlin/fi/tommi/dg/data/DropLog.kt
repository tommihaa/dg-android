package fi.tommi.dg.data

import fi.tommi.dg.data.db.ConnectionDropDao
import fi.tommi.dg.data.db.toDomain
import fi.tommi.dg.data.db.toEntity
import fi.tommi.dg.domain.ConnectionDrop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Katkohistoria, eli milloin `Board not confirmed` tuli ja miksi.
 *
 * **Tämä ei kosketa sivustoon eikä voi koskea**, kuten [ReminderBook]: toteutus saa vain
 * DAO:n. Kirjoittaja on lauta silloin kun teko päättyi katkoon, ja lukija on Info-välilehden
 * lista. Ks. [fi.tommi.dg.domain.ConnectionDrop].
 *
 * **Historia on rajattu eikä täysi**, ja raja on [KEEP]. Kysymys johon rivi vastaa on
 * "mikä se äskeinen oli", ja siihen riittää sadan viimeisen jono. Vanhin putoaa lisäyksessä
 * eikä erillisessä siivouksessa, jottei taulu kasva laitteella jonka verkko pätkii.
 */
interface DropLog {

    /** Uusin ensin. Virta, jotta lista päivittyy jos katko osuu sen ollessa auki. */
    fun observeNewestFirst(): Flow<List<ConnectionDrop>>

    suspend fun record(drop: ConnectionDrop)

    suspend fun count(): Int

    companion object {
        const val KEEP = 100
    }
}

class RoomDropLog(
    private val dao: ConnectionDropDao,
) : DropLog {

    override fun observeNewestFirst(): Flow<List<ConnectionDrop>> =
        dao.observeNewestFirst().map { rows -> rows.map { it.toDomain() } }

    override suspend fun record(drop: ConnectionDrop) {
        dao.insert(drop.toEntity())
        dao.trimTo(DropLog.KEEP)
    }

    override suspend fun count(): Int = dao.count()
}
