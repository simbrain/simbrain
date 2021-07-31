package org.simbrain.network.core

import com.thoughtworks.xstream.XStream
import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.events.NetworkEvents
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.layouts.LineLayout.LineOrientation
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.util.*
import org.simbrain.util.math.SimbrainMath
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln

/**
 * <b>Network</b> provides core neural network functionality and is the main neural network model object. The core
 * data structure is a [NetworkModelList] that associates classes of [NetworkModel] with linked hash sets of
 * instances of those types.
 */
class Network {

    companion object {
        /**
         * An internal "static" id giving networks unique numbers within the same simbrain session.
         */
        private var current_id = 0
    }

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
    var events = NetworkEvents(this)
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
     * In iterations or msec.
     */
    var time = 0.0
        private set(i) {
            if (i < time) {
                for (n in flatNeuronList) {
                    val nur = n.updateRule
                    if (nur.isSpikingNeuron) {
                        val snur = nur as SpikingNeuronUpdateRule
                        val diff: Double = i - (time - snur.lastSpikeTime)
                        snur.setLastSpikeTime(if (diff < 0) 0.0 else diff)
                    }
                }
            }
            field = i
        }

    /**
     * Time step.
     */
    var timeStep = DEFAULT_TIME_STEP

    /**
     * Local thread flag for starting and stopping the network
     */
    private val _isRunning = AtomicBoolean()
    var isRunning: Boolean
        get() = _isRunning.get()
        set(value) {
            _isRunning.set(value)
        }

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
     * TODO: Resolve priority update issue. Here as a hack to make the list available to groups that want to update via
     * priorities WITHIN the group... To be resolved.
     */
    @Transient
    var prioritySortedNeuronList: ArrayList<Neuron> = ArrayList()
        private set

    /**
     * Manage ids for all network elements.
     */
    val idManager = SimpleIdManager { cls -> networkModels.unsafeGet(cls).size + 1 }

    /**
     * An optional name for the network that defaults to "Network[current_id]".
     */
    val name: String = "Network$current_id"

    /**
     * A counter for the total number of iterations run by this network.
     */
    private var iterCount = 0

    /**
     * How frequently this network should fire events.
     */
    var updateFreq = 1

    /**
     * A special flag for if the network is being run for a one-time single iteration.
     */
    var oneOffRun = false

    /**
     * Initialize the network.
     */
    init {
        current_id++
    }

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
    val allModelsInDeserializationOrder get() = networkModels.allInDeserializationOrder

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    fun update() {

        // Main update
        updateManager.invokeAllUpdates()

        updateTime()
        events.fireUpdateTimeDisplay(false)
        iterCount++
        setUpdateCompleted(true)
        events.fireUpdateCompleted()
    }

