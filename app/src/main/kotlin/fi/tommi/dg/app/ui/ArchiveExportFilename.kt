package fi.tommi.dg.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Ehdotettu tiedostonimi arkiston viennille.
 *
 * Oma tiedostonsa samasta syystä kuin [ArchiveEdge]: ainoa osa joka voi mennä vikaan on
 * aikavyöhyke, ja se on tässä testattavissa ilman `Activity`ä tai käyttöjärjestelmän omaa
 * tiedostonvalitsinta.
 */
object ArchiveExportFilename {

    /** Kiinteä numeromuoto eikä laitteen kieli, samasta syystä kuin [ArchiveEdge.formatDate]. */
    private val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)

    fun of(nowEpochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val date = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate().format(FORMAT)
        return "dg-archive-$date.json"
    }
}
