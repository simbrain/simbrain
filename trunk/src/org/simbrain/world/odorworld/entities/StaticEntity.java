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
import java.awt.Rectangle;

import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.environment.TwoDEnvironment;
import org.simbrain.world.odorworld.LifeCycle;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * <b>StaticEntity</b> represents an entity in the world. 
 */
public class StaticEntity implements OdorWorldEntity {

    /** Icon renderer. */
    private ImageIcon image = new ImageIcon();

    /** location of this object in the environment. */
    private double[] location = new double[2];

    /** Filename of the image associated with this entity. */
    private String imageName;

    /** Parent world. */
    private OdorWorld parent;

    /** Name of entity. */
    private String name = "";

    /** Whether this inhibits movement. */
	private boolean inhibitsMovement = true;

	/** Smell of this entity, if any. */
	private SmellSource smellSource;
	
	/** Life cycle of this entity, if it can be eaten. */
	private LifeCycle lifeCycle;

    /**
     * Construct a world entity with a random smell signature.
     *
     * @param imageName kind of entity (flower, cheese, etc)
     * @param x x location of new entity
     * @param y y location of new entity
     * @param wr the world to which this belongs
     */
    public StaticEntity(final OdorWorld wr, final String imageName, final double[] location) {
        parent = wr;
        this.imageName = imageName;
        this.name = imageName;
        image.setImage(ResourceManager.getImage(imageName));
        this.location = location;
    	lifeCycle = new LifeCycle(this);
    }

    /**
     * @return Name of image.
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * @param string Sets name of image.
     */
    public void setImageName(final String string) {
        image.setImage(ResourceManager.getImage(string));
        imageName = string;
    }

    /**
     * @return Returns the parentWorld.
     */
    public OdorWorld getParent() {
        return parent;
    }

    /**
     * @param parentWorld The parentWorld to set.
     */
    public void setParent(final OdorWorld parentWorld) {
        this.parent = parentWorld;
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
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return The image.
     */
    ImageIcon getImage() {
        return image;
    }

    /**
     * @param theImage The image to be set.
     */
    void setImage(final Image theImage) {
        image = new ImageIcon();
        this.image.setImage(theImage);
    }

    /**
     * {@inheritDoc}
     */
	public void paintEntity(Component component, Graphics g) {
		//g.fillRect(this.getBounds().x, this.getBounds().y, this.getBounds().width, this.getBounds().height);
		image.paintIcon(component, g, (int) getSuggestedLocation()[0], (int) getSuggestedLocation()[1]);		
	}

    /**
     * {@inheritDoc}
     */
	public void postSerializationInit() {
        setImage(ResourceManager.getImage(getImageName()));
	}

    /**
     * {@inheritDoc}
     */
	public Rectangle getBounds() {
		return new Rectangle((int) location[0], (int) location[1], image.getIconWidth(), image.getIconHeight());
	}

    /**
     * {@inheritDoc}
     */
	public void update() {
		// No implementation
	}

    /**
     * {@inheritDoc}
     */
	public TwoDEnvironment getEnvironment() {
		return getParent();
	}
	
	/**
	 * Suggested = actual for static entities.
	 *
	 * @return
	 */
	public double[] getLocation() {
		return location;
	}

    /**
     * {@inheritDoc}
     */
	public void setLocation(double[] location) {
		this.location = location;	
	}

    /**
     * {@inheritDoc}
     */
	public double[] getSuggestedLocation() {
		return location;
	}

    /**
     * {@inheritDoc}
     */
	public void setSuggestedLocation(double[] location) {
		this.location = location;
	}

    /**
     * {@inheritDoc}
     */
	public boolean inhibitsMovement() {
		return this.inhibitsMovement;
	}

    /**
     * {@inheritDoc}
     */
	public void setInhibitsMovement(boolean inhibitsMovement) {
		this.inhibitsMovement = inhibitsMovement;
	}

    /**
     * {@inheritDoc}
     */
	public void setSmellSource(SmellSource source) {
		smellSource = source;		
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
		return lifeCycle;
	}
}
