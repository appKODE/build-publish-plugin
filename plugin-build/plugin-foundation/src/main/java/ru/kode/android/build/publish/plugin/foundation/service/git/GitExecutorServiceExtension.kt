package ru.kode.android.build.publish.plugin.foundation.service.git

import org.gradle.api.provider.Provider

/**
 * Extension class that provides access to a [GitExecutorService] instance.
 *
 * This class serves as a Gradle extension point for configuring and accessing
 * the Git executor service throughout the build process. It wraps a provider
 * for lazy initialization of the Git executor service.
 *
 * @see GitExecutorService
 * @see Provider
 */
abstract class GitExecutorServiceExtension(
    /**
     * A provider for the [GitExecutorService] instance.
     * This allows for lazy initialization and proper Gradle configuration caching.
     */
    val executorService: Provider<GitExecutorService>,
)
