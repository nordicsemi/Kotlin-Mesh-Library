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
        maven(url = "https://jitpack.io")
        maven(url = "https://androidx.dev/storage/compose-compiler/repository/")
    }
    versionCatalogs {
        create("libs") {
            from("no.nordicsemi.gradle:version-catalog:3.3-2")
            // The catalog ships androidx.hilt 1.2.0, which does not contain the
            // androidx.hilt.lifecycle.viewmodel.compose package used by the view models.
            version("androidxHilt", "1.3.0")
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

if (file("../Android-nRF-Connect-Device-Manager").exists()) {
    includeBuild("../Android-nRF-Connect-Device-Manager")
}
