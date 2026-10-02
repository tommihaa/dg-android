package fi.tommi.dg.app.session

import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Laiteasetusten vienti ja tuonti kopioitavana merkkijonona (Tommin tilaus 28.9.2026:
 * *"export ja import ominaisuus: copy-pastettava merkkimössö"*).
 *
 * **Mukana on vain [FILES]**, eli This device -osion valinnat sekä otteluluettelon järjestys ja
 * viestien pelaajasuodatin (Tommin valinta 28.9.2026). Tunnukset, sovelluslukko, varmuuskopio,
 * viestiarkisto, ottelumuisti, ratingit, pikaviestit ja kuvakansio eivät kulje koskaan: ne ovat
 * joko salaisia, laitteeseen sidottuja (kansion lupa) tai sisältöä eikä valintoja.
 *
 * Muoto on `DGA1.` ja sen perään deflatella pakattu JSON URL-turvallisena base64:nä. Etuliite
 * erottaa merkkijonon muusta leikepöydän tekstistä, ja numero antaa tulevalle muodolle tilaa.
 * JSONissa jokainen arvo kantaa tyyppinsä, koska `SharedPreferences` erottaa kokonaisluvun
 * liukuluvusta eikä JSON.
 *
 * Tuonti korvaa kunkin mukana olevan tiedoston kokonaan: puuttuva avain palaa oletukseensa,
 * kuten viejän laitteella. Tuntematon tiedosto ohitetaan, joten vieras merkkijono ei voi
 * kirjoittaa [FILES]in ulkopuolelle.
 */
class SettingsTransfer(private val prefs: (String) -> SharedPreferences) {

    /** Luettu merkkijono: tiedosto → avain → arvo. Arvo on Boolean, String, Int, Long, Float tai Set<String>. */
    data class Payload(val files: Map<String, Map<String, Any>>)

    fun export(): String {
        val root = JSONObject()
        val files = JSONObject()
        FILES.forEach { name ->
            val entries = JSONObject()
            prefs(name).all.toSortedMap().forEach { (key, value) -> encode(value)?.let { entries.put(key, it) } }
            files.put(name, entries)
        }
        root.put("v", 1)
        root.put("p", files)
        val packed = deflate(root.toString().toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(packed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /** Merkkijono luettuna, tai null kun se ei ole tämän sovelluksen vientiä. Ei kirjoita mitään. */
    fun parse(text: String): Payload? = runCatching {
        val body = text.filterNot { it.isWhitespace() }
        if (!body.startsWith(PREFIX)) return null
        val json = JSONObject(String(inflate(Base64.decode(body.removePrefix(PREFIX), Base64.URL_SAFE)), Charsets.UTF_8))
        if (json.optInt("v") != 1) return null
        val files = json.getJSONObject("p")
        val out = linkedMapOf<String, Map<String, Any>>()
        FILES.filter(files::has).forEach { name ->
            val entries = files.getJSONObject(name)
            out[name] = entries.keys().asSequence().associateWith { key -> decode(entries.getJSONArray(key)) }
        }
        Payload(out)
    }.getOrNull()

    /** Tiedostot joiden sisältö muuttuisi tuonnissa, [FILES]in järjestyksessä. */
    fun changes(payload: Payload): List<String> =
        payload.files.filter { (name, entries) -> normalized(prefs(name).all) != normalized(entries) }.keys
            .sortedBy(FILES::indexOf)

    /** Kirjoittaa mukana olevat tiedostot. Kutsutaan vasta käyttäjän vahvistuksen jälkeen. */
    fun apply(payload: Payload) {
        payload.files.forEach { (name, entries) ->
            val edit = prefs(name).edit().clear()
            entries.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                when (value) {
                    is Boolean -> edit.putBoolean(key, value)
                    is String -> edit.putString(key, value)
                    is Int -> edit.putInt(key, value)
                    is Long -> edit.putLong(key, value)
                    is Float -> edit.putFloat(key, value)
                    is Set<*> -> edit.putStringSet(key, value as Set<String>)
                }
            }
            edit.commit()
        }
    }

    private fun normalized(map: Map<String, *>): Map<String, Any?> = map.mapValues { (_, v) -> (v as? Set<*>)?.toSet() ?: v }

    private fun encode(value: Any?): JSONArray? = when (value) {
        is Boolean -> JSONArray().put("b").put(value)
        is String -> JSONArray().put("s").put(value)
        is Int -> JSONArray().put("i").put(value)
        is Long -> JSONArray().put("l").put(value)
        is Float -> JSONArray().put("f").put(value.toString())
        is Set<*> -> JSONArray().put("S").put(JSONArray(value.filterIsInstance<String>().sorted()))
        else -> null
    }

    private fun decode(item: JSONArray): Any = when (item.getString(0)) {
        "b" -> item.getBoolean(1)
        "s" -> item.getString(1)
        "i" -> item.getInt(1)
        "l" -> item.getLong(1)
        "f" -> item.getString(1).toFloat()
        "S" -> item.getJSONArray(1).let { a -> (0 until a.length()).map(a::getString).toSet() }
        else -> error("tuntematon tyyppi")
    }

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION).apply { setInput(bytes); finish() }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(bytes: ByteArray): ByteArray {
        val inflater = Inflater().apply { setInput(bytes) }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) error("katkennut")
            out.write(buffer, 0, n)
            if (out.size() > MAX_BYTES) error("liian suuri")
        }
        inflater.end()
        return out.toByteArray()
    }

    companion object {
        const val PREFIX = "DGA1."

        /** Purettu JSON ei saa kasvaa tätä suuremmaksi; vienti on noin kilotavu. */
        private const val MAX_BYTES = 64 * 1024

        /** Siirrettävät tiedostot, asetusruudun järjestyksessä. */
        val FILES = listOf(
            "dg_app_theme",
            "dg_board_style",
            "dg_board_shuffle",
            "dg_dice_style",
            "dg_score_style",
            "dg_busy_style",
            "dg_forced_steps",
            "dg_greedy_bearoff",
            "dg_move_arrows",
            "dg_beaver_confirm",
            "dg_dice_tap",
            "dg_sky_theme",
            "dg_rarity",
            "dg_match_order",
            "dg_message_filter",
            "dg_portrait_lock",
            "dg_board_rotation",
            "dg_full_screen",
            "dg_handedness",
        )
    }
}
