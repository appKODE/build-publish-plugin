package ru.kode.android.build.publish.plugin.core.zip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileExtensionsTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should keep source type in archive name so apk and bundle differ`() {
        val dir = tempDir.resolve("outputs.apk").apply { mkdirs() }
        val apk = dir.resolve("app-release-vc33.apk").apply { writeText("apk") }
        val bundle = dir.resolve("app-release-vc33.aab").apply { writeText("aab") }

        assertEquals(dir.resolve("app-release-vc33-apk.zip"), apk.zipped())
        assertEquals(dir.resolve("app-release-vc33-aab.zip"), bundle.zipped())
    }
}
