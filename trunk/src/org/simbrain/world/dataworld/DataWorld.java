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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.simbrain.coupling.CouplingMenuItem;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.network.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.world.Agent;
import org.simbrain.world.World;


/**
 * @author rbartley
 *
 * <b>DataWorld</b> creates a table and then adds it to the viewport.
 */
public class DataWorld extends JPanel implements MouseListener,World, Agent, KeyListener {

	private TableModel model = new TableModel(this);
	private JTable table = new JTable(model);
	private DataWorldFrame parentFrame;
	
	// List of neural networks to update when this world is updated
	private ArrayList commandTargets = new ArrayList();

	private int upperBound = 0;
	private int lowerBound = 0;
	
	private int current_row = 1;
	private String name;

	private Point selectedPoint;
	
	private JMenuItem addRow = new JMenuItem("Insert row");
	private JMenuItem addCol = new JMenuItem("Insert column");
	private JMenuItem remRow = new JMenuItem("Delete row");
	private JMenuItem remCol = new JMenuItem("Delete column");
	private JMenuItem changeName = new JMenuItem("Edit button text");
	
	
	public DataWorld(DataWorldFrame ws) {
		super(new BorderLayout());
		setParentFrame(ws);
		table.getColumnModel().getColumn(0).setCellRenderer(
				new ButtonRenderer(table.getDefaultRenderer(JButton.class)));
		table.addMouseListener(this);
		this.add("Center", table);
		
		addRow.addActionListener(parentFrame);
		addRow.setActionCommand("addRowHere");
		addCol.addActionListener(parentFrame);
		addCol.setActionCommand("addColHere");
		remRow.addActionListener(parentFrame);
		remRow.setActionCommand("remRowHere");
		remCol.addActionListener(parentFrame);
		remCol.setActionCommand("remColHere");
		changeName.addActionListener(parentFrame);
		changeName.setActionCommand("changeButtonName");
		
		table.addKeyListener(this);
	}


	public void resetModel(String[][] data) {
		model = new TableModel(data);
		table.setModel(model);
		table.getColumnModel().getColumn(0).setCellRenderer(
				new ButtonRenderer(table.getDefaultRenderer(JButton.class)));

		parentFrame.resize();
	}
	
	public void changeButtonName(final JButton button){
		final JDialog getName = new JDialog();
		final JTextField name = new JTextField();
		JPanel buttonPanel = new JPanel();
		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		
		ActionListener tempList = new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if(arg0.getActionCommand().equals("ok")){
					if(name.getText().length() != 0){
						button.setText(name.getText());
						getName.dispose();
					}
					repaint();
				} else if (arg0.getActionCommand().equals("cancel")){
					getName.dispose();
				}
			}
			
		};
		
		getName.getContentPane().setLayout(new BorderLayout());
		getName.setTitle("Enter Text");
		name.setSize(25,95);
		getName.getContentPane().add(name,BorderLayout.CENTER);
		getName.setModal(true);
		getName.setResizable(false);
		getName.setSize(150,100);
		ok.addActionListener(tempList);
		ok.setActionCommand("ok");
		buttonPanel.add(ok);
		getName.getRootPane().setDefaultButton(ok);
		cancel.addActionListener(tempList);
		cancel.setActionCommand("cancel");
		buttonPanel.add(cancel);
		getName.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		getName.setLocationRelativeTo(null);

		getName.setVisible(true);
	}

	public void mouseClicked(MouseEvent e) {
		//This makes the buttons act like buttons instead of images
		Point point = e.getPoint();
		if (table.columnAtPoint(point) == 0 && !(e.isControlDown() == true || e.getButton() == 3)) {
			current_row = table.rowAtPoint(point);
			updateNetwork();
		} else
			return;
		
	}

	public void mousePressed(MouseEvent e) {
		
		selectedPoint = e.getPoint();
		
		if((e.getButton() == MouseEvent.BUTTON3) || e.isControlDown()){
			JPopupMenu menu  = buildPopupMenu();
			menu.show(this, (int)selectedPoint.getX(), (int)selectedPoint.getY());
		}
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
	
	public JPopupMenu buildPopupMenu(){
		JPopupMenu ret = new JPopupMenu();
		
		ret.add(addRow);
		if(this.getTable().columnAtPoint(selectedPoint) != 0)
			ret.add(addCol);
		ret.add(remRow);
		if(this.getTable().columnAtPoint(selectedPoint) != 0)
			ret.add(remCol);
		if(this.getTable().columnAtPoint(selectedPoint) == 0)
			ret.add(changeName);
		
		return ret;
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
		int i = Integer.parseInt(sensor_id[0]) - 1;
		String snum = new String("" + table.getModel().getValueAt(current_row, i + 1));
		return Double.parseDouble(snum);
	}

	/**
	 * Returns a menu with on id, "Column X" for each column
	 */
	public JMenu getSensorIdMenu(ActionListener al) {
		JMenu ret = new JMenu("" + this.getName());
		for(int i = 0; i < table.getColumnCount()-1; i++) {
			CouplingMenuItem stimItem  = new CouplingMenuItem("Column " + (i + 1), new SensoryCoupling(this, new String[] {"" + (i + 1)}));
			stimItem.addActionListener(al);
			ret.add(stimItem);				
		}
		return ret;
	}
	
	public void randomize(){
		if (upperBound <= lowerBound){
			displayRandomizeDialog();
		}
		for(int i=1; i<table.getColumnCount();i++){
			for(int j=0; j<table.getRowCount();j++){
				table.setValueAt(randomInteger(),j,i);
			}
		}
	}

	public Double randomInteger(){
		if (upperBound >= lowerBound){
			double drand = Math.random();
			drand = drand*(upperBound - lowerBound) + lowerBound;
			Double element = new Double(drand);
			return element;
		}
		return new Double(0);
	}
	
	public void displayRandomizeDialog(){
		StandardDialog rand = new StandardDialog(this.getParentFrame().getWorkspace(), "randomize Bounds");
		JPanel pane = new JPanel();
		JTextField lower = new JTextField();
		JTextField upper = new JTextField();
		lower.setText(Integer.toString(getLowerBound()));
		lower.setColumns(3);
		upper.setText(Integer.toString(getUpperBound()));
		upper.setColumns(3);
		pane.add(new JLabel("Lower Bound"));
		pane.add(lower);
		pane.add(new JLabel("Upper Bound"));
		pane.add(upper);
		
		rand.setContentPane(pane);
		
		rand.pack();
		
		rand.setLocationRelativeTo(getParentFrame());
		rand.setVisible(true);
		
		if(!rand.hasUserCancelled()){
			setLowerBound(Integer.parseInt(lower.getText()));
			setUpperBound(Integer.parseInt(upper.getText()));
		}
		repaint();
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


	public int getLowerBound() {
		return lowerBound;
	}


	public void setLowerBound(int lowerBound) {
		this.lowerBound = lowerBound;
	}


	public int getUpperBound() {
		return upperBound;
	}


	public void setUpperBound(int upperBound) {
		this.upperBound = upperBound;
	}


	public int getCurrent_row() {
		return current_row;
	}


	public void setCurrent_row(int current_row) {
		this.current_row = current_row;
	}


	public Point getSelectedPoint() {
		return selectedPoint;
	}


	public void setSelectedPoint(Point selectedPoint) {
		this.selectedPoint = selectedPoint;
	}

	public void keyTyped(KeyEvent arg0) {
		this.getParentFrame().setChangedSinceLastSave(true);
		
	}


	public void keyPressed(KeyEvent arg0) {
	}


	public void keyReleased(KeyEvent arg0) {
	}


}