package ru.kode.android.build.publish.plugin.core.enity

/**
 * Represents a build variant in an Android project.
 *
 * A build variant is a combination of build type and product flavor that Gradle uses to create a build.
 * This class captures the essential information about a build variant including its name, flavor, and build type.
 */
data class BuildVariant(
    /**
     * The full name of the build variant (e.g., "stagingDebug")
     */
    val name: String,
    /**
     * The name of the product flavor, or null if no flavor is configured
     */
    val flavorName: String?,
    /**
     * The name of the build type (e.g., "debug", "release"), or null if not specified
     */
    val buildTypeName: String?,
    /**
     * The list of product flavors for this build variant.
     *
     * The list is empty if no flavor is configured.
     */
    val productFlavors: List<ProductFlavor>,
) {
    /**
     * Represents a product flavor in an Android project.
     *
     * A product flavor is a named configuration that can be used to customize the build of an Android project.
     * This class captures essential information about a product flavor including its name and dimension.
     */
    data class ProductFlavor(
        /**
         * The dimension that this product flavor belongs to.
         */
        val dimension: String,
        /**
         * The name of the product flavor.
         */
        val name: String,
    )
}
