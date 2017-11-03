package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.network.groups.NeuronGroup;

public class CreaturesChemEmitter {
	
	private boolean organIsBrain;
	
	private NeuronGroup tissue;
	
	// Put a reference to the locus here (once we know how to implement that)
	
	private CreaturesChem chemical;
	
	private boolean isDigital;
	
	private double sampleRate;
	
	private double gain;
	
	private double threshold;
	
	private boolean clearAfterReading;
	
	private boolean invertInput;
	
	private double value;
	
	// http://double.nz/creatures/genetics/emitter.htm

	/* The activation of a chemical emitter can be taken from either particular characteristics
	 * of a brain lobe or particular characteristics of a creature itself, such as a chemical 
	 * receptor's activation, if they are fertile or pregnant, if they are dead or asleep, what 
	 * their enviroment is like, or what their drive levels are.
	 * What is targeted is set by three different variables: organ, tissue, and locus.
	 * The different values these variables can take is tabled in the url above.
	 */
	
	/* The sample rate determines how often the emitter is checked and processed. Gain defines how
	 * much of the chemical will be injected. The value of the emitter must pass a threshold before
	 * it can fire (with the exact threshold depending on whether it's digital or analog). If 
	 * "Clear Source Byte after Reading" is checked, then the value is set to zero after the emitter 
	 * is processed, each time it is processed. If "Invert Input Signal" is checked, then the value used
	 * in calculations will be (255 - value) instead of (value). The resulting output of the emitter does
	 * not replace the set chemical's amount, but adds onto it.
	 */
	
	// Formula for analog emitters:
	// value = (value - threshold) * (gain / 255)
	
	// Formula for digital emitters:
	// value = gain
}
