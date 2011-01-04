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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.propertyeditor.ComboBoxWrapper;

/**
 * <b>TextWorld</b> acts as a text interface to neural networks, for use in
 * language parsing and other tasks. Users input text which parsed into vector
 * form and sent to the network, and vectors from the network are converted into
 * text and sent to this world.
 */
public class TextWorld {

    // TODO: Set pause time, create editable dictionary
    // TODO: The outputs don't come out nicely
    // TODO: Set output menu using largest vector in the current dictionary
    // TODO: Ability to set different delimiters

    /** Time to pause (in milliseconds) between parsed text to be sent. */
    private int pauseTime = 100;

    /** Highlight color. */
    private Color highlightColor = Color.GRAY;

    /** Does enter read current line. */
    private boolean sendEnter = true;

    /** List of parsing style. */
    public enum ParseStyle {
        CHARACTER, WORD, SENTENCE
    };

    /** The current parsing styles. */
    private ParseStyle parseStyle = ParseStyle.WORD;

    /** The current input coding. */
    private double[] inputCoding;

    /** List of listeners on this world. */
    private List<TextListener> listenerList = new ArrayList<TextListener>();

    /**
     * Constructs an instance of TextWorld.
     */
    public TextWorld() {

        // TODO: 10 is just arbitrary for now.  When the design if fleshed out more it can be set
        //      and changed in some principled way.
        inputCoding = SimbrainMath.zeroVector(10);

        // For Testing
        // dictionary.put("this", new double[] { .1, .2, -1, 0 });
        // dictionary.put("is", new double[] { .2, 0, 0, 4 });
        // dictionary.put("a", new double[] { .3, 1, 5, 4 });
        // dictionary.put("test", new double[] { .4, .5, -.9, 1 });
    }

    /**
     * Add a listener.
     *
     * @param listener the listener to add
     */
    public void addListener(TextListener listener) {
        listenerList.add(listener);
    }

    /**
     * Remove a listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(TextListener listener) {
        listenerList.remove(listener);
    }

    /**
     * @return the pauseTime
     */
    public int getPauseTime() {
        return pauseTime;
    }

    /**
     * @param pauseTime the pauseTime to set
     */
    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    /**
     * @return the highlightColor
     */
    public Color getHighlightColor() {
        return highlightColor;
    }

    /**
     * @param highlightColor the highlightColor to set
     */
    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
    }

    /**
     * @return the sendEnter
     */
    public boolean isSendEnter() {
        return sendEnter;
    }

    /**
     * @param sendEnter the sendEnter to set
     */
    public void setSendEnter(boolean sendEnter) {
        this.sendEnter = sendEnter;
    }

    /**
     * Returns the current parse style inside a comboboxwrapper. Used by
     * preference dialog.
     *
     * @return the the comboBox
     */
    public ComboBoxWrapper getParseStyle() {
        return new ComboBoxWrapper() {
            public Object getCurrentObject() {
                return parseStyle;
            }

            public Object[] getObjects() {
                return ParseStyle.values();
            }
        };
    }

    /**
     * Set the current parse style. Used by preference dialog.
     *
     * @param parseStyle the current style.
     */
    public void setParseStyle(ComboBoxWrapper parseStyle) {
        setTheParseStyle((ParseStyle) parseStyle.getCurrentObject());
    }

    /**
     * Set the parse style object.
     *
     * @param parseStyle the current parse style
     */
    private void setTheParseStyle(ParseStyle parseStyle) {
        this.parseStyle = parseStyle;
    }

    /**
     * Get the current parse style.
     *
     * @return the current parse style
     */
    public ParseStyle getTheParseStyle() {
        return parseStyle;
    }

    /**
     * @return the inputCoding
     */
    public double[] getInputCoding() {
        return inputCoding;
    }

    /**
     * Notify listeners that the text has changed.
     */
    private void fireTextChangedEvent() {
        for (TextListener listener : listenerList) {
            listener.textChanged();
        }
    }

    /**
     * Code a character numerically. Convert a given character to an array of
     * doubles.
     *
     * @param c the character to be coded.
     */
    public void encodeCharacter(char c) {

        // This is just a quick temporary method to generate a
        //  vector from a character
        String binaryRep = Integer.toBinaryString(c);
        for (int i = 0; i < inputCoding.length - 1; i++) {
            if (i < binaryRep.length()) {
                inputCoding[i] = Double.parseDouble(binaryRep.substring(i,
                        i + 1));
            }
        }
        fireTextChangedEvent();
    }

    /**
     * Code a word numerically. Convert a given string representation of a word
     * to an array of doubles.
     *
     * @param word the word to be coded
     */
    public void encodeWord(String word) {
        // Another temporary coding scheme
        for (int i = 0; i < inputCoding.length - 1; i++) {
            if (i < word.length()) {
                inputCoding[i] = word.codePointAt(i);
            }
        }
        fireTextChangedEvent();
    }

}
