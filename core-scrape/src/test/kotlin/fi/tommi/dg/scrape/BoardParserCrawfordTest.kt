package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CrawfordReading
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Crawford-peli, mitattuna koko kaapatusta korpuksesta 14.8.2026 (648 lautasivua).
 *
 * Sivu merkitsee sen kahdella tavalla yhtä aikaa: tähti johtajan pistemäärän perässä ja
 * kuutiokuvan puuttuminen kokonaan. Merkit olivat samaa mieltä kaikilla 39 Crawford-sivulla,
 * eikä yhtään erimielistä sivua ollut.
 *
 * **Fixturet ovat samasta ottelusta peräkkäisistä peleistä**, ja se on niiden koko arvo:
 * `move_crawford.html` on 6*-3 ilman kuutiota ja `move_after_crawford.html` seuraava peli
 * 6-4 kuution kanssa. Kaksi eri ottelua ei erottaisi Crawfordia siitä, että jokin muu asia
 * eroaa otteluiden välillä.
 */
class BoardParserCrawfordTest {

    private fun board(name: String) = BoardParser.parse(
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText(),
    )

    private fun html(name: String) =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    @Test
    fun `tähti säilyy pistekentässä eikä pudota lukua`() {
        val panels = checkNotNull(board("move_crawford.html")).players
        val johtaja = panels.single { it.scoreLabel?.contains('*') == true }

        // Molemmat: merkki tallessa raakana ja luku yhä luettavissa. Vanha jäsennin antoi
        // jälkimmäisen ja hukkasi edellisen sanomatta mitään.
        assertEquals("6*", johtaja.scoreLabel)
        assertEquals(6, johtaja.score)

        val toinen = panels.single { it.scoreLabel?.contains('*') != true }
        assertEquals("3", toinen.scoreLabel)
        assertEquals(3, toinen.score)
    }

    @Test
    fun `kuutio puuttuu Crawford-sivulta ja on seuraavassa pelissä`() {
        assertNull(checkNotNull(board("move_crawford.html")).cube)
        assertNotNull(checkNotNull(board("move_after_crawford.html")).cube)
    }

    @Test
    fun `molemmat merkit yhdessä antavat Crawfordin`() {
        assertEquals(CrawfordReading.CRAWFORD, checkNotNull(board("move_crawford.html")).crawford)
    }

    @Test
    fun `sama ottelu seuraavassa pelissä ei ole enää Crawford`() {
        val jalkeen = checkNotNull(board("move_after_crawford.html"))

        assertEquals(CrawfordReading.CUBE_IN_PLAY, jalkeen.crawford)
        // Pistetilanne on yhä pistettä vaille voittoa, eli tähti ei ole `n-1`:n synonyymi.
        // Juuri tämä erottaa sivun oman merkinnän siitä mitä pisteistä voisi laskea.
        assertEquals(listOf(6, 4), jalkeen.players.map { it.score })
        assertEquals(7, jalkeen.matchLength)
    }

    @Test
    fun `double repeat ei ole Crawford eikä tuntematon`() {
        // Tommin muistutus 14.8.2026: DR-turnauksissa ei ole Crawford-sääntöä lainkaan.
        // Korpuksen DR-ottelu oli `n-1`:ssä eikä kantanut kumpaakaan merkkiä, joten oikea
        // vastaus on sivun oma tila eikä puuttuva tieto.
        assertEquals(CrawfordReading.CUBE_IN_PLAY, checkNotNull(board("move_cube_dr.html")).crawford)
    }

    @Test
    fun `eri mieltä olevat merkit jäävät tuntemattomaksi`() {
        // Tätä sivua ei ole nähty eikä sen pitäisi olla mahdollinen: kuutio on sivulla ja
        // tähti on silti pistekentässä. Ristiriita rakennetaan tähän käsin, koska sen
        // seuraus on juuri se jota kolmas arvo on varten.
        val ristiriita = html("move_after_crawford.html").replace("score: <B>6</B>", "score: <B>6*</B>")

        val luettu = checkNotNull(BoardParser.parse(ristiriita))
        assertNotNull(luettu.cube)
        assertEquals(CrawfordReading.UNKNOWN, luettu.crawford)
    }
}
