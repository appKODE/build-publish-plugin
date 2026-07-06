[← Documentation](../../README.md) · [All plugins](./index.md)

# Jira plugin

`ru.kode.android.build-publish-novo.jira`

Update Jira tickets with build information.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `jiraAutomation<Variant>` | Applies Jira automation based on issues found in the changelog | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Run Jira automation for the release variant
./gradlew jiraAutomationRelease
```

#### Minimum Setup

1. Configure Jira credentials (Jira Cloud: use an API token as the password)
2. Register at least one project under an instance (`auth { … instance("name") { project("name") { projectKey } } }`)
3. Configure at least one automation feature (label, fix version, or status transition)

Project keys live only in the `auth` instance registry (like Telegram chats under a bot); automation
then selects registered projects by their **local name** through `targetInstance("instance")`.

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.jira")
}

buildPublishJira {
    auth {
        common {
            instance("default") {
                baseUrl.set("https://your-domain.atlassian.net")
                credentials.username.set("your-email@example.com")
                credentials.password.set(providers.environmentVariable("JIRA_API_TOKEN"))
                // Register the project(s) and their keys here
                project("main") { projectKey.set("PROJ") }
            }
        }
    }

    automation {
        common {
            targetInstance("default") {
                // Select the registered project and enable at least one automation action
                project("main") {
                    targetStatusName.set("Ready for QA")
                }
            }
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
    id 'ru.kode.android.build-publish-novo.jira'
}

buildPublishJira {
    auth {
        common {
            instance('default') {
                baseUrl.set('https://your-domain.atlassian.net')
                credentials.username.set('your-email@example.com')
                credentials.password.set(providers.environmentVariable('JIRA_API_TOKEN'))
                // Register the project(s) and their keys here
                project('main') { projectKey.set('PROJ') }
            }
        }
    }

    automation {
        common {
            targetInstance('default') {
                // Select the registered project and enable at least one automation action
                project('main') {
                    targetStatusName.set('Ready for QA')
                }
            }
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishJira {
    auth {
        common {
            instance("default") {
                baseUrl.set("https://your-domain.atlassian.net")
                credentials.username.set("your-email@example.com")
                credentials.password.set(providers.environmentVariable("JIRA_API_TOKEN"))
                project("main") { projectKey.set("PROJ") }
            }
        }
    }

    automation {
        common {
            // Automation-level defaults applied to every selected project (each project can override)
            // Label / fix version patterns use String.format(...)
            // format args order: buildVersion, buildNumber, buildVariant
            labelPattern.set("android-%s-%d")
            fixVersionPattern.set("%s")
            targetStatusName.set("Ready for QA")

            targetInstance("default") {
                // Select the registered project by its local name; no override needed here
                projectNames("main")
            }
        }

        buildVariant("release") {
            targetInstance("default") {
                // Select AND override the project's patterns for this variant
                project("main") {
                    labelPattern.set("release-%s")
                    fixVersionPattern.set("%s")
                    targetStatusName.set("Ready for Release")
                }
            }
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishJira {
    auth {
        common {
            instance('default') {
                baseUrl.set('https://your-domain.atlassian.net')
                credentials.username.set('your-email@example.com')
                credentials.password.set(providers.environmentVariable('JIRA_API_TOKEN'))
                project('main') { projectKey.set('PROJ') }
            }
        }
    }

    automation {
        common {
            // Automation-level defaults applied to every selected project (each project can override)
            // format args order: buildVersion, buildNumber, buildVariant
            labelPattern.set('android-%s-%d')
            fixVersionPattern.set('%s')
            targetStatusName.set('Ready for QA')

            targetInstance('default') {
                // Select the registered project by its local name; no override needed here
                projectNames('main')
            }
        }

        buildVariant('release') {
            targetInstance('default') {
                // Select AND override the project's patterns for this variant
                project('main') {
                    labelPattern.set('release-%s')
                    fixVersionPattern.set('%s')
                    targetStatusName.set('Ready for Release')
                }
            }
        }
    }
}
```

#### Multiple Jira projects and instances

A single automation rule can target several Jira projects at once — including projects that live on
**different Jira instances** with different credentials. Declare each instance with `instance("name")`
inside `auth { common { } }`, and register that instance's projects directly inside it with
`project("localName") { projectKey.set("KEY") }` (Telegram-style, no wrapper). `projectKey` must be
**globally unique** across all instances (prefix routing depends on it). Automation then selects those registered
projects two-level, via `targetInstance("instanceName") { … }`:

- inside a `targetInstance` block, list registry projects with `projectNames("app", "other")`, or
- use `project("app") { … }` to select **and** override that project's patterns.

Automation-level `labelPattern` / `fixVersionPattern` / `targetStatusName` are defaults applied to
every selected project and can be overridden per project.

> For a single project the changelog shorthand still applies: `issueSource("name") { … }` instead of
> `issueSources { issueSource("name") { … } }`.

