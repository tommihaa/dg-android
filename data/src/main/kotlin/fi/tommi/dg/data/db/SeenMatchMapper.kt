package fi.tommi.dg.data.db

import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.SeenMatch

internal fun SeenMatchEntity.toDomain(): SeenMatch = SeenMatch(
    matchId = MatchId(matchId),
    opponent = PlayerRef(name = opponentName, userId = opponentId, profilePath = opponentPath),
    round = round,
    matchLength = matchLength,
    eventName = eventName,
    seenAtEpochMillis = seenAtEpochMillis,
)

internal fun SeenMatch.toEntity(): SeenMatchEntity = SeenMatchEntity(
    matchId = matchId.value,
    opponentName = opponent.name,
    opponentId = opponent.userId,
    opponentPath = opponent.profilePath,
    round = round,
    matchLength = matchLength,
    eventName = eventName,
    seenAtEpochMillis = seenAtEpochMillis,
)
