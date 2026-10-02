package fi.tommi.dg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R

/**
 * Se mitä ruutu sanoo kun näytettävää ei ole: virhe, väärä sivu tai katkennut istunto.
 *
 * **Yksi koti kuudelle kopiolle (1.9.2026, auditoinnin H4).** Sama keskitetty sarake
 * tekstillä ja uudelleenyritysnapilla oli kirjoitettu erikseen kuuteen ruutuun nimillä
 * `Message`, `RetryNote`, `PageRetryNote`, `FailureView` ja `NotATopPageView`. Ne eivät
 * eronneet toisistaan kuin siinä mitä parametreja kukin sattui tarvitsemaan, eli sama
 * ratkaisu oli tehty kuudesti eikä kertaakaan yhdessä paikassa.
 *
 * Kaksi eroa jäi parametreiksi, koska ne ovat kutsupaikan omaa tietoa eivätkä tämän:
 *
 * - [color] oli annettava käsin siellä missä ruutu maalaa oman taustansa. Lautanäkymä on
 *   [Palette.PanelBg]in päällä eikä `Surface`in sisällä, joten periytyvä sisältöväri tulee
 *   teeman vaaleasta oletuksesta ja on lähes musta. Mitattu laitteella 10.8.2026:
 *   `Submit Move` epäonnistui yhteyteen, ja ruudulla näkyi vain `Refresh`, teksti oli
 *   paikallaan koko ajan mutta taustan värisenä. Vika on juuri sitä lajia jota logcat ei
 *   näe, koska mitään ei ole rikki (`docs/TESTAUS.md`). **Lukupinnan tulon (6.9.2026)
 *   jälkeen oletus on teeman `onBackground`** (13.9.2026), koska pinta on teeman taustaväriä
 *   ja teksti valitaan sen mukaan eikä kutsupaikan taustan mukaan: lautanäkymän pakotettu
 *   kerma katosi vaalean teeman pinnalle (mitattu tabletilta 13.9. ilta2). Parametri jää,
 *   koska se on yhä oikea siellä missä pinta ei ole teeman taustaa.
 * - [retryLabel] on eri silloin kun uudelleenyritys ei ole päivitys vaan paluu, esimerkiksi
 *   avatun ketjun sulkeminen.
 *
 * [onRetry] on `null` silloin kun käyttäjä ei voi tehdä mitään, eli nappia ei ole.
 */
@Composable
fun RetryNote(
    text: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = stringResource(R.string.top_refresh),
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        // Lukupinta vain tekstin ja napin alle eikä koko ruudulle (6.9.2026): huomautus on
        // lyhyt, ja loppu ruutu on juuri sitä tyhjää tilaa jonka kuvio saa täyttää.
        Column(
            modifier = Modifier.dgReadingSurface().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = color,
                textAlign = TextAlign.Center,
            )
            onRetry?.let {
                Button(onClick = it) { Text(retryLabel) }
            }
        }
    }
}

/**
 * Verkkovirhe tekstiksi, **yhdessä paikassa kolmentoista sijaan** (1.9.2026, auditoinnin H4).
 *
 * Sama kahden haaran muunnos oli inlinetty jokaiseen ruutuun joka näyttää [Failure]n. Malli
 * oli koko ajan samassa moduulissa valmiina: `NotABoardKind.text()` on täsmälleen tämä
 * jaettu laajennos, eikä `Failure` ollut koskaan saanut vastaavaa.
 *
 * Jos jokin ruutu tarvitsee eri sanamuodon, se kirjoitetaan kutsupaikkaan poikkeuksena eikä
 * neljäntenätoista kopiona. Yksi sellainen on olemassa: `BoardScreen`in vahvistamaton teko
 * kertoo mitä ei tiedetä (`board_unconfirmed_*`) eikä mikä epäonnistui.
 */
@Composable
fun Failure.text(): String = when (this) {
    Failure.Offline -> stringResource(R.string.error_offline)
    is Failure.Server -> stringResource(R.string.error_server, code)
    Failure.Sleeping -> stringResource(R.string.error_sleeping)
}

/**
 * Latauspalkin paikka, joka on varattu aina ja täytetty vain latauksen aikana.
 *
 * **Palkki ei saa siirtää sisältöä** (Tommin havainto 3.9.2026: *"näyttö nytkähtää
 * ylhäälle ilmestyvän latauspalkin toimesta"*). Yhdeksässä ruudussa palkki oli
 * `if (refreshing) LinearProgressIndicator()`, eli se ilmestyi sarakkeen alkuun ja työnsi
 * kaiken alapuolisen palkin korkeuden verran alas, ja katosi latauksen päättyessä toiseen
 * suuntaan. Kahden nytkähdyksen välissä käyttäjä oli usein juuri lukemassa tai
 * napauttamassa. Paikka on nyt aina sama korkeus (Material 3:n `LinearProgressIndicator`in
 * oletus 4 dp), joten palkki syttyy ja sammuu paikallaan.
 *
 * Korkeus on kirjoitettu tähän eikä luettu komponentilta, koska sitä ei ole julkisena
 * vakiona. Jos Material vaihtaa mittaa, paikka ja palkki eroavat yhden pikselin verran
 * eikä mitään nytkähdä.
 */
@Composable
fun ProgressSlot(active: Boolean, modifier: Modifier = Modifier) {
    // Lukupinta myös tähän (6.9.2026): paikka on otsakkeen ja listan välissä, ja ilman pintaa
    // se näkyi laitteella kapeana kuviokaistaleena kahden pinnan välissä.
    Box(modifier = modifier.fillMaxWidth().height(4.dp).dgReadingSurface()) {
        if (active) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
