plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nordic.nexus.jvm)
}

group = "no.nordicsemi.kotlin.mesh"

nordicNexusPublishing {
    POM_ARTIFACT_ID = "provisioning"
    POM_NAME = "Provisioning in Bluetooth Mesh"
    POM_DESCRIPTION =
        "Provides a Provisioning related functionality in Bluetooth Mesh for the Kotlin Mesh Library."
    POM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
    POM_SCM_DEV_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
}

dependencies {
    api(project(":mesh:core"))
    api(project(":mesh:bearer-provisioning"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(nordic.kotlin.data)
    // Dependencies used for testing
    implementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.junit)
}