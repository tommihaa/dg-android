package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfileParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val page = ProfileParser.parse(fixture("profile_page.html"))!!

    @Test
    fun `profiilin kohde luetaan otsikosta ja omista linkeista`() {
        assertEquals("pelaaja", page.player.name)
        assertEquals("90001", page.player.userId)
    }

    @Test
    fun `profiilin numero ei sekoitu vastustajien numeroihin`() {
        // Sivulla on vastustajien linkkejä samassa muodossa /bg/user/<id> kuin profiilin
        // omat lajittelulinkit. Numero luetaan siksi /bg/userevent/- ja /bg/userwins/
        // -linkeistä, joita vain profiilin kohteella on.
        val opponents = (page.activeMatches + page.finishedMatches).mapNotNull { it.opponent.userId }
        assertFalse(opponents.contains(page.player.userId))
    }

    @Test
    fun `molemmat taulukot loytyvat ja erottuvat toisistaan`() {
        assertEquals(33, page.activeMatches.size)
        assertEquals(26, page.finishedMatches.size)
    }

    @Test
    fun `kesken oleva ottelu jasentyy kokonaan`() {
        val match = page.activeMatches.first()
        assertEquals("7000001", match.id.value)
        assertEquals("May 26 Deja Vu", match.eventName)
        assertEquals("900001", match.eventId)
        assertEquals("91001", match.opponent.userId)
        assertEquals("vastapelaaja1", match.opponent.name)
        assertEquals("n/a", match.graceText)
        assertEquals("275:21", match.timePoolText)
        assertEquals("2/8", match.round)
        assertEquals(11, match.matchLength)
        assertEquals("/bg/game/7000001/0/list", match.reviewPath)
    }

    @Test
    fun `paattyneen ottelun sarakkeet luetaan otsikoista eika paikoista`() {
        // Tämä on koko jäsentimen olemassaolon syy. Finished-taulukosta puuttuvat Grace ja
        // Time Pool, joten Top Pagen tapa laskea sarake tapahtumasta eteenpäin lukisi
        // kierroksen graceksi ja pituuden time pooliksi.
        val match = page.finishedMatches.first()
        assertEquals("2/4", match.round)
        assertEquals(15, match.matchLength)
        assertNull(match.graceText)
        assertNull(match.timePoolText)
    }

    @Test
    fun `vientilinkki on vain paattyneilla otteluilla`() {
        // Linkin olemassaolo on samalla merkki siitä että ottelu on päättynyt.
        assertTrue(page.finishedMatches.all { it.exportPath != null })
        assertTrue(page.activeMatches.all { it.exportPath == null })

        val match = page.finishedMatches.first()
        assertEquals("/bg/export/${match.id.value}", match.exportPath)
    }

    @Test
    fun `vuoro jaa tuntemattomaksi koska sivu ei kerro sita`() {
        // false olisi arvaus, ja se olisi väärä juuri niissä otteluissa joita käyttäjä
        // eniten etsii. Sivulla ei ole /bg/move/-linkkejä lainkaan.
        assertTrue((page.activeMatches + page.finishedMatches).all { it.myTurn == null })
        assertTrue(page.activeMatches.all { it.playPath == null })
    }

    @Test
    fun `linkiton tapahtuma luetaan solusta`() {
        // Kaapatussa sivussa on yksi ottelu jonka tapahtumasarakkeessa on pelkkä teksti
        // ilman /bg/event/-linkkiä. Nimetty sarake antaa senkin oikein.
        val friendly = page.activeMatches.single { it.eventId == null }
        assertEquals("double-repeat", friendly.eventName)
        assertNotNull(friendly.opponent.userId)
    }

    @Test
    fun `jokaisella rivilla on pelitunniste ja vastustaja`() {
        val all = page.activeMatches + page.finishedMatches
        assertTrue(all.all { it.id.value.isNotBlank() })
        assertTrue(all.all { it.opponent.displayName.isNotEmpty() })
        // Otsikkorivit eivät saa päätyä listaan.
        assertFalse(all.any { it.opponent.name == "Opponent" })
    }

    @Test
    fun `ilman otsikkorivia nimetyt sarakkeet jaavat tyhjiksi eika rivi katoa`() {
        // Puuttuva arvo on parempi kuin arvattu: ilman otsikoita sarakkeiden merkitystä ei
        // voi tietää, mutta linkit ovat yhä luettavissa.
        val html = """
            <HTML><HEAD><TITLE>DailyGammon: Info on player joku</TITLE></HEAD><BODY>
            <TABLE><CAPTION>Active games:</CAPTION>
            <TR>
            <TD>1. </TD><TD><A HREF=/bg/event/1>Tapahtuma</A></TD>
            <TD>n/a</TD><TD>100:00</TD><TD>1/3</TD><TD>7</TD>
            <TD><A href=/bg/user/2>joku muu</A></TD>
            <TD><A href=/bg/game/3/1/list>Review</A></TD>
            </TR>
            </TABLE></BODY></HTML>
        """.trimIndent()

        val parsed = ProfileParser.parse(html)!!
        val match = parsed.activeMatches.single()

        assertEquals("3", match.id.value)
        assertEquals("Tapahtuma", match.eventName)
        assertEquals("2", match.opponent.userId)
        assertNull(match.round)
        assertNull(match.matchLength)
    }

    @Test
    fun `tuntematonta taulukkoa ei lueta otteluiksi`() {
        // Sivulla voi olla muitakin captionillisia taulukoita. Vain nimetyt kaksi luetaan,
        // koska tuntemattoman taulukon sarakkeista ei tiedä mitään.
        val html = """
            <HTML><HEAD><TITLE>DailyGammon: Info on player joku</TITLE></HEAD><BODY>
            <TABLE><CAPTION>Something else:</CAPTION>
            <TR><TD><A href=/bg/game/3/1/list>Review</A></TD></TR>
            </TABLE></BODY></HTML>
        """.trimIndent()

        val parsed = ProfileParser.parse(html)!!

        assertTrue(parsed.activeMatches.isEmpty())
        assertTrue(parsed.finishedMatches.isEmpty())
        assertEquals("joku", parsed.player.name)
    }

    @Test
    fun `rating ja kokemus luetaan nimen suhteen`() {
        val page = ProfileParser.parse(fixture("profile_page.html"))!!
        assertEquals("1234.56", page.ratingText)
        assertEquals("1234", page.experienceText)
    }

    @Test
    fun `esittelyrivit luetaan rakenteesta eika nimilistasta`() {
        val page = ProfileParser.parse(fixture("profile_page.html"))!!
        assertEquals(
            listOf("Real Name", "Location", "E-mail", "Comment"),
            page.fields.map { it.label },
        )
        assertEquals("kommentti", page.fields.last().text)
    }

    @Test
    fun `toisen pelaajan profiililla on eri rivit ja ei yhtaan ottelua`() {
        // Mitattu 27.8.2026: osoitteessa ei ole active- eikä finished-parametria, joten
        // ottelutaulukoita ei ole lainkaan.
        //
        // Rivistö luetaan tästä fixturesta sellaisenaan, eikä siitä saa lukea väitettä
        // toisen pelaajan profiileista yleensä. Tässä luki 29.8.2026 asti että rivistössä
        // on "Home Page eikä E-mail", ja laiteajo kumosi sen: toisen pelaajan profiililla
        // näkyi myös E-mail. Kentät riippuvat siitä mitä pelaaja on täyttänyt, ja juuri
        // siksi `ProfileParser.fields` lukee ne rakenteesta eikä nimilistasta.
        val other = ProfileParser.parse(fixture("profile_page_other.html"))!!
        assertEquals("toinenpelaaja", other.player.name)
        assertEquals("2326.39", other.ratingText)
        assertEquals(
            listOf("Real Name", "Location", "Home Page", "Comment"),
            other.fields.map { it.label },
        )
        assertTrue(other.activeMatches.isEmpty())
        assertTrue(other.finishedMatches.isEmpty())
    }

    @Test
    fun `toisen pelaajan pelilista lajittelulinkin takaa on sama muoto`() {
        // Mitattu 27.8.2026 (`user_profile_other_games.html`): profiilin oma
        // lajittelulinkki `?sort_name=1&active=1&finished=1` palauttaa saman sivun
        // ottelutaulukoineen, samoilla captioneilla ja otsikkoriveillä kuin oma profiili.
        // Sama osoitemuoto `versus`-parametrilla on `matches versus you` -linkillä, joten
        // tämä fixture on mittaus myös sen puolesta että tulossivulle kelpaa tämä
        // jäsennin. Alkuperäisessä kaappauksessa oli 12 + 1303 riviä; fixtureen jätettiin
        // 3 + 3, joista päättyneiden ensimmäinen on ystävyysottelu ilman tapahtumalinkkiä.
        val games = ProfileParser.parse(fixture("profile_page_games.html"))!!

        assertEquals("katseltava", games.player.name)
        assertEquals("92001", games.player.userId)
        assertEquals(3, games.activeMatches.size)
        assertEquals(3, games.finishedMatches.size)

        val active = games.activeMatches.first()
        assertEquals("20:38", active.graceText)
        assertEquals("279:40", active.timePoolText)
        assertEquals("vastustaja1", active.opponent.name)

        val friendly = games.finishedMatches.first()
        assertEquals("backgammon", friendly.eventName)
        assertNull(friendly.eventId)
        assertEquals("-", friendly.round)
        assertEquals("/bg/export/${friendly.id.value}", friendly.exportPath)
    }

    @Test
    fun `turnauslinkit luetaan sivulta eika koota numerosta`() {
        val page = ProfileParser.parse(fixture("profile_page.html"))!!
        assertEquals("/bg/userevent/90001", page.activeTournamentsPath)
        assertEquals("/bg/userwins/90001", page.tournamentWinsPath)
    }

    @Test
    fun `pelilistan ja versus-suodattimen linkit luetaan sivulta`() {
        // Lajittelulinkki on molemmilla profiileilla, versus vain toisen pelaajan.
        // Kumpaakaan ei koota numerosta vaan ne ovat sivun omia linkkejä sellaisinaan.
        val own = ProfileParser.parse(fixture("profile_page.html"))!!
        assertNotNull(own.gamesPath)
        assertTrue(own.gamesPath!!.contains("sort_name"))
        assertNull(own.versusPath)

        // Lajitellulta sivulta puuttuu nykyisen lajittelun oma linkki, joten gamesPath on
        // siellä jokin toinen lajittelu; versus-linkki ei saa kelvata siihen vaikka
        // siinäkin on sort-parametri.
        val other = ProfileParser.parse(fixture("profile_page_games.html"))!!
        assertNotNull(other.gamesPath)
        assertTrue(!other.gamesPath!!.contains("versus="))
        assertNotNull(other.versusPath)
        assertTrue(other.versusPath!!.contains("versus="))
    }
    @Test
    fun `viestilomake luetaan toisen pelaajan profiililta`() {
        // Keskustelun aloitus. Kentät luetaan sivulta eikä koota: osoite on käyttäjälle
        // eikä viestille, ja väärä osoite ei tuottaisi virhettä vaan väärän teon.
        val other = ProfileParser.parse(fixture("profile_page_other.html"))!!
        val form = checkNotNull(other.messageForm)

        assertEquals("/bg/sendmsg/21666", form.action)
        assertEquals(FormMethod.POST, form.method)
        assertEquals("text", form.field)
        assertEquals(80, form.maxLength)
    }

    @Test
    fun `viestilomake ei sekoitu kutsulomakkeeseen joka on sivulla ensin`() {
        // Sivulla on kolme lomaketta ja viestilomake on niistä toinen (mitattu
        // `user_profile_other.html`). Ilman osoitteeseen nojaavaa valintaa tekstikenttä
        // luettaisiin kutsulomakkeesta ja viesti lähtisi ottelukutsuna.
        val other = ProfileParser.parse(fixture("profile_page_other.html"))!!

        assertTrue(other.messageForm!!.action.contains("/bg/sendmsg/"))
        assertFalse(other.messageForm!!.action.contains("invite"))
        // Kutsulomakkeen oma tekstikenttä on `comment`, eli juuri se jonka väärä valinta
        // olisi tuottanut.
        assertFalse(other.messageForm!!.field == "comment")
    }

    @Test
    fun `kutsulomake luetaan kokonaan sivulta`() {
        // Rajaus purettu 3.9.2026. Kolme valintaa vaihtoehtoineen, kaksi tekstikenttää
        // rajoineen ja valintaruutu, kaikki sivun omilla nimillä ja arvoilla.
        val form = checkNotNull(ProfileParser.parse(fixture("profile_page_other.html"))!!.inviteForm)

        assertEquals("/bg/invite/new", form.action)
        assertEquals(FormMethod.POST, form.method)
        assertEquals(mapOf("player" to "21666"), form.hidden)
        assertEquals(listOf("backgammon", "nack", "double-repeat"), form.variant.options.map { it.label })
        assertEquals("1", form.variant.default?.value)
        assertEquals("5", form.length.default?.value)
        assertEquals(15, form.length.options.size)
        assertEquals("-2", form.length.options.last().value)
        assertEquals("time_control", form.timeControl.name)
        assertEquals("Never", form.timeControl.default?.label)
        assertEquals("Weekday (250/+0/72)", form.timeControl.label("11"))
        assertEquals(80, form.comment?.maxLength)
        assertEquals(40, form.name?.maxLength)
        assertEquals("private", form.privateMatch?.value)
    }

    @Test
    fun `sivuutuslomake luetaan piilokenttineen ja napin tekstineen`() {
        val form = checkNotNull(ProfileParser.parse(fixture("profile_page_other.html"))!!.ignoreForm)

        assertEquals("/bg/ignore", form.action)
        // GET eikä POST: muuttava teko linkin muodossa (docs/KOHDE.md).
        assertEquals(FormMethod.GET, form.method)
        assertEquals(mapOf("changeto" to "1", "user" to "21666"), form.fields)
        assertEquals("Ignore toinenpelaaja", form.label)
    }

    @Test
    fun `omalla profiililla ei ole kutsu- eika sivuutuslomaketta`() {
        val own = ProfileParser.parse(fixture("profile_page.html"))!!
        assertNull(own.inviteForm)
        assertNull(own.ignoreForm)
    }

    @Test
    fun `omalla profiililla ei ole viestilomaketta`() {
        // Sivuston oma ero eikä sovelluksen sääntö: itselleen ei voi lähettää. Mitattu
        // raakasivuista, joissa `user_profile.html` ei sisällä osajonoa `sendmsg` lainkaan.
        assertNull(ProfileParser.parse(fixture("profile_page.html"))!!.messageForm)
    }

    @Test
    fun `lajitellulla pelilistasivulla viestilomake on yha tallella`() {
        // Sama sivu eri parametreilla. Jos lomake katoaisi porautumisen myötä, ruudun
        // kyky riippuisi siitä mitä kautta profiilille tultiin.
        assertNotNull(ProfileParser.parse(fixture("profile_page_games.html"))!!.messageForm)
    }
}
