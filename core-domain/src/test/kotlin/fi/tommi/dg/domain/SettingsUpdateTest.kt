package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/**
 * Asetuslähetyksen kokoaminen ja sen neljä vartiota, ilman verkkoa ja ilman HTML:ää.
 *
 * Jokainen tässä testattu vika on sama vika ruudulla: lähetys on koko lomake, joten yksi
 * pudonnut kenttä on nollattu asetus eikä väärä näyttö. Siksi testit kysyvät ensin
 * **pysähtyykö** lähetys, ja vasta sitten mitä se lähettää.
 */
class SettingsUpdateTest {

    private fun page(
        toggles: List<PreferenceToggle> = listOf(
            PreferenceToggle("0", "Confirmation on offering doubles", checked = true),
            PreferenceToggle("6", "Hide pip counts", checked = false),
            PreferenceToggle("7", "Home boards on left side", checked = false),
        ),
        radios: Map<String, String> = mapOf("board" to "1", "order" to "0"),
        radioGroupNames: List<String> = listOf("board", "order"),
        method: FormMethod = FormMethod.POST,
    ) = SettingsPage(
        player = PlayerRef(name = "pelaaja"),
        method = method,
        toggles = toggles,
        radios = radios,
        radioGroupNames = radioGroupNames,
    )

    private fun ready(update: SettingsUpdate): SettingsUpdate.Ready =
        assertInstanceOf(SettingsUpdate.Ready::class.java, update)

    @Test
    fun `muuttumaton tila kootaan sellaisenaan takaisin`() {
        // Kiertokokeen halvin muoto: mitään ei muuteta, ja lähtevän lomakkeen on silti
        // oltava täysi. Juuri tämä tapaus paljastaisi pudonneen kentän.
        val page = page()

        val update = ready(page.update(page))

        assertEquals(
            mapOf("board" to "1", "order" to "0", "0" to "on"),
            update.submission.fields,
        )
    }

    @Test
    fun `rastin poisto nakyy kentan puuttumisena eika arvona`() {
        // Sivustolla ei ole tapaa sanoa "pois". Ainoa tapa on jättää kenttä lähettämättä,
        // ja siksi tämä on erillinen testi eikä ylläolevan muunnos.
        val page = page()

        val update = ready(page.update(page, checked = emptySet()))

        assertEquals(mapOf("board" to "1", "order" to "0"), update.submission.fields)
    }

    @Test
    fun `metodi tulee lomakkeelta eika ole valittu taalla`() {
        val page = page(method = FormMethod.POST)

        assertEquals(FormMethod.POST, ready(page.update(page)).submission.method)
        assertEquals(SETTINGS_FORM_ACTION, ready(page.update(page)).submission.action)
    }

    @Test
    fun `valinnaton radioryhma pysayttaa ennen verkkoa`() {
        val page = page()

        val update = page.update(page, radios = mapOf("board" to "1"))

        assertEquals(SettingsUpdate.MissingRadioGroups(setOf("order")), update)
    }

    @Test
    fun `tuntematon valintaruutu pysayttaa ennen verkkoa`() {
        val page = page()

        val update = page.update(page, checked = setOf("0", "9"))

        assertEquals(SettingsUpdate.UnknownToggles(setOf("9")), update)
    }

    @Test
    fun `tuntematon radioryhma pysayttaa ennen verkkoa`() {
        val page = page()

        val update = page.update(page, radios = page.radios + ("scheme" to "2"))

        assertEquals(SettingsUpdate.UnknownRadioGroups(setOf("scheme")), update)
    }

    @Test
    fun `muuttunut kenttajoukko pysayttaa lahetyksen`() {
        // Kolmas vartio. Ruutu piirrettiin lomakkeesta jolla on kenttä `7`, ja lähetyshetken
        // sivulla sitä ei ole. Numero on paljas, joten mikään lähetyksessä ei kertoisi että
        // rasti osuu nyt eri asetukseen.
        val drawnFrom = page()
        val latest = page(
            toggles = drawnFrom.toggles.filterNot { it.name == "7" },
        )

        val update = drawnFrom.update(latest)

        assertEquals(
            SettingsUpdate.FormChanged(
                drawnFrom = setOf("0", "6", "7", "board", "order"),
                latest = setOf("0", "6", "board", "order"),
            ),
            update,
        )
    }

    @Test
    fun `kenttajoukon vertailu ei valita jarjestyksesta`() {
        // Ryhmien järjestys on sivun järjestys eikä sopimus, joten pelkkä järjestyksen
        // vaihtuminen ei saa estää lähetystä. Vartio koskee joukkoa, ei jonoa.
        val drawnFrom = page()
        val latest = page(radioGroupNames = listOf("order", "board"))

        ready(drawnFrom.update(latest))
    }

    @Test
    fun `muuttunut kenttajoukko havaitaan ennen muita vartioita`() {
        // Muut vartiot mittaavat annettua tilaa lomaketta vasten. Jos lomake on väärä, myös
        // niiden tulos on väärä, joten järjestys on osa turvaa eikä tyylikysymys.
        val drawnFrom = page()
        val latest = page(radioGroupNames = listOf("board", "order", "uusi"))

        val update = drawnFrom.update(latest, radios = mapOf("board" to "1"))

        assertInstanceOf(SettingsUpdate.FormChanged::class.java, update)
    }
}
