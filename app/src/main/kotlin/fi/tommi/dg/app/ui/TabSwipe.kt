package fi.tommi.dg.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.session.Gesture
import kotlin.math.abs

/**
 * Välilehti johon sivuttaispyyhkäisy vie (Tommin päätökset 20.10.2026, `docs/UI.md`).
 *
 * [step] on +1 kun sormi kulkee vasemmalle ja oikealla oleva välilehti tulee esiin, -1
 * vastakkaiseen suuntaan. **Päädyt kiertävät**: Infosta eteenpäin on Matches ja Matchesista
 * taaksepäin Info. Matchesin kaksi osaa ohitetaan, eli ele vaihtaa aina koko välilehteä.
 * Puhdas funktio, jotta järjestys on testattavissa ilman Composea.
 */
fun swipeTarget(from: DgTab, step: Int): DgTab {
    val all = DgTab.entries
    return all[Math.floorMod(from.ordinal + step, all.size)]
}

/**
 * Sivuttaispyyhkäisy vaihtaa välilehteä. Kutsuja antaa tämän vain silloin kun välilehtirivi
 * näkyy, joten laudalla, asetuksissa, merkityissä asemissa ja porautumisruudulla elettä ei ole.
 *
 * **Kuunnellaan `Main`-vaiheessa lasten jälkeen.** Sisältö saa liikkeen ensin: Forumin
 * kuukausirivi vierittää itseään ja lista vierii pystyyn, ja silloin muutos on kulutettu eikä
 * tämä ele laukea. Vasta kun yksikään lapsi ei ottanut sivuttaista liikettä, se on tämän.
 *
 * **Liike kulutetaan heti kun se on tunnistettu sivuttaiseksi.** Ilman sitä rivin
 * napautus laukeaisi sormen noustessa, koska koko levyinen rivi pitää sormen rajojensa
 * sisällä. Otteluriville se olisi avannut ottelun ja kuluttanut jonoa.
 *
 * Kynnys on 72 dp matkaa, sama kuin laudan vedossa, ja vaihto tapahtuu **kerran per ele**.
 * Järjestelmän takaisin-ele lähtee ruudun reunasta, eikä sen liike tule tänne lainkaan.
 */
fun Modifier.swipeBetweenTabs(onSwipe: (step: Int) -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onSwipe)
    val buzz = rememberGestureBuzz()
    pointerInput(Unit) {
        val slop = viewConfiguration.touchSlop
        val threshold = 72.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            var dx = 0f
            var dy = 0f
            var horizontal = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                // Lapsi otti liikkeen (vieritys), joten ele on sen eikä tämän.
                if (!horizontal && change.isConsumed) break
                val delta = change.positionChange()
                dx += delta.x
                dy += delta.y
                if (!horizontal) {
                    if (abs(dy) > slop && abs(dy) >= abs(dx)) break
                    if (abs(dx) > slop && abs(dx) > abs(dy) * 2) horizontal = true
                }
                if (horizontal) {
                    event.changes.forEach { it.consume() }
                    if (abs(dx) >= threshold) {
                        // Värähdys kertoo että ele rekisteröityi (20.10.2026).
                        buzz()
                        latest(if (dx < 0) 1 else -1)
                        // Loppu eleestä kulutetaan, jotta irrotus ei napauta mitään.
                        do {
                            val rest = awaitPointerEvent(PointerEventPass.Main)
                            rest.changes.forEach { it.consume() }
                        } while (rest.changes.any { it.pressed })
                        break
                    }
                }
            }
        }
    }
}

/**
 * Valitun välilehden uusi napautus vie sen listan alkuun (Tommin tilaus 20.10.2026). Arvo
 * kasvaa joka napautuksella, ja jokainen näkyvä lista reagoi vain muutokseen. Arvo ei ole
 * tapahtuma vaan laskuri, jotta myöhemmin piirtyvä lista ei vieritä itseään avautuessaan.
 */
val LocalScrollToTop = compositionLocalOf { 0 }

/** Kutsuu [scroll]ia kun valittua välilehteä napautetaan uudestaan, ks. [LocalScrollToTop]. */
@Composable
fun ScrollToTopOnTabTap(scroll: suspend () -> Unit) {
    val tick = LocalScrollToTop.current
    val seen = remember { mutableIntStateOf(tick) }
    LaunchedEffect(tick) {
        if (tick != seen.intValue) {
            seen.intValue = tick
            scroll()
        }
    }
}

/**
 * Päällä olevat eleet (`GestureStore`, Tommin tilaus 20.10.2026). Jokainen ele lukee itse
 * onko se päällä, jotta kytkin ei tarvitse yhtäkään ehtoa ruutujen parametreihin. Oletus on
 * tyhjä, sama kuin pääkytkimen oletus.
 */
val LocalGestures = compositionLocalOf { emptySet<Gesture>() }

/** Värähdys kun ele ylittää kynnyksen, tai ei mitään kun `Buzz` on pois. */
@Composable
fun rememberGestureBuzz(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val on = Gesture.BUZZ in LocalGestures.current
    return remember(haptic, on) {
        if (on) { { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) } } else { {} }
    }
}
