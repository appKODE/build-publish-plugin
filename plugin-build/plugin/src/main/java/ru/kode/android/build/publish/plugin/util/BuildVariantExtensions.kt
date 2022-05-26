package ru.kode.android.build.publish.plugin.util

import ru.kode.android.build.publish.plugin.enity.BuildVariant

internal fun BuildVariant.concatenated(): String {
    return buildString {
        flavorNames.forEachIndexed { index, flavor ->
            append(if (index == 0) flavor else flavor.capitalized())
        }
        append(if (isEmpty()) buildTypeName else buildTypeName.capitalized())
    }
}
