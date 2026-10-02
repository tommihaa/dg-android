package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Siirtonuolet laudalla kootusta siirrosta ennen `Submit Move`a, laitteen oma kytkin.
 *
 * **Tommin idea 29.9.2026** BGBlitzin ja XG:n kaappauksista: *"siitä olisi suuri hyöty
 * varsinkin kun pakkosiirto tai tuplat"*. Tuplan neljä askelta ja sovelluksen itse asettamat
 * nappulat ovat juuri ne siirrot joiden kulkua ei näe lopputilasta. Kuittaus samana päivänä:
 * nuoli jokaisesta kootusta askeleesta, ei vain tuplista.
 *
 * **Oletus on pois** (kuudes arvo, `docs/ARVOT.md`): oletusasetuksilla pelataan
 * perus-backgammonia, ja tekijän lisät ovat valittavia. Nuolet piirtyvät vain paikallisesta
 * kokoamisesta, koska vain siellä askeleet ovat tiedossa ennen lähetystä.
 *
 * **Vastustajan edellinen siirto on oma kytkimensä** (Tommin kuittaus 29.9.2026 illalla), koska
 * se maksaa lisähaun jokaista omaa vuoroa kohti (`OpponentMove.kt`) ja oma nuoli ei maksa
 * mitään. Sama tiedosto, joten asetusten siirto kattaa molemmat. Oletus on pois.
 */
interface MoveArrowsStore {

    fun get(): Boolean

    fun save(enabled: Boolean)

    fun opponent(): Boolean

    fun saveOpponent(enabled: Boolean)
}

class SharedPrefsMoveArrows(private val prefs: SharedPreferences) : MoveArrowsStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    override fun opponent(): Boolean = prefs.getBoolean(KEY_OPPONENT, false)

    override fun saveOpponent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OPPONENT, enabled).apply()
    }

    companion object {
        const val FILE_NAME = "dg_move_arrows"
        private const val KEY = "move_arrows"
        private const val KEY_OPPONENT = "opponent_arrows"
    }
}
