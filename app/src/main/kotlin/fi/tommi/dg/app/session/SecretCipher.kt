package fi.tommi.dg.app.session

import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Yhden merkkijonon salaus levylle.
 *
 * Rajapinta on erillään avaimen noudosta tarkoituksella. Avain tulee Androidin
 * Keystoresta, jota ei voi ajaa JVM-testissä, mutta itse salaus on tavallista JCE:tä ja
 * siksi testattavissa ilman laitetta. Ilman tätä jakoa koko salauslogiikka olisi
 * todennettavissa vasta tabletilla.
 */
interface SecretCipher {

    /** @throws GeneralSecurityException jos laitteen salausrajapinta ei toimi. */
    fun encrypt(plaintext: String): String

    /** @return null jos arvoa ei saada auki. Ks. [AesGcmCipher.decrypt]. */
    fun decrypt(stored: String): String?
}

/**
 * AES-GCM. Salattu arvo talletetaan muodossa `base64(iv):base64(salakieli+tunniste)`.
 *
 * IV talletetaan mukaan koska se on eri joka kerta, eikä se ole salaisuus. GCM valittiin
 * koska se todentaa sisällön muuttumattomuuden itse: muokattu tavu ei tuota roskaa vaan
 * poikkeuksen, jolloin väärä salasana ei pääse lähtemään verkkoon.
 */
class AesGcmCipher(private val key: () -> SecretKey) : SecretCipher {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Ei omaa IV:tä: annetaan toteutuksen arpoa se, jolloin sama IV ei voi vahingossa
        // toistua saman avaimen kanssa. GCM:llä toisto murtaisi salauksen.
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val secret = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return encode(cipher.iv) + SEPARATOR + encode(secret)
    }

    /**
     * Palauttaa null kaikissa epäonnistumisissa poikkeuksen sijaan.
     *
     * Syy on että jokainen niistä tarkoittaa käyttäjän kannalta samaa asiaa: tunnusta ei
     * ole käytettävissä, joten kirjaudu uudelleen. Näin voi käydä ilman että mitään on
     * rikki, esimerkiksi jos Keystoren avain on vaihtunut laitteen palautuksessa.
     * Poikkeus tässä kaataisi sovelluksen käynnistyksessä.
     */
    override fun decrypt(stored: String): String? {
        val separator = stored.indexOf(SEPARATOR)
        if (separator <= 0 || separator == stored.lastIndex) return null
        return try {
            val iv = decode(stored.substring(0, separator))
            val secret = decode(stored.substring(separator + 1))
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(secret), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            // Kelvoton base64. Sama merkitys kuin epäonnistuneella purulla.
            null
        }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().withoutPadding().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = ":"

        /** GCM:n todennustunnisteen pituus. 128 on suurin ja siksi oletus. */
        const val TAG_BITS = 128
    }
}
