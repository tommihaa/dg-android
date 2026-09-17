package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageArchiveExportTest {

    private fun msg(
        matchId: MatchId? = MatchId("12345"),
        sender: String = "vastustaja",
        body: String = "hyvä siirto",
        rawHeader: String = "You have received the following quick message from vastustaja",
        source: MessageSource = MessageSource.GAME_MESSAGE,
        receivedAt: Long = 1_754_000_000_000,
        opponent: String? = "vastustaja",
    ) = Message(
        matchId = matchId,
        opponent = opponent,
        sender = sender,
        timestampText = "Jul 29, 2026 17:50",
        body = body,
        rawHeader = rawHeader,
        source = source,
        receivedAtEpochMillis = receivedAt,
    )

    @Test
    fun `tyhjä arkisto tuottaa tyhjän listan eikä virhettä`() {
        val json = emptyList<Message>().toArchiveJson(exportedAtEpochMillis = 1_000)

        assertTrue(json.contains("\"messages\": []"))
        assertTrue(json.contains("\"format\": \"$ARCHIVE_EXPORT_FORMAT\""))
        assertTrue(json.contains("\"version\": $ARCHIVE_EXPORT_VERSION"))
        assertTrue(json.contains("\"exportedAtEpochMillis\": 1000"))
    }

    @Test
    fun `tili on tiedoston tasolla ja null kirjoitetaan nakyviin`() {
        // Versio 3 (16.9.2026): vienti on aina yhden tilin arkisto, joten tili on kerran
        // otsakkeessa eikä joka viestissä. Null ei jää pois, jotta lukija erottaa sen
        // vanhasta muodosta jossa kenttää ei ollut.
        assertTrue(listOf(msg()).toArchiveJson(1_000, account = "tommih").contains("\"account\": \"tommih\""))
        assertTrue(emptyList<Message>().toArchiveJson(1_000).contains("\"account\": null"))
    }

    @Test
    fun `jokainen kenttä päätyy vientiin`() {
        val json = listOf(msg()).toArchiveJson(exportedAtEpochMillis = 1_000)

        assertTrue(json.contains("\"matchId\": \"12345\""))
        assertTrue(json.contains("\"sender\": \"vastustaja\""))
        assertTrue(json.contains("\"timestampText\": \"Jul 29, 2026 17:50\""))
        assertTrue(json.contains("\"body\": \"hyvä siirto\""))
        assertTrue(json.contains("\"rawHeader\": \"You have received the following quick message from vastustaja\""))
        assertTrue(json.contains("\"source\": \"GAME_MESSAGE\""))
        assertTrue(json.contains("\"receivedAtEpochMillis\": 1754000000000"))
        assertTrue(json.contains("\"id\": \"${msg().id}\""))
    }

    @Test
    fun `ilmoituksella ei ole ottelua, ja se säilyy nullina eikä merkkijonona`() {
        val json = listOf(msg(matchId = null, source = MessageSource.ANNOUNCEMENT)).toArchiveJson(1_000)

        assertTrue(json.contains("\"matchId\": null"))
    }

    @Test
    fun `lainausmerkki ja rivinvaihto viestin sisällä eivät riko rakennetta`() {
        val json = listOf(msg(body = "hän sanoi \"hyvä\"\nja jatkoi")).toArchiveJson(1_000)

        assertTrue(json.contains("\"body\": \"hän sanoi \\\"hyvä\\\"\\nja jatkoi\""))
        // Rakenne säilyy: sama määrä aukeavia ja sulkeutuvia aaltosulkeita.
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
    }

    @Test
    fun `kaksi viestiä erotetaan pilkulla eikä viimeisen jälkeen`() {
        val json = listOf(msg(body = "eka"), msg(body = "toka")).toArchiveJson(1_000)

        val messagesBlock = json.substringAfter("\"messages\": [").substringBeforeLast("]")
        assertEquals(1, messagesBlock.trim().let { block ->
            // Kaksi viestiä tuottaa tasan yhden pilkun kahden `}`-rivin välissä.
            Regex("},\\s*\\{").findAll(block).count()
        })
        assertTrue(!messagesBlock.trimEnd().endsWith(","))
    }

    @Test
    fun `versio 4 kirjoittaa muistutukset fraasit ja vastausyhteyden`() {
        // Muoto laajeni kerran eikä kahdesti (16.9.2026): laitteen vaihto vie mukanaan kaiken
        // mitä käyttäjä itse kirjoitti. Muistutuksen id ei kulje, koska se on laitteen numero.
        val reminder = Reminder(
            id = 9,
            game = GameKey(MatchId("12345"), opponentScore = 3, selfScore = 1),
            text = "Think about doubling",
            createdAtEpochMillis = 2_000,
        )
        val json = listOf(msg().copy(replyTo = "abc")).toArchiveJson(
            1_000,
            reminders = listOf(reminder),
            phrases = listOf("hi", "gg"),
        )

        assertTrue(json.contains("\"version\": 4"))
        assertTrue(json.contains("\"replyTo\": \"abc\""))
        assertTrue(json.contains("\"opponentScore\": 3"))
        assertTrue(json.contains("\"selfScore\": 1"))
        assertTrue(json.contains("\"text\": \"Think about doubling\""))
        assertTrue(json.contains("\"createdAtEpochMillis\": 2000"))
        assertTrue(json.contains("\"phrases\": [\n    \"hi\",\n    \"gg\"\n  ]"))
        assertTrue(!json.contains("\"id\": 9"))
    }

    @Test
    fun `tyhjat muistutukset ja fraasit kirjoitetaan tyhjina listoina`() {
        val json = emptyList<Message>().toArchiveJson(1_000)

        assertTrue(json.contains("\"reminders\": []"))
        assertTrue(json.contains("\"phrases\": []"))
    }
}
