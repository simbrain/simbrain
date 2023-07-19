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
package org.simbrain.plot.timeseries;

import com.thoughtworks.xstream.XStream;
import org.simbrain.plot.XYSeriesConverter;
import org.simbrain.util.DoubleArrayConverter;
import org.simbrain.util.XStreamUtils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents time series data.
 */
public class TimeSeriesPlotComponent extends WorkspaceComponent {

    /**
     * The data model.
     */
    private final TimeSeriesModel model;

    /**
     * Create new time series plot component.
     *
     * @param name name
     */
    public TimeSeriesPlotComponent(String name) {
        super(name);
        model = new TimeSeriesModel(() -> getWorkspace().getTime());
    }

    /**
     * Creates a new time series component from a specified model. Used in
     * deserializing.
     *
     * @param name  chart name
     * @param model chart model
     */
    public TimeSeriesPlotComponent(String name, TimeSeriesModel model) {
        super(name);
        this.model = model;
        model.setTimeSupplier(() -> getWorkspace().getTime());
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        // Workspace object is not available in the constructor.
        super.setWorkspace(workspace);

        getWorkspace().getCouplingManager().getEvents().getCouplingAdded().on(c -> {
            // A new array coupling is being added to this time series
            if (c.getConsumer().getBaseObject() == model) {

                // Initialize series with provided names, e.g neuron labels
                model.initializeArrayMode(c.getProducer().getLabelArray());
            }
        });

        model.getEvents().getChangeArrayMode().on(() -> {
            // Array mode has been changed
            if (model.isArrayMode()) {
                // Changed from scalar to array mode
                // No action
            } else {
                // Changed from array to scalar mode
                fireAttributeContainerRemoved(model);
            }
        });

        // A new scalar time series has been added
        model.getEvents().getScalarTimeSeriesAdded().on(this::fireAttributeContainerAdded);

        // A scalar time series has been removed
        model.getEvents().getScalarTimeSeriesRemoved().on(this::fireAttributeContainerRemoved);
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        if (model.isArrayMode()) {
            containers.add(model);
        } else {
            containers.addAll(model.getTimeSeriesList());
        }
        return containers;
    }

    public TimeSeriesModel getModel() {
        return model;
    }

    /**
     * Opens a saved time series plot.
     *
     * @param input  stream
     * @param name   name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static TimeSeriesPlotComponent open(final InputStream input, final String name, final String format) {
        TimeSeriesModel dataModel = (TimeSeriesModel) getTimeSeriesXStream().fromXML(input);
        return new TimeSeriesPlotComponent(name, dataModel);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        getTimeSeriesXStream().toXML(model, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public String getXML() {
        return getTimeSeriesXStream().toXML(model);
    }

    public static XStream getTimeSeriesXStream() {
        var xstream = XStreamUtils.getSimbrainXStream();
        xstream.registerConverter(new DoubleArrayConverter());
        xstream.registerConverter(new XYSeriesConverter());
        return xstream;
    }


}
