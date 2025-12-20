package ru.kode.android.build.publish.plugin.core.git.mapper

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.messages.fileCannotBeParsedMessage
import java.io.File

fun Tag.Build.toJson(): String {
    return JsonOutput.toJson(this)
}

@Suppress("ThrowsCount")
fun fromJson(file: File): Tag.Build {
    val parsedBuildTag = JsonSlurper().parse(file)
    return if ((parsedBuildTag as? Map<String, *>) != null) {
        Tag.Build(
            name =
                parsedBuildTag["name"] as? String
                    ?: throw GradleException("name not found"),
            commitSha =
                parsedBuildTag["commitSha"] as? String
                    ?: throw GradleException("commitSha not found"),
            message = parsedBuildTag["message"] as? String,
            buildVersion =
                parsedBuildTag["buildVersion"] as? String
                    ?: throw GradleException("buildVersion not found"),
            buildVariant =
                parsedBuildTag["buildVariant"] as? String
                    ?: throw GradleException("buildVariant not found"),
            buildNumber =
                parsedBuildTag["buildNumber"] as? Int
                    ?: throw GradleException("buildNumber not found"),
        )
    } else {
        throw GradleException(fileCannotBeParsedMessage(file))
    }
}
