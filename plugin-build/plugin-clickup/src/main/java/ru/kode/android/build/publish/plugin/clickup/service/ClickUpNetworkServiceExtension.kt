package ru.kode.android.build.publish.plugin.clickup.service

import org.gradle.api.provider.Provider

abstract class ClickUpNetworkServiceExtension(
    val services: Provider<Map<String, Provider<ClickUpNetworkService>>>
)
