/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.simbrain.world;

import java.awt.Point;

import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainMath;
import org.simbrain.util.Utils;

public class Agent extends WorldEntity {


	/** Directions of absolute movement */
	public static final int SOUTH_WEST = 1;
	public static final int SOUTH = 2;
	public static final int SOUTH_EAST = 3;
	public static final int EAST = 4;
	public static final int WEST = 6;
	public static final int NORTH_WEST = 7;
	public static final int NORTH = 8;
	public static final int NORTH_EAST = 9;
	
	private double[] currentMotor = SimbrainMath.zeroVector(8);
	private double[] currentStimulus = SimbrainMath.zeroVector(8);
	private double[] currentStimulusL = SimbrainMath.zeroVector(8);
	private double[] currentStimulusR = SimbrainMath.zeroVector(8);
	
	private double whisker_angle = Math.PI / 4; // angle in radians
	private double whisker_length = 23;
	private double turnIncrement = 1;
	private double straightMovementIncrement = 2;
	private int absoluteMovementIncrement = 5;  // for absolute movements

	/** orientation of this object; used only by creature currently */
	private double orientation = 300;
    
	public Agent() {}
	
	public Agent(World wr, String the_type, int x, int y, double ori) {
	    super(wr, the_type, x, y);
	    setOrientation(ori);
	}
		
	/**
	 * @return orientation in degrees
	 */
	public double getOrientation() {
		return orientation;
	}
	
	/**
	 * @return orientation in degrees
	 */
	public double getOrientationRad() {
		return orientation * Math.PI / 180;
	}
	
	/**
	 * @param d
	 */
	public void setOrientation(double d) {
		orientation = d;
		if(d <= 352.5 && d < 7.5){
		    this.setImage(ResourceManager.getImage("Mouse_0.gif")); 
		} else if(d >= 7.5 && d < 22.5){
		    this.setImage(ResourceManager.getImage("Mouse_15.gif")); 
		} else if(d >= 22.5 && d < 37.5){
		    this.setImage(ResourceManager.getImage("Mouse_30.gif"));
		} else if(d >= 37.5 && d < 52.5){
		    this.setImage(ResourceManager.getImage("Mouse_45.gif")); 
		} else if(d >= 52.5 && d < 67.5){
		    this.setImage(ResourceManager.getImage("Mouse_60.gif"));
		} else if(d >= 67.5 && d < 82.5){
		    this.setImage(ResourceManager.getImage("Mouse_75.gif"));
		} else if(d >= 82.5 && d < 97.5){
		    this.setImage(ResourceManager.getImage("Mouse_90.gif"));
		} else if(d >= 97.5 && d < 112.5){
		    this.setImage(ResourceManager.getImage("Mouse_105.gif"));
		} else if(d >= 112.5 && d < 127.5){
		    this.setImage(ResourceManager.getImage("Mouse_120.gif"));
		} else if(d >= 127.5 && d < 142.5){
		    this.setImage(ResourceManager.getImage("Mouse_135.gif"));
		} else if(d >= 142.5 && d < 157.5){
		    this.setImage(ResourceManager.getImage("Mouse_150.gif"));
		} else if(d >= 157.5 && d < 172.5){
		    this.setImage(ResourceManager.getImage("Mouse_165.gif"));
		} else if(d >= 172.5 && d < 187.5){
		    this.setImage(ResourceManager.getImage("Mouse_180.gif"));
		} else if(d >= 187.5 && d < 202.5){
		    this.setImage(ResourceManager.getImage("Mouse_195.gif"));
		} else if(d >= 202.5 && d < 217.5){
		    this.setImage(ResourceManager.getImage("Mouse_210.gif"));
		} else if(d >= 217.5 && d < 232.5){
		    this.setImage(ResourceManager.getImage("Mouse_225.gif"));
		} else if(d >= 232.5 && d < 247.5){
		    this.setImage(ResourceManager.getImage("Mouse_240.gif"));
		} else if(d >= 247.5 && d < 262.5){
		    this.setImage(ResourceManager.getImage("Mouse_255.gif"));
		} else if(d >= 262.5 && d < 277.5){
		    this.setImage(ResourceManager.getImage("Mouse_270.gif"));
		} else if(d >= 277.5 && d < 292.5){
		    this.setImage(ResourceManager.getImage("Mouse_285.gif"));
		} else if(d >= 292.5 && d < 307.5){
		    this.setImage(ResourceManager.getImage("Mouse_300.gif"));
		} else if(d >= 307.5 && d < 322.5){
		    this.setImage(ResourceManager.getImage("Mouse_315.gif"));
		} else if(d >= 322.5 && d < 337.5){
		    this.setImage(ResourceManager.getImage("Mouse_330.gif"));
		} else if(d >= 337.5 && d < 352.5){
		    this.setImage(ResourceManager.getImage("Mouse_345.gif"));
		}
	}
	
	
	
	
	/**
	 * @return position of left whisker, given orientation of creature
	 */
	public Point getLeftWhisker() {
		double theta = getOrientationRad();
		int x = (int)(getLocation().x + whisker_length * Math.cos(theta + whisker_angle));
		int y = (int)(getLocation().y - whisker_length * Math.sin(theta + whisker_angle));
		return new Point(x, y);
	}
	/**
	 * @return position of right whisker, given orientation of creature
	 */
	public Point getRightWhisker() {
		double theta = getOrientationRad();
		int x = (int)(getLocation().x + whisker_length * Math.cos(theta - whisker_angle));
		int y = (int)(getLocation().y - whisker_length * Math.sin(theta - whisker_angle));
		return new Point(x, y);
	}
	
