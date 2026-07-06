package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.entity.Tag
import ru.kode.android.build.publish.plugin.core.util.APK_FILE_EXTENSION
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy")
const val DEFAULT_BASE_APK_FILE_NAME = "dev"

/**
 * A strategy for generating the output file name of the APK.
 */
interface OutputApkNameStrategy {
    /**
     * Builds the output file name of the artifact based on the provided parameters.
     *
     * @param outputFileName The original output file name of the artifact.
     * @param tag The build variant and build number information obtained from the git tag.
     * @param baseFileName The base file name to use when generating the output file name.
     * @return The final output file name of the artifact.
     */
    fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String
}

/**
 * A strategy for generating the output file name of the artifact with versioned names.
 *
 * The generated file name includes the version name and version code obtained from the git tag,
 * along with the current date. For example, output file name may look like:
 * `app-debug-vc123-19072023.apk`.
 */
object VersionedApkNamingStrategy : OutputApkNameStrategy {
    /**
     * Builds the output file name of the artifact with versioned names.
     *
     * The generated file name includes the version name and version code obtained from the git tag,
     * along with the current date. For example, output file name may look like:
     * `app-debug-vc123-19072023.apk`.
     *
     * @param outputFileName The original output file name of the artifact.
     * @param tag The build variant and build number information obtained from the git tag.
     * @param baseFileName The base file name to use when generating the output file name.
     * @return The final output file name of the artifact.
     */
    override fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String {
        val formattedDate = LocalDate.now().format(DATE_TIME_FORMATTER)
        val isApk = outputFileName.endsWith(".${APK_FILE_EXTENSION}")
        return if (tag != null && isApk) {
            val versionName = tag.buildVariant
            val versionCode = tag.buildNumber
            "$baseFileName-$versionName-vc$versionCode-$formattedDate.${APK_FILE_EXTENSION}"
        } else if (tag == null && isApk) {
            "$baseFileName-$formattedDate.${APK_FILE_EXTENSION}"
        } else {
            createDefaultOutputFileName(baseFileName, outputFileName)
        }
    }
}

/**
 * A strategy for generating the output file name of the artifact with simple names.
 *
 * The generated file name includes the [baseFileName] if provided, otherwise a default base file
 * name is used. For example, output file name may look like: `dev.apk`.
 */
object SimpleApkNamingStrategy : OutputApkNameStrategy {
    /**
     * Builds the output file name of the artifact with simple names.
     *
     * The generated file name includes the [baseFileName] if provided, otherwise a default base file
     * name is used. For example, output file name may look like: `dev.apk`.
     *
     * @param outputFileName The original output file name of the artifact.
     * @param tag The build variant and build number information obtained from the git tag.
     * @param baseFileName The base file name to use when generating the output file name.
     * @return The final output file name of the artifact.
     */
    override fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String {
        return createDefaultOutputFileName(baseFileName, outputFileName)
    }
}

/**
 * A strategy for generating the output file name of the artifact with a fixed name.
 *
 * The generated file name is obtained from the provided [nameProvider]. The file name includes
 * the extension obtained from the original output file name. For example, output file name may
 * look like: `app-debug.apk`.
 *
 * @property nameProvider The provider of the fixed name to use when generating the output file name.
 */
class FixedApkNamingStrategy(
    private val nameProvider: () -> String,
) : OutputApkNameStrategy {
    /**
     * Builds the output file name of the artifact with a fixed name.
     *
     * The generated file name is obtained from the provided [nameProvider]. The file name includes
     * the extension obtained from the original output file name. For example, output file name may
     * look like: `app-debug.apk`.
     *
     * @param outputFileName The original output file name of the artifact.
     * @param tag The build variant and build number information obtained from the git tag.
     * @param baseFileName The base file name to use when generating the output file name.
     * @return The final output file name of the artifact.
     */
    override fun build(
        outputFileName: String,
        tag: Tag.Build?,
        baseFileName: String?,
    ): String {
        val extension = outputFileName.substringAfterLast('.', APK_FILE_EXTENSION)
        return "${nameProvider()}.$extension"
    }
}

/**
 * Creates the default output file name when no specific strategy is provided.
 *
 * The generated file name includes the base file name if provided, otherwise a default base file
 * name is used. The file name includes the extension obtained from the original output file name.
 * For example, output file name may look like: `dev.apk`.
 *
 * @param baseFileName The base file name to use when generating the output file name.
 * @param outputFileName The original output file name of the artifact.
 * @return The default output file name of the artifact.
 */
private fun createDefaultOutputFileName(
    baseFileName: String?,
    outputFileName: String,
): String {
    val fileType = outputFileName.substringAfterLast('.', APK_FILE_EXTENSION)
    return "${baseFileName ?: DEFAULT_BASE_APK_FILE_NAME}.$fileType"
}
