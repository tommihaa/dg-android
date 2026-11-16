package fi.tommi.dg.app.ui

import android.graphics.Typeface
import android.text.Annotation
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R

/**
 * Pelaajan manuaali.
 *
 * **Ruutu on olemassa yhden päätöksen takia** (Tommi 9.8.2026). Lautaruudun omat napit
 * purettiin ja tilalle tulivat eleet: veto alaspäin hakee sivun uudelleen ja reunapyyhkäisy
 * vie takaisin. Jokainen niistä on näkymätön, ja arvattava ele ei ole ele. Sen sijaan että
 * eleet olisi jätetty löydettäviksi, ne on kirjoitettu tähän. Sivun toimintonapit palasivat
 * lautaruudulle 10.8.2026 pelaamisen myötä, mutta eleet jäivät ja tämä ruutu niiden mukana.
 *
 * Sanavalinta on kanonista: `CLAUDE.md`:n Kenelle-osio rajaa kohderyhmän yhden laitteen
 * **pelaajaksi**, joten teksti puhuu pelaajalle eikä käyttäjälle.
 *
 * Teksti on englantia kuten muukin käyttöliittymä (päätös 5.8.2026), ja samasta syystä:
 * DailyGammonin omat termit ovat sivun tekstiä eikä niille ole suomenkielistä vastinetta.
 *
 * **Manuaali kertoo myös sen mitä sovellus ei tee.** Lupaus kapeni 10.8.2026 lukutilasta
 * painallusehtoon: mikään ei lähde sivustolle ilman pelaajan painallusta, eikä ottelun
 * avaaminen kuluta vuoroa. Pelaajan on voitava luottaa siihen ilman että hän lukee koodia.
 *
 * **Kirjoitettu uusiksi 15.9.2026** (Tommin tilaus: *"tämä pitää olla luotettavaa silloinkin
 * kun sovellus etenee jaettavaksi tai play-kauppaan"*). Jatkossa manuaali on osa toimintoa:
 * käyttäjälle näkyvän toiminnon muutos päivittää sen samassa commitissa (`docs/OHJE.md`), ja
 * `HelpConsistencyTest` vartioi että nimetyt asiat ovat tekstissä.
 *
 * **FAQ-muoto 15.9.2026 illalla** (Tommi: *"aikamoinen wall of text, saisiko siitä FAQ
 * version"* ja *"erot DailyGammonin saittiin myös näkyviksi"*). Kaksi syytä seinään: rivi
 * oli vaakatabletilla noin 190 merkkiä, ja 12 jaksoa oli kerralla auki eikä otsikko kertonut
 * mihin kysymykseen kappale vastaa. Nyt ruudulla on kysymyslista ryhmittäin, vastaus aukeaa
 * napautuksella, ja lukupinta on rajattu [READING_WIDTH]:iin. Erot sivustoon ovat oma ryhmä
 * väitteinä, koska ero on hyödyllisin juuri väitteenä; muut ryhmät ovat kysymyksiä.
 * Vastaustekstit ovat entiset kappaleet, joten `docs/OHJE.md`:n totuustaulukko pätee
 * avaimittain.
 *
 * **Lyhyet kappaleet ja lihavoidut nimet 29.9.2026** (Tommi: vastaukset olivat edelleen
 * *"wall-of-text-like, koska pitkiä kappaleita eikä korostuksia"*, valinta b). Kappaleessa on
 * yksi asia, ja napit, kytkimet, välilehdet ja kentät on lihavoitu samalla nimellä kuin
 * ruudulla, `strings.xml`:n `<b>`-merkinnällä. Merkintä luetaan [helpStyled]illa, joka lukee
 * myös pitkien vastausten `<i>`-väliotsikot (3.10.2026).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ruudun nimen linkki: välilehti, Infon rivi tai Settings. Jaksoviittaukset hoidetaan täällä. */
    onOpenLink: (HelpLink) -> Unit = {},
) {
    val context = LocalContext.current
    // Nimet luetaan kerran kaikista merkkijonoista, jotta linkin kohde ratkeaa samasta
    // lähteestä kuin ruudun nimi (`lihavoidutNimetOvatRuudulla` vartioi samaa lähdettä).
    val names = remember(context) {
        val res = context.resources
        val strings = R.string::class.java.fields.mapNotNull { f ->
            runCatching { f.name to res.getString(f.getInt(null)) }.getOrNull()
        }.toMap()
        helpNameLinks(strings)
    }
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemCoords = remember { mutableMapOf<Int, LayoutCoordinates>() }
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    var scrollTo by remember { mutableStateOf<Int?>(null) }
    // Jaksoviittaus avaa jakson ja vierittää sen kysymysrivin ruudun ylälaitaan. Vieritys
    // odottaa että edellisen jakson sulkeutuminen on animoitu, koska se siirtää kohdetta.
    LaunchedEffect(scrollTo) {
        val q = scrollTo ?: return@LaunchedEffect
        var last = Float.NaN
        var stable = 0
        repeat(90) {
            withFrameNanos {}
            val view = viewport ?: return@repeat
            val item = itemCoords[q]?.takeIf { it.isAttached } ?: return@repeat
            val y = view.localPositionOf(item, Offset.Zero).y + scroll.value
            stable = if (y == last) stable + 1 else 0
            last = y
            if (stable >= 3) {
                scroll.animateScrollTo(y.toInt().coerceAtLeast(0))
                scrollTo = null
                return@LaunchedEffect
            }
        }
        scrollTo = null
    }
    val links = HelpLinks(
        names = names,
        style = helpLinkStyles(),
        onOpen = { link ->
            if (link is HelpLink.Section) {
                questionFor(link.code)?.let { q ->
                    open = q
                    scrollTo = q
                }
            } else {
                onOpenLink(link)
            }
        },
    )
    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            // Back-nappi palasi 26.8.2026 kun manuaali siirtyi Info-välilehden sisään:
            // se ei ole enää oma välilehtensä vaan listarivin takana, ja porautumisesta on
            // päästävä takaisin. Järjestelmä-back tekee saman, ks. `MainActivity`.
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.board_back))
                    }
                },
            )
        },
    ) { insets ->
        // Kehys mittaa sarakkeen korkeuden ja piirtää täytekuvan sen alle (`DgFill.kt`).
        DgFillFrame(modifier = Modifier.padding(insets)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { viewport = it }
                    .verticalScroll(scroll)
                    .dgReadingSurface(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Lukupinta täyttää leveyden, teksti ei: vaakatabletilla koko leveys antoi
                // noin 190 merkin rivin, ja silmä ei löydä seuraavan rivin alkua.
                Column(
                    modifier = Modifier
                        .widthIn(max = READING_WIDTH)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Järjestys on lukujärjestys pelaajalle joka avaa sovelluksen ensimmäistä
                    // kertaa: mikä tämä on, mikä eroaa sivustosta, välilehdet, luettelo, lauta,
                    // viestit, muut välilehdet ja asetukset, ja lopuksi lupaus siitä mitä ei
                    // tapahdu. `HelpConsistencyTest` vartioi että jokainen `help_q_*` on tässä.
                    // Yksi vastaus auki kerrallaan ja tunnus muotoa ryhmä.kohta, ryhmä
                    // kirjaimena (Tommi 15.9.2026 illalla: "numeroida, jotta niihin olisi
                    // helpompi viitata", "siten, että numeron poistuessa kaikki ei mene
                    // uusiksi" ja "ryhmät ehkä numeroinnin sijaan aakkosilla"). Juokseva
                    // numero olisi siirtänyt kaikki seuraavat; ryhmäkohtainen siirtää vain
                    // saman ryhmän loppupään, ja kirjain erottaa ryhmän kohdasta (B.7).
                    HELP_GROUPS.forEachIndexed { g, group ->
                        HelpGroupView(
                            group = group,
                            groupLetter = ('A' + g).toString(),
                            openQuestion = open,
                            onToggle = { q -> open = if (open == q) null else q },
                            links = links,
                            onPlaced = { q, c -> itemCoords[q] = c },
                        )
                    }
                }
            }
        }
    }
}

