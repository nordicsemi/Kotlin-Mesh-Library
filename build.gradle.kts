plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false

    // Nordic plugins are defined in https://github.com/nordicsemi/Nordic-Gradle-Plugins
    alias(libs.plugins.nordic.application) apply false
    alias(libs.plugins.nordic.application.compose) apply false
    alias(libs.plugins.nordic.library) apply false
    alias(libs.plugins.nordic.library.compose) apply false
    alias(libs.plugins.nordic.hilt) apply false
    alias(libs.plugins.nordic.feature) apply false
    alias(libs.plugins.nordic.kotlin.android) apply false
    alias(libs.plugins.nordic.nexus.jvm) apply false
    alias(libs.plugins.nordic.nexus.android) apply false

    // alias(libs.plugins.wire) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    // This plugin is used to generate Dokka documentation.
    alias(libs.plugins.kotlin.dokka) apply false
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