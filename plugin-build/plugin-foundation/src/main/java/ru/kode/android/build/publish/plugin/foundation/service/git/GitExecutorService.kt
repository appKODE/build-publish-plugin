package ru.kode.android.build.publish.plugin.foundation.service.git

import org.ajoberstar.grgit.gradle.GrgitService
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.git.GitChangelogBuilder
import ru.kode.android.build.publish.plugin.core.git.GitCommandExecutor
import ru.kode.android.build.publish.plugin.core.git.GitRepository
import javax.inject.Inject

private val logger: Logger = Logging.getLogger(GitExecutorService::class.java)

/**
 * A Gradle build service that provides Git repository operations.
 *
 * This service acts as a central point for Git operations in the build process,
 * providing access to Git commands, repository information, and changelog generation.
 * It's implemented as a Gradle [BuildService] to ensure proper resource management
 * and lifecycle handling.
 *
 * @see BuildService
 * @see GitCommandExecutor
 * @see GitRepository
 * @see GitChangelogBuilder
 */
abstract class GitExecutorService
    @Inject
    constructor() : BuildService<GitExecutorService.Params> {
        /**
         * Configuration parameters for the Git executor service.
         */
        interface Params : BuildServiceParameters {
            /**
             * The underlying Grgit service used for Git operations
             */
            val grgitService: Property<GrgitService>
        }

        protected abstract val executorProperty: Property<GitCommandExecutor>

        protected abstract val repositoryProperty: Property<GitRepository>

        protected abstract val gitChangelogBuilderProperty: Property<GitChangelogBuilder>

        init {
            executorProperty.set(
                parameters.grgitService.map { grGitService ->
                    GitCommandExecutor(grGitService.grgit)
                },
            )

            repositoryProperty.set(executorProperty!!.map { GitRepository(it) })

            gitChangelogBuilderProperty.set(repositoryProperty!!.map { GitChangelogBuilder(it, logger) })
        }

        /**
         * Provides access to the Git command executor.
         *
         * This property allows executing low-level Git commands.
         *
         * @see GitCommandExecutor
         */
        val commandExecutor: GitCommandExecutor
            get() = executorProperty.get()

        /**
         * Provides access to the Git repository.
         *
         * This property provides repository-level operations such as
         * querying commit history, tags, and branches.
         *
         * @see GitRepository
         */
        val repository: GitRepository
            get() = repositoryProperty.get()

        /**
         * Provides access to the Git changelog builder.
         *
         * This property is used to generate changelogs from Git history.
         *
         * @see GitChangelogBuilder
         */
        val gitChangelogBuilder: GitChangelogBuilder
            get() = gitChangelogBuilderProperty.get()
    }
