package org.simbrain.network.gui.dialogs

import org.simbrain.network.gui.trainer.TrainerGuiActions
import org.simbrain.util.Event
import org.simbrain.util.StandardDialog
import org.simbrain.util.table.NumericTable
import org.simbrain.util.table.SimbrainJTable
import org.simbrain.util.table.SimbrainJTableScrollPanel
import org.simbrain.util.table.TableActionManager
import smile.classification.Classifier
import smile.classification.SVM
import smile.math.kernel.PolynomialKernel
import java.awt.BorderLayout
import java.awt.Dimension
import java.beans.PropertyChangeSupport
import java.util.function.Consumer
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JToolBar
import javax.swing.event.TableModelEvent

class DataPanelEvents(dataPanel: DataPanel): Event(PropertyChangeSupport(dataPanel)) {

    fun onApply(handler: Consumer<Array<DoubleArray>>) = "Apply".itemAddedEvent(handler)
    fun fireApply(data: Array<DoubleArray>) = "Apply"(new = data)

    fun onInsert(handler: Consumer<Int>) = "Insert".itemAddedEvent(handler)
    fun fireInsert(rowNumber: Int) = "Insert"(new = rowNumber)
}

class DataPanel: JPanel() {

    init {
        layout = BorderLayout()
        preferredSize = Dimension(250, 200)
    }

    private val toolbars = JToolBar().also {
        add(it)
        add("North", it)
    }

    val table: NumericTable = NumericTable().apply {
        addTableModelListener { event ->
            when (event.type) {
                TableModelEvent.INSERT -> events.fireInsert(event.firstRow)
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

    val events = DataPanelEvents(this)

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

        events.fireApply(data)
    }

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
            events.onApply { data -> println(data.contentDeepToString()) }
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

        inputPanel.events.onApply { data ->
            input = data
            invokeCallback(input, target)
        }

        targetPanel.events.onApply { data ->
            target = data.map { it[0].toInt() }.toIntArray()
            invokeCallback(input, target)
        }


    }.run { makeVisible() }


}