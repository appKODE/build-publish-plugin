package ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer
import okio.source
import java.io.File

// NOTE_CHUNKS_UPLOAD_LOOP
// range - is represented byte chunk
// Uploading scheme:
// *** - file
// --- - current range
// -----**************
// *****-----*********
// **********-----****
// etc.
internal class ChunkRequestBody(
    private val file: File,
    private val range: LongRange,
    private val contentType: String = "application/octet-stream",
) : RequestBody() {
    private val contentLength: Long by lazy {
        (range.last - range.first).coerceAtMost(file.length() - range.first)
    }

    override fun contentLength(): Long = contentLength

    override fun contentType(): MediaType? = contentType.toMediaTypeOrNull()

    override fun writeTo(sink: BufferedSink) {
        file.source().buffer().use { source ->
            source.skip(range.first)
            var toRead = contentLength
            while (toRead > 0) {
                val read = source.read(sink.buffer, toRead)
                if (read >= 0) toRead -= read else break
            }
        }
    }
}
