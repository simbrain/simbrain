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
package org.simbrain.plot.rasterchart;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents raster data.
 */
public class RasterPlotComponent extends WorkspaceComponent implements AttributeContainer {

    /**
     * The data model.
     */
    private final RasterModel model;

    /**
     * Create new raster plot component.
     *
     * @param name name
     */
    public RasterPlotComponent(final String name) {
        super(name);
        model = new RasterModel();
    }

    /**
     * Creates a new raster plot component from a specified model. Used in
     * deserializing.
     *
     * @param name  chart name
     * @param model chart model
     */
    public RasterPlotComponent(final String name, final RasterModel model) {
        super(name);
        this.model = model;
    }

    public RasterModel getModel() {
        return model;
    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        return model;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        return this;
    }

    /**
     * Opens a saved raster plot.
     *
     * @param input  stream
     * @param name   name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static RasterPlotComponent open(final InputStream input, final String name, final String format) {
        RasterModel dataModel = (RasterModel) RasterModel.getXStream().fromXML(input);
        return new RasterPlotComponent(name, dataModel);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        RasterModel.getXStream().toXML(model, output);
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
        return RasterModel.getXStream().toXML(model);
    }

    /**
     * Set raster values.  Can couple to this.
     *
     * @param values the current "y-axis" value for the raster series
     */
    @Consumable
    public void setValues(final double[] values) {

        // TODO: Move to model
        for (int i = 0, n = values.length; i < n; i++) {
            model.addData(RasterPlotComponent.this.getWorkspace().getTime(), values[i]);
        }
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        containers.add(this);
        return containers;
    }
}
