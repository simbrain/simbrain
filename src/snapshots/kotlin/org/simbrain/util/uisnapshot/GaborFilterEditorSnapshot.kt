package org.simbrain.util.uisnapshot

import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.imageworld.filters.GaborFilter
import java.awt.Component

class GaborFilterEditorSnapshot : UiSnapshotDef {
    override val name = "gabor_filter_editor"

    override fun build(): Component = AnnotatedPropertyEditor(GaborFilter())
}
