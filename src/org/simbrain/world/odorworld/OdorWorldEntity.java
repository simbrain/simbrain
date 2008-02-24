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
package org.simbrain.world.odorworld;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;


/**
 * <b>WorldEntity</b> represents an entity in the world.  These objects represent distal  stimuli relative to the
 * creature.  The functions in this class convert the properties of that distal stimulus into a proximal stimulus,
 * that is, into a pattern of activity across the input nodes of the network.
 */
public class OdorWorldEntity extends AbstractEntity {

    /** Icon renderer. */
    private ImageIcon image = new ImageIcon();

    /** Images to be made into icons. */
    private static ImageIcon[] images;

    /** File system seperator. */
    private static final String FS = System.getProperty("file.separator");

    /** location of this object in the enviornment. */
    private Point location = new Point();

    /** Filename of the image associated with this entity. */
    private String imageName;

    /** for combo boxes. */
    public static final String[] IMAGENAMES = {"Bell.gif", "Candle.png",
            "Bluecheese.gif", "Fish.gif", "Flax.gif", "Flower.gif", 
            "Gouda.gif", "Mouse.gif", "Pansy.gif", "PinkFlower.gif", 
            "Poison.gif", "Swiss.gif", "Tulip.gif" };

    /** Parent world. */
    private OdorWorld parent;

    /** Name of entity. */
    private String name = "";

    /** The stimulus contained in the world. */
    private Stimulus theStimulus = new Stimulus();

    /** Is the stimuls edible. */
    private boolean edible;

    /** Initial number of bites to eat edible item. */
    private final int initBites = 30;

    /** Number of bites to heat edible item. */
    private int bitesToDie = initBites;

    /** Number of bites on stimulus. */
    private int bites = 0;

    /** Likelyhood eaten item will return. */
    private double resurrectionProb = 0;

    /**
     * Default constructor.
     */
    public OdorWorldEntity() {
}

    /**
     * Construct a world entity with a random smell signature.
     *
     * @param imageName kind of entity (flower, cheese, etc)
     * @param x x location of new entity
     * @param y y location of new entity
     * @param wr the world to which this belongs
     */
    public OdorWorldEntity(final OdorWorld wr, final String imageName, final int x, final int y) {
        parent = wr;
        this.imageName = imageName;
        this.name = imageName;
        image.setImage(ResourceManager.getImage(imageName));
        setLocation(new Point(x, y));
    }

    /**
     * Sets new location of entity.
     * @param newPosition New position of entity.
     */
    public void setLocation(final Point newPosition) {
        System.out.println("new location: " + newPosition);
        location = newPosition;
    }

    /**
     * @return Current location of entity.
     */
    public Point getLocation() {
        return location;
    }

    /**
     * @return X position.
     */
    public int getX() {
        return location.x;
    }

    /**
     * @return Y position
     */
    public int getY() {
        return location.y;
    }

    /**
     * Sets x position.
     * @param x Location
     */
    public void setX(final int x) {
        location.x = x;
    }

    /**
     * Sets y position.
     * @param y Location
     */
    public void setY(final int y) {
        location.y = y;
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
     * To display images in combo boxes.
     *
     * @return Image to display  Is this really a good place for this method?
     */
    public static ImageIcon[] imagesRenderer() {
        images = new ImageIcon[OdorWorldEntity.getImageNames().length];

        for (int i = 0; i < OdorWorldEntity.getImageNames().length; i++) {
            images[i] = new ImageIcon("." + FS + "bin" + FS + "org" + FS + "simbrain" + FS + "resource" + FS
                                      + OdorWorldEntity.getImageNames()[i]);
            images[i].setDescription(OdorWorldEntity.getImageNames()[i]);
        }

        return images;
    }

    /**
     * Helper function for combo boxes.
     * @return the imageNameIndex
     * @param in the string to compare
     */
    public int getImageNameIndex(final String in) {
        for (int i = 0; i < IMAGENAMES.length; i++) {
            if (in.equals(IMAGENAMES[i])) {
                return i;
            }
        }

        return 0;
    }

    /**
     * Move world object to a specified location.
     *
     * @param x x coordinate of target location
     * @param y y coordintae of target location
     */
    public void moveTo(final int x, final int y) {
        setLocation(new Point(x, y));
//        getParent().repaint();
//        getParent().setUpdateCompleted(true); // for thread
    }

    /**
     * Names of images.
     * @return Array of image names
     */
    public static String[] getImageNames() {
        return IMAGENAMES;
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
     * @return Returns the theStimulus.
     */
    public Stimulus getStimulus() {
        return theStimulus;
    }

    /**
     * @param theStimulus The theStimulus to set.
     */
    public void setStimulus(final Stimulus theStimulus) {
        this.theStimulus = theStimulus;
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
     * @return Width of image.
     */
    public int getWidth() {
        return image.getIconWidth();
    }

    /**
     * @return Height of image.
     */
    public int getHeight() {
        return image.getIconHeight();
    }

    /**
     * @return Rectangle for wall.
     */
    public Rectangle getRectangle() {
        return new Rectangle(getX() - (getWidth() / 2), getY() - (getHeight() / 2), getWidth(), getHeight());
    }

    /**
     * @param p Point at which rectangle is to be created.
     * @return Rectangle for wall.
     */
    public Rectangle getRectangle(final Point p) {
        return new Rectangle(p.x - (getWidth() / 2), p.y - (getHeight() / 2), getWidth(), getHeight());
    }

//    /**
//     * Paint the entity.
//     *
//     * @param g reference to the World's graphics object
//     */
//    public void paintThis(final Graphics g) {
//        getTheImage().paintIcon(getParent(), g, getLocation().x - halfsize, getLocation().y - halfsize);
//    }

    /**
     * @return Bites to eat item.
     */
    public int getBitesToDie() {
        return bitesToDie;
    }

    /**
     * @param bitesToDie Bites to eat an item.
     */
    public void setBitesToDie(final int bitesToDie) {
        this.bitesToDie = bitesToDie;
    }

    /**
     * @return Whether or not an item is edible.
     */
    public boolean getEdible() {
        return edible;
    }

    /**
     * @param edible Sets whether an item is edible.
     */
    public void setEdible(final boolean edible) {
        this.edible = edible;
    }

    /**
     * Removes the entity that was eaten.
     */
    public void terminate() {
        parent.getAbstractEntityList().remove(this);
        parent.getDeadEntityList().add(this);
    }

    /**
     * @return Likelyhood entity will reappear.
     */
    public double getResurrectionProb() {
        return resurrectionProb;
    }

    /**
     * @param resurrectionProb Likelyhood entity will reappear.
     */
    public void setResurrectionProb(final double resurrectionProb) {
        this.resurrectionProb = resurrectionProb;
    }

    /**
     * @return Number of bites.
     */
    public int getBites() {
        return bites;
    }

    /**
     * Number of times bitten.
     * @param bites Number of bites.
     */
    public void setBites(final int bites) {
        this.bites = bites;
    }

    /**
     * When an entity is resurrected, reset its bite counter.
     */
    public void reset() {
        bites = 0;
    }
}
