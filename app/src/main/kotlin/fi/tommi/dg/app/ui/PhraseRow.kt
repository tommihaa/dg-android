package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R
import fi.tommi.dg.app.session.PhraseBook

/**
 * Fraasinapit kirjoituskentän alla, ja niiden muokkaus. Sama rivi kahdessa paikassa:
 * viestiruudun vastauskentässä (24.8.2026) ja laudan chat-kortissa (3.9.2026, Tommin
 * tilaus), ja lista on yksi ([PhraseBook]).
 *
 * **Nappi täyttää kentän eikä lähetä** (Tommin päätös 24.8.2026, kaksi kuittausta): lähetys
 * jää sivun tai ruudun omalle lähetysnapille, joten vahvistuskysymystä ei synny eikä
 * vahinkopainallus voi tehdä peruuttamatonta tekoa. Painallus liittää eikä korvaa, koska
 * korvaava nappi hävittäisi kirjoitetun tekstin yhdellä kosketuksella, ja juuri se vika on
 * kirjattu DG Mobilesta (`SUBSTANSSI.md` kohta 50).
 *
 * **Lista on muokattava 3.9.2026 alkaen** (Tommin tilaus; oletus on yhä kohdan 58
 * rituaalifraasit, ks. `PhraseStore`). Kaksi tekoa, kumpikin ilman omaa lomaketta:
 *
 * - **Tallennus lukee kentän.** Uusi fraasi kirjoitetaan samaan kenttään kuin viesti ja
 *   tallennetaan napilla. Nappi on pois päältä kun kenttä on tyhjä tai teksti on jo napiksi
 *   tallennettu, joten se ei voi tehdä mitään mitä ei näe. Kenttää ei tyhjennetä
 *   tallennuksessa: teksti on yhä luonnos, ja sen saa lähettää heti.
 * - **Poisto on oma tila.** Muokkaustilassa jokainen nappi poistaa itsensä eikä täytä
 *   kenttää, ja se näkyy rastina napin tekstissä. Tila on tämän rivin oma eikä säily,
 *   joten sitä ei voi jättää vahingossa päälle seuraavaan kertaan.
 *
 * `FlowRow`, koska lista voi olla pidempi kuin ruudun leveys.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhraseRow(
    phrases: List<String>,
    /** Kentän teksti: fraasi liitetään sen perään, ja tallennus lukee sen. */
    draft: String,
    enabled: Boolean,
    onDraftChange: (String) -> Unit,
    onAddPhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingRequested by remember { mutableStateOf(false) }
    // Tyhjä lista sulkee muokkaustilan itsestään: viimeisen poiston jälkeen ei ole enää
    // mitään poistettavaa, ja "Done" ilman nappeja olisi tila ilman sisältöä. Johdettu
    // arvo eikä kirjoitus kompositiossa, jotta tila ei riipu recomposition järjestyksestä.
    val editing = editingRequested && phrases.isNotEmpty()

    val candidate = PhraseBook.normalize(draft)
    val removeLabel = stringResource(R.string.messages_phrase_remove)

    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        phrases.forEach { phrase ->
            if (editing) {
                TextButton(
                    onClick = { onRemovePhrase(phrase) },
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.semantics {
                        contentDescription = removeLabel.format(phrase)
                    },
                ) {
                    Text("$phrase ×")
                }
            } else {
                TextButton(
                    onClick = {
                        onDraftChange(if (draft.isBlank()) phrase else "${draft.trimEnd()} $phrase")
                    },
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(phrase)
                }
            }
        }
        if (!editing) {
            TextButton(
                onClick = { onAddPhrase(draft) },
                enabled = enabled && candidate.isNotEmpty() && candidate !in phrases,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(stringResource(R.string.messages_phrase_save))
            }
        }
        if (phrases.isNotEmpty()) {
            TextButton(
                onClick = { editingRequested = !editing },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    stringResource(
                        if (editing) R.string.messages_phrase_done else R.string.messages_phrase_edit,
                    ),
                )
            }
        }
    }
}
