package fi.tommi.dg.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R

/**
 * Välilehdet ja niiden järjestys (Tommin päätös 26.8.2026, `docs/UI.md`).
 *
 * **Viisi välilehteä eikä kuusi, ja luku on itse päätös.** Kun koko sivuston tieto tuodaan
 * välilehdiksi, kohteita olisi yksitoista, ja silloin palkki ei ole enää navigointi vaan
 * lista. Viisi mahtuu puhelimen leveyteen kerralla. Välilehti on siis kategoria eikä sivu:
 * `Lounge` kantaa myös pelaajalistan ja turnaukset, ja `Info` kantaa asetukset, sovelluksen
 * manuaalin, sivuston oman Helpin ja Links-sivun.
 *
 * `Settings` ja `Help` olivat omat välilehtensä 26.8.2026 asti; ne ovat nyt `Info`n sisällä
 * listariveinä, eivätkä ne ole enää omia reittejään lainkaan (ks. `InfoScreen`).
 *
 * Lauta ei ole välilehti: sinne mennään otteluluettelosta ja sieltä palataan järjestelmän
 * eleellä, eikä palkkia piirretä laudalla lainkaan.
 */
enum class DgTab(val route: String, @StringRes val labelRes: Int) {
    Matches(TOP_ROUTE, R.string.tab_matches),
    Lounge(LOUNGE_ROUTE, R.string.tab_lounge),
    Discussion(DISCUSSION_ROUTE, R.string.tab_discussion),
    Messages(MESSAGES_ROUTE, R.string.tab_messages),
    Info(INFO_ROUTE, R.string.tab_info),
}

/**
 * Reitistä välilehdeksi, tai null kun reitti ei ole välilehti (lauta). Puhdas funktio,
 * jotta palkin näkyvyysehto on testattavissa ilman Composea.
 */
fun tabFor(route: String?): DgTab? = DgTab.entries.firstOrNull { it.route == route }

/**
 * Välilehtipalkki. Kuluttaa tilapalkin insetin itse, koska se on ruudun ylin elementti
 * ja jokainen ruutu on palkin alla oma `Scaffold`insa.
 *
 * Vieritettävyys jää paikalleen vaikka viisi mahtuu puhelimen leveyteen: isolla
 * järjestelmäfontilla se ei enää mahdu, ja vieritettävä palkki on silloin ainoa muoto joka
 * ei leikkaa nimeä pois.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DgTabRow(
    selected: DgTab?,
    onSelect: (DgTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        // Palkki piirretään vain välilehtireiteillä, joten valittu on käytännössä aina
        // olemassa; varalla Matches, jottei indeksi voi osoittaa ohi.
        selectedTabIndex = (selected ?: DgTab.Matches).ordinal,
        // Lukupinta myös palkin alle (6.9.2026): viisi nimeä ovat tekstiä kuvion päällä.
        // Tilapalkin kaistale jää pinnan ulkopuolelle, koska se ei ole sovelluksen tekstiä.
        containerColor = dgReadingSurfaceColor(),
        modifier = modifier.statusBarsPadding(),
    ) {
        DgTab.entries.forEach { tab ->
            // Teksti on omassa sisältölohkossaan eikä `text`-parametrissa, ja se on
            // mitattu valinta (Pixel 8a, 411 dp): `text` lisää 16 dp pehmustetta
            // kummallekin puolelle, jolloin viidestä 82 dp:n välilehdestä jää tekstille
            // 66 dp ja "Matches" katkeaa kahdelle riville kesken sanan. Sisältölohko ei
            // lisää pehmustetta, joten koko leveys on tekstin käytössä.
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    // Jokainen nimi omalla sävyllään, myös valitsemattomat (Tommin päätös
                    // 29.8.2026). Palkki on siis viisivärinen koko ajan, ja väri kertoo
                    // mikä välilehti on kyseessä eikä sitä missä ollaan.
                    color = tabLabelColor(tab, isSystemInDarkTheme()),
                    // Valinta ei saa jäädä pelkän värin varaan, koska värejä on nyt viisi
                    // eikä yksikään niistä ole "valittu". Valitun erottaa lihavointi ja
                    // palkin oma korostusviiva, joka on nykyisen välilehden sävyä.
                    fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal,
                    // Yksi rivi ja kolme pistettä eikä katkaisu kesken sanan: isolla
                    // järjestelmäfontilla nimi ei mahdu, ja lyhennetty nimi on
                    // luettavampi kuin tavuittamaton kahdelle riville revitty.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }
    }
}
