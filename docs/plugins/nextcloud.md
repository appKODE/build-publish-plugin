[← Documentation](../../README.md) · [All plugins](./index.md)

# Nextcloud plugin

`ru.kode.android.build-publish-novo.nextcloud`

Upload APK/AAB artifacts and changelog files to self-hosted Nextcloud folders.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `nextcloudDistributionUpload<Variant>` | Uploads APK to Nextcloud via WebDAV and shares it according to configured `shareMode` | `getLastTagSnapshot<Variant>` |
| `nextcloudDistributionUploadBundle<Variant>` | Uploads bundle (`.aab`) to Nextcloud via WebDAV and shares it according to configured `shareMode` | `getLastTagSnapshot<Variant>` |
| `nextcloudChangelogUpload<Variant>` | Uploads generated changelog file to Nextcloud and shares it according to configured `shareMode` | `getLastTagSnapshot<Variant>`, `generateChangelog<Variant>` |

#### Task Usage Examples

```bash
# Upload file only (APK) for release variant
./gradlew nextcloudDistributionUploadRelease

# Upload changelog only for release variant
./gradlew nextcloudChangelogUploadRelease

# Upload file + changelog in a single invocation
./gradlew nextcloudDistributionUploadRelease nextcloudChangelogUploadRelease

# Upload bundle for release variant
./gradlew nextcloudDistributionUploadBundleRelease
```

#### Minimum Setup

1. Create Nextcloud app password (recommended) for a user account.
2. Choose target remote folder under that user’s files root.
3. Configure plugin:

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.nextcloud")
}

buildPublishNextcloud {
    auth {
        common {
            baseUrl.set("https://cloud.example.com")
            credentials.username.set("mobile-bot")
            credentials.password.set(providers.environmentVariable("NEXTCLOUD_APP_PASSWORD"))
        }
    }

    distribution {
        common {
            remotePath.set("mobile/project-a/release")
            compressed.set(false)
            shareMode.set(ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode.INTERNAL_RECIPIENTS)
            userRecipients("qa-bot", "release-manager")
            groupRecipients("android-team")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.nextcloud'
}

buildPublishNextcloud {
    auth {
        common {
            baseUrl.set('https://cloud.example.com')
            credentials.username.set('mobile-bot')
            credentials.password.set(providers.environmentVariable('NEXTCLOUD_APP_PASSWORD'))
        }
    }

    distribution {
        common {
            remotePath.set('mobile/project-a/release')
            compressed.set(false)
            shareMode.set(ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode.INTERNAL_RECIPIENTS)
            userRecipients('qa-bot', 'release-manager')
            groupRecipients('android-team')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Nextcloud plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth and distribution are required**
  - Configure at least `auth.common { ... }` and matching `distribution.common { ... }` (or per-variant overrides).

- **Folder-first publishing**
  - `remotePath` is required and can be nested (`mobile/project-a/release`).
  - Missing folders are auto-created (WebDAV auto-mkcol + recursive fallback).

- **Task-based publish control**
  - File only: run `nextcloudDistributionUpload<Variant>` / `nextcloudDistributionUploadBundle<Variant>`.
  - Changelog only: run `nextcloudChangelogUpload<Variant>`.
  - Both: run both tasks in one Gradle command.

- **Default share mode is internal recipients**
  - Default `shareMode` is `INTERNAL_RECIPIENTS`.
  - At least one recipient must be configured via `userRecipients(...)` or `groupRecipients(...)`.
  - Task output includes internal file URL (`<baseUrl>/f/<fileId>`).

- **Public link mode is still available**
  - Set `shareMode = PUBLIC_LINK` to keep public share-link behavior.
  - Existing public links are reused for reruns of the same remote path.

- **Deterministic remote naming (overwrite-friendly)**
  - Default remote file name is computed as:
    - Artifact: `<baseFileName>-<buildVersion>-<variant>.<ext>`
    - Changelog: `<baseFileName>-<buildVersion>-<variant>-changelog.<ext>`
  - Same version + variant uploads to the same remote path and overwrites.
  - Set `remoteFileName` to override the computed name.

##### Auth (`buildPublishNextcloud { auth { ... } }`)

Properties (applies to each `NextcloudAuthConfig`):

- **`baseUrl`** *(required)*
  - **What it does**: Base URL of your Nextcloud server.
  - **Example**: `https://cloud.example.com`

- **`credentials.username`** *(required)*
  - **What it does**: Nextcloud username for API/WebDAV operations.

- **`credentials.password`** *(required)*
  - **What it does**: Password or app password.
  - **Recommendation**: Use app passwords from Nextcloud security settings.

##### Distribution (`buildPublishNextcloud { distribution { ... } }`)

Properties (applies to each `NextcloudDistributionConfig`):

- **`remotePath`** *(required)*
  - **What it does**: Folder path under `/remote.php/dav/files/{username}/`.
  - **Example**: `mobile/project-a/release`

- **`compressed`** *(optional, default `false`)*
  - **What it does**: Compresses artifact before upload (`.zip`).

- **`shareMode`** *(optional, default `INTERNAL_RECIPIENTS`)*
  - **What it does**: Selects sharing strategy:
    - `INTERNAL_RECIPIENTS` -> user/group shares + internal URL
    - `PUBLIC_LINK` -> public share link

- **`userRecipients(...)`** *(required when `shareMode=INTERNAL_RECIPIENTS` and `groupRecipients` is empty)*
  - **What it does**: Adds Nextcloud user IDs as recipients.

- **`groupRecipients(...)`** *(required when `shareMode=INTERNAL_RECIPIENTS` and `userRecipients` is empty)*
  - **What it does**: Adds Nextcloud group IDs as recipients.

- **`remoteFileName`** *(optional)*
  - **What it does**: Explicit remote target file name. Overrides deterministic default naming.

##### Task options

- **`nextcloudDistributionUpload<Variant>` / `nextcloudDistributionUploadBundle<Variant>`** support:
  - `--distributionFile=/abs/path/to/app.apk` (or `.aab`)
  - `--remotePath=mobile/project-a/release`
  - `--compressed=true`
  - `--shareMode=INTERNAL_RECIPIENTS` (or `PUBLIC_LINK`)
  - `--userRecipients=user-a,user-b`
  - `--groupRecipients=android-team`
  - `--remoteFileName=autotest-1.2.3-release.apk`

- **`nextcloudChangelogUpload<Variant>`** supports:
  - `--changelogFile=/abs/path/to/changelog.md`
  - `--remotePath=mobile/project-a/release`
  - `--shareMode=INTERNAL_RECIPIENTS` (or `PUBLIC_LINK`)
  - `--userRecipients=user-a,user-b`
  - `--groupRecipients=android-team`
  - `--remoteFileName=autotest-1.2.3-release-changelog.md`

