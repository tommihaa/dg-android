package fi.tommi.dg.data.db

import fi.tommi.dg.domain.ConnectionDrop
import fi.tommi.dg.domain.MatchId

internal fun ConnectionDropEntity.toDomain(): ConnectionDrop = ConnectionDrop(
    id = id,
    atEpochMillis = atEpochMillis,
    matchId = matchId?.let(::MatchId),
    submit = submit,
    cause = cause,
)

internal fun ConnectionDrop.toEntity(): ConnectionDropEntity = ConnectionDropEntity(
    id = id,
    atEpochMillis = atEpochMillis,
    matchId = matchId?.value,
    submit = submit,
    cause = cause,
)
