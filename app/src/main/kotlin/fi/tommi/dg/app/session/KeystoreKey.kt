package fi.tommi.dg.app.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Avain Androidin Keystoresta.
 *
 * Keystoren koko pointti on että avain ei ole tavuja joita sovellus voisi lukea: se elää
 * järjestelmän puolella, ja tuetuilla laitteilla erillisessä suojatussa piirissä. Levyltä
 * kopioitu salattu salasana on siis hyödytön toisella laitteella, koska avain ei lähde
 * mukaan.
 *
 * Käyttäjän tunnistautumista **ei** vaadita avaimen käyttöön. Se olisi houkutteleva
 * lisäys, mutta se rikkoisi istunnon hiljaisen uusimisen: uusiminen tapahtuu keskellä
 * tavallista sivunhakua, eikä siinä kohtaa voi pysähtyä kysymään sormenjälkeä.
 */
object KeystoreKey {

    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "dg_credentials_key"

    /**
     * Palauttaa avaimen ja luo sen ensimmäisellä kerralla.
     *
     * Haku tehdään joka kutsulla eikä välimuistiteta. Se on halpa, ja välimuisti eläisi
     * yli sen hetken jolloin avain mitätöityy, jolloin purku epäonnistuisi hämäävästi
     * vasta paljon myöhemmin.
     */
    fun get(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val existing = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        return existing?.secretKey ?: create()
    }

    fun delete() {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS)
    }

    private fun create(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Sama IV kahdesti murtaisi GCM:n, joten järjestelmä pakotetaan
                // arpomaan se. Tämä on oletus, mutta se sanotaan ääneen koska
                // vaihtoehdon hinta on koko salauksen menetys eikä vain heikennys.
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
