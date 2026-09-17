package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageArchiveImportTest {

    private fun msg(
        body: String = "hyvä siirto",
        receivedAt: Long = 1_754_000_000_000,
        replyTo: String? = null,
    ) = Message(
        matchId = MatchId("12345"),
        opponent = "vastustaja",
        sender = "vastustaja",
        timestampText = "Jul 29, 2026 17:50",
        body = body,
        rawHeader = "You have received the following quick message from vastustaja",
        source = MessageSource.GAME_MESSAGE,
        receivedAtEpochMillis = receivedAt,
        replyTo = replyTo,
    )

    private val muistutus = Reminder(
        id = 7,
        game = GameKey(MatchId("12345"), opponentScore = 3, selfScore = 1),
        text = "Think about doubling",
        createdAtEpochMillis = 2_000,
    )

    private fun luettu(json: String): ArchiveFile =
        (readArchiveJson(json) as ArchiveImport.Read).file

    @Test
    fun `vienti ja tuonti ovat toistensa kaanteet`() {
        // Viestin tunniste lasketaan kentistä, joten sama viesti saa saman id:n tuonnin
        // jälkeen ja kanta tunnistaa sen omakseen. Muistutuksen id ei kulje tiedostossa.
        val alkuperainen = ArchiveFile(
            exportedAtEpochMillis = 1_000,
            account = "tommih",
            messages = listOf(msg(), msg(body = "hän sanoi \"hyvä\"\nja jatkoi", replyTo = msg().id)),
            reminders = listOf(muistutus),
            phrases = listOf("hi", "gg", "ty gg u2"),
        )

        val tulos = readArchiveJson(alkuperainen.toJson())

        val luettu = assertInstanceOf(ArchiveImport.Read::class.java, tulos)
        assertEquals(0, luettu.unreadable)
        assertEquals(alkuperainen.messages, luettu.file.messages)
        assertEquals(alkuperainen.messages.map { it.id }, luettu.file.messages.map { it.id })
        assertEquals(listOf(muistutus.copy(id = 0)), luettu.file.reminders)
        assertEquals(alkuperainen.phrases, luettu.file.phrases)
        assertEquals("tommih", luettu.file.account)
        assertEquals(1_000, luettu.file.exportedAtEpochMillis)
        assertEquals(ARCHIVE_EXPORT_VERSION, luettu.file.version)
    }

    @Test
    fun `versio 1 luetaan ilman kumppania tilia ja muistutuksia`() {
        // Versio 1 (14.8.2026) ennen `opponent`-, `account`-, `replyTo`-, `reminders`- ja
        // `phrases`-kenttiä. Puuttuva kenttä on vanha tiedosto eikä virhe.
        val json = """
            {
              "format": "fi.tommi.dg.archive",
              "version": 1,
              "exportedAtEpochMillis": 1000,
              "messages": [
                {
                  "id": "x",
                  "matchId": "12345",
                  "sender": "vastustaja",
                  "timestampText": "Jul 29, 2026 17:50",
                  "body": "hyvä siirto",
                  "rawHeader": "You have received the following quick message from vastustaja",
                  "source": "GAME_MESSAGE",
                  "receivedAtEpochMillis": 1754000000000
                }
              ]
            }
        """.trimIndent()

        val file = luettu(json)

        assertEquals(1, file.version)
        assertNull(file.account)
        assertEquals(1, file.messages.size)
        assertNull(file.messages.single().opponent)
        assertEquals("hyvä siirto", file.messages.single().body)
        assertTrue(file.reminders.isEmpty())
        assertTrue(file.phrases.isEmpty())
    }

    @Test
    fun `tuntematon laji luetaan tuntemattomaksi eika kaada tuontia`() {
        val json = listOf(msg()).toArchiveJson(1_000).replace("GAME_MESSAGE", "FUTURE_KIND")

        assertEquals(MessageSource.UNKNOWN, luettu(json).messages.single().source)
    }

    @Test
    fun `viesti jolta puuttuu pakollinen kentta ohitetaan ja lasketaan`() {
        val json = """
            {
              "format": "fi.tommi.dg.archive",
              "version": 4,
              "exportedAtEpochMillis": 1000,
              "account": null,
              "messages": [
                { "sender": "a", "body": "ehjä", "source": "QUICK_MESSAGE", "receivedAtEpochMillis": 5 },
                { "sender": "b", "source": "QUICK_MESSAGE", "receivedAtEpochMillis": 6 }
              ],
              "reminders": [
                { "matchId": "1", "opponentScore": 0, "selfScore": 0, "text": "ok", "createdAtEpochMillis": 1 },
                { "matchId": "1", "text": "pisteet puuttuvat", "createdAtEpochMillis": 1 }
              ],
              "phrases": ["hi", 42]
            }
        """.trimIndent()

        val tulos = readArchiveJson(json) as ArchiveImport.Read

        assertEquals(3, tulos.unreadable)
        assertEquals(listOf("ehjä"), tulos.file.messages.map { it.body })
        assertEquals(listOf("ok"), tulos.file.reminders.map { it.text })
        assertEquals(listOf("hi"), tulos.file.phrases)
    }

    @Test
    fun `muu json ja muu teksti eivat ole arkisto`() {
        assertEquals(ArchiveImport.NotAnArchive, readArchiveJson("""{"format": "jokin.muu", "messages": []}"""))
        assertEquals(ArchiveImport.NotAnArchive, readArchiveJson("ei json"))
        assertEquals(ArchiveImport.NotAnArchive, readArchiveJson(""))
        assertEquals(ArchiveImport.NotAnArchive, readArchiveJson("""{"format": "fi.tommi.dg.archive" """))
        assertEquals(ArchiveImport.NotAnArchive, readArchiveJson("[1, 2]"))
    }

    @Test
    fun `unicode pako ja ohjausmerkit palautuvat`() {
        val json = listOf(msg(body = "tab\tja  ja ääkköset")).toArchiveJson(1_000)

        assertEquals("tab\tja  ja ääkköset", luettu(json).messages.single().body)
    }
}
