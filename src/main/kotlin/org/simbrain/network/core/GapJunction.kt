/**
 * A bidirectional electrical connection between two neurons. Unlike a [Synapse] it has no source/target
 * distinction: one conductance produces equal and opposite currents g × (V_other − V_this) into both
 * endpoints, read from the internal membrane potentials their update rules expose. Endpoint neurons pull
 * these currents during input accumulation; see [Neuron.accumulateInputs].
 */
package org.simbrain.network.core

import kotlinx.coroutines.Dispatchers
import org.simbrain.network.events.GapJunctionEvents
import org.simbrain.network.updaterules.interfaces.MembranePotentialProvider
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable

class GapJunction @XStreamConstructor constructor(
    val neuron1: Neuron,
    val neuron2: Neuron
) : NetworkModel(), EditableObject {

    @Transient
    override val events: GapJunctionEvents = GapJunctionEvents()

    /**
     * The conductance g shared by both directions of the junction, in the same units as synaptic
     * strength. Current into each endpoint is g × (V_other − V_this).
     */
    var conductance by GuiEditable(
        initValue = 1.0,
        label = "Conductance",
        description = "Conductance g shared by both directions; current into each endpoint is g × (V other − V this).",
        min = 0.0,
        increment = .1,
        order = 1,
        setter = { value ->
            field = value.coerceAtLeast(0.0)
            events.conductanceUpdated.fire()
        }
    )

    @UserParameter(
        label = "Enabled",
        description = "Junction is enabled. If disabled, it passes no current.",
        order = 2
    )
    var isEnabled: Boolean = true

    @UserParameter(
        label = "Increment",
        description = "Amount the conductance changes when nudged with the arrow keys.",
        minimumValue = 0.0,
        order = 4
    )
    var increment: Double = 0.1

    /**
     * Display reference against which the canvas glyph is scaled; the conductance itself is not
     * clamped to it.
     */
    var upperBound by GuiEditable(
        initValue = 10.0,
        label = "Upper bound",
        description = "Reference maximum conductance used to scale the channel glyph. The conductance " +
            "is not clamped to it.",
        min = 0.1,
        increment = 1.0,
        order = 3,
        setter = { value ->
            field = value.coerceAtLeast(0.1)
            events.conductanceUpdated.fire()
        }
    )

    init {
        if (neuron1 !== neuron2 && neuron1.gapJunctions.none { it.connects(neuron1, neuron2) }) {
            neuron1.addGapJunction(this)
            neuron2.addGapJunction(this)
        }
        neuron1.events.locationChanged.on(Dispatchers.Default) { events.locationChanged.fire() }
        neuron2.events.locationChanged.on(Dispatchers.Default) { events.locationChanged.fire() }
    }

    constructor(neuron1: Neuron, neuron2: Neuron, conductance: Double) : this(neuron1, neuron2) {
        this.conductance = conductance
    }

    constructor(neuron1: Neuron, neuron2: Neuron, template: GapJunction) : this(neuron1, neuron2) {
        copyFrom(template)
    }

    fun copyFrom(template: GapJunction) {
        commonCopyFrom(template)
        conductance = template.conductance
        isEnabled = template.isEnabled
        upperBound = template.upperBound
        increment = template.increment
    }

    override fun increment() {
        conductance += increment
    }

    override fun decrement() {
        conductance -= increment
    }

    fun connects(a: Neuron, b: Neuron): Boolean =
        (neuron1 === a && neuron2 === b) || (neuron1 === b && neuron2 === a)

    fun otherEndpoint(neuron: Neuron): Neuron? = when {
        neuron === neuron1 -> neuron2
        neuron === neuron2 -> neuron1
        else -> null
    }

    /**
     * True when both endpoint rules expose a membrane potential; an inert junction passes no current.
     */
    val isActive: Boolean
        get() = neuron1.updateRule is MembranePotentialProvider && neuron2.updateRule is MembranePotentialProvider

    /**
     * The current this junction contributes to [target] this step, computed from both endpoints'
     * previous-step membrane potentials. Zero when disabled, inert, or [target] is not an endpoint.
     */
    fun currentInto(target: Neuron): Double {
        if (!isEnabled) return 0.0
        val other = otherEndpoint(target) ?: return 0.0
        val targetRule = target.updateRule as? MembranePotentialProvider ?: return 0.0
        val otherRule = other.updateRule as? MembranePotentialProvider ?: return 0.0
        return conductance * (otherRule.membranePotential(other) - targetRule.membranePotential(target))
    }

    context(Network)
    override fun shouldAdd(): Boolean {
        // Registration state is not checked because undo re-adds the model before afterRestore
        // re-registers it with the endpoints; only self-junctions and duplicate pairs are rejected.
        return neuron1 !== neuron2 && neuron1.gapJunctions.none { it !== this && it.connects(neuron1, neuron2) }
    }

    override suspend fun delete(): List<NetworkModel> {
        neuron1.removeGapJunction(this)
        neuron2.removeGapJunction(this)
        events.deleted.fire(this)
        return listOf(this)
    }

    override suspend fun afterRestore(context: Any?) {
        neuron1.addGapJunction(this)
        neuron2.addGapJunction(this)
    }

    override val name: String
        get() = id ?: "Gap junction"

    override fun toString(): String =
        "$displayName: ${neuron1.displayName} ↔ ${neuron2.displayName}, g = $conductance"
}
