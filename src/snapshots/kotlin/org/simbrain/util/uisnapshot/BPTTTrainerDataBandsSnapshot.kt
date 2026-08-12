package org.simbrain.util.uisnapshot

import org.simbrain.network.gui.dialogs.createDataSetPanel
import org.simbrain.network.trainers.BPTTTrainerConfig
import org.simbrain.network.trainers.TrainingDataset
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

/**
 * The training data table with BPTT's truncation windows drawn on it.
 *
 * Ten rows at a depth of four, so the last window is short and the drawing has to cope with a sequence
 * whose length is not a multiple of the depth. A row inside a banded window is selected, since the band
 * and the selection highlight both set a row's background and the selection has to win.
 *
 * The data also declares five row sequences, drawn as heavier rules. Their length is deliberately not the
 * truncation depth, so the two divisions land in different places and it is clear which is which.
 *
 * Worth rendering under `-Ptheme=dark` as well: the band and the rule are the parts most likely to
 * vanish into the background in one theme or the other.
 */
class BPTTTrainerDataBandsSnapshot : UiSnapshotDef {
    override val name = "bptt_trainer_data_bands"

    override fun build(): Component {
        val config = BPTTTrainerConfig().apply { truncationDepth = 4 }
        val dataset = TrainingDataset(
            inputs = MutableList(ROWS) { row -> MutableList(3) { col -> if (col == row % 3) 1.0 else 0.0 } },
            targets = MutableList(ROWS) { row -> MutableList(3) { col -> if (col == (row + 1) % 3) 1.0 else 0.0 } },
            inputSize = 3,
            targetSize = 3,
            // Five row sequences against a depth of four, so the two divisions deliberately fall in
            // different places and can be told apart.
            sequenceLength = 5
        )

        lateinit var panel: Component
        SwingUtilities.invokeAndWait {
            panel = dataset.createDataSetPanel(rowGrouping = { config.rowGrouping }) { }.apply {
                listOf(inputs, targets).forEach { it.preferredSize = Dimension(320, 300) }
                inputs.table.setSelectedRow(5)
            }
        }
        return panel
    }

    companion object {
        private const val ROWS = 10
    }
}
