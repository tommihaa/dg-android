package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val page = SettingsParser.parse(fixture("settings_page.html"))!!

    @Test
    fun `asetusten omistaja luetaan otsikosta`() {
        assertEquals("pelaaja", page.player.name)
    }

    @Test
    fun `vain asetuslomake luetaan eika sivun kahta muuta lomaketta`() {
        // Sivulla on kolme lomaketta eikä yhtään id- tai class-attribuuttia. Ilman
        // action-rajausta ensimmäinen olisi salasananvaihto.
        assertEquals(8, page.toggles.size)
        assertTrue(page.toggles.all { it.name.toIntOrNull() != null })
        assertFalse(page.toggles.any { it.label.contains("Password", ignoreCase = true) })
    }

    @Test
    fun `selite luetaan solusta myos kun siina on elementti`() {
        // `Skip <em>all</em> automatic pages`. Tekstisolmuihin rajattu luku katkaisisi
        // selitteen sanaan Skip, ja silloin kaksi eri asetusta alkaisi samalla sanalla.
        val labels = page.toggles.map { it.label }
        assertTrue(labels.contains("Skip all automatic pages"))
        assertTrue(labels.contains("Skip Opponent's \"Roll Dice\" pages"))
    }

    @Test
    fun `rastit luetaan sellaisina kuin sivu ne kertoo`() {
        // Tämä joukko lähetetään takaisin sellaisenaan, joten jokainen virhe tässä on
        // asetuksen menetys eikä väärä näyttö.
        assertEquals(setOf("0", "1", "3", "4", "5"), page.checked)
        assertEquals(setOf("2", "6", "7"), page.toggles.filterNot { it.checked }.map { it.name }.toSet())
    }

    @Test
    fun `kotialue vasemmalla loytyy selitteesta ja numero tasmaa`() {
        // Kumpikaan yksin ei ole vakaa avain: numero muuttuisi renumeroinnissa ja teksti
        // sivuston sanamuodon muuttuessa. Molemmat yhdessä tekevät kummasta tahansa
        // muutoksesta punaisen testin sen sijaan että osuttaisiin hiljaa väärään ruutuun.
        assertEquals("7", page.nameFor("Home boards on left side"))
    }

    @Test
    fun `tuntematon selite ei arvaa numeroa`() {
        assertNull(page.nameFor("Ei ole olemassa"))
    }

    @Test
    fun `radioryhmat luetaan valintoineen`() {
        assertEquals(mapOf("order" to "0", "color" to "0", "board" to "1"), page.radios)
    }

    @Test
    fun `valinnaton radioryhma erottuu ryhmasta jota ei ole`() {
        // Ilman erottelua valinnaton ryhmä katoaisi lähetyksestä hiljaa, mikä on sama vika
        // kuin rastittamaton valintaruutu.
        val html = """
            <HTML><HEAD><TITLE>DailyGammon -- Settings for joku</TITLE></HEAD><BODY>
            <FORM METHOD=POST ACTION=/bg/profile/pref>
            <TABLE><TR><TD><INPUT TYPE=checkbox NAME=0> Jotain
            </TABLE>
            <TABLE><TR><TD><INPUT TYPE=radio NAME=order VALUE=0><TD>Eka
            <TR><TD><INPUT TYPE=radio NAME=order VALUE=1><TD>Toka
            </TABLE></FORM></BODY></HTML>
        """.trimIndent()

        val parsed = SettingsParser.parse(html)!!

        assertTrue(parsed.radios.isEmpty())
        assertEquals(listOf("order"), parsed.radioGroupNames)
    }

    @Test
    fun `jokaisella ryhmalla on valinta oikealla sivulla`() {
        // Sama vertailu jonka lähettäjän on tehtävä ennen POSTia.
        assertEquals(page.radioGroupNames.toSet(), page.radios.keys)
    }

    // --- Selitteet: näyttöä varten, ei lähetystä ---

    @Test
    fun `radiovaihtoehdon selite luetaan viereisesta solusta`() {
        // Valintaruudun selite on samassa solussa, radion viereisessä. Sama sääntö
        // molemmille lukisi radion selitteeksi tyhjän, ja tyhjä selite näyttäisi ruudulla
        // puuttuvalta asetukselta.
        val board = checkNotNull(page.choices.firstOrNull { it.name == "board" })

        assertEquals(listOf("Classic", "Blue/White", "Mini"), board.options.map { it.label })
        assertEquals("Blue/White", board.selectedLabel)
    }

    @Test
    fun `ryhman otsikko luetaan sita edeltavasta H4-otsikosta`() {
        // Otsikko on taulukon ulkopuolella, joten sitä ei löydä ryhmästä eikä sen
        // vanhemmista. Ainoa suhde jonka sivu antaa on dokumenttijärjestys.
        assertEquals(
            listOf("\"Next\" Match Ordering", "Background Color", "Board Scheme"),
            page.choices.map { it.label },
        )
    }

    @Test
    fun `selitteet eivat muuta lahetyssopimusta`() {
        // Näyttöä varten luettu selite ei saa vuotaa siihen mitä lähetetään. Ryhmien nimet
        // ja valitut arvot ovat sama joukko kuin ennen selitteitä.
        assertEquals(page.radioGroupNames, page.choices.map { it.name })
        assertEquals(
            page.radios,
            page.choices.mapNotNull { group ->
                group.options.firstOrNull { it.selected }?.let { group.name to it.value }
            }.toMap(),
        )
    }

    @Test
    fun `selitteeton vaihtoehto nayttaa arvon eika tyhjaa`() {
        // Varasuunta on sivun oma tieto. Tyhjä rivi olisi väite ettei vaihtoehdolla ole
        // nimeä, ja se on eri asia kuin nimi jota emme osanneet lukea.
        val html = """
            <HTML><HEAD><TITLE>DailyGammon -- Settings for joku</TITLE></HEAD><BODY>
            <FORM METHOD=POST ACTION=/bg/profile/pref>
            <TABLE><TR><TD><INPUT TYPE=radio NAME=board VALUE=2 CHECKED></TR>
            </TABLE></FORM></BODY></HTML>
        """.trimIndent()

        val parsed = SettingsParser.parse(html)!!

        assertEquals(listOf("2"), parsed.choices.single().options.map { it.label })
        assertNull(parsed.choices.single().label)
    }

    @Test
    fun `asetussivu tunnistetaan ja se erottuu profiilisivusta`() {
        val settings = fixture("settings_page.html")
        assertTrue(DgPages.isSettingsPage(settings))
        assertFalse(DgPages.isProfilePage(settings))

        val profile = fixture("profile_page.html")
        assertFalse(DgPages.isSettingsPage(profile))
        assertTrue(DgPages.isProfilePage(profile))
    }

    @Test
    fun `sivu ilman asetuslomaketta ei tuota keksittya tilaa`() {
        // Tyhjä `checked` lähetettynä nollaisi kaikki asetukset, joten väärästä sivusta
        // luettu tyhjä tulos on vaarallisin mahdollinen paluuarvo. Ennen 1.9.2026 sen esti
        // kutsupaikan muistama tunnistus, nyt jäsennin itse (H3).
        assertFalse(DgPages.isSettingsPage(fixture("top_page.html")))
        assertNull(SettingsParser.parse(fixture("top_page.html")))
    }
}
