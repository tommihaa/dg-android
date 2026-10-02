package fi.tommi.dg.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import fi.tommi.dg.domain.ArrowEnd
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.MoveArrow
import kotlin.math.hypot

/**
 * Kootun siirron nuolet laudan päällä (Tommin idea ja kuittaus 29.9.2026, `MoveArrowsStore`).
 *
 * **Kerros eikä osa kiilaa**, koska nuoli kulkee pisteeltä toiselle muurin ja keskikaistan
 * yli, eikä yksikään laudan composable omista sitä tilaa. Laudan periaate on tavalliset
 * composablet eikä Canvas (`docs/UI.md`, semantiikkapuu), ja poikkeus on rajattu tähän:
 * nuoli on koriste joka toistaa jo piirretyn laudan ja nopat, eikä sillä ole tekoa. Canvas
 * ilman kosketuskäsittelyä ei myöskään ota napautuksia, joten kiilat ottavat ne kuten ennen.
 *
 * Paikat lasketaan samoista mitoista kuin lauta: 13 saraketta (6 + muuri + 6), kiilan
 * korkeus ja pinon askel [CheckerStack]in kaavalla. Geometria on [arrowLines]issa, jotta sen
 * voi testata ilman ruutua.
 *
 * **Sama kerros piirtää vastustajan edellisen siirron** (29.9.2026 illalla, `OpponentMove.kt`):
 * [color] on nuolten nappuloiden väri ja [barOnTop] kertoo kummassa muurin segmentissä ne ovat.
 * Oma nappula on yläsegmentissä, vastustajan alasegmentissä ([BarSegment]).
 */
@Composable
internal fun MoveArrowLayer(
    arrows: List<MoveArrow>,
    board: BoardState,
    color: CheckerColor,
    trayWidth: Dp,
    checkerSize: Dp,
    wedgeHeight: Dp,
    bandHeight: Dp,
    mirrored: Boolean,
    fill: Color,
    halo: Color,
    barOnTop: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val frame = ArrowFrame(
            width = size.width,
            trayWidth = trayWidth.toPx(),
            bandWidth = DgBoard.FRAME_PAD_H.toPx(),
            wedgeHeight = wedgeHeight.toPx(),
            bandHeight = bandHeight.toPx(),
            checkerSize = checkerSize.toPx(),
            mirrored = mirrored,
        )
        arrowLines(arrows, board, color, frame, barOnTop).forEach { drawArrow(it, frame.checkerSize, fill, halo) }
    }
}

/** Laudan mitat pikseleinä nuolen laskentaa varten. Ks. [MoveArrowLayer]. */
internal data class ArrowFrame(
    val width: Float,
    val trayWidth: Float,
    val bandWidth: Float,
    val wedgeHeight: Float,
    val bandHeight: Float,
    val checkerSize: Float,
    val mirrored: Boolean,
)

/** Nuolen häntä ja kärki nappuloiden keskipisteinä. */
internal data class ArrowLine(val fromX: Float, val fromY: Float, val toX: Float, val toY: Float)

/**
 * Nuolet pikseleinä. [board] on piirretty lauta (omille nuolille kokoamisen `boardNow`), josta luetaan
 * pinojen nykyiset korkeudet: pinon askel riippuu nappuloiden määrästä, joten sama paikka on
 * eri korkeudella viiden ja kahdeksan nappulan pinossa.
 */
internal fun arrowLines(
    arrows: List<MoveArrow>,
    board: BoardState,
    color: CheckerColor,
    frame: ArrowFrame,
    barOnTop: Boolean = true,
): List<ArrowLine> {
    val playX = if (frame.mirrored) frame.trayWidth + frame.bandWidth else 0f
    val unit = (frame.width - frame.trayWidth - frame.bandWidth) / BOARD_COLUMNS
    val bottomEdge = frame.wedgeHeight * 2 + frame.bandHeight

    fun columnX(column: Int) = playX + (column + 0.5f) * unit

    fun ownCount(point: Int): Int =
        board.points.firstOrNull { it.number == point && it.owner == color }?.count ?: 0

    fun at(end: ArrowEnd, fromTop: Boolean): Offset = when (end) {
        is ArrowEnd.OnPoint -> {
            val offset = stackOffset(ownCount(end.point), end.slot, frame.wedgeHeight, frame.checkerSize)
            Offset(
                columnX(pointColumn(end.point, frame.mirrored)),
                if (isTopRow(end.point)) offset else bottomEdge - offset,
            )
        }
        is ArrowEnd.OnBar -> {
            // Oma nappula on muurin yläsegmentissä ja pinoutuu keskikaistasta ylöspäin
            // (6.9.2026, [BarSegment]). Vastustajan nappula on alasegmentissä ja pinoutuu
            // keskikaistasta alaspäin.
            val onBar = board.bar.firstOrNull { it.color == color }?.count ?: 0
            val offset = stackOffset(onBar, end.slot, frame.wedgeHeight, frame.checkerSize)
            val y = if (barOnTop) {
                frame.wedgeHeight - offset
            } else {
                frame.wedgeHeight + frame.bandHeight + offset
            }
            Offset(columnX(BAR_COLUMN), y)
        }
        // Lokerosarakkeen keskelle sen puoliskon kohdalle, jolta nappula kannettiin.
        ArrowEnd.Off -> Offset(
            if (frame.mirrored) frame.trayWidth / 2 else frame.width - frame.trayWidth / 2,
            if (fromTop) frame.wedgeHeight / 2 else bottomEdge - frame.wedgeHeight / 2,
        )
    }

    return arrows.map { arrow ->
        val fromTop = when (val from = arrow.from) {
            is ArrowEnd.OnPoint -> isTopRow(from.point)
            else -> true
        }
        val tail = at(arrow.from, fromTop)
        val head = at(arrow.to, fromTop)
        ArrowLine(tail.x, tail.y, head.x, head.y)
    }
}

