package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.List;

/**
 * This object maintains a fixed set of chemicals, reactions, emitters, and
 * receptors. The concentration of chemicals changes over time. When this is
 * updated the chemicals are updated, and all the reactions are updated.
 * Updating a reaction checks for an appropriate amount of the "input" chemicals
 * to it, and then if there is enough products or outputs are created which
 * change the levels of the chemicals
 */
public class CreaturesBiochem {

    /**
     * List of chemicals
     */
    private List<CreaturesChem> chemList = new ArrayList<CreaturesChem>();

    /**
     * List of reactions
     */
    private List<CreaturesChemReaction> reactionList = new ArrayList<CreaturesChemReaction>();

    /**
     * List of receptors
     */
    private List<CreaturesChemReceptor> receptorList = new ArrayList<CreaturesChemReceptor>();

    /**
     * List of emitters
     */
    private List<CreaturesChemEmitter> emitterList = new ArrayList<CreaturesChemEmitter>();

    public static boolean tempLogFlag = false;

    public CreaturesBiochem() {
        initDefault();
    }

    /**
     * Creates the biochemistry from a default template.
     */
    private void initDefault() {
        // Add chemicals
        // Reference: https://creatures.wiki/C1_Chemical_List
        CreaturesChem NONE = createNewChem("NONE", 0, 0);
        CreaturesChem pain = createNewChem("Pain", 0, 2.2);
        CreaturesChem comfort = createNewChem("Comfort", 0, 3.45); // Equivalent to NFP
        CreaturesChem hunger = createNewChem("Hunger", 0, 5.65);
        CreaturesChem temperature = createNewChem("Temperature", 0, 3.76); // Equivalent to Hotness/Coldness
        CreaturesChem fatigue = createNewChem("Fatigue", 0, 9.73); // Equivalent to Tiredness
        CreaturesChem drowsiness = createNewChem("Drowsiness", 0, 10); // Equivalent to Sleepiness
        CreaturesChem lonliness = createNewChem("Lonliness", 0, 2.82);
        CreaturesChem crowdedness = createNewChem("Crowdedness", 0, 2.51);
        CreaturesChem fear = createNewChem("Fear", 0, 3.14);
        CreaturesChem boredom = createNewChem("Boredom", 0, 9.73);
        CreaturesChem anger = createNewChem("Anger", 0, 2.82);
        CreaturesChem arousal = createNewChem("Arousal", 0, 4.10); // Equivalent to SexDrive

        // CreaturesChem painIncrease = createNewChem("Pain Increaser", 0, 2.51);
        // CreaturesChem comfortIncrease = createNewChem("Comfort++", 0, 2.51);
        // CreaturesChem hungerIncrease = createNewChem("Hunger++", 0, 2.51);
        // CreaturesChem tempIncrease = createNewChem("Heat", "Temperature++", 0, 2.51);
        // CreaturesChem fatigueIncrease = createNewChem("Fatigue++", 0, 2.51);
        // CreaturesChem drowsinessIncrease = createNewChem("Drowsiness++", 0, 2.51);
        // CreaturesChem lonlinessIncrease = createNewChem("Lonliness++", 0, 2.51);
        // CreaturesChem crowdednessIncrease = createNewChem("Crowdedness++", 0, 2.51);
        // CreaturesChem fearIncrease = createNewChem("Fear++", 0, 2.51);
        // CreaturesChem boredomIncrease = createNewChem("Boredom++", 0, 2.51);
        // CreaturesChem angerIncrease = createNewChem("Anger++", 0, 2.51);
        // CreaturesChem arousalIncrease = createNewChem("Arousal++", 0, 2.51);

        CreaturesChem endorphin = createNewChem("Endorphin", "Pain Decreaser", 0, 3);

        CreaturesChem reward = createNewChem("Reward", 0, 10);

        // Add reactions
        createNewReaction(1, endorphin, 1, pain, 1, reward, 0, NONE, 0.3);
    }

    /**
     * Updates the Biochemistry
     */
    public void update() {
        for (CreaturesChemReaction reaction : this.getReactionList()) {
            if (tempLogFlag) {
                System.out.println("Updating this reaction: " + reaction.toString());
            }
            reaction.update();
        }

        for (CreaturesChem chem : this.getChemList()) {
            chem.decay();
        }
    }

    /**
     * This method creates a new kind of chemical
     *
     * @param name     The name of the chemical
     * @param id       An id for the chemical, to use as a sort of "key"
     * @param amount   An initial concentration of the chemical to start with
     * @param halfLife Determines how quickly the chemical concentration will decay
     * @return
     */
    public CreaturesChem createNewChem(String name, String id, double amount, double halfLife) {
        CreaturesChem chem = new CreaturesChem(name, id, amount, halfLife);
        chemList.add(chem);
        return chem;
    }

    public CreaturesChem createNewChem(String name, double amount, double halfLife) {
        return createNewChem(name, name, amount, halfLife);
    }

    /**
     * Creates a new chemical reaction. A chemical reaction technically needs two
     * reactants and two products, but a placeholder chem ("NONE" by default) can be
     * used to simulate having less
     *
     * @param rRatio1      The amount (or concentration) of reactant 1 that is required
     * @param reactant1    The first reactant chemical
     * @param rRatio2      The amount (or concentration) of reactant 2 that is required
     * @param reactant2    The second reactant chemical
     * @param pRatio1      The amount (or concentration) of product 1 that is produced
     * @param product1     The first product chemical
     * @param pRatio2      The amount (or concentration) of product 2 that is produced
     * @param product2     The second product chemical
     * @param reactionRate Determines how fast this reaction occurs, as a sort of step
     *                     function
     */
    public void createNewReaction(double rRatio1, CreaturesChem reactant1, double rRatio2, CreaturesChem reactant2, double pRatio1, CreaturesChem product1, double pRatio2, CreaturesChem product2, double reactionRate) {

        CreaturesChemReaction react = new CreaturesChemReaction(rRatio1, reactant1, rRatio2, reactant2, pRatio1, product1, pRatio2, product2, reactionRate);
        reactionList.add(react);
    }

    /**
     * Returns the list of chemicals
     *
     * @return
     */
    public List<CreaturesChem> getChemList() {
        return chemList;
    }

    /**
     * Returns the list of chemical reactions
     *
     * @return
     */
    public List<CreaturesChemReaction> getReactionList() {
        return reactionList;
    }

    /**
     * Returns a chemical with that name.
     *
     * @param name
     * @return
     */
    public CreaturesChem getChemByName(String name) {
        for (CreaturesChem chem : chemList) {
            if (chem.getName().equalsIgnoreCase(name)) {
                return chem;
            }
        }
        return null;
    }

    /**
     * Returns a chemical with that id.
     *
     * @param id
     * @return
     */
    public CreaturesChem getChemById(String id) {
        for (CreaturesChem chem : chemList) {
            if (chem.getId().equalsIgnoreCase(id)) {
                return chem;
            }
        }
        return null;
    }

    public CreaturesChem getChemByIndex(int index) {
        return chemList.get(index);
    }

}
