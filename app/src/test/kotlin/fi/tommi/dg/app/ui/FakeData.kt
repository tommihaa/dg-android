package fi.tommi.dg.app.ui

import fi.tommi.dg.app.session.SiteSettingsStore
import fi.tommi.dg.data.ActionQueue
import fi.tommi.dg.data.MessageArchive
import fi.tommi.dg.data.MarkBook
import fi.tommi.dg.data.ReminderBook
import fi.tommi.dg.domain.CheckerPosition
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.PendingAction
import fi.tommi.dg.domain.MarkedPosition
import fi.tommi.dg.domain.Reminder
import fi.tommi.dg.domain.SiteBoardSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/*
 * Datakerroksen kolme säilöä muistissa.
 *
 * **Sauma on fasadissa eikä Roomin DAO:ssa (1.9.2026, auditoinnin H6).** Aiemmin nämä olivat
 * kolme erillistä `MessageDao`-, `PendingActionDao`- ja `ReminderDao`-toteutusta kolmessa
 * testitiedostossa, eli näkymämallin testi tunsi `MessageEntity`n ja Roomin kyselynimet
 * vaikka näkymämalli itse ei tunne kumpaakaan. Nyt fake toteuttaa sen saman rajapinnan jota
 * tuotantokoodikin käyttää, ja rivit ovat verkkotunnuksen tyyppejä.
 *
 * Room ei ole näissä testeissä tarpeen: väitteet koskevat sitä **kirjattiinko ja mitä**,
 * eivät SQL:ää. Oikean kannan kattavat `MessageArchiveTest`, `PendingActionDaoTest` ja
 * `ReminderBookTest` `data`-moduulissa.
 */

/**
 * Viestiarkisto muistissa. Ei kaksoiskappaleiden karsintaa: testi haluaa nähdä myös ne.
 *
 * Tilirajaus ei ole tämän faken väite vaan `MessageArchiveTest`in (`data`): tässä kaikki
 * rivit näkyvät tilistä riippumatta, ja [accounts] kirjaa millä tilillä kukin kirjoitettiin,
 * jotta näkymämallin testi voi todeta että tili välitettiin.
 */
class FakeArchive : MessageArchive {
    val rows = MutableStateFlow<List<Message>>(emptyList())

    /** Kirjoitusten tilit samassa järjestyksessä kuin [rows]. */
    val accounts = mutableListOf<String?>()

    override suspend fun archive(messages: List<Message>, account: String?): List<Message> {
        rows.value = rows.value + messages
        repeat(messages.size) { accounts += account }
        return messages
    }

    override suspend fun addMissing(messages: List<Message>, account: String?): Int {
        val known = rows.value.map { it.id }.toSet()
        val fresh = messages.filter { it.id !in known }
        archive(fresh, account)
        return fresh.size
    }

    override fun observeAll(account: String?): Flow<List<Message>> = rows

    override fun observeByMatch(matchId: String): Flow<List<Message>> = rows

    override fun observeByOpponent(opponent: String, account: String?): Flow<List<Message>> = rows

    override fun observeOpponents(account: String?): Flow<List<String>> =
        rows.map { list -> list.mapNotNull { it.opponent }.distinct() }

    override suspend fun count(): Int = rows.value.size

    override suspend fun claimUnowned(account: String): Int = 0

    override fun observeNewestStoredAt(account: String?): Flow<Long?> = MutableStateFlow(null)
}

/** Lähtevien tekojen jono muistissa, ottelukohtainen korvaussääntö mukaan lukien. */
class FakeQueue : ActionQueue {
    val rows = MutableStateFlow<List<PendingAction>>(emptyList())
    private var nextId = 1L

    override suspend fun record(action: PendingAction): Long {
        val id = nextId++
        // Sama sääntö kuin oikealla jonolla: ottelulla on kerrallaan enintään yksi rivi.
        val others = rows.value.filterNot { it.matchId != null && it.matchId == action.matchId }
        rows.value = others + action.copy(id = id)
        return id
    }

    override fun observeByMatch(matchId: MatchId): Flow<PendingAction?> =
        rows.map { list -> list.firstOrNull { it.matchId == matchId } }

    override fun observeAll(): Flow<List<PendingAction>> = rows

    override suspend fun clear(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun count(): Int = rows.value.size
}

/** Muistutukset muistissa. Elinkaari on kyselyssä, samoin kuin oikeassa kirjassa. */
class FakeReminders : ReminderBook {
    val rows = MutableStateFlow<List<Reminder>>(emptyList())
    private var nextId = 1L

    override fun observe(game: GameKey): Flow<List<Reminder>> =
        rows.map { list -> list.filter { it.game == game } }

    override suspend fun add(game: GameKey, text: String, atEpochMillis: Long): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val rivi = Reminder(
            id = nextId++,
            game = game,
            text = trimmed,
            createdAtEpochMillis = atEpochMillis,
        )
        // Saman ottelun muut pelit siivotaan, kuten oikeassa kirjassa: poisto tapahtuu vain
        // siinä ottelussa jossa käyttäjä on juuri itse kirjoittanut.
        val kept = rows.value.filterNot { it.game.matchId == game.matchId && it.game != game }
        rows.value = kept + rivi
        return true
    }

    override suspend fun remove(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun count(): Int = rows.value.size

    override suspend fun all(): List<Reminder> = rows.value

    override suspend fun addMissing(reminders: List<Reminder>): Int {
        var added = 0
        for (r in reminders) {
            val same = rows.value.any {
                it.game == r.game && it.text == r.text && it.createdAtEpochMillis == r.createdAtEpochMillis
            }
            if (same || r.text.isBlank()) continue
            rows.value = rows.value + r.copy(id = nextId++)
            added++
        }
        return added
    }
}

/** Merkityt asemat muistissa. Ei siivousta, kuten oikeassa kirjassa: vain poisto vie rivin. */
class FakeMarks : MarkBook {
    val rows = MutableStateFlow<List<MarkedPosition>>(emptyList())
    private var nextId = 1L

    override fun observe(game: GameKey): Flow<List<MarkedPosition>> =
        rows.map { list -> list.filter { it.game == game } }

    override fun observeAll(): Flow<List<MarkedPosition>> = rows

    override suspend fun add(game: GameKey, moveNumber: Int, note: String, atEpochMillis: Long, position: CheckerPosition?): Long {
        val id = nextId++
        rows.value = rows.value + MarkedPosition(
            id = id,
            game = game,
            moveNumber = moveNumber,
            note = note.trim(),
            createdAtEpochMillis = atEpochMillis,
            position = position,
        )
        return id
    }

    override suspend fun remove(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun count(): Int = rows.value.size
}

/**
 * Laitteen kopio sivuston lauta-asetuksista muistissa.
 *
 * Oma luokka eikä konstruktorin `null` (1.9.2026, auditoinnin H6): näkymämallin on saatava
 * säilö, jotta kytkennän unohtaminen näkyy käännösvirheenä eikä laudan hiljaisena
 * vanhentumisena.
 */
class FakeSiteSettings(private var stored: SiteBoardSettings = SiteBoardSettings.UNKNOWN) :
    SiteSettingsStore {
    val saved = mutableListOf<SiteBoardSettings>()

    override fun get(): SiteBoardSettings = stored

    override fun save(settings: SiteBoardSettings) {
        stored = settings
        saved += settings
    }
}
