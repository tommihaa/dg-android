package fi.tommi.dg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SeenMatchDao {

    /**
     * Kirjoittaa rivin vanhan päälle. Yhdistäminen vanhaan riviin tehdään [fi.tommi.dg.data.RoomMatchMemory]ssa
     * eikä tässä, koska SQL:n `REPLACE` ei osaa jättää vanhaa kenttää kun uusi on null.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: SeenMatchEntity)

    @Query("SELECT * FROM seen_matches WHERE matchId = :matchId")
    suspend fun get(matchId: String): SeenMatchEntity?

    /** Kaikki rivit virtana, koska turnauslista tarvitsee ne kerralla ottelun numerolla. */
    @Query("SELECT * FROM seen_matches")
    fun observeAll(): Flow<List<SeenMatchEntity>>

    @Query("SELECT COUNT(*) FROM seen_matches")
    suspend fun count(): Int
}
