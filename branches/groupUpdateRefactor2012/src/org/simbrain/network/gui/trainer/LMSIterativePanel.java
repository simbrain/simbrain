/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.trainer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.ErrorListener;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * Panel for controlling iterative trainers.  Can be reused by any GUI element that 
 * invokes an iterative trainer (i.e. any subclass of IterableTtrainer).
 * 
 * @author jeffyoshimi
 */
public class LMSIterativePanel extends JPanel {
	
    /** Parent frame. */
    private GenericFrame parentFrame;
    
    /** Reference to trainer object. */
    private LMSIterative trainer;

    /**
     * Construct the panel.
     *
     * @param networkPanel parent frame
     * @param trainer the trainer to control
     */
    public LMSIterativePanel(NetworkPanel networkPanel,
			LMSIterative trainer) {
    	this.trainer = trainer;
    	add(new IterativeControlsPanel(networkPanel, trainer));
    	JButton propertiesButton = new JButton(TrainerGuiActions.getPropertiesDialogAction(trainer));
    	propertiesButton.setHideActionText(true);
    	add(propertiesButton);
    	add(new JButton(randomizeAction));
    	JButton plotButton = new JButton(TrainerGuiActions.getShowPlotAction(networkPanel, trainer));
    	plotButton.setHideActionText(true);
    	add(plotButton);
    	
        // Add listener
        trainer.addErrorListener(new ErrorListener() {

            public void errorUpdated() {
                parentFrame.pack();
            }
            
        });

	}
    
    /**
     * Action for randomizing the underlying network.
     */
	Action randomizeAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Rand.png"));
			// putValue(NAME, "Show properties");
			putValue(SHORT_DESCRIPTION, "Randomize network");
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent arg0) {
			if (trainer != null) {
				trainer.randomize();
			}
		}
	};

    /**
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(GenericFrame parentFrame) {
        this.parentFrame = parentFrame;
    }
}
