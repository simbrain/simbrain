package org.simbrain.world.dataworld

import org.simbrain.util.table.BasicDataFrame
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible

class DataWorld: AttributeContainer {

    var dataModel = BasicDataFrame(30, 5)

    @Producible
    fun getCurrentStringRow() = dataModel.getCurrentStringRow().toTypedArray()

    @Producible
    fun getCurrentNumericRow() = dataModel.getCurrentDoubleRow().toDoubleArray()

    fun update() {
        dataModel.currentRowIndex = (dataModel.currentRowIndex + 1) % dataModel.rowCount
        dataModel.events.currentRowChanged.fireAndBlock()
    }

    override val id: String = "Data World"
}