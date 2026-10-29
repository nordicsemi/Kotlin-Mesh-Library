pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from("no.nordicsemi.gradle:version-catalog:3.3.1-1")
        }
        create("nordic") {
            from(files("gradle/nordic.versions.toml"))
        }
    }
}
rootProject.name = "Kotlin-Mesh-Library"
include(":app")
include(":core:ui")
include(":core:common")
include(":core:data")
include(":core:navigation")
include(":feature:nodes")
include(":feature:models")
include(":feature:groups")
include(":feature:settings")
include(":feature:proxy")
include(":feature:export")
include(":feature:network-keys")
include(":feature:config-network-keys")
include(":feature:application-keys")
include(":feature:bind-app-keys")
include(":feature:config-application-keys")
include(":feature:provisioners")
include(":feature:provisioning")
include(":feature:scenes")
include(":feature:ivindex")
include(":feature:developer-settings")
include(":feature:dfu")
include(":feature:scanner")

include(":mesh:core")
include(":mesh:crypto")
include(":mesh:provisioning")
include(":mesh:logger")
include(":mesh:bearer")
include(":mesh:bearer-provisioning")
include(":mesh:bearer-pbgatt")
include(":mesh:bearer-gatt")

// if (file("../Android-nRF-Connect-Device-Manager").exists()) {
//     includeBuild("../Android-nRF-Connect-Device-Manager")
// }
// if (file("../Kotlin-BLE-Library").exists()) {
//     includeBuild("../Kotlin-BLE-Library")
// }