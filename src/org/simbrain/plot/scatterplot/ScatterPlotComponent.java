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
package org.simbrain.plot.scatterplot;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Data for a JFreeChart ScatterPlot.
 */
public class ScatterPlotComponent extends WorkspaceComponent<ChartListener> {

    /** Show plot history. */
    private boolean showHistory = false;

    /** Data Model. */
    private ScatterPlotModel model;

    /**
     * Create new PieChart Component.
     *
     * @param name chart name
     */
    public ScatterPlotComponent(final String name) {
        super(name);
        model = new ScatterPlotModel(this);
    }
    
    /**
     * Create new BarChart Component from a specified model.
     * Used in deserializing.
     *
     * @param name chart name
     * @param model chart model
     */
    public ScatterPlotComponent(final String name, final ScatterPlotModel model) {
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
    public ScatterPlotComponent(final String name, final int numDataSources) {
        super(name);
        model = new ScatterPlotModel(this);
        model.addDataSources(numDataSources);
    }

    /**
     * @return the model.
     */
    public ScatterPlotModel getModel() {
        return model;
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    public static ScatterPlotComponent open(final InputStream input,
            final String name, final String format) {
        ScatterPlotModel dataModel = (ScatterPlotModel) ScatterPlotModel.getXStream().fromXML(input);
        return new ScatterPlotComponent(name, dataModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        ScatterPlotModel.getXStream().toXML(model, output);
    }

    /**
     * @return the show history.
     */
    public boolean isShowHistory() {
        return showHistory;
    }

    /**
     * Boolean show history.
     * @param value show history
     */
    public void setShowHistory(final boolean value) {
        showHistory = value;
    }

    /**
     * Update chart settings. Called, e.g., when things are modified using a
     * dialog.
     */
    public void updateSettings() {
        for (ChartListener listener : this.getListeners()) {
            listener.chartSettingsUpdated();
        }
    }

    @Override
    public void update() {

        if (!showHistory) {
            // Constantly erase. How is performance for this version?
            for (ScatterPlotConsumer consumer : model.getConsumers()) {
                model.getDataset().getSeries(consumer.getIndex()).clear();
                model.getDataset().getSeries(consumer.getIndex()).add(consumer.getX(),
                        consumer.getY());
                // System.out.println("--[" + consumer.getIndex() + "]:" +
                // dataset.getSeries(consumer.getIndex()).getItemCount());
            }
        } else {

            // THE VERSION BELOW KEEPS A HISTORY. THERE IS NO "HOT" POINT
            for (ScatterPlotConsumer consumer : model.getConsumers()) {
                model.getDataset().getSeries(consumer.getIndex()).add(consumer.getX(),
                        consumer.getY());
            }
        }
    }

    @Override
    public String getCurrentDirectory() {
        return "." + System.getProperty("file.separator");

    }
    
    @Override
    public String getXML() {
        return ScatterPlotModel.getXStream().toXML(model);
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
}
