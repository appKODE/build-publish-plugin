plugins {
    id("kotlin-convention")
}

group = "ru.kode.android"
version = "1.0.0"

dependencies {
    implementation(gradleApi())
    implementation(gradleTestKit())
    implementation(libs.grgitCore)
}
