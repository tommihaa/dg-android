package fi.tommi.dg.app.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.session.AppTheme

/**
 * Laudan napit Deco-teemassa (Tommin toive ja valinta 24.9.2026: *"art deco animaatiot ovat
 * tyylikkäitä, voisiko nappeja koristella lisää?"*, valinta A ja B yhdessä). Muoto on
 * viistetty kahdeksankulmio kuten laudan kuutio, ja sen sisällä kulkee samanmuotoinen ohut
 * messinkiviiva.
 *
 * **Viiva on napin sisällä eikä ulkopuolella**, toisin kuin chatin mockupissa. Pinon väli
 * on 6 dp, ja ulkopuolinen viiva 3 dp:n päässä olisi koskettanut naapurinapin viivaa. Sisällä
 * se ei vie tilaa eikä muuta asettelua.
 *
 * Vain Deco-teemassa (kuudes arvo: oletusasetuksilla perusmuoto). Muissa teemoissa muoto on
 * [DgBoard.BUTTON_SHAPE] eikä viivaa piirretä.
 */
@Composable
internal fun boardButtonShape(): Shape =
    if (LocalAppTheme.current == AppTheme.DECO) DECO_BUTTON_SHAPE else DgBoard.BUTTON_SHAPE

/** Sisäviiva Deco-teemassa, muuten ei mitään. Piirretään sisällön päälle, koska tausta on napin oma. */
@Composable
internal fun Modifier.decoButtonLine(): Modifier =
    if (LocalAppTheme.current != AppTheme.DECO) this else drawWithContent {
        drawContent()
        val inset = DECO_LINE_INSET.toPx()
        val cut = DECO_CUT.toPx() - inset * 0.6f
        val l = inset
        val t = inset
        val r = size.width - inset
        val b = size.height - inset
        val path = Path().apply {
            moveTo(l + cut, t)
            lineTo(r - cut, t)
            lineTo(r, t + cut)
            lineTo(r, b - cut)
            lineTo(r - cut, b)
            lineTo(l + cut, b)
            lineTo(l, b - cut)
            lineTo(l, t + cut)
            close()
        }
        drawPath(path, DECO_BUTTON_LINE, style = Stroke(width = 1.dp.toPx()))
    }

private val DECO_CUT = 7.dp
private val DECO_LINE_INSET = 4.dp
private val DECO_BUTTON_SHAPE = CutCornerShape(DECO_CUT)

/**
 * Messinki tummemmaksi kuin [Deco.Gold], koska viiva kulkee myös kermaisella täyttönapilla,
 * jolla vaalea kulta jäisi heikoksi. Tummalla ääriviivanapilla sama sävy lukee kultana.
 */
private val DECO_BUTTON_LINE = Color(0xFFA8842A)
