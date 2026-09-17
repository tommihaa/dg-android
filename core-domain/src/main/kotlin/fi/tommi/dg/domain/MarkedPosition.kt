package fi.tommi.dg.domain

/**
 * Merkitty asema, eli pelaajan oma kirjanmerkki ottelun jälkeistä analyysia varten.
 *
 * **Tämä ei ole muistutus vaikka nappi asuu niiden seassa** (Tommin toive 15.9.2026 illalla:
 * *"ottelun jälkeen näkisi mark-positiot, joita analyysin tullessa muistaisi uudelleen
 * pohtia"*). Muistutus elää yhden pelin ja lakkaa näkymästä pisteiden muuttuessa
 * ([Reminder]). Merkki luetaan vasta ottelun jälkeen, useita pelejä myöhemmin, joten sen
 * elinkaari on ottelun eikä pelin, ja se säilyy kunnes pelaaja itse poistaa sen (Tommin
 * valinta samana iltana). Analyysi voi tulla viikkoja myöhemmin (`SUBSTANSSI.md` kohta 20),
 * eikä sivuston tahti saa viedä merkkiä ennen sitä.
 *
 * **Sisältö on se mitä analyysiohjelman läpikäynti tarvitsee kohdan löytämiseen**: peli
 * pisteparina ja otsikon siirtonumero (`Move 377`). Polun tilatunniste ei kelpaa, koska se
 * ei näy analyysiohjelmassa eikä `.mat`-tiedostossa. Vapaaehtoinen sana kertoo miksi, ja se
 * saa olla tyhjä: merkin arvo on kohdassa eikä selityksessä.
 *
 * **Miksi merkki ei mene `.mat`-tiedostoon.** Mitattu 15.9.2026 tällä koneella: XG2 avaa
 * `;`-alkuisilla kommenttiriveillä täydennetyn tiedoston moitteetta, mutta ei näytä rivejä
 * missään, ja GNU Backgammon lukee `;`-rivit vain otsikosta. Kommentti tiedostossa olisi
 * siis näkymätön juuri siinä ohjelmassa jossa se pitäisi lukea. Merkit luetaan siksi
 * sovelluksesta, otteluluettelon linkistä.
 *
 * Kaanoni: `SUBSTANSSI.md` kohta 22 (analyysi on ottelun jälkeen ja läpikäynti siirto
 * siirrolta) ja kohta 27 (analyysin tuote on pelaajan muisti). Merkki on viesti itselle
 * siihen läpikäyntiin eikä neuvo kesken ottelun, joten kohta 9 ei laukea.
 */
data class MarkedPosition(
    /** Rivin tunniste kannassa. Nolla tarkoittaa riviä jota ei ole vielä tallennettu. */
    val id: Long,
    val game: GameKey,
    /** Otsikon juokseva siirtonumero, sama jonka sivu näyttää (`Move 377`). */
    val moveNumber: Int,
    /** Miksi asema merkittiin, tai tyhjä. */
    val note: String,
    val createdAtEpochMillis: Long,
    /** Asema merkintähetkellä, tai null kun sitä ei tallennettu. */
    val position: CheckerPosition? = null,
)
