package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.Tag

const val DEFAULT_BUILD_VERSION = "0.0"

interface VersionNameStrategy {
    fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String
}

object BuildVersionNameStrategy : VersionNameStrategy {
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return tag?.buildVersion ?: DEFAULT_BUILD_VERSION
    }
}

object BuildVersionNumberNameStrategy : VersionNameStrategy {
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

class FixedVersionNameStrategy(
    val versionNameProvider: () -> String,
) : VersionNameStrategy {
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return versionNameProvider()
    }
}

object BuildVersionVariantNameStrategy : VersionNameStrategy {
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

object TagRawNameStrategy : VersionNameStrategy {
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): String {
        return tag?.name ?: DEFAULT_TAG_NAME
    }
}
