package org.simbrain.util.table

import org.jdesktop.swingx.JXTable
import org.simbrain.util.StandardDialog
import org.simbrain.util.cartesianProduct
import smile.math.matrix.Matrix
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPopupMenu
import javax.swing.JScrollPane


/**
 *
 * The main Simbrain table visualization. Can be used to represent mutable or immutable data, which can be numeric or
 * mixed. Provides ability to edit the table, randomize numeric values, produce plots and visualizations, etc.
 *
 * Visualization for [SimbrainDataModel], which in turns wraps several types of table. Depending on whether the
 * model is mutable or not, different GUI actions are enabled. These actions can be further customized  depending on
 * the context.
 */
class SimbrainDataViewer(val model : SimbrainDataModel) : JXTable(model) {

    init {

        columnSelectionAllowed = true

        setGridColor(Color.gray)

        addMouseListener(object : MouseAdapter() {

            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val menu = buildPopupMenu()
                    menu.show(this@SimbrainDataViewer, e.x, e.y)
                }
            }
        })
    }

    fun getSelectedCells(): List<Pair<Int, Int>> {
        return selectedRows.toList().cartesianProduct(selectedColumns.toList())
    }

    fun randomizeSelectedCells() {
        getSelectedCells().forEach{ (x,y) ->
            model.setValueAt(model.cellRandomizer.random, x,y)
        }
    }

    fun fillSelectedCells(fillVal: Double) {
        getSelectedCells().forEach{ (x,y) ->
            model.setValueAt(fillVal, x,y)
        }
    }

    fun zeroFillSelectedCells() {
        fillSelectedCells(0.0)
    }

    fun buildPopupMenu(): JPopupMenu {
        val ret = JPopupMenu()
        if (model.isMutable) {
            ret.add(getFillAction())
            ret.add(getZeroFillAction())
            ret.add(getRandomizeAction())
            ret.add(getEditRandomizerAction())
            ret.add(getRandomizeColumnAction())
        }
        if (model is DataFrameWrapper) {
            ret.add(model.getShowScatterPlotAction())
        }
        ret.add(getShowHistogramAction())
        ret.add(getShowPlotAction())
        return ret
    }

}

fun main() {

    // val sdv = SimbrainDataViewer(DataFrameWrapper(Read.arff("simulations/tables/iris.arff")))
    val sdv = SimbrainDataViewer(DoubleDataWrapper(Matrix.randn(10,4).toArray()))

    StandardDialog().apply {
        contentPane = JScrollPane(sdv)
        isVisible = true
        pack()
        setLocationRelativeTo(null)
    }
}
