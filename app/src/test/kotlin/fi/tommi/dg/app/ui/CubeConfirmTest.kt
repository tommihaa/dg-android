package fi.tommi.dg.app.ui

import fi.tommi.dg.app.R
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
    fun `ilman sivuston verify-ruutua rastia ei voi vaatia`() {
        // Asetus on pelaajan omalla tilillä, ja lauta kantaa sen: ruutu puuttuu sivulta
        // (fixture move_roll_double_no_verify.html). Dialogi jää ainoaksi toiseksi eleeksi.
        listOf("Double", "Accept").forEach { label ->
            assertEquals(label, CubeAction.ASK, cubeActionFor(label, siteAsksVerify = false, verified = false))
        }
    }

    @Test
    fun `muut napit lahtevat sellaisenaan`() {
        // Decline mukaan lukien: sivu ei vaadi sille rastia, ja se meni ilman rastia läpi
        // kuudesti 3.9.2026.
        listOf("Roll Dice", "Submit Move", "Next", "Decline", "Submit Greedy Bearoff").forEach { label ->
            assertEquals(label, CubeAction.SEND, cubeActionFor(label, siteAsksVerify = true, verified = false))
        }
    }

    /**
     * `Accept` on aina kuution hyväksyntä, myös peruutetulla sivulla.
     *
     * Korjattu 8.9.2026: 5.9.2026 kirjattu "peruutuspyyntö" oli kaappauksen mukaan
     * tarjous jonka edellä sivu kertoi peruutuksesta (*erichktn has doubled.*), ja
     * Tony 77:n sama yhdistelmä 8.9.2026 päättyi omistettuun `cube2`-kuutioon.
     */
    @Test
    fun `accept puhuu kuutiosta myos peruutetulla sivulla`() {
        assertEquals(R.string.board_accept_text, cubeDialogBody("Accept"))
    }

    @Test
    fun `tuplauksen runko on tuplausteksti`() {
        assertEquals(R.string.board_double_text, cubeDialogBody("Double"))
    }
}
