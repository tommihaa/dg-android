package fi.tommi.dg.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Ilmoitus siitä että yhteys on palannut.
 *
 * Rajapinta eikä luokka samasta syystä kuin [PageFetcher]: näkymämalli ja ruutu halutaan
 * ajaa ilman laitetta, eikä `ConnectivityManager` ole ajettavissa yksikkötestissä.
 *
 * **Virta kertoo vain palaamisesta, ei nykytilasta.** Se on rajaus eikä puute. Nykytila
 * vastaisi kysymykseen *onko yhteyttä*, ja siihen vastaa jo se mitä edellinen haku palautti;
 * kaksi lähdettä samasta asiasta olisi kaksi totuutta. Tässä tarvitaan vain se hetki jona
 * kannattaa yrittää uudelleen.
 */
fun interface NetworkAvailability {
    fun availability(): Flow<Unit>
}

/**
 * Androidin oma ilmoitus, rajattuna niihin verkkoihin joilla on internet.
 *
 * `NET_CAPABILITY_INTERNET` on ehtona siksi että ilman sitä `onAvailable` laukeaa myös
 * verkosta joka ei vie mihinkään, ja silloin uudelleenyritys epäonnistuisi heti. Ehto on
 * mitattavissa lentotilakokeella (`docs/TESTAUS.md`), ja juuri se koe oli syy tähän
 * tiedostoon.
 */
fun androidNetworkAvailability(context: Context): NetworkAvailability {
    val appContext = context.applicationContext
    return NetworkAvailability {
        callbackFlow {
            val manager = appContext.getSystemService(ConnectivityManager::class.java)
            if (manager == null) {
                // Ei ilmoituksia, mutta ei myöskään kaatumista: ruutu jää siihen tilaan
                // jossa käsin painettu Refresh on ainoa tie eteenpäin, eli entiselleen.
                close()
                return@callbackFlow
            }
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(Unit)
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            manager.registerNetworkCallback(request, callback)
            awaitClose { manager.unregisterNetworkCallback(callback) }
        }
    }
}
