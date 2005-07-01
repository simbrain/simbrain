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

import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.network.NetworkFrame;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;

import edu.umd.cs.piccolo.PNode;

/**
 * This class wraps a Gauge object in a Simbrain workspace frame, which also stores 
 * information about the variables the Gauge is representing.
 */
public class GaugeFrame extends JInternalFrame implements InternalFrameListener, ActionListener{

	
	private Workspace workspace;
	private Gauge theGauge;

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
	JMenuItem openHi = new JMenuItem("Open High-Dimensional Dataset");
	JMenuItem openLow = new JMenuItem("Open Low-Dimensional Dataset");
	JMenuItem openCombined = new JMenuItem("Open Combined (High/Low) Dataset");
	JMenuItem saveLow = new JMenuItem("Save Low-Dimensional Dataset");
	JMenuItem saveHi = new JMenuItem("Save High-Dimensional Dataset");
	JMenuItem saveCombined = new JMenuItem("Save Combined (High/Low) Dataset");
	JMenuItem addHi = new JMenuItem("Add High-Dimensional Data");
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
 
		theGauge = new Gauge();
		getContentPane().add(theGauge.getGp());

		this.addInternalFrameListener(this);
		this.setResizable(true);
		this.setMaximizable(true);
		this.setIconifiable(true);
		this.setClosable(true);	
		
		setUpMenus();
	}
	
	
	private void setUpMenus() {
		setJMenuBar(mb);
		
		mb.add(fileMenu);
		mb.add(prefsMenu);
		//mb.add(helpMenu);
		
		openHi.addActionListener(this);
		openLow.addActionListener(this);
		openCombined.addActionListener(this);
		saveHi.addActionListener(this);
		saveLow.addActionListener(this);
		saveCombined.addActionListener(this);
		addHi.addActionListener(this);
		projectionPrefs.addActionListener(this);
		graphicsPrefs.addActionListener(this);
		generalPrefs.addActionListener(this);
		setAutozoom.addActionListener(this);
		close.addActionListener(this);
		
		openCombined.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		saveCombined.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		fileMenu.add(openCombined);
		fileMenu.add(saveCombined);
		fileMenu.addSeparator();
		fileMenu.add(openHi);
		fileMenu.add(saveHi);
		fileMenu.add(addHi);
		fileMenu.addSeparator();		
		fileMenu.add(openLow);
		fileMenu.add(saveLow);
		fileMenu.addSeparator();		
		fileMenu.add(close);
		
		prefsMenu.add(projectionPrefs);
		prefsMenu.add(graphicsPrefs);
		prefsMenu.add(generalPrefs);
		prefsMenu.addSeparator();
		prefsMenu.add(setAutozoom);
		
		//helpMenu.add(helpItem);
		
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if( (e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class) ) {

			JMenuItem jmi = (JMenuItem) e.getSource();
			
			if(jmi == openCombined)  {
				theGauge.getGp().openCombined();
				String localDir = new String(System.getProperty("user.dir"));
				if (theGauge.getGp().getCurrentFile() != null) {
					this.setPath(Utils.getRelativePath(localDir, theGauge.getGp().getCurrentFile().getAbsolutePath()));					
					setTitle(theGauge.getGp().getCurrentFile().getName());
				}
			} else if(jmi == saveCombined)  {
				theGauge.getGp().saveCombined();
				String localDir = new String(System.getProperty("user.dir"));
				if (theGauge.getGp().getCurrentFile() != null) {
					this.setPath(Utils.getRelativePath(localDir, theGauge.getGp().getCurrentFile().getAbsolutePath()));					
					setTitle(theGauge.getGp().getCurrentFile().getName());
				}
			} else if(jmi == openHi)  {
					theGauge.getGp().openHi();
				} else if(jmi == openLow)  {
					theGauge.getGp().openLow();
			} else if(jmi == saveLow)  {
				theGauge.getGp().saveLow();
			} else if(jmi == saveHi)  {
				theGauge.getGp().saveHi();
			} else if(jmi == addHi)  {
				theGauge.getGp().addHi();
			} else if(jmi == projectionPrefs)  {
				theGauge.getGp().handlePreferenceDialogs();
			} else if(jmi == graphicsPrefs)  {
				theGauge.getGp().handleGraphicsDialog();
			} else if(jmi == generalPrefs)  {
				theGauge.getGp().handleGeneralDialog();
			} else if(jmi == setAutozoom)  {
				theGauge.getGp().setAutoZoom(setAutozoom.isSelected());
				theGauge.getGp().repaint();
			} else if(jmi == close){
				dispose();
			}
		}
			
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
		theGauge.addDatapoint(getState());
	}

	public void internalFrameOpened(InternalFrameEvent e){
	}
	
	public void internalFrameClosing(InternalFrameEvent e){
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
	 * @return Returns the theGauge.
	 */
	public Gauge getGauge() {
		return theGauge;
	}
	/**
	 * @param theGauge The theGauge to set.
	 */
	public void setGauge(Gauge theGauge) {
		this.theGauge = theGauge;
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
		theGauge.init(gaugedVars.size());
		persistentGaugedVars = getGaugedVarsString();
	}
	
	/**
	 * Get a string version of the list of gauged variables/
	 * For persistence
	 */
	private String getGaugedVarsString() {
		String ret = new String();
		
		for (int i = 0; i < gaugedVars.size(); i++) {
			String name = ((GaugeSource)gaugedVars.get(i)).getName();
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
	
	public void hasChanged(){
		Object[] options = {"Yes", "No"};
		int s = JOptionPane.showInternalOptionDialog(this,"This Guage has changed since last save,\nWould you like to save these changes?","Guage Has Changed",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null, options,options[0]);
		if (s == 0){
//			save();
		} else if (s == 1){
		}
	}
}