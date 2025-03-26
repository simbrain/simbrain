package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * Implement a simple hearing sensor. When the phrase is heard, the sensor is
 * activated and and outputValue is sent out.
 *
 * @author Jeff Yoshimi
 */
class Hearing(

    phrase: String = "Hi!",

    @UserParameter(
        label = "Output Amount",
        description = "The amount of activation to be sent to a neuron coupled with this sensor.",
        order = 20
    )
    var outputAmount: Double = 1.0

) : Sensor("""Hear: "$phrase""""), VisualizableEntityAttribute {

    @UserParameter(
        label = "Utterance",
        description = "The string or phrase associated with this sensor. Hearing sensors get activated when it senses a speech effectors of the same utterance.",
        order = 10
    )
    @set:Consumable(customDescriptionMethod = "getAttributeDescription")
    var phrase: String = phrase
        set(value) {
            field = value
            events.propertyChanged.fire()
        }

    @UserParameter(
        label = "Linger Time",
        description = "Anything heard will remain `heard` for this many iterations. " +
                "Allows the agent to process a signal before it disappears.",
        order = 30
    )
    var lingerTime: Int = 10

    /**
     * Maximum characters per row before warping around in a HearingNode.
     */
    @UserParameter(
        label = "Characters per Row",
        description = ("The maximum number of characters that can be displayed in one row in the hearing bubble. "
                + "This setting only affects visual representation."),
        order = 50
    )
    var charactersPerRow: Int = 32

    /**
     * Whether this is activated.
     */
    var isActivated: Boolean = false
        private set

    // Counter for lingertime
    private var counter = 0

    private var utteranceHeard = false

    fun hear(phrase: String) {
        if (phrase.equals(this.phrase, ignoreCase = true))
            utteranceHeard = true
    }

    override fun update(parent: OdorWorldEntity) {
        counter = (counter - 1).coerceAtLeast(0)

        if (utteranceHeard) {
            isActivated = true
            counter = lingerTime
            events.updated.fire()
        }

        utteranceHeard = false

        if (counter <= 0) {
            if (isActivated) {
                isActivated = false
                events.updated.fire()
            }
        }
    }

    @get:Producible(customDescriptionMethod = "getAttributeDescription")
    val value: Double get() = if (this.isActivated) outputAmount else 0.0

    override var label: String
        get() = if (super.label.isEmpty()) {
            "Hear $phrase"
        } else {
            super.label
        }
        set(value) {
            super.label = value
        }


    override val name = "Hearing Sensor"

    override fun copy(): Hearing {
        return Hearing(phrase, outputAmount).also {
            it.isActivated = this.isActivated
            it.counter = this.counter
            it.lingerTime = this.lingerTime
            it.charactersPerRow = this.charactersPerRow
        }
    }
}