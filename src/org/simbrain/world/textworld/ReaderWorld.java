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
import org.simbrain.workspace.Producible;
import org.simbrain.world.textworld.TextListener.TextAdapter;

import com.thoughtworks.xstream.XStream;

/**
 * <b>ReaderWorld</b> intuitively models "reading". Text in the main display is
 * parsed by letter or word (where a "word" is the determined by a regular
 * expression that can be customized), and highlighted. This item is converted
 * in to scalar or vector values and sent to consumers (mainly neurons and
 * neuron groups) via couplings.
 *
 * When the reader world is updated, the current character or word is
 * highlighted. A dictionary is consulted, and if a match is found, any
 * correspond couplings produce values:
 * <ul>
 * <li>Scalar: When the character or word is highlighted, send a value of 1 to
 * all associated consumers. Stored in the "token dictionary"</li>
 * <li>Vector: When the character or word is highlighted, send a vector to all
 * associated consumers. These vectors are specified in the "vector dictionary".
 * </li>
 * </ul>
 */
public final class ReaderWorld extends TextWorld {

    /**
     * The reader world "dictionary", which associates string tokens with arrays
     * of doubles.
     */
    private final LinkedHashMap<String, double[]> tokenToVectorDictionary = new LinkedHashMap<String, double[]>();

    /** The current text item. */
    private TextItem currentTextItem;

    /**
     * Length of vectors in the tokenToVector Dict. Assumes all vectors in the
     * dictionary have the same length. Currently reset whenever a new item is
     * added to the dictionary. (TODO: There is no current way of ensuring that
     * only vectors with the same number of components are added to the dict).
     */
    private int vectorLength = 5;

    /** List of parsing style. */
    public enum ParseStyle {
        CHARACTER, WORD
    };

    /** The current parsing style. */
    private ParseStyle parseStyle = ParseStyle.WORD;

    /** Regular expression pattern. By default search for whole words */
    private Pattern pattern;

    // TODO: Document other good choices in the pref dialog. e.g. (\\w+)
    /** Regular expression for matcher. */
    private String regularExpression = "(\\S+)";

    /** Pattern matcher. */
    private Matcher matcher;

    // Initialize tokenToVectorDictionary
    {
        tokenToVectorDictionary.put("hello", new double[] { .2, 0, 0 });
        tokenToVectorDictionary.put("how", new double[] { 1, 0, 1 });
        tokenToVectorDictionary.put("are", new double[] { 0, 1, 0 });
        tokenToVectorDictionary.put("you", new double[] { 1, .5, 0 });
    }

    /**
     * Factory method for Reader world.
     *
     * @return the constructed world.
     */
    public static ReaderWorld createReaderWorld() {
        final ReaderWorld r = new ReaderWorld();
        r.addListener(new TextAdapter() {

            public void textChanged() {
                r.updateMatcher();
            }

            public void positionChanged() {
                r.updateMatcher();
            }
        });
        return r;
    }

    /**
     * Constructs an instance of TextWorld.
     */
    private ReaderWorld() {
        pattern = Pattern.compile(regularExpression);
        matcher = pattern.matcher(getText());
    }

