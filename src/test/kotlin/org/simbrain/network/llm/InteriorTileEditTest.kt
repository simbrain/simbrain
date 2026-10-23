/**
 * Tests for the interior tile actions on language models: hand edits to the matrix a weight
 * tile shows, tile probes pinned to a layer and head, logit lens labels for plotted checkpoints,
 * and the probe couplings and tile labels that ride through workspace serialization.
 */
package org.simbrain.network.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.compositor.DeckTile
import org.simbrain.network.compositor.MatrixTile
import org.simbrain.network.compositor.visibleToken
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.network.trainers.Randomize
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.serialization.WorkspaceSerializer
import org.simbrain.world.dataworld.DataWorld
import org.simbrain.world.dataworld.DataWorldComponent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class InteriorTileEditTest {

    private fun tinyModel() = TinyLanguageModel(TinyLmConfig(
        contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1
    )).apply { tokenLabels = arrayListOf("a", "b", "c", "d", " e") }

    private fun MatrixTile.weights() = tensor.toFloatArray()

    @Test
    fun `clearing a weight tile zeroes its matrix and reruns the context`() {
        val model = tinyModel()
        model.setContext(intArrayOf(1, 2, 3))
        model.forwardContext()
        val wq = model.scene.tile("layers.0.attn.wq") as MatrixTile
        assertTrue(model.model.plan.port("layers.0.attn.q").tensor.toFloatArray().any { it != 0f })

        model.editWeights(listOf(wq), WeightEdit.CLEAR)

        assertTrue(wq.weights().all { it == 0f })
        assertTrue(wq.values.all { it == 0f }, "tile republished from the edited matrix")
        assertTrue(model.model.plan.port("layers.0.attn.q").tensor.toFloatArray().all { it == 0f },
            "queries recomputed from the cleared Wq")
    }

    @Test
    fun `up and down move every weight by the tile's increment`() {
        val model = tinyModel()
        val w1 = model.scene.tile("layers.0.mlp.w1") as MatrixTile
        val before = w1.weights()
        model.setTileIncrement(w1, 0.5)

        model.editWeights(listOf(w1), WeightEdit.INCREMENT)
        model.editWeights(listOf(w1), WeightEdit.INCREMENT)
        model.editWeights(listOf(w1), WeightEdit.DECREMENT)

        w1.weights().forEachIndexed { i, v -> assertEquals(before[i] + 0.5f, v, 1e-5f) }
    }

    @Test
    fun `tiles without their own increment use the model's`() {
        val model = tinyModel()
        val w2 = model.scene.tile("layers.0.mlp.w2") as MatrixTile
        val before = w2.weights()
        model.weightIncrement = 0.25

        model.editWeights(listOf(w2), WeightEdit.DECREMENT)

        w2.weights().forEachIndexed { i, v -> assertEquals(before[i] - 0.25f, v, 1e-5f) }
    }

    @Test
    fun `randomize redraws weights with the model's weight initialization`() {
        val model = tinyModel()
        model.weightInitialization = Randomize().apply { distribution = UniformRealDistribution(2.0, 3.0) }
        val wo = model.scene.tile("layers.0.attn.wo") as MatrixTile

        model.editWeights(listOf(wo), WeightEdit.RANDOMIZE)

        assertTrue(wo.weights().all { it in 2f..3f })
    }

    @Test
    fun `weight edits ignore tiles that are not weights`() {
        val model = tinyModel()
        model.setContext(intArrayOf(1, 2, 3))
        model.forwardContext()
        val resid = model.scene.tile("resid0") as MatrixTile
        val before = resid.tensor.toFloatArray()

        model.editWeights(listOf(resid), WeightEdit.CLEAR)

        assertArrayEquals(before, resid.tensor.toFloatArray())
    }

    @Test
    fun `replacing weights writes the new matrix`() {
        val model = tinyModel()
        val wk = model.scene.tile("layers.0.attn.wk") as MatrixTile
        val values = FloatArray(wk.tensor.size) { it.toFloat() }

        model.replaceWeights(wk, values)

        assertArrayEquals(values, wk.weights())
    }

    @Test
    fun `a probe keeps reading the head shown when it was made`() {
        val model = tinyModel()
        model.setContext(intArrayOf(1, 2, 3, 4))
        model.forwardContext()
        val deck = model.scene.tile("layers.0.attn.weights") as DeckTile
        deck.selectedSlice = 2
        val probe = model.probeFor(deck)
        deck.selectedSlice = 0

        val weights = model.model.plan.port("layers.0.attn.weights").tensor
        val row = 3
        val expected = DoubleArray(deck.cols) { weights[2 * deck.rows + row, it].toDouble() }
        assertEquals(2, probe.pinnedHead)
        assertArrayEquals(expected, probe.values, 1e-6)
        assertEquals(1.0, probe.values.sum(), 1e-4, "one head's attention row is a distribution")
    }

    @Test
    fun `a probe reads the current token's row of a full-sequence tile`() {
        val model = tinyModel()
        model.setContext(intArrayOf(1, 2))
        model.forwardContext()
        val probe = model.probeFor(model.scene.tile("resid0"))

        val resid = model.model.plan.port("resid0").tensor
        assertArrayEquals(DoubleArray(resid.cols) { resid[1, it].toDouble() }, probe.values, 1e-6)
        assertEquals(-1, probe.pinnedLayer)
        assertEquals(-1, probe.pinnedHead)
    }

    @Test
    fun `probes made for the same view are shared`() {
        val model = tinyModel()
        val tile = model.scene.tile("resid0")

        assertSame(model.probeFor(tile), model.probeFor(tile))
        assertEquals(1, model.tileProbes.size)
    }

    @Test
    fun `a checkpoint probe carries the logit lens token`() {
        val model = tinyModel()
        model.setContext(intArrayOf(1, 2, 3))
        model.forwardContext()
        val resid = model.scene.tile("layers.0.resid")
        val probe = model.probeFor(resid)

        val lens = model.scene.lens!!
        val reading = lens.readings[lens.sources.indexOfFirst { it.name == "layers.0.resid" }]
        assertTrue(model.hasLensReading(resid))
        assertEquals(visibleToken(model.tokenLabels!![reading.tokenId]), probe.lensToken)
    }

    @Test
    fun `lens tokens are withheld with the lens off or off the checkpoints`() {
        val model = tinyModel()
        model.setContext(intArrayOf(1, 2, 3))
        model.forwardContext()
        val query = model.scene.tile("layers.0.attn.q")
        assertFalse(model.hasLensReading(query))
        assertNull(model.probeFor(query).lensToken)

        val residProbe = model.probeFor(model.scene.tile("resid0"))
        assertNotNull(residProbe.lensToken)
        model.scene.lens!!.enabled = false
        assertNull(residProbe.lensToken)
    }

    @Test
    fun `a lens-labeled projection labels each point with the lens token`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Net")
        workspace.addWorkspaceComponent(networkComponent)
        val model = tinyModel()
        runBlocking { networkComponent.network.addNetworkModel(model) }
        val probe = model.probeFor(model.scene.tile("layers.0.resid"))
        val plot = ProjectionComponent("Projection")
        workspace.addWorkspaceComponent(plot)
        with(workspace.couplingManager) {
            probe.getProducer("getValues") couple plot.getConsumer("addPoint")
            probe.getProducer("getLensToken") couple plot.getConsumer("setLabel")
        }
        model.setContext(intArrayOf(1, 2, 3))
        model.forwardContext()

        runBlocking { workspace.couplingManager.updateCouplings() }

        val point = plot.projector.dataset.currentPoint!!
        assertArrayEquals(probe.values, point.upstairsPoint, 1e-9)
        assertEquals(probe.lensToken, point.label)
    }

    @Test
    fun `probe names follow the tile's label`() {
        val model = tinyModel()
        model.label = "TLM"
        val resid = model.scene.tile("resid0")
        val probe = model.probeFor(resid)
        model.setTileLabel(resid, "stream in")

        assertEquals("TLM stream in", probe.attributeName)
    }

    @Test
    fun `probe couplings and tile labels survive workspace serialization`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Net")
        workspace.addWorkspaceComponent(networkComponent)
        val model = tinyModel()
        runBlocking { networkComponent.network.addNetworkModel(model) }
        val resid = model.scene.tile("resid0")
        model.setTileLabel(resid, "stream in")
        val probe = model.probeFor(resid)
        val recorder = DataWorldComponent("Recorder", DataWorld(cols = probe.values.size))
        workspace.addWorkspaceComponent(recorder)
        with(workspace.couplingManager) {
            probe.getProducer("getValues") couple recorder.dataWorld.getConsumer("setCurrentNumericRow")
        }

        val serializer = WorkspaceSerializer(workspace)
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        workspace.clearWorkspace()
        runBlocking { serializer.deserialize(ByteArrayInputStream(bas.toByteArray())) }

        val restored = (workspace.getComponent("Net") as NetworkComponent).network
            .getModels<TinyLanguageModel>().single()
        val restoredProbe = workspace.couplingManager.couplings.single().producer.baseObject as TileProbe
        assertSame(restored, restoredProbe.host)
        assertEquals("resid0", restoredProbe.tileId)
        assertEquals("stream in", restored.scene.tile("resid0").displayTitle)
    }

    @Test
    fun `lfm weight edits refeed the window and restore rereads the file`() {
        val dir = assumeOrRequireWeights()
        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 32)
        languageModel.initialText = "The capital of France is"
        languageModel.loadWeights()
        repeat(3) { languageModel.step() }
        val wq = languageModel.loaded!!.scene.tile("block.w.self_attn.q_proj.weight") as MatrixTile
        val original = wq.weights()

        languageModel.editWeights(listOf(wq), WeightEdit.CLEAR)

        assertTrue(languageModel.hasEditedWeights)
        assertTrue(wq.weights().all { it == 0f })
        assertEquals(0, languageModel.fedTokenCount, "caches from the old weights are dropped")
        assertTrue(languageModel.isPromptProcessing, "the window is queued to run again")

        languageModel.restoreWeights()

        assertFalse(languageModel.hasEditedWeights)
        assertArrayEquals(original, wq.weights())
    }

    @Test
    fun `lfm lens tokens match the current token even while the async lens trails`() {
        val dir = assumeOrRequireWeights()
        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 32)
        languageModel.initialText = "The capital of France is"
        languageModel.loadWeights()
        val scene = languageModel.loaded!!.scene
        val probe = languageModel.probeFor(scene.tile("block.resid"))
        repeat(6) { languageModel.step() }

        val token = probe.lensToken
        val lens = scene.lens!!
        val index = lens.sources.indexOfFirst { it.name == "layers.${probe.pinnedLayer}.resid" }
        val version = lens.sources[index].tensor.version
        val deadline = System.currentTimeMillis() + 10_000
        while (lens.readings[index].sourceVersion != version && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        assertNotNull(token)
        assertEquals(visibleToken(languageModel.loaded!!.tokenizer.decode(intArrayOf(lens.readings[index].tokenId))), token)
    }
}
