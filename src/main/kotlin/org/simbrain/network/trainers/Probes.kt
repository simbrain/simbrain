package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.UserParameter
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.geom.Point2D
import kotlin.concurrent.withLock

/**
 * Utilities for linear probes: supervised readouts trained on activations harvested from a layer of a
 * host network, used to test what information that layer represents. Training a probe never modifies
 * the host network because the probe's input layer is a gradient boundary (see [accumulateBackprop]).
 */

/**
 * Runs the host network's forward pass over [inputs] and records [probedLayer]'s activations after
 * each pass. The result can be used as the input rows of a probe's [TrainingDataset]. Must be re-run
 * whenever the host network is retrained, since harvested activations go stale.
 */
context(Network)
fun SupervisedNetwork.harvestActivations(probedLayer: Layer, inputs: List<List<Double>>): MutableList<MutableList<Double>> =
    trainingLock.withLock {
        inputs.map { row ->
            inputLayer.setActivations(row.toDoubleArray())
            forwardPass()
            probedLayer.activationArray.toMutableList()
        }.toMutableList()
    }

/**
 * [harvestActivations] for a [ConvolutionalNeuralNetwork] host, whose forward sweep happens in
 * [ConvolutionalNeuralNetwork.update] rather than a trainer forward pass. [probedLayer] may be a layer
 * inside the CNN pipeline or a probe-created flatten array outside it (see the [TensorLayer] overload
 * of [createProbe]); an outside layer is not part of the CNN's update sweep, so its inputs are pulled
 * explicitly after each sweep.
 */
context(Network)
fun ConvolutionalNeuralNetwork.harvestActivations(probedLayer: Layer, inputs: List<List<Double>>): MutableList<MutableList<Double>> {
    val insideCnn = probedLayer in modelList.all
    return trainingLock.withLock {
        inputs.map { row ->
            inputTensorLayer.activations = row.toDoubleArray()
            update()
            if (!insideCnn) {
                probedLayer.accumulateInputs()
                probedLayer.update()
            }
            probedLayer.activationArray.toMutableList()
        }.toMutableList()
    }
}

/**
 * Assembles harvested activations and derived targets into a probe [TrainingDataset].
 */
fun harvestedDataset(inputs: MutableList<MutableList<Double>>, targets: MutableList<MutableList<Double>>): TrainingDataset {
    require(inputs.isNotEmpty()) { "Cannot build a probe dataset from an empty harvest" }
    require(inputs.size == targets.size) { "Harvested ${inputs.size} activation rows but ${targets.size} target rows" }
    return TrainingDataset(
        inputs = inputs,
        targets = targets,
        inputSize = inputs.first().size,
        targetSize = targets.first().size
    )
}

/**
 * Creates a probe reading from [probedLayer]: a chain of optional hidden [NeuronArray]s (empty by
 * default, which keeps the probe linear — added capacity confounds what is being measured), a Softmax
 * readout, connecting [WeightMatrix] models, and a [Probe] wrapping the whole path. All new
 * models are added to the network and placed to the right of [probedLayer] starting at [offset].
 *
 * The returned probe has a placeholder dataset; populate it with [harvestActivations].
 */
context(Network)
fun createProbe(
    probedLayer: Layer,
    readoutSize: Int,
    readoutLabels: Array<String>? = null,
    hiddenSizes: List<Int> = emptyList(),
    label: String = "Probe",
    offset: Point2D = point(550.0, 0.0),
): Probe = buildProbe(probedLayer, probedLayer, readoutSize, readoutLabels, hiddenSizes, label, offset)

/**
 * [createProbe] for a [TensorLayer] (e.g. a CNN conv or pool stage). Since tensor layers are outside
 * the [Layer] hierarchy the trainer operates on, a [FlattenConnector] and flatten [NeuronArray] are
 * inserted first and the probe reads from the flatten array.
 */
context(Network)
fun createProbe(
    probedTensor: TensorLayer,
    readoutSize: Int,
    readoutLabels: Array<String>? = null,
    hiddenSizes: List<Int> = emptyList(),
    label: String = "Probe",
    offset: Point2D = point(550.0, 0.0),
): Probe {
    val flat = NeuronArray(probedTensor.shape.size).apply {
        this.label = "$label flatten"
        location = point(probedTensor.locationX + offset.x, probedTensor.locationY + offset.y)
    }
    val flatten = FlattenConnector(probedTensor, flat)
    addNetworkModelsAsync(flat, flatten)
    return buildProbe(probedTensor, flat, readoutSize, readoutLabels, hiddenSizes, label, offset)
}

