package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer

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
            "Required `$name` or `$defaultName` configuration not found. " +
                "This can happen if the configuration was not registered or if the name is incorrect. " +
                "Make sure that there is no errors in the build script. " +
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
            "Required `$name` or `$defaultName` configuration not found. " +
                "This can happen if the configuration was not registered or if the name is incorrect. " +
                "Make sure that there is no errors in the build script. " +
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

inline fun <reified T : Any> NamedDomainObjectContainer<T>.getCommon(defaultName: String = COMMON_CONTAINER_NAME): T? {
    return findByName(defaultName)
}

inline fun <reified T : Any> BuildPublishDomainObjectContainer<T>.getCommon(defaultName: String = COMMON_CONTAINER_NAME): T? {
    return findByName(defaultName)
}

@Throws(InvalidUserDataException::class)
fun <T : Any> NamedDomainObjectContainer<T>.common(configurationAction: Action<in T>): NamedDomainObjectProvider<T> {
    return this.register(COMMON_CONTAINER_NAME, configurationAction)
}

@Throws(InvalidUserDataException::class)
fun <T : Any> NamedDomainObjectContainer<T>.buildVariant(
    buildVariant: String,
    configurationAction: Action<in T>,
): NamedDomainObjectProvider<T> {
    return this.register(buildVariant, configurationAction)
}

inline fun <reified T : Any> Provider<Map<String, Provider<T>>>.flatMapByNameOrCommon(
    name: String,
    defaultName: String = COMMON_CONTAINER_NAME,
): Provider<T> {
    return this.flatMap { providers ->
        providers[name]
            ?: providers[defaultName]
            ?: throw GradleException(
                "Required `$name` or `$defaultName` configuration not found. " +
                    "This can happen if the configuration was not registered or if the name is incorrect. " +
                    "Make sure that there is no errors in the build script. ",
                IllegalStateException("Provider map keys: ${providers.keys}")
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
            "Required `$name` or `$defaultName` configuration not found. " +
                "This can happen if the configuration was not registered or if the name is incorrect. " +
                "Make sure that there is no errors in the build script. ",
            IllegalStateException("Provider map keys: ${this.keys}")
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
