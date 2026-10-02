package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Kummalla kädellä pelaaja siirtää nappuloita, laitteen oma kytkin (`docs/ASETUKSET.md` luku
 * 3). Sivupaneeli menee **vastakkaiselle puolelle**: oikeakätisellä vasemmalle,
 * vasenkätisellä oikealle.
 *
 * **Oletus on oikeakätinen**, jolloin käytös on sama kuin ennen 9.9.2026: paneeli vasemmalla.
 *
 * Tämä korvaa 8.9.2026 tehdyn ratkaisun jossa paneelin puoli luettiin sivuston `Home boards on
 * left side` -kentästä ([BoardLook.mirrored]). Peruste kumoutui käytössä: peilatessa siirtokäsi
 * ei vaihdu, vain paneeli siirtyy, joten puoli riippuu kätisyydestä eikä laudan suunnasta
 * (`SUBSTANSSI.md` kohta 103, Tommi 9.9.2026: *"sivupaneeli menee sen käden alle jolla siirrän,
 * vasen käsi jää odottamaan ja oikea liikkuu liikaa"*). Vanha sääntö osui oikeaan kahdessa
 * yhdistelmässä neljästä, ja molemmat virheelliset ovat aitoja: oikeakätinen peilattuna ja
 * vasenkätinen ilman peilausta.
 *
 * Kätisyys on pelaajan ja laitteen ominaisuus eikä tiliasetus, joten tämä on samaa lajia kuin
 * [PortraitLockStore]: ei lähde koskaan verkkoon.
 */
interface HandednessStore {

    /** Tosi kun pelaaja siirtää vasemmalla kädellä, jolloin sivupaneeli on oikealla. */
    fun get(): Boolean

    fun save(leftHanded: Boolean)
}

class SharedPrefsHandedness(private val prefs: SharedPreferences) : HandednessStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(leftHanded: Boolean) {
        prefs.edit().putBoolean(KEY, leftHanded).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_handedness"
        const val KEY = "left_handed"
    }
}
