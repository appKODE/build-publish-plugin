package ru.kode.android.build.publish.plugin.git.entity

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
        override val message: String?
    ) : Tag()

    data class Build(
        override val name: String,
        override val commitSha: String,
        override val message: String?,
        val buildVariant: String,
        val buildNumber: Int
    ) : Tag() {
        constructor(tag: Tag, buildVariants: Set<String>) : this(
            tag.name,
            tag.commitSha,
            tag.message,
            buildVariant = buildVariants.firstOrNull { tag.name.contains(it) }
                ?: throw GradleException(
                    "No buildVariants for ${tag.name}. Available variants: $buildVariants",
                ),
            buildNumber = Regex("\\d+").findAll(tag.name).last().value.toInt()
        )
    }
}
