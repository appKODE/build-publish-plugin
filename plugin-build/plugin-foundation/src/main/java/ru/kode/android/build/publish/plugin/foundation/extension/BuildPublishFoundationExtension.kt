package ru.kode.android.build.publish.plugin.foundation.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.foundation.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.foundation.config.OutputConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishFoundationExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val output: NamedDomainObjectContainer<OutputConfig> =
            objectFactory.domainObjectContainer(OutputConfig::class.java)

        internal val changelog: NamedDomainObjectContainer<ChangelogConfig> =
            objectFactory.domainObjectContainer(ChangelogConfig::class.java)

        val outputConfig: (buildName: String) -> OutputConfig = { buildName ->
            output.getByNameOrRequiredCommon(buildName)
        }

        val outputConfigOrNull: (buildName: String) -> OutputConfig? = { buildName ->
            output.getByNameOrNullableCommon(buildName)
        }

        val changelogConfig: (buildName: String) -> ChangelogConfig = { buildName ->
            changelog.getByNameOrRequiredCommon(buildName)
        }

        val changelogConfigOrNull: (buildName: String) -> ChangelogConfig? = { buildName ->
            changelog.getByNameOrNullableCommon(buildName)
        }

        fun output(configurationAction: Action<BaseDomainContainer<OutputConfig>>) {
            val container = BaseDomainContainer(output)
            configurationAction.execute(container)
        }

        fun changelog(configurationAction: Action<BaseDomainContainer<ChangelogConfig>>) {
            val container = BaseDomainContainer(changelog)
            configurationAction.execute(container)
        }

        fun outputCommon(configurationAction: Action<OutputConfig>) {
            common(output, configurationAction)
        }

        fun changelogCommon(configurationAction: Action<ChangelogConfig>) {
            common(changelog, configurationAction)
        }
    }
