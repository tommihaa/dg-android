plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Riippuvuuslähteet ovat vain `settings.gradle.kts`:ssä. Täällä ollut `subprojects`-lohko
// poistettiin, koska projektikohtainen `repositories` voittaa settingsin määrittelyn: se
// pudotti `google()`:n pois, ja AndroidX-riippuvuudet jäivät löytymättä.
