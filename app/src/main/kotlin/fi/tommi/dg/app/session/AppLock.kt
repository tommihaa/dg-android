package fi.tommi.dg.app.session

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Sormenjälki/kasvot TAI laitteen oma PIN/kuvio/salasana kelpaavat molemmat. Viestiarkisto
 * ei saa jäädä lukkoon vain siksi ettei laitteella ole sormenjälkilukijaa.
 */
private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/**
 * **Sovelluslukko on toistaiseksi pois päältä** (Tommin päätös 21.8.2026: *"se on vaan
 * tiellä"*). Takaisin päälle vaihtamalla tämä `true`ksi, eikä muuta tarvita: koko lukko on
 * tämän lipun takana, ja sen ympärillä oleva koodi on ennallaan.
 *
 * **Mitä pois kytkeminen maksaa, jotta se on kirjattu eikä arvattu.** Viestiarkisto on
 * sovelluksen olemassaolon syy, ja se on ainoa kopio: sivusto ei säilytä viestejä, joten
 * arkistoa ei voi hakea uudelleen mistään. Lukko oli se mikä esti arkiston lukemisen
 * laitteen ollessa auki jonkun toisen kädessä. Laitteen oma lukitusnäyttö on yhä voimassa,
 * joten suoja ei katoa kokonaan vaan kutistuu yhteen kerrokseen.
 *
 * Lippu on vakio eikä asetus, koska asetus vaatisi oman ruutunsa, oman tallennuksensa ja
 * oman päätöksensä siitä mikä on oletus. Se on eri kokoinen työ kuin mitä pyydettiin, ja
 * *toistaiseksi* on juuri se sana joka tekee vakiosta oikean muodon.
 */
private const val APP_LOCK_ENABLED = false

/**
 * Kertoo onko lukko käytössä ja onko laitteella ylipäätään mitään millä sen voisi avata.
 *
 * Jos avaamiseen ei ole mitään, lukkoa ei näytetä lainkaan: sovellus lukkiutuisi muuten
 * pysyvästi laitteella jolla ei ole omaa suojausta, eikä käyttäjä pääsisi enää koskaan
 * viesteihinsä käsiksi.
 */
fun canRequestAppLock(activity: FragmentActivity): Boolean =
    APP_LOCK_ENABLED &&
        BiometricManager.from(activity).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

/**
 * Pyytää tunnistuksen käyttöjärjestelmän omalla ruudulla.
 *
 * [onFailure] kutsutaan sekä käyttäjän peruutuksesta että väärästä tunnisteesta; kumpikaan
 * ei ole poikkeuksellinen, joten kutsuja päättää itse näytetäänkö uudelleenyrityspainike.
 * Yksittäinen väärä yritys (`onAuthenticationFailed`) ei kutsu kumpaakaan, koska
 * käyttöjärjestelmä näyttää oman virheensä ja antaa yrittää heti uudestaan.
 */
fun requestAppLockAuthentication(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure()
            }
        },
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(fi.tommi.dg.app.R.string.app_lock_title))
        .setSubtitle(activity.getString(fi.tommi.dg.app.R.string.app_lock_subtitle))
        // Ei setNegativeButtonTextiä: se ei ole sallittu yhdessä DEVICE_CREDENTIALin
        // kanssa, ja järjestelmän oma peruutus (takaisin-nappi) riittää.
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()

    prompt.authenticate(promptInfo)
}
