plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nordic.nexus.jvm)
}

group = "no.nordicsemi.kotlin.mesh"

nordicNexusPublishing {
    POM_ARTIFACT_ID = "crypto"
    POM_NAME = "Bluetooth Mesh Crypto Library"
    POM_DESCRIPTION = "Provides a set of cryptographic functions for the Kotlin Mesh Library."
    POM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_URL = "https://github.com/nordicsemi/Kotlin-Mesh-Library"
    POM_SCM_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
    POM_SCM_DEV_CONNECTION = "scm:git@github.com:nordicsemi/Kotlin-Mesh-Library.git"
}

dependencies {
    implementation(nordic.kotlin.data)
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    testImplementation(libs.kotlin.junit)
}

// Applies proguard rules to the crypto module
tasks.withType<Jar> {
    from("module-rules.pro") {
        into("META-INF/proguard/")
    }
}