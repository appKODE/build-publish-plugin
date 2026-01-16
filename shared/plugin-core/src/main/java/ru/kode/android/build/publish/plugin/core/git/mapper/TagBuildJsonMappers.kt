package ru.kode.android.build.publish.plugin.core.git.mapper

import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.messages.fileCannotBeParsedMessage
import java.io.File

fun BuildTagSnapshot.toJson(): String {
    return Json.encodeToString(this)
}

@Suppress("ThrowsCount", "SwallowedException")
fun fromJson(file: File): BuildTagSnapshot {
    return try {
        Json.decodeFromString(file.readText())
    } catch (_: Exception) {
        throw GradleException(fileCannotBeParsedMessage(file))
    }
}
