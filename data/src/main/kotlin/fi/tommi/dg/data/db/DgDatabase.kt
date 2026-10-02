package fi.tommi.dg.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Sovelluksen kanta.
 *
 * Tässä ei ole `fallbackToDestructiveMigration`ia eikä tule. Se on tavallinen oletus
 * kehityksen aikana, mutta tässä sovelluksessa se poistaisi ainoan olemassa olevan kopion
 * viestihistoriasta: sivusto ei säilytä viestejä, joten pudotettua taulua ei voi hakea
 * uudestaan mistään. Skeemamuutos vaatii siis kirjoitetun migraation, myös silloin kun
 * versio ei ole vielä julkaistu.
 */
@Database(
    entities = [
        MessageEntity::class,
        PendingActionEntity::class,
        ReminderEntity::class,
        SeenMatchEntity::class,
        MarkedPositionEntity::class,
        ConnectionDropEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
abstract class DgDatabase : RoomDatabase() {

    abstract fun messages(): MessageDao

    abstract fun pendingActions(): PendingActionDao

    abstract fun reminders(): ReminderDao

    abstract fun seenMatches(): SeenMatchDao

    abstract fun markedPositions(): MarkedPositionDao

    abstract fun connectionDrops(): ConnectionDropDao

    companion object {
        const val FILE_NAME = "dg.db"

        /**
         * `messages.rawHeader`, eli sivun oma otsikkorivi omana sarakkeenaan
         * (päätös 9.8.2026, ks. [fi.tommi.dg.domain.Message.rawHeader]).
         *
         * **Migraatio on kirjoitettu vaikka mitään ei ole siirrettävänä.** Kannassa ei
         * ollut päätöshetkellä yhtään viestiä, joten tyhjentävä pudotus olisi tuottanut
         * saman lopputuloksen tänään. Se ei tee siitä oikeaa: kannan ainoa suoja on että
         * jokainen skeemamuutos on kirjoitettu, ja poikkeus jonka perustelu on
         * "juuri tästä ei ollut haittaa" kuluu pois ensimmäisen tallennetun viestin
         * jälkeen, hiljaa ja huomaamatta.
         *
         * Oletusarvo on tyhjä merkkijono eikä null, koska sarake on ei-null. Tyhjä
         * tarkoittaa tässä samaa kuin muuallakin: sivu ei kertonut otsikkoa. Yhtään
         * riviä ei ole olemassa, joten väite ei koske mitään olemassa olevaa dataa.
         *
         * Huom mitä migraatio **ei** tee: se ei laske viestien tunnisteita uudelleen.
         * [fi.tommi.dg.domain.Message.id] on sisältötiiviste ja uusi kenttä muuttaa sen,
         * joten migroitu rivi olisi pääavaimeltaan vanhentunut. Tämä on turvallista tasan
         * niin kauan kuin taulu on tyhjä, ja siksi muutos tehtiin nyt eikä myöhemmin.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN rawHeader TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * `pending_actions` uuteen muotoon: rivi kuvaa painallusta eikä valmista osoitetta
         * (päätös 10.8.2026, ks. [PendingActionEntity] ja
         * [fi.tommi.dg.domain.PendingAction]).
         *
         * **Tämä on kokoelman ainoa migraatio joka pudottaa taulun, ja ehto sille on
         * kirjoitettava auki.** Sarakkeiden merkitys muuttui kokonaan: vanha `path` oli
         * valmis osoite, ja siitä ei voi johtaa napin nimeä eikä kokoamistilaa takaisin.
         * Siirrettävää ei siis ole edes periaatteessa.
         *
         * Turvallisuus ei silti lepää sen varassa vaan siinä, että **taulu on ollut
         * kirjoittajaton koko olemassaolonsa ajan**: yksikään tuotantokoodin kutsupaikka ei
         * ole lisännyt siihen riviä, vaan ainoat kirjoitukset ovat tulleet tämän moduulin
         * omista testeistä. Ehto on tarkistettavissa hakemalla `enqueue`n kutsujat.
         *
         * `messages` ei kuulu tämän piiriin eikä koskaan kuulu. Se on ainoa kopio
         * viestihistoriasta, koska sivusto ei säilytä viestejä, ja siksi sitä koskeva
         * skeemamuutos on aina rivit säilyttävä. Ero näiden kahden välillä on juuri se syy
         * miksi `fallbackToDestructiveMigration` on poissa: se ei osaa erottaa taulua jonka
         * saa pudottaa siitä jota ei saa.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS pending_actions")
                db.execSQL(
                    """
                    CREATE TABLE pending_actions (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        matchId TEXT,
                        boardPath TEXT NOT NULL,
                        submit TEXT NOT NULL,
                        pendingMove TEXT,
                        verified INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        attempts INTEGER NOT NULL,
                        lastErrorText TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX index_pending_actions_matchId ON pending_actions (matchId)"
                )
            }
        }

        /**
         * `messages.opponent`, eli keskustelukumppani viestin omana sarakkeenaan
         * (päätös 16.8.2026, ks. [fi.tommi.dg.domain.Message.opponent]).
         *
         * **Rivit säilyttävä lisäys, ja se on ehto eikä sattuma.** `messages` on ainoa
         * kopio viestihistoriasta, joten sitä koskeva skeemamuutos ei saa pudottaa taulua
         * eikä laskea pääavaimia uudelleen. Kumpaakaan ei tarvita: sarake on nullable ja
         * uusi, ja [fi.tommi.dg.domain.Message.opponent] on tarkoituksella tiivisteen
         * ulkopuolella, joten jo tallennettujen viestien tunnisteet eivät liiku.
         *
         * **`null` vanhoilla riveillä on oikea arvo eikä puute.** Se tarkoittaa ettei
         * keskustelukumppania tiedetty tallennushetkellä, mikä on täsmälleen totta: tietoa
         * ei ollut olemassa ennen tätä muutosta. Oletusarvo, joka arvaisi nimen
         * [MessageEntity.sender]istä, olisi sovelluksen väite eikä sivun kertoma, ja se
         * näyttäisi jälkikäteen mitatulta tiedolta.
         *
         * Indeksi luodaan samassa migraatiossa, koska se on kyselyn ehto eikä viilaus:
         * pelaajakohtainen haku on arkiston ainoa pääsypolku.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN opponent TEXT")
                db.execSQL("CREATE INDEX index_messages_opponent ON messages (opponent)")
            }
        }

        /**
         * `reminders`, eli muistutus itselle yhden pelin ajaksi
         * (päätös 23.8.2026, ks. [ReminderEntity] ja [fi.tommi.dg.domain.Reminder]).
         *
         * **Uusi taulu eikä muutos vanhaan**, joten mitään ei siirretä eikä pudoteta.
         * `messages` ja `pending_actions` ovat tämän migraation ulkopuolella sanan
         * täydessä merkityksessä: niihin ei kosketa yhdelläkään lauseella.
         *
         * Indeksi luodaan samassa migraatiossa, koska se on kyselyn ehto eikä viilaus:
         * muistutukset haetaan aina yhden pelin tunnisteella
         * ([ReminderDao.observeByGame]), ja juuri se kysely on koko elinkaari.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE reminders (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        matchId TEXT NOT NULL,
                        opponentScore INTEGER NOT NULL,
                        selfScore INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX index_reminders_matchId_opponentScore_selfScore
                    ON reminders (matchId, opponentScore, selfScore)
                    """.trimIndent(),
                )
            }
        }

        /**
         * Vastauksen kohde omaksi sarakkeekseen (24.8.2026).
         *
         * `ALTER TABLE ... ADD COLUMN` ilman oletusarvoa, koska vanhat rivit **eivät** ole
         * vastauksia tuntemattomaan viestiin vaan viestejä joiden yhteyttä ei kirjattu. `NULL`
         * sanoo juuri sen. Tietoa ei voi täydentää jälkikäteen: sivustolla ei ole ketjuja,
         * joten yhteys on olemassa vain jos se syntyi lähetyshetkellä.
         *
         * Indeksi samassa migraatiossa, koska se on kyselyn ehto eikä viilaus: ruutu kysyy
         * onko viestiin vastattu, ja se on haku tällä sarakkeella.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToId TEXT")
                db.execSQL("CREATE INDEX index_messages_replyToId ON messages (replyToId)")
            }
        }

        /**
         * Vastauslomake viestin omiksi sarakkeikseen (30.8.2026, Tommin pyyntö:
         * arkiston viestiin vastataan klikkaamalla, ks. [fi.tommi.dg.domain.Message.replyForm]).
         *
         * Rivit säilyttävä lisäys kuten jokainen `messages`-tauluun koskeva migraatio:
         * neljä nullable-saraketta, ei pääavainten uudelleenlaskentaa. **`NULL` vanhoilla
         * riveillä on oikea arvo eikä puute**: lomaketta ei luettu talteen silloin kun sivu
         * oli käsillä, eikä sitä voi lukea jälkikäteen, koska viestisivu kului lukuhetkellä.
         * Kokoaminen lähettäjän numerosta olisi juuri se osoitteen kokoaminen jota tässä
         * projektissa ei tehdä.
         *
         * Ei indeksiä: sarakkeilla ei haeta, ne vain kulkevat rivin mukana.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN replyAction TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyMethod TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyField TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyMaxLength INTEGER")
            }
        }

        /**
         * `seen_matches`, eli viimeksi nähty vastustaja ja kierros ottelun numerolla
         * (Tommin päätös 15.9.2026, ks. [SeenMatchEntity] ja [fi.tommi.dg.domain.SeenMatch]).
         *
         * **Uusi taulu eikä muutos vanhaan**, joten mitään ei siirretä eikä pudoteta, ja
         * `messages` on tämän ulkopuolella sanan täydessä merkityksessä. Ei indeksiä:
         * pääavain on ottelun numero ja se on ainoa hakuehto.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE seen_matches (
                        matchId TEXT NOT NULL PRIMARY KEY,
                        opponentName TEXT,
                        opponentId TEXT,
                        opponentPath TEXT,
                        round TEXT,
                        matchLength INTEGER,
                        eventName TEXT,
                        seenAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * `marked_positions`, eli merkityt asemat ottelun jälkeistä analyysia varten
         * (Tommin toive 15.9.2026 illalla, ks. [MarkedPositionEntity] ja
         * [fi.tommi.dg.domain.MarkedPosition]).
         *
         * **Uusi taulu eikä muutos vanhaan**, kuten `seen_matches`: mitään ei siirretä eikä
         * pudoteta. Indeksi on ottelulle, koska merkit luetaan ottelun mitalta eikä yhden
         * pelin. Tämä ei ole `reminders`-taulun laajennus, koska elinkaari on eri:
         * muistutus lakkaa pelin päättyessä, merkki vasta käyttäjän poistoon.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE marked_positions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        matchId TEXT NOT NULL,
                        opponentScore INTEGER NOT NULL,
                        selfScore INTEGER NOT NULL,
                        moveNumber INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX index_marked_positions_matchId ON marked_positions (matchId)",
                )
            }
        }

        /**
         * `messages.account`, eli tili jonka kirjautuneena viesti otettiin talteen (Tommin
         * päätös 15.9.2026, toteutus 16.9.2026; ks. [MessageEntity.account]).
         *
         * **Vanhat rivit jäävät `null`iksi tarkoituksella.** Migraatio ei tiedä tilin nimeä,
         * koska tunnukset ovat `:app`in säilössä eivätkä kannassa, ja keksitty arvo olisi
         * väärä tieto oikean muotoisena. Ensimmäinen kirjautuminen migraation jälkeen ottaa
         * ne omikseen ([MessageDao.claimUnowned]); laitteella oli sarakkeen tullessa yksi
         * tili, joten se on oikein.
         *
         * Tunnisteita ei lasketa uudelleen, koska tili ei ole osa niitä. Sama peruste kuin
         * `opponent`-sarakkeella (migraatio 3→4): tunniste on viestin sisällöstä, omistaja
         * tämän laitteen tieto.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN account TEXT")
                db.execSQL("CREATE INDEX index_messages_account ON messages (account)")
            }
        }

        /**
         * `marked_positions.position`, eli asema merkintähetkellä `.sgf`-viennin kohdistusta
         * varten (Tommin tilaus 16.9.2026 illalla, ks. [MarkedPositionEntity.position]).
         *
         * **Vanhat rivit jäävät `null`iksi tarkoituksella**, samasta syystä kuin
         * `messages.account`: asemaa ei voi laskea jälkikäteen, koska sivusto ei säilytä
         * lautaa siirtonumerolla, ja keksitty asema kohdistaisi merkin väärään siirtoon.
         * Null kirjoitetaan viennissä pelin alkuun kuten ennen saraketta.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE marked_positions ADD COLUMN position TEXT")
            }
        }

        /**
         * `connection_drops`, eli katkohistoria: milloin `Board not confirmed` tuli ja miksi
         * (Tommin tilaus 18.9.2026, ks. [ConnectionDropEntity] ja
         * [fi.tommi.dg.domain.ConnectionDrop]).
         *
         * **Uusi taulu eikä muutos vanhaan**, kuten `seen_matches`: mitään ei siirretä eikä
         * pudoteta, ja `messages` on tämän ulkopuolella sanan täydessä merkityksessä. Ei
         * indeksiä: taulu luetaan aina kokonaan uusin ensin, ja sen koko on rajattu sataan.
         * Aiempia katkoja ei voi täydentää, koska niiden ainoa jälki oli jonon rivi joka on
         * jo poistettu; laskuri 41 laitteen kannassa on kaikki mitä niistä jäi.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE connection_drops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        atEpochMillis INTEGER NOT NULL,
                        matchId TEXT,
                        submit TEXT NOT NULL,
                        cause TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Kanta on **yksi olio prosessia kohti**, ja se on oikeellisuutta eikä säästö.
         *
         * Roomin muutostenseuranta elää olion sisällä: kaksi `RoomDatabase`-oliota samaan
         * tiedostoon eivät kerro toisilleen kirjoituksista, joten toisen kautta tehty
         * lisäys ei laukaisisi toisen `Flow`ta. Ruutu jäisi näyttämään vanhaa tilaa ilman
         * virheilmoitusta, eli sama hiljainen vikamuoto jota vastaan tässä projektissa
         * muutenkin suojaudutaan.
         *
         * Tarve syntyi 10.8.2026, kun ulos näkyviä osia tuli kaksi ([DgData]). Yhdellä
         * kutsupaikalla vika ei olisi voinut ilmetä, mutta se ei ollut rakenteen ansiota.
         */
        @Volatile
        private var instance: DgDatabase? = null

        fun open(context: Context): DgDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DgDatabase::class.java,
                    FILE_NAME,
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
