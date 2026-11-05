package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pelaajavalikon kohdat näkyvät vain kun kohde on olemassa (`PlayerMenu.kt`): kohta joka
 * avaisi tyhjän ruudun olisi väite jota sovellus ei voi lunastaa.
 */
class PlayerMenuTest {

    @Test
    fun `kaikki kolme kun kaikki kohteet ovat olemassa`() {
        assertEquals(
            listOf(
                PlayerMenuEntry.Profile("/bg/user/1"),
                PlayerMenuEntry.Tournament("/bg/event/2"),
                PlayerMenuEntry.Conversation("ackammon"),
            ),
            playerMenuEntries("ackammon", "/bg/user/1", "/bg/event/2", setOf("ackammon")),
        )
    }

    @Test
    fun `viestit vain kun arkistossa on keskustelu`() {
        assertEquals(
            listOf(PlayerMenuEntry.Profile("/bg/user/1")),
            playerMenuEntries("ackammon", "/bg/user/1", null, setOf("Ackammon")),
        )
    }

    @Test
    fun `ei mitaan kun kohteita ei ole`() {
        assertEquals(emptyList<PlayerMenuEntry>(), playerMenuEntries(null, null, null, emptySet()))
        assertEquals(emptyList<PlayerMenuEntry>(), playerMenuEntries("", null, null, setOf("")))
    }
}
