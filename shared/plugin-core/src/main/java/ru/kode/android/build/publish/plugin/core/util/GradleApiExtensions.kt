package ru.kode.android.build.publish.plugin.core.util

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.messages.requiredConfigurationNotFoundMessage

const val COMMON_CONTAINER_NAME = "default"

@Suppress("TooGenericExceptionCaught") // Need to catch all exceptions
inline fun <reified T : Any> NamedDomainObjectContainer<T>.getByNameOrRequiredCommon(
    name: String,
    defaultName: String = COMMON_CONTAINER_NAME,
): T {
    return try {
        findByName(name) ?: getByName(defaultName)
    } catch (ex: Throwable) {
        throw GradleException(
            requiredConfigurationNotFoundMessage(name, defaultName),
            ex,
        )
    }
}

inline fun <reified T : Any> NamedDomainObjectContainer<T>.getByNameOrNullableCommon(
    name: String,
    defaultName: String = COMMON_CONTAINER_NAME,
): T? {
    return findByName(name) ?: findByName(defaultName)
}

@Suppress("TooGenericExceptionCaught") // Need to catch all exceptions
inline fun <reified T : Any> BuildPublishDomainObjectContainer<T>.getByNameOrRequiredCommon(
    name: String,
    defaultName: String = COMMON_CONTAINER_NAME,
): T {
    return try {
        findByName(name) ?: getByName(defaultName)
    } catch (ex: Exception) {
        throw GradleException(
            requiredConfigurationNotFoundMessage(name, defaultName),
            ex,
        )
    }
}

inline fun <reified T : Any> BuildPublishDomainObjectContainer<T>.getByNameOrNullableCommon(
    name: String,
    defaultName: String = COMMON_CONTAINER_NAME,
): T? {
    return findByName(name) ?: findByName(defaultName)
}

@Suppress("MaxLineLength")
inline fun <reified T : Any> NamedDomainObjectContainer<T>.getCommon(defaultName: String = COMMON_CONTAINER_NAME): T? {
    return findByName(defaultName)
}

@Suppress("MaxLineLength")
inline fun <reified T : Any> BuildPublishDomainObjectContainer<T>.getCommon(defaultName: String = COMMON_CONTAINER_NAME): T? {
    return findByName(defaultName)
}

@Suppress("MaxLineLength")
@Throws(InvalidUserDataException::class)
fun <T : Any> NamedDomainObjectContainer<T>.common(configurationAction: Action<in T>): NamedDomainObjectProvider<T> {
    return this.register(COMMON_CONTAINER_NAME, configurationAction)
}

@Suppress("MaxLineLength")
@Throws(InvalidUserDataException::class)
fun <T : Any> NamedDomainObjectContainer<T>.common(configurationAction: Closure<in T>): NamedDomainObjectProvider<T> {
    return this.register(
        COMMON_CONTAINER_NAME,
        Action { target ->
            configureGroovy(configurationAction, target)
        },
    )
}

@Throws(InvalidUserDataException::class)
fun <T : Any> NamedDomainObjectContainer<T>.buildVariant(
    buildVariant: String,
    configurationAction: Action<in T>,
): NamedDomainObjectProvider<T> {
    return this.register(buildVariant, configurationAction)
}

@Throws(InvalidUserDataException::class)
fun <T : Any> NamedDomainObjectContainer<T>.buildVariant(
    buildVariant: String,
    configurationAction: Closure<in T>,
): NamedDomainObjectProvider<T> {
    return this.register(
        buildVariant,
        Action { target ->
            configureGroovy(configurationAction, target)
        },
    )
}

inline fun <reified T : Any> Provider<Map<String, Provider<T>>>.flatMapByNameOrCommon(
    name: String,
    defaultName: String = COMMON_CONTAINER_NAME,
): Provider<T> {
    return this.flatMap { providers ->
        providers[name]
            ?: providers[defaultName]
            ?: throw GradleException(
                requiredConfigurationNotFoundMessage(name, defaultName),
                IllegalStateException("Provider map keys: ${providers.keys}"),
            )
    }
}

inline fun <reified T : Any> Map<String, Provider<T>>.getByNameOrCommon(
    name: String,
    defaultName: String = COMMON_CONTAINER_NAME,
): Provider<T> {
    return this[name]
        ?: this[defaultName]
        ?: throw GradleException(
            requiredConfigurationNotFoundMessage(name, defaultName),
            IllegalStateException("Provider map keys: ${this.keys}"),
        )
}

fun Project.serviceName(
    serviceName: String,
    postfix: String? = null,
): String {
    return if (postfix == null) {
        "${serviceName}_$name"
    } else {
        "${serviceName}_${name}_$postfix"
    }
}