	public void turnRight(double value) {
		value = computeAngle(getOrientation() - (value * turnIncrement));
		setOrientation(value);
		//System.out.println("Orientation = " + getOrientation());
	}
	public void turnLeft(double value) {
		value = computeAngle(getOrientation() + (value * turnIncrement));
		setOrientation(value);
		//System.out.println("Orientation = " + getOrientation());
	}
	
	/**
	 * Ensures that val lies between 0 and 360
	 * 
	 */
	private double computeAngle(double val) {
		while (val >= 360) val-=360;
		while (val < 0) val+=360;
		return val;
	}
	
	public void goStraight(double value) {
		double theta = getOrientationRad();
		value *= straightMovementIncrement;
		Point p = new Point((int)(Math.round(getLocation().x + value * Math.cos(theta))),(int)(Math.round(getLocation().y - value * Math.sin(theta))));		
		if(validMove(p)) {
			moveTo(0, p.x, p.y);
			wrapAround();
		}
	}
	

	/**
	 * Contains the main code for moving the creature in a specified direction.  
	 * 
	 * @param direction integer reprsentation of one of 8 directions to move the creature in
	 */
	public void moveDirection(int direction) {


		Point creaturePosition = getLocation();
		int possiblePosition_x = getLocation().x;
		int possiblePosition_y = getLocation().y;

		switch (direction) {
			case SOUTH_WEST :
				possiblePosition_x = creaturePosition.x - absoluteMovementIncrement;
				possiblePosition_y = creaturePosition.y + absoluteMovementIncrement;
				//           currentOutput = "Southwest";
				break;
			case SOUTH :
				possiblePosition_y = creaturePosition.y + absoluteMovementIncrement;
				//          currentOutput = "South";
				break;
			case SOUTH_EAST :
				possiblePosition_x = creaturePosition.x + absoluteMovementIncrement;
				possiblePosition_y = creaturePosition.y + absoluteMovementIncrement;
				//          currentOutput = "Southeast";
				break;
			case WEST :
				possiblePosition_x = creaturePosition.x - absoluteMovementIncrement;
				//          currentOutput = "West";
				break;
			case 5 :
				break;
			case EAST :
				possiblePosition_x = creaturePosition.x + absoluteMovementIncrement;
				//          currentOutput = "East";
				break;
			case NORTH_WEST :
				possiblePosition_x = creaturePosition.x - absoluteMovementIncrement;
				possiblePosition_y = creaturePosition.y - absoluteMovementIncrement;
				//          currentOutput = "Northwest";
				break;
			case NORTH :
				possiblePosition_y = creaturePosition.y - absoluteMovementIncrement;
				//          currentOutput = "North";
				break;
			case NORTH_EAST :
				possiblePosition_x = creaturePosition.x + absoluteMovementIncrement;
				possiblePosition_y = creaturePosition.y - absoluteMovementIncrement;
				//          currentOutput = "Northeast";
				break;
			default :
				//     	currentOutput = "None";
			break;
			
	
		}

		Point possiblePosition = new Point(possiblePosition_x, possiblePosition_y);

		if (validMove(possiblePosition)) {
			setLocation(possiblePosition);
			wrapAround();
		}

	}

