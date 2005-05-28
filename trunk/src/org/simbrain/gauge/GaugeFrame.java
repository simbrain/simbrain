/*
 * Created on May 28, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.gauge;


import javax.swing.JInternalFrame;

import org.hisee.core.Gauge;
import org.hisee.graphics.GaugePanel;
import org.simbrain.workspace.Workspace;


/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GaugeFrame extends JInternalFrame {

	
	private Workspace workspace;
	private Gauge theGauge;
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
 
		Gauge theGauge = new Gauge();
		GaugePanel gp = new GaugePanel(theGauge);
		getContentPane().add(gp);
		gp.setUpMenus(this);
		
		this.setResizable(true);
		this.setMaximizable(true);
		this.setIconifiable(true);
		this.setClosable(true);	

		this.getContentPane().add("Center", gp);		

		theGauge.setUsingGraphics(true);
		theGauge.setUsingOnOff(true, gp);
		theGauge.setUsingHotPoint(true);
		theGauge.setProperties(gp);
		
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

}
