package fi.tommi.dg.app.ui

import android.os.SystemClock
import android.util.Log

/**
 * Käännösmerkin ([RotationWait]) syyt logcatiin, tagilla `DgKaanto`. Luetaan `adb logcat -s
 * DgKaanto` ja rinnalle WindowManagerin *rotation changed* -rivit.
 *
 * Lisätty 18.10.2026 (Tommin valinta): pelisessiossa `sessio-18-10-ilta2` merkki oli ruudulla
 * noin 8 s, vaikka kasvojentunnistus oli pois, eikä Androidin loki kertonut miksi kääntö viipyi.
 * Sovelluksen anturi piti laitetta sivuttaisena, Android ei. Kallistus kertoo oliko laite niin
 * lähellä vaakatasoa että Android hylkää käännön, mitä sovelluksen kulma-anturi ei huomioi.
 * Rivit kirjataan vain tilan vaihtuessa, joten loki ei kasva laitetta pideltäessä.
 */
object KaantoLoki {
    private const val TAG = "DgKaanto"

    /** Anturin asento vaihtui: kulma 0–359, kallistus asteina ([tiltDegrees]), uusi asento. */
    fun anturi(kulma: Int, kallistus: Float, sivuttain: Boolean?) {
        Log.i(TAG, "anturi kulma=$kulma kallistus=${"%.0f".format(kallistus)} sivuttain=$sivuttain ${aika()}")
    }

    /** Anturin ja ikkunan ristiriita alkoi tai päättyi. */
    fun ristiriita(on: Boolean, ikkunaVaaka: Boolean) {
        Log.i(TAG, "ristiriita=$on ikkuna=${if (ikkunaVaaka) "vaaka" else "pysty"} ${aika()}")
    }

    /** Merkki tuli ruudulle tai poistui. */
    fun merkki(nakyy: Boolean) {
        Log.i(TAG, "merkki=${if (nakyy) "näkyy" else "pois"} ${aika()}")
    }

    /** Sovellus pakotti suunnan itse ([RotationWait], oma kääntö). */
    fun oma(suunta: Int, syy: String) {
        Log.i(TAG, "oma kääntö suunta=$suunta syy=$syy ${aika()}")
    }

    private fun aika() = "t=${SystemClock.elapsedRealtime()}"
}
