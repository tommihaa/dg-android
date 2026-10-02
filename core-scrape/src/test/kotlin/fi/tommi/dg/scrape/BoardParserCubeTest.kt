package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CubePosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Kuutio, mitattuna kuudesta oikeasta ottelusta 9.8.2026 (`CubeCaptureLiveTest`).
 *
 * Kaksi asiaa ratkesi samalla ajolla, ja ne olivat eri kysymyksiä. **Kuutiokuva on joka
 * sivulla**, joten laiteajossa nähty tyhjä keskitila ei ollut puuttuva kuva vaan ALT jota ei
 * voinut lukea luvuksi: `cubedr.gif` sanoo `dr`. Ja **omistetun kuution solu on eri rivillä**
 * kuin omistamattoman, eli omistajuus on sijainti eikä sivun uusi kenttä. Jälkimmäinen oli
 * kanonissa avoin kohta, jonka ratkaisuksi oli nimetty täsmälleen tämä koe.
 */
class BoardParserCubeTest {

    private fun board(name: String) = BoardParser.parse(
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText(),
    )

    @Test
    fun `ei-numeerinen ALT säilyy eikä muutu nulliksi`() {
        val cube = checkNotNull(board("move_cube_dr.html")?.cube)

        assertEquals("dr", cube.label)
        // Tämä on koko vian ydin: vanha jäsennin palautti tästä nullin, ja null on
        // erottamaton siitä ettei kuutiota ole sivulla lainkaan.
        assertNull(cube.value)
        assertNotNull(board("move_cube_dr.html")?.cube)
    }

    @Test
    fun `sama kuva kahdella ALTilla antaa saman arvon`() {
        // Mitattu 10.8.2026 laiteajon yhteydessä: `/images/2/cube2.gif` kantaa ottelussa
        // 5309133 ALTia `cube2` ja ottelussa 5310938 ALTia `2`. Kumpikin on kakkoskuutio, ja
        // ALTista luettu arvo olisi toisessa 2 ja toisessa null. Ruudulla se näkyi tekstinä
        // `CUBE2` kuution numeron sijaan.
        val altSana = checkNotNull(board("move_cube_alt_cube2.html")?.cube)
        val altNumero = checkNotNull(board("move_cube_owned.html")?.cube)

        assertEquals(2, altSana.value)
        assertEquals(2, altNumero.value)

        // ALT säilyy raakana kummassakin, koska sivun sana on sivun sana. Vain arvo
        // luetaan muualta.
        assertEquals("cube2", altSana.label)
        assertEquals("2", altNumero.label)
    }

    @Test
    fun `DR luetaan tiedostonimestä eikä ALTista`() {
        // Sama vika kuin kakkoskuutiolla, nyt DR:llä: `cubedrs.gif` kantaa ALTia `dr` 34
        // sivulla ja `cubedrs` 13:lla (mitattu 3.9.2026 koko korpuksesta), ja ALTista
        // luettuna kuutiossa luki laitteella `CUBEDRS`. ALT säilyy raakana kuten ennenkin.
        val altSana = checkNotNull(board("move_cube_drs_alt.html")?.cube)
        val altDr = checkNotNull(board("move_cube_dr.html")?.cube)

        assertEquals(true, altSana.doubleRepeat)
        assertEquals(true, altDr.doubleRepeat)
        assertEquals("cubedrs", altSana.label)
        assertNull(altSana.value)
        assertEquals(CubePosition.BOTTOM, altSana.position)

        // Numerokuutio ei ole DR, vaikka sen ALT olisi sana.
        assertEquals(false, board("move_cube_alt_cube2.html")?.cube?.doubleRepeat)
    }

    @Test
    fun `omistamaton kuutio on keskimmäisessä solussa`() {
        listOf("move_board.html", "move_cube_dr.html").forEach { name ->
            assertEquals(CubePosition.MIDDLE, board(name)?.cube?.position, name)
        }
    }

    @Test
    fun `omistettu kuutio on ylimmässä solussa ja kantaa arvon`() {
        val cube = checkNotNull(board("move_cube_owned.html")?.cube)

        assertEquals("2", cube.label)
        assertEquals(2, cube.value)
        // Sama solu on omistamattomalla sivulla tyhjä, joten paikka on merkitsevä eikä
        // taulukon täytettä.
        assertEquals(CubePosition.TOP, cube.position)
    }

    @Test
    fun `paikka luetaan solun omasta valignista eikä rivien järjestyksestä`() {
        // Rivi-indeksi antaisi näistä fixtureista saman vastauksen kuin valign, joten
        // kumpaakaan lukutapaa ei voi erottaa niistä sellaisenaan. Sama sivu ilman kuution
        // solun valignia erottaa: rivi on yhä ylin, joten rivejä laskeva jäsennin sanoisi
        // TOP. Tulos on UNKNOWN eikä MIDDLE, koska keskikohta on sivulla oma merkityksensä
        // eikä oletusarvo.
        val html = checkNotNull(javaClass.getResourceAsStream("/fixtures/move_cube_owned.html"))
            .reader(Charsets.UTF_8).readText()
            .replaceFirst("VALIGN=top ", "")

        assertEquals(CubePosition.UNKNOWN, BoardParser.parse(html)?.cube?.position)
    }
}
