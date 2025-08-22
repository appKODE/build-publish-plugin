package ru.kode.android.build.publish.plugin.foundation.utils

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.service.TagService
import java.io.File

internal fun Grgit.addAll() {
    this.add(mapOf("patterns" to setOf(".")))
}

internal fun File.initGit(): Grgit {
    return Grgit.init(mapOf("dir" to this))
}

internal fun Grgit.commit(message: String) {
    this.commit(mapOf("message" to message))
}

internal fun Grgit.addAllAndCommit(message: String) {
    this.addAll()
    this.commit(message)
}

internal fun TagService.addNamed(name: String) {
    this.add(mapOf("name" to name))
}

internal fun TagService.find(expectedTagName: String): Commit {
    return this.list().find { it.name == expectedTagName }!!.commit
}
