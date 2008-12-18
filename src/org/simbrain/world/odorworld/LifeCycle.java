package org.simbrain.world.odorworld;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Manage the lifecycle of an entity that can come into and go out of existence.
 */
public class LifeCycle {
	
	/** Whether it's dead or not. */
	private boolean isDead = false;

    /** Number of bites to heat edible item. */
    private int bitesToDie = 200;

    /** Number of bites on stimulus. */
    private int bites = 0;

    /** Likelihood eaten item will return. */
    private double resurrectionProb = .01;
    
	/** Parent entity. */
	OdorWorldEntity parent;
	
	/**
	 * @param entity
	 */
	public LifeCycle(OdorWorldEntity entity) {
		this.parent = entity;		
	}

    /**
     * @return Bites to eat item.
     */
    public int getBitesToDie() {
        return bitesToDie;
    }

    /**
     * @param bitesToDie Bites to eat an item.
     */
    public void setBitesToDie(final int bitesToDie) {
        this.bitesToDie = bitesToDie;
    }

    /**
     * @return Likelihood entity will reappear.
     */
    public double getResurrectionProb() {
        return resurrectionProb;
    }

    /**
     * @param resurrectionProb Likelihood entity will reappear.
     */
    public void setResurrectionProb(final double resurrectionProb) {
        this.resurrectionProb = resurrectionProb;
    }

    /**
     * @return Number of bites.
     */
    public int getBites() {
        return bites;
    }

    /**
     * Number of times bitten.
     * @param bites Number of bites.
     */
    public void setBites(final int bites) {
        this.bites = bites;
    }

	/**
	 * @return the parent
	 */
	public OdorWorldEntity getParent() {
		return parent;
	}

	/**
	 * Take one step closer to death.
	 */
	public void bite() {
		bites++;	
	}
	
	/**
	 * @return the isDead
	 */
	public boolean isDead() {
		return isDead;
	}

	/**
	 * @param isDead the isDead to set
	 */
	public void setDead(boolean isDead) {
		this.isDead = isDead;
	}

	/**
	 * Life and death.
	 */
	public void update() {
		if (isDead) {
			if (Math.random() < this.getResurrectionProb()) {
				isDead = false; // Resurrect
				bites = 0;
			}
		} else {
			if (this.getBites() > this.getBitesToDie()) {
				isDead = true; // Terminate
			}
		}
	}

}
