plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.wire)
}


android {
    namespace = "no.nordicsemi.android.nrfmesh.core.data"
}

wire {
    kotlin {}
}

dependencies {
    implementation(nordic.permissions.ble)
    implementation(nordic.kotlin.data)
    implementation(nordic.blek.client.android)

    implementation("androidx.datastore:datastore-core:1.2.1")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.datastore:datastore-preferences-proto:1.2.1")

    implementation(project(":core:ui"))
    api(project(":core:common"))
    implementation(project(":mesh:core"))
    implementation(project(":mesh:bearer-pbgatt"))
    implementation(project(":mesh:bearer-gatt"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.androidx.test.rules)
}