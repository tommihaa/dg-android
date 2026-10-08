package fi.tommi.dg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /**
     * Tallentaa viestin jos sitä ei vielä ole.
     *
     * [OnConflictStrategy.IGNORE] eikä `REPLACE`: pääavain on sisältötiiviste, joten
     * törmäys tarkoittaa **täsmälleen samaa viestiä**. Korvaaminen kirjoittaisi
     * [MessageEntity.storedAtEpochMillis]-kentän uusiksi ja siirtäisi vanhan viestin
     * listan kärkeen ilman että mikään on muuttunut.
     *
     * @return rivinumero, tai -1 jos viesti oli jo kannassa.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(messages: List<MessageEntity>): List<Long>

    /**
     * Kirjautuneen tilin arkisto uusin ensin.
     *
     * **Tilillä rajattu 16.9.2026 alkaen**, ks. [MessageEntity.account]. `null`-tili ei
     * osu mihinkään riviin, koska SQL:n `=` ei koskaan ole tosi NULLille; kirjautumaton
     * näkee siis tyhjän arkiston eikä kaikkien tilien yhteistä listaa. Omistajattomat
     * rivit (ennen 16.9.2026 tallennetut) tulevat näkyviin vasta [claimUnowned]in jälkeen.
     */
    @Query("SELECT * FROM messages WHERE account = :account AND deletedAtEpochMillis IS NULL ORDER BY storedAtEpochMillis DESC, id ASC")
    fun observeAll(account: String?): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE matchId = :matchId AND deletedAtEpochMillis IS NULL ORDER BY storedAtEpochMillis ASC, id ASC")
    fun observeByMatch(matchId: String): Flow<List<MessageEntity>>

    /**
     * Yhden pelaajan koko historia vanhimmasta uusimpaan.
     *
     * **Tämä on arkiston pääsypolku, ei yksi haku muiden joukossa.** Perustelu on
     * `SUBSTANSSI.md`:n kohdissa 26 ja 46: kohde tiedetään ennen kuin arkisto avataan, ja
     * kysymys johon tämä vastaa esitetään ottelun alussa (kirjoitanko tälle, mistä
     * puhuttiin). Hakua ei ole eikä tule, ks. `docs/AVOIMET.md`.
     *
     * Järjestys on nouseva kuten [observeByMatch]issa, koska keskustelu luetaan alusta
     * loppuun. [observeAll] on laskeva, koska se on lista eikä keskustelu.
     *
     * Nimivertailu on SQLiten oletus eli **kirjainkokoherkkä**. Se on tässä oikein: nimi
     * tulee sivuston omasta linkistä eikä käyttäjän kirjoittamana, joten kaksi eri
     * kirjoitusasua tarkoittaisi kahta eri asiaa eikä samaa nimeä väärin kirjoitettuna.
     */
    @Query("SELECT * FROM messages WHERE account = :account AND opponent = :opponent AND deletedAtEpochMillis IS NULL ORDER BY storedAtEpochMillis ASC, id ASC")
    fun observeByOpponent(opponent: String, account: String?): Flow<List<MessageEntity>>

    /**
     * Ketkä arkistossa ovat, uusimman viestin mukaan järjestettynä.
     *
     * Vastaa kysymykseen "kenen kanssa olen puhunut", joka on pelaajakohtaisen näkymän
     * sisäänkäynti. Nimetön rivi jää pois: `null` tarkoittaa ettei kumppania tiedetty, eikä
     * tuntematon ole henkilö jonka historiaa voisi avata.
     */
    @Query(
        """
        SELECT opponent FROM messages
        WHERE account = :account AND opponent IS NOT NULL AND deletedAtEpochMillis IS NULL
        GROUP BY opponent
        ORDER BY MAX(storedAtEpochMillis) DESC
        """
    )
    fun observeOpponents(account: String?): Flow<List<String>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun byId(id: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int

    /**
     * Arkiston reunapäivä: milloin **tämä laite** kirjoitti viimeksi jotain kantaan.
     *
     * Kysely on `storedAtEpochMillis`ille eikä viestin omalle ajalle kahdesta syystä.
     * Sivuston aika on [MessageEntity.timestampText], eli tulkitsematonta tekstiä jota ei
     * voi verrata mihinkään. Ja kysymys johon tämä vastaa on "mihin asti arkisto ulottuu",
     * mikä on laitteen kirjoitushetki eikä lähetyshetki.
     *
     * Tyhjä kanta antaa `null`in eikä nollaa: `MAX` tyhjästä on SQL:ssä NULL, ja se on
     * oikea vastaus. Nolla olisi vuoden 1970 päivämäärä, eli väärä tieto oikean muotoisena.
     */
    @Query("SELECT MAX(storedAtEpochMillis) FROM messages WHERE account = :account AND deletedAtEpochMillis IS NULL")
    fun observeNewestStoredAt(account: String?): Flow<Long?>

    /**
     * Ensimmäinen kirjautunut tili ottaa omistajattomat rivit omikseen.
     *
     * Rivit ovat omistajattomia vain siksi, että ne tallennettiin ennen kuin sarake oli
     * olemassa (migraatio 9→10). Migraatio itse ei voi tietää tilin nimeä, koska tunnukset
     * asuvat `:app`in säilössä eikä kannassa. Laitteella oli sarakkeen tullessa yksi tili,
     * joten kaikki vanhat rivit kuuluvat sille, ja ensimmäinen kirjautuminen on hetki jolloin
     * nimi tiedetään. Kutsu on idempotentti: toisella kerralla ei ole mitään otettavaa.
     *
     * @return kuinka monta riviä sai omistajan.
     */
    @Query("UPDATE messages SET account = :account WHERE account IS NULL")
    suspend fun claimUnowned(account: String): Int

    /**
     * Merkitsee annetut viestit poistetuiksi tämän tilin arkistossa (Tommin päätös 6.10.2026,
     * `docs/AVOIMET.md` › *Roskakori ja yksittäisen viestin poisto*).
     *
     * Rivi jää kantaan, ja jokainen ruudun kysely ohittaa sen. Jo poistettu ei saa uutta
     * aikaa, jotta roskakorin ryhmä ja 30 päivän laskuri pysyvät ensimmäisen poiston mukaisina.
     * Tili rajaa poiston kuten luvut: toisen tilin rivi ei poistu, vaikka tunniste osuisi.
     *
     * @return poistetuiksi merkittyjen rivien määrä.
     */
    @Query(
        """
        UPDATE messages SET deletedAtEpochMillis = :at
        WHERE account = :account AND id IN (:ids) AND deletedAtEpochMillis IS NULL
        """
    )
    suspend fun markDeleted(ids: List<String>, account: String?, at: Long): Int

    /**
     * Palauttaa poistetut viestit arkistoon (*Restore* ja *Undo*). Viesti palaa sellaisenaan,
     * joten se asettuu listalle alkuperäiselle paikalleen tallennusajan mukaan.
     *
     * @return palautettujen rivien määrä.
     */
    @Query(
        """
        UPDATE messages SET deletedAtEpochMillis = NULL
        WHERE account = :account AND id IN (:ids) AND deletedAtEpochMillis IS NOT NULL
        """
    )
    suspend fun restore(ids: List<String>, account: String?): Int

    /** Tilin poistetut viestit, uusin poisto ensin. Roskakorin sisältö. */
    @Query(
        """
        SELECT * FROM messages
        WHERE account = :account AND deletedAtEpochMillis IS NOT NULL
        ORDER BY deletedAtEpochMillis DESC, storedAtEpochMillis ASC, id ASC
        """
    )
    fun observeDeleted(account: String?): Flow<List<MessageEntity>>

    /**
     * Poistaa lopullisesti ennen [cutoff]ia poistetuiksi merkityt (30 päivän siivous). Ainoa
     * kova poisto jonka DAO tarjoaa: se koskee vain rivejä jotka käyttäjä on jo poistanut,
     * joten mikään arkistossa näkyvä ei voi kadota tätä kautta. Kaikki tilit kerralla, koska
     * aika on laitteen eikä tilin.
     *
     * @return lopullisesti poistettujen rivien määrä.
     */
    @Query("DELETE FROM messages WHERE deletedAtEpochMillis IS NOT NULL AND deletedAtEpochMillis < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long): Int
}
