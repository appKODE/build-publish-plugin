package ru.kode.android.build.publish.plugin.git.mapper

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.git.entity.Tag
import java.io.File

fun Tag.Build.toJson(): String {
    return JsonOutput.toJson(this)
}

@Suppress("ThrowsCount")
fun fromJson(file: File): Tag.Build {
    val parsedBuildTag = JsonSlurper().parse(file)
    return if ((parsedBuildTag as? Map<String, *>) != null) {
        Tag.Build(
            name = parsedBuildTag["name"] as? String
                ?: throw GradleException("name not found"),
            commitSha = parsedBuildTag["commitSha"] as? String
                ?: throw GradleException("commitSha not found"),
            message = parsedBuildTag["message"] as? String,
            buildVariant = parsedBuildTag["buildVariant"] as? String
                ?: throw GradleException("buildVariant not found"),
            buildNumber = parsedBuildTag["buildNumber"] as? Int
                ?: throw GradleException("buildNumber not found")
        )
    } else {
        throw GradleException("file $file can't be parsed: it has wrong data or different object")
    }
}
