[← Documentation](../../README.md) · [All plugins](./index.md)

# VK plugin

`ru.kode.android.build-publish-novo.vk`

Send APK/AAB artifacts as documents to a VK conversation on behalf of a community.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `vkDistributionUpload<Variant>` | Uploads APK as a VK document and sends it to `peerId` | `getLastTagSnapshot<Variant>` |
| `vkDistributionUploadBundle<Variant>` | Uploads bundle (`.aab`) as a VK document and sends it to `peerId` | `getLastTagSnapshot<Variant>` |
| `uploadToVk` | Standalone: sends any file (`--file`, `--peerId`, `--title`, `--message`) | — |

#### Task Usage Examples

```bash
# Send APK for release variant
./gradlew vkDistributionUploadRelease

# Send bundle for release variant
./gradlew vkDistributionUploadBundleRelease
```

#### Minimum Setup

1. In the community settings ("API usage" → "Access tokens") create a community token with the
   **docs** and **messages** rights, and enable community messages.
2. Pick the receiving conversation (`peerId`):
   - a user id — the user must have allowed messages from the community;
   - `2000000000 + chat_id` — a chat the community was added to.
3. Store the token in a file outside version control and configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.vk")
}

buildPublishVk {
    auth {
        common {
            accessTokenFile.set(rootProject.file("vk-token.txt"))
        }
    }

    distribution {
        common {
            peerId.set(2000000001L)
            compressed.set(true)
            message.set("New Android build")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.vk'
}

buildPublishVk {
    auth {
        common {
            accessTokenFile.set(rootProject.file('vk-token.txt'))
        }
    }

    distribution {
        common {
            peerId.set(2000000001L)
            compressed.set(true)
            message.set('New Android build')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Auth and distribution are required**
  - Configure at least `auth.common { ... }` and matching `distribution.common { ... }` (or per-variant overrides).

- **Send flow (VK API 5.199)**
  - `docs.getMessagesUploadServer(type=doc, peer_id)` → multipart upload of the file →
    `docs.save` → `messages.send` with the `doc<owner>_<id>` attachment.
  - The token is sent as an `Authorization: Bearer` header to `api.vk.com` only, never to the upload host.
  - The task logs the message id and the document URL.

- **Use `compressed = true`**
  - VK restricts some document types; whether raw `.apk`/`.aab` files are accepted has not been verified.
    A `.zip` is the safe choice.

- **Common errors**
  - `5` invalid token, `15` / `27` missing rights or wrong token type (a community token is required),
    `901` the user has not allowed messages from the community, `917` the community has no access to the chat.

- **Deterministic document title**
  - Defaults to `<baseFileName>-<buildVersion>-<variant>.<ext>`. Set `title` to override.

##### Auth (`buildPublishVk { auth { ... } }`)

- **`accessTokenFile`** *(required)*
  - **What it does**: File with a VK community access token (`docs`, `messages` rights).

##### Distribution (`buildPublishVk { distribution { ... } }`)

- **`peerId`** *(required)*
  - **What it does**: Receiving conversation: user id or `2000000000 + chat_id`.

- **`compressed`** *(optional, default `false`)*
  - **What it does**: Compresses the artifact before upload (`.zip`).

- **`message`** *(optional)*
  - **What it does**: Text sent together with the document.

- **`title`** *(optional)*
  - **What it does**: Explicit document title. Overrides the deterministic default.

##### Task options

- **`vkDistributionUpload<Variant>` / `vkDistributionUploadBundle<Variant>`**:
  `--distributionFile`, `--peerId`, `--compressed`, `--message`, `--title`
