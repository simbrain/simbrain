/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.workspace;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.simbrain.coupling.Coupling;
import org.simbrain.coupling.MotorCoupling;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.UserPreferences;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.SFileChooser;
import org.simbrain.world.Agent;
import org.simbrain.world.WorldFrame;

public class Workspace extends JFrame implements ActionListener{

	private JDesktopPane desktop;
	private static final String FS = System.getProperty("file.separator");
	// File separator.  For platfrom independence.
	private static final String defaultFile = "." + FS + "simulations" + FS + "sims" + FS + "two_agents.xml";
	
	File current_file = null;
	//TODO: Make default window size settable, sep for net, world, gauge
	int width = 450;
	int height = 450;

	private ArrayList networkList = new ArrayList();
	private ArrayList worldList = new ArrayList();
	private ArrayList gaugeList = new ArrayList();

	//TODO: Window closing events remove networks from list
	
	/**
	 * Default constructor
	 */
	public Workspace()
	{
	
		super("Simbrain");

		//Make the big window be indented 50 pixels from each edge
		//of the screen.
		int inset = 50;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(inset, inset,
							screenSize.width  - inset*2,
							screenSize.height - inset*2);

		//Set up the GUI.
		desktop = new JDesktopPane(); //a specialized layered pane
		setContentPane(desktop);
		setJMenuBar(createMenuBar());
		
		//Open initial workspace
		WorkspaceSerializer.readWorkspace(this, new File(defaultFile));

	    //Make dragging a little faster but perhaps uglier.
	    //desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
	}
	
	/**
	 * Build the menu bar
	 * 
	 * @return the menu bar
	 */
	protected JMenuBar createMenuBar() {
			JMenuBar menuBar = new JMenuBar();

			//Set up the lone menu.
			JMenu menu = new JMenu("File");
			menu.setMnemonic(KeyEvent.VK_D);
			menuBar.add(menu);

			//Set up the first  item.
			JMenuItem menuItem = new JMenuItem("Open Workspace");
			menuItem.setMnemonic(KeyEvent.VK_O);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_O,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("openWorkspace");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			
			menuItem = new JMenuItem("Save Workspace");
			menuItem.setMnemonic(KeyEvent.VK_S);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_S,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("saveWorkspace");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			
			menuItem = new JMenuItem("Save Workspace As");
			menuItem.setActionCommand("saveWorkspaceAs");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			menu.addSeparator();

			menuItem = new JMenuItem("Clear Workspace");
			menuItem.setActionCommand("clearWorkspace");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			menu.addSeparator();
			
			menuItem = new JMenuItem("New Network");
			menuItem.setMnemonic(KeyEvent.VK_N);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("newNetwork");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			
			
			menuItem = new JMenuItem("New World");
			menuItem.setMnemonic(KeyEvent.VK_W);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("newWorld");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			
			menuItem = new JMenuItem("New Gauge");
			menuItem.setActionCommand("newGauge");
			menuItem.setMnemonic(KeyEvent.VK_G);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.addActionListener(this);
			menu.add(menuItem);
			menu.addSeparator();

			//Set up the second menu item.
			menuItem = new JMenuItem("Quit");
			menuItem.setMnemonic(KeyEvent.VK_Q);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("quit");
			menuItem.addActionListener(this);
			menu.add(menuItem);

			return menuBar;
	}

