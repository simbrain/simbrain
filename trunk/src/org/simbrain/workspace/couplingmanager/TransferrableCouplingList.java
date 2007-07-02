package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;

/**
 * Packages a list of couplings into an object for drag and drop data transfer.
 * 
 * @author jyoshimi
 */
public class TransferrableCouplingList {
    
    ArrayList list = null;
    
    private String producerOrConsumer;

    public TransferrableCouplingList(ArrayList list, String producerOrConsumer) {
        this.list = list;
        this.producerOrConsumer = producerOrConsumer;
    }

    /**
     * @return the list
     */
    public ArrayList getList() {
        return list;
    }

    /**
     * @return the producerOrConsumer
     */
    public String getProducerOrConsumer() {
        return producerOrConsumer;
    }
    
    
}