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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simbrain.util.propertyeditor.ComboBoxWrapper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>ReaderWorld</b> parses underlying text and can produce numerical
 * representations of it.
 */
public class ReaderWorld extends TextWorld {

    /** The current item of text (letter, word, etc., depending on parse configuration) .*/
    private String currentItem;
    
    /** List of parsing style. */
    public enum ParseStyle {
        CHARACTER, WORD
    };

    /** The current parsing style. */
    private ParseStyle parseStyle = ParseStyle.CHARACTER;

    /** For use with word parsing. */
    private String delimeter = "";

    /** Regular expression for parsing. */
    private Pattern pattern;

    /** Pattern matcher. */
    private Matcher matcher;

    /**
     * Constructs an instance of TextWorld.
     */
    public ReaderWorld() {
        setDelimeter("\\s");
    }


    /**
     * Advance the position in the text, and update the current item.
     */
    public void update() {
        // parse text
        if (parseStyle == ParseStyle.CHARACTER) {
            if(getPosition() < getText().length()) {
                currentItem = getText().substring(getPosition(), getPosition()+1);
                setPosition(getPosition()+1);
            } else {
                currentItem = "";
            }
        } else if (parseStyle == ParseStyle.WORD) {

            // Search string from caret to end of doc
            matcher = pattern.matcher(getText().substring(getPosition()));
            if(matcher.find()) {
                currentItem = matcher.group();
                setPosition(getPosition() + matcher.group().length());
            } else {
                currentItem = "";
            }
        }
        fireTextChangedEvent();
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
     * @return the currentItem
     */
    public String getCurrentItem() {
        return currentItem;
    }


    /**
     * @param parseStyle the parseStyle to set
     */
    public void setParseStyle(ParseStyle parseStyle) {
        this.parseStyle = parseStyle;
    }

    /**
     * @return the delimeter
     */
    public String getDelimeter() {
        return delimeter;
    }


    /**
     * @param delimeter the delimeter to set
     */
    public void setDelimeter(String delimeter) {
        this.delimeter = delimeter;
        pattern = Pattern.compile(delimeter);
    }

    /**
     * Returns 1 if the current item is this character, or 0 otherwise.
     * Used for localist representations of letters.
     *
     * @param letter the letter to search for
     * @return 1 if the letter is contained, 0 otherwise.
     */
    public int matchCurrentLetter(char letter) {
        if (currentItem.equalsIgnoreCase(String.valueOf(letter))) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Returns 1 if the current item matches the provided text, 0 otherwise. Used for localist 
     * representation of words.
     *
     * @param text the text to search for
     * @return 1 if the word is contained, 0 otherwise.
     */
    public int matchCurrentItem(String text) {
        if (currentItem.equalsIgnoreCase(text)) {
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
        XStream xstream = TextWorld.getXStream();
        return xstream;
    }



}
