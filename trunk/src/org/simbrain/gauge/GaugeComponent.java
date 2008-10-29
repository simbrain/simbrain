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
package org.simbrain.gauge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.simbrain.console.ConsoleComponent;
import org.simbrain.gauge.core.Dataset;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.Projector;
import org.simbrain.gauge.core.Variable;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.OdorWorld;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>GaugeComponent</b> wraps a Gauge object in a Simbrain workspace frame, which also
 * stores information about the variables the Gauge is representing.
 */
public class GaugeComponent extends WorkspaceComponent<GaugeComponentListener> {
    /** the static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GaugeComponent.class);
    
    /** Consumer list. */
    private Collection<Consumer> consumers = new ArrayList<Consumer>();
    
    /**
     * Creates a new gauge component.
     * 
     * @param name The name of the component.
     */
    public GaugeComponent(final String name) {
        super(name);
    }

    /** Current gauge. */
    private Gauge gauge = new Gauge();
    
    /**
     * Returns the underlying Gauge.
     * 
     * @return The underlying Gauge.
     */
    public Gauge getGauge() {
        return gauge;
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(Projector.class, "logger");
        xstream.omitField(Dataset.class, "logger");
        xstream.omitField(Dataset.class, "distances");
        xstream.omitField(Dataset.class, "dataset");
        return xstream;
    }

    /**
     * Update couplings.
     * TODO Think about this...
     * @param dims dimensions to update
     */
    public void resetCouplings(final int dims) {
        consumers.clear();
        for (int i = 0; i < dims; i++) {
            consumers.add(new Variable(gauge, this, i));
        }
    }
    
    /**
     * Wires the provided producers to gauge consumers.
     * If the number of producers has changed since the last
     * wire-up or this is the first wire-up, the component
     * the Gauge is refreshed
     * 
     * @param producers The producers too wire up with couplings.
     */
    @SuppressWarnings("unchecked")
    void wireCouplings(final Collection<? extends Producer> producers) {
        /* Handle Coupling wire-up */
        
        LOGGER.debug("wiring " + producers.size() + " producers");
        
        int oldDims = gauge.getDimensions();
        
        int newDims = producers.size();

        resetCouplings(newDims);
        
        Iterator<? extends Producer> producerIterator = producers.iterator();
        
        for (Consumer consumer : consumers) {
            if (producerIterator.hasNext()) {
                Coupling<?> coupling = new Coupling(producerIterator.next()
                    .getDefaultProducingAttribute(), consumer.getDefaultConsumingAttribute());
                getWorkspace().addCoupling(coupling);
            }
        }

        /* If the new data is inconsistent with the old, reset the gauge */
        if (oldDims != newDims) {
            gauge.init(newDims);
            for (GaugeComponentListener listener : getListeners()) {
                listener.dimensionsChanged(newDims);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void update() {
        gauge.updateCurrentState();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void closing() {
        // TODO Auto-generated method stub
        
    }

    public static GaugeComponent open(InputStream input, final String name, final String format) {
        return (GaugeComponent) getXStream().fromXML(input);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(output);
    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) {
        super.setCurrentDirectory(currentDirectory);
        GaugePreferences.setCurrentDirectory(currentDirectory);
    }

    @Override
    public String getCurrentDirectory() {
        return GaugePreferences.getCurrentDirectory();
    }

    @Override
    public String getXML() {
        return getXStream().toXML(gauge.getCurrentProjector());
    }

    @Override
    public void deserializeFromReader(FileReader reader) {
        gauge.setCurrentProjector((Projector) getXStream().fromXML(reader));
    }
}
