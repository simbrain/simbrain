package org.simbrain.network.dialog.synapse;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.util.LabelledItemPanel;

/**
 * 
 * <b>AbstractSpikeResponsePanel</b>
 */
public abstract class AbstractSpikeResponsePanel extends JPanel{
    public static final String NULL_STRING = "...";
    
    protected LabelledItemPanel mainPanel = new LabelledItemPanel();
    protected ArrayList spikeResponderList; // The neurons being modified
	
    protected org.simnet.interfaces.Network parentNet = null;
	
    public void addItem(String text, JComponent comp) {
        mainPanel.addItem(text,comp);
    }
    public void addItemLabel(JLabel text, JComponent comp) {
        mainPanel.addItemLabel(text,comp);
    }
    
    public AbstractSpikeResponsePanel() {
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
        
    }
    
    /**
     * Populate fields with current data
     */
    public abstract void fillFieldValues();

    /**
     * Populate fields with default data
     */
    public abstract void fillDefaultValues();

     /**
      * Called externally when the dialog is closed,
      * to commit any changes made
      */
    public abstract void commitChanges();

    /**
     * @return Returns the spiker_list.
     */
    public ArrayList getSpikeResponderList() {
        return spikeResponderList;
    }
    
    /**
     * @param spiker_list
     *            The spiker_list to set.
     */
    public void setSpikeResponderList(ArrayList spiker_list) {
        this.spikeResponderList = spiker_list;
    }

    /**
     * Add notes or other text to bottom of panel.  Can be html formatted.
     */
    public void addBottomText(String text) {
        JPanel labelPanel = new JPanel();
        JLabel theLabel = new JLabel(text);
        labelPanel.add(theLabel);
        this.add(labelPanel, BorderLayout.SOUTH);
        
    }
}
