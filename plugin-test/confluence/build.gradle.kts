import java.util.Properties

plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.agp)
    implementation(libs.plugin.foundation)
    implementation(libs.plugin.confluence)
    implementation(libs.client.confluence)

    testImplementation(libs.plugin.core)
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
    systemProperty("CONFLUENCE_BASE_URL", project.getEnvOrProperty("CONFLUENCE_BASE_URL"))
    systemProperty("CONFLUENCE_USER_NAME", project.getEnvOrProperty("CONFLUENCE_USER_NAME"))
    systemProperty("CONFLUENCE_USER_PASSWORD", project.getEnvOrProperty("CONFLUENCE_USER_PASSWORD"))
    systemProperty("CONFLUENCE_PAGE_ID", project.getEnvOrProperty("CONFLUENCE_PAGE_ID"))
    systemProperty("PROXY_USER", project.getEnvOrProperty("PROXY_USER"))
    systemProperty("PROXY_PASSWORD", project.getEnvOrProperty("PROXY_PASSWORD"))
    systemProperty("PROXY_HOST", project.getEnvOrProperty("PROXY_HOST"))
    systemProperty("PROXY_PORT", project.getEnvOrProperty("PROXY_PORT"))

    useJUnitPlatform()
    testLogging {
        val isCI = System.getenv("CI") == "true"
        showStackTraces = true
        showExceptions = true
        showCauses = true
        showStandardStreams = !isCI
    }
}

private fun Project.getEnvOrProperty(name: String): String {
    System.getenv(name)?.let { return it }

    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        props.load(file.inputStream())
        props.getProperty(name)?.let { return it }
    }
    return "not_defined_stub"
}
