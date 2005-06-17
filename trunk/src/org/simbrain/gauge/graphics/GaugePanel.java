/*
 * Part of HiSee, a tool for visualizing high dimensional datasets
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.gauge.core.Dataset;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectCoordinate;
import org.simbrain.gauge.core.ProjectPCA;
import org.simbrain.gauge.core.ProjectSammon;
import org.simbrain.gauge.core.Projector;
import org.simbrain.gauge.core.Utils;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;

/**
 * <b>GaugePanel</b> is the main panel in which data are displayed.  Menu and toolbar handling code
 * are also provided here.
 */
public class GaugePanel extends PCanvas implements ActionListener {

	private static final String FS = System.getProperty("file.separator");
	protected String name = "";

	// CHANGE HERE if adding projection algorithm
	private GaugeThread theThread;

	protected static File current_file = null;
	private JCheckBox onOffBox =
		new JCheckBox(ResourceManager.getImageIcon("GaugeOn.gif"));
	private JButton openBtn =
		new JButton(ResourceManager.getImageIcon("Open.gif"));
	private JButton saveBtn =
		new JButton(ResourceManager.getImageIcon("Save.gif"));
	protected JButton iterateBtn =
		new JButton(ResourceManager.getImageIcon("Step.gif"));
	private JButton playBtn =
		new JButton(ResourceManager.getImageIcon("Play.gif"));
	private JButton prefsBtn =
		new JButton(ResourceManager.getImageIcon("Prefs.gif"));
	private JButton clearBtn =
		new JButton(ResourceManager.getImageIcon("Eraser.gif"));
	private JButton randomBtn =
		new JButton(ResourceManager.getImageIcon("Rand.gif"));
	private JComboBox projectionList = 
		new JComboBox(Gauge.getProjectorList());
	
	private JPanel bottomPanel = new JPanel();
	
	private JToolBar theToolBar = new JToolBar();
	private JToolBar statusBar = new JToolBar();
	private JToolBar errorBar = new JToolBar();

	
	private JLabel pointsLabel = new JLabel();
	private JLabel dimsLabel = new JLabel();
	private JLabel errorLabel = new JLabel();
	

	JMenuBar mb = new JMenuBar();
	JMenu fileMenu = new JMenu("File  ");
	JMenuItem openHi = new JMenuItem("Open High-Dimensional Dataset");
	JMenuItem openLow = new JMenuItem("Open Low-Dimensional Dataset");
	JMenuItem openCombined = new JMenuItem("Open Combined (High/Low) Dataset");

	JMenuItem saveLow = new JMenuItem("Save Low-Dimensional Dataset");
	JMenuItem saveHi = new JMenuItem("Save High-Dimensional Dataset");
	JMenuItem saveCombined = new JMenuItem("Save Combined (High/Low) Dataset");
	JMenuItem addHi = new JMenuItem("Add High-Dimensional Data");
	JMenuItem quitItem = new JMenuItem("Quit");
	JMenu prefsMenu = new JMenu("Preferences");
	JMenuItem projectionPrefs = new JMenuItem("Projection Preferences");
	JMenuItem graphicsPrefs = new JMenuItem("Graphics /GUI Preferences");
	JMenuItem generalPrefs = new JMenuItem("General Preferences");
	JMenuItem setAutozoom = new JCheckBoxMenuItem("Autoscale", true);
	JMenu helpMenu = new JMenu("Help");
	JMenuItem helpItem = new JMenuItem("Help");
	
	public JFrame theFrame;
	public ArrayList node_list = new ArrayList();
	private Gauge theGauge;
	private double minx, maxx, miny, maxy;
	private KeyEventHandler keyEventHandler;
	private MouseEventHandler mouseHandler;
	private boolean autoZoom = true;
	
