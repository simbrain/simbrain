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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.simbrain.coupling.Coupling;
import org.simbrain.coupling.MotorCoupling;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.UserPreferences;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.SFileChooser;
import org.simbrain.world.odorworld.Agent;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldFrame;

/**
 * <b>Workspace</b> is the high-level container for all Simbrain windows--network, world, and gauge. 
 *  These components are handled here, as are couplings and linkages between them.
 */
public class Workspace extends JFrame implements ActionListener{

	private JDesktopPane desktop;
	private static final String FS = System.getProperty("file.separator");
	private static final String defaultFile = "." + FS + "simulations" + FS + "sims" + FS + "two_agents.xml";
	File current_file = null;
	
	// Counters used for naming new networks, worlds, and gauges
	private int net_index = 1;
	private int world_index = 1;
	private int gauge_index = 1;
	private int desktopWidth = 1500;
	private int desktopHeight = 1500;
	
	
	//TODO: Make default window size settable, sep for net, world, gauge
	int width = 450;
	int height = 450;
	private ArrayList networkList = new ArrayList();
	private ArrayList worldList = new ArrayList();
	private ArrayList gaugeList = new ArrayList();
	private CouplingList couplingList = new CouplingList();
	
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
//		setContentPane(desktop);
		setJMenuBar(createMenuBar());
		desktop.setPreferredSize(new Dimension(desktopWidth,desktopHeight));

		JScrollPane workspaceScroller = new JScrollPane();
		setContentPane(workspaceScroller);
		workspaceScroller.setViewportView(desktop);
		workspaceScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		workspaceScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		
		//Open initial workspace
		WorkspaceSerializer.readWorkspace(this, new File(defaultFile));

