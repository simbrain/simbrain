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
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectCoordinate;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>DialogCoordinate</b> is a dialog box for setting the properties of the 
 * coordinate projection algorithm
 */
public class DialogCoordinate extends StandardDialog {
	
	private Gauge theGauge;
	
	private JTextField firstDimField = new JTextField();
	private JTextField secondDimField= new JTextField();
	private JCheckBox autoFind = new JCheckBox();
	
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	
	/**
	  * This method is the default constructor.
	  */
	 public DialogCoordinate(Gauge gauge)
	 {
	 	 theGauge = gauge;
		 init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
		 setTitle("Coordinate Dialog");

		 fillFieldValues();
		 myContentPane.setBorder(BorderFactory.createEtchedBorder());

		 firstDimField.setColumns(4);
		 myContentPane.addItem("First dimension to project", firstDimField);
		myContentPane.addItem("Second dimension to project", secondDimField);
		myContentPane.addItem("Automatically use most variant dimensions", autoFind);
		 
		 setContentPane(myContentPane);
	 }
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		ProjectCoordinate gauge = (ProjectCoordinate)theGauge.getProjector();
		firstDimField.setText(Integer.toString(gauge.getHi_d1()+1));
		secondDimField.setText(Integer.toString(gauge.getHi_d2()+1));
		autoFind.setSelected(((ProjectCoordinate)theGauge.getProjector()).isAutoFind());
	 }
	 
	/**
	* Set projector values based on fields 
	*/
   public void setProjector() {
		((ProjectCoordinate)theGauge.getProjector()).setHi_d1(Integer.valueOf(firstDimField.getText()).intValue()-1);
		((ProjectCoordinate)theGauge.getProjector()).setHi_d2(Integer.valueOf(secondDimField.getText()).intValue()-1);
		((ProjectCoordinate)theGauge.getProjector()).setAutoFind(autoFind.isSelected());
	}


}