context(Network)
private fun buildProbe(
    probedModel: LocatableModel,
    inputLayer: Layer,
    readoutSize: Int,
    readoutLabels: Array<String>?,
    hiddenSizes: List<Int>,
    label: String,
    offset: Point2D,
): Probe {
    require(readoutSize > 0) { "Probe readout size must be positive" }
    require(hiddenSizes.all { it > 0 }) { "Probe hidden layer sizes must be positive" }

    fun place(index: Int, layer: Layer) {
        layer.location = point(
            inputLayer.locationX + offset.x + index * 350.0,
            inputLayer.locationY + offset.y
        )
    }

    val newModels = mutableListOf<NetworkModel>()
    var current: Layer = inputLayer
    hiddenSizes.forEachIndexed { i, size ->
        val hidden = NeuronArray(size).apply {
            this.label = "$label hidden ${i + 1}"
            updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.Relu }
        }
        place(i, hidden)
        newModels += WeightMatrix(current, hidden)
        newModels += hidden
        current = hidden
    }
    val readout = NeuronArray(readoutSize).apply {
        this.label = "$label readout"
        updateRule = SoftmaxRule()
        gridMode = true
        readoutLabels?.let { labelArray = it }
    }
    place(hiddenSizes.size, readout)
    newModels += WeightMatrix(current, readout)
    newModels += readout
    val probe = Probe(probedModel, inputLayer, readout).apply { this.label = label }
    newModels += probe
    addNetworkModelsAsync(newModels)
    return probe
}

/**
 * Probes whose probed model is one of [hostModels]. Interactive apply/advance-row controls pass the
 * host's layers (or a subnetwork's internal models) after a host forward pass, which never walks
 * probe connectors, then call [Probe.refreshOutput] on each so probe readouts follow along.
 */
fun Network.probesReading(hostModels: Collection<NetworkModel>): List<Probe> =
    getModels(Probe::class.java).filter { probe -> hostModels.any { it === probe.probedModel } }

/**
 * Re-harvests every stale probe that has a registered [Probe.datasetRebuilder]; probes without one
 * (e.g. after loading a workspace without re-running the creating simulation) are left stale.
 * Rebuilds run sequentially: each harvest takes the network training lock and clamps/restores host
 * input state, so interleaving them buys nothing. Returns the number of probes rebuilt.
 */
suspend fun Network.rebuildStaleProbeDatasets(): Int {
    val staleProbes = getModels(Probe::class.java).filter { it.stale && it.datasetRebuilder != null }
    staleProbes.forEach { it.rebuildDataset() }
    return staleProbes.size
}

/**
 * The accuracy of always guessing the most common class in [targets]: the baseline a probe must beat
 * before it demonstrates anything about the probed layer. Rows are read as one-hot / softmax targets
 * (class = argmax); single-column rows are read as binary targets thresholded at 0.5.
 */
fun majorityClassProportion(targets: List<List<Double>>): Double {
    require(targets.isNotEmpty()) { "Cannot compute a majority baseline from empty targets" }
    val classCounts = targets.groupingBy { row ->
        if (row.size == 1) if (row[0] > 0.5) 1 else 0 else row.indices.maxBy { row[it] }
    }.eachCount()
    return classCounts.values.max().toDouble() / targets.size
}

/**
 * Creates a random-label control for this probe: a second [Probe] with the same architecture, reading
 * the same layer, whose targets are shuffled copies of this probe's targets. If the control performs
 * well the original probe's accuracy reflects memorization capacity, not information in the probed
 * layer. Placed below this probe's readout path.
 */
context(Network)
fun Probe.createShuffledControl(): Probe {
    val hiddenSizes = layers.filter { it !== inputLayer && it !== outputLayer }.map { it.size }
    val control = buildProbe(
        probedModel = probedModel,
        inputLayer = inputLayer,
        readoutSize = outputLayer.size,
        readoutLabels = (outputLayer as? NeuronArray)?.labelArray,
        hiddenSizes = hiddenSizes,
        label = "$displayName shuffled control",
        offset = point(
            outputLayer.locationX - inputLayer.locationX - hiddenSizes.size * 350.0,
            outputLayer.locationY - inputLayer.locationY + 300.0
        )
    )
    control.trainingSet = shuffledTargetsCopy(trainingSet)
    control.testingSet = shuffledTargetsCopy(testingSet)
    control.targetDescription = "Shuffled-label control for $displayName"
    control.stale = stale
    control.trainerConfig.learningRate = trainerConfig.learningRate
    control.trainerConfig.computeAccuracy = trainerConfig.computeAccuracy
    control.trainerConfig.testConfiguration.enabled = trainerConfig.testConfiguration.enabled
    return control
}

private fun shuffledTargetsCopy(dataset: TrainingDataset) = TrainingDataset(
    inputs = dataset.inputs.map { it.toMutableList() }.toMutableList(),
    targets = dataset.targets.shuffled().map { it.toMutableList() }.toMutableList(),
    inputSize = dataset.inputSize,
    targetSize = dataset.targetSize
)

/**
 * Editable options for probe creation, for use with a creation dialog (see `createEditorDialog`).
 * The probe task (readout size, labels, and target derivation) is fixed by whoever opens the dialog;
 * these are the user-tunable knobs.
 */
class ProbeCreator(label: String = "Probe") : EditableObject {

    @UserParameter(label = "Label", order = 10)
    var label: String = label

    @UserParameter(
        label = "Hidden layer sizes",
        description = "Comma-separated sizes of optional hidden layers, e.g. \"20, 10\". " +
                "Leave empty for a linear probe (recommended: added capacity confounds what the probe measures).",
        order = 20
    )
    var hiddenSizes: String = ""

    override val name = "Create Probe"

    fun parseHiddenSizes(): List<Int> = hiddenSizes
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }
}
