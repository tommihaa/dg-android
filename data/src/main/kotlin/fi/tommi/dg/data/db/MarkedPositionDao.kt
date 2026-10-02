package fi.tommi.dg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkedPositionDao {

    @Insert
    suspend fun insert(row: MarkedPositionEntity): Long

    /**
     * Yhden pelin merkit siirtonumeron järjestyksessä. Lauta näyttää nämä laudan alla
     * heti merkinnän jälkeen, jotta painallus näkyy tehdyksi.
     */
    @Query(
        """
        SELECT * FROM marked_positions
        WHERE matchId = :matchId AND opponentScore = :opponentScore AND selfScore = :selfScore
        ORDER BY moveNumber ASC, id ASC
        """
    )
    fun observeByGame(matchId: String, opponentScore: Int, selfScore: Int): Flow<List<MarkedPositionEntity>>

    /**
     * Kaikki merkit uusin ottelu ensin, ja ottelun sisällä siirtonumeron järjestyksessä.
     *
     * **Ei ottelukohtaista kyselyä eikä siivousta**, koska elinkaari on käyttäjän poisto
     * (Tommin valinta 15.9.2026). Rivi jota ei ole poistettu on rivi jota ei ole vielä
     * pohdittu, ja lista on juuri sitä varten olemassa.
     */
    @Query("SELECT * FROM marked_positions ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<MarkedPositionEntity>>

    @Query("DELETE FROM marked_positions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM marked_positions")
    suspend fun count(): Int
}
