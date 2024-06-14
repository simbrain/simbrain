package org.simbrain.workspace.couplings

import org.simbrain.workspace.*
import java.lang.reflect.Method

/**
 * Cache method objects for each [AttributeContainer]. Reflection is still used to create Method objects but they are
 * cached here for quick access.
 *
 * A utility class for [CouplingManager]. Provides optimized ways to access specific sets of producers and consumers.
 * These methods should not be called directly and this class should not be instantiated outside of CouplingManager.
 *
 * @author Yulin Li
 */
class CouplingCache(val couplingManager: CouplingManager) {

    /**
     * The main cache for [Method] objects.
     */
    private val attributeMethods = HashMap<Class<AttributeContainer>, List<Method>>()

    /**
     * Cache properties of [Producible] methods like annotations and custom descriptions.
     */
    private val producerBuilders = HashMap<Method, (AttributeContainer) -> Producer>()

    /**
     * Cache properties of [Consumable] methods like annotations and custom descriptions.
     */
    private val consumerBuilders = HashMap<Method, (AttributeContainer) -> Consumer>()

    /**
     * Get all the [Producible] or [Consumable] methods from an [AttributeContainer] and put them in the main cache.
     */
    fun getMethods(container: AttributeContainer): List<Method> {
        return getMethods(container.javaClass)
    }

    fun getMethods(containerClass: Class<AttributeContainer>): List<Method> {
        return attributeMethods.getOrPut(containerClass) {
            containerClass.methods
                    .filter { it.isProducible() || it.isConsumable() }
                    .sortedBy { it.name }
        }
    }

    fun getProducibleMethods(container: AttributeContainer): List<Method> {
        return getMethods(container).filter { it.isProducible() }
    }

    fun getProducibleMethods(containerClass: Class<AttributeContainer>): List<Method> {
        return getMethods(containerClass).filter { it.isProducible() }
    }

    fun getConsumableMethods(container: AttributeContainer): List<Method> {
        return getMethods(container).filter { it.isConsumable() }
    }

    fun getConsumableMethods(containerClass: Class<AttributeContainer>): List<Method> {
        return getMethods(containerClass).filter { it.isConsumable() }
    }

    fun getVisibility(method: Method) = couplingManager.methodVisibilities.getOrElse(method) {
        method.getAnnotation(Producible::class.java)?.defaultVisibility
                ?: method.getAnnotation(Consumable::class.java).defaultVisibility
    }

    fun AttributeContainer.getProducer(methodName: String): Producer {
        return javaClass.findProducibleMethod(methodName)?.let { method ->
            getProducer(method)
        } ?: throw NoSuchMethodException(
                "No producible method with name $methodName was found in class ${this@CouplingCache.javaClass.simpleName}."
        )
    }

    fun AttributeContainer.getProducer(method: Method): Producer = producerBuilders.getOrPut(method) {

        // The objects below are what are cached by the builder
        val annotation = method.getAnnotation(Producible::class.java)
                ?: throw IllegalArgumentException("Method ${method.name} is not producible.")

        val customDescription = javaClass.findMethod(annotation.customDescriptionMethod)
        val arrayDescriptionMethod = javaClass.findMethod(annotation.arrayDescriptionMethod)

        val customPriorityMethod = javaClass.findMethod(annotation.customPriorityMethod)
        val priority = customPriorityMethod?.invoke(this) as? Int ?: annotation.priority


        fun (attributeContainer: AttributeContainer) = Producer.builder(attributeContainer, method)
                .description(annotation.description)
                .priority(priority)
                .customDescription(customDescription)
                .arrayDescriptionMethod(arrayDescriptionMethod)
                .build()
    }(this)

    fun AttributeContainer.getConsumer(methodName: String): Consumer {
        return javaClass.findConsumableMethod(methodName)?.let { method ->
            this.getConsumer(method)
        } ?: throw NoSuchMethodException(
                "No consumable method with name $methodName was found in class ${this@CouplingCache.javaClass.simpleName}."
        )
    }

