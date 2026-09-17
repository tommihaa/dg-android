package fi.tommi.dg.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Kuution särmän pituus. Kuusikulmio on `sqrt(3)` särmää leveä ja kaksi särmää korkea. */
private val CubeSide: Dp = 22.dp

/** Lintuviivan leveys. Viiva on ohut, jotta se lukeutuu piirroksena eikä palkkina. */
private val BirdStroke: Dp = 1.5.dp

/**
 * Kolmen tahkon peittävyydet. Ero tahkojen välillä tekee tasokuviosta kuution, eli tämä on
 * muoto eikä sävytys: yhtä suurina arvoina jäljelle jäisi pelkkä vinoneliöruudukko.
 *
 * **Arvot nostettiin 6.9.2026 kahdesti.** Alkuperäiset 0,015, 0,045 ja 0,075 oli mitoitettu
 * niin että kuvio kesti tekstin alla. Tommin päätös viidestä kuvaksi piirretystä
 * kandidaatista (*"täytetään valkoinen tila upeilla kuvioilla"*) nosti ne nelinkertaisiksi
 * (0,10, 0,24 ja 0,38), ja laitekuvan jälkeen hän sanoi *"tessellaatiot ovat vaisuja,
 * toisaalta tekstit ovat luettavia"*. Koska tekstin alla on lukupinta
 * ([DgReadingSurfaceAlpha]) eikä tyhjän tilan voimakkuus enää rajoita luettavuutta, arvot
 * nostettiin siihen missä oikea tahko on lähes täyttä mustetta (0,18, 0,45 ja 0,80).
 * **Seuraavana päivänä sama mittari antoi eri lukeman** (*"kuviot muuttuvat alhaalla liian
 * intensiivisiksi ja tekstin lukeminen vaikeutuu"*), joten arvot laskettiin näiden kahden
 * väliin ja lukupinta nousi, jotta tekstin alle jäävä kuvio on ohuempi kuin missään
 * aiemmassa versiossa. Kaksi vastakkaista lukemaa kahdelta päivältä on kirjattu
 * `docs/AVOIMET.md`:hen kysymyksenä siitä riittääkö yksi kiinteä voimakkuus.
 */
private const val AlphaTop = 0.14f
private const val AlphaLeft = 0.34f
private const val AlphaRight = 0.60f

/**
 * Lintuviivan peittävyys mustetilassa. Luettavuus on mitattu lukupinnan alta, ei paljaana:
 * tummin kohta on lintuviiva, joka antaa pinnan alla `#F4F1EC`, ja sitä vasten välilehtien
 * matalin aksentti (Lounge `#2A7430`) on 5,12:1. AA-raja on 4,5:1.
 */
private const val AlphaBird = 0.65f

/**
 * Sumi-e-tilan lintu: yksi tehosteväri mustepiirroksen päällä. Väri on vermilion, ja tila on
 * makuprofiilin resepti (LK Creations: muste, yksi kirkas tehosteväri ja hallitseva ympyrä).
 * Viiva on täyttä väriä; lukupinnan alla se antaa `#F7EBE6`, ja Lounge sitä vasten on 4,94:1.
 */
private val SumiEBird = Color(0xFFC8452A)
private const val AlphaSumiEBird = 1.0f

/** Sumi-e-tilan kuu: iso vaalea ympyrä lintujen takana, mustetta ohuena. */
private const val AlphaMoon = 0.16f

