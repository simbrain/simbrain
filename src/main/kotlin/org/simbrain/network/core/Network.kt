package org.simbrain.network.core

import kotlinx.coroutines.*
import org.simbrain.network.events.NetworkEvents
import org.simbrain.network.gui.PlacementManager
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.LOG_10
import org.simbrain.util.SimpleIdManager
import org.simbrain.util.UpdateAction
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.updater.PerformanceMonitor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.random.Random


/**
 * <b>Network</b> provides core neural network functionality and is the main neural network model object. The core
 * data structure is a [NetworkModelList] that associates classes of [NetworkModel] with linked hash sets of
 * instances of those types.
 *
 * To add models, use [Network.addNetworkModelAsync] and friends.
 *
 * To remove models use [Network.getModels] and call .delete() on the resulting models. Get models can be called with
 * an argument to filter by model type, e.g., getModels(Neuron.class)
 *
 * For details about network update see https://docs.simbrain.net/docs/network/updateLogic.html
 *
 * Note that much of the logic of the updates happens in [Layer.accumulateInputs] or [Neuron.accumulateInputs], and in [Connector.updatePSR] or [Synapse.updatePSR]
 *
 */
class Network: CoroutineScope, EditableObject {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    /**
     * Two types of time used in simulations.
     */
    enum class TimeType {
        /**
         * Network update iterations are time-steps.
         */
        DISCRETE,

        /**
         * Simulation of real time. Each updates advances time by length.
         */
        CONTINUOUS
    }

    /**
     * Handle network events.
     */
    @Transient
    var events = NetworkEvents()
        private set

    /**
     * Main data structure containing all [NetworkModel]s: neurons, synapses, etc.
     */
    private val networkModels = NetworkModelList()

    /**
     * Encodes all parent-child relationships between NetworkModels.
     * For example, each Neuron in a NeuronGroup is mapped to its to parent NeuronGroup
     * Needed for undo / redo.
     */
    val childToParentMap = mutableMapOf<NetworkModel, NetworkModel>()

    @Transient
    var updateManager = NetworkUpdateManager(this)

    /**
     * In iterations or msec.
     */
    var time = 0.0
        private set

    /**
     * Time step.
     */
    @UserParameter("Time Step", increment = .1, minimumValue = 0.0, order = 10)
    var timeStep = NetworkPreferences.defaultTimeStep

    /**
     * Whether this is a discrete or continuous time network.
     */
    @UserParameter("Time Type", description = "Whether to display iterations or time (display property only; " +
            "does not impact logical update)", order = 20)
    var timeType = TimeType.DISCRETE

    /**
     * Whether network has been updated yet; used by thread.
     */
    @Transient
    private var updateCompleted = AtomicBoolean(false)

    /**
     * Manage ids for all network elements.
     */
    @Transient
    var idManager = SimpleIdManager({ cls -> networkModels.getRawModelSet(cls).size + 1 })
        private set

    /**
     * Manages placement of new nodes, groups, etc.
     */
    @Transient
    var placementManager = PlacementManager()

    /**
     * Returns a linked hash set of models of the specified type.
     */
    fun <T : NetworkModel> getModels(cls: Class<T>) = networkModels[cls]

    /**
     * Returns a linked hash set of models of a type specified using a generic.
     */
    inline fun <reified T : NetworkModel> getModels() = getModels(T::class.java)

    /**
     * Returns a flattened list of all network models.
     */
    val allModels get() = networkModels.all
    val allModelsDeep get() = networkModels.deepAll

    /**
     * Returns a list of network models in the order needed to reconstruct a network properly. Example: nodes must be
     * added before synapses which refer to them.
     */
    val modelsInReconstructionOrder get() = networkModels.allInUpdatingOrder

    var randomSeed = Random.nextLong()
        set(value) {
            field = value
            random = Random(value)
        }

    @Transient
    var random = Random(randomSeed)
        private set

    private var shouldUpdateTimeType = true

    private fun updateInternal(name: String) {
        // Main update
        updateManager.actionList.forEach {
            runBlocking {
                PerformanceMonitor.record(it, "${name}:${it.description}") {
                    it.run()
                }
            }
        }

        if (shouldUpdateTimeType) {
            updateTimeType()
            shouldUpdateTimeType = false
        }

        updateTime()
        setUpdateCompleted(true)
    }

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    @JvmOverloads
    fun update(name: String = "") {
        updateInternal(name)
        events.updated.fireAndBlock()
    }

