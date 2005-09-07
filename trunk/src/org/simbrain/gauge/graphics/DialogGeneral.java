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
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.gauge.core.Settings;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogGeneral</b> is a dialog box for setting general Gauge properties.
 * 
 */
public class DialogGeneral extends StandardDialog {
	
	private GaugePanel theGaugePanel;
	
	private JTextField perturbationFactor = new JTextField();
	private JTextField tolerance = new JTextField();
	private JComboBox addMethod = new JComboBox(Settings.addMethods);

	
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	
	/**
	  * This method is the default constructor.
	  */
	 public DialogGeneral(GaugePanel gp)
	 {
		theGaugePanel = gp;
		init();
	 }

	 /**
	  * This method initializes the components on the panel.
	  */
	 private void init()
	 {
	 	setTitle("General Dialog");

		fillFieldValues();
		myContentPane.setBorder(BorderFactory.createEtchedBorder());

		myContentPane.addItem("Only add new point if at least this far from any other point", tolerance);
		myContentPane.addItem("Degree to which to perturb overlapping low-dimensional points", perturbationFactor);				
		myContentPane.addItem("Method for adding new datapoints", addMethod);
		 
		 setContentPane(myContentPane);
	 }
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		tolerance.setText(Double.toString(theGaugePanel.getGauge().getCurrentProjector().getTolerance()));		
		perturbationFactor.setText(Double.toString(theGaugePanel.getGauge().getCurrentProjector().getPerturbationAmount()));		
		int i = theGaugePanel.getGauge().getCurrentProjector().getAddMethodIndex();
		addMethod.setSelectedIndex(i);	
	 }
	 
	/**
	 * Set projector values based on fields 
	 */
    public void commit() {

		theGaugePanel.getGauge().getCurrentProjector().setTolerance(Double.valueOf(tolerance.getText()).doubleValue());
		theGaugePanel.getGauge().getCurrentProjector().setPerturbationAmount(Double.valueOf(perturbationFactor.getText()).doubleValue());
		theGaugePanel.getGauge().getCurrentProjector().setAddMethod(addMethod.getSelectedItem().toString());
    }


}
