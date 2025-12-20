// Archivo: build.gradle.kts (Raíz)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Solo definimos la versión aquí con apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}