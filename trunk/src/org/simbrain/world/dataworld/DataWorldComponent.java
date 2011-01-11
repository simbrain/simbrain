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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.util.table.SimbrainDataTable;
import org.simbrain.util.table.SimbrainTableListener;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;

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
     * Objects which can be used to get or set column values in the current row.
     */
    private List<ColumnAttribute> consumerList = new ArrayList<ColumnAttribute>();

    /**
     * Objects which can be used to get or set column values in the current row.
     */
    private List<ColumnAttribute> producerList = new ArrayList<ColumnAttribute>();

    /** Producing column attribute type. */
    private final AttributeType producingColumnType = new AttributeType(this, "Column", "getValue", double.class, true);

    /** Consuming column attribute type. */
    private final AttributeType consumingColumnType = new AttributeType(this, "Column", "setValue", double.class, true);

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

    /**
     * Construct a data world component from data.
     * @param name name of component
     * @param inputData data data to use to initialize the model
     */
    public DataWorldComponent(String name, double[][] data) {
        this(name);
        dataModel.setData(data);
        init();
    }

    /**
     * Construct data world from a model. Used (for example) in deserializing.
     *
     * @param name name of component
     * @param dataModel the model
     */
    @SuppressWarnings("unchecked")
    public DataWorldComponent(final String name, final SimbrainDataTable dataModel) {
        super(name);
        this.dataModel = (SimbrainDataTable) dataModel;
        init();
    }

    /**
     * Initialize consumers and producers.
     */
    private void init() {

        getProducerTypes().add(producingColumnType);
        getConsumerTypes().add(consumingColumnType);

        for (int i = 0; i < dataModel.getColumnCount(); i++) {
            addColumnAttribute(i, consumerList);
            addColumnAttribute(i, producerList);
        }

        dataModel.addListener(listener);
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        if (consumingColumnType.isVisible()) {
            for (ColumnAttribute attribute : consumerList) {
                String description = consumingColumnType.getSimpleDescription("Column " + (attribute.getIndex() + 1));
                PotentialConsumer consumer = getAttributeManager().createPotentialConsumer(attribute, consumingColumnType, description);
                returnList.add(consumer);
            }
        }
        return returnList;
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {
        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();
        if (producingColumnType.isVisible()) {
            for (ColumnAttribute attribute : consumerList) {
                String description = producingColumnType.getDescription("Column_" + (attribute.getIndex() + 1));
                PotentialProducer producer = getAttributeManager().createPotentialProducer(attribute, producingColumnType, description);
                returnList.add(producer);
            }
        }
        return returnList;
    }


    /** Listener. */
    private final SimbrainTableListener listener = new SimbrainTableListener() {

        public void columnAdded(int column) {
            int index = dataModel.getColumnCount() - 1;
            addColumnAttribute(index, consumerList);
            addColumnAttribute(index, producerList);
            DataWorldComponent.this.firePotentialAttributesChanged();
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void columnRemoved(int index) {
            ColumnAttribute producer = getColumnAttribute(index, producerList);
            ColumnAttribute consumer = getColumnAttribute(index, consumerList);
            DataWorldComponent.this.fireAttributeObjectRemoved(producer);
            DataWorldComponent.this.fireAttributeObjectRemoved(consumer);
            consumerList.remove(producer);
            producerList.remove(consumer);
            DataWorldComponent.this.firePotentialAttributesChanged();
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void tableDataChanged() {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void cellDataChanged(int row, int column) {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void rowAdded(int row) {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void rowRemoved(int row) {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

        public void tableStructureChanged() {
            DataWorldComponent.this.setChangedSinceLastSave(true);
        }

    };

    /**
     * Return the column producer with specified index, or null if none found.
     *
     * @param i index of setter
     * @return the column attribute
     */
    public ColumnAttribute getColumnProducer(int i) {
        if (i < producerList.size()) {
            return producerList.get(i); 
        }
        return null;
    }

    /**
     * Return the column consumer with specified index, or null if none found.
     *
     * @param i index of setter
     * @return the column attribute
     */
    public ColumnAttribute getColumnConsumer(int i) {
        if (i < consumerList.size()) {
            return consumerList.get(i); 
        }
        return null;
    }

    /**
     * Return the column attribute with specified index, or null if none found.
     *
     * @param i index of setter
     * @param list list to check
     * @return the column attribute
     */
    private ColumnAttribute getColumnAttribute(int i, List<ColumnAttribute> list) {
        for (ColumnAttribute attribute : list) {
            if (attribute.getIndex() == i) {
                return attribute;
            }
        }
        return null;
    }

    /**
     * Add a setter with the specified index.
     *
     * @param i index of setter
     * @param list list to check
     */
    private void addColumnAttribute(int i, List<ColumnAttribute> list) {
        for (ColumnAttribute attribute : list) {
            if (attribute.getIndex() == i) {
                return;
            }
        }
        list.add(new ColumnAttribute(i));
    }

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

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof ColumnAttribute) {
            if (producerList.contains(object)) {
                return "producerList:" + ((ColumnAttribute) object).getIndex();
            } else if (consumerList.contains(object)) {
                return "consumerList:" + ((ColumnAttribute) object).getIndex();
            }
        }
        return null;
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        try {
            String producerOrConsumer = objectKey.split(":")[0];
            String index = objectKey.split(":")[1];
            int i = Integer.parseInt(index);
            ColumnAttribute attribute = null;
            if (producerOrConsumer.equalsIgnoreCase("producerList")) {
                attribute = getColumnAttribute(i, producerList);
            } else if (producerOrConsumer.equalsIgnoreCase("consumerList")) {
                attribute = getColumnAttribute(i, consumerList);
            }
            return  attribute;
        } catch (NumberFormatException e) {
            return null; // the supplied string was not an integer
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

    /**
     * Object which sets a value of one bar in a bar chart.
     */
    public class ColumnAttribute {

        /** Index. */
        private int index;

        /**
         * Construct a column attribute object.
         *
         * @param index index of the bar to set
         */
        public ColumnAttribute(final int index) {
            this.index = index;
        }

        /**
         * Set the value.
         *
         * @param val value for the bar
         */
        public void setValue(final double val) {
            dataModel.setValueCurrentRow(index, val);
        }

        /**
         * Returns the value of the current row at the specified index.
         *
         * @return the value
         */
        public double getValue() {
            return dataModel.getValueCurrentRow(index);
        }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }

    }

    /**
     * @return the producingColumnType
     */
    public AttributeType getProducingColumnType() {
        return producingColumnType;
    }

    /**
     * @return the consumingColumnType
     */
    public AttributeType getConsumingColumnType() {
        return consumingColumnType;
    }

}
