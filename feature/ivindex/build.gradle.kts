plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.android.library)
    alias(libs.plugins.nordic.feature.hilt.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.feature.ivindex"
}

dependencies {
    implementation(nordic.kotlin.data)
    implementation(libs.kotlinx.datetime)

    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":mesh:core"))

    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")
}