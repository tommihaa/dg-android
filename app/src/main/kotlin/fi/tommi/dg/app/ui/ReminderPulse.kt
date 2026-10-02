package fi.tommi.dg.app.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Muistutuksen korostus: kuutiomuistutuksen kuutio ja lähettämätön asetuslomake.
 *
 * @property ring kehyksen ja hehkun voimakkuus 0..1.
 * @property heat silmän väri: 1 on punainen laser, 0 on purppura. Ks. [CubeRobotFace].
 * @property eyes silmien näkyvyys 1..0: 1 on laser, 0 on tavallinen reikä kuoressa.
 * @property grow kuution kasvu 0..1: kasvaa pulssien ajan ja jää ykköseen. Vain kuutio
 *   käyttää sitä, ks. [CUBE_GROWTH].
 */
@Immutable
internal data class ReminderAttention(val ring: Float, val heat: Float, val eyes: Float, val grow: Float = 0f)

/**
 * Muistutuksen korostuksen voimakkuus, kun muistutus on voimassa.
 *
 * **Yksi muoto kaikille odottaville teoille** (Tommin kirjaus 22.9.2026, valinta 24.9.2026:
 * *"purppura kehys jää"*). Syntyi kuutiomuistutukselle, ja `Update Preferences` sai saman
 * kuuden pulssin ja purppuran lepokehyksen oman jatkuvan alfasykkeensä tilalle
 * (`SettingsScreen.SubmitArea`). Sovelluksessa ei ole muita odottavasta teosta muistuttavia
 * animaatioita; muut toistuvat animaatiot ovat odotusilmaisimia ja loppuvat vastaukseen.
 * Asetusruutu käyttää vain [ReminderAttention.ring]ia, silmät ovat kuution omat.
 *
 * **Kuusi pulssia ja sen jälkeen pysyvä kehys, ei jatkuvaa vilkkumista** (määrä kaksinkertaistui
 * Tommin pyynnöstä 3.9.2026, kolme oli liian ohi ennen kuin katse ehti kuutioon). Rajaus itse on
 * mitattu: laudan asettelu on kerran tuottanut 294 värähtelevää ruutua viidessä sekunnissa
 * tyhjäkäynnillä (`docs/TESTAUS.md`), ja paikallaan olevan näkymän kuuluu piirtää nolla.
 * Pulssit ovat rajattu tapahtuma, kehys ei piirrä mitään sen jälkeen.
 *
 * **Silmä jäähtyy punaisesta purppuraan täsmälleen pulssien mitassa** (Tommin pyyntö 3.9.2026).
 * Punainen on huomioväri jota lauta käyttää jo kahteen muuhun asiaan, joten se ei kelpaa
 * pysyväksi tilaksi; hetkellisenä se kelpaa, koska katse on silloin juuri saapumassa eikä
 * lukemassa laudan värejä. Jäähtyminen loppuu samalla hetkellä kuin vilkkuminen, jotta ruutu
 * asettuu kerralla eikä kahdessa erässä.
 *
 * **Lepotila on purppura kehys, ei purppura silmä** (Tommin valinta 3.9.2026): pulssien jälkeen
 * silmät häivytetään takaisin tavallisiksi rei'iksi ja muistutus jää kehykseen. Silmät ovat siis
 * huomion herätys ja kehys on muistin paikka, eli sama jako kuin animoidun ja pysyvän välillä
 * muutenkin. Kehys on myös se osa joka näkyy numeropinnalla, jossa silmiä ei ole lainkaan.
 *
 * **Kuutio kasvaa pulssien ajan ja jää isoksi** (Tommin valinta 27.9.2026, *"kasvaa pulssien
 * ajan ja jää isoksi"*, samalla `Pick me` -lippu poistui pelaajakortista). Kasvu kulkee koko
 * pulssisarjan mitassa kuten jäähtyminenkin, joten ruutu asettuu kerralla. Iso koko on
 * lepotila eikä animaatio: se ei piirrä mitään pulssien jälkeen.
 *
 * **Animaatioasetus luetaan laitteelta.** `ANIMATOR_DURATION_SCALE` on nolla kun käyttäjä on
 * poistanut animaatiot käytöstä, ja silloin korostus tulee suoraan pysyvänä purppurana
 * kehyksenä ilman lasersilmiä. Se on esteettömyysasetus eikä makuasia: vilkkuminen ja
 * välähtävä punainen ovat juuri se osa jota se koskee.
 */
@Composable
internal fun reminderAttention(attention: Boolean): ReminderAttention {
    val context = LocalContext.current
    val animate = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val level = remember { Animatable(0f) }
    val heat = remember { Animatable(0f) }
    val eyes = remember { Animatable(0f) }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(attention, animate) {
        if (!attention) {
            level.snapTo(0f)
            heat.snapTo(0f)
            eyes.snapTo(0f)
            grow.snapTo(0f)
            return@LaunchedEffect
        }
        if (animate) {
            heat.snapTo(1f)
            eyes.snapTo(1f)
            // Jäähtyminen kestää koko pulssisarjan, eli se lasketaan samoista luvuista kuin
            // sarja itse eikä omasta vakiostaan: muuten kahden luvun muuttaminen erikseen
            // päästäisi värin asettumaan ennen vilkkumista tai sen jälkeen.
            launch { heat.animateTo(0f, tween(durationMillis = CUBE_PULSES * 2 * CUBE_PULSE_MS)) }
            launch { grow.animateTo(1f, tween(durationMillis = CUBE_PULSES * 2 * CUBE_PULSE_MS)) }
            repeat(CUBE_PULSES) {
                level.animateTo(1f, tween(durationMillis = CUBE_PULSE_MS))
                level.animateTo(0.35f, tween(durationMillis = CUBE_PULSE_MS))
            }
            eyes.animateTo(0f, tween(durationMillis = CUBE_PULSE_MS))
        }
        level.snapTo(0.35f)
        heat.snapTo(0f)
        eyes.snapTo(0f)
        grow.snapTo(1f)
    }
    return ReminderAttention(level.value, heat.value, eyes.value, grow.value)
}

/**
 * Kuution koko muistutuksen lepotilassa suhteessa tavalliseen, ks. [reminderAttention].
 * Piirretään `graphicsLayer`in skaalauksena eikä koon muutoksena, joten laudan asettelu ei
 * liiku eikä mittaa uudelleen: kuutio kasvaa paikallaan oman paikkansa yli.
 */
internal const val CUBE_GROWTH = 1.35f

/** Kuutiomuistutuksen pulssien määrä, ks. [reminderAttention]. */
private const val CUBE_PULSES = 6

/** Yhden pulssin puolikkaan kesto millisekunteina, ks. [reminderAttention]. */
private const val CUBE_PULSE_MS = 400
