[← Documentation](../../README.md)

# Design & CI-friendliness

This plugin suite is designed to be "build friendly" and behave well in CI/CD environments.

- **Lazy configuration (Providers / configuration avoidance)**
  - Most values are modeled via Gradle `Property` / `Provider` APIs and are resolved late.
  - Tasks are registered using Gradle’s configuration avoidance APIs (so they don’t get realized unless needed).

- **Conditional task creation**
  - Many tasks are registered only when the corresponding configuration is present.
  - Examples:
    - Jira: `jiraAutomation<Variant>` is created only if at least one automation action is enabled.
    - ClickUp: `clickUpAutomation<Variant>` is created only if tag/fixVersion automation is enabled.
    - Slack/Telegram: distribution tasks are skipped when destinations are not configured.

- **No network calls at configuration time**
  - Network operations are executed only during task execution (not during Gradle configuration).

- **Worker API for heavy work**
  - Network uploads and message sending are delegated to Gradle Worker API work actions where applicable.
  - This keeps task execution responsive and avoids blocking the main build thread.

- **Shared services for network clients**
  - External integrations (Slack/Telegram/Jira/Confluence/Nextcloud/ClickUp/etc.) use Gradle Shared Build Services.
  - This avoids re-creating HTTP clients for each task and improves stability/throughput.

- **Variant-aware wiring via Android Components**
  - Tasks are wired per Android build variant, with a predictable naming scheme and clear dependencies.

