package org.simbrain.workspace

import org.simbrain.util.CouplingCache
import java.lang.reflect.Method

/**
 * Maintains a list of [Coupling]'s, and of potential [Producer] and
 * [Consumer] objects.  Supports creation of couplings, setting of producer
 * and consumer visibility, and filtering of all three types of object, e.g.
 *
 * This is a transient field of [Workspace] which is thus not persisted.
 *
 * Coupling creation should rely on the factory methods here
 * rather than by invoking constructors on Coupling directly so that couplings will
 * be properly managed and serialized.
 */
class CouplingManager(workspace: Workspace) {

    /**
     * All couplings for the workspace.
     */
    val couplings
        get() = couplingCache.couplings

    private val couplingCache = CouplingCache(workspace)

    /**
     * List of listeners to fire updates when couplings are changed.
     */
    val events = CouplingEvents(this)

    val visibleMethods
        get() = couplingCache.visibleMethods

    val WorkspaceComponent.producerMethods
        get() = couplingCache.producers[this].map { it.method }.toSet().sortedBy { it.name }

    val WorkspaceComponent.consumerMethods
        get() = couplingCache.consumers[this].map { it.method }.toSet().sortedBy { it.name }

    val WorkspaceComponent.visibleProducers
        get() = couplingCache.producers[this].filter { it.method.isVisible }.sortedBy { it.id }

    val WorkspaceComponent.visibleConsumers
        get() = couplingCache.consumers[this].filter { it.method.isVisible }.sortedBy { it.id }

    val AttributeContainer.visibleProducers
        get() = couplingCache.producers[this].filter { it.method.isVisible }.sortedBy { it.id }

    val AttributeContainer.visibleConsumers
        get() = couplingCache.consumers[this].filter { it.method.isVisible }.sortedBy { it.id }

    val Consumer.compatibleProducers
        get() = couplingCache.producers[this.type].filter { it.method.isVisible }.sortedBy { it.id }

    val Producer.compatibleConsumers
        get() = couplingCache.consumers[this.type].filter { it.method.isVisible }.sortedBy { it.id }

    fun Consumer.compatiblesOfComponent(component: WorkspaceComponent)
            = compatibleProducers.filter { it in couplingCache.producers[component] }

    fun Producer.compatiblesOfComponent(component: WorkspaceComponent)
            = compatibleConsumers.filter { it in couplingCache.consumers[component] }

    fun AttributeContainer.consumerByName(name: String)
            = couplingCache.consumers[this].find { it.method.name == name }

    fun AttributeContainer.producerByName(name: String)
            = couplingCache.producers[this].find { it.method.name == name }

    /**
     * Create a coupling from a producer and consumer of the same type.
     *
     * @param producer producer part of the coupling
     * @param consumer consumer part of the coupling
     * @return the newly creating coupling
     */
    fun createCoupling(producer: Producer?, consumer: Consumer?) = +Coupling.create(producer, consumer)
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
        couplings.forEach(couplingCache::remove)
        // What to do here?
        events.fireCouplingsRemoved(couplings)
    }

    /**
     * Return a coupling in the workspace by the coupling id.
     */
    fun getCoupling(id: String) = couplings.find { it.id.equals(id, ignoreCase = true) }

    fun AttributeContainer.getProducer(methodName: String) = couplingCache.producers[this].find { it.method.name == methodName }

    fun AttributeContainer.getConsumer(methodName: String) = couplingCache.consumers[this].find { it.method.name == methodName }

    fun Method.setVisibility(visible: Boolean) = couplingCache.setVisible(this, visible)

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
        couplingCache.remove(coupling)
        events.fireCouplingRemoved(coupling)
    }

    /**
     * Add a coupling to the cache.
     */
    private operator fun Coupling.unaryPlus(): Coupling {
        couplingCache.add(this)
        events.fireCouplingAdded(this)
        return this
    }

    private val Method.isVisible
        get() = this in couplingCache.visibleMethods

}
