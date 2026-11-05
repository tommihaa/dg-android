package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Eleet joita pelaaja voi kytkeä pois (Tommin tilaus 20.10.2026: *"kaikki session eleet
 * tulisi koota kytkimen alle kuten Game Sounds"*). Järjestys on asetusruudun järjestys.
 *
 * Nimi tallennetaan, ei järjestysnumeroa, jotta uuden eleen lisääminen väliin ei käännä
 * vanhaa valintaa toiseksi. Järjestelmän omat eleet (paluu reunalta, palkkien esiin
 * pyyhkäisy) eivät ole tässä, koska ne eivät ole sovelluksen.
 */
enum class Gesture {
    PULL_TO_REFRESH,
    SWIPE_TABS,
    TAP_TAB_TOP,
    HOLD_PLAYER_MENU,
    BUZZ,
}

/** Päällä olevat eleet pääkytkimen ja yksittäisten ruutujen jälkeen. Puhdas funktio. */
fun activeGestures(enabled: Boolean, off: Set<Gesture>): Set<Gesture> =
    if (enabled) Gesture.entries.toSet() - off else emptySet()

/** Tallennetut nimet eleiksi; tuntematon nimi (poistettu ele) jätetään pois. */
fun gesturesNamed(names: Set<String>): Set<Gesture> =
    names.mapNotNull { name -> Gesture.entries.firstOrNull { it.name == name } }.toSet()

/**
 * Eleiden pääkytkin ja pois kytketyt eleet, laitteen oma valinta. Samaa lajia kuin
 * `Game sounds` ja sen ruudut ([AppThemeStore.woodSound], [AppThemeStore.mutedSounds]).
 *
 * **Pääkytkimen oletus on pois** (Tommin päätös 20.10.2026: *"äänet ja värähdykset pitää käydä
 * laittamassa päälle, samalla kunnioitetaan olemassaolevien pelaajien rauhaa"*), kuudennen
 * arvon mukaisesti (`docs/ARVOT.md`). Ruudut ovat oletuksena ruksattuina kuten äänissä, joten
 * pääkytkin yksin ottaa kaikki käyttöön. Jokaisen eleen teon saa myös ruudulta.
 */
interface GestureStore {

    fun enabled(): Boolean

    fun saveEnabled(enabled: Boolean)

    fun off(): Set<String>

    fun saveOff(names: Set<String>)
}

class SharedPrefsGestures(private val prefs: SharedPreferences) : GestureStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun enabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    override fun saveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override fun off(): Set<String> = prefs.getStringSet(KEY_OFF, null)?.toSet().orEmpty()

    override fun saveOff(names: Set<String>) {
        prefs.edit().putStringSet(KEY_OFF, names).apply()
    }

    companion object {
        const val FILE_NAME = "dg_gestures"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_OFF = "off"
    }
}
