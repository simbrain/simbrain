package org.simbrain.util

import org.simbrain.workspace.*
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import java.lang.reflect.Method
import java.lang.reflect.Type

typealias JConsumer<T> = java.util.function.Consumer<T>

/**
 * A cache of all [Attribute] and [Coupling] objects in the [Workspace]. Used to provide
 * fast indexed access to producers, consumers, couplings, etc., and to filter these quickly.
 */
class CouplingCache(workspace: Workspace) {

    val producers = ProducerCache()

    val consumers = ConsumerCache()

    val visibleMethods
        get() = producers.visibleMethods + consumers.visibleMethods

    val couplings
        get() = couplingsByContainer.values.flatten().toSet().sortedBy { it.id }

    private val couplingsByContainer = HashMap<AttributeContainer, HashSet<Coupling>>()

    val Collection<Attribute>.visible
        get() = this.filter { it.method in visibleMethods }

    init {
        workspace.events.apply {

            onComponentAdded(JConsumer {

                it.attributeContainers.forEach{ ac ->
                    producers.addContainer(it, ac)
                    consumers.addContainer(it, ac)
                }

                it.events.apply {

                    onAttributeContainerAdded(JConsumer { ac ->
                        producers.addContainer(it, ac)
                        consumers.addContainer(it, ac)
                    })

                    onAttributeContainerRemoved(JConsumer { ac ->
                        producers.removeContainer(it, ac)
                        consumers.removeContainer(it, ac)
                        couplingsByContainer.remove(ac)
                    })
                }
            })

            onComponentRemoved(JConsumer {
                it.attributeContainers.forEach { ac ->
                    producers.removeContainer(it, ac)
                    consumers.removeContainer(it, ac)
                    couplingsByContainer.remove(ac)
                }
            })

        }

    }

    /**
     * Adds a coupling to the cache.
     */
    fun add(coupling: Coupling) {
        val producerContainer = coupling.producer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }
        val consumerContainer = coupling.consumer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }

        if (producerContainer in couplingsByContainer) {
            couplingsByContainer[producerContainer]!!.add(coupling)
        } else {
            couplingsByContainer[producerContainer] = hashSetOf(coupling)
        }

        if (consumerContainer in couplingsByContainer) {
            couplingsByContainer[consumerContainer]!!.add(coupling)
        } else {
            couplingsByContainer[consumerContainer] = hashSetOf(coupling)
        }
    }

    /**
     * Removes a coupling from the cache
     */
    fun remove(coupling: Coupling) {
        val producerContainer = coupling.producer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }
        val consumerContainer = coupling.consumer.baseObject.run {
            if (this is PeripheralAttribute) this.parent else this
        }

        couplingsByContainer[producerContainer]?.remove(coupling)
        couplingsByContainer[consumerContainer]?.remove(coupling)
    }

    /**
     * Sets an [Attribute.method] to being visible, i.e. visible in the GUI.
     */
    fun setVisible(method: Method, visible: Boolean) {
        val cache = when {
            method.isProducible() -> producers
            method.isConsumable() -> consumers
            else -> return
        }

        if (visible) {
            cache.visibleMethods.add(method)
        } else {
            cache.visibleMethods.remove(method)
        }
    }

    /**
     * Cache of an [Attribute].
     */
    abstract class AttributeCache<T : Attribute> {

        private val all = HashSet<T>()

        private val byType = HashMap<Type, HashSet<T>>()

        private val byContainer = HashMap<AttributeContainer, HashSet<T>>()

        private val byComponent = HashMap<WorkspaceComponent, HashSet<T>>()

        val visibleMethods = HashSet<Method>()

        operator fun get(type: Type) = byType[type] ?: hashSetOf()

        operator fun get(container: AttributeContainer) = byContainer[container] ?: hashSetOf()

        operator fun get(component: WorkspaceComponent) = byComponent[component] ?: hashSetOf()

        abstract fun addContainer(component: WorkspaceComponent, container: AttributeContainer)

        protected fun addAttributes(component: WorkspaceComponent, attributes: Collection<T>) {

            all.addAll(attributes)

            attributes.groupBy { it.type }.forEach {

                if (it.key !in byType) {
                    byType[it.key] = it.value.toHashSet()
                } else {
                    byType[it.key]!!.addAll(it.value)
                }
            }

            attributes.groupBy { it.baseObject as AttributeContainer }.forEach {
                if (it.key !in byContainer) {
                    byContainer[it.key] = it.value.toHashSet()
                } else {
                    byContainer[it.key]!!.addAll(it.value)
                }
            }


            if (component in byComponent) {
                byComponent[component]!!.addAll(attributes)
            } else {
                byComponent[component] = attributes.toHashSet()
            }
        }

        fun removeContainer(component: WorkspaceComponent, attributeContainer: AttributeContainer) {
            val attributes = byContainer[attributeContainer]
            if (attributes != null) {
                all.removeAll(attributes)
                byType.values.forEach { it.removeAll(attributes) }
                byComponent[component]?.removeAll(attributes)
                byContainer.remove(attributeContainer)
            }
        }

    }

    inner class ProducerCache : AttributeCache<Producer>() {
        override fun addContainer(component: WorkspaceComponent, container: AttributeContainer) {
            addAttributes(component, container.producers)
            container.producibles
                    .filter { it !in visibleMethods }
                    .filter { it.getAnnotation(Producible::class.java).defaultVisibility }
                    .let { visibleMethods.addAll(it) }
        }
    }

    inner class ConsumerCache : AttributeCache<Consumer>() {
        override fun addContainer(component: WorkspaceComponent, container: AttributeContainer) {
            addAttributes(component, container.consumers)
            container.consumables
                    .filter { it !in visibleMethods }
                    .filter { it.getAnnotation(Consumable::class.java).defaultVisibility }
                    .let { visibleMethods.addAll(it) }
        }
    }
}