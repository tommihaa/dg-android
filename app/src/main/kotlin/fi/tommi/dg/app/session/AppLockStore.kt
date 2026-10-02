package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Kysytäänkö sormenjälki, kasvot tai laitteen PIN sovelluksen käynnistyessä, laitteen oma
 * kytkin (`AppLock.kt`).
 *
 * **Oletus on pois** (Tommin päätös 27.9.2026). Lukko oli käytössä 21.8.2026 asti ja sitten
 * vakiolipun takana pois, koska se vaikeutti testausta (*"se on vaan tiellä"*). Tommi halusi
 * sille oman asetuksen, ja oletus seuraa kuudetta arvoa: oletuksilla perus, lisät valittavina.
 * Sama laji kuin [PortraitLockStore]: ei lähde koskaan verkkoon.
 */
interface AppLockStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsAppLock(private val prefs: SharedPreferences) : AppLockStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_app_lock"
        const val KEY = "app_lock"
    }
}
