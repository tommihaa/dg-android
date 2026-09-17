package fi.tommi.dg.scrape

import fi.tommi.dg.domain.BoardScheme
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kaksi asetusta jotka vaikuttavat lautasivuun, mitattuna 3.8.2026 oikeaa sivustoa vasten
 * (`SettingsRiskCaptureLiveTest`). Molemmat olivat sitä ennen kirjattuja hypoteeseja, ja
 * toinen niistä osoittautui vääräksi.
 *
 * Fixturet ovat samasta ottelusta ja samasta osoitteesta kuin `move_board_arrows.html`,
 * peräkkäin haettuina, joten erot johtuvat asetuksesta eivätkä eri laudasta.
 */
class BoardParserSchemeTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val normal = fixture("move_board_arrows.html")
    private val mini = fixture("move_board_mini.html")
    private val noPips = fixture("move_board_no_pips.html")

    @Test
    fun `lautaskeema vaihtaa hakemiston eika tiedostonimia`() {
        // Kirjattu hypoteesi sanoi että skeema voi vaihtaa kuvatiedostojen nimet, ja että
        // se kaataisi `img[src*=pt_]`-valinnan ja noppien värin luvun. **Se oli väärin.**
        // Skeema on polun hakemistonumero (1 Classic, 2 Blue/White, 3 Mini), ja tiedostonimet
        // ovat kaikissa samat. Molemmat nimeen nojaavat kohdat ovat siis turvassa.
        // Vertailu koskee juuri niitä kahta kuvalajia joita jäsennin tulkitsee nimestä.
        // Koko kuvajoukko ei ole sama, koska Mini pudottaa nuolet ja lisää välikkeen, ks.
        // omat testinsä alempana.
        assertEquals(fileNames(normal), fileNames(mini))
        assertEquals(setOf("/images/2"), directories(normal))
        assertEquals(setOf("/images/3"), directories(mini))
    }

    @Test
    fun `Mini pudottaa pistenumerot ja lauta jaa siksi tyhjaksi`() {
        // Tämä on skeeman oikea vaara, ja se on eri kuin arvattu. Nappulat ovat sivulla
        // tallessa: 24 pistekuvaa ALT-koodeineen. Numerorivi sen sijaan puuttuu kokonaan,
        // ja koska jäsennin lukee numeron sivulta eikä päättele sitä suunnasta (sääntö
        // 1.8.2026, ks. docs/KOHDE.md), pisteitä ei voi numeroida yhtäkään.
        assertEquals(24, Jsoup.parse(mini).select("img[src*=pt_]").size)
        assertTrue(Jsoup.parse(mini).select("img[src*=pt_][alt~=^[yb]\\d+$]").isNotEmpty())

        val board = BoardParser.parse(mini)

        // **Jäsennys ei kaadu eikä palauta nullia vaan tyhjän laudan**, joka on yksin
        // erottamaton laudasta jolla ei ole yhtään nappulaa. Tämä oli 13.8.2026 asti
        // kirjattu hiljaiseksi viaksi ja avoimeksi sopimuskysymykseksi. Sopimus jäi
        // ennalleen, koska tyhjän listan ei tarvitse todistaa mitään: syy on luettavissa
        // sivulta, ks. `scheme` ja sen omat testit alempana.
        assertNotNull(board)
        assertTrue(board!!.points.isEmpty(), "Mini-skeemasta ei saa pistenumeroita")
    }

    @Test
    fun `Mini sailyttaa nopat paneelit ja tilatunnisteen`() {
        // Sivu ei ole hyödytön: kaikki muu paitsi pisteet on yhä luettavissa. Se on syy
        // siihen ettei tyhjää lautaa voi tulkita "tämä ei ole lautasivu".
        val board = checkNotNull(BoardParser.parse(mini))

        assertEquals(2, board.dice.size)
        assertTrue(board.dice.all { it.owner != null }, "Noppien väri luetaan yhä nimestä")
        assertEquals(2, board.players.size)
        assertTrue(board.players.all { it.pips != null })
        assertNotNull(board.stateToken)
    }

    @Test
    fun `Mini pudottaa myos edellisen siirron nuolet`() {
        // Nuolet ovat `arrow_*.gif` ja niiden ALT on `A`. Mini-skeemassa niitä ei ole
        // lainkaan, eli tieto edellisestä siirrosta katoaa sivulta kokonaan.
        assertTrue(Jsoup.parse(normal).select("img[src*=arrow_]").isNotEmpty())
        assertTrue(Jsoup.parse(mini).select("img[src*=arrow_]").isEmpty())
    }

    @Test
    fun `Mini on sivuston oma kapean nayton lauta`() {
        // Tämä ei ole jäsennysasia vaan löytö: Mini on ainoa skeema jolla on viewport-meta,
        // eli sivustolla on jo olemassa puhelimelle tarkoitettu lauta.
        assertTrue(Jsoup.parse(mini).selectFirst("meta[name=viewport]") != null)
        assertNull(Jsoup.parse(normal).selectFirst("meta[name=viewport]"))
    }

    @Test
    fun `Hide pip counts vie vahdin muttei jasennysta`() {
        // Hypoteesi piti paikkansa. Jäsennys ei riko mitään, ja juuri se tekee viasta
        // hiljaisen: `BoardParserTest` vertaa laudasta laskettua summaa paneelin lukuun, ja
        // ilman lukua vertailukohtaa ei ole. Vahti muuttuu tyhjäksi lupaukseksi eikä kaadu.
        val board = checkNotNull(BoardParser.parse(noPips))

        assertEquals(2, board.players.size)
        assertTrue(board.players.all { it.player.displayName.isNotBlank() })
        assertTrue(board.players.all { it.pips == null }, "Pip-luku on piilotettu")

        // Lauta itse on tallessa, eli virhe ei näy missään muualla kuin vahdin puuttumisena.
        assertEquals(24, board.points.size)
        assertTrue(board.points.any { it.count > 0 })
    }

    @Test
    fun `pip-vahdin lahde on paneelissa eika laudassa`() {
        // Sama lauta molemmissa, ja ero on vain paneelin luvussa. Tämä väittää nimenomaan
        // sen että laudasta laskettava puoli ei muutu, eli vahdin kaksi puolta eivät ole
        // samaa lähdettä.
        val withPips = checkNotNull(BoardParser.parse(normal))
        val without = checkNotNull(BoardParser.parse(noPips))

        assertEquals(withPips.points, without.points)
        assertFalse(withPips.players.map { it.pips } == without.players.map { it.pips })
    }

    @Test
    fun `skeema luetaan sivulta eika paatella tyhjasta listasta`() {
        // Kohta 13.8.2026 asti auki: mitä `parse` palauttaa kun Mini pudottaa pistenumerot.
        // Sopimus ei muuttunut, koska syytä ei tarvitse päätellä seurauksesta: hakemisto on
        // sivulla, ja se on mitattu jo tämän luokan ensimmäisessä testissä. Tämä väittää
        // että jäsennin myös lukee sen.
        assertEquals(BoardScheme.BLUE_WHITE, checkNotNull(BoardParser.parse(normal)).scheme)
        assertEquals(BoardScheme.MINI, checkNotNull(BoardParser.parse(mini)).scheme)
    }

    @Test
    fun `tuntematon hakemisto ei ole Mini eika mikaan muukaan`() {
        // Suunta on tässä tärkeä: tuntematon luetaan tuntemattomaksi eikä lähimmäksi
        // tunnetuksi. Väärä arvaus näkyisi käyttäjälle neuvona vaihtaa asetus jota vika ei
        // koske, eli ohjeena tehdä turha muutos oikean pelin asetuksiin.
        val outoHakemisto = normal.replace("/images/2/", "/images/9/")

        assertNull(checkNotNull(BoardParser.parse(outoHakemisto)).scheme)
    }

    @Test
    fun `ristiriitainen sivu ei ole mikaan skeema`() {
        // Kahta hakemistoa samalla sivulla ei ole nähty, eikä sitä siksi voi tulkita. Jos
        // se joskus näkyy laitteella, sivu kuuluu ottaa talteen eikä arvata täällä.
        val sekasivu = normal.replaceFirst("/images/2/", "/images/1/")

        assertNull(checkNotNull(BoardParser.parse(sekasivu)).scheme)
    }

    private fun fileNames(html: String): Set<String> =
        Jsoup.parse(html).select("img[src*=pt_], img[src*=die_]")
            .map { it.attr("src").substringAfterLast('/') }
            .toSortedSet()

    private fun directories(html: String): Set<String> =
        Jsoup.parse(html).select("img[src*=pt_], img[src*=die_]")
            .map { it.attr("src").substringBeforeLast('/') }
            .toSortedSet()
}
