plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core-domain"))
    implementation(libs.jsoup)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform {
        // Korpusmittaukset lukevat `raakasivut/`-kansiota, joka on koneella eikä repossa.
        // Ne eivät kuulu tavalliseen ajoon: tulos riippuu siitä mitä on kaapattu.
        excludeTags("korpus")
    }
}

// Erillinen tehtävä kaapattua korpusta vasten: ./gradlew :core-scrape:korpusTest
// Raportti tulostuu ja tallentuu `core-scrape/build/korpus/`-kansioon.
val korpusTest by tasks.registering(Test::class) {
    description = "Ajaa korpusmittaukset raakasivut/-kansiota vasten."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("korpus")
    }
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
    }
}
