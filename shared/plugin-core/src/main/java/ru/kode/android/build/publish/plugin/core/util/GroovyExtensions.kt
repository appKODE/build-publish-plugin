package ru.kode.android.build.publish.plugin.core.util

import groovy.lang.Closure

fun <T : Any> configureGroovy(
    closure: Closure<in T>,
    target: T,
) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = target
    closure.call(target)
}
