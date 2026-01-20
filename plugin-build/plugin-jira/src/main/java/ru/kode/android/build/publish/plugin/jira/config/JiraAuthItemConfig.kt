package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import javax.inject.Inject

abstract class JiraAuthItemConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named {
        @get:Input
        abstract val baseUrl: Property<String>

        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)

        @get:Input
        abstract val projectKeys: SetProperty<String>

        fun credentials(action: Action<BasicAuthCredentials>) {
            action.execute(credentials)
        }

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
