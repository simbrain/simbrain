package org.simbrain.workspace.couplings

import org.simbrain.workspace.*
import java.lang.reflect.Method
import java.lang.reflect.Type

/**
 * A pair containing a producer and a consumer, with an optional chain of [transforms] between them.
 * When updated, the producer produces a value, the transforms are applied in order, and the consumer
 * consumes the result.
 *
 * @see http://www.simbrain.net/Documentation/docs/Pages/Workspace/Couplings.html
 * @see https://www.youtube.com/watch?v=zDUY9mUKZ-I
 */
class Coupling private constructor(
    val producer: Producer,
    val consumer: Consumer,
    val transforms: List<CouplingOperation<*, *>>
) {

    /**
     * This is the main action!  Set the value of the consumer based on the value of the producer, run
     * through the transform chain. Suspends when an attribute method or transform does. A null from the
     * producer or from any transform means "nothing this tick": the consumer is not invoked.
     *
     * Note that values are passed by reference, so that it is up to the producing or
     * consuming methods to make defensive copies as needed.
     * (cf http://www.javapractices.com/topic/TopicAction.do?Id=15)).
     */
    suspend fun update() {
        var value = producer.getValue() ?: return
        for (transform in transforms) {
            value = transform.applyErased(value) ?: return
        }
        consumer.setValue(value)
    }

    /**
     * The type of value entering the coupling; what leaves it is [consumer]'s type, which a transform
     * chain may make different.
     */
    val type: Type
        get() = producer.type

    /**
     * Identifies the coupling by its endpoints alone, so ids stay stable for archives regardless of
     * transforms.
     */
    val id: String
        get() = "${producer.id} > ${consumer.id}"

    val description: String
        get() = if (transforms.isEmpty()) {
            "$producer > $consumer"
        } else {
            "$producer > ${transforms.joinToString(" > ") { it.name }} > $consumer"
        }

    override fun toString() = description

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coupling

        if (producer != other.producer) return false
        if (consumer != other.consumer) return false
        if (transforms != other.transforms) return false

        return true
    }

    override fun hashCode(): Int {
        var result = producer.hashCode()
        result = 31 * result + consumer.hashCode()
        result = 31 * result + transforms.hashCode()
        return result
    }

    companion object {

        /**
         * Main creation method for couplings. The chain must type-check end to end: the producer's type
         * into the first transform, each transform's output into the next, and the last one (or the
         * producer, with no transforms) into the consumer, where boxed and primitive forms are
         * interchangeable (see [attributeTypesMatch]).
         */
        @Throws(MismatchedAttributesException::class)
        fun create(
            producer: Producer?,
            consumer: Consumer?,
            transforms: List<CouplingOperation<*, *>> = emptyList()
        ): Coupling {
            if (producer == null || consumer == null) {
                throw IllegalArgumentException("Producer and Consumer cannot be null")
            }
            chainError(producer, consumer, transforms)?.let { throw MismatchedAttributesException(it) }
            return Coupling(producer, consumer, transforms)
        }

        /**
         * Why this chain does not type-check between these endpoints, or null when it does. Used both
         * by [create] and for live validation in the transform editor.
         */
        fun chainError(
            producer: Producer,
            consumer: Consumer,
            transforms: List<CouplingOperation<*, *>>
        ): String? {
            var current: Type = producer.type
            var upstream = producer.toString()
            for (transform in transforms) {
                if (!attributeTypesMatch(current, transform.inputType)) {
                    return "$upstream type $current does not match transform ${transform.name} " +
                            "input type ${transform.inputType}"
                }
                current = transform.outputType
                upstream = "transform ${transform.name}"
            }
            if (!attributeTypesMatch(current, consumer.type)) {
                return "$upstream type $current does not match consumer type ${consumer.type}"
            }
            return null
        }
    }
}

/**
 * Return whether the specified method is producible: annotated, and taking no arguments beyond the
 * trailing continuation of a suspend function. Versions of the function that take arguments are ignored.
 */
fun Method.isProducible() = isAnnotationPresent(Producible::class.java) &&
        parameterCount == (if (isSuspendAttribute) 1 else 0)

/**
 * Return whether the specified method is consumable: annotated, and taking exactly the value argument
 * plus, for a suspend function, its trailing continuation.
 */
fun Method.isConsumable() = isAnnotationPresent(Consumable::class.java) &&
        parameterCount == (if (isSuspendAttribute) 2 else 1)
