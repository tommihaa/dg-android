package fi.tommi.dg.app.ui

import fi.tommi.dg.app.session.BoardStyle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manuaalin johdonmukaisuus (`HelpScreen.kt`, `docs/OHJE.md`). Tommin tilaus 15.9.2026: tekstin
 * on oltava luotettavaa myös kun sovellus jaetaan. Tämä ei voi todistaa että jokainen lause on
 * tosi, mutta se vartioi sen osan joka on nimettävissä: kun välilehti, lajittelunappi,
 * lautatyyli tai laiteasetuksen kytkin lisätään tai nimetään uudestaan, manuaalin on
 * mainittava se samalla nimellä, tai tämä testi kaatuu.
 *
 * Luetaan suoraan `strings.xml`:stä, koska JVM-testissä ei ole `Resources`-oliota, ja
 * manuaalin kysymykset `HelpScreen.kt`:stä, jotta kysymystä ei voi olla tekstinä ilman ruutua.
 *
 * FAQ-muoto 15.9.2026 illalla: avaimet ovat `help_q_<avain>` (kysymys) ja `help_a_<avain>`
 * (vastaus), ryhmät `help_group_*`. Maininta tarkistetaan vastauksista, ja yksi asia saa
 * asua useassa vastauksessa (esim. lajittelunapit sekä erojen ryhmässä että luettelon
 * kysymyksessä), joten tarkistus kohdistuu avainjoukkoon eikä yhteen jaksoon.
 */
class HelpConsistencyTest {

