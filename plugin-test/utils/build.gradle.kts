plugins {
    id("kotlin-convention")
}

group = "ru.kode.android"
version = libs.versions.buildPublishShared.get()

dependencies {
    implementation(gradleApi())
    implementation(gradleTestKit())
    implementation(libs.grgitCore)
}
