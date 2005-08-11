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

package org.simbrain.world.dataworld;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;

/**
 * <b>DataWorldFrame</b> is a "spreadsheet world" used to send
 * rows of raw data to input nodes.
 */
public class DataWorldFrame extends JInternalFrame implements ActionListener,InternalFrameListener, MenuListener {

	private static final String FS = "/"; //System.getProperty("file.separator");Separator();
	private File current_file = null;
	private String currentDirectory = "." + FS + "simulations" + FS + "worlds";
	private JScrollPane worldScroller = new JScrollPane();
	private Workspace workspace;
	private DataWorld world;
	
	// For workspace persistence 
	private String path;
	private int xpos;
	private int ypos;
	private int the_width;
	private int the_height;
	
	JMenuBar mb = new JMenuBar();
	JMenu file = new JMenu("File  ");
	JMenuItem open = new JMenuItem("Open");
	JMenuItem save = new JMenuItem("Save");
	JMenuItem close = new JMenuItem("Close");
	JMenu edit = new JMenu("Edit");
	JMenuItem addRow = new JMenuItem("Add a row");
	JMenuItem addCol = new JMenuItem("Add a column");
	JMenuItem zeroFill = new JMenuItem("ZeroFill the Table");
	JMenuItem remRow = new JMenuItem("Remove a row");
	JMenuItem remCol = new JMenuItem("Remove a column");
	JMenuItem randomize = new JMenuItem("Randomize");
	JMenuItem randomProps = new JMenuItem("Adjust Randomization Bounds");
	
	private boolean changedSinceLastSave = false;
	
	public DataWorldFrame() {
	}
	
	/**
	 * Construct a new world panel.  Set up the toolbars.  Create an 
	 * instance of a world object.
	 */
	public DataWorldFrame(Workspace ws) { 
		
		workspace = ws;
		init();
	}
	
	public void init() {
		
		this.setResizable(true);
		this.setMaximizable(true);
		this.setIconifiable(true);
		this.setClosable(true);	
		this.addInternalFrameListener(this);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add("Center", worldScroller);
		world = new DataWorld(this);
		addMenuBar(world);
		worldScroller.setViewportView(world);
		worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		worldScroller.setEnabled(false);
		
		this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		
		this.resize();
		
		setVisible(true);
		
	}

	/**
	 * Creates the Menu Bar and adds it to the frame.
	 * 
	 * @param frame
	 * @param table
	 */
	public void addMenuBar(DataWorld table) {
		open.addActionListener(this);
		open.setActionCommand("open");
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		save.addActionListener(this);
		save.setActionCommand("save");
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		close.addActionListener(this);
		close.setActionCommand("close");
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mb.add(file);
		file.add(open);
		file.add(save);
		file.add(close);
		file.addMenuListener(this);
		
		addRow.addActionListener(this);
		addRow.setActionCommand("addRow");
		addCol.addActionListener(this);
		addCol.setActionCommand("addCol");
		remRow.addActionListener(this);
		remRow.setActionCommand("remRow");
		remCol.addActionListener(this);
		remCol.setActionCommand("remCol");
		zeroFill.addActionListener(this);
		zeroFill.setActionCommand("zeroFill");
		randomize.addActionListener(this);
		randomize.setActionCommand("randomize");
		randomProps.addActionListener(this);
		randomProps.setActionCommand("randomProps");
		edit.add(addRow);
		edit.add(addCol);
		edit.add(zeroFill);
		edit.addSeparator();
		edit.add(remRow);
		edit.add(remCol);
		edit.addSeparator();
		edit.add(randomize);
		edit.add(randomProps);
		mb.add(edit);
		
		setJMenuBar(mb);
	}

	
	/**
	 * Resize based on number of rows 
	 */
	public void resize() {
		int height = 59 + world.getTable().getRowCount() * world.getTable().getRowHeight();
		this.setBounds(this.getX(), this.getY(), this.getWidth(), height);
	}
	public File getCurrentFile() {
		return current_file;
	}

	public DataWorld getWorld() {
		return world;
	}

	/**
	 * Show the dialog for choosing a world to open
	 */
	public void openWorld() {
		SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
		File theFile = chooser.showOpenDialog();
		if (theFile != null) {
			readWorld(theFile);
			currentDirectory = chooser.getCurrentLocation();
		}
	}


