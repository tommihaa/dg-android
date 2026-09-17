package fi.tommi.dg.net

import fi.tommi.dg.domain.SettingsUpdate
import fi.tommi.dg.domain.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Kiertokoe: luettu tila lähetettynä takaisin tuottaa saman sivun kenttä kentältä.
 *
 * Tämä on `docs/ASETUKSET.md` todennussuunnitelman kohta 2, ja se on eri testi kuin
 * `SettingsRiskCaptureLiveTest`. Ero on tarkoituksellinen ja se koskee riskiä eikä
 * kattavuutta: riskitesti hakee lisäksi lautasivun neljästi, ja **lautasivun lukeminen lukee
 * myös keskustelun**. Live-testi ei arkistoi mitään, joten se veisi mahdollisen viestin
 * sivustolta eikä mihinkään. Kiertokoe ei tarvitse lautaa lainkaan.
 *
 * **Mitään ei muuteta.** Lähetettävä tila on täsmälleen se joka luettiin, joten
 * onnistuneessa ajossa asetukset ovat ajon jälkeen samat kuin ennen sitä myös silloin kun
 * testi keskeytyy kesken. Tämä on ainoa live-testi tässä repossa jolla ei ole
 * palautuslohkoa, ja syy on juuri se: palautettavaa ei synny.
 *
 * **Tämä kutsuu tuotantokoodia.** Kokoaminen ja vartiot ovat `SettingsPage.update`issa
 * `core-domain`issa, eli testi todentaa sen mikä menee laitteelle eikä omaa kopiotaan.
 */
@Tag("live")
class SettingsRoundTripLiveTest {

    @Test
    fun `luettu tila lahetettyna takaisin tuottaa saman sivun`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        val before = client.liveSettings()
        println("Ennen: rastit=${before.checked} radiot=${before.radios} metodi=${before.method}")

        // Kokoaminen tuotantokoodilla, samoilla vartioilla jotka ruutu saa.
        val update = before.update(before)
        val ready = assertInstanceOf(SettingsUpdate.Ready::class.java, update)
        assertEquals(before.checked, ready.checked, "Kokoaminen muutti rasteja")
        assertEquals(before.radios, ready.radios, "Kokoaminen muutti radioita")

        val response = client.send(ready.submission)
        assertInstanceOf(DgResponse.Ok::class.java, response, "Lähetys ei mennyt läpi")

        val after = client.liveSettings()
        println("Jälkeen: rastit=${after.checked} radiot=${after.radios}")

        // Kenttä kentältä. Rastit joukkona ja radiot karttana, koska juuri niiden
        // katoaminen on se vika jota tämä koe etsii.
        assertEquals(before.checked, after.checked, "Valintaruudut muuttuivat kierroksella")
        assertEquals(before.radios, after.radios, "Radiovalinnat muuttuivat kierroksella")
        assertEquals(
            before.fieldNames,
            after.fieldNames,
            "Lomakkeen kenttäjoukko muuttui kesken kokeen",
        )
    }
}
