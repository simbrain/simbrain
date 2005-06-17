/* This program is free software; you can redistribute it and/or modify
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

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolBar;

import org.simbrain.world.World;
;

/**
 * @author rbartley
 *
 * <b>DataWorld</b> creates a table and then adds it to the viewport.
 */
public class DataWorld extends JPanel implements ActionListener, MouseListener,World {

	private TableModel model = new TableModel();
	private JTable table = new JTable(model);
	private DataWorldFrame parentFrame;
	

	public DataWorld() {
		super(new BorderLayout());
		table.getColumnModel().getColumn(0).setCellRenderer(
				new ButtonRenderer(table.getDefaultRenderer(JButton.class)));
		table.addMouseListener(this);
		addToolBar(this);
		this.add("Center", table);
	}

	
	/**
	 * Creates the Menu Bar and adds it to the frame.
	 * 
	 * @param frame
	 * @param table
	 */
	public void addMenuBar(DataWorldFrame frame, DataWorld table) {
		JMenuBar mb = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(table);
		open.setActionCommand("open");
		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(table);
		save.setActionCommand("save");
		JMenuItem close = new JMenuItem("Close");
		close.addActionListener(table);
		close.setActionCommand("close");
		mb.add(file);
		file.add(open);
		file.add(save);
		file.add(close);
		frame.setJMenuBar(mb);
	}

	/**
	 * Creates and adds the toolbar to the panel.
	 * 
	 * @param table
	 */
	public static void addToolBar(DataWorld table) {
		JToolBar tb = new JToolBar();
		tb.setRollover(false);
		tb.setFloatable(true);
		JButton addRow = new JButton("Add a row");
		addRow.addActionListener(table);
		addRow.setActionCommand("addRow");
		JButton addCol = new JButton("Add a column");
		addCol.addActionListener(table);
		addCol.setActionCommand("addCol");
		JButton zeroFill = new JButton("ZeroFill the Table");
		zeroFill.addActionListener(table);
		zeroFill.setActionCommand("zeroFill");
		tb.add(addRow);
		tb.add(addCol);
		tb.add(zeroFill);
		//this line is necessary to keep the toolbar on top
		table.add("North",tb);
	}



	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("open")) {
			openData();
		} else if (e.getActionCommand().equals("save")) {
			saveData();
		} else if (e.getActionCommand().equals("close")) {
			;
		} else if (e.getActionCommand().equals("addRow")) {
			model.addRow(model.newRow());
		} else if (e.getActionCommand().equals("addCol")) {
			model.addColumn("Int");
			model.zeroFillNew();
			//Necessary to keep the buttons properly rendered
			table.getColumnModel().getColumn(0)
					.setCellRenderer(
							new ButtonRenderer(table
									.getDefaultRenderer(JButton.class)));
		} else if (e.getActionCommand().equals("zeroFill")) {
			model.zeroFill();
		}
	}

	/**
	 * Writes out the data from the table as CSV.
	 *
	 */
	private void saveData() {

		FileWriter out;
		PrintWriter p;

		try {
			out = new FileWriter("saveData.txt");

			// Connect print stream to the output stream
			p = new PrintWriter(out);

			for (int i = 1; i < model.getRowCount(); i++) {
				for (int j = 0; j < model.getColumnCount(); j++) {
					p.print(model.getValueAt(j, i) + ",");
				}
				p.println();
			}

			p.close();
		} catch (Exception e) {
			System.err.println("Error writing to file");
		}

	}

	//currently a stub, but will open data in the future
	public void openData() {
		System.out
				.println("I can't currently figure out file handling, so ...");
	}

	public void mouseClicked(MouseEvent e) {
		//This makes the buttons act like buttons instead of images
		Point locus = e.getPoint();
		if (table.columnAtPoint(locus) == 0) {
			int row = table.rowAtPoint(locus);
			for (int i = 1; i < table.getColumnCount(); i++) {

				System.out.print(table.getModel().getValueAt(row, i));
				System.out.print("  ");
			}
			System.out.println();
		} else
			return;
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public String getType() {
		return "DataWorld";
	}

	/**
	 * @return Returns the parentFrame.
	 */
	public DataWorldFrame getParentFrame() {
		return parentFrame;
	}
	/**
	 * @param parentFrame The parentFrame to set.
	 */
	public void setParentFrame(DataWorldFrame parentFrame) {
		this.parentFrame = parentFrame;
	}
	/**
	 * @return Returns the table.
	 */
	public JTable getTable() {
		return table;
	}
	/**
	 * @param table The table to set.
	 */
	public void setTable(JTable table) {
		this.table = table;
	}
}