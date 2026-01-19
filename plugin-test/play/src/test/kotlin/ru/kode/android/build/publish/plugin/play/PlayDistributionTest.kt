package ru.kode.android.build.publish.plugin.play

import org.gradle.api.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import java.io.File
import java.io.IOException

@Disabled // It is disabled because it is not implemented yet
class PlayDistributionTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `play build distribution available with distribution`() {
        // TODO: Add real test implementation
    }
}
