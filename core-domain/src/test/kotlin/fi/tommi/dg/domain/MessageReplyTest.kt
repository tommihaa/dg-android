package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * `ReplyForm.write` on portin toinen puoli, ja jokainen väite koskee sitä mitä EI voi
 * lähettää. Sama muoto kuin `BoardFormPressTest`illä: tyypin arvo on tae, ei mukavuus.
 */
class MessageReplyTest {

    private val lomake = ReplyForm(
        action = "/bg/sendmsg/91004",
        method = FormMethod.POST,
        field = "text",
        maxLength = 80,
    )

    @Test
    fun `vastaus kootaan sivun omista arvoista`() {
        val submission = requireNotNull(lomake.write("Great match!"))

        assertEquals("/bg/sendmsg/91004", submission.action)
        // Metodi kulkee lomakkeelta lähetykseen asti. Lautasivun lomake on GET ja tämä
        // POST, eikä kumpaakaan valita kutsupaikassa.
        assertEquals(FormMethod.POST, submission.method)
        assertEquals(mapOf("text" to "Great match!"), submission.fields)
    }

    @Test
    fun `tyhjaa ei laheteta`() {
        // Tyhjä lähetys kuluttaisi teon eikä sanoisi mitään. Pelkkä välilyönti on sama asia.
        assertNull(lomake.write(""))
        assertNull(lomake.write("   "))
    }

    @Test
    fun `yli sivun rajan menevä lahtee, koska palvelin ei leikkaa vaan rivittaa`() {
        // Aiemmin tämä palautti nullin, ja perustelu oli että viesti menisi perille
        // puolikkaana. Mitattu 24.8.2026 oikeaa sivustoa vasten: 1329 merkin viesti
        // tallentui kokonaisena, ja sivusto rivitti sen 80 merkin kohdalta. Rivitys on
        // luettavaa tekstiä eikä katoavaa sisältöä, joten estettävää ei ole.
        assertNotNull(lomake.write("x".repeat(81)))
        assertNotNull(lomake.write("x".repeat(1329)))
    }

    @Test
    fun `tuntematon raja ja tunnettu raja kayttaytyvat nyt samoin`() {
        // maxLength luetaan yhä sivulta, koska se on sivun oma tosiasia, mutta se ei enää
        // eroa nullista lähettämisen kannalta. Ero säilyy tiedossa eikä käytöksessä.
        val rajaton = lomake.copy(maxLength = null)
        assertNotNull(rajaton.write("x".repeat(500)))
        assertNull(rajaton.write(" "), "Tyhjä on tyhjä rajasta riippumatta")
    }
}
