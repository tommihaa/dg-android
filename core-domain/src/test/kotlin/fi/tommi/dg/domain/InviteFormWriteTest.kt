package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InviteFormWriteTest {

    private val form = InviteForm(
        action = "/bg/invite/new",
        method = FormMethod.POST,
        hidden = mapOf("player" to "21666"),
        variant = SelectField("variant", listOf(
            ChoiceOption("1", "backgammon", selected = true),
            ChoiceOption("2", "nack", selected = false),
        )),
        length = SelectField("length", listOf(
            ChoiceOption("1", "1", selected = false),
            ChoiceOption("5", "5", selected = true),
        )),
        timeControl = SelectField("time_control", listOf(
            ChoiceOption("0", "Never", selected = true),
            ChoiceOption("11", "Weekday (250/+0/72)", selected = false),
        )),
        comment = TextField("comment", 80),
        name = TextField("name", 40),
        privateMatch = CheckboxField("private", "private"),
    )

    @Test
    fun `kentat ovat sivun jarjestyksessa ja tekstit mukana tyhjinakin`() {
        val submission = form.write(InviteChoice(variant = "2", length = "5", timeControl = "11"))!!

        assertEquals("/bg/invite/new", submission.action)
        assertEquals(FormMethod.POST, submission.method)
        // Selain lahettaa tyhjat tekstikentat, ruksaamattoman ruudun ei.
        assertEquals(
            listOf("player", "variant", "length", "comment", "name", "time_control"),
            submission.fields.keys.toList(),
        )
        assertEquals("21666", submission.fields["player"])
        assertEquals("2", submission.fields["variant"])
        assertEquals("", submission.fields["comment"])
    }

    @Test
    fun `ruksattu ruutu lahettaa sivun oman arvon`() {
        val submission = form.write(
            InviteChoice(variant = "1", length = "1", timeControl = "0", comment = "hi", name = "Ystavyys", privateMatch = true),
        )!!
        assertEquals("private", submission.fields["private"])
        assertEquals("hi", submission.fields["comment"])
        assertEquals("Ystavyys", submission.fields["name"])
    }

    @Test
    fun `arvo jota sivu ei tarjoa ei lahde`() {
        // Portti eika virhe: palvelimen vastausta keksittyyn arvoon ei ole mitattu.
        assertNull(form.write(InviteChoice(variant = "9", length = "5", timeControl = "0")))
        assertNull(form.write(InviteChoice(variant = "1", length = "99", timeControl = "0")))
        assertNull(form.write(InviteChoice(variant = "1", length = "5", timeControl = "x")))
    }

    @Test
    fun `sivuutus lahettaa piilokentat sellaisinaan`() {
        val ignore = IgnoreForm(
            action = "/bg/ignore",
            method = FormMethod.GET,
            fields = mapOf("changeto" to "1", "user" to "21666"),
            label = "Ignore pelaaja",
        )
        val submission = ignore.write()
        assertEquals(FormMethod.GET, submission.method)
        assertEquals(mapOf("changeto" to "1", "user" to "21666"), submission.fields)
        assertEquals("1", ignore.changeTo)
    }
}
