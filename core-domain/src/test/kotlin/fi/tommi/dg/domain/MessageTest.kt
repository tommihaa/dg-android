package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class MessageTest {

    private fun msg(
        sender: String = "vastustaja",
        body: String = "hyvä siirto",
        rawHeader: String = "You have received the following quick message from vastustaja",
        receivedAt: Long = 1_754_000_000_000,
        opponent: String? = "vastustaja",
    ) = Message(
        matchId = MatchId("12345"),
        opponent = opponent,
        sender = sender,
        timestampText = "Jul 29, 2026 17:50",
        body = body,
        rawHeader = rawHeader,
        source = MessageSource.GAME_MESSAGE,
        receivedAtEpochMillis = receivedAt,
    )

    @Test
    fun `sama olio tuottaa saman tunnisteen`() {
        // Tämä on se idempotenssi jota oikeasti tarvitaan: epäonnistuneen kirjoituksen
        // uusiminen samalla oliolla ei saa tuottaa duplikaattia.
        assertEquals(msg().id, msg().id)
    }

    @Test
    fun `eri sisältö tuottaa eri tunnisteen`() {
        assertNotEquals(msg().id, msg(body = "huono siirto").id)
    }

    @Test
    fun `sama sisältö eri saapumishetkellä on eri viesti`() {
        // Sopimusmuutos 1.8.2026. Pikaviestisivulla ei ole aikaleimaa, joten kaksi
        // erikseen lähetettyä "Great match!" olisivat ilman tätä erottamattomat ja
        // jälkimmäinen katoaisi hiljaa. Kohteliaisuus toistuu, joten tapaus on tavallinen
        // eikä reunatapaus.
        assertNotEquals(
            msg(body = "Great match!", receivedAt = 1_754_000_000_000).id,
            msg(body = "Great match!", receivedAt = 1_754_000_060_000).id,
        )
    }

    @Test
    fun `sama viesti eri otsikolla on eri viesti`() {
        // Otsikko on osa tunnistetta, koska se on osa sisältöä: sivu voi kertoa saman
        // tekstin eri otsikon alla, ja silloin kyse on kahdesta eri tapahtumasta.
        // Tämä on samalla se testi joka kaatuu jos otsikko unohdetaan tiivisteestä.
        assertNotEquals(
            msg(rawHeader = "You have received the following telegram message:").id,
            msg(rawHeader = "You have received something entirely new").id,
        )
    }

    @Test
    fun `kenttäraja ei ole siirrettävissä`() {
        assertNotEquals(
            msg(sender = "ab", body = "c").id,
            msg(sender = "a", body = "bc").id,
        )
    }
}
