/*
 * Part of HiSee, a tool for visualizing high dimensional datasets
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.graphics;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectSammon;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogSammon</b> is a dialog box for setting the properties of the 
 * Sammon mapping algorithm.
 */

public class DialogSammon extends StandardDialog {
	
	private Gauge theGauge;
	
	private JTextField epsilonField = new JTextField();

	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	
	/**
	  * This method is the default constructor.
	  */
	 public DialogSammon(Gauge gauge)
	 {
	 	 theGauge = gauge;
		 init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
		 setTitle("Sammon Dialog");

		 fillFieldValues();
		 myContentPane.setBorder(BorderFactory.createEtchedBorder());
		 epsilonField.setColumns(3);
		 myContentPane.addItem("Step size", epsilonField);
		 
		 setContentPane(myContentPane);
	 }
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		ProjectSammon gauge = (ProjectSammon)theGauge.getCurrentProjector();
	 	epsilonField.setText(Double.toString(gauge.getEpsilon()));
	 }
	 
	/**
	* Set projector values based on fields 
	*/
   public void setProjector() {
	   ((ProjectSammon)theGauge.getCurrentProjector()).setEpsilon(Double.valueOf(epsilonField.getText()).doubleValue());
	}


}
