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
public class ConsumerList extends AbstractListModel implements ListModel {

    private ArrayList<Consumer> consumerList = new ArrayList<Consumer>(); 
    
    public ConsumerList(ArrayList<Consumer>  consumerList) {
        this.consumerList = consumerList;
    }
    
    public Object getElementAt(int index) {
       return consumerList.get(index);
    }

    public int getSize() {
        return consumerList.size();
    }

    public void addElement(Consumer element) {
        consumerList.add(element);
    }
}