/**
 * Tumman teeman kuvio: motiiviviivan leveys ja kuutioiden vaimennus.
 *
 * **Tommin päätökset 6.9.2026 luonnoskuvista** (`tyokalut/motiiviluonnos.py`), kolme peräkkäin:
 * *"värit ovat vaimeita ja motiivilla ei ole väliä, kunhan se vaihtuu"*, sitten *"kuutiot
 * himmeämmiksi"* ja *"vielä himmeämpi … en pidä että vähintään kolmannes alaosasta on
 * käytännössä monotonista kuutiota, vähempikin riittää"*. Motiiviviiva on siis täyttä väriä
 * ja kuutiot ovat samaa palettia kertoimella [NightCube]. Kaista on tummassa kapeampi kuin
 * vaaleassa: täysi kuutio alhaalta [NightCubeFloor]…[NightCubeFloor]+[NightCubeSpan] ja
 * muodonmuutos [NightBand] sen päällä, jolloin motiivit täyttävät yli puolet ruudusta.
 * Vaalean arvot (0,15 + 0,15 ja 0,40) ovat [dgSkyLayout]in oletukset eivätkä muuttuneet.
 */
private val NightStroke: Dp = 2.dp
private const val NightCube = 0.25f
private const val NightCubeFloor = 0.05f
private const val NightCubeSpan = 0.05f
private const val NightBand = 0.30f

/**
 * Motiivi: yhden viivan hahmo paikalliskoordinaateissa. [t] kulkee -1…1 niin että -1 on
 * vasen siivenkärki ja 1 oikea, jolloin muodonmuutos V:stä hahmoksi on sama lerp kaikille.
 */
internal typealias DgMotif = (t: Float, span: Float, s: Float) -> Offset

/**
 * Yöpaletti: motiivi ja neljä väriä. Tahkot ovat kolme väriä, viiva neljäs, ja [moon] on kuun
 * peittävyys viivan värillä (0 ei kuuta). Nimet ovat luonnostyökalun paneelien nimet.
 */
internal class DgNightPalette(
    val name: String,
    val motif: DgMotif,
    val top: Color,
    val left: Color,
    val right: Color,
    val line: Color,
    val moon: Float = 0f,
)

/**
 * Yksi solu taivaassa. [p] on muodonmuutoksen vaihe (0 ehjä kuutio, 1 valmis lintu) ja [fade]
 * himmennys harvenevalla alueella. Sijainti on solun kuusikulmion keskipiste pikseleinä.
 */
internal data class DgSkyCell(val cx: Float, val cy: Float, val p: Float, val fade: Float)

/** Kuu: keskipiste ja säde pikseleinä. Arvotaan aina, piirretään vain sumi-e-tilassa. */
internal data class DgSkyMoon(val cx: Float, val cy: Float, val r: Float)

/**
 * Yhden ruudun taivas: vyöhykerajat ruudun korkeuden osuuksina alhaalta lukien, solut ja kuu.
 * Rajat ovat tässä jotta testi voi todeta että ne arvottiin sallitulta väliltä.
 */
internal data class DgSkyLayout(
    val cubeEnd: Float,
    val birdStart: Float,
    val sparseStart: Float,
    val cells: List<DgSkyCell>,
    val moon: DgSkyMoon,
)

private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)

private fun smooth(v: Float): Float {
    val c = clamp01(v)
    return c * c * (3f - 2f * c)
}

/** Pehmeä nousu nollasta yhteen välillä [a, b]. */
private fun seg(p: Float, a: Float, b: Float): Float = smooth((p - a) / (b - a))

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * Taivaan asettelu: kuusikulmioruudukko, jonka jokaiselle solulle lasketaan vaihe ruudun
 * pystysuunnasta ja arvotaan sijainnin hajonta ja pudotus lintualueella.
 *
 * **Sama siemen antaa saman taivaan**, koska `Random(seed)` on deterministinen. Se on syy
 * miksi arpa heitetään kutsupaikassa eikä tässä: kutsupaikka päättää milloin taivas vaihtuu.
 *
 * **Kolme vyöhykettä alhaalta ylös, ja rajat arvotaan.** Suunta on Tommin (6.9.2026):
 * *"alhaalta ylöspäin toteutuvat tessellaatiot vaikuttaisi minua lisää."* Alhaalla kuutiot
 * ehjinä maana [cubeEnd]-rajaan asti (osuus ruudun korkeudesta alhaalta lukien), välissä
 * muodonmuutos [birdStart]-rajaan asti, ja siitä ylöspäin linnut, jotka alkavat harveta
 * [sparseStart]-rajalta. Rajat vaihtelevat siemenen mukaan, jotta kahden ruudun taivaat
 * eroavat muutenkin kuin lintujen paikoista. Kaistojen mitat ([cubeFloor], [cubeSpan] ja
 * [band]) ovat parametreja, koska tumma teema käyttää kapeampaa kuutiokaistaa kuin vaalea;
 * oletukset ovat vaalean arvot.
 *
 * Rivit ja sarakkeet menevät ruudun yli molempiin suuntiin, jotta reunoille osuvat solut
 * piirtyvät leikattuina eivätkä jää kokonaan pois.
 */
