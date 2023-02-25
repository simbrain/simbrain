package org.simbrain.network.core

import kotlinx.coroutines.*
import org.simbrain.network.LocatableModel
import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.ConnectionSelector
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.connections.Sparse
import org.simbrain.network.events.NetworkEvents2
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.gui.PlacementManager
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.util.*
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.updater.PerformanceMonitor
import org.simbrain.workspace.updater.UpdateAction
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln

/**
 * The initial time-step for the network.
 */
private val DEFAULT_TIME_STEP = SimbrainPreferences.getDouble("networkDefaultTimeStep")

/**
 * Constant value for Math.log(10); used to approximate log 10.
 */
private val LOG_10 = ln(10.0)

/**
 * <b>Network</b> provides core neural network functionality and is the main neural network model object. The core
 * data structure is a [NetworkModelList] that associates classes of [NetworkModel] with linked hash sets of
 * instances of those types.
 *
 * To add models, use [Network.addNetworkModel] and friends.
 *
 * To remove models use [Network.getModels] and call .delete() on the resulting models. Get models can be called with
 * an argument to filter by model type, e.g getModels(Neuron.class)
 */
class Network: CoroutineScope {

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
    var events = NetworkEvents2()
        private set

    /**
     * Main data structure containing all [NetworkModel]s: neurons, synapses, etc.
     */
    private val networkModels = NetworkModelList()

    /**
     * The update manager for this network.
     */
    val updateManager = NetworkUpdateManager(this)

    /**
     * Connection strategy for connecting free neurons.
     */
    val neuronConnector = ConnectionSelector(Sparse())

    /**
     * Randomizer for all free weights, regardless of polarity. Applying it can change the polarity of a weight.
     */
    val weightRandomizer = ProbabilityDistribution.Randomizer(UniformRealDistribution(-1.0, 1.0))

    /**
     * Randomizer for free excitatory weights.
     */
    val excitatoryRandomizer = ProbabilityDistribution.Randomizer(UniformRealDistribution(0.0, 1.0))

    /**
     * Randomizer for free inhibitory weights.
     */
    val inhibitoryRandomizer = ProbabilityDistribution.Randomizer(UniformRealDistribution(-1.0, 0.0))

    /**
     * In iterations or msec.
     */
    var time = 0.0
        private set(i) {
            field = i
        }

    /**
     * Time step.
     */
    var timeStep = DEFAULT_TIME_STEP

    /**
     * Whether this is a discrete or continuous time network.
     */
    private var timeType = TimeType.DISCRETE

    /**
     * Whether network has been updated yet; used by thread.
     */
    @Transient
    private var updateCompleted = AtomicBoolean(false)

    /**
     * List of neurons sorted by their update priority. Used in priority based update.
     * Lower numbers updated first, as in first priority, second priority, etc.
     */
    @Transient
    var prioritySortedNeuronList: ArrayList<Neuron> = ArrayList()
        private set

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

    /**
     * Returns a list of network models in the order needed to reconstruct a network properly. Example: nodes must be
     * added before synapses which refer to them.
     */
    val modelsInReconstructionOrder get() = networkModels.allInReconstructionOrder

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    @JvmOverloads

    fun update(name: String = "") {

        // Main update
        updateManager.actionList.forEach {
            runBlocking {
                PerformanceMonitor.record(it, "${name}:${it.description}") {
                    it.run()
                }
            }
        }

        updateTime()
        setUpdateCompleted(true)
        events.updated.fireAndBlock()
    }

    /**
     * Update the priority list used for priority based update.
     */
    fun updatePriorityList() {
        // TODO: Uses flat neuron list, but does this make sense? NeuronGroups should handle their own update orders.
        prioritySortedNeuronList = ArrayList(flatNeuronList)
        resortPriorities()
    }

    /**
     * Resort the neurons according to their update priorities.
     */
    fun resortPriorities() {
        prioritySortedNeuronList.sortWith { neuron1, neuron2 ->
            val priority1 = neuron1.updatePriority
            val priority2 = neuron2.updatePriority
            priority1.compareTo(priority2)
        }
    }