/**
 * Nuolen reuna täytön mukaan: tumma täyttö saa vaalean reunan, vaalea tumman (Tommin valinta
 * 29.9.2026 illalla). Musta vastustajan nuoli tummalla laudalla ja mustien nappuloiden päällä
 * erottui tumman reunan kanssa huonosti (`sessio-29-9-ilta3`, Verneriin ottelu).
 */
internal fun arrowHalo(fill: Color): Color = if (fill.luminance() < 0.35f) LIGHT_HALO else DgBoard.Palette.Outline

private val LIGHT_HALO = Color(0xFFEDE6D6)

private const val BOARD_COLUMNS = 13
private const val BAR_COLUMN = 6

private fun isTopRow(point: Int) = point >= 13

/**
 * Pisteen sarake 0..12 vasemmalta, muuri on 6. Sama järjestys kuin `topNumbers` ja
 * `bottomNumbers` laudalla: peilaamattomana ylärivi 13..24 ja alarivi 12..1 vasemmalta.
 */
internal fun pointColumn(point: Int, mirrored: Boolean): Int {
    val position = when {
        isTopRow(point) && !mirrored -> point - 13
        isTopRow(point) -> 24 - point
        !mirrored -> 12 - point
        else -> point - 1
    }
    return if (position < 6) position else position + 1
}

/**
 * Pinon paikan [slot] keskipisteen etäisyys kannasta, [CheckerStack]in kaavalla. Pino
 * mitoitetaan vähintään paikan korkuiseksi, koska lähtöpisteen nappula ei ole enää pinossa
 * mutta nuolen häntä osoittaa siihen kohtaan jossa se oli.
 */
internal fun stackOffset(count: Int, slot: Int, available: Float, checkerSize: Float): Float {
    val minStep = checkerSize * DgBoard.MIN_STACK_STEP_RATIO
    val drawable = (1 + ((available - checkerSize) / minStep).toInt()).coerceIn(1, DgBoard.MAX_CHECKERS)
    val visible = minOf(maxOf(count, slot + 1), drawable)
    val step = if (visible <= 1) checkerSize else minOf(checkerSize, (available - checkerSize) / (visible - 1))
    return checkerSize / 2 + step * minOf(slot, visible - 1)
}

/**
 * Yksi nuoli: tumma reuna ensin ja oma väri sen päälle, jotta nuoli erottuu sekä huovasta
 * että saman värisistä nappuloista. Kärki jää nappulan sisään eikä sen keskelle, jotta
 * nappulan luku (pinon määrä) pysyy luettavana.
 */
private fun DrawScope.drawArrow(line: ArrowLine, checkerSize: Float, fill: Color, halo: Color) {
    val dx = line.toX - line.fromX
    val dy = line.toY - line.fromY
    val length = hypot(dx, dy)
    if (length < 1f) return
    val ux = dx / length
    val uy = dy / length
    val shaft = checkerSize * 0.13f
    val edge = checkerSize * 0.05f
    val headLength = checkerSize * 0.5f
    val headHalf = checkerSize * 0.28f

    val tip = Offset(line.toX - ux * checkerSize * 0.2f, line.toY - uy * checkerSize * 0.2f)
    val base = Offset(tip.x - ux * headLength, tip.y - uy * headLength)
    val start = Offset(line.fromX, line.fromY)
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x - uy * headHalf, base.y + ux * headHalf)
        lineTo(base.x + uy * headHalf, base.y - ux * headHalf)
        close()
    }

    drawLine(halo, start, base, strokeWidth = shaft + edge * 2, cap = StrokeCap.Round)
    drawPath(head, halo, style = Stroke(width = edge * 2))
    drawLine(fill, start, base, strokeWidth = shaft, cap = StrokeCap.Round)
    drawPath(head, fill)
}