internal fun dgSkyLayout(
    width: Float,
    height: Float,
    sidePx: Float,
    seed: Int,
    cubeFloor: Float = 0.15f,
    cubeSpan: Float = 0.15f,
    band: Float = 0.40f,
): DgSkyLayout {
    val rng = Random(seed)
    val halfWidth = sqrt(3f) * sidePx / 2f
    val rowStep = 1.5f * sidePx
    val cubeEnd = cubeFloor + rng.nextFloat() * cubeSpan
    val birdStart = cubeEnd + band + rng.nextFloat() * 0.10f
    val sparseStart = birdStart + 0.06f

    val rows = (height / rowStep).toInt() + 2
    val cols = (width / (2f * halfWidth)).toInt() + 2
    val cells = ArrayList<DgSkyCell>(rows * cols)
    for (row in -1..rows) {
        val cy = row * rowStep
        val shift = if (row % 2 == 0) 0f else halfWidth
        for (col in -1..cols) {
            val cx = col * 2f * halfWidth + shift
            // Osuus alhaalta lukien: 0 on ruudun alareuna ja 1 yläreuna.
            val y = 1f - cy / height
            val p = seg(y, cubeEnd, birdStart)
            // Arvonnat tehdään joka solulle myös silloin kun niitä ei käytetä, jotta
            // ylempien vyöhykkeiden koko ei muuta alempien lintujen paikkoja.
            val jx = rng.nextFloat() * 2f - 1f
            val jy = rng.nextFloat() * 2f - 1f
            val drop = rng.nextFloat()
            val jitter = if (p < 0.7f) 0f else (p - 0.7f) / 0.3f
            var fade = 1f
            if (y > sparseStart) {
                val q = (y - sparseStart) / (1f - sparseStart)
                if (drop < q * 1.1f) continue
                fade = 1f - 0.35f * q
            }
            cells += DgSkyCell(
                cx = cx + jx * jitter * halfWidth * 0.8f,
                cy = cy + jy * jitter * sidePx * 0.9f,
                p = p,
                fade = fade,
            )
        }
    }
    // Kuu arvotaan solujen jälkeen, jotta sen lisäys ei siirtänyt lintujen paikkoja. Se on
    // lintualueella ruudun yläosassa ja leveydestä 22–30 %, eli iso muttei koko taivas.
    val moon = DgSkyMoon(
        cx = width * (0.30f + rng.nextFloat() * 0.40f),
        cy = height * (0.16f + rng.nextFloat() * 0.10f),
        r = width * (0.22f + rng.nextFloat() * 0.08f),
    )
    return DgSkyLayout(cubeEnd, birdStart, sparseStart, cells, moon)
}

/**
 * Yksi piirto-operaatio: polku ja peittävyys. Täyttö on tahko, viiva on ääriviiva tai lintu.
 * Viivan peittävyys on suhteellinen (0…1 lintuviivan täydestä), koska sen väri ja täysi
 * peittävyys riippuvat tilasta ja ratkeavat vasta piirrettäessä.
 */
