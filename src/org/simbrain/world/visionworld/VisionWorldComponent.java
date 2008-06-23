/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.dataworld.DataWorldComponent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Vision world frame.
 */
public final class VisionWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {//implements CouplingContainer {

    /** Vision world. */
    private final VisionWorld visionWorld;


    /**
     * Create a new vision world frame with the specified name.
     *
     * @param name name
     */
    public VisionWorldComponent(final String name) {
        this(name, new MutableVisionWorldModel());
    }

    public VisionWorldComponent(final String name, VisionWorldModel model) {
        super(name);
        visionWorld = new VisionWorld(model);
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // omit fields
        return xstream;
    }
    
    /**
     * Recreates an instance of this class from a saved component.
     * 
     * @param input
     * @param name
     * @param format
     * @return
     */
    public static VisionWorldComponent open(InputStream input, String name, String format) {
        return (VisionWorldComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(output);
    }
    
    @Override
    public void closing() {
        // empty
    }
     


    /** {@inheritDoc} */
    public List<Consumer> getConsumers() {
        return Collections.<Consumer>emptyList();
    }
    /** {@inheritDoc} */
    public List<Producer> getProducers() {
        List<Producer> producers = new ArrayList<Producer>();
        VisionWorldModel model = visionWorld.getModel();
        SensorMatrix sensorMatrix = model.getSensorMatrix();
        for (int row = 0, rows = sensorMatrix.columns(); row < rows; row++) {
            for (int column = 0, columns = sensorMatrix.rows(); column < columns; column++) {
                Sensor sensor = sensorMatrix.getSensor(row, column);
                sensor.setParentComponent(this);
                producers.add(sensor);
            }
        }
        return Collections.unmodifiableList(producers);
    }

    /** {@inheritDoc} */
    @Override
    public void update() {
        // Possibly change this later so only sensors with couplings are updated.
        VisionWorldModel model = visionWorld.getModel();
        PixelMatrix pixelMatrix = model.getPixelMatrix();
        SensorMatrix sensorMatrix = model.getSensorMatrix();
        for (int column = 0, columns = sensorMatrix.columns(); column < columns; column++) {
            for (int row = 0, rows = sensorMatrix.rows(); row < rows; row++) {
                Sensor sensor = sensorMatrix.getSensor(row, column);
                sensor.sample(pixelMatrix);
            }
        }
    }

    public VisionWorld getVisionWorld() {
        return visionWorld;
    }
}
