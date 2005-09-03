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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.coupling.Coupling;
import org.simbrain.coupling.MotorCoupling;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.world.dataworld.DataWorldFrame;
import org.simbrain.world.odorworld.OdorWorldFrame;


/**
 * WorkspaceSerializer handles workspace persistence.  It contains static methods for reading and writing
 * workspace files, and also serves as a buffer for Castor initialization.
 */
public class WorkspaceSerializer {
	
	private static final String FS = System.getProperty("file.separator");
	
	//Holders for unmarshalling
	private ArrayList networkList = new ArrayList();
	private ArrayList odorWorldList = new ArrayList();
	private ArrayList dataWorldList = new ArrayList();
	private ArrayList gaugeList = new ArrayList();
	
	/**
	 * Read in workspace file
	 * 
	 * @param wspace reference to current workspace
	 * @param f file containing new workspace information
	 */
	public static void readWorkspace(Workspace wspace, File f) {
		wspace.clearWorkspace();
		WorkspaceSerializer w_serializer = new WorkspaceSerializer();
		try {
			Reader reader = new FileReader(f);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "workspace_mapping.xml");
			Unmarshaller unmarshaller = new Unmarshaller(w_serializer);
			unmarshaller.setMapping(map);
			//unmarshaller.setDebug(true);
			w_serializer = (WorkspaceSerializer)unmarshaller.unmarshal(reader);
			
		} catch (java.io.FileNotFoundException e) {
		    JOptionPane.showMessageDialog(null, "Could not find workspace file \n"
			        + f, "Warning", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();       
		    return;
		} catch (Exception e){
		    JOptionPane.showMessageDialog(null, "There was a problem opening the workspace file \n"
			        + f, "Warning", JOptionPane.ERROR_MESSAGE);
		    e.printStackTrace();
		    return;
		}
		for(int i = 0; i < w_serializer.getOdorWorldList().size(); i++) {
			OdorWorldFrame wld = (OdorWorldFrame)w_serializer.getOdorWorldList().get(i);
			wld.init();
			wld.setWorkspace(wspace);
			wld.setBounds(wld.getXpos(), wld.getYpos(), wld.getThe_width(), wld.getThe_height());
			if (wld.getGenericPath() != null) {
				wld.readWorld(new File(wld.getGenericPath()));						
			}
			wld.getWorld().setParentWorkspace(wspace);

			wspace.addOdorWorld(wld);
		}
		
		for(int i = 0; i < w_serializer.getDataWorldList().size(); i++) {
			DataWorldFrame wld = (DataWorldFrame)w_serializer.getDataWorldList().get(i);
			wld.init();
			wld.setWorkspace(wspace);
			wld.setBounds(wld.getXpos(), wld.getYpos(), wld.getThe_width(), wld.getThe_height());
			if (wld.getGenericPath() != null) {
				wld.readWorld(new File(wld.getGenericPath()));						
			}
			wspace.addDataWorld(wld);
		}

		for(int i = 0; i < w_serializer.getNetworkList().size(); i++) {
			NetworkFrame net = (NetworkFrame)w_serializer.getNetworkList().get(i);
			net.init();
			net.setWorkspace(wspace);
			net.setBounds(net.getXpos(), net.getYpos(), net.getThe_width(), net.getThe_height());
			if (net.getGenericPath() != null) {
				net.getNetPanel().open(new File(net.getGenericPath()));				
			}
			wspace.addNetwork(net);
		}

		for(int i = 0; i < w_serializer.getGaugeList().size(); i++) {
			GaugeFrame gauge = (GaugeFrame)w_serializer.getGaugeList().get(i);
			gauge.init();
			gauge.setWorkspace(wspace);
			gauge.initGaugedVars();
			
			if(gauge.getGaugedVars() == null) {
				continue;
			}
			
			if(gauge.getGenericPath() != null) {
				//gauge.getGaugePanel().openCombined(new File(gauge.getGenericPath()));								
			}
			
			gauge.setBounds(gauge.getXpos(), gauge.getYpos(), gauge.getThe_width(), gauge.getThe_height());
			wspace.addGauge(gauge);
				gauge.setTitle(gauge.getGaugePanel().getCurrentFile().getName());
		}
		
		// Create couplings and attach agents to them
		ArrayList couplings = wspace.getCouplingList();	
		wspace.attachAgentsToCouplings(couplings);

		// Graphics clean up
		wspace.getNetworkList().repaintAllNetworkPanels();
		wspace.setTitle(f.getName());
		wspace.setCurrentFile(f);
		
		wspace.setWorkspaceChanged(false);

	}
	

	/**
	 * Save workspace information
	 * 
	 * @param ws reference to current workspace
	 * @param theFile file to save information to
	 */
	public static void writeWorkspace(Workspace ws, File theFile) {

		WorkspaceSerializer serializer = new WorkspaceSerializer();
		
		for(int i = 0; i < ws.getNetworkList().size(); i++) {
			NetworkFrame net = (NetworkFrame)ws.getNetworkList().get(i);
			net.initBounds();
		}
		for(int i = 0; i < ws.getOdorWorldList().size(); i++) {
			OdorWorldFrame wld = (OdorWorldFrame)ws.getOdorWorldList().get(i);
			wld.initBounds();
		}
		for(int i = 0; i < ws.getDataWorldList().size(); i++) {
			DataWorldFrame wld = (DataWorldFrame)ws.getDataWorldList().get(i);
			wld.initBounds();
		}		
		for(int i = 0; i < ws.getGaugeList().size(); i++) {
			GaugeFrame gauge = (GaugeFrame)ws.getGaugeList().get(i);
			gauge.initBounds();
		}
		
		serializer.setNetworkList(ws.getNetworkList());
		serializer.setOdorWorldList(ws.getOdorWorldList());
		serializer.setDataWorldList(ws.getDataWorldList());
		serializer.setGaugeList(ws.getGaugeList());
		
		LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");
	
		try {
			FileWriter writer = new FileWriter(theFile);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "workspace_mapping.xml");
			Marshaller marshaller = new Marshaller(writer);
			marshaller.setMapping(map);
			//marshaller.setDebug(true);
			marshaller.marshal(serializer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ws.setTitle(theFile.getName());		
		ws.setWorkspaceChanged(false);
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
	 * @return Returns the odorWorldList.
	 */
	public ArrayList getOdorWorldList() {
		return odorWorldList;
	}
	/**
	 * @param odorWorldList The odorWorldList to set.
	 */
	public void setOdorWorldList(ArrayList worldList) {
		this.odorWorldList = worldList;
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
	}
}