    suspend fun updateSuspend(name: String = "") {
        updateInternal(name)
        events.updated.fire().await()
    }

    /**
     * Update the priority list used for priority based update.
     */
    private fun updatePriorityList() {
        networkModels.updatePriorityList()
    }

    /**
     * This function is used to update the neuron and sub-network activation values if the user chooses to set different
     * priority values for a subset of neurons and sub-networks. The priority value determines the order in which the
     * neurons and sub-networks get updated - smaller priority value elements will be updated before larger priority
     * value elements.
     */
    fun updateModelsByPriority() {
        networkModels.allInPriorityOrder.forEach {
            it.accumulateInputs()
            it.update()
        }
    }

    /**
     * Default asynchronous update method called by [org.simbrain.network.update_actions.BufferedUpdate].
     */
    suspend fun bufferedUpdate()  = coroutineScope {
        networkModels.all.forEach { it.accumulateInputs() }
        networkModels.all.forEach { it.update() }
    }

    /**
     * Set the activation level of all neurons to zero.
     */
    fun clearActivations() {
        flatNeuronList.forEach(Neuron::clear)
    }

    val freeNeurons get() = networkModels.get<Neuron>()

    val freeSynapses get() = networkModels.get<Synapse>()

    /**
     * Create "flat" list of neurons, which includes the top-level neurons plus all group neurons.
     *
     * @return the flat list
     */
    val flatNeuronList: List<Neuron>
        get() = buildList {
            addAll(networkModels.get<Neuron>())
            for (neuronGroup in networkModels.all.filterIsInstance<NeuronGroup>()) {
                addAll(neuronGroup.neuronList)
            }
            for (subnetwork in networkModels.get<Subnetwork>()) {
                addAll(subnetwork.modelList.get<NeuronGroup>().flatMap { it.neuronList })
            }
        }

    /**
     * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses.
     *
     * @return the flat list
     */
    val flatSynapseList: List<Synapse>
        get() = sequence {
            yieldAll(networkModels.get<Synapse>())
            yieldAll(networkModels.get<SynapseGroup>().flatMap { sg -> sg.synapses })
            yieldAll(networkModels.get<Subnetwork>().flatMap { subnetwork ->
                subnetwork.modelList.get<SynapseGroup>().flatMap { it.synapses }
            })
        }.toList()

