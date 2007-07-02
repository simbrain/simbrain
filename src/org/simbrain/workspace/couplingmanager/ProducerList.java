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
public class ProducerList extends AbstractListModel implements ListModel {

    private ArrayList<Producer> producerList = new ArrayList<Producer>(); 
    
    public ProducerList(ArrayList<Producer>  producerList) {
        this.producerList = producerList;
    }
    
    public Object getElementAt(int index) {
       return producerList.get(index);
    }

    public int getSize() {
        return producerList.size();
    }

    public void addElement(Producer element) {
        producerList.add(element);
    }
}