internal sealed class DgSkyOp(val path: Path, val alpha: Float) {
    /**
     * Tahko. [alpha] on geometrian osuus (haipuminen ja himmennys) ilman tahkon omaa
     * peittävyyttä, joka ratkeaa piirrettäessä tilan mukaan: vaaleassa [AlphaTop]-vakiot
     * mustetta, tummassa paletin väri kertoimella [NightCube] ja syvyydellä [depth]
     * (valo ylhäältä, alemmat kuutiot tummempia).
     */
    class FillOp(path: Path, val face: DgFace, alpha: Float, val depth: Float) : DgSkyOp(path, alpha)
    class StrokeOp(path: Path, alpha: Float) : DgSkyOp(path, alpha)
}

internal enum class DgFace { TOP, LEFT, RIGHT }

/**
 * Linnun siluetti yhtenä viivana: siivet ylös ja kärjet notkahtavat. Muoto on 6.9.2026
 * luonnoksista se ainoa joka lukeutui linnuksi; kaksi kaarta ylöspäin lukeutui kukkuloiksi.
 * [t] on -1..1 siiven kärjestä kärkeen, [span] puolet siipien välistä ja [s] korkeusmitta.
 */
private fun birdPoint(t: Float, span: Float, s: Float): Offset {
    val x = t * span
    var y = -abs(t).pow(1.25f) * s * 0.34f
    if (abs(t) > 0.86f) {
        val k = (abs(t) - 0.86f) / 0.14f
        y += k * k * s * 0.16f
    }
    return Offset(x, y)
}

/** Kala sivulta: pyrstön kärjestä selkää pitkin kuonoon, vatsaa takaisin ja pyrstön toiseen kärkeen. */
private fun fishPoint(t: Float, span: Float, s: Float): Offset {
    val u = (t + 1f) / 2f
    val pi = PI.toFloat()
    return when {
        u < 0.1f -> {
            val k = u / 0.1f
            Offset(-span + k * span * 0.35f, -s * 0.28f * (1f - k))
        }
        u < 0.5f -> {
            val k = (u - 0.1f) / 0.4f
            Offset(-span * 0.65f + k * span * 1.65f, -s * 0.30f * sin(k * pi))
        }
        u < 0.9f -> {
            val k = (u - 0.5f) / 0.4f
            Offset(span - k * span * 1.65f, s * 0.30f * sin(k * pi))
        }
        else -> {
            val k = (u - 0.9f) / 0.1f
            Offset(-span * 0.65f - k * span * 0.35f, s * 0.28f * k)
        }
    }
}

/** Sieni: jalka ylös, lakki kaarena, jalka alas. */
private fun mushroomPoint(t: Float, span: Float, s: Float): Offset {
    val u = (t + 1f) / 2f
    val pi = PI.toFloat()
    return when {
        u < 0.2f -> {
            val k = u / 0.2f
            Offset(-span * 0.28f, s * 0.45f - k * s * 0.55f)
        }
        u < 0.8f -> {
            val k = (u - 0.2f) / 0.6f
            val a = pi * (1f - k)
            Offset(cos(a) * span, -s * 0.10f - sin(a) * s * 0.55f)
        }
        else -> {
            val k = (u - 0.8f) / 0.2f
            Offset(span * 0.28f, -s * 0.10f + k * s * 0.55f)
        }
    }
}

/** Lehti: kaksi kaarta kärjestä kärkeen. */
private fun leafPoint(t: Float, span: Float, s: Float): Offset {
    val u = (t + 1f) / 2f
    val pi = PI.toFloat()
    return if (u < 0.5f) {
        val k = u / 0.5f
        Offset(-span + k * 2f * span, -s * 0.42f * sin(k * pi))
    } else {
        val k = (u - 0.5f) / 0.5f
        Offset(span - k * 2f * span, s * 0.42f * sin(k * pi))
    }
}

/** Viisisakarainen tähti yhtenä viivana (pentagrammi): kärkien järjestys viivaa pitkin. */
private val StarOrder = intArrayOf(0, 2, 4, 1, 3, 0)

