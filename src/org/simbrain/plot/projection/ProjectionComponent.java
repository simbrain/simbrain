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

import org.simbrain.util.projection.DataPoint;
import org.simbrain.util.projection.DataPointColored;
import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Component for a projection plot.
 */
public class ProjectionComponent extends WorkspaceComponent implements AttributeContainer {

    /**
     * Data model.
     */
    private ProjectionModel projectionModel;

    /**
     * Create new Projection Component.
     *
     * @param name
     */
    public ProjectionComponent(final String name) {
        super(name);
        projectionModel = new ProjectionModel();
    }

    /**
     * Create a projection component from an existing set of data. Used in
     * deserializing.
     *
     * @param model projection model
     * @param name  name of component
     */
    public ProjectionComponent(final ProjectionModel model, final String name) {
        super(name);
        projectionModel = model;

        // Add the data to the chart.
        int numPoints = projectionModel.getProjector().getNumPoints();
        for (int i = 0; i < numPoints; i++) {
            DataPoint point = projectionModel.getProjector().getDownstairs().getPoint(i);
            if (point != null) {
                projectionModel.addPoint(point.get(0), point.get(1));
            }
        }

    }

    @Override
    public String getXML() {
        return ProjectionModel.getXStream().toXML(this);
    }

    /**
     * Opens a saved projection component.
     *
     * @param input  stream
     * @param name   name of file
     * @param format format
     * @return component to be opened
     */
    public static ProjectionComponent open(InputStream input, final String name, final String format) {
        ProjectionModel model = (ProjectionModel) ProjectionModel.getXStream().fromXML(input);
        return new ProjectionComponent(model, name);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        projectionModel.getProjector().getUpstairs().preSaveInit();
        projectionModel.getProjector().getDownstairs().preSaveInit();
        ProjectionModel.getXStream().toXML(projectionModel, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public void closing() {
    }

    /**
     * Get the current state of the dimension objects, send this to the
     * projection algorithm, and update the graphics.
     */
    @Override
    public void update() {
        // Notify Gui that this component was updated.
        fireUpdateEvent();
    }

    /**
     * Get reference to underlying projector object.
     *
     * @return projector object.
     */
    public Projector getProjector() {
        return projectionModel.getProjector();
    }

    /**
     * Clear the dataset.
     */
    public void clearData() {
        projectionModel.getProjector().reset();
    }

    /**
     * Used for debugging model.
     */
    public void debug() {
        System.out.println("------------ Print contents of dataset ------------");
        Projector projector = projectionModel.getProjector();
        for (int i = 0; i < projector.getNumPoints(); i++) {
            // System.out.println("<" + i + "> "
            // + projector.getProjectedPoint(i).get(0) + ","
            // + projector.getProjectedPoint(i).get(1));
        }
        System.out.println("--------------------------------------");
    }

    /**
     * @return the projectionModel
     */
    public ProjectionModel getProjectionModel() {
        return projectionModel;
    }

    @Producible
    public double[] getCurrentPoint() {
        if (projectionModel.getProjector().getCurrentPoint() != null) {
            return projectionModel.getProjector().getCurrentPoint().getVector();
        }
        return null;
    }

    @Consumable
    public void setLabel(String text) {
        if (projectionModel.getProjector().getCurrentPoint() != null) {
            String currentText = projectionModel.getProjector().getCurrentPoint().getLabel();
            // // Don't empty filled text
            // if (text.isEmpty() && !currentText.isEmpty()) {
            //     return;
            // }
            projectionModel.getProjector().getCurrentPoint().setLabel(text);
        }
    }

    /**
     * Add a new point to the projection dataset using an array.
     *
     * @param newPoint the new point
     */
    @Consumable
    public void addPoint(double[] newPoint) {
        if (newPoint.length != projectionModel.getProjector().getDimensions()) {
            projectionModel.getProjector().init(newPoint.length);
        }
        projectionModel.getProjector().addDatapoint(new DataPointColored(newPoint));
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> container = new ArrayList<>();
        container.add(this);
        return container;
    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        return this;
    }

}
