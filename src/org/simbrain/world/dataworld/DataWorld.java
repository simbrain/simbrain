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
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolBar;

import org.simbrain.coupling.CouplingMenuItem;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.network.NetworkPanel;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.Agent;
import org.simbrain.world.World;
;

/**
 * @author rbartley
 *
 * <b>DataWorld</b> creates a table and then adds it to the viewport.
 */
public class DataWorld extends JPanel implements ActionListener, MouseListener,World, Agent {

	private TableModel model = new TableModel();
	private JTable table = new JTable(model);
	private DataWorldFrame parentFrame;
	
	// List of neural networks to update when this world is updated
	private ArrayList commandTargets = new ArrayList();

	private int current_row = 1;
	private String name;

	public DataWorld(DataWorldFrame ws) {
		super(new BorderLayout());
		setParentFrame(ws);
		table.getColumnModel().getColumn(0).setCellRenderer(
				new ButtonRenderer(table.getDefaultRenderer(JButton.class)));
		table.addMouseListener(this);
		//addToolBar(this);
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
			this.getParentFrame().openWorld();
		} else if (e.getActionCommand().equals("save")) {
			this.getParentFrame().saveWorld();
		} else if (e.getActionCommand().equals("close")) {
			model.removeAllRows();
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

	public void resetModel(String[][] data) {
		model = new TableModel(data);
		table.setModel(model);
		table.getColumnModel().getColumn(0).setCellRenderer(
				new ButtonRenderer(table.getDefaultRenderer(JButton.class)));

		parentFrame.resize();
	}

	public void mouseClicked(MouseEvent e) {
		//This makes the buttons act like buttons instead of images
		Point point = e.getPoint();
		if (table.columnAtPoint(point) == 0) {
			current_row = table.rowAtPoint(point);
			updateNetwork();
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

	/**
	 * Used when the creature is directly moved in the world.
	 * 
	 * Used to update network from world, in a way which avoids iterating 
	 * the net more than once
	 */
	public void updateNetwork() {
		for(int i = 0; i < commandTargets.size(); i++) {
			NetworkPanel np = (NetworkPanel)commandTargets.get(i);
			if ((np.getInteractionMode() == NetworkPanel.BOTH_WAYS) || (np.getInteractionMode() == NetworkPanel.WORLD_TO_NET)) {
				np.updateNetworkAndWorld();
			}
		}
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
	
	/**
	 * Dataworlds contain one agent, themselves
	 * 
	 * @return Returns the agentList.
	 */
	public ArrayList getAgentList() {
		ArrayList ret = new ArrayList();
		ret.add(this);
		return ret;
	}

	/**
	 * Dataworlds are agents, hence this returns itself
	 * 
	 * @return Returns the world this agent is associated with, itself
	 */
	public World getParentWorld() {
		return this;
	}


	/**
	 * Returns the value in the given column of the table
	 * uses the current row.
	 */
	public double getStimulus(String[] sensor_id) {
		int i = Integer.parseInt(sensor_id[0]);
		String snum = new String("" + table.getModel().getValueAt(current_row, i + 1));
		return Double.parseDouble(snum);
	}

	/**
	 * Returns a menu with on id, "Column X" for each column
	 */
	public JMenu getSensorIdMenu(ActionListener al) {
		JMenu ret = new JMenu("" + this.getName());
		for(int i = 0; i < table.getColumnCount()-1; i++) {
			CouplingMenuItem stimItem  = new CouplingMenuItem("Column " + (i + 1), new SensoryCoupling(this, new String[] {"" + i}));
			stimItem.addActionListener(al);
			ret.add(stimItem);				
		}
		return ret;
	}

	/**
	 * Unused stub; data worlds don't receive commands
	 */
	public void setMotorCommand(String[] commandList, double value) {		
	}


	/**
	 * Unused stub; data worlds don't receive commands
	 */
	public JMenu getMotorCommandMenu(ActionListener al) {
		return null;
	}


	/**
	 * Add a network to this world's list of command targets
	 * That neural net will be updated when the world is
	 */
	public void addCommandTarget(NetworkPanel np) {
		if(commandTargets.contains(np) == false) {
			commandTargets.add(np);
		}
	}

	/**
	 * Remove a network from the list of command targets
	 * that are updated when the world is
	 */
	public void removeCommandTarget(NetworkPanel np) {
		commandTargets.remove(np);
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.getParentFrame().setTitle(name);
		this.name = name;
	}
	/**
	 * @return Returns the commandTargets.
	 */
	public ArrayList getCommandTargets() {
		return commandTargets;
	}
	/**
	 * @param commandTargets The commandTargets to set.
	 */
	public void setCommandTargets(ArrayList commandTargets) {
		this.commandTargets = commandTargets;
	}
	/**
	 * @return Returns the model.
	 */
	public TableModel getModel() {
		return model;
	}
	/**
	 * @param model The model to set.
	 */
	public void setModel(TableModel model) {
		this.model = model;
	}
}