	private static final double OFFSET_X = 10;
	private static final double OFFSET_Y = 10;
	private static final int CLEARED= -1;
	
	
	// Application parameters
	private boolean update_completed = false;
	private boolean colorMode = false;
	private int numIterationsBetweenUpdate = 10;
	private double scale = .2;
	private boolean showError = false;
	private boolean showStatus = true;
	private double minimumPointSize = .05;
	
	//Piccolo stuff
	private PCamera cam;
	private PPath pb;
	
	// "Hot" points 
	private int hotPoint = 0;
	private Color hotColor = Color.RED;
	private Color defaultColor = Color.GREEN;
	
	/**
	 * For use where a separate frame is created outside of HiSee
	 * 
	 * @param g reference to the Gauge 
	 */
	public GaugePanel(Gauge g) {
		theGauge = g;
		init();
		
	}
	
	/**
	 * A gauge which represents a projection of some subset of the state and 
	 * parameter space of a neural network.
	 * 
	 * @param theName name of this gauge, shown in its title bar
	 * @param objects the objects whose states are to be gauged
	 */
	public GaugePanel(Gauge g, JFrame f) {
		theGauge = g;
		theFrame = f;
		
		init();
		setUpMenus();
	
	}
	
	public void init() {
		cam = this.getCamera();
		setLayout(new BorderLayout());
		setBackground(Color.black);
		
		handleMenuEvents();
		
		onOffBox.setToolTipText("Turn gauge on or off");
		openBtn.setToolTipText("Open high-dimensional data");
		saveBtn.setToolTipText("Save data");
		playBtn.setToolTipText("Iterate projection algorithm");
		iterateBtn.setToolTipText("Step projection algorithm");
		clearBtn.setToolTipText("Clear current data");

		projectionList.setMaximumSize(new java.awt.Dimension(200,100));
		
		keyEventHandler = new KeyEventHandler(this);
		addInputEventListener(keyEventHandler);
		getRoot().getDefaultInputManager().setKeyboardFocus(keyEventHandler);
		
		mouseHandler = new MouseEventHandler(this);
		addInputEventListener(mouseHandler);
		
		theToolBar.add(onOffBox);
		theToolBar.add(projectionList);
		theToolBar.add(playBtn);
		theToolBar.add(iterateBtn);
		theToolBar.add(clearBtn);
		theToolBar.add(randomBtn);
		
		statusBar.add(pointsLabel);
		statusBar.add(dimsLabel);
		setShowStatus(showStatus);
		
		errorBar.add(errorLabel);
		setShowError(showError);
		this.updateProjectionMenu();
		

		projectionList.addActionListener(this);
		
		onOffBox.addActionListener(this);
		openBtn.addActionListener(this);
		saveBtn.addActionListener(this);
		iterateBtn.addActionListener(this);
		clearBtn.addActionListener(this);
		playBtn.addActionListener(this);
		prefsBtn.addActionListener(this);
		randomBtn.addActionListener(this);
		
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add("South", statusBar);
		bottomPanel.add("North", errorBar);
		add("North", theToolBar);
		add("South", bottomPanel);
	}
	
	/**
	 * Sets up the main menu bar
	 */
	private void setUpMenus() {
		theFrame.setJMenuBar(mb);
		initMenus();
	}
	
