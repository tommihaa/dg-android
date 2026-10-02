package fi.tommi.dg.app.ui

import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.BeaverConfirmStore
import fi.tommi.dg.domain.BoardForm
import fi.tommi.dg.domain.SiteBoardSettings
import fi.tommi.dg.domain.FormMethod
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kuutioteon reitti painalluksesta lähetykseen: rasti pakollinen, dialogi sen jälkeen.
 *
 * Tommin päätös 3.9.2026, ks. [cubeActionFor]. Korvaa `DoubleConfirmTest`in (31.8.2026),
 * jossa rastittamaton `Double` avasi dialogin ja ohitti ruudun.
 */
class CubeConfirmTest {

    @Test
    fun `rastittamaton kuutioteko ei lahde eika kysy`() {
        // Sivusto vastaisi samaan pyyntöön rivillä "Previous move not verified!" (mitattu
        // 3.9.2026), joten pyyntöä ei tehdä lainkaan: ruutu sanoo saman ennen lähetystä.
        listOf("Double", "Accept").forEach { label ->
            assertEquals(label, CubeAction.NEEDS_VERIFY, cubeActionFor(label, siteAsksVerify = true, verified = false))
        }
    }

    @Test
    fun `rastitettu kuutioteko kysyy dialogilla ennen lahetysta`() {
        // Rasti on sivun oma toinen ele, dialogi on sovelluksen kolmas. Tuplaus on hidas
        // tapahtuma, ja kolme elettä on valittu hinta vahingosta.
        listOf("Double", "Accept").forEach { label ->
            assertEquals(label, CubeAction.ASK, cubeActionFor(label, siteAsksVerify = true, verified = true))
        }
    }

    @Test
    fun `ilman sivuston verify-ruutua nappi lahettaa suoraan`() {
        // Asetus on pelaajan omalla tilillä, ja lauta kantaa sen: ruutu puuttuu sivulta
        // (fixture move_roll_double_no_verify.html). Tommin päätös 26.9.2026: sovellus ei
        // vahvista vastoin pelaajan valintaa (sessio-26-9-ilta2, SUBSTANSSI.md kohta 102).
        listOf("Double", "Accept").forEach { label ->
            assertEquals(label, CubeAction.SEND, cubeActionFor(label, siteAsksVerify = false, verified = false))
        }
    }

    @Test
    fun `laudan kuutio kysyy myos ilman ruutua`() {
        // Kuution napautus on sovelluksen oma oikotie, ja vahinko on siinä helpompi.
        listOf("Double", "Accept").forEach { label ->
            assertEquals(label, CubeAction.ASK, cubeActionFor(label, siteAsksVerify = false, verified = false, fromCube = true))
            // Ruudun kanssa kuutio noudattaa samaa rastia kuin nappi.
            assertEquals(label, CubeAction.NEEDS_VERIFY, cubeActionFor(label, siteAsksVerify = true, verified = false, fromCube = true))
        }
    }

    @Test
    fun `muut napit lahtevat sellaisenaan`() {
        listOf("Roll Dice", "Submit Move", "Next", "Submit Greedy Bearoff").forEach { label ->
            assertEquals(label, CubeAction.SEND, cubeActionFor(label, siteAsksVerify = true, verified = false))
        }
    }

    @Test
    fun `decline vaatii rastin vain kun sen ruutu on piirretty`() {
        // Asetus pois: ruutua ei ole, ja Decline meni ilman rastia läpi kuudesti 3.9.2026.
        assertEquals(CubeAction.SEND, cubeActionFor("Decline", siteAsksVerify = false, verified = false))
        // Asetus päällä: sivu vastasi rastittamattomaan rivillä "Previous move not verified!"
        // kolmesti 25.9.2026 (sessio-25-9-ilta).
        assertEquals(CubeAction.NEEDS_VERIFY, cubeActionFor("Decline", siteAsksVerify = true, verified = false))
        // Rastitettu lähtee ilman dialogia.
        assertEquals(CubeAction.SEND, cubeActionFor("Decline", siteAsksVerify = true, verified = true))
    }

    @Test
    fun `ruudut ovat tasmalleen sivun ruudut kaikilla nelja yhdistelmalla`() {
        // SUBSTANSSI.md kohta 102, korjaus 26.9.2026: sivu on ainoa lähde.
        val offer = BoardForm(
            action = "/bg/move/7000011/1", method = FormMethod.GET, pendingMove = null,
            submits = listOf("Accept", "Decline"), verify = "Accept",
        )
        // Hyväksyntä päällä, kieltäytyminen pois (8.9.2026).
        assertEquals(listOf("Accept"), verifyBoxesFor(offer))
        // Molemmat päällä (sessio-25-9-ilta2).
        assertEquals(listOf("Accept", "Decline"), verifyBoxesFor(offer.copy(verifyDecline = true)))
        // Hyväksyntä pois, kieltäytyminen päällä: yksi ruutu eikä kaksi (tabletti 26.9.2026).
        assertEquals(listOf("Decline"), verifyBoxesFor(offer.copy(verify = null, verifyDecline = true)))
        // Molemmat pois.
        assertEquals(emptyList<String>(), verifyBoxesFor(offer.copy(verify = null)))
        // Heittosivulla ei ole Declinea, joten sen ruutua ei piirretä.
        val roll = offer.copy(submits = listOf("Roll Dice", "Double"), verify = "Double", verifyDecline = true)
        assertEquals(listOf("Double"), verifyBoxesFor(roll))
    }

