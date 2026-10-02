package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.pipCount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Laudan suunta: käyttäjäasetus `Home boards on left side` peilaa koko sivun.
 *
 * Fixturet ovat sama lauta samalta hetkeltä, luettuna asetuksen molemmilla arvoilla
 * (kaapattu 1.8.2026). Pari on olemassa juuri tätä testiä varten: kaksi eri näköistä
 * sivua, joiden on tuotettava sama piste-erittely.
 *
 * Aiempi jäsennin päätteli pisteen numeron kuvan suunnasta ja tuotti peilatusta sivusta
 * väärän laudan tuottamatta virhettä. Se on pahin mahdollinen vikamuoto tässä
 * sovelluksessa, koska väärä pistenumero olisi näkynyt vasta väärinä siirtoina.
 */
class BoardParserOrientationTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val oikea = checkNotNull(BoardParser.parse(fixture("move_board_oikea.html")))
    private val vasen = checkNotNull(BoardParser.parse(fixture("move_board_vasen.html")))

    @Test
    fun `peilattu lauta tuottaa saman piste-erittelyn`() {
        assertEquals(24, oikea.points.size)
        assertEquals(24, vasen.points.size)
        assertEquals((1..24).toList(), vasen.points.map { it.number })
        assertEquals(oikea.points, vasen.points)
    }

    @Test
    fun `siirtolinkit osoittavat samoille pisteille kummassakin suunnassa`() {
        // Tämä pari on kaapattu nopanheittoa edeltävässä tilassa, jossa sivulla ei ole
        // yhtään ?move=-linkkiä. Väite on siis toistaiseksi tyhjä molemmilla puolilla, ja
        // se sanotaan ääneen: siirtolinkkien numerointia vahtii move_board.html, ei tämä.
        assertEquals(oikea.moves, vasen.moves)
        assertTrue(oikea.moves.isEmpty(), "Fixture vaihtui: nyt sivulla on siirtolinkkejä")
    }

    @Test
    fun `pip-vahti pitää molemmissa suunnissa`() {
        // Sama vahti kuin BoardParserTestissä: laudan ja pelaajapaneelin on täsmättävä.
        // Peilatulla sivulla vanha jäsennin antoi tästä 174/201 ja 165/185, eli virhe
        // näkyi juuri tässä.
        listOf(oikea, vasen).forEach { board ->
            assertEquals(138, board.pipCount(CheckerColor.YELLOW, homeIsLowNumbers = false))
            // Sininen jää 25 vajaaksi paneelin luvusta 162, koska yksi nappula on
            // muurilla. Muuri ei ole piste eikä siksi mukana summassa.
            //
            // Tämä väite jää tänne vaikka muuri on 5.8.2026 alkaen jäsennetty: se on yhä
            // tosi pipCountista, jonka sopimus on nimenomaan "vain pisteet". Se on samalla
            // se testi joka hajoaa jos joku sulauttaa muurin pipCountiin. Muurin kanssa
            // laskettua täsmäystä väittää BoardParserBarTest.
            assertEquals(137, board.pipCount(CheckerColor.BLUE, homeIsLowNumbers = true))
            assertEquals(setOf(138, 162), board.players.mapNotNull { it.pips }.toSet())
        }
    }

    @Test
    fun `muurilla oleva nappula ei päädy pisteeksi`() {
        // Muurin kuva on bar_b1.gif eikä pt_-alkuinen, ja sen sarake on numerorivillä
        // tyhjä. Kumpi tahansa ehto yksin riittäisi, ja molemmat ovat voimassa.
        //
        // Sininen on siis 14 pisteillä, ja puuttuva viidestoista on muurilla eikä hukassa.
        // Väite on tässä muodossa myös sen jälkeen kun muuri on jäsennetty, koska se vahtii
        // nimenomaan sitä ettei muuri vuoda pisteisiin.
        listOf(oikea, vasen).forEach { board ->
            assertEquals(15, board.points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count })
            assertEquals(14, board.points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count })
            assertEquals(1, board.bar.sumOf { it.count })
        }
    }
}
