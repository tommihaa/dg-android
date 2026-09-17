package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Taustakuvion väritila, laitteen oma kytkin (`docs/ASETUKSET.md` luku 3).
 *
 * **Tommin päätös 6.9.2026** viidestä kuvaksi piirretystä kandidaatista
 * (`tyokalut/taustaluonnos.py`): voimakas yksivärinen kuvio on oletus, ja sumi-e-tila
 * (muste, vermilionin väriset linnut ja kuu taivaalla) on asetus. **Oletus on pois**, koska
 * Tommin oma sana samana iltana oli *"värilliset tessellaatiot app-asetus"*: väri on valinta
 * eikä lähtötila. Sama laji kuin [PortraitLockStore]: ei lähde koskaan verkkoon.
 *
 * **Toinen avain 6.9.2026: kuvio päällä vai pois** ([pattern]), molemmissa teemoissa.
 * Tommin tilaus samana iltana: *"vie koodiin ja sovelluksen omana asetuksena haluaako
 * yksivärisen taustan vai näitä"*, ja heti perään oletus: *"oletuksena taustakuva vaalealla
 * ja tummalla taustalla pois päältä."* **Oletus on siis pois**, ja se koskee myös vaaleaa
 * teemaa, jossa kuvio oli 6.9. päivästä iltaan asti aina. Sumi-e on tämän alakytkin: se
 * vaikuttaa vain kun kuvio on päällä ja teema vaalea.
 */
interface SkyThemeStore {

    fun get(): Boolean

    fun save(enabled: Boolean)

    fun pattern(): Boolean

    fun savePattern(enabled: Boolean)
}

class SharedPrefsSkyTheme(private val prefs: SharedPreferences) : SkyThemeStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    override fun pattern(): Boolean = prefs.getBoolean(KEY_PATTERN, false)

    override fun savePattern(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PATTERN, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_sky_theme"
        const val KEY = "sumi_e"
        const val KEY_PATTERN = "pattern"
    }
}
