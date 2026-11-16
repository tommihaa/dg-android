package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarkedPositionTest {

    @Test
    fun `katselupolku on mitattu asemaosoite`() {
        val mark = MarkedPosition(
            id = 1,
            game = GameKey(MatchId("5312677"), opponentScore = 3, selfScore = 1),
            moveNumber = 916,
            note = "",
            createdAtEpochMillis = 0,
        )
        assertEquals("/bg/game/5312677/0/916", mark.viewerPath)
    }
}
