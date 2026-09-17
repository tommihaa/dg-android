package fi.tommi.dg.app.ui

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Piilottaa tilapalkin ja navigointipalkin niin kauan kuin kutsuva ruutu on näkyvissä.
 *
 * **Tämä on korkeutta eikä ulkoasua.** Sovellus piirtää jo palkkien alle
 * (`enableEdgeToEdge`), mutta sisältö väistää ne `safeDrawingPadding`illa, joten ne vievät
 * käytettävissä olevaa korkeutta oikeasti. Lautaruudulla juuri se korkeus on kiilan korkeus:
 * lauta ei vierity vaan kutistuu, joten jokainen palkeille menevä dp on pois nappulan koosta.
 * Sama budjetti tuotti 8.8.2026 puolikkaan laudan, ks. `docs/UI.md`.
 *
 * **Rajattu yhteen ruutuun tarkoituksella** (Tommin valinta 8.8.2026), ja 20.8.2026 alkaen
 * lisäksi yhteen ehtoon: [enabled]. Kutsuja päättää, ks. alempi kappale palkeista.
 * Otteluluettelo ja
 * asetusruutu ovat vieritettäviä listoja, ja niillä kellonaika, akku ja takaisin-ele ovat
 * hyödyllisempiä kuin muutama rivi lisää. Piilotus on siis siellä missä sen hinta maksetaan
 * takaisin.
 *
 * *Korjaus 10.8.2026:* tässä luki perusteluna myös että listaruuduilla korkeus ei ole pula. Se
 * mitattiin laitteella vääräksi: vaakatilassa otteluluettelon yläosa täytti koko ikkunan ja
 * lista jäi nollan korkuiseksi. Rajaus itse ei muutu, koska sen kantava peruste on että lauta
 * kutistuu eikä vierity. Korkeuspula ratkaistiin siellä missä se syntyi, ks.
 * `MainActivity`in suuntaohjaus ja `TopScreen`.
 *
 * `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`: palkit saa hetkeksi näkyviin pyyhkäisemällä
 * reunasta, eivätkä ne silloin muuta asettelua. Ilman tätä käyttäjä ei pääsisi
 * järjestelmän omiin toimintoihin lainkaan tältä ruudulta.
 *
 * *Korjaus 20.8.2026:* tässä luki että sovelluksen oma paluu ei ole tämän varassa, koska
 * lautaruudulla on oma `Back`-nappinsa nappisarakkeessa. Väite oli vanhentunut eikä väärin
 * kirjoitettu: nappisarake purettiin 9.8.2026, eikä `BoardScreen`issä ole `Back`ia missään
 * tilassa. `docs/UI.md` sanoi tämän oikein koko ajan, joten sama asia eli kahtena totuutena
 * kymmenen päivää. Poistuminen on siis kokonaan tämän eleen varassa niin kauan kuin palkit
 * ovat piilossa, ja juuri se on [enabled]-ehdon syy.
 *
 * **Ruudun omat tiedot eivät seuraa palkkeja, eivätkä ne voi seurata.** 9.8.2026 ne sidottiin
 * tähän eleeseen `WindowInsets.areStatusBarsVisible`in kautta, ja 10.8.2026 mitattiin laitteella
 * ettei se toimi: transient-palkit on valittu juuri siksi etteivät ne muuta insettejä, joten
 * lippu ei muutu koskaan. Tiedot tulevat nyt vedosta alaspäin, ks. `BoardScreen`in
 * `TransientInfo`. Palkit ja tiedot ovat siis kaksi eri elettä.
 *
 * **Sama pyyhkäisy on molemmilla laitteilla myös ainoa tie takaisin.** Sekä Pixel 8a että
 * Galaxy Tab S7+ ovat kolmen napin tilassa (`navigation_mode` on `0`, mitattu 10.8. ja
 * 20.8.2026), joten reunasta tehtävää takaisin-elettä ei ole kummassakaan: pyyhkäisy
 * paljastaa palkit ja `◀` vie takaisin. Poistuminen on siis kaksi tekoa.
 *
 * **Ja siksi piilotus on nyt ehdollinen** ([enabled], `DgBoard.barsAreFree`). Tommi
 * 20.8.2026: *"ainakin tabletille pitää saada ne kolme nappia tai lisää eleitä"*. Tabletilla
 * nappula on jo `CHECKER_MAX`issa, joten piilotus ei osta yhtään dp:tä mutta maksaa
 * poistumistien; puhelimella se ostaa neljänneksen nappulan koosta. Ehto on siis mitta eikä
 * laite, eikä sen ratkaisemiseen tarvittu uutta elettä.
 *
 * Palautus tapahtuu `onDispose`ssa eikä ruudulta poistuttaessa käsin, jotta palkit palaavat
 * myös silloin kun ruutu katoaa muusta syystä kuin takaisin-painalluksesta: katkennut
 * istunto vie lautaruudulta pois ilman että kukaan painoi mitään.
 */
