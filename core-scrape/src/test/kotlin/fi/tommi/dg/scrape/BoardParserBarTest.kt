package fi.tommi.dg.scrape

import fi.tommi.dg.domain.BarCheckers
import fi.tommi.dg.domain.CheckerCheck
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.PipCheck
import fi.tommi.dg.domain.reconcileCheckers
import fi.tommi.dg.domain.reconcilePips
import fi.tommi.dg.domain.totalPipCount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Muurilla (bar) olevat nappulat.
 *
 * Muuri oli jäsentimen katve 5.8.2026 asti, ja katve oli hiljainen: laudasta puuttui
 * nappuloita ilman että mikään huomautti. `BoardParserOrientationTest` oli kirjannut aukon
 * kommenttina (sininen 137 kun paneeli sanoi 162), eli se tiedettiin muttei korjattu.
 *
 * Sama vikamuoto kuin ulos kannetuilla: väri on tiedostonimessä eikä ALTissa. Se löytyi
 * vasta kolmatta fixturea katsomalla, mikä on tämän testiluokan olemassaolon syy.
 */
class BoardParserBarTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private fun board(name: String) = checkNotNull(BoardParser.parse(fixture(name)))

    /**
     * Muurin siirtolinkki, eli se teko jolla nappula tuodaan takaisin laudalle.
     *
     * **Tama oli estava katve 22.8.2026 asti**, ja se on eri katve kuin luokan otsikossa
     * kuvattu: nappulat luettiin oikein, mutta linkkia niiden ymparilta ei luettu lainkaan.
     * Sovelluksella ei siis voinut pelata yhtakaan vuoroa jossa oli tullut syodyksi.
     *
     * Syy oli tyypissa eika unohduksessa. Siirrot kulkevat `MoveLink`ina jolla on pakollinen
     * `fromPoint: Int`, ja muurilla ei ole numeroa, joten muurin linkki ei mahtunut siihen
     * putkeen ja suodattui pois numeroituihin pisteisiin rajaavassa ehdossa.
     *
     * Fixture on talteenotettu oikeasta ottelusta 22.8.2026 ja anonymisoitu. Se on ainoa
     * fixture jossa muurin nappula on klikattava: viisi muuta muurillista sivua ovat kaikki
     * tilanteita joissa linkkia ei ole, mika on itsessaan syy siihen ettei katve nakynyt.
     */
    @Test
    fun `muurin nappulan linkki luetaan sivulta`() {
        val board = board("move_bar_entry.html")

        val bar = board.bar.single()
        assertEquals(CheckerColor.YELLOW, bar.color)
        assertEquals(1, bar.count)
        assertEquals("/bg/move/7000031/289?move=y", bar.href)
    }

    /**
     * Muurin linkki on `null` silloin kun sivu ei tarjoa sita, eika esimerkiksi tyhja
     * merkkijono. Ilman tata ruutu tekisi kosketettavan alueen sellaisesta muurista jolla
     * ei ole tekoa, ja napautus veisi osoitteeseen jota ei ole.
     */
    @Test
    fun `muuri ilman linkkia jattaa osoitteen tyhjaksi`() {
        // Vastustajan nappula muurilla: sivu ei tarjoa sille linkkia, koska teko ei ole
        // kirjautuneen pelaajan.
        assertTrue(board("move_rollback.html").bar.all { it.href == null })
        assertTrue(board("move_board_oikea.html").bar.all { it.href == null })
    }

    /**
     * Muurin linkki ei saa vuotaa siirtolistaan, koska muurilla ei ole pistenumeroa jolle
     * se kuuluisi. Vaara numero tuottaisi vaaran siirron eika virhetta.
     */
    @Test
    fun `muurin linkki ei nay siirtolistassa`() {
        val board = board("move_bar_entry.html")

        assertTrue(board.moves.none { it.code == "y" }, "Muuri ei ole piste: ${board.moves}")
        assertTrue(board.moves.all { it.fromPoint in 1..24 })
    }

    @Test
    fun `peilattu lauta tuottaa saman muurin`() {
        // Sama työ jonka peilipari jo tekee pisteille: kaksi eri näköistä sivua samalta
        // hetkeltä, ja niiden on tuotettava sama tulos.
        val oikea = board("move_board_oikea.html")
        val vasen = board("move_board_vasen.html")

        assertEquals(listOf(BarCheckers(CheckerColor.BLUE, 1)), oikea.bar)
        assertEquals(oikea.bar, vasen.bar)
    }

    @Test
    fun `muurin väri luetaan tiedostonimestä eikä ALTista`() {
        // Tämä on koko luokan kantava väite. move_rollback.html sisältää saman kuvan
        // bar_b1.gif kuin orientaatiopari, mutta sen ALT on paljas "1" ilman värikirjainta.
        // ALT_CHECKERSia uudelleen käyttävä jäsennin antaisi tästä nullin värin, ja tämä
        // testi menee silloin punaiseksi. Sama poikkeus kuin ulos kannetuilla ja nopilla.
        assertEquals(
            listOf(BarCheckers(CheckerColor.BLUE, 1)),
            board("move_rollback.html").bar,
        )
    }

    @Test
    fun `tyhjä muuri on tyhjä lista eikä puuttuva tieto`() {
        listOf("move_board.html", "move_borne_off.html", "move_cube_offered.html").forEach { name ->
            assertTrue(board(name).bar.isEmpty(), "Odotettiin tyhjää muuria: $name")
        }
    }

    @Test
    fun `muuri mukaan luettuna nappuloita on 15 kumpaakin väriä`() {
        // Backgammonin invariantti, ja nyt se on väitettävissä myös siitä fixturesta jossa
        // yksi nappula on muurilla. Ennen tätä lukema oli 14 ja se oli kommentoitu
        // hyväksytyksi vajaudeksi.
        listOf("move_board_oikea.html", "move_board_vasen.html").forEach { name ->
            val board = board(name)
            CheckerColor.entries.forEach { color ->
                val onPoints = board.points.filter { it.owner == color }.sumOf { it.count }
                val onBar = board.bar.filter { it.color == color }.sumOf { it.count }
                val off = board.borneOff.filter { it.color == color }.sumOf { it.count }
                assertEquals(15, onPoints + onBar + off, "$name, $color")
            }
        }
    }

    @Test
    fun `muurillisen laudan pip-luku täsmää paneeliin vasta muurin kanssa`() {
        // Aukko lakkaa olemasta kommentti ja muuttuu väitteeksi: pisteistä laskettu 137 on
        // yhä tosi (BoardParserOrientationTest väittää sen), mutta paneelin luku on 162 ja
        // sen saa vain muurin kanssa.
        listOf("move_board_oikea.html", "move_board_vasen.html").forEach { name ->
            val board = board(name)
            assertEquals(138, board.totalPipCount(CheckerColor.YELLOW, homeIsLowNumbers = false))
            assertEquals(162, board.totalPipCount(CheckerColor.BLUE, homeIsLowNumbers = true))
        }
    }

    @Test
    fun `pip-vertailu tunnistaa suunnan itse ja täsmää paneeliin`() {
        // reconcilePips kokeilee molemmat suunnat, koska mallissa ei ole orientaatiokenttää
        // eikä suuntaa voi päätellä väristä. Huomaa että nämä kaksi fixturea vaativat eri
        // suunnan: se on juuri se syy miksi kokeilu on olemassa.
        assertEquals(PipCheck.Agrees(99, 117), board("move_board.html").reconcilePips())
        assertEquals(PipCheck.Agrees(138, 162), board("move_board_oikea.html").reconcilePips())
        assertEquals(PipCheck.Agrees(138, 162), board("move_board_vasen.html").reconcilePips())
    }

    @Test
    fun `nappulavahti laskee kolme lahdetta ja tasmaa oikeilla laudoilla`() {
        // Pisteet, muuri ja ulos kannetut. Vahti on olemassa siksi etta pip lukee naista
        // vain kaksi ensimmaista: ulos kannettu nappula ei tuota pipseja, joten sen
        // katoaminen on pip-vahdille nakymaton (mitattu 22.8.2026).
        assertEquals(CheckerCheck.Agrees(15), board("move_board.html").reconcileCheckers())
        assertEquals(CheckerCheck.Agrees(15), board("move_borne_off.html").reconcileCheckers())
        assertEquals(
            CheckerCheck.Agrees(15),
            board("move_borne_off_full_stack.html").reconcileCheckers(),
        )
    }

    @Test
    fun `nappulavahti olisi nahnyt pudonneen pinon`() {
        // Sama sivu jolla vika mitattiin, mutta taydet pinot poistettuna. Nain nayttaisi
        // lauta jonka jasennin lukee vanhalla lausekkeella: keltaiselta puuttuu viisi.
        // Testi on siis vahdin oma naytto siita etta se olisi hälyttänyt.
        val vajaa = fixture("move_borne_off_full_stack.html").replace("off_y5.gif", "tyhja.gif")
        val board = checkNotNull(BoardParser.parse(vajaa))

        assertEquals(CheckerCheck.Disagrees(yellow = 10, blue = 15), board.reconcileCheckers())
    }

    @Test
    fun `pip-vertailu on saatavuudeltaan tyhjä kun sivu ei anna vertailukohtaa`() {
        // Kaksi eri syytä, ja kumpikaan ei ole virhe. Hide pip counts -asetus vie paneelin
        // luvut, ja Mini-skeema vie pistenumerot. Kummassakin tapauksessa vertailua ei voi
        // tehdä, ja se on eri asia kuin että vertailu ei täsmäisi.
        assertEquals(PipCheck.Unavailable, board("move_board_no_pips.html").reconcilePips())
        assertEquals(PipCheck.Unavailable, board("move_board_mini.html").reconcilePips())
    }

    @Test
    fun `ristiriita raportoidaan omana tuloksenaan`() {
        // Rakennetaan ristiriita käsin, koska sellaista sivua ei ole eikä pidäkään olla:
        // jos jäsennin menee hiljaa väärään, tämä on se tyyppi jonka käyttöliittymä näyttää.
        val board = board("move_board.html")
        val vaarennetty = board.copy(bar = listOf(BarCheckers(CheckerColor.YELLOW, 1)))

        val tulos = vaarennetty.reconcilePips()
        assertInstanceOf(PipCheck.Disagrees::class.java, tulos)
        // Joukkona eikä listana: paneelien järjestys sivulla ei ole tiedossa, ja
        // BoardParserTest vertaa samaa lukuparia samasta syystä.
        assertEquals(setOf(99, 117), (tulos as PipCheck.Disagrees).panel.toSet())
    }
}
