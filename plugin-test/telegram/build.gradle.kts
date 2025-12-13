import java.util.Properties

plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("ru.kode.android:plugin-foundation")
    implementation("ru.kode.android:plugin-telegram")

    testImplementation("ru.kode.android:plugin-core")
    testImplementation(project(":utils"))

    testImplementation(gradleApi())
    testImplementation(libs.agp)
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
        systemProperty("TELEGRAM_BOT_ID", project.getEnvOrProperty("TELEGRAM_BOT_ID"))
        systemProperty("TELEGRAM_CHAT_ID", project.getEnvOrProperty("TELEGRAM_CHAT_ID"))
        systemProperty("TELEGRAM_BOT_SERVER_BASE_URL", project.getEnvOrProperty("TELEGRAM_BOT_SERVER_BASE_URL"))
        systemProperty("TELEGRAM_BOT_SERVER_USERNAME", project.getEnvOrProperty("TELEGRAM_BOT_SERVER_USERNAME"))
        systemProperty("TELEGRAM_BOT_SERVER_PASSWORD", project.getEnvOrProperty("TELEGRAM_BOT_SERVER_PASSWORD"))
        systemProperty("PROXY_USER", project.getEnvOrProperty("PROXY_USER"))
        systemProperty("PROXY_PASSWORD", project.getEnvOrProperty("PROXY_PASSWORD"))
        systemProperty("PROXY_HOST", project.getEnvOrProperty("PROXY_HOST"))
        systemProperty("PROXY_PORT", project.getEnvOrProperty("PROXY_PORT"))
    }

    useJUnitPlatform()
    testLogging {
        showStackTraces = true
        showExceptions = true
        showCauses = true
        showStandardStreams = true
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
