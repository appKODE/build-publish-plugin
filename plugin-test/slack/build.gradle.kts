import java.util.Properties

plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.agp)
    implementation("ru.kode.android:plugin-foundation")
    implementation("ru.kode.android:plugin-slack")

    testImplementation("ru.kode.android:plugin-core")
    testImplementation(project(":utils"))

    testImplementation(gradleApi())
    testImplementation(libs.grgitCore)
    testImplementation(libs.grgitGradle)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit-pioneer:junit-pioneer:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    doFirst {
        systemProperty("JIRA_BASE_URL", project.getEnvOrProperty("JIRA_BASE_URL"))
        systemProperty("SLACK_WEBHOOK_URL", project.getEnvOrProperty("SLACK_WEBHOOK_URL"))
        systemProperty("SLACK_ICON_URL", project.getEnvOrProperty("SLACK_ICON_URL"))
        systemProperty("SLACK_UPLOAD_API_TOKEN", project.getEnvOrProperty("SLACK_UPLOAD_API_TOKEN"))
        systemProperty("SLACK_DISTRIBUTION_CHANNEL", project.getEnvOrProperty("SLACK_DISTRIBUTION_CHANNEL"))
        systemProperty("PROXY_USER", project.getEnvOrProperty("PROXY_USER"))
        systemProperty("PROXY_PASSWORD", project.getEnvOrProperty("PROXY_PASSWORD"))
        systemProperty("PROXY_HOST", project.getEnvOrProperty("PROXY_HOST"))
        systemProperty("PROXY_PORT", project.getEnvOrProperty("PROXY_PORT"))
    }

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
