package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R

/**
 * Näytetään kun laite osaa tunnistaa mutta käyttäjä ei vielä ole tunnistautunut, tai kun
 * edellinen yritys peruttiin/epäonnistui. [onRetry] avaa järjestelmän oman tunnistusruudun
 * uudelleen; tätä ruutua itseään ei voi ohittaa muuten.
 */
@Composable
fun AppLockScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_lock_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.app_lock_subtitle))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.app_lock_retry))
        }
    }
}
