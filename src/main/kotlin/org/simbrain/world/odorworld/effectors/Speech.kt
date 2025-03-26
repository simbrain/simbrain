package org.simbrain.world.odorworld.effectors

import org.simbrain.util.UserParameter
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.workspace.Consumable
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.VisualizableEntityAttribute

/**
 * Model simple speech behaviors. Each speech effector is associated with a
 * specific phrase. To activate speech either manually activate or de-activate
 * it or set the "value" to a number above a threshold (currently set to 0).
 *
 * @author Jeff Yoshimi
 */
class Speech(
    @set:Consumable(customDescriptionMethod = "getAttributeDescription")
    @UserParameter(
        label = "Utterance",
        description = "The thing this speech effector says.",
        order = 3
    )
    var phrase: String = "Hi!",

    @UserParameter(
        label = "Threshold",
        description = "Threshold above which to \"the message\".",
        order = 5
    ) var threshold: Double = 0.1

) : Effector("""Say: "$phrase""""), VisualizableEntityAttribute {

    @UserParameter(
        label = "Characters per Row",
        description = ("The maximum number of characters that can be displayed in one row in the speech bubble. This setting only affects visual representation."),
        order = 4
    )
    var charactersPerRow: Int = 32

    @UserParameter(label = "Decay Function", order = 10, tab = "Dispersion")
    private var decayFunction: DecayFunction = LinearDecayFunction(128.0)

    /**
     * Whether this is activated. If so, display the phrase and notify all
     * hearing sensors.
     */
    var isActivated: Boolean = false

    /**
     * If amount is greater than threshold, activate the speech.
     */
    @set:Consumable(customDescriptionMethod = "getAttributeDescription")
    var amount = 0.0

    override fun update(parent: OdorWorldEntity) {
        if (amount > threshold) {
            if (!this.isActivated) {
                this.isActivated = true
                events.updated.fire()
            }
            amount = 0.0 // reset
        } else {
            if (this.isActivated) {
                this.isActivated = false
                events.updated.fire()
            }
        }
        if (this.isActivated) {
            // TODO: now using dispersion distance only, get real decay value and set threshold later.
            parent.getEntitiesInRadius(decayFunction.dispersion).filter { it != parent }.forEach {
                it.speakToEntity(phrase)
            }
        }
    }

    override fun copy(): Speech {
        return Speech(phrase, threshold).also {
            it.amount = this.amount
            it.isActivated = this.isActivated
            it.charactersPerRow = this.charactersPerRow
            it.decayFunction = this.decayFunction.copy() as DecayFunction
        }
    }

    override val name: String
        get() = "Speech Effector"

    override var label: String
        get() = if (super.label.isEmpty()) {
                "Say $phrase"
            } else {
                super.label
            }
        set(value) {
            super.label = value
        }
}