package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DgPagesTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    @Test
    fun `login-sivu tunnistetaan`() {
        assertTrue(DgPages.isLoginPage(fixture("login_page.html")))
    }

    @Test
    fun `uloskirjautunut top page tunnistetaan login-sivuksi`() {
        // Tämä on se tapaus joka rikkoo naiivin asiakkaan: palvelin vastasi 200 OK,
        // mutta sisältö on kirjautumislomake eikä ottelulista.
        assertTrue(DgPages.isLoginPage(fixture("top_page_logged_out.html")))
    }

    @Test
    fun `paluupolku luetaan piilokentästä`() {
        assertEquals("top/", DgPages.loginReturnPath(fixture("top_page_logged_out.html")))
        assertEquals("login/", DgPages.loginReturnPath(fixture("login_page.html")))
    }

    @Test
    fun `otteluluettelo tunnistetaan myos ilman otteluita`() {
        assertTrue(DgPages.isTopPage(fixture("top_page.html")))
        // Tämä on koko vahdin syy: sivu jolla ei ole yhtään siirrettävää ottelua on
        // kelvollinen otteluluettelo, ja se pitää erottaa vastauksesta joka ei ole
        // otteluluettelo lainkaan. Taulukoihin nojaava tunnistus kaatuisi tässä.
        assertTrue(DgPages.isTopPage(fixture("top_page_no_matches.html")))
    }

    @Test
    fun `muita sivuja ei luulla otteluluetteloksi`() {
        // Uloskirjautunut top page on tässä tärkein: se tulee 200 OK:lla samasta
        // osoitteesta, ja ilman tätä se jäsentyisi tyhjäksi listaksi.
        assertFalse(DgPages.isTopPage(fixture("top_page_logged_out.html")))
        assertFalse(DgPages.isTopPage(fixture("login_page.html")))
        assertFalse(DgPages.isTopPage(fixture("profile_page.html")))
        assertFalse(DgPages.isTopPage(fixture("settings_page.html")))
        assertFalse(DgPages.isTopPage(fixture("forum_thread.html")))
        assertFalse(DgPages.isTopPage(fixture("move_board.html")))
        assertFalse(DgPages.isTopPage(fixture("chat_thread.html")))
        assertFalse(DgPages.isTopPage(fixture("inbox_telegram.html")))
    }

    @Test
    fun `profiilisivu tunnistetaan sisallosta`() {
        assertTrue(DgPages.isProfilePage(fixture("profile_page.html")))
    }

    @Test
    fun `muita sivuja ei luulla profiiliksi`() {
        // Sivun laji ei ole pääteltävissä pyydetystä osoitteesta, joten tunnistuksen on
        // erotettava nämä toisistaan sisällön perusteella.
        assertFalse(DgPages.isProfilePage(fixture("top_page.html")))
        assertFalse(DgPages.isProfilePage(fixture("top_page_no_matches.html")))
        assertFalse(DgPages.isProfilePage(fixture("login_page.html")))
    }

    @Test
    fun `kaikki lautafixturet tunnistetaan laudoiksi`() {
        // Lista on nimetty eikä globattu: uusi lautafixture pakottaa silloin päätöksen
        // sen sijaan että liukuisi mukaan huomaamatta.
        listOf(
            "move_board.html",
            "move_board_arrows.html",
            "move_board_oikea.html",
            "move_board_vasen.html",
            "move_board_no_pips.html",
            "move_rollback.html",
            "move_assembling.html",
            "move_submit_ready.html",
            "move_greedy_bearoff.html",
            "move_cube_offered.html",
            "move_borne_off.html",
        ).forEach { name ->
            assertTrue(DgPages.isBoardPage(fixture(name)), "Pitäisi olla lauta: $name")
        }
    }

    @Test
    fun `Mini-skeema on lautasivu vaikka siita ei saa pisteita`() {
        // Erikseen omana testinään, koska se on rajanveto eikä rivi listassa. Mini pudottaa
        // pistenumerorivin, joten BoardParser antaa siitä laudan jonka points on tyhjä.
        // Predikaatti sanoo silti kyllä, ja se on tarkoitus: käyttäjä saa silloin
        // "asemaa ei voitu lukea" eikä väärää "tuo ei ollut lautasivu". Vain edellisen
        // suhteen hän voi toimia, koska korjaus on hänen oma asetuksensa.
        assertTrue(DgPages.isBoardPage(fixture("move_board_mini.html")))
    }

    @Test
    fun `chat-sivu on rakenteeltaan lauta ja se sanotaan aaneen`() {
        // Siirron jälkeisellä chat-sivulla on aito 24 pisteen lauta, joten predikaatti
        // sanoo kyllä. Lauta voi silti olla spekulatiivinen, eli sivuston arvaus tulevasta
        // asemasta, ja sen erottaa vain ChatParser. Väite on tässä siksi että predikaatin
        // rajat olisivat luettavissa eivätkä pääteltävissä.
        assertTrue(DgPages.isBoardPage(fixture("chat_thread.html")))
    }

    @Test
    fun `muita sivuja ei luulla laudaksi`() {
        // top_page_no_matches.html on tämän predikaatin olemassaolon syy: se saapui
        // 3.8.2026 mitatusti /bg/move/-osoitteesta ja 200 OK:lla, kun viimeinenkin siirto
        // oli tehty.
        listOf(
            "top_page_no_matches.html",
            "top_page.html",
            "top_page_logged_out.html",
            "login_page.html",
            "profile_page.html",
            "settings_page.html",
            "inbox_quick_message.html",
            "inbox_telegram.html",
            "forum_thread.html",
        ).forEach { name ->
            assertFalse(DgPages.isBoardPage(fixture(name)), "Ei saisi olla lauta: $name")
        }
    }

    @Test
    fun `lahetyksen kuittaus tunnistetaan`() {
        assertTrue(DgPages.isMessageSentPage(fixture("message_sent.html")))
    }

    @Test
    fun `muita sivuja ei luulla lahetyksen kuittaukseksi`() {
        // Pikaviesti on tassa tarkein: se on sen sivun laji joka on ruudulla juuri ennen
        // lahetysta, joten se on lahin vaara positiivinen. Login-sivu on toinen syy koko
        // predikaatille, koska katkennut istunto palauttaa sen 200 OK:lla.
        listOf(
            "inbox_quick_message.html",
            "inbox_telegram.html",
            "login_page.html",
            "top_page.html",
            "profile_page.html",
        ).forEach { name ->
            assertFalse(DgPages.isMessageSentPage(fixture(name)), "Ei saisi olla kuittaus: $name")
        }
    }

    @Test
    fun `lounge tunnistetaan tarjouslomakkeesta`() {
        assertTrue(DgPages.isLoungePage(fixture("lounge_page.html")))
    }

    @Test
    fun `muita sivuja ei luulla loungeksi`() {
        // Navigointipalkin /bg/lounge on linkki eikä lomake, joten se ei saa laueta.
        listOf(
            "top_page.html",
            "top_page_no_matches.html",
            "top_page_logged_out.html",
            "login_page.html",
            "profile_page.html",
            "settings_page.html",
            "forum_index.html",
            "forum_thread.html",
            "move_board.html",
            "inbox_quick_message.html",
        ).forEach { name ->
            assertFalse(DgPages.isLoungePage(fixture(name)), "Ei saisi olla lounge: $name")
        }
    }

    @Test
    fun `hyvaksynnan kuittaus tunnistetaan`() {
        assertTrue(DgPages.isJoinConfirmationPage(fixture("lounge_join_confirm.html")))
    }

    @Test
    fun `muita sivuja ei luulla hyvaksynnan kuittaukseksi`() {
        // Lounge on tassa tarkein: se on se sivu joka on ruudulla juuri ennen Joinia,
        // ja se oli 31.8.2026 asti ainoa jota luettiin onnistumiseksi.
        listOf(
            "lounge_page.html",
            "top_page.html",
            "login_page.html",
            "message_sent.html",
            "move_board.html",
        ).forEach { name ->
            assertFalse(DgPages.isJoinConfirmationPage(fixture(name)), "Ei saisi olla kuittaus: ${'$'}name")
        }
    }

    @Test
    fun `kuittaussivua ei luulla loungeksi`() {
        assertFalse(DgPages.isLoungePage(fixture("lounge_join_confirm.html")))
    }

    @Test
    fun `ottelun paattymissivu tunnistetaan`() {
        assertTrue(DgPages.isMatchOverPage(fixture("match_over.html")))
        assertTrue(DgPages.isMatchOverPage(fixture("match_over_chat.html")))
    }

    @Test
    fun `lautasivuja ei luulla paattymissivuksi`() {
        // Kesken olevan ottelun sivut ovat se lahin vaara positiivinen: niilla on samat
        // pelaajat ja pisteet, muttei tulosta.
        listOf(
            "move_board.html",
            "move_roll_double.html",
            "top_page.html",
            "match_log.html",
        ).forEach { name ->
            assertFalse(DgPages.isMatchOverPage(fixture(name)), "Ei saisi olla paattymissivu: ${'$'}name")
        }
    }

    @Test
    fun `palstan indeksi tunnistetaan ketjulinkeista`() {
        assertTrue(DgPages.isForumIndex(fixture("forum_index.html")))
    }

    @Test
    fun `politics-palsta tunnistetaan samalla valitsimella`() {
        // Palstan tunnus on osa polkua eikä vakio `main`, joten `main`-alkuinen valitsin
        // olisi hylännyt tämän sivun kokonaan (mitattu 27.8.2026).
        assertTrue(DgPages.isForumIndex(fixture("forum_index_politics.html")))
    }

    @Test
    fun `muita sivuja ei luulla palstan indeksiksi`() {
        // Lukusivu on tässä tärkein: sen footerissa on /bg/forum2/main-linkkejä mutta ei
        // yhtään read-linkkiä, ja ilman eroa indeksi ja ketju sekoittuisivat.
        listOf(
            "forum_thread.html",
            "forum_read.html",
            "top_page.html",
            "lounge_page.html",
            "login_page.html",
        ).forEach { name ->
            assertFalse(DgPages.isForumIndex(fixture(name)), "Ei saisi olla indeksi: $name")
        }
    }

    @Test
    fun `pelaajalista tunnistetaan otsikosta ja riveista`() {
        assertTrue(DgPages.isPlayerList(fixture("player_list.html")))
    }

    @Test
    fun `loungea ei luulla pelaajalistaksi`() {
        // Tämä on se ainoa oikeasti vaarallinen sekaannus: loungella on sekä
        // profiililinkkejä riveillä että oma /bg/plist-linkki, eli kumpikin
        // rakenteellinen tuntomerkki erikseen.
        listOf(
            "lounge_page.html",
            "top_page.html",
            "profile_page.html",
            "tournament_hall.html",
        ).forEach { name ->
            assertFalse(DgPages.isPlayerList(fixture(name)), "Ei saisi olla pelaajalista: $name")
        }
    }

    @Test
    fun `tournament hall tunnistetaan captioneista`() {
        assertTrue(DgPages.isTournamentHall(fixture("tournament_hall.html")))
    }

    @Test
    fun `loungen Sign-Up-taulukkoa ei luulla halliksi`() {
        listOf(
            "lounge_page.html",
            "top_page.html",
            "player_list.html",
        ).forEach { name ->
            assertFalse(DgPages.isTournamentHall(fixture(name)), "Ei saisi olla halli: $name")
        }
    }

    @Test
    fun `turnaussivu tunnistetaan saannoista`() {
        assertTrue(DgPages.isEventPage(fixture("event_page.html")))
    }

    @Test
    fun `turnauslistoja ei luulla turnaussivuksi`() {
        // Halli ja pelaajan omat listat puhuvat kaikki turnauksista, mutta vain
        // turnaussivulla on Conditions of Contest.
        listOf(
            "tournament_hall.html",
            "player_events.html",
            "player_wins.html",
            "lounge_page.html",
            "profile_page.html",
        ).forEach { name ->
            assertFalse(DgPages.isEventPage(fixture(name)), "Ei saisi olla turnaussivu: $name")
        }
    }

    @Test
    fun `pelaajan turnauslistat tunnistetaan otsikosta`() {
        assertTrue(DgPages.isPlayerTournaments(fixture("player_events.html")))
        assertTrue(DgPages.isPlayerTournaments(fixture("player_wins.html")))
    }

    @Test
    fun `muita sivuja ei luulla pelaajan turnauslistaksi`() {
        listOf(
            "tournament_hall.html",
            "event_page.html",
            "profile_page.html",
            "top_page.html",
        ).forEach { name ->
            assertFalse(
                DgPages.isPlayerTournaments(fixture(name)),
                "Ei saisi olla turnauslista: $name",
            )
        }
    }

    @Test
    fun `toisen pelaajan profiili tunnistetaan samoin kuin oma`() {
        // Sivulla ei ole ottelutaulukoita lainkaan, joten captioniin nojaava tunnistus
        // olisi hylännyt sen. Otsikko on sama molemmilla.
        assertTrue(DgPages.isProfilePage(fixture("profile_page_other.html")))
    }

    @Test
    fun `ketjun lukusivu tunnistetaan viestin rakenteesta`() {
        assertTrue(DgPages.isForumThread(fixture("forum_thread.html")))
        assertTrue(DgPages.isForumThread(fixture("forum_read.html")))
    }

    @Test
    fun `muita sivuja ei luulla ketjuksi`() {
        // Chat-sivut ovat lähin vaara: niissäkin on nimiä ja tekstiä, mutta ei
        // Posted by -allekirjoitusta. Lomakesivuilla on name-attribuutteja kentissä,
        // mutta predikaatti kysyy a[name]-ankkuria eikä kenttiä.
        listOf(
            "chat_thread.html",
            "chat_thread_says.html",
            "chat_thread_aakkoset.html",
            "forum_index.html",
            "settings_page.html",
            "inbox_quick_message.html",
            "top_page.html",
        ).forEach { name ->
            assertFalse(DgPages.isForumThread(fixture(name)), "Ei saisi olla ketju: $name")
        }
    }

    @Test
    fun `tavallista sivua ei luulla login-sivuksi`() {
        val html = "<HTML><BODY><TABLE><TR><TD>Match vs Someone</TD></TR></TABLE></BODY></HTML>"
        assertFalse(DgPages.isLoginPage(html))
    }

    @Test
    fun `sivuston help ja linkkisivu tunnistetaan otsikosta`() {
        assertTrue(DgPages.isSiteHelpPage(fixture("site_help.html")))
        assertTrue(DgPages.isSiteLinksPage(fixture("site_links.html")))
    }

    @Test
    fun `muita sivuja ei luulla helpiksi eika linkkisivuksi`() {
        // Tasan-vertailun koko pointti: jokaisen sivun otsikossa on sana DailyGammon,
        // joten contains laukeaisi kaikilla. Ristiin myös toisiinsa.
        listOf(
            "top_page.html",
            "login_page.html",
            "profile_page.html",
            "lounge_page.html",
            "forum_index.html",
            "move_board.html",
        ).forEach { name ->
            assertFalse(DgPages.isSiteHelpPage(fixture(name)), "Ei saisi olla help: $name")
            assertFalse(DgPages.isSiteLinksPage(fixture(name)), "Ei saisi olla linkkisivu: $name")
        }
        assertFalse(DgPages.isSiteHelpPage(fixture("site_links.html")))
        assertFalse(DgPages.isSiteLinksPage(fixture("site_help.html")))
    }
    /**
     * Kutsun kuittaus tunnistetaan otsikosta, mitattu 4.9.2026 (`invite_sent.html`).
     * Ristiin muihin sivuihin, koska jokaisen otsikossa on sana DailyGammon: tasan-vertailu
     * on koko ehto, ja contains laukeaisi kaikilla.
     */
    @Test
    fun `kutsun kuittaus tunnistetaan eika muita luulla siksi`() {
        assertTrue(DgPages.isInviteSentPage(fixture("invite_sent.html")))

        listOf(
            "top_page.html",
            "login_page.html",
            "profile_page.html",
            "lounge_page.html",
            "message_sent.html",
            "ignore_done.html",
            "forum_posted.html",
        ).forEach { name ->
            assertFalse(DgPages.isInviteSentPage(fixture(name)), "Ei saisi olla kutsun kuittaus: $name")
        }
    }

    /**
     * Palstakommentin kuittaus tunnistetaan `Review your message.` -linkista, mitattu
     * 4.9.2026 (`forum_posted.html`). Otsikko ei kelpaa: se on sama kuin ketjulla ja
     * indeksilla, ja juuri ne kaksi on erotettava.
     */
    @Test
    fun `palstalahetyksen kuittaus tunnistetaan linkista`() {
        assertTrue(DgPages.isForumPostedPage(fixture("forum_posted.html")))

        listOf(
            "forum_index.html",
            "forum_index_politics.html",
            "forum_thread.html",
            "forum_read.html",
            "forum_month.html",
            "forum_compose_add.html",
        ).forEach { name ->
            assertFalse(DgPages.isForumPostedPage(fixture(name)), "Ei saisi olla kuittaus: $name")
        }
    }

    /**
     * Kuittaus nimeaa syntyneen viestin, ja se on sen hyodyllisin piirre: osoite osoittaa
     * ankkuriin ketjussa eika ketjun alkuun.
     */
    @Test
    fun `kuittaus nimeaa syntyneen viestin osoitteen`() {
        val path = DgPages.postedMessagePath(org.jsoup.Jsoup.parse(fixture("forum_posted.html")))

        assertEquals("/bg/forum2/politics/read/60001/54#54", path)
    }

    /** Käyttökatkoilmoitus mitattu 11.9.2026: otsikko `DailyGammon Backups`, ei rakennetta. */
    @Test
    fun `nukkumissivu tunnistetaan otsikosta`() {
        val html = fixture("site_sleeping.html")

        assertTrue(DgPages.isSleepingPage(html))
        assertFalse(DgPages.isLoginPage(html), "Nukkumissivu ei ole login-sivu")
        assertFalse(DgPages.isTopPage(html), "Nukkumissivu ei ole otteluluettelo")
        assertFalse(DgPages.isBoardPage(html), "Nukkumissivu ei ole lauta")
    }

    @Test
    fun `muita sivuja ei luulla nukkumissivuksi`() {
        listOf(
            "login_page.html",
            "top_page_logged_out.html",
            "top_page.html",
            "top_page_no_matches.html",
            "move_board.html",
            "profile_page.html",
            "settings_page.html",
            "forum_thread.html",
        ).forEach { name ->
            assertFalse(DgPages.isSleepingPage(fixture(name)), "Ei saisi nukkua: $name")
        }
    }

}
