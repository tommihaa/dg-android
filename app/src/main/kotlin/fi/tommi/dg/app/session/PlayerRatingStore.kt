package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Pelaajien ratingit sellaisina kuin ne viimeksi nähtiin, käyttäjänumeron mukaan.
 *
 * Tommin valinta 24.9.2026 (*"vain jo nähty"*): laudan pelaajakortin nimi saa
 * harvinaisuusvärin ratingin mukaan, mutta laudan sivulla ratingia ei ole, eikä sitä haeta
 * erikseen. Luku kirjataan vain sivuilta jotka haetaan joka tapauksessa (profiili,
 * pelaajalista), joten varasto ei tee yhtään pyyntöä ja ylläpidolle annettu lupaus pitää
 * (*"only when the person using it asks for something"*, `docs/YHTEYDENOTTO.md`). Tuntematon
 * pelaaja on null, ja kortti jää silloin entiselleen.
 *
 * Laitteen oma tieto kuten [LoungeFoldStore], ei lähde koskaan verkkoon.
 */
interface PlayerRatingStore {

    fun rating(userId: String): Double?

    /** Kirjaa ratingit; sivun teksti luetaan tässä, ja lukukelvoton ohitetaan. */
    fun record(ratings: Map<String, String?>)

    /**
     * Rating nimellä, kun sivu antaa nimen ilman käyttäjänumeroa (foorumin ketjun aloittaja,
     * Inboxin keskustelu). Nimi sovitetaan kirjainkoosta piittaamatta numeroon, jonka
     * [recordNames] on kirjannut (Tommin tilaus 26.9.2026).
     */
    fun ratingByName(name: String): Double? = null

    /** Kirjaa nimen ja käyttäjänumeron parit sivuilta, joilla molemmat näkyvät. */
    fun recordNames(names: Map<String, String>) = Unit

    companion object {
        /** Ei muista mitään. Näkymämallien oletus, jotta testit eivät tarvitse varastoa. */
        val NONE: PlayerRatingStore = object : PlayerRatingStore {
            override fun rating(userId: String): Double? = null
            override fun record(ratings: Map<String, String?>) = Unit
        }

        /** Sivun ratingteksti luvuksi (`2219.90` → 2219.9), tai null. */
        fun parse(text: String?): Double? = text?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }
}

class SharedPrefsPlayerRatings(private val prefs: SharedPreferences) : PlayerRatingStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun rating(userId: String): Double? =
        prefs.getString(userId, null)?.toDoubleOrNull()

    override fun record(ratings: Map<String, String?>) {
        val edit = prefs.edit()
        var changed = false
        ratings.forEach { (userId, text) ->
            val value = PlayerRatingStore.parse(text) ?: return@forEach
            edit.putString(userId, value.toString())
            changed = true
        }
        if (changed) edit.apply()
    }

    override fun ratingByName(name: String): Double? =
        prefs.getString(nameKey(name), null)?.let(::rating)

    override fun recordNames(names: Map<String, String>) {
        val edit = prefs.edit()
        var changed = false
        names.forEach { (name, userId) ->
            if (name.isBlank() || prefs.getString(nameKey(name), null) == userId) return@forEach
            edit.putString(nameKey(name), userId)
            changed = true
        }
        if (changed) edit.apply()
    }

    private companion object {
        const val FILE_NAME = "dg_player_ratings"

        // 27.–29.9.2026 kirjoitetut sijarajat (`top32`, `top100`, `top132`, `top300` ja
        // `top1000`) jäävät tiedostoon lukemattomina: portaat ovat kiinteitä 29.9.2026 illasta.

        /** Nimiavain samassa tiedostossa; etuliite erottaa sen numeroavaimista. */
        fun nameKey(name: String) = "n:" + name.trim().lowercase()
    }
}
