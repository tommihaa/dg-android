package fi.tommi.dg.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.SettingsTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Laiteasetusten vienti ja tuonti (Tommin tilaus 28.9.2026). Kaksi nappia laiteosion alla:
 * *Save to file* kirjoittaa merkkijonon tiedostoon ja *Open file* avaa sen esikatseluruutuun.
 * Leikepöydän napit *Copy settings* ja *Paste settings* poistettiin 29.9.2026 (Tommin tilaus:
 * tiedostonapit hoitavat saman ilman sekaannusta).
 *
 * **Tuonti näyttää ensin mitkä valinnat muuttuvat ja kirjoittaa vasta vahvistuksesta**
 * (Tommin valinta 28.9.2026). Lukukelvoton teksti ei muuta mitään. Kirjoituksen jälkeen
 * [onImported] luo näkymän uudelleen, jotta jokainen ruutu lukee uudet arvot säilöistä.
 *
 * **Tiedosto kulkee Androidin omalla tiedostoikkunalla** (Tommin tilaus 28.9.2026), joten
 * kohde voi olla Drive tai mikä tahansa muu. Syy oli leikepöydän raja: tabletin leikepöytä ei
 * päässyt Pixelille, ja käsin kirjoitettuun 505 merkin riviin näppäimistö lisäsi pisteen perään
 * välilyönnin. Ruudun tekstikenttä on yhä muokattava, joten rivin voi liittää siihen käsin.
 */
@Composable
internal fun SettingsTransferGroup(transfer: SettingsTransfer, onImported: () -> Unit) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf<Boolean?>(null) }
    // Tiedostosta luettu rivi esikatseluruudun alkuarvoksi; null kun ruutu on kiinni.
    var fromFile by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = transfer.export()
                saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "wt")
                            ?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
                    }.getOrDefault(false)
                }
            }
        }
    }
    // Tyyppisuodatin on väljä samasta syystä kuin arkiston tuonnissa: Drive ja tiedostonhallinta
    // nimeävät `.txt`:n vaihtelevasti, ja tiukka suodatin piilottaisi tiedoston ilman virhettä.
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)
                            ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }.getOrNull()
                }
                saved = null
                fromFile = text.orEmpty().trim()
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)) {
        Text(
            text = stringResource(R.string.settings_transfer),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.settings_transfer_explain),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { saved = null; saveLauncher.launch(FILE_NAME) }) {
                Text(stringResource(R.string.settings_transfer_save))
            }
            Button(onClick = { saved = null; openLauncher.launch(arrayOf("*/*")) }) {
                Text(stringResource(R.string.settings_transfer_open))
            }
        }
        val note = when {
            saved == true -> R.string.settings_transfer_saved
            saved == false -> R.string.settings_transfer_save_failed
            else -> null
        }
        if (note != null) {
            Text(
                text = stringResource(note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
    fromFile?.let { initial ->
        PasteDialog(
            transfer = transfer,
            initial = initial,
            onClose = { fromFile = null },
            onImported = { fromFile = null; onImported() },
        )
    }
}

@Composable
private fun PasteDialog(
    transfer: SettingsTransfer,
    initial: String,
    onClose: () -> Unit,
    onImported: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    // Null ennen esikatselua; sen jälkeen luettu sisältö ja muuttuvat tiedostot.
    var preview by remember { mutableStateOf<Pair<SettingsTransfer.Payload?, List<String>>?>(null) }
    val payload = preview?.first
    val changes = preview?.second.orEmpty()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.settings_transfer_dialog)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; preview = null },
                    label = { Text(stringResource(R.string.settings_transfer_field)) },
                    maxLines = 4,
                    // Näppäimistön korjaukset rikkovat rivin: Gboard lisäsi `DGA1.`:n perään
                    // välilyönnin ja ison kirjaimen (mitattu Pixelillä 28.9.2026).
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                    ),
                    // Puhelimella kirjoittaminen kääntää pystyyn myös dialogissa (`reportsTyping`).
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).reportsTyping(),
                )
                when {
                    preview == null -> Unit
                    payload == null -> Text(stringResource(R.string.settings_transfer_unreadable))
                    changes.isEmpty() -> Text(stringResource(R.string.settings_transfer_same))
                    else -> {
                        val labels = changes.map { fileLabel(it) }
                        Text(stringResource(R.string.settings_transfer_changes, labels.joinToString("\n") { "· $it" }))
                    }
                }
            }
        },
        confirmButton = {
            if (payload != null && changes.isNotEmpty()) {
                TextButton(onClick = { transfer.apply(payload); onImported() }) {
                    Text(stringResource(R.string.settings_transfer_use))
                }
            } else {
                TextButton(
                    onClick = { val p = transfer.parse(text); preview = p to (p?.let(transfer::changes).orEmpty()) },
                    enabled = text.isNotBlank(),
                ) { Text(stringResource(R.string.settings_transfer_preview)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** Tiedoston nimi asetusruudun omaksi otsikoksi, jotta esikatselu puhuu samoin sanoin kuin ruutu. */
@Composable
private fun fileLabel(file: String): String = stringResource(labelFor(file))

@StringRes
private fun labelFor(file: String): Int = when (file) {
    "dg_app_theme" -> R.string.settings_theme
    "dg_board_style" -> R.string.settings_board_style
    "dg_board_shuffle" -> R.string.settings_board_shuffle_toggle
    "dg_dice_style" -> R.string.settings_dice_style
    "dg_score_style" -> R.string.settings_score_style
    "dg_busy_style" -> R.string.settings_busy_style
    "dg_forced_steps" -> R.string.settings_forced_steps_toggle
    "dg_greedy_bearoff" -> R.string.settings_greedy_bearoff_toggle
    // Ryhmän otsikko eikä kytkin, koska tiedostossa on kaksi kytkintä (oma ja vastustajan
    // siirto). Rivi puuttui 29.9.2026 asti. Tiedosto nimettiin silloin Handednessiksi.
    "dg_move_arrows" -> R.string.settings_move_arrows
    "dg_beaver_confirm" -> R.string.settings_confirm_beaver_toggle
    "dg_dice_tap" -> R.string.settings_dice_tap
    "dg_rarity" -> R.string.settings_rarity
    "dg_match_order" -> R.string.settings_transfer_match_order
    "dg_message_filter" -> R.string.settings_transfer_message_filter
    "dg_portrait_lock" -> R.string.settings_portrait_lock_toggle
    "dg_board_rotation" -> R.string.settings_board_rotates_toggle
    "dg_full_screen" -> R.string.settings_full_screen_toggle
    else -> R.string.settings_handedness
}

/** Tallennusikkunan ehdottama nimi; käyttäjä voi vaihtaa sen. */
private const val FILE_NAME = "dg-android-settings.txt"
