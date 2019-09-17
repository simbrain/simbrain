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
import org.simbrain.workspace.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
     * Table model.
     */
    private NumericTable dataTable;

    // Note this class used to contain code for scalar valued column couplings.
    // It would not be hard to get back.
    // See revision 765ac647917d0859e98da47a6a1d0c54b238a7bc

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

    @Override
    public void setWorkspace(Workspace workspace) {
        // Workspace object is not available in the constructor.
        super.setWorkspace(workspace);
        getWorkspace().getCouplingManager().addCouplingListener(new CouplingListenerAdapter() {
            @Override
            public void couplingAdded(Coupling<?> coupling) {
                if (coupling.getConsumer().getBaseObject() == DataWorldComponent.this) {
                    if (coupling.getProducer().getLabelArray() != null) {
                        dataTable.setColumnHeadings(Arrays.asList(coupling.getProducer().getLabelArray()));
                    }
                }
            }
        });
    }

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
                setChangedSinceLastSave(true);
            }
        });
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

    @Consumable()
    public void setRow(double[] values) {
        dataTable.setVectorCurrentRow(values);
    }

    @Producible()
    public double[] getRow() {
        return dataTable.getVectorCurrentRow();
    }

}
