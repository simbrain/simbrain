package org.simbrain.util.table

import org.simbrain.util.Events

/**
 * Column events are not needed since they are handled by the JTable,
 * but since rows are rendered using a separate object [DataViewerScrollPane], they need their own events.
 */
class TableEvents: Events() {

    val currentRowChanged = NoArgEvent()
    val rowNameChanged = NoArgEvent()

}