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
2. Register at least one project under an account (`auth { … account("name") { apiTokenFile; project("name") { workspaceName; taskIdPrefix } } }`)
3. Configure at least one automation action (tag, or fix version)

Workspaces and task-id prefixes live only in the `auth` account registry (like Telegram chats under
a bot); automation then selects registered projects by their **local name** through
`targetAccount("account")`.

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
            account("main") {
                apiTokenFile.set(file("clickup-token.txt"))
                // Register the project(s), their workspaces and task-id prefixes here
                project("app") {
                    workspaceName.set("Your Workspace")
                    taskIdPrefix.set("APP")
                }
            }
        }
    }

    automation {
        common {
            // Enable at least one automation action
            tagPattern.set("v%1\$s")

            // Select the registered project
            targetAccount("main") { projectNames("app") }
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
            account('main') {
                apiTokenFile.set(file('clickup-token.txt'))
                // Register the project(s), their workspaces and task-id prefixes here
                project('app') {
                    workspaceName.set('Your Workspace')
                    taskIdPrefix.set('APP')
                }
            }
        }
    }

    automation {
        common {
            // Enable at least one automation action
            tagPattern.set('v%1$s')

            // Select the registered project
            targetAccount('main') { projectNames('app') }
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
            account("main") {
                apiTokenFile.set(file("clickup-token.txt"))
                project("app") {
                    workspaceName.set("Your Workspace")
                    taskIdPrefix.set("APP")
                }
            }
        }
    }

    automation {
        common {
            // Automation-level defaults applied to every selected project (each project can override)
            // Patterns use String.format(pattern, buildVersion, buildNumber, buildVariant)
            // Example: v1.2.3 / 42 / release
            tagPattern.set("v%1\$s")

            // Fix version automation requires BOTH properties below
            fixVersionPattern.set("v%1\$s")
            fixVersionFieldName.set("Fix version")

            targetAccount("main") {
                // Select the registered project by its local name; no override needed here
                projectNames("app")
            }
        }

        buildVariant("release") {
            targetAccount("main") {
                // Select AND override the project's patterns for this variant
                project("app") {
                    tagPattern.set("release-%1\$s")
                    fixVersionPattern.set("v%1\$s")
                    fixVersionFieldName.set("Fix version")
                }
            }
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishClickUp {
    auth {
        common {
            account('main') {
                apiTokenFile.set(file('clickup-token.txt'))
                project('app') {
                    workspaceName.set('Your Workspace')
                    taskIdPrefix.set('APP')
                }
            }
        }
    }

    automation {
        common {
            // Automation-level defaults applied to every selected project (each project can override)
            // format args order: buildVersion, buildNumber, buildVariant
            tagPattern.set('v%1$s')

            // Fix version automation requires BOTH properties below
            fixVersionPattern.set('v%1$s')
            fixVersionFieldName.set('Fix version')

            targetAccount('main') {
                // Select the registered project by its local name; no override needed here
                projectNames('app')
            }
        }

        buildVariant('release') {
            targetAccount('main') {
                // Select AND override the project's patterns for this variant
                project('app') {
                    tagPattern.set('release-%1$s')
                    fixVersionPattern.set('v%1$s')
                    fixVersionFieldName.set('Fix version')
                }
            }
        }
    }
}
```

#### Multiple ClickUp accounts and projects

A single automation rule can target several ClickUp projects at once — including projects that live on
**different ClickUp accounts** with different API tokens. Declare each account with `account("name")`
inside `auth { common { } }`, and register that account's projects directly inside it with
`project("localName") { workspaceName.set("…"); taskIdPrefix.set("…") }` (Telegram-style, no wrapper).
ClickUp has no self-hosted host, so an account carries only its `apiTokenFile` (no base URL); multiple
accounts simply mean multiple ClickUp API tokens. `taskIdPrefix` must be **globally unique** across all
accounts (prefix routing depends on it). Automation then selects those registered projects two-level,
via `targetAccount("accountName") { … }`:

- inside a `targetAccount` block, list registry projects with `projectNames("app", "other")`, or
- use `project("app") { … }` to select **and** override that project's patterns.

Automation-level `tagPattern` / `fixVersionPattern` / `fixVersionFieldName` are defaults applied to
every selected project and can be overridden per project.

```kotlin
buildPublishClickUp {
    auth {
        common {
            account("main") {
                apiTokenFile.set(file("clickup-token.txt"))
                project("app") {
                    workspaceName.set("Your Workspace")
                    taskIdPrefix.set("APP")
                }
            }
            // A second ClickUp account (named "vendor") with its own token and project registry
            account("vendor") {
                apiTokenFile.set(file("clickup-vendor-token.txt"))
                project("vendor") {
                    workspaceName.set("Vendor Workspace")
                    taskIdPrefix.set("VEN")
                }
            }
        }
    }

    automation {
        common {
            // Defaults applied to every selected project below
            fixVersionPattern.set("v%1\$s")
            fixVersionFieldName.set("Fix version")

            targetAccount("main") {
                projectNames("app")
            }
            targetAccount("vendor") {
                // Override the shared default tag pattern just for this project
                project("vendor") {
                    tagPattern.set("vendor-%1\$s")
                }
            }
        }
    }
}
```

Behavior:

- **Routing by task-id prefix** — each changelog reference with a known custom-task-id prefix
  (`APP-123`, `VEN-42`) is routed to the registry project whose `taskIdPrefix` matches, and its
  account, token and workspace are used. References that match no selected project are logged and
  skipped.
- **Define one `issueSources` source per task-id prefix** — issue extraction only sees ids matched by
  the foundation `changelog { … issueSources { … } }` patterns. For the example above, add a source
  for each prefix so both `APP-…` and `VEN-…` ids are extracted:
  ```kotlin
  changelog {
      common {
          issueSources {
              issueSource("app") {
                  numberPattern.set("APP-\\d+")
              }
              issueSource("vendor") {
                  numberPattern.set("VEN-\\d+")
              }
          }
      }
  }
  ```
  A project whose prefix is absent from every source pattern matches no references and is logged at
  info level.
- **Where config lives** — `workspaceName` and `taskIdPrefix` live only in the `auth … account`
  project registry. Automation selects projects by their local name and supplies the optional
  `tagPattern`, `fixVersionPattern` and `fixVersionFieldName` (as automation-level defaults or
  per-project overrides).
- **Fail-fast validation** — referencing an unknown account name or an unknown project name fails the
  build at configuration time, as does declaring two projects with the same `taskIdPrefix`.

#### Resolving changelog issue references (titles)

Independently of automation, the ClickUp plugin can act as a **resolver** for the foundation
`issueReferences { }` markers (`CLOSES:` / `FIXES:` — see the [Foundation plugin](./foundation.md)).
When enabled it turns ClickUp task references into task **names** (titles) and the foundation
changelog is enriched, so the resolved titles then appear in every distributed changelog
(Slack / Telegram / Nextcloud). It reuses the existing `auth { }` credentials.

This is opt-in: **declaring** the `issueResolution { }` block enables it. It selects which
accounts/projects to READ from via `fromAccount("accountName") { projectNames(…) }`, reusing the same
`auth` tokens:

```kotlin
buildPublishClickUp {
    // auth { … } as above, registering account "main" with project "app" (prefix APP)

    issueResolution {
        common {
            fromAccount("main") {
                projectNames("app")
            }
        }
    }
}
```

```groovy
buildPublishClickUp {
    // auth { … } as above, registering account "main" with project "app" (prefix APP)

    issueResolution {
        common {
            fromAccount('main') {
                projectNames('app')
            }
        }
    }
}
```

Behavior:

- **Prefixed references are routed by their prefix** — `APP-123` routes by its `taskIdPrefix` to the
  owning account + workspace and is resolved as a ClickUp **custom task id**
  (`?custom_task_ids=true&team_id=<id>`), regardless of how many projects are selected.
- **References without a known prefix are treated as native ids** — a reference whose prefix is not
  registered is attempted against the selected accounts as a native ClickUp task id.
- **Non-blocking** — an unresolved reference never fails the build; how it renders is controlled by
  the foundation `unresolvedIssueStrategy { }`.

Each provider handles only the references it recognises, so Jira and ClickUp resolvers can be declared
together without clashing.

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The ClickUp plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth configuration is required**
  - At least `auth.common { ... }` (internal common name is `default`) with one `account("name") { … }`
    registering one `project("name") { … }` must be configured.

- **Automation configuration is required per variant**
  - If there is no matching `automation.common { ... }` or `automation.buildVariant("<name>") { ... }`, configuration fails.

- **The task is created only if at least one automation action is enabled**
  - `clickUpAutomation<Variant>` is registered only if:
    - `tagPattern` is set, or
    - both `fixVersionPattern` and `fixVersionFieldName` are set.

- **Fix version settings must be provided together**
  - If you set only one of `fixVersionPattern` / `fixVersionFieldName`, configuration fails.

- **Task IDs are extracted from the changelog**
  - The task scans the generated changelog file using the union of every foundation
    `issueSources` `numberPattern` (ClickUp uses only the patterns, never `urlPrefix`), then routes
    each id to a project by its `taskIdPrefix`.
  - If no references are found at all, the task logs an info message and does nothing. If a configured
    project matches no references (its prefix is absent from all `issueSources` patterns), the task
    logs an info-level message for that project and skips it.

##### Auth (`buildPublishClickUp { auth { common { account("name") { ... } } } }`)

An `auth` block holds one or more named **accounts**, each an API token + a project registry, declared
with `account("name") { … }`. ClickUp has no self-hosted host, so an account carries only its
`apiTokenFile` (no base URL); multiple accounts mean multiple ClickUp API tokens. Every project a build
touches (for automation or issue resolution) must be registered here, directly inside the account it
lives on, via `project("localName") { workspaceName.set("…"); taskIdPrefix.set("…") }` (Telegram-style,
no wrapper block).

Properties (applies to each `account`, i.e. each `ClickUpAccountConfig`):

- **`apiTokenFile`** *(required)*
  - **What it does**: File containing this account's ClickUp API token.
  - **Why you need it**: Used to authenticate ClickUp API requests for this account.
  - **Where to get it**: ClickUp UI `Profile picture (bottom-left) -> Apps -> Developer API -> Generate`.
  - **Notes**: Keep it out of VCS.

- **`project("localName") { workspaceName.set("…"); taskIdPrefix.set("…") }` on each account** *(required, at least one across all accounts)*
  - **What it does**: Registers a project directly on the account (no wrapper block). Each project has
    a **local name** (used to reference it from `automation` / `issueResolution`), a `workspaceName`
    and a `taskIdPrefix`.
  - **`workspaceName`** *(required)*: ClickUp workspace (team) name, shown in ClickUp’s workspace
    switcher (top-left in the UI). Used to resolve custom-field ids and the `team_id` for custom-task-id
    lookups.
  - **`taskIdPrefix`** *(required)*: The ClickUp custom-task-id prefix (e.g. `APP` for `APP-123`).
    Changelog references whose id starts with this prefix are routed to this project and resolved as
    custom task ids. Must be **globally unique** across all accounts — duplicates fail the build at
    configuration time.

##### Automation (`buildPublishClickUp { automation { ... } }`)

Each automation config (`common` / `buildVariant(...)`) selects registered projects two-level, via
`targetAccount("accountName") { … }`. Inside a `targetAccount` block you either list registry projects
with `projectNames("app", "other")`, or use `project("app") { … }` to select **and** override that
project's patterns. There must be at least one selected project — see
[Multiple ClickUp accounts and projects](#multiple-clickup-accounts-and-projects).

Automation-level `tagPattern` / `fixVersionPattern` / `fixVersionFieldName` are **defaults** applied to
every selected project; a per-project `project("name") { … }` override wins for that project.

- **`targetAccount("accountName") { ... }`** *(required, at least one)*
  - **What it does**: Selects an `auth` account to automate, then chooses its registered projects.
  - **`projectNames("app", "other")`**: selects registry projects by local name (using the
    automation-level default patterns).
  - **`project("app") { ... }`**: selects a registry project by local name and overrides its patterns.
  - Referencing an unknown account name or project name fails the build at configuration time.

Per-project / automation-level pattern properties:

- **`tagPattern`** *(optional)*
  - **What it does**: Adds a tag to each ClickUp task referenced in the changelog.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `v%1$s`, `release-%1$s`.

- **`fixVersionPattern`** *(optional, requires `fixVersionFieldName`)*
  - **What it does**: Computes a fix version value for each task.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `v%1$s`.

- **`fixVersionFieldName`** *(optional, requires `fixVersionPattern`)*
  - **What it does**: Name of the ClickUp custom field where fix version will be written.
  - **Example**: `Fix version`.
  - **Where to get it**: The exact custom field name configured in your ClickUp space/list (Custom Fields settings). The plugin matches by name.

##### Issue resolution (`buildPublishClickUp { issueResolution { ... } }`)

Opt-in block that lets ClickUp resolve the foundation `issueReferences` markers (`CLOSES:` / `FIXES:`)
to task **names**. See [Resolving changelog issue references (titles)](#resolving-changelog-issue-references-titles).

Properties (per `common` / `buildVariant(...)`):

- **`fromAccount("accountName") { projectNames(...) }`** *(required to enable resolution)*
  - **What it does**: Selects which registered account/projects to READ tasks from, reusing the same
    `auth` tokens. Declaring the block enables the ClickUp resolver.
  - **Routing**: a reference with a known `taskIdPrefix` (`APP-123`) is routed by prefix to its
    account + workspace and resolved as a ClickUp custom task id; a reference with no known prefix is
    attempted against the selected accounts as a native ClickUp id.

##### Task options (`clickUpAutomation<Variant>`)

The task supports overriding inputs via CLI options:

- `--accountName=main` — selects the account to use (defaults to the `common` / first declared account)
- `--workspaceName=Your Workspace`
- `--buildTagSnapshotFile=/abs/path/to/tag.json`
- `--changelogFile=/abs/path/to/changelog.md`
- `--issuePattern=APP-\\d+` — repeatable; pass once per task-id pattern to extract
- `--fixVersionPattern=v%1$s`
- `--fixVersionFieldName=Fix version`
- `--tagPattern=release-%1$s`

The standalone ClickUp tasks (`addClickUpTag`, `addClickUpFixVersion`) also accept an optional
`--accountName` (defaulting to the `common` / first account); `addClickUpFixVersion` additionally keeps
`--workspaceName` / `--fieldName`.

---
