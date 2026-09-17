package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

class ArchiveExportFilenameTest {

    /** 2026-08-07T00:00:00Z. Kiinteä luku eikä laskettu, sama kuin `ArchiveEdgeTest`. */
    private val elokuunSeitsemasUtc = 1_786_060_800_000L

    @Test
    fun `nimi on numeromuotoinen paivamaara`() {
        assertEquals(
            "dg-archive-2026-08-07.json",
            ArchiveExportFilename.of(elokuunSeitsemasUtc, ZoneOffset.UTC),
        )
    }

    @Test
    fun `sama hetki on eri paiva eri vyohykkeella`() {
        val sekuntiaEnnenKeskiyota = elokuunSeitsemasUtc - 1_000

        assertEquals(
            "dg-archive-2026-08-06.json",
            ArchiveExportFilename.of(sekuntiaEnnenKeskiyota, ZoneOffset.UTC),
        )
        assertEquals(
            "dg-archive-2026-08-07.json",
            ArchiveExportFilename.of(sekuntiaEnnenKeskiyota, ZoneId.of("Europe/Helsinki")),
        )
    }
}
