package ru.kode.android.build.publish.plugin.core.enity

/**
 * Represents a build variant in an Android project, which is a combination of build type and product flavors.
 *
 * A build variant is the result of Gradle combining a build type and product flavors from the BuildType and
 * ProductFlavor configurations. This class encapsulates all the necessary information about a specific build variant
 * that is used throughout the build and publish process.
 *
 * @see ProductFlavor for information about product flavor configurations.
 */
data class BuildVariant(
    /**
     * The full name of the build variant (e.g., "stagingDebug"), which is a combination of
     * the build type and product flavor names.
     */
    val name: String,
    /**
     * The name of the product flavor, or null if no flavor is configured.
     */
    val flavorName: String?,
    /**
     * The name of the build type (e.g., "debug", "release"), or null if not specified.
     */
    val buildTypeName: String?,
    /**
     * The list of [ProductFlavor] objects for this build variant. The list is empty if no
     * flavors are configured.
     */
    val productFlavors: List<ProductFlavor>,
    /**
     * The default version code for this build variant, or null if not specified.
     */
    val defaultVersionCode: Int?,
    /**
     * The default version name for this build variant, or null if not specified.
     */
    val defaultVersionName: String?,
) {
    /**
     * Represents a product flavor configuration in an Android project.
     *
     * A product flavor defines a customized version of the application build by the project. Each flavor
     * can specify its own application ID, version information, and other build configurations that override
     * the default settings in the main source set.
     */
    data class ProductFlavor(
        /**
         * The dimension that this product flavor belongs to. Dimensions are used to group
         * product flavors together, allowing for combinations of flavors across different
         * dimensions.
         */
        val dimension: String,
        /**
         * The name of the product flavor, which must be unique within its dimension.
         */
        val name: String,
    )
}
