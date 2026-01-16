package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.Tag

const val DEFAULT_VERSION_CODE = 1

/**
 * A strategy for generating the version code of the APK.
 */
interface VersionCodeStrategy {
    /**
     * Builds the version code of the APK based on the provided parameters.
     *
     * @param buildVariant The build variant information.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The version code of the APK. If the [Tag.Build.buildNumber] is not null, it will be used.
     *         Otherwise, the [DEFAULT_VERSION_CODE] will be used.
     */
    fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): Int
}

/**
 * A strategy for generating the version code of the APK using the build variant and the build number
 * obtained from the git tag.
 */
object BuildVersionCodeStrategy : VersionCodeStrategy {
    /**
     * Builds the version code of the APK based on the provided parameters.
     *
     * @param buildVariant The build variant information.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The version code of the APK. If the [Tag.Build.buildNumber] is not null, it will be used.
     *         Otherwise, the [DEFAULT_VERSION_CODE] will be used.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): Int {
        return tag?.buildNumber ?: DEFAULT_VERSION_CODE
    }
}

/**
 * A strategy for generating the version code of the APK with a fixed value.
 *
 * @property versionCodeProvider The provider of the fixed version code to use when generating the version code of the APK.
 */
class FixedVersionCodeStrategy(
    val versionCodeProvider: () -> Int,
) : VersionCodeStrategy {
    /**
     * Builds the version code of the APK based on the provided parameters.
     *
     * @param buildVariant The build variant information.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The version code of the APK. The fixed version code provided by [versionCodeProvider] will be used.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): Int {
        return versionCodeProvider()
    }
}

/**
 * A strategy for generating the version code of the APK based on the semantic version format.
 *
 * The generated version code is obtained by parsing the semantic version and flattening it into a single integer.
 * The format of the semantic version is: MAJOR.MINOR.PATCH, where MAJOR, MINOR and PATCH are integers.
 * The resulting version code is computed as: (MAJOR * 1000 + MINOR) * 1000 + PATCH.
 * If the tag is null, the default version code is used.
 */
object SemanticVersionFlattenedCodeStrategy : VersionCodeStrategy {
    /**
     * Builds the version code of the APK based on the provided parameters.
     *
     * The generated version code is obtained by parsing the semantic version and flattening it into a single integer.
     * The format of the semantic version is: MAJOR.MINOR.PATCH, where MAJOR, MINOR and PATCH are integers.
     * The resulting version code is computed as: (MAJOR * 1000 + MINOR) * 1000 + PATCH.
     * If the tag is null, the default version code is used.
     *
     * @param buildVariant The build variant information.
     * @param tag The build variant and build number information obtained from the git tag. The build version
     *            should be in the semantic version format.
     *
     * @return The version code of the APK.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): Int {
        return if (tag != null) {
            val major = tag.buildVersion.substringBefore(".").toInt()
            val minor = tag.buildVersion.substringAfter(".").toInt()
            (major * 1000 + minor) * 1000 + tag.buildNumber
        } else {
            DEFAULT_VERSION_CODE
        }
    }
}
