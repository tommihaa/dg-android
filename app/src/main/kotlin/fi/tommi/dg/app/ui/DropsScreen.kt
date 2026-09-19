package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.domain.ConnectionDrop
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.SeenMatch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Katkohistoria: milloin `Board not confirmed` tuli ja miksi, uusin ensin.
 *
 * **Tämä on lukulista eikä työkalu**, kuten [MarksScreen] (Tommin tilaus 18.9.2026, *"tee
 * katkohistoria"*). Kortti kertoo katkosta sillä hetkellä, mutta sen syy katosi kortin
 * mukana, eikä tallentamattoman pelisession palkista jäänyt mitään luettavaa. Rivi kertoo
 * hetken, ottelun, napin ja poikkeuksen luokan nimen, eikä siitä avaudu mitään: lauta on
 * jo ratkennut ja ottelu voi olla ohi.
 *
 * **Ottelu nimetään ottelumuistista** (`MatchMemory`), samoin kuin merkityillä asemilla:
 * rivi kantaa vain numeron. Syy näytetään sellaisenaan (`SocketTimeoutException`), koska
 * lukija on se joka mittaa, ja käännös sanaksi hävittäisi juuri sen eron jota etsitään.
 *
 * Poistoa ei ole: rivi ei ole tehtävä joka kuitataan, ja historia rajaa itsensä sataan
 * (`DropLog.KEEP`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropsScreen(
    drops: List<ConnectionDrop>,
    remembered: Map<MatchId, SeenMatch>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.drops_title)) },
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
                    text = stringResource(R.string.drops_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (drops.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.drops_empty),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                return@DgLazyColumn
            }
            items(drops, key = { it.id }, contentType = { DgStripedRow }) { drop ->
                DropRow(drop = drop, seen = drop.matchId?.let { remembered[it] })
            }
        }
    }
}

/**
 * Yksi katko kahdella rivillä: hetki ja nappi, sen alla ottelu ja syy. Hetki on laitteen
 * kellosta ja vyöhykkeestä, koska kysymys on mitä pelaaja teki silloin, eikä sivuston aikaa
 * ole olemassa: katko on juuri se hetki jolloin sivusto ei vastannut.
 */
@Composable
private fun DropRow(drop: ConnectionDrop, seen: SeenMatch?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = stringResource(R.string.drops_row, DropClock.format(drop.atEpochMillis), drop.submit),
            style = MaterialTheme.typography.bodyMedium,
        )
        val matchId = drop.matchId
        val opponent = seen?.opponent?.name
        val match = when {
            matchId == null -> null
            opponent != null -> stringResource(R.string.marks_match_versus, opponent, matchId.value)
            else -> stringResource(R.string.marks_match_number, matchId.value)
        }
        Text(
            text = listOfNotNull(match, drop.cause).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Hetki minuutin tarkkuudella laitteen vyöhykkeessä, englanniksi kuten muu käyttöliittymä. */
internal object DropClock {
    private val FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)

    fun format(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
}
