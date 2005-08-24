package org.simnet.interfaces;

import org.simnet.synapses.spikeresponders.*;

public abstract class SpikeResponder {
    
	protected double value = 0;
	protected boolean scaleByPSPDifference = false;
    protected double psRestingPotential = 0;
    
    protected Synapse parent;
    
    private static String[] typeList = {Step.getName(), JumpAndDecay.getName(), RiseAndDecay.getName()};
    
    public abstract SpikeResponder duplicate();
    public abstract void update();
    
    public SpikeResponder duplicate(SpikeResponder s){
        s.setScaleByPSPDifference(getScaleByPSPDifference());
        s.setPsRestingPotential(getPsRestingPotential());
        return s;
    }
    
	/**
	 * @return the name of the class of this synapse
	 */
	public String getType() {
		return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
	}
	
    /**
     * @return Returns the typeList.
     */
    public static String[] getTypeList() {
        return typeList;
    }
    /**
     * @param typeList The typeList to set.
     */
    public static void setTypeList(String[] typeList) {
        SpikeResponder.typeList = typeList;
    }
	/**
	 * @return Returns the psRestingPotential.
	 */
	public double getPsRestingPotential() {
		return psRestingPotential;
	}
	/**
	 * @param psRestingPotential The psRestingPotential to set.
	 */
	public void setPsRestingPotential(double psRestingPotential) {
		this.psRestingPotential = psRestingPotential;
	}
	/**
	 * @return Returns the scaleByPSPDifference.
	 */
	public boolean getScaleByPSPDifference() {
		return scaleByPSPDifference;
	}
	/**
	 * @param scaleByPSPDifference The scaleByPSPDifference to set.
	 */
	public void setScaleByPSPDifference(boolean scaleByPSPDifference) {
		this.scaleByPSPDifference = scaleByPSPDifference;
	}
	
	/**
	 * Helper function for combo boxes.  Associates strings with indices.
	 */	
	public static int getSpikerTypeIndex(String type) {
		for (int i = 0; i < typeList.length; i++) {
			if (type.equals(typeList[i])) {
				return i;
			}
		}
		return 0;
	}
	/**
	 * @return Returns the value.
	 */
	public double getValue() {
		if (this.getScaleByPSPDifference() == true) {
			return value * (getPsRestingPotential() - parent.getTarget().getActivation());
		}
		return value;
	}
	/**
	 * @param value The value to set.
	 */
	public void setValue(double value) {
		this.value = value;
	}
	/**
	 * @return Returns the parent.
	 */
	public Synapse getParent() {
		return parent;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(Synapse parent) {
		this.parent = parent;
	}
}
