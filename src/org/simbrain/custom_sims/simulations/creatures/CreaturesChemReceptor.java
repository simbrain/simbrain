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
 * digital or analog). The nominal value determines what the default value used
 * for the locus will be when the chemical doesn't pass the threshold (only used
 * for particular loci). Gain is used either as a scaling factor or further
 * defines the value sent to the locus, depending on whether the receptor is
 * analog or digital. The flag "Output REDUCES with increased stimulation" is
 * hopefully self explanatory.
 * 
 * {@link http://double.nz/creatures/genetics/receptor.htm}
 */
public class CreaturesChemReceptor {

    private boolean organIsBrain;

    private NeuronGroup tissue;

    // Put a reference to the locus here (once we know how to implement that)

    private CreaturesChem chemical;

    private double threshold;

    private double nominal;

    private double gain;

    private boolean outputReduces;

    private boolean isDigital;

    private double value;

    // Formula for output value in an analog receptor: (R = -1 if outputReduces
    // is true, R = 1 otherwise)
    // Nominal + (((ChemicalAmount - Threshold) * Gain/255) * R)

    // Formula for output value in a digital receptor:
    // Nominal + ((ChemicalAmount > Threshold ? Gain : 0) * R)
}