private fun starPoint(t: Float, span: Float, s: Float): Offset {
    val u = (t + 1f) / 2f
    val n = 5
    val idx = u * n
    val i = idx.toInt().coerceIn(0, n - 1)
    val k = idx - idx.toInt()
    fun pt(j: Int): Offset {
        val a = (-PI / 2 + StarOrder[j] * 2 * PI / n).toFloat()
        return Offset(cos(a) * span * 0.8f, sin(a) * s * 0.55f)
    }
    val a = pt(i)
    val b = pt(i + 1)
    return Offset(lerp(a.x, b.x, k), lerp(a.y, b.y, k))
}

/**
 * Viisi yöpalettia, ja jokaisessa oma motiivi. Arpa valitsee ruudun avauksessa yhden
 * ([dgNightPalette]), koska Tommin sana oli *"motiivilla ei ole väliä, kunhan se vaihtuu"*.
 * Värit ovat täyttä kylläisyyttä mustalla; kuutiot vaimennetaan piirrettäessä.
 */
internal val DgNightPalettes: List<DgNightPalette> = listOf(
    DgNightPalette(
        "Revontulet + linnut", ::birdPoint,
        Color(0xFF3DFFA0), Color(0xFF2B7BFF), Color(0xFF8A3DFF), Color(0xFFDDF7FF),
    ),
    DgNightPalette(
        "Meri + kalat", ::fishPoint,
        Color(0xFF45E0D8), Color(0xFF1E6FB8), Color(0xFF12315C), Color(0xFFBFF4FF),
    ),
    DgNightPalette(
        "Hehku + sienet", ::mushroomPoint,
        Color(0xFFFFB347), Color(0xFFFF5E3A), Color(0xFF8F1D3A), Color(0xFFFFE7C2),
    ),
    DgNightPalette(
        "Kuutamo + lehdet", ::leafPoint,
        Color(0xFFDDE6FF), Color(0xFF6C82C4), Color(0xFF2A3560), Color(0xFFFFF0B0), moon = 0.5f,
    ),
    DgNightPalette(
        "Sumi-e + tähdet", ::starPoint,
        Color(0xFFCBB093), Color(0xFF7A6650), Color(0xFF3A3028), Color(0xFFFF5A36), moon = 0.35f,
    ),
)

/**
 * Yöpaletti siemenestä. Sama siemen antaa saman paletin, kuten se antaa saman taivaan.
 * Viisi lippua; luolan seinä oli yhden version kuudes, sitten täytekuva ja poistui 17.9.2026.
 */
internal fun dgNightPalette(seed: Int): DgNightPalette =
    DgNightPalettes[Math.floorMod(seed, DgNightPalettes.size)]

/**
 * Yhden solun muodonmuutos kuutiosta hahmoksi, neljässä vaiheessa jotka menevät limittäin:
 *
 * 1. sivutahkot haipyvät ja kuutio litistyy pinnaksi (p 0…0,45),
 * 2. ylätahko muuttuu täytöstä ääriviivaksi ja irtoaa ruudukosta kutistumalla, jotta
 *    naapurien kärjet eivät enää kosketa (p 0,2…0,6),
 * 3. yläkärki laskee keskipisteeseen ja jäljelle jää V (p 0,4…0,75),
 * 4. V taipuu [motif]-viivaksi (p 0,6…1); vaaleassa se on aina lintu, tummassa paletin hahmo.
 *
 * Ylätahkon alareunat ovat valmiiksi V, jonka kärki on solun keskellä ja siivet ylöspäin,
 * eli muodonmuutos käyttää kuution omaa muotoa eikä vaihda sitä toiseen.
 */
