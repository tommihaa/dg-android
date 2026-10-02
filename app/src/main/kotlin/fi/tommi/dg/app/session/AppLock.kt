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
 * Onko laitteella ylipäätään mitään millä lukon voisi avata.
 *
 * Jos avaamiseen ei ole mitään, lukkoa ei näytetä lainkaan vaikka asetus olisi päällä:
 * sovellus lukkiutuisi muuten pysyvästi laitteella jolla ei ole omaa suojausta, eikä käyttäjä
 * pääsisi enää koskaan viesteihinsä käsiksi. Asetusruutu näyttää kytkimen silloin harmaana.
 *
 * **Lukko on asetus 27.9.2026 alkaen** (`AppLockStore`, oletus pois). Sitä ennen se oli
 * vakiolipun `APP_LOCK_ENABLED = false` takana 21.8.2026 lähtien.
 */
fun appLockAvailable(activity: FragmentActivity): Boolean =
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
