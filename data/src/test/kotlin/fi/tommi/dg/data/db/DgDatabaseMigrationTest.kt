package fi.tommi.dg.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migraatiot ajettuina oikeaa SQLiteä vasten.
 *
 * **Testi on kirjoitettu raa'an SQLiten tasolle eikä `MigrationTestHelper`illa**, ja se on
 * valinta eikä puute. Helper vaatisi oman riippuvuutensa ja Roomin viedyn skeeman
 * lataamisen testiassetina, ja se todentaisi lisäksi eri asian: että kanta vastaa vietyä
 * skeemaa. Sen todistaa jo Roomin oma käännösaikainen tarkistus. Se mitä *tämä* migraatio
 * voi rikkoa on rivien säilyminen, ja se on juuri se mitä tässä ajetaan.
 *
 * Taulu rakennetaan käsin version 1 muodossa. Se on tarkoituksellinen toiste
 * `schemas/1.json`ista: jos se joskus eroaa, ero on tässä testissä näkyvä eikä
 * käyttäjän kannassa piilevä.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DgDatabaseMigrationTest {

    private var helper: SupportSQLiteOpenHelper? = null

    @After
    fun sulje() {
        helper?.close()
    }

    @Test
    fun `migraatio lisaa otsikkosarakkeen ja sailyttaa rivit`() {
        val db = avaaVersio1()

        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('tiiviste', '5302842', 'vastustaja', 'Jul 29 2026 20:14',
                    'Good roll', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)

        db.query("SELECT body, rawHeader FROM messages WHERE id = 'tiiviste'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Good roll", rivi.getString(0))
            // Tyhjä eikä null: sarake on ei-null, ja tyhjä tarkoittaa muuallakin sitä
            // ettei sivu kertonut otsikkoa. Vanha rivi ei siis väitä otsikosta mitään.
            assertEquals("", rivi.getString(1))
        }
    }

    @Test
    fun `toinen migraatio uudistaa jonon eika koske viesteihin`() {
        // Migraatio 2 → 3 on kokoelman ainoa taulun pudottava migraatio, ja sen ehto on että
        // pudotettava taulu on kirjoittajaton. Testi väittää ehdon molemmat puolet: jono saa
        // uuden muodon, ja viestirivi on koskematon sen jälkeen.
        val db = avaaVersio1()
        db.execSQL(
            """
            CREATE TABLE pending_actions (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                matchId TEXT,
                path TEXT NOT NULL,
                label TEXT NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                attempts INTEGER NOT NULL,
                lastErrorText TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('tiiviste', '5302842', 'vastustaja', 'Jul 29 2026 20:14',
                    'Good roll', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_2_3.migrate(db)

        db.execSQL(
            """
            INSERT INTO pending_actions
                (matchId, boardPath, submit, pendingMove, verified,
                 createdAtEpochMillis, attempts, lastErrorText)
            VALUES ('5302842', '/bg/move/5302842/561', 'Submit Move', 'rrmm', 0, 1000, 0, NULL)
            """.trimIndent(),
        )

        db.query("SELECT boardPath, submit, pendingMove FROM pending_actions").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("/bg/move/5302842/561", rivi.getString(0))
            assertEquals("Submit Move", rivi.getString(1))
            assertEquals("rrmm", rivi.getString(2))
        }
        db.query("SELECT body FROM messages WHERE id = 'tiiviste'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Good roll", rivi.getString(0))
        }
    }

    @Test
    fun `kolmas migraatio lisaa vastustajan ja jattaa vanhan rivin nimettomaksi`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('tiiviste', '5302842', 'vastustaja', 'Jul 29 2026 20:14',
                    'Good roll', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)

        db.query("SELECT body, opponent FROM messages WHERE id = 'tiiviste'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            // Rivi säilyi: tämä on se mitä migraatio voi rikkoa, ja `messages` on ainoa
            // kopio viestihistoriasta.
            assertEquals("Good roll", rivi.getString(0))
            // **Null eikä lähettäjän nimi.** Vanhalla rivillä kumppania ei tiedetty
            // tallennushetkellä, ja arvaus `sender`istä näyttäisi jälkikäteen mitatulta
            // tiedolta. Ero tyhjään merkkijonoon on tässä merkitsevä, toisin kuin
            // otsikkosarakkeella yllä.
            assertEquals(true, rivi.isNull(1))
        }
    }

    @Test
    fun `vastustajan haku loytaa vain oman rivinsa`() {
        val db = avaaVersio1()
        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)

        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, opponent, sender, timestampText, body, rawHeader, source,
                 storedAtEpochMillis)
            VALUES ('a', '5302842', 'vastustaja', 'vastustaja', '', 'Good roll', '',
                    'GAME_MESSAGE', 1000),
                   ('b', '5302999', 'toinen', 'toinen', '', 'Hi', '',
                    'GAME_MESSAGE', 2000)
            """.trimIndent(),
        )

        db.query("SELECT body FROM messages WHERE opponent = 'vastustaja'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Good roll", rivi.getString(0))
        }
    }

    @Test
    fun `neljas migraatio lisaa muistutustaulun eika koske viesteihin`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('tiiviste', '5302842', 'vastustaja', 'Jul 29 2026 20:14',
                    'Good roll', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)
        DgDatabase.MIGRATION_4_5.migrate(db)

        db.execSQL(
            """
            INSERT INTO reminders
                (matchId, opponentScore, selfScore, text, createdAtEpochMillis)
            VALUES ('5302842', 8, 7, 'Think about doubling', 1000)
            """.trimIndent(),
        )

        db.query(
            "SELECT text FROM reminders WHERE matchId = '5302842' AND opponentScore = 8 AND selfScore = 7"
        ).use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Think about doubling", rivi.getString(0))
        }
        // Viestirivi on koskematon. Uusi taulu ei ole syy tarkistaa tätä, vaan tapa jolla
        // tämä kanta on suojattu: jokainen migraatio väittää erikseen ettei se koskenut
        // ainoaan kopioon viestihistoriasta.
        db.query("SELECT body FROM messages WHERE id = 'tiiviste'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Good roll", rivi.getString(0))
        }
    }

    @Test
    fun `viides migraatio lisaa vastauksen kohteen ja jattaa vanhat rivit ilman sita`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('vanha', NULL, 'vastustaja', '', 'Great match!', 'QUICK_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)
        DgDatabase.MIGRATION_4_5.migrate(db)
        DgDatabase.MIGRATION_5_6.migrate(db)

        // **Vanha rivi jää nulliksi, ja se on oikea vastaus eikä puute.** Yhteys syntyy vain
        // lähetyshetkellä, koska sivustolla ei ole viestiketjuja. Mikä tahansa täydennetty
        // arvo olisi keksitty.
        db.query("SELECT replyToId FROM messages WHERE id = 'vanha'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertTrue(rivi.isNull(0))
        }

        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis, replyToId)
            VALUES ('uusi', NULL, 'mina', '', 'You too', 'QUICK_MESSAGE', 2000, 'vanha')
            """.trimIndent(),
        )

        // Kysely toiseen suuntaan on se jota ruutu tekee: onko tähän viestiin vastattu.
        db.query("SELECT id FROM messages WHERE replyToId = 'vanha'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("uusi", rivi.getString(0))
        }
        // Sama väite kuin joka migraatiossa: viestihistorian ainoaan kopioon ei koskettu.
        db.query("SELECT body FROM messages WHERE id = 'vanha'").use { rivi ->
            rivi.moveToFirst()
            assertEquals("Great match!", rivi.getString(0))
        }
    }

    @Test
    fun `kuudes migraatio lisaa vastauslomakkeen ja jattaa vanhat rivit ilman sita`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('vanha', NULL, 'vastapelaaja', '', 'Great match!', 'QUICK_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)
        DgDatabase.MIGRATION_5_6.migrate(db)
        DgDatabase.MIGRATION_6_7.migrate(db)

        // **Vanha rivi jää nulliksi, ja se on oikea vastaus eikä puute.** Lomaketta ei luettu
        // talteen silloin kun sivu oli käsillä, eikä viestisivua saa takaisin. Kokoaminen
        // lähettäjästä olisi keksitty osoite.
        db.query(
            "SELECT replyAction, replyMethod, replyField, replyMaxLength FROM messages WHERE id = 'vanha'"
        ).use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertTrue(rivi.isNull(0))
            assertTrue(rivi.isNull(1))
            assertTrue(rivi.isNull(2))
            assertTrue(rivi.isNull(3))
        }

        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis,
                 replyAction, replyMethod, replyField, replyMaxLength)
            VALUES ('uusi', NULL, 'vastapelaaja', '', 'Hi', 'QUICK_MESSAGE', 2000,
                    '/bg/sendmsg/91004', 'POST', 'text', 80)
            """.trimIndent(),
        )

        db.query("SELECT replyAction FROM messages WHERE id = 'uusi'").use { rivi ->
            rivi.moveToFirst()
            assertEquals("/bg/sendmsg/91004", rivi.getString(0))
        }
        // Sama väite kuin joka migraatiossa: viestihistorian ainoaan kopioon ei koskettu.
        db.query("SELECT body FROM messages WHERE id = 'vanha'").use { rivi ->
            rivi.moveToFirst()
            assertEquals("Great match!", rivi.getString(0))
        }
    }

    @Test
    fun `yhdeksas migraatio lisaa tilin ja jattaa vanhan rivin omistajattomaksi`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('vanha', '5302842', 'vastustaja', '', 'Great match!', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)
        DgDatabase.MIGRATION_9_10.migrate(db)

        // **Vanha rivi jää nulliksi, ja se on oikea vastaus eikä puute.** Migraatio ei tiedä
        // tilin nimeä, koska tunnukset eivät ole kannassa. Nimen antaa ensimmäinen
        // kirjautuminen (`MessageDao.claimUnowned`), ei tämä.
        db.query("SELECT account FROM messages WHERE id = 'vanha'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertTrue(rivi.isNull(0))
        }

        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis, account)
            VALUES ('uusi', '5302842', 'vastustaja', '', 'gg', 'GAME_MESSAGE', 2000, 'tommih')
            """.trimIndent(),
        )

        // Ruudun kysely on tilillä rajattu, ja sen on löydettävä vain oma rivinsä.
        db.query("SELECT id FROM messages WHERE account = 'tommih'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("uusi", rivi.getString(0))
        }
        // Sama väite kuin joka migraatiossa: viestihistorian ainoaan kopioon ei koskettu.
        db.query("SELECT body FROM messages WHERE id = 'vanha'").use { rivi ->
            rivi.moveToFirst()
            assertEquals("Great match!", rivi.getString(0))
        }
    }

    @Test
    fun `kymmenes migraatio lisaa aseman ja jattaa vanhan merkin ilman sita`() {
        val db = avaaVersio1()
        DgDatabase.MIGRATION_8_9.migrate(db)
        db.execSQL(
            """
            INSERT INTO marked_positions
                (matchId, opponentScore, selfScore, moveNumber, note, createdAtEpochMillis)
            VALUES ('5316472', 1, 5, 377, 'recube after rollback?', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_10_11.migrate(db)

        // Vanha merkki jää ilman asemaa, koska asemaa ei voi laskea jälkikäteen; vienti
        // kirjoittaa sen pelin alkuun kuten ennen saraketta.
        db.query("SELECT position, note FROM marked_positions WHERE moveNumber = 377").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertTrue(rivi.isNull(0))
            assertEquals("recube after rollback?", rivi.getString(1))
        }
        db.execSQL(
            """
            INSERT INTO marked_positions
                (matchId, opponentScore, selfScore, moveNumber, note, createdAtEpochMillis, position)
            VALUES ('5316472', 1, 5, 380, '', 2000, '1:0,0,0,0,0,0,5,0,3,0,0,0,0,5,0,0,0,0,0,0,0,0,0,0,2,0/0,0,0,0,0,0,5,0,3,0,0,0,0,5,0,0,0,0,0,0,0,0,0,0,2,0')
            """.trimIndent(),
        )
        db.query("SELECT position FROM marked_positions WHERE moveNumber = 380").use { rivi ->
            rivi.moveToFirst()
            assertTrue(rivi.getString(0).startsWith("1:0,0,0,0,0,0,5,"))
        }
    }

    @Test
    fun `yhdestoista migraatio lisaa katkohistorian eika koske viesteihin`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('tiiviste', '5302842', 'vastustaja', 'Jul 29 2026 20:14',
                    'Good roll', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_11_12.migrate(db)

        db.execSQL(
            """
            INSERT INTO connection_drops (atEpochMillis, matchId, submit, cause)
            VALUES (1000, '5302842', 'Submit Move', 'SocketTimeoutException')
            """.trimIndent(),
        )
        db.query("SELECT submit, cause FROM connection_drops").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Submit Move", rivi.getString(0))
            assertEquals("SocketTimeoutException", rivi.getString(1))
        }
        db.query("SELECT body FROM messages WHERE id = 'tiiviste'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Good roll", rivi.getString(0))
        }
    }

    @Test
    fun `kahdeksas migraatio lisaa merkityt asemat eika koske viesteihin`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('tiiviste', '5316472', 'opponent', 'Sep 15 2026 22:42',
                    'gg', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)
        DgDatabase.MIGRATION_8_9.migrate(db)

        db.execSQL(
            """
            INSERT INTO marked_positions
                (matchId, opponentScore, selfScore, moveNumber, note, createdAtEpochMillis)
            VALUES ('5316472', 1, 5, 377, 'recube after rollback?', 1000)
            """.trimIndent(),
        )
        // Saman pelin toinen merkki on toinen rivi: pääavain on juokseva eikä peli.
        db.execSQL(
            """
            INSERT INTO marked_positions
                (matchId, opponentScore, selfScore, moveNumber, note, createdAtEpochMillis)
            VALUES ('5316472', 1, 5, 380, '', 2000)
            """.trimIndent(),
        )

        db.query("SELECT moveNumber FROM marked_positions WHERE matchId = '5316472' ORDER BY id").use { rivi ->
            assertEquals(2, rivi.count)
            rivi.moveToFirst()
            assertEquals(377, rivi.getInt(0))
        }
        db.query("SELECT body FROM messages WHERE id = 'tiiviste'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("gg", rivi.getString(0))
        }
    }

    /**
     * `messages` sellaisena kuin se oli versiossa 1. Muut taulut jätetään pois, koska
     * migraatio ei koske niitä eikä tässä ajeta Roomia vaan yhtä `ALTER TABLE`a.
     */
    @Test
    fun `seitsemas migraatio lisaa ottelumuistin eika koske viesteihin`() {
        val db = avaaVersio1()
        db.execSQL(
            """
            INSERT INTO messages
                (id, matchId, sender, timestampText, body, source, storedAtEpochMillis)
            VALUES ('tiiviste', '5302842', 'vastustaja', 'Jul 29 2026 20:14',
                    'Good roll', 'GAME_MESSAGE', 1000)
            """.trimIndent(),
        )

        DgDatabase.MIGRATION_1_2.migrate(db)
        DgDatabase.MIGRATION_3_4.migrate(db)
        DgDatabase.MIGRATION_7_8.migrate(db)

        db.execSQL(
            """
            INSERT INTO seen_matches
                (matchId, opponentName, opponentId, opponentPath, round, matchLength, eventName,
                 seenAtEpochMillis)
            VALUES ('5302842', 'vastustaja', '90002', '/bg/user/90002', '4/5', 21,
                    'The Marathon #4305', 1000)
            """.trimIndent(),
        )
        // Sama numero toistamiseen korvaa rivin eikä lisää toista: pääavain on ottelu.
        db.execSQL(
            """
            INSERT OR REPLACE INTO seen_matches
                (matchId, opponentName, opponentId, opponentPath, round, matchLength, eventName,
                 seenAtEpochMillis)
            VALUES ('5302842', 'vastustaja', '90002', '/bg/user/90002', '5/5', 21,
                    'The Marathon #4305', 2000)
            """.trimIndent(),
        )

        db.query("SELECT round FROM seen_matches WHERE matchId = '5302842'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("5/5", rivi.getString(0))
        }
        db.query("SELECT body FROM messages WHERE id = 'tiiviste'").use { rivi ->
            assertEquals(1, rivi.count)
            rivi.moveToFirst()
            assertEquals("Good roll", rivi.getString(0))
        }
    }

    private fun avaaVersio1(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val avaaja = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                // Nimetön kanta on muistissa, jolloin testien välille ei jää tiedostoa.
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE messages (
                                id TEXT NOT NULL PRIMARY KEY,
                                matchId TEXT,
                                sender TEXT NOT NULL,
                                timestampText TEXT NOT NULL,
                                body TEXT NOT NULL,
                                source TEXT NOT NULL,
                                storedAtEpochMillis INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, from: Int, to: Int) = Unit
                })
                .build(),
        )
        helper = avaaja
        return avaaja.writableDatabase
    }
}
