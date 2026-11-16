package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Näytetäänkö laudalla sovelluksen omat lisälinkit `Mark position`, `Reminders` ja
 * `Cube reminder` (testaajan toive 9.10.2026: *"Possible to hide them for a cleaner
 * display?"*, Tommin kuittaus samana päivänä).
 *
 * **Oletus on päällä**, koska linkit ovat sovelluksen nykyinen käytös. Pois kytkeminen
 * piilottaa vain linkit: jo kirjoitetut muistutukset näkyvät laudan alla kuten ennen, ja
 * merkit ovat otteluluettelon takana.
 *
 * **Sivun napit `Swap Dice` ja `Skip Game` omilla kytkimillään 12.11.2026 alkaen** (Tommin
 * päätös, `docs/UI.md` › *Swap Dice ja Skip Game omille kytkimilleen*). Ne olivat siihen asti
 * aina näkyvissä, koska ne ovat sivun tekoja. Sama tiedosto, jotta asetusten siirto kattaa
 * ne, ja sama oletus: päällä.
 */
interface BoardExtrasStore {

    fun get(): Boolean

    fun save(shown: Boolean)

    fun swapDice(): Boolean = true

    fun saveSwapDice(shown: Boolean) {}

    fun skipGame(): Boolean = true

    fun saveSkipGame(shown: Boolean) {}
}

class SharedPrefsBoardExtras(private val prefs: SharedPreferences) : BoardExtrasStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, true)

    override fun save(shown: Boolean) {
        prefs.edit().putBoolean(KEY, shown).apply()
    }

    override fun swapDice(): Boolean = prefs.getBoolean(KEY_SWAP_DICE, true)

    override fun saveSwapDice(shown: Boolean) {
        prefs.edit().putBoolean(KEY_SWAP_DICE, shown).apply()
    }

    override fun skipGame(): Boolean = prefs.getBoolean(KEY_SKIP_GAME, true)

    override fun saveSkipGame(shown: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_GAME, shown).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_board_extras"
        const val KEY = "shown"
        const val KEY_SWAP_DICE = "swap_dice"
        const val KEY_SKIP_GAME = "skip_game"
    }
}
