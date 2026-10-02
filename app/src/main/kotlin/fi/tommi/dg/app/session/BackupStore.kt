package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Viestiarkiston näkyvä varmuuskopio: mihin tiedostoon se kirjoitetaan ja milloin se
 * viimeksi onnistui.
 *
 * **Miksi tämä on olemassa vaikka Auto Backup on jo päällä.** Kanta menee Google-tilille
 * 13.8.2026 alkaen (`data_extraction_rules.xml`), mutta se kopio **palautuu vain
 * asennettaessa**, sitä ei voi pyytää, eikä sovellus näe sen tilaa millään rajapinnalla.
 * Käyttäjälle ei siis ole mitään merkkiä siitä onko historia tallessa. Tämä säilö kantaa sen
 * merkin: yksi tiedosto jonka käyttäjä itse valitsi, ja aika jolloin siihen viimeksi
 * kirjoitettiin. Kohta on `docs/AVOIMET.md`:ssä, ja tämä on siitä se puolisko jonka Tommi
 * valitsi ensin (1.9.2026).
 *
 * **Osoite on yksi tiedosto eikä kansio**, ja se on tietoinen rajaus. Kansio (`OpenDocumentTree`)
 * antaisi päivätyt tiedostot, mutta myös kasvavan pinon kopioita joista mikään ei ole *se*
 * ajantasainen. Yksi ylikirjoitettava tiedosto vastaa siihen kysymykseen johon tämä on:
 * *"onko historia tallessa ja miltä hetkeltä"*. Päivätty vienti on yhä olemassa erikseen
 * (`Export`), ja se on eri teko: kopio talteen, ei ajantasainen varmuuskopio.
 *
 * Uri talletetaan merkkijonona ja **käyttöoikeus otetaan pysyväksi** kutsupaikassa
 * (`takePersistableUriPermission`). Ilman sitä osoite olisi voimassa vain sen kerran, ja
 * seuraava kirjoitus epäonnistuisi hiljaa — juuri se vikamuoto jota tämä ominaisuus on
 * vastaan.
 */
interface BackupStore {

    /** Valittu tiedosto, tai null kun varmuuskopiota ei ole otettu käyttöön. */
    fun target(): String?

    /** Milloin tiedostoon viimeksi kirjoitettiin onnistuneesti, tai null. */
    fun lastSaved(): Long?

    /** Käyttöönotto tai kohteen vaihto. Nollaa aiemman ajan, koska tiedosto on toinen. */
    fun saveTarget(uri: String)

    /** Onnistuneen kirjoituksen jälkeen. */
    fun saveTime(epochMillis: Long)

    /**
     * Kohde on kadonnut: käyttäjä poisti tiedoston tai oikeus meni. Osoite unohdetaan, jotta
     * ruutu voi pyytää käyttöönottoa uudestaan sen sijaan että yrittäisi loputtomasti samaa.
     *
     * **Aika jää muistiin**, ja se on 1.9.2026 tehty korjaus eikä alkuperäinen käytös.
     * Aiemmin vika pyyhki myös ajan, jolloin seuraava käynnistys sanoi *"ei varmuuskopiota"*
     * vaikka kopio oli olemassa. Se on juuri se väärä varmuus jota vastaan koko rivi on, ja
     * peilikuvana: hiljainen väärä ei on yhtä väärä kuin hiljainen väärä kyllä. Säilynyt aika
     * on myös se mistä `BackupUiState.of` tunnistaa vian käynnistyksen yli.
     */
    fun forget()
}

class SharedPrefsBackup(private val prefs: SharedPreferences) : BackupStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun target(): String? = prefs.getString(KEY_URI, null)

    override fun lastSaved(): Long? =
        prefs.getLong(KEY_SAVED, 0L).takeIf { it > 0L }

    override fun saveTarget(uri: String) {
        prefs.edit().putString(KEY_URI, uri).remove(KEY_SAVED).apply()
    }

    override fun saveTime(epochMillis: Long) {
        prefs.edit().putLong(KEY_SAVED, epochMillis).apply()
    }

    override fun forget() {
        prefs.edit().remove(KEY_URI).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_backup"
        const val KEY_URI = "target_uri"
        const val KEY_SAVED = "last_saved"
    }
}
