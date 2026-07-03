plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidApplicationComposeConventionPlugin.kt
    alias(libs.plugins.nordic.application.compose)
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidHiltConventionPlugin.kt
    alias(libs.plugins.nordic.hilt)
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
    implementation(libs.androidx.hilt.navigation.compose)

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
    implementation(project(":mesh:core"))
    implementation(project(":mesh:provisioning"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)
}
