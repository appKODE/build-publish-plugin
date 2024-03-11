package ru.kode.android.build.publish.plugin.enity

import org.gradle.api.GradleException

sealed class Tag {
    abstract val name: String
    abstract val commitSha: String

    /**
     * A message from the tag object, will be present only for annotated tags
     */
    abstract val message: String?

    data class Generic(
        override val name: String,
        override val commitSha: String,
        override val message: String?,
    ) : Tag()

    data class Build(
        override val name: String,
        override val commitSha: String,
        override val message: String?,
        val buildVersion: String,
        val buildVariant: String,
        val buildNumber: Int,
    ) : Tag() {
        constructor(tag: Tag, buildVariant: String) : this(
            tag.name,
            tag.commitSha,
            tag.message,
            buildVersion = tag.toBuildVersion(),
            buildVariant = tag.toBuildVariant(buildVariant),
            buildNumber = tag.toBuildNumber(),
        )
    }
}

private fun Tag.toBuildVariant(buildVariant: String): String {
    return if (this.name.contains(buildVariant)) {
        buildVariant
    } else {
        throw GradleException(
            "No buildVariant for ${this.name}. Available variants: $buildVariant",
        )
    }
}

// TODO: Add logic to configure logic to extract build version outside
private fun Tag.toBuildVersion(): String {
    val tagFirstPart = name.split("-").first()
    val numbers = Regex("\\d+").findAll(tagFirstPart).toList()
    return numbers.dropLast(1).joinToString(separator = ".") { it.value }
}

// TODO: Add logic to configure logic to extract build number outside
private fun Tag.toBuildNumber(): Int {
    val tagFirstPart = name.split("-").first()
    return Regex("\\d+").findAll(tagFirstPart).last().value.toInt()
}
