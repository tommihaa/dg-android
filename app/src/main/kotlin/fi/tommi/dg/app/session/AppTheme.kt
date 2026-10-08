package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Sovelluksen teema: väri- ja artefaktimaailma yhtenä valintana (Tommin tilaus 22.9.2026:
 * *"dg android teemoja, selkeämpiä kokonaisuuksia väri- ja artefaktimaailmoista"*).
 *
 * Teema kattaa ensimmäisessä versiossa kolme palaa (Tommin kuittaus samana iltana), ja
 * Wood-teema 4.10.2026 alkaen neljännen eli äänen:
 * sovelluksen värit ja kirjasimen, laudan ([BoardStyle.DECO]) ja odotuksen ilmaisimen
 * (deco-viimeistely). Laudan ja ilmaisimen valinta jää omiksi asetuksikseen hienosäätöä
 * varten; teeman vaihto vain kirjoittaa ne kerralla. Oletus on [PLAIN], eli perus-backgammon
 * kuudennen arvon mukaisesti (`docs/ARVOT.md`).
 */
enum class AppTheme {
    /** Sovelluksen omat värit, laite päättää vaalean ja tumman. Oletus. */
    PLAIN,

    /** Musta emali, kulta ja pienet rattaat; aina tumma. */
    DECO,

    /**
     * Puu (Tommin tilaus 4.10.2026: *"wood-paneleille oman teeman, missä äänet ovat mukana kun
     * nopat kolahtaa puuhun"*). Hunajainen vaahtera tai tumma pähkinä laitteen tilan mukaan,
     * serif-otsikot ja kolahdus puuhun. Toimii kaikilla neljällä puulaudalla: valittu puulauta
     * säilyy, ja muu lauta vaihtuu pähkinäksi ([BoardStyle.WALNUT]). Ääni on tekijän lisä ja
     * sillä on oma kytkin ([AppThemeStore.woodSound]), `docs/ASETUKSET.md` › Wood-teema.
     */
    WOOD,
}

/** Puulaudat, joilla Wood-teema pitää valitun laudan (Tommin valinta 4.10.2026). */
val BoardStyle.isWood: Boolean
    get() = this == BoardStyle.MAPLE || this == BoardStyle.WALNUT ||
        this == BoardStyle.OAK_LEATHER || this == BoardStyle.OLIVE

/**
 * Laudan ja ilmaisimen viimeistely kun [theme] valitaan, lähtien siitä mitä pelaajalla on nyt.
 * Paluu Plainiin on [leave]. Puhdas funktio, jotta siirtymät ovat testattavissa ilman ruutua.
 */
fun enter(theme: AppTheme, board: BoardStyle, busyDeco: Boolean): Pair<BoardStyle, Boolean> = when (theme) {
    AppTheme.PLAIN -> board to busyDeco
    AppTheme.DECO -> BoardStyle.DECO to true
    // Kultaiset rattaat eivät kuulu puuhun, joten ilmaisin piirtyy laudan omissa väreissä.
    AppTheme.WOOD -> (if (board.isWood) board else BoardStyle.WALNUT) to false
}

/**
 * Teemasta poistuminen: lauta ja viimeistely jotka olivat ennen teemaa ([before]). Teeman
 * oma lauta palautuu, mutta jos pelaaja on sillä välin itse vaihtanut laudan teeman ulkopuolelle,
 * hänen valintansa jää.
 */
fun leave(
    theme: AppTheme,
    board: BoardStyle,
    busyDeco: Boolean,
    before: Pair<BoardStyle, Boolean>,
): Pair<BoardStyle, Boolean> = when (theme) {
    AppTheme.PLAIN -> board to busyDeco
    AppTheme.DECO -> (if (board == BoardStyle.DECO) before.first else board) to before.second
    // Puulauta jonka teema itse asetti palautuu; pelaajan oma puulauta ennen teemaa on [before].
    AppTheme.WOOD -> (if (board.isWood) before.first else board) to before.second
}

interface AppThemeStore {

    fun get(): AppTheme

    fun save(theme: AppTheme)

    /** Lauta ja ilmaisimen viimeistely ennen teemaan siirtymistä, jotta [AppTheme.PLAIN] palauttaa ne. */
    fun saveBefore(board: BoardStyle, busyDeco: Boolean)

    fun before(): Pair<BoardStyle, Boolean>

    /**
     * Soivatko pelin äänet (kytkin `Game sounds`). Oletus on pois kaikissa teemoissa, myös
     * Woodissa (Tommin valinta 6.10.2026), koska äänet irtosivat teemasta ja kuudennen arvon
     * mukaan oletuksilla sovellus on äänetön. Asuu teeman tiedostossa nimellä `wood_sound`,
     * jotta aiempi valinta säilyy ja kulkee asetusten siirrossa.
     */
    fun woodSound(): Boolean

    fun saveWoodSound(enabled: Boolean)
}

class SharedPrefsAppTheme(private val prefs: SharedPreferences) : AppThemeStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): AppTheme {
        val stored = prefs.getString(KEY, null) ?: return AppTheme.PLAIN
        return AppTheme.entries.firstOrNull { it.name == stored } ?: AppTheme.PLAIN
    }

    override fun save(theme: AppTheme) {
        prefs.edit().putString(KEY, theme.name).apply()
    }

    override fun saveBefore(board: BoardStyle, busyDeco: Boolean) {
        prefs.edit().putString(KEY_BOARD, board.name).putBoolean(KEY_BUSY_DECO, busyDeco).apply()
    }

    override fun before(): Pair<BoardStyle, Boolean> {
        val board = BoardStyle.entries.firstOrNull { it.name == prefs.getString(KEY_BOARD, null) }
            ?: BoardStyle.SITE
        return board to prefs.getBoolean(KEY_BUSY_DECO, false)
    }

    override fun woodSound(): Boolean = prefs.getBoolean(KEY_WOOD_SOUND, false)

    override fun saveWoodSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WOOD_SOUND, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_app_theme"
        const val KEY = "app_theme"
        const val KEY_BOARD = "board_before"
        const val KEY_BUSY_DECO = "busy_deco_before"
        const val KEY_WOOD_SOUND = "wood_sound"
    }
}
