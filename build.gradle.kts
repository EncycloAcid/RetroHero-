plugins {
    id("com.android.application") version "9.5.2" apply false
    // AGP 9 compiles Kotlin itself; this plugin adds the Compose compiler (and pins the Kotlin version).
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