private fun cellOps(cell: DgSkyCell, sidePx: Float, height: Float, motif: DgMotif, out: MutableList<DgSkyOp>) {
    val s = sidePx
    val hw = sqrt(3f) * s / 2f
    val cx = cell.cx
    val cy = cell.cy
    val p = cell.p
    val fade = cell.fade
    val depth = 1f - 0.35f * clamp01(cy / height)

    fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }

    val center = Offset(cx, cy)
    val topLeft = Offset(cx - hw, cy - s / 2f)
    val bottomLeft = Offset(cx - hw, cy + s / 2f)
    val bottom = Offset(cx, cy + s)
    val bottomRight = Offset(cx + hw, cy + s / 2f)
    val topRight = Offset(cx + hw, cy - s / 2f)

    val sideFade = 1f - seg(p, 0f, 0.45f)
    if (sideFade > 0f) {
        out += DgSkyOp.FillOp(quad(center, topLeft, bottomLeft, bottom), DgFace.LEFT, sideFade * fade, depth)
        out += DgSkyOp.FillOp(quad(center, bottom, bottomRight, topRight), DgFace.RIGHT, sideFade * fade, depth)
    }

    val topFill = 1f - seg(p, 0.2f, 0.6f)
    val outline = seg(p, 0.2f, 0.55f)
    val shrink = lerp(1f, 0.72f, seg(p, 0.25f, 0.9f))
    val sTopLeft = Offset(cx - hw * shrink, cy - s / 2f * shrink)
    val sTopRight = Offset(cx + hw * shrink, cy - s / 2f * shrink)
    val cap = 1f - seg(p, 0.4f, 0.75f)
    val topPoint = Offset(cx, lerp(cy, cy - s * shrink, cap))
    if (topFill > 0f) {
        out += DgSkyOp.FillOp(quad(center, sTopLeft, topPoint, sTopRight), DgFace.TOP, topFill * fade, depth)
    }
    if (outline > 0f && cap > 0f) {
        val hat = Path().apply {
            moveTo(sTopLeft.x, sTopLeft.y); lineTo(topPoint.x, topPoint.y); lineTo(sTopRight.x, sTopRight.y)
        }
        out += DgSkyOp.StrokeOp(hat, outline * cap * fade)
    }
    if (outline > 0f) {
        val bend = seg(p, 0.6f, 1f)
        val wing = Path()
        val n = 24
        for (i in 0..n) {
            val t = -1f + 2f * i / n
            val vx = t * hw * shrink
            val vy = -abs(t) * s / 2f * shrink
            val b = motif(t, hw * shrink * 1.05f, s * 1.15f)
            val x = cx + lerp(vx, b.x, bend)
            val y = cy + lerp(vy, b.y, bend)
            if (i == 0) wing.moveTo(x, y) else wing.lineTo(x, y)
        }
        out += DgSkyOp.StrokeOp(wing, outline * fade)
    }
}

/** Koko taivas piirto-operaatioiksi. Lasketaan kerran per koko ja siemen, piirretään joka kehys. */
internal fun dgSkyOps(
    layout: DgSkyLayout,
    sidePx: Float,
    height: Float,
    motif: DgMotif = ::birdPoint,
): List<DgSkyOp> {
    val out = ArrayList<DgSkyOp>(layout.cells.size * 4)
    for (cell in layout.cells) cellOps(cell, sidePx, height, motif, out)
    return out
}

private fun faceAlpha(face: DgFace): Float = when (face) {
    DgFace.TOP -> AlphaTop
    DgFace.LEFT -> AlphaLeft
    DgFace.RIGHT -> AlphaRight
}

private fun DrawScope.drawSky(
    layout: DgSkyLayout,
    ops: List<DgSkyOp>,
    ink: Color,
    sumiE: Boolean,
    strokePx: Float,
) {
    val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val birdInk = if (sumiE) SumiEBird else ink
    val birdAlpha = if (sumiE) AlphaSumiEBird else AlphaBird
    if (sumiE) {
        val moon = layout.moon
        drawCircle(ink, radius = moon.r, center = Offset(moon.cx, moon.cy), alpha = AlphaMoon)
    }
    for (op in ops) {
        if (op.alpha <= 0.001f) continue
        when (op) {
            is DgSkyOp.FillOp -> drawPath(op.path, ink, alpha = op.alpha * faceAlpha(op.face), style = Fill)
            is DgSkyOp.StrokeOp -> drawPath(op.path, birdInk, alpha = op.alpha * birdAlpha, style = stroke)
        }
    }
}

