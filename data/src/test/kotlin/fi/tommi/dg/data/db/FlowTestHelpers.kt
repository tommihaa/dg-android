package fi.tommi.dg.data.db

import kotlinx.coroutines.flow.first

/**
 * Flow'n ensimmäinen arvo. Roomin palauttama Flow ei pääty itsestään, joten testi jäisi
 * odottamaan ilman [first]-rajausta.
 */
suspend fun MessageDao.observeAllOnce(account: String = "tommih"): List<MessageEntity> =
    observeAll(account).first()

suspend fun MessageDao.observeByMatchOnce(matchId: String): List<MessageEntity> =
    observeByMatch(matchId).first()

suspend fun MessageDao.newestStoredAtOnce(account: String = "tommih"): Long? =
    observeNewestStoredAt(account).first()

suspend fun PendingActionDao.observeOldestFirstOnce(): List<PendingActionEntity> =
    observeOldestFirst().first()

suspend fun PendingActionDao.observeByMatchOnce(matchId: String): List<PendingActionEntity> =
    observeByMatch(matchId).first()
