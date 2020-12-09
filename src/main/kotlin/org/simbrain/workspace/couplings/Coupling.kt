package org.simbrain.workspace.couplings

import org.simbrain.workspace.*
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
 * Return whether the specified method is producible.
 */
fun Method.isProducible() = isAnnotationPresent(Producible::class.java)

/**
 * Return whether the specified method is consumable.
 */
fun Method.isConsumable() = isAnnotationPresent(Consumable::class.java)
