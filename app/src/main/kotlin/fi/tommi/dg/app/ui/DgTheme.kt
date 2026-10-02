package fi.tommi.dg.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import fi.tommi.dg.app.session.AppTheme

/**
 * Sovelluksen oma väriteema tekstinäkymille (otteluluettelo, viestit, asetukset, manuaali).
 *
 * Olemassaolon syy on mitattu eikä esteettinen: paljas `MaterialTheme {}` ajoi Material 3:n
 * oletusvaaleaa palettia, jonka violettia `#6750A4` kukaan ei ollut valinnut (värikatsaus
 * 24.8.2026, `docs/UI.md`). Primary on Tommin sanoin "taolaisempi kehyspuu": laudan
 * kehysväri `#6E4C33` hillittynä, eli kylläisyys alas ja sävy harmaan luonnonpuun suuntaan.
 * Kontrasti taustaa `#FEF7FF` vasten on 6,43:1, AA-raja on 4,5:1.
 *
 * **Pintapaletti asetettiin 6.9.2026, ja syy on mitattu eikä esteettinen.** Tommin havainto
 * oli että vaalea teema on väritön ja tylsä, ja koodi kertoi miksi: primary oli ainoa asetettu
 * arvo, joten kaikki muu tuli Material 3:n oletuksista, jotka ovat violetin sävyisiä. Ne
 * kolme jotka näkyvät eniten, on laskettu lähteestä eikä arvattu: `onSurfaceVariant` on
 * sovelluksen käytetyin väri (59 kohtaa), `primary` toiseksi käytetyin (28) ja
 * `surfaceVariant` piirtää viisi pintaa. Oletukset `#49454F`, `#E7E0EC` ja `#FEF7FF` ovat
 * kaikki violettiin taittavia, eli väritön vaikutelma syntyi juuri niistä.
 *
 * **Sävy on sama kehyspuu kuin primaryssa, ei uusi väri.** Neutraalit taittavat lämpimään
 * (ruskean suuntaan) siellä missä oletukset taittoivat violettiin, joten paletti on yksi
 * perhe eikä kaksi. Kylläisyys on matala samasta syystä kuin primaryssa: väri saa erottaa,
 * muttei huutaa.
 *
 * **Kontrastit mitattu uutta taustaa `#FBF8F4` vasten, AA-raja on 4,5:1.** `onSurface`
 * `#1E1A16` on 16,33:1, `onSurfaceVariant` `#4C443B` on 9,03:1 taustalla ja 7,31:1
 * `surfaceVariant`illa, ja `primary` on 6,39:1 (oli 6,43:1 vanhaa taustaa vasten, eli ero on
 * kohinaa). `outline` `#7E756A` on 4,28:1, mikä ylittää käyttöliittymäelementin 3:1-rajan
 * muttei tekstin rajaa; sitä ei käytetä tekstiin.
 *
 * **Taustakuva on eri kysymys eikä se ratkea täällä** (`docs/AVOIMET.md`). Tämä paletti antaa
 * sille pohjan kummassakin tapauksessa, koska kuva tarvitsee alleen värin joka tapauksessa.
 *
 * Lautanäkymä ei käytä tätä lainkaan: sen värit ovat `DgBoard.Palette` (X-22, mitattu
 * Monte Carlosta).
 */
internal val DgColorScheme = lightColorScheme(
    primary = Color(0xFF6B5844),
    primaryContainer = Color(0xFFE9DCCB),
    onPrimaryContainer = Color(0xFF241A0E),
    secondaryContainer = Color(0xFFE7DFD2),
    onSecondaryContainer = Color(0xFF2A231A),
    background = Color(0xFFFBF8F4),
    onBackground = Color(0xFF1E1A16),
    surface = Color(0xFFFBF8F4),
    onSurface = Color(0xFF1E1A16),
    surfaceVariant = Color(0xFFE8E0D6),
    onSurfaceVariant = Color(0xFF4C443B),
    surfaceDim = Color(0xFFDFD9D1),
    surfaceBright = Color(0xFFFBF8F4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F1EB),
    surfaceContainer = Color(0xFFF1EBE4),
    surfaceContainerHigh = Color(0xFFEBE5DD),
    surfaceContainerHighest = Color(0xFFE5DFD7),
    outline = Color(0xFF7E756A),
    outlineVariant = Color(0xFFD0C7BC),
)

