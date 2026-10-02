package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Noppien painallus tekona, kaksi laitteen omaa kytkintä (`docs/ASETUKSET.md` luku 3).
 *
 * **Tommin tilaus 4.9.2026:** *"kun kaikki nopat on harmaita, niin noppia painamalla
 * toteutuu Submit move"* ja *"kun yksikään noppa ei ole harmaa, niin noppia painamalla
 * toteutuu Swap Dice"*. Kaksi kytkintä eikä yksi, koska teot ovat eri painoisia: vaihto on
 * peruttavissa saman painalluksen toistolla, lähetys päättää vuoron.
 *
 * **Ehto luetaan paikallisesta kokoamisesta** (Tommin kuittaus 4.9.2026, kaksi kysymystä).
 * Harmaus on kokoamisen ominaisuus, ja sivun omalla laudalla sitä ei ole lainkaan, joten
 * kytkimet eivät tee siellä mitään. Lähetyksen ehto on vuoron täysinäisyys eikä noppien
 * väri: pakotetussa vuorossa toinen noppa jää harmaantumatta, ja juuri silloin sivusto ei
 * salli enempää. Sääntö on yhtenä funktiona `BoardScreen.diceTapFor`issa, ei kahtena
 * ehtona piirtokohdassa.
 *
 * **Oletus on pois molemmilla**, sama peruste kuin [ForcedStepsStore]illa. Noppa ei ole
 * tähän asti ollut nappi, joten päälle kytkeminen muuttaa sen mitä laudalla oleva
 * painallus tarkoittaa.
 */
interface DiceSubmitStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

/** Noppien painallus vaihtaa noppien järjestyksen. Sama laji kuin [DiceSubmitStore]. */
interface DiceSwapStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsDiceSubmit(private val prefs: SharedPreferences) : DiceSubmitStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_dice_tap"
        const val KEY = "dice_submit"
    }
}

class SharedPrefsDiceSwap(private val prefs: SharedPreferences) : DiceSwapStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_dice_tap"
        const val KEY = "dice_swap"
    }
}
