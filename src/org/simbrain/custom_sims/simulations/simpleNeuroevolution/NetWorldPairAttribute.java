package org.simbrain.custom_sims.simulations.simpleNeuroevolution;

import java.util.Comparator;

public class NetWorldPairAttribute {
	int netID;
	int netIndex;
	double fitnessScore;
	int windowLocationX;
	int windowLocationY;
	int width;
	int height;
	
	public NetWorldPairAttribute(int netID, int netIndex, int x, int y, int w, int h) { 
		this.netID = netID;
		this.netIndex = netIndex;
		this.fitnessScore = 0;
		setWindowLocationX(x);
		setWindowLocationY(y);
		setWidth(w);
		setHeight(h);
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
	
	public void setWindowLocationX(int x) {
		this.windowLocationX = x;
	}
	
	public void setWindowLocationY(int y) {
		this.windowLocationY = y;
	}
	
	public void setWidth(int w) {
		this.width = w;
	}
	
	public void setHeight(int h) {
		this.height = h;
	}
	
	public int getWindowLocationX() {
		return this.windowLocationX;
	}
	
	public int getWindowLocationY() {
		return this.windowLocationY;
	}
	
	public int getWindowWidth() {
		return this.width;
	}
	
	public int getWindowHeight() {
		return this.height;
	}
}

class FitnessComparator implements Comparator<NetWorldPairAttribute> {
    public int compare(NetWorldPairAttribute na1, NetWorldPairAttribute na2) {
        if(na1.getFitnessScore() > na2.getFitnessScore()) return 1;
        if(na1.getFitnessScore() < na2.getFitnessScore()) return -1;
        return 0;
    }
}