/**
 * Sama teema yötilassa (auditoinnin D5, tehty 26.8.2026).
 *
 * Kaksi väriä on asetettu, ja molemmilla on syynsä; muut tulevat Material 3:n tummasta
 * oletuspaletista, joka on suunniteltu tummalle taustalle eikä ole tässä arvaus.
 *
 * **Primary on sama puu vaaleampana.** Vaalean teeman `#6B5844` on tummaa taustaa vasten
 * 1,84:1 eli lukukelvoton, joten sävy on sama mutta vaaleampi: `#CBB093` on oletustaustaa
 * `#1C1B1F` vasten 8,2:1.
 *
 * **Tertiary on nimetty tässä siksi, ettei se saa törmätä erroriin.** Otteluluettelo merkitsee
 * kaksi aikatilaa eri väreillä (`MatchClock`): nollaan tullut `Grace` tertiaryllä ja vähiin
 * käynyt `Time Pool` errorilla. Material 3:n tummat oletukset ovat `#EFB8C8` ja `#F2B8B5`,
 * eli kaksi lähes samaa vaaleanpunaista, jolloin ero katoaisi juuri siltä riviltä jota
 * varten merkintä on olemassa. `#E6C36A` on kellertävä ja erottuu punaisesta myös
 * harmaasävyisenä (10,0:1 taustaa vasten). Vaaleassa teemassa oletukset erottuvat toisistaan
 * (`#7D5260` ja `#B3261E`), joten siihen ei kosketa.
 */
internal val DgDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCBB093),
    tertiary = Color(0xFFE6C36A),
)

/**
 * Teeman valinta laitteen yöasetuksesta.
 *
 * **Asetusta noudatetaan, sitä ei tarjota uudestaan sovelluksessa.** Vaihtoehtona olisi ollut
 * oma kolmiasentoinen valinta (vaalea, tumma, järjestelmä), mutta se olisi toinen paikka
 * jossa sama asia päätetään, ja auditoinnin vaatimus on juuri järjestelmän asetuksen
 * noudattaminen.
 *
 * Lautaruutu ei muutu yötilassa, ja se on rajaus eikä puute: laudan värit ovat mitattu
 * kokonaisuus (X-22) jonka tausta on jo lähes musta, eikä sitä voi kääntää vaaleaksi tai
 * tummemmaksi paloittain rikkomatta juuri sitä mittaa.
 */
/**
 * Välilehden oma aksenttiväri (Tommin päätös 29.8.2026: *"kolmonen ilman ottelulistaa"*).
 *
 * **Otteluluettelo on tarkoituksella ilman omaa sävyä** ja pitää teeman perusprimaryn, eli
 * saman kehyspuun kuin ennenkin. Syy on että sen rivit ovat jo värillisiä siellä missä väri
 * merkitsee jotain: `MatchClock` maalaa nollaan tulleen gracen tertiaryllä ja vähiin käyneen
 * poolin errorilla. Kolmas sävy samalla rivillä kilpailisi niiden kanssa.
 *
 * **Sävyt on mitattu eikä valittu silmällä.** Kontrasti tummaa oletustaustaa `#1C1B1F` ja
 * vaaleaa taustaa vasten, AA-raja on 4,5:1. **Vaalean sarakkeen luvut mitattiin uudestaan
 * 6.9.2026**, kun tausta vaihtui oletuksesta `#FEF7FF` omaan `#FBF8F4`:ään; sävyt itse eivät
 * muuttuneet, ja jokainen luku laski neljä sadasosaa, eli järjestys ja johtopäätös pysyivät:
 *
 * | Välilehti | Tumma | | Vaalea | |
 * |---|---|---|---|---|
 * | Matches (perus) | `#CBB093` | 8,30:1 | `#6B5844` | 6,39:1 |
 * | Lounge | `#A8C69A` | 9,14:1 | `#2A7430` | 5,45:1 |
 * | Discussion | `#9EBBD6` | 8,59:1 | `#1565A8` | 5,73:1 |
 * | Messages | `#C4AAD3` | 8,19:1 | `#8034A0` | 6,80:1 |
 * | Info | `#93C6C0` | 9,04:1 | `#00706B` | 5,62:1 |
 *
 * Kaikki ylittävät rajan, ja tummat sävyt pitävät saman vaimean luonteen kuin kehyspuu.
 *
 * **Vaalean sarakkeen kylläisyys nostettiin 6.9.2026, ja syy on Tommin havainto**
 * *"perus-moodin välilehtien värejä en nykyisellään erota toisistaan helposti"*. Aiemmat
 * sävyt (`#4C6B41`, `#3F5F7A`, `#5F4A6B`, `#3D6862`) olivat kaikki matalan kylläisyyden
 * tummia värejä lähes samalla vaaleudella, joten ne erottuivat taustasta muttei toisistaan.
 * Ero mitattiin CIELAB-etäisyytenä: pienin pari oli 20 (`Discussion` ja `Messages`), ja uusilla
 * sävyillä se on 37 (`Lounge` ja `Info`). Kontrastivaatimus ei löystynyt, ja kuvio on laskettu
 * mukaan: matalinkin sävy on 5,00:1 kuvion tummimman kohdan `#F2EEE9` päällä.
 *
 * **Tumma sarake jätettiin ennalleen**, koska havainto koski vaaleaa. Tummat sävyt ovat
 * vaalean puolen värejä eri tehtävässä: siellä ne erottuvat mustasta taustasta, eikä samaa
 * ongelmaa ole raportoitu.
 *
 * **Väri ei ole ainoa merkki, ja se on tässä olennaista.** Vihreä ja turkoosi (Lounge, Info)
 * sekä sininen ja violetti (Discussion, Messages) voivat lähestyä toisiaan värisokealla
 * lukijalla. Välilehdellä on aina myös nimi, joten sävy on vahvistus eikä tunniste.
 */
