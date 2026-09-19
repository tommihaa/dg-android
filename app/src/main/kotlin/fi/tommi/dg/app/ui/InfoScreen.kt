package fi.tommi.dg.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
enum class InfoSection(@StringRes val labelRes: Int, @StringRes val summaryRes: Int) {
    /**
     * **Asetukset eivät ole tällä listalla** (Tommin päätös 4.9.2026, *"Info ja
     * asetusnäkymä ja valinta ehdottomasti erikseen"*). Info on lukemista ja asetukset
     * ovat säätämistä, joten niillä on eri ruutu ja eri nappi. Rivi oli tässä 4.9.2026
     * asti, ja `SETTINGS_ROUTE` on nyt sen ainoa kohde.
     */
    Help(R.string.info_help_label, R.string.info_help_summary),
    /**
     * Katkohistoria (Tommin tilaus 18.9.2026). Heti manuaalin perässä, koska se on
     * sovelluksen omaa tietoa kuten manuaali, ja ennen sivuston sivuja.
     */
    Drops(R.string.info_drops_label, R.string.info_drops_summary),
    SiteHelp(R.string.info_site_help_label, R.string.info_site_help_summary),
    SiteLinks(R.string.info_site_links_label, R.string.info_site_links_summary),
    /**
     * Sivuston luovutussivu `/bg/resign` (3.9.2026, Tommin tilaus). Info-välilehden rivi
     * eikä otteluluettelon nappi, koska luovutus on sivustolla tavallinen valikkotoiminto
     * (`SUBSTANSSI.md` kohta 36) ja Top Pagen oma linkki on navigointipalkissa.
     */
    Resign(R.string.info_resign_label, R.string.info_resign_summary),
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
) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .dgReadingSurface(),
        ) {
            InfoSection.entries.forEach { section ->
                InfoRow(
                    label = stringResource(section.labelRes),
                    summary = stringResource(section.summaryRes),
                    onClick = { onOpen(section) },
                )
                HorizontalDivider()
            }

            // Viimeisenä ja ilman vahvistusdialogia. Teko on nyt kahden napautuksen päässä
            // eri välilehdellä, eikä sen vieressä ole mitään mitä painetaan päivittäin.
            InfoRow(
                label = stringResource(R.string.action_sign_out),
                summary = stringResource(R.string.action_sign_out_summary),
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
private fun InfoRow(label: String, summary: String, onClick: () -> Unit) {
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
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
