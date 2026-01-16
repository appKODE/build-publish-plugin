package ru.kode.android.build.publish.plugin.core.util

import java.util.Locale

fun String.capitalized(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }

fun String.ellipsizeAt(size: Int): String {
    return if (this.length <= size) {
        this
    } else {
        this
            .take(size - 1)
            .plus(Typography.ellipsis)
    }
}

fun String.mask(
    maskChar: Char = '*',
    visibleChars: Int = 4,
): String {
    val visibleLength = visibleChars.coerceAtMost(this.length / 2)
    val start = this.take(visibleLength)
    val end = if (this.length > visibleLength * 2) this.takeLast(visibleLength) else ""
    return start + maskChar.toString().repeat(4) + end
}

fun String.replaceLast(
    oldValue: String,
    newValue: String,
    ignoreCase: Boolean = false,
): String {
    val index = lastIndexOf(oldValue, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + oldValue.length, newValue)
}
