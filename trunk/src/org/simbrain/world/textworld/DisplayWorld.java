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
package org.simbrain.world.textworld;

import com.thoughtworks.xstream.XStream;

/**
 * <b>TextWorld</b> is a text world which is for display of text, and number >
 * text conversion.
 */
public class DisplayWorld extends TextWorld {

    /** Threshold for displaying text. */
    private double threshold = .5;

    /**
     * Constructs an instance of TextWorld.
     */
    public DisplayWorld() {
    }

    /**
     * Advance the position in the text, and update the current item.
     */
    public void update() {
    }

    /**
     * Add a text to the end of the underling text object.
     * 
     * @param newText the text to add
     */
    public void addText(String newText) {
        if (getText() != null) {
            setPosition(getText().length());
            setText(getText().concat(newText));
        } else {
            setText(newText);
        }
        fireTextChangedEvent();
    }

    /**
     * Add the provided text, if the provided value is above threshold. Called
     * by consumers reading data from (e.g) neural networks. If node activation
     * > threshold then display a particular word.
     * 
     * @param string text to add
     * @param value value to check against threshold
     */
    public void addTextIfAboveThreshold(final double value, final String string) {
        if (value > threshold) {
            addText(string + " "); // TODO: Replace space with user-specified "buffer" string
        }
    }

    /**
     * Returns a properly initialized xstream object.
     * 
     * @return the XStream object
     */
    static XStream getXStream() {
        XStream xstream = TextWorld.getXStream();
        return xstream;
    }

}
