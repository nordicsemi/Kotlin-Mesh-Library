plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt
    alias(libs.plugins.nordic.library.compose)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.core.ui"
}

dependencies {
    api(nordic.ui)
    api(libs.androidx.compose.material.icons.extended)
    implementation(nordic.logger)
    implementation(nordic.log.timber)

    implementation(project(":core:common"))
    implementation(project(":core:navigation"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)

    implementation("androidx.compose.material3:material3:1.4.0")

    implementation(project(":mesh:core"))
}