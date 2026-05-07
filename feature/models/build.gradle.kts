plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh.feature.models"
}

dependencies {
    implementation(nordic.kotlin.data)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)

    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.kotlin.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.rules)

    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:bind-app-keys"))
    implementation(project(":feature:config-application-keys"))
    implementation(project(":feature:groups"))
    implementation(project(":mesh:core"))
}