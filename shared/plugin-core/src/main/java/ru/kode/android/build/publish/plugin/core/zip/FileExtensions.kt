package ru.kode.android.build.publish.plugin.core.zip

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("NestedBlockDepth", "MagicNumber") // simple zip logic
fun List<File>.zipFiles(zipFile: File): File {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
        val data = ByteArray(1024)
        for (file in this) {
            FileInputStream(file).use { fi ->
                BufferedInputStream(fi).use { origin ->
                    val entry = ZipEntry(file.name)
                    out.putNextEntry(entry)
                    while (true) {
                        val readBytes = origin.read(data)
                        if (readBytes == -1) {
                            break
                        }
                        out.write(data, 0, readBytes)
                    }
                }
            }
        }
    }
    return zipFile
}
