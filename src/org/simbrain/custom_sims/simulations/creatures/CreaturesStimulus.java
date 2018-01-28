package org.simbrain.custom_sims.simulations.creatures;

/**
 * Stimulus genes define how a creature will react to a stimulus, in terms of
 * chemistry or neuron activation. The effects of a stimulus gene occur when the
 * stimulus set for that gene occurs (ex. If a creature gets an object, all of
 * the creature's stimulus genes relating to "I have got" will trigger their
 * effects). There is a list of possible stimuli to react to.
 * <p>
 * Stimulus genes also have a "significance" value. When applicable, the neuron
 * in stimulus source that is representative of the object that caused the
 * stimulus gets increased activation based on the significance value. (Ex.
 * mouse A approaches mouse B, causing B's "It is approaching" stimulus gene to
 * activate. If the significance value of that gene is equal to five, then the
 * activation of the "Mouse" neuron in B's stimulus source lobe is increased by
 * five.)
 * <p>
 * A stimulus gene can target a neuron in the general sensory lobe. This is
 * accompanied by an intensity value, which acts for that sensory neuron just as
 * the significance value does for a stimulus source neuron.
 * <p>
 * Up to four chemicals can be injected when the effects of a stimulus gene is
 * activated. These four chemicals come with a value that sets how much of that
 * chemical is injected.
 * <p>
 * There are three boolean flags that can be checked or unchecked:
 * <p>
 * "Modulate using sensory signal" is not well understood, but it seems that it
 * would cause the significance value to first be modulated by the value of the
 * neuron associated with the stimulus before it's applied.
 */
public class CreaturesStimulus {

    // http://double.nz/creatures/genetics/stimulus.htm

}
