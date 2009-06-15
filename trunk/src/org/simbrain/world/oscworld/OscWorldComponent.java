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
package org.simbrain.world.oscworld;

import java.io.OutputStream;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import ca.odell.glazedlists.event.ListEventListener;

import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * OSC world component.
 */
public final class OscWorldComponent
    extends WorkspaceComponent {

    /** OSC port in. */
    private final OSCPortIn oscPortIn;

    /** OSC port out. */
    private final OSCPortOut oscPortOut;

    /** Default OSC out host. */
    private static final InetAddress DEFAULT_OSC_OUT_HOST;

    /** Default OSC in port. */
    private static final int DEFAULT_OSC_IN_PORT = 9998;

    /** Default OSC out port. */
    private static final int DEFAULT_OSC_OUT_PORT = 9999;

    static {
        try {
            DEFAULT_OSC_OUT_HOST = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("could not create default OSC out host", e);
        }
    }


    /**
     * Create a new OSC world component with the specified name.
     *
     * @param name name of this OSC world component
     */
    public OscWorldComponent(final String name) {
        super(name);
        try {
            oscPortIn = new OSCPortIn(DEFAULT_OSC_IN_PORT);
            oscPortIn.startListening();
        }
        catch (SocketException e) {
            throw new RuntimeException("could not create OSC port in", e);
        }
        try {
            oscPortOut = new OSCPortOut(DEFAULT_OSC_OUT_HOST, DEFAULT_OSC_OUT_PORT);
        }
        catch (SocketException e) {
            throw new RuntimeException("could not create OSC port out", e);
        }
    }


    /** {@inheritDoc} */
    public void closing() {
        oscPortIn.stopListening();
        // TODO:  always throws SocketException; wrote email to JavaOSC author
        oscPortIn.close();
        oscPortOut.close();
        // TODO:  remove list event listeners
    }

    /** {@inheritDoc} */
    public void save(final OutputStream outputStream, final String format) {
        // empty
    }

    /** {@inheritDoc} */
    public void update() {
        // empty
    }

    // TODO:  make these bound properties
    /**
     * Return the OSC in host name.
     *
     * @return the OSC in host name
     */
    String getOscInHost() {
        return DEFAULT_OSC_OUT_HOST.toString();
    }

    /**
     * Return the OSC out host name.
     *
     * @return the OSC out host name
     */
    String getOscOutHost() {
        return DEFAULT_OSC_OUT_HOST.toString();
    }

    /**
     * Return the OSC in port number.
     *
     * @return the OSC in port number
     */
    int getOscInPort() {
        return DEFAULT_OSC_IN_PORT;
    }

    /**
     * Return the OSC out port number.
     *
     * @return the OSC out port number
     */
    int getOscOutPort() {
        return DEFAULT_OSC_OUT_PORT;
    }

    /**
     * Return the OSC port in for this OSC world component.
     *
     * @return the OSC port in for this OSC world component
     */
    OSCPortIn getOscPortIn() {
        return oscPortIn;
    }

    /**
     * Return the OSC port out for this OSC world component.
     *
     * @return the OSC port out for this OSC world component
     */
    OSCPortOut getOscPortOut() {
        return oscPortOut;
    }

    //TODO: Moving consumer, producer lists to top level breaks this stuff which uses glazed lists..
    
    /**
     * Add the specified OSC message consumer list event listener.
     *
     * @param listener OSC message consumer list event listener to add
     */
    void addConsumerListEventListener(final ListEventListener<OscMessageConsumer> listener) {
//        getConsumers().addListEventListener(listener);
    }

    /**
     * Remove the specified OSC message consumer list event listener.
     *
     * @param listener OSC message consumer list event listener to remove
     */
    void removeConsumerListEventListener(final ListEventListener<OscMessageConsumer> listener) {
 //       getConsumers().removeListEventListener(listener);
    }


    /**
     * Add the specified OSC message producer list event listener.
     *
     * @param listener OSC message producer list event listener to add
     */
    void addProducerListEventListener(final ListEventListener<OscMessageProducer> listener) {
   //     getConsumers().addListEventListener(listener);
    }

    /**
     * Remove the specified OSC message producer list event listener.
     *
     * @param listener OSC message producer list event listener to remove
     */
    void removeProducerListEventListener(final ListEventListener<OscMessageProducer> listener) {
//        getConsumers().removeListEventListener(listener);
    }

    /**
     * Add a new OSC in message with the specified address.
     *
     * @param address OSC in message address, must not be null and must start with
     *    <code>'/'</code> character
     */
    public void addInMessage(final String address) {
        OscMessageProducer producer = new OscMessageProducer(address, this);
        addProducer(producer);
    }

    /**
     * Add a new OSC out message with the specified address.
     *
     * @param address OSC out message address, must not be null and must start with
     *    <code>'/'</code> character
     */
    public void addOutMessage(final String address) {
        OscMessageConsumer consumer = new OscMessageConsumer(address, this);
        addConsumer(consumer);
    }
}