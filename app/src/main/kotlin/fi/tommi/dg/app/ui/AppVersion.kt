package fi.tommi.dg.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.PackageInfoCompat
import fi.tommi.dg.app.R

/**
 * Asennettu versio pienenä rivinä (Tommin päätökset 9.10.2026): laiteasetusten alalaidassa ja
 * Top Pagen arkistorivin alla. Testaaja ei nähnyt mistään, oliko päivitys jo tullut, ja etsi
 * versiota asetuksista. Luku tulee paketista eikä koodista, joten se on aina asennetun version.
 */
@Composable
internal fun AppVersionLine(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val info = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    } ?: return
    Text(
        text = stringResource(
            R.string.settings_version,
            info.versionName.orEmpty(),
            PackageInfoCompat.getLongVersionCode(info),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
