package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.PolymorphicDomainObjectContainer

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

@Throws(InvalidUserDataException::class)
fun <T> PolymorphicDomainObjectContainer<T>.createDefault(
    configurationAction: Action<in T>
): NamedDomainObjectProvider<T> {
    return this.register(DEFAULT_CONTAINER_NAME, configurationAction)
}

@Throws(InvalidUserDataException::class)
fun <T> NamedDomainObjectContainer<T>.createDefault(
    configurationAction: Action<in T>
): NamedDomainObjectProvider<T> {
    return this.register(DEFAULT_CONTAINER_NAME, configurationAction)
}
