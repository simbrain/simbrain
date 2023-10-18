package org.simbrain.world.dataworld

import org.simbrain.util.table.BasicDataWrapper
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible

class DataWorld: AttributeContainer {

    var dataModel = BasicDataWrapper(
        data = MutableList(30) {
            MutableList(5) {
                0.0
            }
        }
    )

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