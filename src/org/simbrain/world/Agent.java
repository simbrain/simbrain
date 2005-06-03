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

//TODO: Agent will have to become an interface, with "getType", "getWorldType", 

import java.awt.Point;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainMath;
import org.simbrain.util.Utils;
import org.simbrain.coupling.*;

public class Agent extends WorldEntity {
	
	private double whiskerAngle = Math.PI / 4; // angle in radians
	private double whiskerLength = 23;
	private double turnIncrement = 1;
	private double movementIncrement = 2;

	/** orientation of this object; used only by creature currently */
	private double orientation = 300;
	
	//TODO: Remove defaultName
	private String name = "Mouse 1";
	
    
	public Agent() {}
	
	public Agent(World wr, String nm, String the_type, int x, int y, double ori) {
	    super(wr, the_type, x, y);
	    this.name = nm;
	    setOrientation(ori);
	}

	/**
	 * Returns a menu with the sesnors this agent is equipped with
	 * 
	 * @param al the action listener (currently in the network panel) which listens to these menu events
	 * @return a JMenu with a list of sensors on this agent
	 */
	public JMenu getSensorIdMenu(ActionListener al) {

		
		JMenu ret = new JMenu("" + this.getName());
		
		JMenu centerMenu = new JMenu("Center");
		for(int i = 0; i < 9; i++) {
			CouplingMenuItem stimItem  = new CouplingMenuItem("" + i,new SensoryCoupling(this, new String[] {"Center", "" + i}));
			stimItem.addActionListener(al);
			centerMenu.add(stimItem);				
		}
		ret.add(centerMenu);
		
		JMenu leftMenu = new JMenu("Left");
		for(int i = 0; i < 9; i++) {
			CouplingMenuItem stimItem  = new CouplingMenuItem("" + i,new SensoryCoupling(this, new String[] {"Left", "" + i}));
			stimItem.addActionListener(al);
			leftMenu.add(stimItem);				
		}
		ret.add(leftMenu);
		
		JMenu rightMenu = new JMenu("Right");
		for(int i = 0; i < 9; i++) {
			CouplingMenuItem stimItem  = new CouplingMenuItem("" + i,new SensoryCoupling(this, new String[] {"Right", "" + i}));
			stimItem.addActionListener(al);
			rightMenu.add(stimItem);				
		}
		ret.add(rightMenu);	
			
		return ret;
		
	}
	
	/**
	 * Returns a menu with the motor commands available to this agent
	 * 
	 * @param al the action listener (currently in the network panel) which listens to these menu events
	 * @return a JMenu with the motor commands available for this agent
	 */
	public JMenu getMotorCommandMenu(ActionListener al) {

		JMenu ret = new JMenu("" + this.getName());
		
		CouplingMenuItem motorItem  = new CouplingMenuItem("Straight",new MotorCoupling(this, new String[] {"Straight"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				
		
	    motorItem  = new CouplingMenuItem("Right",new MotorCoupling(this, new String[] {"Right"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("Left",new MotorCoupling(this, new String[] {"Left"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("North",new MotorCoupling(this, new String[] {"North"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("West",new MotorCoupling(this, new String[] {"West"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("East",new MotorCoupling(this, new String[] {"East"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("North-east",new MotorCoupling(this, new String[] {"North-east"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("North-west",new MotorCoupling(this, new String[] {"North-west"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("South-east",new MotorCoupling(this, new String[] {"South-east"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

	    motorItem  = new CouplingMenuItem("South-west",new MotorCoupling(this, new String[] {"South-west"}));
		motorItem.addActionListener(al);
		ret.add(motorItem);				

		return ret;
		
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
	
	public void goStraight(double value) {
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
		for (int i = 0; i < parent.getEntityList().size(); i++) {
			WorldEntity temp = (WorldEntity) parent.getEntityList().get(i);
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
	
		
	//////////////////////////////////////////
	// "Motor methods"						//
	//										//
	// Network output --> Creature Movement //
	//////////////////////////////////////////

	//TODO: Change name?
	public void motorCommand(String[] commandList, double value) {

		String name = commandList[0];
				
		if (name.equals("Straight")) {
			goStraight(value);
		} else if (name.equals("Left")) {
			turnLeft(value);
		} else if (name.equals("Right")) {
			turnRight(value);
		} else {
			absoluteMovement(name, value);
		}
		
		parent.repaint();
	}

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
	public double getStimulus(String[] sensor_id) {
		
		int max = this.getHighestDimensionalStimulus();
		double[] currentStimulus = SimbrainMath.zeroVector(max);
		WorldEntity temp = null;
		double distance = 0;
		
		String sensorLocation = sensor_id[0];
		int sensor_index = Integer.parseInt(sensor_id[1]);
		
		//Sum proximal stimuli corresponding to each object
		if(sensorLocation.equals("Center")) {
			for (int i = 0; i < parent.getEntityList().size(); i++) {
				temp = (WorldEntity) parent.getEntityList().get(i);
				distance = SimbrainMath.distance(temp.getLocation(), getLocation());  
				if ( temp == this) continue;
				currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
			}		
		} else if(sensorLocation.equals("Left")) {
			for (int i = 0; i < parent.getEntityList().size(); i++) {
				temp = (WorldEntity) parent.getEntityList().get(i);
				distance = SimbrainMath.distance(temp.getLocation(), getLeftWhisker());  			
				if ( temp == this) continue;
				currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
			}		
		} else if(sensorLocation.equals("Right")) {
			for (int i = 0; i < parent.getEntityList().size(); i++) {
				temp = (WorldEntity) parent.getEntityList().get(i);
				distance = SimbrainMath.distance(temp.getLocation(), getRightWhisker()); 
				if ( temp == this) continue;
				currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
			}		
		}

		return currentStimulus[sensor_index % max];
	
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
		for (int i = 0; i < parent.getEntityList().size(); i++) {
				temp = ((WorldEntity) parent.getEntityList().get(i)).getStimulus();
				if(temp.getStimulusDimension() > max) max = temp.getStimulusDimension();
		}
		return max;
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
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

}
