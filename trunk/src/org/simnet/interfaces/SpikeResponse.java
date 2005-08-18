package org.simnet.interfaces;

public abstract class SpikeResponse {
    
    private boolean scaleByPSPDifference = false;
    private double psRestingPotential = 0;
    
    private static String[] typeList = null;
    
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
}
