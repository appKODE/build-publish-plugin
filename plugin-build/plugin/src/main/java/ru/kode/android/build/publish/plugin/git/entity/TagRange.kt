package ru.kode.android.build.publish.plugin.git.entity

data class TagRange(
    val currentBuildTag: Tag,
    val previousBuildTag: Tag?
) {
    fun asCommitRange(): CommitRange {
        return CommitRange(previousBuildTag?.commitSha, currentBuildTag.commitSha)
    }
}
