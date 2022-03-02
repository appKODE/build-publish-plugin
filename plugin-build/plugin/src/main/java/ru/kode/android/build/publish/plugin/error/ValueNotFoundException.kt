package ru.kode.android.build.publish.plugin.error

import org.gradle.api.GradleException

class ValueNotFoundException(value: String) : GradleException(
    "value [$value] not found and should be provided for configuration"
)
