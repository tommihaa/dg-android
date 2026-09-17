package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.CompositionSession
import fi.tommi.dg.domain.Die
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.MoveLink
import fi.tommi.dg.domain.Point
import fi.tommi.dg.domain.PlayerPanel
import fi.tommi.dg.domain.PlayerRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Noppien painalluksen sääntö: mitä painallus tekee missäkin kokoamisen tilassa.
 *
 * Testi ajaa oikean [CompositionSession]in eikä jäljittele sen tiloja, koska juuri niiden
 * kaksi rajaa ovat se mitä Tommi kuittasi 4.9.2026. Vaihto on mahdollinen vain ennen
 * ensimmäistä poimintaa, ja lähetys tulee mahdolliseksi vasta kun vuoro on täysi.
 *
 * Asema on sama kuin `LocalCompositionTest`in ensimmäisessä testissä (`core-domain`), jotta
 * kirjaimet ja askelten kertymä ovat jo todennettuja eivätkä tämän testin väitteitä.
 */
class DiceTapTest {

    /**
     * Kaksi omaa pistettä, yksi vastustajan ja nopat 6 ja 3. Linkit ovat sivun omia, ja
     * niiden on vastattava generaattorin ensimmäisiä askelia: muuten `begin` kieltäytyy
     * eikä testi mittaisi sääntöä vaan aseman rakentamista.
     */
    private fun lauta(): BoardState {
        val omat = mapOf(13 to 2, 8 to 2)
        val vastustaja = mapOf(1 to 2)
        val points = (1..24).map { number ->
            val own = omat[number] ?: 0
            val opp = vastustaja[number] ?: 0
            Point(
                number = number,
                owner = if (own > 0) CheckerColor.YELLOW else if (opp > 0) CheckerColor.BLUE else null,
                count = own + opp,
            )
        }
        val yellowPips = points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count * it.number }
        val bluePips = points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count * (25 - it.number) }
        return BoardState(
            matchId = MatchId("7000011"),
            moveNumber = null,
            stateToken = "123",
            eventName = null,
            eventId = null,
            round = null,
            matchLength = 7,
            cube = null,
            dice = listOf(Die(6, CheckerColor.YELLOW), Die(3, CheckerColor.YELLOW)),
            scheme = BoardScheme.BLUE_WHITE,
            points = points,
            players = listOf(
                PlayerPanel(PlayerRef(name = "vastapelaaja"), bluePips, 0, "0", null),
                PlayerPanel(PlayerRef(name = "pelaaja"), yellowPips, 0, "0", null),
            ),
            prompt = null,
            notices = emptyList(),
            rolledBack = false,
            speculative = false,
            moves = listOf(
                MoveLink(13, "m", "/bg/move/7000011/123?move=m"),
                MoveLink(8, "h", "/bg/move/7000011/123?move=h"),
            ),
            commands = emptyList(),
            form = null,
            undoHref = null,
            borneOff = emptyList(),
            bar = emptyList(),
        )
    }

    private fun alku(): CompositionSession = checkNotNull(LocalComposition.begin(lauta()))

    @Test
    fun `koskemattomilla nopilla painallus vaihtaa jarjestyksen`() {
        assertEquals(DiceTap.SWAP, diceTapFor(alku(), submitEnabled = true, swapEnabled = true))
    }

    @Test
    fun `taydella vuorolla painallus lahettaa`() {
        val taysi = checkNotNull(checkNotNull(alku().step(13)).step(8))
        assertEquals(DiceTap.SUBMIT, diceTapFor(taysi, submitEnabled = true, swapEnabled = true))
    }

    /**
     * Kesken kokoamisen kumpikaan ehto ei täyty: vaihto on jo mennyt ohi ja vuoro ei ole
     * vielä täysi. Tämä on se tila jossa painallus **ei saa tehdä mitään**, koska
     * kummankaan teon merkitys ei ole silloin luettavissa noppien väristä.
     */
    @Test
    fun `kesken kokoamisen painallus ei tee mitaan`() {
        val kesken = checkNotNull(alku().step(13))
        assertNull(diceTapFor(kesken, submitEnabled = true, swapEnabled = true))
    }

    @Test
    fun `kytkin pois sulkee oman tekonsa eika toista`() {
        val taysi = checkNotNull(checkNotNull(alku().step(13)).step(8))
        assertNull(diceTapFor(alku(), submitEnabled = true, swapEnabled = false))
        assertNull(diceTapFor(taysi, submitEnabled = false, swapEnabled = true))
        assertEquals(DiceTap.SWAP, diceTapFor(alku(), submitEnabled = false, swapEnabled = true))
        assertEquals(DiceTap.SUBMIT, diceTapFor(taysi, submitEnabled = true, swapEnabled = false))
    }

    /**
     * Ilman kokoamista painallus ei tee mitään, vaikka molemmat kytkimet olisivat päällä.
     * Tämä on Tommin kuittaus 4.9.2026: ehto luetaan kokoamisesta, ja sivun omalla laudalla
     * harmautta ei ole olemassa.
     */
    @Test
    fun `ilman kokoamista painallus ei tee mitaan`() {
        assertNull(diceTapFor(null, submitEnabled = true, swapEnabled = true))
    }
}