```kotlin
buildPublishJira {
    auth {
        common {
            instance("default") {
                baseUrl.set("https://your-domain.atlassian.net")
                credentials.username.set("your-email@example.com")
                credentials.password.set(providers.environmentVariable("JIRA_API_TOKEN"))
                project("app") { projectKey.set("APP") }
            }
            // A second Jira instance (named "legacy") with its own credentials and project registry
            instance("legacy") {
                baseUrl.set("https://jira.your-company.com")
                credentials.username.set("service-account")
                credentials.password.set(providers.environmentVariable("LEGACY_JIRA_TOKEN"))
                project("legacy") { projectKey.set("LEG") }
            }
        }
    }

    automation {
        common {
            // Defaults applied to every selected project below
            fixVersionPattern.set("%s")
            targetStatusName.set("Ready for QA")

            targetInstance("default") {
                projectNames("app")
            }
            targetInstance("legacy") {
                // Override the shared default status just for this project
                project("legacy") {
                    targetStatusName.set("Done")
                }
            }
        }
    }
}
```

Behavior:

- **Routing by issue-key prefix** — each changelog issue is routed to the registry project whose
  `projectKey` matches its key prefix (e.g. `LEG-42` → the `legacy` project on the `legacy` instance).
  The matching project's instance, credentials and patterns are used. Issues that match no selected
  project are logged and skipped.
- **Define one `issueSources` source per project key** — issue extraction only sees keys matched by
  the foundation `changelog { … issueSources { … } }` patterns. For the example above, add a source
  for each key prefix so both `APP-…` and `LEG-…` keys are extracted (and, in Slack/Telegram, linked
  to their respective hosts):
  ```kotlin
  changelog {
      common {
          issueSources {
              issueSource("app") {
                  numberPattern.set("APP-\\d+")
                  urlPrefix.set("https://your-domain.atlassian.net/browse/")
              }
              issueSource("legacy") {
                  numberPattern.set("LEG-\\d+")
                  urlPrefix.set("https://jira.your-company.com/browse/")
              }
          }
      }
  }
  ```
  A project whose key prefix is absent from every source pattern matches no issues and is logged at
  info level.
- **Where config lives** — `projectKey` lives only in the `auth … instance` project registry. Automation
  selects projects by their local name and supplies the optional `labelPattern`, `fixVersionPattern`
  and `targetStatusName` (as automation-level defaults or per-project overrides).
- **Fail-fast validation** — referencing an unknown instance name or an unknown project name fails the
  build at configuration time, as does declaring two projects with the same `projectKey`.

#### Resolving changelog issue references (titles)

Independently of automation, the Jira plugin can act as a **resolver** for the foundation
`issueReferences { }` markers (`CLOSES:` / `FIXES:` — see the
[Foundation plugin](./foundation.md)). When configured it
fetches each referenced issue's **title** and the foundation changelog is enriched accordingly, so the
resolved titles then appear in every distributed changelog (Slack / Telegram / Nextcloud).

This is opt-in: **declaring** the `issueResolution { }` block enables it. It selects which
instances/projects to READ from via `fromInstance("instanceName") { projectNames(…) }`, reusing the same
`auth` credentials:

```kotlin
buildPublishJira {
    // auth { … } as above, registering instance "default" with project "app" (key APP)

    issueResolution {
        common {
            fromInstance("default") {
                projectNames("app")
            }
        }
    }
}
```

```groovy
buildPublishJira {
    // auth { … } as above, registering instance "default" with project "app" (key APP)

    issueResolution {
        common {
            fromInstance('default') {
                projectNames('app')
            }
        }
    }
}
```

Behavior:

- **Bare numbers resolve against the sole selected project's key** — `CLOSES: 3458` resolves to
  `APP-3458` when exactly one project is selected (here key `APP`). If more than one project is
  selected, bare numbers are ambiguous and are left unresolved — use prefixed keys instead.
- **Prefixed keys are always routed by their prefix** — `APP-3458`, `LEG-42`, etc. resolve regardless
  of how many projects are selected.
- **Non-blocking** — an unresolved reference never fails the build; how it renders is controlled by
  the foundation `unresolvedIssueStrategy { }`.

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Jira plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth configuration is required**
  - At least `auth.common { ... }` (internal common name is `default`) must be configured.

- **Automation configuration is required per variant**
  - If there is no matching `automation.common { ... }` or `automation.buildVariant("<name>") { ... }`, configuration fails.

- **The task is created only if at least one automation action is enabled**
  - `jiraAutomation<Variant>` is registered only if at least one of:
    - `labelPattern`
    - `fixVersionPattern`
    - `targetStatusName`
    is set.

- **Issue keys are extracted from the changelog**
  - The task scans the generated changelog file using the union of every foundation
    `issueSources` `numberPattern`, then routes each key to a project by its key prefix.
  - If no issues are found at all, the task logs an info message and does nothing. If a configured
    project matches no keys (its prefix is absent from all `issueSources` patterns), the task logs
    an info-level message for that project and skips it.

