package org.simbrain.workspace.couplings

import org.simbrain.workspace.*
import java.lang.reflect.Method

/**
 * Maintains a list of [Coupling]'s, and of potential [Producer] and
 * [Consumer] objects.  Supports creation of couplings, setting of producer
 * and consumer visibility, and filtering of all three types of object, e.g.
 *
 * This is a transient field of [Workspace] which is thus not persisted.
 *
 * Couplings can be created in a many-to-many fashion. One to many coupligns were not allowed in earlier versions of
 * Simbrain, because they can produce unexpected behaviors, where one producer will overrwrite the value of another.
 * However, many-to-one couplings can be useful, and are handled by special consumers, e.g. Neuron.addInputValue
 * or OdorWorld's effectors like StraightMovement, which has an addAmount method.
 *
 * Coupling creation should rely on the factory methods here
 * rather than by invoking constructors on Coupling directly so that couplings will
 * be properly managed and serialized.
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
                    couplingCache.getMethods(container)
                            .filter { couplingCache.getVisibility(it) }
                            .forEach { yield(it) }
                }
            }
        }.toSet()

    /**
     * A collection of all producible methods in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.producerMethods: Set<Method>
        get() = sequence {
            attributeContainers.forEach { container ->
                couplingCache.getMethods(container)
                        .filter { it.isProducible() }
                        .forEach { yield(it) }
            }
        }.toSet()

    /**
     * A collection of all consumable methods in a given [WorkspaceComponent]
     */
    val WorkspaceComponent.consumerMethods: Set<Method>
        get() = sequence {
            attributeContainers.forEach { container ->
                couplingCache.getMethods(container)
                        .filter { it.isConsumable() }
                        .forEach { yield(it) }
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
    fun Consumer.compatiblesOfComponent(component: WorkspaceComponent)
            = couplingCache.getCompatibleVisibleProducers(this, component)

    /**
     * A collection of all [compatibleConsumers] in a given [WorkspaceComponent]
     */
    fun Producer.compatiblesOfComponent(component: WorkspaceComponent)
            = couplingCache.getCompatibleVisibleConsumers(this, component)

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

    fun removeCouplings(couplings: List<Coupling>) {
        couplings.forEach {
            _couplings.remove(it.producer to it.consumer)
        }
        // What to do here?
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
    fun updateCouplings() {
        for (coupling in couplings) {
            coupling.update()
        }
    }

    /**
     * Remove a specific coupling
     *
     * @param coupling the coupling to remove
     */
    fun removeCoupling(coupling: Coupling) {
        _couplings.remove(coupling)
        attributeContainerCouplings[coupling.producer.baseObject]?.remove(coupling)
        attributeContainerCouplings[coupling.consumer.baseObject]?.remove(coupling)
        events.fireCouplingRemoved(coupling)
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
