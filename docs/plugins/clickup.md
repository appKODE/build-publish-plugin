[← Documentation](../../README.md) · [All plugins](./index.md)

# ClickUp plugin

`ru.kode.android.build-publish-novo.clickup`

Update ClickUp tasks with build information.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `clickUpAutomation<Variant>` | Updates ClickUp tasks referenced in the changelog (tags / fix version custom field) | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Apply automation for the release variant
./gradlew clickUpAutomationRelease
```

#### Minimum Setup

1. Create a ClickUp API token (store it in a local file, don’t commit it)
2. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.clickup")
}

buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(file("clickup-token.txt"))
        }
    }

    automation {
        common {
            workspaceName.set("Your Workspace")

            // Enable at least one automation action
            tagPattern.set("%s")
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
    id 'ru.kode.android.build-publish-novo.clickup'
}

buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(file('clickup-token.txt'))
        }
    }

    automation {
        common {
            workspaceName.set('Your Workspace')

            // Enable at least one automation action
            tagPattern.set('%s')
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(file("clickup-token.txt"))
        }
    }

    automation {
        common {
            workspaceName.set("Your Workspace")

            // Patterns use String.format(pattern, buildVersion, buildNumber, buildVariant)
            // Example: v1.2.3 / 42 / release
            tagPattern.set("release-%s")

            // Fix version automation requires BOTH properties below
            fixVersionPattern.set("%s")
            fixVersionFieldName.set("Fix Version")
        }

        buildVariant("release") {
            workspaceName.set("Your Workspace")
            tagPattern.set("%s")
            fixVersionPattern.set("%s")
            fixVersionFieldName.set("Fix Version")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(file('clickup-token.txt'))
        }
    }

    automation {
        common {
            workspaceName.set('Your Workspace')

            // format args order: buildVersion, buildNumber, buildVariant
            tagPattern.set('release-%s')

            // Fix version automation requires BOTH properties below
            fixVersionPattern.set('%s')
            fixVersionFieldName.set('Fix Version')
        }

        buildVariant('release') {
            workspaceName.set('Your Workspace')
            tagPattern.set('%s')
            fixVersionPattern.set('%s')
            fixVersionFieldName.set('Fix Version')
        }
    }
}
```

#### Resolving changelog issue references (titles)

Independently of automation, the ClickUp plugin can act as a **resolver** for the foundation
`issueReferences { }` markers (`CLOSES:` / `FIXES:` — see the [Foundation plugin](./foundation.md)).
When enabled it turns ClickUp task-id references into task **names** (titles) and the foundation
changelog is enriched, so the resolved titles then appear in every distributed changelog
(Slack / Telegram / Nextcloud). It reuses the existing `auth { }` credentials.

This is opt-in via `issueResolution { }`:

```kotlin
buildPublishClickUp {
    // auth { … } as above

    issueResolution {
        common {
            // Optional: restrict which reference tokens ClickUp attempts
            // taskIdPattern.set("…")
        }
    }
}
```

```groovy
buildPublishClickUp {
    // auth { … } as above

    issueResolution {
        common {
            // Optional: restrict which reference tokens ClickUp attempts
            // taskIdPattern.set('…')
        }
    }
}
```

The optional `taskIdPattern` restricts which reference tokens ClickUp attempts (tokens not matching are
ignored), so Jira and ClickUp resolvers coexist without clashing. Resolution is non-blocking — an
unresolved reference never fails the build; how it renders is controlled by the foundation
`unresolvedIssueStrategy { }`.

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The ClickUp plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth configuration is required**
  - At least `auth.common { ... }` (internal common name is `default`) must be configured.

- **Automation configuration is required per variant**
  - If there is no matching `automation.common { ... }` or `automation.buildVariant("<name>") { ... }`, configuration fails.

- **The task is created only if at least one automation action is enabled**
  - `clickUpAutomation<Variant>` is registered only if:
    - `tagPattern` is set, or
    - both `fixVersionPattern` and `fixVersionFieldName` are set.

- **Fix version settings must be provided together**
  - If you set only one of `fixVersionPattern` / `fixVersionFieldName`, configuration fails.

- **Issue/task IDs are extracted from the changelog**
  - The task scans the generated changelog file using the union of every foundation
    `issueSources` `numberPattern` (ClickUp uses only the patterns, never `urlPrefix`).
  - If no issues are found, the task logs an info message and does nothing.

##### Auth (`buildPublishClickUp { auth { ... } }`)

Properties (applies to each `ClickUpAuthConfig`):

- **`apiTokenFile`** *(required)*
  - **What it does**: File containing your ClickUp API token.
  - **Why you need it**: Used to authenticate ClickUp API requests.
  - **Where to get it**: ClickUp UI `Profile picture (bottom-left) -> Apps -> Developer API -> Generate`.
  - **Notes**: Keep it out of VCS.

##### Automation (`buildPublishClickUp { automation { ... } }`)

Properties (applies to each `ClickUpAutomationConfig`):

- **`workspaceName`** *(required)*
  - **What it does**: ClickUp workspace name.
  - **Why you need it**: Used to resolve custom field ids for fix version updates.
  - **Where to get it**: The workspace name shown in ClickUp’s workspace switcher (top-left in the UI).

- **`tagPattern`** *(optional)*
  - **What it does**: Adds a tag to each ClickUp task referenced in the changelog.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `release-%s`.

- **`fixVersionPattern`** *(optional, requires `fixVersionFieldName`)*
  - **What it does**: Computes a fix version value for each task.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `%s`.

- **`fixVersionFieldName`** *(optional, requires `fixVersionPattern`)*
  - **What it does**: Name of the ClickUp custom field where fix version will be written.
  - **Example**: `Fix Version`.
  - **Where to get it**: The exact custom field name configured in your ClickUp space/list (Custom Fields settings). The plugin matches by name.

##### Task options (`clickUpAutomation<Variant>`)

The task supports overriding inputs via CLI options:

- `--workspaceName=Your Workspace`
- `--buildTagSnapshotFile=/abs/path/to/tag.json`
- `--changelogFile=/abs/path/to/changelog.md`
- `--issuePattern=#(\\d+)` — repeatable; pass once per issue-key pattern to extract
- `--fixVersionPattern=%s`
- `--fixVersionFieldName=Fix Version`
- `--tagPattern=release-%s`

---

