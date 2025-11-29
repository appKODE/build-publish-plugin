package ru.kode.android.build.publish.plugin.test.utils

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.service.TagService
import java.io.File

fun Grgit.addAll() {
    this.add(mapOf("patterns" to setOf(".")))
}

fun File.initGit(): Grgit {
    return Grgit.init(mapOf("dir" to this))
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

fun TagService.addNamedWithMessage(
    name: String,
    message: String,
) {
    this.add(mapOf("name" to name, "message" to message))
}

fun TagService.find(expectedTagName: String): Commit {
    return this.list().find { it.name == expectedTagName }!!.commit
}
