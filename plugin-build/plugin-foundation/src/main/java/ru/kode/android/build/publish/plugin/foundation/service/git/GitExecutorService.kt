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

abstract class GitExecutorService
    @Inject
    constructor() : BuildService<GitExecutorService.Params> {
        interface Params : BuildServiceParameters {
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

        val commandExecutor: GitCommandExecutor
            get() = executorProperty.get()

        val repository: GitRepository
            get() = repositoryProperty.get()

        val gitChangelogBuilder: GitChangelogBuilder
            get() = gitChangelogBuilderProperty.get()

        companion object {
            private val logger: Logger = Logging.getLogger(GitExecutorService::class.java)
        }
    }
