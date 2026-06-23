plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.feature.scanner"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    implementation(nordic.permissions.ble)
    implementation(nordic.scanner.ble)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)

    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":mesh:core"))
    implementation(project(":mesh:bearer-gatt"))
}