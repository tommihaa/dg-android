plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "fi.tommi.dg.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Sama toolchain kuin core-moduuleissa: koneella on JDK 21, eikä kahta eri versiota
// kannata pitää yllä saman repon sisällä.
kotlin {
    jvmToolchain(21)
}

// Skeema viedään versioon. Se on ainoa koneluettava todiste siitä miltä kanta oikeasti
// näyttää, ja migraatiotesti tarvitsee vanhan version tiedostona.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    api(project(":core-domain"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.coroutines.core)
    ksp(libs.room.compiler)

    // Room ajetaan tavallisina JVM-testeinä Robolectricillä, koska laitetta ei ole.
    // Siksi tämä moduuli käyttää JUnit 4:ää, toisin kuin core-*-moduulit (JUnit 5):
    // Robolectric on JUnit 4:n ajuri, eikä sitä kannata kiertää.
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.coroutines.test)
}
