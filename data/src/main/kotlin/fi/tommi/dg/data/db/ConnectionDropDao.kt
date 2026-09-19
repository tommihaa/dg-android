package fi.tommi.dg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDropDao {

    @Insert
    suspend fun insert(row: ConnectionDropEntity): Long

    /** Uusin ensin, koska lukija etsii sitä korttia jonka juuri sulki. */
    @Query("SELECT * FROM connection_drops ORDER BY atEpochMillis DESC, id DESC")
    fun observeNewestFirst(): Flow<List<ConnectionDropEntity>>

    @Query("SELECT COUNT(*) FROM connection_drops")
    suspend fun count(): Int

    /**
     * Pudottaa vanhimmat niin että jäljelle jää [keep] uusinta. Raja on lisäyksen yhteydessä
     * eikä erillisenä siivouksena, jotta taulu ei kasva rajatta laitteella jolla katkoja on paljon.
     */
    @Query(
        """
        DELETE FROM connection_drops WHERE id NOT IN (
            SELECT id FROM connection_drops ORDER BY atEpochMillis DESC, id DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int)
}
