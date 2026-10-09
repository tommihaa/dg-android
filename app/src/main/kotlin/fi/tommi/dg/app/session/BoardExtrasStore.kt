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
 * merkit ovat otteluluettelon takana. `Skip Game` ja `Message` jäävät, koska ne ovat
 * sivun omia tekoja eivätkä sovelluksen lisiä.
 */
interface BoardExtrasStore {

    fun get(): Boolean

    fun save(shown: Boolean)
}

class SharedPrefsBoardExtras(private val prefs: SharedPreferences) : BoardExtrasStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, true)

    override fun save(shown: Boolean) {
        prefs.edit().putBoolean(KEY, shown).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_board_extras"
        const val KEY = "shown"
    }
}
