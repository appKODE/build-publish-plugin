[← Documentation](../README.md)

# Custom plugin development

The foundation plugin will automatically configure any Gradle extension that:

- is registered on the project via `project.extensions.create(...)`, and
- has a type that extends `ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension`.

That mechanism is the intended extension point for adding custom behaviour.

### Creating a custom plugin that participates in variant configuration

1) Create a Gradle plugin (standard `java-gradle-plugin` setup).

2) Create an extension that extends `BuildPublishConfigurableExtension`:

```kotlin
abstract class BuildPublishMyExtension : BuildPublishConfigurableExtension() {
    abstract val enabled: Property<Boolean>

    override fun configure(project: Project, input: ExtensionInput, variant: ApplicationVariant) {
        if (!enabled.get()) return
        // Register tasks for input.buildVariant.name and wire them to input.output/input.changelog.
    }
}
```

3) Register the extension in your plugin and require the foundation plugin:

```kotlin
class BuildPublishMyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("ru.kode.android.build-publish-novo.foundation")
        project.extensions.create("buildPublishMy", BuildPublishMyExtension::class.java)
    }
}
```

### Attaching a custom plugin to a local project (separate module / separate repo)

If you develop your own Build Publish-compatible plugin in a separate module (or even a separate repository),
you can attach it to the main project without publishing to a remote repository.

#### Option A: `build-logic` / `build-conventions` (recommended for project-local conventions)

- Put your convention plugin in `build-logic` / `build-conventions`.
- Add Build Publish plugin artifacts to the convention module dependencies (see Installation section above).
- Apply your convention plugin from app modules.

This is best when the plugin is project-specific and you want it versioned together with the app.

#### Option B: Composite build (`includeBuild`) (recommended for reusable local plugins)

If your custom plugin is a standalone Gradle plugin project (uses `java-gradle-plugin`), you can attach it as a
**composite build**.

Kotlin DSL (`settings.gradle.kts`):

```kotlin
pluginManagement {
    includeBuild("../my-build-publish-plugin")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

Groovy DSL (`settings.gradle`):

```groovy
pluginManagement {
    includeBuild("../my-build-publish-plugin")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

After that you can apply the custom plugin by its id in any module:

```kotlin
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("your.custom.build-publish-plugin")
}
```

#### Option C: Publish to `mavenLocal()` (quick local testing)

If your custom plugin project has `maven-publish` configured, you can publish it locally:

- `./gradlew publishToMavenLocal`

Then add `mavenLocal()` to the repository list (either `pluginManagement.repositories` or normal `repositories`)
and apply the plugin by id.

This approach is convenient for local testing, but composite builds are usually a better long-term workflow.

