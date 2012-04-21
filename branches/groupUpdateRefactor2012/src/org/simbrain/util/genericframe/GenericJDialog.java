package org.simbrain.util.genericframe;

import java.awt.Dialog;
import java.awt.Frame;
import java.beans.PropertyVetoException;

import javax.swing.JDialog;

/**
 * JDialog which implements Generic Frame
 */
public class GenericJDialog extends JDialog implements GenericFrame {

    public GenericJDialog(Frame parent, String title) {
    	super(parent, title);
	}

	public GenericJDialog() {
	}

	public void setIcon(boolean b) throws PropertyVetoException {
    }     
    

}

