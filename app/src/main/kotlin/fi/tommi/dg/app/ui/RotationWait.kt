package fi.tommi.dg.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fi.tommi.dg.app.R
import fi.tommi.dg.app.isChromeOs
import fi.tommi.dg.app.ui.DgBoard.Palette
import kotlinx.coroutines.delay

/**
 * Odotusmerkki laitteen kääntyessä, kaikilla ruuduilla (Tommin tilaus 15.10.2026 *"kun vaihtaa
 * tilaa siis pysty-vaaka-pysty variaatioista niin odotus-animaatio kertomaan, että jotain on
 * tekeillä"*). Ruutu himmenee, ja keskelle tulee valittu odotuksen ilmaisin ja teksti
 * *Turning the screen…*. Valinnat ja mittaukset: `docs/UI.md` › Odotusmerkki käännössä.
 *
 * **Kaksi odotusta, kaksi laukaisijaa.** Pixel 8a pidättää käännön anturissaan niin kauan kuin
 * puhelin heiluu (lokissa *swing_time check*, mitattu 3,7 s), eikä Android tai sovellus saa
 * siitä tietoa. Siksi sovellus lukee itse kallistusta ([OrientationEventListener]) ja näyttää
 * merkin kun laite on jo käännetty mutta ikkuna ei. Tabletilla (SM-T970) ikkuna vaihtuu heti,
 * mutta järjestelmä pitää ruutua paikallaan noin 0,8 s ennen pyörähdystä; sen kattaa
 * [ROTATION_WAIT_MS] ikkunan suunnan vaihdosta.
 *
 * **Ikkunan suunta luetaan mittauksen rajoista eikä `LocalConfiguration`ista.** Kaksi kokeilua
 * jossa näkyvyys päätettiin `LaunchedEffect`issä tai `LocalConfiguration`in suunnasta jäivät
 * tabletin nauhalla pois odotuksen ajalta. Tämä muoto koostuu samassa mittauksessa kuin uusi
 * asettelu, ja Tommi näki sen tabletilla heti odotuksen alussa.
 *
 * **Oma kääntö laudalla (Tommin valinta 19.10.2026).** Pixelin anturi ilmoitti vaaka-asennon
 * pelisessiossa `sessio-19-10-yo` 14,6 s myöhässä ilman lokiin jäävää syytä. Kun [takeOver] on
 * päällä ja ristiriita on kestänyt [TAKEOVER_AFTER_MS], suunta pakotetaan sovelluksen oman
 * anturin mukaan ([onForce]), ja sen jälkeen sovellus kääntää itse laudalta poistumiseen asti.
 * Paluuta Androidin käännölle ei ole kesken laudan, koska järjestelmän anturi voi olla yhä
 * jäljessä ja kääntäisi ruudun takaisin. Vain laitteella jonka luonnollinen asento on pysty.
 */