	/**
	 * Check to see if the creature can move to a given new location.  If it is
	 * off screen or on top of a creature, disallow the move.
	 * 
	 * @param possibleCreatureLocation on-screen location to be checked
	 * @return true if the move is valid, false otherwise
	 */
	protected boolean validMove(Point possibleCreatureLocation) {

		if (possibleCreatureLocation.x > World.WORLD_WIDTH
			|| possibleCreatureLocation.x < 0
			|| possibleCreatureLocation.y > World.WORLD_HEIGHT
			|| possibleCreatureLocation.y < 0) {
			if (parent.getLocalBounds()== true) { // only restrict boundaries if bounds is on
				return false;
			}
		}
		
		//creature collision
		for (int i = 0; i < parent.getObjectList().size(); i++) {
			WorldEntity temp = (WorldEntity) parent.getObjectList().get(i);
			if (temp == this) continue;
			int distance = SimbrainMath.distance(possibleCreatureLocation, temp.getLocation());
			if (distance < World.OBJECT_SIZE) {
				return false;
			}
		}
		return true;
	}
	
	public void wrapAround() {
		
		if (getLocation().x >= World.WORLD_WIDTH)
			 getLocation().x -= World.WORLD_WIDTH;
		if (getLocation().x < 0)
			 getLocation().x += World.WORLD_WIDTH;
		if (getLocation().y >= World.WORLD_WIDTH)
			 getLocation().y -= World.WORLD_WIDTH;
		if (getLocation().y < 0)
			 getLocation().y += World.WORLD_WIDTH;
	}
	
	
	//////////////////////
	// Update methods   //
	//////////////////////

	/**
	 *  Update the world (currently, just the creature), based on the motor
	 *  vector sent from the network.  How output vectors (sets of activation levels
	 *  at the output nodes of the network) are mapped to movements varies, and
	 *  can be set in the WorldDialog.
	 * 
	 * @param fromNet the output vector from the neural network
	 */
	public void update(double[] fromNet) {
		//System.out.println(" " + Utils.getVectorString(fromNet));

		//Move in the directions corresponding to nodes whose value is greater than the average value across
		//the output nodes
		double avg = SimbrainMath.getAverage(currentMotor);
		for (int i = 0; i < currentMotor.length; i++) {
			if (((int) currentMotor[i]) > avg) {
				// Each node is a direction.  
				moveDirection(i);
			}
		}
		this.getParent().repaint();
	}


	/**
	 * Movement initiated by network, as opposed to by clicking the mouse
	 * 
	 * @param netOutput a single-value version of update, representing the most active output node
	 */
	public void moveCreatureNetwork(int netOutput) {

		moveDirection(netOutput);
		this.getParent().repaint();

	}
	
	//////////////////////////////////////////
	// "Motor methods"						//
	//										//
	// Network output --> Creature Movement //
	//////////////////////////////////////////

