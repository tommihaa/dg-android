package fi.tommi.dg.app.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Salaus testataan tavallisella AES-avaimella eikä Keystoresta haetulla.
 *
 * Keystorea ei voi ajaa JVM:llä, mutta algoritmi, tallennusmuoto ja virheiden käsittely
 * ovat samat riippumatta siitä mistä avain tulee. Testaamatta jää vain avaimen nouto,
 * eli juuri se osa joka on laitteesta kiinni eikä olisi testattavissa muutenkaan.
 */
class AesGcmCipherTest {

    private fun avain(): SecretKey =
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val key = avain()
    private val cipher = AesGcmCipher { key }

    @Test
    fun `salattu arvo aukeaa samaksi`() {
        val salattu = cipher.encrypt("salasana")

        assertEquals("salasana", cipher.decrypt(salattu))
    }

    @Test
    fun `aakkoset sailyvat`() {
        // UTF-8 sanotaan koodissa ääneen, joten se myös testataan. Hiljainen
        // merkistömuunnos tuottaisi salasanan joka ei enää kelpaa palvelimelle.
        val alkuperainen = "hämäränsalasana€"

        assertEquals(alkuperainen, cipher.decrypt(cipher.encrypt(alkuperainen)))
    }

    @Test
    fun `sama teksti tuottaa eri salakielen joka kerta`() {
        // IV arvotaan uudestaan joka kerta. Jos tämä pettäisi, kaksi samaa salasanaa
        // näyttäisi levyllä samalta, ja GCM:llä IV:n toisto murtaa salauksen.
        val eka = cipher.encrypt("salasana")
        val toka = cipher.encrypt("salasana")

        assertNotEquals(eka, toka)
        assertEquals("salasana", cipher.decrypt(eka))
        assertEquals("salasana", cipher.decrypt(toka))
    }

    @Test
    fun `salakieli ei sisalla selkokielista salasanaa`() {
        val salattu = cipher.encrypt("salasana")

        assertTrue(!salattu.contains("salasana"))
    }

    @Test
    fun `muokattu tavu tunnistetaan eika palauteta roskaa`() {
        // Tämä on syy GCM:n valintaan. Ilman todennustunnistetta muokattu salakieli
        // purkautuisi roskaksi, joka lähtisi verkkoon salasanana.
        val salattu = cipher.encrypt("salasana")
        val rikottu = salattu.dropLast(1) + if (salattu.last() == 'A') 'B' else 'A'

        assertNull(cipher.decrypt(rikottu))
    }

    @Test
    fun `vaaralla avaimella ei aukea`() {
        val salattu = cipher.encrypt("salasana")
        val toisellaAvaimella = AesGcmCipher { avain() }

        assertNull(toisellaAvaimella.decrypt(salattu))
    }

    @Test
    fun `roskasyote palautuu nullina eika poikkeuksena`() {
        // Näin käy jos asetustiedosto on jotain muuta kuin odotettiin. Poikkeus
        // kaataisi sovelluksen käynnistyksessä, mikä on huonompi kuin uusi kirjautuminen.
        assertNull(cipher.decrypt(""))
        assertNull(cipher.decrypt("eirakennetta"))
        assertNull(cipher.decrypt(":"))
        assertNull(cipher.decrypt(":vainsalakieli"))
        assertNull(cipher.decrypt("ei base64:ei base64"))
    }

    @Test
    fun `tyhja teksti kulkee lapi`() {
        assertEquals("", cipher.decrypt(cipher.encrypt("")))
    }
}