	public void setUpMenus(JInternalFrame frame) {
		frame.setJMenuBar(mb);
		initMenus();
	}

	
	private void initMenus() {
		mb.add(fileMenu);
		mb.add(prefsMenu);
		//mb.add(helpMenu);
		fileMenu.add(openHi);
		fileMenu.add(openLow);
		fileMenu.add(openCombined);
		fileMenu.addSeparator();
		fileMenu.add(saveHi);
		fileMenu.add(saveLow);
		fileMenu.add(saveCombined);
		fileMenu.addSeparator();		
		fileMenu.add(addHi);
		fileMenu.addSeparator();		
		fileMenu.add(quitItem);
		prefsMenu.add(projectionPrefs);
		prefsMenu.add(graphicsPrefs);
		prefsMenu.add(generalPrefs);
		prefsMenu.addSeparator();
		prefsMenu.add(setAutozoom);
		//helpMenu.add(helpItem);
		
	}
	/**
	 * Handles menu events using anonymous inner classes
	 */
	private void handleMenuEvents() {
		openHi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openHi();
			}
		});
		openLow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openLow();
			}
		});
		openCombined.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openCombined();
			}
		});
		saveHi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveHi();
			}
		});
		saveLow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveLow();
			}
		});
		saveCombined.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveCombined();
			}
		});
		addHi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addHi();
			}
		});
		projectionPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handlePreferenceDialogs();
			}
		});
		graphicsPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleGraphicsDialog();
			}
		});
		generalPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleGeneralDialog();
			}
		});
		setAutozoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setAutoZoom(setAutozoom.isSelected());
				repaint();
			}
		});
		quitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(1);
			}
		});

	}


	/**
	 * Open preference dialog based on which projector is currently selected
	 */
	private void handlePreferenceDialogs() {

		if((theGauge.getUpstairs() == null) || (theGauge.getDownstairs() == null)) {
			return;
		}	
		
		if (theGauge.getProjector() instanceof ProjectSammon) {
			DialogSammon dialog = new DialogSammon(theGauge);
			showProjectorDialog((StandardDialog)dialog);
		} else if (theGauge.getProjector() instanceof ProjectCoordinate) {
			DialogCoordinate dialog = new DialogCoordinate(theGauge);
			showProjectorDialog((StandardDialog)dialog);
			theGauge.getProjector().project();
			updateGauge();
		} 
	}

	/**
	 * Show graphics dialog
	 */
	private void handleGraphicsDialog() {
		DialogGraphics dialog = new DialogGraphics(this);
		dialog.pack();
		dialog.show();
		if(!dialog.hasUserCancelled())
		{
			dialog.getValues();
		}
	}
	
	/**
	 * Show genneral prefs dialog
	 */
	private void handleGeneralDialog() {
		DialogGeneral dialog = new DialogGeneral(this);
		dialog.pack();
		dialog.show();
		if(!dialog.hasUserCancelled())
		{
			dialog.getValues();
		}
	}
	
	
	/**
	 * Show projector dialog
	 * 
	 * @param dialog
	 */
	private void showProjectorDialog(StandardDialog dialog) {
		dialog.pack();
		dialog.show();
		if(!dialog.hasUserCancelled())
		{
			dialog.setProjector();
		}
	}
	
	
	//////////////////////////////
	// INITIALIZE AND UPDATE	//
	//////////////////////////////
	
	/**
	 * Initialize the graphics component.
	 * In particular, populate the list of "PNodes" (the graphics objects
	 * shown on screen).
	 */
	public void initGaugePanel() {
		node_list.clear();
		hotPoint = CLEARED;
		this.getLayer().removeAllChildren();
		
		for (int i = 0; i < theGauge.getDownstairs().getNumPoints(); i++) {
			addNode(new PNodeDatapoint(theGauge.getDownstairs().getPoint(i), i));
		}
		dimsLabel.setText("     Dimensions: " + theGauge.getUpstairs().getDimensions());
		this.setColorMode(this.isColorMode());
	
	}
	
	/**
	 * Update the gauge.  Assumes the low-d datapoints (which are what the
	 * gauge shows) have been changed.  
	 * This method is called by the gauge thread
	 * 
	 */
	public void updateGauge() {
		
		pointsLabel.setText("  Datapoints: " + theGauge.getDownstairs().getNumPoints());
		if (theGauge.getProjector().isIterable() == true) {
			errorLabel.setText(" Error:" + theGauge.getError());
		}
		
		if(node_list.size() == 0) {
			return;
		}
		
		double[] tempPoint;
		for (int i = 0; i < theGauge.getDownstairs().getNumPoints(); i++) {
			tempPoint = theGauge.getDownstairs().getPoint(i);
			((PNodeDatapoint)node_list.get(i)).setOffset(tempPoint[0], tempPoint[1]); //Assumes 2-d
		}
		autoscale();
		setUpdateCompleted(true);
		
		
	}
	
	

	/**
	 * Reset the gauge, removing all PNodes and references
	 * to datapoints
	 */
	public void resetGauge() {
		this.getLayer().removeAllChildren();
		node_list.clear();
		hotPoint = CLEARED;
	}
	
	
	/**
	 * Adds a PNode (datapoint) to the gauge panel
	 * 
	 * @param theNode the node to add to the network
	 */
	public void addNode(PNode theNode) {
		node_list.add(theNode);
		this.getLayer().addChild(theNode);
	}
	
	/**
	 * Manually set the currently selected projection algorithm.  Used when the projection
	 * method is changed independently of the user
	 */
	public void updateProjectionMenu() {
		Projector proj = theGauge.getProjector();
		if (proj instanceof ProjectCoordinate) {
			projectionList.setSelectedIndex(2);
		} else if (proj instanceof ProjectPCA) {
			projectionList.setSelectedIndex(1);
		} else if (proj instanceof ProjectSammon) {
			projectionList.setSelectedIndex(0);
		}
		setToolbarIterable(proj.isIterable());
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		Object e1 = e.getSource();
		
		// Handle drop down list; Change current projection algorithm
		if (e1 instanceof JComboBox) {
			
			stopThread();			
			String selectedGauge = ((JComboBox)e1).getSelectedItem().toString();			
			//The setCurrentProjector will wipe out the hot-point, so store it and reset it after the init
			int temp_hot_point = hotPoint;
			theGauge.setCurrentProjector(selectedGauge);
			Projector proj = theGauge.getProjector();
			if (proj == null) {
				return;
			}
			if((proj.isIterable() == true) && (showError == true)) {
				errorBar.setVisible(true);
			} else {
				errorBar.setVisible(false);
			}
			
			proj.checkDatasets(); 
			setToolbarIterable(proj.isIterable());
			this.setColorMode(this.isColorMode());

			setHotPoint(temp_hot_point); 
			updateGauge();
			
		}
		
		// Handle Check boxes
		if (e1 instanceof JCheckBox) {
			if (e1 == onOffBox) {
				if (theGauge.isOn() == true) {
					theGauge.setOn(false);
					onOffBox.setIcon(
						ResourceManager.getImageIcon("GaugeOff.gif"));
					onOffBox.setToolTipText("Turn gauge on");
				} else {
					theGauge.setOn(true);
					onOffBox.setIcon(
						ResourceManager.getImageIcon("GaugeOn.gif"));
					onOffBox.setToolTipText("Turn gauge off");
				}
			}
		} 
		
		// Handle Button Presses 
		if (e1 instanceof JButton){
			JButton btemp = (JButton) e.getSource();

			if (btemp == iterateBtn) {
				iterate();
				updateGauge();
			} else if (btemp == clearBtn) {
				theGauge.init(theGauge.getUpstairs().getDimensions());
				resetGauge();
			} else if (btemp == playBtn) {
					if (theThread == null) {
							theThread = new GaugeThread(this);
					}
					if (theThread.isRunning() == false) {
						startThread();
					} else {
						stopThread();
					}
			} else if (btemp == randomBtn) {
				theGauge.getDownstairs().randomize(100);
				updateGauge();
			} else if (btemp == prefsBtn) {
			}
		}
	}
	
	//////////////////////////
	// THREAD METHODS		//
	//////////////////////////
	
	private void stopThread() {
		playBtn.setIcon(ResourceManager.getImageIcon("Play.gif"));
		playBtn.setToolTipText("Start iterating projection algorithm");
		if (theThread == null) {
			return;
		}
		theThread.setRunning(false);
		theThread = null;
	}
	
	private void startThread() {
		if (theThread == null) {
				theThread = new GaugeThread(this);
		}
		playBtn.setIcon(ResourceManager.getImageIcon("Stop.gif"));
		playBtn.setToolTipText(
				"Stop iterating projection algorithm");
		theThread.setRunning(true);
		theThread.start();
	}


	/**
	 * Forward command to gauge;
	 * iterate the gauge one time
	 */
	public void iterate() {
		theGauge.iterate(numIterationsBetweenUpdate); 
	}
	
	//////////////////////////
	// GRAPHICS	 METHODS	//
	//////////////////////////
	
	/**
	 * Scale the data so that it fits on the screen
	 * Assumes 2-d data.
	 */
	public void autoscale() {

		if (node_list == null) {
			return; 
		} 
		if (node_list.size() == 0) {
			return;
		}
		
		PNodeDatapoint p = (PNodeDatapoint)node_list.get(0);

		minx = maxx =  p.getGlobalX();
		miny = maxy =  p.getGlobalY();
		double x,y;
				
		if (node_list.size() == 1) {
			p.setSize(.4);
			pb = PPath.createRectangle((float)(minx - 10), (float)(miny - 10), (float)(21), (float)(21));
			cam.animateViewToCenterBounds(pb.getBounds(), true, 0);
			return;
		}
		for (int i = 1; i < theGauge.getUpstairs().getNumPoints(); i++) {
			p = (PNodeDatapoint)node_list.get(i);
			x =  p.getGlobalX();
			y = p.getGlobalY();			
				if (x < minx)
						minx = x;
				if (x > maxx)
						maxx = x;
				if (y < miny)
						miny = y;
				if (y> maxy)
						maxy = y;
		}
		
		
		double width = maxx - minx;
		double height =  maxy - miny;

		setSizes(theGauge.getDownstairs().getMaximumDistance() / 50);

		//pb =  PPath.createRectangle((float)minx, (float)miny, (float)width, (float)height);		
		pb =  PPath.createRectangle((float)(minx - (width*scale)), (float)(miny - (height*scale)), (float)((1+scale*2) * width), (float)((1+scale*2) * height));
		//this.getLayer().addChild(pb);
		
		cam.animateViewToCenterBounds(pb.getBounds(), true, 0);
		
	}
	
	/**
	 * Color every seventh point a different color; allows tracking of order
	 */
	public void colorPoints() {
		// Use different colors for the points
		if (node_list.size() == 0) {
			return;
		}
		for (int i = 0; i < node_list.size(); i++) {
			PNodeDatapoint pn = (PNodeDatapoint) node_list.get(i);
			if (i % 7 == 0)
				pn.setColor(java.awt.Color.red);
			if (i % 7 == 1)
				pn.setColor(java.awt.Color.orange);
			if (i % 7 == 2)
				pn.setColor(java.awt.Color.yellow);
			if (i % 7 == 3)
				pn.setColor(java.awt.Color.green);
			if (i % 7 == 4)
				pn.setColor(java.awt.Color.cyan);
			if (i % 7 == 5)
				pn.setColor(java.awt.Color.blue);
			if (i % 7 == 6)
				pn.setColor(java.awt.Color.magenta);
			}
		}
		
	/**
	 * Color all datapoints a specified color
	 * @param c new color
	 */
	public void setColor(Color c) {
		if (node_list.size() == 0) {
			return;
		}
		for (int i = 0; i < node_list.size(); i++) {
			PNodeDatapoint pn = (PNodeDatapoint) node_list.get(i);
			pn.setColor(c);
		}
	}
	
	
	/**
	 * Set a unique datapoint to "hot" mode, which just means it is shown
	 * in a different color, to indicate (e.g.) that it is the current point
	 * in a set of points.
	 * 
	 * @param i index of datapoint to designate as "hot"
	 */
	public void setHotPoint(int i) {
		
		
		if (theGauge.isUsingHotPoint() == false) {
			return;
		}
		
		if (i == CLEARED) {
			return;
		}
		
		if (i >= node_list.size()) {
			System.out.println("ERROR: the designated point (" + i + ") is outside the dataset bounds (dataset size = " + node_list.size() + ")");
			return;
		}
		if (hotPoint >= node_list.size()) {
			System.out.println("ERROR: the designated hot-point (" + hotPoint + ") is outside the dataset bounds (dataset size = " + node_list.size() + ")");
			return;
		}
		
		//New hot point to hot color
		hotPoint = i;
		((PNodeDatapoint) node_list.get(hotPoint)).setColor(hotColor);
		((PNode) node_list.get(hotPoint)).moveToFront();
	}
	
	/**
	 * @return "current" point in use by another component.
	 */
	public int getHotPoint() {
		
		return hotPoint;
	}
	
	public void repaint() {
		super.repaint();
		if (autoZoom == true) {
			autoscale();
		}
	}
	
	//////////////////////////////////
	// OPEN, CLOSE AND ADD METHODS	//
	//////////////////////////////////
		
	/**
	 * Open saved hi dimensional data
	 */
	public void openHi() {
		
		resetGauge();
		
		JFileChooser chooser  = new JFileChooser();
		chooser.addChoosableFileFilter(new HiDFilter()); 
		chooser.setCurrentDirectory(new File(theGauge.getDefaultDir()));
		int result = chooser.showDialog(this, "Open");
		if (result == JFileChooser.APPROVE_OPTION) {
			Dataset data = new Dataset();
			data.readData(chooser.getSelectedFile());
			theGauge.getProjector().init(data, null);
			theGauge.getProjector().project();
			initGaugePanel();
			updateGauge();
			autoscale();

		}
	}
	 
	
	class HiDFilter extends javax.swing.filechooser.FileFilter {
			public boolean accept(File file) {
				String filename = file.getName();
				return (filename.endsWith( ".hi" ) || file.isDirectory());
			}
			public String getDescription() {
				return "*.hi" ;
			}
		} 
	
	
	/**
	 * Opens a combined dataset, incorporating both high dimensional data
	 * and a previous projection of those data to two dimensions
	 */
	public void openCombined() {
		

		resetGauge();
		
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new CombFilter()); 
		chooser.setCurrentDirectory(new File(theGauge.getDefaultDir()));
		int result = chooser.showDialog(this, "Open");
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = chooser.getSelectedFile();
		
		FileInputStream f = null;
		String[][] values = null;
		ArrayList dataHi = new ArrayList();			
		ArrayList dataLow = new ArrayList();

		CSVParser theParser = null;

		try {
			theParser =
				new CSVParser(f = new FileInputStream(file), "", "", "#");
			// # is a comment delimeter in net files
			values = theParser.getAllValues();
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}

		//Assumes the low-dimensional space is 2 dimensional
		for(int i = 0; i < values.length; i++) {
			if (values[i].length == 2) {
				dataLow.add(Utils.stringArrayToDoubleArray(values[i]));
			}
			else {
				dataHi.add(Utils.stringArrayToDoubleArray(values[i]));
			}
		}

		theGauge.getProjector().init(new Dataset(dataHi), new Dataset(dataLow));
		initGaugePanel();
		updateGauge();
		autoscale();
	}
	
	class CombFilter extends javax.swing.filechooser.FileFilter {
			public boolean accept(File file) {
				String filename = file.getName();
				return (filename.endsWith( ".comb" ) || file.isDirectory());
			}
			public String getDescription() {
				return "*.comb" ;
			}
		} 
	
	/**
	 * Open saved low dimensional data
	 */
	public void openLow() {
		
		resetGauge();
		
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new LowDFilter()); 
		chooser.setCurrentDirectory(new File(theGauge.getDefaultDir()));
		int result = chooser.showDialog(this, "Open");
		if (result == JFileChooser.APPROVE_OPTION) {
			theGauge.getDownstairs().readData(chooser.getSelectedFile());
		}
		
		initGaugePanel();
		theGauge.getProjector().compareDatasets();
		updateGauge();
		autoscale();

	}
	class LowDFilter extends javax.swing.filechooser.FileFilter {
			public boolean accept(File file) {
				String filename = file.getName();
				return (filename.endsWith( ".low" ) || file.isDirectory());
			}
			public String getDescription() {
				return "*.low" ;
			}
		} 
		
	/**
	 * Open saved low dimensional data
	 */
	public void addHi() {
		
		resetGauge();
		
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new HiDFilter()); 
		chooser.setCurrentDirectory(new File(theGauge.getDefaultDir()));
		int result = chooser.showDialog(this, "Open");
		if (result == JFileChooser.APPROVE_OPTION) {
			theGauge.getProjector().addUpstairs(chooser.getSelectedFile());	
		}

		initGaugePanel();
		updateGauge();
		autoscale();

	}
	

	
	/**
	 * Save high dimensional data, for example, after data have been added to the dataset)
	 */
	public void saveHi() {
				
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new HiDFilter()); 
		chooser.setCurrentDirectory(new File(theGauge.getDefaultDir()));
	
		int result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			theGauge.getUpstairs().saveData(chooser.getSelectedFile());			
			addExtension(chooser.getSelectedFile(), "hi");
		}
	
	}
	
	/**
	 * Save the high dimensional data and the low dimensional projectino in one dataset
	 */
	public void saveCombined() {
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new CombFilter()); 
		chooser.setCurrentDirectory(new File(theGauge.getDefaultDir()));
		
		int result = chooser.showDialog(this, "Save");
		File theFile = null;
		if (result == JFileChooser.APPROVE_OPTION) {
			theFile = chooser.getSelectedFile();
		} else return;
		
		FileOutputStream f = null;
		try {
			f = new FileOutputStream(theFile);
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}
		if (f == null) {
			return;
		}
		
		//Create network file
		PrintStream ps = new PrintStream(f);
		CSVPrinter thePrinter = new CSVPrinter(f);

		thePrinter.printlnComment("");
		thePrinter.printlnComment("Combined File: " + theFile.getName());
		thePrinter.printlnComment("");
		thePrinter.println();
		thePrinter.println(theGauge.getUpstairs().getDoubleStrings());
		thePrinter.println();
		thePrinter.println();
		thePrinter.println(theGauge.getDownstairs().getDoubleStrings());
		thePrinter.println();
		
		addExtension(chooser.getSelectedFile(), "comb");
	}
	

	/**
	 * Save low-dimensional data.
	 */
	public void saveLow() {
				
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new LowDFilter()); 
		chooser.setCurrentDirectory(new File(theGauge.getDefaultDir()));
		
		int result = chooser.showDialog(this, "Save");
		if (result == JFileChooser.APPROVE_OPTION) {
			theGauge.getDownstairs().saveData(chooser.getSelectedFile());
			addExtension(chooser.getSelectedFile(), "low");
		}
		
	}
	
	
	/**
	 * Check to see if the file has the extension, and if not, add it.
	 * 
	 * @param theFile file to add extension to
	 * @param extension extension to add to file
	 */
	private static void addExtension(File theFile, String extension) {
		
		if(theFile.getName().endsWith("." + extension)) return;
		
		theFile.renameTo(new File(theFile.getAbsolutePath().concat("." + extension)));
		
	}


	//////////////////////////////////
	// GETTER AND SETTER METHODS	//
	//////////////////////////////////

	
	/**
	 * Reset the sizes of all datapoint PNodes
	 * 
	 * @param s new size, in pixels, for datapoints
	 */
	public void setSizes(double s) {
		
		if ((s < minimumPointSize) || (Double.isNaN(s))) {
			s = minimumPointSize;
		}
		for (int i = 0; i < node_list.size(); i++) {
			PNodeDatapoint p = (PNodeDatapoint)node_list.get(i);
			p.setSize(s);
		}
		
	}


	/**
	 * @return a reference to the gauge
	 */
	public Gauge getGauge() {
		return theGauge;
	}

	/**
	 * Used by the thread to be sure an iteration is complete
	 * before it iterates again
	 * 
	 * @return true if update is completed, false otherwise
	 */
	public boolean isUpdateCompleted() {
		return update_completed;
	}

	/**
	 * Used by the thread to be sure an iteration is complete
	 * before it iterates again
	 * 
	 * @param b true if update is completed, false otherwise
	 */
	public void setUpdateCompleted(boolean b) {
		update_completed = b;
	}

	/**
	 * Enable or disable buttons depending on whether the current projection algorithm allows
	 * for iterations or not
	 * 
	 * @param b whether the current projection algorithm can be iterated or not
	 */
	private void setToolbarIterable(boolean b) {
		if (b == true) {
			playBtn.setEnabled(true);
			iterateBtn.setEnabled(true);
		}
		else {
			playBtn.setEnabled(false);
			iterateBtn.setEnabled(false);
		}
	}
	
	

	/**
	 * @return true if the gauge is in color mode (colors the datapoints), false otehrwise
	 */
	public boolean isColorMode() {
		return colorMode;
	}

	/**
	 * @param b true if the gauge is in color mode (colors the datapoints), false otehrwise
	 */
	public void setColorMode(boolean b) {
		colorMode = b;
		if (colorMode == true) {
			colorPoints();
		} else {
			setColor(defaultColor);
		}
	}


	/**
	 * @return number of iterations the projection algorithm takes between graphics updates
	 */
	public int getNumIterationsBetweenUpdate() {
		return numIterationsBetweenUpdate;
	}

	/**
	 * @param i number of iterations the projection algorithm takes between graphics updates
	 */
	public void setNumIterationsBetweenUpdate(int i) {
		numIterationsBetweenUpdate = i;
	}

	/**
	 * @return scale factor for shrinking the dataset so it fits on-screen
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * @param d cale factor for shrinking the dataset so it fits on-screen
	 */
	public void setScale(double d) {
		scale = d;
	}

	/**
	 * Used to programatically set the projector
	 */
	public void setProjector(String projector) {
		
	}
	
	/**
	 * @return true if error information should be shown, false otherwise
	 */
	public boolean isShowError() {
		return showError;
	}

	/**
	 * @return true if status information (dimensions and number of datapoints)
	 *  should be shown, false otherwise
	 */
	public boolean isShowStatus() {
		return showStatus;
	}

	/**
	 * @param b true if status information (dimensions and number of datapoints)
	 *  should be shown, false otherwise
	 */
	public void setShowStatus(boolean b) {
		statusBar.setVisible(b);
		showStatus = b;
	}
	
	/**
	 * @param b  true if error information should be shown, false otherwise
	 */
	public void setShowError(boolean b) {
		errorBar.setVisible(b);
		showError = b;
	}

	/**
	 * @return the minimum size which all datapoints must be
	 */
	public double getMinimumPointSize() {
		return minimumPointSize;
	}

	/**
	 * @param d the minimum size which all datapoints must be
	 */
	public void setMinimumPointSize(double d) {
		minimumPointSize = d;
	}

	/**
	 * You can turn autozoom off if you want to zoom in on data.
	 * 
	 * @return true if autozoom (which automatically scales the dataset to the screen) is on,
	 * false otherwise.
	 */
	public boolean isAutoZoom() {
		return autoZoom;
	}

	/**
	 * @param b true if autozoom (which automatically scales the dataset to the screen) is on,
	 * false otherwise.
	 */
	public void setAutoZoom(boolean b) {
		autoZoom = b;
	}
	
	public JCheckBox getOnOffBox() {
		return onOffBox;
	}
	

	
	
	/**
	 * @return Returns the theFrame.
	 */
	public JFrame getTheFrame() {
		return theFrame;
	}
	/**
	 * @param theFrame The theFrame to set.
	 */
	public void setTheFrame(JFrame theFrame) {
		this.theFrame = theFrame;
	}
}
