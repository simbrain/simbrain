package org.simbrain.util.widgets

import org.simbrain.util.table.MatrixDataWrapper
import org.simbrain.util.table.SimbrainDataViewer
import smile.math.matrix.Matrix
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.JPanel
import kotlin.math.min

/**
 * A parameter widget to edit double array.
 * Moved from ReflectivePropertyEditor.
 */
class MatrixWidget : JPanel() {
    /**
     * The table component
     */
    private val table by lazy {
        SimbrainDataViewer(model, useDefaultToolbarAndMenu = false, useHeaders = false, usePadding = false).also {
            it.table.tableHeader = null
            add(it)
            // TODO: Manually adding space in case a horizontal scrollbar is present.
            //       Need to figure out a way to have the tables pack automatically to correct size.
            minimumSize = Dimension(200, min((model.rowCount + 1) * 17 + 2, 100))
            preferredSize = Dimension(200, min((model.rowCount + 1) * 17 + 2, 100))
        }
    }

    init {
        layout = BorderLayout()
    }

    /**
     * The table model
     */
    private lateinit var model: MatrixDataWrapper


    var values: Matrix
        /**
         * Get values of this widget.
         * @return the values
         */
        get() = Matrix.of(model.get2DDoubleArray())
        /**
         * Set values for this widget and update the model and visual.
         * @param values the values to set
         */
        set(values) {
            model = MatrixDataWrapper(values)
            table.model = model
        }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        table.isEnabled = enabled
        if (!enabled) {
            table.foreground = Color.gray
        }
    }
}
