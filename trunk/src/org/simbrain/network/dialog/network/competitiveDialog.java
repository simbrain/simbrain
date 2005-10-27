/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.network;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

public class CompetitiveDialog extends StandardDialog {
	private LabelledItemPanel logic = new LabelledItemPanel();
	private JTextField numberOfNeurons = new JTextField();
	private JTextField epsilon = new JTextField();
	private NetworkPanel thePanel;
	
	public CompetitiveDialog(NetworkPanel np) {
		thePanel = np;
		init();
	}
	
	private void init(){
	    setTitle("New Competitive Netwok");
	    logic.setLocation(500, 0);
	    logic.addItem("Number of neurons", numberOfNeurons);
	    logic.addItem("Epsilon", epsilon);
	    setContentPane(logic);
	}
	
	/**
	 * @return Returns the epsilon.
	 */
	public int getEpsilon() {
		return Integer.parseInt(epsilon.getText());
	}
	/**
	 * @return Returns the numberOfNeurons.
	 */
	public int getNumberOfNeurons() {
		return Integer.parseInt(numberOfNeurons.getText());
	}
	/**
	 * @param numberOfNeurons The numberOfNeurons to set.
	 */
	public void setNumberOfNeurons(JTextField numberOfNeurons) {
		this.numberOfNeurons = numberOfNeurons;
	}
}