@Composable
fun HideSystemBarsWhileVisible(enabled: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(view, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val window = view.context.findActivity()?.window
            ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val bars = WindowInsetsCompat.Type.systemBars()

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(bars)

        onDispose { controller.show(bars) }
    }
}

/**
 * Vaihtaa palkkien ikonit vaaleiksi niin kauaksi aikaa kuin kutsuva ruutu on näkyvissä.
 *
 * **Tämä on lautaruudun poikkeus koko sovelluksen sääntöön, ja ehto on taustan väri.**
 * `MainActivity` kertoo palkeille että tausta on vaalea, koska sovelluksella ei ole tummaa
 * teemaa. Lautaruutu on ainoa poikkeus: sen tausta on `DgBoard.Palette.PanelBg` eli
 * `0xFF1B1712`, käytännössä musta. Ilman tätä siellä olisi tummat ikonit mustalla, eli
 * täsmälleen sama vika jonka korjaamiseksi `MainActivity`in rivit kirjoitettiin, vain
 * toisin päin.
 *
 * **Tämä ei ollut tarpeen ennen 20.8.2026**, koska lautaruudulla palkit olivat aina
 * piilossa. Sen jälkeen ne ovat isolla laitteella näkyvissä ([HideSystemBarsWhileVisible]
 * ja `DgBoard.barsAreFree`), joten laudan päälle piirtyy nyt palkki jonka ikonit pitää
 * erottua mustasta. Pienelläkin laitteella tämä on tarpeen, koska piilotetut palkit saa
 * hetkeksi esiin pyyhkäisemällä, ja ne tulevat silloin saman mustan päälle.
 *
 * Erillinen [HideSystemBarsWhileVisible]istä eikä sen sisällä, koska ehdot ovat eri: ikonit
 * koskevat kaikkia laitteita aina, piilotus vain niitä joilla palkit maksavat korkeutta.
 * Yhdistettynä kutsuja joutuisi antamaan piilotuksen ehdon myös ikoneille, ja ne menisivät
 * väärin juuri sillä laitteella jolla palkit näkyvät.
 */
@Composable
fun DarkSystemBarIconsWhileVisible() {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
            ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNavigation = controller.isAppearanceLightNavigationBars

        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        // Palautus entiseen eikä vakioon `true`, samasta syystä kuin palkkien palautus
        // tapahtuu `onDispose`ssa: ruutu voi kadota muustakin syystä kuin takaisin-eleestä,
        // eikä tämä saa päättää mitä sen jälkeen tulee.
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}

/**
 * Composablen konteksti ei ole aina suoraan `Activity`, koska teemat ja muut kerrokset
 * kääriytyvät sen ympärille. Kääre puretaan silmukalla eikä yhdellä castilla, jotta piilotus
 * ei jää hiljaa tekemättä väärän tyyppisen kontekstin takia.
 */
internal tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
