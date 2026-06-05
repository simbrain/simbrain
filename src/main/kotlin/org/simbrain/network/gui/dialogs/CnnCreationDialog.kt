package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.core.*
import org.simbrain.network.gui.ConvLayerTemplate
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.PoolLayerTemplate
import org.simbrain.network.gui.addSubnetworkAction
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.Theme
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.showErrorDialog
import org.simbrain.util.toDisplayText
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Dialog for creating a [ConvolutionalNeuralNetwork] from scratch.
 *
 * Users configure an input shape, add conv/pool layers and dense layers
 * incrementally, and the dialog builds the full pipeline on OK.
 */
class CnnCreationDialog(private val networkPanel: NetworkPanel) : StandardDialog() {

    // Layer spec models

    private sealed class LayerSpec {
        /** Short description of the layer parameters (no output shape). */
        abstract fun description(): String
        abstract fun outputShape(inputShape: TensorShape): TensorShape

        class Conv(val template: ConvLayerTemplate = ConvLayerTemplate()) : LayerSpec() {
            override fun description(): String =
                "Conv: ${template.kernelSize}x${template.kernelSize}, " +
                        "${template.numFilters} filters, ${template.activation.toDisplayText()}"

            override fun outputShape(inputShape: TensorShape): TensorShape =
                inputShape.convOutputShape(template.kernelSize, template.stride, template.padding, template.numFilters)
        }

        class Pool(val template: PoolLayerTemplate = PoolLayerTemplate()) : LayerSpec() {
            override fun description(): String =
                "Pool: ${template.poolSize}x${template.poolSize} ${template.poolingType.toDisplayText()}, Stride ${template.stride}"

            override fun outputShape(inputShape: TensorShape): TensorShape =
                inputShape.poolOutputShape(template.poolSize, template.stride)
        }
    }

    private data class DenseLayerSpec(var neurons: Int = 128)

    private val tensorLayers = mutableListOf<LayerSpec>()
    private val denseLayers = mutableListOf<DenseLayerSpec>()

    // Input shape fields
    private val heightField = JTextField("28", 4)
    private val widthField = JTextField("28", 4)
    private val channelsField = JTextField("1", 3)

    // Output field
    private val outputNeuronsField = JTextField("10", 4)

    // Layer list displays
    // Columns: index | description | -> | H | x | W | x | C | Edit | up | down | Remove
    private val tensorLayerListPanel = JPanel(MigLayout(
        "fillx, ins 0, gapy 4",
        "[][grow,fill][][right][][right][][right][][][][]"
    ))
    private val denseLayerListPanel = JPanel(MigLayout(
        "fillx, ins 0, gapy 4",
        "[][grow,fill][][][]"
    ))
    private val flattenLabel = JLabel()

    init {
        title = "New Convolutional Neural Network"

        val mainPanel = JPanel(MigLayout("fillx, ins 10, wrap", "[grow,fill]"))

        // Input shape section
        val inputPanel = JPanel(MigLayout("ins 0", "[][][][][][]"))
        inputPanel.add(JLabel("Input Shape:"))
        inputPanel.add(JLabel("H:"))
        inputPanel.add(heightField)
        inputPanel.add(JLabel("W:"))
        inputPanel.add(widthField)
        inputPanel.add(JLabel("C:"))
        inputPanel.add(channelsField)
        mainPanel.add(inputPanel)

        // Auto-update on input shape changes
        val shapeChangeListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = rebuildAll()
            override fun removeUpdate(e: DocumentEvent?) = rebuildAll()
            override fun changedUpdate(e: DocumentEvent?) = rebuildAll()
        }
        heightField.document.addDocumentListener(shapeChangeListener)
        widthField.document.addDocumentListener(shapeChangeListener)
        channelsField.document.addDocumentListener(shapeChangeListener)

