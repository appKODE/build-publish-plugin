package ru.kode.android.build.publish.plugin.core.util

import ru.kode.android.build.publish.plugin.core.enity.BuildVariant

fun BuildVariant.capitalizedName(): String {
    return this.name.capitalized()
}
