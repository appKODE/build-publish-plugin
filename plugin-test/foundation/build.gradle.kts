plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.agp)
    implementation("ru.kode.android:plugin-foundation")

    testImplementation("ru.kode.android:plugin-core")
    testImplementation(project(":utils"))

    testImplementation(gradleApi())
    testImplementation(libs.grgitCore)
    testImplementation(libs.grgitGradle)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStackTraces = true
        showExceptions = true
        showCauses = true
        showStandardStreams = true
    }
}
