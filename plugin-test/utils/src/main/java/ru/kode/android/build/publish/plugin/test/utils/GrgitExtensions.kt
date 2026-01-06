package ru.kode.android.build.publish.plugin.test.utils

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.service.BranchService
import org.ajoberstar.grgit.service.TagService
import java.io.File

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
