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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Vision world frame.
 */
public final class VisionWorldComponent extends WorkspaceComponent {

    /** Vision world. */
    private final VisionWorld visionWorld;

    /**
     * Create a new vision world frame with the specified name.
     *
     * @param name name
     */
    public VisionWorldComponent(final String name) {
        this(name, new MutableVisionWorldModel());
        initAttributes();
    }

    /**
     * Create a new vision world frame with the specified name and model.
     *
     * @param name name
     * @param model model
     */
    public VisionWorldComponent(final String name, VisionWorldModel model) {
        super(name);
        visionWorld = new VisionWorld(model);
        initAttributes();
    }

    /**
     * Initialize attributes.
     */
    private void initAttributes() {
        if (getProducerTypes().size() == 0) {
            addProducerType(new AttributeType(this, "Sensor", "Value", double.class, true));
        }
        visionWorld.getModel().addModelListener(new VisionWorldModelListener() {

            public void pixelMatrixChanged(VisionWorldModelEvent event) {
            }

            public void sensorMatrixChanged(VisionWorldModelEvent event) {
               VisionWorldComponent.this.firePotentialAttributesChanged();
            }

        });
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {
        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();
        for (AttributeType type : getVisibleProducerTypes()) {
            if (type.getTypeID().equalsIgnoreCase("Sensor")) {
              SensorMatrix sensorMatrix = getVisionWorld().getModel().getSensorMatrix();
              for (int row = 0, rows = sensorMatrix.rows(); row < rows; row++) {
                  for (int column = 0, columns = sensorMatrix.columns(); column < columns; column++) {
                      Sensor sensor = sensorMatrix.getSensor(row, column);
                      returnList.add(new PotentialProducer(sensor, sensor.getKey(), type));
                  }
              }
            }
        }
        return returnList;
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        String[] rowCol = objectKey.split(","); //todo check that string is valid
        int row = Integer.parseInt(rowCol[0]);
        int col = Integer.parseInt(rowCol[1]);
        return getVisionWorld().getModel().getSensorMatrix().getSensor(row, col);
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof Sensor) {
            return ((Sensor)object).getKey();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public static VisionWorldComponent open(InputStream input, String name, String format) {
        MutableVisionWorldModel model = (MutableVisionWorldModel) AbstractVisionWorldModel.getXStream().fromXML(input);
        return new VisionWorldComponent(name, model);
    }

    @Override
    public void save(final OutputStream output, final String format) {
         MutableVisionWorldModel.getXStream().toXML(visionWorld.getModel(), output);
    }

    @Override
    public void closing() {
        // empty
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

    /**
     * Returns vision world canvas.
     *
     * @return vision world.
     */
    public VisionWorld getVisionWorld() {
        return visionWorld;
    }
}
