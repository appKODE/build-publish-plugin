package ru.kode.android.build.publish.plugin.extension

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.extension.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.ChangelogSettingsConfig
import ru.kode.android.build.publish.plugin.extension.config.FirebaseAppDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.SlackConfig
import ru.kode.android.build.publish.plugin.extension.config.TelegramConfig
import javax.inject.Inject

const val EXTENSION_NAME = "buildPublish"

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishExtension @Inject constructor(objectFactory: ObjectFactory) {
    val changelog: NamedDomainObjectContainer<ChangelogSettingsConfig> =
        objectFactory.domainObjectContainer(ChangelogSettingsConfig::class.java)
    val telegram: NamedDomainObjectContainer<TelegramConfig> =
        objectFactory.domainObjectContainer(TelegramConfig::class.java)
    val slack: NamedDomainObjectContainer<SlackConfig> =
        objectFactory.domainObjectContainer(SlackConfig::class.java)
    val firebaseDistribution: NamedDomainObjectContainer<FirebaseAppDistributionConfig> =
        objectFactory.domainObjectContainer(FirebaseAppDistributionConfig::class.java)
    val appCenterDistribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
        objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)
}
