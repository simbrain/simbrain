package org.simbrain.util.uisnapshot

import org.simbrain.network.gui.dialogs.createDataSetPanel
import org.simbrain.network.trainers.BPTTTrainerConfig
import org.simbrain.network.trainers.TrainingDataset
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

/**
 * The training data table with BPTT's fixed sequences drawn on it.
 *
 * Eight rows form two four-step sequences. A row inside a band is selected, since the band and selection
 * highlight both set a row's background and the selection has to win.
 *
 * Worth rendering under `-Ptheme=dark` as well: the band and the rule are the parts most likely to
 * vanish into the background in one theme or the other.
 */
class BPTTTrainerDataBandsSnapshot : UiSnapshotDef {
    override val name = "bptt_trainer_data_bands"

    override fun build(): Component {
        val config = BPTTTrainerConfig().apply { sequenceLength = 4 }
        val dataset = TrainingDataset(
            inputs = MutableList(ROWS) { row -> MutableList(3) { col -> if (col == row % 3) 1.0 else 0.0 } },
            targets = MutableList(ROWS) { row -> MutableList(3) { col -> if (col == (row + 1) % 3) 1.0 else 0.0 } },
            inputSize = 3,
            targetSize = 3,
        )

        lateinit var panel: Component
        SwingUtilities.invokeAndWait {
            panel = dataset.createDataSetPanel(
                rowGrouping = { config.rowGrouping },
                sequenceLength = { config.sequenceLength },
                rowsPerEdit = { config.sequenceLength },
                editUnitName = "sequence"
            ) { }.apply {
                listOf(inputs, targets).forEach { it.preferredSize = Dimension(320, 300) }
                inputs.table.setSelectedRow(5)
            }
        }
        return panel
    }

    companion object {
        private const val ROWS = 8
    }
}
