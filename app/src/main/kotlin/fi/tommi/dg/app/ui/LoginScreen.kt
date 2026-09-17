package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    state: TopUiState.SignedOut,
    onSignIn: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var login by rememberSaveable { mutableStateOf("") }
    // Salasanaa ei säilötä rememberSaveableen: se päätyisi tallennettuun tilaan ja sitä
    // kautta järjestelmän muistiin. remember riittää, koska kenttä täytetään kerran.
    var password by remember { mutableStateOf("") }
    // **Oletus on piilotettu, ja se on valinta eikä perimä.** Piilotettu kenttä tekee
    // lyöntivirheestä näkymättömän, ja mobiilissa kirjoittaminen on virheille altista
    // (Tommi 26.8.2026); näyttäminen on siksi olemassa. Se ei silti ole oletus, koska
    // kenttä on auki myös sivullisen silmille eikä ruutu tiedä kuka katsoo.
    var passwordVisible by remember { mutableStateOf(false) }
    // Tallennuskehotteen pyytäjä, ks. lähetysnapin kommentti.
    val autofill = LocalAutofillManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .dgReadingSurface()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(modifier = Modifier.widthIn(max = 480.dp)) {
            Text(
                text = stringResource(R.string.login_cleartext_warning),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }

        // Kentät kerrotaan salasanamanagerille (auditoinnin kohta B2) Compose 1.8:n
        // semanttisella autofillilla. Vanha AutofillNode-viritys poistettiin, koska
        // laitemittaus 24.8.2026 osoitti ettei se luovuta täyttöä loppuun asti (istunto jäi
        // WAITING_DATASET_AUTH -tilaan). Autofillin tuoma tunnus voi sisältää ylimääräisiä
        // välilyöntejä (foorumitapaus 4/2021), ja se siivotaan TopViewModel.signInissä eikä
        // tässä: siivous kuuluu sinne missä arvo otetaan käyttöön, ei jokaiseen kenttään
        // erikseen.
        //
        // **Pelkkä contentType ei riitä, ja se on mitattu 26.8.2026.** Tämän ruudun
        // autofill-istunto oli laitteella STATE_ACTIVE ja tarjous näkyi näppäimistön
        // yläpuolella, mutta tarjolla ei ollut yhtään osumaa: Google Passwordsissa ei ole
        // riviä tälle sovellukselle eikä sitä voi syntyä, koska mikään ei kerro
        // järjestelmälle että lomake lähetettiin. Ilman tallennusta ei ole täyttöä, joten
        // B2 ei ole täyttöongelma vaan tallennusongelma. Sen toinen puolisko on
        // [AutofillManager.commit] alla.
        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text(stringResource(R.string.login_username)) },
            supportingText = { Text(stringResource(R.string.login_username_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .semantics { contentType = ContentType.Username },
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            // Teksti eikä kuvake: tässä sovelluksessa ei ole yhtäkään ikonia, ja yksi
            // ainoa toisi mukanaan ikonikirjaston ja kysymyksen mitä muuta sillä pitäisi
            // merkitä. Sana kertoo saman ilman kumpaakaan.
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        stringResource(
                            if (passwordVisible) {
                                R.string.login_password_hide
                            } else {
                                R.string.login_password_show
                            }
                        )
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .semantics { contentType = ContentType.Password },
        )

        if (state.lastAttemptFailed || state.storageFailed) {
            Text(
                text = stringResource(
                    if (state.storageFailed) R.string.login_storage_failed else R.string.login_failed
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (state.busy) {
            CircularProgressIndicator()
        } else {
            Button(
                // **Commit on tallennuskehotteen ainoa laukaisin, ja se on tässä eikä
                // onnistumisen jälkeen.** Kehote koskee niitä kenttiä jotka ovat ruudulla
                // sillä hetkellä, ja onnistunut kirjautuminen vie ruudun pois, joten
                // onnistumista odottava commit tulisi liian myöhään. Hinta sanotaan
                // ääneen: kehote voi tulla myös väärästä salasanasta, ja silloin sen saa
                // hylätä. Vaihtoehto oli ettei kehotetta tule koskaan, mikä on 24.8.2026
                // asti mitattu tila.
                onClick = {
                    onSignIn(login, password)
                    autofill?.commit()
                },
                enabled = login.isNotBlank() && password.isNotEmpty(),
            ) {
                Text(stringResource(R.string.login_submit))
            }
        }
    }
}
