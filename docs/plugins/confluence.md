[← Documentation](../../README.md) · [All plugins](./index.md)

# Confluence plugin

`ru.kode.android.build-publish-novo.confluence`

Update Confluence pages with release notes.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `confluenceDistributionUpload<Variant>` | Uploads APK to a Confluence page as an attachment and adds a comment | - |
| `confluenceDistributionUploadBundle<Variant>` | Uploads bundle (`.aab`) to a Confluence page as an attachment and adds a comment | - |

#### Task Usage Examples

```bash
# Upload APK for the release variant
./gradlew confluenceDistributionUploadRelease

# Upload bundle for the release variant
./gradlew confluenceDistributionUploadBundleRelease
```

#### Minimum Setup

1. Create Confluence API token (Confluence Cloud) or use your account password (Server/Data Center)
2. Find the Confluence page id (it is part of the page URL)
3. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.confluence")
}

buildPublishConfluence {
    auth {
        common {
            baseUrl.set("https://your-domain.atlassian.net/wiki")
            credentials.username.set("your-email@example.com")
            credentials.password.set(providers.environmentVariable("CONFLUENCE_API_TOKEN"))
        }
    }

    distribution {
        common {
            pageId.set("12345678")
            compressed.set(true)
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
    id 'ru.kode.android.build-publish-novo.confluence'
}

buildPublishConfluence {
    auth {
        common {
            baseUrl.set('https://your-domain.atlassian.net/wiki')
            credentials.username.set('your-email@example.com')
            credentials.password.set(providers.environmentVariable('CONFLUENCE_API_TOKEN'))
        }
    }

    distribution {
        common {
            pageId.set('12345678')
            compressed.set(true)
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishConfluence {
    auth {
        common {
            baseUrl.set("https://your-domain.atlassian.net/wiki")
            credentials.username.set("your-email@example.com")
            credentials.password.set(providers.environmentVariable("CONFLUENCE_API_TOKEN"))
        }
    }

    distribution {
        common {
            pageId.set("12345678")
            compressed.set(true)
        }

        buildVariant("release") {
            pageId.set("87654321")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishConfluence {
    auth {
        common {
            baseUrl.set('https://your-domain.atlassian.net/wiki')
            credentials.username.set('your-email@example.com')
            credentials.password.set(providers.environmentVariable('CONFLUENCE_API_TOKEN'))
        }
    }

    distribution {
        common {
            pageId.set('12345678')
            compressed.set(true)
        }

        buildVariant('release') {
            pageId.set('87654321')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Confluence plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth and distribution configuration are required**
  - `auth` must be configured (at least `auth.common { ... }`, internal common name is `default`).
  - `distribution` must be configured for each variant (`common { ... }` or `buildVariant("<name>") { ... }`).

- **The task uploads a file and adds a comment**
  - After successful upload, the plugin also posts a comment with the uploaded file name.

- **Ensure the artifact exists**
  - The task uses the variant output produced by the Android build. If the artifact was not built yet, run `assemble<Variant>` / `bundle<Variant>` first.

##### Auth (`buildPublishConfluence { auth { ... } }`)

Properties (applies to each `ConfluenceAuthConfig`):

- **`baseUrl`** *(required)*
  - **What it does**: Base URL of your Confluence instance.
  - **Common values**:
    - Cloud: `https://your-domain.atlassian.net/wiki`
    - Server/Data Center: `https://confluence.your-company.com`

- **`credentials.username`** *(required)*
  - **What it does**: Username/email used for authentication.

- **`credentials.password`** *(required)*
  - **What it does**: Password or API token.
  - **Recommendation**: For Confluence Cloud use an API token.
  - **Where to get it (Confluence Cloud)**: Atlassian account settings `Security -> API tokens -> Create API token` (use the token as the password).

##### Distribution (`buildPublishConfluence { distribution { ... } }`)

Properties (applies to each `ConfluenceDistributionConfig`):

- **`pageId`** *(required)*
  - **What it does**: Id of the Confluence page where the file should be uploaded.
  - **How to get**: It is part of the page URL, for example:
    - `.../wiki/spaces/SPACE/pages/12345678/Page+Title` → `pageId = 12345678`

- **`compressed`** *(optional, default `false`)*
  - **What it does**: Compresses the distribution file to a `.zip` before upload.
  - **Why you might need it**: Can reduce upload time for large artifacts.

##### Task options

- **`confluenceDistributionUpload<Variant>` / `confluenceDistributionUploadBundle<Variant>`** supports (CLI options):
  - `--distributionFile=/abs/path/to/app.apk` (or `.aab`)
  - `--pageId=12345678`
  - `--compressed=true`

---

