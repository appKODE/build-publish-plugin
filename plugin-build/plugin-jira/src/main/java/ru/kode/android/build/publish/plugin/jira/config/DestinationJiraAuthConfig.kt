package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class DestinationJiraAuthConfig
    @Inject
    constructor() {
        @get:Input
        abstract val authName: Property<String>

        @get:Input
        internal abstract val projectKeys: SetProperty<String>

        fun projectKey(projectKey: String) {
            projectKeys.add(projectKey)
        }

        fun projectKey(projectKey: Provider<String>) {
            projectKeys.add(projectKey)
        }

        fun projectKeys(vararg projectKey: String) {
            projectKeys.addAll(projectKey.toList())
        }

        fun projectKeys(projectKeys: Iterable<String>) {
            this.projectKeys.addAll(projectKeys.toList())
        }

        fun projectKeys(projectKeys: Provider<List<String>>) {
            this.projectKeys.addAll(projectKeys)
        }
    }
