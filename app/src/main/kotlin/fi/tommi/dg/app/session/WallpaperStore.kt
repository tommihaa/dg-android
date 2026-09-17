package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Tyhjän tilan taustakuvien kansio: laitteen kansio jonka käyttäjä osoitti kerran asetuksista.
 *
 * **Miksi kansio eikä asset** (Tommin päätökset 15.9.2026, `docs/AVOIMET.md` › *Katutaide
 * tyhjään tilaan*). Kuvat ovat muiden taiteilijoiden töitä Blueskysta: omalla laitteella
 * omasta kansiosta luettuna yksityiskäyttöä, APK:hon paketoituna jakelua. Kansio sopii myös
 * yhden laitteen rajaukseen, koska kuvat asuvat siellä missä arkistokin. Kuvat viedään
 * laitteelle PC:ltä (`tyokalut/taustakuvat.py`), ja sovellus vain lukee.
 *
 * **Mekanismi on sama kuin varmuuskopiolla** ([BackupStore]): osoite on `content://`-uri
 * merkkijonona, ja lukuoikeus otetaan pysyväksi kutsupaikassa
 * (`takePersistableUriPermission`). Ero on että tämä on kansio (`OpenDocumentTree`) eikä
 * tiedosto, koska kuvia on satoja ja niistä arvotaan. Manifestiin ei tule uutta oikeutta.
 *
 * **Kansio on itse kytkin.** Erillistä päällä/pois-avainta ei ole: kun kansio on valittu,
 * kuvat näkyvät, ja `Stop showing` unohtaa kansion. Sama laji kuin [SkyThemeStore]: ei lähde
 * koskaan verkkoon.
 */
interface WallpaperStore {

    /** Valittu kansio uri-merkkijonona, tai null kun kuvia ei näytetä. */
    fun folder(): String?

    fun saveFolder(uri: String)

    /**
     * Kansio pois: käyttäjän valinta, tai oikeus meni (kansio poistettu tai lupa peruttu).
     * Jälkimmäisessä osoite unohdetaan samasta syystä kuin varmuuskopiossa: muuten jokainen
     * ruudun avaus yrittäisi samaa ja epäonnistuisi hiljaa, eikä asetusruutu voisi kertoa
     * että kansio pitää valita uudestaan.
     */
    fun forget()
}

class SharedPrefsWallpaper(private val prefs: SharedPreferences) : WallpaperStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun folder(): String? = prefs.getString(KEY_FOLDER, null)

    override fun saveFolder(uri: String) {
        prefs.edit().putString(KEY_FOLDER, uri).apply()
    }

    override fun forget() {
        prefs.edit().remove(KEY_FOLDER).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_wallpaper"
        const val KEY_FOLDER = "folder_uri"
    }
}
