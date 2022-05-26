package ru.kode.android.build.publish.plugin.util

import java.util.Locale
import kotlin.text.replaceFirstChar

internal fun String.capitalized(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }
