package org.simbrain.custom_sims.simulations.creatures;

/**
 * 
 *
 */
public class CreaturesChemReaction {

	/** The reactants */
	private CreaturesChem reactant1;

	private CreaturesChem reactant2;

	/** The amount of each reactant used */
	private double rRatio1;

	private double rRatio2;

	/** The products */
	private CreaturesChem product1;

	private CreaturesChem product2;

	/** The amount of each product created */
	private double pRatio1;

	private double pRatio2;

	/** The rate at which these reactions are made */
	private double reactionRate;

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

	public void update() {
		if (checkAmounts()) {
//			System.out.println("Starting reaction!\n");

//			System.out.println(reactant1.getName() + " Before: " + reactant1.getAmount());
			reactant1.incrementAmount(-(rRatio1 * reactionRate));
//			System.out.println(reactant1.getName() + " After: " + reactant1.getAmount());
//			System.out.println(reactant2.getName() + " Before: " + reactant2.getAmount());
			reactant2.incrementAmount(-(rRatio2 * reactionRate));
//			System.out.println(reactant2.getName() + " After: " + reactant2.getAmount());

//			System.out.println(product1.getName() + " Before: " + product1.getAmount());
			product1.incrementAmount(pRatio1 * reactionRate);
//			System.out.println(product1.getName() + " After: " + product1.getAmount());
//			System.out.println(product2.getName() + " Before: " + product2.getAmount());
			product2.incrementAmount(pRatio2 * reactionRate);
//			System.out.println(product2.getName() + " After: " + product2.getAmount());
		}
	}

	/** Makes sure there is enough reactants to make the products */
	private boolean checkAmounts() {
		double requirement1 = rRatio1 * reactionRate;
		double requirement2 = rRatio2 * reactionRate;
		if ((reactant1.getAmount() > requirement1) && (reactant2.getAmount() > requirement2)) {
			return true;
		}

//		System.out.println("Not enough reactants");
		return false;
	}

}
