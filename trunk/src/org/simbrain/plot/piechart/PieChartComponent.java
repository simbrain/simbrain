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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * Daa for a JFreeChart pie chart.
 */
public class PieChartComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Data model. */
    private PieChartModel model;

    /**
     * Create new PieChart Component.
     * @param name of chart
     */
    public PieChartComponent(final String name) {
        super(name);
        model = new PieChartModel(this);
    }
    
    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name name of component
     * @param model to use for the plot
     */
    public PieChartComponent(final String name, final PieChartModel model) {
        super(name);
        this.model = model;
        model.setParent(this);
    }


    /**
     * Streams file data for opening saved charts.
     * @param input stream
     * @param name file name
     * @param format format
     * @return component to be opened
     */
    public static PieChartComponent open(final InputStream input,
            final String name, final String format) {
        PieChartModel dataModel = (PieChartModel) PieChartModel.getXStream().fromXML(input);
        return new PieChartComponent(name, dataModel);
    }

    /**
     * @return the model.
     */
    public PieChartModel getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        PieChartModel.getXStream().toXML(model, output);
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

    @Override
    public void update() {
        double total = 0;
        for (PieDataConsumer consumer : model.getConsumers()) {
            total+=consumer.getValue();
        }
        if (total == 0) return; // TODO: Do something more sensible for this case
        for (PieDataConsumer consumer : model.getConsumers()) {
            model.getDataset().setValue(consumer.getIndex(), consumer.getValue() / total);
        }
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
        return PieChartModel.getXStream().toXML(model);
    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) {
        super.setCurrentDirectory(currentDirectory);
    }

    @Override
    public String getCurrentDirectory() {
        return "." + System.getProperty("file.separator");

    }
}