/** Noin 80 merkkiä `bodyMedium`-tekstiä; sama luokka kuin kirjan palsta. */
private val READING_WIDTH = 640.dp

/**
 * Mihin App Helpin korostus vie (Tommin tilaus 8.11.2026, *"app help korostukset linkeiksi"*,
 * valinta: molemmat). Jaksoviittaus avaa jakson, ja ruudun nimi vie ruutuun silloin kun kohde
 * on yksiselitteinen: välilehti, Infon oma rivi tai Settings. Napit ja muut teot (Refresh,
 * Double, Log Out) jäävät linkittä, koska nimi kertoo mitä painetaan eikä mihin mennään.
 */
sealed interface HelpLink {
    data class Section(val code: String) : HelpLink
    data class Tab(val tab: DgTab) : HelpLink
    data class Info(val section: InfoSection) : HelpLink
    data object Settings : HelpLink
}

/**
 * Ruudun nimet linkkeineen, avaimena näkyvä teksti. Puhdas funktio, jotta `HelpConsistencyTest`
 * voi ajaa sen `strings.xml`:ää vasten.
 *
 * Settings-linkki tulee nimelle jonka **jokainen** merkkijono on `settings_`-avaimella: Board
 * on sekä laudan otsikko että asetusryhmä, joten se jää linkittä. `settings_submit` on
 * DailyGammon-segmentin nappi, ja Settings avautuu laiteosioon.
 */
