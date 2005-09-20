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


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.coupling.Coupling;
import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.world.Agent;
import org.simbrain.world.World;
import org.simbrain.world.dataworld.DataWorldFrame;
import org.simbrain.world.odorworld.OdorWorldAgent;
import org.simbrain.world.odorworld.OdorWorldFrame;

/**
 * <b>Workspace</b> is the high-level container for all Simbrain windows--network, world, and gauge. 
 *  These components are handled here, as are couplings and linkages between them.
 */
public class Workspace extends JFrame implements ActionListener, WindowListener,ComponentListener, MenuListener{

	private JDesktopPane desktop;
	private static final String FS = System.getProperty("file.separator");
	private static final String defaultFile = "." + FS + "simulations" + FS + "sims" + FS + "two_agents.xml";
    private SFileChooser simulationChooser = new SFileChooser("." + FS 
            + "simulations"+ FS + "sims", "xml");
	private File current_file = null;
	
	// Counters used for naming new networks, worlds, and gauges
	private int net_index = 1;
	private int odor_world_index = 1;
	private int data_world_index = 1;
	private int gauge_index = 1;
	
	// Lists of frames
	private ArrayList networkList = new ArrayList();
	private ArrayList odorWorldList = new ArrayList();
	private ArrayList dataWorldList = new ArrayList();
	private ArrayList gaugeList = new ArrayList();

	// Default desktop size
	private int desktopWidth = 1500;
	private int desktopHeight = 1500;
	
	//Default window sizes
	int width = 450;
	int height = 450;
	
	private boolean workspaceChanged = false;
	
	private JMenuItem saveItem = new JMenuItem("Save Workspace");

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
		setJMenuBar(createMenuBar());
		desktop.setPreferredSize(new Dimension(desktopWidth,desktopHeight));

		JScrollPane workspaceScroller = new JScrollPane();
		setContentPane(workspaceScroller);
		workspaceScroller.setViewportView(desktop);
		workspaceScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		workspaceScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		addWindowListener(this);
		
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

			JMenu fileMenu = new JMenu("File");
			fileMenu.addMenuListener(this);
			fileMenu.setMnemonic(KeyEvent.VK_D);
			menuBar.add(fileMenu);
			
			JMenu helpMenu = new JMenu("Help");
			menuBar.add(helpMenu);

			JMenuItem menuItem = new JMenuItem("Open Workspace");
			menuItem.setMnemonic(KeyEvent.VK_O);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_O,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("openWorkspace");
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			
			saveItem.setMnemonic(KeyEvent.VK_S);
			saveItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_S,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			saveItem.setActionCommand("saveWorkspace");
			saveItem.addActionListener(this);
			fileMenu.add(saveItem);
			
			menuItem = new JMenuItem("Save Workspace As");
			menuItem.setActionCommand("saveWorkspaceAs");
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			fileMenu.addSeparator();

			menuItem = new JMenuItem("Clear Workspace");
			menuItem.setActionCommand("clearWorkspace");
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			fileMenu.addSeparator();
			
			menuItem = new JMenuItem("New Network");
			menuItem.setMnemonic(KeyEvent.VK_N);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("newNetwork");
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			
			
			menuItem = new JMenuItem("New World");
			menuItem.setMnemonic(KeyEvent.VK_W);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("newWorld");
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			
			menuItem = new JMenuItem("New DataWorld");
			menuItem.setMnemonic(KeyEvent.VK_D);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("newDataWorld");
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			
			menuItem = new JMenuItem("New Gauge");
			menuItem.setActionCommand("newGauge");
			menuItem.setMnemonic(KeyEvent.VK_G);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			fileMenu.addSeparator();

			menuItem = new JMenuItem("Quit");
			menuItem.setMnemonic(KeyEvent.VK_Q);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			menuItem.setActionCommand("quit");
			menuItem.addActionListener(this);
			fileMenu.add(menuItem);
			
