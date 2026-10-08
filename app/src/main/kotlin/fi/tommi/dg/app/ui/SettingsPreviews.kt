package fi.tommi.dg.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fi.tommi.dg.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.session.BusyStyle
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.CompositionSession
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.scrape.BoardParser
import kotlinx.coroutines.delay

/**
 * Asetusruudun vaihtoehdot kortteina esikatseluineen (Tommin tilaus 23.9.2026: *"preview per
 * vaihtoehto"*, valinta C chatin neljästä: kortit vierekkäin, ja tarkennus *"mielellään
 * pelitilanteista"*). Kortti korvaa radiorivin viidessä valintaryhmässä; kytkimet jäävät
 * riveiksi, koska ne kuvaavat tekoa eivätkä ulkoasua.
 *
 * Esikatselu piirtää yhden oikean pelitilanteen sovelluksen omilla komponenteilla, ks.
 * [PreviewBoard]. Tilanne on anonymisoitu fixture `move_greedy_bearoff.html` (21 pisteen
 * ottelu, kuutio 2:ssa, heitto 3-3), josta yksi askel on jo pelattu. Tupla on valittu siksi,
 * että noppavalinnan ero näkyy vain tuplassa, ja pelattu askel siksi, että harmaantuminen
 * näkyy kummassakin esityksessä.
 */
internal class PreviewSituation(private val page: BoardState, private val composing: CompositionSession?) {
    /** Lauta sellaisena kuin se piirtyy, noppaesitys valinnan mukaan. */
    fun board(siteDice: Boolean): BoardState = composing?.boardNow(siteDice) ?: page
}

private const val PREVIEW_ASSET = "preview/situation.html"

/**
 * Tilanne luetaan kerran ruudun elinaikana. Null jos asset ei jäsenny, jolloin kortit
 * piirtyvät ilman kuvaa eikä ruutu kaadu: esikatselu on lisä eikä valinnan ehto.
 */
@Composable
internal fun rememberPreviewSituation(): PreviewSituation? {
    val context = LocalContext.current
    return remember {
        runCatching {
            val html = context.assets.open(PREVIEW_ASSET).use { it.reader(Charsets.UTF_8).readText() }
            val page = BoardParser.parse(html) ?: return@runCatching null
            val session = LocalComposition.begin(page)
            // Kuutoselta kolmoselle: ensimmäinen askel näkyy laudalla ja yksi noppa harmaantuu.
            val played = session?.legalFromPoints()?.maxOrNull()?.let(session::step)
            PreviewSituation(page, played ?: session)
        }.getOrNull()
    }
}

/** Korttirivi, joka rivittyy kapealla ruudulla. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OptionCards(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}

/**
 * Yksi vaihtoehto: esikatselu ylhäällä, radio ja nimi alla.
 *
 * **Kuvan napautus avaa ison kuvan** (Tommin tilaus 6.10.2026: *"poista suurennuslasi, sen
 * sijaan klikkaamalla kuvaa saa sen ison kuvan"*, ja valinta kaikista kuvista eikä vain
 * laudoista). Siihen asti kuvan kulmassa oli suurennuslasi (valinta 23.9.2026) ja koko kortti
 * valitsi. Nyt kuva on suurennuksen pinta, ja kuvan alla oleva rivi radioineen valitsee.
 * Ison kuvan `Use this` valitsee kuten ennen. [large] piirtää ison kuvan annetussa
 * korkeudessa; null tarkoittaa ettei kuvaa ole mitä suurentaa (Mini), jolloin koko kortti
 * valitsee kuten ennen.
 *
 * [enabled] pois tarkoittaa vaihtoehtoa jota sovellus ei osaa näyttää (Mini, Tommin päätös
 * 25.9.2026). Kortti näkyy himmeänä ja selittää syyn, mutta sitä ei voi valita. Jos se on jo
 * valittuna sivustolla, valinta näkyy ja lähtee lomakkeen mukana muuttumattomana.
 *
 * [checkbox] vaihtaa radion rastiksi, kun kortti on joukon jäsen eikä yksi vaihtoehto
 * (ottelukohtaisen laudan arvontajoukko, Tommin valinta A 27.9.2026).
 */
@Composable
internal fun OptionCard(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    width: Dp,
    large: (@Composable (height: Dp) -> Unit)? = null,
    enabled: Boolean = true,
    checkbox: Boolean = false,
    preview: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    var zoomed by remember { mutableStateOf(false) }
    val zoomLabel = stringResource(R.string.settings_preview_zoom)
    Column(
        modifier = Modifier
            .width(width)
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onSelect,
                role = if (checkbox) Role.Checkbox else Role.RadioButton,
            )
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(8.dp),
    ) {
        Box(
            modifier = if (large != null) {
                // Sisempi napautettava voittaa kortin valinnan, joten kuva ei valitse.
                Modifier.fillMaxWidth().clickable(onClickLabel = zoomLabel, role = Role.Button) { zoomed = true }
            } else {
                Modifier.fillMaxWidth()
            },
            contentAlignment = Alignment.Center,
        ) {
            preview()
        }
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checkbox) {
                Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
            } else {
                RadioButton(selected = selected, onClick = null, enabled = enabled)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (zoomed && large != null) {
        BoardZoomDialog(
            label = label,
            onUse = { onSelect(); zoomed = false },
            onClose = { zoomed = false },
            board = large,
        )
    }
}