        // Tensor layers section
        val tensorSection = JPanel(MigLayout("fillx, ins 8 12 8 12, wrap", "[grow,fill]")).apply {
            border = Theme.sectionBorder("Tensor Layers (Conv / Pool)")
        }
        val tensorScrollPane = JScrollPane(tensorLayerListPanel).apply {
            border = null
            minimumSize = java.awt.Dimension(0, 130)
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        tensorSection.add(tensorScrollPane, "growx")

        val tensorButtonPanel = JPanel(MigLayout("ins 4")).apply {
            add(JButton("+ Add Conv Layer").apply {
                addActionListener { addTensorLayer(LayerSpec.Conv()) }
            })
            add(JButton("+ Add Pool Layer").apply {
                addActionListener { addTensorLayer(LayerSpec.Pool()) }
            })
        }
        tensorSection.add(tensorButtonPanel)
        mainPanel.add(tensorSection)

        // Flatten label
        mainPanel.add(flattenLabel, "gaptop 5, gapbottom 5")

        // Dense layers section
        val denseSection = JPanel(MigLayout("fillx, ins 8 12 8 12, wrap", "[grow,fill]")).apply {
            border = Theme.sectionBorder("Dense Layers")
        }
        val denseScrollPane = JScrollPane(denseLayerListPanel).apply {
            border = null
            minimumSize = java.awt.Dimension(0, 80)
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        denseSection.add(denseScrollPane, "growx")

        val denseButtonPanel = JPanel(MigLayout("ins 4")).apply {
            add(JButton("+ Add Dense Layer").apply {
                addActionListener { addDenseLayer(DenseLayerSpec()) }
            })
        }
        denseSection.add(denseButtonPanel)
        mainPanel.add(denseSection)

        // Output section
        val outputPanel = JPanel(MigLayout("ins 0", "[][]"))
        outputPanel.add(JLabel("Output Neurons:"))
        outputPanel.add(outputNeuronsField)
        mainPanel.add(outputPanel)

        contentPane = mainPanel

        // Add default layers
        addTensorLayer(LayerSpec.Conv(ConvLayerTemplate().apply { numFilters = 8 }))
        addTensorLayer(LayerSpec.Pool())
        addTensorLayer(LayerSpec.Conv(ConvLayerTemplate().apply { numFilters = 16 }))
        addTensorLayer(LayerSpec.Pool())

        pack()
    }

    private fun getInputShape(): TensorShape? {
        return try {
            TensorShape(
                heightField.text.trim().toInt(),
                widthField.text.trim().toInt(),
                channelsField.text.trim().toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    // Tensor layer management

    private fun addTensorLayer(spec: LayerSpec) {
        tensorLayers.add(spec)
        rebuildAll()
    }

    private fun removeTensorLayer(index: Int) {
        tensorLayers.removeAt(index)
        rebuildAll()
    }

    private fun editTensorLayer(index: Int) {
        when (val spec = tensorLayers[index]) {
            is LayerSpec.Conv -> spec.template.createEditorDialog(parent = this) {
                rebuildAll()
            }.display()
            is LayerSpec.Pool -> spec.template.createEditorDialog(parent = this) {
                rebuildAll()
            }.display()
        }
    }

    private fun moveTensorLayerUp(index: Int) {
        if (index > 0) {
            val temp = tensorLayers[index]
            tensorLayers[index] = tensorLayers[index - 1]
            tensorLayers[index - 1] = temp
            rebuildAll()
        }
    }

    private fun moveTensorLayerDown(index: Int) {
        if (index < tensorLayers.size - 1) {
            val temp = tensorLayers[index]
            tensorLayers[index] = tensorLayers[index + 1]
            tensorLayers[index + 1] = temp
            rebuildAll()
        }
    }

    // Dense layer management

    private fun addDenseLayer(spec: DenseLayerSpec) {
        denseLayers.add(spec)
        rebuildAll()
    }

    private fun removeDenseLayer(index: Int) {
        denseLayers.removeAt(index)
        rebuildAll()
    }

    private fun moveDenseLayerUp(index: Int) {
        if (index > 0) {
            val temp = denseLayers[index]
            denseLayers[index] = denseLayers[index - 1]
            denseLayers[index - 1] = temp
            rebuildAll()
        }
    }

    private fun moveDenseLayerDown(index: Int) {
        if (index < denseLayers.size - 1) {
            val temp = denseLayers[index]
            denseLayers[index] = denseLayers[index + 1]
            denseLayers[index + 1] = temp
            rebuildAll()
        }
    }

    private fun rebuildAll() {
        rebuildTensorLayerList()
        rebuildDenseLayerList()
    }

    private fun rebuildTensorLayerList() {
        tensorLayerListPanel.removeAll()

        val inputShape = getInputShape()
        var currentShape = inputShape
        var hasError = false

        for ((i, spec) in tensorLayers.withIndex()) {
            // Index column
            tensorLayerListPanel.add(JLabel("${i + 1}."), "right")

            if (currentShape != null && !hasError) {
                try {
                    val out = spec.outputShape(currentShape)
                    // Description
                    tensorLayerListPanel.add(JLabel(spec.description()))
                    // Arrow
                    tensorLayerListPanel.add(JLabel("\u2192"))
                    // H x W x C in separate right-aligned columns
                    tensorLayerListPanel.add(JLabel("${out.height}"), "right")
                    tensorLayerListPanel.add(JLabel("\u00d7"))
                    tensorLayerListPanel.add(JLabel("${out.width}"), "right")
                    tensorLayerListPanel.add(JLabel("\u00d7"))
                    tensorLayerListPanel.add(JLabel("${out.channels}"), "right")
                    currentShape = out
                } catch (e: Exception) {
                    tensorLayerListPanel.add(
                        JLabel("<html><font color='red'>Invalid: ${e.message}</font></html>"),
                        "span 7"
                    )
                    hasError = true
                }
            } else {
                tensorLayerListPanel.add(
                    JLabel("<html><font color='gray'>Cannot compute (earlier error)</font></html>"),
                    "span 7"
                )
            }

            // Buttons
            tensorLayerListPanel.add(JButton("Edit").apply { addActionListener { editTensorLayer(i) } })
            tensorLayerListPanel.add(JButton("\u2191").apply {
                toolTipText = "Move up"
                addActionListener { moveTensorLayerUp(i) }
                isEnabled = i > 0
            })
            tensorLayerListPanel.add(JButton("\u2193").apply {
                toolTipText = "Move down"
                addActionListener { moveTensorLayerDown(i) }
                isEnabled = i < tensorLayers.size - 1
            })
            tensorLayerListPanel.add(JButton("Remove").apply {
                addActionListener { removeTensorLayer(i) }
            }, "wrap")
        }

        // Update flatten label
        if (currentShape != null && !hasError) {
            flattenLabel.text = "Flatten: ${currentShape.size} features"
        } else if (tensorLayers.isEmpty()) {
            flattenLabel.text = "Flatten: (add tensor layers first)"
        } else {
            flattenLabel.text = "Flatten: (fix errors above)"
        }

        tensorLayerListPanel.revalidate()
        tensorLayerListPanel.repaint()
    }

    private fun rebuildDenseLayerList() {
        denseLayerListPanel.removeAll()

        for ((i, spec) in denseLayers.withIndex()) {
            // Index
            denseLayerListPanel.add(JLabel("${i + 1}."), "right")

            // Neurons field
            val neuronsField = JTextField(spec.neurons.toString(), 5)
            neuronsField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updateSpec()
                override fun removeUpdate(e: DocumentEvent?) = updateSpec()
                override fun changedUpdate(e: DocumentEvent?) = updateSpec()
                private fun updateSpec() {
                    neuronsField.text.trim().toIntOrNull()?.let { spec.neurons = it }
                }
            })
            val fieldPanel = JPanel(MigLayout("ins 0", "[][]")).apply {
                add(JLabel("Neurons:"))
                add(neuronsField)
            }
            denseLayerListPanel.add(fieldPanel)

            // Buttons
            denseLayerListPanel.add(JButton("\u2191").apply {
                toolTipText = "Move up"
                addActionListener { moveDenseLayerUp(i) }
                isEnabled = i > 0
            })
            denseLayerListPanel.add(JButton("\u2193").apply {
                toolTipText = "Move down"
                addActionListener { moveDenseLayerDown(i) }
                isEnabled = i < denseLayers.size - 1
            })
            denseLayerListPanel.add(JButton("Remove").apply {
                addActionListener { removeDenseLayer(i) }
            }, "wrap")
        }

        denseLayerListPanel.revalidate()
        denseLayerListPanel.repaint()
    }

    override fun closeDialogOk() {
        val inputShape = getInputShape()
        if (inputShape == null) {
            showErrorDialog("Invalid input shape.")
            return
        }

        if (tensorLayers.isEmpty()) {
            showErrorDialog("Add at least one tensor layer.")
            return
        }

        val outputNeurons = try {
            outputNeuronsField.text.trim().toInt().also { require(it > 0) }
        } catch (e: Exception) {
            showErrorDialog("Invalid output neuron count.")
            return
        }

        // Validate dense layer neuron counts
        for ((i, spec) in denseLayers.withIndex()) {
            if (spec.neurons <= 0) {
                showErrorDialog("Dense layer ${i + 1} must have > 0 neurons.")
                return
            }
        }

        // Validate all tensor shapes
        var currentShape: TensorShape = inputShape
        for ((i, spec) in tensorLayers.withIndex()) {
            try {
                currentShape = spec.outputShape(currentShape)
            } catch (e: Exception) {
                showErrorDialog("Error in tensor layer ${i + 1}: ${e.message}")
                return
            }
        }

        // Build the pipeline
        val network = networkPanel.network
        val location = network.placementManager.lastClickedLocation

        val inputLayer = TensorLayer(inputShape).apply {
            label = "Input ($inputShape)"
            isClamped = true
        }
        inputLayer.setLocation(location.x, location.y)

        var prevTensor = inputLayer
        var yOffset = -400.0
        val allModels = mutableListOf<NetworkModel>(inputLayer)

        // Tensor stages
        for ((i, spec) in tensorLayers.withIndex()) {
            val outShape = spec.outputShape(prevTensor.shape)
            val targetTensor = TensorLayer(outShape)

            when (spec) {
                is LayerSpec.Conv -> {
                    targetTensor.activationFunction = spec.template.activation
                    targetTensor.label = "Conv${i + 1} ($outShape)"
                    val connector = ConvolutionConnector(
                        prevTensor, targetTensor,
                        spec.template.kernelSize, spec.template.numFilters,
                        spec.template.stride, spec.template.padding
                    )
                    allModels.add(targetTensor)
                    allModels.add(connector)
                }
                is LayerSpec.Pool -> {
                    targetTensor.label = "Pool${i + 1} ($outShape)"
                    val connector = PoolingConnector(
                        prevTensor, targetTensor,
                        spec.template.poolSize, spec.template.stride, spec.template.poolingType
                    )
                    allModels.add(targetTensor)
                    allModels.add(connector)
                }
            }

            targetTensor.setLocation(location.x, location.y + yOffset)
            yOffset -= 400.0
            prevTensor = targetTensor
        }

        // Flatten
        val flatSize = prevTensor.shape.size
        val flatArray = NeuronArray(flatSize).apply {
            label = "Flatten ($flatSize)"
        }
        flatArray.setLocation(location.x, location.y + yOffset)
        yOffset -= 400.0
        val flattenConnector = FlattenConnector(prevTensor, flatArray)
        allModels.add(flatArray)
        allModels.add(flattenConnector)

        // Dense layers
        var prevLayer: Layer = flatArray
        for ((i, spec) in denseLayers.withIndex()) {
            val denseArray = NeuronArray(spec.neurons).apply {
                label = "Dense${i + 1} (${spec.neurons})"
            }
            denseArray.setLocation(location.x, location.y + yOffset)
            yOffset -= 400.0
            val wm = WeightMatrix(prevLayer, denseArray)
            allModels.add(denseArray)
            allModels.add(wm)
            prevLayer = denseArray
        }

        // Output
        val outputArray = NeuronArray(outputNeurons).apply {
            label = "Output ($outputNeurons)"
        }
        outputArray.setLocation(location.x, location.y + yOffset)
        val denseWeights = WeightMatrix(prevLayer, outputArray)
        allModels.add(outputArray)
        allModels.add(denseWeights)

        // Create the CNN wrapper — it discovers the pipeline from inputLayer to outputArray.
        // Only the CNN subnetwork is added to the network; its children live in the
        // subnetwork's modelList and are removed from top-level networkModels.
        addSubnetworkAction(networkPanel) {
            ConvolutionalNeuralNetwork(inputLayer, outputArray)
        }

        super.closeDialogOk()
    }
}
