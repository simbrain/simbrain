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
import org.simbrain.util.UserParameter;
import org.simbrain.util.XStreamUtils;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.DoubleStream;

/**
 * Model data for pie charts.
 */
public class PieChartModel implements AttributeContainer, EditableObject {

    /**
     * JFreeChart dataset for pie charts.
     */
    final private DefaultPieDataset<String> dataset = new DefaultPieDataset<>();

    @UserParameter(label = "Empty pie threshold", description = "If the total input to the chart is below this number it becomes empty")
    private Double emptyPieThreshold = .0001;

    private Boolean isUninitialized;

    /**
     * Names for the "slices" in the barchart. Can be set via coupling events
     * in {@link PieChartComponent}.
     */
    private String[] sliceNames = {};

    /**
     * Track how many slices there are. If an array with a different number of
     * components is sent to this component, numSlices is updated.
     */
    private int numSlices = 0;

    public PieChartModel() {
        emptyPie();
    }

    public DefaultPieDataset getDataset() {
        return dataset;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        return XStreamUtils.getSimbrainXStream();
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private Object readResolve() {
        return this;
    }

    private void updatePieStatus() {
        if(isUninitialized) {
            dataset.clear();
            isUninitialized = false;
        }
    }

    /**
     * Show this when there is no data or effectively no data.
     */
    private void emptyPie() {
        isUninitialized = true;
        dataset.clear();
        dataset.setValue("Empty pie", 1.0);
    }

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable()
    public void setValues(double[] vector) {
        if (vector.length == 0) {
            throw new IllegalArgumentException("Pie chart supplied with empty array");
        }
        try {
            SwingUtilities.invokeAndWait(() -> {

                updatePieStatus();

                // Take care of size mismatches
                if (vector.length != numSlices) {
                    dataset.clear();
                    numSlices = vector.length;
                }

                double total = DoubleStream.of(vector).map(Math::abs).sum();

                // For minimal activation case just show a single pie slice
                if (total < emptyPieThreshold) {
                    emptyPie();
                    return;
                }
                for (int i = 0; i < vector.length; i++) {
                    if (i < sliceNames.length) {
                        dataset.setValue(sliceNames[i], Math.abs(vector[i] / total));
                    } else {
                        dataset.setValue("" + i, Math.abs(vector[i] / total));
                    }
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSliceNames(String[] names) {
        this.sliceNames = names;
    }

    public String[] getSliceNames() {
        return sliceNames;
    }

    @Override
    public String getId() {
        return "Pie Chart";
    }

    public Double getEmptyPieThreshold() {
        return emptyPieThreshold;
    }

    public void setEmptyPieThreshold(Double emptyPieThreshold) {
        this.emptyPieThreshold = emptyPieThreshold;
    }
}
