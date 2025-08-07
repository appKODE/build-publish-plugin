package ru.kode.android.build.publish.plugin.foundation.service

import org.ajoberstar.grgit.gradle.GrgitService
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.command.GitCommandExecutor
import ru.kode.android.build.publish.plugin.core.git.ChangelogBuilder
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
        protected abstract val changelogBuilderProperty: Property<ChangelogBuilder>

        init {
            executorProperty.set(
                parameters.grgitService.map { grGitService ->
                    GitCommandExecutor(grGitService.grgit)
                },
            )
            repositoryProperty.set(executorProperty!!.map { GitRepository(it) })
            changelogBuilderProperty.set(repositoryProperty!!.map { ChangelogBuilder(it, logger) })
        }

        val commandExecutor: GitCommandExecutor
            get() = executorProperty.get()

        val repository: GitRepository
            get() = repositoryProperty.get()

        val changelogBuilder: ChangelogBuilder
            get() = changelogBuilderProperty.get()

        companion object {
            private val logger: Logger = Logging.getLogger(GitExecutorService::class.java)
        }
    }
