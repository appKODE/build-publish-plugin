package ru.kode.android.build.publish.plugin.jira.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * A single Jira **instance**: a base URL + credentials, plus the shared registry of [projects] that
 * live on it.
 *
 * Declared via `auth { … { instance("name") { … } } }`. Projects nest under their instance (mirroring
 * how a Telegram bot owns its chats), so an instance is implicit from where a project is declared and
 * consumers reference projects by name (`targetInstance`/`fromInstance`) without repeating an
 * `instanceName`.
 *
 * @see BasicAuthCredentials For the credential options
 * @see JiraProjectDef For the per-project registry entry
 */
abstract class JiraInstanceConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named,
        CommonConfigMergeable<JiraInstanceConfig> {
        @get:Input
        abstract val baseUrl: Property<String>

        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)

        /**
         * Internal registry of projects that live on this instance. Add projects via [project].
         */
        internal val projects: NamedDomainObjectContainer<JiraProjectDef> =
            objects.domainObjectContainer(JiraProjectDef::class.java)

        /**
         * Registers a Jira project on this instance.
         *
         * @param name A unique-per-instance identifier (e.g. "app"), referenced by consumers via
         *             `projectNames(...)`.
         * @param action Configuration applied to the new [JiraProjectDef].
         */
        fun project(
            name: String,
            action: Action<JiraProjectDef>,
        ) {
            projects.register(name, action)
        }

        /**
         * Registers a Jira project on this instance using a Groovy closure.
         *
         * @param name A unique-per-instance identifier (e.g. "app").
         * @param configurationClosure The Groovy closure applied to the new [JiraProjectDef].
         *
         * @see project
         */
        fun project(
            name: String,
            @DelegatesTo(value = JiraProjectDef::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in JiraProjectDef>,
        ) {
            project(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: JiraInstanceConfig) {
            baseUrl.convention(common.baseUrl)
            credentials.inheritFrom(common.credentials)
            projects.inheritNamedFrom(common.projects)
        }
    }