	public void motorCommand(String name, double value) {

		// Must implement an actual rule  for dealing with intensity here!
		if (value < 1) {
			return;
		}

		if (name.equals("North")) {
			moveDirection(Agent.NORTH);
		} else if (name.equals("South")) {
			moveDirection(Agent.SOUTH);
		} else if (name.equals("West")) {
			moveDirection(Agent.WEST);
		} else if (name.equals("East")) {
			moveDirection(Agent.EAST);
		} else if (name.equals("North-west")) {
			moveDirection(Agent.NORTH_WEST);
		} else if (name.equals("North-east")) {
			moveDirection(Agent.NORTH_EAST);
		} else if (name.equals("South-west")) {
			moveDirection(Agent.SOUTH_WEST);
		} else if (name.equals("South-east")) {
			moveDirection(Agent.SOUTH_EAST);
		} else if (name.equals("Straight")) {
			goStraight(value);
		} else if (name.equals("Left")) {
			turnLeft(value);
		} else if (name.equals("Right")) {
			turnRight(value);
		}
		
		parent.repaint();
	}
		
	/**
	 * Updates the proximal stimulus to be sent to the network
	 */
	public void updateStimulus() {
		
		WorldEntity temp = null;

		currentStimulus = SimbrainMath.zeroVector(getHighestDimensionalStimulus());
		currentStimulusL = SimbrainMath.zeroVector(getHighestDimensionalStimulus());
		currentStimulusR = SimbrainMath.zeroVector(getHighestDimensionalStimulus());

		double distance = 0;
		
		//Sum proximal stimuli corresponding to each object
		for (int i = 0; i < parent.getObjectList().size(); i++) {
				temp = (WorldEntity) parent.getObjectList().get(i);
				if ( temp == this) continue;
				distance = SimbrainMath.distance(temp.getLocation(), getLocation());  
				currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulusObject().getStimulus(distance));
				
				distance = SimbrainMath.distance(temp.getLocation(), getLeftWhisker()); 
				currentStimulusL = SimbrainMath.addVector(currentStimulusL, temp.getStimulusObject().getStimulus(distance));
				
				distance = SimbrainMath.distance(temp.getLocation(), getRightWhisker());  
				currentStimulusR = SimbrainMath.addVector(currentStimulusR, temp.getStimulusObject().getStimulus(distance));
		}		
		
	}
	

	public double getStimulus(String in_label) {
		
		int max = this.getHighestDimensionalStimulus();
		
		if (in_label.startsWith("L")) {
			return currentStimulusL[(Integer.parseInt(in_label.substring(1))-1) % max];
		} else if (in_label.startsWith("R")) {
			return currentStimulusR[(Integer.parseInt(in_label.substring(1))-1) % max];
		} else {
			return currentStimulus[(Integer.parseInt(in_label)-1) % max];
		}
	
	}
	
	/**
	 * Go through entities in this world and find the one with the greatest number of dimensions.
	 * This will determine the dimensionality of the proximal stimulus sent to the network
	 * 
	 * @return the number of dimensions in the highest dimensional stimulus
	 */
	public int getHighestDimensionalStimulus() {
		Stimulus temp = null;
		int max = 0;
		for (int i = 0; i < parent.getObjectList().size(); i++) {
				temp = ((WorldEntity) parent.getObjectList().get(i)).getStimulusObject();
				if(temp.getStimulusDimension() > max) max = temp.getStimulusDimension();
		}
		return max;
	}

	/**
	 * Calculate the stimulus to send to the neural network based on the locations
	 * and smell signatures of surrounding objects
	 * 
	 * @return an array of values to serve as input to the neural net.
	 */
	public double[] getStimulus() {
		return currentStimulus;
	}
	
	public int getAbsoluteMovementIncrement() {
		return absoluteMovementIncrement;
	}
	public  void setAbsoluteMovementIncrement(int mi) {
		absoluteMovementIncrement = mi;
	}


	
	/**
	 * @return Returns the straight_factor.
	 */
	public double getStraightMovementIncrement() {
		return straightMovementIncrement;
	}
	/**
	 * @param straight_factor The straight_factor to set.
	 */
	public void setStraightMovementIncrement(double straight_factor) {
		this.straightMovementIncrement = straight_factor;
	}
	/**
	 * @return Returns the turn_factor.
	 */
	public double getTurnIncrement() {
		return turnIncrement;
	}
	/**
	 * @param turn_factor The turn_factor to set.
	 */
	public void setTurnIncrement(double turn_factor) {
		this.turnIncrement = turn_factor;
	}
}