internal fun accentFor(tab: DgTab?, dark: Boolean): Color? = when (tab) {
    null, DgTab.Matches -> null
    DgTab.Lounge -> if (dark) Color(0xFFA8C69A) else Color(0xFF2A7430)
    DgTab.Discussion -> if (dark) Color(0xFF9EBBD6) else Color(0xFF1565A8)
    DgTab.Messages -> if (dark) Color(0xFFC4AAD3) else Color(0xFF8034A0)
    DgTab.Info -> if (dark) Color(0xFF93C6C0) else Color(0xFF00706B)
}

/**
 * Ruudun otsikkorivin värit: otsikko on välilehden sävyä (Tommin päätös 29.8.2026).
 *
 * Sääntö on tässä eikä kahdeksassa ruudussa, jotta otsikot eivät pääse eriytymään
 * toisistaan. Arvo on `MaterialTheme.colorScheme.primary`, eli sama jonka [DgTabAccent]
 * on jo korvannut välilehden sävyllä; otteluluettelo ja välilehtien ulkopuoliset ruudut
 * (lauta, porautumisruutu) saavat siitä perusteeman kehyspuun.
 *
 * Vain otsikko värjätään. Nappien (`Refresh`, `Back`) väri tulee jo primarysta, ja
 * ikonien jättäminen oletuksiin pitää rivin muuten Material 3:n omana.
 *
 * **Tausta on lukupinta 6.9.2026 alkaen** (`DgReadingSurface.kt`), koska otsikkorivi on
 * tekstiä ja toimintoja kuvion päällä siinä missä listakin. Ennen sitä tausta oli oletus,
 * ja se toimi vain koska kuvio oli näkymättömyyden rajalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun dgTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = dgReadingSurfaceColor(),
    titleContentColor = MaterialTheme.colorScheme.primary,
)

/**
 * Välilehden nimen väri palkissa, myös silloin kun välilehti ei ole valittuna.
 *
 * **Perusprimary luetaan skeemasta eikä `MaterialTheme`sta**, ja se on ero jonka huomaa vasta
 * ruudulta: palkki piirtyy [DgTabAccent]in sisällä, joten `MaterialTheme.colorScheme.primary`
 * on siinä kohtaa *nykyisen* välilehden sävy. Ilman tätä otteluluettelon nimi olisi ottanut
 * naapurinsa värin sen mukaan millä välilehdellä ollaan.
 */
internal fun tabLabelColor(tab: DgTab, dark: Boolean, deco: Boolean = false): Color =
    if (deco) (if (dark) DecoColorScheme else DecoLightColorScheme).primary else accentFor(tab, dark) ?: (if (dark) DgDarkColorScheme else DgColorScheme).primary