    fun AttributeContainer.getConsumer(method: Method): Consumer = consumerBuilders.getOrPut(method) {

        // The objects below are what are cached by the builder
        val annotation = method.getAnnotation(Consumable::class.java)
                ?: throw IllegalArgumentException("Method ${method.name} is not consumable.")

        val customDescription = javaClass.findMethod(annotation.customDescriptionMethod)

        val customPriorityMethod = javaClass.findMethod(annotation.customPriorityMethod)
        val priority = customPriorityMethod?.invoke(this) as? Int ?: annotation.priority

        fun (attributeContainer: AttributeContainer) = Consumer.builder(attributeContainer, method)
                .priority(priority)
                .description(annotation.description)
                .customDescription(customDescription)
                .build()
    }(this)

    fun getProducers(attributeContainer: AttributeContainer): Sequence<Producer> = sequence {
        getProducibleMethods(attributeContainer).forEach {
            yield(attributeContainer.getProducer(it))
        }
    }

    fun getConsumers(attributeContainer: AttributeContainer): Sequence<Consumer> = sequence {
        getConsumableMethods(attributeContainer).forEach {
            yield(attributeContainer.getConsumer(it))
        }
    }

    fun getProducers(workspaceComponent: WorkspaceComponent): Sequence<Producer> = sequence {
        workspaceComponent.attributeContainers.forEach { container ->
            getProducers(container).forEach {
                yield(it)
            }
        }
    }

    fun getConsumers(workspaceComponent: WorkspaceComponent): Sequence<Consumer> = sequence {
        workspaceComponent.attributeContainers.forEach { container ->
            getConsumers(container).forEach {
                yield(it)
            }
        }
    }

    fun getVisibleProducers(attributeContainer: AttributeContainer): Sequence<Producer> = sequence {
        getProducibleMethods(attributeContainer)
                .filter { getVisibility(it) }
                .forEach { yield(attributeContainer.getProducer(it)) }
    }

    fun getVisibleConsumers(attributeContainer: AttributeContainer): Sequence<Consumer> = sequence {
        getConsumableMethods(attributeContainer)
                .filter { getVisibility(it) }
                .forEach { yield(attributeContainer.getConsumer(it)) }
    }

    fun getVisibleProducers(workspaceComponent: WorkspaceComponent): Sequence<Producer> = sequence {
        workspaceComponent.attributeContainers.groupBy { it.javaClass }.entries.forEach { (clazz, containers) ->
            val methods = getProducibleMethods(clazz).filter { getVisibility(it) }
            containers.forEach { container ->
                methods.forEach { method ->
                    if (method.isProducible()) {
                        yield(container.getProducer(method))
                    }
                }
            }
        }
    }

    fun getVisibleConsumers(workspaceComponent: WorkspaceComponent): Sequence<Consumer> = sequence {
        workspaceComponent.attributeContainers.groupBy { it.javaClass }.entries.forEach { (clazz, containers) ->
            val methods = getConsumableMethods(clazz).filter { getVisibility(it) }
            containers.forEach { container ->
                methods.forEach { method ->
                    if (method.isConsumable()) {
                        yield(container.getConsumer(method))
                    }
                }
            }
        }
    }

    fun getCompatibleVisibleProducers(
            consumer: Consumer,
            workspaceComponent: WorkspaceComponent
    ): Sequence<Producer> = sequence {
        workspaceComponent.attributeContainers.groupBy { it.javaClass }.entries.forEach { (clazz, containers) ->
            val methods = getProducibleMethods(clazz)
                    .filter { getVisibility(it) }
                    .filter { it.returnType == consumer.type }
            containers.forEach { container ->
                methods.forEach { method ->
                    yield(container.getProducer(method))
                }
            }
        }
    }

    fun getCompatibleVisibleConsumers(
            producer: Producer,
            workspaceComponent: WorkspaceComponent
    ): Sequence<Consumer> = sequence {
        workspaceComponent.attributeContainers.groupBy { it.javaClass }.entries.forEach { (clazz, containers) ->
            val methods = getConsumableMethods(clazz)
                    .filter { getVisibility(it) }
                    .filter { it.parameterTypes[0] == producer.type }
            containers.forEach { container ->
                methods.forEach { method ->
                    yield(container.getConsumer(method))
                }
            }
        }
    }

    private fun Class<AttributeContainer>.findMethod(name: String): Method? = methods.find { it.name == name }

    private fun Class<AttributeContainer>.findConsumableMethod(name: String): Method? = methods
        .first{ it.isConsumable() && it.name == name }

    private fun Class<AttributeContainer>.findProducibleMethod(name: String): Method? = methods
        .first{ it.isProducible() && it.name == name }

}