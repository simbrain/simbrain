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

import com.thoughtworks.xstream.XStream;
import org.simbrain.util.DoubleArrayConverter;
import org.simbrain.util.XStreamUtils;
import org.simbrain.util.projection.DataPointColored;
import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Component for a projection plot.
 */
public class ProjectionComponent extends WorkspaceComponent implements AttributeContainer {

    // /**
    //  * Data model.
    //  */
    // private ProjectionModel projectionModel;

    /**
     * The main model object.
     */
    private Projector projector = new Projector();

    /**
     * Create new Projection Component.
     */
    public ProjectionComponent(final String name) {
        super(name);
    }

    /**
     * Create a projection component from an existing set of data. Used in
     * deserializing.
     */
    public ProjectionComponent(final Projector proj, final String name) {
        super(name);
        projector = proj;
    }

    private static XStream getProjectorXStream() {
        var xstream = XStreamUtils.getSimbrainXStream();
        xstream.registerConverter(new DoubleArrayConverter());
        return xstream;
    }

    @Override
    public String getXML() {
        return getProjectorXStream().toXML(getProjector());
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
        Projector proj = (Projector) getProjectorXStream().fromXML(input);
        return new ProjectionComponent(proj, name);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        projector.getUpstairs().preSaveInit();
        projector.getDownstairs().preSaveInit();
        getProjectorXStream().toXML(projector, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    /**
     * Get the current state of the dimension objects, send this to the
     * projection algorithm, and update the graphics.
     */
    @Override
    public void update() {
        // Notify Gui that this component was updated.
        getEvents().fireComponentUpdated();
    }

    public Projector getProjector() {
        return projector;
    }

    /**
     * Clear the dataset.
     */
    public void clearData() {
        projector.reset();
    }

    /**
     * Used for debugging model.
     */
    public void debug() {
        System.out.println("------------ Print contents of dataset ------------");
        for (int i = 0; i < projector.getNumPoints(); i++) {
            // System.out.println("<" + i + "> "
            // + projector.getProjectedPoint(i).get(0) + ","
            // + projector.getProjectedPoint(i).get(1));
        }
        System.out.println("--------------------------------------");
    }

    /**
     * Probability of the current point relative to past predictions. Only works
     * with Bayesian coloring
     */
    @Producible(defaultVisibility = false)
    public double getCurrentStateProbability() {
        return projector.getCurrentStateProbability();
    }

    @Producible
    public double[] getCurrentPoint() {
        if (projector.getCurrentPoint() != null) {
            return projector.getCurrentPoint().getVector();
        }
        return null;
    }

    @Consumable
    public void setLabel(String text) {
        if (projector.getCurrentPoint() != null) {
            String currentText = projector.getCurrentPoint().getLabel();
             // Don't empty filled text
             if (text.isEmpty() && !currentText.isEmpty()) {
                 return;
             }
            projector.getCurrentPoint().setLabel(text);
        }
    }

    /**
     * Add a new point to the projection dataset using an array.
     *
     * @param newPoint the new point
     */
    @Consumable
    public void addPoint(double[] newPoint) {
        if (newPoint.length != projector.getDimensions()) {
            projector.init(newPoint.length);
        }
        projector.addDatapoint(new DataPointColored(newPoint));
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> container = new ArrayList<>();
        container.add(this);
        container.add(this.projector);
        return container;
    }

    @Override
    public String getId() {
        return super.getName();
    }
}
