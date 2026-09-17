package fi.tommi.dg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {

    @Insert
    suspend fun enqueue(action: PendingActionEntity): Long

    /**
     * Jono vanhin ensin. Järjestys on osa oikeellisuutta eikä makuasia: siirtosarja on
     * peräkkäisiä osoitteita joissa polun tilatunniste kasvaa, joten väärässä
     * järjestyksessä lähetetty toinen askel osuisi tilaan jota ei ole.
     */
    @Query("SELECT * FROM pending_actions ORDER BY createdAtEpochMillis ASC, id ASC")
    suspend fun oldestFirst(): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions ORDER BY createdAtEpochMillis ASC, id ASC")
    fun observeOldestFirst(): Flow<List<PendingActionEntity>>

    @Query(
        "SELECT * FROM pending_actions WHERE matchId = :matchId " +
            "ORDER BY createdAtEpochMillis ASC, id ASC"
    )
    fun observeByMatch(matchId: String): Flow<List<PendingActionEntity>>

    @Query("SELECT COUNT(*) FROM pending_actions")
    suspend fun count(): Int

    /** Kutsutaan vasta kun palvelin on vastannut onnistuneesti. */
    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun remove(id: Long)

    /**
     * Poistaa ottelun aiemmat rivit. Kutsutaan uutta riviä kirjattaessa, ks.
     * [fi.tommi.dg.data.ActionQueue.record].
     */
    @Query("DELETE FROM pending_actions WHERE matchId = :matchId")
    suspend fun removeByMatch(matchId: String)
}