	/**
	 *  React to menu selections
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand(); 
		
		if (cmd.equals("newNetwork")){
			addNetwork();
		} else if (cmd.equals("newWorld")) {
			addWorld();
		} else if (cmd.equals("newGauge")) {
			addGauge();
		} else if(cmd.equals("clearWorkspace")){
		    clearWorkspace();
		} else if (cmd.equals("openWorkspace")) {
			showOpenFileDialog();
		} else if (cmd.equals("saveWorkspace")) {
			saveFile();
		} else if (cmd.equals("saveWorkspaceAs")){
		    showSaveFileAsDialog();
		} else if (cmd.equals("quit")) {
			quit();
		}
	}


	/**
	 * Add a network to the workspace, to be initialized with default values
	 */
	public void addNetwork() {
		NetworkFrame network = new NetworkFrame(this);
			
		if(networkList.size() == 0) {
			network.setBounds(5, 35, width, height);
		} else {
			int newx = ((NetworkFrame)networkList.get(networkList.size() - 1)).getBounds().x + 40;
			int newy = ((NetworkFrame)networkList.get(networkList.size() - 1)).getBounds().y + 40;	
			network.setBounds(newx, newy, width, height);
		}			
		
		addNetwork(network);
	}
	
	
	/**
	 * Add a network to the workspace
	 * 
	 * @param network the networkFrame to add
	 */
	public void addNetwork(NetworkFrame network) {
		desktop.add(network);
		networkList.add(network);
		network.setVisible(true); //necessary as of 1.3
		try {
			network.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {}

	}
	
	/**
	 * Add a new world to the workspace, to be initialized with default values
	 */
	public void addWorld() {
		WorldFrame world = new WorldFrame(this);
		world.getWorldRef().setName("World " + worldList.size());
		if(worldList.size() == 0) {
			world.setBounds(505, 35, width, height);
		} else {
			int newx = ((WorldFrame)worldList.get(worldList.size() - 1)).getBounds().x + 40;
			int newy = ((WorldFrame)worldList.get(worldList.size() - 1)).getBounds().y + 40;	
			world.setBounds(newx, newy, width, height);
		}
			
		addWorld(world);
	}

	/**
	 * Add a world to the workspace
	 * 
	 * @param world the worldFrame to add
	 */
	public void addWorld(WorldFrame world) {
		desktop.add(world);
		worldList.add(world);
		world.setVisible(true);
		try {
			world.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {}

	}
	/**
	 * Add a new gauge to the workspace, to be initialized with default values
	 */
	public void addGauge() {
		GaugeFrame gauge = new GaugeFrame(this);

		if(gaugeList.size() == 0) {
			gauge.setBounds(5, 490, 300, 300);
		} else {
			int newx = ((GaugeFrame)gaugeList.get(gaugeList.size() - 1)).getBounds().x + 310;
			int newy = ((GaugeFrame)gaugeList.get(gaugeList.size() - 1)).getBounds().y;	
			gauge.setBounds(newx, newy, 300, 300);
		}		
		
		addGauge(gauge);
	}
	

	/**
	 * Add a gauge to the workspace
	 * 
	 * @param gauge the worldFrame to add
	 */
	public void addGauge(GaugeFrame gauge) {	
		desktop.add(gauge);
		gaugeList.add(gauge);
		gauge.setVisible(true);
		try {
			gauge.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {}
	}
	
	/**
	 * @return reference to the last network added to this workspace
	 */
	public NetworkFrame getLastNetwork() {
		if (networkList.size() > 0)
			return (NetworkFrame)networkList.get(networkList.size()-1);
		else return null;
	}

	/**
	 * @return reference to the last world added to this workspace
	 */
	public WorldFrame getLastWorld() {
		if (worldList.size() > 0)
			return (WorldFrame)worldList.get(networkList.size()-1);
		else return null;
	}
	
	/**
	 * Remove all items (networks, worlds, etc.) from this workspace
	 */
	public void clearWorkspace() {
	    
		for(int i = 0; i < networkList.size(); i++) {
			try {
				((NetworkFrame)networkList.get(i)).setClosed(true);
			} catch (java.beans.PropertyVetoException e) {}
		}
		networkList.clear();

		for(int i = 0; i < worldList.size(); i++) {
			try {
				((WorldFrame)worldList.get(i)).setClosed(true);
			} catch (java.beans.PropertyVetoException e) {}
		}		
		worldList.clear();
		
		for(int i = 0; i < gaugeList.size(); i++) {
			try {
				((GaugeFrame)gaugeList.get(i)).setClosed(true);
			} catch (java.beans.PropertyVetoException e) {}
		}		
		gaugeList.clear();
		
		current_file = null;
		this.setTitle("Simbrain");
	}
	

	/**
	 * Shows the dialog for opening a simulation file
	 */
	public void showOpenFileDialog() {
	    SFileChooser simulationChooser = new SFileChooser("." + FS 
	        	        + "simulations"+ FS + "sims", "xml");
		File simFile = simulationChooser.showOpenDialog();
		if(simFile != null){
		    WorkspaceSerializer.readWorkspace(this, simFile);
		    current_file = simFile;
		}
	}

	/**
	 * Shows the dialog for saving a simulation file
	 */
	public void showSaveFileAsDialog(){
	    SFileChooser simulationChooser = new SFileChooser("." + FS 
    	        + "simulations"+ FS + "sims", "xml");
	    File simFile = simulationChooser.showSaveDialog();
	    if(simFile != null){
	    		WorkspaceSerializer.writeWorkspace(this, simFile);
	    		current_file = simFile;
	    }
	}
	
	public void saveFile(){
	    if(current_file != null){
	        WorkspaceSerializer.writeWorkspace(this, current_file);
	    } else {
	        showSaveFileAsDialog();
	    }
		
	}


	
	/**
	 * Quit the application	 
	 */
	protected void quit() {
			UserPreferences.saveAll(); // Save all user preferences
			System.exit(0);
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 */
	private static void createAndShowGUI() {
			//Make sure we have nice window decorations.
			//JFrame.setDefaultLookAndFeelDecorated(true);

			//Create and set up the window.
			Workspace sim = new Workspace();
			sim.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			//Display the window.
			sim.setVisible(true);
			
			//Now that all frames are open, repaint alll Piccolo PCanvases
			sim.repaintAllNetworkPanels();
	}
	

	/**
	 * Simbrain main method.  Creates a single instance of the Simulation class
	 * 
	 * @param args currently not used
	 */
	public static void main(String[] args) {
		try {
			//UIManager.setLookAndFeel(new MetalLookAndFeel());
			
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
						createAndShowGUI();
				}
			});
		} catch (Exception e) {
			System.err.println("Couldn't set look and feel!");
		}
	}
	

	/**
	 * @return Returns the networkList.
	 */
	public ArrayList getNetworkList() {
		return networkList;
	}
	/**
	 * @param networkList The networkList to set.
	 */
	public void setNetworkList(ArrayList networkList) {
		this.networkList = networkList;
	}
	/**
	 * @return Returns the worldList.
	 */
	public ArrayList getWorldList() {
		return worldList;
	}
	/**
	 * @param worldList The worldList to set.
	 */
	public void setWorldList(ArrayList worldList) {
		this.worldList = worldList;
	}
	/**
	 * @return Returns the gaugeList.
	 */
	public ArrayList getGaugeList() {
		return gaugeList;
	}
	/**
	 * @param gaugeList The gaugeList to set.
	 */
	public void setGaugeList(ArrayList gaugeList) {
		this.gaugeList = gaugeList;
	}
	
	public ArrayList getAgentList() {
		
		ArrayList ret = new ArrayList();
		//Go through worlds, and get each of their agent lists
		for(int i = 0; i < getWorldList().size(); i++) {
			WorldFrame wld = (WorldFrame)getWorldList().get(i);
			for(int j = 0; j < wld.getAgentList().size(); j++) {
				ret.add(wld.getAgentList().get(j));
			}
		}
		return ret;
	}
	
	// Coupling Stuff
	
	/**
	 * Returns a menuItem which shows what possible sources there are for motor couplings in
	 * this workspace.  
	 */
	public JMenu getMotorCommandMenu(ActionListener al) {
		JMenu ret = new JMenu("Motor Commands");
		
		for(int i = 0; i < getWorldList().size(); i++) {
			WorldFrame wld = (WorldFrame)getWorldList().get(i);
			JMenu wldMenu = new JMenu(wld.getWorldRef().getName());
			ret.add(wldMenu);
			for(int j = 0; j < wld.getAgentList().size(); j++) {
				wldMenu.add(((Agent)wld.getAgentList().get(j)).getMotorCommandMenu(al));
			}
		}
		
		JMenuItem notOutputItem = new JMenuItem("Not output");
		notOutputItem.addActionListener(al);
		notOutputItem.setActionCommand("Not output");
		ret.add(notOutputItem);

		
		return ret;
	}
	
	/**
	 * Returns a menuItem which shows what possible sources there are for sensory couplings in
	 * this workspace.  
	 */
	public JMenu getSensorIdMenu(ActionListener al) {
		JMenu ret = new JMenu("Sensors");
				
		for(int i = 0; i < getWorldList().size(); i++) {
			WorldFrame wld = (WorldFrame)getWorldList().get(i);
			JMenu wldMenu = new JMenu(wld.getWorldRef().getName());
			ret.add(wldMenu);
			for(int j = 0; j < wld.getAgentList().size(); j++) {
				wldMenu.add(((Agent)wld.getAgentList().get(j)).getSensorIdMenu(al));
			}
		}		
		
		JMenuItem notInputItem = new JMenuItem("Not input");
		notInputItem.addActionListener(al);
		notInputItem.setActionCommand("Not input");
		ret.add(notInputItem);
		
		return ret;
	}	
	
	//TODO: Later, also check for world type
	/**
	 * Given a world-name and angent-name (Stored in a temporary coupling object), find a matching
	 * world-agent pair or, failing that, an agent whiich matches.  Otherwise return null. 
	 * used by the workspace when opening network with input and output nodes; if a match can 
	 * be found the relevant coupling is created, otherwise no coupling is created (if null)
	 * 
	 * This will connect a node to the FIRST valid agent found
	 * 
	 * @param c a temporary coupling which holds an agent-name and world-name
	 * @return a real coupling which matches the temporary one
	 */
	public Agent getAgentFromTempCoupling(Coupling c) {

		//First go for a matching agent in the named world
		for(int i = 0; i < getWorldList().size(); i++) {
			WorldFrame wld = (WorldFrame)getWorldList().get(i);
			if (c.getWorldName().equals(wld.getWorldRef().getName())) {
				for(int j = 0; j < wld.getAgentList().size(); j++) {
					Agent a = (Agent)wld.getAgentList().get(j);
					if(c.getAgentName().equals(a.getName())) {
						return a;
					}
				}				
			}
		}		
		
		//Then go for any matching agent
		for(int i = 0; i < getAgentList().size(); i++) {
				Agent a = (Agent)getAgentList().get(i);
				if(c.getAgentName().equals(a.getName())) {
					return a;
				}
		}		
	
		//Otherwise give up
		return null;
	}
	

	//TODO:  Should be called on windown closing event.
	/**
	 * Scans open networks for uncoupled nodes and elminates relevant couplings
	 */
	public void resetCoupledNodes() {

			for(int j = 0; j < getNetworkList().size(); j++) {
				NetworkFrame net = (NetworkFrame)getNetworkList().get(j);
				for(int k = 0; k < net.getNetPanel().getInputList().size(); k++) {
					PNodeNeuron pn = (PNodeNeuron)net.getNetPanel().getInputList().get(k);
					SensoryCoupling sc = pn.getSensoryCoupling();
					if (sc.getAgent() == null) {
						System.out.println("HERE");
						pn.setOutput(false);
					}
				}
				for(int k = 0; k < net.getNetPanel().getOutputList().size(); k++) {
					PNodeNeuron pn = (PNodeNeuron)net.getNetPanel().getOutputList().get(k);
					MotorCoupling mc = pn.getMotorCoupling();
					if (mc.getAgent() == null) {
						pn.setOutput(false);
					}
				}
				
			}
	}

	//Temp fix
	public void removeCoupledNodes() {
		
		for(int j = 0; j < getNetworkList().size(); j++) {
			NetworkFrame net = (NetworkFrame)getNetworkList().get(j);
			for(int k = 0; k < net.getNetPanel().getNeuronList().size(); k++) {
				PNodeNeuron pn = (PNodeNeuron)net.getNetPanel().getNeuronList().get(k);
				pn.setInput(false);
				pn.setOutput(false);
			}
		}
	}
	
	// Otherwise network PCanvases don't show up initially
	private void repaintAllNetworkPanels() {
		
		for(int j = 0; j < getNetworkList().size(); j++) {
			NetworkFrame net = (NetworkFrame)getNetworkList().get(j);
			net.getNetPanel().repaint();
		}
	}

}
