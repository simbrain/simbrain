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
import org.simbrain.util.Utils;
import org.simbrain.util.projection.DataPoint;
import org.simbrain.util.projection.NTree;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>DisplayWorld</b> intuitively models "speaking", though it can also be
 * useful as a diagnostic component. Any time it's useful to see text output in
 * Simbrain, a display world can be used.
 *
 * Numbers and vectors from (mainly) neural networks are converted into text
 * that is displayed in the display world. Display world is used in conjunction
 * with couplings (links between components in Simbrain), via consumers. When
 * the workspace is updated, consumers are activated, and corresponding text is
 * displayed.
 *
 * The display world can be thought of as containing a set of "buttons" (one for
 * each consumer). When a given button is activated by a coupling, the
 * corresponding text is displayed. As with reader world, there are scalar and
 * vector attributes:
 * <ul>
 * <li>Scalar: When the scalar coupling is activated, the associated token
 * (character or word) is produced and displayed. Stored in the
 * "token dictionary"</li>
 * <li>Vector: When the vector coupling is activated, the associated token is
 * produced and displayed. These vectors are specified in the
 * "vector dictionary". When a vector comes in, the closest vector in the
 * dictionary is found, and the corresponding token is displayed.</li>
 * </ul>
 *
 */
public class DisplayWorld extends TextWorld {

    /** Tree associating vectors with tokens. */
    private NTree vectorToTokenDict = new NTree(3);

    /**
     * Persistent form of data for the ntree.
     */
    private List<StringDataPoint> persistentData = new ArrayList<StringDataPoint>();

    // Initialize vectorToTokenDict dictionary with sample entries
    {

        this.addVectorTokenPair("mouse", new double[] { 1, 0, 0 });
        this.addVectorTokenPair("cheese", new double[] { 0, 1, 0 });
        this.addVectorTokenPair("flower", new double[] { 0, 0, 1 });
    }

    /** Threshold for displaying text. */
    private double displayThreshold = .5;

    /**
     * Constructs an instance of TextWorld.
     */
    public DisplayWorld() {
    }

    @Override
    public void update() {
    }

    /**
     * Finds the closest vector in terms of Euclidean distance, then returns the
     * String associated with it. Used by vector-based couplings. Separated from
     * {@link #displayClosestWord(double[])} so it can be used in scripts.
     *
     * @param key the vector to check.
     * @return the closest associated String
     */
    public String getClosestWord(double[] key) {
        // TODO: This could be made more complex using the ntree properties. E.g
        // if not within a threshold return a default vector.
        // I tried this before but it raised too many issues (like what the
        // default tolerance should be) so put it off.
        DataPoint point = vectorToTokenDict.getClosestPoint(new DataPoint(key));
        if (point != null) {
            if (point instanceof StringDataPoint) {
                return ((StringDataPoint) point).getString();
            }
        }
        return "";
    }

    /**
     * Display the string associated with the closest matching vector in the
     * dictionary. See {@link #getClosestWord(double[])}.
     *
     * @param key the vector to use to search the dictionary.
     */
    public void displayClosestWord(double[] key) {
        this.addText(getClosestWord(key) + " ");
    }

    /**
     * Add the provided text, if the provided value is above threshold. Called
     * by consumers reading data from (e.g) neural networks. If node activation
     * &#62; threshold then display a particular word.
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
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    static XStream getXStream() {
        XStream xstream = TextWorld.getXStream();
        xstream.omitField(DisplayWorld.class, "vectorToTokenDict");
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
        super.readResolve();
        postOpenInit();
        return this;
    }

    /**
     * Initializes persistent data. Initializes Dataset from persistent data.
     */
    public void preSaveInit() {
        persistentData.clear();
        for (DataPoint point : vectorToTokenDict.asArrayList()) {
            persistentData.add((StringDataPoint) point);
        }
    }

    /**
     * Initializes Dataset from persistent data.
     */
    public void postOpenInit() {
        if (persistentData.isEmpty()) {

            return;
        }
        vectorToTokenDict = new NTree(persistentData.get(0).getDimension());
        for (DataPoint point : persistentData) {
            vectorToTokenDict.add(point);
        }
    }

    /**di
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
     * @return the vectorToTokenDict
     */
    public NTree getVectorToTokenDict() {
        return vectorToTokenDict;
    }

    /**
     * Loads a new vector to token dictionary stored as a matrix of Strings.
     * Assumes the length of the first entry is the same as that of all
     * following entries.
     *
     * @param tableData the string matrix encoding the dictionary.
     */
    public void loadVectorToTokenDict(String[][] tableData) {
        vectorToTokenDict = new NTree(tableData[0].length);
        for (int i = 0; i < tableData.length; i++) {
            double[] vector = Utils.parseVectorString(tableData[i][1]);
            addVectorTokenPair(tableData[i][0], vector);
        }
        fireDictionaryChangedEvent();
    }

    /**
     * Add an entry to the vector-token dictionary.
     *
     * @param token the String to add
     * @param vec the vector
     */
    public void addVectorTokenPair(String token, double[] vec) {
        //System.out.println(token + "-->" + Arrays.toString(vec));
        StringDataPoint point = new StringDataPoint(vec, token);
        vectorToTokenDict.add(point);
    }

    /**
     * Associates a datapoint object (basically a double vector that can be
     * processed by the ntree) with a string.
     */
    public class StringDataPoint extends DataPoint {

        /** The associated string. */
        private final String string;

        /**
         * Construct the string data point.
         *
         * @param data the double vector
         * @param string the string
         */
        public StringDataPoint(double[] data, String string) {
            super(data);
            this.string = string;
        }

        /**
         * Get the associated string.
         *
         * @return the string
         */
        public String getString() {
            return string;
        }

    }

}
