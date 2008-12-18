/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.world.odorworld.entities;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.environment.Agent;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.environment.TwoDEnvironment;
import org.simbrain.util.environment.effectors.*;
import org.simbrain.util.environment.sensors.*;
import org.simbrain.world.odorworld.LifeCycle;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * <b>MovingEntity</b> represents a sprite in an OdorWorld which can move.
 * 
 * TODO: Add animations?
 */
public class MovingEntity implements OdorWorldEntity {

	/** Images for various angles. */
	final TreeMap<Double, Image> images = new TreeMap<Double, Image>();
	
    /** Icon renderer. */
    private ImageIcon currentImage = new ImageIcon();
    
	/** Location of entity. */
	double[] actualLocation;

	/** Reference to 2d Agent. */
	private Agent agent = null;
	
	/** Default location for sensors relative to agent. */
	private static double WHISKER_ANGLE = Math.PI / 4;

	/** Amount to manually rotate. */
    public final double manualMotionTurnIncrement = 4;

	/** Amount to manually rotate. */
    public final double manualStraightMovementIncrement = 4;
    
    /** Parent. */
    private OdorWorld parent;
    
    /** Smell source, if any. */
    private SmellSource smellSource;

    /** Whether this entity inhibits movement. */
	private boolean inhibitsMovement;
    	
    /**
     * Creates an instance of an agent.
     *
     * @param wr Odor world to place agent
     * @param nm Name of agent
     * @param x Position
     * @param y Position
     */
    public MovingEntity(OdorWorld parent, final String name, double[] location) {
    	agent = new Agent(parent, name, location); // TODO: For now this is all automatic but make it settable
    	actualLocation = location;
    	this.parent = parent;
        currentImage.setImage(ResourceManager.getImage("Mouse.gif")); // TODO
        initEffectorsAndSensors();
        initializeImages();
    }
    
    /**
     * Initialize effectors and sensors, thereby allowing other Simbrain components to couple to the agent.
     */
    public void initEffectorsAndSensors() {
    	agent.getEffectors().add(new StraightMovementEffector(agent, "Straight", 1, 1));
    	agent.getEffectors().add(new RotationEffector(agent, "Left", 1, 1));
    	agent.getEffectors().add(new RotationEffector(agent, "Right", 1, -1));
    	agent.getSensors().add(new SmellSensor(agent, "Left", 1, WHISKER_ANGLE));
    	agent.getSensors().add(new SmellSensor(agent, "Center", 1));
    	agent.getSensors().add(new SmellSensor(agent, "Right", 1, -WHISKER_ANGLE));
    	agent.getSensors().add(new BumpSensor(agent, "Bump", 1));
    }
    
    /**
     * Currently set to a specific image!
     */
    public void initializeImages () {
        images.put(7.5, ResourceManager.getImage("Mouse_0.gif"));
        images.put(22.5, ResourceManager.getImage("Mouse_15.gif"));
        images.put(37.5, ResourceManager.getImage("Mouse_30.gif"));
        images.put(52.5, ResourceManager.getImage("Mouse_45.gif"));
        images.put(67.5, ResourceManager.getImage("Mouse_60.gif"));
        images.put(82.5, ResourceManager.getImage("Mouse_75.gif"));
        images.put(97.5, ResourceManager.getImage("Mouse_90.gif"));
        images.put(112.5, ResourceManager.getImage("Mouse_105.gif"));
        images.put(127.5, ResourceManager.getImage("Mouse_120.gif"));
        images.put(142.5, ResourceManager.getImage("Mouse_135.gif"));
        images.put(157.5, ResourceManager.getImage("Mouse_150.gif"));
        images.put(172.5, ResourceManager.getImage("Mouse_165.gif"));
        images.put(187.5, ResourceManager.getImage("Mouse_180.gif"));
        images.put(202.5, ResourceManager.getImage("Mouse_195.gif"));
        images.put(217.5, ResourceManager.getImage("Mouse_210.gif"));
        images.put(232.5, ResourceManager.getImage("Mouse_225.gif"));
        images.put(247.5, ResourceManager.getImage("Mouse_240.gif"));
        images.put(262.5, ResourceManager.getImage("Mouse_255.gif"));
        images.put(277.5, ResourceManager.getImage("Mouse_270.gif"));
        images.put(292.5, ResourceManager.getImage("Mouse_285.gif"));
        images.put(307.5, ResourceManager.getImage("Mouse_300.gif"));
        images.put(322.5, ResourceManager.getImage("Mouse_315.gif"));
        images.put(337.5, ResourceManager.getImage("Mouse_330.gif"));
        images.put(352.5, ResourceManager.getImage("Mouse_345.gif"));
    }
    