/**
 * Kietoo sisällön välilehden omaan sävyyn korvaamalla `primary`n.
 *
 * **Korvattava on juuri primary eikä uusi oma väri**, koska ruudut käyttävät jo primarya
 * kaikkeen mikä on aksenttia: `Refresh`, `Back`, osastootsikot, `Old Threads`, linkkisivun
 * alleviivaus ja välilehtipalkin korostus. Yksi arvo siis riittää, eikä yhtäkään ruutua
 * tarvinnut muuttaa tämän takia.
 *
 * Tyhjä aksentti (otteluluettelo, lauta, porautumisruutu) jättää teeman koskematta.
 */
@Composable
internal fun DgTabAccent(tab: DgTab?, content: @Composable () -> Unit) {
    // Decossa kaikki välilehdet ovat kultaa: nimi tunnistaa välilehden, sävy ei ole ainoa merkki.
    val accent = if (LocalAppTheme.current == AppTheme.DECO) null else accentFor(tab, dgDark())
    if (accent == null) {
        content()
    } else {
        MaterialTheme(
            // `onPrimary` on korvattava primaryn mukana, muuten täytetyn napin teksti jää
            // oletuspaletin violettiin: mitattu laitteella 29.8.2026, Loungen `Join` oli
            // vihreä nappi jossa luki tummanvioletti teksti. Sävyt eivät ole aksentteja
            // vaan neutraaleja, koska tekstin tehtävä on lukua eikä väriä.
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = accent,
                onPrimary = if (dgDark()) Color(0xFF1C1B1F) else Color.White,
            ),
            content = content,
        )
    }
}

/** Valittu teema; [DgTheme] tarjoaa, ja [dgDark] sekä välilehtien sävyt lukevat. */
internal val LocalAppTheme = compositionLocalOf { AppTheme.PLAIN }

/**
 * Onko näkymä tumma. Laite päättää, myös Decossa 26.9.2026 alkaen (Tommin valinta: Deco seuraa
 * laitetta, `docs/ASETUKSET.md` › Vaalea Deco). Kaikki jotka valitsevat vaalean ja tumman
 * välillä lukevat tämän eivätkä `isSystemInDarkTheme`ia suoraan, jotta teeman oma poikkeus
 * pysyy yhdessä paikassa jos sellainen vielä tulee.
 */
@Composable
internal fun dgDark(): Boolean = isSystemInDarkTheme()

/**
 * Piirretäänkö Deco-lauta vaaleana: teema on Deco ja laite vaaleassa tilassa. Plain-teemassa
 * valittu Deco-lauta pysyy tummana, koska vaalea lauta kuuluu vaalean Decon teemaan eikä
 * lautatyyliin (valinta 3: lautaruudun ympäristö on musta molemmissa).
 */
@Composable
internal fun decoLightBoard(): Boolean = LocalAppTheme.current == AppTheme.DECO && !dgDark()

/**
 * Deco-teeman värit (Tommin tilaus 22.9.2026). Kulta primaryna, musta emali taustana ja
 * norsunluu tekstinä. Tertiary on jade eikä kullan sävy, koska otteluluettelon `Grace`
 * käyttää sitä ja sen on erotuttava sekä primarysta että errorista (ks. [DgDarkColorScheme]).
 * Kontrastit mustaa `#121212` vasten: norsunluu `#F3E6CC` 15,6:1, kulta `#D4AF37` 9,4:1,
 * hiljainen `#C9B98F` 10,0:1 ja jade `#7FC8B6` 10,1:1; AA-raja on 4,5:1.
 */
internal val DecoColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF141414),
    primaryContainer = Color(0xFF3A3020),
    onPrimaryContainer = Color(0xFFF3E6CC),
    secondaryContainer = Color(0xFF2A2620),
    onSecondaryContainer = Color(0xFFF3E6CC),
    tertiary = Color(0xFF7FC8B6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF3E6CC),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF3E6CC),
    surfaceVariant = Color(0xFF26231D),
    onSurfaceVariant = Color(0xFFC9B98F),
    surfaceDim = Color(0xFF0E0E0E),
    surfaceBright = Color(0xFF2A2620),
    surfaceContainerLowest = Color(0xFF0B0B0B),
    surfaceContainerLow = Color(0xFF171614),
    surfaceContainer = Color(0xFF1B1A17),
    surfaceContainerHigh = Color(0xFF22201C),
    surfaceContainerHighest = Color(0xFF2A2721),
    outline = Color(0xFF8C7440),
    outlineVariant = Color(0xFF3A3222),
)

