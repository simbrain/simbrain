/*
 * Created on May 28, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.gauge;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.NetworkPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;
import org.xml.sax.XMLReader;

import edu.umd.cs.piccolo.PNode;
import org.exolab.castor.tools.MappingTool;

/**
 * This class wraps a Gauge object in a Simbrain workspace frame, which also stores 
 * information about the variables the Gauge is representing.
 */
public class GaugeFrame extends JInternalFrame implements InternalFrameListener, ActionListener, MenuListener{

	public static final String FS = "/"; // System.getProperty("file.separator");Separator();
	
	private Workspace workspace;
	private GaugePanel theGaugePanel;

	private String name = null;
	private ArrayList gaugedVars;		// the variables this gauge gauges 
	private String persistentGaugedVars;
	
	// For workspace persistence 
	private String path = null;
	private String networkName = null;
	private int xpos;
	private int ypos;
	private int the_width;
	private int the_height;
	
	private boolean changedSinceLastSave = false;
	
	// Menu stuff
	JMenuBar mb = new JMenuBar();
	JMenu fileMenu = new JMenu("File  ");
	JMenuItem openHi = new JMenuItem("Import High-Dimensional CSV");
	JMenuItem openCombinedTest = new JMenuItem("Open(testing)");
	JMenuItem saveLow = new JMenuItem("Export Low-Dimensional CSV");
	JMenuItem saveHi = new JMenuItem("Export High-Dimensional CSV");
	JMenuItem saveCombinedTest = new JMenuItem("Save(testing)");
	JMenu fileOpsMenu = new JMenu("Import / Export");
	JMenuItem close = new JMenuItem("Close");
	JMenu prefsMenu = new JMenu("Preferences");
	JMenuItem projectionPrefs = new JMenuItem("Projection Preferences");
	JMenuItem graphicsPrefs = new JMenuItem("Graphics /GUI Preferences");
	JMenuItem generalPrefs = new JMenuItem("General Preferences");
	JMenuItem setAutozoom = new JCheckBoxMenuItem("Autoscale", true);
	JMenu helpMenu = new JMenu("Help");
	JMenuItem helpItem = new JMenuItem("Help");

	public GaugeFrame() {	
	}

	public GaugeFrame(Workspace ws) {

		workspace = ws;
		init();
	}
	
	public void init() {
 
		theGaugePanel = new GaugePanel();
		getContentPane().add(theGaugePanel);

		this.addInternalFrameListener(this);
		this.setResizable(true);
		this.setMaximizable(true);
		this.setIconifiable(true);
		this.setClosable(true);	
		this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		
		setUpMenus();
	}
	
	
	private void setUpMenus() {
		setJMenuBar(mb);
		
		mb.add(fileMenu);
		mb.add(prefsMenu);
		mb.add(helpMenu);
		
		fileMenu.addMenuListener(this);
		
		openHi.addActionListener(this);
		openCombinedTest.addActionListener(this);		
		saveHi.addActionListener(this);
		saveLow.addActionListener(this);
		saveCombinedTest.addActionListener(this);
		projectionPrefs.addActionListener(this);
		graphicsPrefs.addActionListener(this);
		generalPrefs.addActionListener(this);
		setAutozoom.addActionListener(this);
		close.addActionListener(this);
		helpItem.addActionListener(this);
		
	//	openCombined.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	//	saveCombinedAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		fileMenu.add(openCombinedTest);
		fileMenu.add(saveCombinedTest);
		fileMenu.addSeparator();
		fileMenu.add(fileOpsMenu);
		fileOpsMenu.add(openHi);
		fileOpsMenu.add(saveHi);
		fileOpsMenu.add(saveLow);
		fileMenu.addSeparator();
		fileMenu.add(close);
		
		prefsMenu.add(projectionPrefs);
		prefsMenu.add(graphicsPrefs);
		prefsMenu.add(generalPrefs);
		prefsMenu.addSeparator();
		prefsMenu.add(setAutozoom);
		
		helpMenu.add(helpItem);
		
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if( (e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class) ) {

			JMenuItem jmi = (JMenuItem) e.getSource();
			
			if(jmi == openCombinedTest){
				openCombinedTest();
			} else if(jmi == saveCombinedTest)  {
				saveCombinedTest();
			}  else if(jmi == openHi)  {
				theGaugePanel.openHi();
			} else if(jmi == saveLow)  {
				theGaugePanel.saveLow();
			} else if(jmi == saveHi)  {
				theGaugePanel.saveHi();
			} else if(jmi == projectionPrefs)  {
				theGaugePanel.handlePreferenceDialogs();
			} else if(jmi == graphicsPrefs)  {
				theGaugePanel.handleGraphicsDialog();
			} else if(jmi == generalPrefs)  {
				theGaugePanel.handleGeneralDialog();
			} else if(jmi == setAutozoom)  {
				theGaugePanel.setAutoZoom(setAutozoom.isSelected());
				theGaugePanel.repaint();
			} else if(jmi == close){
				if(isChangedSinceLastSave()){
					hasChanged();
				} else
					dispose();
			} else if(jmi == helpItem){
				Utils.showQuickRef(this);
			}
		}
			
	}
	
	
	public void openCombinedTest(){
		SFileChooser chooser = new SFileChooser(theGaugePanel.getGauge().getDefaultDir(), "xml");
		File theFile = chooser.showOpenDialog();
		
		if(theFile != null){
		    readGauge(theFile);
		    theGaugePanel.getGauge().setDefaultDir(chooser.getCurrentLocation());
		}
		String localDir = new String(System.getProperty("user.dir"));
		if (theGaugePanel.getCurrentFile() != null) {
			this.setPath(Utils.getRelativePath(localDir, theGaugePanel.getCurrentFile().getAbsolutePath()));					
			setTitle(theGaugePanel.getCurrentFile().getName());
		}
	}

//	public void saveCombined(){
//		if(theGaugePanel.getCurrentFile() != null){
//			theGaugePanel.saveCombined(theGaugePanel.getCurrentFile());
//		}
//		else {
//			saveCombinedAs();
//		}
//		this.setChangedSinceLastSave(false);
//		
//	}
	
