package ru.kode.android.build.publish.plugin.test.utils

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.service.BranchService
import org.ajoberstar.grgit.service.TagService
import org.ajoberstar.grgit.util.JGitUtil
import org.eclipse.jgit.lib.PersonIdent
import java.io.File
import java.time.Instant
import java.time.ZoneId

fun Grgit.addAll() {
    this.add(mapOf("patterns" to setOf(".")))
}

fun File.initGit(bare: Boolean = false): Grgit {
    return Grgit.init(
        if (bare) {
            mapOf("dir" to this, "bare" to true)
        } else {
            mapOf("dir" to this)
        }
    )
}

fun Grgit.commit(message: String) {
    this.commit(mapOf("message" to message))
}

fun Grgit.addAllAndCommit(message: String) {
    this.addAll()
    this.commit(message)
}

fun TagService.addNamed(name: String) {
    this.add(mapOf("name" to name))
}

fun BranchService.addNamed(name: String) {
    this.add(mapOf("name" to name))
}

fun TagService.addAnnotated(name: String) {
    this.add(mapOf("name" to name, "annotate" to true))
}

fun Grgit.checkoutBranch(name: String) {
    this.checkout(mapOf("branch" to name))
}

fun TagService.addNamedWithMessage(
    name: String,
    message: String,
) {
    this.add(mapOf("name" to name, "message" to message))
}

fun TagService.findTag(expectedTagName: String): Commit {
    return this.list().find { it.name == expectedTagName }!!.commit
}

fun Grgit.commitAmend(message: String) {
    this.commit(
        mapOf(
            "amend" to true,
            "message" to message
        )
    )
}

fun Grgit.switchBranch(name: String) {
    this.checkout(
        mapOf("branch" to name)
    )
}

fun Grgit.createAndSwitchBranch(name: String) {
    this.checkout(
        mapOf(
            "branch" to name,
            "createBranch" to true
        )
    )
}

fun Grgit.currentBranch(): String {
    return this.branch.current().name
}

fun Grgit.commitWithDate(message: String, date: Instant): Commit {
    val author = PersonIdent(
        "author",
        "author@example.com",
        date,
        ZoneId.systemDefault()
    )
    val commit = this.repository.jgit.commit().apply {
        this.message = message
        this.author = author
        this.committer = author
    }.call()
    return JGitUtil.convertCommit(this.repository, commit)
}

fun Grgit.checkoutCommitDetached(commitSha: String) {
    val repo = this.repository.jgit
    repo.checkout()
        .setName(commitSha)
        .call()
}
