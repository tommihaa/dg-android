package fi.tommi.dg.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import java.net.URI

/** Yksi pelaajien ylläpitämä kohde: nimi, ylläpitäjä tai tekijä ja osoite. */
internal data class PlayersLink(val title: String, val keeper: String, val url: String)

/** Ryhmä otsikkoineen; otsikko on merkkijonoresurssi, nimet ovat omia nimiään. */
internal data class PlayersLinkGroup(@StringRes val titleRes: Int, val links: List<PlayersLink>)

/**
 * Pelaajien ylläpitämät linkit (Tommin tilaus ja valinnat 15.10.2026, Players mapin
 * innoittamana). Osoitteet luettiin DG:n palstalta samana päivänä, ketjun numero kunkin
 * rivin perässä.
 *
 * **Rivi avaa taulukon suoraan eikä palstaketjua** (Tommin valinta: *"päivitetään osoitteet
 * kauden vaihtuessa"*). Turnausten taulukot ovat kausikohtaisia (`2026`, `#5`, `-16`), joten
 * osoite vanhenee kun uusi kausi alkaa, ja se vaihdetaan tähän käsin.
 *
 * Snowie puuttuu tarkoituksella: ohjelma on lopetettu, ja `snowie.com` myy nykyään
 * lumijäämyllyjä.
 */
internal val PLAYERS_LINKS = listOf(
    PlayersLinkGroup(
        R.string.players_links_rankings,
        listOf(
            // Ketju 64259. Kolmesta taulukosta ensimmäinen (aktiiviset, kuukausi ja vuodet
            // taaksepäin); kaksi muuta ovat saman rankingin rinnakkaisnäkymiä.
            PlayersLink(
                "DG Active Players Ranking",
                "Keram56",
                "https://docs.google.com/spreadsheets/d/18ycNZzHgKDZQ8-p_Io5lDeZmCNu0e6QvxHirMUMnbC8/edit?usp=sharing",
            ),
        ),
    ),
    PlayersLinkGroup(
        R.string.players_links_tournaments,
        listOf(
            // Ketju 64131, lyhytosoite tiny.cc/dg_8x8.
            PlayersLink("DailyGammon 8x8 Tournament", "eg", "https://dg8x8.gergelyelias.com/"),
            // Ketju 64225, kaksi sarjaa: tinyurl.com/ABG-TC2026 ja tinyurl.com/ABG-PL2026.
            PlayersLink(
                "Anti-backgammon Championship 2026",
                "basplund",
                "https://docs.google.com/spreadsheets/d/e/2PACX-1vRREKcHpMaIXlNO0f_qetnQ2q7qkFSEuN_B8P5C3pPGbmyELyWxShCWlrq6dyddHAhZVOhXQ2GQqqly/pubhtml",
            ),
            PlayersLink(
                "Anti-backgammon Premier League 2026",
                "basplund",
                "https://docs.google.com/spreadsheets/d/e/2PACX-1vT98ULhKHlniSrM2eG7n9_wO7uHN4apV9JCmGm4pyap0ydWXTaDJsMW97AGu7ye6APAQrgV28k-Cmk4/pubhtml",
            ),
            // Ketju 63732, tinyurl.com/SCOOBY-Challenge-16 (lyhyempi SCOOBY-Challenge on 404).
            PlayersLink(
                "Scooby's Challenge 16",
                "BIG-SCOOBY",
                "https://docs.google.com/spreadsheets/d/e/2PACX-1vQMXUJWT9uF6T-z7nWHirCAfZORZFqgpj_GOHSpvDoSzHeGxFY_0VRdM4EzBjkZHAoKNVWGMZPnozQv/pubhtml",
            ),
            // Ketju 63626, tiny.cc/dg_pp5.
            PlayersLink(
                "Picture Postcard Tournament #5",
                "Pendragon",
                "https://docs.google.com/spreadsheets/d/e/2PACX-1vROJKVt5PmeVBA7uYR9JK66A2W-LQdrynd62y1vtXeJlDrlfxI17kf18d0p88cWhrHDI2eWsyBjWs_B/pubhtml",
            ),
            // Ketju 63708, tiny.cc/DGIC_2026.
            PlayersLink(
                "International Challenges 2026",
                "ianshaw",
                "https://docs.google.com/spreadsheets/d/e/2PACX-1vRx849I7FfmUGZq0Dq8VDbyB0e7MCK005WeDApk3P8D1v87jFZyloQx2ClkeTfBcxNaTfP96PHW9wps/pubhtml",
            ),
        ),
    ),
    PlayersLinkGroup(
        R.string.players_links_apps,
        listOf(
            PlayersLink("DailyGammon for iOS", "hape42", "https://apps.apple.com/us/app/dailygammon/id1449956942"),
            PlayersLink(
                "DG Mobile",
                "Android",
                "https://play.google.com/store/apps/details?id=com.dailygammon.DGMobile",
            ),
            // Ketjut 64121 ja 64144. Tekijän oma osoite; se ohjaa palvelimelle, jonka nimi
            // voi vaihtua.
            PlayersLink("Second Roll", "darkhelmet", "http://secondroll.de"),
        ),
    ),
    PlayersLinkGroup(
        R.string.players_links_analysis,
        listOf(
            PlayersLink("BGBlitz", "Frank Berger", "https://www.bgblitz.com/"),
            PlayersLink("Backgammon Sage Pro", "Mark Higgins", "https://sage.bgsage.ai/"),
            PlayersLink("GNU Backgammon", "GNU", "https://www.gnu.org/software/gnubg/"),
            PlayersLink("eXtreme Gammon 2", "XG", "https://www.extremegammon.com/"),
        ),
    ),
)

/**
 * Infon *Players' links* -ruutu: ryhmitelty lista, jonka jokainen rivi avaa selaimen.
 *
 * Rivin toinen rivi on ylläpitäjä ja osoitteen palvelin, ei koko osoite: taulukoiden
 * osoitteet ovat satamerkkisiä tunnisteita, mutta palvelin riittää kertomaan minne
 * napautus vie, samalla lupauksella kuin DailyGammon Linksissä.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersLinksScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.players_links_label)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.board_back))
                    }
                },
            )
        },
    ) { padding ->
        DgLazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.players_links_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            PLAYERS_LINKS.forEach { group ->
                item {
                    Text(
                        text = stringResource(group.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                items(group.links, contentType = { DgStripedRow }) { link ->
                    PlayersLinkRow(link)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PlayersLinkRow(link: PlayersLink) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(link.url) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = link.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            text = "${link.keeper} · ${linkHost(link.url)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Osoitteen palvelin ilman `www.`-alkua; jäsennysvirheessä koko osoite. */
internal fun linkHost(url: String): String =
    runCatching { URI(url).host?.removePrefix("www.") }.getOrNull() ?: url
