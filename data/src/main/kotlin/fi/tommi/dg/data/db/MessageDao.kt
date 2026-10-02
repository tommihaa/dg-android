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
    @Query("SELECT * FROM messages WHERE account = :account ORDER BY storedAtEpochMillis DESC, id ASC")
    fun observeAll(account: String?): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE matchId = :matchId ORDER BY storedAtEpochMillis ASC, id ASC")
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
    @Query("SELECT * FROM messages WHERE account = :account AND opponent = :opponent ORDER BY storedAtEpochMillis ASC, id ASC")
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
        WHERE account = :account AND opponent IS NOT NULL
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
    @Query("SELECT MAX(storedAtEpochMillis) FROM messages WHERE account = :account")
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

    // Poistoa ei ole tarkoituksella. Viesti on muuttumaton tapahtuma, ja tämä kanta on
    // ainoa paikka jossa se on olemassa. Jos siivous joskus tarvitaan, se on tietoinen
    // ominaisuus eikä DAO:n oletusvalikoimaa.
}