internal fun helpNameLinks(strings: Map<String, String>): Map<String, HelpLink> {
    val out = mutableMapOf<String, HelpLink>()
    strings.entries.groupBy({ it.value }, { it.key }).forEach { (value, keys) ->
        if (keys.all { it.startsWith("settings_") && it !in SITE_SEGMENT_KEYS }) out[value] = HelpLink.Settings
    }
    val fixed = mapOf(
        "tab_matches" to HelpLink.Tab(DgTab.Matches),
        "tab_lounge" to HelpLink.Tab(DgTab.Lounge),
        "tab_discussion" to HelpLink.Tab(DgTab.Discussion),
        "tab_messages" to HelpLink.Tab(DgTab.Messages),
        "tab_info" to HelpLink.Tab(DgTab.Info),
        "info_drops_label" to HelpLink.Info(InfoSection.Drops),
        "players_links_label" to HelpLink.Info(InfoSection.PlayersLinks),
        "settings_title" to HelpLink.Settings,
    )
    fixed.forEach { (key, link) -> strings[key]?.let { out[it] = link } }
    return out
}

private val SITE_SEGMENT_KEYS = setOf("settings_submit")

/** Jaksoviittaus muodossa *F.4 How do I …?*, sama muoto jota `jaksoviittauksenKoodiOsuuKysymykseen` vartioi. */
internal val SECTION_REFERENCE = Regex("""\b([A-H]\.\d+) ([^?.]+\?)""")

/** Koodista kysymykseksi samalla laskulla kuin ruudun numerointi. */
internal fun questionFor(code: String): Int? {
    val group = code.firstOrNull()?.let { it - 'A' } ?: return null
    val item = code.substringAfter('.').toIntOrNull() ?: return null
    return HELP_GROUPS.getOrNull(group)?.items?.getOrNull(item - 1)?.question
}

internal class HelpLinks(
    val names: Map<String, HelpLink>,
    val style: TextLinkStyles,
    val onOpen: (HelpLink) -> Unit,
)

internal data class HelpItem(@StringRes val question: Int, @StringRes val answer: Int)

internal data class HelpGroup(@StringRes val heading: Int, val items: List<HelpItem>)

