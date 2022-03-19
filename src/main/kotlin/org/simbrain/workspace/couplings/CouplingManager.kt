package org.simbrain.workspace.couplings

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.simbrain.network.core.Neuron
import org.simbrain.util.cartesianProduct
import org.simbrain.workspace.*
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import java.lang.reflect.Method

/**
 * Maintains a list of [Coupling]s, and of potential [Producer] and [Consumer] objects. Supports creation of
 * couplings, setting of producer and consumer visibility, and filtering of all three types of objecs.
 *
 * Couplings can be created in a many-to-many fashion. Many-to-one couplings were not allowed in earlier versions of
 * Simbrain, because they can produce unexpected behaviors, where one producer will overrwrite the value of another.
 * However, many-to-one couplings can be useful, for example to aggregate many sources of inputs. Examples include Neuron.addInputValue
 * and OdorWorld' StraightMovement effector which has an addAmount method.
 *
 * Coupling creation should rely on the factory methods here rather than by invoking constructors on Coupling
 * directly, so that couplings will be properly managed and serialized.Serialization is handled by [ArchivedCoupling].
 *
 * This is a transient field of [Workspace] which is thus not persisted.
 */
class CouplingManager(val workspace: Workspace) {

    private val couplingCache = CouplingCache(this)

    /**
     * Backing field for [couplings].
     */
    private val _couplings = LinkedHashSet<Coupling>()

    /**
     * Returns all couplings
     */
    val couplings: Set<Coupling> = _couplings

    /**
     * Couplings associated with an [AttributeContainer]. For faster lookup.
     */
    private val attributeContainerCouplings = HashMap<AttributeContainer, LinkedHashSet<Coupling>>()

    val methodVisibilities = HashMap<Method, Boolean>()

    /**
     * List of listeners to fire updates when couplings are changed.
     */
    val events = CouplingEvents(this)

    /**
     * A collection of all producible and consumable methods that are visible
     */
    val visibleMethods: Set<Method>
        get() = sequence {
            workspace.componentList.forEach { component ->
                component.attributeContainers.forEach { container ->
                    couplingCache.getMethods(container).filter { couplingCache.getVisibility(it) }.forEach { yield(it) }
                }
            }
        }.toSet()

    /**
     * A collection of all producible methods in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.producerMethods: Set<Method>
        get() = sequence {
            attributeContainers.forEach { container ->
                couplingCache.getMethods(container).filter { it.isProducible() }.forEach { yield(it) }
            }
        }.toSet()

    /**
     * A collection of all consumable methods in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.consumerMethods: Set<Method>
        get() = sequence {
            attributeContainers.forEach { container ->
                couplingCache.getMethods(container).filter { it.isConsumable() }.forEach { yield(it) }
            }
        }.toSet()

    /**
     * A collection of all visible [Producer]s in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.producers: Sequence<Producer>
        get() = couplingCache.getProducers(this)

    /**
     * A collection of all visible [Consumer]s in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.consumers: Sequence<Consumer>
        get() = couplingCache.getConsumers(this)

    /**
     * A collection of all visible [Producer]s in a given [AttributeContainer]
     */
    val AttributeContainer.producers: Sequence<Producer>
        get() = couplingCache.getProducers(this)

    /**
     * A collection of all visible [Consumer]s in a given [AttributeContainer]
     */
    val AttributeContainer.consumers: Sequence<Consumer>
        get() = couplingCache.getConsumers(this)

    /**
     * A collection of all visible [Producer]s in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.visibleProducers: Sequence<Producer>
        get() = couplingCache.getVisibleProducers(this)

    /**
     * A collection of all visible [Consumer]s in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.visibleConsumers: Sequence<Consumer>
        get() = couplingCache.getVisibleConsumers(this)

    /**
     * A collection of all visible [Producer]s in a given [AttributeContainer]
     */
    val AttributeContainer.visibleProducers: Sequence<Producer>
        get() = couplingCache.getVisibleProducers(this)

    /**
     * A collection of all visible [Consumer]s in a given [AttributeContainer]
     */
    val AttributeContainer.visibleConsumers: Sequence<Consumer>
        get() = couplingCache.getVisibleConsumers(this)

    /**
     * Induces a priority on consumers which allows for auto-coupling, i.e. making couplings between
     * [AttributeContainer]s without specifying specific consumers or producers.
     */
    val Consumer.preference: Int
        get() = when {

            baseObject is StraightMovement && method.name == "setAmount" -> 10
            baseObject is Turning && method.name == "setAmount" -> 10
            with(baseObject) { this is Neuron && isClamped && method.name == "forceSetActivation" } -> 10
            with(baseObject) { this is Neuron && !isClamped && method.name == "addInputValue" } -> 10
            else -> 0
        }

    /**
     * See [Consumer.preference]
     */
    val Producer.preference: Int
        get() = when {
            else -> 0
        }


    /**
     * Find the first [Consumer] in an [AttributeContainer] which has the given method name
     */
    fun AttributeContainer.getConsumer(methodName: String): Consumer = with(couplingCache) {
        getConsumer(methodName)
    }

    /**
     * Find the first [Producer] in an [AttributeContainer] which has the given method name
     */
    fun AttributeContainer.getProducer(methodName: String) = with(couplingCache) {
        getProducer(methodName)
    }

    /**
     * A collection of all [compatibleProducers] in a given [WorkspaceComponent]
     */
    fun Consumer.compatiblesOfComponent(component: WorkspaceComponent) =
        couplingCache.getCompatibleVisibleProducers(this, component)

