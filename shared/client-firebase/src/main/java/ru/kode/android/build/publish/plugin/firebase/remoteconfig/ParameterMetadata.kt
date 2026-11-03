package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import java.time.LocalDate

private val METADATA_ENTRY = Regex("""\b(owner|type|created|expires)=(\S+)""")

/**
 * Lifecycle metadata written into a parameter description, e.g.
 * `Checkout redesign. owner=@ivan type=release created=2026-07-01 expires=2026-12-01`.
 * Firebase keeps no creation date per parameter, so this is the only source of a flag's age.
 */
data class ParameterMetadata(
    val owner: String? = null,
    val type: String? = null,
    val created: LocalDate? = null,
    val expires: LocalDate? = null,
)

/**
 * Parses `owner=`, `type=`, `created=` and `expires=` (ISO dates) from [description]; invalid dates are ignored.
 */
fun parseMetadata(description: String?): ParameterMetadata {
    val entries = METADATA_ENTRY.findAll(description.orEmpty()).associate { it.groupValues[1] to it.groupValues[2] }
    return ParameterMetadata(
        owner = entries["owner"],
        type = entries["type"],
        created = entries["created"]?.toDateOrNull(),
        expires = entries["expires"]?.toDateOrNull(),
    )
}

private fun String.toDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
