package org.simbrain.util.uisnapshot

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Padding
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.TensorActivation
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.TransformerBlock
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.ClassificationDatasetEncoding
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.util.point
import smile.math.matrix.Matrix
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * One of every model node (in the view modes worth checking) on a single network canvas, so the
 * harness can render it light + dark and spot non-themed / white surfaces. Layout is on a wide grid
 * with generous spacing so nothing overlaps. A few models get deterministic non-zero activations /
 * weights so hot/cool colormap colors show (no Math.random — the scene must be reproducible).
 */
class KitchenSinkSnapshot : UiSnapshotDef {
    override val name = "kitchen_sink"

    /** Spread of deterministic values in [-1, 1] for an array of the given size. */
    private fun spread(size: Int): DoubleArray =
        DoubleArray(size) { (it - (size - 1) / 2.0) / ((size - 1) / 2.0).coerceAtLeast(1.0) }

    override fun build(): Component {
        // Capture the harness-selected theme before any app code reinstalls a stale look-and-feel
        // mid-build; we re-assert it (then run the recolor hook) at the end.
        val harnessDark = javax.swing.UIManager.getBoolean("laf.dark")
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(1400, 1000) }

        runBlocking {

            // Row 1: neuron arrays in every image/circle view mode.

            // NeuronArray, line/flat horizontal image.
            val naLineH = NeuronArray(25).apply {
                label = "Array line/flat (h)"
                gridMode = false
                circleMode = false
                verticalLayout = false
                setActivations(spread(25))
            }
            network.addNetworkModel(naLineH, usePlacementManager = false)
            naLineH.location = point(-600.0, -500.0)

            // NeuronArray, line/flat vertical image.
            val naLineV = NeuronArray(25).apply {
                label = "Array line/flat (v)"
                gridMode = false
                circleMode = false
                verticalLayout = true
                setActivations(spread(25))
            }
            network.addNetworkModel(naLineV, usePlacementManager = false)
            naLineV.location = point(-300.0, -500.0)

            // NeuronArray, grid (square image) mode.
            val naGrid = NeuronArray(25).apply {
                label = "Array grid"
                gridMode = true
                setActivations(spread(25))
            }
            network.addNetworkModel(naGrid, usePlacementManager = false)
            naGrid.location = point(0.0, -500.0)

            // NeuronArray, grid image with bias image shown.
            val naBias = NeuronArray(25).apply {
                label = "Array + bias"
                gridMode = true
                isShowBias = true
                setActivations(spread(25))
            }
            network.addNetworkModel(naBias, usePlacementManager = false)
            naBias.location = point(300.0, -500.0)

            // NeuronArray, CIRCLE mode (grid of NeuronCircleNodes).
            val naCircle = NeuronArray(25).apply {
                label = "Array circle"
                circleMode = true
                gridMode = true
                setActivations(spread(25))
            }
            network.addNetworkModel(naCircle, usePlacementManager = false)
            naCircle.location = point(600.0, -500.0)

            // Row 2: weight matrix between two arrays.
            val wmSrc = NeuronArray(25).apply {
                label = "WM source"
                gridMode = true
                setActivations(spread(25))
            }
            val wmTgt = NeuronArray(16).apply {
                label = "WM target"
                gridMode = true
            }
            network.addNetworkModel(wmSrc, usePlacementManager = false)
            network.addNetworkModel(wmTgt, usePlacementManager = false)
            wmSrc.location = point(-600.0, -200.0)
            wmTgt.location = point(-200.0, -200.0)
            val wm = WeightMatrix(wmSrc, wmTgt)
            network.addNetworkModel(wm, usePlacementManager = false)
            // Deterministic spread of +/- weights so neg/mid/pos colors show.
            val wmData = DoubleArray(25 * 16) { ((it % 7) - 3) / 3.0 }
            wm.setWeights(wmData)

            // Row 2 right: free-standing text node.
            val textObject = NetworkTextObject("Free text node").apply {
                fontSize = 18
                isBold = true
            }
            textObject.location = point(150.0, -200.0)
            network.addNetworkModel(textObject, usePlacementManager = false)

            // Row 3: NeuronCollection (a few neurons + green tab + outline).
            val nc1 = Neuron().apply { location = point(-600.0, 100.0) }
            val nc2 = Neuron().apply { location = point(-560.0, 100.0) }
            val nc3 = Neuron().apply { location = point(-520.0, 100.0) }
            network.addNetworkModel(nc1, usePlacementManager = false)
            network.addNetworkModel(nc2, usePlacementManager = false)
            network.addNetworkModel(nc3, usePlacementManager = false)
            val nc = NeuronCollection(listOf(nc1, nc2, nc3)).apply { label = "Collection" }
            network.addNetworkModel(nc, usePlacementManager = false)

            // Row 3 middle: SynapseGroup in directed (collapsed green arrow) mode.
            val sgSrc = listOf(
                Neuron().apply { location = point(-260.0, 60.0) },
                Neuron().apply { location = point(-220.0, 60.0) },
                Neuron().apply { location = point(-240.0, 100.0) }
            )
            sgSrc.forEach { network.addNetworkModel(it, usePlacementManager = false) }
            val sgSrcGroup = NeuronCollection(sgSrc).apply { label = "SG src" }
            network.addNetworkModel(sgSrcGroup, usePlacementManager = false)
            val sgTgt = listOf(
                Neuron().apply { location = point(60.0, 60.0) },
                Neuron().apply { location = point(100.0, 60.0) },
                Neuron().apply { location = point(80.0, 100.0) }
            )
            sgTgt.forEach { network.addNetworkModel(it, usePlacementManager = false) }
            val sgTgtGroup = NeuronCollection(sgTgt).apply { label = "SG tgt" }
            network.addNetworkModel(sgTgtGroup, usePlacementManager = false)
            val sg = SynapseGroup(sgSrcGroup, sgTgtGroup).apply {
                autoVisibility = false   // stop the threshold from re-expanding it
                displaySynapses = false  // -> directed green arrow
            }
            network.addNetworkModel(sg, usePlacementManager = false)

            // Row 4: SRN subnetwork (Outline + tab + 4 internal NeuronArray layers + weight matrices).
            val srn = SRNNetwork(
                numInputNodes = 5,
                numHiddenNodes = 5,
                numOutputNodes = 5,
                initialPosition = point(0, 0)
            )
            network.addNetworkModel(srn, usePlacementManager = false)
            srn.location = point(-600.0, 350.0)

            // Row 4 right: SmileClassifier (Outline + tab + 2 green-tab NeuronCollections + bezier arrow).
            val svm = SVMClassifier(
                createClassificationDataset(
                    inputs = mutableListOf(
                        mutableListOf(0.0, 0.0, 0.0),
                        mutableListOf(1.0, 0.0, 0.0),
                        mutableListOf(0.0, 1.0, 0.0),
                        mutableListOf(1.0, 1.0, 0.0)
                    ),
                    targets = mutableListOf(-1, 1, 1, -1),
                    encoding = ClassificationDatasetEncoding.Bipolar
                ),
                1.0
            )
            val classifier = ClassifierNetwork(svm)
            network.addNetworkModel(classifier, usePlacementManager = false)
            classifier.location = point(-150.0, 350.0)

            // Row 4 far right: Hopfield subnetwork (carries an InfoText status readout under it).
            val hopfield = Hopfield(8)
            network.addNetworkModel(hopfield, usePlacementManager = false)
            hopfield.location = point(300.0, 350.0)
            hopfield.updateStateInfoText()

            // Row 5: TensorLayer (8x8x3) — thumbnail strip / single-channel / RGB views all exercisable.
            val tensor = TensorLayer(TensorShape(8, 8, 3)).apply {
                label = "Tensor 8x8x3"
                thumbnailStripMode = true
            }
            network.addNetworkModel(tensor, usePlacementManager = false)
            tensor.location = point(-600.0, 650.0)
            // Deterministic spread across the 192-element tensor so the colormap shows hot/cool.
            tensor.activations = spread(tensor.shape.size)

            // Row 5 middle: ConvolutionConnector (single-kernel heatmap + kernel grid) via two tensors.
            val convIn = TensorLayer(TensorShape(8, 8, 3)).apply {
                label = "Conv in 8x8x3"
                activations = spread(8 * 8 * 3)
            }
            val convOut = TensorLayer(convIn.shape.convOutputShape(3, 1, Padding.SAME, 4)).apply {
                label = "Conv out 8x8x4"
                activationFunction = TensorActivation.RELU
            }
            network.addNetworkModel(convIn, usePlacementManager = false)
            network.addNetworkModel(convOut, usePlacementManager = false)
            convIn.location = point(-250.0, 650.0)
            convOut.location = point(100.0, 650.0)
            val conv = ConvolutionConnector(
                convIn, convOut,
                kernelSize = 3, numFilters = 4, stride = 1, padding = Padding.SAME
            ).apply { label = "Convolution" }
            network.addNetworkModel(conv, usePlacementManager = false)
            // Deterministic visible kernel weights.
            val kernelData = conv.kernels
            for (i in kernelData.indices) kernelData[i] = ((i % 5) - 2) / 2.0
            conv.kernels = kernelData

            // Row 5 right: FlattenConnector (plain bezier arrow Tensor -> NeuronArray).
            val poolOut = TensorLayer(TensorShape(4, 4, 4)).apply {
                label = "Pool 4x4x4"
                activations = spread(4 * 4 * 4)
            }
            val flat = NeuronArray(4 * 4 * 4).apply {
                label = "Flattened (64)"
                gridMode = true
            }
            network.addNetworkModel(poolOut, usePlacementManager = false)
            network.addNetworkModel(flat, usePlacementManager = false)
            poolOut.location = point(450.0, 650.0)
            flat.location = point(750.0, 650.0)
            val flatten = FlattenConnector(poolOut, flat).apply { label = "Flatten" }
            network.addNetworkModel(flatten, usePlacementManager = false)

            // Row 6: TransformerBlock (matrices/sequences/attention/FF + white junction & multiply glyphs).
            val tb = TransformerBlock(sequenceSize = 7, inputSize = 4, hiddenSize = 16).apply {
                label = "Transformer"
            }
            network.addNetworkModel(tb, usePlacementManager = false)
            tb.location = point(-600.0, 950.0)

            // Row 6 right: ActivationSequence (sequence image + programmatic row highlight).
            val seq = ActivationSequence(sequenceSize = 7, inputSize = 4).apply {
                label = "Activation sequence 7x4"
            }
            network.addNetworkModel(seq, usePlacementManager = false)
            seq.location = point(300.0, 950.0)
            // Deterministic spread across the 7x4 activation matrix so the colormap shows hot/cool.
            val seqMatrix = Matrix(7, 4)
            for (r in 0 until 7) for (c in 0 until 4) {
                seqMatrix[r, c] = (((r * 4 + c) % 7) - 3) / 3.0
            }
            seq.activations = seqMatrix
            // Exercise the programmatic cyan row highlight.
            seq.highlightedRows = setOf(seq.sequenceSize - 1)

            // SKIPPED DeepNet: model class (kotlindl/DeepNet.kt), node class (DeepNetNode.kt), and the
            // NetworkPanel.createNode dispatch for it are all commented out — it cannot be instantiated
            // or rendered in the current build.
        }

        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            // Re-assert the harness theme, then run the live-recolor hook so every node recolors
            // under it. This exercises the theme-switch path and is robust against the harness
            // reinstalling a stale look-and-feel mid-build.
            if (harnessDark) FlatDarkLaf.setup() else FlatLightLaf.setup()
            panel.preferenceLoader()
            network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
