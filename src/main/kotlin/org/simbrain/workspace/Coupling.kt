package org.simbrain.workspace

import java.lang.reflect.Method
import java.lang.reflect.Type


/**
 * A pair containing a producer and a consumer.  When updated, the producer produces a value
 * and the consumer consumes it.  See http://www.simbrain.net/Documentation/docs/Pages/Workspace/Couplings.html
 * and https://www.youtube.com/watch?v=zDUY9mUKZ-I
 *
 * @param <T> the type of the coupling. E.g a double coupling is a link that gets a double value from a producer
 *           and then sets a double value on a consumer.
 *
 * @author Jeff Yoshimi
 * @author Tim Shea
 * @author Yulin Li
 * @author Matt Watson
 */
class Coupling private constructor(val producer: Producer, val consumer: Consumer) {

    /**
     * This is the main action!  Set the value of the consumer based on the
     * value of the producer.
     *
     * Note that values are passed by reference, so that it is up to the producing or
     * consuming methods to make defensive copies as needed.
     * (cf http://www.javapractices.com/topic/TopicAction.do?Id=15)).
     */
    fun update() {
        consumer.setValue(producer.value)
    }

    val type: Type
        get() = producer.type

    val id: String
        get() = "${producer.id} > ${consumer.id}"

    val description: String
        get() = "$producer > $consumer"

    override fun toString() = description

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coupling

        if (producer != other.producer) return false
        if (consumer != other.consumer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = producer.hashCode()
        result = 31 * result + consumer.hashCode()
        return result
    }

    companion object {

        /**
         * Main creation  method for couplings.
         *
         * @param producer the producer
         * @param consumer the consumer
         * @param <T> the type of the coupling (usually double or double[])
         * @return the coupling
         */
        @Throws(MismatchedAttributesException::class)
        fun create(producer: Producer?, consumer: Consumer?) = if (producer == null || consumer == null) {
            throw IllegalArgumentException("Producer and Consumer cannot be null")
        } else if (consumer.type == producer.type) {
            Coupling(producer, consumer)
        } else {
            throw MismatchedAttributesException(
                    "Producer type ${producer.type} does not match consumer ${consumer.type}"
            )
        }
    }
}

/**
 * All methods that are annotated with [Consumable] in a attribute container.
 */
val AttributeContainer.consumables
    get() = javaClass.methods.filter { it.isConsumable() }

/**
 * Get all methods of the [AttributeContainer] annotated as [Producible]
 */
val AttributeContainer.producibles
    get() = javaClass.methods.filter { it.isProducible() }

/**
 * TODO: Yulin will evaluate and rewrite appropriate javadoc.
 * Get all consumers from the container.
 */
val AttributeContainer.consumers
    get() = this.consumables.map { this.getConsumer(it) }

/**
 * TODO: Yulin will evaluate and rewrite appropriate javadoc.
 */
val AttributeContainer.producers
    get() = this.producibles.map { this.getProducer(it) }

/**
 * Get a specific consumer from a specified [AttributeContainer].
 *
 * @param container The object in which to find the consumable.
 * @param methodName The name of the consumable method.
 * @return The consumer
 */
fun AttributeContainer.getConsumer(methodName: String): Consumer {

    val method = this.javaClass.methods.find { it.name == methodName }

    if (method != null) {
        return getConsumer(method)
    } else {
        throw NoSuchMethodException(
                "No consumable method with name $methodName was found in class ${this.javaClass.simpleName}."
        )
    }

}

/**
 * Get a specific producer from the [AttributeContainer] object.
 *
 * @param container      The container in which to find the producible.
 * @param methodName The name of the producible method.
 * @return The producer.
 */
fun AttributeContainer.getProducer(methodName: String): Producer {
    return try {
        val method = javaClass.getMethod(methodName)
        getProducer(method)
    } catch (ex: NoSuchMethodException) {
        throw IllegalArgumentException(ex)
    }
}

/**
 * Return whether the specified method is producible.
 */
fun Method.isProducible() = isAnnotationPresent(Producible::class.java)

/**
 * Return whether the specified method is consumable.
 */
fun Method.isConsumable() = isAnnotationPresent(Consumable::class.java)

/**
 * Create a producer from the specified method on the [AttributeContainer].
 */
fun AttributeContainer.getProducer(method: Method): Producer {
    val annotation = method.getAnnotation(Producible::class.java)
            ?: throw IllegalArgumentException("Method ${method.name} is not producible.")
    return Producer.builder(this, method)
            .description(annotation.description)
            .customDescription(getMethod(annotation.customDescriptionMethod))
            .arrayDescriptionMethod(getMethod(annotation.arrayDescriptionMethod))
            .visibility(annotation.defaultVisibility)
            .build()
}

/**
 * Create a consumer from the specified method on the
 * [AttributeContainer].
 */
fun AttributeContainer.getConsumer(method: Method): Consumer {
    val annotation = method.getAnnotation(Consumable::class.java)
            ?: throw IllegalArgumentException(String.format("Method %s in class %s is not consumable.",
                    method.name, method.declaringClass.simpleName))
    return Consumer.builder(this, method)
            .customDescription(getMethod(annotation.customDescriptionMethod))
            .description(annotation.description)
            .visibility(annotation.defaultVisibility)
            .build()
}

/**
 * Helper to get a method object given the object and methodname
 */
fun AttributeContainer.getMethod(methodName: String?) = try {
    javaClass.getMethod(methodName)
} catch (ex: NoSuchMethodException) {
    null
}