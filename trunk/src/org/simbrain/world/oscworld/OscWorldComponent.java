package org.simbrain.world.oscworld;

import java.io.OutputStream;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    extends WorkspaceComponent<WorkspaceComponentListener> {

    /** OSC port in. */
    private final OSCPortIn oscPortIn;

    /** OSC port out. */
    private final OSCPortOut oscPortOut;

    /** List of OSC consumers. */
    private final EventList<OscMessageConsumer> consumers;

    /** List of OSC producers. */
    private final EventList<OscMessageProducer> producers;

    /** Default OSC out host. */
    private static final InetAddress DEFAULT_OSC_OUT_HOST;

    /** Default OSC in port. */
    private static final int DEFAULT_OSC_IN_PORT = 9998;

    /** Default OSC out port. */
    private static final int DEFAULT_OSC_OUT_PORT = 9999;

    static
    {
        try
        {
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
        consumers = GlazedLists.eventList(new ArrayList<OscMessageConsumer>());
        producers = GlazedLists.eventList(new ArrayList<OscMessageProducer>());
    }


    /** {@inheritDoc} */
    public void close() {
        oscPortIn.close();
        oscPortOut.close();
        // TODO:  remove consumer list event listeners
    }

    /** {@inheritDoc} */
    public void save(final OutputStream outputStream, final String format) {
        // empty
    }

    /** {@inheritDoc} */
    public void update() {
        // empty
    }

    /** {@inheritDoc} */
    public Collection<? extends Consumer> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }

    /** {@inheritDoc} */
    public Collection<? extends Producer> getProducers() {
        return Collections.unmodifiableList(producers);
    }

    // TODO:  make these bound properties
    String getOscInHost() {
        return DEFAULT_OSC_OUT_HOST.toString();
    }

    String getOscOutHost() {
        return DEFAULT_OSC_OUT_HOST.toString();
    }

    int getOscInPort() {
        return DEFAULT_OSC_IN_PORT;
    }

    int getOscOutPort() {
        return DEFAULT_OSC_OUT_PORT;
    }

    /**
     * Return the OSC port in for this OSC world component.
     *
     * @return the OSC port in for this OSC world component
     */
    OSCPortOut getOscPortIn() {
        return oscPortOut;
    }

    /**
     * Return the OSC port out for this OSC world component.
     *
     * @return the OSC port out for this OSC world component
     */
    OSCPortOut getOscPortOut() {
        return oscPortOut;
    }

    /**
     * Add the specified OSC message consumer list event listener.
     *
     * @param listener OSC message consumer list event listener to add
     */
    void addConsumerListEventListener(final ListEventListener<OscMessageConsumer> listener) {
        consumers.addListEventListener(listener);
    }

    /**
     * Remove the specified OSC message consumer list event listener.
     *
     * @param listener OSC message consumer list event listener to remove
     */
    void removeConsumerListEventListener(final ListEventListener<OscMessageConsumer> listener) {
        consumers.removeListEventListener(listener);
    }

    // TODO:  remove from API
    EventList<OscMessageConsumer> getConsumersEventList() {
        return consumers;
    }

    /**
     * Add the specified OSC message producer list event listener.
     *
     * @param listener OSC message producer list event listener to add
     */
    void addProducerListEventListener(final ListEventListener<OscMessageProducer> listener) {
        producers.addListEventListener(listener);
    }

    /**
     * Remove the specified OSC message producer list event listener.
     *
     * @param listener OSC message producer list event listener to remove
     */
    void removeProducerListEventListener(final ListEventListener<OscMessageProducer> listener) {
        producers.removeListEventListener(listener);
    }

    // TODO:  remove from API
    EventList<OscMessageProducer> getProducersEventList() {
        return producers;
    }

    /**
     * Add a new OSC in message with the specified address.
     *
     * @param address OSC in message address, must not be null and must start with <code>'/'</code> character
     */
    void addInMessage(final String address) {
        OscMessageProducer producer = new OscMessageProducer(address, this);
        producers.add(producer);
    }

    /**
     * Add a new OSC out message with the specified address.
     *
     * @param address OSC out message address, must not be null and must start with <code>'/'</code> character
     */
    void addOutMessage(final String address) {
        OscMessageConsumer consumer = new OscMessageConsumer(address, this);
        consumers.add(consumer);
    }
}