    /**
     * A collection of all [compatibleConsumers] in a given [WorkspaceComponent]
     */
    fun Producer.compatiblesOfComponent(component: WorkspaceComponent) =
        couplingCache.getCompatibleVisibleConsumers(this, component)

    /**
     * Create a coupling from a producer and consumer of the same type.
     *
     * @param producer producer part of the coupling
     * @param consumer consumer part of the coupling
     * @return the newly creating coupling
     */
    fun createCoupling(producer: Producer?, consumer: Consumer?) = Coupling.create(producer, consumer).also {
        _couplings.add(it)
        attributeContainerCouplings.getOrPut(it.producer.baseObject) { LinkedHashSet() }.add(it)
        attributeContainerCouplings.getOrPut(it.consumer.baseObject) { LinkedHashSet() }.add(it)
        events.fireCouplingAdded(it)
    }

    /**
     * Convenience operator for creating couplings.
     */
    infix fun Producer?.couple(consumer: Consumer?) = createCoupling(this, consumer)

    /**
     * Couple the first type-matched producer-consumer pair, where these are ordered by preference.
     */
    fun createCoupling(producingContainer: AttributeContainer, consumingContainer: AttributeContainer): Coupling {
        val (producer, consumer) = (producingContainer.producers cartesianProduct consumingContainer.consumers).filter { (a, b) -> a.type == b.type }
            .sortedByDescending { (a, b) -> a.preference + b.preference }.firstOrNull() ?: throw RuntimeException(
            "No compatible attributes found between $producingContainer and $consumingContainer"
        )
        return producer couple consumer
    }

    infix fun AttributeContainer.couple(consumingContainer: AttributeContainer) =
        createCoupling(this, consumingContainer)

    /**
     * Create a coupling from each producer to every consumer of the same type.
     * Will throw an exception if any of the types do not match.
     *
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     * @throws MismatchedAttributesException An exception indicating that a pair of attributes did not match
     * types. This will be thrown for the first such pair encountered.
     */
    @Throws(MismatchedAttributesException::class)
    fun createOneToManyCouplings(producers: Collection<Producer>, consumers: Collection<Consumer>) {
        for (producer in producers) {
            for (consumer in consumers) {
                createCoupling(producer, consumer)
            }
        }
    }

    /**
     * Create a coupling from each attribute in the smaller collection to a corresponding attribute.
     * Will throw an exception if any of the types do not match.
     *
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     */
    @Throws(MismatchedAttributesException::class)
    fun createOneToOneCouplings(producers: Collection<Producer>, consumers: Collection<Consumer>) {
        val consumerIterator = consumers.iterator()
        for (producer in producers) {
            if (consumerIterator.hasNext()) {
                val consumer = consumerIterator.next()
                createCoupling(producer, consumer)
            } else {
                break
            }
        }
    }

    fun createOneToOneCouplings(
        producers: Collection<AttributeContainer>, consumers: Collection<AttributeContainer>
    ): List<Coupling> = (producers zip consumers).map { (producer, consumer) -> producer couple consumer }

    infix fun Collection<AttributeContainer>.couple(consumers: Collection<AttributeContainer>): List<Coupling> =
        createOneToOneCouplings(this, consumers)

    fun removeCouplings(couplings: List<Coupling>) {
        couplings.forEach { coupling ->
            removeCouplingWithoutFiringEvent(coupling)
        }
        events.fireCouplingsRemoved(couplings)
    }

    /**
     * Return a coupling in the workspace by the coupling id.
     */
    fun getCoupling(id: String) = couplings.find { it.id.equals(id, ignoreCase = true) }

    fun Method.setVisibility(visible: Boolean) {
        methodVisibilities[this] = visible
    }

    /**
     * Convenience method for updating a set of couplings.
     *
     * @param couplingList the list of couplings to be updated
     */
    fun updateCouplings(couplingList: List<Coupling>) {
        couplingList.forEach { it.update() }
    }

    /**
     * Update all couplings by setting the consumers to take the values of their producers.
     */
    suspend fun updateCouplings() {
        coroutineScope {
            couplings.map { async { it.update() } }.awaitAll()
        }
    }

    /**
     * Remove a specific coupling
     *
     * @param coupling the coupling to remove
     */
    fun removeCoupling(coupling: Coupling) {
        removeCouplingWithoutFiringEvent(coupling)
        events.fireCouplingRemoved(coupling)
    }

    private fun removeCouplingWithoutFiringEvent(coupling: Coupling) {
        _couplings.remove(coupling)
        attributeContainerCouplings[coupling.producer.baseObject]?.let {
            it.remove(coupling)
            if (it.isEmpty()) {
                attributeContainerCouplings.remove(coupling.producer.baseObject)
            }
        }
        attributeContainerCouplings[coupling.consumer.baseObject]?.let {
            it.remove(coupling)
            if (it.isEmpty()) {
                attributeContainerCouplings.remove(coupling.consumer.baseObject)
            }
        }
    }

    fun removeAttributeContainer(attributeContainer: AttributeContainer) {
        attributeContainerCouplings[attributeContainer]?.let {
            it.forEach { coupling ->
                _couplings.remove(coupling)
                if (coupling.consumer.baseObject !== attributeContainer) {
                    attributeContainerCouplings[coupling.consumer.baseObject]?.remove(coupling)
                }
                if (coupling.producer.baseObject !== attributeContainer) {
                    attributeContainerCouplings[coupling.producer.baseObject]?.remove(coupling)
                }
            }
            events.fireCouplingsRemoved(it.toList())
        }
        attributeContainerCouplings.remove(attributeContainer)
    }

}
