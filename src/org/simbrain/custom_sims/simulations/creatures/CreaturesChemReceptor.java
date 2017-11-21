package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.network.groups.NeuronGroup;

/**
 * Chemical receptors can either alter a particular characteristic of a brain
 * lobe or it can affect the creature's life stage, chem emitters, fertility,
 * drive levels, make it perform involuntary actions (like sneezing), or even
 * kill it outright. What is targeted is set by three different variables:
 * organ, tissue, and locus. The different values these variables can take is
 * tabled in the url above.
 * 
 * The receptor monitors a certain chemical, and when that chemical passes a set
 * threshold the effect set by the receptor triggers (depending on whether it's
 * digital or analog).
 * 
 * {@link http://double.nz/creatures/genetics/receptor.htm}
 */
public class CreaturesChemReceptor {

	/**
	 * Determines if the receptor changes a property somewhere in the creature's
	 * brain (if true) or elsewhere in the creature (if false)
	 */
	private boolean organIsBrain;

	/** The brain lobe to target. Only relevant if organIsBrain is set to true */
	private NeuronGroup tissue;

	// Put a reference to the locus here (once we know how to implement that)

	/** Reference to the chemical checked */
	private CreaturesChem chemical;

	/**
	 * The threshold the receptor must pass before it can fire. Works differently
	 * depending on whether the receptor is analogue or digital
	 */
	private double threshold;

	/**
	 * The nominal value determines what the default value used for the locus will
	 * be when the chemical doesn't pass the threshold (only used for particular
	 * loci)
	 */
	private double nominal;

	/**
	 * Gain is used either as a scaling factor or further defines the value sent to
	 * the locus, depending on whether the receptor is analog or digital.
	 */
	private double gain;

	/**
	 * A flag for "Output REDUCES with increased stimulation". Hopefully self
	 * explanatory.
	 */
	private boolean outputReduces;

	/**
	 * This flag determines whether the receptor is analogue (false) or digital
	 * (true). Changing this setting will change how various aspects of the receptor
	 * works, such as how the "value" of the receptor is calculated.
	 */
	private boolean isDigital;

	/**
	 * The value the locus will be changed by. Calculated based on various factors
	 */
	private double value;

	// Formula for output value in an analog receptor: (R = -1 if outputReduces
	// is true, R = 1 otherwise)
	// Nominal + (((ChemicalAmount - Threshold) * Gain/255) * R)

	// Formula for output value in a digital receptor:
	// Nominal + ((ChemicalAmount > Threshold ? Gain : 0) * R)
}