    /**
     * Returns a list of all neuron groups including those in subnetworks.
     */
    val flatNeuronGroupList: List<NeuronGroup>
        get() = sequence {
            yieldAll(networkModels.get<NeuronGroup>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.modelList.get() })
        }.toList()

    /**
     * Returns a list of all synapse groups including those in subnetworks.
     */
    val flatSynapseGroupList: List<SynapseGroup>
        get() = sequence {
            yieldAll(networkModels.get<SynapseGroup>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.modelList.get() })
        }.toList()

    /**
     * Returns a list of all weight matrices including those in subnetworks.
     */
    val flatWeightMatrixList: List<WeightMatrix>
        get() = sequence {
            yieldAll(networkModels.get<WeightMatrix>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.modelList.get() })
        }.toList()

    private fun assignId(model: NetworkModel) {
        model.id = idManager.getAndIncrementId(model.javaClass)
        when (model) {
            is NeuronGroup -> model.neuronList.forEach { assignId(it) }
            is SynapseGroup -> model.synapses.forEach { assignId(it) }
            is Subnetwork -> model.modelList.all.forEach { assignId(it) }
        }
    }

    /**
     * Add a new [NetworkModel]. All network models must be added using this method.
     * For best results call with `?.await()` when possible.
     */
    @JvmOverloads
    fun addNetworkModelAsync(model: NetworkModel, usePlacementManager: Boolean = true, useAutoAssignedId: Boolean = true): Deferred<Boolean>? {
        if (model.shouldAdd()) {
            if (useAutoAssignedId) {
                assignId(model)
            }
            networkModels.add(model)
            if (usePlacementManager && model is LocatableModel && model.shouldBePlaced) {
                placementManager.placeObject(model)
            }
            (model as? LocatableModel)?.let { locatableModel ->
                locatableModel.events.locationChanged.on {
                    events.boundsChanged.fire()
                }
            }
            model.events.deleted.on(wait = true) {
                networkModels.remove(it)
                childToParentMap.remove(it)
                events.modelRemoved.fire(it).join()
                updatePriorityList()
            }
            val deferred = events.modelAdded.fire(model)
            when(model) {
                is AbstractNeuronCollection -> {
                    model.neuronList.forEach { childToParentMap[it] = model }
                    model.neuronList.forEach { n ->
                        n.events.deleted.on(wait = true) { childToParentMap.remove(n) }
                    }
                }
                is SynapseGroup -> {
                    model.synapses.forEach { childToParentMap[it] = model }
                    model.synapses.forEach { s ->
                        s.events.deleted.on(wait = true) { childToParentMap.remove(s) }
                    }
                }
                is Subnetwork -> {
                    model.modelList.all.forEach { childToParentMap[it] = model }
                    model.modelList.all.forEach { m ->
                        m.events.deleted.on(wait = true) { childToParentMap.remove(m) }
                    }
                }
                is SupervisedModel -> {
                    model.layers.forEach { childToParentMap[it] = model }
                    model.weightMatrices.forEach { childToParentMap[it] = model }
                    model.synapseGroups.forEach { childToParentMap[it] = model }
                    model.layers.forEach { l ->
                        l.events.deleted.on(wait = true) { childToParentMap.remove(l) }
                    }
                    model.weightMatrices.forEach { m ->
                        m.events.deleted.on(wait = true) { childToParentMap.remove(m) }
                    }
                    model.synapseGroups.forEach { sg ->
                        sg.events.deleted.on(wait = true) { childToParentMap.remove(sg) }
                    }
                }
            }
            return deferred
        }
        return null
    }

    suspend fun addNetworkModel(model: NetworkModel, usePlacementManager: Boolean = true, useAutoAssignedId: Boolean = true) {
        addNetworkModelAsync(model, usePlacementManager, useAutoAssignedId)?.await()
    }

    /**
     * Returns the precision of the current time step.
     *
     * @return the precision of the current time step.
     */
    private fun getTimeStepPrecision(): Int = ceil(ln(timeStep) / LOG_10).toInt().let {
        if (it < 0) {
            abs(it) + 1
        } else {
            0
        }
    }

    /**
     * @return list of deleted models (needed for undo /redo)
     */
    suspend fun deleteModels(networkModels: List<NetworkModel>): List<NetworkModel> {

        fun isLastChildOfParent(childToParentMap: Map<NetworkModel, NetworkModel>, model: NetworkModel): Boolean {
            return childToParentMap[model]?.let { parent ->
                childToParentMap.values.count { it == parent } == 1
            } == true
        }

        return buildList {

            suspend fun deleteModel(childToParentMap: MutableMap<NetworkModel, NetworkModel>, model: NetworkModel) {

                val parent = childToParentMap[model]
                if (isLastChildOfParent(childToParentMap, model) || parent is SupervisedModel) {
                    parent?.let { parent ->
                        addAll(parent.delete())
                        // If (1) deleting a supervised model because one of its children models has been deleted or
                        //    (2) deleting a neuron collection because its last node was deleted
                        //  then the child model must be manually deleted
                        if (parent is NeuronCollection || parent is SupervisedModel) {
                            addAll(model.delete())
                        }
                    }
                } else {
                    addAll(model.delete())
                }
                // Remove all children of deleted parents from the map
                childToParentMap.entries.filter { it.value == parent }.map { it.key }.forEach {
                    childToParentMap.remove(it)
                }
            }

            networkModels.forEach {
                if (it is Subnetwork) {
                    it.modelList.all.forEach { model -> deleteModel(it.childToParentMap, model) }
                }
                deleteModel(childToParentMap, it)
            }
        }
    }

    /**
     * Returns a copy of this network based on its xml rep.
     */
    fun copy(): Network {
        val xmlRepresentation = getNetworkXStream().toXML(this)
        return getNetworkXStream().fromXML(xmlRepresentation) as Network
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private fun readResolve(): Any {

        job = SupervisorJob()

        coroutineContext = Dispatchers.Default + job

        events = NetworkEvents()

        updateManager = NetworkUpdateManager(this)

        placementManager = PlacementManager()

        updateCompleted = AtomicBoolean(false)

        // Initialize update manager
        networkModels.allInUpdatingOrder.forEach { model ->
            model.events.deleted.on(wait = true) {
                networkModels.remove(it)
                events.modelRemoved.fire(it)
            }
        }
        
        // Set up neuron listeners for all neuron collections after deserialization
        networkModels.allInUpdatingOrder.filterIsInstance<AbstractNeuronCollection>().forEach { collection ->
            collection.setupNeuronListeners()
        }
        
        idManager = SimpleIdManager ({ cls -> networkModels.getRawModelSet(cls).size + 1 })
        return this
    }

    /**
     * Returns the current number of iterations.
     *
     * @return the number of update iterations which have been run since the network was created.
     */
    val iterations: Long get() = (time / timeStep).toLong()

    /**
     * string version of time, with units.
     */
    val timeLabel: String
        get() = if (timeType == TimeType.DISCRETE) {
            "$iterations iterations"
        } else {
            "${SimbrainMath.roundDouble(time, getTimeStepPrecision() + 1)} msec"
        }

    /**
     * If there is a single continuous neuron in the network, consider this a continuous network.
     */
    private fun updateTimeType() {
        timeType = TimeType.DISCRETE
        if (flatNeuronList.any { it.timeType == TimeType.CONTINUOUS }) {
            timeType = TimeType.CONTINUOUS
        }
    }

    /**
     * Increment the time counter, using a different method depending on whether this is a continuous or discrete.
     * network.
     */
    fun updateTime() {
        time += timeStep
    }

    fun resetTime() {
        time = 0.0
        flatNeuronList.forEach {
            (it.dataHolder as? SpikingScalarData)?.lastSpikeTime = Double.MIN_VALUE
        }
        networkModels.get<NeuronArray>().forEach {
            (it.dataHolder as? SpikingMatrixData)?.lastSpikeTimes?.fill(Double.MIN_VALUE)
        }
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before updating again.
     *
     * @return whether the network has been updated or not
     */
    fun isUpdateCompleted(): Boolean {
        return updateCompleted.get()
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before updating again.
     *
     * @param b whether the network has been updated or not.
     */
    fun setUpdateCompleted(b: Boolean) {
        updateCompleted.set(b)
    }

    override fun toString(): String {
        val printThreshold = 20

        fun formatGroup(label: String, models: List<NetworkModel>): String? {
            if (models.isEmpty()) return null
            return if (models.size <= printThreshold) {
                "$label:\n" + models.joinToString("\n") { model ->
                    model.toString().lines().joinToString("\n") { "  $it" }
                }
            } else {
                "$label: ${models.size} total"
            }
        }

        return buildString {
            appendLine("------Network------")

            allModels.groupBy { it::class.simpleName ?: "Unknown" }
                .mapNotNull { (className, models) -> formatGroup(className, models) }
                .forEach { appendLine(it) }
        }
    }

    /**
     * Forward to [NetworkUpdateManager.addAction]
     */
    fun addUpdateAction(action: UpdateAction) {
        updateManager.addAction(action)
    }

    /**
     * Forward to [NetworkUpdateManager.removeAction]
     */
    fun removeUpdateAction(action: UpdateAction) {
        updateManager.removeAction(action)
    }

    /**
     * Adds a list of network elements to this network. Used in copy / paste.
     *
     * @param toAdd list of objects to add.
     */
    @JvmOverloads
    fun addNetworkModelsAsync(toAdd: List<NetworkModel>, usePlacementManager: Boolean = true, useAutoAssignedId: Boolean = true) = toAdd.mapNotNull { addNetworkModelAsync(it, usePlacementManager, useAutoAssignedId) }

    suspend fun addNetworkModels(toAdd: List<NetworkModel>, usePlacementManager: Boolean = true, useAutoAssignedId: Boolean = true) {
        addNetworkModelsAsync(toAdd, usePlacementManager, useAutoAssignedId).awaitAll()
    }

    /**
     * Var arg version of addNetworkModels.
     *
     * Ex: addNetworkModels(synapse1, synapse2, neuron1, neuron2, ...)
     */
    fun addNetworkModelsAsync(vararg toAdd: NetworkModel, usePlacementManager: Boolean = true, useAutoAssignedId: Boolean = true) = toAdd.mapNotNull { addNetworkModelAsync(it, usePlacementManager, useAutoAssignedId) }

    suspend fun addNetworkModels(vararg toAdd: NetworkModel, usePlacementManager: Boolean = true, useAutoAssignedId: Boolean = true) {
        addNetworkModelsAsync(*toAdd, usePlacementManager = usePlacementManager, useAutoAssignedId = useAutoAssignedId).awaitAll()
    }

    fun selectModels(models: List<NetworkModel>) {
        events.selected.fire(models)
    }

}