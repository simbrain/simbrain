package org.simbrain.util.table

import org.jdesktop.swingx.JXTable
import org.simbrain.util.StandardDialog
import smile.io.Read
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPopupMenu
import javax.swing.JScrollPane


/**
 * Wrapper for Smile DataFrame
 */
class SimbrainDataViewer(val model : SimbrainDataModel) : JXTable(model) {

    /**
     * Whether to display the default popup menu.
     */
    private val displayPopUpMenu = true

    var selectedPoint: Point? = null

    init {
        columnSelectionAllowed = true
        isRolloverEnabled = true
        setRowSelectionAllowed(true)
        setGridColor(gridColor)

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                selectedPoint = e.point
                if (e.isPopupTrigger && displayPopUpMenu) {
                    val menu = buildPopupMenu()
                    menu.show(this@SimbrainDataViewer, selectedPoint!!.getX().toInt(), selectedPoint!!.getY().toInt())
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger && displayPopUpMenu) {
                    val menu: JPopupMenu = buildPopupMenu()
                    menu.show(this@SimbrainDataViewer, selectedPoint!!.getX().toInt(), selectedPoint!!.getY().toInt())
                }
            }
        })
    }

    fun buildPopupMenu(): JPopupMenu {
        val ret = JPopupMenu()
        // TODO: Make generic to SimbrainDataModel
        ret.add(getShowPlotAction(model as DataFrameWrapper))
        return ret
    }

}

fun main() {

    val df = Read.csv("simulations/tables/iris_input.csv")
    val sdv = SimbrainDataViewer(DataFrameWrapper(df))

    StandardDialog().apply {
        contentPane = JScrollPane(sdv)
        isVisible = true
        setLocationRelativeTo(null)
        pack()
    }
}
