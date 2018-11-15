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

import org.simbrain.plot.ChartDataSource;
import org.simbrain.plot.ChartListener;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Data for a JFreeChart ScatterPlot.
 */
public class ScatterPlotComponent extends WorkspaceComponent {

    /**
     * Data Model.
     */
    private ScatterPlotModel model;

    /**
     * Objects which can be used to set the scatter plot.
     */
    private List<ScatterPlotSetter> setterList = new ArrayList<ScatterPlotSetter>();

    /**
     * Create new ScatterChart Component.
     *
     * @param name chart name
     */
    public ScatterPlotComponent(final String name) {
        super(name);
        model = new ScatterPlotModel();
        model.defaultInit();
        initModelListener();
    }

    /**
     * Create new Scatter Plot Component from a specified model.
     * <p>
     * Used in deserializing.
     *
     * @param name  chart name
     * @param model chart model
     */
    public ScatterPlotComponent(final String name, final ScatterPlotModel model) {
        super(name);
        this.model = model;
        initModelListener();
    }

    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name           name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public ScatterPlotComponent(final String name, final int numDataSources) {
        super(name);
        model = new ScatterPlotModel();
        model.addDataSources(numDataSources);
        initModelListener();
    }

    @Override
    public List<Object> getAttributeContainers() {
        List<Object> models = new ArrayList<Object>();
        models.add(model);
        models.addAll(setterList);
        return models;
    }

    /**
     * Return the setter with specified index, or null if none found.
     *
     * @param i index of setter
     * @return the setter object
     */
    public ScatterPlotSetter getSetter(int i) {
        for (ScatterPlotSetter setter : setterList) {
            if (setter.getIndex() == i) {
                return setter;
            }
        }
        return null;
    }

    /**
     * Add a setter with the specified index.
     *
     * @param i index of setter
     */
    public void addSetter(int i) {
        for (ScatterPlotSetter setter : setterList) {
            if (setter.getIndex() == i) {
                return;
            }
        }
        setterList.add(new ScatterPlotSetter(i));
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

    public static ScatterPlotComponent open(final InputStream input, final String name, final String format) {
        ScatterPlotModel dataModel = (ScatterPlotModel) ScatterPlotModel.getXStream().fromXML(input);
        return new ScatterPlotComponent(name, dataModel);
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        try {
            int i = Integer.parseInt(objectKey);
            ScatterPlotSetter setter = getSetter(i);
            return setter;
        } catch (NumberFormatException e) {
            return null; // the supplied string was not an integer
        }
    }

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
        // Constantly erase. How is performance for this version?
        for (ScatterPlotSetter setter : setterList) {
            Integer index = setter.getIndex() + 1;
            if (!model.isShowHistory()) {
                model.getDataset().getSeries(index).clear();
            }
            model.getDataset().getSeries(index).add(setter.getX(), setter.getY());
        }
    }

    /**
     * Object which sets a value of one slice of a scatter chart.
     */
    public class ScatterPlotSetter {

        /**
         * Index.
         */
        private Integer index;

        /**
         * X Value.
         */
        private double xval;

        /**
         * Y Value.
         */
        private double yval;

        /**
         * Construct a setter object.
         *
         * @param index index of the bar to set
         */
        public ScatterPlotSetter(final Integer index) {
            this.index = index;
        }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }

        /**
         * @return the xval
         */
        @Producible
        public double getX() {
            return xval;
        }

        /**
         * @param xval the xval to set
         */
        @Consumable
        public void setX(double xval) {
            this.xval = xval;
        }

        /**
         * @return the yval
         */
        @Producible
        public double getY() {
            return yval;
        }

        /**
         * @param yval the yval to set
         */
        @Consumable
        public void setY(double yval) {
            this.yval = yval;
        }
    }

}
