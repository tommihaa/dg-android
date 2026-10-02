package fi.tommi.dg.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity

/**
 * Pitää kirjoituskentän **ja sen lähetysnapin** näkyvissä näppäimistön yläpuolella.
 *
 * Kiinnitetään ryhmään, jossa kenttä ja nappi ovat (Tommin päätös 1.10.2026: kenttä ja sen
 * lähetysnappi pysyvät näkyvissä aina kun kirjoitetaan). Väistö itse on `MainActivity`n
 * NavHostissa (`imePadding`), ja se kutistaa ruudun näppäimistön yläpuolelle. Compose
 * vierittää fokusoidun kentän näkyviin itsestään, mutta vain kentän: mitattu 1.10.2026
 * tabletilla vaakana, foorumin Create New Thread jäi kentän alle näppäimistön taakse.
 *
 * Pyyntö toistetaan aina kun näppäimistön korkeus muuttuu, koska näppäimistö nousee
 * animaationa ja ensimmäinen pyyntö osuu vielä täyteen korkeuteen.
 */
/**
 * Dialogin kirjoituskentän fokus `MainActivity`lle, joka kääntää puhelimen pystyyn
 * kirjoittamisen ajaksi (Tommin päätös 1.10.2026).
 *
 * Tarvitaan, koska dialogi on oma ikkunansa: NavHostin `onFocusChanged` ei näe sen kenttää.
 * Mitattu 1.10.2026 Pixel 8a:lla vaakana, asetusten siirtodialogin Cancel ja Preview jäivät
 * näppäimistön alle. Oletus ei tee mitään, jotta esikatselut ja testit toimivat ilman
 * tarjoajaa.
 */
val LocalReportTyping = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

/**
 * Kertoo [LocalReportTyping]ille kun tämä kenttä saa tai menettää fokuksen. Purkautuessa
 * ilmoitetaan aina `false`, koska suljettu dialogi ei välttämättä ehdi menettää fokusta.
 */
@Composable
fun Modifier.reportsTyping(): Modifier {
    val report by rememberUpdatedState(LocalReportTyping.current)
    DisposableEffect(Unit) { onDispose { report(false) } }
    return onFocusChanged { report(it.isFocused) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepInViewWhileTyping(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, imeBottom) {
        if (focused && imeBottom > 0) requester.bringIntoView()
    }
    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { focused = it.hasFocus }
}
