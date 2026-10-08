package fi.tommi.dg.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fi.tommi.dg.app.R
import fi.tommi.dg.domain.BoardState

/**
 * Kolahdus puuhun (Tommin tilaus 4.10.2026: *"äänet ovat mukana kun nopat kolahtaa puuhun"*).
 * Kolme tapahtumaa Tommin valinnan mukaan: nopan laskeutuminen, nappulan siirto ja kuution
 * tuplaus. Neljäs on ohi-napautus pisteeseen jolta ei voi siirtää (6.10.2026), ja se soi vain
 * painalluskytkimen ollessa päällä. Äänet on syntetisoitu (`tyokalut/puuaanet.py`), joten
 * lisenssiä ei ole.
 *
 * **Ääni on poikkeus äänettömyyslinjaan** (kaanoni 4.10.2026, laajennettu 6.10.2026,
 * `docs/AVOIMET.md`) ja siksi sen rajat ovat tässä yhdessä paikassa: soi vain kun kutsuja antaa
 * luvan (kytkin `Game sounds`, oletus pois, mikä tahansa teema), laitteen äänetön tai
 * värinätila voittaa, ja TalkBackin ollessa päällä ääni vaikenee, jotta se ei peitä puhetta.
 */
internal enum class WoodSound(val res: Int) {
    DICE(R.raw.wood_dice),
    CHECKER(R.raw.wood_checker),
    CUBE(R.raw.wood_cube),
    MISS(R.raw.wood_miss),
}

/**
 * Mikä ääni soi kun lauta vaihtuu [prev]istä [next]iin, tai null. [prevSteps] ja [nextSteps]
 * ovat paikallisesti koottujen askelten määrät (`CompositionSession.steps`): oma siirto kootaan
 * sovelluksessa eikä se muuta lautaa ennen lähetystä, joten nappulan askel näkyy vain niistä
 * (laitteella 5.10.2026, Tommi: *"nyt ei kuulunut mitään"*). Peruminen on hiljainen. Puhdas funktio, jotta sääntö
 * on testattavissa ilman laitetta.
 *
 * Ensimmäinen lauta ja toisen ottelun lauta ovat hiljaisia, koska niissä mikään ei liikkunut
 * pelaajan silmien edessä: ottelun avaus näyttää valmiin tilanteen. Uudet nopat voittavat
 * nappulat, koska vuoron vaihtuessa molemmat voivat muuttua samalla sivulla ja yksi ääni
 * kerrallaan riittää. Kuutio ei ole tässä vaan napin painalluksessa ([WOOD_CUBE_SUBMITS]).
 */
internal fun woodSoundFor(
    prev: BoardState?,
    next: BoardState,
    prevSteps: Int = 0,
    nextSteps: Int = 0,
): WoodSound? {
    if (prev == null || prev.matchId != next.matchId) return null
    // Käytetty osa (`spent`) muuttuu jokaisella askeleella, joten se ei ole uusi heitto.
    fun BoardState.roll() = dice.map { it.value to it.owner }
    fun BoardState.checkers() = Triple(points, bar, borneOff)
    return when {
        next.dice.isNotEmpty() && next.roll() != prev.roll() -> WoodSound.DICE
        next.checkers() != prev.checkers() -> WoodSound.CHECKER
        nextSteps > prevSteps -> WoodSound.CHECKER
        else -> null
    }
}

/** Soitin yhdelle ruudulle; vapautetaan kun ruutu poistuu. */
internal class WoodSoundPlayer(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val accessibility = context.getSystemService(AccessibilityManager::class.java)
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = WoodSound.entries.associateWith { pool.load(context, it.res, 1) }

    fun play(sound: WoodSound) {
        // Pelin ääni kulkee mediavoimakkuudella, jota äänetön tila ei vaimenna, joten tila
        // luetaan itse: vain normaali soittoäänitila päästää kolahduksen läpi.
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        if (accessibility?.isTouchExplorationEnabled == true) return
        ids[sound]?.let { pool.play(it, VOLUME, VOLUME, 1, 0, 1f) }
    }

    fun release() = pool.release()

    private companion object {
        /** Kolahdus on taustaa eikä ilmoitus, joten se soi alle täyden voimakkuuden. */
        const val VOLUME = 0.7f
    }
}

/** Soitin kun [enabled], muuten null, jolloin ruutu on äänetön eikä lataa mitään. */
@Composable
internal fun rememberWoodSounds(enabled: Boolean): WoodSoundPlayer? {
    val context = LocalContext.current
    val player = remember(enabled) { if (enabled) WoodSoundPlayer(context.applicationContext) else null }
    DisposableEffect(player) { onDispose { player?.release() } }
    return player
}
