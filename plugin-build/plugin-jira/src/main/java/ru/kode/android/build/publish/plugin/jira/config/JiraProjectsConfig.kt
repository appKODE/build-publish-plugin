package ru.kode.android.build.publish.plugin.jira.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * Container of Jira projects targeted by an automation rule.
 *
 * Declared inside `automation { … { projects { … } } }`. Each project is added via the [project]
 * method and points at its own project key and, optionally, its own Jira instance (via `instanceName`)
 * and per-project automation patterns. Changelog issues are routed to a project by their issue-key
 * prefix (e.g. `PROJ-123` -> `PROJ`).
 *
 * Example:
 * ```
 * projects {
 *     project("main")   { projectKey.set("APP") }
 *     project("legacy") { projectKey.set("LEG"); instanceName.set("legacy") }
 * }
 * ```
 *
 * @see JiraProjectConfig
 */
abstract class JiraProjectsConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : CommonConfigMergeable<JiraProjectsConfig> {
        /**
         * Internal container of the configured projects.
         *
         * This property holds all projects added via the [project] method. Access to this container is
         * restricted to the plugin implementation.
         */
        internal val projects: NamedDomainObjectContainer<JiraProjectConfig> =
            objects.domainObjectContainer(JiraProjectConfig::class.java)

        /**
         * Configures a new Jira project with the given name and settings.
         *
         * This method registers a new [JiraProjectConfig] and applies the provided configuration action
         * to it. Each project must have a unique name within the automation rule.
         *
         * @param name A unique identifier for the project (e.g. "main", "legacy"). Used only to
         *             distinguish projects within the build script.
         * @param action A configuration block applied to the new [JiraProjectConfig] instance.
         *
         * @see JiraProjectConfig For available per-project options
         */
        fun project(
            name: String,
            action: Action<JiraProjectConfig>,
        ) {
            projects.register(name, action)
        }

        /**
         * Configures a new Jira project with the given name using a Groovy closure.
         *
         * @param name A unique identifier for the project (e.g. "main").
         * @param configurationClosure The Groovy closure applied to the new [JiraProjectConfig].
         *
         * @see project
         */
        fun project(
            name: String,
            @DelegatesTo(value = JiraProjectConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in JiraProjectConfig>,
        ) {
            project(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: JiraProjectsConfig) {
            projects.inheritNamedFrom(common.projects)
        }
    }
