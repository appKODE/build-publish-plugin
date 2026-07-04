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
2. Configure at least one automation feature (label, fix version, or status transition)

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
            }
        }
    }

    automation {
        common {
            // Single project: no surrounding projects { } block needed
            project("main") {
                projectKey.set("PROJ")

                // Enable at least one automation action
                targetStatusName.set("Ready for QA")
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
            }
        }
    }

    automation {
        common {
            // Single project: no surrounding projects { } block needed
            project('main') {
                projectKey.set('PROJ')

                // Enable at least one automation action
                targetStatusName.set('Ready for QA')
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
            }
        }
    }

    automation {
        common {
            projects {
                project("main") {
                    projectKey.set("PROJ")

                    // Label / fix version patterns use String.format(...)
                    // format args order: buildVersion, buildNumber, buildVariant
                    labelPattern.set("android-%s-%d")
                    fixVersionPattern.set("%s")

                    targetStatusName.set("Ready for QA")
                }
            }
        }

        buildVariant("release") {
            projects {
                project("main") {
                    projectKey.set("PROJ")
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
            }
        }
    }

    automation {
        common {
            projects {
                project('main') {
                    projectKey.set('PROJ')

                    // format args order: buildVersion, buildNumber, buildVariant
                    labelPattern.set('android-%s-%d')
                    fixVersionPattern.set('%s')

                    targetStatusName.set('Ready for QA')
                }
            }
        }

        buildVariant('release') {
            projects {
                project('main') {
                    projectKey.set('PROJ')
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
inside `auth { common { } }`, then list the projects under `automation { … { projects { … } } }`,
pointing each at an instance with `instanceName`. A project without `instanceName` uses the instance named
`default` (or the only instance, when just one is declared):

> For a single project you can drop the `projects { }` wrapper and call `project("name") { … }`
> (or the unnamed `project { … }`) directly inside `common` / `buildVariant(...)`, as in the
> Minimum Setup above. The same shorthand exists for the changelog: `issueSource("name") { … }`
> instead of `issueSources { issueSource("name") { … } }`.

```kotlin
buildPublishJira {
    auth {
        common {
            // Default instance; used by any project that doesn't set instanceName
            instance("default") {
                baseUrl.set("https://your-domain.atlassian.net")
                credentials.username.set("your-email@example.com")
                credentials.password.set(providers.environmentVariable("JIRA_API_TOKEN"))
            }
            // A second Jira instance (named "legacy"); referenced from a project via instanceName
            instance("legacy") {
                baseUrl.set("https://jira.your-company.com")
                credentials.username.set("service-account")
                credentials.password.set(providers.environmentVariable("LEGACY_JIRA_TOKEN"))
            }
        }
    }

    automation {
        common {
            projects {
                // Uses the default instance (no instanceName)
                project("main") {
                    projectKey.set("APP")
                    fixVersionPattern.set("%s")
                    targetStatusName.set("Ready for QA")
                }
                // Uses the "legacy" instance and its own status
                project("legacy") {
                    projectKey.set("LEG")
                    instanceName.set("legacy")
                    fixVersionPattern.set("%s")
                    targetStatusName.set("Done")
                }
            }
        }
    }
}
```

Behavior:

- **Routing by issue-key prefix** — each changelog issue is routed to the project whose `projectKey`
  matches its key prefix (e.g. `LEG-42` → the `LEG` project). The matching project's instance,
  credentials and patterns are used. Issues that match no configured project are logged and skipped.
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
- **Per-project config** — each project declares its own `projectKey` and its own optional
  `labelPattern`, `fixVersionPattern` and `targetStatusName`; there are no automation-level defaults.
- **`instanceName`** — optional; when omitted a project uses the auth configuration matching the build
  variant, falling back to `common`. Referencing an unknown `instanceName` fails the build at
  configuration time, as does declaring two projects with the same `projectKey`.

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

An `auth` block holds one or more named **instances**, each a base URL + credentials, declared with
`instance("name") { … }`. Projects reference an instance by its name via `instanceName`; a project with
no `instanceName` uses the instance named `default`, or the sole instance when only one is declared.

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

##### Automation (`buildPublishJira { automation { ... } }`)

Each automation config (`common` / `buildVariant(...)`) declares one or more target projects via
`projects { project("name") { ... } }`. There must be at least one project — see
[Multiple Jira projects and instances](#multiple-jira-projects-and-instances). For a single project
you can skip the `projects { }` wrapper and call `project("name") { … }` — or the unnamed
`project { … }` — directly inside `common` / `buildVariant(...)`.

Properties (applies to each project inside `projects { }`):

- **`projectKey`** *(required)*
  - **What it does**: Jira project key (e.g. `PROJ`). Changelog issues whose key starts with this
    prefix (case-insensitive) are routed to this project.
  - **Where to get it**: Jira project key shown in the project sidebar / project settings, and also in issue keys/URLs (e.g. `https://.../browse/PROJ-123` → `projectKey = PROJ`).

- **`instanceName`** *(optional)*
  - **What it does**: Names the `auth { }` configuration (Jira instance / credentials) to use for
    this project. When omitted, falls back to the variant-matched auth, then `common`.

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

##### Task options (`jiraAutomation<Variant>`)

The task supports overriding inputs via CLI options:

- `--buildTagSnapshotFile=/abs/path/to/tag.json`
- `--changelogFile=/abs/path/to/changelog.md`
- `--issuePattern=([A-Z]+-\\d+)` — repeatable; pass once per issue-key pattern to extract

---

