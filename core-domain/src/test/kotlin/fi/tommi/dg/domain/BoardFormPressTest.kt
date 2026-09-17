package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Portin kokoamiskielto, eli se tae joka korvasi lukutilan 10.8.2026.
 *
 * **Väite on muodossa "lähetystä ei voi rakentaa" eikä "lähetystä ei tehty".** Sama ero kuin
 * aiemmassa lukutilaväitteessä, joka sanoi "pyyntö ei lähtenyt" eikä "nappia ei painettu".
 * Suurin osa taetta ei ole näissä testeissä lainkaan vaan kääntäjässä: [FormSubmission]in
 * konstruktori on `internal`, joten mikään muu moduuli ei voi ohittaa [press]iä. Tämä
 * luokka asuu samassa moduulissa ja voisi siis rakentaa lähetyksen käsin; se ei tee niin,
 * koska silloin se testaisi eri asiaa kuin mitä kutsupaikat voivat tehdä.
 */
class BoardFormPressTest {

    @Test
    fun `nappia jota sivu ei tarjoa ei voi painaa`() {
        val form = lomake(submits = listOf("Roll Dice", "Double"))

        assertNull(form.press("Submit Move"))
    }

    @Test
    fun `siirtoa ei voi lahettaa ennen kuin sivu tarjoaa napin`() {
        // Mitattu piirre: `Submit Move` ilmestyy vasta kun siirto on täysi, ja kesken
        // kootussa siirrossa sivulla on vain "Undo Move". Sivu kertoo siis itse milloin
        // lähettäminen on mahdollista, eikä kirjaimia lasketa täällä.
        val kesken = lomake(submits = emptyList(), pendingMove = null)

        assertNull(kesken.press("Submit Move"))
    }

    @Test
    fun `kentat tulevat sivulta sellaisinaan`() {
        val form = lomake(
            action = "/bg/move/7000011/117",
            submits = listOf("Submit Move"),
            pendingMove = "rrmm",
        )

        val lahetys = form.press("Submit Move")

        assertEquals("/bg/move/7000011/117", lahetys?.action)
        assertEquals(mapOf("move" to "rrmm", "submit" to "Submit Move"), lahetys?.fields)
    }

    @Test
    fun `vahvistusruutu ei rastitu itsestaan`() {
        // Sivulla ruutu on rastittamaton ("Verify Double"), ja sen tarkoitus on olla toinen
        // ele ennen peruuttamatonta tekoa. Automaattinen rasti tekisi sen käyttäjän puolesta.
        val form = lomake(submits = listOf("Roll Dice", "Double"), verify = "Double")

        assertEquals(mapOf("submit" to "Double"), form.press("Double")?.fields)
    }

    @Test
    fun `rastitettu vahvistus menee sivun omalla arvolla`() {
        val form = lomake(submits = listOf("Accept", "Decline"), verify = "Accept")

        assertEquals(
            mapOf("submit" to "Accept", "verify" to "Accept"),
            form.press("Accept", verify = true)?.fields,
        )
    }

    @Test
    fun `ruudutonta lomaketta ei voi vahvistaa`() {
        // Arvoa ei ole mistä lukea, ja keksitty arvo olisi täsmälleen se mitä koko tyyppi
        // estää. Pyyntö ei kaadu vaan jättää kentän pois: nappi on yhä sivun oma.
        val form = lomake(submits = listOf("Submit Move"), pendingMove = "rrmm", verify = null)

        assertEquals(
            mapOf("move" to "rrmm", "submit" to "Submit Move"),
            form.press("Submit Move", verify = true)?.fields,
        )
    }

