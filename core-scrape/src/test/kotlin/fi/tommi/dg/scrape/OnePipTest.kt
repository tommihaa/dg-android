package fi.tommi.dg.scrape

import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.resolveSeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Vastustajalla yksi pippi jäljellä, mitattu lauta 27.9.2026 (`raakasivut/sessio-27-9-ilta2`,
 * sivu 0011). Sivu kirjoittaa yksikön taivutettuna, `1 pip`, ja jäsennin luki vain muodon
 * `pips`. Paneelissa oli silloin yksi pip-luku, istuin jäi ratkeamatta ja kokoaminen putosi
 * sivuston reittiin: vuoro maksoi neljä ylimääräistä pyyntöä (`?move=j`, peruminen, `j`, `jh`).
 */
class OnePipTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).reader(Charsets.ISO_8859_1).readText()

    private fun board() =
        checkNotNull(BoardParser.parse(fixture("move_opponent_one_pip.html"))) { "Lautaa ei jäsennetty" }

    @Test
    fun `yksikkomuoto 1 pip luetaan pip-luvuksi`() {
        assertEquals(listOf(1, 81), board().players.map { it.pips })
    }

    @Test
    fun `yhden pipin lauta kokoaa paikallisesti`() {
        assertNotNull(board().resolveSeat()) { "Istuin jäi ratkeamatta" }
        assertNotNull(LocalComposition.begin(board()))
    }
}
