package org.simbrain.network.core

import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.util.SnapshotSet
import org.simbrain.util.CachedObject
import java.util.concurrent.ConcurrentHashMap

/**
 * The main data structure for [NetworkModel]s. Wraps a map from classes to ordered sets of those objects.
 * Backed by insertion-ordered [SnapshotSet]s. The set deals with duplication; insertion order provides the iteration order.
 *
 * Used both by [Network] and by [Subnetwork].
 */
class NetworkModelList {

    /**
     * Backing for the collection: a map from model types to insertion-ordered model sets.
     */
    @XStreamImplicit
    private val networkModels: MutableMap<Class<out NetworkModel>, SnapshotSet<NetworkModel>?> = ConcurrentHashMap()

    @Suppress("UNCHECKED_CAST")
    fun <T : NetworkModel> put(modelClass: Class<T>, model: T) {
        allInUpdatingOrderCache.invalidate()
        allInPriorityOrderCache.invalidate()
        updatePlanCache.invalidate()
        if (modelClass in networkModels) {
            networkModels[modelClass]!!.add(model)
        } else {
            val newSet = SnapshotSet<T>()
            newSet.add(model)
            networkModels[modelClass] = newSet as SnapshotSet<NetworkModel>
        }
    }

    /**
     * Put in the list without checking type. Needed for de-serialization. Avoid, and if used
     * use with caution.
     */
    fun putUnsafe(modelClass: Class<out NetworkModel>, model: NetworkModel) {
        allInUpdatingOrderCache.invalidate()
        allInPriorityOrderCache.invalidate()
        updatePlanCache.invalidate()
        if (modelClass in networkModels) {
            networkModels[modelClass]!!.add(model)
        } else {
            val newSet = SnapshotSet<NetworkModel>()
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
        allInUpdatingOrderCache.invalidate()
        allInPriorityOrderCache.invalidate()
        updatePlanCache.invalidate()
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
    operator fun <T : NetworkModel> get(modelClass: Class<T>): Set<T> {
        return if (networkModels.containsKey(modelClass)) {
            networkModels[modelClass] as Set<T>
        } else {
            emptySet()
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : NetworkModel> get() = get(T::class.java)

    /**
     * Returns a set corresponding to the provided network model type.
     * Does not guarantee that the returned set contains models of that type.
     */
    fun getRawModelSet(modelClass: Class<*>?): Set<*> {
        return if (networkModels.containsKey(modelClass)) {
            networkModels[modelClass]!!
        } else {
            emptySet<NetworkModel>()
        }
    }

    val all: List<NetworkModel>
        get() = networkModels.values.flatMap { it?.map { item -> item } ?: listOf() }

    val deepAll: List<NetworkModel>
        get() = networkModels.values.flatMap { set ->
                set?.flatMap { item ->
                    when (item) {
                        is NeuronCollection -> listOf(item) + item.neuronList
                        else -> listOf(item)
                    }
                } ?: listOf()
        }

    private val allInUpdatingOrderCache = CachedObject { all.sortedBy { updatingOrder(it) } }

    /**
     * Returns a list of network models in the order required for proper updating and reconstruction of all network models.
     * For example, neurons must be recreated before synapses since the synapses refer to neurons.
     */
    val allInUpdatingOrder by allInUpdatingOrderCache::value

    private val allInPriorityOrderCache = CachedObject { allInUpdatingOrder.sortedBy { it.priority } }

    val allInPriorityOrder by allInPriorityOrderCache::value

    private val updatePlanCache = CachedObject { NetworkUpdatePlan(allInUpdatingOrder) }

    /**
     * The cached iteration order used by [Network.bufferedUpdate].
     */
    val updatePlan by updatePlanCache::value

    fun remove(model: NetworkModel) {
        allInUpdatingOrderCache.invalidate()
        allInPriorityOrderCache.invalidate()
        updatePlanCache.invalidate()
        if (model is Subnetwork) {
            // Forces all subclasses of subnetwork to be grouped with the subnetwork class
            networkModels[Subnetwork::class.java]?.remove(model)
        } else {
            networkModels[model.javaClass]?.remove(model)
        }
    }

    fun updatePriorityList() {
        allInPriorityOrderCache.invalidate()
    }


    override fun toString(): String =  all.joinToString("\n") { "$it" }

    val size get() = networkModels.values.sumBy { it?.size ?: 0 }
}

/**
 * Custom serializer that stores [Network.networkModels], which is a map, as a flat list of [NetworkModel]s.
 */
class NetworkModelListConverter : Converter {

    override fun canConvert(type: Class<*>?) = NetworkModelList::class.java == type

    override fun marshal(source: Any?, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        val modelList = source as NetworkModelList
        modelList.allInUpdatingOrder.forEach { model ->
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
