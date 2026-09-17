package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChatFormWriteTest {

    private val lomake = ChatForm(
        action = "/bg/move/7000003/1121",
        method = FormMethod.POST,
        field = "chat",
        quoteField = "quote",
        quoteValue = "on",
        quoteCheckedByDefault = true,
        hiddenFields = mapOf("commit" to "1"),
        submits = listOf("Next Game", "To Top"),
    )

    @Test
    fun `rivinvaihdot lahtevat CRLF-muodossa kuten selaimesta`() {
        // Mitattu 28.8.2026: paljailla LF:illä lähetetystä viestistä katosi vastaanottajalta
        // yksi merkki jokaisen LF-parin jäljestä ("\n\nGemini" saapui muodossa "\n\nemini").
        // Selain normalisoi lomakekentän rivinvaihdot CRLF:ksi, ja palvelin olettaa sen.
        val submission = requireNotNull(lomake.write("eka\nkappale\n\ntoka", quote = true, submit = "To Top"))
        assertEquals("eka\r\nkappale\r\n\r\ntoka", submission.fields["chat"])
    }

    @Test
    fun `valmiiksi CRLF-muotoinen teksti ei tuplaannu`() {
        val submission = requireNotNull(lomake.write("eka\r\ntoka", quote = false, submit = "To Top"))
        assertEquals("eka\r\ntoka", submission.fields["chat"])
    }

    @Test
    fun `nappia jota sivu ei tarjoa ei voi painaa`() {
        assertNull(lomake.write("hei", quote = true, submit = "Send"))
    }
}
