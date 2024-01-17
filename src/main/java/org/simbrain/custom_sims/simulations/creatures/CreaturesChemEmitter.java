package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.network.neurongroups.NeuronGroup;

/**
 * Emits chemicals "inside" the creature, depending on environmental factors.
 * When they "emit" they increase the value of one of the chemicals in
 * CreaturesBiochem Emitters are located inside the CreaturesBiochem object,
 * where they are set up by genes.
 * <p>
 * Reference: http://double.nz/creatures/genetics/emitter.htm
 * <p>
 * The activation of a chemical emitter can be taken from either particular
 * characteristics of a brain lobe or particular characteristics of a creature
 * itself, such as a chemical receptor's activation, if they are fertile or
 * pregnant, if they are dead or asleep, what their enviroment is like, or what
 * their drive levels are. What is targeted is set by three different variables:
 * organ, tissue, and locus. The different values these variables can take is
 * tabled in the url above.
 */
public class CreaturesChemEmitter {

    /**
     * Determines if the emitter checks a property somewhere in the creature's brain
     * (if true) or elsewhere in the creature (if false)
     */
    private boolean organIsBrain;

    /**
     * The brain lobe to check. Only relevant if organIsBrain is set to true
     */
    private NeuronGroup tissue;

    // Put a reference to the locus here (once we know how to implement that)

    /**
     * Reference to the chemical emitted
     */
    private CreaturesChem chemical;

    /**
     * This flag determines whether the emitter is analogue (false) or digital
     * (true). Changing this setting will change how various aspects of the emitter
     * works, such as how the "value" of the emitter is calculated.
     */
    private boolean isDigital;

    /**
     * Sample rate determines how often the emitter is checked and processed.
     */
    private double sampleRate;

    /**
     * Gain defines how much the amount of the chemical will be incremented by
     */
    private double gain;

    /**
     * The threshold the emitter must pass before it can fire. Works differently
     * depending on whether the emitter is analogue or digital
     */
    private double threshold;

    /**
     * If true, the value of the emitter will always reset to zero after it's been
     * processed, each time it's processed
     */
    private boolean clearAfterReading;

    /**
     * If true, the value will be set to it's inverse ((255 - value) in base game)
     * after calculation, as opposed to keeping it at (value)
     */
    private boolean invertInput;

    /**
     * The amount by which the set chemical will be incremented by when the emitter fires
     */
    private double value;

    // Formula for analog emitters:
    // value = (value - threshold) * (gain / 255)

    // Formula for digital emitters:
    // value = gain
}
