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

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.ComboBoxWrapper;

import com.thoughtworks.xstream.XStream;

/**
 * <b>ReaderWorld</b> parses the text in the underlying text world by letter or
 * "word" (where a "word" is the text between instances of a delimiter specified
 * by a regular expression), and can then convert these items to numbers for use
 * in, for example, a neural networks.
 */
public final class ReaderWorld extends TextWorld {

    /**
     * The reader world "dictionary", which associates string tokens with arrays
     * of doubles.
     */
    private final LinkedHashMap<String, double[]> vectorDictionary =
            new LinkedHashMap<String, double[]>();

    /**
     * Default zero vector to return if no matching entry is found in the vector
     * dictionary.
     */
    private final static double[] ZERO_VEC = new double[5];

    /** List of parsing style. */
    public enum ParseStyle {
        CHARACTER, WORD
    };

    /** The current parsing style. */
    private ParseStyle parseStyle = ParseStyle.WORD;

    /** For use with word parsing. */
    private String delimeter = "";

    /** Regular expression for parsing. */
    private Pattern pattern;

    /** Pattern matcher. */
    private Matcher matcher;

    /**
     * Position after the last delimeter found, which is where the current item
     * should begin.
     */
    private int positionAfterLastDelimeter;

    /**
     * Whether the matcher object is in a valid state so that matcher.end() can
     * be called.
     */
    private boolean matcherInValidState = false;

    //Initialize vector dictionary to sample values
    {
        vectorDictionary.put("hello", new double[] { .2, 0, 0 });
        vectorDictionary.put("how", new double[] { .1 });
        vectorDictionary.put("are", new double[] { 1 });
        vectorDictionary.put("you", new double[] { 0, .5, 0 });
    }

    /**
     * Factory method for Reader world.
     *
     * @return the constructed world.
     */
    public static ReaderWorld createReaderWorld() {
        final ReaderWorld r = new ReaderWorld();
        // Text Listener
        r.addListener(new TextListener() {

            public void textChanged() {
                // System.out.println("In textchanged");
                r.resetMatcher();
            }

            public void dictionaryChanged() {
            }

            public void positionChanged() {
                // System.out.println("In position changed");
                r.resetMatcher();
            }

            public void currentItemChanged(TextItem newItem) {
            }

        });
        return r;
    }
    /**
     * Constructs an instance of TextWorld.
     */
    private ReaderWorld() {
        // Set whitespace as default delimeter
        setDelimeter("\\s");
        resetMatcher();
    }


    /**
     * Reset the parser and specify the region focused on by it, to go from the
     * current cursor position to the end of the text.
     */
    void resetMatcher() {
        int begin = getPosition();
        int end = getText().length();
        // System.out.println(begin + "," + end);
        matcher = pattern.matcher(getText());
        matcher.region(begin, end);
        positionAfterLastDelimeter = begin;
        matcherInValidState = false;
    }

    /**
     * Advance the position in the text, and update the current item.
     */
    public void update() {
        if (parseStyle == ParseStyle.CHARACTER) {
            if (getPosition() < getText().length()) {
                int begin = getPosition();
                int end = getPosition() + 1;
                if (begin <= end) {
                    setCurrentItem(new TextItem(begin, end, getText()
                            .substring(begin, end)));
                    setPosition(end);
                } else {
                    System.err.println("Problem with positions:" + begin + ","
                            + end);
                }

            } else {
                // System.out.println("here");
                // setCurrentItem(new TextItem(getPosition(), getPosition(),
                // ""));
                setPosition(1);
            }
        } else if (parseStyle == ParseStyle.WORD) {
            if (matcher != null) {
                if (matcherInValidState) {
                    // System.out.println("matcherInValidState");
                    positionAfterLastDelimeter = matcher.end();
                }
                if (getPosition() < getText().length()) {
                    if (matcher.find()) {
                        matcherInValidState = true;
                        // System.out.println("Delimeter found: ["
                        // + matcher.group() + "](" + matcher.start()
                        // + "," + matcher.end() + ")");
                        int begin = positionAfterLastDelimeter;
                        int end = matcher.start();
                        if (begin <= end) {
                            setCurrentItem(new TextItem(begin, end, getText()
                                    .substring(begin, end)));
                            setPosition(end, false); // But now it does not
                                                     // reset!
                        } else {
                            System.err.println("Problem with positions:"
                                    + begin + "," + end);
                        }
                        // System.out.println(getCurrentItem());
                    } else {
                        matcherInValidState = false;
                        // System.out.println("nothing found");
                        setCurrentItem(new TextItem(getPosition(),
                                getPosition(), ""));
                        setPosition(0); // TODO:Option to reset to beginning of
                                        // text
                    }
                } else {
                    // System.out.println("position >= text.length");
                    setCurrentItem(new TextItem(getPosition(), getPosition(),
                            ""));
                    setPosition(0);
                }
            }
        }
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
     * @param parseStyle
     *            the current style.
     */
    public void setParseStyle(ComboBoxWrapper parseStyle) {
        setTheParseStyle((ParseStyle) parseStyle.getCurrentObject());
    }

    /**
     * Set the parse style object.
     *
     * @param parseStyle
     *            the current parse style
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
     * @param parseStyle
     *            the parseStyle to set
     */
    public void setParseStyle(ParseStyle parseStyle) {
        this.parseStyle = parseStyle;
        //TODO: Fire an event that the radio button listens to
    }

    /**
     * @return the delimeter
     */
    public String getDelimeter() {
        return delimeter;
    }

    /**
     * @param delimeter
     *            the delimeter to set
     */
    public void setDelimeter(String delimeter) {
        this.delimeter = delimeter;
        pattern = Pattern.compile(delimeter);
        resetMatcher();
    }

    /**
     * Returns 1 if the current item is this character, or 0 otherwise. Used for
     * localist representations of letters.
     *
     * @param letter
     *            the letter to search for
     * @return 1 if the letter is contained, 0 otherwise.
     */
    public int matchCurrentLetter(char letter) {
        if (getCurrentItem() == null) {
            return 0;
        }
        if (getCurrentItem().getText().equalsIgnoreCase(String.valueOf(letter))) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Return the vector associated with the currently parsed token, or a
     * default zero vector.
     *
     * @param token
     *            the token to associate with a vector
     * @return the associated vector
     */
    private double[] getVector(String token) {
        double[] vector = vectorDictionary.get(token);
        if (vector == null) {
            // Default vector if no matching string is found in the token map.
            return ZERO_VEC;
        } else {
            return vector;
        }
    }

    /**
     * Returns the double array associated with the currently selected token
     * (character or word). The reader world can produce a vector at any moment
     * by calling this function. Called by reflection by ReaderComponent.
     *
     * @return the vector corresponding to the currently parsed token.
     */
    public double[] getCurrentVector() {
        // System.out.println(Arrays.toString(this.getVector(this.getCurrentItem()
        // .getText())));
        if (getCurrentItem() == null) {
            return this.ZERO_VEC;
        } else {
            return this.getVector(this.getCurrentItem().getText());
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

    /**
     * Reset the reader world dictionary.
     *
     * @param tableData array of strings to re-populate dictionary
     */
    public void resetVectorDictionary(String[][] tableData) {
        vectorDictionary.clear();
        for (int i = 0; i < tableData.length; i++) {
            double[] vector = Utils.parseVectorString(tableData[i][1]);
            vectorDictionary.put(tableData[i][0], vector);
        }
        fireDictionaryChangedEvent();
    }

    /**
     * @return the tokenVectorMap
     */
    public LinkedHashMap<String, double[]> getVectorDictionary() {
        return vectorDictionary;
    }
}
