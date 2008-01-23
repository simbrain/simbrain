package org.simbrain.workspace.gui.couplingmanager;

import java.util.ArrayList;

/**
 * Packages a list of couplings into an object for drag and drop data transfer.
 *
 * @author jyoshimi
 */
public class TransferrableCouplingList {

    /** List of couplings. */
    private ArrayList list = null;

    /** String value determing producer or consumer. */
    private String producerOrConsumer;

    /**
     * Creates a new transferrable coupling list.
     * @param list of couplings.
     * @param producerOrConsumer string value.
     */
    public TransferrableCouplingList(final ArrayList list, final String producerOrConsumer) {
        this.list = list;
        this.producerOrConsumer = producerOrConsumer;
    }

    /**
     * Returns the list of couplings.
     * @return the list
     */
    public ArrayList getList() {
        return list;
    }

    /**
     * Returns the string value.
     * @return the producerOrConsumer
     */
    public String getProducerOrConsumer() {
        return producerOrConsumer;
    }
}