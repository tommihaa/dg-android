import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Allekirjoitustiedot luetaan repon ulkopuolelta. Tiedosto on gitignoressa ja itse
// avainvarasto asuu repon ulkopuolella, ks. keystore.properties.example.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = if (keystorePropsFile.exists()) {
    Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
} else {
    null
}

android {
    namespace = "fi.tommi.dg.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "fi.tommi.dg"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        // Kielivalikoima on tässä eikä pelkästään res-kansioiden olemassaolossa: näin
        // kääntämättä jäänyt kieli ei pääse mukaan puolivalmiina.
        //
        // Käyttöliittymä on pelkkää englantia (päätös 5.8.2026, ks. docs/AVOIMET.md). Rivi jää
        // silti tähän: ilman sitä kirjastojen mukanaan tuomat käännökset päätyisivät
        // pakettiin, ja sovellus puhuisi kahta kieltä sekaisin laitteen kielen mukaan.
        resourceConfigurations += listOf("en")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    signingConfigs {
        if (keystoreProps != null) {
            create("julkaisu") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("julkaisu")
        }
        // Poikkeus tavallisesta: myös debug allekirjoitetaan julkaisuavaimella.
        //
        // Vakiokäytäntö on antaa debugin käyttää koneen jaettua debug-avainta, mutta
        // silloin laitteella olevaa debug-asennusta ei voi koskaan päivittää release-
        // versioksi: allekirjoitus ei täsmää, asennus on poistettava ja **viestiarkisto
        // lähtee mukana**. Arkisto on ainoa olemassa oleva kopio, ja allowBackup="false"
        // estää varmuuskopion tarkoituksella, joten menetys olisi lopullinen.
        //
        // Jos keystore.properties puuttuu, debug palaa oletusavaimeen jotta projektin saa
        // käännettyä ilman avainta. Silloin laitteelle ei pidä asentaa mitään mitä aikoo
        // säilyttää.
        debug {
            signingConfigs.findByName("julkaisu")?.let { signingConfig = it }
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // android.util.Log ja SystemClock palauttavat JVM-testissa oletusarvon eivatka heita:
        // KitkaLoki (kitkamittauksen aikaleimat) kutsuu niita BoardViewModelin jokaisessa haussa.
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-net"))
    implementation(project(":core-scrape"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)
    // **Fragment nostetaan käsin, ja se on korjaus eikä siivous (1.9.2026).**
    //
    // `biometric:1.1.0` on tämän ainoa lähde, ja se tuo `fragment:1.2.5`:n. Siinä
    // versiossa `FragmentActivity.startActivityForResult` vaatii että pyyntökoodi mahtuu
    // 16 bittiin, kun taas `ActivityResultRegistry` (jonka päällä
    // `rememberLauncherForActivityResult` on) arpoo koodit sen yläpuolelta. Yhdistelmä
    // kaataa **jokaisen** tiedostonvalitsimen heti napautuksesta:
    // `IllegalArgumentException: Can only use lower 16 bits for requestCode`.
    //
    // Mitattu laitteella 1.9.2026 varmuuskopion napista, ja sama vika koski arkiston
    // `Export`ia siitä asti kun se lisättiin: yksikkötestit kattoivat JSONin, eivät
    // valitsimen avaamista. `MainActivity` on `FragmentActivity` koska `BiometricPrompt`
    // vaatii sen, joten aktiviteettia ei voi vaihtaa; nostettava on fragment.
    implementation(libs.androidx.fragment)
    implementation(libs.coroutines.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    // SharedPreferences tarvitsee Androidin. Keystore ei toimi Robolectricillä, ja siksi
    // salaus on erotettu omaksi luokakseen jonka voi testata tavallisella AES-avaimella.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
