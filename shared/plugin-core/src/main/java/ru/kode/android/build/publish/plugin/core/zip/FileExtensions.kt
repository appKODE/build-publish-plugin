package ru.kode.android.build.publish.plugin.core.zip

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("NestedBlockDepth", "MagicNumber") // simple zip logic
fun List<File>.zipAllInto(zipFile: File): File {
    require(isNotEmpty()) { "File list cannot be empty." }

    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        this.forEach { file ->
            if (!file.isFile) return@forEach

            FileInputStream(file).buffered().use { input ->
                val entry = ZipEntry(file.name)
                zipOut.putNextEntry(entry)
                input.copyTo(zipOut, buffer.size)
                zipOut.closeEntry()
            }
        }
    }

    return zipFile
}
