plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.feature.proxy"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    implementation(nordic.permissions.ble)
    implementation(nordic.scanner.ble)

    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":mesh:core"))
    implementation(project(":mesh:bearer-gatt"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)
}