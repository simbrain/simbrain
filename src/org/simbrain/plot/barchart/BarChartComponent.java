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
package org.simbrain.plot.barchart;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * Data for a JFreeChart pie chart.
 */
public class BarChartComponent extends WorkspaceComponent<ChartListener> {

    /** Data model. */
    private BarChartModel model;

    /**
     * Create new BarChart Component.
     *
     * @param name chart name
     */
    public BarChartComponent(final String name) {
        super(name);
        model = new BarChartModel(this);
    }

    /**
     * Create new BarChart Component from a specified model.
     * Used in deserializing.
     *
     * @param name chart name
     * @param model chart model
     */
    public BarChartComponent(final String name, final BarChartModel model) {
        super(name);
        this.model = model;
        this.model.setParent(this);
    }

    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public BarChartComponent(final String name, final int numDataSources) {
        super(name);
        model = new BarChartModel(this);
        model.addDataSources(numDataSources);
    }

    /**
     * Returns model.
     *
     * @return the model.
     */
    public BarChartModel getModel() {
        return model;
    }

    /**
     * Opens a saved bar chart.
     * @param input stream
     * @param name name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static BarChartComponent open(final InputStream input,
            final String name, final String format) {
        BarChartModel dataModel = (BarChartModel) BarChartModel.getXStream().fromXML(input);
        return new BarChartComponent(name, dataModel);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        BarChartModel.getXStream().toXML(model, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public void closing() {
    }

    @Override
    public void update() {
        for (BarChartConsumer consumer : model.getConsumers()) {
            model.getDataset().setValue(consumer.getValue(), new Integer(1), consumer.getIndex());
        }
    }
    
    /**
     * Update chart settings.  Called, e.g., when things are modified using a dialog.
     */
    public void updateSettings() {
    	for (ChartListener listener : this.getListeners()) {
    		listener.chartSettingsUpdated();
    	}
    }

    @Override
    public String getCurrentDirectory() {
        return "." + System.getProperty("file.separator");

    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) {
        super.setCurrentDirectory(currentDirectory);
    }
    
    @Override
    public List<? extends Consumer> getConsumers() {
        return (List<? extends Consumer>) model.getConsumers();
    }
    
    @Override
    public List<? extends Producer> getProducers() {
        return Collections.<Producer>emptyList();
    }

    @Override
    public String getXML() {
        return BarChartModel.getXStream().toXML(model);
    }
}
