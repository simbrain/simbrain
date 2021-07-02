package org.simbrain.network.gui.dialogs

import org.simbrain.util.StandardDialog
import org.simbrain.util.Utils
import org.simbrain.util.table.NumericTable
import org.simbrain.util.table.SimbrainJTable
import javax.swing.JButton
import javax.swing.JPanel

class DataApplierDialog(val dataConsumer: (data: DoubleArray) -> Unit): JPanel() {

    private val data = ArrayList<DoubleArray>()

    private var row = 0

    private val toolBar = JPanel().apply {
        val runAction = Utils.createAction("Run", "Iterate training once.","menu_icons/Step.png") {
            dataConsumer(data[row])
            row = (row + 1) % data.size
        }
        add(JButton(runAction))
    }.also { add(it) }

    private val jTable = SimbrainJTable.createTable(NumericTable())
        .apply {  }
        .also { add(it) }



}


fun main() {
    StandardDialog().apply {
        contentPane = DataApplierDialog { println(it.joinToString(", ")) }
    }.run { makeVisible() }
}