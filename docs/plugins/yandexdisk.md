[← Documentation](../../README.md) · [All plugins](./index.md)

# Yandex Disk plugin

`ru.kode.android.build-publish-novo.yandexdisk`

Upload APK/AAB artifacts and changelog files to a Yandex Disk folder, optionally with a public link.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `yandexDiskDistributionUpload<Variant>` | Uploads APK to Yandex Disk, publishes it when `publicLink = true` | `getLastTagSnapshot<Variant>` |
| `yandexDiskDistributionUploadBundle<Variant>` | Uploads bundle (`.aab`) to Yandex Disk, publishes it when `publicLink = true` | `getLastTagSnapshot<Variant>` |
| `yandexDiskChangelogUpload<Variant>` | Uploads generated changelog file to Yandex Disk, publishes it when `publicLink = true` | `getLastTagSnapshot<Variant>`, `generateChangelog<Variant>` |
| `uploadToYandexDisk` | Standalone: uploads any file (`--file`, `--remotePath`, `--remoteFileName`) | — |

#### Task Usage Examples

```bash
# Upload APK for release variant
./gradlew yandexDiskDistributionUploadRelease

# Upload APK + changelog in a single invocation
./gradlew yandexDiskDistributionUploadRelease yandexDiskChangelogUploadRelease

# Upload bundle for release variant
./gradlew yandexDiskDistributionUploadBundleRelease
```

#### Minimum Setup

1. Register an app at [oauth.yandex.ru](https://oauth.yandex.ru/) with the `cloud_api:disk.write` and
   `cloud_api:disk.read` permissions and get an OAuth token for the disk owner
   ([quickstart](https://yandex.ru/dev/disk-api/doc/en/concepts/quickstart)).
2. Store the token in a file outside version control.
3. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.yandexdisk")
}

buildPublishYandexDisk {
    auth {
        common {
            oauthTokenFile.set(rootProject.file("yandex-disk-token.txt"))
        }
    }

    distribution {
        common {
            remotePath.set("builds/project-a/release")
            compressed.set(false)
            publicLink.set(true)
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.yandexdisk'
}

buildPublishYandexDisk {
    auth {
        common {
            oauthTokenFile.set(rootProject.file('yandex-disk-token.txt'))
        }
    }

    distribution {
        common {
            remotePath.set('builds/project-a/release')
            compressed.set(false)
            publicLink.set(true)
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Auth and distribution are required**
  - Configure at least `auth.common { ... }` and matching `distribution.common { ... }` (or per-variant overrides).

- **Upload flow**
  - `GET /v1/disk/resources/upload?path=...&overwrite=true` returns an upload URL, then the file is sent with
    `PUT` to that URL. The OAuth token is sent only to `cloud-api.yandex.net`, never to the upload host.
  - Missing folders in `remotePath` are created (`PUT /v1/disk/resources`).

- **Private by default**
  - With `publicLink = false` (default) the file stays private to the disk owner and the task logs its
    `disk:/` path.
  - With `publicLink = true` the file is published (`PUT /v1/disk/resources/publish`) and the public URL is
    logged. An existing public link is reused on reruns.

- **Deterministic remote naming (overwrite-friendly)**
  - Artifact: `<baseFileName>-<buildVersion>-<variant>.<ext>`
  - Changelog: `<baseFileName>-<buildVersion>-<variant>-changelog.<ext>`
  - Same version + variant overwrites the same remote file. Set `remoteFileName` to override.

##### Auth (`buildPublishYandexDisk { auth { ... } }`)

- **`oauthTokenFile`** *(required)*
  - **What it does**: File with a Yandex OAuth token (`cloud_api:disk.write`, plus `cloud_api:disk.read` for
    public links).

##### Distribution (`buildPublishYandexDisk { distribution { ... } }`)

- **`remotePath`** *(required)*
  - **What it does**: Folder relative to the disk root. **Example**: `builds/project-a/release`

- **`compressed`** *(optional, default `false`)*
  - **What it does**: Compresses the artifact before upload (`.zip`).

- **`publicLink`** *(optional, default `false`)*
  - **What it does**: Publishes the uploaded file and logs its public link.

- **`remoteFileName`** *(optional)*
  - **What it does**: Explicit remote file name. Overrides deterministic default naming.

##### Task options

- **`yandexDiskDistributionUpload<Variant>` / `yandexDiskDistributionUploadBundle<Variant>`**:
  `--distributionFile`, `--remotePath`, `--compressed`, `--publicLink`, `--remoteFileName`
- **`yandexDiskChangelogUpload<Variant>`**: `--changelogFile`, `--remotePath`, `--publicLink`, `--remoteFileName`