    /**
     * This function is used to update the neuron and sub-network activation values if the user chooses to set different
     * priority values for a subset of neurons and sub-networks. The priority value determines the order in which the
     * neurons and sub-networks get updated - smaller priority value elements will be updated before larger priority
     * value elements.
     */
    fun updateNeuronsByPriority() {
        for (neuron in prioritySortedNeuronList) {
            neuron.updateInputs()
            neuron.update()
        }
    }

    /**
     * Update all network models except for neurons.
     * Obviously a temporary method!
     */
    fun updateAllButNeurons() {
        // TODO: Temporary function until we create a generalized priority based update
        listOf(NeuronGroup::class.java,
        NeuronCollection::class.java,
        NeuronArray::class.java,
        Connector::class.java,
        SynapseGroup::class.java,
        Subnetwork::class.java,
        Synapse::class.java)
            .flatMap { networkModels[it] }
            .forEach {nm ->
                nm.updateInputs()
                nm.update()
            }
    }

    /**
     * Default asynchronous update method called by [org.simbrain.network.update_actions.BufferedUpdate].
     */
    fun bufferedUpdate() {
        networkModels.all.forEach { it.updateInputs() }
        networkModels.all.forEach { it.update() }
    }

    suspend fun asyncBufferedUpdate()  = coroutineScope {
        networkModels.getAsyncModels().map { async { it.updateInputs() } }.awaitAll()
        networkModels.getNonAsyncModels().forEach { it.updateInputs() }
        networkModels.getAsyncModels().map { async { it.update() } }.awaitAll()
        networkModels.getNonAsyncModels().forEach { it.update() }
    }

    /**
     * Set the activation level of all neurons to zero.
     */
    fun clearActivations() {
        flatNeuronList.forEach(Neuron::clear)
    }

    /**
     * Find a neuron with a given string id.
     *
     * @param id id to search for.
     * @return neuron with that id, null otherwise
     */
    fun getFreeNeuron(id: String?): Neuron? = networkModels.get<Neuron>().firstOrNull {
        it.id.equals(id, ignoreCase = true)
    }

    /**
     * Find a synapse with a given string id.
     *
     * @param id id to search for.
     * @return synapse with that id, null otherwise
     */
    fun getFreeSynapse(id: String?): Synapse? = networkModels.get<Synapse>().firstOrNull {
        it.id.equals(id, ignoreCase = true)
    }

    /**
     * Create "flat" list of neurons, which includes the top-level neurons plus all group neurons.
     *
     * @return the flat list
     */
    val flatNeuronList: List<Neuron>
        get() = sequence {
            yieldAll(networkModels.get<Neuron>())
            for (neuronGroup in networkModels.get<NeuronGroup>()) {
                yieldAll(neuronGroup.neuronList)
            }
            for (subnetwork in networkModels.get<Subnetwork>()) {
                yieldAll(subnetwork.modelList.get<NeuronGroup>().flatMap { it.neuronList })
            }
        }.toList()

