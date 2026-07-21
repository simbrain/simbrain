package org.simbrain.network.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.workspace.Workspace

class GenerativeModelTest {

    @Test
    fun `both families in one workspace each describe their own hidden state`() {
        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net", network))
        val languageModel = LanguageModel()
        val transformer = TeachingTransformer(TeachingTransformerConfig(
            contextSize = 6, embedDim = 8, numHeads = 2, hiddenDim = 8, vocabSize = 6, numLayers = 1,
        ))
        runBlocking {
            network.addNetworkModel(languageModel)
            network.addNetworkModel(transformer)
        }

        with(workspace.couplingManager) {
            // The shared base getter means one cached producer builder serves both classes;
            // the description must still dispatch to each instance's own label
            val first = languageModel.getProducer("getHiddenState")
            val second = transformer.getProducer("getHiddenState")
            assertTrue(first.simpleDescription.contains("layer 0 residual"),
                "got: ${first.simpleDescription}")
            assertTrue(second.simpleDescription.contains("final residual"),
                "got: ${second.simpleDescription}")
        }
    }
}
