@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = PluginCoordinates.GROUP
    version = PluginCoordinates.VERSION
}
