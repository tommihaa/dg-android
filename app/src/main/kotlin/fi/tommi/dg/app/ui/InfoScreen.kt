package fi.tommi.dg.app.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R

/**
 * Info-välilehden kohteet.
 *
 * **Lista on lyhyempi kuin taulukko `docs/UI.md`:ssä, ja se on tahallista.** Välilehden
 * luvattu sisältö on viisi kohdetta; DG Help ja Links tulivat 29.8.2026 kun sivut oli
 * haettu ja jäsennetty, ja jäljellä on peliasetusten erottaminen sovelluksen omista.
 * Rivi jota ei voi avata ei ole rivi, joten sekin lisätään vasta kun sillä on sisältö.
 *
 * Järjestys on lukijan eikä lähteen: sovelluksen oma manuaali ja asetukset ensin,
 * sivuston omat tekstit niiden perässä.
 */
/**
 * Rivin alkuperä nimen värinä (Tommin valinta 8.11.2026, mockupin vaihtoehto A): järjestys
 * pysyy aakkosellisena, ja väri kertoo kenen kohde on. Teeman rooleista, jotta erottelu
 * toimii jokaisessa teemassa: sivusto korostusvärillä, sovellus tekstin värillä ja pelaajat
 * teeman kolmannella värillä. Rating-värejä ei käytetä, koska ne tarkoittavat jo muuta.
 */
enum class InfoOrigin { Site, App, Players }

@Composable
private fun InfoOrigin.color(): Color = when (this) {
    InfoOrigin.Site -> MaterialTheme.colorScheme.primary
    InfoOrigin.App -> MaterialTheme.colorScheme.onSurface
    InfoOrigin.Players -> MaterialTheme.colorScheme.tertiary
}

enum class InfoSection(
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    val origin: InfoOrigin,
) {
    /**
     * **Asetukset eivät ole tällä listalla** (Tommin päätös 4.9.2026, *"Info ja
     * asetusnäkymä ja valinta ehdottomasti erikseen"*). Info on lukemista ja asetukset
     * ovat säätämistä, joten niillä on eri ruutu ja eri nappi. Rivi oli tässä 4.9.2026
     * asti, ja `SETTINGS_ROUTE` on nyt sen ainoa kohde.
     */
    Help(R.string.info_help_label, R.string.info_help_summary, InfoOrigin.App),
    /**
     * Katkohistoria (Tommin tilaus 18.9.2026). Heti manuaalin perässä, koska se on
     * sovelluksen omaa tietoa kuten manuaali, ja ennen sivuston sivuja.
     */
    Drops(R.string.info_drops_label, R.string.info_drops_summary, InfoOrigin.App),
    SiteHelp(R.string.info_site_help_label, R.string.info_site_help_summary, InfoOrigin.Site),
    SiteLinks(R.string.info_site_links_label, R.string.info_site_links_summary, InfoOrigin.Site),
    /**
     * Pelaajien ylläpitämät linkit (Tommin tilaus 15.10.2026). Oma rivi erillään sivuston
     * Linksistä, koska lähde on pelaajat eikä DailyGammon; Players mapin rivi piirtyy
     * Linksin ja tämän väliin, joten pelaajien kohteet ovat peräkkäin.
     */
    PlayersLinks(R.string.players_links_label, R.string.players_links_summary, InfoOrigin.Players),
    /**
     * Sivuston luovutussivu `/bg/resign` (3.9.2026, Tommin tilaus). Info-välilehden rivi
     * eikä otteluluettelon nappi, koska luovutus on sivustolla tavallinen valikkotoiminto
     * (`SUBSTANSSI.md` kohta 36) ja Top Pagen oma linkki on navigointipalkissa.
     */
    Resign(R.string.info_resign_label, R.string.info_resign_summary, InfoOrigin.Site),
}

