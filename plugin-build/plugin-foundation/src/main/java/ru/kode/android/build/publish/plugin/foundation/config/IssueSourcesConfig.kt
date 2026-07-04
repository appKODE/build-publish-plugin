package ru.kode.android.build.publish.plugin.foundation.config

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
 * Container of issue-tracker sources used when processing the changelog.
 *
 * Declared inside `changelog { … { issueSources { … } } }`. Each source is added via the [issueSource]
 * method and pairs a regex that recognizes issue keys with the URL those keys link to. Declaring
 * several sources lets one changelog reference issues from different projects and/or different
 * issue-tracker hosts — extraction (Jira/ClickUp) unions every source's `numberPattern`, and link
 * formatting (Slack/Telegram) resolves each matched key against its own source's `urlPrefix`.
 *
 * Example:
 * ```
 * issueSources {
 *     issueSource("base") { numberPattern.set("BASE-\\d+"); urlPrefix.set("https://jira1/browse/") }
 *     issueSource("leg")  { numberPattern.set("LEG-\\d+");  urlPrefix.set("https://jira2/browse/") }
 * }
 * ```
 *
 * @see IssueSourceConfig
 */
abstract class IssueSourcesConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : CommonConfigMergeable<IssueSourcesConfig> {
        /**
         * Internal container of the configured issue sources.
         *
         * This property holds all sources added via the [issueSource] method. Access to this container
         * is restricted to the plugin implementation.
         */
        internal val sources: NamedDomainObjectContainer<IssueSourceConfig> =
            objects.domainObjectContainer(IssueSourceConfig::class.java)

        /**
         * Configures a new issue source with the given name and settings.
         *
         * This method registers a new [IssueSourceConfig] and applies the provided configuration action
         * to it. Each source must have a unique name within the changelog configuration.
         *
         * @param name A unique identifier for the source (e.g. "base", "legacy"). Used only to
         *             distinguish sources within the build script.
         * @param action A configuration block applied to the new [IssueSourceConfig] instance.
         *
         * @see IssueSourceConfig For available per-source options
         */
        fun issueSource(
            name: String,
            action: Action<IssueSourceConfig>,
        ) {
            sources.register(name, action)
        }

        /**
         * Configures a new issue source with the given name using a Groovy closure.
         *
         * @param name A unique identifier for the source (e.g. "base").
         * @param configurationClosure The Groovy closure applied to the new [IssueSourceConfig].
         *
         * @see issueSource
         */
        fun issueSource(
            name: String,
            @DelegatesTo(value = IssueSourceConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in IssueSourceConfig>,
        ) {
            issueSource(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: IssueSourcesConfig) {
            sources.inheritNamedFrom(common.sources)
        }
    }
