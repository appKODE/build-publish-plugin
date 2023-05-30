# Changelog

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