internal val HELP_GROUPS = listOf(
    HelpGroup(
        R.string.help_group_about,
        listOf(
            HelpItem(R.string.help_q_about, R.string.help_a_about),
            HelpItem(R.string.help_q_encryption, R.string.help_a_encryption),
            HelpItem(R.string.help_q_site_only, R.string.help_a_site_only),
        ),
    ),
    HelpGroup(
        R.string.help_group_differences,
        listOf(
            HelpItem(R.string.help_q_d_messages, R.string.help_a_d_messages),
            HelpItem(R.string.help_q_d_session, R.string.help_a_d_session),
            HelpItem(R.string.help_q_d_sort, R.string.help_a_d_sort),
            HelpItem(R.string.help_q_d_local, R.string.help_a_d_local),
            HelpItem(R.string.help_q_d_place, R.string.help_a_d_place),
            HelpItem(R.string.help_q_d_tournament, R.string.help_a_d_tournament),
            HelpItem(R.string.help_q_d_archive, R.string.help_a_d_archive),
            HelpItem(R.string.help_q_d_accounts, R.string.help_a_d_accounts),
            HelpItem(R.string.help_q_d_reminders, R.string.help_a_d_reminders),
            HelpItem(R.string.help_q_d_marks, R.string.help_a_d_marks),
            HelpItem(R.string.help_q_d_look, R.string.help_a_d_look),
            HelpItem(R.string.help_q_d_confirm, R.string.help_a_d_confirm),
            HelpItem(R.string.help_q_d_unconfirmed, R.string.help_a_d_unconfirmed),
        ),
    ),
    HelpGroup(
        R.string.help_group_tabs,
        listOf(
            HelpItem(R.string.help_q_tabs, R.string.help_a_tabs),
            // Värit omana kohtanaan 6.10.2026 (Tommin tilaus): ne koskevat jokaista välilehteä,
            // ja aiemmin säännöt olivat laudan paneelin vastauksessa.
            HelpItem(R.string.help_q_colours, R.string.help_a_colours),
            // Kopioinnin rajat omana kohtanaan (Tommin tilaus 8.11.2026): maalaus koskee
            // jokaista välilehteä kuten värit.
            HelpItem(R.string.help_q_copy, R.string.help_a_copy),
        ),
    ),
    HelpGroup(
        R.string.help_group_matches,
        listOf(
            HelpItem(R.string.help_q_order, R.string.help_a_order),
            HelpItem(R.string.help_q_open, R.string.help_a_open),
            HelpItem(R.string.help_q_finished, R.string.help_a_finished),
            HelpItem(R.string.help_q_tournaments, R.string.help_a_tournaments),
        ),
    ),
    HelpGroup(
        R.string.help_group_board,
        listOf(
            HelpItem(R.string.help_q_move, R.string.help_a_move),
            HelpItem(R.string.help_q_forced, R.string.help_a_forced),
            HelpItem(R.string.help_q_double, R.string.help_a_double),
            HelpItem(R.string.help_q_unconfirmed, R.string.help_a_unconfirmed),
            HelpItem(R.string.help_q_gestures, R.string.help_a_gestures),
            HelpItem(R.string.help_q_beside, R.string.help_a_beside),
            HelpItem(R.string.help_q_skip, R.string.help_a_skip),
            HelpItem(R.string.help_q_look, R.string.help_a_look),
            HelpItem(R.string.help_q_small_screen, R.string.help_a_small_screen),
        ),
    ),
    HelpGroup(
        R.string.help_group_messages,
        listOf(
            HelpItem(R.string.help_q_take, R.string.help_a_take),
            HelpItem(R.string.help_q_queue, R.string.help_a_queue),
            HelpItem(R.string.help_q_reply, R.string.help_a_reply),
            HelpItem(R.string.help_q_archive, R.string.help_a_archive),
            HelpItem(R.string.help_q_one_device, R.string.help_a_one_device),
            HelpItem(R.string.help_q_move_device, R.string.help_a_move_device),
        ),
    ),
    HelpGroup(
        R.string.help_group_lounge,
        listOf(
            HelpItem(R.string.help_q_join, R.string.help_a_join),
            HelpItem(R.string.help_q_forum, R.string.help_a_forum),
            HelpItem(R.string.help_q_settings, R.string.help_a_settings),
        ),
    ),
    HelpGroup(R.string.help_group_never, listOf(HelpItem(R.string.help_q_never, R.string.help_a_never))),
)

@Composable
private fun HelpGroupView(
    group: HelpGroup,
    groupLetter: String,
    openQuestion: Int?,
    onToggle: (Int) -> Unit,
    links: HelpLinks? = null,
    onPlaced: (Int, LayoutCoordinates) -> Unit = { _, _ -> },
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$groupLetter  " + stringResource(group.heading),
            style = MaterialTheme.typography.titleSmall,
            // Sama sääntö kuin muualla, ks. `LoungeScreen.SectionHeading`.
            color = MaterialTheme.colorScheme.primary,
        )
        group.items.forEachIndexed { i, item ->
            HelpItemView(
                number = "$groupLetter.${i + 1}",
                item = item,
                open = openQuestion == item.question,
                onToggle = { onToggle(item.question) },
                links = links,
                modifier = Modifier.onGloballyPositioned { onPlaced(item.question, it) },
            )
        }
    }
}

/**
 * Numeroitu kysymys aina näkyvissä, vastaus napautuksella. Avoin kohta säilyy käännön yli
 * (`rememberSaveable` kutsujassa), koska manuaalia luetaan usein juuri laitetta kääntäen.
 * Numero on `stringResource`in ulkopuolella, jotta kysymysteksti pysyy sellaisenaan
 * `HelpConsistencyTest`in luettavana.
 */
