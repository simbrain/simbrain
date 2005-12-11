package org.simbrain.network.nodes;

import org.simbrain.network.NetworkPanel;

import edu.umd.cs.piccolo.PNode;


/**
 * Debug node.
 */
public abstract class ScreenElement extends PNode {
    
    private NetworkPanel parentPanel;

    /**
     * @return Returns the parentPanel.
     */
    public NetworkPanel getParentPanel() {
        return parentPanel;
    }

    /**
     * @param parentPanel The parentPanel to set.
     */
    public void setParentPanel(NetworkPanel parentPanel) {
        this.parentPanel = parentPanel;
    }
    
    

}
