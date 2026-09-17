package fi.tommi.dg.data

import fi.tommi.dg.data.db.ReminderDao
import fi.tommi.dg.data.db.toDomain
import fi.tommi.dg.data.db.toEntity
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Muistutukset, eli pelaajan omat muistiinpanot pelin ajaksi.
 *
 * **Tämä ei kosketa sivustoon eikä voi koskea.** Toteutus saa vain DAO:n, joten se on
 * signatuurista luettavissa samalla tavalla kuin `FormSender`in puuttuminen kertoo
 * näkymämallista. Muistutus on sovelluksen omaa tietoa alusta loppuun, ks.
 * [fi.tommi.dg.domain.Reminder].
 *
 * **Poisto on olemassa, toisin kuin [MessageArchive]ssa**, eikä ero ole epäjohdonmukaisuus.
 * Viesti on ainoa kopio siitä mitä sivusto ei säilytä, joten sen poistaminen olisi
 * peruuttamatonta tiedon hävitystä. Muistutuksen kirjoitti käyttäjä itse ja hän voi
 * kirjoittaa sen uudestaan, ja rivin poisto on nimenomaan se teko jota häneltä odotetaan
 * kun muistutus on hoidettu.
 *
 * Rajapinta samasta syystä kuin [MessageArchive]lla (1.9.2026, auditoinnin H6).
 */
interface ReminderBook {

    /**
     * Yhden pelin muistutukset. Elinkaari on tässä kyselyssä eikä ajastuksessa, ks.
     * `ReminderDao.observeByGame`.
     */
    fun observe(game: GameKey): Flow<List<Reminder>>

    /**
     * Kirjoittaa muistutuksen ja siivoaa saman ottelun vanhentuneet rivit.
     *
     * Siivous on tässä eikä laudan latauksessa, ja perustelu on `ReminderDao.deleteOtherGames`
     * issa: poisto tapahtuu vain siinä ottelussa jossa käyttäjä on juuri itse kirjoittanut,
     * eikä koskaan pelkän jäsennetyn pisteluvun nojalla.
     *
     * Tyhjä teksti ei tallennu. Se ei ole validointia vaan sama sääntö kuin tyhjällä
     * vastauksella viestiruudussa: teko jolla ei ole sisältöä ei ole teko.
     *
     * @return kirjattiinko rivi.
     */
    suspend fun add(game: GameKey, text: String, atEpochMillis: Long): Boolean

    suspend fun remove(id: Long)

    suspend fun count(): Int

    /** Kaikki muistutukset, viennin lähde (`ArchiveFile.reminders`). */
    suspend fun all(): List<Reminder>

    /**
     * Tuonti: kirjoittaa ne muistutukset joita kannassa ei vielä ole, eikä koske muihin.
     *
     * **Vain lisää, ei siivoa** (Tommin päätös 16.9.2026: tuonti vain lisää eikä koskaan
     * korvaa). [add]in siivous saman ottelun muista peleistä ei laukea tässä, koska
     * tuotu rivi ei ole käyttäjän juuri kirjoittama eikä siten todiste siitä mikä peli on
     * meneillään. Vanhentunut rivi jää kantaan näkymättömänä, kuten se olisi jäänyt
     * ilman tuontiakin; elinkaari on kyselyssä (`ReminderDao.observeByGame`).
     *
     * @return kuinka monta riviä kirjoitettiin.
     */
    suspend fun addMissing(reminders: List<Reminder>): Int
}

class RoomReminderBook(
    private val dao: ReminderDao,
) : ReminderBook {

    override fun observe(game: GameKey): Flow<List<Reminder>> =
        dao.observeByGame(game.matchId.value, game.opponentScore, game.selfScore)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(game: GameKey, text: String, atEpochMillis: Long): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        dao.insert(
            Reminder(
                id = 0,
                game = game,
                text = trimmed,
                createdAtEpochMillis = atEpochMillis,
            ).toEntity(),
        )
        dao.deleteOtherGames(game.matchId.value, game.opponentScore, game.selfScore)
        return true
    }

    override suspend fun remove(id: Long) = dao.delete(id)

    override suspend fun count(): Int = dao.count()

    override suspend fun all(): List<Reminder> = dao.all().map { it.toDomain() }

    override suspend fun addMissing(reminders: List<Reminder>): Int {
        var added = 0
        for (reminder in reminders) {
            val text = reminder.text.trim()
            if (text.isEmpty()) continue
            val game = reminder.game
            val same = dao.countSame(
                game.matchId.value, game.opponentScore, game.selfScore, text, reminder.createdAtEpochMillis,
            )
            if (same > 0) continue
            dao.insert(reminder.copy(id = 0, text = text).toEntity())
            added++
        }
        return added
    }
}
