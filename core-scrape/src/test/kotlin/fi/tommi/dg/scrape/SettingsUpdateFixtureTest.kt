package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.SETTINGS_FORM_ACTION
import fi.tommi.dg.domain.SettingsUpdate
import fi.tommi.dg.domain.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Lähetyksen kokoaminen **oikeaa sivua vasten**, ilman verkkoa.
 *
 * `SettingsUpdateTest` (`core-domain`) todentaa vartiot käsin rakennetulla sivulla, eli sen
 * mitä koodi lupaa. Tämä testi todentaa saman ketjun päästä päähän kaapatulta sivulta:
 * jäsennys, kokoaminen ja lopulliset kentät. Ero on siinä että käsin rakennettu sivu ei voi
 * paljastaa jäsentimen ja kokoajan välistä eroa, koska se on kirjoitettu molempia varten.
 *
 * Tämä on `docs/ASETUKSET.md` todennussuunnitelman kohta 1. Kohta 2 on kiertokoe verkossa,
 * eikä sitä voi ajaa täällä.
 */
class SettingsUpdateFixtureTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val page = SettingsParser.parse(fixture("settings_page.html"))!!

    @Test
    fun `metodi luetaan kaapatulta sivulta`() {
        // Sivun asetuslomake on POST, toisin kuin lautasivun toimintolomakkeet. Jos tämä
        // luettaisiin väärin, kentät menisivät paikkaan josta palvelin ei niitä lue, eikä
        // mikään kertoisi virhettä: asetus vain ei tallentuisi.
        assertEquals(FormMethod.POST, page.method)
    }

    @Test
    fun `koskematon sivu kootaan tasmalleen omaksi tilakseen`() {
        // Kiertokokeen offline-puolisko. Jos jokin kenttä putoaa tässä, se putoaa myös
        // livenä, ja silloin asetus nollautuu ilman että kukaan pyysi sitä.
        val update = assertInstanceOf(SettingsUpdate.Ready::class.java, page.update(page))

        assertEquals(SETTINGS_FORM_ACTION, update.submission.action)
        assertEquals(
            mapOf(
                "order" to "0",
                "color" to "0",
                "board" to "1",
                "0" to "on",
                "1" to "on",
                "3" to "on",
                "4" to "on",
                "5" to "on",
            ),
            update.submission.fields,
        )
    }

    @Test
    fun `rastittamaton kentta ei ole lahetyksessa lainkaan`() {
        // Kolme rastittamatonta (`2`, `6`, `7`) eivät saa esiintyä missään muodossa, eivät
        // myöskään arvolla "off". Sivustolla arvo "off" olisi rasti.
        val update = assertInstanceOf(SettingsUpdate.Ready::class.java, page.update(page))

        assertTrue(setOf("2", "6", "7").none { it in update.submission.fields })
    }

    @Test
    fun `pip-lukujen piilotus lisaa tasan yhden kentan`() {
        // Sama muutos jota `SettingsRiskCaptureLiveTest` ajaa verkossa. Täällä todennetaan
        // se osa joka ei tarvitse tiliä: lähtevä lomake on muuten sama kuin ennen.
        val hidePips = checkNotNull(page.nameFor("Hide pip counts"))
        val before = assertInstanceOf(SettingsUpdate.Ready::class.java, page.update(page))

        val after = assertInstanceOf(
            SettingsUpdate.Ready::class.java,
            page.update(page, checked = page.checked + hidePips),
        )

        assertEquals(
            before.submission.fields + (hidePips to "on"),
            after.submission.fields,
        )
    }

    @Test
    fun `lautaskeeman vaihto ei kosketa muihin kenttiin`() {
        val before = assertInstanceOf(SettingsUpdate.Ready::class.java, page.update(page))

        val after = assertInstanceOf(
            SettingsUpdate.Ready::class.java,
            page.update(page, radios = page.radios + ("board" to "2")),
        )

        assertEquals(before.submission.fields + ("board" to "2"), after.submission.fields)
    }

    @Test
    fun `toisen sivun lomaketta ei edes saa lahetyksen pohjaksi`() {
        // Ennen 1.9.2026 profiilisivu jäsentyi tyhjäksi asetussivuksi, joka näytti
        // kelvolliselta lomakkeelta ilman yhtään asetusta; vain kenttäjoukon vertailu esti
        // lähetyksen nollaamasta kaikkea. Nyt jäsennin kieltäytyy sivusta (H3), eli
        // vaarallista pohjaa ei synny lainkaan.
        assertNull(SettingsParser.parse(fixture("profile_page.html")))
    }

    @Test
    fun `eri kenttajoukko ei kelpaa lahetyksen pohjaksi`() {
        // Kenttäjoukon vertailu on yhä olemassa ja tarpeen: kaksi asetussivua voi erota
        // toisistaan, esimerkiksi jos sivusto lisää tai poistaa asetuksen. Vartio on siis
        // eri asia kuin väärän sivun tunnistus, ja tämä on sen oma väite.
        val kavennettu = page.copy(toggles = page.toggles.drop(1))

        val update = page.update(kavennettu)

        assertInstanceOf(SettingsUpdate.FormChanged::class.java, update)
    }
}
