package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.Tag
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy")
const val DEFAULT_BASE_FILE_NAME = "dev"

interface OutputApkNameStrategy {
    fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String
}

object VersionedApkNamingStrategy : OutputApkNameStrategy {
    override fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String {
        val formattedDate = LocalDate.now().format(DATE_TIME_FORMATTER)
        return if (tag != null && outputFileName.endsWith(".apk")) {
            val versionName = tag.buildVariant
            val versionCode = tag.buildNumber
            "$baseFileName-$versionName-vc$versionCode-$formattedDate.apk"
        } else if (tag == null && outputFileName.endsWith(".apk")) {
            "$baseFileName-$formattedDate.apk"
        } else {
            createDefaultOutputFileName(baseFileName, outputFileName)
        }
    }
}

object SimpleApkNamingStrategy : OutputApkNameStrategy {
    override fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String {
        return createDefaultOutputFileName(baseFileName, outputFileName)
    }
}

class FixedApkNamingStrategy(
    private val nameProvider: () -> String
) : OutputApkNameStrategy {
    override fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String {
        return "${nameProvider()}.apk"
    }
}

private fun createDefaultOutputFileName(
    baseFileName: String?,
    outputFileName: String,
): String {
    val fileType = outputFileName.split(".").last()
    return "${baseFileName ?: DEFAULT_BASE_FILE_NAME}.$fileType"
}
