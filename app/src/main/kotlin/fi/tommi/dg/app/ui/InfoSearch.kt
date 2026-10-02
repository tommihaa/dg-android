package fi.tommi.dg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.SiteHelpPage
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.SiteHelpParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Infon haku (Tommin tilaus 24.9.2026): App Help ja sivuston oma Help, yhdestä kentästä.
 *
 * **Kaksi lähdettä, kaksi eri saatavuutta.** App Help on sovelluksen omaa tekstiä ja aina
 * käsillä. DailyGammon Help ei ole tallessa missään, vaan Info-rivi hakee sen `/help`ista
 * joka avauksella, joten haku noutaa sen kerran kun kenttään kirjoitetaan ensimmäinen
 * merkki. `/help` on sivuston staattinen sivu eikä kuluta jonoa, sama pyyntö kuin rivin
 * avaus. Epäonnistunut nouto ei kaada hakua: App Helpin osumat näkyvät silti, ja rivi
 * kertoo että toinen puoli puuttuu.
 */
enum class InfoHitSource { APP, SITE }

/**
 * Yksi osuma. [number] on App Helpin tunnus (`B.7`) tai sivuston osaston nimi, eli se
 * jolla lukija löytää kohdan omalta ruudultaan.
 */
data class InfoHit(
    val source: InfoHitSource,
    val number: String,
    val question: String,
    val answer: String,
)

/** Haettava kohta ennen hakua; App Helpin kohdat luetaan resursseista ruudulla. */
data class InfoEntry(val number: String, val question: String, val answer: String)

/**
 * Osuu kun **jokainen** hakusana löytyy kysymyksestä tai vastauksesta, kirjainkoosta
 * välittämättä. Sanojen ei tarvitse olla peräkkäin, koska manuaalia haetaan aiheella
 * (`archive backup`) eikä lainauksella. Kysymyksestä osuneet ensin, koska otsikko-osuma
 * on useammin se kohta jota etsittiin.
 */
fun searchInfo(query: String, app: List<InfoEntry>, site: SiteHelpPage?): List<InfoHit> {
    val words = queryWords(query)
    if (words.isEmpty()) return emptyList()
    val all = app.map { InfoHit(InfoHitSource.APP, it.number, it.question, it.answer) } +
        site?.sections.orEmpty().flatMap { section ->
            section.entries.map { InfoHit(InfoHitSource.SITE, section.title, it.question, it.answer) }
        }
    val hits = all.filter { hit ->
        words.all { hit.question.contains(it, ignoreCase = true) || hit.answer.contains(it, ignoreCase = true) }
    }
    return hits.sortedByDescending { hit -> words.any { hit.question.contains(it, ignoreCase = true) } }
}

/** Hakusanat, joita myös korostus käyttää. */
fun queryWords(query: String): List<String> = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

/** DailyGammon Helpin noudon tila haun näkökulmasta. */
sealed interface SiteHelpSearchState {
    /** Ei vielä pyydetty: kenttä on ollut tyhjä. */
    data object Idle : SiteHelpSearchState
    data object Loading : SiteHelpSearchState
    data class Loaded(val page: SiteHelpPage) : SiteHelpSearchState
    /** Nouto tai jäsennys epäonnistui; syytä ei eritellä, koska ruutu tarjoaa saman uusinnan. */
    data object Failed : SiteHelpSearchState
}

/**
 * Noutaa `/help`in kerran haun tarpeisiin. Ei ajastusta eikä uudelleenyritystä: uusinta
 * on käyttäjän napautus ([load] uudelleen), sama sääntö kuin porautumisruudun `Refresh`issä.
 */
class InfoSearchViewModel(
    private val pages: PageFetcher,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _site = MutableStateFlow<SiteHelpSearchState>(SiteHelpSearchState.Idle)
    val site: StateFlow<SiteHelpSearchState> = _site.asStateFlow()

    /** Noutaa vain kun sivua ei ole eikä nouto ole kesken, joten jokainen näppäily ei lähetä pyyntöä. */
    fun load() {
        val now = _site.value
        if (now is SiteHelpSearchState.Loading || now is SiteHelpSearchState.Loaded) return
        _site.value = SiteHelpSearchState.Loading
        viewModelScope.launch {
            _site.value = withContext(io) {
                (pages.fetch(SITE_HELP_PATH) as? DgResponse.Ok)
                    ?.let { SiteHelpParser.parse(it.html) }
                    ?.let { SiteHelpSearchState.Loaded(it) }
                    ?: SiteHelpSearchState.Failed
            }
        }
    }

    class Factory(private val pages: PageFetcher) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = InfoSearchViewModel(pages) as T
    }

    private companion object {
        const val SITE_HELP_PATH = "/help"
    }
}
