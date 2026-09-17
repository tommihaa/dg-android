package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.domain.MarkedPosition
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.SeenMatch

/**
 * Merkityt asemat ottelun jälkeistä analyysia varten, ottelun mukaan ryhmiteltyinä.
 *
 * **Tämä on lukulista eikä työkalu** (Tommin toive 15.9.2026 illalla: *"ottelun jälkeen
 * näkisi mark-positiot, joita analyysin tullessa muistaisi uudelleen pohtia"*). Rivi kertoo
 * mistä kohdasta `.mat`-tiedostoa asema löytyy analyysiohjelmassa, eli pelin pistetilanteen
 * ja siirtonumeron, sekä sanan jonka pelaaja kirjoitti. Lauta ei avaudu tästä, koska
 * ottelu on tyypillisesti jo päättynyt eikä sivustolla ole aseman osoitetta.
 *
 * **Ottelu nimetään ottelumuistista** (`MatchMemory`), samoin kuin turnauslistassa: merkki
 * kantaa vain ottelun numeron, ja vastustaja sekä turnaus luetaan siitä mitä sovellus
 * viimeksi näki. Lauta kirjoittaa muistiin joka avauksella, joten merkitty ottelu on aina
 * muistissa, ellei kantaa ole palautettu toiselta laitteelta.
 *
 * **Poisto on kuittaus**, ei virhe: rivi poistetaan kun asema on pohdittu. Vahvistusta ei
 * kysytä samasta syystä kuin muistutuksilla, rivin voi kirjoittaa uudestaan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksScreen(
    marks: List<MarkedPosition>,
    remembered: Map<MatchId, SeenMatch>,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ryhmittely ottelun numerolla, uusin merkki ensin määrää ottelun paikan ja ottelun
    // sisällä siirtonumero järjestää (sama kuin `.mat`-tiedoston lukujärjestys).
    val groups = remember(marks) {
        marks.groupBy { it.game.matchId }
            .map { (matchId, rows) -> matchId to rows.sortedWith(compareBy({ it.game.selfScore + it.game.opponentScore }, { it.moveNumber })) }
    }
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.marks_title)) },
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
                    text = stringResource(R.string.marks_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.marks_empty),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                return@DgLazyColumn
            }
            groups.forEach { (matchId, rows) ->
                item(key = "match-${matchId.value}") {
                    MatchHeading(matchId = matchId, seen = remembered[matchId])
                }
                items(rows, key = { it.id }, contentType = { DgStripedRow }) { mark ->
                    MarkRow(mark = mark, onRemove = onRemove)
                }
            }
        }
    }
}

/**
 * Ottelun otsikko: vastustaja ja turnaus muistista, tai pelkkä numero kun muistia ei ole.
 * Numero on aina mukana, koska se on `.mat`-tiedoston nimi (`dg-<id>.mat`) ja siten
 * se sana jolla tiedosto löytyy työpöydältä.
 */
@Composable
private fun MatchHeading(matchId: MatchId, seen: SeenMatch?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 2.dp)) {
        val opponent = seen?.opponent?.name
        Text(
            text = if (opponent != null) {
                stringResource(R.string.marks_match_versus, opponent, matchId.value)
            } else {
                stringResource(R.string.marks_match_number, matchId.value)
            },
            style = MaterialTheme.typography.titleSmall,
        )
        val detail = listOfNotNull(
            seen?.eventName,
            seen?.round?.let { stringResource(R.string.page_round, it) },
            seen?.matchLength?.let { stringResource(R.string.lounge_length_label, it) },
        ).joinToString(", ")
        if (detail.isNotEmpty()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Yksi merkki, ja sen alla viiva samaan tapaan kuin Inbox-välilehden riveillä. Viiva on
 * Tommin tilaus 17.9.2026 (*"tarvitsee markit toisistaan erottavat viivat"*): rivit ovat
 * samannäköisiä ja kahden tekstirivin korkuisia, joten ilman viivaa raja ei näy.
 */
@Composable
private fun MarkRow(mark: MarkedPosition, onRemove: (Long) -> Unit) {
    val removeLabel = stringResource(R.string.marks_remove)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Pisteet sivun järjestyksessä, vastustaja ensin, sama kuin `GameKey`ssä ja
            // `.mat`-tiedoston pelin otsikossa (`opponent : 1   tommih : 5`).
            Text(
                text = stringResource(
                    R.string.marks_position,
                    mark.game.opponentScore,
                    mark.game.selfScore,
                    mark.moveNumber,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            val note = mark.note.takeIf { it.isNotBlank() }
            Text(
                text = listOfNotNull(note, ArchiveEdge.formatDate(mark.createdAtEpochMillis)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = { onRemove(mark.id) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.semantics { contentDescription = removeLabel },
        ) {
            Text(text = "×", style = MaterialTheme.typography.bodyMedium)
        }
    }
    HorizontalDivider()
}
