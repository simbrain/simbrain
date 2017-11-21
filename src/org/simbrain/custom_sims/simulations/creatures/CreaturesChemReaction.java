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
	// TODO: Move these print statements to CreaturesBiochem, where they could be
	// used in conjunction with the temp log flag
	public void update() {
		if (checkAmounts()) {
			// System.out.println("Starting reaction!\n");

			// System.out.println(reactant1.getName() + " Before: " +
			// reactant1.getAmount());
			reactant1.incrementAmount(-(rRatio1 * reactionRate));
			// System.out.println(reactant1.getName() + " After: " + reactant1.getAmount());
			// System.out.println(reactant2.getName() + " Before: " +
			// reactant2.getAmount());
			reactant2.incrementAmount(-(rRatio2 * reactionRate));
			// System.out.println(reactant2.getName() + " After: " + reactant2.getAmount());

			// System.out.println(product1.getName() + " Before: " + product1.getAmount());
			product1.incrementAmount(pRatio1 * reactionRate);
			// System.out.println(product1.getName() + " After: " + product1.getAmount());
			// System.out.println(product2.getName() + " Before: " + product2.getAmount());
			product2.incrementAmount(pRatio2 * reactionRate);
			// System.out.println(product2.getName() + " After: " + product2.getAmount());
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

}
