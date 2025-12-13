package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.Tag

const val DEFAULT_TAG_PATTERN = ".+\\.(\\d+)-%s"

const val DEFAULT_TAG_NAME = "v$DEFAULT_BUILD_VERSION.$DEFAULT_VERSION_CODE-%s"

const val DEFAULT_TAG_COMMIT_SHA = "hardcoded_default_stub_commit_sha"

const val DEFAULT_TAG_COMMIT_MESSAGE = "hardcoded_default_stub_commit_message"

interface TagGenerationStrategy {
    fun build(buildVariant: String): Tag.Build
}

object HardcodedTagGenerationStrategy : TagGenerationStrategy {
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