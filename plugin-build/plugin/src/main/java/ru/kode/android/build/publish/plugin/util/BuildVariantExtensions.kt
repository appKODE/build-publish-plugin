package ru.kode.android.build.publish.plugin.util

import ru.kode.android.build.publish.plugin.enity.BuildVariant

internal fun BuildVariant.capitalizedName(): String {
    return this.name.capitalized()
}
