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

package org.simbrain.network;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.GaugeFrame;
import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;

import calpa.html.CalHTMLPane;
import calpa.html.CalHTMLPreferences;

/**
 * This frame contains a neural network 
 */
public class NetworkFrame extends JInternalFrame
	implements ActionListener, MenuListener, InternalFrameListener {

	private static final String FS = System.getProperty("file.separator");
	
	private Workspace workspace;
	private NetworkPanel netPanel = new NetworkPanel(this);	
	
	// For workspace persistence 
	private String path = null;
	private int xpos;
	private int ypos;
	private int the_width;
	private int the_height;

	private boolean changedSinceLastSave = false;
	
	JMenuBar mb = new JMenuBar();
	JMenu fileMenu = new JMenu("File  ");
	JMenuItem newNetSubmenu = new JMenu("New");
	JMenuItem newWTAItem = new JMenuItem("Winner take all network");
	JMenuItem newHopfieldItem = new JMenuItem("Hopfield network");
	JMenuItem newBackpropItem = new JMenuItem("Backprop network");
	JMenuItem openNetItem = new JMenuItem("Open");
	JMenuItem saveNetItem = new JMenuItem("Save");
	JMenuItem saveAsItem = new JMenuItem("Save As");
	JMenuItem close = new JMenuItem("Close");
	JMenu editMenu = new JMenu("Edit  ");
	JMenuItem copyItem = new JMenuItem("Copy Selection");
	JMenuItem pasteItem = new JMenuItem("Paste Selection");
	JMenuItem setNeuronItem = new JMenuItem("Set Neuron(s)");
	JMenuItem setWeightItem = new JMenuItem("Set Weight(s)");
	JMenuItem selectAll = new JMenuItem("Select All");
	JMenuItem alignSubmenu = new JMenu("Align");
	JMenuItem alignHorizontal = new JMenuItem("Horizontal");
	JMenuItem alignVertical = new JMenuItem("Vertical");
	JMenuItem spacingSubmenu = new JMenu("Spacing");
	JMenuItem spacingHorizontal = new JMenuItem("Horizontal");
	JMenuItem spacingVertical = new JMenuItem("Vertical");
	JMenuItem setInOutItem = new JCheckBoxMenuItem("Show I/O Info", false);	
	JMenuItem setAutozoom = new JCheckBoxMenuItem("Autozoom", true);	
	JMenuItem prefsItem = new JMenuItem("Preferences");
	JMenu gaugeMenu = new JMenu("Gauges  ");
	JMenuItem addGaugeItem = new JMenuItem("Add Gauge");
	JMenu helpMenu = new JMenu("Help");
	JMenuItem quickRefItem = new JMenuItem("Simbrain Help");

	
	public NetworkFrame() {	
	}

	public NetworkFrame(Workspace ws) {

		workspace = ws;
		init();
	}
	
	public void init() {
 
		this.setResizable(true);
		this.setMaximizable(true);
		this.setIconifiable(true);
		this.setClosable(true);	
		setUpMenus();
		this.getContentPane().add("Center", netPanel);
		this.addInternalFrameListener(this);
	}

	
	/**
	 * Sets up the main menu bar
	 */
	private void setUpMenus() {

		this.setJMenuBar(mb);
		
		mb.add(fileMenu);
		fileMenu.add(newNetSubmenu);
		newNetSubmenu.add(newWTAItem);
		newWTAItem.addActionListener(this);
		newNetSubmenu.add(newHopfieldItem);
		newHopfieldItem.addActionListener(this);
		newNetSubmenu.add(newBackpropItem);
		newBackpropItem.addActionListener(this);
		fileMenu.addSeparator();
		openNetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(openNetItem);
		openNetItem.addActionListener(this);
		saveNetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(saveNetItem);
		saveNetItem.addActionListener(this);
		fileMenu.add(saveAsItem);
		saveAsItem.addActionListener(this);
		fileMenu.add(close);
		close.addActionListener(this);
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.addMenuListener(this);

		mb.add(editMenu);
		copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		editMenu.add(copyItem);
		copyItem.addActionListener(this);
		pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		editMenu.add(pasteItem);
		pasteItem.addActionListener(this);
		editMenu.addSeparator();
		selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		editMenu.add(selectAll);
		selectAll.addActionListener(this);
		editMenu.addSeparator();
		editMenu.add(alignSubmenu);
		alignSubmenu.add(alignHorizontal);
		alignHorizontal.addActionListener(this);
		alignSubmenu.add(alignVertical);
		alignVertical.addActionListener(this);
		editMenu.add(spacingSubmenu);
		spacingSubmenu.add(spacingHorizontal);
		spacingHorizontal.addActionListener(this);
		spacingSubmenu.add(spacingVertical);
		spacingVertical.addActionListener(this);
		editMenu.addSeparator();
		editMenu.add(setNeuronItem);
		setNeuronItem.addActionListener(this);
		editMenu.add(setWeightItem);
		setWeightItem.addActionListener(this);
		editMenu.addSeparator();
		editMenu.add(setInOutItem);
		setInOutItem.addActionListener(this);
		editMenu.add(setAutozoom);
		setAutozoom.addActionListener(this);
		editMenu.addSeparator();
		editMenu.add(prefsItem);
		prefsItem.addActionListener(this);
		editMenu.addMenuListener(this);
		
		mb.add(gaugeMenu);
		gaugeMenu.add(addGaugeItem);
		addGaugeItem.addActionListener(this);
		gaugeMenu.addMenuListener(this);
		
		mb.add(helpMenu);
		quickRefItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		helpMenu.add(quickRefItem);
		quickRefItem.addActionListener(this);

	}

	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if( (e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class) ) {

			JMenuItem jmi = (JMenuItem) e.getSource();
			
			if(jmi == openNetItem)  {
				netPanel.open();
				changedSinceLastSave = false;
			} else if(jmi == saveAsItem)  {
				netPanel.saveAs();
				changedSinceLastSave = false;
			} else if(jmi == saveNetItem)  {
				netPanel.save();
				changedSinceLastSave = false;
			} else if(jmi == selectAll)  {
				netPanel.getHandle().selectAll();
			} else if(jmi == prefsItem)  {
				netPanel.showNetworkPrefs();
				changedSinceLastSave = true;
			} else if(jmi == setNeuronItem)  {
				netPanel.showNeuronPrefs();
				changedSinceLastSave = true;
			} else if(jmi == setWeightItem)  {
				netPanel.showWeightPrefs();
				changedSinceLastSave = true;
			} else if(jmi == setInOutItem)  {
				//netPanel.showInOut(setInOutItem.isSelected());
			} else if(jmi == setAutozoom)  {
				netPanel.setAutoZoom(setAutozoom.isSelected());
				netPanel.repaint();
				changedSinceLastSave = true;
			} else if(jmi == prefsItem)  {
				netPanel.showNetworkPrefs();
				changedSinceLastSave = true;
			} else if(jmi == addGaugeItem)  {
				netPanel.addGauge();
				changedSinceLastSave = true;
			} else if(jmi == newWTAItem)  {
				netPanel.showWTADialog();
				changedSinceLastSave = true;
			} else if(jmi == newHopfieldItem)  {
				netPanel.showHopfieldDialog();
				changedSinceLastSave = true;
			} else if(jmi == newBackpropItem)  {
				netPanel.showBackpropDialog();
				changedSinceLastSave = true;
			} else if(jmi == copyItem)  {
				netPanel.getHandle().copyToClipboard();
			} else if(jmi == pasteItem)  {
				netPanel.getHandle().pasteFromClipboard();
				changedSinceLastSave = true;
			} else if(jmi == alignHorizontal)  {
				netPanel.alignHorizontal();
				changedSinceLastSave = true;
			} else if(jmi == alignVertical)  {
				netPanel.alignVertical();
				changedSinceLastSave = true;
			} else if(jmi == spacingHorizontal)  {
				netPanel.spacingHorizontal();
				changedSinceLastSave = true;
			} else if(jmi == spacingVertical)  {
				netPanel.spacingVertical();
				changedSinceLastSave = true;
			} else if(jmi == quickRefItem)  {
				showQuickRef();
			} else if(jmi == close){
				if(isChangedSinceLastSave()){
					hasChanged();
				}
				dispose();
			}
		}
		
		
	}

	
	public void internalFrameOpened(InternalFrameEvent e){
	}
	
	public void internalFrameClosing(InternalFrameEvent e){
		if(isChangedSinceLastSave()){
			hasChanged();
		}

	}

	public void internalFrameClosed(InternalFrameEvent e){
		
		this.getNetPanel().resetNetwork();
		this.getWorkspace().getCouplingList().removeCouplings(this.getNetPanel());
		this.getWorkspace().getNetworkList().remove(this);

		// To prevent currently linked gauges from being updated
		ArrayList gauges = this.getWorkspace().getGauges(this);
		for(int i = 0; i < gauges.size(); i++) {
			((GaugeFrame)gauges.get(i)).setNetworkName(null);
		}

		//resentCommandTargets
	}
	
	public void internalFrameIconified(InternalFrameEvent e){
	}

	public void internalFrameDeiconified(InternalFrameEvent e){
	}
	
	public void internalFrameActivated(InternalFrameEvent e){
	}

	public void internalFrameDeactivated(InternalFrameEvent e){
	}

	/**
	 * Shows the quick reference guide in the help menu.  The quick reference
	 * is an html page in the Simbrain/doc directory
	 */
	public void showQuickRef() {
		
		URL url = null;
		try {
		   url = new URL("file:" + System.getProperty("user.dir") 
		           + FS + "docs" + FS + "SimbrainDocs.html");
		} catch (java.net.MalformedURLException e) {
		   System.err.println("Malformed URL");
		   return;
		}
		
		JFrame f = new JFrame();

		//create a Preferences object
		CalHTMLPreferences pref = new CalHTMLPreferences();

		//use one of its methods to enable the test navbar
		pref.setShowTestNavBar(true);

		//now pass the pref object to the Pane's constructor
		CalHTMLPane pane = new CalHTMLPane(pref, null, null);
		f.getContentPane().add(pane, "Center");
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		f.setSize(new Dimension(Math.min(d.width - 10, 800), Math.min(d.height - 40, 600)));
		f.setVisible(true);
		if (url != null) {
		   pane.showHTMLDocument(url);
		}

	 
	}
	
	
	
	////////////////////////////
	// Menu Even      //
	////////////////////////////

	public void menuCanceled(MenuEvent e) {
	}

	public void menuDeselected(MenuEvent e) {
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.MenuListener#menuSelected(javax.swing.event.MenuEvent)
	 */
	public void menuSelected(MenuEvent e) {

		// Handle gauge submenu
		JMenu gaugeSubMenu = getWorkspace().getGaugeMenu(netPanel);
		if (gaugeSubMenu != null) {
			gaugeMenu.add(gaugeSubMenu);
		}
		
		if(e.getSource().equals(fileMenu)){
			if(isChangedSinceLastSave()){
				saveNetItem.setEnabled(true);
			} else if (!isChangedSinceLastSave()){
				saveNetItem.setEnabled(false);
			}
		}

		// Handle set-neuron and set-weight menu-items.
		int num_neurons = netPanel.getSelectedNeurons().size();
		if (num_neurons > 0) {
			setNeuronItem.setText(
				"Set "
					+ num_neurons
					+ (num_neurons > 1
						? " Selected Neurons"
						: " Selected Neuron"));
			setNeuronItem.setEnabled(true);
		} else {
			setNeuronItem.setText("Set Selected Neuron(s)");
			setNeuronItem.setEnabled(false);
		}
		int num_weights = netPanel.getSelectedWeights().size();
		if (num_weights > 0) {
			setWeightItem.setText(
				"Set "
					+ num_weights
					+ (num_weights > 1
						? " Selected Weights"
						: " Selected Weight"));
			setWeightItem.setEnabled(true);
		} else {
			setWeightItem.setText("Set Selected Weight(s)");
			setWeightItem.setEnabled(false);
		}

	}

	////////////////////////////
	// Main method		      //
	///////////////////////////


	/**
	 * @return Returns the netPanel.
	 */
	public NetworkPanel getNetPanel() {
		return netPanel;
	}
	/**
	 * @param netPanel The netPanel to set.
	 */
	public void setNetPanel(NetworkPanel netPanel) {
		this.netPanel = netPanel;
	}
	
	/**
	 * @return Returns the path.  Used in persistence.
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * 
	 * @return platform-specific path.  Used in persistence.
	 */
	public String getGenericPath() {
		String ret =  path;
		if (path == null) {
			return null;
		}
		ret.replace('/', System.getProperty("file.separator").charAt(0));
		return ret;
	}
	
	/**
	 * @param path The path to set.  Used in persistence.
	 */
	public void setPath(String path) {
		this.path = path;
	}
	/**
	 * @return Returns the parent.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setWorkspace(Workspace parent) {
		this.workspace = parent;
	}
	
	
	/**
	 * For Castor.  Turn Component bounds into separate variables.  
	 */
	public void initBounds() {
		xpos = this.getX();
		ypos = this.getY();
		the_width = this.getBounds().width;
		the_height = this.getBounds().height;
	}
	
	/**
	 * @return Returns the xpos.
	 */
	public int getXpos() {
		return xpos;
	}
	/**
	 * @param xpos The xpos to set.
	 */
	public void setXpos(int xpos) {
		this.xpos = xpos;	
	}
	/**
	 * @return Returns the ypos.
	 */
	public int getYpos() {
		return ypos;
	}
	/**
	 * @param ypos The ypos to set.
	 */
	public void setYpos(int ypos) {
		this.ypos = ypos;
	}
	/**
	 * @return Returns the the_height.
	 */
	public int getThe_height() {
		return the_height;
	}
	/**
	 * @param the_height The the_height to set.
	 */
	public void setThe_height(int the_height) {
		this.the_height = the_height;
	}
	/**
	 * @return Returns the the_width.
	 */
	public int getThe_width() {
		return the_width;
	}
	/**
	 * @param the_width The the_width to set.
	 */
	public void setThe_width(int the_width) {
		this.the_width = the_width;
	}
	
	private void hasChanged() {
		Object[] options = {"Yes", "No"};
		int s = JOptionPane.showInternalOptionDialog(this,"This Network has changed since last save,\nWould you like to save these changes?","Network Has Changed",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null, options,options[0]);
		if (s == 0){
			netPanel.save();
		} else if (s == 1){
		}
	}

	/**
	 * @return Returns the changedSinceLastSave.
	 */
	public boolean isChangedSinceLastSave() {
		return changedSinceLastSave;
	}
	/**
	 * @param changedSinceLastSave The changedSinceLastSave to set.
	 */
	public void setChangedSinceLastSave(boolean hasChangedSinceLastSave) {
		this.changedSinceLastSave = hasChangedSinceLastSave;
	}
}
