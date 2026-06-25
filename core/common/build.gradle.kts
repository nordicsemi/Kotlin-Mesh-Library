plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.library)
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidHiltConventionPlugin.kt
    alias(libs.plugins.nordic.hilt)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.core.common"
}

dependencies {
    implementation(project(":mesh:core"))
    implementation(project(":mesh:provisioning"))
    implementation(project(":mesh:logger"))
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")

    api(nordic.blek.client.android)
    api(nordic.kotlin.data)
}