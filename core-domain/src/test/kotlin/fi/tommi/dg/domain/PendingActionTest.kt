package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Se ehto joka tekee uudelleenyrityksestä turvallisen.
 *
 * **Kysymys jota nämä testit koskevat on "menikö teko perille", eikä verkkokerros voi
 * vastata siihen.** `DgResponse.Offline` syntyy poikkeuksesta joka voi osua joko ennen sitä
 * kun palvelin luki pyynnön tai sen jälkeen. Vastaus on siksi haettava sivulta itseltään:
 * palvelin pitää siirron kokoamistilaa, ja se tyhjenee kun siirto on lähetetty.
 *
 * Ilman tätä ehtoa jono olisi sokea toisto, eli täsmälleen se mitä `DgClient`in uusintaraja
 * (`Intent.ACT`) on tehty estämään.
 */
class PendingActionTest {

    @Test
    fun `sama nappi ja sama kokoamistila kelpaa toistettavaksi`() {
        val lauta = lauta(lomake(submits = listOf("Submit Move"), pendingMove = "rrmm"))

        assertTrue(lauta.stillOffers(painallus(submit = "Submit Move", pendingMove = "rrmm")))
    }

    @Test
    fun `muuttunut kokoamistila estaa toiston`() {
        // Sivu on samassa ottelussa ja tarjoaa saman napin, mutta kokoamistila on toinen.
        // Ilman kolmatta ehtoa toisto lähettäisi siirron uudelleen tilanteessa jossa
        // edellinen saattoi jo onnistua.
        //
        // Testin nimi sanoi 10.8.2026 asti että muuttunut tila *tarkoittaa* teon menneen
        // perille. Laiteajo kumosi sen: tila nollautuu myös pelkästä uudelleenhausta, joten
        // ero ei erota lähetettyä nollautuneesta. Ehto jää voimaan, koska se erehtyy
        // turvalliseen suuntaan, mutta se ei enää väitä tietävänsä kumpi tapahtui.
        val lauta = lauta(lomake(submits = listOf("Submit Move"), pendingMove = "ff"))

        assertFalse(lauta.stillOffers(painallus(submit = "Submit Move", pendingMove = "rrmm")))
    }

    @Test
    fun `puuttuva kokoamistila ei vastaa olemassa olevaan`() {
        // Null vastaa vain nulliin. Lomake jolla ei ole piilokenttää on eri kysymys kuin
        // lomake jolla on, eikä eroa lueta puuttuvaksi tiedoksi.
        val lauta = lauta(lomake(submits = listOf("Submit Move"), pendingMove = null))

        assertFalse(lauta.stillOffers(painallus(submit = "Submit Move", pendingMove = "rrmm")))
    }

    @Test
    fun `nappi joka on kadonnut sivulta ei kelpaa`() {
        val lauta = lauta(lomake(submits = listOf("Roll Dice"), pendingMove = "rrmm"))

        assertFalse(lauta.stillOffers(painallus(submit = "Submit Move", pendingMove = "rrmm")))
    }

    @Test
    fun `lomakkeeton sivu ei tarjoa mitaan`() {
        assertFalse(lauta(form = null).stillOffers(painallus()))
    }

    @Test
    fun `kokoamistilaa kantava painallus tunnistetaan lajiltaan`() {
        assertTrue(painallus(submit = "Submit Move", pendingMove = "rrmm").dependsOnAssembly)
    }

    @Test
    fun `itsenainen painallus ei riipu kokoamisesta`() {
        // Laji luetaan piilokentästä eikä napin nimestä, joten sama nimi ilman kenttää on
        // itsenäinen teko. Nimeen sidottu ehto olisi arvaus sanasta.
        assertFalse(painallus(submit = "Roll Dice", pendingMove = null).dependsOnAssembly)
        assertFalse(painallus(submit = "Submit Move", pendingMove = null).dependsOnAssembly)
    }

    private fun painallus(
        submit: String = "Submit Move",
        pendingMove: String? = "rrmm",
    ) = PendingAction(
        id = 1,
        matchId = MatchId("7000011"),
        boardPath = "/bg/move/7000011/117",
        submit = submit,
        pendingMove = pendingMove,
        verified = false,
        createdAtEpochMillis = 1_000,
    )

    private fun lomake(
        submits: List<String>,
        pendingMove: String? = null,
    ) = BoardForm(
        action = "/bg/move/7000011/117",
        method = FormMethod.GET,
        pendingMove = pendingMove,
        submits = submits,
        verify = null,
    )

    private fun lauta(form: BoardForm?) = BoardState(
        matchId = MatchId("7000011"),
        moveNumber = null,
        stateToken = null,
        eventName = null,
        eventId = null,
        round = null,
        matchLength = null,
        cube = null,
        dice = emptyList(),
        scheme = BoardScheme.BLUE_WHITE,
        points = emptyList(),
        players = emptyList(),
        prompt = null,
        notices = emptyList(),
        rolledBack = false,
        speculative = false,
        moves = emptyList(),
        commands = emptyList(),
        form = form,
        undoHref = null,
        borneOff = emptyList(),
        bar = emptyList(),
    )
}