    /**
     * Update the priority list used for priority based update.
     */
    fun updatePriorityList() {
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
    fun getLooseNeuron(id: String?): Neuron? = networkModels.get<Neuron>().firstOrNull {
        it.id.equals(id, ignoreCase = true)
    }

    /**
     * Find a synapse with a given string id.
     *
     * @param id id to search for.
     * @return synapse with that id, null otherwise
     */
    fun getLooseSynapse(id: String?): Synapse? = networkModels.get<Synapse>().firstOrNull {
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
            for (neuronCollection in networkModels.get<NeuronCollection>()) {
                yieldAll(neuronCollection.neuronList)
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
    fun addNetworkModel(model: NetworkModel) {
        if (model.shouldAdd()) {
            model.id = idManager.getAndIncrementId(model.javaClass)
            networkModels.add(model)
            model.events.onDeleted{
                networkModels.remove(it)
            }
            events.fireModelAdded(model)
            if (model is Neuron) updatePriorityList()
        }
    }

    /**
     * Create a [NeuronCollection] from a provided list of neurons
     */
    fun createNeuronCollection(neuronList: List<Neuron>): NeuronCollection? {

        // Filter out loose neurons (a neuron is loose if its parent group is null)
        val loose: List<Neuron> = neuronList
            // .filter(n -> n.getParentGroup() == null)  // TODO
            .toList()

        // Only make the neuron collection if some neurons have been selected
        if (loose.isNotEmpty()) {
            // Make the collection
            val nc = NeuronCollection(this, loose)

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
     * Standard method call made to objects after they are deserialized. See: http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private fun readResolve(): Any {

        events = NetworkEvents(this)
        updateCompleted = AtomicBoolean(false)
        updatePriorityList();

        // Initialize update manager
        updateManager.postUnmarshallingInit()
        networkModels.allInDeserializationOrder.forEach { it.postUnmarshallingInit() }
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

    fun resetTime() {
        time = 0.0
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
     * Add an update action to the network' action list (the sequence of actions invoked on each iteration of the
     * network).
     *
     * @param action new action
     */
    fun addUpdateAction(action: NetworkUpdateAction?) {
        updateManager.addAction(action)
    }

    /**
     * Adds a list of network elements to this network. Used in copy / paste.
     *
     * @param toAdd list of objects to add.
     */
    fun addNetworkModels(toAdd: List<NetworkModel>) {
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
        // Freeze loose synapses
        for (synapse in networkModels.get<Synapse>()) {
            synapse.isFrozen = freeze
        }
    }

    val isRedrawTime: Boolean = oneOffRun || (iterCount % updateFreq == 0)

    val looseNeurons get() = networkModels.get<Neuron>()

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
    fun addSynapseGroup(source: NeuronGroup, target: NeuronGroup): SynapseGroup {
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
        ng.setLocation(x, y)
        layoutNeuronGroup(ng, layoutName)
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

}

/**
 * The initial time-step for the network.
 */
private val DEFAULT_TIME_STEP = SimbrainPreferences.getDouble("networkDefaultTimeStep")

/**
 * Constant value for Math.log(10); used to approximate log 10.
 */
private val LOG_10 = ln(10.0)

/**
 * If a subnetwork or synapse group has more than this many synapses, then the initial synapse visibility flag is
 * set false.
 */
@Transient
var synapseVisibilityThreshold = SimbrainPreferences.getInt("networkSynapseVisibilityThreshold")

/**
 * Items must be ordered for deserializing. For example neurons but serialized before synapses.
 */
val deserializationOrder: List<Class<out NetworkModel>> = listOf(
    Neuron::class.java,
    NeuronGroup::class.java,
    NeuronCollection::class.java,
    NeuronArray::class.java,
    Connector::class.java,
    SynapseGroup::class.java,
    Subnetwork::class.java,
    Synapse::class.java
)

/**
 * Convenience method for asynchronously updating a set of neurons, by calling each neuron's update function (which
 * sets a buffer), and then setting each neuron's activation to the buffer state.
 *
 * @param neuronList the list of neurons to be updated
 */
fun updateNeurons(neuronList: List<Neuron>) {
    // TODO: Update by priority if priority based update?
    neuronList.forEach(Neuron::updateInputs)
    neuronList.forEach(Neuron::update)
}

/**
 * Returns a reference to the synapse connecting two neurons, or null if there is none.
 *
 * @param src source neuron
 * @param tar target neuron
 * @return synapse from source to target
 */
fun getLooseSynapse(src: Neuron, tar: Neuron): Synapse? = src.fanOut[tar]

/**
 * Convenient access to a list of activations
 */
var List<Neuron?>.activations: List<Double>
    get() = map { it?.activation ?: 0.0 }
    set(values) = values.forEachIndexed { index, value ->
        this[index]?.let { neuron ->
            if (neuron.isClamped) {
                neuron.forceSetActivation(value)
            } else {
                neuron.activation = value
            }
        }
    }

var List<Neuron?>.labels: List<String>
    get() = map { it?.label ?: "" }
    set(values) = values.forEachIndexed { index, label ->
        this[index]?.let { it.label = label }
    }

var List<Neuron>.auxValues: List<Double>
    get() = map { it.auxValue }
    set(values) = values.forEachIndexed { index, value ->
        this[index].auxValue = value
    }

val List<Synapse>.lengths: List<Double>
    get() = map { it.length }

fun getNetworkXStream(): XStream {
    val xstream = Utils.getSimbrainXStream()
    xstream.registerConverter(NetworkModelListConverter())
    xstream.registerConverter(DoubleArrayConverter())
    xstream.registerConverter(MatrixConverter())
    return xstream
}

fun networkUpdateAction(description: String, longDescription: String = description, action: () -> Unit) =
    object : NetworkUpdateAction {
        override fun invoke() = action()
        override fun getDescription(): String = description
        override fun getLongDescription(): String = longDescription
    }

/**
 * Layout a neuron group.
 *
 * @param ng reference to the group
 * @param layoutName the type of layout to use: "line" (defaults to horizontal),
 * "vertical line", or "grid".  TODO: Add hex.
 */
fun layoutNeuronGroup(ng: NeuronGroup, layoutName: String) {
    if (layoutName.toLowerCase().contains("line")) {
        if (layoutName.equals("vertical line", ignoreCase = true)) {
            val lineLayout = LineLayout(50.0, LineOrientation.VERTICAL)
            ng.layout = lineLayout
        } else {
            val lineLayout = LineLayout(50.0, LineOrientation.HORIZONTAL)
            ng.layout = lineLayout
        }
    } else if (layoutName.equals("grid", ignoreCase = true)) {
        val gridLayout = GridLayout(50.0, 50.0, Math.sqrt(ng.size().toDouble()).toInt()
        )
        ng.layout = gridLayout
    }
    ng.applyLayout()
}

/**
 * Make a single source -> target neuron connection.
 *
 * @param source the source neuron
 * @param target the target neuron
 */
fun connect(source: Neuron, target: Neuron, value: Double): Synapse {
    val synapse = Synapse(source, target)
    synapse.forceSetStrength(value)
    source.network.addNetworkModel(synapse)
    return synapse
}

/**
 * Make a single source -> target neuron connection with specified upper and lower bounds for the synapses.
 */
fun connect(source: Neuron, target: Neuron, value: Double, lowerBound: Double, upperBound: Double) {
    val synapse = Synapse(source, target)
    synapse.forceSetStrength(value)
    synapse.lowerBound = lowerBound
    synapse.upperBound = upperBound
    source.network.addNetworkModel(synapse)
}

/**
 * Connect source to target with a provided learning rule and value.
 *
 * @return the new synapse
 */
fun connect(source: Neuron, target: Neuron, rule: SynapseUpdateRule, value: Double): Synapse? {
    val synapse = Synapse(source, target, rule)
    synapse.forceSetStrength(value)
    source.network.addNetworkModel(synapse)
    return synapse
}

/**
 * Connect input nodes to target nodes with weights initialized to a value.
 */
fun connectAllToAll(source: NeuronGroup, target: NeuronGroup, value: Double): List<Synapse>? {
    val wts = connectAllToAll(source, target)
    wts.forEach(Consumer { wt: Synapse -> wt.forceSetStrength(value) })
    return wts
}

fun connectAllToAll(source: NeuronGroup, target: NeuronGroup): List<Synapse> {
    return AllToAll().connectAllToAll(source.neuronList, target.neuronList)
}

/**
 * Connect a source neuron group to a single target neuron
 */
fun connectAllToAll(inputs: NeuronGroup, target: Neuron): List<Synapse> {
    val connector = AllToAll()
    return connector.connectAllToAll(inputs.neuronList, listOf(target))
}

/**
 * Connect input nodes to target node with weights initialized to a value.
 */
fun connectAllToAll(source: NeuronGroup, target: Neuron, value: Double): List<Synapse> {
    val wts = connectAllToAll(source, target)
    wts.forEach(Consumer { wt: Synapse -> wt.forceSetStrength(value) })
    return wts
}