    /**
     * Set the orientation of the creature.
     *
     * @param d the orientation, in degrees
     */
    public void setOrientation(final double d) {
        SortedMap<Double, Image> headMap = images.headMap(d);
        Image image = headMap.size() > 0 ? 
          images.get(headMap.lastKey()) : images.get(images.firstKey());
         currentImage.setImage(image); 
    }

    //TODO
    public MovingEntity copy() {
//        OdorWorldAgent temp = new OdorWorldAgent();
//        temp.setImageName(getImageName());
//        temp.setMovementIncrement(getMovementIncrement());
//        temp.setName("Copy of " + getName());
//        temp.setOrientation(getOrientation());
//        temp.setStimulus(getStimulus());
//        temp.setImage(getImage().getImage());
//        temp.setTurnIncrement(getTurnIncrement());
        return null;
    }

	/**
	 * Update the agent's sensors
	 */
	public void update() {
		agent.update();
		this.setOrientation(agent.getHeading());
		if (this.getParent().validMove(this, doubleToPoint(this.getSuggestedLocation()))) {
			actualLocation = this.getSuggestedLocation();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle getBounds() {
		return new Rectangle((int) actualLocation[0], (int) actualLocation[1], currentImage.getIconWidth(), currentImage.getIconHeight());
	}

	/**
	 * {@inheritDoc}
	 */
	public OdorWorld getParent() {
		return parent;
	}

	/**
	 * {@inheritDoc}
	 */
	public TwoDEnvironment getEnvironment() {
		return getParent();
	}

	/**
	 * {@inheritDoc}
	 */
	public double[] getSuggestedLocation() {
		return agent.getSuggestedLocation();
	}

	/**
	 * Helper method.
	 */
	private Point doubleToPoint(double[] convert) {
		Point point = new Point();
		point.setLocation(convert[0], convert[1]);
		return point;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSuggestedLocation(double[] location) {
		agent.setSuggestedLocation(location);			
	}

	/**
	 * @return the agent
	 */
	public Agent getAgent() {
		return agent;
	}

	/**
	 * {@inheritDoc}
	 */
	public void paintEntity(Component component, Graphics g) {
		currentImage.paintIcon(component, g, (int) actualLocation[0], (int) actualLocation[1]);		
	}

	/**
	 * {@inheritDoc}
	 */
	public void postSerializationInit() {
		// TODO 
	}

	/**
	 * {@inheritDoc}
	 */
	public void setParent(OdorWorld world) {
		parent = world;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean inhibitsMovement() {
		return inhibitsMovement;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInhibitsMovement(boolean inhibits) {
		inhibitsMovement = inhibits;		
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSmellSource(SmellSource source) {
		this.smellSource = source;
	}

	/**
	 * {@inheritDoc}
	 */
	public SmellSource getSmellSource() {
		return smellSource;
	}

	/**
	 * {@inheritDoc}
	 */
	public LifeCycle getLifeCycleObject() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public double[] getLocation() {
		return actualLocation;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLocation(double[] location) {
		actualLocation = location;
		agent.setSuggestedLocation(actualLocation); // TODO: Confusing... bad smell.s
	}

}