##### Auth (`buildPublishJira { auth { common { instance("name") { ... } } } }`)

An `auth` block holds one or more named **instances**, each a base URL + credentials + a project
registry, declared with `instance("name") { … }`. Every project a build touches (for automation or
issue resolution) must be registered here, directly inside the instance it lives on, via
`project("localName") { projectKey.set("KEY") }` (Telegram-style, no wrapper block).

Properties (applies to each `instance`, i.e. each `JiraInstanceConfig`):

- **`baseUrl`** *(required)*
  - **What it does**: Base URL of your Jira instance.
  - **Examples**:
    - `https://your-domain.atlassian.net` (Cloud)
    - `https://jira.your-company.com` (Server/Data Center)

- **`credentials.username`** *(required)*
  - **What it does**: Username/email used for authentication.

- **`credentials.password`** *(required)*
  - **What it does**: Password or API token.
  - **Recommendation**: For Jira Cloud use an API token.
  - **Where to get it (Jira Cloud)**: Atlassian account settings `Security -> API tokens -> Create API token` (use the token as the password).

- **`project("localName") { projectKey.set("KEY") }` on each instance** *(required, at least one across all instances)*
  - **What it does**: Registers a project directly on the instance (no wrapper block). Each project has
    a **local name** (used to reference it from `automation` / `issueResolution`) and a `projectKey`.
  - **`projectKey`** *(required)*: Jira project key (e.g. `PROJ`). Changelog issues whose key starts
    with this prefix (case-insensitive) are routed to this project. Must be **globally unique** across
    all instances — duplicates fail the build at configuration time.
  - **Where to get it**: Jira project key shown in the project sidebar / project settings, and also in issue keys/URLs (e.g. `https://.../browse/PROJ-123` → `projectKey = PROJ`).

##### Automation (`buildPublishJira { automation { ... } }`)

Each automation config (`common` / `buildVariant(...)`) selects registered projects two-level, via
`targetInstance("instanceName") { … }`. Inside a `targetInstance` block you either list registry
projects with `projectNames("app", "other")`, or use `project("app") { … }` to select **and** override
that project's patterns. There must be at least one selected project — see
[Multiple Jira projects and instances](#multiple-jira-projects-and-instances).

Automation-level `labelPattern` / `fixVersionPattern` / `targetStatusName` are **defaults** applied to
every selected project; a per-project `project("name") { … }` override wins for that project.

- **`targetInstance("instanceName") { ... }`** *(required, at least one)*
  - **What it does**: Selects an `auth` instance to automate, then chooses its registered projects.
  - **`projectNames("app", "other")`**: selects registry projects by local name (using the
    automation-level default patterns).
  - **`project("app") { ... }`**: selects a registry project by local name and overrides its patterns.
  - Referencing an unknown instance name or project name fails the build at configuration time.

Per-project / automation-level pattern properties:

- **`labelPattern`** *(optional)*
  - **What it does**: Adds a computed label to each issue found in the changelog.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `android-%s-%d`.

- **`fixVersionPattern`** *(optional)*
  - **What it does**: Sets (and creates if needed) a fix version on each issue.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `%s` (use only version), or `android-%s`.

- **`targetStatusName`** *(optional)*
  - **What it does**: Transitions issues to the given status (by looking up a matching transition).
  - **Example**: `Ready for QA`, `Ready for Release`.
  - **Where to get it**: The exact Jira status name from your project workflow. It must be a valid transition from the issue’s current status (you can verify via the issue UI `Transitions` menu).

##### Issue resolution (`buildPublishJira { issueResolution { ... } }`)

Opt-in block that lets Jira resolve the foundation `issueReferences` markers (`CLOSES:` / `FIXES:`) to
issue **titles**. See [Resolving changelog issue references (titles)](#resolving-changelog-issue-references-titles).

Properties (per `common` / `buildVariant(...)`):

- **`enabled`** *(default: `false`)*
  - **What it does**: Turns the Jira resolver on. When `false` (or the block is absent), Jira does not
    contribute titles.

- **`fromInstance("instanceName") { projectNames(...) }`** *(required when `enabled`)*
  - **What it does**: Selects which registered instance/projects to READ issues from, reusing the same
    `auth` credentials.
  - **Bare-number resolution**: a bare number (`CLOSES: 3458`) resolves against the **sole** selected
    project's key. With more than one project selected, bare numbers are ambiguous and left
    unresolved — use prefixed keys (`APP-3458`, `LEG-42`). Prefixed keys are always routed by prefix.

##### Task options (`jiraAutomation<Variant>`)

The task supports overriding inputs via CLI options:

- `--buildTagSnapshotFile=/abs/path/to/tag.json`
- `--changelogFile=/abs/path/to/changelog.md`
- `--issuePattern=([A-Z]+-\\d+)` — repeatable; pass once per issue-key pattern to extract

---

