package ru.kode.android.build.publish.plugin.git.entity

data class CommitRange(
    /**
     * SHA of first commit. `null` value will be treated as a reference to initial commit
     */
    val sha1: String?,
    /**
     * SHA of second commit
     */
    val sha2: String
)
