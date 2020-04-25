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
package org.simbrain.network.core;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.events.NetworkTextEvents;

import java.awt.geom.Point2D;

/**
 * <b>NetworkTextObject</b> is a string of text in a neural network, typically
 * used to label elements of a neural network simulation. Contains basic text
 * properties as well.
 */
public class NetworkTextObject implements LocatableModel {

    /**
     * Reference to parent root network of this text object.
     */
    private final Network parent;

    /**
     * x-coordinate of this object in 2-space.
     */
    private double x;

    /**
     * y-coordinate of this object in 2-space.
     */
    private double y;

    /**
     * The main text data.
     */
    private String text = "";

    /**
     * Name of Font for this text.
     */
    private String fontName = "Helvetica";

    /**
     * Font size.
     */
    private int fontSize = 12;

    /**
     * Is this text italic or not.
     */
    private boolean italic;

    /**
     * Is this text bold or not.
     */
    private boolean bold;

    /**
     * Support for property change events.
     */
    private transient NetworkTextEvents events = new NetworkTextEvents(this);

    /**
     * Construct the text object.
     *
     * @param parent root network
     * @param x      x position
     * @param y      y position
     */
    public NetworkTextObject(Network parent, double x, double y) {
        this.parent = parent;
        this.x = x;
        this.y = y;
    }

    /**
     * Construct the text object with initial text.
     *
     * @param parent      root network
     * @param x           x position
     * @param y           y position
     * @param initialText text for the text object
     */
    public NetworkTextObject(Network parent, double x, double y, String initialText) {
        this(parent, x, y);
        this.setText(initialText);
    }

    /**
     * Copy constructor.
     *
     * @param parent parent network for this text object
     * @param text   text object to copy
     */
    public NetworkTextObject(Network parent, NetworkTextObject text) {
        this.parent = parent;
        this.text = text.getText();
        this.x = text.x;
        this.y = text.y;
        this.fontSize = text.getFontSize();
        this.fontName = text.getFontName();
        this.bold = text.isBold();
        this.italic = text.isItalic();
    }


    @Override
    public String toString() {
        return "(" + Math.round(x) + "," + Math.round(y) + "):" + text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    /**
     * Notify listeners that this object has been deleted.
     */
    public void fireDeleted() {
        events.fireDelete();
    }

    /**
     * Perform any initialization required when creating a text object, but after the
     * parent network has been added.
     */
    public void postUnmarshallingInit() {
        events = new NetworkTextEvents(this);
    }

    @NotNull
    @Override
    public Point2D getLocation() {
        return new Point2D.Double(x, y);
    }

    @Override
    public void setLocation(@NotNull Point2D location) {
        x = location.getX();
        y = location.getY();
        events.fireLocationChange();
    }

    public NetworkTextEvents getEvents() {
        return events;
    }

    @Override
    public String getLabel() {
        return text;
    }

    @Override
    public void update() {
        // No implementation
    }

    @Override
    public void setBufferValues() {
        // No implementaiton
    }

    @Override
    public void applyBufferValues() {
        // No implementaiton
    }
}
