package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import org.simbrain.workspace.*;

/**
 * A list of couplings viewed by a jlist
 * @author jyoshimi
 *
 */
public class CouplingList extends AbstractListModel implements ListModel {

    private ArrayList<Coupling> couplingList = new ArrayList<Coupling>(); 
    
    public CouplingList() {
        super();
    }
    
    public CouplingList(ArrayList<Coupling> couplingList) {
        super();
        this.couplingList = couplingList;
    }

    public Object getElementAt(int index) {
       return couplingList.get(index);
    }

    public int getSize() {
        return couplingList.size();
    }

    public void addElement(Coupling element) {
        couplingList.add(element);
        this.fireContentsChanged(this, 0, getSize());
    }
    
    public void bindElementAt(ProducingAttribute producer, int index) {
        couplingList.get(index).setProducingAttribute(producer);
        this.fireContentsChanged(this, 0, getSize());
    }

    public void insertElementAt(Coupling coupling, int i) {
        couplingList.add(i, coupling);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * @return the couplingList
     */
    public ArrayList<Coupling> getCouplingList() {
        return couplingList;
    }

    /**
     * @param couplingList the couplingList to set
     */
    public void setCouplingList(ArrayList<Coupling> couplingList) {
        this.couplingList = couplingList;
    }
}
