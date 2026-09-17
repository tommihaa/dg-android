package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Palvelimen ilmoitukset ja niiden suhde tilannetekstiin.
 *
 * Sivulla on kolme viestikanavaa eri muodoissa: paljas teksti (peruutus), `h4`
 * (tilanne) ja `b` (vakiorivi). Kaksi ensimmäistä kertovat jotain, kolmas ei.
 */
class BoardParserNoticeTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val rollback = checkNotNull(BoardParser.parse(fixture("move_rollback.html")))
    private val tavallinen = checkNotNull(BoardParser.parse(fixture("move_board.html")))

    @Test
    fun `arvattu asema on lippu eika torjunta`() {
        // Sivu on lauta ja jäsentyy, mutta se ei ole nykytila. Lippu on ainoa erotin: lauta
        // itse näyttää täysin tavalliselta. Torjuntaa kokeiltiin 27.8.2026 ja siitä
        // luovuttiin, koska kaapatuista siirtovastauksista 110/312 on tällaisia.
        val arvattu = checkNotNull(BoardParser.parse(fixture("chat_thread.html")))

        assertTrue(arvattu.speculative)
        assertFalse(tavallinen.speculative)
    }

    @Test
    fun `arvauskysymysta ei piirreta kahdesti`() {
        // Kysymys on `body > b`, eli täsmälleen se rivi jonka prompt lukee kun h4 puuttuu.
        // Sama syy kuin peruutuksella alempana: lippu saa oman palkkinsa, joten lause ei
        // kuulu enää tilannetekstiin.
        val arvattu = checkNotNull(BoardParser.parse(fixture("chat_thread.html")))

        assertTrue(arvattu.prompt.orEmpty().contains("proceeds this way"), arvattu.prompt)
        assertNull(arvattu.situation)
    }

    @Test
    fun `toistojonon pituus luetaan linkistä ja nimiö ei jää ilmoitukseksi`() {
        // Rivi on `Pending Replay: <a>_1_</a>`: nimiö tekstisolmuna, luku linkissä. Pelkkä
        // nimiö näkyi laitteella 9.9.2026, koska linkki on elementti ja putosi listasta.
        val dr = checkNotNull(BoardParser.parse(fixture("move_cube_drs_alt.html")))

        assertEquals(1, dr.pendingReplays)
        assertFalse(
            dr.notices.any { it.contains("Pending Replay", ignoreCase = true) },
            "Nimiö jäi ilmoituksiin: ${dr.notices}",
        )
        assertNull(tavallinen.pendingReplays)
    }

    @Test
    fun `peruutusilmoitus päätyy malliin`() {
        assertTrue(
            rollback.notices.any { it.startsWith("Your opponent made an unexpected move") },
            "Ilmoitusta ei luettu: ${rollback.notices}",
        )
        assertTrue(rollback.rolledBack)
    }

    @Test
    fun `vahvistamaton kuutioteko näkyy sivuston punaisena rivinä`() {
        // Sivusto vastaa `?submit=Accept` ilman `verify`ä samalla laudalla ja punaisella
        // FONT-rivillä sen alla (mitattu 3.9.2026). Rivi on elementti eikä tekstisolmu, ja
        // siksi se putosi ilmoituksista: kolme aiempaa `Double`-painallusta (28.8. ja 31.8.)
        // luettiin "sivusto ei sanonut mitään", vaikka sanoi. Painallus joka katoaa tyhjään
        // on tässä projektissa vika, ja tämä on se rivi joka tekee siitä näkyvän.
        val vahvistamaton = checkNotNull(BoardParser.parse(fixture("move_accept_not_verified.html")))

        assertEquals(listOf("Previous move not verified!"), vahvistamaton.plainNotices)
        assertFalse(vahvistamaton.rolledBack)
        // Lauta on yhä tarjouslauta: sama kysymys odottaa, nyt rastin kanssa.
        assertEquals("vastapelaaja has doubled.", vahvistamaton.situation)
        val form = checkNotNull(vahvistamaton.form)
        assertEquals(listOf("Accept", "Decline"), form.submits)
        assertEquals("Accept", form.verify)
    }

    @Test
    fun `tavallisella sivulla ei ole peruutusta`() {
        assertFalse(tavallinen.rolledBack)
        // Tyhjä eikä vain peruutukseton: bodyn paljaat tekstisolmut ovat kelvollinen
        // ilmoituskanava vain jos ne ovat muuten hiljaa. Jos tähän alkaa tulla roskaa,
        // kenttä on hyödytön ja se pitää tietää heti.
        assertEquals(emptyList<String>(), tavallinen.notices)
    }

    @Test
    fun `lipuksi luettu ilmoitus ei ole enää esitettävissä ilmoituksissa`() {
        // Kahdennuksen ainoa vahti. Peruutus on mallissa kahdesti tarkoituksella, lippuna ja
        // tekstinä, ja ruudulla se saa näkyä vain kerran: laitteella 10.8.2026 näkyi molemmat,
        // jolloin sama tapahtuma luettiin kahdeksi.
        assertEquals(emptyList<String>(), rollback.plainNotices)
        assertTrue(rollback.notices.isNotEmpty(), "Raakateksti ei saa kadota mallista")
    }

    @Test
    fun `tuntematon ilmoitus säilyy esitettävissä`() {
        // Rajaus koskee vain tunnettua lausetta. Tuntemattoman sanamuotoa ei voi arvata, joten
        // se on juuri se jonka on päästävä ruudulle asti.
        val tuntematon = "Something the site has never said before."
        val muokattu = rollback.copy(notices = rollback.notices + tuntematon)

        assertEquals(listOf(tuntematon), muokattu.plainNotices)
    }

    @Test
    fun `tilanneteksti on h4 eikä vakiorivi`() {
        // Sivulla on molemmat. Vakiorivi "Please select your action/make your move:" on
        // joka sivulla samana eikä siksi kerro tilanteesta mitään, joten se häviää h4:lle.
        assertEquals("Kayttaja A declines the cube action.", rollback.prompt)
        assertEquals("Please make a checker move.", tavallinen.prompt)
    }

    @Test
    fun `vakiorivi ei paady ruudulle mutta sailyy mallissa`() {
        // Sivu jolla ei ole h4:aa jättää kehotteeksi vakiorivin. Se on sivun kalusteita eikä
        // tämän ottelun tietoa, joten `situation` on null vaikka `prompt` ei ole: malli
        // kantaa sen mitä sivu sanoi, näkymä lukee sen mikä kertoo jotain.
        val vakio = tavallinen.copy(prompt = "Please select your action/make your move:")

        assertNotNull(vakio.prompt)
        assertNull(vakio.situation)
        // Tilanneteksti ei ole tätä lajia eikä katoa mukana.
        assertEquals("Kayttaja A declines the cube action.", rollback.situation)
    }

    @Test
    fun `ilmoitus ja tilanneteksti ovat eri kenttiä`() {
        // Sama sivu kantaa molempia yhtä aikaa, eikä kumpikaan saa syödä toista.
        assertTrue(rollback.notices.isNotEmpty())
        assertTrue(rollback.notices.none { it == rollback.prompt })
    }
}
