package ru.kode.android.build.publish.plugin.core.util

import ru.kode.android.build.publish.plugin.core.entity.BuildVariant
import ru.kode.android.gradle.commons.util.capitalized

fun BuildVariant.capitalizedName(): String {
    return this.name.capitalized()
}
