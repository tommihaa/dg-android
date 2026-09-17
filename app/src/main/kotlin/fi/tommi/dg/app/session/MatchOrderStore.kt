package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import fi.tommi.dg.domain.MatchOrder
import fi.tommi.dg.domain.MatchSortKey

/**
 * Otteluluettelon järjestysvalinta laitteella (auditoinnin G1).
 *
 * Sama laji kuin [BoardStyleStore]: tämän laitteen oma valinta, joka ei lähde koskaan
 * verkkoon. Sivuston omat asetukset asuvat [SiteSettingsStore]ssa, eikä näitä sekoiteta.
 *
 * **Valinta muistetaan käynnistysten yli** (Tommin valinta 26.8.2026). Sen hinta on
 * kirjattava, koska se on tiedossa: sivuston järjestys on kiireellisyysjärjestys, joten
 * unohtunut lajittelu voi jättää kiireellisimmän ottelun listan häntään. Siksi valittu
 * sarake näkyy otsikossa nuolena, ja siitä pääsee takaisin samalla napautuksella jolla se
 * tehtiin.
 */
interface MatchOrderStore {

    fun get(): MatchOrder

    fun save(order: MatchOrder)
}

class SharedPrefsMatchOrder(private val prefs: SharedPreferences) : MatchOrderStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): MatchOrder {
        val stored = prefs.getString(KEY_SORT, null) ?: return MatchOrder.SITE
        // Tuntematon nimi on sivuston järjestys eikä kaato, sama peruste kuin
        // SharedPrefsBoardStylen luvussa: poistettu sarake ei saa estää käynnistystä.
        val key = MatchSortKey.entries.firstOrNull { it.name == stored } ?: return MatchOrder.SITE
        return MatchOrder(key, prefs.getBoolean(KEY_DESCENDING, false))
    }

    override fun save(order: MatchOrder) {
        prefs.edit()
            .putString(KEY_SORT, order.key?.name)
            .putBoolean(KEY_DESCENDING, order.descending)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "dg_match_order"
        const val KEY_SORT = "sort_key"
        const val KEY_DESCENDING = "descending"
    }
}
