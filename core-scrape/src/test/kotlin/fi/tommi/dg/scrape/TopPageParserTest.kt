package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TopPageParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val page = TopPageParser.parse(fixture("top_page.html"))!!

    @Test
    fun `kirjautunut kayttaja tunnistetaan`() {
        assertEquals("pelaaja", page.user.name)
        assertEquals("90001", page.user.userId)
    }

    @Test
    fun `oma kayttajanumero ei sekoitu vastustajien numeroihin`() {
        // Oma numero on sivulla samassa muodossa /bg/user/<id> kuin vastustajien, ja se
        // erottuu vain kyselyparametrista. Ilman erottelua ensimmäinen vastustaja
        // luultaisiin käyttäjäksi itsekseen.
        assertFalse(page.matches.any { it.opponent.userId == page.user.userId })
    }

    @Test
    fun `profiilipolku luetaan linkista sellaisenaan`() {
        // Polkua ei koota käyttäjänumerosta: kyselyparametrit ovat palvelimen omat, ja
        // profiili on ainoa reitti niihin otteluihin joissa ei ole vuoroa.
        assertEquals("/bg/user/90001?days_to_view=30&active=1&finished=1", page.user.profilePath)
    }

    @Test
    fun `kaikki ottelurivit loytyvat ja otsikkorivi ei tule mukaan`() {
        assertEquals(5, page.matches.size)
    }

    @Test
    fun `navigaatiotaulukot eivat tuota otteluita`() {
        // Sivulla on caption-vapaita taulukoita navigaatiolle. Jos ne pääsisivät mukaan,
        // ottelumäärä olisi suurempi kuin viisi.
        assertTrue(page.matches.all { it.playPath != null })
    }

    @Test
    fun `ensimmainen ottelu jasentyy kokonaan`() {
        val match = page.matches.first()
        assertEquals("7000001", match.id.value)
        assertEquals("Nine Lives #1111", match.eventName)
        assertEquals("900001", match.eventId)
        assertEquals("91001", match.opponent.userId)
        assertEquals("vastapelaaja", match.opponent.name)
        assertEquals("0:00", match.graceText)
        assertEquals("278:29", match.timePoolText)
        assertEquals("5/6", match.round)
        assertEquals(9, match.matchLength)
        assertEquals("/bg/game/7000001/0/list", match.reviewPath)
        assertEquals("/bg/move/7000001/541", match.playPath)
    }

    @Test
    fun `vuoro luetaan taulukon captionista`() {
        assertTrue(page.matches.all { it.myTurn == true })
    }

    @Test
    fun `hankalat kayttajanimet sailyvat sellaisenaan`() {
        val names = page.matches.map { it.opponent.name }
        assertTrue(names.contains("Nimi Valilyonnilla"), "Välilyönti katkaisi nimen: $names")
        assertTrue(names.contains("Nimi.Pisteella"), "Piste katkaisi nimen: $names")
        assertTrue(names.contains("nimi-viivalla"), "Väliviiva katkaisi nimen: $names")
    }

    @Test
    fun `ilmoituslippu luetaan mutta se ei ole henkiloviestin merkki`() {
        // Sama linkki näkyy myös turnausuutisista, joten lippu kertoo vain että
        // /bg/nextgame:ssa on jotain.
        assertTrue(page.hasMessageNotice)
    }

    @Test
    fun `jonon polku luetaan linkista eika koota`() {
        // Kuluttava osoite, joten koottu polku ei tuottaisi virhettä vaan peruuttamattoman
        // teon. Väite koskee sitä että arvo tulee sivulta: se on sivun oma href.
        val href = page.messageQueuePath
        assertTrue(href != null && href.contains("/bg/nextgame")) { "Jonon polku: $href" }
    }

    @Test
    fun `loungen ja palstan polut luetaan navigointipalkista sellaisenaan`() {
        assertEquals("/bg/lounge", page.loungePath)
        assertEquals("/bg/forum2", page.forumPath)
        assertEquals("/bg/userevent/90001", page.activeTournamentsPath)
    }

    @Test
    fun `uloskirjautuneella sivulla navigointipolkuja ei ole`() {
        // Login-sivun palkissa ei ole loungen eikä palstan linkkiä. Ennen 1.9.2026 sivu
        // jäsentyi tyhjäksi Top Pageksi jolla kentät olivat null; nyt jäsennin kieltäytyy
        // koko sivusta (H3), mikä on sama vastaus vahvempana.
        assertNull(TopPageParser.parse(fixture("top_page_logged_out.html")))
    }

    @Test
    fun `pelitunnisteeton rivi ohitetaan eika kaada koko listaa`() {
        // Synteettinen tapaus, ei kaapatusta datasta: tuntematon rivi ei saa viedä
        // mukanaan niitä rivejä jotka olisi voitu lukea.
        val html = """
            <HTML><BODY>
            <A HREF="/bg/user/1?days_to_view=30&active=1&finished=1">oma</A>
            <TABLE><CAPTION>Matches where you can move:</CAPTION>
            <TR><TD>1. </TD><TD>Rikkinäinen rivi ilman linkkejä</TD></TR>
            <TR>
            <TD>2. </TD><TD><A HREF=/bg/event/1>Tapahtuma</A></TD>
            <TD>0:00</TD><TD>100:00</TD><TD>1/3</TD><TD>7</TD>
            <TD><A href=/bg/user/2>joku</A></TD>
            <TD><A href=/bg/move/3/4>Play</A></TD>
            </TR>
            </TABLE></BODY></HTML>
        """.trimIndent()

        val parsed = TopPageParser.parse(html)!!

        assertEquals(1, parsed.matches.size)
        assertEquals("3", parsed.matches.single().id.value)
    }

    @Test
    fun `liigarivin nimi luetaan tekstisolusta kun tapahtumalinkkia ei ole`() {
        // Liigaottelun rivi on Top Pagella pelkkaa lihavoitua tekstia ilman
        // `/bg/event/`-linkkia, ja kierros on viiva (kaapattu 14.9.2026, `sessio-14-9-ilta`).
        // Lautasivulla sama puute jatti nimen pois kortista 14.9.2026, ja tama testi
        // sanoo etta luettelossa nimi ei jaa pois.
        val html = """
            <HTML><BODY>
            <A HREF="/bg/user/1?days_to_view=30&active=1&finished=1">oma</A>
            <TABLE><CAPTION>Matches where you can move:</CAPTION>
            <TR>
            <TD>1. </TD>
            <TD><B>DG 8x8 2026 - H vs C - Div 1</B></TD>
            <TD align=center>11:39</TD>
            <TD align=center>175:12</TD><TD ALIGN=center>-</TD>
            <TD align=center>15</TD>
            <TD ALIGN=CENTER><A href=/bg/user/24994>bazari</A></TD>
            <TD><A href=/bg/game/5313587/1/list>Review</A></TD>
            <TD><A href=/bg/move/5313587/1612>Play</A></TD>
            </TR>
            </TABLE></BODY></HTML>
        """.trimIndent()

        val match = TopPageParser.parse(html)!!.matches.single()

        assertEquals("DG 8x8 2026 - H vs C - Div 1", match.eventName)
        assertNull(match.eventId)
        assertEquals("bazari", match.opponent.name)
        assertEquals("11:39", match.graceText)
        assertEquals("175:12", match.timePoolText)
        assertEquals("-", match.round)
        assertEquals(15, match.matchLength)
    }

    @Test
    fun `cannot move -caption ei tulkita vuoroksi`() {
        // Synteettinen varotoimi: merkkijono "can move" sisältyy sanaan "cannot move",
        // joten pelkkä contains-tarkistus antaisi väärän positiivisen. Tätä muotoilua ei
        // ole nähty datassa, joten testi suojaa oletusta eikä havaintoa.
        val html = """
            <HTML><BODY>
            <A HREF="/bg/user/1?days_to_view=30&active=1&finished=1">oma</A>
            <TABLE><CAPTION>Matches where you cannot move:</CAPTION>
            <TR>
            <TD>1. </TD><TD><A HREF=/bg/event/1>Tapahtuma</A></TD>
            <TD>0:00</TD><TD>100:00</TD><TD>1/3</TD><TD>7</TD>
            <TD><A href=/bg/user/2>joku</A></TD>
            <TD><A href=/bg/move/3/4>Play</A></TD>
            </TR>
            </TABLE></BODY></HTML>
        """.trimIndent()

        val parsed = TopPageParser.parse(html)!!

        assertEquals(1, parsed.matches.size)
        assertEquals(false, parsed.matches.single().myTurn)
    }

    @Test
    fun `ilman ilmoituslinkkia lippu on epatosi`() {
        // Profiililinkki on mukana koska se on se mikä tekee sivusta Top Pagen
        // (`DgPages.isTopPage`); ilman sitä jäsennin kieltäytyy koko sivusta.
        val html = """
            <HTML><BODY>
            <h2>Welcome to DailyGammon, joku.</h2>
            <A HREF="/bg/user/90001?days_to_view=30&active=1&finished=1">oma</A>
            </BODY></HTML>
        """.trimIndent()
        val parsed = TopPageParser.parse(html)!!
        assertFalse(parsed.hasMessageNotice)
        assertEquals("90001", parsed.user.userId)
        assertEquals("joku", parsed.user.name)
    }

    // --- Sivu jolla ei ole yhtään siirrettävää ottelua (kaapattu 3.8.2026) ---

    private val noMatches = TopPageParser.parse(fixture("top_page_no_matches.html"))!!

    @Test
    fun `tyhja tulos on kelvollinen eika jasennysvirhe`() {
        // Sivulla ei ole ottelutaulukkoa lainkaan, vain lause siitä ettei siirrettäviä ole.
        // Tyhjä lista on siis oikea vastaus, ja käyttöliittymän on erotettava tämä
        // epäonnistuneesta hausta.
        assertTrue(noMatches.matches.isEmpty())
    }

    @Test
    fun `tyhjalta sivulta luetaan yha kayttaja ja oma numero`() {
        // Tämä on syy siihen ettei tyhjää sivua voi ohittaa kokonaan: istunnon tila ja
        // oma käyttäjänumero ovat yhä luettavissa, ja profiililinkki on ainoa reitti
        // odottaviin otteluihin.
        assertEquals("pelaaja", noMatches.user.name)
        assertEquals("90001", noMatches.user.userId)
    }

    @Test
    fun `tyhjalta sivulta luetaan yha profiilipolku`() {
        // Juuri tällä sivulla se on ainoa jäljellä oleva reitti otteluihin.
        assertEquals("/bg/user/90001?days_to_view=30&active=1&finished=1", noMatches.user.profilePath)
    }

    @Test
    fun `tyhjalla sivulla ei ole ilmoituslippua`() {
        assertFalse(noMatches.hasMessageNotice)
    }

    @Test
    fun `tyhjan sivun navigaatiotaulukot eivat tuota otteluita`() {
        // Sivulla on kaksi caption-vapaata navigaatiotaulukkoa ja ei yhtään muuta.
        // Jos captionin vaatimus katoaisi, juuri tämä sivu tuottaisi roskarivejä.
        assertTrue(noMatches.matches.isEmpty())
    }
}