    /**
     * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses.
     *
     * @return the flat list
     */
    val flatSynapseList: List<Synapse>
        get() = sequence {
            yieldAll(networkModels.get<Synapse>())
            yieldAll(networkModels.get<SynapseGroup2>().flatMap { sg -> sg.synapses })
            yieldAll(networkModels.get<Subnetwork>().flatMap { subnetwork ->
                subnetwork.modelList.get<SynapseGroup>().flatMap { it.allSynapses }
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

    /**
     * Add a new [NetworkModel]. All network models MUST be added using this method.
     */
    fun addNetworkModel(model: NetworkModel): Job? {
        if (model.shouldAdd()) {
            model.id = idManager.getAndIncrementId(model.javaClass)
            networkModels.add(model)
            if (model is LocatableModel && model.shouldBePlaced) {
                placementManager.placeObject(model)
            }
            model.events.deleted.on(wait = true) {
                networkModels.remove(it)
                events.modelRemoved.fireAndForget(it)
            }
            val job = events.modelAdded.fireAndSuspend(model)
            if (model is Neuron) updatePriorityList()
            return job
        }
        return null
    }

    /**
     * Create a [NeuronCollection] from a provided list of neurons
     */
    fun createNeuronCollection(neuronList: List<Neuron>): NeuronCollection? {

        // Filter out free neurons (a neuron is free if its parent group is null)
        val freeNeurons: List<Neuron> = neuronList
            // .filter(n -> n.getParentGroup() == null)  // TODO
            .toList()

        // Only make the neuron collection if some neurons have been selected
        if (freeNeurons.isNotEmpty()) {
            // Make the collection
            val nc = NeuronCollection(this, freeNeurons)

            if (nc.shouldAdd()) {
                nc.label = idManager.getProposedId(nc.javaClass)
                return nc
            }
        }
        return null
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
     * Returns a copy of this network based on its xml rep.
     *
     * @return the copied network.
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

        placementManager = PlacementManager()

        events = NetworkEvents2()
        updateCompleted = AtomicBoolean(false)
        updatePriorityList();

        // Initialize update manager
        updateManager.postOpenInit()
        networkModels.allInReconstructionOrder.forEach { it.postOpenInit() }
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
    fun updateTimeType() {
        timeType = TimeType.DISCRETE
        for (n in flatNeuronList) {
            if (n.timeType == TimeType.CONTINUOUS) {
                timeType = TimeType.CONTINUOUS
            }
        }
    }

    /**
     * Increment the time counter, using a different method depending on whether this is a continuous or discrete.
     * network.
     */
    fun updateTime() {
        time += timeStep
    }

    /**
     * Reset time. Note that last spike time computations for spiking networks will be incorrect.
     */
    fun resetTime() {
        time = 0.0
        // TODO: Earlier code from Zoë used to adjust last spike times on spiking neurons.
        // Not a common use case so commented out for now. If put in a unit test needed.
        // if (i < time) {
        //     for (n in flatNeuronList) {
        //         val nur = n.updateRule
        //         if (nur.isSpikingNeuron) {
        //             val snur = nur as SpikingNeuronUpdateRule
        //             val diff: Double = i - (time - snur.lastSpikeTime)
        //             snur.setLastSpikeTime(if (diff < 0) 0.0 else diff)
        //         }
        //     }
        // }

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

    override fun toString(): String =
        "------Network------\n" + networkModels

    /**
     * Returns a neuron with a matching label.  If more than one
     * neuron has a matching label, the first found is returned.
     *
     * @param label label of neuron to search for
     * @return matched Neuron, if any
     */
    fun getNeuronByLabel(label: String): Neuron? = flatNeuronList.firstOrNull {
        it.label.equals(label, ignoreCase = true)
    }

    /**
     * Returns a neurongroup with a matching label.  If more than one
     * group has a matching label, the first one found is returned.
     *
     * @param label label of NeuronGroup to search for
     * @return matched NeuronGroup, if any
     */
    fun getNeuronGroupByLabel(label: String): NeuronGroup? = flatNeuronGroupList.firstOrNull {
        it.label.equals(label, ignoreCase = true)
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
    fun addNetworkModels(toAdd: List<NetworkModel>): Job {
        val jobs = toAdd.mapNotNull { addNetworkModel(it) }
        return launch { jobs.joinAll() }
    }

    /**
     * Var arg version of addNetworkModels.
     *
     * Ex: addNetworkModels(synapse1, synapse2, neuron1, neuron2, ...)
     */
    fun addNetworkModels(vararg toAdd: NetworkModel) {
        toAdd.forEach { addNetworkModel(it) }
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    fun translate(offsetX: Double, offsetY: Double) {
        for (neuron in flatNeuronList) {
            neuron.offset(offsetX, offsetY)
        }
    }

    /**
     * Freeze or unfreeze all synapses in the network.
     *
     * @param freeze frozen if true; unfrozen if false
     */
    fun freezeSynapses(freeze: Boolean) {
        // Freeze synapses in synapse groups
        for (group in networkModels.get<SynapseGroup>()) {
            // TODO
            // group.setFrozen(freeze, Polarity.BOTH)
        }
        // Freeze free synapses
        for (synapse in networkModels.get<Synapse>()) {
            synapse.isFrozen = freeze
        }
    }

    val freeNeurons get() = networkModels.get<Neuron>()

    val freeSynapses get() = networkModels.get<Synapse>()

    fun addNeuron(block: Neuron.() -> Unit = { }) = Neuron(this)
        .apply(this::addNetworkModel)
        .also(block)

    fun addNeuron(x: Int, y: Int) = Neuron(this)
        .also{
            addNetworkModel(it)
            it.location = point(x,y)
        }

    fun addSynapse(source: Neuron, target: Neuron, block: Synapse.() -> Unit = { }) = Synapse(source, target)
        .apply(block)
        .also(this::addNetworkModel)

    fun addNeuronGroup(count: Int, template: Neuron.() -> Unit = { }) = NeuronGroup(this, List(count) {
        Neuron(this).apply(template)
    }).also { addNetworkModel(it) }

    fun addNeuronGroup(count: Int, location: Point2D? = null, template: Neuron.() -> Unit = { }): NeuronGroup {
        return NeuronGroup(this, List(count) {
            Neuron(this).apply(template)
        }).also {
            addNetworkModel(it)
            if (location != null) {
                val (x, y) = location
                it.setLocation(x, y)
            }
        }
    }

    fun createNeuronGroupTemplate(template: NeuronGroup.() -> Unit) = fun Network.(
        count: Int,
        template: Neuron.() -> Unit
    ) = NeuronGroup(this, List(count) {
        Neuron(this).apply(template)
    }).also { addNetworkModel(it) }

    fun <R> Network.withConnectionStrategy(
        connectionStrategy: ConnectionStrategy,
        block: NetworkWithConnectionStrategy.() -> R
    ): R {
        return NetworkWithConnectionStrategy(this, connectionStrategy).run(block)
    }

    data class NetworkWithConnectionStrategy(
        private val network: Network,
        private val connectionStrategy: ConnectionStrategy
    ) {
        fun connect(source: List<Neuron>, target: List<Neuron>): List<Synapse> {
            return connectionStrategy.connectNeurons(network, source, target)
        }
    }

    /**
     * Add a synapse group between a source and target neuron group
     *
     * @return the new synapse group
     */
    fun addSynapseGroup(source: NeuronGroup, target: NeuronGroup): SynapseGroup2 {
        val sg = SynapseGroup.createSynapseGroup(source, target)
        addNetworkModel(sg)
        return sg
    }

    /**
     * Add a neuron group at the specified location, with a specified number of neurons, layout,
     * and update rule.
     *
     * @return the new neuron group
     */
    fun addNeuronGroup(x: Double, y: Double, numNeurons: Int, layoutName: String, rule: NeuronUpdateRule):
            NeuronGroup {
        val ng = NeuronGroup(this, numNeurons)
        ng.setNeuronType(rule)
        addNetworkModel(ng)
        layoutNeuronGroup(ng, layoutName)
        ng.setLocation(x, y)
        return ng
    }

    /**
     * Add a neuron group with a specified number of neurons, layout, and neuron update rule
     *
     * @return the new neuron group
     */
    fun addNeuronGroup(x: Double, y: Double, numNeurons: Int, layoutName: String): NeuronGroup {
        return addNeuronGroup(x, y, numNeurons, layoutName, LinearRule())
    }

    /**
     * Add a new neuron group at the specified location, with a default line layout and linear neurons.
     *
     * @return the new neuron group.
     */
    fun addNeuronGroup(x: Double, y: Double, numNeurons: Int): NeuronGroup {
        return addNeuronGroup(x, y, numNeurons, "line")
    }

    /**
     * Connect source and target neuron groups with a provided connection strategy.
     *
     * @return the new synapses
     */
    fun connect(source: NeuronGroup, target: NeuronGroup, connector: ConnectionStrategy): List<Synapse?>? {
        return connector.connectNeurons(this, source.neuronList, target.neuronList)
    }

    suspend fun selectModels(models: List<NetworkModel>) {
        events.selected.fireAndSuspend(models)
    }

}