    @Test
    fun `rasti koskee vain omaa tekoaan`() {
        var box = VerifyBox(listOf("Accept", "Decline"), setOf("Accept")) { _, _ -> }
        assertEquals(true, box.isChecked("Accept"))
        assertEquals(false, box.isChecked("Decline"))
        box = VerifyBox(listOf("Accept", "Decline"), setOf("Decline")) { _, _ -> }
        assertEquals(false, box.isChecked("Accept"))
        assertEquals(true, box.isChecked("Decline"))
        // Ilman omaa ruutua Declinella ei ole rastia, vaikka sivun ruutu olisi rastitettu.
        box = VerifyBox(listOf("Accept"), setOf("Accept")) { _, _ -> }
        assertEquals(null, box.boxFor("Decline"))
        assertEquals(false, box.isChecked("Decline"))
    }

    /**
     * `Accept` on aina kuution hyväksyntä, myös peruutetulla sivulla.
     *
     * Korjattu 8.9.2026: 5.9.2026 kirjattu "peruutuspyyntö" oli kaappauksen mukaan
     * tarjous jonka edellä sivu kertoi peruutuksesta (*eemelihk has doubled.*), ja
     * Teppo 77:n sama yhdistelmä 8.9.2026 päättyi omistettuun `cube2`-kuutioon.
     */
    @Test
    fun `accept puhuu kuutiosta myos peruutetulla sivulla`() {
        assertEquals(R.string.board_accept_text, cubeDialogBody("Accept"))
    }

    @Test
    fun `tuplauksen runko on tuplausteksti`() {
        assertEquals(R.string.board_double_text, cubeDialogBody("Double"))
    }

    @Test
    fun `beaver-napit ovat kuutiotekoja ja lahtevat ilman kytkinta suoraan`() {
        // Mitattu 27.9.2026 (sessio-27-9-money-peli/0079): sivu ei pyydä beaverille rastia.
        listOf("Beaver!", "Accept Beaver").forEach { label ->
            assertEquals(label, CubeAction.SEND, cubeActionFor(label, siteAsksVerify = false, verified = false))
            assertEquals(label, CubeAction.ASK, cubeActionFor(label, siteAsksVerify = false, verified = false, deviceConfirms = true))
        }
    }

    @Test
    fun `laitteen kytkin koskee vain beaveria`() {
        // Tommin karsinta 27.9.2026: Accept ja Decline seuraavat sivun omaa asetusta.
        assertEquals(setOf("Beaver!", "Accept Beaver"), BEAVER_SUBMITS)
        assertEquals(CubeAction.SEND, cubeActionFor("Decline", siteAsksVerify = false, verified = false))
        assertEquals(CubeAction.SEND, cubeActionFor("Decline", siteAsksVerify = true, verified = true))
    }

    @Test
    fun `accept-ruudun rasti ei vahvista beaveria`() {
        val box = VerifyBox(listOf("Accept", "Decline"), setOf("Accept")) { _, _ -> }
        assertEquals(null, box.boxFor("Beaver!"))
        assertEquals(false, box.isChecked("Beaver!"))
    }

    @Test
    fun `beaverin dialogeilla on omat tekstit`() {
        assertEquals(R.string.board_beaver_text, cubeDialogBody("Beaver!"))
        assertEquals(R.string.board_accept_beaver_text, cubeDialogBody("Accept Beaver"))
        assertEquals(R.string.board_beaver_title, cubeDialogTitle("Beaver!"))
    }

    @Test
    fun `oletus kopioidaan sivun tuplausvahvistuksesta kunnes kytkimeen kosketaan`() {
        val store = object : BeaverConfirmStore {
            var saved: Boolean? = null
            override fun override() = saved
            override fun save(enabled: Boolean) { saved = enabled }
        }
        assertEquals(true, store.effective(SiteBoardSettings.UNKNOWN.copy(confirmDouble = true)))
        assertEquals(false, store.effective(SiteBoardSettings.UNKNOWN.copy(confirmDouble = false)))
        // Tuntematon sivun asetus on pois, kuten ennen kytkintä.
        assertEquals(false, store.effective(SiteBoardSettings.UNKNOWN))
        store.save(false)
        assertEquals(false, store.effective(SiteBoardSettings.UNKNOWN.copy(confirmDouble = true)))
    }
}
