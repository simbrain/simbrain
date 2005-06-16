/*
 * Created on May 28, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.gauge;


import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.*;

import org.hisee.core.Gauge;
import org.hisee.graphics.GaugePanel;
import org.simbrain.workspace.Workspace;


/**
 * This class wraps a Gauge object in a Simbrain workspace frame, which also stores 
 * information about the variables the Gauge is representing.
 */
public class GaugeFrame extends JInternalFrame implements InternalFrameListener{

	
	private Workspace workspace;
	private Gauge theGauge;

	private String name = null;
	private ArrayList gaugedVars;		// the variables this gauge gauges 
	
	// For workspace persistence 
	private String path = null;
	private int xpos;
	private int ypos;
	private int the_width;
	private int the_height;

	public GaugeFrame() {	
	}

	public GaugeFrame(Workspace ws) {

		workspace = ws;
		init();
	}
	
	public void init() {
 
		theGauge = new Gauge();
		GaugePanel gp = new GaugePanel(theGauge);
		theGauge.setGp(gp);
		getContentPane().add(gp);
		gp.setUpMenus(this);
		
		this.setResizable(true);
		this.setMaximizable(true);
		this.setIconifiable(true);
		this.setClosable(true);	

		this.getContentPane().add("Center", gp);		
		
		this.addInternalFrameListener(this);
        
		theGauge.setUsingGraphics(true);
		theGauge.setUsingOnOff(true, gp);
		theGauge.setUsingHotPoint(true);
		theGauge.setProperties(gp);
		
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
	
}
