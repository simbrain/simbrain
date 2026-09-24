/** Renders the tiny language model's trainer dialog at its packed size, for sizing checks. */
package org.simbrain.util.uisnapshot

import org.simbrain.network.gui.nodes.createTrainingDialog
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.network.llm.TinyLmConfig
import java.awt.Component

class TinyLanguageModelTrainerSnapshot : UiSnapshotDef {
    override val name = "tiny-language-model-trainer"

    override fun build(): Component {
        val model = TinyLanguageModel(TinyLmConfig(
            contextSize = 12, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 8, numLayers = 1
        )).apply { label = "Tiny language model" }
        model.setCorpus(IntArray(60) { it % 8 })
        return model.createTrainingDialog(null).apply { pack() }
    }
}
