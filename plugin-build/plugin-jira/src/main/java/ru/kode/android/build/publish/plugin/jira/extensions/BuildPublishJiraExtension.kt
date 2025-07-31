package ru.kode.android.build.publish.plugin.jira.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.jira.core.JiraConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishJiraExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val jira: NamedDomainObjectContainer<JiraConfig> =
            objectFactory.domainObjectContainer(JiraConfig::class.java)
    }
