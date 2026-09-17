package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Varmuuskopiorivin tila kolmesta arvosta.
 *
 * Testi on olemassa yhdestä syystä, ja se on 1.9.2026 laiteajossa nähty vika: rivi lupasi
 * kommentissaan ajan jota se ei näyttänyt, eikä yksikään testi väittänyt ajasta mitään.
 */
class BackupUiStateTest {

    private val kello = 1_788_281_892_040L

    @Test
    fun `kayttoonottamaton on pois`() {
        assertEquals(
            BackupUiState.Off,
            BackupUiState.of(target = null, lastSaved = null, failedNow = false),
        )
    }

    @Test
    fun `kayttoonotettu ilman kirjoitusta on kaytossa ilman aikaa`() {
        assertEquals(
            BackupUiState.On(null),
            BackupUiState.of(target = "content://kohde", lastSaved = null, failedNow = false),
        )
    }

    @Test
    fun `onnistunut kirjoitus kantaa ajan`() {
        assertEquals(
            BackupUiState.On(kello),
            BackupUiState.of(target = "content://kohde", lastSaved = kello, failedNow = false),
        )
    }

    /** Tämä on se väite jota rivi ei aiemmin pitänyt: aika on mukana myös vian kohdalla. */
    @Test
    fun `vika kantaa ajan jolloin kopio viimeksi onnistui`() {
        assertEquals(
            BackupUiState.Failed(kello),
            BackupUiState.of(target = null, lastSaved = kello, failedNow = true),
        )
    }

    /**
     * Käynnistyksen jälkeen `failedNow` on aina epätosi, koska mikään ei muista vikaa
     * lippuna. Tunnistus on siinä että aika on olemassa ilman osoitetta, ja sen yhdistelmän
     * voi tuottaa vain `forget`.
     */
    @Test
    fun `aika ilman osoitetta on vika myos uudella kaynnistyksella`() {
        assertEquals(
            BackupUiState.Failed(kello),
            BackupUiState.of(target = null, lastSaved = kello, failedNow = false),
        )
    }

    /** Vika ilman yhtään onnistunutta kopiota on mahdollinen: ensimmäinen kirjoitus kaatui. */
    @Test
    fun `vika ilman aiempaa kopiota on vika ilman aikaa`() {
        assertEquals(
            BackupUiState.Failed(null),
            BackupUiState.of(target = null, lastSaved = null, failedNow = true),
        )
    }
}
