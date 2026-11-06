plugins {
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidApplicationConventionPlugin.kt
    alias(libs.plugins.nordic.android.application)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/HiltComposeConventionPlugin.kt
    alias(libs.plugins.nordic.feature.hilt.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.nrfmesh"
    defaultConfig {
        minSdk = 23
        compileSdk = 37
        applicationId = "no.nordicsemi.android.nrfmesh"
        multiDexEnabled = true
    }
}

dependencies {
    implementation(nordic.theme)
    implementation(nordic.permissions.ble)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Material3
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite-android:1.4.0")

    implementation(libs.timber)
    implementation(libs.slf4j.timber)

    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:nodes"))
    implementation(project(":feature:models"))
    implementation(project(":feature:groups"))
    implementation(project(":feature:proxy"))
    implementation(project(":feature:provisioning"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:network-keys"))
    implementation(project(":feature:config-network-keys"))
    implementation(project(":feature:config-application-keys"))
    implementation(project(":feature:bind-app-keys"))
    implementation(project(":feature:application-keys"))
    implementation(project(":feature:scenes"))
    implementation(project(":feature:provisioners"))
    implementation(project(":feature:export"))
    implementation(project(":feature:dfu"))
    implementation(project(":mesh:core"))
    implementation(project(":mesh:provisioning"))
    implementation(project(":feature:scanner"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)
}
