package ru.kode.android.build.publish.plugin.util

import ru.kode.android.build.publish.plugin.enity.BuildVariant
import java.util.Locale
import kotlin.text.replaceFirstChar

internal fun BuildVariant.capitalized(): String = name.replaceFirstChar { it.titlecase(Locale.ROOT) }
