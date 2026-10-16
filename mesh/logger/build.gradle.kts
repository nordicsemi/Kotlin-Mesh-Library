plugins {
    alias(libs.plugins.nordic.kotlin)
    alias(libs.plugins.nordic.publish.jvm)
}

group = "no.nordicsemi.kotlin.mesh"

nordicPublishing {
    POM_ARTIFACT_ID = "logger"
    POM_NAME = "Bluetooth Mesh Logger"
    POM_DESCRIPTION = "Provides a set of Mesh layer logging levels for the Kotlin Mesh Library."
    POM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
    POM_SCM_DEV_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
}