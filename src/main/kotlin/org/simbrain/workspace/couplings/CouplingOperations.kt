/**
 * The standard library of [CouplingOperation]s: same-type scalar transforms (scale, offset, coerce),
 * filters that suppress delivery by returning null (threshold, deadband, on-change), and type-bridging
 * operations between `double[]` and `double` (mean, sum, max, element pick, broadcast).
 * [couplingOperationTypes] lists them for type-choosing editors.
 */
package org.simbrain.workspace.couplings

import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.GuiEditable
import kotlin.math.abs

abstract class ScalarOperation : CouplingOperation<Double, Double>() {
    override val inputType: Class<*> get() = Double::class.java
    override val outputType: Class<*> get() = Double::class.java
}

abstract class ArrayToScalarOperation : CouplingOperation<DoubleArray, Double>() {
    override val inputType: Class<*> get() = DoubleArray::class.java
    override val outputType: Class<*> get() = Double::class.java
}

class ScaleOperation() : ScalarOperation() {

    constructor(factor: Double) : this() {
        this.factor = factor
    }

    var factor by GuiEditable(
        initValue = 1.0,
        label = "Factor",
        description = "The value is multiplied by this factor"
    )

    override val name = "Scale"

    override val displayLabel get() = "Scale ×${factor.compact()}"

    override suspend fun apply(input: Double) = input * factor

    override fun copy() = ScaleOperation(factor)
}

class OffsetOperation() : ScalarOperation() {

    constructor(amount: Double) : this() {
        this.amount = amount
    }

    var amount by GuiEditable(
        initValue = 0.0,
        label = "Amount",
        description = "This amount is added to the value"
    )

    override val name = "Offset"

    override val displayLabel get() = "Offset ${if (amount < 0) "−${(-amount).compact()}" else "+${amount.compact()}"}"

    override suspend fun apply(input: Double) = input + amount

    override fun copy() = OffsetOperation(amount)
}

class CoerceInOperation() : ScalarOperation() {

    constructor(min: Double, max: Double) : this() {
        this.min = min
        this.max = max
    }

    var min by GuiEditable(
        initValue = 0.0,
        label = "Min",
        description = "Values below this are raised to it"
    )

    var max by GuiEditable(
        initValue = 1.0,
        label = "Max",
        description = "Values above this are lowered to it"
    )

    override val name = "Coerce in"

    override val displayLabel get() = "Coerce in [${min.compact()}, ${max.compact()}]"

    override suspend fun apply(input: Double) = input.coerceIn(min, max)

    override fun copy() = CoerceInOperation(min, max)
}

/**
 * Passes the value only when it is at or above the threshold; below it, nothing is delivered this tick.
 */
class ThresholdOperation() : ScalarOperation() {

    constructor(threshold: Double) : this() {
        this.threshold = threshold
    }

    var threshold by GuiEditable(
        initValue = 1.0,
        label = "Threshold",
        description = "Values below this are not delivered"
    )

    override val name = "Threshold"

    override val displayLabel get() = "Threshold ≥ ${threshold.compact()}"

    override suspend fun apply(input: Double) = input.takeIf { it >= threshold }

    override fun copy() = ThresholdOperation(threshold)
}

/**
 * Suppresses values whose magnitude is below epsilon, so near-zero noise is not delivered.
 */
class DeadbandOperation() : ScalarOperation() {

    constructor(epsilon: Double) : this() {
        this.epsilon = epsilon
    }

    var epsilon by GuiEditable(
        initValue = 0.01,
        label = "Epsilon",
        description = "Values with magnitude below this are not delivered"
    )

    override val name = "Deadband"

    override val displayLabel get() = "Deadband ±${epsilon.compact()}"

    override suspend fun apply(input: Double) = input.takeIf { abs(it) >= epsilon }

    override fun copy() = DeadbandOperation(epsilon)
}

/**
 * Delivers only when the value differs from the last delivered one, so a level signal becomes a change
 * signal. The first value after creation or deserialization is always delivered.
 */
class OnChangeOperation : ScalarOperation() {

    @Transient
    private var previous: Double? = null

    override val name = "On change"

    override suspend fun apply(input: Double) = input.takeIf { it != previous }?.also { previous = it }

    override fun copy() = OnChangeOperation()
}

class MeanOperation : ArrayToScalarOperation() {

    override val name = "Mean"

    override suspend fun apply(input: DoubleArray) = if (input.isEmpty()) 0.0 else input.average()

    override fun copy() = MeanOperation()
}

class SumOperation : ArrayToScalarOperation() {

    override val name = "Sum"

    override suspend fun apply(input: DoubleArray) = input.sum()

    override fun copy() = SumOperation()
}

class MaxOperation : ArrayToScalarOperation() {

    override val name = "Max"

    override suspend fun apply(input: DoubleArray) = input.maxOrNull() ?: 0.0

    override fun copy() = MaxOperation()
}

/**
 * Picks one component of an array. An index outside the array suppresses delivery rather than throwing,
 * since array producers can legitimately shrink while a coupling is running.
 */
class ElementOperation() : ArrayToScalarOperation() {

    constructor(index: Int) : this() {
        this.index = index
    }

    var index by GuiEditable(
        initValue = 0,
        label = "Index",
        description = "Which component of the array to pass on",
        min = 0
    )

    override val name = "Element"

    override val displayLabel get() = "Element [$index]"

    override suspend fun apply(input: DoubleArray) = input.getOrNull(index)

    override fun copy() = ElementOperation(index)
}

/**
 * Repeats a scalar into an array of the given size, e.g. to drive every neuron in a collection with one
 * value.
 */
class BroadcastOperation() : CouplingOperation<Double, DoubleArray>() {

    constructor(size: Int) : this() {
        this.size = size
    }

    var size by GuiEditable(
        initValue = 1,
        label = "Size",
        description = "Length of the produced array",
        min = 1
    )

    override val inputType: Class<*> get() = Double::class.java
    override val outputType: Class<*> get() = DoubleArray::class.java

    override val name = "Broadcast"

    override val displayLabel get() = "Broadcast ×$size"

    override suspend fun apply(input: Double) = DoubleArray(size) { input }

    override fun copy() = BroadcastOperation(size)
}

/**
 * Scales an array so its largest magnitude is 1, leaving all-zero arrays unchanged.
 */
class NormalizeOperation : CouplingOperation<DoubleArray, DoubleArray>() {

    override val inputType: Class<*> get() = DoubleArray::class.java
    override val outputType: Class<*> get() = DoubleArray::class.java

    override val name = "Normalize"

    override suspend fun apply(input: DoubleArray): DoubleArray {
        val largest = input.maxOfOrNull { abs(it) } ?: 0.0
        return if (largest == 0.0) input else DoubleArray(input.size) { input[it] / largest }
    }

    override fun copy() = NormalizeOperation()
}

private fun Double.compact(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

val couplingOperationTypes: List<Class<out CopyableObject>> = listOf(
    ScaleOperation::class.java,
    OffsetOperation::class.java,
    CoerceInOperation::class.java,
    ThresholdOperation::class.java,
    DeadbandOperation::class.java,
    OnChangeOperation::class.java,
    MeanOperation::class.java,
    SumOperation::class.java,
    MaxOperation::class.java,
    ElementOperation::class.java,
    BroadcastOperation::class.java,
    NormalizeOperation::class.java,
)
