package org.simbrain.world.dataworld

import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible

class DataWorld(val rows: Int = 30, val cols: Int = 5): AttributeContainer, EditableObject {

    var dataModel = BasicDataFrame(rows, cols)

    var dataEntryMode: DataEntryMode by GuiEditable(
        initValue = DataEntryMode.STATIC
    )

    @Producible
    fun getCurrentStringRow() = dataModel.getCurrentStringRow().toTypedArray()

    @Producible
    fun getCurrentNumericRow() = dataModel.getCurrentDoubleRow().toDoubleArray()

    @Consumable
    fun setCurrentStringRow(row: Array<String>) {
        dataModel.setRow(dataModel.currentRowIndex, row)
        handleRowWrite()
    }

    @Consumable
    fun setCurrentNumericRow(row: DoubleArray) {
        dataModel.setRow(dataModel.currentRowIndex, row.toTypedArray())
        handleRowWrite()
    }

    /**
     * Handle post-write behavior based on current data entry mode
     */
    private fun handleRowWrite() {
        when (dataEntryMode) {
            DataEntryMode.APPEND -> {
                if (dataModel.currentRowIndex == dataModel.rowCount - 1) {
                    dataModel.insertRow(dataModel.currentRowIndex + 1)
                }
            }
            DataEntryMode.LOOP, DataEntryMode.STATIC -> {
                // No additional action needed for LOOP or STATIC modes during write
            }
        }
    }

    suspend fun update() {
        handleRowAdvancement()
        dataModel.events.currentRowChanged.fire().await()
    }

    /**
     * Handle row advancement based on current data entry mode
     */
    private fun handleRowAdvancement() {
        when (dataEntryMode) {
            DataEntryMode.LOOP -> {
                dataModel.currentRowIndex = (dataModel.currentRowIndex + 1) % dataModel.rowCount
            }
            DataEntryMode.APPEND -> {
                dataModel.currentRowIndex = (dataModel.currentRowIndex + 1) % dataModel.rowCount
            }
            DataEntryMode.STATIC -> {
                // Stay on current row - no advancement
            }
        }
    }

    override val id: String = "Data World"

    override val name: String
        get() = id

    /**
     * Defines how the DataWorld handles row advancement and data entry
     */
    enum class DataEntryMode {
        /** Advance to next row on each update, loop back to start when reaching end */
        LOOP,
        /** Advance to next row on each update, add new rows when reaching end */
        APPEND,
        /** Stay on current row, no automatic advancement */
        STATIC
    }
}