/**
 * Tumman teeman piirto: kuu paletin viivavärillä, tahkot paletin väreillä vaimennettuina
 * ([NightCube] kertaa syvyys) ja motiiviviiva täyttä väriä. Lukupinta on sama kuin vaaleassa,
 * koska se lukee taustavärin teemasta.
 */
private fun DrawScope.drawNight(
    layout: DgSkyLayout,
    ops: List<DgSkyOp>,
    palette: DgNightPalette,
    strokePx: Float,
) {
    val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (palette.moon > 0f) {
        val moon = layout.moon
        drawCircle(palette.line, radius = moon.r, center = Offset(moon.cx, moon.cy), alpha = palette.moon)
    }
    for (op in ops) {
        if (op.alpha <= 0.001f) continue
        when (op) {
            is DgSkyOp.FillOp -> {
                val color = when (op.face) {
                    DgFace.TOP -> palette.top
                    DgFace.LEFT -> palette.left
                    DgFace.RIGHT -> palette.right
                }
                drawPath(op.path, color, alpha = clamp01(op.alpha * op.depth * NightCube), style = Fill)
            }
            is DgSkyOp.StrokeOp -> drawPath(op.path, palette.line, alpha = op.alpha, style = stroke)
        }
    }
}

/**
 * Tekstinäkymien tausta: kuutiotessellaatio, joka muuttuu alhaalta ylös linnuiksi.
 *
 * **Päätös on Tommin (6.9.2026)**, ja peruste koskee käyttöä eikä ulkoasua: *"muuttuva tausta
 * saisi minut palaamaan pelaamaan."* Idea on Escherin Metamorphosis-teosten menetelmä, jossa
 * säännöllinen ruudukko irtoaa vähitellen hahmoiksi. Menetelmä on vapaa, teokset eivät, eikä
 * tässä ole niistä mitään. Kuutiotessellaatio (*tumbling blocks*) on vuosisatoja vanha, ja
 * lintusiluetti on oma yhden viivan piirros.
 *
 * **Tausta arvotaan kutsupaikassa** ([seed]) eikä tässä, jotta se joka päättää milloin taivas
 * vaihtuu on luettavissa siitä paikasta jossa muutkin ruudunvaihtoa koskevat päätökset ovat.
 * Sama siemen antaa saman taivaan.
 *
 * **Suora lintutessellaatio yritettiin ensin ja se kaatui kolmesti.** Siirtosymmetria pakottaa
 * jokaisen linnun samaan asentoon ja tekee siivestä kolon naapurin rungossa, eikä sitä korjata
 * parametreilla. Metamorfoosi kiertää ongelman: linnut ovat irrallaan eivätkä tessellaatio.
 *
 * **Kuvio piirretään koodista eikä ladata kuvatiedostona.** Piirto on koko ruudun kokoinen
 * eikä toistuva tiili, koska tiili on määritelmällisesti sama kaikkialla eikä voi muuttua
 * matkalla. Polut lasketaan kerran per koko ja siemen (`drawWithCache`) ja piirretään
 * kehyksittäin.
 *
 * **Luettavuus on mitattu lukupinnan alta**, ja Tommin rajaus 6.9.2026 on että tausta ei saa
 * vaikeuttaa tekstien ja toimintojen lukemista. Saman illan tarkennus oli *"vain tekstin
 * alta"*: tyhjä tila saa täyden kuvion, ja tekstin alla on lukupinta (`DgReadingSurface.kt`),
 * jonka alta mittaus tehdään. Luvut ovat [AlphaBird]in ja [SumiEBird]in kommenteissa.
 *
 * **Kaksi tilaa, ja [sumiE] on asetus** (`SkyThemeStore`, oletus pois): mustetila piirtää
 * kaiken primaryllä, sumi-e-tila vaihtaa linnut vermilioniin ja lisää kuun. Kuutiot ovat
 * samat molemmissa, joten tila ei muuta asettelua eikä mittausta tahkojen osalta.
 *
 * **Tumma teema sai oman kuvionsa 6.9.2026** (Tommi: *"light mode: tessellaatiot, dark mode:
 * värit"*). Sama asettelu ja sama muodonmuutos, mutta viisi palettia ja viisi motiivia
 * ([DgNightPalettes]), joista arpa valitsee ruudun avauksessa yhden samasta siemenestä kuin
 * taivaan. Kuutiokaista on kapeampi ja kuutiot vaimennettuja ([NightCube]), motiiviviiva
 * täyttä väriä.
 *
 * **Tämä on tausta; täytekuva on eri laji eikä osa tätä arpaa** (Tommin päätökset
 * 7.9.2026). Luolan seinä oli yhden version tumman kuudes lippu ja vaalean toinen puolikas
 * (commit 92ac098), jolloin se kilpaili tessellaation kanssa koko ruudusta, ja sitten
 * täytekuva sisällön alle jäävässä tilassa (`DgFill.kt`) kunnes se poistettiin 17.9.2026
 * Tommin sanalla. Nyt tessellaatio on aina tausta kun kuvio on päällä, ja vapaan tilan
 * täyttävät laitteen kansion taustakuvat. Käsijälki mustalla ja viisi käsikuviota olivat
 * laitteella yhden version kumpikin (commitit 30e5deb ja 79f0879), ja Tommi hylkäsi ne
 * laitekuvista.
 *
 * **Kuvio on asetus molemmissa teemoissa, oletus pois** ([pattern], `SkyThemeStore`). Tommin
 * päätös 6.9.2026 illalla: *"oletuksena taustakuva vaalealla ja tummalla taustalla pois
 * päältä."* Pois kytkettynä kumpikin teema on pelkkä taustaväri. Lautanäkymä on ulkona
 * [enabled]-lipulla, joka luetaan kutsupaikasta.
 *
 * **Perusväri maalataan tässä eikä ruuduissa**, koska ruutujen `Scaffold`ien `containerColor`
 * on 6.9.2026 alkaen läpinäkyvä. Uusi ruutu tarvitsee saman rivin tai se jää ilman taustaa.
 */
