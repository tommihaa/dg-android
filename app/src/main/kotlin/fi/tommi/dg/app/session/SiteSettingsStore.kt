package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.SiteBoardSettings

/**
 * Laitteelle säilötty kopio sivuston lautaan vaikuttavista asetuksista.
 *
 * **Tämä on olemassa siksi että lautanäkymä tarvitsee tiedon jota se ei itse hae**
 * (`docs/ASETUKSET.md` luku 2): asetukset luetaan asetussivulta, ei laudalta pääteltynä,
 * ja lauta lukee tätä säilöä. Kirjoittaja on jokainen onnistunut asetussivun luku, eli
 * avaushaku ja asetusruutu; epäonnistunut haku ei kirjoita, joten vanha lukema säilyy.
 * Vanha lukema on vanhentunutta mutta oikeaa tietoa, sama muoto kuin näkymämallien
 * `refreshing`-lipulla.
 *
 * Muoto on sama kuin [SettingsBaseline]lla ja samasta syystä: pieni rajapinta ja
 * injektoitava `prefs`, ei kantataulua kolmelle kentälle.
 */
interface SiteSettingsStore {

    fun get(): SiteBoardSettings

    /**
     * Korvaa säilön kokonaan. Null-kenttä tallentuu nullina eikä säilytä vanhaa arvoa:
     * onnistunut luku on koko totuus siitä mitä sivu sanoi, ja selitteensä menettänyt
     * asetus on silloin tuntematon eikä entinen.
     */
    fun save(settings: SiteBoardSettings)
}

class SharedPrefsSiteSettings(private val prefs: SharedPreferences) : SiteSettingsStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): SiteBoardSettings =
        SiteBoardSettings(
            hidePips = readTriState(KEY_HIDE_PIPS),
            homeBoardsLeft = readTriState(KEY_HOME_LEFT),
            scheme = prefs.getString(KEY_SCHEME, null)?.let { stored ->
                // Tuntematon nimi on null eikä kaato: arvo on kirjoitettu tällä sovelluksella,
                // mutta vanhentunut enum-nimi version vaihtuessa ei saa estää käynnistystä.
                BoardScheme.entries.firstOrNull { it.name == stored }
            },
        )

    override fun save(settings: SiteBoardSettings) {
        prefs.edit()
            .writeTriState(KEY_HIDE_PIPS, settings.hidePips)
            .writeTriState(KEY_HOME_LEFT, settings.homeBoardsLeft)
            .apply {
                val scheme = settings.scheme
                if (scheme == null) remove(KEY_SCHEME) else putString(KEY_SCHEME, scheme.name)
            }
            .apply()
    }

    /** Kolmiarvoinen totuusarvo: puuttuva avain on null, ei false. */
    private fun readTriState(key: String): Boolean? =
        if (prefs.contains(key)) prefs.getBoolean(key, false) else null

    private fun SharedPreferences.Editor.writeTriState(
        key: String,
        value: Boolean?,
    ): SharedPreferences.Editor =
        if (value == null) remove(key) else putBoolean(key, value)

    private companion object {
        const val FILE_NAME = "dg_site_settings"
        const val KEY_HIDE_PIPS = "hide_pips"
        const val KEY_HOME_LEFT = "home_boards_left"
        const val KEY_SCHEME = "board_scheme"
    }
}
