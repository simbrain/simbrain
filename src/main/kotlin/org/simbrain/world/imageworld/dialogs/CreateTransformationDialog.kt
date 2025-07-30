package org.simbrain.world.imageworld.dialogs

import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.imageworld.filters.IdentityOp
import org.simbrain.world.imageworld.filters.ImageTransformation
import org.simbrain.world.imageworld.transformations.TransformationCollection
import javax.swing.JPanel

/**
 * Dialog for creating new transformations.
 * Renamed from CreateFilterDialog to better distinguish from the new multi-filter system.
 */
class CreateTransformationDialog(private val transformationCollection: TransformationCollection) : StandardDialog() {

    init {
        title = "Create Transformation"
        setContentPane(createTransformationPanel())
        pack()
    }

    private fun createTransformationPanel(): JPanel {
        val panel = JPanel()

        // Create a template transformation
        val templateTransformation = ImageTransformation(
            "Transformation ${transformationCollection.transformations.size + 1}",
            transformationCollection.imageSource,
            IdentityOp(),
            100,
            100
        )

        val editor = AnnotatedPropertyEditor(templateTransformation)
        panel.add(editor)

        addCommitTask {
            templateTransformation.applyFilter()
            transformationCollection.addTransformation(templateTransformation)
            transformationCollection.setCurrentTransformation(templateTransformation)
        }

        return panel
    }
}