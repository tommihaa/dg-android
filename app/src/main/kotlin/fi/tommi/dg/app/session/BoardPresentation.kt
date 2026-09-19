package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Noppien ja pisteiden esitys, kaksi paikallista valintaa (`docs/ASETUKSET.md` luku 3).
 *
 * **Omia kytkimiä eikä [BoardStyle]en sidottuja, ja se on Tommin päätös 27.8.2026.**
 * Vaihtoehto jossa lautatila olisi määrännyt myös nämä punnittiin ja hylättiin: X-22-laudalla
 * on voitava pitää sivuston noppaesitys. Sama laji kuin [BoardStyleStore]: laitteen oma maku,
 * ei lähde koskaan verkkoon.
 *
 * **Oletukset ovat nykyinen käytös eivätkä sivuston**, toisin kuin [BoardStyle]lla, ja ero on
 * tarkoituksellinen: kumpikin kytkin syntyi havainnosta eikä viasta, joten oletuksen
 * vaihtaminen olisi eri päätös jota ei ole kysytty.
 */
enum class DiceStyle {
    /**
     * Yksi kuutio jokaista siirtoa kohti: tupla laajenee neljäksi. Kuutiot vähenivät
     * siirrettäessä 2.9.2026 asti; nyt pelattu kuutio jää paikalleen harmaana (Tommin
     * tilaus, `Die.spent`). Sovelluksen alkuperäinen esitys ja oletus.
     */
    COUNTER,

    /**
     * Tasan kaksi kuutiota jotka eivät vähene kokoamisen aikana. Sivuston mitattu käytös
     * (27.8.2026, 2698 kaapattua lautasivua ja jokaisella kaksi noppakuvaa, myös tuplan
     * kokoamisen keskellä). Harmaus on lisä sivuston esitykseen: pelattu kuutio himmenee,
     * tuplassa puolikas kerrallaan.
     */
    SITE,
}

enum class ScoreStyle {
    /** `12-away`, puuttuvat pisteet. Sovelluksen esitys 21.8.2026 alkaen ja oletus. */
    AWAY,

    /**
     * Sivun oma pistekenttä sellaisenaan. Rahapelissä away ei ole olemassa, jolloin
     * molemmat arvot näyttävät tämän; se oli tähänkin asti varareitti.
     */
    SITE,
}

/**
 * Odotuksen ilmaisin nappien paikalla (Tommin tilaus 18.9.2026: *"haluaisin busy-style
 * asetuksen sovellukseen"*). Kolme muotoa `BoardScreen.kt`:ssä: `BusyArc`, `BusyOuroboros`
 * ja `BusyCube`. Sama laji kuin [DiceStyle]: laitteen oma maku, ei lähde koskaan verkkoon.
 * Oletus on kaari, koska se on nykyinen käytös eikä valintaa ole vielä tehty.
 */
enum class BusyStyle {
    /** Pyörivä kaari laudan väreillä (16.9.2026). Oletus. */
    ARC,

    /** Häntäänsä syövä käärme kiertää kehää, kiilan kahdella värillä. */
    OUROBOROS,

    /** Vierivä tuplauskuutio: robotti ensin (64:n paikalla), sitten 2, 4, 8, 16 ja 32. */
    CUBE,
}

interface BusyStyleStore {

    fun get(): BusyStyle

    fun save(style: BusyStyle)
}

interface DiceStyleStore {

    fun get(): DiceStyle

    fun save(style: DiceStyle)
}

interface ScoreStyleStore {

    fun get(): ScoreStyle

    fun save(style: ScoreStyle)
}

class SharedPrefsDiceStyle(private val prefs: SharedPreferences) : DiceStyleStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): DiceStyle {
        val stored = prefs.getString(KEY, null) ?: return DiceStyle.COUNTER
        // Tuntematon nimi on oletus eikä kaato, sama peruste kuin lautatilan luvussa.
        return DiceStyle.entries.firstOrNull { it.name == stored } ?: DiceStyle.COUNTER
    }

    override fun save(style: DiceStyle) {
        prefs.edit().putString(KEY, style.name).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_dice_style"
        const val KEY = "dice_style"
    }
}

class SharedPrefsBusyStyle(private val prefs: SharedPreferences) : BusyStyleStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): BusyStyle {
        val stored = prefs.getString(KEY, null) ?: return BusyStyle.ARC
        return BusyStyle.entries.firstOrNull { it.name == stored } ?: BusyStyle.ARC
    }

    override fun save(style: BusyStyle) {
        prefs.edit().putString(KEY, style.name).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_busy_style"
        const val KEY = "busy_style"
    }
}

class SharedPrefsScoreStyle(private val prefs: SharedPreferences) : ScoreStyleStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): ScoreStyle {
        val stored = prefs.getString(KEY, null) ?: return ScoreStyle.AWAY
        return ScoreStyle.entries.firstOrNull { it.name == stored } ?: ScoreStyle.AWAY
    }

    override fun save(style: ScoreStyle) {
        prefs.edit().putString(KEY, style.name).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_score_style"
        const val KEY = "score_style"
    }
}
