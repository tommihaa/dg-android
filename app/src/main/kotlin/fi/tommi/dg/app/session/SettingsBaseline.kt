package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Asetusten lähtötila: se mitä sivustolla luki ennen kuin tämä sovellus kirjoitti sinne
 * ensimmäisen kerran.
 *
 * **Tämä on `docs/ASETUKSET.md` luvun 5 halvin vakuutus, ja se vastaa siihen lauseeseen
 * jonka vuoksi asetusruutu oli lukutilassa:** väärä lähetys tuhoaisi asetukset eikä
 * alkuperäistä tilaa olisi enää missään. Nyt se on yhdessä paikassa.
 *
 * **Kirjoitetaan kerran eikä päivitetä.** Myöhempi lukema on jo sen jälkeinen tila että
 * sovellus on voinut kirjoittaa, joten sen tallentaminen lähtötilaksi tekisi tästä
 * varmuuskopiosta kopion mahdollisesta virheestä. Kertaluontoisuus on siis koko idea eikä
 * optimointi.
 *
 * Tallennuspäivä on mukana, koska palautettava tila ilman päivää ei kerro mihin se
 * palauttaa. Sama peruste kuin arkiston reunapäivällä otteluluettelossa.
 */
data class SettingsSnapshot(
    val checked: Set<String>,
    val radios: Map<String, String>,
    /** Epookkimillisekunteina. Luku on tallennushetki eikä sivuston oma aika. */
    val storedAt: Long,
)

interface SettingsBaseline {

    fun get(): SettingsSnapshot?

    /**
     * Tallentaa lähtötilan jos sitä ei vielä ole.
     *
     * @return `true` jos tämä kutsu kirjoitti, `false` jos lähtötila oli jo olemassa
     */
    fun captureOnce(checked: Set<String>, radios: Map<String, String>, now: Long): Boolean
}

/**
 * Lähtötila `SharedPreferences`issä, omassa tiedostossaan.
 *
 * Muoto on sama kuin tunnusten säilöllä: pieni rajapinta ja injektoitava `prefs`, jotta
 * näkymämallin testi ei tarvitse Androidia. Kantataulua ei tehdä, koska kyse on yhdestä
 * lukemasta eikä historiasta.
 */
class SharedPrefsSettingsBaseline(private val prefs: SharedPreferences) : SettingsBaseline {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): SettingsSnapshot? {
        val storedAt = prefs.getLong(KEY_STORED_AT, 0L)
        if (storedAt == 0L) return null
        return SettingsSnapshot(
            // Kopio, koska getStringSet palauttaa säilön oman joukon eikä sitä saa muuttaa.
            checked = prefs.getStringSet(KEY_CHECKED, emptySet()).orEmpty().toSet(),
            radios = decodeRadios(prefs.getString(KEY_RADIOS, "").orEmpty()),
            storedAt = storedAt,
        )
    }

    override fun captureOnce(
        checked: Set<String>,
        radios: Map<String, String>,
        now: Long,
    ): Boolean {
        if (prefs.getLong(KEY_STORED_AT, 0L) != 0L) return false
        prefs.edit()
            .putStringSet(KEY_CHECKED, checked)
            .putString(KEY_RADIOS, encodeRadios(radios))
            .putLong(KEY_STORED_AT, now)
            .apply()
        return true
    }

    private companion object {
        const val FILE_NAME = "dg_settings_baseline"
        const val KEY_CHECKED = "checked"
        const val KEY_RADIOS = "radios"
        const val KEY_STORED_AT = "stored_at"

        /**
         * Radiot yhtenä merkkijonona, `nimi=arvo` rivi kerrallaan.
         *
         * Sivuston kenttänimet ja arvot ovat paljaita numeroita ja lyhyitä sanoja, joten
         * erotinmerkkejä ei esiinny niissä. Muoto on tarkoituksella luettava: tämä tiedosto
         * on se paikka josta asetukset kaivetaan silloin kun jokin on mennyt pieleen.
         */
        fun encodeRadios(radios: Map<String, String>): String =
            radios.entries.joinToString("\n") { "${it.key}=${it.value}" }

        fun decodeRadios(encoded: String): Map<String, String> =
            encoded.lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val at = line.indexOf('=')
                    if (at <= 0) null else line.substring(0, at) to line.substring(at + 1)
                }
                .toMap()
    }
}
