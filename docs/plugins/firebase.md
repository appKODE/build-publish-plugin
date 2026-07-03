[← Documentation](../../README.md) · [All plugins](./index.md)

# Firebase plugin

`ru.kode.android.build-publish-novo.firebase`

Publish builds to Firebase App Distribution with support for multiple variants and tester groups.

#### Key Features
- Publish APK/AAB to Firebase App Distribution
- Support for multiple build variants
- Tester group management
- Release notes from changelog
- Integration with Firebase service accounts

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `appDistributionUpload<Variant>` | Uploads the current variant artifact to Firebase App Distribution | Created by the official `com.google.firebase.appdistribution` plugin |

#### Task Usage Examples

```bash
# Upload debug build to Firebase
./gradlew appDistributionUploadDebug
```

#### Minimum Setup

1. Add Firebase configuration to your project:
   - Add `google-services.json` to your app module
     - **Where to get it**: Firebase Console `Project settings -> General -> Your apps` (register an Android app if needed, then download `google-services.json`).
   - Add Firebase App Distribution plugin to your root build script (so the plugin is on the classpath):

     Kotlin DSL:
     ```kotlin
     plugins {
         id("com.google.firebase.appdistribution") version "<your-firebase-appdistribution-version>"
     }
     ```

     Groovy DSL:
     ```groovy
     plugins {
         id 'com.google.firebase.appdistribution' version '<your-firebase-appdistribution-version>'
     }
     ```

2. Configure the plugin:
   
##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.firebase")
   }
   
   buildPublishFirebase {
       distribution {
           common {
               appId.set("your-firebase-app-id")
               serviceCredentialsFile.set(file("path/to/service-account.json"))
               artifactType.set(ArtifactType.Bundle)
               testerGroup("qa-team")
           }
       }
   }
   ```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.firebase'
}

buildPublishFirebase {
    distribution {
        common {
            appId.set('your-firebase-app-id')
            serviceCredentialsFile.set(file('path/to/service-account.json'))
            artifactType.set(ru.kode.android.build.publish.plugin.firebase.config.ArtifactType.Bundle)
            testerGroup('qa-team')
        }
    }
}
```

The Firebase plugin configures the official Firebase App Distribution Gradle plugin. Upload tasks are
created by Firebase itself and typically look like:

- `appDistributionUpload<Variant>`

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishFirebase {
   distribution {
      common {
         appId.set("your-firebase-app-id")
         serviceCredentialsFile.set(file("path/to/service-account.json"))
         artifactType.set(ArtifactType.Bundle)
         testerGroups("qa-team", "developers")
      }

      buildVariant("release") {
         testerGroup("beta-testers")
      }
   }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishFirebase {
    distribution {
        common {
            appId.set('your-firebase-app-id')
            serviceCredentialsFile.set(file('path/to/service-account.json'))
            artifactType.set(ru.kode.android.build.publish.plugin.firebase.config.ArtifactType.Bundle)
            testerGroups('qa-team', 'developers')
        }

        buildVariant('release') {
            testerGroup('beta-testers')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - `buildPublishFirebase` wires Firebase `releaseNotesFile` from the foundation changelog output.
  - Apply `ru.kode.android.build-publish-novo.foundation` and configure `buildPublishFoundation { changelog { ... } }` if you want meaningful release notes.

- **The Firebase App Distribution Gradle plugin is applied conditionally**
  - This plugin applies the official `com.google.firebase.appdistribution` plugin **only if** at least one `distribution { ... }` config is declared.

- **Distribution config must exist per-variant**
  - During variant configuration, if there is no `common { ... }` (or no matching `buildVariant("<name>") { ... }`) the build fails with an explicit error asking you to add distribution config.

##### Distribution (`buildPublishFirebase { distribution { ... } }`)

Configure distribution per Android variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("release") { ... }` to override for a single variant

Properties (applies to each `FirebaseDistributionConfig`):

- **`appId`** *(required)*
  - **What it does**: Firebase App ID to upload to.
  - **Why you need it**: Firebase App Distribution requires a target application.
  - **Where to get it**: Firebase Console `Project settings -> General` (format like `1:1234567890:android:...`).

- **`serviceCredentialsFile`** *(required)*
  - **What it does**: Service account JSON used to authenticate uploads.
  - **Why you need it**: Upload requires server-side credentials.
  - **Where to get it**: Firebase Console `Project settings -> Service accounts -> Generate new private key` (download the JSON).
  - **How to use**: Store outside VCS and pass via `file("...")`.

- **`artifactType`** *(required)*
  - **What it does**: Chooses which artifact to upload.
  - **Values**:
    - `ArtifactType.Apk`
    - `ArtifactType.Bundle`
  - **Why you need it**: Firebase needs to know whether to upload APK or AAB.

- **`testerGroup("...")` / `testerGroups(...)`** *(optional)*
  - **What it does**: Defines which Firebase tester groups receive the release.
  - **Why you need it**: Automates targeting QA/beta groups.
  - **Where to get it**: Firebase Console `App Distribution -> Testers & groups` (create a group if needed, then use the group name).
  - **Notes**: Groups must exist in Firebase Console.

---

