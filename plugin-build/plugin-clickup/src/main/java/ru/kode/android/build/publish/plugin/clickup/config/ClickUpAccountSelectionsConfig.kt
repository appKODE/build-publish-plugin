package ru.kode.android.build.publish.plugin.clickup.config

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * Container of per-account project selections ([ClickUpAccountSelectionConfig]), shared by `automation`
 * (`targetAccount`) and `issueResolution` (`fromAccount`). Each selection is keyed by its account name;
 * repeated selections of the same account are merged.
 */
abstract class ClickUpAccountSelectionsConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : CommonConfigMergeable<ClickUpAccountSelectionsConfig> {
        internal val selections: NamedDomainObjectContainer<ClickUpAccountSelectionConfig> =
            objects.domainObjectContainer(ClickUpAccountSelectionConfig::class.java)

        /**
         * Selects (or extends the selection of) the account named [accountName].
         */
        internal fun select(
            accountName: String,
            action: Action<ClickUpAccountSelectionConfig>,
        ) {
            if (selections.findByName(accountName) == null) {
                selections.register(accountName, action)
            } else {
                action.execute(selections.getByName(accountName))
            }
        }

        override fun inheritFrom(common: ClickUpAccountSelectionsConfig) {
            selections.inheritNamedFrom(common.selections)
        }
    }
