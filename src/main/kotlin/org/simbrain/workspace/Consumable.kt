package org.simbrain.workspace

import org.simbrain.workspace.couplings.LOW_PRIORITY

/**
 * Marks a method as a potential consumer for a [org.simbrain.workspace.couplings.Coupling]. The method
 * may be an ordinary function or a suspend function; see [Attribute] for how each is invoked.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_SETTER)
annotation class Consumable(

    /**
     * Use this if you would like the consumer to be described by a name you provide rather than the
     * method name. For more complex descriptions use [customDescriptionMethod].
     */
    val description: String = "",

    /**
     * The name of a method that returns a custom description for the consumer.
     */
    val customDescriptionMethod: String = "",

    /**
     * Whether this method should be visible in the coupling panels and menus by default. User visibility
     * settings override this value.
     */
    val defaultVisibility: Boolean = true,

    /**
     * See [Attribute.priority].
     */
    val priority: Int = LOW_PRIORITY,

    /**
     * The name of a method that can be used to set priority in a way that depends on the state of the
     * base object.
     */
    val customPriorityMethod: String = ""
)