	    //Make dragging a little faster but perhaps uglier.
	    //desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
	}
	
	/**
	 * Repaint all open network panels.  Useful when workspace changes happen 
	 * that need to be broadcast; also essential when default workspace is initially
	 * opened.
	 */
	public void repaintAllNetworkPanels() {
		
		for(int j = 0; j < getNetworkList().size(); j++) {
			NetworkFrame net = (NetworkFrame)getNetworkList().get(j);
			net.getNetPanel().repaint();
		}
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

	}
	
	/**
	 * Add a new world to the workspace, to be initialized with default values
	 */
	public void addWorld() {
		OdorWorldFrame world = new OdorWorldFrame(this);
		world.getWorld().setName("World " + world_index++);
		if(worldList.size() == 0) {
			world.setBounds(505, 35, width, height);
		} else {
			int newx = ((OdorWorldFrame)worldList.get(worldList.size() - 1)).getBounds().x + 40;
			int newy = ((OdorWorldFrame)worldList.get(worldList.size() - 1)).getBounds().y + 40;	
			world.setBounds(newx, newy, width, height);
		}
			
		addWorld(world);
	}

	/**
	 * Add a world to the workspace
	 * 
	 * @param world the worldFrame to add
	 */
	public void addWorld(OdorWorldFrame world) {
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
		gauge.getGauge().setDefaultDir("." + FS + "simulations" + FS + "gauges");
		
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
		gauge.setName("Gauge " + gauge_index++);
		desktop.add(gauge);
		gaugeList.add(gauge);
		gauge.setVisible(true);
		try {
			gauge.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {}
	}

	//TODO: network specific version of this method?
	/**
	 * Update all gauges
	 */
	public void updateGauges() {
		for(int i = 0; i < getGaugeList().size(); i++) {
			GaugeFrame gauge = (GaugeFrame)getGaugeList().get(i);
			if (gauge.getNetworkName() != null) {
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
	public OdorWorldFrame getLastWorld() {
		if (worldList.size() > 0)
			return (OdorWorldFrame)worldList.get(worldList.size()-1);
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
			if (gauge.getNetworkName() != null) {
				if (gauge.getNetworkName().equals(net.getNetPanel().getName())) {
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
			if (network.getNetPanel().getName().equals(name)) {
				return network;
			}
		}
		return null;
	}
	
	/**
	 * Remove all items (networks, worlds, etc.) from this workspace
	 */
	public void clearWorkspace() {
	    
		net_index = 1; 
		world_index = 1;
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

		while(worldList.size() > 0) {			
			for(int i = 0; i < worldList.size(); i++) {
				try {
					((OdorWorldFrame)worldList.get(i)).setClosed(true);
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
		
		couplingList.clear();
		
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
	
	/**
	 * Get a list of all agents in the workspace
	 * 
	 * @return the list of agents
	 */
	public ArrayList getAgentList() {
		
		ArrayList ret = new ArrayList();
		//Go through worlds, and get each of their agent lists
		for(int i = 0; i < getWorldList().size(); i++) {
			OdorWorldFrame wld = (OdorWorldFrame)getWorldList().get(i);
			for(int j = 0; j < wld.getAgentList().size(); j++) {
				ret.add(wld.getAgentList().get(j));
			}
		}
		return ret;
	}
	
	/**
	 * Returns a menu which shows what possible sources there are for motor couplings in
	 * this workspace.  
	 */
	public JMenu getMotorCommandMenu(ActionListener al) {
		JMenu ret = new JMenu("Motor Commands");
		
		for(int i = 0; i < getWorldList().size(); i++) {
			OdorWorldFrame wld = (OdorWorldFrame)getWorldList().get(i);
			JMenu wldMenu = new JMenu(wld.getWorld().getName());
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
	 * Returns a menu which shows what possible sources there are for sensory couplings in
	 * this workspace.  
	 */
	public JMenu getSensorIdMenu(ActionListener al) {
		JMenu ret = new JMenu("Sensors");
				
		for(int i = 0; i < getWorldList().size(); i++) {
			OdorWorldFrame wld = (OdorWorldFrame)getWorldList().get(i);
			JMenu wldMenu = new JMenu(wld.getWorld().getName());
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
	
	//TODO: Later, also check for world type
	/**
	 * Given a world-name and angent-name (Stored in a temporary coupling object), find a matching
	 * world-agent pair or, failing that, an agent which matches.  Otherwise return null. 
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
			OdorWorldFrame wld = (OdorWorldFrame)getWorldList().get(i);
			if (c.getWorldName().equals(wld.getWorld().getName())) {
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
	
	/**
	 * When a new world is opened, see if any open networks have "null" couplings that that world's
	 * agents can attach to.
	 */
	public void attachAgentsToCouplings() {
		
		CouplingList nullCouplings = couplingList.getNullAgentCouplings();
		
		for(int i = 0; i < nullCouplings.size(); i++) {
			Coupling c = nullCouplings.getCoupling(i);
			for(int j = 0; j < getAgentList().size(); j++) {
				Agent a = (Agent)getAgentList().get(j);
				// if the agent name matches, add this agent to the coupling
				if (c.getAgentName().equals(a.getName())) {
					c.setAgent(a);
					c.getAgent().getParent().addCommandTarget(c.getNeuron().getParentPanel());
					break;
				}
			}
		}
		
		resetCommandTargets();
	}
	
	
	/**
	 * Each world has a list of networks it must update when activities occur in them.
	 * This method clears those lists and resets them based on the current coupling list.
	 */
	public void resetCommandTargets() {
		//TODO: 	There may be a way to do this via coupling.constructor, or 
		//			couplingList			
		//
		//	OR: agents could have references to couplings, and could update those when the world is updated

		// Clear command targets in each world
		for(int i = 0; i < getWorldList().size(); i++) {
			OdorWorldFrame wld = (OdorWorldFrame)getWorldList().get(i);
			wld.getWorld().getCommandTargets().clear();
		}		
		
		// Add command target to each world
		CouplingList couplings = getCouplingList();	
		for (int i = 0; i < couplings.size(); i++) {
			Coupling c = couplings.getCoupling(i);
			OdorWorld w = c.getWorld();
			if (w != null) {
				w.addCommandTarget(c.getNeuron().getParentPanel());				
			}
		}
	}
	
	/**
	 * @return Returns the couplingList.
	 */
	public CouplingList getCouplingList() {
		return couplingList;
	}
	/**
	 * @param couplingList The couplingList to set.
	 */
	public void setCouplingList(CouplingList couplingList) {
		this.couplingList = couplingList;
	}
}