	public void saveCombinedTest(){
	    SFileChooser chooser = new SFileChooser(theGaugePanel.getGauge().getDefaultDir(), "xml");
	    File theFile = chooser.showSaveDialog();
	    
	    if(theFile != null){
	        writeGauge(theFile);
	        theGaugePanel.getGauge().setDefaultDir(chooser.getCurrentLocation());	        
	    }

	}
	
	
	/**
	 * Saves network information to the specified file
	 */
	public void writeGauge(File theFile) {

		theGaugePanel.setCurrentFile(theFile);

		try {
			LocalConfiguration.getInstance().getProperties().setProperty(
						"org.exolab.castor.indent", "true");
			
			FileWriter writer = new FileWriter(theFile);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");
			Marshaller marshaller = new Marshaller(writer);
			marshaller.setMapping(map);
			// marshaller.setDebug(true);
			theGaugePanel.getGauge().getCurrentProjector().getUpstairs().initPersistentData();
			theGaugePanel.getGauge().getCurrentProjector().getDownstairs().initPersistentData();
			marshaller.marshal(theGaugePanel);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		String localDir = new String(System.getProperty("user.dir"));
		setPath(Utils.getRelativePath(localDir, theGaugePanel.getCurrentFile().getAbsolutePath()));
		setName(theFile.getName());
		
	}

	public void readGauge(File f) {
		
		try {
			Reader reader = new FileReader(f);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");
			Unmarshaller unmarshaller = new Unmarshaller(theGaugePanel);
			unmarshaller.setMapping(map);
			//unmarshaller.setDebug(true);
			theGaugePanel = (GaugePanel) unmarshaller.unmarshal(reader);
			theGaugePanel.getGauge().getCurrentProjector().getUpstairs().initCastor();
			theGaugePanel.getGauge().getCurrentProjector().getDownstairs().initCastor();
			theGaugePanel.updateGaugePanel();
			theGaugePanel.updateProjectionMenu();
			
			//Set Path; used in workspace persistence
			String localDir = new String(System.getProperty("user.dir"));
			theGaugePanel.setCurrentFile(f);
			setPath(Utils.getRelativePath(localDir, theGaugePanel.getCurrentFile().getAbsolutePath()));

			
		}  catch (java.io.FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Could not find the file \n" + f,
			        "Warning", JOptionPane.ERROR_MESSAGE);
			return;
		} catch (Exception e){
		    JOptionPane.showMessageDialog(null, "There was a problem opening the file \n" + f,
			        "Warning", JOptionPane.ERROR_MESSAGE);
		    e.printStackTrace();
			return;
		}
		setName(f.getName());

	}

	
	
	
	/**
	 * Used in persisting
	 */
	public void initGaugedVars() {
		NetworkFrame net = getWorkspace().getNetwork(networkName);

		if (net == null) {
			return;
		}
		if (persistentGaugedVars == null) {
			return;	
		}
		
		ArrayList the_vars = new ArrayList();

		StringTokenizer st = new StringTokenizer(persistentGaugedVars, ",");

		while (st.hasMoreTokens()) {
			PNode pn = (PNode) net.getNetPanel().getPNode(st.nextToken());
			if (pn == null) {
				return;
			}
			the_vars.add(pn);
		}

		this.setGaugedVars(the_vars);
	}
	