			menuItem = new JMenuItem("Help");
			menuItem.setActionCommand("help");
			menuItem.addActionListener(this);
			helpMenu.add(menuItem);


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
			addOdorWorld();
		} else if (cmd.equals("newDataWorld")) {
			addDataWorld();
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
			if (changesExist() == true) {
				WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(this);
				if (dialog.hasUserCancelled() == false) {
					quit();
				}
			}
		} else if (cmd.equals("help")){
			Utils.showQuickRef();
		}
	}
	

	//TODO Abstract "simbrain_frame" concept
	//		to eliminate redundant code following

	/**
	 * Add a network to the workspace, to be initialized with default values
	 */
	public void addNetwork() {
		NetworkFrame network = new NetworkFrame(this);
		
		network.setTitle("Network " + net_index++);		
		//TODO: Check that network list does not contain this name
		
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
		this.workspaceChanged = true;
		network.addComponentListener(this);
	}

	/**
	 * Add a new world to the workspace, to be initialized with default values
	 */
	public void addOdorWorld() {
		OdorWorldFrame world = new OdorWorldFrame(this);
		world.getWorld().setName("Odor World " + odor_world_index++);
		if(odorWorldList.size() == 0) {
			world.setBounds(505, 35, width, height);
		} else {
			int newx = ((OdorWorldFrame)odorWorldList.get(odorWorldList.size() - 1)).getBounds().x + 40;
			int newy = ((OdorWorldFrame)odorWorldList.get(odorWorldList.size() - 1)).getBounds().y + 40;	
			world.setBounds(newx, newy, width, height);
		}
		
		world.getWorld().setParentWorkspace(this);
		
		addOdorWorld(world);
	}

	/**
	 * Add a world to the workspace
	 * 
	 * @param world the worldFrame to add
	 */
	public void addOdorWorld(OdorWorldFrame world) {
		desktop.add(world);
		odorWorldList.add(world);
		world.setVisible(true);
		try {
			world.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {}
		this.workspaceChanged = true;
		world.addComponentListener(this);
	}

	/**
	 * Add a new world to the workspace, to be initialized with default values
	 */
	public void addDataWorld() {
		DataWorldFrame world = new DataWorldFrame(this);
		world.getWorld().setName("Data world " + data_world_index++);
		  
		if(dataWorldList.size() == 0) {
			world.setBounds(100, 100, width, height);
		} else {
			int newx = ((DataWorldFrame)dataWorldList.get(dataWorldList.size() - 1)).getBounds().x + 40;
			int newy = ((DataWorldFrame)dataWorldList.get(dataWorldList.size() - 1)).getBounds().y + 40;	
			world.setBounds(newx, newy, width, height);
		}
		
		addDataWorld(world);
	}

	/**
	 * Add a world to the workspace
	 * @param world the worldFrame to add
	 */
	public void addDataWorld(DataWorldFrame world) {
		desktop.add(world);
		dataWorldList.add(world);
		world.resize();
		world.setVisible(true);
		try {
			world.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {}
		
		this.workspaceChanged = true;
		
		world.addComponentListener(this);
	}

	/**
	 * Add a new gauge to the workspace, to be initialized with default values
	 */
	public void addGauge() {
		GaugeFrame gauge = new GaugeFrame(this);
		gauge.setName("Gauge " + gauge_index++);		
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
		
		this.workspaceChanged = true;
		gauge.addComponentListener(this);
	}

	//TODO: network specific version of this method?
	/**
	 * Update all gauges
	 */
	public void updateGauges() {
		for(int i = 0; i < getGaugeList().size(); i++) {
			GaugeFrame gauge = (GaugeFrame)getGaugeList().get(i);
			if (gauge.getGaugedVars().getNetworkName() != null) {
				gauge.update();			
			}
		}
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
	public OdorWorldFrame getLastOdorWorld() {
		if (odorWorldList.size() > 0)
			return (OdorWorldFrame)odorWorldList.get(odorWorldList.size()-1);
		else return null;
	}

	/**
	 * @return reference to the last world added to this workspace
	 */
	public DataWorldFrame getLastDataWorld() {
		if (dataWorldList.size() > 0)
			return (DataWorldFrame)dataWorldList.get(dataWorldList.size()-1);
		else return null;
	}

	/**
	 * @return reference to the last gauge added to this workspace
	 */
	public GaugeFrame getLastGauge() {
		if (gaugeList.size() > 0)
			return (GaugeFrame)gaugeList.get(gaugeList.size()-1);
		else return null;
	}
	
	/**
	 * Return a named gauge, null otherwise
	 */
	public GaugeFrame getGauge(String name) {
		for(int i = 0; i < getGaugeList().size(); i++) {
			GaugeFrame gauge = (GaugeFrame)getGaugeList().get(i);
			if (gauge.getName().equals(name)) {
				return gauge;
			}
		}
		return null;
	}
	
	/**
	 * Get those gauges gauged by the given network
	 */
	public ArrayList getGauges(NetworkFrame net) {
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < gaugeList.size(); i++) {
			GaugeFrame gauge = (GaugeFrame)gaugeList.get(i);
			if (gauge.getGaugedVars().getNetworkName() != null) {
				if (gauge.getGaugedVars().getNetworkName().equals(net.getName())) {
					ret.add(gauge);			
				}				
			}
		}
		return ret;
	}
	
	/**
	 * Return a named network, null otherwise
	 */
	public NetworkFrame getNetwork(String name) {
		for(int i = 0; i < getNetworkList().size(); i++) {
			NetworkFrame network = (NetworkFrame)getNetworkList().get(i);
			if (network.getName().equals(name)) {
				return network;
			}
		}
		return null;
	}
	
	/**
	 * Remove all items (networks, worlds, etc.) from this workspace
	 */
	public void clearWorkspace() {
	    
		if (changesExist() == true) {
			WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(this);
			if (dialog.hasUserCancelled() == true) {
				return;
			}
		} 
		disposeAllFrames();
		current_file = null;
		this.setTitle("Simbrain");
	}
	
	public void disposeAllFrames(){
		
		net_index = 1; 
		data_world_index = 1;
		odor_world_index = 1;
		gauge_index = 1;
		
		//TODO: Is there a cleaner way to do this?  I have to use this while loop
		// because the windowclosing itself removes a window
		while(networkList.size() > 0) {
			for(int i = 0; i < networkList.size(); i++) {
				try {
					((NetworkFrame)networkList.get(i)).setClosed(true);
				} catch (java.beans.PropertyVetoException e) {}
			}
		}

		while(odorWorldList.size() > 0) {			
			for(int i = 0; i < odorWorldList.size(); i++) {
				try {
					((OdorWorldFrame)odorWorldList.get(i)).setClosed(true);
				} catch (java.beans.PropertyVetoException e) {}
			}		
		}
		
		while(dataWorldList.size() > 0) {			
			for(int i = 0; i < dataWorldList.size(); i++) {
				try {
					((DataWorldFrame)dataWorldList.get(i)).setClosed(true);
				} catch (java.beans.PropertyVetoException e) {}
			}		
		}
		
		while(gaugeList.size() > 0) {
			for(int i = 0; i < gaugeList.size(); i++) {
				try {
					((GaugeFrame)gaugeList.get(i)).setClosed(true);
				} catch (java.beans.PropertyVetoException e) {}
			}					
		}
		
		
	}
	

	/**
	 * Shows the dialog for opening a simulation file
	 */
	public void showOpenFileDialog() {
		File simFile = simulationChooser.showOpenDialog();
		if(simFile != null){
		    WorkspaceSerializer.readWorkspace(this, simFile);
		    current_file = simFile;
		}
	}

	/**
	 * Shows the dialog for saving a workspace file
	 */
	public void showSaveFileAsDialog(){
		workspaceChanged = false;
		if(changesExist() == true){
			WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);
			if(theDialog.hasUserCancelled() == true){
				return;
			}
		}
	    File simFile = simulationChooser.showSaveDialog();
	    if(simFile != null){
	    		WorkspaceSerializer.writeWorkspace(this, simFile);
	    		current_file = simFile;
	    }
	}
	
	/**
	 * Show the save dialog 
	 *
	 */
	public void saveFile(){
		
		if(changesExist() == true){
			WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);
			if(theDialog.hasUserCancelled() == true){
				return;
			}
		}
		if(current_file != null){
			WorkspaceSerializer.writeWorkspace(this, current_file);
		} else {
			showSaveFileAsDialog();
		}
	}

    /**
	 * Repaint all open network panels. Useful when workspace changes happen
	 * that need to be broadcast; also essential when default workspace is
	 * initially opened
	 * 
	 */
	public void repaintAllNetworks() {

		for (int j = 0; j < getNetworkList().size(); j++) {
			NetworkFrame net = (NetworkFrame) getNetworkList().get(j);
			net.getNetPanel().repaint();
		}

	}
		       

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI() {
			//Make sure we have nice window decorations.
			//JFrame.setDefaultLookAndFeelDecorated(true);

			//Create and set up the window.
			Workspace sim = new Workspace();
			sim.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			//Display the window.
			sim.setVisible(true);
			
			//Now that all frames are open, repaint alll Piccolo PCanvases
			sim.repaintAllNetworks();
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
	 * @return Returns the worldFrameList.
	 */
	public ArrayList getWorldFrameList() {
		ArrayList ret = new ArrayList();
		ret.addAll(odorWorldList);
		ret.addAll(dataWorldList);
		return ret;
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
	
	/**
	 * Get a list of all agents in the workspace
	 * 
	 * @return the list of agents
	 */
	public ArrayList getAgentList() {
		
		ArrayList ret = new ArrayList();
		//Go through worlds, and get each of their agent lists
		for(int i = 0; i < getWorldList().size(); i++) {
			World wld = (World)getWorldList().get(i);
			ret.addAll(wld.getAgentList());
		}
		return ret;
	}

	// TODO: Single agent concept in World
	
	/**
	 * Returns a menu which shows what possible sources there are for motor couplings in
	 * this workspace.  
	 */
	public JMenu getMotorCommandMenu(ActionListener al,PNodeNeuron theNode) {

		JMenu ret = new JMenu("Motor Commands");
		
		for(int i = 0; i < getWorldFrameList().size(); i++) {
			World wld = (World)getWorldList().get(i);
			JMenu wldMenu = wld.getMotorCommandMenu(al);
			if (wldMenu == null) continue;
			ret.add(wldMenu);
		}
		
		JMenuItem notOutputItem = new JMenuItem("Not output");
		notOutputItem.addActionListener(al);
		notOutputItem.setActionCommand("Not output");
		if (theNode.isOutput())
			ret.add(notOutputItem);

		return ret;
	}
	
	/**
	 * Returns a menu which shows what possible sources there are for sensory couplings in
	 * this workspace.  
	 */
	public JMenu getSensorIdMenu(ActionListener al,PNodeNeuron theNode) {
		JMenu ret = new JMenu("Sensors");
				
		for(int i = 0; i < getWorldFrameList().size(); i++) {
			World wld = (World)getWorldList().get(i);
			JMenu wldMenu = wld.getSensorIdMenu(al);
			if (wldMenu == null) continue;
			ret.add(wldMenu);
		}		
		
		JMenuItem notInputItem = new JMenuItem("Not input");
		notInputItem.addActionListener(al);
		notInputItem.setActionCommand("Not input");
		if(theNode.isInput())
			ret.add(notInputItem);
		
		return ret;
	}	
	
	/**
	 * Returns a menu which shows what gauges are currently in the workspace
	 * Returns null if ther are no gauges
	 */
	public JMenu getGaugeMenu(ActionListener al) {
		
		if (getGaugeList().size() == 0) return null;
		
		JMenu ret = new JMenu("Set Gauge");
		
		for(int i = 0; i < getGaugeList().size(); i++) {
			JMenuItem temp = new JMenuItem(((GaugeFrame)getGaugeList().get(i)).getName());
			temp.setActionCommand("Gauge:" + temp.getText());
			temp.addActionListener(al);
			ret.add(temp);
		}		
		
		return ret;
	}	
	
	/**
	 * Associates a coupling with a matching agent in the current workspace. 
	 * Returns null if no such agent can be found. This method is used when
	 * opening networks, to see if any agents match the network's current
	 * couplings.
	 * 
	 *  1) Try to find a matching world-type, world-name, and agent-name
	 *  2) Try to find a matching world-type and agent-name
	 *  3) Try to find a matching world-type and any agent
	 * 
	 * @param c a temporary coupling which holds an agent-name, agent-type, and world-name
	 * 
	 * @return a matching agent, or null of none is found
	 */
	public OdorWorldAgent findMatchingAgent(Coupling c) {
		//First go for a matching agent in the named world
		for(int i = 0; i < getWorldFrameList().size(); i++) {
			OdorWorldFrame wld = (OdorWorldFrame)getWorldFrameList().get(i);
			if (c.getWorldName().equals(wld.getWorld().getName()) && (c.getWorldType().equals(wld.getWorld().getType()))) {
				for(int j = 0; j < wld.getAgentList().size(); j++) {
					OdorWorldAgent a = (OdorWorldAgent)wld.getAgentList().get(j);
					if(c.getAgentName().equals(a.getName())) {
						return a;
					}
				}				
			}
		}		
		
		//Then go for any matching agent
		for(int i = 0; i < getAgentList().size(); i++) {
				OdorWorldAgent a = (OdorWorldAgent)getAgentList().get(i);
				if(c.getAgentName().equals(a.getName()) && 
						(c.getWorldType().equals(a.getParentWorld().getType()))) {
					return a;
				}
		}		

		//Finally go for any matching world-type and ANY agent
		for(int i = 0; i < getAgentList().size(); i++) {
				OdorWorldAgent a = (OdorWorldAgent)getAgentList().get(i);
				if((c.getWorldType().equals(a.getParentWorld().getType()))) {
					return a;
				}
		}		

		//Otherwise give up
		return null;
	}

	
	/**
	 * Look for "null" couplings (couplings with no agent field), and try to find
	 * suitable agents to attach them to.  These can occur when a neuron's
	 * coupling field stay alive but a world is changed (e.g., an agent is deleted).
	 * Later, when a new world is opened, for example, this method is called
	 * so that the agents in those worlds can be attached to null couplings.
	 * 
	 * More specifically, attach agents to to couplings where 
	 * 	(1) the agent field is null
	 * 	(2) the agent's worldtype matches, and 
	 *  (3) the agent's name matches
	 * 
	 * 
	 * @param couplings the set of couplings to check
	 */
	public void attachAgentsToCouplings(ArrayList couplings) {
		
		for(int i = 0; i < couplings.size(); i++) {
			Coupling c = (Coupling)couplings.get(i);
			for(int j = 0; j < getAgentList().size(); j++) {				
				Agent a = (Agent)getAgentList().get(j);
				// if world-type and agent name matches, add this agent to the coupling				
				if ((c.getAgent() == null) &&
					c.getAgentName().equals(a.getName()) &&
					c.getWorldType().equals(a.getParentWorld().getType())) {
						c.setAgent(a);
						c.getAgent().getParentWorld().addCommandTarget(c.getNeuron().getParentPanel());
						break;
				}
			}
		}
		
		resetCommandTargets();
	}
	
	/**
	 * When a new world is opened, see if any open networks have "null" couplings 
	 * that that world's agents can attach to.
	 */
	public void attachAgentsToCouplings() {
		attachAgentsToCouplings(getCouplingList());
		repaintAllNetworks();
	}
	
	/**
	 * Remove all given agents from the couplng list, by setting the agent field on those
	 * couplings to null.
	 * 
	 * @param w the world whose agents should be removed
	 */
	public void removeAgentsFromCouplings(World w) {
		ArrayList agents = w.getAgentList();
		removeAgentsFromCouplings(agents);
	}
	
	/**
	 * Remove all given agents from the couplng list, by setting the agent field on those
	 * couplings to null.
	 * 
	 * @param agents the list of agents to be removed.
	 */
	public void removeAgentsFromCouplings(ArrayList agents) {
		ArrayList couplings = getCouplingList();
		for (int i = 0; i < couplings.size(); i++ ) {
			for (int j = 0; j < agents.size(); j++ ) {
				if(((Coupling)couplings.get(i)).getAgent() ==  agents.get(j)) {
					((Coupling)couplings.get(i)).setAgent(null);
				}
					
			}
		}	
	}
	

	
	/**
	 * Each world has a list of networks it must update when activities occur in them.
	 * This method clears those lists and resets them based on the current coupling list.
	 * It is invoked, for example, when agents are removed
	 */
	public void resetCommandTargets() {

		// Clear command targets in each world
		for(int i = 0; i < getWorldList().size(); i++) {
			World wld = (World)getWorldList().get(i);
			wld.getCommandTargets().clear();
		}		
		
		// Add command target to each world
		ArrayList couplings = getCouplingList();	
		for (int i = 0; i < couplings.size(); i++) {
			Coupling c = (Coupling)couplings.get(i);
			World w = c.getWorld();
			if (w != null) {
				w.addCommandTarget(c.getNeuron().getParentPanel());				
			}
		}
	}
	
	/**
	 * @return Returns the couplingList.
	 */
	public ArrayList getCouplingList() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < networkList.size(); i++) {
			ret.addAll(((NetworkFrame)networkList.get(i)).getNetPanel().getCouplingList());
		}	
		return ret;
	}
	
	/**
	 * @return Returns the worldList.
	 */
	public ArrayList getWorldList() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < odorWorldList.size(); i++) {
			ret.add(((OdorWorldFrame)odorWorldList.get(i)).getWorld());
		}
		for (int i = 0; i < dataWorldList.size(); i++) {
			ret.add(((DataWorldFrame)dataWorldList.get(i)).getWorld());
		}
		
		return ret;
	}

	/**
	 * @return Returns the dataWorldList.
	 */
	public ArrayList getDataWorldList() {
		return dataWorldList;
	}
	/**
	 * @param dataWorldList The dataWorldList to set.
	 */
	public void setDataWorldList(ArrayList dataWorldList) {
		this.dataWorldList = dataWorldList;
		this.workspaceChanged = true;
	}
	/**
	 * @return Returns the odorWorldList.
	 */
	public ArrayList getOdorWorldList() {
		return odorWorldList;
	}
	/**
	 * @param odorWorldList The odorWorldList to set.
	 */
	public void setOdorWorldList(ArrayList odorWorldList) {
		this.odorWorldList = odorWorldList;
		this.workspaceChanged = true;
	}
	

	/**
	 * Check whether there have been changes in the workspace or its components
	 * 
	 * @return true if changes exist, false otherwise
	 */
	public boolean changesExist() {
		int odorWorldChanges = getOdorWorldChangeList().size();
		int dataWorldChanges = getDataWorldChangeList().size();
		int networkChanges = getNetworkChangeList().size();
		int gaugeChanges = getGaugeChangeList().size();
		
		if ((odorWorldChanges + dataWorldChanges + networkChanges + gaugeChanges) > 0 || workspaceChanged == true) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public ArrayList getOdorWorldChangeList(){
		ArrayList ret = new ArrayList();

		int y = 0;

		for ( int j = 0; j < odorWorldList.size();j++){
			OdorWorldFrame test = (OdorWorldFrame)getOdorWorldList().get(j);
			if (test.isChangedSinceLastSave()){
				ret.add(y,test);
				y++;
			}
		}
		return ret;

	}
	
        public ArrayList getNetworkChangeList() {
		ArrayList ret = new ArrayList();

		int x = 0;
		for (int i = 0; i < networkList.size(); i++) {
			NetworkFrame test = (NetworkFrame) getNetworkList().get(i);
			if (test.isChangedSinceLastSave()) {
				ret.add(x, test);
				x++;
			}
		}

		return ret;

	}
	
	public ArrayList getDataWorldChangeList(){
		ArrayList ret = new ArrayList();

		int z = 0;
		for ( int k = 0; k < dataWorldList.size();k++){
			DataWorldFrame test = (DataWorldFrame)getDataWorldList().get(k);
			if (test.isChangedSinceLastSave()){
				ret.add(z,test);
				z++;
			}
		}
		
		return ret;
	}
	
	
	public ArrayList getGaugeChangeList(){
		ArrayList ret = new ArrayList();
		
		int x = 0;
		for ( int i = 0; i < gaugeList.size();i++){
			GaugeFrame test = (GaugeFrame)getGaugeList().get(i);
			if (test.isChangedSinceLastSave()){
				ret.add(x,test);
				x++;
			}
		}
 
		return ret;
		
	}
	
	protected void quit() {
		NetworkPreferences.saveAll(); // Save all user preferences
		System.exit(0);
	}

	public void windowOpened(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
		if (changesExist() == true) {
			WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(this);
			if (dialog.hasUserCancelled() == true) {
				return;
			}
		}
		quit();
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowIconified(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {
	}

	public void windowActivated(WindowEvent arg0) {
	}

	public void windowDeactivated(WindowEvent arg0) {
	}
	
	public boolean hasWorkspaceChanged(){
		return this.workspaceChanged;
	}
	
	public void setWorkspaceChanged(boolean workspaceChanged){
		this.workspaceChanged = workspaceChanged;
	}

	/**
	 * @return Returns the current_file.
	 */
	public File getCurrentFile() {
		return current_file;
	}
	/**
	 * @param current_file The current_file to set.
	 */
	public void setCurrentFile(File current_file) {
		this.current_file = current_file;
	}


	public void componentHidden(ComponentEvent arg0) {
	}


	public void componentMoved(ComponentEvent arg0) {
		setWorkspaceChanged(true);
	}


	public void componentResized(ComponentEvent arg0) {
		setWorkspaceChanged(true);
	}


	public void componentShown(ComponentEvent arg0) {
	}


	public void menuSelected(MenuEvent arg0) {
		if(changesExist()){
			saveItem.setEnabled(true);
		} else
			saveItem.setEnabled(false);
	}


	public void menuDeselected(MenuEvent arg0) {
		
	}


	public void menuCanceled(MenuEvent arg0) {
		
	}
}
