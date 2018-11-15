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

import org.simbrain.plot.ChartCouplingListener;
import org.simbrain.plot.ChartDataSource;
import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data for a JFreeChart bar chart.
 */
public class BarChartComponent extends WorkspaceComponent {

    /**
     * Data model.
     */
    private BarChartModel model;

    /**
     * Create new BarChart Component.
     *
     * @param name chart name
     */
    public BarChartComponent(String name) {
        super(name);
        model = new BarChartModel();
        initModelListener();
    }

    /**
     * Create new BarChart Component from a specified model. Used in
     * deserializing.
     *
     * @param name  chart name
     * @param model chart model
     */
    public BarChartComponent(String name, BarChartModel model) {
        super(name);
        this.model = model;
        initModelListener();
    }

    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name    name of component
     * @param numBars number of bars in plot
     */
    public BarChartComponent(String name, int numBars) {
        super(name);
        model = new BarChartModel();
        model.addBars(numBars);
        initModelListener();
    }

    /**
     * Add chart listener to model.
     */
    private void initModelListener() {
        model.addListener(new ChartListener() {
            public void dataSourceAdded(ChartDataSource source) {
                fireAttributeContainerAdded(source);
            }

            public void dataSourceRemoved(ChartDataSource source) {
                fireAttributeContainerRemoved(source);
            }
        });
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        // This is a bit of a hack because the workspace is not available in the constructor.
        super.setWorkspace(workspace);
        workspace.addCouplingListener(new ChartCouplingListener(getWorkspace(), model, "Bar"));
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        if (objectKey.equals(model.getId())) {
            return model;
        } else {
            Optional<BarChartModel.Bar> bar = model.getBar(objectKey);
            if (bar.isPresent()) {
                return bar.get();
            }
        }
        return null;
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
     *
     * @param input  stream
     * @param name   name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static BarChartComponent open(InputStream input, String name, String format) {
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
    public String getXML() {
        return BarChartModel.getXStream().toXML(model);
    }

    @Override
    public List<Object> getAttributeContainers() {
        List<Object> models = new ArrayList<Object>();
        models.add(model);
        models.addAll(model.getBars());
        return models;
    }
}
