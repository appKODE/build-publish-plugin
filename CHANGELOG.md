# Changelog

## 1.3.0-alpha13
* Update slack upload logic with new Slack API upload flow

## 1.3.0-alpha12
* Revert logic to set `versionCode` and `versionName` providers

## 1.3.0-alpha11
* Add logic to disable default configurations

## 1.3.0-alpha10
* Add logic to disable fallback for `versionCode` and `versionName`

## 1.3.0-alpha09
* Add logic to upload build as Confluence comment

## 1.3.0-alpha08
* Add logic to send Telegram changelog by chunks
* Fix logic to add links in Telegram changelog to avoid "Upload error, code=400, reason={"ok":false,"error_code":400,"description":"Bad Request: can't parse entities: Character ')' is reserved and must be escaped with the preceding '\\'"}"

## 1.3.0-alpha07
* Add a maximum character limit for notes app center
* Add split attachments into parts when sending large changelog file to avoid "Upload error, code=403, reason=invalid_token"

## 1.3.0-alpha06
* Add ability to upload builds into Telegram via telegramDistributionUpload* tasks

## 1.3.0-alpha05
* Add task to automate ClickUp workflows via clickUpAutomation tasks

## 1.3.0-alpha04
* Fix commit of Google Play track edit being omitted

## 1.3.0-alpha03
* Implement Google Play uploading logic via playUpload* tasks

## 1.3.0-alpha02
* Update apg to 8.3.1 and gradle to 8.7, and other libs

## 1.3.0-alpha01
* Add experimental logic to configure build tag pattern (buildTagPattern). But now it has restrictions: for example, in tag name should be only one - delimiter

## 1.2.0
* Hide firebase app distribution plugin inside, and add logic to apply it only if required
* Add `useVersionsFromTag` property inside OutputConfig to disable logic when versions and file names is applied from tag   
* Change plugin structure (add build conventions, migrate from buildSrc to version catalog)
* Update Kotlin to 1.9.22 and all dependencies to last versions

## 1.1.0-alpha21
* Remove Telegram webhookUrl from TelegramConfig to hide it inside plugin
* Refactor Slack send request on attachment to correctly send long changelog 

## 1.1.0-alpha20
* Add optional topic id for Telegram webhook

## 1.1.0-alpha19
* Add logic to ellipsize slack changelog
* Replace debug and warn logs to info

## 1.1.0-alpha18
* Add sorting by build number to correctly detekt last tag

## 1.1.0-alpha17
* Fix description of changes for AppCenter in the changelog
* Update to new gradle version: gradle plugin: 1.7.4, gradle: 7.5, kotlin: 1.8.10
* Fix logic to attach hypertexts for Telegram

## 1.1.0-alpha16
* Remove sh scripts to post webhooks, replace it with retrofit and okhttp

## 1.1.0-alpha15
* Port git commands execution to [grgit](https://github.com/ajoberstar/grgit) plugin
* Change app name configuration for AppCenter. Now the full name of the application is passed to the configuration
* Refactor AppCenter upload status checking

## 1.1.0-alpha14
* Add logic to publish into AppCenter via AppCenterDistributionTask
* Add logic to automate Jira: change task status, add label and fix version
* Refactor configuration logic: split one big config to small configs for different areas
* Add optimization to get version from tag and generate changelog

## 1.0.5-hotfix01 - 2022-06-01

* Revert fix from 1.0.5 version

## 1.0.5 - 2022-05-30

* Fix logic to get last tag: now it takes without sorting by build number

## 1.0.4 - 2022-05-26

* Added dimensions support to generate complex tasks 

## 1.0.3 - 2022-05-20

* Added `distributionAppId` to override app id for Firebase App Distribution

## 1.0.2 - 2022-05-16

* Added `PrintLastIncreasedTag` task to print last tag with incremented build version 

## 1.0.1 - 2022-05-06

* Added `distributionArtifactType` to set APK or AAB type of artifact
