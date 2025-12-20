package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.Tag

const val DEFAULT_VERSION_CODE = 1

interface VersionCodeStrategy {
    fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): Int
}

object BuildVersionCodeStrategy : VersionCodeStrategy {
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): Int {
        return tag?.buildNumber ?: DEFAULT_VERSION_CODE
    }
}

class FixedVersionCodeStrategy(
    val versionCodeProvider: () -> Int,
) : VersionCodeStrategy {
    override fun build(
        buildVariant: BuildVariant,
        tag: Tag.Build?,
    ): Int {
        return versionCodeProvider()
    }
}
