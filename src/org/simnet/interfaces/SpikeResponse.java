package org.simnet.interfaces;

public abstract class SpikeResponse {
    
    private boolean scaleByPSPDifference = false;
    private double psRestingPotential = 0;
    
    private static String[] typeList = {"test1", "test2"};
    
    public abstract SpikeResponse duplicate();
    public abstract void update();
    
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
}
