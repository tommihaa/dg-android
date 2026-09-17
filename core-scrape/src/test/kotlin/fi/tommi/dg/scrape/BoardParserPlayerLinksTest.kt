package fi.tommi.dg.scrape

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Neljäs lautasivuun vaikuttava käyttäjäasetus, mitattuna 24.8.2026 oikeaa sivustoa vasten
 * (`PlayerNameLinksCaptureLiveTest`). Sitä ei ollut kirjattujen riskien listalla lainkaan,
 * eli listan pituus oli itsessään väärä tieto.
 *
 * `Player Name links on game page` poistaa pelaajanimen ympäriltä `<a href=/bg/user/...>`:n.
 * Se oli `parsePlayers`in ainoa tuntomerkki, joten paneelit katosivat kokonaan ilman että
 * mikään kertoi puutteesta. Fixture on mittauksen toinen otos anonymisoituna; ensimmäinen
 * otos oli sama lauta linkkien kanssa, ja koko ero sivujen välillä oli neljä riviä.
 *
 * **Kolmas otos mitattiin samana päivänä korjauksen jälkeen**: sekä linkit pois että
 * `Hide pip counts` päällä. Se oli korjauksen ainoa mittaamaton oletus, koska pip-rivin
 * kadotessa tunnistuksella on jäljellä vain pistekenttä.
 */
class BoardParserPlayerLinksTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val withLinks = fixture("move_board.html")
    private val withoutLinks = fixture("move_board_no_name_links.html")
    private val neitherLinksNorPips = fixture("move_board_no_links_no_pips.html")

    @Test
    fun `fixture on oikeasti se sivu jota tama testi vaittaa`() {
        // Ilman tätä testi voisi mennä läpi fixturella jolla linkit ovatkin tallessa, eikä
        // se todistaisi mitään. Sivun on siis sanottava itse olevansa se poikkeustapaus.
        assertTrue(Jsoup.parse(withoutLinks).select("a[href*=/bg/user/]").isEmpty())
        assertTrue(Jsoup.parse(withLinks).select("a[href*=/bg/user/]").isNotEmpty())
    }

    @Test
    fun `paneelit loytyvat myos ilman pelaajanimen linkkia`() {
        val players = BoardParser.parse(withoutLinks)?.players

        assertEquals(2, players?.size, "Paneelit katosivat, eli linkkiehto on yhä ainoa")
        assertEquals(listOf("vastapelaaja", "pelaaja"), players?.map { it.player.name })
        assertEquals(listOf(155, 164), players?.map { it.pips })
        assertEquals(listOf(7, 1), players?.map { it.score })
    }

    @Test
    fun `kayttajanumero jaa nulliksi kun linkkia ei ole`() {
        // Tämä on ainoa tieto joka oikeasti katoaa sivulta eikä vain jäsentimeltä: nimi,
        // pipit ja pisteet ovat solun tekstissä, mutta numero on vain linkin osoitteessa.
        // Testi on olemassa jottei sitä joskus paikattaisi arvaamalla.
        val players = BoardParser.parse(withoutLinks)?.players.orEmpty()

        assertTrue(players.isNotEmpty())
        players.forEach { assertNull(it.player.userId, "Numero ei ole sivulla, joten sitä ei saa olla") }
    }

    @Test
    fun `linkillinen sivu lukee kayttajanumeron edelleen`() {
        // Vanha reitti ei saa hiljetä uuden myötä. Sama väite on `BoardParserTest`issä,
        // mutta se ei kerro miksi tämä nimenomainen kenttä on herkkä.
        val players = BoardParser.parse(withLinks)?.players.orEmpty()

        assertEquals(2, players.size)
        players.forEach { assertNotNull(it.player.userId) }
    }

    @Test
    fun `tunnistus on solukohtainen ja sivun rakenne on littea`() {
        // Toinen tuntomerkki luetaan solun **omasta** tekstistä. Väljempi `:contains`
        // antaisi tänään saman tuloksen, ja se on tässä mitattu eikä oletettu: sivulla on
        // kaksi taulukkoa eikä kumpikaan ole solun sisällä, joten ulompaa solua ei ole.
        // Tiukempi muoto valittiin tulevaisuuden varalta, ja tämä testi kertoo jos rakenne
        // muuttuu sisäkkäiseksi: silloin luvut eroavat ja paneeleita voisi tulla liikaa.
        val document = Jsoup.parse(withoutLinks)

        assertEquals(2, document.select("td:matchesOwn(score:)").size)
        assertEquals(
            document.select("td:contains(score:)").size,
            document.select("td:matchesOwn(score:)").size,
            "Sivun rakenne on muuttunut sisäkkäiseksi, tarkista paneelien määrä",
        )
    }

    @Test
    fun `lauta itse on sama molemmilla asetuksilla`() {
        // Verrokki: jos pisteet tai nopat eroaisivat, sivut olisivat eri laudalta eikä
        // paneelivertailu kertoisi asetuksesta mitään. Mittauksessa molemmilla otoksilla oli
        // 11 miehitettyä pistettä ja 2 noppaa; tässä fixturet ovat eri otteluista, joten
        // verrataan sitä että lauta on ylipäätään luettu eikä lukuja keskenään.
        val board = BoardParser.parse(withoutLinks)

        assertNotNull(board)
        assertTrue(board!!.points.any { it.count > 0 }, "Lauta jäi tyhjäksi")
        assertEquals(24, board.points.size)
    }

    @Test
    fun `paneelit loytyvat vaikka seka linkit etta pip-rivi puuttuvat`() {
        // Korjauksen viimeinen mittaamaton oletus, mitattu 24.8.2026: kun pip-luvut ovat
        // piilossa, solusta katoaa `155 pips` ja tunnistuksella on jäljellä vain
        // pistekenttä. Se kantaa, eli `score:` on paikallaan myös tässä yhdistelmässä.
        val players = BoardParser.parse(neitherLinksNorPips)?.players

        assertEquals(2, players?.size)
        assertEquals(listOf("vastapelaaja", "pelaaja"), players?.map { it.player.name })
        assertEquals(listOf(7, 1), players?.map { it.score })
        // Pip-luku on oikeasti poissa sivulta, ja null on siitä oikea lukema. Tämä on sama
        // tilanne kuin `move_board_no_pips.html`issa: vahdilla ei ole mihin verrata, mutta
        // paneeli itse on tallessa, mikä on juuri se ero jonka korjaus toi.
        assertEquals(listOf(null, null), players?.map { it.pips })
    }

    @Test
    fun `pip-rivi on oikeasti poissa yhdistelmasivulta`() {
        // Ilman tätä edellinen testi menisi läpi myös fixturella jolla pipit ovat tallessa.
        assertTrue(Jsoup.parse(neitherLinksNorPips).select("a[href*=/bg/user/]").isEmpty())
        assertFalse(neitherLinksNorPips.contains("pips", ignoreCase = true))
    }
}
