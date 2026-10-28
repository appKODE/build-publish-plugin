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
- Remote Config audit: compares keys declared in code at the last release tag with the live template

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `appDistributionUpload<Variant>` | Uploads the current variant artifact to Firebase App Distribution | Created by the official `com.google.firebase.appdistribution` plugin |
| `checkFirebaseRemoteConfig<Variant>` | Audits Firebase Remote Config against the keys in code (see [Remote Config audit](#remote-config-audit)) | `getLastTagSnapshot<Variant>` |

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

- **Distribution or Remote Config config must exist per-variant**
  - During variant configuration, if neither a `distribution` nor a `remoteConfig` config matches the variant
    (via `common { ... }` or `buildVariant("<name>") { ... }`), the build fails with an explicit error.

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

---

#### Remote Config audit

`checkFirebaseRemoteConfig<Variant>` answers "what is in Remote Config for this release?" without a person who
remembers it. It reads the keys declared in code at the **last release tag** and at the **previous tag**
(straight from git, so the working tree does not matter), fetches the live Remote Config template and writes:

- `build/firebase/remote-config/<variant>/report.md` — human-readable, keys in backticks (safe for messengers)
- `build/firebase/remote-config/<variant>/report.json` — every finding, for CI checks, dashboards or an AI agent
- `build/firebase/remote-config/<variant>/report.sarif` — SARIF 2.1.0 for code scanning (see [SARIF](#sarif))

The task is never up-to-date (the template changes outside the build) and fails if any finding has `Fail`
severity.

| Finding | Default severity | Meaning |
|---------|------------------|---------|
| `AddedInRelease` / `RemovedInRelease` | Info | Key declared in code since / no longer since the previous tag |
| `MissingInRemote` | Warn | Key declared in code but absent from the template — forgot to create it |
| `UnusedInRemote` | Info | Template key not declared, not found by `usageSearch`, not dynamic or ignored — cleanup candidate |
| `TargetsRelease` | Info | Conditional value whose condition uses `app.version` / `app.build` and matches (or may match) this release's `versionName` / `versionCode` |
| `ChangedSinceRelease` | Info | Template version published after the previous tag's commit: who, when, which keys |
| `PotentiallyStale` | Warn | Parameter whose `expires=` date passed, or whose `created=` date is older than `flagLifetimes[type]` (see [Lifecycle metadata](#lifecycle-metadata)) |
| `DefaultsMismatch` | Warn | Firebase default differs from the in-app default, or Firebase uses the in-app default but the app has none |

In `report.json` the `type` field is the upper snake case form (`ADDED_IN_RELEASE`, `MISSING_IN_REMOTE`, …).

Conditions are evaluated with three-valued logic: `percent`, audiences, user properties and other
non-version signals are `Unknown`, so `Unknown && <version matches>` is reported as `Unknown`.

Firebase keeps only the last 300 template versions for 90 days; when the window since the previous tag is
older or larger, the report says so.

##### Setup

```kotlin
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.FindingType
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.Severity

buildPublishFirebase {
    remoteConfigCommon {
        projectId.set("my-app-12345")
        serviceCredentialsFile.set(file(providers.environmentVariable("FIREBASE_RC_SA_KEY")))

        // Where keys are declared. Paths are git paths relative to the repository root.
        keySource("toggles") {
            include("app/src/main/**/AppFeature.kt")
            extractor.set(regex("""data object\s+\w+\s*:\s*AppFeature\(\s*(?:id\s*=\s*)?"([^"]+)""""))
        }
        keySource("values") {
            include("**/*.kt")
            extractor.set(regex("""const val\s+\w+_KEY\s*(?::\s*String)?\s*=\s*"([^"]+)""""))
        }
        keySource("defaults") {
            include("**/res/xml/remote_config_defaults.xml")
            extractor.set(defaultsXml())
        }

        // Template keys found anywhere here as a "string literal" are not reported as unused.
        usageSearch("app/**", "core/**")
        inAppDefaults("**/res/xml/remote_config_defaults.xml") // compared with the Firebase defaults
        flagLifetimes.put("experiment", 14)                   // days; release and experiment default to 40
        dynamicKeyPrefixes.add("survey_") // keys built at runtime
        ignoreKeys.add("ios_.*")           // regex; e.g. keys of another platform in a shared project

        severity(FindingType.MissingInRemote, Severity.Fail)
    }
}
```

Groovy: `severity("MissingInRemote", "Fail")`.

- **`projectId`**, **`serviceCredentialsFile`** *(required)* — Firebase project and a service account JSON key.
  Read access is enough: grant the `Remote Config Viewer` role (`roles/cloudconfig.viewer`).
- **`keySource(name) { include(...); extractor.set(...) }`** — glob(s) plus an extractor. `regex(pattern)` yields
  the first matching capture group (or the whole match); `defaultsXml()` reads `<key>` entries. Any
  `RemoteConfigKeyExtractor` implementation (for example from `buildSrc`) works too — it only gets
  `(path, content)` and must be `java.io.Serializable`.
- **`usageSearch(globs)`**, **`dynamicKeyPrefixes`**, **`ignoreKeys`** — reduce `UnusedInRemote` noise.
- **`inAppDefaults(globs)`** — `setDefaultsAsync(R.xml.…)` files read at the current tag; their `<entry>`
  values are compared with each parameter's Firebase default (`DefaultsMismatch`). DTDs are rejected.
- **`flagLifetimes`** — days a parameter of a metadata `type=` may live (`release` and `experiment`: 40).
  Types without an entry never become `PotentiallyStale`.
- **`severity(type, severity)`** — override defaults; any `Fail` finding fails the task.
- **`apiBaseUrl`** — only for tests or a gateway.

The tag history must be available: on CI use a full clone (`GIT_DEPTH: 0` / `fetch-depth: 0`).

##### Lifecycle metadata

Firebase stores no creation date per parameter, so the age of a flag comes from its description (up to 256
characters). Any of `owner=`, `type=`, `created=` and `expires=` (ISO dates) may appear anywhere in it:

```
Checkout redesign. owner=@ivan type=release created=2026-07-01 expires=2026-12-01
```

A parameter is `PotentiallyStale` when `expires` is before the commit date of the release tag, or `created` plus
`flagLifetimes[type]` is. Long-lived types (`ops`, `permission`, `kill-switch`, …) have no lifetime and never go
stale. The report shows `owner` next to `PotentiallyStale` and `UnusedInRemote` findings.

##### SARIF

`report.sarif` has one rule per finding type. Findings that point at a file of the release tag become results
(`AddedInRelease`, `MissingInRemote`, `DefaultsMismatch`); the others are in `report.md` / `report.json` only.
On GitHub upload it to code scanning:

```yaml
- run: ./gradlew checkFirebaseRemoteConfigRelease
- uses: github/codeql-action/upload-sarif@v3
  if: always()
  with:
    sarif_file: app/build/firebase/remote-config/release/report.sarif
    category: remote-config
```

##### Without an Android build (sender plugin)

The `ru.kode.android.build-publish-novo.sender` plugin registers `checkFirebaseRemoteConfig` with the same DSL,
for repositories without AGP (for example a backend or CI-only project). Release tags come from
`buildTagPattern` (the first group is the build number) instead of a build variant; `app.version` /
`app.build` default to the tag (`1.0.2` / `2` for `v1.0.2`) unless `appVersion` / `appBuild` are set.

```kotlin
buildPublishSender {
    firebaseRemoteConfig {
        projectId.set("my-app-12345")
        serviceCredentialsFile.set(file(providers.environmentVariable("FIREBASE_RC_SA_KEY")))
        buildTagPattern.set("""v\d+\.\d+\.(\d+)""")
        keySource("values") {
            include("**/*.kt")
            extractor.set(regex("""const val\s+\w+_KEY\s*=\s*"([^"]+)""""))
        }
    }
}
```

The version taken from the tag is every number before the first `-` (`v1.0.2-rc1` gives `1.0.2` / `2`), and
the values used are logged at info level. Set `appVersion` / `appBuild` explicitly when the build number is a
separate number after the version (`app.1.2.3.42` would give `1.2.3.42` instead of `1.2.3`); otherwise
`TargetsRelease` may match the wrong conditions.

Reports are written to `build/firebase/remote-config/`.

##### Proxy

The JVM ignores `HTTPS_PROXY`. Pass `-Dhttps.proxyHost=… -Dhttps.proxyPort=…` (for example in `GRADLE_OPTS`);
`firebaseremoteconfig.googleapis.com` and `oauth2.googleapis.com` must be reachable.

##### Sending the report to Telegram

No built-in sender: wire the report into the Telegram plugin's `sendTelegramMessage` task.

```kotlin
import ru.kode.android.build.publish.plugin.telegram.task.standalone.SendTelegramMessageTask

val remoteConfigReport = layout.buildDirectory.file("firebase/remote-config/release/report.md")
tasks.named<SendTelegramMessageTask>("sendTelegramMessage") {
    mustRunAfter("checkFirebaseRemoteConfigRelease")
    message.set(providers.fileContents(remoteConfigReport).asText)
}
```

```bash
./gradlew checkFirebaseRemoteConfigRelease sendTelegramMessage --botName ci --chatName releases
```

