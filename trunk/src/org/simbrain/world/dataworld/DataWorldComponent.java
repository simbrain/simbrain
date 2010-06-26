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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.simbrain.util.table.SimbrainDataTable;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;

import com.Ostermiller.util.CSVParser;

/**
 * <b>DataWorldComponent</b> is a data table which other Simbrain components can
 * use.
 */
public class DataWorldComponent extends WorkspaceComponent {

    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DataWorldComponent.class);

    /** Table model. */
    private SimbrainDataTable dataModel; 

    /**
     * This method is the default constructor.
     */
    public DataWorldComponent(final String name) {
        super(name);
        dataModel = new SimbrainDataTable();
        init();
    }

    /**
     * Construct a dataworld with a specified name, columns, and rows.
     *
     * @param name name of network
     * @param rows number of rows
     * @param columns number of columns
     */
    public DataWorldComponent(final String name, int rows, int columns) {
        super(name);
        dataModel = new SimbrainDataTable(rows, columns);
        init();
    }

    @SuppressWarnings("unchecked")
    private DataWorldComponent(final String name, final SimbrainDataTable dataModel) {
        super(name);
        this.dataModel = (SimbrainDataTable) dataModel;
        init();
    }

    /**
     * Initialize consumers and producers.
     */
    private void init() {
        for (int i = 0; i < dataModel.getColumnCount(); i++) {
            addConsumer(new ConsumingColumn<Double>(this, i));
            addProducer(new ProducingColumn<Double>(this, i));
        }

        dataModel.addListener(listener);
    }

    /** Listener. */
    private final SimbrainDataTable.TableListener listener = new SimbrainDataTable.TableListener() {

        public void columnAdded(int column) {
            int index = dataModel.getColumnCount() - 1;
            addConsumer(new ConsumingColumn<Double>(DataWorldComponent.this, index));
            addProducer(new ProducingColumn<Double>(DataWorldComponent.this, index));
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void columnRemoved(int column) {
            // TODO: This stuff is broken but waiting to refactor attribute stuff anyway
            // int index = dataModel.getColumnCount();
            // getProducers().remove(index);
            // getProducers().remove(index);
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void dataChanged() {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void itemChanged(int row, int column) {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void rowAdded(int row) {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void rowRemoved(int row) {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void structureChanged() {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

    };

    /**
     * Recreates an instance of this class from a saved component.
     *
     * @param input input stream
     * @param name name of file
     * @param format format of file
     * @return new component
     */
    public static DataWorldComponent open(InputStream input, String name, String format) {
        // TODO: Use format  to determine how to open this.
        SimbrainDataTable model = (SimbrainDataTable) SimbrainDataTable.getXStream().fromXML(input);
        return new  DataWorldComponent(name,  model);
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
     * Returns the data model for this component.
     *
     * @return The data model for this component.
     */
    public SimbrainDataTable getDataModel() {
        return dataModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        SimbrainDataTable.getXStream().toXML(dataModel, output);
    }

    @SuppressWarnings("unchecked")
    void wireCouplings(final Collection<? extends Producer> producers) {
        /* Handle Coupling wire-up */
        LOGGER.debug("wiring " + getProducers().size() + " producers");

       Iterator<? extends Producer> producerIterator = getProducers().iterator();

        for (Consumer consumer : getConsumers()) {
            if (producerIterator.hasNext()) {
                Coupling<?> coupling = new Coupling(producerIterator.next()
                    .getProducingAttributes().get(0), consumer.getConsumingAttributes().get(0));
                getWorkspace().addCoupling(coupling);
            }
        }
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
        return SimbrainDataTable.getXStream().toXML(dataModel);
    }

}
