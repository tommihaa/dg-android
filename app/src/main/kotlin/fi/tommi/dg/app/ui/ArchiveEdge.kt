package fi.tommi.dg.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Arkiston reunapäivän muotoilu.
 *
 * Oma tiedostonsa eikä composablen sisällä, koska tämä on ainoa osa reunapäivästä jossa voi
 * mennä vikaan: aikavyöhyke ja epookki. Näin se on testattavissa ilman käyttöliittymää.
 */
object ArchiveEdge {

    /**
     * Kiinteä englanti eikä laitteen kieli. Käyttöliittymä on kokonaan englantia
     * (`resourceConfigurations += listOf("en")`, päätös 5.8.2026), joten laitteen kielen
     * mukaan vaihtuva kuukausi olisi ainoa suomeksi tai saksaksi vilahtava sana koko
     * sovelluksessa.
     *
     * Kuukausi kirjaimin eikä numeroin, koska `7.8.` ja `8/7` ovat eri päiviä eri lukijalle
     * ja tämä rivi luetaan juuri silloin kun epäillään onko tieto vanhaa.
     */
    private val FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /**
     * Reunapäivä luettavana päivämääränä.
     *
     * Vyöhyke on laitteen oma, koska kirjoitushetki kuului käyttäjän omaan vuorokauteen:
     * eilen illalla tallennettu viesti on käyttäjälle eilinen silloinkin kun UTC on jo
     * vaihtanut päivää. Vyöhyke on parametri jotta testi voi kiinnittää sen.
     *
     * Kellonaikaa ei näytetä. Kysymys johon rivi vastaa on "mihin asti arkisto ulottuu", ja
     * siihen päivä riittää; minuutin tarkkuus antaisi vaikutelman että reuna on tarkempi
     * kuin se on, sillä tallennushetki on noutohetki eikä lähetyshetki.
     */
    fun formatDate(storedAtEpochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(storedAtEpochMillis).atZone(zone).toLocalDate().format(FORMAT)
}

/**
 * Reunapäivärivin kolme tilaa.
 *
 * Tila on olemassa siksi että `null` tarkoitti aiemmin kahta eri asiaa. Rivi luki
 * `observeNewestStoredAt()`in arvolla `initialValue = null`, ja null piirtyi tekstinä
 * `Message archive is empty`, joten ruutu **väitti arkiston olevan tyhjä** siihen asti että
 * Room ehti vastata. Löytö on kaavahausta 5.9.2026, ja se on saman vikamuodon neljäs
 * esiintymä: vastaamaton virta luettuna vastaukseksi (`docs/AUDITOINTI-KOMPOSITIO.md`).
 *
 * Vahinko oli pieni, koska rivi korjaa itsensä eikä kirjoita mitään mihinkään. Se on silti
 * juuri se rivi jonka koko tehtävä on olla mittalukema jonka näkee ilman arvailua, ja
 * välähtävä väärä nolla on arvailtava.
 *
 * Erottelu on tyyppinä eikä lipputietona, jotta kolmas tila ei voi jäädä piirtämättä:
 * `when` ilman haaraa ei käänny.
 */
sealed interface ArchiveEdgeState {

    /** Kantaa ei ole vielä luettu. Ei tarkoita tyhjää arkistoa eikä saa näyttää siltä. */
    data object Unknown : ArchiveEdgeState

    /** Kanta vastasi, eikä arkistossa ole yhtään viestiä. */
    data object Empty : ArchiveEdgeState

    /** Kanta vastasi, ja tuorein tallennus on tältä hetkeltä. */
    data class Reaches(val storedAtEpochMillis: Long) : ArchiveEdgeState

    companion object {
        /**
         * Kannan vastaus tilaksi.
         *
         * Tätä kutsutaan vasta kun virta on vastannut, joten `null` tarkoittaa tässä
         * yksiselitteisesti tyhjää arkistoa. `Unknown` ei synny täältä lainkaan vaan on
         * kerääjän alkuarvo, ja se on koko erottelun tarkoitus.
         */
        fun from(newestStoredAtEpochMillis: Long?): ArchiveEdgeState =
            newestStoredAtEpochMillis?.let(::Reaches) ?: Empty
    }
}
