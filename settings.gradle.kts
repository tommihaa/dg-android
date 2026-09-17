// Kolme `core-*`-moduulia ovat pelkkää JVM-Kotlinia tarkoituksella: scrapingin voi ajaa
// JUnitilla ilman emulaattoria. Se raja säilyy vaikka Android-moduulit ovat nyt mukana,
// eli AGP:tä ei lisätä `core-*`:iin.

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "dg-android"

include(":core-domain")
include(":core-net")
include(":core-scrape")
include(":data")
include(":app")
