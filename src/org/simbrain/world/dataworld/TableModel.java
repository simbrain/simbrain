/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.world.dataworld;

import java.util.Vector;

import javax.swing.JButton;
import javax.swing.table.DefaultTableModel;

/**
 * @author rbartley
 *
 * <b>TableModel</b> extends DefaultTableModel so that the addRow and addColumn commands are available
 */
public class TableModel extends DefaultTableModel {

	private int initNumRows = 5;

	private int initNumCols = 5;

	//the constructor initiates the model using addRow and addColumn iterations
	public TableModel() {
		this.addColumn("Print");
		for (int i = 1; i < initNumCols; i++)
			this.addColumn("Int");
		for (int i = 0; i < initNumRows; i++)
			this.addRow(newRow());
	}

	/**
	 * Creates a new, pre-initalised vector to be used in addRow
	 * 
	 * @return Vector
	 */
	public Vector newRow() {
		Vector row = new Vector(this.getColumnCount());
		row.add(0, new JButton("Print this row"));
		for (int i = 1; i < this.getColumnCount(); i++)
			row.add(i, new Integer(0));
		return row;
	}

	/**
	 * Fills the table with zeros
	 *
	 */
	public void zeroFill() {
		for (int i = 1; i < this.getColumnCount(); i++) {
			for (int j = 0; j < this.getRowCount(); j++) {
				this.setValueAt(new Integer(0), j, i);
			}
		}
	}

	//same as zerofill, but only fills the last column
	public void zeroFillNew() {
		for (int j = 0; j < this.getRowCount(); j++) {
			this.setValueAt(new Integer(0), j, this.getColumnCount() - 1);
		}
	}

	public boolean isCellEditable(int row, int col) {
		if (col == 0)
			return false;
		else
			return true;
	}

}