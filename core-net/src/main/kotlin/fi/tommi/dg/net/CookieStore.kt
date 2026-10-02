package fi.tommi.dg.net

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

/**
 * Keksien säilytys.
 *
 * Tällä on yksi tehtävä, ja se on suoraan se kipu jonka takia koko sovellus tehdään:
 * kun keksi säilyy prosessin kuoleman yli, käyttäjä ei joudu kirjautumaan uudestaan.
 * OkHttpin oletus on ettei keksijaria ole lainkaan, jolloin istunto katoaa heti.
 */
interface CookieStore {
    fun load(): List<Cookie>
    fun save(cookies: List<Cookie>)
}

class InMemoryCookieStore : CookieStore {
    private var cookies: List<Cookie> = emptyList()
    override fun load(): List<Cookie> = cookies
    override fun save(cookies: List<Cookie>) {
        this.cookies = cookies
    }
}

/**
 * Levylle kirjoittava säilytys. JVM-puolella tämä on testien toteutus; Androidilla
 * vastaava kirjoittaa sovelluksen omaan tiedostohakemistoon.
 *
 * Muoto on rivi per keksi: `<alkuperä-url>\t<Set-Cookie-merkkijono>`. Alkuperä talletetaan
 * mukaan, koska [Cookie.parse] tarvitsee sen päätelläkseen domainin ja polun oletukset.
 */
class FileCookieStore(private val file: File) : CookieStore {

    override fun load(): List<Cookie> {
        if (!file.exists()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val separator = line.indexOf('\t')
                if (separator < 0) return@mapNotNull null
                val url = line.substring(0, separator).toHttpUrlOrNull() ?: return@mapNotNull null
                Cookie.parse(url, line.substring(separator + 1))
            }
    }

    override fun save(cookies: List<Cookie>) {
        file.parentFile?.mkdirs()
        val text = cookies.joinToString("\n") { cookie ->
            val scheme = if (cookie.secure) "https" else "http"
            "$scheme://${cookie.domain}${cookie.path}\t$cookie"
        }
        file.writeText(text)
    }
}

/**
 * CookieJar joka lukee ja kirjoittaa [CookieStore]en ja pudottaa vanhentuneet keksit.
 * Domainkohtaista tarkkuutta ei tarvita: asiakas puhuu vain yhdelle sivustolle.
 */
internal class StoredCookieJar(private val store: CookieStore) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val incomingNames = cookies.mapTo(mutableSetOf()) { it.name }
        val merged = store.load().filterNot { it.name in incomingNames } + cookies
        store.save(merged)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return store.load().filter { it.expiresAt > now && it.matches(url) }
    }
}
