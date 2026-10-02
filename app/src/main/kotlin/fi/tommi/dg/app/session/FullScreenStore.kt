package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Piilotetaanko tilapalkki ja navigointipalkki koko sovelluksesta, laitteen oma kytkin
 * (Tommin tilaus 30.9.2026: *"heille jotka haluaa pelata full screen ilman mobiililaitteiden
 * palkkeja"*). Palkit saa hetkeksi näkyviin pyyhkäisemällä reunasta, ks. `Immersive.kt`.
 *
 * **Oletus on pois**, ja silloin mikään ei muutu: lautaruutu piilottaa palkit yhä vain kun ne
 * maksaisivat laudan kokoa (`DgBoard.barsAreFree`, 20.8.2026). Päällä piilotus koskee kaikkia
 * ruutuja, myös lukuruutuja joilla se 8.8.2026 rajattiin pois. Tommi valitsi laajuuden
 * monivalinnasta 30.9.2026. Sama laji kuin [PortraitLockStore]: ei lähde koskaan verkkoon.
 */
interface FullScreenStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsFullScreen(private val prefs: SharedPreferences) : FullScreenStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_full_screen"
        const val KEY = "full_screen"
    }
}
