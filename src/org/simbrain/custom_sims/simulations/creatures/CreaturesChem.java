package org.simbrain.custom_sims.simulations.creatures;

public class CreaturesChem {

	/** The name of the chemical */
	private String name;
	
	/** The id of the chemical */
	private String id;

	/** The amount of this chemical currently in the creature's body */
	private double amount;

	// Half-Life Reference:
	// http://discoveralbia.com/2013/01/genetics-lesson-examining-a-creatures-half-life.html
	/**
	 * The half-life of the chemical, as defined by the creature's genetics
	 * Half-lives in C1 behave more like full-lives - the amount of time it takes
	 * for a chemical to decay down to zero, as opposed to half
	 */
	private double halfLife;
	
	public CreaturesChem(String name, String id, double amount, double halfLife) {
		this.name = name;
		this.id = id;
		this.amount = amount;
		this.halfLife = halfLife;
	}
	
	public String getName() {
		return(name);
	}
	
	public String getId() {
		return(id);
	}

	public double getAmount() {
		return(amount);
	}

	public void setAmount(double newAmount) {
		this.amount = newAmount;
	}

	public void incrementAmount(double increment) {
		this.amount += increment;
	}

	public void decay() {
		// TODO: Make this equation work more closely with how it works in Creatures
		System.out.println("\n" + name + " is decaying!");
		System.out.println(name + " Before: " + amount);
		double newAmount = this.amount * (this.halfLife / 10);
		this.amount = Math.max(0, newAmount);
		System.out.println(name + " After: " + amount);
	}

}
