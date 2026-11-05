package fi.tommi.dg.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fi.tommi.dg.app.R

/**
 * Pitkän painalluksen valikko pelaajan nimelle ja otteluriville (Tommin valinta 20.10.2026,
 * `docs/UI.md`).
 *
 * **Syy on jono eikä tila.** Otteluriviltä pääsi ennen profiiliin ja turnaukseen vain laudan
 * sivupaneelin kautta, ja ottelun avaaminen kuluttaa jonoa. Valikko vie samoihin paikkoihin
 * avaamatta ottelua. Ottelun selaussivu jäi pois, koska sen vaikutusta jonoon ei ole mitattu.
 *
 * Jokainen kohta näkyy vain kun sen kohde on olemassa: profiili kun sivu antoi linkin,
 * turnaus kun ottelulla on turnaus, ja viestit kun arkistossa on tämän nimen keskustelu.
 * Kohta joka avaisi tyhjän ruudun olisi väite jota sovellus ei voi lunastaa.
 */
sealed interface PlayerMenuEntry {
    data class Profile(val path: String) : PlayerMenuEntry
    data class Tournament(val path: String) : PlayerMenuEntry
    data class Conversation(val name: String) : PlayerMenuEntry
}

/** Valikon kohdat järjestyksessä. Puhdas funktio, jotta ehdot ovat testattavissa. */
fun playerMenuEntries(
    name: String?,
    profilePath: String?,
    eventPath: String?,
    archived: Set<String>,
): List<PlayerMenuEntry> = listOfNotNull(
    profilePath?.let { PlayerMenuEntry.Profile(it) },
    eventPath?.let { PlayerMenuEntry.Tournament(it) },
    name?.takeIf { it.isNotBlank() && it in archived }?.let { PlayerMenuEntry.Conversation(it) },
)

/**
 * Mitä valikon kohdat tekevät, annettuna kerran `MainActivity`ssä. Null: valikkoa ei ole,
 * esimerkiksi esikatselussa.
 */
class PlayerMenuActions(
    val openPath: (String) -> Unit,
    val openConversation: (String) -> Unit,
    val archived: Set<String>,
)

val LocalPlayerMenu = compositionLocalOf<PlayerMenuActions?> { null }

/**
 * Sisältö jonka napautus tekee [onClick]in ja pitkä painallus avaa valikon. Kun valikossa
 * ei olisi yhtään kohtaa, pitkä painallus ei tee mitään eikä ruudunlukija tarjoa sitä.
 *
 * TalkBack lukee pitkän painalluksen toimintona nimellä [R.string.player_menu_label], joten
 * valikko on ruudunlukijalla sama kuin sormella eikä ylimääräistä elettä tarvita.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerMenuBox(
    name: String?,
    profilePath: String?,
    modifier: Modifier = Modifier,
    eventPath: String? = null,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val actions = LocalPlayerMenu.current
    val on = fi.tommi.dg.app.session.Gesture.HOLD_PLAYER_MENU in LocalGestures.current
    val entries = if (actions == null || !on) emptyList()
    else playerMenuEntries(name, profilePath, eventPath, actions.archived)
    var open by remember { mutableStateOf(false) }
    val buzz = rememberGestureBuzz()
    val clickModifier = when {
        entries.isNotEmpty() -> Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClickLabel = stringResource(R.string.player_menu_label),
            onLongClick = {
                buzz()
                open = true
            },
        )
        onClick != null -> Modifier.combinedClickable(onClick = onClick)
        else -> Modifier
    }
    Box(modifier = modifier.then(clickModifier)) {
        content()
        if (actions != null) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                entries.forEach { entry ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (entry) {
                                    is PlayerMenuEntry.Profile -> stringResource(R.string.player_menu_profile)
                                    is PlayerMenuEntry.Tournament -> stringResource(R.string.player_menu_tournament)
                                    is PlayerMenuEntry.Conversation ->
                                        stringResource(R.string.player_menu_messages, entry.name)
                                },
                            )
                        },
                        onClick = {
                            open = false
                            when (entry) {
                                is PlayerMenuEntry.Profile -> actions.openPath(entry.path)
                                is PlayerMenuEntry.Tournament -> actions.openPath(entry.path)
                                is PlayerMenuEntry.Conversation -> actions.openConversation(entry.name)
                            }
                        },
                    )
                }
            }
        }
    }
}
