package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.SetProperty

/**
 * Merge behavior chosen per `buildVariant { }` block.
 *
 * - [MERGE] (default): the per-version-name block overrides only the properties it sets; every
 *   unset scalar, collection, nested object and nested container inherits from the `common` block.
 * - [REPLACE]: legacy all-or-nothing behavior — the variant fully replaces `common` and inherits
 *   nothing. Kept as an explicit opt-out / backward-compat escape hatch.
 */
enum class MergeStrategy { MERGE, REPLACE }

/**
 * Merge behavior chosen per collection at the builder call site, overriding the variant-level
 * default within [MergeStrategy.MERGE].
 *
 * - [REPLACE] (default): replace-when-set — if the variant sets the collection it wins; if the
 *   variant leaves it untouched it inherits `common`'s value.
 * - [APPEND]: union — the variant's items are added on top of `common`'s.
 */
enum class CollectionStrategy { REPLACE, APPEND }

/**
 * Implemented by every per-version-name config so it can inherit unset values from the matching
 * `common` config. This is explicit and compiler-checked — each config wires its own properties
 * (mirroring how the Android Gradle Plugin merges `defaultConfig` into product flavors), so there
 * is no reflection and no silent skipping of unknown property shapes.
 *
 * Implementations use [org.gradle.api.provider.Property.convention] for scalars (an explicit
 * `set()` wins, otherwise the common value applies), [inheritFrom] for `SetProperty` collections,
 * and [inheritNamedFrom] for nested [NamedDomainObjectContainer]s.
 */
interface CommonConfigMergeable<T> {
    /**
     * Applies [common] as a lazy fallback onto this config: only properties the variant explicitly
     * set are kept, everything else resolves to [common].
     */
    fun inheritFrom(common: T)
}

/**
 * Inherits a `SetProperty` collection from [common], honoring the [strategy] the owning config chose
 * for this collection at its builder call site (default [CollectionStrategy.REPLACE]):
 *
 * - [CollectionStrategy.REPLACE]: `convention(common)` — the variant's items win if it set any,
 *   otherwise it inherits `common`'s items.
 * - [CollectionStrategy.APPEND]: `addAll(common)` — the variant's items are unioned with `common`'s.
 *
 * The strategy is passed in explicitly (stored on the config next to the collection) rather than via
 * shared state, so each config wires its own merge behavior with no global registry.
 */
fun <V : Any> SetProperty<V>.inheritFrom(
    common: SetProperty<V>,
    strategy: CollectionStrategy = CollectionStrategy.REPLACE,
) {
    if (strategy == CollectionStrategy.APPEND) {
        addAll(common)
    } else {
        convention(common)
    }
}

/**
 * Merges a nested [NamedDomainObjectContainer] from [common]: elements present only in [common] are
 * copied in, elements present in both are deep-merged via [CommonConfigMergeable.inheritFrom], and
 * elements defined only on the variant are left untouched.
 */
fun <V : Any> NamedDomainObjectContainer<V>.inheritNamedFrom(common: NamedDomainObjectContainer<V>) {
    for (name in common.names) {
        val commonElement = common.getByName(name)
        val existing = findByName(name)
        if (existing == null) {
            register(name) { copy -> mergeElement(copy, commonElement) }
        } else {
            mergeElement(existing, commonElement)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V : Any> mergeElement(
    target: V,
    common: V,
) {
    (target as? CommonConfigMergeable<V>)?.inheritFrom(common)
}

/**
 * Applies the common config as a fallback onto a freshly configured per-version-name [target] by
 * delegating to the config's own [CommonConfigMergeable.inheritFrom]. Configs that do not implement
 * [CommonConfigMergeable] are left as-is (their per-version-name block already fully replaces common).
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> applyCommonFallback(
    target: T,
    common: T,
) {
    (target as? CommonConfigMergeable<T>)?.inheritFrom(common)
}
