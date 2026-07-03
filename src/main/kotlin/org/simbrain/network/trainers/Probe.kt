package org.simbrain.network.trainers

import kotlinx.coroutines.Dispatchers
import org.simbrain.network.core.*
import org.simbrain.network.events.ProbeEvents

/**
 * A [SupervisedModel] used as a linear probe (see [createProbe]): a readout trained on activations
 * harvested from [probedModel], a layer of a host network, to test what information that layer
 * represents.
 *
 * [probedModel] is the host layer being probed. For [Layer] probes it is the same object as
 * [inputLayer]; for [TensorLayer] probes the input layer is a flatten array bridging the tensor to
 * the probe (see the [TensorLayer] overload of [createProbe]).
 *
 * The probe tracks staleness: harvested activations are a snapshot of the host's representations, so
 * whenever weights upstream of the probed layer change (host training or randomization), [stale]
 * is set and the datasets should be re-harvested before further probe training.
 */
class Probe(
    val probedModel: LocatableModel,
    inputLayer: Layer,
    outputLayer: Layer,
) : SupervisedModel(inputLayer, outputLayer) {

    @Transient
    override val events: ProbeEvents = ProbeEvents()

    /**
     * Canvas text displaying the probed layer, the current prediction, and staleness; kept in sync
     * by [updateCustomInfo]. Position and spacing are user-editable via its settings dialog and
     * serialized with the probe.
     */
    override val customInfo = InfoText("").apply { position = InfoText.Position.ABOVE_INTERACTION_BOX }

    /**
     * Human-readable provenance of the targets, e.g. "Has loop: digit in {0, 6, 8, 9}, derived from
     * MNIST labels".
     */
    var targetDescription: String = ""

    /**
     * True when weights upstream of the probed layer have changed since the datasets were harvested,
     * meaning the harvested activations no longer reflect the host's current representations.
     */
    var stale: Boolean = false
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                events.stalenessChanged.fire()
                updateCustomInfo()
            }
        }

    /**
     * Re-harvests this probe's datasets. Registered by whoever created the probe (typically a
     * simulation, which knows the host network and source data). Not serialized; must be
     * re-registered each time the simulation is run.
     */
    @Transient
    var datasetRebuilder: (suspend () -> Unit)? = null
        set(value) {
            field = value
            updateCustomInfo()
        }

    /**
     * True while [rebuildDataset] is running, so the canvas info text can show progress.
     */
    @Transient
    var rebuilding: Boolean = false
        private set

    @Transient
    private var deleting = false

    @Transient
    private var deletionCascade: List<NetworkModel>? = null

    init {
        upstreamWeightModels().forEach { model ->
            model.events.updated.on(Dispatchers.Default) { stale = true }
        }
        // A probed tensor stage is outside this model's layer chain, so the SupervisedModel
        // deleted-listeners don't cover it. Safety net for deletions that bypass
        // Network.deleteModels (which captures this cascade for undo itself).
        if (probedModel !== inputLayer) {
            probedModel.events.deleted.on(Dispatchers.Default) { delete() }
        }
        outputLayer.events.updated.on(Dispatchers.Default) { updateCustomInfo() }
        updateCustomInfo()
    }

    /**
     * The readout's winning label with its activation, or null if the readout has no labels.
     */
    fun predictedLabel(): Pair<String, Double>? {
        val readout = outputLayer as? NeuronArray ?: return null
        val labels = readout.labelArray ?: return null
        val activations = readout.activationArray
        if (activations.isEmpty()) return null
        val winner = activations.indices.maxBy { activations[it] }
        return labels.getOrNull(winner)?.let { it to activations[winner] }
    }

    fun updateCustomInfo() {
        customInfo.text = buildString {
            append("Probing ${probedModel.displayName}")
            predictedLabel()?.let { (label, confidence) ->
                append("\nPredicted: $label (${"%.2f".format(confidence)})")
            }
            when {
                rebuilding -> append("\nRebuilding dataset...")
                stale -> append(if (datasetRebuilder != null) "\nStale: click to rebuild" else "\nStale: dataset needs rebuild")
            }
        }
        events.customInfoUpdated.fire()
    }

    suspend fun rebuildDataset() {
        datasetRebuilder?.let {
            rebuilding = true
            updateCustomInfo()
            try {
                it()
                stale = false
            } finally {
                rebuilding = false
                updateCustomInfo()
            }
        }
    }

    /**
     * A probe owns its readout path: every model in its chain except the probed host layer was
     * created for the probe (see [createProbe]) and has no meaning without it, so deleting the
     * probe deletes them too. For tensor probes that includes the flatten input array, whose
     * deletion cascades to its [FlattenConnector].
     *
     * Deleted events are barriers ([org.simbrain.util.FlowEvents.AwaitableEvent]): handlers run
     * inline within each fire, so the [SupervisedModel] listeners on owned models re-enter this
     * method in the middle of its own cascade. The [deleting] flag turns those re-entries into
     * no-ops (a handler discards the return value anyway); the memoized cascade is for callers
     * arriving after completion — in particular the [Network.deleteModels] sweep, which records
     * the full list for undo when an inline handler, whose result is discarded, ran the cascade.
     */
    override suspend fun delete(): List<NetworkModel> {
        deletionCascade?.let { return it }
        if (deleting) return emptyList()
        deleting = true
        try {
            return buildList {
                addAll(super.delete())
                val ownsInputLayer = probedModel !== inputLayer
                layers.filter { ownsInputLayer || it !== inputLayer }.forEach { addAll(it.delete()) }
            }.also { deletionCascade = it }
        } finally {
            deleting = false
        }
    }

    override suspend fun afterRestore(context: Any?) {
        super.afterRestore(context)
        deletionCascade = null
    }

    /**
     * Recomputes this probe's prediction from the probed layer's current activations. Interactive
     * host forward passes (e.g. stepping rows in the host's trainer dialog) update only the host's
     * own layers, so callers use this to keep the readout in sync; see [probesReading]. For tensor
     * probes the flatten input array is pulled from the probed stage first. If the host has been
     * retrained since harvest the prediction uses stale probe weights, which [stale] already flags.
     */
    context(Network)
    fun refreshOutput() {
        if (probedModel !== inputLayer) {
            inputLayer.accumulateInputs()
            inputLayer.update()
        }
        forwardPass()
    }

    /**
     * All weight-bearing models (weight matrices, synapse groups, tensor connectors, flatten
     * connectors) upstream of the probe's input layer in the host network. Changes to any of these
     * invalidate harvested activations. The probe's own weight matrices are downstream and excluded.
     */
    private fun upstreamWeightModels(): Set<NetworkModel> {
        val result = mutableSetOf<NetworkModel>()
        val visited = mutableSetOf<NetworkModel>()
        fun visit(node: NetworkModel) {
            if (!visited.add(node)) return
            when (node) {
                is Layer -> {
                    node.incomingConnectors.forEach { result.add(it); visit(it.source) }
                    (node as? NeuronArray)?.incomingFlattenConnectors?.forEach { result.add(it); visit(it.source) }
                    (node as? NeuronCollection)?.incomingSgs?.forEach { result.add(it); visit(it.source) }
                }
                is TensorLayer -> node.incomingTensorConnectors.forEach { result.add(it); visit(it.source) }
            }
        }
        visit(inputLayer)
        return result
    }

    override fun toString(): String = buildString {
        append(super.toString())
        append("\nProbing: ${probedModel.displayName}")
        if (targetDescription.isNotBlank()) append("\nTargets: $targetDescription")
        if (stale) append("\nDataset is stale")
    }
}
