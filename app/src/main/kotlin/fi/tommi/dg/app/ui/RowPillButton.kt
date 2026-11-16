package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Rivin teko ääriviivapillerinä (Tommin valinta 8.11.2026, `docs/UI.md` › *Rivin teot
 * ääriviivapillereinä*). Yksi muoto kaikille listarivien teoille: `Review`, `Active Game`,
 * `Sign Up`, `Cancel Signup`, `Join`, `Hide`, `Reply` ja Share-napit. Täytetty pilleri on
 * varattu vuoron tilalle, jota ei voi napauttaa.
 *
 * Matala (32 dp) jotta rivi pysyy kahden tekstirivin korkuisena, sama mitta kuin entisellä
 * `ShareButton`illa 27.9.2026 alkaen.
 */
@Composable
internal fun RowPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
