package org.simbrain.custom_sims.simulations.creatures;

/**
 * Chemical reactions produce more of up to two set chemicals (called products)
 * by using up some amount of up to two chemicals (called reactants). This is
 * simulated simply by decrementing the "amount" of the reactants and
 * incrementing the "amount" of products by a value dependent on the ratio of
 * the chemicals in the reaction and the rate by which the reaction occurs, such
 * that: reactant -= (ratio * rate), product += (ratio * rate). Ratios are set
 * individually for each chem, but there is only one rate.
 *
 */
public class CreaturesChemReaction {

	/** The first reactant chemical */
	private CreaturesChem reactant1;

	/** The second reactant chemical */
	private CreaturesChem reactant2;

	/** The amount of reactant 1 used */
	private double rRatio1;

	/** The amount of reactant 2 used */
	private double rRatio2;

	/** The first product chemical */
	private CreaturesChem product1;

	/** The second product chemical */
	private CreaturesChem product2;

	/** The amount of product 1 created */
	private double pRatio1;

	/** The amount of product 2 created */
	private double pRatio2;

	/** The rate at which these reactions are made */
	private double reactionRate;

	/** Constructor */
	public CreaturesChemReaction(double rRatio1, CreaturesChem reactant1, double rRatio2, CreaturesChem reactant2,
			double pRatio1, CreaturesChem product1, double pRatio2, CreaturesChem product2, double reactionRate) {
		this.reactant1 = reactant1;
		this.reactant2 = reactant2;

		this.rRatio1 = rRatio1;
		this.rRatio2 = rRatio2;

		this.product1 = product1;
		this.product2 = product2;

		this.pRatio1 = pRatio1;
		this.pRatio2 = pRatio2;

		this.reactionRate = reactionRate;
	}

	/** Update function */
	public void update() {
		if (checkAmounts()) {

			double react1Old = reactant1.getAmount();
			double react2Old = reactant2.getAmount();
			double prod1Old = product1.getAmount();
			double prod2Old = product2.getAmount();

			reactant1.incrementAmount(-(rRatio1 * reactionRate));
			reactant2.incrementAmount(-(rRatio2 * reactionRate));
			product1.incrementAmount(pRatio1 * reactionRate);
			product2.incrementAmount(pRatio2 * reactionRate);

			if (CreaturesBiochem.tempLogFlag) {
				if (!reactant1.getId().equals("NONE")) {
					System.out.println(
							reactant1.getName() + " dropped from " + react1Old + " to " + reactant1.getAmount());
				}
				if (!reactant2.getId().equals("NONE")) {
					System.out.println(
							reactant2.getName() + " dropped from " + react2Old + " to " + reactant2.getAmount());
				}
				if (!product1.getId().equals("NONE")) {
					System.out.println(product1.getName() + " rose from " + prod1Old + " to " + product1.getAmount());
				}
				if (!product2.getId().equals("NONE")) {
					System.out.println(product2.getName() + " rose from " + prod2Old + " to " + product2.getAmount());
				}
			}
		} else if (CreaturesBiochem.tempLogFlag) {
			System.out.println("No reaction occured.");
		}
	}

	/**
	 * This method checks to see that there is at least the minimum concentration of
	 * reaction chemicals needed to perform the reaction
	 * 
	 * @return True if there is enough
	 */
	private boolean checkAmounts() {
		double requirement1 = rRatio1 * reactionRate;
		double requirement2 = rRatio2 * reactionRate;
		if ((reactant1.getAmount() > requirement1) && (reactant2.getAmount() > requirement2)) {
			return true;
		}

		// System.out.println("Not enough reactants");
		return false;
	}

	public String toString() {
		String retString = "";

		// First half of string
		if (!reactant1.getId().equals("NONE") && !reactant2.getId().equals("NONE")) {
			retString += rRatio1 + "*" + reactant1.getName() + " + " + rRatio2 + "*" + reactant2.getName();
		} else if (!reactant1.getId().equals("NONE")) {
			retString += rRatio1 + "*" + reactant1.getName();
		} else if (!reactant2.getId().equals("NONE")) {
			retString += rRatio2 + "*" + reactant2.getName();
		} else {
			return "Not a valid chemical reaction";
		}

		retString += " => ";

		// Second half of string
		if (!product1.getId().equals("NONE") && !product2.getId().equals("NONE")) {
			retString += pRatio1 + "*" + product1.getName() + " + " + pRatio2 + "*" + product2.getName();
		} else if (!product1.getId().equals("NONE")) {
			retString += pRatio1 + "*" + product1.getName();
		} else if (!product2.getId().equals("NONE")) {
			retString += pRatio2 + "*" + product2.getName();
		} else {
			retString += "NONE";
		}

		return retString;
	}

}