/**
 * Info-välilehden juuri: listarivit joista porautuu eteenpäin.
 *
 * **Muoto on listarivi eikä segmenttirivi, ja se on päätetty** (`docs/UI.md` 26.8.2026).
 * Segmenttirivi on kahdelle tai kolmelle rinnakkaiselle, ja nämä kohteet eivät ole
 * rinnakkaisia: asetuslomake on sivuston tilaa, manuaali on sovelluksen omaa tekstiä, ja
 * tulevat kohteet ovat sivuston omia sivuja. Alasvetovalikkoa ei käytetä kummassakaan
 * tapauksessa.
 *
 * **Ruutu ei tiedä mitä rivin takana on.** Se kertoo vain että jokin valittiin, ja
 * `MainActivity` piirtää sub-ruudun saman välilehden sisällä. Näin näkymämalli syntyy vasta
 * kun sitä tarvitaan. Asetukset eivät ole näiden joukossa lainkaan 4.9.2026 alkaen, vaan
 * omalla reitillään otteluluettelon `Settings`-napin takana (`docs/UI.md`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onOpen: (InfoSection) -> Unit,
    /**
     * Uloskirjautuminen (Tommin päätös 4.9.2026). Oli otteluluettelon yläpalkissa
     * `Refresh`in vieressä, ja naapuruus oli koko vika: toista painetaan päivittäin,
     * toista tuskin koskaan.
     *
     * Oma parametrinsa eikä [InfoSection], koska osio on kohde johon porautuu ja tämä on
     * teko. Rivin muoto on silti sama, koska lukijalle se on listan viimeinen rivi.
     */
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    /** DailyGammon Helpin noudon tila haun tarpeisiin, ks. [InfoSearchViewModel]. */
    siteHelp: SiteHelpSearchState = SiteHelpSearchState.Idle,
    /** Pyytää `/help`in noudon; kutsutaan kun hakukenttään kirjoitetaan tai uusinnasta. */
    onLoadSiteHelp: () -> Unit = {},
    /** Kirjautuneen tilin nimi, jonka kohdalle pelaajien kartta avautuu (9.10.2026). */
    ownName: String? = null,
    /** Näkyykö Connection drops; vain viimeaikaisten katkojen jälkeen (8.11.2026). */
    showDrops: Boolean = true,
) {
    // Kenttä on ruudun omaa tilaa: haku ei lähetä mitään sivustolle App Helpin osalta, ja
    // DG Helpin nouto pyydetään erikseen ensimmäisellä merkillä.
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            // Ei Back-nappia: tämä on välilehden juuri, ja siirtyminen pois on palkilla.
            TopAppBar(
                title = { Text(stringResource(R.string.info_title)) },
                colors = dgTopAppBarColors(),
            )
        },
    ) { insets ->
        // `fillMaxWidth` eikä `fillMaxSize` (6.9.2026): sarake mittautuu sisältönsä korkuiseksi,
        // jolloin lukupinta loppuu viimeiseen riviin ja tyhjä tila sen alla saa kuvion täytenä.
        // Kehys mittaa saman korkeuden täytekuvalle (7.9.2026, `DgFill.kt`).
        DgFillFrame(modifier = Modifier.padding(insets)) {
        val scroll = rememberScrollState()
        // Välilehden uusi napautus vie alkuun (`TabSwipe.kt`).
        ScrollToTopOnTabTap { scroll.animateScrollTo(0) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .dgReadingSurface(),
        ) {
            // Hakukenttä listan yläpuolella (Tommin valinta 24.9.2026, mockupin vaihtoehto A):
            // kirjoittaminen alkaa suoraan, ja tulokset korvaavat listan kunnes kenttä
            // tyhjennetään.
            InfoSearchField(
                query = query,
                onQueryChange = {
                    query = it
                    if (it.isNotBlank()) onLoadSiteHelp()
                },
            )
            if (query.isNotBlank()) {
                InfoSearchResults(query = query, siteHelp = siteHelp, onRetry = onLoadSiteHelp)
                return@Column
            }

            val openMap = rememberPlayersMapOpener(ownName)
            InfoSection.entries.filter { it != InfoSection.Drops || showDrops }.forEach { section ->
                InfoRow(
                    label = stringResource(section.labelRes),
                    summary = stringResource(section.summaryRes),
                    origin = section.origin,
                    onClick = { onOpen(section) },
                )
                HorizontalDivider()
                // Pelaajien kartta Linksin perässä (Tommin valinta 9.10.2026 kahdesta
                // laitekaappauksesta, toinen oli linkki Loungen Players-jaksossa). Ei
                // [InfoSection], koska rivi ei porraudu ruutuun vaan avaa selaimen.
                if (section == InfoSection.SiteLinks) {
                    InfoRow(
                        label = stringResource(R.string.players_map_label),
                        summary = stringResource(R.string.players_map_summary),
                        origin = InfoOrigin.Players,
                        onClick = openMap,
                    )
                    HorizontalDivider()
                }
            }

            // Viimeisenä ja ilman vahvistusdialogia. Teko on nyt kahden napautuksen päässä
            // eri välilehdellä, eikä sen vieressä ole mitään mitä painetaan päivittäin.
            InfoRow(
                label = stringResource(R.string.action_sign_out),
                summary = stringResource(R.string.action_sign_out_summary),
                // Unohtaa tunnukset tältä laitteelta, joten teko on sovelluksen oma.
                origin = InfoOrigin.App,
                onClick = onSignOut,
            )
            HorizontalDivider()
        }
        }
    }
}

/**
 * Yksi listarivi. Selite on rivillä eikä vasta sub-ruudulla, koska kohteiden nimet ovat
 * yksisanaisia ja yksi sana ei kerro mitä rivin takaa löytyy.
 */
