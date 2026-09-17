package fi.tommi.dg.net

import java.io.File
import java.util.Properties

/**
 * Lukee DailyGammon-tunnukset projektin juuren `local.properties`-tiedostosta.
 *
 * Tiedosto on gitignoressa ja käyttäjä kirjoittaa sen itse. Tunnuksia ei koskaan
 * tulosteta, ei lokiin eikä testin virheilmoitukseen.
 */
object LocalCredentials {

    fun loadOrNull(): DgCredentials? {
        val file = findLocalProperties() ?: return null
        val properties = Properties().apply {
            file.inputStream().use { load(it) }
        }
        val login = properties.getProperty("dg.login")?.trim().orEmpty()
        val password = properties.getProperty("dg.password").orEmpty()
        if (login.isEmpty() || password.isEmpty()) return null
        return DgCredentials(login, password)
    }

    /**
     * Etsii tiedoston ylöspäin työhakemistosta. Gradle ajaa testit moduulin
     * hakemistossa, joten juuri on tyypillisesti yksi taso ylempänä.
     */
    private fun findLocalProperties(): File? {
        var directory: File? = File(".").absoluteFile
        while (directory != null) {
            val candidate = File(directory, "local.properties")
            if (candidate.isFile) return candidate
            directory = directory.parentFile
        }
        return null
    }
}
