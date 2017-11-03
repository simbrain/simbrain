package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreaturesBiochem {

	/** List of chemicals */
	private List<CreaturesChem> chemList = new ArrayList<CreaturesChem>();

	/** List of reactions */
	private List<CreaturesChemReaction> reactionList = new ArrayList<CreaturesChemReaction>();

	public CreaturesBiochem() {
		initDefault();
	}

	/**
	 * Creates the biochemistry from a default template
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
		
//		CreaturesChem painIncrease = createNewChem("Pain++", 0, 2.51);
//		CreaturesChem comfortIncrease = createNewChem("Comfort++", 0, 2.51);
//		CreaturesChem hungerIncrease = createNewChem("Hunger++", 0, 2.51);
//		CreaturesChem tempIncrease = createNewChem("Heat", "Temperature++", 0, 2.51);
//		CreaturesChem fatigueIncrease = createNewChem("Fatigue++", 0, 2.51);
//		CreaturesChem drowsinessIncrease = createNewChem("Drowsiness++", 0, 2.51);
//		CreaturesChem lonlinessIncrease = createNewChem("Lonliness++", 0, 2.51);
//		CreaturesChem crowdednessIncrease = createNewChem("Crowdedness++", 0, 2.51);
//		CreaturesChem fearIncrease = createNewChem("Fear++", 0, 2.51);
//		CreaturesChem boredomIncrease = createNewChem("Boredom++", 0, 2.51);
//		CreaturesChem angerIncrease = createNewChem("Anger++", 0, 2.51);
//		CreaturesChem arousalIncrease = createNewChem("Arousal++", 0, 2.51);
		
		CreaturesChem endorphin = createNewChem("Endorphin", "Pain--", 0, 3);
		
		CreaturesChem reward = createNewChem("Reward", 0, 10);

		// Add reactions
		createNewReaction(1, endorphin, 1, pain, 1, reward, 0, NONE, 0.3);
	}

	public void update() {
		for (CreaturesChemReaction reaction : this.getReactionList()) {
			reaction.update();
		}

		for (CreaturesChem chem : this.getChemList()) {
			chem.decay();
		}
	}

	public CreaturesChem createNewChem(String name, String id, double amount, double halfLife) {
		CreaturesChem chem = new CreaturesChem(name, id, amount, halfLife);
		chemList.add(chem);
		return chem;
	}

	public CreaturesChem createNewChem(String name, double amount, double halfLife) {
		return createNewChem(name, name, amount, halfLife);
	}

	public void createNewReaction(double rRatio1, CreaturesChem reactant1, double rRatio2, CreaturesChem reactant2,
			double pRatio1, CreaturesChem product1, double pRatio2, CreaturesChem product2, double reactionRate) {

		CreaturesChemReaction react = new CreaturesChemReaction(rRatio1, reactant1, rRatio2, reactant2, pRatio1,
				product1, pRatio2, product2, reactionRate);
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