    @Test
    fun `laudan omat linkit ovat linkit eika lomake`() {
        val lauta = lauta(
            moves = listOf(MoveLink(fromPoint = 6, code = "f", href = "/bg/move/7/1?move=f")),
            commands = listOf(CommandLink("Swap Dice", "/bg/move/7/1?type=9&move=S")),
            undoHref = "/bg/move/7/1",
            form = lomake(action = "/bg/move/7/1", submits = listOf("Submit Move")),
        )

        assertEquals(
            setOf("/bg/move/7/1?move=f", "/bg/move/7/1?type=9&move=S", "/bg/move/7/1"),
            lauta.ownLinks(),
        )
        // Lomakkeen action ei ole seurattava linkki. Se on lomake, ja se kulkee press()in
        // kautta, jotta kentät eivät jää pois osoitetta kopioimalla.
        assertTrue(lauta.ownLinks().none { it.contains("submit=") })
    }

    /**
     * Muurilta laudalle tuominen on laudan oma linkki, eli portin on paastettava se lapi.
     *
     * **Tama oli vian toinen puolisko 22.8.2026.** Jasennin ei lukenut muurin linkkia, joten
     * se ei voinut olla taassa joukossa. Kun jasennin korjattiin, ruutu piirsi kosketettavan
     * alueen mutta `BoardViewModel.follow` hylkasi napautuksen hiljaa jasenyystarkistuksessa.
     * Vika ei siis nakynyt kaannoksessa eika logcatissa, vaan ainoastaan siina etta mitaan
     * ei tapahtunut.
     */
    @Test
    fun `muurin linkki on laudan oma linkki`() {
        val lauta = lauta(
            bar = listOf(
                BarCheckers(CheckerColor.YELLOW, count = 1, href = "/bg/move/7/1?move=y"),
                // Vastustajan nappula muurilla ilman linkkia: ei saa tuoda mitaan joukkoon.
                BarCheckers(CheckerColor.BLUE, count = 2),
            ),
        )

        assertEquals(setOf("/bg/move/7/1?move=y"), lauta.ownLinks())
    }

    @Test
    fun `sivun piilokentat kulkevat mukana`() {
        // Mitattu 22.8.2026: ilman `commit=1`:tä sivusto vastaa 200 ja palauttaa saman
        // sivun, eli teko epäonnistuu onnistumisen näköisenä. Väite on siksi kentistä eikä
        // vastauksesta: pyyntö kantaa sen mitä lomakkeessa on.
        val form = lomake(
            action = "/bg/move/7000011/556",
            submits = listOf("Next Game", "To Top"),
            hiddenFields = mapOf("commit" to "1"),
        )

        assertEquals(
            mapOf("commit" to "1", "submit" to "Next Game"),
            form.press("Next Game")?.fields,
        )
    }

    @Test
    fun `piilokentta ei syrjayta sivun omaa kokoamistilaa`() {
        // `move` on itsekin piilokenttä, joten se tulee kahta reittiä. Testi lukitsee sen
        // ettei järjestys ratkaise arvoa: kummankin lähteen arvo on sivun oma.
        val form = lomake(
            submits = listOf("Submit Move"),
            pendingMove = "rrmm",
            hiddenFields = mapOf("commit" to "1", "move" to "rrmm"),
        )

        assertEquals(
            mapOf("commit" to "1", "move" to "rrmm", "submit" to "Submit Move"),
            form.press("Submit Move")?.fields,
        )
    }

    private fun lomake(
        action: String = "/bg/move/7000011/117",
        submits: List<String>,
        pendingMove: String? = null,
        verify: String? = null,
        hiddenFields: Map<String, String> = emptyMap(),
    ) = BoardForm(
        action = action,
        // Lautasivun lomake on GET, ja se on tässä kirjoitettuna auki koska tyyppi kantaa
        // nyt myös POSTin (pikaviestin vastaus).
        method = FormMethod.GET,
        pendingMove = pendingMove,
        submits = submits,
        verify = verify,
        hiddenFields = hiddenFields,
    )

    private fun lauta(
        moves: List<MoveLink> = emptyList(),
        commands: List<CommandLink> = emptyList(),
        undoHref: String? = null,
        form: BoardForm? = null,
        bar: List<BarCheckers> = emptyList(),
    ) = BoardState(
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
        moves = moves,
        commands = commands,
        form = form,
        undoHref = undoHref,
        borneOff = emptyList(),
        bar = bar,
    )
}
