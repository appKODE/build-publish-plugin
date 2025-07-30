package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.NamedDomainObjectContainer

const val DEFAULT_CONTAINER_NAME = "default"

inline fun <reified T> NamedDomainObjectContainer<T>.getByNameOrRequiredDefault(
    name: String,
    defaultName: String = DEFAULT_CONTAINER_NAME
): T {
    return findByName(name) ?: getByName(defaultName)
}
inline fun <reified T> NamedDomainObjectContainer<T>.getByNameOrNullableDefault(
    name: String,
    defaultName: String = DEFAULT_CONTAINER_NAME
): T? {
    return findByName(name) ?: findByName(defaultName)
}

inline fun <reified T> NamedDomainObjectContainer<T>.getDefault(
    defaultName: String = DEFAULT_CONTAINER_NAME
): T? {
    return findByName(defaultName)
}
