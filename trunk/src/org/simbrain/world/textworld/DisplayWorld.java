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

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.xstream.XStream;

/**
 * <b>DisplayWorld</b> is a text world which is for display of text, and number
 * to text conversion.
 */
public class DisplayWorld extends TextWorld {

    /** A set of strings that can be displayed via couplings in the world. */
    protected Set<String> dictionary = new TreeSet<String>();

    // Initialize dictionary with sample entries
    {
        dictionary.add("mouse");
        dictionary.add("cheese");
        dictionary.add("flower");
        dictionary.add("poison");
        dictionary.add("yum!");
        dictionary.add("yuck!");
    }

    /** Threshold for displaying text. */
    private double displayThreshold = .5;

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
     * @param value value to check against threshold
     * @param string text to add
     */
    public void addTextIfAboveThreshold(final double value, final String string) {
        if (value > displayThreshold) {
            addText(string + " "); // TODO: Replace space with user-specified
                                   // "buffer" string
        }
    }

    /**
     * @return the wordList
     */
    public Set<String> getDictionary() {
        return Collections.unmodifiableSet(dictionary);
    }

    /**
     * Add a word to the dictionary.
     *
     * @param word the word to add
     */
    public void addWordToDictionary(String word) {
        dictionary.add(word);
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

    /**
     * @return the displayThreshold
     */
    public double getDisplayThreshold() {
        return displayThreshold;
    }

    /**
     * @param displayThreshold the displayThreshold to set
     */
    public void setDisplayThreshold(double displayThreshold) {
        this.displayThreshold = displayThreshold;
    }

    /**
     * Reset the dictionary (e.g. after it's been edited.)
     *
     * @param newDict the new dictionary entries
     */
    public void resetDictionary(String[][] newDict) {
        dictionary.clear();
        for (int i = 0; i < newDict.length; i++) {
            dictionary.add(newDict[i][0]);
        }
        fireDictionaryChangedEvent();
    }

}
