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

import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;

/**
 * <b>WorldEntity</b> represents an entity in the world.  These objects represent distal 
 * stimuli relative to the creature.  The functions in this class convert the properties of that
 * distal stimulus into a proximal stimulus, that is, into a pattern of activity across the
 * input nodes of the network.
 */
public class OdorWorldEntity extends ImageIcon {	
	
    private static ImageIcon images[];
	private static final String FS = System.getProperty("file.separator");
	
	/** location of this object in the enviornment */
	private Point location = new Point();

	/** Filename of the image associated with this entity */
	private String imageName;
	
	/** for combo boxes */
	public static final String[] imageNames = {"Mouse.gif", "Fish.gif", "PinkFlower.gif",
	        "Flower.gif", "Gouda.gif", "Swiss.gif", "Bluecheese.gif"};

	protected OdorWorld parent;
	
	private Stimulus theStimulus = new Stimulus();

	public OdorWorldEntity() {
	}

	/**
	 * Construct a world entity with a random smell signature
	 * 
	 * @param im_name kind of entity (flower, cheese, etc)
	 * @param x x location of new entity
	 * @param y y location of new entity
	 */
	public OdorWorldEntity(OdorWorld wr, String im_name, int x, int y) {
		parent = wr;
		imageName = im_name;
		setImage(ResourceManager.getImage(imageName));
		setLocation(new Point(x,y));
	}



	/////////////////////////
	// Getters and Setters //
	/////////////////////////
	public void setLocation(Point newPosition) {
		location = newPosition;
	}

	public Point getLocation() {
		return location;
	}

	public int getX() {
		return location.x;
	}
	
	public int getY() {
		return location.y;
	}
	
	public void setX(int x) {
		location.x = x;
	}
	public void setY(int y) {
		location.y = y;
	}
	
	public String getImageName() {
		return imageName;
	}

	public void setImageName(String string) {
		this.setImage(ResourceManager.getImage(string));
		imageName = string;
	}
	
	/**
	 * To display images in combo boxes
	 * 
	 * @return Image to display
	 * 
	 * Is this really a good place for this method?
	 */
	public static ImageIcon[] imagesRenderer(){
		images = new ImageIcon[OdorWorldEntity.getImageNames().length];
        for (int i = 0; i < OdorWorldEntity.getImageNames().length; i++) {
            images[i] = new ImageIcon( "." + FS + "bin" + FS + "org" + FS +
                    "simbrain" + FS + "resource" + FS + OdorWorldEntity.getImageNames()[i]);
            images[i].setDescription(OdorWorldEntity.getImageNames()[i]);
        }
        return images;
	}

	/**
	 * Helper function for combo boxes
	 */	
	public int getImageNameIndex(String in) {
		for (int i = 0; i < imageNames.length; i++) {
			if (in.equals(imageNames[i])) {
				return i;
			}
		}
		return 0;
	}
	
	/**
	 * Move world object to a specified location
	 * 
	 * @param object_index which object to move, 0 for creature
	 * @param x x coordinate of target location
	 * @param y y coordintae of target location
	 */
	public void moveTo(int object_index, int x, int y) {
		if (object_index == 0) {
			setLocation(new Point(x, y));			
		} else if (object_index <= parent.getEntityList().size()) {
			((OdorWorldEntity)parent.getEntityList().get(object_index-1)).setLocation(new Point(x,y));
		}
	}

	public static String[] getImageNames() {
		return imageNames;
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
	public void setParent(OdorWorld parentWorld) {
		this.parent = parentWorld;
	}
	
	/**
	 * @return Returns the theStimulus.
	 */
	public Stimulus getStimulus() {
		return theStimulus;
	}
	/**
	 * @param theStimulus The theStimulus to set.
	 */
	public void setStimulus(Stimulus theStimulus) {
		this.theStimulus = theStimulus;
	}
}
