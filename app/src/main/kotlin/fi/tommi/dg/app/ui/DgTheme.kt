package fi.tommi.dg.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
internal fun tabLabelColor(tab: DgTab, dark: Boolean): Color =
    accentFor(tab, dark) ?: (if (dark) DgDarkColorScheme else DgColorScheme).primary

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
    val accent = accentFor(tab, isSystemInDarkTheme())
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
                onPrimary = if (isSystemInDarkTheme()) Color(0xFF1C1B1F) else Color.White,
            ),
            content = content,
        )
    }
}

@Composable
internal fun DgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DgDarkColorScheme else DgColorScheme,
        content = content,
    )
}
