package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

class BackupStatusTest {

    /** 2026-08-07T00:00:00Z. Kiinteä luku eikä laskettu, kuten [ArchiveEdgeTest]issä. */
    private val elokuunSeitsemasUtc = 1_786_060_800_000L

    @Test
    fun `aika naytetaan minuutin tarkkuudella`() {
        assertEquals(
            "7 Aug 2026 00:00",
            BackupStatus.format(elokuunSeitsemasUtc, ZoneOffset.UTC),
        )
    }

    /**
     * Sama peruste kuin reunapäivällä: käyttäjän oma vuorokausi on oikea vastaus. Tässä se
     * näkyy myös kellonajassa, joka on koko rivin syy olla erikseen.
     */
    @Test
    fun `vyohyke siirtaa seka paivaa etta kelloa`() {
        assertEquals(
            "7 Aug 2026 03:00",
            BackupStatus.format(elokuunSeitsemasUtc, ZoneId.of("Europe/Helsinki")),
        )
    }

    @Test
    fun `kopioimaton on aina vuorossa`() {
        assertTrue(BackupStatus.isDue(lastSaved = null, now = elokuunSeitsemasUtc))
    }

    @Test
    fun `vuorokautta nuorempi kopio ei kirjoita uudestaan`() {
        val tuntiSitten = elokuunSeitsemasUtc - 60 * 60 * 1000
        assertFalse(BackupStatus.isDue(tuntiSitten, elokuunSeitsemasUtc))
    }

    @Test
    fun `tasan vuorokauden vanha on vuorossa`() {
        val vuorokausiSitten = elokuunSeitsemasUtc - BackupStatus.INTERVAL_MILLIS
        assertTrue(BackupStatus.isDue(vuorokausiSitten, elokuunSeitsemasUtc))
    }

    /**
     * Tulevaisuudessa oleva aika ei ole mahdoton: kellon voi siirtää taaksepäin, ja
     * laite voi vaihtaa vyöhykettä. Vaihtoehto olisi jäädä odottamaan hetkeä joka ei tule,
     * eli lakata kopioimasta hiljaa — juuri se vika jota vastaan tämä ominaisuus on.
     */
    @Test
    fun `tulevaisuudesta oleva merkinta ei jumita kopiointia`() {
        val huomenna = elokuunSeitsemasUtc + BackupStatus.INTERVAL_MILLIS
        assertTrue(BackupStatus.isDue(huomenna, elokuunSeitsemasUtc))
    }
}
