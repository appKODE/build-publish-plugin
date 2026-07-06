package ru.kode.android.build.publish.plugin.core.util

import ru.kode.android.build.publish.plugin.core.entity.BuildVariant

fun BuildVariant.capitalizedName(): String {
    return this.name.capitalized()
}
