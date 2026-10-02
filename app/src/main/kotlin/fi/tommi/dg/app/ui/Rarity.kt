package fi.tommi.dg.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import fi.tommi.dg.app.session.TournamentRoundsStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Roolipelien esineharvinaisuuden värit viidelle portaalle: 0 common (harmaa), 1 uncommon
 * (vihreä), 2 rare (sininen), 3 very rare (violetti) ja 4 legendary (oranssi). Kuudes porras
 * (pinkki, top 32) poistettiin 29.9.2026 Tommin päätöksellä, ja samalla kaikki rajat palasivat
 * kiinteiksi ratingeiksi ([RATING_FLOORS]).
 *
 * Käytöt: Inboxin viestimäärä ([inboxCountTier]) ja pelaajan nimi ratingin mukaan
 * ([ratingTier]), molemmat Tommin tilauksia 24.9.2026, sekä turnauksen nimi kierrosmäärän
 * mukaan ([tournamentTier], 2.10.2026). Tumma sävy on tummalle pinnalle, vaalea tummennettu
 * versio vaalealle.
 */
internal object Rarity {

    private val DARK = listOf(
        Color(0xFFA8A8A8), // common, harmaa
        Color(0xFF3FCF3A), // uncommon, vihreä
        Color(0xFF5A9FF0), // rare, sininen
        Color(0xFFB45CF0), // very rare, violetti
        Color(0xFFFFA630), // legendary, oranssi
    )

    private val LIGHT = listOf(
        Color(0xFF6B6B6B),
        Color(0xFF1A7F14),
        Color(0xFF2A62B0),
        Color(0xFF7A1FB0),
        Color(0xFFB0600A),
    )

    fun color(tier: Int, dark: Boolean): Color = (if (dark) DARK else LIGHT)[tier.coerceIn(0, DARK.lastIndex)]
}

/**
 * Kiinteät alarajat vihreälle, siniselle, violetille ja oranssille: 1600, 1800, 2000 ja 2200
 * (Tommin valinta 29.9.2026 illalla). Harmaa on 1600:n alla. Sijoista luetut rajat (27.–29.9.)
 * poistettiin samalla, joten portaat eivät enää riipu siitä onko pelaajalistaa selattu.
 */
internal val RATING_FLOORS = listOf(1600.0, 1800.0, 2000.0, 2200.0)

/** Ratingin porras 0–4. */
internal fun ratingTier(rating: Double): Int = RATING_FLOORS.count { rating >= it }

/**
 * Tunnetun pelaajan rating käyttäjänumerolla, tai null. Lauta lukee tätä eikä varastoa
 * suoraan, jotta esikatselut ja testit toimivat ilman sitä.
 */
internal val LocalPlayerRating = staticCompositionLocalOf<(String) -> Double?> { { null } }

/**
 * Tunnetun pelaajan rating nimellä, tai null. Sivuille jotka antavat nimen ilman
 * käyttäjänumeroa (foorumin ketjun aloittaja, Inbox), ks. `PlayerRatingStore.ratingByName`.
 */
internal val LocalPlayerRatingByName = staticCompositionLocalOf<(String) -> Double?> { { null } }

/**
 * Rarity-värien kytkin (Tommin tilaus 26.9.2026, oletus päällä). Pois kytkettynä
 * [LocalPlayerRating] ja [LocalPlayerRatingByName] palauttavat aina null, ja tämä kertoo
 * Inboxille että viestimäärä ja nimet piirretään ilman rarity-värejä.
 */
internal val LocalRarityOn = staticCompositionLocalOf { true }

/**
 * Pelaajanimen rarity-väri, tai null kun rating ei ole tiedossa tai kytkin on pois.
 * Käyttäjänumero ensin, nimi toissijaisesti. Yksi paikka kaikille näkymille, jotta
 * portaat ja teeman sävy ovat samat kaikkialla.
 */
@Composable
internal fun playerRarityColor(userId: String?, name: String? = null, dark: Boolean = dgDark()): Color? {
    val rating = userId?.let(LocalPlayerRating.current)
        ?: name?.takeIf { it.isNotBlank() }?.let(LocalPlayerRatingByName.current)
    return rating?.let { Rarity.color(ratingTier(it), dark) }
}

/**
 * Turnauksen porras 0–4 sen mukaan montako ottelua putkeen voittaminen vaatii (Tommin
 * kuittaus 2.10.2026, `docs/UI.md` › *Turnausten rarity-värit*). Kierrosmäärä on voittojen
 * määrä, ja tasaväkisellä pelaajalla voiton todennäköisyys on noin 1/2^kierrokset.
 *
 * Enintään 4 kierrosta harmaa, 5 vihreä, 6 sininen, 7–8 violetti ja vähintään 9 oranssi.
 * **Champs on aina oranssi**: sinne pääsee vain voittamalla saman sarjan tavallisen
 * turnauksen (turnaussivun oma NOTE), joten voittaja on voittanut kaksi turnausta peräkkäin.
 * **Vuosittainen turnaus on aina oranssi** (Tommi 2.10.2026: *"2026 open on kerran vuodessa
 * järjestettävä ja siksi legendary"*). Se tunnistetaan nimen alun vuosiluvusta: Tournament
 * Hallin 3185 rivistä (2021–2026) vain `<vuosi> Open`, `<vuosi> Invitational` ja
 * `<vuosi> Double Repeat` alkavat vuosiluvulla, ja ne toistuvat vuosittain samalla nimellä.
 * Null kun kierrosmäärää ei tiedetä eikä nimi ole Champs tai vuosittainen.
 */
