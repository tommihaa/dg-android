package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckerColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Lautasivun **lomake**, eli ne toiminnot jotka eivät ole linkkejä (kaapattu 4.8.2026,
 * ensimmäinen kokonainen pelisessio lokittavan välityspalvelimen läpi).
 *
 * Miksi tämä on oma testinsä: siirtojen kokoaminen on linkkejä, mutta **jokainen vuoron
 * päättävä ja kuutiota koskeva toiminto on lomakkeen nappi**. Jäsennin luki 4.8.2026 asti
 * vain ankkureita, joten Submit Move, Submit Greedy Bearoff, Roll Dice, Double, Accept ja
 * Decline olivat kaikki näkymättömiä yhtä aikaa. Katve ei näkynyt mistään, koska aiemmat
 * fixturet olivat kaikki tiloista joissa lomakkeella ei ollut mitään kiinnostavaa.
 */
class BoardParserFormTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private fun board(name: String) = checkNotNull(BoardParser.parse(fixture(name))) {
        "Lautasivu ei jäsentynyt lainkaan: $name"
    }

    @Test
    fun `kesken koottu siirto tarjoaa vain peruutuksen`() {
        // Osoite oli ?move=m, eli yksi nappula kahdesta on siirretty. Sivulla on Undo Move
        // muttei piilokenttaa eika lahetysnappia.
        val board = board("move_assembling.html")

        assertNotNull(board.undoHref)
        assertNull(board.form?.pendingMove)
        assertTrue(
            board.form?.submits.orEmpty().isEmpty(),
            "Kesken kootulla siirrolla ei saa olla lahetysnappia",
        )
    }

    @Test
    fun `taysi siirto tuo piilokentan ja lahetysnapin`() {
        // Osoite oli ?move=rrmm, eli tuplaheitto ja nelja nappulaa. Vertaa edelliseen:
        // sivu kertoo itse milloin siirron voi lahettaa, eika sita tarvitse paatella
        // nopista tai kirjainten lukumaarasta.
        val board = board("move_submit_ready.html")
        val form = checkNotNull(board.form)

        assertEquals("rrmm", form.pendingMove)
        assertEquals(listOf("Submit Move"), form.submits)
        assertNotNull(board.undoHref)
    }

    @Test
    fun `undo tyhjentaa koko kokoamisen eika viimeista nappulaa`() {
        // Nelja kirjainta koottuna, ja peruutusosoite on tilatunniste ilman move-parametria.
        // Sama havainto kolmen kirjaimen kohdalta samassa ottelussa, joten kyse ei ole
        // siita etta viimeinen kirjain katoaisi.
        val undo = checkNotNull(board("move_submit_ready.html").undoHref)

        assertFalse(undo.contains("move="), "Undo-osoitteessa ei saa olla koottua siirtoa")
        assertTrue(undo.endsWith("?"), "Undo on tilatunniste tyhjalla kyselylla: $undo")
    }

    @Test
    fun `greedy bearoff on sivuston esilaskema siirto`() {
        // Tama on eri asia kuin kokoaminen: piilokentassa on valmis nelikirjaiminen siirto
        // ilman yhtaan kokoamisaskelta. Sessiossa nakyi kuusi perakkaista greedy-lahetysta
        // joiden valissa ei ollut ?move=-askelia lainkaan.
        val form = checkNotNull(board("move_greedy_bearoff.html").form)

        assertEquals("jgcc", form.pendingMove)
        assertEquals(listOf("Submit Greedy Bearoff"), form.submits)
    }

    @Test
    fun `kuution tarjoaminen antaa kaksi nappia ja yhden valintaruudun`() {
        val board = board("move_cube_offered.html")
        val form = checkNotNull(board.form)

        assertEquals("vastapelaaja has doubled.", board.prompt)
        assertEquals(listOf("Accept", "Decline"), form.submits)
        assertEquals("Accept", form.verify)
    }

    @Test
    fun `verify-ruudun arvo on sivukohtainen vakio eika seuraa painettua nappia`() {
        // Tama kumosi kanonin saannon 4.8.2026. Aiempi teksti sanoi etta ?submit=Decline
        // tulee ilman verifyia, mutta mitattu osoite oli ?submit=Decline&verify=Accept.
        // Syy nakyy tasta: ruudulla on yksi arvo, ja se on sivun myonteinen toiminto.
        // Vanha saanto oli paatelty otoksesta jossa ruutu sattui olemaan rastittamatta.
        val offered = checkNotNull(board("move_cube_offered.html").form)

        assertEquals("Accept", offered.verify)
        assertTrue(
            offered.submits.contains("Decline"),
            "Samalla sivulla on Decline, joten verify ei voi tarkoittaa painettua nappia",
        )
    }

    @Test
    fun `heittovuoro tarjoaa Roll Dicen ja Doublen samalta lomakkeelta`() {
        // Sivu on 22.8.2026 tallennettu siita ottelusta jota katsottiin laitteella samalla
        // hetkella kun ruudulta puuttuivat molemmat napit. Testi on tassa siksi etta se
        // erottaa kaksi selitysta: jasennin lukee molemmat napit, joten ruudun tyhjyys oli
        // asettelua eika jasennysta. Sivu on merkistoltaan windows-1252, ja se on ensimmainen
        // fixture joka on tallennettu selaimen lahdenakymasta eika proxysta.
        val form = checkNotNull(board("move_roll_double.html").form)

        assertEquals(listOf("Roll Dice", "Double"), form.submits)
        assertEquals("Double", form.verify)
        assertNull(form.pendingMove, "Heittovuorolla ei ole koottua siirtoa")
    }

    @Test
    fun `verify-ruutu katoaa sivulta kun pelaajan vahvistusasetus on pois`() {
        // Mitattu 24.8.2026 kahdella tunnuksella: sama heittovuoron sivu haettiin
        // vahvistusasetus paalla (move_roll_double.html, verify=Double) ja pois
        // (tama fixture), ja ainoa ero lomakkeissa on verify-kentan olemassaolo.
        // Sivu siis kantaa pelaajan asetuksen itse, eika asetussivua tarvita sen
        // lukemiseen. Ks. docs/AVOIMET.md, kuution vahvistusruutu.
        val form = checkNotNull(board("move_roll_double_no_verify.html").form)

        assertEquals(listOf("Roll Dice", "Double"), form.submits)
        assertNull(form.verify, "Asetus pois: sivulla ei ole verify-kenttaa")
    }

    @Test
    fun `ulos kannetut nappulat luetaan tiedostonimesta eika ALTista`() {
        // ALT riippuu paikasta eika sisallosta: off_y3_bot on ALT="y3" mutta off_b3_top on
        // ALT="3" ilman varikirjainta. ALTiin nojaava luku menettaisi puolet tapauksista.
        val borneOff = board("move_borne_off.html").borneOff

        assertTrue(borneOff.isNotEmpty(), "Fixture on valittu siksi etta siina on ulos kannettuja")
        assertTrue(
            borneOff.all { it.count in 1..15 },
            "Lukumaaran on oltava jarkeva: $borneOff",
        )
        assertTrue(
            borneOff.any { it.color == CheckerColor.BLUE },
            "Sinisen ALT on paljas luku, ja juuri se katoaisi ALT-pohjaisella luvulla",
        )
    }

    @Test
    fun `taysi pino lasketaan mukaan ulos kannettuihin`() {
        // Mitattu oikeassa ottelussa 22.8.2026: ruudulla luki 4 kun nappuloita oli 9.
        // Sivusto piirtaa taydet viisikot ilman paikkaosaa (off_y5.gif) ja osittaiset sen
        // kanssa (off_y4_top.gif), ja vanha lauseke vaati alaviivan luvun jalkeen. Jokainen
        // taysi pino putosi siis hiljaa, eika yksikaan fixture sisaltanyt sellaista.
        //
        // Pip-luku oli oikein koko ajan, koska se luetaan sivulta. Juuri se teki viasta
        // vaikean huomata: kaksi lukua samalla rivilla, joista toinen oli oikein.
        val borneOff = board("move_borne_off_full_stack.html").borneOff

        assertEquals(1, borneOff.size, "Sivulla on yksi vari kahtena pinona: $borneOff")
        assertEquals(9, borneOff.single().count)
        assertEquals(CheckerColor.YELLOW, borneOff.single().color)
    }

    @Test
    fun `lomakkeen ottelutunniste voi olla eri kuin pyydetty`() {
        // Sivu haettiin osoitteesta /bg/move/<A>/457 ja sen lomake osoittaa otteluun <B>:
        // sivusto siirsi seuraavaan peliin kun edellinen siirto oli tehty. Sama periaate
        // kuin isLoginPagessa mutta terävämpi: myöskään OTTELU ei ole paateltavissa
        // pyydetysta osoitteesta, ei vain sivun laji.
        val form = checkNotNull(board("move_cube_offered.html").form)

        assertTrue(
            form.action.startsWith("/bg/move/"),
            "Lomakkeen action luetaan sellaisenaan: ${form.action}",
        )
    }
}
