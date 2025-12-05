package ru.kode.android.build.publish.plugin.telegram.service

import org.gradle.api.provider.Provider

/**
 * Extension class that provides access to Telegram network services.
 *
 * This class is used internally by the plugin to manage and provide access to
 * configured Telegram network services. It acts as a bridge between the plugin's
 * configuration and the actual network service implementations.
 *
 * @see TelegramService For the actual network service implementation
 * @see BuildPublishTelegramExtension For the main plugin extension that uses this class
 */
abstract class TelegramServiceExtension(
    /**
     * A provider of a map containing Telegram network services,
     * where the key is the bot name and the value is a provider
     * of the corresponding network service.
     */
    val services: Provider<Map<String, Provider<TelegramService>>>,
)
