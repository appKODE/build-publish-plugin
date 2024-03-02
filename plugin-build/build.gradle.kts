@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = "ru.kode.android"
    version = "1.1.0-alpha21"
}
