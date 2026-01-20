package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class JiraAuthsConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named {
        internal val auths: NamedDomainObjectContainer<JiraAuthItemConfig> =
            objects.domainObjectContainer(JiraAuthItemConfig::class.java)

        fun auth(
            authName: String,
            action: Action<JiraAuthItemConfig>,
        ) {
            auths.register(authName, action)
        }
    }
