package ru.kode.android.build.publish.plugin.foundation.service.git

import org.gradle.api.provider.Provider

abstract class GitExecutorServiceExtension(
    val executorService: Provider<GitExecutorService>,
)