internal fun tournamentTier(rounds: Int?, name: String?): Int? {
    if (name != null && (CHAMPS.containsMatchIn(name) || YEARLY.containsMatchIn(name))) return RARITY_TOP
    val count = rounds?.takeIf { it > 0 } ?: return null
    return when {
        count <= 4 -> 0
        count == 5 -> 1
        count == 6 -> 2
        count <= 8 -> 3
        else -> RARITY_TOP
    }
}

private const val RARITY_TOP = 4

/** `Tortoise Threers Champs #25`, `Weekday Warriors Champs #26` (mitattu 2.10.2026). */
private val CHAMPS = Regex("""\bChamps\b""")

/** `2026 Open`, `2025 Invitational`, `2026 Double Repeat` (Tournament Hall 26.8.2026). */
private val YEARLY = Regex("""^(19|20)\d\d\s""")

/**
 * Kierrosmäärä kierrostekstistä: `3/5`, `Round 4/5` → 5. Null kun teksti ei kerro
 * kokonaismäärää (`Round 4`, yksityisotteluiden `-`).
 */
internal fun roundsFromText(text: String?): Int? =
    text?.substringAfter('/', "")?.trim()?.toIntOrNull()?.takeIf { it > 0 }

/** Turnausnumero polusta `/bg/event/<id>`, sama muoto kuin `core-scrape`n `SiteIds.eventId`. */
internal fun eventIdFromPath(path: String?): String? =
    path?.let { EVENT_ID.find(it)?.groupValues?.get(1) }

private val EVENT_ID = Regex("""/bg/event/(\d+)""")

/**
 * Nähdyt kierrosmäärät turnausnumeron mukaan. Kytkin pois päältä antaa
 * [TournamentRoundsStore.NONE]in, kuten [LocalPlayerRating] antaa silloin nullin.
 */
internal val LocalTournamentRounds = staticCompositionLocalOf { TournamentRoundsStore.NONE }

/**
 * Turnauksen nimen rarity-väri, tai null kun porrasta ei tiedetä tai kytkin on pois.
 *
 * Kun kutsuja tietää kierrosmäärän ([rounds]) ja turnausnumeron, luku kirjataan muistiin
 * piirron jälkeen. Näin Tournament Hall ja turnauslistat, joiden sivulla lukua ei ole,
 * värittävät saman turnauksen kun se on kerran nähty luettelossa, loungessa tai
 * turnaussivulla. Kirjaus on [SideEffect]issä eikä näkymämallissa, koska luku on jo ruudulla
 * eikä sitä varten haeta mitään.
 */
@Composable
internal fun tournamentRarityColor(
    eventId: String?,
    name: String?,
    rounds: Int? = null,
    dark: Boolean = dgDark(),
): Color? {
    if (!LocalRarityOn.current) return null
    val store = LocalTournamentRounds.current
    if (eventId != null && rounds != null) SideEffect { store.record(eventId, rounds) }
    val known = rounds ?: eventId?.let(store::rounds)
    return tournamentTier(known, name)?.let { Rarity.color(it, dark) }
}

/**
 * Teksti, jossa turnauksen nimen ensimmäinen esiintymä on [color]issa, tai teksti
 * sellaisenaan kun väriä ei ole. Turnauksen nimi on lauseissa alussa, toisin kuin
 * pelaajan nimi ([textWithPlayerColor]).
 */
internal fun textWithTournamentColor(text: String, name: String?, color: Color?): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val at = name?.takeIf { it.isNotEmpty() }?.let { text.indexOf(it) } ?: -1
        if (color != null && at >= 0) addStyle(SpanStyle(color = color), at, at + name!!.length)
    }

/**
 * Lauseen teksti, jossa pelaajan nimi on rarity-värissä (esim. `vs frans`). Nimen viimeinen
 * esiintymä väritetään, koska nimi on lauseissa lopussa. Ilman tunnettua ratingia teksti
 * palaa sellaisenaan.
 */
@Composable
internal fun textWithPlayerColor(text: String, name: String?, userId: String?): AnnotatedString {
    val color = playerRarityColor(userId, name)
    return buildAnnotatedString {
        append(text)
        val at = name?.takeIf { it.isNotEmpty() }?.let { text.lastIndexOf(it) } ?: -1
        if (color != null && at >= 0) addStyle(SpanStyle(color = color), at, at + name!!.length)
    }
}
