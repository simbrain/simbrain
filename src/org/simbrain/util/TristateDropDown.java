/*
 * Created on Aug 6, 2005
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
package org.simbrain.util;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;

/**
 * @author jyoshimi
 *
 * Essentially a three-state checkbox.  The third "null" state is used to show that there are inconsistencies between
 * various checkboxes.
 * 
 */
public class TristateDropDown extends JComboBox {

	public static int TRUE = 0;
	public static int FALSE = 1;
	public static int NULL = 2;
	
	public TristateDropDown() {
		super();
		addItem("Yes");
		addItem("No");
	}
	
	public TristateDropDown(String itemOne, String itemTwo){
	    super();
	    addItem(itemOne);
	    addItem(itemTwo);
	}
	
	public void setNull() {
		if(this.getItemCount() == 2) {
			addItem("...");		
		}
		setSelectedIndex(NULL);
	}
	
	public boolean isSelected() {
		if (this.getSelectedIndex() == TRUE) {
			return true;
		} else return false;
		
	}
	
	public void setSelected(boolean val) {
		if (val == true) {
			setSelectedIndex(TRUE);
		} else setSelectedIndex(FALSE);
	}
	
	public boolean isNull(){
	    if(this.getSelectedIndex() == NULL){
	        return true;
	    } else {
	        return false;
	    }
	}
}
