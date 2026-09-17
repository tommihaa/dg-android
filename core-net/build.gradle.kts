plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core-domain"))
    // Sivun tunnistus (onko tämä login-sivu) on sivun asia, ei verkon.
    // Siksi riippuvuus kulkee näin päin, eikä toisin.
    implementation(project(":core-scrape"))
    // `api` eikä `implementation`: OkHttpin tyypit näkyvät ulos julkisista rajapinnoista
    // (`DgClient`in konstruktorin `baseUrl` ja `httpClient`, `CookieStore`in `Cookie`).
    // Kutsuja ei voi kääntyä ilman niitä, joten ne kuuluvat tämän moduulin sopimukseen.
    api(libs.okhttp)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(project(":core-scrape"))
    // Vain kaappaustestiä varten: haettua sivua katsotaan sen verran että osataan päättää
    // mikä linkki haetaan seuraavaksi. Jäsentimet asuvat `core-scrape`ssa, tämä ei ole
    // paikka niille.
    testImplementation(libs.jsoup)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform {
        // Verkkoa vaativat testit eivät kuulu tavalliseen ajoon: ne ovat hitaita,
        // riippuvat vieraasta palvelimesta ja vaativat tunnukset.
        excludeTags("live")
    }
}

// Erillinen tehtava oikeaa sivustoa vasten: ./gradlew :core-net:liveTest
val liveTest by tasks.registering(Test::class) {
    description = "Ajaa DailyGammonia vasten oikeilla tunnuksilla (local.properties)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("live")
    }
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
    }
}
