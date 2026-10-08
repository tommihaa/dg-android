package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Painetun pisteen numero syttyy laudan reunassa, laitteen oma kytkin.
 *
 * **Tommin tavoite 4.10.2026:** *"pixelillä nappuloihin osumista halusin helpottaa"*. Ohilyönti
 * on osumaton napautus, koska *"sormi peittää sarakkeen"* (`docs/TOINEN-ASIAKAS.md`). Kytkin
 * näyttää sormen ulkopuolella mihin painallus osui: numero syttyy aksenttivärillä kun pisteeltä
 * voi siirtää ja yliviivattuna kun ei voi. Kosketusalue ja siirto pysyvät ennallaan.
 *
 * **Oletus on pois, eikä se riipu ruudun koosta** (Tommin tarkennus samana iltana: *"pelaaja
 * itse päättää, pieni näyttö on lähes sokealle todella tulkinnanvarainen"*). Kuudes arvo
 * (`docs/ARVOT.md`) pätee siis sellaisenaan.
 */
interface PointPressStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsPointPress(private val prefs: SharedPreferences) : PointPressStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    companion object {
        const val FILE_NAME = "dg_point_press"
        private const val KEY = "point_press"
    }
}
