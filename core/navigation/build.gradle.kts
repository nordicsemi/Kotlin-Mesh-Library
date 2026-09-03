plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt
    alias(libs.plugins.nordic.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.core.navigation"
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(project(":core:common"))

    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")

    // Material3
    api("androidx.compose.material3:material3-window-size-class:1.4.0")

    // Navigation3
    api("androidx.compose.material3.adaptive:adaptive-navigation3:1.3.0")
    api("androidx.navigation3:navigation3-runtime:1.1.7")
    api("androidx.lifecycle:lifecycle-viewmodel-navigation3-android:2.11.0")
}