    /**
     * Returns 1 if the current item is this character, or 0 otherwise. Used for
     * localist representations of letters.
     *
     * @param token the letter to search for
     * @return 1 if the letter is contained, 0 otherwise.
     */
    public int getMatchingScalar(String token) {
        if (getCurrentItem() == null) {
            return 0;
        }
        if (getCurrentItem().getText().equalsIgnoreCase(token)) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Return the vector associated with the currently parsed token, or a
     * default zero vector.
     *
     * @param token the token to associate with a vector
     * @return the associated vector
     */
    public double[] getMatchingVector(String token) {
        double[] vector = tokenToVectorDictionary.get(token);
        //System.out.println(token + "-->" + Arrays.toString(vector));
        if (vector == null) {
            // Return zero vector if no matching string is found in the token
            // map.
            return new double[vectorLength];
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
    @Producible
    public double[] getCurrentVector() {
        if (getCurrentItem() == null) {
            return new double[vectorLength];
        } else {
            return getMatchingVector(getCurrentItem().getText());
        }
    }

    /**
     * Returns a standard java string containing the character or characters
     * selected by the reader world.
     * @return the current string
     */
    @Producible
    public String getCurrentString() {
        if (getCurrentItem() == null) {
            return "";
        } else {
            return getCurrentItem().getText();
        }
    }

    /**
     * Loads a vector to token dictionary.
     *
     * @param tableData the dictionary to add.
     */
    public void loadTokenToVectorDict(String[][] tableData) {
        tokenToVectorDictionary.clear();
        for (int i = 0; i < tableData.length; i++) {
            double[] vector = Utils.parseVectorString(tableData[i][1]);
            addTokenVectorPair(tableData[i][0], vector);
        }
        fireDictionaryChangedEvent();
    }

    /**
     * Add an entry to the token-vector dictionary.
     *
     * @param token the String to add
     * @param vector the vector
     */
    public void addTokenVectorPair(String token, double[] vector) {
        tokenToVectorDictionary.put(token, vector);
        vectorLength = vector.length;
    }

    /**
     * @return the tokenVectorMap
     */
    public LinkedHashMap<String, double[]> getTokenToVectorDict() {
        return tokenToVectorDictionary;
    }

    /**
     * Advance the position in the text, and update the current item.
     */
    public void update() {
        if (parseStyle == ParseStyle.CHARACTER) {
            wrapText();
            int begin = getPosition();
            int end = getPosition() + 1;
            setCurrentItem(new TextItem(begin, end, getText().substring(begin,
                    end)));
            setPosition(end);
        } else if (parseStyle == ParseStyle.WORD) {
            if (matcher == null) {
                return;
            }
            wrapText();
            boolean matchFound = findNextToken();
            if (matchFound) {
                selectCurrentToken();
            } else {
                // No match found. Go back to the beginning of the text area
                // and select the first token found
                setPosition(0);
                updateMatcher();
                // Having wrapped to the beginning select the next token, if
                // there is one.
                if (findNextToken()) {
                    selectCurrentToken();
                }
            }
        }

    }

    /**
     * Reset the parser and specify the region focused on by it, to go from the
     * current cursor position to the end of the text.
     */
    void updateMatcher() {
        int begin = getPosition();
        int end = getText().length();
        // System.out.println(begin + "," + end);
        matcher.reset(getText());
        matcher.region(begin, end);
    }

    /**
     * Find the next token in the text area.
     *
     * @return true if some token is found, false otherwise.
     */
    private boolean findNextToken() {
        boolean foundToken = matcher.find();
        if (foundToken) {
            int begin = matcher.start();
            int end = matcher.end();
            String text = matcher.group();
            // System.out.println("[" + text + "](" + begin + "," + end + ")");
            currentTextItem = new TextItem(begin, end, text);
        } else {
            currentTextItem = null;
        }
        return foundToken;
    }

    /**
     * Select the current token.
     */
    private void selectCurrentToken() {
        setCurrentItem(currentTextItem);
        setPosition(currentTextItem.getEndPosition());
    }

    /**
     * If the position is at the end of the text area, "reset" the position to
     * 0.
     */
    private void wrapText() {
        if (atEnd()) {
            setPosition(0);
            updateMatcher();
        }
    }

    /**
     * @return true if the current position is past the end of the text area,
     *         false otherwise.
     */
    private boolean atEnd() {
        return getPosition() >= getText().length();
    }

    /**
     * Utility method to "preview" the next token after the current one. Used in
     * some scripts.
     *
     * @return the next token in the text area.
     */
    public String previewNextToken() {
        matcher.find();
        String nextOne = matcher.group();
        updateMatcher(); // Return matcher to its previous state
        return nextOne;
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
        firePrefsChangedEvent();
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
     * @param parseStyle the parseStyle to set
     */
    public void setParseStyle(ParseStyle parseStyle) {
        this.parseStyle = parseStyle;
        // TODO: Fire an event that the radio button listens to
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
     * @return the regularExpression
     */
    public String getRegularExpression() {
        return regularExpression;
    }

    /**
     * @param regularExpression the regularExpression to set
     */
    public void setRegularExpression(String regularExpression) {
        this.regularExpression = regularExpression;
        pattern = Pattern.compile(regularExpression);
        matcher = pattern.matcher(getText());
        updateMatcher();
    }

}
