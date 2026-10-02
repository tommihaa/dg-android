package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import fi.tommi.dg.net.CredentialsProvider
import fi.tommi.dg.net.DgCredentials
import java.security.GeneralSecurityException

/**
 * Tunnusten säilytys laitteella.
 *
 * Toteuttaa [CredentialsProvider]n, joten [fi.tommi.dg.net.DgClient] osaa uusia istunnon
 * hiljaisesti ilman että käyttäjää kysytään uudestaan. Se on koko sovelluksen toinen
 * olemassaolon syy, joten tunnusten on säilyttävä prosessin kuoleman yli.
 */
interface CredentialsStore : CredentialsProvider {

    /**
     * @return false jos salasanaa ei saatu suojattua. Kutsujan on silloin kerrottava se
     *   käyttäjälle eikä jatkettava kuin kirjautuminen olisi onnistunut: muuten seuraisi
     *   silmukka jossa kirjautuminen näyttää menevän läpi mutta unohtuu heti.
     */
    fun save(login: String, password: String): Boolean

    fun clear()

    fun hasCredentials(): Boolean
}

/**
 * Sovelluksen omiin asetuksiin kirjoittava toteutus. Salasana salataan
 * [SecretCipher]illä, jonka avain elää Androidin Keystoressa.
 *
 * Salaus ei poista sitä tosiasiaa että sivustolla ei ole HTTPS:ää lainkaan: sama salasana
 * kulkee joka pyynnöllä verkossa salaamattomana, ja käyttäjälle kerrotaan se
 * kirjautumisruudulla. Salaus sulkee eri reiän kuin verkkoyhteys, eli sen että levylle
 * jäänyt tiedosto olisi luettavissa sellaisenaan.
 *
 * Käyttäjänimeä ei salata. Se ei ole salaisuus, ja selkokielisenä se kertoo suoraan onko
 * tunnus tallessa myös silloin kun avain on mitätöitynyt.
 */
class SharedPrefsCredentialsStore(
    private val prefs: SharedPreferences,
    private val cipher: SecretCipher,
    /** Uloskirjautumisessa poistetaan myös avain. Erotettu testattavuuden vuoksi. */
    private val deleteKey: () -> Unit = KeystoreKey::delete,
) : CredentialsStore {

    constructor(context: Context) : this(
        prefs = context.applicationContext
            .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
        cipher = AesGcmCipher(KeystoreKey::get),
    )

    override fun get(): DgCredentials? {
        val login = prefs.getString(KEY_LOGIN, null) ?: return null
        val stored = prefs.getString(KEY_PASSWORD, null) ?: return null
        val password = cipher.decrypt(stored) ?: return null
        // Tyhjä arvo ei ole tunnus. DgCredentials heittäisi, ja poikkeus istunnon
        // hiljaisen uusimisen sisällä olisi väärä tapa kertoa ettei kirjautumista ole.
        if (login.isBlank() || password.isEmpty()) return null
        return DgCredentials(login, password)
    }

    override fun save(login: String, password: String): Boolean {
        val protected = try {
            cipher.encrypt(password)
        } catch (e: GeneralSecurityException) {
            // Laitteen salausrajapinta ei toimi. Selkokielinen tallennus varmuuden
            // vuoksi olisi tässä väärä armo: se pettäisi juuri sen lupauksen jonka takia
            // tämä luokka on olemassa, eikä käyttäjä saisi tietää siitä.
            clear()
            return false
        }
        prefs.edit {
            putString(KEY_LOGIN, login.trim())
            putString(KEY_PASSWORD, protected)
        }
        return true
    }

    override fun clear() {
        prefs.edit { clear() }
        // Avain poistetaan vasta arvojen jälkeen. Toisessa järjestyksessä keskeytys
        // jättäisi levylle arvon jota ei voi enää avata eikä tunnistaa roskaksi.
        runCatching { deleteKey() }
    }

    override fun hasCredentials(): Boolean = get() != null

    companion object {
        const val FILE_NAME = "dg_credentials"
        private const val KEY_LOGIN = "login"
        // Nimi sanoo mitä arvo on. Vanhaa selkokielistä avainta ei siivota erikseen:
        // sovellusta ei ole ajettu millään laitteella, joten sellaista arvoa ei ole
        // olemassa, eikä koodia kirjoiteta datalle jota ei voi olla.
        private const val KEY_PASSWORD = "password_encrypted"
    }
}
