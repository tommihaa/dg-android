package fi.tommi.dg.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fi.tommi.dg.app.R

/**
 * Peruuttamattoman teon vahvistus, yhtenä runkona (Tommin päätös 4.9.2026, kaavahaku K8).
 *
 * Sama dialogi oli kirjoitettu auki kuudesti: laudan kuutioteko, palstan lähetys, loungen
 * Join ja Sign Up sekä profiilin Invite ja Ignore. Otsikko, teksti, vahvistusnappi ja
 * peruutusnappi toistuivat rivi riviltä, ja `docs/AVOIMET.md` piti kohtaa auki siksi että
 * yhtenäistäminen koskettaa viittä ruutua kerralla. Kuudes oli jo kirjoitettu kun kohta
 * ratkaistiin, eli ennakko piti.
 *
 * **Runko on yhteinen, sanat eivät.** Tämä ei päätä mitä dialogi sanoo eikä milloin se
 * avataan: kutsuja antaa otsikon, tekstin ja vahvistusnapin sanan, ja jokainen kutsupaikka
 * kantaa oman perustelunsa omassa `@Composable`-funktiossaan. Se on tarkoituksellista, koska
 * `docs/UI.md` kuvaa dialogien sanat ruutukohtaisesti ja se kuvaus pysyy voimassa.
 *
 * **Vahvistusnapin sana on sivun oma sana** siellä missä teko on sivuston teko (`Double`,
 * `Accept`, palstan `submitLabel`, sivuutuksen napin teksti). Sääntö on `docs/UI.md`:n
 * *Sivusto nimeää teot, sovellus nimeää paikat*, eikä se ole tämän funktion tiedossa vaan
 * kutsujan; siksi [confirmLabel] on tavallinen merkkijono eikä valinta kahdesta.
 *
 * **Peruutusnappi on oletuksena `Cancel`** (`dialog_cancel`, jonka jaettu nimi sovittiin jo
 * 3.9.2026). Palstan lähetys sanoo *Not yet*, koska siellä peruutus tarkoittaa keskeneräistä
 * tekstiä eikä hylättyä aikomusta, ja se annetaan silloin [cancelLabel]ina.
 *
 * Tähän ei nostettu niitä kahta dialogia jotka **kertovat lopputuloksen** eivätkä kysy mitään
 * (laudan `UnconfirmedDialog`, asetusten `SaveResultDialog`). Niissä on yksi nappi eikä
 * kahta, eikä niillä ole peruttavaa tekoa; sama runko peittäisi eron kysymisen ja
 * kertomisen välillä.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String = stringResource(R.string.dialog_cancel),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
    )
}
