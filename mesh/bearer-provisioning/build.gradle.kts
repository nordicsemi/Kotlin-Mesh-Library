plugins {
    alias(libs.plugins.nordic.kotlin)
    alias(libs.plugins.nordic.publish.jvm)
}

group = "no.nordicsemi.kotlin.mesh"

nordicPublishing {
    POM_ARTIFACT_ID = "bearer-provisioning"
    POM_NAME = "Provisioning Bearer extension in Bluetooth Mesh"
    POM_DESCRIPTION =
        "Provides an extension of the Bearer as a Provisioning Bearer in Bluetooth Mesh for the Kotlin Mesh Library."
    POM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
    POM_SCM_DEV_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
}

dependencies {
    api(project(":mesh:bearer"))
}