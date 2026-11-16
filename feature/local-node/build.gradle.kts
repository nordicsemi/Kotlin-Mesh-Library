plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.android.library)
    alias(libs.plugins.nordic.feature.hilt.compose)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.feature.localnode"
}

dependencies {
    implementation(nordic.theme)
    implementation(nordic.kotlin.data)

    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":feature:models"))
    implementation(project(":feature:nodes"))
    implementation(project(":mesh:core"))
}
