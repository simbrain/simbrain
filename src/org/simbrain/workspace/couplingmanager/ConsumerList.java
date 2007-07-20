package org.simbrain.workspace.couplingmanager;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import org.simbrain.workspace.Consumer;

/**
 * A list of couplings viewed by a jlist.
 * @author jyoshimi
 *
 */
public class ConsumerList extends AbstractListModel implements ListModel, MouseListener {

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

    public void mouseClicked(MouseEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println(e.getPoint());
    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

	/**
	 * @return the consumerList
	 */
	public ArrayList<Consumer> asArrayList() {
		return consumerList;
	}
}
