# Changelog

## 🚨 Important: Project lineage change

Starting with **build-publish-novo**, this project introduces a **new package name**, **new internal architecture**, and **independent versioning**.

- `build-publish-novo` is **not backward-compatible** with the legacy `build-publish` plugin
- Version numbers **do not continue** the legacy sequence
- Legacy versions are preserved below **for reference only**

---

## 🚀 build-publish-novo (new lineage)

### 1.2.1
- Add Telegram and Confluence distribution compression option, by default it is disabled

### 1.2.0
**Breaking changes**
- Introduce modular `build-publish-novo-*` plugin suite (apply only required integrations)
- Extract shared logic into `ru.kode.android:build-publish-novo-core`
- Remove AppCenter integration (delete related config/tasks and migrate to another distribution channel)
- Rename `getLastTag<Variant>` → `getLastTagSnapshot<Variant>`
- Default `versionName` is now derived from tag build version only (`1.2` / `1.2.3`);
  to include build number, configure `BuildVersionNumberNameStrategy`

**Features**
- Variant-aware tag snapshot with validation (positive build numbers, monotonic per variant stream)
- Changelog generation based on previous tag on a different commit (supports CI re-tags without empty changelogs)
- Telegram lookup task to discover `chatId` / `topicId`
- Support custom Telegram Bot API server

**Internal**
- Expanded plugin-test coverage
- Improved CI secret handling patterns (including base64-decoded secret files)

---

## 📦 build-publish-novo-core

### 1.2.0
- Shared core library used by all `novo` plugins
- Tag snapshot model and tag-range utilities for changelog generation
- Version strategies (`VersionNameStrategy`, `VersionCodeStrategy`)
- Tag pattern builder utilities

---

## 🗃 Legacy build-publish (deprecated)

> ⚠️ The versions below belong to the **legacy package name and versioning scheme**.  
> This plugin is deprecated and no longer developed.

### 1.3.0-alpha18
- Fix logic to set Telegram distribution properties

### 1.3.0-alpha17
- Fix logic to provide correct Telegram auth credentials

### 1.3.0-alpha16
- Add custom host for Telegram bot with basic auth
- Add task to publish bundle for Telegram distribution

### 1.3.0-alpha15
- Ignore proxy settings according to `nonProxyHosts`

### 1.3.0-alpha14
- Configure OkHttp proxy settings for authorization
- Disable caching by default for tag-related tasks

### 1.3.0-alpha13
- Update Slack upload logic to new Slack API flow

### 1.3.0-alpha12
- Revert logic for `versionCode` and `versionName` providers

### 1.3.0-alpha11
- Add ability to disable default configurations

### 1.3.0-alpha10
- Disable fallback for `versionCode` and `versionName`

### 1.3.0-alpha09
- Upload build as Confluence comment

### 1.3.0-alpha08
- Send Telegram changelog in chunks
- Fix Telegram entity escaping issues

### 1.3.0-alpha07
- Add max character limit for AppCenter notes
- Split large changelog attachments

### 1.3.0-alpha06
- Upload builds to Telegram via `telegramDistributionUpload*` tasks

### 1.3.0-alpha05
- Automate ClickUp workflows via `clickUpAutomation` tasks

### 1.3.0-alpha04
- Fix omitted Google Play track edit commit

### 1.3.0-alpha03
- Implement Google Play upload via `playUpload*` tasks

### 1.3.0-alpha02
- Update AGP to 8.3.1 and Gradle to 8.7

### 1.3.0-alpha01
- Experimental configurable build tag pattern (single `-` delimiter restriction)

### 1.2.0
- Internalize Firebase App Distribution plugin
- Add `useVersionsFromTag` option
- Restructure plugin (build conventions, version catalog)
- Update Kotlin to 1.9.22 and dependencies

### 1.1.0-alpha21
- Hide Telegram webhook URL inside plugin
- Refactor Slack attachment handling

### 1.1.0-alpha20
- Optional Telegram topic ID support

### 1.1.0-alpha19
- Ellipsize Slack changelog
- Promote debug/warn logs to info

### 1.1.0-alpha18
- Sort tags by build number to detect last tag correctly

### 1.1.0-alpha17
- Fix AppCenter changelog descriptions
- Update Gradle and Kotlin versions
- Fix Telegram hyperlink attachments

### 1.1.0-alpha16
- Replace shell scripts with Retrofit + OkHttp

### 1.1.0-alpha15
- Migrate git commands to `grgit`
- Improve AppCenter app name configuration
- Refactor AppCenter upload status checks

### 1.1.0-alpha14
- AppCenter distribution task
- Jira automation (status, labels, fix version)
- Refactor configuration into domain-specific blocks
- Optimize tag-based versioning and changelog generation

### 1.0.5-hotfix01 – 2022-06-01
- Revert fix from 1.0.5

### 1.0.5 – 2022-05-30
- Fix last-tag detection logic

### 1.0.4 – 2022-05-26
- Add dimensions support for complex task generation

### 1.0.3 – 2022-05-20
- Override App ID for Firebase App Distribution

### 1.0.2 – 2022-05-16
- Add `PrintLastIncreasedTag` task

### 1.0.1 – 2022-05-06
- Configure distribution artifact type (APK / AAB)
