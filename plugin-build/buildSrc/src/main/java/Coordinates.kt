object PluginCoordinates {
    const val ID = "ru.kode.android.build-publish"
    const val GROUP = "ru.kode.android"
    const val VERSION = "1.0.5-hotfix01"
    const val IMPLEMENTATION_CLASS = "ru.kode.android.build.publish.plugin.BuildPublishPlugin"
}

object PluginBundle {
    const val WEBSITE = "https://github.com/appKODE/build-publish-plugin"
    const val VCS = "https://github.com/appKODE/build-publish-plugin"
    const val DESCRIPTION = "Android plugin to publish bundles and apks to Firebase App Distribution with changelogs"
    const val DISPLAY_NAME = "Configure project with Firebase App Distribution and changelogs"
    val TAGS = listOf(
        "firebase",
        "publish",
        "changelog",
        "build"
    )
}
