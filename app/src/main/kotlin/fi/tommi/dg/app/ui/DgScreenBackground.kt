package fi.tommi.dg.app.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Ruutujen yhteinen perusväri, maalattuna kerran `MainActivity`ssä.
 *
 * **Perusväri maalataan tässä eikä ruuduissa**, koska ruutujen `Scaffold`ien `containerColor`
 * on 6.9.2026 alkaen läpinäkyvä. Uusi ruutu tarvitsee saman rivin tai se jää ilman taustaa.
 *
 * **Taustakuvio poistettiin 6.10.2026** (Tommin karsinta: asetusten *Lists and screens*
 * -ryhmän kaksi ensimmäistä kytkintä olivat liikaa). Kuutioista linnuiksi muuttuva
 * metamorfoosi, tumman teeman viisi palettia ja sumi-e-tila olivat tiedostossa `DgPattern.kt`,
 * joka on git-historiassa. Vapaan tilan täyttävät nyt vain laitteen kansion taustakuvat
 * (`DgWallpaper.kt`).
 */
@Composable
internal fun Modifier.dgScreenBackground(): Modifier =
    this.background(MaterialTheme.colorScheme.background)
