package fi.tommi.dg.app.ui

import android.os.SystemClock
import android.util.Log

/**
 * Kitkamittauksen aikaleimat logcatiin, tagilla `DgKitka`.
 *
 * Nauha (16.9.2026, `raakasivut/LUEMINUT.md`) mittasi napautuksesta uuden laudan piirtoon
 * 1,0–1,2 s, kun DG Mobile teki saman 0,4–0,6 sekunnissa, mutta nauha ei erota mihin aika
 * kuluu: lähetys, vastauksen odotus, jäsennys, kannan kirjoitus ja piirto ovat kehyksissä
 * yhtä hiljaista aikaa. Tämä luokka merkitsee ne kohdat, ja rivit luetaan `adb logcat -s
 * DgKitka`. Kello on [SystemClock.elapsedRealtime], jotta luvut ovat vertailukelpoisia
 * proxyn sekuntikellon sijaan sovelluksen sisällä.
 *
 * Yksi mittaus kerrallaan: [alku] avaa sen ja [merkki] kirjaa välin, ja [piirretty] sulkee
 * sen ensimmäisellä kehyksellä joka piirtää uuden tilan. Sama tila piirtyy monta kertaa
 * (recomposition), joten sulkeminen katsoo tilan identiteettiä eikä kirjaa toista kertaa.
 */
object KitkaLoki {
    private const val TAG = "DgKitka"

    @Volatile private var alkuhetki = 0L
    @Volatile private var tunniste = ""
    @Volatile private var odottaaPiirtoa: Any? = null

    /** Avaa mittauksen; [mita] on teko, esimerkiksi `press Roll Dice`. */
    fun alku(mita: String) {
        alkuhetki = SystemClock.elapsedRealtime()
        tunniste = mita
        odottaaPiirtoa = null
        Log.i(TAG, "alku $mita")
    }

    /** Kirjaa välikohdan millisekunteina mittauksen alusta. */
    fun merkki(nimi: String) {
        if (alkuhetki == 0L) return
        Log.i(TAG, "$nimi +${SystemClock.elapsedRealtime() - alkuhetki} ms ($tunniste)")
    }

    /** Tila jonka ensimmäistä piirtoa odotetaan; annetaan heti kun tila on asetettu. */
    fun odota(tila: Any) {
        odottaaPiirtoa = tila
    }

    /** Kutsutaan jokaisesta laudan piirrosta; kirjaa vain odotetun tilan ensimmäisen kerran. */
    fun piirretty(tila: Any) {
        if (tila !== odottaaPiirtoa) return
        odottaaPiirtoa = null
        Log.i(TAG, "piirretty +${SystemClock.elapsedRealtime() - alkuhetki} ms ($tunniste)")
    }
}