	/**
	 * Read a world from a world-xml file.
	 * 
	 * @param theFile the xml file containing world information
	 */
	public void readWorld(File theFile) {
		
		current_file = theFile;

		String[][] data = Utils.getStringMatrix(theFile);

		//world.getModel().addMatrix(data);
		world.resetModel(data);
		getWorkspace().attachAgentsToCouplings();
		setName(theFile.getName());

		//Set Path; used in workspace persistence
		String localDir = new String(System.getProperty("user.dir"));		
		setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));		
	}
	
	/**
	 * Opens a file-save dialog and saves world information to the specified file
	 * 
	 * Called by "Save As"
	 */
	public void saveWorld() {
		SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
		File worldFile = chooser.showSaveDialog();
		if (worldFile != null){
		    saveWorld(worldFile);
		    current_file = worldFile;
		    currentDirectory = chooser.getCurrentLocation();
		}
		setChangedSinceLastSave(false);
	}

	/**
	 * Save a specified file
	 * 
	 * Called by "save"
	 * 
	 * @param worldFile
	 */
	public void saveWorld(File worldFile) {
		
		current_file = worldFile;
		String[][] data = new String[world.getTable().getRowCount()][world.getTable().getColumnCount()-1];
		
		for (int i = 0; i < world.getTable().getRowCount(); i++) {
			for (int j = 1; j < world.getTable().getColumnCount(); j++) {
				data[i][j-1] = new String("" + world.getTable().getValueAt(i, j));
			}
		}
		
		Utils.writeMatrix(data, current_file);

		String localDir = new String(System.getProperty("user.dir"));		
		setPath(Utils.getRelativePath(localDir, worldFile.getAbsolutePath()));		
			
		setName(worldFile.getName());	
	}
	
	
	public void internalFrameOpened(InternalFrameEvent e){
	}
	
	public void internalFrameClosing(InternalFrameEvent e){
		if (isChangedSinceLastSave()){
			hasChanged();
		} else
			dispose();
	}

	public void internalFrameClosed(InternalFrameEvent e){
		this.getWorkspace().getCouplingList().removeAgentsFromCouplings(this.getWorld());
		this.getWorkspace().getDataWorldList().remove(this);
		
		DataWorldFrame dat = workspace.getLastDataWorld() ;
		
		if (dat != null){
			dat.grabFocus();
			workspace.repaint();
		}

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
	 * @param path The path to set; used in persistence.
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 * 
	 * @return path information; used in persistence
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * 
	 * @return platform-specific path
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
	 * @return Returns the workspace.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}
	
	/**
	 * @param workspace The workspace to set.
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
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
	
	public void setName(String name) {
		setTitle(name);		
		world.setName(name);
		
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("open")) {
			openWorld();
			changedSinceLastSave = false;
		} else if (e.getActionCommand().equals("save")) {
			saveWorld();
			changedSinceLastSave = false;
		} else if (e.getActionCommand().equals("addRow")) {
			this.getWorld().getModel().addRow(this.getWorld().getModel().newRow());
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("addRowHere")){
			this.getWorld().getModel().insertRow(this.getWorld().getTable().rowAtPoint(this.getWorld().getSelectedPoint()),
					this.getWorld().getModel().newRow());
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("addCol")) {
			this.getWorld().getModel().addColumn("Int");
			this.getWorld().getModel().zeroFillNew();
			//Necessary to keep the buttons properly rendered
			this.getWorld().getTable().getColumnModel().getColumn(0)
					.setCellRenderer(
							new ButtonRenderer(this.getWorld().getTable()
									.getDefaultRenderer(JButton.class)));
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("addColHere")){
			insertColumnAtPoint(this.getWorld().getSelectedPoint());
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("remRow")){
			this.getWorld().getModel().removeRow(this.getWorld().getTable().getRowCount()-1);
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("remRowHere")){
			this.getWorld().getModel().removeRow(
					this.getWorld().getTable().rowAtPoint(this.getWorld().getSelectedPoint()));
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("remCol")){
			this.getWorld().getTable().removeColumn(
					this.getWorld().getTable().getColumnModel().getColumn(
							this.getWorld().getTable().getColumnCount()-1));
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("remColHere")){
			this.getWorld().getTable().getColumnModel().removeColumn(
					this.getWorld().getTable().getColumnModel().getColumn(
							this.getWorld().getTable().columnAtPoint(
									this.getWorld().getSelectedPoint())));
			changedSinceLastSave = true;
			resize();
		} else if (e.getActionCommand().equals("zeroFill")) {
			this.getWorld().getModel().zeroFill();
			changedSinceLastSave = true;
		} else if (e.getActionCommand().equals("close")){
			if(isChangedSinceLastSave()){
				hasChanged();
			}else
				dispose();
		} else if (e.getActionCommand().equals("randomize")){
			world.randomize();
			changedSinceLastSave = true;
		} else if (e.getActionCommand().equals("randomProps")){
			world.displayRandomizeDialog();
			changedSinceLastSave = true;
		} else if (e.getActionCommand().equals("changeButtonName")){
			world.changeButtonName((JButton)world.getTable().getValueAt(
					world.getTable().rowAtPoint(world.getSelectedPoint()),
					world.getTable().columnAtPoint(world.getSelectedPoint())));
		}
	}
	
	private void insertColumnAtPoint(Point p){
		Vector data = this.getWorld().getModel().getDataVector();
		int target = this.getWorld().getTable().columnAtPoint(p);
		int numRows = data.size();
		int numCols = ((Vector)data.get(0)).size();

		for (int j = 0; j < numRows; j++){
				((Vector)data.get(j)).insertElementAt(new Double(0),target);
		}
		
		
		Vector headers = new Vector(numCols + 1);
		headers.add(0,"Send");
		for (int j= 1; j< numCols + 1;j++){
			headers.add(j,"Double");
		}
		
		this.getWorld().getModel().setDataVector(data,headers);
		
		this.getWorld().getTable().getColumnModel().getColumn(0)
		.setCellRenderer(
				new ButtonRenderer(this.getWorld().getTable()
						.getDefaultRenderer(JButton.class)));
	}
	
	/**
	 * Checks to see if anything has changed and then offers to save if true
	 *
	 */
	private void hasChanged() {
		Object[] options = {"Save", "Don't Save","Cancel"};
		int s = JOptionPane.showInternalOptionDialog(this,"This World has changed since last save,\nWould you like to save these changes?","World Has Changed",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null, options,options[0]);
		if (s == 0){
			saveWorld();
			dispose();
		} else if (s == 1){
			dispose();
		} else if (s == 2){
			return;
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

	public void menuSelected(MenuEvent e) {
		if(e.getSource().equals(file)){
			if(isChangedSinceLastSave()){
				save.setEnabled(true);
			} else if (!isChangedSinceLastSave()){
				save.setEnabled(false);
			}
		}
	}

	public void menuDeselected(MenuEvent e) {
	}

	public void menuCanceled(MenuEvent e) {
	}
}