package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import fi.tommi.dg.domain.SiteBoardSettings

/**
 * Laitteen oma kytkin `Confirm Beaver`: kysyykö dialogi ennen kuin money gamen `Beaver!` tai
 * `Accept Beaver` lähtee.
 *
 * **Tommin tilaus 27.9.2026** money gamen jälkeen. Sivun vahvistusasetukset eivät kata beaveria
 * (mitattu `sessio-27-9-money-peli/0079`: ruudut vain `Verify Accept` ja `Verify Decline`), joten
 * ilman kytkintä `Beaver!` lähtee yhdellä napautuksella. Ensin tehtiin kolme kytkintä (Accept,
 * Beaver, Decline), ja Tommi karsi kaksi samana aamuna: *"sovelluksen omaksi asetukseksi
 * tarvitaan vain confirm beaver - poista ne kaksi muuta, dailygammonin asetukset hoitaa ne"*.
 *
 * **Oletus kopioidaan sivun `Confirmation on offering doubles` -asetuksesta** (Tommin vastaus:
 * *"kopioi oletukseksi dailygammon asetukset double-vastaavuuksille"*), koska beaver on tuplaus.
 * Oletus elää sivun mukana niin kauan kuin pelaaja ei ole koskenut kytkimeen; kosketus tallentaa
 * laitteen oman arvon, ja se voittaa sen jälkeen. Tuntematon sivun asetus on pois.
 */
interface BeaverConfirmStore {

    /** Pelaajan oma valinta, tai null kun kytkimeen ei ole koskettu. */
    fun override(): Boolean?

    fun save(enabled: Boolean)

    /** Voimassa oleva arvo: pelaajan valinta, muuten sivun tuplausvahvistus, muuten pois. */
    fun effective(site: SiteBoardSettings): Boolean = override() ?: site.confirmDouble ?: false
}

class SharedPrefsBeaverConfirm(private val prefs: SharedPreferences) : BeaverConfirmStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun override(): Boolean? =
        if (prefs.contains(KEY)) prefs.getBoolean(KEY, false) else null

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_beaver_confirm"
        const val KEY = "confirm_beaver"
    }
}
