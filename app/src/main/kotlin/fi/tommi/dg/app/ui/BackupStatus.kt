package fi.tommi.dg.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Varmuuskopion aika ja se milloin uusi kopio on tarpeen.
 *
 * Oma tiedostonsa samasta syystä kuin [ArchiveEdge] ja [ArchiveExportFilename]: nämä kaksi
 * ovat ainoat osat joissa voi mennä vikaan (aikavyöhyke ja väliaikaehto), ja tässä ne ovat
 * testattavissa ilman `Activity`ä, tiedostonvalitsinta tai kelloa.
 */
object BackupStatus {

    /**
     * Kellonaika on mukana toisin kuin [ArchiveEdge]ssä, ja ero on kysymyksessä. Reunapäivä
     * vastaa siihen mihin asti arkisto ulottuu, ja siihen päivä riittää. Tämä rivi vastaa
     * siihen **onko kopio tuore**, ja samana päivänä aamulla tehty kopio on eri asia kuin
     * hetki sitten tehty.
     *
     * Kiinteä englanti eikä laitteen kieli, samasta syystä kuin [ArchiveEdge]ssä.
     */
    private val FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)

    fun format(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(FORMAT)

    /**
     * Vuorokausi, ja se on valinta eikä tekninen raja.
     *
     * Sovellus ei aja mitään taustalla (ei `WorkManager`ia, ei herätyksiä), joten kopio
     * syntyy vain silloin kun viestiruutu avataan. Tämän sovelluksen käyttäjä avaa sen
     * päivittäin, koska ottelut ovat kierrospohjaisia, joten vuorokauden väli tarkoittaa
     * käytännössä *"kerran päivässä, ensimmäisellä avauksella"*. Tiheämpi väli kirjoittaisi
     * saman tiedoston uudestaan ilman että arkistossa on välttämättä mitään uutta.
     */
    const val INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000

    /**
     * Onko automaattisen kopion aika.
     *
     * `null` eli ei koskaan kopioitu on **kyllä**: käyttöönoton jälkeen ensimmäinen kopio
     * syntyy heti eikä vasta vuorokauden päästä. Tuleva aika (kello siirretty taaksepäin,
     * eri aikavyöhyke) luetaan myös kyllä-vastaukseksi, koska vaihtoehto olisi jäädä
     * odottamaan hetkeä joka ei tule.
     */
    fun isDue(lastSaved: Long?, now: Long): Boolean {
        if (lastSaved == null) return true
        if (lastSaved > now) return true
        return now - lastSaved >= INTERVAL_MILLIS
    }
}
