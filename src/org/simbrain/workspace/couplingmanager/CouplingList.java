package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.ProducingAttribute;

/**
 * A list of couplings viewed by a jlist.
 * @author jyoshimi
 *
 */
public class CouplingList extends AbstractListModel implements ListModel {

    /** List of couplings. */
    private ArrayList<Coupling> couplingList = new ArrayList<Coupling>();

    /**
     * Default constructor.
     */
    public CouplingList() {
        super();
    }

    /**
     * Constructs a list of couplings.
     * @param couplingList list of couplings
     */
    public CouplingList(final ArrayList<Coupling> couplingList) {
        super();
        this.couplingList = couplingList;
    }

    /**
     * Returns the object at the specified location.
     * @param index of item
     * @return object at given index
     */
    public Object getElementAt(final int index) {
       return couplingList.get(index);
    }

    /**
     * Returns the size of the list.
     * @return size of list
     */
    public int getSize() {
        return couplingList.size();
    }

    /**
     * Adds a coupling to the list.
     * @param element to be added
     */
    public void addElement(final Coupling element) {
        couplingList.add(element);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * Position to bind a producer.
     * @param producer to be bound
     * @param index of location to bind
     */
    public void bindElementAt(final ProducingAttribute producer, final int index) {
        couplingList.get(index).setProducingAttribute(producer);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * Inserts a coupling at the specified location.
     * @param coupling to be inserted
     * @param i location of insertion
     */
    public void insertElementAt(final Coupling coupling, final int i) {
        couplingList.add(i, coupling);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * Returns the coupling list.
     * @return the couplingList
     */
    public ArrayList<Coupling> getCouplingList() {
        return couplingList;
    }

    /**
     * Sets the coupling list.
     * @param couplingList the couplingList to set
     */
    public void setCouplingList(final ArrayList<Coupling> couplingList) {
        this.couplingList = couplingList;
    }
}
