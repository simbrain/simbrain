package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.workspace.AttributeContainer;

/**
 * Creatures have a list of chemicals (see CreaturesBiochem) that evolve over
 * time, with a decay dynamic. They never disappear they just go to zero. When
 * reactions happen pre-existing chemicals increase their value. Value
 * represents how much is in the system.
 * <p>
 * There are (roughly) four lists of things "inside" the creature
 * <p>
 * <ul>
 * <li>Chemicals: see above</li>
 * <li>Emitters: increase values of chemicals</li>
 * <li>Receptors: detect chemical values and do things based on that</li>
 * <li>Reactors: if reactants get high enough, the reaction occurs, which
 * decreases values of reactants and increases the values of products.</li>
 * </ul>
 * <p>
 * So for example a receptor might watch level of some chemical the creature
 * has, and if that goes above or below a threshold it will kill the creature.
 */
public class CreaturesChem implements AttributeContainer {

    /**
     * The name of the chemical
     */
    private String name;

    /**
     * The id of the chemical
     */
    private String id;

    /**
     * The amount of this chemical currently in the creature's body
     */
    private double amount;

    // Half-Life Reference:
    // http://discoveralbia.com/2013/01/genetics-lesson-examining-a-creatures-half-life.html
    /**
     * The half-life of the chemical, as defined by the creature's genetics
     * Half-lives in C1 behave more like full-lives - the amount of time it
     * takes for a chemical to decay down to zero, as opposed to half
     */
    private double halfLife;

    public CreaturesChem(String name, String id, double amount, double halfLife) {
        this.name = name;
        this.id = id;
        this.amount = amount;
        this.halfLife = halfLife;
    }

    public String getName() {
        return (name);
    }

    public String getId() {
        return (id);
    }

    public double getAmount() {
        return (amount);
    }

    public void setAmount(double newAmount) {
        this.amount = newAmount;
    }

    public void incrementAmount(double increment) {
        this.amount += increment;
    }

    public void decay() {
        // TODO: Make this equation work more closely with how it works in
        // Creatures
        // System.out.println("\n" + name + " is decaying!");
        // System.out.println(name + " Before: " + amount);
        double newAmount = this.amount * (this.halfLife / 10);
        this.amount = Math.max(0, newAmount);
        // System.out.println(name + " After: " + amount);
    }

}
