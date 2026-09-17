package fi.tommi.dg.net

/**
 * Tunnukset. Ei data class, koska sen generoima [toString] paljastaisi salasanan.
 * Lokiin vuotanut salasana on pahempi kuin puuttuva loki.
 */
class DgCredentials(val login: String, val password: String) {
    init {
        require(login.isNotBlank()) { "Käyttäjänimi puuttuu" }
        require(password.isNotEmpty()) { "Salasana puuttuu" }
    }

    override fun toString(): String = "DgCredentials(login=$login, password=***)"
}

fun interface CredentialsProvider {
    /** Palauttaa tunnukset, tai null jos käyttäjä ei ole vielä kirjautunut. */
    fun get(): DgCredentials?
}
