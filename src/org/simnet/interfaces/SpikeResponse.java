package org.simnet.interfaces;

import org.simnet.synapses.spikeresponders.*;

public abstract class SpikeResponse {
    
    private boolean scaleByPSPDifference = false;
    private double psRestingPotential = 0;
    private Synapse parent;
    
    private static String[] typeList = {Step.getName(), JumpAndDecay.getName(), RiseAndDecay.getName()};
    
    public abstract SpikeResponse duplicate();
    public abstract void update();
    
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
        SpikeResponse.typeList = typeList;
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
	public boolean isScaleByPSPDifference() {
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
}
