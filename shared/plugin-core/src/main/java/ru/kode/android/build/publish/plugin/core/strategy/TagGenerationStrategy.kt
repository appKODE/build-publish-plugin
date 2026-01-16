package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.Tag

const val DEFAULT_TAG_PATTERN = ".+\\.(\\d+)-%s"

const val DEFAULT_TAG_NAME = "v$DEFAULT_BUILD_VERSION.$DEFAULT_VERSION_CODE-%s"

const val DEFAULT_TAG_COMMIT_SHA = "hardcoded_default_stub_commit_sha"

const val DEFAULT_TAG_COMMIT_MESSAGE = "hardcoded_default_stub_commit_message"

/**
 * A strategy for generating the build tag metadata.
 *
 * @see [Tag.Build] for more details about the generated tag metadata.
 */
interface TagGenerationStrategy {
    /**
     * Generates the build tag metadata for the given [buildVariant].
     *
     * @param buildVariant The build variant to generate the tag metadata for.
     * @return The generated [Tag.Build] with the build tag metadata.
     */
    fun build(buildVariant: String): Tag.Build
}

/**
 * A strategy that generates the build tag metadata using hardcoded default stub values.
 *
 * This strategy is used as a fallback when the actual tag metadata can't be obtained from Git.
 *
 * @see [Tag.Build] for more details about the generated tag metadata.
 */
object HardcodedTagGenerationStrategy : TagGenerationStrategy {
    /**
     * Generates the build tag metadata using hardcoded default stub values.
     *
     * This strategy is used as a fallback when the actual tag metadata can't be obtained from Git.
     *
     * @param buildVariant The build variant for which the tag metadata is generated.
     * @return The build tag metadata with hardcoded default stub values.
     * @see [Tag.Build] for more details about the generated tag metadata.
     */
    override fun build(buildVariant: String): Tag.Build {
        return Tag.Build(
            name = DEFAULT_TAG_NAME.format(buildVariant),
            commitSha = DEFAULT_TAG_COMMIT_SHA,
            message = DEFAULT_TAG_COMMIT_MESSAGE,
            buildVersion = DEFAULT_BUILD_VERSION,
            buildVariant = buildVariant,
            buildNumber = DEFAULT_VERSION_CODE,
        )
    }
}