@Composable
internal fun Modifier.dgScreenBackground(
    enabled: Boolean,
    seed: Int,
    sumiE: Boolean,
    pattern: Boolean,
): Modifier {
    val base = MaterialTheme.colorScheme.background
    val withBase = this.background(base)
    if (!enabled || !pattern) return withBase
    if (isSystemInDarkTheme()) {
        val palette = dgNightPalette(seed)
        return withBase.drawWithCache {
            val sidePx = CubeSide.toPx()
            val strokePx = NightStroke.toPx()
            val layout = dgSkyLayout(
                size.width, size.height, sidePx, seed,
                cubeFloor = NightCubeFloor, cubeSpan = NightCubeSpan, band = NightBand,
            )
            val ops = dgSkyOps(layout, sidePx, size.height, palette.motif)
            onDrawBehind { drawNight(layout, ops, palette, strokePx) }
        }
    }
    val ink = DgColorScheme.primary
    return withBase.drawWithCache {
        val sidePx = CubeSide.toPx()
        val strokePx = BirdStroke.toPx()
        val layout = dgSkyLayout(size.width, size.height, sidePx, seed)
        val ops = dgSkyOps(layout, sidePx, size.height)
        onDrawBehind { drawSky(layout, ops, ink, sumiE, strokePx) }
    }
}
