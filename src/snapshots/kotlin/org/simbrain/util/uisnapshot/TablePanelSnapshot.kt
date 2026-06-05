package org.simbrain.util.uisnapshot

import org.simbrain.util.table.BasicDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import java.awt.Component
import java.awt.Dimension

class TablePanelSnapshot : UiSnapshotDef {
    override val name = "table_panel"

    override fun build(): Component {
        val data = (1..6).map { r -> (1..4).map { c -> (r * c).toDouble() as Any? } }
        return SimbrainTablePanel(BasicDataFrame(data)).apply {
            preferredSize = Dimension(380, 220)
        }
    }
}
