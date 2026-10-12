plugins {
    alias(libs.plugins.ksp) apply false

    // Nordic plugins are defined in https://github.com/nordicsemi/Nordic-Gradle-Plugins
    alias(libs.plugins.nordic.android.application) apply false
    alias(libs.plugins.nordic.android.library) apply false
    alias(libs.plugins.nordic.kotlin) apply false
    alias(libs.plugins.nordic.feature.hilt) apply false
    alias(libs.plugins.nordic.feature.hilt.compose) apply false
    alias(libs.plugins.nordic.feature.compose) apply false
    alias(libs.plugins.nordic.publish.jvm) apply false
    alias(libs.plugins.nordic.publish.android) apply false

    alias(libs.plugins.kotlin.serialization) apply false

    // This applies Nordic look & feel to generated Dokka documentation.
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/NordicDokkaPlugin.kt
    alias(libs.plugins.nordic.dokka) apply true
}

// Configure main Dokka page
dokka {
    moduleName.set("Kotlin Mesh Library")
    pluginsConfiguration.html {
        homepageLink.set("https://github.com/nordicsemi/Kotlin-Mesh-Library")
    }
}