package fi.tommi.dg.data.db

import fi.tommi.dg.domain.CheckerPosition
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.MarkedPosition
import fi.tommi.dg.domain.MatchId

internal fun MarkedPositionEntity.toDomain(): MarkedPosition = MarkedPosition(
    id = id,
    game = GameKey(
        matchId = MatchId(matchId),
        opponentScore = opponentScore,
        selfScore = selfScore,
    ),
    moveNumber = moveNumber,
    note = note,
    createdAtEpochMillis = createdAtEpochMillis,
    position = position?.let { CheckerPosition.decode(it) },
)

internal fun MarkedPosition.toEntity(): MarkedPositionEntity = MarkedPositionEntity(
    id = id,
    matchId = game.matchId.value,
    opponentScore = game.opponentScore,
    selfScore = game.selfScore,
    moveNumber = moveNumber,
    note = note,
    createdAtEpochMillis = createdAtEpochMillis,
    position = position?.encode(),
)
