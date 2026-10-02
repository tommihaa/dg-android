package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Sovelluksen teema: väri- ja artefaktimaailma yhtenä valintana (Tommin tilaus 22.9.2026:
 * *"dg android teemoja, selkeämpiä kokonaisuuksia väri- ja artefaktimaailmoista"*).
 *
 * Teema kattaa ensimmäisessä versiossa kolme palaa (Tommin kuittaus samana iltana):
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
}

interface AppThemeStore {

    fun get(): AppTheme

    fun save(theme: AppTheme)

    /** Lauta ja ilmaisimen viimeistely ennen teemaan siirtymistä, jotta [AppTheme.PLAIN] palauttaa ne. */
    fun saveBefore(board: BoardStyle, busyDeco: Boolean)

    fun before(): Pair<BoardStyle, Boolean>
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

    private companion object {
        const val FILE_NAME = "dg_app_theme"
        const val KEY = "app_theme"
        const val KEY_BOARD = "board_before"
        const val KEY_BUSY_DECO = "busy_deco_before"
    }
}