    private val strings: Map<String, String> by lazy {
        val xml = File("src/main/res/values/strings.xml").readText()
        Regex("""<string name="([a-z_0-9]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'").replace("\\n", "\n") }
    }

    private val helpScreen: String by lazy { File("src/main/kotlin/fi/tommi/dg/app/ui/HelpScreen.kt").readText() }

    private fun answers(vararg keys: String): String =
        keys.joinToString("\n") { strings["help_a_$it"] ?: error("help_a_$it puuttuu") }

    private fun assertMentions(keys: List<String>, names: List<String>) {
        val body = answers(*keys.toTypedArray())
        val missing = names.filter { it !in body }
        assertTrue("help_a_${keys.joinToString("|")} ei mainitse: $missing", missing.isEmpty())
    }

    @Test
    fun jokaisellaKysymyksellaOnVastausJaRuutu() {
        val questions = strings.keys.filter { it.startsWith("help_q_") }
        assertTrue(questions.size >= 30)
        for (q in questions) {
            val a = "help_a_" + q.removePrefix("help_q_")
            assertTrue("$a puuttuu", a in strings)
            assertTrue("$q ei ole HelpScreenissä", "R.string.$q" in helpScreen)
            assertTrue("$a ei ole HelpScreenissä", "R.string.$a" in helpScreen)
        }
        val groups = strings.keys.filter { it.startsWith("help_group_") }
        assertTrue(groups.size >= 6)
        for (g in groups) assertTrue("$g ei ole HelpScreenissä", "R.string.$g" in helpScreen)
    }

    @Test
    fun erotSivustoonOvatOmaRyhma() {
        assertTrue("help_group_differences" in strings)
        val diffs = strings.keys.filter { it.startsWith("help_q_d_") }
        assertTrue("erojen ryhmässä on alle 8 väitettä: ${diffs.size}", diffs.size >= 8)
    }

    @Test
    fun salaamatonYhteysSanotaan() {
        // Tommin tilaus 21.9.2026: pelaajille painotetaan että viestit kulkevat selkokielisinä. Jos
        // sivusto joskus tarjoaa HTTPS:n ja selkokielen sallinta poistuu, tämä kaatuu ja vastaus
        // kirjoitetaan uusiksi, koska väite "unencrypted" ei silloin enää pidä.
        val config = File("src/main/res/xml/network_security_config.xml").readText()
        val cleartext = "cleartextTrafficPermitted=\"true\"" in config && "dailygammon.com" in config
        assertTrue("network_security_config ei enää salli selkokieltä: help_a_encryption vanheni", cleartext)
        assertMentions(listOf("encryption"), listOf("HTTPS", "unencrypted", "messages", "browser"))
    }

    @Test
    fun kahdenLaitteenYhdistaminenSanotaan() {
        // Peterille 19.9.2026 lähetetty kirje lupaa laitteiden arkistojen yhdistämisen; se tehdään
        // tiedoston kautta ja manuaalin on sanottava se (Tommin kysymys 21.9.2026).
        assertMentions(listOf("move_device"), listOf("merged", strings.getValue("messages_import_action"), strings.getValue("messages_export_action")))
    }

    @Test
    fun tilinLuontiSanotaan() {
        // Tommin päätös 27.9.2026: tili luodaan sivustolla, ja manuaalin on sanottava se.
        // 1.10.2026 linkki Create Account -sivulle poistettiin (Play Consolen Data safety
        // laskee ohjauksen tilin luonniksi), joten kirjautumisruutu ei saa avata /bg/create-sivua
        // eikä manuaali saa luvata sitä. Sivun nimi jää
        // site_only-vastaukseen lihavoimatta, koska ruudulla sitä ei enää ole.
        assertMentions(listOf("about"), listOf("dailygammon.com", "email"))
        assertTrue("dailygammon.com" in strings.getValue("login_no_account"))
        assertFalse(strings.getValue("help_a_about").contains("Create Account"))
        val login = File("src/main/kotlin/fi/tommi/dg/app/ui/LoginScreen.kt").readText()
        assertFalse("LoginScreen avaa /bg/create-sivun", "\"bg/create\"" in login)
    }

    @Test
    fun lihavointiOnParillinenJaKokonainen() {
        // 29.9.2026: vastausten nimet lihavoidaan <b>-merkinnällä. Pariton merkintä lihavoisi
        // loput vastauksesta, ja tyhjä tai sisäkkäinen näkyisi ruudulla väärin ilman virhettä.
        for ((key, text) in strings.filterKeys { it.startsWith("help_a_") }) {
            val tags = Regex("</?b>").findAll(text).map { it.value }.toList()
            assertTrue("$key: <b> ja </b> eivät vuorottele: $tags", tags.withIndex().all { (i, t) -> t == if (i % 2 == 0) "<b>" else "</b>" })
            assertTrue("$key: tyhjä lihavointi", "<b></b>" !in text)
        }
    }

    @Test
    fun valiotsikkoAloittaaKappaleensa() {
        // 3.10.2026: pitkien vastausten väliotsikot ovat <i>-merkinnällä, omalla rivillään
        // kappaleen alussa. Keskellä virkettä oleva <i> piirtyisi otsikkokirjaimella, ja
        // otsikko ilman tekstiä perässään ei otsikoisi mitään.
        for ((key, text) in strings.filterKeys { it.startsWith("help_a_") }) {
            for (p in text.split("\n\n")) {
                if ("<i>" in p || "</i>" in p) {
                    assertTrue("$key: väliotsikko ei aloita kappaletta omalla rivillään: $p", Regex("(?s)<i>[^<\n]+</i>\n[^\n].*").matches(p))
                    assertTrue("$key: kaksi väliotsikkoa samassa kappaleessa: $p", p.indexOf("<i>", 1) < 0)
                }
            }
        }
    }

    @Test
    fun lihavoidutNimetOvatRuudulla() {
        // Tommin kysymys 29.9.2026: portti huomasi puuttuvan maininnan mutta ei poistettua
        // toimintoa, ja samana päivänä vastaus lupasi jo poistetut Copy settings ja Paste
        // settings -napit. Poistettu nappi vie merkkijononsa mukanaan, joten jokaisen lihavoidun
        // nimen on löydyttävä jostain muusta kuin help_-merkkijonosta. Poikkeukset ovat nimiä
        // jotka tulevat sivuston sivulta tai Androidilta eivätkä ole sovelluksen omia.
        val notOurs = setOf(
            "Active Game", // sivuston linkki, sovellus ei näytä sitä (docs/KOHDE.md)
            "Background Color", // sivuston asetuslomakkeen kenttä
            "Roll Dice", // sivuston nappi, nimi luetaan sivulta
            "Verify Decline", // sivuston vahvistusruutu
            "Verify Double", // sivuston vahvistusruutu
            "Save to Drive", // Androidin jakovalikon kohde
        )
        val ui = strings.filterKeys { !it.startsWith("help_") }.values.joinToString("\n")
        val named = strings.filterKeys { it.startsWith("help_a_") }.values
            .flatMap { Regex("<b>(.*?)</b>").findAll(it).map { m -> m.groupValues[1] }.toList() }
            .toSet()
        val gone = named.filter { it !in ui && it !in notOurs }
        assertTrue("manuaali lihavoi nimiä joita ruudulla ei ole (poistettu tai nimetty uudelleen): $gone", gone.isEmpty())
        val stale = notOurs.filter { it !in named }
        assertTrue("poikkeuslistassa nimiä joita manuaali ei enää mainitse: $stale", stale.isEmpty())
    }

    @Test
    fun sivustolleJaavatNimelta() {
        // Tommin tilaus 29.9.2026: docs/KOHDE.md › Sivuston toiminnot joita sovellus ei tee on
        // manuaalissa yhtenä vastauksena. Nimet ovat sivuston omat, joten pelaaja tunnistaa ne.
        // Donations puuttuu tarkoituksella, ks. kommentti strings.xml:ssä.
        assertMentions(
            listOf("site_only"),
            listOf("Create Account", "Change Password", "Public Profile", "Mini", "Background Color", "Active Game"),
        )
        assertTrue("help_a_site_only mainitsee lahjoitukset", "Donat" !in answers("site_only"))
    }

    @Test
    fun valilehdetNimelta() {
        assertMentions(listOf("tabs"), listOf("tab_matches", "tab_lounge", "tab_discussion", "tab_messages", "tab_info").map { strings.getValue(it) })
    }

    @Test
    fun katkohistoriaNimelta() {
        // Info-rivin nimi sekä välilehtien luettelossa että kortin kohdassa (18.9.2026).
        assertMentions(listOf("tabs"), listOf(strings.getValue("info_drops_label")))
        assertMentions(listOf("unconfirmed"), listOf(strings.getValue("info_drops_label"), strings.getValue("tab_info")))
    }

    @Test
    fun lajittelunapitNimelta() {
        val cols = listOf("col_grace", "col_time_pool", "col_round", "col_length", "col_opponent").map { strings.getValue(it) }
        assertMentions(listOf("order"), cols + listOf(strings.getValue("top_sort_by")))
        assertMentions(listOf("d_sort"), cols + listOf(strings.getValue("top_sort_by")))
        assertMentions(listOf("open"), listOf(strings.getValue("top_refresh")))
    }

    @Test
    fun lautatyylitNimelta() {
        val styles = strings.keys.filter { it.startsWith("settings_board_style_") }.map { strings.getValue(it) }
        // Joka tyylillä on nimensä, ja App Help nimeää ne kaikki.
        assertEquals(BoardStyle.entries.size, styles.size)
        assertMentions(listOf("look"), styles)
        assertMentions(listOf("look"), listOf(strings.getValue("settings_theme"), strings.getValue("settings_board_style"),strings.getValue("settings_dice_style"), strings.getValue("settings_score_style"), strings.getValue("settings_busy_style")))
    }

    @Test
    fun laiteasetustenKytkimetNimelta() {
        val toggles = strings.keys.filter { it.startsWith("settings_") && it.endsWith("_toggle") }.map { strings.getValue(it) }
        assertTrue(toggles.size >= 8)
        assertMentions(listOf("settings"), toggles)
        assertMentions(listOf("settings"), listOf(strings.getValue("settings_wallpaper"), strings.getValue("settings_board_style"), strings.getValue("settings_device_section")))
    }

    @Test
    fun jononNappiJaInboxinTeotNimelta() {
        assertMentions(listOf("take"), listOf(strings.getValue("messages_queue_fetch")))
        assertMentions(listOf("archive"), listOf(strings.getValue("messages_export_action"), strings.getValue("backup_set_up"), strings.getValue("backup_now")))
        assertMentions(listOf("d_archive"), listOf(strings.getValue("messages_export_action"), strings.getValue("backup_set_up")))
        // Roskakori 6.10.2026: poiston, perumisen ja palautuksen napit nimeltä.
        assertMentions(
            listOf("archive"),
            listOf(
                strings.getValue("messages_delete_action"),
                strings.getValue("messages_delete_message"),
                strings.getValue("messages_copy_text"),
                strings.getValue("messages_undo"),
                strings.getValue("messages_restore"),
            ),
        )
        // Tilikohtainen arkisto (16.9.2026): väitteen on nimettävä ne teot joita rajaus koskee.
        assertMentions(listOf("d_accounts"), listOf(strings.getValue("messages_export_action"), strings.getValue("backup_set_up"), strings.getValue("action_sign_out")))
    }

    @Test
    fun laudanViereisetNimelta() {
        assertMentions(listOf("beside"), listOf(strings.getValue("board_chat_open"), strings.getValue("board_chat_reply"), strings.getValue("board_reminder_cube")))
        assertMentions(listOf("skip"), listOf(strings.getValue("board_skip_game")))
        // Merkitty asema: linkki laudalla ja lista otteluluettelossa nimeltä (15.9.2026).
        assertMentions(listOf("d_marks"), listOf(strings.getValue("board_mark_position"), strings.getValue("board_reminders_title"), strings.getValue("tab_matches")))
        // Vientilinkit profiililla (16.9.2026): merkki kulkee `.sgf`:ssä, ja manuaalin on nimettävä molemmat linkit.
        assertMentions(listOf("d_marks"), listOf(strings.getValue("page_export_share"), strings.getValue("page_export_share_sgf")))
    }

    @Test
    fun loungenJaForuminTeotNimelta() {
        assertMentions(listOf("join"), listOf(strings.getValue("lounge_join"), strings.getValue("lounge_signup"), strings.getValue("lounge_signup_cancel")))
        assertMentions(listOf("forum"), listOf(strings.getValue("discussion_new"), strings.getValue("discussion_add_comment")))
    }

    @Test
    fun tuontiLuvataanJaSenSaantoSanotaan() {
        // Tuonti tuli 16.9.2026 (Tommin tilaus), ja se vain lisää eikä koskaan korvaa.
        // Manuaalin on nimettävä nappi, sanottava sääntö ja kerrottava mikä ei vielä kulje
        // tiedostossa (merkityt asemat), jottei siirto lupaa enempää kuin tiedosto kantaa.
        assertMentions(listOf("move_device", "archive"), listOf(strings.getValue("messages_import_action"), strings.getValue("messages_export_action")))
        val move = answers("move_device")
        assertTrue("move_device ei sano että laitteen oma jää ennalleen", "left as it is" in move)
        assertTrue("move_device ei sano että merkit puuttuvat", "Marked positions are not in the file" in move)
        val archive = answers("archive")
        assertTrue("archive ei sano ettei tuonti korvaa", "nothing already here is changed" in archive.lowercase())
        assertTrue("archive ei luettele tiedoston sisältöä", "messages, reminders and phrases" in archive)
    }
}
