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

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
/**
 * Sensor.
 */
public final class Sensor implements Producer {

    /** Filter for this sensor. */
    private Filter filter;

    /** Receptive field for this sensor. */
    private final ReceptiveField receptiveField;

    /** Property change support. */
    private final PropertyChangeSupport propertyChangeSupport;

    /** Sample. */
    private double sample;

    /** Producer description. */
    private String producerDescription;

    /** Reference to parent workspace component. */
    private WorkspaceComponent parentComponent;

    /** No filter. */
    private static final Filter NO_FILTER = new Filter()
        {
            /** {@inheritDoc} */
            public double filter(final BufferedImage image) {
                return 0.0d;
            }

            /** {@inheritDoc} */
            public String getDescription() {
                return "None";
            }
        };


    /**
     * Create a new sensor with the specified receptive field and no filter.
     *
     * @param receptiveField receptive field for this sensor, must not be null
     */
    public Sensor(final ReceptiveField receptiveField) {
        this(NO_FILTER, receptiveField);
    }

    /**
     * Create a new sensor with the specified filter and receptive field.
     *
     * @param filter filter for this sensor, must not be null
     * @param receptiveField receptive field for this sensor, must not be null
     */
    public Sensor(final Filter filter, final ReceptiveField receptiveField) {
        if (filter == null) {
            throw new IllegalArgumentException("filter must not be null");
        }
        if (receptiveField == null) {
            throw new IllegalArgumentException("receptiveField must not be null");
        }
        this.filter = filter;
        this.receptiveField = receptiveField;
        updateProducerDescription();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }


    /**
     * Sample the specified pixel matrix, reducing a view of the pixel matrix
     * through the receptive field of this sensor to a single numerical value
     * with the filter for this sensor.
     *
     * @see #getFilter
     * @see #getReceptiveField
     * @param pixelMatrix pixel matrix, must not be null
     * @return a view of the specified pixel matrix through the receptive field
     *    of this sensor reduced to a single numerical value
     */
    public double sample(final PixelMatrix pixelMatrix) {
        if (pixelMatrix == null) {
            throw new IllegalArgumentException("pixelMatrix must not be null");
        }
        Image image = pixelMatrix.view(receptiveField);
        BufferedImage bufferedImage = toBufferedImage(image);
        sample = filter.filter(bufferedImage);
        return sample;
    }

    /**
     * Return the last sample value for this sensor.
     *
     * @return the last sample value for this sensor
     */
    public double getLastSample() {
        return sample;
    }

    /**
     * Return the filter for this sensor.
     * The filter will not be null.
     *
     * @return the filter for this sensor
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Set the filter for this sensor to <code>filter</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param filter filter for this sensor, must not be null
     */
    public void setFilter(final Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter must not be null");
        }
        Filter oldFilter = this.filter;
        this.filter = filter;
        updateProducerDescription();
        propertyChangeSupport.firePropertyChange("filter", oldFilter, this.filter);
    }

    /**
     * Return the receptive field for this sensor.
     * The receptive field will not be null.
     *
     * @return the receptive field for this sensor
     */
    public ReceptiveField getReceptiveField() {
        return receptiveField;
    }

    /**
     * Convert the specified image to a BufferedImage, if necessary.
     *
     * @param image image to convert
     * @return the specified image converted to a BufferedImage
     */
    private BufferedImage toBufferedImage(final Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        if (image instanceof VolatileImage) {
            VolatileImage volatileImage = (VolatileImage) image;
            return volatileImage.getSnapshot();
        }
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
        GraphicsConfiguration graphicsConfiguration = graphicsDevice.getDefaultConfiguration();
        BufferedImage bufferedImage = graphicsConfiguration.createCompatibleImage(image.getWidth(null),
                                                                                  image.getHeight(null));
        Graphics2D g = bufferedImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bufferedImage;
    }

    /**
     * Add the specified property change listener.
     *
     * @param listener listener to add
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Add the specified property change listener for the specified property.
     *
     * @param propertyName property name
     * @param listener listener to add
     */
    public void addPropertyChangeListener(final String propertyName,
                                          final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove the specified property change listener.
     *
     * @param listener listener to remove
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Remove the specified property change listener for the specified property.
     *
     * @param propertyName property name
     * @param listener listener to remove
     */
    public void removePropertyChangeListener(final String propertyName,
                                             final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /** {@inheritDoc} */
    public WorkspaceComponent getParentComponent() {
        return parentComponent;
    }

//    public String getKey() {
//        return filter.getDescription() + " (" + receptiveField.getCenterX() + ")(" + receptiveField.getCenterY() + ")";
//    }

    /**
     * Update producer description.
     */
    private void updateProducerDescription() {
        StringBuffer sb = new StringBuffer();
        sb.append(getFilter().getDescription());
        sb.append(" at (");
        sb.append(receptiveField.getCenterX());
        sb.append(", ");
        sb.append(receptiveField.getCenterY());
        sb.append(")");
        producerDescription = sb.toString();
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return producerDescription;
    }

    /** {@inheritDoc} */
    public Double getValue() {
        return Double.valueOf(sample);
    }

    /** {@inheritDoc} */
    public void setParentComponent(final WorkspaceComponent parentComponent) {
        this.parentComponent = parentComponent;
    }
    
    //TODO

    public String getAttributeDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    public AttributeType getAttributeType() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getKey() {
        // TODO Auto-generated method stub
        return null;
    }
}
