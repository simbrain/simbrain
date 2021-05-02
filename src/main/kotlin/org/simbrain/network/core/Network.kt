package org.simbrain.network.core

import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.events.NetworkEvents
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.util.*
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.math.SimbrainMath
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
 * If a subnetwork or synapse group has more than this many synapses, then the initial synapse visibility flag is
 * set false.
 */
@Transient
var synapseVisibilityThreshold = SimbrainPreferences.getInt("networkSynapseVisibilityThreshold")

class Network {

    companion object {
        /**
         * An internal id giving networks unique numbers within the same simbrain session.
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

    init {
        current_id++
    }

    fun <T : NetworkModel> getModels(cls: Class<T>) = networkModels[cls]
    inline fun <reified T : NetworkModel> getModels() = getModels(T::class.java)

    val allModels get() = networkModels.all
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
            neuron.update()
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
     * Return the neuron at the specified index of the internal list storing neurons.
     *
     * @param neuronIndex index of the neuron
     * @return the neuron at that index
     */
    @Deprecated("This is linear search")
    fun getLooseNeuron(neuronIndex: Int): Neuron {
        val iterator: Iterator<Neuron> = networkModels.get<Neuron>().iterator()
        for (i in 0 until neuronIndex) {
            iterator.next()
        }
        return iterator.next()
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
                for (neuronGroup in subnetwork.neuronGroupList) {
                    yieldAll(neuronGroup.neuronList)
                }
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
                subnetwork.synapseGroupList.flatMap { it.allSynapses }
            })
        }.toList()

    /**
     * Returns a list of all neuron groups including those in subnetworks.
     */
    val flatNeuronGroupList: List<NeuronGroup>
        get() = sequence {
            yieldAll(networkModels.get<NeuronGroup>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.neuronGroupList })
        }.toList()

    /**
     * Returns a list of all synapse groups including those in subnetworks.
     */
    val flatSynapseGroupList: List<SynapseGroup>
        get() = sequence {
            yieldAll(networkModels.get<SynapseGroup>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.synapseGroupList })
        }.toList()

    /**
     * Returns a list of all weight matrices including those in subnetworks.
     */
    val flatWeightMatrixList: List<WeightMatrix>
        get() = sequence {
            yieldAll(networkModels.get<WeightMatrix>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.weightMatrixList })
        }.toList()

    /**
     * Add a new [NetworkModel]. All network models MUST be added using this method.
     */
    fun addNetworkModel(networkModel: NetworkModel) {
        if (networkModel.shouldAdd()) {
            networkModel.id = idManager.getAndIncrementId(networkModel.javaClass)
            networkModels.add(networkModel)
            events.fireModelAdded(networkModel)
        }
    }


    /**
     * Delete a [NetworkModel].
     */
    fun delete(toDelete: NetworkModel) {
        networkModels.remove(toDelete)
        toDelete.delete()
        events.fireModelRemoved(toDelete)
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
        preSaveInit()
        val xmlRepresentation = Utils.getSimbrainXStream().toXML(this)
        postSaveReInit()
        return Utils.getSimbrainXStream().fromXML(xmlRepresentation) as Network
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

        // Initialize update manager
        updateManager.postUnmarshallingInit()
        networkModels.allInDeserializationOrder.forEach { it.postUnmarshallingInit() }
        return this
    }

    /**
     * Perform operations required before saving a network. Post-opening operations occur in [.readResolve].
     */
    fun preSaveInit() {
        for (group in networkModels.get<SynapseGroup>()) {
            group.preSaveInit()
        }
    }

    /**
     * Returns synapse groups to a usable state after a save is performed.
     */
    fun postSaveReInit() {
        for (group in networkModels.get<SynapseGroup>()) {
            group.postSaveReInit()
        }
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

    override fun toString(): String = """
        Root Network
        =================
        ${networkModels.all.joinToString("\n        ") { "[${it.id}] $it" }}
    """.trimIndent()


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
            group.setFrozen(freeze, Polarity.BOTH)
        }
        // Freeze loose synapses
        for (synapse in networkModels.get<Synapse>()) {
            synapse.isFrozen = freeze
        }
    }

    var isRunning: Boolean
        get() = _isRunning.get()
        set(value) {
            _isRunning.set(value)
        }

    val isRedrawTime: Boolean = oneOffRun || (iterCount % updateFreq == 0)

    val looseNeurons get() = networkModels.get<Neuron>()

    fun addNeuron(block: Neuron.() -> Unit = { }) = Neuron(this)
        .apply(block)
        .also(this::addNetworkModel)

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

    fun connectAllToAll(source: NeuronGroup, target: NeuronGroup): List<Synapse> {
        return AllToAll().connectAllToAll(source.neuronList, target.neuronList)
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
     * The main data structure for [NetworkModel]s. Wraps a map from classes to ordered sets of those objects.
     */
    private class NetworkModelList {

        /**
         * Backing for the collection: a map from model types to linked hash sets.
         */
        @XStreamImplicit
        private val networkModels: MutableMap<Class<out NetworkModel>, LinkedHashSet<NetworkModel>?> = HashMap()

        @Suppress("UNCHECKED_CAST")
        fun <T : NetworkModel> put(modelClass: Class<T>, model: T) {
            if (modelClass in networkModels) {
                networkModels[modelClass]!!.add(model)
            } else {
                val newSet = LinkedHashSet<T>()
                newSet.add(model)
                networkModels[modelClass] = newSet as LinkedHashSet<NetworkModel>
            }
        }

        /**
         * Put in the list without checking type. Needed for de-serialization. Avoid, and if used
         * use with caution.
         */
        fun putUnsafe(modelClass: Class<out NetworkModel>, model: NetworkModel) {
            if (modelClass in networkModels) {
                networkModels[modelClass]!!.add(model)
            } else {
                val newSet = LinkedHashSet<NetworkModel>()
                newSet.add(model)
                networkModels[modelClass] = newSet
            }
        }

        /**
         * Add a collection of network models to the map.
         */
        fun addAll(models: Collection<NetworkModel>) {
            models.forEach { add(it) }
        }

        /**
         * Add a network model to the map.
         */
        fun add(model: NetworkModel) {
            if (model is Subnetwork) {
                put(Subnetwork::class.java, model)
            } else {
                put(model.javaClass, model)
            }
        }

        /**
         * Returns an ordered set of network models of a specific type.
         */
        @Suppress("UNCHECKED_CAST")
        operator fun <T : NetworkModel> get(modelClass: Class<T>): LinkedHashSet<T> {
            return if (networkModels.containsKey(modelClass)) {
                networkModels[modelClass] as LinkedHashSet<T>
            } else {
                LinkedHashSet()
            }
        }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : NetworkModel> get() = get(T::class.java)

        //TODO
        fun unsafeGet(modelClass: Class<*>?): LinkedHashSet<*> {
            return if (networkModels.containsKey(modelClass)) {
                networkModels[modelClass]!!
            } else {
                LinkedHashSet<NetworkModel>()
            }
        }

        val all: List<NetworkModel>
            get() = networkModels.values.flatMap { it?.map { item -> item } ?: listOf() }

        /**
         * Returns a list of network models in the order required for proper deserilization.
         */
        val allInDeserializationOrder: List<NetworkModel>
            get() {
                val keys = networkModels.keys.toMutableSet()
                return sequence {
                    for (cls in deserializationOrder) {
                        networkModels[cls]?.let { yieldAll(it) }
                        keys.remove(cls)
                    }
                    for (cls in keys) {
                        networkModels[cls]?.let { yieldAll(it) }
                    }
                }.toList()
            }

        fun remove(model: NetworkModel) {
            if (model is Subnetwork) {
                networkModels[Subnetwork::class.java]?.remove(model)
            } else {
                networkModels[model.javaClass]?.remove(model)
            }
        }
    }

    /**
     * Custom serializer that stores [Network.networkModels], which is a map, as a flat list of [NetworkModel]s.
     */
    class NetworkModelListConverter : Converter {

        override fun canConvert(type: Class<*>?) = NetworkModelList::class.java == type

        override fun marshal(source: Any?, writer: HierarchicalStreamWriter, context: MarshallingContext) {
            val modelList = source as NetworkModelList
            modelList.allInDeserializationOrder.forEach { model ->
                writer.startNode(model::class.java.name)
                context.convertAnother(model)
                writer.endNode()
            }
        }

        override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
            val modelList = NetworkModelList()
            while (reader.hasMoreChildren()) {
                reader.moveDown()
                val cls = Class.forName(reader.nodeName)
                val model = context.convertAnother(reader.value, cls) as NetworkModel
                modelList.putUnsafe(cls as Class<out NetworkModel>, model)
                reader.moveUp()
            }
            return modelList
        }

    }

}

/**
 * Items must be ordered for deserializing. For example neurons but serialized before synapses.
 */
private val deserializationOrder: List<Class<out NetworkModel>> = listOf(
    Neuron::class.java,
    NeuronGroup::class.java,
    NeuronCollection::class.java,
    NeuronArray::class.java,
    WeightMatrix::class.java,
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
    for (neuron in neuronList) {
        neuron.updateInputs()
    }
    for (neuron in neuronList) {
        neuron.update()
    }
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

fun networkUpdateAction(description: String, longDescription: String = description, action: () -> Unit) =
    object : NetworkUpdateAction {
        override fun invoke() = action()
        override fun getDescription(): String = description
        override fun getLongDescription(): String = longDescription
    }