/**
 * Vaalean Decon värit (Tommin valinta 26.9.2026, paletti A: norsunluu ja messinki). Tumman
 * Decon käänteinen: norsunluutausta, lähes musta teksti ja tumma messinki aksenttina, koska
 * tumman Decon kulta `#D4AF37` jää norsunluulla tekstinä 1,8:aan. Kontrastit norsunluuta
 * `#F6EFE0` / luettelon riviä `#EDE3CC` vasten: teksti 13,4 / 12,1, messinki 5,4 / 4,9,
 * hiljainen 5,6 / 5,0 ja jade 5,5 / 5,0. Napit pysyvät mustana lakkana ([DecoButtons.kt]).
 */
internal val DecoLightColorScheme = lightColorScheme(
    primary = Color(0xFF7A5C14),
    onPrimary = Color(0xFFF6EFE0),
    primaryContainer = Color(0xFFEBDDB8),
    onPrimaryContainer = Color(0xFF2A241A),
    secondaryContainer = Color(0xFFEDE3CC),
    onSecondaryContainer = Color(0xFF2A241A),
    tertiary = Color(0xFF1F6B5C),
    background = Color(0xFFF6EFE0),
    onBackground = Color(0xFF2A241A),
    surface = Color(0xFFF6EFE0),
    onSurface = Color(0xFF2A241A),
    surfaceVariant = Color(0xFFEDE3CC),
    onSurfaceVariant = Color(0xFF6B5D40),
    surfaceDim = Color(0xFFE4D9C1),
    surfaceBright = Color(0xFFFBF7EE),
    surfaceContainerLowest = Color(0xFFFFFCF5),
    surfaceContainerLow = Color(0xFFF9F3E7),
    surfaceContainer = Color(0xFFF3EAD7),
    surfaceContainerHigh = Color(0xFFEDE3CC),
    surfaceContainerHighest = Color(0xFFE7DCC2),
    outline = Color(0xFFC9A14A),
    outlineVariant = Color(0xFFE0D2B0),
)

/**
 * Decon otsikkokirjasin Poiret One (Tommin lupa ladata 22.9.2026). Fontti on SIL Open Font
 * License 1.1 -lisenssillä, ja lisenssiteksti kulkee sovelluksen mukana
 * (`assets/licenses/OFL-PoiretOne.txt`). Leipäteksti pysyy oletuksena, koska ohut kirjasin ei
 * kanna pitkää tekstiä.
 */
internal val PoiretOne = FontFamily(Font(fi.tommi.dg.app.R.font.poiret_one))

private val DecoFont = PoiretOne

private fun TextStyle.deco() = copy(fontFamily = DecoFont, letterSpacing = 0.6.sp)

private val PlainTypography = Typography()

private val DecoTypography = Typography().let {
    it.copy(
        displayLarge = it.displayLarge.deco(),
        displayMedium = it.displayMedium.deco(),
        displaySmall = it.displaySmall.deco(),
        headlineLarge = it.headlineLarge.deco(),
        headlineMedium = it.headlineMedium.deco(),
        headlineSmall = it.headlineSmall.deco(),
        titleLarge = it.titleLarge.deco(),
        titleMedium = it.titleMedium.deco(),
        titleSmall = it.titleSmall.deco(),
    )
}

@Composable
internal fun DgTheme(theme: AppTheme = AppTheme.PLAIN, content: @Composable () -> Unit) {
    // Yksi kutsu eikä haara: kaksi eri `MaterialTheme`-kutsua haaroissa rakensi koko puun
    // uudelleen teeman vaihtuessa, ja navigointi palasi Top Pagelle (laitteella 22.9.2026).
    val deco = theme == AppTheme.DECO
    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = when {
                deco && isSystemInDarkTheme() -> DecoColorScheme
                deco -> DecoLightColorScheme
                isSystemInDarkTheme() -> DgDarkColorScheme
                else -> DgColorScheme
            },
            typography = if (deco) DecoTypography else PlainTypography,
            content = content,
        )
    }
}
