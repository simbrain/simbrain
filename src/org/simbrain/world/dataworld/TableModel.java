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
	/**
	 * Constructor for default table, initialized with 
	 * some data
	 *
	 */
	public TableModel(DataWorld world) {
		this.addColumn("");
		for (int i = 1; i < initNumCols; i++)
			this.addColumn(Integer.toString(i));
		for (int i = 0; i < initNumRows; i++)
			this.addRow(newRow());
	}

	/**
	 * Constructor for table given data
	 * 
	 * @param data
	 */
	public TableModel(String[][] data) {
		this.addColumn("");
		int numCols = data[0].length + 1;
		for (int i = 1; i < numCols; i++) {
			this.addColumn(Integer.toString(i));			
		}

		for (int i = 0; i < data.length; i++) {
			Vector row = new Vector(data[i].length + 1);
			row.add(0, new JButton("Send"));
			for (int j = 0; j < data[i].length; j++) {
				row.add(j+1, Double.valueOf((String)data[i][j]));
			}
			addRow(row);
		}
	}

	
	/**
	 * Creates a new, pre-initalised vector to be used in addRow
	 * 
	 * @return Vector
	 */
	public Vector newRow() {
		Vector row = new Vector(this.getColumnCount());
		row.add(0, new JButton("Send"));
		for (int i = 1; i < this.getColumnCount(); i++)
			row.add(i, new Double(0));
		return row;
	}

	/**
	 * Fills the table with zeros
	 *
	 */
	public void zeroFill() {
		for (int i = 1; i < this.getColumnCount(); i++) {
			for (int j = 0; j < this.getRowCount(); j++) {
				this.setValueAt(new Double(0), j, i);
			}
		}
	}

	//same as zerofill, but only fills the last column
	public void zeroFillNew() {
		for (int j = 0; j < this.getRowCount(); j++) {
			this.setValueAt(new Double(0), j, this.getColumnCount() - 1);
		}
	}
	
	/**
	 * Clear the table
	 */
	public void removeAllRows()
	{
	  for(int i=this.getRowCount();i>0;--i)
	    this.removeRow(i-1);      
	}

	/**
	 * Add a matrix of string data to the table,
	 * as doubles
	 * 
	 * @param data the matrix of string doubles to add
	 */
	public void addMatrix(String[][] data) {
				
		removeAllRows();
		
		
		int numCols = data[0].length + 1;
		this.addColumn("");
		for (int i = 1; i < numCols-1; i++) {
			this.addColumn(Integer.toString(i));			
		}

		for (int i = 0; i < data.length; i++) {
			Vector row = new Vector(data[i].length + 1);
			row.add(0, new JButton("Send"));
			for (int j = 0; j < data[i].length; j++) {
				row.add(j+1, Double.valueOf((String)data[i][j]));
			}
			addRow(row);
		}
		
	}
	
	public boolean isCellEditable(int row, int col) {
		if (col == 0)
			return false;
		else
			return true;
	}
	
	public Vector getColumnIdentifiers(){
		return this.columnIdentifiers;
	}
}