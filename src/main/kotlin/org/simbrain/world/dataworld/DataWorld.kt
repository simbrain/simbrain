package org.simbrain.world.dataworld

import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible

class DataWorld(val rows: Int = 30, val cols: Int = 5): AttributeContainer, EditableObject {

    var dataModel = BasicDataFrame(rows, cols)

    var appendMode: DataEntryMode by GuiEditable(
        initValue = DataEntryMode.LOOP
    )

    @Producible
    fun getCurrentStringRow() = dataModel.getCurrentStringRow().toTypedArray()

    @Producible
    fun getCurrentNumericRow() = dataModel.getCurrentDoubleRow().toDoubleArray()

    @Consumable
    fun setCurrentStringRow(row: Array<String>) {
        dataModel.setRow(dataModel.currentRowIndex, row)
        if (appendMode == DataEntryMode.APPEND && dataModel.currentRowIndex == dataModel.rowCount - 1) {
            dataModel.insertRow(dataModel.currentRowIndex + 1)
        }
    }

    @Consumable
    fun setCurrentNumericRow(row: DoubleArray) {
        dataModel.setRow(dataModel.currentRowIndex, row.toTypedArray())
        if (appendMode == DataEntryMode.APPEND && dataModel.currentRowIndex == dataModel.rowCount - 1) {
            dataModel.insertRow(dataModel.currentRowIndex + 1)
        }
    }

    fun update() {
        dataModel.currentRowIndex = (dataModel.currentRowIndex + 1) % dataModel.rowCount
        dataModel.events.currentRowChanged.fire()
    }

    override val id: String = "Data World"

    enum class DataEntryMode {
        LOOP, APPEND
    }
}