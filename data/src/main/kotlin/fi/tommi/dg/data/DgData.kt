package fi.tommi.dg.data

import android.content.Context
import fi.tommi.dg.data.db.DgDatabase

/**
 * Tämän moduulin ainoa sisäänkäynti.
 *
 * Room jää tarkoituksella tänne: jos `:app` viittaisi [DgDatabase]en, se joutuisi
 * tuntemaan `RoomDatabase`n ja sitä myöten Roomin koko rajapinnan. Silloin tallennustavan
 * vaihto vuotaisi käyttöliittymäkerrokseen asti. Ulos näkyvät vain [MessageArchive],
 * [ActionQueue], [ReminderBook], [MatchMemory], [MarkBook] ja [DropLog], joiden lupaus on tallennustavasta
 * riippumaton.
 */
object DgData {

    fun messageArchive(context: Context): MessageArchive =
        RoomMessageArchive(DgDatabase.open(context).messages())

    fun actionQueue(context: Context): ActionQueue =
        RoomActionQueue(DgDatabase.open(context).pendingActions())

    fun reminderBook(context: Context): ReminderBook =
        RoomReminderBook(DgDatabase.open(context).reminders())

    fun matchMemory(context: Context): MatchMemory =
        RoomMatchMemory(DgDatabase.open(context).seenMatches())

    fun markBook(context: Context): MarkBook =
        RoomMarkBook(DgDatabase.open(context).markedPositions())

    fun dropLog(context: Context): DropLog =
        RoomDropLog(DgDatabase.open(context).connectionDrops())
}