@Composable
fun RotationWait(
    takeOver: Boolean = false,
    forced: Int? = null,
    onForce: (Int?) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    // Laitteen asento anturista: true sivuttain, false pystyssä luonnolliseen asentoon nähden,
    // null kun laite on lähes vaakatasossa tai kulma on rajojen välissä.
    var sideways by remember { mutableStateOf<Boolean?>(null) }
    // Sama asento `ActivityInfo`n suuntana oman käännön varalle; null pitää edellisen.
    var deviceTarget by remember { mutableStateOf<Int?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context, lifecycleOwner) {
        // Kallistus vaakatasosta asteina, vain lokia varten (ks. [KaantoLoki]).
        var tilt = Float.NaN
        val sensors = context.getSystemService(SensorManager::class.java)
        val accelerometer = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val tiltListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tilt = tiltDegrees(event.values[0], event.values[1], event.values[2])
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val listener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {
            override fun onOrientationChanged(angle: Int) {
                val next = sidewaysFor(angle, sideways)
                if (next != sideways) KaantoLoki.anturi(angle, tilt, next)
                sideways = next
                deviceTarget = targetFor(angle, deviceTarget)
            }
        }
        // Anturi vain kun sovellus on edessä, jotta taustalla ei kulu akkua.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (listener.canDetectOrientation()) {
                    listener.enable()
                    accelerometer?.let { sensors?.registerListener(tiltListener, it, SensorManager.SENSOR_DELAY_UI) }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    listener.disable()
                    sensors?.unregisterListener(tiltListener)
                    sideways = null
                    deviceTarget = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listener.disable()
            sensors?.unregisterListener(tiltListener)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight

        // Ikkunan suunnan vaihdon jälkeinen odotus (tabletin 0,8 s).
        var settled by remember { mutableStateOf(landscape) }
        LaunchedEffect(landscape) {
            delay(ROTATION_WAIT_MS)
            settled = landscape
        }

        // Anturin ja ikkunan ristiriita (Pixelin pidätys). Luonnollinen asento päätellään
        // näytön kierrosta ja ikkunan suunnasta, jotta vaakaluontoinen tabletti ei käänny väärin.
        val rotation = remember(landscape) { displayRotation(activity) }
        val naturalPortrait = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) !landscape else landscape
        val deviceLandscape = sideways?.let { if (naturalPortrait) it else !it }
        val mismatch = deviceLandscape != null && deviceLandscape != landscape && rotationFollowsDevice(activity)
        var pending by remember { mutableStateOf(false) }
        // Katon alku: ensimmäinen ristiriita tässä ikkunan suunnassa. Hetken heilahdus takaisin
        // ei nollaa sitä, koska Pixelillä 18.10.2026 merkki pysyi noin 20 s, kun anturin
        // välähdykset aloittivat 10 s:n katon kahdesti alusta.
        var mismatchSince by remember(landscape) { mutableStateOf<Long?>(null) }
        LaunchedEffect(mismatch) {
            KaantoLoki.ristiriita(mismatch, landscape)
            pending = false
            if (mismatch) {
                val since = mismatchSince ?: SystemClock.elapsedRealtime().also { mismatchSince = it }
                // Lyhyt heilautus ei ole kääntö: merkki vasta kun asento on pysynyt hetken.
                delay(PENDING_AFTER_MS)
                // Katto sille, ettei Android käännä lainkaan (esim. kasvojen suuntaa lukeva
                // automaattikääntö vastustaa). Silloin merkki ei saa jäädä ruudulle.
                val left = PENDING_MAX_MS - (SystemClock.elapsedRealtime() - since)
                if (left <= 0) return@LaunchedEffect
                pending = true
                delay(left)
                pending = false
            } else {
                // Laite on palannut ikkunan suuntaan: uusi kääntöyritys saa oman kattonsa.
                delay(MISMATCH_RESET_MS)
                mismatchSince = null
            }
        }

        // Oma kääntö: otetaan käyttöön kun Android viipyy, ja sen jälkeen seurataan omaa anturia.
        val auto = remember(landscape, forced) { autoRotateOn(activity) }
        val canTakeOver = takeOver && auto && naturalPortrait &&
            activity?.let { !it.isInMultiWindowMode && !isChromeOs(it) } == true
        LaunchedEffect(canTakeOver, forced, mismatch, deviceTarget) {
            val target = deviceTarget
            when {
                !canTakeOver -> if (forced != null) onForce(null)
                forced == null -> if (mismatch && target != null) {
                    delay(TAKEOVER_AFTER_MS)
                    KaantoLoki.oma(target, "android viipyy")
                    onForce(target)
                }
                target != null && target != forced -> {
                    delay(FORCED_SETTLE_MS)
                    KaantoLoki.oma(target, "anturi")
                    onForce(target)
                }
            }
        }

        val shown = landscape != settled || pending
        LaunchedEffect(shown) { KaantoLoki.merkki(shown) }
        if (shown) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                // Teksti kertoo syyn (Tommin tarkennus 15.10.2026: *"tämä odotus-animaatio
                // johtuu ruudun käännöstä"*), koska pelkkä merkki voisi näyttää sivuston odotukselta.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BusyIndicator(Modifier, ArcLook.X22, track = Palette.Frame)
                    Text(
                        stringResource(R.string.rotation_wait),
                        color = Color(0xFFF0EBC0),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

/**
 * Kulma 0–359 ([OrientationEventListener]) asennoksi luonnolliseen asentoon nähden. Rajojen
 * välissä (30–60 astetta kummastakin) asento pysyy edellisenä, jotta 45 asteen kohdalla
 * pidetty laite ei vilkuta merkkiä.
 */
internal fun sidewaysFor(angle: Int, previous: Boolean?): Boolean? = when {
    angle == OrientationEventListener.ORIENTATION_UNKNOWN -> null
    angle in 60..120 || angle in 240..300 -> true
    angle < 30 || angle > 330 || angle in 150..210 -> false
    else -> previous
}

/**
 * Kallistus asteina samalla kaavalla kuin Androidin oma kääntökuuntelija
 * (`WindowOrientationListener`: asin(z / g)), jotta lokin luku on verrattavissa sen rajoihin:
 * 0 on pystyssä, 90 pöydällä näyttö ylöspäin ja -90 näyttö alaspäin. NaN ilman painovoimaa.
 */
internal fun tiltDegrees(x: Float, y: Float, z: Float): Float {
    val g = kotlin.math.sqrt(x * x + y * y + z * z)
    if (g == 0f) return Float.NaN
    return Math.toDegrees(kotlin.math.asin((z / g).coerceIn(-1f, 1f).toDouble())).toFloat()
}

/**
 * Kulma 0–359 suunnaksi oman käännön varalle, kun luonnollinen asento on pysty. Kulma 270 on
 * `LANDSCAPE` (Pixel 8a 19.10.2026: kulma 288, Android kääntyi kiertoon 1). Ylösalaisin pidetty
 * ja rajojen välissä pidetty laite sekä vaakataso pitävät edellisen suunnan.
 */
internal fun targetFor(angle: Int, previous: Int?): Int? = when {
    angle == OrientationEventListener.ORIENTATION_UNKNOWN -> previous
    angle in 240..300 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    angle in 60..120 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    angle < 30 || angle > 330 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    else -> previous
}

private fun autoRotateOn(activity: Activity?): Boolean = activity != null &&
    Settings.System.getInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

private fun displayRotation(activity: Activity?): Int {
    activity ?: return Surface.ROTATION_0
    @Suppress("DEPRECATION")
    return (activity.getSystemService(WindowManager::class.java)).defaultDisplay.rotation
}

/**
 * Kääntääkö Android ikkunan laitteen mukana juuri nyt. Ei, kun automaattinen kääntö on pois,
 * kun sovellus pakottaa suunnan (pystylukko, laudan vaakapakotus, puhelimella kirjoittaminen),
 * jaetussa näytössä tai Chromebookilla. Silloin anturin ja ikkunan ristiriita on pysyvä eikä
 * odotus, ja merkki jäisi ruudulle.
 */
private fun rotationFollowsDevice(activity: Activity?): Boolean {
    activity ?: return false
    if (activity.isInMultiWindowMode || isChromeOs(activity)) return false
    return autoRotateOn(activity) && activity.requestedOrientation in FOLLOWING
}

private val FOLLOWING = setOf(
    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    ActivityInfo.SCREEN_ORIENTATION_USER,
    ActivityInfo.SCREEN_ORIENTATION_SENSOR,
    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
    ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
)

/**
 * Tommin valinta kolmesta (0,7 / 1,0 / 1,5 s). Kattaa tabletin noin 1,1 s:n käännön melkein
 * kokonaan, ja Pixel 8a:lla (noin 0,5 s) merkki jää hetkeksi valmiin ruudun päälle.
 */
internal const val ROTATION_WAIT_MS = 1000L

/** Anturin asennon on pysyttävä näin kauan ennen merkkiä. */
private const val PENDING_AFTER_MS = 250L

/** Tommin hyväksymä katto 15.10.2026: merkki enintään 10 s, jos Android ei käännä. */
private const val PENDING_MAX_MS = 10_000L

/**
 * Ristiriidan kesto ennen omaa kääntöä. Pixelin tavallinen kääntö vie 0,5–0,7 s ja heilunnan
 * pidätys enintään 3,7 s (15.10.2026), joten pidempi raja jättäisi 19.10. jumista vain osan pois.
 */
internal const val TAKEOVER_AFTER_MS = 1_000L

/** Oman käännön aikana uuden asennon on pysyttävä näin kauan ennen kääntöä. */
private const val FORCED_SETTLE_MS = 300L

/** Näin kauan ikkunan suunnassa pysynyt laite aloittaa katon alusta seuraavalla kääntöyrityksellä. */
private const val MISMATCH_RESET_MS = 2_000L
