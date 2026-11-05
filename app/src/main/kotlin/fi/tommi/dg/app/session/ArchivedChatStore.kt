package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * Ne ottelun chat-viestit jotka on jo kirjoitettu arkistoon, sivun mukaan.
 *
 * **Kaanonin tarkennus 2.11.2026** (`docs/KOHDE.md` › *Saapumishetki on osa viestin
 * tunnistetta*): ottelun chat-viesti tallennetaan kerran sivua kohden. Sivun voi lukea
 * uudelleen, ja jokainen luku antaisi viestille uuden saapumishetken ja siksi uuden
 * tunnisteen, jolloin `insertIfNew` ei estäisi toista riviä. Mitattu kuorella 2.11.2026:
 * sama päättymissivu kolmesti, sama viesti kolmesti arkistossa.
 *
 * Avain on tiiviste parista (sivun oma lomakeosoite, viestin teksti), arvo arkistoidun
 * rivin tunniste. Tekstiä ei säilötä tänne, koska se on jo arkistossa. Laitteen oma tieto
 * kuten [PlayerRatingStore], ei lähde koskaan verkkoon.
 */
interface ArchivedChatStore {

    /** Arkistoidun rivin tunniste, tai null kun tämä sivu ja teksti ovat uusia. */
    fun get(key: String): String?

    fun put(key: String, messageId: String)

    companion object {
        /**
         * Muistaa vain olion elinajan, eli entinen `archivedChats`-kartta. Näkymämallien
         * oletus, jotta testit eivät tarvitse laitetta.
         */
        fun inMemory(): ArchivedChatStore = object : ArchivedChatStore {
            private val seen = mutableMapOf<String, String>()
            override fun get(key: String): String? = seen[key]
            override fun put(key: String, messageId: String) {
                seen[key] = messageId
            }
        }

        /**
         * Avain sivun osoitteesta ja tekstistä. Nollatavu erottaa kentät samasta syystä kuin
         * `Message.contentHash`issa: ilman sitä ("ab","c") ja ("a","bc") olisivat sama avain.
         */
        fun key(pageKey: String, text: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(pageKey.toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(text.toByteArray(Charsets.UTF_8))
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Säilö laitteella. Rivejä on yksi saapunutta chat-viestiä kohden, eli kymmeniä kuukaudessa,
 * joten raja [MAX_ENTRIES] on varokeino eikä odotettu tila: ylityksessä vanhimmat lähtevät.
 * Poistettu avain tarkoittaa vain, että sama sivu kirjoittaisi viestin uudelleen.
 */
class SharedPrefsArchivedChats(
    private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
) : ArchivedChatStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(key: String): String? =
        prefs.getString(key, null)?.substringBefore(SEPARATOR)?.takeIf { it.isNotEmpty() }

    override fun put(key: String, messageId: String) {
        val edit = prefs.edit().putString(key, "$messageId$SEPARATOR${now()}")
        val all = prefs.all
        if (all.size >= MAX_ENTRIES) {
            all.entries
                .sortedBy { (it.value as? String)?.substringAfter(SEPARATOR)?.toLongOrNull() ?: 0L }
                .take(all.size - MAX_ENTRIES + 1)
                .forEach { edit.remove(it.key) }
        }
        edit.apply()
    }

    private companion object {
        const val FILE_NAME = "dg_archived_chats"
        const val SEPARATOR = '|'
        const val MAX_ENTRIES = 2000
    }
}
