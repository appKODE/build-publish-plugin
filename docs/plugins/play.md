[← Documentation](../../README.md) · [All plugins](./index.md)

# Play Store plugin

`ru.kode.android.build-publish-novo.play`

Publish builds to Google Play Store with support for multiple tracks and release types.

This integration is based on ideas and implementation details from the community plugin
https://github.com/Triple-T/gradle-play-publisher, but the logic is adapted to this repository’s
variant-driven build-publish flow and could not be used as-is.

#### Key Features
- Publish to Google Play Console
- Support for multiple tracks (internal, alpha, beta, production)
- Release management (draft, in progress, completed)
- Support for release notes in multiple languages
- Integration with Google Play service account

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `playUpload<Variant>` | Uploads a bundle (`.aab`) to Google Play | `bundle<Variant>`, `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Upload release bundle to internal testing track
./gradlew playUploadRelease

# Override track via CLI options
./gradlew playUploadRelease --trackId=internal --updatePriority=0
```

#### Minimum Setup

1. Create a service account in Google Play Console
2. Download the JSON key file and add it to your project
3. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.play")
   }
   
   buildPublishPlay {
       auth {
           common {
               appId.set("com.example.app")
               apiTokenFile.set(file("play-account.json"))
           }
       }

       distribution {
           common {
               trackId.set("internal")
               updatePriority.set(0)
           }
       }
   }
   ```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishPlay {
    auth {
        common {
            appId.set("com.example.app")
            apiTokenFile.set(file("play-account.json"))
        }
    }

    distribution {
        common {
            trackId.set("internal")
            updatePriority.set(0)
        }

        buildVariant("release") {
            trackId.set("production")
            updatePriority.set(1)
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishPlay {
    auth {
        common {
            appId.set('com.example.app')
            apiTokenFile.set(file('play-account.json'))
        }
    }

    distribution {
        common {
            trackId.set('internal')
            updatePriority.set(0)
        }

        buildVariant('release') {
            trackId.set('production')
            updatePriority.set(1)
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Play plugin registers the `playUpload<Variant>` task from `BuildPublishPlayExtension.configure(...)`.
  - The foundation plugin is responsible for invoking `configure(...)` for all `BuildPublishConfigurableExtension` instances, so Play tasks appear only if `ru.kode.android.build-publish-novo.foundation` is applied.

- **Both `auth` and `distribution` must be configured**
  - `auth` is used to create Play API network services.
  - `distribution` is required to configure track/priority.
  - If there is no matching `common { ... }` (the internal common name is `default`) or no matching `buildVariant("<name>") { ... }`, the build fails fast with a “required configuration not found” error.

- **Uploads only support AAB**
  - `playUpload<Variant>` uploads an Android App Bundle (`.aab`). If the input file is not `.aab`, the task fails.

##### Auth (`buildPublishPlay { auth { ... } }`)

Configure Play authentication per variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("release") { ... }` to override for a single variant

Properties (applies to each `PlayAuthConfig`):

- **`appId`** *(required)*
  - **What it does**: The applicationId / package name of the app in Google Play Console.
  - **Why you need it**: Used to target the correct app when calling Play Developer API.
  - **Where to get it**: Your app module `applicationId` (Android Gradle config) / the package name shown in Google Play Console for the app.

- **`apiTokenFile`** *(required)*
  - **What it does**: Service account JSON key file.
  - **Why you need it**: Required to authenticate Play Developer API requests.
  - **Where to get it**: Google Play Console `Setup -> API access` (create/link a service account, then create/download a JSON key).
  - **How to use**: Store outside VCS and pass via `file("...")`.

##### Distribution (`buildPublishPlay { distribution { ... } }`)

Configure distribution per variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("release") { ... }` to override for a single variant

Properties (applies to each `PlayDistributionConfig`):

- **`trackId`** *(default: `internal`)*
  - **What it does**: Target track to publish to.
  - **Why you need it**: Different tracks are used for internal/alpha/beta/production flows.
  - **Typical values**: `internal`, `alpha`, `beta`, `production`.
  - **Where to get it**: Pick the track you use in Google Play Console (`Testing` / `Production`). The id is typically one of the values above.

- **`updatePriority`** *(default: `0`)*
  - **What it does**: In-app update priority (`0..5`) sent with the release.
  - **Why you need it**: Allows controlling update urgency for supported update flows.

##### Task options (`playUpload<Variant>`)

The upload task supports overriding some inputs via CLI (Gradle task options):

- `--trackId=internal`
- `--updatePriority=0`

The task is wired by default to:

- `getLastTagSnapshot<Variant>` (to compute release name metadata)

Note: `playUpload<Variant>` consumes the bundle output via AGP artifacts (`SingleArtifact.BUNDLE`).

---

