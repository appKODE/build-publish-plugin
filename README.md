# firebase-publish-plugin

A configurable plugin to generate changelog from tags and publish results into Firebase App Distribution

### `preMerge` task

A `preMerge` task on the top level build is already provided in the plugin.
This allows you to run all the `check` tasks both in the top level and in the included build.

You can easily invoke it with:

```
./gradlew preMerge
```

If you need to invoke a task inside the included build with:

```
./gradlew -p plugin-build <task-name>
```