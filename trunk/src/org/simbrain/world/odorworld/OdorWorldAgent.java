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

package org.simbrain.world.odorworld;
import java.awt.Point;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainMath;
import org.simbrain.world.Agent;
import org.simbrain.world.World;

/**
 * <b>Agent</b> represents in a creature in the world which can react to stimuli and move.  Agents are
 * controlled by neural networks, in particular their input and output nodes.
 */
public class OdorWorldAgent extends OdorWorldEntity implements Agent {
	
	private double whiskerAngle = Math.PI / 4; // angle in radians
	private double whiskerLength = 23;
	private double turnIncrement = 1;
	private double movementIncrement = 2;

	/** orientation of this object; used only by creature currently */
	private double orientation = 300;
	

	    
	public OdorWorldAgent() {}
	
	public OdorWorldAgent(OdorWorld wr, String nm, String the_type, int x, int y, double ori) {
	    super(wr, the_type, x, y);
	    super.setName(nm);
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
	 * Set the orienation of the creature
	 * 
	 * @param d the orientation, in degrees
	 */
	public void setOrientation(double d) {
		orientation = d;
		if(d <= 352.5 && d < 7.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_0.gif")); 
		} else if(d >= 7.5 && d < 22.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_15.gif")); 
		} else if(d >= 22.5 && d < 37.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_30.gif"));
		} else if(d >= 37.5 && d < 52.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_45.gif")); 
		} else if(d >= 52.5 && d < 67.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_60.gif"));
		} else if(d >= 67.5 && d < 82.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_75.gif"));
		} else if(d >= 82.5 && d < 97.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_90.gif"));
		} else if(d >= 97.5 && d < 112.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_105.gif"));
		} else if(d >= 112.5 && d < 127.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_120.gif"));
		} else if(d >= 127.5 && d < 142.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_135.gif"));
		} else if(d >= 142.5 && d < 157.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_150.gif"));
		} else if(d >= 157.5 && d < 172.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_165.gif"));
		} else if(d >= 172.5 && d < 187.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_180.gif"));
		} else if(d >= 187.5 && d < 202.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_195.gif"));
		} else if(d >= 202.5 && d < 217.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_210.gif"));
		} else if(d >= 217.5 && d < 232.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_225.gif"));
		} else if(d >= 232.5 && d < 247.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_240.gif"));
		} else if(d >= 247.5 && d < 262.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_255.gif"));
		} else if(d >= 262.5 && d < 277.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_270.gif"));
		} else if(d >= 277.5 && d < 292.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_285.gif"));
		} else if(d >= 292.5 && d < 307.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_300.gif"));
		} else if(d >= 307.5 && d < 322.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_315.gif"));
		} else if(d >= 322.5 && d < 337.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_330.gif"));
		} else if(d >= 337.5 && d < 352.5){
		    getTheImage().setImage(ResourceManager.getImage("Mouse_345.gif"));
		}
	}
	
	
	/**
	 * @return position of left whisker, given orientation of creature
	 */
	public Point getLeftWhisker() {
		double theta = getOrientationRad();
		int x = (int)(getLocation().x + whiskerLength * Math.cos(theta + whiskerAngle));
		int y = (int)(getLocation().y - whiskerLength * Math.sin(theta + whiskerAngle));
		return new Point(x, y);
	}
	/**
	 * @return position of right whisker, given orientation of creature
	 */
	public Point getRightWhisker() {
		double theta = getOrientationRad();
		int x = (int)(getLocation().x + whiskerLength * Math.cos(theta - whiskerAngle));
		int y = (int)(getLocation().y - whiskerLength * Math.sin(theta - whiskerAngle));
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
	
	public void goStraightForward(double value) {
		if (value == 0) {
			return;
		}
		double theta = getOrientationRad();
		value *= movementIncrement;
		Point p = new Point((int)(Math.round(getLocation().x + value * Math.cos(theta))),(int)(Math.round(getLocation().y - value * Math.sin(theta))));
		if(validMove(p)) {
			moveTo(0, p.x, p.y);
			wrapAround();
		}
	}

	public void goStraightBackward(double value) {
		if (value == 0) {
			return;
		}
		double theta = getOrientationRad();
		value *= movementIncrement;
		Point p = new Point((int)(Math.round(getLocation().x - value * Math.cos(theta))),(int)(Math.round(getLocation().y + value * Math.sin(theta))));
		if(validMove(p)) {
			moveTo(0, p.x, p.y);
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

		if ((parent.getUseLocalBounds()== true) && !parent.contains(possibleCreatureLocation)) {
				return false;
		}
		
		if (this.getParent().getObjectInhibitsMovement() == false){
		    return true;
		}
		
		//creature collision
		for (int i = 0; i < parent.getAbstractEntityList().size(); i++) {
			AbstractEntity temp = (AbstractEntity) parent.getAbstractEntityList().get(i);
			if (temp == this) continue;
			if (temp.getRectangle().intersects(getRectangle(possibleCreatureLocation))) {
				if(temp.getEdible()){
					temp.setBites(temp.getBites()+1);
					if(temp.getBites()>=temp.getBitesToDie()){
						temp.terminate();
					}
				}
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Implements a "video-game" world or torus, such that when an object leaves on side
	 * of the screen it reappears on the other.
	 */
	public void wrapAround() {
		
		if (parent.getUseLocalBounds()== true) { 
			return;
		}

		if (getLocation().x >= this.getParent().getWorldWidth())
			 getLocation().x -= this.getParent().getWorldWidth();
		if (getLocation().x < 0)
			 getLocation().x += this.getParent().getWorldWidth();
		if (getLocation().y >= this.getParent().getWorldHeight())
			 getLocation().y -= this.getParent().getWorldHeight();
		if (getLocation().y < 0)
			 getLocation().y += this.getParent().getWorldHeight();
	}
	
	
	/**
	 * Actiate a motor command on this agent
	 * 
	 * @param commandList the command itself
	 * @param value the activation level of the output neuron which produced this command
	 */
	public void setMotorCommand(String[] commandList, double value) {

		String name = commandList[0];
				
		if (name.equals("Forward")) {
			goStraightForward(value);
		} else if (name.equals("Backward")){
			goStraightBackward(value);
		} else if (name.equals("Left")) {
			turnLeft(value);
		} else if (name.equals("Right")) {
			turnRight(value);
		} else {
			absoluteMovement(name, value);
		}
		
		parent.repaint();
	}

	/**
	 * Move the agent in an absolute direction
	 * 
	 * @param name the name of the direction to move in
	 * @param value activation level of associated output node
	 */
	private void absoluteMovement(String name, double value) {

		Point creaturePosition = getLocation();
		int possiblePosition_x = getLocation().x;
		int possiblePosition_y = getLocation().y;

		int increment = (int)(movementIncrement * value);
		
		if (name.equals("North")) {
			possiblePosition_y = creaturePosition.y - increment;
		} else if (name.equals("South")) {
			possiblePosition_y = creaturePosition.y + increment;
		} else if (name.equals("West")) {
			possiblePosition_x = creaturePosition.x - increment;
		} else if (name.equals("East")) {
			possiblePosition_x = creaturePosition.x + increment;
		} else if (name.equals("North-west")) {
			possiblePosition_x = creaturePosition.x - increment;
			possiblePosition_y = creaturePosition.y - increment;
		} else if (name.equals("North-east")) {
			possiblePosition_x = creaturePosition.x + increment;
			possiblePosition_y = creaturePosition.y - increment;
		} else if (name.equals("South-west")) {
			possiblePosition_x = creaturePosition.x - increment;
			possiblePosition_y = creaturePosition.y + increment;
		} else if (name.equals("South-east")) {
			possiblePosition_x = creaturePosition.x + increment;
			possiblePosition_y = creaturePosition.y + increment;
		}
		
		Point possiblePosition = new Point(possiblePosition_x, possiblePosition_y);

		if (validMove(possiblePosition)) {
			setLocation(possiblePosition);
			wrapAround();
		}
	
	}

	//TODO: Do this operation for just the stim_id requested.  Make more efficient: Talk to Scott.
	
	/**
	 * Get the stimulus associated with the a given sensory id
	 */
	public double getStimulus(String[] sensor_id) {
		

		int max = this.getParent().getHighestDimensionalStimulus();
		double[] currentStimulus = SimbrainMath.zeroVector(max);
		AbstractEntity temp = null;
		double distance = 0;
		
		String sensorLocation = sensor_id[0];
		int sensor_index = Integer.parseInt(sensor_id[1]) - 1;
		
		//Sum proximal stimuli corresponding to each object
		if(sensorLocation.equals("Center")) {
			for (int i = 0; i < parent.getAbstractEntityList().size(); i++) {
				temp = (AbstractEntity) parent.getAbstractEntityList().get(i);
				distance = SimbrainMath.distance(temp.getLocation(), getLocation());  
				if ( temp == this) continue;
				currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
			}		
		} else if(sensorLocation.equals("Left")) {
			for (int i = 0; i < parent.getAbstractEntityList().size(); i++) {
				temp = (AbstractEntity) parent.getAbstractEntityList().get(i);
				distance = SimbrainMath.distance(temp.getLocation(), getLeftWhisker());  			
				if ( temp == this) continue;
				currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
			}		
		} else if(sensorLocation.equals("Right")) {
			for (int i = 0; i < parent.getAbstractEntityList().size(); i++) {
				temp = (AbstractEntity) parent.getAbstractEntityList().get(i);
				distance = SimbrainMath.distance(temp.getLocation(), getRightWhisker()); 
				if ( temp == this) continue;
				currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
			}		
		}
		
		return currentStimulus[sensor_index % max];
	
	}
	
	
	/**
	 * @return Returns the straight_factor.
	 */
	public double getMovementIncrement() {
		return movementIncrement;
	}
	/**
	 * @param straight_factor The straight_factor to set.
	 */
	public void setMovementIncrement(double straight_factor) {
		this.movementIncrement = straight_factor;
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
    /**
     * @return Returns the whiskerAngle.
     */
    public double getWhiskerAngle() {
        return whiskerAngle;
    }
    /**
     * @param whiskerAngle The whiskerAngle to set.
     */
    public void setWhiskerAngle(double whiskerAngle) {
        this.whiskerAngle = whiskerAngle;
    }
    /**
     * @return Returns the whiskerLength.
     */
    public double getWhiskerLength() {
        return whiskerLength;
    }
    /**
     * @param whiskerLength The whiskerLength to set.
     */
    public void setWhiskerLength(double whiskerLength) {
        this.whiskerLength = whiskerLength;
    }


	/**
	 * Returns parent world
	 */
	public World getParentWorld() {
		return this.getParent();
	}

}
