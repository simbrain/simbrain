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

import org.simbrain.util.Utils;
import org.simbrain.workspace.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Pie chart component.
 */
public class PieChartComponent extends WorkspaceComponent {

    /**
     * Data model.
     */
    private PieChartModel model;

    /**
     * Create new PieChart Component.
     *
     * @param name of chart
     */
    public PieChartComponent(final String name) {
        super(name);
        model = new PieChartModel();
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        // This is a bit of a hack because the workspace is not available in the constructor.
        super.setWorkspace(workspace);
        getWorkspace().getCouplingManager().addCouplingListener(new CouplingListenerAdapter() {
            @Override
            public void couplingAdded(Coupling<?> coupling) {
                if (coupling.getConsumer().getBaseObject() == model) {
                    model.setSliceNames(coupling.getProducer().getLabelArray());
                }
            }
        });
    }

    /**
     * Initializes a pie chart with a model.
     * <p>
     * Used in deserializing.
     *
     * @param name  name of component
     * @param model to use for the plot
     */
    public PieChartComponent(final String name, final PieChartModel model) {
        super(name);
        this.model = model;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> container = new ArrayList<>();
        container.add(model);
        return container;
    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        return model;
    }

    /**
     * Streams file data for opening saved charts.
     *
     * @param input  stream
     * @param name   file name
     * @param format format
     * @return component to be opened
     */
    public static PieChartComponent open(final InputStream input, final String name, final String format) {
        PieChartModel dataModel = (PieChartModel) PieChartModel.getXStream().fromXML(input);
        return new PieChartComponent(name, dataModel);
    }

    /**
     * @return the model.
     */
    public PieChartModel getModel() {
        return model;
    }

    @Override
    public void save(final OutputStream output, final String format) {
        PieChartModel.getXStream().toXML(model, output);
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
        return Utils.getSimbrainXStream().toXML(model);
    }

}
