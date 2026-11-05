package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Palkin näkyvyysehto: jokainen välilehtireitti osuu välilehteen, ja lauta ei ole
 * välilehti. Jälkimmäinen on se väite jonka varassa palkki ja vaakatila pysyvät samassa
 * portissa (`MainActivity`).
 */
class DgTabsTest {

    @Test
    fun `jokainen valilehtireitti osuu valilehteen`() {
        assertEquals(DgTab.Matches, tabFor(TOP_ROUTE))
        assertEquals(DgTab.Lounge, tabFor(LOUNGE_ROUTE))
        assertEquals(DgTab.Discussion, tabFor(DISCUSSION_ROUTE))
        assertEquals(DgTab.Messages, tabFor(MESSAGES_ROUTE))
        assertEquals(DgTab.Info, tabFor(INFO_ROUTE))
    }

    /**
     * Välilehtien luku on itse päätös eikä sivutuote (`docs/UI.md` 26.8.2026): viisi mahtuu
     * puhelimen leveyteen kerralla, ja kuudes tekisi palkista listan. Väite on tässä siksi,
     * että välilehden lisääminen on juuri se muutos joka menee läpi huomaamatta.
     */
    @Test
    fun `valilehtia on viisi`() {
        assertEquals(5, DgTab.entries.size)
    }

    /** Pyyhkäisyn järjestys ja kiertävät päädyt (Tommin päätös 20.10.2026). */
    @Test
    fun `pyyhkaisy kulkee palkin jarjestyksessa ja kiertaa paadyissa`() {
        assertEquals(DgTab.Lounge, swipeTarget(DgTab.Matches, 1))
        assertEquals(DgTab.Messages, swipeTarget(DgTab.Discussion, 1))
        assertEquals(DgTab.Discussion, swipeTarget(DgTab.Messages, -1))
        assertEquals(DgTab.Matches, swipeTarget(DgTab.Info, 1))
        assertEquals(DgTab.Info, swipeTarget(DgTab.Matches, -1))
    }

    @Test
    fun `lauta ei ole valilehti`() {
        assertNull(tabFor(BoardRoute.PATTERN))
    }

    @Test
    fun `tuntematon reitti ei osu mihinkaan`() {
        assertNull(tabFor(null))
        assertNull(tabFor("jotain"))
    }
}