/**
 * Vastauksen `<b>`-merkinnät [bold]-tyyliksi ja `<i>`-merkinnät [heading]-tyyliksi.
 * `stringResource` palauttaa pelkän merkkijonon ja pudottaa merkinnät, joten vastaus luetaan
 * `getText`illä, joka säilyttää ne `StyleSpan`eina.
 *
 * **Väliotsikot 3.10.2026** (Tommi: B.10 oli *"liian wall-of-text"*, valinta: pitkät vastaukset
 * ja haku samalla muotoilulla). `<i>` on väliotsikko, koska `<b>` on jo ruudun nimien merkki;
 * otsikko aloittaa kappaleensa omalla rivillään ilman tyhjää riviä väliin, jotta se kuuluu
 * alla olevaan tekstiin eikä leiju kappaleiden välissä (ensimmäinen laiteajo 3.10.2026), minkä
 * `HelpConsistencyTest` vartioi. Haku (`InfoScreen`) käyttää
 * tätä samaa funktiota, jotta osuma näyttää samalta kuin manuaalissa.
 */
internal fun CharSequence.helpStyled(
    bold: SpanStyle,
    heading: SpanStyle,
    dark: Boolean? = null,
    links: HelpLinks? = null,
): AnnotatedString {
    val source = this
    return buildAnnotatedString {
        append(source.toString())
        if (links != null) {
            for (m in SECTION_REFERENCE.findAll(source)) {
                val link = HelpLink.Section(m.groupValues[1])
                addLink(LinkAnnotation.Clickable(m.value, links.style) { links.onOpen(link) }, m.range.first, m.range.last + 1)
            }
        }
        if (source is Spanned) {
            for (span in source.getSpans(0, source.length, StyleSpan::class.java)) {
                val style = when (span.style) {
                    Typeface.BOLD -> bold
                    Typeface.ITALIC -> heading
                    else -> continue
                }
                val start = source.getSpanStart(span)
                val end = source.getSpanEnd(span)
                addStyle(style, start, end)
                if (span.style == Typeface.BOLD && links != null) {
                    val name = source.subSequence(start, end).toString()
                    links.names[name]?.let { link ->
                        addLink(LinkAnnotation.Clickable(name, links.style) { links.onOpen(link) }, start, end)
                    }
                }
            }
            // Värisana omalla värillään (Tommin tilaus 8.11.2026, C.2): `<annotation
            // rarity="0">grey</annotation>` saa portaan 0 sävyn samasta [Rarity]-taulukosta
            // kuin nimet, joten ohjeen sana ja ruudun nimi ovat aina samaa väriä.
            if (dark != null) {
                for (span in source.getSpans(0, source.length, Annotation::class.java)) {
                    if (span.key != "rarity") continue
                    val tier = span.value.toIntOrNull() ?: continue
                    addStyle(
                        SpanStyle(color = Rarity.color(tier, dark)),
                        source.getSpanStart(span),
                        source.getSpanEnd(span),
                    )
                }
            }
        }
    }
}

/** Vastauksen kaksi tyyliä teemasta: nimet korostusvärillä, väliotsikot otsikkokirjaimella. */
@Composable
internal fun helpBoldStyle(): SpanStyle =
    SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

/** Linkki on nimen korostus alleviivattuna, jotta se erottuu linkittömästä nimestä. */
@Composable
internal fun helpLinkStyles(): TextLinkStyles =
    TextLinkStyles(style = helpBoldStyle().copy(textDecoration = TextDecoration.Underline))

@Composable
internal fun helpHeadingStyle(): SpanStyle =
    MaterialTheme.typography.titleMedium.toSpanStyle().copy(color = MaterialTheme.colorScheme.onSurface)

@Composable
private fun HelpItemView(
    number: String,
    item: HelpItem,
    open: Boolean,
    onToggle: () -> Unit,
    links: HelpLinks? = null,
    modifier: Modifier = Modifier,
) {
    // Napautus kuuluu vain kysymysriville, ja vastaus on maalattavissa (Tommin havainto
    // 8.11.2026: pitkä painallus vastauksessa sulki jakson, koska koko sarake oli yksi
    // `clickable` ja se laukaisee napautuksen myös pitkän painalluksen lopussa).
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$number  " + stringResource(item.question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        )
        if (open) {
            val context = LocalContext.current
            val bold = helpBoldStyle()
            val heading = helpHeadingStyle()
            SelectionContainer {
                Text(
                    text = context.resources.getText(item.answer).helpStyled(bold, heading, dgDark(), links),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp),
                )
            }
        }
    }
}
