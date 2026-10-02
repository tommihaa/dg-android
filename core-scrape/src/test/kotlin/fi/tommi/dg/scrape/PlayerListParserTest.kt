package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerListParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val list = PlayerListParser.parse(fixture("player_list.html"))!!

    @Test
    fun `otsikkorivi ei ole pelaaja`() {
        assertEquals(4, list.players.size)
    }

    @Test
    fun `rivi jasentyy valistyssolujen yli`() {
        // Rivillä on seitsemän solua joista kolme on tyhjiä välistyksiä, joten tämä on
        // tämän sivun oma ansa: rivin alusta luettuna sijoitus olisi tyhjä solu.
        val first = list.players.first()
        assertEquals(1, first.rank)
        assertEquals("ekapelaaja", first.player.name)
        assertEquals("21666", first.player.userId)
        assertEquals("/bg/user/21666", first.player.profilePath)
        assertEquals("2326.39", first.ratingText)
        assertEquals("10765", first.experienceText)
    }

    @Test
    fun `moniosainen nimi sailyy kokonaisena`() {
        assertEquals("toka pelaaja", list.players[1].player.name)
    }

    @Test
    fun `rating ja kokemus ovat tekstina eivatka lukuina`() {
        // Sivun oma muoto säilyy: "2292.2" ei ole sama merkkijono kuin "2292.20", eikä
        // jäsennin ota kantaa kumpi on oikea tapa näyttää se.
        assertEquals("2292.2", list.players[3].ratingText)
    }

    @Test
    fun `sivun omat linkit luetaan jarjestyksessa ja omalla sanallaan`() {
        assertEquals(
            listOf("Sort By Name", "Sort By Rating", "Sort By Experience", "Next 100"),
            list.links.map { it.label },
        )
        assertEquals("/bg/plist?type=rate&start=101&length=100", list.links.last().path)
    }

    @Test
    fun `hakutulos on pelaajalista ilman sivutusta`() {
        // Mitattu 3.9.2026 oikeasta hausta (`POST /bg/plist`, `like=…`): sama otsikko ja
        // taulukko kuin listasivulla, rivejä vain osumat, alalaidassa vain Sort By -linkit.
        val result = PlayerListParser.parse(fixture("player_search.html"))!!
        val row = result.players.single()
        assertEquals("hakutulos", row.player.name)
        assertEquals("/bg/user/40001", row.player.profilePath)
        assertEquals(1, row.rank)
        assertEquals("1500", row.ratingText)
        assertEquals("0", row.experienceText)
        assertEquals(
            listOf("Sort By Name", "Sort By Rating", "Sort By Experience"),
            result.links.map { it.label },
        )
    }

    @Test
    fun `loungea ei jasenneta pelaajalistaksi`() {
        // Mitattu eikä oletettu: loungen kutsurivit menivät tämän jäsentimen läpi ja
        // tuottivat rivejä joiden "rating" oli aikaraja. Ennen 1.9.2026 sen esti vain
        // kutsupaikan muistama predikaatti; nyt jäsennin ajaa sen itse (H3).
        val lounge = fixture("lounge_page.html")
        assertFalse(DgPages.isPlayerList(lounge))
        assertNull(PlayerListParser.parse(lounge))
    }
}
