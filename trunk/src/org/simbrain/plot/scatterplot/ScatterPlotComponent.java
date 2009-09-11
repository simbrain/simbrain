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

import java.awt.EventQueue;
import java.io.InputStream;
import java.io.OutputStream;

import org.simbrain.plot.ChartListener;
import org.simbrain.plot.barchart.BarChartConsumer;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Data for a JFreeChart ScatterPlot.
 */
public class ScatterPlotComponent extends WorkspaceComponent {

    /** Data Model. */
    private ScatterPlotModel model;

    /**
     * Create new PieChart Component.
     *
     * @param name chart name
     */
    public ScatterPlotComponent(final String name) {
        super(name);
        model = new ScatterPlotModel();
        addListener();
        model.defaultInit();
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
        initializeAttributes();
        addListener();
    }

    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public ScatterPlotComponent(final String name, final int numDataSources) {
        super(name);
        model = new ScatterPlotModel();
        addListener();
        model.addDataSources(numDataSources);
    }

    /**
     * Initialize consuming attributes.
     */
    private void initializeAttributes() {
        this.getConsumers().clear();
        for (int i = 0; i < model.getDataset().getSeriesCount(); i++) {
            addConsumer(new ScatterPlotConsumer(this, i));
        }
    }

    /**
     * Add chart listener to model.
     */
    private void addListener() {
        
        model.addListener(new ChartListener() {

            /**
             * {@inheritDoc}
             */
            public void dataSourceAdded(final int index) {
                ScatterPlotConsumer newAttribute = new ScatterPlotConsumer(ScatterPlotComponent.this, index);
                addConsumer(newAttribute);
            }

            /**
             * {@inheritDoc}
             */
            public void dataSourceRemoved(final int index) {
                ScatterPlotConsumer toBeRemoved = (ScatterPlotConsumer) getConsumers().get(index);
                removeConsumer(toBeRemoved);
            }
            
        });
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

    @Override
    public String getXML() {
        return ScatterPlotModel.getXStream().toXML(model);
    }

    @Override
    public void update() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Constantly erase. How is performance for this version?
                for (Consumer consumer : getConsumers()) {
                    ScatterPlotConsumer s_consumer = (ScatterPlotConsumer) consumer;
                    Integer index = s_consumer.getIndex();
                    if (!model.isShowHistory()) {
                        getModel().getDataset().getSeries(index).clear();
                    }
                    model.getDataset().getSeries(index).add(s_consumer.getX(),
                            s_consumer.getY());
                    // System.out.println("[" + consumer.getIndex() + "]:" +
                    // dataset.getSeries(consumer.getIndex()).getItemCount());
                }
            }
        });
    }

}
