plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.feature.groups"
}

dependencies {
    implementation(nordic.theme)
    implementation(nordic.kotlin.data)
    implementation(libs.kotlinx.serialization.json)

    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":feature:application-keys"))
    implementation(project(":mesh:core"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)
}