	/**
	 * Send state information to gauge
	 */
	public void update() {
		changedSinceLastSave = true;
		theGaugePanel.getGauge().addDatapoint(getState());
		theGaugePanel.updateGaugePanel();
		theGaugePanel.setHotPoint(theGaugePanel.getGauge().getUpstairs().getClosestIndex(getState()));
	}

	public void internalFrameOpened(InternalFrameEvent e){
	}
	
	public void internalFrameClosing(InternalFrameEvent e){
		if(isChangedSinceLastSave()){
			hasChanged();
		} else
			dispose();

	}

	public void internalFrameClosed(InternalFrameEvent e){
		this.getWorkspace().getGaugeList().remove(this);
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

	/**
	 * @return Returns the theGaugePanel.
	 */
	public GaugePanel getGaugePanel() {
		return theGaugePanel;
	}
	/**
	 * @param theGaugePanel The theGaugePanel to set.
	 */
	public void setGaugePanel(GaugePanel theGaugePanel) {
		this.theGaugePanel = theGaugePanel;
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
		this.name = name;
		setTitle(name);
	}
	
	/**
	 * @return Returns the gaugedVars.
	 */
	public ArrayList getGaugedVars() {
		return gaugedVars;
	}
	/**
	 * @param gaugedVars The gaugedVars to set.
	 */
	public void setGaugedVars(ArrayList gaugedVars) {
		this.gaugedVars = gaugedVars;
		theGaugePanel.getGauge().init(gaugedVars.size());
		persistentGaugedVars = getGaugedVarsString();
	}
	
	/**
	 * Get a string version of the list of gauged variables/
	 * For persistence
	 */
	private String getGaugedVarsString() {
		String ret = new String();
		
		for (int i = 0; i < gaugedVars.size(); i++) {
			String name = ((GaugeSource)gaugedVars.get(i)).getId();
			if (name == null) break;
			if (i == gaugedVars.size() -1) {
				ret = ret.concat(name);
			} else {
				ret = ret.concat(name + ",");
			}
		}
		
		return ret;
	}
		
	// Convert gauged variable states into a double array to be sent
	// to the hisee gauge
	private double[] getState() {

		double ret[] = new double[gaugedVars.size()];

		Iterator it = gaugedVars.iterator();
		int i = 0;
		while (it.hasNext()) {
			GaugeSource gs = (GaugeSource)it.next();
			ret[i] = gs.getGaugeValue();
			i++;
		}
		return ret;
	}
	
	/**
	 * @return Returns the networkName.
	 */
	public String getNetworkName() {
		return networkName;
	}
	/**
	 * @param networkName The networkName to set.
	 */
	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}
	
	/**
	 * @return Returns the persistentGaugedVars.
	 */
	public String getPersistentGaugedVars() {
		return persistentGaugedVars;
	}
	/**
	 * @param persistentGaugedVars The persistentGaugedVars to set.
	 */
	public void setPersistentGaugedVars(String persistentGaugedVars) {
		this.persistentGaugedVars = persistentGaugedVars;
	}
	
	
	/**
	 * Checks to see if anything has changed and then offers to save if true
	 *
	 */
	public void hasChanged(){
		Object[] options = {"Save", "Don't Save","Cancel"};
		int s = JOptionPane.showInternalOptionDialog(this, "Gauge " + this.getName() + " has changed since last save,\nwould you like to save these changes?","Gauge Has Changed",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null, options,options[0]);
		if (s == 0){
		//	saveCombined();
			dispose();
		} else if (s == 1){
			dispose();
		} else
			return;
	}

	public boolean isChangedSinceLastSave() {
		return changedSinceLastSave;
	}

	public void setChangedSinceLastSave(boolean changedSinceLastSave) {
		this.changedSinceLastSave = changedSinceLastSave;
	}

	public void menuCanceled(MenuEvent arg0) {
	}

	public void menuDeselected(MenuEvent arg0) {
	}

	public void menuSelected(MenuEvent arg0) {
		if(arg0.getSource().equals(fileMenu)){
			if(this.isChangedSinceLastSave()){
			//	saveCombined.setEnabled(true);
			} else if (!this.isChangedSinceLastSave()){
			//	saveCombined.setEnabled(false);
			}
		}
	}
}