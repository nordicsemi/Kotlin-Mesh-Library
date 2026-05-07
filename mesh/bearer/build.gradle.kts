plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nordic.nexus.jvm)
}

group = "no.nordicsemi.kotlin.mesh"

nordicNexusPublishing {
    POM_ARTIFACT_ID = "bearer"
    POM_NAME = "Bearer implementation for Bluetooth Mesh"
    POM_DESCRIPTION = "Provides the Bearer implementation for the Kotlin Mesh Library."
    POM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
    POM_SCM_DEV_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
}

dependencies {
    api(project(":mesh:logger"))
    implementation(nordic.kotlin.data)
    implementation(libs.kotlinx.coroutines.core)
    // Dependencies used for testing
    testImplementation(libs.kotlin.junit)
}