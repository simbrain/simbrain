package org.simbrain.custom_sims.simulations.simpleNeuroevolution;

import java.util.Comparator;

public class NetworkAttribute {
	int netID;
	int netIndex;
	double fitnessScore;
	
	public NetworkAttribute(int netID, int netIndex) { 
		this.netID = netID;
		this.netIndex = netIndex;
		this.fitnessScore = 0;
	}
	
	public int getNetID() {
		return this.netID;
	}
	
	public int getNetIndex() {
		return netIndex;
	}
	
	public void setNetIndex(int netIndex) {
		this.netIndex = netIndex;
	}
	
	public double getFitnessScore() {
		return fitnessScore;
	}
	
	public void addFitness() {
		fitnessScore += 1;
	}
	
	public void addFitness(double delta) {
		if(delta > 0) {
			fitnessScore += delta;
		}
	}
	
}

class FitnessComparator implements Comparator<NetworkAttribute> {
    public int compare(NetworkAttribute na1, NetworkAttribute na2) {
        if(na1.getFitnessScore() > na2.getFitnessScore()) return 1;
        if(na1.getFitnessScore() < na2.getFitnessScore()) return -1;
        return 0;
    }
}
