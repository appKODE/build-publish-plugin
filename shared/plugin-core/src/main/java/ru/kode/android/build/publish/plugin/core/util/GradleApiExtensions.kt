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
    strategy: MergeStrategy = MergeStrategy.MERGE,
    configurationAction: Action<in T>,
): NamedDomainObjectProvider<T> {
    return this.register(buildVariant) { target ->
        configurationAction.execute(target)
        applyCommonFallbackIfNeeded(target, strategy)
    }
}

@Throws(InvalidUserDataException::class)
fun <T : Any> NamedDomainObjectContainer<T>.buildVariant(
    buildVariant: String,
    strategy: MergeStrategy = MergeStrategy.MERGE,
    configurationAction: Closure<in T>,
): NamedDomainObjectProvider<T> {
    return this.register(buildVariant) { target ->
        configureGroovy(configurationAction, target)
        applyCommonFallbackIfNeeded(target, strategy)
    }
}

/**
 * Applies the common ("default") configuration as a lazy fallback onto a freshly configured
 * per-version-name [target], unless [strategy] is [MergeStrategy.REPLACE]. Runs during element
 * realization, so it sees the fully evaluated `common` block regardless of DSL declaration order.
 */
private fun <T : Any> NamedDomainObjectContainer<T>.applyCommonFallbackIfNeeded(
    target: T,
    strategy: MergeStrategy,
) {
    if (strategy == MergeStrategy.MERGE) {
        findByName(COMMON_CONTAINER_NAME)?.let { common -> applyCommonFallback(target, common) }
    }
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

/**
 * Resolves the service backing a plugin's standalone tasks: the common ("default") instance when it
 * exists, otherwise the first registered instance. Centralizes the single unavoidable unchecked cast
 * out of the type-erased `Provider<*>` service map so each plugin no longer repeats it (and no longer
 * mismatches the common key — the default instance is registered under [COMMON_CONTAINER_NAME]).
 */
@Suppress("MaxLineLength")
inline fun <reified T : Any> Map<String, Provider<*>>.resolveStandaloneService(defaultName: String = COMMON_CONTAINER_NAME): Provider<T> {
    val provider =
        this[defaultName]
            ?: values.firstOrNull()
            ?: throw GradleException("No standalone service registered (available keys: $keys)")
    @Suppress("UNCHECKED_CAST")
    return provider as Provider<T>
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
