/**
 * Base class for transform operations that a [Coupling] applies between its producer and consumer,
 * modeled on the image world's ImageOperation but typed at both ends: an operation declares its input
 * and output types so a chain can be validated against the coupling's endpoints when the coupling is
 * created ([Coupling.create]), including type-bridging operations such as a mean from `double[]` to
 * `double`. Returning null from [apply] filters the tick: the chain stops and the consumer is not
 * invoked, which is how threshold and change-detection operations suppress delivery.
 *
 * Operations are [CopyableObject]s with [org.simbrain.util.propertyeditor.GuiEditable] parameters so the
 * property editor can edit them, and they are serialized directly into the workspace archive with their
 * coupling (see [org.simbrain.workspace.serialization.ArchivedCoupling]); transient runtime state such
 * as a change detector's last value must be marked so and survive absent. Unlike image operations there
 * is no enabled flag: a disabled type-bridging operation would break the chain's type contract, so
 * removing an operation from the chain is the way to switch it off.
 */
package org.simbrain.workspace.couplings

import org.simbrain.util.propertyeditor.CopyableObject

abstract class CouplingOperation<I, O> : CopyableObject {

    /**
     * The type this operation accepts, compared against the producer's type or the previous operation's
     * [outputType] with [org.simbrain.workspace.attributeTypesMatch].
     */
    abstract val inputType: Class<*>

    /**
     * The type this operation yields.
     */
    abstract val outputType: Class<*>

    /**
     * Transform one value. Returning null suppresses this tick: later operations and the consumer are
     * not invoked.
     */
    abstract suspend fun apply(input: I): O?

    abstract override fun copy(): CouplingOperation<I, O>

    override fun getTypeList() = couplingOperationTypes

    override fun toString() = name
}

/**
 * Apply an operation to a value whose static type was erased by chain storage. Safe when the chain was
 * validated at coupling creation.
 */
@Suppress("UNCHECKED_CAST")
internal suspend fun CouplingOperation<*, *>.applyErased(value: Any): Any? =
    (this as CouplingOperation<Any, Any>).apply(value)
