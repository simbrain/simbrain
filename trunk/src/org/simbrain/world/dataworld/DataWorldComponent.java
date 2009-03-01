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
package org.simbrain.world.dataworld;

import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.odorworld.WorldClipboard;

/**
 * <b>DataWorldComponent</b> is a "spreadsheet world" used to send rows of raw data to input nodes.
 */
public class DataWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {
    
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DataWorldComponent.class);

    /** Table model. */
    private DataModel<Double> dataModel;

    /**
     * Returns the data model for this component.
     * 
     * @return The data model for this component.
     */
    public DataModel<Double> getDataModel() {
        return dataModel;
    }

    /**
     * This method is the default constructor.
     */
    public DataWorldComponent(final String name) {
        super(name);
        dataModel = new DataModel<Double>(this);
    }

    /**
     * This method is the default constructor.
     */
    public DataWorldComponent(final String name, int columns, int rows) {
        super(name);
        dataModel = new DataModel<Double>(this, columns, rows);
    }

    
    @SuppressWarnings("unchecked")
    private DataWorldComponent(final String name, final DataModel<?> dataModel) {
        super(name);
        this.dataModel = (DataModel<Double>) dataModel;
        this.dataModel.setParent(this);
    }
    
    /**
     * Recreates an instance of this class from a saved component.
     * 
     * @param input
     * @param name
     * @param format
     * @return
     */
    public static DataWorldComponent open(InputStream input, String name, String format) {
        DataModel<?> model = (DataModel<?>) DataModel.getXStream().fromXML(input);
        return new DataWorldComponent(name,  model);
    }

    @Override
    public void setCurrentDirectory(final String currentDirectory) {        
        super.setCurrentDirectory(currentDirectory);
        DataWorldPreferences.setCurrentDirectory(currentDirectory);
    }

    @Override
    public String getCurrentDirectory() {
        return DataWorldPreferences.getCurrentDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        DataModel.getXStream().toXML(dataModel, output);
    }
    
    @SuppressWarnings("unchecked")
    void wireCouplings(final Collection<? extends Producer> producers) {
        /* Handle Coupling wire-up */
        LOGGER.debug("wiring " + producers.size() + " producers");

       Iterator<? extends Producer> producerIterator = producers.iterator();

        for (Consumer consumer : getConsumers()) {
            if (producerIterator.hasNext()) {
                Coupling<?> coupling = new Coupling(producerIterator.next()
                    .getDefaultProducingAttribute(), consumer.getDefaultConsumingAttribute());
                getWorkspace().addCoupling(coupling);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Consumer> getConsumers() {
        return dataModel.getConsumers();
    }
    
    @Override
    public List<? extends Producer> getProducers() {
        return dataModel.getProducers();
    }

    @Override
    public void update() {
        dataModel.update();
        this.fireUpdateEvent();
    }

    @Override
    public void closing() {
    }
    
    @Override
    public String getXML() {
        return DataModel.getXStream().toXML(dataModel);
    }

}
