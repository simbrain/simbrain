package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.gui.trainer.TrainerGuiActions
import org.simbrain.util.Events2
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.table.NumericTable
import org.simbrain.util.table.SimbrainJTable
import org.simbrain.util.table.SimbrainJTableScrollPanel
import org.simbrain.util.table.TableActionManager
import smile.classification.Classifier
import smile.classification.SVM
import smile.math.kernel.PolynomialKernel
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToolBar
import javax.swing.event.TableModelEvent

class DataPanel: JPanel() {

    init {
        layout = BorderLayout()
        // preferredSize = Dimension(250, 200)
    }

    private val toolbars = JToolBar().also {
        add(it)
        add("North", it)
    }

    val table: NumericTable = NumericTable().apply {
        addTableModelListener { event ->
            when (event.type) {
                TableModelEvent.INSERT -> events.insertRow.fireAndForget(event.firstRow)
            }
        }
    }

    val jTable = SimbrainJTable.createTable(table)

    private val scrollPanel = SimbrainJTableScrollPanel(jTable)
        .also { add("Center", it) }

    private val fileToolBar = JToolBar().apply {
        add(TrainerGuiActions.getOpenCSVAction(jTable, null))
        add(TableActionManager.getSaveCSVAction(table))
        toolbars.add(this)
    }

    // private val editToolBar = JToolBar().apply {
    //     add(TableActionManager.getInsertRowAction(jTable))
    //     add(TableActionManager.getDeleteRowAction(jTable))
    //     toolbars.add(this)
    // }

    private val numericEditToolbar = JToolBar().apply {
        add(TableActionManager.getRandomizeAction(jTable))
        add(TableActionManager.getNormalizeAction(jTable))
        add(TableActionManager.getZeroFillAction(jTable))
        add(TableActionManager.getFillAction(jTable))
        toolbars.add(this)
    }

    val events = DataPanelEvents2()

    fun applyData() {
        val data = sequence {
            for (i in 0 until table.rowCount) {
                yield(
                    sequence {
                        for (j in 0 until table.logicalColumnCount) {
                            yield(table.getLogicalValueAt(i, j))
                        }
                    }.toList().toDoubleArray()
                )
            }
        }.toList().toTypedArray()

        events.updateData.fireAndForget(data)
    }

}

/**
 * See [Events2].
 */
class DataPanelEvents2: Events2() {
    val updateData = AddedEvent<Array<DoubleArray>>()
    val insertRow = AddedEvent<Int>()
}

fun main() {

    fun <T> consumeClassifier(classifier: Classifier<T>) {
        println(classifier)
    }

    StandardDialog().apply {
        val mainPanel = JPanel().apply {
            contentPane = this
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        }

        val inputPanel = DataPanel().apply {
            table.setData(arrayOf(
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(0.0, 1.0),
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(1.0, 1.0)
            ))
            events.updateData.on { data -> println(data.contentDeepToString()) }
            addClosingTask { applyData() }
            mainPanel.add(this)
        }

        val targetPanel = DataPanel().apply {
            table.setData(arrayOf(
                doubleArrayOf(-1.0),
                doubleArrayOf(1.0),
                doubleArrayOf(1.0),
                doubleArrayOf(-1.0)
            ))
            addClosingTask { applyData() }
            mainPanel.add(this)
        }

        var input: Array<DoubleArray>? = null
        var target: IntArray? = null

        fun invokeCallback(input: Array<DoubleArray>?, target: IntArray?) {
            if (input != null && target != null) {
                val kernel = PolynomialKernel(2)
                consumeClassifier(SVM.fit(input, target, kernel, 1000.0, 1E-3))
            }
        }

        inputPanel.events.updateData.on { data ->
            input = data
            invokeCallback(input, target)
        }

        targetPanel.events.updateData.on { data ->
            target = data.map { it[0].toInt() }.toIntArray()
            invokeCallback(input, target)
        }


    }.run { makeVisible() }


}

class InputTargetDataPanel(): JPanel() {

    val inputs = DataPanel()
    val targets = DataPanel()

    val addRemoveRows = JToolBar().apply {
        // Add row
        add(JButton().apply {
            icon = ResourceManager.getImageIcon("menu_icons/AddTableRow.png")
            toolTipText = "Insert a row"
            addActionListener {
                inputs.table.insertRow(inputs.jTable.selectedRow)
                targets.table.insertRow(inputs.jTable.selectedRow)
            }
        })

        // Delete row
        // TODO: Delete selected rows. For that abstract out table code
        add(JButton().apply {
            icon = ResourceManager.getImageIcon("menu_icons/DeleteRowTable.png")
            toolTipText = "Delete last row"
            addActionListener {
                inputs.table.removeRow(inputs.jTable.rowCount - 1)
                targets.table.removeRow(targets.jTable.rowCount - 1)
            }
        })
    }

    init {
        layout = MigLayout()
        add(addRemoveRows, "wrap")

        // Add the data panels
        add(inputs, "growx")
        add(targets, "growx")
    }

}