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

import org.apache.log4j.Logger;
import org.simbrain.util.table.NumericTable;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.workspace.WorkspaceComponent;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>DataWorldComponent</b> is a data table which other Simbrain components can
 * use.
 */
public class DataWorldComponent extends WorkspaceComponent implements AttributeContainer {

    /**
     * The static logger for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(DataWorldComponent.class);

    /**
     * Recreates an instance of this class from a saved component.
     *
     * @param input  input stream
     * @param name   name of file
     * @param format format of file
     * @return new component
     */
    public static DataWorldComponent open(InputStream input, String name, String format) {
        // TODO: Use format to determine how to open this.
        NumericTable model = (NumericTable) NumericTable.getXStream().fromXML(input);
        return DataWorldComponent.createDataWorld(model, name);
    }

    /**
     * Create the data world component.
     *
     * @param data the underlying data
     * @param name the title for this world
     * @return the created component.
     */
    public static DataWorldComponent createDataWorld(NumericTable data, String name) {
        DataWorldComponent component = new DataWorldComponent(data, name);
        component.initializeModelListener();
        return component;
    }

    /**
     * Table model.
     */
    private NumericTable dataTable;

    /**
     * List of columns in the table.
     */
    private List<TableColumn> columns = new ArrayList<>();

    /**
     * Construct data world from a model. Used (for example) in deserializing.
     *
     * @param dataTable the underlying data
     * @param name      the name of this component
     */
    private DataWorldComponent(NumericTable dataTable, String name) {
        super(name);
        this.dataTable = dataTable;
    }

    /**
     * Initialize consumers and producers.
     */
    private void initializeModelListener() {
        dataTable.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent evt) {
                // TODO. Below is intended to capture column inserts and
                // deletes, though it captures other stuff too.
                if (dataTable.getColumnCount() != columns.size()) {
                    updateColumns();
                }
                setChangedSinceLastSave(true);
            }
        });
    }

    private void updateColumns() {
        while (dataTable.getColumnCount() < columns.size()) {
            TableColumn column = columns.remove(columns.size() - 1);
            fireAttributeContainerRemoved(column);
        }
        while (dataTable.getColumnCount() > columns.size()) {
            TableColumn column = new TableColumn(columns.size());
            columns.add(column);
        }
    }

    /**
     * Return an id for the table.
     */
    public String getId() {
        return "DataTable";
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> models = new ArrayList<>();
        models.add(this);
        models.addAll(columns);
        return models;
    }

    /**
     * Returns the data model for this component.
     */
    public NumericTable getDataModel() {
        return dataTable;
    }

    @Override
    public void save(final OutputStream output, final String format) {
        NumericTable.getXStream().toXML(dataTable, output);
    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        if (objectKey.equals("DataTable")) {
            return this;
        } else if (objectKey.contains("Column")) {
            int index = Integer.parseInt(objectKey.substring(6));
            return columns.get(index);
        } else {
            return null;
        }
    }

    @Override
    public void update() {
        dataTable.updateCurrentRow();
        this.fireUpdateEvent();
    }

    @Override
    public void closing() {
    }

    @Override
    public String getXML() {
        return NumericTable.getXStream().toXML(dataTable);
    }

    @Consumable(idMethod = "getId")
    public void setRow(double[] values) {
        dataTable.setVectorCurrentRow(values);
    }

    @Producible(idMethod = "getId")
    public double[] getRow() {
        return dataTable.getVectorCurrentRow();
    }

    /**
     * TableColumn writes to a specific column of the outer DataWorld.
     */
    public class TableColumn implements AttributeContainer {

        /**
         * The index of the column.
         */
        private int index;

        /**
         * Construct a TableColumn.
         *
         * @param index index of the column to set.
         */
        public TableColumn(int index) {
            this.index = index;
        }

        /**
         * Return the id of the column.
         */
        public String getId() {
            return "Column" + index;
        }

        /**
         * Return the index of the column.
         */
        public int getIndex() {
            return index;
        }

        /**
         * Return the value of the current cell of the column.
         */
        @Producible(idMethod = "getId")
        public double getValue() {
            return dataTable.getValueCurrentRow(index);
        }

        /**
         * Set the value of the current cell of the column.
         */
        @Consumable(idMethod = "getId")
        public void setValue(double value) {
            dataTable.setValueCurrentRow(index, value);
        }

    }
}
