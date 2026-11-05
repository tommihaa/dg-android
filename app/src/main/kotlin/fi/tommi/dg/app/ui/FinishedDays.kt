package fi.tommi.dg.app.ui

/**
 * Profiilin osoitteen `days_to_view`-luku, eli kuinka monen päivän päättyneet sivu näyttää.
 *
 * **Raja on päättymispäivä eikä aloituspäivä** (mitattu 31.10.2026, `docs/TESTIKUTSUT.md` ›
 * Pascalin toive). Luku vaihdetaan sivun omaan linkkiin eikä osoitetta koota, samasta
 * syystä kuin `TopPage.profilePath`issa: muut kyselyparametrit ovat palvelimen omat.
 *
 * **Ilman lukua sivu näyttää koko historian**, kunhan linkissä on `finished=1` (mitattu samana
 * päivänä: oma profiili 1930 ottelua, toisen pelaajan lajittelulinkki 7409). Toisen pelaajan
 * ottelut avautuvat juuri sellaisesta linkistä, joten [ALL] on sen lähtötila eikä poikkeus.
 */
internal object FinishedDays {

    /** Profiilin painikkeet. 30 on sivuston oma luku Top Pagen linkissä. */
    val CHOICES = listOf(3, 10, 30, 90)

    /** Koko historia: luku poistetaan linkistä (Tommin valinta 31.10.2026). */
    const val ALL = 0

    /**
     * Sivun oma oletus: paljas `/bg/user/<id>`, jota ottelurivin vastustajalinkki käyttää.
     * Se näytti 31.10.2026 pelaajan kymmenen viimeisintä päättynyttä, eli määrän eikä
     * päivärajaa (luvuilla 7, 10 ja 14 samalla pelaajalla viisi). Mikään painike ei siis
     * vastaa sitä, ja ruutu näyttää painikkeet valitsematta.
     */
    const val SITE = -1

    private val PARAM = Regex("""([?&])days_to_view=(\d+)""")
    private val FINISHED = Regex("""[?&]finished=1(&|$)""")
    private val PLAIN = Regex("""^/bg/user/\d+$""")

    /**
     * Polun raja: luku, [ALL] kun ottelutaulukot ovat linkissä ilman lukua, [SITE] paljaalle
     * profiililinkille, tai null kun polku ei kanna päättyneitä (turnaus, ohje).
     */
    fun of(path: String): Int? =
        PARAM.find(path)?.groupValues?.get(2)?.toIntOrNull()
            ?: ALL.takeIf { FINISHED.containsMatchIn(path) }
            ?: SITE.takeIf { PLAIN.matches(path) }

    /** Sama polku toisella rajalla. Polku jolla [of] on null palaa ennallaan. */
    fun withDays(path: String, days: Int): String {
        if (of(path) == null || days == SITE) return path
        if (PLAIN.matches(path)) {
            // Kyselyn muoto on Top Pagen linkin oma, eikä sitä keksitä tässä.
            return if (days == ALL) "$path?active=1&finished=1"
            else "$path?days_to_view=$days&active=1&finished=1"
        }
        val found = PARAM.find(path)
        return when {
            days == ALL && found == null -> path
            days == ALL -> {
                // Ensimmäinen parametri vie `?`:n mukanaan, joten seuraava saa sen.
                val rest = path.removeRange(found!!.range)
                if (found.groupValues[1] == "?") rest.replaceFirst("&", "?") else rest
            }
            found == null -> "$path&days_to_view=$days"
            else -> path.replaceRange(found.range, found.groupValues[1] + "days_to_view=" + days)
        }
    }
}