/** Material 3:n oma himmennys pois käytöstä olevalle sisällölle. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Pistekenttä kummallekin pelaajalle paneelin pohjalla, vastustaja ylempänä kuten laudalla.
 *
 * **Nimi ja pari kuten pelaajakortissa** (Tommin tilaus 6.10.2026: *"pelaajakortissa on kaksi
 * away-tietoa samalla rivillä"*). Siihen asti kuva näytti kummallekin vain oman luvun, vaikka
 * kortti näyttää 4.10.2026 alkaen parin oma ensin, `4-away 2-away` tai `1-3/5`.
 */
@Composable
internal fun PreviewScore(board: BoardState, look: BoardLook, panel: PanelLook, awayShown: Boolean) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(panel.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        board.players.forEach { player ->
            val paint = previewPaint(board, look, player)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawCircle(paint.fill)
                    val ring = paint.ring ?: DgBoard.Palette.Outline
                    val inlay = paint.inlay
                    if (inlay == null) {
                        drawCircle(ring, style = Stroke(1.dp.toPx()))
                    } else {
                        // Sama inlay kuin laudalla ([checkerRim]), esikatselun mitoissa.
                        val hair = CHECKER_INLAY_HAIR.toPx()
                        val w = 1.dp.toPx()
                        val r = size.minDimension / 2f
                        drawCircle(inlay, r - hair / 2f, style = Stroke(hair))
                        drawCircle(ring, r - hair - w / 2f, style = Stroke(w))
                        drawCircle(inlay, r - hair - w - hair / 2f, style = Stroke(hair))
                    }
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = player.player.name.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = panel.text,
                    )
                    Text(
                        text = previewCardScoreText(board, player, awayShown).orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = panel.text,
                    )
                }
            }
        }
    }
}

/**
 * Ilmaisin keskikaistan huovalla, koska se on sen paikka laudalla. Satunnainen vaihtaa
 * muotoa kahden ja puolen sekunnin välein, koska sillä ei ole omaa muotoa.
 */
@Composable
internal fun PreviewBusyOnBand(style: BusyStyle, board: BoardState, look: BoardLook) {
    var shown by remember(style) { mutableStateOf(if (style == BusyStyle.RANDOM) BusyStyle.shapes.first() else style) }
    if (style == BusyStyle.RANDOM) {
        LaunchedEffect(Unit) {
            val shapes = BusyStyle.shapes
            var i = 0
            while (true) {
                delay(2500)
                i = (i + 1) % shapes.size
                shown = shapes[i]
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(look.band),
        contentAlignment = Alignment.Center,
    ) {
        PreviewBusy(shown, board, look)
    }
}

/**
 * Kortin kuva lähes koko ruudun kokoisena. Lauta piirtyy samalla [PreviewBoard]illa kuin
 * kortissa, joten iso kuva ei voi erota pienestä; nopat, pisteet ja odotus suurennetaan
 * [ZoomedPicture]lla. `Use this` valitsee ja sulkee, `Close` vain sulkee.
 */
@Composable
internal fun BoardZoomDialog(
    label: String,
    onUse: () -> Unit,
    onClose: () -> Unit,
    board: @Composable (Dp) -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            // Lauta on leveämpi kuin korkea, joten korkeus johdetaan leveydestä ja rajataan
            // niin että napeille jää tilaa pystyssäkin.
            val height = minOf(maxWidth * 0.55f, 640.dp)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                board(height)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.board_chat_close))
                    }
                    Button(onClick = onUse, modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.settings_preview_use))
                    }
                }
            }
        }
    }
}

/**
 * Pieni kuva skaalattuna ison kuvan tilaan (6.10.2026, kaikki kuvat isoksi). Nopat, pisteet ja
 * odotus piirtyvät luonnollisessa koossaan ja suurenevat kokonaisina, enintään [ZOOM_MAX]
 * kertaiseksi ja niin että mahtuvat: tekstien koko tulee fontista, joten piirtäminen
 * suurempana rivittäisi ne eikä suurentaisi.
 */
@Composable
internal fun ZoomedPicture(height: Dp, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxWidth().height(height)) { measurables, constraints ->
        val picture = measurables.first().measure(Constraints())
        val scale = minOf(
            ZOOM_MAX,
            constraints.maxWidth / picture.width.coerceAtLeast(1).toFloat(),
            constraints.maxHeight / picture.height.coerceAtLeast(1).toFloat(),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            picture.placeWithLayer(
                (constraints.maxWidth - picture.width) / 2,
                (constraints.maxHeight - picture.height) / 2,
            ) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
}

private const val ZOOM_MAX = 4f
