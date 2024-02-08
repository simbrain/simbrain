package org.simbrain.custom_sims.simulations.creatures;

// import org.simbrain.network.core.Synapse;
// import org.simbrain.network.learningrules.StaticSynapseRule;
// import org.simbrain.network.util.ScalarDataHolder;
//
// /**
//  * @author Sharai
//  */
// public class CreaturesSynapseRule extends StaticSynapseRule {
//
//     /**
//      * Dendrites in Creatures can be of two different types, type 1 and type 2.
//      * These two different types do not differ from one another in any way
//      * beyond what settings they're given when they're defined in a lobe's gene
//      * code.
//      *
//      * @author Sharai
//      */
//     public static enum DendriteType {
//         TYPE1, TYPE2
//     }
//
//     /**
//      * Short Term Weight.
//      */
//     private double STW;
//
//     /**
//      * Long Term Weight.
//      */
//     private double LTW;
//
//     /**
//      * Susceptibility to reinforcement. Not to be confused with the
//      * susceptibility SVRule, which defines what to set this variable at.
//      */
//     private double susceptibility;
//
//     /**
//      * The rate at which the synapse's susceptibility relaxes back to it's rest
//      * state.
//      */
//     // A rest state of zero, maybe? Not clear what rest state Chris Double was
//     // referring to.
//     private double relaxSusceptibility;
//
//     /**
//      * The rate at which STW relaxes back to the LTW.
//      */
//     private double relaxSTW;
//
//     /**
//      * The rate at which LTW rises towards STW, assuming LTWGainRateToggle is
//      * set to true.
//      */
//     private double LTWGainRate;
//
//     /**
//      * A flag that determines whether LTW will rise towards STW or not.
//      */
//     private Boolean LTWGainRateToggle;
//
//     /**
//      * Controls dendritic migration. The higher this is, the less likely it will
//      * migrate. Note: linkStrength is sometimes referred to as just 'strength'
//      * in documents. Do NOT confuse this for the strength/activation of the
//      * weight.
//      */
//     private double linkStrength;
//
//     /**
//      * The amount that linkStrength will increase by when the Gain By SVRule
//      * result is greater than zero.
//      */
//     private double gain;
//
//     /**
//      * The amount that the linkStrength will decrease by when the Lose By SVRule
//      * result is greater than zero.
//      */
//     // TODO: The current name reads a bit strange. Maybe we should rename this
//     // to
//     // something else? (But not loss)
//     private double lose;
//
//     /**
//      * Sets the dendrite type.
//      */
//     private DendriteType dendriteType;
//
//     public double getSTW() {
//         return STW;
//     }
//
//     public double getLTW() {
//         return LTW;
//     }
//
//     public double getSuscept() {
//         return susceptibility;
//     }
//
//     public double getRelaxSuscept() {
//         return relaxSusceptibility;
//     }
//
//     public double getRelaxSTW() {
//         return relaxSTW;
//     }
//
//     public double getLTWGainRate() {
//         return LTWGainRate;
//     }
//
//     public double getLinkStrength() {
//         return linkStrength;
//     }
//
//     public DendriteType getType() {
//         return dendriteType;
//     }
//
//     public Boolean getLTWGainRateToggle() {
//         return LTWGainRateToggle;
//     }
//
//     public double getGain() {
//         return gain;
//     }
//
//     public void setGain(double num) {
//         gain = num;
//     }
//
//     public double getLose() {
//         return lose;
//     }
//
//     public void setLose(double num) {
//         lose = num;
//     }
//
//     @Override
//     public String getName() {
//         return "Creatures Synapse";
//     }
//
//     @Override
//     public void apply(Synapse synapse, ScalarDataHolder data) {
//         // super.update(synapse, data);
//         //System.out.println("updating synapse: " + synapse.getId());
//     }
//
// }
