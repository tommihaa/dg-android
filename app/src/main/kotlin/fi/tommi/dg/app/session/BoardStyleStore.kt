package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Laudan tyyli, sovelluksen oma paikallinen valinta (`docs/ASETUKSET.md` luku 3).
 *
 * Tämä ei ole sivuston asetus eikä lähde koskaan verkkoon, ja juuri siksi se asuu omassa
 * tyypissään eikä [SiteSettingsStore]n seassa: toinen on kopio sivustosta, toinen on tämän
 * laitteen oma maku, ja sekoitettuina ruudulla ei näkyisi kumpi lähtee verkkoon.
 */
enum class BoardStyle {
    /**
     * Sivustouskollinen: lauta noudattaa pelaajan DailyGammon-asetuksia. Oletus, koska se
     * on Tommin päätöksen sisältö (24.8.2026): uusi käyttäjä näkee tutun laudan eikä
     * toisen pelaajan makua.
     */
    SITE,

    /** Monte Carlo X-22, sovelluksen oma ulkoasu roolivärein. Tommin oma valinta. */
    X22,

    /**
     * Monte Carlo variant (13.9.2026): vihreä huopa, oranssi ja kerma kiila, valkoinen ja
     * musta nappula roolivärein. Malli Gammon Geaux 2026 -finaalin lähetyslaudasta, nimi on
     * Tommin, koska valmistajaa ei todennettu. Ainoa tyyli joka vaihtaa huovan ja kehyksen
     * värin (`docs/ASETUKSET.md` luku 4, tarkennus 13.9.2026).
     */
    MONTE_CARLO_VARIANT,
}

interface BoardStyleStore {

    fun get(): BoardStyle

    fun save(style: BoardStyle)
}

class SharedPrefsBoardStyle(private val prefs: SharedPreferences) : BoardStyleStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): BoardStyle {
        val stored = prefs.getString(KEY_STYLE, null) ?: return BoardStyle.SITE
        // Tuntematon nimi on oletus eikä kaato, sama peruste kuin skeeman luvussa.
        return BoardStyle.entries.firstOrNull { it.name == stored } ?: BoardStyle.SITE
    }

    override fun save(style: BoardStyle) {
        prefs.edit().putString(KEY_STYLE, style.name).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_board_style"
        const val KEY_STYLE = "board_style"
    }
}
