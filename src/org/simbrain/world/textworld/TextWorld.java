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
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>TextWorld</b> is an environment for modeling speech and reading and other
 * linguistic phenomena. It is the superclass for readerworld, where text is
 * converted or "read" and used to produce activation in neural nets (reader
 * world), and display world, where activations from neural nets can be used to
 * display text (e.g. modeled speech).
 */
public abstract class TextWorld {

    /** The main text in the text world. */
    private String text = "";

    /** The current item of text (letter, word, etc.) */
    private TextItem currentItem;

    /** What the current position in the text is. */
    private int position = 0;

    /** Last position in the text. */
    private int lastPosition = 0;

    /** List of listeners on this world. */
    private List<TextListener> listenerList = new ArrayList<TextListener>();

    /** Highlight color. */
    private Color highlightColor = Color.GRAY;

    /**
     * Set of a strings that can be coupled to via scalar couplings. In
     * ReaderWorld when the token is parsed a 1 is sent to consumers In
     * DisplayWorld when a value above a threshold is consumed the token is
     * displayed in the text area.
     */
    protected Set<String> tokenDictionary = new TreeSet<String>();

    // Populate token dictionary with sample items
    {
        tokenDictionary.add("mouse");
        tokenDictionary.add("cheese");
        tokenDictionary.add("flower");
        tokenDictionary.add("poison");
        tokenDictionary.add("yum!");
        tokenDictionary.add("yuck!");
    }

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
    public void fireTextChangedEvent() {
        for (TextListener listener : listenerList) {
            listener.textChanged();
        }
    }

    /**
     * Notify listeners that the dictionary has changed.
     */
    public void fireDictionaryChangedEvent() {
        for (TextListener listener : listenerList) {
            listener.dictionaryChanged();
        }
    }

    /**
     * Notify listeners that preferences have changed.
     */
    public void firePrefsChangedEvent() {
        for (TextListener listener : listenerList) {
            listener.preferencesChanged();
        }
    }

    /**
     * Notify listeners that the caret position has changed.
     */
    public void firePositionChangedEvent() {
        for (TextListener listener : listenerList) {
            listener.positionChanged();
        }
    }

    /**
     * Notify listeners that the caret position has changed.
     * @param newItem
     */
    public void fireCurrentItemChanged(TextItem newItem) {
        for (TextListener listener : listenerList) {
            listener.currentItemChanged(newItem);
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
        setText(text, true);
    }

    /**
     * Set text, and fire an event if the fireEvent flag is set.
     *
     * @param text the text to set
     * @param fireEvent whether or not to fire an event
     */
    public void setText(final String text, final boolean fireEvent) {
        this.text = text;
        if (fireEvent) {
            fireTextChangedEvent();
        }
    }

    /**
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * @param newPosition the position to set
     */
    public void setPosition(int newPosition) {
        setPosition(newPosition, true);
    }

    /**
     * Set position. Fire event only if specified.
     *
     * @param newPosition new position to set
     * @param fireEvent whether to fire event
     */
    public void setPosition(final int newPosition, final boolean fireEvent) {
        if (newPosition <= text.length()) {
            lastPosition = position;
            this.position = newPosition;
            if (fireEvent) {
                firePositionChangedEvent();
            }
        } else {
            System.err.println("Invalid position:" + newPosition);
        }
    }

    /**
     * @return the lastPosition
     */
    public int getLastPosition() {
        return lastPosition;
    }

    /**
     * @return the currentItem
     */
    public TextItem getCurrentItem() {
        return currentItem;
    }

    /**
     * @param currentItem the currentItem to set
     */
    public void setCurrentItem(TextItem currentItem) {
        this.currentItem = currentItem;
        fireCurrentItemChanged(currentItem);
    }

    /**
     * Returns the text of the current item
     *
     * @return text of current item, or null if current item is null.
     */
    public String getCurrentText() {
        if (currentItem == null) {
            return "";
        } else {
            return currentItem.getText();
        }
    }

    /**
     * Returns a "preview" of the next character in the world. Used in some
     * scripts.
     *
     * @return the next character.
     */
    public String previewNextChar() {
        if (position < text.length()) {
            return text.substring(position, position + 1);
        } else if (position == text.length()) {
            return text.substring(0, 1);
        }
        return "";
    }

    /**
     * Reset the dictionary (e.g. after it's been edited.)
     *
     * @param newDict the new dictionary entries
     */
    public void loadTokenDictionary(String[][] newDict) {
        tokenDictionary.clear();
        for (int i = 0; i < newDict.length; i++) {
            tokenDictionary.add(newDict[i][0]);
        }
        fireDictionaryChangedEvent();
    }

    /**
     * @return the wordList
     */
    public Set<String> getTokenDictionary() {
        return tokenDictionary;
    }

    /**
     * Add a word to the dictionary.
     *
     * @param word the word to add
     */
    public void addWordToTokenDictionary(String word) {
        tokenDictionary.add(word);
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

    /**
     * Represents the "current item" as String, and includes a representation of
     * the beginning and ending of the item in the main text.
     */
    public class TextItem {

        /** Initial position in main text. */
        private final int beginPosition;

        /** Final position in main text. */
        private final int endPosition;

        /** The item text. */
        private final String text;

        /**
         * Construct this text item.
         *
         * @param beginPosition
         * @param endPosition
         * @param text
         */
        public TextItem(int beginPosition, int endPosition, String text) {
            this.beginPosition = beginPosition;
            this.endPosition = endPosition;
            this.text = text;
        }

        /**
         * @return the beginPosition
         */
        public int getBeginPosition() {
            return beginPosition;
        }

        /**
         * @return the endPosition
         */
        public int getEndPosition() {
            return endPosition;
        }

        /**
         * @return the text
         */
        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "(" + beginPosition + "," + endPosition + ") " + text;
        }

    }
}
