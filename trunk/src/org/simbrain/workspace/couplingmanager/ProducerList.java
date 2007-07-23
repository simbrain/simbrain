package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;

/**
 * A list of couplings viewed by a jlist.
 * @author jyoshimi
 *
 */
public class ProducerList extends AbstractListModel implements ListModel {

    /** List of producers. */
    private ArrayList<Producer> producerList = new ArrayList<Producer>();

    /**
     * Creates the producer list.
     * @param producerList list of producers.
     */
    public ProducerList(final ArrayList<Producer>  producerList) {
        this.producerList = producerList;
    }

    /**
     * Returns the object at the specified location.
     * @param index of object.
     * @return object at location.
     */
    public Object getElementAt(final int index) {
       return producerList.get(index);
    }

    /**
     * Size of producer list.
     * @return size of list.
     */
    public int getSize() {
        return producerList.size();
    }

    /**
     * Adds a producer to the specified location.
     * @param element to be added.
     */
    public void addElement(final Producer element) {
        producerList.add(element);
    }

    /**
     * @return the consumerList
     */
    public ArrayList<Producer> asArrayList() {
        return producerList;
    }
}
