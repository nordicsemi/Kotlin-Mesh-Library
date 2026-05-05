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
             from("no.nordicsemi.android.gradle:version-catalog:2.15")
        }
        create("nordic") {
            from("no.nordicsemi.android:version-catalog:2025.12.01")
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

include(":mesh:core")
include(":mesh:crypto")
include(":mesh:provisioning")
include(":mesh:logger")
include(":mesh:bearer")
include(":mesh:bearer-provisioning")
include(":mesh:bearer-pbgatt")
include(":mesh:bearer-gatt")

// if (file("../Android-Common-Libraries").exists()) {
//     includeBuild("../Android-Common-Libraries")
// }
// if (file("../Android-Gradle-Plugins").exists()) {
//     includeBuild("../Android-Gradle-Plugins")
// }
// if (file("../Kotlin-Util-Library").exists()) {
//     includeBuild("../Kotlin-Util-Library")
// }
// if (file("../Kotlin-BLE-Library").exists()) {
//     includeBuild("../Kotlin-BLE-Library")
// }