@Composable
private fun InfoRow(label: String, summary: String, origin: InfoOrigin, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = origin.color(),
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Hakukenttä. Ei erillistä hakunappia, koska haku ei lähetä mitään: tulokset päivittyvät
 * kirjoittaessa, ja tyhjennys on kentän oma rasti.
 */
@Composable
private fun InfoSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        label = { Text(stringResource(R.string.info_search_label)) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                TextButton(onClick = { onQueryChange("") }) {
                    Text(stringResource(R.string.info_search_clear))
                }
            }
        } else {
            null
        },
    )
}

/**
 * Osumat listana. App Helpin kohdat luetaan resursseista tässä, koska ne ovat
 * `stringResource`in takana; sivuston kohdat tulevat noudosta.
 */
@Composable
private fun InfoSearchResults(query: String, siteHelp: SiteHelpSearchState, onRetry: () -> Unit) {
    val resources = LocalContext.current.resources
    val appEntries = remember(resources) {
        HELP_GROUPS.flatMapIndexed { g, group ->
            group.items.mapIndexed { i, item ->
                InfoEntry(
                    number = "${'A' + g}.${i + 1}",
                    question = resources.getString(item.question),
                    answer = resources.getString(item.answer),
                )
            }
        }
    }
    // Merkinnät säilyttävä vastaus App Helpin tunnuksella, jotta osuma piirtyy samoin kuin
    // manuaalissa (3.10.2026). Haku itse lukee pelkän tekstin [appEntries]istä.
    val appStyled = remember(resources) {
        HELP_GROUPS.flatMapIndexed { g, group ->
            group.items.mapIndexed { i, item -> "${'A' + g}.${i + 1}" to resources.getText(item.answer) }
        }.toMap()
    }
    val site = (siteHelp as? SiteHelpSearchState.Loaded)?.page
    val hits = remember(query, appEntries, site) { searchInfo(query, appEntries, site) }
    val words = remember(query) { queryWords(query) }

    // Noudon tila ensin, koska se kertoo onko lista kokonainen.
    when (siteHelp) {
        SiteHelpSearchState.Loading -> InfoSearchNote(stringResource(R.string.info_search_loading))
        SiteHelpSearchState.Failed -> InfoSearchNote(
            text = stringResource(R.string.info_search_failed),
            onClick = onRetry,
        )
        else -> Unit
    }
    if (hits.isEmpty()) {
        InfoSearchNote(stringResource(R.string.info_search_empty, query.trim()))
        return
    }
    // Yksi osuma auki kerrallaan, sama kuin manuaalissa.
    var open by remember(query) { mutableStateOf<InfoHit?>(null) }
    hits.forEach { hit ->
        InfoHitRow(
            hit = hit,
            styledAnswer = if (hit.source == InfoHitSource.APP) appStyled[hit.number] else null,
            words = words,
            open = open == hit,
            onToggle = { open = if (open == hit) null else hit },
        )
        HorizontalDivider()
    }
}

@Composable
private fun InfoSearchNote(text: String, onClick: (() -> Unit)? = null) {
    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * Osuma: lähde ja kohdan tunnus, kysymys, ja vastauksesta kaksi riviä. Napautus avaa
 * vastauksen kokonaan paikallaan, joten lukija ei joudu etsimään kohtaa uudelleen
 * manuaalista tai sivuston ohjeesta.
 */
@Composable
private fun InfoHitRow(
    hit: InfoHit,
    styledAnswer: CharSequence?,
    words: List<String>,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val highlight = SpanStyle(
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    )
    val bold = helpBoldStyle()
    val heading = helpHeadingStyle()
    // Suljettuna koko osuma avaa, avattuna vain otsake sulkee ja vastaus on maalattavissa
    // (Tommin havainto 8.11.2026, sama kuin manuaalin `HelpItemView`).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (open) Modifier else Modifier.clickable(onClick = onToggle))
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column(
            modifier = if (open) Modifier.fillMaxWidth().clickable(onClick = onToggle) else Modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = when (hit.source) {
                    InfoHitSource.APP -> stringResource(R.string.info_search_source_app, hit.number)
                    InfoHitSource.SITE -> stringResource(R.string.info_search_source_site, hit.number)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = highlighted(AnnotatedString(hit.question), words, highlight),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val answer = highlighted(
            styledAnswer?.helpStyled(bold, heading, dgDark()) ?: AnnotatedString(hit.answer),
            words,
            highlight,
        )
        if (open) {
            SelectionContainer {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Hakusanojen esiintymät korostettuina, kirjainkoosta välittämättä; [base]n tyylit säilyvät. */
private fun highlighted(base: AnnotatedString, words: List<String>, style: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        append(base)
        val text = base.text
        words.forEach { word ->
            var from = text.indexOf(word, ignoreCase = true)
            while (from >= 0) {
                addStyle(style, from, from + word.length)
                from = text.indexOf(word, from + word.length, ignoreCase = true)
            }
        }
    }
