package fi.tommi.dg.data.db

import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Reminder

internal fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    game = GameKey(
        matchId = MatchId(matchId),
        opponentScore = opponentScore,
        selfScore = selfScore,
    ),
    text = text,
    createdAtEpochMillis = createdAtEpochMillis,
)

internal fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    matchId = game.matchId.value,
    opponentScore = game.opponentScore,
    selfScore = game.selfScore,
    text = text,
    createdAtEpochMillis = createdAtEpochMillis,
)
