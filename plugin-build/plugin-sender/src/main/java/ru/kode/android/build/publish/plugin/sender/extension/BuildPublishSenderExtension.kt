package ru.kode.android.build.publish.plugin.sender.extension

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import javax.inject.Inject

abstract class BuildPublishSenderExtension
    @Inject
    constructor(private val objects: ObjectFactory) {
        internal var slackConfig: SenderSlackConfig? = null
        internal var telegramConfig: SenderTelegramConfig? = null
        internal var nextcloudConfig: SenderNextcloudConfig? = null
        internal var jiraConfig: SenderJiraConfig? = null
        internal var confluenceConfig: SenderConfluenceConfig? = null
        internal var clickUpConfig: SenderClickUpConfig? = null

        fun slack(action: Action<SenderSlackConfig>) {
            val config = objects.newInstance(SenderSlackConfig::class.java)
            action.execute(config)
            slackConfig = config
        }

        fun slack(
            @DelegatesTo(value = SenderSlackConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in SenderSlackConfig>,
        ) {
            slack { target -> configureGroovy(configurationClosure, target) }
        }

        fun telegram(action: Action<SenderTelegramConfig>) {
            val config = objects.newInstance(SenderTelegramConfig::class.java)
            action.execute(config)
            telegramConfig = config
        }

        fun telegram(
            @DelegatesTo(value = SenderTelegramConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in SenderTelegramConfig>,
        ) {
            telegram { target -> configureGroovy(configurationClosure, target) }
        }

        fun nextcloud(action: Action<SenderNextcloudConfig>) {
            val config = objects.newInstance(SenderNextcloudConfig::class.java)
            action.execute(config)
            nextcloudConfig = config
        }

        fun nextcloud(
            @DelegatesTo(value = SenderNextcloudConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in SenderNextcloudConfig>,
        ) {
            nextcloud { target -> configureGroovy(configurationClosure, target) }
        }

        fun jira(action: Action<SenderJiraConfig>) {
            val config = objects.newInstance(SenderJiraConfig::class.java)
            action.execute(config)
            jiraConfig = config
        }

        fun jira(
            @DelegatesTo(value = SenderJiraConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in SenderJiraConfig>,
        ) {
            jira { target -> configureGroovy(configurationClosure, target) }
        }

        fun confluence(action: Action<SenderConfluenceConfig>) {
            val config = objects.newInstance(SenderConfluenceConfig::class.java)
            action.execute(config)
            confluenceConfig = config
        }

        fun confluence(
            @DelegatesTo(value = SenderConfluenceConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in SenderConfluenceConfig>,
        ) {
            confluence { target -> configureGroovy(configurationClosure, target) }
        }

        fun clickUp(action: Action<SenderClickUpConfig>) {
            val config = objects.newInstance(SenderClickUpConfig::class.java)
            action.execute(config)
            clickUpConfig = config
        }

        fun clickUp(
            @DelegatesTo(value = SenderClickUpConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in SenderClickUpConfig>,
        ) {
            clickUp { target -> configureGroovy(configurationClosure, target) }
        }
    }

abstract class SenderSlackConfig {
    abstract val webhookUrl: Property<String>
    abstract val uploadApiTokenFile: RegularFileProperty
}

abstract class SenderTelegramConfig {
    abstract val botId: Property<String>
    abstract val chatId: Property<String>
    abstract val topicId: Property<String>
    abstract val serverBaseUrl: Property<String>
}

abstract class SenderNextcloudConfig {
    abstract val baseUrl: Property<String>
    abstract val username: Property<String>
    abstract val password: Property<String>
}

abstract class SenderJiraConfig {
    abstract val baseUrl: Property<String>
    abstract val username: Property<String>
    abstract val apiToken: Property<String>
}

abstract class SenderConfluenceConfig {
    abstract val baseUrl: Property<String>
    abstract val username: Property<String>
    abstract val apiToken: Property<String>
}

abstract class SenderClickUpConfig {
    abstract val apiTokenFile: RegularFileProperty
}
