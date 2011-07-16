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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>TextWorld</b> is the base class for world types that involve the
 * processing and display of text in relation to (mainly for now) neural
 * networks.
 */
public abstract class TextWorld {

    /** The main text in the text world. */
    private String text;

    /** The current item of text (character, word, sentence, depending on parse style) .*/
    private String currentItem;

    /** What the current position in the text is. */
    private int position = 0;

    /** Last position in the text. */
    private int lastPosition;
    
    /** Word list for word parsing in both directions (reading and display). */
    private Set<String> dictionary = new TreeSet<String>();

    /** List of listeners on this world. */
    private List<TextListener> listenerList = new ArrayList<TextListener>();

    /** Highlight color. */
    private Color highlightColor = Color.GRAY;

    /**
     * Constructs an instance of TextWorld.
     */
    public TextWorld() {
    }

    /**
     * Advance the position in the text, and update the current item.
     */
    public abstract void update();
    
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
     * Notify listeners that the text has changed.
     */
    protected void fireTextChangedEvent() {
        for (TextListener listener : listenerList) {
            listener.textChanged();
        }
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
     * @return the position
     */
    public int getPosition() {
        return position;
    }


    /**
     * @param position the position to set
     */
    public void setPosition(int newPosition) {
        //System.out.println(position);
        lastPosition = position;
        this.position = newPosition;
    }


    /**
     * @return the currentItem
     */
    public String getCurrentItem() {
        return currentItem;
    }


    /**
     * @return the lastPosition
     */
    public int getLastPosition() {
        return lastPosition;
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
     * Check to see if the dictionary contains the provided word. Used
     * for localist representations of words at component level.
     * 
     * @param word word to check for
     * @return 1 if found, false otherwise
     */
    public double checkForWordInDictionary(String word) {
        if (dictionary.contains(word)) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(TextWorld.class, "listenerList");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    protected Object readResolve() {
        listenerList = new ArrayList<TextListener>();
        return this;
    }

}
