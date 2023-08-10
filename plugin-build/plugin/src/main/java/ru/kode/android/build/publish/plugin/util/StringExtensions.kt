package ru.kode.android.build.publish.plugin.util

import java.util.Locale

internal fun String.capitalized(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }

fun String.ellipsizeAt(size: Int): String {
    return if (this.length <= size) this
    else this
        .take(size - 1)
        .plus(Typography.ellipsis)
}
