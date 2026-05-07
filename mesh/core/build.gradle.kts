plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.nordic.nexus.jvm)
}

group = "no.nordicsemi.kotlin.mesh"

nordicNexusPublishing {
    POM_ARTIFACT_ID = "core"
    POM_NAME = "Bluetooth Mesh Core Library"
    POM_DESCRIPTION = "Provides a complete set of Bluetooth Mesh features for the Kotlin Mesh Library."
    POM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
    POM_SCM_DEV_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
}

dependencies {
    api(project(":mesh:bearer"))
    api(project(":mesh:crypto"))
    api(project(":mesh:logger"))
    implementation(nordic.kotlin.data)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    // Dependencies used for testing
    testImplementation(libs.kotlin.test)
}