package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import org.simbrain.workspace.Consumer;

/**
 * A list of couplings viewed by a jlist.
 * @author jyoshimi
 *
 */
public class ConsumerList extends AbstractListModel implements ListModel {

    /** List of consumers. */
    private ArrayList<Consumer> consumerList = new ArrayList<Consumer>();

    /**
     * Constructs a list of consumers.
     * @param consumerList List of consumers
     */
    public ConsumerList(final ArrayList<Consumer>  consumerList) {
        this.consumerList = consumerList;
    }

    /**
     * Returns a specific consumer.
     * @param index of consumer.
     * @return consumer at specific location.
     */
    public Object getElementAt(final int index) {
       return consumerList.get(index);
    }

    /**
     * Returns the number of consumers in the list.
     * @return Number of consumers.
     */
    public int getSize() {
        return consumerList.size();
    }

    /**
     * Adds a new element to the consumer list.
     * @param element Consumer to be added
     */
    public void addElement(final Consumer element) {
        consumerList.add(element);
    }
}
