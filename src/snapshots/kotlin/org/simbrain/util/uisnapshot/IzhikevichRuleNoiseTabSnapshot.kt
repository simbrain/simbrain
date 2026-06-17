package org.simbrain.util.uisnapshot

import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.Component

class IzhikevichRuleNoiseTabSnapshot : UiSnapshotDef {
    override val name = "izhikevich_rule_noise_tab"

    override fun build(): Component = AnnotatedPropertyEditor(IzhikevichRule()).also {
        it.selectTab("Noise")
        it.expandDetailPanels()
    }
}
