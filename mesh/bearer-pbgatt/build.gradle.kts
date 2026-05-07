plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nordic.nexus.jvm)
}

group = "no.nordicsemi.kotlin.mesh"

nordicNexusPublishing {
    POM_ARTIFACT_ID = "bearer-pbgatt"
    POM_NAME = "PB-GATT extension for Bluetooth Mesh"
    POM_DESCRIPTION = "Provides a GATT implementation of the Provisioning bearer for the Kotlin Mesh Library."
    POM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
    POM_SCM_DEV_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
}

dependencies {
    api(project(":mesh:provisioning"))
    api(project(":mesh:bearer-gatt"))
    // Dependencies used for testing
    testImplementation(libs.kotlin.junit)
}