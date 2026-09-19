package fi.tommi.dg.app.ui

import java.io.File
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
        assertTrue(styles.size == 3)
        assertMentions(listOf("look"), styles)
        assertMentions(listOf("look"), listOf(strings.getValue("settings_board_style"), strings.getValue("settings_dice_style"), strings.getValue("settings_score_style"), strings.getValue("settings_busy_style")))
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
