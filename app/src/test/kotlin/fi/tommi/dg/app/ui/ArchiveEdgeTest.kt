package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

class ArchiveEdgeTest {

    /** 2026-08-07T00:00:00Z. Kiinteä luku eikä laskettu, jotta testi ei toista toteutusta. */
    private val elokuunSeitsemasUtc = 1_786_060_800_000L

    @Test
    fun `reunapaiva muotoillaan kuukausi kirjaimin`() {
        assertEquals(
            "7 Aug 2026",
            ArchiveEdge.formatDate(elokuunSeitsemasUtc, ZoneOffset.UTC),
        )
    }

    /**
     * Vyöhyke ei ole koriste. Sama hetki on kahdella laitteella eri päivä, ja käyttäjälle
     * oikea vastaus on hänen oma vuorokautensa.
     */
    @Test
    fun `sama hetki on eri paiva eri vyohykkeella`() {
        val sekuntiaEnnenKeskiyota = elokuunSeitsemasUtc - 1_000

        assertEquals(
            "6 Aug 2026",
            ArchiveEdge.formatDate(sekuntiaEnnenKeskiyota, ZoneOffset.UTC),
        )
        assertEquals(
            "7 Aug 2026",
            ArchiveEdge.formatDate(sekuntiaEnnenKeskiyota, ZoneId.of("Europe/Helsinki")),
        )
    }

    /**
     * Kanta vastasi tyhjällä, eikä se ole sama asia kuin lukematta oleva kanta.
     *
     * Tämä on se ero jonka takia [ArchiveEdgeState] on olemassa. Ennen 5.9.2026 molemmat
     * olivat `null`, ja rivi sanoi lukemattomasta kannasta "Message archive is empty".
     */
    @Test
    fun `tyhja kanta ei ole sama kuin lukematon kanta`() {
        assertEquals(ArchiveEdgeState.Empty, ArchiveEdgeState.from(null))
        assertNotEquals(ArchiveEdgeState.Unknown, ArchiveEdgeState.from(null))
    }

    /** Vastannut kanta antaa reunapäivän sellaisenaan, eikä [ArchiveEdgeState.from] tuota
     * koskaan lukematonta tilaa: se on kerääjän alkuarvo eikä kannan vastaus. */
    @Test
    fun `vastannut kanta antaa reunapaivan`() {
        assertEquals(
            ArchiveEdgeState.Reaches(elokuunSeitsemasUtc),
            ArchiveEdgeState.from(elokuunSeitsemasUtc),
        )
    }
}
