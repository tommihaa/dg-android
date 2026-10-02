package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Odottavien määrä on luettelon johdettu ominaisuus (Tommin toive 31.8.2026, *"0 odottavaa"*).
 *
 * Kolme arvoa on pidettävä erillään: vuoro, ei vuoroa ja ei tietoa. Vain ensimmäinen
 * lasketaan, ja tyhjä luettelo antaa nollan eikä puuttuvaa lukua, koska nolla on juuri se
 * tila jota varten rivi on olemassa.
 */
class TopPageTest {

    private fun match(id: String, myTurn: Boolean?) = Match(
        id = MatchId(id),
        eventName = "Event $id",
        eventId = null,
        opponent = PlayerRef(name = "opponent"),
        myTurn = myTurn,
        round = null,
        matchLength = null,
        graceText = null,
        timePoolText = null,
        playPath = null,
        reviewPath = null,
    )

    private fun page(vararg matches: Match) = TopPage(
        user = PlayerRef(name = "me"),
        messageQueuePath = null,
        matches = matches.toList(),
    )

    @Test
    fun `tyhja luettelo antaa nollan eika puuttuvaa lukua`() {
        assertEquals(0, page().yourTurnCount)
    }

    @Test
    fun `vain vuorossa olevat lasketaan ja tuntematon ei ole kumpaakaan`() {
        val page = page(match("1", true), match("2", false), match("3", null), match("4", true))
        assertEquals(2, page.yourTurnCount)
    }
}
