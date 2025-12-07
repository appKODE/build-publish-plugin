package ru.kode.android.build.publish.plugin.test.utils

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class AlwaysInfoLogger : Logger by Logging.getLogger("JiraTest") {
    override fun info(message: String?) {
        println("[INFO] $message")
    }
}