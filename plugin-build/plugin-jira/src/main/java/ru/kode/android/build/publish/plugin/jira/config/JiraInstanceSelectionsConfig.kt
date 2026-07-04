package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * Container of per-instance project selections ([JiraInstanceSelectionConfig]), shared by `automation`
 * (`targetInstance`) and `issueResolution` (`fromInstance`). Each selection is keyed by its instance
 * name; repeated selections of the same instance are merged.
 */
abstract class JiraInstanceSelectionsConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : CommonConfigMergeable<JiraInstanceSelectionsConfig> {
        internal val selections: NamedDomainObjectContainer<JiraInstanceSelectionConfig> =
            objects.domainObjectContainer(JiraInstanceSelectionConfig::class.java)

        /**
         * Selects (or extends the selection of) the instance named [instanceName].
         */
        internal fun select(
            instanceName: String,
            action: Action<JiraInstanceSelectionConfig>,
        ) {
            if (selections.findByName(instanceName) == null) {
                selections.register(instanceName, action)
            } else {
                action.execute(selections.getByName(instanceName))
            }
        }

        override fun inheritFrom(common: JiraInstanceSelectionsConfig) {
            selections.inheritNamedFrom(common.selections)
        }
    }
