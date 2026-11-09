package ru.kode.android.build.publish.plugin.yandexdisk.controller

import ru.kode.android.build.publish.plugin.yandexdisk.controller.entity.YandexDiskShare
import java.io.File

interface YandexDiskController {
    /**
     * Uploads [file] to `disk:/<remotePath>/<remoteFileName>`, creating missing folders
     * and overwriting an existing file with the same name.
     */
    fun uploadFile(
        remotePath: String,
        remoteFileName: String,
        file: File,
    )

    /**
     * Publishes the uploaded file and returns its public link, reusing an existing one.
     */
    fun publishFile(
        remotePath: String,
        remoteFileName: String,
    ): YandexDiskShare

    fun resolveRemoteFilePath(
        remotePath: String,
        fileName: String,
    ): String
}
