package org.simbrain.util.uisnapshot

import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.Component

class IzhikevichRuleEditorSnapshot : UiSnapshotDef {
    override val name = "izhikevich_rule_editor"
    override fun build(): Component = AnnotatedPropertyEditor(IzhikevichRule())
}
