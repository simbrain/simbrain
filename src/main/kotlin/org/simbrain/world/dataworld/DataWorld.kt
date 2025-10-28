package org.simbrain.world.dataworld

import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible

class DataWorld(val rows: Int = 30, val cols: Int = 5): AttributeContainer, EditableObject {

    var dataModel = BasicDataFrame(rows, cols)

    /**
     * Controls how the current row advances on each update tick.
     * - LOOP: advance to next row, wrap to 0 at the end
     * - STATIC: stay on the current row
     */
    var rowAdvancement: RowAdvancement by GuiEditable(
        initValue = RowAdvancement.LOOP,
        description = "Row advancement per update: Loop cycles through rows; Static stays on the current row."
    )

    /**
     * Controls what happens when data is written.
     * - OVERWRITE: write into the current row
     * - APPEND: if writing at the last row, insert a new row after it (table grows)
     */
    var writeBehavior: WriteBehavior by GuiEditable(
        initValue = WriteBehavior.OVERWRITE,
        description = "Data write behavior: Overwrite writes into current row; append grows the table at the end."
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
     * Handle post-write behavior based on writeBehavior.
     * APPEND only takes effect when we're at the last row.
     */
    private fun handleRowWrite() {
        if (writeBehavior == WriteBehavior.APPEND &&
            dataModel.currentRowIndex == dataModel.rowCount - 1
        ) {
            dataModel.insertRow(dataModel.currentRowIndex + 1)
        }
    }

    suspend fun update() {
        handleRowAdvancement()
        dataModel.events.currentRowChanged.fire().await()
    }

    private fun handleRowAdvancement() {
        when (rowAdvancement) {
            RowAdvancement.LOOP -> {
                dataModel.currentRowIndex = (dataModel.currentRowIndex + 1) % dataModel.rowCount
            }
            RowAdvancement.STATIC -> {
                // no movement
            }
        }
    }

    override val id: String = "Data World"

    override val name: String
        get() = id

    /**
     * How the current row advances each update.
     */
    enum class RowAdvancement {
        LOOP,
        STATIC
    }

    /**
     * How data writes behave at the end of the table.
     */
    enum class WriteBehavior {
        OVERWRITE,
        APPEND
    }
}
