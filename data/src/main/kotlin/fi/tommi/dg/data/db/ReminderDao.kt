package fi.tommi.dg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    /** Kaikki muistutukset otteluittain, viennin lähde. */
    @Query("SELECT * FROM reminders ORDER BY matchId ASC, createdAtEpochMillis ASC, id ASC")
    suspend fun all(): List<ReminderEntity>

    /**
     * Onko sama muistutus jo kannassa. Muistutuksella ei ole sisältöavainta kuten viestillä,
     * joten samuus on kaikki viisi kenttää yhdessä; sama teksti samaan peliin eri hetkellä
     * on kaksi muistutusta, kuten se on käyttäjän kirjoittamanakin.
     */
    @Query(
        """
        SELECT COUNT(*) FROM reminders
        WHERE matchId = :matchId AND opponentScore = :opponentScore AND selfScore = :selfScore
          AND text = :text AND createdAtEpochMillis = :createdAtEpochMillis
        """
    )
    suspend fun countSame(
        matchId: String,
        opponentScore: Int,
        selfScore: Int,
        text: String,
        createdAtEpochMillis: Long,
    ): Int

    /**
     * Yhden pelin muistutukset vanhimmasta uusimpaan.
     *
     * **Kysely on täsmälleen yhdelle pisteparille, ja siinä on koko elinkaari.** Muistutus
     * elää pelin mitalta (Tommin päätös 23.8.2026), eikä sitä toteuteta vanhenemisajalla
     * eikä siivouksella vaan sillä ettei toisen pelin riviä kysytä. Peli vaihtuu kun pisteet
     * muuttuvat, jolloin tämä kysely ei enää osu edellisen pelin riveihin. Elinkaari on siis
     * rakenteessa eikä ajastuksessa, ja se pitää myös silloin kun siivous jäisi tekemättä.
     */
    @Query(
        """
        SELECT * FROM reminders
        WHERE matchId = :matchId AND opponentScore = :opponentScore AND selfScore = :selfScore
        ORDER BY createdAtEpochMillis ASC, id ASC
        """
    )
    fun observeByGame(matchId: String, opponentScore: Int, selfScore: Int): Flow<List<ReminderEntity>>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Saman ottelun muiden pelien rivit pois.
     *
     * **Tämä on siivousta eikä elinkaari**, ja ero on tärkeä: näkyvyyden ratkaisee jo
     * [observeByGame], joten tämä ei voi muuttaa sitä mitä ruudulla näkyy. Ilman tätä kanta
     * kasvaisi riveillä jotka ovat pysyvästi näkymättömiä.
     *
     * **Kutsutaan vain kun käyttäjä kirjoittaa uuden muistutuksen samaan otteluun**, ei
     * sivun latauksesta. Automaattinen poisto laudan lukemisen yhteydessä olisi nojannut
     * jäsennettyyn pisteeseen: yksi väärin luettu piste hävittäisi rivit hiljaa, ja se on
     * tämän projektin pahin vikamuoto. Nyt poisto tapahtuu vain siinä ottelussa jossa
     * käyttäjä on juuri itse tehnyt saman tiedon uudestaan.
     */
    @Query(
        """
        DELETE FROM reminders
        WHERE matchId = :matchId
          AND NOT (opponentScore = :opponentScore AND selfScore = :selfScore)
        """
    )
    suspend fun deleteOtherGames(matchId: String, opponentScore: Int, selfScore: Int)

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun count(): Int
}
