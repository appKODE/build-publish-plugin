[← Documentation](../README.md)

# Reusable client libraries & standalone tasks

Each integration's network logic is published as a standalone library on Maven Central, on top of
`ru.kode.android:build-publish-novo-core`:

| Library | Integration |
| --- | --- |
| `ru.kode.android:build-publish-novo-client-slack` | Slack |
| `ru.kode.android:build-publish-novo-client-telegram` | Telegram |
| `ru.kode.android:build-publish-novo-client-nextcloud` | Nextcloud |
| `ru.kode.android:build-publish-novo-client-jira` | Jira |
| `ru.kode.android:build-publish-novo-client-confluence` | Confluence |
| `ru.kode.android:build-publish-novo-client-clickup` | ClickUp |

You can depend on a client directly (outside Gradle plugins) via its `*ControllerFactory`, or use
the CLI-invokable **standalone tasks** the plugins register. Every input is a `--option`:

```bash
# Provided by the integration plugins (applied to an Android module):
./gradlew addClickUpTag --tag release --taskIds abc123,def456
./gradlew addClickUpFixVersion --version 1.2.3 --taskIds abc123 --workspaceName "My WS" --fieldName "Fix Version"
./gradlew uploadToConfluence --file build/outputs/app.apk --pageId 12345
./gradlew addConfluenceComment --pageId 12345 --fileName app.apk
./gradlew addJiraFixVersion --version 1.2.3 --projectKey PROJ --issueNumbers PROJ-1,PROJ-2
./gradlew addJiraLabel --label released --issueNumbers PROJ-1,PROJ-2
./gradlew transitionJiraIssue --transitionName Done --projectKey PROJ --issueNumbers PROJ-1
./gradlew uploadToNextcloud --file build/outputs/app.apk --remotePath builds
./gradlew sendSlackMessage --message "Build is ready"
./gradlew sendSlackFile --file build/outputs/app.apk --channels C123,C456
./gradlew sendTelegramMessage --botName ci --chatName releases --message "Build is ready"
./gradlew sendTelegramFile --botName ci --chatName releases --file build/outputs/app.apk
```

> `transitionJiraIssue` uppercases the `--projectKey` and fails fast if the requested transition
> does not exist for the project.
>
> When more than one Jira instance is configured (see [Multiple Jira projects and
> instances](plugins/jira.md#multiple-jira-projects-and-instances)), the Jira standalone tasks accept an optional
> `--instanceName` to pick the instance, e.g.
> `./gradlew addJiraFixVersion --instanceName legacy --version 1.2.3 --projectKey LEG --issueNumbers LEG-1`.
> Without it, the common (`default`) instance is used.

The **`ru.kode.android.build-publish-novo.sender`** plugin (`plugin-sender`) is an Android-free
aggregator that exposes all of the tasks above from a single `buildPublishSender { … }` extension —
useful for non-Android or standalone automation projects.

