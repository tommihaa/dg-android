package fi.tommi.dg.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nimi linkkinä profiiliin (Tommin tilaus 2.11.2026, `PlayerLink.kt`). */
class PlayerLinkTest {

    @Test
    fun `nimi saa linkin ja muu teksti ei`() {
        val avattu = mutableListOf<String>()
        val teksti = nameSpan("vs frans", "frans", Color.Red, "/bg/user/42") { avattu += it }

        assertEquals("vs frans", teksti.text)
        val linkit = teksti.getLinkAnnotations(0, teksti.length)
        assertEquals(1, linkit.size)
        assertEquals(3, linkit.single().start)
        assertEquals(8, linkit.single().end)
        (linkit.single().item as LinkAnnotation.Clickable).linkInteractionListener!!
            .onClick(linkit.single().item)
        assertEquals(listOf("/bg/user/42"), avattu)
    }

    @Test
    fun `ilman polkua nimi on pelkka vari`() {
        val teksti = nameSpan("vs frans", "frans", Color.Red, null, null)

        assertEquals("vs frans", teksti.text)
        assertTrue(teksti.getLinkAnnotations(0, teksti.length).isEmpty())
        assertEquals(1, teksti.spanStyles.size)
    }

    @Test
    fun `nimeton teksti palaa sellaisenaan`() {
        val teksti = nameSpan("Chat", "frans", null, "/bg/user/42") { }

        assertEquals("Chat", teksti.text)
        assertTrue(teksti.getLinkAnnotations(0, teksti.length).isEmpty())
    }
}
