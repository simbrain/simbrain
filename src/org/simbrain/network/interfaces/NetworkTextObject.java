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
package org.simbrain.network.interfaces;

/**
 * <b>NetworkTextObject</b> is a string of text in a neural network, typically
 * used to label elements of a neural network simulation.  Contains basic text
 * properties as well.
 */
public class NetworkTextObject {

    /** Reference to parent root network of this text object. */
    private final RootNetwork parent;

    /** x-coordinate of this object in 2-space. */
    private double x;

    /** y-coordinate of this object in 2-space. */
    private double y;

    /** The main text data. */
    private String text = "";

    /** Name of Font for this text. */
    private String fontName = "Helvetica";

    /** Font size. */
    private int fontSize = 12;

    /** Is this text italic or not. */
    private boolean italic;

    /** Is this text bold or not. */
    private boolean bold;

    /**
     * Construct the text object.
     *
     * @param parent root network
     * @param x x position
     * @param y y position
     */
    public NetworkTextObject(RootNetwork parent, double x, double y) {
        this.parent = parent;
        this.x = x;
        this.y = y;
    }

    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "(" + Math.round(x) + "," + Math.round(y) + "):" + text;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the fontName
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * @param fontName the fontName to set
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * @return the fontSize
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * @param fontSize the fontSize to set
     */
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * @return the italic
     */
    public boolean isItalic() {
        return italic;
    }

    /**
     * @param italic the italic to set
     */
    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    /**
     * @return the bold
     */
    public boolean isBold() {
        return bold;
    }

    /**
     * @param bold the bold to set
     */
    public void setBold(boolean bold) {
        this.bold = bold;
    }


}
