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
package org.simbrain.plot.piechart;

import com.thoughtworks.xstream.XStream;
import org.jfree.data.general.DefaultPieDataset;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import java.util.stream.DoubleStream;

/**
 * Model data for pie charts.
 */
public class PieChartModel implements AttributeContainer {

    /**
     * JFreeChart dataset for pie charts.
     */
    private DefaultPieDataset dataset = new DefaultPieDataset();

    /**
     * Names for the "slices" in the barchart.  Set via coupling events.
     */
    private String[] sliceNames = {};

    /**
     * Track how many slices there are.  If an array with a different number of
     * components is sent to this component, numSlices is updated.
     */
    private int numSlices = 0;

    /**
     * Default constructor.
     */
    public PieChartModel() {
    }

    /**
     * @return the data set.
     */
    public DefaultPieDataset getDataset() {
        return dataset;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = Utils.getSimbrainXStream();
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        return this;
    }

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable()
    public void setValues(double[] vector) {

        // Take care of size mismatches
        if (vector.length != numSlices) {
            dataset.clear();
            numSlices = vector.length;
        }

        // Write the data
        double total = DoubleStream.of(vector).sum() + .001;
        for (int i = 0; i < vector.length; i++) {
            if (i < sliceNames.length) {
                dataset.setValue(sliceNames[i], Math.abs(vector[i] / total));
            } else {
                dataset.setValue("" + i, Math.abs(vector[i] / total));
            }
        }
    }

    public void setSliceNames(String[] names) {
        this.sliceNames = names;
    }


}
