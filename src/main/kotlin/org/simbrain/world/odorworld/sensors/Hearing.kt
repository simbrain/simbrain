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
        order = 5
    )
    var outputAmount: Double = 1.0

) : Sensor("""Hear: "$phrase""""), VisualizableEntityAttribute {

    @UserParameter(
        label = "Utterance",
        description = "The string or phrase associated with this sensor. Hearing sensors get activated when it senses a speech effectors of the same utterance.",
        order = 3
    )
    @set:Consumable(customDescriptionMethod = "getAttributeDescription")
    var phrase: String = phrase
        set(value) {
            field = value
            events.propertyChanged.fire()
        }

    /**
     * Maximum characters per row before warping around in a HearingNode.
     */
    @UserParameter(
        label = "Characters per Row",
        description = ("The maximum number of characters that can be displayed in one row in the hearing bubble. "
                + "This setting only affects visual representation."),
        order = 4
    )
    var charactersPerRow: Int = 32

    /**
     * Whether this is activated.
     */
    var isActivated: Boolean = false
        private set

    // TODO: Clean up / Make this settable
    private var time = 0

    private val heardPhrases = mutableListOf<String>()

    @UserParameter(
        label = "Linger Time",
        description = "The time to linger after the phrase is heard.",
        order = 10
    )
    var lingerTime = 10

    fun hear(phrase: String) {
        heardPhrases.add(phrase)
    }

    override fun update(parent: OdorWorldEntity) {
        time = (time - 1).coerceAtLeast(0)

        heardPhrases
            .filter { it.equals(phrase, ignoreCase = true) }
            .forEach {
                isActivated = true
                time = lingerTime
                events.updated.fire()
            }

        heardPhrases.clear()

        if (time <= 0) {
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
            it.time = this.time
            it.lingerTime = this.lingerTime
            it.charactersPerRow = this.charactersPerRow
        }
    }
}