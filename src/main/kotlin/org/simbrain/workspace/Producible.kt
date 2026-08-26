package org.simbrain.workspace

import org.simbrain.workspace.couplings.LOW_PRIORITY

/**
 * Marks a method as a potential producer for a [org.simbrain.workspace.couplings.Coupling]. The method
 * may be an ordinary function or a suspend function; see [Attribute] for how each is invoked.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Producible(

    /**
     * Use this if you would like the producer to be described by a name you provide rather than the
     * method name. For more complex descriptions use [customDescriptionMethod].
     */
    val description: String = "",

    /**
     * The name of a method that returns a custom description for the producer.
     */
    val customDescriptionMethod: String = "",

    /**
     * (For attributes of type double[] only.) The name of a method returning a
     * `List<AttributeComponent>` describing each component of the array produced by this producer: a
     * stable key and a display name apiece. Example: a neuron collection returns its neurons' ids paired
     * with their labels, which a bar chart uses to name its bars and a time series uses to follow a
     * neuron's series when the collection's membership changes.
     */
    val arrayComponentsMethod: String = "",

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
     * base object. See [org.simbrain.network.core.Neuron] for examples.
     */
    val customPriorityMethod: String = ""
)
