package ru.kode.android.build.publish.plugin.git.entity

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
        val buildType: String,
        val buildNumber: Int
    ) : Tag() {
        constructor(tag: Tag, buildVariants: Set<String>) : this(
            tag.name,
            tag.commitSha,
            tag.message,
            buildType = buildVariants.first { tag.name.contains(it) },
            buildNumber = Regex("\\d+").findAll(tag.name).last().value.toInt()
        )
    }
}
