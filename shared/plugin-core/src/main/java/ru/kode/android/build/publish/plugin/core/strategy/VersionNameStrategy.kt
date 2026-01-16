package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.Tag

const val DEFAULT_BUILD_VERSION = "0.0"

/**
 * A strategy for generating the version name of the APK.
 *
 * The generated version name can be obtained from the provided [Tag.Build] information or
 * a fixed value obtained from [versionNameProvider] if the tag is null.
 */
interface VersionNameStrategy {
    /**
     * Builds the version name of the APK based on the provided [BuildVariant] and [Tag.Build].
     *
     * @param buildVariant The build variant for which the version name is being generated.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The generated version name of the APK.
     */
    fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String
}

/**
 * A strategy for generating the version name of the APK based on the [Tag.Build] information.
 *
 * The generated version name is obtained from the [Tag.Build.buildVersion] field. If the tag is null,
 * a default value is used.
 */
object BuildVersionNameStrategy : VersionNameStrategy {
    /**
     * Builds the version name of the APK based on the provided [BuildVariant] and [Tag.Build].
     *
     * @param buildVariant The build variant for which the version name is being generated.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The generated version name of the APK. If the tag is null, a default value
     *         ([DEFAULT_BUILD_VERSION]) is used.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return tag?.buildVersion ?: DEFAULT_BUILD_VERSION
    }
}

/**
 * A strategy for generating the version name of the APK based on the [Tag.Build] information.
 *
 * The generated version name is obtained from the [Tag.Build.buildVersion] and [Tag.Build.buildNumber] fields.
 * If the tag is null, a default value is used.
 */
object BuildVersionNumberNameStrategy : VersionNameStrategy {
    /**
     * Builds the version name of the APK based on the provided [BuildVariant] and [Tag.Build].
     *
     * @param buildVariant The build variant for which the version name is being generated.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The generated version name of the APK. If the tag is null, a default value
     *         (<code>$DEFAULT_BUILD_VERSION.$DEFAULT_VERSION_CODE</code>) is used.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return if (tag != null) {
            "${tag.buildVersion}.${tag.buildNumber}"
        } else {
            "$DEFAULT_BUILD_VERSION.$DEFAULT_VERSION_CODE"
        }
    }
}

/**
 * A strategy for generating the version name of the APK with a fixed value.
 *
 * @property versionNameProvider The provider of the fixed version name to use when generating the version name of the APK.
 */
class FixedVersionNameStrategy(
    val versionNameProvider: () -> String,
) : VersionNameStrategy {
    /**
     * Builds the version name of the APK based on the provided [BuildVariant] and [Tag.Build].
     *
     * @param buildVariant The build variant for which the version name is being generated.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The generated version name of the APK. The fixed value is obtained from the
     *         [versionNameProvider].
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return versionNameProvider()
    }
}

/**
 * A strategy for generating the version name of the APK based on the [Tag.Build] information
 * and the [BuildVariant] name.
 *
 * The generated version name is obtained from the [Tag.Build.buildVersion] and [Tag.Build.buildNumber] fields
 * and the [BuildVariant.name]. If the tag is null, a default value is used.
 */
object BuildVersionNumberVariantNameStrategy : VersionNameStrategy {
    /**
     * Builds the version name of the APK based on the provided [BuildVariant] and [Tag.Build].
     *
     * @param buildVariant The build variant for which the version name is being generated.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The generated version name of the APK. If the tag is null, a default value
     *         (<code>$DEFAULT_BUILD_VERSION-${buildVariant.name}</code>) is used.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return if (tag != null) {
            "${tag.buildVersion}.${tag.buildNumber}-${buildVariant.name}"
        } else {
            "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
        }
    }
}

/**
 * A strategy for generating the version name of the APK based on the [Tag.Build] information
 * and the [BuildVariant] name.
 *
 * The generated version name is obtained from the [Tag.Build.buildVersion] and [BuildVariant.name].
 * If the tag is null, a default value is used.
 */
object BuildVersionVariantNameStrategy : VersionNameStrategy {
    /**
     * Builds the version name of the APK based on the provided [BuildVariant] and [Tag.Build].
     *
     * @param buildVariant The build variant for which the version name is being generated.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The generated version name of the APK. If the tag is null, a default value
     *         (<code>$DEFAULT_BUILD_VERSION-${buildVariant.name}</code>) is used.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return if (tag != null) {
            "${tag.buildVersion}-${buildVariant.name}"
        } else {
            "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
        }
    }
}

/**
 * A strategy for generating the version name of the APK based on the [Tag.Build.name] field.
 *
 * The generated version name is obtained from the [Tag.Build.name] field. If the tag is null,
 * a default value is used.
 */
object TagRawNameStrategy : VersionNameStrategy {
    /**
     * Builds the version name of the APK based on the [Tag.Build.name] field.
     *
     * @param buildVariant The build variant for which the version name is being generated.
     * @param tag The build variant and build number information obtained from the git tag.
     *
     * @return The generated version name of the APK. If the tag is null, a default value
     *         (<code>$DEFAULT_TAG_NAME</code>) is used.
     */
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return tag?.name ?: DEFAULT_TAG_NAME
    }
}
