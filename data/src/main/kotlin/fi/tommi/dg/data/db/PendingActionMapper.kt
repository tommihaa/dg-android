package fi.tommi.dg.data.db

import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PendingAction

/**
 * Domainin ja kannan välinen muunnos.
 *
 * Suora kenttä kentältä, eikä siinä ole tulkittavaa: rivi kuvaa painallusta, ja painallus
 * on kokonaan sivulta luettua tekstiä. Jos tähän ilmestyy joskus normalisointia, se on
 * merkki siitä että jono on alkanut koota jotain, ks. [PendingAction].
 *
 * [PendingActionEntity.id] on autogeneroitu, joten kirjoitussuunnassa se jätetään nollaksi
 * ja Room antaa oikean. Lukusuunnassa se tulee mukaan, koska poisto tarvitsee sen.
 *
 * **Yksi kenttä ei kulje kumpaankaan suuntaan**: [PendingActionEntity.attempts] on kannan
 * sarake jota domain ei tunne, ja se jää oletusarvoonsa. Ks. sen oma teksti.
 */
fun PendingAction.toEntity(): PendingActionEntity = PendingActionEntity(
    matchId = matchId?.value,
    boardPath = boardPath,
    submit = submit,
    pendingMove = pendingMove,
    verified = verified,
    createdAtEpochMillis = createdAtEpochMillis,
    lastErrorText = lastErrorText,
)

fun PendingActionEntity.toDomain(): PendingAction = PendingAction(
    id = id,
    matchId = matchId?.let(::MatchId),
    boardPath = boardPath,
    submit = submit,
    pendingMove = pendingMove,
    verified = verified,
    createdAtEpochMillis = createdAtEpochMillis,
    lastErrorText = lastErrorText,
)
