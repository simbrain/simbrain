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
package org.simbrain.plot.projection;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectPCA;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Data for a JFreeChart ScatterPlot.
 * 
 * TODO:
 *  Color last point (override paint and just get last one?)
 *  Basicaly get all the stuff from the old gui here!
 *  Tooltips
 *  Add ability to plot multiple HDV's at once?
 *  Maybe ability to connect them with lines
 */
public class ProjectionComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Consumer list. */
    private ArrayList<ProjectionConsumer> consumers= new ArrayList<ProjectionConsumer>();
    
    /** Scatter Plot Data. */
    private XYSeriesCollection dataset;
    
    /** Default number of sources. */
    private final int DEFAULT_NUMBER_OF_SOURCES = 25; // This is the dimensionality of the hi D dataset
    
    /** High Dimensional Projection. */
    private Gauge gauge = new Gauge();

    /**
     * Create new PieChart Component.
     */
    public ProjectionComponent(final String name) {
        super(name);
        init(DEFAULT_NUMBER_OF_SOURCES);
    }
    
    /**
     * Initializes a JFreeChart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public ProjectionComponent(final String name, final int numDataSources) {
        super(name);
        init(numDataSources);
    }
    
    /**
     * Initialize plot.
     *
     * @param numSources number of data sources
     */
    private void init(final int numSources) {
        this.setAttributeListingStyle(AttributeListingStyle.TOTAL);
        dataset = new XYSeriesCollection();
        dataset.addSeries(new XYSeries("Data"));
        for (int i = 0; i < numSources; i++) {
            addSource();
        }
        gauge.init(numSources);
    }

    /**
     * Adds a source to dataset.
     */
    public void addSource() {
        int currentSize = consumers.size() + 1;
        ProjectionConsumer newAttribute = new ProjectionConsumer(this,
                "Dimension" + currentSize, currentSize);
        consumers.add(newAttribute);
        gauge.init(currentSize);
    }

    /**
     * Removes a source from the dataset.
     */
    public void removeSource() {
        int currentSize = consumers.size() - 1;

        if (currentSize > 0) {
//            dataset.removeSeries(lastSeriesIndex);
            consumers.remove(currentSize);
            gauge.init(currentSize);
        }
    }
 
    /**
     * Return JFreeChart xy dataset.
     * 
     * @return dataset
     */
    public XYDataset getDataset() { 
        return dataset;
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(WorkspaceComponent.class, "component");
        xstream.omitField(WorkspaceComponent.class, "listenerList");
        xstream.omitField(WorkspaceComponent.class, "workspace");
        xstream.omitField(WorkspaceComponent.class, "logger");
        xstream.omitField(ProjectPCA.class, "logger");

        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized.
     * See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     * 
     * @return Initialized object.
     */
    private Object readResolve() {
        System.out.println("ReadResolve.");
        return this;
    }
       
    /**
     * {@inheritDoc}
     */
    public static ProjectionComponent open(InputStream input, final String name, final String format) {
        return (ProjectionComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(this, output);
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
    public Collection<ProjectionConsumer> getConsumers() {
        return consumers;
    }

    @Override
    public void update() {

        double[] temp = new double[getConsumers().size()];
        int i = 0;
        for (ProjectionConsumer consumer : getConsumers()) {
            temp[i++] = consumer.getValue();
        }
        gauge.addDatapoint(temp);
        
        double[] lastPoint = gauge.getProjectedPoint(gauge.getSize()-1);
        if (lastPoint != null) {
            dataset.getSeries(0).add(lastPoint[0], lastPoint[1]);
        }

    }
    
    public Gauge getGauge() {
        return gauge;
    }
    
    public void clearData() {
        dataset.getSeries(0).clear();
        gauge.reset();
    }
    
    public void changeProjection() {
        gauge.getCurrentProjector().project(); // Should this have happened already?
        resetChartDataset();
    }
    
    public void resetChartDataset() {
        dataset.getSeries(0).clear();
        for (int i = 0; i < gauge.getSize(); i++) {
            double[] point = gauge.getProjectedPoint(i);
            if(point != null) {
                dataset.getSeries(0).add(point[0], point[1]);
            }
        }
    }


    @Override
    public String getCurrentDirectory() {
        return "." + System.getProperty("file.separator");

    }
    
    @Override
    public String getXML() {
        return ProjectionComponent.getXStream().toXML(this);
    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) {
        super.setCurrentDirectory(currentDirectory);
    }

}
