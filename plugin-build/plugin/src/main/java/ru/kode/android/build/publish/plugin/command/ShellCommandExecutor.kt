package ru.kode.android.build.publish.plugin.command

import org.gradle.api.logging.Logging
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream

interface ShellCommandExecutor {
    /**
     * Sends JSON in [jsonBody] to a web-hook at [webHookUrl]
     */
    fun sendToWebHook(webHookUrl: String, jsonBody: String)

    /**
     * Sends url formatted data to web-hook at [webHookUrl]
     */
    fun sendToWebHook(webHookUrl: String)
}

private class WindowsShellCommandExecutor : ShellCommandExecutor {
    override fun sendToWebHook(webHookUrl: String, jsonBody: String) = Unit
    override fun sendToWebHook(webHookUrl: String) = Unit
}

private class LinuxShellCommandExecutor(
    private val execOperations: ExecOperations,
) : ShellCommandExecutor {

    private val logger = Logging.getLogger(this::class.java)

    override fun sendToWebHook(webHookUrl: String, jsonBody: String) {
        executeInShell(
            "curl -i -X POST " +
                "-H 'Content-Type: application/json' " +
                "-d $jsonBody $webHookUrl"
        )
    }

    override fun sendToWebHook(webHookUrl: String) {
        logger.debug("sending changelog to $webHookUrl")
        executeInShell("curl -sS '$webHookUrl'")
    }

    private fun executeInShell(command: String): List<String> {
        val stdout = ByteArrayOutputStream()
        execOperations.exec {
            it.commandLine("bash", "-c", command)
            it.standardOutput = stdout
        }

        val commandResult = stdout.toString("utf-8").trim()
        stdout.close()

        logger.debug("command: $command")
        logger.debug("shell output: $commandResult")
        return if (commandResult.isEmpty()) emptyList() else commandResult.lineSequence().map { it.trim() }.toList()
    }
}

fun getShellCommandExecutor(execOperations: ExecOperations): ShellCommandExecutor {
    return if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
        WindowsShellCommandExecutor()
    } else {
        LinuxShellCommandExecutor(execOperations)
    }
}
